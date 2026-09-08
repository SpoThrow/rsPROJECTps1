package server.content.skills;

import server.Config;
import server.game.players.Client;

public class SkillMasters {

	public enum MasterData {
		HUNTER(5113, 9948, Config.HUNTER),
		CONSTRUCTION(4247, 9789, Config.CONSTRUCTION),
		SLAYER(1599, 9786, Config.SLAYER),
		RUNECRAFTING(553, 9765, Config.RUNECRAFTING),
		STRENGTH(4297, 9750, Config.STRENGTH),
		ATTACK(4288, 9747, Config.ATTACK),
		DEFENCE(705, 9753, Config.DEFENCE),
		MAGIC(1658, 9762, Config.MAGIC),
		PRAYER(802, 9759, Config.PRAYER),
		RANGED(682, 9756, Config.RANGED),
		HITPOINTS(961, 9768, Config.HITPOINTS),
		MINING(3295, 9792, Config.MINING),
		SMITH(604, 9795, Config.SMITHING),
		FISHING(308, 9798, Config.FISHING),
		FM(4946, 9804, Config.FIREMAKING),
		COOKING(847, 9801, Config.COOKING),
		FLETCHING(575, 9783, Config.FLETCHING),
		CRAFTING(805, 9780, Config.CRAFTING),
		FARMING(3299, 9810, Config.FARMING),
		WC(4906, 9807, Config.WOODCUTTING),
		HERBLORE(455, 9774, Config.HERBLORE),
		THIEVING(2270, 9777, Config.THIEVING),
		AGILITY(437, 9771, Config.AGILITY);

		private int
		masterId,
		capeId,
		skillId;

		MasterData(int masterId, int capeId, int skillId) {
			this.masterId = masterId;
			this.capeId = capeId;
			this.skillId = skillId;
		}

		public int getMaster() {
			return masterId;
		}

		public int getCape() {
			return capeId;
		}

		public int getSkill() {
			return skillId;
		}
	}
	
	public static int getMaster() {
		for(MasterData m : MasterData.values()) {
			if (m.getMaster() == m.getMaster()) {
				return m.getMaster();
			}
		}
		return -1;	
	}
	
	public static int getSkill(Client c) {
		for(MasterData m : MasterData.values()) { 
			if (c.talkingNpc == m.getMaster()) {
			return m.getSkill();
			}
		}
		return -1;	
	}
	
    public static String getSkillName(Client c) {
        for (MasterData m : MasterData.values()) {
            if (c.talkingNpc == m.getMaster()) {
                return m.name().toLowerCase();
            }
        }
        return "";
    }

	public static int checkMaxedSkills(Client c) {
		int maxed = 0;
		for (int j = 0; j < c.playerLevel.length; j++) {
			if (c.getLevelForXP(c.playerXP[j]) >= 99) {
				maxed++;				
			}			
		}		
		return maxed;
	}

	public static void addSkillCape(Client c) {
		if(c.getItems().freeSlots() < 2) {
			c.getDH().sendStatement("You need at least 2 free inventory spaces to buy a skillcape.");
			c.nextChat = -1;
			return;
		}
		int maxed = checkMaxedSkills(c);
		for(MasterData m : MasterData.values()) {
			if (c.talkingNpc == m.getMaster()) {
				if (c.getPA().getLevelForXP(c.playerXP[getSkill(c)]) >= 99) {
					if (c.getItems().playerHasItem(995, 99000)) {
						if (maxed > 1) {
							c.getItems().addItem(m.capeId + 1, 1);
						} else {
							c.getItems().addItem(m.capeId, 1);
						}
						c.getItems().addItem(m.capeId + 2, 1);
						c.getItems().deleteItem(995, 99000);
						c.getPA().closeAllWindows();
					} else {
						c.getDH().sendStatement("You need 99,000 coins to buy a "+getSkillName(c)+" skillcape.");
						c.nextChat = -1;
						return;
					}
				} else {
					c.getDH().sendStatement("You need 99 "+getSkillName(c)+" to buy this skillcape.");
					c.nextChat = -1;
					return;
				}
			}
		}
	}
	
	public static void startDialogue(Client c) {
		for(MasterData m : MasterData.values()) {
			if (m.getMaster() == m.getMaster()) {
				c.getDH().sendDialogues(5, m.getMaster());
			}
		}
	}
	
	public static void masterDialogue(Client c) {
		for(MasterData m : MasterData.values()) {
			if (c.talkingNpc == m.getMaster()) {
				c.npcType = c.talkingNpc;
				c.getDH().sendNpcChat2("Hello I'm the "+getSkillName(c)+" master, would you like to", 
					"buy a skillcape?", c.getDH().CALM);
				c.nextChat = 441;
			}
		}
	}
	
	public static void masterOptions(Client c) {
		for(MasterData m : MasterData.values()) {
			if (c.talkingNpc == m.getMaster()) {
				c.getDH().sendOption2("I want to buy a skill cape.", "No thanks.");
				c.dialogueAction = 10000; 
			}
		}
	}

}
