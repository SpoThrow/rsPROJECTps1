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
	 * ⚠⚠ THE ONE OWNER OF THE TEXTURED SHADE FADE (Phase 7.4j): a corner shade code with
	 * {@code method378}'s own fog fold applied.
	 *
	 * <p><b>Why this is a code and not a colour.</b> The GL path hands the raw shade to the
	 * fragment shader and the SHADER derives the darkness block and the extra shift from it -
	 * so the fade has to be applied to the CODE before the seam, not folded into a colour
	 * afterwards. That is also what the software does: {@code method378}'s first three
	 * statements fade {@code k1/l1/i2}, the three corner shade codes, and only then is anything
	 * derived from them.
	 *
	 * <p>⚠⚠ <b>AND THE FADE IS NOT A TINT, which is why skipping it was a real bug rather than
	 * a shade of grey.</b> {@code fadeHsl} reads its argument as an HSL colour, so a bare shade
	 * code presents as luminance {@code code} and the fade pulls it toward 68 - and because the
	 * BLOCK and SHIFT are derived from the faded value, fog can change WHICH of the four
	 * darkness copies is sampled. A fog-free oracle cannot see the difference, so this had to be
	 * pinned deliberately (see the harness) rather than left to a spot check.
	 *
	 * <p>⚠ {@link Fog#fadeHsl} is reused rather than re-derived: this package treats the fog
	 * curve as having one owner, and a second copy here is exactly the drift that decision
	 * exists to prevent.
	 *
	 * @param shadeCode  the model's raw corner shade ({@code 127 - light})
	 * @param sceneDepth the depth {@code method378} would read, i.e. {@code Fog.sceneDepth} at
	 *                   draw time
	 */
	public static int fadedShade(int shadeCode, int sceneDepth) {
		// ⚠ The guard is method378's, verbatim: under 50 there is no fog to apply, and with
		// fogStrength at 0 fadeHsl's factor is 0 anyway - but the guard is kept so the two
		// conditions are stated in one place rather than inferred from fadeHsl's internals.
		if (sceneDepth > 50 && game.client.fogStrength > 0) {
			return Fog.fadeHsl(shadeCode, sceneDepth);
		}
		return shadeCode;
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
	 * <p>⚠️ <b>Delegates to {@link #fadedShade} so the fade has ONE owner</b> - the previous
	 * copy of the guard here is what this step removed, because the GL path needs the faded CODE
	 * and a second expression of "when does fog apply" would be free to drift from it.
	 *
	 * @param sceneDepth the depth {@code method378} would have read, i.e.
	 *                   {@code Fog.sceneDepth} at draw time
	 */
	public static int shade(int brightnessBlock0, int shadeCode, int sceneDepth) {
		return shade(brightnessBlock0, fadedShade(shadeCode, sceneDepth));
	}

	/**
	 * The texel index a normalised texture coordinate selects.
	 *
	 * <p><b>Which is what {@code method379}'s {@code i = l1 / l5} works out to.</b> The
	 * rasteriser divides the interpolated texture PLANE by the interpolated DEPTH plane
	 * per pixel - so the ratio is perspective-correct rather than affine, which is why the
	 * seam carries the three ramp NUMERATORS ({@code uNum/vNum/wNum}) and not projected
	 * coordinates and not camera-space ones either: dividing the interpolated numerators is
	 * what reproduces {@code method379}, and dividing an interpolated camera-space triple
	 * is the different shape {@link ui.TextureRamps} measures at 12 of 10962 pixels. The
	 * texel index is then {@code i >> 6} at size 64 and {@code i >> 7} at size 128, i.e.
	 * {@code (uNum/wNum) * size} in both cases.
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
	 * The COLUMN a texture coordinate selects - <b>CLAMPED</b>, which is what {@code method379} does
	 * to {@code i}.
	 *
	 * <p>⚠⚠ <b>This and {@link #texelRow} are separate methods because the software treats the two
	 * axes DIFFERENTLY, and that asymmetry was the bug (Phase 7.8).</b> Both call sites of
	 * {@code method379} clamp the column - {@code Texture.java:1969}
	 * ({@code if (i < 0) i = 0; else if (i > 16256) i = 16256;}) at size 128 and the equivalent at
	 * 64 - and the pre-shift bound {@code 16256 >> 7} IS {@code size - 1}, so a plain
	 * {@code clamp(0, size-1)} is the right transcription.
	 */
	public static int texelColumn(int texel, int size) {
		if (texel < 0) {
			return 0;
		}
		return texel >= size ? size - 1 : texel;
	}

	/**
	 * The ROW a texture coordinate selects - <b>WRAPPED, not clamped</b>, which is what
	 * {@code method379} does to {@code j}.
	 *
	 * <p>⚠⚠⚠ <b>This one method is the fix for the random-coloured tiles.</b> The software's fetch
	 * is {@code ai1[(j & 0x3f80) + (i >> 7)]} ({@code Texture.java:1995} at size 128, and
	 * {@code (j & 0xfc0) + (i >> 6)} at {@code Texture.java:1840} for size 64) - so the row is taken
	 * through a bitwise MASK, which is a WRAP modulo the texture side, while the column two tokens
	 * to its right was CLAMPED by the guard above. {@code 0x3f80 = 127 << 7}, so the mask is exactly
	 * {@code (j >> 7) & 127}, i.e. the row index modulo 128.
	 *
	 * <p>⚠ <b>THE GL SHADER USED TO CLAMP BOTH AXES, and the divergence is total rather than
	 * subtle:</b> for the live probe's own values the software wraps {@code 194 -> 66},
	 * {@code 167 -> 39}, {@code 256 -> 0}, {@code -5 -> 123} and {@code -18 -> 110}, where a clamp
	 * gives the edge texel ({@code 127} or {@code 0}) every time. ⚠⚠ <b>And the probe showed roughly
	 * HALF of all probed corners resolving out of range</b>, so on those faces the two renderers
	 * sample unrelated rows of the texture - which is precisely a patchwork of unrelated colours,
	 * the symptom this step exists to remove.
	 *
	 * <p>⚠ Verified rather than asserted: {@code (raw & 0x3f80) >> 7} was evaluated against
	 * {@code ((texel % size) + size) % size} for {@code 194, 167, 256, -5, 136, 140, 131, 108, 66,
	 * -18} and agreed on every one. The modulo form is used rather than a bitwise {@code &} because
	 * it states the wrap without depending on a language's signed-shift behaviour - and because the
	 * software's own mask is equivalent to it, which is the only thing that matters.
	 *
	 * @param texel the row the coordinate resolves to, possibly negative or past the end
	 * @param size  the texture side, a power of two
	 */
	public static int texelRow(int texel, int size) {
		return ((texel % size) + size) % size;
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

	/**
	 * Side, in texels, of EVERY texture the client holds - and therefore the side of
	 * every layer of a texture array, which requires all layers to agree.
	 *
	 * <p>⚠️ <b>It is a global property rather than a per-texture one, and it changes when
	 * the detail level changes.</b> {@code Texture.lowMem} selects 64 or 128, and
	 * {@code client.main} calls {@code setHighMem()} before anything else, so the 128
	 * side is the one that actually runs in play. An uploader must therefore read this
	 * rather than hardcode a side, and must rebuild if the flag moves.
	 */
	public static int layerSize() {
		return Texture.lowMem ? 64 : 128;
	}
}
