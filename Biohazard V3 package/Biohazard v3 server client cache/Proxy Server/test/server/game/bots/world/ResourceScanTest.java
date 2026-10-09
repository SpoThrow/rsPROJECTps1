package server.game.bots.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import server.game.objects.Objects;

/**
 * The object scan behind the generic gather leaf, tested the way {@code ScannedLocator} is: an injected
 * source and classifier, so the subject is the search and not the world.
 *
 * <p>What is worth pinning here is what a gather leaf depends on: that the nearest object wins, that
 * the wrong kind, the wrong plane, the removed tombstones and anything past the radius are all ignored,
 * and that no match is an empty answer rather than a throw.
 */
class ResourceScanTest {

	private static final int OAK = 1276;
	private static final int ROCK = 2091;
	private static final int REMOVED = -1;

	/** A world with these objects in the region at 3200,3200 and nothing anywhere else. */
	private static ResourceScan world(Objects... objects) {
		final List<Objects> region = new ArrayList<Objects>(Arrays.asList(objects));
		return ResourceScan.with(new ScannedLocator.RegionSource() {
			@Override
			public List<Objects> objects(int baseX, int baseY) {
				return baseX == 3200 && baseY == 3200 ? region : Collections.<Objects>emptyList();
			}
		}, new ScannedLocator.KindSource() {
			@Override
			public String kindOf(int objectId) {
				if (objectId == OAK) {
					return "tree";
				}
				return objectId == ROCK ? "rock" : null;
			}
		});
	}

	private static Objects at(int id, int x, int y) {
		return new Objects(id, x, y, 0, 0, 10);
	}

	@Test
	void theNearestObjectOfTheKindWins() {
		ResourceScan scan = world(at(OAK, 3210, 3200), at(OAK, 3205, 3200), at(OAK, 3230, 3200));

		List<ObjectTarget> found = scan.nearest(3200, 3200, 0, LocationKind.TREE, 40, 3);

		assertEquals(3, found.size());
		assertEquals(3205, found.get(0).x(), "closest first");
		assertEquals(3210, found.get(1).x());
		assertEquals(3230, found.get(2).x());
		assertEquals(OAK, found.get(0).objectId(), "the id is what an interaction needs, so it is kept");
	}

	@Test
	void otherKindsAreIgnored() {
		ResourceScan scan = world(at(ROCK, 3201, 3200), at(OAK, 3203, 3200));

		List<ObjectTarget> found = scan.nearest(3200, 3200, 0, LocationKind.TREE, 20, 5);

		assertEquals(1, found.size(), "the nearer rock is not a tree");
		assertEquals(3203, found.get(0).x());
	}

	@Test
	void anotherPlaneIsNotAnAnswer() {
		ResourceScan scan = world(new Objects(OAK, 3201, 3200, 2, 0, 10));

		assertTrue(scan.nearest(3200, 3200, 0, LocationKind.TREE, 20, 5).isEmpty(),
				"a tree on plane 2 is not the tree this bot can click");
		assertEquals(1, scan.nearest(3200, 3200, 2, LocationKind.TREE, 20, 5).size());
	}

	@Test
	void anythingPastTheRadiusIsNotSearchedFor() {
		ResourceScan scan = world(at(OAK, 3210, 3200));

		assertEquals(1, scan.nearest(3200, 3200, 0, LocationKind.TREE, 10, 5).size());
		assertTrue(scan.nearest(3200, 3200, 0, LocationKind.TREE, 9, 5).isEmpty(),
				"ten tiles away, with a radius of nine");
	}

	@Test
	void aRemovedObjectsTombstoneIsNotAThingToClick() {
		ResourceScan scan = world(at(REMOVED, 3201, 3200), at(OAK, 3202, 3200));

		List<ObjectTarget> found = scan.nearest(3200, 3200, 0, LocationKind.TREE, 20, 5);

		assertEquals(1, found.size(), "Region leaves a negative-id tombstone where an object was removed");
		assertEquals(OAK, found.get(0).objectId());
	}

	@Test
	void theSingularLookupIsTheOneCallAGatherLeafMakes() {
		assertEquals(3205, world(at(OAK, 3205, 3200)).nearest(3200, 3200, 0, LocationKind.TREE, 8).x());
		assertNull(world(at(ROCK, 3201, 3200)).nearest(3200, 3200, 0, LocationKind.TREE, 8),
				"nothing of the kind is null, not an exception: the leaf reports FAILURE from it");
	}

	@Test
	void aLimitAndAnEmptyWorldAreBothHandled() {
		ResourceScan scan = world(at(OAK, 3201, 3200), at(OAK, 3202, 3200));

		assertEquals(1, scan.nearest(3200, 3200, 0, LocationKind.TREE, 20, 1).size());
		assertTrue(scan.nearest(3200, 3200, 0, LocationKind.TREE, 20, 0).isEmpty(),
				"a page size of zero is not a request for everything");
		assertTrue(world().nearest(3200, 3200, 0, LocationKind.TREE, 20, 5).isEmpty());
	}
}
