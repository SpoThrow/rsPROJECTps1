package server.content.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import core.util.ISAACRandomGen;
import server.Config;
import server.game.players.Client;
import server.game.players.Player;
import server.game.players.actions.items.ItemOnObjectRegistry;
import server.game.players.actions.items.ItemUseRegistry;

/**
 * Pins the water table, the registry wiring, and the fact that this is what the potter's wheel reads.
 *
 * <p>The failure this guards against is not a wrong number but an unreachable feature: {@code 1761}
 * existed, the wheel read it, and nothing made it. A test that only checked "clay plus water equals
 * soft clay" would pass with the registration missing entirely, so the pair is driven through
 * {@link ItemUseRegistry} and the wheel is asked about the product.
 */
class SoftClayTest {

	/** full -> empty, exactly as the enum is written, for the three plain pairs. */
	private static final int[][] PLAIN_PAIRS = {
			{ 1929, 1925 }, // bucket of water -> bucket
			{ 1937, 1935 }, // jug of water -> jug
			{ 227, 229 },   // vial of water -> vial
	};

	/** The waterskin is a ladder, not a pair: four doses down to the empty skin. */
	private static final int[] WATERSKIN_LADDER = { 1823, 1825, 1827, 1829, 1831 };

	// ---------------------------------------------------------------------------------------
	// The table.
	// ---------------------------------------------------------------------------------------

	@Test
	void everyContainerHasAnEmptyFormThatIsNotItself() {
		for (SoftClay.WaterContainer container : SoftClay.WaterContainer.values()) {
			assertTrue(container.getEmpty() > 0, "container " + container + " has no empty form");
			assertNotEquals(container.getFull(), container.getEmpty(),
					"container " + container + " empties into itself");
		}
	}

	@Test
	void theThreePlainContainersAreTheItemTablePairings() {
		assertEquals(3, PLAIN_PAIRS.length);
		for (int[] pair : PLAIN_PAIRS) {
			SoftClay.WaterContainer container = SoftClay.forWater(pair[0]);
			assertNotNull(container, "id " + pair[0] + " is not a water source");
			assertEquals(pair[1], container.getEmpty(), "empty form of " + pair[0]);
		}
	}

	@Test
	void thePlainEmptyFormsAreDeadEnds() {
		// A bucket, a jug and a vial do not hold water once emptied, so they must not be water
		// sources themselves. Without this a click on the empty could loop the action.
		for (int[] pair : PLAIN_PAIRS) {
			assertNull(SoftClay.forWater(pair[1]), "empty " + pair[1] + " must not be water");
		}
	}

	@Test
	void theWaterskinIsALadderThatEndsAtTheEmptySkin() {
		for (int i = 0; i < WATERSKIN_LADDER.length - 1; i++) {
			SoftClay.WaterContainer container = SoftClay.forWater(WATERSKIN_LADDER[i]);
			assertNotNull(container, "dose " + WATERSKIN_LADDER[i] + " is not a water source");
			assertEquals(WATERSKIN_LADDER[i + 1], container.getEmpty(),
					"dose " + WATERSKIN_LADDER[i] + " should step down one");
		}
		// The last step is the skin with nothing in it, which is where the ladder stops.
		assertNull(SoftClay.forWater(WATERSKIN_LADDER[WATERSKIN_LADDER.length - 1]),
				"an empty waterskin must not be usable");
	}

	@Test
	void forWaterAnswersNullForAnythingElse() {
		// The registry asks about whatever was used, so an unrelated id must not throw: clay itself
		// is the case that actually reaches here, from a player who used two clays together.
		assertNull(SoftClay.forWater(SoftClay.CLAY), "clay is not water");
		assertNull(SoftClay.forWater(SoftClay.SOFT_CLAY));
		assertNull(SoftClay.forWater(0));
		assertNull(SoftClay.forWater(-1));
	}

	@Test
	void noTwoRowsShareAnId() {
		Set<Integer> full = new HashSet<>();
		Set<Integer> empty = new HashSet<>();
		for (SoftClay.WaterContainer container : SoftClay.WaterContainer.values()) {
			assertTrue(full.add(container.getFull()), "duplicate full form " + container.getFull());
			// Empties are compared only against other empties: 1825 is both the third dose of a
			// waterskin and what a full one becomes, and that is the ladder working as intended.
			assertTrue(empty.add(container.getEmpty()), "duplicate empty form " + container.getEmpty());
		}
	}

	@Test
	void everyReferencedIdIsInTheItemRange() {
		assertTrue(SoftClay.CLAY > 0 && SoftClay.CLAY < Config.ITEM_LIMIT);
		assertTrue(SoftClay.SOFT_CLAY > 0 && SoftClay.SOFT_CLAY < Config.ITEM_LIMIT);
		for (SoftClay.WaterContainer container : SoftClay.WaterContainer.values()) {
			assertTrue(container.getFull() > 0 && container.getFull() < Config.ITEM_LIMIT);
			assertTrue(container.getEmpty() > 0 && container.getEmpty() < Config.ITEM_LIMIT);
		}
	}

	// ---------------------------------------------------------------------------------------
	// The wiring.
	// ---------------------------------------------------------------------------------------

	@Test
	void everyContainerIsRegisteredAgainstClayInBothOrders() {
		for (SoftClay.WaterContainer container : SoftClay.WaterContainer.values()) {
			assertTrue(ItemUseRegistry.isRegistered(SoftClay.CLAY, container.getFull()),
					"clay on " + container.getFull() + " is not registered");
			assertTrue(ItemUseRegistry.isRegistered(container.getFull(), SoftClay.CLAY),
					"the reversed lookup must agree for " + container.getFull());
		}
	}

	@Test
	void unrelatedPairsAreNotClaimed() {
		// The registry throws on a duplicate registration, and it is shared with fletching; these
		// are the pairs it must not be holding on soft clay's behalf.
		assertFalse(ItemUseRegistry.isRegistered(SoftClay.CLAY, SoftClay.CLAY));
		assertFalse(ItemUseRegistry.isRegistered(SoftClay.CLAY, SoftClay.SOFT_CLAY));
		assertFalse(ItemUseRegistry.isRegistered(1929, 1925), "an empty bucket is not a recipe");
	}

	@Test
	void theProductIsNotAWaterSourceAndCannotBeFedBackIn() {
		assertNull(SoftClay.forWater(SoftClay.SOFT_CLAY));
		assertFalse(ItemUseRegistry.isRegistered(SoftClay.SOFT_CLAY, SoftClay.CLAY));
	}

	@Test
	void theWheelAcceptsThisItemAndTheTwoConstantsAgree() {
		// This is the reachability claim, not a tidiness check. The Pottery tab is entered from
		// 1761 and the wheel reads it; the two classes naming the id separately is exactly how the
		// wheel ended up reading something nothing produced.
		assertEquals(Pottery.SOFT_CLAY, SoftClay.SOFT_CLAY, "the wheel and this must name one item");
		for (int wheel : Pottery.WHEEL_OBJECTS) {
			assertTrue(ItemOnObjectRegistry.isRegistered(SoftClay.SOFT_CLAY, wheel),
					"soft clay on wheel " + wheel + " is not registered, so this still leads nowhere");
		}
	}

	@Test
	void clayIsWhatTheOvenStagePretendsItIs() {
		// The unfired ids are the wheel's output, not this action's; 434 must not have leaked into
		// the pottery table as a material the wheel reads.
		assertNull(Pottery.forUnfired(SoftClay.CLAY), "clay is the wheel's input, not its output");
	}

	// ---------------------------------------------------------------------------------------
	// Behaviour.
	// ---------------------------------------------------------------------------------------

	private static final int SLOT = 4;

	private static Client client() {
		Client c = new Client(null, SLOT);
		c.getOutStream().packetEncryption = new ISAACRandomGen(new int[] { 1, 2, 3, 4 });
		c.expModifier = 1;
		return c;
	}

	private static Client withItems(int... idAmountPairs) {
		Client c = client();
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

	@Test
	void waterOnClayYieldsOneSoftClayAndAnEmptyContainer() {
		Client c = withItems(SoftClay.CLAY, 1, 1929, 1);
		SoftClay.mix(c, SoftClay.CLAY, 1929);
		assertEquals(1, count(c, SoftClay.SOFT_CLAY));
		assertEquals(0, count(c, SoftClay.CLAY), "the clay is consumed");
		assertEquals(0, count(c, 1929), "the water is gone");
		assertEquals(1, count(c, 1925), "the bucket is left");
	}

	@Test
	void theReversedClickIsTheSameAction() {
		Client c = withItems(SoftClay.CLAY, 1, 1929, 1);
		SoftClay.mix(c, 1929, SoftClay.CLAY);
		assertEquals(1, count(c, SoftClay.SOFT_CLAY));
		assertEquals(1, count(c, 1925));
	}

	@Test
	void theClickThroughTheRegistryIsTheSameAction() {
		// The one test that would fail if SoftClayItemUses were never called from the registry's
		// static block, which is the actual defect this table was written to fix.
		Client c = withItems(SoftClay.CLAY, 1, 1937, 1);
		assertTrue(ItemUseRegistry.dispatch(c, 1937, SoftClay.CLAY),
				"the registry must claim (clay, jug of water)");
		assertEquals(1, count(c, SoftClay.SOFT_CLAY));
		assertEquals(1, count(c, 1935), "the jug is left");
	}

	@Test
	void oneClickSoftensOneClayAndLeavesTheStack() {
		Client c = withItems(SoftClay.CLAY, 5, 1929, 3);
		SoftClay.mix(c, SoftClay.CLAY, 1929);
		assertEquals(1, count(c, SoftClay.SOFT_CLAY), "one clay per click");
		assertEquals(4, count(c, SoftClay.CLAY));
		assertEquals(2, count(c, 1929));
		assertEquals(1, count(c, 1925));
	}

	@Test
	void theWaterskinStepsDownOneDosePerClick() {
		Client c = withItems(SoftClay.CLAY, 5, 1823, 1);
		for (int i = 0; i < WATERSKIN_LADDER.length - 1; i++) {
			SoftClay.mix(c, SoftClay.CLAY, WATERSKIN_LADDER[i]);
			assertEquals(0, count(c, WATERSKIN_LADDER[i]),
					"dose " + WATERSKIN_LADDER[i] + " should be consumed");
			assertEquals(1, count(c, WATERSKIN_LADDER[i + 1]),
					"dose " + WATERSKIN_LADDER[i] + " should leave " + WATERSKIN_LADDER[i + 1]);
		}
		assertEquals(4, count(c, SoftClay.SOFT_CLAY));
		assertEquals(1, count(c, SoftClay.CLAY), "one clay must be left for the next assertion");

		// The empty skin is held, there is clay to spend, and nothing happens.
		SoftClay.mix(c, SoftClay.CLAY, 1831);
		assertEquals(4, count(c, SoftClay.SOFT_CLAY), "an empty waterskin must not soften anything");
		assertEquals(1, count(c, SoftClay.CLAY));
		assertEquals(1, count(c, 1831));
	}

	@Test
	void aPlayerWithNoClayGetsNothing() {
		Client c = withItems(1929, 1);
		SoftClay.mix(c, SoftClay.CLAY, 1929);
		assertEquals(0, count(c, SoftClay.SOFT_CLAY));
		assertEquals(1, count(c, 1929), "the water must not be poured out for nothing");
		assertEquals(0, count(c, 1925));
	}

	@Test
	void aPlayerWithNoWaterGetsNothing() {
		Client c = withItems(SoftClay.CLAY, 1);
		SoftClay.mix(c, SoftClay.CLAY, 1929);
		assertEquals(0, count(c, SoftClay.SOFT_CLAY));
		assertEquals(1, count(c, SoftClay.CLAY));
	}

	@Test
	void aFullPackStillSoftensClay() {
		// Both slots are used by the pair itself, so a swap that added before it deleted would need
		// a free slot that a full pack does not have and would silently consume the clay.
		Client c = client();
		c.playerItems[0] = SoftClay.CLAY + 1;
		c.playerItemsN[0] = 1;
		c.playerItems[1] = 1929 + 1;
		c.playerItemsN[1] = 1;
		for (int i = 2; i < c.playerItems.length; i++) {
			c.playerItems[i] = 4151 + 1;
			c.playerItemsN[i] = 1;
		}
		SoftClay.mix(c, SoftClay.CLAY, 1929);
		assertEquals(1, count(c, SoftClay.SOFT_CLAY));
		assertEquals(1, count(c, 1925));
		assertEquals(0, count(c, SoftClay.CLAY));
		assertEquals(0, count(c, 1929));
	}

	@Test
	void theActionAwardsNoExperience() {
		// The zero is the real number. Baking a crafting xp figure in here would be inventing one,
		// so it is pinned rather than left as a "we forgot to call addSkillXP" smell.
		Client c = withItems(SoftClay.CLAY, 1, 1929, 1);
		int before = c.skills.playerXP[Player.playerCrafting];
		SoftClay.mix(c, SoftClay.CLAY, 1929);
		assertEquals(0D, SoftClay.XP);
		assertEquals(before, c.skills.playerXP[Player.playerCrafting],
				"soft clay must not award crafting experience");
	}
}
