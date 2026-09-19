import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

final class MenuEntrySwapper {

	static final int ACTION_SWAP = 1600;
	static final int ACTION_RESET = 1601;
	static final int ACTION_MARK = 1602;
	static final int ACTION_UNMARK = 1603;

	private static final String[] NPC_LEFT_CLICK = {
		"Bank", "Collect", "Exchange", "Trade", "Shop", "Teleport",
		"Travel", "Take-boat", "Pay-fare", "Charter", "Claim", "Rewards"
	};

	private static final HashMap swaps = new HashMap();

	static void load(Properties props) {
		swaps.clear();
		Iterator it = props.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			String key = String.valueOf(entry.getKey());
			if (key.startsWith("mes.")) {
				swaps.put(key.substring(4), String.valueOf(entry.getValue()));
			}
		}
	}

	static void save(Properties props) {
		Iterator it = swaps.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			props.setProperty("mes." + entry.getKey(), String.valueOf(entry.getValue()));
		}
	}

	static int apply(String[] names, int[] ids, int[] cmd1, int[] cmd2, int[] cmd3, int row, boolean shiftDown) {
		if (row <= 1) {
			return row;
		}
		if (!shiftDown) {
			swapLeftClick(names, ids, cmd1, cmd2, cmd3, row);
		}
		if (shiftDown) {
			row = addShiftEntries(names, ids, cmd1, cmd2, cmd3, row);
		}
		return row;
	}

	static boolean captureFromMenu(String[] names, int[] ids, int index, int row) {
		if (index < 0 || index >= row) {
			return false;
		}
		int id = ids[index];
		if (id == 1107 || id == ACTION_SWAP || id == ACTION_RESET || id == ACTION_MARK || id == ACTION_UNMARK) {
			return false;
		}
		if (isExamine(id)) {
			return false;
		}
		String key = targetKey(names[index], id);
		String verb = actionVerb(names[index]);
		if (key.length() == 0 || verb.length() == 0 || verb.equalsIgnoreCase("Walk here") || verb.equalsIgnoreCase("Cancel")) {
			return false;
		}
		swaps.put(key, verb);
		return true;
	}

	static boolean handleAction(client c, int action, int index) {
		if (action == ACTION_RESET) {
			String key = targetKey(c.menuActionName[index], c.menuActionID[index]);
			if (key.length() == 0 && c.menuActionRow > 1) {
				key = targetKey(c.menuActionName[c.menuActionRow - 1], c.menuActionID[c.menuActionRow - 1]);
			}
			if (key.length() > 0) {
				swaps.remove(key);
				c.pushMessage("Left-click reset for that target.", 0, "");
				c.saveClientSettings();
			}
			return true;
		}
		if (action == ACTION_SWAP) {
			String verb = actionVerb(c.menuActionName[index]);
			if (verb.startsWith("Swap left-click: ")) {
				verb = verb.substring("Swap left-click: ".length()).trim();
			}
			String key = "";
			for (int i = 0; i < c.menuActionRow; i++) {
				if (c.menuActionID[i] == ACTION_SWAP || c.menuActionID[i] == ACTION_RESET) {
					continue;
				}
				if (actionVerb(c.menuActionName[i]).equalsIgnoreCase(verb)) {
					key = targetKey(c.menuActionName[i], c.menuActionID[i]);
					if (key.length() > 0) {
						break;
					}
				}
			}
			if (key.length() > 0 && verb.length() > 0) {
				swaps.put(key, verb);
				c.pushMessage("Left-click is now " + verb + ".", 0, "");
				c.saveClientSettings();
			}
			return true;
		}
		if (action == ACTION_MARK) {
			GroundMarkers.setAt(c, c.menuActionCmd1[index], c.menuActionCmd2[index], c.menuActionCmd3[index], true);
			return true;
		}
		if (action == ACTION_UNMARK) {
			GroundMarkers.setAt(c, c.menuActionCmd1[index], c.menuActionCmd2[index], c.menuActionCmd3[index], false);
			return true;
		}
		return false;
	}

	private static void swapLeftClick(String[] names, int[] ids, int[] cmd1, int[] cmd2, int[] cmd3, int row) {
		int current = row - 1;
		String key = targetKey(names[current], ids[current]);
		String wanted = null;
		if (key.length() > 0 && swaps.containsKey(key)) {
			wanted = (String) swaps.get(key);
		} else {
			wanted = builtInSwap(names, ids, row);
		}
		if (wanted == null) {
			return;
		}
		int found = -1;
		for (int i = 0; i < row; i++) {
			if (isExamine(ids[i]) || ids[i] == 1107) {
				continue;
			}
			if (actionVerb(names[i]).equalsIgnoreCase(wanted)) {
				found = i;
				break;
			}
		}
		if (found >= 0 && found != current) {
			swap(names, ids, cmd1, cmd2, cmd3, found, current);
		}
	}

	private static String builtInSwap(String[] names, int[] ids, int row) {
		int current = row - 1;
		String verb = actionVerb(names[current]);
		if (verb.equalsIgnoreCase("Talk-to") || verb.equalsIgnoreCase("Talk to") || verb.equalsIgnoreCase("Talk")) {
			for (int p = 0; p < NPC_LEFT_CLICK.length; p++) {
				for (int i = 0; i < row; i++) {
					if (actionVerb(names[i]).equalsIgnoreCase(NPC_LEFT_CLICK[p])) {
						return NPC_LEFT_CLICK[p];
					}
				}
			}
		}
		if (verb.equalsIgnoreCase("Bury")) {
			for (int i = 0; i < row; i++) {
				if (actionVerb(names[i]).equalsIgnoreCase("Use") && isInventoryAction(ids[i])) {
					return "Use";
				}
			}
		}
		if ((verb.equalsIgnoreCase("Use") || verb.equalsIgnoreCase("Open")) && names[current].indexOf("@cya@") != -1) {
			for (int i = 0; i < row; i++) {
				if (actionVerb(names[i]).equalsIgnoreCase("Bank")) {
					return "Bank";
				}
			}
		}
		return null;
	}

	private static int addShiftEntries(String[] names, int[] ids, int[] cmd1, int[] cmd2, int[] cmd3, int row) {
		boolean hasWalk = false;
		String resetKey = "";
		for (int i = 0; i < row; i++) {
			if (ids[i] == 516) {
				hasWalk = true;
			}
			String key = targetKey(names[i], ids[i]);
			if (key.length() > 0) {
				resetKey = key;
			}
		}
		if (GroundMarkers.enabled && hasWalk && WorldController.hoverTileX >= 0) {
			boolean marked = GroundMarkers.hoveredIsMarked();
			int wx = client.getBaseX() + WorldController.hoverTileX;
			int wy = client.getBaseY() + WorldController.hoverTileY;
			row = prepend(names, ids, cmd1, cmd2, cmd3, row, marked ? "Unmark tile" : "Mark tile",
					marked ? ACTION_UNMARK : ACTION_MARK, wx, wy, client.scenePlane);
		}
		if (resetKey.length() > 0 && swaps.containsKey(resetKey)) {
			row = prepend(names, ids, cmd1, cmd2, cmd3, row, "Reset left-click", ACTION_RESET, 0, 0, 0);
		}
		String[] extra = new String[row];
		int[] extraA = new int[row];
		int[] extraB = new int[row];
		int[] extraC = new int[row];
		int extraCount = 0;
		for (int i = 0; i < row && extraCount < extra.length; i++) {
			if (isExamine(ids[i]) || ids[i] == 1107 || ids[i] == 516) {
				continue;
			}
			if (ids[i] >= ACTION_SWAP && ids[i] <= ACTION_UNMARK) {
				continue;
			}
			String verb = actionVerb(names[i]);
			String key = targetKey(names[i], ids[i]);
			if (verb.length() == 0 || key.length() == 0) {
				continue;
			}
			extra[extraCount] = "Swap left-click: " + verb;
			extraA[extraCount] = cmd1[i];
			extraB[extraCount] = cmd2[i];
			extraC[extraCount] = cmd3[i];
			extraCount++;
		}
		for (int i = 0; i < extraCount && row < names.length - 2; i++) {
			row = prepend(names, ids, cmd1, cmd2, cmd3, row, extra[i], ACTION_SWAP, extraA[i], extraB[i], extraC[i]);
		}
		return row;
	}

	private static int prepend(String[] names, int[] ids, int[] cmd1, int[] cmd2, int[] cmd3, int row, String name, int id, int a, int b, int c) {
		if (row >= names.length) {
			return row;
		}
		for (int i = row; i > 0; i--) {
			names[i] = names[i - 1];
			ids[i] = ids[i - 1];
			cmd1[i] = cmd1[i - 1];
			cmd2[i] = cmd2[i - 1];
			cmd3[i] = cmd3[i - 1];
		}
		names[0] = name;
		ids[0] = id;
		cmd1[0] = a;
		cmd2[0] = b;
		cmd3[0] = c;
		return row + 1;
	}

	private static int insert(String[] names, int[] ids, int[] cmd1, int[] cmd2, int[] cmd3, int row, String name, int id, int a, int b, int c) {
		if (row >= names.length) {
			return row;
		}
		names[row] = name;
		ids[row] = id;
		cmd1[row] = a;
		cmd2[row] = b;
		cmd3[row] = c;
		return row + 1;
	}

	private static void swap(String[] names, int[] ids, int[] cmd1, int[] cmd2, int[] cmd3, int a, int b) {
		String ns = names[a];
		names[a] = names[b];
		names[b] = ns;
		int t = ids[a];
		ids[a] = ids[b];
		ids[b] = t;
		t = cmd1[a];
		cmd1[a] = cmd1[b];
		cmd1[b] = t;
		t = cmd2[a];
		cmd2[a] = cmd2[b];
		cmd2[b] = t;
		t = cmd3[a];
		cmd3[a] = cmd3[b];
		cmd3[b] = t;
	}

	static String actionVerb(String name) {
		if (name == null) {
			return "";
		}
		int at = name.indexOf('@');
		String verb = at == -1 ? name : name.substring(0, at);
		int tab = verb.indexOf('\t');
		if (tab != -1) {
			verb = verb.substring(0, tab);
		}
		return verb.trim();
	}

	static String targetKey(String name, int id) {
		if (name == null) {
			return "";
		}
		String target = coloredTarget(name);
		if (target.length() == 0) {
			return "";
		}
		if (name.indexOf("@yel@") != -1) {
			return "n:" + target;
		}
		if (name.indexOf("@cya@") != -1) {
			return "o:" + target;
		}
		if (name.indexOf("@lre@") != -1) {
			return (isInventoryAction(id) ? "i:" : "g:") + target;
		}
		if (name.indexOf("@whi@") != -1) {
			return "p:" + target;
		}
		return "";
	}

	private static String coloredTarget(String name) {
		int idx = name.lastIndexOf('@');
		if (idx < 4) {
			return "";
		}
		String target = name.substring(idx + 1).trim();
		int level = target.indexOf(" (level-");
		if (level != -1) {
			target = target.substring(0, level).trim();
		}
		int tab = target.indexOf('\t');
		if (tab != -1) {
			target = target.substring(0, tab).trim();
		}
		return target;
	}

	private static boolean isExamine(int id) {
		int raw = id >= 2000 ? id - 2000 : id;
		return raw == 1025 || raw == 1226 || raw == 1125;
	}

	private static boolean isInventoryAction(int id) {
		int raw = id >= 2000 ? id - 2000 : id;
		return raw == 74 || raw == 454 || raw == 539 || raw == 493 || raw == 847 || raw == 447
				|| raw == 632 || raw == 78 || raw == 867 || raw == 431 || raw == 53 || raw == 1125;
	}
}
