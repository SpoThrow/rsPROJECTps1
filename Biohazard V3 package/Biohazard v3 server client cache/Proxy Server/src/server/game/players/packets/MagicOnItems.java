package server.game.players.packets;

import server.game.players.Client;


/**
 * Magic on items
 **/
public class MagicOnItems implements PacketType {

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		int slot = c.getInStream().readSignedWord();
		int itemId = c.getInStream().readSignedWordA();
		@SuppressWarnings("unused")
		int junk = c.getInStream().readSignedWord();
		int spellId = c.getInStream().readSignedWordA();
		if(!c.getItems().playerHasItem(itemId, 1, slot))
			return;
		c.attackMode.usingMagic = true;
		c.getPA().magicOnItems(slot, itemId, spellId);
		c.attackMode.usingMagic = false;

	}

}
