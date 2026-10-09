package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import server.game.bots.composite.Selector;
import server.game.bots.composite.Sequence;
import server.game.bots.condition.BankOpen;
import server.game.bots.condition.HasItem;
import server.game.bots.condition.InventoryFull;
import server.game.bots.condition.IsDead;
import server.game.bots.condition.SkillAtLeast;
import server.game.bots.condition.WithinRange;
import server.game.bots.decorator.Invert;

/**
 * Roadmap Phase B: the condition leaves, checked against a real possessed bot.
 *
 * <p>These run through {@link PlayerBotContext} rather than a fake one on purpose. Three of
 * the six read the client directly, so a fake would prove only that the fake agrees with
 * itself; going through possession exercises the same object the tree does when it runs, and
 * the fixture is the one slice 1 already established for testing states against a live world
 * character.
 */
class ConditionStatesTest {

	private static final String NAME = "botcond";

	private static final int LOG_ID = 1511;
	private static final int COINS = 995;
	private static final int WOODCUTTING = 8;

	/** The tile every position assertion is measured from. */
	private static final int HOME_X = 3200, HOME_Y = 3200;

	@AfterEach
	void tearDown() throws IOException {
		BotTestFixture.cleanUp(NAME);
	}

	@Test
	void hasItemAsksTheInventory() {
		BotPlayer bot = BotTestFixture.possess(NAME);
		bot.getItems().addItem(LOG_ID, 1);
		PlayerBotContext ctx = new PlayerBotContext(bot);

		assertEquals(BotStatus.SUCCESS, run(new HasItem(LOG_ID), ctx));
		assertEquals(BotStatus.FAILURE, run(new HasItem(COINS), ctx), "an item never held is not held");
	}

	@Test
	void inventoryFullBecomesTrueOnlyWhenTheSlotsRunOut() {
		BotPlayer bot = BotTestFixture.possess(NAME);
		PlayerBotContext ctx = new PlayerBotContext(bot);

		assertEquals(BotStatus.FAILURE, run(new InventoryFull(), ctx), "a fresh bot has room");

		fillInventory(bot);

		assertEquals(BotStatus.SUCCESS, run(new InventoryFull(), ctx));
	}

	@Test
	void withinRangeMeasuresTheSameDistanceTheWalkingStateDoes() {
		BotPlayer bot = BotTestFixture.possess(NAME);
		BotTestFixture.teleport(bot, HOME_X, HOME_Y);
		PlayerBotContext ctx = new PlayerBotContext(bot);

		assertEquals(BotStatus.SUCCESS, run(new WithinRange(HOME_X, HOME_Y, 0), ctx));
		assertEquals(BotStatus.SUCCESS, run(new WithinRange(HOME_X + 1, HOME_Y, 1), ctx));
		assertEquals(BotStatus.FAILURE, run(new WithinRange(HOME_X + 10, HOME_Y, 1), ctx));
	}

	@Test
	void skillAtLeastComparesTheLevelTable() {
		BotPlayer bot = BotTestFixture.possess(NAME);
		bot.skills.playerLevel[WOODCUTTING] = 60;
		PlayerBotContext ctx = new PlayerBotContext(bot);

		assertEquals(BotStatus.SUCCESS, run(new SkillAtLeast(WOODCUTTING, 60), ctx), "equal is enough");
		assertEquals(BotStatus.FAILURE, run(new SkillAtLeast(WOODCUTTING, 61), ctx));
	}

	@Test
	void skillAtLeastFailsRatherThanThrowingOnAnIndexOutsideTheTable() {
		BotPlayer bot = BotTestFixture.possess(NAME);
		PlayerBotContext ctx = new PlayerBotContext(bot);

		// Authored data can be wrong; an exception here would escape into the game tick, so an
		// unsatisfiable question has to be an answer.
		assertEquals(BotStatus.FAILURE, run(new SkillAtLeast(-1, 1), ctx));
		assertEquals(BotStatus.FAILURE, run(new SkillAtLeast(25, 1), ctx));
	}

	@Test
	void bankOpenReadsTheBankFlag() {
		BotPlayer bot = BotTestFixture.possess(NAME);
		PlayerBotContext ctx = new PlayerBotContext(bot);

		assertEquals(BotStatus.FAILURE, run(new BankOpen(), ctx));

		bot.isBanking = true;

		assertEquals(BotStatus.SUCCESS, run(new BankOpen(), ctx));
	}

	@Test
	void isDeadReadsTheDeathFlag() {
		BotPlayer bot = BotTestFixture.possess(NAME);
		PlayerBotContext ctx = new PlayerBotContext(bot);

		assertEquals(BotStatus.FAILURE, run(new IsDead(), ctx));

		bot.isDead = true;

		assertEquals(BotStatus.SUCCESS, run(new IsDead(), ctx));
	}

	/**
	 * The policy the roadmap writes out, built from a condition rather than wired in code:
	 * {@code Selector(Sequence(not-full -> work), bank)}. This is the acceptance criterion for
	 * Phase B — the first succeeding alternative is the one that runs.
	 */
	@Test
	void theChopOrBankPolicyRunsTheFirstAlternativeThatSucceeds() {
		BotPlayer bot = BotTestFixture.possess(NAME);
		PlayerBotContext ctx = new PlayerBotContext(bot);
		List<String> ran = new ArrayList<String>();
		BotState work = new Recorder("work", ran);
		BotState bank = new Recorder("bank", ran);

		Selector policy = new Selector(
				new Sequence(new Invert(new InventoryFull()), work),
				bank);

		// Room in the bag: the first alternative applies and the fallback is never reached.
		assertEquals(BotStatus.SUCCESS, run(policy, ctx));
		assertEquals(List.of("work"), ran);

		fillInventory(bot);
		ran.clear();

		// Full: the guard fails, so the selector moves on to banking.
		assertEquals(BotStatus.SUCCESS, run(policy, ctx));
		assertEquals(List.of("bank"), ran);
	}

	/** Enters a state, ticks it once and returns what it reported. */
	private static BotStatus run(BotState state, PlayerBotContext ctx) {
		state.enter(ctx);
		return state.tick(ctx);
	}

	/** A leaf that reports SUCCESS and records that it ran. */
	private static final class Recorder implements BotState {

		private final String label;
		private final List<String> journal;

		Recorder(String label, List<String> journal) {
			this.label = label;
			this.journal = journal;
		}

		@Override
		public void enter(BotContext ctx) {
			journal.add(label);
		}

		@Override
		public BotStatus tick(BotContext ctx) {
			return BotStatus.SUCCESS;
		}

		@Override
		public void exit(BotContext ctx, boolean interrupted) {
		}
	}

	/** Fills every inventory slot, using distinct ids so nothing stacks. */
	private static void fillInventory(BotPlayer bot) {
		// Distinct ids, because a repeated id would just stack in the slot it already fills.
		int itemId = 1;
		while (bot.getItems().freeSlots() > 0 && itemId < 9000) {
			bot.getItems().addItem(itemId++, 1);
		}
	}
}
