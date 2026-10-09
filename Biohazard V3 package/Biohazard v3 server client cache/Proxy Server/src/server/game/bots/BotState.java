package server.game.bots;

/**
 * One node of a bot's behaviour tree.
 *
 * <p>Implementations keep their own progress and report it through {@link #tick}'s return
 * value; they must <b>not</b> choose the next state. Only composites call
 * {@link #enter}/{@link #exit}, which is the whole point of the {@link BotStatus} contract
 * ({@code BOT_PLAN.md} §3.3).
 */
public interface BotState {

	/** Called once, on the game thread, when this state becomes current. */
	void enter(BotContext ctx);

	/** One game tick. Return RUNNING to continue, SUCCESS/FAILURE to report done. */
	BotStatus tick(BotContext ctx);

	/**
	 * Called once when this state stops being current.
	 *
	 * @param interrupted true when left before reporting SUCCESS or FAILURE (death, reset,
	 *                    bot removed, shutdown), so it can release charges, animations and
	 *                    CycleEvents.
	 */
	void exit(BotContext ctx, boolean interrupted);

	/**
	 * A label for this node, used by traces and diagnostics. Deliberately cheap and
	 * overridable, so the tree is self-describing from day one (roadmap seam 1).
	 */
	default String name() {
		return getClass().getSimpleName();
	}
}
