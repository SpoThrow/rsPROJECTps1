package server.game.players.packets;

import server.game.players.Client;
import server.game.players.PacketType;
import server.game.players.PathFinder;
import server.game.players.PlayerHandler;


/**
 * Walking packet
 **/
public class Walking implements PacketType {

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		if(!c.canWalk())
			return;
		//castlewars
		if(c.isAttackingGate)
			c.isAttackingGate = false;
		if(c.isResetting)
			c.isResetting = false;
		if((c.duelStatus >= 1 && c.duelStatus <= 4) || c.duelStatus == 6) {
			c.getTradeAndDuel().declineDuel();
			c.getTradeAndDuel().resetDuel();
			Client o = (Client) PlayerHandler.players[c.duelingWith];
			if(o!=null) {
				o.getTradeAndDuel().declineDuel();
				o.getTradeAndDuel().resetDuel();
			}
			if(c.duelStatus == 6) {
				c.getTradeAndDuel().claimStakedItems();		
			}
		}
		if(c.openDuel && c.duelStatus != 5) {
			Client o = (Client) PlayerHandler.players[c.duelingWith];
			if(o != null)
				o.getTradeAndDuel().declineDuel();
			c.getTradeAndDuel().declineDuel();
		}
		if(c.inTrade)
			return;
		if(c.usingMagic) {
			c.stopMovement();
		}
		if(c.isWc) {
			c.isWc = false;
		}
		if (c.isDoingEmote) {
            c.startAnimation(65535);
            c.gfx0(65535);
            c.isDoingEmote = false;
        }
		c.walkingToItem = false;
		// 248 is the walk sent with an object or NPC click. Clearing the click
		// here cancels the bank/shop action after you arrive, so it only opens
		// on a second click.
		if (packetType != 248) {
			c.clickNpcType = 0;
			c.clickObjectType = 0;
		}
		if (packetType == 248 || packetType == 164) {
			c.faceUpdate(0);
			c.npcIndex = 0;
			c.playerIndex = 0;
			if (packetType != 248 && (c.followId > 0 || c.followId2 > 0))
				c.getPA().resetFollow();
		}		
		if(c.duelRule[1] && c.duelStatus == 5) {
			if(PlayerHandler.players[c.duelingWith] != null) { 
				if(!c.goodDistance(c.getX(), c.getY(), PlayerHandler.players[c.duelingWith].getX(), PlayerHandler.players[c.duelingWith].getY(), 1) || c.attackTimer == 0) {
					c.sendMessage("Walking has been disabled in this duel!");
				}
			}
			c.playerIndex = 0;	
			return;		
		}
		
		if(c.freezeTimer > 0) {
			if(PlayerHandler.players[c.playerIndex] != null) {
				if(c.goodDistance(c.getX(), c.getY(), PlayerHandler.players[c.playerIndex].getX(), PlayerHandler.players[c.playerIndex].getY(), 1) && packetType != 98) {
					c.playerIndex = 0;	
					return;
				}
			}
			if (packetType != 98) {
				c.sendMessage("A magical force stops you from moving.");
				c.playerIndex = 0;
			}	
			return;
		}
		
		if (System.currentTimeMillis() - c.lastSpear < 4000) {
			c.sendMessage("You have been stunned.");
			c.playerIndex = 0;
			return;
		}
		
		if (packetType == 98) {
			c.mageAllowed = true;
		}
	
		
		
		if(c.respawnTimer > 3) {
			return;
		}
		//Reset all
		c.getPA().resetVariables();
		c.getPA().closeAllWindows();
		c.getPA().removeAllWindows();
		
		if(packetType == 248) {
			packetSize -= 14;
		}
		int steps = (packetSize - 5) / 2;
		if (++steps > c.walkingQueueSize) {
			return;
		}

		int firstStepX = c.getInStream().readSignedWordBigEndianA() - c.getMapRegionX() * 8;
		int[] relX = new int[steps];
		int[] relY = new int[steps];
		relX[0] = 0;
		relY[0] = 0;
		for (int i = 1; i < steps; i++) {
			relX[i] = c.getInStream().readSignedByte();
			relY[i] = c.getInStream().readSignedByte();
		}
		int firstStepY = c.getInStream().readSignedWordBigEndian() - c.getMapRegionY() * 8;
		boolean running = c.getInStream().readSignedByteC() == 1 && c.playerEnergy > 0;
		c.isResting = false;

		int destLocalX = firstStepX;
		int destLocalY = firstStepY;
		for (int i = 1; i < steps; i++) {
			destLocalX = firstStepX + relX[i];
			destLocalY = firstStepY + relY[i];
		}
		int destAbsX = destLocalX + c.getMapRegionX() * 8;
		int destAbsY = destLocalY + c.getMapRegionY() * 8;

		// Server-side BFS (rsmod RouteFinding) — ignore client waypoints for accuracy.
		PathFinder.getPathFinder().findRoute(c, destAbsX, destAbsY, true, 1, 1);
		c.setNewWalkCmdIsRunning(running);
		c.isRunning = running || c.isRunning2;
		c.newWalkCmdSteps = 0;
	}

}
