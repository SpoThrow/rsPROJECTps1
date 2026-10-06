package ui;

/**
 * The frame-level seam in front of {@link GlBatcher} (Phase 7.2c).
 *
 * <p><b>Why this exists.</b> The decision {@code GlSceneRenderer} has to make is
 * per-FRAME, not per-draw: the whole scene goes to GL or none of it does. But the inputs
 * to that decision - the driver, the shader program, the texture atlas and the
 * framebuffer - all belong to {@link GlBatcher}. While the renderer named
 * {@code GlBatcher} directly, the whole lifecycle was reachable only through a real GL
 * context, and this repo's harness deliberately ships WITHOUT LWJGL 3, so none of it
 * could be tested. Naming the frame operations is what makes the latch, the routing and
 * the readback gate testable; what is left unverified is the driver itself, which is the
 * live gate's job (7.4).
 *
 * <p><b>Frame scope.</b> One batch holds ONE frame. {@link #beginFrame} starts it and
 * clears it, {@link #flush} draws what was submitted, {@link #readInto} copies it out.
 * Submissions in between are UNORDERED: the batch resolves them with a depth attachment
 * rather than painter's order, which is what lets a renderer accept the ground and the
 * models in the interleaved order {@code WorldController.method314} produces them.
 *
 * <p><b>Threading.</b> Every method here runs on the game thread, because
 * {@link GlScene#ensure()} requires it - see {@link GlScene}'s class doc. The seam is
 * dispatched from {@code Model.method443} and the ground hooks, which are game-thread
 * code.
 */
public interface SceneBatch extends TriangleSink {

	/**
	 * Starts a frame and clears it to {@code clearArgb}.
	 *
	 * <p>{@code clearArgb} is the scene's background, and it is a parameter rather than a
	 * constant because the software path does not clear to a constant either: {@code
	 * client.method146} calls {@code Fog.fillBackground()} when fog is on and
	 * {@code DrawingArea.setAllPixelsToZero()} when it is not. A GL clear that disagrees
	 * would leave the sky the wrong colour.
	 *
	 * @return {@code true} if the batch is ready to accept submissions for this frame
	 */
	boolean beginFrame(int clearArgb);

	/**
	 * Draws everything submitted since {@link #beginFrame}.
	 *
	 * @return {@code false} if none of it could be drawn
	 */
	boolean flush();

	/**
	 * Copies the drawn frame into a software pixel buffer.
	 *
	 * <p>The two conversions (the row flip, since {@code glReadPixels} is bottom-up, and
	 * dropping alpha, since the client writes {@code 0x00RRGGBB}) belong to the
	 * implementation rather than to the caller - see {@code GlBatcher.readInto}.
	 *
	 * @param dest       destination pixels, at least {@code viewportWidth() * viewportHeight()} long
	 * @param destStride pixels per destination row
	 * @param destX      column in {@code dest} to start writing at
	 * @param destY      row in {@code dest} to start writing at
	 * @return {@code true} if pixels were written
	 */
	boolean readInto(int[] dest, int destStride, int destX, int destY);

	/**
	 * The width, in pixels, of the frame this batch renders - and it is answerable
	 * BEFORE {@link #beginFrame}, because a caller has to be able to ask whether the
	 * current software drawing area even matches it. See {@code GlSceneRenderer}'s
	 * size gate for why a mismatch must decline rather than scale.
	 */
	int viewportWidth();

	/** The height, in pixels, of the frame this batch renders; see {@link #viewportWidth()}. */
	int viewportHeight();

	/**
	 * Brings the batch's initialisation up AT THE SIZE THE FRAME WILL BE, and reports
	 * whether it succeeded.
	 *
	 * <p>⚠️ <b>This is deliberately separate from {@link #ready()}, and conflating the two
	 * is a DEADLOCK rather than a matter of style.</b> {@code ready()} only REPORTS; the
	 * initialisation is done by {@link #beginFrame}. So a caller that gated the frame on
	 * {@code ready()} and only then called {@code beginFrame} was asking whether a batch
	 * that nothing had tried to bring up was up yet, getting {@code false}, declining, and
	 * never reaching the call that would have made it {@code true} - i.e. no frame could
	 * EVER latch. A caller that has to know before it commits a frame (see
	 * {@code GlSceneRenderer}'s latch and its size gate) must ask THIS, and {@link #ready()}
	 * then reports the same answer.
	 *
	 * <p>⚠️ <b>The size is a parameter for the same reason: the frame target has to BE the
	 * drawing area.</b> A viewport fixed at 765x503 declined every frame in every
	 * configuration, because 765x503 is neither the fixed mode's 512x334 nor the resizable
	 * mode's frame-minus-sidebar-and-title geometry. So the caller states what it is about
	 * to draw, and an implementation that already exists at a different size restates its
	 * frame target rather than refusing - which is what a window resize or a mode switch
	 * looks like from here.
	 *
	 * <p>Idempotent apart from the restate: a failure is remembered rather than retried, so
	 * a broken driver costs one message rather than one per frame.
	 *
	 * @param width  the drawing area's width in pixels, {@code > 0}
	 * @param height the drawing area's height in pixels, {@code > 0}
	 * @return {@code true} if triangles can be drawn at that size this frame
	 */
	boolean ensure(int width, int height);

	/** Whether a frame target exists at all - the driver, the program and the framebuffer. */
	boolean ready();

	/**
	 * The colour a frame should be cleared to: the scene background.
	 *
	 * <p><b>Why this is on the seam rather than read by the caller.</b> The value depends on
	 * {@code client.fogStrength} (see {@code Fog.sceneBackgroundRgb}), and loading the
	 * {@code client} class is NOT headless-safe - its static initialisers need a display -
	 * so a renderer that read it directly could not be driven in the harness at all. Behind
	 * the seam there is still exactly ONE derivation of it ({@code Fog} owns the curve and
	 * the fog-off branch), while a test double can answer without loading anything.
	 */
	int sceneBackground();

	/** A one-line summary for the console, whether it succeeded or not. */
	String describe();
}
