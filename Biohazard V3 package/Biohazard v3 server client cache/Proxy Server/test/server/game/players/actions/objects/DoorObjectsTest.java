package server.game.players.actions.objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Pins the door table lifted out of {@code firstClickObject}, including the two gate ids
 * that used to reach it by falling through.
 *
 * <p>{@code 1516} and {@code 1519} are why this family could not migrate as a plain id
 * list. In the switch they had an {@code objectY == 9698} branch of their own and then
 * fell through into the eleven labels beneath it, so a handler that only called
 * {@code doorHandling} would have dropped the walk-around-the-gate case. Both halves now
 * live in {@link DoorObjects}.
 *
 * <p>Deleting those switch cases without carrying both halves would have been worse than
 * losing a branch: the next label in the switch was {@code case 9319}, which moves the
 * player up a height level.
 */
class DoorObjectsTest {

	private static final int[] EXPECTED_DOORS = {
			1530, 1531, 1533, 1534, 11712, 11711, 11707, 11708, 6725, 3198, 3197,
	};

	private static final int[] EXPECTED_GATES = { 1516, 1519 };

	@Test
	void tablesMatchTheSwitchTheyReplaced() {
		assertArrayEquals(EXPECTED_DOORS, DoorObjects.ids());
		assertArrayEquals(EXPECTED_GATES, DoorObjects.gates());
	}

	@Test
	void everyDoorAndGateIsClaimedOnFirstClick() {
		// Being claimed is what stops the switch running. These ids no longer have cases
		// in it, so an unclaimed one would land in default: handleGenericObject.
		for (int id : EXPECTED_DOORS) {
			assertTrue(ObjectHandler.isRegistered(id, ObjectClick.FIRST), "door " + id);
		}
		for (int id : EXPECTED_GATES) {
			assertTrue(ObjectHandler.isRegistered(id, ObjectClick.FIRST), "gate " + id);
		}
	}

	@Test
	void noneOfThemClaimAnotherClick() {
		// These were first-click cases only. Claiming a second or third click would
		// shadow a different action for the same object id.
		for (int id : EXPECTED_DOORS) {
			assertFalse(ObjectHandler.isRegistered(id, ObjectClick.SECOND), "second " + id);
			assertFalse(ObjectHandler.isRegistered(id, ObjectClick.THIRD), "third " + id);
		}
		for (int id : EXPECTED_GATES) {
			assertFalse(ObjectHandler.isRegistered(id, ObjectClick.SECOND), "second " + id);
			assertFalse(ObjectHandler.isRegistered(id, ObjectClick.THIRD), "third " + id);
		}
	}

	@Test
	void gatesAreNotAlsoInThePlainDoorTable() {
		// Both loops register on FIRST click, so an id present in both lists would make
		// ObjectHandler's static block throw during class-load and take every object
		// family down with it. Asserting it here keeps that failure legible.
		Set<Integer> doors = new HashSet<>();
		for (int id : EXPECTED_DOORS) {
			doors.add(id);
		}
		for (int id : EXPECTED_GATES) {
			assertFalse(doors.contains(id), "gate " + id + " is also in the plain door table");
		}
	}

	@Test
	void theCastleWarsDoorCasesDeletedAlongsideTheseAreNotClaimedHere() {
		// Six Castle Wars door cases went with the door block. They were unreachable:
		// firstClickObject's preamble returns whenever CastleWarObjects.handleObject does,
		// and handleObject returns true on every path for all six. 4423/4424/4427/4428 are
		// claimed on second click by CastleWarsDoorObjects, which is a separate action.
		for (int id : new int[] { 4467, 4465, 4427, 4428, 4424, 4423 }) {
			assertFalse(ObjectHandler.isRegistered(id, ObjectClick.FIRST), "first " + id);
		}
	}
}
