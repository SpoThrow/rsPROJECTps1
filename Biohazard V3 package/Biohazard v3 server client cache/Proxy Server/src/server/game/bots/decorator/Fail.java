package server.game.bots.decorator;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;

/**
 * Runs its child and reports FAILURE whatever the child reports (apart from still running) —
 * the mirror of {@link Succeed}.
 *
 * <p><b>Why the negative is worth its own node.</b> A tree sometimes needs a branch whose
 * whole job is to <em>not</em> be taken by default, or a check whose success must stop the
 * enclosing {@link server.game.bots.composite.Sequence}. Writing it as {@code Fail(x)} is
 * clearer than {@code Invert(Succeed(x))}, and it is the honest way to express "this step
 * working is itself the failure signal" — for example a death guard, where the child
 * succeeding (the bot is dead) must abort the routine.
 *
 * <p>Its use is deliberately narrow, and the palette groups it with the other adapters: a
 * tree that fails on purpose is a smell unless the comment says why.
 */
@BotNode(id = "fail", category = "decorator",
		summary = "Runs its child and reports failure whatever the child reports.")
public final class Fail implements BotState {

	private final BotState child;
	private boolean active;

	public Fail(@Param(description = "The step whose outcome becomes a failure.") BotState child) {
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
		return BotStatus.FAILURE;
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
		return "Fail";
	}
}
