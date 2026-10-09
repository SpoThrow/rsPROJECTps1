package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import server.game.players.PlayerHandler;

/**
 * Slice-1 step 3: {@code BotManager} possess/release, the hard cap, and the persistence
 * round-trip that proves the possessed account is a real character.
 */
class BotManagerCapTest {

	private static final String PASSWORD = "swordfish";

	/** Every account this test created, so the tear-down can delete the files. */
	private final List<String> created = new ArrayList<String>();

	@AfterEach
	void tearDown() throws IOException {
		// Release whatever is still live, then remove every file this test wrote.
		for (BotPlayer bot : BotManager.all()) {
			BotManager.release(bot.playerName);
		}
		for (String name : created) {
			Files.deleteIfExists(characterFile(name));
		}
		created.clear();
	}

	private static Path characterFile(String name) {
		return Paths.get("./Data/characters/" + name.toLowerCase() + ".txt");
	}

	private static String account(int i) {
		return "botcap" + i; // <= 12 chars, alphanumeric, carries the reserved prefix
	}

	private boolean newAccount(String name) {
		created.add(name);
		return BotManager.createAccount(name, PASSWORD);
	}

	@Test
	void possessRefusesBeyondTheCapAndAddsNothing() {
		for (int i = 0; i < BotManager.MAX_BOTS; i++) {
			assertTrue(newAccount(account(i)), "account " + account(i) + " is created");
			assertNotNull(BotManager.possess(account(i), PASSWORD), "possess " + account(i));
		}
		assertEquals(BotManager.MAX_BOTS, BotManager.count(), "exactly the cap is live");

		String overflow = account(BotManager.MAX_BOTS);
		assertTrue(newAccount(overflow), "the account itself can still be created");

		assertNull(BotManager.possess(overflow, PASSWORD), "the cap refuses the next possess");
		assertEquals(BotManager.MAX_BOTS, BotManager.count(), "and adds nothing");
	}

	@Test
	void everyPossessedBotIsFlaggedAndOccupiesItsOwnSlot() {
		for (int i = 0; i < 3; i++) {
			newAccount(account(i));
			assertNotNull(BotManager.possess(account(i), PASSWORD));
		}

		Set<Integer> slots = new HashSet<Integer>();
		for (BotPlayer bot : BotManager.all()) {
			assertTrue(bot.isBot, "flagged as a bot");
			assertTrue(bot.isActive, "live in the world");
			assertTrue(BotNames.hasBotPrefix(bot.playerName), "carries the reserved prefix");
			assertTrue(slots.add(bot.playerId), "each bot has a distinct slot");
			assertTrue(PlayerHandler.players[bot.playerId] == bot, "and is registered there");
		}
	}

	@Test
	void aNameAlreadyInTheWorldCannotBePossessedTwice() {
		assertTrue(newAccount("botdup"));
		assertNotNull(BotManager.possess("botdup", PASSWORD));

		assertNull(BotManager.possess("botdup", PASSWORD), "no second controller for one name");
		assertNotNull(PlayerHandler.getPlayer("botdup"), "the first possession still holds it");
	}

	@Test
	void possessMutateReleaseRoundTripsTheCharacter() {
		assertTrue(newAccount("botrt"));

		BotPlayer bot = BotManager.possess("botrt", PASSWORD);
		assertNotNull(bot);
		bot.playerRights = 2; // persisted as `character-rights`

		assertTrue(BotManager.release("botrt"), "release succeeds");
		assertNull(BotManager.get("botrt"), "and the bot is no longer live");
		assertTrue(Files.exists(characterFile("botrt")), "release writes the character");

		BotPlayer again = BotManager.possess("botrt", PASSWORD);
		assertNotNull(again, "the account can be re-possessed");
		assertEquals(2, again.playerRights, "the mutated state survived the round-trip");
	}

	@Test
	void aWrongPasswordIsRefused() {
		assertTrue(newAccount("botpw"));

		assertNull(BotManager.possess("botpw", "not-the-password"));
		assertEquals(0, BotManager.count(), "a refused possess leaves nothing live");
	}

	@Test
	void anUnknownAccountIsNotCreatedByPossess() {
		// Creation is createAccount's job, never a side effect of possession.
		assertNull(BotManager.possess("botghost", PASSWORD));
		assertFalse(Files.exists(characterFile("botghost")), "possess must not invent an account");
	}

	@Test
	void createAccountRefusesIllegalOrDuplicateNames() {
		assertFalse(BotManager.createAccount("[bot]x", PASSWORD), "punctuation is not login-legal");
		assertFalse(BotManager.createAccount("abcdefghijklm", PASSWORD), "13 chars exceeds the cap");
		assertFalse(BotManager.createAccount("", PASSWORD));

		assertTrue(newAccount("botonce"));
		assertFalse(BotManager.createAccount("botonce", PASSWORD), "an existing name is a possess, not a create");
	}

	@Test
	void releaseIsIdempotent() {
		assertFalse(BotManager.release("botnobody"), "releasing an unknown name is not an error");

		assertTrue(newAccount("botrel"));
		assertNotNull(BotManager.possess("botrel", PASSWORD));
		assertTrue(BotManager.release("botrel"));
		assertFalse(BotManager.release("botrel"), "the second release finds nothing");
	}
}
