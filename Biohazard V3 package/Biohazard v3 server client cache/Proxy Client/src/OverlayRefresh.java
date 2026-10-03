/**
 * Cached HUD overlay strings so XP/FPS/memory text is not rebuilt every present.
 */
final class OverlayRefresh {

	static final int XP_MS = 250;
	static final int PERF_MS = 200;

	static long xpAt;
	static final String[] xpLines = new String[32];
	static int xpCount;
	static int xpW;
	static int xpH;

	static long perfAt;
	static final String[] perfLines = new String[5];
	static final int[] perfCols = new int[5];
	static int perfCount;
	static int perfW;

	static boolean stale(long lastMs, int intervalMs) {
		return lastMs <= 0L || System.currentTimeMillis() - lastMs >= intervalMs;
	}

	static void invalidateXp() {
		xpAt = 0L;
		xpCount = 0;
	}
}
