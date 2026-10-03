package server.clip.region;

/**
 * A direction-addressed view of the tile collision data — "can I walk from this tile in
 * this compass direction?"
 *
 * <p>This is a <b>view, not a second matrix.</b> {@link Region} remains the only place
 * collision bits are stored and loaded; nothing here allocates or caches anything. It
 * delegates to {@link SmartPathFinder#canStep}, which {@link Region} documents as "the sole
 * step API used by player/NPC movement". Delegating to the same primitive the movement loop
 * uses is the whole point: it makes it impossible for this facade to disagree with the code
 * that actually moves entities, which is exactly the failure mode that a second, parallel
 * clipping implementation (see {@code WalkingCheck}/{@code ClipMap.bin}) would introduce.
 *
 * <p><b>Direction convention.</b> Indices are {@code 0..7} in the order used throughout this
 * server — {@code NORTH, NORTH_EAST, EAST, SOUTH_EAST, SOUTH, SOUTH_WEST, WEST, NORTH_WEST} —
 * which is the same order as {@code Misc.directionDeltaX}/{@code directionDeltaY} and the
 * client's own direction table. {@link #deltaX(int)}/{@link #deltaY(int)} expose that mapping
 * so the convention can be pinned against {@code Misc} by a test rather than by inspection.
 *
 * <p><b>Note on the coordinate arguments.</b> {@code (x, y)} is the tile the entity is
 * <em>standing on</em>, not the destination. {@link Region}'s direction-aware overload
 * ({@code getClipping(x, y, z, dx, dy)}) takes the same origin-and-delta shape, but note that
 * its diagonal branches test the walk masks against the <em>source</em> tile whereas
 * {@code SmartPathFinder} tests them against the <em>destination</em> tile. That fork is
 * pre-existing and is why this class routes through {@code SmartPathFinder} only: it is the
 * variant the live movement path already agrees with.
 *
 * <p>Intended for movement and combat-follow code that reasons in directions ("is the tile to
 * my north blocked?") rather than in raw deltas or masks.
 */
public final class Movement {

	/** North, {@code (0, +1)}. */
	public static final int NORTH = 0;
	/** North-east, {@code (+1, +1)}. */
	public static final int NORTH_EAST = 1;
	/** East, {@code (+1, 0)}. */
	public static final int EAST = 2;
	/** South-east, {@code (+1, -1)}. */
	public static final int SOUTH_EAST = 3;
	/** South, {@code (0, -1)}. */
	public static final int SOUTH = 4;
	/** South-west, {@code (-1, -1)}. */
	public static final int SOUTH_WEST = 5;
	/** West, {@code (-1, 0)}. */
	public static final int WEST = 6;
	/** North-west, {@code (-1, +1)}. */
	public static final int NORTH_WEST = 7;

	/** Direction deltas, in the same order as {@code Misc.directionDeltaX}. */
	private static final int[] DELTA_X = { 0, 1, 1, 1, 0, -1, -1, -1 };

	/** Direction deltas, in the same order as {@code Misc.directionDeltaY}. */
	private static final int[] DELTA_Y = { 1, 1, 0, -1, -1, -1, 0, 1 };

	private Movement() {
	}

	/** The x delta for a direction index. Throws on an out-of-range direction. */
	public static int deltaX(int direction) {
		check(direction);
		return DELTA_X[direction];
	}

	/** The y delta for a direction index. Throws on an out-of-range direction. */
	public static int deltaY(int direction) {
		check(direction);
		return DELTA_Y[direction];
	}

	/**
	 * True when the step from {@code (x, y)} one tile in {@code direction} is refused by
	 * clipping. Out-of-range directions throw rather than defaulting, because a caller that
	 * passes {@code -1} (a common "no direction" sentinel) has a bug that would otherwise
	 * read as "never blocked".
	 */
	public static boolean isBlocked(int x, int y, int z, int direction) {
		check(direction);
		return !SmartPathFinder.canStep(x, y, DELTA_X[direction], DELTA_Y[direction], z);
	}

	/** The inverse of {@link #isBlocked}: true when the step is allowed. */
	public static boolean canWalk(int x, int y, int z, int direction) {
		return !isBlocked(x, y, z, direction);
	}

	private static void check(int direction) {
		if (direction < NORTH || direction > NORTH_WEST) {
			throw new IllegalArgumentException(
					"direction must be 0..7 (N,NE,E,SE,S,SW,W,NW), got " + direction);
		}
	}
}
