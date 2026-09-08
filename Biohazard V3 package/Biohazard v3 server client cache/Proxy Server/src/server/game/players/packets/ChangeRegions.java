package server.game.players.packets;

import server.Server;
import server.content.music.Music;
import server.game.players.Client;
import server.game.players.PacketType;

/**
 * Change Regions
 */
public class ChangeRegions implements PacketType {

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		//if(!c.mapRegionDidChange)
			//return;
		if(c.isAttackingGate)
			c.isAttackingGate = false;
		Music.playMusic(c);
		Server.itemHandler.reloadItems(c);
		c.clearLists();
		Server.objectManager.loadObjects(c);
		
		c.saveFile = true;
		
		if(c.skullTimer > 0) {
			c.isSkulled = true;	
			c.headIconPk = 0;
			c.getPA().requestUpdates();
		}

	}
		
}
