package server.game.players;

import java.util.ArrayList;

import server.Config;

/**
 * Who gets credit for a kill, reached as {@code c.killCredit}.
 *
 * <p>Extracted in §4.19 from the loose combat scalars. The server decides drops by measuring damage
 * rather than by who landed the last hit, and it does so with <b>two independent mechanisms</b> that
 * both live here: {@link #totalDamageDealt}, a single running total that is compared across
 * attackers and then zeroed, and {@link #damageTaken}, a per-attacker table indexed by attacker id
 * (the counterpart booked by the attacker's own combat code). {@link #killerId} names the credited
 * killer for the PK case, and {@link #attackedPlayers} records who this player has engaged.
 *
 * <p>⚠️ <b>{@link #killerId} is declared on {@code NPC} as well</b> ({@code NPC.java:32}), with the
 * mirrored meaning — on a player it is <em>who killed me</em> (driving the item drop to them), on an
 * NPC it is <em>who killed the NPC</em>. That is the §4.16 {@code underAttackBy} shape again, so the
 * rewriter left 21 {@code NPC} references and 4 locals alone.
 *
 * <p>{@link #attackedPlayers} is the loosest fit in the bag — a list among scalars — but it shares
 * the exact lifecycle of the rest: it is cleared wherever the encounter is reset, and it is read
 * next to {@code targeting.playerIndex} in the mutual-attack check. It is kept here rather than
 * orphaned, and is the obvious candidate to peel off if this bag is ever split.
 *
 * <p>The defaults are all "no encounter": {@code 0} for the ids and totals, an all-zero table, and
 * an empty list. The two composite fields are initialised here rather than in a constructor so the
 * bag needs no constructor, matching the other collaborators.
 */
public final class KillCredit {

	/** Index of the player credited with killing this player, used when dropping their items; {@code 0} = none. ⚠️ Also declared on {@code NPC}. */
	public int killerId;

	/** Running damage this player has dealt to their current target; the drop logic compares it across attackers and then resets it to {@code 0}. */
	public int totalDamageDealt;

	/** Per-attacker damage table indexed by attacker id — the second drop-credit mechanism. Reallocated wholesale when the encounter is reset. */
	public int[] damageTaken = new int[Config.MAX_PLAYERS];

	/** Players this one has engaged, for the mutual-attack check in {@code attackPlayer}; cleared with the rest of the encounter. */
	public ArrayList<Integer> attackedPlayers = new ArrayList<Integer>();
}
