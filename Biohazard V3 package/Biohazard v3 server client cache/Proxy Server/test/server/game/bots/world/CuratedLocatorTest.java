package server.game.bots.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/** The curated table: plane-strictness, ordering, names and family filtering. */
class CuratedLocatorTest {

	private static final Location DRAYNOR_BANK = new Location("draynor_bank", LocationKind.BANK,
			3091, 3241, 0, 6, 5, Arrays.asList("draynor"));
	private static final Location CASTLE_BANK = new Location("castle_bank", LocationKind.BANK,
			3207, 3221, 2, 4, 1, null);
	private static final Location LUMBRIDGE_ALTAR = Location.point("lumbridge_altar",
			LocationKind.PRAYER, 3243, 3207, 0);
	private static final Location DRAYNOR_OAKS = new Location("draynor_oaks", LocationKind.TREE,
			3103, 3217, 0, 16, 23, Arrays.asList("oak"));

	private static List<Location> table() {
		return new ArrayList<Location>(Arrays.asList(
				DRAYNOR_BANK, CASTLE_BANK, LUMBRIDGE_ALTAR, DRAYNOR_OAKS));
	}

	@Test
	void nearestIsOrderedByDistanceToTheNearestEdgeOfTheBox() {
		CuratedLocator all = CuratedLocator.of(table());

		// Standing in the middle of the Draynor box: it is 0 away, not ~3 tiles to its centre.
		List<Location> found = all.nearest(3093, 3243, 0, 4);

		assertEquals(3, found.size(), "plane 0 holds the bank, the oaks and the altar: " + found);
		assertEquals("draynor_bank", found.get(0).name(), "the box the query stands in is nearest");
		assertEquals("draynor_oaks", found.get(1).name(), "then the oaks, 10 tiles east and 4 south");
		assertEquals("lumbridge_altar", found.get(2).name(), "and the altar across the river");
	}

	@Test
	void nearestNeverCrossesAPlane() {
		CuratedLocator all = CuratedLocator.of(table());

		// The castle bank is at 3207,3221 on plane 2. From the courtyard below it is 21 tiles away
		// *and a floor*, which is not a shorter trip than the Draynor bank 130 tiles away.
		List<Location> onPlaneZero = all.nearest(3207, 3221, 0, 4);

		for (Location location : onPlaneZero) {
			assertEquals(0, location.plane(), "plane 0 returned " + location);
		}
		List<Location> onPlaneTwo = all.nearest(3207, 3221, 2, 4);
		assertEquals("castle_bank", onPlaneTwo.get(0).name(), "and plane 2 does find it");
	}

	@Test
	void aPlaneWithNothingOnItGivesAnEmptyAnswerRatherThanAForeignOne() {
		CuratedLocator onlyBanks = CuratedLocator.filter(table(), LocationKind.BANK);

		assertTrue(onlyBanks.nearest(3207, 3221, 1, 4).isEmpty(),
				"there is no bank on plane 1, so there is no answer");
	}

	@Test
	void theLimitIsHonoured() {
		CuratedLocator all = CuratedLocator.of(table());

		assertEquals(0, all.nearest(3093, 3243, 0, 0).size());
		assertEquals(1, all.nearest(3093, 3243, 0, 1).size());
		assertEquals(3, all.nearest(3093, 3243, 0, 9).size(), "only three exist on plane 0");
	}

	@Test
	void byNameFindsACuratedPlaceAndNothingElse() {
		CuratedLocator all = CuratedLocator.of(table());

		assertSame(DRAYNOR_BANK, all.byName("draynor_bank"));
		assertNull(all.byName("not_a_place"));
		assertNull(all.byName(null));
	}

	@Test
	void filteringByKindGivesAFamilyWithoutCopyingTheTable() {
		CuratedLocator trees = CuratedLocator.filter(table(), LocationKind.TREE);

		assertEquals(1, trees.all().size());
		assertEquals(LocationKind.TREE, trees.all().get(0).kind());
		assertNull(trees.byName("draynor_bank"), "a bank is not in the tree family");
		assertEquals(1, trees.nearest(3100, 3200, 0, 4).size());
	}

	@Test
	void aFamilyCanHoldSeveralKinds() {
		CuratedLocator services = CuratedLocator.filter(table(), LocationKind.BANK,
				LocationKind.PRAYER);

		assertEquals(3, services.all().size(), "two banks and an altar");
		assertEquals(2, services.nearest(3093, 3243, 0, 9).size(),
				"plane 0 of that family is the bank and the altar");
	}

	@Test
	void aDuplicateNameKeepsTheFirstRowSoABotIsReproducible() {
		Location first = Location.point("a_bank", LocationKind.BANK, 1, 1, 0);
		Location second = Location.point("a_bank", LocationKind.BANK, 2, 2, 0);
		CuratedLocator all = CuratedLocator.of(Arrays.asList(first, second));

		assertSame(first, all.byName("a_bank"), "the first wins, deterministically");
		assertEquals(2, all.all().size(), "but both rows still exist for nearest()");
	}

	@Test
	void anEmptyTableIsEmptyRatherThanNull() {
		CuratedLocator none = CuratedLocator.of(Collections.<Location>emptyList());

		assertTrue(none.isEmpty());
		assertTrue(none.nearest(0, 0, 0, 4).isEmpty());
		assertNull(none.byName("anything"));
	}
}
