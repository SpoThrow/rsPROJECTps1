package server.game.minigames.castlewars;

import java.util.HashMap;
import java.util.Iterator;

import server.Config;
import server.Server;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.items.ItemAssistant;
import server.game.npcs.NPCHandler;
import server.game.objects.Object;
import server.game.players.Client;
import server.game.players.PlayerHandler;
import core.util.Misc;

public class CastleWars {

    /*
     * Game timers.
     */
    private static final int GAME_TIMER = 500; //1500 * 600 = 900000ms = 15 minutes
    private static final int GAME_START_TIMER = 400;
    /*
     * Hashmap for the waitingroom players
     */
    private static HashMap<Client, Integer> waitingRoom = new HashMap<Client, Integer>();
    /*
     * hashmap for the gameRoom players
     */
    private static HashMap<Client, Integer> gameRoom = new HashMap<Client, Integer>();
    /*
     * The coordinates for the waitingRoom both sara/zammy
     */
    private static final int[][] WAIT_ROOM = {
        {2377, 9485}, //sara
        {2421, 9524} //zammy
    };
    /*
     * The coordinates for the gameRoom both sara/zammy
     */
    private static final int[][] GAME_ROOM = {
        {2426, 3076}, //sara
        {2372, 3131} //zammy
    };
    private static final int[][] FLAG_STANDS = {
        {2429, 3074}, //sara {X-Coord, Y-Coord)
        {2370, 3133} //zammy
    };
    /*
     * Scores for saradomin and zamorak!
     */
    private static int[] scores = {0, 0};
    /*
     * Booleans to check if a team's flag is safe
     */
    public static int zammyFlag = 0;
    public static int saraFlag = 0;
    /*
     * Zamorak and saradomin banner/capes item ID's
     */
    public static final int SARA_BANNER = 4037;
    public static final int ZAMMY_BANNER = 4039;
    public static final int SARA_CAPE = 4041;
    public static final int ZAMMY_CAPE = 4042;
    public static final int SARA_HOOD = 4513;
    public static final int ZAMMY_HOOD = 4515;
    /*
     * 
     */
	public static long SCatapult;
    
    public static boolean zammySideDoor, zammyRock1, zammyRock2, zammyCatapult, saraSideDoor, saraRock1, saraRock2, saraCatapult;
    public static int saraMainGateHP;
	public static int zammyMainGateHP;
    private static int properTimer = 0;
    private static int timeRemaining = -1;
    private static boolean messageSent = false;
    private static int gameStartTimer = GAME_START_TIMER;
    private static boolean gameStarted = false;
	public static int zammyMainDoor;
	public static int saraMainDoor;
	public static long ZCatapult;
	public static int[] BARRICADE_INDEX = new int[20];

    /**
     * Method we use to add someone to the waitinroom in a different method, this will filter out some error messages
     * @param player the player that wants to join
     * @param team the team!
     */
    public static void addToWaitRoom(Client player, int team) {
        if (player == null) {
            return;
        } else if (gameStarted == true) {
            player.sendMessage("There's already a Castle Wars going. Please wait a few minutes before trying again.");
            return;
        } else if (player.playerEquipment[player.playerHat] > 0 || player.playerEquipment[player.playerCape] > 0) {
            player.sendMessage("You may not wear capes or helmets inside of castle wars.");
            return;
        }
        toWaitingRoom(player, team);
    }
    
	public static void resetVariables() {
		saraMainDoor = 1;
		saraSideDoor = false;
		saraRock1 = true;
		saraRock2 = true;
		saraCatapult = true;
		SCatapult = 0;
		zammyMainGateHP = 100;
		saraMainGateHP = 100;
		zammyMainDoor = 1;
		zammySideDoor = false;
		zammyRock1 = true;
		zammyRock2 = true;
		zammyCatapult = true;
		ZCatapult = 0;
		scores[0] = 0;
		scores[1] = 0;
		messageSent = false;
	}
    
	public static void takeFromStall(final Client c, final int item) {
		if(c.isTakingFromStall)
			return;
		c.startAnimation(832);
		c.isTakingFromStall = true;
		CycleEventHandler.addEvent(c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				if (c.getItems().addItem(item, 1)) {
					c.getItems();
					c.sendMessage("You take a " + ItemAssistant.getItemName(item).toLowerCase() + ".");
				}
				container.stop();
			}
			@Override
			public void stop() {
				c.isTakingFromStall = false;
			}
		}, 2);
	}
	
	public static void removeBarricades() {
		for (int i = 0; i < BARRICADE_INDEX.length; i++) {
			if (BARRICADE_INDEX[i] == 0)
				continue;
			NPCHandler.npcs[BARRICADE_INDEX[i]].isDead = true;
		}
	}

    /**
     * Method we use to transfer to player from the outside to the waitingroom (:
     * @param player the player that wants to join
     * @param team team he wants to be in - team = 1 (saradomin), team = 2 (zamorak), team = 3 (random)
     */
    public static void toWaitingRoom(Client player, int team) {
		int[] food = {391, 385, 379, 333, 329, 373, 361, 7946, 397, 1891, 365, 339, 1942, 6701, 6705, 7056, 7054, 7058, 7060, 315, 347, 325, 1897, 2289, 2293, 2297, 2301, 2309, 2323, 2325, 2327, 351, 6703, 1963, 6883, 2108, 2118, 2116, 15272};
		for (int i = 0; i < food.length; i++) {
			if (player.getItems().playerHasItem(food[i])) {
				player.sendMessage("You may not bring your own food inside of castle wars.");
				return;
			}
		}
        if (team == 1) {
            if (getSaraPlayers() > getZammyPlayers() && getSaraPlayers() > 0) {
                player.sendMessage("The saradomin team is full, try again later!");
                return;
            }
            if (getZammyPlayers() >= getSaraPlayers() || getSaraPlayers() == 0) {
                player.sendMessage("You have been added to the @blu@Saradomin@bla@ team.");
                player.sendMessage("Next Game Begins In:@red@ " + (((gameStartTimer * 3)+3) + (timeRemaining * 3)) + " @bla@seconds.");
                addCapes(player, SARA_CAPE);
                addHoods(player, SARA_HOOD);
                waitingRoom.put(player, team);
                player.getPA().movePlayer(WAIT_ROOM[team - 1][0] + Misc.random(5), WAIT_ROOM[team - 1][1] + Misc.random(5), 0);
            }
        } else if (team == 2) {
            if (getZammyPlayers() > getSaraPlayers() && getZammyPlayers() > 0) {
                player.sendMessage("The zamorak team is full, try again later!");
                return;
            }
            if (getZammyPlayers() <= getSaraPlayers() || getZammyPlayers() == 0) {
                player.sendMessage("[@red@RANDOM TEAM@bla@] You have been added to the @red@Zamorak@bla@ team.");
                player.sendMessage("Next Game Begins In:@red@ " + ((gameStartTimer * 3) + (timeRemaining * 3)) + " @bla@seconds.");
                addCapes(player, ZAMMY_CAPE);
                addHoods(player, ZAMMY_HOOD);
                waitingRoom.put(player, team);
                player.getPA().movePlayer(WAIT_ROOM[team - 1][0] + Misc.random(5), WAIT_ROOM[team - 1][1] + Misc.random(5), 0);
            }
        } else if (team == 3) {
            toWaitingRoom(player, getZammyPlayers() > getSaraPlayers() ? 1 : 2);
            return;
        }
    }

    /**
     * Method to add score to scoring team
     * @param player the player who scored
     * @param banner banner id!
     */
    public static void returnFlag(Client player, int wearItem) {
        if (player == null) {
            return;
        }
        if (wearItem != SARA_BANNER && wearItem != ZAMMY_BANNER) {
            return;
        }
        int team = gameRoom.get(player);
        int objectId = -1;
        int objectTeam = -1;
        switch (team) {
            case 1:
                if (wearItem == SARA_BANNER) {
                    setSaraFlag(0);
                    objectId = 4902;
                    objectTeam = 0;
                    player.sendMessage("returned the sara flag!");
                } else {
                    objectId = 4903;
                    objectTeam = 1;
                    setZammyFlag(0);
                    scores[0]++; //upping the score of a team; team 0 = sara, team 1 = zammy
                    player.sendMessage("The team of Saradomin scores 1 point!");
                }
                break;
            case 2:
                if (wearItem == ZAMMY_BANNER) {
                    setZammyFlag(0);
                    objectId = 4903;
                    objectTeam = 1;
                    player.sendMessage("returned the zammy flag!");
                } else {
                    objectId = 4902;
                    objectTeam = 0;
                    setSaraFlag(0);
                    scores[1]++; //upping the score of a team; team 0 = sara, team 1 = zammy
                    player.sendMessage("The team of Zamorak scores 1 point!");
                    zammyFlag = 0;
                }
                break;
        }
        changeFlagObject(objectId, objectTeam);
        player.getPA().createPlayerHints(10, -1);
        player.playerEquipment[player.playerWeapon] = -1;
        player.playerEquipmentN[player.playerWeapon] = 0;
        player.getItems().updateSlot(3);
        player.appearanceUpdateRequired = true;
        player.updateRequired = true;
        player.getItems().resetItems(3214);
    }

    /**
     * Method that will capture a flag when being taken by the enemy team!
     * @param player the player who returned the flag
     * @param team
     */
    public static void captureFlag(Client player) {
        if (player.playerEquipment[player.playerWeapon] > 0) {
           // player.sendMessage("Please remove your weapon before attempting to get the flag again!");
            player.getItems().removeItem(player.playerEquipment[player.playerWeapon], player.playerWeapon);
            //return;
        }
        int team = gameRoom.get(player);
        if (team == 2 && saraFlag == 0) { //sara flag
            setSaraFlag(1);
            addFlag(player, SARA_BANNER);
            createHintIcon(player, 1);
            changeFlagObject(4377, 0);
        }
        if (team == 1 && zammyFlag == 0) {
            setZammyFlag(1);
            addFlag(player, ZAMMY_BANNER);
            createHintIcon(player, 2);
            changeFlagObject(4378, 1);
        }
    }

    /**
     * Method that will add the flag to a player's weapon slot
     * @param player the player who's getting the flag
     * @param flagId the banner id.
     */
    public static void addFlag(Client player, int flagId) {
        player.playerEquipment[player.playerWeapon] = flagId;
        player.playerEquipmentN[player.playerWeapon] = 1;
        player.getItems().updateSlot(player.playerWeapon);
        player.appearanceUpdateRequired = true;
        player.updateRequired = true;
    }
    
	public static void updateCwObects(Client c) {
		if (isInCw(c)) {
			CastleWarObjects.castleWarsFlags(c, 1);
			CastleWarObjects.castleWarsFlags(c, 2);
			CastleWarObjects.castleWarsRocks(c, 1);
			CastleWarObjects.castleWarsRocks(c, 2);
			CastleWarObjects.castleWarsCatapults(c, 1);
			CastleWarObjects.castleWarsCatapults(c, 2);
			CastleWarObjects.castleWarsDoors(c, 1);
			CastleWarObjects.castleWarsDoors(c, 2);
		}
	}
	
	public static void updateCwObects(String type, int team) {
		for (int d = 0; d < Config.MAX_PLAYERS; d++) {
			if (PlayerHandler.players[d] != null && PlayerHandler.players[d].isActive) {
				Client t = (Client) PlayerHandler.players[d];
				if (isInCw(t)) {
					if (type == "flag") {
						CastleWarObjects.castleWarsFlags(t, team);
					}
					if (type == "rock") {
						CastleWarObjects.castleWarsRocks(t, team);
					}
					if (type == "catapult") {
						CastleWarObjects.castleWarsCatapults(t, team);
					}
					if (type == "door") {
						CastleWarObjects.castleWarsDoors(t, team);
					}
					updateCastleWarsScreen(t);
				}
			}
		}
	}

    /**
     * Method we use to handle the flag dropping
     * @param player the player who dropped the flag/died
     * @param flagId the flag item ID
     */
    public static void dropFlag(Client player, int flagId) {
        int object = -1;
        switch (flagId) {
            case SARA_BANNER: //sara
                setSaraFlag(2);
                object = 4900;
                createFlagHintIcon(player.getX(), player.getY());
                break;
            case ZAMMY_BANNER: //zammy
                setZammyFlag(2);
                object = 4901;
                createFlagHintIcon(player.getX(), player.getY());
                break;
        }
		player.playerEquipment[player.playerWeapon] = -1;
		player.playerEquipmentN[player.playerWeapon] = 0;
		player.getItems().updateSlot(player.playerWeapon);
		player.appearanceUpdateRequired = true;
		player.updateRequired = true;
		new Object(object, player.getX(), player.getY(), player.heightLevel, 0, 10, object, -1, 1);
    }

    /**
     * Method we use to pickup the flag when it was dropped/lost
     * @param Player the player who's picking it up
     * @param objectId the flag object id.
     */
    public static void pickupFlag(Client player) {
        int team = gameRoom.get(player);
        switch (player.objectId) {
            case 4900: //sara
				if (player.playerEquipment[player.playerWeapon] > 0) {
					//player.sendMessage("Please remove your weapon before attempting to get the flag again!");
					//return;
					player.getItems().removeItem(
							player.playerEquipment[player.playerWeapon],
							player.playerWeapon);
				}
				if (saraFlag != 2) {
					//player.sendMessage("Flag already taken 'test'");
					return;
				}
		        if (team == 2 && saraFlag == 2)
		        	createHintIcon(player, 1);
                setSaraFlag(1);
                addFlag(player, 4037);
                break;
            case 4901: //zammy
				if (player.playerEquipment[player.playerWeapon] > 0) {
					//player.sendMessage("Please remove your weapon before attempting to get the flag again!");
					//return;
					player.getItems().removeItem(
							player.playerEquipment[player.playerWeapon],
							player.playerWeapon);
				}
				if (zammyFlag != 2) {
					//player.sendMessage("Flag already taken 'test'");
					return;
				}
		        if (team == 1 && zammyFlag == 2)
		        	createHintIcon(player, 2);
                setZammyFlag(1);
                addFlag(player, 4039);
                break;
        }
        createHintIcon(player, (gameRoom.get(player) == 1) ? 2 : 1);
        @SuppressWarnings("rawtypes")
		Iterator iterator = gameRoom.keySet().iterator();
        while (iterator.hasNext()) {
            Client teamPlayer = (Client) iterator.next();
            teamPlayer.getPA().createObjectHints(player.objectX, player.objectY, 170, -1);
            new Object(-1, player.objectX, player.objectY, player.heightLevel, 0, 10, -1, -1, 1);
        }
        return;
    }

    /**
     * Hint icons appear to your team when a enemy steals flag
     * @param player the player who took the flag
     * @param t team of the opponent team. (: 
     */
    @SuppressWarnings("rawtypes")
	public static void createHintIcon(Client player, int t) {
        Iterator iterator = gameRoom.keySet().iterator();
        while (iterator.hasNext()) {
            Client teamPlayer = (Client) iterator.next();
            System.out.println(teamPlayer.playerName + " => Team => " + gameRoom.get(teamPlayer));
            System.out.println("desired team = " + t);
            teamPlayer.getPA().createPlayerHints(10, -1); 
            if (gameRoom.get(teamPlayer) == t) {
                System.out.println("created hint icons for playername " + teamPlayer.playerName + " and team number: " + t);
                teamPlayer.getPA().createPlayerHints(10, player.playerId);
                teamPlayer.getPA().requestUpdates();
            }
        }
    }

    /**
     * Hint icons appear to your team when a enemy steals flag
     * @param player the player who took the flag
     * @param t team of the opponent team. (:
     */
    public static void createFlagHintIcon(int x, int y) {
        @SuppressWarnings("rawtypes")
		Iterator iterator = gameRoom.keySet().iterator();
        while (iterator.hasNext()) {
            Client teamPlayer = (Client) iterator.next();
            teamPlayer.getPA().createObjectHints(x, y, 170, 2);
        }
    }

    /**
     * This method is used to get the teamNumber of a certain player
     * @param player
     * @return
     */
    public static int getTeamNumber(Client player) {
        if (player == null) {
            return -1;
        }
        if (gameRoom.containsKey(player)) {
            return gameRoom.get(player);
        }
        return -1;
    }

    /**
     * The leaving method will be used on click object or log out
     * @param player player who wants to leave
     */
    public static void leaveWaitingRoom(Client player) {
        if (player == null) {
            System.out.println("player is null");
            return;
        }
        if (waitingRoom.containsKey(player)) {
            waitingRoom.remove(player);
            player.getPA().createPlayerHints(10, -1);
            player.sendMessage("You left your team!");
            deleteGameItems(player);
            player.getPA().movePlayer(2439 + Misc.random(4), 3085 + Misc.random(5), 0);
            return;
        }
        player.getPA().movePlayer(2439 + Misc.random(4), 3085 + Misc.random(5), 0);
        System.out.println("Waiting room map does not contain " + player.playerName);
    }

    public static void process() {
        if (properTimer > 0) {
            properTimer--;
            return;
        } else {
            properTimer = 4;
        }
        if (gameStartTimer > 0) {
            if (gameStartTimer <= 10) {
            	if(!messageSent)
				for (int j = 0; j < PlayerHandler.players.length; j++) {
					if (PlayerHandler.players[j] != null) {
						Client c2 = (Client)PlayerHandler.players[j];
						c2.sendMessage("@cr2@@red@[Castle Wars] @dre@New game starts in 30 seconds!");
						messageSent = true;
					}
				}
            }
            gameStartTimer--;
            updatePlayers();
        } else if (gameStartTimer == 0) {
        	resetVariables();
            startGame();
        }
        if (timeRemaining > 0) {
            timeRemaining--;
            //updateInGamePlayers();
            updateInterfaceForAll();
        } else if (timeRemaining == 0) {
            endGame();
            removeBarricades();
        }
    }

    /**
     * Method we use to update the player's interface in the waiting room
     */
    public static void updatePlayers() {
        @SuppressWarnings("rawtypes")
		Iterator iterator = waitingRoom.keySet().iterator();
        while (iterator.hasNext()) {
            Client player = (Client) iterator.next();
            if (player != null) {
                player.getPA().sendFrame126("Next Game Begins In:@red@ " + ((gameStartTimer * 3) + (timeRemaining * 3)) + " @whi@seconds.", 6570);
                player.getPA().sendFrame126("Zamorak Players: @red@" + getZammyPlayers() + "@whi@.", 6572);
                player.getPA().sendFrame126("Saradomin Players: @blu@" + getSaraPlayers() + "@whi@.", 6664);
                player.getPA().walkableInterface(6673);
            }
        }
    }
    
    public static void updateCastleWarsScreen(Client c) {
    	if (isInCw(c)) {
    		c.getPA().walkableInterface(11146);
			c.getPA().sendFrame126(timeRemaining * 3 + " seconds", 11155);
			int config = zammyMainGateHP;
			if (getTeamNumber(c) == 2 ? zammySideDoor : saraSideDoor) {
				config += 128;
			}
			if (!saraRock1) {
				config += 256;
			}
			if (!saraRock2) {
				config += 512;
			}
			if (getTeamNumber(c) == 2 ? !zammyCatapult : !saraCatapult) {
				config += 1024;
			}
			if (getTeamNumber(c) == 2 ? zammyFlag == 2 : saraFlag == 2) {
				config += 2097152 * 2;
			} else if (getTeamNumber(c) == 2 ? zammyFlag == 1 : saraFlag == 1) {
				config += 2097152 * 1;
			}
			config += 16777216 * (getTeamNumber(c) == 2 ? scores[1] : scores[0]);
			c.getPA().sendFrame87(getTeamNumber(c) == 2 ? 377 : 378, config);

			config = saraMainGateHP;
			if (getTeamNumber(c) == 2 ? zammySideDoor : saraSideDoor) {
				config += 128;
			}
			if (!zammyRock1) {
				config += 256;
			}
			if (!zammyRock2) {
				config += 512;
			}
			if (!saraCatapult) {
				config += 1024;
			}
			if (getTeamNumber(c) == 2 ? saraFlag == 2 : zammyFlag == 2) {
				config += 2097152 * 2;
			} else if (getTeamNumber(c) == 2 ? saraFlag == 1 : zammyFlag == 1) {
				config += 2097152 * 1;
			}
			config += 16777216 * (getTeamNumber(c) == 2 ? scores[0] : scores[1]);
			c.getPA().sendFrame87(getTeamNumber(c) == 2 ? 378 : 377, config);
		}
	}
    
	public static void updateInterfaceForAll() {
		for (int d = 0; d < Config.MAX_PLAYERS; d++) {
			if (PlayerHandler.players[d] != null && PlayerHandler.players[d].isActive) {
				Client t = (Client) PlayerHandler.players[d];
				if (isInCw(t) || isInCwWait(t)) {
					updateCastleWarsScreen(t);
				}
			}
		}
	}
    
	public static boolean attackDoor(final Client c, final int object) {
		if(c.isAttackingGate)
			return true;
		if (object != 4423 && object != 4424 && object != 4427 && object != 4428) {
			return false;
		}
		if ((object == 4423 || object == 4424) && getTeamNumber(c) == 1 || (object == 4427 || object == 4427) && getTeamNumber(c) == 2) {
			c.sendMessage("You can't attack your own team's door!");
			return true;
		}
		c.isAttackingGate = true;
		CycleEventHandler.addEvent(c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				if (c == null || !c.isActive || !c.isAttackingGate) {
					container.stop();
					return;
				}
				final int defence = 100;
				//final int stat = 0; //fightxp
				int damage = 0;
				if (c.usingSpecial && !c.autocasting) { // special attack
					c.sendMessage("You can't use special attack on this door.");
					c.usingSpecial = false;
					c.getItems().updateSpecialBar();
				} else if (c.autocasting) {
					c.spellId = c.autocastId;
					if (!c.getCombat().checkMagicReqs(c.spellId)) {
						container.stop();
						return;
					}
				}
				if (!c.autocasting) {
					c.startAnimation(c.getCombat().getWepAnim(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase()));
				} else {
					c.startAnimation(c.MAGIC_SPELLS[c.spellId][2]);
				}
				if (c.autocasting) { // magic
					int pX = c.getX();
					int pY = c.getY();
					int nX = c.objectX;
					int nY = c.objectX;
					int offX = (pY - nY) * -1;
					int offY = (pX - nX) * -1;
					if (c.MAGIC_SPELLS[c.spellId][3] > 0) {
						if (c.getCombat().getStartGfxHeight() == 100) {
							c.gfx100(c.MAGIC_SPELLS[c.spellId][3]);
						} else {
							c.gfx0(c.MAGIC_SPELLS[c.spellId][3]);
						}
					}
					if (c.MAGIC_SPELLS[c.spellId][4] > 0) {
						c.getPA().createPlayersProjectile(pX, pY, offX, offY, 50, 78, c.MAGIC_SPELLS[c.spellId][4], c.getCombat().getStartHeight(), c.getCombat().getEndHeight(), 0, c.getCombat().getStartDelay());
					}
					damage = Misc.random(c.MAGIC_SPELLS[c.oldSpellId][6]);
					//damage = c.getCombat().getHit(MagicMaxHit.magicMaxHit(c, c.spellId), c.getCombat().getMagic(), defence);
					final boolean magicFailed = false;
					if(Misc.random(c.getCombat().mageAtk()) > Misc.random(defence))
						c.magicFailed = false;
					else
						c.magicFailed = true;
					if (!magicFailed) {
						c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage*Config.MAGIC_EXP_RATE), 6); 
					}
					CycleEventHandler.addEvent(c, new CycleEvent() {
						boolean failMage = magicFailed;
						int spellId = c.spellId;
						@Override
						public void execute(CycleEventContainer container2) {
							try {
								c.getPA().createPlayersStillGfx(failMage ? 85 : c.MAGIC_SPELLS[spellId][5], c.objectX, c.objectY, 0, 1);
							} catch (Exception e) {
								container2.stop();
							}
						}
						@Override
						public void stop() {
						}
					}, c.getCombat().getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase()));
					c.spellId = -1;
				} else if (c.usingBow || c.usingRangeWeapon) { // range attack
					damage = Misc.random(c.getCombat().rangeMaxHit());
					final int offX, offY;
					int oX = c.objectX, oY = c.objectY;
					final int pX = c.getX();
					final int pY = c.getY();
					offX = (pY - oY) * -1;
					offY = (pX - oX) * -1;
					try {
						c.getPA().createPlayersProjectile(pX, pY, offX, offY, 50, c.getCombat().getProjectileSpeed(), c.getCombat().getRangeProjectileGFX(), 43, 31, 0, c.getCombat().getProjectileShowDelay());
					} catch (Exception e) {
					}
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE/3), 4); 
				} else {
					damage = Misc.random(c.getCombat().calculateMeleeMaxHit());
					if(c.fightMode == 3) {
						c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 0); 
						c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 1);
						c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 2); 				
						c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 3);
						c.getPA().refreshSkill(0);
						c.getPA().refreshSkill(1);
						c.getPA().refreshSkill(2);
						c.getPA().refreshSkill(3);
					} else {
						c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE), c.fightMode); 
						c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 3);
						c.getPA().refreshSkill(c.fightMode);
						c.getPA().refreshSkill(3);
					}
				}
				if (object == 4423 || object == 4424) {
					if (saraMainGateHP - damage < 0) {
						saraMainGateHP = 0;
					} else {
						saraMainGateHP -= damage;
					}
				} else if (object == 4427 || object == 4428) {
					if (zammyMainGateHP - damage < 0) {
						zammyMainGateHP = 0;
					} else {
						zammyMainGateHP -= damage;
					}
				}
				if (saraMainGateHP < 1) {
					CastleWars.saraMainDoor = 3;
					updateCwObects("door", 1);
					updateInterfaceForAll();
					container.stop();
				}
				if (zammyMainGateHP < 1) {
					CastleWars.zammyMainDoor = 3;
					updateCwObects("door", 2);
					updateInterfaceForAll();
					container.stop();
				}
				updateInterfaceForAll();
			}
			@Override
			public void stop() {
				c.getCombat().resetPlayerAttack();
				c.isAttackingGate = false;
			}
		}, c.getCombat().getAttackDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase()));
		return true;
	}

    /**
     * Method we use the update the player's interface in the game room
     */
    public static void updateInGamePlayers() {
        if (getSaraPlayers() > 0 && getZammyPlayers() > 0) {
            @SuppressWarnings("rawtypes")
			Iterator iterator = gameRoom.keySet().iterator();
            while (iterator.hasNext()) {
                Client player = (Client) iterator.next();
                int config;
                if (player == null) {
                    continue;
                }
                player.getPA().walkableInterface(11146);
                player.getPA().sendFrame126("Zamorak = " + scores[1], 11147);
                player.getPA().sendFrame126(scores[0] + " = Saradomin", 11148);
                player.getPA().sendFrame126(timeRemaining * 3 + " secs", 11155);
                config = (2097152 * saraFlag);
                player.getPA().sendFrame87(378, config);
                config = (2097152 * zammyFlag); //flags 0 = safe 1 = taken 2 = dropped
                player.getPA().sendFrame87(377, config);
            }
        }
    }

    /*
     * Method that will start the game when there's enough players.
     */
    public static void startGame() {
        if (getSaraPlayers() < 1 || getZammyPlayers() < 1) {
            gameStartTimer = GAME_START_TIMER;
            return;
        }
        gameStartTimer = -1;
        System.out.println("Starting Castle Wars game.");
        gameStarted = true;
        timeRemaining = GAME_TIMER / 2;
        @SuppressWarnings("rawtypes")
		Iterator iterator = waitingRoom.keySet().iterator();
        while (iterator.hasNext()) {
            Client player = (Client) iterator.next();
            int team = waitingRoom.get(player);
            if (player == null) {
                continue;
            }
            player.getPA().walkableInterface(-1);
            player.getPA().movePlayer(GAME_ROOM[team - 1][0] + Misc.random(3), GAME_ROOM[team - 1][1] - Misc.random(3), 1);
            player.getPA().movePlayer(GAME_ROOM[team - 1][0] + Misc.random(3), GAME_ROOM[team - 1][1] - Misc.random(3), 1);
            gameRoom.put(player, team);
        }
        waitingRoom.clear();
    }

    /*
     * Method we use to end an ongoing cw game.
     */
    @SuppressWarnings("rawtypes")
	public static void endGame() {
        Iterator iterator = gameRoom.keySet().iterator();
        while (iterator.hasNext()) {
            Client player = (Client) iterator.next();
            int team = gameRoom.get(player);
            if (player == null) {
                continue;
            }
    		for (int i = 0; i < 7; i++) {
    			player.playerLevel[i] = player.getLevelForXP(player.playerXP[i]);
    			player.getPA().refreshSkill(i);
    		}
    		player.isDead = false;
            player.cwGames++;
            player.getPA().movePlayer(2440 + Misc.random(3), 3089 - Misc.random(3), 0);
            player.sendMessage("[@red@CASTLE WARS@bla@] The Castle Wars Game has ended!");
            player.sendMessage("[@red@CASTLE WARS@bla@] Kills: @red@ " + player.cwKills + " @bla@Deaths:@red@ " + player.cwDeaths + "@bla@ Games Played: @red@" + player.cwGames + "@bla@.");
            player.getPA().createPlayerHints(10, -1);
            deleteGameItems(player);
            if (scores[0] == scores[1]) {
                player.getItems().addItem(4067, 30);
                player.sendMessage("Tie game! You gain 30 CastleWars tickets!");
            } else if (team == 1) {
                if (scores[0] > scores[1]) {
                    player.getItems().addItem(4067, 50);
                    player.sendMessage("You won the CastleWars Game. You received 50 CastleWars Tickets!");
                } else if (scores[0] < scores[1]) {
                    player.getItems().addItem(4067, 20);
                    player.sendMessage("You lost the CastleWars Game. You received 20 CastleWars Tickets!");
                }
            } else if (team == 2) {
                if (scores[1] > scores[0]) {
                    player.getItems().addItem(4067, 50);
                    player.sendMessage("You won the CastleWars Game. You received 50 CastleWars Tickets!");
                } else if (scores[1] < scores[0]) {
                    player.getItems().addItem(4067, 20);
                    player.sendMessage("You lost the CastleWars Game. You received 20 CastleWars Tickets!");
                }
            }
        }
        resetGame();
    }

    /**
     * reset the game variables
     */
    public static void resetGame() {
    	Server.objectManager.removeGameObjects(1);
        changeFlagObject(4902, 0);
        changeFlagObject(4903, 1);
        setSaraFlag(0);
        setZammyFlag(0);
        timeRemaining = -1;
        System.out.println("Ending Castle Wars game.");
        gameStartTimer = GAME_START_TIMER;
        gameStarted = false;
        gameRoom.clear();
    }

    /**
     * Method we use to remove a player from the game
     * @param player the player we want to be removed
     */
    public static void removePlayerFromCw(Client player) {
        if (player == null) {
            System.out.println("Error removing player from castle wars [REASON = null]");
            return;
        }
        if (gameRoom.containsKey(player)) {
            /*
             * Logging/leaving with flag
             */
            if (player.getItems().playerHasEquipped(SARA_BANNER)) {
                player.getItems().removeItem(player.playerEquipment[3], 3);
                setSaraFlag(0); //safe flag
            } else if (player.getItems().playerHasEquipped(ZAMMY_BANNER)) {
                player.getItems().removeItem(player.playerEquipment[3], 3);
                setZammyFlag(0); //safe flag
            }
            deleteGameItems(player);
            player.getPA().movePlayer(2440, 3089, 0);
            System.out.println("Deleting playername test: " + player.playerName);
            player.sendMessage("[@red@CASTLE WARS@bla@] The Casle Wars Game has ended for you!");
            player.sendMessage("[@red@CASTLE WARS@bla@] Kills: @red@ " + player.cwKills + " @bla@Deaths:@red@ " + player.cwDeaths + "@bla@.");
            player.getPA().createPlayerHints(10, -1);
            gameRoom.remove(player);
        }
        if (getZammyPlayers() <= 0 || getSaraPlayers() <= 0) {
            endGame();
        }
    }

    /**
     * Will add a cape to a player's equip
     * @param player the player
     * @param capeId the capeId
     */
    public static void addCapes(Client player, int capeId) {
        player.playerEquipment[player.playerCape] = capeId;
        player.playerEquipmentN[player.playerCape] = 1;
        player.getItems().updateSlot(player.playerCape);
        player.appearanceUpdateRequired = true;
        player.updateRequired = true;
    }
    
    public static void addHoods(Client player, int hatId) {
        player.playerEquipment[player.playerHat] = hatId;
        player.playerEquipmentN[player.playerHat] = 1;
        player.getItems().updateSlot(player.playerHat);
        player.appearanceUpdateRequired = true;
        player.updateRequired = true;
        player.setAppearanceUpdateRequired(true);
    }

    /**
     * This method will delete all items received in game. Easy to add items to the array. (:
     * @param player the player who want the game items deleted from.
     */
    public static void deleteGameItems(Client player) {
        switch (player.playerEquipment[3]) {
            case 4037:
            case 4039:
                //player.getItems().removeItem(player.playerEquipment[3], 3);
				player.playerEquipment[3] = -1;
				player.playerEquipmentN[3] = 0;	 
				player.getItems().updateSlot(player.playerWeapon);
				player.appearanceUpdateRequired = true;
				player.updateRequired = true;
                System.out.println("removed weapon:" + player.playerEquipment[3]);
                break;
        }
		switch (player.playerEquipment[0]) {
			case 4513:
			case 4515:
				player.playerEquipment[0] = -1;
				player.playerEquipmentN[0] = 0;	 
				player.getItems().updateSlot(player.playerHat);
				player.appearanceUpdateRequired = true;
				player.updateRequired = true;
				player.setAppearanceUpdateRequired(true);
	            //player.getItems().removeItem(player.playerEquipment[0], 0);
	            System.out.println("removed hood:" + player.playerEquipment[0]);
	            break;
		}
        switch (player.playerEquipment[1]) {
            case 4042:
            case 4041:
               // player.getItems().removeItem(player.playerEquipment[1], 1);
				player.playerEquipment[1] = -1;
				player.playerEquipmentN[1] = 0;	 
				player.getItems().updateSlot(player.playerCape);
				player.appearanceUpdateRequired = true;
				player.updateRequired = true;
                System.out.println("removed cape:" + player.playerEquipment[1]);
                break;
        }
        int[] items = {4049, 1265, 4045, 4053, 4042, 4041, 4037, 4039};
        for (int i = 0; i < items.length; i++) {
            if (player.getItems().playerHasItem(items[i])) {
                player.getItems().deleteItem2(items[i], player.getItems().getItemAmount(items[i]));
            }
        }
    }

    /**
     * Methode we use to get the zamorak players
     * @return the amount of players in the zamorakian team!
     */
    public static int getZammyPlayers() {
        int players = 0;
        @SuppressWarnings("rawtypes")
		Iterator iterator = (!waitingRoom.isEmpty()) ? waitingRoom.values().iterator() : gameRoom.values().iterator();
        while (iterator.hasNext()) {
            if ((Integer) iterator.next() == 2) {
                players++;
            }
        }
        return players;
    }

    /**
     * Method we use to get the saradomin players!
     * @return the amount of players in the saradomin team!
     */
    public static int getSaraPlayers() {
        int players = 0;
        @SuppressWarnings("rawtypes")
		Iterator iterator = (!waitingRoom.isEmpty()) ? waitingRoom.values().iterator() : gameRoom.values().iterator();
        while (iterator.hasNext()) {
            if ((Integer) iterator.next() == 1) {
                players++;
            }
        }
        return players;
    }

    /**
     * Method we use for checking if the player is in the gameRoom
     * @param player player who will be checking
     * @return
     */
    public static boolean isInCw(Client player) {
        return gameRoom.containsKey(player);
    }

    /**
     * Method we use for checking if the player is in the waitingRoom
     * @param player player who will be checking
     * @return
     */
    public static boolean isInCwWait(Client player) {
        return waitingRoom.containsKey(player);
    }

    /**
     * Method to make sara flag change status 0 = safe, 1 = taken, 2 = dropped
     * @param status
     */
    public static void setSaraFlag(int status) {
        saraFlag = status;
    }

    /**
     * Method to make zammy flag change status 0 = safe, 1 = taken, 2 = dropped
     * @param status
     */
    public static void setZammyFlag(int status) {
        zammyFlag = status;
    }

    /**
     * Method we use for the changing the object of the flag stands when capturing/returning flag
     * @param objectId the object
     * @param team the team of the player
     */
    public static void changeFlagObject(int objectId, int team) {
            new Object(objectId, FLAG_STANDS[team][0], FLAG_STANDS[team][1], 3, 0, 10, objectId, -1, 1);
    }
}