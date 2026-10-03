package server.game.players.packets.buttons;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the prayer button table.
 *
 * <p>The mapping is positional: {@code IDS[prayerIndex]} must be the button that
 * activates that prayer. Getting one wrong makes a player toggle the wrong prayer,
 * which is worse than a dead button because it looks like it worked.
 *
 * <p>The expected ids were extracted from the switch by pairing each {@code case N:}
 * with the argument of its {@code activatePrayer(M)} call, not retyped from the new
 * table.
 */
class PrayerButtonsTest {

	private static final int[] EXPECTED = {
			21233, 21234, 21235, 70080, 70082, 21236, 21237, 21238, 21239, 21240,
			21241, 70084, 70086, 21242, 21243, 21244, 21245, 21246, 21247, 70088,
			70090, 2171, 2172, 2173, 70092, 70094,
	};

	@Test
	void tableMatchesTheSwitchItReplaced() {
		assertArrayEquals(EXPECTED, PrayerButtons.ids());
	}

	@Test
	void everyPrayerButtonIsRegistered() {
		for (int prayer = 0; prayer < EXPECTED.length; prayer++) {
			int id = EXPECTED[prayer];
			assertTrue(ButtonHandler.isRegistered(id),
					"prayer " + prayer + " (button " + id + ") has no handler");
		}
	}

	@Test
	void theHighestIndexesAreChivalryAndPiety() {
		// Named because these two are the ones players notice; if the table ever
		// shifts, it is most likely to shift here.
		assertEquals(26, PrayerButtons.ids().length);
		assertEquals(70092, PrayerButtons.ids()[24], "24 is chivalry");
		assertEquals(70094, PrayerButtons.ids()[25], "25 is piety");
	}

	@Test
	void curseButtonsAreDeliberatelyNotInThisRegistry() {
		// Curses are claimed by handleCurseButton in ClickingButtons' preamble, by
		// range, which returns before dispatch is ever reached. Registering them here
		// would be unreachable code.
		for (int id : new int[] { 87231, 87255, 88001, 88013, 22503 }) {
			assertFalse(ButtonHandler.isRegistered(id), "curse button " + id + " is handled elsewhere");
		}
	}
}
