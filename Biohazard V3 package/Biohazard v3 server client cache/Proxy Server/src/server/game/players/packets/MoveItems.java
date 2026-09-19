package server.game.players.packets;

import server.game.players.Client;
import server.game.players.PacketType;

public class MoveItems implements PacketType {

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		int interfaceId = c.getInStream().readUnsignedWordBigEndianA();
		int insertMode = c.getInStream().readSignedByteC();
		int itemFrom = c.getInStream().readUnsignedWordBigEndianA();
		int itemTo = c.getInStream().readUnsignedWordBigEndian();
		if (c.inTrade || c.tradeStatus == 1 || c.duelStatus == 1) {
			return;
		}
		if (interfaceId >= 10335 && interfaceId <= 10342) {
			if (!c.isBanking) {
				return;
			}
			int destTab = interfaceId - 10335 + 1;
			int abs = c.getBank().toAbsolute(itemFrom);
			c.getBank().moveToTab(abs, destTab);
			return;
		}
		if (interfaceId == 5382) {
			if (!c.isBanking) {
				return;
			}
			c.getBank().swapOrInsert(itemFrom, itemTo, insertMode == 1 || c.insertMode);
			return;
		}
		c.getItems().moveItems(itemFrom, itemTo, interfaceId);
	}
}
