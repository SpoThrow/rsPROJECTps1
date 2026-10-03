package server.game.players;

/**
 * Per-player state for the Player Owned Shop (POS) interface.
 *
 * <p>These 17 fields used to sit directly on {@link Player}, interleaved with unrelated
 * state. They are grouped here because they are only ever read and written together, by
 * three files ({@code PlayerAssistant}, {@code BankX2}, {@code ClickItem}), and they all
 * die with the shop session.
 *
 * <p>Reached as {@code player.pos}. Note that {@code pos} here means <em>Player Owned
 * Shop</em>, not <em>position</em> -- when the movement cluster is extracted it must not
 * try to take this name.
 *
 * <p>This is a deliberate data bag: the fields are public, exactly as they were on
 * {@link Player}. Extracting the cluster is the first step; encapsulating it is a
 * separate one, so that a behaviour change cannot hide inside a large mechanical move.
 */
public final class PosSession {

	/** Slots on the buy screen. Matches the arrays that used to be sized inline. */
	public static final int LISTING_SLOTS = 20;

	/** A sell is in progress; {@link #sellStep} says which prompt is up. */
	public boolean selling;
	public int sellItemId;
	public int sellAmount;
	public int sellPrice;
	public int sellStep;

	public String[] buySellers = new String[LISTING_SLOTS];
	public int[] buyIndexes = new int[LISTING_SLOTS];
	public long[] buyListingIds = new long[LISTING_SLOTS];
	public boolean buying;
	public long buyListingId;
	public int buyMax;

	public int sortMode;
	public int browseType;
	public String browseQuery = "";
	public String browseTitle = "Recent Listings";

	public long confirmRemoveId;
	public long editListingId;
}
