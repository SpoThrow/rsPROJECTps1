package server.game.players.actions.items;

import server.game.players.Client;

/**
 * Handles one {@code (itemId, objectId)} pair used on each other, migrated out of
 * {@code UseItem.ItemonObject}.
 *
 * <p><b>Ordered, unlike {@link ItemUseAction}.</b> "Use this item on that object" is not
 * the same as "use that object on this item": the legacy method is a {@code switch} on the
 * object id with an inner check on the item, so the pair has a direction and a handler is
 * told which is which.
 */
public interface ItemOnObjectAction {

	void handle(Client c, int itemId, int objectId, int objectX, int objectY);
}
