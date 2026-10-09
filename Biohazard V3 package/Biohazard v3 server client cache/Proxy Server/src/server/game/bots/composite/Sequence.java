package server.game.bots.composite;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;

/**
 * Runs children in order. A child's SUCCESS advances to the next child; a child's FAILURE
 * aborts the whole sequence, and a child's RUNNING keeps that child current across ticks.
 *
 * <p>Succeeding children are advanced within the same tick, so a sequence of
 * instantly-succeeding children does not cost one tick each. The work is bounded by the
 * number of children, so this cannot spin the game thread.
 */
public final class Sequence implements BotState {

	private final BotState[] children;
	private int index;
	private boolean active;

	public Sequence(BotState... children) {
		this.children = children == null ? new BotState[0] : children.clone();
	}

	@Override
	public void enter(BotContext ctx) {
		index = 0;
		active = true;
		if (children.length > 0) {
			children[0].enter(ctx);
		}
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		if (children.length == 0) {
			return BotStatus.SUCCESS;
		}
		while (true) {
			BotStatus status = children[index].tick(ctx);
			if (status == BotStatus.RUNNING) {
				return BotStatus.RUNNING;
			}
			// The child reported its own outcome, so this is not an interruption.
			children[index].exit(ctx, false);
			if (status == BotStatus.FAILURE) {
				return BotStatus.FAILURE;
			}
			index++;
			if (index >= children.length) {
				return BotStatus.SUCCESS;
			}
			children[index].enter(ctx);
		}
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		// Only the currently-running child is inside enter/exit; children already advanced
		// past were exited when they reported SUCCESS/FAILURE.
		if (active && interrupted && index < children.length) {
			children[index].exit(ctx, true);
		}
		active = false;
	}

	@Override
	public String name() {
		return "Sequence(" + children.length + ")";
	}
}
