package server.game.bots.composite;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;

/**
 * A {@link Selector} that tries its alternatives in a shuffled order, so equally valid
 * choices do not always resolve to the same one.
 *
 * <p><b>Why this exists.</b> Bots that visibly repeat one action look scripted. Two
 * identical spots — two oaks, two fishing spots, two banks — should be chosen between, not
 * always chosen in list order, and the same is true of idle flourishes. {@code
 * RandomSelector} is that variation, and it is the only place a tree introduces
 * non-determinism on purpose.
 *
 * <p>The outcome rules are exactly {@link Selector}'s; the shuffle changes only which
 * alternative is attempted first. It happens once per {@code enter}, so within a single run
 * the order is stable and a RUNNING child keeps its place rather than being re-picked every
 * tick.
 *
 * <p><b>The shuffle uses {@link BotContext#random}.</b> That source is per server rather
 * than per bot, so this is variation, not reproducibility: two bots may differ, and neither
 * can be replayed from a seed. Determinism is the job of the world layer's seeded
 * {@code RandomTileIn}, which is where a replayable choice belongs.
 */
@BotNode(id = "random_selector", category = "composite",
		summary = "Tries its children in a shuffled order and reports the first that succeeds.")
public final class RandomSelector implements BotState {

	private final BotState[] children;
	/** Indices into {@code children}, permuted on each enter. */
	private final int[] order;
	private int position;
	private boolean active;

	public RandomSelector(
			@Param(description = "The alternatives, tried in a shuffled order.")
					BotState... children) {
		this.children = children == null ? new BotState[0] : children.clone();
		this.order = new int[this.children.length];
	}

	@Override
	public void enter(BotContext ctx) {
		for (int i = 0; i < order.length; i++) {
			order[i] = i;
		}
		shuffle(ctx);
		position = 0;
		active = true;
		if (children.length > 0) {
			children[order[0]].enter(ctx);
		}
	}

	/**
	 * Fisher-Yates, descending. The swap partner is {@code 0..i} inclusive because {@code
	 * BotContext.random} is inclusive of its bound ({@code Misc.random}); an exclusive
	 * {@code 0..i-1} would also be a valid shuffle, so this is correct either way.
	 */
	private void shuffle(BotContext ctx) {
		for (int i = order.length - 1; i > 0; i--) {
			int j = ctx.random(i);
			int swap = order[i];
			order[i] = order[j];
			order[j] = swap;
		}
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		while (position < order.length) {
			BotState child = children[order[position]];
			BotStatus status = child.tick(ctx);
			if (status == BotStatus.RUNNING) {
				return BotStatus.RUNNING;
			}
			child.exit(ctx, false);
			if (status == BotStatus.SUCCESS) {
				active = false;
				return BotStatus.SUCCESS;
			}
			position++;
			if (position < order.length) {
				children[order[position]].enter(ctx);
			}
		}
		active = false;
		return BotStatus.FAILURE;
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		if (active && interrupted && position < order.length) {
			children[order[position]].exit(ctx, true);
		}
		active = false;
	}

	@Override
	public String name() {
		return "RandomSelector(" + children.length + ")";
	}
}
