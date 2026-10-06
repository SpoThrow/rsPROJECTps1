package ui;

import model.Model;

/**
 * The GL renderer's bring-up stage (Phase 7.2a).
 *
 * <p><b>What this does TODAY: creates the context and declines everything.</b> The
 * first scene submission is what triggers {@link GlScene#ensure()} - and that call
 * site is the whole point of this step, because {@code SceneRasterizer.dispatch} runs
 * on the GAME THREAD, which is exactly the thread {@link GlScene} requires. So this
 * brings the context up on the correct thread and proves the lifecycle in a live
 * client, while every operation still returns {@code false} and the software path
 * draws everything. <b>No pixel changes.</b>
 *
 * <p><b>Why declining rather than drawing is the honest first step.</b> The one thing
 * that must not happen in a bring-up step is a scene that half-draws - and the scene
 * seam makes that a live hazard, not a theoretical one: a renderer that ACCEPTED a
 * model without drawing it would make models disappear. Worse, a PARTIAL GL scene
 * cannot compose correctly even if it draws everything it accepts: the software path
 * interleaves ground and models per tile inside {@code WorldController.method314}, so
 * taking over only the ground would put GL's ground and the software's models in
 * different buffers with no correct merge order. The scene therefore has to be taken
 * over as a WHOLE or not at all, and "not at all" is what this step does.
 *
 * <p><b>7.2b is where it starts drawing</b> - the whole scene, into {@link
 * GlScene}'s framebuffer, read back into the software framebuffer so the UI still
 * composites on top in software.
 *
 * <p><b>Logging is once per operation kind</b>, never per call: these run thousands of
 * times a second, and a per-call log would be worse than useless.
 */
public final class GlSceneRenderer implements GpuRenderer.Implementation {

	private final String name;

	private boolean reportedPresent;
	private boolean reportedScene;

	public GlSceneRenderer(String name) {
		this.name = name;
	}

	public boolean presentGameFrame(RSImageProducer producer, int destX, int destY) {
		if (!reportedPresent) {
			reportedPresent = true;
			System.out.println("Renderer '" + name + "': present stays software (7.2a), "
					+ "so the UI composites over the scene exactly as before.");
		}
		return false;
	}

	public boolean drawModel(Model model, int orientation, int camA, int camB, int camC,
			int camD, int dx, int dy, int dz, int uid) {
		reportSceneOnce();
		return false;
	}

	public boolean drawGroundTriangle(int x0, int y0, int x1, int y1, int x2, int y2,
			int colour0, int colour1, int colour2, int textureId, boolean flatMesh,
			int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7, int t8) {
		reportSceneOnce();
		return false;
	}

	/**
	 * Brings the context up on the first scene submission - i.e. on the game thread -
	 * and says so once. A failure here is not fatal: {@link GlScene#ensure()} records
	 * why, and this renderer declines either way, so the software path carries on.
	 */
	private void reportSceneOnce() {
		if (reportedScene) {
			return;
		}
		reportedScene = true;

		boolean ready = GlScene.ensure();
		System.out.println("Renderer '" + name + "' scene context: " + GlScene.describe());
		if (ready) {
			System.out.println("Renderer '" + name + "': context is up, but 7.2a draws nothing - "
					+ "the scene is still drawn in software.");
		} else {
			System.out.println("Renderer '" + name + "': falling back to the software path.");
		}
	}
}
