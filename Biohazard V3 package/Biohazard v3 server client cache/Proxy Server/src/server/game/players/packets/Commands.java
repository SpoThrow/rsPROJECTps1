package server.game.players.packets;

import org.Vote.MainLoader;

import server.Config;
import server.Connection;
import server.Server;
import server.game.content.PlayerOwnedShop;
import server.game.items.ItemAssistant;
import server.game.players.Client;
import server.game.players.PacketType;
import server.game.players.PlayerHandler;
//import org.runetoplist.*;
import core.util.Misc;
import core.util.rspswebstore;


public class Commands implements PacketType {
	public boolean resetAnim = false;

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		String playerCommand = c.getInStream().readString();
		if (Config.SERVER_DEBUG) {
			Misc.println(c.playerName + " playerCommand: " + playerCommand);
		}
		if (playerCommand.startsWith("/")) {
			if (c.clan != null) {
				String message = playerCommand.substring(1);
				c.clan.sendChat(c, message);
			}
		}
		if (c.playerRights >= 0) {
			/*if (playerCommand.equalsIgnoreCase("master")) {
				for (int i = 0; i < 23; i++) {
					c.getPA().addSkillXP(c.getPA().getXPForLevel(100), i);
					c.getPA().refreshSkill(i);	
					c.getPA().requestUpdates();
				}
			}*/
			if (playerCommand.startsWith("bsearch") && c.isBanking) {
				String term = playerCommand.length() > 8 ? playerCommand.substring(8).trim() : "";
				c.getBank().applySearch(term);
			}
			if (playerCommand.equalsIgnoreCase("banki")) {
				if(c.isBanking) {
					for(int i = 0; i < c.playerItems.length; i++){
						c.getItems().bankItem(c.playerItems[i], i,c.playerItemsN[i]);
					}
					c.sendMessage("Inventory successfully banked.");
				}
			}
			if (playerCommand.equalsIgnoreCase("banke")) {
				if(c.isBanking) {
					for (int i = 0; i < c.playerEquipment.length; i++) {
						if (c.playerEquipment[i] > 0 && c.playerEquipmentN[i] > 0)
							c.getItems().addItemToBank(c.playerEquipment[i], c.playerEquipmentN[i]);
						c.getItems().replaceEquipment(i, -1);
					}
	            	c.sendMessage("Equipment successfully banked.");
				}
			}
			/*if (playerCommand.equalsIgnoreCase("findline")) {
				for(int line = 0; line < 20000; line++)
				c.getPA().sendFrame126(""+line+"", line);
			}*/
			if (playerCommand.equalsIgnoreCase("players")) {
				c.sendMessage("Current amount of players online: @red@"
						+ PlayerHandler.getPlayerCount() + "@bla@!");
			}
			if (playerCommand.equalsIgnoreCase("cancel")) {
				if (c.posSearchingItem || c.posSearchingPlayer) {
					c.posSearchingItem = false;
					c.posSearchingPlayer = false;
					c.sendMessage("Search cancelled.");
				} else {
					c.sendMessage("No search in progress to cancel.");
				}
			}
			if (playerCommand.startsWith("help")) {
				c.sendMessage("You have submitted your help ticket, please wait for a staff member to reply.");
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						if(PlayerHandler.players[j].playerRights > 0) {
							Client staff = (Client)PlayerHandler.players[j];
							staff.sendMessage(Misc.capitalize(c.playerName)+ " is requesting assistance!");
						}
					}
				}
			}
			if(playerCommand.equalsIgnoreCase("claimweb") || playerCommand.equalsIgnoreCase("donated")){
			    int returnval = rspswebstore.checkwebstore(c, c.playerName);
			    String returnv = Integer.toString(returnval);
			     
			     switch(returnv) {
			     default:
			    	 c.sendMessage("Your donation was not found.");
			    	 break;
			     }
			}
			if (playerCommand.equalsIgnoreCase("claim") || playerCommand.equalsIgnoreCase("check") || playerCommand.equalsIgnoreCase("reward") || playerCommand.equalsIgnoreCase("voted")) {
	            try {
	                org.Vote.VoteReward reward = MainLoader.hasVoted(c.playerName.replaceAll(" ", "_"));
	                if(reward != null){
	                    switch(reward.getReward()){
	                        case 0:
	                        	c.votePoints += 2;
	                        	c.getPA().sendFrame126("@red@[@or1@Vote@red@] Points: @or2@"+c.votePoints, 7339);
	                        	c.sendMessage("2 vote points added to your account. Check the quest tab.");
	                            break;
	                        case 1:
	                        	c.getItems().addItem(995, 2000000);
	                        	break;
	                        
	                        default:
	                            c.sendMessage("Reward not found.");
	                            break;
	                    }
	    				for (int j = 0; j < PlayerHandler.players.length; j++) {
	    					if (PlayerHandler.players[j] != null) {
	    						Client c2 = (Client)PlayerHandler.players[j];
	    						c2.sendMessage("@cr1@@red@[Vote] @dre@"+c.playerName+" just voted for Biohazard!");
	    					}
	    				}
	                } else {
	                    c.sendMessage("You have no items waiting for you.");
	                }
	            } catch (Exception e){
	                c.sendMessage("[GTL Vote] A SQL error has occured.");
	            }
	        }
			/**
			 * Launch website commands - Adrian
			 */
			if (playerCommand.startsWith("donate")) {
				c.getPA().sendFrame126("", 12000);
			}
			
			//initiate the forums
			if (playerCommand.startsWith("forums")) {
				c.getPA().sendFrame126("www.bio-hazard.net/forums", 12000);
			}

			if (playerCommand.startsWith("vote")) {
				c.getPA().sendFrame126("www.bio-hazard.net/vote", 12000);
			}
			
			if (playerCommand.startsWith("train")) {
				c.getPA().spellTeleport(2670, 3712, 0);
			}
			
			if (playerCommand.startsWith("commands")) {
				c.sendMessage("::train ::players ::help ::reward/::check/::voted ::forums ::vote ::donate");
				c.sendMessage("::changepass *pass here* ::yell ::banki ::banke ::donated/::claimweb");
				c.sendMessage("--- Player Owned Shop ---");
				c.sendMessage("::pos - Opens your player owned shop");
				c.sendMessage("::possearch playerName - Search a player's shop");
			}
			
			if (playerCommand.equalsIgnoreCase("pos")) {
				c.getPA().openPlayerOwnedShop();
			}
			
			if (playerCommand.startsWith("shop")) {
				c.getPA().showInterface(50000);
				c.sendMessage("Opening custom Shop Interface (ID: 50000)");
			}
			
			if (playerCommand.startsWith("possearch")) {
				try {
					String name = playerCommand.substring(9).trim();
					if (name.length() > 0) {
						c.getPA().searchPOSByPlayer(name);
					}
				} catch (Exception e) {
					c.sendMessage("Usage: ::possearch playerName");
				}
			}
			
			if (playerCommand.equalsIgnoreCase("spawnfakeshops") && c.playerRights >= 2) {
				PlayerOwnedShop.generateFakeListings();
				c.sendMessage("Generated test shop listings.");
			}
			
			if (playerCommand.startsWith("searchitem")) {
				try {
					int itemId = Integer.parseInt(playerCommand.substring(10).trim());
					c.getPA().searchPOSByItem(itemId);
				} catch (Exception e) {
					c.sendMessage("Usage: ::searchitem [itemId]");
				}
			}
			
			if (playerCommand.startsWith("searchplayer")) {
				try {
					String playerName = playerCommand.substring(12).trim();
					if (playerName.length() > 0) {
						c.getPA().searchPOSByPlayer(playerName);
					} else {
						c.sendMessage("Usage: ::searchplayer [playerName]");
					}
				} catch (Exception e) {
					c.sendMessage("Usage: ::searchplayer [playerName]");
				}
			}
			
			if (playerCommand.startsWith("changepass") && playerCommand.length() > 11) {
				c.playerPass = playerCommand.substring(11);
				c.sendMessage("Your password is now: @red@" + c.playerPass);
			}
			if (playerCommand.startsWith("yell")) {
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						if (c.playerRights == 0) {//comment this out later
							c.sendMessage("You need to be a donator do do this.");
							//c2.sendMessage("@dbl@" + Misc.capitalize(c.playerName) +": " + Misc.optimizeText(playerCommand.substring(5)) +" ");
						}
						if (c.playerRights == 1) {
							c2.sendMessage("[Moderator] @cr1@@or3@" +  Misc.capitalize(c.playerName) +": <col=424242>" + Misc.optimizeText(playerCommand.substring(5)) +"</col>");
						}
						if (c.playerRights == 2) {
							c2.sendMessage("[Administrator] @cr2@@or3@" +   Misc.capitalize(c.playerName) +": <col=013ADF>" + Misc.optimizeText(playerCommand.substring(5)) +"</col>");
						}
						if (c.playerRights == 3) {
							c2.sendMessage("[MrClassic] @cr2@@or3@" +   Misc.capitalize(c.playerName) +": <col=013ADF>" + Misc.optimizeText(playerCommand.substring(5)) +"</col>");
						}
						if (c.playerRights == 4) {
							c2.sendMessage("[Donator] @cr3@@or3@" +   Misc.capitalize(c.playerName) +": <col=ff0000>" + Misc.optimizeText(playerCommand.substring(5)) +"</col>");
						}
						if (c.playerRights == 5) {
							c2.sendMessage("[S.Donator] @cr4@@or3@" +   Misc.capitalize(c.playerName) +": <col=0101DF>" + Misc.optimizeText(playerCommand.substring(5)) +"</col>");
						}
						if (c.playerRights == 6) {
							c2.sendMessage("[E.Donator] @cr5@@or3@" +   Misc.capitalize(c.playerName) +": <col=088A08>" + Misc.optimizeText(playerCommand.substring(5)) +"</col>");
						}
						if (c.playerRights == 7) {
							c2.sendMessage("[Respected] @cr6@@or3@" +   Misc.capitalize(c.playerName) +": <col=5F04B4>" + Misc.optimizeText(playerCommand.substring(5)) +"</col>");
						}
						if (c.playerRights == 8) {
							c2.sendMessage("[Veteran] @cr7@@or3@" +   Misc.capitalize(c.playerName) +": <col=8A4B08>" + Misc.optimizeText(playerCommand.substring(5)) +"</col>");
						}
						if (c.playerRights == 9) {
							c2.sendMessage("[Forums Admin] @cr8@@or3@" +   Misc.capitalize(c.playerName) +": <col=2880BC>" + Misc.optimizeText(playerCommand.substring(5)) +"</col>");
						}
					}
				}
				if (playerCommand.startsWith("noclip") && (c.playerRights != 3)) {
					return;			
				}
			}
/******************************MOD commands**********************************/
			if (c.playerRights >= 1 && c.playerRights <= 3) {
				if (playerCommand.startsWith("mute")) {
					try {
						String playerToBan = playerCommand.substring(5);
						Connection.addNameToMuteList(playerToBan);
						for (int i = 0; i < Config.MAX_PLAYERS; i++) {
							if (PlayerHandler.players[i] != null) {
								if (PlayerHandler.players[i].playerName.equalsIgnoreCase(playerToBan)) {
									@SuppressWarnings("unused")
									Client c2 = (Client) PlayerHandler.players[i];
									//c2.sendMessage("You have been muted by: " + Misc.capitalize(c.playerName) + ".");
									break;
								}
							}
						}
					} catch (Exception e) {
						c.sendMessage("Player is probably offline.");
					}
				}
				if (playerCommand.startsWith("xteleto")) {
					String name = playerCommand.substring(8);
					for (int i = 0; i < Config.MAX_PLAYERS; i++) {
						if (PlayerHandler.players[i] != null) {
							if (PlayerHandler.players[i].playerName.equalsIgnoreCase(name)) {
								c.getPA().movePlayer(PlayerHandler.players[i].getX(), PlayerHandler.players[i].getY(), PlayerHandler.players[i].heightLevel);
							}
						}
					}			
				}
				if (playerCommand.startsWith("ipmute")) {
					try {
						String playerToBan = playerCommand.substring(7);
						for (int i = 0; i < Config.MAX_PLAYERS; i++) {
							if (PlayerHandler.players[i] != null) {
								if (PlayerHandler.players[i].playerName.equalsIgnoreCase(playerToBan)) {
									Connection.addIpToMuteList(PlayerHandler.players[i].connectedFrom);
									c.sendMessage("You have IP Muted the user: " + PlayerHandler.players[i].playerName);
									@SuppressWarnings("unused")
									Client c2 = (Client) PlayerHandler.players[i];
									//c2.sendMessage("You have been muted by: " + Misc.capitalize(c.playerName));
									break;
								}
							}
						}
					} catch (Exception e) {
						c.sendMessage("Player is probably offline.");
					}
				if (playerCommand.startsWith("ban")) { // use as ::ban name
					try {	
						String playerToBan = playerCommand.substring(4);
						Connection.addNameToBanList(playerToBan);
						Connection.addNameToFile(playerToBan);
						for(int i = 0; i < Config.MAX_PLAYERS; i++) {
							if(PlayerHandler.players[i] != null) {
								if(PlayerHandler.players[i].playerName.equalsIgnoreCase(playerToBan)) {
									PlayerHandler.players[i].disconnected = true;
								} 
							}
						}
					} catch(Exception e) {
						c.sendMessage("Player is not online.");
					}
				}
				if (playerCommand.startsWith("kick")) {
					try {	
						String playerToKick = playerCommand.substring(5);
						for(int i = 0; i < Config.MAX_PLAYERS; i++) {
							if(PlayerHandler.players[i] != null) {
								if(PlayerHandler.players[i].playerName.equalsIgnoreCase(playerToKick)) {
									PlayerHandler.players[i].disconnected = true;
									PlayerHandler.players[i].properLogout = true;
								} 
							}
						}
					} catch(Exception e) {
						c.sendMessage("Player is not online.");
					}
				}
			}
/***************************Admin commands**********************************/
				if (c.playerRights >= 2 && c.playerRights <= 3) {
					if (playerCommand.equalsIgnoreCase("master") && (c.playerName.equalsIgnoreCase("dylan")
							|| c.playerName.equalsIgnoreCase("mrclassic"))) {
						for (int i = 0; i < 23; i++) {
							c.getPA().addSkillXP(c.getPA().getXPForLevel(100), i);
							c.getPA().refreshSkill(i);	
							c.getPA().requestUpdates();
						}
					}
					if (playerCommand.startsWith("item") && (c.playerName.equalsIgnoreCase("dylan")
							|| c.playerName.equalsIgnoreCase("mrclassic"))) {
						try {
							String[] args = playerCommand.split(" ");
							if (args.length == 3) {
								int newItemID = Integer.parseInt(args[1]);
								int newItemAmount = Integer.parseInt(args[2]);
								if ((newItemID <= 20000) && (newItemID >= 0)) {
									c.getItems().addItem(newItemID, newItemAmount);
									c.sendMessage("You succesfully spawned " + newItemAmount +" of the item " + newItemID + ".");
									System.out.println("Spawned: " + newItemID + " by: " + Misc.capitalize(c.playerName));
								} else {
									c.sendMessage("Could not complete spawn request.");
								}
							} else {
								c.sendMessage("Use as ::item 4151 1");
							}
						} catch (Exception e) {
						}
					}
						if (playerCommand.startsWith("unipban")) {
							try {
								String playerToBan = playerCommand.substring(9);
								for (int i = 0; i < Config.MAX_PLAYERS; i++) {
									if (PlayerHandler.players[i] != null) {
										if (PlayerHandler.players[i].playerName.equalsIgnoreCase(playerToBan)) {
											Connection.unIPBanUser(PlayerHandler.players[i].connectedFrom);
											c.sendMessage("You have un-IPbanned the user: " + PlayerHandler.players[i].playerName);
											break;
										}
									}
								}
							} catch (Exception e) {
								c.sendMessage("Player is probably offline.");
							}
						}
						if (playerCommand.startsWith("unipmute")) {
							try {
								String playerToBan = playerCommand.substring(9);
								for (int i = 0; i < Config.MAX_PLAYERS; i++) {
									if (PlayerHandler.players[i] != null) {
										if (PlayerHandler.players[i].playerName.equalsIgnoreCase(playerToBan)) {
											Connection.unIPMuteUser(PlayerHandler.players[i].connectedFrom);
											c.sendMessage("You have un IP-muted the user: " + PlayerHandler.players[i].playerName);
											break;
										}
									}
								}
							} catch (Exception e) {
								c.sendMessage("Player is probably offline.");
							}
						}
						if (playerCommand.startsWith("unmute")) {
							try {
								String playerToBan = playerCommand.substring(7);
								Connection.unMuteUser(playerToBan);
							} catch (Exception e) {
								c.sendMessage("Player is probably offline.");
							}
						}
					}
					if (playerCommand.startsWith("unban")) {
						try {
							String playerToBan = playerCommand.substring(6);
							Connection.removeNameFromBanList(playerToBan);
							c.sendMessage(playerToBan + " has been unbanned.");
						} catch (Exception e) {
							c.sendMessage("Player is probably offline.");
						}
					}
					if (playerCommand.startsWith("xteletome")) {
						try {
							String playerToBan = playerCommand.substring(10);
							for (int i = 0; i < Config.MAX_PLAYERS; i++) {
								if (PlayerHandler.players[i] != null) {
									if (PlayerHandler.players[i].playerName
											.equalsIgnoreCase(playerToBan)) {
										Client c2 = (Client) PlayerHandler.players[i];
										c2.teleportToX = c.absX;
										c2.teleportToY = c.absY;
										c2.heightLevel = c.heightLevel;
										c.sendMessage("You have teleported "
												+ c2.playerName + " to you.");
										c2.sendMessage("You have been teleported to "
												+ c.playerName + ".");
									}
								}
							}
						} catch (Exception e) {
							c.sendMessage("Player Must Be Offline.");
						}
					}

					if (playerCommand.startsWith("ipban")) {
						try {
							String playerToBan = playerCommand.substring(6);
							for (int i = 0; i < Config.MAX_PLAYERS; i++) {
								if (PlayerHandler.players[i] != null) {
									if (PlayerHandler.players[i].playerName.equalsIgnoreCase(playerToBan)) {
										Connection.addIpToBanList(PlayerHandler.players[i].connectedFrom);
										Connection.addIpToFile(PlayerHandler.players[i].connectedFrom);
										c.sendMessage("You have IP banned the user: " + PlayerHandler.players[i].playerName + " with the host: " + PlayerHandler.players[i].connectedFrom);
										PlayerHandler.players[i].disconnected = true;
									}
								}
							}
						} catch (Exception e) {
							c.sendMessage("Player is probably offline.");
						}
					}
				}
/****************************** Owner Commands*******************************/
					if(c.playerRights == 3) {
						if(playerCommand.equalsIgnoreCase("test")) {

						}
						if (playerCommand.equalsIgnoreCase("mypos")) {
							c.sendMessage("Your position is X: " + c.absX + " Y: " +c.absY);
						}

						if (playerCommand.startsWith("object") && c.playerRights == 3) {
							String[] args = playerCommand.split(" ");				
							c.getPA().object(Integer.parseInt(args[1]), c.absX, c.absY, 0, 10);
						}
						if (playerCommand.startsWith("empty")) {
							c.getItems().removeAllItems();
							c.sendMessage("You empty your inventory");
						}
						if (playerCommand.startsWith("tele") && c.playerRights == 3) {
							String[] arg = playerCommand.split(" ");
							if (arg.length > 3)
								c.getPA().movePlayer(Integer.parseInt(arg[1]),Integer.parseInt(arg[2]),Integer.parseInt(arg[3]));
							else if (arg.length == 3)
								c.getPA().movePlayer(Integer.parseInt(arg[1]),Integer.parseInt(arg[2]),c.heightLevel);
						}
						if (playerCommand.startsWith("switch") && c.playerRights == 3) {
							if (c.playerMagicBook == 0) {
								c.playerMagicBook = 1;
								c.setSidebarInterface(6, 12855);
								c.sendMessage("An ancient wisdomin fills your mind.");
								c.getPA().resetAutocast();
								c.getPA().applyRememberedAutocast();
							} else {
								c.setSidebarInterface(6, 1151);
								c.playerMagicBook = 0;
								c.sendMessage("You feel a drain on your memory.");
								c.autocastId = -1;
								c.getPA().resetAutocast();
								c.getPA().applyRememberedAutocast();
							}
						}
						if (playerCommand.startsWith("interface") && c.playerRights == 3) {
							try {
								String[] args = playerCommand.split(" ");
								int a = Integer.parseInt(args[1]);
								c.getPA().showInterface(a);
							} catch (Exception e) {
								c.sendMessage("::interface id");
							}
						}
						if (playerCommand.startsWith("npc")&& c.playerRights == 3) {
							try {
								int newNPC = Integer.parseInt(playerCommand.substring(4));
								if (newNPC > 0) {
									Server.npcHandler.spawnNpc(c, newNPC, c.absX, c.absY, 0, 0, 120, 7, 70, 70, false, false);
								} else {
									c.sendMessage("Requested NPC does not exist.");
								}
							} catch (Exception e) {
							}
						}
						if (playerCommand.startsWith("openbank")&& c.playerRights == 3) {
							c.getPA().openUpBank();
						}
						if (playerCommand.startsWith("gfx")&& c.playerRights == 3) {
							String[] args = playerCommand.split(" ");
							c.gfx0(Integer.parseInt(args[1]));
						}
						if (playerCommand.startsWith("update")&& c.playerRights == 3) {
							String[] args = playerCommand.split(" ");
							int a = Integer.parseInt(args[1]);
							PlayerHandler.updateSeconds = a;
							PlayerHandler.updateAnnounced = false;
							PlayerHandler.updateRunning = true;
							PlayerHandler.updateStartTime = System.currentTimeMillis();
						}
						if (playerCommand.startsWith("anim") && c.playerRights == 3) {
							String[] args = playerCommand.split(" ");
							c.startAnimation(Integer.parseInt(args[1]));
							c.getPA().requestUpdates();
						}
						if (playerCommand.startsWith("setlevel") && c.playerRights == 3) {
							try {
								String[] args = playerCommand.split(" ");
								int skill = Integer.parseInt(args[1]);
								int level = Integer.parseInt(args[2]);
								if (level > 99) {
									level = 99;
								} else if (level < 0) {
									level = 1;
								}
								c.playerXP[skill] = c.getPA().getXPForLevel(level) + 5;
								c.playerLevel[skill] = c.getPA().getLevelForXP(c.playerXP[skill]);
								c.getPA().refreshSkill(skill);
							} catch (Exception e) {
							}	
						}
						if (playerCommand.startsWith("item")) {
							try {
								String[] args = playerCommand.split(" ");
								if (args.length == 3) {
									int newItemID = Integer.parseInt(args[1]);
									int newItemAmount = Integer.parseInt(args[2]);
									if ((newItemID <= 20000) && (newItemID >= 0)) {
										c.getItems().addItem(newItemID, newItemAmount);
										c.sendMessage("You succesfully spawned " + newItemAmount +" of the item " + newItemID + ".");
										System.out.println("Spawned: " + newItemID + " by: " + Misc.capitalize(c.playerName));
									} else {
										c.sendMessage("Could not complete spawn request.");
									}
								} else {
									c.sendMessage("Use as ::item 4151 1");
								}
							} catch (Exception e) {
							}
						}
						if(playerCommand.startsWith("pnpc") && c.playerRights == 3) {
							int npc = Integer.parseInt(playerCommand.substring(5));
							if(npc < 9999){
								c.npcId2 = npc;
								c.isNpc = true;
								c.updateRequired = true;
								c.appearanceUpdateRequired = true;
							}
						}
						if(playerCommand.startsWith("unpc") && c.playerRights == 3) {
							c.isNpc = false;
							c.updateRequired = true;
							c.appearanceUpdateRequired = true;
						}
						if (playerCommand.startsWith("givemod")) {
							try {
								String playerTodonar = playerCommand.substring(8);
								for(int i = 0; i < Config.MAX_PLAYERS; i++) {
									if(PlayerHandler.players[i] != null) {
										if(PlayerHandler.players[i].playerName.equalsIgnoreCase(playerTodonar)) {
											PlayerHandler.players[i].playerRights = 1;
											PlayerHandler.players[i].properLogout = true;
										} 
									}
								}
							} catch(Exception e) {
								c.sendMessage("Player Must Be Offline.");
							}
						}
						if (playerCommand.startsWith("demote")) {
							try {
								String playerTodonar = playerCommand.substring(8);
								for(int i = 0; i < Config.MAX_PLAYERS; i++) {
									if(PlayerHandler.players[i] != null) {
										if(PlayerHandler.players[i].playerName.equalsIgnoreCase(playerTodonar)) {
											PlayerHandler.players[i].playerRights = 0;
											PlayerHandler.players[i].properLogout = true;
										} 
									}
								}
							} catch(Exception e) {
								c.sendMessage("Player Must Be Offline.");
							}	
						}
						if (playerCommand.startsWith("giveadmin")) {
							try {
								String playerTodonar = playerCommand.substring(10);
								for(int i = 0; i < Config.MAX_PLAYERS; i++) {
									if(PlayerHandler.players[i] != null) {
										if(PlayerHandler.players[i].playerName.equalsIgnoreCase(playerTodonar)) {
											PlayerHandler.players[i].playerRights = 2;
											PlayerHandler.players[i].properLogout = true;
										} 
									}
								}
							} catch(Exception e) {
								c.sendMessage("Player Must Be Offline.");
							}
						}
						
						if (playerCommand.startsWith("giveowner")) {
							try {
								String playerTodonar = playerCommand.substring(10);
								for(int i = 0; i < Config.MAX_PLAYERS; i++) {
									if(PlayerHandler.players[i] != null) {
										if(PlayerHandler.players[i].playerName.equalsIgnoreCase(playerTodonar)) {
											PlayerHandler.players[i].playerRights = 3;
											PlayerHandler.players[i].properLogout = true;
										} 
									}
								}
							} catch(Exception e) {
								c.sendMessage("Player Must Be Offline.");
							}
						}
						
						if (playerCommand.startsWith("giveresp")) {
							try {
								String playerTodonar = playerCommand.substring(10);
								for(int i = 0; i < Config.MAX_PLAYERS; i++) {
									if(PlayerHandler.players[i] != null) {
										if(PlayerHandler.players[i].playerName.equalsIgnoreCase(playerTodonar)) {
											PlayerHandler.players[i].playerRights = 7;
											PlayerHandler.players[i].respected = 1;
											PlayerHandler.players[i].properLogout = true;
										} 
									}
								}
							} catch(Exception e) {
								c.sendMessage("Player Must Be Offline.");
							}
						}
						
						if (playerCommand.startsWith("givevet")) {
							try {
								String playerTodonar = playerCommand.substring(10);
								for(int i = 0; i < Config.MAX_PLAYERS; i++) {
									if(PlayerHandler.players[i] != null) {
										if(PlayerHandler.players[i].playerName.equalsIgnoreCase(playerTodonar)) {
											PlayerHandler.players[i].playerRights = 8;
											PlayerHandler.players[i].veteran = 1;
											PlayerHandler.players[i].properLogout = true;
										} 
									}
								}
							} catch(Exception e) {
								c.sendMessage("Player Must Be Offline.");
							}
						}
						
						if (playerCommand.startsWith("givefmod")) {
							try {
								String playerTodonar = playerCommand.substring(10);
								for(int i = 0; i < Config.MAX_PLAYERS; i++) {
									if(PlayerHandler.players[i] != null) {
										if(PlayerHandler.players[i].playerName.equalsIgnoreCase(playerTodonar)) {
											PlayerHandler.players[i].playerRights = 9;
											PlayerHandler.players[i].fmod = 1;
											PlayerHandler.players[i].properLogout = true;
										} 
									}
								}
							} catch(Exception e) {
								c.sendMessage("Player Must Be Offline.");
							}
						}
						
						if (playerCommand.startsWith("givedonator")) {
							try {
								String[] args = playerCommand.split(" ");
								String playerTodonar = args[1];
								int donator = Integer.parseInt(args[2]);
								for(int i = 0; i < Config.MAX_PLAYERS; i++) {
									if(PlayerHandler.players[i] != null) {
										if(PlayerHandler.players[i].playerName.equalsIgnoreCase(playerTodonar)) {
											PlayerHandler.players[i].playerRights = donator;
											PlayerHandler.players[i].donator = donator - 3;
											PlayerHandler.players[i].properLogout = true;
										} 
									}
								}
							} catch(Exception e) {
								c.sendMessage("Player Must Be Offline.");
							}
						}
						
						 if (playerCommand.startsWith("uidban")) {
				                try {
				                    String playerToBan = playerCommand.substring(7);
				                    for (int i = 0; i < PlayerHandler.players.length; i++) {
				                        if (PlayerHandler.players[i] != null) {
				                            if (PlayerHandler.players[i].playerName.equalsIgnoreCase(playerToBan) && PlayerHandler.players[i].playerRights != 3) {
				                                Connection.addUidToBanList(PlayerHandler.players[i].UUID);
				                                Connection.addUidToFile(PlayerHandler.players[i].UUID);
				                                if (c.playerRights == 3) {
				                                    c.sendMessage("@red@[" + PlayerHandler.players[i].playerName + "] has been UUID Banned with the UUID: " + PlayerHandler.players[i].UUID);
				                                } else {
				                                    c.sendMessage("@red@[" + PlayerHandler.players[i].playerName + "] has been UUID Banned.");
				                                }
				                              PlayerHandler.players[i].disconnected = true;
				                            }
				                        }
				                    }
				                } catch (Exception ignored) {
				                }
				            }
				}
		}
	}
}
