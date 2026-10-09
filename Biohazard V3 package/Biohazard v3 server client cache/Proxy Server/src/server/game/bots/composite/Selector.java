package server.game.bots.composite;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;

/**
 * Tries children in order and reports the first one that succeeds — the "else if" of a
 * behaviour tree ({@code BOT_ROADMAP.md} §5.3).
 *
 * <p><b>Selector is what turns hand-wired policy into readable policy.</b> {@code Sequence}
 * alone can only say "do this, then that"; policy needs "chop if there is room, otherwise
 * bank", which is a choice between alternatives. Swapping the two is the intended shape:
 * the common case is listed first, and its condition failing is how the fallback runs.
 *
 * <p>Outcomes follow the same contract as {@link Sequence}: a RUNNING child stays current
 * and nothing after it is entered, a SUCCESS ends the selector immediately, and a FAILURE
 * only advances to the next alternative. Only when every alternative fails does the
 * selector itself fail.
 *
 * <p>Succeeding and failing children are advanced within the same tick, so a run of
 * instantly-deciding condition leaves costs one tick rather than one tick each. The work is
 * bounded by the number of children, so this cannot spin the game thread. An empty selector
 * has no alternative that can succeed and therefore fails.
 */
@BotNode(id = "selector", category = "composite",
		summary = "Tries its children in order and reports the first that succeeds.")
public final class Selector implements BotState {

	private final BotState[] children;
	private int index;
	private boolean active;

	public Selector(
			@Param(description = "The alternatives, tried in this order.") BotState... children) {
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
		while (index < children.length) {
			BotStatus status = children[index].tick(ctx);
			if (status == BotStatus.RUNNING) {
				return BotStatus.RUNNING;
			}
			// The child reported its own outcome, so this is not an interruption.
			children[index].exit(ctx, false);
			if (status == BotStatus.SUCCESS) {
				active = false;
				return BotStatus.SUCCESS;
			}
			index++;
			if (index < children.length) {
				children[index].enter(ctx);
			}
		}
		// Every alternative failed, or there were none.
		active = false;
		return BotStatus.FAILURE;
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		// Only the currently-running child is inside enter/exit; the alternatives that already
		// failed were exited as they were passed over.
		if (active && interrupted && index < children.length) {
			children[index].exit(ctx, true);
		}
		active = false;
	}

	@Override
	public String name() {
		return "Selector(" + children.length + ")";
	}
}
