package server.game.players;

/**
 * The player's per-action availability clocks: "may I do X again yet?".
 *
 * <p>Eighteen fields that used to be declared straight on {@link Player}, reached as
 * {@code c.timers}. This is deliberately a <em>sub-slice</em> of the timer state: the
 * duration and scheduling timers (`hitDelay`, `attackTimer`, `freezeTimer`, `respawnTimer`,
 * `delayedDamage`/`delayedDamage2`) were left for the combat pass, and `skullTimer` stayed
 * because it is persisted (it has a save key in {@code PlayerSave}).
 *
 * <p><b>Three shapes, and the difference matters.</b> <b>Fifteen</b> are <em>"last used"
 * epoch-millisecond stamps</em>, always read against the clock as
 * {@code System.currentTimeMillis() - X > threshold} (available again) or
 * {@code < threshold} (still cooling down) — for example
 * {@code System.currentTimeMillis() - c.timers.foodDelay > 2000} in {@code PlayerAssistant}.
 * <b>Two</b> are small <em>tick countdowns</em> never compared to the clock: {@link #clawDelay}
 * (set to {@code 2} when claws activate, decremented once per tick in {@code Client}) and
 * {@link #ssDelay} (set to {@code 4} on a Soul Split hit, decremented in
 * {@code Curse.handleProcess()}), both with {@code > 0} meaning "active". {@link #reduceSpellDelay}
 * is different again in kind: a six-slot table of stamps, one per reduce-spell slot, indexed by
 * {@code reduceSpellId} and compared against {@code REDUCE_SPELL_TIME}. (Filing {@code ssDelay}
 * with the stamps — one countdown instead of two — is a mistake made in the first draft of this
 * note; nothing in its name suggests it is Soul Split.)
 *
 * <p><b>Every default is {@code 0}, and that is load-bearing.</b> For the stamps, {@code 0}
 * means "never used", so {@code now() - 0} is far larger than any threshold and the action
 * is available immediately — the correct state for a new player. The same value is the
 * <em>reset sentinel</em> after a fight ({@code c.timers.teleBlockDelay = 0;} clears a
 * teleblock) and the only value that means "no claws active". A non-zero default would
 * silently lock every new player out of eating, alching, teleblocking and so on.
 *
 * <p>⚠️ <b>{@code teleBlockDelay} is split from its sibling {@code teleBlockLength}, and
 * that is a known wart.</b> The timestamp moved here but {@code teleBlockLength}, the
 * duration it is compared against ({@code now() - teleBlockDelay < teleBlockLength}), stayed
 * on {@link Player} as a duration. They should be reunited when the combat pass takes the
 * durations. Note also that {@code teleBlockDelay} appears in the save path without having a
 * key of its own: {@code PlayerSave} matches {@code token.equals("teleblock-length")} and
 * then sets {@code teleBlockDelay = System.currentTimeMillis()} — the on-disk key belongs to
 * {@code teleBlockLength}, and the delay is derived at load, so no save key moved.
 *
 * <p>Deliberately left on {@link Player} with this pass: {@code specDelay} and
 * {@code restoreStatsDelay} (both sit in the same declaration list but were outside the
 * agreed sub-slice — {@code specDelay}'s initialiser is {@code System.currentTimeMillis()},
 * not {@code 0}, so it is not the same shape), {@code poisonDelay}, {@code teleTimer},
 * {@code saveTimer}, {@code teleBlockLength} and the persisted {@code skullTimer}.
 *
 * <p>This is a data bag on purpose. Extracting the cluster and encapsulating it are separate
 * steps, so that a behaviour change cannot hide inside the mechanical move.
 */
public final class Timers {

	/**
	 * Earliest epoch-millis at which food may be eaten again ({@code now() - foodDelay > 2000}).
	 * Not food-specific: {@code Agility} reads the same field as a general 2s obstacle cooldown
	 * ({@code now() - foodDelay < 2000} = "still cooling down") and sets it, and {@code Potions}
	 * copies {@link #potDelay} into it — eating, drinking and agility share one clock.
	 */
	public long foodDelay;

	/** Earliest epoch-millis at which a potion may be drunk again. Copied into {@link #foodDelay} by {@code Potions}. */
	public long potDelay;

	/** Earliest epoch-millis at which high-alchemy may be cast again. */
	public long alchDelay;

	/**
	 * Epoch-millis at which the current teleblock began, or {@code 0} for "not teleblocked".
	 * Read as {@code now() - teleBlockDelay < teleBlockLength}, so {@code 0} must stay the
	 * reset value. See the class note about its split from {@code teleBlockLength}.
	 */
	public long teleBlockDelay;

	/** Earliest epoch-millis at which a god spell may be cast again. */
	public long godSpellDelay;

	/**
	 * Per-slot epoch-millis stamps for the reduce-spell cooldowns, indexed by
	 * {@code reduceSpellId} and compared against {@code REDUCE_SPELL_TIME}. Zeroed slots mean
	 * "never reduced", which is why all six start at {@code 0}.
	 */
	public long[] reduceSpellDelay = new long[6];

	/**
	 * ⚠️ A tick countdown, not a timestamp, despite the {@code long} type: <b>Soul Split</b>
	 * (curse {@code curseActive[18]}). Set to {@code 4} on a soul-split hit in {@code Curse},
	 * decremented in {@code Curse.handleProcess()}, and {@code > 0} blocks a repeat trigger.
	 */
	public long ssDelay;

	/**
	 * ⚠️ A tick countdown, not a timestamp: set to {@code 2} when claws activate and
	 * decremented once per tick in {@code Client}; {@code > 0} means claws are still active.
	 */
	public int clawDelay;

	/** Earliest epoch-millis at which bones may be buried again. */
	public long buryDelay;

	/** Earliest epoch-millis at which the dragonfire shield special may be used again. */
	public long dfsDelay;

	/** Earliest epoch-millis at which protection-from-magic may be re-toggled. */
	public long protMageDelay;

	/** Earliest epoch-millis at which protection-from-melee may be re-toggled. */
	public long protMeleeDelay;

	/** Earliest epoch-millis at which protection-from-range may be re-toggled. */
	public long protRangeDelay;

	/** Earliest epoch-millis at which a duel-related action may be taken again ({@code now() - duelDelay > 800}). */
	public long duelDelay;

	/** Epoch-millis of the last combat action, used to hold a disconnected player for 10s. {@code 0} = out of combat. */
	public long logoutDelay;

	/** Earliest epoch-millis at which telekinetic grab may be cast again. */
	public long teleGrabDelay;

	/** Epoch-millis combat last ended; the player is "in combat" while {@code now() - this < 3300}. */
	public long singleCombatDelay;

	/** Second single-combat stamp; the player is clear only when {@code now() - this > 3300}. */
	public long singleCombatDelay2;
}
