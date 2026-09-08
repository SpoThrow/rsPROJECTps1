package server.game.minigames.bountyhunter;

import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.items.ItemAssistant;
import server.game.players.Client;
import server.game.players.Player;
import server.game.players.PlayerHandler;
import core.util.Misc;

public class BountyHunter {
	
	public static void updateInterface(Client c) {
		if(playerHasTarget(c)) {
			c.getPA().sendFrame126(c.targetName, 25350);
		} else {
			c.getPA().sendFrame126("No Target", 25350);
		}
		if(c.isRogue && c.penaltyTimer && c.safeTimer > 0) {
			c.getPA().sendFrame126("Penalty Timer:", 28502);
		} else if(c.isRogue && !c.penaltyTimer && c.safeTimer > 0) {
			c.getPA().sendFrame126("Leave Timer:", 28502);
		} else if(c.safeTimer <= 0) {
			c.getPA().sendFrame126("", 28502);
			c.getPA().sendFrame126("", 28503);
		}
	}
	
	public static void startPenaltyTimer(Client c) {
		if(c.inBhArea() && c.isRogue) {
			c.penaltyTimer = true;
			c.safeTimer = 180;
		}
	}
	
	public static void startLeaveTimer(Client c) {
		if(c.inBhArea() && c.isRogue) {
			c.safeTimer = 180;
			c.penaltyTimer = false;
		}
	}
	
	public static boolean checkReqs(Client c) {
		if(c == null)
			return false;
		if(c.safeTimer > 0) {
			c.sendMessage("Wait "+c.safeTimer+" more seconds before entering the crater again.");
			return false;
		}
		if(ItemAssistant.getWeaponCount(c) > 4) {
			c.sendMessage("You cannot bring more than 4 weapons with you.");
			return false;
		}
		if(ItemAssistant.getBodyCount(c) > 1) {
			c.sendMessage("You cannot bring more than 1 body part with you.");
			return false;
		}
		if(ItemAssistant.getLegCount(c) > 1) {
			c.sendMessage("You cannot bring more than 1 legs part with you.");
			return false;
		}
		return true;
	}
	
	public static void enterCrater(Client c, int objectId) {
		if(!checkReqs(c))
			return;
		switch(objectId) {
		case 28119: //low
			if(c.combatLevel > 2 && c.combatLevel <= 55) {
				c.getPA().movePlayer(3138, 3669, 0);
				assignSkull(c, 1);
			} else {
				c.sendMessage("Sorry, only players level 3-55 are allowed to enter this crater!");
				return;
			}
			break;
		case 28120: //med
			if(c.combatLevel >= 50 && c.combatLevel <= 100) {
				c.getPA().movePlayer(3138, 3669, 4);
				assignSkull(c, 1);
			} else {
				c.sendMessage("Sorry, only players level 50-100 are allowed to enter this crater!");
				return;
			}
			break;
		case 28121: //hi
			if(c.combatLevel >= 95) {
				c.getPA().movePlayer(3138, 3669, 8);
				assignSkull(c, 1);
			} else {
				c.sendMessage("Sorry, only players level 95+ are allowed to enter this crater!");
				return;
			}
			break;
		}
	}
	
	public static void leaveCrater(final Client c) {
		Client target = PlayerHandler.players[c.targetIndex];
		if(c.safeTimer > 0 && !c.penaltyTimer) {
			c.sendMessage("Wait "+c.safeTimer+" more seconds before leaving the crater.");
			return;
		}
		if(c.targetIndex > 0 && target.targetIndex > 0)
			target.targetIndex = 0;
		if(c.targetIndex > 0)
			resetTarget(c);
		if(c.isRogue) {
			c.safeTimer = 180;
			c.isRogue = false;
		}
		assignSkull(c, 0);
		c.getPA().movePlayer(3179, 3685, 0);
		CycleEventHandler.stopEvents(c);
		checkBHTimer(c);
	}
	
	public static void checkBHTimer(final Client c) {
		if(c.safeTimer > 0) {
			CycleEventHandler.addEvent(c, new CycleEvent() {
				@Override
				public void execute(CycleEventContainer container) {
					if(c.safeTimer > 0) {
						c.safeTimer--;
						if(c.inBhArea() && !c.isRogue){
							c.getPA().walkableInterface(197);
							c.getPA().sendFrame126("@or1@"+c.safeTimer, 199);
						} else if(c.inBhArea() && c.isRogue){
							c.getPA().sendFrame126(""+c.safeTimer, 28503);
						}
						updateInterface(c);
					} else {
						updateInterface(c);
						container.stop();
					}
				}
				@Override
				public void stop() {
					//c.safeTimer = 0;
				}
			}, 2);
		}
	}
	
	public static void assignSkull(Client c, int i) {
		c.isSkulled = i == 1 ? true : false;
		//c.skullTimer = i == 1 ? Config.SKULL_TIMER : -1;
		c.headIconPk = i == 1 ? c.getPA().getBhSkull() : -1;
		c.inBH = i == 1 ? true : false;
		handleBHTargetTimer(c);
		updateInterface(c);
		c.getPA().requestUpdates();
	}
	
	public static boolean playerHasTarget(Client player) {
		return player.targetIndex != 0 && (player.targetName != "" || player.targetName != null);
	}
	
	public static void resetTarget(Client player) {
		Client target = PlayerHandler.players[player.targetIndex];
		handleBHTargetTimer(target);
		target.getPA().createPlayerHints(-1, player.playerId);
		target.targetIndex = 0;
		target.targetName = null;
		updateInterface(target);
		player.getPA().createPlayerHints(-1, player.targetIndex);
		player.targetIndex = 0;
		player.targetName = null;
		updateInterface(player);
	}
	
	public static void assignTarget(Client player) {
		for (Player players : PlayerHandler.players) {
			if (players != null) {
				Client p = (Client)players;
				if(p != player && p.inBH) {
					if(playerHasTarget(p))
						return;
					setTarget(player, p.playerId, p.playerName);
					setTarget(p, player.playerId, player.playerName);
					updateInterface(player);
					updateInterface(p);
				}
			}
		}
	}
	
	public static boolean targetIsNull(String targetName) {
		for (Player p : PlayerHandler.players)
			if (p != null && p.playerName.equalsIgnoreCase(targetName))
				return false;
		return true;
	}

	public static void setTarget(Client player, int targetPlayerId, String targetName) {
		player.targetIndex = targetPlayerId;
		player.targetName = targetName;
		if (PlayerHandler.players[targetPlayerId] != null) {
			player.getPA().createPlayerHints(10, player.targetIndex);
		}
		//player.sendMessage("Target: "+targetName);
	}
	
	public static void handleBHTargetTimer(final Client c) {
			CycleEventHandler.addEvent(c, new CycleEvent() {
				@Override
				public void execute(CycleEventContainer container) {
					//System.out.println(""+playerHasTarget(c));
					if (!playerHasTarget(c) && c.inBH)
						assignTarget(c);
					else
						container.stop();
				}
				@Override
				public void stop() {
				}
			}, 1);
	}
	
	public static void handleBHDeath(Client c) {
		Client target = PlayerHandler.players[c.targetIndex];
		Client rogue = PlayerHandler.players[c.killerId];
		if(c.killerId == c.targetIndex) {
			target.safeTimer = 0;
			if(target.targetName.equalsIgnoreCase(c.playerName)) {
				target.bountyKills++;
				target.isBounty = true;
				target.getPA().loadQuests();
				assignSkull(c, 0);
				resetTarget(target);
				updateInterface(target);
			}
		} else {
			rogue.rogueKills++;
			rogue.isRogue = true;
			startPenaltyTimer(rogue);
			updateInterface(rogue);
			CycleEventHandler.stopEvents(rogue);
			checkBHTimer(rogue);
			rogue.getPA().loadQuests();
			assignSkull(c, 0);
			resetTarget(c);
		}
	}
	
	public static void handleReward(Client c, int reward) {
		if(c.bountyKills >= 10 * c.killsMultiplier) {
			switch(reward) {
			case 1:
				if(c.combatLevel > 2 && c.combatLevel <= 55) {
					c.getItems().addItemToBank(995, (500000+Misc.random(1000000)));
					c.getDH().sendDialogues(513, c.talkingNpc);
				} else {
					c.getDH().sendDialogues(512, c.talkingNpc);
					return;
				}
				break;
			case 2:
				if(c.combatLevel >= 50 && c.combatLevel <= 100) {
					c.getItems().addItemToBank(995, (1000000+Misc.random(2300000)));
					c.getDH().sendDialogues(513, c.talkingNpc);
				} else {
					c.getDH().sendDialogues(512, c.talkingNpc);
					return;
				}
				break;
			case 3:
				if(c.combatLevel >= 95) {
					c.getItems().addItemToBank(995, (2000000+Misc.random(5000000)));
					c.getDH().sendDialogues(513, c.talkingNpc);
				} else {
					c.getDH().sendDialogues(512, c.talkingNpc);
					return;
				}
				break;
			}
			c.killsMultiplier++;
		} else {
			c.getDH().sendDialogues(511, c.talkingNpc);
			return;
		}
	}

}

