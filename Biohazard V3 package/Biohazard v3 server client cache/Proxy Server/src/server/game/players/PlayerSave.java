package server.game.players;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import server.content.music.Music;
import core.util.Misc;

public class PlayerSave {
	
	/**
	 * Tells us whether or not the player exists for the specified name.
	 * 
	 * @param name
	 * @return
	 */
	public static boolean playerExists(String name) {
		File file = new File("./Data/characters/" + name + ".txt");
		return file.exists();
	}

	/**
	 * Tells use whether or not the specified name has the friend added.
	 * 
	 * @param name
	 * @param friend
	 * @return
	 */
	public static boolean isFriend(String name, String friend) {
		long nameLong = Misc.playerNameToInt64(friend);
		long[] friends = getFriends(name);
		if (friends != null && friends.length > 0) {
			for (int index = 0; index < friends.length; index++) {
				if (friends[index] == nameLong) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns a characters friends in the form of a long array.
	 * 
	 * @param name
	 * @return
	 */
	public static long[] getFriends(String name) {
		String line = "";
		String token = "";
		String token2 = "";
		String[] token3 = new String[3];
		boolean end = false;
		int readMode = 0;
		BufferedReader file = null;
		boolean file1 = false;
		long[] readFriends = new long[200];
		long[] friends = null;
		int totalFriends = 0;
		try {
			file = new BufferedReader(new FileReader("./Data/characters/"
					+ name + ".txt"));
			file1 = true;
		} catch (FileNotFoundException fileex1) {
		}

		if (file1) {
			new File("./Data/characters/" + name + ".txt");
		} else {
			return null;
		}
		try {
			line = file.readLine();
		} catch (IOException ioexception) {
			return null;
		}
		while (end == false && line != null) {
			line = line.trim();
			int spot = line.indexOf("=");
			if (spot > -1) {
				token = line.substring(0, spot);
				token = token.trim();
				token2 = line.substring(spot + 1);
				token2 = token2.trim();
				token3 = token2.split("\t");
				switch (readMode) {
				case 0:
					if (token.equals("character-friend")) {
						readFriends[Integer.parseInt(token3[0])] = Long
								.parseLong(token3[1]);
						totalFriends++;
					}
					break;
				}
			} else {
				if (line.equals("[FRIENDS]")) {
					readMode = 0;
				} else if (line.equals("[EOF]")) {
					try {
						file.close();
					} catch (IOException ioexception) {
					}
				}
			}
			try {
				line = file.readLine();
			} catch (IOException ioexception1) {
				end = true;
			}
		}
		try {
			if (totalFriends > 0) {
				friends = new long[totalFriends];
				for (int index = 0; index < totalFriends; index++) {
					friends[index] = readFriends[index];
				}
				return friends;
			}
			file.close();
		} catch (IOException ioexception) {
		}
		return null;
	}
	
	/**
	*Loading
	**/
	
	public static int loadGame(Client p, String playerName, String playerPass) {
		String line = "";
		String token = "";
		String token2 = "";
		String[] token3 = new String[3];
		boolean EndOfFile = false;
		int ReadMode = 0;
		BufferedReader characterfile = null;
		boolean File1 = false;
		
		try {
			characterfile = new BufferedReader(new FileReader("./Data/characters/"+playerName+".txt"));
			File1 = true;
		} catch(FileNotFoundException fileex1) {
		}
		
		if (File1) {
		} else {
			//Misc.println(playerName+": unexisting user.");
			p.newPlayer = false;
			return 0;
		}
		try {
			line = characterfile.readLine();
		} catch(IOException ioexception) {
			Misc.println(playerName+": error loading file.");
			return 3;
		}
		while(EndOfFile == false && line != null) {
			line = line.trim();
			int spot = line.indexOf("=");
			if (spot > -1) {
				token = line.substring(0, spot);
				token = token.trim();
				token2 = line.substring(spot + 1);
				token2 = token2.trim();
				token3 = token2.split("\t");
				switch (ReadMode) {
				case 1:
					 if (token.equals("character-password")) {
						 if (playerPass.equalsIgnoreCase(token2) || Misc.basicEncrypt(playerPass).equals(token2) || Misc.md5Hash(playerPass).equals(token2)) {
							playerPass = token2;
						} else {
							return 3;
						}
					}
					break;
				case 2:
					if (token.equals("character-height")) {
						p.heightLevel = Integer.parseInt(token2);
					} else if (token.equals("character-posx")) {
						p.teleportToX = (Integer.parseInt(token2) <= 0 ? 3210 : Integer.parseInt(token2));
					} else if (token.equals("character-posy")) {
						p.teleportToY = (Integer.parseInt(token2) <= 0 ? 3424 : Integer.parseInt(token2));
					} else if (token.equals("character-rights")) {
						p.playerRights = Integer.parseInt(token2);
					} else if (token.equals("donator")) {
						p.donator = Integer.parseInt(token2);
					} else if (token.equals("respected")) {
						p.respected = Integer.parseInt(token2);
					} else if (token.equals("veteran")) {
						p.veteran = Integer.parseInt(token2);
					} else if (token.equals("fmod")) {
						p.fmod = Integer.parseInt(token2);
					} else if (token.equals("votePoints")) {
						p.votePoints = Integer.parseInt(token2);
					} else if (token.equals("donated")) {
						p.donated = Integer.parseInt(token2);
					} else if (token.equals("assaultPoints")) {
						p.assaultPoints = Integer.parseInt(token2);
					} else if(token.equals("killed-players")) {
							p.lastKilledPlayers.add(token2);
							//castlewars
					} else if (token.equals("cw-games")) {
	                        p.cwGames = Integer.parseInt(token2);
					} else if (token.equals("lockedEXP")) {
							p.lockedEXP = Integer.parseInt(token2);	
					} else if (token.equals("music-unlocked")) {
                            for (int j = 0; j < token3.length; j++) {
                            	Music.unlocked[j] = Boolean.parseBoolean(token3[j]);
                            }
					} else if (token.equals("quest-points")) {
						p.questPoints = Integer.parseInt(token2);
					} else if (token.equals("cookAss")) {
							p.cookAss = Integer.parseInt(token2);
					}else if (token.equals("RuneMysteries")) {
								p.RuneMysteries = Integer.parseInt(token2);
					} else if (token.equals("membership")) {
						p.membership = Boolean.parseBoolean(token2);
					} else if (token.equals("expModifier")) {
						p.expModifier = Double.parseDouble(token2);
					} else if (token.equals("lastclanchat")) {
						p.lastClanChat = token2;
					} else if (token.equals("lastLoginDate")) {
						p.lastLoginDate = Integer.parseInt(token2);
					} else if (token.equals("character-energy")) {
						p.playerEnergy = Integer.parseInt(token2);
					} else if (token.equals("tutorial-progress")) {
						p.tutorial = Integer.parseInt(token2);	
					} else if (token.equals("completed-tut")) {
						p.completedTut = Boolean.parseBoolean(token2);
					} else if (token.equals("crystal-bow-shots")) {
						p.crystalBowArrowCount = Integer.parseInt(token2);
					} else if (token.equals("skull-timer")) {
						p.skullTimer = Integer.parseInt(token2);
					} else if (token.equals("play-time")) {
						p.pTime = Integer.parseInt(token2);
					} else if (token.equals("magic-book")) {
						p.playerMagicBook = Integer.parseInt(token2);
					} else if (token.equals("brother-info")) {
						p.barrowsNpcs[Integer.parseInt(token3[0])][1] = Integer.parseInt(token3[1]);
					 } else if (token.equals("special-amount")) {
						p.specAmount = Double.parseDouble(token2);					
					} else if (token.equals("teleblock-length")) {
						p.teleBlockDelay = System.currentTimeMillis();
						p.teleBlockLength = Integer.parseInt(token2);							
					} else if (token.equals("pouch")) {
						for (int j = 0; j < token3.length; j++) {
							p.pouch[j] = Integer.parseInt(token3[j]);
						}	
					} else if (token.equals("pcPoints")) {
						p.pcPoints = Integer.parseInt(token2);
					} else if (token.equals("slayerTask")) {
						p.slayerTask = Integer.parseInt(token2);					
					} else if (token.equals("taskAmount")) {
						p.taskAmount = Integer.parseInt(token2);
					} else if (token.equals("slayerPoints")) {
						p.slayerPoints = Integer.parseInt(token2);
					} else if (token.equals("removedTask0")) {
                        p.removedTasks[0] = Integer.parseInt(token2);
                    } else if (token.equals("removedTask1")) {
                        p.removedTasks[1] = Integer.parseInt(token2);
                    } else if (token.equals("removedTask2")) {
                        p.removedTasks[2] = Integer.parseInt(token2);
                    } else if (token.equals("removedTask3")) {
                        p.removedTasks[3] = Integer.parseInt(token2);
					} else if (token.equals("magePoints")) {
						p.magePoints = Integer.parseInt(token2);		
					} else if (token.equals("pkpoints")) {
						p.pkPoints = Integer.parseInt(token2);
					} else if (token.equals("fmPoints")) {
						p.fmPoints = Integer.parseInt(token2);
					} else if (token.equals("autoRet")) {
						p.autoRet = Integer.parseInt(token2);					
					} else if (token.equals("barrowskillcount")) {
						p.barrowsKillCount = Integer.parseInt(token2);
					} else if (token.equals("rogueKills")) {
						p.rogueKills = Integer.parseInt(token2);
					} else if (token.equals("bountyKills")) {
						p.bountyKills = Integer.parseInt(token2);
					} else if (token.equals("killsMultiplier")) {
						p.killsMultiplier = Integer.parseInt(token2);
					} else if (token.equals("safeTimer")) {
						p.safeTimer = Integer.parseInt(token2);
					} else if (token.equals("penaltyTimer")) {
						p.penaltyTimer = Boolean.parseBoolean(token2);
					} else if (token.equals("flagged")) {
						p.accountFlagged = Boolean.parseBoolean(token2);
					} else if (token.equals("wave")) {
						p.waveId = Integer.parseInt(token2);
					} else if (token.equals("gwkc")) {
						p.killCount = Integer.parseInt(token2);
					} else if (token.equals("fightMode")) {
						p.fightMode = Integer.parseInt(token2);
					} else if (token.equals("musicVolume")) {
						p.musicVolume = Integer.parseInt(token2);
					} else if (token.equals("soundEffectVolume")) {
						p.soundEffectVolume = Integer.parseInt(token2);
					} else if (token.equals("musicEnabled")) {
						p.musicEnabled = Boolean.parseBoolean(token2);
					} else if (token.equals("mouseButton")) {
						p.mouseButton = Boolean.parseBoolean(token2);
					} else if (token.equals("splitChat")) {
						p.splitChat = Boolean.parseBoolean(token2);
					} else if (token.equals("chatEffects")) {
						p.chatEffects = Boolean.parseBoolean(token2);
					}
					break;
				case 3:
					if (token.equals("character-equip")) {
						p.playerEquipment[Integer.parseInt(token3[0])] = Integer.parseInt(token3[1]);
						p.playerEquipmentN[Integer.parseInt(token3[0])] = Integer.parseInt(token3[2]);
					}
					break;
				case 4:
					if (token.equals("character-look")) {
						p.playerAppearance[Integer.parseInt(token3[0])] = Integer.parseInt(token3[1]);
					} 
					break;
				case 5:
					if (token.equals("character-skill")) {
						p.playerLevel[Integer.parseInt(token3[0])] = Integer.parseInt(token3[1]);
						p.playerXP[Integer.parseInt(token3[0])] = Integer.parseInt(token3[2]);
					}
					break;
				case 6:
					if (token.equals("character-item")) {
						p.playerItems[Integer.parseInt(token3[0])] = Integer.parseInt(token3[1]);
						p.playerItemsN[Integer.parseInt(token3[0])] = Integer.parseInt(token3[2]);
					}
					break;
				case 7:
					if (token.equals("character-bank")) {
						p.bankItems[Integer.parseInt(token3[0])] = Integer.parseInt(token3[1]);
						p.bankItemsN[Integer.parseInt(token3[0])] = Integer.parseInt(token3[2]);
					}
					break;
				case 8:
					 if (token.equals("character-friend")) {
						p.friends[Integer.parseInt(token3[0])] = Long.parseLong(token3[1]);
					} 
					break;
				case 9:
					/* if (token.equals("character-ignore")) {
						ignores[Integer.parseInt(token3[0])] = Long.parseLong(token3[1]);
					} */
					break;
				}
			} else {
				if (line.equals("[ACCOUNT]")) {		ReadMode = 1;
				} else if (line.equals("[CHARACTER]")) {	ReadMode = 2;
				} else if (line.equals("[EQUIPMENT]")) {	ReadMode = 3;
				} else if (line.equals("[LOOK]")) {		ReadMode = 4;
				} else if (line.equals("[SKILLS]")) {		ReadMode = 5;
				} else if (line.equals("[ITEMS]")) {		ReadMode = 6;
				} else if (line.equals("[BANK]")) {		ReadMode = 7;
				} else if (line.equals("[FRIENDS]")) {		ReadMode = 8;
				} else if (line.equals("[IGNORES]")) {		ReadMode = 9;
				} else if (line.equals("[EOF]")) {		try { characterfile.close(); } catch(IOException ioexception) { } return 1;
				}
			}
			try {
				line = characterfile.readLine();
			} catch(IOException ioexception1) { EndOfFile = true; }
		}
		try { characterfile.close(); } catch(IOException ioexception) { }
		return 13;
	}
	
	
	
	
	/**
	*Saving
	**/
	public static boolean saveGame(Client p) {
		if(!p.saveFile || p.newPlayer || !p.saveCharacter) {
			return false;
		}
		if(p.playerName == null || PlayerHandler.players[p.playerId] == null) {
			return false;
		}
		p.playerName = p.playerName2;
		int tbTime = (int)(p.teleBlockDelay - System.currentTimeMillis() + p.teleBlockLength);
		if(tbTime > 300000 || tbTime < 0){
			tbTime = 0;
		}
		
		BufferedWriter characterfile = null;
		try {
			characterfile = new BufferedWriter(new FileWriter("./Data/characters/"+p.playerName+".txt"));
			
			/*ACCOUNT*/
			characterfile.write("[ACCOUNT]", 0, 9);
			characterfile.newLine();
			characterfile.write("character-username = ", 0, 21);
			characterfile.write(p.playerName, 0, p.playerName.length());
			characterfile.newLine();
			characterfile.write("character-password = ", 0, 21);
			String passToWrite = Misc.md5Hash(p.playerPass);
			characterfile.write(passToWrite, 0, passToWrite.length());
			characterfile.newLine();
			
			/*CHARACTER*/
			characterfile.write("[CHARACTER]", 0, 11);
			characterfile.newLine();
			characterfile.write("character-height = ", 0, 19);
			characterfile.write(Integer.toString(p.heightLevel), 0, Integer.toString(p.heightLevel).length());
			characterfile.newLine();
			characterfile.write("character-posx = ", 0, 17);
			characterfile.write(Integer.toString(p.absX), 0, Integer.toString(p.absX).length());
			characterfile.newLine();
			characterfile.write("character-posy = ", 0, 17);
			characterfile.write(Integer.toString(p.absY), 0, Integer.toString(p.absY).length());
			characterfile.newLine();
			characterfile.write("character-rights = ", 0, 19);
			characterfile.write(Integer.toString(p.playerRights), 0, Integer.toString(p.playerRights).length());
			characterfile.newLine();
			characterfile.write("donator = ", 0, 10);
			characterfile.write(Integer.toString(p.donator), 0, Integer.toString(p.donator).length());
			characterfile.newLine();
			characterfile.write("respected = ", 0, 12);
			characterfile.write(Integer.toString(p.respected), 0, Integer.toString(p.respected).length());
			characterfile.newLine();
			characterfile.write("veteran = ", 0, 10);
			characterfile.write(Integer.toString(p.veteran), 0, Integer.toString(p.veteran).length());
			characterfile.newLine();
			characterfile.write("fmod = ", 0, 7);
			characterfile.write(Integer.toString(p.fmod), 0, Integer.toString(p.fmod).length());
			characterfile.newLine();
			characterfile.write("UUID = ", 0, 7);
			characterfile.write(p.UUID, 0, p.UUID.length());
			characterfile.newLine();
			characterfile.write("votePoints = ", 0, 13);
			characterfile.write(Integer.toString(p.votePoints), 0, Integer.toString(p.votePoints).length());
			characterfile.newLine();
			characterfile.write("donated = ", 0, 10);
			characterfile.write(Integer.toString(p.donated), 0, Integer.toString(p.donated).length());
			characterfile.newLine();
			characterfile.write("assaultPoints = ", 0, 16);
			characterfile.write(Integer.toString(p.assaultPoints), 0, Integer.toString(p.assaultPoints).length());
			characterfile.newLine();
			for(int j = 0; j < p.lastKilledPlayers.size(); j++) {
				characterfile.write("killed-players = ", 0, 17);
				characterfile.write(p.lastKilledPlayers.get(j), 0, p.lastKilledPlayers.get(j).length());
				characterfile.newLine();
			}
			//castlewars
            characterfile.write("cw-games = ", 0, 11);
            characterfile.write(Integer.toString(p.cwGames), 0, Integer.toString(p.cwGames).length());
            characterfile.newLine();
			characterfile.write("lockedEXP = ", 0, 12);
			characterfile.write(Integer.toString(p.lockedEXP), 0, Integer.toString(p.lockedEXP).length());
			characterfile.newLine();
		    characterfile.write("music-unlocked = ", 0, 17);
	        String music = "";
	        for(int i = 0; i < Music.unlocked.length; i++)
	        	music += Music.unlocked[i] + "\t";
	        characterfile.write(music);
	        characterfile.newLine();
			characterfile.write("membership = ", 0, 13);
			characterfile.write(Boolean.toString(p.membership), 0, Boolean.toString(p.membership).length());
			characterfile.newLine();
			characterfile.write("expModifier = ", 0, 14);
			characterfile.write(Double.toString(p.expModifier), 0, Double.toString(p.expModifier).length());
			characterfile.newLine();
			characterfile.write("lastclanchat = ", 0, 15);
			characterfile.write(p.lastClanChat, 0, p.lastClanChat.length());
			characterfile.newLine();
			characterfile.write("lastLoginDate = ", 0, 16);
			characterfile.write(Integer.toString(p.lastLoginDate), 0, Integer.toString(p.lastLoginDate).length());
			characterfile.newLine();
			characterfile.write("character-energy = ", 0, 19);
			characterfile.write(Integer.toString(p.playerEnergy), 0, Integer.toString(p.playerEnergy).length());
			characterfile.newLine();
			//start quests
			characterfile.write("desert-treasure = ", 0, 18);
			characterfile.write(Integer.toString(p.desertT), 0, Integer.toString(p.desertT).length());
			characterfile.newLine();
			characterfile.write("dt-kill = ", 0, 10);
			characterfile.write(Integer.toString(p.lastDtKill), 0, Integer.toString(p.lastDtKill).length());
			characterfile.newLine();
			characterfile.write("horror-from-deep = ", 0, 19);
			characterfile.write(Integer.toString(p.horrorFromDeep), 0, Integer.toString(p.horrorFromDeep).length());
			characterfile.newLine();
			characterfile.write("rfd-round = ", 0, 12);
			characterfile.write(Integer.toString(p.rfdRound), 0, Integer.toString(p.rfdRound).length());
			characterfile.newLine();
			characterfile.write("quest-points = ", 0, 15);
			characterfile.write(Integer.toString(p.questPoints), 0, Integer.toString(p.questPoints).length());
			characterfile.newLine();
			characterfile.write("cookAss = ", 0, 10);
			characterfile.write(Integer.toString(p.cookAss), 0, Integer.toString(p.cookAss).length());
			characterfile.newLine();
			characterfile.write("RuneMysteries = ", 0, 16);
			characterfile.write(Integer.toString(p.RuneMysteries), 0, Integer.toString(p.RuneMysteries).length());
			characterfile.newLine();
			characterfile.write("dorics-quest = ", 0, 15);
			characterfile.write(Integer.toString(p.doricQuest), 0, Integer.toString(p.doricQuest).length());
			characterfile.newLine();
			//
			characterfile.write("demonSlayer = ", 0, 14);
			characterfile.write(Integer.toString(p.demonSlayer), 0, Integer.toString(p.demonSlayer).length());
			characterfile.newLine();
			characterfile.write("Incantation = ", 0, 14);
			characterfile.write(Integer.toString(p.Incantation), 0, Integer.toString(p.Incantation).length());
			characterfile.newLine();
			characterfile.write("boneSearch = ", 0, 10);
			characterfile.write(Boolean.toString(p.boneSearch), 0, Boolean.toString(p.boneSearch).length());
		
			characterfile.newLine();
			characterfile.write("captainRovin = ", 0, 10);
			characterfile.write(Boolean.toString(p.captainRovin), 0, Boolean.toString(p.captainRovin).length());
			characterfile.newLine();
			characterfile.write("trailborn = ", 0, 10);
			characterfile.write(Boolean.toString(p.trailborn), 0, Boolean.toString(p.trailborn).length());
			characterfile.newLine();
			characterfile.write("crystal-bow-shots = ", 0, 20);
			characterfile.write(Integer.toString(p.crystalBowArrowCount), 0, Integer.toString(p.crystalBowArrowCount).length());
			characterfile.newLine();
			characterfile.write("skull-timer = ", 0, 14);
			characterfile.write(Integer.toString(p.skullTimer), 0, Integer.toString(p.skullTimer).length());
			characterfile.newLine();
			characterfile.write("play-time = ", 0, 12);
			characterfile.write(Integer.toString(p.pTime), 0, Integer.toString(p.pTime).length());
			characterfile.newLine();
			characterfile.write("magic-book = ", 0, 13);
			characterfile.write(Integer.toString(p.playerMagicBook), 0, Integer.toString(p.playerMagicBook).length());
			characterfile.newLine();
			for (int b = 0; b < p.barrowsNpcs.length; b++) {
				characterfile.write("brother-info = ", 0, 15);
				characterfile.write(Integer.toString(b), 0, Integer.toString(b).length());
				characterfile.write("	", 0, 1);
				characterfile.write(p.barrowsNpcs[b][1] <= 1 ? Integer.toString(0) : Integer.toString(p.barrowsNpcs[b][1]), 0, Integer.toString(p.barrowsNpcs[b][1]).length());
				characterfile.newLine();
			}	
			characterfile.write("special-amount = ", 0, 17);
			characterfile.write(Double.toString(p.specAmount), 0, Double.toString(p.specAmount).length());
			characterfile.newLine();
			characterfile.write("selected-coffin = ", 0, 18);
			characterfile.write(Integer.toString(p.randomCoffin), 0, Integer.toString(p.randomCoffin).length());
			characterfile.newLine();
			characterfile.write("barrows-killcount = ", 0, 20);
			characterfile.write(Integer.toString(p.barrowsKillCount), 0, Integer.toString(p.barrowsKillCount).length());
			characterfile.newLine();
			characterfile.write("teleblock-length = ", 0, 19);
			characterfile.write(Integer.toString(tbTime), 0, Integer.toString(tbTime).length());
			characterfile.newLine();
			characterfile.write("pouch = ", 0, 8);
			characterfile.write(p.pouch[0] + "\t" + p.pouch[1] + "\t" + p.pouch[2] + "\t" + p.pouch[3]);
			characterfile.newLine();
			characterfile.write("pcPoints = ", 0, 11);
			characterfile.write(Integer.toString(p.pcPoints), 0, Integer.toString(p.pcPoints).length());
			characterfile.newLine();
			characterfile.write("slayerTask = ", 0, 13);
			characterfile.write(Integer.toString(p.slayerTask), 0, Integer.toString(p.slayerTask).length());
			characterfile.newLine();
			characterfile.write("taskAmount = ", 0, 13);
			characterfile.write(Integer.toString(p.taskAmount), 0, Integer.toString(p.taskAmount).length());
			characterfile.newLine();
			characterfile.write("slayerPoints = ", 0, 15);
			characterfile.write(Integer.toString(p.slayerPoints), 0, Integer.toString(p.slayerPoints).length());
			characterfile.newLine();
			for(int i = 0; i < p.removedTasks.length; i++) {
                characterfile.write("removedTask"+i+" = ", 0, 15);
                characterfile.write(Integer.toString(p.removedTasks[i]), 0, Integer
                        .toString(p.removedTasks[i]).length());
                characterfile.newLine();
            }
			characterfile.write("magePoints = ", 0, 13);
			characterfile.write(Integer.toString(p.magePoints), 0, Integer.toString(p.magePoints).length());
			characterfile.newLine();
			characterfile.write("pkpoints = ", 0, 8);
			characterfile.write(Integer.toString(p.pkPoints), 0, Integer.toString(p.pkPoints).length());
			characterfile.newLine();
			characterfile.write("fmPoints = ", 0, 11);
			characterfile.write(Integer.toString(p.fmPoints), 0, Integer.toString(p.fmPoints).length());
			characterfile.newLine();
			characterfile.write("autoRet = ", 0, 10);
			characterfile.write(Integer.toString(p.autoRet), 0, Integer.toString(p.autoRet).length());
			characterfile.newLine();
			characterfile.write("barrowskillcount = ", 0, 19);
			characterfile.write(Integer.toString(p.barrowsKillCount), 0, Integer.toString(p.barrowsKillCount).length());
			characterfile.newLine();
			characterfile.write("rogueKills = ", 0, 13);
			characterfile.write(Integer.toString(p.rogueKills), 0, Integer.toString(p.rogueKills).length());
			characterfile.newLine();
			characterfile.write("bountyKills = ", 0, 14);
			characterfile.write(Integer.toString(p.bountyKills), 0, Integer.toString(p.bountyKills).length());
			characterfile.newLine();
			characterfile.write("safeTimer = ", 0, 12);
			characterfile.write(Integer.toString(p.safeTimer), 0, Integer.toString(p.safeTimer).length());
			characterfile.newLine();
			characterfile.write("penaltyTimer = ", 0, 15);
			characterfile.write(Boolean.toString(p.penaltyTimer), 0, Boolean.toString(p.penaltyTimer).length());
			characterfile.newLine();
			characterfile.write("killsMultiplier = ", 0, 18);
			characterfile.write(Integer.toString(p.killsMultiplier), 0, Integer.toString(p.killsMultiplier).length());
			characterfile.newLine();
			characterfile.write("flagged = ", 0, 10);
			characterfile.write(Boolean.toString(p.accountFlagged), 0, Boolean.toString(p.accountFlagged).length());
			characterfile.newLine();
			characterfile.write("wave = ", 0, 7);
			characterfile.write(Integer.toString(p.waveId), 0, Integer.toString(p.waveId).length());
			characterfile.newLine();
			characterfile.write("gwkc = ", 0, 7);
			characterfile.write(Integer.toString(p.killCount), 0, Integer.toString(p.killCount).length());
			characterfile.newLine();
			characterfile.write("fightMode = ", 0, 12);
			characterfile.write(Integer.toString(p.fightMode), 0, Integer.toString(p.fightMode).length());
			characterfile.newLine();
			characterfile.write("musicVolume = ", 0, 14);
			characterfile.write(Integer.toString(p.musicVolume), 0, Integer.toString(p.musicVolume).length());
			characterfile.newLine();
			characterfile.write("soundEffectVolume = ", 0, 20);
			characterfile.write(Integer.toString(p.soundEffectVolume), 0, Integer.toString(p.soundEffectVolume).length());
			characterfile.newLine();
			characterfile.write("musicEnabled = ", 0, 15);
			characterfile.write(Boolean.toString(p.musicEnabled), 0, Boolean.toString(p.musicEnabled).length());
			characterfile.newLine();
			characterfile.write("mouseButton = ", 0, 14);
			characterfile.write(Boolean.toString(p.mouseButton), 0, Boolean.toString(p.mouseButton).length());
			characterfile.newLine();
			characterfile.write("splitChat = ", 0, 12);
			characterfile.write(Boolean.toString(p.splitChat), 0, Boolean.toString(p.splitChat).length());
			characterfile.newLine();
			characterfile.write("chatEffects = ", 0, 15);
			characterfile.write(Boolean.toString(p.chatEffects), 0, Boolean.toString(p.chatEffects).length());
			characterfile.newLine();
			characterfile.write("void = ", 0, 7);
			String toWrite = p.voidStatus[0] + "\t" + p.voidStatus[1] + "\t" + p.voidStatus[2] + "\t" + p.voidStatus[3] + "\t" + p.voidStatus[4];
			characterfile.write(toWrite);
			characterfile.newLine();
			characterfile.write("completed-tut = ", 0, 16);
			characterfile.write(Boolean.toString(p.completedTut), 0, Boolean.toString(p.completedTut).length());
			characterfile.newLine();
			characterfile.newLine();
			
			/*EQUIPMENT*/
			characterfile.write("[EQUIPMENT]", 0, 11);
			characterfile.newLine();
			for (int i = 0; i < p.playerEquipment.length; i++) {
				characterfile.write("character-equip = ", 0, 18);
				characterfile.write(Integer.toString(i), 0, Integer.toString(i).length());
				characterfile.write("	", 0, 1);
				characterfile.write(Integer.toString(p.playerEquipment[i]), 0, Integer.toString(p.playerEquipment[i]).length());
				characterfile.write("	", 0, 1);
				characterfile.write(Integer.toString(p.playerEquipmentN[i]), 0, Integer.toString(p.playerEquipmentN[i]).length());
				characterfile.write("	", 0, 1);
				characterfile.newLine();
			}
			characterfile.newLine();
			
			/*LOOK*/
			characterfile.write("[LOOK]", 0, 6);
			characterfile.newLine();
			for (int i = 0; i < p.playerAppearance.length; i++) {
				characterfile.write("character-look = ", 0, 17);
				characterfile.write(Integer.toString(i), 0, Integer.toString(i).length());
				characterfile.write("	", 0, 1);
				characterfile.write(Integer.toString(p.playerAppearance[i]), 0, Integer.toString(p.playerAppearance[i]).length());
				characterfile.newLine();
			}
			characterfile.newLine();
			
			/*SKILLS*/
			characterfile.write("[SKILLS]", 0, 8);
			characterfile.newLine();
			for (int i = 0; i < p.playerLevel.length; i++) {
				characterfile.write("character-skill = ", 0, 18);
				characterfile.write(Integer.toString(i), 0, Integer.toString(i).length());
				characterfile.write("	", 0, 1);
				characterfile.write(Integer.toString(p.playerLevel[i]), 0, Integer.toString(p.playerLevel[i]).length());
				characterfile.write("	", 0, 1);
				characterfile.write(Integer.toString(p.playerXP[i]), 0, Integer.toString(p.playerXP[i]).length());
				characterfile.newLine();
			}
			characterfile.newLine();
			
			/*ITEMS*/
			characterfile.write("[ITEMS]", 0, 7);
			characterfile.newLine();
			for (int i = 0; i < p.playerItems.length; i++) {
				if (p.playerItems[i] > 0) {
					characterfile.write("character-item = ", 0, 17);
					characterfile.write(Integer.toString(i), 0, Integer.toString(i).length());
					characterfile.write("	", 0, 1);
					characterfile.write(Integer.toString(p.playerItems[i]), 0, Integer.toString(p.playerItems[i]).length());
					characterfile.write("	", 0, 1);
					characterfile.write(Integer.toString(p.playerItemsN[i]), 0, Integer.toString(p.playerItemsN[i]).length());
					characterfile.newLine();
				}
			}
			characterfile.newLine();
			
		/*BANK*/
			characterfile.write("[BANK]", 0, 6);
			characterfile.newLine();
			for (int i = 0; i < p.bankItems.length; i++) {
				if (p.bankItems[i] > 0) {
					characterfile.write("character-bank = ", 0, 17);
					characterfile.write(Integer.toString(i), 0, Integer.toString(i).length());
					characterfile.write("	", 0, 1);
					characterfile.write(Integer.toString(p.bankItems[i]), 0, Integer.toString(p.bankItems[i]).length());
					characterfile.write("	", 0, 1);
					characterfile.write(Integer.toString(p.bankItemsN[i]), 0, Integer.toString(p.bankItemsN[i]).length());
					characterfile.newLine();
				}
			}
			characterfile.newLine();
			
		/*FRIENDS*/
			characterfile.write("[FRIENDS]", 0, 9);
			characterfile.newLine();
			for (int i = 0; i < p.friends.length; i++) {
				if (p.friends[i] > 0) {
					characterfile.write("character-friend = ", 0, 19);
					characterfile.write(Integer.toString(i), 0, Integer.toString(i).length());
					characterfile.write("	", 0, 1);
					characterfile.write("" + p.friends[i]);
					characterfile.newLine();
				}
			}
			characterfile.newLine();
			
		/*IGNORES*/
			/*characterfile.write("[IGNORES]", 0, 9);
			characterfile.newLine();
			for (int i = 0; i < ignores.length; i++) {
				if (ignores[i] > 0) {
					characterfile.write("character-ignore = ", 0, 19);
					characterfile.write(Integer.toString(i), 0, Integer.toString(i).length());
					characterfile.write("	", 0, 1);
					characterfile.write(Long.toString(ignores[i]), 0, Long.toString(ignores[i]).length());
					characterfile.newLine();
				}
			}
			characterfile.newLine();*/
		/*EOF*/
			characterfile.write("[EOF]", 0, 5);
			characterfile.newLine();
			characterfile.newLine();
			characterfile.close();
		} catch(IOException ioexception) {
			Misc.println(p.playerName+": error writing file.");
			return false;
		}
		return true;
	}	
	

}