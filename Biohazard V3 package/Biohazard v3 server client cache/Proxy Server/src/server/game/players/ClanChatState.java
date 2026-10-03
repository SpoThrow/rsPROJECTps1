package server.game.players;

/**
 * Per-player clan-chat state.
 *
 * <p>Three fields that used to sit directly on {@link Player}. They are grouped here
 * because they describe one thing -- which clan chat this player is in, and a pending
 * kick confirmation -- and they are read from a handful of places across the tree
 * ({@code Clan}, {@code Client}, {@code PlayerSave}, {@code InterfaceAction},
 * {@code ReceiveString}). Reached as {@code player.clanChat}.
 *
 * <p>This is a deliberate data bag -- the fields stay public, exactly as they were on
 * {@link Player}. Extracting the cluster is one step; encapsulating it is another, so
 * that a behaviour change cannot hide inside a mechanical move.
 */
public final class ClanChatState {

	/**
	 * The clan chat this player is in, identified by the clan founder's name
	 * ({@code Clan.getFounder()}); empty when not in one. Set on joining, cleared when
	 * leaving, and read on login to rejoin the channel the player was last in.
	 *
	 * <p>Was {@code Player.lastClanChat}. Note that the <em>save-file key</em> is the
	 * literal {@code lastclanchat} (see {@code PlayerSave}) and is deliberately unchanged
	 * by Phase 4.3 -- renaming the key would invalidate every existing character file, so
	 * only the Java name moved.
	 */
	public String channel = "";

	/** Member name awaiting a second click on Kick, or empty. Not persisted. */
	public String pendingKick = "";

	/** When {@link #pendingKick} was set, for the 8-second confirm window. */
	public long pendingKickAt;
}
