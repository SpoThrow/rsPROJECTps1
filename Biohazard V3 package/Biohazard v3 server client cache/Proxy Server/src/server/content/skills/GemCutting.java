package server.content.skills;

import java.util.Arrays;

import server.Config;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.items.ItemAssistant;
import server.game.players.Client;
import server.game.players.Player;

/**
 * Cutting gems with a chisel, through the chatbox creation menu.
 *
 * <p><b>What this replaced.</b> Using the chisel on an uncut gem cut it, and then kept cutting the
 * whole stack every four cycles until the gems ran out or the player walked. There was nothing to
 * see and nothing to choose: the click was the decision, and the amount was always "all of them".
 * The crafting guide's own Gem Cutting promise is a number, and the interface the guide implies is
 * the chatbox one, so the click now opens it.
 *
 * <p><b>Rows are the gems the player is carrying, not the whole table.</b> There are nine uncut
 * gems and the menu holds five, and a player cutting gems is cutting the ones in their pack — so
 * the rows are built from the pack and the order is the table's own. That is the same choice the
 * tanner makes, and it is a deliberate ceiling rather than a silent one: a player holding more than
 * five kinds sees the first five the table lists and is told the rest are not on the menu, which is
 * a better answer than a menu that claims to list their gems and does not.
 *
 * <p>The pacing is deliberately unchanged: one gem every four cycles, which is what the action
 * already did. Only the stopping condition is new — it stops after the amount the player asked for
 * instead of at the end of the stack.
 */
public class GemCutting extends CraftingData {

	/**
	 * Its own event id, so stopping a cut cannot stop the pottery, spin or leather action running
	 * under the same {@code playerIsCrafting} flag — and so a cancelled cut can be taken off the
	 * scheduler rather than left to notice the flag on its next tick.
	 */
	private static final int GEM_EVENT = 4620;

	/** Cycles between gems, which is what this action has always run at. */
	private static final int ACTION_CYCLES = 4;

	/**
	 * The chisel on an uncut gem: what {@code UseItem} calls.
	 *
	 * <p>The item that was clicked decides only that this is the chisel's business — which gem to
	 * cut is the menu's question, answered by the row the player picks, exactly as the pair in a
	 * herblore mix is answered by a button rather than by the click.
	 */
	public static void cutGem(final Client c, final int itemUsed, final int usedWith) {
		openMenu(c);
	}

	/** Opens the chisel's chatbox on the uncut gems in the player's pack. */
	public static void openMenu(final Client c) {
		if (c.playerIsCrafting) {
			return;
		}
		int[] gems = carriedGems(c);
		if (gems.length == 0) {
			c.sendMessage("You have no uncut gems to cut.");
			return;
		}
		int shown = CreationMenu.open(c, CreationMenu.Kind.GEM_CUTTING, gems);
		if (shown < gems.length) {
			// The row limit is the interface's, not a rule of the skill, so the player is told
			// rather than left to wonder where the rest of their gems went.
			c.sendMessage("The chisel can only offer five kinds of gem at a time.");
		}
	}

	/**
	 * The uncut gems in the pack, in the table's order, deduplicated by kind rather than by slot.
	 *
	 * <p>Two stacks of sapphires are one row: the row is a product to make, and the amount is the
	 * button's business, so listing a kind twice would be two rows answering the same click.
	 */
	public static int[] carriedGems(final Client c) {
		int[] found = new int[cutGemData.values().length];
		int n = 0;
		for (cutGemData g : cutGemData.values()) {
			if (c.getItems().playerHasItem(g.getUncut(), 1)) {
				found[n++] = g.getUncut();
			}
		}
		return Arrays.copyOf(found, n);
	}

	/** The table row for an uncut gem, or null when the id is not one. */
	public static cutGemData forUncut(final int uncut) {
		for (cutGemData g : cutGemData.values()) {
			if (g.getUncut() == uncut) {
				return g;
			}
		}
		return null;
	}

	/**
	 * A row's worth of cutting. Called from {@link CreationMenu#route} with the row's gem and the
	 * amount its button asks for.
	 *
	 * <p>Every condition is re-checked here rather than trusted from the click that opened the
	 * menu: the pack can have changed while the menu was up, and the level can be short of the
	 * gem's — which is reachable, because the menu lists what the player holds rather than what
	 * they can cut. That is the point of listing by pack: a level-30 player can see that the
	 * diamond they are carrying is a diamond, and be told what it wants.
	 */
	public static void select(final Client c, final int uncut, final int amount) {
		cutGemData gem = forUncut(uncut);
		if (gem == null || amount <= 0) {
			return;
		}
		if (c.skills.playerLevel[Player.playerCrafting] < gem.getLevel()) {
			c.sendMessage("You need a crafting level of " + gem.getLevel() + " to cut this gem.");
			return;
		}
		if (!c.getItems().playerHasItem(uncut, 1)) {
			c.sendMessage("You have no " + ItemAssistant.getItemName(uncut).toLowerCase() + " to cut.");
			return;
		}
		if (c.playerIsCrafting) {
			return;
		}
		CreationMenu.close(c);
		c.getPA().removeAllWindows();
		start(c, gem, amount);
	}

	/**
	 * Cuts one gem every {@link #ACTION_CYCLES}, up to {@code amount} times.
	 *
	 * <p>The gem and the amount travel in the closure rather than through fields, so two players
	 * cutting at once cannot read each other's choice — the same shape as the fletching and
	 * herblore actions, and the reason the old static-field loops had to go.
	 */
	private static void start(final Client c, final cutGemData gem, final int amount) {
		c.playerIsCrafting = true;
		c.startAnimation(gem.getAnimation());
		CycleEventHandler.addEvent(GEM_EVENT, c, new CycleEvent() {

			private int left = amount;

			@Override
			public void execute(CycleEventContainer container) {
				if (!c.playerIsCrafting || left <= 0 || !c.getItems().playerHasItem(gem.getUncut(), 1)) {
					container.stop();
					return;
				}
				c.getItems().deleteItem2(gem.getUncut(), 1);
				c.getItems().addItem(gem.getCut(), 1);
				c.getPA().addSkillXP((int) gem.getXP() * Config.CRAFTING_EXPERIENCE,
						Player.playerCrafting);
				c.sendMessage("You cut the " + ItemAssistant.getItemName(gem.getUncut()).toLowerCase() + ".");
				c.startAnimation(gem.getAnimation());
				left--;
			}

			@Override
			public void stop() {
				cancel(c);
			}
		}, ACTION_CYCLES);
	}

	/**
	 * Stops a running cut, if one is running.
	 *
	 * <p>The event stop is unconditional and the flag is not, for the reason {@code Spinning.cancel}
	 * documents: {@code playerIsCrafting} means "some crafting action is running", so whichever
	 * cancel runs second must still remove its own event rather than find the flag already cleared.
	 */
	public static void cancel(final Client c) {
		if (c.playerIsCrafting) {
			c.playerIsCrafting = false;
			c.startAnimation(65535);
		}
		CycleEventHandler.stopEvents(c, GEM_EVENT);
	}
}
