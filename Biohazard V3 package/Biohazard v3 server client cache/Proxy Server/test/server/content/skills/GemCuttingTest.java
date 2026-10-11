package server.content.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import core.util.ISAACRandomGen;
import server.Config;
import server.event.CycleEventHandler;
import server.game.players.Client;
import server.game.players.Player;

/**
 * Pins the chisel's chatbox menu and the action behind it.
 *
 * <p>The two that matter longest are the level check and the stopping condition. The level check
 * is reachable in a way it never used to be — the menu lists the gems the player is <em>holding</em>
 * rather than the ones they can cut, so a low-level player can pick a diamond — and the stopping
 * condition is the whole point of the change: the old action cut until the stack ran out, and the
 * amount on the button has to be what ends it.
 */
class GemCuttingTest {

	/** Sapphire: uncut, cut, level 20, 50 xp. Written out so a table edit is visible here. */
	private static final int UNCUT_SAPPHIRE = 1623;
	private static final int CUT_SAPPHIRE = 1607;
	private static final int UNCUT_DIAMOND = 1617;
	private static final int CUT_DIAMOND = 1601;
	private static final int CHISEL = 1755;

	/** Four cycles between gems, which is what this action has always run at. */
	private static final int CYCLES_PER_GEM = 4;

	private Client lastClient;

	private static Client client() {
		Client c = new Client(null, 2);
		c.getOutStream().packetEncryption = new ISAACRandomGen(new int[] { 1, 2, 3, 4 });
		c.expModifier = 1;
		return c;
	}

	private Client cutter(int level, int... idAmountPairs) {
		Client c = client();
		lastClient = c;
		int slot = 0;
		for (int i = 0; i < idAmountPairs.length; i += 2) {
			c.playerItems[slot] = idAmountPairs[i] + 1;
			c.playerItemsN[slot] = idAmountPairs[i + 1];
			slot++;
		}
		c.skills.playerLevel[Player.playerCrafting] = level;
		return c;
	}

	@AfterEach
	void stopEvents() {
		if (lastClient != null) {
			CycleEventHandler.stopEvents(lastClient);
		}
		lastClient = null;
	}

	// ---------------------------------------------------------------------------------------
	// The rows
	// ---------------------------------------------------------------------------------------

	@Test
	void theMenuListsTheGemsInThePacksTableOrder() {
		// Diamond was put in first and sapphire second, and sapphire is still first: the rows are
		// the table's order, not the pack's, so the same gems always land on the same buttons.
		Client c = cutter(99, UNCUT_DIAMOND, 1, UNCUT_SAPPHIRE, 1);

		assertArrayEqualsInt(new int[] { UNCUT_SAPPHIRE, UNCUT_DIAMOND }, GemCutting.carriedGems(c));
	}

	@Test
	void twoStacksOfOneGemAreOneRow() {
		// The row is a product to make and the amount is the button's business, so a second stack of
		// the same gem must not become a second row answering the same click.
		Client c = cutter(99, UNCUT_SAPPHIRE, 5, UNCUT_SAPPHIRE, 7);

		assertArrayEqualsInt(new int[] { UNCUT_SAPPHIRE }, GemCutting.carriedGems(c));
	}

	@Test
	void gemsThatAreNotCarriedAreNotRows() {
		Client c = cutter(99, UNCUT_SAPPHIRE, 1);

		assertArrayEqualsInt(new int[] { UNCUT_SAPPHIRE }, GemCutting.carriedGems(c),
				"one kind held is one row, not nine");
	}

	@Test
	void theChiselOnAGemOpensTheMenuRatherThanCutting() {
		// The change in one assertion: the click that used to start cutting now only asks.
		Client c = cutter(99, UNCUT_SAPPHIRE, 5);

		GemCutting.cutGem(c, CHISEL, UNCUT_SAPPHIRE);

		assertEquals(CreationMenu.Kind.GEM_CUTTING, c.creationMenu, "the chatbox is up");
		assertEquals(5, count(c, UNCUT_SAPPHIRE), "and nothing has been cut yet");
		assertEquals(0, count(c, CUT_SAPPHIRE));
	}

	@Test
	void theChiselWithNoGemsOpensNothing() {
		Client c = cutter(99);

		GemCutting.cutGem(c, CHISEL, CHISEL);

		assertNull(c.creationMenu, "there is nothing to offer, so nothing is offered");
	}

	@Test
	void theRowOnTheMenuIsTheGemThatGetsCut() {
		// Two kinds in the pack, and the row decides which one, exactly as a herblore button
		// decides which potion the pair makes.
		Client c = cutter(99, UNCUT_SAPPHIRE, 3, UNCUT_DIAMOND, 3);
		GemCutting.cutGem(c, CHISEL, UNCUT_SAPPHIRE);

		assertArrayEqualsInt(new int[] { UNCUT_SAPPHIRE, UNCUT_DIAMOND, -1, -1, -1 }, c.creationRows);
		CreationMenu.route(c, CreationMenu.BUTTONS[1][0]); // row 2 is the diamond, make 1
		passCycles(CYCLES_PER_GEM);

		assertEquals(1, count(c, CUT_DIAMOND), "the diamond row cut a diamond");
		assertEquals(3, count(c, UNCUT_SAPPHIRE), "and the sapphires were left alone");
	}

	// ---------------------------------------------------------------------------------------
	// The action
	// ---------------------------------------------------------------------------------------

	@Test
	void makeOneCutsOneAndNotTheWholeStack() {
		Client c = cutter(99, UNCUT_SAPPHIRE, 10);
		GemCutting.cutGem(c, CHISEL, UNCUT_SAPPHIRE);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][0]); // make 1
		passCycles(CYCLES_PER_GEM * 6);

		assertEquals(1, count(c, CUT_SAPPHIRE), "the button asked for one");
		assertEquals(9, count(c, UNCUT_SAPPHIRE));
	}

	@Test
	void makeTenCutsTenAndThenStops() {
		Client c = cutter(99, UNCUT_SAPPHIRE, 28);
		GemCutting.cutGem(c, CHISEL, UNCUT_SAPPHIRE);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][2]); // make 10
		passCycles(CYCLES_PER_GEM * 20);

		assertEquals(10, count(c, CUT_SAPPHIRE));
		assertEquals(18, count(c, UNCUT_SAPPHIRE));
	}

	@Test
	void theActionEndsWhenTheStackRunsOutBeforeTheAmount() {
		Client c = cutter(99, UNCUT_SAPPHIRE, 3);
		GemCutting.cutGem(c, CHISEL, UNCUT_SAPPHIRE);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][3]); // make 28
		passCycles(CYCLES_PER_GEM * 8);

		assertEquals(3, count(c, CUT_SAPPHIRE), "three is all there was");
		assertEquals(0, count(c, UNCUT_SAPPHIRE));
	}

	@Test
	void choosingABatchClosesTheMenu() {
		Client c = cutter(99, UNCUT_SAPPHIRE, 10);
		GemCutting.cutGem(c, CHISEL, UNCUT_SAPPHIRE);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][0]);

		assertNull(c.creationMenu, "a running batch cannot still be taking clicks");
	}

	@Test
	void cuttingPaysTheTableXpThroughTheCraftingRate() {
		Client c = cutter(99, UNCUT_SAPPHIRE, 1);
		double before = c.skills.playerXP[Player.playerCrafting];
		GemCutting.cutGem(c, CHISEL, UNCUT_SAPPHIRE);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][0]);
		passCycles(CYCLES_PER_GEM);

		CraftingData.cutGemData sapphire = GemCutting.forUncut(UNCUT_SAPPHIRE);
		assertNotNull(sapphire);
		assertEquals((int) (sapphire.getXP() * Config.CRAFTING_EXPERIENCE),
				c.skills.playerXP[Player.playerCrafting] - before, 1e-9);
	}

	// ---------------------------------------------------------------------------------------
	// The level rule
	// ---------------------------------------------------------------------------------------

	@Test
	void theMenuOffersGemsThePlayerCannotCutYet() {
		// Not a bug: the rows are what the player is carrying. A low-level player carrying a
		// diamond can see it on the menu and be told what it wants, which is a better answer than
		// a menu that silently omits it.
		Client c = cutter(1, UNCUT_DIAMOND, 1);

		GemCutting.cutGem(c, CHISEL, UNCUT_DIAMOND);

		assertEquals(CreationMenu.Kind.GEM_CUTTING, c.creationMenu);
		assertArrayEqualsInt(new int[] { UNCUT_DIAMOND, -1, -1, -1, -1 }, c.creationRows);
	}

	@Test
	void aGemAboveThePlayersLevelCutsNothing() {
		Client c = cutter(1, UNCUT_DIAMOND, 5);
		GemCutting.cutGem(c, CHISEL, UNCUT_DIAMOND);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][0]);
		passCycles(CYCLES_PER_GEM * 2);

		assertEquals(0, count(c, CUT_DIAMOND), "level 1 cannot cut a diamond");
		assertEquals(5, count(c, UNCUT_DIAMOND), "and nothing was consumed trying");
	}

	@Test
	void theLevelRuleIsCheckedOnTheRowNotOnTheMenu() {
		// Sapphire is 20 and the player is 20, diamond is 43 and they are not. The row that is in
		// reach must still work while the one that is not sits there.
		Client c = cutter(20, UNCUT_SAPPHIRE, 1, UNCUT_DIAMOND, 1);
		GemCutting.cutGem(c, CHISEL, UNCUT_SAPPHIRE);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][0]); // sapphire
		passCycles(CYCLES_PER_GEM);
		assertEquals(1, count(c, CUT_SAPPHIRE), "level 20 cuts a sapphire");

		GemCutting.cutGem(c, CHISEL, UNCUT_DIAMOND);
		CreationMenu.route(c, CreationMenu.BUTTONS[0][0]); // diamond, now the only row
		passCycles(CYCLES_PER_GEM);
		assertEquals(0, count(c, CUT_DIAMOND), "and still cannot cut a diamond");
	}

	// ---------------------------------------------------------------------------------------
	// Walking away
	// ---------------------------------------------------------------------------------------

	@Test
	void walkingEndsTheCutBeforeItDelivers() {
		Client c = cutter(99, UNCUT_SAPPHIRE, 10);
		GemCutting.cutGem(c, CHISEL, UNCUT_SAPPHIRE);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][3]);
		c.getPA().resetVariables(); // what every walk step reaches
		passCycles(CYCLES_PER_GEM * 4);

		assertEquals(0, count(c, CUT_SAPPHIRE), "a cancelled batch must not deliver");
		assertEquals(10, count(c, UNCUT_SAPPHIRE), "and must not consume");
	}

	@Test
	void walkingClosesTheMenuToo() {
		// The menu is not an action, so it does not sit behind playerIsCrafting and would otherwise
		// survive the walk with rows that no longer do anything.
		Client c = cutter(99, UNCUT_SAPPHIRE, 10);
		GemCutting.cutGem(c, CHISEL, UNCUT_SAPPHIRE);

		c.getPA().resetVariables();

		assertNull(c.creationMenu);
	}

	@Test
	void aFreshChoiceAfterAWalkDoesNotRunTwoLoops() {
		// The queued event has to be removed, not just have its flag cleared: otherwise it wakes on
		// its next tick, finds the flag set by the new batch and cuts alongside it.
		Client c = cutter(99, UNCUT_SAPPHIRE, 12);
		GemCutting.cutGem(c, CHISEL, UNCUT_SAPPHIRE);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][3]);
		c.getPA().resetVariables();
		GemCutting.cutGem(c, CHISEL, UNCUT_SAPPHIRE);
		CreationMenu.route(c, CreationMenu.BUTTONS[0][0]); // make 1 this time
		passCycles(CYCLES_PER_GEM * 2);

		assertEquals(1, count(c, CUT_SAPPHIRE), "one batch, one gem");
		assertEquals(11, count(c, UNCUT_SAPPHIRE));
	}

	@Test
	void theTableHasAnEntryForEveryGemTheGuideLists() {
		// Nine uncut gems, and the row order above is this order. A row added to the table without
		// an id here would be a gem the menu lists in the pack but cannot name.
		int[] uncut = new int[CraftingData.cutGemData.values().length];
		int i = 0;
		for (CraftingData.cutGemData g : CraftingData.cutGemData.values()) {
			uncut[i++] = g.getUncut();
			assertNotNull(GemCutting.forUncut(g.getUncut()));
			assertTrue(g.getLevel() > 0, "every gem has a level");
		}
		assertEquals(9, uncut.length, "the guide's Gem Cutting tab lists nine");
		assertEquals(UNCUT_SAPPHIRE, uncut[0], "and sapphire is the first row of the menu");
	}

	// ---------------------------------------------------------------------------------------

	private static void passCycles(int cycles) {
		for (int i = 0; i < cycles; i++) {
			CycleEventHandler.process();
		}
	}

	private static int count(Client c, int id) {
		int total = 0;
		for (int i = 0; i < c.playerItems.length; i++) {
			if (c.playerItems[i] == id + 1) {
				total += c.playerItemsN[i];
			}
		}
		return total;
	}

	private static void assertArrayEqualsInt(int[] expected, int[] actual) {
		assertArrayEqualsInt(expected, actual, "");
	}

	private static void assertArrayEqualsInt(int[] expected, int[] actual, String message) {
		assertEquals(expected.length, actual.length, message);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], actual[i], message + " element " + i);
		}
	}
}
