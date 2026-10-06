package ui;

import model.Model;
import model.Texture;
import scene.Fog;
import scene.WorldController;

/**
 * The face step: projected vertices to coloured triangles (Phase 7.2b-2b).
 *
 * <p><b>What this is, and why it is a separate piece from the projection.</b>
 * {@link GlModelProjection} reproduces the transform {@code Model.method443} performs
 * on vertices. That is only half of what {@code method443} does: the vertices then go
 * through {@code method483}, which walks the model's FACES. This class is that walk.
 * For each face it decides whether the face is drawn at all, resolves its colours, and
 * hands {@link GlBatcher} a triangle in screen-pixel space.
 *
 * <p><b>⚠⚠ The one thing the software does here that this deliberately does NOT
 * reproduce, and the reason that is safe.</b> {@code method483} ends by sorting the
 * surviving faces into {@code anIntArrayArray1672}, a per-depth BUCKET, and draws the
 * buckets from far to near - a painter's algorithm. That sort exists only because a
 * software rasteriser has no depth buffer. A GL depth attachment does the same job in
 * hardware, so reproducing the ordering would be pure waste; what must be reproduced
 * is the CULLING, the COLOURS and the DEPTHS, and those are. Faces are therefore
 * emitted in FACE-INDEX order, and the depth values are supplied per vertex so the
 * depth test resolves them.
 *
 * <p>⚠ <b>And emitting in face-index order is not merely acceptable, it reproduces the
 * software's tie-breaking.</b> Within one bucket {@code method483} appends faces in
 * ascending face index and draws them in that order, so on an exact depth tie the
 * HIGHER face index wins. Equal depths necessarily share a bucket (the bucket is
 * derived from the mean depth), so a depth buffer with {@code GL_LEQUAL} - later wins
 * on a tie - agrees with the software exactly.
 *
 * <p><b>What is verified, and how.</b> The harness drives the REAL
 * {@code Model.method443} on a parsed real-format fixture, reads the REAL face buckets
 * {@code method483} left behind, and requires this class's per-face outcomes to match
 * them face for face; it then reads the pixel the REAL software rasteriser painted and
 * requires this class's colour to be the same number. A hand-written expectation could
 * only prove this class agrees with the same reading of {@code method483} that produced
 * it, which is 5.3's trap; the software path can dispute it and that is the point.
 *
 * <p><b>⚠ What this class does NOT yet do, stated so the gap is measured rather than
 * assumed.</b> One thing the software path can draw is still not representable here, and
 * it is counted rather than dropped:
 * <ul>
 *   <li><b>Textured faces</b> (render type 2 or 3). ⚠ <b>7.2b-2f closed the sink half of
 *       this.</b> This class RESOLVES a textured face completely - the texture id out of
 *       the face's colour slot, the screen-affine ramp NUMERATORS read at the
 *       {@code renderType >> 2} texture-coordinate index, and the raw per-corner shade
 *       code - and hands it to {@link TriangleSink#textured}, and
 *       {@link GlBatcher#supportsTextures()} now answers {@code true} because it has an
 *       atlas, a UV attribute and a sampler. So in the client a textured face is drawn
 *       rather than counted. ⚠ <b>The sink can still decline per face and the outcome is
 *       still counted rather than dropped:</b> a face whose texture is not in the loaded
 *       cache, or whose ramp numerators wrapped a 32-bit int, comes back
 *       {@link #NEEDS_TEXTURE}. That is exactly why the answer is asked for per face
 *       instead of assumed once.</li>
 * </ul>
 * ⚠ <b>The near plane is NO LONGER on that list, for TEXTURED faces either.</b>
 * {@code method485}'s camera-space clipper is reproduced by {@link GlClipper} and reached
 * through the {@link #CLIPPED} branch, so a face the near plane cuts is rebuilt and drawn
 * here the way the software draws it - including a CUT TEXTURED face, which until 7.2b-2n
 * was the common decline. What remains unrepresentable is only a cut face whose edge leaves
 * the clipper's reciprocal table - {@link #NEEDS_CLIPPING} - which is a divergence in the
 * SOFTWARE too (it aborts the model mid-draw there).
 *
 * <p>⚠ {@link #NEEDS_TEXTURE}, {@link #NEEDS_CLIPPING} and {@link #NO_COLOUR} are the
 * whole reason {@link #allRepresentable()} exists: the 7.2b-2 wiring step must take the
 * scene over WHOLE or not at all, so it needs to ask this question about the frame before
 * committing to it. The honest answer today is still "not yet", but now for TEXTURES
 * alone rather than for every face close to the camera.
 *
 * <p>⚠⚠ <b>And asking WHETHER is not the same as knowing WHAT, which is why
 * {@link #declineReason()} exists.</b> The first live GL run (7.4) ended at exactly that
 * gap: the client printed that the frame was withheld because "a model face the GL path
 * cannot represent", which names this file rather than the fault - and the eight distinct
 * failures behind {@link #NEEDS_TEXTURE} alone (see the {@code TEXTURE_*} constants) call
 * for completely different fixes. The reason is therefore recorded at each {@code return}
 * site and reported with its census, rather than being reconstructed from the counts
 * afterwards. {@link #allRepresentable()} stays a cheap count test so the per-model path
 * allocates nothing; only the frame that is actually withheld pays for its own explanation.
 *
 * <p><b>⚠ One colour difference that is real and is not hidden.</b> The software
 * interpolates the 16-bit colour CODE across a face and looks each interpolated code up
 * in the palette per pixel; this class resolves the palette at the three CORNERS and
 * lets the GPU interpolate the resulting RGB. The corners agree exactly in both; the
 * interior can differ by about one palette step on a gouraud face. On a flat face (all
 * three corner codes equal - the majority of scenery) the two are identical. Closing
 * that gap means sampling the palette in the fragment shader with a code attribute
 * rather than an RGB one, which is a batcher change, not a pipeline change, and is
 * recorded here rather than pretended away.
 *
 * <p>⚠ Not yet wired to anything: zero call sites, like 5.1's accessors, 5.2's buffers
 * and {@link GlModelProjection} when they landed. Its scratch is per-instance, not
 * static, because 5.1's finding is that shared static draw scratch is exactly what made
 * the software path's projected arrays unusable to anyone else.
 */
public final class GlFacePipeline {

	/** A triangle was submitted for this face. */
	public static final int DRAWN = 0;

	/** Back-facing: {@code method483}'s signed-area test rejected it. */
	public static final int CULLED = 1;

	/**
	 * At least one vertex sits at or behind the near plane, so {@code method443} hands
	 * the face to {@code method485}'s clipper - and the clipper HANDLED it. This outcome
	 * means "representable, via the clip path", <b>not</b> "something was drawn": the
	 * clipper legitimately submits nothing when the face lies wholly behind the plane or
	 * when its rebuilt polygon is back-facing, and the software draws nothing either.
	 *
	 * <p>⚠ Whether a face was cut <i>at all</i> is a separate question, reported by
	 * {@link #clipped()}. A cut face can come out {@link #CLIPPED} whether or not its
	 * corner colour resolves - a cut face whose corner colour will not resolve is
	 * {@link #NO_COLOUR} - so folding the cut flag into the outcome would make the clip
	 * oracle agree for the wrong reason.
	 */
	public static final int CLIPPED = 2;

	/**
	 * At least one vertex sits at or behind the near plane and the clipper could not be
	 * applied, so nothing was submitted. See {@link GlClipper#UNUSABLE} for the one case
	 * that produces this.
	 */
	public static final int NEEDS_CLIPPING = 3;

	/**
	 * Render type 2 or 3 - textured - and the sink could not draw it. Since 7.2b-2n this
	 * means one of three things rather than a missing description: the SINK cannot sample
	 * textures, the face's ramp numerators wrapped a 32-bit int (so the mapping is no longer
	 * affine), or {@link TriangleSink#textured} declined the submission itself.
	 * {@link TriangleSink#supportsTextures()} and
	 * {@link TriangleSink#textured} are the two questions, and the near plane is not one of
	 * them any more.
	 */
	public static final int NEEDS_TEXTURE = 4;

	/**
	 * The face has no usable colour. Either the model's per-corner lit colours are
	 * absent - nothing has run the lighting pass - or a corner code falls outside the
	 * palette, which {@code method481} cannot produce and so means these are not this
	 * client's lit colours at all.
	 */
	public static final int NO_COLOUR = 5;

	/**
	 * {@code anIntArray1637[face] == -1}: {@code method483} skips it before any test.
	 * It is not a triangle, so it is neither drawn nor culled.
	 */
	public static final int SKIPPED = 6;

	/**
	 * Render type 2 or 3: a textured triangle was submitted through
	 * {@link TriangleSink#textured} (Phase 7.2b-2e).
	 *
	 * <p>⚠ <b>A SEPARATE outcome from {@link #DRAWN}, and the separation is load-bearing
	 * for the tests rather than cosmetic.</b> {@link #DRAWN} means a coloured triangle
	 * arrived on {@link TriangleSink#triangle}, and the face-step oracle maps each
	 * {@code DRAWN} outcome back to a recorded coloured triangle in face order. A
	 * textured submission goes down a different method and carries a different payload,
	 * so folding it into {@link #DRAWN} would break that one-to-one mapping and
	 * invalidate the geometry checks rather than extend them.
	 *
	 * <p>{@link #allRepresentable()} treats it as representable, which is what makes it
	 * a distinct outcome rather than a flavour of {@link #NEEDS_TEXTURE}.
	 */
	public static final int TEXTURED = 7;

	/** Number of distinct outcomes; valid indices for {@link #outcomes}. */
	public static final int OUTCOME_COUNT = 8;

	/**
	 * Why the last {@link #emit} produced {@link #NEEDS_TEXTURE} - one of the
	 * {@code TEXTURE_*} constants below, or {@link #TEXTURE_OK} when it produced none.
	 *
	 * <p>⚠⚠ <b>This exists because {@link #declineReason()} used to be unable to say
	 * anything, and that cost a live run.</b> {@code NEEDS_TEXTURE} covers at least eight
	 * genuinely different failures - a model with no texture coordinates, an index outside
	 * the model's own arrays, a colour slot holding no id, ramp numerators that wrapped a
	 * 32-bit int, a sink with no atlas, and a sink that simply refused - and they call for
	 * completely different fixes. Reporting only the OUTCOME (as the first wiring step did)
	 * leaves a live client printing "a model face the GL path cannot represent (see
	 * GlFacePipeline)", which names the file and not the fault. The sub-reason is recorded
	 * at each {@code return} site, so it is the actual code path rather than a
	 * reconstruction.
	 */
	public static final int TEXTURE_OK = 0;
	/** The model's per-corner lit colours are absent, so a textured face has no shade. */
	public static final int TEXTURE_NO_COLOURS = 1;
	/** {@link TriangleSink#supportsTextures()} is false - no atlas, so nothing to sample. */
	public static final int TEXTURE_UNSUPPORTED = 2;
	/** {@code Model.hasTextures()} is false: the model carries no texture coordinates. */
	public static final int TEXTURE_NOT_TEXTURED = 3;
	/** The face's texture-coordinate index is outside the model's own texture arrays. */
	public static final int TEXTURE_BAD_COORDS = 4;
	/** The texture-coordinate entry names a vertex outside the model. */
	public static final int TEXTURE_BAD_VERTICES = 5;
	/** The face's colour slot holds no texture id ({@code < 0}). */
	public static final int TEXTURE_BAD_ID = 6;
	/** The ramp numerators wrapped a 32-bit int, so the mapping is no longer affine. */
	public static final int TEXTURE_RAMP_OVERFLOW = 7;
	/** {@link TriangleSink#textured} declined the submission itself. */
	public static final int TEXTURE_SINK_DECLINED = 8;
	/**
	 * A null from {@link #texturedRamps} that recorded no reason of its own - a catch-all,
	 * so the census can never claim a reason it did not observe.
	 */
	public static final int TEXTURE_UNKNOWN = 9;

	/** Number of distinct texture sub-reasons; valid indices for the name tables. */
	public static final int TEXTURE_REASON_COUNT = 10;

	private static final String[] OUTCOME_NAMES = { "DRAWN", "CULLED", "CLIPPED",
			"NEEDS_CLIPPING", "NEEDS_TEXTURE", "NO_COLOUR", "SKIPPED", "TEXTURED" };

	private static final String[] TEXTURE_DECLINE_NAMES = { "TEXTURE_OK", "TEXTURE_NO_COLOURS",
			"TEXTURE_UNSUPPORTED", "TEXTURE_NOT_TEXTURED", "TEXTURE_BAD_COORDS",
			"TEXTURE_BAD_VERTICES", "TEXTURE_BAD_ID", "TEXTURE_RAMP_OVERFLOW",
			"TEXTURE_SINK_DECLINED", "TEXTURE_UNKNOWN" };

	private static final String[] TEXTURE_DECLINE_DETAILS = { "no decline",
			"the model has no lit corner colours, so the face has no shade to carry",
			"the sink cannot sample textures (no atlas), so the face was not submitted",
			"the model carries no texture coordinates at all",
			"the face's texture-coordinate index is outside the model's own texture arrays",
			"the texture-coordinate entry names a vertex outside the model",
			"the face's colour slot holds no texture id",
			"the ramp numerators wrapped a 32-bit int, so the mapping is the software's wrap "
					+ "and is no longer affine",
			"the sink declined the submission - its texture id is not in the loaded cache, or "
					+ "the three wNum numerators are zero or cross zero inside the triangle",
			"the face could not be described for a reason the resolver did not record" };

	private static final String DETAIL_NEEDS_CLIPPING =
			"a clipped edge left the clipper's reciprocal table, which diverges in the "
					+ "software too (method443 aborts the model mid-draw there)";

	private static final String DETAIL_NO_COLOUR =
			"the model has no lit corner colours, or a corner code is outside the palette";

	/**
	 * The depth at each end of the {@code z} window {@link #depthToZ} maps onto
	 * {@code [0,1]}: the camera-space depth range the scene straddles.
	 *
	 * <p>⚠ Deliberately {@link WorldController#FAR_PLANE} rather than a new constant.
	 * {@code method443} already refuses to draw a model whose origin is at or past it,
	 * so it is the horizon this path has to agree with; a second number here would be a
	 * second thing to keep in step.
	 */
	private static final int DEPTH_SPAN = WorldController.FAR_PLANE;

	/** The depth above which {@code Texture.method374} applies fog. Mirrors its guard. */
	private static final int FOG_MIN_DEPTH = 50;

	/** Alpha byte the batcher receives. Blending is off, so it cannot change a pixel. */
	private static final int OPAQUE = 0xff000000;

	private static final int[] NO_VERTICES = new int[0];
	private static final boolean[] NO_FLAGS = new boolean[0];

	private int[] vertexX = NO_VERTICES;
	private int[] vertexY = NO_VERTICES;
	private int[] vertexDepth = NO_VERTICES;
	private int[] vertexCamX = NO_VERTICES;
	private int[] vertexCamY = NO_VERTICES;
	private int[] outcomes = NO_VERTICES;
	private boolean[] clipped = NO_FLAGS;

	/** The clipper's scratch: 3 or 4 points, screen space, depth and a colour CODE. */
	private final int[] clipX = new int[GlClipper.MAX_POINTS];
	private final int[] clipY = new int[GlClipper.MAX_POINTS];
	private final int[] clipDepth = new int[GlClipper.MAX_POINTS];
	private final int[] clipColour = new int[GlClipper.MAX_POINTS];

	private final int[] counts = new int[OUTCOME_COUNT];

	/**
	 * The FIRST texture sub-reason of the last {@link #emit}, or {@link #TEXTURE_OK}.
	 *
	 * <p>⚠ FIRST rather than last, matching {@code GlSceneRenderer.declineFrame}: the first
	 * unrepresentable thing is the one to fix, and a later, more common one would otherwise
	 * mask an earlier, rarer one.
	 */
	private int textureDecline;

	private int triangles;
	private int lastSceneDepth;
	private int lastVertexCount;
	private int lastFaceCount;

	/**
	 * The texture id resolved by the last {@link #texturedRamps} call.
	 *
	 * <p>⚠ <b>A scratch field rather than a second return value, and not by preference:</b>
	 * the whole reason this path exists is to stop allocating per frame, and returning the
	 * ramps and the id as one object would allocate twice per textured face instead of
	 * once. It is read immediately after the helper and never across a face, exactly like
	 * {@link #clipX} and friends.
	 */
	private int texturedId;

	/**
	 * Projects {@code model}, walks its faces exactly as {@code method483} does, and
	 * submits the representable ones to {@code sink}.
	 *
	 * <p>Every face gets an outcome, whether or not it was submitted, and the outcome
	 * is what {@link #allRepresentable()} and the harness read. Nothing is drawn for a
	 * face whose outcome is anything but {@link #DRAWN}, and nothing throws: a model
	 * with unusable arrays yields {@code 0} and leaves the census empty.
	 *
	 * <p>The arguments mirror {@link Model#method443}, so a caller that already has them
	 * - the scene seam does - passes them straight through.
	 *
	 * @param orientation the model's rotation index; 0 means "no rotation applied"
	 * @param camA..camD  the current camera sin/cos, {@code WorldController.anInt458..461}
	 * @param dx,dy,dz    the model's position relative to the camera origin
	 * @param centreX     the screen centre X, {@code Texture.textureInt1}
	 * @param centreY     the screen centre Y, {@code Texture.textureInt2}
	 * @param sink        where triangles go; must be on the game thread
	 * @return the number of triangles submitted
	 */
	public int emit(Model model, int orientation, int camA, int camB, int camC, int camD,
			int dx, int dy, int dz, int centreX, int centreY, TriangleSink sink) {
		triangles = 0;
		lastVertexCount = 0;
		lastFaceCount = 0;
		textureDecline = TEXTURE_OK;
		for (int i = 0; i < OUTCOME_COUNT; i++) {
			counts[i] = 0;
		}
		textureDecline = TEXTURE_OK;
		if (model == null || sink == null) {
			return 0;
		}
		int[] faceA = model.faceVertexA();
		int[] faceB = model.faceVertexB();
		int[] faceC = model.faceVertexC();
		int vertices = model.vertexCount();
		int faces = model.faceCount();
		if (faceA == null || faceB == null || faceC == null || vertices <= 0 || faces <= 0) {
			return 0;
		}
		ensureCapacity(vertices, faces);
		lastVertexCount = vertices;
		lastFaceCount = faces;
		java.util.Arrays.fill(clipped, 0, faces, false);

		GlModelProjection.project(model, orientation, camA, camB, camC, camD, dx, dy, dz,
				centreX, centreY, vertexX, vertexY, vertexDepth, vertexCamX, vertexCamY);
		// method443 sets Fog.sceneDepth to this for the duration of the draw, and the
		// software rasterisers read it per face - so it is part of the colour contract,
		// not a convenience. Reproduced by computing the same value rather than by
		// reading the static, which belongs to whoever is drawing.
		lastSceneDepth = GlModelProjection.modelDepth(camA, camB, camC, camD, dx, dy, dz);

		int[] renderTypes = model.faceRenderTypes();
		int[] colourA = model.faceCornerColoursA();
		int[] colourB = model.faceCornerColoursB();
		int[] colourC = model.faceCornerColoursC();
		boolean lit = colourA != null && colourB != null && colourC != null;

		for (int face = 0; face < faces; face++) {
			int a = faceA[face];
			int b = faceB[face];
			int c = faceC[face];
			if (a < 0 || a >= vertices || b < 0 || b >= vertices || c < 0 || c >= vertices) {
				counts[SKIPPED]++;
				outcomes[face] = SKIPPED;
				continue;
			}
			int xa = vertexX[a];
			int xb = vertexX[b];
			int xc = vertexX[c];
			int outcome;
			if (renderTypes != null && renderTypes[face] == -1) {
				// method483's very first test: not a triangle at all.
				outcome = SKIPPED;
			} else if (xa == GlModelProjection.OFFSCREEN
					|| xb == GlModelProjection.OFFSCREEN
					|| xc == GlModelProjection.OFFSCREEN) {
				// ⚠ ORDER IS CONTRACT. method483 tests the near plane BEFORE it tests
				// the winding, and a face with an offscreen vertex is bucketed WITHOUT
				// ever being backface-culled - method485 tests the winding of the
				// REBUILT polygon instead, later and on different coordinates. Testing
				// the winding first would silently drop faces the software draws.
				//
				// ⚠ clipped[] is set from the near-plane test alone, never from the
				// outcome: a cut face can come out DRAWN, CULLED-equivalent,
				// NEEDS_TEXTURE or NO_COLOUR, and the harness's clip oracle compares
				// this flag against the software's own aBooleanArray1664.
				clipped[face] = true;
				outcome = clipFace(model, face, renderTypes, colourA, colourB, colourC, lit,
						centreX, centreY, sink);
			} else if (!isFrontFacing(xa, vertexY[a], xb, vertexY[b], xc, vertexY[c])) {
				outcome = CULLED;
			} else if (renderTypes != null && (renderTypes[face] & 2) != 0) {
				outcome = submitTextured(model, face, renderTypes, colourA, colourB, colourC,
						lit, a, b, c, centreX, centreY, sink);
			} else if (!lit) {
				outcome = NO_COLOUR;
			} else if ((renderTypes == null ? 0 : renderTypes[face] & 1) != 0) {
				// ⚠ RENDER TYPE 1 IS ONE FLAT COLOUR, NOT A GOURAUD TRIANGLE, and the
				// reason lies in how the model was LIT rather than in this pipeline:
				// method479's flat path writes anIntArray1634[i2] and leaves 1635/1636
				// UNTOUCHED, which is exactly why method484's type-1 case reads
				// anIntArray1634 alone and hands method376 a single colour for the whole
				// face. Resolving all three corners here - as the branch below
				// legitimately does for type 0, where method480 writes all three - would
				// read two slots the software never wrote; on a freshly allocated model
				// those slots are ZERO, i.e. palette[0], so the face would come out with
				// two dark corners that the software never draws.
				outcome = DRAWN;
				int flat = resolveFlatColour(colourA[face], lastSceneDepth);
				if (flat < 0) {
					outcome = NO_COLOUR;
				} else {
					sink.triangle(vertexX[a], vertexY[a], absoluteZ(vertexDepth[a]), OPAQUE | flat,
							vertexX[b], vertexY[b], absoluteZ(vertexDepth[b]), OPAQUE | flat,
							vertexX[c], vertexY[c], absoluteZ(vertexDepth[c]), OPAQUE | flat);
					triangles++;
				}
			} else {
				outcome = DRAWN;
				int ca = resolveCornerColour(colourA[face], lastSceneDepth);
				int cb = resolveCornerColour(colourB[face], lastSceneDepth);
				int cc = resolveCornerColour(colourC[face], lastSceneDepth);
				if (ca < 0 || cb < 0 || cc < 0) {
					outcome = NO_COLOUR;
				} else {
					// Face-index order, deliberately: see the class doc for why this
					// reproduces the software's tie-breaking rather than fighting it.
					// ⚠ The opaque alpha is added HERE, not by resolveCornerColour:
					// that method's return value is compared against a pixel the
					// software path painted, and those are 0x00RRGGBB. The batcher's
					// input is ARGB and blending is off, so the byte is inert either
					// way - but it is stated rather than left to chance.
					sink.triangle(vertexX[a], vertexY[a], absoluteZ(vertexDepth[a]), OPAQUE | ca,
							vertexX[b], vertexY[b], absoluteZ(vertexDepth[b]), OPAQUE | cb,
							vertexX[c], vertexY[c], absoluteZ(vertexDepth[c]), OPAQUE | cc);
					triangles++;
				}
			}
			outcomes[face] = outcome;
			counts[outcome]++;
		}
		return triangles;
	}

	/**
	 * The clipped branch: rebuild the face with {@link GlClipper}, then draw it the way
	 * {@code method485} would.
	 *
	 * <p>⚠ <b>Everything here mirrors {@code method485}'s tail, including what it does NOT
	 * do.</b> The winding test is on the REBUILT polygon's first three points - never on the
	 * original projected ones, which may not even exist - it gates BOTH triangles of a quad,
	 * and a polygon of fewer than three points draws nothing at all. Those three are easy
	 * to "tidy" into something more sensible and wrong.
	 */
	private int clipFace(Model model, int face, int[] renderTypes, int[] colourA,
			int[] colourB, int[] colourC, boolean lit,
			int centreX, int centreY, TriangleSink sink) {
		if (!lit) {
			// method485 reads the corner-colour arrays unconditionally, so with none there is
			// nothing to rebuild the colours from - and the uncut path reports the same
			// outcome for the same model, which is the point of reporting it here too.
			return NO_COLOUR;
		}
		int points = GlClipper.clip(model, face, vertexX, vertexY, vertexCamX, vertexCamY,
				vertexDepth, lastSceneDepth, centreX, centreY,
				clipX, clipY, clipDepth, clipColour);
		if (points == GlClipper.UNUSABLE) {
			return NEEDS_CLIPPING;
		}
		if (points < 3) {
			// Wholly behind the plane: the software's polygon is empty and it draws nothing.
			// Still CLIPPED rather than CULLED, because method483 had already BUCKETED this
			// face - the software's own record says "kept", and the harness compares that.
			return CLIPPED;
		}
		if (!isFrontFacing(clipX[0], clipY[0], clipX[1], clipY[1], clipX[2], clipY[2])) {
			return CLIPPED;
		}
		int type = renderTypes == null ? 0 : renderTypes[face] & 3;
		if ((type & 2) != 0) {
			// ⚠⚠⚠ PHASE 7.2b-2n: A CUT TEXTURED FACE IS NOW DRAWN, NOT DECLINED. Until this
			// step it returned NEEDS_TEXTURE, and that was the COMMON decline rather than a
			// pathological one - the player's own model and anything you walk up to carry a
			// textured face across the near plane - so a whole-frame accept/decline latch
			// would have handed those frames back to the software in exactly the situation
			// that matters.
			//
			// ⚠ The ramps come from the MODEL, not from the clipped polygon, and that is the
			// finding this branch rests on: method485 hands method378 the SAME nine for both
			// triangles of the quad, and method378's ramps depend only on the nine and the
			// origin. So the mapping here is IDENTICAL to the uncut case, and only the screen
			// triangles and the shades differ. See GlFacePipeline#texturedRamps.
			if (!sink.supportsTextures()) {
				return markTextureDecline(TEXTURE_UNSUPPORTED);
			}
			TextureRamps ramps = texturedRamps(model, face, centreX, centreY);
			if (ramps == null) {
				// texturedRamps recorded its own reason; TEXTURE_UNKNOWN only stands if it
				// somehow returned null without one, so the census cannot claim more than it saw.
				return markTextureDecline(TEXTURE_UNKNOWN);
			}
			int textureId = texturedId;
			if (type == 3) {
				// method485's render-type-3 branch passes anIntArray1634[i] three times - the
				// RAW slot A, NOT the interpolated anIntArray1680 - which is the same flat
				// treatment the uncut type-3 path gets, and for the same reason.
				int flat = colourA[face];
				if (!submitClippedTextured(0, 1, 2, flat, flat, flat, ramps, textureId, sink)) {
					return markTextureDecline(TEXTURE_SINK_DECLINED);
				}
				if (points == 4 && !submitClippedTextured(0, 2, 3, flat, flat, flat, ramps,
						textureId, sink)) {
					return markTextureDecline(TEXTURE_SINK_DECLINED);
				}
			} else {
				// method485's render-type-2 branch passes anIntArray1680[0..2] for the first
				// triangle and [0],[2],[3] for the second - the INTERPOLATED clip shades, which
				// is exactly what GlClipper writes into clipColour.
				if (!submitClippedTextured(0, 1, 2, clipColour[0], clipColour[1], clipColour[2],
						ramps, textureId, sink)) {
					return markTextureDecline(TEXTURE_SINK_DECLINED);
				}
				if (points == 4 && !submitClippedTextured(0, 2, 3, clipColour[0], clipColour[2],
						clipColour[3], ramps, textureId, sink)) {
					return markTextureDecline(TEXTURE_SINK_DECLINED);
				}
			}
			return CLIPPED;
		}
		if (type == 1) {
			// ⚠ The FLAT colour from the face's ORIGINAL slot A - not the interpolated clip
			// colours. method485's type-1 branch hands both triangles
			// modelIntArray3[anIntArray1634[i]] and never reads anIntArray1680 at all.
			int flat = resolveFlatColour(colourA[face], lastSceneDepth);
			if (flat < 0) {
				return NO_COLOUR;
			}
			submitClipped(0, 1, 2, flat, flat, flat, sink);
			if (points == 4) {
				submitClipped(0, 2, 3, flat, flat, flat, sink);
			}
			return CLIPPED;
		}
		int k0 = resolveCornerColour(clipColour[0], lastSceneDepth);
		int k1 = resolveCornerColour(clipColour[1], lastSceneDepth);
		int k2 = resolveCornerColour(clipColour[2], lastSceneDepth);
		int k3 = points == 4 ? resolveCornerColour(clipColour[3], lastSceneDepth) : 0;
		if (k0 < 0 || k1 < 0 || k2 < 0 || (points == 4 && k3 < 0)) {
			return NO_COLOUR;
		}
		// method485's quad split, in its order: (0,1,2) then (0,2,3), colours following.
		submitClipped(0, 1, 2, k0, k1, k2, sink);
		if (points == 4) {
			submitClipped(0, 2, 3, k0, k2, k3, sink);
		}
		return CLIPPED;
	}

	/**
	 * One triangle of a clipped polygon. Each point carries its OWN depth, because a
	 * clipped polygon mixes front vertices (which keep their projected depth) with
	 * intersections sitting on the near plane - one shared depth would flatten the front
	 * corners onto the plane and lose the depth ordering between clipped faces.
	 */
	private void submitClipped(int i0, int i1, int i2, int c0, int c1, int c2, TriangleSink sink) {
		sink.triangle(clipX[i0], clipY[i0], absoluteZ(clipDepth[i0]), OPAQUE | c0,
				clipX[i1], clipY[i1], absoluteZ(clipDepth[i1]), OPAQUE | c1,
				clipX[i2], clipY[i2], absoluteZ(clipDepth[i2]), OPAQUE | c2);
		triangles++;
	}

	/**
	 * One TEXTURED triangle of a clipped polygon (Phase 7.2b-2n).
	 *
	 * <p>⚠ <b>The ramps are the UNCUT face's, passed in rather than rebuilt here, and that
	 * is the substance of the step rather than an optimisation.</b> {@code method485}'s
	 * textured branches hand {@code method378} the same nine for both clipped triangles, so
	 * the mapping is the face's and not the polygon's; the polygon only says WHERE to
	 * rasterise it and which shades to use. Rebuilding the ramps from the clipped points
	 * would produce a different, wrong mapping that would look plausible.
	 *
	 * <p>⚠ <b>The shades are the CLIP shades, not the corner shades</b> - except for render
	 * type 3, whose caller passes slot A three times because {@code method485}'s type-3
	 * branch does. Each point carries its OWN depth for the same reason
	 * {@link #submitClipped} does: a clipped polygon mixes front vertices with
	 * intersections sitting on the near plane.
	 */
	private boolean submitClippedTextured(int i0, int i1, int i2, int s0, int s1, int s2,
			TextureRamps ramps, int textureId, TriangleSink sink) {
		int[] n0 = ramps.attributeAt(clipX[i0], clipY[i0]);
		int[] n1 = ramps.attributeAt(clipX[i1], clipY[i1]);
		int[] n2 = ramps.attributeAt(clipX[i2], clipY[i2]);
		boolean accepted = sink.textured(
				clipX[i0], clipY[i0], absoluteZ(clipDepth[i0]), n0[0], n0[1], n0[2], s0,
				clipX[i1], clipY[i1], absoluteZ(clipDepth[i1]), n1[0], n1[1], n1[2], s1,
				clipX[i2], clipY[i2], absoluteZ(clipDepth[i2]), n2[0], n2[1], n2[2], s2,
				textureId);
		if (!accepted) {
			return false;
		}
		triangles++;
		return true;
	}

	/**
	 * The textured branch: render type 2 or 3, resolved into everything the software
	 * {@code Texture.method378} is handed (Phase 7.2b-2e).
	 *
	 * <p><b>What is resolved, and from where - all of it from the software's own
	 * conventions rather than from a guess:</b>
	 * <ul>
	 *   <li><b>The texture id</b> comes from the face's COLOUR slot, not from the render
	 *       type word ({@link Model#faceTextureId}). For a textured face that slot holds
	 *       no colour at all, which is why it must never be rendered as one.</li>
	 *   <li><b>The nine camera-space values</b> are read at the vertex indices
	 *       {@code renderType >> 2} selects ({@link Model#faceTextureIndex}) - and those
	 *       indices are DIFFERENT vertex indices from the face's own corners, which is the
	 *       whole point of the indirection. {@code w} is recovered as
	 *       {@code depth + sceneDepth} rather than from a third projected array, exactly as
	 *       {@link GlClipper} does, so the two cannot drift. They become the three ramp
	 *       NUMERATORS per triangle vertex - see {@link #texturedRamps} for why that is the
	 *       form the sink must be handed rather than the triple.</li>
	 *   <li><b>The shade codes</b> are the raw per-corner values, with render type 3
	 *       taking slot A three times because {@code method484}'s type-3 branch never
	 *       reads 1635/1636 - the same flat/gouraud distinction the untextured path
	 *       makes, and for the same reason ({@code method479} writes only slot A for the
	 *       odd render types).</li>
	 * </ul>
	 *
	 * <p>⚠ <b>The texture-coordinate lookup is bounds-checked here, and the texture ID
	 * deliberately is NOT.</b> The index into {@code anIntArray1643/1644/1645} is the one
	 * this class can validate - it must land inside the arrays and name real vertices -
	 * and an unusable one means the face cannot be described, so it is
	 * {@link #NEEDS_TEXTURE}. The texture ID is a GLOBAL cache index, not a property of
	 * the model: only the sink that owns the texture storage can say whether a given id
	 * can be sampled, so the id is passed through and the SINK is the thing that
	 * declines. Bounding it here against the model's own texture count would be a
	 * plausible-looking check that rejects valid faces.
	 *
	 * @return {@link #TEXTURED} if the sink accepted, {@link #NEEDS_TEXTURE} otherwise
	 */
	private int submitTextured(Model model, int face, int[] renderTypes, int[] colourA,
			int[] colourB, int[] colourC, boolean lit, int a, int b, int c, int centreX,
			int centreY, TriangleSink sink) {
		if (!lit) {
			return markTextureDecline(TEXTURE_NO_COLOURS);
		}
		if (!sink.supportsTextures()) {
			return markTextureDecline(TEXTURE_UNSUPPORTED);
		}
		TextureRamps ramps = texturedRamps(model, face, centreX, centreY);
		if (ramps == null) {
			// texturedRamps recorded its own reason; see the same note in clipFace.
			return markTextureDecline(TEXTURE_UNKNOWN);
		}
		int textureId = texturedId;
		// Type 3 passes slot A three times - method484's type-3 branch reads no other slot.
		int shadeA = colourA[face];
		int shadeB = (renderTypes[face] & 3) == 3 ? shadeA : colourB[face];
		int shadeC = (renderTypes[face] & 3) == 3 ? shadeA : colourC[face];
		// The ramps are anchored at the screen ORIGIN and evaluated at the FACE's own
		// projected vertices - the two being different vertices is fine, and is the point:
		// the mapping is a function of screen position, and the triangle only bounds where
		// it is rasterised.
		int[] na = ramps.attributeAt(vertexX[a], vertexY[a]);
		int[] nb = ramps.attributeAt(vertexX[b], vertexY[b]);
		int[] nc = ramps.attributeAt(vertexX[c], vertexY[c]);
		boolean accepted = sink.textured(
				vertexX[a], vertexY[a], absoluteZ(vertexDepth[a]), na[0], na[1], na[2], shadeA,
				vertexX[b], vertexY[b], absoluteZ(vertexDepth[b]), nb[0], nb[1], nb[2], shadeB,
				vertexX[c], vertexY[c], absoluteZ(vertexDepth[c]), nc[0], nc[1], nc[2], shadeC,
				textureId);
		if (!accepted) {
			return markTextureDecline(TEXTURE_SINK_DECLINED);
		}
		triangles++;
		return TEXTURED;
	}

	/**
	 * The ramp form for a textured face, or {@code null} when the face cannot be described.
	 * On success the resolved texture id is left in {@link #texturedId}.
	 *
	 * <p>⚠⚠ <b>SHARED BY THE UNCUT AND THE CLIPPED TEXTURED PATHS, AND THAT SHARING IS THE
	 * WHOLE CONTENT OF PHASE 7.2b-2n RATHER THAN A TIDINESS.</b> {@code method485}'s
	 * textured branches hand {@code method378} the <b>same nine</b> for both triangles of a
	 * clipped quad -
	 * {@code anIntArray1668/1669/1670[anIntArray1643/1644/1645[renderType >> 2]]}, the
	 * model's texture-coordinate vertices, unchanged. And {@code method378} builds its ramps
	 * (the {@code << 14}/{@code << 8}/{@code << 5} minors) from <b>the nine and the screen
	 * origin alone</b>: its screen arguments are used only to walk spans. So the mapping
	 * attached to a textured face is a property of the MODEL and the ORIGIN, and is
	 * <b>completely independent of the screen triangle</b>. Two consequences, and the second
	 * is what makes the clipped case cheap:
	 * <ol>
	 *   <li>Clipping changes WHICH SCREEN REGION is rasterised and the INTERPOLATED SHADES.
	 *       It never changes the mapping.</li>
	 *   <li>So a clipped textured face needs no texture data carried through the clipper at
	 *       all - and a caller that rebuilt the ramps from the clipped polygon would get a
	 *       DIFFERENT, wrong mapping rather than an equivalent one. Building them once here
	 *       is what stops the two paths from drifting apart.</li>
	 * </ol>
	 *
	 * <p>⚠ The nine are read at the TEXTURE-COORDINATE vertices ({@code anIntArray1643/1644/1645}),
	 * which are NOT the face's corners - the indirection 7.2k measured. The shades and the
	 * screen triangle come from the corners; the mapping does not. Keeping that split is the
	 * point, so this method deliberately does not take the corners at all.
	 *
	 * @return the ramps, or {@code null} if the model has no usable textured face here
	 */
	private TextureRamps texturedRamps(Model model, int face, int centreX, int centreY) {
		if (!model.hasTextures()) {
			noteTextureDecline(TEXTURE_NOT_TEXTURED);
			return null;
		}
		int index = model.faceTextureIndex(face);
		int[] textureVertexA = model.textureVertexA();
		int[] textureVertexB = model.textureVertexB();
		int[] textureVertexC = model.textureVertexC();
		if (index < 0 || index >= textureVertexA.length
				|| index >= textureVertexB.length || index >= textureVertexC.length) {
			noteTextureDecline(TEXTURE_BAD_COORDS);
			return null;
		}
		int vertices = model.vertexCount();
		int ta = textureVertexA[index];
		int tb = textureVertexB[index];
		int tc = textureVertexC[index];
		if (ta < 0 || ta >= vertices || tb < 0 || tb >= vertices || tc < 0 || tc >= vertices) {
			noteTextureDecline(TEXTURE_BAD_VERTICES);
			return null;
		}
		int textureId = model.faceTextureId(face);
		if (textureId < 0) {
			noteTextureDecline(TEXTURE_BAD_ID);
			return null;
		}
		// ⚠ The nine, in method378's own order: the u plane, the v plane, then the w plane,
		// read at (ta, tb, tc). w is recovered as depth + sceneDepth rather than from a third
		// projected array, exactly as GlClipper does, so the two cannot drift.
		int wa = vertexDepth[ta] + lastSceneDepth;
		int wb = vertexDepth[tb] + lastSceneDepth;
		int wc = vertexDepth[tc] + lastSceneDepth;
		int size = GlTextures.layerSize();
		TextureRamps ramps = TextureRamps.of(vertexCamX[ta], vertexCamX[tb], vertexCamX[tc],
				vertexCamY[ta], vertexCamY[tb], vertexCamY[tc], wa, wb, wc, centreX, centreY,
				TextureRamps.denShiftFor(size), TextureRamps.colShiftFor(size), size);
		if (ramps.overflows()) {
			// ⚠ The ramps wrapped a 32-bit int, so the mapping is the software's WRAP and is
			// no longer affine - a shader cannot reproduce it by interpolating numerators.
			// Declining is the honest answer; the frame falls back to the software path, which
			// reproduces the wrap exactly.
			noteTextureDecline(TEXTURE_RAMP_OVERFLOW);
			return null;
		}
		texturedId = textureId;
		return ramps;
	}

	/**
	 * {@code method483}'s winding test, exactly: the signed area of the projected
	 * triangle. The bucket write - and therefore every triangle the software draws -
	 * sits inside {@code if (area > 0)}, so a POSITIVE area means front-facing and is
	 * drawn; zero or negative is culled.
	 *
	 * <p>⚠ <b>The vertex roles are not symmetric and the sign is not arbitrary.</b>
	 * {@code method483} computes
	 * {@code (xA - xB) * (yC - yB) - (yA - yB) * (xC - xB) > 0} - A and B on the
	 * first factor, C and B on the second - and reversing the sign inverts which side
	 * of every polygon in the world you see. It is integer arithmetic on screen
	 * coordinates, including whatever overflow those coordinates cause, because the
	 * software path has the same overflow and the point is to agree with it.
	 *
	 * <p>⚠ The screen's y axis points DOWN in this client, which is why the sign looks
	 * inverted against the usual mathematical convention. Verified against the
	 * rasteriser: a face with area {@code -55287} was culled and one with {@code +1430}
	 * survived.
	 *
	 * @param xA,yA first projected vertex
	 * @param xB,yB second projected vertex
	 * @param xC,yC third projected vertex
	 * @return {@code true} if the face is front-facing and must be drawn
	 */
	public static boolean isFrontFacing(int xA, int yA, int xB, int yB, int xC, int yC) {
		return (xA - xB) * (yC - yB) - (yA - yB) * (xC - xB) > 0;
	}

	/**
	 * Maps a {@code method443} camera-space depth to the {@code [0,1]} {@code z} the
	 * batcher expects, {@code 0} being nearest.
	 *
	 * <p>⚠⚠ <b>The {@code depth} this takes must be ABSOLUTE - distance from the camera
	 * origin - and callers holding a value relative to a model's origin must go through
	 * {@link #absoluteZ} instead.</b> {@code method443} stores the RELATIVE form
	 * ({@code anIntArray1667[v] = camZ - k2}) because the software buckets faces within one
	 * model; a shared GL depth buffer needs the absolute one, or every model would sit at
	 * {@code z = 0.5} at its own origin and two models at different distances would become
	 * incomparable. See {@link #absoluteZ}.
	 *
	 * <p>The window is {@code +/- }{@link #DEPTH_SPAN} about zero. An absolute camera depth
	 * is never negative, so in practice this uses the upper half of the range; that is a
	 * deliberate rounding-free choice rather than an oversight, and it stays monotone, which
	 * is the property the depth test actually needs.
	 *
	 * @param depth an ABSOLUTE camera-space depth; see {@link #absoluteZ} for the relative form
	 * @return the {@code z} to hand the batcher, in {@code [0,1]}
	 */
	public static float depthToZ(int depth) {
		float z = 0.5f + (float) depth / (2f * DEPTH_SPAN);
		if (z < 0f) {
			return 0f;
		}
		if (z > 1f) {
			return 1f;
		}
		return z;
	}

	/**
	 * {@link #depthToZ} for a depth carried RELATIVE to the model's origin - i.e. the
	 * {@code vertexDepth}/{@code clipDepth} this class and {@link GlClipper} work in.
	 *
	 * <p>⚠ <b>Why this exists rather than a bare {@code depthToZ} call, and it is a
	 * correction rather than a wrapper.</b> {@code method443} writes
	 * {@code anIntArray1667[v] = camZ - k2}, and {@code k2} DIFFERS PER MODEL - so the raw
	 * value cannot be the depth a shared buffer compares. Adding {@code k2} back (that is
	 * {@link #lastSceneDepth}, the value {@code method443} also assigns to
	 * {@code Fog.sceneDepth}) recovers {@code camZ}, which is monotone in real distance and is
	 * the same quantity the ground seam carries, so ground and models land on one axis.
	 *
	 * <p>⚠ The projection is deliberately NOT the place this is fixed: {@link GlModelProjection}
	 * mirrors {@code anIntArray1667} exactly and {@link GlClipper} consumes that same relative
	 * form, so making the projected array absolute would break the clipper and the oracle that
	 * pins the projection to the software.
	 */
	private float absoluteZ(int relativeDepth) {
		return depthToZ(relativeDepth + lastSceneDepth);
	}


	/**
	 * The colour a 16-bit model colour code becomes on screen, or {@code -1} if the
	 * code is outside the palette.
	 *
	 * <p>This is the whole colour contract in one place and it has two steps, both taken
	 * from the software rasteriser rather than invented here: fog first
	 * ({@code Texture.method374}'s {@code Fog.fadeHsl(colour, Fog.sceneDepth)}, which is
	 * what {@code method443} sets {@code Fog.sceneDepth} to), then a lookup in
	 * {@link Texture#anIntArray1482}. The palette is not an RGB table - it is the
	 * 65536-entry HSL code space itself, indexed as
	 * {@code (hue << 10) | (sat << 7) | lum}, which is exactly the space
	 * {@code method481} produces codes in.
	 *
	 * <p>{@code -1} is returned rather than a substitute colour because a code outside
	 * the palette is not a shade this client can draw - it means the caller's colours
	 * did not come from a lighting pass - and painting something for it would hide that.
	 *
	 * @param code        the 16-bit model colour code
	 * @param sceneDepth  the model's depth, {@code method443}'s {@code k2}
	 * @return the packed {@code 0x00RRGGBB} colour, or {@code -1} if unusable
	 */
	public static int resolveCornerColour(int code, int sceneDepth) {
		if (sceneDepth > FOG_MIN_DEPTH) {
			code = Fog.fadeHsl(code, sceneDepth);
		}
		int[] palette = Texture.anIntArray1482;
		if (palette == null || code < 0 || code >= palette.length) {
			return -1;
		}
		return palette[code];
	}

	/**
	 * The colour the software paints a render-type-1 face with.
	 *
	 * <p>⚠ <b>Same inputs as {@link #resolveCornerColour}, DIFFERENT ORDER, and the order
	 * is the whole point.</b> {@code Texture.method374} (type 0) fades the colour CODE
	 * first and lets the rasteriser look the palette up per interpolated code;
	 * {@code Texture.method376} (type 1) looks the palette up first and then fades the
	 * resulting RGB - {@code Fog.applyFlat(k1)} on a value that is already a colour. With
	 * fog off the two coincide, which is precisely why the difference survives a casual
	 * reading; the oracle for this runs with fog ON for that reason.
	 *
	 * <p>⚠ <b>The depth is passed in rather than read from {@code Fog.sceneDepth}, and it
	 * has to be:</b> the seam runs before {@code Model.java:2346} assigns that field, so
	 * the field still holds the previous model's depth. See {@link Fog#applyFlatAt}.
	 */
	public static int resolveFlatColour(int code, int sceneDepth) {
		int[] palette = Texture.anIntArray1482;
		if (palette == null || code < 0 || code >= palette.length) {
			return -1;
		}
		// Mirrors method376, which calls Fog.applyFlat on the ALREADY PALETTE-RESOLVED
		// colour - so the palette lookup comes first and the fade second, the reverse of
		// resolveCornerColour above.
		return Fog.applyFlatAt(palette[code], sceneDepth);
	}

	/**
	 * The outcome recorded for each face of the last model, index-aligned with
	 * {@code Model.faceVertexA()}.
	 *
	 * <p>⚠ <b>Live storage, valid only until the next {@link #emit}.</b> One int per
	 * face instead of a per-model copy, because the whole reason this path exists is to
	 * stop allocating per frame; and per-instance rather than {@code static}, because
	 * 5.1's finding is that shared static draw scratch is what made the software path's
	 * own arrays unusable. Read what you need before emitting again.
	 *
	 * @return the outcomes array, or an empty array if nothing has been emitted
	 */
	public int[] outcomes() {
		return outcomes;
	}

	/**
	 * Whether {@code method483} routed each face of the last model to {@code method485} -
	 * i.e. a vertex of it sat at or behind the near plane. Index-aligned with
	 * {@code Model.faceVertexA()}, like {@link #outcomes()}.
	 *
	 * <p>⚠ <b>This is a DIFFERENT question from the outcome, and the harness needs both.</b>
	 * The software's own flag for it is {@code aBooleanArray1664}, set in {@code method483}
	 * the moment the near-plane test fires - before the clipper has run and before anything
	 * is known about the render type. Deriving "was it cut" from the outcome would make the
	 * oracle agree for the wrong reason: a cut face can report {@link #CLIPPED},
	 * {@link #NO_COLOUR} or {@link #NEEDS_TEXTURE}, and none of those is evidence about the
	 * near plane on its own.
	 *
	 * @return the flags, or an empty array if nothing has been emitted
	 */
	public boolean[] clipped() {
		return clipped;
	}

	/** How many faces of the last model got {@code outcome}. */
	public int count(int outcome) {
		return outcome >= 0 && outcome < OUTCOME_COUNT ? counts[outcome] : 0;
	}

	/** Triangles submitted by the last {@link #emit}. */
	public int triangles() {
		return triangles;
	}

	/** Faces of the last model, i.e. the valid length of {@link #outcomes()}. */
	public int faceCount() {
		return lastFaceCount;
	}

	/** Vertices of the last model. */
	public int vertexCount() {
		return lastVertexCount;
	}

	/** The scene depth, {@code method443}'s {@code k2}, the last {@link #emit} used. */
	public int sceneDepth() {
		return lastSceneDepth;
	}

	/**
	 * Whether every face of the last model could be represented - i.e. every face was
	 * drawn, textured, culled or skipped, and none needed a texture the sink could not
	 * sample or a clip the clipper could not perform.
	 *
	 * <p>⚠ <b>This is the question the 7.2b-2 wiring step has to ask.</b> The scene must go
	 * over WHOLE or not at all, because the software path interleaves ground and models per
	 * tile and a partial takeover has no correct merge order - so a frame accumulates this
	 * across every model before it commits. ⚠ <b>As of 7.2b-2n what remains is genuinely
	 * rare and not the common case it once was:</b> an id missing from the loaded cache, ramp
	 * numerators that wrapped a 32-bit int, a clipped edge outside the clipper's reciprocal
	 * table, or a colour code outside the palette. A CUT TEXTURED face - once the single
	 * most common reason a gameplay frame would have been refused - is now represented.
	 * ⚠ {@link #CLIPPED} deliberately does NOT appear here: a clipped face is representable,
	 * however it came out.
	 *
	 * <p>⚠ <b>A {@code false} from here is only half an answer, and a caller that stops at
	 * it is the reason 7.4's first run could not name its blocker:</b> ask
	 * {@link #declineReason()} for WHICH outcome, which face, and - for a textured face -
	 * which of the eight sub-cases. This method is the cheap test that decides whether that
	 * is worth building the sentence for.
	 */
	public boolean allRepresentable() {
		// ⚠ Expressed through isUnrepresentable() rather than as three inline comparisons, so
		// this predicate and declineReason() cannot disagree about what "unrepresentable"
		// means - a disagreement would make the frame-decline path report a reason for a
		// frame that was actually whole, or report none for one that was not. The loop is
		// OUTCOME_COUNT iterations over ints and allocates nothing, which is what lets it sit
		// on the per-model path.
		for (int outcome = 0; outcome < OUTCOME_COUNT; outcome++) {
			if (isUnrepresentable(outcome) && counts[outcome] != 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Whether an outcome is one that stops the whole frame going to GL.
	 *
	 * <p>⚠ <b>{@link #CLIPPED} is deliberately not one, and that is the distinction the
	 * early steps turned on:</b> a clipped face is representable however it came out (the
	 * clipper legitimately submits nothing for a face that is wholly behind the near plane
	 * or comes out back-facing, and the software draws nothing there either). Ditto
	 * {@link #CULLED}, {@link #SKIPPED} and {@link #TEXTURED}.
	 *
	 * @param outcome one of the {@code OUTCOME_*} constants
	 * @return {@code true} if a frame containing this outcome must stay in software
	 */
	public static boolean isUnrepresentable(int outcome) {
		return outcome == NEEDS_CLIPPING || outcome == NEEDS_TEXTURE || outcome == NO_COLOUR;
	}

	/** The name of an outcome, for a log line or a census. Never {@code null}. */
	public static String outcomeName(int outcome) {
		return outcome >= 0 && outcome < OUTCOME_COUNT ? OUTCOME_NAMES[outcome] : "UNKNOWN";
	}

	/** The name of a {@code TEXTURE_*} sub-reason, for a log line or a census. */
	public static String textureDeclineName(int reason) {
		return reason >= 0 && reason < TEXTURE_REASON_COUNT
				? TEXTURE_DECLINE_NAMES[reason] : "UNKNOWN";
	}

	/**
	 * The FIRST texture sub-reason of the last {@link #emit}, or {@link #TEXTURE_OK}.
	 *
	 * <p>Exposed because {@code NEEDS_TEXTURE} alone is not actionable: see the
	 * {@code TEXTURE_*} constants for why the eight cases behind it need different fixes.
	 */
	public int textureDecline() {
		return textureDecline;
	}

	/**
	 * WHY the last {@link #emit} is not fully representable, or {@code null} if it is.
	 *
	 * <p>⚠⚠ <b>This is the answer a live run needs, and its absence is what made the first
	 * GL gate run end at "something declined".</b> {@link #allRepresentable()} says only
	 * THAT the frame is not whole, and the eight sub-cases behind {@link #NEEDS_TEXTURE}
	 * alone call for completely different fixes - a missing atlas, a wrapped ramp, a
	 * texture id that is not in the loaded cache - so the outcome alone is a filename, not a
	 * fault. The message carries three things: the CENSUS (every unrepresentable outcome
	 * with its face count, so a second bar to a fix is not hidden behind the first), the
	 * first face that hit the primary outcome, and the primary outcome's own detail
	 * including the recorded texture sub-reason.
	 *
	 * <p>⚠ <b>It allocates, deliberately, and is therefore NOT on the hot path:</b> a caller
	 * should test {@link #allRepresentable()} first and only ask for the text when it is
	 * false and the text will be used. {@code GlSceneRenderer.drawModel} does exactly that.
	 *
	 * @return the reason, or {@code null} when {@link #allRepresentable()} is true
	 */
	public String declineReason() {
		int primary = -1;
		int kinds = 0;
		StringBuilder census = new StringBuilder();
		for (int outcome = 0; outcome < OUTCOME_COUNT; outcome++) {
			if (!isUnrepresentable(outcome) || counts[outcome] == 0) {
				continue;
			}
			if (primary < 0) {
				primary = outcome;
			}
			if (kinds > 0) {
				census.append(", ");
			}
			census.append(outcomeName(outcome)).append(" x").append(counts[outcome]);
			kinds++;
		}
		if (primary < 0) {
			return null;
		}
		return "a model face the GL path cannot represent: " + census + " of " + lastFaceCount
				+ " faces, first at face " + firstFaceWith(primary) + ": " + outcomeDetail(primary);
	}

	/** The primary outcome's own explanation, including the texture sub-reason when it is one. */
	private String outcomeDetail(int outcome) {
		if (outcome == NEEDS_TEXTURE) {
			return "a textured face (render type 2 or 3) - " + textureDeclineName(textureDecline)
					+ ": " + textureDeclineDetail();
		}
		if (outcome == NEEDS_CLIPPING) {
			return DETAIL_NEEDS_CLIPPING;
		}
		return DETAIL_NO_COLOUR;
	}

	private String textureDeclineDetail() {
		return textureDecline >= 0 && textureDecline < TEXTURE_REASON_COUNT
				? TEXTURE_DECLINE_DETAILS[textureDecline] : "unknown texture decline";
	}

	/** Records {@code reason} unless an earlier face already recorded one; see {@link #textureDecline}. */
	private void noteTextureDecline(int reason) {
		if (textureDecline == TEXTURE_OK) {
			textureDecline = reason;
		}
	}

	/** Records a texture decline AND yields the outcome it implies, so the ties cannot drift. */
	private int markTextureDecline(int reason) {
		noteTextureDecline(reason);
		return NEEDS_TEXTURE;
	}

	/** The index of the first face that got {@code outcome}, or {@code -1}. */
	private int firstFaceWith(int outcome) {
		int faces = Math.min(lastFaceCount, outcomes.length);
		for (int face = 0; face < faces; face++) {
			if (outcomes[face] == outcome) {
				return face;
			}
		}
		return -1;
	}

	/** Grows the scratch to the model's size. Never shrinks, so a busy frame allocates once. */
	private void ensureCapacity(int vertices, int faceCount) {
		if (vertexX.length < vertices) {
			vertexX = new int[vertices];
			vertexY = new int[vertices];
			vertexDepth = new int[vertices];
			vertexCamX = new int[vertices];
			vertexCamY = new int[vertices];
		}
		if (outcomes.length < faceCount) {
			outcomes = new int[faceCount];
			clipped = new boolean[faceCount];
		}
	}
}
