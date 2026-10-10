package server.game.minigames.randomevents;

import server.Server;
import server.game.players.Client;
import core.util.Misc;

public class Zombie {
	
	public static int[][] zombie = {
		{3, 	10, 	419, 	19, 	1},
		{11, 	20, 	420, 	40, 	1},
		{21, 	40, 	421, 	80, 	3},
		{61, 	90, 	422, 	105, 	4},
		{91, 	110, 	423, 	120, 	5},
		{111, 	138, 	424, 	150, 	7},
	};

	/**
	 * @return true if a zombie was actually spawned; false if one was already out, the player is
	 *         too low a level for any table row, or the level band matched nothing.
	 */
	public static boolean spawnZombie(Client c) {
		if(c.combatLevel <= 4)
			return false;
		for (int[] aZombie : zombie) {
			if(!c.zombieSpawned) {
				if (c.combatLevel >= aZombie[0] && c.combatLevel <= aZombie[1]) {
					Server.npcHandler.spawnNpc(c, aZombie[2], c.getX() + Misc.random(1), c.getY() + Misc.random(1), c.position.heightLevel, 0, aZombie[3], aZombie[4], aZombie[4] * 10, aZombie[4] * 10, true, false);
					c.zombieSpawned = true;
					return true;
				}
			}
		}
		return false;
	}

}
