package ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.Properties;

import game.client;

/**
 * RuneLite-inspired Stretched Mode: play the fixed 765x503 layout in a larger window.
 *
 * <p><b>What it is here.</b> The client has two layouts: a fixed 765x503 one drawn at
 * absolute coordinates, and a resizable one that anchors every panel to the window. Stretched
 * Mode keeps the fixed layout - everything stays where the fixed screenshots put it - but lets
 * the window grow and scales the whole rendered frame up to fill it. The HUD, chatbox, tabs and
 * game viewport all get bigger together, which is what a player actually wants on a large
 * monitor: the fixed arrangement, just larger.
 *
 * <p><b>How the scale is applied.</b> Every visible pixel is painted through
 * {@code super.graphics}, so one transform on that object scales the whole frame at once:
 * {@link #beginFrame} installs a translate+scale before a frame draws and {@link #endFrame}
 * puts the original transform back. Nothing below this class needs to know - {@code Renderer}'s
 * blits and the handful of direct text draws are all affected by the same transform, and the
 * drawing areas in {@code DrawingArea} write to {@code int[]} buffers, which are not.
 *
 * <p><b>Input.</b> Because the frame is scaled but the game still thinks in 765x503, mouse
 * coordinates are divided by the same scale in {@link #toGameX}/{@link #toGameY} before the
 * client ever sees them. Without this the cursor would be right but every click would land
 * somewhere else.
 *
 * <p><b>Settings.</b> {@code scalePercent} multiplies the fill scale: 100 leaves the frame
 * exactly as large as the window allows, lower values letterbox it, higher values crop in for a
 * zoomed look. {@code keepAspect} forces one scale on both axes so circles stay round.
 *
 * <p>Fully contained here: enabling it only changes the window size and the frame transform, and
 * turning it off restores the 765x503 window, so it cannot leave the client in a half-scaled
 * state.
 */
public final class StretchedMode {

	/** The layout the fixed frame is authored at; the scale is relative to this pair. */
	public static final int BASE_W = 765;
	public static final int BASE_H = 503;

	public static boolean enabled;
	/** One scale for both axes, so the frame is never stretched out of proportion. */
	public static boolean keepAspect = true;
	/** Multiplier on the fill scale, as a percentage. 100 fills the window exactly. */
	public static int scalePercent = 100;
	/** The window the player last resized to while stretched, restored when re-enabling. */
	public static int windowW = 1024;
	public static int windowH = 674;

	private static float scaleX = 1f;
	private static float scaleY = 1f;
	private static int offsetX;
	private static int offsetY;

	private StretchedMode() {
	}

	/** On only in fixed layout: the resizable layout is already window-sized. */
	public static boolean active() {
		return enabled && client.isFixed();
	}

	public static int percentMin() {
		return 50;
	}

	public static int percentMax() {
		return 200;
	}

	/** Cycles the scale in 25% steps, wrapping, for a click-to-change settings row. */
	public static void cyclePercent() {
		scalePercent += 25;
		if (scalePercent > percentMax()) {
			scalePercent = percentMin();
		}
	}

	/**
	 * Recomputes the frame transform from the component size.
	 *
	 * <p>Called once per frame, before anything is drawn, so a window drag takes effect
	 * immediately. A component smaller than the base is left at 1:1 rather than shrunk - the frame
	 * has to be at least readable, and the window cannot go below 765x503 anyway.
	 */
	public static void measure(int compW, int compH) {
		if (!active() || compW <= 0 || compH <= 0) {
			scaleX = 1f;
			scaleY = 1f;
			offsetX = 0;
			offsetY = 0;
			return;
		}
		float fillX = (float) compW / BASE_W;
		float fillY = (float) compH / BASE_H;
		float percent = scalePercent / 100f;
		if (keepAspect) {
			float s = Math.min(fillX, fillY) * percent;
			if (s < 0.5f) {
				s = 0.5f;
			}
			scaleX = s;
			scaleY = s;
		} else {
			scaleX = Math.max(0.5f, fillX * percent);
			scaleY = Math.max(0.5f, fillY * percent);
		}
		offsetX = (int) ((compW - BASE_W * scaleX) / 2f);
		offsetY = (int) ((compH - BASE_H * scaleY) / 2f);
	}

	/**
	 * Installs the stretch transform on the frame's Graphics, returning the transform to restore.
	 *
	 * @return the saved transform, or null when nothing was changed and {@link #endFrame} is a no-op
	 */
	public static AffineTransform beginFrame(Graphics g) {
		if (!(g instanceof Graphics2D) || !active()) {
			return null;
		}
		Graphics2D g2 = (Graphics2D) g;
		AffineTransform saved = g2.getTransform();
		g2.translate(offsetX, offsetY);
		g2.scale(scaleX, scaleY);
		return saved;
	}

	/** Puts the frame's transform back. Safe with a null saved transform. */
	public static void endFrame(Graphics g, AffineTransform saved) {
		if (saved != null && g instanceof Graphics2D) {
			((Graphics2D) g).setTransform(saved);
		}
	}

	/** Window -> game x. Identity unless stretched. */
	public static int toGameX(int x) {
		if (!active() || scaleX <= 0f) {
			return x;
		}
		return Math.round((x - offsetX) / scaleX);
	}

	/** Window -> game y. Identity unless stretched. */
	public static int toGameY(int y) {
		if (!active() || scaleY <= 0f) {
			return y;
		}
		return Math.round((y - offsetY) / scaleY);
	}

	/** The window size the client component should be given while stretched. */
	public static int canvasW(int fallback) {
		return active() ? Math.max(BASE_W, windowW) : fallback;
	}

	public static int canvasH(int fallback) {
		return active() ? Math.max(BASE_H, windowH) : fallback;
	}

	/** Remembers a window size the player dragged to, so it survives a restart. */
	public static void remember(int compW, int compH) {
		if (active() && compW >= BASE_W && compH >= BASE_H) {
			windowW = compW;
			windowH = compH;
		}
	}

	public static void load(Properties props) {
		enabled = readBool(props, "stretchedMode", false);
		keepAspect = readBool(props, "stretchedAspect", true);
		scalePercent = clamp(readInt(props, "stretchedPercent", 100), percentMin(), percentMax());
		windowW = Math.max(BASE_W, readInt(props, "stretchedW", 1024));
		windowH = Math.max(BASE_H, readInt(props, "stretchedH", 674));
	}

	public static void save(Properties props) {
		props.setProperty("stretchedMode", Boolean.toString(enabled));
		props.setProperty("stretchedAspect", Boolean.toString(keepAspect));
		props.setProperty("stretchedPercent", Integer.toString(scalePercent));
		props.setProperty("stretchedW", Integer.toString(windowW));
		props.setProperty("stretchedH", Integer.toString(windowH));
	}

	private static int clamp(int value, int min, int max) {
		if (value < min) {
			return min;
		}
		if (value > max) {
			return max;
		}
		return value;
	}

	private static int readInt(Properties props, String key, int fallback) {
		try {
			return Integer.parseInt(props.getProperty(key, Integer.toString(fallback)));
		} catch (Exception e) {
			return fallback;
		}
	}

	private static boolean readBool(Properties props, String key, boolean fallback) {
		String value = props.getProperty(key);
		return value == null ? fallback : Boolean.parseBoolean(value.trim());
	}
}
