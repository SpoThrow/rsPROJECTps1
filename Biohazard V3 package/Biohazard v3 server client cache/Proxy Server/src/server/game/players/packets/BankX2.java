package server.game.players.packets;

import server.game.players.Client;
/**
 * Bank X Items
 **/
public class BankX2 implements PacketType {
	
	// Helper method to reset POS sell state
	private void resetSellState(Client c) {
		c.pos.selling = false;
		c.pos.sellStep = 0;
		c.pos.sellItemId = 0;
		c.pos.sellAmount = 0;
		c.pos.sellPrice = 0;
	}
	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		int Xamount = c.getInStream().readDWord();
		if (Xamount < 0)// this should work fine
		{
			Xamount = c.getItems().getItemAmount(c.xRemoveId);
		}
		if (Xamount == 0) {
			Xamount = 1;
		}

		if (server.game.content.ItemSpawnSearch.handleAmount(c, Xamount)) {
			return;
		}
		
		if (c.pos.editListingId > 0 && c.xInterfaceId == 43002) {
			int price = Xamount;
			long id = c.pos.editListingId;
			c.pos.editListingId = 0;
			server.game.content.PlayerOwnedShop.editListingPrice(c, id, price);
			c.getPA().openPlayerOwnedShop();
			return;
		}

		if (c.pos.buying && c.xInterfaceId == 44000) {
			c.pos.buying = false;
			int amount = Xamount;
			if (amount < 1) {
				amount = 1;
			}
			if (c.pos.buyMax > 0 && amount > c.pos.buyMax) {
				amount = c.pos.buyMax;
			}
			long listingId = c.pos.buyListingId;
			c.pos.buyListingId = 0;
			c.pos.buyMax = 0;
			if (server.game.content.PlayerOwnedShop.buyListing(c, listingId, amount)) {
				c.getPA().refreshPOSBrowse();
			} else {
				c.getPA().refreshPOSBrowse();
			}
			return;
		}

		// Handle POS sell flow - amount input (step 2)
		if (c.pos.selling && c.pos.sellStep == 2 && c.xInterfaceId == 43000) {
			if (Xamount > 0) {
				int owned = server.game.content.PlayerOwnedShop.ownedCount(c, c.pos.sellItemId);
				if (Xamount > owned) {
					Xamount = owned;
				}
				if (Xamount <= 0) {
					c.sendMessage("You don't have that item anymore.");
					resetSellState(c);
					c.getPA().openPlayerOwnedShop();
					return;
				}
				c.pos.sellAmount = Xamount;
				c.pos.sellStep = 3;
				c.xInterfaceId = 43001;
				c.sendMessage("Enter price EACH. " + Xamount + " x "
						+ server.game.content.PlayerOwnedShop.getItemName(c.pos.sellItemId) + ".");
				c.sendMessage(server.game.content.PlayerOwnedShop.priceHint(c.pos.sellItemId));
				c.getOutStream().createFrame(27);
			} else {
				c.sendMessage("Invalid amount. Please enter a positive number.");
				c.pos.selling = false;
				c.pos.sellStep = 0;
				c.getPA().openPlayerOwnedShop();
			}
			return;
		}
		
		// Handle POS sell flow - price input (step 3)
		if (c.pos.selling && c.pos.sellStep == 3 && c.xInterfaceId == 43001) {
			if (Xamount >= 0) {
				c.pos.sellPrice = Xamount;
				boolean success = server.game.content.PlayerOwnedShop.listItem(c, c.pos.sellItemId, c.pos.sellAmount, c.pos.sellPrice);
				if (success) {
					long total = (long) c.pos.sellAmount * (long) c.pos.sellPrice;
					c.sendMessage("Listed " + c.pos.sellAmount + " x "
							+ server.game.content.PlayerOwnedShop.getItemName(c.pos.sellItemId) + " @ "
							+ c.pos.sellPrice + "gp each (" + total + "gp total).");
				}
				resetSellState(c);
				c.getPA().openPlayerOwnedShop();
			} else {
				c.sendMessage("Invalid price. Please enter a non-negative number.");
				resetSellState(c);
			}
			return;
		}
			if(c.usingLevel) {
		
		c.usingLevel = false;
		if(c.attackSkill) {
				if (c.inWild())
					return;
				for (int j = 0; j < c.playerEquipment.length; j++) {
					if (c.playerEquipment[j] > 0) {
						c.sendMessage("Please remove all your equipment before using this command.");
						return;
					}
				}
				try {	
				int skill = 0;
				int level = Xamount;
				if (level > 99)
					level = 99;
				else if (level < 0)
					level = 1;
				c.skills.playerXP[skill] = c.getPA().getXPForLevel(level)+5;
				c.skills.playerLevel[skill] = c.getPA().getLevelForXP(c.skills.playerXP[skill]);
				c.getPA().refreshSkill(skill);
				c.attackSkill = false;
				c.defenceSkill = false;
				c.strengthSkill = false;
				c.healthSkill = false;
				c.rangeSkill = false;
				c.prayerSkill = false;
				c.mageSkill = false;
				} catch (Exception e){}
		}
		if(c.defenceSkill) {
						if (c.inWild())
					return;
				for (int j = 0; j < c.playerEquipment.length; j++) {
					if (c.playerEquipment[j] > 0) {
						c.sendMessage("Please remove all your equipment before using this command.");
						return;
					}
				}
				try {	
				int skill = 1;
				int level = Xamount;
				if (level > 99)
					level = 99;
				else if (level < 0)
					level = 1;
				c.skills.playerXP[skill] = c.getPA().getXPForLevel(level)+5;
				c.skills.playerLevel[skill] = c.getPA().getLevelForXP(c.skills.playerXP[skill]);
				c.getPA().refreshSkill(skill);
				c.attackSkill = false;
				c.defenceSkill = false;
				c.strengthSkill = false;
				c.healthSkill = false;
				c.rangeSkill = false;
				c.prayerSkill = false;
				c.mageSkill = false;
				} catch (Exception e){}
		}
				if(c.strengthSkill) {
						if (c.inWild())
					return;
				for (int j = 0; j < c.playerEquipment.length; j++) {
					if (c.playerEquipment[j] > 0) {
						c.sendMessage("Please remove all your equipment before using this command.");
						return;
					}
				}
				try {	
				int skill = 2;
				int level = Xamount;
				if (level > 99)
					level = 99;
				else if (level < 0)
					level = 1;
				c.skills.playerXP[skill] = c.getPA().getXPForLevel(level)+5;
				c.skills.playerLevel[skill] = c.getPA().getLevelForXP(c.skills.playerXP[skill]);
				c.getPA().refreshSkill(skill);
				c.attackSkill = false;
				c.defenceSkill = false;
				c.strengthSkill = false;
				c.healthSkill = false;
				c.rangeSkill = false;
				c.prayerSkill = false;
				c.mageSkill = false;
				} catch (Exception e){}
		}
				if(c.healthSkill) {
						if (c.inWild())
					return;
				for (int j = 0; j < c.playerEquipment.length; j++) {
					if (c.playerEquipment[j] > 0) {
						c.sendMessage("Please remove all your equipment before using this command.");
						return;
					}
				}
				try {	
				int skill = 3;
				int level = Xamount;
				if (level > 99)
					level = 99;
				else if (level < 0)
					level = 1;
				c.skills.playerXP[skill] = c.getPA().getXPForLevel(level)+5;
				c.skills.playerLevel[skill] = c.getPA().getLevelForXP(c.skills.playerXP[skill]);
				c.getPA().refreshSkill(skill);
				c.attackSkill = false;
				c.defenceSkill = false;
				c.strengthSkill = false;
				c.healthSkill = false;
				c.rangeSkill = false;
				c.prayerSkill = false;
				c.mageSkill = false;
				} catch (Exception e){}
		}
				if(c.rangeSkill) {
						if (c.inWild())
					return;
				for (int j = 0; j < c.playerEquipment.length; j++) {
					if (c.playerEquipment[j] > 0) {
						c.sendMessage("Please remove all your equipment before using this command.");
						return;
					}
				}
				try {	
				int skill = 4;
				int level = Xamount;
				if (level > 99)
					level = 99;
				else if (level < 0)
					level = 1;
				c.skills.playerXP[skill] = c.getPA().getXPForLevel(level)+5;
				c.skills.playerLevel[skill] = c.getPA().getLevelForXP(c.skills.playerXP[skill]);
				c.getPA().refreshSkill(skill);
				c.attackSkill = false;
				c.defenceSkill = false;
				c.strengthSkill = false;
				c.healthSkill = false;
				c.rangeSkill = false;
				c.prayerSkill = false;
				c.mageSkill = false;
				} catch (Exception e){}
		}
				if(c.prayerSkill) {
						if (c.inWild())
					return;
				for (int j = 0; j < c.playerEquipment.length; j++) {
					if (c.playerEquipment[j] > 0) {
						c.sendMessage("Please remove all your equipment before using this command.");
						return;
					}
				}
				try {	
				int skill = 5;
				int level = Xamount;
				if (level > 99)
					level = 99;
				else if (level < 0)
					level = 1;
				c.skills.playerXP[skill] = c.getPA().getXPForLevel(level)+5;
				c.skills.playerLevel[skill] = c.getPA().getLevelForXP(c.skills.playerXP[skill]);
				c.getPA().refreshSkill(skill);
				c.attackSkill = false;
				c.defenceSkill = false;
				c.strengthSkill = false;
				c.healthSkill = false;
				c.rangeSkill = false;
				c.prayerSkill = false;
				c.mageSkill = false;
				} catch (Exception e){}
		}
				if(c.mageSkill) {
						if (c.inWild())
					return;
				for (int j = 0; j < c.playerEquipment.length; j++) {
					if (c.playerEquipment[j] > 0) {
						c.sendMessage("Please remove all your equipment before using this command.");
						return;
					}
				}
				try {	
				int skill = 6;
				int level = Xamount;
				if (level > 99)
					level = 99;
				else if (level < 0)
					level = 1;
				c.skills.playerXP[skill] = c.getPA().getXPForLevel(level)+5;
				c.skills.playerLevel[skill] = c.getPA().getLevelForXP(c.skills.playerXP[skill]);
				c.getPA().refreshSkill(skill);
				c.attackSkill = false;
				c.defenceSkill = false;
				c.strengthSkill = false;
				c.healthSkill = false;
				c.rangeSkill = false;
				c.prayerSkill = false;
				c.mageSkill = false;
				} catch (Exception e){}
		}
		}
		if (c.settingBankX || c.xInterfaceId == 26033) {
			if (Xamount < 1) {
				Xamount = 1;
			}
			c.lastBankX = Xamount;
			c.bankQuantity = -1;
			c.settingBankX = false;
			c.xInterfaceId = 0;
			c.getBank().refreshQuantityUi();
			c.sendMessage("Bank X set to " + Xamount + ".");
			return;
		}
		switch(c.xInterfaceId) {
			case 5064:
				if(!c.getItems().playerHasItem(c.xRemoveId, Xamount))
					return;
				c.lastBankX = Xamount;
				c.getItems().bankItem(c.playerItems[c.xRemoveSlot] , c.xRemoveSlot, Xamount);
				break;
				
			case 5382:
				c.lastBankX = Xamount;
				c.getItems().fromBank(c.xRemoveId, c.xRemoveSlot, Xamount);
				break;
				
			case 3322:
				if(!c.getItems().playerHasItem(c.xRemoveId, Xamount))
					return;
				if (c.duelStatus <= 0) {
					if (Xamount > c.getItems().getItemAmount(c.xRemoveId))
						c.getTradeAndDuel().tradeItem(c.xRemoveId, c.xRemoveSlot,
								c.getItems().getItemAmount(c.xRemoveId));
					else
						c.getTradeAndDuel().tradeItem(c.xRemoveId, c.xRemoveSlot,
									Xamount);
				} else {
					if (Xamount > c.getItems().getItemAmount(c.xRemoveId))
						c.getTradeAndDuel().stakeItem(c.xRemoveId, c.xRemoveSlot,
								c.getItems().getItemAmount(c.xRemoveId));
					else
						c.getTradeAndDuel().stakeItem(c.xRemoveId, c.xRemoveSlot,
								Xamount);
				}
				break;
				
			case 3415:
				if(c.duelStatus <= 0) { 
	            	c.getTradeAndDuel().fromTrade(c.xRemoveId, c.xRemoveSlot, Xamount);
				} 
				break;
				
			case 6669:
				c.getTradeAndDuel().fromDuel(c.xRemoveId, c.xRemoveSlot, Xamount);
				break;			
		}
	}
}