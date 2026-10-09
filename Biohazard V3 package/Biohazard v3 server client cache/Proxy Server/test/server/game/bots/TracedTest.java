package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The tracing wrapper — roadmap Phase F.
 *
 * <p>Two things are being pinned. First, that it is <b>transparent</b>: a wrapped tree behaves exactly
 * like an unwrapped one, which is what lets the builder wrap everything without any composite, test or
 * call site noticing. Second, that it is <b>quiet</b>: a state that is merely running writes nothing, so
 * a bot that walks for hundreds of ticks costs a handful of events rather than hundreds.
 */
class TracedTest {

	/** Reports RUNNING for a fixed number of ticks, then SUCCESS. */
	private static final class Runs implements BotState {
		private final int ticks;
		private int seen;

		Runs(int ticks) {
			this.ticks = ticks;
		}

		@Override
		public void enter(BotContext ctx) {
			seen = 0;
		}

		@Override
		public BotStatus tick(BotContext ctx) {
			return ++seen >= ticks ? BotStatus.SUCCESS : BotStatus.RUNNING;
		}

		@Override
		public void exit(BotContext ctx, boolean interrupted) {
		}

		@Override
		public String name() {
			return "Runs(" + ticks + ")";
		}
	}

	/** Fails immediately, with a reason, the way a leaf that gave up does. */
	private static final class GivesUp implements BotState {

		@Override
		public void enter(BotContext ctx) {
		}

		@Override
		public BotStatus tick(BotContext ctx) {
			ctx.trace().note("no oak within 8 tiles");
			return BotStatus.FAILURE;
		}

		@Override
		public void exit(BotContext ctx, boolean interrupted) {
		}
	}

	@Test
	void nameDelegatesSoAWrappedTreeStillReadsAsItself() {
		Traced traced = new Traced(new Runs(1));

		assertEquals("Runs(1)", traced.name());
		assertEquals("Traced(Runs(1))", traced.toString());
	}

	@Test
	void enterAndExitDelegateToTheWrappedNode() {
		Counting counting = new Counting();
		FakeBotContext ctx = new FakeBotContext();
		Traced traced = new Traced(counting);

		traced.enter(ctx);
		traced.tick(ctx);
		traced.exit(ctx, true);

		assertEquals(1, counting.enters);
		assertEquals(1, counting.ticks);
		assertEquals(1, counting.exits);
		assertSame(counting, traced.child());
	}

	@Test
	void aRunningTickWritesNothing() {
		FakeBotContext ctx = new FakeBotContext();
		Traced traced = new Traced(new Runs(5));

		traced.enter(ctx);
		assertEquals(1, ctx.trace().recordedCount(), "the enter is worth one event");

		for (int i = 0; i < 4; i++) {
			traced.tick(ctx);
		}
		assertEquals(1, ctx.trace().recordedCount(), "still running is not a transition");

		traced.tick(ctx);
		assertEquals(2, ctx.trace().recordedCount(), "the outcome is");
		assertEquals(BotTrace.Kind.SUCCESS, ctx.trace().history(1).get(0).kind());
	}

	@Test
	void successFollowedByExitIsRecordedOnceNotTwice() {
		FakeBotContext ctx = new FakeBotContext();
		Traced traced = new Traced(new Runs(1));

		traced.enter(ctx);
		traced.tick(ctx);
		// The composites do exactly this: a child reports, then is exited anyway.
		traced.exit(ctx, false);

		assertEquals(2, ctx.trace().recordedCount(), "one enter, one outcome");
	}

	@Test
	void anInterruptIsRecordedAsAbort() {
		FakeBotContext ctx = new FakeBotContext();
		Traced traced = new Traced(new Runs(100));

		traced.enter(ctx);
		traced.tick(ctx);
		traced.exit(ctx, true);

		assertEquals(2, ctx.trace().recordedCount());
		assertEquals(BotTrace.Kind.ABORT, ctx.trace().history(1).get(0).kind());
	}

	@Test
	void ticksInStateIsMeasuredOnTheDriverClock() {
		FakeBotContext ctx = new FakeBotContext();
		Traced traced = new Traced(new Runs(4));

		traced.enter(ctx);
		for (int i = 0; i < 4; i++) {
			ctx.onTick(); // what the controller does before ticking the tree
			traced.tick(ctx);
		}

		BotTrace.Event outcome = ctx.trace().history(1).get(0);
		assertEquals(BotTrace.Kind.SUCCESS, outcome.kind());
		assertEquals(4, outcome.ticksInState(), "the state ran for four ticks");
	}

	@Test
	void aReasonSetByTheStateRidesAlongWithItsOutcome() {
		FakeBotContext ctx = new FakeBotContext();
		Traced traced = new Traced(new GivesUp());

		traced.enter(ctx);
		traced.tick(ctx);

		BotTrace.Event failure = ctx.trace().lastFailure();
		assertEquals("no oak within 8 tiles", failure.note());
		assertEquals(BotTrace.Kind.FAILURE, failure.kind());
	}

	@Test
	void nestingProducesThePathRootFirstDeepestLast() {
		FakeBotContext ctx = new FakeBotContext();
		Traced root = new Traced(new Composite("outer",
				new Traced(new Composite("inner", new Traced(new Runs(3))))));

		root.enter(ctx);
		root.tick(ctx);

		// The path is exactly the enters not yet matched by an outcome — no depth is passed in by any
		// node, which is what makes a trace correct for a tree assembled by any means.
		assertEquals("outer > inner > Runs(3)", ctx.trace().pathLine());
	}

	@Test
	void thePathUnwindsAsStepsReportSuccess() {
		FakeBotContext ctx = new FakeBotContext();
		Traced root = new Traced(new Composite("outer",
				new Traced(new Composite("inner", new Traced(new Runs(1))))));

		root.enter(ctx);
		root.tick(ctx);

		// Every node finished in one tick, so nothing is current and the whole path has unwound.
		assertEquals("(idle)", ctx.trace().pathLine());
		assertTrue(ctx.trace().history(10).size() >= 6, "enter and outcome for each of three nodes");
		assertEquals(BotTrace.Kind.SUCCESS, ctx.trace().history(1).get(0).kind(),
				"the last thing that happened was the root succeeding");
	}

	@Test
	void wrappingSomethingAlreadyTracedIsRejectedRatherThanDoubled() {
		Traced once = new Traced(new Runs(1));

		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
				() -> new Traced(once));
		assertTrue(thrown.getMessage().contains("already traced"), thrown.getMessage());
		assertThrows(IllegalArgumentException.class, () -> new Traced(null));
	}

	/** Delegates to children in order, like a Sequence but with no advance bookkeeping to get wrong. */
	private static final class Composite implements BotState {
		private final String label;
		private final BotState[] children;
		private int index;

		Composite(String label, BotState... children) {
			this.label = label;
			this.children = children;
		}

		@Override
		public void enter(BotContext ctx) {
			index = 0;
			if (children.length > 0) {
				children[0].enter(ctx);
			}
		}

		@Override
		public BotStatus tick(BotContext ctx) {
			while (index < children.length) {
				BotStatus status = children[index].tick(ctx);
				if (status == BotStatus.RUNNING) {
					return BotStatus.RUNNING;
				}
				children[index].exit(ctx, false);
				index++;
				if (index < children.length) {
					children[index].enter(ctx);
				}
			}
			return BotStatus.SUCCESS;
		}

		@Override
		public void exit(BotContext ctx, boolean interrupted) {
		}

		@Override
		public String name() {
			return label;
		}
	}

	private static final class Counting implements BotState {
		int enters;
		int ticks;
		int exits;

		@Override
		public void enter(BotContext ctx) {
			enters++;
		}

		@Override
		public BotStatus tick(BotContext ctx) {
			ticks++;
			return BotStatus.RUNNING;
		}

		@Override
		public void exit(BotContext ctx, boolean interrupted) {
			exits++;
		}
	}
}
