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
	 * fact and being a hope. {@link GlBatcher} answers {@code false} today: it has no
	 * atlas, no UV attribute and no sampler.
	 *
	 * @return {@code true} if {@link #textured} can actually draw something
	 */
	boolean supportsTextures();

	/**
	 * Accepts one textured triangle.
	 *
	 * <p>⚠ <b>{@code u/v/w} are CAMERA-SPACE, not normalised texture coordinates, and
	 * that is deliberate.</b> They are the software's {@code anIntArray1668/1669/1670} -
	 * the values {@code Texture.method378} hands {@code method379}, which divides by
	 * {@code w} per PIXEL to get a perspective-correct sample. Handing over coordinates
	 * that were already divided and letting the GPU interpolate those affinely would be
	 * the same error the near-plane clipper refuses to make (see {@link GlClipper}), so
	 * the divide is left to the one layer that can do it once per pixel.
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
	 * @param u0,v0,w0  first vertex camera-space texture coordinates
	 * @param shade0    first vertex model shade code
	 * @param textureId the texture to sample, {@code Model.faceTextureId}
	 * @return {@code true} if the triangle was accepted; {@code false} means the sink
	 *         could not draw it and the caller must not treat it as drawn
	 */
	boolean textured(float x0, float y0, float z0, float u0, float v0, float w0, int shade0,
			float x1, float y1, float z1, float u1, float v1, float w1, int shade1,
			float x2, float y2, float z2, float u2, float v2, float w2, int shade2,
			int textureId);
}
