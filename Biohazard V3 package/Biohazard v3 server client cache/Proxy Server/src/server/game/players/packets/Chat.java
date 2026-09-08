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
		
		// Handle POS search input via chat
		if (c.posSearchingItem || c.posSearchingPlayer) {
			String chatText = Misc.textUnpack(c.getChatText(), c.packetSize - 2);
			System.out.println("[POS Search Debug] Raw chatText: '" + chatText + "'");
			chatText = chatText.toLowerCase().trim();
			System.out.println("[POS Search Debug] Processed chatText: '" + chatText + "'");
			
			if (chatText.equals("cancel")) {
				c.posSearchingItem = false;
				c.posSearchingPlayer = false;
				c.sendMessage("Search cancelled.");
			} else if (c.posSearchingItem) {
				c.posSearchingItem = false;
				c.posSearchingPlayer = false;
				c.getPA().searchPOSByItemName(chatText);
			} else if (c.posSearchingPlayer) {
				c.posSearchingItem = false;
				c.posSearchingPlayer = false;
				c.getPA().searchPOSByPlayer(chatText);
			}
			// Don't send as public chat - consume the message
			return;
		}
		
		if (!Connection.isMuted(c))
			c.setChatTextUpdateRequired(true);
	}	
}
