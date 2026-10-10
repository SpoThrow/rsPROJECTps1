package server.content.skills;

import server.Config;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.items.ItemAssistant;
import server.game.players.Client;
import server.game.players.Player;

/**
 * Spinning at a spinning wheel: wool into a ball of wool, flax into a bow string.
 *
 * <p><b>This did not exist.</b> The Crafting guide has advertised a Spinning tab since before
 * this work — {@code SkillInterfaces.craftingComplex} prints "1 Wool" and "10 Flax into Bow
 * Strings" — and the client tells the player to go to a wheel in the examine text of the flax
 * itself ("I should use this with a spinning wheel."), but there was no spinning wheel handler
 * anywhere and {@code Flax.java} only picked the flax. Both products were therefore only
 * obtainable as impling rewards.
 *
 * <p>That mattered more after bow stringing landed: the only way to get a bow string was an
 * impling. Spinning flax is the intended source and now works.
 *
 * <p><b>No "how many would you like to make?" step, deliberately.</b> The action repeats until
 * the material runs out or the player walks, the same shape as {@code GemCutting} and the ticked
 * fletching actions, so one click is one stack. A make-X chatbox would add a click and a second
 * piece of state for no gain.
 *
 * <p><b>Numbers.</b> Levels come from the guide's own list, which is also the OSRS pair (wool 1,
 * flax 10) — so the promise the guide was already making is now enforced. Flax's 15 xp is
 * Necrotic's and OSRS's. Wool's 3 is Redone's integer for OSRS's 2.5; this skill stores xp as an
 * {@code int} like every other crafting table here, so it cannot carry the half. Animation 896
 * and the per-action message are Redone's and Necrotic's, both of which use 896 for either
 * material.
 */
public class Spinning extends CraftingData {

	/**
	 * The spinning wheel objects. Taken from {@code Data/objectSize.cfg}, which names the objects
	 * in the world rather than being copied from another server: 2644 and 4309 are the common pair
	 * and 8748 is the third.
	 *
	 * <p>Read by both registrations — the click handler and the item-on-object handler — so the
	 * two can never disagree about which objects are wheels.
	 */
	public static final int[] WHEEL_OBJECTS = { 2644, 4309, 8748 };

	public enum Material {

		WOOL(1737, 1759, 1, 3),
		FLAX(1779, 1777, 10, 15);

		private final int material;
		private final int product;
		private final int levelReq;
		private final int xp;

		Material(int material, int product, int levelReq, int xp) {
			this.material = material;
			this.product = product;
			this.levelReq = levelReq;
			this.xp = xp;
		}

		public int getMaterial() {
			return material;
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

	static final int SPIN_ANIMATION = 896;

	/**
	 * Its own event id, so stopping a spin cannot stop the gem or leather action running under
	 * the same {@code playerIsCrafting} flag — and so a cancelled spin can be removed from the
	 * scheduler outright instead of being left to notice the flag on its next tick.
	 */
	private static final int SPIN_EVENT = 4616;

	public static Material forId(int itemId) {
		for (Material m : Material.values()) {
			if (m.getMaterial() == itemId) {
				return m;
			}
		}
		return null;
	}

	/**
	 * What a click on the wheel does, as opposed to using an item on it.
	 *
	 * <p>One material held is no choice at all, so it spins it. Both held is a choice, and the
	 * click does not make it: wool and flax become different items and whichever the server picked
	 * would be the wrong answer half the time — a stack of flax quietly turning into bow strings
	 * is not a mistake worth risking to save a click. The player picks with the item instead,
	 * using either one on the wheel.
	 */
	public static void openWheel(Client c) {
		boolean wool = c.getItems().playerHasItem(Material.WOOL.getMaterial());
		boolean flax = c.getItems().playerHasItem(Material.FLAX.getMaterial());
		if (wool && flax) {
			c.sendMessage("You are carrying both wool and flax. Use the one you want to spin.");
			return;
		}
		if (flax) {
			spin(c, Material.FLAX);
		} else if (wool) {
			spin(c, Material.WOOL);
		} else {
			c.sendMessage("You need some wool or flax to spin.");
		}
	}

	/**
	 * Starts spinning {@code m}, repeating until it runs out.
	 *
	 * <p>{@code playerIsCrafting} is the guard, as it is for gem cutting and leather, so the three
	 * cannot overlap and {@code resetVariables} ends all of them on a walk.
	 */
	public static void spin(Client c, Material m) {
		if (c.playerIsCrafting) {
			return;
		}
		if (c.skills.playerLevel[Player.playerCrafting] < m.getLevelReq()) {
			c.sendMessage("You need a crafting level of " + m.getLevelReq() + " to spin this.");
			return;
		}
		if (!c.getItems().playerHasItem(m.getMaterial(), 1)) {
			c.sendMessage("You have no " + ItemAssistant.getItemName(m.getMaterial()).toLowerCase()
					+ " to spin.");
			return;
		}

		c.playerIsCrafting = true;
		c.startAnimation(SPIN_ANIMATION);
		CycleEventHandler.addEvent(SPIN_EVENT, c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				if (!c.playerIsCrafting || !c.getItems().playerHasItem(m.getMaterial(), 1)) {
					container.stop();
					return;
				}
				c.getItems().deleteItem2(m.getMaterial(), 1);
				c.getItems().addItem(m.getProduct(), 1);
				c.getPA().addSkillXP(m.getXp() * Config.CRAFTING_EXPERIENCE, Player.playerCrafting);
				c.sendMessage("You spin the " + ItemAssistant.getItemName(m.getMaterial()).toLowerCase()
						+ " into a " + ItemAssistant.getItemName(m.getProduct()).toLowerCase() + ".");
				c.startAnimation(SPIN_ANIMATION);
			}

			@Override
			public void stop() {
				cancel(c);
			}
		}, 2);
	}

	/**
	 * Stops a running spin, if one is running.
	 *
	 * <p>Its own event id rather than {@code stopEvents(c)}, for the same reason as the fletching
	 * cancel: the player owns other skills' events and stopping all of them would put a fire out
	 * or end a smelt.
	 *
	 * <p>The event stop is unconditional and the flag is not. {@code playerIsCrafting} now means
	 * "some crafting action is running", not "this one is" — pottery shares it and
	 * {@code resetCrafting} calls both cancels in turn, so whichever runs second would otherwise
	 * find the flag already cleared and leave its own event queued.
	 */
	public static void cancel(Client c) {
		if (c.playerIsCrafting) {
			c.playerIsCrafting = false;
			c.startAnimation(65535);
		}
		CycleEventHandler.stopEvents(c, SPIN_EVENT);
	}
}
