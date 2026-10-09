package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import server.event.CycleEventHandler;
import server.game.bots.script.BotScript;
import server.game.bots.world.Location;
import server.game.bots.world.LocationKind;
import server.game.bots.world.Locations;
import server.game.bots.world.ResourceScan;
import server.game.bots.world.ScannedLocator;
import server.game.objects.Objects;
import server.game.players.PlayerHandler;

/**
 * Phase D's acceptance criterion, run for real: <b>a gathering bot declared in one {@code BotScript}
 * block, with no new state class</b>, chopping and banking through the server's own code paths.
 *
 * <p>This is {@code ChopBankLoopTest} with the four states it names by hand replaced by one script
 * declaration — the difference the phase exists to make. The bot, the tree, the bank tile and the tick
 * loop are the same, because what changed is how the behaviour is <em>expressed</em>, not how it runs.
 *
 * <p>The world the script sees is injected (a curated {@code Locations} and a {@code ResourceScan} over
 * a fake region) so the test is about the script and not about {@code Data/cfg} or a loaded map. The
 * object it finds is the same oak, at the same tile, that the hand-written test chops, so the
 * interaction goes down the identical server path.
 */
class ScriptLoopTest {

	private static final String NAME = "botscript";

	private static final int TREE_ID = 1276, LOG_ID = 1511, AXE = 1351;
	private static final int TREE_X = 3200, TREE_Y = 3200;
	private static final int BANK_X = 3210, BANK_Y = 3200;

	/** Generous, but finite: it guards against a script that returns RUNNING forever. */
	private static final int TICK_CAP = 20000;

	@AfterEach
	void tearDown() throws IOException {
		BotTestFixture.cleanUp(NAME);
	}

	@Test
	void aGatheringBotDeclaredInOneScriptBlockChopsAndBanks() {
		BotPlayer bot = BotTestFixture.possess(NAME);
		BotTestFixture.teleport(bot, TREE_X + 1, TREE_Y);
		bot.getItems().addItem(AXE, 1);
		bot.skills.playerLevel[8] = 99; // a fast, deterministic chop timer

		// The script, in one block. Nothing above this line is a behaviour, and nothing below it is
		// either: the four things a woodcutter does are named, not implemented.
		BotScript script = BotScript.named("test_gather_oak", places(), scan())
				.gatherLoop(LocationKind.TREE, LOG_ID, LocationKind.BANK)
				.forever();
		bot.attach(new BotController(bot, script.root()));

		PlayerHandler handler = new PlayerHandler();
		int ticks = 0;
		for (; ticks < TICK_CAP && bankedLogs(bot) == 0; ticks++) {
			// This loop IS the game tick, so it signals the tick boundary the way Server.tick() does.
			// BotManager's per-tick budget is reset here; a loop that skipped this would be driving bots
			// outside the contract the server honours.
			BotManager.beginTick();
			handler.process();
			CycleEventHandler.process();
		}

		assertTrue(bankedLogs(bot) > 0,
				"the script gathered and banked (ticks=" + ticks + ")");
		assertFalse(bot.getItems().playerHasItem(LOG_ID), "and every log was deposited");
		assertTrue(ticks < TICK_CAP, "the script completed inside the cap");
	}

	/** A curated table naming where a tree and a bank are, which is all the script has to be given. */
	private static Locations places() {
		List<Location> authored = Arrays.asList(
				Location.point("test_tree", LocationKind.TREE, TREE_X, TREE_Y, 0),
				Location.point("test_bank", LocationKind.BANK, BANK_X, BANK_Y, 0));
		return Locations.curated(authored);
	}

	/** A world whose region at 3200,3200 holds one oak, which is the object the bot will click. */
	private static ResourceScan scan() {
		final List<Objects> objects = Arrays.asList(new Objects(TREE_ID, TREE_X, TREE_Y, 0, 0, 10));
		ScannedLocator.RegionSource source = new ScannedLocator.RegionSource() {
			@Override
			public List<Objects> objects(int baseX, int baseY) {
				return baseX == 3200 && baseY == 3200 ? objects : Collections.<Objects>emptyList();
			}
		};
		ScannedLocator.KindSource kinds = new ScannedLocator.KindSource() {
			@Override
			public String kindOf(int objectId) {
				return objectId == TREE_ID ? "tree" : null;
			}
		};
		return ResourceScan.with(source, kinds);
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
