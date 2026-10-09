package server.game.bots;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import core.util.Misc;
import server.Config;
import server.game.bots.script.BotScript;
import server.game.bots.script.BotScripts;
import server.game.bots.world.Location;
import server.game.bots.world.Locations;
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

	/**
	 * The most tree ticks one game tick may perform, and the wall-clock ceiling on the time they may
	 * take. Read once from {@link Config} so the scheduler's hot path is two field reads.
	 */
	public static final int TICK_BUDGET = Config.BOT_TICK_BUDGET;
	private static final long TICK_BUDGET_NANOS = Config.BOT_TICK_BUDGET_MS * 1_000_000L;

	/**
	 * The live count budget, normally {@link #TICK_BUDGET}.
	 *
	 * <p>Mutable only so a test can shrink the budget below a handful of bots and so exercise the
	 * throttle end to end — at the shipped budget of {@link #TICK_BUDGET} and a cap of
	 * {@code MAX_BOTS}, the budget never bites, and a bound that is never reached is a bound that is
	 * never proven. There is no operator-facing setter: {@code TICK_BUDGET} is a compile-time constant
	 * like every other {@link Config} value.
	 */
	private static int tickBudget = TICK_BUDGET;

	private static final List<BotPlayer> live = new ArrayList<BotPlayer>();

	// ---- per-tick scheduling (roadmap Phase H) ----------------------------------------------
	// Reset by beginTick() each game tick, consumed by tickTree(). Kept as plain fields rather than an
	// object because there is exactly one tick in flight (the tick is single-threaded) and this is read
	// once per bot per tick.

	/** How many game ticks have been scheduled. Drives the rotation window; never reset. */
	private static long tickNumber;

	/** Wall-clock nanoseconds spent inside behaviour trees so far this tick. */
	private static long treeNanosThisTick;

	/** Tree ticks actually performed this tick, and those declined. Reported by {@link #tickStats()}. */
	private static int treeTicksThisTick;
	private static int deferredThisTick;

	/** So the cap is reported once, not once per refused possess. */
	private static boolean capReported;

	/** The last config read, and whether {@link #start()} has already run. */
	private static BotsConfig.Result loaded = new BotsConfig.Result(new ArrayList<BotProfile>(),
			new ArrayList<String>());
	private static boolean started;

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
	 * Loads {@code Data/cfg/bots.cfg} and spawns every enabled row — roadmap Phase E, the single call
	 * {@code Server.main} makes.
	 *
	 * <p><b>A missing file spawns nothing, and that is the point.</b> With no config the server boots
	 * exactly as it did before bots existed, which is what lets the whole package be deleted without a
	 * trace. With a config, a new bot is a line rather than a rebuild — the phase's acceptance
	 * criterion.
	 *
	 * <p>Runs once. A second call is ignored so a stray call site cannot double-spawn; reloading the
	 * file at runtime is {@link #reload()}, which is explicit about what it does.
	 *
	 * @return the number of bots actually possessed
	 */
	public static int start() {
		if (started) {
			return 0;
		}
		started = true;
		return apply(BotsConfig.load());
	}

	/**
	 * Re-reads the config and moves the live set to match it: rows that are new or newly enabled are
	 * spawned, rows that were removed or disabled are released.
	 *
	 * <p>This is the "author a bot without restarting" path, and the reason the diff is by
	 * <em>account</em> rather than a full restart is that a restart would throw away a running bot's
	 * position and inventory. A row edited in place (a script swap) takes effect on the next
	 * despawn/spawn pair, not mid-run — deliberately: silently swapping a live tree out from under a bot
	 * is how you get a half-executed walk.
	 *
	 * @return the number of bots spawned by this reload
	 */
	public static int reload() {
		return apply(BotsConfig.load());
	}

	/**
	 * Makes the live set match {@code result}: releases bots that are gone or disabled, spawns the
	 * enabled rows, and reports the read problems.
	 *
	 * <p>Package-private so a test can drive the whole config-to-live path from a temp file without
	 * depending on the process working directory. {@link #start()} and {@link #reload()} are its two
	 * public faces — boot and operator — and both exist only to decide <em>when</em> it runs.
	 *
	 * @return the number of bots spawned
	 */
	static int apply(BotsConfig.Result result) {
		loaded = result;
		for (String problem : result.problems()) {
			Misc.println("[bots] bots.cfg " + problem);
		}
		for (BotPlayer bot : all()) {
			BotProfile profile = result.byAccount(bot.playerName);
			if (profile == null || !profile.enabled()) {
				release(bot.playerName);
			}
		}
		// Counted as a delta rather than by the return of spawn(), because spawn() is idempotent and
		// returns an already-live bot too — counting that would report a spawn that did not happen.
		// spawnAll() is that same delta, so the cohort path and this one cannot disagree.
		int spawned = spawnAll(result.enabled());
		if (spawned > 0 || !result.problems().isEmpty()) {
			Misc.println("[bots] " + spawned + " of " + result.enabled().size()
					+ " configured bot(s) running (" + count() + " live)");
		}
		return spawned;
	}

	/**
	 * Possesses the account a profile names and attaches its script — the one place a config row becomes
	 * a live bot.
	 *
	 * <p><b>The script is checked before the account is created.</b> A typo in {@code script} should not
	 * leave a stray character file behind, so an unknown script is refused before any disk work.
	 *
	 * <p><b>A missing account is created, not an error.</b> A config line is meant to be sufficient
	 * ({@code BOT_ACCOUNTS.md} §1), so an account file that is not there yet is created with the row's
	 * own password, provisioned from the row's {@code profile} (or {@link BotProfiles#DEFAULT}), and then
	 * possessed — which is what gives a fresh bot the axe its script needs.
	 *
	 * @return the live bot, or null when it could not be spawned
	 */
	public static BotPlayer spawn(BotProfile profile) {
		if (profile == null) {
			return null;
		}
		BotPlayer existing = get(profile.account());
		if (existing != null) {
			return existing; // already running: a spawn is idempotent, not an error
		}
		BotScript script = BotScripts.byName(profile.script());
		if (script == null) {
			Misc.println("[bots] " + profile.account() + ": no script named \"" + profile.script()
					+ "\" (known: " + BotScripts.names() + ")");
			return null;
		}
		if (!characterFile(profile.account()).exists()
				&& !createAccount(profile.account(), profile.password(), kitFor(profile))) {
			Misc.println("[bots] " + profile.account() + ": could not create the account");
			return null;
		}
		BotPlayer bot = possess(profile.account(), profile.password(), script.root());
		if (bot == null) {
			Misc.println("[bots] " + profile.account() + ": could not possess (see above)");
			return null;
		}
		applyHome(bot, profile);
		return bot;
	}

	/** Releases the account a profile names, if it is running. */
	public static boolean despawn(BotProfile profile) {
		return profile != null && release(profile.account());
	}

	/**
	 * Resolves the kit a row asks for, falling back to {@link BotProfiles#DEFAULT} with a note.
	 *
	 * <p><b>An unknown profile name is a message, not a refusal</b> — the same treatment an unknown
	 * {@code home} gets, and for the same reason. It is only consulted when the character does not exist
	 * yet, so refusing the row would also refuse an account that is perfectly fine on disk; and the
	 * default kit carries a tool for every resource the world supports, so a typo costs a leaner kit
	 * rather than a dead bot.
	 */
	private static BotProfiles.Profile kitFor(BotProfile row) {
		if (row.profile() == null) {
			return BotProfiles.DEFAULT;
		}
		BotProfiles.Profile kit = BotProfiles.named(row.profile());
		if (kit == null) {
			Misc.println("[bots] " + row.account() + ": no profile named \"" + row.profile()
					+ "\" (known: " + BotProfiles.names() + "); using the default kit");
			return BotProfiles.DEFAULT;
		}
		return kit;
	}

	/**
	 * Moves a freshly possessed bot to its configured home, if it has one.
	 *
	 * <p>Resolved here rather than at parse time so reading {@code bots.cfg} touches no world (see
	 * {@link BotProfile}). An unknown home is a message, not a failure: the bot is already possessed and
	 * running, and standing where the account was saved is a survivable answer.
	 */
	private static void applyHome(BotPlayer bot, BotProfile profile) {
		if (profile.home() == null) {
			return;
		}
		Location place = Locations.live().everything().byName(profile.home());
		if (place == null) {
			Misc.println("[bots] " + profile.account() + ": no place named \"" + profile.home()
					+ "\"; leaving the bot where the account was saved");
			return;
		}
		bot.position.teleportToX = place.centreX();
		bot.position.teleportToY = place.centreY();
		bot.position.heightLevel = place.plane();
		bot.getNextPlayerMovement();
	}

	/** The last config read. Empty until {@link #start()} or {@link #reload()} has run. */
	public static List<BotProfile> profiles() {
		return loaded.profiles();
	}

	/** The configured row for {@code account}, or null. Case-insensitive, like a login. */
	public static BotProfile profileFor(String account) {
		return loaded.byAccount(account);
	}

	/**
	 * Materialises an account on disk without logging it in and <b>without</b> the
	 * new-player path (no {@code addStarter}: it is IP-gated and freezes walking — see
	 * {@code BOT_ACCOUNTS.md} §4) — and with {@link BotProfiles#DEFAULT}'s kit.
	 *
	 * @return true if a character file was written; false if the name is illegal or taken
	 */
	public static boolean createAccount(String name, String password) {
		return createAccount(name, password, BotProfiles.DEFAULT);
	}

	/**
	 * As {@link #createAccount(String, String)}, but the new account is provisioned from {@code profile}:
	 * tie flag, kit, skills, starting tile and spellbook.
	 *
	 * <p>This is the step whose absence left every spawned bot empty-handed. It used to create the
	 * character and stop there, so a config-spawned woodcutter owned no axe and its {@code Gather} loop
	 * failed on the first click — the gap the roadmap recorded as "a spawned woodcutter has no axe until
	 * {@code BotProvisioning} lands". Provisioning runs here and only here, so re-possessing an existing
	 * character never re-grants.
	 *
	 * @return true if a character file was written; false if the name is illegal or taken
	 */
	public static boolean createAccount(String name, String password, BotProfiles.Profile profile) {
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

		// The kit, skills and starting tile — the profile is the only thing that decides where a
		// character begins, so there is no second default position here to drift from it.
		BotProvisioning.provision(bot, profile == null ? BotProfiles.DEFAULT : profile);

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

	// ---- per-tick scheduling (roadmap Phase H §5.7) ------------------------------------------

	/**
	 * Starts a game tick for the bot system: advances the rotation and decides which bots may act.
	 *
	 * <p>Called once per tick by {@code Server.tick()}, before the player loop. Everything the budget
	 * does is set up here and consumed in {@link #tickTree(BotPlayer)}, so a bot never has to compute its
	 * own turn — {@link BotPlayer#treeTickAllowed} is simply read.
	 *
	 * <p><b>Why a rotating window rather than a queue.</b> The obvious budget — "tick the first K bots,
	 * resume at K next tick" — starves the tail, because the player loop always visits bots in slot order
	 * and the same low slots would win every tick. Offsetting the window by the tick number means the
	 * bots exempted this tick are a different set next tick, so every bot is reached within
	 * {@code ceil(live / budget)} ticks and none is starved. The cost is that a bot's tree runs every
	 * {@code ceil(live / budget)} ticks rather than every tick, which <em>is</em> the throughput cap the
	 * budget imposes — stated plainly rather than hidden.
	 *
	 * <p>With {@code live <= budget} (the default at {@code MAX_BOTS = 10}) every flag is true and the
	 * tick is byte-for-byte what it was before this existed.
	 */
	public static void beginTick() {
		tickNumber++;
		treeNanosThisTick = 0;
		treeTicksThisTick = 0;
		deferredThisTick = 0;
		int size = live.size();
		for (int i = 0; i < size; i++) {
			live.get(i).treeTickAllowed = inTickWindow(i, size, tickBudget, tickNumber);
		}
	}

	/**
	 * Shrinks (or restores) the live count budget. <b>Test seam only</b> — see {@link #tickBudget}.
	 *
	 * @param budget the budget to use, or a non-positive value to restore {@link #TICK_BUDGET}
	 */
	static void setTickBudgetForTesting(int budget) {
		tickBudget = budget <= 0 ? TICK_BUDGET : budget;
	}

	/**
	 * Whether bot {@code index} of {@code size} is inside this tick's window.
	 *
	 * <p>Pure and package-private so the rotation can be tested exhaustively at sizes no test could
	 * afford to build as live bots — hundreds of indices, thousands of ticks, no world or disk involved.
	 */
	static boolean inTickWindow(int index, int size, int budget, long tick) {
		if (budget <= 0 || size <= budget) {
			return true;
		}
		long start = Math.floorMod(tick * (long) budget, (long) size);
		return Math.floorMod((long) index - start, (long) size) < budget;
	}

	/**
	 * Offers one bot's behaviour tree a tick, subject to the budget. Called from
	 * {@link BotPlayer#process()} for every live bot, every game tick.
	 *
	 * <p>Declining is not an error and not silent: the bot keeps its state exactly as it was and is
	 * offered again next tick. That is the whole point — deferring a <em>decision</em> costs nothing,
	 * while overrunning the tick costs every real player on the server.
	 *
	 * <p>Both budgets are checked before the tree runs. The count budget is a flag already computed by
	 * {@link #beginTick()}. The time budget is measured from the tree work alone
	 * ({@link #treeNanosThisTick}), so engine work for real players earlier in the tick cannot consume
	 * it — and it engages <b>only when the count budget is actually exceeded</b>. While every bot fits
	 * inside the count budget there is no oversubscription for it to bound, so the tick stays fully
	 * deterministic and machine-independent; a caller that drives bots without calling
	 * {@link #beginTick()} (a test looping {@code process()}) therefore cannot be starved by an
	 * accumulator nothing is resetting. When the population does exceed the budget, the reset is
	 * guaranteed because {@code Server.tick()} calls {@code beginTick()} every tick.
	 */
	static void tickTree(BotPlayer bot) {
		if (bot == null) {
			return;
		}
		BotController controller = bot.controller();
		if (controller == null) {
			return;
		}
		boolean oversubscribed = tickBudget > 0 && live.size() > tickBudget;
		if (!bot.treeTickAllowed
				|| (oversubscribed && TICK_BUDGET_NANOS > 0 && treeNanosThisTick >= TICK_BUDGET_NANOS)) {
			deferredThisTick++;
			return;
		}
		long start = System.nanoTime();
		controller.tick();
		treeNanosThisTick += System.nanoTime() - start;
		treeTicksThisTick++;
	}

	/**
	 * What the budget did this tick: ticks performed, ticks deferred, tree nanoseconds spent.
	 *
	 * <p>Exposed because a deferred bot looks like a slow bot, and "why is it slow" should have an answer
	 * that is not a profiler. {@code ::bot list} prints it; a test asserts on it.
	 */
	public static String tickStats() {
		StringBuilder out = new StringBuilder();
		out.append(treeTicksThisTick).append(" ticked");
		if (deferredThisTick > 0) {
			out.append(", ").append(deferredThisTick).append(" deferred");
		}
		if (TICK_BUDGET_NANOS > 0) {
			out.append(", ").append(treeNanosThisTick / 1_000_000L).append("ms in trees");
		}
		return out.toString();
	}

	/** Tree ticks performed in the tick just scheduled. */
	public static int tickedLastTick() {
		return treeTicksThisTick;
	}

	/** Tree ticks the budget declined in the tick just scheduled. */
	public static int deferredLastTick() {
		return deferredThisTick;
	}

	/**
	 * Spawns every row in {@code profiles}, in order — the cohort form.
	 *
	 * <p>A cohort rather than a loop at the call site because the count that matters is "how many
	 * actually started", which only this method can compute correctly: {@link #spawn} is idempotent and
	 * returns an already-live bot, so summing its returns would report spawns that did not happen.
	 *
	 * @return the number of bots that became live as a result
	 */
	public static int spawnAll(List<BotProfile> profiles) {
		if (profiles == null || profiles.isEmpty()) {
			return 0;
		}
		int before = count();
		for (BotProfile profile : profiles) {
			spawn(profile);
		}
		return count() - before;
	}

	/**
	 * Stops every live bot — the graceful-stop path (roadmap Phase H): exit each tree with cleanup
	 * (releasing charges, animations and the {@code CycleEvent}s a leaf owns), then save the character
	 * and free its slot.
	 *
	 * <p><b>Called on shutdown, before the characters are written.</b> Without it a bot could be written
	 * mid-tree with a cycle event still scheduled against an object, and its save would claim a state it
	 * was about to leave. {@link #release} already does the ordered teardown for one bot; this is that
	 * same path for all of them, over a snapshot so the iteration is not invalidated by the removal.
	 *
	 * @return the number of bots stopped
	 */
	public static int stopAll() {
		int stopped = 0;
		for (BotPlayer bot : all()) {
			if (release(bot.playerName)) {
				stopped++;
			}
		}
		return stopped;
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
