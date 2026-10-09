package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import server.Config;

/**
 * The per-tick work budget — {@code BOT_ROADMAP.md} §5.7, roadmap Phase H's acceptance criterion.
 *
 * <p><b>The claim being tested:</b> N bots run within a bounded per-tick cost. The game tick is
 * single-threaded and shared with real players, so "bounded" means every tick ticks at most
 * {@link Config#BOT_TICK_BUDGET} behaviour trees, and — the half that is easy to get wrong — <b>no bot is
 * starved by that bound</b>: each one is reached within one rotation.
 *
 * <p><b>Tested two ways, because they fail differently.</b> The rotation arithmetic is proven
 * exhaustively at hundreds of bots, which no test could afford to build as live {@code BotPlayer}s with
 * real character files; the wiring is then proven end to end against a handful of real bots, which is
 * what catches a scheduler that is correct in isolation but never consulted. A test that only did the
 * second would never reach the bound; a test that only did the first would not notice the bound never
 * being applied.
 *
 * <p>The shipped budget (32) exceeds {@code BotManager.MAX_BOTS} (10), so at the current cap it never
 * bites and every bot ticks every tick — asserted below as a property, because that is the behaviour
 * existing deployments must keep. To exercise the throttle, the budget is shrunk through the test seam.
 */
class BotSchedulingTest {

	private static final String PASSWORD = BotTestFixture.PASSWORD;

	/** Login-legal, and short enough for the 12-char cap. */
	private static final String[] NAMES = { "botsch1", "botsch2", "botsch3", "botsch4", "botsch5" };

	@AfterEach
	void tearDown() throws IOException {
		BotManager.setTickBudgetForTesting(0); // restores the configured value
		BotTestFixture.cleanUp(NAMES);
	}

	/** Counts what a scheduler does to it, so a test can assert a tree did or did not run. */
	private static final class Counting implements BotState {
		int ticks;
		int enters;
		int exits;
		boolean lastExitInterrupted;

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
			lastExitInterrupted = interrupted;
		}

		@Override
		public String name() {
			return "Counting";
		}
	}

	private static Set<Integer> allowed(int size, int budget, long tick) {
		Set<Integer> out = new HashSet<Integer>();
		for (int i = 0; i < size; i++) {
			if (BotManager.inTickWindow(i, size, budget, tick)) {
				out.add(i);
			}
		}
		return out;
	}

	// ---- the rotation arithmetic, proven at scale ---------------------------------------------

	@Test
	void whenEveryBotFitsInTheBudgetTheyAllActEveryTick() {
		// The property the current cap depends on: MAX_BOTS (10) is well under the budget (32), so the
		// budget must be completely inert. If this ever fails, the budget has started throttling a
		// deployment that never needed throttling.
		assertTrue(BotManager.MAX_BOTS <= Config.BOT_TICK_BUDGET,
				"the shipped budget must not throttle the shipped cap");

		for (long tick = 1; tick <= 50; tick++) {
			for (int i = 0; i < BotManager.MAX_BOTS; i++) {
				assertTrue(BotManager.inTickWindow(i, BotManager.MAX_BOTS, Config.BOT_TICK_BUDGET, tick),
						"bot " + i + " on tick " + tick);
			}
		}
	}

	@Test
	void whenBotsOutnumberTheBudgetExactlyTheBudgetActEachTick() {
		int size = 100;
		int budget = 7;

		for (long tick = 1; tick <= 40; tick++) {
			// Exactly the budget, not "at most": the window is a fixed size, so the bound is tight and a
			// tick that under-performs is silently losing throughput.
			assertEquals(budget, allowed(size, budget, tick).size(), "tick " + tick);
		}
	}

	@Test
	void everyBotIsReachedWithinOneRotationWhateverTickItStartsOn() {
		int size = 100;
		int budget = 7;
		int period = (size + budget - 1) / budget; // 15 ticks

		// Start from many different ticks: the cursor is cumulative in production, so the guarantee must
		// hold from any phase, not just from tick 1.
		for (long start = 1; start <= 30; start++) {
			for (int i = 0; i < size; i++) {
				boolean reached = false;
				for (long tick = start; tick < start + period && !reached; tick++) {
					reached = BotManager.inTickWindow(i, size, budget, tick);
				}
				assertTrue(reached, "bot " + i + " starved when the rotation began at tick " + start);
			}
		}
	}

	@Test
	void theWindowMovesBetweenTicksRatherThanBeingAFixedSet() {
		// If the window were fixed, the bound would still hold but the same bots would be permanently
		// excluded — the starvation mode the rotation exists to avoid.
		assertNotEquals(allowed(100, 7, 5), allowed(100, 7, 6),
				"the exempted set must change from tick to tick");
	}

	@Test
	void aNonPositiveBudgetDisablesTheThrottleEntirely() {
		for (int i = 0; i < 100; i++) {
			assertTrue(BotManager.inTickWindow(i, 100, 0, 3), "budget 0 means unlimited");
			assertTrue(BotManager.inTickWindow(i, 100, -1, 3), "a negative budget means unlimited");
		}
	}

	@Test
	void theBoundHoldsAcrossAQuarterOfAMillionBotTicks() {
		// The stress claim, in the only form that is both honest and affordable: 200 bots over 1250 ticks
		// (a quarter of a million would-be tree ticks) never exceed the budget on any tick, and every bot
		// acts each rotation. Asserted inside the loop so a single bad tick fails with its number.
		int size = 200;
		int budget = 8;
		int period = (size + budget - 1) / budget; // 25

		int worstTick = 0;
		for (int round = 0; round < 50; round++) {
			boolean[] seen = new boolean[size];
			for (long tick = round * period + 1; tick <= (round + 1) * period; tick++) {
				int acted = 0;
				for (int i = 0; i < size; i++) {
					if (BotManager.inTickWindow(i, size, budget, tick)) {
						acted++;
						seen[i] = true;
					}
				}
				worstTick = Math.max(worstTick, acted);
				assertTrue(acted <= budget, "tick " + tick + " acted " + acted + " times, budget " + budget);
			}
			for (int i = 0; i < size; i++) {
				assertTrue(seen[i], "bot " + i + " did not act at all in round " + round);
			}
		}
		assertEquals(budget, worstTick, "the bound is tight, not merely respected");
	}

	// ---- the gate on a single bot --------------------------------------------------------------

	@Test
	void aBotTheBudgetExemptedIsDeferredRatherThanTicked() {
		BotPlayer bot = BotTestFixture.possess(NAMES[0]);
		Counting tree = new Counting();
		bot.attach(new BotController(bot, tree));

		// beginTick() first: it resets the counters and sets the flag from the live roster, so the manual
		// exemption below must come after it or the scheduler would overwrite it.
		BotManager.beginTick();
		bot.treeTickAllowed = false;
		int deferredBefore = BotManager.deferredLastTick();

		BotManager.tickTree(bot);

		assertEquals(0, tree.ticks, "an exempted bot's tree must not run");
		assertEquals(deferredBefore + 1, BotManager.deferredLastTick(), "and the deferral is counted");
	}

	@Test
	void aBotTheBudgetAdmittedTicksAndIsCounted() {
		BotPlayer bot = BotTestFixture.possess(NAMES[0]);
		Counting tree = new Counting();
		bot.attach(new BotController(bot, tree));

		BotManager.beginTick();
		BotManager.tickTree(bot);

		assertEquals(1, tree.ticks);
		assertEquals(1, BotManager.tickedLastTick(), "a performed tick is counted, not just a decision");
	}

	@Test
	void aPossessedButIdleBotIsNeitherTickedNorCountedAsDeferred() {
		// A bot with no controller is a valid state (possessed but idle). It must not inflate the deferral
		// count, which is an operator's signal that the budget is biting.
		BotPlayer bot = BotTestFixture.possess(NAMES[0]);
		BotManager.beginTick();
		bot.treeTickAllowed = false;
		int deferredBefore = BotManager.deferredLastTick();

		BotManager.tickTree(bot);

		assertEquals(deferredBefore, BotManager.deferredLastTick(), "no controller is not a deferral");
	}

	// ---- end to end, with real bots ------------------------------------------------------------

	@Test
	void withTheShippedBudgetEveryLiveBotTicksEveryTick() {
		List<BotPlayer> bots = new ArrayList<BotPlayer>();
		List<Counting> trees = new ArrayList<Counting>();
		for (int i = 0; i < 3; i++) {
			BotPlayer bot = BotTestFixture.possess(NAMES[i]);
			Counting tree = new Counting();
			bot.attach(new BotController(bot, tree));
			bots.add(bot);
			trees.add(tree);
		}
		assertEquals(3, BotManager.count(), "no other bot should be live during this test");

		BotManager.beginTick();
		for (BotPlayer bot : bots) {
			BotManager.tickTree(bot);
		}

		assertEquals(3, BotManager.tickedLastTick(), "all three acted");
		assertEquals(0, BotManager.deferredLastTick(), "and the budget deferred none of them");
		for (Counting tree : trees) {
			assertEquals(1, tree.ticks);
		}
	}

	@Test
	void aBudgetBelowThePopulationBoundsEachTickAndStillReachesEveryBot() {
		// The end-to-end form of the stress claim, at a scale character files allow: five real bots with a
		// budget of two. Each tick must act on exactly two, and the rotation must cover all five within
		// three ticks — so the throttle bounds the tick without starving anyone.
		List<BotPlayer> bots = new ArrayList<BotPlayer>();
		List<Counting> trees = new ArrayList<Counting>();
		for (String name : NAMES) {
			BotPlayer bot = BotTestFixture.possess(name);
			Counting tree = new Counting();
			bot.attach(new BotController(bot, tree));
			bots.add(bot);
			trees.add(tree);
		}
		assertEquals(bots.size(), BotManager.count(), "no other bot should be live during this test");

		int budget = 2;
		int period = (bots.size() + budget - 1) / budget; // 3
		BotManager.setTickBudgetForTesting(budget);

		boolean[] everActed = new boolean[bots.size()];
		for (int tick = 0; tick < period; tick++) {
			int[] before = new int[bots.size()];
			for (int i = 0; i < bots.size(); i++) {
				before[i] = trees.get(i).ticks;
			}

			BotManager.beginTick();
			for (BotPlayer bot : bots) {
				BotManager.tickTree(bot);
			}

			assertEquals(budget, BotManager.tickedLastTick(), "tick " + tick + " must act on exactly the budget");
			assertEquals(bots.size() - budget, BotManager.deferredLastTick(),
					"tick " + tick + " must defer the rest");

			for (int i = 0; i < bots.size(); i++) {
				if (trees.get(i).ticks > before[i]) {
					everActed[i] = true;
				}
			}
		}
		for (int i = 0; i < bots.size(); i++) {
			assertTrue(everActed[i], "bot " + i + " was starved across " + period + " ticks");
		}
	}

	// ---- cohort lifecycle ---------------------------------------------------------------------

	@Test
	void stopAllStopsEveryTreeWithCleanupAndFreesEverySlot() {
		List<BotPlayer> bots = new ArrayList<BotPlayer>();
		List<Counting> trees = new ArrayList<Counting>();
		for (int i = 0; i < 3; i++) {
			BotPlayer bot = BotTestFixture.possess(NAMES[i]);
			Counting tree = new Counting();
			bot.attach(new BotController(bot, tree));
			bots.add(bot);
			trees.add(tree);
		}

		assertEquals(3, BotManager.stopAll());

		assertEquals(0, BotManager.count(), "every bot is released");
		assertEquals(0, BotManager.stopAll(), "and a second stop finds nothing to do");
		for (int i = 0; i < bots.size(); i++) {
			Counting tree = trees.get(i);
			assertEquals(1, tree.exits, "the tree was exited exactly once");
			assertTrue(tree.lastExitInterrupted,
					"interrupted/cleanup exit, so charges and CycleEvents are released before the save");
			assertTrue(bots.get(i).isCharacterSaved(), "and the character was written before the slot freed");
		}
	}

	@Test
	void spawnAllCountsOnlyTheBotsThatActuallyStarted() {
		List<BotProfile> rows = BotsConfig.parseLines(Arrays.asList(
				"account " + NAMES[0] + " password " + PASSWORD + " script gather_oak",
				"account " + NAMES[1] + " password " + PASSWORD + " script gather_oak"))
				.profiles();

		assertEquals(2, BotManager.spawnAll(rows), "two new rows start two bots");
		// spawn() is idempotent and returns an already-live bot, so a naive sum would report two more
		// spawns here. The cohort count must not.
		assertEquals(0, BotManager.spawnAll(rows), "a second cohort pass starts nothing and says so");
		assertEquals(2, BotManager.count());
	}

	@Test
	void releasingABotBetweenTicksRebuildsTheWindowWithoutError() {
		// A roster change invalidates every index the previous window was computed from. The scheduler
		// must rebuild rather than act on a stale index — and must not throw when it does.
		BotPlayer first = BotTestFixture.possess(NAMES[0]);
		first.attach(new BotController(first, new Counting()));
		BotTestFixture.possess(NAMES[1]);

		BotManager.setTickBudgetForTesting(1);
		BotManager.beginTick();
		assertTrue(BotManager.release(NAMES[0]), "the first bot leaves");
		BotManager.beginTick(); // rebuild the window with only one bot live

		assertEquals(1, BotManager.count());
		BotPlayer survivor = BotManager.all().get(0);
		assertTrue(survivor.treeTickAllowed,
				"one live bot and a budget of one means the survivor is always in the window");
	}
}
