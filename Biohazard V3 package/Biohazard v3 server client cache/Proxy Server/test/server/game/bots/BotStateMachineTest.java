package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.jupiter.api.Test;

import server.game.bots.composite.Repeat;
import server.game.bots.composite.Sequence;

/**
 * Slice-1 step 4: composites own every transition. These tests use fake states that return
 * scripted statuses, so no client or world is needed — the point is purely the transition
 * and enter/exit bookkeeping.
 */
class BotStateMachineTest {

	/**
	 * A leaf that returns queued outcomes in order and then repeats its last outcome
	 * forever, recording how often each lifecycle method ran.
	 */
	private static final class Fake implements BotState {

		private final Deque<BotStatus> script = new ArrayDeque<BotStatus>();
		private final BotStatus tail;
		int enters, exits, ticks;
		Boolean interruptedOnExit;

		Fake(BotStatus... outcomes) {
			for (BotStatus s : outcomes) {
				script.add(s);
			}
			tail = outcomes[outcomes.length - 1];
		}

		@Override
		public void enter(BotContext ctx) {
			enters++;
		}

		@Override
		public BotStatus tick(BotContext ctx) {
			ticks++;
			return script.isEmpty() ? tail : script.poll();
		}

		@Override
		public void exit(BotContext ctx, boolean interrupted) {
			exits++;
			interruptedOnExit = interrupted;
		}
	}

	/** The composites never dereference the context, and the fakes ignore it. */
	private static final BotContext CTX = null;

	// ------------------------------------------------------------------ Sequence

	@Test
	void aSequenceAdvancesThroughSucceedingChildrenInOneTick() {
		Fake a = new Fake(BotStatus.SUCCESS);
		Fake b = new Fake(BotStatus.SUCCESS);
		Fake c = new Fake(BotStatus.SUCCESS);
		Sequence seq = new Sequence(a, b, c);

		seq.enter(CTX);

		assertEquals(BotStatus.SUCCESS, seq.tick(CTX));
		assertEquals(1, a.enters);
		assertEquals(1, b.enters);
		assertEquals(1, c.enters);
		assertEquals(1, a.exits, "a child that reported SUCCESS is exited before the next enters");
		assertEquals(Boolean.FALSE, a.interruptedOnExit, "SUCCESS is not an interruption");
	}

	@Test
	void aFailedChildAbortsTheSequenceAndTheRestNeverEnter() {
		Fake a = new Fake(BotStatus.SUCCESS);
		Fake b = new Fake(BotStatus.FAILURE);
		Fake c = new Fake(BotStatus.SUCCESS);
		Sequence seq = new Sequence(a, b, c);

		seq.enter(CTX);

		assertEquals(BotStatus.FAILURE, seq.tick(CTX));
		assertEquals(0, c.enters, "C must never run after B fails");
		assertEquals(1, b.exits, "the failing child still gets its own exit");
		assertEquals(Boolean.FALSE, b.interruptedOnExit, "it reported its own outcome");
	}

	@Test
	void aRunningChildStaysCurrentAcrossTicks() {
		Fake a = new Fake(BotStatus.RUNNING, BotStatus.SUCCESS);
		Fake b = new Fake(BotStatus.SUCCESS);
		Sequence seq = new Sequence(a, b);

		seq.enter(CTX);

		assertEquals(BotStatus.RUNNING, seq.tick(CTX));
		assertEquals(1, a.ticks);
		assertEquals(0, b.enters, "the next child is not entered while A runs");

		assertEquals(BotStatus.SUCCESS, seq.tick(CTX));
		assertEquals(2, a.ticks);
		assertEquals(1, b.enters);
	}

	@Test
	void anEmptySequenceSucceedsImmediately() {
		assertEquals(BotStatus.SUCCESS, new Sequence().tick(CTX));
	}

	@Test
	void exitingEarlyInterruptsTheCurrentChildExactlyOnce() {
		Fake a = new Fake(BotStatus.RUNNING);
		Sequence seq = new Sequence(a, new Fake(BotStatus.SUCCESS));

		seq.enter(CTX);
		assertEquals(BotStatus.RUNNING, seq.tick(CTX));

		seq.exit(CTX, true);

		assertEquals(1, a.exits);
		assertEquals(Boolean.TRUE, a.interruptedOnExit, "an abandoned child is told it was interrupted");
	}

	// ------------------------------------------------------------------ Repeat

	@Test
	void aFiniteRepeatRunsTheChildExactlyCountTimesThenSucceeds() {
		Fake a = new Fake(BotStatus.SUCCESS);
		Repeat repeat = new Repeat(a, 3);

		repeat.enter(CTX);

		assertEquals(BotStatus.SUCCESS, repeat.tick(CTX));
		assertEquals(3, a.ticks, "A is ticked to SUCCESS exactly three times");
		assertEquals(3, a.enters, "once initially and once per re-entry");
		assertEquals(3, a.exits);
	}

	@Test
	void aForeverRepeatNeverReportsDoneAndYieldsEachTick() {
		Fake a = new Fake(BotStatus.SUCCESS);
		Repeat repeat = new Repeat(a, -1);

		repeat.enter(CTX);

		assertEquals(BotStatus.RUNNING, repeat.tick(CTX));
		assertEquals(BotStatus.RUNNING, repeat.tick(CTX));
		// One completed run per tick — a child that succeeds instantly must not spin the
		// game thread inside a single tick.
		assertEquals(2, a.ticks);
		assertEquals(3, a.enters, "initial enter plus one re-entry per tick");
	}

	@Test
	void aRepeatPropagatesFailure() {
		Fake a = new Fake(BotStatus.FAILURE);
		Repeat repeat = new Repeat(a, 5);

		repeat.enter(CTX);

		assertEquals(BotStatus.FAILURE, repeat.tick(CTX));
		assertEquals(1, a.ticks, "it does not retry past a failure");
	}

	@Test
	void aZeroCountRepeatDoesNotRunItsChild() {
		Fake a = new Fake(BotStatus.SUCCESS);
		Repeat repeat = new Repeat(a, 0);

		repeat.enter(CTX);

		assertEquals(BotStatus.SUCCESS, repeat.tick(CTX));
		assertEquals(0, a.ticks);
		assertEquals(0, a.enters);
	}

	// ------------------------------------------------------------------ labels

	@Test
	void everyNodeIsSelfDescribing() {
		Fake a = new Fake(BotStatus.SUCCESS);
		assertEquals("Fake", a.name(), "a leaf defaults to its class name");
		assertEquals("Sequence(2)", new Sequence(a, a).name());
		assertEquals("Repeat(forever)", new Repeat(a, -1).name());
		assertEquals("Repeat(3)", new Repeat(a, 3).name());
	}
}
