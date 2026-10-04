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
			c.getPA().sendFrame126(c.bountyHunter.targetName, 25350);
		} else {
			c.getPA().sendFrame126("No Target", 25350);
		}
		if(c.bountyHunter.isRogue && c.bountyHunter.penaltyTimer && c.bountyHunter.safeTimer > 0) {
			c.getPA().sendFrame126("Penalty Timer:", 28502);
		} else if(c.bountyHunter.isRogue && !c.bountyHunter.penaltyTimer && c.bountyHunter.safeTimer > 0) {
			c.getPA().sendFrame126("Leave Timer:", 28502);
		} else if(c.bountyHunter.safeTimer <= 0) {
			c.getPA().sendFrame126("", 28502);
			c.getPA().sendFrame126("", 28503);
		}
	}
	
	public static void startPenaltyTimer(Client c) {
		if(c.inBhArea() && c.bountyHunter.isRogue) {
			c.bountyHunter.penaltyTimer = true;
			c.bountyHunter.safeTimer = 180;
		}
	}
	
	public static void startLeaveTimer(Client c) {
		if(c.inBhArea() && c.bountyHunter.isRogue) {
			c.bountyHunter.safeTimer = 180;
			c.bountyHunter.penaltyTimer = false;
		}
	}
	
	public static boolean checkReqs(Client c) {
		if(c == null)
			return false;
		if(c.bountyHunter.safeTimer > 0) {
			c.sendMessage("Wait "+c.bountyHunter.safeTimer+" more seconds before entering the crater again.");
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
		Client target = PlayerHandler.players[c.bountyHunter.targetIndex];
		if(c.bountyHunter.safeTimer > 0 && !c.bountyHunter.penaltyTimer) {
			c.sendMessage("Wait "+c.bountyHunter.safeTimer+" more seconds before leaving the crater.");
			return;
		}
		if(c.bountyHunter.targetIndex > 0 && target.bountyHunter.targetIndex > 0)
			target.bountyHunter.targetIndex = 0;
		if(c.bountyHunter.targetIndex > 0)
			resetTarget(c);
		if(c.bountyHunter.isRogue) {
			c.bountyHunter.safeTimer = 180;
			c.bountyHunter.isRogue = false;
		}
		assignSkull(c, 0);
		c.getPA().movePlayer(3179, 3685, 0);
		CycleEventHandler.stopEvents(c);
		checkBHTimer(c);
	}
	
	public static void checkBHTimer(final Client c) {
		if(c.bountyHunter.safeTimer > 0) {
			CycleEventHandler.addEvent(c, new CycleEvent() {
				@Override
				public void execute(CycleEventContainer container) {
					if(c.bountyHunter.safeTimer > 0) {
						c.bountyHunter.safeTimer--;
						if(c.inBhArea() && !c.bountyHunter.isRogue){
							c.getPA().walkableInterface(197);
							c.getPA().sendFrame126("@or1@"+c.bountyHunter.safeTimer, 199);
						} else if(c.inBhArea() && c.bountyHunter.isRogue){
							c.getPA().sendFrame126(""+c.bountyHunter.safeTimer, 28503);
						}
						updateInterface(c);
					} else {
						updateInterface(c);
						container.stop();
					}
				}
				@Override
				public void stop() {
					//c.bountyHunter.safeTimer = 0;
				}
			}, 2);
		}
	}
	
	public static void assignSkull(Client c, int i) {
		c.isSkulled = i == 1 ? true : false;
		//c.skullTimer = i == 1 ? Config.SKULL_TIMER : -1;
		c.appearance.headIconPk = i == 1 ? c.getPA().getBhSkull() : -1;
		c.bountyHunter.inBH = i == 1 ? true : false;
		handleBHTargetTimer(c);
		updateInterface(c);
		c.getPA().requestUpdates();
	}
	
	public static boolean playerHasTarget(Client player) {
		// Was `targetIndex != 0 && (targetName != "" || targetName != null)`, which was
		// satisfied by every possible value of targetName: null passed `!= ""`, and ""
		// passed `!= null`. Both were reference comparisons too. So the whole name half of
		// the check was dead and this only ever tested targetIndex -- meaning a player left
		// holding a stale targetIndex with no usable name was never re-assigned a target by
		// handleBHTargetTimer. Now it actually requires an assigned, non-empty name.
		return player.bountyHunter.targetIndex != 0
				&& player.bountyHunter.targetName != null
				&& !player.bountyHunter.targetName.isEmpty();
	}
	
	public static void resetTarget(Client player) {
		Client target = PlayerHandler.players[player.bountyHunter.targetIndex];
		handleBHTargetTimer(target);
		target.getPA().createPlayerHints(-1, player.playerId);
		target.bountyHunter.targetIndex = 0;
		target.bountyHunter.targetName = null;
		updateInterface(target);
		player.getPA().createPlayerHints(-1, player.bountyHunter.targetIndex);
		player.bountyHunter.targetIndex = 0;
		player.bountyHunter.targetName = null;
		updateInterface(player);
	}
	
	public static void assignTarget(Client player) {
		for (Player players : PlayerHandler.players) {
			if (players != null) {
				Client p = (Client)players;
				if(p != player && p.bountyHunter.inBH) {
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
		player.bountyHunter.targetIndex = targetPlayerId;
		player.bountyHunter.targetName = targetName;
		if (PlayerHandler.players[targetPlayerId] != null) {
			player.getPA().createPlayerHints(10, player.bountyHunter.targetIndex);
		}
		//player.sendMessage("Target: "+targetName);
	}
	
	public static void handleBHTargetTimer(final Client c) {
			CycleEventHandler.addEvent(c, new CycleEvent() {
				@Override
				public void execute(CycleEventContainer container) {
					//System.out.println(""+playerHasTarget(c));
					if (!playerHasTarget(c) && c.bountyHunter.inBH)
						assignTarget(c);
					else
						container.stop();
				}
				@Override
				public void stop() {
				}
			}, 1);
	}
	
	/**
	 * Whether killing {@code dying} earns {@code target} the Bounty Hunter kill credit.
	 *
	 * <p>Split out of {@link #handleBHDeath} so both directions can be tested: the award
	 * branch continues into {@code loadQuests}/{@code assignSkull}/{@code resetTarget},
	 * which need a real login session, so exercising it end to end is not a unit test.
	 *
	 * <p>{@code dying.playerName} is deliberately the receiver: it is always set for a
	 * logged-in player, whereas {@code target.targetName} is null whenever that player has
	 * no assigned target (it defaults to null and {@code resetTarget} sets it back), and
	 * calling {@code equalsIgnoreCase} on it threw out of the death handler.
	 */
	static boolean isKillCreditFor(Client target, Client dying) {
		return dying.playerName != null
				&& dying.playerName.equalsIgnoreCase(target.bountyHunter.targetName);
	}

	public static void handleBHDeath(Client c) {
		int targetIndex = c.bountyHunter.targetIndex;
		int killerId = c.killCredit.killerId;
		Client target = playerAt(targetIndex);
		Client rogue = playerAt(killerId);
		// targetIndex and killerId are 1-based player slots with 0 meaning "none". Resolving
		// them through a guarded lookup matters: the raw PlayerHandler.players[...] threw when
		// the slot was empty, and when both were 0 this took the target branch and wrote
		// safeTimer = 0 onto whichever player happened to occupy slot 0.
		if (targetIndex > 0 && killerId == targetIndex && target != null) {
			target.bountyHunter.safeTimer = 0;
			if(isKillCreditFor(target, c)) {
				target.bountyHunter.bountyKills++;
				target.bountyHunter.isBounty = true;
				target.getPA().loadQuests();
				assignSkull(c, 0);
				resetTarget(target);
				updateInterface(target);
			}
		} else if (rogue != null) {
			rogue.bountyHunter.rogueKills++;
			rogue.bountyHunter.isRogue = true;
			startPenaltyTimer(rogue);
			updateInterface(rogue);
			CycleEventHandler.stopEvents(rogue);
			checkBHTimer(rogue);
			rogue.getPA().loadQuests();
			assignSkull(c, 0);
			resetTarget(c);
		}
	}

	/** Player at a 1-based slot, or {@code null} when the index means "none" (&le;0) or is out of range. */
	private static Client playerAt(int index) {
		if (index <= 0 || index >= PlayerHandler.players.length) {
			return null;
		}
		return PlayerHandler.players[index];
	}
	
	public static void handleReward(Client c, int reward) {
		if(c.bountyHunter.bountyKills >= 10 * c.bountyHunter.killsMultiplier) {
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
			c.bountyHunter.killsMultiplier++;
		} else {
			c.getDH().sendDialogues(511, c.talkingNpc);
			return;
		}
	}

}

