package server.game.players;

import server.Config;
import server.Server;
import server.content.skills.Fishing;
import server.content.skills.Implings;
import server.content.skills.Implings.Imps;
import server.content.skills.JewelryMaking;
import server.content.skills.Mining;
import server.content.skills.Runecrafting;
import server.content.skills.Smelting;
import server.content.skills.Tanning;
import server.content.skills.Woodcutting;
import server.game.minigames.barrows.Barrows;
import server.game.minigames.barrows.BarrowsData;
import server.game.minigames.bountyhunter.BountyHunter;
import server.game.minigames.castlewars.CastleWarObjects;
import server.game.minigames.castlewars.CastleWars;
import server.game.minigames.pestcontrol.PestControlRewards;
import server.game.minigames.roguesden.WallSafes;
import server.game.objects.Object;
import core.util.Misc;
import core.util.ScriptManager;

public class ActionHandler {
	
	private Client c;
	
	public ActionHandler(Client Client) {
		this.c = Client;
	}
	
	private boolean obj(int obX, int obY) {
        return c.objectX == obX && c.objectY == obY;
}
	
	
	public void firstClickObject(int objectType, int obX, int obY) {
		c.clickObjectType = 0;
		c.turnPlayerTo(obX, obY);
		if (Mining.miningRocks(c, objectType)) {
			Mining.attemptData(c, objectType, obX, obY);
			return;
		}

		if(objectType == 299 || objectType == 11745 || objectType == 356) {
			c.getTT().searchObject(obX, obY, objectType);
			return;
		} 

		//castlewars
		if(CastleWarObjects.handleObject(c, objectType, obX, obY))
			return;
		
		if(Barrows.barrowObjects(c, objectType)) {
			Barrows.objectAction(c, objectType);
			return;
		}

			/*ObjectDef def = ObjectDef.getObjectDef(objectType);
			if(def != null)
			if(def.name.equalsIgnoreCase("Ladder")) {
				if(def.actions[0].equals("Climb-up")) {
					if(obX == 3069 && obY == 10256) { // custom locations
						c.getPA().movePlayer(3017, 3850, 0);
						return;
					}
					if(obX == 3017 && obY == 10249) { // custom locations
						c.getPA().movePlayer(3069, 3857, 0);
						return;
					}
					if(c.getY() > 6400) {
						c.getPA().movePlayer(obX+1, obY+1-6400, c.heightLevel);
						return;
					} else {
						c.getPA().movePlayer(c.absX, c.absY, c.heightLevel+1);
						return;
					}
				}
				if(def.actions[0].equals("Climb-down")) {
					if(obX == 3017 && obY == 3849) { // custom locations
						c.getPA().movePlayer(3069, 10257, 0);
						return;
					}
					if(obX == 3069 && obY == 3856) { // custom locations
						c.getPA().movePlayer(3017, 10248, 0);
						return;
					}
					if(c.getY() < 6400 && (c.heightLevel & 3) == 0) {
						c.getPA().movePlayer(c.getX(), c.getY()+6400, c.heightLevel);
						return;
					} else {
						c.getPA().movePlayer(c.absX, c.absY, c.heightLevel-1);
						return;
					}
				}
				if(def.actions[0].equals("Climb")) {
					if(c.getY() > 6400) {
						c.getPA().movePlayer(obX+1, obY+1-6400, c.heightLevel);
						return;
					} else {
						c.getPA().movePlayer(c.absX, c.absY, c.heightLevel+1);
						return;
					}
				}
			}*/
		switch(objectType) {
		case 11214:
			if(c.absX != obX && c.absY != obY) {
				c.sendMessage("You need to stand on the platform in order to build.");
				return;
			}
			c.getPA().showInterface(31250);
			break;
		case 28119:
		case 28120:
		case 28121:
			BountyHunter.enterCrater(c, objectType);
			break;
		case 28122:
			BountyHunter.leaveCrater(c);
			break;
			
		case 7236: //wallsafes
			WallSafes.checkWallSafe(c);
			break;
		case 8143: //picking herbs
			c.getFarming().pickHerb();
			break;
		//castlewars
		case 4467:
		c.getPA().movePlayer(c.absX == 2834 ? 2385 : 2384, 3134, 0);
		break;
		
		case 4427:
		c.getPA().movePlayer(2373, c.absY == 3120 ? 3119 : 3120, 0);
		break;
		
		case 4428:
		c.getPA().movePlayer(2372, c.absY == 3120 ? 3119 : 3120, 0);
		break;
		
		case 4465:
		c.getPA().movePlayer( c.absX == 2414 ? 2415 : 2414, 3073, 0);
		break;
		
		case 4424:
		c.getPA().movePlayer(2427, c.absY == 3087 ? 3088 : 3087, 0);
		break;
		
		case 4423:
		c.getPA().movePlayer(2427, c.absY == 3087 ? 3088 : 3087, 0);
		break;
        case 4411:
        case 4415:
        case 4417:
        case 4418:
        case 4420:
        case 4469:
        case 4470:
        case 4419:
        case 4911:
        case 4912:
        case 1747:
        case 1757:
        case 4437:
        case 6281:
        case 6280:
        case 4472:
        case 4471:
        case 4406:
        case 4407:
        case 4458:
        case 4902:
        case 4903:
        case 4900:
        case 4901:
        case 4461:
        case 4463:
        case 4464:
        case 4377:
        case 4378:
            CastleWarObjects.handleObject(c, objectType, obX, obY);
        case 1568:
            if (obX == 3097 && obY == 3468)
                c.getPA().movePlayer(3097, 9868, 0);
            else
                CastleWarObjects.handleObject(c, obY, obY, obY);
            break;
            //end
            
            
            
		case 1276:
			Woodcutting.startWoodcutting(c, 0, c.objectX, c.objectY, c.clickObjectType);
			break;
		case 1278:
			Woodcutting.startWoodcutting(c, 1, c.objectX, c.objectY, c.clickObjectType);
			break;
		case 1286:
			Woodcutting.startWoodcutting(c, 2, c.objectX, c.objectY, c.clickObjectType);
			break;
		case 1281:
			Woodcutting.startWoodcutting(c, 3, c.objectX, c.objectY, c.clickObjectType);
			break;
		case 1308:
			Woodcutting.startWoodcutting(c, 4, c.objectX, c.objectY, c.clickObjectType);
			break;
		case 5552:
			Woodcutting.startWoodcutting(c, 5, c.objectX, c.objectY, c.clickObjectType);
			break;
		case 1307:
			Woodcutting.startWoodcutting(c, 6, c.objectX, c.objectY, c.clickObjectType);
			break;
		case 1309:
			Woodcutting.startWoodcutting(c, 7, c.objectX, c.objectY, c.clickObjectType);
			break;
		case 1306:
			Woodcutting.startWoodcutting(c, 8, c.objectX, c.objectY, c.clickObjectType);
			break;
		case 5551:
			Woodcutting.startWoodcutting(c, 9, c.objectX, c.objectY, c.clickObjectType);
			break;
		case 5553:
			Woodcutting.startWoodcutting(c, 10, c.objectX, c.objectY, c.clickObjectType);
			break;
		case 733:
			boolean canUseWeapon = false;
			for (int funWeapon : Config.WEBS_CANNOT) {
				if (c.playerEquipment[c.playerWeapon] == funWeapon) {
					canUseWeapon = true;
				}
			}
			if (canUseWeapon) {
				c.sendMessage("Only a sharp blade can cut through this sticky web.");
				return;
			}

			c.startAnimation(451);
			if (Misc.random(4) == 1) {
				c.getPA().removeObject(c.objectX, c.objectY);
				c.sendMessage("You slash the web apart.");
			} else {
				c.sendMessage("You fail to cut through it.");
				return;
			}

			if (c.objectX == 3158 && c.objectY == 3951) {
				new Object(734, c.objectX, c.objectY, c.getHeightLevel(), 1, 10,
						733, 50);
			} else {
				new Object(734, c.objectX, c.objectY, c.getHeightLevel(), 0, 10,
						733, 50);
			}
			break;
		case 5960:
			c.getPA().startTeleport2(3090, 3956, 0);
			break;
			case 5959:
				c.getPA().startTeleport2(2539, 4712, 0);
			break;
		case 1815:
			c.getPA().startTeleport2(2561, 3311, 0);
			break;
		case 11666:
		case 3044:
		case 2781:
			Smelting.openInterface(c);
			break;
		case 492:
			c.getPA().movePlayer(2857, 9569, 0);
			break;
		case 1764:
			c.getPA().movePlayer(2857, 3167, 0);
			break;
		case 9358:
			c.getPA().movePlayer(2480, 5175, 0);
			break;
		case 9359:
			c.getPA().movePlayer(2862, 9572, 0);
			break;
		case 12467:
		case 12468:
			c.getPA().movePlayer(2974, 9511, 0);
			break;
			
		case 2643:
			JewelryMaking.mouldInterface(c);
			break;
			
		case 8930:
			c.getPA().movePlayer(1975, 4409, 3);
		break;
		
		case 10177: // Dagganoth ladder 1st level
			c.getPA().movePlayer(1798, 4407, 3);
		break;	
		
		case 10193:
			c.getPA().movePlayer(2545, 10143, 0);
		break;
		
		case 10194:
			//c.getPA().movePlayer(2544, 3741, 0); watchtower TODO
		break;
		
		case 10195: 
			c.getPA().movePlayer(1809, 4405, 2);
		break;
		
		case 10196:
			c.getPA().movePlayer(1807, 4405, 3);
		break;
		
		case 10197:
			c.getPA().movePlayer(1823, 4404, 2);
		break;
		
		case 10198:
			c.getPA().movePlayer(1825, 4404, 3);
		break;
		
		case 10199:
			c.getPA().movePlayer(1834, 4388, 2);
		break;
		
		case 10200:
			c.getPA().movePlayer(1834, 4390, 3);
		break;
	
		case 10201:
			c.getPA().movePlayer(1811, 4394, 1);
		break;
		
		case 10202:
			c.getPA().movePlayer(1812, 4394, 2);
		break;
		
		case 10203:
			c.getPA().movePlayer(1799, 4386, 2);
		break;
		
		case 10204:
			c.getPA().movePlayer(1799, 4388, 1);
		break;
		
		case 10205:
			c.getPA().movePlayer(1796, 4382, 1);
		break;
		
		case 10206:
			c.getPA().movePlayer(1796, 4382, 2);
		break;
		
		case 10207:
			c.getPA().movePlayer(1800, 4369, 2);
		break;
		
		case 10208:
			c.getPA().movePlayer(1802, 4370, 1);
		break;
		
		case 10209:
			c.getPA().movePlayer(1827, 4362, 1);
		break;
		
		case 10210:
			c.getPA().movePlayer(1825, 4362, 2);
		break;
		
		case 10211:
			c.getPA().movePlayer(1863, 4373, 2);
		break;
		
		case 10212:
			c.getPA().movePlayer(1863, 4371, 1);
		break;
		
		case 10213:
			c.getPA().movePlayer(1864, 4389, 1);
		break;
		
		case 10214:
			c.getPA().movePlayer(1864, 4387, 2);
		break;
		
		case 10215:
			c.getPA().movePlayer(1890, 4407, 0);
		break;
		
		case 10216:
			c.getPA().movePlayer(1890, 4406, 1);
		break;
		
		case 10217:
			c.getPA().movePlayer(1957, 4373, 1);
		break;
		
		case 10218:
			c.getPA().movePlayer(1957, 4371, 0);
		break;
		
		case 10219:
			c.getPA().movePlayer(1824, 4379, 3);
		break;
		
		case 10220:
			c.getPA().movePlayer(1824, 4381, 2);
		break;
		
		case 10221:
			c.getPA().movePlayer(1838, 4375, 2);
		break;
		
		case 10222:
			c.getPA().movePlayer(1838, 4377, 3);
		break;
		
		case 10223:
			c.getPA().movePlayer(1850, 4386, 1);
		break;
		
		case 10224:
			c.getPA().movePlayer(1850, 4387, 2);
		break;
		
		case 10225:
			c.getPA().movePlayer(1932, 4378, 1);
		break;
		
		case 10226:
			c.getPA().movePlayer(1932, 4380, 2);
		break;
		
		case 10227:
			if (obj(1961, 4392))
				c.getPA().movePlayer(1961, 4392, 2);
			else 
				c.getPA().movePlayer(1932, 4377, 1);
		break;
		
		case 10228:
			c.getPA().movePlayer(1961, 4393, 3);
		break;
		
		case 10229:
			c.getPA().movePlayer(1912, 4367, 0);
		break;
		
		case 10230:
			c.getPA().movePlayer(2899, 4449, 0);
		break;
		case 26428:
			if(c.zamorakKills < 15) {
				c.sendMessage("You need 15 Zamorak kills to enter the boss' chamber.");
				return;
			}
			c.getPA().movePlayer(2925, 5331, 6);
			break;
		case 26427:
			if(c.saraKills < 15) {
				c.sendMessage("You need 15 Saradomin kills to enter the boss' chamber.");
				return;
			}
			c.getPA().movePlayer(2907, 5265, 4);
			break;
		case 26426:
			if(c.armaKills < 15) {
				c.sendMessage("You need 15 Armadyl kills to enter the boss' chamber.");
				return;
			}
			c.getPA().movePlayer(2839, 5296, 6);
			break;
		case 26384:
			if(c.absX == 2851 && c.absY == 5333) {
				c.getPA().movePlayer(2850, 5333, 2);
			} else if(c.absX == 2850 && c.absY == 5333) {
				c.getPA().movePlayer(2851, 5333, 2);
			}
			break;
		case 26425:
			if(c.bandosKills < 15) {
				c.sendMessage("You need 15 Bandos kills to enter the boss' chamber.");
				return;
			}
			c.getPA().movePlayer(2864, 5354, 6);
			break; 
		case 2823:
			c.getPA().movePlayer(2881, 5310, 2);
			break;
		case 12230:
			c.getPA().movePlayer(2506, 3038, 0);
			break;
		case 5099:
			if(c.playerLevel[16] < 34) {
				c.sendMessage("You need an Agility level of 34 to pass this.");
				return;
			}
			if(c.objectX == 2698 && c.objectY == 9498) {
				c.getPA().movePlayer(2698, 9492, 0);
			} else if(c.objectX == 2698 && c.objectY == 9493) {
				c.getPA().movePlayer(2698, 9499, 0);
			}
			break;
		case 5088:
			if(c.playerLevel[16] < 30) {
				c.sendMessage("You need an Agility level of 30 to pass this.");
				return;
			}
			c.getPA().movePlayer(2687, 9506, 0);
			break;
		case 5090:
			if(c.playerLevel[16] < 30) {
				c.sendMessage("You need an Agility level of 30 to pass this.");
				return;
			}
			c.getPA().movePlayer(2682, 9506, 0);
			break;
		case 5103:
		case 5105:
		case 5106:
		case 5107:
			if(c.getX() > obX) {
				c.getPA().walkTo(-2, 0);
			} else {
				c.getPA().walkTo(2, 0);
			}
			break;
		case 5104:
			if(c.getY() > obY) {
				c.getPA().walkTo(0, -2);
			} else {
				c.getPA().walkTo(0, 2);
			}
			break;
		case 5110:
			if(c.playerLevel[16] < 12) {
				c.sendMessage("You need an Agility level of 12 to pass this.");
				return;
			}
			c.getPA().movePlayer(2647, 9557, 0);
			break;
		case 5111:
			if(c.playerLevel[16] < 12) {
				c.sendMessage("You need an Agility level of 12 to pass this.");
				return;
			}
			c.getPA().movePlayer(2649, 9562, 0);
			break;
		case 2320:
			if(obX == 3120 && obY == 9964) {
				c.getPA().movePlayer(3120, 9970, 0);
			} else if(obX == 3120 && obY == 9969) {
				c.getPA().movePlayer(3120, 9963, 0);
			}
			break;
		case 2111:
			Mining.attemptDataGem(c, objectType, obX, obY);
			break;
			
		case 69:
		case 2178:
			//if (c.objectX == 2675 && c.objectY == 3170) {
				//c.getDH().sendDialogues(79, 0);
			//} else {
				if (c.playerLevel[Player.playerFishing] <= 50) {
					c.sendMessage("You need a fishing level of 50 or higher to play Fishing Trawler.");
					return;
				}
				Server.trawler.getWaitingRoom().join(c);
			//}
			break;

		case 2179:
		case 70:
			Server.trawler.getWaitingRoom().leave(c);
			break;
		case 2167:
			Server.trawler.fixHole(c, obX, obY);
			break;
		case 2166:
			Server.trawler.showReward(c);
			break;
		case 2159:
		case 2160:
			c.trawlerFade(2676, 3170, 0);
			break;
		case 2175:
			Server.trawler.downLadder(c, obX, obY);
			break;
		case 2174:
			Server.trawler.upLadder(c, obX, obY);
			break;
			
		case 2213:
		case 26972:
		case 14367:
			c.getDH().sendDialogues(1000, 494);
			//c.getPA().openUpBank();
			break;
			
		case 3193:
		case 4483:
			c.getPA().openUpBank();
			break;
				
		/*Abyss - By Beanerrr*/
		case 7129: //Fire Riff
			if ((c.getItems().playerHasItem(1442,1)) || (c.getItems().playerHasItem(5537)) || (c.playerEquipment[c.playerHat] == 5537)) {
				c.getPA().spellTeleport(2583, 4838, 0);
				c.getPA().addSkillXP(15 * Config.RUNECRAFTING_EXPERIENCE, Player.playerRunecrafting);
			} else {
				c.sendMessage("You need either a Talisman or a Tiara to get past this.");
			}
		break;
		case 7130: //Earth Riff
			if ((c.getItems().playerHasItem(1440,1)) || (c.getItems().playerHasItem(5535)) || (c.playerEquipment[c.playerHat] == 5535)) {
				c.getPA().spellTeleport(2660, 4839, 0);
				c.getPA().addSkillXP(15 * Config.RUNECRAFTING_EXPERIENCE, Player.playerRunecrafting);
			} else {
				c.sendMessage("You need either a Talisman or a Tiara to get past this.");
			}
		break;
		case 7131: //Body Riff
			if ((c.getItems().playerHasItem(1446,1)) || (c.getItems().playerHasItem(5533)) || (c.playerEquipment[c.playerHat] == 5533)) {
				c.getPA().spellTeleport(2527, 4833, 0);
				c.getPA().addSkillXP(15 * Config.RUNECRAFTING_EXPERIENCE, Player.playerRunecrafting);
				
			} else {
				c.sendMessage("You need either a Talisman or a Tiara to get past this.");
			}
		break;
		case 7132: //Cosmic Riff
			if ((c.getItems().playerHasItem(1454,1)) || (c.getItems().playerHasItem(5539)) || (c.playerEquipment[c.playerHat] == 5539)) {
				c.getPA().spellTeleport(2162, 4833, 0);
				
				c.getPA().addSkillXP(15 * Config.RUNECRAFTING_EXPERIENCE, Player.playerRunecrafting);
				
			} else {
				c.sendMessage("You need either a Talisman or a Tiara to get past this.");
			}
		break;
		case 7133: //Nature Riff
			if ((c.getItems().playerHasItem(1462,1)) || (c.getItems().playerHasItem(5541)) || (c.playerEquipment[c.playerHat] == 5541)) {
				c.getPA().spellTeleport(2398, 4841, 0);
				
				c.getPA().addSkillXP(15 * Config.RUNECRAFTING_EXPERIENCE, Player.playerRunecrafting);
				
			} else {
				c.sendMessage("You need either a Talisman or a Tiara to get past this.");
			}
		break;
		case 7134: //Chaos Riff
			if ((c.getItems().playerHasItem(1452,1)) || (c.getItems().playerHasItem(5543)) || (c.playerEquipment[c.playerHat] == 5543)) {
				c.getPA().spellTeleport(2269, 4843, 0);
				
				c.getPA().addSkillXP(15 * Config.RUNECRAFTING_EXPERIENCE, Player.playerRunecrafting);
				
			} else {
				c.sendMessage("You need either a Talisman or a Tiara to get past this.");
			}
		break;
		case 7135: //Law Riff
			if ((c.getItems().playerHasItem(1458,1)) || (c.getItems().playerHasItem(5545)) || (c.playerEquipment[c.playerHat] == 5545)) {
				c.getPA().spellTeleport(2464, 4834, 0);
				
				c.getPA().addSkillXP(15 * Config.RUNECRAFTING_EXPERIENCE, Player.playerRunecrafting);
				
			} else {
				c.sendMessage("You need either a Talisman or a Tiara to get past this.");
			}
		break;
		case 7136: //Death Riff
			if ((c.getItems().playerHasItem(1456,1)) || (c.getItems().playerHasItem(5547)) || (c.playerEquipment[c.playerHat] == 5547)) {
				c.getPA().spellTeleport(2207, 4836, 0);
				
				c.getPA().addSkillXP(15 * Config.RUNECRAFTING_EXPERIENCE, Player.playerRunecrafting);
				
			} else {
				c.sendMessage("You need either a Talisman or a Tiara to get past this.");
			}
		break;
		case 7137: //Water Riff
			if ((c.getItems().playerHasItem(1444,1)) || (c.getItems().playerHasItem(5531)) || (c.playerEquipment[c.playerHat] == 5531)) {
				c.getPA().spellTeleport(2713, 4836, 0);
				
				c.getPA().addSkillXP(15 * Config.RUNECRAFTING_EXPERIENCE, Player.playerRunecrafting);
				
			} else {
				c.sendMessage("You need either a Talisman or a Tiara to get past this.");
			}
		break;
		case 7138: //Soul Riff
			if ((c.getItems().playerHasItem(1460,1)) || (c.getItems().playerHasItem(5551)) || (c.playerEquipment[c.playerHat] == 5551)) {
				
				Runecrafting.craftRunes(c, 30625);
				
			} else {
				c.sendMessage("You need either a Talisman or a Tiara to get past this.");
			}
		break;
		case 7139: //Air Riff
			if ((c.getItems().playerHasItem(1438,1)) || (c.getItems().playerHasItem(5527)) || (c.playerEquipment[c.playerHat] == 5527)) {
				c.getPA().spellTeleport(2845, 4832, 0);
				
				c.getPA().addSkillXP(15 * Config.RUNECRAFTING_EXPERIENCE, Player.playerRunecrafting);
				
			} else {
				c.sendMessage("You need either a Talisman or a Tiara to get past this.");
			}
		break;
		case 7140: //Mind Riff
			if ((c.getItems().playerHasItem(1448,1)) || (c.getItems().playerHasItem(5529)) || (c.playerEquipment[c.playerHat] == 5529)) {
				c.getPA().spellTeleport(2788, 4841, 0);
				
				c.getPA().addSkillXP(15 * Config.RUNECRAFTING_EXPERIENCE, Player.playerRunecrafting);
				
			} else {
				c.sendMessage("You need either a Talisman or a Tiara to get past this.");
			}
		break;
		case 7141: //Blood Riff
			if ((c.getItems().playerHasItem(1450,1)) || (c.getItems().playerHasItem(5549)) || (c.playerEquipment[c.playerHat] == 5549)) {
				
				Runecrafting.craftRunes(c, 30624);
				
			} else {
				c.sendMessage("You need either a Talisman or a Tiara to get past this.");
			}
		break;
		/*End of Abbyss - By Beanerrr*/
		
		/*
		 * Agility
		 */
		 		case 2492:
			if (c.killCount >= 20) {
				c.getDH().sendOption4("Armadyl", "Bandos", "Saradomin", "Zamorak");
				c.dialogueAction = 20;
			} else {
				c.sendMessage("You need 20 kill count before teleporting to a boss chamber.");
			}
		break;
		case 2288:
			break;
		case 2309:
			if (c.getX() == 2998 && c.getY() == 3916) {
				c.getAgil().doWildernessEntrance(c);
			}
			break;
		case 2295:
			if (c.getX() == 2474 && c.getY() == 3436) {
				c.getAgil().doGnomeLog(c);
			}
			break;
		case 2285: //NET1
			c.getAgil().doGnomeNet1(c);
				break;
		case 2313: //BRANCH1
			c.getAgil().doGnomeBranch1(c);
				break;
		case 2312: //ROPE
			if (c.getX() == 2477 && c.getY() == 3420) {
				c.getAgil().doGnomeRope(c);
			}
				break;
			case 2314: //BRANCH2
			c.getAgil().doGnomeBranch2(c);
				break;
			case 2286: //NET2
			c.getAgil().doGnomeNet2(c);
				break;
			case 154: //PIPE1
				if (c.getX() ==  2484 && c.getY() == 3430) {
					c.getAgil().doGnomePipe1(c);
				}
				break;
			case 4058: //PIPE2
				if (c.getX() == 2487 && c.getY() == 3430) {
					c.getAgil().doGnomePipe2(c);
				}
				break;
		/*
		 * END OF AGILITY
		 * 
		 * */
		case 9294:
			if (c.absX == 2880 && c.absY == 9813) {
				c.getPA().movePlayer(2878, 9813, 0);
			} else if (c.absX == 2878 && c.absY == 9813) {
				c.getPA().movePlayer(2880, 9813, 0);
			}
			break;
		case 9293:
			if (c.objectX == 2887 && c.objectY == 9799) {
				c.getPA().movePlayer(2892, 9799, 0);
			}
			if (c.objectX == 2890 && c.objectY == 9799) {
				c.getPA().movePlayer(2886, 9799, 0);
			}
			break;
		case 26933:
			if (c.objectX == 3097 && c.objectY == 3468) {
				c.getPA().movePlayer(3117, 9852, 0);
			}
			break;
		case 1755:
			if (c.objectX == 2884 && c.objectY == 9797) {
				c.getPA().movePlayer(2844, 3516, 0);
			}
			if (c.objectX == 3116 && c.objectY == 9852) {
				c.getPA().movePlayer(3096, 3468, 0);
			}
			if ((c.objectX == 3020 && c.objectY == 9739)
					|| (c.objectX == 3019 && c.objectY == 9740)
					 || (c.objectX == 3018 && c.objectY == 9739)
					 || (c.objectX == 3029 && c.objectY == 9738)) {
				c.getPA().movePlayer(3017, 3339, 0);
			}
			if ((c.objectX == 3018 && c.objectY == 3339)
					|| (c.objectX == 3019 && c.objectY == 3340)
					 || (c.objectX == 3020 && c.objectY == 3339)
					 || (c.objectX == 3019 && c.objectY == 3338)) {
				c.getPA().movePlayer(3021, 9739, 0);
			}
			break;
		case 2:
			c.getPA().movePlayer(3029, 9582, 0);
			break;
			/* Shops */
		case 6839:
			c.getShops().openShop(9);
			c.sendMessage("As you look down in the chest, you see a small monkey with a money-pouch");
			c.sendMessage("next to him. I think there is where I should put the coins.");
			break;
		/*case 2492:
			if (c.objectX == 2889 && c.objectY == 4813) {
				c.getPA().startTeleport(3252, 3401, 0, "modern");
			}
			break;*/
		case 9356:
			c.getPA().enterCaves();
			c.sendMessage("Good luck!");
		break;

                case 2557:
                     if(c.getItems().playerHasItem(1523, 1) && c.absX == 3190 && c.absY == 3957) {
                        c.getPA().movePlayer(3190, 3958, 0);
                     } else if(c.getItems().playerHasItem(1523, 1) && c.absX == 3190 && c.absY == 3958) {
                        c.getPA().movePlayer(3190, 3957, 0);
                     }
                break;

                case 2995:
                       c.getPA().startTeleport2(2717, 9801, 0);
                       c.sendMessage("Welcome to the dragon lair, be aware. It's very dangerous.");
                break;

		case 1816:
			c.getPA().startTeleport2(2271, 4680, 0);			
		break;
		case 1817:
			c.getPA().startTeleport(3067, 10253, 0, "modern");
		break;
		case 1814:
			//ardy lever
			c.getPA().startTeleport(3153, 3923, 0, "modern");
		break;

		case 2882:
		case 2883:
			if (c.objectX == 3268) {
				if (c.absX < c.objectX) {
					c.getPA().walkTo(1,0);
				} else {
					c.getPA().walkTo(-1,0);
				}
			}
		break;

		case 1765:
			c.getPA().movePlayer(3067, 10256, 0);
		break;
		case 272:
		c.getPA().movePlayer(c.absX, c.absY, 1);
		break;
		
		case 273:
		c.getPA().movePlayer(c.absX, c.absY, 0);
		break;

		case 245:
		c.getPA().movePlayer(c.absX, c.absY + 2, 2);
		break;

		case 246:
		c.getPA().movePlayer(c.absX, c.absY - 2, 1);
		break;

		case 1766:
		c.getPA().movePlayer(3016, 3849, 0);
		break;

		case 6552:
		if (c.playerMagicBook == 0) {
                        c.playerMagicBook = 1;
                        c.setSidebarInterface(6, 12855);
                        c.autocasting = false;
                        c.sendMessage("An ancient wisdomin fills your mind.");
                        c.getPA().resetAutocast();
                        c.getPA().applyRememberedAutocast();
		} else {
			c.setSidebarInterface(6, 1151); //modern
			c.playerMagicBook = 0;
                        c.autocasting = false;
			c.sendMessage("You feel a drain on your memory.");
			c.autocastId = -1;
			c.getPA().resetAutocast();
			c.getPA().applyRememberedAutocast();
		}	
		break;
		case 410:
			if (c.playerMagicBook == 0 || c.playerMagicBook == 1) {
	                        c.playerMagicBook = 2;
	                        c.setSidebarInterface(6, 29999);
	                        c.autocasting = false;
	                        c.sendMessage("Lunar Spells have been activated!");
	                        c.getPA().resetAutocast();
	                        c.getPA().applyRememberedAutocast();
			} else {
				c.setSidebarInterface(6, 1151); //modern
				c.playerMagicBook = 0;
	                        c.autocasting = false;
				c.sendMessage("You feel a drain on your memory.");
				c.autocastId = -1;
				c.getPA().resetAutocast();
				c.getPA().applyRememberedAutocast();
			}	
			break;
		

		case 1733:
			c.getPA().movePlayer(c.absX, c.absY + 6393, 0);
		break;
		
		case 1734:
			c.getPA().movePlayer(c.absX, c.absY - 6396, 0);
		break;
		
		case 9357:
			c.getPA().resetTzhaar();
		break;
		
		case 8959:
			if (c.getX() == 2490 && (c.getY() == 10146 || c.getY() == 10148)) {
				if (c.getPA().checkForPlayer(2490, c.getY() == 10146 ? 10148 : 10146)) {
					new Object(6951, c.objectX, c.objectY, c.heightLevel, 1, 10, 8959, 15);	
				}			
			}
		break;

		case 2623:
			if (c.absX >= c.objectX)
				c.getPA().walkTo(-1,0);
			else
				c.getPA().walkTo(-1,0);
		break;
		//pc boat
		case 14315:
			c.getPA().movePlayer(2661,2639,0);
		break;
		case 14314:
			c.getPA().movePlayer(2657,2639,0);
		break;
		
		case 1596:
		case 1597:
		if (c.getY() > c.objectY)
			c.getPA().walkTo(0, -1);
		else
			c.getPA().walkTo(0, 1);
		break;
		
		case 1557:
		case 1558:
			if((c.objectX == 3106 || c.objectX == 3105) && c.objectY == 9944) {
				if (c.getY() > c.objectY)
					c.getPA().walkTo(0, -1);
				else
					c.getPA().walkTo(0, 1);
			} else {
				if (c.getX() > c.objectX)
					c.getPA().walkTo(-1, 0);
				else
					c.getPA().walkTo(1, 0);
			}
		break;
		
		case 14235:
		case 14233:
			if (c.objectX == 2670)
				if (c.absX <= 2670)
					c.absX = 2671;
				else
					c.absX = 2670;
			if (c.objectX == 2643)
				if (c.absX >= 2643)
					c.absX = 2642;
				else
					c.absX = 2643;
			if (c.absX <= 2585)
				c.absY += 1;
			else c.absY -= 1;
			c.getPA().movePlayer(c.absX, c.absY, 0);
		break;
		
		case 14829: case 14830: case 14827: case 14828: case 14826: case 14831:
			//Server.objectHandler.startObelisk(objectType);
			Server.objectManager.startObelisk(objectType);
		break;
		
		case 9369:
			if (c.getY() > 5175)
				c.getPA().movePlayer(2399, 5175, 0);
			else
				c.getPA().movePlayer(2399, 5177, 0);
		break;
		
		case 10284:
			if(c.barrowsKill < 5) {
				c.sendMessage("You must kill all the brothers to receive a reward!");
				return;
			}
			if(c.barrowsKill == 5) {
				Barrows.spawnLastBrother(c);
			}
			if(c.barrowsKill > 5) {
				Barrows.refreshBrothers(c);
				BarrowsData.addLoot(c);
			}
			break;
		
		/*//barrows
		//Chest
		case 10284:
			//c.shakeScreen(3, 2, 3, 2);
			if(c.barrowsKillCount < 5) {
				c.sendMessage("You must kill all the brothers to receive a reward!");
			}
			if(c.barrowsKillCount == 5 && c.barrowsNpcs[c.randomCoffin][1] == 1) {
				c.sendMessage("I have already awakened this brother.");
			}
			if(c.barrowsNpcs[c.randomCoffin][1] == 0 && c.barrowsKillCount >= 5) {
				Server.npcHandler.spawnNpc(c, c.barrowsNpcs[c.randomCoffin][0], 3551, 9694-1, 0, 0, 120, 30, 200, 200, true, true);
				c.barrowsNpcs[c.randomCoffin][1] = 1;
			}
			if((c.barrowsKillCount > 5 || c.barrowsNpcs[c.randomCoffin][1] == 2) && c.getItems().freeSlots() >= 2) {
				c.resetShaking();
				c.getPA().resetBarrows();
				c.getItems().addItem(c.getPA().randomRunes(), Misc.random(150) + 100);
				if (Misc.random(2) == 1)
					c.getItems().addItem(c.getPA().randomBarrows(), 1);
				c.getPA().startTeleport(3564, 3288, 0, "modern");
			} else if(c.barrowsKillCount > 5 && c.getItems().freeSlots() <= 1) {
				c.sendMessage("You need two empty slots in your inventory to receive the reward.");
			}
			break;
		//doors
		case 6749:
			if(obX == 3562 && obY == 9678) {
				c.getPA().object(3562, 9678, 6749, -3, 0);
				c.getPA().object(3562, 9677, 6730, -1, 0);
			} else if(obX == 3558 && obY == 9677) {
				c.getPA().object(3558, 9677, 6749, -1, 0);
				c.getPA().object(3558, 9678, 6730, -3, 0);
			}
			break;
		case 6730:
			if(obX == 3558 && obY == 9677) {
				c.getPA().object(3562, 9678, 6749, -3, 0);
				c.getPA().object(3562, 9677, 6730, -1, 0);
			} else if(obX == 3558 && obY == 9678) {
				c.getPA().object(3558, 9677, 6749, -1, 0);
				c.getPA().object(3558, 9678, 6730, -3, 0);
			}
			break;
		case 6727:
			if(obX == 3551 && obY == 9684) {
				c.sendMessage("You can't open this door...");
			}
			break;
		case 6746:
			if(obX == 3552 && obY == 9684) {
				c.sendMessage("You can't open this door...");
			}
			break;
		case 6748:
			if(obX == 3545 && obY == 9678) {
				c.getPA().object(3545, 9678, 6748, -3, 0);
				c.getPA().object(3545, 9677, 6729, -1, 0);
			} else if(obX == 3541 && obY == 9677) {
				c.getPA().object(3541, 9677, 6748, -1, 0);
				c.getPA().object(3541, 9678, 6729, -3, 0);
			}
			break;
		case 6729:
			if(obX == 3545 && obY == 9677){
				c.getPA().object(3545, 9678, 6748, -3, 0);
				c.getPA().object(3545, 9677, 6729, -1, 0);
			} else if(obX == 3541 && obY == 9678) {
				c.getPA().object(3541, 9677, 6748, -1, 0);
				c.getPA().object(3541, 9678, 6729, -3, 0);
			}
			break;
		case 6726:
			if(obX == 3534 && obY == 9684) {
				c.getPA().object(3534, 9684, 6726, -4, 0);
				c.getPA().object(3535, 9684, 6745, -2, 0);
			} else if(obX == 3535 && obY == 9688) {
				c.getPA().object(3535, 9688, 6726, -2, 0);
				c.getPA().object(3534, 9688, 6745, -4, 0);
			}
			break;
		case 6745:
			if(obX == 3535 && obY == 9684) {
				c.getPA().object(3534, 9684, 6726, -4, 0);
				c.getPA().object(3535, 9684, 6745, -2, 0);
			} else if(obX == 3534 && obY == 9688) {
				c.getPA().object(3535, 9688, 6726, -2, 0);
				c.getPA().object(3534, 9688, 6745, -4, 0);
			}
			break;
		case 6743:
			if(obX == 3545 && obY == 9695) {
				c.getPA().object(3545, 9694, 6724, -1, 0);
				c.getPA().object(3545, 9695, 6743, -3, 0);
			} else if(obX == 3541 && obY == 9694) {
				c.getPA().object(3541, 9694, 6724, -1, 0);
				c.getPA().object(3541, 9695, 6743, -3, 0);
			}
			break;
		case 6724:
			if(obX == 3545 && obY == 9694) {
				c.getPA().object(3545, 9694, 6724, -1, 0);
				c.getPA().object(3545, 9695, 6743, -3, 0);
			} else if(obX == 3541 && obY == 9695) {
				c.getPA().object(3541, 9694, 6724, -1, 0);
				c.getPA().object(3541, 9695, 6743, -3, 0);
			}
			break; 

		case 6707: // verac
			c.getPA().movePlayer(3556, 3298, 0);
			break;
			
		case 6823:
			if(server.game.minigames.barrows.Barrows.selectCoffin(c, objectType)) {
				return;
			}
			if(c.barrowsNpcs[0][1] == 0) {
				Server.npcHandler.spawnNpc(c, 2030, c.getX(), c.getY()-1, -1, 0, 120, 25, 200, 200, true, true);
				c.barrowsNpcs[0][1] = 1;
			} else {
				c.sendMessage("You have already searched in this sarcophagus.");
			}
			break;

		case 6706: // torag 
			c.getPA().movePlayer(3553, 3283, 0);
			break;
			
		case 6772:
			if(server.game.minigames.barrows.Barrows.selectCoffin(c, objectType)) {
				return;
			}
			if(c.barrowsNpcs[1][1] == 0) {
				Server.npcHandler.spawnNpc(c, 2029, c.getX()+1, c.getY(), -1, 0, 120, 20, 200, 200, true, true);
				c.barrowsNpcs[1][1] = 1;
			} else {
				c.sendMessage("You have already searched in this sarcophagus.");
			}
			break;
			
			
		case 6705: // karil stairs
			c.getPA().movePlayer(3565, 3276, 0);
			break;
		case 6822:
			if(server.game.minigames.barrows.Barrows.selectCoffin(c, objectType)) {
				return;
			}
			if(c.barrowsNpcs[2][1] == 0) {
				Server.npcHandler.spawnNpc(c, 2028, c.getX(), c.getY()-1, -1, 0, 90, 17, 200, 200, true, true);
				c.barrowsNpcs[2][1] = 1;
			} else {
				c.sendMessage("You have already searched in this sarcophagus.");
			}
			break;
			
		case 6704: // guthan stairs
			c.getPA().movePlayer(3578, 3284, 0);
			break;
		case 6773:
			if(server.game.minigames.barrows.Barrows.selectCoffin(c, objectType)) {
				return;
			}
			if(c.barrowsNpcs[3][1] == 0) {
				Server.npcHandler.spawnNpc(c, 2027, c.getX(), c.getY()-1, -1, 0, 120, 23, 200, 200, true, true);
				c.barrowsNpcs[3][1] = 1;
			} else {
				c.sendMessage("You have already searched in this sarcophagus.");
			}
			break;
			
		case 6703: // dharok stairs
			c.getPA().movePlayer(3574, 3298, 0);
			break;
		case 6771:
			if(server.game.minigames.barrows.Barrows.selectCoffin(c, objectType)) {
				return;
			}
			if(c.barrowsNpcs[4][1] == 0) {
				Server.npcHandler.spawnNpc(c, 2026, c.getX(), c.getY()-1, -1, 0, 120, 45, 250, 250, true, true);
				c.barrowsNpcs[4][1] = 1;
			} else {
				c.sendMessage("You have already searched in this sarcophagus.");
			}
			break;
			
		case 6702: // ahrim stairs
			c.getPA().movePlayer(3565, 3290, 0);
			break;
		case 6821:
			if(server.game.minigames.barrows.Barrows.selectCoffin(c, objectType)) {
				return;
			}
			if(c.barrowsNpcs[5][1] == 0) {
				Server.npcHandler.spawnNpc(c, 2025, c.getX(), c.getY()-1, -1, 0, 90, 19, 200, 200, true, true);
				c.barrowsNpcs[5][1] = 1;
			} else {
				c.sendMessage("You have already searched in this sarcophagus.");
			}
			break;*/
			
		
		
		/*case 8143:
			if (c.farm[0] > 0 && c.farm[1] > 0) {
				Farming.pickHerb(c);
			}
		break;*/
	
			// DOORS
		case 1516:
		case 1519:
			if (c.objectY == 9698) {
				if (c.absY >= c.objectY)
					c.getPA().walkTo(0,-1);
				else
					c.getPA().walkTo(0,1);
				break;
			}
		case 1530:
		case 1531:
		case 1533:
		case 1534:
		case 11712:
		case 11711:
		case 11707:
		case 11708:
		case 6725:
		case 3198:
		case 3197:
			Server.objectHandler.doorHandling(objectType, c.objectX, c.objectY, 0);	
			break;
		
		case 9319:
			if (c.heightLevel == 0)
				c.getPA().movePlayer(c.absX, c.absY, 1);
			else if (c.heightLevel == 1)
				c.getPA().movePlayer(c.absX, c.absY, 2);
		break;
		
		case 9320:
			if (c.heightLevel == 1)
				c.getPA().movePlayer(c.absX, c.absY, 0);
			else if (c.heightLevel == 2)
				c.getPA().movePlayer(c.absX, c.absY, 1);
		break;
		
		case 4496:
		case 4494:
			if (c.heightLevel == 2) {
				c.getPA().movePlayer(c.absX - 5, c.absY, 1);
			} else if (c.heightLevel == 1) {
				c.getPA().movePlayer(c.absX + 5, c.absY, 0);
			}
		break;
		
		case 4493:
			if (c.heightLevel == 0) {
				c.getPA().movePlayer(c.absX - 5, c.absY, 1);
			} else if (c.heightLevel == 1) {
				c.getPA().movePlayer(c.absX + 5, c.absY, 2);
			}
		break;
		
		case 4495:
			if (c.heightLevel == 1) {
				c.getPA().movePlayer(c.absX + 5, c.absY, 2);
			}
		break;
		
		case 5126:
			if (c.absY == 3554)
				c.getPA().walkTo(0,1);
			else
				c.getPA().walkTo(0,-1);
		break;
		
		case 1759:
			if (c.objectX == 2884 && c.objectY == 3397) {
				c.getPA().movePlayer(c.absX, c.absY + 6400, 0);
			} else if (c.objectX == 2845 && c.objectY == 3516) {
				c.getPA().movePlayer(2884, 9798, 0);
			} else if (c.objectX == 2848 && c.objectY == 3513) {
				c.getPA().movePlayer(2884, 9798, 0);
			} else if (c.objectX == 2848 && c.objectY == 3519) {
				c.getPA().movePlayer(2884, 9798, 0);
			}
		break;
		/*case 1558:
			if (c.absX == 3041 && c.absY == 10308) {
                            c.getPA().movePlayer(3040, 10308, 0);	
                        } else if(c.absX == 3040 && c.absY == 10308) {
                                  c.getPA().movePlayer(3041, 10308, 0);
                        } else if(c.absX == 3040 && c.absY == 10307) {
                                  c.getPA().movePlayer(3041, 10307, 0);
                        } else if(c.absX == 3041 && c.absY == 10307) {
                                  c.getPA().movePlayer(3040, 10307, 0);
                        } else if(c.absX == 3044 && c.absY == 10341) {
                                  c.getPA().movePlayer(3045, 10341, 0);
                        } else if(c.absX == 3045 && c.absY == 10341) {
                                  c.getPA().movePlayer(3044, 10341, 0);
                        } else if(c.absX == 3044 && c.absY == 10342) {
                                  c.getPA().movePlayer(3045, 10342, 0);
                        } else if(c.absX == 3045 && c.absY == 10342) {
                                  c.getPA().movePlayer(3044, 10343, 0);
                        }
		break;
		case 1557:
			if (c.absX == 3023 && c.absY == 10312) {
                            c.getPA().movePlayer(3022, 10312, 0);	
                        } else if(c.absX == 3022 && c.absY == 10312) {
                                  c.getPA().movePlayer(3023, 10312, 0);
                        } else if(c.absX == 3023 && c.absY == 10311) {
                                  c.getPA().movePlayer(3022, 10311, 0);
                        } else if(c.absX == 3022 && c.absY == 10311) {
                                  c.getPA().movePlayer(3023, 10311, 0);
                        }
		break;*/
 		case 3203: //dueling forfeit
			if (c.duelCount > 0) {
				c.sendMessage("You may not forfeit yet.");
				break;
			}
			Client o = (Client) PlayerHandler.players[c.duelingWith];				
			if(o == null) {
				c.getTradeAndDuel().resetDuel();
				c.getPA().movePlayer(Config.DUELING_RESPAWN_X+(Misc.random(Config.RANDOM_DUELING_RESPAWN)), Config.DUELING_RESPAWN_Y+(Misc.random(Config.RANDOM_DUELING_RESPAWN)), 0);
				break;
			}
			if(c.duelRule[0]) {
				c.sendMessage("Forfeiting the duel has been disabled!");
				break;
			}
			{
				o.getPA().movePlayer(Config.DUELING_RESPAWN_X+(Misc.random(Config.RANDOM_DUELING_RESPAWN)), Config.DUELING_RESPAWN_Y+(Misc.random(Config.RANDOM_DUELING_RESPAWN)), 0);
				c.getPA().movePlayer(Config.DUELING_RESPAWN_X+(Misc.random(Config.RANDOM_DUELING_RESPAWN)), Config.DUELING_RESPAWN_Y+(Misc.random(Config.RANDOM_DUELING_RESPAWN)), 0);
				o.duelStatus = 6;
				o.getTradeAndDuel().duelVictory();
				c.getTradeAndDuel().resetDuel();
				c.getTradeAndDuel().resetDuelItems();
				o.sendMessage("The other player has forfeited the duel!");
				c.sendMessage("You forfeit the duel!");
				break;
			}
			
		case 409:
			if(c.playerLevel[5] < c.getPA().getLevelForXP(c.playerXP[5])) {
				c.startAnimation(645);
				c.playerLevel[5] = c.getPA().getLevelForXP(c.playerXP[5]);
				c.sendMessage("You recharge your prayer points.");
				c.getPA().refreshSkill(5);
			} else {
				c.sendMessage("You already have full prayer points.");
			}
			break;
			
		case 2873:
			if (!c.getItems().ownsCape()) {
				c.startAnimation(645);
				c.sendMessage("Saradomin blesses you with a cape.");
				c.getItems().addItem(2412, 1);
			}	
		break;
		case 2875:
			if (!c.getItems().ownsCape()) {
				c.startAnimation(645);
				c.sendMessage("Guthix blesses you with a cape.");
				c.getItems().addItem(2413, 1);
			}
		break;
		case 2874:
			if (!c.getItems().ownsCape()) {
				c.startAnimation(645);
				c.sendMessage("Zamorak blesses you with a cape.");
				c.getItems().addItem(2414, 1);
			}
		break;
		
		default:
			ScriptManager.callFunc("objectClick1_"+objectType, c, objectType, obX, obY);
			break;

		}
	}
	
	public void secondClickObject(int objectType, int obX, int obY) {
		c.clickObjectType = 0;
			/*ObjectDef def = ObjectDef.getObjectDef(objectType);
			if(def != null)
				if(def.name.equalsIgnoreCase("Ladder")) {
					if(def.actions[1].equals("Climb-up")) {
						if(obX == 3069 && obY == 10256) { // custom locations
							c.getPA().movePlayer(3017, 3850, 0);
							return;
						}
						if(obX == 3017 && obY == 10249) { // custom locations
							c.getPA().movePlayer(3069, 3857, 0);
							return;
						}
						if(c.getY() > 6400) {
							c.getPA().movePlayer(obX+1, obY+1-6400, c.heightLevel);
							return;
						} else {
							c.getPA().movePlayer(c.absX, c.absY, c.heightLevel+1);
							return;
						}
					}
					if(def.actions[1].equals("Climb-down")) {
						if(obX == 3017 && obY == 3849) { // custom locations
							c.getPA().movePlayer(3069, 10257, 0);
							return;
						}
						if(obX == 3069 && obY == 3856) { // custom locations
							c.getPA().movePlayer(3017, 10248, 0);
							return;
						}
						if(c.getY() < 6400 && (c.heightLevel & 3) == 0) {
							c.getPA().movePlayer(c.getX(), c.getY()+6400, c.heightLevel);
							return;
						} else {
							c.getPA().movePlayer(c.absX, c.absY, c.heightLevel-1);
							return;
						}
					}
				}*/
		switch(objectType) {
		//castlewars
		case 4423:
		case 4424:
		case 4427:
		case 4428:
			CastleWars.attackDoor(c, objectType);
			break;
			//end
			
			
		case 10177:
			c.getPA().movePlayer(2544, 3741, 0);
			break;
		case 2213:
		case 26972:
		case 14367:
			c.getPA().openUpBank();
			break;
			
		case 2646:
			Flax.pickFlax(c, obX, obY);
		break;
		case 11666:
		case 3044:
		case 2781:
			Smelting.openInterface(c);
			break;
		case 2090:
		case 2091:
		case 3042:
			Mining.prospectRock(c, "copper ore");
			break;
		case 2094:
		case 2095:
		case 3043:
			Mining.prospectRock(c, "tin ore");
			break;
		case 2110:
			Mining.prospectRock(c, "blurite ore");
			break;
		case 2092:
		case 2093:
			Mining.prospectRock(c, "iron ore");
			break;
		case 2100:
		case 2101:
			Mining.prospectRock(c, "silver ore");
			break;
		case 2098:
		case 2099:
			Mining.prospectRock(c, "gold ore");
			break;
		case 2096:
		case 2097:
			Mining.prospectRock(c, "coal");
			break;
		case 2102:
		case 2103:
			Mining.prospectRock(c, "mithril ore");
			break;
		case 2104:
		case 2105:
			Mining.prospectRock(c, "adamantite ore");
			break;
		case 2106:
		case 2107:
			Mining.prospectRock(c, "runite ore");
			break;
		case 450:
		case 451:
			Mining.prospectNothing(c);
		break;
			case 2558:
				if (System.currentTimeMillis() - c.lastLockPick < 3000 || c.freezeTimer > 0)
					break;
				if (c.getItems().playerHasItem(1523,1)) {
						c.lastLockPick = System.currentTimeMillis();
						if (Misc.random(10) <= 3){
							c.sendMessage("You fail to pick the lock.");
							break;
						}
					if (c.objectX == 3044 && c.objectY == 3956) {
						if (c.absX == 3045) {
							c.getPA().walkTo2(-1,0);
						} else if (c.absX == 3044) {
							c.getPA().walkTo2(1,0);
						}
					
					} else if (c.objectX == 3038 && c.objectY == 3956) {
						if (c.absX == 3037) {
							c.getPA().walkTo2(1,0);
						} else if (c.absX == 3038) {
							c.getPA().walkTo2(-1,0);
						}				
					} else if (c.objectX == 3041 && c.objectY == 3959) {
						if (c.absY == 3960) {
							c.getPA().walkTo2(0,-1);
						} else if (c.absY == 3959) {
							c.getPA().walkTo2(0,1);
						}					
					}
				} else {
					c.sendMessage("I need a lockpick to pick this lock.");
				}
			break;
		default:
			ScriptManager.callFunc("objectClick2_"+objectType, c, objectType, obX, obY);
			break;
		}
	}
	
	
	public void thirdClickObject(int objectType, int obX, int obY) {
		c.clickObjectType = 0;
		/*ObjectDef def = ObjectDef.getObjectDef(objectType);
		if(def != null)
		if(def.name.equalsIgnoreCase("Ladder")) {
			if(def.actions[2].equals("Climb-down")) {
				if(obX == 3017 && obY == 3849) { // custom locations
					c.getPA().movePlayer(3069, 10257, 0);
					return;
				}
				if(obX == 3069 && obY == 3856) { // custom locations
					c.getPA().movePlayer(3017, 10248, 0);
					return;
				}
				if(c.getY() < 6400 && (c.heightLevel & 3) == 0) {
					c.getPA().movePlayer(c.getX(), c.getY()+6400, c.heightLevel);
					return;
				} else {
					c.getPA().movePlayer(c.absX, c.absY, c.heightLevel-1);
					return;
				}
			}
			if(def.actions[2].equals("Climb-up")) {
				if(obX == 3069 && obY == 10256) { // custom locations
					c.getPA().movePlayer(3017, 3850, 0);
					return;
				}
				if(obX == 3017 && obY == 10249) { // custom locations
					c.getPA().movePlayer(3069, 3857, 0);
					return;
				}
				if(c.getY() > 6400) {
					c.getPA().movePlayer(obX+1, obY+1-6400, c.heightLevel);
					return;
				} else {
					c.getPA().movePlayer(c.absX, c.absY, c.heightLevel+1);
					return;
				}
			}
		}*/
		c.sendMessage("Object type: " + objectType);
		switch(objectType) {
		case 10177: // Dagganoth ladder 1st level
			c.getPA().movePlayer(1798, 4407, 3);
		break;	
		//In here
		default:
			ScriptManager.callFunc("objectClick3_"+objectType, c, objectType, obX, obY);
			break;
		}
	}
	
	public void firstClickNpc(int npcType) {
		c.clickNpcType = 0;
		//c.npcClickIndex = 0;
		if(c.getTT().clueNpc(npcType))
			return;
		if(Implings.Imps.implings.containsKey(npcType)) {
			Imps.catchImp(c, npcType, c.npcClickIndex);
			return;
		}
		c.npcClickIndex = 0;
		
		// Check npc-shop mapping first
		int shopId = server.world.ShopHandler.getShopForNpc(npcType);
		if (shopId != -1) {
			c.getShops().openShop(shopId);
			return;
		}
		
		switch(npcType) {
		/**
		 * Rank switcher
		 */
		case 663: //rank switch
			c.getDH().sendDialogues(750, npcType);
			break;
			
		case 1301: //supplies2
			c.getShops().openShop(81);
			break;
		
		/**
		 * travelers
		 */
		case 213: //varrock.....
			c.getDH().sendDialogues(555, npcType);
			break;
		case 216: //kharid.....
			c.getDH().sendDialogues(557, npcType);
			break;
		case 377: //sailor 1
			c.getDH().sendDialogues(559, npcType);
			break;
		case 376: //sailor 2
			c.getDH().sendDialogues(561, npcType);
			break;
		case 1704: //sailor 3
			c.getDH().sendDialogues(563, npcType);
			break;
		case 1304: //sailor 4
			c.getDH().sendDialogues(565, npcType);
			break;
		case 231: //barb.....
			c.getDH().sendDialogues(567, npcType);
			break;
		case 263: //rest.....
			c.getDH().sendDialogues(569, npcType);
			break;
		case 2139: //gnome city.....
			c.getDH().sendDialogues(571, npcType);
			break;
		case 1182: //elf.....
			c.getDH().sendDialogues(573, npcType);
			break;
			
		case 647: //rest
			c.getDH().sendDialogues(577, npcType);
			break;
		case 608: //rest
			c.getDH().sendDialogues(579, npcType);
			break;
		case 741: //rest
			c.getDH().sendDialogues(581, npcType);
			break;
		case 746: //rest
			c.getDH().sendDialogues(583, npcType);
			break;
		case 1841: //rest
			c.getDH().sendDialogues(585, npcType);
			break;
		case 2637: //rest
			c.getDH().sendDialogues(587, npcType);
			break;
		case 1703: //rest
			c.getDH().sendDialogues(589, npcType);
			break;
		case 514: //rest
			c.getDH().sendDialogues(591, npcType);
			break;
		case 515: //rest
			c.getDH().sendDialogues(593, npcType);
			break;
		case 2580: //rest
			c.getDH().sendDialogues(595, npcType);
			break;
		case 2138: //gnome glider
			c.getPA().showInterface(802);
			break;
		
		case 5113://hunt
			c.getDH().sendDialogues(551, npcType);
			break;
		case 4247://constr
			c.getDH().sendDialogues(549, npcType);
			break;
		case 2157: //bh
			c.getDH().sendDialogues(505, npcType);
			break;
		case 664: //stat reset
			c.getDH().sendDialogues(502, npcType);
			break;
		case 537:
			c.getShops().openShop(77);
			break;
		case 675:
			c.getShops().openShop(76);
			break;
		case 410:
			c.getDH().sendDialogues(16, -1);
			break;
		case 378:
			c.getPA().movePlayer(2977, 9515, 1);
			break;
		case 239:
			c.getShops().openShop(74);
			break;
		case 3788:
			PestControlRewards.exchangePestPoints(c);
			break;
		case 3789:
			c.getShops().openShop(75);
			break;
		case 2296:
			c.fadeKQ(3229, 3108, 0);
			break;
		case 556:
			c.getPA().movePlayer(2872, 5269, 2);
			break;
		case 2257:
			c.getPA().movePlayer(2919, 5274, 0);
			break;
		case 2259:
			c.getPA().movePlayer(2885, 5344, 2);
			break;
		case 261://snow mountain traveler
			c.getDH().sendDialogues(487, npcType);
			break;
			
		case 511: //desert traveler
			c.getDH().sendDialogues(486, npcType);
			break;
			
		case 510: //dungeon traveler
			c.getDH().sendDialogues(488, npcType);
			break;
			
		//case 377: //boat traveler
			//c.getDH().sendDialogues(490, npcType);
			//break;
			
		case 693: //rang guild shots
			c.getDH().sendDialogues(483, npcType);
			break;
			
		case 694: //rang guild store
			c.getShops().openShop(73);
			break;
			
		case 4297: //str
			c.getDH().sendDialogues(444, npcType);
			break;
			
		case 4288: //atack
			c.getDH().sendDialogues(445, npcType);
			break;
			
		case 705: //def
			c.getDH().sendDialogues(449, npcType);
			break;
			
		case 961: //hp
			c.getDH().sendDialogues(447, npcType);
			break;
			
		case 1658: //magic
			c.getDH().sendDialogues(455, npcType);
			break;
			
		case 802: //pray
			c.getDH().sendDialogues(451, npcType);
			break;
			
		case 682: //ranged
			c.getDH().sendDialogues(453, npcType);
			break;
			
		case 1599: //slayer
			c.getDH().sendDialogues(442, npcType);
			break;
			
		case 3295: //mining
			c.getDH().sendDialogues(427, npcType);
			break;
			
		//case 553: //rc
			//c.getDH().sendDialogues(421, npcType);
			//break;
			
		case 604://smithing
			c.getDH().sendDialogues(429, npcType);
			break;
			
		case 308: //fishing
			c.getDH().sendDialogues(431, npcType);
			break;
			
		case 4946://fm
			c.getDH().sendDialogues(435, npcType);
			break;
			
		case 847: //cook
			c.getDH().sendDialogues(433, npcType);
			break;
			
		case 575: //fletching
			c.getDH().sendDialogues(425, npcType);
		break;
		
		case 805: //crafting
			c.getDH().sendDialogues(423, npcType);
			break;
			
		case 3299: //farming
			c.getDH().sendDialogues(439, npcType);
			break;
			
		case 4906: //wc
			c.getDH().sendDialogues(437, npcType);
			break;
			
		case 455: //herbaloreh
			c.getDH().sendDialogues(417, npcType);
			break;
			
		case 2270: //theivng
			c.getDH().sendDialogues(419, npcType);
			break;
			
		case 437: //agility
			c.getDH().sendDialogues(415, npcType);
			break;
			
		case 588:
			c.getShops().openShop(102);
			break;

			case 2356:
			c.getShops().openShop(105);
			break;

			case 3796:
			c.getShops().openShop(106);
			break;

			case 1860:
			c.getShops().openShop(107);
			break;

			case 519:
			c.getShops().openShop(108); //should sell barrows or something like that
			break;

			case 562:
			c.getShops().openShop(1010);
			break;

			case 581:
			c.getShops().openShop(1011);
			break;

			case 554:
			c.getShops().openShop(1013);
			break;

			case 601:
			c.getShops().openShop(1014);
			break;

			case 1039:
			c.getShops().openShop(1016);
			break;

			case 2353:
			c.getShops().openShop(1017);
			break;

			case 3166:
			c.getShops().openShop(1018);
			break;

			case 2161:
			c.getShops().openShop(1019);
			break;

			case 2162:
			c.getShops().openShop(1020);
			break;

			case 600:
			c.getShops().openShop(1021);
			break;

			case 603:
			c.getShops().openShop(1022);
			break;

			case 593:
			c.getShops().openShop(1023);
			break;

			case 585:
			c.getShops().openShop(1025);
			break;

			case 2305:
			c.getShops().openShop(1026);
			break;

			case 2307:
			c.getShops().openShop(1027);
			break;

			case 2304:
			c.getShops().openShop(1028);
			break;

			case 2306:
			c.getShops().openShop(1029);
			break;

			case 517:
			c.getShops().openShop(1030);
			break;

			case 558:
			c.getShops().openShop(1031);
			break;

			case 576:
			c.getShops().openShop(1032);
			break;

			case 1369:
			c.getShops().openShop(1033);
			break;

			case 557:
			c.getShops().openShop(1034);
			break;

			case 1038:
			c.getShops().openShop(1035);
			break;

			case 1433:
			c.getShops().openShop(1036);
			break;

			case 584:
			c.getShops().openShop(1037);
			break;

			case 540:
			c.getShops().openShop(1038);
			break;

			case 538:
			c.getShops().openShop(1040);
			break;

			case 1303:
			c.getShops().openShop(1041);
			break;

			case 578:
			c.getShops().openShop(1042);
			break;

			case 587:
			c.getShops().openShop(1043);
			break;

			case 1398:
			c.getShops().openShop(1044);
			break;
			
			case 1865:
			c.getShops().openShop(1046);
			break;

			case 543:
			c.getShops().openShop(1047);
			break;

			case 2198:
			c.getShops().openShop(1048);
			break;

			case 580:
			c.getShops().openShop(1049);
			break;

			case 1862:
			c.getShops().openShop(1050);
			break;
			
			case 559:
			c.getShops().openShop(9);
			break;

			case 583:
			c.getShops().openShop(1051);
			break;
                              
                        case 461:
			c.getShops().openShop(1053);
			break;


			case 903:
			c.getShops().openShop(1054);
			break;

			case 2258:
			c.getPA().startTeleport(3039, 4834, 0, "modern"); //first click teleports second click open shops
			break;

			case 1435:
			c.getShops().openShop(1056);
			break;

			case 3800:
			c.getShops().openShop(1057);
			break;

			case 2623:
			c.getShops().openShop(1058);
			break;

			case 594:
			c.getShops().openShop(1059);
			break;

			case 579:
			c.getShops().openShop(1060);
			break;

			case 2160:
			case 2191:
			c.getShops().openShop(1061);
			break;

			case 549:
				c.getShops().openShop(8);
			break;

			case 542:
			c.getShops().openShop(1064);
			break;

			case 3038:
			c.getShops().openShop(1065);
			break;

			case 544:
			c.getShops().openShop(1066);
			break;

			case 541:
			c.getShops().openShop(1067);
			break;

			case 1434:
			c.getShops().openShop(1068);
			break;

			case 577:
			c.getShops().openShop(1069);
			break;

			case 539:
			c.getShops().openShop(1070);
			break;

			case 1980:
			c.getShops().openShop(1071);
			break;

			case 382:
			c.getShops().openShop(1073);
			break;

			case 3541:
			c.getShops().openShop(1074);
			break;

			case 1436:
			c.getShops().openShop(1076);
			break;

			case 590:
			c.getShops().openShop(1077);
			break;

			case 971:
			c.getShops().openShop(1078);
			break;

			case 1040:
			c.getShops().openShop(1080);
			break;

			case 563:
			c.getShops().openShop(1081);
			break;

			case 522:
			c.getShops().openShop(1082);
			break;

			case 524:
			c.getShops().openShop(1083);
			break;

			case 526:
			c.getShops().openShop(1084);
			break;

			case 2154:
			c.getShops().openShop(1085);
			break;

			case 1334:
			c.getShops().openShop(1086);
			break;

			case 2552:
			c.getShops().openShop(1087);
			break;

			case 528:
			c.getShops().openShop(1088);
			break;

			case 1254:
			c.getShops().openShop(1089);
			break;

			case 2086:
			c.getShops().openShop(1090);
			break;

			case 3824:
			c.getShops().openShop(1091);
			break;

			case 1866:
			c.getShops().openShop(1092);
			break;

			case 1699:
			c.getShops().openShop(1093);
			break;

			case 1282:
			c.getShops().openShop(1094);
			break;
			case 516:
			c.getShops().openShop(1096);
			break;

			case 560:
			c.getShops().openShop(1097);
			break;

			case 471:
			c.getShops().openShop(1098);
			break;

			case 1208:
			c.getShops().openShop(1099);
			break;

			case 532:
			c.getShops().openShop(1100);
			break;

			case 534:
			c.getShops().openShop(1102);
			break;

			case 836:
			c.getShops().openShop(1103);
			break;

			case 551:
			c.getShops().openShop(1104);
			break;

			case 586:
			c.getShops().openShop(1105);
			break;

			case 564:
			c.getShops().openShop(1106);
			break;

			case 573:
			case 1316:
			case 547:
			c.getShops().openShop(1108);
			break;

			case 1787:
			c.getShops().openShop(1110);
			break;
			
		case 1597:
			c.getDH().sendDialogues(400, c.npcType);
			break;
		case 804:
			Tanning.sendTanningInterface(c);
			break;
			
		case 309:
		case 312:
		case 313:
		case 316:
		case 326:
			Fishing.setupFishing(c, Fishing.forSpot(npcType, false));
			break;
			
		case 2244:
			if (c.completedTut){
			c.getDH().sendDialogues(474, 2244);
			} else {
			c.getDH().sendDialogues(460, 2244);
			}
			break;
		case 484:
			c.getDH().sendDialogues(88, 484);
			break;
		/*Quests*/
		/*case 741:
			if(c.RuneMysteries == 0) {
				c.getDH().sendDialogues(1100, 741);
			} else if (c.RuneMysteries == 4) {
				c.getDH().sendDialogues(1099, 741);
			}
			
			break;*/
			
		case 300:
			if(c.RuneMysteries == 1) {
				c.getDH().sendDialogues(1149, 300);
			} else if(c.RuneMysteries == 3) {
				c.getDH().sendDialogues(1192, 300);
			}
		
			break;
			
		case 553:
			if(c.RuneMysteries == 2) {
				c.getDH().sendDialogues(1173, 553);
			} else {
				c.getShops().openShop(6);
			}
			break;
		case 278:
			if (c.cookAss == 0) {
				c.getDH().sendDialogues(600, 278);
			} else if (c.cookAss == 1) {
				c.getDH().sendDialogues(616, 278);
			} else if (c.cookAss == 2) {
				c.getDH().sendDialogues(619, 278);
			} else if (c.cookAss == 3) {
				c.getDH().sendDialogues(624, 278);
			}
			break;
		case 3001:
			c.getDH().sendDialogues(300, 3001);
			break;
		case 209:
			c.getDH().sendDialogues(86, 209);
			break;
		case 606:
			c.getDH().sendDialogues(673, 606);
			break;
		case 1917:
			c.getDH().sendDialogues(84, 1917);
			break;
		case 2201:
			c.getDH().sendDialogues(83, 2201);
			break;
		case 462:
			c.getDH().sendDialogues(17, 462);
			break;
		case 1696:
			c.getShops().openShop(47);
			break;
		case 291:
			c.getDH().sendDialogues(77, 291);
			break;
		case 545:
			c.getDH().sendDialogues(999, 545);
			break;

		case 692:
			c.getDH().sendDialogues(75, 692);
			break;
		case 706:
			c.getDH().sendDialogues(70, 706);
			break;
		case 599:
			c.getDH().sendDialogues(63, 599);
			break;
		//case 1304:
		//	c.getDH().sendDialogues(58, 1304);
			//break;
		case 201:
			c.getDH().sendDialogues(9001, 201);
			break;
		case 494:
		case 495:
		case 496:
		case 497:
			c.getDH().sendDialogues(1000, 494);
		break;
			case 1598:
				if (c.slayerTask <= 0) {
					c.getDH().sendDialogues(11,npcType);
				} else {
					c.getDH().sendDialogues(13,npcType);
				}
			break;

			case 1526:
			c.getDH().sendDialogues(55,npcType);
			break;


			//case 212:
			case 589:
			c.getDH().sendDialogues(56,npcType);
			break;

			case 1152:
				c.getDH().sendDialogues(16,npcType);
			break;

			case 905:
				c.getDH().sendDialogues(5, npcType);
			break;
			case 460:
				c.getDH().sendDialogues(3, npcType);
			break;

			case 904:
				c.sendMessage("You have " + c.magePoints + " points.");
			break;
			
			case 520: case 521: case 550: case 595: case 561: case 531:
			c.getDH().sendDialogues(999, npcType);
			break;
			
			case 812:
			c.getDH().sendDialogues(998, npcType);
			break;
			
			case 546:
				c.getDH().sendDialogues(1004, npcType);
			break;
			case 548:
				c.getDH().sendDialogues(1008, npcType);
				break;
			case 641:
				c.getDH().sendDialogues(997, npcType);
			break;
			case 530:
				c.getDH().sendDialogues(996, npcType);
			break;
		default:
		//c.getDH().sendDialogues(144, npcType);
			c.getDH().sendDialogues(2000, npcType);
			ScriptManager.callFunc("npcClick1_"+npcType, c, npcType);
			if(c.playerRights == 3) 
				Misc.println("First Click Npc : "+npcType);
			break;
		}
	}

	public void secondClickNpc(int npcType) {
		c.clickNpcType = 0;
		c.npcClickIndex = 0;
		if(c.getTT().clueNpc(npcType))
			return;
		switch(npcType) {
		case 1301: //supplies2
			c.getShops().openShop(81);
			break;
		case 2157: //bh
			c.getDH().sendDialogues(505, npcType);
			break;
		case 553:
			c.getShops().openShop(6);
			break;
		case 537:
			c.getShops().openShop(77);
			break;
			
		case 958:
			c.getPA().openUpBank();
		break;
		
		case 2234:
		case 2235:
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
		case 6:
		case 7:
		case 1757:
		case 1758:
		case 1759:
		case 1760:
		case 1761:
		case 1715:
		case 1714:
		case 1710:
		case 1711:
		case 1712:
		case 15:
		case 18:
		case 187:
		case 9:
		case 10:
		case 1880:
		case 1881:
		case 1926:
		case 1927:
		case 1928:
		case 1929:
		case 1930:
		case 1931:
		case 23:
		case 26:
		case 1883:
		case 1884:
		case 32:
		case 1904:
		case 1905:
		case 20:
		case 365:
		case 2256:
		case 66:
		case 67:
		case 68:
		case 21:
			c.getThieving().pickpocketNpc(c, npcType);
			break;
			
		case 3788:
			PestControlRewards.exchangePestPoints(c);
			break;
		case 3789:
			c.getShops().openShop(75);
			break;
		
		case 309:
		case 312:
		case 313:
		case 316:
		case 326:
			Fishing.setupFishing(c, Fishing.forSpot(npcType, true));
			break;
			
		case 209:
			c.getShops().openShop(22);
			break;
		case 1917:
			c.getShops().openShop(21);
			break;
		case 2620:
			c.getShops().openShop(20);
			break;
		case 2622:
			c.getShops().openShop(19);
			break;
		case 1696:
			c.getShops().openShop(47);
			break;
		case 545:
			c.getShops().openShop(13);
			break;
		case 1658:
			c.getShops().openShop(14);
			break;
			
		case 692:
			c.getShops().openShop(11);
			break;
			/* - - Shops - - */
			/* General Store / Assistant Varrock */
			case 520: case 521:
			c.getShops().openShop(2);
			break;
			/* Thessalia Varrock */
			case 548:
			c.getShops().openShop(24);
			break;
			/* Zaff Varrock */
			case 546:
			c.getShops().openShop(3);
			break;
			/* Swordshop Varrock */
			case 561:
			c.getShops().openShop(4);
			break;
			/* Tea Shop */
			case 595:
			c.getShops().openShop(5);
			break;
			/* Lowe's Archery Emporium */
			case 550:
			c.getShops().openShop(7);
			break;
			/* Horvik's Armour Shop */
			case 549:
			c.getShops().openShop(8);
			break;
			case 494: 
			case 495: 
				case 496: 
					case 497: 
						case 498: 
							case 499:
			c.getPA().openUpBank();
			break;
			
							case 588:
								c.getShops().openShop(102);
								break;

								case 2356:
								c.getShops().openShop(105);
								break;

								case 3796:
								c.getShops().openShop(106);
								break;

								case 1860:
								c.getShops().openShop(107);
								break;

								case 519:
								c.getShops().openShop(108); //should sell barrows or something like that
								break;

								case 562:
								c.getShops().openShop(1010);
								break;

								case 581:
								c.getShops().openShop(1011);
								break;

								case 554:
								c.getShops().openShop(1013);
								break;

								case 601:
								c.getShops().openShop(1014);
								break;

								case 1039:
								c.getShops().openShop(1016);
								break;

								case 2353:
								c.getShops().openShop(1017);
								break;

								case 3166:
								c.getShops().openShop(1018);
								break;
								
								case 559:
									c.getShops().openShop(109);
									break;

								case 2161:
								c.getShops().openShop(1019);
								break;

								case 2162:
								c.getShops().openShop(1020);
								break;

								case 600:
								c.getShops().openShop(1021);
								break;

								case 603:
								c.getShops().openShop(1022);
								break;

								case 593:
								c.getShops().openShop(1023);
								break;

								case 585:
								c.getShops().openShop(1025);
								break;

								case 2305:
								c.getShops().openShop(1026);
								break;

								case 2307:
								c.getShops().openShop(1027);
								break;

								case 2304:
								c.getShops().openShop(1028);
								break;

								case 2306:
								c.getShops().openShop(1029);
								break;

								case 517:
								c.getShops().openShop(1030);
								break;

								case 558:
								c.getShops().openShop(1031);
								break;

								case 576:
								c.getShops().openShop(1032);
								break;

								case 1369:
								c.getShops().openShop(1033);
								break;

								case 557:
								c.getShops().openShop(1034);
								break;

								case 1038:
								c.getShops().openShop(1035);
								break;

								case 1433:
								c.getShops().openShop(1036);
								break;

								case 584:
								c.getShops().openShop(1037);
								break;

								case 540:
								c.getShops().openShop(1038);
								break;

								case 538:
								c.getShops().openShop(1040);
								break;

								case 1303:
								c.getShops().openShop(1041);
								break;

								case 578:
								c.getShops().openShop(1042);
								break;

								case 587:
								c.getShops().openShop(1043);
								break;

								case 1398:
								c.getShops().openShop(1044);
								break;
								
								case 1865:
								c.getShops().openShop(1046);
								break;

								case 543:
								c.getShops().openShop(1047);
								break;

								case 2198:
								c.getShops().openShop(1048);
								break;

								case 580:
								c.getShops().openShop(1049);
								break;

								case 1862:
								c.getShops().openShop(1050);
								break;

								case 583:
								c.getShops().openShop(1051);
								break;
					                              
					                        case 461:
								c.getShops().openShop(1053);
								break;


								case 903:
								c.getShops().openShop(1054);
								break;

								case 2258:
								c.getPA().startTeleport(3039, 4834, 0, "modern"); //first click teleports second click open shops
								break;

								case 1435:
								c.getShops().openShop(1056);
								break;

								case 3800:
								c.getShops().openShop(1057);
								break;

								case 2623:
								c.getShops().openShop(1058);
								break;

								case 594:
								c.getShops().openShop(1059);
								break;

								case 579:
								c.getShops().openShop(1060);
								break;

								case 2160:
								case 2191:
								c.getShops().openShop(1061);
								break;

								case 542:
								c.getShops().openShop(1064);
								break;

								case 3038:
								c.getShops().openShop(1065);
								break;

								case 544:
								c.getShops().openShop(1066);
								break;

								case 541:
								c.getShops().openShop(1067);
								break;

								case 1434:
								c.getShops().openShop(1068);
								break;

								case 577:
								c.getShops().openShop(1069);
								break;

								case 539:
								c.getShops().openShop(1070);
								break;

								case 1980:
								c.getShops().openShop(1071);
								break;

								case 382:
								c.getShops().openShop(1073);
								break;

								case 3541:
								c.getShops().openShop(1074);
								break;

								case 1436:
								c.getShops().openShop(1076);
								break;

								case 590:
								c.getShops().openShop(1077);
								break;

								case 971:
								c.getShops().openShop(1078);
								break;

								case 1040:
								c.getShops().openShop(1080);
								break;

								case 563:
								c.getShops().openShop(1081);
								break;

								case 522:
								c.getShops().openShop(1082);
								break;

								case 524:
								c.getShops().openShop(1083);
								break;

								case 526:
								c.getShops().openShop(1084);
								break;

								case 2154:
								c.getShops().openShop(1085);
								break;

								case 1334:
								c.getShops().openShop(1086);
								break;

								case 2552:
								c.getShops().openShop(1087);
								break;

								case 528:
								c.getShops().openShop(1088);
								break;

								case 1254:
								c.getShops().openShop(1089);
								break;

								case 2086:
								c.getShops().openShop(1090);
								break;

								case 3824:
								c.getShops().openShop(1091);
								break;

								case 1866:
								c.getShops().openShop(1092);
								break;

								case 1699:
								c.getShops().openShop(1093);
								break;

								case 1282:
								c.getShops().openShop(1094);
								break;
								case 516:
								c.getShops().openShop(1096);
								break;

								case 560:
								c.getShops().openShop(1097);
								break;

								case 471:
								c.getShops().openShop(1098);
								break;

								case 1208:
								c.getShops().openShop(1099);
								break;

								case 532:
								c.getShops().openShop(1100);
								break;

								case 534:
								c.getShops().openShop(1102);
								break;

								case 836:
								c.getShops().openShop(1103);
								break;

								case 551:
								c.getShops().openShop(1104);
								break;

								case 586:
								c.getShops().openShop(1105);
								break;

								case 564:
								c.getShops().openShop(1106);
								break;

								case 573:
								case 1316:
								case 547:
								c.getShops().openShop(1108);
								break;

								case 1787:
								c.getShops().openShop(1110);
								break;
								
			default:
			//c.getDH().sendDialogues(144, npcType);
				ScriptManager.callFunc("npcClick2_"+npcType, c, npcType);
				if(c.playerRights == 3) 
					Misc.println("Second Click Npc : "+npcType);
				break;
			
		}
	}
	
	public void thirdClickNpc(int npcType) {
		c.clickNpcType = 0;
		c.npcClickIndex = 0;
		switch(npcType) {
		case 70:
		case 1596:
		case 1597:
		case 1598:
		case 1599:
		c.getShops().openShop(1109);
		break;

case 836:
		c.getShops().openShop(1103);
		break;
		case 1526:
			c.getShops().openShop(78);
			break;
		case 553:
			//if ((c.adventurer && c.RuneMysteries == 4) || !c.adventurer) {
				c.getPA().movePlayer(2898, 4819, 0);
			//} else {
			//	c.sendMessage("You haven't completed Rune Mysteries yet.");
			//}
			break;
			default:
			//c.getDH().sendDialogues(144, npcType);
				ScriptManager.callFunc("npcClick3_"+npcType, c, npcType);
				if(c.playerRights == 3) 
					Misc.println("Third Click NPC : "+npcType);
				break;

		}
	}
	

}