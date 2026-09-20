import java.util.Properties;

/**
 * Barrows overlay: minimap initials, dead colour, prayer-drain timer, chest value.
 */
final class BarrowsPlugin {

	static boolean enabled = true;

	private static int killedMask;
	private static int killCount;
	private static int chestValue = -1;
	private static long drainStart;

	private static final String[] LETTERS = { "A", "K", "D", "V", "T", "G" };
	private static final int[][] HILLS = {
			{ 3565, 3288 }, { 3565, 3275 }, { 3575, 3298 },
			{ 3557, 3298 }, { 3553, 3283 }, { 3577, 3283 }
	};

	static void load(Properties props) {
		enabled = readBool(props, "barrowsPlugin", true);
	}

	static void save(Properties props) {
		props.setProperty("barrowsPlugin", Boolean.toString(enabled));
	}

	static boolean apply(String text) {
		if (text == null) {
			return false;
		}
		if (text.startsWith("barrows:")) {
			String[] parts = text.substring(8).split(":");
			if (parts.length < 2) {
				return false;
			}
			try {
				killedMask = Integer.parseInt(parts[0]);
				killCount = Integer.parseInt(parts[1]);
			} catch (Exception e) {
				return false;
			}
			return true;
		}
		if (text.startsWith("barrowschest:")) {
			try {
				chestValue = Integer.parseInt(text.substring(13).trim());
			} catch (Exception e) {
				return false;
			}
			return true;
		}
		return false;
	}

	static boolean inArea(int worldX, int worldY) {
		if (worldX >= 3520 && worldX <= 3585 && worldY >= 3265 && worldY <= 3312) {
			return true;
		}
		if (worldX >= 3523 && worldX <= 3589 && worldY >= 9666 && worldY <= 9735) {
			return true;
		}
		return false;
	}

	static void drawInfo(TextDrawingArea font, int worldX, int worldY) {
		if (!enabled || font == null || !inArea(worldX, worldY)) {
			return;
		}
		if (drainStart == 0L) {
			drainStart = System.currentTimeMillis();
		}
		int remain = 18 - (int) ((System.currentTimeMillis() - drainStart) / 1000L % 18L);
		InfoBoxes.start("barrows", font);
		InfoBoxes.line("Barrows KC: " + killCount, 0xFF981F);
		InfoBoxes.line("Prayer drain: " + remain + "s", 0x00A0FF);
		if (chestValue >= 0) {
			InfoBoxes.line("Chest: " + formatValue(chestValue), 0xFFFF00);
		}
		InfoBoxes.flush();
	}

	static void drawMinimap(client c, int worldX, int worldY, int plane) {
		if (!enabled || c == null || !inArea(worldX, worldY) || client.myPlayer == null) {
			return;
		}
		for (int i = 0; i < HILLS.length; i++) {
			int dx = ((HILLS[i][0] - client.getBaseX()) * 4 + 2) - client.myPlayer.x / 32;
			int dy = ((HILLS[i][1] - client.getBaseY()) * 4 + 2) - client.myPlayer.y / 32;
			int color = brotherKilled(i) ? 0x808080 : 0x00FF00;
			c.drawBarrowsMinimapLetter(LETTERS[i], dx, dy, color);
		}
	}

	static int brotherColor(int npcType) {
		if (!enabled) {
			return 0;
		}
		int idx = brotherIndex(npcType);
		if (idx < 0) {
			return 0;
		}
		return brotherKilled(idx) ? 0x808080 : 0x00FF00;
	}

	static boolean isBrother(int npcType) {
		return brotherIndex(npcType) >= 0;
	}

	private static int brotherIndex(int npcType) {
		if (npcType == 2025) {
			return 0;
		}
		if (npcType == 2028) {
			return 1;
		}
		if (npcType == 2026) {
			return 2;
		}
		if (npcType == 2030) {
			return 3;
		}
		if (npcType == 2029) {
			return 4;
		}
		if (npcType == 2027) {
			return 5;
		}
		return -1;
	}

	private static boolean brotherKilled(int idx) {
		return (killedMask & (1 << idx)) != 0;
	}

	private static String formatValue(int v) {
		if (v >= 1000000) {
			return (v / 1000000) + "m";
		}
		if (v >= 1000) {
			return (v / 1000) + "k";
		}
		return Integer.toString(v);
	}

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}
}
