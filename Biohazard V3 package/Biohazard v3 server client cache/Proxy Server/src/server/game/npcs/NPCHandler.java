package server.game.npcs;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import server.Config;
import server.Server;
import server.clip.region.Region;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.minigames.castlewars.CastleWars;
import server.game.minigames.randomevents.RiverTroll;
import server.game.minigames.randomevents.RockGolem;
import server.game.minigames.randomevents.SpiritTree;
import server.game.minigames.randomevents.Zombie;
import server.game.players.Client;
import server.game.players.Player;
import server.game.players.PlayerHandler;
import server.world.definitions.EntityDef;
import core.util.Misc;

public class NPCHandler {
	public static int maxNPCs = 7000;
	public static int maxListedNPCs = 7000;
	public static int maxNPCDrops = 7000;
	public static NPC npcs[] = new NPC[maxNPCs];
	public static NPCList NpcList[] = new NPCList[maxListedNPCs];
	public static NPCDrop npcDropList[][] = new NPCDrop[NPCHandler.maxListedNPCs][];

	public NPCHandler() {
		for (int i = 0; i < maxNPCs; i++) {
			npcs[i] = null;
		}
		for (int i = 0; i < maxListedNPCs; i++) {
			NpcList[i] = null;
		}
		/*
		 * When public static void main within the Server class is read so will
		 * this configuration file.
		 */

		loadNPCDrops("./Data/CFG/npc_drops.cfg");
		loadNPCList("./Data/CFG/npc.cfg");
		loadAutoSpawn("./Data/CFG/spawn-config.cfg");
	}

	public float getRarity(String value) {
		float dropRarity = 0.0f;
		if (value.equalsIgnoreCase("ALWAYS")) {
			dropRarity = 100.0f;
		} else if (value.equalsIgnoreCase("VERY_COMMON")) {
			dropRarity = 75.0f;
		} else if (value.equalsIgnoreCase("COMMON")) {
			int x = Misc.random(51, 100); // 1 in 2 through 50 kills will give
										// this item
			dropRarity = (float) (1 - (1 - (1 / x))) * 100;
		} else if (value.equalsIgnoreCase("UNCOMMON")) {
			int x = Misc.random(84, 152); // 1 in 51 through 100 kills will give
											// this item
			dropRarity = (float) (1 - (1 - (1 / x))) * 100;
		} else if (value.equalsIgnoreCase("RARE")) {
			int x = Misc.random(101, 412); // 1 in 101 through 512 kills will
											// give this item
			dropRarity = (float) (1 - (1 - (1 / x))) * 100;
		} else if (value.equalsIgnoreCase("VERY_RARE")) {
			int x = Misc.random(513, 850); // 1 in 513 through 1024 kills will
											// give this item
			dropRarity = (float) (1 - (1 - (1 / x))) * 100;
		} else if (value.equalsIgnoreCase("SUPER_RARE")) {
			int x = Misc.random(1025, 3096); // 1 in 1025 through 4096 kills
												// will give this item
			dropRarity = (float) (1 - (1 - (1 / x))) * 100;
		} else if (value.equalsIgnoreCase("EXTREMELY_RARE")) {
			int x = Misc.random(4097, 6192); // 1 in 4097 through 8192 kills
												// will give this item
			dropRarity = (float) (1 - (1 - (1 / x))) * 100;
		} else {
			dropRarity = Float.parseFloat(value);
		}
		return dropRarity;
	}

	/**
	 * 
	 * @param FileName
	 * @return NPC drops
	 * @start Used for relaying a console print out of intialization times.
	 * @dropsCount Used for relaying a console print out of the number of drops. @
	 *             & used to separate drops amounts of the same type of item by
	 *             amount. Example chickens: drop 5, 10 or 15 feathers, not a
	 *             range. TODO finish @ : used to separate drops using a range.
	 *             Example goblins: drop 1-24 coins. @ / used to indicate
	 *             multiple NPC's that have the same drops. Example cows:
	 *             2310/1768/1767/1766/955/397/81 are all the ID's for cows,
	 *             which share a common droptable.
	 */

	public boolean loadNPCDrops(final String FileName) {
		long start = System.currentTimeMillis();
		int dropsCount = 0;
		int npcCount = 0;
		String line = "";
		String token = "";
		String token2 = "";
		String token2_2 = "";
		String[] token3 = new String[10];
		boolean EndOfFile = false;
		BufferedReader characterfile = null;
		try {
			characterfile = new BufferedReader(new FileReader("./" + FileName));
		} catch (final FileNotFoundException fileex) {
			Misc.println(FileName + ": file not found.");
			return false;
		}
		try {
			line = characterfile.readLine();
		} catch (final IOException ioexception) {
			Misc.println(FileName + ": error loading file.");
			try {
				characterfile.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//return false;
		}
		while (EndOfFile == false && line != null) {
			line = line.trim();
			final int spot = line.indexOf("=");
			if (spot > -1) {
				token = line.substring(0, spot);
				token = token.trim();
				token2 = line.substring(spot + 1);
				token2 = token2.trim();
				token2_2 = token2.replaceAll("\t\t", "\t");
				while (token2_2.indexOf("\t\t") > -1) {
					token2_2 = token2_2.replaceAll("\t\t", "\t");
				}
				token3 = token2_2.split("\t");
				if (token.equals("drop")) {
					String[] slotTokens = token3[0].trim().split("/");
					for (String slotToken : slotTokens) {
						if (slotToken.length() > 0) {
							int slot = Integer.parseInt(slotToken.trim());
							List<NPCDrop> newNpcDrops = new LinkedList<NPCDrop>();
							for (int i = 1; i <= token3.length - 1; i++) {
								String[] token4 = token3[i].trim().split(":");
								NPCDrop npcDrop = null;
								if (token4.length == 3) {
									String amounts = token4[1].trim();
									if (amounts.indexOf("&") > -1) {
										String[] token5 = amounts.split("&");
										for (String amount : token5) {
											amount = amount.trim();
											if (amount.length() > 0) {
												npcDrop = new NPCDrop(
														slot,
														Integer.parseInt(token4[0]
																.trim()),
														Integer.parseInt(amount),
														token4[2].trim());
												if (npcDrop != null) {
													newNpcDrops.add(npcDrop);
												}
											}
										}
									} else {
										npcDrop = new NPCDrop(slot,
												Integer.parseInt(token4[0]
														.trim()),
												Integer.parseInt(token4[1]
														.trim()),
												token4[2].trim());
										if (npcDrop != null) {
											newNpcDrops.add(npcDrop);
										}
									}
								}
								if (token4.length == 4) {
									npcDrop = new NPCDrop(slot,
											Integer.parseInt(token4[0].trim()),
											Integer.parseInt(token4[1].trim()),
											Integer.parseInt(token4[2].trim()),
											token4[2].trim());
									if (npcDrop != null) {
										newNpcDrops.add(npcDrop);
									}
								}
							}
							npcDropList[slot] = newNpcDrops
									.toArray(new NPCDrop[0]);
							dropsCount += npcDropList[slot].length;
							npcCount += 1;
						}
					}
				}
			} else if (line.equals("[ENDOFDROPLIST]")) {
				try {
					characterfile.close();
				} catch (final IOException ioexception) {
				}
				System.out.println("            Loaded " + dropsCount
						+ " npc drops for " + npcCount + " npcs in "
						+ (System.currentTimeMillis() - start) + "ms.");
				return true;
			}
			try {
				line = characterfile.readLine();
			} catch (final IOException ioexception1) {
				EndOfFile = true;
			}
		}
		try {
			characterfile.close();
		} catch (final IOException ioexception) {
		}
		return false;
	}

	public void multiAttackGfx(int i, int gfx) {
		if (npcs[i].projectileId < 0)
			return;
		for (int j = 0; j < PlayerHandler.players.length; j++) {
			if (PlayerHandler.players[j] != null) {
				Client c = (Client) PlayerHandler.players[j];
				if (c.heightLevel != npcs[i].heightLevel)
					continue;
				if (PlayerHandler.players[j].goodDistance(c.absX, c.absY,
						npcs[i].absX, npcs[i].absY, 15)) {
					int nX = NPCHandler.npcs[i].getX() + offset(i);
					int nY = NPCHandler.npcs[i].getY() + offset(i);
					int pX = c.getX();
					int pY = c.getY();
					int offX = (nY - pY) * -1;
					int offY = (nX - pX) * -1;
					c.getPA().createPlayersProjectile(nX, nY, offX, offY, 50,
							getProjectileSpeed(i), npcs[i].projectileId, 43,
							31, -c.getId() - 1, 65);
				}
			}
		}
	}

	public boolean switchesAttackers(int i) {
		switch (npcs[i].npcType) {
		case 2551:
		case 2552:
		case 2553:
		case 2559:
		case 2560:
		case 2561:
		case 2563:
		case 2564:
		case 2565:
		case 2892:
		case 2894:
			return true;

		}

		return false;
	}

	public void multiAttackDamage(int i) {
		int max = getMaxHit(i);
		for (int j = 0; j < PlayerHandler.players.length; j++) {
			if (PlayerHandler.players[j] != null) {
				Client c = (Client) PlayerHandler.players[j];
				if (c.isDead || c.heightLevel != npcs[i].heightLevel)
					continue;
				if (PlayerHandler.players[j].goodDistance(c.absX, c.absY,
						npcs[i].absX, npcs[i].absY, 15)) {
					if (npcs[i].attackType == 2) {
						if (!c.prayerActive[16]) {
							if (Misc.random(500) + 200 > Misc.random(c
									.getCombat().mageDef())) {
								int dam = Misc.random(max);
								c.dealDamage(dam);
								c.handleHitMask(dam);
							} else {
								c.dealDamage(0);
								c.handleHitMask(0);
							}
						} else {
							c.dealDamage(0);
							c.handleHitMask(0);
						}
					} else if (npcs[i].attackType == 1) {
						if (!c.prayerActive[17]) {
							int dam = Misc.random(max);
							if (Misc.random(500) + 200 > Misc.random(c
									.getCombat().calculateRangeDefence())) {
								c.dealDamage(dam);
								c.handleHitMask(dam);
							} else {
								c.dealDamage(0);
								c.handleHitMask(0);
							}
						} else {
							c.dealDamage(0);
							c.handleHitMask(0);
						}
					}
					if (npcs[i].endGfx > 0) {
						c.gfx0(npcs[i].endGfx);
					}
				}
				c.getPA().refreshSkill(3);
			}
		}
	}

	public int getClosePlayer(int i) {
		for (int j = 0; j < PlayerHandler.players.length; j++) {
			if (PlayerHandler.players[j] != null) {
				if (j == npcs[i].spawnedBy)
					return j;
				if (goodDistance(PlayerHandler.players[j].absX,
						PlayerHandler.players[j].absY, npcs[i].absX,
						npcs[i].absY, 2 + distanceRequired(i)
								+ followDistance(i))
						|| isFightCaveNpc(i)) {
					if ((PlayerHandler.players[j].underAttackBy <= 0 && PlayerHandler.players[j].underAttackBy2 <= 0)
							|| PlayerHandler.players[j].inMulti())
						if (PlayerHandler.players[j].heightLevel == npcs[i].heightLevel)
							return j;
				}
			}
		}
		return 0;
	}

	public int getCloseRandomPlayer(int i) {
		ArrayList<Integer> players = new ArrayList<Integer>();
		for (int j = 0; j < PlayerHandler.players.length; j++) {
			if (PlayerHandler.players[j] != null) {
				if (goodDistance(PlayerHandler.players[j].absX,
						PlayerHandler.players[j].absY, npcs[i].absX,
						npcs[i].absY, 2 + distanceRequired(i)
								+ followDistance(i))
						|| isFightCaveNpc(i)) {
					if ((PlayerHandler.players[j].underAttackBy <= 0 && PlayerHandler.players[j].underAttackBy2 <= 0)
							|| PlayerHandler.players[j].inMulti())
						if (PlayerHandler.players[j].heightLevel == npcs[i].heightLevel)
							players.add(j);
				}
			}
		}
		if (players.size() > 0)
			return players.get(Misc.random(players.size() - 1));
		else
			return 0;
	}

	public int npcSize(int i) {
		switch (npcs[i].npcType) {
		case 2883:
		case 2882:
		case 2881:
			return 3;
		}
		return 0;
	}

	public boolean isAggressive(int i) {
		switch (npcs[i].npcType) {
		case 6203:
		case 6206:
		case 6208:
		case 6204:
		case 6260:
		case 6261:
		case 8528:
		case 6263:
		case 6265:
		case 6222:
		case 6223:
		case 6225:
		case 6227:
		case 6247:
		case 6248:
		case 6250:
		case 6252:
		case 2892:
		case 2894:
		case 2881:
		case 2882:
		case 2883:
		case 1459:// monkey guard
		case 3068:// wyvern
		case 795:
		case 55:
		case 941:
		case 54:
		case 53:
		case 396:
		case 186:
		case 282:
		case 82:
		case 49:
		case 96:
		case 97:
		case 141:
		case 1558:
			return true;
		}
		/*
		 * if (npcs[i].inWild() && npcs[i].MaxHP > 0) return true;
		 */
		if (isFightCaveNpc(i))
			return true;
		return false;
	}

	public boolean isFightCaveNpc(int i) {
		switch (npcs[i].npcType) {
		case 2627:
		case 2630:
		case 2631:
		case 2741:
		case 2743:
		case 2745:
			return true;
		}
		return false;
	}

	/**
	 * Summon npc, barrows, etc
	 **/
	public void spawnNpc(final Client c, int npcType, int x, int y,
			int heightLevel, int WalkingType, int HP, int maxHit, int attack,
			int defence, boolean attackPlayer, boolean headIcon) {
		// first, search for a free slot
		int slot = -1;
		for (int i = 1; i < maxNPCs; i++) {
			if (npcs[i] == null) {
				slot = i;
				break;
			}
		}
		if (slot == -1) {
			// Misc.println("No Free Slot");
			return; // no free slot found
		}
		final NPC newNPC = new NPC(slot, npcType);
		newNPC.absX = x;
		newNPC.absY = y;
		newNPC.makeX = x;
		newNPC.makeY = y;
		newNPC.heightLevel = heightLevel;
		newNPC.walkingType = WalkingType;
		newNPC.HP = HP;
		newNPC.MaxHP = HP;
		newNPC.maxHit = maxHit;
		newNPC.attack = attack;
		newNPC.defence = defence;
		newNPC.spawnedBy = c.getId();
		if (headIcon)
			c.getPA().drawHeadicon(1, slot, 0, 0);
		if (attackPlayer) {
			newNPC.underAttack = true;
			if (c != null) {
				/*
				 * if(server.game.minigames.barrows.Barrows.COFFIN_AND_BROTHERS[c
				 * .randomCoffin][1] != newNPC.npcType) { if(newNPC.npcType ==
				 * 2025 || newNPC.npcType == 2026 || newNPC.npcType == 2027 ||
				 * newNPC.npcType == 2028 || newNPC.npcType == 2029 ||
				 * newNPC.npcType == 2030) {
				 * newNPC.forceChat("You dare disturb my rest!"); } }
				 * if(server.game
				 * .minigames.barrows.Barrows.COFFIN_AND_BROTHERS[c
				 * .randomCoffin][1] == newNPC.npcType) {
				 * newNPC.forceChat("You dare steal from us!"); }
				 */

				newNPC.killerId = c.playerId;
			}
		}
		for (int[] aSpiritTree : SpiritTree.spiritTree) {
			if (newNPC.npcType == aSpiritTree[2]) {
				newNPC.forceChat("Leave these woods and never return!");
				CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer container) {
						container.stop();
					}

					@Override
					public void stop() {
						// if(!newNPC.isDead) {
						newNPC.isDead = true;
						newNPC.updateRequired = true;
						c.treeSpawned = false;
						// }
					}
				}, 200);
			}
		}
		for (int[] aRockGolem : RockGolem.rockGolem) {
			if (newNPC.npcType == aRockGolem[2]) {
				newNPC.forceChat("Raarrrgghh! Flee human!");
				CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer container) {
						container.stop();
					}

					@Override
					public void stop() {
						// if(!newNPC.isDead) {
						newNPC.isDead = true;
						newNPC.updateRequired = true;
						c.golemSpawned = false;
						// }
					}
				}, 200);
			}
		}
		for (int[] aRiverTroll : RiverTroll.riverTroll) {
			if (newNPC.npcType == aRiverTroll[2]) {
				newNPC.forceChat("Fishies be mine! Leave dem fishies!");
				CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer container) {
						container.stop();
					}

					@Override
					public void stop() {
						// if(!newNPC.isDead) {
						newNPC.isDead = true;
						newNPC.updateRequired = true;
						c.trollSpawned = false;
						// }
					}
				}, 200);
			}
		}
		for (int[] aZombie : Zombie.zombie) {
			if (newNPC.npcType == aZombie[2]) {
				newNPC.forceChat("Braaaainssss!");
				CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer container) {
						container.stop();
					}

					@Override
					public void stop() {
						// if(!newNPC.isDead) {
						newNPC.isDead = true;
						newNPC.updateRequired = true;
						c.zombieSpawned = false;
						// }
					}
				}, 200);
			}
		}
		npcs[slot] = newNPC;
	}
	
	//castlewars
	public void spawnBarricade(int npcType, int x, int y, int heightLevel,
			int WalkingType, int HP, int maxHit, int attack, int defence, int team) {
		// first, search for a free slot
		int slot = -1;
		for (int i = 1; i < maxNPCs; i++) {
			if (npcs[i] == null) {
				slot = i;
				break;
			}
		}
		if (slot == -1) {
			// Misc.println("No Free Slot");
			return; // no free slot found
		}
		NPC newNPC = new NPC(slot, npcType);
		newNPC.absX = x;
		newNPC.absY = y;
		newNPC.makeX = x;
		newNPC.makeY = y;
		newNPC.heightLevel = heightLevel;
		newNPC.walkingType = WalkingType;
		newNPC.HP = HP;
		newNPC.MaxHP = HP;
		newNPC.maxHit = maxHit;
		newNPC.attack = attack;
		newNPC.defence = defence;
		newNPC.team = team;
		npcs[slot] = newNPC;
		//castlewars
		if (npcType == 1532) {
			for (int j = 0; j < CastleWars.BARRICADE_INDEX.length; j++) {
				if (CastleWars.BARRICADE_INDEX[j] == 0) {
					CastleWars.BARRICADE_INDEX[j] = slot;
					break;
				}
			}
		}
	}

	public void spawnNpc2(int npcType, int x, int y, int heightLevel,
			int WalkingType, int HP, int maxHit, int attack, int defence) {
		// first, search for a free slot
		int slot = -1;
		for (int i = 1; i < maxNPCs; i++) {
			if (npcs[i] == null) {
				slot = i;
				break;
			}
		}
		if (slot == -1) {
			// Misc.println("No Free Slot");
			return; // no free slot found
		}
		NPC newNPC = new NPC(slot, npcType);
		newNPC.absX = x;
		newNPC.absY = y;
		newNPC.makeX = x;
		newNPC.makeY = y;
		newNPC.heightLevel = heightLevel;
		newNPC.walkingType = WalkingType;
		newNPC.HP = HP;
		newNPC.MaxHP = HP;
		newNPC.maxHit = maxHit;
		newNPC.attack = attack;
		newNPC.defence = defence;
		npcs[slot] = newNPC;
	}

	/**
	 * Emotes
	 **/

	public static int getAttackEmote(int i) {
		if (npcs[i].npcType == 3761)
			return 3880;
		if (npcs[i].npcType == 3760)
			return 3880;
		if (npcs[i].npcType == 3771)
			return 3922;

		switch (npcs[i].npcType) {
		// GODWARS
		case 3247: // Hobgoblin
			return 6184;

		case 6270: // Cyclops
		case 6269: // Ice cyclops
			return 4652;

		case 6219: // Spiritual Warrior
		case 6255: // Spiritual Warrior
			return 451;

		case 6229: // Spirtual Warrior arma
			return 6954;

		case 6218: // Gorak
			return 4300;

		case 6212: // Werewolf
			return 6536;

		case 6220: // Spirtual Ranger
		case 6256: // Spirtual Ranger
			return 426;

		case 6257: // Spirtual Mage
		case 6221: // Spirtual Mage
			return 811;

		case 6276: // Spirtual Ranger
		case 6278: // Spirtual Mage
		case 6272: // Ork
		case 6274: // Ork
		case 6277: // Spirtual Warrior bandos
			return 4320;

		case 6230: // Spirtual Ranger
		case 6233: // Aviansie
		case 6239: // Aviansie
		case 6232: // Aviansie
			return 6953;

		case 6254: // Saradomin Priest
			return 440;
		case 6258: // Saradomin Knight
			return 7048;
		case 6231: // Spirtual Mage
			return 6952;

		case 6260:
			if (npcs[i].attackType == 0)
				return 7060;
			else
				return 7063;
		case 2060:// slash bash
			return 359;
		case 5902:// inadequacy
			return 6318;
		case 5421:// mutant tarn
			return 5617;
		case 5666:// barrelchest
			return 5894;
		case 5665:
		case 5664:// zombiepirate
			return 5889;
		case 3847: // stq
			return 3991;
		case 3200: // chaos ele
			return 3146;
		case 115: // ogre
			return 359;
		case 3340: // giant mole
			return 3312;
		case 4972: // giant roc
			return 5024;
		case 4971: // baby roc
			return 5031;
		case 1608: // kurask
			return 1512;
		case 1616: // basilisk
			return 1546;

		case 1632: // turoth
			return 1595;
		case 1622: // rockslug
			return 1564;
		case 1317:
		case 2267:
		case 2268:
		case 2269:
		case 3181:
		case 3182:
		case 3183:
		case 3184:
		case 3185:
			return 451;
		case 6279:
		case 6280:
		case 6281:
		case 6282:
			return 6185;
		case 5529:
			return 5782;
		case 6116: // Seagull
			return 6811;
		case 5247:
			return 5411;
		case 6261:
		case 6263:
		case 6265:
			return 6154;
		case 6222:
			return 6976;
		case 6225:
			return 6953;
		case 6223:
			return 6952;
		case 6227:
			return 6954;
			// end of arma gwd
			// sara gwd
		case 6247:
			return 6964;
		case 6248:
			return 6376;
		case 6250:
			return 7018;
		case 6252:
			return 7009;
		case 6203:
			if (npcs[i].attackType == 0)
				return 6945;
			else
				return 6947; // end
		case 6204:
			return 64;
		case 6208:
			return 6947;
		case 6206:
			return 6945;
		case 5219:
		case 5218:
		case 5217:
		case 5216:
		case 5215:
		case 5214:
		case 5213:
			return 5097; // Penance
		case 5233:
		case 5232:
		case 5231:
		case 5230:
			return 5395;
		case 3761:
		case 3760:
			return 3880;
		case 3771:
			return 3922;
		case 3062:
			return 2955;
		case 131:
			return 5669;
		case 4404:
		case 4405:
		case 4406:
			return 4266;
		case 1019:
			return 1029;
		case 1020:
			return 1035;
		case 1021:
			return 1040;
		case 1022:
			return 1044;
		case 1676:
			return 1626;
		case 1677:
			return 1616;
		case 1678:
			return 1612;
		case 195:
		case 196:
		case 202:
		case 204:
		case 203:
			return 412;
		case 4353:
		case 4354:
		case 4355:
			return 4234;
		case 2709:
		case 2710:
		case 2711:
		case 2712:
			return 1162;

		case 1007:
			return 811;
		case 3058:
			return 2930;
		case 113:
			return 128;
		case 114:
			return 359;
		case 1265:
			return 1312;
		case 118:
			return 99;
		case 127:
			return 185;
		case 914:
			return 197;
		case 1620:
		case 1621:
			return 1562;
		case 678:
			return 426;
		case 1192:
			return 1245;
		case 119:
			return 99;
		case 2263:
			return 2182;
		case 3347:
		case 3346:
			return 3326;
		case 3063:
			return 2930;
		case 3061:
			return 2958;
		case 3066:
			return 2927;
		case 3452:// penance queen
			return 5411;
		case 2783:// dark beast
			return 2731;
		case 908:
			return 128;
		case 909:
			return 143;
		case 911:
			return 64;
		case 1624:
		case 1625:
			return 1557;
		case 3060:
			return 2962;
		case 2598:
		case 2599:
		case 2600:
		case 2601:
		case 2602:
		case 2603:
		case 2604:// tzhar-hur
			return 2609;
		case 2892:// Spinolyp
			return 2869;
		case 2881: // supreme
			return 2855;
		case 2882: // prime
			return 2854;
		case 2883: // rex
			return 2853;
		case 2457:
			return 2365;
		case 1341:
			return 1341;
		case 2606:// tzhaar-xil
			return 2609;
		case 66:
		case 67:
		case 168:
		case 169:
		case 162:
		case 68:// gnomes
			return 190;
		case 913:
		case 912:
			return 1162;
		case 160:
		case 161:
			return 191;
		case 163:
		case 164:
			return 192;
		case 438:
		case 439:
		case 440:
		case 441:
		case 442: // Tree spirit
		case 443:
			return 94;
		case 391:
		case 392:
		case 393:
		case 394:
		case 395:// river troll
		case 396:
			return 284;
		case 413:
		case 414:
		case 415:
		case 416:
		case 417:// rock golem
		case 418:
			return 153;

		case 3741: // Pest control
			return 3901;
		case 3776:
			return 3897;
		case 3751:
		case 3750:
		case 3749:
		case 3748:
		case 3747:
			return 3908;
		case 1153:
		case 1154:
		case 1155:
		case 1156:
		case 1157:
		case 1158: // kalphite
			return 6223;
		case 1160: // kalphite
			return 6235;

		case 2734:
		case 2627:// tzhaar
			return 2621;
		case 2630:
		case 2738:
		case 2736:
		case 2629:
			return 2625;
		case 2631:
		case 2632:
			return 2633;
		case 2741:
			return 2636;

		case 60:
		case 62:
		case 64:
		case 59:
		case 61:
		case 63:
		case 134:
		case 1009:
		case 2035: // spider
			return 5327;

		case 6006: // wolfman
			return 6547;

		case 1612:// banshee
			return 1523;

		case 1648:
		case 1649:
		case 1650:
		case 1651:
		case 1652:
		case 1653:
		case 1654:
		case 1655:
		case 1656:
		case 1657:// crawling hand
			return 1592;

		case 1604:
		case 1605:
		case 1606:
		case 1607:// aberrant specter
			return 1507;

		case 1618:
		case 1619:// bloodveld
			return 1552;

		case 1643:
		case 1644:
		case 1645:
		case 1646:
		case 1647:// infernal mage
			return 429;

		case 1613:// nechryael
			return 1528;

		case 1610:
		case 1611:// gargoyle
			return 1517;

		case 1615:// abyssal demon
			return 1537;

		case 1633: // pyrefiend
		case 3406:
			return 1582;

		case 1459:// monkey guard
			return 1402;

		case 1456:// monkey archer
			return 1394;

		case 1125:// dad
			return 284;

		case 1096:
		case 1097:
		case 1098:
		case 1106:
		case 1942:
		case 1108:
		case 1109:
		case 1110:
		case 1111:
		case 1112:
		case 1116:
		case 1117:
		case 1101:// troll
			return 284;
		case 1095:
			return 1142;

		case 49:// hellhound
		case 142:
		case 95:
		case 96:
			return 6581;

		case 74:
		case 75:
		case 76:
			return 5571;

		case 73:
		case 751: // zombie
		case 77:
		case 419:
		case 420:
		case 421:
		case 422:
		case 423:
		case 424:
			return 5568;

		case 103:
		case 104:
		case 655:
		case 749:
		case 491: // ghost
			return 5540;

		case 708: // imp
			return 169;

		case 397:
			return 59;

		case 172:
			return 1162;

		case 86:
		case 87:
			return 186;
		case 47:// rat
			return 2705;

		case 122:// hobgoblin
			return 164;

		case 1770:
		case 1771:
		case 1772:
		case 1773:
		case 101:
		case 2678:
		case 2679:
		case 1774:
		case 1775:
		case 1769:
		case 1776:// goblins
			return 6184;

		case 141:
			return 6579;
		case 1593:
			return 6562;
		case 1954:
		case 152:
		case 45:
		case 1558: // wolf
			return 75;

		case 1560: // troll
		case 1566:
			return 284;

		case 89:
			return 6376;
		case 133: // unicorns
			return 289;

		case 1585:
		case 110:
		case 1582:
		case 1583:
		case 1584:
		case 1586: // giant
		case 4291:
		case 4292:
			return 4666;
		case 111:
		case 4687:
			return 4672;
		case 4690:
		case 4691:
		case 4692:
		case 4693:
		case 117:
		case 116:
		case 112:
		case 1587:
		case 1588:
			return 4652;

		case 128: // snake
		case 1479:
			return 275;

		case 2591:
		case 2620: // tzhaar
			return 2609;

		case 2610:
		case 2619: // tzhaar
			return 2610;

		case 2033: // rat
		case 748:
			return 138;

		case 2031: // bloodveld
			return 2070;

		case 90:
		case 91:
		case 5359:
		case 5384:
		case 92:
		case 93: // skeleton
			return 5485;

		case 1766:
		case 1767:
		case 81: // cow
			return 5849;
		case 3065:
			return 2721;
		case 21: // hero
			return 451;

		case 1017:
		case 2693:
		case 41: // chicken
			return 5387;

		case 82:
		case 83:
		case 752:
		case 3064:
		case 4694:
		case 4695:
		case 4696:
		case 4697:
		case 84:
		case 4702:
		case 4703:
		case 4704:
		case 4705: // lesser
			return 64;

		case 123: // hobgoblin
			return 163;

		case 9: // guard
		case 32: // guard
		case 20: // paladin
			return 451;

		case 2456:
			return 1343;
		case 2455:
		case 2454:
		case 1338: // dagannoth
		case 1340:
		case 1342:
			return 1341;

		case 941:// green dragon
		case 55:
		case 50:
		case 54:
		case 742:
		case 1589:
		case 52:
		case 53:
		case 4669:
		case 4670:
		case 4671:
		case 1590:
		case 1591:
		case 1592:
		case 5363:
			if (npcs[i].attackType == 3) {
				return 81;
			} else {
				if (Misc.random(1) == 1) {
					return 91;
				} else {
					return 80;// or 80 //81 for spitting fire
				}
			}

		case 1092:
		case 19: // white knight
			return 406;

		case 125: // ice warrior
			return 451;

		case 78:
		case 412: // bat
			return 4915;

		case 105:
		case 1195:
		case 1196:// bear
		case 106:
			return 4922;
		case 2889:
			return 2859;
		case 132: // monkey
			return 220;

		case 1600:// cave crawler
			return 227;

		case 108:// scorpion
		case 1477:
			return 6254;
		case 107:
		case 144:
			return 6254;

		case 2028: // karil
			return 2075;

		case 2025: // ahrim
			return 729;

		case 2026: // dharok
			return 2067;

		case 2027: // guthan
			return 2080;

		case 2029: // torag
			return 0x814;

		case 2030: // verac
			return 2062;
		case 2745: // verac
			if (npcs[i].attackType == 1)
				return 2652;
			if (npcs[i].attackType == 2)
				return 2656;
			if (npcs[i].attackType == 0)
				return 2655;
		default:
			if(EntityDef.forID(npcs[i].npcType).standAnim == 808)
				return 0x326;
			else
				return EntityDef.forID(npcs[i].npcType).walkAnim+2;
		}
	}

	public static int getBlockEmote(int i) {
		if (npcs[i].npcType == 3776)
			return 3895;
		if (npcs[i].npcType == 3761)
			return 3881;
		if (npcs[i].npcType == 3760)
			return 3881;
		if (npcs[i].npcType == 3771)
			return 3921;
		if (npcs[i].npcType == 3751 || npcs[i].npcType == 3750
				|| npcs[i].npcType == 3749 || npcs[i].npcType == 3748
				|| npcs[i].npcType == 3747)
			return 3909;
		if (npcs[i].npcType == 3741)
			return 3902;

		switch (npcs[i].npcType) {
		case 78:
		case 412: // bat
			return 4916;
		case 6250: // sara lion
			return 7017;
		case 6248: // sara horse
			return 6375;
		case 6247: // sara boss
			return 6966;
		case 2060:// slash bash
			return 360;
		case 5902:// inadequacy
			return 6319;
		case 5421:// mutant tarn
			return 5618;
		case 5666:// barrelchest
			return 5897;
		case 5665:
		case 5664:// zombiepirate
			return 5890;
		case 3847: // stq
			return 3992;
		case 103:
		case 104:
		case 655:
		case 749:
		case 491: // ghost
			return 5541;
		case 3200: // chaos ele
			return 3148;
		case 115: // ogre
			return 360;
		case 3340: // giant mole
			return 3311;
		case 4972: // giant roc
			return 5026;
		case 4971: // baby roc
			return 5032;
		case 1608: // kurask
			return 1514;
		case 1616: // basilisk
			return 1547;
		case 1632: // turoth
			return 1596;

		case 1622: // rockslug
			return 1567;
		case 1600: // cave crawler
			return -1;

		case 6279:
		case 6280:
		case 6281:
		case 6282:
			return 6183;
		case 5529:
			return 5783;
			// bandos
		case 6260:
			return 7061;
		case 6261:
		case 6263:
		case 6265:
			return 6155;
		case 5247:
			return 5413;
		case 6116: // Seagull
			return 6810;
		case 5233:
		case 5232:
		case 5231:
		case 5230:
			return 5396;
		case 5219:
		case 5218:
		case 5217:
		case 5216:
		case 5215:
		case 5214:
		case 5213:
			return 5096; // Penance
		case 113:
			return 129;
		case 114:
			return 360;
		case 3058:
			return 2937;
		case 3061:
			return 2961;
		case 3063:
			return 2937;
		case 131:
			return 5670;
		case 1676:
			return 1627;
		case 1677:
			return 1617;
		case 1678:
			return 1613;
		case 1019:
			return 1030;
		case 1020:
			return 1036;
		case 1021:
			return 1042;
		case 1022:
			return 1046;
		case 914:
			return 194;
		case 4353:
		case 4354:
		case 4355:
			return 4232;
		case 4404:
		case 4405:
		case 4406:
			return 4267;
		case 3065:
			return 2720;
		case 1620:
		case 1621:
			return 1560;
		case 3066:
			return 2926;
		case 1265:
			return 1313;
		case 118:
			return 100;
		case 2263:
			return 2181;
		case 82:
		case 84:
		case 4702:
		case 4703:
		case 4704:
		case 4705:
		case 752:
		case 4694:
		case 4695:
		case 4696:
		case 4697:
		case 3064:
		case 83: // lesser
			return 65;
		case 3347:
		case 3346:
			return 3325;
		case 1192:
			return 1244;
		case 3062:
			return 2953;
		case 3060:
			return 2964;
		case 2892: // Spinolyp
		case 2894: // Spinolyp
		case 2896: // Spinolyp
			return 2869;
		case 1624:
		case 1625:
			return 1555;
		case 1354:
		case 1341:
		case 2455:// dagannoth
		case 2454:
		case 2456:
			return 1340;
		case 127:
			return 186;
		case 119:
			return 100;
		case 2881: // supreme
		case 2882: // prime
		case 2883: // rex
			return 2852;
		case 3452:// penance queen
			return 5413;
		case 2745:
			return 2653;
		case 2743:
			return 2645;
		case 1590:// metal dragon
		case 1591:
		case 1592:
		case 5363:
			return 89;
		case 2598:
		case 2599:
		case 2600:
		case 2610:
		case 2601:
		case 2602:
		case 2603:
		case 2606:// tzhaar-xil
		case 2591:
		case 2604:// tzhar-hur
			return 2606;
		case 66:
		case 67:
		case 168:
		case 169:
		case 162:
		case 68:// gnomes
			return 193;
		case 160:
		case 161:
			return 194;
		case 163:
		case 164:
			return 193;
		case 438:
		case 439:
		case 440:
		case 441:
		case 442: // Tree spirit
		case 443:
			return 95;
		case 391:
		case 392:
		case 393:
		case 394:
		case 395:// river troll
		case 396:
		case 1560:
		case 1566:
			return 285;
		case 413:
		case 414:
		case 415:
		case 416:
		case 417:// rock golem
		case 418:
			return 154;

		case 1153:
		case 1154:
		case 1155:
		case 1156:
		case 1157:
		case 1158: // kalphite
			return 6232;
		case 1160: // kalphite
			return 6237;
		case 2783:// dark beast
			return 2732;
		case 2734:
		case 2627:// tzhaar
			return 2622;
		case 2630:
		case 2629:
		case 2736:
		case 2738:
			return 2626;
		case 2631:
		case 2632:
			return 2629;
		case 2741:
			return 2635;

		case 908:
			return 129;
		case 909:
			return 147;
		case 911:
			return 65;

		case 1459:// monkey guard
			return 1403;

		case 1633: // pyrefiend
		case 3406:
			return 1581;

		case 1612:// banshee
			return 1525;

		case 1648:
		case 1649:
		case 1650:
		case 1651:
		case 1652:
		case 1653:
		case 1654:
		case 1655:
		case 1656:
		case 1657:// crawling hand
			return 1591;

		case 1604:
		case 1605:
		case 1606:
		case 1607:// aberrant specter
			return 1509;

		case 1618:
		case 1619:// bloodveld
			return 1550;

		case 1643:
		case 1644:
		case 1645:
		case 1646:
		case 1647:// infernal mage
			return 430;

		case 1613:// nechryael
			return 1529;

		case 1610:
		case 1611:// gargoyle
			return 1519;

		case 1615:// abyssal demon
			return 1537;

		case 1770:
		case 1771:
		case 1772:
		case 1773:
		case 101:
		case 2678:
		case 2679:
		case 1774:
		case 1775:
		case 1769:
		case 1776:// goblins
			return 6183;

		case 132: // monkey
			return 221;

		case 6006: // wolfman
			return 6538;

		case 1456:// monkey archer
			return 1395;

		case 108:// scorpion
		case 1477:
			return 6255;
		case 107:
		case 144:
			return 6255;

		case 1125:// dad
			return 285;

		case 1096:
		case 1097:
		case 1098:
		case 1942:
		case 1108:
		case 1109:
		case 1110:
		case 1111:
		case 1112:
		case 1116:
		case 1117:
		case 1101:// troll
		case 1106:
			return 285;
		case 1095:
			return 285;

		case 123:
		case 122:// hobgoblin
			return 165;

		case 49:// hellhound
		case 142:
		case 95:
		case 96:
		case 125:
			return 6578;
		case 141:
			return 6578;
		case 1593:
			return 6563;
		case 152:
		case 45:
		case 1558: // wolf
		case 1954:
			return 76;

		case 89:
			return 6375;
		case 133: // unicorns
			return 290;

		case 105:
		case 1195:
		case 1196:// bear
			return 4921;

		case 74:
		case 75:
		case 76:
			return 5574;

		case 73:
		case 751: // zombie
		case 77:
		case 419:
		case 420:
		case 421:
		case 422:
		case 423:
		case 424:
			return 5574;

		case 60:
		case 64:
		case 59:
		case 61:
		case 63:
		case 134:
		case 2035: // spider
		case 62:
		case 1009:
			return 5328;

		case 1585:
		case 110:
		case 1582:
		case 1583:
		case 1584:
		case 1586: // giant
		case 4291:
		case 4292:
			return 4671;
		case 111:
		case 4687:
			return 4671;
		case 4690:
		case 4691:
		case 4692:
		case 4693:
		case 117:
		case 116:
		case 112:
		case 1587:
		case 1588:
			return 4651;

		case 50: // kbd
			return 89;

		case 941:// green dragon
		case 55:
		case 742:
		case 1589:
		case 53:
		case 4669:
		case 4670:
		case 4671:
		case 52:
		case 54:
			return 89;
		case 2889:
			return 2860;
		case 81: // cow
		case 397:
			return 5850;

		case 708: // imp
			return 170;

		case 86:
		case 87:
			return 139;
		case 47:// rat
			return 2706;
		case 2457:
			return 2366;
		case 128: // snake
		case 1479:
			return 276;

		case 1017:
		case 2693:
		case 41: // chicken
			return 5388;

		case 90:
		case 91:
		case 5359:
		case 5384:
		case 92:
		case 93: // skeleton
			return 5489;

		/*case 3247: // Hobgoblin
		case 6270: // Cyclops
		case 6269: // Ice cyclops
		case 6219: // Spiritual Warrior
		case 6255: // Spiritual Warrior
		case 6229: // Spirtual Warrior arma
		case 6218: // Gorak
		case 6212: // Werewolf
		case 6220: // Spirtual Ranger
		case 6256: // Spirtual Ranger
		case 6257: // Spirtual Mage
		case 6221: // Spirtual Mage
		case 6276: // Spirtual Ranger
		case 6278: // Spirtual Mage
		case 6272: // Ork
		case 6274: // Ork
		case 6277: // Spirtual Warrior bandos
		case 6230: // Spirtual Ranger
		case 6233: // Aviansie
		case 6239: // Aviansie
		case 6232: // Aviansie
		case 6254: // Saradomin Priest
		case 6258: // Saradomin Knight
		case 6231: // Spirtual Mage
			return -1;*/

		default:
			return -1;
		}
	}

	public static int getDeadEmote(int i) {
		if (npcs[i].npcType == 3761)
			return 3883;
		if (npcs[i].npcType == 3760)
			return 3883;
		if (npcs[i].npcType == 3741)
			return 3903;
		if (npcs[i].npcType == 3776)
			return 3894;
		if (npcs[i].npcType == 3751 || npcs[i].npcType == 3750
				|| npcs[i].npcType == 3749 || npcs[i].npcType == 3748
				|| npcs[i].npcType == 3747)
			return 3910;
		if (npcs[i].npcType == 3771)
			return 3922;

		switch (npcs[i].npcType) {
		case 3247: // Hobgoblin
			return 6182;

		case 6270: // Cyclops
		case 6269: // Ice cyclops
			return 4653;

		case 6218: // Gorak
			return 4302;

		case 6212: // Werewolf
			return 6537;

		case 6276: // Spirtual Ranger
		case 6278: // Spirtual Mage
		case 6272: // Ork
		case 6274: // Ork
		case 6277: // Spirtual Warrior bandos
			return 4321;

		case 6230: // Spirtual Ranger
		case 6233: // Aviansie
		case 6239: // Aviansie
		case 6232: // Aviansie
		case 6231: // Spirtual Mage
		case 6229: // Spirtual Warrior arma

		case 6223: // Aviansie
		case 6225: // Spirtual Mage
		case 6227: // Spirtual Warrior arma
			return 6956;
		case 6252:
			return 7011;
		case 6222:
			return 6975;
		case 6203:
		case 6204:
		case 6206:
		case 6208:
			return 6946;
		case 6250: // sara lion
			return 7016;
		case 6247: // sara boss
			return 6965;
		case 6248: // sara horse
			return 6377;
		case 6261:
		case 6263:
		case 6265:
			return 6156;
		case 2060:// slash bash
			return 361;
		case 5902:// inadequacy
			return 6322;
		case 5421:// mutant tarn
			return 5619;
		case 5666:// barrelchest
			return 5898;
		case 3847: // stq
			return 3993;
		case 103:
		case 104:
		case 655:
		case 749:
		case 491: // ghost
			return 5542;
		case 1158:
			return 6242;
		case 1160:
			return 6233;
		case 3200: // chaos ele
			return 3147;
		case 115: // ogre
			return 361;
		case 3340: // giant mole
			return 3313;
		case 4972: // giant roc
			return 5027;
		case 4971: // baby roc
			return 5033;
		case 1608: // kurask
			return 1513;
		case 1616: // basilisk
			return 1548;
		case 1632: // turoth
			return 1597;

		case 1622: // rockslug
			return 1568;
		case 1600: // cave crawler
			return 228;

		case 6279:
		case 6280:
		case 6281:
		case 6282:
			return 6182;
		case 5529:
			return 5784;
		case 6116: // Seagull
			return 6812;
		case 5247:
			return 5412;
		case 5233:
		case 5232:
		case 5231:
		case 5230:
			return 5397;
		case 1019:
			return 1031;
		case 1020:
			return 1037;
		case 1021:
			return 1041;
		case 1022:
			return 1048;
		case 5219:
		case 5218:
		case 5217:
		case 5216:
		case 5215:
		case 5214:
		case 5213:
			return 5098; // Penance
		case 4353:
		case 4354:
		case 4355:
			return 4233;
		case 113:
			return 131;
		case 114:
			return 361;
		case 3058:
			return 2938;
		case 3057:
			return 2945;
		case 3063:
			return 2938;
		case 131:
			return 5671;

		case 1676:
			return 1628;
		case 1677:
			return 1618;
		case 1678:
			return 1614;

		case 4404:
		case 4405:
		case 4406:
			return 4265;
		case 914:
			return 196;
		case 3065:
			return 2728;
		case 1620:
		case 1621:
			return 1563;
		case 3066:
			return 2925;
		case 1265:
			return 1314;
		case 118:
			return 102;
		case 2263:
			return 2183;
		case 2598:
		case 2599:
		case 2600:
		case 2601:
		case 2602:
		case 2603:
		case 2606:// tzhaar-xil
		case 2591:
		case 2604:// tzhar-hur
			return 2607;
		case 3347:
		case 3346:
			return 3327;
		case 1192:
			return 1246;
		case 1624:
		case 1625:
			return 1558;
		case 2892:
			return 2865;
		case 127:
			return 188;
		case 119:
			return 102;
		case 2881: // supreme
		case 2882: // prime
		case 2883: // rex
			return 2856;
		case 1590:// metal dragon
		case 1591:
		case 1592:
		case 5363:
			return 92;
		case 2783:// dark beast
			return 2733;
		case 3452:// penance queen
			return 5412;
		case 2889:
			return 2862;
		case 1354:// dagnnoth mother
		case 1341:
			return 1342;
		case 2457:
			return 2367;

		case 66:
		case 67:
		case 168:
		case 169:
		case 162:
		case 68:// gnomes
			return 196;
		case 160:
		case 161:
			return 196;
		case 163:
		case 164:
			return 196;
		case 1153:
		case 1154:
		case 1155:
		case 1156:
		case 1157:
			return 6230;

		case 438:
		case 439:
		case 440:
		case 441:
		case 442: // Tree spirit
		case 443:
			return 97;
		case 391:
		case 392:
		case 393:
		case 394:
		case 395:// river troll
		case 396:
			return 287;
		case 413:
		case 414:
		case 415:
		case 416:
		case 417:// rock golem
		case 418:
			return 156;

		case 2745:
			return 2654;
		case 2743:
			return 2646;

		case 2734:
		case 2627:// tzhaar
			return 2620;
		case 2630:
		case 2629:
		case 2736:
		case 2738:
			return 2627;
		case 2631:
		case 2632:
			return 2630;
		case 2741:
			return 2638;

		case 908:
			return 131;
		case 909:
			return 146;
		case 911:
			return 67;

		case 60:
		case 59:
		case 61:
		case 63:
		case 64:
		case 134:
		case 2035: // spider
		case 62:
		case 1009:
			return 5329;

		case 6006: // wolfman
			return 6537;

		case 1612:// banshee
			return 1524;

		case 1648:
		case 1649:
		case 1650:
		case 1651:
		case 1652:
		case 1653:
		case 1654:
		case 1655:
		case 1656:
		case 1657:// crawling hand
			return 1590;

		case 1604:
		case 1605:
		case 1606:
		case 1607:// aberrant specter
			return 1508;

		case 1618:
		case 1619:// bloodveld
			return 1553;

		case 1643:
		case 1644:
		case 1645:
		case 1646:
		case 1647:// infernal mage
			return 2304;

		case 1613:// nechryael
			return 1530;

		case 1610:
		case 1611:// gargoyle
			return 1518;

		case 1615:// abyssal demon
			return 1538;

		case 3406:
		case 1633: // pyrefiend
			return 1580;

		case 1456:// monkey archer
			return 1397;

		case 122:// hobgoblin
			return 167;

		case 1125:// dad
			return 287;

		case 1096:
		case 1097:
		case 1098:
		case 1942:
		case 1108:
		case 1109:
		case 1110:
		case 1111:
		case 1112:
		case 1116:
		case 1117:
		case 1101:// troll
		case 1106:
		case 1095:
			return 287;

		case 49:// hellhound
		case 142:
		case 95:
		case 96:
			return 6576;

		case 74:
		case 75:
		case 76:
			return 5569;

		case 73:
		case 751: // zombie
		case 77:
		case 419:
		case 420:
		case 421:
		case 422:
		case 423:
		case 424:
			return 5569;

		case 5665:
		case 5664:// zombiepirate
			return 5891;

		case 1770:
		case 1771:
		case 1772:
		case 1773:
		case 101:
		case 2678:
		case 2679:
		case 1774:
		case 1775:
		case 1769:
		case 1776:// goblins
			return 6182;

		case 50: // kbd
			return 92;

		case 708: // imp
		case 3062:
			return 172;

		case 81: // cow
		case 397:
			return 5851;

		case 86:
		case 87:
			return 141;
		case 47:// rat
			return 2707;

		case 2620: // tzhaar
		case 2610:
		case 2619:
			return 2607;

		case 1560: // troll
		case 1566:
			return 287;

		case 89:
			return 6377;
		case 133: // unicorns
			return 292;

		case 2033: // rat
		case 748:
			return 141;

		case 2031: // bloodvel
			return 2073;

		case 105:
		case 1195:
		case 1196: // bear
			return 4930;

		case 128: // snake
		case 1479:
			return 278;

		case 132: // monkey
			return 223;

		case 108:// scorpion
		case 1477:
			return 6256;
		case 144:
		case 107:
			return 6256;

		case 90:
		case 91:
		case 5359:
		case 5384:
		case 92:
		case 93: // skeleton
			return 5491;

		case 82:
		case 3064:
		case 4694:
		case 4695:
		case 4696:
		case 4697:
		case 83:
		case 752:
		case 84:
		case 4702:
		case 4703:
		case 4704:
		case 4705: // lesser
			return 67;

		case 941:// green dragon
		case 55:
		case 53:
		case 4669:
		case 4670:
		case 4671:
		case 742:
		case 1589:
		case 54:
		case 52:
			return 92;
		case 6260:
			return 7062;
		case 123: // hobgoblin
		case 3061:
			return 167;
		case 141:
			return 6570;
		case 1593:
			return 6564;
		case 152:
		case 45:
		case 1558: // wolf
		case 1954:
			return 78;

		case 1459:// monkey guard
			return 1404;

		case 78:
		case 412: // bat
			return 4917;

		case 1766:
		case 1767:
			return 0x03E;

		case 1017:
		case 2693:
		case 41: // chicken
			return 5389;

		case 1585:
		case 110:
		case 1582:
		case 1583:
		case 1584:
		case 1586: // giant
		case 4291:
		case 4292:
			return 4673;
		case 111:
		case 4687:
			return 4673;
		case 4690:
		case 4691:
		case 4692:
		case 4693:
		case 117:
		case 116:
		case 112:
		case 1587:
		case 1588:
			return 4653;

		case 2455:
		case 2454:
		case 2456:
		case 1338: // dagannoth
		case 1340:
		case 1342:
			return 1342;

		case 125: // ice warrior
			return 843;

		default:
			return -1;
		}
	}

	/**
	 * Attack delays
	 **/
	public int getNpcDelay(int i) {
		switch (npcs[i].npcType) {
		case 2025:
		case 2028:
			return 7;

		case 6222:
		case 6223:
		case 6206:
		case 6208:
		case 6204:
		case 6225:
		case 6227:
		case 6260:
			return 6;
			// saradomin gw boss
		case 6247:
			return 3;

		case 2745:
			return 8;

		case 2558:
		case 2559:
		case 2560:
		case 2561:
		case 2550:
			return 6;
			// saradomin gw boss
		case 2562:
			return 2;

		default:
			return 5;
		}
	}

	/**
	 * Hit delays
	 **/
	public int getHitDelay(int i) {
		switch (npcs[i].npcType) {
		case 2881:
		case 2882:
		case 3200:
		case 2892:
		case 2894:
			return 3;

		case 2743:
		case 2631:
		case 2558:
		case 2559:
		case 2560:
			return 3;

		case 2745:
			if (npcs[i].attackType == 1 || npcs[i].attackType == 2)
				return 5;
			else
				return 2;

		case 2025:
			return 4;
		case 2028:
			return 3;

		default:
			return 2;
		}
	}

	/**
	 * Npc respawn time
	 **/
	public int getRespawnTime(int i) {
		switch (npcs[i].npcType) {
		case 1158:
		case 1160:
			return -1;
		case 2881:
		case 2882:
		case 2883:
		case 2558:
		case 2559:
		case 2560:
		case 2561:
		case 2562:
		case 2563:
		case 2564:
		case 2550:
		case 2551:
		case 2552:
		case 2553:
			return 100;
		case 6142:
		case 6143:
		case 6144:
		case 6145:
			return 500;
		default:
			return 25;
		}
	}

	/*
	 * public String[] guardRandomTalk = { "We must not fail!",
	 * "Take down the portals", "The Void Knights will not fall!",
	 * "Hail the Void Knights!", "We are beating these scum!" };
	 */

	public void newNPC(int npcType, int x, int y, int heightLevel,
			int WalkingType, int HP, int maxHit, int attack, int defence) {
		// first, search for a free slot
		int slot = -1;
		for (int i = 1; i < maxNPCs; i++) {
			if (npcs[i] == null) {
				slot = i;
				break;
			}
		}

		if (slot == -1)
			return; // no free slot found

		NPC newNPC = new NPC(slot, npcType);
		newNPC.absX = x;
		newNPC.absY = y;
		newNPC.makeX = x;
		newNPC.makeY = y;
		newNPC.heightLevel = heightLevel;
		newNPC.walkingType = WalkingType;
		newNPC.HP = HP;
		newNPC.MaxHP = HP;
		newNPC.maxHit = maxHit;
		newNPC.attack = attack;
		newNPC.defence = defence;
		npcs[slot] = newNPC;
	}

	public void newNPCList(int npcType, String npcName, int combat, int HP) {
		// first, search for a free slot
		int slot = -1;
		for (int i = 0; i < maxListedNPCs; i++) {
			if (NpcList[i] == null) {
				slot = i;
				break;
			}
		}

		if (slot == -1)
			return; // no free slot found

		NPCList newNPCList = new NPCList(npcType);
		newNPCList.npcName = npcName;
		newNPCList.npcCombat = combat;
		newNPCList.npcHealth = HP;
		NpcList[slot] = newNPCList;
	}

	public void handleClipping(int i) {
		NPC npc = npcs[i];
		if (npc.moveX == 1 && npc.moveY == 1) {
			if ((Region
					.getClipping(npc.absX + 1, npc.absY + 1, npc.heightLevel) & 0x12801e0) != 0) {
				npc.moveX = 0;
				npc.moveY = 0;
				npcs[i].walkingType = 1;
				if ((Region
						.getClipping(npc.absX, npc.absY + 1, npc.heightLevel) & 0x1280120) == 0)
					npc.moveY = 1;
				else
					npc.moveX = 1;
			}
		} else if (npc.moveX == -1 && npc.moveY == -1) {
			if ((Region
					.getClipping(npc.absX - 1, npc.absY - 1, npc.heightLevel) & 0x128010e) != 0) {
				npc.moveX = 0;
				npc.moveY = 0;
				npcs[i].walkingType = 1;
				if ((Region
						.getClipping(npc.absX, npc.absY - 1, npc.heightLevel) & 0x1280102) == 0)
					npc.moveY = -1;
				else
					npc.moveX = -1;
			}
		} else if (npc.moveX == 1 && npc.moveY == -1) {
			if ((Region
					.getClipping(npc.absX + 1, npc.absY - 1, npc.heightLevel) & 0x1280183) != 0) {
				npc.moveX = 0;
				npc.moveY = 0;
				npcs[i].walkingType = 1;
				if ((Region
						.getClipping(npc.absX, npc.absY - 1, npc.heightLevel) & 0x1280102) == 0)
					npc.moveY = -1;
				else
					npc.moveX = 1;
			}
		} else if (npc.moveX == -1 && npc.moveY == 1) {
			if ((Region
					.getClipping(npc.absX - 1, npc.absY + 1, npc.heightLevel) & 0x128013) != 0) {
				npc.moveX = 0;
				npc.moveY = 0;
				npcs[i].walkingType = 1;
				if ((Region
						.getClipping(npc.absX, npc.absY + 1, npc.heightLevel) & 0x1280120) == 0)
					npc.moveY = 1;
				else
					npc.moveX = -1;
			}
		} // Checking Diagonal movement.

		if (npc.moveY == -1) {
			if ((Region.getClipping(npc.absX, npc.absY - 1, npc.heightLevel) & 0x1280102) != 0) {
				npc.moveY = 0;
				npcs[i].walkingType = 1;
			}
		} else if (npc.moveY == 1) {
			if ((Region.getClipping(npc.absX, npc.absY + 1, npc.heightLevel) & 0x1280120) != 0) {
				npc.moveY = 0;
				npcs[i].walkingType = 1;
			}
		} // Checking Y movement.
		if (npc.moveX == 1) {
			if ((Region.getClipping(npc.absX + 1, npc.absY, npc.heightLevel) & 0x1280180) != 0) {
				npc.moveX = 0;
				npcs[i].walkingType = 1;
			}
		} else if (npc.moveX == -1) {
			if ((Region.getClipping(npc.absX - 1, npc.absY, npc.heightLevel) & 0x1280108) != 0) {
				npc.moveX = 0;
				npcs[i].walkingType = 1;
			}
		} // Checking X movement.
	}

	public void process() {
		for (int i = 0; i < maxNPCs; i++) {
			if (npcs[i] == null)
				continue;
			npcs[i].clearUpdateFlags();

		}
		/*
		 * if (npcs[i].npcType == 812){ if (Misc.random(10) == 4)
		 * npcs[i].forceChat
		 * (guardRandomTalk[Misc.random3(guardRandomTalk.length)]); }
		 */
		for (int i = 0; i < maxNPCs; i++) {
			if (npcs[i] != null) {
				if (npcs[i].actionTimer > 0) {
					npcs[i].actionTimer--;
				}

				if (npcs[i].freezeTimer > 0) {
					npcs[i].freezeTimer--;
				}

				if (npcs[i].hitDelayTimer > 0) {
					npcs[i].hitDelayTimer--;
				}

				if (npcs[i].hitDelayTimer == 1) {
					npcs[i].hitDelayTimer = 0;
					applyDamage(i);
				}

				if (npcs[i].attackTimer > 0) {
					npcs[i].attackTimer--;
				}

				if (npcs[i].spawnedBy > 0) { // delete summons npc
					if (PlayerHandler.players[npcs[i].spawnedBy] == null
							|| PlayerHandler.players[npcs[i].spawnedBy].heightLevel != npcs[i].heightLevel
							|| PlayerHandler.players[npcs[i].spawnedBy].respawnTimer > 0
							|| !PlayerHandler.players[npcs[i].spawnedBy]
									.goodDistance(
											npcs[i].getX(),
											npcs[i].getY(),
											PlayerHandler.players[npcs[i].spawnedBy]
													.getX(),
											PlayerHandler.players[npcs[i].spawnedBy]
													.getY(), 20)) {

						if (PlayerHandler.players[npcs[i].spawnedBy] != null) {
							for (int o = 0; o < PlayerHandler.players[npcs[i].spawnedBy].barrowsNpcs.length; o++) {
								if (npcs[i].npcType == PlayerHandler.players[npcs[i].spawnedBy].barrowsNpcs[o][0]) {
									if (PlayerHandler.players[npcs[i].spawnedBy].barrowsNpcs[o][1] == 1)
										PlayerHandler.players[npcs[i].spawnedBy].barrowsNpcs[o][1] = 0;
								}
							}
						}
						npcs[i] = null;
					}
				}
				if (npcs[i] == null)
					continue;

				/**
				 * Attacking player
				 **/
				if (isAggressive(i) && !npcs[i].underAttack && !npcs[i].isDead
						&& !switchesAttackers(i)) {
					npcs[i].killerId = getCloseRandomPlayer(i);
				} else if (isAggressive(i) && !npcs[i].underAttack
						&& !npcs[i].isDead && switchesAttackers(i)) {
					npcs[i].killerId = getCloseRandomPlayer(i);
				}

				if (System.currentTimeMillis() - npcs[i].lastDamageTaken > 5000)
					npcs[i].underAttackBy = 0;

				if ((npcs[i].killerId > 0 || npcs[i].underAttack)
						&& !npcs[i].walkingHome && retaliates(npcs[i].npcType)) {
					if (!npcs[i].isDead) {
						int p = npcs[i].killerId;
						if (PlayerHandler.players[p] != null) {
							//castlewars
							if(npcs[i].npcType == 1532)
								return;
							Client c = (Client) PlayerHandler.players[p];
							followPlayer(i, c.playerId);
							if (npcs[i] == null)
								continue;
							if (npcs[i].attackTimer == 0) {
								attackPlayer(c, i);
							}
						} else {
							npcs[i].killerId = 0;
							npcs[i].underAttack = false;
							npcs[i].facePlayer(0);
						}
					}
				}

				/**
				 * Random walking and walking home
				 **/
				if (npcs[i] == null)
					continue;
				if ((!npcs[i].underAttack || npcs[i].walkingHome)
						&& npcs[i].randomWalk && !npcs[i].isDead) {
					npcs[i].facePlayer(0);
					npcs[i].killerId = 0;
					if (npcs[i].spawnedBy == 0) {
						if ((npcs[i].absX > npcs[i].makeX
								+ Config.NPC_RANDOM_WALK_DISTANCE)
								|| (npcs[i].absX < npcs[i].makeX
										- Config.NPC_RANDOM_WALK_DISTANCE)
								|| (npcs[i].absY > npcs[i].makeY
										+ Config.NPC_RANDOM_WALK_DISTANCE)
								|| (npcs[i].absY < npcs[i].makeY
										- Config.NPC_RANDOM_WALK_DISTANCE)) {
							npcs[i].walkingHome = true;
						}
					}

					if (npcs[i].walkingHome && npcs[i].absX == npcs[i].makeX
							&& npcs[i].absY == npcs[i].makeY) {
						npcs[i].walkingHome = false;
					} else if (npcs[i].walkingHome) {
						npcs[i].moveX = GetMove(npcs[i].absX, npcs[i].makeX);
						npcs[i].moveY = GetMove(npcs[i].absY, npcs[i].makeY);
						handleClipping(i);
						npcs[i].getNextNPCMovement(i);
						npcs[i].updateRequired = true;
					}
					if (npcs[i].walkingType == 1) {
						if (Misc.random(3) == 1 && !npcs[i].walkingHome) {
							int MoveX = 0;
							int MoveY = 0;
							int Rnd = Misc.random(9);
							if (Rnd == 1) {
								MoveX = 1;
								MoveY = 1;
							} else if (Rnd == 2) {
								MoveX = -1;
							} else if (Rnd == 3) {
								MoveY = -1;
							} else if (Rnd == 4) {
								MoveX = 1;
							} else if (Rnd == 5) {
								MoveY = 1;
							} else if (Rnd == 6) {
								MoveX = -1;
								MoveY = -1;
							} else if (Rnd == 7) {
								MoveX = -1;
								MoveY = 1;
							} else if (Rnd == 8) {
								MoveX = 1;
								MoveY = -1;
							}

							if (MoveX == 1) {
								if (npcs[i].absX + MoveX < npcs[i].makeX + 1) {
									npcs[i].moveX = MoveX;
								} else {
									npcs[i].moveX = 0;
								}
							}

							if (MoveX == -1) {
								if (npcs[i].absX - MoveX > npcs[i].makeX - 1) {
									npcs[i].moveX = MoveX;
								} else {
									npcs[i].moveX = 0;
								}
							}

							if (MoveY == 1) {
								if (npcs[i].absY + MoveY < npcs[i].makeY + 1) {
									npcs[i].moveY = MoveY;
								} else {
									npcs[i].moveY = 0;
								}
							}

							if (MoveY == -1) {
								if (npcs[i].absY - MoveY > npcs[i].makeY - 1) {
									npcs[i].moveY = MoveY;
								} else {
									npcs[i].moveY = 0;
								}
							}

							@SuppressWarnings("unused")
							int x = (npcs[i].absX + npcs[i].moveX);
							@SuppressWarnings("unused")
							int y = (npcs[i].absY + npcs[i].moveY);
							/*
							 * if (VirtualWorld.I(npcs[i].heightLevel,
							 * npcs[i].absX, npcs[i].absY, x, y, 0))
							 * npcs[i].getNextNPCMovement(i); else {
							 * npcs[i].moveX = 0; npcs[i].moveY = 0; }
							 */
							handleClipping(i);
							npcs[i].getNextNPCMovement(i);
							npcs[i].updateRequired = true;
						}
					}
				}

				if (npcs[i].isDead == true) {
					if (npcs[i].actionTimer == 0 && npcs[i].applyDead == false
							&& npcs[i].needRespawn == false) {
						npcs[i].updateRequired = true;
						npcs[i].facePlayer(0);
						npcs[i].killedBy = getNpcKillerId(i);
						npcs[i].animNumber = getDeadEmote(i); // dead emote
						npcs[i].animUpdateRequired = true;
						npcs[i].freezeTimer = 0;
						npcs[i].applyDead = true;
						npcs[i].actionTimer = 4; // delete time
						resetPlayersInCombat(i);
						killedBrother(i);
					} else if (npcs[i].actionTimer == 0
							&& npcs[i].applyDead == true
							&& npcs[i].needRespawn == false) {
						npcs[i].needRespawn = true;
						npcs[i].actionTimer = getRespawnTime(i); // respawn time
						dropItems(i); // npc drops items!
						appendSlayerExperience(i);
						appendBandosKC(i);
						appendArmadylKC(i);
						appendZamorakKC(i);
						appendSaradominKC(i);
						killedPenance(i);
						killedKQ(i);
						npcs[i].absX = npcs[i].makeX;
						npcs[i].absY = npcs[i].makeY;
						npcs[i].HP = npcs[i].MaxHP;
						npcs[i].animNumber = 0x328;
						npcs[i].updateRequired = true;
						npcs[i].animUpdateRequired = true;
						if (npcs[i].npcType >= 2440 && npcs[i].npcType <= 2446) {
							Server.objectManager.removeObject(npcs[i].absX,
									npcs[i].absY);
						}
						if (npcs[i].npcType == 2745) {
							handleJadDeath(i);
						}
						//castlewars
						if (npcs[i].npcType >= 1532 && npcs[i].npcType <= 1534) {
							handleBarricadeDeath(i);
							continue;
						}
					} else if (npcs[i].actionTimer == 0
							&& npcs[i].needRespawn == true
							&& npcs[i].npcType != 1158) {
						Client player = (Client) PlayerHandler.players[npcs[i].spawnedBy];
						if (player != null) {
							npcs[i] = null;
						} else {
							int old1 = npcs[i].npcType;
							int old2 = npcs[i].makeX;
							int old3 = npcs[i].makeY;
							int old4 = npcs[i].heightLevel;
							int old5 = npcs[i].walkingType;
							int old6 = npcs[i].MaxHP;
							int old7 = npcs[i].maxHit;
							int old8 = npcs[i].attack;
							int old9 = npcs[i].defence;

							npcs[i] = null;
							newNPC(old1, old2, old3, old4, old5, old6, old7,
									old8, old9);
						}
					}
				}
			}
		}
	}

	public boolean getsPulled(int i) {
		switch (npcs[i].npcType) {
		case 6260:
			if (npcs[i].firstAttacker > 0)
				return false;
			break;
		}
		return true;
	}

	public boolean multiAttacks(int i) {
		switch (npcs[i].npcType) {
		case 6222:// bandos?
			return true;

		case 6247:// saradomin?
			if (npcs[i].attackType == 2)
				return true;

		case 6260:// armadyl?
			if (npcs[i].attackType == 1)
				return true;

		case 1158: // kq
			if (npcs[i].attackType == 2)
				return true;
		case 1160: // kq
			if (npcs[i].attackType == 1)
				return true;
		default:
			return false;
		}

	}

	/**
	 * Npc killer id?
	 **/

	public int getNpcKillerId(int npcId) {
		int oldDamage = 0;
		@SuppressWarnings("unused")
		int count = 0;
		int killerId = 0;
		for (int p = 1; p < Config.MAX_PLAYERS; p++) {
			if (PlayerHandler.players[p] != null) {
				if (PlayerHandler.players[p].lastNpcAttacked == npcId) {
					if (PlayerHandler.players[p].totalDamageDealt > oldDamage) {
						oldDamage = PlayerHandler.players[p].totalDamageDealt;
						killerId = p;
					}
					PlayerHandler.players[p].totalDamageDealt = 0;
				}
			}
		}
		return killerId;
	}

	/**
	 * 
	 */

	private void killedKQ(int i) {
		Client c = (Client) PlayerHandler.players[npcs[i].killedBy];
		if (c != null) {
			switch (npcs[i].npcType) {
			case 1158:
				KalphiteQueen.spawnSecondForm(c, i);
				break;
			case 1160:
				KalphiteQueen.spawnFirstForm(c, i);
				break;
			}
		}
	}

	private void killedPenance(int i) {
		Client c = (Client) PlayerHandler.players[npcs[i].killedBy];
		if (c != null) {
			switch (npcs[i].npcType) {
			case 5247:
				c.assaultPoints += 4;
				break;
			case 5219:
				c.assaultPoints += 2;
				break;
			case 5213:
				c.assaultPoints += 1;
				break;
			}
			c.getPA().sendFrame126("@red@[@or1@Assault@red@] Points: @or2@"+c.assaultPoints, 7332);
		}
	}

	public void killedBrother(int i) {
		Client c = (Client) PlayerHandler.players[npcs[i].killedBy];
		if (c != null) {
			switch (npcs[i].npcType) {
			case 2025:
				c.killedBrother[0] = true;
				c.barrowsKill++;
				break;
			case 2026:
				c.killedBrother[2] = true;
				c.barrowsKill++;
				break;
			case 2027:
				c.killedBrother[5] = true;
				c.barrowsKill++;
				break;
			case 2028:
				c.killedBrother[1] = true;
				c.barrowsKill++;
				break;
			case 2029:
				c.killedBrother[4] = true;
				c.barrowsKill++;
				break;
			case 2030:
				c.killedBrother[3] = true;
				c.barrowsKill++;
				break;
			}
			if(i == c.hiddenBrother) {
				c.spawnedFinalBrother = false;
			}
		}
	}
	
	//castlewars
	public void handleBarricadeDeath(int i) {
		for (int j = 0; j < CastleWars.BARRICADE_INDEX.length; j++) {
			if (CastleWars.BARRICADE_INDEX[j] == i) {
				CastleWars.BARRICADE_INDEX[j] = 0;
				npcs[i] = null;
			}
		}
		if(npcs[i] != null) {
			if(npcs[i].team == 1)
				Player.saraBarricades--;
			else
				Player.zammyBarricades--;
		}
	}

	public void handleJadDeath(int i) {
		Client c = (Client) PlayerHandler.players[npcs[i].spawnedBy];
		c.getItems().addItem(6570, 1);
		c.getItems().addItem(6529, 5000 + Misc.random(10000));
		c.getDH().sendDialogues(69, 2617);
		c.getPA().resetTzhaar();
		c.waveId = 300;
	}

	/**
	 * Dropping Items!
	 **/

	public void dropItems(int i) {
		boolean ringS = false;
		@SuppressWarnings("unused")
		int npc = 0;
		Client c = (Client) PlayerHandler.players[npcs[i].killedBy];
		if (c != null) {
			if (npcs[i].npcType == 912 || npcs[i].npcType == 913
					|| npcs[i].npcType == 914)
				c.magePoints += 1;

			if (npcDropList[NPCHandler.npcs[i].npcType] != null) {
				for (NPCDrop drop : npcDropList[NPCHandler.npcs[i].npcType]) {
					if (NPCHandler.npcs[i].npcType == drop.npcId) {
						float dropRarity = getRarity(drop.rarity);
						int rareChance = Misc.random(100.0f);
						if(c.expModifier == c.EASY)
							rareChance *= 1.5;
						if(c.expModifier == c.HARD)
							rareChance *= 0.95;
						if(c.expModifier == c.EXTREME)
							rareChance *= 0.85;
					    if(c.playerEquipment[c.playerRing] == 2572) {
					    	rareChance = (rareChance/2);
					    	if(!ringS)
					    		c.sendMessage("@whi@Your ring glows brightly.");
					    	ringS = true;
					    }
						if (Misc.random(rareChance) <= dropRarity) {
							if (drop.itemAmount2 != -1) {
								int itemAmount = drop.itemAmount;
								int itemAmount2 = drop.itemAmount2;
								if (drop.itemAmount > drop.itemAmount2) {
									itemAmount2 = drop.itemAmount;
									itemAmount = drop.itemAmount2;
								}
								int amount = Misc.random(itemAmount,
										itemAmount2);
								Server.itemHandler.createGroundItem(c,
										drop.itemId, NPCHandler.npcs[i].absX,
										NPCHandler.npcs[i].absY, amount,
										c.playerId);
							} else {
								Server.itemHandler.createGroundItem(c,
										drop.itemId, NPCHandler.npcs[i].absX,
										NPCHandler.npcs[i].absY,
										drop.itemAmount, c.playerId);
							}
						}
					}
				}
				ringS = false;
			}
			/*
			 * if (Misc.random(4000) == 0) {
			 * Server.itemHandler.createGroundItem(c, 1050,
			 * NPCHandler.npcs[i].absX, NPCHandler.npcs[i].absY, 1, c.playerId);
			 * c
			 * .sendMessage("@red@Congratulations!! You got the special drop!");
			 * }
			 */
			if (Misc.random(500) == 0) {
				Server.itemHandler.createGroundItem(c, Misc.random(1) == 0 ? 985 : 987,
						NPCHandler.npcs[i].absX, NPCHandler.npcs[i].absY, 1,
						c.playerId);
			}
			int clueE = (2677+Misc.random(7));
			int clueM = (2685+Misc.random(7));
			int clueH = (2693+Misc.random(7));
			if (Misc.random(500) == 0) {
				Server.itemHandler.createGroundItem(c, clueE,
						NPCHandler.npcs[i].absX, NPCHandler.npcs[i].absY, 1,
						c.playerId);
			}
			if (Misc.random(750) == 0) {
				Server.itemHandler.createGroundItem(c, clueM,
						NPCHandler.npcs[i].absX, NPCHandler.npcs[i].absY, 1,
						c.playerId);
			}
			if (Misc.random(1000) == 0) {
				Server.itemHandler.createGroundItem(c, clueH,
						NPCHandler.npcs[i].absX, NPCHandler.npcs[i].absY, 1,
						c.playerId);
			}
			// if(c.clanId >= 0) {
			// ClanChatHandler.handleLootShare(c,
			// ((int[][])NPCDrops.rareDrops.get(Integer.valueOf(npcs[i].npcType)))[random][0],
			// ((int[][])NPCDrops.rareDrops.get(Integer.valueOf(npcs[i].npcType)))[random][1]);
			// }
		}
	}

	// id of bones dropped by npcs
	public int boneDrop(int type) {
		switch (type) {
		case 1:// normal bones
		case 9:
		case 100:
		case 12:
		case 17:
		case 803:
		case 18:
		case 81:
		case 101:
		case 41:
		case 19:
		case 90:
		case 75:
		case 86:
		case 78:
		case 912:
		case 913:
		case 914:
		case 1648:
		case 1643:
		case 1618:
		case 1624:
		case 1625:
		case 181:
		case 119:
		case 49:
		case 26:
		case 1341:
			return 526;
		case 117:
			return 532;// big bones
		case 50:// drags
		case 53:
		case 54:
		case 55:
		case 941:
		case 1590:
		case 1591:
		case 1592:
			return 536;
		case 84:
		case 1615:
		case 1613:
		case 82:
		case 3200:
			return 592;
		case 2881:
		case 2882:
		case 2883:
			return 6729;
		default:
			return -1;
		}
	}

	public int getStackedDropAmount(int itemId, int npcId) {
		switch (itemId) {
		case 995:
			switch (npcId) {
			case 1:
				return 50 + Misc.random(50);
			case 9:
				return 133 + Misc.random(100);
			case 1624:
				return 1000 + Misc.random(300);
			case 1618:
				return 1000 + Misc.random(300);
			case 1643:
				return 1000 + Misc.random(300);
			case 1610:
				return 1000 + Misc.random(1000);
			case 1613:
				return 1500 + Misc.random(1250);
			case 1615:
				return 3000;
			case 18:
				return 500;
			case 101:
				return 60;
			case 913:
			case 912:
			case 914:
				return 750 + Misc.random(500);
			case 1612:
				return 250 + Misc.random(500);
			case 1648:
				return 250 + Misc.random(250);
			case 90:
				return 200;
			case 82:
				return 1000 + Misc.random(455);
			case 52:
				return 400 + Misc.random(200);
			case 49:
				return 1500 + Misc.random(2000);
			case 1341:
				return 1500 + Misc.random(500);
			case 26:
				return 500 + Misc.random(100);
			case 20:
				return 750 + Misc.random(100);
			case 21:
				return 890 + Misc.random(125);
			case 117:
				return 500 + Misc.random(250);
			case 2607:
				return 500 + Misc.random(350);
			}
			break;
		case 11212:
			return 10 + Misc.random(4);
		case 565:
		case 561:
			return 10;
		case 560:
		case 563:
		case 562:
			return 15;
		case 555:
		case 554:
		case 556:
		case 557:
			return 20;
		case 892:
			return 40;
		case 886:
			return 100;
		case 6522:
			return 6 + Misc.random(5);

		}

		return 1;
	}

	/**
	 * Slayer Experience
	 **/
	public void appendSlayerExperience(int i) {
		@SuppressWarnings("unused")
		int npc = 0;
		Client c = (Client) PlayerHandler.players[npcs[i].killedBy];
		if (c != null) {
			if (c.getSlayer().isSlayerTask(npcs[i].npcType)) {
				c.taskAmount--;
				
				c.getPA().addSkillXP(npcs[i].MaxHP * Config.SLAYER_EXPERIENCE,
						18);
				
				if (c.taskAmount <= 0) {
					
					c.getPA().addSkillXP(
							(npcs[i].MaxHP * 8) * Config.SLAYER_EXPERIENCE, 18);
					
					int points = c.getSlayer().getDifficulty(c.slayerTask) * 4;
					c.slayerTask = -1;
					c.slayerPoints += points;
					c.getPA().sendFrame126("@red@[@or1@Slayer@red@] Points: @or2@"+c.slayerPoints, 7383);
					c.sendMessage("You completed your slayer task. You obtain "
							+ points
							+ " slayer points. Please talk to Vanakka.");
				}
			}
		}
	}

	/**
	 * Resets players in combat
	 */

	public void resetPlayersInCombat(int i) {
		for (int j = 0; j < PlayerHandler.players.length; j++) {
			if (PlayerHandler.players[j] != null)
				if (PlayerHandler.players[j].underAttackBy2 == i)
					PlayerHandler.players[j].underAttackBy2 = 0;
		}
	}

	/**
	 * Npc names
	 **/

	public String getNpcName(int npcId) {
		for (int i = 0; i < maxNPCs; i++) {
			if (NPCHandler.NpcList[i] != null) {
				if (NPCHandler.NpcList[i].npcId == npcId) {
					return NPCHandler.NpcList[i].npcName;
				}
			}
		}
		return "-1";
	}

	/**
	 * Npc Follow Player
	 **/

	public int GetMove(int i, int Place1, int Place2) {
		if ((Place1 - Place2) == 0) {
			npcs[i].walkingType = 1;
			return 0;
		} else if ((Place1 - Place2) < 0) {
			return 1;
		} else if ((Place1 - Place2) > 0) {
			return -1;
		}
		npcs[i].walkingType = 1;
		return 0;
	}
	
	public int GetMove(int Place1, int Place2) {
		if ((Place1 - Place2) == 0) {
			return 0;
		} else if ((Place1 - Place2) < 0) {
			return 1;
		} else if ((Place1 - Place2) > 0) {
			return -1;
		}
		return 0;
	}

	public boolean followPlayer(int i) {
		switch (npcs[i].npcType) {
		case 2892:
		case 2894:
			return false;
		}
		return true;
	}

	public void followPlayer(int i, int playerId) {
		if (PlayerHandler.players[playerId] == null) {
			return;
		}
		if (PlayerHandler.players[playerId].respawnTimer > 0) {
			npcs[i].facePlayer(0);
			npcs[i].randomWalk = true;
			npcs[i].underAttack = false;
			return;
		}

		if (!followPlayer(i)) {
			npcs[i].facePlayer(playerId);
			return;
		}

		int playerX = PlayerHandler.players[playerId].absX;
		int playerY = PlayerHandler.players[playerId].absY;
		npcs[i].randomWalk = false;
		if (goodDistance(npcs[i].getX(), npcs[i].getY(), playerX, playerY,
				distanceRequired(i)))
			return;
		if ((npcs[i].spawnedBy > 0)
				|| ((npcs[i].absX < npcs[i].makeX + Config.NPC_FOLLOW_DISTANCE)
						&& (npcs[i].absX > npcs[i].makeX
								- Config.NPC_FOLLOW_DISTANCE)
						&& (npcs[i].absY < npcs[i].makeY
								+ Config.NPC_FOLLOW_DISTANCE) && (npcs[i].absY > npcs[i].makeY
						- Config.NPC_FOLLOW_DISTANCE))) {
			if (npcs[i].heightLevel == PlayerHandler.players[playerId].heightLevel) {
				if (PlayerHandler.players[playerId] != null && npcs[i] != null) {
					if (playerY < npcs[i].absY) {
						npcs[i].moveX = GetMove(i, npcs[i].absX, playerX);
						npcs[i].moveY = GetMove(i, npcs[i].absY, playerY);
					} else if (playerY > npcs[i].absY) {
						npcs[i].moveX = GetMove(i, npcs[i].absX, playerX);
						npcs[i].moveY = GetMove(i, npcs[i].absY, playerY);
					} else if (playerX < npcs[i].absX) {
						npcs[i].moveX = GetMove(i, npcs[i].absX, playerX);
						npcs[i].moveY = GetMove(i, npcs[i].absY, playerY);
					} else if (playerX > npcs[i].absX) {
						npcs[i].moveX = GetMove(i, npcs[i].absX, playerX);
						npcs[i].moveY = GetMove(i, npcs[i].absY, playerY);
					} else if (playerX == npcs[i].absX
							|| playerY == npcs[i].absY) {
						int o = Misc.random(3);
						switch (o) {
						case 0:
							npcs[i].moveX = GetMove(i, npcs[i].absX, playerX);
							npcs[i].moveY = GetMove(i, npcs[i].absY, playerY + 1);
							break;

						case 1:
							npcs[i].moveX = GetMove(i, npcs[i].absX, playerX);
							npcs[i].moveY = GetMove(i, npcs[i].absY, playerY - 1);
							break;

						case 2:
							npcs[i].moveX = GetMove(i, npcs[i].absX, playerX + 1);
							npcs[i].moveY = GetMove(i, npcs[i].absY, playerY);
							break;

						case 3:
							npcs[i].moveX = GetMove(i, npcs[i].absX, playerX - 1);
							npcs[i].moveY = GetMove(i, npcs[i].absY, playerY);
							break;
						}
					}
					@SuppressWarnings("unused")
					int x = (npcs[i].absX + npcs[i].moveX);
					@SuppressWarnings("unused")
					int y = (npcs[i].absY + npcs[i].moveY);
					npcs[i].facePlayer(playerId);
					handleClipping(i);
					npcs[i].getNextNPCMovement(i);
					npcs[i].facePlayer(playerId);
					npcs[i].updateRequired = true;
				}
			}
		} else {
			npcs[i].facePlayer(0);
			npcs[i].randomWalk = true;
			npcs[i].underAttack = false;
		}
	}

	/**
	 * load spell
	 **/
	public void loadSpell2(int i) {
		npcs[i].attackType = 3;
		int random = Misc.random(3);
		if (random == 0) {
			npcs[i].projectileId = 393; // red
			npcs[i].endGfx = 430;
		} else if (random == 1) {
			npcs[i].projectileId = 394; // green
			npcs[i].endGfx = 429;
		} else if (random == 2) {
			npcs[i].projectileId = 395; // white
			npcs[i].endGfx = 431;
		} else if (random == 3) {
			npcs[i].projectileId = 396; // blue
			npcs[i].endGfx = 428;
		}
	}

	public int random;
	public int random2;
	public Client c;

	public NPCHandler(Client client) {
		this.c = client;
	}

	public void loadSpell(int i) {
		@SuppressWarnings("unused")
		Client c = (Client) PlayerHandler.players[npcs[i].oldIndex];
		switch (npcs[i].npcType) {
		case 6263:
			npcs[i].attackType = 2; // Magic
			npcs[i].projectileId = 1203;
			break;
		case 6265:
			npcs[i].attackType = 1; // Range
			npcs[i].projectileId = 1206;
			break;
		case 1158:// kq first form
			int kqRandom = Misc.random(3);
			if (kqRandom == 2) {
				npcs[i].projectileId = 280; // gfx
				npcs[i].attackType = 2;
				npcs[i].endGfx = 279;
			} else {
				npcs[i].endGfx = -1;
				npcs[i].projectileId = -1;
				npcs[i].attackType = 0;
			}
			break;
		case 1160:// kq secondform
			int kqRandom2 = Misc.random(3);
			if (kqRandom2 == 2) {
				npcs[i].projectileId = 279; // gfx
				npcs[i].attackType = 1 + Misc.random(1);
				npcs[i].endGfx = 278;
			} else {
				npcs[i].endGfx = -1;
				npcs[i].projectileId = -1;
				npcs[i].attackType = 0;

			}
			break;
		case 795:
			random = Misc.random(4);
			if (random == 0 || random == 1 || random == 2 || random == 3) {
				npcs[i].endGfx = -1;
				npcs[i].projectileId = -1;
				npcs[i].attackType = 0;
			} else {
				// c.gfx0(369);
				npcs[i].freezeTimer = 10;
				// c.freezeTimer = 10;
				npcs[i].attackType = 1;
				// npcs[i].forceChat("Freeze, feel the power of ice!");
				// c.sendMessage("@red@Ice Queen: @dbl@Freeze, feel the power of ice!");
				npcs[i].forceChat("Hereby, I unleash my power of ice!");
				// c.sendMessage("@red@Ice queen: @dbl@Hereby, I unleash my power of ice!");
			}
			break;
		case 2892:
			npcs[i].projectileId = 94;
			npcs[i].attackType = 2;
			npcs[i].endGfx = 95;
			break;
		case 2894:
			npcs[i].projectileId = 298;
			npcs[i].attackType = 1;
			break;
		case 53:
		case 54:
		case 55:
		case 941:
		case 1591:
		case 1590:
		case 1592:
		case 5363:
			int randoma = Misc.random(2);
			if (randoma == 0) {
				npcs[i].projectileId = 393; // red
				npcs[i].endGfx = 430;
				npcs[i].attackType = 3;
			} else if (randoma == 1) {
				npcs[i].projectileId = -1; // melee
				npcs[i].endGfx = -1;
				npcs[i].attackType = 0;
			} else if (randoma == 2) {
				npcs[i].projectileId = -1; // melee
				npcs[i].endGfx = -1;
				npcs[i].attackType = 0;
			}
			break;
		case 50:
			int random = Misc.random(8);
			if (random == 0) {
				npcs[i].projectileId = 393; // red
				npcs[i].endGfx = 430;
				npcs[i].attackType = 3;
			} else if (random == 1) {
				npcs[i].projectileId = 394; // green
				npcs[i].endGfx = 429;
				npcs[i].attackType = 3;
			} else if (random == 2) {
				npcs[i].projectileId = 395; // white
				npcs[i].endGfx = 431;
				npcs[i].attackType = 3;
			} else if (random == 3) {
				npcs[i].projectileId = 396; // blue
				npcs[i].endGfx = 428;
				npcs[i].attackType = 3;
			} else if (random == 4) {
				npcs[i].projectileId = -1; // melee
				npcs[i].endGfx = -1;
				npcs[i].attackType = 0;
			} else if (random == 5) {
				npcs[i].projectileId = -1; // melee
				npcs[i].endGfx = -1;
				npcs[i].attackType = 0;
			} else if (random == 6) {
				npcs[i].projectileId = -1; // melee
				npcs[i].endGfx = -1;
				npcs[i].attackType = 0;
			} else if (random == 7) {
				npcs[i].projectileId = -1; // melee
				npcs[i].endGfx = -1;
				npcs[i].attackType = 0;
			} else if (random == 8) {
				npcs[i].projectileId = -1; // melee
				npcs[i].endGfx = -1;
				npcs[i].attackType = 0;
			}
			break;
		// arma npcs
		case 6227:// kilisa
			npcs[i].attackType = 0;
			break;
		case 6225:// geerin
		case 6233:
		case 6230:
			npcs[i].attackType = 1;
			npcs[i].projectileId = 1190;
			break;
		case 6239:
			npcs[i].attackType = 1;
			npcs[i].projectileId = 1191;
			break;
		case 6232:
			npcs[i].attackType = 1;
			npcs[i].projectileId = 1191;
			break;
		case 6276:
			npcs[i].attackType = 1;
			npcs[i].projectileId = 1195;
			break;
		case 6223:// skree
			npcs[i].attackType = 2;
			npcs[i].projectileId = 1199;
			break;
		case 6257:// saradomin strike
			npcs[i].attackType = 2;
			npcs[i].endGfx = 76;
			break;
		case 6221:// zamorak strike
			npcs[i].attackType = 2;
			npcs[i].endGfx = 78;
			break;
		case 6231:// arma
			npcs[i].attackType = 2;
			npcs[i].projectileId = 1199;
			break;
		case 6222:// kree
			random = Misc.random(1);
			npcs[i].attackType = 1 + random;
			if (npcs[i].attackType == 1) {
				npcs[i].projectileId = 1197;
			} else {
				npcs[i].attackType = 2;
				npcs[i].projectileId = 1198;
			}
			break;
		// sara npcs
		case 6203:
			random = Misc.random(2);
			if (random == 0 || random == 1) {
				npcs[i].attackType = 0;
				npcs[i].projectileId = -1;
			} else {
				npcs[i].attackType = 2;
				npcs[i].projectileId = 1211;
			}
			break;
		case 6247: // sara
			random = Misc.random(1);
			if (random == 0) {
				npcs[i].attackType = 2;
				npcs[i].endGfx = 1224;
				npcs[i].projectileId = -1;
			} else if (random == 1)
				npcs[i].attackType = 0;
			break;
		case 6248: // star
			npcs[i].attackType = 0;
			break;
		case 6250: // growler
			npcs[i].attackType = 2;
			npcs[i].projectileId = 1203;
			break;
		case 6252: // bree
			npcs[i].attackType = 1;
			npcs[i].projectileId = 9;
			break;
		// bandos npcs
		case 6260:// bandos
			random = Misc.random(2);
			if (random == 0 || random == 1) {
				npcs[i].attackType = 0;
			} else {
				npcs[i].attackType = 1;
				npcs[i].projectileId = 1200;
			}
			break;
		case 3847:
			npcs[i].attackType = 2;
			npcs[i].projectileId = 162;
			npcs[i].endGfx = 163;
			break;
		case 2025:
			npcs[i].attackType = 2;
			int r = Misc.random(3);
			if (r == 0) {
				npcs[i].gfx100(158);
				npcs[i].projectileId = 159;
				npcs[i].endGfx = 160;
			}
			if (r == 1) {
				npcs[i].gfx100(161);
				npcs[i].projectileId = 162;
				npcs[i].endGfx = 163;
			}
			if (r == 2) {
				npcs[i].gfx100(164);
				npcs[i].projectileId = 165;
				npcs[i].endGfx = 166;
			}
			if (r == 3) {
				npcs[i].gfx100(155);
				npcs[i].projectileId = 156;
			}
			break;
		case 2881:// supreme
			npcs[i].attackType = 1;
			npcs[i].projectileId = 298;
			break;

		case 2882:// prime
			npcs[i].attackType = 2;
			npcs[i].projectileId = 162;
			npcs[i].endGfx = 477;
			break;

		case 2028:
			npcs[i].attackType = 1;
			npcs[i].projectileId = 27;
			break;

		case 3200:
			int r2 = Misc.random(1);
			if (r2 == 0) {
				npcs[i].attackType = 1;
				npcs[i].gfx100(550);
				npcs[i].projectileId = 551;
				npcs[i].endGfx = 552;
			} else {
				npcs[i].attackType = 2;
				npcs[i].gfx100(553);
				npcs[i].projectileId = 554;
				npcs[i].endGfx = 555;
			}
			break;
		case 2745:
			int r3 = 0;
			if (goodDistance(npcs[i].absX, npcs[i].absY,
					PlayerHandler.players[npcs[i].spawnedBy].absX,
					PlayerHandler.players[npcs[i].spawnedBy].absY, 1))
				r3 = Misc.random(2);
			else
				r3 = Misc.random(1);
			if (r3 == 0) {
				npcs[i].attackType = 2;
				npcs[i].endGfx = 157;
				npcs[i].projectileId = 448;
			} else if (r3 == 1) {
				npcs[i].attackType = 1;
				npcs[i].endGfx = 451;
				npcs[i].projectileId = -1;
			} else if (r3 == 2) {
				npcs[i].endGfx = -1;
				npcs[i].projectileId = -1;
			}
			break;
		case 2743:
			npcs[i].attackType = 2;
			npcs[i].projectileId = 445;
			npcs[i].endGfx = 446;
			break;

		case 2631:
			npcs[i].attackType = 1;
			npcs[i].projectileId = 443;
			break;
		}
	}

	/**
	 * Distanced required to attack
	 **/
	public int distanceRequired(int i) {
		switch (npcs[i].npcType) {
		case 2025:
		case 2028:
			return 6;
		case 50:
		case 2562:
			return 2;
		case 2881:// dag kings
		case 2882:
		case 3200:// chaos ele
		case 2743:
		case 2631:
		case 2745:
			return 8;
		case 2883:// rex
			return 1;
		case 2552:
		case 2553:
		case 2556:
		case 2557:
		case 2558:
		case 2559:
		case 2560:
		case 2564:
		case 2565:
			return 9;
			// things around dags
		case 2892:
		case 2894:
			return 10;
		default:
			return 1;
		}
	}

	public int followDistance(int i) {
		switch (npcs[i].npcType) {
		case 2550:
		case 2551:
		case 2562:
		case 2563:
			return 8;
		case 2883:
			return 4;
		case 2881:
		case 2882:
			return 1;

		}
		return 0;

	}

	public int getProjectileSpeed(int i) {
		switch (npcs[i].npcType) {
		case 2881:
		case 2882:
		case 3200:
			return 85;

		case 2745:
			return 130;

		case 50:
			return 90;

		case 2025:
			return 85;

		case 2028:
			return 80;

		default:
			return 85;
		}
	}

	/**
	 * NPC Attacking Player
	 **/

	public void attackPlayer(Client c, int i) {
		if (npcs[i] != null) {
			if (npcs[i].isDead)
				return;
			if (!npcs[i].inMulti() && npcs[i].underAttackBy > 0
					&& npcs[i].underAttackBy != c.playerId) {
				npcs[i].killerId = 0;
				return;
			}
			if (!npcs[i].inMulti()
					&& (c.underAttackBy > 0 || (c.underAttackBy2 > 0 && c.underAttackBy2 != i))) {
				npcs[i].killerId = 0;
				return;
			}
			if (npcs[i].heightLevel != c.heightLevel) {
				npcs[i].killerId = 0;
				return;
			}
			npcs[i].facePlayer(c.playerId);
			boolean special = false;// specialCase(c,i);
			if (goodDistance(npcs[i].getX(), npcs[i].getY(), c.getX(),
					c.getY(), distanceRequired(i)) || special) {
				if (c.respawnTimer <= 0) {
					npcs[i].facePlayer(c.playerId);
					npcs[i].attackTimer = getNpcDelay(i);
					npcs[i].hitDelayTimer = getHitDelay(i);
					npcs[i].attackType = 0;
					if (special)
						loadSpell2(i);
					else
						loadSpell(i);
					if (npcs[i].attackType == 3)
						npcs[i].hitDelayTimer += 2;
					if (multiAttacks(i)) {
						multiAttackGfx(i, npcs[i].projectileId);
						startAnimation(getAttackEmote(i), i);
						npcs[i].oldIndex = c.playerId;
						return;
					}
					if (npcs[i].projectileId > 0) {
						int nX = NPCHandler.npcs[i].getX() + offset(i);
						int nY = NPCHandler.npcs[i].getY() + offset(i);
						int pX = c.getX();
						int pY = c.getY();
						int offX = (nY - pY) * -1;
						int offY = (nX - pX) * -1;
						c.getPA().createPlayersProjectile(nX, nY, offX, offY,
								50, getProjectileSpeed(i),
								npcs[i].projectileId, 43, 31, -c.getId() - 1,
								65);
					}
					c.underAttackBy2 = i;
					c.singleCombatDelay2 = System.currentTimeMillis();
					npcs[i].oldIndex = c.playerId;
					startAnimation(getAttackEmote(i), i);
					c.getPA().removeAllWindows();
				}
			}
		}
	}

	public int offset(int i) {
		switch (npcs[i].npcType) {
		case 50:
			return 2;
		case 2881:
		case 2882:
			return 1;
		case 2745:
		case 2743:
			return 1;
		}
		return 0;
	}

	public boolean specialCase(Client c, int i) { // responsible for npcs that
													// much
		if (goodDistance(npcs[i].getX(), npcs[i].getY(), c.getX(), c.getY(), 8)
				&& !goodDistance(npcs[i].getX(), npcs[i].getY(), c.getX(),
						c.getY(), distanceRequired(i)))
			return true;
		return false;
	}

	public boolean retaliates(int npcType) {
		return npcType < 6142 || npcType > 6145
				&& !(npcType >= 2440 && npcType <= 2446);
	}

	public void appendBandosKC(int i) {
		Client c = (Client) PlayerHandler.players[npcs[i].killedBy];
		if (c != null) {
			int[] bandosGodKC = { 6271, 6272, 6273, 6274, 6275, 6268, 122,
					6279, 6280, 6281, 6282, 6283, 6269, 6270, 6276, 6277, 6278 };
			for (int j : bandosGodKC) {
				if (npcs[i].npcType == j) {
					if (c.bandosKills < 15) {
						c.bandosKills++;
						c.getPA().sendFrame126("@cya@" + c.bandosKills, 16217);
						// c.sendMessage("Bandos Killcount: " + c.bandosGwdKC);
					}

					break;
				}
			}
		}
	}

	public void appendArmadylKC(int i) {
		Client c = (Client) PlayerHandler.players[npcs[i].killedBy];
		if (c != null) {
			int[] armaGodKC = { 6229, 6230, 6231, 6232, 6233, 6234, 6235, 6236,
					6237, 6238, 6239, 6240, 6241, 6242, 6243, 6244, 6245, 6246,
					275, 274, };
			for (int j : armaGodKC) {
				if (npcs[i].npcType == j) {
					if (c.armaKills < 15) {
						c.armaKills++;
						c.getPA().sendFrame126("@cya@" + c.armaKills, 16216);
						// c.sendMessage("Armadyl Killcount: " + c.armaGwdKC);
					}

					break;
				}
			}
		}
	}

	public void appendSaradominKC(int i) {
		Client c = (Client) PlayerHandler.players[npcs[i].killedBy];
		if (c != null) {
			int[] saraGodKC = { 6254, 6255, 6256, 6257, 6258, 6259, 96, 97,
					111, 125, 913 };
			for (int j : saraGodKC) {
				if (npcs[i].npcType == j) {
					if (c.saraKills < 15) {
						c.saraKills++;
						c.getPA().sendFrame126("@cya@" + c.saraKills, 16218);
						// c.sendMessage("Saradomin Killcount: " + c.saraGwdKC);
					}

					break;
				}
			}
		}
	}

	public void appendZamorakKC(int i) {
		Client c = (Client) PlayerHandler.players[npcs[i].killedBy];
		if (c != null) {
			int[] zamGodKC = { 6210, 6211, 6212, 6214, 6218, 49, 82, 83, 84,
					94, 92, 75, 78, 912 };
			for (int j : zamGodKC) {
				if (npcs[i].npcType == j) {
					if (c.zamorakKills < 15) {
						c.zamorakKills++;
						c.getPA().sendFrame126("@cya@" + c.zamorakKills, 16219);
						// c.sendMessage("Zamorak Killcount: " + c.zamGwdKC);
					}

					break;
				}
			}
		}
	}

	public void applyDamage(int i) {
		if (npcs[i] != null) {
			if (PlayerHandler.players[npcs[i].oldIndex] == null) {
				return;
			}
			if (npcs[i].isDead)
				return;
			Client c = (Client) PlayerHandler.players[npcs[i].oldIndex];
			if (multiAttacks(i)) {
				multiAttackDamage(i);
				return;
			}
			if (c.playerIndex <= 0 && c.npcIndex <= 0)
				if (c.autoRet == 1)
					c.npcIndex = i;
			if (c.attackTimer <= 3 || c.attackTimer == 0 && c.npcIndex == 0
					&& c.oldNpcIndex == 0) {
				c.startAnimation(c.getCombat().getBlockEmote());
			}
			if (c.respawnTimer <= 0) {
				int damage = 0;
				if (npcs[i].attackType == 0) {
					damage = Misc.random(npcs[i].maxHit);
					if (damage > 0)
						c.getCombat().applyRecoilNPC(damage, i);
					if (10 + Misc.random(c.getCombat().calculateMeleeDefence()) > Misc
							.random(NPCHandler.npcs[i].attack)) {
						damage = 0;
					}
					if (c.prayerActive[18] || c.curseActive[9]) { // protect
																	// from
																	// melee
						if (npcs[i].npcType == 2030 || npcs[i].npcType == 1158
								|| npcs[i].npcType == 1160)
							damage = (damage / 2);
						else
							damage = 0;
					}
					if (c.playerLevel[3] - damage < 0) {
						damage = c.playerLevel[3];
					}
				}

				if (npcs[i].attackType == 1) { // range
					damage = Misc.random(npcs[i].maxHit);
					if (damage > 0)
						c.getCombat().applyRecoilNPC(damage, i);
					if (10 + Misc.random(c.getCombat().calculateRangeDefence()) > Misc
							.random(NPCHandler.npcs[i].attack)) {
						damage = 0;
					}
					if (c.prayerActive[17]) { // protect from range
						damage = 0;
					}
					if (c.playerLevel[3] - damage < 0) {
						damage = c.playerLevel[3];
					}
				}

				if (npcs[i].attackType == 2) { // magic
					damage = Misc.random(npcs[i].maxHit);
					if (damage > 0)
						c.getCombat().applyRecoilNPC(damage, i);
					boolean magicFailed = false;
					if (10 + Misc.random(c.getCombat().mageDef()) > Misc
							.random(NPCHandler.npcs[i].attack)) {
						damage = 0;
						magicFailed = true;
					}
					if (c.prayerActive[16]) { // protect from magic
						damage = 0;
						magicFailed = true;
					}
					if (c.playerLevel[3] - damage < 0) {
						damage = c.playerLevel[3];
					}
					if (npcs[i].endGfx > 0
							&& (!magicFailed || isFightCaveNpc(i))) {
						c.gfx100(npcs[i].endGfx);
					} else {
						c.gfx100(85);
					}
				}

				if (npcs[i].attackType == 3) { // fire breath
					int anti = c.getPA().antiFire();
					if (anti == 0) {
						damage = Misc.random(30) + 10;
						c.sendMessage("You are badly burnt by the dragon's fire!");
					} else if (anti == 1)
						damage = Misc.random(12);
					else if (anti == 2)
						damage = Misc.random(5);
					if (c.playerLevel[3] - damage < 0)
						damage = c.playerLevel[3];
					c.gfx100(npcs[i].endGfx);
				}
				handleSpecialEffects(c, i, damage);
				c.logoutDelay = System.currentTimeMillis(); // logout delay
				// c.setHitDiff(damage);
				c.handleHitMask(damage);
				c.playerLevel[3] -= damage;
				c.getPA().refreshSkill(3);
				c.updateRequired = true;
				// c.setHitUpdateRequired(true);
			}
		}
	}

	public void handleSpecialEffects(Client c, int i, int damage) {
		if (npcs[i].npcType == 2892 || npcs[i].npcType == 2894
				|| npcs[i].npcType == 1158 || npcs[i].npcType == 1160) {
			if (damage > 0) {
				if (c != null) {
					if (c.playerLevel[5] > 0) {
						c.playerLevel[5]--;
						c.getPA().refreshSkill(5);
						c.getPA().appendPoison(c, 12);
					}
				}
			}
		}

	}

	public static void startAnimation(int animId, int i) {
		npcs[i].animNumber = animId;
		npcs[i].animUpdateRequired = true;
		npcs[i].updateRequired = true;
	}

	public boolean goodDistance(int objectX, int objectY, int playerX,
			int playerY, int distance) {
		return ((objectX - playerX <= distance && objectX - playerX >= -distance) && (objectY
				- playerY <= distance && objectY - playerY >= -distance));
	}

	public int getMaxHit(int i) {
		switch (npcs[i].npcType) {
		case 6222:
			if (npcs[i].attackType == 2)
				return 28;
			else
				return 68;
		case 1158:
			if (npcs[i].attackType == 2)
				return 30;
			else
				return 21;
		case 1160:
			if (npcs[i].attackType == 2 || npcs[i].attackType == 1)
				return 30;
			else
				return 21;
		case 6203:
			if (npcs[i].attackType == 0)
				return 40;
			else
				return 35;
		case 6247:
			return 31;
		case 6260:
			return 36;
		}
		return 1;
	}

	public boolean loadAutoSpawn(String FileName) {
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
			characterfile = new BufferedReader(new FileReader("./" + FileName));
		} catch (FileNotFoundException fileex) {
			Misc.println(FileName + ": file not found.");
			return false;
		}
		try {
			line = characterfile.readLine();
		} catch (IOException ioexception) {
			Misc.println(FileName + ": error loading file.");
			try {
				characterfile.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//return false;
		}
		while (EndOfFile == false && line != null) {
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
				if (token.equals("spawn")) {
					newNPC(Integer.parseInt(token3[0]),
							Integer.parseInt(token3[1]),
							Integer.parseInt(token3[2]),
							Integer.parseInt(token3[3]),
							Integer.parseInt(token3[4]),
							getNpcListHP(Integer.parseInt(token3[0])),
							Integer.parseInt(token3[5]),
							Integer.parseInt(token3[6]),
							Integer.parseInt(token3[7]));

				}
			} else {
				if (line.equals("[ENDOFSPAWNLIST]")) {
					try {
						characterfile.close();
					} catch (IOException ioexception) {
					}
					return true;
				}
			}
			try {
				line = characterfile.readLine();
			} catch (IOException ioexception1) {
				EndOfFile = true;
			}
		}
		try {
			characterfile.close();
		} catch (IOException ioexception) {
		}
		return false;
	}

	public int getNpcListHP(int npcId) {
		for (int i = 0; i < maxListedNPCs; i++) {
			if (NpcList[i] != null) {
				if (NpcList[i].npcId == npcId) {
					return NpcList[i].npcHealth;
				}
			}
		}
		return 0;
	}

	public String getNpcListName(int npcId) {
		for (int i = 0; i < maxListedNPCs; i++) {
			if (NpcList[i] != null) {
				if (NpcList[i].npcId == npcId) {
					return NpcList[i].npcName;
				}
			}
		}
		return "nothing";
	}

	@SuppressWarnings("resource")
	public boolean loadNPCList(String FileName) {
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
			characterfile = new BufferedReader(new FileReader("./" + FileName));
		} catch (FileNotFoundException fileex) {
			Misc.println(FileName + ": file not found.");
			return false;
		}
		try {
			line = characterfile.readLine();
		} catch (IOException ioexception) {
			Misc.println(FileName + ": error loading file.");
			try {
				characterfile.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		}
		while (EndOfFile == false && line != null) {
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
				if (token.equals("npc")) {
					newNPCList(Integer.parseInt(token3[0]), token3[1],
							Integer.parseInt(token3[2]),
							Integer.parseInt(token3[3]));
				}
			} else {
				if (line.equals("[ENDOFNPCLIST]")) {
					try {
						characterfile.close();
					} catch (IOException ioexception) {
					}
					return true;
				}
			}
			try {
				line = characterfile.readLine();
			} catch (IOException ioexception1) {
				EndOfFile = true;
			}
		}
		try {
			characterfile.close();
		} catch (IOException ioexception) {
		}
		return false;
	}

}
