package ui;

import model.Model;
import model.Texture;
import scene.WorldController;

/**
 * The GL renderer's FRAME LIFECYCLE (Phase 7.2c).
 *
 * <p><b>The shape of the problem, and why it is one decision per frame rather than one
 * per draw.</b> {@code WorldController.method314} renders ground and models INTERLEAVED
 * per tile, while the software path composits them purely by PAINTER ORDER - the client
 * has no depth buffer anywhere in its own rasterisers, so occlusion is "whatever was
 * drawn last". GL renders into an offscreen target with its own depth attachment, and two
 * independently ordered images cannot be merged after the fact. So a frame has to go to
 * GL as a whole or not at all, and the decision is therefore taken ONCE - at the frame's
 * first scene submission - and applied to every submission that follows, without
 * re-deciding. That is {@link #latch()}.
 *
 * <p><b>Why the latch is SOUND.</b> It keys on GLOBAL preconditions only, so they cannot
 * change mid-frame: is the context up, is the texture atlas ready, and does the GL
 * viewport match the software drawing area. If they hold, the frame goes to GL; if not,
 * every submission returns {@code false} and the software path draws the whole frame
 * exactly as before.
 *
 * <p><b>⚠️ THE FRAME IS SHADOW-RENDERED, NOT TAKEN OVER, AND THAT IS DELIBERATE.</b> The
 * latch decides whether GL RENDERS the frame, not whether the software is told to stop.
 * Submissions still return {@code false}, so {@code method443} runs its own body and the
 * software scene is drawn as well; at the scene-finished seam GL's version REPLACES it,
 * but only when the whole frame was representable. The reason is that consuming a draw is
 * IRREVERSIBLE: a face that declines in the middle of a frame would leave a hole in the
 * GL image with the software image already given up, and neither can be recovered. So
 * until the per-face outcomes are total, the software image stays as the fallback, and a
 * frame that is not fully representable is DISCARDED rather than half-drawn. The cost is
 * that nothing is saved yet; the benefit is that a wrong GL frame can never be the only
 * frame. Flipping to a real takeover is then a one-line change per operation, on evidence
 * rather than on hope - which is the live gate's job (7.4).
 *
 * <p><b>⚠️ THE GROUND NOW CARRIES DEPTH AND IS SUBMITTED (Phase 7.2c-2).</b> The ground seam
 * used to arrive without a camera-space depth - its argument list mirrored what
 * {@code Texture.method374}/{@code method378} receive, and neither rasteriser is given one,
 * because the software composites ground against models by paint order rather than by depth.
 * GL cannot do that: its depth attachment would let a far thing punch through a near hill,
 * which the software does not do. So {@code WorldController.method315}/{@code method316} now
 * pass the tile's own {@code Fog.sceneDepth} through the seam, and this method turns it into
 * geometry: untextured ground becomes a coloured triangle through
 * {@link GlFacePipeline#resolveCornerColour} (the same colour contract the model path uses,
 * and the same one {@code method374} implements), textured ground becomes a
 * {@link TextureRamps} submission whose three numerator pairs are evaluated at the tile's own
 * projected corners. Ground and models therefore land on ONE depth axis - see
 * {@link GlFacePipeline#depthToZ}, including the note on why the depth fed to it is absolute.
 *
 * <p><b>What still declines, and each is named rather than silent</b> - see
 * {@link #frameDeclineReason()}, so a live run measures what is left instead of leaving it to
 * be inferred:
 * <ul>
 *   <li>{@link #GROUND_LOWMEM_DECLINE} - the LOW-DETAIL textured ground. On low detail the
 *       software does not texturise at all: it darkens the three colour codes through
 *       {@code WorldController.method317}/{@code anIntArray485} and hands them to
 *       {@code method374}. Neither is reachable from {@code ui}, so reproducing that branch
 *       would mean a second copy of a darkening mechanism. It is a real branch, but it is off
 *       in the detail level the client actually selects.</li>
 *   <li>{@link #GROUND_COLOUR_DECLINE} - a ground colour code outside the palette.</li>
 *   <li>{@link #GROUND_MAPPING_DECLINE} - a textured tile this sink cannot draw: no texture
 *       atlas, ramp numerators that wrapped a 32-bit int (so the mapping is no longer affine),
 *       or the sink declining the submission itself.</li>
 *   <li><b>An unrepresentable MODEL face - and unlike the three above, the reason for this one
 *       belongs to {@link GlFacePipeline}, not to a constant here.</b> It is reported by
 *       {@link GlFacePipeline#declineReason()}, which names the OUTCOME ({@code NEEDS_CLIPPING},
 *       {@code NEEDS_TEXTURE}, {@code NO_COLOUR}), the face that first hit it, and the census of
 *       every other outcome that also failed - plus, for a textured face, which of the eight
 *       {@code TEXTURE_*} sub-cases it was. ⚠ <b>That delegation is the point rather than a
 *       tidiness:</b> a fixed string here (which is what this used to be) names the file to
 *       read instead of the fault to fix, which is exactly where the first live gate run
 *       stopped.</li>
 * </ul>
 *
 * <p><b>⚠️ ONE FIDELITY ITEM IS KNOWINGLY DEFERRED, and it is flagged rather than hidden:
 * the SHADE FOG FADE.</b> {@code Texture.method374} and {@code method378} both fade each
 * shade code through {@code Fog.fadeHsl(shade, Fog.sceneDepth)} before rasterising (note this
 * applies to the TEXTURED path too - {@code method378}'s first three statements), and
 * {@code ui/GlTextures.shade(block0, shadeCode, sceneDepth)} exists precisely to express
 * that. Neither the model path nor the ground path above calls it yet - both pass the raw
 * shade code - so a fogged textured face will come out at the wrong brightness in GL.
 * Making the two paths agree is the point of passing both through one resolver, which is
 * why this is left as a single item rather than fixed in one path only.
 *
 * <p><b>Logging is once per kind</b>, never per call: these run thousands of times a
 * second, and a per-call log would be worse than useless.
 *
 * <p>⚠ <b>One fragility, recorded rather than papered over: the latch is cleared by
 * {@link #sceneFinished}, so an exception ESCAPING {@code client.method146} would leave it
 * set and the next frame would reuse the batch</b> - costing one wrong frame, not a leak,
 * because {@code GlBatcher.beginFrame} clears its own buffers and the next successful frame
 * re-latches. The software path is equally exposed (its own body has no {@code finally}),
 * so this is not a new failure mode, and an explicit frame-START signal would be the fix if
 * it ever matters.
 */
public final class GlSceneRenderer implements GpuRenderer.Implementation {

	/**
	 * The software's "already drawn" colour code. {@code method374} and {@code method378}
	 * are both guarded on it at the ground call sites, so a triangle carrying it is drawn
	 * by NOBODY - which is why GL must skip it too rather than draw a triangle the
	 * software would have omitted.
	 */
	private static final int SKIP_COLOUR = 0xbc614e;

	/**
	 * The ground decline reasons, named once so the live log, the harness and the next step's
	 * scope all agree on what is left. Public because each is a FACT about the frame rather
	 * than an implementation detail - a caller may reasonably branch on one.
	 */
	public static final String GROUND_LOWMEM_DECLINE =
			"ground: the LOW-DETAIL textured tile is not reproduced - the software darkens its "
					+ "colour codes through WorldController.method317/anIntArray485 and draws them "
					+ "flat through method374, and neither is reachable from ui";

	public static final String GROUND_COLOUR_DECLINE =
			"ground: a ground colour code is outside the palette, so the tile has no colour this "
					+ "client can draw";

	public static final String GROUND_MAPPING_DECLINE =
			"ground: a textured tile cannot be mapped - no texture atlas, ramp numerators that "
					+ "wrapped a 32-bit int so the mapping is not affine, or the sink declined it";

	private final String name;
	private final SceneBatch batch;
	private final GlFacePipeline pipeline = new GlFacePipeline();

	// ---------------------------------------- the per-frame latch
	private boolean frameLatched;
	private boolean frameInGl;
	private boolean frameRepresentable;
	private String frameDeclineReason;

	// ---------------------------------------- counters and one-shot logs
	private int framesLatched;
	private int framesReadBack;
	private int framesDiscarded;

	/** Ground tiles submitted since construction, and which of them went down the textured path. */
	private int groundTriangles;
	private int texturedGroundTriangles;

	/**
	 * The camera-space depth of the last ground tile the seam handed over.
	 *
	 * <p>⚠ <b>Exposed so the DEPTH ITSELF can be asserted rather than inferred.</b> The whole
	 * content of Phase 7.2c-2 is that the ground arrives with the software's own tile depth;
	 * a test that only counted a triangle would pass just as well if the depth were dropped
	 * and a constant substituted, which is exactly the failure worth catching.
	 */
	private int lastGroundDepth;

	private boolean reportedPresent;
	private boolean reportedScene;
	private boolean reportedDiscard;
	private boolean reportedSizeMismatch;

	/** Production form: the real GPU batch, which is where {@code GlScene.ensure(int, int)} runs. */
	public GlSceneRenderer(String name) {
		this(name, new GlBatcher());
	}

	/**
	 * The seam form, used by the harness to drive the lifecycle with a recording batch.
	 *
	 * <p>Public rather than package-private because the harness lives in the default
	 * package and cannot see {@code ui}'s package-private members - the same reason
	 * {@link GpuRenderer#install} is public while {@link SceneRasterizer#install} is not.
	 */
	public GlSceneRenderer(String name, SceneBatch batch) {
		this.name = name;
		this.batch = batch;
	}

	// ---------------------------------------- the present seam

	/**
	 * Declines, always - and that is the plan rather than an omission.
	 *
	 * <p>The scene is composited into the software framebuffer at {@link #sceneFinished},
	 * which is the only place it CAN be: the HUD is drawn on top of the scene before the
	 * present, so a renderer that presented its own offscreen frame would have to
	 * reproduce the whole of the software's 2D composition. Returning {@code false} keeps
	 * the existing software blit as the thing that puts the frame on screen.
	 */
	@Override
	public boolean presentGameFrame(RSImageProducer producer, int destX, int destY) {
		if (!reportedPresent) {
			reportedPresent = true;
			System.out.println("Renderer '" + name + "': the scene is composited at the "
					+ "scene-finished seam, so the frame is presented by the software blit.");
		}
		return false;
	}

	// ---------------------------------------- the scene seam

	@Override
	public boolean drawModel(Model model, int orientation, int camA, int camB, int camC, int camD,
			int dx, int dy, int dz, int uid) {
		if (!latch()) {
			return false;
		}
		pipeline.emit(model, orientation, camA, camB, camC, camD, dx, dy, dz,
				Texture.textureInt1, Texture.textureInt2, batch);
		// ⚠⚠ The reasons the frame is not whole are now NAMED by the pipeline rather than
		// summarised here, and that change is the direct answer to the first live gate run
		// (2026-10-06): the client reported "a model face the GL path cannot represent (see
		// GlFacePipeline)", which says which FILE to read and not which FAULT to fix. See
		// GlFacePipeline#declineReason for why the outcome alone is not actionable - eight
		// distinct failures hide behind NEEDS_TEXTURE by itself.
		//
		// ⚠ The order is deliberate: allRepresentable() is a count test over ints and costs
		// nothing, declineReason() allocates a sentence, and drawModel runs once per model per
		// frame. And the second test is on frameDeclineReason rather than on the pipeline,
		// because declineFrame keeps the FIRST reason - so once the frame already has one
		// there is nothing for this to add, however many models decline after it.
		if (frameDeclineReason == null && !pipeline.allRepresentable()) {
			declineFrame(pipeline.declineReason());
		}
		// Shadow stage: the software body still runs. See the class doc.
		return false;
	}

	@Override
	public boolean drawGroundTriangle(int x0, int y0, int x1, int y1, int x2, int y2,
			int colour0, int colour1, int colour2, int textureId, boolean flatMesh,
			int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7, int t8,
			int depth) {
		if (!latch()) {
			return false;
		}
		if (colour0 == SKIP_COLOUR) {
			// The software draws NOTHING for this triangle - its own call sites are guarded
			// on this sentinel - so GL drawing it would ADD a triangle, not match one. The
			// frame stays representable.
			return false;
		}
		lastGroundDepth = depth;
		// ⚠ ONE z for the whole tile, and that is the software's own granularity rather than
		// a shortcut: method315 submits a whole tile as a unit and fogs it with this one mean
		// depth. See SceneRasterizer.Implementation#drawGroundTriangle.
		float z = GlFacePipeline.depthToZ(depth);

		if (textureId != -1) {
			// The software's own branch: `anIntArray720 == -1` is the only untextured case,
			// and a non-lowMem texture goes through method378. The lowMem arm is the flat
			// darkened-code variant, which ui cannot reproduce - see GROUND_LOWMEM_DECLINE.
			if (WorldController.lowMem) {
				declineFrame(GROUND_LOWMEM_DECLINE);
				return false;
			}
			return submitTexturedGround(x0, y0, x1, y1, x2, y2, colour0, colour1, colour2,
					t0, t1, t2, t3, t4, t5, t6, t7, t8, textureId, z);
		}

		// method374's colour contract, through the ONE owner of it: the code is fogged first
		// and the PALETTE is then looked up per pixel, which is method374's order (and is the
		// reverse of method376's - see GlFacePipeline#resolveFlatColour).
		int c0 = GlFacePipeline.resolveCornerColour(colour0, depth);
		int c1 = GlFacePipeline.resolveCornerColour(colour1, depth);
		int c2 = GlFacePipeline.resolveCornerColour(colour2, depth);
		if (c0 < 0 || c1 < 0 || c2 < 0) {
			declineFrame(GROUND_COLOUR_DECLINE);
			return false;
		}
		if (!batch.triangle(x0, y0, z, c0, x1, y1, z, c1, x2, y2, z, c2)) {
			declineFrame(GROUND_MAPPING_DECLINE);
			return false;
		}
		groundTriangles++;
		// Shadow stage: the software body still runs. See the class doc.
		return false;
	}

	/**
	 * A textured ground tile, as the ramp numerators {@link TextureRamps} exists to produce.
	 *
	 * <p>⚠ <b>The ramps are anchored at the screen origin ({@code Texture.textureInt1/2}) and
	 * evaluated at the tile's own projected corners</b> - the two are different points, and
	 * that is the point: the mapping is a function of screen position, and the triangle only
	 * bounds where it is rasterised. This is exactly the arrangement the model path uses, so
	 * the two cannot drift.
	 *
	 * <p>⚠ <b>{@code t0..t8} go in AS GIVEN.</b> The seam already carries whichever of the
	 * software's two sets {@code method378} would receive for this triangle - the flat tile and
	 * the non-flat tile pass genuinely different sets, not one set reordered - so normalising
	 * them here would be a second, wrong reading. See
	 * {@code SceneRasterizer.Implementation#drawGroundTriangle}.
	 *
	 * <p>⚠ <b>The detail level is read from the ATLAS owner, not from the drawer.</b>
	 * {@code method379} branches on {@code Texture.lowMem}, and {@link GlTextures#layerSize()}
	 * is the single reader of it, so the shifts and the layer side cannot disagree with the
	 * texture array that was actually uploaded.
	 *
	 * <p>⚠ <b>The shade codes are passed RAW, deliberately</b> - matching the model path, and
	 * leaving the fog fade as the one joint item the class doc describes rather than fixing it
	 * in one path only.
	 */
	private boolean submitTexturedGround(int x0, int y0, int x1, int y1, int x2, int y2,
			int shade0, int shade1, int shade2,
			int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7, int t8,
			int textureId, float z) {
		if (!batch.supportsTextures()) {
			declineFrame(GROUND_MAPPING_DECLINE);
			return false;
		}
		int size = GlTextures.layerSize();
		TextureRamps ramps = TextureRamps.of(t0, t1, t2, t3, t4, t5, t6, t7, t8,
				Texture.textureInt1, Texture.textureInt2, TextureRamps.denShiftFor(size),
				TextureRamps.colShiftFor(size), size);
		if (ramps.overflows()) {
			// The numerators wrapped a 32-bit int, so the mapping is the software's WRAP and is
			// no longer affine - a shader cannot reproduce it by interpolating them.
			declineFrame(GROUND_MAPPING_DECLINE);
			return false;
		}
		int[] a = ramps.attributeAt(x0, y0);
		int[] b = ramps.attributeAt(x1, y1);
		int[] c = ramps.attributeAt(x2, y2);
		if (!batch.textured(x0, y0, z, a[0], a[1], a[2], shade0,
				x1, y1, z, b[0], b[1], b[2], shade1,
				x2, y2, z, c[0], c[1], c[2], shade2, textureId)) {
			declineFrame(GROUND_MAPPING_DECLINE);
			return false;
		}
		texturedGroundTriangles++;
		groundTriangles++;
		return false;
	}

	/**
	 * The scene is complete: draw the batched frame and, if it is whole, put it where the
	 * software scene was.
	 *
	 * <p><b>Why here and nowhere else.</b> The caller places this between
	 * {@code WorldController.method313} and the software 2D that follows it - NPC hulls,
	 * the anti-alias pass, item names, head icons and the 3D screen overlays. So reading
	 * back HERE means the anti-alias pass and every overlay run over the GL image exactly
	 * as they run over the software one, while reading back at the present would erase
	 * them. {@code client.saveSceneBackup()} also runs after this point, so an
	 * {@code fpsUnlocked} frame cache preserves the GL scene rather than the software one
	 * it replaced.
	 *
	 * @return {@code true} if {@code producer} now holds the GL scene
	 */
	@Override
	public boolean sceneFinished(RSImageProducer producer) {
		if (!frameLatched) {
			// No scene was submitted this frame - a menu, a loading screen, or a frame that
			// never reached method313. Nothing to finish.
			return false;
		}
		boolean inGl = frameInGl;
		boolean whole = frameInGl && frameRepresentable;
		boolean replaced = false;
		if (inGl) {
			batch.flush();
			if (whole) {
				replaced = readBack(producer);
			}
			if (!replaced) {
				framesDiscarded++;
				reportDiscardOnce();
			}
		}
		resetFrame();
		return replaced;
	}

	// ---------------------------------------- the latch

	/**
	 * The frame's single accept/decline decision, taken on the first scene submission and
	 * replayed for the rest of the frame.
	 *
	 * <p><b>Every precondition here is GLOBAL</b>, which is what makes deciding once sound:
	 * none of them can change between the first submission and the last. A per-face or
	 * per-tile condition could, and deciding on one of those would be the bug this shape
	 * exists to avoid.
	 *
	 * <p>⚠⚠ <b>The first precondition used to be a DEADLOCK, and the live gate is what
	 * found it (2026-10-06).</b> It read {@code if (!batch.ready())}, and {@code ready()} is
	 * a pure query whose flag is only ever set by {@link SceneBatch#ensure()} - which
	 * {@code beginFrame} is the only caller of. So the latch asked whether a batch that
	 * nothing had tried to bring up was up yet, got {@code false}, declined, and never
	 * reached the call that would have set it: <b>no frame could ever latch, on any machine
	 * and in any window size</b>, and the live client said so in the one line that names the
	 * shape of it - {@code "GL batcher unavailable (null)"}, the {@code (null)} being the
	 * failure string that {@code ensure()} never got to assign. ⚠ <b>The harness could not
	 * see it because its recording batch answers {@code ready()} with a constant
	 * {@code true}</b> - a faithful double of "a recording batch is ready", but one that
	 * models readiness as a fact about the world rather than as the state of an
	 * initialisation. The fix is to ASK for the initialisation, which is also why
	 * {@code SceneBatch#ensure} exists as its own method.
	 *
	 * @return {@code true} if the GL path is rendering this frame
	 */
	private boolean latch() {
		if (frameLatched) {
			return frameInGl;
		}
		frameLatched = true;
		frameInGl = false;
		frameRepresentable = true;
		frameDeclineReason = null;

		// ⚠ ensure(...) and NOT ready(). ready() only REPORTS, and the batch is brought up by
		// beginFrame - so testing ready() here asked whether a batch that nothing had tried
		// to start yet was started, got `false` forever, and never reached the call that
		// would have made it true. No frame could latch at all. See SceneBatch#ensure.
		//
		// ⚠ And the SIZE is stated here, because the frame target has to BE the drawing
		// area. A viewport fixed at 765x503 is neither fixed mode's 512x334 nor resizable
		// mode's frame-minus-sidebar-and-title geometry, so it matched NOTHING and declined
		// every frame in every configuration. Asking for the drawing area turns the check
		// below from `DrawingArea == renderer == 765x503` - which could never be true - into
		// the POST-CONDITION it should always have been: did it come up at the size we need?
		if (!batch.ensure(DrawingArea.width, DrawingArea.height)) {
			reportSceneOnce(false);
			return false;
		}
		// Read back over a buffer of a different size and the picture would be cropped or
		// sheared, and neither scaling nor centring reproduces the software render, so a
		// batch that did not take the size declines the frame rather than being stretched.
		if (DrawingArea.width != batch.viewportWidth()
				|| DrawingArea.height != batch.viewportHeight()) {
			reportSizeMismatchOnce();
			reportSceneOnce(true);
			return false;
		}
		if (!batch.beginFrame(batch.sceneBackground())) {
			reportSceneOnce(true);
			return false;
		}
		framesLatched++;
		frameInGl = true;
		reportSceneOnce(true);
		return true;
	}

	/**
	 * Marks the frame not-whole, keeping the FIRST reason.
	 *
	 * <p>First rather than last on purpose: the first unrepresentable thing is the one to
	 * fix, and a later, more common decline would otherwise mask an earlier, rarer one.
	 */
	private void declineFrame(String reason) {
		frameRepresentable = false;
		if (frameDeclineReason == null) {
			frameDeclineReason = reason;
		}
	}

	/**
	 * Copies the GL frame over the software scene.
	 *
	 * <p>Anchored at {@code (0, 0)} because the latch has already required the drawing area
	 * to be exactly the viewport - so the producer's pixels ARE the frame, with no offset
	 * and no stride slack to account for.
	 */
	private boolean readBack(RSImageProducer producer) {
		if (producer == null || producer.anIntArray315 == null) {
			return false;
		}
		if (batch.readInto(producer.anIntArray315, producer.anInt316, 0, 0)) {
			framesReadBack++;
			return true;
		}
		return false;
	}

	/**
	 * Clears the latch so the NEXT frame decides for itself.
	 *
	 * <p>⚠ {@code frameDeclineReason} is deliberately NOT cleared here: it is the record of
	 * the frame that just finished, which is what {@link #frameDeclineReason()} is asked
	 * for. It is cleared when the next frame latches instead.
	 */
	private void resetFrame() {
		frameLatched = false;
		frameInGl = false;
		frameRepresentable = true;
	}

	// ---------------------------------------- diagnostics

	/**
	 * Why the last finished frame was discarded, or {@code null} if it was not - i.e. if
	 * it was read back, or if the GL path never took it in the first place.
	 *
	 * <p>Exposed because the remaining distance to a visible GL frame is otherwise
	 * invisible: with the ground declining, every frame is discarded, and "which decline"
	 * is the only question worth asking of a live run.
	 */
	public String frameDeclineReason() {
		return frameDeclineReason;
	}

	/** Frames the GL path latched, i.e. started rendering. */
	public int framesLatched() {
		return framesLatched;
	}

	/** Frames whose pixels were actually put into the software framebuffer. */
	public int framesReadBack() {
		return framesReadBack;
	}

	/** Frames GL started and threw away because something in them was not representable. */
	public int framesDiscarded() {
		return framesDiscarded;
	}

	/** Ground tiles submitted to the batch since construction, textured or not. */
	public int groundTriangles() {
		return groundTriangles;
	}

	/** Ground tiles that went down the textured path. */
	public int texturedGroundTriangles() {
		return texturedGroundTriangles;
	}

	/** The camera-space depth of the last ground tile the seam handed over. */
	public int lastGroundDepth() {
		return lastGroundDepth;
	}

	private void reportSceneOnce(boolean ready) {
		if (reportedScene) {
			return;
		}
		reportedScene = true;
		System.out.println("Renderer '" + name + "' scene batch: " + batch.describe());
		if (!ready) {
			System.out.println("Renderer '" + name + "': the scene stays in software "
					+ "(the GL batch is not usable).");
		}
	}

	private void reportSizeMismatchOnce() {
		if (reportedSizeMismatch) {
			return;
		}
		reportedSizeMismatch = true;
			System.out.println("Renderer '" + name + "': GL viewport " + batch.viewportWidth() + "x"
					+ batch.viewportHeight() + " is not the drawing area " + DrawingArea.width
					+ "x" + DrawingArea.height + ", so the scene stays in software. The batch was "
					+ "asked for that drawing area and did not take it, so scaling or centring the "
					+ "frame would not reproduce the software render.");
	}

	private void reportDiscardOnce() {
		if (reportedDiscard) {
			return;
		}
		reportedDiscard = true;
		System.out.println("Renderer '" + name + "': discarding GL frames while the scene is not "
				+ "fully representable, so the software image stands. First reason: "
				+ frameDeclineReason);
	}
}
