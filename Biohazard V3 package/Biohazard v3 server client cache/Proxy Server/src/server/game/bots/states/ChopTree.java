package server.game.bots.states;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;
import server.game.players.actions.objects.ObjectClick;

/**
 * Chops a tree until the inventory is full.
 *
 * <p><b>The registry, not the skill.</b> {@code enter} issues a first-click interaction on
 * the tree; the {@code ObjectHandler} registry then invokes
 * {@code Woodcutting.startWoodcutting} exactly as a real click would. This class never
 * names a skill class, so a newly migrated skill becomes bot-usable with no change here.
 *
 * <p>Chopping runs on its own {@code CycleEvent}, so this state only watches: it returns
 * SUCCESS when the bag fills, FAILURE if a chop session never starts (no axe, too low a
 * level), and otherwise re-issues the interaction, which is how a tree that fell or a
 * random event is recovered from rather than treated as terminal.
 */
@BotNode(id = "chop_tree", category = "state",
		summary = "Clicks a tree and chops until the inventory is full.")
public final class ChopTree implements BotState {

	/** The distance the tree click packet uses (see {@code ClickObject.FIRST_CLICK}). */
	private static final int CHOP_RANGE = 3;

	/** Ticks to wait for the session to start before declaring it unstartable. */
	private static final int START_BUDGET = 3;

	/** Ticks of re-issuing without the bag filling before giving up. */
	private static final int REISSUE_BUDGET = 120;

	private final int treeId;
	private final int logItemId;
	private final int treeX;
	private final int treeY;

	private int ticks;
	private boolean started;

	public ChopTree(
			@Param(description = "Object id of the tree to click.") int treeId,
			@Param(description = "Item id of the log it yields, used to watch the inventory.")
					int logItemId,
			@Param(description = "x of the tree's tile.") int treeX,
			@Param(description = "y of the tree's tile.") int treeY) {
		this.treeId = treeId;
		this.logItemId = logItemId;
		this.treeX = treeX;
		this.treeY = treeY;
	}

	@Override
	public void enter(BotContext ctx) {
		ticks = 0;
		started = false;
		issue(ctx);
	}

	private void issue(BotContext ctx) {
		ctx.interactObject(treeId, treeX, treeY, ObjectClick.FIRST, CHOP_RANGE);
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		ticks++;
		if (ctx.freeSlots() == 0) {
			return BotStatus.SUCCESS;
		}
		if (isChopping(ctx)) {
			started = true;
			return BotStatus.RUNNING;
		}
		if (!started) {
			// The session never began: no axe, or the level is too low. Retrying forever would
			// spin, so this is a genuine failure.
			return ticks >= START_BUDGET ? BotStatus.FAILURE : BotStatus.RUNNING;
		}
		// A session ran and ended with slots still free (the tree fell, or a random event).
		// Re-issue while there is room; give up only if that never restarts it.
		if (ticks >= REISSUE_BUDGET) {
			return BotStatus.FAILURE;
		}
		issue(ctx);
		return BotStatus.RUNNING;
	}

	/** Reads whether a skill session is running; this is observation, not a skill call. */
	private boolean isChopping(BotContext ctx) {
		return ctx.isSkilling();
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		if (interrupted) {
			// Leave the session to the normal skill cleanup; nothing is held here.
		}
	}

	@Override
	public String name() {
		return "ChopTree(" + treeId + ")";
	}
}
