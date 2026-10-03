package server.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the cycle-event error path.
 *
 * <p>An event whose {@code execute} throws used to be taken out of rotation with
 * {@code setRunning(false)}, which does <em>not</em> invoke the event's {@code stop()}
 * callback — only {@link CycleEventContainer#stop()} does. Everything an event sets up is
 * torn down in that callback, so a single thrown exception leaked the event's state. The
 * woodcutting event is the clearest case: it clears {@code active}/{@code treeX}/
 * {@code treeY} in {@code stop()}, and {@code startWoodcutting} early-returns on
 * {@code active}, so one crashed tick locked that player out of woodcutting until they
 * relogged.
 *
 * <p>These tests drive {@link CycleEventHandler#process()} directly rather than through a
 * live server, and assert only on their own event, because the handler's event list is
 * static and shared with whatever other tests registered events.
 */
class CycleEventHandlerTest {

	private Object owner;

	@BeforeEach
	void setUp() {
		owner = new Object();
	}

	@AfterEach
	void tearDown() {
		// Mark this test's event stopped so it cannot be executed by anyone else's
		// process() call. Container.stop() runs the callback as well, which is harmless
		// here -- these events only flip flags.
		CycleEventHandler.stopEvents(owner);
	}

	private void addThrowingEvent(final int[] executes, final boolean[] stopped) {
		CycleEventHandler.addEvent(owner, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				executes[0]++;
				throw new IllegalStateException("deliberate test failure");
			}

			@Override
			public void stop() {
				stopped[0] = true;
			}
		}, 1);
	}

	@Test
	void aThrowingEventStillRunsItsStopCallback() {
		final int[] executes = { 0 };
		final boolean[] stopped = { false };
		addThrowingEvent(executes, stopped);

		CycleEventHandler.process();

		assertEquals(1, executes[0], "the event should have been executed once");
		assertTrue(stopped[0], "stop() must run even when execute() throws, or state leaks");
	}

	@Test
	void aCrashedEventIsNotRetried() {
		final int[] executes = { 0 };
		final boolean[] stopped = { false };
		addThrowingEvent(executes, stopped);

		CycleEventHandler.process();
		CycleEventHandler.process();
		CycleEventHandler.process();

		assertEquals(1, executes[0], "a crashed event must be taken out of rotation, not retried every tick");
	}

	@Test
	void aThrowingStopCallbackDoesNotEscapeIntoTheTickLoop() {
		// stop() is overridden by ~95 events across the codebase, so it is third-party code
		// from the handler's point of view. If a crashing event's cleanup also crashes, that
		// must cost the one event, not the whole server tick.
		final int[] executes = { 0 };
		CycleEventHandler.addEvent(owner, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				executes[0]++;
				throw new IllegalStateException("deliberate execute failure");
			}

			@Override
			public void stop() {
				throw new IllegalStateException("deliberate stop failure");
			}
		}, 1);

		CycleEventHandler.process();

		assertEquals(1, executes[0], "the tick loop must survive a throwing stop() callback");
	}

	@Test
	void aSelfStoppingEventRunsItsCallbackExactlyOnce() {
		// The property the fix above relies on: Container.stop() is idempotent, because it
		// is guarded by isRunning(). That is what makes routing the error path through
		// stop() safe -- an event that already stopped itself in execute() (which is the
		// normal success path for the skill events, e.g. Thieving awards its loot from
		// stop()) cannot be cleaned up or rewarded twice.
		final int[] executes = { 0 };
		final int[] stops = { 0 };
		CycleEventHandler.addEvent(owner, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				executes[0]++;
				container.stop();
			}

			@Override
			public void stop() {
				stops[0]++;
			}
		}, 1);

		CycleEventHandler.process();
		CycleEventHandler.process();

		assertEquals(1, executes[0], "a stopped event must not execute again");
		assertEquals(1, stops[0], "the stop callback must run exactly once, however often it is asked to stop");
	}
}
