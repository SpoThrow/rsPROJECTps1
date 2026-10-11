package server.content.skills;

import server.Config;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.items.ItemAssistant;
import server.game.players.Client;
import server.game.players.Player;

/**
 * Gold jewellery at interface 4161: a gold bar, a gem and a mould.
 *
 * <p><b>One table, and the interface is built from it.</b> This used to be two lists that were
 * supposed to agree — the recipes, and a second array of ids to draw in the interface's slots —
 * and they did not: the recipe table named {@code 6583} as the onyx ring while the interface drew
 * {@code 6575}. 6583 is the Ring of stone, an enchanted ring, so clicking the onyx ring in the
 * mould interface matched no recipe and returned in silence: the product the interface offered was
 * the one product that could not be made. The rows are now derived from {@link Product} in
 * declaration order, so a slot and its recipe cannot drift apart again, and
 * {@code tools/mould-interface-probe} reads the interface out of the real cache to check the two
 * against each other rather than trusting either.
 *
 * <p><b>The interface cannot ask for "all", and that is what the chatbox is for.</b> Read out of
 * the cache, containers 4233/4239/4245 carry exactly three actions — {@code Make}, {@code Make 5},
 * {@code Make 10} — and two empty slots. A make-all click is therefore not something this client
 * can send: the option does not exist to be clicked. So the amount prompt is the chatbox, on the
 * same {@link CreationMenu} the chisel, the tanner and the potter already use, which offers
 * 1/5/10/28. The interface's own {@code Make 5} and {@code Make 10} are kept as the shortcuts they
 * already were; {@code Make} — the left click — opens the prompt instead of silently making one,
 * which costs a click and is the only way make-all can be reached at all.
 *
 * <p><b>The batch is ticked, and it was not.</b> {@code mouldItem} ran a {@code while} over the
 * whole stack in the game cycle of the click: no animation, no way to stop, and the pack could be
 * emptied before the player saw a thing. It is now one item every {@link #ACTION_CYCLES}, with all
 * of its state in the event closure rather than on the class, and it stops when the player walks.
 */
public class JewelryMaking extends CraftingData {

	/** The bar every recipe consumes. */
	public static final int GOLD_BAR = 2357;

	/** Sentinel for "this recipe takes no gem", which is every plain gold piece. */
	public static final int NO_GEM = -1;

	/**
	 * Cycles per item. Two — the cadence the anvil and the fletching knife run at — rather than
	 * gem cutting's four, because making jewellery is a shorter action than cutting a gem. The old
	 * code was instantaneous, so there is no pre-existing cadence to preserve here.
	 */
	private static final int ACTION_CYCLES = 2;

	/**
	 * Its own event id, so a cancelled batch is taken off the scheduler rather than left to notice
	 * the flag on a later tick, and so stopping it cannot stop another crafting action.
	 */
	private static final int JEWELRY_EVENT = 4621;

	/**
	 * Making jewellery is the furnace pose, so it is the same animation smelting and the pottery
	 * oven use. The old path played none at all.
	 */
	private static final int CRAFT_ANIMATION = 899;

	/**
	 * The three moulds, and the three rows of interface 4161 each one unlocks.
	 *
	 * <p>The frames are the interface's own, read out of the cache: {@code container} is the
	 * seven-slot item box the server fills, {@code modelFrame} holds the mould icon shown when the
	 * mould is missing, and {@code labelFrame} carries the line telling the player which mould that
	 * is. Rows are 7 wide because the interface draws seven — one per gem, plain gold first.
	 */
	public enum Kind {

		RING(1592, 4233, 4229, 4230, "ring", "rings"),
		NECKLACE(1597, 4239, 4235, 4236, "necklace", "necklaces"),
		AMULET(1595, 4245, 4241, 4242, "amulet", "amulets");

		final int mould;
		final int container;
		final int modelFrame;
		final int labelFrame;
		final String one;
		final String many;

		Kind(int mould, int container, int modelFrame, int labelFrame, String one, String many) {
			this.mould = mould;
			this.container = container;
			this.modelFrame = modelFrame;
			this.labelFrame = labelFrame;
			this.one = one;
			this.many = many;
		}

		/**
		 * What is written into this row's label frame when the mould is not in the pack.
		 *
		 * <p>Interface 4161 ships this exact sentence as its own default text, which the old code
		 * overwrote with a hand-typed copy — and the copy for amulets read "You need a amulet mould
		 * to craft necklaces". The sentence is written here rather than left to the cache because
		 * the label is cleared once the mould is held, and a frame cannot be un-cleared.
		 */
		String missingMould() {
			return "You need " + article(one) + " " + one + " mould to craft " + many + ".";
		}
	}

	/**
	 * Every recipe, in the order interface 4161 draws each kind.
	 *
	 * <p>The levels and the experience are the guide's own. The gem is the <em>cut</em> gem, which
	 * is what {@link GemCutting} produces, and {@link #NO_GEM} marks the plain gold pieces a bar
	 * makes on its own.
	 *
	 * <p><b>Declaration order is load-bearing</b>: {@link #productsFor} hands a kind's rows to the
	 * interface in this order, so a product's slot is its position here. Plain gold is first for
	 * each kind, which is where both the guide and the interface put it.
	 */
	public enum Product {

		GOLD_RING(1635, NO_GEM, 5, 15, Kind.RING),
		SAPPHIRE_RING(1637, 1607, 20, 40, Kind.RING),
		EMERALD_RING(1639, 1605, 27, 55, Kind.RING),
		RUBY_RING(1641, 1603, 34, 70, Kind.RING),
		DIAMOND_RING(1643, 1601, 43, 85, Kind.RING),
		DRAGONSTONE_RING(1645, 1615, 55, 100, Kind.RING),
		/** The unenchanted onyx ring. 6583 is the Ring of stone, which is the enchanted one. */
		ONYX_RING(6575, 6573, 67, 115, Kind.RING),

		GOLD_NECKLACE(1654, NO_GEM, 6, 20, Kind.NECKLACE),
		SAPPHIRE_NECKLACE(1656, 1607, 22, 55, Kind.NECKLACE),
		EMERALD_NECKLACE(1658, 1605, 29, 60, Kind.NECKLACE),
		RUBY_NECKLACE(1660, 1603, 40, 75, Kind.NECKLACE),
		DIAMOND_NECKLACE(1662, 1601, 56, 90, Kind.NECKLACE),
		DRAGONSTONE_NECKLACE(1664, 1615, 72, 105, Kind.NECKLACE),
		ONYX_NECKLACE(6577, 6573, 82, 120, Kind.NECKLACE),

		GOLD_AMULET(1673, NO_GEM, 8, 30, Kind.AMULET),
		SAPPHIRE_AMULET(1675, 1607, 24, 65, Kind.AMULET),
		EMERALD_AMULET(1677, 1605, 31, 70, Kind.AMULET),
		RUBY_AMULET(1679, 1603, 50, 85, Kind.AMULET),
		DIAMOND_AMULET(1681, 1601, 70, 100, Kind.AMULET),
		DRAGONSTONE_AMULET(1683, 1615, 80, 150, Kind.AMULET),
		ONYX_AMULET(6579, 6573, 90, 165, Kind.AMULET);

		final int product;
		final int gem;
		final int level;
		final int xp;
		final Kind kind;

		Product(int product, int gem, int level, int xp, Kind kind) {
			this.product = product;
			this.gem = gem;
			this.level = level;
			this.xp = xp;
			this.kind = kind;
		}

		/** True when this recipe is a bar on its own, with nothing set into it. */
		public boolean hasGem() {
			return gem != NO_GEM;
		}
	}

	/** Each kind's products in slot order, built once because both the interface and the clicks want it. */
	private static final Product[][] ROWS = buildRows();

	private static Product[][] buildRows() {
		Kind[] kinds = Kind.values();
		Product[][] rows = new Product[kinds.length][];
		for (int k = 0; k < kinds.length; k++) {
			int count = 0;
			for (Product p : Product.values()) {
				if (p.kind == kinds[k]) {
					count++;
				}
			}
			rows[k] = new Product[count];
			int slot = 0;
			for (Product p : Product.values()) {
				if (p.kind == kinds[k]) {
					rows[k][slot++] = p;
				}
			}
		}
		return rows;
	}

	/** The products of a kind, in the order the interface's slots hold them. */
	public static Product[] productsFor(Kind kind) {
		return ROWS[kind.ordinal()].clone();
	}

	/** The recipe for a product id, or null when the id is not one of them. */
	public static Product forProduct(int product) {
		for (Product p : Product.values()) {
			if (p.product == product) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Shows the mould interface, filling each kind's seven slots with its products.
	 *
	 * <p>A kind whose mould is not held gets its slots emptied and its label filled with the reason,
	 * which is what the interface was drawn for. Writing {@code -1} into every slot matters as much
	 * as writing the products: the interface keeps whatever was last drawn in a box nobody
	 * overwrites, so a second visit would otherwise still show the first visit's items.
	 */
	public static void mouldInterface(Client c) {
		c.getPA().showInterface(4161);
		// The chatbox is the other half of this interaction now, so it does not survive the
		// interface being reopened: a prompt left over from a previous visit would make the
		// product of an abandoned click.
		CreationMenu.close(c);
		for (Kind kind : Kind.values()) {
			boolean hasMould = c.getItems().playerHasItem(kind.mould, 1);
			Product[] products = productsFor(kind);
			for (int slot = 0; slot < products.length; slot++) {
				c.getPA().sendFrame34(slotItem(c, kind, slot), slot, kind.container, 1);
			}
			if (hasMould) {
				c.getPA().sendFrame246(kind.modelFrame, 0, -1);
				c.getPA().sendFrame126("", kind.labelFrame);
			} else {
				c.getPA().sendFrame246(kind.modelFrame, 120, kind.mould);
				c.getPA().sendFrame126(kind.missingMould(), kind.labelFrame);
			}
		}
	}

	/**
	 * What interface 4161 should be holding in one of its slots: the product, or {@code -1} when the
	 * mould that unlocks the row is not in the pack.
	 *
	 * <p>Split out of {@link #mouldInterface} so the rule can be asserted without reading the
	 * frames the server sent — the frames are the interface's business, the choice of what goes in
	 * them is this one's.
	 *
	 * @return the product id for the slot, or -1 when the row is locked
	 */
	static int slotItem(Client c, Kind kind, int slot) {
		if (slot < 0 || slot >= productsFor(kind).length) {
			return -1;
		}
		return c.getItems().playerHasItem(kind.mould, 1) ? productsFor(kind)[slot].product : -1;
	}

	/**
	 * The interface's {@code Make} click — the left click — for the product in that slot.
	 *
	 * <p>Opens the chatbox on this one product rather than making one, because the interface has no
	 * way to ask for more than ten and no way to ask for all. The product is carried by
	 * {@link CreationMenu} rather than by a field here, so the menu and the click that answers it
	 * cannot disagree about what is being made.
	 */
	public static void openAmountPrompt(Client c, int product) {
		if (c.playerIsCrafting) {
			return;
		}
		Product p = forProduct(product);
		if (!reportMissing(c, p)) {
			return;
		}
		CreationMenu.open(c, CreationMenu.Kind.JEWELLERY, new int[] { product });
	}

	/**
	 * The interface's {@code Make 5} and {@code Make 10}: the click already chose the amount.
	 *
	 * <p>Kept because they are the interface's own shortcuts and they work; only the left click
	 * routes through the chatbox.
	 */
	public static void makeNow(Client c, int product, int amount) {
		if (c.playerIsCrafting) {
			return;
		}
		Product p = forProduct(product);
		if (!reportMissing(c, p)) {
			return;
		}
		c.getPA().removeAllWindows();
		start(c, p, amount);
	}

	/**
	 * A row of the chatbox: make {@code amount} of the product the menu was opened on.
	 *
	 * <p>Called from {@link CreationMenu#route}. Every condition is re-checked here rather than
	 * trusted from the click that opened the menu — the bar, the gem and the mould can all have
	 * left the pack in between, and the menu stays open while that happens.
	 */
	public static void select(Client c, int product, int amount) {
		if (c.playerIsCrafting || amount <= 0) {
			return;
		}
		Product p = forProduct(product);
		if (p == null) {
			return;
		}
		if (!reportMissing(c, p)) {
			return;
		}
		CreationMenu.close(c);
		start(c, p, amount);
	}

	/**
	 * Checks the level, the bar, the gem and the mould, telling the player what is missing.
	 *
	 * <p>All four are checked at every entry point rather than only when the interface is drawn:
	 * the mould and the bar are both things the player can drop or bank while the interface is
	 * open, and without this the only symptom would be a product that silently never appears.
	 *
	 * @return true when the recipe can be made
	 */
	static boolean reportMissing(Client c, Product p) {
		if (p == null) {
			return false;
		}
		if (c.skills.playerLevel[Player.playerCrafting] < p.level) {
			c.sendMessage("You need a Crafting level of " + p.level + " to make this.");
			return false;
		}
		if (!c.getItems().playerHasItem(GOLD_BAR, 1)) {
			c.sendMessage("You need a gold bar to make this.");
			return false;
		}
		if (p.hasGem() && !c.getItems().playerHasItem(p.gem, 1)) {
			c.sendMessage(needMessage(ItemAssistant.getItemName(p.gem)));
			return false;
		}
		if (!c.getItems().playerHasItem(p.kind.mould, 1)) {
			c.sendMessage("You need " + article(p.kind.one) + " " + p.kind.one
					+ " mould to make this.");
			return false;
		}
		return true;
	}

	/**
	 * Makes one item every {@link #ACTION_CYCLES}, up to {@code amount} times.
	 *
	 * <p>The recipe and the amount travel in the closure rather than through fields, so two players
	 * making jewellery at once cannot read each other's choice — the same shape as the gem cutting
	 * and herblore actions, and the reason the old static-field loop had to go.
	 *
	 * <p>The materials are re-read on every tick, and a tick with no room for the product is not
	 * spent: jewellery consumes two items for one, so a full pack would otherwise eat a bar and a
	 * gem per tick and hand nothing back.
	 */
	private static void start(final Client c, final Product p, final int amount) {
		c.playerIsCrafting = true;
		c.startAnimation(CRAFT_ANIMATION);
		CycleEventHandler.addEvent(JEWELRY_EVENT, c, new CycleEvent() {

			private int left = amount;

			@Override
			public void execute(CycleEventContainer container) {
				if (!c.playerIsCrafting || left <= 0 || !canConsume(c, p)) {
					container.stop();
					return;
				}
				c.getItems().deleteItem2(GOLD_BAR, 1);
				if (p.hasGem()) {
					c.getItems().deleteItem2(p.gem, 1);
				}
				c.getItems().addItem(p.product, 1);
				c.getPA().addSkillXP(p.xp * Config.CRAFTING_EXPERIENCE, Player.playerCrafting);
				c.getPA().refreshSkill(Player.playerCrafting);
				c.sendMessage("You make " + article(name(p)) + " "
						+ name(p).toLowerCase() + ".");
				c.startAnimation(CRAFT_ANIMATION);
				left--;
			}

			@Override
			public void stop() {
				cancel(c);
			}
		}, ACTION_CYCLES);
	}

	/** Whether the tick can be paid for: the recipe's materials, and somewhere to put the product. */
	private static boolean canConsume(Client c, Product p) {
		if (!c.getItems().playerHasItem(GOLD_BAR, 1)) {
			return false;
		}
		if (p.hasGem() && !c.getItems().playerHasItem(p.gem, 1)) {
			return false;
		}
		// Consuming two items into one never changes the free-slot count, so testing it once is
		// enough to know the product fits.
		return c.getItems().freeSlots() > 0;
	}

	private static String name(Product p) {
		return ItemAssistant.getItemName(p.product);
	}

	/** "a" or "an", by the sound of the name this server actually gives the item. */
	private static String article(String item) {
		String s = item.toLowerCase();
		return s.startsWith("a") || s.startsWith("e") || s.startsWith("i") || s.startsWith("o")
				|| s.startsWith("u") ? "an" : "a";
	}

	/** The "you need a bar and a gem" line, keeping the old wording but with the right article. */
	static String needMessage(String gem) {
		return "You need a gold bar and " + article(gem) + " " + gem.toLowerCase()
				+ " to make this.";
	}

	/**
	 * Stops a running batch, if one is running.
	 *
	 * <p>The event stop is unconditional and the flag is not, for the reason {@code Spinning.cancel}
	 * documents: {@code playerIsCrafting} means "some crafting action is running", so whichever
	 * cancel runs second must still remove its own event rather than find the flag already cleared.
	 */
	public static void cancel(Client c) {
		if (c.playerIsCrafting) {
			c.playerIsCrafting = false;
			c.startAnimation(65535);
		}
		CycleEventHandler.stopEvents(c, JEWELRY_EVENT);
	}

	/**
	 * A ball of wool on an unstrung amulet. Untouched by the rewrite above: stringing is a
	 * two-item recipe with no mould, no interface and no amount, which makes it the one part of
	 * jewellery making that is not a menu question.
	 */
	public static void stringAmulet(final Client c, final int itemUsed, final int usedWith) {
		final int amuletId = (itemUsed == 1759 ? usedWith : itemUsed);
		for (final amuletData a : amuletData.values()) {
			if (amuletId == a.getAmuletId()) {
				c.getItems().deleteItem(1759, 1);
				c.getItems().deleteItem(amuletId, 1);
				c.getItems().addItem(a.getProduct(), 1);
				c.getPA().addSkillXP(4 * Config.CRAFTING_EXPERIENCE, Player.playerCrafting);
			}
		}
	}
}
