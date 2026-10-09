package server.game.bots.condition;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;

/**
 * True when the bot is within a number of tiles of a point — the condition form of
 * {@code WalkTo}'s arrival test ({@code BOT_ROADMAP.md} §5.3).
 *
 * <p><b>Why this is a leaf and not just part of {@code WalkTo}.</b> Deciding "am I close
 * enough?" is independent of moving: a tree may want to interact from here, pick a different
 * spot, or skip the walk entirely because a previous step already left the bot in range. As
 * a condition it composes with everything; as a line inside {@code WalkTo} it composes with
 * nothing.
 *
 * <p>Range is measured with {@code arrivedAt}, the same distance test the walking state and
 * the interaction dispatch use, so "in range" means one thing in this codebase rather than
 * three.
 */
@BotNode(id = "within_range", category = "condition",
		summary = "Succeeds when the bot is within a number of tiles of a point.")
public final class WithinRange implements BotState {

	private final int x;
	private final int y;
	private final int range;

	public WithinRange(
			@Param(description = "x of the point to measure from.") int x,
			@Param(description = "y of the point to measure from.") int y,
			@Param(description = "How many tiles away still counts as in range.") int range) {
		this.x = x;
		this.y = y;
		this.range = range;
	}

	@Override
	public void enter(BotContext ctx) {
		// Stateless.
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		return ctx.arrivedAt(x, y, range) ? BotStatus.SUCCESS : BotStatus.FAILURE;
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		// Stateless.
	}

	@Override
	public String name() {
		return "WithinRange(" + x + "," + y + "+" + range + ")";
	}
}
