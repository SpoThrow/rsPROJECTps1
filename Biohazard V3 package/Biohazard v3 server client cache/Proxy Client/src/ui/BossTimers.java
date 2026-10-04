package ui;

import java.util.ArrayList;
import java.util.Properties;import game.NPC;
import game.client;



public final class BossTimers {

	public static boolean enabled = true;

	private static final int[] IDS = {
			50, 1158, 1160, 3200, 2881, 2882, 2883, 6222, 6260, 6247, 6203, 3847
	};
	private static final String[] NAMES = {
			"KBD", "KQ", "KQ", "Chaos Ele", "Supreme", "Prime", "Rex", "Kree'arra",
			"Graardor", "Zilyana", "K'ril", "Sea Troll Queen"
	};
	private static final int[] SECONDS = {
			90, 90, 90, 60, 90, 90, 90, 90, 90, 90, 90, 60
	};
	private static final int[] ICONS = {
			1747, 3142, 3142, 592, 6729, 6731, 6733, 11718, 11724, 11720, 11716, 395
	};

	private static final ArrayList active = new ArrayList();
	private static final int[] lastHp = new int[32768];

	public static void load(Properties props) {
		enabled = readBool(props, "bossTimers", true);
	}

	public static void save(Properties props) {
		props.setProperty("bossTimers", Boolean.toString(enabled));
	}

	public static void observe(NPC npc, int index) {
		if (!enabled || npc == null || npc.desc == null) {
			return;
		}
		if (index < 0 || index >= lastHp.length) {
			return;
		}
		int hp = npc.currentHealth;
		int prev = lastHp[index];
		lastHp[index] = hp;
		if (prev <= 0 || hp > 0) {
			return;
		}
		int type = (int) npc.desc.interfaceType;
		int boss = indexOf(type);
		if (boss < 0) {
			return;
		}
		int wx = client.getBaseX() + npc.smallX[0];
		int wy = client.getBaseY() + npc.smallY[0];
		active.add(new long[] { System.currentTimeMillis() + SECONDS[boss] * 1000L, boss, wx, wy, client.scenePlane });
	}

	public static int tileColor(int localX, int localY, int plane) {
		if (!enabled) {
			return 0;
		}
		int wx = client.getBaseX() + localX;
		int wy = client.getBaseY() + localY;
		long now = System.currentTimeMillis();
		for (int i = 0; i < active.size(); i++) {
			long[] t = (long[]) active.get(i);
			if (t[0] <= now) {
				continue;
			}
			if ((int) t[2] == wx && (int) t[3] == wy && (int) t[4] == plane) {
				return 0xFF4040;
			}
		}
		return 0;
	}

	public static void draw(client c, TextDrawingArea font) {
		if (!enabled || font == null) {
			return;
		}
		long now = System.currentTimeMillis();
		for (int i = active.size() - 1; i >= 0; i--) {
			long[] t = (long[]) active.get(i);
			int left = (int) ((t[0] - now) / 1000L);
			if (left <= 0) {
				active.remove(i);
				continue;
			}
			int boss = (int) t[1];
			InfoBoxes.icon("boss" + i, iconOf(boss), format(left), 0xFF6666);
			if (c != null && (int) t[4] == client.scenePlane) {
				int sx = (((int) t[2] - client.getBaseX()) << 7) + 64;
				int sy = (((int) t[3] - client.getBaseY()) << 7) + 64;
				c.calcEntityScreenPos(sx, 0, sy);
				int dx = c.getSpriteDrawX();
				int dy = c.getSpriteDrawY();
				if (dx >= 0 && dy >= 0) {
					String label = Integer.toString(left);
					font.drawText(0, label, dy + 1, dx);
					font.drawText(0xFFFFFF, label, dy, dx);
				}
			}
		}
	}

	private static int iconOf(int boss) {
		if (boss < 0 || boss >= ICONS.length) {
			return 4155;
		}
		return ICONS[boss];
	}

	private static int indexOf(int id) {
		for (int i = 0; i < IDS.length; i++) {
			if (IDS[i] == id) {
				return i;
			}
		}
		return -1;
	}

	private static String format(int sec) {
		int m = sec / 60;
		int s = sec % 60;
		if (m <= 0) {
			return s + "s";
		}
		if (s < 10) {
			return m + ":0" + s;
		}
		return m + ":" + s;
	}

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}
}
