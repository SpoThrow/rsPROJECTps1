package server.content.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import core.util.ISAACRandomGen;
import server.Config;
import server.content.skills.Runecrafting.RunecraftingData;
import server.event.CycleEventHandler;
import server.game.players.Client;
import server.game.players.Player;

/**
 * Pins the runecrafting table and the three facts the tick rewrite depends on: binding is paced one
 * essence at a time, the multiplier ladder pays its top rung, and the action belongs to the player
 * rather than to the server.
 *
 * <p>The table is the part that fails quietly — a wrong altar id is an altar that does nothing, and
 * a wrong rune id is an altar that hands out the wrong rune — so it is pinned row by row. The
 * ladder is pinned the same way because the old loop read it from its second element and so could
 * never reach {@code 10}: an air essence at level 99 made nine air runes, and
 * {@link #anEssenceAtTheTopRungMakesTenAirRunes} is that bug, named.
 *
 * <p>The pacing half is here because the old implementation had none: {@code craftRunes} was a
 * single {@code while} over the whole pack, so a batch was one game cycle and nothing could
 * interrupt it. {@link #theWholeInventoryIsNotOneTick} and {@link #walkingAwayEndsTheBatch} are what
 * changed.
 */
class RunecraftingTest {

	/**
	 * The whole table, exactly as the enum is written: {altar object id, rune id, base xp in tenths
	 * (four rows are {@code x.5}, which tenths avoid writing as doubles), level requirement, then the
	 * multiplier ladder}.
	 *
	 * <p>Rows are looked up by altar id rather than by ordinal, so reordering the enum is not a test
	 * failure — but adding or removing an altar is, which is the point.
	 */
	private static final int[][] ALTARS = {
			{ 2478, 556, 50, 1, 1, 11, 22, 33, 44, 55, 66, 77, 88, 99 }, // air
			{ 2479, 558, 55, 1, 1, 14, 28, 42, 56, 70, 84, 98 }, // mind
			{ 2480, 555, 60, 5, 5, 19, 38, 57, 76, 95 }, // water
			{ 2481, 557, 65, 9, 9, 26, 52, 78 }, // earth
			{ 2482, 554, 70, 14, 14, 35, 70 }, // fire
			{ 2483, 559, 75, 20, 20, 46, 92 }, // body
			{ 2484, 564, 80, 27, 27, 59 }, // cosmic
			{ 2487, 562, 85, 35, 35, 74 }, // chaos
			{ 2486, 561, 90, 44, 44, 91 }, // nature
			{ 2485, 563, 95, 54, 54 }, // law
			{ 2488, 565, 100, 65, 65 }, // death
			{ 30624, 560, 105, 77, 77 }, // blood
			{ 30625, 566, 110, 90, 90 }, // soul
	};

	/** The rune essence, spelled out again so the test does not follow a rename blindly. */
	private static final int ESSENCE = 1436;

	// ---------------------------------------------------------------------------------------
	// The table.
	// ---------------------------------------------------------------------------------------

	@Test
	void everyAltarRowIsPinned() {
		assertEquals(ALTARS.length, RunecraftingData.values().length, "the altar count");

		Set<Integer> seenAltars = new HashSet<>();
		for (int[] row : ALTARS) {
			RunecraftingData altar = Runecrafting.forAltar(row[0]);
			assertNotNull(altar, "no altar for object " + row[0]);
			assertTrue(seenAltars.add(row[0]), "altar " + row[0] + " is in the table twice");
			assertEquals(row[1], altar.getRuneId(), "rune for altar " + row[0]);
			assertEquals(row[2] / 10.0, altar.getXp(), 0.0, "xp for altar " + row[0]);
			assertEquals(row[3], altar.getLevel(), "level for altar " + row[0]);
			assertTrue(Runecrafting.isAltar(row[0]), "altar " + row[0] + " is not recognised");
		}
	}

	@Test
	void everyLadderPaysExactlyOneMoreRunePerRung() {
		for (int[] row : ALTARS) {
			RunecraftingData altar = Runecrafting.forAltar(row[0]);
			// The ladder starts at index 4: index 3 is the level requirement, which the enum's
			// ladder also carries as its own first rung.
			int[] ladder = java.util.Arrays.copyOfRange(row, 4, row.length);
			assertEquals(row[3], ladder[0], "the requirement is the ladder's first rung");

			for (int rung = 0; rung < ladder.length; rung++) {
				assertEquals(rung + 1, altar.getMultiplierForLevel(ladder[rung]),
						"altar " + row[0] + ": rung " + ladder[rung] + " should pay " + (rung + 1)
								+ " runes");
				if (rung > 0) {
					assertTrue(ladder[rung] - 1 >= ladder[rung - 1],
							"altar " + row[0] + ": rungs " + ladder[rung - 1] + " and " + ladder[rung]
									+ " are not a gap");
					assertEquals(rung, altar.getMultiplierForLevel(ladder[rung] - 1),
							"altar " + row[0] + ": one level short of rung " + ladder[rung]
									+ " must still be " + rung + " runes");
				}
			}
		}
	}

	@Test
	void anEssenceAtTheTopRungMakesTenAirRunes() {
		// The old loop walked the ladder from index 1, so the best it could return was length - 1:
		// nine air runes at level 99, and one short of the top on every rune above it.
		RunecraftingData air = RunecraftingData.AIR;
		assertEquals(10, air.getMultiplierForLevel(99), "an air essence at 99 is ten air runes");
		assertEquals(9, air.getMultiplierForLevel(88), "and nine at 88");
		assertEquals(1, air.getMultiplierForLevel(10), "and one below the first rung after the gate");
	}

	@Test
	void isAltarKnowsEveryAltarIncludingTheSoulAltarTheOldCopyMissed() {
		// ClickObject used to carry its own copy of the altar list, and the copy stopped at blood:
		// 30625 was in the table and unreachable from the plain altar. It is a table lookup now.
		assertTrue(Runecrafting.isAltar(30625), "the soul altar must be recognised");
		assertFalse(Runecrafting.isAltar(2477), "one below the first altar is not an altar");
		assertFalse(Runecrafting.isAltar(30626), "one above the last altar is not an altar");
		assertFalse(Runecrafting.isAltar(0), "and neither is nothing");
	}

	// ---------------------------------------------------------------------------------------
	// Binding, one essence at a time.
	// ---------------------------------------------------------------------------------------

	@Test
	void theWholeInventoryIsNotOneTick() {
		Client c = withEssence(3);
		c.skills.playerLevel[Player.playerRunecrafting] = 1;

		Runecrafting.craftRunes(c, RunecraftingData.AIR.getAltarId());
		assertEquals(3, count(c, ESSENCE), "nothing is consumed before the action runs");

		CycleEventHandler.process();
		assertEquals(3, count(c, ESSENCE), "one tick in, nothing has been bound yet");
		assertEquals(0, count(c, 556), "and no runes");

		CycleEventHandler.process();
		assertEquals(2, count(c, ESSENCE), "the second tick is when one essence is bound");
		assertEquals(1, count(c, 556), "and makes one air rune at level 1");
	}

	@Test
	void aFullInventoryOfEssenceIsBoundOnePerAction() {
		Client c = withEssence(28);
		c.skills.playerLevel[Player.playerRunecrafting] = 1;

		Runecrafting.craftRunes(c, RunecraftingData.AIR.getAltarId());
		// Twenty-eight bindings, plus the cycle that finds the stack empty and ends the action.
		for (int i = 0; i < 29; i++) {
			passTwoTicks();
		}

		assertEquals(0, count(c, ESSENCE), "all twenty-eight are bound");
		assertEquals(28, count(c, 556), "one rune each at level 1");
		assertFalse(c.playerSkilling[Player.playerRunecrafting], "and the action has finished");
	}

	@Test
	void everyAltarMakesItsOwnRuneAtItsOwnTopRung() {
		for (int[] row : ALTARS) {
			int rungs = row.length - 4; // index 3 is the level, 4.. is the ladder
			int level = row[row.length - 1];
			Client c = withEssence(1);
			c.skills.playerLevel[Player.playerRunecrafting] = level;

			Runecrafting.craftRunes(c, row[0]);
			passTwoTicks();

			assertEquals(0, count(c, ESSENCE), "altar " + row[0] + " must consume the essence");
			assertEquals(rungs, count(c, row[1]),
					"altar " + row[0] + " at level " + level + " should pay " + rungs + " runes");
		}
	}

	@Test
	void theRuneCountIsMultipliedButTheExperienceIsNot() {
		// Deliberate, and pinned so it reads as a decision: OSRS scales the experience with the
		// multiplier too. It is left flat because Config.RUNECRAFTING_EXPERIENCE is already 15x,
		// which is a balance question rather than a realism one -- see the class comment.
		Client c = withEssence(2);
		c.skills.playerLevel[Player.playerRunecrafting] = 99;

		Runecrafting.craftRunes(c, RunecraftingData.AIR.getAltarId());
		passTwoTicks();
		passTwoTicks();

		assertEquals(20, count(c, 556), "two essences at x10");
		assertEquals(2 * 5 * Config.RUNECRAFTING_EXPERIENCE,
				c.skills.playerXP[Player.playerRunecrafting],
				"the experience is per essence, not per rune");
	}

	@Test
	void theMultiplierIsReReadEveryCycle() {
		// A level-up part-way through a batch pays out at the new rate from the next essence, which
		// requires the multiplier to be read from the player on each cycle rather than captured once.
		Client c = withEssence(2);
		c.skills.playerLevel[Player.playerRunecrafting] = 10;

		Runecrafting.craftRunes(c, RunecraftingData.AIR.getAltarId());
		passTwoTicks();
		assertEquals(1, count(c, 556), "the first essence is bound at x1");

		c.skills.playerLevel[Player.playerRunecrafting] = 99;
		passTwoTicks();
		assertEquals(11, count(c, 556), "the second is bound at x10");
	}

	// ---------------------------------------------------------------------------------------
	// The gates, and what ends a batch.
	// ---------------------------------------------------------------------------------------

	@Test
	void belowTheLevelTheAltarOnlySaysSo() {
		Client c = withEssence(1);
		c.skills.playerLevel[Player.playerRunecrafting] = 43;

		Runecrafting.craftRunes(c, RunecraftingData.LAW.getAltarId());
		passTwoTicks();

		assertEquals(1, count(c, ESSENCE), "nothing is bound without the level");
		assertEquals(0, count(c, 563), "not even the rune");
		assertFalse(c.playerSkilling[Player.playerRunecrafting], "and no action is running");
	}

	@Test
	void anAltarWithNoEssenceSaysSoAndStartsNothing() {
		Client c = withEssence(0);
		c.skills.playerLevel[Player.playerRunecrafting] = 99;

		// The old path was silent here, which reads as a broken altar. Asserting the branch rather
		// than the text: this test's client has no real out stream to read the message back from.
		Runecrafting.craftRunes(c, RunecraftingData.AIR.getAltarId());
		passTwoTicks();

		assertEquals(0, count(c, 556), "no essence, no runes");
		assertFalse(c.playerSkilling[Player.playerRunecrafting], "and no action is running");
	}

	@Test
	void anObjectThatIsNotAnAltarDoesNothing() {
		Client c = withEssence(1);
		c.skills.playerLevel[Player.playerRunecrafting] = 99;

		Runecrafting.craftRunes(c, 1234);
		passTwoTicks();

		assertEquals(1, count(c, ESSENCE), "an unknown object is not an altar");
		assertFalse(c.playerSkilling[Player.playerRunecrafting], "and starts no action");
	}

	@Test
	void theBatchStopsWhenTheEssenceRunsOut() {
		Client c = withEssence(2);
		c.skills.playerLevel[Player.playerRunecrafting] = 1;

		Runecrafting.craftRunes(c, RunecraftingData.AIR.getAltarId());
		passTwoTicks();
		assertTrue(c.playerSkilling[Player.playerRunecrafting], "still binding with one left");

		passTwoTicks();
		assertEquals(0, count(c, ESSENCE), "the last one is bound");
		passTwoTicks(); // the cycle that finds nothing left to bind
		assertFalse(c.playerSkilling[Player.playerRunecrafting], "the stack is gone, so is the action");

		// ...and a further tick cannot bind a rune out of nothing
		passTwoTicks();
		assertEquals(2, count(c, 556));
	}

	@Test
	void walkingAwayEndsTheBatch() {
		Client c = withEssence(5);
		c.skills.playerLevel[Player.playerRunecrafting] = 1;

		Runecrafting.craftRunes(c, RunecraftingData.AIR.getAltarId());
		passTwoTicks();
		assertEquals(1, count(c, 556));

		c.getPA().resetVariables(); // what every walk step reaches
		assertFalse(c.playerSkilling[Player.playerRunecrafting], "the walk ends the action");

		passTwoTicks();
		passTwoTicks();
		assertEquals(1, count(c, 556), "and nothing is bound after it");
		assertEquals(4, count(c, ESSENCE), "the rest of the stack is still there");
	}

	@Test
	void aSecondClickDoesNotStartASecondLoop() {
		Client c = withEssence(4);
		c.skills.playerLevel[Player.playerRunecrafting] = 1;

		Runecrafting.craftRunes(c, RunecraftingData.AIR.getAltarId());
		passTwoTicks();
		Runecrafting.craftRunes(c, RunecraftingData.AIR.getAltarId()); // the impatient second click
		passTwoTicks();

		assertEquals(2, count(c, 556), "two loops over one stack would have bound four");
		assertEquals(2, count(c, ESSENCE));
	}

	@Test
	void cancellingNothingIsHarmless() {
		Client c = client();

		Runecrafting.cancel(c);

		assertFalse(c.playerSkilling[Player.playerRunecrafting]);
	}

	@Test
	void twoPlayersBindingAtOnceDoNotShareAnything() {
		Client a = withEssence(1);
		a.skills.playerLevel[Player.playerRunecrafting] = 99;
		Client b = withEssence(1);
		b.skills.playerLevel[Player.playerRunecrafting] = 54;

		Runecrafting.craftRunes(a, RunecraftingData.AIR.getAltarId());
		Runecrafting.craftRunes(b, RunecraftingData.LAW.getAltarId());
		passTwoTicks();

		assertEquals(10, count(a, 556), "the air altar pays the air crafter");
		assertEquals(0, count(a, 563), "and never the law rune");
		assertEquals(1, count(b, 563), "the law altar pays the law crafter");
		assertEquals(0, count(b, 556), "and a law essence is always one rune");
		assertTrue(a.playerSkilling[Player.playerRunecrafting]);
		assertTrue(b.playerSkilling[Player.playerRunecrafting], "both actions are their own");
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

	/** A client holding {@code amount} rune essence in the first slot. */
	private Client withEssence(int amount) {
		Client c = client();
		clients.add(c);
		if (amount > 0) {
			c.playerItems[0] = ESSENCE + 1;
			c.playerItemsN[0] = amount;
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

	/** Runs the tick loop twice, which is when a two-cycle binding action does its work. */
	private static void passTwoTicks() {
		CycleEventHandler.process();
		CycleEventHandler.process();
	}
}
