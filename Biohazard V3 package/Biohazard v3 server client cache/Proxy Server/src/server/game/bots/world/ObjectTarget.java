package server.game.bots.world;

/**
 * A concrete world object a bot can click: its id, its tile and its plane.
 *
 * <p><b>Why this exists when {@link Location} already does.</b> A {@code Location} is a <em>place</em>
 * — a named box an author drew, or a generated point — and it deliberately does not name an object,
 * because "where in the world is a bank" is a different question from "which booth is this". An
 * interaction needs the object: {@code ObjectHandler.dispatch} takes an object id, and a gather leaf
 * that only had a place would have nowhere to click. So a target is the identity of one object, and a
 * place is the region it lives in. The two are produced from the same scan ({@link ResourceScan}), so
 * they cannot disagree about what is where.
 *
 * <p>Plane is part of identity for the same reason it is on {@code Location}: the object at
 * {@code 3207,3221} on plane 2 is not the object at {@code 3207,3221} on plane 0.
 */
public final class ObjectTarget {

	private final int objectId;
	private final int x;
	private final int y;
	private final int plane;

	public static ObjectTarget of(int objectId, int x, int y, int plane) {
		return new ObjectTarget(objectId, x, y, plane);
	}

	private ObjectTarget(int objectId, int x, int y, int plane) {
		this.objectId = objectId;
		this.x = x;
		this.y = y;
		this.plane = plane;
	}

	public int objectId() {
		return objectId;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public int plane() {
		return plane;
	}

	public Tile tile() {
		return Tile.of(x, y, plane);
	}

	/**
	 * Squared distance to a tile. Squared because the only use is ordering, and the square root would
	 * be taken and thrown away for every candidate in a scan.
	 */
	public int distanceSquaredTo(int tileX, int tileY) {
		int dx = tileX - x;
		int dy = tileY - y;
		return dx * dx + dy * dy;
	}

	@Override
	public String toString() {
		return "object " + objectId + " at " + x + "," + y + " p" + plane;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ObjectTarget)) {
			return false;
		}
		ObjectTarget that = (ObjectTarget) other;
		return objectId == that.objectId && x == that.x && y == that.y && plane == that.plane;
	}

	@Override
	public int hashCode() {
		return ((objectId * 31 + x) * 31 + y) * 31 + plane;
	}
}
