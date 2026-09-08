package server.content.skills.misc;

import server.Config;
import server.game.players.Client;

public class SkillHandler {

	public static final int WOODCUTTING_XP = Config.WOODCUTTING_EXPERIENCE;
	public static final int FIREMAKING_XP = Config.FIREMAKING_EXPERIENCE;
	public static final int COOKING_XP = Config.COOKING_EXPERIENCE;
	public static final int MINING_XP = Config.MINING_EXPERIENCE;
	public static final int FLETCHING_XP = Config.FLETCHING_EXPERIENCE;

	public static final boolean view190 = true;

	public static void resetPlayerSkillVariables(Client c) {
		if (c.playerIsMining) {
			for (int i = 0; i < 2; i++) {
				c.miningProp[i] = -1;
			}
		} else if (c.playerIsCooking) {
			for (int i2 = 0; i2 < 6; i2++) {
				c.cookingProp[i2] = -1;

			}
		}
	}

	public static boolean noInventorySpace(Client c, String skill) {
		if (c.getItems().freeSlots() == 0) {
			c.sendMessage("You haven't got enough inventory space to continue "
					+ skill + "!");
			return false;
		}
		return true;
	}

	public static boolean hasRequiredLevel(final Client c, int id, int lvlReq,
			String skill, String event) {
		if (c.playerLevel[id] < lvlReq) {
			c.getPA().closeAllWindows();
			c.sendMessage("You don't have a high enough " + skill + " level to " + event + ".");
			return false;
		}
		return true;
	}

	public static void deleteTime(Client c) {
		c.doAmount--;
	}
}