package server.game.minigames.barrows;

import server.Server;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.players.Client;
import server.game.players.PlayerSave;
import core.util.Misc;

public class Barrows {

	public static boolean barrowObjects(Client c, int object) {
		for(int i = 0; i < BarrowsData.DOORS.length; i++) {
			if(object == BarrowsData.DOORS[i]) {
				return true;
			}
		}
		for(int i = 0; i < BarrowsData.SARCOPHAGUS.length; i++) {
			if(object == BarrowsData.SARCOPHAGUS[i]) {
				return true;
			}
		}
		for(int i = 0; i < BarrowsData.PLAYER_EXIT.length; i++) {
			if(object == BarrowsData.PLAYER_EXIT[i][0]) {
				return true;
			}
		}
		return false;
	}

	public static void objectAction(Client c, int object) {
		for(int i = 0; i < BarrowsData.PLAYER_EXIT.length; i++) {
			if(BarrowsData.PLAYER_EXIT[i][0] == object) {
				c.getPA().movePlayer(BarrowsData.PLAYER_EXIT[i][1],BarrowsData.PLAYER_EXIT[i][2],0);
				//refreshBrothers(c);
				break;
			}
		}
		for(int i = 0; i < BarrowsData.SARCOPHAGUS.length; i++) {
			if(BarrowsData.SARCOPHAGUS[i] == object) {
				if(c.killedBrother[i] || c.spawnedBrother[i]) {
					c.sendMessage("You have already summoned this brother!");
					return;
				}
				if(c.hiddenBrother == i) {
					c.getDH().sendDialogues(1, -1);
					return;
				}
				c.spawnedBrother[i] = true;
				Server.npcHandler.spawnNpc(c, BarrowsData.BARROW_BROTHER[i][0], BarrowsData.BROTHER_SPAWN[i][0], BarrowsData.BROTHER_SPAWN[i][1], c.heightLevel, 0, BarrowsData.BARROW_BROTHER[i][1], BarrowsData.BARROW_BROTHER[i][2], BarrowsData.BARROW_BROTHER[i][3], BarrowsData.BARROW_BROTHER[i][4], true, true);
				c.sendMessage("You woken up the ancient beast!");
				return;
			}
		}
		for(int i = 0; i < BarrowsData.DOORS.length; i++) {
			if(BarrowsData.DOORS[i] == object) {
				c.getPA().moveThroughDoor(c, object, BarrowsData.DOORS_TYPE[i]);
			}
		}
	}

	public static void teleportUnderground(Client c) {
		int i = Misc.random(BarrowsData.UNDERGROUND_SPAWN.length-1);
		c.teleportToX = BarrowsData.UNDERGROUND_SPAWN[i][0];
		c.teleportToY = BarrowsData.UNDERGROUND_SPAWN[i][1];
		c.heightLevel = 0;
	}

	public static void digToBrother(final Client c) {
		c.startAnimation(830);
		for(int i = 0; i < BarrowsData.BROTHER_AREA.length; i++) {
			if(c.absX >= BarrowsData.BROTHER_AREA[i][0] && c.absX <= BarrowsData.BROTHER_AREA[i][2] && c.absY >= BarrowsData.BROTHER_AREA[i][1] && c.absY <= BarrowsData.BROTHER_AREA[i][3]) {
				teleport(c, i);
				c.stopMovement();
				break;
			}
		}
	}

	public static int getKillcount(Client c) {
		return c.barrowsKill;
	}

	public static void refreshBrothers(Client c) {
		for(int i = 0; i < c.killedBrother.length; i++) {
				c.spawnedBrother[i] = false;
				c.barrowsKill = 0;
				c.killedBrother[i] = false;
		}
	}

	private static void giveHiddenBrother(Client c) {
		boolean show = true;
		if(c.hiddenBrother <= 0) {
			c.hiddenBrother = Misc.random(5);
			if(show) {
				//c.sendMessage("Random brother: "+ BarrowsData.BROTHER_NAME[c.hiddenBrother]+".");
			}
			PlayerSave.saveGame(c);
		}
	}

	private static void teleport(final Client c, final int i) {
		CycleEventHandler.addEvent(c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				c.teleportToX = BarrowsData.PLAYER_ENTRE[i][0];
				c.teleportToY = BarrowsData.PLAYER_ENTRE[i][1];
				c.heightLevel = 3;
				c.getPA().sendFrame99(2);
				giveHiddenBrother(c);
				container.stop();
			}
			@Override
			public void stop() {

			}
		}, 1);
	}

	public static void spawnLastBrother(Client c) {
		if(!c.killedBrother[c.hiddenBrother]) {
			if(c.spawnedFinalBrother) {
				c.sendMessage("You have already spawned the hidden brother.");
				return;
			}
			Server.npcHandler.spawnNpc(c, BarrowsData.BARROW_BROTHER[c.hiddenBrother][0], c.absX, c.absY+ Misc.random(1), c.heightLevel, 0, BarrowsData.BARROW_BROTHER[c.hiddenBrother][1], BarrowsData.BARROW_BROTHER[c.hiddenBrother][2], BarrowsData.BARROW_BROTHER[c.hiddenBrother][3], BarrowsData.BARROW_BROTHER[c.hiddenBrother][4], true, true);
			c.spawnedFinalBrother = true;
		}
	}
}