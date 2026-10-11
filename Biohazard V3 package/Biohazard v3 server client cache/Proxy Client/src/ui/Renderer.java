package ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * The present seam (Phase 4.1a).
 *
 * <p>Before this class existed, every call site put its buffer on screen by
 * calling {@link RSImageProducer#drawGraphics} itself. That made "the
 * framebuffer" and "the thing that puts it on screen" the same object, and left
 * the swap point for a future GPU path implicit and duplicated across ten call
 * sites. {@code Renderer} names the operation instead, so the present can be
 * replaced in one place without a single call site changing.
 *
 * <p>This is a seam, not a change. Behaviour is deliberately identical to
 * calling {@code drawGraphics} directly: the software path below is the same
 * blit in the same order, the existing GPU hook
 * ({@link GlPresent#presentGame}) is still asked first and still returns
 * {@code false}, and no GL code is written anywhere in Phase 4.1.
 *
 * <p>{@code RSImageProducer.drawGraphics} is now package-private so this class
 * is enforced as the only way to present a buffer, rather than merely being
 * documented as such.
 */
public final class Renderer {

	private Renderer() {
	}

	/**
	 * Software blit of one buffer to ({@code destX}, {@code destY}).
	 *
	 * <p>This is the sub-region path: the minimap, chat area, tab area, the
	 * login screen and the fixed-mode frame pieces. These are AWT-composited
	 * overlays rather than the game frame, so they are not the thing a GPU path
	 * takes over, and they take no GPU hook.
	 *
	 * <p>Argument order is ({@code destX}, {@code destY}) - note that
	 * {@code RSImageProducer.drawGraphics} takes them the other way round.
	 */
	public static void blit(RSImageProducer producer, Graphics hostGraphics, int destX, int destY) {
		producer.drawGraphics(destY, hostGraphics, destX);
	}

	/**
	 * Software blit of one <em>rectangle</em> of a buffer to ({@code destX}, {@code destY}).
	 *
	 * <p>Added for fixed mode's HUD layer, which is a whole frame tall and wide but is only allowed
	 * to reach the screen where it carries an overlay: the rest of it is a copy of the frame as the
	 * direct blits built it, and copying that back would be at best redundant and at worst a
	 * disagreement with whatever presented the frame. A region blit is therefore how that layer is
	 * presented, and it is a present like any other, so it belongs on this seam rather than in the
	 * caller.
	 *
	 * <p>Falls back to nothing at all if the host graphics cannot take a sub-image draw, because a
	 * rectangle drawn from the wrong place is worse than a rectangle not drawn.
	 */
	public static void blitRegion(RSImageProducer producer, Graphics hostGraphics, int srcX, int srcY, int w,
			int h, int destX, int destY) {
		if (producer == null || hostGraphics == null || w <= 0 || h <= 0) {
			return;
		}
		Image image = producer.sourceImage();
		if (!(image instanceof BufferedImage)) {
			return;
		}
		if (hostGraphics instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D) hostGraphics;
			// The same interpolation the full-buffer present uses: a region copy at 1:1 must land on
			// the same pixels the direct blits did, and interpolation is what would make a retraced
			// panel visibly softer than the frame around it. The anti-aliasing hints are deliberately
			// left alone - nothing here draws an edge, and they are text hints to the rest of the
			// frame's drawing, which this call runs in the middle of.
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
			g2.drawImage(image, destX, destY, destX + w, destY + h, srcX, srcY, srcX + w, srcY + h, null);
			return;
		}
		hostGraphics.drawImage(image, destX, destY, destX + w, destY + h, srcX, srcY, srcX + w, srcY + h, null);
	}

	/**
	 * The game-frame present - the one a GPU path is expected to take over
	 * wholesale.
	 *
	 * <p>The fallback order is unchanged from before this class existed, with the
	 * Phase 4.2a facade prepended as the first ask:
	 * {@link GpuRenderer#presentGameFrame} (declines when no renderer is installed),
	 * then the pre-existing {@link GlPresent#presentGame} hook, then the software
	 * blit. ⚠️ {@code GlPresent.presentGame} is still a dead stub that returns
	 * {@code false} unconditionally; it is kept here rather than removed so the
	 * order stays identical, and it is already listed for deletion in Phase 8.2 -
	 * deleting it is that step's job, not this one.
	 */
	public static void presentGameFrame(RSImageProducer producer, Graphics hostGraphics, int destX, int destY) {
		if (GpuRenderer.presentGameFrame(producer, destX, destY)) {
			return;
		}
		if (GlPresent.presentGame(producer, destX, destY)) {
			return;
		}
		producer.drawGraphics(destY, hostGraphics, destX);
	}
}
