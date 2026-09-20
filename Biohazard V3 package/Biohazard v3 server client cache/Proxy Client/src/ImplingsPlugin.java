import java.util.HashSet;
import java.util.Properties;

/** Highlight, name, minimap, and notify for hunter implings. */
final class ImplingsPlugin {

	static boolean enabled;
	static boolean names = true;
	static boolean minimapNames = true;
	static boolean notify = true;

	private static final int FIRST = 1028;
	private static final int LAST = 1037;
	private static final String[] TYPE_NAMES = {
			"Baby", "Young", "Gourmet", "Earth", "Essence",
			"Eclectic", "Nature", "Magpie", "Ninja", "Dragon"
	};
	private static final int[] COLORS = {
			0xFFA0A0, 0xC8FFC8, 0xC8C8FF, 0xC07040, 0xFFFFFF,
			0x30FF30, 0x00C000, 0x4040FF, 0x202020, 0xFF2020
	};
	private static final HashSet seen = new HashSet();

	static void load(Properties props) {
		enabled = readBool(props, "implings", false);
		names = readBool(props, "implingNames", true);
		minimapNames = readBool(props, "implingMinimap", true);
		notify = readBool(props, "implingNotify", true);
	}

	static void save(Properties props) {
		props.setProperty("implings", Boolean.toString(enabled));
		props.setProperty("implingNames", Boolean.toString(names));
		props.setProperty("implingMinimap", Boolean.toString(minimapNames));
		props.setProperty("implingNotify", Boolean.toString(notify));
	}

	static boolean isImpling(NPC npc) {
		if (npc == null || npc.desc == null) {
			return false;
		}
		String name = npc.desc.name;
		if (name != null && name.toLowerCase().indexOf("impling") >= 0) {
			return true;
		}
		int id = (int) npc.desc.interfaceType;
		return id >= FIRST && id <= LAST && hasCatch(npc.desc);
	}

	private static boolean hasCatch(EntityDef def) {
		if (def == null || def.actions == null) {
			return false;
		}
		for (int i = 0; i < def.actions.length; i++) {
			if (def.actions[i] != null && def.actions[i].equalsIgnoreCase("Catch")) {
				return true;
			}
		}
		return false;
	}

	static int color(NPC npc) {
		int idx = typeIndex(npc);
		if (idx < 0) {
			return 0x00FF00;
		}
		return COLORS[idx];
	}

	static String typeName(NPC npc) {
		int idx = typeIndex(npc);
		if (idx < 0) {
			return "Impling";
		}
		return TYPE_NAMES[idx] + " impling";
	}

	static void onNpc(client c, NPC npc) {
		if (!enabled || !notify || !isImpling(npc) || c == null) {
			return;
		}
		String key = ((int) npc.desc.interfaceType) + ":" + npc.smallX[0] + ":" + npc.smallY[0];
		if (seen.contains(key)) {
			return;
		}
		seen.add(key);
		c.pushMessage(typeName(npc) + " spawned nearby.", 0, "");
	}

	static void resetSeen() {
		seen.clear();
	}

	static void drawHull(client c, NPC npc) {
		if (!enabled || !isImpling(npc)) {
			return;
		}
		NpcIndicators.drawHull(c, npc, color(npc));
	}

	static void drawName(client c, NPC npc, TextDrawingArea font) {
		if (!enabled || !names || !isImpling(npc) || font == null) {
			return;
		}
		c.npcScreenPos(npc, npc.height + 15);
		int x = c.getSpriteDrawX();
		int y = c.getSpriteDrawY();
		if (x < 0 || y < 0) {
			return;
		}
		String label = typeName(npc);
		int w = font.getTextWidth(label);
		int[] p = OverlayManager.placeWorld(x - w / 2, y - 10, w, 12);
		font.drawText(color(npc), label, p[1] + 10, p[0] + w / 2);
	}

	private static int typeIndex(NPC npc) {
		if (npc == null || npc.desc == null) {
			return -1;
		}
		String name = npc.desc.name;
		if (name != null) {
			String lower = name.toLowerCase();
			for (int i = 0; i < TYPE_NAMES.length; i++) {
				if (lower.startsWith(TYPE_NAMES[i].toLowerCase())) {
					return i;
				}
			}
		}
		int id = (int) npc.desc.interfaceType - FIRST;
		if (id < 0 || id >= TYPE_NAMES.length) {
			return -1;
		}
		return id;
	}

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}
}
