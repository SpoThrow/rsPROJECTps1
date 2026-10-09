package server.game.bots.states;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;

/**
 * Walks to a tile and reports SUCCESS once there and idle.
 *
 * <p>Movement is real, by construction: {@link BotContext#walkTo} runs the ordinary
 * {@code PathFinder} route and the player advances one tile per tick, so no state can move
 * a player instantly. This state adds only the two things a route does not: re-issuing when
 * the queue drains short of the destination, and a stuck budget so a blocked destination
 * becomes FAILURE rather than an infinite RUNNING.
 */
@BotNode(id = "walk_to", category = "state",
		summary = "Walks to a tile and succeeds once there and idle.")
public final class WalkTo implements BotState {

	/** Ticks without a net tile of movement before the destination is declared unreachable. */
	private static final int DEFAULT_STUCK_BUDGET = 40;

	private final int destX;
	private final int destY;
	private final int range;
	private final int stuckBudget;

	private int noProgressTicks;
	private int lastX;
	private int lastY;

	public WalkTo(int destX, int destY, int range) {
		this(destX, destY, range, DEFAULT_STUCK_BUDGET);
	}

	public WalkTo(
			@Param(description = "Destination tile x.") int destX,
			@Param(description = "Destination tile y.") int destY,
			@Param(description = "How many tiles away still counts as arrived.") int range,
			@Param(description = "Ticks without net movement before the destination is declared "
					+ "unreachable; the three-argument constructor uses this default.",
					required = false, value = "40") int stuckBudget) {
		this.destX = destX;
		this.destY = destY;
		this.range = range;
		this.stuckBudget = stuckBudget;
	}

	@Override
	public void enter(BotContext ctx) {
		noProgressTicks = 0;
		lastX = ctx.x();
		lastY = ctx.y();
		ctx.walkTo(destX, destY);
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		if (ctx.arrivedAt(destX, destY, range) && ctx.isIdle()) {
			return BotStatus.SUCCESS;
		}

		if (ctx.x() == lastX && ctx.y() == lastY) {
			noProgressTicks++;
		} else {
			noProgressTicks = 0;
			lastX = ctx.x();
			lastY = ctx.y();
		}
		if (noProgressTicks >= stuckBudget) {
			return BotStatus.FAILURE;
		}

		if (ctx.isIdle()) {
			// The queue drained short of the destination (a reroute, or a blocked step that
			// dropped the remainder): ask for a fresh route.
			ctx.walkTo(destX, destY);
		}
		return BotStatus.RUNNING;
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		// Nothing to release: walking holds no charge beyond the queue itself.
	}

	@Override
	public String name() {
		return "WalkTo(" + destX + "," + destY + ")";
	}
}
