package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Roadmap Phase E's acceptance criterion: <b>a bot is added by editing the config alone.</b>
 *
 * <p>{@link BotManager#apply} is driven directly, with a {@code BotsConfig.Result} built from lines
 * rather than read from a fixed path — the same seam {@link BotManager#start()} and
 * {@link BotManager#reload()} use, so this exercises the real path without depending on the process
 * working directory. The account files are real ({@code Data/characters/…}), because a bot is a real
 * character and the point is that possession goes down the login path.
 */
class BotProfileSpawnTest {

	private static final String PASSWORD = BotTestFixture.PASSWORD;
	private static final String NAME = "botealpha";
	private static final String SECOND = "botebeta";

	@AfterEach
	void tearDown() throws IOException {
		BotTestFixture.cleanUp(NAME, SECOND);
	}

	private static BotsConfig.Result config(String... lines) {
		return BotsConfig.parseLines(Arrays.asList(lines));
	}

	private static String characterFile(String account) {
		return "./Data/characters/" + account + ".txt";
	}

	@Test
	void aConfigLineSpawnsALiveBot() {
		int spawned = BotManager.apply(config("account " + NAME + " password " + PASSWORD
				+ " script gather_oak"));

		assertEquals(1, spawned);
		BotPlayer bot = BotManager.get(NAME);
		assertNotNull(bot, "the row is a live bot");
		assertNotNull(bot.controller(), "with its script attached, not just a possessed character");
		assertTrue(Files.exists(Paths.get(characterFile(NAME))),
				"and a real character file, because a bot is a real account");
		assertNotNull(BotManager.profileFor(NAME), "the row is remembered for ::bot list");
	}

	@Test
	void aDisabledRowSpawnsNothing() {
		int spawned = BotManager.apply(config("account " + NAME + " password " + PASSWORD
				+ " script gather_oak enabled false"));

		assertEquals(0, spawned);
		assertNull(BotManager.get(NAME));
		assertFalse(Files.exists(Paths.get(characterFile(NAME))), "a parked row is not even created");
	}

	@Test
	void anUnknownScriptIsRefusedBeforeAnAccountIsCreated() {
		int spawned = BotManager.apply(config("account " + NAME + " password " + PASSWORD
				+ " script no_such_script"));

		assertEquals(0, spawned);
		assertNull(BotManager.get(NAME));
		// The script is checked first on purpose: a typo must not leave a stray character behind.
		assertFalse(Files.exists(Paths.get(characterFile(NAME))),
				"a typo in the script must not create an account");
	}

	@Test
	void applyingTheSameConfigTwiceSpawnsOnce() {
		BotsConfig.Result result = config("account " + NAME + " password " + PASSWORD
				+ " script gather_oak");

		assertEquals(1, BotManager.apply(result));
		assertEquals(0, BotManager.apply(result), "spawn is idempotent: a reload must not double up");
		assertNotNull(BotManager.get(NAME));
	}

	@Test
	void aRowRemovedFromTheConfigIsReleased() {
		BotManager.apply(config(
				"account " + NAME + " password " + PASSWORD + " script gather_oak",
				"account " + SECOND + " password " + PASSWORD + " script gather_oak"));
		assertNotNull(BotManager.get(SECOND));

		int spawned = BotManager.apply(config("account " + NAME + " password " + PASSWORD
				+ " script gather_oak"));

		assertEquals(0, spawned, "nothing new was spawned");
		assertNotNull(BotManager.get(NAME), "the surviving row keeps running");
		assertNull(BotManager.get(SECOND), "the row that was removed is released");
	}

	@Test
	void aRowParkedWithEnabledFalseIsReleasedOnReload() {
		BotManager.apply(config("account " + NAME + " password " + PASSWORD + " script gather_oak"));

		BotManager.apply(config("account " + NAME + " password " + PASSWORD
				+ " script gather_oak enabled false"));

		assertNull(BotManager.get(NAME), "disabling a row stops the bot without deleting its credentials");
		assertNotNull(BotManager.profileFor(NAME), "and the row is still there to be enabled again");
	}

	@Test
	void anUnknownHomeIsAMessageRatherThanAFailedSpawn() {
		int spawned = BotManager.apply(config("account " + NAME + " password " + PASSWORD
				+ " script gather_oak home definitely_not_a_place"));

		assertEquals(1, spawned, "a bad home must not cost a running bot");
		assertNotNull(BotManager.get(NAME));
	}

	@Test
	void anEmptyConfigReleasesEverything() {
		BotManager.apply(config("account " + NAME + " password " + PASSWORD + " script gather_oak"));
		assertNotNull(BotManager.get(NAME));

		assertEquals(0, BotManager.apply(BotsConfig.parseLines(Collections.<String>emptyList())));
		assertNull(BotManager.get(NAME));
	}

	// ---- provisioning (BOT_ACCOUNTS.md §4.1) ---------------------------------------------------

	/**
	 * The gap this closes, stated as the thing an operator would actually have reported: a bot created
	 * from a config line used to own nothing, so a woodcutter had no axe and its {@code Gather} loop
	 * failed on the first click. The kit is granted at creation and read back through the save file, so
	 * this asserts on the possessed bot rather than on the file.
	 */
	@Test
	void aConfigSpawnedBotOwnsTheKitItsRowAsksFor() {
		BotManager.apply(config("account " + NAME + " password " + PASSWORD
				+ " script gather_oak profile woodcutter"));

		BotPlayer bot = BotManager.get(NAME);
		assertNotNull(bot);
		assertTrue(bot.getItems().playerHasItem(1351),
				"a woodcutter with no axe is a possessed character, not a bot that can chop");
	}

	@Test
	void aRowThatNamesNoProfileStillGetsAWorkingKit() {
		// The obvious config line — account, password, script — has to be sufficient. That is the whole
		// reason the default kit covers every gatherable resource rather than being merely a token grant.
		BotManager.apply(config("account " + NAME + " password " + PASSWORD + " script gather_oak"));

		BotPlayer bot = BotManager.get(NAME);
		assertNotNull(bot);
		assertTrue(bot.getItems().playerHasItem(1351), "the default kit includes an axe");
	}

	@Test
	void anUnknownProfileFallsBackToTheDefaultInsteadOfFailingTheSpawn() {
		int spawned = BotManager.apply(config("account " + NAME + " password " + PASSWORD
				+ " script gather_oak profile no_such_kit"));

		assertEquals(1, spawned, "a bad profile costs a leaner kit, not a dead bot — as with a bad home");
		assertTrue(BotManager.get(NAME).getItems().playerHasItem(1351));
	}

	@Test
	void theKitIsGrantedOnlyOnCreationNotOnEveryPossession() {
		// Provisioning runs at createAccount and nowhere else. If it also ran on possess, a bot would
		// accumulate a fresh axe every restart, which is exactly the kind of slow leak nobody notices
		// until a character's inventory is inexplicably full.
		BotManager.apply(config("account " + NAME + " password " + PASSWORD
				+ " script gather_oak profile woodcutter"));
		BotManager.release(NAME);
		int freeAfterFirst = BotManager.possess(NAME, PASSWORD).getItems().freeSlots();

		BotManager.release(NAME);
		BotPlayer second = BotManager.possess(NAME, PASSWORD);

		assertEquals(freeAfterFirst, second.getItems().freeSlots(),
				"a second possession must not grant the kit again");
	}
}
