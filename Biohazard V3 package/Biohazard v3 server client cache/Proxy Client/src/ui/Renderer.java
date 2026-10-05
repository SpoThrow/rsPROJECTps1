package ui;

import java.awt.Graphics;

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
	 * The game-frame present - the one a GPU path is expected to take over
	 * wholesale.
	 *
	 * <p>Keeps the pre-existing hook and fallback exactly as {@code
	 * client.drawGameBuffer} performed them before this class existed: ask
	 * {@link GlPresent#presentGame} first, and only blit in software if it
	 * declines.
	 */
	public static void presentGameFrame(RSImageProducer producer, Graphics hostGraphics, int destX, int destY) {
		if (GlPresent.presentGame(producer, destX, destY)) {
			return;
		}
		producer.drawGraphics(destY, hostGraphics, destX);
	}
}
