package ui;

/**
 * Anything that can accept a projected, coloured triangle (Phase 7.2b-2b).
 *
 * <p><b>Why this interface exists rather than {@link GlFacePipeline} calling
 * {@link GlBatcher} directly.</b> The pipeline is the piece that has to be verified
 * against the SOFTWARE path, and the software path is available headlessly while a
 * GPU is not - {@code deps/lwjgl3} is deliberately off the harness classpath so
 * {@code GlBatcher} takes its DEGRADATION path there and swallows every triangle. A
 * pipeline that only spoke to {@code GlBatcher} would therefore be untestable without
 * a GPU, and "it compiles" is not a verification this plan accepts for a piece of the
 * rasteriser. With this seam the harness installs a recording sink and compares what
 * the pipeline submits - vertex for vertex, colour for colour - against the real
 * {@code Model.method443}.
 *
 * <p><b>The argument contract is {@link GlBatcher}'s, verbatim, so the seam costs
 * nothing at runtime:</b> vertices are in screen-pixel space with {@code y} growing
 * DOWN, {@code z} in {@code [0,1]} where 0 is nearest, and the colour is packed ARGB.
 * {@code GlBatcher} implements this interface with the very method it already had, so
 * the production path does not gain a call, an object or a copy.
 *
 * <p>⚠ <b>Implementations must be on the GAME THREAD.</b> GL objects are current only
 * there - see {@link GlScene}'s class doc.
 */
public interface TriangleSink {

	/**
	 * Accepts one triangle.
	 *
	 * @param x0,y0,z0 first vertex, pixel x/y with y growing DOWN, z in {@code [0,1]}
	 *                 where 0 is nearest
	 * @param argb0    first vertex colour, packed ARGB
	 * @return {@code true} if the triangle was accepted; {@code false} means the sink
	 *         is not usable (no GL context, no frame in progress) and the caller must
	 *         not treat the triangle as drawn
	 */
	boolean triangle(float x0, float y0, float z0, int argb0,
			float x1, float y1, float z1, int argb1,
			float x2, float y2, float z2, int argb2);

	/**
	 * Whether this sink can draw a textured triangle at all (Phase 7.2b-2e).
	 *
	 * <p>⚠ <b>Asked before every textured face, and the answer is treated as contract
	 * rather than as a hint.</b> A face this sink cannot draw is counted as
	 * {@link GlFacePipeline#NEEDS_TEXTURE} rather than submitted and silently dropped,
	 * which is the difference between {@link GlFacePipeline#allRepresentable()} being a
	 * fact and being a hope. {@link GlBatcher} answers {@code true} as of Phase 7.2b-2f -
	 * it has an atlas, a UV attribute and a sampler - but it may still answer {@code false}
	 * at runtime, because the texture array is built in its own guard: a driver that
	 * refuses integer 3D textures costs the texture path while the flat path keeps
	 * drawing.
	 *
	 * @return {@code true} if {@link #textured} can actually draw something
	 */
	boolean supportsTextures();

	/**
	 * Accepts one textured triangle.
	 *
	 * <p>⚠⚠ <b>{@code u/v/w} ARE THE SOFTWARE'S RAMP NUMERATORS, *NOT* CAMERA-SPACE
	 * COORDINATES AND *NOT* A NORMALISED TEXTURE COORDINATE.</b> They are the three
	 * screen-affine values {@code Texture.method378} builds from its nine camera-space
	 * slots - {@code l4/i5/j5} and friends, the 2x2 minors - evaluated at this vertex.
	 * The sink interpolates them <b>linearly in screen space</b> and DIVIDES per fragment,
	 * so that {@code u/w} and {@code v/w} come out perspective-correct.
	 *
	 * <p>⚠ <b>Why the attribute is the numerator and not the triple, which is what this
	 * seam carried until Phase 7.2b-2l.</b> Handing over camera-space {@code (u,v,w)} and
	 * dividing the interpolated values is a DIFFERENT shape, and it was measured against
	 * the real rasteriser: it matches at <b>12 of 10962</b> pixels, while interpolating
	 * the ramps' numerators and dividing matches at <b>92.5%</b>. The software's mapping
	 * is a ratio of two screen-affine functions, so the two affine halves are the only
	 * thing that may cross the seam.
	 *
	 * <p>✅ <b>And the numerator form needs no further emulation, which was measured
	 * rather than assumed:</b> the software truncates its denominator before dividing
	 * ({@code wNum >> 12} or {@code >> 14}), which is <i>not</i> algebraically the plain
	 * ratio - but at both detail levels the two agree at 99.9% of pixels, so the sink may
	 * simply divide and skip the shift. See {@link TextureRamps}.
	 *
	 * <p>⚠ <b>The denominator can be ZERO</b> where the triangle is edge-on or the
	 * geometry wrapped; a sink must handle it rather than assume {@code w != 0}.
	 *
	 * <p>⚠ <b>{@code shade} is the model's raw per-corner value, NOT a colour.</b> For a
	 * textured face {@code Model.method481} returns {@code 127 - clamp(light)} with no
	 * packing at all, and {@code method379} reads that number as a DARKENING rather than
	 * as a colour code: its bits 4-5 select one of the four darkness blocks
	 * {@code Texture.method371} builds into the texture array, and bit 6 selects a
	 * further right-shift of one. It is passed through RAW rather than pre-decoded, so
	 * that this layer cannot quietly bake in a wrong reading of a mechanism the batcher
	 * has to reproduce literally.
	 *
	 * @param x0,y0,z0  first vertex, pixel x/y with y growing DOWN, z in {@code [0,1]}
	 *                  where 0 is nearest
	 * @param u0,v0,w0  first vertex RAMP NUMERATORS, screen-affine, divided per fragment
	 * @param shade0    first vertex model shade code
	 * @param textureId the texture to sample, {@code Model.faceTextureId}
	 * @return {@code true} if the triangle was accepted; {@code false} means the sink
	 *         could not draw it and the caller must not treat it as drawn
	 */
	boolean textured(float x0, float y0, float z0, float u0, float v0, float w0, int shade0,
			float x1, float y1, float z1, float u1, float v1, float w1, int shade1,
			float x2, float y2, float z2, float u2, float v2, float w2, int shade2,
			int textureId);

	/**
	 * Marks the current end of what has been submitted, in TRIANGLES (Phase 7.4h).
	 *
	 * <p>⚠⚠ <b>Why this exists: to let a caller drop ONE ACTOR rather than the frame.</b>
	 * The scene latch is all-or-nothing - a single unrepresentable face of 128 withheld
	 * every frame - so the fix is to submit a model, and if any of its faces turned out to
	 * be unrepresentable, {@link #rewind} it away and keep the frame. That needs the sink to
	 * be able to undo, and a caller cannot do it from outside: the attribute buffers advance
	 * in lockstep, each by a different number of entries per triangle, so only the sink knows
	 * how to undo one.
	 *
	 * @return a token accepted by {@link #rewind}; opaque to the caller
	 */
	int mark();

	/**
	 * Discards every triangle submitted since {@link #mark()} returned {@code token}.
	 *
	 * <p>⚠ Rewinding to a token from before the current frame's {@code beginFrame} is a
	 * caller error, not a no-op: the buffers are cleared per frame, so such a token has no
	 * meaning. Implementations throw rather than silently discard the frame.
	 */
	void rewind(int token);
}
