package server.content.skills;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import core.util.ISAACRandomGen;
import server.game.players.Client;
import server.game.players.Player;

/**
 * Pins the pouch table and the two things a pouch has to get right: which essence it will take, and
 * which essence it hands back.
 *
 * <p>The bug these are named for is that a pouch took rune essence and nothing else — {@code
 * fillPouch} searched for {@code 1436} alone, so pure essence could not go in, and {@code emptyPouch}
 * handed {@code 1436} back regardless, so reaching the other kind meant a free upgrade or a silent
 * downgrade depending on which way it was wrong. {@link #pureEssenceFillsAndComesBackAsPureEssence}
 * is that bug.
 *
 * <p>The second half is the save file. A pouch stores a count, and the count predates pure essence
 * entirely, so {@link #aCountWithNoKindRecordedReadsAsRuneEssence} is the character file of every
 * account that existed before this change — a count, and no kind — and it decides whether that
 * essence is readable or stranded in a pouch that reports itself empty.
 *
 * <p>Nothing here asserts on message text: a client built for a unit test has no out stream to read
 * it back from, and the message is not the behaviour. Where a refusal matters the assertion is that
 * the pack and the pouch did not move.
 */
class PouchesTest {

	private static final int SMALL = 5509;
	private static final int MEDIUM = 5510;
	private static final int LARGE = 5512;
	private static final int GIANT = 5514;

	/** Spelled out rather than read from {@link Runecrafting}, so a rename cannot follow silently. */
	private static final int RUNE_ESSENCE = 1436;
	private static final int PURE_ESSENCE = 7936;

	private static final int SLOT = 1;

	// ---------------------------------------------------------------------------------------
	// The table.
	// ---------------------------------------------------------------------------------------

	@Test
	void everyPouchRowIsPinned() {
		// {pouch id, level, capacity}, small through giant, as the guide prints them. The row index
		// is load-bearing: Player.pouch and Player.pouchEssence are both keyed by it.
		int[][] expected = {
				{ SMALL, 1, 3 },
				{ MEDIUM, 25, 6 },
				{ LARGE, 50, 9 },
				{ GIANT, 75, 12 },
		};
		assertEquals(expected.length, Pouches.pouchData.length, "the pouch count");
		for (int index = 0; index < expected.length; index++) {
			assertArrayEquals(expected[index], Pouches.pouchData[index], "pouch row " + index);
			assertEquals(index, Pouches.forPouch(expected[index][0]),
					"row " + index + " must be the index the pouch state is keyed by");
		}
	}

	@Test
	void damagedPouchesAndNonPouchesAreNotPouches() {
		// 5511, 5513 and 5515 are the damaged pouches. In OSRS a damaged pouch holds less; this
		// revision has no row for them and no number to give them, so they are recognised as
		// nothing rather than mapped onto a full pouch and quietly made whole.
		for (int id : new int[] { 0, 5508, 5511, 5513, 5515, RUNE_ESSENCE, PURE_ESSENCE }) {
			assertEquals(-1, Pouches.forPouch(id), id + " must not be a pouch");
		}
	}

	// ---------------------------------------------------------------------------------------
	// Both essences.
	// ---------------------------------------------------------------------------------------

	@Test
	void pureEssenceFillsAndComesBackAsPureEssence() {
		// The reported bug. The old fill looked for 1436 alone, so this pack filled nothing; the old
		// empty would have handed back 1436, which is an essence the player never had.
		Client c = client();
		c.skills.playerLevel[Player.playerRunecrafting] = 25;
		give(c, 0, PURE_ESSENCE, 5);

		Pouches.fillPouch(c, MEDIUM);

		assertEquals(0, count(c, PURE_ESSENCE), "the pure essence goes into the pouch");
		assertEquals(5, c.pouch[1], "and the pouch counts it");
		assertEquals(PURE_ESSENCE, c.pouchEssence[1], "and remembers which kind it is");

		Pouches.emptyPouch(c, MEDIUM);

		assertEquals(5, count(c, PURE_ESSENCE), "and it comes back out as pure essence");
		assertEquals(0, count(c, RUNE_ESSENCE), "and never as rune essence");
		assertEquals(0, c.pouch[1], "leaving the pouch empty");
		assertEquals(0, c.pouchEssence[1], "with no kind left recorded");
	}

	@Test
	void runeEssenceFillsAndComesBackAsRuneEssence() {
		Client c = client();
		c.skills.playerLevel[Player.playerRunecrafting] = 1;
		give(c, 0, RUNE_ESSENCE, 2);

		Pouches.fillPouch(c, SMALL);
		assertEquals(2, c.pouch[0]);

		Pouches.emptyPouch(c, SMALL);

		assertEquals(2, count(c, RUNE_ESSENCE), "rune essence is unchanged by any of this");
		assertEquals(0, count(c, PURE_ESSENCE), "and is not turned into the better one");
	}

	@Test
	void anEmptyPouchTakesRuneEssenceFirst() {
		// The same order the altar spends them in, so a pack holding both fills and crafts in one
		// consistent order rather than two.
		Client c = client();
		c.skills.playerLevel[Player.playerRunecrafting] = 25;
		give(c, 0, RUNE_ESSENCE, 1);
		give(c, 1, PURE_ESSENCE, 1);

		Pouches.fillPouch(c, MEDIUM);

		assertEquals(0, count(c, RUNE_ESSENCE), "the rune essence is drawn first");
		assertEquals(1, count(c, PURE_ESSENCE), "leaving the pure essence where it was");
		assertEquals(RUNE_ESSENCE, c.pouchEssence[1]);
	}

	@Test
	void aPouchHoldingOneKindWillNotQuietlyTakeTheOther() {
		// A pouch is one count, not two stacks, so a second kind cannot be held alongside the first
		// and must not be converted into it. The refusal has to leave both the pack and the pouch
		// exactly as they were.
		Client c = client();
		c.skills.playerLevel[Player.playerRunecrafting] = 25;
		give(c, 0, RUNE_ESSENCE, 3);
		give(c, 1, PURE_ESSENCE, 3);

		Pouches.fillPouch(c, MEDIUM);
		assertEquals(3, c.pouch[1]);
		assertEquals(RUNE_ESSENCE, c.pouchEssence[1]);

		Pouches.fillPouch(c, MEDIUM); // only pure essence is left in the pack now

		assertEquals(3, c.pouch[1], "the pouch does not take a second kind");
		assertEquals(RUNE_ESSENCE, c.pouchEssence[1], "and does not convert what it already holds");
		assertEquals(3, count(c, PURE_ESSENCE), "and the pure essence is untouched");
	}

	// ---------------------------------------------------------------------------------------
	// Counts, gates and room.
	// ---------------------------------------------------------------------------------------

	@Test
	void onlyWhatFitsGoesInAndTheRestStaysInThePack() {
		Client c = client();
		c.skills.playerLevel[Player.playerRunecrafting] = 1;
		give(c, 0, RUNE_ESSENCE, 10);

		Pouches.fillPouch(c, SMALL); // capacity 3

		assertEquals(3, c.pouch[0], "a small pouch holds three");
		assertEquals(7, count(c, RUNE_ESSENCE), "and the rest stays in the pack");

		Pouches.fillPouch(c, SMALL);

		assertEquals(3, c.pouch[0], "filling a full pouch changes nothing");
		assertEquals(7, count(c, RUNE_ESSENCE), "and takes nothing from the pack");
	}

	@Test
	void theLevelGateIsOnFillingNotOnEmptying() {
		Client c = client();
		c.skills.playerLevel[Player.playerRunecrafting] = 1; // a medium pouch wants 25
		give(c, 0, PURE_ESSENCE, 4);

		Pouches.fillPouch(c, MEDIUM);

		assertEquals(0, c.pouch[1], "without the level nothing goes in");
		assertEquals(4, count(c, PURE_ESSENCE), "and the pack is untouched");

		// A pouch that is already holding essence is a different question: OSRS does not gate
		// emptying, and being unable to get your essence back out is worse than any level check.
		c.pouch[1] = 4;
		c.pouchEssence[1] = PURE_ESSENCE;

		Pouches.emptyPouch(c, MEDIUM);

		assertEquals(8, count(c, PURE_ESSENCE), "an existing pouch empties without a level");
		assertEquals(0, c.pouch[1]);
	}

	@Test
	void emptyingIsCappedByTheFreeSlotsAndKeepsTheRest() {
		// Essence does not stack, so a slot is one essence and the pack runs out long before the
		// pouch does. What does not fit has to stay behind rather than vanish.
		Client c = client();
		c.skills.playerLevel[Player.playerRunecrafting] = 75;
		give(c, 0, PURE_ESSENCE, 12);

		Pouches.fillPouch(c, GIANT);
		assertEquals(12, c.pouch[3]);

		for (int slot = 0; slot < 26; slot++) {
			give(c, slot, 526, 1); // bones, so the pack has two slots left
		}
		assertEquals(2, c.getItems().freeSlots());

		Pouches.emptyPouch(c, GIANT);

		assertEquals(2, count(c, PURE_ESSENCE), "only what fits comes out");
		assertEquals(10, c.pouch[3], "the rest stays in the pouch");
		assertEquals(PURE_ESSENCE, c.pouchEssence[3], "and it keeps its kind");
	}

	// ---------------------------------------------------------------------------------------
	// The save file.
	// ---------------------------------------------------------------------------------------

	@Test
	void aCountWithNoKindRecordedReadsAsRuneEssence() {
		// Every character file written before pouches could hold pure essence is exactly this: a
		// count, and no kind, because rune essence was the only thing a pouch could hold. Reading it
		// as empty would strand the essence in a pouch that reports itself empty, and reading it as
		// pure essence would upgrade it for free on the way out.
		Client c = client();
		c.pouch[0] = 3;
		c.pouchEssence[0] = 0;

		Pouches.emptyPouch(c, SMALL);

		assertEquals(3, count(c, RUNE_ESSENCE), "the legacy pouch empties as rune essence");
		assertEquals(0, count(c, PURE_ESSENCE), "and nothing upgrades it on the way out");
		assertEquals(0, c.pouch[0], "and the pouch is empty afterwards");
		assertEquals(0, c.pouchEssence[0], "with no kind left recorded");
	}

	@Test
	void aLegacyPouchCanStillBeToppedUp() {
		// The other half of the migration: filling a pouch that predates the recorded kind has to
		// top it up rather than refuse it as holding the wrong essence.
		Client c = client();
		c.skills.playerLevel[Player.playerRunecrafting] = 1;
		c.pouch[0] = 1;
		c.pouchEssence[0] = 0;
		give(c, 0, RUNE_ESSENCE, 5);

		Pouches.fillPouch(c, SMALL); // capacity 3, one already in

		assertEquals(3, c.pouch[0], "the pouch fills to its capacity");
		assertEquals(3, count(c, RUNE_ESSENCE), "taking only the two that fit");
		assertEquals(RUNE_ESSENCE, c.pouchEssence[0], "and the kind is recorded from now on");
	}

	@Test
	void anEmptyPouchEmptiesIntoNothing() {
		// The old empty said "You empty your medium pouch" whether or not anything came out.
		Client c = client();
		int before = c.getItems().freeSlots();

		Pouches.emptyPouch(c, MEDIUM);

		assertEquals(before, c.getItems().freeSlots(), "an empty pouch adds nothing");
		assertEquals(0, count(c, RUNE_ESSENCE));
		assertEquals(0, count(c, PURE_ESSENCE));
	}

	@Test
	void twoPlayersPouchesAreIndependent() {
		// Cheap, and worth pinning: the pouch state lived in static fields in a neighbouring skill
		// until recently, which is the shape of bug that lets one player's pouch answer another's
		// click.
		Client a = client();
		a.skills.playerLevel[Player.playerRunecrafting] = 25;
		give(a, 0, RUNE_ESSENCE, 3);

		Client b = client();
		b.skills.playerLevel[Player.playerRunecrafting] = 25;
		give(b, 0, PURE_ESSENCE, 3);

		Pouches.fillPouch(a, MEDIUM);
		Pouches.fillPouch(b, MEDIUM);

		assertEquals(RUNE_ESSENCE, a.pouchEssence[1], "the first player's pouch holds rune essence");
		assertEquals(PURE_ESSENCE, b.pouchEssence[1], "the second player's holds pure essence");
		assertEquals(3, a.pouch[1]);
		assertEquals(3, b.pouch[1]);
	}

	// ---------------------------------------------------------------------------------------
	// Helpers, the same shape RunecraftingTest uses.
	// ---------------------------------------------------------------------------------------

	private static Client client() {
		Client c = new Client(null, SLOT);
		c.getOutStream().packetEncryption = new ISAACRandomGen(new int[] { 1, 2, 3, 4 });
		c.expModifier = 1;
		return c;
	}

	/** Puts {@code amount} of {@code id} in {@code slot}, the way a login would find it. */
	private static void give(Client c, int slot, int id, int amount) {
		c.playerItems[slot] = id + 1;
		c.playerItemsN[slot] = amount;
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
}
