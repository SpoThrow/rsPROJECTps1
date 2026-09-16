package server;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.DecimalFormat;

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
import server.game.objects.doors.Doors;
import server.game.objects.doors.DoubleDoors;
import server.game.players.Client;
import server.game.players.Player;
import server.game.players.PlayerHandler;
import server.game.players.PlayerSave;
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
 * @author Acquittal
 *
 */

public class Server {
	
	public static PlayerManager playerManager = null;
	private static StillGraphicsManager stillGraphicsManager = null;
	
	public static boolean sleeping;
	public static final int cycleRate;
	public static boolean UpdateServer = false;
	public static long lastMassSave = System.currentTimeMillis();
	private static IoAcceptor acceptor;
	private static ConnectionHandler connectionHandler;
	private static ConnectionThrottleFilter throttleFilter;
	private static SimpleTimer engineTimer, debugTimer;
	private static long cycleTime, cycles, totalCycleTime, sleepTime;
	private static DecimalFormat debugPercentFormat;
	public static boolean shutdownServer = false;		
	public static boolean shutdownClientHandler;			
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
	//public static ClanChatHandler clanChat = new ClanChatHandler();
	public static FightCaves fightCaves = new FightCaves();
	public static MainLoader vote = new MainLoader("localhost", "root", "------", "vote");
	
	static {
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
		//ShutdownHook.getSingleton().run();
		//WalkingCheck.load();
		//Highscores.process();
		/*if (Highscores.connected) {
			System.out.println("Connected to MySQL Database 'highscores'!");
		} else {
			System.out.println("Failed to connect to MySQL Database 'highscores'!");
		}*/
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
		
		/**
		 * Server Successfully Loaded 
		 */
		System.out.println("[Final Stage] Acquittal has been launched on localhost:" + serverlistenerPort + "...");
		/**
		 * Main Server Tick
		 */
		try {
			while (!Server.shutdownServer) {
				if (sleepTime >= 0)
					Thread.sleep(sleepTime);
				else
					Thread.sleep(600);
				engineTimer.reset();
				itemHandler.process();
				playerHandler.process();	
	            npcHandler.process();
				shopHandler.process();
				CycleEventHandler.process();
				objectManager.process();
				//castlewars
				CastleWars.process();
				fightPits.process();
				pestControl.process();
				cycleTime = engineTimer.elapsed();
				sleepTime = cycleRate - cycleTime;
				totalCycleTime += cycleTime;
				cycles++;
				debug();
				/*if (System.currentTimeMillis() - lastMassSave > 3) {
					for(Player p : PlayerHandler.players) {
						if(p == null)
							continue;						
						PlayerSave.saveGame((Client)p);
						lastMassSave = System.currentTimeMillis();
					}
				
				}*/
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("A fatal exception has been thrown!");
			for(Player p : PlayerHandler.players) {
				if(p == null)
					continue;						
				PlayerSave.saveGame((Client)p);
			}
		}
		acceptor = null;
		connectionHandler = null;
		sac = null;
		System.exit(0);
	}
	
	public static void processAllPackets() {
		for (int j = 0; j < PlayerHandler.players.length; j++) {
			if (PlayerHandler.players[j] != null) {
				while(PlayerHandler.players[j].processQueuedPackets());			
			}	
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
