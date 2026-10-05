package ui;

/**
 * The client's 2D drawing surface.
 *
 * <p><b>Phase 4.1c:</b> the substrate this class draws into - the pixel buffer,
 * its dimensions, the clip rectangle, and every primitive that writes to them -
 * now lives in {@link Framebuffer}, the rasteriser seam. This type remains as a
 * subclass so that nothing else had to change.
 *
 * <p>Static members are inherited in Java, so the ~570 existing references keep
 * resolving exactly as before, in both forms used in this codebase:
 * <ul>
 *   <li>qualified - {@code DrawingArea.width}, {@code DrawingArea.pixels}; and</li>
 *   <li>unqualified and inherited - inside {@link Sprite}, {@link Texture},
 *       {@link TextDrawingArea} and {@link Background}, which all extend this
 *       class.</li>
 * </ul>
 * The extraction was therefore a move, not an edit: no call site was rewritten.
 *
 * <p>Behaviour is unchanged by construction (the code moved verbatim) and is
 * pinned by the framebuffer hash asserted in the harness as part of 4.1b.
 */
public class DrawingArea extends Framebuffer {

	public DrawingArea() {}
}
