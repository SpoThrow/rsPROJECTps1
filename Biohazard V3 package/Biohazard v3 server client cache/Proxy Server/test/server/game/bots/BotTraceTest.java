package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import server.game.bots.BotTrace.Event;
import server.game.bots.BotTrace.Kind;

/**
 * The per-bot ring buffer — roadmap Phase F.
 *
 * <p>Package-private access on purpose: {@code entered}/{@code outcome} are the raw reports the
 * {@link Traced} wrapper makes, and testing them here (rather than only through a tree) is what lets the
 * ring, the path and the failure memory be pinned exactly.
 */
class BotTraceTest {

	private static BotTrace trace(int capacity) {
		return new BotTrace(capacity);
	}

	private static List<String> names(BotTrace trace, int n) {
		java.util.List<String> out = new java.util.ArrayList<String>();
		for (Event event : trace.history(n)) {
			out.add(event.name() + ":" + event.kind());
		}
		return out;
	}

	@Test
	void recordsEnterAndOutcomeInOrder() {
		BotTrace trace = trace(8);

		trace.entered("Sequence", 0);
		trace.entered("WalkTo", 0);
		trace.outcome("WalkTo", Kind.SUCCESS, 3, 3);

		assertEquals(java.util.Arrays.asList("Sequence:ENTER", "WalkTo:ENTER", "WalkTo:SUCCESS"),
				names(trace, 10));
		assertEquals(3, trace.recordedCount());
	}

	@Test
	void theRingOverwritesTheOldestNotTheNewest() {
		BotTrace trace = trace(3);

		trace.entered("a", 0);
		trace.entered("b", 0);
		trace.entered("c", 0);
		trace.entered("d", 0);

		// A fresh event is always visible; only the oldest is lost.
		assertEquals(java.util.Arrays.asList("b:ENTER", "c:ENTER", "d:ENTER"), names(trace, 10));
		assertEquals(4, trace.recordedCount(), "the count is lifetime, not ring size");
	}

	@Test
	void historyReturnsAtMostWhatIsAskedForAndNeverMoreThanWasRecorded() {
		BotTrace trace = trace(8);
		trace.entered("a", 0);
		trace.entered("b", 0);

		assertEquals(2, trace.history(10).size());
		assertEquals(1, trace.history(1).size());
		assertEquals("b:ENTER", trace.history(1).get(0).name() + ":"
				+ trace.history(1).get(0).kind());
		assertTrue(trace.history(0).isEmpty());
	}

	@Test
	void thePathIsTheEntersNotYetMatchedByAnOutcome() {
		BotTrace trace = trace(8);

		trace.entered("Repeat(forever)", 0);
		trace.entered("Sequence(2)", 0);
		trace.entered("Gather(tree)", 0);
		assertEquals(java.util.Arrays.asList("Repeat(forever)", "Sequence(2)", "Gather(tree)"),
				trace.path());

		trace.outcome("Gather(tree)", Kind.SUCCESS, 5, 5);
		assertEquals(java.util.Arrays.asList("Repeat(forever)", "Sequence(2)"), trace.path());

		trace.outcome("Sequence(2)", Kind.SUCCESS, 6, 6);
		trace.outcome("Repeat(forever)", Kind.SUCCESS, 7, 7);
		assertTrue(trace.path().isEmpty(), "nothing is current once the root has reported");
	}

	@Test
	void aMismatchedOutcomeUnwindsToTheNearestMatchRatherThanCorruptingThePath() {
		BotTrace trace = trace(8);
		trace.entered("root", 0);
		trace.entered("a", 0);
		trace.entered("b", 0);

		// "a" reported without "b" having reported: a wrong path is worse than a short one, so the
		// stack unwinds to "a" and "b" goes with it.
		trace.outcome("a", Kind.FAILURE, 9, 9);

		assertEquals(java.util.Arrays.asList("root"), trace.path());
	}

	@Test
	void pathLineIsReadableAndSaysIdleWhenNothingIsCurrent() {
		BotTrace trace = trace(8);
		assertEquals("(idle)", trace.pathLine());

		trace.entered("Repeat(forever)", 0);
		trace.entered("Gather(tree)", 0);
		assertEquals("Repeat(forever) > Gather(tree)", trace.pathLine());
	}

	@Test
	void lastFailureSurvivesTheRingOverwritingIt() {
		BotTrace trace = trace(2);
		trace.entered("WalkTo", 0);
		trace.outcome("WalkTo", Kind.FAILURE, 40, 40);
		assertNotNull(trace.lastFailure());
		assertEquals("WalkTo", trace.lastFailure().name());

		// Push enough events to evict the failure entirely; it is still the thing you need to see.
		for (int i = 0; i < 10; i++) {
			trace.entered("x" + i, 41);
		}
		assertNotNull(trace.lastFailure(), "a failure is remembered after it leaves the ring");
	}

	@Test
	void aSuccessDoesNotReplaceTheRememberedFailure() {
		BotTrace trace = trace(8);
		trace.entered("WalkTo", 0);
		trace.outcome("WalkTo", Kind.FAILURE, 40, 40);
		trace.entered("Bank", 41);
		trace.outcome("Bank", Kind.SUCCESS, 2, 43);

		assertEquals("WalkTo", trace.lastFailure().name());
	}

	@Test
	void aNoteIsAttachedToTheNextEventAndOnlyOnce() {
		BotTrace trace = trace(8);
		trace.entered("Gather(tree)", 0);
		trace.note("no tree within 8 tiles");
		trace.outcome("Gather(tree)", Kind.FAILURE, 41, 41);

		assertEquals("no tree within 8 tiles", trace.history(1).get(0).note());

		// The note is consumed: a later outcome must not inherit a stale explanation.
		trace.entered("Gather(tree)", 42);
		trace.outcome("Gather(tree)", Kind.SUCCESS, 3, 45);
		assertNull(trace.history(1).get(0).note());
	}

	@Test
	void describeTellsTheStoryInOneLine() {
		BotTrace trace = trace(8);
		trace.entered("Repeat(forever)", 0);
		trace.entered("Gather(tree)", 0);
		trace.note("tried 8 tree that yielded nothing");
		trace.outcome("Gather(tree)", Kind.FAILURE, 41, 41);

		Event enter = trace.history(3).get(0);
		Event failure = trace.history(1).get(0);

		assertEquals("t=0 Repeat(forever)", enter.describe());
		assertEquals("t=41 Gather(tree) -> FAILURE (after 41t, tried 8 tree that yielded nothing)",
				failure.describe());
	}

	@Test
	void describeOmitsTheReasonWhenThereIsNone() {
		BotTrace trace = trace(8);
		trace.entered("Bank", 0);
		trace.outcome("Bank", Kind.ABORT, 2, 2);

		assertEquals("t=2 Bank -> ABORT (after 2t)", trace.history(1).get(0).describe());
	}
}
