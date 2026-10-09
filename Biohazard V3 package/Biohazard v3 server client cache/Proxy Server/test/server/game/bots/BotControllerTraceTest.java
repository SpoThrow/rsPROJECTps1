package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * The controller as the driver and the failure reporter — roadmap Phase F.
 *
 * <p>The throttle is asserted as <em>state</em> ({@link BotController#reportFailure()} returns the line
 * it printed, or null) rather than by capturing stdout. Printing is the side effect; the decision is the
 * thing with a rule worth pinning.
 */
class BotControllerTraceTest {

	/** Reports FAILURE every tick, with a reason — like a leaf that has given up for a specific cause. */
	private static final class Fails implements BotState {
		private String reason;

		Fails(String reason) {
			this.reason = reason;
		}

		void reason(String reason) {
			this.reason = reason;
		}

		@Override
		public void enter(BotContext ctx) {
		}

		@Override
		public BotStatus tick(BotContext ctx) {
			if (reason != null) {
				ctx.trace().note(reason);
			}
			return BotStatus.FAILURE;
		}

		@Override
		public void exit(BotContext ctx, boolean interrupted) {
		}

		@Override
		public String name() {
			return "Fails";
		}
	}

	/** Never finishes, so only a stop takes it out. */
	private static final class RunsForever implements BotState {

		@Override
		public void enter(BotContext ctx) {
		}

		@Override
		public BotStatus tick(BotContext ctx) {
			return BotStatus.RUNNING;
		}

		@Override
		public void exit(BotContext ctx, boolean interrupted) {
		}

		@Override
		public String name() {
			return "RunsForever";
		}
	}

	/** Attached but not yet ticked, so each test drives the clock itself. */
	private static BotController controller(BotState root, String account) {
		BotPlayer bot = new BotPlayer(0);
		bot.playerName = account;
		BotController controller = new BotController(bot, root);
		controller.enter();
		return controller;
	}

	@Test
	void theClockAdvancesOncePerTickAndStampsEveryEvent() {
		BotController controller = controller(new Traced(new Fails(null)), "botclock");

		assertEquals(0, controller.trace().currentTick(), "attach is tick zero");
		controller.tick();
		assertEquals(1, controller.trace().currentTick());
		controller.tick();
		assertEquals(2, controller.trace().currentTick());

		// Stamped with the tick it happened on, which is what makes "after Nt" measurable.
		assertEquals(2, controller.trace().lastFailure().tick());
		assertEquals(BotTrace.Kind.FAILURE, controller.trace().lastFailure().kind());
	}

	@Test
	void aFailureIsReportedOnceAndThenSuppressedWhileItRepeats() {
		BotController controller = controller(new Traced(new Fails("no oak within 8 tiles")), "botoak");

		// tick() is what reports; the controller owns the log so no caller can forget to write it.
		controller.tick();
		String first = controller.lastReportedFailure();

		assertNotNull(first);
		assertTrue(first.startsWith("[bot] botoak: "), first);
		assertTrue(first.contains("-> FAILURE"), first);
		assertTrue(first.contains("no oak within 8 tiles"), first);

		// The controller restarts a failed root next tick, so the same problem recurs constantly. That
		// must not print every tick.
		controller.tick();
		controller.tick();
		assertEquals(2, controller.suppressedFailures());
		assertEquals(first, controller.lastReportedFailure(), "the line printed did not change");
	}

	@Test
	void aChangedReasonIsReportedAgainAndSaysHowManyItSwallowed() {
		Fails state = new Fails("no oak within 8 tiles");
		BotController controller = controller(new Traced(state), "botoak");
		controller.tick();
		controller.tick();
		controller.tick();
		controller.tick();
		assertEquals(3, controller.suppressedFailures());

		// The bot walked somewhere else and failed differently: that is news, and the count of what was
		// swallowed rides along with it, so nothing is silently lost.
		state.reason("no bank place on plane 0");
		controller.tick();
		String next = controller.lastReportedFailure();

		assertTrue(next.contains("no bank place on plane 0"), next);
		assertTrue(next.contains("repeated 3x"), next);
		assertEquals(0, controller.suppressedFailures());
	}

	@Test
	void aTreeWithNoTracingStillReportsItsFailure() {
		// Hand-built trees are not wrapped (see Traced). The log must still say something true rather
		// than presenting a stale event from an earlier run as if it were current.
		BotController controller = controller(new Fails("nothing records this"), "botraw");
		controller.tick();

		String line = controller.lastReportedFailure();

		assertNotNull(line);
		assertTrue(line.contains("Fails -> FAILURE"), line);
		assertNull(controller.trace().lastFailure(), "nothing traced, so there is no failure event");
	}

	@Test
	void aFailingBotRestartsItselfOnTheNextTickRatherThanStopping() {
		BotController controller = controller(new Traced(new Fails(null)), "botrestart");

		controller.tick();
		assertEquals(BotTrace.Kind.FAILURE, controller.trace().history(1).get(0).kind());

		// Re-entered automatically: a routine bot must not need a human to nudge it back to life.
		controller.tick();
		assertEquals(BotTrace.Kind.FAILURE, controller.trace().history(1).get(0).kind());
		assertTrue(controller.trace().recordedCount() >= 4, "two full enter/fail cycles");
	}

	@Test
	void stoppingMidRunRecordsAnAbortAndEmptiesThePath() {
		BotController controller = controller(new Traced(new RunsForever()), "botstop");
		controller.tick();
		assertEquals(Arrays.asList("RunsForever"), controller.trace().path());

		controller.stop();

		assertEquals(BotTrace.Kind.ABORT, controller.trace().history(1).get(0).kind());
		assertEquals("(idle)", controller.trace().pathLine());
	}
}
