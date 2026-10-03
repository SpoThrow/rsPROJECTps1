package server.game.players.packets.commands;

import server.game.players.Client;

/**
 * Runs one chat command. See {@link CommandHandler}.
 *
 * Generated command registrations pass the raw command text (including the command
 * word itself) because the old if-chain bodies parse their arguments with
 * {@code playerCommand.substring(...)}.
 */
@FunctionalInterface
interface CommandAction {

	void run(Client c, String playerCommand);
}
