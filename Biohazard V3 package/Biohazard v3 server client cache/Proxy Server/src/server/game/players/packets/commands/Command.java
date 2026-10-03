package server.game.players.packets.commands;

import java.util.function.BiPredicate;

import server.game.players.Client;

/**
 * One entry in the {@link CommandHandler} registry: a rights window, a predicate over
 * the raw command text, and the action to run when both match.
 *
 * <p>The old {@code Commands.processPacket} chain gated whole runs of commands with
 * enclosing {@code if (c.playerRights ...)} blocks. Those gates are modelled here as
 * {@link #minimumRights()}/{@link #maximumRights()} rather than as a second predicate,
 * so a test can check the tier a command belongs to without constructing a Client.
 *
 * <p>A command keyed on a single literal keeps that literal (and whether it is a
 * prefix match) so the registry stays introspectable; a command whose original
 * condition was composite ({@code ||}, or an extra clause such as
 * {@code && c.isBanking}) is registered with {@link #where} and reports no literal.
 */
final class Command {

	private final String literal;
	private final boolean prefix;
	private final int minimumRights;
	private final int maximumRights;
	private final BiPredicate<Client, String> predicate;
	private final CommandAction action;

	private Command(String literal, boolean prefix, int minimumRights, int maximumRights,
			BiPredicate<Client, String> predicate, CommandAction action) {
		this.literal = literal;
		this.prefix = prefix;
		this.minimumRights = minimumRights;
		this.maximumRights = maximumRights;
		this.predicate = predicate;
		this.action = action;
	}

	static Command prefix(String literal, int minimumRights, int maximumRights, CommandAction action) {
		return new Command(literal, true, minimumRights, maximumRights,
				(c, playerCommand) -> playerCommand.startsWith(literal), action);
	}

	static Command exact(String literal, int minimumRights, int maximumRights, CommandAction action) {
		return new Command(literal, false, minimumRights, maximumRights,
				(c, playerCommand) -> playerCommand.equalsIgnoreCase(literal), action);
	}

	static Command where(int minimumRights, int maximumRights,
			BiPredicate<Client, String> predicate, CommandAction action) {
		return new Command(null, false, minimumRights, maximumRights, predicate, action);
	}

	boolean appliesTo(Client c, String playerCommand) {
		return c.playerRights >= minimumRights
				&& c.playerRights <= maximumRights
				&& predicate.test(c, playerCommand);
	}

	void run(Client c, String playerCommand) {
		action.run(c, playerCommand);
	}

	/** The literal this command is keyed on, or {@code null} for a composite predicate. */
	String literal() {
		return literal;
	}

	boolean isPrefix() {
		return prefix;
	}

	int minimumRights() {
		return minimumRights;
	}

	int maximumRights() {
		return maximumRights;
	}

	/**
	 * Evaluates only the literal-and-rights half of the predicate, for tests that have
	 * no {@link Client}. Composite ({@code where}) commands always report {@code false}.
	 */
	boolean matchesLiteral(int playerRights, String playerCommand) {
		if (literal == null || playerRights < minimumRights || playerRights > maximumRights) {
			return false;
		}
		return prefix ? playerCommand.startsWith(literal) : playerCommand.equalsIgnoreCase(literal);
	}
}
