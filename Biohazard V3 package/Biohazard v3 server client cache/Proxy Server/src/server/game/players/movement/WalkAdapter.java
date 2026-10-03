package server.game.players.movement;

import server.clip.region.SmartPathFinder;
import server.game.players.Client;
import server.game.players.PathFinder;

/**
 * Thin adapter that feeds SmartPathFinder routes into the existing walking
 * queue. Does not replace the queue — only computes waypoints.
 * <p>
 * Rollback: restore PlayerAssistant.walkTo* bodies to raw newWalkCmd injection.
 */
public final class WalkAdapter {

	private WalkAdapter() {
	}

	public static void walkAbsolute(Client c, int x, int y) {
		if (c == null) {
			return;
		}
		if (c.freezeTimer > 0) {
			return;
		}
		PathFinder.getPathFinder().findRoute(c, x, y, true, 1, 1);
		if (c.wQueueReadPtr == c.wQueueWritePtr) {
			fallbackStep(c, x - c.position.absX, y - c.position.absY);
		}
	}

	/**
	 * Relative scripted walk (legacy walkTo deltas). Converts to absolute and
	 * BFS-routes; falls back to a single canStep toward the target.
	 */
	public static void walkRelative(Client c, int dx, int dy) {
		if (c == null) {
			return;
		}
		if (c.freezeTimer > 0) {
			return;
		}
		if (dx == 0 && dy == 0) {
			return;
		}
		int destX = c.position.absX + dx;
		int destY = c.position.absY + dy;
		PathFinder.getPathFinder().findRoute(c, destX, destY, true, 1, 1);
		if (c.wQueueReadPtr == c.wQueueWritePtr) {
			fallbackStep(c, dx, dy);
		}
	}

	/**
	 * Same as {@link #walkRelative} but also respects freezeDelay (walkTo2 /
	 * walkToCheck semantics).
	 */
	public static void walkRelativeFrozenCheck(Client c, int dx, int dy) {
		if (c == null) {
			return;
		}
		if (c.freezeTimer > 0 || c.freezeDelay > 0) {
			return;
		}
		walkRelative(c, dx, dy);
	}

	/** walkTo3: relative walk that forces walk (not run). */
	public static void walkRelativeNoRun(Client c, int dx, int dy) {
		if (c == null) {
			return;
		}
		c.isRunning2 = false;
		c.isRunning = false;
		walkRelative(c, dx, dy);
	}

	private static void fallbackStep(Client c, int dx, int dy) {
		int sx = clampUnit(dx);
		int sy = clampUnit(dy);
		if (sx == 0 && sy == 0) {
			return;
		}
		if (!SmartPathFinder.canStep(c.position.absX, c.position.absY, sx, sy, c.position.heightLevel)) {
			if (sx != 0 && SmartPathFinder.canStep(c.position.absX, c.position.absY, sx, 0, c.position.heightLevel)) {
				sy = 0;
			} else if (sy != 0 && SmartPathFinder.canStep(c.position.absX, c.position.absY, 0, sy, c.position.heightLevel)) {
				sx = 0;
			} else {
				return;
			}
		}
		c.resetWalkingQueue();
		c.addToWalkingQueue(c.position.absX - (c.position.mapRegionX * 8) + sx, c.position.absY - (c.position.mapRegionY * 8) + sy);
	}

	private static int clampUnit(int v) {
		if (v < 0) {
			return -1;
		}
		if (v > 0) {
			return 1;
		}
		return 0;
	}
}
