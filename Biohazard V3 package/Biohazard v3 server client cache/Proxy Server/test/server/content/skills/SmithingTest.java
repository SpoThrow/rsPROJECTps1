package server.content.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import core.util.ISAACRandomGen;
import server.Config;
import server.event.CycleEventHandler;
import server.game.players.Client;
import server.game.players.Player;

/**
 * Pins the anvil after it came off the instant batch: one item per action, the bar stack as the only
 * limit, and the whole thing owned by the player.
 *
 * <p>The property worth testing here is not that the loop is slower, it is that the action reads
 * <em>nothing</em> from the static {@code item}/{@code xp}/{@code remove}/{@code removeamount}/
 * {@code maketimes} fields the {@code Check*} chains write into. In the instant path those values
 * were read back in the same call, so a ticked action silently re-reading them would take the wrong
 * product the moment a second player used an anvil — which is what
 * {@link #twoPlayersSmithingAtOnceMakeTheirOwnItems} drives through the real {@code readInput} entry
 * point rather than through {@code doaction} directly.
 */
class SmithingTest {

	/** Bronze bar, the metal the a bronze dagger is made of. */
	private static final int BRONZE_BAR = 2349;
	/** Iron bar. */
	private static final int IRON_BAR = 2351;
	/** Bronze dagger, one bar, 13 xp. */
	private static final int BRONZE_DAGGER = 1205;
	/** Iron axe, one bar, 25 xp. */
	private static final int IRON_AXE = 1349;

	// ---------------------------------------------------------------------------------------
	// The batch sizes and the animation, pinned to the numbers the old loop used.
	// ---------------------------------------------------------------------------------------

	@Test
	void oneBarMakesAWholeBatchOfSomeThings() {
		assertEquals(10, Smithing.batchSize(819), "Bronze_dart_tip: ten dart tips a bar");
		assertEquals(15, Smithing.batchSize(1539), "Steel_nails: fifteen a bar");
		assertEquals(15, Smithing.batchSize(882), "Bronze_arrow: fifteen a bar");
		assertEquals(5, Smithing.batchSize(946), "Knife: five a bar -- capitalised, and it used to miss");
		assertEquals(4, Smithing.batchSize(6), "Cannon_base: four a bar -- capitalised too");
		assertEquals(1, Smithing.batchSize(BRONZE_DAGGER), "everything else, one at a time");
	}

	@Test
	void theHammerAnimationIsStillTheOldLiteral() {
		assertEquals(898, Smithing.SMITH_ANIMATION);
	}

	// ---------------------------------------------------------------------------------------
	// One item per action.
	// ---------------------------------------------------------------------------------------

	@Test
	void theAnvilMakesOneItemPerAction() {
		Client c = withBars(BRONZE_BAR, 3);

		Smithing.doaction(c, BRONZE_DAGGER, BRONZE_BAR, 1, 3, -1, -1, 13);
		assertEquals(3, count(c, BRONZE_BAR), "nothing is consumed before the action runs");

		CycleEventHandler.process();
		assertEquals(3, count(c, BRONZE_BAR), "one tick in, nothing has been hammered");
		assertEquals(0, count(c, BRONZE_DAGGER));

		CycleEventHandler.process();
		assertEquals(2, count(c, BRONZE_BAR), "the second tick is when one item is made");
		assertEquals(1, count(c, BRONZE_DAGGER));
	}

	@Test
	void theWholeOrderIsMadeAndThenItStops() {
		Client c = withBars(BRONZE_BAR, 5);

		Smithing.doaction(c, BRONZE_DAGGER, BRONZE_BAR, 1, 5, -1, -1, 13);
		for (int i = 0; i < 6; i++) {
			passTwoTicks();
		}

		assertEquals(0, count(c, BRONZE_BAR), "five bars, five daggers");
		assertEquals(5, count(c, BRONZE_DAGGER));
		assertFalse(c.playerSkilling[Player.playerSmithing], "and the action has finished");
		assertEquals(5 * 13 * Config.SMITHING_EXPERIENCE,
				c.skills.playerXP[Player.playerSmithing], "13 xp an item, five times");
	}

	@Test
	void anItemCostingSeveralBarsTakesThemAll() {
		// A bronze platebody is five bars an item, which is what toremove2 is for: the action has to
		// take five off the stack for every one product.
		Client c = withBars(BRONZE_BAR, 10);

		Smithing.doaction(c, 1117, BRONZE_BAR, 5, 2, -1, -1, 63);
		passTwoTicks();
		assertEquals(5, count(c, BRONZE_BAR), "five bars for the first platebody");
		assertEquals(1, count(c, 1117));

		passTwoTicks();
		assertEquals(0, count(c, BRONZE_BAR));
		assertEquals(2, count(c, 1117));
	}

	// ---------------------------------------------------------------------------------------
	// What ends the action.
	// ---------------------------------------------------------------------------------------

	@Test
	void withoutTheBarsTheAnvilOnlySaysSo() {
		Client c = withBars(BRONZE_BAR, 2);

		assertFalse(Smithing.doaction(c, 1117, BRONZE_BAR, 5, 5, -1, -1, 63),
				"a platebody is five bars and only two are in the pack");
		passTwoTicks();

		assertEquals(2, count(c, BRONZE_BAR), "nothing is consumed");
		assertEquals(0, count(c, 1117));
		assertFalse(c.playerSkilling[Player.playerSmithing], "and no action is running");
	}

	@Test
	void aRunningBatchStopsWhenTheBarsRunOut() {
		Client c = withBars(BRONZE_BAR, 4);

		Smithing.doaction(c, BRONZE_DAGGER, BRONZE_BAR, 1, 5, -1, -1, 13); // asked for five, has four
		for (int i = 0; i < 6; i++) {
			passTwoTicks();
		}

		assertEquals(4, count(c, BRONZE_DAGGER), "four bars, four daggers -- the fifth is not made");
		assertEquals(0, count(c, BRONZE_BAR));
		assertFalse(c.playerSkilling[Player.playerSmithing], "and the action has stopped");
	}

	@Test
	void walkingAwayEndsTheBatch() {
		Client c = withBars(BRONZE_BAR, 5);

		Smithing.doaction(c, BRONZE_DAGGER, BRONZE_BAR, 1, 5, -1, -1, 13);
		passTwoTicks();
		assertEquals(1, count(c, BRONZE_DAGGER));

		c.getPA().resetVariables(); // what every walk step reaches
		assertFalse(c.playerSkilling[Player.playerSmithing], "the walk ends the action");

		passTwoTicks();
		passTwoTicks();
		assertEquals(1, count(c, BRONZE_DAGGER), "and nothing is hammered after it");
		assertEquals(4, count(c, BRONZE_BAR), "the rest of the stack is still there");
	}

	@Test
	void aSecondOrderReplacesTheRunningOne() {
		Client c = withBars(BRONZE_BAR, 4);

		Smithing.doaction(c, BRONZE_DAGGER, BRONZE_BAR, 1, 4, -1, -1, 13);
		passTwoTicks();
		assertEquals(1, count(c, BRONZE_DAGGER));

		Smithing.doaction(c, 1351, BRONZE_BAR, 1, 4, -1, -1, 13); // bronze axe, one bar
		passTwoTicks();

		assertEquals(2, count(c, BRONZE_BAR), "one bar for one action, not one for each");
		assertEquals(1, count(c, BRONZE_DAGGER), "the first order stopped where it was");
		assertEquals(1, count(c, 1351), "and the new one is what is being made");
	}

	// ---------------------------------------------------------------------------------------
	// The statics are not read by the action.
	// ---------------------------------------------------------------------------------------

	@Test
	void twoPlayersSmithingAtOnceMakeTheirOwnItems() {
		// Driven through readInput, which is the entry point that fills the static fields in: two
		// players on two anvils overwrite each other's product, bar and amount there, and each
		// action must still make its own item out of its own bar.
		Client a = withBars(BRONZE_BAR, 2);
		Client b = withBars(IRON_BAR, 2);
		a.skills.playerLevel[Player.playerSmithing] = 99;
		b.skills.playerLevel[Player.playerSmithing] = 99;

		Smithing.readInput(99, Integer.toString(BRONZE_DAGGER), a, 2);
		Smithing.readInput(99, Integer.toString(IRON_AXE), b, 2);

		passTwoTicks();
		passTwoTicks();

		assertEquals(2, count(a, BRONZE_DAGGER), "a makes bronze daggers");
		assertEquals(0, count(a, IRON_AXE), "and never an iron axe");
		assertEquals(0, count(a, IRON_BAR), "out of iron bars");
		assertEquals(0, count(a, BRONZE_BAR), "a's own bars are the ones it used");
		assertEquals(2, count(b, IRON_AXE), "b makes iron axes");
		assertEquals(0, count(b, BRONZE_DAGGER), "and never a bronze dagger");
		assertEquals(0, count(b, IRON_BAR), "out of b's own bars");
	}

	@Test
	void cancellingNothingIsHarmless() {
		Client c = client();

		Smithing.cancel(c);

		assertFalse(c.playerSkilling[Player.playerSmithing]);
	}

	// ---------------------------------------------------------------------------------------
	// Helpers, copied from HerbloreTest: the event list is static and shared with the whole run.
	// ---------------------------------------------------------------------------------------

	private static final int SLOT = 1;

	private final List<Client> clients = new ArrayList<>();

	private static Client client() {
		Client c = new Client(null, SLOT);
		c.getOutStream().packetEncryption = new ISAACRandomGen(new int[] { 1, 2, 3, 4 });
		c.expModifier = 1;
		return c;
	}

	@AfterEach
	void stopEvents() {
		for (Client c : clients) {
			CycleEventHandler.stopEvents(c);
		}
		clients.clear();
	}

	/** A client holding {@code amount} of one bar in the first slot. */
	private Client withBars(int bar, int amount) {
		Client c = client();
		clients.add(c);
		c.playerItems[0] = bar + 1;
		c.playerItemsN[0] = amount;
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

	/** Runs the tick loop twice, which is when a two-cycle anvil action does its work. */
	private static void passTwoTicks() {
		CycleEventHandler.process();
		CycleEventHandler.process();
	}
}
