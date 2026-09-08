package server.game.players.packets;

/**
 * @author Ryan / Lmctruck30
 */

import server.content.skills.Cooking;
import server.game.items.UseItem;
import server.game.players.Client;
import server.game.players.PacketType;

public class ItemOnObject implements PacketType {

	@SuppressWarnings("unused")
	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		/*
		 * a = ?
		 * b = ?
		 */
		
		int a = c.getInStream().readUnsignedWord();
		int objectId = c.getInStream().readSignedWordBigEndian();
		int objectY = c.getInStream().readSignedWordBigEndianA();
		int b = c.getInStream().readUnsignedWord();
		int objectX = c.getInStream().readSignedWordBigEndianA();
		int itemId = c.getInStream().readUnsignedWord();
		if(!c.getItems().playerHasItem(itemId, 1)) {
			return;
		}
		c.cookingCoords[0] = objectX;
		c.cookingCoords[1] = objectY;
		c.objectX = objectX;
		c.objectY = objectY;
		c.turnPlayerTo(objectX, objectY);
		UseItem.ItemonObject(c, objectId, objectX, objectY, itemId);
		switch (objectId) {
		case 12269:
		case 2732:
		case 114:
		case 9374:
		case 2728:
		case 25465:
		case 11404:
		case 11405:
		case 11406:
			Cooking.cookThisFood(c, itemId, objectId); 
			break;
		}

	}
}
