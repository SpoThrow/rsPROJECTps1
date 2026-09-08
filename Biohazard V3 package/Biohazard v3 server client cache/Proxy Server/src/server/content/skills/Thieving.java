package server.content.skills;

import java.util.HashMap;

import server.Config;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.players.Client;
import server.game.players.Player;
import core.util.Misc;

public class Thieving {

	@SuppressWarnings("unused")
	private Client c;

	public Thieving(Client c) {
		this.c = c;
	}

	public boolean playerIsStunned = false;
	public long lastStunned;
	private boolean playerIsPickpocketing = false;
	private boolean successfulThief = true;

	/**
	 * This enum contains all the items that can
	 * be pickpocket from each npc. It also contains
	 * the level required, the xp gained, stun time
	 * and stun damage.
	 * 
	 * To implement this, you will first need to check
	 * if the npc you are trying to pickpocket is an npc
	 * that can be pickpocketed. Then, you will call the
	 * actual pickpocket method below.
	 * 
	 * @author L A
	 *
	 */

	public static enum pickpocket {

		MAN(new int[] {1, 2, 3}, new int[][] {{995, 300}}, 1, 8, 5, 1),
		WOMAN(new int[] {4, 5, 6}, new int[][] {{995, 300}}, 1, 8, 5, 1),
		FARMER(new int[] {7, 1757, 1758, 1759, 1760, 1761}, new int[][] {{995, 900}, {5318, 1}}, 10, 14.5, 5, 1),
		FEMALE_HAM_MEMBER(new int[] {1715}, new int[][] {{882, 20}, {1351, 1}, {1265, 1}, {1349, 1}, {1267, 1}, {886, 20}, {1353, 1}, {1207, 1}, {1129, 1}, {4170, 1}, {4302, 1}, {4298, 1}, {4300, 1}, {4304, 1}, {4306, 1}, {4308, 1}, {4310, 1}, {995, 2100}, {319, 1}, {2138, 1}, {668, 1}, {453, 1}, {440, 1}, {1739, 1}, {314, 5}, {1734, 6}, {1733, 1}, {1511, 1}, {686, 1}, {697, 1}, {1625, 1}, {1627, 1}, {1617, 1}, {199, 1}, {201, 1}, {203, 1}}, 15, 18.5, 4, 3),
		MALE_HAM_MEMBER(new int[] {1714}, new int[][] {{882, 20}, {1351, 1}, {1265, 1}, {1349, 1}, {1267, 1}, {886, 20}, {1353, 1}, {1207, 1}, {1129, 1}, {4170, 1}, {4302, 1}, {4298, 1}, {4300, 1}, {4304, 1}, {4306, 1}, {4308, 1}, {4310, 1}, {995, 2100}, {319, 1}, {2138, 1}, {668, 1}, {453, 1}, {440, 1}, {1739, 1}, {314, 5}, {1734, 6}, {1733, 1}, {1511, 1}, {686, 1}, {697, 1}, {1625, 1}, {1627, 1}, {199, 1}, {201, 1}, {203, 1}}, 20, 22.5, 4, 3),
		HAM_GUARD(new int[] {1710, 1711, 1712}, new int[][] {{882, 20}, {1351, 1}, {1265, 1}, {1349, 1}, {1267, 1}, {886, 20}, {1353, 1}, {1207, 1}, {1129, 1}, {4170, 1}, {4302, 1}, {4298, 1}, {4300, 1}, {4304, 1}, {4306, 1}, {4308, 1}, {4310, 1}, {995, 2100}, {319, 1}, {2138, 1}, {668, 1}, {453, 1}, {440, 1}, {1739, 1}, {314, 5}, {1734, 6}, {1733, 1}, {1511, 1}, {686, 1}, {697, 1}, {1625, 1}, {1627, 1}, {199, 1}, {201, 1}, {203, 1}, {5321, 4}, {5323, 4}, {5319, 4}, {5324, 1}}, 20, 22.5, 4, 3),
		WARRIOR_WOMAN(new int[] {15}, new int[][] {{995, 1800}}, 25, 26, 5, 2),
		AL_KHARID_WARRIOR(new int[] {18}, new int[][] {{995, 1800}}, 25, 26, 5, 2),
		ROGUE(new int[] {187}, new int[][] {{995, 12000}, {556, 8}, {1523, 1}, {1219, 1}, {1993, 1}, {2357, 1}, {1227, 1}}, 32, 35.5, 5, 20),
		MASTER_FARMER(new int[] {2234, 2235}, new int[][] {{5318, 1}, {5319, 1}, {5324, 3}, {5323, 2},  {5321, 2}, {5305, 4}, {5307, 2}, {5308, 2}, {5306, 3}, {5309, 2}, {5310, 1}, {5311, 1}, {5101, 1}, {5102, 1}, {5103, 1}, {5104, 1}, {5105, 1}, {5106, 1}, {5096, 1}, {5097, 1}, {5098, 1}, {5099, 1}, {5100, 1}, {5291, 1}, {5292, 1}, {5293, 1}, {5294, 1}, {5295, 1}, {5296, 1}, {5297, 1}, {5298, 1}, {5299, 1}, {5300, 1}, {5301, 1}, {5302, 1}, {5303, 1}, {5304, 1}, {5280, 1}, {5281, 1}}, 38, 43, 5, 3),
		GUARD(new int[] {9, 10}, new int[][] {{995, 3000}}, 40, 46.5, 5, 2),
		POLLNIVIAN_BEARDED_BANDIT(new int[] {1880, 1881}, new int[][] {{995, 4000}}, 45, 65, 5, 5),
		DESERT_BANDIT(new int[] {1926, 1927, 1928, 1929, 1930, 1931}, new int[][] {{995, 3000}, {179, 1}, {1523, 1}}, 53, 79.5, 5, 30),
		KNIGHT_OF_ARDOUGNE(new int[] {23, 26}, new int[][] {{995, 5000}}, 55, 84.3, 5, 3),
		POLLNIVIAN_BANDIT(new int[] {1883, 1884}, new int[][] {{995, 5000}}, 55, 84.3, 5, 5),
		YANILLE_WATCHMEN(new int[] {32}, new int[][] {{995, 6000}, {2309, 1}}, 65, 137.5, 5, 3),
		MENAPHITE_THUG(new int[] {1904, 1905}, new int[][] {{995, 6000}, {6392, 1}, {6394, 1}, {6396, 1}, {6398, 1}, {6400, 1}, {6402, 1}, {6404, 1}, {6406, 1}}, 65, 137.5, 5, 5),
		PALADIN(new int[] {20, 365, 2256}, new int[][] {{995, 8000}, {562, 2}}, 70, 151.75, 5, 3),
		GNOME(new int[] {66, 67, 68}, new int[][] {{995, 15000}, {557, 1}, {444, 1}, {569, 1}, {2150, 1}, {2162, 1}}, 75, 198.5, 5, 1),
		HERO(new int[] {21}, new int[][] {{995, 10000}, {560, 2}, {565, 1}, {444, 1}, {1993, 1}, {569, 1}, {1601, 1}}, 80, 273.3, 6, 4);

		public int[] npcId;
		public int[][] npcYield;
		public int level, stunTime, damage;
		public double xp;

		pickpocket(int[] npcId, int[][] npcYield, int level, double xp, int stunTime, int damage) {
			this.npcId = npcId;
			this.npcYield = npcYield;
			this.level = level;
			this.xp = xp;
			this.stunTime = stunTime;
			this.damage = damage;
		}

		public static HashMap<Integer, pickpocket> pick = new HashMap<Integer, pickpocket>();

		public static pickpocket forNpcId(int npcId) {
			return pick.get(npcId);
		}

		static {
			for (pickpocket p : pickpocket.values()) {
				for (int npcId : p.npcId) {
					pick.put(npcId, p);
				}
			}
		}

		public int[][] getNpcYield() {
			return npcYield;
		}

		public int getRequiredLevel() {
			return level;
		}

		public double getXp() {
			return xp;
		}

		public int getStunTime() {
			return stunTime;
		}

		public int damageDealt() {
			return damage;
		}
	}

	/**
	 * This boolean checks to see if the npc can be pickpocketed or not
	 * @param npcType The npc id
	 * @return If the npc is an npc that can be pickpocketed or not
	 */

	public static boolean isPickPocketNpc(final int npcType) {
		final pickpocket p = pickpocket.forNpcId(npcType);
		for (int i = 0; i < p.npcId.length; i++) {
			if (npcType == p.npcId[i]) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The lower the return value, the lower the failure rate
	 * @param c the client
	 * @param npcType the npc id
	 * @return an integer to determine how often you will fail
	 */

	private int failureRate(final Client c, final int npcType) {
		final pickpocket p = pickpocket.forNpcId(npcType);
		double npcFactor = p.getRequiredLevel() / 10;
		double levelFactor = 100 / ((c.playerLevel[Player.playerThieving] + 1) - p.getRequiredLevel());
		return (int)Math.floor((levelFactor + npcFactor) / 2);
	}

	/**
	 * Checks to see if the client has the required level to pickpocket the npc
	 * @param c the client 
	 * @param npcType the npc id
	 * @return if the player has the required level or not
	 */

	private boolean canThieveNpc(final Client c, final int npcType) {
		final pickpocket p = pickpocket.forNpcId(npcType);
		if (c.playerLevel[Player.playerThieving] >= p.getRequiredLevel()) {
			return true;
		}
		c.sendMessage("You need a thieving level of "+ p.getRequiredLevel() +" to pickpocket this "+ p.toString().toLowerCase().replaceAll("_", " "));
		return false;
	}

	/**
	 * This will add the item that you pickpocket
	 * @param c the client
	 * @param npcType the npc id
	 */

	private void addThievedItem(final Client c, final int npcType) {
		final pickpocket p = pickpocket.forNpcId(npcType);
		int i = Misc.random(p.getNpcYield().length - 1);
		c.getItems().addItem(p.npcYield[i][0], p.npcYield[i][1]);
		c.getPA().addSkillXP((int) p.getXp() * Config.THIEVING_EXPERIENCE, Player.playerThieving);
		
	}

	/**
	 * What will happen if you fail to pickpocket the npc
	 * @param c the client
	 * @param stunTime how long you will be stunned for
	 * @param damage how much damage will be dealt
	 * @param npcId the npc id
	 */

	private void applyStun(final Client c, final int stunTime, final int damage, int npcId) {
		c.startAnimation(c.getCombat().getBlockEmote());
		c.setHitDiff(damage);
		c.setHitUpdateRequired(true);
		c.playerLevel[3] -= damage;
		c.getPA().refreshSkill(3);
		playerIsStunned = true;
		c.gfx100(80);
		CycleEventHandler.addEvent(c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				if (System.currentTimeMillis() - lastStunned > stunTime) {
					container.stop();
				}
			}
			@Override
			public void stop() {
				playerIsStunned = false;
				c.startAnimation(65535);
			}
		}, 4);
	}

	/**
	 * This is the actual method to pickpocket the npc
	 * @param c the client
	 * @param npcType the npc id
	 */

	public void pickpocketNpc(final Client c, final int npcType) {
		final pickpocket p = pickpocket.forNpcId(npcType);
		if (playerIsPickpocketing) {
			return;
		}
		if (playerIsStunned) {
			c.sendMessage("You are stunned!");
			return;
		}
		if (canThieveNpc(c, npcType)) {
			c.startAnimation(881);
			c.sendMessage("You attempt to pickpocket the "+ p.toString().toLowerCase().replaceAll("_", " ") +".");
			CycleEventHandler.addEvent(c, new CycleEvent() {
				@Override 
				public void execute(CycleEventContainer container) {
					playerIsPickpocketing = true;						
					c.startAnimation(881);
					if (Misc.random(100) < failureRate(c, npcType)) {
						c.sendMessage("You failed to pickpocket the "+ p.toString().toLowerCase().replaceAll("_", " "));
						successfulThief = false;
						container.stop();
					} else {
						successfulThief = true;
						container.stop();
					}
				}
				@Override
				public void stop() {
					playerIsPickpocketing = false;
					if (successfulThief) {
						addThievedItem(c, npcType);	
						c.sendMessage("You successfully pickpocket the "+ p.toString().toLowerCase().replaceAll("_", " ") +".");
						return;
					} else if (!successfulThief) {
						applyStun(c, p.getStunTime() * 1000, p.damageDealt(), npcType);
						lastStunned = System.currentTimeMillis();
						return;
					}
				}
			}, 1);
		}
	}
}