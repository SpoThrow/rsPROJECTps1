package ui;

import java.util.Properties;import game.Player;
import game.client;



public final class PlayerIndicators {

	public static boolean enabled;
	public static boolean tiles;
	public static boolean names = true;
	public static boolean minimapNames;
	static boolean colorMenu = true;
	public static boolean ownPlayer;
	public static boolean friends = true;
	public static boolean team = true;
	public static boolean others;

	static final int COL_OWN = 0x00FFFF;
	static final int COL_FRIEND = 0x00FF80;
	static final int COL_TEAM = 0xAA66FF;
	static final int COL_OTHER = 0xFFFFFF;

	private static final int MAP = 104;
	private static final int[] occupied = new int[MAP * MAP];
	private static final int[] colors = new int[MAP * MAP];
	private static int stamp = 1;

	public static void load(Properties props) {
		enabled = readBool(props, "playerInd", false);
		tiles = readBool(props, "playerIndTiles", false);
		names = readBool(props, "playerIndNames", true);
		minimapNames = readBool(props, "playerIndMini", false);
		colorMenu = readBool(props, "playerIndMenu", true);
		ownPlayer = readBool(props, "playerIndOwn", false);
		friends = readBool(props, "playerIndFriends", true);
		team = readBool(props, "playerIndTeam", true);
		others = readBool(props, "playerIndOthers", false);
	}

	public static void save(Properties props) {
		props.setProperty("playerInd", Boolean.toString(enabled));
		props.setProperty("playerIndTiles", Boolean.toString(tiles));
		props.setProperty("playerIndNames", Boolean.toString(names));
		props.setProperty("playerIndMini", Boolean.toString(minimapNames));
		props.setProperty("playerIndMenu", Boolean.toString(colorMenu));
		props.setProperty("playerIndOwn", Boolean.toString(ownPlayer));
		props.setProperty("playerIndFriends", Boolean.toString(friends));
		props.setProperty("playerIndTeam", Boolean.toString(team));
		props.setProperty("playerIndOthers", Boolean.toString(others));
	}

	public static void beginFrame() {
		stamp++;
		if (stamp == Integer.MAX_VALUE) {
			stamp = 1;
			for (int i = 0; i < occupied.length; i++) {
				occupied[i] = 0;
			}
		}
	}

	public static int colorFor(client c, Player p) {
		if (!enabled || c == null || p == null || p.name == null) {
			return 0;
		}
		if (p == c.myPlayer) {
			return ownPlayer ? COL_OWN : 0;
		}
		if (friends && c.isFriendName(p.name)) {
			return COL_FRIEND;
		}
		if (team && c.myPlayer != null && c.myPlayer.team != 0 && p.team == c.myPlayer.team) {
			return COL_TEAM;
		}
		if (others) {
			return COL_OTHER;
		}
		return 0;
	}

	public static String menuPrefix(client c, Player p) {
		if (!enabled || !colorMenu) {
			return "@whi@";
		}
		int col = colorFor(c, p);
		if (col == COL_FRIEND) {
			return "@gre@";
		}
		if (col == COL_TEAM) {
			return "@yel@";
		}
		if (col == COL_OWN) {
			return "@cya@";
		}
		return "@whi@";
	}

	public static void mark(client c, Player p) {
		if (!tiles) {
			return;
		}
		int col = colorFor(c, p);
		if (col == 0) {
			return;
		}
		int tx = p.x >> 7;
		int ty = p.y >> 7;
		if (tx < 0 || ty < 0 || tx >= MAP || ty >= MAP) {
			return;
		}
		int i = tx + ty * MAP;
		occupied[i] = stamp;
		colors[i] = col;
	}

	public static int tileColor(int x, int y) {
		if (!enabled || !tiles || x < 0 || y < 0 || x >= MAP || y >= MAP) {
			return 0;
		}
		int i = x + y * MAP;
		return occupied[i] == stamp ? colors[i] : 0;
	}

	public static void drawName(client c, Player p, TextDrawingArea font) {
		if (!enabled || !names || font == null || p == null || p.name == null) {
			return;
		}
		int col = colorFor(c, p);
		if (col == 0) {
			return;
		}
		c.npcScreenPos(p, p.height + 15);
		int x = c.getSpriteDrawX();
		int y = c.getSpriteDrawY();
		if (x < 0 || y < 0) {
			return;
		}
		int w = font.getTextWidth(p.name);
		int[] pos = OverlayManager.placeWorld(x - w / 2, y - 10, w, 12);
		font.drawText(col, p.name, pos[1] + 10, pos[0] + w / 2);
	}

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}
}
