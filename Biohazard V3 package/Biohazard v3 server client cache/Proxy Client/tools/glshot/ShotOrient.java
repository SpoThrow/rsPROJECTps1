import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Tests all eight axis-aligned ORIENTATIONS before concluding the images are unrelated.
 *
 * The live tool and ShotAlign only ever searched TRANSLATIONS, so a wrong flip (glBatcher.readInto
 * un-flips row by row, so the handling exists and could be inverted) or a transpose would have
 * looked exactly like "no relationship at all". This is the last mundane explanation standing.
 */
public class ShotOrient {
	static final String[] NAMES = { "normal", "flip X", "flip Y", "flip both (180)",
			"transpose", "transpose + flip X", "transpose + flip Y", "transpose + 180" };

	public static void main(String[] args) throws Exception {
		BufferedImage sw = ImageIO.read(new File("gldiff-software.png"));
		BufferedImage gl = ImageIO.read(new File("gldiff-gl.png"));
		int w = sw.getWidth(), h = sw.getHeight();
		int[] s = sw.getRGB(0, 0, w, h, null, 0, w);
		int[] g = gl.getRGB(0, 0, w, h, null, 0, w);

		System.out.println("orientation             match% (tol 12)   match% (tol 45)");
		for (int o = 0; o < 8; o++) {
			System.out.printf("%-22s %10.2f%% %16.2f%%%n", NAMES[o], sample(s, g, w, h, o, 12) * 100,
					sample(s, g, w, h, o, 45) * 100);
		}

		// Where does the software's whole-frame brightness actually sit? If GL were flipped, the
		// software's top rows would match GL's bottom rows - so print both images' top/bottom means.
		System.out.println();
		System.out.println("vertical brightness profile (mean luma per 1/8 of the frame)");
		for (int b = 0; b < 8; b++) {
			int y0 = h * b / 8, y1 = h * (b + 1) / 8;
			System.out.printf("  band %d  y %3d-%3d   software %5.1f    GL %5.1f%n", b, y0, y1,
					luma(s, w, 0, w, y0, y1), luma(g, w, 0, w, y0, y1));
		}
	}

	static double sample(int[] s, int[] g, int w, int h, int orient, int tol) {
		int match = 0, n = 0;
		for (int y = 160; y < h; y += 4) {
			for (int x = 0; x < w; x += 4) {
				int gx = x, gy = y;
				switch (orient) {
				case 1:
					gx = w - 1 - x;
					break;
				case 2:
					gy = h - 1 - y;
					break;
				case 3:
					gx = w - 1 - x;
					gy = h - 1 - y;
					break;
				case 4:
					gx = y;
					gy = x;
					break;
				case 5:
					gx = h - 1 - y;
					gy = x;
					break;
				case 6:
					gx = y;
					gy = w - 1 - x;
					break;
				default:
					gx = h - 1 - y;
					gy = w - 1 - x;
					break;
				}
				if (gx < 0 || gx >= w || gy < 0 || gy >= h) {
					continue;
				}
				int a = s[y * w + x], b = g[gy * w + gx];
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

	static double luma(int[] p, int w, int x0, int x1, int y0, int y1) {
		long sum = 0;
		int n = 0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				int v = p[y * w + x];
				sum += (((v >> 16) & 255) * 30 + ((v >> 8) & 255) * 59 + (v & 255) * 11) / 100;
				n++;
			}
		}
		return n == 0 ? 0 : (double) sum / n;
	}
}
