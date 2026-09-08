package server.game.players.packets;

import server.Config;
import server.Server;
import server.game.players.Client;
import server.game.players.PacketType;
import server.game.players.PlayerSave;

/**
 * Drop Item
 **/
public class DropItem implements PacketType {

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		int itemId = c.getInStream().readUnsignedWordA();
		c.getInStream().readUnsignedByte();
		c.getInStream().readUnsignedByte();
		int slot = c.getInStream().readUnsignedWordA();
		c.alchDelay = System.currentTimeMillis();
		if(!c.getItems().playerHasItem(itemId, 1)) {
			return;
		}
		if(c.inTrade || c.isBanking) {
			c.sendMessage("You can't drop items while trading or banking!");
			return;
		}
		if(c.isDead) {
			return;
		}
		if(c.arenas()) {
			c.sendMessage("You can't drop items inside the arena!");
			return;
		}
		
		if(itemId == 4045) {
			int explosiveHit = 15;
			c.startAnimation(827);
			c.getItems().deleteItem(itemId, slot, c.playerItemsN[slot]);
			c.handleHitMask(explosiveHit);
			c.dealDamage(explosiveHit);
			c.getPA().refreshSkill(3);
			c.forcedText = "Ow! That really hurt!";
			c.forcedChatUpdateRequired = true;
			c.updateRequired = true;
		}

		boolean droppable = true;
		for (int i : Config.UNDROPPABLE_ITEMS) {
			if (i == itemId) {
				droppable = false;
				break;
			}
		}
		if(c.playerItemsN[slot] != 0 && itemId != -1 && c.playerItems[slot] == itemId + 1) {
			if(droppable) {
				if (c.underAttackBy > 0) {
					if (c.getShops().getItemShopValue(itemId) > 1000) {
						c.sendMessage("You may not drop items worth more than 1000 while in combat.");
						return;
					}
				}
				Server.itemHandler.createGroundItem(c, itemId, c.getX(), c.getY(), c.playerItemsN[slot], c.getId());
				c.getItems().deleteItem(itemId, slot, c.playerItemsN[slot]);
				PlayerSave.saveGame(c);
			} else {
				c.sendMessage("This items cannot be dropped.");
			}
		}

	}
}
