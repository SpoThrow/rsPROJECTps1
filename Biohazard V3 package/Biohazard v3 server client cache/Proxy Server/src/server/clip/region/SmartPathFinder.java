package server.clip.region;

import java.util.Arrays;

/**
 * OSRS-accurate BFS route finder (rsmod RouteFinding / Tomm0017's standalone
 * pathfinder), using this server's 317 collision masks.
 */
public final class SmartPathFinder {

	private static final int SEARCH = 128;
	private static final int RING = 4096;
	private static final int RING_MASK = RING - 1;
	private static final int UNSET = 99999999;
	private static final int SRC_DIR = 99;
	private static final int ALT_COST_CAP = 1000;
	private static final int ALT_DIST_CAP = 100;
	private static final int ALT_RANGE = 10;
	private static final int MAX_WAYPOINTS = 25;

	private static final int DIR_NORTH = 0x1;
	private static final int DIR_EAST = 0x2;
	private static final int DIR_SOUTH = 0x4;
	private static final int DIR_WEST = 0x8;

	private static final SmartPathFinder INSTANCE = new SmartPathFinder();

	private final int[] directions = new int[SEARCH * SEARCH];
	private final int[] distances = new int[SEARCH * SEARCH];
	private final int[] bufX = new int[RING];
	private final int[] bufY = new int[RING];
	private int read;
	private int write;

	public static SmartPathFinder get() {
		return INSTANCE;
	}

	/** Occupancy bits: solid object / projectile-solid. Walls are kept. */
	private static final int OCCUPANT = 0x100 | 0x20000;

	public static boolean canStep(int fromX, int fromY, int dx, int dy, int z) {
		return checkStep(fromX, fromY, dx, dy, z, false);
	}

	/**
	 * True when the edge from from→to is not blocked by a wall. Solid objects on
	 * the destination tile are ignored so NPCs and booths remain approachable.
	 */
	public static boolean canReach(int fromX, int fromY, int toX, int toY, int z) {
		int dx = toX - fromX;
		int dy = toY - fromY;
		if (dx < -1 || dx > 1 || dy < -1 || dy > 1 || (dx == 0 && dy == 0)) {
			return false;
		}
		return checkStep(fromX, fromY, dx, dy, z, true);
	}

	private static boolean checkStep(int fromX, int fromY, int dx, int dy, int z, boolean ignoreOccupant) {
		int toX = fromX + dx;
		int toY = fromY + dy;
		int to = clipMask(toX, toY, z, ignoreOccupant);
		int fromSideX = clipMask(toX, fromY, z, ignoreOccupant);
		int fromSideY = clipMask(fromX, toY, z, ignoreOccupant);
		if (dx == -1 && dy == 0) {
			return (to & 0x1280108) == 0;
		}
		if (dx == 1 && dy == 0) {
			return (to & 0x1280180) == 0;
		}
		if (dx == 0 && dy == -1) {
			return (to & 0x1280102) == 0;
		}
		if (dx == 0 && dy == 1) {
			return (to & 0x1280120) == 0;
		}
		if (dx == -1 && dy == -1) {
			return (to & 0x128010e) == 0 && (fromSideX & 0x1280108) == 0 && (fromSideY & 0x1280102) == 0;
		}
		if (dx == 1 && dy == -1) {
			return (to & 0x1280183) == 0 && (fromSideX & 0x1280180) == 0 && (fromSideY & 0x1280102) == 0;
		}
		if (dx == -1 && dy == 1) {
			return (to & 0x1280138) == 0 && (fromSideX & 0x1280108) == 0 && (fromSideY & 0x1280120) == 0;
		}
		if (dx == 1 && dy == 1) {
			return (to & 0x12801e0) == 0 && (fromSideX & 0x1280180) == 0 && (fromSideY & 0x1280120) == 0;
		}
		return false;
	}

	private static int clipMask(int x, int y, int z, boolean ignoreOccupant) {
		int clip = Region.getClipping(x, y, z);
		return ignoreOccupant ? (clip & ~OCCUPANT) : clip;
	}

	/** True when standing on (x,y) can interact with any tile of the footprint. */
	public static boolean canReachFootprint(int x, int y, int destX, int destY, int destW, int destH, int z) {
		if (destW < 1) {
			destW = 1;
		}
		if (destH < 1) {
			destH = 1;
		}
		for (int tx = destX; tx < destX + destW; tx++) {
			for (int ty = destY; ty < destY + destH; ty++) {
				if (canReach(x, y, tx, ty, z)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isStandable(int x, int y, int z) {
		return (Region.getClipping(x, y, z) & OCCUPANT) == 0;
	}

	/**
	 * @return compressed waypoints in world coords, or null if no route.
	 */
	public int[][] route(int srcX, int srcY, int destX, int destY, int z, boolean moveNear,
			int destW, int destH) {
		if (destW < 1) {
			destW = 1;
		}
		if (destH < 1) {
			destH = 1;
		}
		reset();
		int baseX = srcX - SEARCH / 2;
		int baseY = srcY - SEARCH / 2;
		int localSrcX = srcX - baseX;
		int localSrcY = srcY - baseY;
		int localDestX = destX - baseX;
		int localDestY = destY - baseY;
		if (localDestX < 0 || localDestY < 0 || localDestX >= SEARCH || localDestY >= SEARCH) {
			if (!moveNear) {
				return null;
			}
		}
		boolean approachGoal = moveNear && !footprintStandable(destX, destY, destW, destH, z);
		append(localSrcX, localSrcY, SRC_DIR, 0);
		int[] end = search(baseX, baseY, z, localDestX, localDestY, destW, destH, approachGoal);
		if (end == null) {
			if (!moveNear) {
				return null;
			}
			end = closest(baseX, baseY, z, localDestX, localDestY, destW, destH);
			if (end == null) {
				return null;
			}
		}
		return pack(baseX, baseY, localSrcX, localSrcY, end[0], end[1]);
	}

	private static boolean footprintStandable(int destX, int destY, int destW, int destH, int z) {
		for (int x = destX; x < destX + destW; x++) {
			for (int y = destY; y < destY + destH; y++) {
				if (isStandable(x, y, z)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean reached(int x, int y, int destX, int destY, int destW, int destH) {
		return x >= destX && x < destX + destW && y >= destY && y < destY + destH;
	}

	private int[] search(int baseX, int baseY, int z, int destX, int destY, int destW, int destH,
			boolean approachGoal) {
		int last = SEARCH - 1;
		while (write != read) {
			int cx = bufX[read];
			int cy = bufY[read];
			read = (read + 1) & RING_MASK;
			if (reached(cx, cy, destX, destY, destW, destH)) {
				return new int[] { cx, cy };
			}
			if (approachGoal && canReachFootprint(baseX + cx, baseY + cy, baseX + destX, baseY + destY,
					destW, destH, z)) {
				return new int[] { cx, cy };
			}
			int next = distances[index(cx, cy)] + 1;
			int x;
			int y;
			x = cx - 1;
			y = cy;
			if (cx > 0 && directions[index(x, y)] == 0 && (clip(baseX + x, baseY + y, z) & 0x1280108) == 0) {
				append(x, y, DIR_EAST, next);
			}
			x = cx + 1;
			y = cy;
			if (cx < last && directions[index(x, y)] == 0 && (clip(baseX + x, baseY + y, z) & 0x1280180) == 0) {
				append(x, y, DIR_WEST, next);
			}
			x = cx;
			y = cy - 1;
			if (cy > 0 && directions[index(x, y)] == 0 && (clip(baseX + x, baseY + y, z) & 0x1280102) == 0) {
				append(x, y, DIR_NORTH, next);
			}
			x = cx;
			y = cy + 1;
			if (cy < last && directions[index(x, y)] == 0 && (clip(baseX + x, baseY + y, z) & 0x1280120) == 0) {
				append(x, y, DIR_SOUTH, next);
			}
			x = cx - 1;
			y = cy - 1;
			if (cx > 0 && cy > 0 && directions[index(x, y)] == 0
					&& (clip(baseX + x, baseY + y, z) & 0x128010e) == 0
					&& (clip(baseX + x, baseY + cy, z) & 0x1280108) == 0
					&& (clip(baseX + cx, baseY + y, z) & 0x1280102) == 0) {
				append(x, y, DIR_EAST | DIR_NORTH, next);
			}
			x = cx + 1;
			y = cy - 1;
			if (cx < last && cy > 0 && directions[index(x, y)] == 0
					&& (clip(baseX + x, baseY + y, z) & 0x1280183) == 0
					&& (clip(baseX + x, baseY + cy, z) & 0x1280180) == 0
					&& (clip(baseX + cx, baseY + y, z) & 0x1280102) == 0) {
				append(x, y, DIR_WEST | DIR_NORTH, next);
			}
			x = cx - 1;
			y = cy + 1;
			if (cx > 0 && cy < last && directions[index(x, y)] == 0
					&& (clip(baseX + x, baseY + y, z) & 0x1280138) == 0
					&& (clip(baseX + x, baseY + cy, z) & 0x1280108) == 0
					&& (clip(baseX + cx, baseY + y, z) & 0x1280120) == 0) {
				append(x, y, DIR_EAST | DIR_SOUTH, next);
			}
			x = cx + 1;
			y = cy + 1;
			if (cx < last && cy < last && directions[index(x, y)] == 0
					&& (clip(baseX + x, baseY + y, z) & 0x12801e0) == 0
					&& (clip(baseX + x, baseY + cy, z) & 0x1280180) == 0
					&& (clip(baseX + cx, baseY + y, z) & 0x1280120) == 0) {
				append(x, y, DIR_WEST | DIR_SOUTH, next);
			}
		}
		return null;
	}

	private int[] closest(int baseX, int baseY, int z, int destX, int destY, int width, int length) {
		int lowest = ALT_COST_CAP;
		int bestDist = ALT_DIST_CAP;
		int bestX = -1;
		int bestY = -1;
		for (int x = destX - ALT_RANGE; x <= destX + ALT_RANGE; x++) {
			for (int y = destY - ALT_RANGE; y <= destY + ALT_RANGE; y++) {
				if (x < 0 || y < 0 || x >= SEARCH || y >= SEARCH) {
					continue;
				}
				int dist = distances[index(x, y)];
				if (dist >= ALT_DIST_CAP) {
					continue;
				}
				// Skip the far side of a wall: only tiles that can actually reach the target.
				if (!canReachFootprint(baseX + x, baseY + y, baseX + destX, baseY + destY, width, length, z)) {
					continue;
				}
				int dx = 0;
				if (x < destX) {
					dx = destX - x;
				} else if (x > destX + width - 1) {
					dx = x - (destX + width - 1);
				}
				int dy = 0;
				if (y < destY) {
					dy = destY - y;
				} else if (y > destY + length - 1) {
					dy = y - (destY + length - 1);
				}
				int cost = dx * dx + dy * dy;
				if (cost < lowest || (cost == lowest && dist < bestDist)) {
					bestX = x;
					bestY = y;
					lowest = cost;
					bestDist = dist;
				}
			}
		}
		if (lowest == ALT_COST_CAP) {
			return null;
		}
		return new int[] { bestX, bestY };
	}

	private int[][] pack(int baseX, int baseY, int srcX, int srcY, int endX, int endY) {
		int[] wx = new int[MAX_WAYPOINTS + 1];
		int[] wy = new int[MAX_WAYPOINTS + 1];
		int count = 0;
		int cx = endX;
		int cy = endY;
		int nextDir = directions[index(cx, cy)];
		int currDir = -1;
		int guard = SEARCH * SEARCH;
		while (cx != srcX || cy != srcY) {
			if (nextDir == 0 || guard-- <= 0) {
				return null;
			}
			if (currDir != nextDir) {
				currDir = nextDir;
				if (count == MAX_WAYPOINTS) {
					for (int i = 0; i < count - 1; i++) {
						wx[i] = wx[i + 1];
						wy[i] = wy[i + 1];
					}
					count--;
				}
				wx[count] = baseX + cx;
				wy[count] = baseY + cy;
				count++;
			}
			if ((currDir & DIR_EAST) != 0) {
				cx++;
			} else if ((currDir & DIR_WEST) != 0) {
				cx--;
			}
			if ((currDir & DIR_NORTH) != 0) {
				cy++;
			} else if ((currDir & DIR_SOUTH) != 0) {
				cy--;
			}
			if (cx < 0 || cy < 0 || cx >= SEARCH || cy >= SEARCH) {
				return null;
			}
			nextDir = directions[index(cx, cy)];
		}
		if (count == 0) {
			return new int[0][];
		}
		int[][] path = new int[count][2];
		for (int i = 0; i < count; i++) {
			int src = count - 1 - i;
			path[i][0] = wx[src];
			path[i][1] = wy[src];
		}
		return path;
	}

	private void append(int x, int y, int dir, int distance) {
		int next = (write + 1) & RING_MASK;
		if (next == read) {
			return;
		}
		int i = index(x, y);
		directions[i] = dir;
		distances[i] = distance;
		bufX[write] = x;
		bufY[write] = y;
		write = next;
	}

	private void reset() {
		Arrays.fill(directions, 0);
		Arrays.fill(distances, UNSET);
		read = 0;
		write = 0;
	}

	private static int index(int x, int y) {
		return x * SEARCH + y;
	}

	private static int clip(int x, int y, int z) {
		return Region.getClipping(x, y, z);
	}
}
