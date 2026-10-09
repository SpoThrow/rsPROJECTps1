package botworkshop.export;

/**
 * Supplies the server's own collision value for a world tile.
 *
 * <p>This is an interface so the exporter can take clipping from {@code Region.getClipping} — the
 * single collision source the server itself uses — without the document builder having to depend on
 * {@code Region.load()} having run. That keeps the builder unit-testable with a fixed table, while
 * the real export cannot drift from the server because it is literally asking it.
 */
public interface TileClip {

	/** The clip value for a world tile, in the same encoding {@code Region.getClipping} returns. */
	int clip(int x, int y, int plane);
}
