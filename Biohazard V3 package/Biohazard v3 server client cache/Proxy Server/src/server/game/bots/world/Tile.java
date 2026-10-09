package server.game.bots.world;

/**
 * An immutable tile: a resolved destination, not a player's position.
 *
 * <p>Deliberately not {@code server.game.players.Position}. That class is mutable and carries the
 * player's map-region counters, walk cursor and queued teleport; using it to hold "where this waypoint
 * resolved to" would invite a state method to edit the bot's live position by accident. A destination
 * is a value, so it is modelled as one.
 */
public final class Tile {

	private final int x;
	private final int y;
	private final int plane;

	private Tile(int x, int y, int plane) {
		this.x = x;
		this.y = y;
		this.plane = plane;
	}

	public static Tile of(int x, int y, int plane) {
		return new Tile(x, y, plane);
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

	@Override
	public String toString() {
		return x + "," + y + " p" + plane;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Tile)) {
			return false;
		}
		Tile that = (Tile) other;
		return x == that.x && y == that.y && plane == that.plane;
	}

	@Override
	public int hashCode() {
		return (x * 31 + y) * 31 + plane;
	}
}
