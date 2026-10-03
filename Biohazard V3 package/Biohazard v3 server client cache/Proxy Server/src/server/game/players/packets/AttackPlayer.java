package server.game.players.packets;

import server.Config;
import server.game.items.ItemAssistant;
import server.game.players.Client;
import server.game.players.PlayerHandler;

/**
 * Attack Player
 **/
public class AttackPlayer implements PacketType {

	public static final int ATTACK_PLAYER = 73, MAGE_PLAYER = 249;
	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		c.targeting.playerIndex = 0;
		c.targeting.npcIndex = 0;
		if(c.isDead) {
			return;
		}
		switch(packetType) {		
			
			/**
			* Attack player
			**/
			case ATTACK_PLAYER:
			c.targeting.playerIndex = c.getInStream().readSignedWordBigEndian();
			if(PlayerHandler.players[c.targeting.playerIndex] == null ){
				break;
			}
			
			if(c.timers.respawnTimer > 0) {
				break;
			}
			
			if (c.magic.autocastId > 0)
				c.attackMode.autocasting = true;
			
			if (!c.attackMode.autocasting && c.magic.spellId > 0) {
				c.magic.spellId = 0;
			}
			c.mageFollow = false;
			c.magic.spellId = 0;
			c.attackMode.usingMagic = false;
			boolean usingBow = false;
			boolean usingOtherRangeWeapons = false;
			boolean usingArrows = false;
			boolean usingCross = c.playerEquipment[c.playerWeapon] == 9185;
			for (int bowId : c.BOWS) {
				if(c.playerEquipment[c.playerWeapon] == bowId) {
					usingBow = true;
					if(ItemAssistant.getItemName(c.playerEquipment[c.playerArrows]).contains("arrow")
							|| ItemAssistant.getItemName(c.playerEquipment[c.playerArrows]).contains("bolt")) {
						usingArrows = true;
					}
				}
			}
			if(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).contains("javelin")
					|| ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).contains("dart")
					|| ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).contains("thrownaxe")
					|| ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).contains("knife")) {
				usingOtherRangeWeapons = true;
			}
			if(c.duelStatus == 5) {	
				if(c.duelCount > 0) {
					c.sendMessage("The duel hasn't started yet!");
					c.targeting.playerIndex = 0;
					return;
				}
				if(c.duelRule[9]){
					boolean canUseWeapon = false;
					for(int funWeapon: Config.FUN_WEAPONS) {
						if(c.playerEquipment[c.playerWeapon] == funWeapon) {
							canUseWeapon = true;
						}
					}
					if(!canUseWeapon) {
						c.sendMessage("You can only use fun weapons in this duel!");
						return;
					}
				}
				
				if(c.duelRule[2] && (usingBow || usingOtherRangeWeapons)) {
					c.sendMessage("Range has been disabled in this duel!");
					return;
				}
				if(c.duelRule[3] && (!usingBow && !usingOtherRangeWeapons)) {
					c.sendMessage("Melee has been disabled in this duel!");
					return;
				}
			}
			
			if((usingBow || c.attackMode.autocasting) && c.goodDistance(c.getX(), c.getY(), PlayerHandler.players[c.targeting.playerIndex].getX(), PlayerHandler.players[c.targeting.playerIndex].getY(), 6)) {
				c.attackMode.usingBow = true;
				c.stopMovement();
			}
			
			if(usingOtherRangeWeapons && c.goodDistance(c.getX(), c.getY(), PlayerHandler.players[c.targeting.playerIndex].getX(), PlayerHandler.players[c.targeting.playerIndex].getY(), 3)) {
				c.attackMode.usingRangeWeapon = true;
				c.stopMovement();
			}
			if (!usingBow)
				c.attackMode.usingBow = false;
			if (!usingOtherRangeWeapons)
				c.attackMode.usingRangeWeapon = false;

			if(!usingCross && !usingArrows && usingBow && c.playerEquipment[c.playerWeapon] < 4212 && c.playerEquipment[c.playerWeapon] > 4223) {
				c.sendMessage("You have run out of arrows!");
				return;
			} 
			if(!c.getCombat().correctBowAndArrows()/* < c.playerEquipment[c.playerArrows]*/ && Config.CORRECT_ARROWS && usingBow && !c.getCombat().usingCrystalBow() && c.playerEquipment[c.playerWeapon] != 9185) {
					c.sendMessage("You can't use "+ItemAssistant.getItemName(c.playerEquipment[c.playerArrows]).toLowerCase()+"s with a "+ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase()+".");
					c.stopMovement();
					c.getCombat().resetPlayerAttack();
					return;
			}
			if (c.playerEquipment[c.playerWeapon] == 9185 && !c.getCombat().properBolts()) {
					c.sendMessage("You must use bolts with a crossbow.");
					c.stopMovement();
					c.getCombat().resetPlayerAttack();
					return;				
			}
			if (c.getCombat().checkReqs()) {
				c.followId = c.targeting.playerIndex;
				if (!c.attackMode.usingMagic && !usingBow && !usingOtherRangeWeapons) {
					c.followDistance = 1;
					c.getPA().followPlayer();
				}	
				if (c.timers.attackTimer <= 0) {
					//c.sendMessage("Tried to attack...");
					//c.getCombat().attackPlayer(c.playerIndex);
					//c.attackTimer++;
				}	
			}
			break;
			
			
			/**
			* Attack player with magic
			**/
			case MAGE_PLAYER:
			if (!c.mageAllowed) {
				c.mageAllowed = true;
				break;
			}
			//c.usingSpecial = false;
			//c.getItems().updateSpecialBar();

			c.targeting.playerIndex = c.getInStream().readSignedWordA();
			int castingSpellId = c.getInStream().readSignedWordBigEndian();
			c.attackMode.usingMagic = false;
			if(PlayerHandler.players[c.targeting.playerIndex] == null ){
				break;
			}

			if(c.timers.respawnTimer > 0) {
				break;
			}
			
			for(int i = 0; i < c.MAGIC_SPELLS.length; i++){
				if(castingSpellId == c.MAGIC_SPELLS[i][0]) {
					c.magic.spellId = i;
					c.attackMode.usingMagic = true;
					break;
				}
			}		
			
			if (c.attackMode.autocasting)
				c.attackMode.autocasting = false;
				
			if(!c.getCombat().checkReqs()) {
				break;
			}
			if(c.duelStatus == 5) {	
				if(c.duelCount > 0) {
					c.sendMessage("The duel hasn't started yet!");
					c.targeting.playerIndex = 0;
					return;
				}
				if(c.duelRule[4]) {
					c.sendMessage("Magic has been disabled in this duel!");
					return;
				}
			}
			
			for(int r = 0; r < c.REDUCE_SPELLS.length; r++){	// reducing spells, confuse etc
				if(PlayerHandler.players[c.targeting.playerIndex].REDUCE_SPELLS[r] == c.MAGIC_SPELLS[c.magic.spellId][0]) {
					if((System.currentTimeMillis() - PlayerHandler.players[c.targeting.playerIndex].timers.reduceSpellDelay[r]) < PlayerHandler.players[c.targeting.playerIndex].REDUCE_SPELL_TIME[r]) {
						c.sendMessage("That player is currently immune to this spell.");
						c.attackMode.usingMagic = false;
						c.stopMovement();
						c.getCombat().resetPlayerAttack();
					}
					break;
				}			
			}

			
			if(System.currentTimeMillis() - PlayerHandler.players[c.targeting.playerIndex].timers.teleBlockDelay < PlayerHandler.players[c.targeting.playerIndex].timers.teleBlockLength && c.MAGIC_SPELLS[c.magic.spellId][0] == 12445) {
				c.sendMessage("That player is already affected by this spell.");
				c.attackMode.usingMagic = false;
				c.stopMovement();
				c.getCombat().resetPlayerAttack();
			}
			
	 
			if(c.attackMode.usingMagic) {
				if(c.goodDistance(c.getX(), c.getY(), PlayerHandler.players[c.targeting.playerIndex].getX(), PlayerHandler.players[c.targeting.playerIndex].getY(), 7)) {
					c.stopMovement();
				}
				if (c.getCombat().checkReqs()) {
					c.followId = c.targeting.playerIndex;
					c.mageFollow = true;
				if (c.timers.attackTimer <= 0) {
					//c.getCombat().attackPlayer(c.playerIndex);
					//c.attackTimer++;
				}	
			}
			}
			break;
		
		}
			
		
	}
		
}
