package server.game.bots.decorator;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;

/**
 * Fails its child if the child has not finished within a number of ticks
 * ({@code BOT_ROADMAP.md} §5.4).
 *
 * <p><b>Why a generic timeout matters.</b> Slice 1 gives every work leaf its own tick
 * budget, which is the same decision written three times with three different constants —
 * and every new leaf has to invent a fourth. A timeout lets "this is taking too long"
 * become a property of the tree an author wrote rather than of the Java someone wrote, and
 * it turns the one failure mode a bot actually has (a blocked route, a session that never
 * starts) into FAILURE, which a {@link Retry} or a {@link server.game.bots.composite.Selector}
 * can then act on.
 *
 * <p>The count starts on the tick the decorator is entered and advances once per tick it is
 * ticked, so it measures how long the child has been given, not wall-clock time. That is the
 * right unit here: the game runs at a fixed tick rate, and a leaf that only progresses when
 * ticked cannot outlive a counter that only advances when ticked.
 *
 * <p>When the budget runs out the child is exited as <b>interrupted</b>, because it did not
 * report that outcome itself — that is the signal for it to release charges, animations and
 * {@code CycleEvent}s.
 */
@BotNode(id = "timeout", category = "decorator",
		summary = "Fails its child if it has not finished within a number of ticks.")
public final class Timeout implements BotState {

	private final BotState child;
	private final int ticks;

	private int elapsed;
	private boolean active;

	public Timeout(
			@Param(description = "The step to bound.") BotState child,
			@Param(description = "How many ticks the step may take before it fails.") int ticks) {
		this.child = child;
		this.ticks = ticks;
	}

	@Override
	public void enter(BotContext ctx) {
		elapsed = 0;
		active = true;
		child.enter(ctx);
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		BotStatus status = child.tick(ctx);
		if (status != BotStatus.RUNNING) {
			child.exit(ctx, false);
			active = false;
			return status;
		}
		elapsed++;
		if (elapsed >= ticks) {
			child.exit(ctx, true);
			active = false;
			return BotStatus.FAILURE;
		}
		return BotStatus.RUNNING;
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
		return "Timeout(" + ticks + ")";
	}
}
