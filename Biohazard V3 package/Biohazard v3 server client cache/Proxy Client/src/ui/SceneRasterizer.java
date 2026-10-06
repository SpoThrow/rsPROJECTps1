package ui;

import model.Model;

/**
 * The scene-rasterisation seam (Phase 4.1c-2b).
 *
 * <p><b>What this is.</b> {@link Model#method443} is the point at which the 3D
 * scene submits a model for drawing: one call means "draw this model, placed
 * here, with this camera rotation" - the method loops over the model's own
 * triangles internally. There are 22 call sites and they share a single shape:
 * 21 inside {@code scene.WorldController.method314} (the per-tile scene drawer),
 * plus one delegating wrapper at {@code model.Animable.method443}. This class
 * names that operation so it can be replaced in one place.
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
 * <p><b>The GROUND is covered too (4.1c-2c), so the scene rasteriser is now seamed
 * as a whole.</b> The ground never routes through {@code method443} - it is drawn
 * by {@code WorldController.method315} (the {@code Class43} floor mesh) and
 * {@code WorldController.method316} (the {@code Class40} overlay mesh), which call
 * the triangle rasterisers directly - so it is handled by the separate
 * {@link #dispatchGroundTriangle} operation below, hooked in three places.
 *
 * <p><b>Coverage boundary, stated so it is not over-read.</b> This covers scene
 * <i>submission</i> - models and ground - which is what a GPU path needs. It is not
 * a claim that every rasteriser entry point is seamed:
 * {@code WorldController.method319}/{@code method321}/{@code method305-309}/
 * {@code method323-324} and {@code Model}'s own triangle helpers
 * ({@code method472}, {@code method479}, {@code method483}, {@code method485},
 * {@code method480-486}) are untouched.
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

		/**
		 * Draw a model submitted by the scene.
		 *
		 * @return {@code true} if this rasteriser handled it, so the caller must NOT
		 *         run its software body; {@code false} to DECLINE, in which case the
		 *         caller falls through to software. Declining is what makes a partial
		 *         renderer safe - mirroring {@code GlPresent.presentGame} and
		 *         {@link GpuRenderer.Implementation#presentGameFrame}, which return
		 *         {@code false} for the same reason.
		 */
		boolean drawModel(Model model, int orientation, int camA, int camB, int camC, int camD,
				int dx, int dy, int dz, int uid);

		/**
		 * A GROUND triangle submission, already projected to screen space.
		 *
		 * <p>One call = "draw this projected triangle", mirroring what the software
		 * rasteriser receives at each of the ground's call sites. The arguments are the
		 * same values, in the same order, that {@code Texture.method374}/{@code method378}
		 * are given - so a rasteriser sees exactly what the software path sees.
		 *
		 * <p>The variant is conveyed rather than hidden, because the software path
		 * branches on it: {@code textureId == -1} means untextured, otherwise the
		 * triangle is textured via {@code t0..t8}. {@code Texture.lowMem} is global and
		 * still readable, so the third (low-detail textured) variant is disambiguated too.
		 *
		 * <p>⚠ <b>The nine values are carried exactly as the software rasteriser receives them,
		 * and Phase 7.2b-2h found that this is not one canonical triple - the two hooks pass
		 * DIFFERENT things, because the software itself does.</b> Their layout is
		 * {@code Texture.method378}'s own: three planes of three, {@code s0..s8}, which
		 * {@code method379} divides per pixel. The planes are <i>not</i> in a single vertex
		 * order - in the flat set the {@code u} plane is {@code (i2,i3,l1)} while the
		 * {@code w} plane is {@code (k2,j2,j3)}, i.e. two different orderings of the same
		 * three corners. Do not normalise one into the other when consuming this: the
		 * software's arithmetic depends on the layout as given.
		 *
		 * <p>⚠⚠ <b>Two subtleties a consumer must not paper over.</b> (1) The first hook's
		 * software fallback branches on {@code flatMesh} and the two branches use genuinely
		 * different variables for the same vertex ({@code i2} vs {@code l2} and so on), so
		 * the seam repeats that branch rather than assuming an equivalent set - the two are
		 * not the same set reordered. (2) The second hook's software fallback has NO such
		 * branch and always passes the flat set, even for a non-flat tile; the seam therefore
		 * also passes the flat set there unconditionally, which is faithful to the software
		 * but is worth knowing before trusting these coordinates on non-flat tiles.
		 *
		 * <p>Consequence for a listener: these values are an input description, not a
		 * semantic {@code (u,v,w)} triple you can reorder. Mirror the software.
		 *
		 * <p>⚠⚠ <b>{@code depth} is the CAMERA-SPACE DISTANCE the software itself fogs
		 * this tile with, and it is carried rather than derived (Phase 7.2c-2).</b> It is
		 * the drawer's own {@code Fog.sceneDepth} at the instant it calls the rasteriser -
		 * {@code (k2 + j2 + k3 + j3) / 4}, the mean of the tile's four post-rotation corner
		 * depths, for {@code method315}; {@code (dx + dy) * 96 + 300} for {@code method316}'s
		 * overlay mesh. It is <b>absolute</b> (distance from the camera origin), in the same
		 * fixed-point units as {@code Model.method443}'s per-vertex camera depths - which is
		 * what lets a listener depth-composite ground and models on one axis.
		 *
		 * <p>⚠ <b>It is a per-TILE quantity, not per-vertex, and that mirrors the
		 * software's own granularity</b> - {@code method315} submits a whole tile as a unit
		 * in painter order and fogs it with the one mean depth. A listener that needs a
		 * gradient across the tile must take it from the {@code w} plane of the nine rather
		 * than from here; a listener reproducing the software's fog wants exactly this.
		 *
		 * <p>⚠ <b>Do not re-derive it from the nine.</b> The {@code w} plane is the
		 * per-vertex form of the same quantity, but its ORDERING is not one vertex order and
		 * its mean is not this number; the two are consistent, not interchangeable.
		 *
		 * @param x0,y0..x2,y2 the projected screen triangle
		 * @param colour0..2   the 16-bit model face colours (palette indices)
		 * @param textureId    the texture, or {@code -1} when untextured
		 * @param flatMesh     the mesh's flat flag, as the software's first-hook branch reads it
		 * @param t0..t8       the nine camera-space values used for texture mapping, in the
		 *                     order the software rasteriser receives them
		 * @param depth        the tile's absolute camera-space depth, {@code Fog.sceneDepth}
		 *                     at the call site - see above
		 * @return {@code true} if handled, {@code false} to DECLINE and let the caller
		 *         fall through to software
		 */
		boolean drawGroundTriangle(int x0, int y0, int x1, int y1, int x2, int y2,
				int colour0, int colour1, int colour2, int textureId, boolean flatMesh,
				int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7, int t8,
				int depth);
	}

	private static Implementation implementation;

	private SceneRasterizer() {
	}

	/**
	 * Install the rasteriser that will handle scene models.
	 *
	 * <p>Package-private on purpose: {@link GpuRenderer#install} is the single install
	 * point, so a scene rasteriser cannot be installed independently of the present
	 * half. The same enforcement pattern as {@code RSImageProducer.drawGraphics}.
	 */
	static void install(Implementation impl) {
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
	 *         is no rasteriser installed OR the installed one declined, in which
	 *         case the caller must draw it itself.
	 */
	public static boolean dispatch(Model model, int orientation, int camA, int camB, int camC,
			int camD, int dx, int dy, int dz, int uid) {
		Implementation impl = implementation;
		if (impl == null) {
			return false;
		}
		return impl.drawModel(model, orientation, camA, camB, camC, camD, dx, dy, dz, uid);
	}

	/**
	 * Ground (landscape) triangle seam - Phase 4.1c-2c.
	 *
	 * <p>Why the model seam was not enough: the ground never routes through
	 * {@code Model.method443}. It is drawn by {@code WorldController.method315} (the
	 * {@code Class43} floor mesh) and {@code WorldController.method316} (the
	 * {@code Class40} overlay mesh), which call the triangle rasterisers directly.
	 *
	 * <p>Why the hook sits mid-loop rather than at the top of those methods: unlike
	 * {@code method443}, both of them interleave tile picking with drawing inside the
	 * same triangle loop, so an early return would silently drop tile markers and
	 * hover once a rasteriser is installed.
	 *
	 * <p><b>The {@code depth} argument (Phase 7.2c-2).</b> Both drawers already compute a
	 * camera-space depth for the tile - it is the value they hand the software rasteriser as
	 * its fog distance - so it is threaded through here rather than reconstructed by a
	 * listener. See {@link Implementation#drawGroundTriangle} for what it is and what it is
	 * not.
	 */
	public static boolean dispatchGroundTriangle(int x0, int y0, int x1, int y1, int x2, int y2,
			int colour0, int colour1, int colour2, int textureId, boolean flatMesh,
			int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7, int t8, int depth) {
		Implementation impl = implementation;
		if (impl == null) {
			return false;
		}
		return impl.drawGroundTriangle(x0, y0, x1, y1, x2, y2, colour0, colour1, colour2,
				textureId, flatMesh, t0, t1, t2, t3, t4, t5, t6, t7, t8, depth);
	}
}
