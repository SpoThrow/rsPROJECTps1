package server.game.shops;

import server.Config;
import server.Server;
import server.game.items.Item;
import server.game.items.ItemAssistant;
import server.game.players.Client;
import server.game.players.PlayerHandler;
import server.world.ShopHandler;

/**
 * Shops rewritten
 * @author Acquittal
 * 
 **/

public class ShopAssistant {

	private Client c;
	
	public ShopAssistant(Client client) {
		this.c = client;
	}
	
	/**
	*Shops
	**/
	
	public void openShop(int ShopID){		
		c.getItems().resetItems(3823);
		resetShop(ShopID);
		c.isShopping = true;
		c.myShopId = ShopID;
		c.getPA().sendFrame248(3824, 3822);
		c.getPA().sendFrame126(ShopHandler.ShopName[ShopID], 3901);
	}
	
	public void updatePlayerShop() {
		for (int i = 1; i < Config.MAX_PLAYERS; i++) {
			if (PlayerHandler.players[i] != null) {
				if (PlayerHandler.players[i].isShopping == true && PlayerHandler.players[i].myShopId == c.myShopId && i != c.playerId) {
					PlayerHandler.players[i].updateShop = true;
				}
			}
		}
	}
	
	public boolean shopSellsItem(int itemID) {
		for (int i = 0; i < ShopHandler.ShopItems.length; i++) {
			if(itemID == (ShopHandler.ShopItems[c.myShopId][i] - 1)) {
				return true;
			}
		}
		return false;
	}
	
	
	public void updateshop(int i){
		resetShop(i);
	}
	
	public void resetShop(int ShopID) {
		synchronized(c) {
			int TotalItems = 0;
			for (int i = 0; i < ShopHandler.MaxShopItems; i++) {
				if (ShopHandler.ShopItems[ShopID][i] > 0) {
					TotalItems++;
				}
			}
			if (TotalItems > ShopHandler.MaxShopItems) {
				TotalItems = ShopHandler.MaxShopItems;
			}
			c.getOutStream().createFrameVarSizeWord(53);
			c.getOutStream().writeWord(3900);
			c.getOutStream().writeWord(TotalItems);
			int TotalCount = 0;
			for (int i = 0; i < ShopHandler.ShopItems.length; i++) {
				if (ShopHandler.ShopItems[ShopID][i] > 0 || i <= ShopHandler.ShopItemsStandard[ShopID]) {
					if (ShopHandler.ShopItemsN[ShopID][i] > 254) {
						c.getOutStream().writeByte(255); 					
						c.getOutStream().writeDWord_v2(ShopHandler.ShopItemsN[ShopID][i]);	
					} else {
						c.getOutStream().writeByte(ShopHandler.ShopItemsN[ShopID][i]);
					}
					if (ShopHandler.ShopItems[ShopID][i] > Config.ITEM_LIMIT || ShopHandler.ShopItems[ShopID][i] < 0) {
						ShopHandler.ShopItems[ShopID][i] = Config.ITEM_LIMIT;
					}
					c.getOutStream().writeWordBigEndianA(ShopHandler.ShopItems[ShopID][i]);
					TotalCount++;
				}
				if (TotalCount > TotalItems) {
					break;
				}
			}
			c.getOutStream().endFrameVarSizeWord();
			c.flushOutStream();	
		}
	}
	
	
	public double getItemShopValue(int ItemID, int Type, int fromSlot) {
		double ShopValue = 1;
		@SuppressWarnings("unused")
		double Overstock = 0;
		double TotPrice = 0;
		for (int i = 0; i < Config.ITEM_LIMIT; i++) {
			if (Server.itemHandler.ItemList[i] != null) {
				if (Server.itemHandler.ItemList[i].itemId == ItemID) {
					ShopValue = Server.itemHandler.ItemList[i].ShopValue;
				}
			}
		}
		
		TotPrice = ShopValue;

		if (ShopHandler.ShopBModifier[c.myShopId] == 1) {
			TotPrice *= 1; 
			TotPrice *= 1;
			if (Type == 1) {
				TotPrice *= 1; 
			}
		} else if (Type == 1) {
			TotPrice *= 1; 
		}
		return TotPrice;
	}
	
	public int getItemShopValue(int itemId) {
		for (int i = 0; i < Config.ITEM_LIMIT; i++) {
			if (Server.itemHandler.ItemList[i] != null) {
				if (Server.itemHandler.ItemList[i].itemId == itemId) {
					return (int)Server.itemHandler.ItemList[i].ShopValue;
				}
			}	
		}
		return 0;
	}
	
	
	
	/**
	*buy item from shop (Shop Price)
	**/
	public int getVoteItemValue(int id) {
		switch (id) {
			case 7051:
			case 10051:
			case 10045:
			case 10039:
				return 3;
			case 6865:
			case 6866:
			case 6867:
				return 1;
			case 7053:
				return 10;
			case 10035:
			case 10041:
			case 10047:
			case 10085:
			case 6665:
			case 6666:
			case 7927:
			case 6583:
				return 5;
			case 10037:
			case 10043:
			case 10049:
			case 10663:
			case 10664:
			case 4502:
			case 6859:
			case 7003:
			case 6861:
			case 6863:
			case 6857:
			case 9005:
				return 10;
			case 6858:
			case 6860:
			case 6862:
			case 6856:
			case 4079:
			case 10392:
			case 10394:
			case 10396:
			case 10398:
				return 15;
			case 4084:
				return 50;
			case 3107:
				return 10;
			case 10077:
				return 1;
			case 10079:
				return 2;
			case 10081:
				return 3;
			case 10083:
			case 4551:
			case 3759:
			case 3761:
			case 3763:
			case 3765:
			case 3767:
			case 3769:
			case 3771:
			case 3773:
			case 3775:
			case 3777:
			case 3779:
			case 3781:
			case 3783:
			case 3785:
			case 3787:
			case 3789:
				return 4;
			case 9470:
				return 10;
		}
		return 0;
	}
	public void buyFromShopPrice(int removeId, int removeSlot){
		int ShopValue = (int)Math.floor(getItemShopValue(removeId, 0, removeSlot));
		//ShopValue *= 1.35;
		String ShopAdd = "";
		switch(c.myShopId) {
		case 77:
			c.sendMessage(ItemAssistant.getItemName(removeId)+": currently costs " + getVoteItemValue(removeId) + " vote points.");
			return;
		case 73:
			c.sendMessage(ItemAssistant.getItemName(removeId)+": currently costs " + getRGItemValue(removeId) + " archery tickets.");
			return;
		case 74:
			c.sendMessage(ItemAssistant.getItemName(removeId)+": currently costs " + getAssaultItemValue(removeId) + " assault points.");
			return;
		case 75:
			c.sendMessage(ItemAssistant.getItemName(removeId)+": currently costs " + getPestItemValue(removeId) + " pest control points.");
			return;
		case 78:
			c.sendMessage(ItemAssistant.getItemName(removeId)+": currently costs " + getCWItemValue(removeId) + " castle wars tickets.");
			return;
		case 65:
			c.sendMessage(ItemAssistant.getItemName(removeId)+": currently costs " + getFMItemValue(removeId) + " firemaking points.");
			return;
		case 19:
		case 20:
			c.sendMessage(ItemAssistant.getItemName(removeId)+": currently costs " + ShopValue+ " tokkul"+ShopAdd);
			return;
			
		}
		if (c.myShopId != 19 && c.myShopId != 20 && c.myShopId != 39) {
			if (ShopValue >= 1000 && ShopValue < 1000000) {
				ShopAdd = " (" + (ShopValue / 1000) + "K)";
			} else if (ShopValue >= 1000000) {
				ShopAdd = " (" + (ShopValue / 1000000) + " million)";
			}
			c.sendMessage(ItemAssistant.getItemName(removeId)+": currently costs "+ShopValue+" coins"+ShopAdd);
		}
	}
	
	public int getRGItemValue(int id) {
		switch (id) {
		case 2581: //ranger hat
			return 7500;
		case 2577: //ranger boots
			return 6800;
		case 7370: //green body (g)
			return 5500;
		case 7374: //blue body (g)
			return 6100;
		case 7378: //green chaps (g)
			return 5300;
		case 7382: //blue chaps (g)
			return 5900;
		}
		return 0;
	}
	
	public int getCWItemValue(int id) {
		switch (id) {
		case 4068: //sword
		case 4503: //sword
		case 4508:
			return 220;
		case 4069: //plate
		case 4504:
		case 4509:
			return 810;
		case 4070: //legs
		case 4505:
		case 4510:
			return 740;
		case 4071: //helm
		case 4506:
		case 4511:
			return 90;
		case 4072: //kite
		case 4507:
		case 4512:
			return 430;
		case 1052:
			return 75;
		}
		return 0;
	}
	
	public int getAssaultItemValue(int id) {
		switch (id) {
		case 10551: //fighter torso
			return 110;
		case 10548: //fighter hat
			return 15;
		case 11732: //dragon boots
			return 85;
		case 1434: //dragon mace
			return 25;
		case 7454: //bronze
			return 1;
		case 7455: //iron
			return 3;
		case 7456: //steel
			return 5;
		case 7457: //black
			return 8;
		case 7458: //mith
			return 10;
		case 7459: //addy
			return 12;
		case 7460: //rune
			return 16;
		case 7461: //dragon
			return 23;
		case 7462: //barrows
			return 32;
		case 8850: //rune def
			return 55;
		case 9672: //proselyte
			return 10;
		case 9674: //proselyte
			return 15;
		case 9676: //proselyte
			return 15;
		case 10499: //ava
			return 65;
		}
		return 0;
	}
	
	public int getPestItemValue(int id) {
		switch (id) {
		case 8839: //body
			return 75;
		case 8840: //bottom
			return 60;
		case 11663: //mage helm
		case 11664: //range helm
		case 11665: //melee helm
			return 55;
		case 8842: //gloves
			return 15;
		}
		return 0;
	}
	
	public int getFMItemValue(int id) {
		switch (id) {
		case 7404:
		case 7405:
		case 7406:
		case 10328:
		case 10329: //coloured logs
			return 10;
		case 3481: //gilded plate
			return 125;
		case 3483: //gilded legs
			return 95;
		case 3485: //gilded skirt
			return 80;
		case 3486: //gilded helm
			return 70;
		case 3488: //gilded kite
			return 110;
		}
		return 0;
	}
	
	
	
	/**
	*Sell item to shop (Shop Price)
	**/
	public void sellToShopPrice(int removeId, int removeSlot) {
		for (int i : Config.ITEM_SELLABLE) {
			if (i == removeId) {
				c.sendMessage("You can't sell "+ItemAssistant.getItemName(removeId).toLowerCase() +".");
				return;
			}
		}
		boolean IsIn = false;
		if (ShopHandler.ShopSModifier[c.myShopId] > 1) {
		for (int j = 0; j <= ShopHandler.ShopItemsStandard[c.myShopId]; j++) {
		if (removeId == (ShopHandler.ShopItems[c.myShopId][j] - 1)) {
		IsIn = true;
		break;
		}
		}
		} else {
		IsIn = true;
		}
		
		if(ShopHandler.ShopName[c.myShopId].contains("General"))
			IsIn = true;
		
		if (IsIn == false) {
		c.sendMessage("You can't sell "+ItemAssistant.getItemName(removeId).toLowerCase() +" to this store.");
		} else {
		int ShopValue = (int)Math.round(getItemShopValue(removeId, 1, removeSlot) * 0.80);
		String ShopAdd = "";
		if (ShopValue >= 1000 && ShopValue < 1000000) {
		ShopAdd = " (" + (ShopValue / 1000) + "K)";
		} else if (ShopValue >= 1000000) {
		ShopAdd = " (" + (ShopValue / 1000000) + " million)";
		}
		c.sendMessage(ItemAssistant.getItemName(removeId)+" : shop will buy for "+ShopValue+" coins"+ShopAdd);
		}
		}
	
	
	
	public boolean sellItem(int itemID, int fromSlot, int amount) {
		if(!c.isShopping)
			return false;
		if (c.myShopId == 14) {
			return false;
		}
		if(c.inTrade) {
			return false;
		}
		for (int i : Config.ITEM_SELLABLE) {
			if (i == itemID) {
				c.sendMessage("You can't sell "+ItemAssistant.getItemName(itemID).toLowerCase()+".");
				return false;
			} 
		}
		if (amount > 0 && itemID == (c.playerItems[fromSlot] - 1)) {
			if (ShopHandler.ShopSModifier[c.myShopId] > 1) {
				boolean IsIn = false;
				for (int i = 0; i <= ShopHandler.ShopItemsStandard[c.myShopId]; i++) {
					if (itemID == (ShopHandler.ShopItems[c.myShopId][i] - 1)) {
						IsIn = true;
						break;
					}
				}
				if(ShopHandler.ShopName[c.myShopId].contains("General"))
					IsIn = true;
				
				if (IsIn == false) {
					c.sendMessage("You can't sell "+ItemAssistant.getItemName(itemID).toLowerCase()+" to this store.");
					return false;
				}
			}

			if (amount > c.playerItemsN[fromSlot] && (Item.itemIsNote[(c.playerItems[fromSlot] - 1)] == true || Item.itemStackable[(c.playerItems[fromSlot] - 1)] == true)) {
				amount = c.playerItemsN[fromSlot];
			} else if (amount > c.getItems().getItemAmount(itemID) && Item.itemIsNote[(c.playerItems[fromSlot] - 1)] == false && Item.itemStackable[(c.playerItems[fromSlot] - 1)] == false) {
				amount = c.getItems().getItemAmount(itemID);
			}
			//double ShopValue;
			//double TotPrice;
			int TotPrice2 = 0;
			//int Overstock;
			for (int i = amount; i > 0; i--) {
				TotPrice2 = (int)Math.floor(getItemShopValue(itemID, 1, fromSlot));
				if (c.getItems().freeSlots() > 0 || c.getItems().playerHasItem(995)) {
					if (Item.itemIsNote[itemID] == false) {
						c.getItems().deleteItem(itemID, c.getItems().getItemSlot(itemID), 1);
					} else {
						c.getItems().deleteItem(itemID, fromSlot, 1);
					}
					c.getItems().addItem(995, TotPrice2 *= 0.80);
					//addShopItem(itemID, 1);
				} else {
					c.sendMessage("You don't have enough space in your inventory.");
					break;
				}
			}
			c.getItems().resetItems(3823);
			resetShop(c.myShopId);
			updatePlayerShop();
			return true;
		}
		return true;
	}
	
	public boolean addShopItem(int itemID, int amount) {
		boolean Added = false;
		if (amount <= 0) {
			return false;
		}
		if (Item.itemIsNote[itemID] == true) {
			itemID = c.getItems().getUnnotedItem(itemID);
		}
		for (int i = 0; i < ShopHandler.ShopItems.length; i++) {
			if ((ShopHandler.ShopItems[c.myShopId][i] - 1) == itemID) {
				ShopHandler.ShopItemsN[c.myShopId][i] += amount;
				Added = true;
			}
		}
		if (Added == false) {
			for (int i = 0; i < ShopHandler.ShopItems.length; i++) {
				if (ShopHandler.ShopItems[c.myShopId][i] == 0) {
					ShopHandler.ShopItems[c.myShopId][i] = (itemID + 1);
					ShopHandler.ShopItemsN[c.myShopId][i] = amount;
					ShopHandler.ShopItemsDelay[c.myShopId][i] = 0;
					break;
				}
			}
		}
		return true;
	}
	
	public boolean buyItem(int itemID, int fromSlot, int amount) {
		if(!c.isShopping)
			return false;
		if(!shopSellsItem(itemID)) {
			return false;
		}
		if (c.myShopId == 78 || c.myShopId == 77 || c.myShopId == 73 || c.myShopId == 74 || c.myShopId == 75 || c.myShopId == 61 || c.myShopId == 65) {
			handleOtherShop(itemID);
			return false;
		}
		if (amount > 0) {
			if (amount > ShopHandler.ShopItemsN[c.myShopId][fromSlot]) {
				amount = ShopHandler.ShopItemsN[c.myShopId][fromSlot];
			}
			//double ShopValue;
			//double TotPrice;
			int TotPrice2 = 0;
			//int Overstock;
			int Slot = 0;
			int Slot1 = 0;//Tokkul
			@SuppressWarnings("unused")
			int Slot2 = 0;//Pking Points
			@SuppressWarnings("unused")
			int Slot3 = 0;
			for (int i = amount; i > 0; i--) {
				TotPrice2 = (int)Math.floor(getItemShopValue(itemID, 0, fromSlot));
				Slot = c.getItems().getItemSlot(995);
				Slot1 = c.getItems().getItemSlot(6529);
				Slot2 = c.getItems().getItemSlot(2996);
				if (Slot == -1 && c.myShopId != 19 && c.myShopId != 20) {
					c.sendMessage("You don't have enough coins.");
					break;
				}
				if(Slot1 == -1 && (c.myShopId == 19 || c.myShopId == 20)) {
					c.sendMessage("You don't have enough tokkul.");
					break;
				}
                if(TotPrice2 <= 1) {
                	TotPrice2 = (int)Math.floor(getItemShopValue(itemID, 0, fromSlot));
                	TotPrice2 *= 1.66;
                }
                if(c.myShopId != 20 && c.myShopId != 19 && c.myShopId != 47 && c.myShopId != 38) { //1337 = shop with special value like tokkul
					if (c.playerItemsN[Slot] >= TotPrice2) {
						if (c.getItems().freeSlots() > 0) {
							if (Item.itemStackable[itemID] && c.getItems().playerHasItem(995, TotPrice2*amount)) {
								c.getItems().deleteItem(995, c.getItems().getItemSlot(995), TotPrice2*amount);
								c.getItems().addItem(itemID, amount);
								ShopHandler.ShopItemsN[c.myShopId][fromSlot] -= amount;
								ShopHandler.ShopItemsDelay[c.myShopId][fromSlot] = 0;
								c.getItems().resetItems(3823);
								resetShop(c.myShopId);
								updatePlayerShop();
								return false;
							}
							c.getItems().deleteItem(995, c.getItems().getItemSlot(995), TotPrice2); //*= 1.35);
							c.getItems().addItem(itemID, 1);
							ShopHandler.ShopItemsN[c.myShopId][fromSlot] -= 1;
							ShopHandler.ShopItemsDelay[c.myShopId][fromSlot] = 0;
							if ((fromSlot + 1) > ShopHandler.ShopItemsStandard[c.myShopId]) {
								ShopHandler.ShopItems[c.myShopId][fromSlot] = 0;
							}
						} else {
							c.sendMessage("You don't have enough space in your inventory.");
							break;
						}
					} else {
						c.sendMessage("You don't have enough coins.");
						break;
					}
                }
                if(c.myShopId == 20 || c.myShopId == 19) {
                	if (c.playerItemsN[Slot1] >= TotPrice2) {
						if (c.getItems().freeSlots() > 0) {
							c.getItems().deleteItem(6529, c.getItems().getItemSlot(6529), TotPrice2);// *= 1.35);
							c.getItems().addItem(itemID, 1);
							ShopHandler.ShopItemsN[c.myShopId][fromSlot] -= 1;
							ShopHandler.ShopItemsDelay[c.myShopId][fromSlot] = 0;
							if ((fromSlot + 1) > ShopHandler.ShopItemsStandard[c.myShopId]) {
								ShopHandler.ShopItems[c.myShopId][fromSlot] = 0;
							}
						} else {
							c.sendMessage("You don't have enough space in your inventory.");
							break;
						}
					} else {
						c.sendMessage("You don't have enough tokkul.");
						break;
					}
                }
               
			}
			c.getItems().resetItems(3823);
			resetShop(c.myShopId);
			updatePlayerShop();
			return true;
		}
		return false;
	}	
	
		public void handleOtherShop(int itemID) {
				if (c.myShopId == 61) {
					if (c.votePoints >= getVoteItemValue(itemID)) {
						if (c.getItems().freeSlots() > 0){
							c.votePoints -= getVoteItemValue(itemID);
							c.getItems().addItem(itemID,1);
							c.getPA().sendFrame126("@red@[@or1@Vote@red@] Points: @or2@"+c.votePoints, 7339);
							c.getItems().resetItems(3823);
						}
					} else {
						c.sendMessage("You do not have enough vote points to buy this item.");			
					}
				}
				if (c.myShopId == 65) {
					if (c.fmPoints >= getFMItemValue(itemID)) {
						if (c.getItems().freeSlots() > 0){
							c.fmPoints -= getFMItemValue(itemID);
							c.getItems().addItem(itemID,1);
							c.getPA().sendFrame126("@red@Firemaking Points: @or2@"+c.fmPoints, 7334);
							c.getItems().resetItems(3823);
						}
					} else {
						c.sendMessage("You do not have enough firemaking points to buy this item.");			
					}
				}
				if (c.myShopId == 73) {
					if (c.getItems().playerHasItem(1464, getRGItemValue(itemID))) {
						if (c.getItems().freeSlots() > 0){
							c.getItems().deleteItem2(1464, getRGItemValue(itemID));
							c.getItems().addItem(itemID,1);
							c.getItems().resetItems(3823);
						}
					} else {
						c.sendMessage("You do not have enough archery tickets to buy this item.");			
					}
				}
				if (c.myShopId == 78) {
					if (c.getItems().playerHasItem(4067, getCWItemValue(itemID))) {
						if (c.getItems().freeSlots() > 0){
							c.getItems().deleteItem2(4067, getCWItemValue(itemID));
							c.getItems().addItem(itemID,1);
							c.getItems().resetItems(3823);
						}
					} else {
						c.sendMessage("You do not have enough archery tickets to buy this item.");			
					}
				}
				if (c.myShopId == 74) {
					if (c.assaultPoints >= getAssaultItemValue(itemID)) {
						if (c.getItems().freeSlots() > 0){
							c.assaultPoints -= getAssaultItemValue(itemID);
							c.getItems().addItem(itemID,1);
							c.getPA().sendFrame126("@red@Assault Points: @or2@"+c.assaultPoints, 7332);
							c.getItems().resetItems(3823);
						}
					} else {
						c.sendMessage("You do not have enough assault points to buy this item.");			
					}
				}
				if (c.myShopId == 77) {
					if (c.votePoints >= getVoteItemValue(itemID)) {
						if (c.getItems().freeSlots() > 0){
							c.votePoints -= getVoteItemValue(itemID);
							c.getItems().addItem(itemID,1);
							c.getPA().sendFrame126("@red@Vote Points: @or2@"+c.votePoints, 7383);
							c.getItems().resetItems(3823);
						}
					} else {
						c.sendMessage("You do not have enough vote points to buy this item.");			
					}
				}
				if (c.myShopId == 75) {
					if (c.pcPoints >= getPestItemValue(itemID)) {
						if (c.getItems().freeSlots() > 0){
							c.pcPoints -= getPestItemValue(itemID);
							c.getItems().addItem(itemID,1);
							c.getPA().sendFrame126("@red@Pest Control Points: @or2@"+c.pcPoints, 7333);
							c.getItems().resetItems(3823);
						}
					} else {
						c.sendMessage("You do not have enough pest control points to buy this item.");			
					}
				}
			}
		
		/*public void openSkillCape() {
			int capes = get99Count();
			if (capes > 1)
				capes = 1;
			else
				capes = 0;
			c.myShopId = 14;
			setupSkillCapes(capes, get99Count());		
		}*/
		
		
		
		/*public int[][] skillCapes = {{0,9747,4319,2679},{1,2683,4329,2685},{2,2680,4359,2682},{3,2701,4341,2703},{4,2686,4351,2688},{5,2689,4347,2691},{6,2692,4343,2691},
									{7,2737,4325,2733},{8,2734,4353,2736},{9,2716,4337,2718},{10,2728,4335,2730},{11,2695,4321,2697},{12,2713,4327,2715},{13,2725,4357,2727},
									{14,2722,4345,2724},{15,2707,4339,2709},{16,2704,4317,2706},{17,2710,4361,2712},{18,2719,4355,2721},{19,2737,4331,2739},{20,2698,4333,2700}};*/
		/*public int[] skillCapes = {9747,9753,9750,9768,9756,9759,9762,9801,9807,9783,9798,9804,9780,9795,9792,9774,9771,9777,9786,9810,9765};
		public int get99Count() {
			int count = 0;
			for (int j = 0; j < c.playerLevel.length; j++) {
				if (c.getLevelForXP(c.playerXP[j]) >= 99) {
					count++;				
				}			
			}		
			return count;
		}
		
		public void setupSkillCapes(int capes, int capes2) {
			synchronized(c) {
				c.getItems().resetItems(3823);
				c.isShopping = true;
				c.myShopId = 14;
				c.getPA().sendFrame248(3824, 3822);
				c.getPA().sendFrame126("Skillcape Shop", 3901);
				
				int TotalItems = 0;
				TotalItems = capes2;
				if (TotalItems > Server.shopHandler.MaxShopItems) {
					TotalItems = Server.shopHandler.MaxShopItems;
				}
				c.getOutStream().createFrameVarSizeWord(53);
				c.getOutStream().writeWord(3900);
				c.getOutStream().writeWord(TotalItems);
				int TotalCount = 0;
				for (int i = 0; i < 21; i++) {
					if (c.getLevelForXP(c.playerXP[i]) < 99)
						continue;
					c.getOutStream().writeByte(1);
					c.getOutStream().writeWordBigEndianA(skillCapes[i] + 2);
					TotalCount++;
				}
				c.getOutStream().endFrameVarSizeWord();
				c.flushOutStream();	
			}
		}
		
		public void skillBuy(int item) {
			int nn = get99Count();
			if (nn > 1)
				nn = 1;
			else
				nn = 0;			
			for (int j = 0; j < skillCapes.length; j++) {
				if (skillCapes[j] == item || skillCapes[j]+1 == item) {
					if (c.getItems().freeSlots() > 1) {
						if (c.getItems().playerHasItem(995,99000)) {
							if (c.getLevelForXP(c.playerXP[j]) >= 99) {
								c.getItems().deleteItem(995, c.getItems().getItemSlot(995), 99000);
								c.getItems().addItem(skillCapes[j] + nn,1);
								c.getItems().addItem(skillCapes[j] + 2,1);
							} else {
								c.sendMessage("You must have 99 in the skill of the cape you're trying to buy.");
							}
						} else {
							c.sendMessage("You need 99k to buy this item.");
						}
					} else {
						c.sendMessage("You must have at least 1 inventory spaces to buy this item.");					
					}				
				}		
			}
			c.getItems().resetItems(3823);			
		}
		
		public void openVoid() {
		}

		public void buyVoid(int item) {
		}
		}*/
}

