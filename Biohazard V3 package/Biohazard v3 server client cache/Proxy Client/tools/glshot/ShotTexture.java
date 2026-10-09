import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Measures TEXTURE-NESS: the mean absolute difference between horizontally adjacent pixels, and the
 * fraction of "edge" pixels. A flat-shaded field scores ~0; a textured surface scores high.
 *
 * This separates "the software ground is flat-shaded and GL's is textured" from "both are textured
 * and merely tinted differently", which the distinct-colour count alone cannot.
 */
public class ShotTexture {
	public static void main(String[] a) throws Exception {
		BufferedImage sw = ImageIO.read(new File("gldiff-software.png"));
		BufferedImage gl = ImageIO.read(new File("gldiff-gl.png"));
		int w = sw.getWidth();
		region("GROUND near", sw, gl, w, 150, 750, 500, 666);
		region("GROUND far/right", sw, gl, w, 560, 900, 400, 600);
		region("HORIZON / scenery", sw, gl, w, 150, 570, 190, 390);
	}

	static void region(String name, BufferedImage sw, BufferedImage gl, int w, int x0, int x1, int y0,
			int y1) {
		System.out.println("=== " + name + " ===");
		stat("software", sw, x0, x1, y0, y1);
		stat("GL      ", gl, x0, x1, y0, y1);
		System.out.println();
	}

	static void stat(String label, BufferedImage img, int x0, int x1, int y0, int y1) {
		long sum = 0;
		int edges = 0, n = 0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0 + 1; x < x1; x++) {
				int p = img.getRGB(x, y), q = img.getRGB(x - 1, y);
				int d = Math.abs(((p >> 16) & 255) - ((q >> 16) & 255))
						+ Math.abs(((p >> 8) & 255) - ((q >> 8) & 255))
						+ Math.abs((p & 255) - (q & 255));
				sum += d;
				if (d > 24) {
					edges++;
				}
				n++;
			}
		}
		System.out.printf(
				"  %s  mean |neighbour delta| = %5.1f   edge pixels (delta>24) = %5.1f%%   edges/1000px = %6.1f%n",
				label, (double) sum / n, 100.0 * edges / n, 1000.0 * edges / n);
	}
}
