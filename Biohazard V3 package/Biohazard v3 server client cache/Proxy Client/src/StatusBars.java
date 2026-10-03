import java.util.Properties;

/** HP and prayer bars in the inventory stone columns, matching Old School RuneScape. */
final class StatusBars {

	static boolean enabled = true;
	static boolean numbers = true;
	static boolean icons = true;
	static boolean healPreview = true;

	static int hoverHeal;
	static int posX = 4;
	static int posY = 37;

	private static final int TAB_W = 246;
	private static final int TAB_H = 335;
	private static final int BAR_W = 20;
	private static final int INSET = 4;
	private static final int TOP = 37;
	private static final int BOTTOM = 298;
	private static final int HP_FILL = 0xCC2020;
	private static final int PRAY_FILL = 0x00B8C4;

	static void load(Properties props) {
		enabled = readBool(props, "statusBars", true);
		numbers = readBool(props, "statusBarNumbers", true);
		icons = readBool(props, "statusBarIcons", true);
		healPreview = readBool(props, "statusBarHeal", true);
		posX = readInt(props, "statusBarX", INSET);
		posY = readInt(props, "statusBarY", TOP);
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
		return false;
	}

	static boolean processDrag(int localX, int localY, int click2, int click3, int saveX, int saveY, int tabW, int tabH) {
		return false;
	}

	static void draw(TextDrawingArea font, int hp, int maxHp, int pray, int maxPray, Sprite hpIcon, Sprite prayIcon) {
		if (!enabled) {
			return;
		}
		int h = BOTTOM - TOP;
		if (h < 32) {
			h = TAB_H - TOP - 37;
		}
		int leftX = INSET;
		int rightX = TAB_W - INSET - BAR_W;
		drawBar(leftX, TOP, BAR_W, h, hp, maxHp, HP_FILL, hoverHeal, font, hpIcon);
		drawBar(rightX, TOP, BAR_W, h, pray, maxPray, PRAY_FILL, 0, font, prayIcon);
	}

	private static void drawBar(int x, int y, int w, int h, int cur, int max, int color, int extra,
			TextDrawingArea font, Sprite icon) {
		DrawingArea.method335(0x000000, y, w, h, 140, x);
		DrawingArea.fillPixels(x, w, h, 0x2A241C, y);
		if (max <= 0) {
			max = 1;
		}
		int inner = w - 2;
		int fillH = cur * (h - 2) / max;
		if (fillH > h - 2) {
			fillH = h - 2;
		}
		if (fillH < 0) {
			fillH = 0;
		}
		int fy = y + h - 1 - fillH;
		if (fillH > 0) {
			DrawingArea.drawPixels(fillH, fy, x + 1, color, inner);
		}
		if (healPreview && extra > 0) {
			int healFill = extra * (h - 2) / max;
			if (fillH + healFill > h - 2) {
				healFill = h - 2 - fillH;
			}
			if (healFill > 0) {
				DrawingArea.method335(0xFFFF00, fy - healFill, inner, healFill, 120, x + 1);
			}
		}
		if (icons && icon != null && icon.myWidth > 0) {
			int ix = x + (w - icon.myWidth) / 2;
			int iy = y + 2;
			icon.drawSprite(ix, iy);
		}
		if (numbers && font != null) {
			int textY = fy + fillH / 2 + 4;
			if (textY < y + 22) {
				textY = y + 22;
			}
			if (textY > y + h - 8) {
				textY = y + h - 8;
			}
			font.method382(0xFFFFFF, x + w / 2, Integer.toString(cur), textY, true);
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
