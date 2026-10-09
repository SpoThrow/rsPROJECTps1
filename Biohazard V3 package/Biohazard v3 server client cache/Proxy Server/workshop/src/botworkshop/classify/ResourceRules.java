package botworkshop.classify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import botworkshop.data.LocDefinition;
import server.game.bots.world.ResourceKinds;

/**
 * The workshop's view of the icon-category table — {@code BOT_TOOLING.md} §1b.
 *
 * <p><b>This class owns no rules.</b> The table lives in {@link ResourceKinds}, in the server source
 * set, because the runtime needs the identical answers when a bot asks "nearest oak"
 * ({@code BOT_ROADMAP.md} phase C). The tool compiles against the server, so the dependency runs the
 * right way; keeping a second copy here would let the map draw an icon for something the bot cannot
 * find. This class exists only so the editor and its tests keep the named {@link Rule} shape they
 * were written against.
 */
public final class ResourceRules {

	/** A category of placeable thing, as the editor's palette and filter panel see it. */
	public static final class Rule {
		private final ResourceKinds.Row row;

		Rule(ResourceKinds.Row row) {
			this.row = row;
		}

		/** Stable id, e.g. {@code "tree"}. */
		public String kind() {
			return row.kind();
		}

		/** True for services (bank, range, anvil, altar) rather than gatherable resources. */
		public boolean isService() {
			return row.isService();
		}

		public List<String> actions() {
			return row.actions();
		}

		public List<String> names() {
			return row.names();
		}

		/** Names that must match in full, for words too generic to match as a substring. */
		public List<String> exactNames() {
			return row.exactNames();
		}

		@Override
		public String toString() {
			return row.toString();
		}
	}

	private static final List<Rule> RULES = build();

	private ResourceRules() {
	}

	private static List<Rule> build() {
		List<Rule> rules = new ArrayList<Rule>();
		for (ResourceKinds.Row row : ResourceKinds.rows()) {
			rules.add(new Rule(row));
		}
		return Collections.unmodifiableList(rules);
	}

	/** Every row, in match order. */
	public static List<Rule> rules() {
		return RULES;
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
		return ResourceKinds.classify(def.name(), def.actions());
	}

	/** The rule for {@code kind}, or {@code null}. */
	public static Rule rule(String kind) {
		for (Rule rule : RULES) {
			if (rule.kind().equals(kind)) {
				return rule;
			}
		}
		return null;
	}

	/** True when {@code kind} is a service rather than a resource. */
	public static boolean isService(String kind) {
		return ResourceKinds.isService(kind);
	}
}
