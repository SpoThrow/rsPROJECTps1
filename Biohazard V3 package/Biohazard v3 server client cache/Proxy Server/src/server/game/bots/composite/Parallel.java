package server.game.bots.composite;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;

/**
 * Ticks every child on every tick, and succeeds once all of them have succeeded — the
 * "juggle concerns" composite ({@code BOT_ROADMAP.md} §5.4).
 *
 * <p><b>What "parallel" means without threads.</b> There is one game tick and one bot; this
 * does not run children concurrently, it lets several children make progress in the <em>same
 * </em> tick. That is exactly what an "eat between chops" or "keep a cannon topped up" leaf
 * needs, and it is the one composite whose children are all entered at once.
 *
 * <p>Failure is immediate: as soon as one child reports FAILURE the parallel fails, and any
 * child still mid-run is interrupted, because a branch that has abandoned its siblings must
 * tell them so ({@code exit(ctx, true)}) — the same rule {@link Sequence} applies to its
 * current child, here applied to all of them. Success requires every child, so wrapping a
 * child in {@code Repeat(...)} is how an author says "keep doing this alongside the rest".
 *
 * <p>Two consequences worth knowing: a parallel with no children succeeds vacuously, and the
 * cost per tick is the sum of its children, so a parallel is the easiest node to make
 * expensive. Keep the hot branch outside it.
 */
@BotNode(id = "parallel", category = "composite",
		summary = "Ticks all its children every tick and succeeds when all of them have.")
public final class Parallel implements BotState {

	private final BotState[] children;
	/** True while the child at that index is inside enter/exit. */
	private final boolean[] pending;

	public Parallel(
			@Param(description = "The children, all ticked on every tick.") BotState... children) {
		this.children = children == null ? new BotState[0] : children.clone();
		this.pending = new boolean[this.children.length];
	}

	@Override
	public void enter(BotContext ctx) {
		for (int i = 0; i < children.length; i++) {
			pending[i] = true;
			children[i].enter(ctx);
		}
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		int remaining = 0;
		for (int i = 0; i < children.length; i++) {
			if (!pending[i]) {
				continue;
			}
			BotStatus status = children[i].tick(ctx);
			if (status == BotStatus.RUNNING) {
				remaining++;
				continue;
			}
			pending[i] = false;
			children[i].exit(ctx, false);
			if (status == BotStatus.FAILURE) {
				interruptTheRest(ctx);
				return BotStatus.FAILURE;
			}
		}
		if (remaining > 0) {
			return BotStatus.RUNNING;
		}
		return BotStatus.SUCCESS;
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		// A parallel that reported SUCCESS has already exited every child; one that was
		// abandoned by its parent is interrupted here. Either way, nothing may stay pending.
		interruptTheRest(ctx);
	}

	private void interruptTheRest(BotContext ctx) {
		for (int i = 0; i < children.length; i++) {
			if (pending[i]) {
				pending[i] = false;
				// It never reported an outcome of its own, so it is being interrupted.
				children[i].exit(ctx, true);
			}
		}
	}

	@Override
	public String name() {
		return "Parallel(" + children.length + ")";
	}
}
