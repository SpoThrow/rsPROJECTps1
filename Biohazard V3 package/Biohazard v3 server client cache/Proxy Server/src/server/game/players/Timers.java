package server.game.players;

/**
 * The player's timer bag: availability clocks, durations and countdowns, reached as
 * {@code c.timers}.
 *
 * <p>Extracted in two passes. The first (§4.10) took the eighteen pure {@code *Delay}
 * availability clocks; the second (§4.11) took the duration/scheduling group, which reunites
 * {@link #teleBlockLength} with the {@link #teleBlockDelay} it had been split from and brings
 * the persisted {@link #skullTimer} across.
 *
 * <p><b>Four shapes, and the difference matters.</b>
 * <ol>
 * <li><em>"Last used" epoch-millisecond stamps</em> — {@link #foodDelay} through
 * {@link #singleCombatDelay2}, plus {@link #restoreStatsDelay} — always read against the clock
 * as {@code System.currentTimeMillis() - X > threshold} (available again) or {@code < threshold}
 * (still cooling down), for example
 * {@code System.currentTimeMillis() - c.timers.foodDelay > 2000} in {@code PlayerAssistant}.</li>
 * <li><em>Tick countdowns</em>, never compared to the clock, all using {@code > 0} for
 * "active": {@link #clawDelay}, {@link #ssDelay} (Soul Split), {@link #teleTimer},
 * {@link #hitDelay}, {@link #attackTimer} and {@link #freezeDelay}.</li>
 * <li>{@link #respawnTimer}, a countdown with a <em>negative</em> sentinel: it decrements every
 * tick while {@code > -6}, and {@code -6} means "idle". Its declared default is {@code 0}, so a
 * fresh player walks 0 → -6 over his first six ticks instead of starting idle. That is
 * pre-existing behaviour and is left exactly as found.</li>
 * <li>{@link #reduceSpellDelay}, a six-slot table of stamps indexed by {@code reduceSpellId} and
 * compared against {@code REDUCE_SPELL_TIME}.</li>
 * </ol>
 *
 * <p><b>The defaults are load-bearing, and one of them is deliberately not {@code 0}.</b> For
 * the stamps, {@code 0} means "never used", so {@code now() - 0} exceeds any threshold and the
 * action is available immediately — the correct state for a new player. The same value is the
 * reset sentinel ({@code c.timers.teleBlockDelay = 0;} clears a teleblock) and the only value
 * meaning "no claws active". ⚠️ The exception is {@link #freezeTimer}, which defaults to
 * {@code -6} and <em>must</em>: the freeze code tells thawed states apart by exact negative
 * value ({@code > -6}, {@code <= -3}, {@code < -4}), so {@code -6} is a re-freeze immunity
 * window rather than a stand-in for zero. {@code TimersTest} reflects over the class and pins
 * {@code 0} for every field except that one documented exception, so a future field added with
 * an unintended default fails there.
 *
 * <p><b>Save path.</b> {@link #skullTimer} is persisted under the key {@code skull-timer} and
 * {@link #teleBlockLength} under {@code teleblock-length}; both keys are unchanged by the move.
 * The {@code teleblock-length} branch writes <em>both</em> fields, deriving
 * {@code teleBlockDelay = System.currentTimeMillis()} at load.
 *
 * <p>⚠️ The dead {@code specDelay}, {@code poisonDelay} and {@code prayerDelay} (declared on
 * {@link Player}) and this bag's {@code saveTimer} and {@code teleGrabDelay} have been
 * <em>deleted</em>: every one was written and never read. telegrab's requirements are already
 * enforced by {@code checkMagicReqs}.
 *
 * <p>⚠️ <b>Correction (Phase 5): the claim that used to stand here — that {@code saveTimer} was
 * redundant because {@link PlayerSaving} saves every player every five minutes — was wrong.</b>
 * {@code PlayerSaving.initialize()} had no callers anywhere in the tree, so its thread was never
 * started and its five-minute loop never ran. Deleting {@code saveTimer} was still
 * behaviour-neutral (it was write-only, so it never triggered anything), but it removed the
 * <em>name</em> of a real gap rather than redundancy: there was in fact no periodic save, and a
 * character reached disk only on logout, on the drop/death/barrows paths, or — from Phase 5 — on a
 * clean shutdown. **That gap has since been closed:** {@link PlayerSaving} is now driven from the
 * game tick and writes one character per tick, so progress early in a session is no longer held
 * only in memory.
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
	 * reset value. Reunited with {@link #teleBlockLength} in §4.11.
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

	/** Epoch-millis combat last ended; the player is "in combat" while {@code now() - this < 3300}. */
	public long singleCombatDelay;

	/** Second single-combat stamp; the player is clear only when {@code now() - this > 3300}. */
	public long singleCombatDelay2;

	/* ---------------------------------------------------------------------------------------
	 * §4.11 — duration / scheduling group.
	 * ------------------------------------------------------------------------------------- */

	/** Epoch-millis of the last periodic stat restore; re-fires when {@code now() - this > 60000} in {@code Client}. */
	public long restoreStatsDelay;

	/** Tick countdown for an in-progress teleport (set to 9/10/11, down to 0); {@code > 0} means a teleport is running. */
	public int teleTimer;

	/** Tick countdown to a queued hit; {@code == 1} is the tick the delayed damage is applied. */
	public int hitDelay;

	/** Tick countdown until the player may swing again ({@code == 0} ready); reset from {@code getAttackDelay(...)}. */
	public int attackTimer;

	/**
	 * ⚠️ Tick countdown of the player's current freeze, and the one field whose default is
	 * <em>not</em> {@code 0}: it starts at {@code -6} and thawed states are told apart by exact
	 * negative value ({@code > -6}, {@code <= -3}, {@code < -4}), so {@code -6} is a re-freeze
	 * immunity window. See the class note.
	 */
	public int freezeTimer = -6;

	/**
	 * Freeze length of the ice spell just cast ({@code getFreezeTime()}), used to set the
	 * target's {@link #freezeTimer} and to hold the caster still while {@code > 0}; reset to
	 * {@code 0} once applied.
	 */
	public int freezeDelay;

	/** Damage queued for {@link #hitDelay} to apply. */
	public int delayedDamage;

	/** Second queued hit for a double-hit weapon, applied alongside {@link #delayedDamage}. */
	public int delayedDamage2;

	/**
	 * Tick countdown from death to respawn: decrements every tick while {@code > -6}, and
	 * {@code -6} is the idle sentinel. Declared default is {@code 0}, so a fresh player settles
	 * to {@code -6} over his first six ticks — see the class note.
	 */
	public int respawnTimer;

	/** Duration of the current teleblock, compared as {@code now() - teleBlockDelay < teleBlockLength}. Persisted under {@code teleblock-length}. */
	public int teleBlockLength;

	/** PK skull countdown in ticks; {@code -1} means no skull. Persisted under {@code skull-timer}. */
	public int skullTimer;
}
