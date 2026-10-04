package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the periodic save: it is driven from the game tick, writes at most one character per tick,
 * and — the property that matters most — <b>does not consume the logout's final write</b>.
 *
 * <p>That last one is the trap in this design. {@code Client.saveCharacterOnce()} latches a flag so
 * that logout, the update-kick and the shutdown hook cannot write the same character twice, and the
 * autosave must reuse it rather than latch it. If an autosave latched the flag, the logout that
 * followed would skip its save and the player would silently lose up to five minutes of play — a
 * bug that would look like "the last few minutes don't stick" and be very hard to trace. So there
 * is an explicit test that an autosaved character is <em>still</em> written in full on logout.
 *
 * <p>The clock is injected ({@code process(long)}), so no test waits five minutes.
 */
class PlayerSavingTest {

	private static final int SLOT = 1;
	private static final String NAME = "AutosaveChar";

	private static Path characterFile() {
		return Paths.get("./Data/characters/" + NAME + ".txt");
	}

	@BeforeEach
	void setUp() {
		PlayerSaving.reset(0);
	}

	@AfterEach
	void tearDown() throws IOException {
		PlayerHandler.players[SLOT] = null;
		Files.deleteIfExists(characterFile());
		Files.deleteIfExists(Paths.get("./Data/characters/" + NAME + ".txt.bak"));
		Files.deleteIfExists(Paths.get("./Data/characters/" + NAME + ".txt.tmp"));
	}

	/** A logged-in client — PlayerSave.saveGame refuses one that is not in the player array. */
	private static Client loggedIn(int x) {
		final Client c = new Client(null, SLOT);
		c.playerName = NAME;
		c.playerName2 = NAME;
		c.playerPass = "swordfish";
		c.saveFile = true;
		c.saveCharacter = true;
		c.newPlayer = false;
		c.isActive = true;
		c.position.absX = x;
		PlayerHandler.players[SLOT] = c;
		return c;
	}

	// ---------------------------------------------------------------- the interval

	@Test
	void nothingIsWrittenBeforeTheIntervalElapses() {
		loggedIn(3000);

		PlayerSaving.process(0);
		PlayerSaving.process(PlayerSaving.SAVE_INTERVAL_MS - 1);

		assertFalse(Files.exists(characterFile()), "no autosave before the interval has passed");
		assertFalse(PlayerHandler.players[SLOT].isCharacterSaved(),
				"an autosave must not latch the final-save flag");
	}

	@Test
	void theFirstSweepStartsOnceTheIntervalHasElapsed() {
		loggedIn(3000);

		PlayerSaving.process(0);
		assertFalse(Files.exists(characterFile()));

		PlayerSaving.process(PlayerSaving.SAVE_INTERVAL_MS);

		assertTrue(Files.exists(characterFile()), "the sweep must start when the interval elapses");
	}

	// ---------------------------------------------------------------- one write per tick

	@Test
	void onlyOneCharacterIsWrittenPerTick() {
		final Client a = loggedIn(3000);
		final Client b = new Client(null, SLOT + 1);
		b.playerName = "AutosaveCharB";
		b.playerName2 = "AutosaveCharB";
		b.playerPass = "swordfish";
		b.saveFile = true;
		b.saveCharacter = true;
		b.newPlayer = false;
		b.isActive = true;
		PlayerHandler.players[SLOT + 1] = b;

		try {
			PlayerSaving.process(PlayerSaving.SAVE_INTERVAL_MS);

			assertTrue(Files.exists(characterFile()), "the first player is written");
			assertFalse(Files.exists(Paths.get("./Data/characters/AutosaveCharB.txt")),
					"the second player must wait for the next tick, so the write cannot burst");

			PlayerSaving.process(PlayerSaving.SAVE_INTERVAL_MS);
			assertTrue(Files.exists(Paths.get("./Data/characters/AutosaveCharB.txt")),
					"the second tick completes the sweep");
		} finally {
			PlayerHandler.players[SLOT + 1] = null;
			try {
				Files.deleteIfExists(Paths.get("./Data/characters/AutosaveCharB.txt"));
			} catch (IOException ignored) {
				// best effort — the test's own assertions already ran
			}
		}
	}

	@Test
	void aCompletedSweepWaitsForTheNextInterval() {
		loggedIn(3000);

		// The first call after the interval begins the sweep and writes one character.
		PlayerSaving.process(PlayerSaving.SAVE_INTERVAL_MS);
		assertTrue(PlayerSaving.nextSlot() > 0, "the sweep is under way after writing one character");

		// Mid-sweep calls ignore the interval, so the remaining slots drain on the following ticks.
		// (Same timestamp, so the "sweep finished at" stamp the gate re-arms from is deterministic.)
		for (int i = 0; i < PlayerHandler.players.length && PlayerSaving.nextSlot() != 0; i++) {
			PlayerSaving.process(PlayerSaving.SAVE_INTERVAL_MS);
		}
		assertEquals(0, PlayerSaving.nextSlot(), "the sweep completed and is waiting again");

		// The interval has re-armed from the moment the sweep finished.
		PlayerSaving.process(PlayerSaving.SAVE_INTERVAL_MS + 1);
		assertEquals(0, PlayerSaving.nextSlot(), "a fresh interval has not elapsed yet");

		// ...and it does fire once it has.
		PlayerSaving.process(PlayerSaving.SAVE_INTERVAL_MS * 2);
		assertTrue(PlayerSaving.nextSlot() > 0, "the next sweep has started");
	}

	@Test
	void anEmptyWorldSweepsInstantlyAndDoesNotBusyLoop() {
		PlayerSaving.process(PlayerSaving.SAVE_INTERVAL_MS);

		assertEquals(0, PlayerSaving.nextSlot(),
				"with nobody online the sweep completes at once rather than advancing over ticks");
	}

	@Test
	void anInactiveSlotIsSkipped() {
		final Client c = loggedIn(3000);
		c.isActive = false;

		PlayerSaving.process(PlayerSaving.SAVE_INTERVAL_MS);

		assertFalse(Files.exists(characterFile()), "an inactive client is not autosaved");
		assertEquals(0, PlayerSaving.nextSlot(), "and is skipped without stalling the sweep");
	}

	// ---------------------------------------------------------------- the trap

	@Test
	void anAutosaveDoesNotConsumeTheFinalLogoutSave() throws IOException {
		// THE test for this design. The autosave writes the character, but it must leave the
		// once-guard untouched, so the logout still writes the newest state. If the autosave
		// latched the flag, the position below would never reach the file.
		final Client c = loggedIn(3000);

		PlayerSaving.process(PlayerSaving.SAVE_INTERVAL_MS);
		assertTrue(Files.exists(characterFile()), "the autosave wrote the character");
		assertTrue(Files.readString(characterFile(), StandardCharsets.UTF_8).contains("character-posx = 3000"),
				"and it wrote the state as it was");
		assertFalse(c.isCharacterSaved(), "the autosave must not latch the final-save flag");

		// The player walks on. The logout has to be the write that sticks.
		c.position.absX = 3456;
		assertTrue(c.saveCharacterOnce(), "the logout's final write must still happen");

		assertTrue(Files.readString(characterFile(), StandardCharsets.UTF_8).contains("character-posx = 3456"),
				"the final state must be what ends up on disk, not the autosaved snapshot");
	}

	@Test
	void theAutosaveWritesTheStateAsItIsNow() throws IOException {
		final Client c = loggedIn(3100);

		PlayerSaving.process(PlayerSaving.SAVE_INTERVAL_MS);

		assertTrue(Files.readString(characterFile(), StandardCharsets.UTF_8).contains("character-posx = 3100"),
				"the point of the autosave is that an early session is on disk, not only in memory");
	}
}
