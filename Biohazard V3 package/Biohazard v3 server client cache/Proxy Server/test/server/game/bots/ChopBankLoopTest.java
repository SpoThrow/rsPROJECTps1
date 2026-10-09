package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import server.event.CycleEventHandler;
import server.game.bots.composite.Repeat;
import server.game.bots.composite.Sequence;
import server.game.bots.states.BankLogs;
import server.game.bots.states.ChopTree;
import server.game.bots.states.WalkTo;
import server.game.players.PlayerHandler;

/**
 * Slice-1 step 7: the whole vertical slice, still with no network.
 *
 * <p>One bot, one tree, one bank tile. The real per-player tick is driven directly
 * ({@code PlayerHandler.process()} plus {@code CycleEventHandler.process()}), so walking,
 * chopping and banking all run through the server's own code paths.
 */
class ChopBankLoopTest {

	private static final String NAME = "botloop";

	private static final int TREE_ID = 1276, LOG_ID = 1511, AXE = 1351;
	private static final int TREE_X = 3200, TREE_Y = 3200;
	private static final int BANK_X = 3210, BANK_Y = 3200;

	/** Generous, but finite: it guards against a state that returns RUNNING forever. */
	private static final int TICK_CAP = 20000;

	@AfterEach
	void tearDown() throws IOException {
		BotTestFixture.cleanUp(NAME);
	}

	@Test
	void aBotChopsToAFullBagWalksToTheBankAndBanksTheLogs() {
		BotPlayer bot = BotTestFixture.possess(NAME);
		BotTestFixture.teleport(bot, TREE_X + 1, TREE_Y);
		bot.getItems().addItem(AXE, 1);
		bot.skills.playerLevel[8] = 99; // a fast, deterministic chop timer

		BotState root = new Repeat(new Sequence(
				new WalkTo(TREE_X, TREE_Y, 3),
				new ChopTree(TREE_ID, LOG_ID, TREE_X, TREE_Y),
				new WalkTo(BANK_X, BANK_Y, 1),
				new BankLogs(LOG_ID)), -1);
		bot.attach(new BotController(bot, root));

		PlayerHandler handler = new PlayerHandler();
		int ticks = 0;
		for (; ticks < TICK_CAP && bankedLogs(bot) == 0; ticks++) {
			handler.process();
			CycleEventHandler.process();
		}

		assertTrue(bankedLogs(bot) > 0,
				"logs reached the bank — the whole walk/chop/walk/bank loop ran (ticks=" + ticks + ")");
		assertFalse(bot.getItems().playerHasItem(LOG_ID), "and every log was deposited");
		assertTrue(ticks < TICK_CAP, "the loop completed inside the cap");
	}

	private static int bankedLogs(BotPlayer bot) {
		int total = 0;
		for (int i = 0; i < bot.bankItems.length; i++) {
			if (bot.bankItems[i] - 1 == LOG_ID) {
				total += bot.bankItemsN[i];
			}
		}
		return total;
	}
}
