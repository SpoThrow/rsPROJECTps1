package server.game.players;

import server.clip.region.Region;
import server.clip.region.SmartPathFinder;
import server.game.npcs.NPC;
import server.world.TileControl;

public class PathFinder {

	private static final PathFinder pathFinder = new PathFinder();

	public static PathFinder getPathFinder() {
		return pathFinder;
	}

	public PathFinder() {
	}
	
/** @deprecated Walk-mask geometric LoS; unused. Prefer {@link #hasLineOfSight}. */
public static boolean pathBlocked(NPC attacker, Client victim) {
		
		double offsetX = Math.abs(attacker.absX - victim.position.absX);
		double offsetY = Math.abs(attacker.absY - victim.position.absY);
		
		int distance = TileControl.calculateDistance(attacker, victim);
		
		if (distance == 0) {
			return true;
		}
		
		offsetX = offsetX > 0 ? offsetX / distance : 0;
		offsetY = offsetY > 0 ? offsetY / distance : 0;

		int[][] path = new int[distance][5];
		
		int curX = attacker.absX;
		int curY = attacker.absY;
		int next = 0;
		int nextMoveX = 0;
		int nextMoveY = 0;
		
		double currentTileXCount = 0.0;
		double currentTileYCount = 0.0;

		while(distance > 0) {
			distance--;
			nextMoveX = 0;
			nextMoveY = 0;
			if (curX > victim.position.absX) {
				currentTileXCount += offsetX;
				if (currentTileXCount >= 1.0) {
					nextMoveX--;
					curX--;	
					currentTileXCount -= offsetX;
				}		
			} else if (curX < victim.position.absX) {
				currentTileXCount += offsetX;
				if (currentTileXCount >= 1.0) {
					nextMoveX++;
					curX++;
					currentTileXCount -= offsetX;
				}
			}
			if (curY > victim.position.absY) {
				currentTileYCount += offsetY;
				if (currentTileYCount >= 1.0) {
					nextMoveY--;
					curY--;	
					currentTileYCount -= offsetY;
				}	
			} else if (curY < victim.position.absY) {
				currentTileYCount += offsetY;
				if (currentTileYCount >= 1.0) {
					nextMoveY++;
					curY++;
					currentTileYCount -= offsetY;
				}
			}
			path[next][0] = curX;
			path[next][1] = curY;
			path[next][2] = attacker.heightLevel;//getHeightLevel();
			path[next][3] = nextMoveX;
			path[next][4] = nextMoveY;
			next++;	
		}
		for (int i = 0; i < path.length; i++) {
			if (!Region./*getSingleton().*/getClipping(path[i][0], path[i][1], path[i][2], path[i][3], path[i][4])) { // clipped projectiles by aleksandr
				return true;	
			}
		}
		return false;
	}
	
	// Clipping
		public static boolean pathBlocked(Client attacker, Client victim) {
			
			double offsetX = Math.abs(attacker.position.absX - victim.position.absX);
			double offsetY = Math.abs(attacker.position.absY - victim.position.absY);
			
			int distance = TileControl.calculateDistance(attacker, victim);
			
			if (distance == 0) {
				return true;
			}
			
			offsetX = offsetX > 0 ? offsetX / distance : 0;
			offsetY = offsetY > 0 ? offsetY / distance : 0;

			int[][] path = new int[distance][5];
			
			int curX = attacker.position.absX;
			int curY = attacker.position.absY;
			int next = 0;
			int nextMoveX = 0;
			int nextMoveY = 0;
			
			double currentTileXCount = 0.0;
			double currentTileYCount = 0.0;

			while(distance > 0) {
				distance--;
				nextMoveX = 0;
				nextMoveY = 0;
				if (curX > victim.position.absX) {
					currentTileXCount += offsetX;
					if (currentTileXCount >= 1.0) {
						nextMoveX--;
						curX--;	
						currentTileXCount -= offsetX;
					}		
				} else if (curX < victim.position.absX) {
					currentTileXCount += offsetX;
					if (currentTileXCount >= 1.0) {
						nextMoveX++;
						curX++;
						currentTileXCount -= offsetX;
					}
				}
				if (curY > victim.position.absY) {
					currentTileYCount += offsetY;
					if (currentTileYCount >= 1.0) {
						nextMoveY--;
						curY--;	
						currentTileYCount -= offsetY;
					}	
				} else if (curY < victim.position.absY) {
					currentTileYCount += offsetY;
					if (currentTileYCount >= 1.0) {
						nextMoveY++;
						curY++;
						currentTileYCount -= offsetY;
					}
				}
				path[next][0] = curX;
				path[next][1] = curY;
				path[next][2] = attacker.position.heightLevel;//getHeightLevel();
				path[next][3] = nextMoveX;
				path[next][4] = nextMoveY;
				next++;	
			}
			for (int i = 0; i < path.length; i++) {
				if (!Region./*getSingleton().*/getClipping(path[i][0], path[i][1], path[i][2], path[i][3], path[i][4])) {
					return true;	
				}
			}
			return false;
		}

	/**
	 * True when a projectile-solid wall/object sits between the two tiles.
	 * Uses {@link Region#canProjectileStep} (0x20000), not walk masks — water and
	 * low barriers do not block. The final step onto the target ignores dest solid
	 * so you can shoot AT a target on/near scenery, but intervening solids still block.
	 */
	public static boolean lineBlocked(int x1, int y1, int x2, int y2, int z) {
		if (x1 == x2 && y1 == y2) {
			return false;
		}
		int dx = Math.abs(x2 - x1);
		int dy = Math.abs(y2 - y1);
		int sx = x1 < x2 ? 1 : -1;
		int sy = y1 < y2 ? 1 : -1;
		int err = dx - dy;
		int x = x1;
		int y = y1;
		int guard = dx + dy + 2;
		while (guard-- > 0) {
			if (x == x2 && y == y2) {
				return false;
			}
			int e2 = 2 * err;
			int stepX = 0;
			int stepY = 0;
			if (e2 > -dy) {
				err -= dy;
				stepX = sx;
			}
			if (e2 < dx) {
				err += dx;
				stepY = sy;
			}
			if (stepX == 0 && stepY == 0) {
				return true;
			}
			int nx = x + stepX;
			int ny = y + stepY;
			boolean lastStep = (nx == x2 && ny == y2);
			if (!Region.canProjectileStep(x, y, stepX, stepY, z, lastStep)) {
				return true;
			}
			x = nx;
			y = ny;
		}
		return true;
	}

	/** True when any tile of the attacker can see any tile of the target. */
	public static boolean hasLineOfSight(int fromX, int fromY, int fromSize, int toX, int toY, int toSize, int height) {
		if (fromSize < 1) {
			fromSize = 1;
		}
		if (toSize < 1) {
			toSize = 1;
		}
		for (int fx = 0; fx < fromSize; fx++) {
			for (int fy = 0; fy < fromSize; fy++) {
				for (int tx = 0; tx < toSize; tx++) {
					for (int ty = 0; ty < toSize; ty++) {
						if (!lineBlocked(fromX + fx, fromY + fy, toX + tx, toY + ty, height)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public void findRoute(Client c, int destX, int destY, boolean moveNear,
			int xLength, int yLength) {
		if (c == null) {
			return;
		}
		if (destX == c.position.absX && destY == c.position.absY && !moveNear) {
			return;
		}
		if (!c.walkRepath.walkRepathPending) {
			c.walkRepath.lastWalkDestX = destX;
			c.walkRepath.lastWalkDestY = destY;
		}
		int[][] path = SmartPathFinder.get().route(c.position.absX, c.position.absY, destX, destY, c.position.heightLevel,
				moveNear, xLength, yLength);
		applyRoute(c, path);
	}

	/**
	 * Shortest walk to a tile within {@code range} that has line of sight to the
	 * target footprint. Used when a wall blocks the current firing position.
	 */
	public int[] findShootingTile(Client c, int destX, int destY, int destSize, int range) {
		if (c == null || range < 1) {
			return null;
		}
		if (destSize < 1) {
			destSize = 1;
		}
		if (hasLineOfSight(c.position.absX, c.position.absY, 1, destX, destY, destSize, c.position.heightLevel)
				&& inChebyshevRange(c.position.absX, c.position.absY, destX, destY, destSize, range)) {
			return new int[] { c.position.absX, c.position.absY };
		}
		final int limit = 64;
		boolean[][] seen = new boolean[limit * 2 + 1][limit * 2 + 1];
		int[] qx = new int[limit * limit];
		int[] qy = new int[limit * limit];
		int read = 0;
		int write = 0;
		qx[write] = c.position.absX;
		qy[write] = c.position.absY;
		write++;
		seen[limit][limit] = true;
		while (read < write) {
			int x = qx[read];
			int y = qy[read];
			read++;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					if (dx == 0 && dy == 0) {
						continue;
					}
					int nx = x + dx;
					int ny = y + dy;
					int sx = nx - c.position.absX + limit;
					int sy = ny - c.position.absY + limit;
					if (sx < 0 || sy < 0 || sx >= seen.length || sy >= seen[0].length || seen[sx][sy]) {
						continue;
					}
					if (!SmartPathFinder.canStep(x, y, dx, dy, c.position.heightLevel)) {
						continue;
					}
					seen[sx][sy] = true;
					if (inChebyshevRange(nx, ny, destX, destY, destSize, range)
							&& hasLineOfSight(nx, ny, 1, destX, destY, destSize, c.position.heightLevel)) {
						return new int[] { nx, ny };
					}
					if (write < qx.length) {
						qx[write] = nx;
						qy[write] = ny;
						write++;
					}
				}
			}
		}
		return null;
	}

	private static boolean inChebyshevRange(int px, int py, int nx, int ny, int size, int range) {
		int closestX = px;
		if (px < nx) {
			closestX = nx;
		} else if (px > nx + size - 1) {
			closestX = nx + size - 1;
		}
		int closestY = py;
		if (py < ny) {
			closestY = ny;
		} else if (py > ny + size - 1) {
			closestY = ny + size - 1;
		}
		return Math.abs(px - closestX) <= range && Math.abs(py - closestY) <= range;
	}

	private void applyRoute(Client c, int[][] path) {
		// Replace the queue. Appending onto an in-progress route leaves the
		// previous steps (often toward an NPC) in front of the new click, so
		// the server keeps walking at the enemy while the client runs away.
		c.resetWalkingQueue();
		if (path == null || path.length == 0) {
			return;
		}
		for (int i = 0; i < path.length; i++) {
			c.addToWalkingQueue(localize(path[i][0], c.getMapRegionX()),
					localize(path[i][1], c.getMapRegionY()));
		}
	}

	public int localize(int x, int mapRegion) {
		return x - 8 * mapRegion;
	}

}
