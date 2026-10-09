package server.game.bots.states;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;

/**
 * Opens the bank and deposits every inventory slot holding {@code logItemId}.
 *
 * <p>Returns SUCCESS once none of the item remain in the inventory, FAILURE if the bank
 * never opens or the deposit cannot complete within the budget.
 */
public final class BankLogs implements BotState {

	private static final int BUDGET = 20;

	private final int logItemId;
	private int ticks;

	public BankLogs(int logItemId) {
		this.logItemId = logItemId;
	}

	@Override
	public void enter(BotContext ctx) {
		ticks = 0;
		ctx.openBank();
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		ticks++;

		if (!ctx.client().isBanking) {
			if (ticks >= BUDGET) {
				return BotStatus.FAILURE;
			}
			ctx.openBank();
			return BotStatus.RUNNING;
		}

		ctx.depositItem(logItemId);
		if (!ctx.hasItem(logItemId)) {
			return BotStatus.SUCCESS;
		}
		return ticks >= BUDGET ? BotStatus.FAILURE : BotStatus.RUNNING;
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		// The bank interface is closed by the next walkTo, mirroring the walking packet.
	}

	@Override
	public String name() {
		return "BankLogs(" + logItemId + ")";
	}
}
