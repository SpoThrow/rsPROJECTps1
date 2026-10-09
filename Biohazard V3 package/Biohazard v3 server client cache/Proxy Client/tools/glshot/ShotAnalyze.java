import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Reads the two PNGs GlFrameDiff writes and renders each as a coarse CLASS GRID, so the scene's
 * structure can be read in text rather than guessed at from a thumbnail.
 *
 * Classes:
 *   '.' blank (near-black, i.e. the un-drawn background)
 *   'g' grey   (max-min <= 18, mid brightness)
 *   'd' dark grey (same, but dark)
 *   'o' OLIVE  (R ~= G and both well above B - the software ground colour)
 *   'c' other/colourful
 */
public class ShotAnalyze {
	static final int COLS = 24;
	static final int ROWS = 18;

	public static void main(String[] args) throws Exception {
		BufferedImage sw = ImageIO.read(new File(args[0]));
		BufferedImage gl = ImageIO.read(new File(args[1]));
		int w = sw.getWidth();
		int h = sw.getHeight();
		System.out.println("software " + w + "x" + h + "   gl " + gl.getWidth() + "x" + gl.getHeight());
		System.out.println();

		int[] swPix = sw.getRGB(0, 0, w, h, null, 0, w);
		int[] glPix = gl.getRGB(0, 0, w, h, null, 0, w);

		classFractions("SOFTWARE", swPix, w, h);
		classFractions("GL      ", glPix, w, h);
		System.out.println();

		System.out.println("class grid  (left = SOFTWARE, right = GL)");
		StringBuilder hdr = new StringBuilder("        ");
		for (int c = 0; c < COLS; c++) {
			hdr.append((char) ('0' + (c % 10)));
		}
		hdr.append("   ");
		for (int c = 0; c < COLS; c++) {
			hdr.append((char) ('0' + (c % 10)));
		}
		System.out.println(hdr);
		for (int r = 0; r < ROWS; r++) {
			StringBuilder row = new StringBuilder();
			row.append(String.format("%4d-%3d ", r * h / ROWS, (r + 1) * h / ROWS));
			for (int c = 0; c < COLS; c++) {
				row.append(majority(swPix, w, h, c, r));
			}
			row.append("  |");
			for (int c = 0; c < COLS; c++) {
				row.append(majority(glPix, w, h, c, r));
			}
			System.out.println(row);
		}
		System.out.println();

		// The key confusion matrix for the ground: where does SOFTWARE's olive end up in GL?
		int swOlive = 0;
		int swOliveGlOlive = 0;
		int swOliveGlGrey = 0;
		int swOliveGlBlank = 0;
		int swGrey = 0;
		int swGreyGlOlive = 0;
		for (int i = 0; i < w * h; i++) {
			char a = classify(swPix[i]);
			char b = classify(glPix[i]);
			if (a == 'o') {
				swOlive++;
				if (b == 'o') {
					swOliveGlOlive++;
				} else if (b == 'g' || b == 'd') {
					swOliveGlGrey++;
				} else if (b == '.') {
					swOliveGlBlank++;
				}
			} else if (a == 'g' || a == 'd') {
				swGrey++;
				if (b == 'o') {
					swGreyGlOlive++;
				}
			}
		}
		System.out.println("WHERE THE SOFTWARE'S OLIVE GROUND GOES IN THE GL IMAGE:");
		System.out.println("  software olive pixels            : " + swOlive);
		System.out.println("  ... GL also olive                : " + swOliveGlOlive + " ("
				+ pct(swOliveGlOlive, swOlive) + ")");
		System.out.println("  ... GL grey instead              : " + swOliveGlGrey + " ("
				+ pct(swOliveGlGrey, swOlive) + ")");
		System.out.println("  ... GL blank instead             : " + swOliveGlBlank + " ("
				+ pct(swOliveGlBlank, swOlive) + ")");
		System.out.println("  software grey pixels             : " + swGrey + ", of which GL olive "
				+ pct(swGreyGlOlive, swGrey));
		System.out.println();

		// Mean colour of the lower-centre region (the ground) in each image.
		regionMean("lower-centre (ground)", swPix, glPix, w, h, w / 4, w * 3 / 4, h / 2, h);
		regionMean("upper-centre (sky)   ", swPix, glPix, w, h, w / 4, w * 3 / 4, 0, h / 4);
	}

	static void regionMean(String label, int[] sw, int[] gl, int w, int h, int x0, int x1, int y0,
			int y1) {
		long sr = 0, sg = 0, sb = 0, gr = 0, gg = 0, gb = 0;
		int n = 0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				int i = y * w + x;
				sr += (sw[i] >> 16) & 0xFF;
				sg += (sw[i] >> 8) & 0xFF;
				sb += sw[i] & 0xFF;
				gr += (gl[i] >> 16) & 0xFF;
				gg += (gl[i] >> 8) & 0xFF;
				gb += gl[i] & 0xFF;
				n++;
			}
		}
		System.out.printf("%s  software mean rgb (%d,%d,%d)   gl mean rgb (%d,%d,%d)%n", label,
				sr / n, sg / n, sb / n, gr / n, gg / n, gb / n);
	}

	static void classFractions(String label, int[] pix, int w, int h) {
		int blank = 0, grey = 0, dark = 0, olive = 0, col = 0;
		for (int i = 0; i < w * h; i++) {
			switch (classify(pix[i])) {
			case '.':
				blank++;
				break;
			case 'g':
				grey++;
				break;
			case 'd':
				dark++;
				break;
			case 'o':
				olive++;
				break;
			default:
				col++;
			}
		}
		int n = w * h;
		System.out.printf("%s  blank %s  grey %s  dark %s  OLIVE %s  other-colour %s%n", label,
				pct(blank, n), pct(grey, n), pct(dark, n), pct(olive, n), pct(col, n));
	}

	static char majority(int[] pix, int w, int h, int c, int r) {
		int[] count = new int[128];
		int x0 = c * w / COLS, x1 = (c + 1) * w / COLS;
		int y0 = r * h / ROWS, y1 = (r + 1) * h / ROWS;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				count[classify(pix[y * w + x])]++;
			}
		}
		char best = '.';
		for (char ch : new char[] { '.', 'd', 'g', 'o', 'c' }) {
			if (count[ch] > count[best]) {
				best = ch;
			}
		}
		// ⚠ Mark a MIXED cell, because a cell that is half olive and half grey is the single most
		// interesting thing this grid can show and a bare majority would hide it.
		int total = (x1 - x0) * (y1 - y0);
		if (count[best] < total * 0.62) {
			return best == '.' ? ',' : Character.toUpperCase(best);
		}
		return best;
	}

	static char classify(int rgb) {
		int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
		int mx = Math.max(r, Math.max(g, b));
		int mn = Math.min(r, Math.min(g, b));
		if (mx < 8) {
			return '.';
		}
		if (mx - mn <= 18) {
			return mx < 45 ? 'd' : 'g';
		}
		if (r >= g - 12 && g > b + 22) {
			return 'o';
		}
		return 'c';
	}

	static String pct(int a, int b) {
		if (b == 0) {
			return "n/a";
		}
		return String.format("%.1f%%", 100.0 * a / b);
	}
}
