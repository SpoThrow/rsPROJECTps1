package server;

import core.util.Misc;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class Config {

	public static final boolean SERVER_DEBUG = false;

	/** When true, CombatAssistant.attack* dispatches via CombatStrategy (default on). */
	public static boolean USE_COMBAT_STRATEGY = true;
	
	public static final String SERVER_NAME = "Soul-Trail";
	public static final String WELCOME_MESSAGE = "Welcome to Soul-Trail.";
	public static final String FORUMS = "soul-trail.com";
	
	public static final int CLIENT_VERSION = 1;
	public static boolean sendServerPackets = false;
	
	public static int MESSAGE_DELAY = 6000;
	public static final int ITEM_LIMIT = 25000;
	public static final int MAXITEM_AMOUNT = Integer.MAX_VALUE;
	public static final int BANK_SIZE = 1000;
	public static final int MAX_PLAYERS = 50;
	
	public static final int CONNECTION_DELAY = 50;
	public static final int IPS_ALLOWED = 3;
		
	public static final boolean WORLD_LIST_FIX = false;
	
	public static final int[] ITEM_SELLABLE 		=	{4447,2528,3842,3844,3840,8844,8845,8846,8847,8848,8849,8850,10551,6570,7462,7461,7460,7459,7458,7457,7456,7455,7454,8839,8840,8842,11663,11664,11665,10499,
														9748,9754,9751,9769,9757,9760,9763,9802,9808,9784,9799,9805,9781,9796,9793,9775,9772,9778,9787,9811,9766,
														9749,9755,9752,9770,9758,9761,9764,9803,9809,9785,9800,9806,9782,9797,9794,9776,9773,9779,9788,9812,9767,
														9747,9753,9750,9768,9756,9759,9762,9801,9807,9783,9798,9804,9780,9795,9792,9774,9771,9777,9786,9810,9765,995};
	public static final int[] ITEM_TRADEABLE 		= 	{4447,2528,3842,3844,3840,8844,8845,8846,8847,8848,8849,8850,10551,6570,7462,7461,7460,7459,7458,7457,7456,7455,7454,8839,8840,8842,11663,11664,11665,10499,
														9748,9754,9751,9769,9757,9760,9763,9802,9808,9784,9799,9805,9781,9796,9793,9775,9772,9778,9787,9811,9766,
														9749,9755,9752,9770,9758,9761,9764,9803,9809,9785,9800,9806,9782,9797,9794,9776,9773,9779,9788,9812,9767,
														9747,9753,9750,9768,9756,9759,9762,9801,9807,9783,9798,9804,9780,9795,9792,9774,9771,9777,9786,9810,9765,995};
	public static final int[] UNDROPPABLE_ITEMS 	= 	{4447,6570};
	
	 public static final int[] WEBS_CANNOT = { 9185, 839, 845, 847, 851, 855,
			859, 841, 843, 849, 853, 857, 861, 4212, 4214, 4215, 11235, 4216,
			4217, 4218, 4219, 4220, 4221, 4222, 4223, 6724, 4734, 4934, 4935,
			4936, 4937, 1379, 1381, 1383, 1385, 1387, 1389 };
	
	public static final int[] FUN_WEAPONS	=	{2460,2461,2462,2463,2464,2465,2466,2467,2468,2469,2470,2471,2471,2473,2474,2475,2476,2477};
	
	public static final boolean ADMIN_CAN_TRADE = true;
	public static final boolean ADMIN_CAN_SELL_ITEMS = true;
	public static final boolean ADMIN_DROP_ITEMS = true;
	
	// Spawn points - loaded from config file
	public static int START_LOCATION_X = 3087;
	public static int START_LOCATION_Y = 3505;
	public static int RESPAWN_X = 3221 + Misc.random(2);
	public static int RESPAWN_Y = 3218 + Misc.random(1);
	public static int DUELING_RESPAWN_X = 3362;
	public static int DUELING_RESPAWN_Y = 3263;
	public static final int RANDOM_DUELING_RESPAWN = 5;
	
	// Teleport locations - loaded from config file
	public static Map<String, int[]> teleportLocations = new HashMap<>();
	
	public static final int NO_TELEPORT_WILD_LEVEL = 20;
	public static final int SKULL_TIMER = 1200;
	public static final int TELEBLOCK_DELAY = 20000;
	public static final boolean SINGLE_AND_MULTI_ZONES = true;
	public static final boolean COMBAT_LEVEL_DIFFERENCE = true;
	
	public static final boolean itemRequirements = true;
		
	public static final int MELEE_EXP_RATE = 600;
	public static final int RANGE_EXP_RATE = 575;
	public static final int MAGIC_EXP_RATE = 550;
	
	/**
	 * Turn this into a double, because integers can't contain float/decimal
	 * values without having to truncate them to the nearest whole number
	 */
	public static double SERVER_EXP_BONUS = 1;
	
	/**
	 * Server messages
	 */
	public static int LAST_MESSAGE = 0;
	
	public static final int INCREASE_SPECIAL_AMOUNT = 14500;
	public static final boolean PRAYER_POINTS_REQUIRED = true;
	public static final boolean PRAYER_LEVEL_REQUIRED = true;
	public static final boolean MAGIC_LEVEL_REQUIRED = true;
	public static final int GOD_SPELL_CHARGE = 300000;
	public static final boolean RUNES_REQUIRED = true;
	public static final boolean CORRECT_ARROWS = true;
	public static final boolean CRYSTAL_BOW_DEGRADES = true;
	
	public static final int SAVE_TIMER = 10;
	public static final int NPC_RANDOM_WALK_DISTANCE = 6;
	public static final int NPC_FOLLOW_DISTANCE = 10;												

	/**
	 * How NPCs chase a player.
	 *
	 * <p>{@code false} (default) = authentic OSRS: step greedily toward the target and let
	 * {@code NPCHandler.handleClipping} slide or stop the NPC. This deliberately lets NPCs snag on
	 * fences, trees and wall corners, which is what makes safespotting work.
	 *
	 * <p>{@code true} = route with {@code SmartPathFinder} so NPCs walk around obstacles and reliably
	 * reach the player. More "correct" AI, but it removes most safespots.
	 */
	public static final boolean NPC_SMART_PATHING = false;

	/**
	 * What {@code Region.getClipping} returns for a tile whose collision data we do not have
	 * (the region is absent from {@code map_index}, its map file is missing, or its clip
	 * lookup threw).
	 *
	 * <p>{@code false} (default) = legacy behaviour, {@code 0} = walkable. Terrain the server
	 * has no data for stays passable. This is the safe default because the server's map is
	 * missing regions that the client still renders — most visibly the ground around the Nex
	 * arena, which is bordered by eleven regions with no map data — so blocking them would
	 * freeze players mid-fight.
	 *
	 * <p>{@code true} = fail closed, returning {@link server.clip.region.Region#blockedValue()}.
	 * A tile we know nothing about blocks walking, standing and projectiles instead of becoming
	 * a hole to walk through. Correct in principle, but it changes traversal anywhere the data
	 * is absent, so it needs a live pass before being left on.
	 */
	public static final boolean REGION_FAIL_CLOSED = false;

	/**
	 * Use {@code Data/objectSize.cfg} for object collision footprints instead of the cache's
	 * {@code loc.dat} sizes.
	 *
	 * <p>Must stay in sync with the client, which builds collision from {@code loc.dat} in
	 * {@code Class11.method212}. With this on and the client unpatched the two disagree about ~2229
	 * objects, so the client can predict a step the server refuses (visible as a stutter near scenery).
	 * Set to {@code false} for the previous 1x1-ish behaviour.
	 */
	public static final boolean USE_OBJECT_SIZE_TABLE = true;

	/**
	 * Whether the wandering world NPC ("Max", {@code WorldAdventurer}) spawns.
	 *
	 * <p>{@code false} (default) = no Max. He is a hand-rolled travel/work state machine written
	 * before the behaviour-tree bot system existed, and with bots available the same job is better
	 * done by a script (roadmap Phase G). Until that script exists, off is the honest default: a
	 * single NPC that force-chats at players and teleports itself is not something to leave running
	 * in a live world by accident.
	 *
	 * <p>{@code true} = the pre-bot behaviour, exactly as before. Nothing else has to change to flip
	 * it: {@code WorldAdventurer.spawn()} is the only entry point, so every path in — boot, the
	 * {@code ::max} teleport, and the lazy re-spawn that teleport does — is gated by this one flag.
	 */
	public static final boolean WORLD_ADVENTURER_ENABLED = false;

	/**
	 * How many bots may have their behaviour tree ticked in one game tick — the per-tick work budget
	 * ({@code BOT_ROADMAP.md} §5.7, Phase H).
	 *
	 * <p>The game tick is single-threaded and every bot competes with real players for the same
	 * {@link #cycleRate} ms, so the scoring rule for the bot system is "does this keep per-bot, per-tick
	 * work bounded". This is that bound. When more bots are live than this, the ticks are <b>staggered</b>:
	 * a rotating window of {@code BOT_TICK_BUDGET} bots ticks each game tick and the rest resume next
	 * tick, so every bot is reached within {@code ceil(bots / budget)} ticks.
	 *
	 * <p><b>Deliberately larger than {@code BotManager.MAX_BOTS}.</b> At the current cap the budget never
	 * bites — every bot ticks every tick, exactly as before this existed — because there is no reason to
	 * slow down ten bots. It is a ceiling for the case the cap is raised, not a throttle on the present
	 * one. {@code 0} or less disables it entirely.
	 */
	public static final int BOT_TICK_BUDGET = 32;

	/**
	 * A wall-clock ceiling, in milliseconds, on time spent <em>inside bot behaviour trees</em> in one game
	 * tick — the safety valve above {@link #BOT_TICK_BUDGET}.
	 *
	 * <p>The count budget bounds how many bots act; this bounds how long acting may take, which is the
	 * thing that actually protects the tick when one bot's work is expensive (a pathfind, a region scan).
	 * It counts tree time only, not the surrounding engine work, so it cannot be consumed by real players
	 * processed earlier in the same tick. {@code 0} or less disables it.
	 *
	 * <p>Unlike the count budget this makes behaviour machine-dependent: a slower host ticks fewer bots
	 * per tick. That is the intended reading of a <em>safety valve</em> — degrade bot throughput rather
	 * than overrun the tick — and it is why the deterministic count bound is the primary mechanism and
	 * this is the backstop.
	 */
	public static final int BOT_TICK_BUDGET_MS = 40;

	/**
	 * The loopback port the bot status endpoint listens on, or {@code 0} for no endpoint.
	 *
	 * <p><b>It is on at {@code 8081} so that the workshop's Live bots panel works out of the box</b>, which
	 * is the one route in the tool that needs two processes running at once and the only one that cannot be
	 * answered from a file. Set it back to {@code 0} to remove the socket entirely — the whole endpoint is
	 * one guarded call in {@code Server.main}, so nothing else here is load-bearing and the feature can be
	 * deleted without a trace ({@code BOT_TOOLING.md} §11).
	 *
	 * <p><b>Read-only, and loopback-only.</b> It answers {@code GET /live/bots} with what the live bots
	 * are doing — position, current state path, last failure ({@code BOT_ROADMAP.md} Phase F) — and
	 * nothing else: no possess, no despawn, no step. It binds to {@code 127.0.0.1} only, so even with it
	 * on, nothing off the machine can reach it, which is what makes a debugging endpoint acceptable to
	 * leave enabled in a live world.
	 *
	 * <p><b>The port matches the tool's default.</b> {@code LiveProxy.DEFAULT_PORT} and the
	 * {@code botStatusPort} property {@code workshopServe} sets are both {@code 8081}, so watching a bot
	 * needs no argument on either side. Changing this one means passing {@code -PbotStatusPort=} to the
	 * workshop as well.
	 *
	 * <p><b>What it deliberately does not do yet.</b> {@code BOT_TOOLING.md} Stage T7 also sketches
	 * possess/release and pause/step. Those are writes, they change a real character, and a runaway one
	 * would be a way to drive bots without the {@code ::bot} command's gating — so they are not built
	 * until the read-only half has been used enough to say what they should do.
	 */
	public static final int BOT_STATUS_PORT = 8081;

	public static final int[] UNDEAD_NPCS = {90,91,92,93,94,103,104,73,74,75,76,77};
	
	public static final int ATTACK = 0;
	public static final int DEFENCE = 1;
	public static final int STRENGTH = 2;
	public static final int HITPOINTS = 3;
	public static final int RANGED = 4;
	public static final int PRAYER = 5;
	public static final int MAGIC = 6;
	public static final int COOKING = 7;
	public static final int WOODCUTTING = 8;
	public static final int FLETCHING = 9;
	public static final int FISHING = 10;
	public static final int FIREMAKING = 11;
	public static final int CRAFTING = 12;
	public static final int SMITHING = 13;
	public static final int MINING = 14;
	public static final int HERBLORE = 15;
	public static final int AGILITY = 16;
	public static final int THIEVING = 17;
	public static final int SLAYER = 18;
	public static final int FARMING = 19;
	public static final int RUNECRAFTING = 20;
	public static final int CONSTRUCTION = 21;
	public static final int HUNTER = 22;

	/**
	 * Glory - kept for backward compatibility
	 */
	public static final int EDGEVILLE_X = 3087;
	public static final int EDGEVILLE_Y = 3500;
	public static final String EDGEVILLE = "";
	public static final int AL_KHARID_X = 3293;
	public static final int AL_KHARID_Y = 3174;
	public static final String AL_KHARID = "";
	public static final int KARAMJA_X = 3087;
	public static final int KARAMJA_Y = 3500;
	public static final String KARAMJA = "";
	public static final int MAGEBANK_X = 2538;
	public static final int MAGEBANK_Y = 4716;
	public static final String MAGEBANK = "";
	
	/**
	* Teleport Spells - kept for backward compatibility, use teleportLocations map instead
	**/
	// modern
	public static final int VARROCK_X = 3210;
	public static final int VARROCK_Y = 3424;
	public static final String VARROCK = "";
	public static final int LUMBY_X = 3222;
	public static final int LUMBY_Y = 3218;
	public static final String LUMBY = "";
    public static final int FALADOR_X = 2964;
	public static final int FALADOR_Y = 3378;
	public static final String FALADOR = "";
	public static final int CAMELOT_X = 2757;
	public static final int CAMELOT_Y = 3477;
	public static final String CAMELOT = "";
	public static final int CATHERBY_X = 2804;
	public static final int CATHERBY_Y = 3433;
	public static final String CATHERBY = "";
	public static final int ARDOUGNE_X = 2662;
	public static final int ARDOUGNE_Y = 3305;
	public static final String ARDOUGNE = "";
	public static final int WHITE_WOLF_MOUNTAIN_X = 2848;
	public static final int WHITE_WOLF_MOUNTAIN_Y = 3498;
	public static final String WHITE_WOLF_MOUTAIN = "";
	public static final int TROLLHEIM_X = 3243;
	public static final int TROLLHEIM_Y = 3513;
	public static final String TROLLHEIM = "";
	public static final int APE_ATOLL_X = 2762;
	public static final int APE_ATOLL_Y = 2784;
	public static final String APE_ATOLL = "";
	
	// ancient
	
	public static final int PADDEWWA_X = 3098;
	public static final int PADDEWWA_Y = 9884;
	
	public static final int SENNTISTEN_X = 3322;
	public static final int SENNTISTEN_Y = 3336;

    public static final int KHARYRLL_X = 3492;
	public static final int KHARYRLL_Y = 3471;

	public static final int LASSAR_X = 3006;
	public static final int LASSAR_Y = 3471;
	
	public static final int DAREEYAK_X = 3161;
	public static final int DAREEYAK_Y = 3671;
	
	public static final int CARRALLANGAR_X = 3156;
	public static final int CARRALLANGAR_Y = 3666;
	
	public static final int ANNAKARL_X = 3288;
	public static final int ANNAKARL_Y = 3886;
	
	public static final int GHORROCK_X = 2977;
	public static final int GHORROCK_Y = 3873;
 
	public static final int TIMEOUT = 20;
	public static final int CYCLE_TIME = 600;
	public static final int BUFFER_SIZE = 10000;
	
	/**
	 * Slayer Variables
	 */
	public static final int[][] SLAYER_TASKS = {{1,87,90,4,5}, 
												{6,7,8,9,10},
												{11,12,13,14,15},
												{1,1,15,20,25},
												{30,35,40,45,50},
												{60,75,80,85,90}};
	
	/**
	* Skill Experience Multipliers
	*/	
	public static final int WOODCUTTING_EXPERIENCE = 15; //9
	public static final int MINING_EXPERIENCE = 16; //10
	public static final int SMITHING_EXPERIENCE = 16; //10
	public static final int FARMING_EXPERIENCE = 17; //11
	public static final int FIREMAKING_EXPERIENCE = 14; //8
	public static final int HERBLORE_EXPERIENCE = 16;//10
	public static final int FISHING_EXPERIENCE = 15; //7
	public static final int AGILITY_EXPERIENCE = 18;//16
	public static final int PRAYER_EXPERIENCE = 18;//15
	public static final int RUNECRAFTING_EXPERIENCE = 15;//10
	public static final int CRAFTING_EXPERIENCE = 16;//9
	public static final int THIEVING_EXPERIENCE = 15;//8
	public static final int SLAYER_EXPERIENCE = 19;//15
	public static final int COOKING_EXPERIENCE = 18;//8
	public static final int FLETCHING_EXPERIENCE = 15;//10
	public static final int CONSTRUCTION_EXPERIENCE = 12;//10
	public static final int HUNTER_EXPERIENCE = 12;//10
	
	/**
	 * Load configuration files
	 */
	public static void loadConfigurations() {
		loadSpawnPoints();
		loadTeleports();
	}
	
	private static void loadSpawnPoints() {
		try (BufferedReader reader = new BufferedReader(new FileReader("./Data/cfg/spawn-points.cfg"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("spawn = ")) {
					String[] parts = line.split("\\s+");
					if (parts.length >= 5) {
						try {
							String name = parts[2].replace("_", " ").toLowerCase();
							int x = Integer.parseInt(parts[3]);
							int y = Integer.parseInt(parts[4]);
							
							if (name.equals("start_location")) {
								START_LOCATION_X = x;
								START_LOCATION_Y = y;
							} else if (name.equals("respawn")) {
								RESPAWN_X = x;
								RESPAWN_Y = y;
							} else if (name.equals("dueling_respawn")) {
								DUELING_RESPAWN_X = x;
								DUELING_RESPAWN_Y = y;
							}
						} catch (NumberFormatException e) {
							// Skip invalid lines
						}
					}
				}
			}
		} catch (IOException e) {
			System.out.println("Could not load spawn points: " + e.getMessage());
		}
	}
	
	private static void loadTeleports() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./Data/cfg/teleports.cfg"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("teleport = ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 6) {
                        try {
                            String category = parts[2].replace("_", " ");
                            String name = parts[3].replace("_", " ");
                            int x = Integer.parseInt(parts[4]);
                            int y = Integer.parseInt(parts[5]);
                            // Store with category prefix for easy lookup
                            teleportLocations.put(category + " " + name, new int[]{x, y});
                            // Also store with just name for backward compatibility
                            teleportLocations.put(name, new int[]{x, y});
                        } catch (NumberFormatException e) {
                            // Skip invalid lines
                        }
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("Could not load teleports: " + e.getMessage());
        }
    }
	
	/**
	 * Get teleport coordinates by name
	 */
	public static int[] getTeleportLocation(String name) {
		return teleportLocations.get(name.toLowerCase());
	}
}
