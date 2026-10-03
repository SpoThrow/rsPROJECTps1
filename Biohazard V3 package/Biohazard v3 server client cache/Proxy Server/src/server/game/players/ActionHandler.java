package server.game.players;

import server.clip.region.ObjectDef;
import server.Config;
import server.Server;
import server.content.skills.Implings;
import server.content.skills.Implings.Imps;
import server.content.skills.JewelryMaking;
import server.content.skills.Mining;
import server.content.skills.Runecrafting;
import server.content.skills.Smelting;
import server.content.skills.Tanning;
import server.content.skills.Woodcutting;
import server.game.content.DwarfCannon;
import server.game.minigames.barrows.Barrows;
import server.game.minigames.barrows.BarrowsData;
import server.game.minigames.bountyhunter.BountyHunter;
import server.game.minigames.castlewars.CastleWarObjects;
import server.game.minigames.pestcontrol.PestControlRewards;
import server.game.minigames.roguesden.WallSafes;
import server.game.npcs.WorldAdventurer;
import server.game.objects.Object;
import server.game.players.actions.npcs.NpcActionHandler;
import server.game.players.actions.npcs.NpcClick;
import server.game.players.actions.objects.ObjectClick;
import server.game.players.actions.objects.ObjectHandler;
import core.util.Misc;

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

		// Migrated object actions. Consulted before the switch below so that an
		// object can be moved out of it without the two ever both running.
		if (ObjectHandler.dispatch(c, objectType, ObjectClick.FIRST, obX, obY)) {
			return;
		}

		switch(objectType) {
		case 11214:
			if(c.position.absX != obX && c.position.absY != obY) {
				c.sendMessage("You need to stand on the platform in order to build.");
				return;
			}
			c.getPA().showInterface(31250);
			break;
		case 7236: //wallsafes
			WallSafes.checkWallSafe(c);
			break;
		case 8143: //picking herbs
			c.getFarming().pickHerb();
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
		case 2643:
			JewelryMaking.mouldInterface(c);
			break;
			
		case 10194:
			//c.getPA().movePlayer(2544, 3741, 0); watchtower TODO
		break;
		
		case 10227:
			if (obj(1961, 4392))
				c.getPA().movePlayer(1961, 4392, 2);
			else 
				c.getPA().movePlayer(1932, 4377, 1);
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
			if(c.position.absX == 2851 && c.position.absY == 5333) {
				c.getPA().movePlayer(2850, 5333, 2);
			} else if(c.position.absX == 2850 && c.position.absY == 5333) {
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
		case 5099:
			if(c.skills.playerLevel[16] < 34) {
				c.sendMessage("You need an Agility level of 34 to pass this.");
				return;
			}
			if(c.objectX == 2698 && c.objectY == 9498) {
				c.getPA().movePlayer(2698, 9492, 0);
			} else if(c.objectX == 2698 && c.objectY == 9493) {
				c.getPA().movePlayer(2698, 9499, 0);
			}
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
				if (c.skills.playerLevel[Player.playerFishing] <= 50) {
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
			if (c.position.absX == 2880 && c.position.absY == 9813) {
				c.getPA().movePlayer(2878, 9813, 0);
			} else if (c.position.absX == 2878 && c.position.absY == 9813) {
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
			/* Shops */
		case 6839:
			c.getShops().openShop(9);
			c.sendMessage("As you look down in the chest, you see a small monkey with a money-pouch");
			c.sendMessage("next to him. I think there is where I should put the coins.");
			break;
		case 9356:
			c.getPA().enterCaves();
			c.sendMessage("Good luck!");
		break;

                case 2557:
                     if(c.getItems().playerHasItem(1523, 1) && c.position.absX == 3190 && c.position.absY == 3957) {
                        c.getPA().movePlayer(3190, 3958, 0);
                     } else if(c.getItems().playerHasItem(1523, 1) && c.position.absX == 3190 && c.position.absY == 3958) {
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
				if (c.position.absX < c.objectX) {
					c.getPA().walkTo(1,0);
				} else {
					c.getPA().walkTo(-1,0);
				}
			}
		break;

		case 272:
		c.getPA().movePlayer(c.position.absX, c.position.absY, 1);
		break;
		
		case 273:
		c.getPA().movePlayer(c.position.absX, c.position.absY, 0);
		break;

		case 245:
		c.getPA().movePlayer(c.position.absX, c.position.absY + 2, 2);
		break;

		case 246:
		c.getPA().movePlayer(c.position.absX, c.position.absY - 2, 1);
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
			c.getPA().movePlayer(c.position.absX, c.position.absY + 6393, 0);
		break;
		
		case 1734:
			c.getPA().movePlayer(c.position.absX, c.position.absY - 6396, 0);
		break;
		
		case 9357:
			c.getPA().resetTzhaar();
		break;
		
		case 8959:
			if (c.getX() == 2490 && (c.getY() == 10146 || c.getY() == 10148)) {
				if (c.getPA().checkForPlayer(2490, c.getY() == 10146 ? 10148 : 10146)) {
					new Object(6951, c.objectX, c.objectY, c.position.heightLevel, 1, 10, 8959, 15);	
				}			
			}
		break;

		case 2623:
			if (c.position.absX >= c.objectX)
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
				if (c.position.absX <= 2670)
					c.position.absX = 2671;
				else
					c.position.absX = 2670;
			if (c.objectX == 2643)
				if (c.position.absX >= 2643)
					c.position.absX = 2642;
				else
					c.position.absX = 2643;
			if (c.position.absX <= 2585)
				c.position.absY += 1;
			else c.position.absY -= 1;
			c.getPA().movePlayer(c.position.absX, c.position.absY, 0);
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
		
			
		
		
	
		case 9319:
			if (c.position.heightLevel == 0)
				c.getPA().movePlayer(c.position.absX, c.position.absY, 1);
			else if (c.position.heightLevel == 1)
				c.getPA().movePlayer(c.position.absX, c.position.absY, 2);
		break;
		
		case 9320:
			if (c.position.heightLevel == 1)
				c.getPA().movePlayer(c.position.absX, c.position.absY, 0);
			else if (c.position.heightLevel == 2)
				c.getPA().movePlayer(c.position.absX, c.position.absY, 1);
		break;
		
		case 4496:
		case 4494:
			if (c.position.heightLevel == 2) {
				c.getPA().movePlayer(c.position.absX - 5, c.position.absY, 1);
			} else if (c.position.heightLevel == 1) {
				c.getPA().movePlayer(c.position.absX + 5, c.position.absY, 0);
			}
		break;
		
		case 4493:
			if (c.position.heightLevel == 0) {
				c.getPA().movePlayer(c.position.absX - 5, c.position.absY, 1);
			} else if (c.position.heightLevel == 1) {
				c.getPA().movePlayer(c.position.absX + 5, c.position.absY, 2);
			}
		break;
		
		case 4495:
			if (c.position.heightLevel == 1) {
				c.getPA().movePlayer(c.position.absX + 5, c.position.absY, 2);
			}
		break;
		
		case 5126:
			if (c.position.absY == 3554)
				c.getPA().walkTo(0,1);
			else
				c.getPA().walkTo(0,-1);
		break;
		
		case 1759:
			if (c.objectX == 2884 && c.objectY == 3397) {
				c.getPA().movePlayer(c.position.absX, c.position.absY + 6400, 0);
			} else if (c.objectX == 2845 && c.objectY == 3516) {
				c.getPA().movePlayer(2884, 9798, 0);
			} else if (c.objectX == 2848 && c.objectY == 3513) {
				c.getPA().movePlayer(2884, 9798, 0);
			} else if (c.objectX == 2848 && c.objectY == 3519) {
				c.getPA().movePlayer(2884, 9798, 0);
			}
		break;
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
			if(c.skills.playerLevel[5] < c.getPA().getLevelForXP(c.skills.playerXP[5])) {
				c.startAnimation(645);
				c.skills.playerLevel[5] = c.getPA().getLevelForXP(c.skills.playerXP[5]);
				c.sendMessage("You recharge your prayer points.");
				c.getPA().refreshSkill(5);
			} else {
				switchPrayerBook();
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
			handleGenericObject(1, objectType, obX, obY);
			break;

		}
	}
	
	public void secondClickObject(int objectType, int obX, int obY) {
		c.clickObjectType = 0;
		if (ObjectHandler.dispatch(c, objectType, ObjectClick.SECOND, obX, obY)) {
			return;
		}
		switch(objectType) {
		case 10177:
			c.getPA().movePlayer(2544, 3741, 0);
			break;
		case 2646:
			Flax.pickFlax(c, obX, obY);
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
						if (c.position.absX == 3045) {
							c.getPA().walkTo2(-1,0);
						} else if (c.position.absX == 3044) {
							c.getPA().walkTo2(1,0);
						}
					
					} else if (c.objectX == 3038 && c.objectY == 3956) {
						if (c.position.absX == 3037) {
							c.getPA().walkTo2(1,0);
						} else if (c.position.absX == 3038) {
							c.getPA().walkTo2(-1,0);
						}				
					} else if (c.objectX == 3041 && c.objectY == 3959) {
						if (c.position.absY == 3960) {
							c.getPA().walkTo2(0,-1);
						} else if (c.position.absY == 3959) {
							c.getPA().walkTo2(0,1);
						}					
					}
				} else {
					c.sendMessage("I need a lockpick to pick this lock.");
				}
			break;
		case 409:
			switchPrayerBook();
			break;
		default:
			handleGenericObject(2, objectType, obX, obY);
			break;
		}
	}
	
	
	public void thirdClickObject(int objectType, int obX, int obY) {
		c.clickObjectType = 0;
		if (DwarfCannon.isCannonObject(objectType)) {
			DwarfCannon.pickup(c, obX, obY);
			return;
		}
		c.sendMessage("Object type: " + objectType);
		if (ObjectHandler.dispatch(c, objectType, ObjectClick.THIRD, obX, obY)) {
			return;
		}
		switch(objectType) {
		case 10177: // Dagganoth ladder 1st level
			c.getPA().movePlayer(1798, 4407, 3);
		break;	
		//In here
		default:
			handleGenericObject(3, objectType, obX, obY);
			break;
		}
	}

	private boolean handleGenericObject(int click, int objectType, int obX, int obY) {
		ObjectDef def = ObjectDef.getObjectDef(objectType);
		if (def == null || def.name == null) {
			return false;
		}
		String name = def.name.toLowerCase();
		if (name.equals("null") || name.length() == 0) {
			return false;
		}
		String action = null;
		if (def.actions != null && click - 1 >= 0 && click - 1 < def.actions.length) {
			action = def.actions[click - 1];
		}
		String act = action == null ? "" : action.toLowerCase();
		if (act.length() == 0 && click == 1 && def.actions != null) {
			for (int i = 0; i < def.actions.length; i++) {
				if (def.actions[i] != null && def.actions[i].length() > 0) {
					act = def.actions[i].toLowerCase();
					break;
				}
			}
		}
		if (click == 2 && isPrayerAltar(objectType, name)) {
			switchPrayerBook();
			return true;
		}
		if (act.length() == 0) {
			return false;
		}
		if (act.indexOf("switch") >= 0 || act.indexOf("convert") >= 0) {
			if (isPrayerAltar(objectType, name)) {
				switchPrayerBook();
				return true;
			}
		}
		if (act.indexOf("bank") >= 0 || name.indexOf("bank booth") >= 0
				|| name.indexOf("bank chest") >= 0 || name.equals("bank")) {
			c.getPA().openUpBank();
			return true;
		}
		if (act.indexOf("climb-up") >= 0 || (act.equals("climb") && click == 1)) {
			return climbObject(obX, obY, true);
		}
		if (act.indexOf("climb-down") >= 0) {
			return climbObject(obX, obY, false);
		}
		if (act.equals("open") || act.equals("close")) {
			return false;
		}
		if (act.indexOf("mine") >= 0 || act.indexOf("prospect") >= 0) {
			if (Mining.miningRocks(c, objectType)) {
				Mining.attemptData(c, objectType, obX, obY);
				return true;
			}
			if (act.indexOf("prospect") >= 0) {
				c.sendMessage("This rock contains ore.");
				return true;
			}
			return false;
		}
		if (act.indexOf("chop") >= 0) {
			int tree = treeIndexForName(name);
			if (tree >= 0) {
				Woodcutting.startWoodcutting(c, tree, obX, obY, click);
				return true;
			}
			return false;
		}
		if (act.indexOf("pray") >= 0 || act.indexOf("recharge") >= 0) {
			if(c.skills.playerLevel[5] < c.getPA().getLevelForXP(c.skills.playerXP[5])) {
				c.startAnimation(645);
				c.skills.playerLevel[5] = c.getPA().getLevelForXP(c.skills.playerXP[5]);
				c.sendMessage("You recharge your prayer points.");
				c.getPA().refreshSkill(5);
			} else {
				c.sendMessage("You already have full prayer points.");
			}
			return true;
		}
		return false;
	}

	private boolean isPrayerAltar(int objectType, String name) {
		if (objectType == 6552 || objectType == 410) {
			return false;
		}
		if (objectType == 409) {
			return true;
		}
		if (name.equals("altar") && name.indexOf("ancient") < 0 && name.indexOf("lunar") < 0) {
			return true;
		}
		return false;
	}

	private void switchPrayerBook() {
		if (c.altarPrayed == 0) {
			c.altarPrayed = 1;
			c.getCombat().resetPrayers();
			c.getPA().setPrayerBook();
			c.gfx100(2011);
			c.startAnimation(645);
			c.sendMessage("The altar switches your prayers to Curses.");
		} else {
			c.altarPrayed = 0;
			c.getCombat().resetPrayers();
			c.getPA().setPrayerBook();
			c.gfx100(2011);
			c.startAnimation(645);
			c.sendMessage("The altar switches your prayers to the regular book.");
		}
	}

	private int treeIndexForName(String name) {
		if (name.indexOf("magic") >= 0)
			return 8;
		if (name.indexOf("yew") >= 0)
			return 7;
		if (name.indexOf("maple") >= 0)
			return 6;
		if (name.indexOf("willow") >= 0)
			return 4;
		if (name.indexOf("oak") >= 0)
			return 3;
		if (name.indexOf("tree") >= 0 || name.indexOf("dead") >= 0)
			return 0;
		return -1;
	}

	private boolean climbObject(int obX, int obY, boolean up) {
		if (up) {
			if(obX == 3069 && obY == 10256) {
				c.getPA().movePlayer(3017, 3850, 0);
				return true;
			}
			if(obX == 3017 && obY == 10249) {
				c.getPA().movePlayer(3069, 3857, 0);
				return true;
			}
			if(c.getY() > 6400) {
				c.getPA().movePlayer(obX + 1, obY + 1 - 6400, c.position.heightLevel);
			} else {
				c.getPA().movePlayer(c.position.absX, c.position.absY, c.position.heightLevel + 1);
			}
			return true;
		}
		if(obX == 3017 && obY == 3849) {
			c.getPA().movePlayer(3069, 10257, 0);
			return true;
		}
		if(obX == 3069 && obY == 3856) {
			c.getPA().movePlayer(3017, 10248, 0);
			return true;
		}
		if(c.getY() < 6400 && (c.position.heightLevel & 3) == 0) {
			c.getPA().movePlayer(c.getX(), c.getY() + 6400, c.position.heightLevel);
		} else {
			int height = c.position.heightLevel - 1;
			if (height < 0) {
				height = 0;
			}
			c.getPA().movePlayer(c.position.absX, c.position.absY, height);
		}
		return true;
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
		if (WorldAdventurer.isAdventurer(c.npcClickIndex)) {
			WorldAdventurer.talk(c);
			c.npcClickIndex = 0;
			return;
		}
		c.npcClickIndex = 0;
		
		// Check npc-shop mapping first
		int shopId = server.world.ShopHandler.getShopForNpc(npcType);
		if (shopId != -1) {
			c.getShops().openShop(shopId);
			return;
		}
		
		// Migrated NPC actions. Deliberately after the npc-shop guard above: that
		// guard claims 38 of the ids this registry also holds, and it must keep
		// winning, exactly as it did over the switch.
		if (NpcActionHandler.dispatch(c, npcType, NpcClick.FIRST)) {
			return;
		}
		
		switch(npcType) {
		/**
		 * Rank switcher
		 */
		/**
		 * travelers
		 */
		case 2138: //gnome glider
			c.getPA().showInterface(802);
			break;
		
		case 410:
			c.getDH().sendDialogues(16, -1);
			break;
		case 3788:
			PestControlRewards.exchangePestPoints(c);
			break;
		case 2296:
			c.fadeKQ(3229, 3108, 0);
			break;
		//case 377: //boat traveler
			//c.getDH().sendDialogues(490, npcType);
			//break;
			
		//case 553: //rc
			//c.getDH().sendDialogues(421, npcType);
			//break;
			


		case 1597:
			c.getDH().sendDialogues(400, c.npcType);
			break;
		case 804:
			Tanning.sendTanningInterface(c);
			break;
			
			
		case 2244:
			if (c.completedTut){
			c.getDH().sendDialogues(474, 2244);
			} else {
			c.getDH().sendDialogues(460, 2244);
			}
			break;
		/*Quests*/
			
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
		//case 1304:
		//	c.getDH().sendDialogues(58, 1304);
			//break;
			case 1598:
				if (c.slayerTask <= 0) {
					c.getDH().sendDialogues(11,npcType);
				} else {
					c.getDH().sendDialogues(13,npcType);
				}
			break;





			case 904:
				c.sendMessage("You have " + c.magePoints + " points.");
			break;
			
		default:
		//c.getDH().sendDialogues(144, npcType);
			c.getDH().sendDialogues(2000, npcType);
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
		if (NpcActionHandler.dispatch(c, npcType, NpcClick.SECOND)) {
			return;
		}
		switch(npcType) {
		case 3788:
			PestControlRewards.exchangePestPoints(c);
			break;
			
			/* - - Shops - - */
			/* General Store / Assistant Varrock */
			/* Thessalia Varrock */
			/* Zaff Varrock */
			/* Swordshop Varrock */
			/* Tea Shop */
			/* Lowe's Archery Emporium */
			/* Horvik's Armour Shop */


			default:
			//c.getDH().sendDialogues(144, npcType);
				if(c.playerRights == 3) 
					Misc.println("Second Click Npc : "+npcType);
				break;
			
		}
	}
	
	public void thirdClickNpc(int npcType) {
		c.clickNpcType = 0;
		c.npcClickIndex = 0;
		if (NpcActionHandler.dispatch(c, npcType, NpcClick.THIRD)) {
			return;
		}
		// Every case moved to the NPC registry, so only the default body is left and the
		// switch no longer has anything to dispatch on. The commented-out sendDialogues
		// was unreachable even before that.
		if (c.playerRights == 3)
			Misc.println("Third Click NPC : " + npcType);
	}
	

}