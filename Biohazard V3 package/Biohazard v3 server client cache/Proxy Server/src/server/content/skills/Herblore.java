package server.content.skills;

import server.Config;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.items.ItemAssistant;
import server.game.players.Client;
import server.game.players.Player;

/**
 * Herblore: cleaning herbs, grinding, unfinished potions and finished potions.
 *
 * <p><b>Everything here is one action per click, and it used to be a batch.</b> The four families
 * were driven by four static fields ({@code itemToDelete}, {@code itemToAdd}, {@code potExp} and
 * friends) that the click filled in and a tick loop read back out, with the amounts chosen through
 * a make-X menu. Two things were wrong with that, and only one of them is about realism:
 *
 * <ul>
 * <li>A static field is shared by every player on the server. Two people mixing at once — one an
 * attack potion, one a super strength — wrote to the same four fields, so the tick loop could
 * deliver one player's product to the other, or consume the wrong materials.
 * <li>The menus are not how herblore works in OSRS. A herb on a vial of water is one potion per
 * click, and so is an ingredient on an unfinished potion: there is no "how many would you like"
 * for either. Fletching's fifteen-at-a-time and ten-at-a-time batches exist because OSRS makes
 * arrows and bolts in batches; herblore does not.
 * </ul>
 *
 * <p>So the tables below are read directly by the action instead of being flattened into shared
 * state, which is also what lets the pairs be registered in
 * {@link server.game.players.actions.items.ItemUseRegistry} — see {@code HerbloreItemUses}. Every
 * recipe is a pair of item ids, so the registry owns the whole family rather than only part of it.
 *
 * <p><b>Cleaning is not ticked, and that is a decision.</b> A grimy herb is one item in the pack
 * with no second item and no world object, so there is nothing for a tick to pace against and
 * nothing a walk can interrupt — the same reasoning as {@code SoftClay}. OSRS has no cleaning
 * animation, so there is none here; the message and the experience are the whole of the feedback.
 * Mixing and grinding are ticked at two cycles, the fletching cadence, because they are the actions
 * a player repeats.
 *
 * <p><b>Levels are this revision's own guide except where the guide is demonstrably wrong.</b> The
 * Herblore guide prints a level beside every potion and every herb, and those printed levels are
 * the promise this table has to keep, so they are what is used. One pair of rows is swapped in the
 * guide — it prints ranging at 69 and antifire at 72, and the experience values in this very table
 * (163 for ranging, 158 for antifire) are the OSRS ones for 72 and 69 — so the guide was corrected
 * rather than the table. Rows the guide does not print at all (combat, hunter) are kept, because a
 * table may know more than the guide's summary.
 */
public class Herblore {

	/** The plain base for every herb that is not a weapon poison. */
	public static final int VIAL_OF_WATER = 227;
	/** The empty vial, left behind once a dose is drunk rather than by anything here. */
	public static final int VIAL = 229;
	/** The base for weapon poison+ and weapon poison++, in place of a vial. */
	public static final int COCONUT_MILK = 5935;
	/** Required to grind, and not consumed by it. */
	public static final int PESTLE_AND_MORTAR = 233;

	/** Mixing an unfinished or a finished potion. */
	static final int MIX_ANIMATION = 363;
	/** Grinding with the pestle. */
	static final int GRIND_ANIMATION = 364;

	/**
	 * Event id for the ticked herblore action, non-zero for the same reason fletching's is: it is
	 * what lets {@link #cancel} stop this action and nothing else the player is running.
	 */
	private static final int HERBLORE_EVENT = 4615;

	/** Two cycles per action, matching fletching rather than the old one-cycle loop. */
	private static final int ACTION_CYCLES = 2;

	/** Sentinel for "this recipe has only one material". */
	private static final int NO_SECOND_MATERIAL = 0;

	/**
	 * Grimy herb -> clean herb, with the level and experience cleaning awards.
	 *
	 * <p>Sixteen rows: the fourteen the guide prints on its Herbs tab, plus spirit weed and wergali,
	 * which this revision's item table carries and the guide does not. The levels and the experience
	 * are the OSRS ones, and they are also what the guide prints — including guam, which this table
	 * used to clean at level 1 instead of 3.
	 */
	public enum Cleaning {

		GUAM(199, 249, 3, 3),
		MARRENTILL(201, 251, 5, 4),
		TARROMIN(203, 253, 11, 5),
		HARRALANDER(205, 255, 20, 6),
		RANARR(207, 257, 25, 8),
		TOADFLAX(3049, 2998, 30, 8),
		WERGALI(14836, 14854, 30, 8),
		SPIRIT_WEED(12174, 12172, 35, 8),
		IRIT(209, 259, 40, 9),
		AVANTOE(211, 261, 48, 10),
		KWUARM(213, 263, 54, 11),
		SNAPDRAGON(3051, 3000, 59, 12),
		CADANTINE(215, 265, 65, 13),
		LANTADYME(2485, 2481, 67, 13),
		DWARF_WEED(217, 267, 70, 14),
		TORSTOL(219, 269, 75, 15);

		private final int grimy;
		private final int clean;
		private final int levelReq;
		private final int xp;

		Cleaning(int grimy, int clean, int levelReq, int xp) {
			this.grimy = grimy;
			this.clean = clean;
			this.levelReq = levelReq;
			this.xp = xp;
		}

		public int getGrimy() {
			return grimy;
		}

		public int getClean() {
			return clean;
		}

		public int getLevelReq() {
			return levelReq;
		}

		public int getXp() {
			return xp;
		}
	}

	/**
	 * Pestle and mortar on an item, producing a powder.
	 *
	 * <p>No level and no experience: grinding is preparation, not potion making, and OSRS awards
	 * nothing for it. The pestle is not consumed — it is the tool, and every row shares it, which is
	 * why the action is registered as the pair (item, {@link Herblore#PESTLE_AND_MORTAR}).
	 */
	public enum Grinding {

		UNICORN_HORN(237, 235, "unicorn horn"),
		CHOCOLATE_BAR(1973, 1975, "chocolate bar"),
		BIRD_NEST(5075, 6693, "bird's nest"),
		KEBBIT_TEETH(10109, 10111, "kebbit teeth"),
		BLUE_DRAGON_SCALE(243, 241, "blue dragon scale"),
		DESERT_GOAT_HORN(9735, 9736, "desert goat horn"),
		DIAMOND_ROOT(14703, 14704, "diamond root"),
		RUNE_SHARDS(6466, 6467, "rune shards");

		private final int input;
		private final int product;
		private final String name;

		Grinding(int input, int product, String name) {
			this.input = input;
			this.product = product;
			this.name = name;
		}

		public int getInput() {
			return input;
		}

		public int getProduct() {
			return product;
		}

		public String getName() {
			return name;
		}
	}

	/**
	 * Herb on a base -> unfinished potion. No experience, as in OSRS: the experience arrives with
	 * the finished potion, and this step is the reason a player can carry fourteen unfinished
	 * potions around.
	 *
	 * <p>The level is the finished potion's, which is how OSRS gates the unfinished step too. Two
	 * rows use coconut milk ({@link #COCONUT_MILK}) rather than a vial, which is the whole of the
	 * weapon poison family's difference.
	 *
	 * <p>Spirit weed and wergali are the deliberate omissions, on the same grounds {@code SoftClay}
	 * left out the bowl of water: both have a clean herb and an unfinished potion in this revision's
	 * item table, but nothing here finishes them, so the pair would lead to a potion with no recipe.
	 * Wergali's unfinished id also sits next to {@code Clean_wergali} in the table, which is the sort
	 * of neighbour worth not guessing at.
	 */
	public enum Unfinished {

		GUAM(VIAL_OF_WATER, 249, 91, 3),
		MARRENTILL(VIAL_OF_WATER, 251, 93, 5),
		TARROMIN(VIAL_OF_WATER, 253, 95, 11),
		HARRALANDER(VIAL_OF_WATER, 255, 97, 20),
		RANARR(VIAL_OF_WATER, 257, 99, 25),
		TOADFLAX(VIAL_OF_WATER, 2998, 3002, 30),
		IRIT(VIAL_OF_WATER, 259, 101, 40),
		AVANTOE(VIAL_OF_WATER, 261, 103, 48),
		KWUARM(VIAL_OF_WATER, 263, 105, 54),
		SNAPDRAGON(VIAL_OF_WATER, 3000, 3004, 59),
		CADANTINE(VIAL_OF_WATER, 265, 107, 65),
		LANTADYME(VIAL_OF_WATER, 2481, 2483, 67),
		DWARF_WEED(VIAL_OF_WATER, 267, 109, 70),
		TORSTOL(VIAL_OF_WATER, 269, 111, 75),
		WEAPON_POISON_PLUS(COCONUT_MILK, 6016, 5936, 73),
		WEAPON_POISON_PLUS_PLUS(COCONUT_MILK, 2398, 5939, 82);

		private final int base;
		private final int herb;
		private final int potion;
		private final int levelReq;

		Unfinished(int base, int herb, int potion, int levelReq) {
			this.base = base;
			this.herb = herb;
			this.potion = potion;
			this.levelReq = levelReq;
		}

		public int getBase() {
			return base;
		}

		public int getHerb() {
			return herb;
		}

		public int getPotion() {
			return potion;
		}

		public int getLevelReq() {
			return levelReq;
		}
	}

	/**
	 * Unfinished potion + secondary -> the potion you drink.
	 *
	 * <p>Twenty-seven rows: the twenty-five the guide prints on its Potions tab, plus combat and
	 * hunter, which it does not. The guide's promises are what the last five rows close — energy,
	 * agility, super energy, antidote+ and antidote++ were advertised on that tab with nothing in
	 * this class able to make any of them.
	 *
	 * <p>Levels and experience are the OSRS values, and every one of them is also printed by the
	 * guide for the potions the guide lists. The experience is the real per-potion figure, which is
	 * why the antifire row reads 158 where the guide's level beside it is off by one tier — see the
	 * class comment.
	 */
	public enum Finished {

		ATTACK(91, 221, 121, 3, 25),
		ANTIPOISON(93, 235, 175, 5, 38),
		STRENGTH(95, 225, 115, 12, 50),
		RESTORE(97, 223, 127, 22, 63),
		ENERGY(97, 1975, 3010, 26, 68),
		DEFENCE(99, 239, 133, 30, 75),
		AGILITY(3002, 2152, 3034, 34, 80),
		COMBAT(97, 9736, 9741, 36, 84),
		PRAYER(99, 231, 139, 38, 88),
		SUPER_ATTACK(101, 221, 145, 45, 100),
		SUPER_ANTIPOISON(101, 235, 181, 48, 106),
		FISHING(103, 231, 151, 50, 112),
		SUPER_ENERGY(97, 2970, 3018, 52, 118),
		HUNTER(103, 10111, 10000, 53, 120),
		SUPER_STRENGTH(105, 225, 157, 55, 125),
		WEAPON_POISON(105, 241, 187, 60, 137),
		SUPER_RESTORE(3004, 223, 3026, 63, 142),
		SUPER_DEFENCE(107, 239, 163, 66, 150),
		ANTIDOTE_PLUS(3002, 6049, 5945, 68, 155),
		ANTIFIRE(2483, 241, 2454, 69, 158),
		RANGING(109, 245, 169, 72, 163),
		WEAPON_POISON_PLUS(5936, 223, 5937, 73, 165),
		MAGIC(2483, 3138, 3042, 76, 173),
		ZAMORAK_BREW(111, 247, 189, 78, 175),
		ANTIDOTE_PLUS_PLUS(101, 6051, 5954, 79, 178),
		SARADOMIN_BREW(3002, 6693, 6687, 81, 180),
		WEAPON_POISON_PLUS_PLUS(5939, 6018, 5940, 82, 190);

		private final int unfinished;
		private final int secondary;
		private final int potion;
		private final int levelReq;
		private final int xp;

		Finished(int unfinished, int secondary, int potion, int levelReq, int xp) {
			this.unfinished = unfinished;
			this.secondary = secondary;
			this.potion = potion;
			this.levelReq = levelReq;
			this.xp = xp;
		}

		public int getUnfinished() {
			return unfinished;
		}

		public int getSecondary() {
			return secondary;
		}

		public int getPotion() {
			return potion;
		}

		public int getLevelReq() {
			return levelReq;
		}

		public int getXp() {
			return xp;
		}
	}

	/** Looks a grimy herb up by the id the player clicked. */
	public static Cleaning forGrimyHerb(int itemId) {
		for (Cleaning herb : Cleaning.values()) {
			if (herb.grimy == itemId) {
				return herb;
			}
		}
		return null;
	}

	/** Looks a clean herb up by the id, which is how the unfinished rows are keyed. */
	public static Cleaning forCleanHerb(int itemId) {
		for (Cleaning herb : Cleaning.values()) {
			if (herb.clean == itemId) {
				return herb;
			}
		}
		return null;
	}

	/** Looks a grindable up by either side of the pair, pestle excluded. */
	public static Grinding forGrindable(int itemId) {
		for (Grinding grindable : Grinding.values()) {
			if (grindable.input == itemId) {
				return grindable;
			}
		}
		return null;
	}

	/**
	 * Finds the unfinished recipe in a pair, or {@code null}.
	 *
	 * <p>Keyed on the pair rather than on the herb alone, so a clean herb used on the wrong base is
	 * nothing rather than a potion. Matching the herb alone would also make (herb, herb) a recipe.
	 */
	public static Unfinished forUnfinished(int item1, int item2) {
		for (Unfinished row : Unfinished.values()) {
			if ((row.base == item1 && row.herb == item2) || (row.base == item2 && row.herb == item1)) {
				return row;
			}
		}
		return null;
	}

	/** Finds the finishing recipe in a pair, or {@code null}. Keyed on the pair, as above. */
	public static Finished forFinished(int item1, int item2) {
		for (Finished row : Finished.values()) {
			if ((row.unfinished == item1 && row.secondary == item2)
					|| (row.unfinished == item2 && row.secondary == item1)) {
				return row;
			}
		}
		return null;
	}

	/** True when the id is a grimy herb, which is all a click on a herb can be. */
	public static boolean isHerb(int item) {
		return forGrimyHerb(item) != null;
	}

	/**
	 * Cleans one herb: click a grimy herb, get the clean one.
	 *
	 * <p><b>It cleans the herb that was clicked.</b> The old version looped the whole table and acted
	 * on every row it found in the pack, so a player carrying a guam and a torstol and clicking the
	 * guam cleaned both — and deleted the second one from the *first* herb's slot, because the loop
	 * reused the slot it was handed.
	 *
	 * @param slot the slot the grimy herb occupies, so the right item is removed when the player has
	 *             several kinds of herb at once
	 */
	public static void cleanHerb(Client c, int itemId, int slot) {
		Cleaning herb = forGrimyHerb(itemId);
		if (herb == null) {
			return;
		}
		if (c.skills.playerLevel[Player.playerHerblore] < herb.levelReq) {
			c.sendMessage("You need an Herblore level of at least " + herb.levelReq
					+ " to clean this herb.");
			return;
		}
		if (!c.getItems().playerHasItem(herb.grimy, 1)) {
			return;
		}
		c.getItems().deleteItem(herb.grimy, slot, 1);
		c.getItems().addItem(herb.clean, 1);
		c.getPA().addSkillXP(herb.xp * Config.HERBLORE_EXPERIENCE, Player.playerHerblore);
		c.sendMessage("You clean the dirt from the "
				+ ItemAssistant.getItemName(herb.clean).toLowerCase() + ".");
	}

	/**
	 * Pestle and mortar on a grindable.
	 *
	 * <p>Registered in {@code ItemUseRegistry} for every row, so it is reached through
	 * {@link server.game.players.actions.items.ItemUseRegistry#dispatch} and no longer through the
	 * make-X menu the old grinding path opened.
	 */
	public static void grind(Client c, int itemUsed, int useWith) {
		Grinding grindable = forGrindable(itemUsed);
		if (grindable == null) {
			grindable = forGrindable(useWith);
		}
		if (grindable == null) {
			return;
		}
		if (itemUsed != PESTLE_AND_MORTAR && useWith != PESTLE_AND_MORTAR) {
			return;
		}
		start(c, grindable.input, NO_SECOND_MATERIAL, grindable.product, 0, 0, GRIND_ANIMATION,
				"You grind down the " + grindable.name + ".");
	}

	/**
	 * Herb on a base, or a secondary on an unfinished potion.
	 *
	 * <p>One entry point for both because the two families are the same action with different
	 * numbers, and because the registry hands over whichever id the client sent first. Unfinished is
	 * tried first only so that the two tables cannot be confused for one another; their pairs are
	 * disjoint, which a test pins.
	 */
	public static void mix(Client c, int item1, int item2) {
		Unfinished unfinished = forUnfinished(item1, item2);
		if (unfinished != null) {
			start(c, unfinished.base, unfinished.herb, unfinished.potion, 0, unfinished.levelReq,
					MIX_ANIMATION, null);
			return;
		}
		Finished finished = forFinished(item1, item2);
		if (finished != null) {
			start(c, finished.unfinished, finished.secondary, finished.potion, finished.xp,
					finished.levelReq, MIX_ANIMATION,
					"You make a " + ItemAssistant.getItemName(finished.potion).toLowerCase() + ".");
		}
	}

	/**
	 * One ticked herblore action: consume the materials, deliver the product, award the experience.
	 *
	 * <p><b>Per player, with no shared state.</b> The recipe travels with the event closure rather
	 * than through static fields, which is what the old path could not do and why it cross-wired two
	 * players mixing at the same time.
	 *
	 * <p>The materials are re-checked on the tick rather than trusted from the click, two cycles
	 * later, exactly as the fletching actions do: the player can have dropped or banked one in
	 * between.
	 *
	 * @param mat2        {@link #NO_SECOND_MATERIAL} when the recipe consumes only one item, which is
	 *                    grinding
	 * @param message     sent on completion, or {@code null} to stay silent as unfinished potions do
	 */
	private static void start(final Client c, final int mat1, final int mat2, final int product,
			final int xp, int levelReq, int animation, final String message) {
		if (c.playerSkilling[Player.playerHerblore]) {
			return;
		}
		if (c.skills.playerLevel[Player.playerHerblore] < levelReq) {
			c.sendMessage("You need an Herblore level of at least " + levelReq
					+ " to mix this potion.");
			return;
		}
		if (!has(c, mat1) || (mat2 != NO_SECOND_MATERIAL && !has(c, mat2))) {
			return;
		}

		c.playerSkilling[Player.playerHerblore] = true;
		c.startAnimation(animation);

		CycleEventHandler.addEvent(HERBLORE_EVENT, c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				if (!c.playerSkilling[Player.playerHerblore] || !has(c, mat1)
						|| (mat2 != NO_SECOND_MATERIAL && !has(c, mat2))) {
					container.stop();
					return;
				}
				c.getItems().deleteItem2(mat1, 1);
				if (mat2 != NO_SECOND_MATERIAL) {
					c.getItems().deleteItem2(mat2, 1);
				}
				c.getItems().addItem(product, 1);
				if (xp > 0) {
					c.getPA().addSkillXP(xp * Config.HERBLORE_EXPERIENCE, Player.playerHerblore);
				}
				if (message != null) {
					c.sendMessage(message);
				}
				container.stop();
			}

			@Override
			public void stop() {
				cancel(c);
			}
		}, ACTION_CYCLES);
	}

	/** True when the player is holding at least one of {@code itemId}. */
	private static boolean has(Client c, int itemId) {
		return c.getItems().playerHasItem(itemId, 1);
	}

	/**
	 * Stops a running herblore action, if one is running.
	 *
	 * <p>Keyed on its own event id rather than on the player: {@code stopEvents(c)} would stop every
	 * event the player owns, and they own other skills' events too. Called from
	 * {@code PlayerAssistant.resetVariables}, which every walk reaches, so walking away ends the
	 * action the way it ends fletching and cooking.
	 */
	public static void cancel(Client c) {
		if (c.playerSkilling[Player.playerHerblore]) {
			c.playerSkilling[Player.playerHerblore] = false;
			CycleEventHandler.stopEvents(c, HERBLORE_EVENT);
			c.startAnimation(65535);
		}
	}
}
