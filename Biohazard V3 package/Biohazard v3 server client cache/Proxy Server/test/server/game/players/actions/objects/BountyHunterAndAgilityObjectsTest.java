package server.game.players.actions.objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Pins the bounty hunter crater objects and the gnome agility obstacles lifted out of
 * {@code ActionHandler}.
 */
class BountyHunterAndAgilityObjectsTest {

	private static final int[] ENTRANCES = { 28119, 28120, 28121 };
	private static final int CRATER_EXIT = 28122;

	private static final int[][] GATED = {
			{ 5088, 30, 2687, 9506, 0 },
			{ 5090, 30, 2682, 9506, 0 },
			{ 5110, 12, 2647, 9557, 0 },
			{ 5111, 12, 2649, 9562, 0 },
	};

	private static final int[] PUSH_X = { 5103, 5105, 5106, 5107 };
	private static final int PUSH_Y = 5104;

	@Test
	void bountyHunterEntrancesAndExitMatchTheSwitch() {
		assertArrayEquals(ENTRANCES, BountyHunterObjects.entrances());
		assertEquals(CRATER_EXIT, BountyHunterObjects.exit());
	}

	@Test
	void everyBountyHunterObjectIsRegistered() {
		for (int id : ENTRANCES) {
			assertTrue(ObjectHandler.isRegistered(id, ObjectClick.FIRST), "entrance " + id);
		}
		assertTrue(ObjectHandler.isRegistered(CRATER_EXIT, ObjectClick.FIRST), "crater exit");
	}

	@Test
	void agilityTablesMatchTheSwitch() {
		int[][] actual = AgilityObjects.gated();
		assertEquals(GATED.length, actual.length);
		for (int i = 0; i < GATED.length; i++) {
			assertArrayEquals(GATED[i], actual[i], "gated obstacle row " + i);
		}
		assertArrayEquals(PUSH_X, AgilityObjects.pushAlongX());
		assertEquals(PUSH_Y, AgilityObjects.pushAlongY());
	}

	@Test
	void everyAgilityObstacleIsRegistered() {
		for (int[] row : GATED) {
			assertTrue(ObjectHandler.isRegistered(row[0], ObjectClick.FIRST), "gated " + row[0]);
		}
		for (int id : PUSH_X) {
			assertTrue(ObjectHandler.isRegistered(id, ObjectClick.FIRST), "push-x " + id);
		}
		assertTrue(ObjectHandler.isRegistered(PUSH_Y, ObjectClick.FIRST), "push-y " + PUSH_Y);
	}

	@Test
	void noObstacleIsRegisteredTwice() {
		Set<Integer> seen = new HashSet<>();
		for (int[] row : GATED) {
			assertTrue(seen.add(row[0]), "gated " + row[0] + " appears twice");
		}
		for (int id : PUSH_X) {
			assertTrue(seen.add(id), "push-x " + id + " appears twice");
		}
		assertTrue(seen.add(PUSH_Y), "push-y " + PUSH_Y + " appears twice");
	}
}
