package ui;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * The offscreen OpenGL context (Phase 7.2a).
 *
 * <p><b>What this owns.</b> One hidden GLFW window's OpenGL context and one
 * framebuffer object (RGBA8 colour plus a depth24 renderbuffer) that the GL scene
 * renderer draws into. Nothing here draws; this is the lifecycle the rest of
 * Phase 7 depends on, kept separate so it can be verified on its own.
 *
 * <p><b>Why offscreen, and why the context is created on the GAME THREAD.</b>
 * Phase 7.1 chose offscreen over {@code AWTGLCanvas}; see the plan for the full
 * argument. The threading half is settled by how the client already runs:
 * {@code client} extends {@code RSApplet}, which {@code implements Runnable} and is
 * started with {@code startRunnable(this, 1)} - so the game loop is its OWN thread,
 * and {@code paint(Graphics)} on the EDT is where the software present happens.
 * Scene submission ({@link SceneRasterizer#dispatch}) therefore runs on the game
 * thread, so the context is created there, lazily, on first use. Every later GL
 * call happens on that same thread and the EDT never touches GL at all - which is
 * why there is no cross-thread context juggling to get wrong.
 *
 * <p><b>Failure is a DECLINE, never a crash.</b> The failure modes here are real and
 * varied: the LWJGL 3 jars may be absent (the classpath {@code Run.bat} controls),
 * the natives may fail to load on a machine with no driver, the context or the FBO
 * may not be creatable. All of them are caught and recorded, and {@link #ensure()}
 * reports {@code false} - so the renderer falls back to the software path rather
 * than taking the client down. This mirrors the defensive posture of Phase 2.
 *
 * <p><b>Why the harness can test this headlessly.</b> {@code deps/lwjgl3} is
 * deliberately NOT on the harness classpath, so in the harness this class takes its
 * degradation path - which is exactly the path that matters for robustness. The
 * successful path needs a real GPU and is the live gate's job (7.4).
 */
public final class GlScene {

	/**
	 * The size the frame target starts at, before anything has said what the drawing area
	 * is. ⚠️ <b>NOT the live size, and no longer assumed to be "the fixed game area"</b> -
	 * that assumption was wrong in both modes and is what the live gate refuted (2026-10-06).
	 * The real size arrives as the parameter of {@link #ensure(int, int)}; see its doc.
	 */
	public static final int DEFAULT_WIDTH = 765;
	public static final int DEFAULT_HEIGHT = 503;

	private static boolean attempted;
	private static boolean available;
	private static String unavailableReason;
	private static String version = "";
	private static String rendererName = "";

	private static long window;
	private static int framebuffer;
	private static int colourBuffer;
	private static int depthBuffer;
	private static int width = DEFAULT_WIDTH;
	private static int height = DEFAULT_HEIGHT;

	private GlScene() {
	}

	/**
	 * Creates the frame target AT THE SIZE THE CALLER IS ABOUT TO DRAW, and reports whether
	 * it is usable.
	 *
	 * <p>⚠️ <b>The size is a PARAMETER because the constant it replaced could never
	 * match - and the live gate is what proved it (2026-10-06).</b> The old viewport was
	 * {@code DEFAULT_WIDTH/HEIGHT} (765x503), on the assumption that this was "the fixed
	 * game area". It is not: in FIXED mode the game raster is 512x334, and in RESIZABLE
	 * mode {@code Jframe.setCanvasSize} sizes the FRAME to {@code width +
	 * PluginSidebar.eastWidth() + insets} by {@code height + TitleBar.barHeight() + insets}
	 * and the game component then STRETCHES to fill it - so the drawing area is at least
	 * 907x666 and grows with the window. <b>Neither mode, at any window size, yields
	 * 765x503</b>, so the size gate in {@code GlSceneRenderer} declined every frame,
	 * permanently, in every configuration. A live client printed it: {@code GL viewport
	 * 765x503 does not match the drawing area 907x666}.</p>
	 *
	 * <p>Brings up on the first call, and RESTATES the frame target on a later call whose
	 * drawing area differs - which is what a window resize or a fixed/resizable switch looks
	 * like from here. Nothing draws to the window's own framebuffer (the hidden window
	 * exists only to own the context), so a restate is two {@code glRenderbufferStorage}
	 * calls and a completeness check, not a context rebuild.</p>
	 *
	 * <p>Must be called from the GAME THREAD - see the class doc. The context is left
	 * current on the calling thread, which is what makes later draws from that thread
	 * work without a second {@code makeContextCurrent}.
	 *
	 * @param width  the drawing area's width in pixels, {@code > 0}
	 * @param height the drawing area's height in pixels, {@code > 0}
	 * @return {@code true} if a context and framebuffer are ready to draw into at that size
	 */
	public static synchronized boolean ensure(int width, int height) {
		if (attempted) {
			if (!available) {
				return false;
			}
			return restate(width, height);
		}
		attempted = true;
		try {
			create(width, height);
			available = true;
		} catch (Throwable t) {
			// Deliberately Throwable, not Exception: the realistic failures here include
			// NoClassDefFoundError (the LWJGL 3 jars are absent) and UnsatisfiedLinkError
			// (the natives cannot load). Both must degrade to software, not kill the client.
			available = false;
			unavailableReason = describe(t);
			destroyQuietly();
		}
		return available;
	}

	/**
	 * Re-states the frame target's size, and reports whether it is complete afterwards.
	 *
	 * <p>Declines rather than throws. A failure here is deliberately TERMINAL for the
	 * session: the FBO's attachments would be in an unknown state, and rebuilding the
	 * context to recover is not worth the risk while the software path remains the
	 * fallback the client keeps.
	 */
	private static boolean restate(int newWidth, int newHeight) {
		if (newWidth <= 0 || newHeight <= 0) {
			return false;
		}
		if (newWidth == width && newHeight == height) {
			return true;
		}
		try {
			GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, colourBuffer);
			GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_RGBA8, newWidth, newHeight);
			GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthBuffer);
			GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH_COMPONENT24, newWidth,
					newHeight);
			width = newWidth;
			height = newHeight;
			GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
			if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE) {
				return true;
			}
			unavailableReason = "the frame target is incomplete at " + newWidth + "x" + newHeight;
		} catch (Throwable t) {
			unavailableReason = describe(t);
		}
		available = false;
		return false;
	}

	private static void create(int initialWidth, int initialHeight) {
		width = initialWidth;
		height = initialHeight;
		if (!GLFW.glfwInit()) {
			throw new IllegalStateException("glfwInit returned false");
		}
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);

		window = GLFW.glfwCreateWindow(width, height, "biohazard-gl", 0L, 0L);
		if (window == 0L) {
			throw new IllegalStateException("glfwCreateWindow returned 0 (hidden window)");
		}
		GLFW.glfwMakeContextCurrent(window);
		GL.createCapabilities();

		version = GL11.glGetString(GL11.GL_VERSION);
		rendererName = GL11.glGetString(GL11.GL_RENDERER);
		createFramebuffer();
	}

	private static void createFramebuffer() {
		framebuffer = GL30.glGenFramebuffers();
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);

		colourBuffer = GL30.glGenRenderbuffers();
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, colourBuffer);
		GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_RGBA8, width, height);
		GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
				GL30.GL_RENDERBUFFER, colourBuffer);

		depthBuffer = GL30.glGenRenderbuffers();
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthBuffer);
		GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH_COMPONENT24, width, height);
		GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
				GL30.GL_RENDERBUFFER, depthBuffer);

		int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
		if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
			throw new IllegalStateException("framebuffer incomplete: 0x" + Integer.toHexString(status));
		}
	}

	/** Whether {@link #ensure()} has been called - true even when it failed. */
	public static boolean attempted() {
		return attempted;
	}

	/** Whether a context and framebuffer are ready to draw into. */
	public static boolean available() {
		return available;
	}

	/**
	 * The framebuffer object scene rendering targets, or {@code 0} when unavailable.
	 *
	 * <p>Package-private: {@link GlBatcher} is the only thing that binds it, and the
	 * draw path is meant to go through the batcher rather than reach into the scene.
	 */
	static int framebufferId() {
		return framebuffer;
	}

	/** Why the context is unavailable, or {@code null} when it is available. */
	public static String unavailableReason() {
		return unavailableReason;
	}

	public static int width() {
		return width;
	}

	public static int height() {
		return height;
	}

	/** A one-line summary for the console, whether it succeeded or not. */
	public static String describe() {
		if (available) {
			return "offscreen " + width + "x" + height + ", GL_VERSION=" + version
					+ ", GL_RENDERER=" + rendererName;
		}
		return "unavailable (" + unavailableReason + ")";
	}

	/** Releases the context. Nothing calls this yet; 7.2b decides the shutdown point. */
	public static synchronized void dispose() {
		destroyQuietly();
		available = false;
	}

	private static void destroyQuietly() {
		try {
			if (window != 0L) {
				GLFW.glfwDestroyWindow(window);
			}
		} catch (Throwable ignored) {
			// Best effort: a failure here must not mask the original reason.
		}
		window = 0L;
		framebuffer = 0;
		colourBuffer = 0;
		depthBuffer = 0;
	}

	private static String describe(Throwable t) {
		String message = t.getMessage();
		return t.getClass().getName() + (message == null ? "" : ": " + message);
	}
}
