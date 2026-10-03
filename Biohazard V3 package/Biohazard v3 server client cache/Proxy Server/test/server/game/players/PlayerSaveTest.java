package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Characterisation tests for the flat-file character format. They pin today's
 * behaviour so the save path can be restructured later without silently
 * changing what a .txt file means.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class PlayerSaveTest {

	private static final int SLOT = 1;
	private static final String NAME = "TestChar";
	private static final String PASS = "swordfish";
	/** Chosen because it is not a valid identifier fragment, so a leak is obvious. */
	private static final String CLAN_CHANNEL = "Some-Founder";

	private static Path save() {
		return Paths.get("./Data/characters/" + NAME + ".txt");
	}

	private static Path bak() {
		return Paths.get("./Data/characters/" + NAME + ".txt.bak");
	}

	private static Path tmp() {
		return Paths.get("./Data/characters/" + NAME + ".txt.tmp");
	}

	@AfterEach
	void tearDown() throws IOException {
		PlayerHandler.players[SLOT] = null;
		Files.deleteIfExists(save());
		// unwritableBackupDoesNotBlockTheSave parks a non-empty directory on the .bak path
		if (Files.isDirectory(bak())) {
			Files.deleteIfExists(bak().resolve("blocker"));
		}
		Files.deleteIfExists(bak());
		Files.deleteIfExists(tmp());
	}

	private static Client savableClient() {
		Client c = new Client(null, SLOT);
		c.playerName = NAME;
		c.playerName2 = NAME;
		c.playerPass = PASS;
		c.saveFile = true;
		c.saveCharacter = true;
		c.newPlayer = false;
		PlayerHandler.players[SLOT] = c;
		return c;
	}

	private static void populate(Client c) {
		c.position.heightLevel = 1;
		c.position.absX = 3000;
		c.position.absY = 3100;
		c.playerRights = 2;
		c.skills.playerLevel[1] = 50;
		c.skills.playerXP[1] = 123456;
		c.playerItems[0] = 995;
		c.playerItemsN[0] = 1000;
		c.clanChat.channel = CLAN_CHANNEL;
		// Non-default on purpose: the field initialisers are 0, 0, true, 3, so the
		// round-trip below only proves anything if these differ from them.
		c.settings.musicVolume = 3;
		c.settings.soundEffectVolume = 1;
		c.settings.musicEnabled = false;
		c.settings.brightness = 1;
		// Bounty Hunter defaults are 0, 0, 1, 0 and false respectively.
		c.bountyHunter.rogueKills = 7;
		c.bountyHunter.bountyKills = 9;
		c.bountyHunter.killsMultiplier = 4;
		c.bountyHunter.safeTimer = 120;
		c.bountyHunter.penaltyTimer = true;
	}

	@Test
	void firstSavePublishesFileAndLeavesNothingBehind() {
		Client c = savableClient();
		populate(c);

		assertTrue(PlayerSave.saveGame(c), "saveGame should succeed");
		assertTrue(Files.exists(save()), "character file should exist");
		assertFalse(Files.exists(tmp()), "temp file should have been moved into place");
		assertFalse(Files.exists(bak()), "first save has no previous version to keep");
	}

	@Test
	void secondSaveKeepsThePreviousVersionAsBak() throws IOException {
		Client c = savableClient();
		populate(c);
		assertTrue(PlayerSave.saveGame(c));

		c.position.absX = 3200;
		assertTrue(PlayerSave.saveGame(c));

		assertTrue(Files.exists(bak()), "previous save should be retained as .bak");
		assertTrue(Files.readString(bak(), StandardCharsets.UTF_8).contains("character-posx = 3000"),
				".bak should hold the value from the save before last");
		assertTrue(Files.readString(save(), StandardCharsets.UTF_8).contains("character-posx = 3200"),
				"live file should hold the newest value");
	}

	@Test
	void unwritableBackupDoesNotBlockTheSave() throws IOException {
		Client c = savableClient();
		populate(c);
		assertTrue(PlayerSave.saveGame(c));

		// Park a non-empty directory on the .bak path so the backup copy cannot
		// succeed by any means. The backup is a safety net: if it can veto the
		// publish, a locked or full disk costs the player the whole character.
		Files.deleteIfExists(bak());
		Files.createDirectories(bak());
		Files.write(bak().resolve("blocker"), new byte[0]);

		c.position.absX = 3300;
		assertTrue(PlayerSave.saveGame(c), "a failed .bak refresh must not stop the save");
		assertTrue(Files.readString(save(), StandardCharsets.UTF_8).contains("character-posx = 3300"),
				"the new state must still be published");
		assertFalse(Files.exists(tmp()), "temp file must not be left behind");
	}

	@Test
	void characterStateSurvivesARoundTrip() {
		Client saved = savableClient();
		populate(saved);
		assertTrue(PlayerSave.saveGame(saved));

		Client loaded = new Client(null, SLOT);
		PlayerHandler.players[SLOT] = loaded;
		assertEquals(1, PlayerSave.loadGame(loaded, NAME, PASS), "[EOF] should report a clean load");

		assertEquals(1, loaded.position.heightLevel);
		assertEquals(3000, loaded.position.teleportToX);
		assertEquals(3100, loaded.position.teleportToY);
		assertEquals(2, loaded.playerRights);
		assertEquals(50, loaded.skills.playerLevel[1]);
		assertEquals(123456, loaded.skills.playerXP[1]);
		assertEquals(995, loaded.playerItems[0]);
		assertEquals(1000, loaded.playerItemsN[0]);
		assertEquals(3, loaded.settings.musicVolume, "musicVolume must survive a restart");
		assertEquals(1, loaded.settings.soundEffectVolume, "soundEffectVolume must survive a restart");
		assertFalse(loaded.settings.musicEnabled, "musicEnabled must survive a restart");
		assertEquals(1, loaded.settings.brightness, "brightness must survive a restart");
		assertEquals(7, loaded.bountyHunter.rogueKills, "rogueKills must survive a restart");
		assertEquals(9, loaded.bountyHunter.bountyKills, "bountyKills must survive a restart");
		assertEquals(4, loaded.bountyHunter.killsMultiplier, "killsMultiplier must survive a restart");
		assertEquals(120, loaded.bountyHunter.safeTimer, "safeTimer must survive a restart");
		assertTrue(loaded.bountyHunter.penaltyTimer, "penaltyTimer must survive a restart");
	}

	@Test
	void clanChatChannelRoundTripsUnderItsLegacyFileKey() throws IOException {
		// Phase 4.3 renamed the Java field to clanChat.channel. The on-disk key is a
		// separate contract: it must stay `lastclanchat` or every existing character file
		// stops loading its clan chat. This is the only place that contract is checked.
		Client saved = savableClient();
		populate(saved);
		assertTrue(PlayerSave.saveGame(saved));

		String text = Files.readString(save(), StandardCharsets.UTF_8);
		assertTrue(text.contains("lastclanchat = " + CLAN_CHANNEL),
				"the save key must remain the legacy `lastclanchat`");
		assertFalse(text.contains("clanChat"),
				"the Java name must not leak into the file format");

		Client loaded = new Client(null, SLOT);
		PlayerHandler.players[SLOT] = loaded;
		assertEquals(1, PlayerSave.loadGame(loaded, NAME, PASS));
		assertEquals(CLAN_CHANNEL, loaded.clanChat.channel);
	}

	@Test
	void clientSettingsRoundTripUnderTheirOwnFileKeys() throws IOException {
		// Phase 4.4 moved these six fields under client.settings but deliberately left the
		// leaf names alone, because four of them are also the literal save-file keys. Pin
		// both halves of that: the keys still work, and the collaborator path stays out of
		// the file format.
		Client saved = savableClient();
		populate(saved);
		assertTrue(PlayerSave.saveGame(saved));

		String text = Files.readString(save(), StandardCharsets.UTF_8);
		assertTrue(text.contains("musicVolume = 3"), "musicVolume must keep its own key");
		assertTrue(text.contains("soundEffectVolume = 1"), "soundEffectVolume must keep its own key");
		assertTrue(text.contains("brightness = 1"), "brightness must keep its own key");
		// Known cosmetic bug, pinned rather than fixed: the write is
		// `write("musicEnabled = ", 0, 14)` but the literal is 15 characters, so the
		// separating space is dropped and the line reads `musicEnabled =false`. The loader
		// trims token2, so it parses fine. Fixing the length literal to 15 is a deliberate
		// format change, and this assertion should be what has to change with it.
		assertTrue(text.contains("musicEnabled =false"),
				"musicEnabled is written with no separating space (the 14-length literal truncates it)");

		assertFalse(text.contains("settings"), "the Java collaborator name must not leak into the file");
		assertFalse(text.contains("isLoopingMusic"),
				"isLoopingMusic is not persisted and must stay out of the format");
	}

	@Test
	void brightnessRoundTrips() throws IOException {
		// Fixed here as its own step, separate from Phase 4.4's move: saveGame had always
		// written `brightness = <n>`, but loadGame had no `token.equals("brightness")` case,
		// so the value reached the file and was then ignored. Since Client.initialize
		// pushes settings.brightness into config frame 166 on login, every player silently
		// lost their brightness setting on every relog -- invisibly, because it works for
		// the whole session. Phase 4.4 pinned the broken behaviour first; this is the flip.
		Client saved = savableClient();
		populate(saved);
		assertTrue(PlayerSave.saveGame(saved));

		assertTrue(Files.readString(save(), StandardCharsets.UTF_8).contains("brightness = 1"),
				"the chosen brightness must reach the file");

		Client loaded = new Client(null, SLOT);
		PlayerHandler.players[SLOT] = loaded;
		assertEquals(1, PlayerSave.loadGame(loaded, NAME, PASS));
		assertEquals(1, loaded.settings.brightness, "brightness must survive a restart, not reset to the default");
	}

	@Test
	void anUnreadableBrightnessIsSkippedLikeAnyOtherBadField() throws IOException {
		// The new loader case goes through Integer.parseInt, so it must sit inside the same
		// Phase 0.6 guard as every other numeric read: a hand-edited or truncated value has
		// to cost one field, not the whole login.
		Client c = savableClient();
		populate(c);
		// bankQuantity is written immediately after brightness in the save, so it is the
		// proof that the read loop carried on past the bad value rather than stopping at it.
		c.bankQuantity = 42;
		assertTrue(PlayerSave.saveGame(c));

		List<String> lines = new ArrayList<>(Files.readAllLines(save(), StandardCharsets.UTF_8));
		boolean corrupted = false;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).startsWith("brightness = ")) {
				lines.set(i, "brightness = VERY_BRIGHT");
				corrupted = true;
				break;
			}
		}
		assertTrue(corrupted, "expected to find a brightness line to corrupt");
		Files.write(save(), lines, StandardCharsets.UTF_8);

		Client loaded = new Client(null, SLOT);
		PlayerHandler.players[SLOT] = loaded;
		assertEquals(1, PlayerSave.loadGame(loaded, NAME, PASS),
				"a malformed brightness must not stop the load from reaching [EOF]");
		assertEquals(3, loaded.settings.brightness, "the unreadable value is skipped, leaving the default");
		assertEquals(42, loaded.bankQuantity, "the field written after the corrupt one must still be read");
	}

	@Test
	void bountyHunterTalliesRoundTripUnderTheirOwnFileKeys() throws IOException {
		// Phase 4.6 moved these under client.bountyHunter but kept the leaf names, because
		// five of them are also the literal save keys. Pin the file half of that: the keys
		// still work, and the collaborator name stays out of the format.
		Client saved = savableClient();
		populate(saved);
		assertTrue(PlayerSave.saveGame(saved));

		String text = Files.readString(save(), StandardCharsets.UTF_8);
		assertTrue(text.contains("rogueKills = 7"), "rogueKills must keep its own key");
		assertTrue(text.contains("bountyKills = 9"), "bountyKills must keep its own key");
		assertTrue(text.contains("killsMultiplier = 4"), "killsMultiplier must keep its own key");
		assertTrue(text.contains("safeTimer = 120"), "safeTimer must keep its own key");
		assertTrue(text.contains("penaltyTimer = true"), "penaltyTimer must keep its own key");

		assertFalse(text.contains("bountyHunter"),
				"the Java collaborator name must not leak into the file format");
		assertFalse(text.contains("targetIndex"),
				"targetIndex is session state and must not be persisted");
		assertFalse(text.contains("targetName"),
				"targetName is session state and must not be persisted");
		assertFalse(text.contains("inBH"), "inBH is session state and must not be persisted");
	}

	@Test
	void wrongPasswordIsRejected() {
		Client c = savableClient();
		populate(c);
		assertTrue(PlayerSave.saveGame(c));

		assertEquals(3, PlayerSave.loadGame(new Client(null, SLOT), NAME, "not-the-password"));
	}

	@Test
	void missingFileReportsANewCharacter() {
		Client c = new Client(null, SLOT);
		assertEquals(0, PlayerSave.loadGame(c, "NoSuchCharacter", "whatever"));
		assertFalse(c.newPlayer);
	}

	@Test
	void truncatedFileIsReportedAsThirteen() throws IOException {
		Client c = savableClient();
		populate(c);
		assertTrue(PlayerSave.saveGame(c));

		List<String> lines = new ArrayList<>(Files.readAllLines(save(), StandardCharsets.UTF_8));
		assertTrue(lines.remove("[EOF]"), "the save should end with an [EOF] marker");
		Files.write(save(), lines, StandardCharsets.UTF_8);

		Client loaded = new Client(null, SLOT);
		PlayerHandler.players[SLOT] = loaded;
		assertEquals(13, PlayerSave.loadGame(loaded, NAME, PASS),
				"running off the end without [EOF] must not be reported as a clean load");
		assertEquals(3000, loaded.position.teleportToX, "the partial state up to the truncation is still read");
	}

	@Test
	void oneUnreadableFieldDoesNotAbortTheRestOfTheFile() throws IOException {
		Client c = savableClient();
		populate(c);
		assertTrue(PlayerSave.saveGame(c));

		List<String> lines = new ArrayList<>(Files.readAllLines(save(), StandardCharsets.UTF_8));
		boolean corrupted = false;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).startsWith("character-posx")) {
				lines.set(i, "character-posx = NOT_A_NUMBER");
				corrupted = true;
				break;
			}
		}
		assertTrue(corrupted, "expected to find a character-posx line to corrupt");
		Files.write(save(), lines, StandardCharsets.UTF_8);

		Client loaded = new Client(null, SLOT);
		PlayerHandler.players[SLOT] = loaded;
		assertEquals(1, PlayerSave.loadGame(loaded, NAME, PASS),
				"a malformed field must not stop the load from reaching [EOF]");
		assertEquals(2, loaded.playerRights,
				"fields after the corrupt one must still be read");
		assertEquals(50, loaded.skills.playerLevel[1]);
		assertEquals(995, loaded.playerItems[0]);
	}
}
