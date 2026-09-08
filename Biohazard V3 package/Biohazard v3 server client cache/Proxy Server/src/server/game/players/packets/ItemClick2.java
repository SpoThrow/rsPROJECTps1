package server.game.players.packets;

import server.content.skills.Implings;
import server.content.skills.Implings.ImpRewards;
import server.content.skills.Pouches;
import server.game.players.Client;
import server.game.players.PacketType;
import core.util.Misc;

/**
 * Item Click 2 Or Alternative Item Option 1
 * 
 * @author Ryan / Lmctruck30
 * 
 * Proper Streams
 */

public class ItemClick2 implements PacketType {

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		int itemId = c.getInStream().readSignedWordA();
		
		if (!c.getItems().playerHasItem(itemId,1)) {
			return;
		}
		for (int i = 0; i < Pouches.pouchData.length; i++) {
			if (itemId == Pouches.pouchData[i][0]) {
				Pouches.checkPouch(c, itemId);
			}
		}
		if(Implings.ImpRewards.impReward.containsKey(itemId)) {
			ImpRewards.getReward(c, itemId);
			return;
		}
		switch (itemId) {
		case 6865:
			c.startAnimation(3005);
			c.gfx0(513);
			break;
		case 6866:
			c.startAnimation(3005);
			c.gfx0(517);
			break;
		case 6867:
			c.startAnimation(3005);
			c.gfx0(509);
			break;
		case 4079:
			c.startAnimation(1459);
		break;
		case 11694:
			if(c.getItems().freeSlots() > 3) {
            if (c.getItems().playerHasItem(11694)) {
            c.getItems().deleteItem(11694, 1);
            c.getItems().addItem(11702, 1);
            c.getItems().addItem(11710, 1);
			c.getItems().addItem(11712, 1);
			c.getItems().addItem(11714, 1);
            c.sendMessage("You dismantle your godsword and get shards and a hilt");
            } else {
            c.sendMessage("You need a godsword to dismantle it you duping little jerk");
            }
		} else {
			c.sendMessage("You need at least 4 free slots.");
			return;
		}
         break;
		 
			case 11696:
				if(c.getItems().freeSlots() > 3) {
				if (c.getItems().playerHasItem(11696)) {
            c.getItems().deleteItem(11696, 1);
            c.getItems().addItem(11704, 1);
            c.getItems().addItem(11710, 1);
			c.getItems().addItem(11712, 1);
			c.getItems().addItem(11714, 1);
            c.sendMessage("You dismantle your godsword and get shards and a hilt");
            } else {
            c.sendMessage("You need a godsword to dismantle it you duping little jerk");
            }
		} else {
			c.sendMessage("You need at least 4 free slots.");
			return;
		}
			break;
			case 11698:
				if(c.getItems().freeSlots() > 3) {
				if (c.getItems().playerHasItem(11698)) {
            c.getItems().deleteItem(11698, 1);
            c.getItems().addItem(11706, 1);
            c.getItems().addItem(11710, 1);
			c.getItems().addItem(11712, 1);
			c.getItems().addItem(11714, 1);
            c.sendMessage("You dismantle your godsword and get shards and a hilt");
            } else {
         return;
            }
		} else {
			c.sendMessage("You need at least 4 free slots.");
			return;
		}
			break;
			case 11700:
				if(c.getItems().freeSlots() > 3) {
				//you have to add this cause i dont have zammy boss on my server
				if (c.getItems().playerHasItem(11700)) {
		            c.getItems().deleteItem(11700, 1);
		            c.getItems().addItem(11708, 1);
		            c.getItems().addItem(11710, 1);
					c.getItems().addItem(11712, 1);
					c.getItems().addItem(11714, 1);
		            c.sendMessage("You dismantle your godsword and get shards and a hilt");
		            } else {
		           return;
		            }
				} else {
					c.sendMessage("You need at least 4 free slots.");
					return;
				}
			break;
		default:
			if (c.playerRights == 3)
				Misc.println(c.playerName+ " - Item3rdOption: "+itemId);
			break;
		}

	}

}
