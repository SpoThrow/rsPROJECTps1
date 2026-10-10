package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import server.event.CycleEventHandler;
import server.game.bots.script.BotScript;
import server.game.bots.script.ScriptDocument;
import server.game.bots.world.Location;
import server.game.bots.world.LocationKind;
import server.game.bots.world.Locations;
import server.game.bots.world.ResourceScan;
import server.game.bots.world.ScannedLocator;
import server.game.objects.Objects;
import server.game.players.PlayerHandler;

/**
 * Tooling stage T6's acceptance criterion: <b>the script the editor authored runs the slice-1 chop→bank
 * loop end to end, with no hand-written bot code.</b>
 *
 * <p>This is {@link ScriptLoopTest} with one difference that is the whole point: the behaviour comes from
 * {@code Data/cfg/bots/chop_and_bank.json} — the file the timeline editor wrote and committed — loaded
 * through the server's own {@link ScriptDocument}, rather than from a {@code BotScript} block built here.
 * Nothing between the file and the bank tells the bot what to do.
 *
 * <p><b>The world is injected, and that is the established pattern rather than a shortcut.</b> The two
 * leaves that touch the world resolve it lazily on first use — {@code WalkToNearest} through
 * {@code Locations.live()}, {@code Gather} through {@code ResourceScan.live()} — so installing a table and
 * a scan here is what lets a script authored against a live server run in a JVM with no {@code Data/world}
 * and no loaded regions. The bot, the tree, the bank and the tick loop are the real ones: the interaction
 * goes down the same {@code ObjectHandler}/skill path a player's click does, which is why this is a test of
 * the file and not of a fixture.
 *
 * <p><b>What it does not prove.</b> That the nodes resolve the <em>live</em> world. That is what
 * {@code ScriptLoopTest} and the in-game {@code ::bot reload} run cover; this one proves the file the tool
 * emits is a behaviour the runtime actually executes.
 */
class AuthoredScriptRunTest {

	private static final String NAME = "authoredloop";

	/** The committed example. Named here rather than discovered, so a rename fails loudly. */
	private static final String SCRIPT = "chop_and_bank";

	private static final int TREE_ID = 1276, LOG_ID = 1511, AXE = 1351;
	private static final int TREE_X = 3200, TREE_Y = 3200;
	private static final int BANK_X = 3210, BANK_Y = 3200;

	/** Generous, but finite: it guards against a script that returns RUNNING forever. */
	private static final int TICK_CAP = 20000;

	@AfterEach
	void tearDown() throws IOException {
		// Order matters: the bot is released while the world it may be walking in is still installed.
		BotTestFixture.cleanUp(NAME);
		Locations.uninstall();
		ResourceScan.uninstall();
	}

	@Test
	void theAuthoredScriptChopsAndBanksThroughTheRealServerPaths() throws IOException {
		BotScript script = loadAuthoredScript();

		// The file is the loop, not something that merely parses: the editor compiles a repeat around
		// the four steps, and that is what the runtime was handed.
		assertEquals("Repeat(forever)", script.root().name(),
				"chop_and_bank.json should compile to a forever-repeat of the gather loop");

		// Create the bot before installing the world, so this cannot accidentally depend on install
		// order: the leaves resolve their world on first tick, not at construction.
		BotPlayer bot = BotTestFixture.possess(NAME);
		BotTestFixture.teleport(bot, TREE_X + 1, TREE_Y);
		bot.getItems().addItem(AXE, 1);
		bot.skills.playerLevel[8] = 99; // a fast, deterministic chop timer

		installWorld();
		bot.attach(new BotController(bot, script.root()));

		PlayerHandler handler = new PlayerHandler();
		int ticks = 0;
		for (; ticks < TICK_CAP && bankedLogs(bot) == 0; ticks++) {
			// This loop IS the game tick, so it signals the tick boundary the way Server.tick() does
			// (BotManager's per-tick budget resets here).
			BotManager.beginTick();
			handler.process();
			CycleEventHandler.process();
		}

		assertTrue(bankedLogs(bot) > 0,
				"the authored script gathered and banked (ticks=" + ticks + ")");
		assertFalse(bot.getItems().playerHasItem(LOG_ID), "and every log was deposited");
		assertTrue(ticks < TICK_CAP, "the script completed inside the cap");
	}

	@Test
	void theAuthoredScriptIsOnDiskAsTheEditorLeftIt() throws IOException {
		// A guard on the artifact itself: the name is the file name (what a bots.cfg row points at), and
		// an empty or unparsable file would fail here rather than as a skipped line at boot.
		Path file = scriptFile();
		assertTrue(Files.isRegularFile(file), "missing " + file + " — the example the tool writes");

		String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
		BotScript script = ScriptDocument.fromJson(SCRIPT, text);

		assertEquals(SCRIPT, ScriptDocument.nameOfFile(file.getFileName().toString()),
				"the file name is the script's name");
		assertTrue(script.root() != null, "the document builds a root");
	}

	/** The committed example, read from the real {@code Data/cfg/bots} rather than a fixture. */
	private static BotScript loadAuthoredScript() throws IOException {
		Path file = scriptFile();
		assertTrue(Files.isRegularFile(file),
				"missing " + file + " — author it in the workshop, or re-add the committed example");
		String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
		return ScriptDocument.fromJson(SCRIPT, text);
	}

	private static Path scriptFile() {
		String dataRoot = System.getProperty("workshopDataRoot");
		assertTrue(dataRoot != null && !dataRoot.isBlank(),
				"the test task must set workshopDataRoot to Data/; see build.gradle");
		return Path.of(dataRoot, "cfg", "bots", SCRIPT + ".json");
	}

	/**
	 * The world the script sees: an oak and a bank, placed by hand.
	 *
	 * <p>Same shape as {@link ScriptLoopTest}'s injected world, installed through the live seams rather
	 * than passed to a constructor, because a document has no constructor to pass them to.
	 */
	private static void installWorld() {
		Locations.install(Locations.curated(Arrays.asList(
				Location.point("test_tree", LocationKind.TREE, TREE_X, TREE_Y, 0),
				Location.point("test_bank", LocationKind.BANK, BANK_X, BANK_Y, 0))));

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
		ResourceScan.install(ResourceScan.with(source, kinds));
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
