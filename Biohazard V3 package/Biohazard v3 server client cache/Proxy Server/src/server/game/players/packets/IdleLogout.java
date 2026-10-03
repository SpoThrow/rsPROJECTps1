package server.game.players.packets;


import server.game.players.Client;


public class IdleLogout implements PacketType {
	
	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		if(c.targeting.underAttackBy > 0 || c.targeting.underAttackBy2 > 0)
			return;
		//if (!c.playerName.equalsIgnoreCase("Sanity"))
			//c.logout();
	}
}