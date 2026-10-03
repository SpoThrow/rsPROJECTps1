package server.game.players;

/**
 * Per-player woodcutting session state.
 *
 * <p>Fields that used to sit on {@link Player} among the unrelated declared flags around
 * them. Reached as {@code player.woodcutting}.
 *
 * <p>Modelled on the earlier sessions: this is a deliberate data bag so that a behaviour
 * change cannot hide inside the move. Note what is deliberately <em>not</em> here:
 * {@code Woodcutting.a}, the selected axe index, was {@code static} and therefore shared
 * by every player, so moving it into a per-player object would have been a behaviour
 * change. It is a {@code final} local of {@code startWoodcutting} instead.
 */
public final class WoodcuttingSession {

	/**
	 * The session is live. Set by {@code Woodcutting.startWoodcutting}, checked first by
	 * both cycle events and by the movement handler, and cleared by the event's
	 * {@code stop()} callback. This is the only flag: it is the one the movement handler
	 * clears to cancel chopping when the player walks away, so it is what decides whether
	 * a new click starts a session.
	 */
	public boolean active;

	/** X of the tree being cut. Non-zero only while {@link #active}. */
	public int treeX;

	/** Y of the tree being cut. Non-zero only while {@link #active}. */
	public int treeY;
}
