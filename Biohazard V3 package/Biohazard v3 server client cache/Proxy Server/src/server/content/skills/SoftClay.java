package server.content.skills;

import server.game.players.Client;

/**
 * Soft clay — the step the Crafting guide never prints and the potter's wheel cannot work without.
 *
 * <p>The wheel and the oven landed first (§4c) reading item {@code 1761}, and nothing in this
 * server produced it: the id appeared only as a baby- and young-imping reward, so the Pottery tab
 * was reachable in name only. Water on clay is what closes that, and it is the whole of the
 * action — no level, no experience, no animation, one clay per click.
 *
 * <p>Nothing here is ticked, and that is a decision rather than an omission. Both items are
 * already in the pack, so the result is a swap of two slots: there is no walk to stop and no world
 * object to pace against. The tick loops the wheel and the loom need exist because they act on the
 * world. Nor is this a repeating action — a repeat would have to pick a container, and a player
 * carrying a jug and a waterskin would watch it choose one.
 *
 * <p><b>The zero is the real number, not a placeholder.</b> Making soft clay gives no experience
 * in OSRS, so any figure here would be invented. The action still resolves its ids through
 * {@code QolValidatorTest} with every other table.
 */
public final class SoftClay {

	/** What the player holds: the thing the wheel eats and the water container is used on. */
	public static final int CLAY = 434;

	/**
	 * What comes out. Read by {@link Pottery}, which reads it as {@link Pottery#SOFT_CLAY} — the
	 * two constants are pinned equal by a test rather than left to agree by hand.
	 */
	public static final int SOFT_CLAY = 1761;

	/**
	 * The experience this action awards: none. Named rather than left out so the test that pins it
	 * reads as a decision instead of an accidental omission.
	 */
	public static final double XP = 0;

	/**
	 * The full water container -> the container you are left holding.
	 *
	 * <p>Bucket, jug and vial are the three plain pairs. The waterskin is the one that is a ladder
	 * rather than a pair — four doses down to the empty skin — so it is written out row by row
	 * instead of derived, because that is the part a mistake would hide in.
	 *
	 * <p>Shorter than the wiki's list, deliberately. A bowl of water ({@code 4454}) has no
	 * definition in this revision's {@code item.cfg} at all, and a watering can ({@code 5331}) is
	 * modelled as an uncharged tool whose doses live in the farming layer, so neither is claimed
	 * here rather than registered against a full form this server cannot back.
	 */
	public enum WaterContainer {

		BUCKET_OF_WATER(1929, 1925, "bucket"),
		JUG_OF_WATER(1937, 1935, "jug"),
		VIAL_OF_WATER(227, 229, "vial"),
		WATERSKIN_FULL(1823, 1825, "waterskin"),
		WATERSKIN_THREE(1825, 1827, "waterskin"),
		WATERSKIN_TWO(1827, 1829, "waterskin"),
		WATERSKIN_ONE(1829, 1831, "waterskin");

		private final int full;
		private final int empty;
		private final String name;

		WaterContainer(int full, int empty, String name) {
			this.full = full;
			this.empty = empty;
			this.name = name;
		}

		/** The container as the player is holding it. */
		public int getFull() {
			return full;
		}

		/** What is left once the water is gone. */
		public int getEmpty() {
			return empty;
		}

		/** Named only so the message can say which container emptied. */
		public String getName() {
			return name;
		}
	}

	private SoftClay() {
	}

	/**
	 * @return the container {@code itemId} is a full form of, or {@code null}. Null rather than a
	 *         throw because the wheel and the loom already ask about unrelated ids this way.
	 */
	public static WaterContainer forWater(int itemId) {
		for (WaterContainer container : WaterContainer.values()) {
			if (container.full == itemId) {
				return container;
			}
		}
		return null;
	}

	/**
	 * Water on clay. Called from {@link server.game.players.actions.items.ItemUseRegistry} for
	 * every {@code (clay, container)} pair, so the two ids arrive in whichever order the client
	 * sent them and the pair is normalised before it gets here.
	 *
	 * <p>The order of the four item operations matters: each delete frees the slot its add then
	 * fills, so the swap never needs a free slot it does not have and a player with a full pack can
	 * still soften clay. Deleting first and adding second is what makes that true.
	 */
	public static void mix(Client c, int itemUsed, int useWith) {
		WaterContainer container = forWater(itemUsed == CLAY ? useWith : itemUsed);
		if (container == null) {
			return;
		}
		int claySlot = c.getItems().getItemSlot(CLAY);
		int waterSlot = c.getItems().getItemSlot(container.full);
		if (claySlot < 0 || waterSlot < 0) {
			return;
		}
		c.getItems().deleteItem(CLAY, claySlot, 1);
		c.getItems().addItem(SOFT_CLAY, 1);
		c.getItems().deleteItem(container.full, waterSlot, 1);
		c.getItems().addItem(container.empty, 1);
		c.sendMessage("You mix the clay and the water, and the " + container.name + " runs empty.");
	}
}
