package server.game.minigames.crystalchest;

import core.util.Misc;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.players.Client;

/*
 * @author Liberty / Robbie
 */

public class CrystalChest {

	private static final int[] CHEST_REWARDS = { 1079, 1093, 526, 1969, 371, 2363, 451 };
	public static final int[] KEY_HALVES = { 985, 987 };
	public static final int KEY = 989;
	private static final int DRAGONSTONE = 1631;
	private static final int OPEN_ANIMATION = 881;

	public static void makeKey(Client c) {
		if (c.getItems().playerHasItem(toothHalf(), 1)
				&& c.getItems().playerHasItem(loopHalf(), 1)) {
			c.getItems().deleteItem(toothHalf(), 1);
			c.getItems().deleteItem(loopHalf(), 1);
			c.getItems().addItem(KEY, 1);
		}
	}

	public static boolean canOpen(Client c) {
		if (c.getItems().playerHasItem(KEY)) {
			return true;
		} else {
			c.sendMessage("The chest is locked");
			return false;
		}
	}

	public static void searchChest(final Client c, final int id, final int x,
			final int y) {
		if (canOpen(c)) {
			c.sendMessage("You unlock the chest with your key.");
			c.getItems().deleteItem(KEY, 1);
			c.startAnimation(OPEN_ANIMATION);
			c.getPA().checkObjectSpawn(id + 1, x, y, 2, 10);
			CycleEventHandler.addEvent(c, new CycleEvent() {
				@Override
				public void execute(CycleEventContainer container) {
					c.getItems().addItem(DRAGONSTONE, 1);
					c.getItems().addItem(995, Misc.random(8230));
					c.getItems().addItem(
							CHEST_REWARDS[Misc.random(getLength() - 1)], 1);
					c.sendMessage("You find some treasure in the chest.");
					c.getPA().checkObjectSpawn(id, x, y, 2, 10);

					container.stop();
				}
					@Override
					public void stop() {
					}
				}, 1);
		}
	}

	public static int getLength() {
		return CHEST_REWARDS.length;
	}
	public static int toothHalf(){
		return KEY_HALVES[0];
	}
	public static int loopHalf(){
		return KEY_HALVES[1];
	}
}