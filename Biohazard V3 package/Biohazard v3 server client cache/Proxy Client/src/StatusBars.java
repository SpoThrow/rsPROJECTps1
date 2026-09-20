import java.util.Properties;

/** HP and prayer bars beside the inventory, with optional numbers and heal preview. */
final class StatusBars {

	static boolean enabled = true;
	static boolean numbers = true;
	static boolean icons = true;
	static boolean healPreview = true;

	static int hoverHeal;
	static int posX = 4;
	static int posY = 42;

	private static final int BAR_H = 170;
	private static int boxW = 22;
	private static int boxH = 194;
	private static boolean dragging;
	private static int grabX;
	private static int grabY;

	static void load(Properties props) {
		enabled = readBool(props, "statusBars", true);
		numbers = readBool(props, "statusBarNumbers", true);
		icons = readBool(props, "statusBarIcons", true);
		healPreview = readBool(props, "statusBarHeal", true);
		posX = readInt(props, "statusBarX", 4);
		posY = readInt(props, "statusBarY", 42);
	}

	static void save(Properties props) {
		props.setProperty("statusBars", Boolean.toString(enabled));
		props.setProperty("statusBarNumbers", Boolean.toString(numbers));
		props.setProperty("statusBarIcons", Boolean.toString(icons));
		props.setProperty("statusBarHeal", Boolean.toString(healPreview));
		props.setProperty("statusBarX", Integer.toString(posX));
		props.setProperty("statusBarY", Integer.toString(posY));
	}

	static boolean dragging() {
		return dragging;
	}

	static boolean processDrag(int localX, int localY, int click2, int click3, int saveX, int saveY, int tabW, int tabH) {
		if (!enabled) {
			dragging = false;
			return false;
		}
		if (dragging) {
			if (click2 != 1 || !RSApplet.altIsDown) {
				dragging = false;
				clamp(tabW, tabH);
				if (client.instance != null) {
					client.instance.saveClientSettings();
				}
				return false;
			}
			posX = localX - grabX;
			posY = localY - grabY;
			clamp(tabW, tabH);
			return true;
		}
		if (!RSApplet.altIsDown) {
			return false;
		}
		if (click2 != 1 && click3 != 1) {
			return false;
		}
		int sx = click3 == 1 ? saveX : localX;
		int sy = click3 == 1 ? saveY : localY;
		if (sx < posX || sy < posY || sx >= posX + boxW || sy >= posY + boxH) {
			return false;
		}
		dragging = true;
		grabX = sx - posX;
		grabY = sy - posY;
		posX = localX - grabX;
		posY = localY - grabY;
		clamp(tabW, tabH);
		return true;
	}

	static void draw(TextDrawingArea font, int hp, int maxHp, int pray, int maxPray) {
		if (!enabled) {
			return;
		}
		int x = posX;
		int y = posY;
		int h = BAR_H;
		int w = 6;
		boxW = 22;
		boxH = h + 24;
		if (RSApplet.altIsDown || dragging) {
			DrawingArea.method335(0x000000, y - 12, boxW, boxH, 80, x - 2);
			DrawingArea.fillPixels(x - 2, boxW, boxH, 0xFFE14A, y - 12);
		}
		drawBar(x, y, w, h, hp, maxHp, 0xCC2020, hoverHeal);
		if (numbers && font != null) {
			font.method385(0xFFFFFF, Integer.toString(hp), y + h + 12, x - 2);
		}
		if (icons) {
			DrawingArea.drawPixels(6, y - 10, x, 0xCC2020, 6);
		}
		int px = x + 10;
		drawBar(px, y, w, h, pray, maxPray, 0x00A0FF, 0);
		if (numbers && font != null) {
			font.method385(0xFFFFFF, Integer.toString(pray), y + h + 12, px - 2);
		}
		if (icons) {
			DrawingArea.drawPixels(6, y - 10, px, 0x00A0FF, 6);
		}
	}

	private static void clamp(int tabW, int tabH) {
		if (tabW < 32) {
			tabW = 246;
		}
		if (tabH < 32) {
			tabH = 335;
		}
		if (posX < 0) {
			posX = 0;
		}
		if (posY < 12) {
			posY = 12;
		}
		if (posX + boxW > tabW) {
			posX = Math.max(0, tabW - boxW);
		}
		if (posY + boxH > tabH - 40) {
			posY = Math.max(12, tabH - 40 - boxH);
		}
	}

	private static void drawBar(int x, int y, int w, int h, int cur, int max, int color, int extra) {
		DrawingArea.method335(0x000000, y, w, h, 180, x);
		DrawingArea.fillPixels(x, w, h, 0x3A3228, y);
		if (max <= 0) {
			max = 1;
		}
		int fill = cur * (h - 2) / max;
		if (fill > h - 2) {
			fill = h - 2;
		}
		if (fill < 0) {
			fill = 0;
		}
		int fy = y + h - 1 - fill;
		if (fill > 0) {
			DrawingArea.drawPixels(fill, fy, x + 1, color, w - 2);
		}
		if (healPreview && extra > 0) {
			int healFill = extra * (h - 2) / max;
			if (fill + healFill > h - 2) {
				healFill = h - 2 - fill;
			}
			if (healFill > 0) {
				DrawingArea.method335(0xFFFF00, fy - healFill, w - 2, healFill, 120, x + 1);
			}
		}
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
