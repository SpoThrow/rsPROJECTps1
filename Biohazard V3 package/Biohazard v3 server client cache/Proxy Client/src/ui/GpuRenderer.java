package ui;

/**
 * The single install point for a non-software renderer (Phase 4.2a).
 *
 * <p><b>Why this exists.</b> Phase 4.1's wording is "define a {@code GpuRenderer}
 * seam around the two things that will change: the scene/model rasterisation and
 * the present". What 4.1 actually produced is <b>two</b> seams, not one:
 * {@link Renderer} for the present and {@link SceneRasterizer} for scene
 * rasterisation. Both work and both are live-verified, but they are separate types
 * with separate install points, so "a {@code GpuRenderer} seam" was two seams
 * wearing one plan item's name. This class is the thin facade that owns and wires
 * both, so a renderer is installed once.
 *
 * <p><b>Thin, deliberately.</b> It adds no behaviour, no buffering and no second
 * copy of the software path: it holds one reference, forwards the present call, and
 * delegates the scene install to {@link SceneRasterizer}. Like both seams it fronts,
 * it is inert by default - with nothing installed {@link #presentGameFrame} returns
 * {@code false} and the caller falls through to the existing software blit. No GL
 * code is written anywhere in Phase 4.
 *
 * <p><b>The sub-region blit is deliberately NOT on this interface.</b>
 * {@link Renderer#blit} covers the minimap, chat area, tab area, login screen and
 * the fixed-mode frame pieces, which are AWT-composited overlays rather than the
 * game frame - not the thing a GPU path takes over - so they take no hook.
 *
 * <p><b>Enforcement, not convention.</b> {@link SceneRasterizer#install} is now
 * package-private, so this is the only install point callers outside {@code ui}
 * have and a second independent install cannot reappear by accident. That mirrors
 * what {@code RSImageProducer.drawGraphics} already does for {@link Renderer}.
 */
public final class GpuRenderer {

	/**
	 * A complete non-software renderer: the present <i>and</i> the scene.
	 *
	 * <p>Extends {@link SceneRasterizer.Implementation} rather than restating those
	 * operations, because a scene rasteriser already is one of the two things this
	 * owns - so the scene seam is satisfied by inheritance and cannot drift out of
	 * sync with the scene interface.
	 *
	 * <p>Implementations are expected to DECLINE what they cannot handle, and every
	 * operation here can express that: {@link #presentGameFrame} returns
	 * {@code false}, and the inherited scene operations return {@code false} too. So a
	 * partial renderer is safe - anything it declines falls through to the software
	 * path rather than disappearing.
	 */
	public interface Implementation extends SceneRasterizer.Implementation {

		/**
		 * Present the game frame.
		 *
		 * <p>Called before the pre-existing {@code GlPresent} hook, which is kept
		 * after this one so the fallback order is unchanged.
		 *
		 * @return {@code true} if it presented the frame, in which case the caller
		 *         must not blit in software; {@code false} to decline.
		 */
		boolean presentGameFrame(RSImageProducer producer, int destX, int destY);

		/**
		 * The 3D scene for this frame is finished, and {@code producer} holds it
		 * (Phase 7.2c).
		 *
		 * <p><b>Why the present is the wrong place for this, and why a second operation
		 * had to exist.</b> {@link #presentGameFrame} runs AFTER the HUD is composited
		 * into the same buffer - minimap, tabs, chat and the XP overlays all land on top
		 * of the scene first. So a renderer that read its offscreen scene back at present
		 * time would erase every one of them. The scene has its own boundary, and this
		 * names it: {@code client.method146} submits the whole 3D scene and then draws
		 * software 2D over it, and the seam sits exactly between the two.
		 *
		 * <p><b>Declining is the safe answer, and it is what a software renderer must
		 * do.</b> It is called once per frame, whether or not the renderer took the
		 * scene.
		 *
		 * <p>⚠ Abstract rather than a {@code default} returning {@code false}, and the
		 * reason is not the Java level - this tree targets 8, so a default method would
		 * compile. It is that the frame boundary is the thing a renderer either takes or
		 * does not, and inheriting an answer would let a future renderer skip the question
		 * entirely. Declaring it makes {@code return false} an explicit "I draw in
		 * software, there is nothing to composite back" rather than a default nobody reads.
		 *
		 * @param producer the buffer the scene was drawn into - the same one the HUD is
		 *                 about to be composited onto
		 * @return {@code true} if the renderer replaced the scene in {@code producer}
		 */
		boolean sceneFinished(RSImageProducer producer);
	}

	private static Implementation implementation;

	private GpuRenderer() {
	}

	/**
	 * Install the renderer. Passing {@code null} restores the software path for both
	 * seams at once, so a caller cannot leave one half installed by accident.
	 */
	public static void install(Implementation impl) {
		implementation = impl;
		SceneRasterizer.install(impl);
	}

	/** The installed renderer, or {@code null} when the software path is in use. */
	public static Implementation implementation() {
		return implementation;
	}

	/**
	 * Offers the game frame to the installed renderer.
	 *
	 * @return {@code true} if a renderer presented it - in which case the caller must
	 *         return without running its own blit - or {@code false} if there is none
	 *         installed and the caller must present it itself.
	 */
	public static boolean presentGameFrame(RSImageProducer producer, int destX, int destY) {
		Implementation impl = implementation;
		if (impl == null) {
			return false;
		}
		return impl.presentGameFrame(producer, destX, destY);
	}

	/**
	 * Tells the installed renderer that this frame's 3D scene is complete (Phase 7.2c).
	 *
	 * <p>Called from the game loop's scene-finished seam, between the scene submission and
	 * the software 2D drawn over it - see
	 * {@link Implementation#sceneFinished(RSImageProducer)} for why the present is the
	 * wrong boundary.
	 *
	 * @return {@code true} if a renderer replaced the scene in {@code producer}, or
	 *         {@code false} if there is none installed, or it declined
	 */
	public static boolean sceneFinished(RSImageProducer producer) {
		Implementation impl = implementation;
		if (impl == null) {
			return false;
		}
		return impl.sceneFinished(producer);
	}
}
