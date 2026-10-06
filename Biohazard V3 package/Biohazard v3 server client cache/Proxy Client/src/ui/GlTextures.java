package ui;

import model.Texture;
import scene.Fog;

/**
 * The faithful texture policy for the GL path (Phase 7.2b-2f): how a texture id becomes
 * texels, and how a shade code becomes brightness.
 *
 * <p><b>Why this is a separate, pure class rather than logic inside the shader.</b> The
 * GL arm cannot run in the test harness - LWJGL is deliberately absent there, because
 * the harness must stay hermetic and GPU-free - so anything that lives only in GLSL is
 * <i>unverifiable</i> in this repo. The decision this class exists to encode is
 * therefore expressed in Java, driven against the software renderer's OWN arrays by an
 * oracle, and mirrored in the fragment shader. The shader is then a transcription of
 * something already proven rather than the only copy of a claim.
 *
 * <p><b>⚠️ The two facts this encodes, both read out of {@code Texture} rather than
 * assumed, and both easy to get plausibly wrong.</b>
 * <ol>
 *   <li><b>A textured face's corner slot is a SHADE, not a colour.</b>
 *       {@code Model.method481} returns {@code 127 - clamp(light)} for a face with
 *       {@code (renderType & 2) == 2}, so the value is in {@code [0, 127]} and - the part
 *       that inverts intuition - <b>0 is BRIGHTEST and 127 is DARKEST</b>.</li>
 *   <li><b>The shade's bits are the brightness control, and the low four are unused.</b>
 *       {@code Texture.method379} folds {@code shade & 0x600000} (after the shade has
 *       been shifted up) straight into the texture ARRAY INDEX, and that constant is
 *       {@code 3 << 21} - which, once the {@code >> 7} texel shift is applied, is exactly
 *       {@code 3 * 16384}: one of {@code method371}'s four pre-darkened copies. So
 *       <b>bits 4-5 select the block</b> and <b>bit 6 is one further {@code >>> 1}</b>.
 *       Bits 0-3 never matter.</li>
 * </ol>
 *
 * <p>⚠️ <b>The block formulas are reproduced EXACTLY, including the masking, and the
 * masking is not decoration.</b> Each of {@code method371}'s four copies is written as
 * {@code k2 - (k2 >>> n) & 0xf8f8ff} where {@code k2} is the ALREADY-masked block-0
 * value - so the subtraction is allowed to BORROW ACROSS CHANNELS and the mask then
 * repairs it. Doing the obvious thing instead (darkening each channel in isolation)
 * gives a different, subtly wrong colour, which is why the arithmetic is done on the
 * PACKED int here and in the shader alike.
 */
public final class GlTextures {

	/** The client loads exactly this many texture slots, and some may have failed. */
	public static final int TEXTURE_COUNT = 51;

	/**
	 * The mask {@code method371} applies to every block. Red and green keep their top
	 * five bits; <b>blue keeps all eight</b>, which is why this is not {@code 0xf8f8f8}.
	 */
	static final int BLOCK_MASK = 0xf8f8ff;

	private GlTextures() {
	}

	/**
	 * Which of {@code method371}'s four pre-darkened copies a shade code selects.
	 *
	 * @param shade the corner's raw shade code, {@code 127 - clamp(light)}
	 * @return {@code 0} (full brightness) to {@code 3} (darkest)
	 */
	public static int brightnessBlock(int shade) {
		return (shade >> 4) & 3;
	}

	/**
	 * The further one-bit darkening {@code method379} applies on top of the block.
	 *
	 * @param shade the corner's raw shade code
	 * @return {@code 0} or {@code 1}
	 */
	public static int extraShift(int shade) {
		return shade >> 6;
	}

	/**
	 * The exact packed pixel {@code method371} stores for one texel at one brightness
	 * block - i.e. the software's own darkening arithmetic, reproduced.
	 *
	 * @param brightnessBlock0 block 0 of the texture, already masked
	 * @param block            {@code 0} to {@code 3}
	 * @return the packed {@code 0x00RRGGBB} of that block
	 */
	public static int blockColour(int brightnessBlock0, int block) {
		switch (block) {
			case 1:
				return (brightnessBlock0 - (brightnessBlock0 >>> 3)) & BLOCK_MASK;
			case 2:
				return (brightnessBlock0 - (brightnessBlock0 >>> 2)) & BLOCK_MASK;
			case 3:
				return (brightnessBlock0 - (brightnessBlock0 >>> 2)
						- (brightnessBlock0 >>> 3)) & BLOCK_MASK;
			default:
				return brightnessBlock0;
		}
	}

	/**
	 * The packed pixel the software rasteriser would write for this texel under this
	 * shade code, with no fog.
	 */
	public static int shade(int brightnessBlock0, int shadeCode) {
		return blockColour(brightnessBlock0, brightnessBlock(shadeCode))
				>>> extraShift(shadeCode);
	}

	/**
	 * The same, with the shade FADED the way {@code method378} fades it.
	 *
	 * <p>⚠️ <b>A textured face's shade goes through {@code Fog.fadeHsl} before it is used,
	 * and that is not a formality.</b> {@code fadeHsl} reads its argument as an HSL
	 * colour, so a bare shade code {@code 0..127} presents as hue 0, saturation 0 and
	 * luminance {@code shade} - and the fade pulls that luminance toward 68. Because the
	 * BLOCK and SHIFT are then derived from the faded value, fog does not merely tint a
	 * textured face: it can change which of the four darkness copies is sampled. Skip it
	 * and fogged textured faces come out at the wrong brightness, in a way that a
	 * fog-free oracle would never catch.
	 *
	 * <p>{@link Fog#fadeHsl} is deliberately REUSED rather than re-derived: this package
	 * already treats the fog curve as having a single owner (see {@code Fog.applyFlatAt}),
	 * and a second copy of it here is exactly the drift that decision exists to prevent.
	 *
	 * @param sceneDepth the depth {@code method378} would have read, i.e.
	 *                   {@code Fog.sceneDepth} at draw time
	 */
	public static int shade(int brightnessBlock0, int shadeCode, int sceneDepth) {
		if (sceneDepth > 50 && game.client.fogStrength > 0) {
			shadeCode = Fog.fadeHsl(shadeCode, sceneDepth);
		}
		return shade(brightnessBlock0, shadeCode);
	}

	/**
	 * The texel index a normalised texture coordinate selects.
	 *
	 * <p><b>Which is what {@code method379}'s {@code i = l1 / l5} works out to.</b> The
	 * rasteriser divides the interpolated texture PLANE by the interpolated DEPTH plane
	 * per pixel - so the ratio is perspective-correct rather than affine, which is the
	 * whole reason the seam carries camera-space {@code u/v/w} instead of projected
	 * coordinates - and {@code i >> 7} is the texel index, giving {@code (u/w) * size}.
	 *
	 * <p>⚠️ <b>The clamp is the software's, and it is asymmetric.</b> {@code method379}
	 * clamps to {@code [7, size*size - size]}, i.e. to a seven-{@code size}ths inset at
	 * the LOW end and the last texel exactly at the high end. A plain
	 * {@code clamp(0, size-1)} reproduces the visible behaviour because the low inset is
	 * under one texel - but it is a deliberate simplification, recorded rather than
	 * silently made.
	 */
	public static int texelIndex(float uv, int size) {
		int texel = (int) Math.floor(uv * size);
		if (texel < 0) {
			return 0;
		}
		return texel >= size ? size - 1 : texel;
	}

	/**
	 * Whether the loaded cache holds a usable texture for this id.
	 *
	 * <p>The uploader must ask, because {@code Texture.unpack} swallows per-texture
	 * failures and a slot can legitimately be empty.
	 */
	public static boolean available(int id) {
		return Texture.hasTexture(id);
	}
}
