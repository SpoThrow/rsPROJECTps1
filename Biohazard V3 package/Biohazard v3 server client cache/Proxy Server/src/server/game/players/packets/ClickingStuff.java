package server.game.players.packets;

import server.game.players.Client;
import server.game.players.PacketType;
import server.game.players.PlayerHandler;
import core.util.Misc;


/**
 * Clicking stuff (interfaces)
 **/
public class ClickingStuff implements PacketType {

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		if (c.inTrade) {
			if(!c.acceptedTrade) {
				Misc.println("trade reset");
				c.getTradeAndDuel().declineTrade();
			}
		}
		if (c.isBanking)
			c.isBanking = false;
		if(c.isShopping)
			c.isShopping = false;
		if(c.openDuel && c.duelStatus >= 1 && c.duelStatus <= 4) {
			Client o = (Client) PlayerHandler.players[c.duelingWith];
			if(o != null)
				o.getTradeAndDuel().declineDuel();
			c.getTradeAndDuel().declineDuel();
		}
		if(c.isResetting)
			c.isResetting = false;		
		if(c.duelStatus == 6)
			c.getTradeAndDuel().claimStakedItems();
	}
		
}
