package server.game.bots.condition;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;

/**
 * True when the bot is dead — the guard a routine puts at the top so a death is handled
 * rather than worked through ({@code BOT_ROADMAP.md} §5.3).
 *
 * <p><b>Death is not an edge case for a bot; it is a routine occurrence.</b> While dead the
 * client still ticks, but walking, skilling and banking all refuse, so a routine without a
 * death guard spends the respawn window issuing interactions that cannot happen. As a
 * condition it composes the way the other guards do — {@code Fail(IsDead())} at the head of a
 * sequence aborts the routine the moment it becomes true, and a selector can send the bot
 * home to recover.
 *
 * <p>This reads the possessed client directly, like the work states do for their session
 * flag. The honest long-term home is an observation on {@link BotContext}; it moves there
 * when the context stops being player-shaped ({@code BOT_ROADMAP.md} §5.1).
 */
@BotNode(id = "is_dead", category = "condition",
		summary = "Succeeds when the bot is dead.")
public final class IsDead implements BotState {

	public IsDead() {
		// A constant question; no parameters to carry.
	}

	@Override
	public void enter(BotContext ctx) {
		// Stateless.
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		return ctx.client().isDead ? BotStatus.SUCCESS : BotStatus.FAILURE;
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		// Stateless.
	}

	@Override
	public String name() {
		return "IsDead";
	}
}
