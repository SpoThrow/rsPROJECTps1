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
 * <p><b>Mixing is a chatbox and a batch. The batch is the older half; the chatbox is the newer
 * one, and the state behind both used to be the bug.</b> The four families were driven by four
 * static fields ({@code itemToDelete}, {@code itemToAdd}, {@code potExp} and friends) that the
 * click filled in and a tick loop read back out, with the amount taken from a make-X menu. The
 * menu itself was right — OSRS does ask "how many would you like to make?" when a herb goes on a
 * vial of water, and it does keep making them while the player stands there — but everything
 * holding it together was wrong:
 *
 * <ul>
 * <li>A static field is shared by every player on the server. Two people mixing at once — one an
 * attack potion, one a super strength — wrote to the same four fields, so the tick loop could
 * deliver one player's product to the other, or consume the wrong materials. The recipe now
 * travels with the click (on the player) and then with the event (in its closure).
 * <li>The amount lived in those same fields, so "make 5" was a server-wide instruction.
 * </ul>
 *
 * <p>So the tables below are read directly by the action instead of being flattened into shared
 * state, which is also what lets the pairs be registered in
 * {@link server.game.players.actions.items.ItemUseRegistry} — see {@code HerbloreItemUses}. Every
 * recipe is a pair of item ids, so the registry owns the whole family rather than only part of it.
 *
 * <p><b>How the mix works, in three steps.</b> Combining the pair opens interface 4429 with the
 * potion's own model and name and stores the pair on the player ({@link #mix}); the four buttons
 * on that chatbox pick 1, 5, 10 or all ({@link #select}); and the tick loop then produces one
 * potion every two cycles until the amount asked for is made, the materials run out, or the player
 * walks ({@link #start}). Nothing is consumed until the first potion is actually made, so the
 * chatbox is free to be ignored.
 *
 * <p><b>Cleaning is not ticked, and that is a decision.</b> A grimy herb is one item in the pack
 * with no second item and no world object, so there is nothing for a tick to pace against and
 * nothing a walk can interrupt — the same reasoning as {@code SoftClay}. OSRS has no cleaning
 * animation, so there is none here; the message and the experience are the whole of the feedback.
 * Mixing and grinding are ticked at two cycles, the fletching cadence, because they are the actions
 * a player repeats. Grinding keeps its one-per-click: the pestle is a tool rather than an
 * ingredient, and a grind is not a potion.
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

	/**
	 * The four "make" buttons on the mixing chatbox, with the number each one asks for.
	 *
	 * <p>They are the ids interface 4429's own buttons send, and they are the ones this server's
	 * pre-rewrite herblore menu read, so they are known to work in this client rather than inferred.
	 * Note they are <em>not</em> the same ids as the leather menu's: interface 1743 rows send
	 * {@code 10238} for ten and {@code 6212} for twenty-eight, where 4429 sends five and ten. A row
	 * is only interchangeable with another row of its own interface.
	 */
	private static final int[][] AMOUNT_BUTTONS = {
			{ 10239, 1 },
			{ 10238, 5 },
			{ 6212, 10 },
			{ 6211, 28 },
	};

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
	 *
	 * <p>One grind per click, no chatbox: the pestle is the tool and the item is the ingredient, so
	 * there is no second material for an amount to be an amount <em>of</em>.
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
				"You grind down the " + grindable.name + ".", 1);
	}

	/**
	 * Herb on a base, or a secondary on an unfinished potion: opens the "how many would you like to
	 * make?" chatbox for the recipe the pair belongs to.
	 *
	 * <p>One entry point for both because the two families are the same action with different
	 * numbers, and because the registry hands over whichever id the client sent first. Unfinished is
	 * tried first only so that the two tables cannot be confused for one another; their pairs are
	 * disjoint, which a test pins.
	 *
	 * <p><b>Nothing is consumed here.</b> The pair is remembered on the player and the interface is
	 * opened; the materials only leave the pack once a button has been clicked and the first potion
	 * is actually made, which is what makes the chatbox safe to ignore or walk away from.
	 */
	public static void mix(Client c, int item1, int item2) {
		Unfinished unfinished = forUnfinished(item1, item2);
		if (unfinished != null) {
			openChatbox(c, item1, item2, unfinished.potion, unfinished.levelReq);
			return;
		}
		Finished finished = forFinished(item1, item2);
		if (finished != null) {
			openChatbox(c, item1, item2, finished.potion, finished.levelReq);
		}
	}

	/**
	 * Opens the mixing chatbox for a pair: the recipe's model and name into interface 4429, and the
	 * pair into the player.
	 *
	 * <p>The level is checked here rather than at the button, because the client's menu would
	 * otherwise offer to make something the player cannot make. The materials are checked for the
	 * same reason: a menu that opens with nothing to mix can only be answered with a message.
	 *
	 * <p>No {@code sendFrame126} for the "how many would you like to make?" line: that text is part
	 * of the interface in the cache, and only the model frame and the name beside it change.
	 */
	static void openChatbox(Client c, int item1, int item2, int product, int levelReq) {
		// A running batch is already using the pair this click would overwrite.
		if (c.playerSkilling[Player.playerHerblore]) {
			return;
		}
		if (c.skills.playerLevel[Player.playerHerblore] < levelReq) {
			c.sendMessage("You need an Herblore level of at least " + levelReq
					+ " to mix this potion.");
			return;
		}
		if (!has(c, item1) || !has(c, item2)) {
			return;
		}
		SkillHandler.send1Item(c, product, SkillHandler.view190);
		c.herbloreItem1 = item1;
		c.herbloreItem2 = item2;
		c.herbloreDialogue = true;
	}

	/**
	 * A "make" button on the mixing chatbox. Called from {@code ClickingButtons} while
	 * {@code herbloreDialogue} is set.
	 *
	 * <p>The pair is re-read from the player and re-resolved into a recipe here rather than being
	 * carried in a field of its own, so the ids stored on the player are the only pending state and
	 * the tables stay the single source of truth for what those ids make. An unknown button, an
	 * unknown pair or an empty player all fall through and do nothing.
	 */
	public static void select(Client c, int buttonId) {
		int amount = amountFor(buttonId);
		if (amount <= 0) {
			return;
		}
		Unfinished unfinished = forUnfinished(c.herbloreItem1, c.herbloreItem2);
		if (unfinished != null) {
			closeChatbox(c);
			c.getPA().removeAllWindows();
			start(c, unfinished.base, unfinished.herb, unfinished.potion, 0,
					unfinished.levelReq, MIX_ANIMATION, null, amount);
			return;
		}
		Finished finished = forFinished(c.herbloreItem1, c.herbloreItem2);
		if (finished != null) {
			closeChatbox(c);
			c.getPA().removeAllWindows();
			start(c, finished.unfinished, finished.secondary, finished.potion, finished.xp,
					finished.levelReq, MIX_ANIMATION,
					"You make a " + ItemAssistant.getItemName(finished.potion).toLowerCase() + ".",
					amount);
		}
	}

	/** The number one of the chatbox's buttons asks for, or {@code 0} when it is not one of them. */
	public static int amountFor(int button) {
		for (int[] pair : AMOUNT_BUTTONS) {
			if (pair[0] == button) {
				return pair[1];
			}
		}
		return 0;
	}

	/**
	 * Forgets the pair the chatbox was open for, so a later click cannot make the old recipe.
	 *
	 * <p>Sends nothing: the caller decides whether the interface also has to be closed. A walk
	 * closes it on its own, a button press has to close it explicitly.
	 */
	public static void closeChatbox(Client c) {
		c.herbloreDialogue = false;
		c.herbloreItem1 = -1;
		c.herbloreItem2 = -1;
	}

	/**
	 * One herblore action, ticked, repeated until {@code amount} is reached.
	 *
	 * <p><b>Per player, with no shared state.</b> The recipe and the amount travel with the event
	 * closure rather than through static fields, which is what the old path could not do and why it
	 * cross-wired two players mixing at the same time.
	 *
	 * <p>The materials are re-checked on every tick rather than trusted from the click, exactly as
	 * the fletching actions do: the player can have dropped or banked one in between, and a batch
	 * runs for long enough that it will happen. A depleted stack ends the action rather than a
	 * half-made potion, and "make all" is simply the amount the client asked for (28), so it ends
	 * the same way.
	 *
	 * @param mat2        {@link #NO_SECOND_MATERIAL} when the recipe consumes only one item, which is
	 *                    grinding
	 * @param message     sent on each completion, or {@code null} to stay silent as unfinished
	 *                    potions do
	 * @param amount      how many to make, or how many the player asked for — whichever runs out
	 *                    first wins
	 */
	private static void start(final Client c, final int mat1, final int mat2, final int product,
			final int xp, int levelReq, final int animation, final String message, int amount) {
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
		final int[] remaining = { amount };

		CycleEventHandler.addEvent(HERBLORE_EVENT, c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				if (!c.playerSkilling[Player.playerHerblore] || remaining[0] <= 0 || !has(c, mat1)
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
				// Re-armed each cycle, as pottery does: one animation lasts about one action, and a
				// batch of twenty-eight would otherwise be silent and still after the first.
				c.startAnimation(animation);
				remaining[0]--;
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
	 * Stops a running herblore action, if one is running, and drops any open chatbox.
	 *
	 * <p>Keyed on its own event id rather than on the player: {@code stopEvents(c)} would stop every
	 * event the player owns, and they own other skills' events too. Called from
	 * {@code PlayerAssistant.resetVariables}, which every walk reaches, so walking away ends the
	 * action the way it ends fletching and cooking.
	 *
	 * <p>The event stop is guarded by the flag because only a running action owns an event here —
	 * unlike the crafting family, herblore's cancel is not the only thing that can be running.
	 * {@code closeChatbox} is unconditional, because the chatbox can be open with no action at all.
	 */
	public static void cancel(Client c) {
		closeChatbox(c);
		if (c.playerSkilling[Player.playerHerblore]) {
			c.playerSkilling[Player.playerHerblore] = false;
			CycleEventHandler.stopEvents(c, HERBLORE_EVENT);
			c.startAnimation(65535);
		}
	}
}
