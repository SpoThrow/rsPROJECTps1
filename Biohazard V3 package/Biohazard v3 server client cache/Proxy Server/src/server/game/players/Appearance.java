package server.game.players;

/**
 * How a player looks, and the flags that decide when that look is re-sent to the client.
 *
 * <p>Six fields that used to be declared straight on {@link Player}, reached as
 * {@code player.appearance}: the appearance slots {@code playerAppearance}, the
 * {@code appearanceUpdateRequired} dirty flag, and the three overhead icons —
 * {@code headIcon} (prayer/curse), {@code headIconPk} (PK skull) and {@code headIconHints}
 * (duel) — plus {@code canChangeAppearance}.
 *
 * <p><b>Two defaults are load-bearing.</b> {@code appearanceUpdateRequired} starts
 * {@code true}, so the first appearance block a new player sends goes out rather than
 * being skipped; and {@code headIcon}/{@code headIconPk} start at {@code -1}, which is the
 * "no icon" value every setter restores them to. A {@code 0} head icon is a real icon
 * (the PK skull is painted with {@code headIconPk = 0}), so getting either default wrong
 * shows a spurious overhead icon or hides a real one.
 *
 * <p><b>{@code playerProps} deliberately did not move.</b> It is
 * {@code protected static Stream} on {@link Player} — a single shared scratch buffer used
 * to serialise the appearance block for whichever player is being written, not per-player
 * state. It belongs with the serialiser, not here, and it is noted in the plan alongside
 * {@code Music.unlocked} as a pre-existing shared-static hazard.
 *
 * <p><b>Persistence is a table.</b> {@code playerAppearance} is written by
 * {@code PlayerSave} as an {@code [LOOK]} section of {@code character-look = <index>
 * <value>} lines and read back the same way; the section and key names are string literals,
 * so the path change cannot move them. The other five are session state and are not saved.
 *
 * <p>Kept out of this cluster on purpose: {@code playerName} (identity, not appearance) and
 * the chat-text family ({@code chatText}, {@code chatTextColor}, {@code chatTextEffects},
 * {@code chatTextSize}, {@code chatTextUpdateRequired}, {@code forcedChatUpdateRequired}),
 * which is what a player is <em>saying</em> rather than how they look.
 *
 * <p>This is a data bag on purpose. Extracting the cluster and encapsulating it are
 * separate steps, so that a behaviour change cannot hide inside the mechanical move.
 */
public final class Appearance {

	/**
	 * The twelve appearance slots (gender, head, torso, arms, hands, legs, feet, hair,
	 * beard, colour, and the two extra colour slots). Declared {@code 13} long on
	 * {@link Player} and still is — the save loop writes all thirteen, so the length is
	 * part of the on-disk format.
	 */
	public int[] playerAppearance = new int[13];

	/**
	 * Whether the player's appearance block needs re-sending on the next update. Starts
	 * {@code true} so a new player is described to the client immediately; if this were
	 * {@code false} a fresh login would render as an unset appearance until something else
	 * forced an update.
	 */
	public boolean appearanceUpdateRequired = true;

	/** Whether the player is currently allowed to open the appearance editor. */
	public boolean canChangeAppearance = false;

	/**
	 * Overhead prayer or curse icon, or {@code -1} for none. Set from
	 * {@code PRAYER_HEAD_ICONS}/{@code CURSE_HEAD_ICONS} and written into the appearance
	 * block; every reset path restores it to {@code -1}.
	 */
	public int headIcon = -1;

	/**
	 * Overhead PK skull icon, or {@code -1} for none. Note {@code 0} is a valid skull, so
	 * this is not a boolean in disguise.
	 */
	public int headIconPk = -1;

	/** Overhead duel icon state: set to {@code 2} on a duel result, back to {@code 0} on reset. */
	public int headIconHints;
}
