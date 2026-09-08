package server.game.players.packets;

import server.Server;
import server.game.players.Client;
import server.game.players.PacketType;

public class ChangeRegion implements PacketType {

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		if(c.isAttackingGate)
			c.isAttackingGate = false;
		c.clearLists();
		Server.objectManager.loadObjects(c);
	}

}
