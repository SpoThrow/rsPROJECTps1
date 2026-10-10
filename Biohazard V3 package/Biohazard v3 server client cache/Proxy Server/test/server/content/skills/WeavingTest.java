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
 * Pins the loom table, the batch size, and the two ways in.
 *
 * <p>Weaving is a two-recipe skill behind a guide tab that has been advertising it, so the failure
 * mode this guards against is not a wrong number but a pair: the material, the amount and the
 * product all have to line up or the loom eats a player's wool and hands back nothing.
 */
class WeavingTest {

	/** material -> {product, level, xp, amount}, exactly as the enum is written. */
	private static final int[][] TABLE = {
			{ 1759, 3224, 10, 12, 4 },   // four balls of wool -> cloth
			{ 5931, 5418, 21, 38, 4 },   // four jute fibres -> empty sack
	};

	@Test
	void everyRowIsLookedUpByItsMaterial() {
		for (int[] row : TABLE) {
			Weaving.Weave w = Weaving.forMaterial(row[0]);
			assertNotNull(w, "no weaving row for material " + row[0]);
			assertEquals(row[1], w.getProduct(), "product for " + row[0]);
			assertEquals(row[2], w.getLevelReq(), "level for " + row[0]);
			assertEquals(row[3], w.getXp(), "xp for " + row[0]);
			assertEquals(row[4], w.getAmount(), "amount per action for " + row[0]);
		}
	}

	@Test
	void bothRowsAreTheOnesTheGuidePrints() {
		// The Crafting guide's Weaving tab lists exactly these two, at 10 and 21.
		Weaving.Weave cloth = Weaving.forMaterial(1759);
		Weaving.Weave sack = Weaving.forMaterial(5931);
		assertEquals(3224, cloth.getProduct(), "wool weaves into cloth");
		assertEquals(10, cloth.getLevelReq());
		assertEquals(5418, sack.getProduct(), "jute weaves into an empty sack");
		assertEquals(21, sack.getLevelReq());
	}

	@Test
	void theJuteIsTheItemItsExamineTextNames() {
		// Item 5931's examine is "I can weave this to make sacks." That sentence was pointing at a
		// loom that did not do anything; this is the row that makes it true.
		assertNotNull(Weaving.forMaterial(5931), "jute fibre must be weavable");
		assertEquals(5418, Weaving.forMaterial(5931).getProduct());
	}

	@Test
	void everyRowEatsABatchNotOneItem() {
		// Four in, one out. If the amount column were ever set to 1 the skill would still "work"
		// and would quietly hand out cloth for a quarter of its cost.
		for (int[] row : TABLE) {
			assertTrue(row[4] > 1, "material " + row[0] + " should be consumed as a batch");
		}
	}

	@Test
	void theClickMessageCountsWhatTheTableCounts() {
		// clickLoom's refusal message says "four balls of wool or four jute fibres" in words,
		// because it is a sentence a player reads. That makes it a second copy of the amount
		// column, so it is pinned here: change an amount and this fails before a player is told
		// the wrong number.
		assertEquals(4, Weaving.forMaterial(1759).getAmount());
		assertEquals(4, Weaving.forMaterial(5931).getAmount());
	}

	@Test
	void forMaterialAnswersNullRatherThanThrowingForAnythingElse() {
		// The loom asks about whatever was used on it, so an unrelated id must not throw. Cloth is
		// the case that actually reaches it, from a player who already wove some.
		assertNull(Weaving.forMaterial(3224), "cloth is the product, not a material");
		assertNull(Weaving.forMaterial(-1));
		assertNull(Weaving.forMaterial(0));
		assertNull(Weaving.forMaterial(1761), "soft clay is a potter's problem");
	}

	@Test
	void noTwoRowsShareAMaterialOrAProduct() {
		java.util.Set<Integer> materials = new java.util.HashSet<>();
		java.util.Set<Integer> products = new java.util.HashSet<>();
		for (Weaving.Weave w : Weaving.Weave.values()) {
			assertTrue(materials.add(w.getMaterial()), "duplicate material " + w.getMaterial());
			assertTrue(products.add(w.getProduct()), "duplicate product " + w.getProduct());
		}
	}

	@Test
	void aClickOnTheLoomIsRegisteredForEveryLoom() {
		for (int loom : Weaving.LOOM_OBJECTS) {
			assertTrue(ObjectHandler.isRegistered(loom, ObjectClick.FIRST),
					"loom " + loom + " has no first-click handler");
		}
	}

	@Test
	void everyMaterialIsRegisteredOnEveryLoom() {
		for (int loom : Weaving.LOOM_OBJECTS) {
			for (Weaving.Weave w : Weaving.Weave.values()) {
				assertTrue(ItemOnObjectRegistry.isRegistered(w.getMaterial(), loom),
						"item " + w.getMaterial() + " on loom " + loom + " is not registered");
			}
		}
	}

	@Test
	void theMaterialsAreNotRegisteredAsObjects() {
		for (Weaving.Weave w : Weaving.Weave.values()) {
			assertFalse(ObjectHandler.isRegistered(w.getMaterial(), ObjectClick.FIRST),
					"material " + w.getMaterial() + " is an item, not an object");
		}
	}

	@Test
	void aProductIsNotRegisteredAsALoomInput() {
		// Weaving is one-way, and cloth must not be feedable back into the loom.
		for (int loom : Weaving.LOOM_OBJECTS) {
			for (Weaving.Weave w : Weaving.Weave.values()) {
				assertFalse(ItemOnObjectRegistry.isRegistered(w.getProduct(), loom),
						"product " + w.getProduct() + " is an output, not an input");
			}
		}
	}

	@Test
	void theLoomIdsAreDistinctAndInTheObjectRange() {
		java.util.Set<Integer> seen = new java.util.HashSet<>();
		for (int loom : Weaving.LOOM_OBJECTS) {
			assertTrue(loom > 0 && loom < Config.ITEM_LIMIT, "loom id " + loom + " is out of range");
			assertTrue(seen.add(loom), "loom " + loom + " is listed twice");
		}
	}

	@Test
	void theAnimationIsTheSpinningWheelOne() {
		// Borrowed deliberately: neither reference server implements a loom, so neither has a loom
		// animation to copy. Pinned so the borrow is visible rather than mistaken for a found id.
		assertEquals(Spinning.SPIN_ANIMATION, Weaving.WEAVE_ANIMATION);
	}

	// ---------------------------------------------------------------------------------------
	// Behaviour, driven through the real event loop.
	// ---------------------------------------------------------------------------------------

	private static final int SLOT = 2;

	private Client lastClient;

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

	private static void passTwoTicks() {
		CycleEventHandler.process();
		CycleEventHandler.process();
	}

	@AfterEach
	void stopWeavingEvents() {
		if (lastClient != null) {
			CycleEventHandler.stopEvents(lastClient);
		}
		lastClient = null;
	}

	@Test
	void weavingMakesOneItemPerActionAndEatsFourMaterials() {
		Client c = crafter(10, 1759, 12);

		Weaving.weave(c, Weaving.Weave.CLOTH);
		for (int i = 0; i < 8; i++) {
			passTwoTicks();
		}

		assertEquals(3, count(c, 3224), "twelve wool is three cloth");
		assertEquals(0, count(c, 1759), "and all twelve are gone");
		assertFalse(c.playerIsCrafting, "and the action has ended");
	}

	@Test
	void weavingStopsWhileFourMaterialsAreLeft() {
		// The batch boundary is where a loop that deleted one item per tick would still "work".
		Client c = crafter(10, 1759, 7);

		Weaving.weave(c, Weaving.Weave.CLOTH);
		for (int i = 0; i < 8; i++) {
			passTwoTicks();
		}

		assertEquals(1, count(c, 3224), "seven wool is one cloth");
		assertEquals(3, count(c, 1759), "and three are left, not enough for a second");
		assertFalse(c.playerIsCrafting, "so the loop has stopped");
	}

	@Test
	void nothingIsConsumedOnTheTickTheActionIsScheduled() {
		Client c = crafter(10, 1759, 8);

		Weaving.weave(c, Weaving.Weave.CLOTH);
		CycleEventHandler.process();

		assertEquals(8, count(c, 1759), "one tick in, nothing has been woven");

		CycleEventHandler.process();
		assertEquals(4, count(c, 1759), "the second tick is when four are consumed");
	}

	@Test
	void weavingBelowTheLevelMakesNothing() {
		Client c = crafter(9, 1759, 8); // cloth asks for 10

		Weaving.weave(c, Weaving.Weave.CLOTH);
		passTwoTicks();

		assertEquals(0, count(c, 3224), "no level, no cloth");
		assertEquals(8, count(c, 1759), "and no wool is consumed");
		assertFalse(c.playerIsCrafting, "and no action was started");
	}

	@Test
	void theSackIsRefusedWithOnlyThreeJute() {
		Client c = crafter(99, 5931, 3);

		Weaving.weave(c, Weaving.Weave.EMPTY_SACK);
		passTwoTicks();

		assertEquals(0, count(c, 5418));
		assertEquals(3, count(c, 5931), "three is not a batch");
		assertFalse(c.playerIsCrafting);
	}

	@Test
	void theSackIsWovenAtItsLevel() {
		Client c = crafter(21, 5931, 4);

		Weaving.weave(c, Weaving.Weave.EMPTY_SACK);
		passTwoTicks();

		assertEquals(1, count(c, 5418), "one empty sack");
		assertEquals(0, count(c, 5931));
	}

	@Test
	void aSecondWeaveDoesNotStartASecondAction() {
		Client c = crafter(99, 1759, 20);

		Weaving.weave(c, Weaving.Weave.CLOTH);
		Weaving.weave(c, Weaving.Weave.CLOTH);
		passTwoTicks();

		assertEquals(1, count(c, 3224), "one action, not two racing each other");
	}

	@Test
	void walkingAwayEndsTheWeaveBeforeItDelivers() {
		Client c = crafter(99, 1759, 8);

		Weaving.weave(c, Weaving.Weave.CLOTH);
		c.getPA().resetVariables(); // what every walk step reaches
		passTwoTicks();

		assertEquals(0, count(c, 3224), "a cancelled action must not deliver");
		assertEquals(8, count(c, 1759), "and must not consume");
	}

	@Test
	void aWalkFollowedByAFreshWeaveDoesNotRunTwoLoops() {
		Client c = crafter(99, 1759, 20);

		Weaving.weave(c, Weaving.Weave.CLOTH);
		c.getPA().resetVariables();
		Weaving.weave(c, Weaving.Weave.CLOTH);
		passTwoTicks();

		assertEquals(1, count(c, 3224), "one action's worth, not two");
		assertEquals(16, count(c, 1759));
	}

	@Test
	void walkingAwayStopsSpinningPotteryAndWeavingTogether() {
		// resetCrafting cancels all three by their own event ids. Any of the three stops being
		// unconditional and the one that runs second leaves its loop queued behind a cleared flag.
		Client c = crafter(99, 1759, 8);

		Weaving.weave(c, Weaving.Weave.CLOTH);
		c.getPA().resetVariables();
		passTwoTicks();
		passTwoTicks();

		assertEquals(0, count(c, 3224), "the weave must be gone, not merely paused");
		assertEquals(8, count(c, 1759));
	}

	@Test
	void weavingPaysTheTableXpThroughTheCraftingRate() {
		Client c = crafter(99, 5931, 4);
		double before = c.skills.playerXP[Player.playerCrafting];

		Weaving.weave(c, Weaving.Weave.EMPTY_SACK);
		passTwoTicks();

		assertEquals(Weaving.Weave.EMPTY_SACK.getXp() * Config.CRAFTING_EXPERIENCE,
				c.skills.playerXP[Player.playerCrafting] - before, 1e-9);
	}

	@Test
	void clickingTheLoomWeavesTheOneMaterialYouAreCarrying() {
		for (int[] row : TABLE) {
			Client c = crafter(99, row[0], row[4]);

			Weaving.clickLoom(c);
			passTwoTicks();

			assertEquals(1, count(c, row[1]), "clicking the loom with only " + row[0]
					+ " should make " + row[1]);
		}
	}

	@Test
	void clickingTheLoomWithBothMaterialsChoosesNeither() {
		Client c = crafter(99, 1759, 4, 5931, 4);

		Weaving.clickLoom(c);
		passTwoTicks();

		assertEquals(0, count(c, 3224), "nothing is woven");
		assertEquals(0, count(c, 5418));
		assertEquals(4, count(c, 1759), "and nothing is consumed");
		assertEquals(4, count(c, 5931));
		assertFalse(c.playerIsCrafting, "and no action is left running");
	}

	@Test
	void clickingTheLoomWithoutABatchDoesNothing() {
		// Three wool is one short of the batch and one jute is three short, so neither row is a
		// choice — the click must not take the three as "close enough".
		Client c = crafter(99, 1759, 3, 5931, 1);

		Weaving.clickLoom(c);
		passTwoTicks();

		assertEquals(0, count(c, 3224));
		assertFalse(c.playerIsCrafting);
	}

	@Test
	void usingWoolOnTheLoomWeavesWoolEvenWithJuteHeld() {
		// The explicit half of the pair: this is how a player carrying both picks wool.
		Client c = crafter(10, 1759, 8, 5931, 8);

		assertTrue(ItemOnObjectRegistry.dispatch(c, 1759, Weaving.LOOM_OBJECTS[0], 0, 0),
				"the pair must be claimed by the registry");
		passTwoTicks();

		assertEquals(1, count(c, 3224), "the wool is woven");
		assertEquals(0, count(c, 5418), "and the jute is untouched");
		assertEquals(8, count(c, 5931));
	}

	@Test
	void usingJuteOnTheLoomWeavesItThroughTheRegistry() {
		Client c = crafter(21, 1759, 8, 5931, 8);

		assertTrue(ItemOnObjectRegistry.dispatch(c, 5931, Weaving.LOOM_OBJECTS[1], 0, 0),
				"the pair must be claimed by the registry");
		passTwoTicks();

		assertEquals(1, count(c, 5418), "the jute is woven");
		assertEquals(0, count(c, 3224), "and the wool is untouched");
		assertEquals(8, count(c, 1759));
	}

	@Test
	void spinningAndWeavingCannotRunAtOnce() {
		// Both use playerIsCrafting with different event ids, so the flag is the only thing keeping
		// two loops off the same inventory.
		Client c = withItems(1779, 5, 1759, 8);
		c.skills.playerLevel[Player.playerCrafting] = 99;

		Spinning.spin(c, Spinning.Material.FLAX);
		Weaving.weave(c, Weaving.Weave.CLOTH);
		passTwoTicks();

		assertEquals(1, count(c, 1777), "the spin is running");
		assertEquals(0, count(c, 3224), "and the weave was refused");
	}
}
