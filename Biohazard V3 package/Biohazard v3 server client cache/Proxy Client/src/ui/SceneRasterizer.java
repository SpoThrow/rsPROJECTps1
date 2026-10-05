package ui;

import model.Model;

/**
 * The scene-rasterisation seam (Phase 4.1c-2b).
 *
 * <p><b>What this is.</b> {@link Model#method443} is the point at which the 3D
 * scene submits a model for drawing: one call means "draw this model, placed
 * here, with this camera rotation" - the method loops over the model's own
 * triangles internally. There are 22 call sites, all inside
 * {@code scene.WorldController.method313}, and they share a single shape. This
 * class names that operation so it can be replaced in one place.
 *
 * <p><b>Why the dispatch is at the TOP of method443 rather than at the 22 call
 * sites.</b> Routing each call site would mean editing 22 lines, and a site
 * missed would silently keep a software-only path. An early dispatch inside
 * {@code method443} covers every caller - now and in future - and changes zero
 * call sites. It also mirrors the pattern already established for the present
 * seam: {@code Renderer.presentGameFrame} asks {@link GlPresent} first and falls
 * through to the software blit.
 *
 * <p><b>This is a seam, not a change.</b> No implementation is installed by
 * default, so {@link #dispatch} returns {@code false} and {@code method443}
 * runs its existing body unmodified - not moved, not wrapped, not renamed. The
 * 4.1c-2a framebuffer hash must stay unmoved, and that is the proof this step is
 * behaviour-neutral. No GL code is written anywhere in Phase 4.
 *
 * <p><b>Known gap, stated rather than implied.</b> This seam covers MODELS:
 * objects, walls, roofs, NPCs and players. It does NOT cover the ground, because
 * {@code WorldController.method316} draws the landscape mesh with 11 direct
 * rasteriser calls and never routes through {@code method443}. A GPU path needs
 * both, so the ground is a separate step (4.1c-2c) and the scene rasteriser
 * should not be described as seamed until that is done too.
 */
public final class SceneRasterizer {

	/**
	 * A swappable scene rasteriser.
	 *
	 * <p>The arguments mirror {@link Model#method443} exactly and are named after
	 * what they mean at the call sites rather than after their positions there:
	 * {@code camA..camD} are the current plane's camera sin/cos rotation
	 * ({@code WorldController.anInt458..anInt461}, which come from
	 * {@code Model.modelIntArray1/2}), {@code dx/dy/dz} are the model's position
	 * minus the camera origin, and {@code uid} is 0 for anything not
	 * mouse-pickable.
	 */
	public interface Implementation {

		void drawModel(Model model, int orientation, int camA, int camB, int camC, int camD,
				int dx, int dy, int dz, int uid);
	}

	private static Implementation implementation;

	private SceneRasterizer() {
	}

	/** Install the rasteriser that will handle scene models. */
	public static void install(Implementation impl) {
		implementation = impl;
	}

	/** The installed rasteriser, or {@code null} when the software path is in use. */
	public static Implementation implementation() {
		return implementation;
	}

	/**
	 * Offers a model to the installed rasteriser.
	 *
	 * @return {@code true} if a rasteriser handled it - in which case the caller
	 *         must return without running its own body - or {@code false} if there
	 *         is no rasteriser installed and the caller must draw it itself.
	 */
	public static boolean dispatch(Model model, int orientation, int camA, int camB, int camC,
			int camD, int dx, int dy, int dz, int uid) {
		Implementation impl = implementation;
		if (impl == null) {
			return false;
		}
		impl.drawModel(model, orientation, camA, camB, camC, camD, dx, dy, dz, uid);
		return true;
	}
}
