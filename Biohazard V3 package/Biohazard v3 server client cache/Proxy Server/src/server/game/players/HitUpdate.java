package server.game.players;

/**
 * The hit bookkeeping for this player, reached as {@code c.hitUpdate}.
 *
 * <p>Extracted in §4.17. It is the whole of what happens between "a hit is decided" and "the
 * client is told about it": the two hitsplat slots the update mask advertises, and the damage that
 * has been <em>reserved</em> against this player but not yet applied to their hitpoints.
 *
 * <p>⚠️ <b>The four mask names are also declared on {@code NPC}</b> ({@code NPC.java} lines
 * 29/54/297/298) because an NPC advertises its splats through the same protocol, and NPC's own
 * update block reads them bare. So this is genuinely the same shape as §4.16's {@code underAttackBy}
 * — the rewriter had to pick a side by receiver type, and deliberately left 41 NPC references
 * alone. {@link #pendingHitpoints} has no NPC twin, because the NPC side reserves damage under the
 * different name {@code pendingDamage}.
 *
 * <p><b>Only {@link Player} touches these fields directly.</b> Every other class goes through the
 * accessors that stay on {@code Player} — {@code setHitDiff}, {@code setHitDiff2},
 * {@code setHitUpdateRequired}, {@code setHitUpdateRequired2}, {@code isHitUpdateRequired},
 * {@code getHitUpdateRequired2} — so the move is internal and those methods' signatures did not
 * change. That is also why the cluster is small (32 edits) despite the fields being load-bearing.
 *
 * <p>All defaults are the "nothing to report" value: {@code false} for the flags and {@code 0} for
 * the values and the reserved counter. A non-zero {@code hitDiff} default would paint a splat on
 * every spawn, and a {@code true} flag would make the update mask advertise a block that was never
 * written — a client desync, not a cosmetic glitch.
 */
public final class HitUpdate {

	/**
	 * Damage for the <b>first</b> hitsplat, to be written when {@link #hitUpdateRequired} is set.
	 * A byte on the wire, so it is truncated to 0–255 by {@code Stream.writeByte}.
	 */
	public int hitDiff;

	/**
	 * Damage for the <b>second</b> hitsplat, written by {@code appendHitUpdate2} when
	 * {@link #hitUpdateRequired2} is set. This slot carries double hits and the poison splat —
	 * {@code appendHitUpdate2} overrides its colour byte from {@code Player.poisonMask}.
	 */
	public int hitDiff2;

	/** Set when {@link #hitDiff} must be included in this tick's update mask (bit {@code 0x20}). */
	public boolean hitUpdateRequired;

	/** Set when {@link #hitDiff2} must be included in this tick's update mask (bit {@code 0x200}). Cleared every tick by {@code clearUpdateFlags}. */
	public boolean hitUpdateRequired2;

	/**
	 * Damage reserved against this player but <em>not yet applied</em> to hitpoints.
	 *
	 * <p>{@code reservePlayerHit} adds to it when a hit is queued, {@code dealDamage} deducts from it
	 * as damage lands, and {@code remainingPlayerHp()} subtracts it from the live hitpoints so a
	 * queued hit cannot be capped at more than the player will actually have. It is the player-side
	 * counterpart of the NPC's {@code pendingDamage}.
	 */
	public int pendingHitpoints;
}
