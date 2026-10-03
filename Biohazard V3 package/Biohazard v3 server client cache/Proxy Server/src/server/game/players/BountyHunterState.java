package server.game.players;

/**
 * Per-player Bounty Hunter state: who this player is hunting, whether they are in the
 * crater, and their kill tallies and timers.
 *
 * <p>Eleven fields that used to be scattered across {@link Player} -- the original nine
 * plus {@code safeTimer} (declared among the level-requirement flags) and the dead
 * {@code bountyIcon} (declared next to {@code headIcon}). Reached as
 * {@code player.bountyHunter}.
 *
 * <p>Four of the eleven are persisted in {@code PlayerSave} -- {@code rogueKills},
 * {@code bountyKills}, {@code killsMultiplier} and {@code penaltyTimer} (and
 * {@code safeTimer}, five in all) -- and every one of those save keys is the identical
 * field name, so as in 4.4 the leaf names are left alone and only the path moves.
 *
 * <p>This is a data bag on purpose. Extracting the cluster and encapsulating it are
 * separate steps, so that a behaviour change cannot hide inside the mechanical move.
 */
public final class BountyHunterState {

	/** Player index of the assigned target, or {@code 0} for none. Persisted? No. */
	public int targetIndex;

	/**
	 * Name of the assigned target, or {@code null} for none.
	 *
	 * <p>⚠️ Defaults to {@code null} and {@code BountyHunter.resetTarget} deliberately sets
	 * it back to {@code null}, but {@code BountyHunter.handleBHDeath} calls
	 * {@code targetName.equalsIgnoreCase(...)} on it with no null guard, so that path NPEs
	 * whenever the target has no name assigned. A pre-existing bug, pinned not fixed by
	 * this move -- see the plan.
	 */
	public String targetName;

	/** Is the player inside the Bounty Hunter crater. */
	public boolean inBH;

	/** Is the player the rogue in their pairing (no penalty on death). */
	public boolean isRogue;

	/** Is the player currently flagged as someone else's bounty. */
	public boolean isBounty;

	/**
	 * A boolean despite the "Timer" in its name: whether the player is serving a rogue
	 * death penalty rather than counting down. Persisted as {@code penaltyTimer}.
	 */
	public boolean penaltyTimer;

	/**
	 * Crater re-entry cooldown, in seconds. Persisted as {@code safeTimer}.
	 *
	 * <p>Despite the generic name this is Bounty Hunter state only -- every one of its
	 * sites is in {@code BountyHunter} or guarded by {@code c.isRogue && c.inBhArea()} in
	 * {@code ItemHandler}. If it ever becomes a general PvP safe timer it should move out
	 * of this class, but nothing else uses it today.
	 */
	public int safeTimer;

	/** Kills scored while in Bounty Hunter. Persisted as {@code bountyKills}. */
	public int bountyKills;

	/** Kills scored as a rogue. Persisted as {@code rogueKills}. */
	public int rogueKills;

	/**
	 * Multiplier applied to Bounty Hunter kill rewards. Persisted as
	 * {@code killsMultiplier}. ⚠️ Defaults to {@code 1}, not {@code 0}: it multiplies the
	 * reward, so a {@code 0} default would silently zero every payout.
	 */
	public int killsMultiplier = 1;

	/**
	 * ⚠️ Dead: never read and never written anywhere live. Its only remaining reference is
	 * a commented-out {@code playerProps.writeByte(bountyIcon)} in {@link Player}. Moved
	 * in with the cluster so the Bounty Hunter concept lives in one place; deletable.
	 */
	public int bountyIcon;
}
