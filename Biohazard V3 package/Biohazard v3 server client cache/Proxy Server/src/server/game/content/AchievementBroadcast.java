package server.game.content;

import server.Server;
import server.game.items.ItemAssistant;
import server.game.players.Client;
import server.game.players.PlayerHandler;

/**
 * Server-wide achievement broadcasts for 99s, max total, and rare drops.
 * Kept quiet: one line per event, no extra chatter.
 */
public final class AchievementBroadcast {

	private static final String[] SKILLS = {
			"Attack", "Defence", "Strength", "Hitpoints", "Ranged", "Prayer", "Magic",
			"Cooking", "Woodcutting", "Fletching", "Fishing", "Firemaking", "Crafting",
			"Smithing", "Mining", "Herblore", "Agility", "Thieving", "Slayer", "Farming",
			"Runecrafting", "Hunter", "Summoning", "Construction", "Dungeoneering"
	};

	private static final int[] RARE_ITEMS = {
			4151, 11235, 11286, 11335, 3140, 4087, 4585, 1187, 1149, 6731, 6733, 6735, 6737,
			6739, 6571, 6585, 11702, 11704, 11706, 11708, 11694, 11696, 11698, 11700,
			11724, 11726, 11728, 11718, 11720, 11722, 11716, 11732, 11730,
			1050, 1053, 1055, 1057, 1038, 1040, 1042, 1044, 1046, 1048, 962, 1037,
			1419, 4565, 2577, 2581, 2631, 2633, 2635, 2637, 2639,
			4708, 4710, 4712, 4714, 4716, 4718, 4720, 4722, 4724, 4726, 4728, 4730,
			4732, 4734, 4736, 4738, 4745, 4747, 4749, 4751, 4753, 4755, 4757, 4759,
			11710, 11712, 11714, 11702, 4153
	};

	private AchievementBroadcast() {
	}

	public static void announce(String message) {
		if (message == null || message.length() == 0) {
			return;
		}
		for (int i = 0; i < PlayerHandler.players.length; i++) {
			if (PlayerHandler.players[i] != null) {
				Client other = (Client) PlayerHandler.players[i];
				other.sendMessage("@cr2@@red@[Achievement] @dre@" + message);
			}
		}
	}

	public static void onLevelUp(Client c, int skill, int level, int total) {
		if (c == null) {
			return;
		}
		if (level == 99) {
			announce(c.playerName + " has achieved 99 " + skillName(skill) + "!");
		}
		if (total >= 2277 && !c.maxTotalBroadcast) {
			c.maxTotalBroadcast = true;
			announce(c.playerName + " has reached the highest total level possible (" + total + ")!");
			c.getItems().addItemToBank(9813, 1);
			c.getItems().addItemToBank(9814, 1);
			c.sendMessage("A quest cape and hood has been added to your bank.");
			if (c.playerRights == 0) {
				c.playerRights = 8;
				c.veteran = 1;
				c.sendMessage("Please re-login for your new Veteran rank.");
			}
		}
	}

	public static void rareDrop(Client c, int itemId, int npcType) {
		if (c == null || !isRare(itemId)) {
			return;
		}
		String item = ItemAssistant.getItemName(itemId);
		String npc = "a monster";
		try {
			String listed = Server.npcHandler.getNpcListName(npcType);
			if (listed != null && listed.length() > 0 && !listed.equalsIgnoreCase("none")) {
				npc = listed.replace('_', ' ');
			}
		} catch (Exception e) {
		}
		announce(c.playerName + " has received a rare " + item + " from " + npc + "!");
	}

	public static boolean isRare(int itemId) {
		for (int i = 0; i < RARE_ITEMS.length; i++) {
			if (RARE_ITEMS[i] == itemId) {
				return true;
			}
		}
		return false;
	}

	private static String skillName(int skill) {
		if (skill >= 0 && skill < SKILLS.length) {
			return SKILLS[skill];
		}
		return "a skill";
	}
}
