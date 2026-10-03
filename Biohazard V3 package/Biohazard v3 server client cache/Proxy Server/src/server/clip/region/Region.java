package server.clip.region;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import server.Config;
import server.game.items.GroundItem;
import server.game.objects.Objects;


public class Region {
	
	private static Region[] regions;
	private int id;
	private int[][][] clips = new int[4][][];
	private boolean members = false;
	public boolean itemProcessing = false;
	public static boolean startup = false;
	public List<GroundItem> floorItems = new ArrayList<GroundItem>();
	public ArrayList<Objects> realObjects = new ArrayList<Objects>();

	public Region(int id, boolean members) {
		this.id = id;
		this.members = members;
	}

	public int id() {
		return id;
	}

	public boolean members()
	{
		return members;
	}

	public static Region getRegion(int x, int y) {
		int regionX = x >> 3;
		int regionY = y >> 3;
		int regionId = (regionX / 8 << 8) + regionY / 8;
		for (Region region : regions) {
			if (region.id() == regionId) {
				return region;
			}
		}
		return null;
	}

	public static boolean objectExists(int id, int x, int y, int z) {
		Region r = getRegion(x, y);
		if (r == null)
			return false;
		for (Objects o : r.realObjects) {
			if (o.objectId == id) {
				if(o.objectId == 28122 || o.objectId == 11214 || o.objectId == 9357) {
					if (o.objectX == x && o.objectY == y) {
						return true;
					}
				}
				if (o.objectX == x && o.objectY == y && o.objectHeight == z) {
					return true;
				}
			}
		}
		return false;
	}
	
	/** Projectile-impenetrable bit set when ObjectDef.solid() is true at load. */
	private static final int PROJECTILE = 0x20000;

	/**
	 * True when a projectile can take one step (dx,dy) from (fromX,fromY).
	 * Uses projectile flags only — walk-blocked water/low fences do not block.
	 * @param ignoreDestSolid when true, destination tile solid does not block
	 *        (shoot AT a target standing on/near solid scenery).
	 */
	public static boolean canProjectileStep(int fromX, int fromY, int dx, int dy, int height,
			boolean ignoreDestSolid) {
		try {
			if (height > 3) {
				height = 0;
			}
			if (dx < -1 || dx > 1 || dy < -1 || dy > 1 || (dx == 0 && dy == 0)) {
				return false;
			}
			int toX = fromX + dx;
			int toY = fromY + dy;
			int to = getClipping(toX, toY, height);
			int sideX = getClipping(toX, fromY, height);
			int sideY = getClipping(fromX, toY, height);
			if (ignoreDestSolid) {
				to &= ~PROJECTILE;
			}
			if (dx == -1 && dy == 0) {
				return (to & PROJECTILE) == 0;
			}
			if (dx == 1 && dy == 0) {
				return (to & PROJECTILE) == 0;
			}
			if (dx == 0 && dy == -1) {
				return (to & PROJECTILE) == 0;
			}
			if (dx == 0 && dy == 1) {
				return (to & PROJECTILE) == 0;
			}
			// Diagonals: dest + both adjacent sides (same geometry as walk canStep).
			if (dx == -1 && dy == -1) {
				return (to & PROJECTILE) == 0 && (sideX & PROJECTILE) == 0 && (sideY & PROJECTILE) == 0;
			}
			if (dx == 1 && dy == -1) {
				return (to & PROJECTILE) == 0 && (sideX & PROJECTILE) == 0 && (sideY & PROJECTILE) == 0;
			}
			if (dx == -1 && dy == 1) {
				return (to & PROJECTILE) == 0 && (sideX & PROJECTILE) == 0 && (sideY & PROJECTILE) == 0;
			}
			if (dx == 1 && dy == 1) {
				return (to & PROJECTILE) == 0 && (sideX & PROJECTILE) == 0 && (sideY & PROJECTILE) == 0;
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean canProjectileStep(int fromX, int fromY, int dx, int dy, int height) {
		return canProjectileStep(fromX, fromY, dx, dy, height, false);
	}


	public static void addClippingForVariableObject(int x, int y, int height, int type, int direction, boolean flag) {

		if (type == 0) {
			if (direction == 0) {
				addClipping(x, y, height, 128 + (flag ? 0x20000 : 0));
				addClipping(x - 1, y, height, 8 + (flag ? 0x20000 : 0));
			} else if (direction == 1) {
				addClipping(x, y, height, 2 + (flag ? 0x20000 : 0));
				addClipping(x, y + 1, height, 32 + (flag ? 0x20000 : 0));
			} else if (direction == 2) {
				addClipping(x, y, height, 8 + (flag ? 0x20000 : 0));
				addClipping(x + 1, y, height, 128 + (flag ? 0x20000 : 0));
			} else if (direction == 3) {
				addClipping(x, y, height, 32 + (flag ? 0x20000 : 0));
				addClipping(x, y - 1, height, 2 + (flag ? 0x20000 : 0));
			}
		} else if (type == 1 || type == 3) {
			if (direction == 0) {
				addClipping(x, y, height, 1);
				addClipping(x - 1, y, height, 16);
			} else if (direction == 1) {
				addClipping(x, y, height, 4);
				addClipping(x + 1, y + 1, height, 64);
			} else if (direction == 2) {
				addClipping(x, y, height, 16);
				addClipping(x + 1, y - 1, height, 1);
			} else if (direction == 3) {
				addClipping(x, y, height, 64);
				addClipping(x - 1, y - 1, height, 4);
			}
		} else if (type == 2) {
			if (direction == 0) {
				addClipping(x, y, height, 130 + (flag ? 0x20000 : 0));
				addClipping(x - 1, y, height, 8 + (flag ? 0x20000 : 0));
				addClipping(x, y + 1, height, 32 + (flag ? 0x20000 : 0));
			} else if (direction == 1) {
				addClipping(x, y, height, 10 + (flag ? 0x20000 : 0));
				addClipping(x, y + 1, height, 32 + (flag ? 0x20000 : 0));
				addClipping(x + 1, y, height, 128 + (flag ? 0x20000 : 0));
			} else if (direction == 2) {
				addClipping(x, y, height, 40 + (flag ? 0x20000 : 0));
				addClipping(x + 1, y, height, 128 + (flag ? 0x20000 : 0));
				addClipping(x, y - 1, height, 2 + (flag ? 0x20000 : 0));
			} else if (direction == 3) {
				addClipping(x, y, height, 160 + (flag ? 0x20000 : 0));
				addClipping(x, y - 1, height, 2 + (flag ? 0x20000 : 0));
				addClipping(x - 1, y, height, 8 + (flag ? 0x20000 : 0));
			}
		}
		if (flag) {
			if (type == 0) {
				if (direction == 0) {
					addClipping(x, y, height, 65536);
					addClipping(x - 1, y, height, 4096);
				} else if (direction == 1) {
					addClipping(x, y, height, 1024);
					addClipping(x, y + 1, height, 16384);
				} else if (direction == 2) {
					addClipping(x, y, height, 4096);
					addClipping(x + 1, y, height, 65536);
				} else if (direction == 3) {
					addClipping(x, y, height, 16384);
					addClipping(x, y - 1, height, 1024);
				}
			}
			if (type == 1 || type == 3) {
				if (direction == 0) {
					addClipping(x, y, height, 512);
					addClipping(x - 1, y + 1, height, 8192);
				} else if (direction == 1) {
					addClipping(x, y, height, 2048);
					addClipping(x + 1, y + 1, height, 32768);
				} else if (direction == 2) {
					addClipping(x, y, height, 8192);
					addClipping(x + 1, y + 1, height, 512);
				} else if (direction == 3) {
					addClipping(x, y, height, 32768);
					addClipping(x - 1, y - 1, height, 2048);
				}
			} else if (type == 2) {
				if (direction == 0) {
					addClipping(x, y, height, 66560);
					addClipping(x - 1, y, height, 4096);
					addClipping(x, y + 1, height, 16384);
				} else if (direction == 1) {
					addClipping(x, y, height, 5120);
					addClipping(x, y + 1, height, 16384);
					addClipping(x + 1, y, height, 65536);
				} else if (direction == 2) {
					addClipping(x, y, height, 20480);
					addClipping(x + 1, y, height, 65536);
					addClipping(x, y - 1, height, 1024);
				} else if (direction == 3) {
					addClipping(x, y, height, 81920);
					addClipping(x, y - 1, height, 1024);
					addClipping(x - 1, y, height, 4096);
				}
			}
		}
	}

	private static void addClippingForSolidObject(int x, int y, int height, int xLength, int yLength, boolean flag) {
		int clipping = 256;
		if (flag) {
			clipping += 0x20000;
		}
		for (int i = x; i < x + xLength; i++) {
			for (int i2 = y; i2 < y + yLength; i2++) {
				addClipping(i, i2, height, clipping);
			}
		}
	}

	public static void addObject(int objectId, int x, int y, int height, int type, int direction) {
		Region r = Region.getRegion(x, y);
		if (r != null) {
			if (!startup) {
				for (Objects o : r.realObjects) {
					if (o.objectId >= 0) {
						if (o.objectX == x && o.objectY == y && o.objectHeight == height) {
							o.objectId = -1;
							break;
						}
					}
				}
			}
			r.realObjects.add(new Objects(objectId, x, y, height, direction, type));
		}
		if(objectId < 0)
			return;
		ObjectDef def = ObjectDef.getObjectDef(objectId);
		if (def == null) {
			return;
		}
		if(type < 4)
			def.setSolid(objectId);
		// loc.dat has no size for most scenery — thousands of blocking objects read
		// back as 1x1, which is why walking through trees and statues looks wrong.
		// Prefer the authoritative footprints in Data/objectSize.cfg and fall back to
		// the cache only where the table has no entry. The rotation swap is applied
		// to the resolved pair so it still turns a 2x3 into a 3x2.
		int xLength = def.xLength();
		int yLength = def.yLength();
		if (Config.USE_OBJECT_SIZE_TABLE) {
			ObjectSizes sizes = ObjectSizes.get();
			xLength = sizes.width(objectId, xLength);
			yLength = sizes.height(objectId, yLength);
		}
		if (direction == 1 || direction == 3) {
			int swap = xLength;
			xLength = yLength;
			yLength = swap;
		}
		boolean blocksWalk = def.aBoolean767();
		// Projectile solid from ObjectDef.solid(); short-object overrides strip 0x20000 only.
		boolean blocksProjectiles = def.solid() && !projectileSolidOverride(objectId);
		if (type == 22) {
			if (def.hasActions() && blocksWalk) {
				addClipping(x, y, height, 0x200000);
			}
		} else if (type >= 9) {
			if(blocksWalk)
			{
				addClippingForSolidObject(x, y, height, xLength, yLength, blocksProjectiles);
			}
		} else if (type >= 0 && type <= 3) {
			if(def.aBoolean767())
			{
				addClippingForVariableObject(x, y, height, type, direction, blocksProjectiles);
			}
		}
	}

	/**
	 * Short scenery that walks-block but must not block shots (OSRS-style).
	 * Empty until a concrete object fails in-game tests — add IDs here only.
	 */
	private static boolean projectileSolidOverride(int objectId) {
		switch (objectId) {
		// Example (disabled): case 1234: return true;
		default:
			return false;
		}
	}

	public static int getClipping(int x, int y, int height) {
		try {
			if(height > 3)
				height = 0;
			int regionX = x >> 3;
		int regionY = y >> 3;
		int regionId = ((regionX / 8) << 8) + (regionY / 8);
		for (Region r : regions) {
			if (r.id() == regionId) {
				return r.getClip(x, y, height);
			}
		}
		return 0;
		} catch(Exception e) {
		}
		return 0;
	}

	public static boolean getClipping(int x, int y, int height, int moveTypeX, int moveTypeY)
	{
		try {
			if(height > 3)
				height = 0;
			int checkX = (x + moveTypeX);
			int checkY = (y + moveTypeY);
			if(moveTypeX == -1 && moveTypeY == 0)
				return (getClipping(x, y, height) & 0x1280108) == 0;
			else	
				if(moveTypeX == 1 && moveTypeY == 0)
					return (getClipping(x, y, height) & 0x1280180) == 0;
				else
					if(moveTypeX == 0 && moveTypeY == -1)
						return (getClipping(x, y, height) & 0x1280102) == 0;
					else
						if(moveTypeX == 0 && moveTypeY == 1)
							return (getClipping(x, y, height) & 0x1280120) == 0;
						else
							if(moveTypeX == -1 && moveTypeY == -1)
								return ((getClipping(x, y, height) & 0x128010e) == 0 && (getClipping(checkX - 1, checkY, height) & 0x1280108) == 0 && (getClipping(checkX - 1, checkY, height) & 0x1280102) == 0);
							else
								if(moveTypeX == 1 && moveTypeY == -1)
									return ((getClipping(x, y, height) & 0x1280183) == 0 && (getClipping(checkX + 1, checkY, height) & 0x1280180) == 0 && (getClipping(checkX, checkY - 1, height) & 0x1280102) == 0);
								else
									if(moveTypeX == -1 && moveTypeY == 1)
										return ((getClipping(x, y, height) & 0x1280138) == 0 && (getClipping(checkX - 1, checkY, height) & 0x1280108) == 0 && (getClipping(checkX, checkY + 1, height) & 0x1280120) == 0);
									else
										if(moveTypeX == 1 && moveTypeY == 1)
											return ((getClipping(x, y, height) & 0x12801e0) == 0 && (getClipping(checkX + 1, checkY, height) & 0x1280180) == 0 && (getClipping(checkX, checkY + 1, height) & 0x1280120) == 0);
										else
										{
											System.out.println("[FATAL ERROR]: At getClipping: "+x+", "+y+", "+height+", "+moveTypeX+", "+moveTypeY);
											return false;
										}
		} catch (Exception e) { return true; }
	}


	public static void load() {
		startup = true;
		try {
			File f = new File("./Data/world/map_index");
			byte[] buffer = new byte[(int) f.length()];
			DataInputStream dis = new DataInputStream(new FileInputStream(f));
			dis.readFully(buffer);
			dis.close();
			ByteStream in = new ByteStream(buffer);
			int size = in.length() / 6;
			regions = new Region[size];
			int[] regionIds = new int[size];
			int[] mapGroundFileIds = new int[size];
			int[] mapObjectsFileIds = new int[size];
			boolean[] isMembers = new boolean[size];
			for (int i = 0; i < size; i++) {
				regionIds[i] = in.getUShort();
				mapGroundFileIds[i] = in.getUShort();
				mapObjectsFileIds[i] = in.getUShort();
			}
			for (int i = 0; i < size; i++) {
				regions[i] = new Region(regionIds[i], isMembers[i]);
			}
			for (int i = 0; i < size; i++) {
				byte[] file1 = getBuffer(new File("./Data/world/map/" + mapObjectsFileIds[i] + ".gz"));
				byte[] file2 = getBuffer(new File("./Data/world/map/" + mapGroundFileIds[i] + ".gz"));
				if (file1 == null || file2 == null) {
					continue;
				}
				try {
					loadMaps(regionIds[i], new ByteStream(file1), new ByteStream(file2));
				} catch(Exception e) {
					System.out.println("Error loading map region: " + regionIds[i] + " (" + e.getClass().getSimpleName() + ")");
				}
			}
			System.out.println("[Region] DONE LOADING REGION CONFIGURATIONS");
			verifyClippingConsistency();
		} catch (Exception e) {
			e.printStackTrace();
		}
		startup = false;
	}

	/**
	 * Ownership: Region holds the clip matrix; SmartPathFinder.canStep is the
	 * sole step API used by player/NPC movement, and Movement is the
	 * direction-addressed view over it. There is no second clipping source.
	 * Samples a few known solid tiles and checks Region.getClipping vs
	 * SmartPathFinder.canStep agreement for cardinal steps.
	 */
	public static void verifyClippingConsistency() {
		int mismatches = 0;
		int samples = 0;
		// Lumbridge castle courtyard / bank area samples (z=0).
		int[][] points = new int[][] {
			{ 3222, 3218 }, { 3205, 3209 }, { 3094, 3491 }, { 2949, 3371 },
			{ 3080, 3503 }, { 3253, 3420 }, { 3363, 3275 }, { 2662, 3305 }
		};
		int[][] deltas = new int[][] {
			{ 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }
		};
		for (int p = 0; p < points.length; p++) {
			int x = points[p][0];
			int y = points[p][1];
			if (getRegion(x, y) == null) {
				continue;
			}
			for (int d = 0; d < deltas.length; d++) {
				int dx = deltas[d][0];
				int dy = deltas[d][1];
				boolean regionOk = getClipping(x + dx, y + dy, 0, dx, dy);
				boolean smartOk = SmartPathFinder.canStep(x, y, dx, dy, 0);
				samples++;
				if (regionOk != smartOk) {
					mismatches++;
				}
			}
		}
		if (mismatches == 0) {
			System.out.println("[Region] Clip verify OK (" + samples + " samples, Region vs SmartPathFinder)");
		} else {
			System.out.println("[Region] Clip verify: " + mismatches + "/" + samples
					+ " Region.getClipping(dx,dy) vs SmartPathFinder.canStep mismatches (informational)");
		}
	}

	private static void loadMaps(int regionId, ByteStream str1, ByteStream str2) {
		int absX = (regionId >> 8) * 64;
		int absY = (regionId & 0xff) * 64;
		int[][][] someArray = new int[4][64][64];
		for (int i = 0; i < 4; i++) {
			for (int i2 = 0; i2 < 64; i2++) {
				for (int i3 = 0; i3 < 64; i3++) {
					while (true) {
						int v = str2.getUByte();
						if (v == 0) {
							break;
						} else if (v == 1) {
							str2.skip(1);
							break;
						} else if (v <= 49) {
							str2.skip(1);
						} else if (v <= 81) {
							someArray[i][i2][i3] = v - 49;
						}
					}
				}
			}
		}
		for (int i = 0; i < 4; i++) {
			for (int i2 = 0; i2 < 64; i2++) {
				for (int i3 = 0; i3 < 64; i3++) {
					if ((someArray[i][i2][i3] & 1) == 1) {
						int height = i;
						if ((someArray[1][i2][i3] & 2) == 2) {
							height--;
						}
						if (height >= 0 && height <= 3) {
							addClipping(absX + i2, absY + i3, height, 0x200000);
						}
					}
				}
			}
		}
		int objectId = -1;
		int incr;
		while ((incr = str1.getUSmart()) != 0) {
			objectId += incr;
			int location = 0;
			int incr2;
			while ((incr2 = str1.getUSmart()) != 0) {
				location += incr2 - 1;
				int localX = (location >> 6 & 0x3f);
				int localY = (location & 0x3f);
				int height = location >> 12;
		int objectData = str1.getUByte();
		int type = objectData >> 2;
						int direction = objectData & 0x3;
						if (localX < 0 || localX >= 64 || localY < 0 || localY >= 64) {
							continue;
						}
						if ((someArray[1][localX][localY] & 2) == 2) {
							height--;
						}
						if (height >= 0 && height <= 3) {

							addObject(objectId, absX + localX, absY + localY, height, type, direction);
						}
			}
		}
	}

	public static byte[] getBuffer(File f) throws Exception
	{
		if(!f.exists())
			return null;
		FileInputStream fis = new FileInputStream(f);
		GZIPInputStream gzip = new GZIPInputStream(fis);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] tmp = new byte[4096];
		int n;
		while ((n = gzip.read(tmp)) != -1) {
			bos.write(tmp, 0, n);
		}
		gzip.close();
		fis.close();
		byte[] buffer = bos.toByteArray();
		if(buffer.length < 10)
			return null;
		return buffer;
	}


	public static boolean canMove(int startX, int startY, int endX, int endY, int height, int xLength, int yLength) {
		int diffX = endX - startX;
		int diffY = endY - startY;
		int max = Math.max(Math.abs(diffX), Math.abs(diffY));
		for (int ii = 0; ii < max; ii++) {
			int currentX = endX - diffX;
			int currentY = endY - diffY;
			for (int i = 0; i < xLength; i++) {
				for (int i2 = 0; i2 < yLength; i2++) {
					if (diffX < 0 && diffY < 0) {
						if ((getClipping(currentX + i - 1, currentY + i2 - 1, height) & 0x128010e) != 0 || (getClipping(currentX + i - 1, currentY + i2, height) & 0x1280108) != 0
								|| (getClipping(currentX + i, currentY + i2 - 1, height) & 0x1280102) != 0) {
							return false;
						}
					} else if (diffX > 0 && diffY > 0) {
						if ((getClipping(currentX + i + 1, currentY + i2 + 1, height) & 0x12801e0) != 0 || (getClipping(currentX + i + 1, currentY + i2, height) & 0x1280180) != 0
								|| (getClipping(currentX + i, currentY + i2 + 1, height) & 0x1280120) != 0) {
							return false;
						}
					} else if (diffX < 0 && diffY > 0) {
						if ((getClipping(currentX + i - 1, currentY + i2 + 1, height) & 0x1280138) != 0 || (getClipping(currentX + i - 1, currentY + i2, height) & 0x1280108) != 0
								|| (getClipping(currentX + i, currentY + i2 + 1, height) & 0x1280120) != 0) {
							return false;
						}
					} else if (diffX > 0 && diffY < 0) {
						if ((getClipping(currentX + i + 1, currentY + i2 - 1, height) & 0x1280183) != 0 || (getClipping(currentX + i + 1, currentY + i2, height) & 0x1280180) != 0
								|| (getClipping(currentX + i, currentY + i2 - 1, height) & 0x1280102) != 0) {
							return false;
						}
					} else if (diffX > 0 && diffY == 0) {
						if ((getClipping(currentX + i + 1, currentY + i2, height) & 0x1280180) != 0) {
							return false;
						}
					} else if (diffX < 0 && diffY == 0) {
						if ((getClipping(currentX + i - 1, currentY + i2, height) & 0x1280108) != 0) {
							return false;
						}
					} else if (diffX == 0 && diffY > 0) {
						if ((getClipping(currentX + i, currentY + i2 + 1, height) & 0x1280120) != 0) {
							return false;
						}
					} else if (diffX == 0 && diffY < 0) {
						if ((getClipping(currentX + i, currentY + i2 - 1, height) & 0x1280102) != 0) {
							return false;
						}
					}
				}
			}
			if (diffX < 0) {
				diffX++;
			} else if (diffX > 0) {
				diffX--;
			}
			if (diffY < 0) {
				diffY++;
			} else if (diffY > 0) {
				diffY--;
			}
		}
		return true;
	}
	
	
	private void addClip(int x, int y, int height, int shift) {
		int regionAbsX = (id >> 8) * 64;
		int regionAbsY = (id & 0xff) * 64;
		if (clips[height] == null) {
			clips[height] = new int[64][64];
		}
		clips[height][x - regionAbsX][y - regionAbsY] |= shift;
	}

	private int getClip(int x, int y, int height) {
		if (height > 3 || height < 0) {
			height = Math.abs(height) % 4;
		}
		int regionAbsX = (id >> 8) * 64;
		int regionAbsY = (id & 0xff) * 64;
		if (clips[height] == null) {
			return 0;
		}
		return clips[height][x - regionAbsX][y - regionAbsY];
	}

	private static void addClipping(int x, int y, int height, int shift) {
		int regionX = x >> 3;
		int regionY = y >> 3;
		int regionId = ((regionX / 8) << 8) + (regionY / 8);
		for (Region r : regions) {
			if (r.id() == regionId) {
				r.addClip(x, y, height, shift);
				break;
			}
		}
	}

	public static void tempClip(int x, int y, int height){
		int regionX = x >> 3;
		int regionY = y >> 3;
		int regionId = ((regionX / 8) << 8) + (regionY / 8);
		for (Region r : regions) {
			if (r.id() == regionId) {
				r.addClip(x, y, height, 0x200000);
				break;
			}
		}
	}
	

}