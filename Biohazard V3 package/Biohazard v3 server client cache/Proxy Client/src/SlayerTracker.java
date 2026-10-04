import java.util.Properties;

final class SlayerTracker {

	static boolean enabled = true;
	static boolean highlight = true;
	static boolean countOnItems = true;

	static String task = "";
	static int remaining;
	static String location = "";

	private static final int[] lastHp = new int[32768];

	static void load(Properties props) {
		enabled = readBool(props, "slayer", true);
		highlight = readBool(props, "slayerHighlight", true);
		countOnItems = readBool(props, "slayerCountItems", true);
		task = props.getProperty("slayerTask", "");
		if (task == null) {
			task = "";
		}
		remaining = readInt(props, "slayerAmt", 0);
		location = props.getProperty("slayerLoc", "");
		if (location == null) {
			location = "";
		}
	}

	static void save(Properties props) {
		props.setProperty("slayer", Boolean.toString(enabled));
		props.setProperty("slayerHighlight", Boolean.toString(highlight));
		props.setProperty("slayerCountItems", Boolean.toString(countOnItems));
		props.setProperty("slayerTask", task == null ? "" : task);
		props.setProperty("slayerAmt", Integer.toString(remaining));
		props.setProperty("slayerLoc", location == null ? "" : location);
	}

	static void onMessage(String text) {
		if (text == null) {
			return;
		}
		String s = strip(text);
		int kill = indexOfIgnore(s, "your new task is to kill ");
		if (kill >= 0) {
			parseAmountName(s.substring(kill + "your new task is to kill ".length()));
			return;
		}
		int have = indexOfIgnore(s, "i currently have ");
		if (have < 0) {
			have = indexOfIgnore(s, "you currently have ");
		}
		if (have >= 0) {
			String rest = s.substring(have + (s.toLowerCase().indexOf("i currently have ") == have
					? "i currently have ".length() : "you currently have ".length()));
			int toKill = indexOfIgnore(rest, " to kill");
			if (toKill > 0) {
				rest = rest.substring(0, toKill);
			}
			parseAmountName(rest);
			return;
		}
		if (indexOfIgnore(s, "you completed your slayer task") >= 0
				|| indexOfIgnore(s, "cancelled your current task") >= 0) {
			task = "";
			remaining = 0;
			location = "";
			return;
		}
		if (indexOfIgnore(s, "task can be found in the ") >= 0) {
			int i = indexOfIgnore(s, "task can be found in the ");
			location = s.substring(i + "task can be found in the ".length()).trim();
			if (location.endsWith(".")) {
				location = location.substring(0, location.length() - 1);
			}
		}
	}

	static boolean isTaskNpc(NPC npc) {
		if (!enabled || remaining <= 0 || task.length() == 0 || npc == null || npc.desc == null
				|| npc.desc.name == null) {
			return false;
		}
		return nameMatches(npc.desc.name, task);
	}

	static boolean matches(NPC npc) {
		return highlight && isTaskNpc(npc);
	}

	static boolean observe(NPC npc, int index) {
		if (npc == null || index < 0 || index >= lastHp.length) {
			return false;
		}
		int hp = npc.currentHealth;
		int prev = lastHp[index];
		lastHp[index] = hp;
		if (prev <= 0 || hp > 0 || !isTaskNpc(npc)) {
			return false;
		}
		remaining--;
		if (remaining < 0) {
			remaining = 0;
		}
		return true;
	}

	static boolean isSlayerItem(int itemId) {
		if (!countOnItems || itemId < 0) {
			return false;
		}
		if (remaining <= 0 && (task == null || task.length() == 0)) {
			return false;
		}
		if (itemId == 4155 || itemId == 15051) {
			return true;
		}
		if (itemId >= 8901 && itemId <= 8921) {
			return true;
		}
		ItemDef def = ItemDef.forID(itemId);
		if (def == null || def.name == null) {
			return false;
		}
		String n = def.name.toLowerCase();
		return n.indexOf("slayer helm") >= 0 || n.indexOf("slayer helmet") >= 0 || n.indexOf("black mask") >= 0
				|| n.equals("enchanted gem");
	}

	static void draw(TextDrawingArea font) {
		if (!enabled || font == null) {
			return;
		}
		if (remaining <= 0 && (task == null || task.length() == 0)) {
			return;
		}
		InfoBoxes.icon("slayer", 4155, Integer.toString(remaining), 0x33CC66);
	}

	private static void parseAmountName(String rest) {
		rest = rest.trim();
		if (rest.endsWith(".")) {
			rest = rest.substring(0, rest.length() - 1).trim();
		}
		int space = rest.indexOf(' ');
		if (space <= 0) {
			return;
		}
		try {
			remaining = Integer.parseInt(rest.substring(0, space).trim());
		} catch (Exception e) {
			return;
		}
		task = rest.substring(space + 1).trim();
		if (task.endsWith(".")) {
			task = task.substring(0, task.length() - 1).trim();
		}
	}

	private static boolean nameMatches(String npcName, String taskName) {
		String a = normalize(npcName);
		String b = normalize(taskName);
		if (a.length() == 0 || b.length() == 0) {
			return false;
		}
		return a.indexOf(b) >= 0 || b.indexOf(a) >= 0;
	}

	private static String normalize(String s) {
		s = s.toLowerCase().trim();
		if (s.endsWith("s") && s.length() > 3) {
			s = s.substring(0, s.length() - 1);
		}
		return s.replace('_', ' ');
	}

	private static String strip(String s) {
		StringBuffer sb = new StringBuffer(s.length());
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) != '@') {
				sb.append(s.charAt(i));
				continue;
			}
			i += 4;
		}
		return sb.toString();
	}

	private static int indexOfIgnore(String hay, String needle) {
		return hay.toLowerCase().indexOf(needle);
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
}
