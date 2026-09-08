package server.content.skills;

import java.util.HashMap;
import java.util.Random;

import server.Config;
import server.Server;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.npcs.NPC;
import server.game.npcs.NPCHandler;
import server.game.players.Client;
import core.util.Misc;

public class Implings {

	public static final Random random = new Random();


	/**
	 * 
	 * Imp rewards
	 * @author Emre
	 */
	public enum ImpRewards {
		BABY(11238, new int[][] { {1755,1}, {1734,1}, {1733, 1}, {946,1}, {1985,1}, {2347,1}, {1759,1}, {1927,1}, {319,1}, {2007,1}, {1779,1}, {7170,1}, {1438,1}, {2355,1}, {1607,1}, {1743,1}, {379,1}, {1761,1} }),
		YOUNG(11240, new int[][] { {361,1}, {1902,1}, {1539,5}, {1524,1}, {7936,1}, {855,1}, {1353,1}, {2293,1}, {7178,1}, {247,1}, {453,1}, {1777,1}, {231,1}, {1761,1}, {8778,1}, {133,1}, {2359,1} }),
		GOURMENT(11242, new int[][] { {365,1}, {361,1}, {2011,1}, {1897,1}, {2327,1}, {2007,1}, {5970,1}, {380,4}, {7179, 1, 5}, {386,3}, {1883,1}, {3145, 2}, {5755,1}, {5406,1}, {10137, 5} }),
		EARTH(11244, new int[][] { {6033,6}, {1440,1}, {5535, 1}, {557, 32}, {1442,1}, {1784,4}, {1273,1}, {447,1}, {1606,2} }),
		ESSENCE(11246, new int[][] { {7936,20}, {555,30}, {556,30}, {558,25},{559,28}, {562, 4}, {1448, 1}, {564, 4}, {563, 13}, {565, 7}, {566, 11} }),
		ECLECTIC(11248, new int[][] { {1273,1}, {5970,1}, {231,1}, {556, 30, 47}, {8779, 4}, {1199,1}, {4527,1}, {444,1}, {2358, 5}, {7937, 20, 35}, {237,1}, {2493,1}, {10083,1}, {1213,1}, {450, 10}, {5760, 2}, {7208,1}, {5321, 3}, {1391, 1}, {1601,1} }),
		NATURE(11250, new int[][] { {5100,1}, {5104, 1}, {5281,1}, {5294,1}, {6016,1}, {1513,1}, {254, 4}, {5313,1}, {5286,1}, {5285, 1}, {3000,1}, {5974,1}, {5297,1}, {5299,1}, {5298, 5}, {5304,1}, {5295, 1}, {270,2}, {5303,1} }),
		MAGPIE(11252, new int[][] { {1701,3}, {1732, 3}, {2569,3}, {3391,1}, {4097,1}, {5541,1}, {1747, 6}, {1347,1}, {2571, 4}, {4095, 1}, {2364, 2}, {1215, 1}, {1185, 1}, {1602, 4}, {5287, 1}, {987,1}, {985,1}, {993,1}, {5300,1} } ),
		NINJA(11254, new int[][] { {4097,1}, {3385,1}, {892, 70}, {140,4}, {1748 , 10, 16}, {1113, 1}, {1215, 1}, {1333,1}, {1347,1}, {9342, 2}, {5938,4}, {6156, 3}, {9194, 4}, {6313,1}, {805, 50} }),
		DRAGON(11256, new int[][] { {11212,100, 500}, {9341, 3, 40}, {1305,1}, {11232, 105, 350}, {11237, 100, 500}, {9193, 10, 49}, {535, 111, 297}, {1216, 3}, {11230, 105, 350}, {5316, 1}, {537, 52, 99}, {1616, 3, 6}, {1705, 2, 4}, {5300, 6}, {7219, 5, 15}, {4093, 1}, {5547, 1}, {1701, 2, 4} } );

		public static HashMap<Integer, ImpRewards> impReward = new HashMap<>();

		static {
			for(ImpRewards t : ImpRewards.values()) {
				impReward.put(t.itemId, t);
			}
		}

		private int itemId;
		private int[][] rewards;

		ImpRewards(int itemId, int[][] rewards) {
			this.itemId = itemId;
			this.rewards = rewards;
		}
		public int getItemId() {
			return itemId;
		}
		public int[][] getRewards() {
			return rewards;
		}

		public static void getReward(Client c, int itemId) {
			if(c.getItems().freeSlots() < 2) {
				c.sendMessage("You need atleast 2 free slots.");
				return;
			}

			ImpRewards t = ImpRewards.impReward.get(itemId);
			c.getItems().deleteItem(t.getItemId(), c.getItems().getItemSlot(t.getItemId()), 1);
			int r = random.nextInt(t.getRewards().length);
			if(t.getRewards()[r].length == 3) {
				int amount = t.getRewards()[r][1] + random.nextInt(t.getRewards()[r][2] - t.getRewards()[r][1]);
				c.getItems().addItem(t.getRewards()[r][0], amount);
			} else {
				c.getItems().addItem(t.getRewards()[r][0], t.getRewards()[r][1]);
			}
			if(Misc.random(15) == 0) {
				c.sendMessage("The impling jar breaks but you do get a reward!");
			} else {
				c.getItems().addItem(11260, 1);
				c.sendMessage("You open the impling jar and get a reward!");
			}
		}
	}
	
	private static int IMPLINGS_AMOUNT = 40, RARE_IMPLINGS_AMOUNT = 8;
	public static void spawnImplings(){
		int westOrEast = 0, northOrSouth = 0;
		for (int i = 0; i <= IMPLINGS_AMOUNT; i++) {
			int random = Misc.random(3);
			switch(random) {
			case 0:
				westOrEast = 2796-Misc.random(20);
				northOrSouth = 2915-Misc.random(21);
				break;
			case 1:
				westOrEast = 2796+Misc.random(20);
				northOrSouth = 2915+Misc.random(21);
				break;
			case 2:
				westOrEast = 2796+Misc.random(21);
				northOrSouth = 2915-Misc.random(20);
				break;
			case 3:
				westOrEast = 2796-Misc.random(21);
				northOrSouth = 2915+Misc.random(20);
				break;
			}
			Server.npcHandler.newNPC(1028+Misc.random(7), westOrEast, northOrSouth, 0, 1, 0, 0, 0, 0);
		}
		for (int i2 = 0; i2 <= RARE_IMPLINGS_AMOUNT; i2++) {
			int random = Misc.random(3);
			switch(random) {
			case 0:
				westOrEast = 2796-Misc.random(20);
				northOrSouth = 2915-Misc.random(20);
				break;
			case 1:
				westOrEast = 2796+Misc.random(20);
				northOrSouth = 2915+Misc.random(20);
				break;
			case 2:
				westOrEast = 2796+Misc.random(20);
				northOrSouth = 2915-Misc.random(20);
				break;
			case 3:
				westOrEast = 2796-Misc.random(20);
				northOrSouth = 2915+Misc.random(20);
				break;
			}
			Server.npcHandler.newNPC(6063+Misc.random(1), westOrEast, northOrSouth, 0, 1, 0, 0, 0, 0);
		}
	}

	/**
	 * 
	 * Catching imps, spawning, despawning
	 * @author Emre
	 *
	 */
	public enum Imps {
		BABY_IMPLING(1028, 1, 25, 11238),
		YOUNG_IMPLING(1029, 22, 65, 11240),
		GOURMET_IMPLING(1030, 28, 113, 11242),
		EARTH_IMPLING(1031, 36, 177, 11244),
		ESSENCE_IMPLING(1032, 42, 255, 11246),
		ECLECTIC_IMPLING(1033, 50, 289, 11248),
		NATURE_IMPLING(1034, 58, 353, 11250),
		MAGPIE_IMPLING(1035, 65, 409, 11252),
		NINJA_IMPLING(6063, 74, 481, 11254),
		DRAGON_IMPLING(6064, 83, 553, 11256);

		public static HashMap<Integer, Imps> implings = new HashMap<>();

		static {
			for(Imps t : Imps.values()) {
				implings.put(t.npc, t);
			}
		}

		private int npc;
		private int levelRequired;
		private int experience;
		private int itemId;

		Imps(final int npc, final int levelRequired, final int experience, final int itemId) {
			this.npc = npc;
			this.levelRequired = levelRequired;
			this.experience = experience;
			this.itemId = itemId;
		}
		public int getImpId() {
			return npc;
		}
		public int getLevelRequired() {
			return levelRequired;
		}
		public int getXp() {
			return experience;
		}
		public int getJar() {
			return itemId;
		}
		public static Imps forId(int id) {
			return implings.get(id);
		}
		public static void catchImp(Client c, int npcType, final int npcidx) {
			
			if(c.playerSkilling[22]) {
				return;
			}
			Imps t = Imps.implings.get(npcType);

			if(c.playerEquipment[c.playerWeapon] != 10010 && c.playerEquipment[c.playerWeapon] != 11259 ) {
				c.sendMessage("You need a butterfly net.");
				return;
			}

			if(c.playerLevel[22] < t.getLevelRequired()) {
				c.sendMessage("You need a hunter of " + t.getLevelRequired() + " to catch this imp.");
				return;
			}

			if(!c.getItems().playerHasItem(11260)) {
				c.sendMessage("You need a impling jar.");
				return;
			}
			c.playerSkilling[22] = true;
			if(Misc.random(25) == 0) {
				c.startAnimation(6999);
				c.sendMessage("You failed to catch the imp...");
			} else {
				c.startAnimation(6999);
				c.getItems().addItem(t.getJar(), 1);
				c.getItems().deleteItem(11260, c.getItems().getItemSlot(11260), 1);
				c.getPA().addSkillXP(t.getXp() * Config.HUNTER_EXPERIENCE, 22);
				c.sendMessage("You catch the Imp!");
				impDeath(c, npcType, npcidx);
			}
			c.playerSkilling[22] = false;

		}
		public static void respawnImp(Client c, final int id, final int x, final int y, final int z) {
			CycleEventHandler.addEvent(c, new CycleEvent() {
				
				int i = 0;
				@Override
				public void execute(CycleEventContainer container) {
					if(i++ == 1) {
						Server.npcHandler.newNPC(id, x, y, z, 1, 0, 0, 0, 0);
						container.stop();
					}
				}

				@Override
				public void stop() {
				}
			}, 100);
		}
		public static void impDeath(Client c, int npcType, int idx) {
			Imps t = Imps.implings.get(npcType);
			NPC n = NPCHandler.npcs[idx];
			if(n != null && n.npcType == t.getImpId()) {
					int x = n.absX;
					int y = n.absY;
					int z = n.heightLevel;
					n.absX = 0;
					n.absY = 0;
					n.isDead = true;
					NPCHandler.npcs[idx] = null;
					respawnImp(c, npcType, x, y, z);
			}
		}
	}
}