package server.game.players.packets;

import server.game.players.Client;
import server.game.players.PacketType;

/**
 * Silent Packet
 **/
public class SilentPacket implements PacketType {
	
	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		if (packetType == 0 && c.getOutStream() != null && c != null) {
			c.getOutStream().createFrame(90);
			c.flushOutStream();
		}
	}	
}
