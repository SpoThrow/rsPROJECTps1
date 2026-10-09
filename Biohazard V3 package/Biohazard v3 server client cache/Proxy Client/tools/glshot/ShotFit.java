import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Asks whether the GL image is a LINEAR TRANSFORM of the software image, per channel:
 *
 *     gl = a * sw + b
 *
 * This is the question the mean signed delta could not answer. A value of a near 1 with b near 0
 * means identical. a < 1 with b > 0 means a BLEND toward a constant (fog, an ambient add, a wash).
 * a > 1 with b < 0 means a contrast stretch. A low R^2 means GL is NOT a transform of the software
 * at all - i.e. different content, and no amount of shading arithmetic will reconcile them.
 */
public class ShotFit {
	public static void main(String[] args) throws Exception {
		BufferedImage sw = ImageIO.read(new File("gldiff-software.png"));
		BufferedImage gl = ImageIO.read(new File("gldiff-gl.png"));
		int w = sw.getWidth();
		region("WHOLE FRAME", sw, gl, w, 0, w, 0, 666);
		region("GROUND near", sw, gl, w, 150, 750, 500, 666);
		region("HORIZON / scenery", sw, gl, w, 150, 570, 190, 390);
	}

	static void region(String name, BufferedImage sw, BufferedImage gl, int w, int x0, int x1, int y0,
			int y1) {
		System.out.println("=== " + name + " ===");
		for (int shift : new int[] { 16, 8, 0 }) {
			fit(shift == 16 ? "RED  " : shift == 8 ? "GREEN" : "BLUE ", sw, gl, x0, x1, y0, y1,
					shift);
		}
		// How well does GL match software at all, ignoring transform?
		System.out.println();
	}

	static void fit(String label, BufferedImage sw, BufferedImage gl, int x0, int x1, int y0, int y1,
			int shift) {
		double n = 0, sx = 0, sy = 0, sxx = 0, sxy = 0, syy = 0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				double s = (sw.getRGB(x, y) >> shift) & 0xFF;
				double g = (gl.getRGB(x, y) >> shift) & 0xFF;
				n++;
				sx += s;
				sy += g;
				sxx += s * s;
				sxy += s * g;
				syy += g * g;
			}
		}
		double cov = sxy / n - (sx / n) * (sy / n);
		double varS = sxx / n - (sx / n) * (sx / n);
		double varG = syy / n - (sy / n) * (sy / n);
		double a = varS == 0 ? 0 : cov / varS;
		double b = sy / n - a * (sx / n);
		double r2 = (varS == 0 || varG == 0) ? 0 : (cov * cov) / (varS * varG);
		System.out.printf(
				"  %s  gl = %+.3f * sw %+.1f    R^2 = %.3f   (sw mean %5.1f -> gl mean %5.1f)%n", label,
				a, b, r2, sx / n, sy / n);
	}
}
