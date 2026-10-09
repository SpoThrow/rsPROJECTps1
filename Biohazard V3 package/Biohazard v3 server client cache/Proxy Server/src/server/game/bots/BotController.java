package server.game.bots;

/**
 * Attaches a behaviour tree to a possessed bot and ticks it once per game tick.
 *
 * <p>The controller owns the {@link BotContext}; the bot only knows it has one. This is the
 * "a controller is attached to a real character and can be detached without destroying it"
 * shape from {@code BOT_PLAN.md} §5.4.
 */
public final class BotController {

	private final PlayerBotContext ctx;
	private final BotState root;
	private boolean entered;

	public BotController(BotPlayer bot, BotState root) {
		this.ctx = new PlayerBotContext(bot);
		this.root = root;
	}

	public PlayerBotContext context() {
		return ctx;
	}

	public BotState root() {
		return root;
	}

	/** Enters the root. Called once when the controller is attached. */
	public void enter() {
		if (entered) {
			return;
		}
		ctx.onStateEntered();
		root.enter(ctx);
		entered = true;
	}

	/** One game tick of the tree. */
	public void tick() {
		if (!entered) {
			enter();
		}
		ctx.onTick();
		BotStatus status = root.tick(ctx);
		if (status != BotStatus.RUNNING) {
			// A root that finishes (a bounded script, or a failure): exit cleanly and let the
			// next tick re-enter, so a routine bot restarts itself instead of stopping.
			root.exit(ctx, false);
			entered = false;
		}
	}

	/** Detaches: releases charges, animations and CycleEvents by interrupting the tree. */
	public void stop() {
		if (entered) {
			root.exit(ctx, true);
			entered = false;
		}
	}
}
