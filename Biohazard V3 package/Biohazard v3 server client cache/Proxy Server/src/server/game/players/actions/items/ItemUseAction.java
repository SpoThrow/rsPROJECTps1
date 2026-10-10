package server.game.players.actions.items;

import server.game.players.Client;

/**
 * Handles one unordered pair of items used on each other, migrated out of
 * {@code UseItem.ItemonItem}.
 *
 * <p><b>Unordered, because that is what the legacy method is.</b> Every check in it is
 * written {@code a == x && b == y || b == x && a == y} — the client sending the pair the
 * other way round is not a different action. {@link ItemUseRegistry} normalises the pair
 * before keying on it, so a handler cannot be registered for one order only and quietly
 * miss half the clicks.
 *
 * <p>{@code void} for the same reason as the object and button registries: registration is
 * the claim that this pair is handled, so there is nothing left for the handler to report
 * by returning.
 */
public interface ItemUseAction {

	void handle(Client c, int itemUsed, int useWith);
}
