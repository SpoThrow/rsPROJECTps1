package server.game.bots.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.BotTrace;
import server.game.bots.FakeBotContext;

/**
 * Roadmap Phase F's acceptance criterion, from the author's side: <b>a script built with the fluent
 * builder is traced end to end, with no state class and no composite knowing a trace exists.</b>
 *
 * <p>This is the pairing that makes tracing work. The builder assembles the tree bottom-up, so wrapping
 * each step as it becomes a node means the terminal's {@code Sequence} receives already-reporting
 * children and is wrapped in turn — a complete path, obtained without a generic child accessor and
 * without one line of tracing inside {@code Sequence}, {@code Selector} or {@code Repeat}.
 *
 * <p>Only {@code walkTo} steps are used here. They are the one step that needs nothing from the world,
 * and the point is the <em>tracing</em>, not the leaves: {@code gather} would want a live
 * {@code ResourceScan} and {@code bankAll} a real client, and a test that had to fake those would be
 * asserting on the fake rather than on the trace.
 */
class ScriptTraceTest {

	private static ScriptBuilder builder(String name) {
		return BotScript.named(name);
	}

	private static List<String> names(FakeBotContext ctx) {
		List<String> out = new ArrayList<String>();
		for (BotTrace.Event event : ctx.trace().history(20)) {
			out.add(event.name());
		}
		return out;
	}

	@Test
	void wrappingIsTransparentSoRootStillReadsAsTheCompositeTheTerminalBuilt() {
		BotScript script = builder("trace").walkTo(3210, 3200, 1).forever();

		assertEquals("Repeat(forever)", script.root().name());
		assertEquals("Repeat(3)", builder("t").walkTo(1, 2, 1).times(3).root().name());
		assertEquals("Sequence(1)", builder("o").walkTo(1, 2, 1).once().root().name());
	}

	@Test
	void everyStepAndTheCompositeAroundItAppearInTheTrace() {
		BotScript script = builder("traced").walkTo(3210, 3200, 1).walkTo(3220, 3200, 1).once();
		FakeBotContext ctx = new FakeBotContext();
		BotState root = script.root();

		ctx.onTick();
		root.enter(ctx);
		root.tick(ctx);

		// The whole story: the composite entered, the first step entered and succeeded, the second
		// step entered and succeeded, the composite succeeded.
		assertEquals(Arrays.asList(
				"Sequence(2)",
				"WalkTo(3210,3200)", "WalkTo(3210,3200)",
				"WalkTo(3220,3200)", "WalkTo(3220,3200)",
				"Sequence(2)"), names(ctx));
		assertEquals(BotTrace.Kind.SUCCESS, ctx.trace().history(1).get(0).kind());
		assertEquals("(idle)", ctx.trace().pathLine(), "everything finished in one tick");
	}

	@Test
	void thePathShowsTheStepThatIsCurrentlyRunning() {
		BotScript script = builder("path").walkTo(3210, 3200, 1).once();
		// Not arrived, so the walk stays current instead of succeeding on the first tick.
		FakeBotContext ctx = new FakeBotContext().arrived(false);
		BotState root = script.root();

		ctx.onTick();
		root.enter(ctx);
		// Entering a sequence enters its first step, so the path already names what is about to run.
		assertEquals("Sequence(1) > WalkTo(3210,3200)", ctx.trace().pathLine());

		assertEquals(BotStatus.RUNNING, root.tick(ctx));
		assertEquals("Sequence(1) > WalkTo(3210,3200)", ctx.trace().pathLine());
	}

	@Test
	void aForeverLoopReEntersItsChildForTheNextCycleAndThePathSaysSo() {
		BotScript script = builder("cycle").walkTo(1, 2, 1).walkTo(3, 4, 1).forever();
		FakeBotContext ctx = new FakeBotContext();
		BotState root = script.root();

		ctx.onTick();
		root.enter(ctx);
		assertEquals(BotStatus.RUNNING, root.tick(ctx));

		// The steps finished, so the repeat re-entered the sequence for the next run: the path is the
		// live nesting, which is exactly what "why is my bot doing this" needs answered.
		assertEquals("Repeat(forever) > Sequence(2) > WalkTo(1,2)", ctx.trace().pathLine());
	}

	@Test
	void theTraceIsTheBotsOwnSoTwoBotsDoNotShareHistory() {
		BotScript script = builder("sharedtrace").walkTo(3210, 3200, 1).once();
		FakeBotContext first = new FakeBotContext();
		FakeBotContext second = new FakeBotContext();

		// The same script mints a separate tree per bot, and each tree reports to its own bot's trace.
		first.onTick();
		script.root().enter(first);

		assertEquals(2, first.trace().recordedCount(), "the composite and the step it entered");
		assertEquals(0, second.trace().recordedCount(), "a second bot starts with an empty history");
		assertTrue(names(first).contains("Sequence(1)"), names(first).toString());
	}
}
