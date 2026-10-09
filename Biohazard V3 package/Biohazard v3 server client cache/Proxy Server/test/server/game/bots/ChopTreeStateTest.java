package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import server.event.CycleEventHandler;
import server.game.bots.states.ChopTree;
import server.game.players.actions.objects.ObjectClick;
import server.game.players.actions.objects.ObjectHandler;

/**
 * Slice-1 step 6: chopping must go through the object registry, never a direct skill call.
 */
class ChopTreeStateTest {

	private static final String NAME = "botchop";

	private static final int TREE_ID = 1276;   // a normal tree
	private static final int LOG_ID = 1511;    // logs
	private static final int AXE = 1351;       // bronze axe
	private static final int TREE_X = 3200;
	private static final int TREE_Y = 3200;

	@AfterEach
	void tearDown() throws IOException {
		BotTestFixture.cleanUp(NAME);
	}

	private static BotPlayer chopper(boolean withAxe) {
		BotPlayer bot = BotTestFixture.possess(NAME);
		BotTestFixture.teleport(bot, TREE_X + 1, TREE_Y);
		if (withAxe) {
			bot.getItems().addItem(AXE, 1);
		}
		return bot;
	}

	@Test
	void theRegistryStartsTheSessionAndTheEntryIsPinned() {
		BotPlayer bot = chopper(true);
		PlayerBotContext ctx = new PlayerBotContext(bot);

		ChopTree chop = new ChopTree(TREE_ID, LOG_ID, TREE_X, TREE_Y);
		chop.enter(ctx);

		assertTrue(bot.woodcutting.active,
				"the ObjectHandler registry started the skill, not a direct Woodcutting call");
		assertTrue(ObjectHandler.isRegistered(TREE_ID, ObjectClick.FIRST),
				"regression pin: if the registry entry is removed, this must fail loudly");
	}

	@Test
	void choppingFillsTheBagAndThenSucceeds() {
		BotPlayer bot = chopper(true);
		bot.skills.playerLevel[8] = 99; // a fast, deterministic timer
		PlayerBotContext ctx = new PlayerBotContext(bot);

		ChopTree chop = new ChopTree(TREE_ID, LOG_ID, TREE_X, TREE_Y);
		chop.enter(ctx);

		BotStatus status = BotStatus.RUNNING;
		for (int i = 0; i < 5000 && status == BotStatus.RUNNING; i++) {
			CycleEventHandler.process();
			status = chop.tick(ctx);
		}

		assertEquals(BotStatus.SUCCESS, status, "a full bag ends the chop");
		assertEquals(0, ctx.freeSlots(), "the bag is full");
		assertTrue(ctx.hasItem(LOG_ID), "logs were produced");

		// The skill's own event stops once there is no room, which ends the session.
		for (int i = 0; i < 5 && bot.woodcutting.active; i++) {
			CycleEventHandler.process();
		}
		assertFalse(bot.woodcutting.active, "the chop session ends when the bag is full");
	}

	@Test
	void withoutAnAxeTheStateFailsInsteadOfSpinning() {
		BotPlayer bot = chopper(false);
		PlayerBotContext ctx = new PlayerBotContext(bot);

		ChopTree chop = new ChopTree(TREE_ID, LOG_ID, TREE_X, TREE_Y);
		chop.enter(ctx);

		BotStatus status = BotStatus.RUNNING;
		for (int i = 0; i < 10 && status == BotStatus.RUNNING; i++) {
			status = chop.tick(ctx);
		}

		assertEquals(BotStatus.FAILURE, status, "no axe is a failure, not an infinite RUNNING");
		assertFalse(bot.woodcutting.active);
	}
}
