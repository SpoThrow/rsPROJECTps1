package server.game.bots.states;

import java.util.List;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;
import server.game.bots.world.Location;
import server.game.bots.world.LocationKind;
import server.game.bots.world.Locator;
import server.game.bots.world.Locations;
import server.game.bots.world.RandomTileIn;
import server.game.bots.world.Tile;

/**
 * Walks to the nearest place of a kind — {@code walkToNearest(Tree.OAK, 3)} from
 * {@code BOT_ROADMAP.md} §5.5, and the first step of every gathering script.
 *
 * <p><b>This is the state that makes roadmap C pay off.</b> It names a <em>kind</em>, not a tile: the
 * destination is resolved from {@link Locations} when the state is entered, so the same script works
 * wherever the bot is standing and a new resource area is a row in {@code locations.cfg} rather than an
 * edit here. Slice 1 could only say {@code WalkTo(3192, 3223)}.
 *
 * <p><b>Resolved on enter, not at construction.</b> A bot that banked and came back is somewhere else
 * than it was when the script was built, and "nearest" means nearest <em>now</em>. It also means a
 * script needs no world to be constructed, which is why {@code BotScript.root()} takes no context.
 *
 * <p><b>Boxes spread, points do not.</b> A curated place is a box an author dragged, so the destination
 * is a random walkable tile inside it ({@link RandomTileIn}) — several bots sent to
 * {@code draynor_oaks} stand in different squares instead of queueing on one. A scanned place is
 * already a single object's tile, so there is nothing to spread.
 *
 * <p><b>The seed is per bot.</b> By default it is derived from the bot's name, so two bots sharing a
 * waypoint choose different tiles while each bot resolves the same waypoint the same way every time —
 * reproducible from a trace, which is the property {@code BOT_ROADMAP.md} §5.2 asks for. An explicit
 * {@code seed} overrides it, for a test or a pinned run.
 *
 * <p>The actual walking is delegated to {@link WalkTo}, so re-issuing the route when the queue drains
 * and declaring a blocked destination FAILURE are implemented once, in the state that already owns them.
 */
@BotNode(id = "walk_to_nearest", category = "state",
		summary = "Walks to the nearest place of a kind, spreading to a walkable tile inside its box.")
public final class WalkToNearest implements BotState {

	/** Resolve the spread tile from the bot's identity rather than a fixed number. */
	public static final int SEED_FROM_BOT = -1;

	private final Locator<Location> places;
	private final LocationKind kind;
	private final int range;
	private final int seed;

	private Location place;
	private BotState walk;

	public WalkToNearest(
			@Param(description = "Kind of place to walk to, e.g. tree or bank.") LocationKind kind,
			@Param(description = "How many tiles away still counts as arrived.") int range,
			@Param(description = "Spread seed; -1 derives it from the bot so two bots do not share a "
					+ "tile.", required = false, value = "-1") int seed) {
		this(null, kind, range, seed);
	}

	/**
	 * The injectable form: a given locator instead of the live world. Used by tests, which have no
	 * {@code Data/cfg} to load and no regions to scan, and deliberately not annotated, so it cannot be
	 * mistaken for the node's schema.
	 *
	 * <p>A null {@code places} is the live table, resolved on first use rather than here. That is what
	 * keeps constructing a script free of file IO — registering a script must not read {@code Data/cfg},
	 * or merely referring to the registry would load the world.
	 */
	public WalkToNearest(Locator<Location> places, LocationKind kind, int range, int seed) {
		this.places = places;
		this.kind = kind;
		this.range = range;
		this.seed = seed;
	}

	private Locator<Location> places() {
		return places != null ? places : Locations.live().forKind(kind);
	}

	/** The place this state resolved to, or null before it has. For traces and tests. */
	public Location place() {
		return place;
	}

	@Override
	public void enter(BotContext ctx) {
		place = null;
		walk = null;
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		if (walk == null) {
			if (place == null) {
				List<Location> nearest = places().nearest(ctx.x(), ctx.y(), ctx.height(), 1);
				if (nearest.isEmpty()) {
					// Nothing of this kind on this plane. Not recoverable by retrying, so fail: a
					// Selector above can then route the bot somewhere else. The note is what makes the
					// failure legible in the console (roadmap Phase F).
					ctx.trace().note("no " + kind.id() + " place on plane " + ctx.height());
					return BotStatus.FAILURE;
				}
				place = nearest.get(0);
			}
			Tile destination = destination(ctx);
			walk = new WalkTo(destination.x(), destination.y(), range);
			walk.enter(ctx);
		}
		BotStatus status = walk.tick(ctx);
		if (status != BotStatus.RUNNING) {
			walk.exit(ctx, false);
			walk = null;
			if (status == BotStatus.FAILURE) {
				// The inner walk gave up (stuck, or the route failed). Name the place so the log says
				// which destination was unreachable rather than only that something failed.
				ctx.trace().note("could not reach " + place.name() + " in " + place.plane());
			}
		}
		return status;
	}

	/** The concrete tile inside the resolved place: the box spread, or the point itself. */
	private Tile destination(BotContext ctx) {
		if (place.isPoint()) {
			return Tile.of(place.x(), place.y(), place.plane());
		}
		return new RandomTileIn(place, RandomTileIn.liveWalkable()).resolve(seedFor(ctx));
	}

	private int seedFor(BotContext ctx) {
		if (seed != SEED_FROM_BOT) {
			return seed;
		}
		String name = ctx.agent().name();
		return name == null ? 0 : name.hashCode();
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		if (walk != null) {
			walk.exit(ctx, interrupted);
			walk = null;
		}
	}

	@Override
	public String name() {
		return "WalkToNearest(" + kind.id() + ")";
	}
}
