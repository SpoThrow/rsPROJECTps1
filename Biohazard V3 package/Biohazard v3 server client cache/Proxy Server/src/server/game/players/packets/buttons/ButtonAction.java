package server.game.players.packets.buttons;

import server.game.players.Client;

/**
 * Handles one exact interface button id.
 *
 * <p>Deliberately {@code void}: registration in {@link ButtonHandler} is itself
 * the claim that this id is handled. Handlers that need to decline a <em>range</em>
 * of ids (the POS and curse buttons, for instance) stay in the preamble of
 * {@code ClickingButtons}, because a map keyed on an exact id cannot express them.
 */
public interface ButtonAction {

	void handle(Client c, int actionButtonId);
}
