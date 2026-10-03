import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

final class InventoryTags {

	static final int ACTION_TAG = 1620;
	static final int ACTION_UNTAG = 1626;

	static boolean enabled;
	static int style;
	static int opacity = 55;

	static final int STYLE_UNDERLINE = 0;
	static final int STYLE_OUTLINE = 1;
	static final int STYLE_FILL = 2;

	private static final int[] COLORS = { 0xFF3030, 0x30FF60, 0x3090FF, 0xFFFF00, 0xFF40FF, 0xFF981F };
	private static final String[] NAMES = { "Red", "Green", "Blue", "Yellow", "Pink", "Orange" };
	private static final HashMap tags = new HashMap();

	static void load(Properties props) {
		enabled = readBool(props, "invTags", false);
		style = clamp(readInt(props, "invTagStyle", 0), 0, 2);
		opacity = clamp(readInt(props, "invTagOpacity", 55), 10, 100);
		tags.clear();
		String packed = props.getProperty("invTagList", "");
		if (packed == null || packed.length() == 0) {
			return;
		}
		String[] parts = packed.split(";");
		for (int i = 0; i < parts.length; i++) {
			String[] bits = parts[i].split("=");
			if (bits.length < 2) {
				continue;
			}
			try {
				tags.put(Integer.valueOf(Integer.parseInt(bits[0].trim())),
						Integer.valueOf(Integer.parseInt(bits[1].trim())));
			} catch (Exception e) {
			}
		}
	}

	static void save(Properties props) {
		props.setProperty("invTags", Boolean.toString(enabled));
		props.setProperty("invTagStyle", Integer.toString(style));
		props.setProperty("invTagOpacity", Integer.toString(opacity));
		StringBuffer sb = new StringBuffer();
		Iterator it = tags.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry e = (Map.Entry) it.next();
			if (sb.length() > 0) {
				sb.append(';');
			}
			sb.append(e.getKey()).append('=').append(e.getValue());
		}
		props.setProperty("invTagList", sb.toString());
	}

	static int colorOf(int itemId) {
		Integer c = (Integer) tags.get(Integer.valueOf(itemId));
		return c == null ? 0 : c.intValue();
	}

	static boolean isAction(int id) {
		if (id >= 2000) {
			id -= 2000;
		}
		return id >= ACTION_TAG && id <= ACTION_UNTAG;
	}

	static int addShiftEntries(String[] names, int[] ids, int[] cmd1, int[] cmd2, int[] cmd3, int row) {
		if (!enabled || row <= 0) {
			return row;
		}
		int item = -1;
		String label = "";
		for (int i = 0; i < row; i++) {
			if (!isInvAction(ids[i])) {
				continue;
			}
			item = cmd1[i];
			label = target(names[i]);
			break;
		}
		if (item < 0) {
			return row;
		}
		if (colorOf(item) != 0) {
			row = prepend(names, ids, cmd1, cmd2, cmd3, row, "Remove tag" + suffix(label), ACTION_UNTAG, item, 0, 0);
		}
		for (int i = NAMES.length - 1; i >= 0; i--) {
			row = prepend(names, ids, cmd1, cmd2, cmd3, row, "Tag " + NAMES[i] + suffix(label), ACTION_TAG + i, item, 0, 0);
		}
		return row;
	}

	static void handle(int action, int itemId, client c) {
		if (itemId < 0) {
			return;
		}
		if (action == ACTION_UNTAG) {
			tags.remove(Integer.valueOf(itemId));
			c.pushMessage("Inventory tag removed.", 0, "");
			return;
		}
		int idx = action - ACTION_TAG;
		if (idx < 0 || idx >= COLORS.length) {
			return;
		}
		tags.put(Integer.valueOf(itemId), Integer.valueOf(COLORS[idx]));
		c.pushMessage("Tagged " + NAMES[idx].toLowerCase() + ".", 0, "");
	}

	static void cycleStyle() {
		style = (style + 1) % 3;
	}

	static String styleLabel() {
		if (style == STYLE_OUTLINE) {
			return "Outline";
		}
		if (style == STYLE_FILL) {
			return "Fill";
		}
		return "Underline";
	}

	static void setOpacity(int value) {
		opacity = clamp(value, 10, 100);
	}

	static void drawSlot(int itemId, int x, int y) {
		int color = colorOf(itemId);
		if (color == 0) {
			return;
		}
		int alpha = 20 + (opacity * 180) / 100;
		if (style == STYLE_FILL) {
			DrawingArea.method335(color, y + 1, 31, 31, alpha, x + 1);
			return;
		}
		if (style == STYLE_OUTLINE) {
			int x2 = x + 31;
			int y2 = y + 31;
			DrawingArea.drawLine(x + 1, y + 1, x2, y + 1, color);
			DrawingArea.drawLine(x + 1, y2, x2, y2, color);
			DrawingArea.drawLine(x + 1, y + 1, x + 1, y2, color);
			DrawingArea.drawLine(x2, y + 1, x2, y2, color);
			if (opacity >= 60) {
				DrawingArea.drawLine(x + 2, y + 2, x2 - 1, y + 2, color);
				DrawingArea.drawLine(x + 2, y2 - 1, x2 - 1, y2 - 1, color);
			}
			return;
		}
		DrawingArea.drawLine(x + 2, y + 30, x + 30, y + 30, color);
		DrawingArea.drawLine(x + 2, y + 29, x + 30, y + 29, color);
	}

	private static int clamp(int v, int min, int max) {
		if (v < min) {
			return min;
		}
		if (v > max) {
			return max;
		}
		return v;
	}

	private static int readInt(Properties props, String key, int def) {
		try {
			return Integer.parseInt(props.getProperty(key, Integer.toString(def)));
		} catch (Exception e) {
			return def;
		}
	}

	private static boolean isInvAction(int id) {
		if (id >= 2000) {
			id -= 2000;
		}
		return id == 632 || id == 78 || id == 867 || id == 431 || id == 53 || id == 74 || id == 454 || id == 539
				|| id == 493 || id == 847 || id == 447 || id == 1125;
	}

	private static String target(String name) {
		if (name == null) {
			return "";
		}
		int at = name.lastIndexOf("@lre@");
		if (at >= 0) {
			return name.substring(at + 5).trim();
		}
		return "";
	}

	private static String suffix(String label) {
		return label.length() > 0 ? " @lre@" + label : "";
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

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}
}
