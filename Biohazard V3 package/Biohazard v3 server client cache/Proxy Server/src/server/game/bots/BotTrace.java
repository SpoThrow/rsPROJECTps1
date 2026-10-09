package server.game.bots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One bot's recent history — roadmap Phase F, the thing that turns "my bot is doing something weird"
 * into a readable answer.
 *
 * <p><b>What it records and what it does not.</b> Events are <em>transitions</em>: a state becoming
 * current ({@link Kind#ENTER}) and a state reporting an outcome ({@link Kind#SUCCESS}/{@link Kind#FAILURE},
 * or {@link Kind#ABORT} when it is interrupted). A state that is merely still running does not write
 * anything, so a leaf that walks for two hundred ticks costs two events, not two hundred. That is what
 * keeps this affordable on the single game thread (roadmap §7).
 *
 * <p><b>The path is derived, not passed in.</b> The tree contract is that {@code enter}/{@code exit} are
 * strictly nested, so the sequence of enters not yet matched by an outcome <em>is</em> the current path —
 * root first, deepest leaf last. Maintaining it here rather than in each node means a node cannot forget
 * to report its depth, and a tree assembled by any means traces correctly as long as its nodes report.
 *
 * <p><b>Where the reports come from.</b> {@link Traced} wraps each node and calls into this class through
 * {@link BotContext#trace()}. Nothing else in the bot package needs to know a trace exists.
 *
 * <p><b>A reason is optional and comes from the state itself.</b> A {@link BotStatus} carries no text, so
 * a leaf that knows <em>why</em> it gave up calls {@link #note} with one sentence before returning; the
 * nearest enclosing traced node picks it up and attaches it to the event it records. Without a note the
 * event still says how long the state ran, which is the {@code (stuck 40t)} half of the roadmap's
 * example log.
 *
 * <p><b>Bounded by construction.</b> The buffer is a fixed ring: recording is O(1), and the oldest event
 * is overwritten rather than the newest dropped — a fresh failure is always visible.
 */
public final class BotTrace {

	/** What kind of transition an event is. */
	public enum Kind {

		/** A state became current. */
		ENTER,

		/** A state reported its job done. */
		SUCCESS,

		/** A state reported it cannot finish. */
		FAILURE,

		/** A state was left before it reported an outcome: death, reset, despawn, shutdown. */
		ABORT
	}

	/** Events kept per bot. Tens of transitions, not thousands: enough to see the last few cycles. */
	public static final int DEFAULT_CAPACITY = 64;

	/** One recorded transition. Immutable. */
	public static final class Event {

		private final long tick;
		private final Kind kind;
		private final String name;
		private final int ticksInState;
		private final String note;

		Event(long tick, Kind kind, String name, int ticksInState, String note) {
			this.tick = tick;
			this.kind = kind;
			this.name = name;
			this.ticksInState = ticksInState;
			this.note = note;
		}

		/** The trace's tick counter when this happened; 0 is attach time. */
		public long tick() {
			return tick;
		}

		public Kind kind() {
			return kind;
		}

		/** The state's own {@link BotState#name()}. */
		public String name() {
			return name;
		}

		/** Ticks the state was current before reporting this outcome; 0 for an ENTER. */
		public int ticksInState() {
			return ticksInState;
		}

		/** The state's one-line reason for this outcome, or null. */
		public String note() {
			return note;
		}

		/** One line for a console or a chat dump, e.g. {@code t=42 WalkToNearest -> FAILURE (after 40t)}. */
		public String describe() {
			StringBuilder line = new StringBuilder();
			line.append("t=").append(tick).append(' ').append(name);
			if (kind == Kind.ENTER) {
				return line.toString();
			}
			line.append(" -> ").append(kind).append(" (after ").append(ticksInState).append('t');
			if (note != null && !note.isBlank()) {
				line.append(", ").append(note);
			}
			line.append(')');
			return line.toString();
		}

		@Override
		public String toString() {
			return describe();
		}
	}

	private final Event[] ring;
	private int next;
	private int recorded;
	private long tick;
	private final List<String> path = new ArrayList<String>();
	private Event lastFailure;
	private String pendingNote;

	public BotTrace() {
		this(DEFAULT_CAPACITY);
	}

	public BotTrace(int capacity) {
		this.ring = new Event[Math.max(1, capacity)];
	}

	/**
	 * The reason a state is about to report. Cleared when the next event consumes it, so a note left by a
	 * state that then keeps running does not attach itself to an unrelated outcome later.
	 */
	public void note(String reason) {
		this.pendingNote = reason;
	}

	/** The tick counter, advanced once per game tick by the controller. */
	public long currentTick() {
		return tick;
	}

	/** Advances the tick counter. Called once per tick by {@link BotController}, before the tree ticks. */
	void advance() {
		tick++;
	}

	/** Records a state becoming current and pushes it onto the path. */
	void entered(String name, long now) {
		path.add(name);
		record(new Event(now, Kind.ENTER, name, 0, null));
	}

	/**
	 * Records an outcome and pops the state off the path.
	 *
	 * <p>The pop is by name and forgiving: if the top is not {@code name} the stack is unwound to the
	 * nearest match, because a wrong path is worse than a short one and a node that reports late must not
	 * be able to corrupt every path after it.
	 */
	void outcome(String name, Kind kind, int ticksInState, long now) {
		for (int i = path.size() - 1; i >= 0; i--) {
			if (path.get(i).equals(name)) {
				while (path.size() > i) {
					path.remove(path.size() - 1);
				}
				break;
			}
		}
		String note = pendingNote;
		pendingNote = null;
		Event event = new Event(now, kind, name, ticksInState, note);
		record(event);
		if (kind == Kind.FAILURE) {
			lastFailure = event;
		}
	}

	private void record(Event event) {
		ring[next] = event;
		next = (next + 1) % ring.length;
		recorded++;
	}

	/** The current path: root first, the state that is running now last. Empty when nothing is current. */
	public List<String> path() {
		return Collections.unmodifiableList(new ArrayList<String>(path));
	}

	/** The path as one readable line, or {@code "(idle)"} when nothing is current. */
	public String pathLine() {
		if (path.isEmpty()) {
			return "(idle)";
		}
		StringBuilder line = new StringBuilder();
		for (int i = 0; i < path.size(); i++) {
			if (i > 0) {
				line.append(" > ");
			}
			line.append(path.get(i));
		}
		return line.toString();
	}

	/** The last {@code n} events, oldest first. Fewer when fewer have happened. */
	public List<Event> history(int n) {
		int size = Math.min(recorded, ring.length);
		int take = Math.min(Math.max(0, n), size);
		List<Event> out = new ArrayList<Event>(take);
		// Walk back from the newest, then reverse: the ring only knows where the cursor is.
		for (int i = 0; i < take; i++) {
			out.add(ring[(next - 1 - i + ring.length * 2) % ring.length]);
		}
		Collections.reverse(out);
		return out;
	}

	/** The most recent failure, or null. Survives the event leaving the ring: it is the one you need. */
	public Event lastFailure() {
		return lastFailure;
	}

	/** Total events ever recorded, including ones the ring has since overwritten. */
	public int recordedCount() {
		return recorded;
	}
}
