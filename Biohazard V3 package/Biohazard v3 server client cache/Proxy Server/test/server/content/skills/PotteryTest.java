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
import server.game.players.actions.items.ItemOnObjectRegistry;
import server.game.players.actions.objects.ObjectClick;
import server.game.players.actions.objects.ObjectHandler;

/**
 * Pins the pottery tables, both stages of the action, and the two ways each stage can be reached.
 *
 * <p>The whole feature was missing, so most of these would have failed against the code this
 * replaced — but the ones that matter longest are the wiring and the level rule. The registries
 * are filled by static blocks, so a family that stops being registered is not a compile error, it
 * is a wheel that does nothing; and firing's "no level requirement" is the kind of rule a later
 * reader will assume is a bug and "fix", breaking a level-1 player holding an unfired plant pot.
 */
class PotteryTest {

	/** unfired, fired, shaping level, shaping xp, firing xp — exactly as the enum is written. */
	private static final double[][] TABLE = {
			{ 1787, 1931, 1, 6.3, 6.3 },      // pot
			{ 1789, 2313, 7, 15, 10 },        // pie dish
			{ 1791, 1923, 8, 18, 15 },        // bowl
			{ 5352, 5350, 19, 20, 17.5 },     // plant pot
			{ 4438, 4440, 25, 20, 20 },       // pot lid
	};

	/** The chatbox rows, in the order the interface shows them. */
	private static final int[][] BUTTONS = {
			{ 34245, 34244, 34243, 34242 },
			{ 34249, 34248, 34247, 34246 },
			{ 34253, 34252, 34251, 34250 },
			{ 35001, 35000, 34255, 34254 },
			{ 35005, 35004, 35003, 35002 },
	};

	/**
	 * The snakeskin rows of {@code CraftingData.leatherData}, which sit on the same five chatbox
	 * rows of the same interface. Used to prove the pottery button table is the real one rather
	 * than a guess — see {@link #theButtonsAreTheSameChatboxRowsAsSnakeskin}.
	 */
	private static final CraftingData.leatherData[] SNAKESKIN_ROWS = {
			CraftingData.leatherData.SNAKESKIN_BODY,
			CraftingData.leatherData.SNAKESKIN_CHAPS,
			CraftingData.leatherData.SNAKESKIN_BANDANA,
			CraftingData.leatherData.SNAKESKIN_BOOTS,
			CraftingData.leatherData.SNAKESKIN_VAMBRACES,
	};

	@Test
	void everyRowIsLookedUpByItsUnfiredId() {
		for (double[] row : TABLE) {
			Pottery.Shape s = Pottery.forUnfired((int) row[0]);
			assertNotNull(s, "no pottery row for unfired " + (int) row[0]);
			assertEquals(row[1], s.getFired(), "fired product for " + (int) row[0]);
			assertEquals(row[2], s.getLevelReq(), "wheel level for " + (int) row[0]);
			assertEquals(row[3], s.getShapeXp(), 1e-9, "shaping xp for " + (int) row[0]);
			assertEquals(row[4], s.getFireXp(), 1e-9, "firing xp for " + (int) row[0]);
		}
	}

	@Test
	void theShapingLevelsAreTheOnesTheGuidePrints() {
		// The Crafting guide's Pottery tab has always listed these five items at 1, 7, 8, 19 and
		// 25, and the OSRS pottery table gives the same figures. A silent edit here would turn the
		// guide back into a promise the server does not keep.
		int[] levels = new int[TABLE.length];
		for (int i = 0; i < TABLE.length; i++) {
			levels[i] = (int) TABLE[i][2];
		}
		assertArrayEqualsInt(new int[] { 1, 7, 8, 19, 25 }, levels);
	}

	@Test
	void firingXpIsTheOvensOwnColumnNotTheWheels() {
		// Three rows differ (pie dish 15 -> 10, bowl 18 -> 15, plant pot 20 -> 17.5), so a copy-paste
		// of the shaping column would be invisible for pot and pot lid and wrong everywhere else.
		assertTrue(Pottery.Shape.PIE_DISH.getFireXp() != Pottery.Shape.PIE_DISH.getShapeXp(),
				"the pie dish pays 15 to shape and 10 to fire");
		assertEquals(17.5, Pottery.Shape.PLANT_POT.getFireXp(), 1e-9,
				"the plant pot's firing figure is the half one, 17.5");
		assertEquals(20, Pottery.Shape.POT_LID.getFireXp(), 1e-9,
				"and the pot lid's two columns happen to be equal, 20 and 20");
	}

	@Test
	void forUnfiredAnswersNullRatherThanThrowingForAnythingElse() {
		// The oven asks about whatever was used on it, so an unrelated id must not throw. A finished
		// item is the case that actually reaches it, from a player firing a stack they just shaped.
		assertNull(Pottery.forUnfired(1931), "a fired pot is not an unfired item");
		assertNull(Pottery.forUnfired(Pottery.SOFT_CLAY), "soft clay is not shaped yet");
		assertNull(Pottery.forUnfired(-1));
		assertNull(Pottery.forUnfired(0));
	}

	@Test
	void noTwoRowsShareAnUnfiredOrAFiredId() {
		// forUnfired returns the first match, so a duplicate unfired id would make the second row
		// unreachable from the oven. A duplicate fired id would make two items make the same thing.
		java.util.Set<Integer> unfired = new java.util.HashSet<>();
		java.util.Set<Integer> fired = new java.util.HashSet<>();
		for (Pottery.Shape s : Pottery.Shape.values()) {
			assertTrue(unfired.add(s.getUnfired()), "duplicate unfired id " + s.getUnfired());
			assertTrue(fired.add(s.getFired()), "duplicate fired id " + s.getFired());
		}
	}

	@Test
	void everyRowSitsOnItsOwnRowOfFourButtons() {
		Pottery.Shape[] shapes = Pottery.Shape.values();
		assertEquals(BUTTONS.length, shapes.length, "one row of buttons per recipe");
		for (int i = 0; i < shapes.length; i++) {
			int[][] buttons = shapes[i].getButtons();
			assertEquals(4, buttons.length, shapes[i] + " should have make 1/5/10/28");
			for (int j = 0; j < 4; j++) {
				assertEquals(BUTTONS[i][j], buttons[j][0], shapes[i] + " button " + j);
				assertEquals(new int[] { 1, 5, 10, 28 }[j], buttons[j][1],
						shapes[i] + " amount for button " + j);
			}
		}
	}

	@Test
	void theButtonsAreTheSameChatboxRowsAsSnakeskin() {
		// This is what justifies rows 4 and 5 of the button table: they are the two ids 2006Redone
		// never wired up, and they are taken from this server's own snakeskin rows, which occupy
		// the same chatbox rows of the same interface and are known to work. If either table is
		// ever reordered this fails, which is the point -- the two must keep matching.
		Pottery.Shape[] shapes = Pottery.Shape.values();
		for (int i = 0; i < SNAKESKIN_ROWS.length; i++) {
			for (int[] pair : shapes[i].getButtons()) {
				assertEquals(SNAKESKIN_ROWS[i].getAmount(pair[0]), pair[1],
						shapes[i] + " should be the same chatbox row as " + SNAKESKIN_ROWS[i]);
			}
		}
	}

	@Test
	void noTwoRowsShareAButton() {
		// One button, one recipe. Overlap would make a click ambiguous and forButton would return
		// whichever row happens to be first.
		java.util.Set<Integer> seen = new java.util.HashSet<>();
		for (Pottery.Shape s : Pottery.Shape.values()) {
			for (int[] pair : s.getButtons()) {
				assertTrue(seen.add(pair[0]), "button " + pair[0] + " is used by two rows");
			}
		}
	}

	@Test
	void forButtonFindsEachRow() {
		for (Pottery.Shape s : Pottery.Shape.values()) {
			for (int[] pair : s.getButtons()) {
				assertEquals(s, Pottery.forButton(pair[0]), "button " + pair[0]);
				assertEquals(pair[1], s.getAmount(pair[0]), "amount behind button " + pair[0]);
			}
		}
		assertNull(Pottery.forButton(0), "an unknown button is not a row");
		assertNull(Pottery.forButton(1));
	}

	@Test
	void aNonMatchIsAnAmountOfZeroNotMinusOne() {
		// getAmount is used directly as the make count, so the sentinel has to be a number that
		// cannot be mistaken for "make none of them".
		assertEquals(0, Pottery.Shape.POT.getAmount(34249), "that is the pie dish's button");
	}

	// ---------------------------------------------------------------------------------------
	// The wiring.
	// ---------------------------------------------------------------------------------------

	@Test
	void aClickOnTheWheelIsRegisteredForEveryWheel() {
		for (int wheel : Pottery.WHEEL_OBJECTS) {
			assertTrue(ObjectHandler.isRegistered(wheel, ObjectClick.FIRST),
					"wheel " + wheel + " has no first-click handler");
		}
	}

	@Test
	void softClayIsRegisteredOnEveryWheel() {
		for (int wheel : Pottery.WHEEL_OBJECTS) {
			assertTrue(ItemOnObjectRegistry.isRegistered(Pottery.SOFT_CLAY, wheel),
					"soft clay on wheel " + wheel + " is not registered");
		}
	}

	@Test
	void everyUnfiredRowIsRegisteredOnEveryOven() {
		// A pair missing here reaches the legacy switch in UseItem.ItemonObject and does nothing,
		// which looks identical to the feature never having been written.
		for (int oven : Pottery.OVEN_OBJECTS) {
			for (Pottery.Shape s : Pottery.Shape.values()) {
				assertTrue(ItemOnObjectRegistry.isRegistered(s.getUnfired(), oven),
						"unfired " + s.getUnfired() + " on oven " + oven + " is not registered");
			}
		}
	}

	@Test
	void theOvensClaimNoClicks() {
		// 2643 is the object this server hangs JewelryMaking.mouldInterface on. Claiming its first
		// click for pottery would take gold-bar-on-oven with it and leave jewellery-making dead.
		for (int oven : Pottery.OVEN_OBJECTS) {
			assertFalse(ObjectHandler.isRegistered(oven, ObjectClick.FIRST), "oven " + oven);
			assertFalse(ObjectHandler.isRegistered(oven, ObjectClick.SECOND), "oven " + oven);
			assertFalse(ObjectHandler.isRegistered(oven, ObjectClick.THIRD), "oven " + oven);
		}
	}

	@Test
	void goldBarOnTheOvenIsStillLeftToTheSwitch() {
		// The pair-keyed registry is what makes the oven stage possible at all: it claims only the
		// fifteen (unfired, oven) combinations, so 2357 on 2643 still falls through to the switch.
		assertFalse(ItemOnObjectRegistry.isRegistered(2357, 2643),
				"gold bar on 2643 must stay in the switch, not be claimed here");
	}

	@Test
	void softClayOnAnOvenIsNotRegistered() {
		// Soft clay is a wheel material. Registering it on an oven would consume it into nothing.
		for (int oven : Pottery.OVEN_OBJECTS) {
			assertFalse(ItemOnObjectRegistry.isRegistered(Pottery.SOFT_CLAY, oven), "oven " + oven);
		}
	}

	@Test
	void aFiredItemIsNotRegisteredAsAnOvenInput() {
		// Firing is one-way. A finished pot on the oven must not be claimable, or the loop could
		// consume its own output.
		for (int oven : Pottery.OVEN_OBJECTS) {
			for (Pottery.Shape s : Pottery.Shape.values()) {
				assertFalse(ItemOnObjectRegistry.isRegistered(s.getFired(), oven),
						"fired " + s.getFired() + " is an output, not an input");
			}
		}
	}

	@Test
	void theObjectIdsAreDistinctAndInTheObjectRange() {
		java.util.Set<Integer> seen = new java.util.HashSet<>();
		for (int id : allObjectIds()) {
			assertTrue(id > 0 && id < Config.ITEM_LIMIT, "object id " + id + " is out of range");
			assertTrue(seen.add(id), "object " + id + " is listed twice");
		}
	}

	private static int[] allObjectIds() {
		int[] all = new int[Pottery.WHEEL_OBJECTS.length + Pottery.OVEN_OBJECTS.length];
		System.arraycopy(Pottery.WHEEL_OBJECTS, 0, all, 0, Pottery.WHEEL_OBJECTS.length);
		System.arraycopy(Pottery.OVEN_OBJECTS, 0, all, Pottery.WHEEL_OBJECTS.length,
				Pottery.OVEN_OBJECTS.length);
		return all;
	}

	@Test
	void theAnimationAndSoundAreTheOvenOnes() {
		// A wrong animation here is a shape with no visual at all, which is the state this whole
		// file was written to leave behind.
		assertEquals(896, Pottery.WHEEL_ANIMATION);
		assertEquals(899, Pottery.OVEN_ANIMATION);
		assertEquals(469, Pottery.OVEN_SOUND);
	}

	// ---------------------------------------------------------------------------------------
	// Behaviour, driven through the real event loop.
	//
	// CycleEventHandler's event list is static and shared with every other test in the run, so
	// these assert only on their own client and stop their own events afterwards.
	// ---------------------------------------------------------------------------------------

	private static final int SLOT = 2;

	private Client lastClient;

	/**
	 * A client with a working out-stream. Frame writers NPE without the encoder a real login
	 * installs, and the inventory mutators re-send the inventory frame.
	 */
	private static Client client() {
		Client c = new Client(null, SLOT);
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

	private Client crafter(int level, int... idAmountPairs) {
		Client c = withItems(idAmountPairs);
		c.skills.playerLevel[Player.playerCrafting] = level;
		return c;
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

	/** Runs the tick loop twice, which is when a two-cycle action does its work. */
	private static void passTwoTicks() {
		CycleEventHandler.process();
		CycleEventHandler.process();
	}

	@AfterEach
	void stopPotteryEvents() {
		if (lastClient != null) {
			CycleEventHandler.stopEvents(lastClient);
		}
		lastClient = null;
	}

	private static void assertArrayEqualsInt(int[] expected, int[] actual) {
		assertEquals(expected.length, actual.length);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], actual[i], "index " + i);
		}
	}

	@Test
	void shapingMakesOneUnfiredItemPerActionAndStopsWhenTheClayRunsOut() {
		Client c = crafter(99, Pottery.SOFT_CLAY, 3);

		Pottery.shape(c, Pottery.Shape.POT, 28);
		for (int i = 0; i < 8; i++) {
			passTwoTicks();
		}

		assertEquals(3, count(c, 1787), "three unfired pots, one per action");
		assertEquals(0, count(c, Pottery.SOFT_CLAY), "the clay is all used");
		assertFalse(c.playerIsCrafting, "and the action has ended");
	}

	@Test
	void shapingStopsAtTheAmountAskedFor() {
		// "Make 5" has to stop at five even with clay left, or the button means nothing.
		Client c = crafter(99, Pottery.SOFT_CLAY, 20);

		Pottery.shape(c, Pottery.Shape.BOWL, 5);
		for (int i = 0; i < 20; i++) {
			passTwoTicks();
		}

		assertEquals(5, count(c, 1791));
		assertEquals(15, count(c, Pottery.SOFT_CLAY), "and the rest of the clay is untouched");
	}

	@Test
	void nothingIsConsumedOnTheTickTheActionIsScheduled() {
		// The pacing is the point of the tick loop: the clay is still there on the tick the action
		// is queued, which is what makes it interruptible.
		Client c = crafter(99, Pottery.SOFT_CLAY, 5);

		Pottery.shape(c, Pottery.Shape.POT, 28);
		CycleEventHandler.process();

		assertEquals(5, count(c, Pottery.SOFT_CLAY), "one tick in, nothing has been shaped");

		CycleEventHandler.process();
		assertEquals(4, count(c, Pottery.SOFT_CLAY), "the second tick is when it shapes");
	}

	@Test
	void shapingBelowTheLevelMakesNothing() {
		Client c = crafter(24, Pottery.SOFT_CLAY, 5); // pot lid asks for 25

		Pottery.select(c, Pottery.Shape.POT_LID.getButtons()[0][0]);
		passTwoTicks();

		assertEquals(0, count(c, 4438), "no level, no unfired pot lid");
		assertEquals(5, count(c, Pottery.SOFT_CLAY), "and no clay is consumed");
		assertFalse(c.playerIsCrafting, "and no action was started");
	}

	@Test
	void theLowestRowIsShapedAtLevelOne() {
		// The guide says 1, so it has to work at 1.
		Client c = crafter(1, Pottery.SOFT_CLAY, 2);

		Pottery.shape(c, Pottery.Shape.POT, 1);
		passTwoTicks();

		assertEquals(1, count(c, 1787));
		assertEquals(1, count(c, Pottery.SOFT_CLAY));
	}

	@Test
	void shapingWithoutClayDoesNothing() {
		Client c = crafter(99, 1931, 5);

		Pottery.shape(c, Pottery.Shape.POT, 5);
		passTwoTicks();

		assertEquals(0, count(c, 1787));
		assertFalse(c.playerIsCrafting, "and nothing is left running");
	}

	@Test
	void aSecondShapeDoesNotStartASecondAction() {
		// playerIsCrafting guards the looping crafters; a double click would otherwise run two
		// loops over one stack and consume twice as fast.
		Client c = crafter(99, Pottery.SOFT_CLAY, 10);

		Pottery.shape(c, Pottery.Shape.POT, 28);
		Pottery.shape(c, Pottery.Shape.POT, 28);
		passTwoTicks();

		assertEquals(1, count(c, 1787), "one action, not two racing each other");
	}

	@Test
	void walkingAwayEndsShapingBeforeItDelivers() {
		Client c = crafter(99, Pottery.SOFT_CLAY, 5);

		Pottery.shape(c, Pottery.Shape.POT, 28);
		c.getPA().resetVariables(); // what every walk step reaches
		passTwoTicks();

		assertEquals(0, count(c, 1787), "a cancelled action must not deliver");
		assertEquals(5, count(c, Pottery.SOFT_CLAY), "and must not consume");
	}

	@Test
	void aWalkFollowedByAFreshShapeDoesNotRunTwoLoops() {
		// Cancelling has to remove the queued event, not just clear the flag. If it only cleared
		// the flag, the old event would wake up on its next tick, find the flag set again by the
		// new action, and shape a second item -- two loops over one stack, at double speed.
		Client c = crafter(99, Pottery.SOFT_CLAY, 10);

		Pottery.shape(c, Pottery.Shape.POT, 28);
		c.getPA().resetVariables();
		Pottery.shape(c, Pottery.Shape.POT, 28);
		passTwoTicks();

		assertEquals(1, count(c, 1787), "one item per action, not two");
		assertEquals(9, count(c, Pottery.SOFT_CLAY));
	}

	@Test
	void spinningAndPotteryCannotRunAtOnce() {
		// Both use playerIsCrafting, and the oven event and the spin event have different ids, so
		// the flag is the only thing keeping two loops off the same inventory.
		Client c = withItems(1779, 5, Pottery.SOFT_CLAY, 5);
		c.skills.playerLevel[Player.playerCrafting] = 99;

		Spinning.spin(c, Spinning.Material.FLAX);
		Pottery.shape(c, Pottery.Shape.POT, 28);
		passTwoTicks();

		assertEquals(1, count(c, 1777), "the spin is running");
		assertEquals(0, count(c, 1787), "and the pottery click was refused");
	}

	@Test
	void walkingAwayStopsBothPotteryAndSpinning() {
		// resetCrafting cancels each skill by its own event id. If either stop were gated on
		// playerIsCrafting, the second cancel to run would see the flag already cleared and leave
		// its own event queued.
		Client c = crafter(99, Pottery.SOFT_CLAY, 5);

		Pottery.shape(c, Pottery.Shape.POT, 28);
		c.getPA().resetVariables();

		Spinning.spin(c, Spinning.Material.FLAX); // irrelevant to this client, just exercises it
		passTwoTicks();
		passTwoTicks();

		assertEquals(0, count(c, 1787), "the pottery loop must be gone, not merely paused");
		assertEquals(5, count(c, Pottery.SOFT_CLAY));
	}

	@Test
	void shapingPaysTheTableXpThroughTheCraftingRate() {
		Client c = crafter(99, Pottery.SOFT_CLAY, 1);
		double before = c.skills.playerXP[Player.playerCrafting];

		Pottery.shape(c, Pottery.Shape.POT, 1);
		passTwoTicks();

		assertEquals((int) (Pottery.Shape.POT.getShapeXp() * Config.CRAFTING_EXPERIENCE),
				c.skills.playerXP[Player.playerCrafting] - before, 1e-9,
				"pottery pays the table xp through the crafting rate like every other crafting table");
	}

	@Test
	void openingTheWheelRemembersWhichMenuIsOpen() {
		Client c = crafter(99, Pottery.SOFT_CLAY, 5);
		c.craftDialogue = true; // stale snakeskin menu

		Pottery.openWheel(c);

		assertTrue(c.potteryDialogue, "the wheel's buttons are live");
		assertFalse(c.craftDialogue, "and the leather menu's are not");
	}

	@Test
	void openingTheWheelWithoutClayIsRefusedAndLeavesNoMenu() {
		Client c = crafter(99, 1931, 1);

		Pottery.openWheel(c);

		assertFalse(c.potteryDialogue, "nothing was opened, so no button should be live");
	}

	@Test
	void theLeatherMenuClosesThePotteryOne() {
		// The other half of the mutual exclusion, and the half a later change is most likely to
		// forget: both menus are interface 8938 with the same four buttons per row.
		Client c = crafter(99, Pottery.SOFT_CLAY, 5);
		c.potteryDialogue = true;

		LeatherMaking.craftLeatherDialogue(c, 1733, 1745); // needle on green dragon leather

		assertTrue(c.craftDialogue, "the leather menu is open");
		assertFalse(c.potteryDialogue, "so the pottery one must not still be live");
	}

	@Test
	void firingMakesOneItemPerActionAndStopsWhenTheStackRunsOut() {
		Client c = crafter(1, 1787, 3);

		Pottery.fire(c, 1787);
		for (int i = 0; i < 8; i++) {
			passTwoTicks();
		}

		assertEquals(3, count(c, 1931), "three pots, one per action");
		assertEquals(0, count(c, 1787), "and the unfired ones are all gone");
		assertFalse(c.playerIsCrafting, "and the action has ended");
	}

	@Test
	void firingHasNoLevelRequirement() {
		// Not a missing check. Only shaping asks for a level; the oven hardens what the player
		// already owns, up to and including a plant pot whose wheel row wants 19.
		Client c = crafter(1, 5352, 1);

		assertTrue(ItemOnObjectRegistry.dispatch(c, 5352, Pottery.OVEN_OBJECTS[0], 0, 0),
				"using it on the oven must be claimed by the registry");
		passTwoTicks();

		assertEquals(1, count(c, 5350), "a level-1 player can still fire it");
	}

	@Test
	void firingPaysTheFireXpNotTheShapingXp() {
		Client c = crafter(99, 1789, 1); // pie dish: 15 to shape, 10 to fire
		double before = c.skills.playerXP[Player.playerCrafting];

		Pottery.fire(c, 1789);
		passTwoTicks();

		assertEquals((int) (Pottery.Shape.PIE_DISH.getFireXp() * Config.CRAFTING_EXPERIENCE),
				c.skills.playerXP[Player.playerCrafting] - before, 1e-9);
	}

	@Test
	void everyOvenFiresThroughTheRegistry() {
		for (int oven : Pottery.OVEN_OBJECTS) {
			Client c = crafter(1, 4438, 1);
			assertTrue(ItemOnObjectRegistry.dispatch(c, 4438, oven, 0, 0),
					"oven " + oven + " should claim an unfired pot lid");
			passTwoTicks();
			assertEquals(1, count(c, 4440), "oven " + oven + " should fire it");
			CycleEventHandler.stopEvents(c);
		}
	}

	@Test
	void anUnrelatedItemOnTheOvenIsNotClaimed() {
		// The oven must not become a universal sink for whatever is used on it.
		for (int oven : Pottery.OVEN_OBJECTS) {
			Client c = crafter(99, 1931, 1);
			assertFalse(ItemOnObjectRegistry.dispatch(c, 1931, oven, 0, 0),
					"a finished pot is not an oven input");
		}
	}
}
