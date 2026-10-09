package server.game.bots.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import server.game.objects.Objects;

/**
 * The scanned locator: the logic, the ring bound, the plane filter and the two caches.
 *
 * <p>Driven by a fake world rather than {@code Region.load()}. Loading 1226 regions and 1.9 million
 * objects to test "does it find the tree two regions east" would make this suite slow and its memory
 * use unpredictable, and it would test {@code Region} rather than this class. The live wiring is
 * covered by {@code ResourceKindsParityTest}, and end to end by {@code workshopValidate}.
 */
class ScannedLocatorTest {

	private static final int TREE = 1276, BANK = 2213, ROCK = 2091, JUNK = 9999;

	/** Counts calls, so cache behaviour is asserted rather than assumed. */
	private static final class CountingSource implements ScannedLocator.RegionSource {
		final Map<Long, List<Objects>> regions = new HashMap<Long, List<Objects>>();
		int calls;

		void put(int baseX, int baseY, List<Objects> objects) {
			regions.put(key(baseX, baseY), objects);
		}

		@Override
		public List<Objects> objects(int baseX, int baseY) {
			calls++;
			List<Objects> found = regions.get(key(baseX, baseY));
			return found == null ? Collections.<Objects>emptyList() : found;
		}

		private static long key(int baseX, int baseY) {
			return ((long) baseX << 32) | (baseY & 0xffffffffL);
		}
	}

	private static final class CountingKinds implements ScannedLocator.KindSource {
		final Map<Integer, String> kinds = new HashMap<Integer, String>();
		int calls;

		CountingKinds() {
			kinds.put(TREE, "tree");
			kinds.put(BANK, "bank");
			kinds.put(ROCK, "rock");
		}

		@Override
		public String kindOf(int objectId) {
			calls++;
			return kinds.get(objectId);
		}
	}

	private static final class FakeClock implements ScannedLocator.Clock {
		long now = 1_000_000;

		@Override
		public long now() {
			return now;
		}
	}

	private static Objects object(int id, int x, int y, int plane) {
		return new Objects(id, x, y, plane, 0, 10);
	}

	private static List<Objects> objects(Objects... values) {
		List<Objects> list = new ArrayList<Objects>();
		Collections.addAll(list, values);
		return list;
	}

	private CountingSource world;
	private CountingKinds kinds;
	private FakeClock clock;
	private RegionScanCache cache;

	private ScannedLocator locator(int maxRings) {
		world = new CountingSource();
		kinds = new CountingKinds();
		clock = new FakeClock();
		// One region: 3200..3263. Objects sit at known offsets inside it.
		world.put(3200, 3200, objects(
				object(TREE, 3217, 3241, 0),
				object(BANK, 3248, 3240, 0),
				object(ROCK, 3250, 3205, 1),
				object(JUNK, 3201, 3201, 0)));
		// The next region east: 3264..3327. Its rock is 15 tiles from x=3255.
		world.put(3264, 3200, objects(object(ROCK, 3270, 3210, 0)));
		cache = ScannedLocator.cacheOver(world, kinds, clock, ScannedLocator.DEFAULT_TTL_MILLIS);
		return ScannedLocator.live(ScannedLocator.objectKinds(), maxRings, cache);
	}

	@Test
	void findsAClassifiedObjectInTheQueriedRegion() {
		ScannedLocator locator = locator(ScannedLocator.DEFAULT_MAX_RINGS);

		List<Location> found = locator.nearest(3217, 3241, 0, 1);

		assertEquals(1, found.size());
		assertEquals(LocationKind.TREE, found.get(0).kind());
		assertEquals(3217, found.get(0).x());
		assertEquals(3241, found.get(0).y());
	}

	@Test
	void anUnclassifiedObjectIsNotALocation() {
		ScannedLocator locator = locator(ScannedLocator.DEFAULT_MAX_RINGS);

		// 3201,3201 is the junk object's tile and the closest thing to a query there, but no
		// location may carry its coordinates: nothing classified it, so it is not a place.
		List<Location> found = locator.nearest(3201, 3201, 0, 8);

		assertFalse(found.isEmpty(), "the tree and the bank are still in the same region");
		for (Location location : found) {
			assertFalse(location.x() == 3201 && location.y() == 3201,
					"the unclassified object must not appear, got " + location);
		}
	}

	@Test
	void findsAMatchAcrossARegionBoundary() {
		ScannedLocator locator = locator(ScannedLocator.DEFAULT_MAX_RINGS);

		// x=3255 is in region 3200; the only rock on plane 0 is in region 3264.
		List<Location> found = locator.nearest(3255, 3210, 0, 1);

		assertFalse(found.isEmpty(), "the ring search must look into the neighbouring region");
		assertEquals(LocationKind.ROCK, found.get(0).kind());
		assertEquals(3270, found.get(0).x());
	}

	@Test
	void aBucketOfAnotherPlaneIsNeverTheAnswer() {
		ScannedLocator locator = locator(ScannedLocator.DEFAULT_MAX_RINGS);

		// The rock is on plane 1. Asking on plane 1 finds it; asking on plane 0 must not.
		assertFalse(locator.nearest(3250, 3205, 1, 1).isEmpty(), "plane 1 finds the plane-1 rock");
		for (Location location : locator.nearest(3250, 3205, 0, 4)) {
			assertEquals(0, location.plane(), "a plane-0 query returned " + location);
		}
	}

	@Test
	void nearestIsOrderedByDistanceToTheNearestEdgeNotTheCentre() {
		ScannedLocator locator = locator(ScannedLocator.DEFAULT_MAX_RINGS);

		List<Location> found = locator.nearest(3217, 3241, 0, 4);

		assertTrue(found.size() >= 2, "got " + found);
		assertEquals(3217, found.get(0).x(), "the tree at the query tile is nearest");
		assertEquals(3248, found.get(1).x(), "then the bank, 31 tiles east and 1 south");
	}

	@Test
	void theLimitIsHonoured() {
		ScannedLocator locator = locator(ScannedLocator.DEFAULT_MAX_RINGS);

		assertEquals(1, locator.nearest(3217, 3241, 0, 1).size());
		assertEquals(2, locator.nearest(3217, 3241, 0, 2).size());
		assertEquals(0, locator.nearest(3217, 3241, 0, 0).size());
	}

	@Test
	void aRegionIsScannedOnceAndThenReused() {
		ScannedLocator locator = locator(ScannedLocator.DEFAULT_MAX_RINGS);

		locator.nearest(3217, 3241, 0, 1);
		int afterFirst = world.calls;
		assertTrue(afterFirst >= 1, "the first query scans");

		locator.nearest(3217, 3241, 0, 1);
		// A second query answered from the region already scanned must not touch the source again.
		// Both of these land on a location's own tile, so the ring bound stops the search at ring 0.
		locator.nearest(3248, 3240, 0, 1);

		assertEquals(afterFirst, world.calls, "re-queries inside a scanned region must not re-scan");
	}

	@Test
	void aQueryWhoseRingBoundIsNotMetKeepsLookingAndSaysSoByScanningMore() {
		// The bound is deliberately conservative: it only stops the search when something found is
		// closer than the nearest tile a further ring could hold. A query a few tiles off a location
		// therefore still looks outward, which costs one extra ring but cannot miss a nearer object.
		ScannedLocator locator = locator(ScannedLocator.DEFAULT_MAX_RINGS);

		locator.nearest(3217, 3241, 0, 1);
		int afterExactHit = world.calls;
		locator.nearest(3218, 3242, 0, 1);

		assertTrue(world.calls > afterExactHit,
				"a near-miss query looks outward rather than stopping at ring 0");
	}

	@Test
	void aRegionIsRescannedAfterTheTtlExpires() {
		ScannedLocator locator = locator(ScannedLocator.DEFAULT_MAX_RINGS);

		locator.nearest(3217, 3241, 0, 1);
		int afterFirst = world.calls;
		clock.now += ScannedLocator.DEFAULT_TTL_MILLIS + 1;
		locator.nearest(3217, 3241, 0, 1);

		assertTrue(world.calls > afterFirst, "an expired region is scanned again");
	}

	@Test
	void aKindIsLookedUpOncePerObjectIdNoMatterHowManyTimesItWasPlaced() {
		// ObjectDef re-parses loc.dat on every cache miss and only caches 20 entries, so a
		// classification that is not memoised would be paid for on every scan of every region.
		ScannedLocator locator = locator(ScannedLocator.DEFAULT_MAX_RINGS);

		locator.nearest(3217, 3241, 0, 4);
		int afterFirst = kinds.calls;
		clock.now += ScannedLocator.DEFAULT_TTL_MILLIS + 1;
		locator.nearest(3217, 3241, 0, 4); // forces a fresh scan of the same ids

		assertTrue(afterFirst > 0, "the first scan classifies");
		assertEquals(afterFirst, kinds.calls, "the second scan must reuse the memo for every id");
	}

	@Test
	void aMatchBeyondTheRingBoundIsNotFoundAndThatIsTheDocumentedLimit() {
		// Ring 0 is the query's own region only. The only rock on plane 0 lives in the region next
		// door, so with a zero-ring bound it must not be found — this is the cost of not scanning the
		// world, stated as a test rather than left as a surprise.
		ScannedLocator locator = locator(0);

		List<Location> found = locator.nearest(3201, 3201, 0, 8);

		assertFalse(found.isEmpty(), "the query's own region has the tree and the bank");
		for (Location location : found) {
			assertTrue(location.kind() == LocationKind.TREE || location.kind() == LocationKind.BANK,
					"with ring 0 the rock in region 3264 is out of range, got " + location);
		}
	}

	@Test
	void theRingBoundStillLooksOutwardWhenNothingIsInTheQueriedRegion() {
		// The complement of the test above: a region with nothing classified must not stop the search.
		ScannedLocator locator = locator(ScannedLocator.DEFAULT_MAX_RINGS);
		world.put(3200, 3264, objects(object(JUNK, 3201, 3265, 0)));

		List<Location> found = locator.nearest(3201, 3265, 0, 1);

		assertFalse(found.isEmpty(), "the search must leave an empty region rather than give up");
		assertEquals(LocationKind.TREE, found.get(0).kind(), "found the tree a region north: " + found);
	}

	@Test
	void aScannedLocationHasAGeneratedNameAndNoBylineLookup() {
		ScannedLocator locator = locator(ScannedLocator.DEFAULT_MAX_RINGS);

		Location tree = locator.nearest(3217, 3241, 0, 1).get(0);

		assertTrue(tree.name().startsWith("tree:"), tree.name());
		assertEquals(null, locator.byName("draynor_bank"), "scanning is not a name lookup");
		assertTrue(locator.all().isEmpty(), "all() promises nothing without a world scan");
	}

	@Test
	void theKindFilterDecidesWhatAFamilySees() {
		locator(ScannedLocator.DEFAULT_MAX_RINGS); // builds world, kinds and clock
		Set<LocationKind> onlyBanks = EnumSet.of(LocationKind.BANK);
		ScannedLocator banks = ScannedLocator.live(onlyBanks, 2,
				ScannedLocator.cacheOver(world, kinds, clock, 1000));

		List<Location> found = banks.nearest(3217, 3241, 0, 4);

		assertEquals(1, found.size(), "only the bank is in this family: " + found);
		assertEquals(LocationKind.BANK, found.get(0).kind());
	}
}
