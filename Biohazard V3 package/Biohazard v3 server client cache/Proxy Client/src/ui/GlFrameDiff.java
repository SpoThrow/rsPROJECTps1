package ui;

/**
 * Compares the GL image against the SOFTWARE image for the same frame, and classifies
 * their disagreement by ITS SPATIAL SHAPE rather than by its size (Phase 7.5a).
 *
 * <p>⚠⚠ <b>WHY THIS EXISTS AT ALL: "representable" is not the same claim as "correct".</b>
 * Everything before this step measures whether the GL path can express a face at all -
 * {@code TextureRamps.reproducesExactlyAt}, the ramp-overflow policy, the skip counters.
 * None of it can see a frame that is <b>fully representable and still wrong</b>: the wrong
 * texture id resolved, the wrong atlas layer sampled, a shade block off by one. The live log
 * proves a frame was composited; it cannot prove the frame is the right picture. This class
 * is the missing half.
 *
 * <p>⚠⚠⚠ <b>AND THE ORACLE IS FREE, WHICH IS THE WHOLE REASON THIS IS CHEAP.</b> The
 * software scene is NOT skipped in the shadow stage - it is fully drawn into
 * {@code RSImageProducer}'s {@code int[]}, and the GL readback then OVERWRITES it in place
 * (see {@code GlSceneRenderer.readBack}). So at the moment before {@code readInto} runs,
 * that array holds the software's own answer for exactly this frame, computed by the
 * original 2006 code. One defensive copy taken at that instant is a per-pixel ground truth
 * that needs no fixture, no golden image and no second render pass.
 *
 * <p><b>How the disagreement is classified, and why the metric is shape and not count.</b>
 * A count of differing pixels cannot tell these two apart, and they need opposite responses:
 * <ul>
 *   <li><b>Sampling-class.</b> The software evaluates the texture mapping exactly every eight
 *       pixels and interpolates between ({@code method379}'s {@code j7 = (j4 - i) >> 3} with
 *       {@code i = j4} resetting at each anchor), so its coordinate differs from a plain
 *       per-pixel ratio by at most ~0.05 texel - enough to pick the neighbouring texel on a
 *       few percent of pixels. That disagreement is <b>scattered</b>: single pixels and thin
 *       edges, wherever a texel boundary happens to fall. Expected, benign, and it does NOT
 *       look like anything to a player.</li>
 *   <li><b>Asset/geometry-class.</b> A wrong texture id, a wrong atlas layer, a mis-built
 *       ramp, or a missing face disagrees over <b>AREA</b>: whole tiles and whole faces.</li>
 * </ul>
 * So the load-bearing statistic is <b>solid blocks</b> - an {@link #BLOCK}x{@link #BLOCK}
 * region in which EVERY pixel differs. Uniform scattered noise cannot produce one: at a
 * disagreement rate of 5%, the chance of a single 8x8 block being entirely different is
 * 0.05^64, i.e. never. A single solid block is therefore strong evidence of something
 * other than sampling, and the two block flavours separate a further pair of causes:
 * <b>solid + large deltas = the wrong asset</b>, while <b>solid + small deltas = the right
 * asset shaded systematically wrong</b> (which is exactly the shape the 7.4j fog fade had).
 *
 * <p>⚠⚠ <b>The delta magnitude is a SECONDARY signal and is deliberately not the primary
 * one.</b> "One texel off" on a high-frequency texture can be a huge RGB change, and a whole
 * face shaded one block too dark can be a tiny one - so magnitude alone would sort the two
 * classes backwards. Shape sorts them correctly; magnitude only splits the solid ones.
 *
 * <p>⚠ <b>ALPHA IS EXCLUDED FROM THE COLOUR DELTA, ON PURPOSE.</b> The question here is
 * whether the PICTURE agrees, and alpha is not part of it. An alpha-only disagreement is
 * counted separately ({@link #alphaOnly()}) because it is a real signal of its own - GL
 * writes {@code 1.0} for a textured fragment, so an alpha that differs says something about
 * how the shader was reached rather than about what it drew.
 *
 * <p><b>Pure by construction: no GL, no statics, no logging.</b> It takes two {@code int[]}
 * so the harness can drive it with synthetic images and pin the classification, which is the
 * part that has to be right - a comparator that always says "identical" would be worse than
 * none, and this project's standard is that the classifier itself is mutation-tested.
 */
public final class GlFrameDiff {

	/**
	 * The solid-block side length. ⚠ Eight because it is the software's own anchor period
	 * ({@code method379} divides every eighth pixel), so a block is exactly the smallest
	 * region over which the software's mapping is recomputed from scratch - a disagreement
	 * that fills one is a disagreement about the mapping itself rather than about where
	 * inside an anchor group a texel boundary fell.
	 */
	public static final int BLOCK = 8;

	/**
	 * The largest per-channel difference still called "small".
	 *
	 * <p>⚠ This splits the SOLID blocks into the two causes above; it does not separate
	 * sampling from assets, and it is not meant to. 20 of 255 is well above the few units a
	 * neighbouring texel of a similar colour costs and well below a different texture.
	 */
	public static final int NEAR_CHANNEL_DELTA = 20;

	private int[] snapshot;
	private int width;
	private int height;

	private int[] blockDiffering;
	private int[] blockFar;
	private int[] blockNear;
	private int blocksWide;
	private int blocksHigh;

	// ---- the last comparison's results
	private int compared;
	private int identical;
	private int differing;
	private int near;
	private int far;
	private int alphaOnly;
	private int solidBlocks;
	private int solidFar;
	private int solidNear;
	private int blocks;
	private int longestRun;
	private int maxDelta;

	/**
	 * Per-image signatures, so a disagreement can be attributed to WHAT the images contain
	 * rather than only to how much they differ (Phase 7.5a-2).
	 *
	 * <p>⚠⚠ <b>These exist because the first live run produced a result that cannot be
	 * interpreted without them.</b> It reported <b>99.7% of pixels differing and 9255 of 9576
	 * blocks entirely different</b> - which is not "the texture mapping is slightly off", and
	 * not really a plausible comparison between two renders of one scene at all. Two views of
	 * the same world agree over most of the frame. So before believing "the GL image is wrong"
	 * the tool has to rule out the mundane explanations, and each of these rules out one:
	 * <ul>
	 *   <li><b>{@code blank} / {@code mean}</b> - if one image is empty (a cleared buffer that
	 *       the scene never reached), or if the two means are a permutation of each other, the
	 *       fault is plumbing or a colour-order swap rather than mapping.</li>
	 *   <li><b>{@code sample}</b> - a handful of actual pixel values at the SAME coordinates,
	 *       because "software = 0x00AB1234, GL = 0x003412AB" identifies a channel swap at a
	 *       glance where a percentage never can.</li>
	 *   <li><b>{@code bestOffset}</b> - the disagreement re-measured at small integer shifts. A
	 *       readback that is flipped, one row out, or offset in x would look like a total
	 *       disagreement at zero offset and collapse to agreement at the right one.</li>
	 * </ul>
	 */
	private int softwareBlank;
	private int glBlank;
	private int softwareMeanRgb;
	private int glMeanRgb;
	private String samples;
	/** Mean SIGNED per-channel difference over differing pixels, GL minus software. */
	private long signedR;
	private long signedG;
	private long signedB;
	private int bestOffsetX;
	private int bestOffsetY;
	private int bestOffsetMatches;
	private int bestOffsetTotal;

	/**
	 * The offsets the alignment search tries, in x and y.
	 *
	 * <p>⚠ Small integers plus a couple of larger strides, because the faults worth excluding
	 * are a one-pixel/one-row slip and a block-level offset - not an arbitrary translation,
	 * which would have been obvious on screen.
	 */
	private static final int[] OFFSETS = { -8, -2, -1, 0, 1, 2, 8 };

	/** How much of the frame the alignment search looks at: every Nth pixel in x and y. */
	private static final int OFFSET_STEP = 4;

	/**
	 * Defensive copy of the SOFTWARE image, taken before the GL readback overwrites it.
	 *
	 * @return whether a snapshot was taken; {@code false} if the source is unusable, which the
	 *         caller should treat as "no comparison this frame" rather than as a failure
	 */
	public boolean capture(int[] pixels, int width, int height) {
		if (pixels == null || width <= 0 || height <= 0 || pixels.length < width * height) {
			return false;
		}
		if (snapshot == null || snapshot.length < width * height) {
			snapshot = new int[width * height];
		}
		this.width = width;
		this.height = height;
		System.arraycopy(pixels, 0, snapshot, 0, width * height);
		return true;
	}

	/** Whether {@link #capture} has produced a snapshot that a later compare can use. */
	public boolean hasSnapshot() {
		return snapshot != null && width > 0 && height > 0;
	}

	/**
	 * Compares the snapshot against {@code pixels} (the framebuffer AFTER the GL readback).
	 *
	 * <p>⚠ It reports rather than throws when the sizes disagree: a resize between the
	 * snapshot and the readback is a legitimate thing for a debug tool to survive, and the
	 * right answer is "no comparison", not a crash in the render loop.
	 *
	 * @return whether a comparison actually happened
	 */
	public boolean compare(int[] pixels, int width, int height) {
		reset(pixels, width, height);
		if (snapshot == null || pixels == null
				|| width != this.width || height != this.height
				|| pixels.length < width * height) {
			return false;
		}
		compared = width * height;
		int run = 0;
		for (int y = 0; y < height; y++) {
			run = 0;
			int rowStart = y * width;
			for (int x = 0; x < width; x++) {
				int i = rowStart + x;
				int sw = snapshot[i];
				int gl = pixels[i];
				if (sw == 0) {
					softwareBlank++;
				}
				if (gl == 0) {
					glBlank++;
				}
				softwareMeanRgb += ((sw >> 16) & 0xFF) + ((sw >> 8) & 0xFF) + (sw & 0xFF);
				glMeanRgb += ((gl >> 16) & 0xFF) + ((gl >> 8) & 0xFF) + (gl & 0xFF);
				if ((sw & 0xFFFFFF) == (gl & 0xFFFFFF)) {
					// ⚠ The picture agrees, so the run of disagreement ends here whether or not
					// alpha differed - see the class doc on why alpha is excluded.
					identical++;
					if (sw != gl) {
						alphaOnly++;
					}
					run = 0;
					continue;
				}
				differing++;
				run++;
				if (run > longestRun) {
					longestRun = run;
				}
				// ⚠⚠ THE SIGNED SUM, AND IT IS THE DISCRIMINATOR THE FIRST CONTENT LINE COULD NOT
				// PROVIDE. A systematic transform (a wrong shade block, an extra darkening, a
				// channel bias) makes this mean consistently NON-ZERO and signed; genuinely
				// different content averages out near zero because some pixels come out lighter
				// and others darker. The first live run's samples showed software drawing
				// saturated OLIVE where GL drew near-GRAY and vice versa, which no hue-preserving
				// darkening can produce - so this number is what confirms "content" over
				// "transform" without another live run.
				signedR += ((gl >> 16) & 0xFF) - ((sw >> 16) & 0xFF);
				signedG += ((gl >> 8) & 0xFF) - ((sw >> 8) & 0xFF);
				signedB += (gl & 0xFF) - (sw & 0xFF);
				int delta = channelDelta(sw, gl);
				if (delta > maxDelta) {
					maxDelta = delta;
				}
				if (delta <= NEAR_CHANNEL_DELTA) {
					near++;
					blockNear[blockIndex(x, y)]++;
				} else {
					far++;
					blockFar[blockIndex(x, y)]++;
				}
				blockDiffering[blockIndex(x, y)]++;
			}
		}
		classifyBlocks(width, height);
		sample(width, height, pixels);
		searchOffsets(width, height, pixels);
		return true;
	}

	/**
	 * Records a handful of actual pixel values from both images at the SAME coordinates.
	 *
	 * <p>⚠ Chosen as a 4x3 grid inset from the edges rather than the corners, because a corner
	 * is the one place a flip or an offset can coincide with the truth and look innocent.
	 */
	private void sample(int width, int height, int[] pixels) {
		StringBuilder sb = new StringBuilder();
		for (int gy = 1; gy <= 3; gy++) {
			for (int gx = 1; gx <= 4; gx++) {
				int x = Math.min(width - 1, width * gx / 5);
				int y = Math.min(height - 1, height * gy / 4);
				int i = y * width + x;
				if (gx > 1) {
					sb.append(' ');
				}
				sb.append("(").append(x).append(',').append(y).append(")s=")
						.append(hex(snapshot[i])).append(" g=").append(hex(pixels[i]));
			}
			sb.append("; ");
		}
		samples = sb.toString().trim();
	}

	/**
	 * Re-measures the disagreement at small integer shifts, to exclude a misaligned readback.
	 *
	 * <p>⚠⚠ <b>Why this is worth the pass: a total disagreement at zero offset and a near-total
	 * agreement at one small offset is a completely different diagnosis from a total
	 * disagreement at every offset</b> - the first is a plumbing fault in the readback (a
	 * flip, a stride, a row), the second is the picture itself. Worth knowing before rewriting
	 * any mapping code.
	 *
	 * <p>⚠ Run on a subsample, because this is 49 passes over the frame and it is a diagnostic
	 * rather than part of the render.
	 */
	private void searchOffsets(int width, int height, int[] pixels) {
		int best = -1;
		for (int oi = 0; oi < OFFSETS.length; oi++) {
			int dy = OFFSETS[oi];
			for (int oj = 0; oj < OFFSETS.length; oj++) {
				int dx = OFFSETS[oj];
				int matched = 0;
				int total = 0;
				for (int y = 0; y < height; y += OFFSET_STEP) {
					int sy = y + dy;
					if (sy < 0 || sy >= height) {
						continue;
					}
					for (int x = 0; x < width; x += OFFSET_STEP) {
						int sx = x + dx;
						if (sx < 0 || sx >= width) {
							continue;
						}
						total++;
						if ((snapshot[sy * width + sx] & 0xFFFFFF)
								== (pixels[y * width + x] & 0xFFFFFF)) {
							matched++;
						}
					}
				}
				if (total > 0 && matched > best) {
					best = matched;
					bestOffsetX = dx;
					bestOffsetY = dy;
					bestOffsetMatches = matched;
					bestOffsetTotal = total;
				}
			}
		}
	}

	private static String hex(int argb) {
		String s = Integer.toHexString(argb);
		while (s.length() < 8) {
			s = "0" + s;
		}
		return s;
	}

	/**
	 * The per-image signature, as a sentence.
	 *
	 * <p>⚠ Deliberately separate from {@link #verdict()}: that method answers "what is the shape
	 * of the disagreement", this one answers "are these even two renders of the same scene".
	 * The second question comes FIRST when the numbers look like a total disagreement, which
	 * is exactly what the first live run produced.
	 */
	public String diagnose() {
		if (compared == 0) {
			return "no comparison was taken";
		}
		int swMean = softwareMeanRgb / (compared * 3);
		int glMean = glMeanRgb / (compared * 3);
		return "software: " + softwareBlank + " blank px of " + compared + ", mean channel "
				+ swMean + "/255 (" + percent(softwareBlank, compared) + " blank). GL: " + glBlank
				+ " blank px, mean channel " + glMean + "/255 (" + percent(glBlank, compared)
				+ " blank). Mean signed delta over differing pixels (GL minus software): R "
				+ signedMean(signedR) + ", G " + signedMean(signedG) + ", B " + signedMean(signedB)
				+ " - a CONSISTENT bias means a transform (shade, darkening, channel), while "
				+ "values near zero mean genuinely different content. Best alignment: offset ("
				+ bestOffsetX + "," + bestOffsetY + ") matches " + percent(bestOffsetMatches,
						bestOffsetTotal) + " of a " + OFFSET_STEP + "-pixel subsample. Samples: "
				+ samples;
	}

	private String signedMean(long total) {
		if (differing == 0) {
			return "n/a";
		}
		double mean = (double) total / differing;
		return (mean >= 0 ? "+" : "") + (Math.round(mean * 10.0) / 10.0);
	}

	/**
	 * Writes BOTH images to PNG, because statistics took this as far as they could (Phase 7.5c).
	 *
	 * <p>⚠⚠ <b>Why this is worth doing rather than more analysis.</b> The first content line ruled
	 * out the mundane causes - both images rich, best alignment (0,0), so nothing is empty and
	 * nothing is shifted - and the sampled values then showed the software drawing saturated
	 * OLIVE where GL drew near-GRAY and the reverse elsewhere. That is different content, not a
	 * transform, and it cannot be resolved by more counting: the question is now "what does each
	 * image look like", and that is a question for eyes. ⚠ One glance at two PNGs answers what
	 * any number of percentages cannot - whether GL's ground is the wrong texture, or the wrong
	 * shade, or the software image itself is not what we think it is.
	 *
	 * <p>⚠ Printed as absolute paths so the files can be found and attached without hunting.
	 *
	 * @param prefix    written as {@code prefix-software.png} and {@code prefix-gl.png}
	 * @param glPixels  the framebuffer after the readback, i.e. the GL image
	 * @return whether BOTH files were written
	 */
	public boolean writePngs(String prefix, int[] glPixels, int width, int height) {
		if (snapshot == null || glPixels == null || width != this.width || height != this.height
				|| width <= 0 || height <= 0) {
			return false;
		}
		return writePng(prefix + "-software.png", snapshot, width, height)
				&& writePng(prefix + "-gl.png", glPixels, width, height);
	}

	private static boolean writePng(String path, int[] argb, int width, int height) {
		try {
			// ⚠ TYPE_INT_RGB rather than ARGB on purpose: the composited frame carries no alpha
			// at all (see the class doc on why alpha is masked out), so an ARGB image would be
			// fully transparent and every viewer would draw it as black - a dump that proves
			// nothing while looking like a rendering fault.
			java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(width, height,
					java.awt.image.BufferedImage.TYPE_INT_RGB);
			img.setRGB(0, 0, width, height, argb, 0, width);
			return javax.imageio.ImageIO.write(img, "png", new java.io.File(path));
		} catch (Throwable t) {
			// A debug dump must never take the client down.
			return false;
		}
	}

	private int blockIndex(int x, int y) {
		return (y / BLOCK) * blocksWide + (x / BLOCK);
	}

	/**
	 * Counts the blocks whose EVERY pixel differs, and which flavour of delta filled them.
	 *
	 * <p>⚠ The block's own area is used rather than {@code BLOCK*BLOCK}, because a frame whose
	 * width is not a multiple of eight has a partial column of blocks - and calling a 4-wide
	 * edge block "solid" on 32 differing pixels would manufacture exactly the signal this
	 * class exists to detect.
	 */
	private void classifyBlocks(int width, int height) {
		for (int by = 0; by < blocksHigh; by++) {
			int bh = Math.min(BLOCK, height - by * BLOCK);
			for (int bx = 0; bx < blocksWide; bx++) {
				int bw = Math.min(BLOCK, width - bx * BLOCK);
				int area = bw * bh;
				int bi = by * blocksWide + bx;
				if (blockDiffering[bi] != area) {
					continue;
				}
				solidBlocks++;
				if (blockFar[bi] == area) {
					solidFar++;
				} else if (blockNear[bi] == area) {
					solidNear++;
				}
			}
		}
	}

	/**
	 * Zeroes the last results and sizes the block grids for this frame.
	 *
	 * <p>⚠ The block arrays are REUSED when they already fit, because this runs inside the
	 * render loop and allocating a few thousand ints per frame is the exact cost this path
	 * exists to avoid.
	 */
	private void reset(int[] pixels, int width, int height) {
		compared = 0;
		identical = 0;
		differing = 0;
		near = 0;
		far = 0;
		alphaOnly = 0;
		solidBlocks = 0;
		solidFar = 0;
		solidNear = 0;
		blocks = 0;
		longestRun = 0;
		maxDelta = 0;
		softwareBlank = 0;
		glBlank = 0;
		softwareMeanRgb = 0;
		glMeanRgb = 0;
		samples = null;
		bestOffsetX = 0;
		bestOffsetY = 0;
		bestOffsetMatches = 0;
		bestOffsetTotal = 0;
		signedR = 0;
		signedG = 0;
		signedB = 0;
		if (width <= 0 || height <= 0) {
			blocksWide = 0;
			blocksHigh = 0;
			return;
		}
		blocksWide = (width + BLOCK - 1) / BLOCK;
		blocksHigh = (height + BLOCK - 1) / BLOCK;
		blocks = blocksWide * blocksHigh;
		int size = blocks;
		if (blockDiffering == null || blockDiffering.length < size) {
			blockDiffering = new int[size];
			blockFar = new int[size];
			blockNear = new int[size];
		} else {
			java.util.Arrays.fill(blockDiffering, 0, size, 0);
			java.util.Arrays.fill(blockFar, 0, size, 0);
			java.util.Arrays.fill(blockNear, 0, size, 0);
		}
	}

	/**
	 * The largest per-channel difference, ignoring alpha.
	 *
	 * <p>⚠ Operates on the PACKED ints and never unpacks into floats: these are the software's
	 * own 8-bit channels, and the whole point of the integer atlas is that a channel is a byte
	 * rather than a number that has been through a colour space.
	 */
	private static int channelDelta(int a, int b) {
		int dr = Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF));
		int dg = Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF));
		int db = Math.abs((a & 0xFF) - (b & 0xFF));
		return Math.max(dr, Math.max(dg, db));
	}

	// ---------------------------------------- the verdict

	/**
	 * What the shape of the disagreement says, as a sentence rather than a number.
	 *
	 * <p>⚠ <b>This is the part a human reads, so it states the CLASS and the evidence for it
	 * rather than only the counts.</b> A bare "42,318 pixels differ" invites the wrong
	 * reaction (it sounds large) when the correct reading is "scattered, therefore sampling".
	 */
	public String verdict() {
		if (compared == 0) {
			return "no comparison was taken";
		}
		if (differing == 0) {
			return "IDENTICAL - the GL image and the software image agree on every pixel of "
					+ compared + " (alpha " + (alphaOnly == 0 ? "included" : "differs on "
							+ alphaOnly + " of them") + ")";
		}
		String rate = percent(differing, compared);
		if (solidBlocks == 0) {
			return "SAMPLING-CLASS - " + differing + " of " + compared + " pixels differ (" + rate
					+ "), scattered: NOT ONE " + BLOCK + "x" + BLOCK + " block is entirely "
					+ "different, which a merely-off-by-a-texel mapping produces and a wrong "
					+ "asset does not. Largest delta " + maxDelta + "/255, longest run "
					+ longestRun + "px.";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("AREA-CLASS - ").append(differing).append(" of ").append(compared)
				.append(" pixels differ (").append(rate).append(") and ").append(solidBlocks)
				.append(" of ").append(blocks).append(' ').append(BLOCK).append('x').append(BLOCK)
				.append(" blocks are ENTIRELY different - uniform scattered noise cannot do "
						+ "that, so this is not sampling. ");
		if (solidFar > 0) {
			sb.append(solidFar).append(" of them have LARGE deltas (max ").append(maxDelta)
					.append("/255), i.e. the WRONG COLOUR came from somewhere - suspect the "
							+ "resolved texture id or the atlas layer. ");
		}
		if (solidNear > 0) {
			sb.append(solidNear).append(" have SMALL deltas, i.e. the right asset shaded "
					+ "systematically wrong - suspect the shade block or the fog fade. ");
		}
		sb.append("Longest run ").append(longestRun).append("px.");
		return sb.toString();
	}

	private static String percent(int part, int whole) {
		if (whole == 0) {
			return "n/a";
		}
		return (Math.round(10000.0 * part / whole) / 100.0) + "%";
	}

	/**
	 * The whole comparison as one line.
	 *
	 * <p>⚠ The counts are printed even when the verdict is "identical", so a clean run is
	 * evidence of a comparison having HAPPENED rather than of this method not being called.
	 */
	public String describe() {
		return "compared " + compared + " pixels: identical " + identical + ", differing "
				+ differing + " (identical RGB but different alpha: " + alphaOnly + "; small "
				+ near + ", large " + far + "), solid " + BLOCK + "x" + BLOCK + " blocks "
				+ solidBlocks + " of " + blocks + " (" + solidFar + " large-delta, " + solidNear
				+ " small-delta), longest differing run " + longestRun + "px, max channel delta "
				+ maxDelta + "/255";
	}

	// ---- accessors. Each is a FACT about the last comparison, and each is asserted in the
	// ---- harness, because a classifier nobody checks is the failure mode this exists to fix.

	public int compared() {
		return compared;
	}

	public int identical() {
		return identical;
	}

	public int differing() {
		return differing;
	}

	public int near() {
		return near;
	}

	public int far() {
		return far;
	}

	public int alphaOnly() {
		return alphaOnly;
	}

	public int solidBlocks() {
		return solidBlocks;
	}

	public int solidFar() {
		return solidFar;
	}

	public int solidNear() {
		return solidNear;
	}

	public int blocks() {
		return blocks;
	}

	public int longestRun() {
		return longestRun;
	}

	public int maxDelta() {
		return maxDelta;
	}

	public int softwareBlank() {
		return softwareBlank;
	}

	public int glBlank() {
		return glBlank;
	}

	public String samples() {
		return samples;
	}

	public int bestOffsetX() {
		return bestOffsetX;
	}

	public int bestOffsetY() {
		return bestOffsetY;
	}

	public int bestOffsetMatches() {
		return bestOffsetMatches;
	}

	public int bestOffsetTotal() {
		return bestOffsetTotal;
	}

	public long signedR() {
		return signedR;
	}

	public long signedG() {
		return signedG;
	}

	public long signedB() {
		return signedB;
	}
}
