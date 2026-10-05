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
}
