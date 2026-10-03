package server.game.players.actions.objects;

import server.game.players.Client;

/**
 * Handles one {@code (objectType, click)} pair, migrated out of the switch in
 * {@code ActionHandler}.
 *
 * <p>{@code void} for the same reason as the button registry: registration is the
 * claim that this pair is handled. Guards that claim a <em>set</em> of objects by
 * predicate (mining rocks, the Barrows objects, the dwarf cannon's pickup) stay in
 * {@code ActionHandler}'s preamble, because a map keyed on an exact id cannot
 * express them.
 */
public interface ObjectAction {

	void handle(Client c, int objectType, int objectX, int objectY);
}
