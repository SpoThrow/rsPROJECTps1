package server.game.players.packets;

import core.util.Misc;
import server.content.skills.Pouches;
import server.game.players.Client;
import server.game.players.PacketType;


/**
 * Wear Item
 **/
public class WearItem implements PacketType {

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		c.wearId = c.getInStream().readUnsignedWord();
		c.wearSlot = c.getInStream().readUnsignedWordA();
		c.interfaceId = c.getInStream().readUnsignedWordA();
		if(!c.getItems().playerHasItem(c.wearId, 1, c.wearSlot)) {
			return;
		}
		if(c.isDead) {
			return;
		}
		if(c.wearId == 6583) {
			c.getPA().setSidebarInterfaces(c, false);
			c.sendMessage("As you put on the ring you turn into a rock!");
			c.npcId2 = 2626;
			c.isNpc = true;
			c.canWalk = false;
			c.updateRequired = true;
			c.setAppearanceUpdateRequired(true);
		}

		if(c.wearId == 7927) {
			c.getPA().setSidebarInterfaces(c, false);
			c.sendMessage("As you put on the ring you turn into an egg!");
			c.npcId2 = 3689 + Misc.random(5);
			c.isNpc = true;
			c.canWalk = false;
			c.updateRequired = true;
			c.setAppearanceUpdateRequired(true);
		}
		if (c.wearId == 6866) {
		c.startAnimation(3004);
		c.gfx0(516);
		return;
		}
		if (c.wearId == 6867) {
		c.startAnimation(3004);
		c.gfx0(508);
		return;
		}
		if (c.wearId == 6865) {
		c.startAnimation(3004);
		c.gfx0(512);
		return;
		}
		@SuppressWarnings("unused")
		int oldCombatTimer = c.attackTimer;
		if (c.playerIndex > 0 || c.npcIndex > 0)
			c.getCombat().resetPlayerAttack();
		if (c.wearId >= 5509 && c.wearId <= 5515) {
			Pouches.emptyPouch(c, c.wearId);
			return;
		}
		c.getTradeAndDuel().declineDuel();
			//c.attackTimer = oldCombatTimer;
		c.getItems().wearItem(c.wearId, c.wearSlot);
	}

}
