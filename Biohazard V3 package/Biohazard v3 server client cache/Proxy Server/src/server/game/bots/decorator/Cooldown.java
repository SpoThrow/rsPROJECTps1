package server.game.bots.decorator;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.composite.Sequence;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;

/**
 * Runs its child, then holds for a number of ticks before reporting SUCCESS — a pause
 * <em>after</em> the work, which is what a cooldown is for a bot.
 *
 * <p><b>This is {@code Sequence(child, Delay(ticks))} with a name.</b> That composition is
 * exactly the behaviour, so this class builds it and delegates rather than keeping a second
 * copy of the wait loop: two implementations of one behaviour is how the two drift apart.
 * The reason to give it a name anyway is the palette and the trace — an author reaches for
 * "cooldown" and reads {@code Cooldown(5)} in the log, not "Sequence(2)".
 *
 * <p>The interesting failure path is inherited, not re-invented: a child that FAILUREs
 * propagates immediately and skips the hold entirely, so a broken step does not also cost a
 * delay, and a child abandoned mid-run is still exited as interrupted by the sequence.
 */
@BotNode(id = "cooldown", category = "decorator",
		summary = "Runs its child, then holds for a number of ticks before succeeding.")
public final class Cooldown implements BotState {

	private final BotState body;
	private final int ticks;

	public Cooldown(
			@Param(description = "The step to run.") BotState child,
			@Param(description = "Ticks to hold after the child succeeds.") int ticks) {
		this.body = new Sequence(child, new Delay(ticks));
		this.ticks = ticks;
	}

	@Override
	public void enter(BotContext ctx) {
		body.enter(ctx);
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		return body.tick(ctx);
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		body.exit(ctx, interrupted);
	}

	@Override
	public String name() {
		return "Cooldown(" + ticks + ")";
	}
}
