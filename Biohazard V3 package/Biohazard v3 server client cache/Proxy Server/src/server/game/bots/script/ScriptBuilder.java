package server.game.bots.script;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import server.game.bots.BotState;
import server.game.bots.composite.Repeat;
import server.game.bots.composite.Sequence;
import server.game.bots.states.BankLogs;
import server.game.bots.states.Gather;
import server.game.bots.states.WalkTo;
import server.game.bots.states.WalkToNearest;
import server.game.bots.world.Location;
import server.game.bots.world.LocationKind;
import server.game.bots.world.Locator;
import server.game.bots.world.Locations;
import server.game.bots.world.ResourceScan;

/**
 * The fluent builder behind {@link BotScript#named} — {@code BOT_ROADMAP.md} §5.5.
 *
 * <p><b>Steps accumulate; a terminal makes the script.</b> Reading down the chain is reading the
 * routine:
 *
 * <pre>BotScript.named("gather_oak")
 *     .gatherLoop(LocationKind.TREE, LOGS, LocationKind.BANK)
 *     .forever();</pre>
 *
 * <p>That is one block that declares a gathering bot with no new state class, which is Phase D's
 * acceptance criterion. Accumulating rather than nesting is deliberate: the roadmap's sketch passes leaf
 * factories as arguments to one call, which only reads well if they are static — and a static factory
 * cannot be given a test world. A builder that owns the world can be, which is the difference between a
 * script that is testable and one that is only runnable.
 *
 * <p><b>Steps are factories, not instances.</b> A state holds progress — a {@code WalkTo} remembers the
 * route it issued, a {@code Gather} remembers its target — so a tree built once and handed to two bots
 * would have them overwrite each other's progress. {@link #root()} therefore mints a fresh tree, and
 * each step with it, every time it is called. One registered script can serve every bot that names it.
 *
 * <p><b>Building touches no world.</b> A {@code null} {@link Locations} or {@link ResourceScan} means
 * "the live one, resolved when a state needs it" (see {@code WalkToNearest}). A script can therefore be
 * registered from a static initialiser without reading {@code Data/cfg}.
 */
public final class ScriptBuilder {

	private final String name;
	private final Locations locations;
	private final ResourceScan scan;
	private final List<Supplier<BotState>> steps = new ArrayList<Supplier<BotState>>();

	ScriptBuilder(String name, Locations locations, ResourceScan scan) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("a script needs a name");
		}
		this.name = name;
		this.locations = locations;
		this.scan = scan;
	}

	// ---- steps -----------------------------------------------------------------------------

	/** Walk to an exact tile. The escape hatch for a place no locator knows. */
	public ScriptBuilder walkTo(final int x, final int y, final int range) {
		return step(new Supplier<BotState>() {
			@Override
			public BotState get() {
				return new WalkTo(x, y, range);
			}
		});
	}

	/** Walk to the nearest place of a kind, spreading within its box. */
	public ScriptBuilder walkToNearest(final LocationKind kind, final int range) {
		return step(new Supplier<BotState>() {
			@Override
			public BotState get() {
				Locator<Location> places = locations == null ? null : locations.forKind(kind);
				return new WalkToNearest(places, kind, range, WalkToNearest.SEED_FROM_BOT);
			}
		});
	}

	/** Find the nearest object of a kind near the bot, walk to it and click until the bag is full. */
	public ScriptBuilder gather(final LocationKind kind, final int itemId) {
		return step(new Supplier<BotState>() {
			@Override
			public BotState get() {
				return new Gather(scan, kind, itemId, Gather.DEFAULT_RANGE, ResourceScan.DEFAULT_RADIUS,
						Gather.FIRST_CLICK);
			}
		});
	}

	/** Open the bank and deposit every slot holding {@code itemId}. */
	public ScriptBuilder bankAll(final int itemId) {
		return step(new Supplier<BotState>() {
			@Override
			public BotState get() {
				return new BankLogs(itemId);
			}
		});
	}

	/**
	 * The whole gather cycle, which is what most bots actually are: travel to the resource, gather until
	 * full, travel to the service, empty the bag. {@code BOT_ROADMAP.md} §5.5's {@code GatherLoop}.
	 *
	 * <p>Travel comes first on purpose. {@code Gather} searches a small radius around the bot, so it only
	 * finds anything once the bot is standing in the resource area; a loop that gathered first would fail
	 * in the middle of a field.
	 *
	 * @param resource the kind to gather
	 * @param itemId   the item it yields, watched for progress and then deposited
	 * @param service  the kind to empty the bag at, normally a bank
	 */
	public ScriptBuilder gatherLoop(LocationKind resource, int itemId, LocationKind service) {
		return walkToNearest(resource, Gather.DEFAULT_RANGE)
				.gather(resource, itemId)
				.walkToNearest(service, 2)
				.bankAll(itemId);
	}

	/**
	 * A step the builder does not name — for a node added later, or a composite used as one step. Also a
	 * factory, so the node is built per {@link BotScript#root()} call like every other step.
	 */
	public ScriptBuilder step(Supplier<BotState> factory) {
		if (factory == null) {
			throw new IllegalArgumentException("a step cannot be null");
		}
		steps.add(factory);
		return this;
	}

	// ---- terminals -------------------------------------------------------------------------

	/** Run the steps over and over. The ordinary root for a routine bot. */
	public BotScript forever() {
		requireSteps();
		return build(new Root() {
			@Override
			BotState make(BotState[] children) {
				return new Repeat(new Sequence(children), -1);
			}
		});
	}

	/** Run the steps {@code count} times, then stop. */
	public BotScript times(final int count) {
		requireSteps();
		return build(new Root() {
			@Override
			BotState make(BotState[] children) {
				return new Repeat(new Sequence(children), count);
			}
		});
	}

	/** Run the steps once. */
	public BotScript once() {
		requireSteps();
		return build(new Root() {
			@Override
			BotState make(BotState[] children) {
				return new Sequence(children);
			}
		});
	}

	private void requireSteps() {
		if (steps.isEmpty()) {
			// A script with no steps succeeds instantly and forever, which reads as a bot that does
			// nothing and is far likelier to be a mistake than an intention. Say so here.
			throw new IllegalStateException("script \"" + name + "\" has no steps");
		}
	}

	private BotScript build(Root root) {
		return new Built(name, steps, root);
	}

	/** How a terminal assembles the steps into a root. */
	private abstract static class Root {
		abstract BotState make(BotState[] children);
	}

	/** The built script: a name and the recipe to assemble its root, over and over. */
	private static final class Built implements BotScript {

		private final String name;
		private final List<Supplier<BotState>> steps;
		private final Root root;

		Built(String name, List<Supplier<BotState>> steps, Root root) {
			this.name = name;
			this.steps = new ArrayList<Supplier<BotState>>(steps);
			this.root = root;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public BotState root() {
			BotState[] children = new BotState[steps.size()];
			for (int i = 0; i < children.length; i++) {
				children[i] = steps.get(i).get();
			}
			return root.make(children);
		}

		@Override
		public String toString() {
			return "BotScript(" + name + ")";
		}
	}
}
