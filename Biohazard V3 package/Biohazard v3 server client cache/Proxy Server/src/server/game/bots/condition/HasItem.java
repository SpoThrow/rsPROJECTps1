package server.game.bots.condition;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;

/**
 * True when the bot is carrying at least one of an item — a condition leaf
 * ({@code BOT_ROADMAP.md} §5.3).
 *
 * <p><b>Conditions are how a tree asks a question without acting.</b> They return on the
 * same tick they are ticked and never report RUNNING, which is what lets a {@link
 * server.game.bots.composite.Selector} walk a run of alternatives in one tick. Keeping them
 * as leaves rather than as flags on the states that care is what makes policy readable:
 * {@code Selector(HasItem(AXE), ...)} says "only chop with an axe" in the tree, where slice 1
 * could only say it inside {@code ChopTree}.
 */
@BotNode(id = "has_item", category = "condition",
		summary = "Succeeds when the bot carries at least one of an item.")
public final class HasItem implements BotState {

	private final int itemId;

	public HasItem(@Param(description = "Item id to look for in the inventory.") int itemId) {
		this.itemId = itemId;
	}

	@Override
	public void enter(BotContext ctx) {
		// Conditions hold no progress between ticks; nothing to set up.
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		return ctx.hasItem(itemId) ? BotStatus.SUCCESS : BotStatus.FAILURE;
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		// A condition that was never entered has nothing to release.
	}

	@Override
	public String name() {
		return "HasItem(" + itemId + ")";
	}
}
