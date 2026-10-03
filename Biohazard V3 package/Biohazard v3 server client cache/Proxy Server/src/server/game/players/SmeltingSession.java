package server.game.players;

import server.content.skills.Smelting;

/**
 * Per-player state for the smelting skill.
 *
 * <p>These six fields used to sit directly on {@link Player}. They are grouped here
 * because every one of them is read or written only from {@link Smelting}, and they all
 * die with the smelt session. Reached as {@code player.smelt}.
 *
 * <p>This is a deliberate data bag -- the fields stay public, exactly as they were on
 * {@link Player}. Extracting the cluster is one step; encapsulating it is another, so
 * that a behaviour change cannot hide inside a mechanical move.
 *
 * <p><b>Three things this move turned up, none of them changed here</b> (see the Phase 4.2
 * note in REFACTORING_PLAN.md):
 * <ul>
 * <li>{@link #active} is <em>write-only</em>. It is set true when the interface opens and
 *     false on reset, and never read by anything. It is kept rather than deleted so this
 *     pass stays behaviour-neutral.</li>
 * <li>{@link #barType} is a redundant intermediate. It is written, read once to derive
 *     {@link #bar}, and cleared -- {@code Smelting.startSmelting} could hold it in a local
 *     instead of on the player.</li>
 * <li>{@link #eventId} is never reassigned, so it is really the constant 5567.</li>
 * </ul>
 */
public final class SmeltingSession {

	/**
	 * Set when the smelt interface opens, cleared on reset. Write-only as of Phase 4.2 --
	 * nothing reads it, so it could not affect behaviour if it were removed.
	 */
	public boolean active;

	/** Bars still to smelt. Decremented once per completed bar. */
	public int amount;

	/** The bar being smelted, looked up from {@link #barType}. */
	public Smelting.Bars bar;

	/** Only ever used to derive {@link #bar}. See the class note. */
	public String barType = "";

	/** Timestamp of the last completed bar, for the 1s throttle. */
	public long lastSmelt;

	/** Cycle-event id passed to {@code CycleEventHandler}. Never reassigned. */
	public int eventId = 5567;
}
