package ui;

/**
 * The software rasteriser's texture mapping, as a function of its nine camera-space
 * slots (Phase 7.2b-2l).
 *
 * <p><b>What this is, and why it is the ground resolver's core.</b> The ground never
 * submits a {@code Model} - {@link SceneRasterizer.Implementation#drawGroundTriangle}
 * hands over nine raw camera-space {@code int}s plus a screen triangle and nothing else
 * - so there is no {@code Model} to project and no equivalent of {@link GlFacePipeline}.
 * Those nine values are precisely what {@code Texture.method378} is given, and this class
 * is the pure function from them to the texture mapping.
 *
 * <p>⚠⚠ <b>The mapping is NOT per-vertex interpolation, and that was MEASURED rather than
 * read.</b> {@code method378} builds nine ramp coefficients from the nine slots by taking
 * 2x2 minors, and {@code method379} evaluates, per pixel,
 * {@code column = (uNum / (wNum >> 12)) >> 6}. Both numerator and denominator are
 * <i>affine in screen space</i>, so the result is a <b>ratio of two screen-affine
 * functions</b> - which is not any interpolation of per-vertex attributes. Phase 7.2b-2k
 * pinned this: on a geometry whose ramps fit in 32 bits, this formula reproduces the real
 * rasteriser's texel at <b>92.5% of pixels within one texel</b>, while interpolating
 * {@code u/w} matches 0 and interpolating {@code (u,v,w)} and dividing matches 12.
 *
 * <p><b>How a GL path is meant to consume this - the division of labour.</b> A shader
 * cannot evaluate {@code (uNum / (wNum >> 12)) >> 6} from a per-vertex attribute pair
 * alone, because the RATIO is not affine. But {@code uNum}, {@code vNum} and {@code wNum}
 * ARE affine, so the shader interpolates those three <i>linearly in screen space</i>
 * (GLSL {@code noperspective}) and divides in the fragment shader. So the correct handoff
 * is {@link #uNumerator}/{@link #vNumerator}/{@link #wNumerator} evaluated at the
 * triangle's three screen vertices - <b>not</b> the raw nine, and not the ratio.
 *
 * <p>✅ <b>AND THE SHADER NEEDS NO SHIFT EMULATION - MEASURED, because the plausible
 * reading says otherwise.</b> {@code method379} computes
 * {@code col = (uNum / (wNum >> 12)) >> 6}, i.e. it <i>truncates the denominator</i>
 * before dividing, which is not the same as the plain ratio {@code (uNum/wNum) * size}.
 * The difference is largest where {@code w} is small, i.e. close to the camera - exactly
 * where it would be noticed. Measured at BOTH detail levels, the plain ratio and the
 * software's shifted form agree at <b>10963 of 10964 pixels (size 64)</b> and
 * <b>10956 of 10964 (size 128)</b>, so a fragment shader may evaluate the plain ratio and
 * skip the shift entirely. Without that measurement the shift would have looked mandatory.
 *
 * <p>⚠⚠ <b>THE TWO DETAIL LEVELS USE DIFFERENT SHIFTS, AND SIZE 128 IS THE ONE THAT RUNS.</b>
 * Read out of {@code method379}'s two branches:
 * <pre>
 *   size  64 (lowMem): i = l1 / (j2 &gt;&gt; 12), clamp [0, 4032],  column = i &gt;&gt; 6
 *   size 128:          i = l1 / (j2 &gt;&gt; 14), clamp [0, 16256], column = i &gt;&gt; 7
 * </pre>
 * {@code GlTextures.layerSize()} records that {@code client.main} calls
 * {@code setHighMem()} before anything else, so <b>128 is the level in play</b>; a
 * resolver pinned only on the 64 branch would be right in the harness and wrong on
 * screen. The shifts are therefore parameters here and never hardcoded.
 *
 * <p>⚠ <b>The arithmetic is deliberately {@code int}, wrapping included.</b> {@code
 * method378} shifts each 2x2 minor left by 14, 8 or 5 bits into an {@code int}, so a
 * geometry with large camera values <i>wraps</i> and the shipped client's picture is a
 * property of that wrap. A resolver that used {@code long} to "fix" the overflow would
 * therefore disagree with the software it exists to reproduce. {@link #overflows()} reports
 * whether the wrap happened, because a wrapping geometry cannot be handed to a shader at
 * all (its ramps are not affine), and that is worth being able to detect rather than
 * guess at.
 *
 * <p>⚠ <b>{@code textureInt1}/{@code textureInt2} are PARAMETERS, not reads from
 * {@link Texture}.</b> They are the screen origin the ramps are anchored to, and passing
 * them keeps this class a pure function that a headless harness can drive without a live
 * {@code Texture} - the same reason {@code GlTextures.shade} takes its scene depth rather
 * than reading {@code Fog} (this package's one-owner rule).
 *
 * <p><b>Accuracy, stated so it is not over-read.</b> {@code method379} seeds each span at
 * its left edge with a <i>truncated</i> step and then interpolates within 8-pixel blocks,
 * so the software's mapping is not an exact continuous function of {@code (x, y)} - it
 * carries a span-structure-dependent error of up to about one texel. This class evaluates
 * the exact affine form, so it agrees with the software to that tolerance and not
 * bit-exactly (measured: 96.4% within one texel at size 64, 91.2% at size 128, on the
 * fixture triangle). A caller that needs bit-exactness must reproduce {@code method379}'s
 * span walk; a caller that needs the mapping does not.
 */
public final class TextureRamps {

	private final int uNumBase;
	private final int vNumBase;
	private final int wNumBase;
	private final int uNumStepY;
	private final int vNumStepY;
	private final int wNumStepY;
	private final int uNumStepX;
	private final int vNumStepX;
	private final int wNumStepX;
	private final int originX;
	private final int originY;
	private final int denShift;
	private final int colShift;
	private final int size;
	private final boolean overflowed;

	private TextureRamps(int uNumBase, int vNumBase, int wNumBase, int uNumStepY, int vNumStepY,
			int wNumStepY, int uNumStepX, int vNumStepX, int wNumStepX, int originX, int originY,
			int denShift, int colShift, int size, boolean overflowed) {
		this.uNumBase = uNumBase;
		this.vNumBase = vNumBase;
		this.wNumBase = wNumBase;
		this.uNumStepY = uNumStepY;
		this.vNumStepY = vNumStepY;
		this.wNumStepY = wNumStepY;
		this.uNumStepX = uNumStepX;
		this.vNumStepX = vNumStepX;
		this.wNumStepX = wNumStepX;
		this.originX = originX;
		this.originY = originY;
		this.denShift = denShift;
		this.colShift = colShift;
		this.size = size;
		this.overflowed = overflowed;
	}

	/**
	 * The denominator shift {@code method379} uses at a layer side - {@code 12} at 64,
	 * {@code 14} at 128. Exposed because the two branches differ and a caller must not
	 * assume the one the harness happens to run.
	 */
	public static int denShiftFor(int size) {
		return size <= 64 ? 12 : 14;
	}

	/** The column shift {@code method379} uses at a layer side - {@code 6} at 64, {@code 7} at 128. */
	public static int colShiftFor(int size) {
		return size <= 64 ? 6 : 7;
	}

	/**
	 * Builds the ramps {@code Texture.method378} would build from the same nine slots.
	 *
	 * <p>The slots are the software's own layout - three planes of three,
	 * {@code (t0,t1,t2)}, {@code (t3,t4,t5)}, {@code (t6,t7,t8)} - and they are taken in
	 * the order the software receives them, <b>not normalised</b>: {@code method378} is
	 * handed the same value twice under two names and the minors mix the planes, so
	 * reordering these to look like three vertices would change the arithmetic.
	 *
	 * @param t0..t8  the nine camera-space values, in {@code method378}'s argument order
	 * @param originX the screen x the ramps are anchored to, i.e. {@code Texture.textureInt1}
	 * @param originY the screen y they are anchored to, i.e. {@code Texture.textureInt2}
	 * @param denShift {@link #denShiftFor} for the layer side in use
	 * @param colShift {@link #colShiftFor} for the layer side in use
	 * @param size     the layer side, which fixes the column clamp and the row mask
	 */
	public static TextureRamps of(int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7,
			int t8, int originX, int originY, int denShift, int colShift, int size) {
		long k2 = (long) t0 - t1;
		long j3 = (long) t3 - t4;
		long i4 = (long) t6 - t7;
		long l2 = (long) t2 - t0;
		long k3 = (long) t5 - t3;
		long j4 = (long) t8 - t6;
		long l4 = l2 * t3 - k3 * t0 << 14;
		long i5 = k3 * t6 - j4 * t3 << 8;
		long j5 = j4 * t0 - l2 * t6 << 5;
		long k5 = k2 * t3 - j3 * t0 << 14;
		long l5 = j3 * t6 - i4 * t3 << 8;
		long i6 = i4 * t0 - k2 * t6 << 5;
		long j6 = j3 * l2 - k2 * k3 << 14;
		long k6 = i4 * k3 - j3 * j4 << 8;
		long l6 = k2 * j4 - i4 * l2 << 5;
		boolean overflowed = overflows(l4) || overflows(i5) || overflows(j5) || overflows(k5)
				|| overflows(l5) || overflows(i6) || overflows(j6) || overflows(k6)
				|| overflows(l6);
		// ⚠ The int expressions are the SAME ones method378 uses, so where the values wrap
		// this reproduces the wrap rather than correcting it - which is the point.
		return new TextureRamps((int) l4, (int) k5, (int) j6, (int) j5, (int) i6, (int) l6,
				(int) i5 >> 3, (int) l5 >> 3, (int) k6 >> 3, originX, originY, denShift, colShift,
				size, overflowed);
	}

	private static boolean overflows(long ramp) {
		return ramp > Integer.MAX_VALUE || ramp < Integer.MIN_VALUE;
	}

	/** The texture column numerator at a screen point - affine in {@code (x, y)}. */
	public int uNumerator(int x, int y) {
		return uNumBase + uNumStepY * (y - originY) + uNumStepX * (x - originX);
	}

	/** The texture row numerator at a screen point - affine in {@code (x, y)}. */
	public int vNumerator(int x, int y) {
		return vNumBase + vNumStepY * (y - originY) + vNumStepX * (x - originX);
	}

	/** The denominator at a screen point - affine in {@code (x, y)}; zero is degenerate. */
	public int wNumerator(int x, int y) {
		return wNumBase + wNumStepY * (y - originY) + wNumStepX * (x - originX);
	}

	/**
	 * The texture column the software samples at a screen point, in
	 * {@code [0, size - 1]}, or {@code -1} where the denominator is zero.
	 *
	 * <p>Clamped exactly as {@code method379} clamps it ({@code i} to
	 * {@code [0, (size-1) << colShift]}), then shifted, so a caller can compare against the
	 * real rasteriser texel for texel rather than through a private convention.
	 */
	public int column(int x, int y) {
		int d = wNumerator(x, y) >> denShift;
		if (d == 0) {
			return -1;
		}
		int i = uNumerator(x, y) / d;
		int clampMax = (size - 1) << colShift;
		if (i < 0) {
			i = 0;
		} else if (i > clampMax) {
			i = clampMax;
		}
		return i >> colShift;
	}

	/**
	 * The texture row the software samples at a screen point, in {@code [0, size - 1]}, or
	 * {@code -1} where the denominator is zero.
	 *
	 * <p>The software does not clamp the row the way it clamps the column - it masks with
	 * {@code & 0xfc0} (at 64) or {@code & 0x3f80} (at 128) on the way into the array - so
	 * the mask is applied here too, to keep this the same number the rasteriser would index
	 * with.
	 */
	public int row(int x, int y) {
		int d = wNumerator(x, y) >> denShift;
		if (d == 0) {
			return -1;
		}
		return (vNumerator(x, y) / d >> colShift) & (size - 1);
	}

	/**
	 * Whether any of the nine ramps overflowed a 32-bit int for this geometry.
	 *
	 * <p>⚠ <b>Reports a real branch rather than a warning.</b> Where the ramps wrap, the
	 * software's mapping is still well-defined (it is the wrap the shipped client uses),
	 * but it is <b>no longer affine</b>, so a shader CANNOT reproduce it by interpolating
	 * the three numerators - {@link #column} stays faithful, the GL handoff does not.
	 * Callers intending to hand a triangle to the GPU should decline instead.
	 */
	public boolean overflows() {
		return overflowed;
	}

	/**
	 * The three numerator values a GL path hands the sink at one screen vertex, in the
	 * order a fragment shader interpolates and then divides them.
	 */
	public int[] attributeAt(int x, int y) {
		return new int[] { uNumerator(x, y), vNumerator(x, y), wNumerator(x, y) };
	}

	/** For diagnostics only - the base ramps, their steps, and the anchor. */
	@Override
	public String toString() {
		return "TextureRamps[u=" + uNumBase + "+" + uNumStepY + "y+" + uNumStepX + "x, v="
				+ vNumBase + "+" + vNumStepY + "y+" + vNumStepX + "x, w=" + wNumBase + "+"
				+ wNumStepY + "y+" + wNumStepX + "x, origin=" + originX + "," + originY
				+ (overflowed ? ", OVERFLOWED]" : "]");
	}
}
