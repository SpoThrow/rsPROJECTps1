package server.game.bots.world;

/**
 * A waypoint that is one exact tile — the slice-1 behaviour, kept as a {@link Waypoint} so a caller
 * does not have to know which kind it was handed.
 *
 * <p>{@code resolve} ignores the seed: a fixed point is fixed for every bot, which is what "stand
 * <em>here</em>" means when an author really does want one tile (a doorway, a bank booth).
 */
public final class FixedPoint implements Waypoint {

	private final Tile tile;

	public FixedPoint(Tile tile) {
		if (tile == null) {
			throw new IllegalArgumentException("a fixed point needs a tile");
		}
		this.tile = tile;
	}

	public FixedPoint(int x, int y, int plane) {
		this(Tile.of(x, y, plane));
	}

	@Override
	public Tile resolve(int seed) {
		return tile;
	}

	@Override
	public String toString() {
		return "FixedPoint(" + tile + ")";
	}
}
