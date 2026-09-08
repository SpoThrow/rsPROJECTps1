package server.game.players.packets;

import server.content.skills.JewelryMaking;
import server.content.skills.Smithing;
import server.game.players.Client;
import server.game.players.PacketType;
import server.game.players.Player;
/**
 * Bank 10 Items
 **/
public class Bank10 implements PacketType {

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
	int interfaceId = c.getInStream().readUnsignedWordBigEndian();
	int removeId = c.getInStream().readUnsignedWordA();
	int removeSlot = c.getInStream().readUnsignedWordA();
		switch(interfaceId){
		
		case 4233:
		case 4239:
		case 4245:
			JewelryMaking.mouldItem(c, removeId, 10);
			break;
			
			case 1688:
				c.getPA().useOperate(removeId);
			break;
			case 3900:
				c.getShops().buyItem(removeId, removeSlot, 5);
				break;
			
			case 3823:
				if(!c.getItems().playerHasItem(removeId))
					return;
				c.getShops().sellItem(removeId, removeSlot, 5);
				break;	

			case 5064:
				if(!c.getItems().playerHasItem(removeId))
					return;
				c.getItems().bankItem(removeId, removeSlot, 10);
				break;
			
			case 5382:
			c.getItems().fromBank(removeId, removeSlot, 10);
			break;
			
			case 3322:
				if(!c.getItems().playerHasItem(removeId))
					return;
			if(c.duelStatus <= 0) { 
                c.getTradeAndDuel().tradeItem(removeId, removeSlot, 10);
           	} else {
				c.getTradeAndDuel().stakeItem(removeId, removeSlot, 10);
			}	
			break;
			
			case 3415:
			if(c.duelStatus <= 0) { 
				c.getTradeAndDuel().fromTrade(removeId, removeSlot, 10);
           	} 
			break;
			
			case 6669:
			c.getTradeAndDuel().fromDuel(removeId, removeSlot, 10);
			break;
			case 1119:
			case 1120:
			case 1121:
			case 1122:
			case 1123:
				Smithing.readInput(c.playerLevel[Player.playerSmithing], Integer.toString(removeId), c, 5);
			break;
		
		}	
	}

}
