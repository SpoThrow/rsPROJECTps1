package server.game.bots.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.FakeBotContext;
import server.game.bots.world.Location;
import server.game.bots.world.LocationKind;
import server.game.bots.world.Locations;

/**
 * The fluent builder — Phase D's "a gathering bot in one block".
 *
 * <p>Two properties matter more than the shape of the tree. First, that a step's <em>order</em> is the
 * order it was written in. Second, that {@code root()} mints a fresh tree every call: a registered
 * script is shared by every bot that names it, and a state holds progress, so a tree handed out twice
 * would have two bots overwriting each other's walk and target.
 */
class ScriptBuilderTest {

	private static final Location OAKS = new Location("oaks", LocationKind.TREE, 3200, 3200, 0, 4, 4,
			null);

	/** A curated world, so a walk resolves without loading Data/cfg. */
	private static Locations world() {
		return Locations.curated(Arrays.asList(OAKS));
	}

	private static ScriptBuilder builder(String name) {
		return BotScript.named(name, world(), null);
	}

	@Test
	void theStepsRunInTheOrderTheyWereWritten() {
		BotScript script = builder("ordered").walkTo(3210, 3200, 1).walkTo(3200, 3200, 3).once();
		BotState root = script.root();
		FakeBotContext ctx = new FakeBotContext();

		root.enter(ctx);
		BotStatus status = root.tick(ctx);

		assertEquals(BotStatus.SUCCESS, status);
		assertEquals(Arrays.asList("3210,3200", "3200,3200"), ctx.walks, "written order is run order");
	}

	@Test
	void gatherLoopWrapsItsStepsInAForeverRepeat() {
		BotScript script = builder("loop")
				.gatherLoop(LocationKind.TREE, 1511, LocationKind.BANK)
				.forever();

		// Sequence(4) inside a forever repeat — and nothing here needed a state class to be written,
		// which is the whole point of the phase. That the four are travel, gather, travel, bank in that
		// order is what the end-to-end script test proves on a real bot.
		assertEquals("Repeat(forever)", script.root().name());
	}

	@Test
	void eachTerminalWrapsTheStepsDifferently() {
		assertEquals("Repeat(forever)", builder("f").walkTo(1, 2, 1).forever().root().name());
		assertEquals("Repeat(3)", builder("t").walkTo(1, 2, 1).times(3).root().name());
		assertEquals("Sequence(1)", builder("o").walkTo(1, 2, 1).once().root().name());
	}

	@Test
	void rootMintsANewTreeEveryTimeBecauseOneScriptServesEveryBot() {
		BotScript script = builder("shared").walkTo(3210, 3200, 1).walkTo(3200, 3200, 3).forever();

		BotState first = script.root();
		BotState second = script.root();

		assertEquals(first.name(), second.name(), "the same shape");
		assertNotSame(first, second, "a state holds progress, so no two bots may share an instance");
	}

	@Test
	void theNameIsTheHandleAScriptIsReferredToBy() {
		assertEquals("gather_oak", builder("gather_oak").walkTo(1, 2, 1).once().name());
		assertEquals("BotScript(gather_oak)", builder("gather_oak").walkTo(1, 2, 1).once().toString());
	}

	@Test
	void aNameThatIsMissingOrBlankIsRejectedAtBuildTime() {
		assertThrows(IllegalArgumentException.class, () -> BotScript.named(null));
		assertThrows(IllegalArgumentException.class, () -> BotScript.named("   "));
	}

	@Test
	void aScriptWithNoStepsIsRejectedRatherThanSucceedingForever() {
		// A script that does nothing is far likelier to be a mistake than an intention.
		assertThrows(IllegalStateException.class, () -> builder("empty").forever());
		assertThrows(IllegalStateException.class, () -> builder("empty").times(2));
		assertThrows(IllegalStateException.class, () -> builder("empty").once());
	}

	@Test
	void aNullStepIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> builder("nullstep").step(null));
	}
}
