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
 * <p><b>⚠⚠ AND THE DEPTH IT CARRIES IS PER CORNER, NOT PER TILE (Phase 7.11).</b> One depth for
 * the whole tile is the right granularity for the software's FOG but the wrong one for a depth
 * BUFFER: a tile is 128 world units across, so its corners' true depths differ, and a flat plate
 * compared against the wall standing on it hides that wall's base wherever the wall is further
 * away than the tile's mean. That was the diagonal-wall occlusion. {@link #drawGroundTriangle}
 * now takes the three corner depths as well and gives each vertex its own {@code z}; the tile
 * mean remains, and is still the only thing the fog uses.
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
 *       atlas, ramp numerators that wrapped a 32-bit int <b>without cancelling</b> (see
 *       {@link TextureRamps#reproducesExactlyAt} - a wrap that cancels is drawn, not
 *       declined), or the sink declining the submission itself.</li>
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
 * <p><b>✅ THE SHADE FOG FADE IS NOW APPLIED IN BOTH PATHS (Phase 7.4j), and the joint fix was
 * the point of deferring it.</b> {@code Texture.method374} and {@code method378} both fade each
 * shade code through {@code Fog.fadeHsl(shade, Fog.sceneDepth)} before rasterising (the TEXTURED
 * path too - {@code method378}'s first three statements), and for a textured face that is not a
 * tint: the bits of the faded code select WHICH darkness copy of the texture is sampled, so a
 * fog-free oracle can never catch its absence. Both the model path ({@code GlFacePipeline}) and
 * the ground path here now pass their corner codes through the ONE resolver,
 * {@link GlTextures#fadedShade}, so neither can be updated without the other.
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
					+ "wrapped a 32-bit int WITHOUT cancelling (so the mapping is not reproducible "
					+ "even by the exact affine form), or the sink declined it";

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
	 * Model triangles submitted since startup, and how many of them were TEXTURED (7.4l).
	 *
	 * <p>⚠ Counted here rather than read off the batcher because the renderer holds its batch
	 * through the {@code SceneBatch} interface, which deliberately does not expose triangle
	 * counts - and because an actor that gets REWOUND must not be counted. So these are
	 * incremented only on the representable path, and never need decrementing.
	 */
	private int modelTriangles;
	private int texturedModelTriangles;

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
	private boolean reportedGroundSkip;
	/** See {@link #reportGroundColour}: counted per screen band, not by draw order. */
	private int groundProbeSeen;
	private int groundProbeAllOlive;
	private int groundProbeAllGrey;
	private int groundProbeNearBand;
	private int groundProbeNearOlive;
	private int groundProbeNearGrey;
	private int groundProbeNearReported;
	/** The near-field band the GL dump shows as grey and the software dump as olive. */
	private static final int NEAR_BAND_Y = 480;
	private static final int GROUND_PROBE_SUMMARY_AT = 500;
	private boolean reportedComposite;
	private String modelSkipReason;

	/**
	 * The software image, kept aside so the GL image can be compared against it (Phase 7.5a).
	 *
	 * <p>⚠⚠ <b>Null unless {@code -Dsoultrail.gldiff=true}, and that is not just politeness
	 * about cost.</b> The comparison copies the whole framebuffer and walks it every frame,
	 * and normal play must not pay for a debug tool. The property is read ONCE, in the
	 * constructor, so the answer cannot change under a frame - the same rule the latch
	 * follows about preconditions.
	 *
	 * <p>⚠ The oracle is free: the software scene is fully drawn into the producer's array in
	 * the shadow stage and the GL readback then overwrites it there, so the array holds the
	 * software's own answer for this frame at the instant {@link #readBack} runs. One copy at
	 * that instant is a per-pixel ground truth needing no fixture and no second render.
	 */
	private final GlFrameDiff diff;
	/** Window for the throttled diff line - see {@link #maybeReportDiff()}. */
	private static final long DIFF_REPORT_INTERVAL_MS = 5000L;
	private long lastDiffReportMillis;
	private boolean reportedDiff;
	/** Window for the throttled skip-rate line - see {@link #maybeReportSkipRate()}. */
	private static final long SKIP_REPORT_INTERVAL_MS = 10000L;
	private long lastSkipReportMillis;
	private int modelSkipEventsAtReport;
	private int groundTilesSkippedAtReport;
	/** Textured ground tiles skipped as unmappable, cumulative (Phase 7.4h). */
	private int groundTilesSkipped;
	/** Model skip EVENTS across all frames - see {@link #modelSkipEvents()} (Phase 7.4j). */
	private int modelSkipEvents;
	/** The uid of the last skipped model, or {@code Integer.MIN_VALUE} (Phase 7.4j). */
	private int modelSkippedUid = Integer.MIN_VALUE;
	/**
	 * The distinct model uids the GL path has dropped - see {@link #reportModelSkippedOnce}.
	 * Sized by the number of distinct actors actually dropped (one or two in practice), not by
	 * the number of skips.
	 */
	private final java.util.Set<Integer> skippedModelUids = new java.util.HashSet<Integer>();

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
		this.diff = diffRequested() ? new GlFrameDiff() : null;
	}

	/**
	 * Whether the A/B diff was asked for on the command line ({@code -Dsoultrail.gldiff=true}).
	 *
	 * <p>⚠ Follows {@code PacketTap}'s convention rather than inventing a second one: a debug
	 * tool is off unless a property names it, so normal play is unchanged and "it was on" is
	 * never an assumption. ⚠ Read in the constructor, so it cannot flip mid-frame.
	 */
	private static boolean diffRequested() {
		try {
			String v = System.getProperty("soultrail.gldiff");
			return v != null && !v.trim().isEmpty() && !"false".equalsIgnoreCase(v.trim());
		} catch (SecurityException e) {
			// A property read that is denied must not cost the player their renderer.
			return false;
		}
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
		// ⚠⚠ PHASE 7.4h: THE ACTOR IS DROPPED, NOT THE FRAME - the policy the 7.4e-7.4g live
		// runs made necessary. Those runs went 25 of 128 faces -> ~1, and at that point the
		// whole-frame latch was costing the ENTIRE GL feature for the sake of ONE face: GL
		// never drew a pixel because a single face of one model could not be represented. So
		// the mark/rewind pair exists: submit the model, and if any of its faces turned out
		// to be unrepresentable, undo the whole model and keep the frame.
		//
		// ⚠ The granularity is the MODEL rather than the single face, and that is a delivery
		// choice rather than a technical one: a half-submitted model would be a hole in the
		// middle of something the player is looking at, whereas a whole dropped model is a
		// missing actor - and the software body still runs underneath (see the class doc), so
		// the frame the latch discards is never what the user sees anyway.
		//
		// ⚠⚠ IT IS STILL A VISIBLE LOSS AND IS NOT DRESSED UP AS ANYTHING ELSE: the GL frame
		// REPLACES the software scene at sceneFinished, so a dropped model is ABSENT from the
		// finished image rather than merely un-accelerated. That is the price of the policy,
		// and the alternative (veto the frame) is what 7.4d-7.4g showed to be unshippable.
		int mark = batch.mark();
		pipeline.emit(model, orientation, camA, camB, camC, camD, dx, dy, dz,
				Texture.textureInt1, Texture.textureInt2, batch);
		if (!pipeline.allRepresentable()) {
			batch.rewind(mark);
			reportModelSkipped(pipeline.declineReason(), uid);
		} else {
			// ⚠⚠ COUNTED ONLY HERE, ON THE PATH WHERE THE TRIANGLES REALLY STAYED IN THE BATCH.
			// Counting before the representability check would add the triangles of an actor that
			// was undone a moment later - inflating exactly the number this counter exists to be
			// evidence for ("how much textured geometry reached the GPU"). ⚠ In the live client
			// that is a real error of up to a whole model's face count per frame (233 in the
			// observed case); it is NOT reproducible in the harness, where an unrepresentable
			// fixture happens to emit zero triangles - see the harness note, which says so rather
			// than pretending the placement is test-covered.
			modelTriangles += pipeline.triangles();
			texturedModelTriangles += pipeline.count(GlFacePipeline.TEXTURED);
		}
		// Shadow stage: the software body still runs. See the class doc.
		return false;
	}

	/**
	 * Names the model the GL path just DROPPED, once per DISTINCT actor.
	 *
	 * <p>⚠ Without it the skip would be invisible: the frame would be read back, look
	 * plausible, and be missing an actor - the failure mode this whole plan keeps catching.
	 * ⚠ It is deliberately NOT one-shot any more; the body says why a one-shot note could not
	 * answer "which model is missing".
	 */
	private void reportModelSkipped(String reason, int uid) {
		modelSkipReason = reason;
		modelSkippedUid = uid;
		modelSkipEvents++;
		// ⚠⚠⚠ THE UID IS NAMED, AND EVERY DISTINCT ONE IS NAMED ONCE (2026-10-07). The first
		// live run after 7.4j reported "skipping a MODEL ... NEEDS_TEXTURE x1 of 233 faces, first
		// at face 200" - which says HOW BADLY a model failed but not WHICH model, so the plan's
		// own next question ("is the skipped model visibly missing?") could only be answered by
		// hunting the scene by eye. `uid` is the actor's real identity (WorldController passes
		// `class10.uid` / `object4.uid` / ...), and `drawModel` was already being handed it and
		// throwing it away.
		//
		// ⚠ Why PER DISTINCT UID rather than a plain one-shot: a one-shot note answers "does this
		// happen" but not "WHAT is missing", and a per-frame note floods the log. Logging each
		// new uid once is bounded by the number of distinct actors the GL path actually drops -
		// in practice one or two - so it names them all without becoming noise.
		boolean first = skippedModelUids.isEmpty();
		if (skippedModelUids.add(uid)) {
			System.out.println("Renderer '" + name + "': skipping MODEL uid " + uid + " ("
					+ skippedModelUids.size() + " distinct so far)"
					+ (first ? ": the GL path cannot represent it, and the rest of the frame is "
							+ "drawn WITHOUT it - so that actor is MISSING from the GL image (the "
							+ "software path would have drawn it)." : " - that actor is likewise "
							+ "MISSING from the GL image.")
					+ " Reason: " + reason);
		}
		maybeReportSkipRate();
	}

	/**
	 * ⚠⚠ A THROTTLED RATE LINE, so the skip RATE is readable from a live log (Phase 7.4k).
	 *
	 * <p><b>Why the event counter alone is not enough.</b> {@link #modelSkipEvents()} was added
	 * so "one model dropped every frame" could be told apart from "an occasional drop" - but a
	 * counter that nothing prints is only useful to a debugger, and the whole point of this
	 * project's log lines is that a live run answers the question by itself. So the counters are
	 * reported at most once every {@link #SKIP_REPORT_INTERVAL_MS}, and <b>only when something
	 * was actually skipped in the window</b> - a clean session stays silent, which is what keeps
	 * a real signal findable.
	 *
	 * <p>⚠ It reports a DELTA and a TOTAL, because they answer different questions: the delta is
	 * the rate now, the total is how much of the session has been affected.
	 */
	private void maybeReportSkipRate() {
		long now = System.currentTimeMillis();
		if (now - lastSkipReportMillis < SKIP_REPORT_INTERVAL_MS) {
			return;
		}
		int modelDelta = modelSkipEvents - modelSkipEventsAtReport;
		int tileDelta = groundTilesSkipped - groundTilesSkippedAtReport;
		if (modelDelta == 0 && tileDelta == 0) {
			// Nothing happened in this window - report the elapsed time as consumed, so the next
			// interesting window is measured from here rather than from the last interesting one.
			lastSkipReportMillis = now;
			return;
		}
		// ⚠ A NONSENSE DURATION WAS PRINTED HERE ON THE FIRST REPORT, and the live log showed it
		// as "in the last 1791374742s" - because the sentinel for "no previous report" was 0,
		// which is the epoch, so the window came out as the entire age of the clock. A log line
		// whose first instance reads as nonsense invites the reader to distrust the rest of it.
		// The sentinel is now recognised as itself and named in words instead.
		String window = lastSkipReportMillis == 0 ? "since startup"
				: "in the last " + ((now - lastSkipReportMillis) / 1000) + "s";
		lastSkipReportMillis = now;
		modelSkipEventsAtReport = modelSkipEvents;
		groundTilesSkippedAtReport = groundTilesSkipped;
		System.out.println("Renderer '" + name + "': GL skipped " + modelDelta + " model submissions "
				+ "and " + tileDelta + " ground tiles " + window + " ("
				+ modelSkipEvents + " model and " + groundTilesSkipped + " tile skips since startup, "
				+ skippedModelUids.size() + " distinct model uids); frames read back "
				+ framesReadBack + ", discarded " + framesDiscarded + ".");
	}

	/**
	 * ⚠ How many model skips have happened in total, across all frames (Phase 7.4j follow-up).
	 *
	 * <p><b>This is an EVENT count, not a count of distinct models, and the distinction is the
	 * point.</b> The log note above is one-shot, so it cannot show whether the same model is
	 * dropped on every frame (a persistent hole) or a different one each frame (a rate) - and
	 * those two need completely different responses. A count that climbs in step with the frame
	 * rate means a model that is always in view is missing.
	 */
	public int modelSkipEvents() {
		return modelSkipEvents;
	}

	/**
	 * The uid of the last skipped model, or {@code Integer.MIN_VALUE} if none has been skipped.
	 *
	 * <p>⚠ Exposed so the note is assertable: a log line that fades into scrollback is not a
	 * contract, and the whole reason this was added is that "something is missing" must be a
	 * fact the harness can check rather than a sentence a human has to spot.
	 */
	public int modelSkippedUid() {
		return modelSkippedUid;
	}

	/**
	 * How many DISTINCT actors the GL path has dropped (Phase 7.4k).
	 *
	 * <p>⚠ The companion to {@link #modelSkipEvents()}, and the two must not be confused: a
	 * session that drops ONE actor on every frame has a huge event count and a distinct count of
	 * 1 - which is a single visible hole the player stares at, not a spreading problem. The
	 * reverse (many distinct, few events) is falling scenery. Exposed so the difference is
	 * assertable rather than something a human has to infer from two log lines.
	 */
	public int distinctSkippedModelCount() {
		return skippedModelUids.size();
	}

	/**
	 * The reason the last unrepresentable model was SKIPPED, or {@code null} (Phase 7.4h).
	 *
	 * <p>⚠ Exposed so the skip is assertable rather than merely logged: the policy change
	 * replaced "the frame is withheld" with "the actor is dropped", and a dropped actor is
	 * invisible in a passing frame unless something records why.
	 */
	public String modelSkipReason() {
		return modelSkipReason;
	}

	@Override
	public boolean drawGroundTriangle(int x0, int y0, int x1, int y1, int x2, int y2,
			int colour0, int colour1, int colour2, int textureId, boolean flatMesh,
			int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7, int t8,
			int depth, int depth0, int depth1, int depth2) {
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
		// ⚠⚠⚠ PHASE 7.11: ONE z PER CORNER, NOT ONE PER TILE. This is the fix for the
		// diagonal-wall occlusion, and it is worth stating exactly what was wrong before.
		//
		// Through 7.10 this method gave all three vertices `depthToZ(depth)`, i.e. the TILE's
		// single mean camera-space depth (for method315, (k2+j2+k3+j3)/4). That is the right
		// granularity for the software, which has no depth buffer at all and relies on painter
		// order - but it is WRONG for a z-buffer, because a tile is 128 world units across and
		// its corners' true depths differ by up to ~128*|sin yaw| fixed-point units. The buffer
		// was therefore comparing a FLAT plate against the wall standing on it: wherever the
		// wall's base was further away than the tile's mean, the tile won and the wall's lower
		// part was erased - which is what makes it appear only for tiles carrying a diagonal
		// wall, and only at the pitch angles that widen the spread.
		//
		// The three depths arrive in screen-corner order for method315 and method316 alike
		// (see SceneRasterizer.Implementation#drawGroundTriangle), are the SAME absolute axis as
		// Model.method443's per-vertex depths, and go through the SAME depthToZ - so a wall's
		// base now lands on the tile's own surface value instead of on a mean it never had.
		//
		// ⚠ `depth` itself is still used, and must stay used, for the FOG (see
		// submitTexturedGround): the software fogs a whole tile with its one mean depth, and
		// reproducing that is a different job from occluding correctly.
		float z0 = GlFacePipeline.depthToZ(depth0);
		float z1 = GlFacePipeline.depthToZ(depth1);
		float z2 = GlFacePipeline.depthToZ(depth2);

		if (textureId != -1) {
			// The software's own branch: `anIntArray720 == -1` is the only untextured case,
			// and a non-lowMem texture goes through method378. The lowMem arm is the flat
			// darkened-code variant, which ui cannot reproduce - see GROUND_LOWMEM_DECLINE.
			if (WorldController.lowMem) {
				declineFrame(GROUND_LOWMEM_DECLINE);
				return false;
			}
			return submitTexturedGround(x0, y0, x1, y1, x2, y2, colour0, colour1, colour2,
					t0, t1, t2, t3, t4, t5, t6, t7, t8, textureId, z0, z1, z2, depth);
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
		reportGroundColour(y0, y1, y2, colour0, colour1, colour2, c0, c1, c2, depth);
		// ⚠⚠⚠ PHASE 7.10: THE GROUND IS FORCED OPAQUE, AND IT MUST BE STATED HERE. The
		// software does exactly this - WorldController.java:1665 and :1971 both assign
		// `Texture.anInt1465 = 0` before walking the tiles - so a ground tile is never
		// alpha-blended however the model path left that static. Since 7.10 turned blending
		// ON, this is load-bearing rather than cosmetic: resolveCornerColour returns a palette
		// entry whose alpha byte is ZERO (the palette is 0x00RRGGBB, which is why the old
		// comment could call the byte "inert"), and a zero fragment alpha under
		// SRC_ALPHA/ONE_MINUS_SRC_ALPHA is FULLY TRANSPARENT - the entire ground would have
		// vanished. The whole scene that is not a modelled face is opaque ground, so this one
		// OR is the difference between a blended scene and no landscape at all.
		int opaque = 0xff000000;
		if (!batch.triangle(x0, y0, z0, opaque | c0, x1, y1, z1, opaque | c1, x2, y2, z2,
				opaque | c2)) {
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
	 *
	 * <p>⚠⚠ <b>{@code z0..z2} are PER-CORNER and {@code depth} is the tile mean, and the two
	 * must not be merged (Phase 7.11).</b> The z's are the corners' own camera-space depths and
	 * are what the depth test compares; {@code depth} is the single value the software fogs the
	 * whole tile with and is what the fade below must keep using. Using the mean for the z's is
	 * the diagonal-wall occlusion this step removes; using a corner z for the fade would fog one
	 * of the tile's three vertices differently from the software.
	 */
	private boolean submitTexturedGround(int x0, int y0, int x1, int y1, int x2, int y2,
			int shade0, int shade1, int shade2,
			int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7, int t8,
			int textureId, float z0, float z1, float z2, int depth) {
		if (!batch.supportsTextures()) {
			declineFrame(GROUND_MAPPING_DECLINE);
			return false;
		}
		// ⚠⚠ PHASE 7.4j: the tile's shades are FADED here, exactly as the model path fades its
		// own - {@code method374}/{@code method378} both fold every shade through
		// {@code Fog.fadeHsl} with {@code Fog.sceneDepth} before deriving the darkness block from
		// it, and for ground {@code Fog.sceneDepth} IS this tile's depth. ⚠ The depth is a
		// PARAMETER rather than a read of {@link #lastGroundDepth}, deliberately: the field is
		// assigned a few lines earlier in the caller, and a fade that silently depends on that
		// ordering would break the moment anything moved.
		// ⚠ Doing it for the model and not for the ground was the half-fix this step exists to
		// avoid: the two paths share ONE resolver ({@link GlTextures#fadedShade}) precisely so
		// neither can be updated alone.
		shade0 = GlTextures.fadedShade(shade0, depth);
		shade1 = GlTextures.fadedShade(shade1, depth);
		shade2 = GlTextures.fadedShade(shade2, depth);
		int size = GlTextures.layerSize();
		TextureRamps ramps = TextureRamps.of(t0, t1, t2, t3, t4, t5, t6, t7, t8,
				Texture.textureInt1, Texture.textureInt2, TextureRamps.denShiftFor(size),
				TextureRamps.colShiftFor(size), size);
		if (ramps.overflows() && !ramps.reproducesExactlyAt(x0, y0, x1, y1, x2, y2)) {
			// ⚠⚠ PHASE 7.4h: SKIP THIS TILE, KEEP THE FRAME - see drawModel for the policy and
			// for the honest note that a skipped tile is MISSING from the finished GL image.
			// A tile is the natural actor here: it is submitted on its own, so there is
			// nothing to undo and no mark/rewind is needed.
			//
			// ⚠ The exception is the reason 7.4c measured the guard at all: a wrap that DOES
			// cancel is drawn, and only a wrap that does not is skipped, because the latter's
			// mapping is not reproducible even by the exact affine form.
			//
			// ⚠ It is a SAMPLE of the tile (its three projected corners), not a proof over
			// every pixel - and where the sample cannot support the claim the tile is skipped,
			// which is the conservative direction.
			reportGroundSkippedOnce();
			return false;
		}
		int[] a = ramps.attributeAt(x0, y0);
		int[] b = ramps.attributeAt(x1, y1);
		int[] c = ramps.attributeAt(x2, y2);
		if (!batch.textured(x0, y0, z0, a[0], a[1], a[2], shade0,
				x1, y1, z1, b[0], b[1], b[2], shade1,
				x2, y2, z2, c[0], c[1], c[2], shade2, textureId)) {
			// ⚠⚠ PHASE 7.4h: per-actor, like the ramp overflow above. The sink's own refusals
			// here are PER-TILE - a texture id missing from the atlas - so skipping the tile is
			// the scoped answer. (A sink that is not ready at all is a different case and is
			// caught by latch() before this point, not here.)
			reportGroundSkippedOnce();
			return false;
		}
		texturedGroundTriangles++;
		groundTriangles++;
		return false;
	}

	/**
	 * The one-shot note that a textured GROUND TILE was skipped rather than the frame being
	 * withheld (Phase 7.4h).
	 *
	 * <p>⚠ Same reason as {@link #reportModelSkipped}: the finished GL image REPLACES the
	 * software scene, so a skipped tile is a MISSING patch of ground rather than a tile that
	 * merely fell back - and that must be visible in the log rather than inferred.
	 */
	/**
	 * Reports the ground's resolved colours, split by WHERE on screen the triangle lands
	 * (Phase 7.6g).
	 *
	 * <p>⚠⚠ <b>Why the first-triangle version was not enough, and this is a correction of my own
	 * 7.6f probe rather than an extension.</b> That probe printed the FIRST untextured ground
	 * triangle and it came back OLIVE (`1c1e00/1c1e00/6e791e`), which on its own reads as "the
	 * ground colours are fine, look elsewhere". ⚠ <b>But the first triangle is by definition the
	 * one drawn earliest, and the walk order puts the FAR ground first - it was a dark horizon
	 * tile, not the near field that the dumps show as grey.</b> A sample chosen by DRAW ORDER is
	 * not a sample of the region in question, and treating it as one would have retired the
	 * strongest lead on the strength of an unrepresentative pixel.
	 *
	 * <p>So this splits the report by SCREEN BAND: every untextured triangle whose topmost vertex
	 * is below y=480 (the band the class grid shows as a solid olive field in the software and a
	 * grey field in GL) is counted and classified, and the first five are printed in full.
	 */
	private void reportGroundColour(int y0, int y1, int y2, int code0, int code1, int code2,
			int c0, int c1, int c2, int depth) {
		groundProbeSeen++;
		boolean olive = olive(c0) && olive(c1) && olive(c2);
		boolean grey = grey(c0) && grey(c1) && grey(c2);
		if (olive) {
			groundProbeAllOlive++;
		}
		if (grey) {
			groundProbeAllGrey++;
		}
		int minY = Math.min(y0, Math.min(y1, y2));
		if (minY >= NEAR_BAND_Y) {
			groundProbeNearBand++;
			if (olive) {
				groundProbeNearOlive++;
			}
			if (grey) {
				groundProbeNearGrey++;
			}
			if (groundProbeNearReported < 5) {
				groundProbeNearReported++;
				System.out.println("Renderer '" + name + "': GROUND NEAR BAND (y>="
						+ NEAR_BAND_Y + ") - codes " + hex(code0) + "/" + hex(code1) + "/"
						+ hex(code2) + " -> palette " + hex(c0) + "/" + hex(c1) + "/" + hex(c2)
						+ " topY " + minY + " depth " + depth + " - "
						+ (olive ? "OLIVE" : grey ? "GREY" : "OTHER"));
			}
		}
		if (groundProbeSeen == GROUND_PROBE_SUMMARY_AT) {
			System.out.println("Renderer '" + name + "': GROUND COLOUR SUMMARY over "
					+ groundProbeSeen + " untextured ground triangles - all-olive "
					+ groundProbeAllOlive + ", all-grey " + groundProbeAllGrey
					+ ". NEAR BAND (topY>=" + NEAR_BAND_Y + "): " + groundProbeNearBand
					+ " triangles, " + groundProbeNearOlive + " OLIVE, " + groundProbeNearGrey
					+ " GREY. " + (groundProbeNearBand > 0 && groundProbeNearOlive == groundProbeNearBand
							? "Every near-field triangle is OLIVE, so the ground colours are correct "
									+ "all the way down and the grey in the GL image is introduced "
									+ "AFTER submission (z, a later pass, or the composite) - NOT a "
									+ "colour-resolution bug."
							: groundProbeNearBand > 0 && groundProbeNearGrey > 0
									? "Some near-field triangles resolve GREY, so the palette or the "
											+ "code IS wrong for them before any pixel is drawn."
									: "No near-field triangles yet - the summary is inconclusive."));
		}
	}

	private static boolean olive(int c) {
		if (c < 0) {
			return false;
		}
		int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
		return g >= r - 25 && g > b + 20;
	}

	private static boolean grey(int c) {
		if (c < 0) {
			return false;
		}
		int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
		int mx = Math.max(r, Math.max(g, b));
		int mn = Math.min(r, Math.min(g, b));
		return mx - mn <= 20;
	}

	private static String hex(int v) {
		return v < 0 ? "DECLINED" : String.format("%06x", v & 0xFFFFFF);
	}

	private void reportGroundSkippedOnce() {
		groundTilesSkipped++;
		maybeReportSkipRate();
		if (reportedGroundSkip) {
			return;
		}
		reportedGroundSkip = true;
		System.out.println("Renderer '" + name + "': skipping a textured GROUND TILE the GL path "
				+ "cannot map, and drawing the rest of the frame WITHOUT it - so that patch of "
				+ "ground is MISSING from the GL image (the software path would have drawn it).");
	}

	/**
	 * Whether a textured ground tile has been SKIPPED in this session (Phase 7.4h).
	 *
	 * <p>⚠ Exposed for the same reason {@link #modelSkipReason()} is: the policy change made a
	 * skipped tile invisible in a frame that otherwise looks fine, so the fact has to be
	 * assertable rather than merely logged.
	 */
	public boolean groundSkipped() {
		return groundTilesSkipped > 0;
	}

	/** How many textured ground tiles have been skipped as unmappable, cumulative (7.4h). */
	public int groundTilesSkipped() {
		return groundTilesSkipped;
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
				// ⚠⚠ SNAPSHOT BEFORE THE READBACK, and this ordering is the entire mechanism
				// (Phase 7.5a). The software scene was already drawn into this array by the
				// shadow stage; readInto is about to overwrite it in place with the GL
				// image. So this is the last instant at which the software's own answer for
				// THIS frame exists, and copying here is what makes the comparison an oracle
				// rather than a guess. Taken after the flush so a flush that throws cannot
				// leave a snapshot of a frame that never completed.
				boolean snapped = diff != null && producer != null
						&& producer.anIntArray315 != null
						&& diff.capture(producer.anIntArray315, producer.anInt316,
								producer.anInt317);
				replaced = readBack(producer);
				if (replaced && snapped
						&& diff.compare(producer.anIntArray315, producer.anInt316,
								producer.anInt317)) {
					reportDiff(producer);
				}
			}
			if (!replaced) {
				framesDiscarded++;
				reportDiscardOnce();
			} else {
				reportCompositeOnce(producer);
			}
		}
		resetFrame();
		return replaced;
	}

	/**
	 * Prints how far the GL image and the software image DISAGREE, and what shape the
	 * disagreement has (Phase 7.5a).
	 *
	 * <p>⚠⚠ <b>Why this is not covered by anything that came before.</b> Every other line this
	 * renderer prints answers "could GL draw it": the skip notes, the composite counts, the
	 * decline reasons. None of them can see a frame that is fully representable and still the
	 * WRONG PICTURE - a mis-resolved texture id, a wrong atlas layer, a shade block off by one.
	 * The live log could prove a frame was composited and still leave "is the ground garbled?"
	 * answerable only by eye, which is where this plan was left.
	 *
	 * <p>⚠ The verdict is {@link GlFrameDiff#verdict()}'s rather than a threshold here, because
	 * the classification is the part that has to be right and is the part the harness pins.
	 *
	 * <p>⚠ Two cadences, for the same reason the skip line has two: the FIRST comparison is
	 * printed in full (it is the one taken at the frame the run started on), and later ones are
	 * throttled, so a player can walk around and see whether the verdict CHANGES with the scene
	 * rather than only what it was once.
	 */
	private void reportDiff(RSImageProducer producer) {
		long now = System.currentTimeMillis();
		if (!reportedDiff) {
			reportedDiff = true;
			lastDiffReportMillis = now;
			System.out.println("Renderer '" + name + "': GL vs SOFTWARE diff of frame #"
					+ framesReadBack + " (" + producer.anInt316 + "x" + producer.anInt317
					+ ") - " + diff.describe() + ". VERDICT: " + diff.verdict());
			// ⚠⚠ WHAT THE IMAGES ACTUALLY CONTAIN, on the first comparison only (Phase 7.5a-2).
			// The first live run of this tool reported 99.7% of pixels differing - which cannot
			// be read as "the mapping is slightly off" and does not look like a comparison
			// between two renders of one scene at all. This line is what distinguishes the
			// mundane explanations (an empty image, a channel-order swap, a misaligned
			// readback) from a genuine content difference, and it prints ONCE because the
			// answer is structural rather than per-frame.
			System.out.println("Renderer '" + name + "': GL vs SOFTWARE CONTENT - " + diff.diagnose());
			// ⚠⚠ 7.5c: AND THE TWO IMAGES THEMSELVES, because the content line above ruled out
			// everything that could be ruled out numerically and the remaining question - "what
			// does each image look like?" - is a question for eyes. Written ONCE, at the same
			// moment as the content line, so the PNGs are provably the frame that was measured.
			if (diff.writePngs("gldiff", producer.anIntArray315, producer.anInt316,
					producer.anInt317)) {
				System.out.println("Renderer '" + name + "': wrote " + new java.io.File(
						"gldiff-software.png").getAbsolutePath() + " and " + new java.io.File(
								"gldiff-gl.png").getAbsolutePath()
						+ " - compare them side by side; this frame is the one measured above.");
			} else {
				System.out.println("Renderer '" + name + "': could not write the comparison PNGs "
						+ "(the working directory is not writable?) - the numbers above are the "
						+ "only record of frame #" + framesReadBack + ".");
			}
			return;
		}
		if (now - lastDiffReportMillis < DIFF_REPORT_INTERVAL_MS) {
			return;
		}
		lastDiffReportMillis = now;
		System.out.println("Renderer '" + name + "': GL vs SOFTWARE diff (latest of "
				+ framesReadBack + " frames, " + diff.differing() + " pixels differ): "
				+ diff.verdict());
	}

	/**
	 * ⚠⚠ POSITIVE EVIDENCE THAT GL IS ON SCREEN - printed ONCE, on the first frame actually
	 * composited (Phase 7.4l).
	 *
	 * <p><b>Why this exists, and it is a real gap rather than a nicety.</b> Every previous claim
	 * in this plan that "GL is presenting frames" rests on an ABSENCE: no {@code discarding GL
	 * frames} line, no size-mismatch line. Absence is decent evidence - {@link #readBack} fails
	 * and logs on a size mismatch - but it cannot distinguish "the GL image was composited" from
	 * "the GL path ran and drew almost nothing", and it never states the thing that actually
	 * matters for fidelity: <b>how many TEXTURED triangles reached the GPU</b>. That last number
	 * is the one the 7.4j shade fog fade lives behind - if it were zero, the fade could not
	 * matter and something upstream would be wrong instead.
	 *
	 * <p>⚠ It reports the counts for the FIRST composited frame only, because that is the frame
	 * whose composition is the new fact. Later totals are the throttled skip line's job.
	 */
	private void reportCompositeOnce(RSImageProducer producer) {
		if (reportedComposite) {
			return;
		}
		reportedComposite = true;
		int textured = texturedModelTriangles + texturedGroundTriangles;
		int total = modelTriangles + groundTriangles;
		System.out.println("Renderer '" + name + "': COMPOSITED GL frame #" + framesReadBack
				+ " at " + batch.viewportWidth() + "x" + batch.viewportHeight() + " into the "
				+ "software framebuffer (" + producer.anInt316 + "x" + producer.anInt317 + "). "
				+ "Since startup: " + total + " triangles (" + modelTriangles + " model, "
				+ groundTriangles + " ground), of which " + textured + " are TEXTURED ("
				+ texturedModelTriangles + " model faces, " + texturedGroundTriangles
				+ " ground tiles). This is GL's own image, not the software scene - the software "
				+ "scene was drawn and then replaced.");
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

	/**
	 * Model triangles submitted since startup, and the TEXTURED subset (Phase 7.4l).
	 *
	 * <p>⚠ Exposed so the composite line's central claim - that textured geometry really reached
	 * the GPU - is a checkable number rather than a sentence in a log. ⚠ And the rewind path is
	 * why it is asserted: an actor that was undone contributed nothing, so these must not move.
	 */
	public int modelTriangles() {
		return modelTriangles;
	}

	public int texturedModelTriangles() {
		return texturedModelTriangles;
	}

	/**
	 * The A/B comparator, or {@code null} when {@code -Dsoultrail.gldiff} did not ask for one.
	 *
	 * <p>⚠ Exposed so the harness can drive the comparison THROUGH this renderer's seam rather
	 * than only against {@link GlFrameDiff} directly - the classifier is pinned as a pure unit,
	 * but "the seam actually calls it, in the right order, on the frame that was read back" is
	 * a separate claim and the one that decides whether a live run prints anything.
	 */
	public GlFrameDiff diff() {
		return diff;
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
