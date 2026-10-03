package server.game.players.actions.objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the Castle Wars ids whose click is owned by {@code firstClickObject}'s preamble.
 *
 * <p>These used to also sit in the switch, calling {@code CastleWarObjects.handleObject}
 * a second time — and because that method's movement branches read the player's current
 * position, the second call could move the player somewhere different from the first.
 * Registering them here as no-ops is what stops the switch running.
 */
class CastleWarsObjectClicksTest {

	private static final int[] EXPECTED = {
			4411, 4415, 4417, 4418, 4420, 4469, 4470, 4419, 4911, 4912, 1747, 1757, 4437,
			6281, 6280, 4472, 4471, 4406, 4407, 4458, 4902, 4903, 4900, 4901, 4461, 4463,
			4464, 4377, 4378,
	};

	@Test
	void tableMatchesTheSwitchItReplaced() {
		assertArrayEquals(EXPECTED, CastleWarsObjectClicks.ids());
	}

	@Test
	void everyCastleWarsObjectIsClaimedOnFirstClick() {
		// Being claimed is the point: dispatch returning true is what keeps the switch
		// from calling handleObject a second time.
		for (int id : EXPECTED) {
			assertTrue(ObjectHandler.isRegistered(id, ObjectClick.FIRST),
					"castle wars object " + id + " is not claimed, so the switch will run");
		}
	}

	@Test
	void noneOfThemClaimAnotherClick() {
		for (int id : EXPECTED) {
			assertEquals(false, ObjectHandler.isRegistered(id, ObjectClick.SECOND), "second " + id);
			assertEquals(false, ObjectHandler.isRegistered(id, ObjectClick.THIRD), "third " + id);
		}
	}

	@Test
	void theMissingEndOfTheGroupIsNotClaimed() {
		// 1568 sat after the fall-through and was removed with the group: handleObject's
		// own case 1568 returns true unconditionally, so the preamble already returns for
		// id 1568 and its switch case could never run.
		assertEquals(false, ObjectHandler.isRegistered(1568, ObjectClick.FIRST));
	}
}
