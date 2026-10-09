package server.game.bots.decorator;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;

/**
 * Runs its child and reports SUCCESS whatever the child reports (apart from still running) —
 * "this is optional" ({@code BOT_ROADMAP.md} §5.4).
 *
 * <p><b>The other half of failure handling.</b> {@link Retry} and {@code Timeout} decide what
 * a failure <em>means</em>; this decides that it does not stop the branch. A step that is
 * nice to have — eat if food is in the bag, equip a better axe if one is there — should not
 * abort the routine when it cannot be done, and without this the author would have to give
 * every such leaf a special "best effort" mode.
 *
 * <p>RUNNING is passed through, because a child mid-run has not decided anything yet and
 * swallowing that would report work as finished before it finished.
 */
@BotNode(id = "succeed", category = "decorator",
		summary = "Runs its child and reports success whatever the child reports.")
public final class Succeed implements BotState {

	private final BotState child;
	private boolean active;

	public Succeed(@Param(description = "The optional step.") BotState child) {
		this.child = child;
	}

	@Override
	public void enter(BotContext ctx) {
		active = true;
		child.enter(ctx);
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		BotStatus status = child.tick(ctx);
		if (status == BotStatus.RUNNING) {
			return BotStatus.RUNNING;
		}
		child.exit(ctx, false);
		active = false;
		return BotStatus.SUCCESS;
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		if (active) {
			child.exit(ctx, true);
			active = false;
		}
	}

	@Override
	public String name() {
		return "Succeed";
	}
}
