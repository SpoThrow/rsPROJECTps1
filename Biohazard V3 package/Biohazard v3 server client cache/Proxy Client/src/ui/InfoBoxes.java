package ui;

import def.ItemDef;
import game.client;

public final class InfoBoxes {

	private static String[] lines = new String[16];
	private static int[] colors = new int[16];
	private static int count;
	private static String boxId;
	private static TextDrawingArea font;

	static void start(String id, TextDrawingArea f) {
		flush();
		boxId = id;
		font = f;
		count = 0;
	}

	static void line(String text, int color) {
		if (text == null || count >= lines.length) {
			return;
		}
		lines[count] = text;
		colors[count] = color;
		count++;
	}

	public static void flush() {
		if (boxId != null && count > 0) {
			draw(boxId, font, lines, colors, count);
		}
		boxId = null;
		count = 0;
		font = null;
	}

	public static void draw(String id, TextDrawingArea f, String text, int color) {
		if (f == null || text == null) {
			return;
		}
		int w = f.getTextWidth(text) + 8;
		OverlayManager.Panel p = OverlayManager.place(id, OverlayManager.TOP_LEFT, w, 14);
		OverlayManager.paint(p, 150);
		f.method385(color, text, p.y + 11, p.x + 4);
	}

	static void draw(String id, TextDrawingArea f, String[] texts, int[] cols, int n) {
		if (f == null || texts == null || n <= 0) {
			return;
		}
		int w = 10;
		for (int i = 0; i < n; i++) {
			if (texts[i] == null) {
				continue;
			}
			int tw = f.getTextWidth(texts[i]) + 8;
			if (tw > w) {
				w = tw;
			}
		}
		OverlayManager.Panel p = OverlayManager.place(id, OverlayManager.TOP_LEFT, w, n * 15);
		OverlayManager.paint(p, 150);
		for (int i = 0; i < n; i++) {
			if (texts[i] == null) {
				continue;
			}
			f.method385(cols[i], texts[i], p.y + 11 + i * 15, p.x + 4);
		}
	}

	static Sprite itemSprite(int itemId) {
		try {
			return ItemDef.getSprite(itemId, 1, 0);
		} catch (Exception e) {
			return null;
		}
	}

	public static void icon(String id, int itemId, String text, int color) {
		icon(id, itemSprite(itemId), text, color);
	}

	static void icon(String id, Sprite sprite, String text, int color) {
		if (text == null) {
			text = "";
		}
		int tw = fontWidth(text);
		int icon = sprite != null ? 32 : 0;
		int w = icon + tw + 10;
		int h = 32;
		OverlayManager.Panel p = OverlayManager.place(id, OverlayManager.TOP_LEFT, w, h);
		OverlayManager.paint(p, 160);
		if (sprite != null) {
			sprite.drawSprite(p.x + 2, p.y);
		}
		if (client.instance != null && client.instance.smallText != null) {
			client.instance.smallText.method385(color, text, p.y + 21, p.x + icon + 4);
		}
	}

	private static int fontWidth(String text) {
		if (client.instance == null || client.instance.smallText == null || text == null) {
			return text == null ? 0 : text.length() * 7;
		}
		return client.instance.smallText.getTextWidth(text);
	}
}
