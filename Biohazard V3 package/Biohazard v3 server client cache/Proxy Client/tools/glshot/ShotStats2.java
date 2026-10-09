import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Answers, for a region, whether an image is a genuine RENDER or a FLAT FILL.
 *
 * A render has thousands of distinct colours and a shading gradient; a fill has a handful of
 * colours with one dominating and no gradient. This is what separates "the software ground is
 * flat-coloured" from "the software ground was filled with one colour".
 */
public class ShotStats2 {
	public static void main(String[] args) throws Exception {
		BufferedImage sw = ImageIO.read(new File("gldiff-software.png"));
		BufferedImage gl = ImageIO.read(new File("gldiff-gl.png"));
		int w = sw.getWidth(), h = sw.getHeight();

		region("GROUND (near)", sw, gl, w, h, 150, 750, 500, 666);
		region("GROUND (far/right)", sw, gl, w, h, 560, 900, 400, 600);
		region("HORIZON / scenery", sw, gl, w, h, 150, 570, 190, 390);
		region("WHOLE FRAME", sw, gl, w, h, 0, w, 0, h);
	}

	static void region(String name, BufferedImage sw, BufferedImage gl, int w, int h, int x0,
			int x1, int y0, int y1) {
		System.out.println("=== " + name + "  (" + x0 + "," + y0 + ")-(" + x1 + "," + y1 + ") ===");
		stats("software", sw, x0, x1, y0, y1);
		stats("GL      ", gl, x0, x1, y0, y1);
		System.out.println("  per-band mean rgb (top of region -> bottom):");
		for (int b = 0; b < 6; b++) {
			int ya = y0 + (y1 - y0) * b / 6;
			int yb = y0 + (y1 - y0) * (b + 1) / 6;
			System.out.printf("    y %3d-%3d   software (%3d,%3d,%3d)   GL (%3d,%3d,%3d)%n", ya, yb,
					mean(sw, x0, x1, ya, yb, 16), mean(sw, x0, x1, ya, yb, 8),
					mean(sw, x0, x1, ya, yb, 0), mean(gl, x0, x1, ya, yb, 16),
					mean(gl, x0, x1, ya, yb, 8), mean(gl, x0, x1, ya, yb, 0));
		}
		System.out.println();
	}

	static void stats(String label, BufferedImage img, int x0, int x1, int y0, int y1) {
		Map<Integer, Integer> counts = new HashMap<>();
		int n = 0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				counts.merge(img.getRGB(x, y) & 0xFFFFFF, 1, Integer::sum);
				n++;
			}
		}
		int max = 0;
		int maxColour = 0;
		for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
			if (e.getValue() > max) {
				max = e.getValue();
				maxColour = e.getKey();
			}
		}
		System.out.printf("  %s  distinct colours %,d  (%.1f%% of pixels)   top %06x = %.1f%% of region%n",
				label, counts.size(), 100.0 * counts.size() / n, maxColour, 100.0 * max / n);
	}

	static int mean(BufferedImage img, int x0, int x1, int y0, int y1, int shift) {
		long sum = 0;
		int n = 0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				sum += (img.getRGB(x, y) >> shift) & 0xFF;
				n++;
			}
		}
		return n == 0 ? 0 : (int) (sum / n);
	}
}
