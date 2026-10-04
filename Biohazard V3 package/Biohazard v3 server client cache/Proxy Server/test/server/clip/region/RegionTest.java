package server.clip.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import server.Config;

/**
 * Pins the two properties of {@link Region}'s collision lookup that the O(1) region
 * index and the fail-closed policy depend on.
 *
 * <p>The index is keyed by {@link Region#regionIdOf}. If that ever disagreed with the id
 * a {@link Region} is constructed with, every lookup would miss and the whole world would
 * read as blocked — a total-failure mode with no crash to point at it, which is why the
 * mapping is asserted by value here and not merely exercised.
 *
 * <p>{@link Region#BLOCKED} is the second half: a tile with no collision data must stop
 * every direction the movement code tests, or it would only be half-blocked and still
 * leak a diagonal squeeze.
 *
 * <p>Region data is not loaded under the test task (nothing calls {@code Region.load()}),
 * so these tests deliberately exercise the no-data path. The real-geometry check is
 * {@code Region.verifyClippingConsistency}, which runs at boot.
 */
class RegionTest {

	@Test
	void regionIdOfMatchesTheRegionIdRegionsAreKeyedBy() {
		// Region ids are (regionX / 8 << 8) + regionY / 8 with regionX = x >> 3.
		// 3222,3218 -> 3222>>3 = 402, 402/8 = 50 -> (50 << 8) + 50.
		assertEquals((50 << 8) + 50, Region.regionIdOf(3222, 3218));
	}

	@Test
	void everyTileOfARegionMapsToTheSameIdIncludingItsLastRowAndColumn() {
		assertEquals(Region.regionIdOf(3200, 3200), Region.regionIdOf(3263, 3263),
				"3200 and 3263 are the first and last tile of the same region");
		// 3264 >> 3 = 408, 408 / 8 = 51 -> the next region.
		assertEquals(Region.regionIdOf(3200, 3200) + (1 << 8), Region.regionIdOf(3264, 3200));
	}

	@Test
	void regionIdOfDoesNotFallFoulOfShiftPrecedence() {
		// x >> 3 / 8 parses as x >> (3 / 8) == x >> 0 == x, which would give 3200 here
		// instead of 50. The parentheses in regionIdOf are load-bearing.
		assertEquals((50 << 8) + 50, Region.regionIdOf(3200, 3200));
	}

	@Test
	void crossingARegionBoundaryChangesTheIdByExactlyOne() {
		int atBoundary = Region.regionIdOf(3200, 3200);
		assertEquals(atBoundary - (1 << 8), Region.regionIdOf(3199, 3200), "one tile west");
		assertEquals(atBoundary - 1, Region.regionIdOf(3200, 3199), "one tile south");
	}

	@Test
	void anUnknownRegionResolvesToNullRatherThanThrowing() {
		assertNull(Region.getRegion(100000, 100000));
	}

	@Test
	void clippingForUnknownTerrainFollowsTheFailClosedFlag() {
		// No region resolves here. With REGION_FAIL_CLOSED on the tile must be blocked; with it
		// off the legacy behaviour (0 = walkable) is preserved, which is what keeps the ground
		// around content the server has no map for — the Nex arena — traversable.
		int clip = Region.getClipping(100000, 100000, 0);
		if (Config.REGION_FAIL_CLOSED) {
			assertEquals(Region.blockedValue(), clip);
		} else {
			assertEquals(0, clip, "legacy behaviour: unknown terrain is walkable");
		}
	}

	@Test
	void theBlockedValueStopsAllFourCardinalsAndAllFourDiagonals() {
		int blocked = Region.blockedValue();
		assertTrue((blocked & 0x1280108) != 0, "west");
		assertTrue((blocked & 0x1280180) != 0, "east");
		assertTrue((blocked & 0x1280102) != 0, "north");
		assertTrue((blocked & 0x1280120) != 0, "south");
		assertTrue((blocked & 0x128010e) != 0, "south-west diagonal");
		assertTrue((blocked & 0x1280183) != 0, "north-east diagonal");
		assertTrue((blocked & 0x1280138) != 0, "south-east diagonal");
		assertTrue((blocked & 0x12801e0) != 0, "north-west diagonal");
	}

	@Test
	void theBlockedValueMakesATileNeitherStandableNorShootableThrough() {
		// OCCUPANT = 0x100 (solid) | 0x20000 (projectile-solid); SmartPathFinder.isStandable
		// and the projectile code both test those bits.
		int blocked = Region.blockedValue();
		assertTrue((blocked & 0x100) != 0, "solid bit");
		assertTrue((blocked & 0x20000) != 0, "projectile-solid bit");
	}

	@Test
	void theMovementStepApiAgreesWithRegionAboutUnknownTerrain() {
		// The "one collision source" invariant: Region's clip value and SmartPathFinder's step
		// API must not disagree about a tile with no data, whichever way the flag is set.
		boolean blocked = Config.REGION_FAIL_CLOSED;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0) {
					continue;
				}
				assertEquals(!blocked, SmartPathFinder.canStep(100000, 100000, dx, dy, 0),
						"canStep disagrees with REGION_FAIL_CLOSED for delta " + dx + "," + dy);
			}
		}
	}
}
