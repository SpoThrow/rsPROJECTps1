package server.game.bots.decorator;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;

/**
 * Does nothing for a number of ticks and then succeeds — a leaf, not a wrapper
 * ({@code BOT_ROADMAP.md} §5.4).
 *
 * <p><b>Waiting is a real behaviour for a bot.</b> A bot that acts on the exact tick it is
 * able to looks mechanical, and some interactions genuinely need a pause: re-issuing a click
 * too fast, or reopening a bank on the tick it closed, is how a loop reads as a script.
 * {@code Delay} is the vocabulary for that pause, so a tree can say "wait three ticks" where
 * slice 1 would have needed a {@code Wait} leaf with its own counter.
 *
 * <p>The count advances once per tick this state is ticked, and it reports SUCCESS on the
 * tick that completes the wait. It has no child and holds nothing, so it cannot leak on exit,
 * and {@code Delay(0)} succeeds on its first tick rather than being special-cased away.
 */
@BotNode(id = "delay", category = "decorator",
		summary = "Does nothing for a number of ticks and then succeeds.")
public final class Delay implements BotState {

	private final int ticks;
	private int elapsed;

	public Delay(@Param(description = "How many ticks to wait before succeeding.") int ticks) {
		this.ticks = ticks;
	}

	@Override
	public void enter(BotContext ctx) {
		elapsed = 0;
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		elapsed++;
		return elapsed >= ticks ? BotStatus.SUCCESS : BotStatus.RUNNING;
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		// Nothing to release: a delay holds no charge, animation or event.
	}

	@Override
	public String name() {
		return "Delay(" + ticks + ")";
	}
}
