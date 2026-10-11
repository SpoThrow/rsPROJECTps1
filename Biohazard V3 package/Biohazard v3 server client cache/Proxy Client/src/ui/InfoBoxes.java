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
		p.infobox = true;
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
		p.infobox = true;
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

	/**
	 * The item's sprite drawn as a <em>bundle</em> rather than as one of it.
	 *
	 * <p>{@link ItemDef#getSprite} swaps in a wider model when the amount it is asked for crosses
	 * one of the item's {@code stackAmounts} steps - a single arrow becomes the handful of arrows
	 * the inventory draws when you pick a stack up. That is what an ammunition infobox wants: one
	 * arrow beside "1,000" reads as a single arrow with a big number, a bundle of them reads as
	 * ammunition. Passing a million asks for the last step the item defines, so it is the fullest
	 * bundle there is whatever steps that particular item was authored with.
	 *
	 * <p>Falls back to the single-item sprite, which is what a non-stackable item renders for any
	 * amount anyway - {@code getSprite} forces {@code j = -1} when the item has no {@code stackIDs}.
	 */
	static Sprite stackSprite(int itemId) {
		try {
			Sprite bundled = ItemDef.getSprite(itemId, STACK_SPRITE_AMOUNT, 0);
			return bundled != null ? bundled : itemSprite(itemId);
		} catch (Exception e) {
			return itemSprite(itemId);
		}
	}

	/** Far past any {@code stackAmounts} step, so the item's largest bundle model is the one drawn. */
	private static final int STACK_SPRITE_AMOUNT = 1000000;

	public static void icon(String id, int itemId, String text, int color) {
		icon(id, itemSprite(itemId), text, color);
	}

	/**
	 * An infobox with an item icon and its value drawn <em>over</em> the icon.
	 *
	 * <p>The value used to sit to the right of the sprite, on a wide panel. It now sits over the
	 * bottom of the sprite in the RuneLite style: the panel is the size of the icon, the number is
	 * centred along the bottom, and four black pixels behind it keep it legible against the art.
	 */
	static void icon(String id, Sprite sprite, String text, int color) {
		if (text == null) {
			text = "";
		}
		int iconW = sprite != null ? sprite.myWidth : 32;
		if (iconW < 1) {
			iconW = 32;
		}
		int iconH = sprite != null ? sprite.myHeight : 32;
		if (iconH < 1) {
			iconH = 32;
		}
		int tw = fontWidth(text);
		int w = Math.max(iconW + 4, tw + 4);
		int h = iconH + 2;
		OverlayManager.Panel p = OverlayManager.place(id, OverlayManager.TOP_LEFT, w, h);
		p.infobox = true;
		OverlayManager.paint(p, 160);
		if (sprite != null) {
			sprite.drawSprite(p.x + (w - iconW) / 2, p.y + 1);
		}
		if (text.length() > 0 && client.instance != null && client.instance.smallText != null) {
			TextDrawingArea f = client.instance.smallText;
			int tx = p.x + (w - tw) / 2;
			int ty = p.y + h - 4;
			f.method385(0, text, ty - 1, tx);
			f.method385(0, text, ty + 1, tx);
			f.method385(0, text, ty, tx - 1);
			f.method385(0, text, ty, tx + 1);
			f.method385(color, text, ty, tx);
		}
	}

	private static int fontWidth(String text) {
		if (client.instance == null || client.instance.smallText == null || text == null) {
			return text == null ? 0 : text.length() * 7;
		}
		return client.instance.smallText.getTextWidth(text);
	}
}
