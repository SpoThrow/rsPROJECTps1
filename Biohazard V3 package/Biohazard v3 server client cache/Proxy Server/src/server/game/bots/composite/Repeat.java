package server.game.bots.composite;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;

/**
 * Re-runs a child. A child's SUCCESS restarts it; FAILURE propagates and ends the repeat.
 *
 * <p><b>Finite counts are exhausted in the tick that finishes them</b>, so
 * {@code Repeat(child, 3)} returns SUCCESS on the tick that completes the third run.
 * <b>A forever repeat ({@code count < 0}) yields to the next tick after each completed run</b>
 * — that is what stops a child which succeeds instantly from spinning the game thread.
 * A forever repeat is therefore the slice-1 root and simply keeps ticking.
 */
@BotNode(id = "repeat", category = "composite",
		summary = "Runs its child a fixed number of times, or forever.")
public final class Repeat implements BotState {

	private final BotState child;
	private final int count;
	private int completed;

	/**
	 * @param count how many times to run the child; a negative value means "forever"
	 */
	public Repeat(
			@Param(description = "The step to repeat.") BotState child,
			@Param(description = "How many times to run it; negative means forever.",
					required = false, value = "-1") int count) {
		this.child = child;
		this.count = count;
	}

	@Override
	public void enter(BotContext ctx) {
		completed = 0;
		if (count != 0) {
			child.enter(ctx);
		}
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		if (count == 0) {
			return BotStatus.SUCCESS;
		}
		while (true) {
			BotStatus status = child.tick(ctx);
			if (status == BotStatus.RUNNING) {
				return BotStatus.RUNNING;
			}
			child.exit(ctx, false);
			if (status == BotStatus.FAILURE) {
				return BotStatus.FAILURE;
			}
			completed++;
			if (count >= 0 && completed >= count) {
				return BotStatus.SUCCESS;
			}
			child.enter(ctx);
			if (count < 0) {
				// Forever: one completed run per tick. Looping here would let a child that
				// succeeds immediately (e.g. a zero-tick wait) starve the game thread.
				return BotStatus.RUNNING;
			}
		}
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		if (interrupted) {
			child.exit(ctx, true);
		}
	}

	@Override
	public String name() {
		return "Repeat(" + (count < 0 ? "forever" : Integer.toString(count)) + ")";
	}
}
