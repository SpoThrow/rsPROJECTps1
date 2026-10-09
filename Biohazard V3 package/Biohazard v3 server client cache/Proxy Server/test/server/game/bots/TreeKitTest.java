package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.junit.jupiter.api.Test;

import server.game.bots.composite.Parallel;
import server.game.bots.composite.RandomSelector;
import server.game.bots.composite.Selector;
import server.game.bots.decorator.Cooldown;
import server.game.bots.decorator.Delay;
import server.game.bots.decorator.Fail;
import server.game.bots.decorator.Invert;
import server.game.bots.decorator.Retry;
import server.game.bots.decorator.Succeed;
import server.game.bots.decorator.Timeout;
import server.game.players.actions.objects.ObjectClick;

/**
 * Roadmap Phase B: the tree kit. Selector, Parallel and the decorators, tested the same way
 * slice 1 tested {@code Sequence}/{@code Repeat} — scripted fake leaves and a fake context, so
 * the subject is purely the transitions and the enter/exit bookkeeping.
 *
 * <p>The cases that matter here are the <em>failure paths</em>: a child abandoned mid-run must
 * be told it was interrupted, and a composite that reports an outcome of its own must not
 * leave a child inside {@code enter}/{@code exit}. Those are the rules slice 1 established for
 * {@code Sequence}, and every new node is judged against them.
 */
class TreeKitTest {

	/** A leaf that returns queued outcomes, then repeats its last one forever. */
	private static final class Fake implements BotState {

		private final int id;
		private final Deque<BotStatus> script = new ArrayDeque<BotStatus>();
		private final BotStatus tail;
		int enters, exits, ticks;
		Boolean interruptedOnExit;
		/** When set, the id of this node is appended on enter, so an order can be asserted. */
		List<Integer> journal;

		Fake(int id, BotStatus... outcomes) {
			this.id = id;
			for (BotStatus s : outcomes) {
				script.add(s);
			}
			tail = outcomes[outcomes.length - 1];
		}

		@Override
		public void enter(BotContext ctx) {
			enters++;
			if (journal != null) {
				journal.add(id);
			}
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

	/**
	 * A context that answers every question without a world. {@code random} returns a scripted
	 * value, so {@code RandomSelector}'s shuffle can be pinned exactly.
	 */
	private static final class FakeContext implements BotContext {

		private final Deque<Integer> randoms = new ArrayDeque<Integer>();
		private final int randomDefault;

		FakeContext(int randomDefault) {
			this.randomDefault = randomDefault;
		}

		/** Queues the values {@code random} returns, in order. */
		FakeContext scripted(int... values) {
			for (int value : values) {
				randoms.add(value);
			}
			return this;
		}

		@Override public BotPlayer client() { return null; }
		@Override public int x() { return 0; }
		@Override public int y() { return 0; }
		@Override public int height() { return 0; }
		@Override public boolean arrivedAt(int x, int y, int range) { return true; }
		@Override public boolean isIdle() { return true; }
		@Override public int freeSlots() { return 28; }
		@Override public boolean hasItem(int itemId) { return false; }
		@Override public int ticksInState() { return 0; }
		@Override public int random(int bound) {
			return randoms.isEmpty() ? randomDefault : randoms.poll();
		}
		@Override public void walkTo(int x, int y) { }
		@Override public boolean interactObject(int objectId, int x, int y, ObjectClick click,
				int range) { return true; }
		@Override public void openBank() { }
		@Override public boolean depositItem(int itemId) { return true; }
		@Override public void onStateEntered() { }
		@Override public void onTick() { }
	}

	private final BotContext ctx = new FakeContext(0);

	// ------------------------------------------------------------------ Selector

	@Test
	void aSelectorStopsAtTheFirstChildThatSucceeds() {
		Fake a = new Fake(0, BotStatus.FAILURE);
		Fake b = new Fake(1, BotStatus.SUCCESS);
		Fake c = new Fake(2, BotStatus.SUCCESS);
		Selector selector = new Selector(a, b, c);

		selector.enter(ctx);

		assertEquals(BotStatus.SUCCESS, selector.tick(ctx));
		assertEquals(1, a.enters, "the first alternative was tried");
		assertEquals(1, b.enters, "the second was reached after the first failed");
		assertEquals(0, c.enters, "C must never run once B succeeds");
		assertEquals(Boolean.FALSE, a.interruptedOnExit, "a failing child reports its own outcome");
	}

	@Test
	void aSelectorKeepsARunningChildCurrentAndDoesNotReachTheRest() {
		Fake a = new Fake(0, BotStatus.FAILURE);
		Fake b = new Fake(1, BotStatus.RUNNING, BotStatus.SUCCESS);
		Fake c = new Fake(2, BotStatus.SUCCESS);
		Selector selector = new Selector(a, b, c);

		selector.enter(ctx);

		assertEquals(BotStatus.RUNNING, selector.tick(ctx));
		assertEquals(0, c.enters, "the next alternative is not entered while B runs");
		assertEquals(BotStatus.SUCCESS, selector.tick(ctx));
		assertEquals(2, b.ticks, "B stayed current across ticks rather than being re-chosen");
		assertEquals(0, c.enters);
	}

	@Test
	void aSelectorFailsOnlyWhenEveryAlternativeFails() {
		Fake a = new Fake(0, BotStatus.FAILURE);
		Fake b = new Fake(1, BotStatus.FAILURE);
		Selector selector = new Selector(a, b);

		selector.enter(ctx);

		assertEquals(BotStatus.FAILURE, selector.tick(ctx));
		assertEquals(1, a.enters);
		assertEquals(1, b.enters);
	}

	@Test
	void anEmptySelectorFailsBecauseNoAlternativeCanSucceed() {
		assertEquals(BotStatus.FAILURE, new Selector().tick(ctx));
	}

	@Test
	void aSelectorInterruptedMidRunTellsTheRunningChild() {
		Fake a = new Fake(0, BotStatus.FAILURE);
		Fake b = new Fake(1, BotStatus.RUNNING);
		Selector selector = new Selector(a, b);

		selector.enter(ctx);
		assertEquals(BotStatus.RUNNING, selector.tick(ctx));

		selector.exit(ctx, true);

		assertEquals(1, a.exits);
		assertEquals(Boolean.FALSE, a.interruptedOnExit, "the passed-over alternative was not cut off");
		assertEquals(1, b.exits);
		assertEquals(Boolean.TRUE, b.interruptedOnExit, "the running alternative is told it was cut off");
	}

	// ------------------------------------------------------------------ RandomSelector

	@Test
	void aRandomSelectorVisitsEveryAlternativeExactlyOnceInOneRun() {
		List<Integer> order = new ArrayList<Integer>();
		Fake a = new Fake(0, BotStatus.FAILURE);
		Fake b = new Fake(1, BotStatus.FAILURE);
		Fake c = new Fake(2, BotStatus.FAILURE);
		a.journal = order;
		b.journal = order;
		c.journal = order;
		RandomSelector selector = new RandomSelector(a, b, c);

		selector.enter(ctx);
		assertEquals(BotStatus.FAILURE, selector.tick(ctx));

		order.sort(null);
		assertEquals(List.of(0, 1, 2), order, "a shuffle is a permutation: nothing is skipped or repeated");
	}

	@Test
	void aRandomSelectorShufflesTheOrderItTriesThemIn() {
		List<Integer> order = new ArrayList<Integer>();
		Fake a = new Fake(0, BotStatus.FAILURE);
		Fake b = new Fake(1, BotStatus.FAILURE);
		Fake c = new Fake(2, BotStatus.FAILURE);
		a.journal = order;
		b.journal = order;
		c.journal = order;
		RandomSelector selector = new RandomSelector(a, b, c);

		// Every swap partner is 0: descending Fisher-Yates turns [0,1,2] into [1,2,0].
		selector.enter(ctx);
		assertEquals(BotStatus.FAILURE, selector.tick(ctx));

		assertEquals(List.of(1, 2, 0), order, "the declaration order is not the order it was tried in");
	}

	// ------------------------------------------------------------------ Parallel

	@Test
	void aParallelTicksEveryChildOnEveryTick() {
		Fake a = new Fake(0, BotStatus.RUNNING);
		Fake b = new Fake(1, BotStatus.RUNNING);
		Parallel parallel = new Parallel(a, b);

		parallel.enter(ctx);

		assertEquals(BotStatus.RUNNING, parallel.tick(ctx));
		assertEquals(1, a.ticks);
		assertEquals(1, b.ticks);
		assertEquals(BotStatus.RUNNING, parallel.tick(ctx));
		assertEquals(2, a.ticks, "a parallel makes progress on all of its children, not one per tick");
		assertEquals(2, b.ticks);
	}

	@Test
	void aParallelSucceedsWhenEveryChildHasSucceeded() {
		Fake a = new Fake(0, BotStatus.RUNNING, BotStatus.SUCCESS);
		Fake b = new Fake(1, BotStatus.SUCCESS);
		Parallel parallel = new Parallel(a, b);

		parallel.enter(ctx);

		assertEquals(BotStatus.RUNNING, parallel.tick(ctx));
		assertEquals(BotStatus.SUCCESS, parallel.tick(ctx));
		assertEquals(Boolean.FALSE, b.interruptedOnExit, "a child that succeeded was not cut off");
	}

	@Test
	void aParallelFailsImmediatelyAndInterruptsWhatIsStillRunning() {
		Fake a = new Fake(0, BotStatus.RUNNING);
		Fake b = new Fake(1, BotStatus.FAILURE);
		Parallel parallel = new Parallel(a, b);

		parallel.enter(ctx);

		assertEquals(BotStatus.FAILURE, parallel.tick(ctx));
		assertEquals(Boolean.FALSE, b.interruptedOnExit, "the failing child reported its own outcome");
		assertEquals(1, a.exits);
		assertEquals(Boolean.TRUE, a.interruptedOnExit,
				"a sibling still mid-run is cut off when the parallel gives up");

		parallel.exit(ctx, false);
		assertEquals(1, a.exits, "an already-abandoned child is not exited twice");
	}

	@Test
	void aParallelWithNoChildrenSucceedsVacuously() {
		assertEquals(BotStatus.SUCCESS, new Parallel().tick(ctx));
	}

	// ------------------------------------------------------------------ Retry

	@Test
	void aRetryReRunsItsChildUntilItSucceeds() {
		Fake a = new Fake(0, BotStatus.FAILURE, BotStatus.FAILURE, BotStatus.SUCCESS);
		Retry retry = new Retry(a, 3);

		retry.enter(ctx);

		assertEquals(BotStatus.SUCCESS, retry.tick(ctx));
		assertEquals(3, a.ticks, "it took all three tries, in one tick");
		assertEquals(3, a.enters, "a retried child is re-entered, not re-ticked");
	}

	@Test
	void aRetryGivesUpAfterItsAttempts() {
		Fake a = new Fake(0, BotStatus.FAILURE);
		Retry retry = new Retry(a, 3);

		retry.enter(ctx);

		assertEquals(BotStatus.FAILURE, retry.tick(ctx));
		assertEquals(3, a.ticks, "exactly the declared number of tries, so a permanent failure is bounded");
	}

	@Test
	void aRetryPassesRunningThroughWithoutCountingItAsAFailure() {
		Fake a = new Fake(0, BotStatus.RUNNING, BotStatus.SUCCESS);
		Retry retry = new Retry(a, 3);

		retry.enter(ctx);

		assertEquals(BotStatus.RUNNING, retry.tick(ctx));
		assertEquals(1, a.enters, "a running child is not re-entered");
		assertEquals(BotStatus.SUCCESS, retry.tick(ctx));
	}

	@Test
	void aRetryInterruptedMidRunTellsTheChild() {
		Fake a = new Fake(0, BotStatus.RUNNING);
		Retry retry = new Retry(a, 3);

		retry.enter(ctx);
		assertEquals(BotStatus.RUNNING, retry.tick(ctx));

		retry.exit(ctx, true);

		assertEquals(1, a.exits);
		assertEquals(Boolean.TRUE, a.interruptedOnExit);
	}

	// ------------------------------------------------------------------ Timeout

	@Test
	void aTimeoutFailsARunningChildOnceItsBudgetIsSpent() {
		Fake a = new Fake(0, BotStatus.RUNNING);
		Timeout timeout = new Timeout(a, 2);

		timeout.enter(ctx);

		assertEquals(BotStatus.RUNNING, timeout.tick(ctx));
		assertEquals(BotStatus.FAILURE, timeout.tick(ctx), "the budget of two ticks was spent");
		assertEquals(1, a.exits);
		assertEquals(Boolean.TRUE, a.interruptedOnExit,
				"a child cut off by its budget is told it was interrupted");
	}

	@Test
	void aTimeoutPassesThroughAnOutcomeItDoesNotHaveToBound() {
		Fake success = new Fake(0, BotStatus.SUCCESS);
		Timeout quick = new Timeout(success, 5);
		quick.enter(ctx);
		assertEquals(BotStatus.SUCCESS, quick.tick(ctx));
		assertEquals(Boolean.FALSE, success.interruptedOnExit);

		Fake failure = new Fake(1, BotStatus.FAILURE);
		Timeout certain = new Timeout(failure, 5);
		certain.enter(ctx);
		assertEquals(BotStatus.FAILURE, certain.tick(ctx));
	}

	// ------------------------------------------------------------------ Delay

	@Test
	void aDelaySucceedsOnTheTickThatCompletesItsWait() {
		Delay delay = new Delay(3);

		delay.enter(ctx);

		assertEquals(BotStatus.RUNNING, delay.tick(ctx));
		assertEquals(BotStatus.RUNNING, delay.tick(ctx));
		assertEquals(BotStatus.SUCCESS, delay.tick(ctx));
	}

	@Test
	void aZeroDelaySucceedsOnItsFirstTick() {
		Delay delay = new Delay(0);

		delay.enter(ctx);

		assertEquals(BotStatus.SUCCESS, delay.tick(ctx));
	}

	// ------------------------------------------------------------------ Cooldown

	@Test
	void aCooldownHoldsAfterItsChildSucceeds() {
		Fake a = new Fake(0, BotStatus.SUCCESS);
		Cooldown cooldown = new Cooldown(a, 2);

		cooldown.enter(ctx);

		assertEquals(BotStatus.RUNNING, cooldown.tick(ctx), "the hold starts on the tick the child finished");
		assertEquals(BotStatus.SUCCESS, cooldown.tick(ctx));
		assertEquals(1, a.ticks, "the child ran once; the cooldown is a pause, not a re-run");
	}

	@Test
	void aCooldownDoesNotHoldWhenItsChildFails() {
		Fake a = new Fake(0, BotStatus.FAILURE);
		Cooldown cooldown = new Cooldown(a, 5);

		cooldown.enter(ctx);

		assertEquals(BotStatus.FAILURE, cooldown.tick(ctx), "a broken step must not also cost a delay");
	}

	// ------------------------------------------------------------------ Invert / Succeed / Fail

	@Test
	void anInvertSwapsSuccessAndFailure() {
		Fake succeeds = new Fake(0, BotStatus.SUCCESS);
		Invert inverted = new Invert(succeeds);
		inverted.enter(ctx);
		assertEquals(BotStatus.FAILURE, inverted.tick(ctx));

		Fake fails = new Fake(1, BotStatus.FAILURE);
		Invert flipped = new Invert(fails);
		flipped.enter(ctx);
		assertEquals(BotStatus.SUCCESS, flipped.tick(ctx));
	}

	@Test
	void anInvertPassesRunningThroughBecauseItHasNoOutcomeToInvert() {
		Fake a = new Fake(0, BotStatus.RUNNING);
		Invert invert = new Invert(a);

		invert.enter(ctx);

		assertEquals(BotStatus.RUNNING, invert.tick(ctx));
	}

	@Test
	void aSucceedMakesAnOptionalStepNotMatter() {
		Fake fails = new Fake(0, BotStatus.FAILURE);
		Succeed optional = new Succeed(fails);
		optional.enter(ctx);
		assertEquals(BotStatus.SUCCESS, optional.tick(ctx), "a best-effort step cannot abort the branch");

		Fake succeeds = new Fake(1, BotStatus.SUCCESS);
		Succeed plain = new Succeed(succeeds);
		plain.enter(ctx);
		assertEquals(BotStatus.SUCCESS, plain.tick(ctx));
	}

	@Test
	void aFailTurnsASucceedingChildIntoAnAbort() {
		Fake dead = new Fake(0, BotStatus.SUCCESS);
		Fail guard = new Fail(dead);
		guard.enter(ctx);
		assertEquals(BotStatus.FAILURE, guard.tick(ctx), "the guard firing must abort the routine");

		Fake alive = new Fake(1, BotStatus.FAILURE);
		Fail other = new Fail(alive);
		other.enter(ctx);
		assertEquals(BotStatus.FAILURE, other.tick(ctx), "and staying quiet still fails, because the child failed");
	}

	// ------------------------------------------------------------------ labels

	@Test
	void everyNewNodeIsSelfDescribing() {
		Fake a = new Fake(0, BotStatus.SUCCESS);
		assertEquals("Selector(2)", new Selector(a, a).name());
		assertEquals("RandomSelector(2)", new RandomSelector(a, a).name());
		assertEquals("Parallel(2)", new Parallel(a, a).name());
		assertEquals("Retry(3)", new Retry(a, 3).name());
		assertEquals("Timeout(40)", new Timeout(a, 40).name());
		assertEquals("Delay(3)", new Delay(3).name());
		assertEquals("Cooldown(5)", new Cooldown(a, 5).name());
		assertEquals("Invert", new Invert(a).name());
		assertEquals("Succeed", new Succeed(a).name());
		assertEquals("Fail", new Fail(a).name());
	}
}
