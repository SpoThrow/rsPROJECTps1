package server.content.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import core.util.ISAACRandomGen;
import server.event.CycleEventHandler;
import server.game.players.Client;
import server.game.players.Player;
import server.game.players.packets.MakeXAmount;

/**
 * Pins the one thing every chatbox creation menu depends on: that the button ids belong to
 * interface 8938 and to nothing else, and that the player's open menu -- not the id -- decides who
 * answers a click.
 *
 * <p>The table is checked against the two families that were using the interface before this class
 * existed rather than against a copy of itself, because a transcription error here would compile,
 * pass a self-check and hand one family's clicks to another. That check is the whole reason these
 * ids are written down in one place.
 */
class CreationMenuTest {

	private Client lastClient;

	private static Client client() {
		Client c = new Client(null, 2);
		c.getOutStream().packetEncryption = new ISAACRandomGen(new int[] { 1, 2, 3, 4 });
		c.expModifier = 1;
		return c;
	}

	private Client withItems(int... idAmountPairs) {
		Client c = client();
		lastClient = c;
		int slot = 0;
		for (int i = 0; i < idAmountPairs.length; i += 2) {
			c.playerItems[slot] = idAmountPairs[i] + 1;
			c.playerItemsN[slot] = idAmountPairs[i + 1];
			slot++;
		}
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
	// The table
	// ---------------------------------------------------------------------------------------

	@Test
	void theTableIsTheOneTheSnakeskinRowsAlreadySend() {
		// CraftingData's snakeskin rows occupy the same five rows of the same interface, and their
		// own tables are what the server has been answering clicks with until now. If this class
		// had the ids wrong, these would be the ids that disagree.
		CraftingData.leatherData[] rows = {
				CraftingData.leatherData.SNAKESKIN_BODY,
				CraftingData.leatherData.SNAKESKIN_CHAPS,
				CraftingData.leatherData.SNAKESKIN_BANDANA,
				CraftingData.leatherData.SNAKESKIN_BOOTS,
				CraftingData.leatherData.SNAKESKIN_VAMBRACES,
		};
		assertEquals(rows.length, CreationMenu.ROWS, "the interface has one row per snakeskin item");
		for (int row = 0; row < rows.length; row++) {
			for (int i = 0; i < CreationMenu.AMOUNTS.length; i++) {
				int button = CreationMenu.BUTTONS[row][i];
				assertEquals(button, rows[row].getButtonId(button),
						"row " + row + " button " + button + " is not the snakeskin row's");
				assertEquals(CreationMenu.AMOUNTS[i], rows[row].getAmount(button),
						"button " + button + " asks for a different amount on the snakeskin row");
			}
		}
	}

	@Test
	void noButtonAppearsOnTwoRows() {
		// Two rows sharing a button would make one click do two things, and the row that went
		// second would be decided by the order of a loop nobody would think to look at.
		java.util.Set<Integer> seen = new java.util.HashSet<Integer>();
		for (int[] row : CreationMenu.BUTTONS) {
			assertEquals(CreationMenu.AMOUNTS.length, row.length, "every row carries every amount");
			for (int button : row) {
				assertTrue(seen.add(button), "button " + button + " is on more than one row");
			}
		}
	}

	@Test
	void theAmountsAreOneFiveTenAndTheWholePack() {
		// 28 is the inventory, which is what the interface's own fourth button means. Written down
		// because all four are read out of the table above and a reordering would be invisible.
		assertArrayEqualsInt(new int[] { 1, 5, 10, 28 }, CreationMenu.AMOUNTS);
	}

	@Test
	void rowAndAmountAreReadBackFromTheButton() {
		for (int row = 0; row < CreationMenu.ROWS; row++) {
			for (int i = 0; i < CreationMenu.AMOUNTS.length; i++) {
				int button = CreationMenu.BUTTONS[row][i];
				assertEquals(row, CreationMenu.rowFor(button), "row for " + button);
				assertEquals(CreationMenu.AMOUNTS[i], CreationMenu.amountFor(button), "amount for " + button);
			}
		}
		assertEquals(-1, CreationMenu.rowFor(0), "nothing is not a row");
		assertEquals(-1, CreationMenu.rowFor(10239), "and a herblore button is not one either");
		assertEquals(0, CreationMenu.amountFor(0), "an unknown button asks for nothing");
	}

	@Test
	void makeXIsRefusedWithoutAnOpenMenu() {
		// The ids a product click travels on are not exclusively this interface's, and neither is
		// the make-X opcode. With nothing open there is no row and no item, so the request has to
		// be dropped rather than guessed at.
		Client c = withItems(1623, 5);

		assertFalse(CreationMenu.submit(c, 0, 5), "no menu means no row to answer");
	}

	@Test
	void makeXOnABlankRowIsSwallowedRatherThanRefused() {
		// Same reasoning as the button case above: five rows are live on the client even when the
		// menu filled one, and a blank row must do nothing rather than repeat the row above it.
		Client c = withItems(1623, 5);
		CreationMenu.open(c, CreationMenu.Kind.GEM_CUTTING, new int[] { 1623 });

		assertTrue(CreationMenu.submit(c, 3, 5), "row 4 is blank, but it is still this menu's");
	}

	@Test
	void makeXOutsideTheRowsIsNotClaimed() {
		Client c = withItems(1623, 5);
		CreationMenu.open(c, CreationMenu.Kind.GEM_CUTTING, new int[] { 1623 });

		assertFalse(CreationMenu.submit(c, -1, 5), "there is no row before the first");
		assertFalse(CreationMenu.submit(c, CreationMenu.ROWS, 5), "and none after the fifth");
	}

	@Test
	void makeXOfNothingIsNotARequestToMakeOne() {
		// A count of zero means the box was submitted empty or the packet was garbled. Either way
		// it is not "one", and quietly turning it into one is how a stray packet makes a thing.
		Client c = withItems(1623, 5);
		CreationMenu.open(c, CreationMenu.Kind.GEM_CUTTING, new int[] { 1623 });

		assertTrue(CreationMenu.submit(c, 0, 0), "the row is claimed so nothing else answers it");
	}

	@Test
	void theMakeXCeilingIsTheOneTheClientAlsoUses() {
		// The client clamps to X_MAX before it sends and this clamps again on arrival. If the two
		// drifted apart, a legitimate request would be silently reduced, which reads in game as the
		// chatbox ignoring the number.
		assertEquals(9999, CreationMenu.MAX_X);
	}

	@Test
	void theMakeXOpcodeHasTheSizeTheDecoderNeeds() {
		// An opcode the size table does not know is a length of zero, and a length of zero makes
		// every make-X disconnect the player. The opcode lives with the packet and the length lives
		// with the client, so this is the assertion that keeps the two in step.
		assertEquals(4, Client.PACKET_SIZES[MakeXAmount.OPCODE],
				"make-X must be the four bytes its payload is");
	}

	// ---------------------------------------------------------------------------------------
	// Opening and closing
	// ---------------------------------------------------------------------------------------

	@Test
	void openingFillsTheRowsAndBlanksTheRest() {
		Client c = withItems();

		CreationMenu.open(c, CreationMenu.Kind.GEM_CUTTING, new int[] { 1623, 1621 });

		assertEquals(CreationMenu.Kind.GEM_CUTTING, c.creationMenu);
		assertArrayEqualsInt(new int[] { 1623, 1621, -1, -1, -1 }, c.creationRows);
	}

	@Test
	void openingReportsHowManyRowsItCouldShow() {
		// The families cap themselves on this, so it has to be the truth rather than the count
		// they asked for.
		Client c = withItems();

		assertEquals(3, CreationMenu.open(c, CreationMenu.Kind.GEM_CUTTING, new int[] { 1, 2, 3 }));
		assertEquals(CreationMenu.ROWS,
				CreationMenu.open(c, CreationMenu.Kind.GEM_CUTTING, new int[] { 1, 2, 3, 4, 5, 6, 7 }));
	}

	@Test
	void openingClearsTheTwoOlderMenusOnTheSameInterface() {
		// A button fires whichever flag is set. Nothing else keeps the wheel and the snakeskin rows
		// from answering a click on the chisel's menu.
		Client c = withItems();
		c.potteryDialogue = true;
		c.craftDialogue = true;

		CreationMenu.open(c, CreationMenu.Kind.TANNING, new int[] { 1739 });

		assertFalse(c.potteryDialogue, "the wheel's buttons must not still be live");
		assertFalse(c.craftDialogue, "and neither must the snakeskin rows'");
	}

	@Test
	void theWheelClosesTheCreationMenu() {
		// The other direction, and the one a later change is most likely to forget.
		Client c = withItems(Pottery.SOFT_CLAY, 5);
		c.skills.playerLevel[Player.playerCrafting] = 99;
		CreationMenu.open(c, CreationMenu.Kind.GEM_CUTTING, new int[] { 1623 });

		Pottery.openWheel(c);

		assertNull(c.creationMenu, "opening the wheel must take the chisel's menu down");
		assertTrue(c.potteryDialogue);
	}

	@Test
	void closingLeavesNoRowBehind() {
		Client c = withItems();
		CreationMenu.open(c, CreationMenu.Kind.GEM_CUTTING, new int[] { 1623, 1621 });

		CreationMenu.close(c);

		assertNull(c.creationMenu);
		assertArrayEqualsInt(new int[] { -1, -1, -1, -1, -1 }, c.creationRows);
	}

	@Test
	void closingIsSafeWithNothingOpen() {
		Client c = withItems();

		CreationMenu.close(c);

		assertNull(c.creationMenu, "a walk calls this unconditionally");
	}

	// ---------------------------------------------------------------------------------------
	// Routing
	// ---------------------------------------------------------------------------------------

	@Test
	void noMenuMeansNoButtonIsClaimed() {
		// The interface's buttons must not be swallowed for players who have never opened a menu,
		// because those ids are not exclusively this interface's to answer.
		Client c = withItems(1623, 1);

		assertFalse(CreationMenu.route(c, CreationMenu.BUTTONS[0][0]),
				"with nothing open the button is not this menu's to take");
	}

	@Test
	void aButtonOfTheOpenMenuIsClaimed() {
		Client c = withItems(1623, 5);
		CreationMenu.open(c, CreationMenu.Kind.GEM_CUTTING, new int[] { 1623 });

		assertTrue(CreationMenu.route(c, CreationMenu.BUTTONS[0][0]),
				"a row of the open menu is the open menu's");
	}

	@Test
	void aClickOnABlankRowIsSwallowedRatherThanRefused() {
		// A one-row menu is still five rows of live buttons on the client. Falling through to the
		// switch in ClickingButtons is how a blank row would end up doing something else entirely.
		Client c = withItems(1623, 5);
		CreationMenu.open(c, CreationMenu.Kind.GEM_CUTTING, new int[] { 1623 });

		assertTrue(CreationMenu.route(c, CreationMenu.BUTTONS[3][0]),
				"row 4 is blank, but it is still this menu's button");
	}

	@Test
	void aButtonThatIsNoRowIsNotClaimed() {
		Client c = withItems(1623, 5);
		CreationMenu.open(c, CreationMenu.Kind.GEM_CUTTING, new int[] { 1623 });

		assertFalse(CreationMenu.route(c, 10239),
				"a herblore make-one button is not a row of this interface");
	}

	@Test
	void theOpenMenuDecidesWhoAnswers() {
		// Both families are reachable from the same five rows, so this is the test that says the
		// kind on the player is what separates them.
		Client c = withItems(1623, 1, 1739, 1, 995, 1000);
		c.skills.playerLevel[Player.playerCrafting] = 99;

		CreationMenu.open(c, CreationMenu.Kind.TANNING, new int[] { 1741 });
		CreationMenu.route(c, CreationMenu.BUTTONS[0][0]);

		assertEquals(1, count(c, 1741), "the tanner's row made leather");
		assertEquals(0, count(c, 1607), "and the gem sitting in the pack was left alone");
	}

	@Test
	void theBoltTipKindTipsTheRowItsGem() {
		// A fourth family reaches the same five rows, and the gem it is opened on is a CUT gem: the
		// menu's rows are the cut gems in the pack, not the uncut ones the chisel's other menu lists.
		Client c = withItems(1755, 1, 1607, 2); // chisel, cut sapphire
		CreationMenu.open(c, CreationMenu.Kind.BOLT_TIPS, new int[] { 1607 });

		assertTrue(CreationMenu.route(c, CreationMenu.BUTTONS[0][0]), "the row is this menu's");
		passCycles(2);

		assertEquals(10, count(c, 9189), "a sapphire row makes sapphire bolt tips");
		assertEquals(1, count(c, 1607), "and spends one gem");
	}

	@Test
	void aBlankRowOfTheBoltTipMenuMakesNothing() {
		// The same rule as every other family: a row the menu left empty must do nothing rather
		// than repeat the row above it, and must still be claimed so nothing else answers it.
		Client c = withItems(1755, 1, 1607, 2);
		CreationMenu.open(c, CreationMenu.Kind.BOLT_TIPS, new int[] { 1607 });

		assertTrue(CreationMenu.route(c, CreationMenu.BUTTONS[3][0]), "row 5 is this menu's");
		passCycles(4);

		assertEquals(0, count(c, 9189), "an empty row made nothing");
		assertEquals(2, count(c, 1607), "and spent nothing");
	}

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
