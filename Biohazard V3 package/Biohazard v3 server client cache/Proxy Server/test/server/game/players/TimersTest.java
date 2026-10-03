package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * Pins the timers sub-slice after Phase 4.10 moved it off {@link Player} into {@link Timers}.
 *
 * <p>The whole cluster rests on one invariant — <em>every default is the {@code 0}
 * sentinel</em> — because fifteen of these fields are "last used" timestamps read as
 * {@code System.currentTimeMillis() - X > threshold}, so {@code 0} means "never used" and
 * the action is available immediately. {@code 0} is also the reset sentinel for
 * {@code teleBlockDelay} ("not teleblocked") and for the two counters, {@code clawDelay}
 * ("no claws active") and {@code ssDelay} ("Soul Split idle").
 * Rather than assert eighteen fields by name, the first test reflects over the class, so a
 * field added later with a non-zero default fails here.
 *
 * <p>The counters are then checked against the comparisons the real code uses, so that the
 * sentinel is tested by its <em>meaning</em> and not just its value.
 */
class TimersTest {

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void everyTimerDefaultIsTheZeroSentinel() throws Exception {
		final Timers timers = new Timers();
		int counted = 0;

		for (Field f : Timers.class.getDeclaredFields()) {
			if (Modifier.isStatic(f.getModifiers()) || f.isSynthetic()) {
				continue;
			}
			counted++;
			f.setAccessible(true);
			Object v = f.get(timers);
			String where = "Timers." + f.getName();
			if (v instanceof Long) {
				assertEquals(0L, v, where + " must default to the 0 sentinel");
			} else if (v instanceof Integer) {
				assertEquals(0, v, where + " must default to the 0 sentinel");
			} else if (v instanceof long[]) {
				long[] a = (long[]) v;
				assertEquals(6, a.length, where + " should have one slot per reduce spell");
				for (int i = 0; i < a.length; i++) {
					assertEquals(0L, a[i], where + "[" + i + "] must start zeroed");
				}
			} else {
				throw new AssertionError(where + " is an unexpected kind of field: " + v);
			}
		}

		// Guard against the loop passing vacuously if the cluster is ever emptied.
		assertEquals(18, counted, "the timers cluster should hold 18 fields");
	}

	@Test
	void aNewPlayerCanUseEveryDelayedActionImmediately() {
		final Client c = client();
		final Timers t = c.timers;
		long now = System.currentTimeMillis();

		// The exact comparisons each action uses, with the new player's 0s.
		assertTrue(now - t.foodDelay > 2000, "a new player must be able to eat at once");
		assertTrue(now - t.alchDelay > 1000, "a new player must be able to alch at once");
		assertTrue(now - t.buryDelay > 1500, "a new player must be able to bury at once");
		assertTrue(now - t.duelDelay > 800, "a new player must be free of duel cooldown");
		// Out of combat: the logout-hold condition in PlayerHandler must not hold them.
		assertTrue(now - t.logoutDelay > 10000, "a new player must not be held as in-combat");
		assertTrue(now - t.singleCombatDelay > 3300, "a new player must not be in single combat");
		assertTrue(now - t.singleCombatDelay2 > 3300, "a new player must not be in single combat");
		// Not teleblocked: now - teleBlockDelay must NOT be under teleBlockLength.
		assertFalse(now - t.teleBlockDelay < c.teleBlockLength, "a new player must not be teleblocked");
		// teleGrabDelay is deliberately not asserted: it is written in MagicOnFloorItems and read
		// nowhere, so the tele-grab cooldown is unimplemented and there is no comparison to make.
	}

	@Test
	void countdownsAreIdleForANewPlayer() {
		// clawDelay and ssDelay are countdowns guarded by `> 0`, not timestamps; 0 means "off".
		// ssDelay is Soul Split: set to 4 per hit, decremented in Curse.handleProcess().
		assertFalse(client().timers.clawDelay > 0, "claws must be off for a new player");
		assertFalse(client().timers.ssDelay > 0, "Soul Split must be idle for a new player");
	}

	@Test
	void theReduceSpellTableIsIndexableByReduceSpellId() {
		// Indices come from REDUCE_SPELLS, and the comparison uses REDUCE_SPELL_TIME.
		final Client c = client();
		assertEquals(c.REDUCE_SPELLS.length, c.timers.reduceSpellDelay.length,
				"one reduceSpellDelay slot per reduce spell");
		assertEquals(c.REDUCE_SPELL_TIME.length, c.timers.reduceSpellDelay.length,
				"one reduceSpellDelay slot per reduce-spell immunity time");
	}

	@Test
	void eachPlayerOwnsItsOwnTimersAndItsOwnReduceSpellArray() {
		final Client a = client();
		final Client b = client();

		assertSame(a.timers, a.timers, "the same player must keep one timers object");
		assertNotSame(a.timers, b.timers, "two players must not share timers");
		// A half-shared object would pass the check above, so the array is checked too.
		assertNotSame(a.timers.reduceSpellDelay, b.timers.reduceSpellDelay,
				"two players must not share the reduceSpellDelay array");

		a.timers.foodDelay = System.currentTimeMillis();
		a.timers.clawDelay = 2;
		a.timers.reduceSpellDelay[0] = 12345L;

		assertEquals(0L, b.timers.foodDelay, "delays leaked between players");
		assertEquals(0, b.timers.clawDelay, "the claw countdown leaked between players");
		assertEquals(0L, b.timers.reduceSpellDelay[0], "the reduce-spell table leaked between players");
	}
}
