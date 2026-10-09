package server.game.bots.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * {@link RandomTileIn} and {@link FixedPoint}: the "rough idea within certain tiles" mechanism.
 *
 * <p>The properties that matter are reproducibility (a stuck bot can be replayed from its trace) and
 * spread (two bots sent to one box do not stack). Both are asserted directly, and both are
 * deterministic rather than eyeballed, because {@link java.util.Random}'s sequence is specified.
 */
class RandomTileInTest {

	/** Draynor oaks, from the seed file: a 16x23 box. */
	private static Location oaks() {
		return new Location("draynor_oaks", LocationKind.TREE, 3103, 3217, 0, 16, 23,
				Arrays.asList("oak"));
	}

	/** Everything walkable — the common case, so the random roll always succeeds. */
	private static final RandomTileIn.Walkable OPEN = new RandomTileIn.Walkable() {
		@Override
		public boolean test(int x, int y, int plane) {
			return true;
		}
	};

	@Test
	void theSameSeedAlwaysResolvesToTheSameTile() {
		RandomTileIn waypoint = new RandomTileIn(oaks(), OPEN);

		for (int seed = 0; seed < 50; seed++) {
			assertEquals(waypoint.resolve(seed), waypoint.resolve(seed),
					"seed " + seed + " must be reproducible, or a stuck bot cannot be replayed");
		}
	}

	@Test
	void twoBotsSharingAWaypointLandOnDifferentTiles() {
		RandomTileIn waypoint = new RandomTileIn(oaks(), OPEN);

		Set<Tile> chosen = new HashSet<Tile>();
		for (int seed = 1; seed <= 20; seed++) {
			chosen.add(waypoint.resolve(seed));
		}

		assertTrue(chosen.size() >= 10,
				"20 bots on one waypoint should spread out, got " + chosen.size() + " distinct tiles");
		assertNotEquals(waypoint.resolve(1), waypoint.resolve(2), "the first two seeds differ");
	}

	@Test
	void everyResolvedTileIsInsideTheBoxOnTheRightPlane() {
		Location box = oaks();
		RandomTileIn waypoint = new RandomTileIn(box, OPEN);

		for (int seed = 0; seed < 200; seed++) {
			Tile tile = waypoint.resolve(seed);
			assertTrue(box.contains(tile.x(), tile.y(), box.plane()), seed + " gave " + tile);
		}
	}

	@Test
	void aBlockedCandidateIsRejectedRatherThanReturned() {
		// Only the one tile is walkable, so every roll misses until the fallback finds it.
		final int onlyX = 3110, onlyY = 3230;
		RandomTileIn.Walkable single = new RandomTileIn.Walkable() {
			@Override
			public boolean test(int x, int y, int plane) {
				return x == onlyX && y == onlyY;
			}
		};
		RandomTileIn waypoint = new RandomTileIn(oaks(), single);

		for (int seed = 0; seed < 20; seed++) {
			Tile tile = waypoint.resolve(seed);
			assertEquals(onlyX, tile.x(), "seed " + seed + " must not land on a blocked tile");
			assertEquals(onlyY, tile.y());
		}
	}

	@Test
	void whenTheWholeBoxIsBlockedTheCentreIsReturnedAndThatIsTheHonestAnswer() {
		RandomTileIn.Walkable closed = new RandomTileIn.Walkable() {
			@Override
			public boolean test(int x, int y, int plane) {
				return false;
			}
		};
		Location box = oaks();

		Tile tile = new RandomTileIn(box, closed).resolve(7);

		assertEquals(box.centreX(), tile.x());
		assertEquals(box.centreY(), tile.y());
		assertEquals(box.plane(), tile.plane());
	}

	@Test
	void whenTheCentreIsBlockedTheNearestWalkableTileOutwardIsUsed() {
		Location box = new Location("square", LocationKind.TREE, 3200, 3200, 0, 5, 5, null);
		final int centreX = box.centreX(), centreY = box.centreY();
		// The ring just outside the centre is walkable; the centre itself is not.
		RandomTileIn.Walkable ring = new RandomTileIn.Walkable() {
			@Override
			public boolean test(int x, int y, int plane) {
				if (x == centreX && y == centreY) {
					return false;
				}
				return Math.max(Math.abs(x - centreX), Math.abs(y - centreY)) == 1;
			}
		};

		Tile tile = new RandomTileIn(box, ring).resolve(3);

		assertTrue(Math.max(Math.abs(tile.x() - centreX), Math.abs(tile.y() - centreY)) == 1,
				"the fallback spirals outward and takes the first walkable tile, got " + tile);
	}

	@Test
	void theFallbackIsSeedIndependentSoAFailureCannotLookLikeSuccess() {
		RandomTileIn.Walkable closed = new RandomTileIn.Walkable() {
			@Override
			public boolean test(int x, int y, int plane) {
				return false;
			}
		};
		RandomTileIn waypoint = new RandomTileIn(oaks(), closed);

		assertEquals(waypoint.resolve(1), waypoint.resolve(9999),
				"every seed reaches the same fallback, which is the point of a fallback");
	}

	@Test
	void theRetryBudgetIsBoundedSoAMostlyBlockedBoxCannotSpin() {
		final int retries = 5;
		final int[] calls = { 0 };
		RandomTileIn.Walkable counting = new RandomTileIn.Walkable() {
			@Override
			public boolean test(int x, int y, int plane) {
				calls[0]++;
				return false;
			}
		};

		new RandomTileIn(new Location("box", LocationKind.TREE, 3200, 3200, 0, 4, 4, null), counting,
				retries).resolve(1);

		// 5 random rolls, then the deterministic fallback: the centre, then the spiral.
		assertTrue(calls[0] < 100, "the search must be bounded, made " + calls[0] + " checks");
	}

	@Test
	void aOneTileRegionResolvesToThatTile() {
		Location point = Location.point("lumbridge_altar", LocationKind.PRAYER, 3243, 3207, 0);

		Tile tile = new RandomTileIn(point, OPEN).resolve(42);

		assertEquals(3243, tile.x());
		assertEquals(3207, tile.y());
	}

	@Test
	void aFixedPointIgnoresTheSeedSoEveryBotGoesToTheSameTile() {
		FixedPoint waypoint = new FixedPoint(3091, 3243, 0);

		assertEquals(Tile.of(3091, 3243, 0), waypoint.resolve(1));
		assertEquals(Tile.of(3091, 3243, 0), waypoint.resolve(999));
	}

	@Test
	void theWalkBlockMaskIsTheUnionOfTheDirectionalMasks() {
		// Pinned because the whole "is this tile walkable" answer rests on it, and it is the sort of
		// constant that a tidy-up would happily change.
		assertEquals(0x12801FF, RandomTileIn.WALK_BLOCK_MASK);
	}
}
