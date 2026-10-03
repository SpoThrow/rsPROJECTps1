package server.game.players.actions.objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the dwarf cannon object ids migrated out of {@code ActionHandler}.
 */
class DwarfCannonObjectsTest {

	private static final int[] EXPECTED = { 6, 7, 8, 9 };

	@Test
	void tableMatchesTheSwitchItReplaced() {
		assertArrayEquals(EXPECTED, DwarfCannonObjects.ids());
	}

	@Test
	void everyCannonObjectIsRegisteredOnFirstClick() {
		for (int id : EXPECTED) {
			assertTrue(ObjectHandler.isRegistered(id, ObjectClick.FIRST),
					"cannon object " + id + " has no handler");
		}
	}

	@Test
	void cannonObjectsAreAlsoClaimedOnSecondClick() {
		// Second click picks the cannon up. Third click also has cannon handling, but
		// it goes through DwarfCannon.isCannonObject in ActionHandler, not this map.
		for (int id : EXPECTED) {
			assertTrue(ObjectHandler.isRegistered(id, ObjectClick.SECOND),
					"cannon object " + id + " should be picked up on second click");
			assertEquals(false, ObjectHandler.isRegistered(id, ObjectClick.THIRD));
		}
	}
}
