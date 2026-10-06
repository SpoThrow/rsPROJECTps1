package ui;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import model.Model;
import sign.signlink;

/**
 * Selects the renderer from {@code client_settings.properties} (Phase 4.2b).
 *
 * <p><b>Why this exists.</b> Phase 4.2 asks for the renderer to be selectable at
 * runtime behind a setting, defaulting to software, reusing the existing
 * {@code client_settings.properties} mechanism rather than inventing a second
 * config path. This reads that property and installs the result through
 * {@link GpuRenderer#install}, which is the single install point 4.2a enforced.
 *
 * <p><b>⚠️ Not the {@code openGl} key, and this is the trap to avoid.</b>
 * {@code openGl} / {@code client.openGlEnabled} already exist and select
 * <i>Java2D's own</i> pipeline via {@code sun.java2d.opengl} system properties -
 * a different thing entirely from selecting a renderer of ours. So this uses its
 * own key, {@value #PROPERTY}. Conflating the two would make the name lie.
 *
 * <p><b>Defaults to software, which is what makes the step safe.</b> An absent key,
 * an unreadable file or an unrecognised value all select software, so a cache that
 * has never seen this property behaves exactly as it did before the property
 * existed.
 *
 * <p><b>No GL code anywhere in Phase 4.</b> There is no renderer to select yet, so
 * a non-software value installs {@link #PLACEHOLDER_NAME} - a renderer that DECLINES
 * everything and therefore changes nothing. That exercises the real load -> select ->
 * install path end to end without pretending a GPU path exists. It is deliberately
 * not a no-op stub that swallows work: see {@link DecliningRenderer}.
 *
 * <p><b>Phase 7.2a adds one more recognised value, {@value #GL_NAME}.</b> It installs
 * {@link GlSceneRenderer}, which brings the offscreen context up but still declines
 * every operation - so it, too, changes no pixel. Any other non-software value still
 * installs {@link DecliningRenderer}. The default is untouched: an absent key is
 * still software, so the software path remains both the default and the fallback
 * (7.3).
 */
public final class RendererConfig {

	/** The property name. Deliberately NOT {@code openGl} - see the class doc. */
	public static final String PROPERTY = "renderer";

	/** The default and always-safe selection. */
	public static final String SOFTWARE = "software";

	/** The bring-up selection: installs a renderer that declines everything. */
	public static final String PLACEHOLDER_NAME = "placeholder";

	/**
	 * The GL selection (Phase 7.2a). Installs {@link GlSceneRenderer}, which brings the
	 * offscreen context up on the game thread and - for now - still declines every
	 * operation, so this arm renders identically to software until 7.2b.
	 */
	public static final String GL_NAME = "gl";

	private static String requested = SOFTWARE;

	private RendererConfig() {
	}

	/**
	 * Reads the setting and installs the renderer it names.
	 *
	 * <p>Safe to call more than once; each call re-reads the property.
	 */
	public static void select() {
		requested = readRequested();
		if (SOFTWARE.equalsIgnoreCase(requested)) {
			GpuRenderer.install(null);
			return;
		}
		if (GL_NAME.equalsIgnoreCase(requested)) {
			GpuRenderer.install(new GlSceneRenderer(requested));
			return;
		}
		GpuRenderer.install(new DecliningRenderer(requested));
	}

	/** The selection as requested by the property, after the last {@link #select()}. */
	public static String requestedName() {
		return requested;
	}

	/** Whether the last {@link #select()} left the software path in place. */
	public static boolean isSoftware() {
		return GpuRenderer.implementation() == null;
	}

	private static String readRequested() {
		try {
			File propsFile = new File(signlink.findcachedir() + "client_settings.properties");
			if (!propsFile.exists()) {
				return SOFTWARE;
			}
			Properties props = new Properties();
			FileInputStream in = new FileInputStream(propsFile);
			props.load(in);
			in.close();
			String v = props.getProperty(PROPERTY);
			if (v == null) {
				return SOFTWARE;
			}
			v = v.trim();
			if (v.isEmpty()) {
				return SOFTWARE;
			}
			return v;
		} catch (Exception e) {
			// Unreadable config must never cost the player their renderer.
			return SOFTWARE;
		}
	}

	/**
	 * A renderer that declines every operation, so installing it changes no pixel.
	 *
	 * <p>⚠️ <b>Why it declines rather than doing nothing.</b> The scene seam treats a
	 * non-null implementation as authoritative unless that implementation says
	 * otherwise: {@code Model.method443} returns early when the dispatch reports
	 * {@code true}, and the ground hooks skip their software branch the same way. A
	 * placeholder that accepted submissions without drawing them would therefore make
	 * MODELS AND GROUND DISAPPEAR - exactly the failure this class exists to avoid. So
	 * every method returns {@code false} / declines, and the software path runs
	 * untouched.
	 *
	 * <p>It logs once per operation kind rather than per call, because these run
	 * thousands of times a second.
	 */
	public static final class DecliningRenderer implements GpuRenderer.Implementation {

		private final String name;
		private boolean reportedPresent;
		private boolean reportedScene;

		public DecliningRenderer(String name) {
			this.name = name;
		}

		public boolean presentGameFrame(RSImageProducer producer, int destX, int destY) {
			if (!reportedPresent) {
				reportedPresent = true;
				System.out.println("Renderer '" + name + "' is selected but not implemented; "
						+ "presenting in software.");
			}
			return false;
		}

		public boolean drawModel(Model model, int orientation, int camA, int camB, int camC,
				int camD, int dx, int dy, int dz, int uid) {
			reportScene();
			return false;
		}

		public boolean drawGroundTriangle(int x0, int y0, int x1, int y1, int x2, int y2,
				int colour0, int colour1, int colour2, int textureId, boolean flatMesh,
				int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7, int t8,
				int depth) {
			reportScene();
			return false;
		}

		public boolean sceneFinished(RSImageProducer producer) {
			// Nothing was taken from the scene, so there is nothing to composite back.
			return false;
		}

		private void reportScene() {
			if (!reportedScene) {
				reportedScene = true;
				System.out.println("Renderer '" + name + "' is selected but not implemented; "
						+ "scene is drawn in software.");
			}
		}
	}
}
