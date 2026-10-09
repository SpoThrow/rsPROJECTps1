package server.game.players.packets.commands;

import java.util.List;

import server.game.bots.BotController;
import server.game.bots.BotManager;
import server.game.bots.BotPlayer;
import server.game.bots.BotProfile;
import server.game.bots.BotProfiles;
import server.game.bots.BotProvisioning;
import server.game.bots.BotTrace;
import server.game.players.Client;

/**
 * The {@code ::bot} command family — roadmap Phase E's runtime half and Phase F's eyes, owner-gated.
 *
 * <pre>
 * ::bot                  list the configured bots and which are live
 * ::bot list             the same
 * ::bot spawn &lt;account&gt;  spawn a configured row now
 * ::bot despawn &lt;account&gt; stop a live bot and save its character
 * ::bot reload           re-read Data/cfg/bots.cfg and move the live set to match
 * ::bot reprovision &lt;account&gt; &lt;profile&gt;  rebuild a live character's kit (dev tool)
 * ::bot info &lt;account&gt;   what a live bot is doing, and what it last failed at
 * ::botinfo &lt;account&gt;    the same, under the name the roadmap gives it
 * </pre>
 *
 * <p><b>One registry entry for the family, one for {@code ::botinfo}.</b> {@code CommandHandler} runs
 * <em>every</em> command whose predicate matches ({@link CommandHandler} explains why), so registering
 * {@code spawn}/{@code despawn}/{@code reload}/{@code list} as four literals would be four more things
 * that can match a line they should not. {@code ::botinfo} is separate only because it is a distinct
 * word: it does not start with {@code "bot "}, so the two can never both fire.
 *
 * <p><b>Spawn takes a configured account, not a script name.</b> The point of Phase E is that a bot is
 * a config line; a command that could invent a bot on the fly would be a second, undocumented way to
 * define one, and the two would drift. Editing {@code bots.cfg} and running {@code ::bot reload} is the
 * sanctioned path, and it leaves a record.
 */
final class BotCommands {

	/** How many transitions {@code ::bot info} shows. Enough for about two gather cycles. */
	private static final int HISTORY = 10;

	private BotCommands() {
	}

	static void register() {
		CommandHandler.register(Command.where(3, 3,
				(c, playerCommand) -> {
					String trimmed = playerCommand.trim();
					return trimmed.equals("bot") || trimmed.startsWith("bot ");
				},
				(c, playerCommand) -> handle(c, playerCommand)));

		CommandHandler.register(Command.where(3, 3,
				(c, playerCommand) -> {
					String trimmed = playerCommand.trim();
					return trimmed.equals("botinfo") || trimmed.startsWith("botinfo ");
				},
				(c, playerCommand) -> info(c, playerCommand.trim().split("\\s+"), 1)));
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
		case "reprovision":
			reprovision(c, args);
			return;
		case "info":
			info(c, args, 2);
			return;
		default:
			usage(c);
		}
	}

	private static void list(Client c) {
		List<BotProfile> profiles = BotManager.profiles();
		c.sendMessage("Bots: " + BotManager.count() + " live, " + profiles.size() + " configured"
				+ " (last tick: " + BotManager.tickStats() + ")");
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

	/**
	 * Dev-only: rebuild a live bot's character from {@link BotProfiles} — clear, then re-apply
	 * ({@code BOT_ACCOUNTS.md} §4.1). The sanctioned way to change a kit, because provisioning runs only
	 * at creation, so an existing account would otherwise keep whatever it was first made with.
	 *
	 * <p><b>The bot must be live.</b> The change is applied to the character in memory and persisted by
	 * the ordinary save; a bot that is not possessed has no client to apply it to. To rebuild one that is
	 * not running, delete its {@code Data/characters/<name>.txt} and spawn it again.
	 *
	 * <p><b>The script is not restarted</b>, so a bot mid-chop loses its axe underneath it and will fail
	 * the current cycle. Timing is the operator's, which is why this is owner-gated and documented as a
	 * testing tool rather than an everyday action.
	 */
	private static void reprovision(Client c, String[] args) {
		if (args.length < 4) {
			c.sendMessage("Usage: ::bot reprovision <account> <profile>");
			return;
		}
		BotPlayer bot = BotManager.get(args[2]);
		if (bot == null) {
			c.sendMessage("No live bot called \"" + args[2] + "\". Try ::bot list.");
			return;
		}
		BotProfiles.Profile kit = BotProfiles.named(args[3]);
		if (kit == null) {
			c.sendMessage("No profile called \"" + args[3] + "\". Known: " + BotProfiles.names() + ".");
			return;
		}
		BotProvisioning.reprovision(bot, kit);
		c.sendMessage("Reprovisioned " + bot.playerName + " from \"" + kit.name() + "\" ("
				+ kit.itemCount() + " item(s)); it saves like any other character change.");
	}

	/**
	 * The observability dump (roadmap Phase F): what this bot is doing right now, and what it last gave
	 * up on. Three short blocks, because this is read in a game chat window.
	 *
	 * @param nameAt the index in {@code args} holding the account, so both {@code ::bot info x} and
	 *               {@code ::botinfo x} can share one implementation
	 */
	private static void info(Client c, String[] args, int nameAt) {
		if (args.length <= nameAt) {
			c.sendMessage("Usage: ::botinfo <account>");
			return;
		}
		BotPlayer bot = BotManager.get(args[nameAt]);
		if (bot == null) {
			c.sendMessage("No live bot called \"" + args[nameAt] + "\". Try ::bot list.");
			return;
		}
		BotController controller = bot.controller();
		if (controller == null) {
			// Possessed but idle — a valid state, and worth saying rather than reporting an empty trace.
			c.sendMessage(bot.playerName + " is possessed but has no script attached.");
			return;
		}
		BotTrace trace = controller.trace();
		BotProfile profile = BotManager.profileFor(args[nameAt]);
		String script = profile == null ? "(no config row)" : profile.script();
		c.sendMessage(bot.playerName + " — script " + script + ", tree " + controller.root().name()
				+ ", t=" + trace.currentTick() + ", " + trace.recordedCount() + " transition(s)");
		c.sendMessage("  now: " + trace.pathLine());
		BotTrace.Event failure = trace.lastFailure();
		c.sendMessage("  last failure: " + (failure == null ? "none" : failure.describe()));
		for (BotTrace.Event event : trace.history(HISTORY)) {
			c.sendMessage("    " + event.describe());
		}
	}

	private static void usage(Client c) {
		c.sendMessage("::bot list | ::bot spawn <account> | ::bot despawn <account> | ::bot reload"
				+ " | ::bot reprovision <account> <profile> | ::botinfo <account>");
	}
}
