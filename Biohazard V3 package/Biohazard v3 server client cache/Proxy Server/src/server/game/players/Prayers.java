package server.game.players;

/**
 * The player's prayer and curse state, reached as {@code c.prayers}.
 *
 * <p>Extracted in §4.12 as a <em>sub-slice</em> of the combat pass: this is the mutable
 * per-player state only. The {@code PRAYER_*} and {@code CURSE_*} lookup tables
 * ({@code PRAYER_GLOW}, {@code PRAYER_DRAIN_RATE}, {@code CURSE_DRAIN}, ...) deliberately stayed
 * on {@link Player} for the same reason the skill-index constants did — they are reference data
 * rather than per-player state, and the already-migrated button modules read
 * {@code PRAYER_GLOW}/{@code CURSE_GLOW} directly. Likewise {@code prayerDelay} stayed behind:
 * it has zero references anywhere in the tree (see §4.11).
 *
 * <p><b>The two defaulted fields are not zero, and both are load-bearing.</b>
 * <ul>
 * <li>{@link #prayerPoint} is a <em>fractional drain accumulator</em>, not a point counter; it
 * starts at {@code 1.0}. {@code CombatAssistant.handlePrayerDrain} subtracts the per-tick drain
 * and, when it drops to {@code <= 0}, wraps it with {@code prayerPoint = 1.0 + prayerPoint} —
 * i.e. {@code 1.0} is the unit modulus and the negative remainder is the carry into the next
 * point. Starting it at {@code 0} would deduct a prayer point on the very first tick.</li>
 * <li>{@link #prayerId} defaults to {@code -1} ("none"), matching the {@code -1}-as-none
 * convention the appearance icons use. ⚠️ It is <em>write-only</em>: it is assigned
 * {@code -1} in {@code CombatAssistant.reducePrayerLevel} and never read.</li>
 * </ul>
 *
 * <p>⚠️ {@link #usingPrayer} is also <em>write-only</em> — recomputed as true/false in
 * {@code getPrayerDelay} and {@code handlePrayerDrain}, but never consulted. Both were moved
 * with the cluster rather than deleted, so that removing them stays a deliberate decision
 * (the same treatment §4.11 gave {@code saveTimer}, and §4.10 gave {@code teleGrabDelay}).
 *
 * <p>⚠️ The two arrays here are read by index conventions owned elsewhere: {@link #prayerActive}
 * is indexed by the {@code PRAYER_*} tables and the prayer buttons on {@link Player}'s side,
 * {@link #curseActive} by {@code CURSE_*} (indices up to 18 are used, so its length must stay 20).
 * A length change here is a silent out-of-bounds waiting to happen.
 *
 * <p>This is a data bag on purpose. Extracting the cluster and encapsulating it are separate
 * steps, so that a behaviour change cannot hide inside the mechanical move.
 */
public final class Prayers {

	/** Which of the 26 standard prayers are on. Index space is {@code Player.PRAYER_*}; length must stay 26. */
	public boolean[] prayerActive = {
		false,false,false,false,false,false,false,false,false,false,false,false,false,
		false,false,false,false,false,false,false,false,false,false,false,false,false
	};

	/** Which of the 20 curses are on. Index space is {@code Player.CURSE_*}; curses up to index 18 are used, so length must stay 20. */
	public boolean[] curseActive = {
		false,false,false,false,false,
		false,false,false,false,false,
		false,false,false,false,false,
		false,false,false,false,false
	};

	/**
	 * ⚠️ Fractional prayer drain accumulator, default {@code 1.0} and not {@code 0}: the drain
	 * wraps it as {@code 1.0 + prayerPoint} when it reaches {@code <= 0}, so {@code 1.0} is the
	 * unit modulus. See the class note.
	 */
	public double prayerPoint = 1.0;

	/** ⚠️ Epoch-millis of the last prayer/curse toggle; blocks re-toggling while {@code now() - this < 5000}. */
	public long stopPrayerDelay;

	/** ⚠️ Write-only: set to {@code -1} in {@code reducePrayerLevel}, never read. {@code -1} = none. */
	public int prayerId = -1;

	/** ⚠️ Write-only: recomputed in {@code getPrayerDelay}/{@code handlePrayerDrain}, never read. */
	public boolean usingPrayer;
}
