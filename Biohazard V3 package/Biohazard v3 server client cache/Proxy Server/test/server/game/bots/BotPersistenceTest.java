package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import server.game.players.PlayerHandler;

/**
 * Slice-1 step 2: a bot persists exactly like a player.
 *
 * <p>The point of these tests is that {@code isBot} is a <em>label</em>, not a save gate.
 * Bots go through the ordinary {@code PlayerSave}/{@code saveAllPlayers} paths untouched,
 * because the possessed account is a real character a human can later log into.
 */
class BotPersistenceTest {

	private static final int SLOT = 1;
	private static final String NAME = "botalpha";

	@AfterEach
	void tearDown() throws IOException {
		PlayerHandler.players[SLOT] = null;
		Files.deleteIfExists(characterFile(NAME));
	}

	private static Path characterFile(String name) {
		return Paths.get("./Data/characters/" + name + ".txt");
	}

	/** Registers a possessed bot the way {@code BotManager} will: real name, real slot. */
	private static BotPlayer possessed(String name) {
		BotPlayer b = new BotPlayer(SLOT);
		b.playerName = name;
		b.playerName2 = name;
		b.playerPass = "swordfish";
		b.newPlayer = false; // PlayerSave refuses a client that is still "new"
		PlayerHandler.players[SLOT] = b;
		return b;
	}

	@Test
	void aBotIsFlaggedAndSavesOnceLikeAPlayer() {
		BotPlayer b = possessed(NAME);

		assertTrue(b.isBot, "a BotPlayer identifies itself as a bot");
		assertTrue(b.isActive, "and is live in the world");
		assertTrue(b.saveFile && b.saveCharacter, "a bot persists like a player");

		assertTrue(b.saveCharacterOnce(), "the character reaches the disk");
		assertTrue(Files.exists(characterFile(NAME)), "an ordinary character file appears");
		assertFalse(b.saveCharacterOnce(), "and only one write, exactly like a player");
	}

	@Test
	void theShutdownSweepIncludesBots() {
		possessed(NAME);

		assertTrue(PlayerHandler.saveAllPlayers() >= 1,
				"saveAllPlayers must not skip bots — that is the shutdown hook's contract");
		assertTrue(Files.exists(characterFile(NAME)));
	}

	@Test
	void isBotIsALabelNotASaveGate() {
		BotPlayer b = possessed(NAME);
		b.isBot = false; // e.g. a human taking the account over

		assertTrue(b.saveCharacterOnce(), "clearing the flag must not change saving");
		assertTrue(Files.exists(characterFile(NAME)));
	}

	@Test
	void aBotHasNoSessionAndNeverThrowsOnFlush() {
		BotPlayer b = possessed(NAME);

		assertTrue(b.getSession() == null, "a bot is sessionless by construction");
		b.getOutStream().packetEncryption = new core.util.ISAACRandomGen(new int[] { 1, 2, 3, 4 });
		b.sendMessage("this would be sent to a player");
		b.flushOutStream();
		assertEquals(0, b.getOutStream().currentOffset, "and the bytes are dropped, not queued");
	}
}
