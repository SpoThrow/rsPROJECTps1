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
		if (pixels == null || width < 3 || height < 3 || strength < 1) {
			return;
		}
		int contrastMin = strength >= 3 ? 180 : (strength >= 2 ? 260 : 340);
		int maxBlend = strength >= 3 ? 96 : (strength >= 2 ? 64 : 40);
		if (aaScratch == null || aaScratch.length < pixels.length) {
			aaScratch = new int[pixels.length];
		}
		System.arraycopy(pixels, 0, aaScratch, 0, pixels.length);
		int[] src = aaScratch;
		int stride = width;
		for (int y = 1; y < height - 1; y++) {
			int row = y * stride;
			for (int x = 1; x < width - 1; x++) {
				int i = row + x;
				int c = src[i];
				int left = src[i - 1];
				int right = src[i + 1];
				int up = src[i - stride];
				int down = src[i + stride];
				if ((c | left | right | up | down) == 0) {
					continue;
				}
				int lc = luma(c);
				int lL = luma(left);
				int lR = luma(right);
				int lU = luma(up);
				int lD = luma(down);
				int lMin = lc;
				if (lL < lMin) {
					lMin = lL;
				}
				if (lR < lMin) {
					lMin = lR;
				}
				if (lU < lMin) {
					lMin = lU;
				}
				if (lD < lMin) {
					lMin = lD;
				}
				int lMax = lc;
				if (lL > lMax) {
					lMax = lL;
				}
				if (lR > lMax) {
					lMax = lR;
				}
				if (lU > lMax) {
					lMax = lU;
				}
				if (lD > lMax) {
					lMax = lD;
				}
				int range = lMax - lMin;
				if (range < contrastMin) {
					continue;
				}
				int horz = lL - lR;
				if (horz < 0) {
					horz = -horz;
				}
				int vert = lU - lD;
				if (vert < 0) {
					vert = -vert;
				}
				int n;
				int nLuma;
				if (horz >= vert) {
					if (lL >= lR) {
						n = left;
						nLuma = lL;
					} else {
						n = right;
						nLuma = lR;
					}
				} else {
					if (lU >= lD) {
						n = up;
						nLuma = lU;
					} else {
						n = down;
						nLuma = lD;
					}
				}
				if (lc + 24 >= nLuma) {
					continue;
				}
				int blend = (range * maxBlend) / 900;
				if (blend > maxBlend) {
					blend = maxBlend;
				}
				if (blend < 12) {
					continue;
				}
				pixels[i] = mixWeight(c, n, 256 - blend, blend);
			}
		}
	}

	private static int luma(int c) {
		return ((c >> 16 & 0xff) * 3) + ((c >> 8 & 0xff) * 6) + (c & 0xff);
	}

	private static int mixWeight(int a, int b, int wa, int wb) {
		return (((a & 0xff00ff) * wa + (b & 0xff00ff) * wb) >> 8 & 0xff00ff)
				+ (((a & 0xff00) * wa + (b & 0xff00) * wb) >> 8 & 0xff00);
	}

	private static int[] aaScratch;
}
