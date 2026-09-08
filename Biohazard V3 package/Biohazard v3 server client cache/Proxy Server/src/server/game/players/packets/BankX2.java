package server.game.players.packets;

import server.game.players.Client;
import server.game.players.PacketType;
/**
 * Bank X Items
 **/
public class BankX2 implements PacketType {
	
	// Helper method to reset POS sell state
	private void resetSellState(Client c) {
		c.posSelling = false;
		c.posSellStep = 0;
		c.posSellItemId = 0;
		c.posSellAmount = 0;
		c.posSellPrice = 0;
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
		
		// Handle POS sell flow - amount input (step 2)
		if (c.posSelling && c.posSellStep == 2 && c.xInterfaceId == 43000) {
			if (Xamount > 0) {
				c.posSellAmount = Xamount;
				c.posSellStep = 3;
				c.xInterfaceId = 43001; // Set interface ID for price
				c.getOutStream().createFrame(27); // Open Enter Amount dialog for price
			} else {
				c.sendMessage("Invalid amount. Please enter a positive number.");
				c.posSelling = false;
				c.posSellStep = 0;
				c.getPA().openPlayerOwnedShop();
			}
			return;
		}
		
		// Handle POS sell flow - price input (step 3)
		if (c.posSelling && c.posSellStep == 3 && c.xInterfaceId == 43001) {
			if (Xamount >= 0) {
				c.posSellPrice = Xamount;
				// Validate player still has the item before deleting
				if (c.getItems().playerHasItem(c.posSellItemId, c.posSellAmount)) {
					// Record amount before deletion
					int amountBefore = c.getItems().getItemAmount(c.posSellItemId);
					// Remove the full amount from inventory (deletes across all slots)
					c.getItems().deleteItem2(c.posSellItemId, c.posSellAmount);
					// Verify items were actually removed
					int amountAfter = c.getItems().getItemAmount(c.posSellItemId);
					int amountRemoved = amountBefore - amountAfter;
					
					if (amountRemoved < c.posSellAmount) {
						// Not all items were removed - cancel listing
						c.sendMessage("Failed to remove all items from inventory.");
						resetSellState(c);
						c.getPA().openPlayerOwnedShop();
						return;
					}
					
					// Add listing
					boolean success = server.game.content.PlayerOwnedShop.addListing(c.playerName, c.posSellItemId, c.posSellAmount, c.posSellPrice);
					if (!success) {
						// addListing failed - restore the items
						c.getItems().addItem(c.posSellItemId, c.posSellAmount);
						c.sendMessage("Failed to add listing. Items have been restored.");
						resetSellState(c);
						c.getPA().openPlayerOwnedShop();
						return;
					}
					
					c.sendMessage("Item listed successfully!");
					resetSellState(c);
					c.getPA().openPlayerOwnedShop();
				} else {
					c.sendMessage("You don't have enough of that item.");
					resetSellState(c);
					c.getPA().openPlayerOwnedShop();
				}
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
				c.playerXP[skill] = c.getPA().getXPForLevel(level)+5;
				c.playerLevel[skill] = c.getPA().getLevelForXP(c.playerXP[skill]);
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
				c.playerXP[skill] = c.getPA().getXPForLevel(level)+5;
				c.playerLevel[skill] = c.getPA().getLevelForXP(c.playerXP[skill]);
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
				c.playerXP[skill] = c.getPA().getXPForLevel(level)+5;
				c.playerLevel[skill] = c.getPA().getLevelForXP(c.playerXP[skill]);
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
				c.playerXP[skill] = c.getPA().getXPForLevel(level)+5;
				c.playerLevel[skill] = c.getPA().getLevelForXP(c.playerXP[skill]);
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
				c.playerXP[skill] = c.getPA().getXPForLevel(level)+5;
				c.playerLevel[skill] = c.getPA().getLevelForXP(c.playerXP[skill]);
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
				c.playerXP[skill] = c.getPA().getXPForLevel(level)+5;
				c.playerLevel[skill] = c.getPA().getLevelForXP(c.playerXP[skill]);
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
				c.playerXP[skill] = c.getPA().getXPForLevel(level)+5;
				c.playerLevel[skill] = c.getPA().getLevelForXP(c.playerXP[skill]);
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
		switch(c.xInterfaceId) {
			case 5064:
				if(!c.getItems().playerHasItem(c.xRemoveId, Xamount))
					return;
				c.getItems().bankItem(c.playerItems[c.xRemoveSlot] , c.xRemoveSlot, Xamount);
				break;
				
			case 5382:
				c.getItems().fromBank(c.bankItems[c.xRemoveSlot] , c.xRemoveSlot, Xamount);
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