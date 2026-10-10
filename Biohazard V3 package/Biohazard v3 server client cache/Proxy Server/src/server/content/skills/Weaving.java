package server.content.skills;

import server.Config;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.items.ItemAssistant;
import server.game.players.Client;
import server.game.players.Player;

/**
 * Weaving at a loom: balls of wool into cloth, jute fibres into empty sacks.
 *
 * <p><b>This did not exist, in either reference server either.</b> The Crafting guide has printed
 * a Weaving tab — "10 Cloth" and "21 Vegetable Sack" — since before this work, and this server's
 * own item examine texts already pointed at the loom ("I can weave this to make sacks." on jute
 * fibre), but nothing read item 5931 anywhere and nothing handled an object named "Loom", though
 * {@code Data/objectSize.cfg} names two of them. 2006Redone advertises the same tab with the same
 * absence of implementation, so there was nothing to port; the numbers come from the OSRS loom
 * table.
 *
 * <p><b>Two recipes, four materials each.</b> Cloth is four balls of wool at level 10 for 12 xp;
 * an empty sack is four jute fibres at level 21 for 38 xp. Neither is one-to-one, which is why
 * {@link Weave} carries an amount and the loop consumes the whole batch per action rather than one
 * item at a time.
 *
 * <p><b>No "how many would you like to make?" step, as with spinning.</b> The two guide lines are
 * the two recipes, and they take different materials, so a click or an item already says which one
 * was meant. The action repeats until the material runs out or the player walks.
 *
 * <p><b>The animation is borrowed.</b> Neither source implements a loom, so neither has a loom
 * animation to copy; this uses the spinning wheel's 896, which is the closest action in the same
 * skill and the one a player would expect to see. Inventing an id would be worse than borrowing a
 * documented one, and there is nothing to invent it from.
 */
public final class Weaving {

	/**
	 * The loom objects, named as such in {@code Data/objectSize.cfg}. Read by both the click
	 * family and the item family so the two cannot disagree about which objects are looms.
	 */
	public static final int[] LOOM_OBJECTS = { 787, 8717 };

	static final int WEAVE_ANIMATION = 896;

	/** Its own event id, so stopping a weave cannot stop a spin or a pot running on the same flag. */
	private static final int WEAVE_EVENT = 4619;

	/** Two cycles, matching spinning, fletching and pottery. */
	private static final int CYCLE_TICK = 2;

	/** One row of the loom table: what goes in, how much of it, what comes out, level and xp. */
	public enum Weave {

		CLOTH(1759, 3224, 10, 12, 4),
		EMPTY_SACK(5931, 5418, 21, 38, 4);

		private final int material;
		private final int amount;
		private final int product;
		private final int levelReq;
		private final int xp;

		Weave(int material, int product, int levelReq, int xp, int amount) {
			this.material = material;
			this.product = product;
			this.levelReq = levelReq;
			this.xp = xp;
			this.amount = amount;
		}

		public int getMaterial() {
			return material;
		}

		public int getAmount() {
			return amount;
		}

		public int getProduct() {
			return product;
		}

		public int getLevelReq() {
			return levelReq;
		}

		public int getXp() {
			return xp;
		}
	}

	/** The row a material belongs to, or null — the loom asks this about whatever is used on it. */
	public static Weave forMaterial(int itemId) {
		for (Weave w : Weave.values()) {
			if (w.getMaterial() == itemId) {
				return w;
			}
		}
		return null;
	}

	/**
	 * What a click on the loom does, as opposed to using an item on it.
	 *
	 * <p>One weaveable material held is no choice at all, so it weaves it. Both held is a choice,
	 * and the click does not make it — a stack of jute quietly becoming cloth is the wrong answer
	 * half the time. The player picks with the item instead.
	 */
	public static void clickLoom(Client c) {
		Weave available = null;
		for (Weave w : Weave.values()) {
			if (!c.getItems().playerHasItem(w.getMaterial(), w.getAmount())) {
				continue;
			}
			if (available != null) {
				c.sendMessage("You are carrying more than one thing you could weave here."
						+ " Use the one you want.");
				return;
			}
			available = w;
		}
		if (available == null) {
			// Kept in step with the table by a test: both amounts are four.
			c.sendMessage("You need four balls of wool or four jute fibres to weave.");
			return;
		}
		weave(c, available);
	}

	/**
	 * Starts weaving {@code weave}, repeating until it runs out.
	 *
	 * <p>{@code playerIsCrafting} is the guard, as it is for spinning and pottery, so the three
	 * cannot overlap and {@code resetVariables} ends all of them on a walk.
	 */
	public static void weave(Client c, Weave weave) {
		if (c.playerIsCrafting) {
			return;
		}
		if (c.skills.playerLevel[Player.playerCrafting] < weave.getLevelReq()) {
			c.sendMessage("You need a crafting level of " + weave.getLevelReq() + " to weave this.");
			return;
		}
		if (!c.getItems().playerHasItem(weave.getMaterial(), weave.getAmount())) {
			c.sendMessage("You do not have enough "
					+ ItemAssistant.getItemName(weave.getMaterial()).toLowerCase() + " to weave that.");
			return;
		}

		c.playerIsCrafting = true;
		c.startAnimation(WEAVE_ANIMATION);
		CycleEventHandler.addEvent(WEAVE_EVENT, c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				if (!c.playerIsCrafting
						|| !c.getItems().playerHasItem(weave.getMaterial(), weave.getAmount())) {
					container.stop();
					return;
				}
				c.getItems().deleteItem2(weave.getMaterial(), weave.getAmount());
				c.getItems().addItem(weave.getProduct(), 1);
				c.getPA().addSkillXP(weave.getXp() * Config.CRAFTING_EXPERIENCE,
						Player.playerCrafting);
				c.sendMessage("You weave the "
						+ ItemAssistant.getItemName(weave.getMaterial()).toLowerCase() + " on the loom.");				c.startAnimation(WEAVE_ANIMATION);
			}

			@Override
			public void stop() {
				cancel(c);
			}
		}, CYCLE_TICK);
	}

	/**
	 * Stops a running weave, if one is running.
	 *
	 * <p>The event stop is unconditional and the flag is not: {@code playerIsCrafting} means "some
	 * crafting action is running", not "this one is", and {@code resetCrafting} calls every
	 * crafting cancel in turn, so whichever runs second would otherwise find the flag already
	 * cleared and leave its own loop queued.
	 */
	public static void cancel(Client c) {
		if (c.playerIsCrafting) {
			c.playerIsCrafting = false;
			c.startAnimation(65535);
		}
		CycleEventHandler.stopEvents(c, WEAVE_EVENT);
	}
}
