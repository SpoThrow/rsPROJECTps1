package server.game.bots.condition;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;

/**
 * True while the bank interface is open — the condition a deposit routine checks before it
 * tries to deposit ({@code BOT_ROADMAP.md} §5.3).
 *
 * <p><b>The reason this cannot be inlined into the depositing leaves.</b> {@code openUpBank}
 * calls {@code resetVariables()}, which clears the client's transient click fields, and the
 * bank frame also closes on walking. A routine that assumed "I opened it, so it is open"
 * would be wrong on exactly those paths; asking the client each tick is the only answer that
 * survives them. It is also the shape that lets a routine be re-entered safely: the
 * condition is true or false on its own merits, regardless of how the tree arrived here.
 *
 * <p>This reads the possessed client directly, like the work states do for their session
 * flag. The honest long-term home is an observation on {@link BotContext}; it moves there
 * when the context stops being player-shaped ({@code BOT_ROADMAP.md} §5.1).
 */
@BotNode(id = "bank_open", category = "condition",
		summary = "Succeeds while the bank interface is open.")
public final class BankOpen implements BotState {

	public BankOpen() {
		// A constant question; no parameters to carry.
	}

	@Override
	public void enter(BotContext ctx) {
		// Stateless.
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		return ctx.client().isBanking ? BotStatus.SUCCESS : BotStatus.FAILURE;
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		// Stateless.
	}

	@Override
	public String name() {
		return "BankOpen";
	}
}
