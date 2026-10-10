package server.content.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import core.util.ISAACRandomGen;
import server.Config;
import server.content.skills.Herblore.Cleaning;
import server.content.skills.Herblore.Finished;
import server.content.skills.Herblore.Grinding;
import server.content.skills.Herblore.Unfinished;
import server.event.CycleEventHandler;
import server.game.players.Client;
import server.game.players.Player;
import server.game.players.actions.items.ItemUseRegistry;

/**
 * Pins herblore's four tables and the two facts the rewrite depends on: a recipe travels with its
 * own action, and one click is one potion.
 *
 * <p>The tables are the part that fails quietly — a wrong clean-herb id is a herb that cannot be
 * cleaned rather than an error, and a potion whose level disagrees with the guide is a promise
 * broken in a menu nobody diffs. The behaviour half is here too, because it is what the old
 * implementation could not do: the recipes used to live in static fields shared by every player on
 * the server, and {@link #twoPlayersMixingAtOnceDoNotCrossWire} is that bug.
 */
class HerbloreTest {

	// ---------------------------------------------------------------------------------------
	// Tables, pinned row by row.
	// ---------------------------------------------------------------------------------------

	/** {grimy, clean, level, xp}, exactly as the enum is written. */
	private static final int[][] CLEANING = {
			{ 199, 249, 3, 3 },    // guam
			{ 201, 251, 5, 4 },    // marrentill
			{ 203, 253, 11, 5 },   // tarromin
			{ 205, 255, 20, 6 },   // harralander
			{ 207, 257, 25, 8 },   // ranarr
			{ 3049, 2998, 30, 8 }, // toadflax
			{ 14836, 14854, 30, 8 }, // wergali
			{ 12174, 12172, 35, 8 }, // spirit weed
			{ 209, 259, 40, 9 },   // irit
			{ 211, 261, 48, 10 },  // avantoe
			{ 213, 263, 54, 11 },  // kwuarm
			{ 3051, 3000, 59, 12 }, // snapdragon
			{ 215, 265, 65, 13 },  // cadantine
			{ 2485, 2481, 67, 13 }, // lantadyme
			{ 217, 267, 70, 14 },  // dwarf weed
			{ 219, 269, 75, 15 },  // torstol
	};

	/** {input, powder}, exactly as the enum is written. */
	private static final int[][] GRINDING = {
			{ 237, 235 },   // unicorn horn
			{ 1973, 1975 }, // chocolate bar
			{ 5075, 6693 }, // bird's nest
			{ 10109, 10111 }, // kebbit teeth
			{ 243, 241 },   // blue dragon scale
			{ 9735, 9736 }, // desert goat horn
			{ 14703, 14704 }, // diamond root
			{ 6466, 6467 }, // rune shards
	};

	/** {base, herb, potion, level}, exactly as the enum is written. */
	private static final int[][] UNFINISHED = {
			{ 227, 249, 91, 3 }, { 227, 251, 93, 5 }, { 227, 253, 95, 11 },
			{ 227, 255, 97, 20 }, { 227, 257, 99, 25 }, { 227, 2998, 3002, 30 },
			{ 227, 259, 101, 40 }, { 227, 261, 103, 48 }, { 227, 263, 105, 54 },
			{ 227, 3000, 3004, 59 }, { 227, 265, 107, 65 }, { 227, 2481, 2483, 67 },
			{ 227, 267, 109, 70 }, { 227, 269, 111, 75 },
			{ 5935, 6016, 5936, 73 }, { 5935, 2398, 5939, 82 },
	};

	/**
	 * {unfinished, secondary, potion, level, xp}, exactly as the enum is written.
	 *
	 * <p>The five that were added are called out at the bottom: the guide printed all five levels,
	 * and nothing in this class could make any of them before this change.
	 */
	private static final int[][] FINISHED = {
			{ 91, 221, 121, 3, 25 },    // attack
			{ 93, 235, 175, 5, 38 },    // antipoison
			{ 95, 225, 115, 12, 50 },   // strength
			{ 97, 223, 127, 22, 63 },   // restore
			{ 97, 1975, 3010, 26, 68 }, // energy
			{ 99, 239, 133, 30, 75 },   // defence
			{ 3002, 2152, 3034, 34, 80 }, // agility
			{ 97, 9736, 9741, 36, 84 }, // combat
			{ 99, 231, 139, 38, 88 },   // prayer
			{ 101, 221, 145, 45, 100 }, // super attack
			{ 101, 235, 181, 48, 106 }, // super antipoison
			{ 103, 231, 151, 50, 112 }, // fishing
			{ 97, 2970, 3018, 52, 118 }, // super energy
			{ 103, 10111, 10000, 53, 120 }, // hunter
			{ 105, 225, 157, 55, 125 }, // super strength
			{ 105, 241, 187, 60, 137 }, // weapon poison
			{ 3004, 223, 3026, 63, 142 }, // super restore
			{ 107, 239, 163, 66, 150 }, // super defence
			{ 3002, 6049, 5945, 68, 155 }, // antidote+
			{ 2483, 241, 2454, 69, 158 }, // antifire
			{ 109, 245, 169, 72, 163 }, // ranging
			{ 5936, 223, 5937, 73, 165 }, // weapon poison+
			{ 2483, 3138, 3042, 76, 173 }, // magic
			{ 111, 247, 189, 78, 175 }, // zamorak brew
			{ 101, 6051, 5954, 79, 178 }, // antidote++
			{ 3002, 6693, 6687, 81, 180 }, // saradomin brew
			{ 5939, 6018, 5940, 82, 190 }, // weapon poison++
	};

	@Test
	void everyCleaningRowIsPinnedAndFoundByItsGrimyHerb() {
		for (int[] row : CLEANING) {
			Cleaning herb = Herblore.forGrimyHerb(row[0]);
			assertNotNull(herb, "no cleaning recipe for grimy herb " + row[0]);
			assertEquals(row[1], herb.getClean(), "clean id for " + row[0]);
			assertEquals(row[2], herb.getLevelReq(), "level for " + row[0]);
			assertEquals(row[3], herb.getXp(), "xp for " + row[0]);
			assertEquals(herb, Herblore.forCleanHerb(row[1]), "the clean id must find its own row");
		}
		assertNull(Herblore.forGrimyHerb(249), "a clean herb is not something you clean");
		assertFalse(Herblore.isHerb(249), "and it must not answer isHerb");
		assertEquals(CLEANING.length, Cleaning.values().length, "a row was added or lost");
	}

	@Test
	void everyGrindingRowIsPinnedAndThePestleIsNotAnInput() {
		for (int[] row : GRINDING) {
			Grinding grindable = Herblore.forGrindable(row[0]);
			assertNotNull(grindable, "no grinding recipe for " + row[0]);
			assertEquals(row[1], grindable.getProduct(), "product for " + row[0]);
		}
		assertNull(Herblore.forGrindable(Herblore.PESTLE_AND_MORTAR),
				"the pestle is the tool, not a thing you grind");
		assertEquals(GRINDING.length, Grinding.values().length, "a row was added or lost");
	}

	@Test
	void everyUnfinishedRowIsPinnedAndFoundInEitherOrder() {
		for (int[] row : UNFINISHED) {
			Unfinished forward = Herblore.forUnfinished(row[0], row[1]);
			assertNotNull(forward, "no unfinished recipe for " + row[1]);
			assertEquals(row[2], forward.getPotion(), "unfinished id for " + row[1]);
			assertEquals(row[3], forward.getLevelReq(), "level for " + row[1]);
			assertEquals(forward, Herblore.forUnfinished(row[1], row[0]),
					"the pair must be order-independent");
		}
		assertNull(Herblore.forUnfinished(227, 227), "a base on itself is not a recipe");
		assertEquals(UNFINISHED.length, Unfinished.values().length, "a row was added or lost");
	}

	@Test
	void everyFinishedRowIsPinnedAndFoundInEitherOrder() {
		for (int[] row : FINISHED) {
			Finished forward = Herblore.forFinished(row[0], row[1]);
			assertNotNull(forward, "no finishing recipe for " + row[1]);
			assertEquals(row[2], forward.getPotion(), "potion id for " + row[1]);
			assertEquals(row[3], forward.getLevelReq(), "level for " + row[1]);
			assertEquals(row[4], forward.getXp(), "xp for " + row[1]);
			assertEquals(forward, Herblore.forFinished(row[1], row[0]),
					"the pair must be order-independent");
		}
		assertEquals(FINISHED.length, Finished.values().length, "a row was added or lost");
	}

	@Test
	void theFivePotionsTheGuideAdvertisedNowResolve() {
		// The reachability claim of this change. The guide's Potions tab has printed these five
		// since before the work and the old table produced none of them: energy, agility, super
		// energy, antidote+ and antidote++. A row is not enough — the *pair* has to resolve.
		int[][] promised = {
				{ 97, 1975, 3010 },  // energy
				{ 3002, 2152, 3034 },// agility
				{ 97, 2970, 3018 },  // super energy
				{ 3002, 6049, 5945 },// antidote+
				{ 101, 6051, 5954 }, // antidote++
		};
		for (int[] row : promised) {
			Finished finished = Herblore.forFinished(row[0], row[1]);
			assertNotNull(finished, "the guide prints a potion from " + row[0] + " + " + row[1]);
			assertEquals(row[2], finished.getPotion(), "and it is the potion the guide names");
		}
	}

	@Test
	void theMixingTablesShareNoPair() {
		// Herblore.mix tries the unfinished table first, so a pair in both would silently become an
		// unfinished potion whatever the player meant. Their key spaces are separate by construction
		// (bases are 227 and 5935, the other side is an id in the 90s to 3000s), and this is the
		// assertion that keeps them that way.
		Set<Long> unfinished = new HashSet<>();
		for (Unfinished row : Unfinished.values()) {
			unfinished.add(key(row.getBase(), row.getHerb()));
		}
		for (Finished row : Finished.values()) {
			assertFalse(unfinished.contains(key(row.getUnfinished(), row.getSecondary())),
					"pair " + row.getUnfinished() + "+" + row.getSecondary()
							+ " is in both mixing tables");
		}
		assertEquals(unfinished.size(), Unfinished.values().length,
				"two unfinished rows share a pair, so one of them is unreachable");
	}

	// ---------------------------------------------------------------------------------------
	// Cleaning: one herb per click, and the one that was clicked.
	// ---------------------------------------------------------------------------------------

	@Test
	void cleaningGuamNeedsLevelThree() {
		Client c = withItems(199, 1);
		c.skills.playerLevel[Player.playerHerblore] = 2;
		Herblore.cleanHerb(c, 199, 0);
		assertEquals(0, count(c, 249), "guam cleaning needs 3, not the 1 this table used to say");

		c.skills.playerLevel[Player.playerHerblore] = 3;
		Herblore.cleanHerb(c, 199, 0);
		assertEquals(1, count(c, 249), "and at 3 it cleans");
		assertEquals(0, count(c, 199), "consuming the grimy herb");
	}

	@Test
	void cleaningCleansOnlyTheHerbThatWasClicked() {
		// The old loop walked the whole table and acted on every row it found in the pack, so
		// clicking one grimy herb cleaned all of them — and deleted the second from the first
		// herb's slot. Two different herbs in the pack is the smallest inventory that shows it.
		Client c = withItems(199, 1, 219, 1);
		c.skills.playerLevel[Player.playerHerblore] = 99;

		Herblore.cleanHerb(c, 199, 0);

		assertEquals(1, count(c, 249), "the guam was cleaned");
		assertEquals(1, count(c, 219), "the torstol was not touched");
		assertEquals(0, count(c, 269), "and no torstol appeared");
	}

	@Test
	void cleaningAwardsTheRowsExperience() {
		Client c = withItems(199, 1);
		c.skills.playerLevel[Player.playerHerblore] = 99;
		Herblore.cleanHerb(c, 199, 0);
		assertEquals(3 * Config.HERBLORE_EXPERIENCE, c.skills.playerXP[Player.playerHerblore],
				"guam cleaning is 3 xp");
	}

	@Test
	void clickingSomethingThatIsNotAGrimyHerbCleansNothing() {
		Client c = withItems(249, 1);
		c.skills.playerLevel[Player.playerHerblore] = 99;
		Herblore.cleanHerb(c, 249, 0);
		assertEquals(1, count(c, 249), "a clean herb stays as it is");
		assertEquals(0, count(c, 269), "and nothing is conjured from it");
	}

	// ---------------------------------------------------------------------------------------
	// Grinding: the pestle is a tool, so it survives.
	// ---------------------------------------------------------------------------------------

	@Test
	void grindingNeedsThePestleAndKeepsIt() {
		Client c = withItems(237, 1, 233, 1);
		Herblore.grind(c, 237, 233);
		passTwoTicks();

		assertEquals(1, count(c, 235), "the horn becomes dust");
		assertEquals(0, count(c, 237), "and is consumed");
		assertEquals(1, count(c, 233), "the pestle is not");
	}

	@Test
	void grindingNothingButTheItemDoesNothing() {
		// No pestle in the pair, so the registry would not have dispatched this at all; the guard is
		// here because grind() is public and a test calls it directly.
		Client c = withItems(237, 1);
		Herblore.grind(c, 237, 237);
		passTwoTicks();
		assertEquals(0, count(c, 235), "a horn with no pestle stays a horn");
	}

	@Test
	void grindingIsTickedAndAwardsNoExperience() {
		Client c = withItems(237, 1, 233, 1);
		Herblore.grind(c, 237, 233);
		CycleEventHandler.process();
		assertEquals(1, count(c, 237), "one tick in, nothing has been ground");

		CycleEventHandler.process();
		assertEquals(1, count(c, 235), "the second tick is when it runs");
		assertEquals(0, c.skills.playerXP[Player.playerHerblore],
				"grinding is preparation and awards nothing, as in OSRS");
	}

	// ---------------------------------------------------------------------------------------
	// Mixing: one potion per click, ticked, and per player.
	// ---------------------------------------------------------------------------------------

	@Test
	void aVialOfWaterAndAGuamMakeAGuamPotionUnfinished() {
		Client c = withItems(227, 1, 249, 1);
		c.skills.playerLevel[Player.playerHerblore] = 3;

		assertTrue(ItemUseRegistry.dispatch(c, 227, 249), "the pair belongs to the registry");
		passTwoTicks();

		assertEquals(1, count(c, 91), "a vial of water and a guam make guam potion (unf)");
		assertEquals(0, count(c, 227), "the vial is consumed");
		assertEquals(0, count(c, 249), "and so is the herb");
		assertEquals(0, c.skills.playerXP[Player.playerHerblore],
				"the unfinished step awards nothing; the experience comes with the potion");
	}

	@Test
	void anUnfinishedPotionAndItsSecondaryMakeThePotion() {
		Client c = withItems(91, 1, 221, 1);
		c.skills.playerLevel[Player.playerHerblore] = 3;

		Herblore.mix(c, 91, 221);
		passTwoTicks();

		assertEquals(1, count(c, 121), "guam potion (unf) and eye of newt make an attack potion");
		assertEquals(0, count(c, 91), "the unfinished potion is consumed");
		assertEquals(0, count(c, 221), "and so is the secondary");
		assertEquals(25 * Config.HERBLORE_EXPERIENCE, c.skills.playerXP[Player.playerHerblore],
				"an attack potion is 25 xp");
	}

	@Test
	void aLevelBelowTheRequirementMakesNoPotion() {
		Client c = withItems(91, 1, 221, 1);
		c.skills.playerLevel[Player.playerHerblore] = 2; // an attack potion needs 3

		Herblore.mix(c, 91, 221);
		passTwoTicks();

		assertEquals(0, count(c, 121), "no level, no potion");
		assertEquals(1, count(c, 91), "and nothing consumed");
	}

	@Test
	void mixingIsTickedAndRechecksTheMaterials() {
		Client c = withItems(91, 2, 221, 2);
		c.skills.playerLevel[Player.playerHerblore] = 99;

		Herblore.mix(c, 91, 221);
		CycleEventHandler.process();
		assertEquals(2, count(c, 91), "one tick in, the materials are still there");

		CycleEventHandler.process();
		assertEquals(1, count(c, 121), "the second tick delivers exactly one potion");
		assertEquals(1, count(c, 91), "from exactly one of each material — not a batch");
	}

	@Test
	void walkingAwayCancelsMixing() {
		Client c = withItems(91, 1, 221, 1);
		c.skills.playerLevel[Player.playerHerblore] = 99;

		Herblore.mix(c, 91, 221);
		c.getPA().resetVariables(); // what every walk step reaches
		passTwoTicks();

		assertEquals(1, count(c, 91), "a cancelled action must not consume anything");
		assertEquals(0, count(c, 121), "and must not deliver the potion");
	}

	@Test
	void twoPlayersMixingAtOnceDoNotCrossWire() {
		// The bug the rewrite exists for. The old implementation put the recipe in four static
		// fields, so two players mixing different things at the same time overwrote each other's
		// materials and product. The recipe now travels with its own event.
		Client attack = withItems(91, 1, 221, 1);
		Client strength = withItems(95, 1, 225, 1);
		attack.skills.playerLevel[Player.playerHerblore] = 99;
		strength.skills.playerLevel[Player.playerHerblore] = 99;

		Herblore.mix(attack, 91, 221);
		Herblore.mix(strength, 95, 225);
		passTwoTicks();

		assertEquals(1, count(attack, 121), "the attack potion belongs to the attack mixer");
		assertEquals(0, count(attack, 115), "and not the strength potion");
		assertEquals(1, count(strength, 115), "the strength potion belongs to the other");
		assertEquals(0, count(strength, 121), "and not the attack potion");
	}

	// ---------------------------------------------------------------------------------------
	// The registry: herblore is a pair family, so every recipe is one.
	// ---------------------------------------------------------------------------------------

	@Test
	void everyHerbloreRecipeIsRegisteredInBothOrders() {
		for (Grinding row : Grinding.values()) {
			assertRegistered(row.getInput(), Herblore.PESTLE_AND_MORTAR);
		}
		for (Unfinished row : Unfinished.values()) {
			assertRegistered(row.getBase(), row.getHerb());
		}
		for (Finished row : Finished.values()) {
			assertRegistered(row.getUnfinished(), row.getSecondary());
		}
	}

	@Test
	void cleaningIsNotARegistryPairBecauseItIsAClick() {
		// A grimy herb is clicked rather than combined, so it belongs to ClickItem. Registering the
		// pair would make using one grimy herb on another a cleaning action, which is not a thing.
		assertFalse(ItemUseRegistry.isRegistered(199, 249),
				"grimy guam on clean guam is not a recipe, it is a mistake");
		assertFalse(ItemUseRegistry.isRegistered(199, 199), "and neither is a herb on itself");
	}

	@Test
	void aHerblorePairCannotBeRegisteredTwice() {
		// The registry is what stops two overlapping recipes both firing, which the legacy sequence
		// of if-blocks in UseItem could not. Re-registering a recipe has to throw rather than
		// silently shadow, so this is asserted rather than assumed.
		assertThrows(IllegalStateException.class,
				() -> ItemUseRegistry.register(227, 249, (c, a, b) -> { }));
		assertThrows(IllegalStateException.class,
				() -> ItemUseRegistry.register(249, 227, (c, a, b) -> { }));
	}

	// ---------------------------------------------------------------------------------------
	// Helpers, copied from FletchingTest: the event list is static and shared with the whole run.
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

	/** Puts {@code amount} of {@code id} in a slot. Items are stored as id + 1. */
	private Client withItems(int... idAmountPairs) {
		Client c = client();
		clients.add(c);
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

	/** Runs the tick loop twice, which is when a two-cycle herblore action does its work. */
	private static void passTwoTicks() {
		CycleEventHandler.process();
		CycleEventHandler.process();
	}

	private static void assertRegistered(int itemA, int itemB) {
		assertTrue(ItemUseRegistry.isRegistered(itemA, itemB),
				"pair " + itemA + "+" + itemB + " is not registered");
		assertTrue(ItemUseRegistry.isRegistered(itemB, itemA),
				"the registry is order-independent, so " + itemB + "+" + itemA + " must match");
	}

	private static long key(int itemA, int itemB) {
		int low = Math.min(itemA, itemB);
		int high = Math.max(itemA, itemB);
		return ((long) low << 32) | (high & 0xFFFFFFFFL);
	}
}
