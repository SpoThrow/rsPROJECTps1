package server.game.players.packets;

import server.Connection;
import server.game.players.Client;
import server.game.players.PacketType;
import core.util.Misc;

/**
 * Chat
 **/
public class Chat implements PacketType {

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		c.setChatTextEffects(c.getInStream().readUnsignedByteS());
		c.setChatTextColor(c.getInStream().readUnsignedByteS());
        c.setChatTextSize((byte)(c.packetSize - 2));
        c.inStream.readBytes_reverseA(c.getChatText(), c.getChatTextSize(), 0);
		
		if (c.isBanking && (c.awaitingBankSearch || c.bankSearching)) {
			String chatText = Misc.textUnpack(c.getChatText(), c.packetSize - 2);
			chatText = chatText.trim();
			if (chatText.equalsIgnoreCase("cancel")) {
				c.getBank().clearSearch();
				c.sendMessage("Bank search cancelled.");
			} else {
				c.getBank().applySearch(chatText);
			}
			return;
		}

		// Handle POS search input via chat
		if (c.posSearchingItem || c.posSearchingPlayer) {
			String chatText = Misc.textUnpack(c.getChatText(), c.packetSize - 2);
			chatText = chatText.toLowerCase().trim();
			if (chatText.equals("cancel")) {
				c.posSearchingItem = false;
				c.posSearchingPlayer = false;
				c.sendMessage("Search cancelled.");
			} else if (c.posSearchingItem) {
				c.posSearchingItem = false;
				c.posSearchingPlayer = false;
				c.getPA().searchPOSByItemName(chatText);
			} else {
				c.posSearchingItem = false;
				c.posSearchingPlayer = false;
				c.getPA().searchPOSByPlayer(chatText);
			}
			return;
		}
		
		if (!Connection.isMuted(c))
			c.setChatTextUpdateRequired(true);
	}	
}
