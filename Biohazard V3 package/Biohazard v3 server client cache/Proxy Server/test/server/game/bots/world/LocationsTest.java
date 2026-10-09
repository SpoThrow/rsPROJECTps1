package server.game.bots.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import server.game.objects.Objects;

/**
 * The facade's families, and the rule that decides which of them get a scan arm.
 *
 * <p>A family is a filtered view of one table plus, for the families that can be scanned, the same
 * shared {@link RegionScanCache}. Both halves matter: without the filter, {@code banks()} would offer
 * trees; without the exclusion, {@code teleports()} would scan the world looking for an object that
 * does not exist.
 */
class LocationsTest {

	private static final Location BANK = Location.point("a_bank", LocationKind.BANK, 3091, 3243, 0);
	private static final Location TREE = new Location("oaks", LocationKind.TREE, 3103, 3217, 0, 16, 23,
			null);
	private static final Location TELEPORT = Location.point("modern_varrock", LocationKind.TELEPORT,
			3210, 3424, 0);

	private static List<Location> table() {
		return Arrays.asList(BANK, TREE, TELEPORT);
	}

	@Test
	void eachFamilySeesOnlyItsOwnKinds() {
		Locations locations = Locations.curated(table());

		assertEquals(1, locations.banks().all().size());
		assertEquals(1, locations.trees().all().size());
		assertEquals(1, locations.teleports().all().size());
		assertEquals(LocationKind.BANK, locations.banks().all().get(0).kind());
		assertTrue(locations.rocks().isEmpty(), "no rocks were authored, and that is not an error");
	}

	@Test
	void resourcesAndServicesSplitTheGatherableFromTheRest() {
		Locations locations = Locations.curated(table());

		assertEquals(1, locations.resources().all().size(), "the tree");
		assertEquals(1, locations.services().all().size(), "the bank; the teleport is travel, not a service");
		assertEquals(LocationKind.TREE, locations.resources().all().get(0).kind());
	}

	@Test
	void everythingReturnsTheWholeTableIncludingWhatNoFamilyClaims() {
		Locations locations = Locations.curated(table());

		assertEquals(3, locations.everything().all().size());
		assertEquals(3, locations.all().size());
	}

	@Test
	void forKindFindsAnyKindThroughTheSameFamiliesItAlreadyHas() {
		Locations locations = Locations.curated(table());

		// A script names a kind, not a family method, so this is the one seam a builder needs.
		assertEquals(locations.trees().all(), locations.forKind(LocationKind.TREE).all());
		assertEquals(TREE, locations.forKind(LocationKind.TREE).nearest(3103, 3217, 0, 1).get(0));
		assertEquals(BANK, locations.forKind(LocationKind.BANK).nearest(3091, 3243, 0, 1).get(0));
		assertTrue(locations.forKind(LocationKind.ROCK).isEmpty(), "no rocks were authored");
	}

	@Test
	void aCuratedOnlyTableHasNoScanArmSoAnUnknownPlaceIsSimplyAbsent() {
		Locations locations = Locations.curated(table());

		// There is no world to scan, so a family with nothing behind it answers nothing — rather than
		// throwing, or inventing a row.
		assertTrue(locations.rocks().nearest(3200, 3200, 0, 4).isEmpty());
		assertEquals(BANK, locations.banks().nearest(3091, 3243, 0, 1).get(0));
	}

	@Test
	void aTableBackedByACacheTopsAFamilyUpFromTheWorld() {
		// The curated table has no rock; the world does. With a cache attached, rocks() answers from
		// the scan, which is exactly the "deleted locations.cfg" path.
		ScannedLocator.RegionSource source = new ScannedLocator.RegionSource() {
			@Override
			public List<Objects> objects(int baseX, int baseY) {
				if (baseX == 3200 && baseY == 3200) {
					return Arrays.asList(new Objects(2091, 3250, 3205, 0, 0, 10));
				}
				return Collections.<Objects>emptyList();
			}
		};
		ScannedLocator.KindSource kinds = new ScannedLocator.KindSource() {
			@Override
			public String kindOf(int objectId) {
				return objectId == 2091 ? "rock" : null;
			}
		};
		RegionScanCache cache = ScannedLocator.cacheOver(source, kinds, new ScannedLocator.Clock() {
			@Override
			public long now() {
				return 1;
			}
		}, 1000);

		Locations locations = Locations.of(Collections.<Location>emptyList(), cache);

		List<Location> rocks = locations.rocks().nearest(3200, 3200, 0, 1);
		assertEquals(1, rocks.size(), "the scan arm answers for a family with no curated rows");
		assertEquals(LocationKind.ROCK, rocks.get(0).kind());
		assertEquals(3250, rocks.get(0).x());

		// And a family with no object kind behind it does not scan at all.
		assertTrue(locations.teleports().nearest(3200, 3200, 0, 1).isEmpty());
	}

	@Test
	void aFamilyWithAnObjectKindGetsAScanArmEvenWhenItAlsoHasCuratedRows() {
		// Mixed families are the hard case: the curated bank must answer, and the scan must still be
		// reachable for the rock that was never authored.
		ScannedLocator.RegionSource source = new ScannedLocator.RegionSource() {
			@Override
			public List<Objects> objects(int baseX, int baseY) {
				return baseX == 3200 && baseY == 3200
						? Arrays.asList(new Objects(2213, 3091, 3243, 0, 0, 10))
						: Collections.<Objects>emptyList();
			}
		};
		ScannedLocator.KindSource kinds = new ScannedLocator.KindSource() {
			@Override
			public String kindOf(int objectId) {
				return objectId == 2213 ? "bank" : null;
			}
		};
		RegionScanCache cache = ScannedLocator.cacheOver(source, kinds, new ScannedLocator.Clock() {
			@Override
			public long now() {
				return 1;
			}
		}, 1000);

		Locations locations = Locations.of(Collections.singletonList(BANK), cache);

		// The curated bank covers the scanned booth, so the answer is still one place with the
		// authored name.
		List<Location> banks = locations.banks().nearest(3091, 3243, 0, 4);
		assertEquals(1, banks.size(), "curated and scanned describe one place: " + banks);
		assertEquals("a_bank", banks.get(0).name());
		assertFalse(locations.services().isEmpty(), "the services family can answer");
	}

	@Test
	void theFamiliesAreBuiltOnceAndReused() {
		Locations locations = Locations.curated(table());

		assertTrue(locations.banks() == locations.banks(), "a family is not rebuilt per call");
		assertEquals(EnumSet.of(LocationKind.BANK), CuratedLocator.filter(table(), LocationKind.BANK).kinds());
	}
}
