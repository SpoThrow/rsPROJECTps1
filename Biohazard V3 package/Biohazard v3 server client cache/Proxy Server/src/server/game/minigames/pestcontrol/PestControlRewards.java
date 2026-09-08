package server.game.minigames.pestcontrol;

import server.Config;
import server.game.players.Client;

public class PestControlRewards {
	
	/**
	 * Can players use points exchange yes ? no
	 */
	public final static boolean CAN_EXCHANGE_POINTS = true;

	/**
	 * Rewards interface int id
	 */
	public final static int REWARDS_INTERFACE = 18691;

	/**
	 * Void Knights that can exchange points
	 */
	public final static int[] VOID_KNIGHTS = {3788, 3789};

	/**
	 * Reward Types
	 */
	public final static int
	NONE = 0,
	ATTACK = 1,
	STRENGTH = 2,
	DEFENCE = 3,
	RANGED = 4,
	MAGIC = 5,
	HITPOINTS = 6,
	PRAYER = 7;

	/**
	 * Reward Type Selected
	 */
	public static int rewardSelected = 0;

	/**
	 * Gets String Reward Chosen
	 * @return
	 */
	public static String checkReward() {
		if(rewardSelected == NONE) {
			return "None";
		} else if(rewardSelected == NONE) {
			return "None";
		} else if(rewardSelected == ATTACK) {
			return "Attack";
		} else if(rewardSelected == STRENGTH) {
			return "Strength";
		} else if(rewardSelected == DEFENCE) {
			return "Defence";
		} else if(rewardSelected == RANGED) {
			return "Ranged";
		} else if(rewardSelected == MAGIC) {
			return "Magic";
		} else if(rewardSelected == HITPOINTS) {
			return "Hitpoints";
		} else if(rewardSelected == PRAYER) {
			return "Prayer";
		}
		return "";
	}

	/**
	 * Opens Point Exchange
	 * @param c
	 * @param button
	 */
	public static void exchangePestPoints(Client c) {
		if (!CAN_EXCHANGE_POINTS) {
			c.sendMessage("Pest Control point exchange is currently disabled.");
			return;
		}
		c.getPA().sendFrame126("Void Knights' Training Options", 18758);
		c.getPA().sendFrame126("ATTACK", 18767);
		c.getPA().sendFrame126("STRENGTH", 18768);
		c.getPA().sendFrame126("DEFENCE", 18769);
		c.getPA().sendFrame126("RANGED", 18770);
		c.getPA().sendFrame126("MAGIC", 18771);
		c.getPA().sendFrame126("HITPOINTS", 18772);
		c.getPA().sendFrame126("PRAYER", 18773);
		c.getPA().sendFrame126(checkReward(), 18782);
		c.getPA().sendFrame126("Points: "+c.pcPoints, 18783);
		c.sendMessage("You currently have "+c.pcPoints+" pest control points.");
		c.getPA().showInterface(REWARDS_INTERFACE);
	}

	public static void handlePestButtons(Client c, int button) {
		switch(button) {

		/**
		 * Attack
		 */
		case 73072:
		case 73079:
			rewardSelected = ATTACK;
			c.getPA().sendFrame126(checkReward(), 18782);
			break;

			/**
			 * Strength
			 */
		case 73073:
		case 73080:
			rewardSelected = STRENGTH;
			c.getPA().sendFrame126(checkReward(), 18782);
			break;

			/**
			 * Defence	
			 */
		case 73074:
		case 73081:
			rewardSelected = DEFENCE;
			c.getPA().sendFrame126(checkReward(), 18782);
			break;

			/**
			 * Ranged	
			 */
		case 73075:
		case 73082:
			rewardSelected = RANGED;
			c.getPA().sendFrame126(checkReward(), 18782);
			break;

			/**
			 * Magic		
			 */
		case 73076:
		case 73083:
			rewardSelected = MAGIC;
			c.getPA().sendFrame126(checkReward(), 18782);
			break;

			/**
			 * Hitpoints	
			 */
		case 73077:
		case 73084:
			rewardSelected = HITPOINTS;
			c.getPA().sendFrame126(checkReward(), 18782);
			break;

			/**
			 * Prayer	
			 */
		case 73078:
		case 73085:
			rewardSelected = PRAYER;
			c.getPA().sendFrame126(checkReward(), 18782);
			break;

			/**
			 * Confirm	
			 */
		case 73091:
			switch(rewardSelected) {
			case NONE:
				c.sendMessage("You don't have a reward selected.");
				break;
			case ATTACK:
				if(c.pcPoints > 1) {
					c.getPA().addSkillXP(c.playerLevel[Config.ATTACK] * c.playerLevel[Config.ATTACK]/17.5 * 4, Config.ATTACK);
					c.sendMessage("You have been rewarded attack experience.");
					c.pcPoints -= 2;
				} else {
					c.sendMessage("You need at least 2 pest control points to exchange your points.");
				}
				break;
			case STRENGTH:
				if(c.pcPoints > 1) {
					c.getPA().addSkillXP(c.playerLevel[Config.STRENGTH] * c.playerLevel[Config.STRENGTH]/17.5 * 4, Config.STRENGTH);
					c.sendMessage("You have been rewarded strength experience.");
					c.pcPoints -= 2;
				} else {
					c.sendMessage("You need at least 2 pest control points to exchange your points.");
				}
				break;
			case DEFENCE:
				if(c.pcPoints > 1) {
					c.getPA().addSkillXP(c.playerLevel[Config.DEFENCE] * c.playerLevel[Config.DEFENCE]/17.5 * 4, Config.DEFENCE);
					c.sendMessage("You have been rewarded defence experience.");
					c.pcPoints -= 2;
				} else {
					c.sendMessage("You need at least 2 pest control points to exchange your points.");
				}
				break;
			case RANGED:
				if(c.pcPoints > 1) {
					c.getPA().addSkillXP(c.playerLevel[Config.RANGED] * c.playerLevel[Config.RANGED]/17.5 * 4, Config.RANGED);
					c.sendMessage("You have been rewarded ranged experience.");
					c.pcPoints -= 2;
				} else {
					c.sendMessage("You need at least 2 pest control points to exchange your points.");
				}
				break;
			case MAGIC:
				if(c.pcPoints > 1) {
					c.getPA().addSkillXP(c.playerLevel[Config.MAGIC] * c.playerLevel[Config.MAGIC]/17.5 * 4, Config.MAGIC);
					c.sendMessage("You have been rewarded magic experience.");
					c.pcPoints -= 2;
				} else {
					c.sendMessage("You need at least 2 pest control points to exchange your points.");
				}
				break;
			case HITPOINTS:
				if(c.pcPoints > 1) {
					c.getPA().addSkillXP(c.playerLevel[Config.HITPOINTS] * c.playerLevel[Config.HITPOINTS]/17.5 * 4, Config.HITPOINTS);
					c.sendMessage("You have been rewarded hitpoints experience.");
					c.pcPoints -= 2;
				} else {
					c.sendMessage("You need at least 2 pest control points to exchange your points.");
				}
				break;
			case PRAYER:
				if(c.pcPoints > 1) {
					c.getPA().addSkillXP(c.playerLevel[Config.PRAYER] * c.playerLevel[Config.PRAYER]/8.75 * 4, Config.PRAYER);
					c.sendMessage("You have been rewarded prayer experience.");
					c.pcPoints -= 2;
				} else {
					c.sendMessage("You need at least 2 pest control points to exchange your points.");
				}
				break;
			}
			
			c.getPA().sendFrame126("Points: "+c.pcPoints, 18783);
			break;
		}
	}

}
