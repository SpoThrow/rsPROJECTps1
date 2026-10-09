package server.game.players.packets.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import server.game.players.Client;

/**
 * Ordered registry of chat commands, consulted by
 * {@link server.game.players.packets.Commands#processPacket}.
 *
 * <p><strong>Order and multi-match are load-bearing.</strong> The implementation this
 * replaced was a linear if-chain in which every {@code if} was evaluated -- there was
 * no {@code else if} -- so two commands could both match one input and both run, in
 * source order. {@code ::xteletome name} matched both {@code xteleto} and
 * {@code xteletome}, and {@code ::item 4151 1} matched it twice for an owner.
 * {@link #dispatch} therefore walks the whole list and runs every match rather than
 * stopping at the first, and the registration order in the static block below
 * reproduces the original source order.
 *
 * <p>Adding a {@code *Commands} class without registering it here leaves every command
 * in it dead, so the registration calls are the one place that must stay in sync.
 */
public final class CommandHandler {

	private static final List<Command> COMMANDS = new ArrayList<>();

	static {
		GeneralCommands.register();
		ModeratorCommands.register();
		StaffCommands.register();
		OwnerCommands.register();
		// Hand-written, and registered after the generated groups because the generated files are
		// regenerated from the old if-chain and must not be edited. See BotCommands.
		BotCommands.register();
	}

	private CommandHandler() {
	}

	static void register(Command command) {
		COMMANDS.add(command);
	}

	/**
	 * Runs every registered command whose predicate matches, in registration order.
	 *
	 * @return true if at least one command ran. The return value is informational: the
	 *         caller must not use it to skip anything, because a later command may still
	 *         match the same input.
	 */
	public static boolean dispatch(Client c, String playerCommand) {
		boolean handled = false;
		for (Command command : COMMANDS) {
			if (command.appliesTo(c, playerCommand)) {
				command.run(c, playerCommand);
				handled = true;
			}
		}
		return handled;
	}

	/** Registration-order snapshot, for tests. */
	static List<Command> all() {
		return Collections.unmodifiableList(COMMANDS);
	}
}
