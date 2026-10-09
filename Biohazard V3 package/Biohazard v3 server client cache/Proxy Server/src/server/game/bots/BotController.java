package server.game.bots;

import core.util.Misc;

/**
 * Attaches a behaviour tree to a possessed bot and ticks it once per game tick.
 *
 * <p>The controller owns the {@link BotContext}; the bot only knows it has one. This is the
 * "a controller is attached to a real character and can be detached without destroying it"
 * shape from {@code BOT_PLAN.md} §5.4.
 *
 * <p><b>It is also the only thing that sees the root's outcome.</b> A tree reports its result to
 * whoever ticked it, and the controller is that caller for the root — so this is where a finished or
 * failed routine is noticed, and where the one-line failure log (roadmap Phase F) belongs.
 */
public final class BotController {

	private final PlayerBotContext ctx;
	private final BotState root;
	private boolean entered;

	/** The signature of the last failure printed, and how many identical ones were swallowed since. */
	private String lastLoggedSignature;
	private int suppressedFailures;
	/** The last line actually printed, kept so a test can assert on it without capturing stdout. */
	private String lastReportedFailure;

	public BotController(BotPlayer bot, BotState root) {
		this.ctx = new PlayerBotContext(bot);
		this.root = root;
	}

	public PlayerBotContext context() {
		return ctx;
	}

	/** This bot's transition history. What {@code ::botinfo} reads. */
	public BotTrace trace() {
		return ctx.trace();
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
		// onTick advances the trace's clock, so every event this tick is stamped with this tick.
		ctx.onTick();
		BotStatus status = root.tick(ctx);
		if (status != BotStatus.RUNNING) {
			// A root that finishes (a bounded script, or a failure): exit cleanly and let the
			// next tick re-enter, so a routine bot restarts itself instead of stopping.
			root.exit(ctx, false);
			entered = false;
			if (status == BotStatus.FAILURE) {
				reportFailure();
			}
		}
	}

	/**
	 * Prints the one-line failure the roadmap asks for — {@code [bot] willow: Gather(tree) -> FAILURE
	 * (after 41t, no tree within 8 tiles)} — and returns the line, or null when it was suppressed.
	 *
	 * <p><b>Throttled by signature, not by a timer.</b> The controller restarts a finished root on the
	 * very next tick, so a bot that can never succeed would otherwise print this line every 600 ms and
	 * drown the console it exists to inform. Printing on each <em>change</em> means a new problem is
	 * always visible immediately, a repeating one is quiet, and the count of what was swallowed is
	 * reported when at last something different happens — so nothing is hidden, only summarised.
	 *
	 * <p>Private because {@link #tick()} calls it: reporting is not a thing a caller decides to do, it is
	 * what a failing bot does. A test observes it through {@link #lastReportedFailure()} and
	 * {@link #suppressedFailures()} rather than by capturing stdout.
	 */
	private void reportFailure() {
		String signature = failureSignature();
		if (signature.equals(lastLoggedSignature)) {
			suppressedFailures++;
			return;
		}
		StringBuilder line = new StringBuilder("[bot] ").append(name()).append(": ").append(failureText());
		if (suppressedFailures > 0) {
			line.append(" [previous failure repeated ").append(suppressedFailures).append("x]");
		}
		lastReportedFailure = line.toString();
		lastLoggedSignature = signature;
		suppressedFailures = 0;
		Misc.println(lastReportedFailure);
	}

	/** The display name of the actor this controller drives, or a placeholder before one is set. */
	private String name() {
		String name = ctx.agent().name();
		return name == null ? "?" : name;
	}

	/** The failure this tick, as text; the root's name when nothing traced reported one. */
	private String failureText() {
		BotTrace.Event failure = freshFailure();
		return failure == null ? root.name() + " -> FAILURE" : failure.describe();
	}

	/**
	 * The failure recorded this tick, or null. The tick check matters: {@code lastFailure} is remembered
	 * on purpose — it is the one thing you always want, even after the event has left the ring — but a
	 * root can also fail without any traced node reporting one (a tree not built by the builder).
	 * Presenting a stale event as if it were current would be worse than saying less.
	 */
	private BotTrace.Event freshFailure() {
		BotTrace.Event failure = ctx.trace().lastFailure();
		return failure != null && failure.tick() == ctx.trace().currentTick() ? failure : null;
	}

	/** What makes two failures "the same problem": which state, and why it gave up. */
	private String failureSignature() {
		BotTrace.Event failure = freshFailure();
		return failure == null ? root.name() : failure.name() + "|" + failure.note();
	}

	/** The last line printed. For tests, and the seed of a future {@code ::botinfo} failure section. */
	String lastReportedFailure() {
		return lastReportedFailure;
	}

	/** How many identical failures have been swallowed since the last line was printed. */
	int suppressedFailures() {
		return suppressedFailures;
	}

	/** Detaches: releases charges, animations and CycleEvents by interrupting the tree. */
	public void stop() {
		if (entered) {
			root.exit(ctx, true);
			entered = false;
		}
	}
}
