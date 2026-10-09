package server.game.bots;

import server.game.bots.meta.RuntimeOnly;

/**
 * Wraps one node so it reports its transitions to the bot's {@link BotTrace} — roadmap Phase F.
 *
 * <p><b>Why a wrapper and not a line in every composite.</b> A tree's transitions are known exactly
 * where a child is entered and exited: in {@code Sequence}, {@code Selector}, {@code Repeat}, the
 * decorators and the controller. Adding a reporting call at each of those fifteen-odd sites would be
 * fifteen chances to miss one, and it would put tracing into the composition vocabulary. Wrapping the
 * node instead means the reports are produced by the node's own {@code enter}/{@code tick}/{@code exit}
 * calls, in the nesting order the tree contract already guarantees, and not one composite changes.
 *
 * <p><b>Transparent, by design.</b> {@link #name()} delegates, so {@code script.root().name()} still
 * reads {@code Repeat(forever)}; {@code enter}/{@code exit} delegate; and {@code tick} returns exactly
 * what its child returned. A wrapped tree behaves identically to an unwrapped one — it just says what it
 * is doing.
 *
 * <p><b>Once per entry, never per tick.</b> A {@code RUNNING} tick writes nothing. Only becoming current
 * and reporting an outcome produce an event, and {@link #open} is what guarantees a state that returns
 * SUCCESS and is then {@code exit}ed anyway (which the composites do) is recorded once, not twice.
 *
 * <p><b>Where wrapping happens.</b> {@link server.game.bots.script.ScriptBuilder} wraps the nodes it
 * builds, bottom-up, so every script authored through the builder is traced end to end. A tree assembled
 * by hand is not: there is no generic child accessor to walk it, and the builder is the sanctioned
 * authoring surface (Phase D/E), so that is the honest boundary. Hand-built trees still run — they simply
 * produce no trace.
 *
 * <p><b>Not an authorable node.</b> {@link RuntimeOnly} says so, which is what keeps the workshop's
 * palette honest: this is a {@code BotState} the runtime applies, not one an author places.
 */
@RuntimeOnly("applied by the builder and the runtime for tracing; never placed in the editor")
public final class Traced implements BotState {

	private final BotState child;

	/** Whether this node is currently inside its own enter/exit pair. */
	private boolean open;

	/** The tick this node was entered on, for the "after Nt" half of an event. */
	private long enteredAt;

	public Traced(BotState child) {
		if (child == null) {
			throw new IllegalArgumentException("a traced node needs something to wrap");
		}
		if (child instanceof Traced) {
			throw new IllegalArgumentException("already traced: " + child.name());
		}
		this.child = child;
	}

	/** The wrapped node. For tests and for anyone who needs the real type back. */
	public BotState child() {
		return child;
	}

	@Override
	public void enter(BotContext ctx) {
		BotTrace trace = ctx.trace();
		if (!open) {
			open = true;
			enteredAt = trace.currentTick();
			trace.entered(name(), enteredAt);
		}
		child.enter(ctx);
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		BotStatus status = child.tick(ctx);
		if (status != BotStatus.RUNNING && open) {
			open = false;
			ctx.trace().outcome(name(), kindOf(status), ticks(ctx), ctx.trace().currentTick());
		}
		return status;
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		if (open) {
			// Left before reporting an outcome: the controller's stop, a despawn, a shutdown.
			open = false;
			ctx.trace().outcome(name(), BotTrace.Kind.ABORT, ticks(ctx), ctx.trace().currentTick());
		}
		child.exit(ctx, interrupted);
	}

	/** Ticks this node was current. The trace's counter, so it is monotone even across a pause. */
	private int ticks(BotContext ctx) {
		long elapsed = ctx.trace().currentTick() - enteredAt;
		return elapsed <= 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, elapsed);
	}

	private static BotTrace.Kind kindOf(BotStatus status) {
		return status == BotStatus.SUCCESS ? BotTrace.Kind.SUCCESS : BotTrace.Kind.FAILURE;
	}

	@Override
	public String name() {
		return child.name();
	}

	@Override
	public String toString() {
		return "Traced(" + child.name() + ")";
	}
}
