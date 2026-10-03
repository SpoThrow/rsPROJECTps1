final class StatusTimers {

	static long freezeUntil;
	static long vengUntil;
	static long vengOtherUntil;
	static long teleblockUntil;
	static long antifireUntil;
	static long energyUntil;
	static long poisonUntil;
	static long antipoisonUntil;
	static long specUntil;
	static long chargeUntil;

	private static Sprite vengSpell;
	private static Sprite vengOtherSkull;
	private static boolean triedVengLoad;

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
		} else if (s.indexOf("vengeance other") >= 0 || s.indexOf("has cast vengeance") >= 0) {
			vengOtherUntil = now + 30000L;
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
		long now = System.currentTimeMillis();
		if (gfx == 369 || gfx == 363 || gfx == 360 || gfx == 181) {
			freezeUntil = now + 20000L;
		} else if (gfx == 604) {
			vengUntil = now + 30000L;
		} else if (gfx == 643 || gfx == 437) {
			vengOtherUntil = now + 30000L;
		}
	}

	static void draw(TextDrawingArea font) {
		if (font == null) {
			return;
		}
		ensureSprites();
		long now = System.currentTimeMillis();
		icon("stFreeze", 1582, freezeUntil, now, 0x66CCFF);
		iconSprite("stVeng", vengSpell, vengUntil, now, 0xFF3030);
		iconSprite("stVengO", vengOtherSkull, vengOtherUntil, now, 0xFFFFFF);
		icon("stTb", 563, teleblockUntil, now, 0xCC66FF);
		icon("stAf", 2452, antifireUntil, now, 0xFF981F);
		icon("stNrg", 3016, energyUntil, now, 0xE6C832);
		icon("stPsn", 2446, poisonUntil, now, 0x66FF33);
		icon("stAp", 2448, antipoisonUntil, now, 0x99FF66);
		icon("stSpec", 4151, specUntil, now, 0xFFFF66);
		icon("stChg", 2415, chargeUntil, now, 0x66CCFF);
	}

	private static void ensureSprites() {
		if (vengSpell == null && !triedVengLoad && RSInterface.interfaceCache != null) {
			triedVengLoad = true;
			vengSpell = widgetSprite(30306);
			if (vengSpell == null) {
				vengSpell = RSInterface.imageLoader(36, "Lunar/LUNARON");
			}
			if (vengSpell != null && vengSpell.myWidth <= 0) {
				vengSpell = null;
			}
		}
		if (vengOtherSkull == null && client.instance != null) {
			vengOtherSkull = client.instance.pkSkullIcon();
		}
	}

	private static Sprite widgetSprite(int id) {
		if (RSInterface.interfaceCache == null || id < 0 || id >= RSInterface.interfaceCache.length) {
			return null;
		}
		RSInterface rsi = RSInterface.interfaceCache[id];
		if (rsi == null) {
			return null;
		}
		if (rsi.sprite2 != null && rsi.sprite2.myWidth > 0) {
			return rsi.sprite2;
		}
		if (rsi.sprite1 != null && rsi.sprite1.myWidth > 0) {
			return rsi.sprite1;
		}
		return null;
	}

	private static void icon(String id, int item, long until, long now, int color) {
		if (until <= now) {
			return;
		}
		int sec = (int) ((until - now) / 1000L);
		InfoBoxes.icon(id, item, formatTime(sec), color);
	}

	private static void iconSprite(String id, Sprite sprite, long until, long now, int color) {
		if (until <= now) {
			return;
		}
		int sec = (int) ((until - now) / 1000L);
		InfoBoxes.icon(id, sprite, formatTime(sec), color);
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
