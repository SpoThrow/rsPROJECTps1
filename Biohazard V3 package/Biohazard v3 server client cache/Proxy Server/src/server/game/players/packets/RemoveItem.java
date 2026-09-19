package server.game.players.packets;

import server.content.skills.JewelryMaking;
import server.content.skills.Smithing;
import server.game.players.Client;
import server.game.players.PacketType;
import server.game.players.Player;


/**
 * Remove Item
 **/
public class RemoveItem implements PacketType {

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		int interfaceId = c.getInStream().readUnsignedWordA();
		int removeSlot = c.getInStream().readUnsignedWordA();
		int removeId = c.getInStream().readUnsignedWordA();
		@SuppressWarnings("unused")
		int shop = 0;
		@SuppressWarnings("unused")
		int value = 0;
		@SuppressWarnings("unused")
		String name = "null";
	
		switch(interfaceId) {
		
		case 4233:
		case 4239:
		case 4245:
			JewelryMaking.mouldItem(c, removeId, 1);
			break;
			
			case 1688:
			c.getItems().removeItem(removeId, removeSlot);
			break;
			
			case 5064:
			c.getItems().bankClickDeposit(removeId, removeSlot);
			break;
			
			case 5382:
			c.getItems().bankClickWithdraw(removeId, removeSlot);
			break;
			
			case 3900:
			c.getShops().buyFromShopPrice(removeId, removeSlot);
			break;
			
			case 3823:
			c.getShops().sellToShopPrice(removeId, removeSlot);
			break;
			
			case 3322:
			if(c.duelStatus <= 0) { 
                c.getTradeAndDuel().tradeItem(removeId, removeSlot, 1);
           	} else {
				c.getTradeAndDuel().stakeItem(removeId, removeSlot, 1);
			}
			break;
			
			case 3415:
			if(c.duelStatus <= 0) { 
				c.getTradeAndDuel().fromTrade(removeId, removeSlot, 1);
           	} 
			break;
			
			case 6669:
			c.getTradeAndDuel().fromDuel(removeId, removeSlot, 1);
			break;
			case 1119:
			case 1120:
			case 1121:
			case 1122:
			case 1123:
				Smithing.readInput(c.playerLevel[Player.playerSmithing], Integer.toString(removeId), c, 1);
			break;
		}
	}
			
}
