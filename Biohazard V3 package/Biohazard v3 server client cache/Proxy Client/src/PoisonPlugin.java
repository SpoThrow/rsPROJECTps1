import java.util.Properties;

/** Poison infobox, HP-orb recolour, and next-hit tooltip. */
final class PoisonPlugin {

	static boolean enabled = true;

	private static int damage;
	private static int ticks = -1;
	private static long lastUpdate;

	static void load(Properties props) {
		enabled = readBool(props, "poisonPlugin", true);
	}

	static void save(Properties props) {
		props.setProperty("poisonPlugin", Boolean.toString(enabled));
	}

	static boolean apply(String text) {
		if (text == null || !text.startsWith("poison:")) {
			return false;
		}
		try {
			damage = Integer.parseInt(text.substring(7).trim());
			if (damage <= 0) {
				damage = 0;
				ticks = -1;
			} else {
				ticks = 36;
				lastUpdate = System.currentTimeMillis();
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	static boolean poisoned() {
		return enabled && damage > 0;
	}

	static void drawInfo(TextDrawingArea font) {
		if (!poisoned() || font == null) {
			return;
		}
		int remain = remainingSeconds();
		InfoBoxes.draw("poison", font, "Poison -" + damage + "  next " + remain + "s", 0x33CC33);
	}

	static String orbTooltip() {
		if (!poisoned()) {
			return null;
		}
		return "Next poison damage: " + damage + " in " + remainingSeconds() + "s";
	}

	static void tintHpOrb(int ox, int oy, int ow, int oh) {
		if (!poisoned()) {
			return;
		}
		if (ow <= 0) {
			ow = 57;
		}
		if (oh <= 0) {
			oh = 34;
		}
		DrawingArea.method335(0x228822, oy, ow, oh, 70, ox);
	}

	private static int remainingSeconds() {
		if (ticks < 0) {
			return 0;
		}
		int elapsed = (int) ((System.currentTimeMillis() - lastUpdate) / 600L);
		int left = ticks - elapsed;
		if (left < 0) {
			left = 0;
		}
		return (left * 6 + 5) / 10;
	}

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}
}
