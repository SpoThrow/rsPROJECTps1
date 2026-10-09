package server.game.bots;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import core.util.Misc;
import server.Config;
import server.game.players.PlayerHandler;
import server.game.players.PlayerSave;

/**
 * Creates, possesses and releases bots, and enforces the hard bot cap.
 *
 * <p>A bot is a real character driven by code instead of a socket ({@code BOT_PLAN.md}
 * §5.4). {@code possess} attaches a controller to an existing account; {@code release}
 * saves it and frees the slot, exactly as a logout would — so the same account can then be
 * logged into by a human.
 *
 * <p>Count is tracked here rather than derived from the player array, so the cap is O(1)
 * and cannot be defeated by a bot that is momentarily {@code !isActive}.
 */
public final class BotManager {

	/** Well under {@code Config.MAX_PLAYERS} (50), leaving headroom for real players. */
	public static final int MAX_BOTS = 10;

	/** The marker written to {@code connectedFrom} for a sessionless client. */
	public static final String BOT_CONNECTED_FROM = "bot";

	/** Where a freshly created bot stands until a profile says otherwise. */
	private static final int DEFAULT_X = 3087, DEFAULT_Y = 3236, DEFAULT_PLANE = 0;

	private static final List<BotPlayer> live = new ArrayList<BotPlayer>();

	/** So the cap is reported once, not once per refused possess. */
	private static boolean capReported;

	private BotManager() {
	}

	public static int count() {
		return live.size();
	}

	/** A snapshot, so callers cannot mutate the live list. */
	public static List<BotPlayer> all() {
		return Collections.unmodifiableList(new ArrayList<BotPlayer>(live));
	}

	public static BotPlayer get(String name) {
		if (name == null) {
			return null;
		}
		for (BotPlayer bot : live) {
			if (name.equalsIgnoreCase(bot.playerName)) {
				return bot;
			}
		}
		return null;
	}

	/**
	 * Slice-1 wiring point. Left a no-op on purpose: bots are started through
	 * {@code Data/cfg/bots.cfg} (roadmap Phase E), so {@code Server.main} stays untouched.
	 */
	public static void start() {
		// Intentionally empty.
	}

	/**
	 * Materialises an account on disk without logging it in and <b>without</b> the
	 * new-player path (no {@code addStarter}: it is IP-gated and freezes walking — see
	 * {@code BOT_ACCOUNTS.md} §4).
	 *
	 * @return true if a character file was written; false if the name is illegal or taken
	 */
	public static boolean createAccount(String name, String password) {
		if (!BotNames.isLoginLegal(name) || password == null || password.isEmpty()) {
			return false;
		}
		if (characterFile(name).exists()) {
			return false;
		}
		BotPlayer bot = new BotPlayer(0);
		bot.playerName = name;
		bot.playerName2 = name;
		bot.playerPass = password.toLowerCase();
		bot.newPlayer = false;      // PlayerSave refuses a "new" client
		bot.addStarter = false;     // never the tutorial / IP-gated starter
		bot.canWalk = true;
		bot.position.absX = bot.position.teleportToX = DEFAULT_X;
		bot.position.absY = bot.position.teleportToY = DEFAULT_Y;
		bot.position.heightLevel = DEFAULT_PLANE;

		// saveGame refuses a client that is not in the player array, so the account is
		// registered just long enough to be written and then taken back out of the world.
		if (!PlayerHandler.registerSessionless(bot)) {
			return false;
		}
		boolean written = PlayerSave.saveGame(bot);
		PlayerHandler.players[bot.playerId] = null;
		return written;
	}

	/**
	 * Attaches a controller to an existing account.
	 *
	 * <p>Return codes from {@link PlayerSave#loadGame} are handled as login handles them:
	 * {@code 0} no such account (use {@link #createAccount}), {@code 3} wrong password,
	 * {@code 13} truncated file — let it in but never write it back.
	 *
	 * @return the live bot, or null if it could not be possessed
	 */
	public static BotPlayer possess(String name, String password) {
		if (name == null || password == null) {
			return null;
		}
		if (get(name) != null) {
			return null; // already possessed by this manager
		}
		if (live.size() >= MAX_BOTS) {
			if (!capReported) {
				capReported = true;
				Misc.println("[bots] refusing to possess " + name + ": cap of " + MAX_BOTS + " reached");
			}
			return null;
		}
		if (PlayerHandler.getPlayer(name) != null || PlayerHandler.isPlayerOn(name)) {
			return null; // a human (or another controller) already holds this name
		}

		BotPlayer bot = new BotPlayer(0);
		bot.playerName = name;
		bot.playerName2 = name;
		bot.playerPass = password.toLowerCase();

		int load = PlayerSave.loadGame(bot, name, password);
		if (load == 0 || load == 3) {
			return null;
		}
		bot.newPlayer = false;
		if (load == 13) {
			// No [EOF]: operate on the partial state, but never overwrite the damaged file
			// (and its .bak) with what we loaded — the same rule login applies.
			Misc.println("[bots] " + name + ": truncated save, saving disabled for this session");
			bot.saveFile = false;
		}

		if (!PlayerHandler.registerSessionless(bot)) {
			return null;
		}
		bot.connectedFrom = BOT_CONNECTED_FROM;
		live.add(bot);
		return bot;
	}

	/**
	 * Detaches the controller, saves the character and frees its slot — the handover point
	 * a human logs in at.
	 *
	 * @return true if a possessed bot was released
	 */
	public static boolean release(String name) {
		BotPlayer bot = get(name);
		if (bot == null) {
			return false;
		}
		live.remove(bot);
		// Interrupt the tree first so charges, animations and CycleEvents are released
		// before the character is written.
		if (bot.controller() != null) {
			bot.controller().stop();
			bot.attach(null);
		}
		bot.saveCharacterOnce();
		if (bot.playerId >= 0 && bot.playerId < PlayerHandler.players.length
				&& PlayerHandler.players[bot.playerId] == bot) {
			PlayerHandler.players[bot.playerId] = null;
		}
		return true;
	}

	/**
	 * Possesses an account and attaches a behaviour tree to it. The root enters immediately
	 * and ticks once per game tick through {@link BotPlayer#process()}.
	 *
	 * @param root the behaviour tree, or null for a possessed-but-idle bot
	 */
	public static BotPlayer possess(String name, String password, BotState root) {
		BotPlayer bot = possess(name, password);
		if (bot != null && root != null) {
			bot.attach(new BotController(bot, root));
		}
		return bot;
	}

	private static File characterFile(String name) {
		return new File("./Data/characters/" + name.toLowerCase() + ".txt");
	}

	static {
		if (MAX_BOTS > Config.MAX_PLAYERS) {
			throw new IllegalStateException("MAX_BOTS cannot exceed Config.MAX_PLAYERS");
		}
	}
}
