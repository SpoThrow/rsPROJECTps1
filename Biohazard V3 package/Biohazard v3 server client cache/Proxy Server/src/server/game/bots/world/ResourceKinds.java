package server.game.bots.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import server.clip.region.ObjectDef;

/**
 * The one table that answers "what kind of resource or service is this object?".
 *
 * <p>Two consumers ask that question and they must never disagree:
 *
 * <ul>
 * <li>the <b>runtime</b> ({@link ScannedLocator}), so a bot can find "nearest oak" by looking at
 *     the world it is actually standing in;
 * <li>the <b>workshop</b> icon layer, so the map an author clicks on draws the same tree the bot
 *     will walk to.
 * </ul>
 *
 * <p>This is why the table lives here, in the server source set, rather than in the tool: the tool
 * already compiles against the server, so the dependency runs one way and there is exactly one copy
 * of the rules. A second copy in the tool would be the drift that {@code BOT_TOOLING.md} §11 warns
 * about — an author builds a bot around an icon the runtime does not recognise, and nothing fails
 * until a bot wanders off looking for a tree that was never a tree.
 *
 * <p><b>Two passes, and the order matters.</b> Actions are matched across every row first; only if no
 * action matched is the name consulted. That is what makes the bank row work, and it is not a detail
 * — bank booth 2213 carries the actions {@code Use}, {@code Use-quickly} and {@code Collect} and
 * <em>no</em> {@code Bank} action at all, so an action-only table would miss every booth in the world.
 * Chests and the like do expose {@code Bank}, so both checks are needed rather than either one.
 *
 * <p><b>Conservative on purpose.</b> An object matching nothing is left unclassified rather than
 * forced into the nearest category, because a wrong icon is worse than no icon: the author would
 * build a bot around a tree that is really a wall.
 */
public final class ResourceKinds {

	/** The kind ids, in match order. Kept here so callers never spell a string by hand. */
	public static final String TREE = "tree";
	public static final String ROCK = "rock";
	public static final String FISHING = "fishing";
	public static final String BANK = "bank";
	public static final String COOKING = "cooking";
	public static final String SMITHING = "smithing";
	public static final String PRAYER = "prayer";

	/** One row of the table: a kind, whether it is a service, and what identifies it. */
	public static final class Row {
		private final String kind;
		private final boolean service;
		private final List<String> actions;
		private final List<String> names;
		private final List<String> exactNames;

		Row(String kind, boolean service, String[] actions, String[] names, String[] exactNames) {
			this.kind = kind;
			this.service = service;
			this.actions = lowercase(actions);
			this.names = lowercase(names);
			this.exactNames = lowercase(exactNames);
		}

		/** Stable id, e.g. {@code "tree"}. */
		public String kind() {
			return kind;
		}

		/** True for services (bank, range, anvil, altar) rather than gatherable resources. */
		public boolean isService() {
			return service;
		}

		public List<String> actions() {
			return actions;
		}

		public List<String> names() {
			return names;
		}

		/** Names that must match in full, for words too generic to match as a substring. */
		public List<String> exactNames() {
			return exactNames;
		}

		@Override
		public String toString() {
			return kind + (service ? " (service)" : " (resource)");
		}
	}

	private static final Row[] ROWS = {
			// Resource rows. "chop" rather than "chop down" on purpose: the shipped cache uses
			// three spellings — "Chop down" (79 objects), "Chop-down" (22) and "Chop" (11) — and
			// matching only the first would hide a quarter of the trees in the world.
			row(TREE, false, new String[] { "chop" }),
			// "mine" also covers "Mine-through", which is a mineable tunnel.
			row(ROCK, false, new String[] { "mine" }),
			row(FISHING, false, new String[] { "net", "bait", "lure", "cage", "harpoon" }),
			// Service rows.
			//
			// ⚠ The cooking, smithing and prayer-recharge actions in BOT_TOOLING.md §1b match
			// NOTHING in the shipped cache: measured across loc.dat, "Cook" 0, "Smith" 0 and
			// "Recharge" 0 objects. Ranges and anvils are not identifiable by action here, so the
			// icon layer cannot show them until a name- or id-based rule is added. The rows are
			// kept because they are correct in principle and cost nothing, but they are known-dead
			// for this cache rather than quietly assumed to work — ResourceRulesTest pins that.
			// "bank booth" and "bank chest" match as substrings, but the bare word "bank" must match
			// in full: as a substring it also catches "Bank wall", "Bank sign", "Bank table" and
			// the rest of the bank building's scenery, which put a bank icon on 311 objects where
			// the correct count is 176.
			row(BANK, true, new String[] { "bank" },
					new String[] { "bank booth", "bank chest" }, new String[] { "bank" }),
			row(COOKING, true, new String[] { "cook" }),
			row(SMITHING, true, new String[] { "smith" }),
			// "pray" covers all three spellings present: "Pray" (30), "Pray-at" (30), "Pray at" (3).
			row(PRAYER, true, new String[] { "pray", "recharge" }),
	};

	private ResourceKinds() {
	}

	private static Row row(String kind, boolean service, String[] actions) {
		return new Row(kind, service, actions, new String[0], new String[0]);
	}

	private static Row row(String kind, boolean service, String[] actions, String[] names,
			String[] exactNames) {
		return new Row(kind, service, actions, names, exactNames);
	}

	/** Every row, in match order. */
	public static List<Row> rows() {
		return Collections.unmodifiableList(Arrays.asList(ROWS));
	}

	/** The row for {@code kind}, or {@code null}. */
	public static Row row(String kind) {
		for (Row row : ROWS) {
			if (row.kind.equals(kind)) {
				return row;
			}
		}
		return null;
	}

	/** True when {@code kind} is a service rather than a resource. */
	public static boolean isService(String kind) {
		Row row = row(kind);
		return row != null && row.service;
	}

	/**
	 * The kind a definition belongs to, or {@code null} when it is ordinary scenery.
	 *
	 * <p>{@code actions} may be {@code null} and may contain {@code null} slots — {@code ObjectDef}
	 * leaves a hole where the {@code hidden} action was, and reading it as a string would throw.
	 */
	public static String classify(ObjectDef def) {
		if (def == null) {
			return null;
		}
		return classify(def.name, def.actions);
	}

	/** The kind an already-decoded name/actions pair belongs to, or {@code null}. */
	public static String classify(String name, String[] actions) {
		for (Row row : ROWS) {
			if (matchesAction(row, actions)) {
				return row.kind;
			}
		}
		for (Row row : ROWS) {
			if (matchesName(row, name)) {
				return row.kind;
			}
		}
		return null;
	}

	/** The same, for a caller holding a list (the tool's decoded records). */
	public static String classify(String name, List<String> actions) {
		for (Row row : ROWS) {
			if (matchesAction(row, actions)) {
				return row.kind;
			}
		}
		for (Row row : ROWS) {
			if (matchesName(row, name)) {
				return row.kind;
			}
		}
		return null;
	}

	private static boolean matchesAction(Row row, String[] actions) {
		if (actions == null) {
			return false;
		}
		for (String action : actions) {
			if (action != null && containsAny(action, row.actions)) {
				return true;
			}
		}
		return false;
	}

	private static boolean matchesAction(Row row, List<String> actions) {
		if (actions == null) {
			return false;
		}
		for (String action : actions) {
			if (action != null && containsAny(action, row.actions)) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsAny(String action, List<String> keywords) {
		String lower = action.toLowerCase();
		for (String keyword : keywords) {
			if (lower.contains(keyword)) {
				return true;
			}
		}
		return false;
	}

	private static boolean matchesName(Row row, String name) {
		if (name == null) {
			return false;
		}
		String lower = name.toLowerCase();
		for (String keyword : row.names) {
			if (lower.contains(keyword)) {
				return true;
			}
		}
		for (String exact : row.exactNames) {
			if (lower.equals(exact)) {
				return true;
			}
		}
		return false;
	}

	private static List<String> lowercase(String[] values) {
		List<String> out = new ArrayList<String>(values.length);
		for (String value : values) {
			out.add(value.toLowerCase());
		}
		return Collections.unmodifiableList(out);
	}
}
