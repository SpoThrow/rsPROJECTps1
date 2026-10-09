package server.game.bots.decorator;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;

/**
 * Re-runs its child after a FAILURE, up to a fixed number of tries, and gives up after that
 * ({@code BOT_ROADMAP.md} §5.4).
 *
 * <p><b>This is what a retry looks like in a single-threaded tick model.</b> Slice 1 parked
 * its retry inside the leaves: {@code ChopTree} holds a re-issue budget because a tree that
 * fell must be re-clicked. That is a policy decision living in a leaf, and every new leaf
 * would have to make it again. {@code Retry} moves it out, so {@code Retry(ChopTree(...), 3)}
 * says the same thing once, and a leaf that simply reports its outcome stays usable both
 * with and without retrying.
 *
 * <p>A RUNNING child is passed through untouched; only the outcome that would end the branch
 * is intercepted. Retries happen in the same tick while they fail immediately, so a child
 * which can only fail costs one tick rather than one tick per attempt — bounded by {@code
 * attempts}, so it cannot spin the game thread.
 */
@BotNode(id = "retry", category = "decorator",
		summary = "Re-runs its child after a failure, up to a fixed number of tries.")
public final class Retry implements BotState {

	private static final int DEFAULT_ATTEMPTS = 3;

	private final BotState child;
	private final int attempts;

	/** Tries used so far; the count includes the attempt currently in progress. */
	private int tries;
	private boolean childRunning;
	private boolean active;

	/**
	 * The three-argument form: retry the child the default number of times.
	 *
	 * <p>Deliberately carries no {@code @Param}: the registry treats a constructor whose
	 * parameters are all annotated as the schema, and a node with two of those is rejected
	 * rather than guessed between. The default it supplies is declared on the constructor
	 * below, which is the one the editor reads.
	 */
	public Retry(BotState child) {
		this(child, DEFAULT_ATTEMPTS);
	}

	/**
	 * @param attempts total tries before giving up; {@code 1} runs the child once
	 */
	public Retry(
			@Param(description = "The step to retry.") BotState child,
			@Param(description = "Total number of tries before giving up.",
					required = false, value = "3") int attempts) {
		this.child = child;
		this.attempts = attempts;
	}

	@Override
	public void enter(BotContext ctx) {
		tries = 0;
		childRunning = false;
		active = true;
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		while (true) {
			if (!childRunning) {
				tries++;
				childRunning = true;
				child.enter(ctx);
			}
			BotStatus status = child.tick(ctx);
			if (status == BotStatus.RUNNING) {
				return BotStatus.RUNNING;
			}
			child.exit(ctx, false);
			childRunning = false;
			if (status == BotStatus.SUCCESS) {
				active = false;
				return BotStatus.SUCCESS;
			}
			if (tries >= attempts) {
				active = false;
				return BotStatus.FAILURE;
			}
		}
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		if (active && childRunning) {
			// Abandoned by the parent before it reported an outcome.
			child.exit(ctx, true);
			childRunning = false;
		}
		active = false;
	}

	@Override
	public String name() {
		return "Retry(" + attempts + ")";
	}
}
