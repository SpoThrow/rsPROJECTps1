package server.game.players;

/**
 * The periodic character save.
 *
 * <p><b>Why this exists.</b> Before this, nothing saved on a schedule at all. {@code PlayerSaving}
 * looked like it did — it had a {@link Runnable} loop, a five-minute timer and a
 * {@code saveAllPlayers()} — but {@code initialize()} had <em>no callers anywhere in the tree</em>,
 * so the thread was never started and the loop never ran. A character reached disk only on logout,
 * on the drop/death/barrows paths that call {@code PlayerSave.saveGame} directly, and (after Phase
 * 5) on a clean shutdown. A crash or a hard kill therefore lost <em>everything</em> since each
 * player last logged out.
 *
 * <p><b>It runs on the game thread, one character per tick.</b> The old design's fatal flaw was
 * that it saved every player in a burst from its own background thread, reading live game state
 * while the tick mutated it, and racing the logout path on the same client's file. The two ways
 * out were to snapshot state on the game thread and write it off-thread, or to drive the save from
 * the tick. This is the second: {@link #process()} is called once per tick from
 * {@code Server.tick()}, advances the sweep by at most one character, and returns. No locks, no
 * races, and no burst — at 600&nbsp;ms a tick, even a full realm drains at under two writes a
 * second, and {@code PlayerHandler.process()} has already been allowed to run per-player error
 * isolation, which this matches.
 *
 * <p>⚠️ <b>The one cost is that a character write happens on the tick thread</b>, so a
 * pathologically slow disk delays that tick. That is the deliberate trade: the alternative — a
 * background writer — is the bug this replaces. It is also why only <em>one</em> character is
 * written per tick rather than a whole batch.
 *
 * <p><b>It deliberately does not mark the character as saved.</b> {@link Client#saveCharacterOnce()}
 * is the <em>final</em> write and latches a flag so logout, the update-kick and the shutdown hook
 * cannot write the same character twice. A periodic save must not latch that flag, or the logout
 * that follows it would skip the last few minutes of play. So this calls
 * {@link PlayerSave#saveGame(Client)} directly and leaves the once-guard alone — an autosave
 * followed by a logout writes twice, on purpose, and the second write is the one that counts.
 */
public final class PlayerSaving {

	/** How often a full sweep starts, in milliseconds. Matches the five minutes this class always intended. */
	public static final long SAVE_INTERVAL_MS = 5 * 60 * 1000;

	/** The slot the sweep will visit next. Zero means "between sweeps", not "slot zero". */
	private static int nextSlot;

	/** When the current wait began — or, mid-sweep, when that sweep began. */
	private static long lastSweepStart = System.currentTimeMillis();

	/** Characters written by the sweep in progress, for the completion line. */
	private static int sweepSaved;

	private PlayerSaving() {
	}

	/** One tick's worth of the sweep. Called from {@code Server.tick()} after the handlers. */
	public static void process() {
		process(System.currentTimeMillis());
	}

	/**
	 * The sweep, with the clock passed in so a test can drive it.
	 *
	 * <p>{@code nextSlot == 0} doubles as "waiting": the interval is only consulted at the start of
	 * a sweep, so once a sweep begins it runs to completion regardless of how long that takes
	 * (with a full realm it takes longer than the interval, and overlapping sweeps are not wanted).
	 */
	static void process(long now) {
		if (nextSlot == 0 && now - lastSweepStart < SAVE_INTERVAL_MS) {
			return;
		}
		Client[] players = PlayerHandler.players;
		while (nextSlot < players.length) {
			Client player = players[nextSlot++];
			if (player == null || !player.isActive) {
				continue;
			}
			PlayerSave.saveGame(player);
			sweepSaved++;
			return; // one character per tick, so a slow disk can only ever delay one tick
		}
		if (sweepSaved > 0) {
			System.out.println("[Autosave] Saved " + sweepSaved + " character(s).");
		}
		nextSlot = 0;
		lastSweepStart = now;
		sweepSaved = 0;
	}

	/**
	 * Returns the sweep to its starting state, with the wait beginning at {@code now}.
	 *
	 * <p>Package-private for tests, which need a known clock rather than whatever
	 * {@code System.currentTimeMillis()} returned at class-load. Production never calls it.
	 */
	static void reset(long now) {
		nextSlot = 0;
		lastSweepStart = now;
		sweepSaved = 0;
	}

	/** The slot the sweep will visit next; 0 while between sweeps. Exposed so tests can observe the sweep. */
	static int nextSlot() {
		return nextSlot;
	}
}
