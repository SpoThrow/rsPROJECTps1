import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Wide-range alignment search over the two dumped frames.
 *
 * The live tool's search is bounded to offsets in {-8,-2,-1,0,1,2,8} px, on the assumption that the
 * only shift plausible is a readback stride/flip error. But a CAMERA or VIEWPOINT difference between
 * the two renders would show as a shift of tens of pixels, and that range was never searched - so
 * "no alignment found" so far does NOT rule out a positional mismatch. This searches +/-140 px.
 */
public class ShotAlign {
	public static void main(String[] args) throws Exception {
		BufferedImage sw = ImageIO.read(new File("gldiff-software.png"));
		BufferedImage gl = ImageIO.read(new File("gldiff-gl.png"));
		int w = sw.getWidth(), h = gl.getHeight();
		int[] s = sw.getRGB(0, 0, w, h, null, 0, w);
		int[] g = gl.getRGB(0, 0, w, h, null, 0, w);

		int bestDx = 0, bestDy = 0;
		double bestScore = -1;
		System.out.println("searching dx,dy in +/-140 step 4 (score = % of sampled pixels within delta 12)");
		for (int dy = -140; dy <= 140; dy += 4) {
			for (int dx = -140; dx <= 140; dx += 4) {
				double score = score(s, g, w, h, dx, dy);
				if (score > bestScore) {
					bestScore = score;
					bestDx = dx;
					bestDy = dy;
				}
			}
		}
		System.out.printf("  BEST offset (%+d,%+d) score %.2f%%%n", bestDx, bestDy, bestScore * 100);
		System.out.printf("  offset (0,0) score        %.2f%%%n", score(s, g, w, h, 0, 0) * 100);

		// Refine around the best, step 1.
		double fine = -1;
		int fdx = bestDx, fdy = bestDy;
		for (int dy = bestDy - 3; dy <= bestDy + 3; dy++) {
			for (int dx = bestDx - 3; dx <= bestDx + 3; dx++) {
				double score = score(s, g, w, h, dx, dy);
				if (score > fine) {
					fine = score;
					fdx = dx;
					fdy = dy;
				}
			}
		}
		System.out.printf("  refined BEST offset (%+d,%+d) score %.2f%%%n", fdx, fdy, fine * 100);

		// And with a looser tolerance, since two rasterisers never agree exactly.
		System.out.println();
		System.out.println("same search with a loose tolerance (delta 45) - do the SHAPES line up even if colours do not?");
		double loose = -1;
		int ldx = 0, ldy = 0;
		for (int dy = -140; dy <= 140; dy += 4) {
			for (int dx = -140; dx <= 140; dx += 4) {
				double score = score(s, g, w, h, dx, dy, 45);
				if (score > loose) {
					loose = score;
					ldx = dx;
					ldy = dy;
				}
			}
		}
		System.out.printf("  BEST offset (%+d,%+d) score %.2f%%   (at (0,0): %.2f%%)%n", ldx, ldy,
				loose * 100, score(s, g, w, h, 0, 0, 45) * 100);
	}

	static double score(int[] s, int[] g, int w, int h, int dx, int dy) {
		return score(s, g, w, h, dx, dy, 12);
	}

	static double score(int[] s, int[] g, int w, int h, int dx, int dy, int tol) {
		int match = 0, n = 0;
		for (int y = 160; y < h; y += 4) {
			int sy = y, gy = y + dy;
			if (gy < 0 || gy >= h) {
				continue;
			}
			for (int x = 0; x < w; x += 4) {
				int gx = x + dx;
				if (gx < 0 || gx >= w) {
					continue;
				}
				int a = s[sy * w + x], b = g[gy * w + gx];
				int d = Math.abs(((a >> 16) & 255) - ((b >> 16) & 255))
						+ Math.abs(((a >> 8) & 255) - ((b >> 8) & 255))
						+ Math.abs((a & 255) - (b & 255));
				if (d <= tol) {
					match++;
				}
				n++;
			}
		}
		return n == 0 ? 0 : (double) match / n;
	}
}
