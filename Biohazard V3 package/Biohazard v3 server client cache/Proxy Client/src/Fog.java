final class Fog {

	static int sceneDepth;

	static int fogRgb() {
		int strength = client.fogStrength;
		if (strength <= 1) {
			return 0x6E7E8C;
		}
		if (strength == 2) {
			return 0x8494A2;
		}
		return 0x96A6B4;
	}

	static int factor(int z) {
		int strength = client.fogStrength;
		if (strength < 1 || z < 50) {
			return 0;
		}
		if (strength > 3) {
			strength = 3;
		}
		int start = 3400 - strength * 550;
		int range = 2200 - strength * 250;
		if (range < 700) {
			range = 700;
		}
		if (z <= start) {
			return 0;
		}
		int t = ((z - start) << 8) / range;
		int cap = 48 + strength * 28;
		if (t > cap) {
			t = cap;
		}
		return t;
	}

	static int fadeHsl(int hsl, int z) {
		if (hsl == 0xbc614e) {
			return hsl;
		}
		int t = factor(z);
		if (t <= 0) {
			return hsl;
		}
		int inv = 256 - t;
		int hue = hsl >> 10 & 63;
		int sat = hsl >> 7 & 7;
		int lum = hsl & 127;
		sat = (sat * inv) >> 8;
		lum = (lum * inv + 68 * t) >> 8;
		if (sat < 0) {
			sat = 0;
		} else if (sat > 7) {
			sat = 7;
		}
		if (lum < 2) {
			lum = 2;
		} else if (lum > 126) {
			lum = 126;
		}
		return (hue << 10) + (sat << 7) + lum;
	}

	static int fadeRgb(int rgb, int z) {
		int t = factor(z);
		if (t <= 0) {
			return rgb;
		}
		int fog = fogRgb();
		int inv = 256 - t;
		int r = ((rgb >> 16 & 0xff) * inv + (fog >> 16 & 0xff) * t) >> 8;
		int g = ((rgb >> 8 & 0xff) * inv + (fog >> 8 & 0xff) * t) >> 8;
		int b = ((rgb & 0xff) * inv + (fog & 0xff) * t) >> 8;
		return (r << 16) + (g << 8) + b;
	}

	static int applyFlat(int rgb) {
		if (sceneDepth <= 50 || client.fogStrength <= 0) {
			return rgb;
		}
		return fadeRgb(rgb, sceneDepth);
	}

	static void fillBackground() {
		int[] pixels = DrawingArea.pixels;
		if (pixels == null || client.fogStrength <= 0) {
			return;
		}
		int width = DrawingArea.width;
		int top = DrawingArea.topY;
		int bottom = DrawingArea.bottomY;
		int left = DrawingArea.topX;
		int right = DrawingArea.bottomX;
		if (width <= 0 || bottom <= top || right <= left) {
			return;
		}
		int color = fogRgb();
		if (left == 0 && right == width && top == 0 && bottom == DrawingArea.height) {
			java.util.Arrays.fill(pixels, 0, width * (bottom - top), color);
			return;
		}
		for (int y = top; y < bottom; y++) {
			int row = y * width + left;
			java.util.Arrays.fill(pixels, row, row + (right - left), color);
		}
	}

	static void antiAliasEdges(int[] pixels, int width, int height) {
		int strength = client.aaStrength;
		if (pixels == null || width < 3 || height < 3 || strength < 2) {
			return;
		}
		if (!client.isFixed() && strength < 3) {
			return;
		}
		int threshold = strength >= 3 ? 160 : 260;
		int selfWeight = strength >= 3 ? 2 : 5;
		int denom = selfWeight + 4;
		if (aaScratch == null || aaScratch.length < pixels.length) {
			aaScratch = new int[pixels.length];
		}
		System.arraycopy(pixels, 0, aaScratch, 0, pixels.length);
		int[] copy = aaScratch;
		int yStep = !client.isFixed() && width * height > 700000 ? 2 : 1;
		for (int y = 1; y < height - 1; y += yStep) {
			int row = y * width;
			for (int x = 1; x < width - 1; x++) {
				int i = row + x;
				int c = copy[i];
				int l = copy[i - 1];
				int r = copy[i + 1];
				int u = copy[i - width];
				int d = copy[i + width];
				int contrast = diff(c, l) + diff(c, r) + diff(c, u) + diff(c, d);
				if (contrast > threshold) {
					int rr = ((c >> 16 & 0xff) * selfWeight + (l >> 16 & 0xff) + (r >> 16 & 0xff) + (u >> 16 & 0xff) + (d >> 16 & 0xff)) / denom;
					int gg = ((c >> 8 & 0xff) * selfWeight + (l >> 8 & 0xff) + (r >> 8 & 0xff) + (u >> 8 & 0xff) + (d >> 8 & 0xff)) / denom;
					int bb = ((c & 0xff) * selfWeight + (l & 0xff) + (r & 0xff) + (u & 0xff) + (d & 0xff)) / denom;
					pixels[i] = (rr << 16) + (gg << 8) + bb;
				}
			}
		}
	}

	private static int diff(int a, int b) {
		int dr = (a >> 16 & 0xff) - (b >> 16 & 0xff);
		int dg = (a >> 8 & 0xff) - (b >> 8 & 0xff);
		int db = (a & 0xff) - (b & 0xff);
		if (dr < 0) {
			dr = -dr;
		}
		if (dg < 0) {
			dg = -dg;
		}
		if (db < 0) {
			db = -db;
		}
		return dr + dg + db;
	}

	private static int[] aaScratch;
}
