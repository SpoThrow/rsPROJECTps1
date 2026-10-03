package server.game.players.packets;

import server.game.players.Client;
import server.game.players.PlayerHandler;

public class FollowPlayer implements PacketType {
	
	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		int followPlayer = c.getInStream().readUnsignedWordBigEndian();
		if(PlayerHandler.players[followPlayer] == null) {
			return;
		}
		c.targeting.playerIndex = 0;
		c.targeting.npcIndex = 0;
		c.mageFollow = false;
		c.attackMode.usingBow = false;
		c.attackMode.usingRangeWeapon = false;
		c.followDistance = 1;
		c.followId = followPlayer;
	}	
}
