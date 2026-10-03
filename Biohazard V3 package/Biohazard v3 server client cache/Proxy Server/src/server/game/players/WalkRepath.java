package server.game.players;

/**
 * The last walk destination a {@link Player} was sent to, and the flag that stops the
 * recovery for it from re-entering.
 *
 * <p>Reached as {@code player.walkRepath}. This is the bookkeeping behind one silent
 * repath: when a walk step is refused by clipping, {@link Player}'s walk loop resets the
 * queue and, if there is a destination recorded here and the player is not already on it,
 * asks the path finder for a fresh route once. {@code walkRepathPending} is set around
 * that call so a failure inside the path finder cannot recurse back into it.
 *
 * <p>Split out from {@link Position} rather than folded into it: the three fields describe
 * a retry of a walk request, not a coordinate, and a boolean guarding re-entry has no
 * business in a class called "position".
 *
 * <p>All three were declared on {@link Player} and none of them collides with another
 * type, so unlike the coordinate trio this group would have been safe to move by name.
 */
public final class WalkRepath {

	/**
	 * Last click-walk or scripted destination X, or {@code -1} for none. Written by the
	 * path finder unless a repath is already pending.
	 */
	public int lastWalkDestX = -1;

	/** Last click-walk or scripted destination Y, or {@code -1} for none. */
	public int lastWalkDestY = -1;

	/** Set only around the recovery path-finder call, so it cannot re-enter itself. */
	public boolean walkRepathPending;
}
