import java.util.ArrayList;
import java.util.Properties;

final class BossTimers {

	static boolean enabled = true;

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

	private static final ArrayList active = new ArrayList();
	private static final int[] lastHp = new int[32768];

	static void load(Properties props) {
		enabled = readBool(props, "bossTimers", true);
	}

	static void save(Properties props) {
		props.setProperty("bossTimers", Boolean.toString(enabled));
	}

	static void observe(NPC npc, int index) {
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
		active.add(new long[] { System.currentTimeMillis() + SECONDS[boss] * 1000L, boss });
	}

	static void draw(TextDrawingArea font) {
		if (!enabled || font == null) {
			return;
		}
		InfoBoxes.start("bosses", font);
		long now = System.currentTimeMillis();
		for (int i = active.size() - 1; i >= 0; i--) {
			long[] t = (long[]) active.get(i);
			int left = (int) ((t[0] - now) / 1000L);
			if (left <= 0) {
				active.remove(i);
				continue;
			}
			int boss = (int) t[1];
			InfoBoxes.line(NAMES[boss] + " " + format(left), 0xFF6666);
		}
		InfoBoxes.flush();
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
