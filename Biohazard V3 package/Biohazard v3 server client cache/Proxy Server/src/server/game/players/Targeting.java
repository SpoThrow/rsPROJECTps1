package server.game.players;

/**
 * Who this player is fighting, reached as {@code c.targeting}.
 *
 * <p>Extracted in §4.16 as the targeting half of the combat tail. <b>It holds both directions of
 * a fight</b> — the target I have selected, and the attacker who has selected me — because the
 * multi-combat checks read them together
 * ({@code c.targeting.underAttackBy > 0 || c.targeting.underAttackBy2 > 0}).
 *
 * <p>⚠️ <b>{@link #underAttackBy} is declared on {@code NPC} as well, with a different meaning on
 * each side.</b> On a player it is the <em>index of whoever is attacking me</em>; on an NPC it is
 * the same idea from the NPC's side. The two live in the same statements — e.g. {@code NPCHandler}
 * reads {@code npcs[i].underAttackBy} and {@code c.targeting.underAttackBy} a few lines apart — so
 * this is a genuinely type-dependent field, not just a shadowed name. The rewriter resolved it by
 * type: 11 {@code NPC} references were deliberately left alone. Every other field here is declared
 * only on {@link Player}.
 *
 * <p><b>None of these are persisted</b> (they are re-established from the current fight), and all
 * default to {@code 0}, meaning "no target" / "not under attack" — which is also why the index
 * convention is 1-based, with {@code 0} reserved as the sentinel.
 *
 * <p>This is a data bag on purpose. Extracting the cluster and encapsulating it are separate
 * steps, so that a behaviour change cannot hide inside the mechanical move.
 */
public final class Targeting {

	/** Index of the player currently being attacked; {@code 0} = none. */
	public int playerIndex;

	/** Index of the NPC currently being attacked; {@code 0} = none. */
	public int npcIndex;

	/** The previous player target, kept so its attack state can be cleared when the target changes. */
	public int oldPlayerIndex;

	/** The previous NPC target, kept so its attack state can be cleared when the target changes. */
	public int oldNpcIndex;

	/** Index of the NPC whose death is credited to this player (for drops). */
	public int killingNpcIndex;

	/** ⚠️ Index of whoever is attacking me; {@code 0} = not under attack. Also declared on {@code NPC} with the mirrored meaning. */
	public int underAttackBy;

	/** The second "under attack by" slot; {@code 0} = none. Read together with {@link #underAttackBy} in the multi-combat checks. */
	public int underAttackBy2;

	/** Soul Split's current player target; {@code 0} = none. */
	public int ssTarget;

	/** Soul Split's current NPC target; {@code 0} = none. */
	public int ssTargetNpc;
}
