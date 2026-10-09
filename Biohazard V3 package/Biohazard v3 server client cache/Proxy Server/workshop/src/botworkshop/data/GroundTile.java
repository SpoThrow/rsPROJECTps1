package botworkshop.data;

/**
 * One landscape tile's floor ids and flags, for a single plane.
 *
 * <p>Nothing here comes from the server: {@code Region.loadMaps} reads exactly these bytes and
 * keeps only the bridge bits of {@link #flags}, so the overlay and underlay ids exist nowhere in
 * the running server. They are decoded here for the editor's map view.
 */
public final class GroundTile {

	/** {@code -1} when the tile's height is derived rather than stored (opcode 0). */
	public static final int HEIGHT_DERIVED = -1;

	/** Overlay (the drawn floor) floor id, {@code 0} for none. Raw map value, unsigned. */
	public final int overlayId;

	/** Underlay (the ground beneath the overlay) floor id, {@code 0} for none. */
	public final int underlayId;

	/**
	 * Tile flags, the map's opcode-50..81 value. Bit 0 is what {@code Region.loadMaps} tests for
	 * occupancy and bit 1 marks a "bridge" tile whose real plane is one below this one.
	 */
	public final int flags;

	/** Explicit tile height, or {@link #HEIGHT_DERIVED}. */
	public final int height;

	GroundTile(int overlayId, int underlayId, int flags, int height) {
		this.overlayId = overlayId;
		this.underlayId = underlayId;
		this.flags = flags;
		this.height = height;
	}

	/** True when the tile carries an overlay to draw. */
	public boolean hasOverlay() {
		return overlayId != 0;
	}

	/** Mirrors {@code Region.loadMaps}: the occupancy bit tested to place a blocker. */
	public boolean isOccupied() {
		return (flags & 1) == 1;
	}

	@Override
	public String toString() {
		return "overlay=" + overlayId + " underlay=" + underlayId + " flags=" + flags
				+ " height=" + (height == HEIGHT_DERIVED ? "derived" : String.valueOf(height));
	}
}
