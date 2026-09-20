import java.util.Properties;

/**
 * RuneLite Cannon plugin: ball count overlay, infobox, low-ball warning,
 * double-hit tiles, and common cannon spots.
 */
final class CannonOverlay {

	static boolean enabled = true;
	static boolean infobox = true;
	static boolean doubleHit;
	static boolean spots = true;
	static int warningThreshold = 15;

	private static boolean active;
	private static int x;
	private static int y;
	private static int z;
	private static int balls;
	private static boolean warned;
	private static String pendingWarning;

	private static final int SPOT_COLOR = 0xFF981F;
	private static final int DOUBLE_COLOR = 0xFF4040;

	private static final int[][] CANNON_SPOTS = {
			{ 2673, 3714, 0 }, { 2678, 3715, 0 }, { 2705, 3718, 0 }, { 2694, 3716, 0 },
			{ 2709, 3730, 0 }, { 2670, 3708, 0 },
			{ 3159, 9903, 0 }, { 3307, 9528, 0 },
			{ 2393, 9782, 0 }, { 2412, 9776, 0 }, { 2401, 9780, 0 },
			{ 2431, 9776, 0 }, { 2413, 9786, 0 }, { 2783, 9686, 0 },
			{ 2524, 10020, 0 }, { 2859, 9778, 0 }, { 2841, 9791, 0 },
			{ 2439, 9821, 0 }, { 2448, 9821, 0 }, { 2456, 9791, 0 },
			{ 3120, 9987, 0 }, { 3218, 9366, 0 }, { 3089, 9960, 0 }
	};

	static void load(Properties props) {
		enabled = readBool(props, "cannonPlugin", true);
		infobox = readBool(props, "cannonInfobox", true);
		doubleHit = readBool(props, "cannonDoubleHit", false);
		spots = readBool(props, "cannonSpots", true);
		warningThreshold = clamp(readInt(props, "cannonWarn", 15), 0, 30);
	}

	static void save(Properties props) {
		props.setProperty("cannonPlugin", Boolean.toString(enabled));
		props.setProperty("cannonInfobox", Boolean.toString(infobox));
		props.setProperty("cannonDoubleHit", Boolean.toString(doubleHit));
		props.setProperty("cannonSpots", Boolean.toString(spots));
		props.setProperty("cannonWarn", Integer.toString(warningThreshold));
	}

	static void cycleWarning() {
		if (warningThreshold <= 0) {
			warningThreshold = 5;
		} else if (warningThreshold < 10) {
			warningThreshold = 10;
		} else if (warningThreshold < 15) {
			warningThreshold = 15;
		} else if (warningThreshold < 25) {
			warningThreshold = 25;
		} else {
			warningThreshold = 0;
		}
	}

	static String warningLabel() {
		return warningThreshold <= 0 ? "Off" : Integer.toString(warningThreshold);
	}

	static boolean apply(String text) {
		if (text == null || !text.startsWith("cannon:")) {
			return false;
		}
		if (text.equals("cannon:off")) {
			active = false;
			warned = false;
			return true;
		}
		String[] parts = text.substring(7).split(":");
		if (parts.length < 4) {
			return false;
		}
		try {
			x = Integer.parseInt(parts[0]);
			y = Integer.parseInt(parts[1]);
			z = Integer.parseInt(parts[2]);
			int next = Integer.parseInt(parts[3]);
			if (next > balls) {
				warned = false;
			}
			balls = next;
			active = true;
			if (enabled && warningThreshold > 0 && balls > 0 && balls <= warningThreshold && !warned) {
				warned = true;
				pendingWarning = "Your cannon has " + balls + " cannonball" + (balls == 1 ? "" : "s") + " remaining!";
			}
			if (enabled && balls == 0) {
				pendingWarning = "Your cannon is out of ammo!";
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	static void draw(client c, TextDrawingArea font, int plane) {
		if (pendingWarning != null && c != null) {
			c.pushMessage(pendingWarning, 0, "");
			pendingWarning = null;
		}
		if (!active || font == null || c == null || plane != z) {
			return;
		}
		int sx = ((x + 1 - client.getBaseX()) << 7) + 64;
		int sy = ((y + 1 - client.getBaseY()) << 7) + 64;
		c.calcEntityScreenPos(sx, 200, sy);
		int drawX = c.getSpriteDrawX();
		int drawY = c.getSpriteDrawY();
		if (drawX >= 0 && drawY >= 0) {
			String label = Integer.toString(balls);
			font.drawText(0, label, drawY + 1, drawX);
			font.drawText(stateColor(), label, drawY, drawX);
		}
	}

	static void drawInfo(TextDrawingArea font) {
		if (!enabled || !infobox || !active || font == null) {
			return;
		}
		InfoBoxes.draw("cannon", font, "Cannon: " + balls, stateColor());
	}

	static int tileColor(int localX, int localY, int plane) {
		if (!enabled) {
			return 0;
		}
		int wx = client.getBaseX() + localX;
		int wy = client.getBaseY() + localY;
		if (doubleHit && active && plane == z && isDoubleHit(wx, wy)) {
			return DOUBLE_COLOR;
		}
		if (spots && isSpot(wx, wy, plane)) {
			return SPOT_COLOR;
		}
		return 0;
	}

	private static boolean isSpot(int wx, int wy, int plane) {
		for (int i = 0; i < CANNON_SPOTS.length; i++) {
			if (CANNON_SPOTS[i][0] == wx && CANNON_SPOTS[i][1] == wy && CANNON_SPOTS[i][2] == plane) {
				return true;
			}
		}
		return false;
	}

	private static boolean isDoubleHit(int wx, int wy) {
		int cx = x + 1;
		int cy = y + 1;
		int dx = wx - cx;
		int dy = wy - cy;
		if (dx < -3 || dx > 3 || dy < -3 || dy > 3) {
			return false;
		}
		if (dy != 1 && dx != 1 && dy != -1 && dx != -1) {
			return false;
		}
		if (dy >= -1 && dy <= 1 && dx >= -1 && dx <= 1) {
			return false;
		}
		return true;
	}

	private static int stateColor() {
		if (balls > 15) {
			return 0x00FF00;
		}
		if (balls > 5) {
			return 0xFF981F;
		}
		return 0xFF3030;
	}

	private static int readInt(Properties props, String key, int def) {
		try {
			return Integer.parseInt(props.getProperty(key, Integer.toString(def)));
		} catch (Exception e) {
			return def;
		}
	}

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}

	private static int clamp(int v, int min, int max) {
		if (v < min) {
			return min;
		}
		if (v > max) {
			return max;
		}
		return v;
	}
}
