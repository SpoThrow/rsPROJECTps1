package ui;

import java.util.Properties;import def.ItemDef;



public final class AmmoOverlay {

	public static boolean enabled = true;

	public static void load(Properties props) {
		enabled = readBool(props, "ammoOverlay", true);
	}

	public static void save(Properties props) {
		props.setProperty("ammoOverlay", Boolean.toString(enabled));
	}

	public static void draw(TextDrawingArea font) {
		if (!enabled || font == null || RSInterface.interfaceCache == null) {
			return;
		}
		RSInterface worn = 1688 < RSInterface.interfaceCache.length ? RSInterface.interfaceCache[1688] : null;
		if (worn == null || worn.inv == null) {
			return;
		}
		int slot = worn.inv.length > 13 ? 13 : worn.inv.length > 3 ? 3 : -1;
		if (slot == 13 && worn.inv[13] <= 0 && worn.inv.length > 3) {
			slot = 3;
		}
		if (slot < 0 || worn.inv[slot] <= 0) {
			return;
		}
		int id = worn.inv[slot] - 1;
		ItemDef def = ItemDef.forID(id);
		if (def == null || def.name == null) {
			return;
		}
		if (slot == 3 && !isAmmoName(def.name)) {
			return;
		}
		int amt = worn.invStackSizes != null && slot < worn.invStackSizes.length ? worn.invStackSizes[slot] : 1;
		int color = amt <= 50 ? 0xFF3030 : amt <= 150 ? 0xFF981F : 0x33CC66;
		InfoBoxes.draw("ammo", font, def.name + " x" + amt, color);
	}

	private static boolean isAmmoName(String name) {
		String n = name.toLowerCase();
		return n.indexOf("arrow") >= 0 || n.indexOf("bolt") >= 0 || n.indexOf("dart") >= 0
				|| n.indexOf("javelin") >= 0 || n.indexOf("knife") >= 0 || n.indexOf("brutal") >= 0;
	}

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}
}
