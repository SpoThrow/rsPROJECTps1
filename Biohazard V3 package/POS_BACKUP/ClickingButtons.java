package server.game.players.packets;

import server.Config;
import server.Server;
import server.content.music.MusicTab;
import server.content.skills.Cooking;
import server.content.skills.CraftingData.tanningData;
import server.content.skills.Fletching;
import server.content.skills.Herblore;
import server.content.skills.LeatherMaking;
import server.content.skills.SkillMasters;
import server.content.skills.Smelting;
import server.content.skills.Tanning;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.items.GameItem;
import server.game.items.ItemAssistant;
import server.game.minigames.barrows.Barrows;
import server.game.minigames.bountyhunter.BountyHunter;
import server.game.minigames.gliding.GnomeGlider;
import server.game.minigames.pestcontrol.PestControlRewards;
import server.game.minigames.sailing.Sailing;
import server.game.players.Client;
import server.game.players.PacketType;
import server.game.players.Player;
import server.game.players.PlayerHandler;
import server.game.players.SkillcapeData;
import core.util.Misc;

/**
 * Clicking most buttons
 **/
public class ClickingButtons implements PacketType {

	@Override
	public void processPacket(final Client c, int packetType, int packetSize) {
		int actionButtonId = Misc.hexToInt(c.getInStream().buffer, 0, packetSize);
		//int actionButtonId = c.getInStream().readShort();
		if (c.isDead)
			return;
		if(c.playerRights == 3){
			Misc.println(c.playerName+ " - actionbutton: "+actionButtonId+" - teleaction: "+c.teleAction+" - dialogueAction: "+c.dialogueAction+"");
		}
		if(c.playerIsFletching) {
			Fletching.handleFletchingClick(c, actionButtonId);
		}
		if(c.craftDialogue) {
			LeatherMaking.craftLeather(c, actionButtonId);
		}
		for (tanningData t : tanningData.values()) {
			if (actionButtonId == t.getButtonId(actionButtonId)) {
				Tanning.tanHide(c, actionButtonId);
			}
		}
		int[] spellIds = {4128,4130,4132,4134,4136,4139,4142,4145,4148,4151,4153,4157,4159,4161,4164,4165,4129,4133,4137,6006,6007,6026,6036,6046,6056,
		4147,6003,47005,4166,4167,4168,48157,50193,50187,50101,50061,50163,50211,50119,50081,50151,50199,50111,50071,50175,50223,50129,50091};
		for(int i=0; i < spellIds.length; i++) {
			if(actionButtonId == spellIds[i]) {
				c.autocasting = true;
				c.autocastId = i;	
			}
		}
		GnomeGlider.flightButtons(c, actionButtonId);
		MusicTab.handleClick(c, actionButtonId);
		Herblore.handleHerbloreButtons(c, actionButtonId);
		PestControlRewards.handlePestButtons(c, actionButtonId);
		
		// Debug for POS buttons
		if (actionButtonId >= 43000 && actionButtonId <= 45000) {
			if(c.playerRights == 3) {
				c.sendMessage("POS Button range detected: " + actionButtonId);
			}
		}
		
		if (c.getPA().handlePOSButton(actionButtonId)) {
			return;
		}
		
		switch (actionButtonId){
		// Custom Shop Interface button
		case 50006:
			c.getPA().closeAllWindows();
			break;
			
		case 14067:
			if(!c.canWalk)
				c.getDH().sendDialogues(518, 599);
			break;
			
		case 23132:
			c.setSidebarInterface(1, 3917);
			c.setSidebarInterface(2, 638);
			c.setSidebarInterface(3, 3213);
			c.setSidebarInterface(4, 1644);
			c.setSidebarInterface(5, 5608);
			if(c.playerMagicBook == 0) {
				c.setSidebarInterface(6, 1151);
			} else if (c.playerMagicBook == 1) {
				c.setSidebarInterface(6, 12855);
			} else if (c.playerMagicBook == 2) {
				c.setSidebarInterface(6, 29999);
			}
			c.setSidebarInterface(7, 58128);
			c.setSidebarInterface(8, 5065);
			c.setSidebarInterface(9, 5715); 
			c.setSidebarInterface(10, 2449);
			c.setSidebarInterface(11, 904);
			c.setSidebarInterface(12, 147);
			c.setSidebarInterface(13, 962);
			c.setSidebarInterface(0, 2423);
			c.getItems().addItem(c.playerEquipment[c.playerRing],1);
			c.getItems().deleteEquipment(c.playerEquipment[c.playerRing], c.playerRing);
			c.isNpc = false;
			c.canWalk = true;
			c.updateRequired = true;
			c.appearanceUpdateRequired = true;
			break;
			
		case 15147://Bronze, 1
			Smelting.startSmelting(c, actionButtonId, 0, 0);
			break;
		case 15146://Bronze, 5
			Smelting.startSmelting(c, actionButtonId, 0, 1);
			break;
		case 10247://Bronze, 10
			Smelting.startSmelting(c, actionButtonId, 0, 2);
			break;
		case 9110://Bronze, 28
			Smelting.startSmelting(c, actionButtonId, 0, 3);
			break;
		case 15151://Iron, 1
			Smelting.startSmelting(c, actionButtonId, 1, 0);
			break;
		case 15150://Iron, 5
			Smelting.startSmelting(c, actionButtonId, 1, 1);
			break;
		case 15149://Iron, 10
			Smelting.startSmelting(c, actionButtonId, 1, 2);
			break;
		case 15148://Iron, 28
			Smelting.startSmelting(c, actionButtonId, 1, 3);
			break;
		case 15155://silver, 1
			Smelting.startSmelting(c, actionButtonId, 2, 0);
			break;
		case 15154://silver, 5
			Smelting.startSmelting(c, actionButtonId, 2, 1);
			break;
		case 15153://silver, 10
			Smelting.startSmelting(c, actionButtonId, 2, 2);
			break;
		case 15152://silver, 28
			Smelting.startSmelting(c, actionButtonId, 2, 3);
			break;
		case 15159://steel, 1
			Smelting.startSmelting(c, actionButtonId, 3, 0);
			break;
		case 15158://steel, 5
			Smelting.startSmelting(c, actionButtonId, 3, 1);
			break;
		case 15157://steel, 10
			Smelting.startSmelting(c, actionButtonId, 3, 2);
			break;
		case 15156://steel, 28
			Smelting.startSmelting(c, actionButtonId, 3, 3);
			break;
		case 15163://gold, 1
			Smelting.startSmelting(c, actionButtonId, 4, 0);
			break;
		case 15162://gold, 5
			Smelting.startSmelting(c, actionButtonId, 4, 1);
			break;
		case 15161://gold, 10
			Smelting.startSmelting(c, actionButtonId, 4, 2);
			break;
		case 15160://gold, 28
			Smelting.startSmelting(c, actionButtonId, 4, 3);
			break;
		case 29017://mithril, 1
			Smelting.startSmelting(c, actionButtonId, 5, 0);
			break;
		case 29016://mithril, 5
			Smelting.startSmelting(c, actionButtonId, 5, 1);
			break;
		case 24253://mithril, 10
			Smelting.startSmelting(c, actionButtonId, 5, 2);
			break;
		case 16062://mithril, 28
			Smelting.startSmelting(c, actionButtonId, 5, 3);
			break;
		case 29022://addy, 1
			Smelting.startSmelting(c, actionButtonId, 6, 0);
			break;
		case 29021://addy, 5
			Smelting.startSmelting(c, actionButtonId, 6, 1);
			break;
		case 29019://addy, 10
			Smelting.startSmelting(c, actionButtonId, 6, 2);
			break;
		case 29018://addy, 28
			Smelting.startSmelting(c, actionButtonId, 6, 3);
			break;
		case 29026://rune, 1
			Smelting.startSmelting(c, actionButtonId, 7, 0);
			break;
		case 29025://rune, 5
			Smelting.startSmelting(c, actionButtonId, 7, 1);
			break;
		case 29024://rune, 10
			Smelting.startSmelting(c, actionButtonId, 7, 2);
			break;
		case 29023://rune, 28
			Smelting.startSmelting(c, actionButtonId, 7, 3);
			break;
		case 108006: // items kept on death
			c.StartBestItemScan(c);
			c.EquipStatus = 0;
			for (int k = 0; k < 4; k++)
				c.getPA().sendFrame34a(10494, -1, k, 1);
			for (int k = 0; k < 39; k++)
				c.getPA().sendFrame34a(10600, -1, k, 1);
			if (c.WillKeepItem1 > 0)
				c.getPA().sendFrame34a(10494, c.WillKeepItem1, 0,
						c.WillKeepAmt1);
			if (c.WillKeepItem2 > 0)
				c.getPA().sendFrame34a(10494, c.WillKeepItem2, 1,
						c.WillKeepAmt2);
			if (c.WillKeepItem3 > 0)
				c.getPA().sendFrame34a(10494, c.WillKeepItem3, 2,
						c.WillKeepAmt3);
			if (c.WillKeepItem4 > 0 && c.prayerActive[10])
				c.getPA().sendFrame34a(10494, c.WillKeepItem4, 3, 1);
			for (int ITEM = 0; ITEM < 28; ITEM++) {
				if (c.playerItems[ITEM] - 1 > 0
						&& !(c.playerItems[ITEM] - 1 == c.WillKeepItem1 && ITEM == c.WillKeepItem1Slot)
						&& !(c.playerItems[ITEM] - 1 == c.WillKeepItem2 && ITEM == c.WillKeepItem2Slot)
						&& !(c.playerItems[ITEM] - 1 == c.WillKeepItem3 && ITEM == c.WillKeepItem3Slot)
						&& !(c.playerItems[ITEM] - 1 == c.WillKeepItem4 && ITEM == c.WillKeepItem4Slot)) {
					c.getPA().sendFrame34a(10600, c.playerItems[ITEM] - 1,
							c.EquipStatus, c.playerItemsN[ITEM]);
					c.EquipStatus += 1;
				} else if (c.playerItems[ITEM] - 1 > 0
						&& (c.playerItems[ITEM] - 1 == c.WillKeepItem1 && ITEM == c.WillKeepItem1Slot)
						&& c.playerItemsN[ITEM] > c.WillKeepAmt1) {
					c.getPA().sendFrame34a(10600, c.playerItems[ITEM] - 1,
							c.EquipStatus,
							c.playerItemsN[ITEM] - c.WillKeepAmt1);
					c.EquipStatus += 1;
				} else if (c.playerItems[ITEM] - 1 > 0
						&& (c.playerItems[ITEM] - 1 == c.WillKeepItem2 && ITEM == c.WillKeepItem2Slot)
						&& c.playerItemsN[ITEM] > c.WillKeepAmt2) {
					c.getPA().sendFrame34a(10600, c.playerItems[ITEM] - 1,
							c.EquipStatus,
							c.playerItemsN[ITEM] - c.WillKeepAmt2);
					c.EquipStatus += 1;
				} else if (c.playerItems[ITEM] - 1 > 0
						&& (c.playerItems[ITEM] - 1 == c.WillKeepItem3 && ITEM == c.WillKeepItem3Slot)
						&& c.playerItemsN[ITEM] > c.WillKeepAmt3) {
					c.getPA().sendFrame34a(10600, c.playerItems[ITEM] - 1,
							c.EquipStatus,
							c.playerItemsN[ITEM] - c.WillKeepAmt3);
					c.EquipStatus += 1;
				} else if (c.playerItems[ITEM] - 1 > 0
						&& (c.playerItems[ITEM] - 1 == c.WillKeepItem4 && ITEM == c.WillKeepItem4Slot)
						&& c.playerItemsN[ITEM] > 1) {
					c.getPA().sendFrame34a(10600, c.playerItems[ITEM] - 1,
							c.EquipStatus, c.playerItemsN[ITEM] - 1);
					c.EquipStatus += 1;
				}
			}
			for (int EQUIP = 0; EQUIP < 14; EQUIP++) {
				if (c.playerEquipment[EQUIP] > 0
						&& !(c.playerEquipment[EQUIP] == c.WillKeepItem1 && EQUIP + 28 == c.WillKeepItem1Slot)
						&& !(c.playerEquipment[EQUIP] == c.WillKeepItem2 && EQUIP + 28 == c.WillKeepItem2Slot)
						&& !(c.playerEquipment[EQUIP] == c.WillKeepItem3 && EQUIP + 28 == c.WillKeepItem3Slot)
						&& !(c.playerEquipment[EQUIP] == c.WillKeepItem4 && EQUIP + 28 == c.WillKeepItem4Slot)) {
					c.getPA().sendFrame34a(10600, c.playerEquipment[EQUIP],
							c.EquipStatus, c.playerEquipmentN[EQUIP]);
					c.EquipStatus += 1;
				} else if (c.playerEquipment[EQUIP] > 0
						&& (c.playerEquipment[EQUIP] == c.WillKeepItem1 && EQUIP + 28 == c.WillKeepItem1Slot)
						&& c.playerEquipmentN[EQUIP] > 1
						&& c.playerEquipmentN[EQUIP] - c.WillKeepAmt1 > 0) {
					c.getPA().sendFrame34a(10600, c.playerEquipment[EQUIP],
							c.EquipStatus,
							c.playerEquipmentN[EQUIP] - c.WillKeepAmt1);
					c.EquipStatus += 1;
				} else if (c.playerEquipment[EQUIP] > 0
						&& (c.playerEquipment[EQUIP] == c.WillKeepItem2 && EQUIP + 28 == c.WillKeepItem2Slot)
						&& c.playerEquipmentN[EQUIP] > 1
						&& c.playerEquipmentN[EQUIP] - c.WillKeepAmt2 > 0) {
					c.getPA().sendFrame34a(10600, c.playerEquipment[EQUIP],
							c.EquipStatus,
							c.playerEquipmentN[EQUIP] - c.WillKeepAmt2);
					c.EquipStatus += 1;
				} else if (c.playerEquipment[EQUIP] > 0
						&& (c.playerEquipment[EQUIP] == c.WillKeepItem3 && EQUIP + 28 == c.WillKeepItem3Slot)
						&& c.playerEquipmentN[EQUIP] > 1
						&& c.playerEquipmentN[EQUIP] - c.WillKeepAmt3 > 0) {
					c.getPA().sendFrame34a(10600, c.playerEquipment[EQUIP],
							c.EquipStatus,
							c.playerEquipmentN[EQUIP] - c.WillKeepAmt3);
					c.EquipStatus += 1;
				} else if (c.playerEquipment[EQUIP] > 0
						&& (c.playerEquipment[EQUIP] == c.WillKeepItem4 && EQUIP + 28 == c.WillKeepItem4Slot)
						&& c.playerEquipmentN[EQUIP] > 1
						&& c.playerEquipmentN[EQUIP] - 1 > 0) {
					c.getPA().sendFrame34a(10600, c.playerEquipment[EQUIP],
							c.EquipStatus, c.playerEquipmentN[EQUIP] - 1);
					c.EquipStatus += 1;
				}
			}
			c.ResetKeepItems();
			c.getPA().showInterface(17100);
			break;
			
			/**Prayers**/
		case 21233: // thick skin
			c.getCombat().activatePrayer(0);
			break;	
		case 21234: // burst of str
			c.getCombat().activatePrayer(1);
			break;	
		case 21235: // charity of thought
			c.getCombat().activatePrayer(2);
			break;	
		case 70080: // range
			c.getCombat().activatePrayer(3);
			break;
		case 70082: // mage
			c.getCombat().activatePrayer(4);
			break;
		case 21236: // rockskin
			c.getCombat().activatePrayer(5);
			break;
		case 21237: // super human
			c.getCombat().activatePrayer(6);
			break;
		case 21238:	// improved reflexes
			c.getCombat().activatePrayer(7);
			break;
		case 21239: //hawk eye
			c.getCombat().activatePrayer(8);
			break;
		case 21240:
			c.getCombat().activatePrayer(9);
			break;
		case 21241: // protect Item
			c.getCombat().activatePrayer(10);
			break;			
		case 70084: // 26 range
			c.getCombat().activatePrayer(11);
			break;
		case 70086: // 27 mage
			c.getCombat().activatePrayer(12);
			break;	
		case 21242: // steel skin
			c.getCombat().activatePrayer(13);
			break;
		case 21243: // ultimate str
			c.getCombat().activatePrayer(14);
			break;
		case 21244: // incredible reflex
			c.getCombat().activatePrayer(15);
			break;	
		case 21245: // protect from magic
			c.getCombat().activatePrayer(16);
			break;					
		case 21246: // protect from range
			c.getCombat().activatePrayer(17);
			break;
		case 21247: // protect from melee
			c.getCombat().activatePrayer(18);
			break;
		case 70088: // 44 range
			c.getCombat().activatePrayer(19);
			break;	
		case 70090: // 45 mystic
			c.getCombat().activatePrayer(20);
			break;				
		case 2171: // retrui
			c.getCombat().activatePrayer(21);
			break;					
		case 2172: // redem
			c.getCombat().activatePrayer(22);
			break;					
		case 2173: // smite
			c.getCombat().activatePrayer(23);
			break;
		case 70092: // chiv
			c.getCombat().activatePrayer(24);
			break;
		case 70094: // piety
			c.getCombat().activatePrayer(25);
			break;

			
        case 164034:
            c.removedTasks[0] = -1;
            c.getSlayer().updateCurrentlyRemoved();
            break;
            
        case 164035:
            c.removedTasks[1] = -1;
            c.getSlayer().updateCurrentlyRemoved();
            break;
            
        case 164036:
            c.removedTasks[2] = -1;
            c.getSlayer().updateCurrentlyRemoved();
            break;
            
        case 164037:
            c.removedTasks[3] = -1;
            c.getSlayer().updateCurrentlyRemoved();
            break;


        case 164028:
            c.getSlayer().cancelTask();
            break;
        case 164029:
            c.getSlayer().removeTask();
            break;    
        case 160052:
            c.getSlayer().buySlayerExperience();
            break;
        case 160053:
            c.getSlayer().buyRespite();
            break;
        case 160054:
            c.getSlayer().buySlayerDart();
            break;
        case 160055:
            c.getSlayer().buyBroadArrows();
            break;

        case 160045:
        case 162033:
        case 164021:
            if (c.interfaceId != 41000)
                c.getSlayer().handleInterface("buy");
            break;


        case 160047:
        case 162035:
        case 164023:
            if (c.interfaceId != 41500)
                c.getSlayer().handleInterface("learn");
            break;


        case 160049:
        case 162037:
        case 164025:
            if (c.interfaceId != 42000)
                c.getSlayer().handleInterface("assignment");
            break;


        case 162030:
        case 164018:
        case 160042:
            c.getPA().removeAllWindows();
            break;
		
		//quests
		case 28170: //ca
			//CooksAssistant.showInformation(c);
			break;
		case 28172: //rm
			//RuneMysteries.showInformation(c);
			break;
			
		case 10252:
			c.antiqueSelect = 0;
			c.sendMessage("You select Attack");
		break;
		case 10253:
			c.antiqueSelect = 2;
			c.sendMessage("You select Strength");
		break;
		case 10254:
			c.antiqueSelect = 4;
			c.sendMessage("You select Ranged");
		break;
		case 10255:
			c.antiqueSelect = 6;
			c.sendMessage("You select Magic");
		break;
		case 11000:
			c.antiqueSelect = 1;
			c.sendMessage("You select Defence");
		break;
		case 11001:
			c.antiqueSelect = 3;
			c.sendMessage("You select Hitpoints");
		break;
		case 11002:
			c.antiqueSelect = 5;
			c.sendMessage("You select Prayer");
		break;
		case 11003:
			c.antiqueSelect = 16;
			c.sendMessage("You select Agility");
		break;
		case 11004:
			c.antiqueSelect = 15;
			c.sendMessage("You select Herblore");
		break;
		case 11005:
			c.antiqueSelect = 17;
			c.sendMessage("You select Thieving");
		break;
		case 11006:
			c.antiqueSelect = 12;
			c.sendMessage("You select Crafting");
		break;
		case 11007:
			c.antiqueSelect = 20;
			c.sendMessage("You select Runecrafting");
		break;
		case 47002:
			c.antiqueSelect = 18;
			c.sendMessage("You select Slayer");
		break;
		case 54090:
			c.antiqueSelect = 19;
			c.sendMessage("You select Farming");
		break;
		case 11008:
			c.antiqueSelect = 14;
			c.sendMessage("You select Mining");
		break;
		case 11009:
			c.antiqueSelect = 13;
			c.sendMessage("You select Smithing");
		break;
		case 11010:
			c.antiqueSelect = 10;
			c.sendMessage("You select Fishing");
		break;
		case 11011:
			c.antiqueSelect = 7;
			c.sendMessage("You select Cooking");
		break;
		case 11012:
			c.antiqueSelect = 11;
			c.sendMessage("You select Firemaking");
		break;
		case 11013:
			c.antiqueSelect = 8;
			c.sendMessage("You select Woodcutting");
		break;
		case 11014:
			c.antiqueSelect = 9;
			c.sendMessage("You select Fletching");
		break;
		case 11015:
			if(c.isResetting) {
				for (int j = 0; j < c.playerEquipment.length; j++)
					if(c.playerEquipment[j] > 0) {
						c.getDH().sendStatement("Please remove all your equipment.");
						c.isResetting = false;
						c.nextChat = 0;
						return;
					}
				if(c.antiqueSelect == 3) {
					c.playerXP[3] = (c.getPA().getXPForLevel(10)+ 1);
					c.playerLevel[3] = c.getPA().getLevelForXP(c.playerXP[3]);
					c.getPA().refreshSkill(3);
					c.getPA().closeAllWindows();
					c.isResetting = false;
					return;
				} else {
					c.playerXP[c.antiqueSelect] = (c.getPA().getXPForLevel(1)+ 1);
					c.playerLevel[c.antiqueSelect] = c.getPA().getLevelForXP(c.playerXP[c.antiqueSelect]);
					c.getPA().refreshSkill(c.antiqueSelect);
					c.getPA().closeAllWindows();
					c.isResetting = false;
					return;
				}
			}
			if(c.rubbedLamp) {
				if(c.getItems().playerHasItem(4447)) {
					c.getItems().deleteItem2(4447, 1);
					c.getPA().closeAllWindows();
					c.getPA().addSkillXP((c.getPA().getXPForLevel(70)+1),c.antiqueSelect);
					c.rubbedLamp = false;
				}
			}
			break;
			
		case 118098:
			c.getPA().vengMe();
			break;
		case 53152:
			Cooking.getAmount(c, 1);
			break;
		case 53151:
			Cooking.getAmount(c, 5);
			break;
		case 53150:
			Cooking.getAmount(c, 10);
			break;
		case 53149:
			Cooking.getAmount(c, 28);
			break;
	/*	case 33206:
if (c.inWild()) {
c.sendMessage("You can't use this in the wilderness!");
break;
}
c.outStream.createFrame(27);
c.attackSkill = true;
c.usingLevel = true;
				c.defenceSkill = false;
				c.strengthSkill = false;
				c.healthSkill = false;
				c.rangeSkill = false;
				c.prayerSkill = false;
				c.mageSkill = false;
break;
case 33209:
if (c.inWild()) {
c.sendMessage("You can't use this in the wilderness!");
break;
}
c.outStream.createFrame(27);
c.strengthSkill = true;
c.usingLevel = true;
c.attackSkill = false;
				c.defenceSkill = false;
				c.healthSkill = false;
				c.rangeSkill = false;
				c.prayerSkill = false;
				c.mageSkill = false;
break;
case 33212:
if (c.inWild()) {
c.sendMessage("You can't use this in the wilderness!");
break;
}
c.outStream.createFrame(27);
c.attackSkill = false;
				c.strengthSkill = false;
				c.healthSkill = false;
				c.rangeSkill = false;
				c.prayerSkill = false;
				c.mageSkill = false;
c.defenceSkill = true;
c.usingLevel = true;
break;
case 33215:
if (c.inWild()) {
c.sendMessage("You can't use this in the wilderness!");
break;
}
c.outStream.createFrame(27);
c.attackSkill = false;
				c.defenceSkill = false;
				c.strengthSkill = false;
				c.healthSkill = false;
				c.prayerSkill = false;
				c.mageSkill = false;
c.rangeSkill = true;
c.usingLevel = true;
break;
case 33218:
if (c.inWild()) {
c.sendMessage("You can't use this in the wilderness!");
break;
}
c.outStream.createFrame(27);
c.prayerSkill = true;
c.usingLevel = true;
c.attackSkill = false;
				c.defenceSkill = false;
				c.strengthSkill = false;
				c.healthSkill = false;
				c.rangeSkill = false;
				c.mageSkill = false;
break;
case 33221:
if (c.inWild()) {
c.sendMessage("You can't use this in the wilderness!");
break;
}
c.outStream.createFrame(27);
c.mageSkill = true;
c.usingLevel = true;
c.attackSkill = false;
				c.defenceSkill = false;
				c.strengthSkill = false;
				c.healthSkill = false;
				c.rangeSkill = false;
				c.prayerSkill = false;
break;
case 33207:
if (c.inWild()) {
c.sendMessage("You can't use this in the wilderness!");
break;
}
c.outStream.createFrame(27);
c.attackSkill = false;
				c.defenceSkill = false;
				c.strengthSkill = false;
				c.rangeSkill = false;
				c.prayerSkill = false;
				c.mageSkill = false;
c.healthSkill = true;
c.usingLevel = true;
break;*/
		case 33224: // runecrafting
			c.getSI().runecraftingComplex(1);
			c.getSI().selected = 6;
			break;
		case 33210: // agility
			c.getSI().agilityComplex(1);
				c.getSI().selected = 8;
			//c.sendMessage("Skill not supported yet.");
			break;
		case 33213: // herblore
			c.getSI().herbloreComplex(1);
			c.getSI().selected = 9;
			break;
		case 33216: // theiving
			c.getSI().thievingComplex(1);
			c.getSI().selected = 10;
			break;
		case 33219: // crafting
			c.getSI().craftingComplex(1);
			c.getSI().selected = 11;
			//c.sendMessage("Skill not supported yet.");
			break;
		case 33222: // fletching
			c.getSI().fletchingComplex(1);
			c.getSI().selected = 12;
			break;
		case 47130:// slayer
			c.getSI().slayerComplex(1);
			c.getSI().selected = 13;
			break;
		case 33214: // fishing
			c.getSI().fishingComplex(1);
			c.getSI().selected = 16;
			break;
		case 33217: // cooking
			c.getSI().cookingComplex(1);
			c.getSI().selected = 17;
			break;
		case 33220: // firemaking
			c.getSI().firemakingComplex(1);
			c.getSI().selected = 18;
			break;
		case 33223: // woodcut
			c.getSI().woodcuttingComplex(1);
			c.getSI().selected = 19;
			break;
		case 54104: // farming
			c.getSI().farmingComplex(1);
			c.getSI().selected = 20;
			//c.sendMessage("Skill not supported yet.");
			break;

		case 34142: // tab 1
			c.getSI().menuCompilation(1);
			break;

		case 34119: // tab 2
			c.getSI().menuCompilation(2);
			break;

		case 34120: // tab 3
			c.getSI().menuCompilation(3);
			break;

		case 34123: // tab 4
			c.getSI().menuCompilation(4);
			break;

		case 34133: // tab 5
			c.getSI().menuCompilation(5);
			break;

		case 34136: // tab 6
			c.getSI().menuCompilation(6);
			break;

		case 34139: // tab 7
			c.getSI().menuCompilation(7);
			break;

		case 34155: // tab 8
			c.getSI().menuCompilation(8);
			break;

		case 34158: // tab 9
			c.getSI().menuCompilation(9);
			break;

		case 34161: // tab 10
			c.getSI().menuCompilation(10);
			break;

		case 59199: // tab 11
			c.getSI().menuCompilation(11);
			break;

		case 59202: // tab 12
			c.getSI().menuCompilation(12);
			break;
		case 59203: // tab 13
			c.getSI().menuCompilation(13);
			break;

		case 150:
			if(c.autoRet > 0) {
				c.autoRet = 0;
			} else {
				c.autoRet = 1;
			}
			break;
			
			//1st tele option
		case 9190:
			switch(c.teleAction) {
			case 1: //monsters // basic
				c.getPA().spellTeleport(3026, 3217, 0);
				break;
				
			case 2: //minigames //barrows
				//c.getPA().spellTeleport(3565, 3314, 0);
				//BA
				c.getPA().spellTeleport(2602, 3155, 0);
				break;
				
			case 3: //bosses //gwd
				c.getPA().spellTeleport(2902, 3724, 0);
				break;
				
			case 4: //PK //Edgeville
				c.getPA().spellTeleport(3089, 3528, 0);
				break;
				
			case 5: //Skills //skills
				c.getPA().spellTeleport(2809,3192,0);
				break;
				
			case 6: //masters //strength
				c.getPA().spellTeleport(2846, 3541, 0);
				break;
			}
			c.teleAction = -1;
			switch(c.dialogueAction) {
			case 751: //display
				if(c.playerRights == 1 || c.playerRights == 2 || c.playerRights == 3) {
					c.sendMessage("You are an In-Game Staff member so you can't switch ranks on this account.");
					c.getPA().closeAllWindows();
					return;
				}
				if(c.donator > 0) {
					c.playerRights = 4;
					c.logout();
				} else {
					c.sendMessage("You have to be a donator of Biohazard to display this rank.");
					c.getPA().closeAllWindows();
				}
				break;
			case 690: //skiling5 //strength
				c.getPA().spellTeleport(2846, 3541, 0);
				break;
			case 590:
				c.fade(3496, 3490, 0);
				break;
			case 588:
				c.fade(3359, 2910, 0);
				break;
			case 570:
				c.fade(2806, 10002, 0);
				break;
			case 568:
				c.fade(3082, 3420, 0);
				break;
			case 562:
				Sailing.startTravel(c, 0);
				break;
			case 560:
				Sailing.startTravel(c, 5);
				break;
			case 558:
				c.fade(3274, 2784, 0);
				break;
			case 556:
				c.fade(3210, 3424, 0);
				break;
			case 478: //skills4 //thieving
				c.getPA().spellTeleport(3048, 4972, 1);
				break;
			case 504: //bosses //cave
				c.getPA().spellTeleport(2513, 3040, 0);
				break;
			case 501: //minigames //tzhaar
				c.getPA().spellTeleport(2444, 5170, 0);
				break;
			case 482: //islands //crandor
				Sailing.startTravel(c, 3);
				break;
				
			case 414: //fishing //alkharid
				c.getPA().spellTeleport(3273,3149,0);
				break;
				
			case 476: //skills2 //firemaking
				c.getPA().spellTeleport(2715,3422,0);
				break;
				
			case 477: //skills3 //mining
				c.getPA().spellTeleport(3016,3339,0);
				break;
				
			case 10:
				c.getPA().spellTeleport(2845, 4832, 0);
				break;
				
			case 11:
				c.getPA().spellTeleport(2584, 4836, 0);
				break;
				
			case 12:
				c.getPA().spellTeleport(2398, 4841, 0);
				break;				
			}
			c.dialogueAction = -1;
			break;

			//2nd tele option
		case 9191:
			switch(c.teleAction) {
			case 1: //monsters //desert
				c.getPA().spellTeleport(3283, 3329, 0);
				break;
				
			case 2: //minigames //pestcontrol
				//c.getPA().spellTeleport(2662, 2650, 0);
				//DUEL ARENA
				c.getPA().spellTeleport(3366, 3266, 0);
				break;
				
			case 3: //bosses //kbd
				c.getPA().spellTeleport(3007, 3849, 0);
				break;
				
			case 4: //PK //castle 
				c.getPA().spellTeleport(3016, 3632, 0);
				break;
				
			case 5: //skills //cooking
				c.getPA().spellTeleport(3209,3215,0);
				break;
				
			case 6: //masters //attack
				c.getPA().spellTeleport(2846, 3541, 0);
				break;
			}
			c.teleAction = -1;
			switch(c.dialogueAction) {
			case 751: //display
				if(c.playerRights == 1 || c.playerRights == 2 || c.playerRights == 3) {
					c.sendMessage("You are an In-Game Staff member so you can't switch ranks on this account.");
					c.getPA().closeAllWindows();
					return;
				}
				if(c.donator > 1) {
					c.playerRights = 5;
					c.logout();
				} else {
					c.sendMessage("You have to donate $40+ to Biohazard to display this rank.");
					c.getPA().closeAllWindows();
				}
				break;
			case 690: //skiling5 //prayer
				c.getPA().spellTeleport(3052, 3481, 0);
				break;
			case 590:
				c.fade(3487, 3288, 0);
				break;
			case 588:
				c.fade(2425, 2919, 0);
				break;
			case 570:
				c.fade(2857, 3166, 0);
				break;
			case 568:
				c.fade(2851, 3675, 0);
				break;
			case 562:
				Sailing.startTravel(c, 8);
				break;
			case 560:
				Sailing.startTravel(c, 7);
				break;
			case 558:
				c.fade(3506, 3496, 0);
				break;
			case 556:
				c.fade(2964, 3378, 0);
				break;
			case 478: //skills4 //woodcutting
				c.getPA().spellTeleport(2969, 3423, 0);
				break;
			case 504: //bosses //underwater
				c.getPA().setUWAnim();
				c.getPA().movePlayer(2972, 9507, 0);
				c.underWater = true;
				break;
			case 501: //fishing //fishing trawler
				c.getPA().spellTeleport(2662,3161,0);
				break;
			case 482: //islands //entrana
				Sailing.startTravel(c, 1);
				break;
				
			case 414: //fishing //shilo
				c.getPA().spellTeleport(2864,2969,0);
				break;
				
			case 476: //skills2 //fishing
				c.getPA().spellTeleport(2597,3402,0);
				break;
				
			case 477: //skills3 //rc
				c.getPA().spellTeleport(3083,3512,0);
				break;
				
			case 10:
				c.getPA().spellTeleport(2787, 4839, 0);
				break;
				
			case 11:
				c.getPA().spellTeleport(2527, 4833, 0);
				break;
				
			case 12:
				c.getPA().spellTeleport(2464, 4834, 0);
				break;
			}
			c.dialogueAction = -1;
			break;
			
			//3rd tele option	
		case 9192:
			switch(c.teleAction) {
			case 1: //monsters //snow mountain
				c.getPA().spellTeleport(2834, 3518, 0);
				break;
				
			case 2: //minigames //tzhaar
				//c.getPA().spellTeleport(2444, 5170, 0);
				//barrows
				c.getPA().spellTeleport(3565, 3314, 0);
				break;
				
			case 3: //bosses //dag kings
				c.getPA().spellTeleport(2547, 3758, 0);
				break;
				
			case 4: //pk //magebank
				c.getPA().spellTeleport(2539, 4716, 0);
				break;
				
			case 5: //skills //crafting
				c.getPA().spellTeleport(2935,3283,0);
				break;
			
			case 6: //masters //prayer
				c.getPA().spellTeleport(3052, 3481, 0);
				break;
			}
			c.teleAction = -1;
			switch(c.dialogueAction) {
			case 751: //display
				if(c.playerRights == 1 || c.playerRights == 2 || c.playerRights == 3) {
					c.sendMessage("You are an In-Game Staff member so you can't switch ranks on this account.");
					c.getPA().closeAllWindows();
					return;
				}
				if(c.donator == 3) {
					c.playerRights = 6;
					c.logout();
				} else {
					c.sendMessage("You have to donate $80+ to Biohazard to display this rank.");
					c.getPA().closeAllWindows();
				}
				break;
			case 690: //skiling5 //defence
				c.getPA().spellTeleport(3221, 3237, 0);
				break;
			case 590:
				c.fade(3494, 3235, 0);
				break;
			case 588:
				c.fade(3309, 2792, 0);
				break;
			case 570:
				c.fade(2808, 3440, 0);
				break;
			case 568:
				c.fade(2773, 3836, 0);
				break;
			case 562:
				Sailing.startTravel(c, 13);
				break;
			case 560:
				Sailing.startTravel(c, 3);
				break;
			case 558:
				c.fade(2792, 3061, 0);
				break;
			case 556:
				c.fade(3222, 3218, 0);
				break;
			case 478: //skills4 //construction
				c.getPA().spellTeleport(2953, 3221, 0);
				break;
			case 504: //bosses //mutant tarn
				c.getPA().spellTeleport(2463, 4773, 0);
				break;
			case 501: //minigames //Castle wars
				c.getPA().spellTeleport(2440, 3092, 0);
				break;
			case 482: //islands //crash island
				Sailing.startTravel(c, 14);
				break;
				
			case 414: //fishing //karamja
				c.getPA().spellTeleport(2925,3171,0);
				break;
			
			case 476: //skills2 //fletching
				c.getPA().spellTeleport(2822,3441,0);
				break;
				
			case 477: //skills3 //slayer
				c.getPA().spellTeleport(2873,2980,0);
				break;
				
			case 10:
				c.getPA().spellTeleport(2713, 4836, 0);
				break;
				
			case 11:
				c.getPA().spellTeleport(2162, 4833, 0);
				break;
				
			case 12:
				c.getPA().spellTeleport(2207, 4836, 0);
				break;
			}
			c.dialogueAction = -1;
			break;
			
			//4th tele option
		case 9193:
			switch(c.teleAction) {
			case 1: //monsters //slayer tower
				c.getPA().spellTeleport(3429, 3538, 0);
				break;
				
			case 2: //minigames //duelarena
				//c.getPA().spellTeleport(3366, 3266, 0);
				//pest control
				c.getPA().spellTeleport(2662, 2650, 0);
				break;
				
			case 3: //bosses //KQ
				c.getPA().spellTeleport(3310, 3109, 0);
				break;
				
			case 4: //pk //hill giants 
				c.getPA().spellTeleport(3288, 3631, 0);
				break;
				
			case 5: //skills //farming
				c.getPA().spellTeleport(2815,3338,0);
				break;
				
			case 6: //masters //defence
				c.getPA().spellTeleport(3221, 3237, 0);
				break;
			}
			c.teleAction = -1;
			switch(c.dialogueAction) {
			case 751: //display
				if(c.playerRights == 1 || c.playerRights == 2 || c.playerRights == 3) {
					c.sendMessage("You are an In-Game Staff member so you can't switch ranks on this account.");
					c.getPA().closeAllWindows();
					return;
				}
				if(c.respected == 1) {
					c.playerRights = 7;
					c.logout();
				} else {
					c.sendMessage("You have to be assigned as a respected member to display this rank.");
					c.getPA().closeAllWindows();
				}
				break;
			case 690: //skiling5 //ranging
				c.getPA().spellTeleport(2667, 3427, 0);
				break;
			case 590:
				c.fade(3690, 3467, 0);
				break;
			case 588:
				c.fade(3176, 2986, 0);
				break;
			case 570:
				c.fade(2604, 3288, 0);
				break;
			case 568:
				c.fade(2909, 3540, 0);
				break;
			case 562:
				Sailing.startTravel(c, 10);
				break;
			case 560:
				Sailing.startTravel(c, 1);
				break;
			case 558:
				c.fade(2815, 3182, 0);
				break;
			case 556:
				c.fade(3093, 3244, 0);
				break;
			case 478: //skills4 //hunter
				c.getPA().spellTeleport(2775, 2887, 0);
				break;
			case 504: //bosses //the inadequacy
				c.getPA().spellTeleport(3318, 3068, 4);
				break;
			case 501: //minigames //soul wars
				c.getPA().spellTeleport(3179, 3685, 0);
				break;
			case 482: //islands //karamja
				Sailing.startTravel(c, 5);
				break;
				
			case 414: //fishing /catherby
				c.getPA().spellTeleport(2841,3435,0);
				break;
				
			case 476: //skills2 //herblore
				c.getPA().spellTeleport(2898,3430,0);
				break;
				
			case 477: //skills3 //smithing
				c.getPA().spellTeleport(2996,3145,0);
				break;
				
			case 10:
				c.getPA().spellTeleport(2660, 4839, 0);
				break;
			}
			c.dialogueAction = -1;
			break;
			
			//5th tele option
		case 9194:
			switch(c.teleAction) {
			case 1: //monsters //dungoens
				c.getPA().spellTeleport(2926, 2910, 0);
				break;
				
			case 2: //minigames //more
				c.getDH().sendDialogues(501, -1);
				c.teleAction = -1;
				//c.getPA().spellTeleport(2444, 5170, 0);
				return;
				
			case 3: //bosses //more
				//c.getPA().spellTeleport(2513, 3040, 0);
				c.getDH().sendDialogues(504, -1);
				c.teleAction = -1;
				return;
				
			case 4: //pk //ardy lever
				c.getPA().spellTeleport(2561, 3311, 0);
				break;
				
			case 5: //skills1
				c.teleAction = -1;
				c.dialogueAction = -1;
				c.getDH().sendDialogues(476, -1);
				return;
				
			case 6: //masters //ranged
				c.getPA().spellTeleport(2667, 3427, 0);
				break;
			}
			c.teleAction = -1;
			switch(c.dialogueAction) {
			case 751: //display
				c.getDH().sendDialogues(752, -1);
				return;
			case 590:
				c.fade(3560, 9950, 0);
				break;
			case 588:
				c.fade(3485, 3089, 0);
				break;
			case 570:
				c.getPA().underWaterTele();
				break;
			case 568:
				c.fade(2834, 3518, 0);
				break;
			case 562:
				Sailing.startTravel(c, 15);
				break;
			case 560:
				Sailing.startTravel(c, 9);
				break;
			case 558:
				c.fade(2891, 3454, 0);
				break;
			case 556:
				c.fade(3293, 3174, 0);
				break;
			case 478: //skilling4 //next
				c.getDH().sendDialogues(690, -1);
				return;
			case 690: //skilling5 //first page
				c.getDH().sendOption5("Agility", "Cooking", "Crafting", "Farming", "Next ->");
				c.teleAction = 5;
				c.dialogueAction = -1;
				return;
			case 504: //bosses //slash bash
				c.getPA().spellTeleport(2467, 9439, 0);
				break;
			case 501: //minigames //back
				c.getDH().sendOption5("Barbarian Assault", "Duel Arena", "Barrows", "Pest Control", "Next ->");
				c.teleAction = 2;
				return;
			case 482: //islands //rellekka
				Sailing.startTravel(c, 10);
				break;
				
			case 414: //fishing //piscatoris fishing colony
				c.getPA().spellTeleport(2330,3689,0);
				break;
			
			case 476: //skills2
				c.teleAction = -1;
				c.dialogueAction = -1;
				c.getDH().sendDialogues(477, -1);
				return;
				
			case 477: //skills3
				c.teleAction = -1;
				c.dialogueAction = -1;
				c.getDH().sendDialogues(478, -1);
				return;
				
			case 10:
			case 11:
				c.dialogueId++;
				c.getDH().sendDialogues(c.dialogueId, 0);
				break;
				
			case 12:
				c.dialogueId = 17;
				c.getDH().sendDialogues(c.dialogueId, 0);
				break;
			}
			c.dialogueAction = -1;
			break;

		/*case 71074:
			if (c.clanId >= 0) {
				if (Server.clanChat.clans[c.clanId].owner.equalsIgnoreCase(c.playerName)) {
					Server.clanChat.sendLootShareMessage(c.clanId, "Lootshare has been toggled to " + (!Server.clanChat.clans[c.clanId].lootshare ? "on" : "off") + " by the clan leader.");
					Server.clanChat.clans[c.clanId].lootshare = !Server.clanChat.clans[c.clanId].lootshare;
				} else
					c.sendMessage("Only the owner of the clan has the power to do that.");
			}	
			break;*/
			
		case 62137:
			if (c.clanId >= 0) {
				c.sendMessage("You are already in a clan.");
				break;
			}
			if (c.getOutStream() != null) {
				c.getOutStream().createFrame(187);
				c.flushOutStream();
			}
			break;

		case 108005:
			c.getPA().showInterface(15106);
			c.getItems().writeBonus();
			break;

		case 59004:
			c.getPA().removeAllWindows();
			break;

		/*case 70212:
			if (c.clanId > -1)
				Server.clanChat.leaveClan(c.playerId, c.clanId);
			else
				c.sendMessage("You are not in a clan.");
			break;*/
		/*case 70209:
			if (c.clanId >= 0) {
				c.sendMessage("You are already in a clan.");
				break;
			}
			if (c.getOutStream() != null) {
				c.getOutStream().createFrame(187);
				c.flushOutStream();
			}	
			break;*/

		case 9178:
			switch(c.dialogueAction) {
			case 752: //display
				if(c.playerRights == 1 || c.playerRights == 2 || c.playerRights == 3) {
					c.sendMessage("You are an In-Game Staff member so you can't switch ranks on this account.");
					c.getPA().closeAllWindows();
					return;
				}
				if(c.veteran == 1) {
					c.playerRights = 8;
					c.logout();
				} else {
					c.sendMessage("You need to have a total level of 2277 to display this rank.");
					c.getPA().closeAllWindows();
				}
				break;
			case 592:
				c.fade(2790, 3063, 0);
				break;
			case 586:
				c.fade(3267, 3166, 0);
				break;
			case 584:
				c.fade(3077, 9771, 0);
				break;
			case 582:
				c.fade(3229, 3186, 0);
				break;
			case 580:
				c.fade(2964, 3355, 0);
				break;
			case 572:
				c.fade(2460, 3394, 0);
				break;
			case 522: //easy
				c.expModifier = c.EASY;
				c.getDH().sendDialogues(523, c.npcType);
				break;
			case 507: //Bounty Hunter // 1st
				BountyHunter.handleReward(c, 1);
				break;
			case 500: //pure
				c.getItems().deleteItem(2528, 1);
				
				c.getPA().addSkillXP(1210422, Player.playerHitpoints);
				c.getPA().addSkillXP(1210422, Player.playerAttack);
				c.getPA().addSkillXP(1210422, Player.playerStrength);
				c.getPA().addSkillXP(1210422, Player.playerRanged);
				c.getPA().addSkillXP(1210422, Player.playerMagic);
				
				c.dialogueAction = -1;
				break;
			case 412: //Thieving //HAM CAMP
				c.getPA().spellTeleport(3149, 9651, 0);
				break;
				
			case 413: //mining //essence
				c.getPA().spellTeleport(2910, 4832, 0);
				break;
				
			case 481: //Dungeons //Brimhaven
				if(Misc.random(20) <= 1) {
					c.getPA().closeAllWindows();
					c.fadeDungeon(2463, 4773, 0);
					return;
				}
				c.getPA().spellTeleport(2707, 9564, 0);
				break;
				
			case 485:
				c.getRG().buyArrows();
				break;
				
			case 550:
				c.getShops().openShop(79);
				break;
				
			case 416:
				c.getPA().showInterface(8292);
				break;
				
			case 420:
				c.getShops().openShop(64);
				break;
				
			case 422:
				c.getShops().openShop(6);
				break;
				
			case 428:
				c.getShops().openShop(22);
				break;
				
			case 432:
				c.getShops().openShop(10);
				break;
				
			case 2:
				c.getPA().startTeleport(3428, 3538, 0, "modern");
				break;
				
			case 3:
				c.getPA().startTeleport(Config.EDGEVILLE_X, Config.EDGEVILLE_Y, 0, "modern");
				break;
				
			case 4:
				c.getPA().startTeleport(3565, 3314, 0, "modern");
				break;
				
			case 401:
				c.getDH().sendDialogues(402, c.npcType);
				break;
				
			}
			if (c.usingGlory) {
				c.getPA().startTeleport(Config.EDGEVILLE_X, Config.EDGEVILLE_Y, 0, "modern");
			}			
			if (c.teleAction == 2) {
				//barrows
				//c.getPA().spellTeleport(3565, 3314, 0);
				//BA
				c.getPA().spellTeleport(2602, 3155, 0);
			}
			if(c.caOption4a) {
				c.getDH().sendDialogues(102, c.npcType);
				c.caOption4a = false;
			}
			if(c.caOption4c) {
				c.getDH().sendDialogues(118, c.npcType);
				c.caOption4c = false;
			}
			c.dialogueAction = -1;
			c.teleAction = -1;
			break;

		case 9179:
			switch(c.dialogueAction) {
			case 752: //display
				if(c.playerRights == 1 || c.playerRights == 2 || c.playerRights == 3) {
					c.sendMessage("You are an In-Game Staff member so you can't switch ranks on this account.");
					c.getPA().closeAllWindows();
					return;
				}
				if(c.fmod == 1) {
					c.playerRights = 9;
					c.logout();
				} else {
					c.sendMessage("You have to be a forum administrator to display this rank.");
					c.getPA().closeAllWindows();
				}
				break;
			case 592:
				c.fade(2852, 2955, 0);
				break;
			case 586:
				c.fade(3291, 3173, 0);
				break;
			case 584:
				c.fade(3127, 3249, 0);
				break;
			case 582:
				c.fade(3259, 3227, 0);
				break;
			case 580:
				c.fade(2914, 3335, 0);
				break;
			case 572:
				c.fade(2465, 3494, 0);
				break;
			case 522: //easy
				c.expModifier = c.NORMAL;
				c.getDH().sendDialogues(523, c.npcType);
				break;
			case 507: //Bounty Hunter // 1st
				BountyHunter.handleReward(c, 2);
				break;
			case 500: //berserker pure
				c.getItems().deleteItem(2528, 1);
				
				c.getPA().addSkillXP(1210422, Player.playerHitpoints);
				c.getPA().addSkillXP(1210422, Player.playerAttack);
				c.getPA().addSkillXP(1210422, Player.playerStrength);
				c.getPA().addSkillXP(1210422, Player.playerRanged);
				c.getPA().addSkillXP(1210422, Player.playerMagic);
				c.getPA().addSkillXP(61513, Player.playerDefence);
				c.getPA().addSkillXP(123661, Player.playerPrayer);
				
				c.dialogueAction = -1;
				break;
			case 412: //Thieving //Draynor
				c.getPA().spellTeleport(3081, 3250, 0);
				break;
				
			case 413: //mining //dwarven mine
				c.getPA().spellTeleport(3045, 9778, 0);
				break;
				
			case 481: //Dungeons //Taverly
				if(Misc.random(20) <= 1) {
					c.getPA().closeAllWindows();
					c.fadeDungeon(2463, 4773, 0);
					return;
				}
				c.getPA().spellTeleport(2884, 9796, 0);
				break;
				
			case 485:
				c.getRG().exchangePoints();
				break;
				
			case 550:
				c.getPA().showInterface(31330);
				break;
				
			case 416:
				c.getPA().spellTeleport(2473, 3438, 0);
				break;
				
			case 420:
				c.getDH().sendDialogues(412, -1);
				return;
				
			case 422:
				c.getPA().spellTeleport(3048, 4809, 0);
				break;
				
			case 428:
				c.getDH().sendDialogues(413, -1);
				return;
				
			case 432:
				c.getDH().sendDialogues(414, -1);
				return;
				
			case 2:
				c.getPA().startTeleport(2884, 3395, 0, "modern");
				break;
				
			case 3:
				c.getPA().startTeleport(3243, 3513, 0, "modern");
				break;
				
			case 4:
				c.getPA().startTeleport(2444, 5170, 0, "modern");
				break;
				
			case 401:
				c.getDH().sendDialogues(404, c.npcType);
				break;
			}
			if (c.usingGlory) {
				c.getPA().startTeleport(Config.AL_KHARID_X, Config.AL_KHARID_Y, 0, "modern");
			}
			if (c.teleAction == 2) {
				//assault
				c.getPA().spellTeleport(2605, 3153, 0);
			}
			if(c.caOption4c) {
				c.getDH().sendDialogues(120, c.npcType);
				c.caOption4c = false;
			}	
			if(c.caPlayerTalk1) {
				c.getDH().sendDialogues(125, c.npcType);
				c.caPlayerTalk1 = false;
			}
			c.dialogueAction = -1;
			c.teleAction = -1;
			break;

		case 9180:
			switch(c.dialogueAction) {
			case 752: //display
				if(c.playerRights == 1 || c.playerRights == 2 || c.playerRights == 3) {
					c.sendMessage("You are an In-Game Staff member so you can't switch ranks on this account.");
					c.getPA().closeAllWindows();
					return;
				}
				c.playerRights = 0;
				c.logout();
				break;
			case 592:
				c.fade(2778, 2917, 0);
				break;
			case 586:
				c.fade(3300, 3307, 0);
				break;
			case 584:
				c.fade(3108, 3160, 0);
				break;
			case 582:
				c.fade(3253, 3267, 0);
				break;
			case 580:
				c.fade(2956, 3506, 0);
				break;
			case 572:
				c.fade(2423, 3504, 0);
				break;
			case 522: //easy
				c.expModifier = c.HARD;
				c.getDH().sendDialogues(523, c.npcType);
				break;
			case 507: //Bounty Hunter // 1st
				BountyHunter.handleReward(c, 3);
				break;
			case 500: //tanker
				c.getItems().deleteItem(2528, 1);
				
				c.getPA().addSkillXP(1210422, Player.playerHitpoints);
				c.getPA().addSkillXP(45530, Player.playerAttack);
				c.getPA().addSkillXP(45530, Player.playerStrength);
				c.getPA().addSkillXP(1210422, Player.playerRanged);
				c.getPA().addSkillXP(1210422, Player.playerMagic);
				c.getPA().addSkillXP(273743, Player.playerDefence);
				c.getPA().addSkillXP(55650, Player.playerPrayer);
				
				c.dialogueAction = -1;
				break;
			case 412: //Thieving //Ardougne
				c.getPA().spellTeleport(2662, 3307, 0);
				break;
				
			case 413: //mining //shilo
				c.getPA().spellTeleport(2826, 2998, 0);
				break;
				
			case 481: //Dungeons //fremmennik slayer dung
				if(Misc.random(20) <= 1) {
					c.getPA().closeAllWindows();
					c.fadeDungeon(2463, 4773, 0);
					return;
				}
				c.getPA().spellTeleport(2806, 10002, 0);
				break;
			
			case 485:
				c.getRG().howAmIDoing();
				break;
				
			case 416:
			case 420:
			case 422:
			case 428:
			case 432:
			case 550:
				SkillMasters.addSkillCape(c);
				break;
				
			case 2:
				c.getPA().startTeleport(2471,10137, 0, "modern");
				break;
				
			case 3:
				c.getPA().startTeleport(3363, 3676, 0, "modern");
				break;
				
			case 4:
				c.getPA().startTeleport(2659, 2676, 0, "modern");
				break;
				
			case 401:
				c.getDH().sendDialogues(410, c.npcType);
				break;
			}
			if (c.usingGlory) {
				c.getPA().startTeleport(Config.KARAMJA_X, Config.KARAMJA_Y, 0, "modern");
			}
			if (c.teleAction == 2) {
				//duel arena
				c.getPA().spellTeleport(3366, 3266, 0);
			}
			if(c.caOption4c) {
				c.getDH().sendDialogues(122, c.npcType);
				c.caOption4c = false;
			}
			if(c.caPlayerTalk1) {
				c.getDH().sendDialogues(127, c.npcType);
				c.caPlayerTalk1 = false;
			}
			c.dialogueAction = -1;
			c.teleAction = -1;
			break;

		case 9181:
			switch(c.dialogueAction) {
			case 752: //display
				c.getDH().sendDialogues(751, -1);
				return;
			case 592:
				c.fade(2696, 3211, 0);
				break;
			case 586:
				c.fade(3304, 3116, 0);
				break;
			case 584:
				c.fade(3110, 9686, 0);
				break;
			case 582:
				c.fade(3236, 3295, 0);
				break;
			case 580:
				c.fade(3007, 3476, 0);
				break;
			case 572:
				c.fade(2532, 3167, 0);
				break;
			case 522: //easy
				c.expModifier = c.EXTREME;
				c.getDH().sendDialogues(523, c.npcType);
				break;
			case 507: //Bounty Hunter // 1st
				c.getPA().closeAllWindows();
				break;
			case 500: //master
				c.getItems().deleteItem(2528, 1);
				
				c.getPA().addSkillXP(1210422, Player.playerHitpoints);
				c.getPA().addSkillXP(1210422, Player.playerAttack);
				c.getPA().addSkillXP(1210422, Player.playerStrength);
				c.getPA().addSkillXP(1210422, Player.playerRanged);
				c.getPA().addSkillXP(1210422, Player.playerMagic);
				c.getPA().addSkillXP(1210422, Player.playerDefence);
				c.getPA().addSkillXP(101334, Player.playerPrayer);
				
				c.dialogueAction = -1;
				break;
			case 412: //Thieving //menaphos/sophanem
				c.getPA().spellTeleport(3270, 2784, 0);
				break;
				
			case 413: //mining //deep wil mine
				c.getPA().spellTeleport(3062, 3884, 0);
				break;
				
			case 481: //Dungeons //edgeville dung
				if(Misc.random(20) <= 1) {
					c.getPA().closeAllWindows();
					c.fadeDungeon(2463, 4773, 0);
					return;
				}
				c.getPA().spellTeleport(3140, 9915, 0);
				break;
				
			case 416:
			case 420:
			case 422:
			case 428:
			case 432:
			case 485:
			case 550:
				c.getPA().closeAllWindows();
				break;
				
			case 2:
				c.getPA().startTeleport(2669,3714, 0, "modern");
				break;
				
			case 3:
				c.getPA().startTeleport(2540, 4716, 0, "modern");
				break;
				
			case 4:
				c.getPA().startTeleport(3366, 3266, 0, "modern");
				c.sendMessage("Dueling is at your own risk. Refunds will not be given for items lost due to glitches.");
				break;
				
			case 401:
				c.getPA().closeAllWindows();
				//c.getSlayer().taskClan();
				break;
			}
			if (c.usingGlory) {
				c.getPA().startTeleport(Config.MAGEBANK_X, Config.MAGEBANK_Y, 0, "modern");
			}
			if(c.caOption4c) {
				c.getDH().sendDialogues(124, c.npcType);
				c.caOption4c = false;
			}
			if(c.caPlayerTalk1) {
				c.getDH().sendDialogues(130, c.npcType);
				c.caPlayerTalk1 = false;
			}
			c.dialogueAction = -1;
			c.teleAction = -1;
			break;

		case 1093:
		case 1094:
		case 1097:
			if (c.autocastId > 0) {
				c.getPA().resetAutocast();
			} else {
				if (c.playerMagicBook == 1) {
					if (c.playerEquipment[c.playerWeapon] == 4675)
						c.setSidebarInterface(0, 1689);
					else
						c.sendMessage("You can't autocast ancients without an ancient staff.");
				} else if (c.playerMagicBook == 0) {
					if (c.playerEquipment[c.playerWeapon] == 4170) {
						c.setSidebarInterface(0, 12050);
					} else {
						c.setSidebarInterface(0, 1829);
					}	
				}

			}		
			break;
			
		case 26010:
			c.getPA().resetAutocast();
			break;

		case 9157:
			switch(c.dialogueAction) {
			case 596:
				c.fade(2884, 9798, 0);
				break;
			case 594:
				c.fade(2703, 9566, 0);
				break;
			case 564:
				Sailing.startTravel(c, 12);
				break;
				
			case 566:
				Sailing.startTravel(c, 16);
				break;
			}
			if(c.dialogueAction == 554) {
				c.getPA().showInterface(2808);
				return;
			}
			if(c.dialogueAction == 553) {
				if(c.isResetting) {
					for (int j = 0; j < c.playerEquipment.length; j++)
						if(c.playerEquipment[j] > 0) {
							c.getDH().sendStatement("Please remove all your equipment.");
							c.isResetting = false;
							c.nextChat = 0;
							return;
						}
						c.playerXP[21] = (c.getPA().getXPForLevel(1)+ 1);
						c.playerLevel[21] = c.getPA().getLevelForXP(c.playerXP[21]);
						c.getPA().refreshSkill(21);
						c.getPA().closeAllWindows();
						c.getPA().reloadConstructionStrings();
						c.isResetting = false;
						return;
				}
				if(c.rubbedLamp) {
					if(c.getItems().playerHasItem(4447)) {
						c.getItems().deleteItem2(4447, 1);
						c.getPA().closeAllWindows();
						c.getPA().addSkillXP((c.getPA().getXPForLevel(70)+1),21);
						c.getPA().reloadConstructionStrings();
						c.rubbedLamp = false;
						return;
					}
				}
			}
			if(c.dialogueAction == 570 && c.RuneMysteries == 0) {
	               c.getDH().sendDialogues(1109, 741);
	        return;
	  }
			if(c.dialogueAction == 605 && c.cookAss == 0) {
				c.getDH().sendDialogues(606, 278);
				return;
			}
			if(c.dialogueAction == 408) {
				c.needsNewTask = true;
				c.getSlayer().generateTask();
			}
			if(c.dialogueAction == 300) {
				c.getDH().sendDialogues(320, 3001);
				}
			/*QUESTS*/
			if (c.dialogueAction == 100) {
				c.getDH().sendDialogues(105, 278);
			}
			/*ENDOFQUESTS*/
			if(c.dialogueAction == 1) {
				Barrows.teleportUnderground(c);
				c.getPA().closeAllWindows();
			}
			if(c.dialogueAction == 14) {
				c.getPA().showInterface(3559); 
				c.canChangeAppearance = true;
			}
			if(c.dialogueAction == 15) {
				c.getShops().openShop(10);
			}
			if (c.dialogueAction == 17) {
				c.getPA().movePlayer(3056, 9555, 0);
				c.getPA().closeAllWindows();
			}
			if (c.dialogueAction == 18) {
				c.getShops().openShop(11);
			}
			if (c.dialogueAction == 19) {
				c.getShops().openShop(15);
			}
			if (c.dialogueAction == 20) {
				c.getPA().movePlayer(2525, 4777, 0);
				c.getPA().closeAllWindows();
			}
			if (c.dialogueAction == 21) {
				c.getPA().spellTeleport(3428, 3537, 0);
				c.getPA().closeAllWindows();
			}
			if (c.dialogueAction == 22) {
				c.getPA().movePlayer(2670, 3714, 0);
				c.getPA().closeAllWindows();
			}
			if (c.dialogueAction == 23) {
				c.getShops().openShop(21);
			}
			if (c.dialogueAction == 24) {
				c.getShops().openShop(22);
			}
			if (c.dialogueAction == 8) {
				c.getPA().resetBarrows();
				c.getPA().closeAllWindows();
			}
			if (c.dialogueAction == 25) {
				c.getPA().movePlayer(2473, 3438, 0);
				c.getPA().closeAllWindows();
			}
			if(c.dialogueAction == 9001) {
				c.getPA().spellTeleport(3333, 3333, 0);
				//c.getPA().startTeleport(3333, 3333, 0, "modern");
			}
			c.dialogueAction = -1;
			c.teleAction = -1;
			break;
		case 9167:
			if(c.teleAction == 7){
				c.getPA().spellTeleport(Config.LUMBY_X, Config.LUMBY_Y, 0);
				c.teleAction = -1;
			}
			switch(c.dialogueAction) {
			case 578:
				c.fade(3239, 9866, 0);
				break;
			case 574:
				c.fade(2343, 3170, 0);
				break;
			case 552:
				c.getShops().openShop(80);
				break;
			case 515:
				c.adventurerPid = true;
				c.getPA().addStarter();
				c.getDH().sendDialogues(473, 2244);
				break;
			case 479: //desert //scorpions
				if(Misc.random(20) <= 1) {
					c.getPA().closeAllWindows();
					c.fadeDesert(3318, 3068, 4);
					return;
				}
				c.getPA().spellTeleport(3300, 3277, 0);
				break;
				
			case 480: //snowmountain //ice queen
				c.getPA().spellTeleport(3056, 9555, 4);
				break;
			
			case 418:
				c.getShops().openShop(12);
				break;
				
			case 424:
				c.getShops().openShop(13);
				break;
				
			case 426:
				c.getShops().openShop(63);
				break;
				
			case 430:
				c.getShops().openShop(62);
				break;
				
			case 434:
				c.getShops().openShop(66);
				break;
				
			case 436:
				c.getShops().openShop(65);
				break;
				
			case 438:
				c.getShops().openShop(16);
				break;
				
			case 440:
				c.getShops().openShop(15);
				break;
				
			case 443:
				c.getSlayer().handleInterface("buy");
				break;
				
			case 441:
				if(c.getPA().getLevelForXP(c.playerXP[Player.playerStrength]) >= 99) {
					c.getShops().openShop(69);
				} else {
					c.getDH().sendStatement("You need a strength level of 99 to open this shop.");
					return;
				}
				break;
				
			case 446:
				if(c.getPA().getLevelForXP(c.playerXP[Player.playerAttack]) >= 99) {
					c.getShops().openShop(70);
				} else {
					c.getDH().sendStatement("You need an attack level of 99 to open this shop.");
					return;
				}
				break;
				
			case 448:
				c.getShops().openShop(71);
				break;
				
			case 450:
				if(c.getPA().getLevelForXP(c.playerXP[Player.playerDefence]) >= 99) {
					c.getShops().openShop(67);
				} else {
					c.getDH().sendStatement("You need a defence level of 99 to open this shop.");
					return;
				}
				break;
				
			case 452:
				c.getShops().openShop(72);
				break;
				
			case 454:
				c.getShops().openShop(68);
				break;
				
			case 456:
				c.getShops().openShop(14);
				break;
				
			case 13:
				c.getPA().movePlayer(3691, 3513, 0);
				c.sendMessage("The sailor takes you to Port Phasmatys.");
				c.getPA().closeAllWindows();
				break;
				
			case 29:
				if(c.votePoints > 0) {
					c.votePoints -= 1;
					c.getItems().addItem(995, 500000);
					c.getDH().sendDialogues(1700, 805);
				} else if (c.votePoints == 0) {
					c.getDH().sendDialogues(1699, 805);
				}
				break;
				
			case 16:
				c.getPA().startTeleport(3367, 3268, 0, "modern");
				break;
			}
			c.dialogueAction = -1;
			c.teleAction = -1;
			break;
			
		case 9168:
			if(c.teleAction == 7){
				c.getPA().spellTeleport(Config.VARROCK_X, Config.VARROCK_Y, 0);
				c.teleAction = -1;
			}
			switch(c.dialogueAction) {
			case 578:
				c.fade(3213, 3459, 0);
				break;
			case 574:
				c.fade(2186, 3147, 0);
				break;
			case 515:
				c.pkerPid = true;
				c.getPA().addStarter();
				c.getDH().sendDialogues(473, 2244);
				break;
			case 479: //desert //al-kharid
				if(Misc.random(20) <= 1) {
					c.getPA().closeAllWindows();
					c.fadeDesert(3318, 3068, 4);
					return;
				}
				c.getPA().spellTeleport(3293, 3178, 0);
				break;
				
			case 480: //snowmountain //ice warriors
				c.getPA().spellTeleport(3056, 9562, 0);
				break;
				
			case 418:
			case 424:
			case 426:
			case 430:
			case 434:
			case 436:
			case 438:
			case 440:
			case 443:
			case 441:
			case 446:
			case 448:
			case 450:
			case 452:
			case 454:
			case 456:
			case 552:
				SkillMasters.addSkillCape(c);
				break;
				
			case 13:
				c.getPA().movePlayer(2956, 3146, 0);
				c.sendMessage("The sailor takes you to Karamja.");
				c.getPA().closeAllWindows();
				break;
				
			case 29:
				c.getShops().openShop(61);
				break;
				
			case 16:
				c.getPA().startTeleport(3565, 3316, 0, "modern");
				break;
			}
			c.dialogueAction = -1;
			c.teleAction = -1;
			break;
			
		case 9169:
			if(c.teleAction == 7){
				c.getPA().spellTeleport(Config.FALADOR_X, Config.FALADOR_Y, 0);
				c.teleAction = -1;
			}
			switch(c.dialogueAction) {
			case 578:
				c.fade(3221, 3372, 0);
				break;
			case 574:
				c.fade(2202, 3253, 0);
				break;
			case 515:
				c.skillerPid = true;
				c.getPA().addStarter();
				c.getDH().sendDialogues(473, 2244);
				break;
			case 479: //desert //bandit camp
				if(Misc.random(20) <= 1) {
					c.getPA().closeAllWindows();
					c.fadeDesert(3318, 3068, 4);
					return;
				}
				c.getPA().spellTeleport(3172, 2982, 0);
				break;
				
			case 480: //snowmountain //mithril
				c.getPA().spellTeleport(1748, 5327, 0);
				break;
				
			case 418:
			case 424:
			case 426:
			case 430:
			case 434:
			case 436:
			case 438:
			case 440:
			case 443:
			case 441:
			case 446:
			case 448:
			case 450:
			case 452:
			case 454:
			case 456:
			case 552:
				c.getPA().closeAllWindows();
				break;
			case 13:
				c.getPA().movePlayer(2772, 3234, 0);
				c.sendMessage("The sailor takes you to Brimhaven.");
				c.getPA().closeAllWindows();
				break;
				
			case 29:
				c.getPA().sendFrame126("www.biohazard-rsps.com/vote/", 12000);
				c.getPA().closeAllWindows();
				break;
				
			case 16:
				c.getPA().startTeleport(2480, 5175, 0, "modern");
				break;
			}
			c.dialogueAction = -1;
			c.teleAction = -1;
			break;

		case 9158:  
			switch(c.dialogueAction) {
			case 564:
				Sailing.startTravel(c, 11);
				break;
			}
			if(c.dialogueAction == 554) {
				c.getDH().sendDialogues(553, -1);
				return;
			}
			if(c.dialogueAction == 553) {
				if(c.isResetting) {
					for (int j = 0; j < c.playerEquipment.length; j++)
						if(c.playerEquipment[j] > 0) {
							c.getDH().sendStatement("Please remove all your equipment.");
							c.isResetting = false;
							return;
						}
						c.playerXP[22] = (c.getPA().getXPForLevel(1)+ 1);
						c.playerLevel[22] = c.getPA().getLevelForXP(c.playerXP[22]);
						c.getPA().refreshSkill(22);
						c.getPA().closeAllWindows();
						c.getPA().reloadHunterStrings();
						c.isResetting = false;
						return;
				}
				if(c.rubbedLamp) {
					if(c.getItems().playerHasItem(4447)) {
						c.getItems().deleteItem2(4447, 1);
						c.getPA().closeAllWindows();
						c.getPA().addSkillXP((c.getPA().getXPForLevel(70)+1),22);
						c.getPA().reloadHunterStrings();
						c.rubbedLamp = false;
						return;
					}
				}
			}
			if(c.dialogueAction == 408) {
				c.getPA().closeAllWindows();
			}
			if(c.dialogueAction == 300) {
				c.getDH().sendDialogues(310, 6139);
				}
			/*QUESTS*/
			if (c.dialogueAction == 100) {
				c.getDH().sendDialogues(107, 278);
			}
			/*ENDOFQUESTS*/
			if (c.dialogueAction == 8) {
				c.getPA().fixAllBarrows();
				c.getPA().closeAllWindows();
			}
			if (c.dialogueAction == 14) {
				c.getDH().sendDialogues(65, 0);
			}
			if (c.dialogueAction == 15) {
				c.getDH().sendDialogues(65, 0);
			}
			if (c.dialogueAction == 17) {
				c.getDH().sendDialogues(65, 0);
			}
			if (c.dialogueAction == 18) {
				c.getPA().closeAllWindows();
			}
			if (c.dialogueAction == 19) {
				c.getDH().sendDialogues(65, 0);
			}
			if (c.dialogueAction == 20) {
				c.getDH().sendDialogues(65, 0);
			}
			if (c.dialogueAction == 21) {
				c.getDH().sendDialogues(65, 0);
			}
			if (c.dialogueAction == 22) {
				c.getPA().closeAllWindows();
			}
			if (c.dialogueAction == 23) {
				c.getDH().sendDialogues(65, 0);
			}
			if (c.dialogueAction == 24) {
				c.getDH().sendDialogues(65, 0);
			}
			if (c.dialogueAction == 25) {
				c.getDH().sendDialogues(65, 0);
				c.getPA().closeAllWindows();
			}
			c.dialogueAction = -1;
			c.teleAction = -1;
			break;

			/**Specials**/
		case 29188:
			c.specBarId = 7636; // the special attack text - sendframe126(S P E C I A L  A T T A C K, c.specBarId);
			c.usingSpecial = !c.usingSpecial;
			c.getItems().updateSpecialBar();
			break;

		case 29163:
			c.specBarId = 7611;
			c.usingSpecial = !c.usingSpecial;
			c.getItems().updateSpecialBar();
			break;

		case 33033:
			c.specBarId = 8505;
			c.usingSpecial = !c.usingSpecial;
			c.getItems().updateSpecialBar();
			break;

		case 29038:
			c.specBarId = 7486;
			/*if (c.specAmount >= 5) {
				c.attackTimer = 0;
				c.getCombat().attackPlayer(c.playerIndex);
				c.usingSpecial = true;
				c.specAmount -= 5;
			}*/
			c.getCombat().handleGmaulPlayer();
			c.getItems().updateSpecialBar();
			break;

		case 29063:
			if(c.getCombat().checkSpecAmount(c.playerEquipment[c.playerWeapon])) {
				c.gfx0(246);
				c.forcedChat("Raarrrrrgggggghhhhhhh!");
				c.startAnimation(1056);
				c.playerLevel[2] = c.getLevelForXP(c.playerXP[2]) + (c.getLevelForXP(c.playerXP[2]) * 15 / 100);
				c.getPA().refreshSkill(2);
				c.getItems().updateSpecialBar();
			} else {
				c.sendMessage("You don't have the required special energy to use this attack.");
			}
			break;

		case 48023:
			c.specBarId = 12335;
			c.usingSpecial = !c.usingSpecial;
			c.getItems().updateSpecialBar();
			break;

		case 29138:
			c.specBarId = 7586;
			c.usingSpecial = !c.usingSpecial;
			c.getItems().updateSpecialBar();
			break;

		case 29113:
			c.specBarId = 7561;
			c.usingSpecial = !c.usingSpecial;
			c.getItems().updateSpecialBar();
			break;

		case 29238:
			c.specBarId = 7686;
			c.usingSpecial = !c.usingSpecial;
			c.getItems().updateSpecialBar();
			break;

			/**Dueling**/			
		case 26065: // no forfeit
		case 26040:
			c.duelSlot = -1;
			c.getTradeAndDuel().selectRule(0);
			break;

		case 26066: // no movement
		case 26048:
			c.duelSlot = -1;
			c.getTradeAndDuel().selectRule(1);
			break;

		case 26069: // no range
		case 26042:
			c.duelSlot = -1;
			c.getTradeAndDuel().selectRule(2);
			break;

		case 26070: // no melee
		case 26043:
			c.duelSlot = -1;
			c.getTradeAndDuel().selectRule(3);
			break;				

		case 26071: // no mage
		case 26041:
			c.duelSlot = -1;
			c.getTradeAndDuel().selectRule(4);
			break;

		case 26072: // no drinks
		case 26045:
			c.duelSlot = -1;
			c.getTradeAndDuel().selectRule(5);
			break;

		case 26073: // no food
		case 26046:
			c.duelSlot = -1;
			c.getTradeAndDuel().selectRule(6);
			break;

		case 26074: // no prayer
		case 26047:	
			c.duelSlot = -1;
			c.getTradeAndDuel().selectRule(7);
			break;

		case 26076: // obsticals
		case 26075:
			c.duelSlot = -1;
			c.getTradeAndDuel().selectRule(8);
			break;

		case 2158: // fun weapons
		case 2157:
			c.duelSlot = -1;
			c.getTradeAndDuel().selectRule(9);
			break;

		case 30136: // sp attack
		case 30137:
			c.duelSlot = -1;
			c.getTradeAndDuel().selectRule(10);
			break;	

		case 53245: //no helm
			c.duelSlot = 0;
			c.getTradeAndDuel().selectRule(11);
			break;

		case 53246: // no cape
			c.duelSlot = 1;
			c.getTradeAndDuel().selectRule(12);
			break;

		case 53247: // no ammy
			c.duelSlot = 2;
			c.getTradeAndDuel().selectRule(13);
			break;

		case 53249: // no weapon.
			c.duelSlot = 3;
			c.getTradeAndDuel().selectRule(14);
			break;

		case 53250: // no body
			c.duelSlot = 4;
			c.getTradeAndDuel().selectRule(15);
			break;

		case 53251: // no shield
			c.duelSlot = 5;
			c.getTradeAndDuel().selectRule(16);
			break;

		case 53252: // no legs
			c.duelSlot = 7;
			c.getTradeAndDuel().selectRule(17);
			break;

		case 53255: // no gloves
			c.duelSlot = 9;
			c.getTradeAndDuel().selectRule(18);
			break;

		case 53254: // no boots
			c.duelSlot = 10;
			c.getTradeAndDuel().selectRule(19);
			break;

		case 53253: // no rings
			c.duelSlot = 12;
			c.getTradeAndDuel().selectRule(20);
			break;

		case 53248: // no arrows
			c.duelSlot = 13;
			c.getTradeAndDuel().selectRule(21);
			break;


		case 26018:	
			Client o = (Client) PlayerHandler.players[c.duelingWith];
			if(o == null) {
				c.getTradeAndDuel().declineDuel();
				return;
			}
			if (c.duelRule[0] && c.duelRule[1]) {
				c.sendMessage("Either turn off No Forfeit or No Movement!");
				break;
			}
			if(c.duelRule[2] && c.duelRule[3] && c.duelRule[4]) {
				c.sendMessage("You won't be able to attack the player with the rules you have set.");
				break;
			}
			c.duelStatus = 2;
			if(c.duelStatus == 2) {
				c.getPA().sendFrame126("Waiting for other player...", 6684);
				o.getPA().sendFrame126("Other player has accepted.", 6684);
			}
			if(o.duelStatus == 2) {
				o.getPA().sendFrame126("Waiting for other player...", 6684);
				c.getPA().sendFrame126("Other player has accepted.", 6684);
			}

			if(c.duelStatus == 2 && o.duelStatus == 2) {
				c.canOffer = false;
				o.canOffer = false;
				c.duelStatus = 3;
				o.duelStatus = 3;
				c.getTradeAndDuel().confirmDuel();
				o.getTradeAndDuel().confirmDuel();
			}
			break;
			
		case 25120:
			if(c.duelStatus == 5) {
				break;
			}
			final Client o1 = (Client) PlayerHandler.players[c.duelingWith];
			if(o1 == null) {
				c.getTradeAndDuel().declineDuel();
				return;
			}

			c.duelStatus = 4;
			if(o1.duelStatus == 4 && c.duelStatus == 4) {
				c.freezeTimer = 1;
				o1.freezeTimer = 1;
				c.getTradeAndDuel().startDuel();
				o1.getTradeAndDuel().startDuel();
				o1.duelCount = 4;
				c.duelCount = 4;
				CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer container) {
						if(System.currentTimeMillis() - c.duelDelay > 800 && c.duelCount > 0) {
							if(c.duelCount != 1) {
								c.forcedChat(""+(--c.duelCount));
								c.duelDelay = System.currentTimeMillis();
							} else {
								c.damageTaken = new int[Config.MAX_PLAYERS];
								c.forcedChat("FIGHT!");
								c.duelCount = 0;
							}
						}
						if (c.duelCount == 0) {
							container.stop();
						}
					}
					@Override
					public void stop() {
					}
				}, 1);
				CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer container) {
						if(System.currentTimeMillis() - o1.duelDelay > 800 && o1.duelCount > 0) {
							if(o1.duelCount != 1) {
								o1.forcedChat(""+(--o1.duelCount));
								o1.duelDelay = System.currentTimeMillis();
							} else {
								o1.damageTaken = new int[Config.MAX_PLAYERS];
								o1.forcedChat("FIGHT!");
								o1.duelCount = 0;
							}
						}
						if (o1.duelCount == 0) {
							container.stop();
						}
					}
					@Override
					public void stop() {
					}
				}, 1);
				c.duelDelay = System.currentTimeMillis();
				o1.duelDelay = System.currentTimeMillis();
			} else {
				c.getPA().sendFrame126("Waiting for other player...", 6571);
				o1.getPA().sendFrame126("Other player has accepted", 6571);
			}
			break;


		case 4169: // god spell charge
			c.usingMagic = true;
			if(!c.getCombat().checkMagicReqs(48)) {
				break;
			}

			if(System.currentTimeMillis() - c.godSpellDelay < Config.GOD_SPELL_CHARGE) {
				c.sendMessage("You still feel the charge in your body!");
				break;
			}
			c.godSpellDelay	= System.currentTimeMillis();
			c.sendMessage("You feel charged with a magical power!");
			c.gfx100(c.MAGIC_SPELLS[48][3]);
			c.startAnimation(c.MAGIC_SPELLS[48][2]);
			c.usingMagic = false;
			break;			
		case 154:
			//skillcapeshere
			SkillcapeData.handleSkillcape(c);
			break;
		case 152:
			if (c.playerEnergy < 1) {
				c.isRunning2 = false;
				c.getPA().sendFrame36(173, 0);
				return;
			}
			c.isRunning2 = !c.isRunning2;
			int frame = c.isRunning2 == true ? 1 : 0;
			c.getPA().sendFrame36(173,frame);
			break;
		case 32195://1
		case 32196:
			c.getAgil().gnomeTicketCounter(c, "1", 2996, 1, 1000);
			break;
		case 32203://10
		case 32197:
			c.getAgil().gnomeTicketCounter(c, "10", 2996, 10, 10000);
			break;
		case 32204://25
		case 32198:
			c.getAgil().gnomeTicketCounter(c, "25", 2996, 25, 25000);
			break;
		case 32199://100
		case 32205:
			c.getAgil().gnomeTicketCounter(c, "100", 2996, 100, 100000);
			break;
		case 32200://1000
		case 32206:
			c.getAgil().gnomeTicketCounter(c, "1000", 2996, 1000, 1000000);
			break;
		case 32192://toadflex
		case 32190:
		case 32202://snapdragon
		case 32201:
		case 32193://piratehook
		case 32189:
			c.sendMessage("Not Available!");
			break;
		case 9154:
			c.logout();
			break;

		case 21010:
			c.takeAsNote = true;
			break;

		case 21011:
			c.takeAsNote = false;
			break;
			//home teleports
		case 4171:
		case 50056:
		case 117048:
			c.getPA().spellTeleport(Config.EDGEVILLE_X, Config.EDGEVILLE_Y, 0);
			break;

		case 4140:
		case 50235:
		case 117112:
			if (System.currentTimeMillis() - c.lastTeleport > 5000) {
			//c.getPA().startTeleport(Config.LUMBY_X, Config.LUMBY_Y, 0, "modern");
			c.getDH().sendOption5("Islands", "Desert", "Snow Mountain", "Slayer Tower", "Dungeons");
			c.teleAction = 1;
			}
			break;

		case 4143:
		case 50245:
		case 117123:
			c.getPA().spellTeleport(3028, 3228, 0);
			break;

		case 4146:
		case 50253:
		case 117131:
			if (System.currentTimeMillis() - c.lastTeleport > 5000) {
				c.getDH().sendOption5("Barbarian Assault", "Duel Arena", "Barrows", "Pest Control", "Next ->");
				c.teleAction = 2;
			}
			break;

		case 4150:
		case 51005:
		case 117154:
			if (System.currentTimeMillis() - c.lastTeleport > 5000) {
				c.getDH().sendOption5("Godwars Dungeon", "King Black Dragon", "Dagannoth Lair", "Kalphite Lair", "Next ->");
				c.teleAction = 3;
			}
			break;			

		case 6004:
		case 51013:
		case 117162:
			if (System.currentTimeMillis() - c.lastTeleport > 5000) {
				c.getDH().sendOption5("Edgeville Pk", "Castle Pk", "Magebank Pk", "Hill Giants Pk", "Ardy Lever");
				c.teleAction = 4;
			}
			break; 

		case 6005:
		case 51023:
		case 117186:
			if (System.currentTimeMillis() - c.lastTeleport > 5000) {
				c.getDH().sendOption5("Agility", "Cooking", "Crafting", "Farming", "Next ->");
				c.teleAction = 5;
			}
			break; 

		case 29031:
		case 51031:
		case 117210:

			break; 		

		case 72038:
			if (System.currentTimeMillis() - c.lastTeleport > 5000) {
				if (c.playerLevel[6] >= 64) {
					if (c.getItems().playerHasItem(555, 2) && c.getItems().playerHasItem(563, 2) && c.getItems().playerHasItem(554, 2) && c.getItems().playerHasItem(1963, 1)) {
						c.getPA().spellTeleport(2760 + Misc.random(3), 2782 +  Misc.random(2), 0);
						c.getItems().deleteItem(555, 2);
						c.getItems().deleteItem(563, 2);
						c.getItems().deleteItem(554, 2);
						c.getItems().deleteItem(1963, 1);
						c.teleAction = 8;
						c.lastTeleport = System.currentTimeMillis();
					} else {
						c.sendMessage("You do not have enough runes to cast this spell.");
					}
				} else {
					c.sendMessage("You need a higher Magic level to cast this spell.");
				}
			}
			break;


		case 9125: //Accurate
		case 6221: // range accurate
		case 22230: //punch (unarmed)
		case 48010: //flick (whip)
		case 21200: //spike (pickaxe)
		case 1080: //bash (staff)
		case 6168: //chop (axe)
		case 6236: //accurate (long bow)
		case 17102: //accurate (darts)
		case 8234: //stab (dagger)
			c.fightMode = 0;
			if (c.autocasting) {
				c.getPA().resetAutocast();
			}
			break;

		case 9126: //Defensive
		case 48008: //deflect (whip)
		case 22228: //block (unarmed)
		case 21201: //block (pickaxe)
		case 1078: //focus - block (staff)
		case 6169: //block (axe)
		case 33019: //fend (hally)
		case 18078: //block (spear)
		case 8235: //block (dagger)
			c.fightMode = 1;
			if (c.autocasting) {
				c.getPA().resetAutocast();
			}
			break;

		case 9127: // Controlled
		case 48009: //lash (whip)
		case 33018: //jab (hally)
		case 6234: //longrange (long bow)
		case 6219: //longrange
		case 18077: //lunge (spear)
		case 18080: //swipe (spear)
		case 18079: //pound (spear)
		case 17100: //longrange (darts)
			c.fightMode = 3;
			if (c.autocasting) {
				c.getPA().resetAutocast();
			}
			break;

		case 9128: //Aggressive
		case 6220: // range rapid
		case 22229: //kick (unarmed)
		case 21203: //impale (pickaxe)
		case 21202: //smash (pickaxe)
		case 1079: //pound (staff)
		case 6171: //hack (axe)
		case 6170: //smash (axe)
		case 33020: //swipe (hally)
		case 6235: //rapid (long bow)
		case 17101: //repid (darts)
		case 8237: //lunge (dagger)
		case 8236: //slash (dagger)
			c.fightMode = 2;
			if (c.autocasting) {
				c.getPA().resetAutocast();
			}
			break;

		case 13092:
			 if (System.currentTimeMillis() - c.lastButton < 400) {

					c.lastButton = System.currentTimeMillis();

					break;

				} else {

					c.lastButton = System.currentTimeMillis();

				}
			Client ot = (Client) PlayerHandler.players[c.tradeWith];
			if(ot == null) {
				c.getTradeAndDuel().declineTrade();
				c.sendMessage("Trade declined as the other player has disconnected.");
				break;
			}
			c.getPA().sendFrame126("Waiting for other player...", 3431);
			ot.getPA().sendFrame126("Other player has accepted", 3431);	
			c.goodTrade= true;
			ot.goodTrade= true;

			for (GameItem item : c.getTradeAndDuel().offeredItems) {
				if (item.id > 0) {
					if(ot.getItems().freeSlots() < c.getTradeAndDuel().offeredItems.size()) {					
						c.sendMessage(ot.playerName +" only has "+ot.getItems().freeSlots()+" free slots, please remove "+(c.getTradeAndDuel().offeredItems.size() - ot.getItems().freeSlots())+" items.");
						ot.sendMessage(c.playerName +" has to remove "+(c.getTradeAndDuel().offeredItems.size() - ot.getItems().freeSlots())+" items or you could offer them "+(c.getTradeAndDuel().offeredItems.size() - ot.getItems().freeSlots())+" items.");
						c.goodTrade= false;
						ot.goodTrade= false;
						c.getPA().sendFrame126("Not enough inventory space...", 3431);
						ot.getPA().sendFrame126("Not enough inventory space...", 3431);
						break;
					} else {
						c.getPA().sendFrame126("Waiting for other player...", 3431);				
						ot.getPA().sendFrame126("Other player has accepted", 3431);
						c.goodTrade= true;
						ot.goodTrade= true;
					}
				}	
			}	
			if (c.inTrade && !c.tradeConfirmed && ot.goodTrade && c.goodTrade) {
				c.tradeConfirmed = true;
				if(ot.tradeConfirmed) {
					c.getTradeAndDuel().confirmScreen();
					ot.getTradeAndDuel().confirmScreen();
					break;
				}

			}


			break;

		case 13218:
			  if (System.currentTimeMillis() - c.lastButton < 400) {

					c.lastButton = System.currentTimeMillis();

					break;

				} else {

					c.lastButton = System.currentTimeMillis();

				}
			if (c.tradeTime > 0)
				return;
			c.tradeAccepted = true;
			Client ot1 = (Client) PlayerHandler.players[c.tradeWith];
			if (ot1 == null) {
				c.getTradeAndDuel().declineTrade();
				c.sendMessage("Other player declined trade!");
				break;
			}

			if (c.inTrade && c.tradeConfirmed && ot1.tradeConfirmed && !c.tradeConfirmed2) {
				c.tradeConfirmed2 = true;
				if(ot1.tradeConfirmed2) {	
					c.acceptedTrade = true;
					ot1.acceptedTrade = true;
					c.getTradeAndDuel().giveItems();
					c.sendMessage("Accepted trade.");
					ot1.sendMessage("Accepted trade.");
					ot1.getTradeAndDuel().giveItems();
					break;
				}
				ot1.getPA().sendFrame126("Other player has accepted.", 3535);
				c.getPA().sendFrame126("Waiting for other player...", 3535);
			}

			break;		
			/* Rules Interface Buttons */
		case 125011: //Click agree
			if(!c.ruleAgreeButton) {
				c.ruleAgreeButton = true;
				c.getPA().sendFrame36(701, 1);
			} else {
				c.ruleAgreeButton = false;
				c.getPA().sendFrame36(701, 0);
			}
			break;
		case 125003://Accept
			if(c.ruleAgreeButton) {
				c.getPA().showInterface(3559);
				c.newPlayer = false;
			} else if(!c.ruleAgreeButton) {
				c.sendMessage("You need to click on you agree before you can continue on.");
			}
			break;
		case 125006://Decline
			c.sendMessage("You have chosen to decline, Client will be disconnected from the server.");
			break;
			/* End Rules Interface Buttons */
			/* Player Options */
		case 74176:
			if(!c.mouseButton) {
				c.mouseButton = true;
				c.getPA().sendFrame36(500, 1);
				c.getPA().sendFrame36(170,1);
			} else if(c.mouseButton) {
				c.mouseButton = false;
				c.getPA().sendFrame36(500, 0);
				c.getPA().sendFrame36(170,0);					
			}
			break;
		case 74184:
			if(!c.splitChat) {
				c.splitChat = true;
				c.getPA().sendFrame36(502, 1);
				c.getPA().sendFrame36(287, 1);
			} else {
				c.splitChat = false;
				c.getPA().sendFrame36(502, 0);
				c.getPA().sendFrame36(287, 0);
			}
			break;
		case 74180:
			if(!c.chatEffects) {
				c.chatEffects = true;
				c.getPA().sendFrame36(501, 1);
				c.getPA().sendFrame36(171, 0);
			} else {
				c.chatEffects = false;
				c.getPA().sendFrame36(501, 0);
				c.getPA().sendFrame36(171, 1);
			}
			break;
		case 74188:
			if(!c.acceptAid) {
				c.acceptAid = true;
				c.getPA().sendFrame36(503, 1);
				c.getPA().sendFrame36(427, 1);
			} else {
				c.acceptAid = false;
				c.getPA().sendFrame36(503, 0);
				c.getPA().sendFrame36(427, 0);
			}
			break;
		case 74192:
			if(!c.isRunning2) {
				c.isRunning2 = true;
				c.getPA().sendFrame36(504, 1);
				c.getPA().sendFrame36(173, 1);
			} else {
				c.isRunning2 = false;
				c.getPA().sendFrame36(504, 0);
				c.getPA().sendFrame36(173, 0);
			}
			break;
		case 74201://brightness1
			c.getPA().sendFrame36(505, 1);
			c.getPA().sendFrame36(506, 0);
			c.getPA().sendFrame36(507, 0);
			c.getPA().sendFrame36(508, 0);
			c.getPA().sendFrame36(166, 1);
			break;
		case 74203://brightness2
			c.getPA().sendFrame36(505, 0);
			c.getPA().sendFrame36(506, 1);
			c.getPA().sendFrame36(507, 0);
			c.getPA().sendFrame36(508, 0);
			c.getPA().sendFrame36(166,2);
			break;

		case 74204://brightness3
			c.getPA().sendFrame36(505, 0);
			c.getPA().sendFrame36(506, 0);
			c.getPA().sendFrame36(507, 1);
			c.getPA().sendFrame36(508, 0);
			c.getPA().sendFrame36(166,3);
			break;

		case 74205://brightness4
			c.getPA().sendFrame36(505, 0);
			c.getPA().sendFrame36(506, 0);
			c.getPA().sendFrame36(507, 0);
			c.getPA().sendFrame36(508, 1);
			c.getPA().sendFrame36(166,4);
			break;
		case 74206://area1
			c.getPA().sendFrame36(509, 1);
			c.getPA().sendFrame36(510, 0);
			c.getPA().sendFrame36(511, 0);
			c.getPA().sendFrame36(512, 0);
			break;
		case 74207://area2
			c.getPA().sendFrame36(509, 0);
			c.getPA().sendFrame36(510, 1);
			c.getPA().sendFrame36(511, 0);
			c.getPA().sendFrame36(512, 0);
			break;
		case 74208://area3
			c.getPA().sendFrame36(509, 0);
			c.getPA().sendFrame36(510, 0);
			c.getPA().sendFrame36(511, 1);
			c.getPA().sendFrame36(512, 0);
			break;
		case 74209://area4
			c.getPA().sendFrame36(509, 0);
			c.getPA().sendFrame36(510, 0);
			c.getPA().sendFrame36(511, 0);
			c.getPA().sendFrame36(512, 1);
			break;
		case 168:
			c.startAnimation(855);
			break;
		case 169:
			c.startAnimation(856);
			break;
		case 162:
			c.startAnimation(857);
			break;
        case 164:
        if(c.playerEquipment[7] == 10396){ c.startAnimation(5312); } else {  c.startAnimation(858);}
        break;
        case 165:
        if(c.playerEquipment[0] == 10392){ c.startAnimation(5315); } else {  c.startAnimation(859);}
        break;
		case 161:
			c.startAnimation(860);
			break;
		case 170:
			c.startAnimation(861);
			break;
		case 171:
			c.startAnimation(862);
			break;
		case 163:
			c.startAnimation(863);
			break;
		case 167:
			c.startAnimation(864);
			break;
		case 172:
			c.startAnimation(865);
			break;
		/*case 166:
			c.startAnimation(866);
			break;*/
        case 166:
        if(c.playerEquipment[7] == 10394){ c.startAnimation(5316); } else {  c.startAnimation(866);}
        break;
		case 52050:
			c.startAnimation(2105);
			break;
		case 52051:
			c.startAnimation(2106);
			break;
		case 52052:
			c.startAnimation(2107);
			break;
		case 52053:
			c.startAnimation(2108);
			break;
		case 52054:
			c.startAnimation(2109);
			break;
		case 52055:
			c.startAnimation(2110);
			break;
        case 52056:
        if(c.playerEquipment[0] == 10398){ c.startAnimation(5313); } else {  c.startAnimation(2111);}
        break;
		case 52057:
			c.startAnimation(2112);
			break;
		case 52058:
			c.startAnimation(2113);
			break;
		case 43092:
			c.startAnimation(0x558);
			break;
		case 2155:
			c.startAnimation(0x46B);
			break;
		case 25103:
			c.startAnimation(0x46A);
			break;
		case 25106:
			c.startAnimation(0x469);
			break;
		case 2154:
			c.startAnimation(0x468);
			break;
		case 52071:
			c.startAnimation(0x84F);
			break;
		case 52072:
			c.startAnimation(0x850);
			break;
		case 59062:
			c.startAnimation(2836);
			break;
		case 72032:
			c.startAnimation(3544);
			break;
		case 72033:
			c.startAnimation(3543);
			break;
		case 72254:
			c.startAnimation(3866);
			break;
			/* END OF EMOTES */
			
			//CONSTRUCTION INTERFACES
			//PUBLIC - PRIVATE
		case 115121:
			c.getPA().closeAllWindows();
			break;
			case 122099://public
				c.getPA().startTeleport2(2214, 3297, 0);
				c.sendMessage("You teleported to the public Construction zone.");
			break;

			case 122102://private
				c.getPA().startTeleport2(2214, 3297, c.playerId * 4);
				c.sendMessage("You teleported to the private Construction zone.");
			break;

			//CHOOSE WHAT TO BUILD
			case 122019://fern
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 1) {
				c.sendMessage("You need a level 1 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(249, 1) && c.getItems().playerHasItem(1511, 1)) {
				c.getItems().deleteItem2(249, 1);
				c.getItems().deleteItem2(1511, 1);
				c.sendMessage("You build a Fern.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(31 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13432, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
			break;

			case 122022://tree
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 5) {
				c.sendMessage("You need a level 5 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1511, 1) && c.getItems().playerHasItem(1511, 1) && c.getItems().playerHasItem(1511, 1)) {
				c.getItems().deleteItem2(1511, 1);
				c.getItems().deleteItem2(1511, 1);
				c.getItems().deleteItem2(1511, 1);
				c.sendMessage("You build a Tree.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(31 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13411, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
			break;

			case 122025://chair
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 19) {
				c.sendMessage("You need a level 19 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1539, 10) && c.getItems().playerHasItem(8778, 1) && c.getItems().playerHasItem(8778, 1)) {
				c.getItems().deleteItem2(1539, 10);
				c.getItems().deleteItem2(8778, 1);
				c.getItems().deleteItem2(8778, 1);
				c.sendMessage("You build a Chair.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(180 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13584, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122028://bookcase
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 29) {
				c.sendMessage("You need a level 29 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1539, 15) && c.getItems().playerHasItem(8778, 1) && c.getItems().playerHasItem(8778, 1) && c.getItems().playerHasItem(8778, 1)) {
				c.getItems().deleteItem2(1539, 15);
				c.getItems().deleteItem2(8778, 1);
				c.getItems().deleteItem2(8778, 1);
				c.getItems().deleteItem2(8778, 1);
				c.sendMessage("You build a Bookcase.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(180 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13598, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122031://greenman's ale
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 26) {
				c.sendMessage("You need a level 26 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1539, 15) && c.getItems().playerHasItem(8778, 1) && c.getItems().playerHasItem(8778, 1)) {
				c.getItems().deleteItem2(1539, 15);
				c.getItems().deleteItem2(8778, 1);
				c.getItems().deleteItem2(8778, 1);
				c.sendMessage("You build a Greenman's ale.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(184 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13571, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122034://small oven
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 24) {
				c.sendMessage("You need a level 24 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(2351, 1) && c.getItems().playerHasItem(2351, 1)) {
				c.getItems().deleteItem2(2351, 1);
				c.getItems().deleteItem2(2351, 1);
				c.sendMessage("You build a Small oven.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(80 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13533, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122037://carved oak bench
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 31) {
				c.sendMessage("You need a level 31 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1539, 15) && c.getItems().playerHasItem(8778, 1) && c.getItems().playerHasItem(8778, 1) && c.getItems().playerHasItem(8778, 1)) {
				c.getItems().deleteItem2(1539, 15);
				c.getItems().deleteItem2(8778, 1);
				c.getItems().deleteItem2(8778, 1);
				c.getItems().deleteItem2(8778, 1);
				c.sendMessage("You build a Carved oak bench.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(240 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13302, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122040://painting stand
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 41) {
				c.sendMessage("You need a level 41 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1539, 20) && c.getItems().playerHasItem(8778, 1) && c.getItems().playerHasItem(8778, 1)) {
				c.getItems().deleteItem2(1539, 20);
				c.getItems().deleteItem2(8778, 1);
				c.getItems().deleteItem2(8778, 1);
				c.sendMessage("You build a Painting stand.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(240 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13717, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122043://bed
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 40) {
				c.sendMessage("You need a level 40 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1539, 20) && c.getItems().playerHasItem(8778, 1) && c.getItems().playerHasItem(8778, 1) && c.getItems().playerHasItem(8778, 1)) {
				c.getItems().deleteItem2(1539, 20);
				c.getItems().deleteItem2(8778, 1);
				c.getItems().deleteItem2(8778, 1);
				c.getItems().deleteItem2(8778, 1);
				c.sendMessage("You build a Bed.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(300 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13151, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122046://teak drawers
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 51) {
				c.sendMessage("You need a level 51 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1539, 20) && c.getItems().playerHasItem(8780, 1) && c.getItems().playerHasItem(8780, 1)) {
				c.getItems().deleteItem2(1539, 20);
				c.getItems().deleteItem2(8780, 1);
				c.getItems().deleteItem2(8780, 1);
				c.sendMessage("You build a Teak drawers.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(180 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13158, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122049://mithril armour
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 28) {
				c.sendMessage("You need a level 28 Construction to do that.");
				return;
			}
			if(c.playerLevel[13] < 68) {
				c.sendMessage("You need a level 68 Smithing to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1159, 1) && c.getItems().playerHasItem(1121, 1) && c.getItems().playerHasItem(1071, 1)) {
				c.getItems().deleteItem2(1159, 1);
				c.getItems().deleteItem2(1121, 1);
				c.getItems().deleteItem2(1071, 1);
				c.sendMessage("You build a Mithril armour.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(135 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().addSkillXP(25 * Config.SMITHING_EXPERIENCE, 13);
				c.getPA().checkObjectSpawn(13491, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122052://adamant armour
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 28) {
				c.sendMessage("You need a level 28 Construction to do that.");
				return;
			}
			if(c.playerLevel[13] < 88) {
				c.sendMessage("You need a level 88 Smithing to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1161, 1) && c.getItems().playerHasItem(1123, 1) && c.getItems().playerHasItem(1073, 1)) {
				c.getItems().deleteItem2(1161, 1);
				c.getItems().deleteItem2(1123, 1);
				c.getItems().deleteItem2(1073, 1);
				c.sendMessage("You build a Adamant armour.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(150 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().addSkillXP(25 * Config.SMITHING_EXPERIENCE, 13);
				c.getPA().checkObjectSpawn(13492, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122055://rune armour
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 28) {
				c.sendMessage("You need a level 28 Construction to do that.");
				return;
			}
			if(c.playerLevel[13] < 99) {
				c.sendMessage("You need a level 99 Smithing to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1163, 1) && c.getItems().playerHasItem(1127, 1) && c.getItems().playerHasItem(1079, 1)) {
				c.getItems().deleteItem2(1163, 1);
				c.getItems().deleteItem2(1127, 1);
				c.getItems().deleteItem2(1079, 1);
				c.sendMessage("You build a Rune armour.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(165 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().addSkillXP(25 * Config.SMITHING_EXPERIENCE, 13);
				c.getPA().checkObjectSpawn(13493, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;


			case 122058://rune display case
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 28) {
				c.sendMessage("You need a level 28 Construction to do that.");
				return;
			}
			if(c.playerLevel[20] < 44) {
				c.sendMessage("You need a level 44 Runecrafting to do that.");
				return;
			}
			if (c.getItems().playerHasItem(563, 100) && c.getItems().playerHasItem(561, 100) && c.getItems().playerHasItem(8780, 1)) {
				c.getItems().deleteItem2(563, 100);
				c.getItems().deleteItem2(561, 1);
				c.getItems().deleteItem2(8780, 1);
				c.sendMessage("You build a Rune display case.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(212 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().addSkillXP(44 * Config.RUNECRAFTING_EXPERIENCE, 20);
				c.getPA().checkObjectSpawn(13508, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122061://archery target
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 81) {
				c.sendMessage("You need a level 81 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1539, 25) && c.getItems().playerHasItem(8780, 1) && c.getItems().playerHasItem(8780, 1) && c.getItems().playerHasItem(8780, 1)) {
				c.getItems().deleteItem2(1539, 25);
				c.getItems().deleteItem2(8780, 1);
				c.getItems().deleteItem2(8780, 1);
				c.getItems().deleteItem2(8780, 1);
				c.sendMessage("You build an Archery target.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(600 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13402, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122064://combat stone
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 59) {
				c.sendMessage("You need a level 59 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(2351, 1) && c.getItems().playerHasItem(2351, 1) && c.getItems().playerHasItem(2351, 1) && c.getItems().playerHasItem(2351, 1)) {
				c.getItems().deleteItem2(2351, 1);
				c.getItems().deleteItem2(2351, 1);
				c.getItems().deleteItem2(2351, 1);
				c.getItems().deleteItem2(2351, 1);
				c.sendMessage("You build a Combat stone.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(200 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(-1, c.absX, c.absY, c.heightLevel, 10);
				Server.npcHandler.spawnNpc(c, 4162, c.absX, c.absY, c.heightLevel, 0, 100, 5, 50, 50, false, true);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122067://elemental balance
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 77) {
				c.sendMessage("You need a level 77 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(2351, 1) && c.getItems().playerHasItem(2351, 1) && c.getItems().playerHasItem(2351, 1) && c.getItems().playerHasItem(2351, 1)) {
				c.getItems().deleteItem2(2351, 1);
				c.getItems().deleteItem2(2351, 1);
				c.getItems().deleteItem2(2351, 1);
				c.getItems().deleteItem2(2351, 1);
				c.sendMessage("You build an Elemental balance.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(356 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(-1, c.absX, c.absY, c.heightLevel, 10);
				Server.npcHandler.spawnNpc(c, 4095, c.absX, c.absY, c.heightLevel, 0, 100, 5, 50, 50, false, true);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122070://mahogany prize chest
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 54) {
				c.sendMessage("You need a level 54 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1539, 20) && c.getItems().playerHasItem(8782, 1) && c.getItems().playerHasItem(8782, 1)) {
				c.getItems().deleteItem2(1539, 20);
				c.getItems().deleteItem2(8782, 1);
				c.getItems().deleteItem2(8782, 1);
				c.sendMessage("You build a Mahogany prize chest.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(860 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13389, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122073://lectern
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 67) {
				c.sendMessage("You need a level 67 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1539, 40) && c.getItems().playerHasItem(8782, 1) && c.getItems().playerHasItem(8782, 1)) {
				c.getItems().deleteItem2(1539, 40);
				c.getItems().deleteItem2(8782, 1);
				c.getItems().deleteItem2(8782, 1);
				c.sendMessage("You build a Lectern.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(580 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13648, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122076://crystal of power
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 66) {
				c.sendMessage("You need a level 66 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1539, 15) && c.getItems().playerHasItem(8782, 1) && c.getItems().playerHasItem(8782, 1) && c.getItems().playerHasItem(2351, 1)) {
				c.getItems().deleteItem2(1539, 15);
				c.getItems().deleteItem2(8782, 1);
				c.getItems().deleteItem2(8782, 1);
				c.getItems().deleteItem2(2351, 1);
				c.sendMessage("You build a Crystal of power.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(890 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13661, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122079://altar
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 64) {
				c.sendMessage("You need a level 64 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1539, 15) && c.getItems().playerHasItem(8782, 1) && c.getItems().playerHasItem(8782, 1) && c.getItems().playerHasItem(2351, 1)) {
				c.getItems().deleteItem2(1539, 15);
				c.getItems().deleteItem2(8782, 1);
				c.getItems().deleteItem2(8782, 1);
				c.getItems().deleteItem2(2351, 1);
				c.sendMessage("You build an Altar.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(910 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13191, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122082://intense burners
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 61) {
				c.sendMessage("You need a level 61 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1539, 10) && c.getItems().playerHasItem(8782, 1) && c.getItems().playerHasItem(8782, 1) && c.getItems().playerHasItem(263, 1)) {
				c.getItems().deleteItem2(1539, 10);
				c.getItems().deleteItem2(8782, 1);
				c.getItems().deleteItem2(8782, 1);
				c.getItems().deleteItem2(263, 1);
				c.sendMessage("You build an Intense burners.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(280 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13210, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122085://hedge
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 80) {
				c.sendMessage("You need a level 80 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1511, 1) && c.getItems().playerHasItem(1511, 1) && c.getItems().playerHasItem(263, 1) && c.getItems().playerHasItem(263, 1)) {
				c.getItems().deleteItem2(1511, 1);
				c.getItems().deleteItem2(1511, 1);
				c.getItems().deleteItem2(263, 1);
				c.getItems().deleteItem2(263, 1);
				c.sendMessage("You build a Hedge.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(316 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13476, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122088://rocnar
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 83) {
				c.sendMessage("You need a level 83 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(2361, 1) && c.getItems().playerHasItem(2361, 1) && c.getItems().playerHasItem(263, 1) && c.getItems().playerHasItem(263, 1)) {
				c.getItems().deleteItem2(2361, 1);
				c.getItems().deleteItem2(2361, 1);
				c.getItems().deleteItem2(263, 1);
				c.getItems().deleteItem2(263, 1);
				c.sendMessage("You build a Rocnar.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(387 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(13373, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

			case 122091://bank chest
			if(!c.getItems().playerHasItem(2347, 1)) {
				c.sendMessage("You need a hammer to do that.");
				return;
			}
			if(c.playerLevel[21] < 92) {
				c.sendMessage("You need a level 92 Construction to do that.");
				return;
			}
			if (c.getItems().playerHasItem(1539, 40) && c.getItems().playerHasItem(8782, 1) && c.getItems().playerHasItem(8782, 1) && c.getItems().playerHasItem(2351, 1)) {
				c.getItems().deleteItem2(1539, 40);
				c.getItems().deleteItem2(8782, 1);
				c.getItems().deleteItem2(8782, 1);
				c.getItems().deleteItem2(2351, 1);
				c.sendMessage("You build a Bank chest.");
				c.getPA().closeAllWindows();
				c.getPA().addSkillXP(800 * Config.CONSTRUCTION_EXPERIENCE, 21);
				c.getPA().checkObjectSpawn(3193, c.absX, c.absY, c.heightLevel, 10);
				} else {
				c.sendMessage("You don't have the required materials.");
				}
				
			break;

		case 24017:
			c.getPA().resetAutocast();
			//c.sendFrame246(329, 200, c.playerEquipment[c.playerWeapon]);
			c.getItems().sendWeapon(c.playerEquipment[c.playerWeapon], ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]));
			//c.setSidebarInterface(0, 328);
			//c.setSidebarInterface(6, c.playerMagicBook == 0 ? 1151 : c.playerMagicBook == 1 ? 12855 : 1151);
			break;
		}
		if (c.isAutoButton(actionButtonId))
			c.assignAutocast(actionButtonId);
	}

}
