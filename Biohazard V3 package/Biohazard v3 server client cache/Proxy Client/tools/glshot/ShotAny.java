import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/** General-purpose analysis of any single client frame. */
public class ShotAny {
	public static void main(String[] args) throws Exception {
		BufferedImage img = ImageIO.read(new File(args[0]));
		int w = img.getWidth(), h = img.getHeight();
		System.out.println("image " + w + "x" + h);

		Map<Integer, Integer> counts = new HashMap<>();
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				counts.merge(img.getRGB(x, y) & 0xFFFFFF, 1, Integer::sum);
			}
		}
		System.out.println("distinct colours in whole frame: " + counts.size());

		// Ground band: sample a grid and report the colours, so the hue spread is visible.
		System.out.println();
		System.out.println("GROUND BAND SAMPLE (x,y) at 60px steps, y from 430 to 620:");
		for (int y = 430; y < 620; y += 60) {
			StringBuilder sb = new StringBuilder("  y=" + y + " ");
			for (int x = 120; x < 860; x += 60) {
				int c = img.getRGB(x, y) & 0xFFFFFF;
				int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
				int mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
				char kind = mx - mn <= 20 ? (mx < 45 ? '.' : 'g') : (g >= r - 25 && g > b + 20 ? 'o'
						: 'c');
				sb.append(String.format("%06x%c ", c, kind));
			}
			System.out.println(sb);
		}

		// Flat-tile detector: run lengths of identical pixels along rows in the ground band.
		System.out.println();
		int[] runs = new int[600];
		long runTotal = 0;
		int runCount = 0;
		for (int y = 430; y < 620; y += 5) {
			int start = 0;
			for (int x = 1; x <= w; x++) {
				if (x == w || (img.getRGB(x, y) & 0xFFFFFF) != (img.getRGB(x - 1, y) & 0xFFFFFF)) {
					int len = x - start;
					if (len < runs.length) {
						runs[len]++;
					}
					runTotal += len;
					runCount++;
					start = x;
				}
			}
		}
		System.out.println("horizontal flat runs in the ground band (mean length "
				+ String.format("%.1f", (double) runTotal / runCount) + "):");
		for (int len = 4; len < 200; len *= 2) {
			int total = 0;
			for (int k = len; k < Math.min(runs.length, len * 2); k++) {
				total += runs[k];
			}
			System.out.println("  runs of length " + len + "-" + (len * 2 - 1) + ": " + total);
		}
	}
}
