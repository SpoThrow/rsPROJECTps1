package server.clip.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import core.util.Misc;

/**
 * Pins {@link Movement} to the collision primitive the live movement loop actually uses.
 *
 * <p>Two things are worth guarding here. First, the direction convention: {@code Movement}
 * must agree with {@code Misc.directionDeltaX/Y}, or a caller reasoning in compass
 * directions would silently walk the wrong way. Second, the delegation target:
 * {@code Movement} must stay a view over {@link SmartPathFinder#canStep} rather than
 * growing its own mask arithmetic, which is the exact duplication that
 * {@code WalkingCheck}/{@code ClipMap.bin} used to represent.
 *
 * <p>Region data is not loaded under the test task, so {@code Region.getClipping} returns 0
 * for every tile and everything reads as passable. These tests therefore assert
 * <em>consistency and contract</em>, not real-world geometry — the real-data check lives in
 * {@code Region.verifyClippingConsistency}, which runs at boot.
 */
class MovementTest {

	private static final int X = 3222;
	private static final int Y = 3218;
	private static final int Z = 0;

	@Test
	void directionConstantsAreZeroThroughSevenInCompassOrder() {
		assertEquals(0, Movement.NORTH);
		assertEquals(1, Movement.NORTH_EAST);
		assertEquals(2, Movement.EAST);
		assertEquals(3, Movement.SOUTH_EAST);
		assertEquals(4, Movement.SOUTH);
		assertEquals(5, Movement.SOUTH_WEST);
		assertEquals(6, Movement.WEST);
		assertEquals(7, Movement.NORTH_WEST);
	}

	@Test
	void deltasMatchMiscsDirectionTables() {
		for (int dir = Movement.NORTH; dir <= Movement.NORTH_WEST; dir++) {
			assertEquals((int) Misc.directionDeltaX[dir], Movement.deltaX(dir),
					"x delta mismatch for direction " + dir);
			assertEquals((int) Misc.directionDeltaY[dir], Movement.deltaY(dir),
					"y delta mismatch for direction " + dir);
		}
	}

	@Test
	void oppositeDirectionsCancelOut() {
		// North/South, North-East/South-West, etc. are four steps apart in the table.
		for (int dir = Movement.NORTH; dir <= Movement.NORTH_WEST; dir++) {
			int opposite = (dir + 4) & 7;
			assertEquals(0, Movement.deltaX(dir) + Movement.deltaX(opposite), "x, dir " + dir);
			assertEquals(0, Movement.deltaY(dir) + Movement.deltaY(opposite), "y, dir " + dir);
		}
	}

	@Test
	void cardinalsHaveExactlyOneNonZeroDeltaAndDiagonalsHaveTwo() {
		int[][] expected = {
				{ 0, 1 }, { 1, 1 }, { 1, 0 }, { 1, -1 },
				{ 0, -1 }, { -1, -1 }, { -1, 0 }, { -1, 1 },
		};
		for (int dir = Movement.NORTH; dir <= Movement.NORTH_WEST; dir++) {
			assertEquals(expected[dir][0], Movement.deltaX(dir), "x, dir " + dir);
			assertEquals(expected[dir][1], Movement.deltaY(dir), "y, dir " + dir);
		}
	}

	@Test
	void isBlockedDelegatesToTheMovementStepApi() {
		// If someone reimplements isBlocked with its own masks instead of delegating,
		// this diverges from the primitive that moves entities.
		for (int dir = Movement.NORTH; dir <= Movement.NORTH_WEST; dir++) {
			boolean expected = !SmartPathFinder.canStep(X, Y, Movement.deltaX(dir), Movement.deltaY(dir), Z);
			assertEquals(expected, Movement.isBlocked(X, Y, Z, dir), "dir " + dir);
		}
	}

	@Test
	void canWalkIsTheExactInverseOfIsBlocked() {
		for (int dir = Movement.NORTH; dir <= Movement.NORTH_WEST; dir++) {
			assertEquals(!Movement.isBlocked(X, Y, Z, dir), Movement.canWalk(X, Y, Z, dir), "dir " + dir);
		}
	}

	@Test
	void outOfRangeDirectionsFailLoudly() {
		// -1 is the usual "no direction" sentinel. Defaulting it to "not blocked" would
		// turn a caller's bug into entities walking through walls, so it must throw.
		for (int bad : new int[] { -1, 8, 9, -100, Integer.MIN_VALUE, Integer.MAX_VALUE }) {
			assertThrows(IllegalArgumentException.class, () -> Movement.isBlocked(X, Y, Z, bad),
					"isBlocked should reject direction " + bad);
			assertThrows(IllegalArgumentException.class, () -> Movement.canWalk(X, Y, Z, bad),
					"canWalk should reject direction " + bad);
			assertThrows(IllegalArgumentException.class, () -> Movement.deltaX(bad),
					"deltaX should reject direction " + bad);
			assertThrows(IllegalArgumentException.class, () -> Movement.deltaY(bad),
					"deltaY should reject direction " + bad);
		}
	}

	@Test
	void withNoRegionLoadedEverythingIsWalkableButTheContractStillHolds() {
		// Documents the test-environment precondition rather than asserting real terrain:
		// the interesting guarantee is that isBlocked and canWalk never disagree.
		assertFalse(Movement.isBlocked(X, Y, Z, Movement.NORTH));
		assertTrue(Movement.canWalk(X, Y, Z, Movement.NORTH));
	}
}
