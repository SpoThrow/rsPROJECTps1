package server.game.players.actions.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Pins the rock-prospect table.
 *
 * <p>The names are what the player reads, and they are parallel to the id rows, so a
 * row inserted or removed in one array but not the other would prospect the wrong
 * rock. The expected table is written as name-to-ids pairs and compared both ways.
 */
class MiningProspectObjectsTest {

	// {expected ore name, ids that report it}, in table order.
	private static final Object[][] EXPECTED = {
			{ "copper ore", new int[] { 2090, 2091, 3042 } },
			{ "tin ore", new int[] { 2094, 2095, 3043 } },
			{ "blurite ore", new int[] { 2110 } },
			{ "iron ore", new int[] { 2092, 2093 } },
			{ "silver ore", new int[] { 2100, 2101 } },
			{ "gold ore", new int[] { 2098, 2099 } },
			{ "coal", new int[] { 2096, 2097 } },
			{ "mithril ore", new int[] { 2102, 2103 } },
			{ "adamantite ore", new int[] { 2104, 2105 } },
			{ "runite ore", new int[] { 2106, 2107 } },
	};

	private static final int[] NOTHING = { 450, 451 };

	@Test
	void oreNamesAndIdRowsStayAligned() {
		int[][] ids = MiningProspectObjects.oreIds();
		String[] names = MiningProspectObjects.oreNames();
		assertEquals(EXPECTED.length, ids.length, "row count");
		assertEquals(EXPECTED.length, names.length, "name count");
		for (int i = 0; i < EXPECTED.length; i++) {
			assertEquals(EXPECTED[i][0], names[i], "row " + i + " name");
			int[] expectedIds = (int[]) EXPECTED[i][1];
			assertEquals(expectedIds.length, ids[i].length, "row " + i + " id count");
			for (int j = 0; j < expectedIds.length; j++) {
				assertEquals(expectedIds[j], ids[i][j], "row " + i + " id " + j);
			}
		}
	}

	@Test
	void coalIsTheOnlyNameWithoutTheOreSuffix() {
		// Carried over verbatim from the switch; easy to "fix" by accident.
		for (String name : MiningProspectObjects.oreNames()) {
			if (name.equals("coal")) {
				continue;
			}
			assertTrue(name.endsWith(" ore"), name + " should end in \" ore\"");
		}
	}

	@Test
	void everyProspectableRockIsRegisteredOnSecondClick() {
		for (int[] row : MiningProspectObjects.oreIds()) {
			for (int id : row) {
				assertTrue(ObjectHandler.isRegistered(id, ObjectClick.SECOND),
						"rock " + id + " has no prospect handler");
			}
		}
		for (int id : NOTHING) {
			assertTrue(ObjectHandler.isRegistered(id, ObjectClick.SECOND),
					"rock " + id + " should prospect as nothing");
		}
	}

	@Test
	void noRockIsListedUnderTwoOres() {
		Set<Integer> seen = new HashSet<>();
		for (int[] row : MiningProspectObjects.oreIds()) {
			for (int id : row) {
				assertTrue(seen.add(id), "rock " + id + " appears twice");
			}
		}
		for (int id : NOTHING) {
			assertTrue(seen.add(id), "rock " + id + " appears twice");
		}
		assertEquals(23, seen.size());
	}
}
