package server.game.bots.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import server.game.objects.Objects;

/**
 * Curated first, scan as fallback — {@code BOT_LOCATIONS.md} A.4.
 *
 * <p>The rule worth testing hardest is the third one: a scanned hit inside a curated box must be
 * dropped. Without it the Draynor bank is reported twice — once as the authored region and once as
 * each booth inside it — and a bot choosing between two answers for one place oscillates.
 */
class MergingLocatorTest {

	private static final int BANK_BOOTH = 2213, TREE = 1276;

	private static final Location DRAYNOR_BANK = new Location("draynor_bank", LocationKind.BANK,
			3091, 3241, 0, 6, 5, null);

	/** A one-object world, so the scan arm has something to find. */
	private static ScannedLocator scannedWorld(final int objectId, final String kind, final int x,
			final int y) {
		ScannedLocator.RegionSource source = new ScannedLocator.RegionSource() {
			@Override
			public List<Objects> objects(int baseX, int baseY) {
				// 64-wide regions: place the object in whichever region actually contains it.
				if ((x >> 6 << 6) == baseX && (y >> 6 << 6) == baseY) {
					return Arrays.asList(new Objects(objectId, x, y, 0, 0, 10));
				}
				return Collections.<Objects>emptyList();
			}
		};
		ScannedLocator.KindSource kinds = new ScannedLocator.KindSource() {
			@Override
			public String kindOf(int id) {
				return id == objectId ? kind : null;
			}
		};
		RegionScanCache cache = ScannedLocator.cacheOver(source, kinds, new ScannedLocator.Clock() {
			@Override
			public long now() {
				return 1;
			}
		}, 1000);
		return ScannedLocator.live(EnumSet.of(LocationKind.byId(kind)), 2, cache);
	}

	@Test
	void theCuratedAnswerIsUsedWhenItExists() {
		MergingLocator merged = new MergingLocator(
				CuratedLocator.filter(Collections.singletonList(DRAYNOR_BANK), LocationKind.BANK),
				null);

		List<Location> found = merged.nearest(3093, 3243, 0, 1);

		assertEquals(1, found.size());
		assertEquals("draynor_bank", found.get(0).name(), "the authored name, not a generated one");
	}

	@Test
	void aScannedHitInsideACuratedBoxIsDroppedRatherThanReportedTwice() {
		// The booth at 3091,3243 is inside the curated 6x5 box. Asking for four, with a scan arm that
		// can see the booth, must still produce exactly one place.
		MergingLocator merged = new MergingLocator(
				CuratedLocator.filter(Collections.singletonList(DRAYNOR_BANK), LocationKind.BANK),
				scannedWorld(BANK_BOOTH, "bank", 3091, 3243));

		List<Location> found = merged.nearest(3093, 3243, 0, 4);

		assertEquals(1, found.size(), "one place, one answer: " + found);
		assertEquals("draynor_bank", found.get(0).name());
	}

	@Test
	void aScannedHitOutsideTheCuratedBoxIsOfferedAsItsOwnPlace() {
		// A curated bank somewhere else does not cover this booth, so the two are genuinely distinct
		// places and both should come back.
		Location varrockBank = new Location("varrock_west_bank", LocationKind.BANK, 3251, 3419, 0, 6,
				1, null);
		MergingLocator merged = new MergingLocator(
				CuratedLocator.filter(Arrays.asList(varrockBank), LocationKind.BANK),
				scannedWorld(BANK_BOOTH, "bank", 3091, 3243));

		List<Location> found = merged.nearest(3093, 3243, 0, 4);

		assertEquals(2, found.size(), "two banks: " + found);
		assertEquals(LocationKind.BANK, found.get(0).kind());
		assertTrue(found.get(0).x() == 3091, "the nearest is the scanned one at the query");
		assertEquals("varrock_west_bank", found.get(1).name());
	}

	@Test
	void theScanTopsUpWhenTheCuratedTableCannotFillTheRequest() {
		// One curated oak, and a scan arm that can see a tree nearby. Asking for two must return two,
		// not one — otherwise "the three nearest oaks" comes back short purely because only one place
		// was ever authored.
		Location curatedOak = new Location("draynor_oaks", LocationKind.TREE, 3103, 3217, 0, 16, 23,
				null);
		MergingLocator merged = new MergingLocator(
				CuratedLocator.filter(Collections.singletonList(curatedOak), LocationKind.TREE),
				scannedWorld(TREE, "tree", 3217, 3241));

		List<Location> found = merged.nearest(3200, 3230, 0, 2);

		assertEquals(2, found.size(), "curated plus a scanned top-up: " + found);
		assertEquals(LocationKind.TREE, found.get(0).kind());
		assertEquals(LocationKind.TREE, found.get(1).kind());
	}

	@Test
	void byNameIsACuratedQuestionBecauseAScanHasNoNames() {
		MergingLocator merged = new MergingLocator(
				CuratedLocator.filter(Collections.singletonList(DRAYNOR_BANK), LocationKind.BANK),
				scannedWorld(BANK_BOOTH, "bank", 3091, 3243));

		assertSame(DRAYNOR_BANK, merged.byName("draynor_bank"));
		assertEquals(null, merged.byName("bank:3091,3243:0"), "generated names are not lookups");
	}

	@Test
	void anEmptyCuratedTableWithNoScanArmAnswersNothing() {
		MergingLocator merged = new MergingLocator(
				CuratedLocator.of(Collections.<Location>emptyList()), null);

		assertTrue(merged.isEmpty());
		assertTrue(merged.nearest(0, 0, 0, 1).isEmpty());
	}

	@Test
	void withTheAuthoredFileDeletedTheScanArmStillAnswers() {
		// The acceptance criterion: deleting locations.cfg degrades to scanned lookups, not a crash.
		MergingLocator merged = new MergingLocator(
				CuratedLocator.of(Collections.<Location>emptyList()),
				scannedWorld(BANK_BOOTH, "bank", 3091, 3243));

		assertEquals(0, merged.all().size(), "all() is the curated table, which is empty");
		assertFalse(merged.nearest(3091, 3243, 0, 4).isEmpty(), "but nearest() still answers");
		assertEquals(LocationKind.BANK, merged.nearest(3091, 3243, 0, 1).get(0).kind());
	}
}
