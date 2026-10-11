package server.game.players.actions.objects;

import server.game.minigames.crystalchest.CrystalChest;

/**
 * Left-click open for the crystal key chest (object 172).
 *
 * <p>The chest only ever had the use-key-on-chest action, so a player standing at it
 * with a key still had to open their inventory and use it by hand. Clicking the chest
 * now does the same thing when the key is held and there is room for the loot.
 *
 * <p>Silent without a key rather than "the chest is locked": a bare click on scenery
 * that says nothing is the ordinary case, and the message is reserved for the
 * deliberate use-item-on-chest action in {@code UseItem}.
 */
public final class CrystalChestObjects {

	/** Crystal key chest. */
	private static final int CHEST = 172;

	/** The chest hands out a dragonstone, coins, and one reward, so it wants the room. */
	private static final int NEEDED_SLOTS = 3;

	private CrystalChestObjects() {
	}

	static void register() {
		ObjectHandler.register(CHEST, ObjectClick.FIRST, (c, objectType, objectX, objectY) -> {
			if (!c.getItems().playerHasItem(CrystalChest.KEY)) {
				return;
			}
			if (c.getItems().freeSlots() < NEEDED_SLOTS) {
				c.sendMessage("You need at least " + NEEDED_SLOTS
						+ " free inventory slots to open the chest.");
				return;
			}
			CrystalChest.searchChest(c, objectType, objectX, objectY);
		});
	}
}
