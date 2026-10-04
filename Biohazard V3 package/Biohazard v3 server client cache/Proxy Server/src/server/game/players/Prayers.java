package server.game.players;

/**
 * The player's prayer and curse state, reached as {@code c.prayers}.
 *
 * <p>Extracted in §4.12 as a <em>sub-slice</em> of the combat pass: this is the mutable
 * per-player state only. The {@code PRAYER_*} and {@code CURSE_*} lookup tables
 * ({@code PRAYER_GLOW}, {@code PRAYER_DRAIN_RATE}, {@code CURSE_DRAIN}, ...) deliberately stayed
 * on {@link Player} for the same reason the skill-index constants did — they are reference data
 * rather than per-player state, and the already-migrated button modules read
 * {@code PRAYER_GLOW}/{@code CURSE_GLOW} directly.
 *
 * <p><b>{@link #prayerPoint} is not zero, and the default is load-bearing.</b> It is a
 * <em>fractional drain accumulator</em>, not a point counter; it starts at {@code 1.0}.
 * {@code CombatAssistant.handlePrayerDrain} subtracts the per-tick drain and, when it drops to
 * {@code <= 0}, wraps it with {@code prayerPoint = 1.0 + prayerPoint} — i.e. {@code 1.0} is the
 * unit modulus and the negative remainder is the carry into the next point. Starting it at
 * {@code 0} would deduct a prayer point on the very first tick.
 *
 * <p>⚠️ The write-only {@code prayerId} and {@code usingPrayer} that used to live here have been
 * <em>deleted</em>: both were assigned (in {@code reducePrayerLevel} and
 * {@code getPrayerDelay}/{@code handlePrayerDrain} respectively) and never read, so they were
 * state wired to nothing.
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
}
