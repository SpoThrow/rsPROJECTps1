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
 * Pins the spinning table, the two ways in, and the ticked loop.
 *
 * <p>The table half fails silently without a test: a wrong product id is a wheel that consumes
 * your wool and gives you nothing, and a wrong level is a guide line that promises something the
 * server will not do. The behaviour half needs a live {@code Client} and the event loop, so it is
 * driven through the real {@code CycleEventHandler} the way the fletching tests are.
 *
 * <p>The two entry points are the part most likely to rot. {@code ObjectHandler} and
 * {@code ItemOnObjectRegistry} are populated by static blocks, so a family that stops being
 * registered is not a compile error — it is a wheel that simply does nothing, which is exactly
 * the bug this class was written to fix.
 */
class SpinningTest {

	/** material id -> {product, level, xp}, exactly as the enum is written. */
	private static final int[][] TABLE = {
			{ 1737, 1759, 1, 3 },
			{ 1779, 1777, 10, 15 },
	};

	@Test
	void everyRowIsLookedUpByItsMaterial() {
		for (int[] row : TABLE) {
			Spinning.Material m = Spinning.forId(row[0]);
			assertNotNull(m, "no spinning row for material " + row[0]);
			assertEquals(row[1], m.getProduct(), "product for " + row[0]);
			assertEquals(row[2], m.getLevelReq(), "level for " + row[0]);
			assertEquals(row[3], m.getXp(), "xp for " + row[0]);
		}
	}

	@Test
	void woolIsTheLevelOneRowAndFlaxIsTheLevelTenOne() {
		// These are the two lines the Crafting guide has always printed for the Spinning tab
		// ("1 Wool", "10 Flax into Bow Strings"). The levels are what makes the guide true, so a
		// silent edit here would turn it back into a promise the server does not keep.
		Spinning.Material wool = Spinning.forId(1737);
		Spinning.Material flax = Spinning.forId(1779);
		assertNotNull(wool, "wool is the material the guide lists at level 1");
		assertEquals(1759, wool.getProduct(), "wool spins into a ball of wool");
		assertEquals(1, wool.getLevelReq());
		assertNotNull(flax, "flax is the material the guide lists at level 10");
		assertEquals(1777, flax.getProduct(), "flax spins into a bow string");
		assertEquals(10, flax.getLevelReq());
	}

	@Test
	void flaxMakesTheBowStringFletchingExpects() {
		// The two skills share this id. If one of them were ever pointed somewhere else the pair
		// would stop meeting in the middle: bow stringing would have nothing to consume.
		assertEquals(Fletching.BOW_STRING, Spinning.forId(1779).getProduct());
	}

	@Test
	void forIdAnswersNullRatherThanThrowingForAnythingElse() {
		// The click path asks about whatever the player is holding, so an unrelated id must not
		// throw. A bow string is the case that actually reaches it, from a player who already
		// spun some.
		assertNull(Spinning.forId(1777), "a finished product is not a material");
		assertNull(Spinning.forId(-1));
		assertNull(Spinning.forId(0));
		assertNull(Spinning.forId(1759), "a ball of wool is the product, not the material");
	}

	@Test
	void noTwoRowsShareAMaterialOrAProduct() {
		// forId returns the first match, so a duplicate material id would make the second row
		// unreachable. A duplicate product would make two materials make the same thing, which is
		// not how either source reads.
		java.util.Set<Integer> materials = new java.util.HashSet<>();
		java.util.Set<Integer> products = new java.util.HashSet<>();
		for (Spinning.Material m : Spinning.Material.values()) {
			assertTrue(materials.add(m.getMaterial()), "duplicate material id " + m.getMaterial());
			assertTrue(products.add(m.getProduct()), "duplicate product id " + m.getProduct());
		}
	}

	@Test
	void everySpinningRowIsRegisteredForItsWheel() {
		// The wiring, not the table. A pair missing here reaches the legacy switch in
		// UseItem.ItemonObject and does nothing, which looks identical to the feature never
		// having been written.
		for (int wheel : Spinning.WHEEL_OBJECTS) {
			for (Spinning.Material m : Spinning.Material.values()) {
				assertTrue(ItemOnObjectRegistry.isRegistered(m.getMaterial(), wheel),
						"item " + m.getMaterial() + " on wheel " + wheel + " is not registered");
			}
		}
	}

	@Test
	void aClickOnTheWheelIsRegisteredForEveryWheel() {
		for (int wheel : Spinning.WHEEL_OBJECTS) {
			assertTrue(ObjectHandler.isRegistered(wheel, ObjectClick.FIRST),
					"wheel " + wheel + " has no first-click handler");
		}
	}

	@Test
	void theMaterialsAreNotRegisteredAsObjects() {
		// They are items. Registering one in ObjectHandler would claim an object id that is not a
		// wheel and that nothing in the world would ever click.
		for (Spinning.Material m : Spinning.Material.values()) {
			assertFalse(ObjectHandler.isRegistered(m.getMaterial(), ObjectClick.FIRST),
					"material " + m.getMaterial() + " is an item, not an object");
		}
	}

	@Test
	void theWheelIdsAreDistinctAndInTheObjectRange() {
		// A duplicate id would throw at class-load and stop the server booting, so the registry
		// already enforces it — but only once every family has run. This pins it earlier and in
		// the file that owns the list.
		java.util.Set<Integer> seen = new java.util.HashSet<>();
		for (int wheel : Spinning.WHEEL_OBJECTS) {
			assertTrue(wheel > 0 && wheel < Config.ITEM_LIMIT, "wheel id " + wheel + " is out of range");
			assertTrue(seen.add(wheel), "wheel " + wheel + " is listed twice");
		}
	}

	// ---------------------------------------------------------------------------------------
	// The ticked loop, driven through the real event loop.
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

	private static int count(Client c, int id) {
		int total = 0;
		for (int i = 0; i < c.playerItems.length; i++) {
			if (c.playerItems[i] == id + 1) {
				total += c.playerItemsN[i];
			}
		}
		return total;
	}

	/** Runs the tick loop twice, which is when a two-cycle spinning action does its work. */
	private static void passTwoTicks() {
		CycleEventHandler.process();
		CycleEventHandler.process();
	}

	@AfterEach
	void stopSpinningEvents() {
		// Frees the static event list for the next test and flips the action flag back. Guarded
		// because stopEvents(null) would also match any event whose owner is null.
		if (lastClient != null) {
			CycleEventHandler.stopEvents(lastClient);
		}
		lastClient = null;
	}

	@Test
	void aSpinMakesOneItemPerActionAndStopsWhenTheMaterialRunsOut() {
		Client c = withItems(1779, 3);
		c.skills.playerLevel[Player.playerCrafting] = 10;

		Spinning.spin(c, Spinning.Material.FLAX);
		for (int i = 0; i < 8; i++) {
			passTwoTicks();
		}

		assertEquals(3, count(c, 1777), "three bow strings, one per action");
		assertEquals(0, count(c, 1779), "the flax is all used");
		assertFalse(c.playerIsCrafting, "and the action has ended");
	}

	@Test
	void nothingIsConsumedOnTheTickTheActionIsScheduled() {
		// The pacing is the point of the tick loop: materials are still there on the tick the
		// action is queued, which is what makes it interruptible.
		Client c = withItems(1779, 5);
		c.skills.playerLevel[Player.playerCrafting] = 10;

		Spinning.spin(c, Spinning.Material.FLAX);
		CycleEventHandler.process();

		assertEquals(5, count(c, 1779), "one tick in, nothing has been spun");

		CycleEventHandler.process();
		assertEquals(4, count(c, 1779), "the second tick is when it spins");
	}

	@Test
	void aSpinBelowTheLevelMakesNothing() {
		Client c = withItems(1779, 5);
		c.skills.playerLevel[Player.playerCrafting] = 9; // flax asks for 10

		Spinning.spin(c, Spinning.Material.FLAX);
		passTwoTicks();

		assertEquals(0, count(c, 1777), "no level, no bow string");
		assertEquals(5, count(c, 1779), "and no flax is consumed");
		assertFalse(c.playerIsCrafting, "and no action was started");
	}

	@Test
	void woolIsSpinnableAtLevelOne() {
		// The guide says 1, so it has to work at 1. Flax is the level-10 row; this pins that the
		// two are not the same row with the same requirement.
		Client c = withItems(1737, 2);
		c.skills.playerLevel[Player.playerCrafting] = 1;

		Spinning.spin(c, Spinning.Material.WOOL);
		passTwoTicks();

		assertEquals(1, count(c, 1759), "one ball of wool");
		assertEquals(1, count(c, 1737));
	}

	@Test
	void spinningWithNoneOfTheMaterialDoesNothing() {
		Client c = withItems(1759, 5);
		c.skills.playerLevel[Player.playerCrafting] = 99;

		Spinning.spin(c, Spinning.Material.FLAX);
		passTwoTicks();

		assertEquals(0, count(c, 1777));
		assertFalse(c.playerIsCrafting, "and nothing is left running");
	}

	@Test
	void aSecondSpinClickDoesNotStartASecondAction() {
		// playerIsCrafting guards the looping crafters; this pins it for spinning, where a double
		// click would otherwise run two loops over one stack and consume twice as fast.
		Client c = withItems(1779, 10);
		c.skills.playerLevel[Player.playerCrafting] = 10;

		Spinning.spin(c, Spinning.Material.FLAX);
		Spinning.spin(c, Spinning.Material.FLAX);
		passTwoTicks();

		assertEquals(1, count(c, 1777), "one action, not two racing each other");
	}

	@Test
	void walkingAwayEndsTheSpinBeforeItDelivers() {
		Client c = withItems(1779, 5);
		c.skills.playerLevel[Player.playerCrafting] = 10;

		Spinning.spin(c, Spinning.Material.FLAX);
		c.getPA().resetVariables(); // what every walk step reaches
		passTwoTicks();

		assertEquals(0, count(c, 1777), "a cancelled action must not deliver");
		assertEquals(5, count(c, 1779), "and must not consume");
	}

	@Test
	void aWalkFollowedByAFreshSpinDoesNotRunTwoLoops() {
		// Cancelling has to remove the queued event, not just clear the flag. If it only cleared
		// the flag, the old event would wake up on its next tick, find the flag set again by the
		// new action, and spin a second item — two loops over one stack, at double speed.
		Client c = withItems(1779, 10);
		c.skills.playerLevel[Player.playerCrafting] = 10;

		Spinning.spin(c, Spinning.Material.FLAX);
		c.getPA().resetVariables();
		Spinning.spin(c, Spinning.Material.FLAX);
		passTwoTicks();

		assertEquals(1, count(c, 1777), "one item per action, not two");
		assertEquals(9, count(c, 1779));
	}

	@Test
	void theXpIsTheTableValueTimesTheCraftingRate() {
		Client c = withItems(1779, 1);
		c.skills.playerLevel[Player.playerCrafting] = 10;
		double before = c.skills.playerXP[Player.playerCrafting];

		Spinning.spin(c, Spinning.Material.FLAX);
		passTwoTicks();

		assertEquals(Spinning.Material.FLAX.getXp() * Config.CRAFTING_EXPERIENCE,
				c.skills.playerXP[Player.playerCrafting] - before, 1e-9,
				"spinning pays the table xp through the crafting rate like every other crafting table");
	}

	@Test
	void clickingTheWheelSpinsTheOneMaterialYouAreCarrying() {
		for (int[] row : TABLE) {
			Client c = withItems(row[0], 1);
			c.skills.playerLevel[Player.playerCrafting] = 99;

			Spinning.openWheel(c);
			passTwoTicks();

			assertEquals(1, count(c, row[1]), "clicking the wheel with only " + row[0]
					+ " should make " + row[1]);
		}
	}

	@Test
	void clickingTheWheelWithBothMaterialsChoosesNeither() {
		// The click does not guess. If it did, a player carrying both who wanted wool would watch
		// a whole stack of flax turn into bow strings.
		Client c = withItems(1737, 5, 1779, 5);
		c.skills.playerLevel[Player.playerCrafting] = 99;

		Spinning.openWheel(c);
		passTwoTicks();

		assertEquals(0, count(c, 1759), "nothing is spun");
		assertEquals(0, count(c, 1777));
		assertEquals(5, count(c, 1737), "and nothing is consumed");
		assertEquals(5, count(c, 1779));
		assertFalse(c.playerIsCrafting, "and no action is left running");
	}

	@Test
	void clickingTheWheelWithNothingSpinnableDoesNothing() {
		Client c = withItems(1759, 1, 1777, 1);
		c.skills.playerLevel[Player.playerCrafting] = 99;

		Spinning.openWheel(c);
		passTwoTicks();

		assertFalse(c.playerIsCrafting);
		assertEquals(1, count(c, 1759), "the products are left alone");
		assertEquals(1, count(c, 1777));
	}

	@Test
	void usingWoolOnTheWheelSpinsWoolEvenWithFlaxHeld() {
		// The explicit half of the pair: this is how a player carrying both picks wool.
		Client c = withItems(1737, 3, 1779, 3);
		c.skills.playerLevel[Player.playerCrafting] = 99;

		assertTrue(ItemOnObjectRegistry.dispatch(c, 1737, Spinning.WHEEL_OBJECTS[0], 0, 0),
				"the pair must be claimed by the registry");
		passTwoTicks();

		assertEquals(1, count(c, 1759), "the wool is spun");
		assertEquals(0, count(c, 1777), "and the flax is untouched");
		assertEquals(3, count(c, 1779));
	}

	@Test
	void usingFlaxOnTheWheelSpinsFlaxThroughTheRegistry() {
		Client c = withItems(1737, 3, 1779, 3);
		c.skills.playerLevel[Player.playerCrafting] = 10;

		assertTrue(ItemOnObjectRegistry.dispatch(c, 1779, Spinning.WHEEL_OBJECTS[1], 0, 0),
				"the pair must be claimed by the registry");
		passTwoTicks();

		assertEquals(1, count(c, 1777), "the flax is spun");
		assertEquals(0, count(c, 1759), "and the wool is untouched");
		assertEquals(3, count(c, 1737));
	}

	@Test
	void theAnimationIsTheSpinningWheelOne() {
		// Not per-material: wool and flax both use the wheel, and Redone and Necrotic both use 896
		// for both. Pinned because a wrong id here is a spin with no visual at all.
		assertEquals(896, Spinning.SPIN_ANIMATION);
	}
}
