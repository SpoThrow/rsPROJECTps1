package server.game.bots.states;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;
import server.game.bots.world.LocationKind;
import server.game.bots.world.ObjectTarget;
import server.game.bots.world.ResourceScan;
import server.game.players.actions.objects.ObjectClick;

/**
 * The generic gather leaf: finds the nearest object of a kind, walks to it, clicks it, and keeps going
 * until the inventory is full — {@code BOT_ROADMAP.md} §5.5's {@code gather(Tree.OAK, Item.LOGS)
 * .untilFull()}, without a state class per resource.
 *
 * <p><b>What makes it generic.</b> Every part that differed between chopping, mining and fishing is
 * either derived or parameterised:
 *
 * <ul>
 * <li><b>Which object</b> — resolved by {@link ResourceScan} from a {@link LocationKind}, so a new
 *     resource is a rule in {@link server.game.bots.world.ResourceKinds} and never a new leaf.
 * <li><b>How to click it</b> — the {@code click} parameter (0, 1, 2 for first, second, third), because
 *     a fishing spot's {@code Net} and {@code Bait} are different clicks on one object while a tree is
 *     always the first. Trees and rocks use the default.
 * <li><b>Whether it is working</b> — inferred from the free slot count rather than from a skill's
 *     session flag. {@code ctx.isIdle()} and {@code ChopTree}'s {@code woodcutting.active} are
 *     woodcutting-shaped by name, so a mining leaf reading them would be lying; a slot that fills is
 *     evidence of progress that is true for every gathering skill. The cost is that a
 *     <em>stackable</em> yield (coins, feathers) would show no progress — see the note below.
 * </ul>
 *
	 * <p><b>Bounded, and honest about failure.</b> It gives up on a target that yields nothing for {@code
	 * STALL_TICKS} — a tree that fell, a rock that depleted, an object that was removed — and looks for
	 * another. The budget is {@code MAX_TARGETS} distinct objects <em>since the last slot filled</em>,
	 * so a routine retarget after a tree falls does not count towards it; only a run of objects that all
	 * yielded nothing does, which is what "there is no resource here" looks like. FAILURE is the right
	 * answer rather than looping forever: the controller restarts a finished root on the next tick, and
	 * a {@link server.game.bots.composite.Selector} above can route the bot somewhere else.
 *
 * <p><b>Progress is measured in slots filled, so a stackable yield does not register.</b> A gathering
 * action whose drops stack in one slot never changes the slot count, and this leaf would eventually
 * declare it stuck. Every gathering skill in this cache yields a slot-filling item (logs, ore, fish),
 * so this is a real limitation rather than a live bug — but it is the reason the check is documented
 * here instead of assumed, and the first thing to fix if a stackable resource is ever needed.
 */
@BotNode(id = "gather", category = "state",
		summary = "Finds the nearest object of a kind, walks to it and clicks until the inventory is full.")
public final class Gather implements BotState {

	/** The click distance the object actions expect. Trees use 3 (see {@code ChopTree}). */
	public static final int DEFAULT_RANGE = 3;

	/** Ticks with no slot filled before the current object is abandoned for another. */
	public static final int STALL_TICKS = 40;

	/** Ticks between re-clicks while stalled: long enough not to spam, short enough to restart a session. */
	public static final int REISSUE_EVERY = 10;

	/** Distinct objects to try before reporting FAILURE, so "no resource here" terminates. */
	public static final int MAX_TARGETS = 8;

	/** First click: the default for trees and rocks. */
	public static final int FIRST_CLICK = 0;

	private final ResourceScan injected;
	private final LocationKind kind;
	private final int itemId;
	private final int range;
	private final int radius;
	private final ObjectClick click;

	/** The live scan, created on first use so constructing a Gather reads no world. */
	private ResourceScan liveScan;

	private ObjectTarget target;
	private BotState approach;
	private int targets;
	private int stall;
	private int lastFree;

	public Gather(
			@Param(description = "Kind of object to gather, e.g. tree or rock.") LocationKind kind,
			@Param(description = "Item id the action yields, watched to measure progress.") int itemId,
			@Param(description = "How many tiles away the object can still be clicked from.",
					required = false, value = "3") int range,
			@Param(description = "Tiles to search for an object, around the bot.",
					required = false, value = "8") int radius,
			@Param(description = "0, 1 or 2 for the first, second or third object click.",
					required = false, value = "0") int click) {
		this(null, kind, itemId, range, radius, click);
	}

	/**
	 * The injectable form: a given scan instead of the live world. Used by tests, which have no regions
	 * to search, and deliberately not annotated, so it cannot be mistaken for the node's schema.
	 *
	 * <p>A null {@code scan} is the live world, resolved on first use, so constructing a gather leaf is
	 * free of world work.
	 */
	public Gather(ResourceScan scan, LocationKind kind, int itemId, int range, int radius, int click) {
		this.injected = scan;
		this.kind = kind;
		this.itemId = itemId;
		this.range = range;
		this.radius = radius;
		this.click = clickOf(click);
	}

	private ResourceScan scan() {
		if (injected != null) {
			return injected;
		}
		if (liveScan == null) {
			liveScan = ResourceScan.live();
		}
		return liveScan;
	}

	/** The object this state is currently working on, or null. For traces and tests. */
	public ObjectTarget target() {
		return target;
	}

	@Override
	public void enter(BotContext ctx) {
		target = null;
		approach = null;
		targets = 0;
		stall = 0;
		lastFree = ctx.freeSlots();
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		if (ctx.freeSlots() == 0) {
			return BotStatus.SUCCESS;
		}
		if (target == null && !resolve(ctx)) {
			return BotStatus.FAILURE;
		}
		if (approach != null) {
			BotStatus status = approach.tick(ctx);
			if (status == BotStatus.RUNNING) {
				return BotStatus.RUNNING;
			}
			approach.exit(ctx, false);
			approach = null;
			if (status == BotStatus.FAILURE) {
				// Unreachable: abandon it and look for another rather than walking at a wall forever.
				target = null;
				stall = 0;
				lastFree = ctx.freeSlots();
				return targets >= MAX_TARGETS ? BotStatus.FAILURE : BotStatus.RUNNING;
			}
			// Arrived: click once now, then watch. The first click must not wait for the stall timer.
			stall = 0;
			lastFree = ctx.freeSlots();
			click(ctx);
			return BotStatus.RUNNING;
		}
		int free = ctx.freeSlots();
		if (free < lastFree) {
			// A slot filled, so whatever it is doing is working. The give-up budget resets too: a
			// retarget after a tree falls is normal, and counting it towards "this resource is not
			// here" would abandon a perfectly good tree field after a few logs.
			stall = 0;
			targets = 0;
		} else {
			stall++;
		}
		lastFree = free;
		if (stall >= STALL_TICKS) {
			// Nothing gained for long enough: the object is gone. Look for another.
			target = null;
			stall = 0;
			return targets >= MAX_TARGETS ? BotStatus.FAILURE : BotStatus.RUNNING;
		}
		if (stall > 0 && stall % REISSUE_EVERY == 0) {
			click(ctx);
		}
		return BotStatus.RUNNING;
	}

	/** Looks for an object of this kind and builds the walk to it. False when there is nothing near. */
	private boolean resolve(BotContext ctx) {
		ObjectTarget found = scan().nearest(ctx.x(), ctx.y(), ctx.height(), kind, radius);
		if (found == null) {
			return false;
		}
		target = found;
		targets++;
		approach = new WalkTo(found.x(), found.y(), range);
		approach.enter(ctx);
		return true;
	}

	private void click(BotContext ctx) {
		ctx.interactObject(target.objectId(), target.x(), target.y(), click, range);
	}

	private static ObjectClick clickOf(int index) {
		ObjectClick[] clicks = ObjectClick.values();
		if (index < 0 || index >= clicks.length) {
			throw new IllegalArgumentException("click index " + index + " is not a click; expected 0.."
					+ (clicks.length - 1));
		}
		return clicks[index];
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		if (approach != null) {
			approach.exit(ctx, interrupted);
			approach = null;
		}
	}

	@Override
	public String name() {
		return "Gather(" + kind.id() + ")";
	}
}
