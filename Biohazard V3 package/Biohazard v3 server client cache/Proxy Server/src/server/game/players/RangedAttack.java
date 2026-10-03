package server.game.players;

/**
 * The ranged attack this player has launched, reached as {@code c.rangedAttack}.
 *
 * <p>Extracted in §4.18 from the loose combat scalars. It is the state needed to <em>finish</em> an
 * attack that has already left the player: which phase the shot is in, the ammo it will drop, the
 * weapon that launched it, the crystal bow's degrade counter and the bow-special flag. The attack
 * itself is resolved when {@link #projectileStage} runs down — this is the bookkeeping that lets
 * the later tick know what "it" was.
 *
 * <p>Unlike §4.16's and §4.17's bags, <b>none of these names exist on {@code NPC}</b>, so this pass
 * had no collisions at all (115 edits, nothing skipped) even though it is the largest of the three.
 *
 * <p>Every field defaults to {@code 0}, meaning "no shot in flight, no weapon remembered". A
 * non-zero default would make a fresh player look mid-attack.
 */
public final class RangedAttack {

	/** Phase of the in-flight shot: {@code 0} = none, {@code 1}/{@code 2} = in flight. Gates whether the attack is treated as a missile and when its damage lands. */
	public int projectileStage;

	/** Item id of the ammo to drop where the shot lands; {@code ItemAssistant} creates or merges a ground item with it. */
	public int rangeItemUsed;

	/** The equipped weapon id as of launch, re-read to detect the dark bow ({@code 11235}) and its double-arrow special. */
	public int lastWeaponUsed;

	/**
	 * Crystal bow degrade counter, incremented per shot and reset at {@code 250} to downgrade the bow.
	 *
	 * <p>⚠️ The only persisted field in the bag: it round-trips through {@code PlayerSave} under the
	 * save key <b>{@code crystal-bow-shots}</b>. The key is a literal string and did not move, and
	 * the load and save paths were rewritten together, so the on-disk format is unchanged.
	 */
	public int crystalBowArrowCount;

	/** Bow-special shot flag/counter, tested alongside {@link #lastWeaponUsed} to pick the special arrow path. */
	public int bowSpecShot;
}
