package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Pins the position cluster after Phase 4.7 moved it off {@link Player} into
 * {@link Position} ({@code player.position}) and {@link WalkRepath}
 * ({@code player.walkRepath}).
 *
 * <p>The defaults are the load-bearing part, and specifically the two that are not zero:
 * {@code teleportToX} and {@code teleportToY} were declared {@code = -1}, and every reader
 * treats {@code -1} as "no teleport queued". A {@code 0} default would read as a request to
 * teleport the player to coordinate 0, so it is pinned separately from the rest.
 *
 * <p>Also pinned: that {@code getX()}/{@code getY()} still agree with {@code absX}/{@code absY}
 * now that those live one level down. They are the seam the rest of the tree should migrate to,
 * so they are worth a test that fails if the move ever breaks the link.
 */
class PositionTest {

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void everyFieldKeepsItsOldDefault() {
		final Position position = new Position();

		assertEquals(0, position.mapRegionX, "mapRegionX was uninitialised");
		assertEquals(0, position.mapRegionY, "mapRegionY was uninitialised");
		assertEquals(0, position.absX, "absX was uninitialised");
		assertEquals(0, position.absY, "absY was uninitialised");
		assertEquals(0, position.currentX, "currentX was uninitialised");
		assertEquals(0, position.currentY, "currentY was uninitialised");
		assertEquals(0, position.heightLevel, "heightLevel was uninitialised");
		assertEquals(-1, position.teleportToX, "teleportToX was initialised to -1");
		assertEquals(-1, position.teleportToY, "teleportToY was initialised to -1");
	}

	@Test
	void aFreshPositionHasNoPendingTeleport() {
		// -1, not 0. The walk loop tests these against -1 to decide whether a teleport is
		// queued, so a 0 default would look like a live request to move to coordinate 0.
		assertEquals(-1, new Position().teleportToX);
		assertEquals(-1, new Position().teleportToY);
	}

	@Test
	void theRepathDestinationDefaultsToNone() {
		final WalkRepath repath = new WalkRepath();

		assertEquals(-1, repath.lastWalkDestX, "lastWalkDestX was initialised to -1");
		assertEquals(-1, repath.lastWalkDestY, "lastWalkDestY was initialised to -1");
		assertFalse(repath.walkRepathPending, "walkRepathPending was uninitialised");
	}

	@Test
	void theGettersReadThroughToTheNewFields() {
		final Client c = client();

		c.position.absX = 3000;
		c.position.absY = 3100;

		assertEquals(3000, c.getX(), "getX no longer agrees with position.absX");
		assertEquals(3100, c.getY(), "getY no longer agrees with position.absY");
	}

	@Test
	void eachPlayerOwnsItsOwnPosition() {
		final Client a = client();
		final Client b = client();

		assertSame(a.position, a.position, "the same player must keep one position object");
		assertNotSame(a.position, b.position, "two players must not share a position");
		assertNotSame(a.walkRepath, b.walkRepath, "two players must not share repath state");

		// Snapshot b first, so the assertions do not depend on what a fresh Client's
		// position happens to hold.
		final int bX = b.position.absX;
		final int bY = b.position.absY;
		final int bZ = b.position.heightLevel;
		final int bTeleportX = b.position.teleportToX;
		final int bDestX = b.walkRepath.lastWalkDestX;
		final boolean bPending = b.walkRepath.walkRepathPending;

		a.position.absX = 12345;
		a.position.absY = 12346;
		a.position.heightLevel = 7;
		a.position.teleportToX = 999;
		a.walkRepath.lastWalkDestX = 888;
		a.walkRepath.walkRepathPending = true;

		assertEquals(bX, b.position.absX, "position leaked between players");
		assertEquals(bY, b.position.absY, "position leaked between players");
		assertEquals(bZ, b.position.heightLevel, "position leaked between players");
		assertEquals(bTeleportX, b.position.teleportToX, "position leaked between players");
		assertEquals(bDestX, b.walkRepath.lastWalkDestX, "repath state leaked between players");
		assertEquals(bPending, b.walkRepath.walkRepathPending, "repath state leaked between players");
	}
}
