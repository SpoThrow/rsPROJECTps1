package server.content.skills;

import server.game.items.ItemDefinitions;
import server.game.players.Client;
import server.game.players.Player;

/**
 * The four runecrafting pouches, small through giant: extra essence carried outside the inventory.
 *
 * <p><b>Why they exist at all.</b> Essence is not stackable — not here and not in OSRS — so the
 * twenty-eight inventory slots are the real limit on a runecrafting trip, and a pouch holding three
 * to twelve more essence is worth the bank trip it saves. That is also why an altar binds only what
 * is in the pack and never what is in a pouch: you empty them into the inventory first, as the real
 * routine does.
 *
 * <p><b>What was wrong.</b> Every pouch could hold rune essence ({@value Runecrafting#RUNE_ESSENCE})
 * and nothing else. {@code fillPouch} looked for that one id and {@code emptyPouch} handed the same
 * id back, so pure essence ({@value Runecrafting#PURE_ESSENCE}) — the members' essence, and what the
 * essence impling drops — could not go into a pouch at all. The altar change that made both essences
 * craftable stopped at the pouch. Two smaller falsehoods went with it: a pouch said "You fill your
 * medium pouch" when it was already full or when the pack held no essence, and said "You empty your
 * medium pouch" with nothing coming out, either of which reads as a pouch that has eaten the
 * essence.
 *
 * <p><b>One kind at a time, and remembered.</b> A pouch stores a count rather than a pair of stacks,
 * and that count has been saved as {@code pouch = a\tb\tc\td} for long enough that its meaning is not
 * up for renegotiation. So which essence a pouch holds travels beside the count in
 * {@link Player#pouchEssence}, and emptying hands back what went in. Without that, a rune essence
 * would come out of a pouch as pure essence — a free upgrade — or a pure essence would come out as
 * rune essence, which is the same bug the other way up. A pouch already holding one kind therefore
 * will not silently swallow the other; it says so instead of guessing.
 *
 * <p>Filling an empty pouch takes rune essence first and pure essence second, which is the order
 * {@link Runecrafting} spends them in at an altar, so a pack holding both pouches and crafts in the
 * same order either way.
 *
 * <p><b>The level gate is on filling only.</b> Emptying a pouch is not the moment to discover you
 * are 24 runecrafting, and OSRS does not stop you either.
 *
 * <p><b>Not here:</b> the damaged pouches ({@code 5511}, {@code 5513}, {@code 5515}) are not in the
 * table, so clicking one still does nothing. They are their own feature — a pouch that degrades and
 * holds less — rather than a missing row.
 */
public class Pouches {

	/** {pouch item id, runecrafting level to fill it, capacity}, small through giant. */
	public static final int[][] pouchData = {
		{5509, 1, 3},
		{5510, 25, 6},
		{5512, 50, 9},
		{5514, 75, 12},
	};

	/**
	 * @return the index of {@code itemId} in {@link #pouchData} — the same index {@link Player#pouch}
	 *         and {@link Player#pouchEssence} are keyed by — or {@code -1} if it is not a pouch.
	 */
	public static int forPouch(final int itemId) {
		for (int index = 0; index < pouchData.length; index++) {
			if (pouchData[index][0] == itemId) {
				return index;
			}
		}
		return -1;
	}

	/**
	 * The kind of essence a non-empty pouch is holding.
	 *
	 * <p>A count with no kind recorded reads as rune essence, and that is a migration rather than a
	 * guess: every character file written before pouches could hold pure essence is in exactly that
	 * state — a count and no kind — because rune essence was the only thing a pouch could hold.
	 * Calling such a pouch empty would strand its essence somewhere the player cannot reach it.
	 */
	private static int kindOf(final Client c, final int index) {
		final int kind = c.pouchEssence[index];
		if (kind == 0 && c.pouch[index] > 0) {
			return Runecrafting.RUNE_ESSENCE;
		}
		return kind;
	}

	public static void fillPouch(final Client c, final int itemId) {
		final int index = forPouch(itemId);
		if (index < 0) {
			return;
		}
		final String name = ItemDefinitions.name(itemId);
		if (c.skills.playerLevel[Player.playerRunecrafting] < pouchData[index][1]) {
			c.sendMessage("You need a runecrafting level of " + pouchData[index][1]
					+ " to fill this pouch.");
			return;
		}
		final int space = pouchData[index][2] - c.pouch[index];
		if (space <= 0) {
			c.sendMessage("Your " + name + " is full.");
			return;
		}

		final int essence;
		if (c.pouch[index] <= 0) {
			if (c.getItems().playerHasItem(Runecrafting.RUNE_ESSENCE)) {
				essence = Runecrafting.RUNE_ESSENCE;
			} else if (c.getItems().playerHasItem(Runecrafting.PURE_ESSENCE)) {
				essence = Runecrafting.PURE_ESSENCE;
			} else {
				c.sendMessage("You have no essence to put in your " + name + ".");
				return;
			}
		} else {
			essence = kindOf(c, index);
		}

		final int moved = Math.min(space, c.getItems().getItemAmount(essence));
		if (moved <= 0) {
			c.sendMessage("Your " + name + " already holds "
					+ ItemDefinitions.name(essence).toLowerCase() + ", and you have no more of it.");
			return;
		}
		c.getItems().deleteItem2(essence, moved);
		c.pouch[index] += moved;
		c.pouchEssence[index] = essence;
		c.sendMessage("You fill your " + name + ".");
	}

	public static void emptyPouch(final Client c, final int itemId) {
		final int index = forPouch(itemId);
		if (index < 0) {
			return;
		}
		final String name = ItemDefinitions.name(itemId);
		final int stored = c.pouch[index];
		if (stored <= 0) {
			c.sendMessage("Your " + name + " is empty.");
			return;
		}
		final int space = c.getItems().freeSlots();
		if (space <= 0) {
			c.sendMessage("You need a free inventory slot to empty your " + name + ".");
			return;
		}
		final int moved = Math.min(stored, space);
		c.getItems().addItem(kindOf(c, index), moved);
		c.pouch[index] -= moved;
		if (c.pouch[index] <= 0) {
			c.pouch[index] = 0;
			c.pouchEssence[index] = 0;
		}
		c.sendMessage("You empty your " + name + ".");
	}

	public static void checkPouch(final Client c, final int itemId) {
		final int index = forPouch(itemId);
		if (index < 0) {
			return;
		}
		if (c.pouch[index] <= 0) {
			c.sendMessage("Your " + ItemDefinitions.name(itemId) + " is empty.");
			return;
		}
		c.sendMessage("You have " + c.pouch[index] + " "
				+ ItemDefinitions.name(kindOf(c, index)).toLowerCase() + " in your "
				+ ItemDefinitions.name(itemId) + ".");
	}
}
