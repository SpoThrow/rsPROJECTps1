/*package server.game.minigames.castlewars;

import server.game.players.Client;

public class CastleWarObjects {

    public static void handleObject(Client c, int id, int x, int y) {
        if (!CastleWars.isInCw(c)) {
            c.sendMessage("You gotta be in castle wars before you can use these objects");
            return;
        }
        switch (id) {
            case 4469:
                if (CastleWars.getTeamNumber(c) == 2) {
                    c.sendMessage("You are not allowed in the other teams spawn point.");
                    break;
                }
                if (x == 2426) {
                    if (c.getY() == 3080) {
                        c.getPA().movePlayer(2426, 3081, c.heightLevel);
                    } else if (c.getY() == 3081) {
                        c.getPA().movePlayer(2426, 3080, c.heightLevel);
                    }
                } else if (x == 2422) {
                    if (c.getX() == 2422) {
                        c.getPA().movePlayer(2423, 3076, c.heightLevel);
                    } else if (c.getX() == 2423) {
                        c.getPA().movePlayer(2422, 3076, c.heightLevel);
                    }
                }
                break;
            case 4470:
                if (CastleWars.getTeamNumber(c) == 1) {
                    c.sendMessage("You are not allowed in the other teams spawn point.");
                    break;
                }
                if (x == 2373 && y == 3126) {
                    if (c.getY() == 3126) {
                        c.getPA().movePlayer(2373, 3127, 1);
                    } else if (c.getY() == 3127) {
                        c.getPA().movePlayer(2373, 3126, 1);
                    }
                } else if (x == 2377 && y == 3131) {
                    if (c.getX() == 2376) {
                        c.getPA().movePlayer(2377, 3131, 1);
                    } else if (c.getX() == 2377) {
                        c.getPA().movePlayer(2376, 3131, 1);
                    }
                }
                break;
            case 4417:
                if (x == 2428 && y == 3081 && c.heightLevel == 1) {
                    c.getPA().movePlayer(2430, 3080, 2);
                }
                if (x == 2425 && y == 3074 && c.heightLevel == 2) {
                    c.getPA().movePlayer(2426, 3074, 3);
                }
                if (x == 2419 && y == 3078 && c.heightLevel == 0) {
                    c.getPA().movePlayer(2420, 3080, 1);
                }
                break;
            case 4415:
                if (x == 2419 && y == 3080 && c.heightLevel == 1) {
                    c.getPA().movePlayer(2419, 3077, 0);
                }
                if (x == 2430 && y == 3081 && c.heightLevel == 2) {
                    c.getPA().movePlayer(2427, 3081, 1);
                }
                if (x == 2425 && y == 3074 && c.heightLevel == 3) {
                    c.getPA().movePlayer(2425, 3077, 2);
                }
                if (x == 2374 && y == 3133 && c.heightLevel == 3) {
                    c.getPA().movePlayer(2374, 3130, 2);
                }
                if (x == 2369 && y == 3126 && c.heightLevel == 2) {
                    c.getPA().movePlayer(2372, 3126, 1);
                }
                if (x == 2380 && y == 3127 && c.heightLevel == 1) {
                    c.getPA().movePlayer(2380, 3130, 0);
                }
                break;
            case 4411:  //castle wars jumping stones
				if (x == c.getX() && y == c.getY())
					c.sendMessage("You are standing on the rock you clicked 'test'");
				else if (x > c.getX() && y == c.getY())
                    c.getPA().walkTo(1, 0);
                else if (x < c.getX() && y == c.getY())
                    c.getPA().walkTo(-1, 0);
                else if (y > c.getY() && x == c.getX())
                    c.getPA().walkTo(0, 1);
                else if (y < c.getY() && x == c.getX())
					c.getPA().walkTo(0, -1);
				else
					c.sendMessage("Can't reach that.");
                break;
            case 4419:
                if (x == 2417 && y == 3074) {
                    if (c.getX() >= 2416 && c.getX() <= 2414) {
                        c.getPA().movePlayer(2417, 3077, 0);
                    } else {
                        c.getPA().movePlayer(2416, 3074, 0);
                    }
                }
                break;
            case 4911:
                if (x == 2421 && y == 3073 && c.heightLevel == 1) {
                    c.getPA().movePlayer(2421, 3074, 0);
                }
                if (x == 2378 && y == 3134 && c.heightLevel == 1) {
                    c.getPA().movePlayer(2378, 3133, 0);
                }
                break;
            case 1747:
                if (x == 2421 && y == 3073 && c.heightLevel == 0) {
                    c.getPA().movePlayer(2421, 3074, 1);
                }
                if (x == 2378 && y == 3134 && c.heightLevel == 0) {
                    c.getPA().movePlayer(2378, 3133, 1);
                }
                break;
            case 4912:
                if (x == 2430 && y == 3082 && c.heightLevel == 0) {
                    c.getPA().movePlayer(c.getX(), c.getY() + 6400, 0);
                }
                if (x == 2369 && y == 3125 && c.heightLevel == 0) {
                    c.getPA().movePlayer(c.getX(), c.getY() + 6400, 0);
                }
                break;
            case 1757:
                if (x == 2430 && y == 9482) {
                    c.getPA().movePlayer(2430, 3081, 0);
                } else {
                    c.getPA().movePlayer(2369, 3126, 0);
                }
                break;

            case 4418:
                if (x == 2380 && y == 3127 && c.heightLevel == 0) {
                    c.getPA().movePlayer(2379, 3127, 1);
                }
                if (x == 2369 && y == 3126 && c.heightLevel == 1) {
                    c.getPA().movePlayer(2369, 3127, 2);
                }
                if (x == 2374 && y == 3131 && c.heightLevel == 2) {
                    c.getPA().movePlayer(2373, 3133, 3);
                }
                break;
            case 4420:
                if (x == 2382 && y == 3131 && c.heightLevel == 0) {
                    if (c.getX() >= 2383 && c.getX() <= 2385) {
                        c.getPA().movePlayer(2382, 3130, 0);
                    } else {
                        c.getPA().movePlayer(2383, 3133, 0);
                    }
                }
                break;
            case 4437:
                if (x == 2400 && y == 9512) {
                    c.getPA().movePlayer(2400, 9514, 0);
                } else if (x == 2391 && y == 9501) {
                    c.getPA().movePlayer(2393, 9502, 0);
                } else if (x == 2409 && y == 9503) {
                    c.getPA().movePlayer(2411, 9503, 0);
                } else if (x == 2401 && y == 9494) {
                    c.getPA().movePlayer(2401, 9493, 0);
                }
                break;
            case 1568:
                if (x == 2399 && y == 3099) {
                    c.getPA().movePlayer(2399, 9500, 0);
                } else {
                    c.getPA().movePlayer(2400, 9507, 0);
                }
            case 6281:
                c.getPA().movePlayer(2370, 3132, 2);
                break;
            case 4472:
                c.getPA().movePlayer(2370, 3132, 1);
                break;
            case 6280:
                c.getPA().movePlayer(2429, 3075, 2);
                break;
            case 4471:
                c.getPA().movePlayer(2429, 3075, 1);
                break;
            case 4406:
                CastleWars.removePlayerFromCw(c);
                break;
            case 4407:
                CastleWars.removePlayerFromCw(c);
                break;
            case 4458:
                c.startAnimation(881);
                c.getItems().addItem(4049, 1);
                c.sendMessage("You get some bandages");
                break;
            case 4902: //sara flag
            case 4377:
                switch (CastleWars.getTeamNumber(c)) {
                    case 1:
                        CastleWars.returnFlag(c, c.playerEquipment[c.playerWeapon]);
                        break;
                    case 2:
                        CastleWars.captureFlag(c);
                        break;
                }
                break;
            case 4903: //zammy flag
            case 4378:
                switch (CastleWars.getTeamNumber(c)) {
                    case 1:
                        CastleWars.captureFlag(c);
                        break;
                    case 2:
                        CastleWars.returnFlag(c, c.playerEquipment[c.playerWeapon]);
                        break;
                }
                break;
            case 4461: //barricades
                c.sendMessage("You get a barricade!");
                c.getItems().addItem(4053, 1);
                break;
            case 4463: // explosive potion!
                c.sendMessage("You get an explosive potion!");
                c.getItems().addItem(4045, 1);
                break;
            case 4464: //pickaxe table
                c.sendMessage("You get a bronzen pickaxe for mining.");
                c.getItems().addItem(1265, 1);
                break;
            case 4900:
            case 4901:
                CastleWars.pickupFlag(c);
            default:
                break;

        }
    }
}*/
package server.game.minigames.castlewars;

import server.Config;
import server.content.skills.Mining;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.objects.Object;
import server.game.players.Client;
import server.game.players.PlayerHandler;
import core.util.Misc;

public class CastleWarObjects {

	public static boolean handleObject(final Client c, final int id, final int x, final int y) {
		switch (id) {
		case 4417:
            if (x == 2428 && y == 3081 && c.heightLevel == 1) {
                c.getPA().movePlayer(2430, 3080, 2);
            }
            if (x == 2425 && y == 3074 && c.heightLevel == 2) {
                c.getPA().movePlayer(2426, 3074, 3);
            }
            if (x == 2419 && y == 3078 && c.heightLevel == 0) {
                c.getPA().movePlayer(2420, 3080, 1);
            }
            break;
        case 4415:
            if (x == 2419 && y == 3080 && c.heightLevel == 1) {
                c.getPA().movePlayer(2419, 3077, 0);
            }
            if (x == 2430 && y == 3081 && c.heightLevel == 2) {
                c.getPA().movePlayer(2427, 3081, 1);
            }
            if (x == 2425 && y == 3074 && c.heightLevel == 3) {
                c.getPA().movePlayer(2425, 3077, 2);
            }
            if (x == 2374 && y == 3133 && c.heightLevel == 3) {
                c.getPA().movePlayer(2374, 3130, 2);
            }
            if (x == 2369 && y == 3126 && c.heightLevel == 2) {
                c.getPA().movePlayer(2372, 3126, 1);
            }
            if (x == 2380 && y == 3127 && c.heightLevel == 1) {
                c.getPA().movePlayer(2380, 3130, 0);
            }
            break;
        case 4419:
            if (x == 2417 && y == 3074) {
                if (c.absX == 2417 && c.absY == 3077) {
                	c.getPA().movePlayer(2416, 3074, 0);
                } else {
                	c.getPA().movePlayer(2417, 3077, 0);
                }
            }
            break;
        case 1747:
            if (x == 2421 && y == 3073 && c.heightLevel == 0) {
                c.getPA().movePlayer(2421, 3074, 1);
            }
            if (x == 2378 && y == 3134 && c.heightLevel == 0) {
                c.getPA().movePlayer(2378, 3133, 1);
            }
            break;

        case 4418:
            if (x == 2380 && y == 3127 && c.heightLevel == 0) {
                c.getPA().movePlayer(2379, 3127, 1);
            }
            if (x == 2369 && y == 3126 && c.heightLevel == 1) {
                c.getPA().movePlayer(2369, 3127, 2);
            }
            if (x == 2374 && y == 3131 && c.heightLevel == 2) {
                c.getPA().movePlayer(2373, 3133, 3);
            }
            break;
        case 4420:
            if (x == 2382 && y == 3131 && c.heightLevel == 0) {
                if (c.getX() >= 2383 && c.getX() <= 2385) {
                    c.getPA().movePlayer(2382, 3130, 0);
                } else {
                    c.getPA().movePlayer(2383, 3133, 0);
                }
            }
            break;
			case 4431 :
			case 4432 :
				if (!(CastleWars.getTeamNumber(c) == 1)) {
					c.sendMessage("You can't fix the enemies main door.");
					return true;
				}
				if (!c.getItems().playerHasItem(4051, 1)) {
					c.sendMessage("You need a toolbox to repair this door.");
					return true;
				}
				c.lastAction = System.currentTimeMillis() + 2400;
				c.startAnimation(894);
				c.getItems().deleteItem(4051, 1);
				c.sendMessage("You attempt to repair the door...");
				
				CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer container) {
						if (Misc.random(2) == 1) {
							c.sendMessage("You successfully repaired the door!");
							CastleWars.saraMainGateHP = 100;
							CastleWars.saraMainDoor = 1;
							CastleWars.updateCwObects("door", 1);
						} else {
							c.sendMessage("You failed at repairing the door.");
						}
						container.stop();
					}
					@Override
					public void stop() {
						c.getPA().resetAnimation();
						
					}
				}, 4);
				return true;
			case 4433 :
			case 4434 :
				if (!(CastleWars.getTeamNumber(c) == 2)) {
					c.sendMessage("You can't fix the enemies main door.");
					return true;
				}
				if (!c.getItems().playerHasItem(4051, 1)) {
					c.sendMessage("You need a toolbox to repair this door.");
					return true;
				}
				c.lastAction = System.currentTimeMillis() + 2400;
				c.startAnimation(894);
				c.getItems().deleteItem(4051, 1);
				c.sendMessage("You attempt to repair the door...");
				CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer container) {
						if (Misc.random(2) == 1) {
							c.sendMessage("You successfully repaired the door!");
							CastleWars.zammyMainGateHP = 100;
							CastleWars.zammyMainDoor = 1;
							CastleWars.updateCwObects("door", 2);
						} else {
							c.sendMessage("You failed at repairing the door.");
						}
						container.stop();
					}
					@Override
					public void stop() {
						c.getPA().resetAnimation();
					}
				}, 4);
				return true;
			case 4385 :
				if (!(CastleWars.getTeamNumber(c) == 2)) {
					c.sendMessage("You can't fix the enemies catapult.");
					return true;
				}
				if (!c.getItems().playerHasItem(4051, 1)) {
					c.sendMessage("You need a toolbox to repair this catapult.");
					return true;
				}
				c.lastAction = System.currentTimeMillis() + 2400;
				c.startAnimation(894);
				c.getItems().deleteItem(4051, 1);
				c.sendMessage("You attempt to repair the catapult...");
				CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer container) {
						if (Misc.random(2) == 1) {
							c.sendMessage("You successfully repaired the catapult!");
							CastleWars.zammyCatapult = true;
							CastleWars.updateCwObects("catapult", 2);
						} else {
							c.sendMessage("You failed at repairing the catapult.");
						}
						container.stop();
					}
					@Override
					public void stop() {
						c.getPA().resetAnimation();
					}
				}, 4);
				return true;

			case 4386 :
				if(c.isRepairing)
					return true;
				if (!(CastleWars.getTeamNumber(c) == 1)) {
					c.sendMessage("You can't fix the enemies catapult.");
					return true;
				}
				if (!c.getItems().playerHasItem(4051, 1)) {
					c.sendMessage("You need a toolbox to repair this catapult.");
					return true;
				}
				c.lastAction = System.currentTimeMillis() + 2400;
				c.startAnimation(894);
				c.getItems().deleteItem(4051, 1);
				c.sendMessage("You attempt to repair the catapult...");
				c.isRepairing = true;
				CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer container) {
						if (Misc.random(2) == 1) {
							c.sendMessage("You successfully repaired the catapult!");
							CastleWars.saraCatapult = true;
							CastleWars.updateCwObects("catapult", 1);
						} else {
							c.sendMessage("You failed at repairing the catapult.");
						}
						container.stop();
					}
					@Override
					public void stop() {
						c.getPA().resetAnimation();
						c.isRepairing = false;
					}
				}, 4);
				return true;

			case 4411 : // Stepping stones + 1 ladder
				if (x == 2377 && y == 3088) {
					if (c.getX() == 2377 && c.getY() == 3089) {
						c.getPA().walkTo(0, -1);
					} else if (c.getX() == 2377 && c.getY() == 3087) {
						c.getPA().walkTo(0, 1);
						c.startAnimation(2588);
					}
				} else if (x == 2377 && y == 3087) {
					if (c.getX() == 2377 && c.getY() == 3088) {
						c.getPA().walkTo(0, -1);
						c.startAnimation(2588);
					} else if (c.getX() == 2377 && c.getY() == 3086) {
						c.getPA().walkTo(0, 1);
						c.startAnimation(2588);
					}
				} else if (x == 2377 && y == 3086) {
					if (c.getX() == 2377 && c.getY() == 3087) {
						c.getPA().walkTo(0, -1);
						c.startAnimation(2588);
					} else if (c.getX() == 2377 && c.getY() == 3085) {
						c.getPA().walkTo(0, 1);
						c.startAnimation(2588);
					}
				} else if (x == 2377 && y == 3085) {
					if (c.getX() == 2377 && c.getY() == 3086) {
						c.getPA().walkTo(0, -1);
						c.startAnimation(2588);
					} else if (c.getX() == 2378 && c.getY() == 3085) {
						c.getPA().walkTo(-1, 0);
						c.startAnimation(2588);
					}
				} else if (x == 2378 && y == 3085) {
					if (c.getX() == 2377 && c.getY() == 3085) {
						c.getPA().walkTo(1, 0);
						c.startAnimation(2588);
					} else if (c.getX() == 2378 && c.getY() == 3084) {
						c.getPA().walkTo(0, 1);
						c.startAnimation(2588);
					}
				} else if (x == 2378 && y == 3084) {
					if (c.getX() == 2378 && c.getY() == 3085) {
						c.getPA().walkTo(0, -1);
						c.startAnimation(2588);
					} else if (c.getX() == 2378 && c.getY() == 3083) {
						c.getPA().walkTo(0, 1);
						c.startAnimation(2588);
					}
				} else if (x == 2420 && y == 3123) {
					if (c.getX() == 2420 && c.getY() == 3122) {
						c.getPA().walkTo(0, 1);
						c.startAnimation(2588);
					} else if (c.getX() == 2419 && c.getY() == 3123) {
						c.getPA().walkTo(1, 0);
						c.startAnimation(2588);
					}
				} else if (x == 2419 && y == 3123) {
					if (c.getX() == 2420 && c.getY() == 3123) {
						c.getPA().walkTo(-1, 0);
						c.startAnimation(2588);
					} else if (c.getX() == 2419 && c.getY() == 3124) {
						c.getPA().walkTo(0, -1);
						c.startAnimation(2588);
					}
				} else if (x == 2419 && y == 3124) {
					if (c.getX() == 2419 && c.getY() == 3125) {
						c.getPA().walkTo(0, -1);
						c.startAnimation(2588);
					} else if (c.getX() == 2419 && c.getY() == 3123) {
						c.getPA().walkTo(0, 1);
						c.startAnimation(2588);
					}
				} else if (x == 2419 && y == 3125) {
					if (c.getX() == 2419 && c.getY() == 3124) {
						c.getPA().walkTo(0, 1);
						c.startAnimation(2588);
					} else if (c.getX() == 2418 && c.getY() == 3125) {
						c.getPA().walkTo(1, 0);
						c.startAnimation(2588);
					}
				} else if (x == 2418 && y == 3125) {
					if (c.getX() == 2419 && c.getY() == 3125) {
						c.getPA().walkTo(-1, 0);
						c.startAnimation(2588);
					} else if (c.getX() == 2418 && c.getY() == 3126) {
						c.getPA().walkTo(0, -1);
						c.startAnimation(2588);
					}
				} else if (x == 2421 && y == 3073 && c.heightLevel == 1) {
					c.getPA().movePlayer(c.getX(), c.getY(), 0);
				}
				return true;
			case 4448 : // collapse tunnel
				if(c.isCollapsing)
					return true;
				if (!Mining.hasPickaxe(c)) {
					c.sendMessage("You do not have a pickaxe that you can use.");
					return true;
				}
				if (x >= 2390 && x <= 2393 && y >= 9500 && y <= 9503 && CastleWars.saraRock1 || x >= 2399 && x <= 2402 && y >= 9511 && y <= 9514 && CastleWars.saraRock2 || x >= 2408 && x <= 2411 && y >= 9502 && y <= 9505 && CastleWars.zammyRock1 || x >= 2400 && x <= 2403 && y >= 9493 && y <= 9496 && CastleWars.zammyRock2) {
					c.sendMessage("This passage is already collapsed.");
					return true;
				}
				c.sendMessage("You attempt to collapse the passage...");
				c.startAnimation(625);
				c.isCollapsing = true;
				CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer p) {
						if (x >= 2390 && x <= 2393 && y >= 9500 && y <= 9503 && CastleWars.saraRock1 || x >= 2399 && x <= 2402 && y >= 9511 && y <= 9514 && CastleWars.saraRock2 || x >= 2408 && x <= 2411 && y >= 9502 && y <= 9505 && CastleWars.zammyRock1 || x >= 2400 && x <= 2403 && y >= 9493 && y <= 9496 && CastleWars.zammyRock2) {
							c.sendMessage("This passage is already collapsed.");
							p.stop();
							return;
						}
							if (x >= 2390 && x <= 2393 && y >= 9500 && y <= 9503) {
								c.getPA().hitPlayers(2391, 2392, 9501, 9502, 1);
								CastleWars.saraRock1 = true;
								CastleWars.updateCwObects("rock", 1);
							}
							if (x >= 2399 && x <= 2402 && y >= 9511 && y <= 9514) {
								c.getPA().hitPlayers(2400, 2401, 9512, 9513, 1);
								CastleWars.saraRock2 = true;
								CastleWars.updateCwObects("rock", 1);
							}
							if (x >= 2408 && x <= 2411 && y >= 9502 && y <= 9505) {
								c.getPA().hitPlayers(2409, 2410, 9503, 9504, 1);
								CastleWars.zammyRock1 = true;
								CastleWars.updateCwObects("rock", 2);
							}
							if (x >= 2400 && x <= 2403 && y >= 9493 && y <= 9496) {
								c.getPA().hitPlayers(2401, 2402, 9494, 9495, 1);
								CastleWars.zammyRock2 = true;
								CastleWars.updateCwObects("rock", 2);
							}
							c.sendMessage("You successfully filled the passage with rocks.");
							p.stop();
					}
					@Override
					public void stop() {
						c.isCollapsing = false;
						c.getPA().resetAnimation();
						c.getPA().resetVariables();
					}
				}, 10);
				return true;

			case 4437 : // mine through tunnel
				if(c.isMining)
					return true;
				if (!Mining.hasPickaxe(c)) {
					c.sendMessage("You do not have a pickaxe that you can use.");
					return true;
				}
				if (x == 2391 && y == 9501 && !CastleWars.saraRock1 || x == 2400 && y == 9512 && !CastleWars.saraRock2 || x == 2409 && y == 9503 && !CastleWars.zammyRock1 || x == 2401 && y == 9494 && !CastleWars.zammyRock2) {
					c.sendMessage("This rock has already been mined.");
					return true;
				}
				c.sendMessage("You attempt to mine the rock...");
				c.startAnimation(625);
				c.isMining = true;
				CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer p) {
						if (x == 2391 && y == 9501 && !CastleWars.saraRock1 || x == 2400 && y == 9512 && !CastleWars.saraRock2 || x == 2409 && y == 9503 && !CastleWars.zammyRock1 || x == 2401 && y == 9494 && !CastleWars.zammyRock2) {
							c.sendMessage("This rock has already been mined.");
							p.stop();
							return;
						}
							if (x == 2391 && y == 9501) {
								CastleWars.saraRock1 = false;
								CastleWars.updateCwObects("rock", 1);
							}
							if (x == 2400 && y == 9512) {
								CastleWars.saraRock2 = false;
								CastleWars.updateCwObects("rock", 1);
							}
							if (x == 2409 && y == 9503) {
								CastleWars.zammyRock1 = false;
								CastleWars.updateCwObects("rock", 2);
							}
							if (x == 2401 && y == 9494) {
								CastleWars.zammyRock2 = false;
								CastleWars.updateCwObects("rock", 2);
							}
							c.sendMessage("You successfully mine through the rocks.");
							p.stop();
					}
					@Override
					public void stop() {
						c.isMining = false;
						c.getPA().resetAnimation();
						c.getPA().resetVariables();
					}
				}, 10);
				return true;

			case 1757 :
				if (x == 2400 && y == 9508) {
					c.getPA().climbLadder(true, 2400, 3107, 0);
				}
				if (x == 2399 && y == 9499) {
					c.getPA().climbLadder(true, 2399, 3100, 0);
				}
				if (x == 2430 && y == 9482) {
					c.getPA().climbLadder(true, 2430, 3081, 0);
				}
				if (x == 2369 && y == 9525) {
					c.getPA().climbLadder(true, 2369, 3126, 0);
				}
				return true;

			case 1568 :
				if (x == 2400 && y == 3108) {
					c.getPA().climbLadder(false, 2400, 9507, 0);
				}
				if (x == 2399 && y == 3099) {
					c.getPA().climbLadder(false, 2399, 9500, 0);
				}
				return true;

			case 6280 :
				if (c.playerEquipment[c.playerWeapon] == 4037 || c.playerEquipment[c.playerWeapon] == 4039) {
					c.sendMessage("You are not allowed in the spawn point with the flag.");
					return true;
				}
				if (!(CastleWars.getTeamNumber(c) == 1)) {
					c.sendMessage("You are not allowed in the other teams spawn point.");
					return true;
				}
				c.getPA().climbLadder(true, 2429, 3075, 2);
				return true;

			case 4471 :
				if (c.playerEquipment[c.playerWeapon] == 4037 || c.playerEquipment[c.playerWeapon] == 4039) {
					c.sendMessage("You are not allowed in the spawn point with the flag.");
					return true;
				}
				if (!(CastleWars.getTeamNumber(c) == 1)) {
					c.sendMessage("You are not allowed in the other teams spawn point.");
					return true;
				}
				c.getPA().climbLadder(false, 2429, 3075, 1);
				return true;

			case 6281 :
				if (c.playerEquipment[c.playerWeapon] == 4037 || c.playerEquipment[c.playerWeapon] == 4039) {
					c.sendMessage("You are not allowed in the spawn point with the flag.");
					return true;
				}
				if (!(CastleWars.getTeamNumber(c) == 2)) {
					c.sendMessage("You are not allowed in the other teams spawn point.");
					return true;
				}
				c.getPA().climbLadder(true, 2370, 3132, 2);
				return true;

			case 4472 :
				if (c.playerEquipment[c.playerWeapon] == 4037 || c.playerEquipment[c.playerWeapon] == 4039) {
					c.sendMessage("You are not allowed in the spawn point with the flag.");
					return true;
				}
				if (!(CastleWars.getTeamNumber(c) == 2)) {
					c.sendMessage("You are not allowed in the other teams spawn point.");
					return true;
				}
				c.getPA().climbLadder(false, 2370, 3132, 1);
				return true;

			case 4469 :
				if (!(CastleWars.getTeamNumber(c) == 1)) {
					c.sendMessage("You are not allowed in the other teams spawn point.");
					return true;
				}
				if (c.playerEquipment[c.playerWeapon] == 4037 || c.playerEquipment[c.playerWeapon] == 4039) {
					c.sendMessage("You are not allowed in the spawn point with the flag.");
					return true;
				}
				if (x == 2426) {
					if (c.getY() == 3080) {
						c.getPA().movePlayer(2426, 3081, c.heightLevel);
					} else if (c.getY() == 3081) {
						c.getPA().movePlayer(2426, 3080, c.heightLevel);
					}
				} else if (x == 2422) {
					if (c.getX() == 2422) {
						c.getPA().movePlayer(2423, 3076, c.heightLevel);
					} else if (c.getX() == 2423) {
						c.getPA().movePlayer(2422, 3076, c.heightLevel);
					}
				}
				return true;

			case 4470 :
				if (!(CastleWars.getTeamNumber(c) == 2)) {
					c.sendMessage("You are not allowed in the other teams spawn point.");
					return true;
				}
				if (c.playerEquipment[c.playerWeapon] == 4037 || c.playerEquipment[c.playerWeapon] == 4039) {
					c.sendMessage("You are not allowed in the spawn point with the flag.");
					return true;
				}
				if (x == 2373 && y == 3126) {
					if (c.getY() == 3126) {
						c.getPA().movePlayer(2373, 3127, 1);
					} else if (c.getY() == 3127) {
						c.getPA().movePlayer(2373, 3126, 1);
					}
				} else if (x == 2377 && y == 3131) {
					if (c.getX() == 2376) {
						c.getPA().movePlayer(2377, 3131, 1);
					} else if (c.getX() == 2377) {
						c.getPA().movePlayer(2376, 3131, 1);
					}
				}
				return true;

			case 4911 :
				if (x == 2421 && y == 3073 && c.heightLevel == 1) {
					c.getPA().climbLadder(false, 2421, 3074, 0);
				}
				if (x == 2378 && y == 3134 && c.heightLevel == 1) {
					c.getPA().climbLadder(false, 2378, 3133, 0);
				}
				return true;

			case 4912 :
				if (x == 2430 && y == 3082 && c.heightLevel == 0) {
					c.getPA().climbLadder(false, 2430, 9481, 0);
				}
				if (x == 2369 && y == 3125 && c.heightLevel == 0) {
					c.getPA().climbLadder(false, 2369, 9526, 0);
				}
				return true;

			case 4465 : // open sara sidedoor
				if(c.pickLocking)
					return true;
				if (!(CastleWars.getTeamNumber(c) == 1)) {
					c.lastAction = System.currentTimeMillis() + 1800;
					c.startAnimation(881);
					c.sendMessage("You attempt to picklock the door...");
					c.pickLocking = true;
					CycleEventHandler.addEvent(c, new CycleEvent() {
						@Override
						public void execute(CycleEventContainer container) {
							if (Misc.random(2) == 1) {
								c.sendMessage("You are successful and the door opens.");
								CastleWars.saraSideDoor = true;
								CastleWars.updateCwObects("door", 1);
							} else {
								c.sendMessage("But you fail.");
							}
							container.stop();
						}
						@Override
						public void stop() {
							c.pickLocking = false;
						}
					}, 3);
				} else {
					CastleWars.saraSideDoor = true;
					CastleWars.updateCwObects("door", 1);
				}
				return true;

			case 4466 : // close sara sidedoor
				CastleWars.saraSideDoor = false;
				CastleWars.updateCwObects("door", 1);
				return true;
			case 4467 : // open zammy sidedoor
				if (!(CastleWars.getTeamNumber(c) == 2)) {
					c.lastAction = System.currentTimeMillis() + 1800;
					c.startAnimation(881);
					c.sendMessage("You attempt to picklock the door...");
					
					CycleEventHandler.addEvent(c, new CycleEvent() {
						@Override
						public void execute(CycleEventContainer container) {
							if (Misc.random(2) == 1) {
								CastleWars.zammySideDoor = true;
								c.sendMessage("You are successful at picklocking the door.");
								CastleWars.updateCwObects("door", 2);
							} else {
								c.sendMessage("But you fail.");
							}
							container.stop();
						}
						@Override
						public void stop() {
							
						}
					}, 3);
				} else {
					CastleWars.zammySideDoor = true;
					CastleWars.updateCwObects("door", 2);
				}
				return true;

			case 4468 : // close zammy sidedoor
				CastleWars.zammySideDoor = false;
				CastleWars.updateCwObects("door", 2);
				return true;

			case 4900 : // Sara flag ground object
				/*if (!(CastleWars.saraFlag == 1) && !(CastleWars.saraFlag == 2)) {
					return true;
				}
				if ((CastleWars.getTeamNumber(c) == 1)) {
					CastleWars.returnFlag(c, 4037);
				} else if ((CastleWars.getTeamNumber(c) == 1) || (CastleWars.getTeamNumber(c) == 2)) {
					CastleWars.captureFlag(c);
				}*/
				CastleWars.pickupFlag(c);
				return true;
				
            case 4902: //sara flag
            case 4377:
                switch (CastleWars.getTeamNumber(c)) {
                    case 1:
                        CastleWars.returnFlag(c, c.playerEquipment[c.playerWeapon]);
                        CastleWars.updateCwObects("flag", CastleWars.getTeamNumber(c));
                        return true;
                    case 2:
                        CastleWars.captureFlag(c);
                        CastleWars.updateCwObects("flag", CastleWars.getTeamNumber(c));
                        return true;
                }
                return true;
            case 4903: //zammy flag
            case 4378:
                switch (CastleWars.getTeamNumber(c)) {
                    case 1:
                        CastleWars.captureFlag(c);
                        CastleWars.updateCwObects("flag", CastleWars.getTeamNumber(c));
                        return true;
                    case 2:
                        CastleWars.returnFlag(c, c.playerEquipment[c.playerWeapon]);
                        CastleWars.updateCwObects("flag", CastleWars.getTeamNumber(c));
                        return true;
                }
                return true;

			case 4901 : // Zammy flag ground object
				/*if (!(CastleWars.zammyFlag == 1) && !(CastleWars.zammyFlag == 2)) {
					return true;
				}
				if ((CastleWars.getTeamNumber(c) == 2)) {
					CastleWars.returnFlag(c, 4039);
				} else if ((CastleWars.getTeamNumber(c) == 1) || (CastleWars.getTeamNumber(c) == 2)) {
					CastleWars.captureFlag(c);
				}*/
				CastleWars.pickupFlag(c);
				return true;

			case 4381 :
				int[][] sDamageArea = {{2417, 3103}, {2409, 3093}, {2402, 3086}, {2397, 3079}, {2420, 3096}};
				if (!(CastleWars.getTeamNumber(c) == 2)) {
					c.sendMessage("Your team can't use this catapult!");
					return true;
				}
				if (System.currentTimeMillis() - CastleWars.ZCatapult < 20000) {
					c.sendMessage("The catapult must cool down before firing again.");
					return true;
				}
				if (!c.getItems().playerHasItem(4043, 1)) {
					c.sendMessage("You don't have anything to load the catapault with!");
					return true;
				}
				int p = 0;//Misc.random(4);
				c.getPA().createPlayersStillGfx(287, sDamageArea[p][0], sDamageArea[p][1], 0, 1);
				c.getPA().hitPlayers(sDamageArea[p][0] - 2, sDamageArea[p][0] + 2, sDamageArea[p][1] - 2, sDamageArea[p][1] + 2, 2);
				c.getItems().deleteItem(4043, 1);
				c.sendMessage("You fire the catapult!");
				CastleWars.ZCatapult = System.currentTimeMillis(); // c.getPA().showInterface(11169);
				return true;

			case 4382 :
				if (!(CastleWars.getTeamNumber(c) == 1)) {
					c.sendMessage("Your team can't use this catapult!");
					return true;
				}
				if (System.currentTimeMillis() - CastleWars.SCatapult < 20000) {
					c.sendMessage("The catapult must cool down before firing again.");
					return true;
				}
				if (!c.getItems().playerHasItem(4043, 1)) {
					c.sendMessage("You don't have anything to load the catapault with!");
					return true;
				}
				c.getPA().hitPlayers(2379, 2393, 3104, 3118, 2);
				c.getItems().deleteItem(4043, 1);
				c.sendMessage("You fire the catapult!");
				CastleWars.SCatapult = System.currentTimeMillis(); // c.getPA().showInterface(11169);
				return true;

			case 4406 :
			case 4407 :
			case 4389 :
			case 4390 :
				CastleWars.removePlayerFromCw(c);
				return true;
            case 4387:
                CastleWars.addToWaitRoom(c, 1); //saradomin
                break;
            case 4388:
                CastleWars.addToWaitRoom(c, 2); // zamorak
                break;
            case 4408:
                CastleWars.addToWaitRoom(c, 3); //guthix
                break;
			case 4458 :
				CastleWars.takeFromStall(c, 4049);
				return true;
			case 4459 :
				CastleWars.takeFromStall(c, 4051);
				return true;
			case 4460 :
				CastleWars.takeFromStall(c, 4043);
				return true;
			case 4461 :
				CastleWars.takeFromStall(c, 4053);
				return true;
			case 4462 :
				CastleWars.takeFromStall(c, 4047);
				return true;
			case 4463 :
				CastleWars.takeFromStall(c, 4045);
				return true;
			case 4464 :
				CastleWars.takeFromStall(c, 1265);
				return true;
			case 4423 :
			case 4424 :
				if (!(CastleWars.getTeamNumber(c) == 1)) {
					c.sendMessage("Your team cannot open this door!");
					return true;
				}
				CastleWars.saraMainDoor = 2;
				CastleWars.updateCwObects("door", 1);
				return true;
			case 4425 :
			case 4426 :
				CastleWars.saraMainDoor = 1;
				CastleWars.updateCwObects("door", 1);
				return true;
			case 4427 :
			case 4428 :
				if (!(CastleWars.getTeamNumber(c) == 2)) {
					c.sendMessage("Your team cannot open this door!");
					return true;
				}
				CastleWars.zammyMainDoor = 2;
				CastleWars.updateCwObects("door", 2);
				return true;
			case 4429 :
			case 4430 :
				CastleWars.zammyMainDoor = 1;
				CastleWars.updateCwObects("door", 2);
				return true;
			default :
				return false;
		}
		return false;
	}

	public static void castleWarsFlags(Client c, int team) {
		if (team == 1) {
			if ((CastleWars.saraFlag == 2)) {
				new Object(4900, c.getX(), c.getY(), c.heightLevel, -3, 10, 4900, -1, 1);
				//Server.objectHandler.createAnObject(c, 4900, c.getX(), c.getY(), c.heightLevel, -3, 10);
			} else {
				new Object(-1, c.getX(), c.getY(), c.heightLevel, -3, 10, -1, -1, 1);
				//Server.objectHandler.createAnObject(c, -1, c.getX(), c.getY(), c.heightLevel, -3, 10);
			}
			if ((CastleWars.saraFlag == 1) || (CastleWars.saraFlag == 2)) {
				new Object(4377, 2429, 3074, 3, -3, 10, 4377, -1, 1);
				//Server.objectHandler.createAnObject(c, 4377, 2429, 3074, 3, -3, 10);
			} else {
				new Object(4902, 2429, 3074, 3, -3, 10, 4902, -1, 1);
				//Server.objectHandler.createAnObject(c, 4902, 2429, 3074, 3, -3, 10);
			}
		} else if (team == 2) {
			if (CastleWars.zammyFlag == 2) {
				new Object(4901, c.getX(), c.getY(), c.heightLevel, -1, 10, 4901, -1, 1);
			} else {
				new Object(-1, c.getX(), c.getY(), c.heightLevel, -1, 10, -1, -1, 1);
			}
			if (CastleWars.zammyFlag == 1 || CastleWars.zammyFlag == 2) {
				 new Object(4378, 2370, 3133, 3, -1, 10, 4378, -1, 1);
			} else {
				new Object(4903, 2370, 3133, 3, -1, 10, 4903, -1, 1);
			}
		}
	}

	public static void castleWarsRocks(Client c, int team) {
		/*if (!c.inCwUnderground()) {
			return;
		}*/
		if (team == 1) {
			if (!CastleWars.saraRock1) {
				new Object(4439, 2391, 9501, 0, 0, 10, 4439, -1, 1);
				//Server.objectHandler.createAnObject(c, 4439, 2391, 9501, 0, 0, 10);
			} else {
				new Object(4437, 2391, 9501, 0, 0, 10, 4437, -1, 1);
				//Server.objectHandler.createAnObject(c, 4437, 2391, 9501, 0, 0, 10);
			}
			if (!CastleWars.saraRock2) {
				new Object(4439, 2400, 9512, 0, 0, 10, 4439, -1, 1);
				//Server.objectHandler.createAnObject(c, 4439, 2400, 9512, 0, 0, 10);
			} else {
				new Object(4437, 2400, 9512, 0, 0, 10, 4437, -1, 1);
				//Server.objectHandler.createAnObject(c, 4437, 2400, 9512, 0, 0, 10);
			}
		} else if (team == 2) {
			if (!CastleWars.zammyRock1) {
				new Object(4439, 2409, 9503, 0, 0, 10, 4439, -1, 1);
				//Server.objectHandler.createAnObject(c, 4439, 2409, 9503, 0, 0, 10);
			} else {
				new Object(4437, 2409, 9503, 0, 0, 10, 4437, -1, 1);
				//Server.objectHandler.createAnObject(c, 4437, 2409, 9503, 0, 0, 10);
			}
			if (!CastleWars.zammyRock2) {
				new Object(4439, 2401, 9494, 0, 0, 10, 4439, -1, 1);
				//Server.objectHandler.createAnObject(c, 4439, 2401, 9494, 0, 0, 10);
			} else {
				new Object(4437, 2401, 9494, 0, 0, 10, 4437, -1, 1);
				//Server.objectHandler.createAnObject(c, 4437, 2401, 9494, 0, 0, 10);
			}
		}
	}

	public static void castleWarsCatapults(Client c, int team) {
		/*if (c.inCwUnderground()) {
			return;
		}*/
		if (team == 1) {
			if (!CastleWars.saraCatapult) {
				new Object(4386, 2413, 3088, 0, 0, 10, 4386, -1, 1);
				//Server.objectHandler.createAnObject(c, 4386, 2413, 3088, 0, 0, 10);
			} else {
				new Object(4382, 2413, 3088, 0, 0, 10, 4382, -1, 1);
				//Server.objectHandler.createAnObject(c, 4382, 2413, 3088, 0, 0, 10);
			}
		} else if (team == 2) {
			if (!CastleWars.zammyCatapult) {
				new Object(4385, 2384, 3117, 0, -2, 10, 4385, -1, 1);
				//Server.objectHandler.createAnObject(c, 4385, 2384, 3117, 0, -2, 10);
			} else {
				new Object(4381, 2384, 3117, 0, -2, 10, 4381, -1, 1);
				//Server.objectHandler.createAnObject(c, 4381, 2384, 3117, 0, -2, 10);
			}
		}
	}

	public static void castleWarsDoors(Client c, int team) {
		/*if (c.inCwUnderground()) {
			return;
		}*/
		if (team == 1) { // 1 = north, 2 = east, 3 = south, 4/0 = west
			if (CastleWars.saraSideDoor) {
				new Object(4466, 2414, 3073, 0, 1, 0, 4466, -1, 1);
				new Object(-1, 2415, 3073, 0, 0, 0, -1, -1, 1);
				//Server.objectHandler.createAnObject(c, 4466, 2414, 3073, 0, 1, 0);
				//Server.objectHandler.createAnObject(c, -1, 2415, 3073, 0, 0, 0);
			} else {
				new Object(4465, 2415, 3073, 0, 0, 0, 4465, -1, 1);
				new Object(-1, 2414, 3073, 0, 0, 0, -1, -1, 1);
				//Server.objectHandler.createAnObject(c, 4465, 2415, 3073, 0, 0, 0);
				//Server.objectHandler.createAnObject(c, -1, 2414, 3073, 0, 0, 0);
			}
			if (CastleWars.saraMainDoor == 1) {
				new Object(4423, 2426, 3088, 0, 3, 0, 4423, -1, 1);
				new Object(4424, 2427, 3088, 0, 3, 0, 4424, -1, 1);
				new Object(-1, 2426, 3087, 0, 0, 0, -1, -1, 1);
				new Object(-1, 2427, 3087, 0, 0, 0, -1, -1, 1);
				//Server.objectHandler.createAnObject(c, 4423, 2426, 3088, 0, 3, 0);
				//Server.objectHandler.createAnObject(c, 4424, 2427, 3088, 0, 3, 0);
				//Server.objectHandler.createAnObject(c, -1, 2426, 3087, 0, 0, 0);
				//Server.objectHandler.createAnObject(c, -1, 2427, 3087, 0, 0, 0);
			} else {
				new Object(CastleWars.saraMainDoor == 2 ? 4425 : 4432, 2426, 3087, 0, 0, 0, CastleWars.saraMainDoor == 2 ? 4425 : 4432, -1, 1);
				new Object(CastleWars.saraMainDoor == 2 ? 4426 : 4431, 2427, 3087, 0, 2, 0, CastleWars.saraMainDoor == 2 ? 4426 : 4431, -1, 1);
				new Object(-1, 2426, 3088, 0, 3, 0, -1, -1, 1);
				new Object(-1, 2427, 3088, 0, 3, 0, -1, -1, 1);
				//Server.objectHandler.createAnObject(c, CastleWars.saraMainDoor == 2 ? 4425 : 4432, 2426, 3087, 0, 0, 0);
				//Server.objectHandler.createAnObject(c, CastleWars.saraMainDoor == 2 ? 4426 : 4431, 2427, 3087, 0, 2, 0);
				//Server.objectHandler.createAnObject(c, -1, 2426, 3088, 0, 3, 0);
				//Server.objectHandler.createAnObject(c, -1, 2427, 3088, 0, 3, 0);
			}
		} else if (team == 2) {
			if (CastleWars.zammySideDoor) {
				new Object(4468, 2385, 3134, 0, 3, 0, 4468, -1, 1);
				new Object(-1, 2384, 3134, 0, 2, 0, -1, -1, 1);
				//Server.objectHandler.createAnObject(c, 4468, 2385, 3134, 0, 3, 0);
				//Server.objectHandler.createAnObject(c, -1, 2384, 3134, 0, 2, 0);
			} else {
				new Object(4467, 2384, 3134, 0, 2, 0, 4467, -1, 1);
				new Object(-1, 2385, 3134, 0, 0, 0, -1, -1, 1);
				//Server.objectHandler.createAnObject(c, 4467, 2384, 3134, 0, 2, 0);
				//Server.objectHandler.createAnObject(c, -1, 2385, 3134, 0, 0, 0);
			}
			if (CastleWars.zammyMainDoor == 1) {
				new Object(4427, 2373, 3119, 0, 1, 0, 4427, -1, 1);
				new Object(4428, 2372, 3119, 0, 1, 0, 4428, -1, 1);
				new Object(-1, 2373, 3120, 0, 0, 0, -1, -1, 1);
				new Object(-1, 2372, 3120, 0, 0, 0, -1, -1, 1);
				//Server.objectHandler.createAnObject(c, 4427, 2373, 3119, 0, 1, 0);
				//Server.objectHandler.createAnObject(c, 4428, 2372, 3119, 0, 1, 0);
				//Server.objectHandler.createAnObject(c, -1, 2373, 3120, 0, 0, 0);
				//Server.objectHandler.createAnObject(c, -1, 2372, 3120, 0, 0, 0);
			} else {
				new Object(CastleWars.zammyMainDoor == 2 ? 4429 : 4433, 2373, 3120, 0, 2, 0, CastleWars.zammyMainDoor == 2 ? 4429 : 4433, -1, 1);
				new Object(CastleWars.zammyMainDoor == 2 ? 4430 : 4434, 2372, 3120, 0, 0, 0, CastleWars.zammyMainDoor == 2 ? 4430 : 4434, -1, 1);
				new Object(-1, 2373, 3119, 0, 1, 0, -1, -1, 1);
				new Object(-1, 2372, 3119, 0, 2, 0, -1, -1, 1);
				//Server.objectHandler.createAnObject(c, CastleWars.zammyMainDoor == 2 ? 4429 : 4433, 2373, 3120, 0, 2, 0);
				//Server.objectHandler.createAnObject(c, CastleWars.zammyMainDoor == 2 ? 4430 : 4434, 2372, 3120, 0, 0, 0);
				//Server.objectHandler.createAnObject(c, -1, 2373, 3119, 0, 1, 0);
				//Server.objectHandler.createAnObject(c, -1, 2372, 3119, 0, 2, 0);
			}
		}
	}

	public void reloadCastleWarsObjects() {
		for (int d = 0; d < Config.MAX_PLAYERS; d++) {
			if (PlayerHandler.players[d] != null && PlayerHandler.players[d].isActive) {
				Client p = (Client) PlayerHandler.players[d];
				if (CastleWars.isInCw(p)) {
					CastleWars.updateCwObects(p);
				}
			}
		}
	}

}