import java.util.Properties;

/**
 * Procedural loot beams inspired by Loot Beams Deluxe.
 * OSRS cache models are not available here, so styles are drawn in screen space.
 */
final class LootBeams {

	static final int STYLE_LIGHT = 0;
	static final int STYLE_MODERN = 1;
	static final int STYLE_FIRE = 2;
	static final int STYLE_ELECTRIC = 3;
	static final int STYLE_CLOUD = 4;
	static final int STYLE_SMOKE = 5;
	static final int STYLE_MIASMA = 6;
	static final int STYLE_GLOW = 7;
	static final int STYLE_COUNT = 8;

	static final int FANFARE_OFF = 0;
	static final int FANFARE_LIGHTNING = 1;
	static final int FANFARE_COLUMN = 2;
	static final int FANFARE_TROPHY = 3;
	static final int FANFARE_FIRE = 4;
	static final int FANFARE_BOLT = 5;
	static final int FANFARE_COUNT = 6;

	static int style = STYLE_MODERN;
	static int fanfare = FANFARE_LIGHTNING;

	private static final int FANFARE_MS = 1100;
	private static final int[] TIER_VALUE = { 20000, 100000, 500000, 1000000, 5000000, 10000000, 50000000, 100000000 };
	private static final int[] TIER_PRIMARY = { 0x66B2FF, 0x99FF99, 0x00E5D0, 0xFFB000, 0xFF6040, 0xFF66E0, 0xB266FF, 0xF5F5F5 };
	private static final int[] TIER_SECONDARY = { 0x99CCFF, 0xCCFFCC, 0x66FFF0, 0xFFD066, 0xFF9980, 0xFF99EC, 0xD1A6FF, 0xFFF0B0 };

	private static final String[] STYLE_NAMES = { "Light", "Modern", "Fire", "Electric", "Cloud", "Smoke", "Miasma", "Glow" };
	private static final String[] FANFARE_NAMES = { "Off", "Lightning", "Rising column", "Trophy", "Incinerate", "Heal bolt" };

	static void save(Properties props) {
		props.setProperty("lootBeamStyle", Integer.toString(style));
		props.setProperty("lootBeamFanfare", Integer.toString(fanfare));
	}

	static void load(Properties props) {
		style = clamp(readInt(props, "lootBeamStyle", STYLE_MODERN), 0, STYLE_COUNT - 1);
		fanfare = clamp(readInt(props, "lootBeamFanfare", FANFARE_LIGHTNING), 0, FANFARE_COUNT - 1);
	}

	static String styleName() {
		return STYLE_NAMES[clamp(style, 0, STYLE_COUNT - 1)];
	}

	static String fanfareName() {
		return FANFARE_NAMES[clamp(fanfare, 0, FANFARE_COUNT - 1)];
	}

	static void cycleStyle() {
		style = (style + 1) % STYLE_COUNT;
	}

	static void cycleFanfare() {
		fanfare = (fanfare + 1) % FANFARE_COUNT;
	}

	static void draw(int x0, int y0, int x1, int y1, int value, long spawnTime) {
		if (x0 < 0 || y0 < 0 || x1 < 0 || y1 < 0) {
			return;
		}
		int h = y0 - y1;
		if (h < 10) {
			h = 10;
			y1 = y0 - h;
		}
		int tier = tierOf(value);
		int primary = TIER_PRIMARY[tier];
		int secondary = TIER_SECONDARY[tier];
		long age = System.currentTimeMillis() - spawnTime;
		if (fanfare != FANFARE_OFF && age >= 0 && age < FANFARE_MS) {
			drawFanfare(x0, y0, x1, y1, primary, secondary, (int) age);
			if (age < 280) {
				return;
			}
		}
		int fade = 256;
		if (age >= 0 && age < FANFARE_MS) {
			fade = (int) ((age - 280) * 256 / (FANFARE_MS - 280));
			if (fade < 0) {
				fade = 0;
			}
			if (fade > 256) {
				fade = 256;
			}
		}
		int pulse = 220 + isin(client.loopCycle * 6) / 8;
		fade = fade * pulse / 256;
		drawStyle(x0, y0, x1, y1, primary, secondary, fade);
	}

	private static int tierOf(int value) {
		int tier = 0;
		for (int i = 0; i < TIER_VALUE.length; i++) {
			if (value >= TIER_VALUE[i]) {
				tier = i;
			}
		}
		return tier;
	}

	private static void drawStyle(int x0, int y0, int x1, int y1, int primary, int secondary, int fade) {
		switch (clamp(style, 0, STYLE_COUNT - 1)) {
		case STYLE_MODERN:
			drawModern(x0, y0, x1, y1, primary, secondary, fade);
			return;
		case STYLE_FIRE:
			drawFire(x0, y0, x1, y1, primary, secondary, fade);
			return;
		case STYLE_ELECTRIC:
			drawElectric(x0, y0, x1, y1, primary, secondary, fade);
			return;
		case STYLE_CLOUD:
			drawCloud(x0, y0, x1, y1, primary, fade);
			return;
		case STYLE_SMOKE:
			drawSmoke(x0, y0, x1, y1, primary, secondary, fade);
			return;
		case STYLE_MIASMA:
			drawMiasma(x0, y0, x1, y1, primary, secondary, fade);
			return;
		case STYLE_GLOW:
			drawGlow(x0, y0, x1, y1, primary, secondary, fade);
			return;
		default:
			drawLight(x0, y0, x1, y1, primary, fade);
		}
	}

	private static void drawLight(int x0, int y0, int x1, int y1, int color, int fade) {
		column(x0, y0, x1, y1, 13, 5, color, 36 * fade / 256);
		column(x0, y0, x1, y1, 7, 3, color, 90 * fade / 256);
		column(x0, y0, x1, y1, 2, 1, brighten(color), 170 * fade / 256);
	}

	private static void drawModern(int x0, int y0, int x1, int y1, int primary, int secondary, int fade) {
		column(x0, y0, x1, y1, 10, 4, primary, 40 * fade / 256);
		column(x0, y0, x1, y1, 4, 1, brighten(primary), 150 * fade / 256);
		helix(x0, y0, x1, y1, 8, 3, secondary, 140 * fade / 256, 3, 5.2, 0);
		helix(x0, y0, x1, y1, 8, 3, secondary, 90 * fade / 256, 3, 5.2, 2.1);
	}

	private static void drawFire(int x0, int y0, int x1, int y1, int primary, int secondary, int fade) {
		int tongues = 5;
		int i;
		for (i = 0; i < tongues; i++) {
			int phase = client.loopCycle * 10 + i * 40;
			int sway = isin(phase) * 7 / 2048;
			int shorten = 8 + (isin(phase * 3) + 2048) * 10 / 4096;
			int tx = x1 + sway;
			int ty = y1 + shorten;
			int bx = x0 + (i - tongues / 2) * 3;
			column(bx, y0, tx, ty, 6, 1, i % 2 == 0 ? primary : secondary, (70 - i * 8) * fade / 256);
		}
		column(x0, y0, x1, y1 + 12, 4, 1, brighten(primary), 80 * fade / 256);
	}

	private static void drawElectric(int x0, int y0, int x1, int y1, int primary, int secondary, int fade) {
		int flicker = (client.loopCycle / 2) % 5;
		int a = (flicker == 0 ? 80 : 160) * fade / 256;
		bolt(x0, y0, x1, y1, primary, a, 7, client.loopCycle);
		bolt(x0 - 4, y0, x1 + 3, y1 + 8, secondary, a * 2 / 3, 5, client.loopCycle + 17);
		bolt(x0 + 5, y0, x1 - 2, y1 + 14, secondary, a / 2, 4, client.loopCycle + 31);
		glowDot(x0, y0, 6, brighten(primary), 70 * fade / 256);
	}

	private static void drawCloud(int x0, int y0, int x1, int y1, int color, int fade) {
		int midY = y0 - (y0 - y1) / 5;
		int t = client.loopCycle;
		int i;
		for (i = 0; i < 7; i++) {
			int drift = isin(t * 4 + i * 90) * 10 / 2048;
			int bob = icos(t * 3 + i * 70) * 4 / 2048;
			int cx = x0 + (i - 3) * 5 + drift;
			int cy = midY + bob + (i % 3) * 3;
			blob(cx, cy, 7 + i % 3, color, 50 * fade / 256);
		}
	}

	private static void drawSmoke(int x0, int y0, int x1, int y1, int primary, int secondary, int fade) {
		int h = y0 - y1;
		if (h < 1) {
			h = 1;
		}
		int i;
		for (i = 0; i < 18; i++) {
			int seed = hash(x0, y0, i);
			int life = (client.loopCycle * 3 + (seed & 255)) % 40;
			int py = y0 - life * h / 40;
			int spread = life / 3;
			int px = x0 + ((seed >> 8) % (spread * 2 + 1)) - spread;
			int col = (i & 1) == 0 ? primary : secondary;
			int a = (70 - life) * fade / 256;
			if (a > 0) {
				blob(px, py, 3 + life / 10, col, a);
			}
		}
	}

	private static void drawMiasma(int x0, int y0, int x1, int y1, int primary, int secondary, int fade) {
		helix(x0, y0, x1, y1, 11, 4, primary, 110 * fade / 256, 2, 3.4, 0);
		helix(x0, y0, x1, y1, 11, 4, secondary, 90 * fade / 256, 2, 3.4, 3.14);
		column(x0, y0, x1, y1, 5, 2, primary, 40 * fade / 256);
	}

	private static void drawGlow(int x0, int y0, int x1, int y1, int primary, int secondary, int fade) {
		column(x0, y0, x1, y1, 16, 7, primary, 45 * fade / 256);
		column(x0, y0, x1, y1, 8, 3, primary, 90 * fade / 256);
		column(x0, y0, x1, y1, 3, 1, brighten(secondary), 160 * fade / 256);
		glowDot(x1, y1, 8, brighten(secondary), 90 * fade / 256);
	}

	private static void drawFanfare(int x0, int y0, int x1, int y1, int primary, int secondary, int age) {
		switch (fanfare) {
		case FANFARE_COLUMN:
			fanfareColumn(x0, y0, x1, y1, primary, age);
			return;
		case FANFARE_TROPHY:
			fanfareTrophy(x0, y0, x1, y1, primary, secondary, age);
			return;
		case FANFARE_FIRE:
			fanfareFire(x0, y0, primary, secondary, age);
			return;
		case FANFARE_BOLT:
			fanfareBolt(x0, y0, x1, y1, primary, age);
			return;
		default:
			fanfareLightning(x0, y0, x1, y1, primary, secondary, age);
		}
	}

	private static void fanfareLightning(int x0, int y0, int x1, int y1, int primary, int secondary, int age) {
		if (age < 220) {
			int flash = 180 - age * 180 / 220;
			glowDot(x0, y0, 18, brighten(secondary), flash);
			glowDot(x0, y0, 10, 0xFFFFFF, flash);
		}
		if (age > 80) {
			int a = age < 500 ? 200 : 200 - (age - 500) * 200 / 600;
			if (a > 0) {
				bolt(x0, y0, x1, y1 - 20, primary, a, 8, age / 30);
				bolt(x0 + 6, y0, x1 - 4, y1, brighten(primary), a * 2 / 3, 6, age / 30 + 9);
			}
		}
	}

	private static void fanfareColumn(int x0, int y0, int x1, int y1, int primary, int age) {
		int rise = age < 550 ? age : 550 - (age - 550);
		if (rise < 0) {
			rise = 0;
		}
		int ty = y0 - (y0 - y1) * rise / 550;
		column(x0, y0, x0, ty, 12, 8, primary, 90);
		column(x0, y0, x0, ty, 4, 3, brighten(primary), 140);
	}

	private static void fanfareTrophy(int x0, int y0, int x1, int y1, int primary, int secondary, int age) {
		int r = 6 + age / 18;
		ring(x0, y0 - age / 8, r, primary, 120);
		ring(x0, y0 - age / 6, r + 6, secondary, 80);
		column(x0, y0, x1, y1 + 20, 5, 2, primary, 70);
		int i;
		for (i = 0; i < 6; i++) {
			int ang = client.loopCycle * 8 + i * 341;
			int lx = x0 + isin(ang) * (r + 4) / 2048;
			int ly = y0 - age / 8 + icos(ang) * 5 / 2048;
			blend(lx, ly, secondary, 160);
		}
	}

	private static void fanfareFire(int x0, int y0, int primary, int secondary, int age) {
		int r = 4 + age / 12;
		int a = age < 400 ? 160 : 160 - (age - 400) * 160 / 700;
		if (a < 0) {
			a = 0;
		}
		glowDot(x0, y0 - age / 10, r, primary, a);
		glowDot(x0, y0 - age / 8, r / 2, secondary, a);
		int i;
		for (i = 0; i < 8; i++) {
			int ang = i * 256 + age;
			int d = r + 4;
			blob(x0 + isin(ang) * d / 2048, y0 - age / 10 + icos(ang) * d / 4096, 3, i % 2 == 0 ? primary : secondary, a / 2);
		}
	}

	private static void fanfareBolt(int x0, int y0, int x1, int y1, int primary, int age) {
		int a = age < 500 ? 200 : 200 - (age - 500) * 200 / 600;
		if (a <= 0) {
			return;
		}
		bolt(x0, y0, x1, y1 - 30, primary, a, 6, 4);
		bolt(x0 - 8, y0, x1, y1 - 10, primary, a * 2 / 3, 5, 11);
		bolt(x0 + 8, y0, x1, y1 - 10, primary, a * 2 / 3, 5, 19);
	}

	private static void column(int x0, int y0, int x1, int y1, int w0, int w1, int rgb, int alpha) {
		if (alpha <= 0) {
			return;
		}
		int steps = y0 - y1;
		if (steps < 0) {
			steps = -steps;
		}
		if (steps < 16) {
			steps = 16;
		}
		if (steps > 90) {
			steps = 90;
		}
		int i;
		int dx;
		for (i = 0; i <= steps; i++) {
			int x = x0 + (x1 - x0) * i / steps;
			int y = y0 + (y1 - y0) * i / steps;
			int w = w0 + (w1 - w0) * i / steps;
			if (w < 1) {
				w = 1;
			}
			for (dx = -w; dx <= w; dx++) {
				int dist = dx < 0 ? -dx : dx;
				int a = alpha * (w - dist + 1) / (w + 1);
				if (a > 0) {
					blend(x + dx, y, rgb, a);
				}
			}
		}
	}

	private static void helix(int x0, int y0, int x1, int y1, int w0, int w1, int rgb, int alpha, int strands, double turns, double offset) {
		int steps = 36;
		int s;
		int i;
		for (s = 0; s < strands; s++) {
			double base = offset + s * (Math.PI * 2.0 / strands) + client.loopCycle * 0.09;
			int px = 0;
			int py = 0;
			boolean first = true;
			for (i = 0; i <= steps; i++) {
				double t = i / (double) steps;
				int w = w0 + (int) ((w1 - w0) * t);
				double ang = base + t * turns * Math.PI * 2.0;
				int x = x0 + (x1 - x0) * i / steps + (int) (Math.cos(ang) * w);
				int y = y0 + (y1 - y0) * i / steps;
				if (!first) {
					line(px, py, x, y, rgb, alpha, 1);
				}
				first = false;
				px = x;
				py = y;
			}
		}
	}

	private static void bolt(int x0, int y0, int x1, int y1, int rgb, int alpha, int segs, int seed) {
		int px = x0;
		int py = y0;
		int i;
		for (i = 1; i <= segs; i++) {
			int x = x0 + (x1 - x0) * i / segs;
			int y = y0 + (y1 - y0) * i / segs;
			int jag = ((hash(seed, i, client.loopCycle / 2) >> 8) % 11) - 5;
			if (i != segs) {
				x += jag;
			}
			line(px, py, x, y, rgb, alpha, 1);
			line(px + 1, py, x + 1, y, rgb, alpha / 2, 1);
			px = x;
			py = y;
		}
	}

	private static void blob(int cx, int cy, int r, int rgb, int alpha) {
		int y;
		int x;
		int rr = r * r;
		for (y = -r; y <= r; y++) {
			for (x = -r; x <= r; x++) {
				int d2 = x * x + y * y;
				if (d2 <= rr) {
					int a = alpha * (rr - d2) / rr;
					if (a > 0) {
						blend(cx + x, cy + y, rgb, a);
					}
				}
			}
		}
	}

	private static void glowDot(int cx, int cy, int r, int rgb, int alpha) {
		blob(cx, cy, r, rgb, alpha);
	}

	private static void ring(int cx, int cy, int r, int rgb, int alpha) {
		if (r < 2) {
			return;
		}
		int i;
		int lastX = cx + r;
		int lastY = cy;
		for (i = 1; i <= 24; i++) {
			int ang = i * 2048 / 24;
			int x = cx + isin(ang) * r / 2048;
			int y = cy + icos(ang) * r / 4096;
			line(lastX, lastY, x, y, rgb, alpha, 1);
			lastX = x;
			lastY = y;
		}
	}

	private static void line(int x0, int y0, int x1, int y1, int rgb, int alpha, int thick) {
		int dx = x1 - x0;
		int dy = y1 - y0;
		if (dx < 0) {
			dx = -dx;
		}
		if (dy < 0) {
			dy = -dy;
		}
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;
		int err = dx - dy;
		int x = x0;
		int y = y0;
		while (true) {
			blend(x, y, rgb, alpha);
			if (thick > 0) {
				blend(x + 1, y, rgb, alpha / 2);
				blend(x, y + 1, rgb, alpha / 2);
			}
			if (x == x1 && y == y1) {
				break;
			}
			int e2 = err << 1;
			if (e2 > -dy) {
				err -= dy;
				x += sx;
			}
			if (e2 < dx) {
				err += dx;
				y += sy;
			}
		}
	}

	private static void blend(int x, int y, int rgb, int alpha) {
		if (alpha <= 0) {
			return;
		}
		if (alpha > 256) {
			alpha = 256;
		}
		if (x < DrawingArea.topX || x >= DrawingArea.bottomX || y < DrawingArea.topY || y >= DrawingArea.bottomY) {
			return;
		}
		int[] pixels = DrawingArea.pixels;
		if (pixels == null) {
			return;
		}
		int i = y * DrawingArea.width + x;
		if (i < 0 || i >= pixels.length) {
			return;
		}
		int dst = pixels[i];
		int ia = 256 - alpha;
		int r = ((rgb >> 16 & 0xff) * alpha + (dst >> 16 & 0xff) * ia) >> 8;
		int g = ((rgb >> 8 & 0xff) * alpha + (dst >> 8 & 0xff) * ia) >> 8;
		int b = ((rgb & 0xff) * alpha + (dst & 0xff) * ia) >> 8;
		pixels[i] = r << 16 | g << 8 | b;
	}

	private static int brighten(int rgb) {
		int r = rgb >> 16 & 0xff;
		int g = rgb >> 8 & 0xff;
		int b = rgb & 0xff;
		r += (255 - r) / 2;
		g += (255 - g) / 2;
		b += (255 - b) / 2;
		return r << 16 | g << 8 | b;
	}

	private static int isin(int angle) {
		return Model.modelIntArray1[angle & 2047];
	}

	private static int icos(int angle) {
		return Model.modelIntArray2[angle & 2047];
	}

	private static int hash(int a, int b, int c) {
		int x = a * 374761393 + b * 668265263 + c * 1274126177;
		x = (x ^ (x >> 13)) * 1274126177;
		return x;
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

	private static int readInt(Properties props, String key, int fallback) {
		try {
			String value = props.getProperty(key);
			if (value != null) {
				return Integer.parseInt(value.trim());
			}
		} catch (Exception e) {
		}
		return fallback;
	}
}
