package server.content.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import core.util.ISAACRandomGen;
import server.game.players.Client;

/**
 * Pins the tanner's chatbox menu: which products it offers, what the amount is bounded by, and that
 * the price survives the move off its own overlay.
 *
 * <p>Two things here are the reason the class exists. The price is one — interface 14670 had a price
 * frame and the chatbox has none, so the price rides on the row's label, and a tanner who charges
 * the wrong amount is a bug players notice at once. The other is that a row is a <em>product</em>
 * and not a hide: cowhide tans into soft leather or hard leather, so a menu with one row per hide
 * would have made the hard/soft choice unreachable.
 */
class TanningTest {

	private static final int COINS = 995;
	private static final int COWHIDE = 1739;
	private static final int SOFT_LEATHER = 1741;
	private static final int HARD_LEATHER = 1743;
	private static final int SNAKE_HIDE = 6287;
	private static final int SNAKESKIN = 6289;
	private static final int GREEN_HIDE = 1753;
	private static final int GREEN_LEATHER = 1745;

	private static Client withItems(int... idAmountPairs) {
		Client c = new Client(null, 2);
		c.getOutStream().packetEncryption = new ISAACRandomGen(new int[] { 1, 2, 3, 4 });
		c.expModifier = 1;
		int slot = 0;
		for (int i = 0; i < idAmountPairs.length; i += 2) {
			c.playerItems[slot] = idAmountPairs[i] + 1;
			c.playerItemsN[slot] = idAmountPairs[i + 1];
			slot++;
		}
		return c;
	}

	// ---------------------------------------------------------------------------------------
	// The rows
	// ---------------------------------------------------------------------------------------

	@Test
	void theMenuListsTheLeathersThePlayerCanMake() {
		Client c = withItems(GREEN_HIDE, 3, COINS, 1000);

		assertArrayEqualsInt(new int[] { GREEN_LEATHER }, Tanning.offeredProducts(c),
				"one hide held is one row, not seven");
	}

	@Test
	void oneHideCanGiveTwoRowsBecauseItMakesTwoThings() {
		// Cowhide is the case that says the row is a product: soft leather at 1 gp and hard leather
		// at 3 gp, from the same hide. One row per hide would have silently dropped one of them.
		Client c = withItems(COWHIDE, 5, COINS, 1000);

		assertArrayEqualsInt(new int[] { SOFT_LEATHER, HARD_LEATHER }, Tanning.offeredProducts(c));
	}

	@Test
	void twoStacksOfOneHideAreStillOneRow() {
		Client c = withItems(COWHIDE, 4, COWHIDE, 6, COINS, 1000);

		assertArrayEqualsInt(new int[] { SOFT_LEATHER, HARD_LEATHER }, Tanning.offeredProducts(c));
	}

	@Test
	void hidesThePlayerIsNotCarryingAreNotRows() {
		// The old overlay listed all eight rows whether or not the player had the hide. A menu of
		// what the pack can make cannot, and does not need to.
		Client c = withItems(COINS, 1000);

		assertArrayEqualsInt(new int[0], Tanning.offeredProducts(c));
	}

	@Test
	void noTwoRowsShareAProduct() {
		// Snakeskin used to be listed twice from the same hide -- 15 gp and 20 gp for the same
		// leather. On the old overlay both were reachable; as rows keyed on the product they would
		// have been two rows answering one click, so the duplicate is gone.
		Set<Integer> products = new HashSet<Integer>();
		for (CraftingData.tanningData t : CraftingData.tanningData.values()) {
			if (!products.add(t.getLeatherId())) {
				throw new AssertionError("leather " + t.getLeatherId() + " is listed on more than one row");
			}
		}
		assertEquals(7, CraftingData.tanningData.values().length, "seven products, not eight");
	}

	@Test
	void theLabelCarriesThePrice() {
		// The chatbox interface has one text frame per row and it is the label, so this is the only
		// place a price can be shown. The wording matches the old overlay's price frame.
		String[] labels = Tanning.labelsFor(new int[] { SOFT_LEATHER, HARD_LEATHER, GREEN_LEATHER });

		assertEquals("Soft leather - 1 gp", labels[0]);
		assertEquals("Hard leather - 3 gp", labels[1], "the two cowhide rows are told apart by it");
		assertEquals("Green d'hide - 20 gp", labels[2]);
	}

	@Test
	void thePricesAreTheTables() {
		assertEquals(1, CraftingData.tanningData.SOFT_LEATHER.getPrice());
		assertEquals(3, CraftingData.tanningData.HARD_LEATHER.getPrice());
		assertEquals(15, CraftingData.tanningData.SNAKESKIN.getPrice(),
				"the cheaper of the two duplicate snakeskin rows is the one kept");
		assertEquals(20, CraftingData.tanningData.GREEN_DRAGON_LEATHER.getPrice());
	}

	@Test
	void everyRowResolvesBackToItsEntry() {
		for (CraftingData.tanningData t : CraftingData.tanningData.values()) {
			assertNotNull(Tanning.forProduct(t.getLeatherId()),
					"no row for leather " + t.getLeatherId());
			assertEquals(t, Tanning.forProduct(t.getLeatherId()));
		}
		assertNull(Tanning.forProduct(-1), "and an id that is not a leather resolves to nothing");
	}

	// ---------------------------------------------------------------------------------------
	// Opening
	// ---------------------------------------------------------------------------------------

	@Test
	void talkingToTheTannerOpensTheChatbox() {
		Client c = withItems(COWHIDE, 5, COINS, 1000);

		Tanning.sendTanningInterface(c);

		assertEquals(CreationMenu.Kind.TANNING, c.creationMenu);
		assertArrayEqualsInt(new int[] { SOFT_LEATHER, HARD_LEATHER, -1, -1, -1 }, c.creationRows,
				"both cowhide products are on the menu");
	}

	@Test
	void theTannerWithNoHidesOpensNothing() {
		Client c = withItems(COINS, 1000);

		Tanning.sendTanningInterface(c);

		assertNull(c.creationMenu, "there is nothing to offer, so nothing is offered");
	}

	@Test
	void theTannerClosesTheChiselsMenu() {
		Client c = withItems(COWHIDE, 5, COINS, 1000);
		CreationMenu.open(c, CreationMenu.Kind.GEM_CUTTING, new int[] { 1623 });

		Tanning.sendTanningInterface(c);

		assertEquals(CreationMenu.Kind.TANNING, c.creationMenu, "one menu, whichever was last opened");
		assertArrayEqualsInt(new int[] { SOFT_LEATHER, HARD_LEATHER, -1, -1, -1 }, c.creationRows,
				"and the gem's rows are gone with it");
	}

	// ---------------------------------------------------------------------------------------
	// Tanning
	// ---------------------------------------------------------------------------------------

	@Test
	void tanningTakesTheHidesAndTheCoins() {
		Client c = withItems(COWHIDE, 10, COINS, 1000);
		Tanning.sendTanningInterface(c);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][3]); // soft leather, make all

		assertEquals(10, count(c, SOFT_LEATHER), "ten hides, ten soft leather");
		assertEquals(0, count(c, COWHIDE));
		assertEquals(990, count(c, COINS), "at 1 gp each");
	}

	@Test
	void theRowChosenIsTheProductMade() {
		// Both rows are live and both would take the same cowhides, so which row was clicked is the
		// whole of the difference between soft and hard leather.
		Client c = withItems(COWHIDE, 4, COINS, 1000);
		Tanning.sendTanningInterface(c);

		CreationMenu.route(c, CreationMenu.BUTTONS[1][3]); // hard leather, make all

		assertEquals(4, count(c, HARD_LEATHER));
		assertEquals(0, count(c, SOFT_LEATHER), "and none of the other product");
		assertEquals(988, count(c, COINS), "at 3 gp each");
	}

	@Test
	void theAmountIsBoundedByTheHidesHeld() {
		Client c = withItems(COWHIDE, 4, COINS, 1000);
		Tanning.sendTanningInterface(c);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][3]); // make 28, but four is all there is

		assertEquals(4, count(c, SOFT_LEATHER));
		assertEquals(0, count(c, COWHIDE));
	}

	@Test
	void theAmountIsBoundedByTheCoins() {
		// Green d'hide is 20 gp. Twenty-five coins is one hide, and the other nine stay in the pack.
		Client c = withItems(GREEN_HIDE, 10, COINS, 25);
		Tanning.sendTanningInterface(c);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][3]);

		assertEquals(1, count(c, GREEN_LEATHER));
		assertEquals(9, count(c, GREEN_HIDE), "what could not be paid for was not taken");
		assertEquals(5, count(c, COINS));
	}

	@Test
	void theAmountAskedForIsTheCeiling() {
		Client c = withItems(COWHIDE, 28, COINS, 5000);
		Tanning.sendTanningInterface(c);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][1]); // make 5

		assertEquals(5, count(c, SOFT_LEATHER));
		assertEquals(23, count(c, COWHIDE));
		assertEquals(4995, count(c, COINS));
	}

	@Test
	void notEnoughCoinsForEvenOneTakesNothing() {
		Client c = withItems(GREEN_HIDE, 3, COINS, 5);
		Tanning.sendTanningInterface(c);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][0]);

		assertEquals(0, count(c, GREEN_LEATHER));
		assertEquals(3, count(c, GREEN_HIDE));
		assertEquals(5, count(c, COINS), "and the coins that would not have bought one stay");
	}

	@Test
	void theMenuRefreshesSoASecondClickCannotUseAStaleRow() {
		// The rows are what the pack can make, so they have to be rebuilt after the pack moves --
		// the old overlay got this for free by staying on screen with live frames.
		Client c = withItems(COWHIDE, 3, COINS, 1000);
		Tanning.sendTanningInterface(c);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][0]); // make 1

		assertEquals(CreationMenu.Kind.TANNING, c.creationMenu, "still open");
		assertEquals(2, count(c, COWHIDE), "and the row is still answering about what is left");
		assertArrayEqualsInt(new int[] { SOFT_LEATHER, HARD_LEATHER, -1, -1, -1 }, c.creationRows);
	}

	@Test
	void theMenuGoesWhenThereIsNothingLeftToTan() {
		Client c = withItems(COWHIDE, 2, COINS, 1000);
		Tanning.sendTanningInterface(c);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][3]); // make all

		assertEquals(2, count(c, SOFT_LEATHER));
		assertNull(c.creationMenu, "nothing left to offer, so the menu comes down");
	}

	@Test
	void aHideThatIsNotCarriedMakesNothing() {
		// Reachable by clicking fast enough to race the refresh, so the row is re-checked rather
		// than trusted.
		Client c = withItems(COINS, 1000);
		Tanning.sendTanningInterface(c);

		Tanning.select(c, SNAKESKIN, 1);

		assertEquals(0, count(c, SNAKESKIN));
	}

	@Test
	void snakeHidesStillReachTheirOneRow() {
		Client c = withItems(SNAKE_HIDE, 2, COINS, 1000);
		Tanning.sendTanningInterface(c);

		CreationMenu.route(c, CreationMenu.BUTTONS[0][0]); // make 1

		assertEquals(1, count(c, SNAKESKIN));
		assertEquals(1, count(c, SNAKE_HIDE));
		assertEquals(985, count(c, COINS), "at 15 gp, the price the duplicate row shared");
	}

	// ---------------------------------------------------------------------------------------

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
