package server.game.players.packets;

import server.game.players.Client;
import server.game.players.PacketType;
import server.game.players.PlayerHandler;
import core.util.Misc;


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
		c.clickNpcType = 0;
		c.clickObjectType = 0;
		if (packetType == 248 || packetType == 164) {
			c.faceUpdate(0);
			c.npcIndex = 0;
			c.playerIndex = 0;
			if (c.followId > 0 || c.followId2 > 0)
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
		c.newWalkCmdSteps = (packetSize - 5)/2;
		if(++c.newWalkCmdSteps > c.walkingQueueSize) {
			c.newWalkCmdSteps = 0;
			return;
		}
		
		c.getNewWalkCmdX()[0] = c.getNewWalkCmdY()[0] = 0;
		
		int firstStepX = c.getInStream().readSignedWordBigEndianA()-c.getMapRegionX()*8;
		for(int i = 1; i < c.newWalkCmdSteps; i++) {
			c.getNewWalkCmdX()[i] = c.getInStream().readSignedByte();
			c.getNewWalkCmdY()[i] = c.getInStream().readSignedByte();
		}
		
		int firstStepY = c.getInStream().readSignedWordBigEndian()-c.getMapRegionY()*8;
		c.setNewWalkCmdIsRunning((c.getInStream().readSignedByteC() == 1) && c.playerEnergy > 0);
		c.isResting = false;
		for(int i1 = 0; i1 < c.newWalkCmdSteps; i1++) {
			c.otherDirection = Misc.direction1(c.absX, c.absY, c.getNewWalkCmdX()[i1] + firstStepX + c.getMapRegionX()*8, c.getNewWalkCmdY()[i1] + firstStepY + c.getMapRegionY()*8);
			c.getNewWalkCmdX()[i1] += firstStepX;
			c.getNewWalkCmdY()[i1] += firstStepY;
		}
	}

}
