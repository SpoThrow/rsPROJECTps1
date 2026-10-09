package server;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.Vote.MainLoader;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import server.clip.region.ObjectDef;
import server.clip.region.Region;
import server.content.skills.Implings;
import server.game.content.PlayerOwnedShop;
//import org.runetoplist.VoteChecker;
import server.event.CycleEventHandler;
import server.game.minigames.castlewars.CastleWars;
import server.game.minigames.pestcontrol.PestControl;
import server.game.minigames.trawler.Trawler;
import server.game.minigames.tzhaar.FightCaves;
import server.game.minigames.tzhaar.FightPits;
import server.game.npcs.NPCHandler;
import server.game.npcs.WorldAdventurer;
import server.game.objects.doors.Doors;
import server.game.objects.doors.DoubleDoors;
import server.game.players.Client;
import server.game.players.PlayerHandler;
import server.game.players.PlayerSaving;
import server.world.ClanManager;
import server.world.ItemHandler;
import server.world.ObjectHandler;
import server.world.ObjectManager;
import server.world.PlayerManager;
import server.world.ShopHandler;
import server.world.StillGraphicsManager;
import server.world.definitions.EntityDef;
import core.net.ConnectionHandler;
import core.net.ConnectionThrottleFilter;
import core.util.SimpleTimer;
import core.util.log.Logger;

/**
 * Server.java
 *
 * @author Sanity
 * @author Graham
 * @author Blake
 * @author Ryan Lmctruck30
 *
 */

public class Server {
	
	public static PlayerManager playerManager = null;
	private static StillGraphicsManager stillGraphicsManager = null;
	
	public static boolean sleeping;
	public static final int cycleRate;
	public static boolean UpdateServer = false;
	private static IoAcceptor acceptor;
	private static ConnectionHandler connectionHandler;
	private static ConnectionThrottleFilter throttleFilter;
	private static SimpleTimer engineTimer, debugTimer;
	private static long cycleTime, cycles, totalCycleTime;
	/**
	 * How much of the current tick's 600&nbsp;ms budget was left unused — negative when the tick
	 * overran. Kept because {@link #getSleepTimer()} exposes it and the tick deliberately retains
	 * the old loop's negative value (see {@link #runTick()}), so it has to keep meaning "the unused
	 * part of the current tick".
	 */
	private static volatile long sleepTime;
	private static DecimalFormat debugPercentFormat;
	public static boolean shutdownServer = false;		
	public static int serverlistenerPort;
	public static ItemHandler itemHandler = new ItemHandler();
	public static PlayerHandler playerHandler = new PlayerHandler();
    public static NPCHandler npcHandler = new NPCHandler();
	public static ShopHandler shopHandler = new ShopHandler();
	public static ObjectHandler objectHandler = new ObjectHandler();
	public static ObjectManager objectManager = new ObjectManager();
	public static ClanManager clanManager = new ClanManager();
	//castlewars
	public static CastleWars castleWars = new CastleWars();
	public static FightPits fightPits = new FightPits();
	public static PestControl pestControl = new PestControl();
	public static Trawler trawler = new Trawler();
	public static FightCaves fightCaves = new FightCaves();
	public static MainLoader vote;

	static {
		java.io.PrintStream previousOut = System.out;
		java.io.PrintStream previousErr = System.err;
		java.io.ByteArrayOutputStream captured = new java.io.ByteArrayOutputStream();
		java.io.PrintStream sink = new java.io.PrintStream(captured);
		try {
			System.setOut(sink);
			System.setErr(sink);
			vote = new MainLoader(
					Configuration.get().getString("vote.host", "localhost"),
					Configuration.get().getString("vote.user", "root"),
					Configuration.get().getString("vote.password", "------"),
					Configuration.get().getString("vote.database", "vote"));
		} catch (Throwable t) {
			vote = null;
		} finally {
			System.setOut(previousOut);
			System.setErr(previousErr);
		}
		String voteLog = captured.toString();
		if (voteLog.indexOf("Error connecting") >= 0 || voteLog.indexOf("CommunicationsException") >= 0
				|| voteLog.indexOf("Communications link failure") >= 0) {
			// The loader's failure noise is deliberately suppressed, but Configuration.load() logs
			// from inside this hijacked region, so its line would be swallowed with the noise —
			// and a missing or unreadable server.properties is exactly what an operator needs to
			// see when the database refuses to connect. Surface our own line before the summary.
			for (String line : voteLog.split("\\r?\\n")) {
				if (line.startsWith("[Configuration]")) {
					System.out.println(line);
				}
			}
			System.out.println("[GTLVote] MySQL is not running on "
					+ Configuration.get().getString("vote.host", "localhost")
					+ "; vote claims are disabled until the database is up.");
		} else if (voteLog.length() > 0) {
			System.out.print(voteLog);
		}
		if(!Config.SERVER_DEBUG) {
			serverlistenerPort = 43594;
		} else {
			serverlistenerPort = 43594;
		}
		cycleRate = 600;
		shutdownServer = false;
		engineTimer = new SimpleTimer();
		debugTimer = new SimpleTimer();
		sleepTime = 0;
		debugPercentFormat = new DecimalFormat("0.0#%");
	}
	
	public static void main(java.lang.String args[]) throws NullPointerException, IOException {
		/**
		 * Starting Up Server
		 */
		ObjectDef.loadConfig();
		Region.load();
		Config.loadConfigurations();
		//Highscores.process();
		System.setOut(new Logger(System.out));
		System.setErr(new Logger(System.err));
		Implings.spawnImplings();
		EntityDef.unpackConfig();
		PlayerOwnedShop.loadShops();
		System.out.println("[Stage 1] NPC drops have been loaded...");
		System.out.println("[Stage 2] NPC spawns have been loaded...");
		System.out.println("[Stage 3] Shops have been loaded...");
		System.out.println("[Stage 4] Object spawns have been loaded...");
		System.out.println("[Stage 5] Teleport and spawn points have been loaded...");
		System.out.println("[Stage 6] Player Owned Shops have been loaded...");
		System.out.println("[Stage 7] Connections are now being accepted...");
		
		/**
		 * Accepting Connections
		 */
		acceptor = new SocketAcceptor();
		connectionHandler = new ConnectionHandler();
		
		playerManager = PlayerManager.getSingleton();
		playerManager.setupRegionPlayers();
		stillGraphicsManager = new StillGraphicsManager();
		SocketAcceptorConfig sac = new SocketAcceptorConfig();
		sac.getSessionConfig().setTcpNoDelay(false);
		sac.setReuseAddress(true);
		sac.setBacklog(100);
		
		throttleFilter = new ConnectionThrottleFilter(Config.CONNECTION_DELAY);
		sac.getFilterChain().addFirst("throttleFilter", throttleFilter);
		acceptor.bind(new InetSocketAddress(serverlistenerPort), connectionHandler, sac);

		/**
		 * Initialise Handlers
		 */
		//EventManager.initialize();
		Doors.getSingleton().load();
		DoubleDoors.getSingleton().load();
		Connection.initialize();
		// No-op while Config.WORLD_ADVENTURER_ENABLED is false; the gate lives in spawn() so the
		// ::max teleport's lazy re-spawn cannot bypass it.
		WorldAdventurer.spawn();
		// The one startup call the bot subsystem gets (roadmap Phase E). With no Data/cfg/bots.cfg
		// this does nothing, so deleting the whole bot package leaves the boot path untouched.
		server.game.bots.BotManager.start();
		
		/**
		 * Server Successfully Loaded 
		 */
		System.out.println("[Final Stage] " + Config.SERVER_NAME + " has been launched on localhost:" + serverlistenerPort + "...");

		/**
		 * Main Server Tick
		 *
		 * The tick is owned by the scheduler below rather than by this method: main used to run a
		 * {@code while (!shutdownServer) { sleep(600 - elapsed); ... }} loop, which meant the game
		 * loop and the JVM's own lifecycle were the same thread and there was nowhere to hook a
		 * clean stop. See {@link #startTicker()}.
		 */
		registerShutdownHook();
		startTicker();
		// main returns here on purpose. The tick thread is non-daemon, so it is what keeps the JVM
		// alive; nothing below this point would ever run.
	}

	/**
	 * The thread that runs {@link #tick()}. Single-threaded on purpose: the entire game — every
	 * player, NPC, shop and cycle event — is single-threaded state, and that is the property that
	 * makes the rest of the server safe to reason about.
	 */
	private static volatile ScheduledExecutorService ticker;

	/**
	 * Starts the game loop on a dedicated scheduler.
	 *
	 * <p>The timing is the same as the loop this replaced: each tick re-schedules the next for
	 * whatever is left of the 600&nbsp;ms budget, so the start-to-start period is 600&nbsp;ms while
	 * the server keeps up, and <em>work + 600</em> if a tick overruns — the old loop's
	 * {@code if (sleepTime >= 0) sleep(sleepTime); else sleep(cycleRate);}. That is deliberately
	 * <em>not</em> a catch-up scheduler: {@code scheduleAtFixedRate} queues a burst of ticks to
	 * make up for an overrun, which for a game loop turns a momentary stall into a cascade.
	 */
	private static void startTicker() {
		ticker = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread thread = new Thread(r, "game-tick");
			thread.setDaemon(false);
			return thread;
		});
		scheduleTick(0);
	}

	/** Queues the next tick, unless a stop has already been requested or the scheduler is down. */
	private static void scheduleTick(long delayMillis) {
		ScheduledExecutorService current = ticker;
		if (shutdownServer || current == null || current.isShutdown()) {
			return;
		}
		current.schedule(Server::runTick, delayMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * One iteration of the loop: measure the tick, book-keep, and queue the next one.
	 *
	 * <p>{@link Throwable} is caught here and nowhere else. Because each tick schedules the next
	 * one, an exception escaping this method would stop the game loop silently and permanently —
	 * the server would look alive, still accept logins, and never process a packet. Per-player
	 * errors are already isolated in {@link PlayerHandler#process()}; anything that reaches this
	 * handler is a handler-level fault, which is worth logging loudly and surviving rather than
	 * dying on.
	 */
	private static void runTick() {
		engineTimer.reset();
		try {
			tick();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		cycleTime = engineTimer.elapsed();
		// Faithful to the loop this replaced, *including* its overrun handling:
		//     if (sleepTime >= 0) sleep(sleepTime); else sleep(cycleRate);
		// so a tick that overruns its budget waits a full cycle rather than firing immediately to
		// catch up. Clamping to zero here instead would reintroduce exactly the catch-up burst
		// scheduleAtFixedRate was rejected for. `sleepTime` itself is left possibly negative
		// because getSleepTimer() exposes it as "how much of this tick was left".
		sleepTime = cycleRate - cycleTime;
		totalCycleTime += cycleTime;
		cycles++;
		debug();
		scheduleTick(sleepTime >= 0 ? sleepTime : cycleRate);
	}

	/**
	 * One game tick.
	 *
	 * <p>The handler order is load-bearing and is stated here once. Players run before NPCs so that
	 * a player's movement and attack this tick are what the NPC AI reacts to, cycle events run after
	 * both so a timer they scheduled can see the result of this tick, and the object/graphics
	 * handlers run last because they only publish state for the next tick's update.
	 *
	 * <p>{@link PlayerSaving#process()} runs at the very end, after every handler has settled this
	 * tick's state, and writes at most one character, so the autosave can never observe a player
	 * mid-update within a tick.
	 */
	private static void tick() {
		itemHandler.process();
		playerHandler.process();	
        npcHandler.process();
		shopHandler.process();
		CycleEventHandler.process();
		server.game.content.DwarfCannon.process();
		objectManager.process();
		//castlewars
		CastleWars.process();
		fightPits.process();
		pestControl.process();
		PlayerSaving.process();
	}

	/**
	 * Installs the one lifecycle that stops the server.
	 *
	 * <p>Before Phase 5 there was no registered hook at all: two {@code ShutdownHook} classes
	 * existed (one package-private to {@code server}, one in {@code core.util}) and neither was
	 * ever added to the runtime — the only reference to either was a commented-out line in
	 * {@code main}. So Ctrl+C, a service restart or a JVM kill discarded every character change
	 * since that player last logged out, and a login sitting in the world for hours would roll
	 * back to nothing.
	 */
	private static void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(Server::requestStop, "server-shutdown"));
	}

	/**
	 * The whole stop sequence: stop accepting new ticks, let the tick in flight finish, then write
	 * the characters.
	 *
	 * <p>Waiting for the in-flight tick is what makes the save safe — this runs on the hook thread,
	 * and a save that overlapped a tick could read a player mid-mutation. Package-private, and
	 * separate from {@link #registerShutdownHook()}, so the sequence is reachable from a test
	 * without the JVM actually being on its way down.
	 *
	 * <p>The JVM calls this once. It does not need a guard of its own because the property that
	 * matters — never writing the same character twice — lives in {@link
	 * Client#saveCharacterOnce()}, which is also what makes it safe to run after a logout has
	 * already written someone.
	 */
	static void requestStop() {
		shutdownServer = true;
		stopTicker();
		int saved = PlayerHandler.saveAllPlayers();
		System.out.println("[Shutdown] Saved " + saved + " character(s).");
	}

	/** Stops the ticker and waits for the in-flight tick to finish. */
	private static void stopTicker() {
		ScheduledExecutorService current = ticker;
		if (current == null) {
			return;
		}
		current.shutdown();
		try {
			if (!current.awaitTermination(10, TimeUnit.SECONDS)) {
				current.shutdownNow();
			}
		} catch (InterruptedException e) {
			current.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	public static boolean playerExecuted = false;
	private static void debug() {
		if (debugTimer.elapsed() > 360*1000 || playerExecuted) {
			long averageCycleTime = totalCycleTime / cycles;
			double engineLoad = ((double) averageCycleTime / (double) cycleRate);
			System.out.println("Currently online: " + PlayerHandler.playerCount+ ", engine load: "+ debugPercentFormat.format(engineLoad));
			totalCycleTime = 0;
			cycles = 0;
			System.gc();
			System.runFinalization();
			debugTimer.reset();
			playerExecuted = false;
		}
	}
	
	public static long getSleepTimer() {
		return sleepTime;
	}
	
	public static StillGraphicsManager getStillGraphicsManager() {
		return stillGraphicsManager;
	}
	
	public static PlayerManager getPlayerManager() {
		return playerManager;
	}
	
	public static ObjectManager getObjectManager() {
		return objectManager;
	}
	
}
