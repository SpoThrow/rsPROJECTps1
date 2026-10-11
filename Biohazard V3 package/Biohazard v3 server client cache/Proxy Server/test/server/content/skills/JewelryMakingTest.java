package server.content.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import server.game.players.packets.MakeXAmount;

/**
 * Pins the jewellery table, the mould interface's rows, and the ticked action behind them.
 *
 * <p>Two of these are regressions rather than descriptions. The onyx ring is the first: the recipe
 * table named {@code 6583} — the Ring of stone — while the interface drew {@code 6575}, so the one
 * product the interface offered for onyx was the one product that matched no recipe and returned in
 * silence. The second is the plain gold pieces: they are made with no gem, and the old code
 * detected that by asking whether {@code ItemAssistant.getItemName(-1)} contained the word
 * "unarmed" — which it does, because that is what the item table falls back to for an unknown id.
 * That worked by accident and said so nowhere. {@link JewelryMaking.NO_GEM} says it out loud.
 */
class JewelryMakingTest {

	/** Gold bar, the moulds, and a few gems and products, written out so a table edit shows up here. */
	private static final int GOLD_BAR = 2357;
	private static final int RING_MOULD = 1592;
	private static final int NECKLACE_MOULD = 1597;
	private static final int AMULET_MOULD = 1595;

	private static final int SAPPHIRE = 1607;          // cut sapphire
	private static final int DIAMOND = 1601;           // cut diamond
	private static final int SAPPHIRE_RING = 1637;
	private static final int ONYX_RING = 6575;
	private static final int RING_OF_STONE = 6583;
	private static final int GOLD_RING = 1635;
	private static final int GOLD_NECKLACE = 1654;

	/** Two cycles per item, the anvil and fletching cadence. */
	private static final int CYCLES_PER_ITEM = 2;

	private Client lastClient;

	private static Client client() {
		Client c = new Client(null, 2);
		c.getOutStream().packetEncryption = new ISAACRandomGen(new int[] { 1, 2, 3, 4 });
		c.expModifier = 1;
		return c;
	}

	/** A player with the listed items, at the given Crafting level. */
	private Client jeweller(int level, int... idAmountPairs) {
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

	/** A jeweller holding everything a sapphire ring needs. */
	private Client ready(int level, int bars, int gems) {
		return jeweller(level, GOLD_BAR, bars, SAPPHIRE, gems, RING_MOULD, 1);
	}

	@AfterEach
	void stopEvents() {
		if (lastClient != null) {
			CycleEventHandler.stopEvents(lastClient);
		}
		lastClient = null;
	}

	// ---------------------------------------------------------------------------------------
	// The table, and the interface it builds
	// ---------------------------------------------------------------------------------------

	@Test
	void aRingRowIsNeverTheRingOfStone() {
		// The defect, stated as a fact about the table: 6583 is an enchanted ring and must not be
		// what a mould produces, whatever the interface's own array once said.
		assertEquals(ONYX_RING, JewelryMaking.Product.ONYX_RING.product);
		assertNull(JewelryMaking.forProduct(RING_OF_STONE),
				"the Ring of stone is not a mould product");
	}

	@Test
	void everyKindFillsExactlyTheSevenSlotsTheInterfaceDraws() {
		// Seven is not a preference: the probe (tools/mould-interface-probe) reads 7x1 out of
		// containers 4233/4239/4245 in the real cache. A table that grew an eighth product would
		// have nowhere to put it.
		for (JewelryMaking.Kind kind : JewelryMaking.Kind.values()) {
			assertEquals(7, JewelryMaking.productsFor(kind).length,
					"kind " + kind + " must fill the interface's seven slots");
		}
	}

	@Test
	void theRowsAreTheProductsAndTheProductsAreTheRows() {
		// The old code had two lists that were supposed to agree and did not. Every product has to
		// be reachable from the interface, and every interface row has to be a product.
		for (JewelryMaking.Kind kind : JewelryMaking.Kind.values()) {
			for (JewelryMaking.Product p : JewelryMaking.productsFor(kind)) {
				assertEquals(kind, p.kind);
				assertNotNull(JewelryMaking.forProduct(p.product));
			}
		}
		assertEquals(JewelryMaking.Product.values().length, 21,
				"three kinds of seven");
	}

	@Test
	void theContainerAndFrameIdsAreTheOnesTheCacheHas() {
		// These three container ids are switched on in RemoveItem/Bank5/Bank10, and the frames are
		// written by mouldInterface. Read out of the cache by tools/mould-interface-probe, so a
		// change here is a change to the real interface and should be deliberate.
		assertEquals(4233, JewelryMaking.Kind.RING.container);
		assertEquals(4239, JewelryMaking.Kind.NECKLACE.container);
		assertEquals(4245, JewelryMaking.Kind.AMULET.container);
		assertEquals(4229, JewelryMaking.Kind.RING.modelFrame);
		assertEquals(4230, JewelryMaking.Kind.RING.labelFrame);
		assertEquals(4235, JewelryMaking.Kind.NECKLACE.modelFrame);
		assertEquals(4236, JewelryMaking.Kind.NECKLACE.labelFrame);
		assertEquals(4241, JewelryMaking.Kind.AMULET.modelFrame);
		assertEquals(4242, JewelryMaking.Kind.AMULET.labelFrame);
	}

	@Test
	void eachKindHasItsOwnMould() {
		assertEquals(RING_MOULD, JewelryMaking.Kind.RING.mould);
		assertEquals(NECKLACE_MOULD, JewelryMaking.Kind.NECKLACE.mould);
		assertEquals(AMULET_MOULD, JewelryMaking.Kind.AMULET.mould);
	}

	@Test
	void eachKindsMouldIsTheOnlyOneThatUnlocksItsRows() {
		Client c = jeweller(99, RING_MOULD, 1);

		assertEquals(SAPPHIRE_RING, JewelryMaking.slotItem(c, JewelryMaking.Kind.RING, 1));
		assertEquals(-1, JewelryMaking.slotItem(c, JewelryMaking.Kind.NECKLACE, 1),
				"a ring mould does not unlock the necklace row");
		assertEquals(-1, JewelryMaking.slotItem(c, JewelryMaking.Kind.AMULET, 1));
	}

	@Test
	void aMissingMouldLeavesEverySlotEmptyRatherThanStale() {
		// The slots have to be written empty, not left alone: the interface keeps whatever was last
		// drawn in a box nobody overwrites.
		Client c = jeweller(99);

		for (int slot = 0; slot < 7; slot++) {
			assertEquals(-1, JewelryMaking.slotItem(c, JewelryMaking.Kind.RING, slot),
					"slot " + slot + " must be cleared");
		}
	}

	@Test
	void theAmuletLineNamesAmulets() {
		// The old text read "You need a amulet mould to craft necklaces", overwriting the correct
		// sentence the cache already had on that frame.
		assertEquals("You need an amulet mould to craft amulets.",
				JewelryMaking.Kind.AMULET.missingMould());
		assertEquals("You need a ring mould to craft rings.", JewelryMaking.Kind.RING.missingMould());
		assertEquals("You need a necklace mould to craft necklaces.",
				JewelryMaking.Kind.NECKLACE.missingMould());
	}

	@Test
	void everyGemTheTableSetsIsOneGemCuttingMakes() {
		// The setter has to be the CUT gem, or jewellery would consume an uncut stone and the two
		// skills would disagree about what a sapphire is.
		for (JewelryMaking.Product p : JewelryMaking.Product.values()) {
			if (!p.hasGem()) {
				continue;
			}
			boolean found = false;
			for (CraftingData.cutGemData g : CraftingData.cutGemData.values()) {
				if (g.getCut() == p.gem) {
					found = true;
				}
			}
			assertTrue(found, p + " sets " + p.gem + ", which is not a cut gem");
		}
	}

	@Test
	void theExperienceIsTheGuidesOwn() {
		assertEquals(5, JewelryMaking.Product.GOLD_RING.level);
		assertEquals(15, JewelryMaking.Product.GOLD_RING.xp);
		assertEquals(67, JewelryMaking.Product.ONYX_RING.level);
		assertEquals(115, JewelryMaking.Product.ONYX_RING.xp);
		assertEquals(90, JewelryMaking.Product.ONYX_AMULET.level);
		assertEquals(165, JewelryMaking.Product.ONYX_AMULET.xp);
	}

	@Test
	void aGemIsNamedWithTheRightArticle() {
		assertEquals("You need a gold bar and a sapphire to make this.",
				JewelryMaking.needMessage("Sapphire"));
		assertEquals("You need a gold bar and an onyx to make this.",
				JewelryMaking.needMessage("Onyx"));
	}

	// ---------------------------------------------------------------------------------------
	// The left click opens the chatbox
	// ---------------------------------------------------------------------------------------

	@Test
	void theLeftClickAsksTheAmountRatherThanMakingOne() {
		// The change in one assertion: the click that used to make one now only asks, because the
		// interface cannot offer "all" and this menu can.
		Client c = ready(99, 5, 5);

		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);

		assertEquals(CreationMenu.Kind.JEWELLERY, c.creationMenu, "the chatbox is up");
		assertEquals(0, count(c, SAPPHIRE_RING), "and nothing has been made yet");
		assertEquals(5, count(c, SAPPHIRE), "nor consumed");
	}

	@Test
	void thePromptCarriesOnlyTheProductThatWasClicked() {
		Client c = ready(99, 5, 5);

		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);

		assertArrayEqualsInt(new int[] { SAPPHIRE_RING, -1, -1, -1, -1 }, c.creationRows,
				"one row, and the other four are cleared rather than left from the last menu");
	}

	@Test
	void theChatboxButtonMakesTheProductTheRowNames() {
		Client c = ready(99, 3, 3);
		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][0]); // make 1
		passCycles(CYCLES_PER_ITEM * 3);

		assertEquals(1, count(c, SAPPHIRE_RING));
		assertNull(c.creationMenu, "a running batch cannot still be taking clicks");
	}

	@Test
	void anUnrelatedButtonDoesNotStartABatch() {
		Client c = ready(99, 3, 3);
		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);

		assertFalse(CreationMenu.route(c, 999999));

		passCycles(CYCLES_PER_ITEM * 2);
		assertEquals(0, count(c, SAPPHIRE_RING));
	}

	// ---------------------------------------------------------------------------------------
	// The action
	// ---------------------------------------------------------------------------------------

	@Test
	void makeOneMakesOneAndNotTheWholeStack() {
		Client c = ready(99, 10, 10);
		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][0]); // make 1
		passCycles(CYCLES_PER_ITEM * 6);

		assertEquals(1, count(c, SAPPHIRE_RING), "the button asked for one");
		assertEquals(9, count(c, GOLD_BAR));
		assertEquals(9, count(c, SAPPHIRE));
	}

	@Test
	void makeTenMakesTenAndThenStops() {
		Client c = ready(99, 28, 28);
		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][2]); // make 10
		passCycles(CYCLES_PER_ITEM * 20);

		assertEquals(10, count(c, SAPPHIRE_RING));
		assertEquals(18, count(c, GOLD_BAR));
		assertEquals(18, count(c, SAPPHIRE));
	}

	@Test
	void makeAllIsCappedByTheBarAndNotByTheButton() {
		// This is the whole reason the chatbox exists for jewellery: the interface's own option is
		// Make 10, so "all" was previously unreachable. Three bars is what three makes it.
		Client c = ready(99, 3, 4);
		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][3]); // make 28
		passCycles(CYCLES_PER_ITEM * 10);

		assertEquals(3, count(c, SAPPHIRE_RING), "three bars is three rings");
		assertEquals(0, count(c, GOLD_BAR));
		assertEquals(1, count(c, SAPPHIRE), "and the fourth gem was not eaten");
	}

	@Test
	void makeAllIsCappedByTheGemToo() {
		Client c = ready(99, 4, 3);

		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);
		CreationMenu.route(c, CreationMenu.BUTTONS[0][3]);
		passCycles(CYCLES_PER_ITEM * 10);

		assertEquals(3, count(c, SAPPHIRE_RING));
		assertEquals(1, count(c, GOLD_BAR), "and the fourth bar was not eaten");
	}

	// ---------------------------------------------------------------------------------------
	// Make X
	// ---------------------------------------------------------------------------------------

	@Test
	void makeXActuallyMakesTheNumberThatWasTyped() {
		// The whole point of the packet. Seven is not 1, 5, 10 or the pack, so no button on the
		// interface can ask for it and nothing but the count in the payload can produce it.
		Client c = ready(99, 20, 20);

		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);
		assertTrue(CreationMenu.submit(c, 0, 7), "the open menu claims the row");
		passCycles(CYCLES_PER_ITEM * 20);

		assertEquals(7, count(c, SAPPHIRE_RING), "seven is seven");
		assertEquals(13, count(c, GOLD_BAR));
		assertEquals(13, count(c, SAPPHIRE));
	}

	/**
	 * The same thing, but through the four bytes the client actually sends.
	 *
	 * <p>{@link #makeXActuallyMakesTheNumberThatWasTyped} calls {@code submit} directly, so it would
	 * pass even if the packet class read its payload wrong - the wrong half of the pair for the
	 * wrong byte order, say, which would turn a request for 7 of the first product into a request
	 * for row 1792 of nothing. This one feeds the exact bytes {@code MakeX.xPayload} produces into
	 * the exact class {@code PacketHandler} dispatches to, so the pack, the byte order and the
	 * unpack are all covered.
	 */
	@Test
	void theMakeXBytesAreWhatTheServerReadsBack() {
		Client c = ready(99, 20, 20);

		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);
		// Row 0 in the high half, 7 in the low, big-endian - written by hand rather than by
		// calling the client's own packer, because a test that shares the packer with the code
		// under test cannot catch the packer being wrong.
		feedPacket(c, 0x00, 0x00, 0x00, 0x07);
		passCycles(CYCLES_PER_ITEM * 20);

		assertEquals(7, count(c, SAPPHIRE_RING), "the four bytes carried the number");
		assertEquals(13, count(c, GOLD_BAR), "and the bar count fell by seven");
	}

	@Test
	void theMakeXBytesAddressTheRowTheClientMeant() {
		// A ring mould offers one product, so row 0 and row 1 are "the ring" and "nothing". If the
		// halves of the payload were swapped, this is the assertion that would stop passing.
		Client c = ready(99, 20, 20);

		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);
		feedPacket(c, 0x00, 0x00, 0x00, 0x05);   // row 0 -> the only row
		passCycles(CYCLES_PER_ITEM * 20);
		assertEquals(5, count(c, SAPPHIRE_RING));

		Client other = ready(99, 20, 20);
		JewelryMaking.openAmountPrompt(other, SAPPHIRE_RING);
		feedPacket(other, 0x00, 0x01, 0x00, 0x05); // row 1 -> past the end of a one-row menu
		passCycles(CYCLES_PER_ITEM * 20);
		assertEquals(0, count(other, SAPPHIRE_RING), "there is no second row to make");
		assertEquals(20, count(other, GOLD_BAR), "and nothing was consumed");
	}

	/** Puts bytes in the player's in-stream and runs one make-X packet over them. */
	private static void feedPacket(Client c, int... bytes) {
		byte[] payload = new byte[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			payload[i] = (byte) bytes[i];
		}
		c.getInStream().buffer = payload;
		c.getInStream().currentOffset = 0;
		new MakeXAmount().processPacket(c, MakeXAmount.OPCODE, payload.length);
	}

	@Test
	void makeXIsStillCappedByThePack() {
		// A count larger than the materials is not an error and not a special case: it is the same
		// stopping condition the make-all button has always used.
		Client c = ready(99, 3, 3);

		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);
		CreationMenu.submit(c, 0, 100);
		passCycles(CYCLES_PER_ITEM * 20);

		assertEquals(3, count(c, SAPPHIRE_RING), "three bars is three rings, whatever was typed");
	}

	@Test
	void makeXAndABigAskAreIndistinguishableOnceTheyLand() {
		// Guards the one thing the client and the server share the meaning of: a make-X of 10 must
		// do exactly what the interface's own make-10 button does, or the row is lying about itself.
		Client viaButton = ready(99, 12, 12);
		Client viaMakeX = ready(99, 12, 12);

		JewelryMaking.openAmountPrompt(viaButton, SAPPHIRE_RING);
		CreationMenu.route(viaButton, CreationMenu.BUTTONS[0][2]); // make 10
		JewelryMaking.openAmountPrompt(viaMakeX, SAPPHIRE_RING);
		CreationMenu.submit(viaMakeX, 0, 10);
		passCycles(CYCLES_PER_ITEM * 20);

		assertEquals(count(viaButton, SAPPHIRE_RING), count(viaMakeX, SAPPHIRE_RING));
		assertEquals(count(viaButton, GOLD_BAR), count(viaMakeX, GOLD_BAR));
		assertEquals(10, count(viaMakeX, SAPPHIRE_RING));
	}

	@Test
	void aPlainGoldPieceNeedsNoGem() {
		// The regression: this used to hinge on getItemName(-1) returning "Unarmed".
		Client c = jeweller(99, GOLD_BAR, 3, RING_MOULD, 1);
		JewelryMaking.openAmountPrompt(c, GOLD_RING);

		assertEquals(CreationMenu.Kind.JEWELLERY, c.creationMenu, "a bar and a mould is enough");
		CreationMenu.route(c, CreationMenu.BUTTONS[0][3]);
		passCycles(CYCLES_PER_ITEM * 8);

		assertEquals(3, count(c, GOLD_RING));
		assertEquals(0, count(c, GOLD_BAR));
	}

	@Test
	void aFullPackStopsTheBatchWithoutEatingAnything() {
		// Jewellery consumes two items for one, so a tick spent with nowhere to put the product
		// would destroy a bar and a gem and hand back nothing.
		Client c = jeweller(99, RING_MOULD, 1, SAPPHIRE, 1);
		for (int slot = 0; slot < c.playerItems.length; slot++) {
			c.playerItems[slot] = GOLD_BAR + 1;
			c.playerItemsN[slot] = 1;
		}
		assertEquals(0, c.getItems().freeSlots(), "the pack is genuinely full");

		JewelryMaking.makeNow(c, GOLD_RING, 5);
		passCycles(CYCLES_PER_ITEM * 6);

		assertEquals(0, count(c, GOLD_RING));
		assertEquals(c.playerItems.length, count(c, GOLD_BAR), "nothing was consumed");
	}

	@Test
	void makingPaysTheTablesXpThroughTheCraftingRate() {
		Client c = ready(99, 1, 1);
		double before = c.skills.playerXP[Player.playerCrafting];

		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);
		CreationMenu.route(c, CreationMenu.BUTTONS[0][0]);
		passCycles(CYCLES_PER_ITEM);

		assertEquals(JewelryMaking.Product.SAPPHIRE_RING.xp * Config.CRAFTING_EXPERIENCE,
				c.skills.playerXP[Player.playerCrafting] - before, 1e-9);
	}

	// ---------------------------------------------------------------------------------------
	// The interface's own shortcuts
	// ---------------------------------------------------------------------------------------

	@Test
	void theShortcutAmountsStillWorkWithoutThePrompt() {
		// "Make 5" and "Make 10" are the interface's own options and they are kept: only the left
		// click routes through the chatbox.
		Client c = ready(99, 28, 28);

		JewelryMaking.makeNow(c, SAPPHIRE_RING, 5);
		passCycles(CYCLES_PER_ITEM * 12);

		assertEquals(5, count(c, SAPPHIRE_RING));
		assertNull(c.creationMenu, "and no menu was opened on the way");
	}

	// ---------------------------------------------------------------------------------------
	// What stops it, and what refuses it
	// ---------------------------------------------------------------------------------------

	@Test
	void thePromptIsNotOpenedWithoutABar() {
		Client c = jeweller(99, SAPPHIRE, 5, RING_MOULD, 1);

		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);

		assertNull(c.creationMenu, "a menu whose only answer is a refusal is worse than no menu");
	}

	@Test
	void thePromptIsNotOpenedWithoutTheMould() {
		Client c = jeweller(99, GOLD_BAR, 5, SAPPHIRE, 5);

		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);

		assertNull(c.creationMenu);
	}

	@Test
	void thePromptIsNotOpenedWithoutTheGem() {
		Client c = jeweller(99, GOLD_BAR, 5, RING_MOULD, 1);

		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);

		assertNull(c.creationMenu);
	}

	@Test
	void thePromptIsNotOpenedBelowTheLevel() {
		Client c = ready(1, 5, 5);

		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);

		assertNull(c.creationMenu, "level 1 cannot make a sapphire ring");
	}

	@Test
	void theMouldIsReCheckedWhenTheButtonIsPressed() {
		// The mould can leave the pack while the interface is open, and the prompt is a second
		// window during which it can leave again.
		Client c = ready(99, 5, 5);
		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);
		removeAll(c, RING_MOULD);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][3]);
		passCycles(CYCLES_PER_ITEM * 6);

		assertEquals(0, count(c, SAPPHIRE_RING));
		assertEquals(5, count(c, GOLD_BAR), "and nothing was consumed trying");
	}

	@Test
	void aLevelTooLowToMakeItMakesNothing() {
		Client c = jeweller(99, GOLD_BAR, 5, DIAMOND, 5, RING_MOULD, 1);
		c.skills.playerLevel[Player.playerCrafting] = 1;

		JewelryMaking.makeNow(c, 1643 /* diamond ring, level 43 */, 5);
		passCycles(CYCLES_PER_ITEM * 6);

		assertEquals(0, count(c, 1643));
		assertEquals(5, count(c, GOLD_BAR));
	}

	// ---------------------------------------------------------------------------------------
	// Walking away
	// ---------------------------------------------------------------------------------------

	@Test
	void walkingEndsTheBatchBeforeItDelivers() {
		Client c = ready(99, 10, 10);
		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][3]);
		c.getPA().resetVariables(); // what every walk step reaches
		passCycles(CYCLES_PER_ITEM * 4);

		assertEquals(0, count(c, SAPPHIRE_RING), "a cancelled batch must not deliver");
		assertEquals(10, count(c, GOLD_BAR), "and must not consume");
		assertEquals(10, count(c, SAPPHIRE));
	}

	@Test
	void walkingClosesThePromptToo() {
		Client c = ready(99, 10, 10);
		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);

		c.getPA().resetVariables();

		assertNull(c.creationMenu);
	}

	@Test
	void aFreshChoiceAfterAWalkDoesNotRunTwoLoops() {
		// The queued event has to be removed, not just have its flag cleared: otherwise it wakes on
		// its next tick, finds the flag set by the new batch and makes alongside it.
		Client c = ready(99, 12, 12);
		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][3]);
		c.getPA().resetVariables();
		JewelryMaking.openAmountPrompt(c, SAPPHIRE_RING);
		CreationMenu.route(c, CreationMenu.BUTTONS[0][0]); // make 1 this time
		passCycles(CYCLES_PER_ITEM * 2);

		assertEquals(1, count(c, SAPPHIRE_RING), "one batch, one ring");
		assertEquals(11, count(c, GOLD_BAR));
	}

	@Test
	void aNecklaceAndARingShareNoState() {
		// Both kinds run through the same action, so an open prompt on one must not answer the
		// other's click — the rows are the only thing that says which is being made.
		Client c = jeweller(99, GOLD_BAR, 5, RING_MOULD, 1, NECKLACE_MOULD, 1);

		JewelryMaking.openAmountPrompt(c, GOLD_NECKLACE);
		CreationMenu.route(c, CreationMenu.BUTTONS[0][0]);
		passCycles(CYCLES_PER_ITEM * 2);

		assertEquals(1, count(c, GOLD_NECKLACE));
		assertEquals(0, count(c, GOLD_RING), "the other kind was not made");
	}

	// ---------------------------------------------------------------------------------------

	private static void removeAll(Client c, int id) {
		for (int slot = 0; slot < c.playerItems.length; slot++) {
			if (c.playerItems[slot] == id + 1) {
				c.playerItems[slot] = 0;
				c.playerItemsN[slot] = 0;
			}
		}
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
