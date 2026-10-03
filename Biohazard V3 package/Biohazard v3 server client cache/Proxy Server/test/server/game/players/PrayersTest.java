package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the prayer/curse state after §4.12 moved it off {@link Player} into {@link Prayers}.
 *
 * <p>Two defaults here are <em>not</em> zero and both matter: {@code prayerPoint} starts at
 * {@code 1.0} because the drain wraps it as {@code 1.0 + prayerPoint} (so {@code 1.0} is the
 * unit modulus, and {@code 0} would deduct a point on the first tick), and {@code prayerId}
 * starts at {@code -1} ("none"). The tests pin both.
 *
 * <p>The arrays are pinned against the lookup tables that stayed on {@link Player}, because the
 * index spaces are shared across the two classes now: a length change on either side is an
 * out-of-bounds waiting to happen and must fail here rather than at runtime.
 */
class PrayersTest {

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void aFreshPlayersDefaultsAreTheLoadBearingOnes() {
		final Prayers p = new Prayers();

		// Not zero: the fractional drain accumulator and the "no prayer selected" id.
		assertEquals(1.0, p.prayerPoint, 0.0, "prayerPoint's 1.0 is the drain unit modulus");
		assertEquals(-1, p.prayerId, "prayerId's -1 means 'none'");

		// Zero / false for the rest.
		assertEquals(0L, p.stopPrayerDelay, "no prayer toggle has happened yet");
		assertFalse(p.usingPrayer, "a fresh player is not using a prayer");

		// Nothing is switched on.
		for (int i = 0; i < p.prayerActive.length; i++) {
			assertFalse(p.prayerActive[i], "prayerActive[" + i + "] must start off");
		}
		for (int i = 0; i < p.curseActive.length; i++) {
			assertFalse(p.curseActive[i], "curseActive[" + i + "] must start off");
		}
	}

	@Test
	void theIndexSpacesStillMatchTheLookupTablesOnPlayer() {
		final Client c = client();

		// These tables deliberately stayed on Player, so the coupling is now cross-class.
		assertEquals(c.PRAYER_GLOW.length, c.prayers.prayerActive.length,
				"prayerActive is indexed by the PRAYER_* tables");
		assertEquals(c.PRAYER_DRAIN_RATE.length, c.prayers.prayerActive.length,
				"prayerActive is drained via PRAYER_DRAIN_RATE");
		assertEquals(c.PRAYER_LEVEL_REQUIRED.length, c.prayers.prayerActive.length,
				"prayerActive is gated by PRAYER_LEVEL_REQUIRED");
		assertEquals(c.PRAYER_NAME.length, c.prayers.prayerActive.length, "one flag per prayer name");

		assertEquals(c.CURSE_GLOW.length, c.prayers.curseActive.length,
				"curseActive is indexed by the CURSE_* tables");
		assertEquals(c.CURSE_DRAIN.length, c.prayers.curseActive.length,
				"curseActive is drained via CURSE_DRAIN");
		assertEquals(c.CURSE_LEVEL_REQUIRED.length, c.prayers.curseActive.length,
				"curseActive is gated by CURSE_LEVEL_REQUIRED");
		assertEquals(c.CURSE_NAME.length, c.prayers.curseActive.length, "one flag per curse name");
	}

	@Test
	void aFreshPlayerMayToggleAPrayerImmediately() {
		// CombatAssistant/Curse block a re-toggle while now - stopPrayerDelay < 5000.
		final Prayers p = client().prayers;
		assertTrue(System.currentTimeMillis() - p.stopPrayerDelay >= 5000,
				"a fresh player must not be blocked from toggling a prayer");
	}

	@Test
	void eachPlayerOwnsItsOwnPrayerStateAndArrays() {
		final Client a = client();
		final Client b = client();

		assertSame(a.prayers, a.prayers, "the same player must keep one prayer state");
		assertNotSame(a.prayers, b.prayers, "two players must not share prayer state");
		// A half-shared object would pass the check above, so the arrays are checked too.
		assertNotSame(a.prayers.prayerActive, b.prayers.prayerActive,
				"two players must not share the prayerActive array");
		assertNotSame(a.prayers.curseActive, b.prayers.curseActive,
				"two players must not share the curseActive array");

		a.prayers.prayerActive[10] = true;
		a.prayers.curseActive[18] = true;
		a.prayers.prayerPoint = 0.5;
		a.prayers.usingPrayer = true;

		assertFalse(b.prayers.prayerActive[10], "prayer flags leaked between players");
		assertFalse(b.prayers.curseActive[18], "curse flags leaked between players");
		assertEquals(1.0, b.prayers.prayerPoint, 0.0, "the drain accumulator leaked between players");
		assertFalse(b.prayers.usingPrayer, "usingPrayer leaked between players");
	}
}
