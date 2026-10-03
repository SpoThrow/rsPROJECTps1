package server.game.players.packets;

import server.Config;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.items.ItemAssistant;
import server.game.items.UseItem;
import server.game.npcs.NPCHandler;
import server.game.npcs.WorldAdventurer;
import server.game.players.Client;

/**
 * Click NPC
 */
public class ClickNPC implements PacketType {
	public static final int ATTACK_NPC = 72, MAGE_NPC = 131, FIRST_CLICK = 155, SECOND_CLICK = 17, THIRD_CLICK = 21, FOURTH_CLICK = 57; //castlewars
	@Override
	public void processPacket(final Client c, int packetType, int packetSize) {
		c.targeting.npcIndex = 0;
		c.npcInteraction.npcClickIndex = 0;
		c.targeting.playerIndex = 0;
		c.npcInteraction.clickNpcType = 0;
		c.getPA().resetFollow();
		if(!c.canWalk)
			return;
		switch(packetType) {
			
			/**
			* Attack npc melee or range
			**/
			case ATTACK_NPC:
			if (!c.mageAllowed) {
				c.mageAllowed = true;
				c.sendMessage("I can't reach that.");
				break;
			}
			c.targeting.npcIndex = c.getInStream().readUnsignedWordA();
			if (NPCHandler.npcs[c.targeting.npcIndex] == null) {
				c.targeting.npcIndex = 0;
				break;
			}
			if (WorldAdventurer.isAdventurer(c.targeting.npcIndex)) {
				c.sendMessage("Max is too busy training.");
				c.targeting.npcIndex = 0;
				break;
			}
			if (NPCHandler.npcs[c.targeting.npcIndex].MaxHP == 0) {
				c.targeting.npcIndex = 0;
				break;
			}			
			if(NPCHandler.npcs[c.targeting.npcIndex] == null){
				break;
			}
			if (c.magic.autocastId > 0)
				c.attackMode.autocasting = true;			
			if (!c.attackMode.autocasting && c.magic.spellId > 0) {
				c.magic.spellId = 0;
			}
			c.followId2 = c.targeting.npcIndex;
			c.faceUpdate(c.targeting.npcIndex);
			c.attackMode.usingMagic = false;
			boolean usingBow = false;
			boolean usingOtherRangeWeapons = false;
			boolean usingArrows = false;
			boolean usingCross = c.playerEquipment[c.playerWeapon] == 9185;
			if (c.playerEquipment[c.playerWeapon] >= 4214 && c.playerEquipment[c.playerWeapon] <= 4223)
				usingBow = true;
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
			if((usingBow || c.attackMode.autocasting) && c.goodDistance(c.getX(), c.getY(), NPCHandler.npcs[c.targeting.npcIndex].getX(), NPCHandler.npcs[c.targeting.npcIndex].getY(), 7)) {
				c.stopMovement();
			}
			
			if(usingOtherRangeWeapons && c.goodDistance(c.getX(), c.getY(), NPCHandler.npcs[c.targeting.npcIndex].getX(), NPCHandler.npcs[c.targeting.npcIndex].getY(), 4)) {
				c.stopMovement();
			}
			if(!usingCross && !usingArrows && usingBow && c.playerEquipment[c.playerWeapon] < 4212 && c.playerEquipment[c.playerWeapon] > 4223 && !usingCross) {
				c.sendMessage("You have run out of arrows!");
				break;
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
			
			if (c.followId > 0) {
				c.getPA().resetFollow();
			}
			c.getPA().followNpc();
			if (c.timers.attackTimer <= 0) {
				c.getCombat().attackNpc(c.targeting.npcIndex);
			}	
			
			break;
			
			/**
			* Attack npc with magic
			**/
			case MAGE_NPC:
			if (!c.mageAllowed) {
				c.mageAllowed = true;
				c.sendMessage("I can't reach that.");
				break;
			}
			//c.usingSpecial = false;
			//c.getItems().updateSpecialBar();
			
			c.targeting.npcIndex = c.getInStream().readSignedWordBigEndianA();
			int castingSpellId = c.getInStream().readSignedWordA();
			c.attackMode.usingMagic = false;
			
			if(NPCHandler.npcs[c.targeting.npcIndex] == null ){
				break;
			}
			if (WorldAdventurer.isAdventurer(c.targeting.npcIndex)) {
				c.sendMessage("Max is too busy training.");
				c.targeting.npcIndex = 0;
				break;
			}
			
			if(NPCHandler.npcs[c.targeting.npcIndex].MaxHP == 0 || NPCHandler.npcs[c.targeting.npcIndex].npcType == 944){
				c.sendMessage("Nothing interesting happens.");
				break;
			}
			
			for(int i = 0; i < c.MAGIC_SPELLS.length; i++){
				if(castingSpellId == c.MAGIC_SPELLS[i][0]) {
					c.magic.spellId = i;
					c.attackMode.usingMagic = true;
					break;
				}
			}
			if(castingSpellId == 1171) { // crumble undead
				for (int npc : Config.UNDEAD_NPCS) {
					if(NPCHandler.npcs[c.targeting.npcIndex].npcType != npc) {
					 c.sendMessage("You can only attack undead monsters with this spell.");
					 c.attackMode.usingMagic = false;
					 c.stopMovement();
					 break;
					}
				}
			}
			/*if(!c.getCombat().checkMagicReqs(c.spellId)) {
				c.stopMovement();
				break;
			}*/
			
			if (c.attackMode.autocasting)
				c.attackMode.autocasting = false;

			if(c.attackMode.usingMagic) {
				if(c.goodDistance(c.getX(), c.getY(), NPCHandler.npcs[c.targeting.npcIndex].getX(), NPCHandler.npcs[c.targeting.npcIndex].getY(), 6)) {
					c.stopMovement();
				}
				if (c.timers.attackTimer <= 0) {
					c.getCombat().attackNpc(c.targeting.npcIndex);
				}
			}
	
			break;
			
			case FIRST_CLICK:
				c.npcInteraction.npcClickIndex = c.inStream.readSignedWordBigEndian();
				if (c.npcInteraction.npcClickIndex <= 0 || NPCHandler.npcs[c.npcInteraction.npcClickIndex] == null) {
					break;
				}
				c.npcInteraction.npcType = NPCHandler.npcs[c.npcInteraction.npcClickIndex].npcType;
				if(c.goodDistance(NPCHandler.npcs[c.npcInteraction.npcClickIndex].getX(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getY(), c.getX(), c.getY(), 1)) {
					c.turnPlayerTo(NPCHandler.npcs[c.npcInteraction.npcClickIndex].getX(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getY());
					NPCHandler.npcs[c.npcInteraction.npcClickIndex].facePlayer(c.playerId);
					c.faceUpdate(c.npcInteraction.npcClickIndex);
					c.getActions().firstClickNpc(c.npcInteraction.npcType);	
				} else {
					c.npcInteraction.clickNpcType = 1;
					c.followId2 = c.npcInteraction.npcClickIndex;
					c.faceUpdate(c.npcInteraction.npcClickIndex);
					c.getPA().followNpc();	
					CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer container) {
						if((c.npcInteraction.clickNpcType == 1) && NPCHandler.npcs[c.npcInteraction.npcClickIndex] != null) {			
							if(c.goodDistance(c.getX(), c.getY(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getX(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getY(), 1)) {
								c.turnPlayerTo(NPCHandler.npcs[c.npcInteraction.npcClickIndex].getX(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getY());
								NPCHandler.npcs[c.npcInteraction.npcClickIndex].facePlayer(c.playerId);
								c.faceUpdate(c.npcInteraction.npcClickIndex);
								c.followId2 = -1;
								c.getActions().firstClickNpc(c.npcInteraction.npcType);
								container.stop();
							}
						}
						if(c.npcInteraction.clickNpcType == 0 || c.npcInteraction.clickNpcType > 1) 
							container.stop();
					}
					@Override
					public void stop() {
						c.npcInteraction.clickNpcType = 0;
					}
				}, 1);
				}
				break;
			
			case SECOND_CLICK:
				c.npcInteraction.npcClickIndex = c.inStream.readUnsignedWordBigEndianA();
				c.npcInteraction.npcType = NPCHandler.npcs[c.npcInteraction.npcClickIndex].npcType;
				if(c.goodDistance(NPCHandler.npcs[c.npcInteraction.npcClickIndex].getX(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getY(), c.getX(), c.getY(), 1)) {
					c.turnPlayerTo(NPCHandler.npcs[c.npcInteraction.npcClickIndex].getX(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getY());
					NPCHandler.npcs[c.npcInteraction.npcClickIndex].facePlayer(c.playerId);
					c.faceUpdate(c.npcInteraction.npcClickIndex);
					c.getActions().secondClickNpc(c.npcInteraction.npcType);	
				} else {
					c.npcInteraction.clickNpcType = 2;
					c.followId2 = c.npcInteraction.npcClickIndex;
					c.faceUpdate(c.npcInteraction.npcClickIndex);
					c.getPA().followNpc();	
					CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer container) {
						if((c.npcInteraction.clickNpcType == 2) && NPCHandler.npcs[c.npcInteraction.npcClickIndex] != null) {			
							if(c.goodDistance(c.getX(), c.getY(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getX(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getY(), 1)) {
								c.turnPlayerTo(NPCHandler.npcs[c.npcInteraction.npcClickIndex].getX(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getY());
								NPCHandler.npcs[c.npcInteraction.npcClickIndex].facePlayer(c.playerId);
								c.faceUpdate(c.npcInteraction.npcClickIndex);
								c.followId2 = -1;
								c.getActions().secondClickNpc(c.npcInteraction.npcType);
								container.stop();
							}
						}
						if(c.npcInteraction.clickNpcType < 2 || c.npcInteraction.clickNpcType > 2) 
							container.stop();
					}
					@Override
					public void stop() {
						c.npcInteraction.clickNpcType = 0;
					}
				}, 1);
				}
				break;
			
			case THIRD_CLICK:
				c.npcInteraction.npcClickIndex = c.inStream.readSignedWord();
				c.npcInteraction.npcType = NPCHandler.npcs[c.npcInteraction.npcClickIndex].npcType;
				if(c.goodDistance(NPCHandler.npcs[c.npcInteraction.npcClickIndex].getX(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getY(), c.getX(), c.getY(), 1)) {
					c.turnPlayerTo(NPCHandler.npcs[c.npcInteraction.npcClickIndex].getX(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getY());
					NPCHandler.npcs[c.npcInteraction.npcClickIndex].facePlayer(c.playerId);
					c.faceUpdate(c.npcInteraction.npcClickIndex);
					c.getActions().thirdClickNpc(c.npcInteraction.npcType);	
				} else {
					c.npcInteraction.clickNpcType = 3;
					c.followId2 = c.npcInteraction.npcClickIndex;
					c.faceUpdate(c.npcInteraction.npcClickIndex);
					c.getPA().followNpc();	
					CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer container) {
						if((c.npcInteraction.clickNpcType == 3) && NPCHandler.npcs[c.npcInteraction.npcClickIndex] != null) {			
							if(c.goodDistance(c.getX(), c.getY(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getX(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getY(), 1)) {
								c.turnPlayerTo(NPCHandler.npcs[c.npcInteraction.npcClickIndex].getX(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getY());
								NPCHandler.npcs[c.npcInteraction.npcClickIndex].facePlayer(c.playerId);
								c.faceUpdate(c.npcInteraction.npcClickIndex);
								c.followId2 = -1;
								c.getActions().thirdClickNpc(c.npcInteraction.npcType);
								container.stop();
							}
						}
						if(c.npcInteraction.clickNpcType < 3) 
							container.stop();
					}
					@Override
					public void stop() {
						c.npcInteraction.clickNpcType = 0;
					}
				}, 1);
				}
				break;
				
				//castlewars
			case FOURTH_CLICK:
				c.npcInteraction.npcClickIndex = c.inStream.readSignedWord();
				c.npcInteraction.npcType = NPCHandler.npcs[c.npcInteraction.npcClickIndex].npcType;
				if(c.goodDistance(NPCHandler.npcs[c.npcInteraction.npcClickIndex].getX(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getY(), c.getX(), c.getY(), 1)) {
					c.turnPlayerTo(NPCHandler.npcs[c.npcInteraction.npcClickIndex].getX(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getY());
					UseItem.ItemonNpc(c, c.itemOnNpcItemId, c.itemOnNpcItemSlot, NPCHandler.npcs[c.npcInteraction.npcClickIndex].npcType);
				} else {
					c.npcInteraction.clickNpcType = 4;
					c.followId2 = c.npcInteraction.npcClickIndex;
					c.faceUpdate(c.npcInteraction.npcClickIndex);
					c.getPA().followNpc();	
					CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer container) {
						if((c.npcInteraction.clickNpcType == 4) && NPCHandler.npcs[c.npcInteraction.npcClickIndex] != null) {			
							if(c.goodDistance(c.getX(), c.getY(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getX(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getY(), 1)) {
								c.turnPlayerTo(NPCHandler.npcs[c.npcInteraction.npcClickIndex].getX(), NPCHandler.npcs[c.npcInteraction.npcClickIndex].getY());
								c.followId2 = -1;
								UseItem.ItemonNpc(c, c.itemOnNpcItemId, c.itemOnNpcItemSlot, NPCHandler.npcs[c.npcInteraction.npcClickIndex].npcType);
								container.stop();
							}
						}
						if(c.npcInteraction.clickNpcType < 4) 
							container.stop();
					}
					@Override
					public void stop() {
						c.npcInteraction.clickNpcType = 0;
					}
				}, 1);
				}
				break;
				
		}

	}
}
