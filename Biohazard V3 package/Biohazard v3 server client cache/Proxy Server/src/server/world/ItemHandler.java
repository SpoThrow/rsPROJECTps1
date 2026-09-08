package server.world;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import server.Config;
import server.game.items.GroundItem;
import server.game.items.ItemList;
import server.game.minigames.bountyhunter.BountyHunter;
import server.game.players.Client;
import server.game.players.Player;
import server.game.players.PlayerHandler;
import core.util.Misc;

/**
* Handles ground items
**/

public class ItemHandler {

	public List<GroundItem> items = new ArrayList<GroundItem>();
	public static final int HIDE_TICKS = 100;
	
	public ItemHandler() {			
		for(int i = 0; i < Config.ITEM_LIMIT; i++) {
			ItemList[i] = null;
		}
		loadItemList("item.cfg");
		loadItemPrices("prices.txt");
	}
	
	/**
	* Adds item to list
	**/	
	public void addItem(GroundItem item) {
		items.add(item);
	}
	
	/**
	* Removes item from list
	**/	
	public void removeItem(GroundItem item) {
		items.remove(item);
	}
	
	/**
	* Item amount
	**/	
	public int itemAmount(String name, int itemId, int itemX, int itemY) {
		for(GroundItem i : items) {
			if (i.hideTicks >= 1 && i.getName().equalsIgnoreCase(name)) {
				if(i.getItemId() == itemId && i.getItemX() == itemX && i.getItemY() == itemY) {
					return i.getItemAmount();
				}
			} else if (i.hideTicks < 1) {
				if(i.getItemId() == itemId && i.getItemX() == itemX && i.getItemY() == itemY) {
					return i.getItemAmount();
				}
			}
		}
		return 0;
	}
	
	
	/**
	* Item exists
	**/	
	public boolean itemExists(int itemId, int itemX, int itemY) {
		for(GroundItem i : items) {
			if(i.getItemId() == itemId && i.getItemX() == itemX && i.getItemY() == itemY) {
				return true;
			}
		}
		return false;
	}
	
	/**
	* Reloads any items if you enter a new region
	**/
	public void reloadItems(Client c) {
		for(GroundItem i : items) {
			if(c != null){
				if (c.getItems().tradeable(i.getItemId()) || i.getName().equalsIgnoreCase(c.playerName)) {
					if (c.distanceToPoint(i.getItemX(), i.getItemY()) <= 60) {
						if(i.hideTicks > 0 && i.getName().equalsIgnoreCase(c.playerName)) {
							c.getItems().removeGroundItem(i.getItemId(), i.getItemX(), i.getItemY(), i.getItemAmount());
							c.getItems().createGroundItem(i.getItemId(), i.getItemX(), i.getItemY(), i.getItemAmount());
						}
						if(i.hideTicks == 0) {
							c.getItems().removeGroundItem(i.getItemId(), i.getItemX(), i.getItemY(), i.getItemAmount());
							c.getItems().createGroundItem(i.getItemId(), i.getItemX(), i.getItemY(), i.getItemAmount());
						}
					}
				}	
			}
		}
	}
	
	public void process() {
		ArrayList<GroundItem> toRemove = new ArrayList<GroundItem>();
		for (int j = 0; j < items.size(); j++) {			
			if (items.get(j) != null) {
				GroundItem i = items.get(j);
				if(i.hideTicks > 0) {
					i.hideTicks--;
				}
				if(i.hideTicks == 1) { // item can now be seen by others
					i.hideTicks = 0;
					createGlobalItem(i);
					i.removeTicks = HIDE_TICKS;
				}
				if(i.removeTicks > 0) {
					i.removeTicks--;
				}
				if(i.removeTicks == 1) {
					i.removeTicks = 0;
					toRemove.add(i);
					//removeGlobalItem(i, i.getItemId(), i.getItemX(), i.getItemY(), i.getItemAmount());
				}
			
			}
		
		}
		
		for (int j = 0; j < toRemove.size(); j++) {
			GroundItem i = toRemove.get(j);
			removeGlobalItem(i, i.getItemId(), i.getItemX(), i.getItemY(), i.getItemAmount());	
		}
		/*for(GroundItem i : items) {
			if(i.hideTicks > 0) {
				i.hideTicks--;
			}
			if(i.hideTicks == 1) { // item can now be seen by others
				i.hideTicks = 0;
				createGlobalItem(i);
				i.removeTicks = HIDE_TICKS;
			}
			if(i.removeTicks > 0) {
				i.removeTicks--;
			}
			if(i.removeTicks == 1) {
				i.removeTicks = 0;
				removeGlobalItem(i, i.getItemId(), i.getItemX(), i.getItemY(), i.getItemAmount());
			}
		}*/
	}
	
	
	
	/**
	* Creates the ground item 
	**/
	public int[][] brokenBarrows = {{4708,4860},{4710,4866},{4712,4872},{4714,4878},{4716,4884},
	{4718,4890},{4720,4896},{4722,4902},{4732,4932},{4734,4938},{4736,4944},{4738,4950},{4724,4908},
	{4726,4914},{4728,4920},{4730,4926},{4745,4956},{4747,4926},{4749,4968},{4751,4974},{4753,4980},
	{4755,4986},{4757,4992},{4759,4998}};
	public void createGroundItem(Client c, int itemId, int itemX, int itemY, int itemAmount, int playerId) {
		if(itemId > 0) {
			if (itemId >= 2412 && itemId <= 2414) {
				//c.sendMessage("The cape vanishes as it touches the ground.");
				return;
			}
			if (itemId == 6570) {
			     return;
			}
			if (itemId > 4705 && itemId < 4760) {
				for (int j = 0; j < brokenBarrows.length; j++) {
					if (brokenBarrows[j][0] == itemId) {
						itemId = brokenBarrows[j][1];
						break;
					}
				}
			}
			if (!server.game.items.Item.itemStackable[itemId] && itemAmount > 0) {
				for (int j = 0; j < itemAmount; j++) {
					c.getItems().createGroundItem(itemId, itemX, itemY, 1);
					GroundItem item = new GroundItem(itemId, itemX, itemY, 1, c.playerId, HIDE_TICKS, PlayerHandler.players[playerId].playerName);
					addItem(item);
				}	
			} else {
				c.getItems().createGroundItem(itemId, itemX, itemY, itemAmount);
				GroundItem item = new GroundItem(itemId, itemX, itemY, itemAmount, c.playerId, HIDE_TICKS, PlayerHandler.players[playerId].playerName);
				addItem(item);
			}
		}
	}
	
	
	/**
	* Shows items for everyone who is within 60 squares
	**/
	public void createGlobalItem(GroundItem i) {
		for (Player p : PlayerHandler.players){
			if(p != null) {
			Client person = (Client)p;
			if(person != null && !p.inFightCaves()){
					if(person.playerId != i.getItemController()) {
						if (!person.getItems().tradeable(i.getItemId()) && person.playerId != i.getItemController())
							continue;
						if (person.distanceToPoint(i.getItemX(), i.getItemY()) <= 60) {
							person.getItems().createGroundItem(i.getItemId(), i.getItemX(), i.getItemY(), i.getItemAmount());
						}
					}
				}
			}
		}
	}
			

			
	/**
	* Removing the ground item
	**/
	
	public void removeGroundItem(Client c, int itemId, int itemX, int itemY, boolean add){
		for(GroundItem i : items) {
			if(i.getItemId() == itemId && i.getItemX() == itemX && i.getItemY() == itemY) {
				if(i.hideTicks > 0 && i.getName().equalsIgnoreCase(c.playerName)) {
					if(add) {
						if(c.safeTimer > 0 && c.isRogue && c.inBhArea())
							BountyHunter.startLeaveTimer(c);
						if (!c.getItems().specialCase(itemId)) {
							if(c.getItems().addItem(i.getItemId(), i.getItemAmount())) {   
								removeControllersItem(i, c, i.getItemId(), i.getItemX(), i.getItemY(), i.getItemAmount());
								break;
							}
						} else {
							c.getItems().handleSpecialPickup(itemId);
							removeControllersItem(i, c, i.getItemId(), i.getItemX(), i.getItemY(), i.getItemAmount());
							break;
						}
					} else {
						removeControllersItem(i, c, i.getItemId(), i.getItemX(), i.getItemY(), i.getItemAmount());
						break;
					}
				} else if (i.hideTicks <= 0) {
					if(add) {
						if(c.safeTimer > 0 && c.isRogue && c.inBhArea())
							BountyHunter.startLeaveTimer(c);
						if(c.getItems().addItem(i.getItemId(), i.getItemAmount())) {  
							removeGlobalItem(i, i.getItemId(), i.getItemX(), i.getItemY(), i.getItemAmount());
							break;
						}
					} else {
						removeGlobalItem(i, i.getItemId(), i.getItemX(), i.getItemY(), i.getItemAmount());
						break;
					}
				}
			}
		}
	}
	
	/**
	* Remove item for just the item controller (item not global yet)
	**/
	
	public void removeControllersItem(GroundItem i, Client c, int itemId, int itemX, int itemY, int itemAmount) {
		c.getItems().removeGroundItem(itemId, itemX, itemY, itemAmount);
		removeItem(i);
	}
	
	/**
	* Remove item for everyone within 60 squares
	**/
	
	public void removeGlobalItem(GroundItem i, int itemId, int itemX, int itemY, int itemAmount) {
		for (Player p : PlayerHandler.players){
			if(p != null) {
			Client person = (Client)p;
				if(person != null){
					if (person.distanceToPoint(itemX, itemY) <= 60) {
						person.getItems().removeGroundItem(itemId, itemX, itemY, itemAmount);
					}
				}
			}
		}
		removeItem(i);
	}
		
	

	/**
	*Item List
	**/
	
	public ItemList ItemList[] = new ItemList[Config.ITEM_LIMIT];
	

	public void newItemList(int ItemId, String ItemName, String ItemDescription, double ShopValue, double LowAlch, double HighAlch, int Bonuses[]) {
		// first, search for a free slot
		int slot = ItemId;

		if(slot == -1) return;		// no free slot found
		ItemList newItemList = new ItemList(ItemId);
		newItemList.itemName = ItemName;
		newItemList.itemDescription = ItemDescription;
		newItemList.ShopValue = ShopValue;
		newItemList.LowAlch = LowAlch;
		newItemList.HighAlch = HighAlch;
		newItemList.Bonuses = Bonuses;
		newItemList.req = getRequirements(ItemName.toLowerCase(),ItemId);
		ItemList[slot] = newItemList;
	}
	
	public int[] getRequirements(String itemName, int itemId) {
	int[] req = new int[24];
	req[0] = 0; req[1] = 0; req[2] = 0; req[3] = 0; req[4] = 0; req[5] = 0; req[6] = 0;
	if(itemName.contains("mystic") || itemName.contains("nchanted")) {
		if (itemName.contains("staff of light")) {
			req[6]  = 75;
			req[0]  = 75;		
		}
			if(itemName.contains("staff")) {
				req[6]   = 20;
				req[0]  = 40;
			} else {
				req[6]   = 20;
				req[1]  = 20;
			}
		}

		
		if (itemName.contains("infinity")) {
			req[6]  = 50;
			req[1]  = 25;		
		} else
		if(itemName.contains("splitbark")) {
			req[6] = 40;
			req[1]  = 40;
		} else
		if (itemName.contains("rune c'bow")) {
			req[4]   = 61;
		} else
		if (itemName.contains("black d'hide")) {
			req[4]   = 70;
		} else
		if (itemName.contains("tzhaar-ket-om")) {
			req[2]   = 60;
		} else
		if (itemName.contains("red d'hide")) {
			req[4]   = 60;
		} else
		if (itemName.contains("blue d'hide")) {
			req[4]   = 50;
		} else
		if (itemName.contains("green d'hide")) {
			req[4]   = 40;
		} else
		if (itemName.contains("snakeskin")) {
			req[4]   = 40;
			req[1]  = 30;
		} else
		if (itemName.contains("initiate")) {
			req[1]  = 20;
		} else
		if(itemName.contains("bronze")) {
			if(itemName.contains("scimitar") || itemName.contains("sword") || itemName.contains("dagger") || itemName.contains("mace") || itemName.contains("warhammer") || itemName.contains("2h") || itemName.contains("axe") || itemName.contains("halberd") || itemName.contains("spear") && !itemName.contains("knife") && !itemName.contains("dart") && !itemName.contains("javelin") && !itemName.contains("thrownaxe") && !itemName.contains("arrow") && !itemName.contains("bolt")) {
				req[0]  = 1;
			} else
			if(!itemName.contains("knife") && !itemName.contains("dart") && !itemName.contains("javelin") && !itemName.contains("thrownaxe") && !itemName.contains("bolt") && !itemName.contains("arrow")) {
				req[0]  = req[1]  = 1;
			}
		
		} else
		if(itemName.contains("iron")) {
			if(itemName.contains("scimitar") || itemName.contains("sword") || itemName.contains("dagger") || itemName.contains("mace") || itemName.contains("warhammer") || itemName.contains("2h") || itemName.contains("axe") || itemName.contains("halberd") || itemName.contains("spear") && !itemName.contains("knife") && !itemName.contains("dart") && !itemName.contains("javelin") && !itemName.contains("thrownaxe") && !itemName.contains("arrow") && !itemName.contains("bolt")) {
				req[0]  = 1;
			} else
			if(!itemName.contains("knife") && !itemName.contains("dart") && !itemName.contains("javelin") && !itemName.contains("thrownaxe") && !itemName.contains("bolt") && !itemName.contains("arrow")) {
				req[0]  = req[1]  = 1;
			}	
			
		} else
		if(itemName.contains("steel")) {
			if(itemName.contains("scimitar") || itemName.contains("sword") || itemName.contains("dagger") || itemName.contains("mace") || itemName.contains("warhammer") || itemName.contains("2h") || itemName.contains("axe") || itemName.contains("halberd") || itemName.contains("spear") && !itemName.contains("knife") && !itemName.contains("dart") && !itemName.contains("javelin") && !itemName.contains("thrownaxe") && !itemName.contains("arrow") && !itemName.contains("bolt")) {
				req[0]  = 5;
			} else
			if(!itemName.contains("knife") && !itemName.contains("dart") && !itemName.contains("javelin") && !itemName.contains("thrownaxe") && !itemName.contains("bolt") && !itemName.contains("arrow")) {
				req[0]  = req[1]  = 5;
			}
			
		} else
		if(itemName.contains("black")) {
			if(itemName.contains("scimitar") || itemName.contains("sword") || itemName.contains("dagger") || itemName.contains("mace") || itemName.contains("warhammer") || itemName.contains("2h") || itemName.contains("axe") || itemName.contains("halberd") || itemName.contains("spear") && !itemName.contains("knife") && !itemName.contains("dart") && !itemName.contains("javelin") && !itemName.contains("thrownaxe") && !itemName.contains("arrow") && !itemName.contains("bolt")) {
				req[0]  = 10;
			} else
			if(!itemName.contains("knife") && !itemName.contains("dart") && !itemName.contains("javelin") && !itemName.contains("thrownaxe") && !itemName.contains("vamb") && !itemName.contains("chap") && !itemName.contains("bolt") && !itemName.contains("arrow") && !itemName.contains("ele'") && !itemName.contains("beret")) {
				req[0]  = req[1]  = 10;
			}
			
		} else
		if(itemName.contains("mithril")) {
			if(itemName.contains("scimitar") || itemName.contains("sword") || itemName.contains("dagger") || itemName.contains("mace") || itemName.contains("warhammer") || itemName.contains("2h") || itemName.contains("axe") || itemName.contains("halberd") || itemName.contains("spear") && !itemName.contains("knife") && !itemName.contains("dart") && !itemName.contains("javelin") && !itemName.contains("thrownaxe") && !itemName.contains("arrow") && !itemName.contains("bolt")) {
				req[0]  = 20;
			} else
			if(!itemName.contains("knife") && !itemName.contains("dart") && !itemName.contains("javelin") && !itemName.contains("thrownaxe") && !itemName.contains("bolt") && !itemName.contains("arrow")) {
				req[0]  = req[1]  = 20;
			}
			
		} else
		if(itemName.contains("adamant")) {
			if(itemName.contains("scimitar") || itemName.contains("sword") || itemName.contains("dagger") || itemName.contains("mace") || itemName.contains("warhammer") || itemName.contains("2h") || itemName.contains("axe") || itemName.contains("halberd") || itemName.contains("spear") && !itemName.contains("knife") && !itemName.contains("dart") && !itemName.contains("javelin") && !itemName.contains("thrownaxe") && !itemName.contains("arrow") && !itemName.contains("bolt")) {
				req[0]  = 30;
			} else
			if(!itemName.contains("knife") && !itemName.contains("dart") && !itemName.contains("javelin") && !itemName.contains("thrownaxe") && !itemName.contains("bolt") && !itemName.contains("arrow")) {
				req[0]  = req[1]  = 30;
			}
			
		} else
		if(itemName.contains("gilded")) {
			req[1] = 40;
		} else
		if(itemName.contains("rune")) {
			if(itemName.contains("scimitar") || itemName.contains("sword") || itemName.contains("dagger") || itemName.contains("mace") || itemName.contains("warhammer") || itemName.contains("2h") || itemName.contains("axe") || itemName.contains("halberd") || itemName.contains("spear") && !itemName.contains("knife") && !itemName.contains("dart") && !itemName.contains("javelin") && !itemName.contains("thrownaxe") && !itemName.contains("arrow") && !itemName.contains("bolt")) {
				req[0]  = 40;
			} else
			if(!itemName.contains("knife") && !itemName.contains("dart") && !itemName.contains("javelin") && !itemName.contains("thrownaxe") && !itemName.contains("'bow") && !itemName.contains("bolt") && !itemName.contains("arrow")) {
				req[1]= 40;
			}
			
		} else
		if(itemName.contains("guthix") || itemName.contains("zamorak") || itemName.contains("sara")) {
			if(!itemName.contains("sword") && !itemName.contains("robe") && !itemName.contains("cape") && !itemName.contains("staff") && !itemName.contains("banner") && !itemName.contains("mjolnir") && !itemName.contains("symbol") && !itemName.contains("bracers") && !itemName.contains("d'hide") && !itemName.contains("chaps") && !itemName.contains("coif") && !itemName.contains("crozier") && !itemName.contains("cloak") && !itemName.contains("mitre") && !itemName.contains("stole")) {
				req[1] = 40;	
			}
		} else
		if(itemName.contains("granite shield")) {
			if(!itemName.contains("maul")){
				req[1]  = 50	;
			}
			
		} else
		if(itemName.contains("granite maul")) {
			if(!itemName.contains("shield")){
				req[0]  = 50	;
			}
			
		} else
		if(itemName.contains("warrior")) {
			if(!itemName.contains("ring")){
				req[1]  = 40	;
			}
			
		} else
		if(itemName.contains("dragonfire")) {
				req[1]  = 70	;
		} else
		if(itemName.contains("enchanted")) {
				req[1]  = 40;
		} else
		if(itemName.contains("d'hide")) {
			if(!itemName.contains("chaps")){
				req[1]  = req[4]   = 40	;
			}
			
		} else
		if(itemName.contains("dragon dagger")) {
			
				req[0]  = 60;
		} else
		if(itemName.contains("drag dagger")) {
			
				req[0]  = 60;
		} else
		if(itemName.contains("ancient")) {
			
				req[0]  = 50;
		} else
		if(itemName.contains("hardleather")) {	
				req[1]  = 10;
		} else
		if(itemName.contains("studded")) {
				req[1]  = 20;
		} else
		if(itemName.contains("bandos")) {
			if (!itemName.contains("godsword")) {
				req[2]  = req[1]  = 65;
			}
		} else
			if(itemName.contains("dragon")) {
				if(itemName.contains("nti-") && !itemName.contains("fire")) {
					req[1] = 1;
				} else
				if(itemName.contains("dragonfire")) {
					req[1] = 70;
				} else
				if(itemName.contains("scimitar") || itemName.contains("sword") || itemName.contains("dagger") || itemName.contains("mace") || itemName.contains("warhammer") || itemName.contains("2h") || itemName.contains("axe") || itemName.contains("halberd") || itemName.contains("spear") && (!itemName.contains("nti-") || !itemName.contains("fire") && !itemName.contains("bolt") && !itemName.contains("arrow") && !itemName.contains("knife") && !itemName.contains("dart") && !itemName.contains("javelin") && !itemName.contains("thrownaxe") && !itemName.contains("arrow") && !itemName.contains("bolt"))) {
					req[0]  = 60;
				} else
				if ((!itemName.contains("nti-") || !itemName.contains("fire")) && (!itemName.contains("bolt") && !itemName.contains("arrow"))) {
					req[1]  = 60;
				}
		} else
		if(itemName.contains("crystal")) {
			if(itemName.contains("shield")) {	
				req[1]  = 70;
			} else {
				req[4]   = 70;
			}
			
		} else
		if(itemName.contains("ahrim")) {
			if(itemName.contains("staff")) {
				req[6]   = 70;
				req[0]  = 70;
			} else {
				req[6]   = 70;
				req[1]  = 70;
			}
		} else
		if(itemName.contains("karil")) {
			if(itemName.contains("crossbow")) {
				req[4]   = 70;
			} else {
				req[4]   = 70;
				req[1]  = 70;
			}
		} else
		if(itemName.contains("armadyl")) {
			if(itemName.contains("godsword")) {
				req[0]  = 75;
			
			} else {
			req[4]   = req[1]  = 65;
			}
		} else
		if(itemName.contains("saradomin")) {
			if(itemName.contains("sword")) {
				req[0]  = 70;
			} 
			if(itemName.contains("crozier")) {
				req[0]  = 1;
			if(itemName.contains("robe")) {
				req[0]  = 1;
			
			}else {
				req[1]  = 40;
			
}
} 
		} else
		if(itemName.contains("godsword")) {
			req[0]  = 75;
		} else
		if (itemName.contains("3rd age") && !itemName.contains("amulet")) {
			req[1]  = 60;
		} else
		if(itemName.contains("verac") || itemName.contains("guthan") || itemName.contains("dharok") || itemName.contains("torag")) {
			if(itemName.contains("hammers")) {
				req[0]  = 70;
				req[2]   = 70;
			} else if(itemName.contains("axe")) {
				req[0]  = 70;
				req[2] = 70;
			} else if(itemName.contains("warspear")) {
				req[0] = 70;
				req[2]   = 70;
			} else if(itemName.contains("flail")) {
				req[0]  = 70;
				req[2]   = 70;
			} else {
				req[1]  = 70;
			} 
		}
			
		switch(itemId) {
		case 2653:
		case 2655:
		case 2657:
		case 2659:
		case 2661:
		case 2663:
		case 2665:
		case 2667:
		case 2669:
		case 2671:
		case 2673:
		case 2675:
			req[1] = 40;
			break;
			
			case 8839:
			case 8840:
			case 8842:
			case 11663:
			case 11664:
			case 11665:
			req[0]  = 42;
			req[4]   = 42;
			req[2]   = 42;
			req[6]   = 42;
			req[1]  = 42;
			break;
			case 6528:
			req[2]   = 50;
			break;
			case 10551:
			case 2503:
			case 2501:
			case 2499:
			case 1135:
			req[1]  = 40;
			break;
			case 11235:
			case 6522:
			req[4]   = 60;
			break;
			case 6524:
			req[1]  = 60;
			break;
			case 11284:
			case 11283:
			req[1]  = 75;
			break;
			case 6889:
			case 6914:
			req[6]   = 60;
			break;
			case 13905:
			case 13899:
			case 13902:
			req[0]  = 78;
			break;
			case 13893: // Pvp armor
			case 13877:
			case 13890:
			case 13884:
			case 13896:
			req[1]  = 78;
			break;

			case 13876:
			case 13870:
			case 13873:
			req[1]  = 78;
			break;

			case 13858:
			case 13862:
			case 13864:
			req[6]   = 78;
			req[1]  = 78;
			break;

			case 13736:
			case 13734:
			case 13740:
			case 13742:
			case 13738:
			case 13744:
			req[1]  = 75;
			break;

			case 13882:
			case 13881:
			case 13880:
			case 13879:
			case 13883:
			req[4]   = 78;
			break;

			case 861:
			req[4]   = 50;
			break;
			case 20171:
			req[4]   = 80;
			break;
			
			case 20135:
			case 20139:
			case 20143:
			case 20147:
			case 20151:
			case 20155:
			case 20159:
			case 20163:
			case 20167:
			req[0]  = 80;
			req[2]   = 80;
			req[1]  = 80;
			req[6]   = 80;
			req[4]   = 80;
			break;

			case 10828:
			req[1]  = 55;
			break;
			case 11724:
			case 11726:
			case 11728:
				req[1]  = 65;
			break;
			case 3751:
			case 3749:
			case 3755:
			req[1]  = 40;
			break;
			
			case 7462:
			case 7461:
			req[1]  = 40;
			break;
			case 8846:
			req[1]  = 5;
			break;
			case 8847:
			req[1]  = 10;
			break;
			case 8848:
			req[1]  = 20;
			break;
			case 8849:
			req[1]  = 30;
			break;

			case 8850:
			req[1]  = 40;
			break;
			case 20072:
			req[1]  = 60;
			break;
			case 10887:
			req[2]   = 60;
			break;

			
			case 7460:
			req[1]  = 20;
			break;
			
			case 15071:
			req[1]  = 60;
			break;

			case 837:
			req[4]   = 61;
			break;
			
			case 15441:
			case 15442:
			case 15443:
			case 15444:
			case 4151: // if you don't want to use names 
			req[0]  = 70;
			break;
			
			case 6724: // seercull
			req[4]   = 50; // idk if that is correct
			break;
			
			case 1319: //rune 2h
			req[0] = 40;
			break;

			case 14484: // dclaw
			req[0]  = 60;
			break;

			case 15241: //hand cannon
			req[4]   = 61;
			break;

			case 18349: //c rapier
			req[0]  = 80;
			break;

			case 18351: //cls
			req[0]  = 80;
			break;

			case 18353: //c maul
			req[0]  = 80;
			break;
			case 16425:
			req[0]  = 99;
			break;

			case 18355: //c staff
			req[0]  = 80;
			req[6]   = 80;
			break;

			case 18357: //c cbow
			req[4]   = 80;
			break;

			case 15042: //eagle-eye
			req[4]   = 80;
			req[1]  = 80;
			break;

			case 15043: //c kite
			req[1]  = 80;
			break;

			case 4153:
			req[0]  = 50;
			req[2]  = 50;
			break;
		}
		return req;
	}
	
	public void loadItemPrices(String filename) {
		try {
			Scanner s = new Scanner(new File("./data/cfg/" + filename));
			while (s.hasNextLine()) {
				String[] line = s.nextLine().split(" ");
				ItemList temp = getItemList(Integer.parseInt(line[0]));
				if (temp != null)
					temp.ShopValue = Integer.parseInt(line[1]);			
			}		
			s.close();
		} catch (IOException e) {
			e.printStackTrace();		
		}
	}
	
	public ItemList getItemList(int i) {
		for (int j = 0; j < ItemList.length; j++) {
			if (ItemList[j] != null) {
				if (ItemList[j].itemId == i) {
					return ItemList[j];				
				}		
			}		
		}
		return null;
	}
	
	public boolean loadItemList(String FileName) {
		String line = "";
		String token = "";
		String token2 = "";
		String token2_2 = "";
		String[] token3 = new String[10];
		boolean EndOfFile = false;
		@SuppressWarnings("unused")
		int ReadMode = 0;
		BufferedReader characterfile = null;
		try {
			characterfile = new BufferedReader(new FileReader("./Data/cfg/"+FileName));
		} catch(FileNotFoundException fileex) {
			Misc.println(FileName+": file not found.");
			return false;
		}
		try {
			line = characterfile.readLine();
		} catch(IOException ioexception) {
			Misc.println(FileName+": error loading file.");
			try {
				characterfile.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//return false;
		}
		while(EndOfFile == false && line != null) {
			line = line.trim();
			int spot = line.indexOf("=");
			if (spot > -1) {
				token = line.substring(0, spot);
				token = token.trim();
				token2 = line.substring(spot + 1);
				token2 = token2.trim();
				token2_2 = token2.replaceAll("\t\t", "\t");
				token2_2 = token2_2.replaceAll("\t\t", "\t");
				token2_2 = token2_2.replaceAll("\t\t", "\t");
				token2_2 = token2_2.replaceAll("\t\t", "\t");
				token2_2 = token2_2.replaceAll("\t\t", "\t");
				token3 = token2_2.split("\t");
				if (token.equals("item")) {
					int[] Bonuses = new int[12];
					for (int i = 0; i < 12; i++) {
						if (token3[(6 + i)] != null) {
							Bonuses[i] = Integer.parseInt(token3[(6 + i)]);
						} else {
							break;
						}
					}
					newItemList(Integer.parseInt(token3[0]), token3[1].replaceAll("_", " "), token3[2].replaceAll("_", " "), Double.parseDouble(token3[4]), Double.parseDouble(token3[4]), Double.parseDouble(token3[6]), Bonuses);
				}
			} else {
				if (line.equals("[ENDOFITEMLIST]")) {
					try { characterfile.close(); } catch(IOException ioexception) { }
					return true;
				}
			}
			try {
				line = characterfile.readLine();
			} catch(IOException ioexception1) { EndOfFile = true; }
		}
		try { characterfile.close(); } catch(IOException ioexception) { }
		return false;
	}
}
