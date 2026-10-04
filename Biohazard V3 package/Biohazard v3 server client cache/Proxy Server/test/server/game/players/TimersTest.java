package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pins the timer bag after §4.10 (the availability clocks) and §4.11 (the duration/scheduling
 * group) moved it off {@link Player} into {@link Timers}.
 *
 * <p>The bag rests on one invariant — <em>every default is the {@code 0} sentinel</em> —
 * because most of these fields are either "last used" stamps read as
 * {@code System.currentTimeMillis() - X > threshold} ({@code 0} = "never used", so the action
 * is available immediately) or countdowns read as {@code > 0} ({@code 0} = "inactive").
 * {@code 0} is also the reset sentinel for {@code teleBlockDelay} ("not teleblocked").
 * ⚠️ There is exactly one deliberate exception, {@code freezeTimer = -6}: the freeze code tells
 * thawed states apart by exact negative value, so {@code -6} is a re-freeze immunity window
 * rather than a stand-in for zero. The first test reflects over the class, pins {@code 0} for
 * everything and pins that exception by name and value — so a field added later with an
 * unintended default fails there.
 *
 * <p>The countdowns are then checked against the comparisons the real code uses, so that the
 * sentinel is tested by its <em>meaning</em> and not just its value.
 */
class TimersTest {

	private static Client client() {
		return new Client(null, 1);
	}

	/**
	 * The only fields whose default is not the {@code 0} sentinel, with why.
	 * {@code freezeTimer}'s {@code -6} is a re-freeze immunity value the freeze code tests for
	 * exactly ({@code > -6}, {@code <= -3}, {@code < -4}), so it must not be "tidied" to 0.
	 */
	private static final Map<String, Object> NON_ZERO_DEFAULTS = Map.of("freezeTimer", -6);

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
			if (NON_ZERO_DEFAULTS.containsKey(f.getName())) {
				assertEquals(NON_ZERO_DEFAULTS.get(f.getName()), v,
						where + " has a load-bearing non-zero default — see the class note");
			} else if (v instanceof Long) {
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

		// Guard against the loop passing vacuously if the bag is ever emptied.
		assertEquals(28, counted, "the timer bag should hold 28 fields");

		// The exception is pinned by value, not merely excused.
		assertEquals(-6, timers.freezeTimer, "freezeTimer's -6 is a re-freeze immunity window");
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
		assertFalse(now - t.teleBlockDelay < t.teleBlockLength, "a new player must not be teleblocked");
		// teleGrabDelay no longer exists: it was written in MagicOnFloorItems and read nowhere,
		// so the write-only field was deleted rather than left as state wired to nothing.
	}

	@Test
	void countdownsAreIdleForANewPlayer() {
		// clawDelay and ssDelay are countdowns guarded by `> 0`, not timestamps; 0 means "off".
		// ssDelay is Soul Split: set to 4 per hit, decremented in Curse.handleProcess().
		assertFalse(client().timers.clawDelay > 0, "claws must be off for a new player");
		assertFalse(client().timers.ssDelay > 0, "Soul Split must be idle for a new player");
	}

	@Test
	void theDurationGroupIsInertForANewPlayer() {
		final Timers t = client().timers;

		// freezeTimer carries the bag's one non-zero default, and its *meaning* is "not frozen".
		assertFalse(t.freezeTimer > 0, "a new player must not be frozen");
		// The countdowns that gate actions are all inactive, and attackTimer means "ready".
		assertFalse(t.teleTimer > 0, "no teleport should be running");
		assertFalse(t.hitDelay > 0, "no hit should be queued");
		assertEquals(0, t.attackTimer, "a new player may attack immediately");
		assertEquals(0, t.delayedDamage, "no damage should be queued");
		assertEquals(0, t.delayedDamage2, "no second hit should be queued");
		assertFalse(t.freezeDelay > 0, "no ice spell should be in flight");
		assertEquals(0, t.teleBlockLength, "a new player has no teleblock duration");
		// restoreStatsDelay is an availability stamp after all (now - it > 60000 in Client).
		assertTrue(System.currentTimeMillis() - t.restoreStatsDelay > 60000,
				"a new player should be due a stat restore");
		// respawnTimer's *idle* sentinel is -6, but its declared default is 0, so a fresh player
		// walks 0 -> -6 over his first ticks. That transient is pinned as documented behaviour.
		assertEquals(0, t.respawnTimer, "respawnTimer's declared default is 0, not the -6 idle value");
		assertTrue(t.respawnTimer > -6, "so a fresh player is still counting down toward -6");
		// saveTimer no longer exists: it was written once in Client and read nowhere, so the
		// write-only field was deleted. NOTE (Phase 5): the reason first recorded here — "periodic
		// saving is handled by PlayerSaving" — was wrong; PlayerSaving.initialize() is never
		// called, so no periodic save runs at all. The deletion is still behaviour-neutral because
		// the field was never read, but a character reaches disk only on logout or on shutdown.
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
		a.timers.hitDelay = 3;
		a.timers.freezeTimer = 30;
		a.timers.skullTimer = 100;

		assertEquals(0L, b.timers.foodDelay, "delays leaked between players");
		assertEquals(0, b.timers.clawDelay, "the claw countdown leaked between players");
		assertEquals(0L, b.timers.reduceSpellDelay[0], "the reduce-spell table leaked between players");
		assertEquals(0, b.timers.hitDelay, "the hit countdown leaked between players");
		assertEquals(-6, b.timers.freezeTimer, "the freeze countdown leaked between players");
		assertEquals(0, b.timers.skullTimer, "the skull countdown leaked between players");
	}
}
