package server.content.skills;

import server.Config;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.items.ItemAssistant;
import server.game.players.Client;
import server.game.players.Player;

public class Herblore extends SkillHandler {
	
	private static int itemToAdd = -1, itemToDelete = -1, itemToDelete2 = -1, potExp = -1;
	private static final int  ANIM = 363, ANIM2 = 364, PESTLE = 233;
	
	private static final int[][] CLEAN_DATA = {
		{199, 249, 1, 3}, {201, 251, 5, 4}, 
		{203, 253, 11, 5}, {205, 255, 20, 6},
		{207, 257, 25, 8}, {3049, 2998, 30, 8},
		{12174, 12172, 35, 8}, {209, 259, 40, 9},
		{14836, 14854, 30, 8}, {211, 261, 48, 10},
		{213, 263, 54, 11}, {3051, 3000, 59, 12},
		{215, 265, 65, 13}, {2485, 2481, 67, 13},
		{217, 267, 70, 14}, {219, 269, 75, 15},
	
	};
	
	public static void cleanHerb(final Client player, final int herbId, int slotId) {
		for (int h = 0; h < CLEAN_DATA.length; h++) {
			if(player.getItems().playerHasItem(CLEAN_DATA[h][0])) {
				if (player.getLevelForXP(player.playerXP[Player.playerHerblore]) < CLEAN_DATA[h][2]) {
					player.sendMessage("You need an herblore level of " + CLEAN_DATA[h][2] + " to clean this herb.");
					return;
				}
				player.getItems().deleteItem(CLEAN_DATA[h][0], slotId, 1);
				player.getItems().addItem(CLEAN_DATA[h][1], 1);
				player.getPA().addSkillXP(CLEAN_DATA[h][3] * Config.HERBLORE_EXPERIENCE, Player.playerHerblore);
				player.sendMessage("You clean the dirt from the "+ItemAssistant.getItemName(CLEAN_DATA[h][1]).toLowerCase()+".");
			}
		}
	}
	
	private static final int[][] POTION_DATA = { 
		{ 91, 221, 121, 1, 25 }, { 93, 235, 175, 5, 38 }, 
		{ 95, 225, 115, 12, 50 }, { 97, 223, 127, 22, 63 }, 
		{ 99, 239, 133, 30, 75 }, { 97, 9736, 9741, 36, 84 }, 
		{ 99, 231, 139, 38, 88 }, { 101, 221, 145, 45, 100 }, 
		{ 101, 235, 181, 48, 106 }, { 103, 231, 151, 50, 112 }, 
		{ 105, 225, 157, 55, 125 }, { 105, 241, 187, 60, 137 }, 
		{ 3004, 223, 3026, 63, 142 }, { 107, 239, 163, 66, 150 }, 
		{ 2483, 241, 2454, 69, 158 }, { 109, 245, 169, 72, 163 }, 
		{ 2483, 3138, 3042, 76, 173 }, { 111, 247, 189, 78, 175 }, 
		{ 3002, 6693, 6687, 81, 180 }, { 103, 10111, 10000, 53, 120 },
		{ 5936, 223, 5937, 73, 165 }, { 5939, 6018, 5940, 82, 190 }, //weppoison + and ++ unf //vialunf, ingr1, pot, level, exp
		{ 227, 249, 91, 1, 0 }, { 227, 251, 93, 5, 0 },
		{ 227, 253, 95, 12, 0 }, { 227, 255, 97, 22, 0 }, 
		{ 227, 257, 99, 30, 0 }, { 227, 259, 101, 34, 0 }, 
		{ 227, 261, 103, 45, 0 }, { 227, 263, 105, 55, 0 },
		{ 227, 265, 107, 66, 0 }, { 227, 267, 109, 72, 0 }, 
		{ 227, 269, 111, 75, 0 }, { 227, 2481, 2483, 67, 0 }, 
		{ 227, 3000, 3004, 59, 0 }, { 227, 2998, 3002, 30, 0 },
		{ 5935, 6016, 5936, 73, 0 },{ 5935, 2398, 5939, 82, 0 },//weppoison + and ++ unf //vialingr, herb1, unf, level, exp
	};
	
	public static void setupPotion(final Client c, int useItem, int itemUsed) {
		for (int f = 0; f < POTION_DATA.length; f++) {
			if ((useItem == POTION_DATA[f][0] && itemUsed == POTION_DATA[f][1]) || (useItem == POTION_DATA[f][1] && itemUsed == POTION_DATA[f][0])) {
				if (c.playerLevel[Player.playerHerblore] < POTION_DATA[f][3]) {
					c.sendMessage("You need an herblore level of " + POTION_DATA[f][3] + " to mix this potion.");
					return;
				}
				send1Item(c, POTION_DATA[f][2], false);
				itemToDelete = POTION_DATA[f][1];
				itemToDelete2 = POTION_DATA[f][0];
				potExp = POTION_DATA[f][4];
				itemToAdd = POTION_DATA[f][2];
				c.isPotionMaking = true;
			}
		}	
	}
	
	public static void makePotion(final Client c, int amount) {
		if(c.playerSkilling[Player.playerHerblore]) {
			return;	
		}
		if(itemToDelete <= 0 || itemToDelete2 <= 0) {
			return;
		}
		c.doAmount = amount;
		c.playerSkilling[Player.playerHerblore] = true;
		c.getPA().removeAllWindows();
		c.startAnimation(ANIM);
		CycleEventHandler.addEvent(4, c, new CycleEvent() {
			@Override
				public void execute(CycleEventContainer container) {
					c.getItems().deleteItem(itemToDelete, c.getItems().getItemSlot(itemToDelete), 1);
					c.getItems().deleteItem(itemToDelete2, c.getItems().getItemSlot(itemToDelete2), 1);
					c.getItems().addItem(itemToAdd, 1);
					c.sendMessage("You make a " + ItemAssistant.getItemName(itemToAdd).toLowerCase() + ".");
					c.getPA().addSkillXP(potExp * Config.HERBLORE_EXPERIENCE, Player.playerHerblore);
					deleteTime(c);
					if(!c.getItems().playerHasItem(itemToDelete2, 1) || !c.getItems().playerHasItem(itemToDelete, 1) || c.doAmount <= 0) {
						container.stop();
					}
					if(!c.isPotionMaking) {
						container.stop();
					}
				}
		@Override
			public void stop() {
				resetHerblore(c);
			}
		}, 1);
		CycleEventHandler.addEvent(4, c, new CycleEvent() {
			@Override
				public void execute(CycleEventContainer container) {
					c.startAnimation(ANIM);
					if(!c.playerSkilling[Player.playerHerblore] || !c.isPotionMaking) {
						container.stop();
					}
				}
			@Override
				public void stop() {
				}
		}, 1);
	}
	
	private final static int[][] GRINDABLES = {
		{237, 235},
		{1973, 1975},
		{5075, 6693},
		{10109, 10111},
		{243, 241},
		{14703, 14704},
		{9735, 9736},
		{6466, 6467},
	};
	
	public static void setupGrinding(final Client c, final int itemUsed, final int usedWith) {
		for (int g = 0; g < GRINDABLES.length; g++) {
			if ((itemUsed == GRINDABLES[g][0] && usedWith == PESTLE) || (itemUsed == PESTLE && usedWith == GRINDABLES[g][0])) {
				send1Item(c, GRINDABLES[g][1], false);
				itemToDelete = GRINDABLES[g][0];
				itemToAdd = GRINDABLES[g][1];
				c.isGrinding = true;
			}
		}
	}
	
	private static void grindItem(final Client c, int amount) {
		if(c.playerSkilling[Player.playerHerblore]) {
			return;	
		}
		if(itemToDelete <= 0) {
			return;
		}
		c.doAmount = amount;
		c.playerSkilling[Player.playerHerblore] = true;
		c.getPA().removeAllWindows();
		c.startAnimation(ANIM2);
		CycleEventHandler.addEvent(c, new CycleEvent() {
			@Override
				public void execute(CycleEventContainer container) {
					c.getItems().deleteItem(itemToDelete, c.getItems().getItemSlot(itemToDelete), 1);
					c.getItems().addItem(itemToAdd, 1);
					c.getItems();
					c.sendMessage("You grind down the " + ItemAssistant.getItemName(itemToDelete).toLowerCase() + ".");
					deleteTime(c);
					if(!c.getItems().playerHasItem(itemToDelete, 1) || c.doAmount <= 0) {
						container.stop();
					}
					if(!c.isGrinding) {
						container.stop();
					}
				}
			@Override
				public void stop() {
					resetHerblore(c);
				}
			}, 1);
		CycleEventHandler.addEvent(c, new CycleEvent() {
			@Override
				public void execute(CycleEventContainer container) {
					c.startAnimation(ANIM2);
					if(!c.playerSkilling[Player.playerHerblore] || !c.isGrinding) {
						container.stop();
					}
				}
			@Override
				public void stop() {
				}
		}, 1);
	}
	
		
	public static void resetHerblore(Client c) {
		itemToAdd = -1;
		itemToDelete = -1;
		itemToDelete2 = -1;
		potExp = -1;
		c.isGrinding = false;
		c.isPotionMaking = false;
		c.playerSkilling[Player.playerHerblore] = false;
		//CycleEventHandler.stopEvents(c, 4);
	}
	
	public static boolean isHerb(int item) {
		for (int i = 0; i < CLEAN_DATA.length; i++) {
			if (item == CLEAN_DATA[i][0])
				return true;
		}
		return false;
       }
	
	public static boolean isIngredient(int item) {
		for(int i = 0; i < POTION_DATA.length; i++) {
			if(item == POTION_DATA[i][0] || item == POTION_DATA[i][1])
				return true;
		}
			return false;
	}
	
	public static boolean isGrindable(int item) {
		for(int i = 0; i < GRINDABLES.length; i++) {
			if(item == GRINDABLES[i][0])
				return true;
		}
                          return false;
	}
	
	public static void handleHerbloreButtons(Client c, int actionButtonId) {
		switch(actionButtonId) {
			case 10239:
				if(c.isGrinding) {
					grindItem(c, 1);
				} else if(c.isPotionMaking) {
					makePotion(c, 1);
				}
				break;
			case 10238:
				if(c.isGrinding) {
					grindItem(c, 5);
				} else if(c.isPotionMaking) {
					makePotion(c, 5);
				}
				break;
			case 6212:
				if(c.isGrinding) {
					grindItem(c, 10);
				} else if(c.isPotionMaking) {
					makePotion(c, 10);
				}
				break;
			case 6211:
				if(c.isGrinding) {
					grindItem(c, 28);
				} else if(c.isPotionMaking) {
					makePotion(c, 28);
				}
				break;
		}
	}
}