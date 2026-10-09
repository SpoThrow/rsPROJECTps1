package server.game.bots.world;

/**
 * A destination that is resolved, not fixed — {@code BOT_ROADMAP.md} §5.2.
 *
 * <p>The point of the indirection is that an author drags a <em>box</em>, not a tile. Passing a tile
 * around would mean every bot funneling onto the exact same square, which is both visibly wrong and
 * occasionally fatal (a bot walking onto another bot's tile). A waypoint is the box; the tile is what
 * comes out.
 *
 * <p>{@code seed} is the per-bot seed. Two bots sharing one waypoint get different tiles, while each
 * bot individually gets the <em>same</em> tile every time it resolves that waypoint — which is what
 * makes a stuck bot replayable from its trace.
 */
public interface Waypoint {

	/** A concrete walkable destination. */
	Tile resolve(int seed);
}
