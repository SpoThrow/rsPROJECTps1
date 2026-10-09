package server.game.players.packets.commands;

import java.util.List;

import server.game.bots.BotManager;
import server.game.bots.BotPlayer;
import server.game.bots.BotProfile;
import server.game.players.Client;

/**
 * The {@code ::bot} command family — roadmap Phase E's runtime half, owner-gated.
 *
 * <pre>
 * ::bot                 list the configured bots and which are live
 * ::bot list            the same
 * ::bot spawn &lt;account&gt; spawn a configured row now
 * ::bot despawn &lt;account&gt; stop a live bot and save its character
 * ::bot reload          re-read Data/cfg/bots.cfg and move the live set to match
 * </pre>
 *
 * <p><b>One registry entry, not five.</b> {@code CommandHandler} runs <em>every</em> command whose
 * predicate matches ({@link CommandHandler} explains why), so five {@code ::bot} subcommands registered
 * as five literals would each be one more thing that can match a line it should not. A single entry
 * whose predicate is {@code "bot"} or {@code "bot ..."} keeps the surface one command deep, the same
 * shape {@code ban }/{\@code kick } use to avoid swallowing {@code banki}/{@code banke}.
 *
 * <p><b>Spawn takes a configured account, not a script name.</b> The point of Phase E is that a bot is
 * a config line; a command that could invent a bot on the fly would be a second, undocumented way to
 * define one, and the two would drift. Editing {@code bots.cfg} and running {@code ::bot reload} is the
 * sanctioned path, and it leaves a record.
 */
final class BotCommands {

	private BotCommands() {
	}

	static void register() {
		CommandHandler.register(Command.where(3, 3,
				(c, playerCommand) -> {
					String trimmed = playerCommand.trim();
					return trimmed.equals("bot") || trimmed.startsWith("bot ");
				},
				(c, playerCommand) -> handle(c, playerCommand)));
	}

	private static void handle(Client c, String playerCommand) {
		String[] args = playerCommand.trim().split("\\s+");
		String action = args.length > 1 ? args[1].toLowerCase() : "list";
		switch (action) {
		case "list":
			list(c);
			return;
		case "spawn":
			spawn(c, args);
			return;
		case "despawn":
			despawn(c, args);
			return;
		case "reload":
			reload(c);
			return;
		default:
			usage(c);
		}
	}

	private static void list(Client c) {
		List<BotProfile> profiles = BotManager.profiles();
		c.sendMessage("Bots: " + BotManager.count() + " live, " + profiles.size() + " configured");
		if (profiles.isEmpty()) {
			c.sendMessage("  no rows in Data/cfg/bots.cfg — add one, then ::bot reload");
			return;
		}
		for (BotProfile profile : profiles) {
			boolean live = BotManager.get(profile.account()) != null;
			c.sendMessage("  " + profile.account() + " — script " + profile.script()
					+ (profile.home() == null ? "" : ", home " + profile.home())
					+ (profile.enabled() ? "" : ", disabled")
					+ (live ? ", live" : ""));
		}
	}

	private static void spawn(Client c, String[] args) {
		if (args.length < 3) {
			c.sendMessage("Usage: ::bot spawn <account>");
			return;
		}
		BotProfile profile = BotManager.profileFor(args[2]);
		if (profile == null) {
			c.sendMessage("No bots.cfg row for \"" + args[2] + "\".");
			return;
		}
		if (BotManager.get(profile.account()) != null) {
			c.sendMessage(profile.account() + " is already live.");
			return;
		}
		BotPlayer bot = BotManager.spawn(profile);
		if (bot == null) {
			c.sendMessage("Could not spawn " + profile.account() + " — see the server console.");
			return;
		}
		c.sendMessage("Spawned " + profile.account() + " running " + profile.script() + ".");
	}

	private static void despawn(Client c, String[] args) {
		if (args.length < 3) {
			c.sendMessage("Usage: ::bot despawn <account>");
			return;
		}
		BotProfile profile = BotManager.profileFor(args[2]);
		if (profile == null) {
			c.sendMessage("No bots.cfg row for \"" + args[2] + "\".");
			return;
		}
		c.sendMessage(BotManager.despawn(profile)
				? "Despawned " + profile.account() + " (character saved)."
				: profile.account() + " is not live.");
	}

	private static void reload(Client c) {
		int spawned = BotManager.reload();
		c.sendMessage("Reloaded bots.cfg: " + spawned + " spawned, " + BotManager.count() + " live.");
	}

	private static void usage(Client c) {
		c.sendMessage("::bot list | ::bot spawn <account> | ::bot despawn <account> | ::bot reload");
	}
}
