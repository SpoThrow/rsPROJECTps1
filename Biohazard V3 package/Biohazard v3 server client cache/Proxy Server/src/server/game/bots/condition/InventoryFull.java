package server.game.bots.condition;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;

/**
 * True when every inventory slot is taken — the gate the chop/bank decision needs
 * ({@code BOT_ROADMAP.md} §5.3).
 *
 * <p>This is the condition side of the "gather until full, then bank" shape the roadmap
 * writes out: {@code Selector(HasFreeSlot-work, BankRoutine)}. It exists as a node rather
 * than as a check inside a gathering leaf because the same question decides several
 * different things — whether to start, whether to keep going, whether to walk to a bank —
 * and a leaf that answers it can be used by all of them without knowing which.
 */
@BotNode(id = "inventory_full", category = "condition",
		summary = "Succeeds when there is no free inventory slot.")
public final class InventoryFull implements BotState {

	public InventoryFull() {
		// A constant question; no parameters to carry.
	}

	@Override
	public void enter(BotContext ctx) {
		// Stateless.
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		return ctx.freeSlots() == 0 ? BotStatus.SUCCESS : BotStatus.FAILURE;
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		// Stateless.
	}

	@Override
	public String name() {
		return "InventoryFull";
	}
}
