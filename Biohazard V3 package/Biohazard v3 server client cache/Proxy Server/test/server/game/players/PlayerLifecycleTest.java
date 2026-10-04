package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import server.util.ConnectionPool;

/**
 * Covers Phase 5's player lifecycle: a character is written <em>exactly once</em> on the way out,
 * however the player leaves — a normal logout, an update-kick, or the shutdown hook.
 *
 * <p><b>The three defects this pins.</b> (1) The logout path in {@code PlayerHandler.process()}
 * called {@code PlayerSave.saveGame(o)} and then {@code removePlayer(o)}, which ran
 * {@code Client.destruct()}, which saved <em>again</em> — so every normal logout wrote the
 * character file twice, and the second write overwrote the first for no reason. (2)
 * {@code Client.destruct()} opened with {@code if (underAttackBy > 0 || underAttackBy2 > 0)
 * return;}, so a player who was in combat when {@code kickAllPlayers} fired (the update-kick
 * path, which bypasses the ten-second out-of-combat hold) was dropped from the player array with
 * <em>no character write at all</em> and a leaked socket. (3) Nothing registered a JVM shutdown
 * hook, so Ctrl+C lost every change since each player last logged out.
 *
 * <p>The hiscores write that {@code destruct()} also performs is pointed at a refused loopback
 * port, so these tests need no database and do not depend on whatever MySQL the machine has.
 */
class PlayerLifecycleTest {

	private static final int SLOT_A = 1;
	private static final int SLOT_B = 2;
	private static final int SLOT_C = 3;

	/** Nothing listens on port 1, so a connect is refused immediately and no MySQL is needed. */
	private static final String DEAD_URL = "jdbc:mysql://127.0.0.1:1/nonexistent";

	/** Abort quickly instead of retrying for a minute if a test leaves the pool backing off. */
	private final ConnectionPool deadPool = new ConnectionPool(DEAD_URL, "user", "password", 1);

	@BeforeEach
	void setUp() {
		HiscoresHandler.usePool(deadPool);
	}

	@AfterEach
	void tearDown() throws IOException {
		HiscoresHandler.usePool(null);
		deadPool.close();
		PlayerHandler.players[SLOT_A] = null;
		PlayerHandler.players[SLOT_B] = null;
		PlayerHandler.players[SLOT_C] = null;
		for (String name : new String[] { "Alpha", "Beta", "Gamma" }) {
			Files.deleteIfExists(characterFile(name));
		}
	}

	private static Path characterFile(String name) {
		return Paths.get("./Data/characters/" + name + ".txt");
	}

	private static Client client(String name, int slot) {
		final Client c = new Client(null, slot);
		c.playerName = name;
		c.playerName2 = name;
		c.playerPass = "swordfish";
		c.saveFile = true;
		c.saveCharacter = true;
		c.newPlayer = false;
		c.isActive = true;
		// PlayerSave.saveGame refuses a client that is not registered in the player array (its
		// "must actually be logged in" precondition), so every fixture has to be logged in.
		PlayerHandler.players[slot] = c;
		return c;
	}

	// ---------------------------------------------------------------- exactly once

	@Test
	void savingACharacterIsExactlyOnce() {
		final Client c = client("Alpha", SLOT_A);

		assertFalse(c.isCharacterSaved(), "a fresh client has written nothing");

		assertTrue(c.saveCharacterOnce(), "the first save must reach the disk");
		assertTrue(c.isCharacterSaved(), "the client now knows it has been written");
		assertTrue(Files.exists(characterFile("Alpha")), "the character file must exist");

		assertFalse(c.saveCharacterOnce(), "a second save must be refused, not repeated");
	}

	@Test
	void savingEveryoneWritesEachCharacterOnce() {
		client("Alpha", SLOT_A);
		client("Beta", SLOT_B);
		client("Gamma", SLOT_C);

		assertEquals(3, PlayerHandler.saveAllPlayers(), "every logged-in character is written");
		assertTrue(Files.exists(characterFile("Alpha")));
		assertTrue(Files.exists(characterFile("Beta")));
		assertTrue(Files.exists(characterFile("Gamma")));

		assertEquals(0, PlayerHandler.saveAllPlayers(),
				"a second sweep must write nothing — this is the shutdown hook's contract");
	}

	@Test
	void anEmptyWorldSavesNobody() {
		assertEquals(0, PlayerHandler.saveAllPlayers(), "no players is not an error");
	}

	// ---------------------------------------------------------------- the destruct guards

	@Test
	void aDestroyedPlayerWithNoSessionIsStillSaved() {
		// destruct() used to open with `if (session == null) return;`. A client with no session is
		// exactly what a half-finished login looks like, and the early return skipped the world
		// cleanup, the save and the socket close in one line.
		final Client c = client("Alpha", SLOT_A);

		c.destruct();

		assertTrue(c.isCharacterSaved(), "a sessionless client must still be written out");
		assertTrue(Files.exists(characterFile("Alpha")));
	}

	@Test
	void aPlayerUnderAttackIsStillSavedWhenDestroyed() {
		// The data-loss bug. `underAttackBy > 0` made destruct() a no-op, so the update-kick path
		// — which sets kickAllPlayers and so bypasses the out-of-combat hold — dropped a fighting
		// player with no save and no socket close.
		final Client c = client("Alpha", SLOT_A);
		c.targeting.underAttackBy = 5;

		c.destruct();

		assertTrue(c.isCharacterSaved(),
				"being in combat must not cost the player his character");
		assertTrue(Files.exists(characterFile("Alpha")));
	}

	@Test
	void destroyingTwiceSavesOnceAndDoesNotThrow() {
		final Client c = client("Alpha", SLOT_A);

		c.destruct();
		c.destruct();

		assertTrue(c.isCharacterSaved());
		assertFalse(c.saveCharacterOnce(), "still exactly one write across both destructs");
	}

	@Test
	void aDestroyedPlayerIsStillWrittenOnceWhenEveryoneIsSaved() {
		// The shutdown hook has to be safe to run after a logout has already happened: the
		// character is already on disk, so the sweep must not write it a second time.
		final Client c = client("Alpha", SLOT_A);
		c.destruct();

		assertEquals(0, PlayerHandler.saveAllPlayers(),
				"a character already written must not be written again by the shutdown sweep");
	}

	// ---------------------------------------------------------------- one lifecycle, not three

	@Test
	void theTwoNeverRegisteredShutdownHooksAreGone() {
		// `server.ShutdownHook` and `core.util.ShutDownHook` both existed and neither was ever
		// passed to Runtime.addShutdownHook — the only reference to either was a commented-out
		// line in Server.main. Two half-wired lifecycles plus the real one in Server is worse than
		// one, so they were deleted rather than repaired. If either name comes back, this fails
		// and whoever added it can say which of the three is authoritative.
		assertThrows(ClassNotFoundException.class, () -> Class.forName("server.ShutdownHook"));
		assertThrows(ClassNotFoundException.class, () -> Class.forName("core.util.ShutDownHook"));
	}

	// ---------------------------------------------------------------- the per-player tick order

	@Test
	void thePerPlayerTickRunsInItsDocumentedOrder() {
		final List<String> order = new ArrayList<>();
		final RecordingClient player = new RecordingClient(SLOT_A, order);
		player.playerName = "Alpha";
		PlayerHandler.players[SLOT_A] = player;

		new PlayerHandler().process();

		assertEquals(List.of(
				// Pass 1 — the per-player tick.
				"preProcessing",           // 1. timers, poison, prayer drain
				"processQueuedPackets",    // 2. drain this client's I/O queue
				"process",                 // 3. hits, skills, actions
				"postProcessing",          // 4. reconcile the walk queue
				"getNextPlayerMovement",   // 5. merge walk, then step
				"processCombatAfterMovement", // 6. follow and swing, from the new position
				// Pass 2 — initialise or publish.
				"initialize",
				// Tail — clear the update flags for the next tick.
				"clearUpdateFlags"), order,
				"a reordering here is invisible to the compiler but changes what the client sees");
	}

	/**
	 * Records the order of the per-player hooks instead of running them. The bodies are all
	 * deliberately <em>not</em> called: this test is about the sequence, and the real ones send
	 * packets and walk the player.
	 */
	private static final class RecordingClient extends Client {

		private final List<String> order;

		RecordingClient(int slot, List<String> order) {
			super(null, slot);
			this.order = order;
			this.isActive = true;
		}

		@Override
		public void preProcessing() {
			order.add("preProcessing");
		}

		@Override
		public boolean processQueuedPackets() {
			order.add("processQueuedPackets");
			return false; // stop the drain loop after one look, as an empty queue does
		}

		@Override
		public void process() {
			order.add("process");
		}

		@Override
		public void postProcessing() {
			order.add("postProcessing");
		}

		@Override
		public void getNextPlayerMovement() {
			order.add("getNextPlayerMovement");
		}

		@Override
		public void processCombatAfterMovement() {
			order.add("processCombatAfterMovement");
		}

		@Override
		public void initialize() {
			order.add("initialize");
		}

		@Override
		public void update() {
			order.add("update");
		}

		@Override
		public void clearUpdateFlags() {
			order.add("clearUpdateFlags");
		}
	}
}
