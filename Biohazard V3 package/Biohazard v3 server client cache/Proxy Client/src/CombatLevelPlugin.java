import java.util.Properties;

/** Decimal combat level on the Combat Options tab. */
final class CombatLevelPlugin {

	static boolean enabled = true;

	static void load(Properties props) {
		enabled = readBool(props, "combatLevelDecimal", true);
	}

	static void save(Properties props) {
		props.setProperty("combatLevelDecimal", Boolean.toString(enabled));
	}

	static double value(int[] maxStats) {
		if (maxStats == null || maxStats.length < 7) {
			return 3.0D;
		}
		int att = maxStats[0];
		int def = maxStats[1];
		int str = maxStats[2];
		int hp = maxStats[3];
		int range = maxStats[4];
		int pray = maxStats[5];
		int mage = maxStats[6];
		double base = 0.25D * (def + hp + Math.floor(pray / 2.0D));
		double melee = 0.325D * (att + str);
		double ranged = 0.325D * Math.floor(range * 1.5D);
		double magic = 0.325D * Math.floor(mage * 1.5D);
		double style = melee;
		if (ranged > style) {
			style = ranged;
		}
		if (magic > style) {
			style = magic;
		}
		return base + style;
	}

	static String label(int[] maxStats) {
		double v = value(maxStats);
		int tenths = (int) Math.round(v * 10.0D);
		int whole = tenths / 10;
		int frac = tenths % 10;
		if (frac < 0) {
			frac = 0;
		}
		return whole + "." + frac;
	}

	static void draw(client c, TextDrawingArea font, int[] maxStats) {
		if (!enabled || font == null || c == null || client.tabID != 0) {
			return;
		}
		String text = "Combat: " + label(maxStats);
		int x = 40;
		int y = 248;
		font.method385(0, text, y + 1, x + 1);
		font.method385(0xffff00, text, y, x);
	}

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}
}
