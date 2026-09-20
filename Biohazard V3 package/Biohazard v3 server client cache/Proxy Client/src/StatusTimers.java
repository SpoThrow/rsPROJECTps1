final class StatusTimers {

	static long freezeUntil;
	static long vengUntil;
	static long teleblockUntil;
	static long antifireUntil;
	static long energyUntil;
	static long poisonUntil;
	static long antipoisonUntil;
	static long specUntil;
	static long chargeUntil;

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
		} else if (s.indexOf("you have been poisoned") >= 0 || s.indexOf("you start to poison") >= 0) {
			poisonUntil = now + 90000L;
		} else if (s.indexOf("poison has worn off") >= 0 || s.indexOf("cured") >= 0 && s.indexOf("poison") >= 0) {
			poisonUntil = 0L;
			if (s.indexOf("superantipoison") >= 0 || s.indexOf("anti-poison") >= 0 || s.indexOf("antipoison") >= 0) {
				antipoisonUntil = now + 360000L;
			}
		} else if (s.indexOf("antipoison") >= 0 || s.indexOf("anti-poison") >= 0) {
			antipoisonUntil = now + 360000L;
			poisonUntil = 0L;
		} else if (s.indexOf("special attack") >= 0 && (s.indexOf("restored") >= 0 || s.indexOf("recharged") >= 0)) {
			specUntil = now + 30000L;
		} else if (s.indexOf("charge") >= 0 && s.indexOf("spell") >= 0) {
			chargeUntil = now + 420000L;
		}
	}

	static void onGraphic(int gfx) {
		if (gfx == 604) {
			vengUntil = System.currentTimeMillis() + 30000L;
		}
	}

	static void draw(TextDrawingArea font) {
		if (font == null) {
			return;
		}
		long now = System.currentTimeMillis();
		InfoBoxes.start("statusTimers", font);
		add(font, "Freeze", freezeUntil, now, 0x66CCFF);
		add(font, "Vengeance", vengUntil, now, 0x33CC33);
		add(font, "Teleblock", teleblockUntil, now, 0xCC66FF);
		add(font, "Antifire", antifireUntil, now, 0xFF981F);
		add(font, "Energy", energyUntil, now, 0xE6C832);
		add(font, "Poison", poisonUntil, now, 0x66FF33);
		add(font, "Antipoison", antipoisonUntil, now, 0x99FF66);
		add(font, "Spec restore", specUntil, now, 0xFFFF66);
		add(font, "Charge", chargeUntil, now, 0x66CCFF);
		InfoBoxes.flush();
	}

	private static void add(TextDrawingArea font, String name, long until, long now, int color) {
		if (until <= now) {
			return;
		}
		int sec = (int) ((until - now) / 1000L);
		InfoBoxes.line(name + " " + formatTime(sec), color);
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
