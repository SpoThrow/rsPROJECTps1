final class StatusTimers {

	static long freezeUntil;
	static long vengUntil;
	static long teleblockUntil;
	static long antifireUntil;
	static long energyUntil;

	static void onMessage(String text) {
		if (text == null) {
			return;
		}
		String s = text.toLowerCase();
		long now = System.currentTimeMillis();
		if (s.indexOf("you have been frozen") >= 0) {
			freezeUntil = now + 20000L;
		} else if (s.indexOf("you have been teleblocked") >= 0) {
			teleblockUntil = now + 300000L;
		} else if (s.indexOf("immunity against dragon fire") >= 0) {
			antifireUntil = now + 120000L;
		} else if (s.indexOf("resistance to dragon fire has worn off") >= 0) {
			antifireUntil = 0L;
		} else if (s.indexOf("taste vengeance") >= 0) {
			vengUntil = 0L;
		} else if (s.indexOf("energy potion") >= 0 || s.indexOf("super energy") >= 0) {
			energyUntil = now + 180000L;
		}
	}

	static void onGraphic(int gfx) {
		if (gfx == 604) {
			vengUntil = System.currentTimeMillis() + 30000L;
		}
	}

	static void draw(TextDrawingArea font, int x, int y) {
		if (font == null) {
			return;
		}
		long now = System.currentTimeMillis();
		y = drawOne(font, "Freeze", freezeUntil, now, 0x66CCFF, x, y);
		y = drawOne(font, "Vengeance", vengUntil, now, 0x33CC33, x, y);
		y = drawOne(font, "Teleblock", teleblockUntil, now, 0xCC66FF, x, y);
		y = drawOne(font, "Antifire", antifireUntil, now, 0xFF981F, x, y);
		drawOne(font, "Energy", energyUntil, now, 0xE6C832, x, y);
	}

	private static int drawOne(TextDrawingArea font, String name, long until, long now, int color, int x, int y) {
		if (until <= now) {
			return y;
		}
		int sec = (int) ((until - now) / 1000L);
		String text = name + " " + formatTime(sec);
		DrawingArea.method335(0x000000, y, font.getTextWidth(text) + 8, 14, 140, x);
		font.method385(color, text, y + 11, x + 4);
		return y + 15;
	}

	private static String formatTime(int sec) {
		if (sec < 0) {
			sec = 0;
		}
		int m = sec / 60;
		int s = sec % 60;
		if (m <= 0) {
			return s + "s";
		}
		if (s < 10) {
			return m + ":0" + s;
		}
		return m + ":" + s;
	}
}
