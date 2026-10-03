package server.game.players.actions.objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the woodcutting object table against the switch cases it replaced.
 *
 * <p>Expected ids were extracted from the original {@code ActionHandler} switch by
 * pairing each {@code case N:} with the first argument of its
 * {@code startWoodcutting(c, M, ...)} call, not retyped from the new table.
 */
class WoodcuttingObjectsTest {

	private static final int[] EXPECTED = {
			1276, 1278, 1286, 1281, 1308, 5552, 1307, 1309, 1306, 5551, 5553,
	};

	@Test
	void tableMatchesTheSwitchItReplaced() {
		assertArrayEquals(EXPECTED, WoodcuttingObjects.ids());
	}

	@Test
	void everyTreeIsRegisteredOnFirstClick() {
		for (int tree = 0; tree < EXPECTED.length; tree++) {
			assertTrue(ObjectHandler.isRegistered(EXPECTED[tree], ObjectClick.FIRST),
					"tree type " + tree + " (object " + EXPECTED[tree] + ") has no handler");
		}
	}

	@Test
	void treesAreNotClaimedOnOtherClicks() {
		// Woodcutting is first-click only; claiming second/third would shadow whatever
		// the switch does for those.
		for (int id : EXPECTED) {
			assertEquals(false, ObjectHandler.isRegistered(id, ObjectClick.SECOND));
			assertEquals(false, ObjectHandler.isRegistered(id, ObjectClick.THIRD));
		}
	}
}
