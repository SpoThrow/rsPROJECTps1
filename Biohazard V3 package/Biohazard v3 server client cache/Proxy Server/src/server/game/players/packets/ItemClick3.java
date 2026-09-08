package server.game.players.packets;

import server.content.skills.TalismanHandler;
import server.content.skills.TalismanHandler.talismanData;
import server.game.items.ItemAssistant;
import server.game.players.Client;
import server.game.players.PacketType;
import core.util.Misc;

/**
 * Item Click 3 Or Alternative Item Option 1
 * 
 * @author Ryan / Lmctruck30
 * 
 * Proper Streams
 */

public class ItemClick3 implements PacketType {

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		int itemId11 = c.getInStream().readSignedWordBigEndianA();
		int itemId1 = c.getInStream().readSignedWordA();
		int itemId = c.getInStream().readSignedWordA();
		if(!c.getItems().playerHasItem(itemId, 1)) {
			return;
		}
		final String name = ItemAssistant.getItemName(itemId);
        if (c.getPotions().isPotion(itemId)) {
        	c.sendMessage("You empty the " + name + ".");
        	c.getItems().deleteItem(itemId, 1);
        	c.getItems().addItem(229, 1);
        }
/* 		final String name = c.getItems().getItemName(itemId);
		if (c.getPotions().isPotion(itemId)) {
			c.sendMessage("There are still some potion left...");
			c.sendMessage("You empty it anyways.");
			c.getItems().deleteItem(itemId, 1);
			c.getItems().addItem(229, 1);
		}
 */
		for (talismanData t : talismanData.values()) {
			if (itemId == t.getTalisman()) {
				TalismanHandler.handleTalisman(c, itemId);
			}
		}
		switch (itemId) {
		case 6865:
			c.startAnimation(3006);
			c.gfx0(514);
			break;
		case 6866:
			c.startAnimation(3006);
			c.gfx0(518);
			break;
		case 6867:
			c.startAnimation(3006);
			c.gfx0(510);
			break;
		case 4079:
			c.startAnimation(1460);
		break;

		case 1712:
			c.getPA().handleGlory(itemId);
			break;
		case 3853:
			c.getPA().handleGlory(itemId);
			break;
		case 2552:
			c.getDH().sendDialogues(68, 0);
			break;
			
		default:
			if (c.playerRights == 3)
				Misc.println(c.playerName+ " - Item3rdOption: "+itemId+" : "+itemId11+" : "+itemId1);
			break;
		}

	}

}
