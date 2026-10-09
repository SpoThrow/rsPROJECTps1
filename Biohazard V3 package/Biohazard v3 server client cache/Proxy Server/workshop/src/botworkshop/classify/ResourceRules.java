package botworkshop.classify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import botworkshop.data.LocDefinition;

/**
 * Turns an object definition into the icon category the map view draws — {@code BOT_TOOLING.md} §1b.
 *
 * <p>The rule set is a table, so a new category is a row rather than code. Each row names a kind, says
 * whether it is a service or a resource, and lists the object actions and names that identify it.
 *
 * <p>Two passes, and the order matters. Actions are matched across every row first; only if no action
 * matched is the name consulted. That is what makes the bank row work, and it is not a detail — bank
 * booth 2213 carries the actions {@code Use}, {@code Use-quickly} and {@code Collect} and <em>no</em>
 * {@code Bank} action at all, so an action-only table would miss every booth in the world. Chests and
 * the like do expose {@code Bank}, so both checks are needed rather than either one.
 *
 * <p>The classification is deliberately conservative: an object that matches nothing is left
 * unclassified rather than being forced into the nearest category, because a wrong icon on the map is
 * worse than no icon — the author would build a bot around a tree that is really a wall.
 */
public final class ResourceRules {

	/** A category of placeable thing, as the editor's palette and filter panel see it. */
	public static final class Rule {
		private final String kind;
		private final boolean service;
		private final List<String> actions;
		private final List<String> names;
		private final List<String> exactNames;

		Rule(String kind, boolean service, String[] actions, String[] names, String[] exactNames) {
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

	private static final Rule[] RULES = {
			// Resource rows. "chop" rather than "chop down" on purpose: the shipped cache uses
			// three spellings — "Chop down" (79 objects), "Chop-down" (22) and "Chop" (11) — and
			// matching only the first would hide a quarter of the trees in the world.
			row("tree", false, new String[] { "chop" }),
			// "mine" also covers "Mine-through", which is a mineable tunnel.
			row("rock", false, new String[] { "mine" }),
			row("fishing", false, new String[] { "net", "bait", "lure", "cage", "harpoon" }),
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
			row("bank", true, new String[] { "bank" },
					new String[] { "bank booth", "bank chest" }, new String[] { "bank" }),
			row("cooking", true, new String[] { "cook" }),
			row("smithing", true, new String[] { "smith" }),
			// "pray" covers all three spellings present: "Pray" (30), "Pray-at" (30), "Pray at" (3).
			row("prayer", true, new String[] { "pray", "recharge" }),
	};

	private ResourceRules() {
	}

	private static Rule row(String kind, boolean service, String[] actions) {
		return new Rule(kind, service, actions, new String[0], new String[0]);
	}

	private static Rule row(String kind, boolean service, String[] actions, String[] names,
			String[] exactNames) {
		return new Rule(kind, service, actions, names, exactNames);
	}

	/** Every row, in match order. */
	public static List<Rule> rules() {
		return Collections.unmodifiableList(Arrays.asList(RULES));
	}

	/**
	 * The kind this object belongs to, or {@code null} when it is ordinary scenery.
	 *
	 * <p>Never throws for a definition the cache failed to parse: an unparsed definition has no
	 * actions and no name, so it simply matches nothing. The validator is where a truncated
	 * definition is treated as an error.
	 */
	public static String classify(LocDefinition def) {
		if (def == null) {
			return null;
		}
		for (Rule rule : RULES) {
			if (matchesAction(rule, def)) {
				return rule.kind;
			}
		}
		for (Rule rule : RULES) {
			if (matchesName(rule, def)) {
				return rule.kind;
			}
		}
		return null;
	}

	/** The rule for {@code kind}, or {@code null}. */
	public static Rule rule(String kind) {
		for (Rule rule : RULES) {
			if (rule.kind.equals(kind)) {
				return rule;
			}
		}
		return null;
	}

	/** True when {@code kind} is a service rather than a resource. */
	public static boolean isService(String kind) {
		Rule rule = rule(kind);
		return rule != null && rule.service;
	}

	private static boolean matchesAction(Rule rule, LocDefinition def) {
		for (String action : def.actions()) {
			String lower = action.toLowerCase();
			for (String keyword : rule.actions) {
				if (lower.contains(keyword)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean matchesName(Rule rule, LocDefinition def) {
		if (def.name() == null) {
			return false;
		}
		String lower = def.name().toLowerCase();
		for (String keyword : rule.names) {
			if (lower.contains(keyword)) {
				return true;
			}
		}
		for (String exact : rule.exactNames) {
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
