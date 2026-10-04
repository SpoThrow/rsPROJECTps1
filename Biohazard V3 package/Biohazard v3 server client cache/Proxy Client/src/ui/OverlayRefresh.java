/**
 * Cached HUD overlay strings so XP/FPS/memory text is not rebuilt every present.
 */
package ui;

public final class OverlayRefresh {

	public static final int XP_MS = 250;
	public static final int PERF_MS = 200;

	public static long xpAt;
	public static final String[] xpLines = new String[32];
	public static int xpCount;
	public static int xpW;
	public static int xpH;

	public static long perfAt;
	public static final String[] perfLines = new String[5];
	public static final int[] perfCols = new int[5];
	public static int perfCount;
	public static int perfW;

	public static boolean stale(long lastMs, int intervalMs) {
		return lastMs <= 0L || System.currentTimeMillis() - lastMs >= intervalMs;
	}

	public static void invalidateXp() {
		xpAt = 0L;
		xpCount = 0;
	}
}
