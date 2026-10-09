package server.game.bots.decorator;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;

/**
 * Swaps its child's SUCCESS and FAILURE — the adapter that makes a condition usable as the
 * negative of itself ({@code BOT_ROADMAP.md} §5.4).
 *
 * <p><b>Why not a {@code NotFull} leaf per condition.</b> A tree that needs "there is room"
 * and "there is no room" could grow both, and then both for every condition it gains. One
 * {@code Invert} is unbounded: it turns any leaf into its opposite without that leaf knowing,
 * which is the same reason the conditions themselves are separate leaves rather than methods.
 *
 * <p>RUNNING is passed through unchanged. A condition never reports RUNNING, so in practice
 * this wraps one; when it does wrap real work, "still running" is not an outcome that can be
 * inverted, and pretending otherwise would turn a running step into a failure.
 */
@BotNode(id = "invert", category = "decorator",
		summary = "Reports the opposite of its child; a running child still runs.")
public final class Invert implements BotState {

	private final BotState child;
	private boolean active;

	public Invert(@Param(description = "The step to negate.") BotState child) {
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
		return status == BotStatus.SUCCESS ? BotStatus.FAILURE : BotStatus.SUCCESS;
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
		return "Invert";
	}
}
