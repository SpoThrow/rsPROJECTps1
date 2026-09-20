import java.util.Properties;

final class ItemStats {

	static boolean enabled = true;

	static void load(Properties props) {
		enabled = readBool(props, "itemStats", true);
	}

	static void save(Properties props) {
		props.setProperty("itemStats", Boolean.toString(enabled));
	}

	static void drawAt(TextDrawingArea font, int itemId, int hp, int prayer, int run, int x, int y) {
		String[] lines = lines(itemId, hp, prayer, run);
		if (font == null || lines == null) {
			return;
		}
		int w = 8;
		for (int i = 0; i < lines.length; i++) {
			int tw = font.getTextWidth(lines[i]) + 8;
			if (tw > w) {
				w = tw;
			}
		}
		int h = lines.length * 14 + 4;
		int drawY = y - h - 2;
		if (drawY < DrawingArea.topY) {
			drawY = y + 32;
		}
		if (x + w > DrawingArea.bottomX) {
			x = DrawingArea.bottomX - w;
		}
		if (x < DrawingArea.topX) {
			x = DrawingArea.topX;
		}
		DrawingArea.method335(0x000000, drawY, w, h, 180, x);
		DrawingArea.fillPixels(x, w, h, 0x5A4933, drawY);
		for (int i = 0; i < lines.length; i++) {
			font.method385(0xff981f, lines[i], drawY + 13 + i * 14, x + 4);
		}
	}

	static String[] lines(int itemId, int hp, int prayer, int run) {
		if (!enabled || itemId < 0) {
			return null;
		}
		String heal = healLine(itemId, hp);
		String pot = potionLine(itemId);
		if (heal == null && pot == null) {
			return null;
		}
		if (heal != null && pot != null) {
			return new String[] { heal, pot };
		}
		return new String[] { heal != null ? heal : pot };
	}

	static int healAmount(int id) {
		if (id == 315 || id == 319) {
			return 3;
		}
		if (id == 333 || id == 351) {
			return 7;
		}
		if (id == 329 || id == 361) {
			return 9;
		}
		if (id == 379) {
			return 12;
		}
		if (id == 373) {
			return 14;
		}
		if (id == 385) {
			return 20;
		}
		if (id == 397) {
			return 21;
		}
		if (id == 391) {
			return 22;
		}
		if (id == 7946) {
			return 16;
		}
		if (id == 3144) {
			return 18;
		}
		if (id == 2309 || id == 1891 || id == 1893 || id == 1895 || id == 4049) {
			return 5;
		}
		return 0;
	}

	private static String healLine(int id, int hp) {
		int amt = healAmount(id);
		if (amt <= 0) {
			return null;
		}
		int next = hp + amt;
		return "Heals " + amt + " (" + hp + " -> " + next + ")";
	}

	private static String potionLine(int id) {
		if (id == 2428 || id == 121 || id == 123 || id == 125) {
			return "Attack +10% +3";
		}
		if (id == 113 || id == 115 || id == 117 || id == 119) {
			return "Strength +10% +3";
		}
		if (id == 2432 || id == 133 || id == 135 || id == 137) {
			return "Defence +10% +3";
		}
		if (id == 2434 || id == 139 || id == 141 || id == 143) {
			return "Prayer +7";
		}
		if (id == 2436 || id == 145 || id == 147 || id == 149) {
			return "Attack +15% +5";
		}
		if (id == 2440 || id == 157 || id == 159 || id == 161) {
			return "Strength +15% +5";
		}
		if (id == 2442 || id == 163 || id == 165 || id == 167) {
			return "Defence +15% +5";
		}
		if (id == 2444 || id == 169 || id == 171 || id == 173) {
			return "Ranged +10% +4";
		}
		if (id == 3040 || id == 3042 || id == 3044 || id == 3046) {
			return "Magic +4";
		}
		if (id == 2430 || id == 127 || id == 129 || id == 131) {
			return "Restore stats";
		}
		if (id == 3024 || id == 3026 || id == 3028 || id == 3030) {
			return "Super restore + Prayer";
		}
		if (id == 2438 || id == 151 || id == 153 || id == 155) {
			return "Restore run energy";
		}
		if (id == 2446 || id == 175 || id == 177 || id == 179) {
			return "Cures poison";
		}
		if (id == 2448 || id == 181 || id == 183 || id == 185) {
			return "Poison immunity ~6m";
		}
		if (id == 2452 || id == 2454 || id == 2456 || id == 2458) {
			return "Antifire ~6m";
		}
		if (id == 6685 || id == 6687 || id == 6689 || id == 6691) {
			return "Saradomin brew: HP+, combat-";
		}
		if (id == 2450 || id == 189 || id == 191 || id == 193) {
			return "Zamorak brew: Attack/Str+, HP-";
		}
		return null;
	}

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}
}
