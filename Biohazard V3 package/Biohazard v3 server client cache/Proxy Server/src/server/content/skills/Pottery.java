package server.content.skills;

import server.Config;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.items.ItemAssistant;
import server.game.players.Client;
import server.game.players.Player;

/**
 * Pottery: soft clay into unfired clay on a potter's wheel, then unfired clay into the finished
 * item in a pottery oven.
 *
 * <p><b>Neither half existed.</b> The Crafting guide's Pottery tab has always listed the five
 * fired items, and every one of them resolves in {@code item.cfg}, but nothing in the server read
 * or wrote the unfired ids — the five rows were a menu with no action behind it. The two objects
 * were already named in {@code Data/objectSize.cfg} ("Potter's Wheel", "Pottery Oven"), so the
 * world has had the fixtures the whole time.
 *
 * <p><b>Two stages, because that is how clay works.</b> The wheel turns soft clay into an unfired
 * item and the oven turns that into something usable. The oven stage is deliberately its own
 * action rather than a step hidden inside the wheel one: an unfired pot is a real item with a real
 * examine ("I need to put this in a pottery oven."), it survives being banked, and a player can
 * shape a hundred of them and then fire them in one trip.
 *
 * <p><b>Firing has no level requirement, and that is not an oversight.</b> Only shaping does; the
 * oven "hardens them" and grants experience for any earthenware the player already owns. The
 * levels live on {@link Shape} and are checked in {@link #select}, i.e. at the wheel. If this file
 * is ever "tidied" by moving the check to the oven, a level-1 player who was given an unfired
 * plant pot would stop being able to finish it, which is the opposite of the guide's promise.
 *
 * <p><b>Numbers.</b> Levels and both xp columns are the ones the Crafting guide already prints for
 * this tab (1, 7, 8, 19, 25) and the OSRS pottery tables, which agree on every row: shaping is
 * 6.3/15/18/20/20 and firing is 6.3/10/15/17.5/20. Redone's {@code Pottery} carries the same
 * numbers for the three rows it implements and agrees with both. Redone's copy of the firing half
 * is unreachable — it is written, but nothing ever calls {@code showFire}, so no button can set
 * the flag it needs — which is why the second stage here is built from the OSRS table rather than
 * ported.
 *
 * <p><b>Not modelled: breaking.</b> The oven has a small chance to crack a piece, falling to zero
 * by level 14. 2006Redone does not model it and no source gives the formula, so this pays a fixed
 * 100% success rather than inventing one. Documented in {@code QOL_PLAN.md} rather than guessed at.
 */
public final class Pottery {

	/**
	 * The potter's wheel objects, named as such in {@code Data/objectSize.cfg}. 2642 is the common
	 * one and 4310 the second; both are read by the click family and the item family below so the
	 * two can never disagree about which objects are wheels.
	 */
	public static final int[] WHEEL_OBJECTS = { 2642, 4310 };

	/**
	 * The pottery oven objects, likewise from {@code Data/objectSize.cfg} — 2643, its duplicate
	 * 4308, and 11601. Read only by the item-on-object family: see {@link #fire} for why a plain
	 * click on an oven is not claimed.
	 */
	public static final int[] OVEN_OBJECTS = { 2643, 4308, 11601 };

	public static final int SOFT_CLAY = 1761;

	static final int WHEEL_ANIMATION = 896;
	static final int OVEN_ANIMATION = 899;

	/** The oven's own sound, as 2006Redone plays it. */
	static final int OVEN_SOUND = 469;

	/**
	 * Their own event ids. {@code playerIsCrafting} is shared with spinning, gem cutting and
	 * leather, so an action can only stop itself by id — {@code stopEvents(c)} would put a fire out
	 * or end a smelt. 4617 and 4618 are clear of the ids this codebase already uses (1, 3, 4, 5,
	 * 100, and fletching's and spinning's constants).
	 */
	private static final int WHEEL_EVENT = 4617;
	private static final int OVEN_EVENT = 4618;

	/**
	 * Cycles between items, matching spinning and fletching. Deliberately not Redone's 3 and 5:
	 * that is 1.8&nbsp;s and 3&nbsp;s on the same 600&nbsp;ms tick, and every other moving crafting
	 * action here runs at two cycles.
	 */
	private static final int CYCLE_TICK = 2;

	/**
	 * The chatbox the wheel opens. 8938 is the five-option interface already used for snakeskin
	 * leather; items are the five picture frames and labels the five text frames, four apart.
	 */
	private static final int SHAPE_INTERFACE = 8938;
	private static final int SHAPE_ITEM_FRAME = 8941;
	private static final int SHAPE_NAME_FRAME = 8949;
	private static final int SHAPE_NAME_STRIDE = 4;
	private static final int SHAPE_ITEM_ZOOM = 180;

	/**
	 * One row of the pottery tables: what soft clay becomes on the wheel, what that becomes in the
	 * oven, the level the wheel asks for, and both xp values.
	 *
	 * <p>{@code buttons} is the row's four "make 1/5/10/28" ids on interface 8938, in that order.
	 * They are the same ids the snakeskin rows of {@code CraftingData.leatherData} use, because it
	 * is the same interface — row <em>n</em> of the chatbox sends the same button whatever the
	 * interface was opened for. That is exactly why exactly one of {@code potteryDialogue} and
	 * {@code craftDialogue} may be set: see {@link #openWheel}.
	 *
	 * <p>Rows 4 and 5 are the two ids Redone never wired up (its pottery button map stops at three
	 * items). They are taken from this server's own snakeskin rows for boots and vambraces, which
	 * occupy the same two chatbox rows and are known to work.
	 */
	public enum Shape {

		POT(1787, 1931, 1, 6.3, 6.3, "Pot",
				new int[][] { { 34245, 1 }, { 34244, 5 }, { 34243, 10 }, { 34242, 28 } }),
		PIE_DISH(1789, 2313, 7, 15, 10, "Pie Dish",
				new int[][] { { 34249, 1 }, { 34248, 5 }, { 34247, 10 }, { 34246, 28 } }),
		BOWL(1791, 1923, 8, 18, 15, "Bowl",
				new int[][] { { 34253, 1 }, { 34252, 5 }, { 34251, 10 }, { 34250, 28 } }),
		PLANT_POT(5352, 5350, 19, 20, 17.5, "Plant Pot",
				new int[][] { { 35001, 1 }, { 35000, 5 }, { 34255, 10 }, { 34254, 28 } }),
		POT_LID(4438, 4440, 25, 20, 20, "Pot Lid",
				new int[][] { { 35005, 1 }, { 35004, 5 }, { 35003, 10 }, { 35002, 28 } });

		private final int unfired;
		private final int fired;
		private final int levelReq;
		private final double shapeXp;
		private final double fireXp;
		private final String label;
		private final int[][] buttons;

		Shape(int unfired, int fired, int levelReq, double shapeXp, double fireXp, String label,
				int[][] buttons) {
			this.unfired = unfired;
			this.fired = fired;
			this.levelReq = levelReq;
			this.shapeXp = shapeXp;
			this.fireXp = fireXp;
			this.label = label;
			this.buttons = buttons;
		}

		public int getUnfired() {
			return unfired;
		}

		public int getFired() {
			return fired;
		}

		public int getLevelReq() {
			return levelReq;
		}

		public double getShapeXp() {
			return shapeXp;
		}

		public double getFireXp() {
			return fireXp;
		}

		/** The label this row shows in the wheel's chatbox, matching the guide's own wording. */
		public String getLabel() {
			return label;
		}

		/**
		 * The number this button asks for, or {@code 0} when the button is not this row's.
		 *
		 * <p>Zero rather than {@code -1} so the caller can use it directly as the make count: a
		 * non-match must never be usable as an amount.
		 */
		public int getAmount(int button) {
			for (int[] pair : buttons) {
				if (pair[0] == button) {
					return pair[1];
				}
			}
			return 0;
		}

		public int[][] getButtons() {
			return buttons.clone();
		}
	}

	/** The row an unfired id belongs to, or null — the oven asks this about whatever was used on it. */
	public static Shape forUnfired(int itemId) {
		for (Shape s : Shape.values()) {
			if (s.getUnfired() == itemId) {
				return s;
			}
		}
		return null;
	}

	/** The row a chatbox button belongs to, or null. */
	public static Shape forButton(int button) {
		for (Shape s : Shape.values()) {
			if (s.getAmount(button) > 0) {
				return s;
			}
		}
		return null;
	}

	/**
	 * Opens the wheel's "what would you like to make?" chatbox.
	 *
	 * <p>Requires soft clay up front. Redone opens the interface either way and then answers every
	 * option with "You need soft clay to do this"; refusing before the menu is one message instead
	 * of one message per click, and the menu cannot be used for anything without clay anyway.
	 *
	 * <p>Also the one place the two chatbox flags are separated. 8938 hosts both this and the
	 * snakeskin leather menu, and both read the same four buttons per row, so a player who opened
	 * one and then the other would otherwise have one click handled twice.
	 */
	public static void openWheel(Client c) {
		if (!c.getItems().playerHasItem(SOFT_CLAY, 1)) {
			c.sendMessage("You need some soft clay to use the potter's wheel.");
			return;
		}
		c.getPA().sendFrame164(SHAPE_INTERFACE);
		Shape[] shapes = Shape.values();
		for (int i = 0; i < shapes.length; i++) {
			c.getPA().itemOnInterface(SHAPE_ITEM_FRAME + i, SHAPE_ITEM_ZOOM, shapes[i].getUnfired());
			c.getPA().sendFrame126(shapes[i].getLabel(), SHAPE_NAME_FRAME + (i * SHAPE_NAME_STRIDE));
		}
		c.craftDialogue = false;
		c.potteryDialogue = true;
	}

	/**
	 * A button on the wheel's chatbox. Called from {@code ClickingButtons} while
	 * {@code potteryDialogue} is set.
	 */
	public static void select(Client c, int buttonId) {
		Shape shape = forButton(buttonId);
		if (shape == null) {
			return;
		}
		if (c.playerIsCrafting) {
			return;
		}
		if (c.skills.playerLevel[Player.playerCrafting] < shape.getLevelReq()) {
			c.sendMessage("You need a crafting level of " + shape.getLevelReq() + " to make this.");
			closeDialogue(c);
			return;
		}
		if (!c.getItems().playerHasItem(SOFT_CLAY, 1)) {
			c.sendMessage("You need some soft clay to use the potter's wheel.");
			closeDialogue(c);
			return;
		}
		shape(c, shape, shape.getAmount(buttonId));
	}

	/**
	 * Shapes up to {@code amount} items, one per two-tick cycle, stopping early when the clay runs
	 * out or the player walks.
	 */
	public static void shape(Client c, Shape shape, int amount) {
		// Also here, not only in select(). shape() is the entry point that actually sets the flag
		// and queues the loop, so it has to be safe on its own; select() checks first only so that
		// a refused click cannot print a level or clay message at a player who is already busy.
		if (c.playerIsCrafting) {
			return;
		}
		closeDialogue(c);
		c.playerIsCrafting = true;
		c.startAnimation(WHEEL_ANIMATION);
		final int[] remaining = { amount };
		CycleEventHandler.addEvent(WHEEL_EVENT, c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				if (!c.playerIsCrafting || remaining[0] <= 0
						|| !c.getItems().playerHasItem(SOFT_CLAY, 1)) {
					container.stop();
					return;
				}
				c.getItems().deleteItem2(SOFT_CLAY, 1);
				c.getItems().addItem(shape.getUnfired(), 1);
				c.getPA().addSkillXP((int) (shape.getShapeXp() * Config.CRAFTING_EXPERIENCE),
						Player.playerCrafting);
				c.sendMessage("You make the soft clay into " + withArticle(shape.getUnfired()) + ".");
				c.startAnimation(WHEEL_ANIMATION);
				remaining[0]--;
			}

			@Override
			public void stop() {
				cancel(c);
			}
		}, CYCLE_TICK);
	}

	/**
	 * Fires whatever unfired item was used on a pottery oven, repeating until that stack is gone.
	 *
	 * <p><b>Only usable by using an item on an oven.</b> A plain click on one is not claimed,
	 * because the oven has no way to know which of the five you meant — and 2643 is also the object
	 * this server's {@code JewelryMaking.mouldInterface} is hung on, so taking its first click
	 * would break jewellery. The item-on-object registry is pair-keyed, so claiming
	 * (unfired id, oven id) leaves gold bar on 2643 exactly where it was.
	 *
	 * <p>No level check, per the class comment. The gate is at the wheel.
	 */
	public static void fire(Client c, int unfiredId) {
		Shape shape = forUnfired(unfiredId);
		if (shape == null || c.playerIsCrafting) {
			return;
		}
		if (!c.getItems().playerHasItem(unfiredId, 1)) {
			return;
		}
		c.playerIsCrafting = true;
		CycleEventHandler.addEvent(OVEN_EVENT, c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				if (!c.playerIsCrafting || !c.getItems().playerHasItem(unfiredId, 1)) {
					container.stop();
					return;
				}
				c.getItems().deleteItem2(unfiredId, 1);
				c.getItems().addItem(shape.getFired(), 1);
				c.getPA().addSkillXP((int) (shape.getFireXp() * Config.CRAFTING_EXPERIENCE),
						Player.playerCrafting);
				c.startAnimation(OVEN_ANIMATION);
				c.getPA().sendSound(OVEN_SOUND, 100, 0);
				c.sendMessage("You put " + withArticle(unfiredId) + " into the oven.");
				c.sendMessage("You retrieve " + withArticle(shape.getFired()) + " from the oven.");
			}

			@Override
			public void stop() {
				cancel(c);
			}
		}, CYCLE_TICK);
	}

	/**
	 * Stops whichever pottery action is running, if any.
	 *
	 * <p>The event stops are unconditional and the flag is not. {@code playerIsCrafting} now means
	 * "some crafting action is running", not "this one is", so it cannot tell us whether a pottery
	 * event is queued — spinning shares it and either skill's cancel may run first. Stopping by id
	 * is harmless when nothing matches, and only the flag and the animation need the guard, since
	 * resetting 65535 on every walk would clear an unrelated emote.
	 */
	public static void cancel(Client c) {
		if (c.playerIsCrafting) {
			c.playerIsCrafting = false;
			c.startAnimation(65535);
		}
		CycleEventHandler.stopEvents(c, WHEEL_EVENT);
		CycleEventHandler.stopEvents(c, OVEN_EVENT);
	}

	/** Closes the wheel's chatbox and drops the flag that makes its buttons live. */
	private static void closeDialogue(Client c) {
		c.getPA().removeAllWindows();
		c.potteryDialogue = false;
	}

	private static String withArticle(int itemId) {
		String name = ItemAssistant.getItemName(itemId).toLowerCase();
		char first = name.charAt(0);
		boolean vowel = first == 'a' || first == 'e' || first == 'i' || first == 'o' || first == 'u';
		return (vowel ? "an " : "a ") + name;
	}
}
