package server.content.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import core.util.ISAACRandomGen;
import server.Config;
import server.content.skills.Fletching.Bolts;
import server.content.skills.Fletching.Fletch;
import server.event.CycleEventHandler;
import server.game.players.Client;
import server.game.players.Player;
import server.game.players.actions.items.ItemUseRegistry;

/**
 * Pins the fletching table and the two facts the ticked rewrite depends on: a product is what
 * names its log, and one log is one action.
 *
 * <p>This is the data half of {@code fletchBow}. The behaviour half — one log consumed per
 * tick, the action ending on a walk — needs a live {@code Client} and an event loop, so what
 * is asserted here is the part that fails silently otherwise: a product id that maps to no
 * log, or two table entries claiming the same product, would show up as a bow that simply
 * cannot be made rather than as an error.
 */
class FletchingTest {

	/** product id -> {log id, level required, xp}, exactly as the enum is written. */
	private static final int[][] TABLE = {
			{ 52, 1511, 1, 5 },   // arrow shafts
			{ 841, 1511, 5, 5 },  // shortbow
			{ 839, 1511, 10, 10 },// longbow
			{ 843, 1521, 20, 17 },// oak shortbow
			{ 845, 1521, 25, 25 },// oak longbow
			{ 849, 1519, 35, 34 },// willow shortbow
			{ 847, 1519, 40, 42 },// willow longbow
			{ 853, 1517, 50, 50 },// maple shortbow
			{ 851, 1517, 55, 59 },// maple longbow
			{ 857, 1515, 65, 68 },// yew shortbow
			{ 855, 1515, 70, 75 },// yew longbow
			{ 861, 1513, 80, 84 },// magic shortbow
			{ 859, 1513, 87, 92 },// magic longbow
	};

	@Test
	void everyProductResolvesToTheLogItIsCutFrom() {
		// The fix in fletchBow is that the log comes from the product rather than from whichever
		// log opened the make-X interface. That only works if this mapping exists for each
		// product, so it is asserted for all of them.
		for (int[] row : TABLE) {
			Fletch f = Fletching.forBow(row[0]);
			assertNotNull(f, "product " + row[0] + " has no table entry");
			assertEquals(row[1], f.getLogID(), "log for product " + row[0]);
			assertEquals(row[2], f.getLevelReq(), "level for product " + row[0]);
			assertEquals(row[3], f.getXp(), "xp for product " + row[0]);
			assertEquals(row[0], f.getBowID(), "the product id must round-trip");
		}
	}

	@Test
	void noTwoProductsAreTheSame() {
		// forBow returns the first match, so a duplicate product id would make the second entry
		// unreachable and the product would silently use the wrong log, level and xp.
		Set<Integer> products = new HashSet<>();
		for (Fletch f : Fletch.values()) {
			assertTrue(products.add(f.getBowID()), "duplicate product id " + f.getBowID() + " in " + f);
		}
	}

	@Test
	void aLogIdIsNotAProductId() {
		// forBow matches on the product, not the log. If the two id spaces ever overlapped, a
		// log could be mistaken for a product and the guards in fletchBow would stop working.
		Set<Integer> logs = new HashSet<>();
		for (Fletch f : Fletch.values()) {
			logs.add(f.getLogID());
		}
		for (Fletch f : Fletch.values()) {
			assertFalse(logs.contains(f.getBowID()),
					"product " + f.getBowID() + " is also used as a log id");
		}
	}

	@Test
	void forBowAnswersNullRatherThanThrowingForAnythingElse() {
		// fletchBow guards on null, so an id that is not a product must not throw. A log id is
		// the case that actually reaches it.
		assertNull(Fletching.forBow(1511), "a log id is not a product");
		assertNull(Fletching.forBow(-1));
		assertNull(Fletching.forBow(0));
		assertNull(Fletching.forBow(3150));
	}

	@Test
	void arrowShaftsAreLevelOneAndFifteenPerLog() {
		Fletch shafts = Fletching.forBow(52);
		assertNotNull(shafts, "the shaft entry must exist");
		assertEquals(1511, shafts.getLogID(), "shafts come from normal logs");
		// The ticked rewrite is the first code to enforce this entry's level, so if it regressed
		// to the 15 it used to carry, low-level players would lose shafts they could always make.
		assertEquals(1, shafts.getLevelReq(), "shafts are level 1 in OSRS");
		assertEquals(15, Fletching.ARROW_SHAFTS_PER_LOG, "one log is fifteen shafts");
	}

	@Test
	void bowLevelRequirementsAreTheKnownValues() {
		// Guards against a level being edited without the change being noticed: the numbers here
		// are the ones the make-X interface advertises, so a silent edit would make the interface
		// lie about what the player can make.
		Map<Integer, Integer> levels = new HashMap<>();
		for (Fletch f : Fletch.values()) {
			levels.put(f.getBowID(), f.getLevelReq());
		}
		assertEquals(5, levels.get(841));
		assertEquals(20, levels.get(843));
		assertEquals(35, levels.get(849));
		assertEquals(50, levels.get(853));
		assertEquals(65, levels.get(857));
		assertEquals(80, levels.get(861));
	}

	@Test
	void theActionIsTickBasedByDefault() {
		// The default the programme asked for: one log per action, not a whole inventory in one
		// call. Flipping this is what reverts to fletchBowInstant.
		assertTrue(Config.FLETCHING_ONE_BY_ONE_ENABLED,
				"fletching should be one-by-one unless deliberately turned off");
	}

	@Test
	void everyBowUsesAnAnimationForCuttingLogs() {
		// Not a per-product animation: the same cut plays for every bow and every shaft. Pinned
		// because a wrong id here is an action with no visual at all.
		assertEquals(1248, Fletching.FLETCH_ANIMATION);
	}

	// ---------------------------------------------------------------------------------------
	// The ticked actions, driven through the real event loop.
	//
	// CycleEventHandler's event list is static and shared with every other test in the run, so
	// these assert only on their own client and stop their own events afterwards.
	// ---------------------------------------------------------------------------------------

	private static final int SLOT = 1;

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

	@AfterEach
	void stopFletchingEvents() {
		// Frees the static event list for the next test and flips the action flag back. Guarded
		// because stopEvents(null) would also match any event whose owner is null.
		if (lastClient != null) {
			CycleEventHandler.stopEvents(lastClient);
		}
		lastClient = null;
	}

	private Client lastClient;

	/** Puts {@code amount} of {@code id} in a slot. Items are stored as id + 1. */
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

	private static int count(Client c, int id) {
		int total = 0;
		for (int i = 0; i < c.playerItems.length; i++) {
			if (c.playerItems[i] == id + 1) {
				total += c.playerItemsN[i];
			}
		}
		return total;
	}

	/** Runs the tick loop twice, which is when a two-cycle fletching action does its work. */
	private static void passTwoTicks() {
		CycleEventHandler.process();
		CycleEventHandler.process();
	}

	@Test
	void forBoltsFindsEveryBoltRecipeByItsUnfinishedBoltId() {
		// The lookup used to match the feather column, so every one of these returned null and
		// only bronze (found via feathers) could be made.
		int[][] expected = {
				{ 819, 877 }, { 820, 9140 }, { 821, 9141 },
				{ 822, 9142 }, { 823, 9143 }, { 824, 9144 },
		};
		for (int[] row : expected) {
			Bolts b = Fletching.forBolts(row[0]);
			assertNotNull(b, "no recipe for unfinished bolt " + row[0]);
			assertEquals(row[1], b.getOutcome(), "outcome for " + row[0]);
			assertEquals(314, b.getItem2(), "every bolt recipe is feathered");
		}
	}

	@Test
	void forBoltsDoesNotAnswerForFeathers() {
		// The exact shape of the old bug: the lookup asked the feather column, so asking about
		// feathers always answered "bronze". A recipe that is not a bolt must read as absent.
		assertNull(Fletching.forBolts(314), "feathers are not an unfinished bolt");
		assertNull(Fletching.forBolts(877), "a finished bolt is not a recipe input");
	}

	@Test
	void boltsAreMadeWhicheverWayRoundTheItemsAreUsed() {
		// Argument order used to decide the outcome: (feathers, bolts) always made bronze and
		// (bolts, feathers) did nothing at all.
		for (int[] order : new int[][] { { 820, 314 }, { 314, 820 } }) {
			Client c = withItems(order[0], 10, order[1], 10);
			c.skills.playerLevel[Player.playerFletching] = 39;

			Fletching.makeBolts(c, order[0], order[1]);
			passTwoTicks();

			assertEquals(10, count(c, 9140), "iron bolts from use order " + order[0] + " then " + order[1]);
			assertEquals(0, count(c, 820), "the unfinished bolts are consumed");
			assertEquals(0, count(c, 314), "the feathers are consumed");
		}
	}

	@Test
	void aWholeStackIsNotConsumedInOneCall() {
		// The point of the phase: a click makes ten bolts, not the whole stack.
		Client c = withItems(819, 40, 314, 40);
		c.skills.playerLevel[Player.playerFletching] = 9;

		Fletching.makeBolts(c, 819, 314);
		passTwoTicks();

		assertEquals(10, count(c, 877), "one action makes exactly ten bolts");
		assertEquals(30, count(c, 819), "and leaves the rest of the stack alone");
	}

	@Test
	void nothingIsConsumedForTwoTicks() {
		// The action is paced by the tick loop, so the materials are still there on the tick it
		// is scheduled on. That is what makes it interruptible at all.
		Client c = withItems(819, 10, 314, 10);
		c.skills.playerLevel[Player.playerFletching] = 9;

		Fletching.makeBolts(c, 819, 314);
		CycleEventHandler.process();

		assertEquals(10, count(c, 819), "one tick in, the action has not run yet");

		CycleEventHandler.process();
		assertEquals(0, count(c, 819), "the second tick is when it runs");
	}

	@Test
	void walkingAwayEndsTheActionBeforeItDelivers() {
		Client c = withItems(819, 10, 314, 10);
		c.skills.playerLevel[Player.playerFletching] = 9;

		Fletching.makeBolts(c, 819, 314);
		c.getPA().resetVariables(); // what every walk step reaches
		passTwoTicks();

		assertEquals(10, count(c, 819), "a cancelled action must not consume anything");
		assertEquals(0, count(c, 877), "and must not deliver the product");
	}

	@Test
	void aLevelBelowTheRequirementMakesNothing() {
		Client c = withItems(819, 10, 314, 10);
		c.skills.playerLevel[Player.playerFletching] = 8; // bronze bolts need 9

		Fletching.makeBolts(c, 819, 314);
		passTwoTicks();

		assertEquals(10, count(c, 819), "no level, no bolt");
		assertEquals(0, count(c, 877));
	}

	@Test
	void arrowsConsumeFifteenOfEachAndYieldFifteen() {
		Client c = withItems(52, 20, 314, 20);
		c.skills.playerLevel[Player.playerFletching] = 1;

		Fletching.makeArrows(c, 52, 314);
		passTwoTicks();

		assertEquals(15, count(c, 53), "shafts and feathers make fifteen headless arrows");
		assertEquals(5, count(c, 52), "fifteen shafts are consumed");
		assertEquals(5, count(c, 314), "fifteen feathers are consumed");
	}

	@Test
	void aBowIsMadePerTickAndStopsWhenTheLogsRunOut() {
		// fletchBow asked for 28 with five logs held: it must make five, one per action, and
		// then stop rather than batching.
		Client c = withItems(1511, 5, 946, 1);
		c.skills.playerLevel[Player.playerFletching] = 5;

		Fletching.fletchBow(c, 841, 28);
		passTwoTicks();

		assertEquals(1, count(c, 841), "one log per action");
		assertEquals(4, count(c, 1511), "and one log consumed");

		for (int i = 0; i < 12; i++) {
			CycleEventHandler.process();
		}
		assertEquals(5, count(c, 841), "it stops when the logs run out");
		assertEquals(0, count(c, 1511));
	}

	@Test
	void oneLogMakesFifteenArrowShafts() {
		Client c = withItems(1511, 1, 946, 1);
		c.skills.playerLevel[Player.playerFletching] = 1;

		Fletching.fletchBow(c, 52, 1);
		passTwoTicks();

		assertEquals(15, count(c, 52), "one log is fifteen shafts");
		assertEquals(0, count(c, 1511), "and it is consumed");
	}

	@Test
	void aBowCannotBeCutWithoutAKnife() {
		Client c = withItems(1511, 5);
		c.skills.playerLevel[Player.playerFletching] = 5;

		Fletching.fletchBow(c, 841, 1);
		passTwoTicks();

		assertEquals(0, count(c, 841), "a knife is required");
		assertEquals(5, count(c, 1511), "and nothing is consumed without one");
	}

	@Test
	void aSecondClickDoesNotStartASecondAction() {
		// playerFletch already guards the batch actions; this pins it for the repeating one,
		// where a double click would otherwise run two overlapping loops over one stack.
		Client c = withItems(1511, 10, 946, 1);
		c.skills.playerLevel[Player.playerFletching] = 5;

		Fletching.fletchBow(c, 841, 10);
		Fletching.fletchBow(c, 841, 10);
		passTwoTicks();

		assertEquals(1, count(c, 841), "one action, not two racing each other");
	}

	// ---------------------------------------------------------------------------------------
	// Bow stringing.
	//
	// The pair is (unstrung bow, bow string), written as {unstrung, strung, level, xp, anim}
	// exactly as the enum is. The composite bow is the only row not in Necrotic's StringingData:
	// it comes from Redone's Stringing, which is why it has no sourced animation of its own.
	// ---------------------------------------------------------------------------------------

	private static final int[][] STRINGING = {
			{ 50, 841, 5, 5, 6678 },
			{ 48, 839, 10, 10, 6684 },
			{ 54, 843, 20, 17, 6679 },
			{ 56, 845, 25, 25, 6685 },
			{ 4825, 4827, 30, 45, 6684 },
			{ 60, 849, 35, 34, 6680 },
			{ 58, 847, 40, 42, 6686 },
			{ 64, 853, 50, 50, 6681 },
			{ 62, 851, 55, 59, 6687 },
			{ 68, 857, 65, 68, 6682 },
			{ 66, 855, 70, 75, 6688 },
			{ 72, 861, 80, 84, 6683 },
			{ 70, 859, 85, 92, 6689 },
	};

	@Test
	void everyStringingRowIsLookedUpInEitherOrder() {
		// The lookup has to be order-blind: the client can send the bowstring as either side of
		// the pair, and a one-sided match would handle half of the clicks.
		for (int[] row : STRINGING) {
			assertEquals(row[0], Fletching.forStringing(row[0], Fletching.BOW_STRING).getUnstrung(),
					"unstrung " + row[0] + " with the string");
			assertEquals(row[0], Fletching.forStringing(Fletching.BOW_STRING, row[0]).getUnstrung(),
					"the string with unstrung " + row[0]);
		}
	}

	@Test
	void everyStringingLevelXpAndAnimationMatchesTheTable() {
		for (int[] row : STRINGING) {
			Fletching.Stringing s = Fletching.forStringing(row[0], Fletching.BOW_STRING);
			assertNotNull(s, "no stringing row for unstrung " + row[0]);
			assertEquals(row[1], s.getStrung(), "strung bow for " + row[0]);
			assertEquals(row[2], s.getLevelReq(), "level for " + row[0]);
			assertEquals(row[3], s.getXp(), "xp for " + row[0]);
			assertEquals(row[4], s.getAnimation(), "animation for " + row[0]);
		}
	}

	@Test
	void stringingXpMatchesCuttingTheSameBow() {
		// Stringing is worth what cutting was worth, per the table copied from Necrotic. If the
		// two ever drift, one of them was edited and the other was not.
		for (int[] row : STRINGING) {
			Fletch cut = Fletching.forBow(row[1]);
			if (cut == null) {
				continue; // the composite bow is not in the cutting table
			}
			assertEquals(cut.getXp(), row[3], "stringing xp for bow " + row[1]);
		}
	}

	@Test
	void everyStringingAnimationIsFromTheSourcedSet() {
		// 6678-6689 is Necrotic's per-bow set. Pinned because the composite bow borrows one of
		// them rather than having its own, and a stray id outside the set would be the shape of
		// an invented number.
		for (Fletching.Stringing s : Fletching.Stringing.values()) {
			assertTrue(s.getAnimation() >= 6678 && s.getAnimation() <= 6689,
					s + " uses animation " + s.getAnimation() + ", which is outside the sourced set");
		}
	}

	@Test
	void noTwoStringingRowsShareAnUnstrungOrStrungBow() {
		// forStringing returns the first match, so a duplicate unstrung id would make the second
		// row unreachable and its bow silently unstringable.
		Set<Integer> unstrung = new HashSet<>();
		Set<Integer> strung = new HashSet<>();
		for (Fletching.Stringing s : Fletching.Stringing.values()) {
			assertTrue(unstrung.add(s.getUnstrung()), "duplicate unstrung id " + s.getUnstrung());
			assertTrue(strung.add(s.getStrung()), "duplicate strung id " + s.getStrung());
		}
	}

	@Test
	void anUnstrungBowIsNotAStrungBow() {
		// A row whose two ids matched would delete the product it was about to add. It would also
		// mean forStringing could match a bow on itself and never consume the string.
		for (Fletching.Stringing s : Fletching.Stringing.values()) {
			assertFalse(s.getUnstrung() == s.getStrung(),
					s + " names the same item on both sides");
		}
	}

	@Test
	void forStringingAnswersNullForPairsThatAreNotABowAndString() {
		// Matching on the unstrung id alone would claim any pair containing an unstrung bow, such
		// as a bow on a chisel. The bowstring has to be the other side.
		assertNull(Fletching.forStringing(70, 1755), "an unstrung bow on a chisel is not stringing");
		assertNull(Fletching.forStringing(1777, 1755), "a bowstring on a chisel is not stringing");
		assertNull(Fletching.forStringing(1777, 1777), "a bowstring on itself is not stringing");
		assertNull(Fletching.forStringing(1511, 1777), "a log and a bowstring is not stringing");
		assertNull(Fletching.forStringing(841, 1777), "an already-strung bow is not stringing");
	}

	@Test
	void everyStringingPairIsRegisteredSoTheLegacyChecksDoNotAlsoRun() {
		// The wiring, not the table: UseItem.ItemonItem returns as soon as dispatch reports the
		// pair claimed. If a row is missing here, the click reaches the legacy body and does
		// nothing, which looks exactly like the feature never having been written.
		for (Fletching.Stringing s : Fletching.Stringing.values()) {
			assertTrue(ItemUseRegistry.isRegistered(s.getUnstrung(), Fletching.BOW_STRING),
					"unstrung " + s.getUnstrung() + " is not registered");
			assertTrue(ItemUseRegistry.isRegistered(Fletching.BOW_STRING, s.getUnstrung()),
					"the reversed order must be the same registration");
		}
	}

	@Test
	void aStringingClickStringifiesThroughTheRegisteredPair() {
		// End to end through the registry, in the reversed order the client may send.
		Client c = withItems(1777, 3, 70, 3);
		c.skills.playerLevel[Player.playerFletching] = 85;

		assertTrue(ItemUseRegistry.dispatch(c, Fletching.BOW_STRING, 70), "the pair must be claimed");
		passTwoTicks();

		assertEquals(1, count(c, 859), "one magic longbow");
		assertEquals(2, count(c, 70), "one unstrung bow consumed");
		assertEquals(2, count(c, 1777), "one bowstring consumed");
	}

	@Test
	void stringingRepeatsUntilTheBowstringsRunOut() {
		// Same shape as fletchBow: one per action, and the action ends when either stack is gone
		// rather than truncating the batch.
		Client c = withItems(1777, 3, 50, 10);
		c.skills.playerLevel[Player.playerFletching] = 5;

		Fletching.stringBow(c, 50, 1777);
		for (int i = 0; i < 8; i++) {
			passTwoTicks();
		}

		assertEquals(3, count(c, 841), "three shortbows, one per action");
		assertEquals(0, count(c, 1777), "the bowstrings are all used");
		assertEquals(7, count(c, 50), "and the unstrung stack stops where the strings did");
	}

	@Test
	void aStringingClickBelowTheLevelConsumesNothing() {
		Client c = withItems(1777, 5, 70, 5);
		c.skills.playerLevel[Player.playerFletching] = 84; // the magic longbow asks for 85

		Fletching.stringBow(c, 70, 1777);
		passTwoTicks();

		assertEquals(0, count(c, 859), "no level, no bow");
		assertEquals(5, count(c, 1777), "and no bowstring is consumed");
		assertEquals(5, count(c, 70));
	}

	@Test
	void stringingWithNoBowstringsDoesNothing() {
		Client c = withItems(50, 5);
		c.skills.playerLevel[Player.playerFletching] = 99;

		Fletching.stringBow(c, 50, 1777);
		passTwoTicks();

		assertEquals(0, count(c, 841));
		assertEquals(5, count(c, 50), "the unstrung bows stay where they are");
	}

	@Test
	void walkingAwayEndsStringingBeforeItDelivers() {
		Client c = withItems(1777, 10, 50, 10);
		c.skills.playerLevel[Player.playerFletching] = 5;

		Fletching.stringBow(c, 50, 1777);
		c.getPA().resetVariables();
		passTwoTicks();

		assertEquals(0, count(c, 841), "a cancelled action must not deliver");
		assertEquals(10, count(c, 1777), "and must not consume");
		assertEquals(10, count(c, 50));
	}
}
