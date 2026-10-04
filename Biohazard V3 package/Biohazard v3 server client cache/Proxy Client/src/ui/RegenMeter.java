package ui;

import java.util.Properties;

/** HP and special-attack regeneration rings on the orbs. */
public final class RegenMeter {

	public static boolean enabled = true;

	private static int lastHp = -1;
	private static int lastSpec = -1;
	private static long hpStart;
	private static long specStart;

	public static void load(Properties props) {
		enabled = readBool(props, "regenMeter", true);
	}

	public static void save(Properties props) {
		props.setProperty("regenMeter", Boolean.toString(enabled));
	}

	public static void track(int hp, int maxHp, int spec) {
		long now = System.currentTimeMillis();
		if (hp != lastHp) {
			lastHp = hp;
			if (hp < maxHp) {
				hpStart = now;
			}
		}
		if (spec != lastSpec) {
			if (spec < lastSpec || lastSpec < 0) {
				specStart = now;
			}
			lastSpec = spec;
		}
	}

	public static void drawHp(int ox, int oy, int ow, int oh, int hp, int maxHp) {
		if (!enabled || hp >= maxHp || maxHp <= 0) {
			return;
		}
		float p = progress(hpStart, 60000L);
		drawRing(ox, oy, ow, oh, p, 0x00FF00);
	}

	public static void drawSpec(int ox, int oy, int ow, int oh, int spec) {
		if (!enabled || spec >= 100) {
			return;
		}
		float p = progress(specStart, 17400L);
		drawRing(ox, oy, ow, oh, p, 0xFF981F);
	}

	private static float progress(long start, long duration) {
		if (start <= 0L) {
			return 0f;
		}
		float p = (float) ((System.currentTimeMillis() - start) % duration) / (float) duration;
		if (p < 0f) {
			return 0f;
		}
		if (p > 1f) {
			return 1f;
		}
		return p;
	}

	private static void drawRing(int ox, int oy, int ow, int oh, float progress, int rgb) {
		if (ow <= 0) {
			ow = 57;
		}
		if (oh <= 0) {
			oh = 34;
		}
		int cx = ox + ow / 3;
		int cy = oy + oh / 2;
		int r = Math.min(ow, oh) / 3;
		if (r < 8) {
			r = 8;
		}
		int segs = 28;
		int shown = (int) (segs * progress);
		if (shown < 1) {
			shown = 1;
		}
		int lastX = cx;
		int lastY = cy - r;
		for (int i = 1; i <= shown; i++) {
			double a = Math.PI * 2.0D * i / segs - Math.PI / 2.0D;
			int x = cx + (int) Math.round(Math.cos(a) * r);
			int y = cy + (int) Math.round(Math.sin(a) * r);
			DrawingArea.drawLine(lastX, lastY, x, y, rgb);
			DrawingArea.drawLine(lastX + 1, lastY, x + 1, y, rgb);
			lastX = x;
			lastY = y;
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
