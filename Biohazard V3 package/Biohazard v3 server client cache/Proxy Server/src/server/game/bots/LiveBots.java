package server.game.bots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the live bots are doing, as JSON — {@code BOT_TOOLING.md} Stage T7's read-only half, and the
 * data behind {@code ::botinfo} made reachable from outside the game.
 *
 * <p><b>Why this exists when {@code ::botinfo} already answers.</b> {@code ::botinfo} needs a logged-in
 * owner and a chat window, so it can only be read by someone already in the world; the workshop's live
 * view needs the same facts from a browser beside the map, where the bot's tile means something. Nothing
 * new is measured here: position, the tree's root, the trace's current path, its last failure and the
 * tick budget's stats are all things the runtime already keeps for its own reasons.
 *
 * <p><b>A view, not a snapshot.</b> The reading is done on the HTTP thread and the writing happens on the
 * game thread, so the two overlap. That is a deliberate trade rather than an oversight: a consistent
 * snapshot would mean copying every bot's trace once per tick on the tick thread, and the whole point of
 * the tick budget ({@code BOT_ROADMAP.md} §5.7) is that per-tick work stays bounded. A debug view read a
 * few times a second does not need a barrier — it needs to not throw and not lie about which bot it is
 * describing, and what follows from that is what this class is careful about:
 *
 * <ul>
 * <li><b>Nothing is dereferenced that could be null.</b> {@link BotManager#all()} copies the live list
 *     while the game thread may be adding to it, so an entry can be null; a bot can be possessed with no
 *     controller; a trace can be empty. Each is answered with text.
 * <li><b>Nothing structural is read.</b> Collections are read through the accessors that copy
 *     ({@link BotManager#all()}, {@link BotTrace#path()}, {@link BotTrace#history(int)}), which is what
 *     keeps a concurrent mutation from failing the request.
 * <li><b>Values may be one tick stale, and that is stated rather than hidden.</b> A bot's tile and its
 *     trace path can be read a tick apart, so the path occasionally describes the state the bot was in
 *     when it was on the neighbouring tile. For "why is my bot doing that", that is the same answer.
 * </ul>
 *
 * <p><b>The JSON is written by hand.</b> The server's {@link server.game.bots.script.Json} is a reader —
 * deliberately, because the tool owns the writing half — and the workshop's writer lives in a source set
 * the server must not depend on. Six fields per bot is well under the point where a dependency would pay
 * for itself, so this writes its own and escapes its own strings.
 */
public final class LiveBots {

	/** The path this is served at, and the only one. Shared with the server that serves it. */
	public static final String PATH = "/live/bots";

	/** How many recent transitions to report per bot. Enough for about two gather cycles. */
	public static final int HISTORY = 10;

	private LiveBots() {
	}

	/**
	 * One bot as the viewer sees it.
	 *
	 * <p>A value object so the JSON can be tested without a game: the fields are exactly what the report
	 * reads, and {@link #views()} is the only part that touches a live bot. Immutable, because a view that
	 * could change under the writer would be a worse seam than no seam.
	 */
	public static final class View {

		private final String name;
		private final String script;
		private final int x;
		private final int y;
		private final int plane;
		private final String tree;
		private final long tick;
		private final List<String> path;
		private final String lastFailure;
		private final List<String> history;

		public View(String name, String script, int x, int y, int plane, String tree, long tick,
				List<String> path, String lastFailure, List<String> history) {
			this.name = name;
			this.script = script;
			this.x = x;
			this.y = y;
			this.plane = plane;
			this.tree = tree;
			this.tick = tick;
			this.path = copy(path);
			this.lastFailure = lastFailure;
			this.history = copy(history);
		}

		public String name() {
			return name;
		}

		public String script() {
			return script;
		}

		public int x() {
			return x;
		}

		public int y() {
			return y;
		}

		public int plane() {
			return plane;
		}

		public String tree() {
			return tree;
		}

		public long tick() {
			return tick;
		}

		/** Root first, the state running now last. Empty when nothing is current. */
		public List<String> path() {
			return path;
		}

		public String lastFailure() {
			return lastFailure;
		}

		public List<String> history() {
			return history;
		}

		@Override
		public String toString() {
			return name + " (" + script + ") at " + x + "," + y + " p" + plane;
		}

		private static List<String> copy(List<String> values) {
			if (values == null) {
				return Collections.emptyList();
			}
			List<String> out = new ArrayList<String>(values.size());
			for (String value : values) {
				// Nulls are dropped rather than written: a list copied while the game thread mutates it
				// can hold one, and "null" in a name reads as a state called null.
				if (value != null) {
					out.add(value);
				}
			}
			return Collections.unmodifiableList(out);
		}
	}

	/**
	 * Every live bot, as views. The one method here that reads the running server.
	 *
	 * <p>Reads through {@link BotManager#all()} so a bot added or released mid-request cannot fail the
	 * call, and tolerates a null entry for the same reason.
	 */
	public static List<View> views() {
		List<View> out = new ArrayList<View>();
		for (BotPlayer bot : BotManager.all()) {
			if (bot == null) {
				continue;
			}
			out.add(view(bot));
		}
		return out;
	}

	private static View view(BotPlayer bot) {
		String name = bot.playerName;
		BotController controller = bot.controller();
		// The row's script, not the attached tree: a bot whose row was edited but has not been respawned
		// is still running the old tree, and the `tree` field below is what says so. Whether the named
		// script exists is a different question with its own offline answer (`workshopResolveScripts`),
		// and reading the script registry from this thread would mean reading a map the game thread
		// rewrites on `::bot reload` — not worth it for a label nothing acts on.
		String script = scriptOf(name);
		String tree = controller == null ? "(possessed, no script attached)" : controller.root().name();
		BotTrace trace = controller == null ? null : controller.trace();
		return new View(name, script, bot.getX(), bot.getY(), bot.position.heightLevel, tree,
				trace == null ? 0 : trace.currentTick(),
				trace == null ? List.<String>of() : trace.path(),
				trace == null || trace.lastFailure() == null ? null : trace.lastFailure().describe(),
				trace == null ? List.<String>of() : describes(trace.history(HISTORY)));
	}

	/**
	 * The configured script for {@code account}, or a placeholder when there is no row for it.
	 *
	 * <p>A bot with no row is a real state — {@code ::bot spawn} takes a row, but {@code possess} can be
	 * called directly — and saying so is more useful than an empty string.
	 */
	private static String scriptOf(String name) {
		BotProfile profile = BotManager.profileFor(name);
		return profile == null ? "(no config row)" : profile.script();
	}

	private static List<String> describes(List<BotTrace.Event> events) {
		List<String> out = new ArrayList<String>(events.size());
		for (BotTrace.Event event : events) {
			// Null-checked: the ring is written by the game thread, so an entry read mid-write can be
			// absent, and a missing line is better than a request that fails.
			if (event != null) {
				out.add(event.describe());
			}
		}
		return out;
	}

	/** The whole report: the tick budget's stats, the cap, and every live bot. */
	public static String toJson() {
		return toJson(views(), BotManager.tickStats(), BotManager.count(), BotManager.MAX_BOTS);
	}

	/**
	 * The report over views a caller supplies, so the JSON is checked without a running server.
	 *
	 * @param views the bots to report, in the order they should appear
	 * @param stats {@link BotManager#tickStats()}, or any text describing this tick
	 * @param count how many bots are live — passed rather than derived, because a test supplies views
	 *              that are not a live population and the two numbers must not be conflated
	 * @param cap   {@link BotManager#MAX_BOTS}
	 */
	public static String toJson(List<View> views, String stats, int count, int cap) {
		StringBuilder out = new StringBuilder();
		out.append('{');
		field(out, "path", PATH);
		out.append(',');
		number(out, "count", count);
		out.append(',');
		number(out, "cap", cap);
		out.append(',');
		field(out, "stats", stats);
		out.append(',');
		name(out, "bots");
		out.append('[');
		boolean first = true;
		for (View view : views) {
			if (view == null) {
				continue;
			}
			if (!first) {
				out.append(',');
			}
			first = false;
			writeBot(out, view);
		}
		out.append("]}");
		return out.toString();
	}

	private static void writeBot(StringBuilder out, View view) {
		out.append('{');
		field(out, "name", view.name());
		out.append(',');
		field(out, "script", view.script());
		out.append(',');
		number(out, "x", view.x());
		out.append(',');
		number(out, "y", view.y());
		out.append(',');
		number(out, "plane", view.plane());
		out.append(',');
		field(out, "tree", view.tree());
		out.append(',');
		number(out, "tick", view.tick());
		out.append(',');
		field(out, "path", String.join(" > ", view.path()));
		out.append(',');
		field(out, "lastFailure", view.lastFailure());
		out.append(',');
		name(out, "history");
		out.append('[');
		for (int i = 0; i < view.history().size(); i++) {
			if (i > 0) {
				out.append(',');
			}
			value(out, view.history().get(i));
		}
		out.append("]}");
	}

	// ---- the smallest JSON writer that can carry this report ------------------------------------

	private static void name(StringBuilder out, String key) {
		value(out, key);
		out.append(':');
	}

	private static void field(StringBuilder out, String key, String text) {
		name(out, key);
		value(out, text);
	}

	private static void number(StringBuilder out, String key, long value) {
		name(out, key);
		out.append(value);
	}

	/** A JSON string, or {@code null}. Escaped rather than trusted: a bot's name and a trace note are
	 * text the server did not compose, and a bare quote in one would make the whole document unparseable
	 * — which the viewer would report as a broken server rather than as the odd name it is. */
	private static void value(StringBuilder out, String text) {
		if (text == null) {
			out.append("null");
			return;
		}
		out.append('"');
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			switch (c) {
			case '"':
				out.append("\\\"");
				break;
			case '\\':
				out.append("\\\\");
				break;
			case '\n':
				out.append("\\n");
				break;
			case '\r':
				out.append("\\r");
				break;
			case '\t':
				out.append("\\t");
				break;
			default:
				if (c < 0x20) {
					out.append(String.format("\\u%04x", (int) c));
				} else {
					out.append(c);
				}
			}
		}
		out.append('"');
	}
}
