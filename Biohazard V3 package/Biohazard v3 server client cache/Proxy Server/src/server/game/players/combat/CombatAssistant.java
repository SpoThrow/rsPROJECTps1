package server.game.players.combat;

import server.Config;
import server.Server;
import server.event.RestoreSpecialAttack;
import server.game.items.ItemAssistant;
import server.game.minigames.castlewars.CastleWars;
import server.game.npcs.KalphiteQueen;
import server.game.npcs.NPC;
import server.game.npcs.NPCHandler;
import server.game.npcs.Nex;
import server.game.players.Client;
import server.game.players.PathFinder;
import server.game.players.Player;
import server.game.players.PlayerHandler;
import core.util.Misc;

/**
 * CombatAssistant.java
 *
 * @author Sanity
 *
 */

public class CombatAssistant{

	private Client c;
	public CombatAssistant(Client Client) {
		this.c = Client;
	}
	
	public int strBonus;
	
	public int[][] slayerReqs = {{1648,5},{1612,15},{1643,45},{1618,50},{1624,65},{1610,75},{1613,80},{1615,85},{2783,90}};
	
	public boolean goodSlayer(int i) {
		for (int j = 0; j < slayerReqs.length; j++) {
			if (slayerReqs[j][0] == NPCHandler.npcs[i].npcType) {
				if (slayerReqs[j][1] > c.playerLevel[Player.playerSlayer]) {
					c.sendMessage("You need a slayer level of " + slayerReqs[j][1] + " to attack this monster.");
					return false;
				}
			}
		}
		return true;
	}
	
	public double getDrainRate() {
		double toRemove = 0.0;
		for(int i = 0; i < c.PRAYER_DRAIN_RATE.length; i++) {
			if(c.prayerActive[i]) { 
				toRemove += c.PRAYER_DRAIN_RATE[i];
			}
		}
		if (toRemove > 0) {
			toRemove /= (1 + (0.035 * c.playerBonus[11]));		
		}
		return toRemove;
	}

	public boolean hasPrayerActive() {
		for (int i = 0; i < 26; i++) {
			if(c.prayerActive[i])
				return true;
		}
		for (int i = 0; i < c.curseActive.length; i++) {
			if (c.curseActive[i])
				return true;
		}
		return false;
	}
	
	public int getPrayerDelay() {
		c.usingPrayer = false;
		int delay = 4000;	
		for(int i = 0; i < c.prayerActive.length; i++) {
			if(c.prayerActive[i] == true) {
				c.usingPrayer = true;
				delay -= c.PRAYER_DRAIN_RATE[i];
			}
		}
		delay += c.playerBonus[11]*500;
		return delay;
	}
	
	/**
	* Attack Npcs
	*/
	public void attackNpc(int i) {		
		if (NPCHandler.npcs[i] != null) {
			if(c.getTT().clueNpc(NPCHandler.npcs[i].npcType))
				return;
			strBonus = c.playerBonus[10];
			if(NPCHandler.npcs[i].spawnedBy != c.playerId && NPCHandler.npcs[i].spawnedBy > 0) {
				resetPlayerAttack();
				c.getDH().sendStatement("This NPC is not meant for you.");
				return;
			}
			if (NPCHandler.npcs[i].isDead || NPCHandler.npcs[i].MaxHP <= 0) {
				c.usingMagic = false;
				c.faceUpdate(0);
				c.npcIndex = 0;
				return;
			}			
			if(c.respawnTimer > 0) {
				c.npcIndex = 0;
				return;
			}
			if ((KalphiteQueen.KQnpc(i) && !KalphiteQueen.fullVerac(c)) || (KalphiteQueen.KQnpc(i) && c.usingMagic)) {
				resetPlayerAttack();
				c.sendMessage("Your attacks have no effect on the Queen.");
				return;
			}
			if (NPCHandler.npcs[i].underAttackBy > 0 && NPCHandler.npcs[i].underAttackBy != c.playerId && !NPCHandler.npcs[i].inMulti()) {
				c.npcIndex = 0;
				c.sendMessage("This monster is already in combat.");
				return;
			}
			if ((c.underAttackBy > 0 || c.underAttackBy2 > 0) && c.underAttackBy2 != i && !c.inMulti()) {
				resetPlayerAttack();
				c.sendMessage("I am already under attack.");
				return;
			}
			//castlewars
			if(NPCHandler.npcs[i].npcType == 1532
					&& NPCHandler.npcs[i].team == CastleWars.getTeamNumber(c)) {
				c.sendMessage("You can't attack your own team's barricade.");
				return;
			}
			if (!goodSlayer(i)) {
				resetPlayerAttack();
				return;
			}
			c.followId2 = i;
			c.followId = 0;
			if(c.attackTimer <= 0) {
				boolean usingBow = false;
				boolean usingArrows = false;
				boolean usingOtherRangeWeapons = false;
				boolean usingCross = c.playerEquipment[c.playerWeapon] == 9185;
				c.bonusAttack = 0;
				c.rangeItemUsed = 0;
				c.projectileStage = 0;
				if (c.autocasting) {
					c.spellId = c.autocastId;
					c.usingMagic = true;
				}
				if(c.spellId > 0) {
                    c.usingMagic = true;
                }
				c.specAccuracy = 1.0;
				c.specDamage = 1.0;
				if(!c.usingMagic) {
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
				}
				if (armaNpc(i) && !usingCross && !usingBow && !c.usingMagic && !usingCrystalBow() && !usingOtherRangeWeapons) {				
					resetPlayerAttack();
					c.sendMessage("You need to range attack this monster!");
					return;
				}
				// Size-aware range: stand next to any tile of large NPCs (Nex etc).
				int nSize = NPCHandler.npcs[i] != null ? Server.npcHandler.npcSize(i) : 1;
				if (nSize < 1) {
					nSize = 1;
				}
				int nx = NPCHandler.npcs[i].getX();
				int ny = NPCHandler.npcs[i].getY();
				boolean inMelee = withinNpcDistance(c.getX(), c.getY(), nx, ny, nSize, 1);
				boolean inHally = withinNpcDistance(c.getX(), c.getY(), nx, ny, nSize, 2);
				boolean inThrown = withinNpcDistance(c.getX(), c.getY(), nx, ny, nSize, 4);
				boolean inLong = withinNpcDistance(c.getX(), c.getY(), nx, ny, nSize, 8);
				boolean inRangeNow = (usingBow || c.usingMagic) ? inLong
						: (usingOtherRangeWeapons ? inThrown
						: (usingHally() ? inHally : inMelee));
				if (!inRangeNow) {
					// OSRS: do not start weapon cooldown until we actually swing.
					return;
				}
				if (!PathFinder.hasLineOfSight(c.absX, c.absY, 1, nx, ny, nSize, c.heightLevel)) {
					return;
				}
				c.attackTimer = getAttackDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
				
				if(!usingCross && !usingArrows && usingBow && (c.playerEquipment[c.playerWeapon] < 4212 || c.playerEquipment[c.playerWeapon] > 4223)) {
					c.sendMessage("You have run out of arrows!");
					c.stopMovement();
					c.freezeTimer = 1;
					c.npcIndex = 0;
					return;
				} 
				if(!correctBowAndArrows()/* < c.playerEquipment[c.playerArrows]*/ && Config.CORRECT_ARROWS && usingBow && !usingCrystalBow() && c.playerEquipment[c.playerWeapon] != 9185) {
					c.sendMessage("You can't use "+ItemAssistant.getItemName(c.playerEquipment[c.playerArrows]).toLowerCase()+"s with a "+ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase()+".");
					c.stopMovement();
					c.freezeTimer = 1;
					c.npcIndex = 0;
					return;
				}
				
				if ((c.playerEquipment[c.playerWeapon] == 4734 || c.playerEquipment[c.playerWeapon] == 4937 || c.playerEquipment[c.playerWeapon] == 4936 || c.playerEquipment[c.playerWeapon] == 4935) && c.playerEquipment[c.playerArrows] != 4740) {
					c.sendMessage("You must use bolt racks with a Karil's X-Bow.");
					c.stopMovement();
					c.freezeTimer = 1;
					c.npcIndex = 0;
					resetPlayerAttack();
					return;				
				}	
				
				
				if (c.playerEquipment[c.playerWeapon] == 9185 && !properBolts()) {
					c.sendMessage("You must use bolts with a crossbow.");
					c.stopMovement();
					c.freezeTimer = 1;
					resetPlayerAttack();
					return;				
				}
				
				if(usingBow || c.usingMagic || usingOtherRangeWeapons || (c.goodDistance(c.getX(), c.getY(), NPCHandler.npcs[i].getX(), NPCHandler.npcs[i].getY(), 2) && usingHally())) {
					c.stopMovement();
				}

				if(!checkMagicReqs(c.spellId)) {
					c.stopMovement();
					c.npcIndex = 0;
					return;
				}
				
				c.faceUpdate(i);
				//c.specAccuracy = 1.0;
				//c.specDamage = 1.0;
				NPCHandler.npcs[i].underAttackBy = c.playerId;
				NPCHandler.npcs[i].lastDamageTaken = System.currentTimeMillis();
				if(c.usingSpecial && !c.usingMagic) {
					if(checkSpecAmount(c.playerEquipment[c.playerWeapon])){
						c.lastWeaponUsed = c.playerEquipment[c.playerWeapon];
						c.lastArrowUsed = c.playerEquipment[c.playerArrows];
						activateSpecial(c.playerEquipment[c.playerWeapon], i);
						if(!c.isRestoringSpec){
							RestoreSpecialAttack.execute(c);
						}
						awardCombatXpOnSwingNpc(i);
						return;
					} else {
						c.sendMessage("You don't have the required special energy to use this attack.");
						c.usingSpecial = false;
						c.getItems().updateSpecialBar();
						//c.npcIndex = 0;
						//return;
					}
				}
				if(usingBow || c.usingMagic || usingOtherRangeWeapons) {
					c.mageFollow = true;
				} else {
					c.mageFollow = false;
				}
				c.specMaxHitIncrease = 0;
				if(!c.usingMagic) {
					c.getItems();
					c.startAnimation(getWepAnim(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase()));
				} else {
					c.startAnimation(c.MAGIC_SPELLS[c.spellId][2]);
				}
				c.lastWeaponUsed = c.playerEquipment[c.playerWeapon];
				c.lastArrowUsed = c.playerEquipment[c.playerArrows];
				if(!usingBow && !c.usingMagic && !usingOtherRangeWeapons) { // melee hit delay
					c.getItems();
					c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
					c.projectileStage = 0;
					c.oldNpcIndex = i;
				}
				
				if(usingBow && !usingOtherRangeWeapons && !c.usingMagic || usingCross) { // range hit delay					
					if (usingCross)
						c.usingBow = true;
					if (c.fightMode == 2)
						c.attackTimer--;
					c.lastArrowUsed = c.playerEquipment[c.playerArrows];
					c.lastWeaponUsed = c.playerEquipment[c.playerWeapon];
					c.gfx100(getRangeStartGFX());	
					c.getItems();
					c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
					c.projectileStage = 1;
					c.oldNpcIndex = i;
					if(c.playerEquipment[c.playerWeapon] >= 4212 && c.playerEquipment[c.playerWeapon] <= 4223) {
						c.rangeItemUsed = c.playerEquipment[c.playerWeapon];
						c.crystalBowArrowCount++;
						c.lastArrowUsed = 0;
					} else {
						c.rangeItemUsed = c.playerEquipment[c.playerArrows];
						c.getItems().deleteArrow();	
					}
					fireProjectileNpc();
				}
				
			if(usingBow && usingCross && c.usingMagic && usingOtherRangeWeapons) {
			c.getPA().followNpc();
			c.stopMovement();
			} else {
			c.followId = 0;
			c.followId2 = i;
			}
							
				
				if(usingOtherRangeWeapons && !c.usingMagic && !usingBow) {	// knives, darts, etc hit delay	
					c.lastWeaponUsed = c.playerEquipment[c.playerWeapon];
					c.rangeItemUsed = c.playerEquipment[c.playerWeapon];
					c.getItems().deleteEquipment();
					c.gfx100(getRangeStartGFX());
					c.lastArrowUsed = 0;
					c.getItems();
					c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
					c.projectileStage = 1;
					c.oldNpcIndex = i;
					if (c.fightMode == 2)
						c.attackTimer--;
					fireProjectileNpc();	
				}

				if(c.usingMagic) {	// magic hit delay
					int pX = c.getX();
					int pY = c.getY();
					int nX = NPCHandler.npcs[i].getX();
					int nY = NPCHandler.npcs[i].getY();
					int offX = (pY - nY)* -1;
					int offY = (pX - nX)* -1;
					c.castingMagic = true;
					c.projectileStage = 2;
					if(c.MAGIC_SPELLS[c.spellId][3] > 0) {
						if(getStartGfxHeight() == 100) {
							c.gfx100(c.MAGIC_SPELLS[c.spellId][3]);
						} else {
							c.gfx0(c.MAGIC_SPELLS[c.spellId][3]);
						}
					}
					if(c.MAGIC_SPELLS[c.spellId][4] > 0) {
						c.getPA().createPlayersProjectile(pX, pY, offX, offY, 50, 78, c.MAGIC_SPELLS[c.spellId][4], getStartHeight(), getEndHeight(), i + 1, 50);
					}
					c.getItems();
					c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
					c.oldNpcIndex = i;
					c.oldSpellId = c.spellId;
                    c.spellId = 0;
					// Queue freeze delay; applied on hit. Also seed freeze now if already accurate.
					if (getFreezeTime() > 0) {
						c.freezeDelay = getFreezeTime();
					}
					if (!c.autocasting)
						c.npcIndex = 0;
				}

				if(usingBow && Config.CRYSTAL_BOW_DEGRADES) { // crystal bow degrading
					if(c.playerEquipment[c.playerWeapon] == 4212) { // new crystal bow becomes full bow on the first shot
						c.getItems().wearItem(4214, 1, 3);
					}
					
					if(c.crystalBowArrowCount >= 250){
						switch(c.playerEquipment[c.playerWeapon]) {
							
							case 4223: // 1/10 bow
							c.getItems().wearItem(-1, 1, 3);
							c.sendMessage("Your crystal bow has fully degraded.");
							if(!c.getItems().addItem(4207, 1)) {
								Server.itemHandler.createGroundItem(c, 4207, c.getX(), c.getY(), 1, c.getId());
							}
							c.crystalBowArrowCount = 0;
							break;
							
							default:
							c.getItems().wearItem(++c.playerEquipment[c.playerWeapon], 1, 3);
							c.sendMessage("Your crystal bow degrades.");
							c.crystalBowArrowCount = 0;
							break;
							
						
						}
					}	
				}
				awardCombatXpOnSwingNpc(i);
			}
		}
	}
	

	private int remainingNpcHp(int i) {
		if (NPCHandler.npcs[i] == null) {
			return 0;
		}
		// Over-reserved pending hits (common at low HP / Wrath) were capping all
		// new damage to 0 — clamp pending so the kill can still finish.
		if (NPCHandler.npcs[i].pendingDamage > NPCHandler.npcs[i].HP) {
			NPCHandler.npcs[i].pendingDamage = NPCHandler.npcs[i].HP;
		}
		int hp = NPCHandler.npcs[i].HP - NPCHandler.npcs[i].pendingDamage;
		return hp < 0 ? 0 : hp;
	}

	private int remainingPlayerHp(int i) {
		if (PlayerHandler.players[i] == null) {
			return 0;
		}
		int hp = PlayerHandler.players[i].playerLevel[3] - PlayerHandler.players[i].pendingHitpoints;
		return hp < 0 ? 0 : hp;
	}

	private int capHit(int damage, int remaining) {
		if (damage < 0) {
			return 0;
		}
		if (damage > remaining) {
			return remaining;
		}
		return damage;
	}

	private void reserveNpcHit(int i, int damage) {
		if (damage > 0 && NPCHandler.npcs[i] != null) {
			NPCHandler.npcs[i].pendingDamage += damage;
		}
	}

	private void reservePlayerHit(int i, int damage) {
		if (damage > 0 && PlayerHandler.players[i] != null) {
			PlayerHandler.players[i].pendingHitpoints += damage;
		}
	}

	private void consumeNpcPending(int i, int damage) {
		if (NPCHandler.npcs[i] == null) {
			return;
		}
		NPCHandler.npcs[i].pendingDamage -= damage;
		if (NPCHandler.npcs[i].pendingDamage < 0) {
			NPCHandler.npcs[i].pendingDamage = 0;
		}
	}

	private void awardMeleeXp(int damage) {
		if (c.fightMode == 3) {
			c.getPA().addSkillXP((damage * Config.MELEE_EXP_RATE / 3), 0);
			c.getPA().addSkillXP((damage * Config.MELEE_EXP_RATE / 3), 1);
			c.getPA().addSkillXP((damage * Config.MELEE_EXP_RATE / 3), 2);
			c.getPA().addSkillXP((damage * Config.MELEE_EXP_RATE / 3), 3);
			c.getPA().refreshSkill(0);
			c.getPA().refreshSkill(1);
			c.getPA().refreshSkill(2);
			c.getPA().refreshSkill(3);
		} else {
			c.getPA().addSkillXP((damage * Config.MELEE_EXP_RATE), c.fightMode);
			c.getPA().addSkillXP((damage * Config.MELEE_EXP_RATE / 3), 3);
			c.getPA().refreshSkill(c.fightMode);
			c.getPA().refreshSkill(3);
		}
	}

	private void awardRangeXp(int damage) {
		if (c.fightMode == 3) {
			c.getPA().addSkillXP((damage * Config.RANGE_EXP_RATE / 3), 4);
			c.getPA().addSkillXP((damage * Config.RANGE_EXP_RATE / 3), 1);
			c.getPA().addSkillXP((damage * Config.RANGE_EXP_RATE / 3), 3);
			c.getPA().refreshSkill(1);
			c.getPA().refreshSkill(3);
			c.getPA().refreshSkill(4);
		} else {
			c.getPA().addSkillXP((damage * Config.RANGE_EXP_RATE), 4);
			c.getPA().addSkillXP((damage * Config.RANGE_EXP_RATE / 3), 3);
			c.getPA().refreshSkill(3);
			c.getPA().refreshSkill(4);
		}
	}

	private void awardMagicXp(int damage) {
		if (c.oldSpellId < 0 || c.oldSpellId >= c.MAGIC_SPELLS.length) {
			return;
		}
		c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage * Config.MAGIC_EXP_RATE), 6);
		c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage * Config.MAGIC_EXP_RATE / 3), 3);
		c.getPA().refreshSkill(3);
		c.getPA().refreshSkill(6);
	}

	private int rollNpcMeleeDamage(int i) {
		int damage = Misc.random(calculateMeleeMaxHit());
		boolean fullVeracsEffect = c.getPA().fullVeracs() && Misc.random(3) == 1;
		if (!fullVeracsEffect) {
			if (Misc.random(NPCHandler.npcs[i].defence) > 10 + Misc.random(calculateMeleeAttack())) {
				damage = 0;
			} else if (NPCHandler.npcs[i].npcType == 2882 || NPCHandler.npcs[i].npcType == 2883) {
				damage = 0;
			}
		}
		damage = Nex.modifyIncomingDamage(NPCHandler.npcs[i], damage, 0);
		return capHit(damage, remainingNpcHp(i));
	}

	private void rollNpcRangeDamage(int i) {
		int damage = Misc.random(rangeMaxHit());
		int damage2 = -1;
		if (c.lastWeaponUsed == 11235 || c.bowSpecShot == 1) {
			damage2 = Misc.random(rangeMaxHit());
		}
		boolean ignoreDef = false;
		if (Misc.random(5) == 1 && c.lastArrowUsed == 9243) {
			ignoreDef = true;
			NPCHandler.npcs[i].gfx0(758);
		}
		if (Misc.random(NPCHandler.npcs[i].defence) > Misc.random(10 + calculateRangeAttack()) && !ignoreDef) {
			damage = 0;
		} else if (NPCHandler.npcs[i].npcType == 2881 || NPCHandler.npcs[i].npcType == 2883 && !ignoreDef) {
			damage = 0;
		}
		if (Misc.random(4) == 1 && c.lastArrowUsed == 9242 && damage > 0) {
			NPCHandler.npcs[i].gfx0(754);
			damage = NPCHandler.npcs[i].HP / 5;
			c.handleHitMask(c.playerLevel[3] / 10);
			c.dealDamage(c.playerLevel[3] / 10);
			c.gfx0(754);
		}
		if (c.lastWeaponUsed == 11235 || c.bowSpecShot == 1) {
			if (Misc.random(NPCHandler.npcs[i].defence) > Misc.random(10 + calculateRangeAttack())) {
				damage2 = 0;
			}
		}
		if (c.dbowSpec) {
			NPCHandler.npcs[i].gfx100(1100);
			if (damage < 8) {
				damage = 8;
			}
			if (damage2 < 8) {
				damage2 = 8;
			}
			c.dbowSpec = false;
		}
		if (damage > 0 && Misc.random(5) == 1 && c.lastArrowUsed == 9244) {
			damage *= 1.45;
			NPCHandler.npcs[i].gfx0(756);
		}
		damage = Nex.modifyIncomingDamage(NPCHandler.npcs[i], damage, 1);
		if (damage2 > 0) {
			damage2 = Nex.modifyIncomingDamage(NPCHandler.npcs[i], damage2, 1);
		}
		damage = capHit(damage, remainingNpcHp(i));
		if (damage2 > 0) {
			damage2 = capHit(damage2, remainingNpcHp(i) - damage);
		}
		c.delayedDamage = damage;
		c.delayedDamage2 = damage2;
	}

	private void rollNpcMagicDamage(int i) {
		int damage = Misc.random(c.MAGIC_SPELLS[c.oldSpellId][6]);
		if (godSpells()) {
			if (System.currentTimeMillis() - c.godSpellDelay < Config.GOD_SPELL_CHARGE) {
				damage += Misc.random(10);
			}
		}
		boolean magicFailed = false;
		int bonusAttack = getBonusAttack(i);
		if (Misc.random(NPCHandler.npcs[i].defence) > 10 + Misc.random(mageAtk()) + bonusAttack) {
			damage = 0;
			magicFailed = true;
		} else if (NPCHandler.npcs[i].npcType == 2881 || NPCHandler.npcs[i].npcType == 2882) {
			damage = 0;
			magicFailed = true;
		}
		damage = Nex.modifyIncomingDamage(NPCHandler.npcs[i], damage, 2);
		damage = capHit(damage, remainingNpcHp(i));
		c.magicFailed = magicFailed;
		c.delayedDamage = damage;
	}

	public void awardCombatXpOnSwingNpc(int i) {
		if (c.hitDelay <= 0 || NPCHandler.npcs[i] == null || NPCHandler.npcs[i].isDead) {
			return;
		}
		c.swingXpAwarded = false;
		if (c.projectileStage == 0) {
			c.delayedDamage = rollNpcMeleeDamage(i);
			reserveNpcHit(i, c.delayedDamage);
			awardMeleeXp(c.delayedDamage);
			if (c.doubleHit) {
				c.delayedDamage2 = rollNpcMeleeDamage(i);
				reserveNpcHit(i, c.delayedDamage2);
				awardMeleeXp(c.delayedDamage2);
			}
			c.swingXpAwarded = true;
			return;
		}
		if (!c.castingMagic && c.projectileStage > 0) {
			rollNpcRangeDamage(i);
			reserveNpcHit(i, c.delayedDamage);
			if (c.delayedDamage2 > 0) {
				reserveNpcHit(i, c.delayedDamage2);
			}
			awardRangeXp(c.delayedDamage);
			c.swingXpAwarded = true;
			return;
		}
		if (c.projectileStage > 0) {
			rollNpcMagicDamage(i);
			reserveNpcHit(i, c.delayedDamage);
			awardMagicXp(c.delayedDamage);
			c.swingXpAwarded = true;
		}
	}

	private int rollPlayerMeleeDamage(int i, int stored) {
		Client o = (Client) PlayerHandler.players[i];
		int damage = stored;
		boolean veracsEffect = c.getPA().fullVeracs() && Misc.random(4) == 1;
		if (Misc.random(o.getCombat().calculateMeleeDefence()) > Misc.random(calculateMeleeAttack()) && !veracsEffect) {
			damage = 0;
			c.bonusAttack = 0;
		} else {
			c.bonusAttack += damage / 3;
		}
		if (protMelee(o) && !veracsEffect) {
			damage = damage * 60 / 100;
		}
		if (c.maxNextHit) {
			damage = calculateMeleeMaxHit();
		}
		return capHit(damage, remainingPlayerHp(i));
	}

	private void rollPlayerRangeDamage(int i) {
		Client o = (Client) PlayerHandler.players[i];
		int damage = Misc.random(rangeMaxHit());
		int damage2 = -1;
		if (c.lastWeaponUsed == 11235 || c.bowSpecShot == 1) {
			damage2 = Misc.random(rangeMaxHit());
		}
		boolean ignoreDef = false;
		if (Misc.random(4) == 1 && c.lastArrowUsed == 9243) {
			ignoreDef = true;
			o.gfx0(758);
		}
		if (Misc.random(10 + o.getCombat().calculateRangeDefence()) > Misc.random(10 + calculateRangeAttack()) && !ignoreDef) {
			damage = 0;
		}
		if (c.lastWeaponUsed == 11235 || c.bowSpecShot == 1) {
			if (Misc.random(10 + o.getCombat().calculateRangeDefence()) > Misc.random(10 + calculateRangeAttack())) {
				damage2 = 0;
			}
		}
		if (c.dbowSpec) {
			o.gfx100(1100);
			if (damage < 8) {
				damage = 8;
			}
			if (damage2 < 8) {
				damage2 = 8;
			}
			c.dbowSpec = false;
		}
		if (damage > 0 && Misc.random(5) == 1 && c.lastArrowUsed == 9244) {
			damage *= 1.45;
			o.gfx0(756);
		}
		if (protRange(o)) {
			damage = damage * 60 / 100;
			if (c.lastWeaponUsed == 11235 || c.bowSpecShot == 1) {
				damage2 = damage2 * 60 / 100;
			}
		}
		if (Misc.random(4) == 1 && c.lastArrowUsed == 9242 && damage > 0) {
			o.gfx0(754);
			damage = remainingPlayerHp(i) / 5;
			c.handleHitMask(c.playerLevel[3] / 10);
			c.dealDamage(c.playerLevel[3] / 10);
			c.gfx0(754);
		}
		damage = capHit(damage, remainingPlayerHp(i));
		if (damage2 > 0) {
			damage2 = capHit(damage2, remainingPlayerHp(i) - damage);
		}
		c.delayedDamage = damage;
		c.delayedDamage2 = damage2;
	}

	private void rollPlayerMagicDamage(int i) {
		Client o = (Client) PlayerHandler.players[i];
		int damage = Misc.random(c.MAGIC_SPELLS[c.oldSpellId][6]);
		if (godSpells()) {
			if (System.currentTimeMillis() - c.godSpellDelay < Config.GOD_SPELL_CHARGE) {
				damage += 10;
			}
		}
		if (c.magicFailed) {
			damage = 0;
		}
		if (protMage(o)) {
			damage = damage * 60 / 100;
		}
		c.delayedDamage = capHit(damage, remainingPlayerHp(i));
	}

	public void awardCombatXpOnSwingPlayer(int i) {
		if (c.hitDelay <= 0 || PlayerHandler.players[i] == null || PlayerHandler.players[i].isDead) {
			return;
		}
		c.swingXpAwarded = false;
		if (c.projectileStage == 0) {
			c.delayedDamage = rollPlayerMeleeDamage(i, c.delayedDamage);
			reservePlayerHit(i, c.delayedDamage);
			awardMeleeXp(c.delayedDamage);
			if (c.doubleHit) {
				c.delayedDamage2 = rollPlayerMeleeDamage(i, c.delayedDamage2);
				if (c.ssSpec) {
					c.delayedDamage2 = capHit(5 + Misc.random(11), remainingPlayerHp(i));
					c.ssSpec = false;
				}
				reservePlayerHit(i, c.delayedDamage2);
				awardMeleeXp(c.delayedDamage2);
			}
			c.swingXpAwarded = true;
			return;
		}
		if (!c.castingMagic && c.projectileStage > 0) {
			rollPlayerRangeDamage(i);
			reservePlayerHit(i, c.delayedDamage);
			if (c.delayedDamage2 > 0) {
				reservePlayerHit(i, c.delayedDamage2);
			}
			awardRangeXp(c.delayedDamage);
			c.swingXpAwarded = true;
			return;
		}
		if (c.projectileStage > 0) {
			rollPlayerMagicDamage(i);
			reservePlayerHit(i, c.delayedDamage);
			awardMagicXp(c.delayedDamage);
			c.swingXpAwarded = true;
		}
	}

	public void delayedHit(int i) { // npc hit delay
		if (NPCHandler.npcs[i] != null) {
			if (NPCHandler.npcs[i].isDead) {
				NPCHandler.npcs[i].pendingDamage = 0;
				c.swingXpAwarded = false;
				c.npcIndex = 0;
				return;
			}
			NPCHandler.npcs[i].facePlayer(c.playerId);
			if(NPCHandler.npcs[i].attackTimer <= 3 || NPCHandler.npcs[i].attackTimer == 0 && !c.castingMagic) { // block animation
				NPCHandler.startAnimation(NPCHandler.getBlockEmote(i), i);
			}
			
			if (NPCHandler.npcs[i].underAttackBy > 0 && Server.npcHandler.getsPulled(i)) {
				NPCHandler.npcs[i].killerId = c.playerId;			
			} else if (NPCHandler.npcs[i].underAttackBy < 0 && !Server.npcHandler.getsPulled(i)) {
				NPCHandler.npcs[i].killerId = c.playerId;
			}
			c.lastNpcAttacked = i;
			if(c.projectileStage == 0) { // melee hit damage
				applyNpcMeleeDamage(i, 1);
				if(c.doubleHit) {
					applyNpcMeleeDamage(i, 2);
				}
				c.isUsingSpecial = false;
			}

			if(!c.castingMagic && c.projectileStage > 0) { // range hit damage
				int damage;
				int damage2 = -1;
				if (c.swingXpAwarded) {
					damage = c.delayedDamage;
					damage2 = c.delayedDamage2;
				} else {
				damage = Misc.random(rangeMaxHit());
				if (c.lastWeaponUsed == 11235 || c.bowSpecShot == 1)
					damage2 = Misc.random(rangeMaxHit());
				boolean ignoreDef = false;
				if (Misc.random(5) == 1 && c.lastArrowUsed == 9243) {
					ignoreDef = true;
					NPCHandler.npcs[i].gfx0(758);
				}

				
				if(Misc.random(NPCHandler.npcs[i].defence) > Misc.random(10+calculateRangeAttack()) && !ignoreDef) {
					damage = 0;
				} else if (NPCHandler.npcs[i].npcType == 2881 || NPCHandler.npcs[i].npcType == 2883 && !ignoreDef) {
					damage = 0;
				}
				
				if (Misc.random(4) == 1 && c.lastArrowUsed == 9242 && damage > 0) {
					NPCHandler.npcs[i].gfx0(754);
					damage = NPCHandler.npcs[i].HP/5;
					c.handleHitMask(c.playerLevel[3]/10);
					c.dealDamage(c.playerLevel[3]/10);
					c.gfx0(754);					
				}
				
				if (c.lastWeaponUsed == 11235 || c.bowSpecShot == 1) {
					if (Misc.random(NPCHandler.npcs[i].defence) > Misc.random(10+calculateRangeAttack()))
						damage2 = 0;
				}
				if (c.dbowSpec) {
					NPCHandler.npcs[i].gfx100(1100);
					if (damage < 8)
						damage = 8;
					if (damage2 < 8)
						damage2 = 8;
					c.dbowSpec = false;
					
				}
				if (damage > 0 && Misc.random(5) == 1 && c.lastArrowUsed == 9244) {
					damage *= 1.45;
					NPCHandler.npcs[i].gfx0(756);
				}
				
				if (NPCHandler.npcs[i].HP - damage < 0) { 
					damage = NPCHandler.npcs[i].HP;
				}
				if (NPCHandler.npcs[i].HP - damage <= 0 && damage2 > 0) {
					damage2 = 0;
				}
				}
				if (NPCHandler.npcs[i].HP - damage < 0) { 
					damage = NPCHandler.npcs[i].HP;
				}
				if (damage2 > 0 && NPCHandler.npcs[i].HP - damage - damage2 < 0) {
					damage2 = NPCHandler.npcs[i].HP - damage;
				}
				if(!c.swingXpAwarded) {
				if(c.fightMode == 3) {
					
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE/3), 4); 
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE/3), 1);				
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE/3), 3);
					c.getPA().refreshSkill(1);
					c.getPA().refreshSkill(3);
					c.getPA().refreshSkill(4);
					
				} else {
					
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE), 4); 
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE/3), 3);
					c.getPA().refreshSkill(3);
					c.getPA().refreshSkill(4);
					
				}
				}
				if (damage > 0) {
					if (NPCHandler.npcs[i].npcType >= 6142 && NPCHandler.npcs[i].npcType <= 6145) {
						c.pcDamage += damage;					
					}				
				}
				boolean dropArrows = true;
						
				for(int noArrowId : c.NO_ARROW_DROP) {
					if(c.lastWeaponUsed == noArrowId) {
						dropArrows = false;
						break;
					}
				}
				if(dropArrows) {
					c.getItems().dropArrowNpc();	
				}
				NPCHandler.npcs[i].underAttack = true;
				NPCHandler.npcs[i].hitDiff = damage;
				NPCHandler.npcs[i].HP -= damage;
				consumeNpcPending(i, damage);
				if (damage2 > -1) {
					NPCHandler.npcs[i].hitDiff2 = damage2;
					NPCHandler.npcs[i].HP -= damage2;
					consumeNpcPending(i, damage2);
					c.totalDamageDealt += damage2;	
				}
				if (c.killingNpcIndex != c.oldNpcIndex) {
					c.totalDamageDealt = 0;				
				}
				c.killingNpcIndex = c.oldNpcIndex;
				c.totalDamageDealt += damage;
				NPCHandler.npcs[i].hitUpdateRequired = true;
				if (damage2 > -1)
					NPCHandler.npcs[i].hitUpdateRequired2 = true;
				NPCHandler.npcs[i].updateRequired = true;
				if (damage > 0) {
					c.getCurse().applyHitEffects(damage, null);
				}
				if (damage2 > 0) {
					c.getCurse().applyHitEffects(damage2, null);
				}

			} else if (c.projectileStage > 0) { // magic hit damage
				int damage;
				boolean magicFailed;
				if (c.swingXpAwarded) {
					damage = c.delayedDamage;
					magicFailed = c.magicFailed;
				} else {
				damage = Misc.random(c.MAGIC_SPELLS[c.oldSpellId][6]);
				if(godSpells()) {
					if(System.currentTimeMillis() - c.godSpellDelay < Config.GOD_SPELL_CHARGE) {
						damage += Misc.random(10);
					}
				}
				magicFailed = false;
				//c.npcIndex = 0;
				int bonusAttack = getBonusAttack(i);
				if (Misc.random(NPCHandler.npcs[i].defence) > 10+ Misc.random(mageAtk()) + bonusAttack) {
					damage = 0;
					magicFailed = true;
				} else if (NPCHandler.npcs[i].npcType == 2881 || NPCHandler.npcs[i].npcType == 2882) {
					damage = 0;
					magicFailed = true;
				}
				
				if (NPCHandler.npcs[i].HP - damage < 0) { 
					damage = NPCHandler.npcs[i].HP;
				}
				
				c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage*Config.MAGIC_EXP_RATE), 6); 
				c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage*Config.MAGIC_EXP_RATE/3), 3);
				c.getPA().refreshSkill(3);
				c.getPA().refreshSkill(6);
				}
				if (NPCHandler.npcs[i].HP - damage < 0) { 
					damage = NPCHandler.npcs[i].HP;
				}
				
				if (damage > 0) {
					if (NPCHandler.npcs[i].npcType >= 6142 && NPCHandler.npcs[i].npcType <= 6145) {
						c.pcDamage += damage;					
					}				
				}
				if(getEndGfxHeight() == 100 && !magicFailed){ // end GFX
					NPCHandler.npcs[i].gfx100(c.MAGIC_SPELLS[c.oldSpellId][5]);
				} else if (!magicFailed){
					NPCHandler.npcs[i].gfx0(c.MAGIC_SPELLS[c.oldSpellId][5]);
				}
				
				if(magicFailed) {	
					NPCHandler.npcs[i].gfx100(85);
				}			
				if(!magicFailed) {
					applyNpcFreeze(i);
					if (c.freezeDelay > 0 && NPCHandler.npcs[i] != null
							&& NPCHandler.npcs[i].freezeTimer < c.freezeDelay) {
						NPCHandler.npcs[i].freezeTimer = c.freezeDelay;
						NPCHandler.npcs[i].moveX = 0;
						NPCHandler.npcs[i].moveY = 0;
						NPCHandler.npcs[i].direction = -1;
					}
					c.freezeDelay = 0;
					switch(c.MAGIC_SPELLS[c.oldSpellId][0]) { 
						case 12901:
						case 12919: // blood spells
						case 12911:
						case 12929:
						int heal = Misc.random(damage / 2);
						if(c.playerLevel[3] + heal >= c.getPA().getLevelForXP(c.playerXP[3])) {
							c.playerLevel[3] = c.getPA().getLevelForXP(c.playerXP[3]);
						} else {
							c.playerLevel[3] += heal;
						}
						c.getPA().refreshSkill(3);
						break;
					}

				}
				NPCHandler.npcs[i].underAttack = true;
				if(c.MAGIC_SPELLS[c.oldSpellId][6] != 0) {
					NPCHandler.npcs[i].hitDiff = damage;
					NPCHandler.npcs[i].HP -= damage;
					consumeNpcPending(i, damage);
					NPCHandler.npcs[i].hitUpdateRequired = true;
					c.totalDamageDealt += damage;
				}
				c.killingNpcIndex = c.oldNpcIndex;			
				NPCHandler.npcs[i].updateRequired = true;
				if (damage > 0) {
					c.getCurse().applyHitEffects(damage, null);
				}
				if (c.inMulti() && multis()) {
					int primaryX = NPCHandler.npcs[i].getX();
					int primaryY = NPCHandler.npcs[i].getY();
					c.barrageCount = 0;
					for (int j = 0; j < NPCHandler.maxNPCs; j++) {
						if (j == i || NPCHandler.npcs[j] == null) {
							continue;
						}
						if (c.barrageCount >= 9) {
							break;
						}
						if (checkMultiBarrageNpcReqs(j, primaryX, primaryY)) {
							appendMultiBarrageNpc(j);
						}
					}
				}
				c.usingMagic = false;
				c.castingMagic = false;
				c.oldSpellId = 0;
			}
		}
	
		if(c.bowSpecShot <= 0) {
			c.oldNpcIndex = 0;
			c.projectileStage = 0;
			c.doubleHit = false;
			c.lastWeaponUsed = 0;
			c.bowSpecShot = 0;
		}
		if(c.bowSpecShot >= 2) {
			c.bowSpecShot = 0;
			//c.attackTimer = getAttackDelay(c.getItems().getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
		}
		if(c.bowSpecShot == 1) {
			fireProjectileNpc();
			c.hitDelay = 2;
			c.bowSpecShot = 0;
		}
		c.swingXpAwarded = false;
	}
	
	
	public void applyNpcMeleeDamage(int i, int damageMask) {
		int damage;
		if (c.swingXpAwarded) {
			damage = damageMask == 1 ? c.delayedDamage : c.delayedDamage2;
		} else {
		damage = Misc.random(calculateMeleeMaxHit());
		boolean fullVeracsEffect = c.getPA().fullVeracs() && Misc.random(3) == 1;
		if (NPCHandler.npcs[i].HP - damage < 0) { 
			damage = NPCHandler.npcs[i].HP;
		}
		
		if (!fullVeracsEffect) {
			if (Misc.random(NPCHandler.npcs[i].defence) > 10 + Misc.random(calculateMeleeAttack())) {
				damage = 0;
			} else if (NPCHandler.npcs[i].npcType == 2882 || NPCHandler.npcs[i].npcType == 2883) {
				damage = 0;
			}
		}	
		}
		if (NPCHandler.npcs[i].HP - damage < 0) { 
			damage = NPCHandler.npcs[i].HP;
		}
		// delayedDamage from rollNpcMeleeDamage is already prayer-modified
		if (!c.swingXpAwarded) {
			damage = Nex.modifyIncomingDamage(NPCHandler.npcs[i], damage, 0);
			if (NPCHandler.npcs[i].HP - damage < 0) {
				damage = NPCHandler.npcs[i].HP;
			}
		}
		boolean guthansEffect = false;
		if (c.getPA().fullGuthans()) {
			if (Misc.random(3) == 1) {
				guthansEffect = true;			
			}		
		}
		if(!c.swingXpAwarded) {
		if(c.fightMode == 3) {
			
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 0); 
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 1);
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 2); 				
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 3);
			c.getPA().refreshSkill(0);
			c.getPA().refreshSkill(1);
			c.getPA().refreshSkill(2);
			c.getPA().refreshSkill(3);
			
		} else {
			
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE), c.fightMode); 
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 3);
			c.getPA().refreshSkill(c.fightMode);
			c.getPA().refreshSkill(3);
			
		}
		}
		if (damage > 0) {
			if (NPCHandler.npcs[i].npcType >= 6142 && NPCHandler.npcs[i].npcType <= 6145) {
				c.pcDamage += damage;					
			}				
		}
		if (damage > 0 && guthansEffect) {
			c.playerLevel[3] += damage;
			if (c.playerLevel[3] > c.getLevelForXP(c.playerXP[3]))
				c.playerLevel[3] = c.getLevelForXP(c.playerXP[3]);
			c.getPA().refreshSkill(3);
			NPCHandler.npcs[i].gfx0(398);		
		}
		NPCHandler.npcs[i].underAttack = true;
		//Server.npcHandler.npcs[i].killerId = c.playerId;
		c.killingNpcIndex = c.npcIndex;
		c.lastNpcAttacked = i;
		switch (c.specEffect) {
			case 4:
				int heal = (damage/2);
				if (damage > 0) {
					if (c.playerLevel[3] + heal > c.getLevelForXP(c.playerXP[3]))
						if (c.playerLevel[3] > c.getLevelForXP(c.playerXP[3]));
						else 
						c.playerLevel[3] = c.getLevelForXP(c.playerXP[3]);
					else 
						c.playerLevel[3] += heal;
					c.getPA().refreshSkill(3);
				}
				c.specEffect = 0;
			break;
		
		}
		switch(damageMask) {
			case 1:
			NPCHandler.npcs[i].hitDiff = damage;
			NPCHandler.npcs[i].HP -= damage;
			consumeNpcPending(i, damage);
			c.totalDamageDealt += damage;
			NPCHandler.npcs[i].hitUpdateRequired = true;	
			NPCHandler.npcs[i].updateRequired = true;
			break;
		
			case 2:
			NPCHandler.npcs[i].hitDiff2 = damage;
			NPCHandler.npcs[i].HP -= damage;
			consumeNpcPending(i, damage);
			c.totalDamageDealt += damage;
			NPCHandler.npcs[i].hitUpdateRequired2 = true;	
			NPCHandler.npcs[i].updateRequired = true;
			c.doubleHit = false;
			break;
			
		}
		if (damage > 0) {
			c.getCurse().applyHitEffects(damage, null);
		}
	}
	
	public void fireProjectileNpc() {
		if(c.oldNpcIndex > 0) {
			if(NPCHandler.npcs[c.oldNpcIndex] != null) {
				c.projectileStage = 2;
				int pX = c.getX();
				int pY = c.getY();
				int nX = NPCHandler.npcs[c.oldNpcIndex].getX();
				int nY = NPCHandler.npcs[c.oldNpcIndex].getY();
				int offX = (pY - nY)* -1;
				int offY = (pX - nX)* -1;
				c.getPA().createPlayersProjectile(pX, pY, offX, offY, 50, getProjectileSpeed(), getRangeProjectileGFX(), 43, 31, c.oldNpcIndex + 1, getStartDelay());
				if (usingDbow())
					c.getPA().createPlayersProjectile2(pX, pY, offX, offY, 50, getProjectileSpeed(), getRangeProjectileGFX(), 60, 31,  c.oldNpcIndex + 1, getStartDelay(), 35);
			}
		}
	}
	

	
	/**
	* Attack Players, same as npc tbh xD
	**/
	
		public void attackPlayer(int i) {

		if (PlayerHandler.players[i] != null) {
			strBonus = c.playerBonus[10];
			
			if (PlayerHandler.players[i] != null) {
				//castlewars
				if (CastleWars.isInCw((Client) PlayerHandler.players[i]) && CastleWars.isInCw(c)) {
					if (CastleWars.getTeamNumber(c) == CastleWars.getTeamNumber((Client) PlayerHandler.players[i])) {
						c.sendMessage("You cannot attack your own teammate.");
						resetPlayerAttack();
						return;
					}
				}
				//castlewars
				if (!CastleWars.isInCw((Client) PlayerHandler.players[i]) && CastleWars.isInCw(c)) {
					c.sendMessage("You cannot attack people outside castle wars.");
					resetPlayerAttack();
					 return;
				}
				
				if (PlayerHandler.players[i].isDead) {
					resetPlayerAttack();
					return;
				}
				
				if(c.respawnTimer > 0 || PlayerHandler.players[i].respawnTimer > 0) {
					resetPlayerAttack();
					return;
				}
				
				if(!c.getCombat().checkReqs()) {
					return;
				}
			
			boolean sameSpot = c.absX == PlayerHandler.players[i].getX() && c.absY == PlayerHandler.players[i].getY();
			if(!c.goodDistance(PlayerHandler.players[i].getX(), PlayerHandler.players[i].getY(), c.getX(), c.getY(), 25) && !sameSpot) {
				resetPlayerAttack();
				return;
			}

			if(PlayerHandler.players[i].respawnTimer > 0) {
				PlayerHandler.players[i].playerIndex = 0;
				resetPlayerAttack();
				return;
			}
			
			if (PlayerHandler.players[i].heightLevel != c.heightLevel) {
				resetPlayerAttack();
				return;
			}
			
			c.followId = i;
			c.followId2 = 0;
			if(c.attackTimer <= 0) {
				c.usingBow = false;
				c.specEffect = 0;
				c.usingRangeWeapon = false;
				c.rangeItemUsed = 0;
				boolean usingBow = false;
				boolean usingArrows = false;
				boolean usingOtherRangeWeapons = false;
				boolean usingCross = c.playerEquipment[c.playerWeapon] == 9185;
				c.projectileStage = 0;
				if (c.autocasting) {
					c.spellId = c.autocastId;
					c.usingMagic = true;
				}
				if(c.spellId > 0) {
                    c.usingMagic = true;
                }
				if (c.absX == PlayerHandler.players[i].absX && c.absY == PlayerHandler.players[i].absY) {
					if (c.freezeTimer > 0) {
						resetPlayerAttack();
						return;
					}	
					c.followId = i;
					c.attackTimer = 0;
					return;
				}
				
				if(!c.usingMagic) {
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
				}
				if (c.autocasting) {
					c.spellId = c.autocastId;
					c.usingMagic = true;
				}
				//c.sendMessage("Made it here2.");
				if(c.spellId > 0) {
                    c.usingMagic = true;
                }
				c.getItems();
				c.attackTimer = getAttackDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());

				if(c.duelRule[9]){
				boolean canUseWeapon = false;
					for(int funWeapon: Config.FUN_WEAPONS) {
						if(c.playerEquipment[c.playerWeapon] == funWeapon) {
							canUseWeapon = true;
						}
					}
					if(!canUseWeapon) {
						c.sendMessage("You can only use fun weapons in this duel!");
						resetPlayerAttack();
						return;
					}
				}
				//c.sendMessage("Made it here3.");
				if(c.duelRule[2] && (usingBow || usingOtherRangeWeapons)) {
					c.sendMessage("Range has been disabled in this duel!");
					return;
				}
				if(c.duelRule[3] && (!usingBow && !usingOtherRangeWeapons && !c.usingMagic)) {
					c.sendMessage("Melee has been disabled in this duel!");
					return;
				}
				
				if(c.duelRule[4] && c.usingMagic) {
					c.sendMessage("Magic has been disabled in this duel!");
					resetPlayerAttack();
					return;
				}
				
				if((!c.goodDistance(c.getX(), c.getY(), PlayerHandler.players[i].getX(), PlayerHandler.players[i].getY(), 4) && (usingOtherRangeWeapons && !usingBow && !c.usingMagic)) 
				|| (!c.goodDistance(c.getX(), c.getY(), PlayerHandler.players[i].getX(), PlayerHandler.players[i].getY(), 2) && (!usingOtherRangeWeapons && usingHally() && !usingBow && !c.usingMagic))
				|| (!c.goodDistance(c.getX(), c.getY(), PlayerHandler.players[i].getX(), PlayerHandler.players[i].getY(), getRequiredDistance()) && (!usingOtherRangeWeapons && !usingHally() && !usingBow && !c.usingMagic)) 
				|| (!c.goodDistance(c.getX(), c.getY(), PlayerHandler.players[i].getX(), PlayerHandler.players[i].getY(), 10) && (usingBow || c.usingMagic))) {
					//c.sendMessage("Setting attack timer to 1");
					c.attackTimer = 1;
					if (!usingBow && !c.usingMagic && !usingOtherRangeWeapons && c.freezeTimer > 0)
						resetPlayerAttack();
					return;
				}
				Client o = (Client)PlayerHandler.players[i];
				if(!PathFinder.hasLineOfSight(c.absX, c.absY, 1, o.absX, o.absY, 1, c.heightLevel)) {
					if((c.usingBow || c.usingMagic || usingOtherRangeWeapons || c.autocasting))
						PathFinder.getPathFinder().findRoute(c, o.absX, o.absY, true, 8, 8);
					if(!c.usingBow && !c.usingMagic && !usingOtherRangeWeapons && !c.autocasting)
						PathFinder.getPathFinder().findRoute(c, o.absX, o.absY, true, 1, 1);
					c.attackTimer = 0;
					return;
				}
				
				if(!usingCross && !usingArrows && usingBow && (c.playerEquipment[c.playerWeapon] < 4212 || c.playerEquipment[c.playerWeapon] > 4223) && !c.usingMagic) {
					c.sendMessage("You have run out of arrows!");
					c.stopMovement();
					resetPlayerAttack();
					return;
				}
				if(!correctBowAndArrows()/* < c.playerEquipment[c.playerArrows]*/ && Config.CORRECT_ARROWS && usingBow && !usingCrystalBow() && c.playerEquipment[c.playerWeapon] != 9185 && !c.usingMagic) {
					c.sendMessage("You can't use "+ItemAssistant.getItemName(c.playerEquipment[c.playerArrows]).toLowerCase()+"s with a "+ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase()+".");
					c.stopMovement();
					resetPlayerAttack();
					return;
				}
				if (c.playerEquipment[c.playerWeapon] == 9185 && !properBolts() && !c.usingMagic) {
					c.sendMessage("You must use bolts with a crossbow.");
					c.stopMovement();
					resetPlayerAttack();
					return;				
				}
				
				
				if(usingBow || c.usingMagic || usingOtherRangeWeapons || usingHally()) {
					c.stopMovement();
				}
				
				if(usingBow || c.usingMagic || usingOtherRangeWeapons || (c.goodDistance(c.getX(), c.getY(), NPCHandler.npcs[i].getX(), NPCHandler.npcs[i].getY(), 2) && usingHally())) {
					c.stopMovement();
				}

				if(!checkMagicReqs(c.spellId)) {
					c.stopMovement();
					c.npcIndex = 0;
					return;
				}
				
				if(!checkMagicReqs(c.spellId)) {
					c.stopMovement();
					resetPlayerAttack();
					return;
				}
				
				c.faceUpdate(i+32768);
				
				if(c.duelStatus != 5 && !c.inBH) {
					if(!c.attackedPlayers.contains(c.playerIndex) && !PlayerHandler.players[c.playerIndex].attackedPlayers.contains(c.playerId)) {
						c.attackedPlayers.add(c.playerIndex);
						c.isSkulled = true;
						c.skullTimer = Config.SKULL_TIMER;
						c.headIconPk = 0;
						c.getPA().requestUpdates();
					} 
				}
				c.specAccuracy = 1.0;
				c.specDamage = 1.0;
				c.delayedDamage = c.delayedDamage2 = 0;
				if(c.usingSpecial && !c.usingMagic) {
					if(c.duelRule[10] && c.duelStatus == 5) {
						c.sendMessage("Special attacks have been disabled during this duel!");
						c.usingSpecial = false;
						c.getItems().updateSpecialBar();
						resetPlayerAttack();
						return;
					}
					if(usingBow || c.usingMagic || usingOtherRangeWeapons) {
						c.mageFollow = true;
					} else {
						c.mageFollow = false;
					}
					if(checkSpecAmount(c.playerEquipment[c.playerWeapon])){
						c.lastArrowUsed = c.playerEquipment[c.playerArrows];
						activateSpecial(c.playerEquipment[c.playerWeapon], i);
						c.followId = c.playerIndex;
						if(!c.isRestoringSpec){
							RestoreSpecialAttack.execute(c);
						}
						awardCombatXpOnSwingPlayer(i);
						return;
					} else {
						c.sendMessage("You don't have the required special energy to use this attack.");
						c.usingSpecial = false;
						c.getItems().updateSpecialBar();
						//c.playerIndex = 0;
						//return;
					}	
				}
				
				if(!c.usingMagic) {
					c.getItems();
					c.startAnimation(getWepAnim(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase()));
					c.mageFollow = false;
				} else {
					c.startAnimation(c.MAGIC_SPELLS[c.spellId][2]);
					c.mageFollow = true;
					c.followId = c.playerIndex;
				}
				PlayerHandler.players[i].underAttackBy = c.playerId;
				PlayerHandler.players[i].logoutDelay = System.currentTimeMillis();
				PlayerHandler.players[i].singleCombatDelay = System.currentTimeMillis();
				PlayerHandler.players[i].killerId = c.playerId;
				c.lastArrowUsed = 0;
				c.rangeItemUsed = 0;
				if(!usingBow && !c.usingMagic && !usingOtherRangeWeapons) { // melee hit delay
					c.followId = PlayerHandler.players[c.playerIndex].playerId;
					c.getPA().followPlayer();
					c.getItems();
					c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
					c.delayedDamage = Misc.random(calculateMeleeMaxHit());
					c.projectileStage = 0;
					c.oldPlayerIndex = i;
				}
								
				if(usingBow && !usingOtherRangeWeapons && !c.usingMagic || usingCross) { // range hit delay
					if(c.playerEquipment[c.playerWeapon] >= 4212 && c.playerEquipment[c.playerWeapon] <= 4223) {
						c.rangeItemUsed = c.playerEquipment[c.playerWeapon];
						c.crystalBowArrowCount++;
					} else {
						c.rangeItemUsed = c.playerEquipment[c.playerArrows];
						c.getItems().deleteArrow();
					}
					if (c.fightMode == 2)
						c.attackTimer--;
					if (usingCross)
						c.usingBow = true;
					c.usingBow = true;
					c.followId = PlayerHandler.players[c.playerIndex].playerId;
					c.getPA().followPlayer();
					c.lastWeaponUsed = c.playerEquipment[c.playerWeapon];
					c.lastArrowUsed = c.playerEquipment[c.playerArrows];
					c.gfx100(getRangeStartGFX());	
					c.getItems();
					c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
					c.projectileStage = 1;
					c.oldPlayerIndex = i;
					fireProjectilePlayer();
				}
											
				if(usingOtherRangeWeapons) {	// knives, darts, etc hit delay
					c.rangeItemUsed = c.playerEquipment[c.playerWeapon];
					c.getItems().deleteEquipment();
					c.usingRangeWeapon = true;
					c.followId = PlayerHandler.players[c.playerIndex].playerId;
					c.getPA().followPlayer();
					c.gfx100(getRangeStartGFX());
					if (c.fightMode == 2)
						c.attackTimer--;
					c.getItems();
					c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
					c.projectileStage = 1;
					c.oldPlayerIndex = i;
					fireProjectilePlayer();
				}

				if(c.usingMagic) {	// magic hit delay
					int pX = c.getX();
					int pY = c.getY();
					int nX = PlayerHandler.players[i].getX();
					int nY = PlayerHandler.players[i].getY();
					int offX = (pY - nY)* -1;
					int offY = (pX - nX)* -1;
					c.castingMagic = true;
					c.projectileStage = 2;
					if(c.MAGIC_SPELLS[c.spellId][3] > 0) {
						if(getStartGfxHeight() == 100) {
							c.gfx100(c.MAGIC_SPELLS[c.spellId][3]);
						} else {
							c.gfx0(c.MAGIC_SPELLS[c.spellId][3]);
						}
					}
					if(c.MAGIC_SPELLS[c.spellId][4] > 0) {
						c.getPA().createPlayersProjectile(pX, pY, offX, offY, 50, 78, c.MAGIC_SPELLS[c.spellId][4], getStartHeight(), getEndHeight(), -i - 1, getStartDelay());
					}
					if (c.autocastId > 0) {
						c.followId = c.playerIndex;
						c.followDistance = 5;
					}
					c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
					c.oldPlayerIndex = i;
					c.oldSpellId = c.spellId;
                    c.spellId = 0;
					if(c.MAGIC_SPELLS[c.oldSpellId][0] == 12891 && o.isMoving) {
						//c.sendMessage("Barrage projectile..");
						c.getPA().createPlayersProjectile(pX, pY, offX, offY, 50, 85, 368, 25, 25, -i - 1, getStartDelay());
					}
					if(Misc.random(mageAtk()) > Misc.random(o.getCombat().mageDef())) {
						c.magicFailed = false;
					} else if(Misc.random(mageAtk()) < Misc.random(o.getCombat().mageDef())) {
						c.magicFailed = true;
					}
					int freezeDelay = getFreezeTime();//freeze time
					if(freezeDelay > 0 && PlayerHandler.players[i].freezeTimer <= -3 && !c.magicFailed) { 
						PlayerHandler.players[i].freezeTimer = freezeDelay;
						o.resetWalkingQueue();
						o.sendMessage("You have been frozen.");
						o.frozenBy = c.playerId;
					}
					if (!c.autocasting && c.spellId <= 0)
						c.playerIndex = 0;
				}

				if(usingBow && Config.CRYSTAL_BOW_DEGRADES) { // crystal bow degrading
					if(c.playerEquipment[c.playerWeapon] == 4212) { // new crystal bow becomes full bow on the first shot
						c.getItems().wearItem(4214, 1, 3);
					}
					
					if(c.crystalBowArrowCount >= 250){
						switch(c.playerEquipment[c.playerWeapon]) {
							
							case 4223: // 1/10 bow
							c.getItems().wearItem(-1, 1, 3);
							c.sendMessage("Your crystal bow has fully degraded.");
							if(!c.getItems().addItem(4207, 1)) {
								Server.itemHandler.createGroundItem(c, 4207, c.getX(), c.getY(), 1, c.getId());
							}
							c.crystalBowArrowCount = 0;
							break;
							
							default:
							c.getItems().wearItem(++c.playerEquipment[c.playerWeapon], 1, 3);
							c.sendMessage("Your crystal bow degrades.");
							c.crystalBowArrowCount = 0;
							break;
						}
					}	
				}
				awardCombatXpOnSwingPlayer(i);
			}
		}
	}
		}
	
	public boolean usingCrystalBow() {
		return c.playerEquipment[c.playerWeapon] >= 4212 && c.playerEquipment[c.playerWeapon] <= 4223;	
	}
	
	public void appendVengeance(int otherPlayer, int damage) {
		if (damage <= 0)
			return;
		Player o = PlayerHandler.players[otherPlayer];
		o.forcedText = "Taste Vengeance!";
		o.forcedChatUpdateRequired = true;
		o.updateRequired = true;
		o.vengOn = false;
		if ((o.playerLevel[3] - damage) > 0) {
			damage = (int)(damage * 0.75);
			if (damage > c.playerLevel[3]) {
				damage = c.playerLevel[3];
			}
			c.setHitDiff2(damage);
			c.setHitUpdateRequired2(true);
			c.playerLevel[3] -= damage;
			c.getPA().refreshSkill(3);
		}	
		c.updateRequired = true;
	}
	
	public void playerDelayedHit(int i) {
		if (PlayerHandler.players[i] != null) {
			if (PlayerHandler.players[i].isDead || c.isDead || PlayerHandler.players[i].playerLevel[3] <= 0 || c.playerLevel[3] <= 0) {
				if (c.swingXpAwarded) {
					PlayerHandler.players[i].pendingHitpoints = 0;
					c.swingXpAwarded = false;
				}
				c.playerIndex = 0;
				return;
			}
			if (PlayerHandler.players[i].respawnTimer > 0) {
				c.faceUpdate(0);
				c.playerIndex = 0;
				return;
			}
			Client o = (Client) PlayerHandler.players[i];
			o.getPA().removeAllWindows();
			if (o.playerIndex <= 0 && o.npcIndex <= 0) {
				if (o.autoRet == 1) {
					o.playerIndex = c.playerId;
				}	
			}
			if(o.attackTimer <= 3 || o.attackTimer == 0 && o.playerIndex == 0 && !c.castingMagic) { // block animation
				o.startAnimation(o.getCombat().getBlockEmote());
			}
			if(o.inTrade) {
				o.getTradeAndDuel().declineTrade();
			}
			if(c.projectileStage == 0) { // melee hit damage								
				applyPlayerMeleeDamage(i, 1);
				if(c.doubleHit) {
					applyPlayerMeleeDamage(i, 2);
				}	
				c.isUsingSpecial = false;
			}
			
			if(!c.castingMagic && c.projectileStage > 0) { // range hit damage
				int damage;
				int damage2 = -1;
				if (c.swingXpAwarded) {
					damage = c.delayedDamage;
					damage2 = c.delayedDamage2;
				} else {
				damage = Misc.random(rangeMaxHit());
				if (c.lastWeaponUsed == 11235 || c.bowSpecShot == 1)
					damage2 = Misc.random(rangeMaxHit());
				boolean ignoreDef = false;
				if (Misc.random(4) == 1 && c.lastArrowUsed == 9243) {
					ignoreDef = true;
					o.gfx0(758);
				}					
				if(Misc.random(10+o.getCombat().calculateRangeDefence()) > Misc.random(10+calculateRangeAttack()) && !ignoreDef) {
					damage = 0;
				}
				if (Misc.random(4) == 1 && c.lastArrowUsed == 9242 && damage > 0) {
					PlayerHandler.players[i].gfx0(754);
					damage = NPCHandler.npcs[i].HP/5;
					c.handleHitMask(c.playerLevel[3]/10);
					c.dealDamage(c.playerLevel[3]/10);
					c.gfx0(754);
				}
				
				if (c.lastWeaponUsed == 11235 || c.bowSpecShot == 1) {
					if (Misc.random(10+o.getCombat().calculateRangeDefence()) > Misc.random(10+calculateRangeAttack()))
						damage2 = 0;
				}
				
				if (c.dbowSpec) {
					o.gfx100(1100);
					if (damage < 8)
						damage = 8;
					if (damage2 < 8)
						damage2 = 8;
					c.dbowSpec = false;
				}
				if (damage > 0 && Misc.random(5) == 1 && c.lastArrowUsed == 9244) {
					damage *= 1.45;
					o.gfx0(756);
				}
				if(protRange(o)) { // if prayer active reduce damage by half 
					damage = (int)damage * 60 / 100;
					if (c.lastWeaponUsed == 11235 || c.bowSpecShot == 1)
						damage2 = (int)damage2 * 60 / 100;
				}
				}
				if(c.playerEquipment[c.playerWeapon] == 700 && o.poisonDamage <= 0 && Misc.random(3) == 0)
					o.getPA().appendPoison(o, 4);
				if (ItemAssistant.getItemName(c.playerEquipment[c.playerArrows]).contains(("(p)")) && o.poisonDamage <= 0 && Misc.random(20) == 1)
					o.getPA().appendPoison(o, 5);
				if (ItemAssistant.getItemName(c.playerEquipment[c.playerArrows]).contains(("(p+)")) && o.poisonDamage <= 0 && Misc.random(10) == 1)
					o.getPA().appendPoison(o, 9);
				if ((ItemAssistant.getItemName(c.playerEquipment[c.playerArrows]).contains(("(p++)"))) && o.poisonDamage <= 0 && Misc.random(4) == 1)
					o.getPA().appendPoison(o, 13);
				if (PlayerHandler.players[i].playerLevel[3] - damage < 0) { 
					damage = PlayerHandler.players[i].playerLevel[3];
				}
				if (PlayerHandler.players[i].playerLevel[3] - damage - damage2 < 0) { 
					damage2 = PlayerHandler.players[i].playerLevel[3] - damage;
				}
				if (damage < 0)
					damage = 0;
				if (damage2 < 0 && damage2 != -1)
					damage2 = 0;
				if (o.vengOn) {
					appendVengeance(i, damage);
					appendVengeance(i, damage2);
				}
				if (damage > 0)
					applyRecoil(damage, i);
				if (damage2 > 0)
					applyRecoil(damage2, i);
				if(!c.swingXpAwarded) {
				if(c.fightMode == 3) {
					
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE/3), 4); 
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE/3), 1);				
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE/3), 3);
					c.getPA().refreshSkill(1);
					c.getPA().refreshSkill(3);
					c.getPA().refreshSkill(4);
					
				} else {
					
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE), 4); 
					c.getPA().addSkillXP((damage*Config.RANGE_EXP_RATE/3), 3);
					c.getPA().refreshSkill(3);
					c.getPA().refreshSkill(4);
					
				}
				}
				boolean dropArrows = true;
						
				for(int noArrowId : c.NO_ARROW_DROP) {
					if(c.lastWeaponUsed == noArrowId) {
						dropArrows = false;
						break;
					}
				}
				if(dropArrows) {
					c.getItems().dropArrowPlayer();	
				}
				PlayerHandler.players[i].underAttackBy = c.playerId;
				PlayerHandler.players[i].logoutDelay = System.currentTimeMillis();
				PlayerHandler.players[i].singleCombatDelay = System.currentTimeMillis();
				PlayerHandler.players[i].killerId = c.playerId;
				//Server.playerHandler.players[i].setHitDiff(damage);
				//Server.playerHandler.players[i].playerLevel[3] -= damage;
				PlayerHandler.players[i].dealDamage(damage);
				PlayerHandler.players[i].damageTaken[c.playerId] += damage;
				c.killedBy = PlayerHandler.players[i].playerId;
				PlayerHandler.players[i].handleHitMask(damage);
				if (damage2 != -1) {
					//Server.playerHandler.players[i].playerLevel[3] -= damage2;
					PlayerHandler.players[i].dealDamage(damage2);
					PlayerHandler.players[i].damageTaken[c.playerId] += damage2;
					PlayerHandler.players[i].handleHitMask(damage2);
				
				}
				o.getPA().refreshSkill(3);
					
				//Server.playerHandler.players[i].setHitUpdateRequired(true);	
				PlayerHandler.players[i].updateRequired = true;
				applySmite(i, damage);
				if (damage2 != -1)
					applySmite(i, damage2);
				if (damage > 0) {
					c.getCurse().applyHitEffects(damage, o);
				}
				if (damage2 > 0) {
					c.getCurse().applyHitEffects(damage2, o);
				}
			
			} else if (c.projectileStage > 0) { // magic hit damage
				int damage;
				if (c.swingXpAwarded) {
					damage = c.delayedDamage;
				} else {
				damage = Misc.random(c.MAGIC_SPELLS[c.oldSpellId][6]);
				if(godSpells()) {
					if(System.currentTimeMillis() - c.godSpellDelay < Config.GOD_SPELL_CHARGE) {
						damage += 10;
					}
				}
				//c.playerIndex = 0;
				if (c.magicFailed)
					damage = 0;
					
				if(protMage(o)) { // if prayer active reduce damage by half 
					damage = (int)damage * 60 / 100;
				}
				}
				if (PlayerHandler.players[i].playerLevel[3] - damage < 0) {
					damage = PlayerHandler.players[i].playerLevel[3];
				}
				if (o.vengOn)
					appendVengeance(i, damage);
				if (damage > 0)
					applyRecoil(damage, i);
				
				if (!c.swingXpAwarded) {
				c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage*Config.MAGIC_EXP_RATE), 6); 
				c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage*Config.MAGIC_EXP_RATE/3), 3);
				c.getPA().refreshSkill(3);
				c.getPA().refreshSkill(6);
				}
				
				
				if(getEndGfxHeight() == 100 && !c.magicFailed){ // end GFX
					PlayerHandler.players[i].gfx100(c.MAGIC_SPELLS[c.oldSpellId][5]);
				} else if (!c.magicFailed){
					PlayerHandler.players[i].gfx0(c.MAGIC_SPELLS[c.oldSpellId][5]);
				} else if(c.magicFailed) {	
					PlayerHandler.players[i].gfx100(85);
				}
				
				if(!c.magicFailed) {
					if(System.currentTimeMillis() - PlayerHandler.players[i].reduceStat > 35000) {
						PlayerHandler.players[i].reduceStat = System.currentTimeMillis();
						switch(c.MAGIC_SPELLS[c.oldSpellId][0]) { 
							case 12987:
							case 13011:
							case 12999:
							case 13023:
							PlayerHandler.players[i].playerLevel[0] -= ((o.getPA().getLevelForXP(PlayerHandler.players[i].playerXP[0]) * 10) / 100);
							break;
						}
					}
					
					switch(c.MAGIC_SPELLS[c.oldSpellId][0]) { 	
						case 12445: //teleblock
						if (System.currentTimeMillis() - o.teleBlockDelay > o.teleBlockLength) {
							o.teleBlockDelay = System.currentTimeMillis();
							o.sendMessage("You have been teleblocked.");
							if (protMage(o))
								o.teleBlockLength = 150000;
							else
								o.teleBlockLength = 300000;
						}		
						break;
						
						case 12901:
						case 12919: // blood spells
						case 12911:
						case 12929:
						int heal = (int)(damage / 4);
						if(c.playerLevel[3] + heal > c.getPA().getLevelForXP(c.playerXP[3])) {
							c.playerLevel[3] = c.getPA().getLevelForXP(c.playerXP[3]);
						} else {
							c.playerLevel[3] += heal;
						}
						c.getPA().refreshSkill(3);
						break;
						
						case 1153:						
						PlayerHandler.players[i].playerLevel[0] -= ((o.getPA().getLevelForXP(PlayerHandler.players[i].playerXP[0]) * 5) / 100);
						o.sendMessage("Your attack level has been reduced!");
						PlayerHandler.players[i].reduceSpellDelay[c.reduceSpellId] = System.currentTimeMillis();
						o.getPA().refreshSkill(0);
						break;
						
						case 1157:
						PlayerHandler.players[i].playerLevel[2] -= ((o.getPA().getLevelForXP(PlayerHandler.players[i].playerXP[2]) * 5) / 100);
						o.sendMessage("Your strength level has been reduced!");
						PlayerHandler.players[i].reduceSpellDelay[c.reduceSpellId] = System.currentTimeMillis();						
						o.getPA().refreshSkill(2);
						break;
						
						case 1161:
						PlayerHandler.players[i].playerLevel[1] -= ((o.getPA().getLevelForXP(PlayerHandler.players[i].playerXP[1]) * 5) / 100);
						o.sendMessage("Your defence level has been reduced!");
						PlayerHandler.players[i].reduceSpellDelay[c.reduceSpellId] = System.currentTimeMillis();					
						o.getPA().refreshSkill(1);
						break;
						
						case 1542:
						PlayerHandler.players[i].playerLevel[1] -= ((o.getPA().getLevelForXP(PlayerHandler.players[i].playerXP[1]) * 10) / 100);
						o.sendMessage("Your defence level has been reduced!");
						PlayerHandler.players[i].reduceSpellDelay[c.reduceSpellId] =  System.currentTimeMillis();
						o.getPA().refreshSkill(1);
						break;
						
						case 1543:
						PlayerHandler.players[i].playerLevel[2] -= ((o.getPA().getLevelForXP(PlayerHandler.players[i].playerXP[2]) * 10) / 100);
						o.sendMessage("Your strength level has been reduced!");
						PlayerHandler.players[i].reduceSpellDelay[c.reduceSpellId] = System.currentTimeMillis();
						o.getPA().refreshSkill(2);
						break;
						
						case 1562:					
						PlayerHandler.players[i].playerLevel[0] -= ((o.getPA().getLevelForXP(PlayerHandler.players[i].playerXP[0]) * 10) / 100);
						o.sendMessage("Your attack level has been reduced!");
						PlayerHandler.players[i].reduceSpellDelay[c.reduceSpellId] = System.currentTimeMillis();					
						o.getPA().refreshSkill(0);
						break;
					}					
				}
				
				PlayerHandler.players[i].logoutDelay = System.currentTimeMillis();
				PlayerHandler.players[i].underAttackBy = c.playerId;
				PlayerHandler.players[i].killerId = c.playerId;
				PlayerHandler.players[i].singleCombatDelay = System.currentTimeMillis();
				if(c.MAGIC_SPELLS[c.oldSpellId][6] != 0) {
					//Server.playerHandler.players[i].playerLevel[3] -= damage;
					PlayerHandler.players[i].dealDamage(damage);
					PlayerHandler.players[i].damageTaken[c.playerId] += damage;
					c.totalPlayerDamageDealt += damage;
					if (!c.magicFailed) {
						//Server.playerHandler.players[i].setHitDiff(damage);
						//Server.playerHandler.players[i].setHitUpdateRequired(true);
						PlayerHandler.players[i].handleHitMask(damage);
					}
				}
				applySmite(i, damage);
				if (damage > 0) {
					c.getCurse().applyHitEffects(damage, o);
				}
				c.killedBy = PlayerHandler.players[i].playerId;	
				o.getPA().refreshSkill(3);
				PlayerHandler.players[i].updateRequired = true;
				c.usingMagic = false;
				c.castingMagic = false;
				if (o.inMulti() && multis()) {
					c.barrageCount = 0;
					for (int j = 0; j < PlayerHandler.players.length; j++) {
						if (PlayerHandler.players[j] != null) {
							if (j == o.playerId)
								continue;
							if (c.barrageCount >= 9)
								break;
							if (o.goodDistance(o.getX(), o.getY(), PlayerHandler.players[j].getX(), PlayerHandler.players[j].getY(), 1))
								appendMultiBarrage(j, c.magicFailed);
						}	
					}
				}
				c.getPA().refreshSkill(3);
				c.getPA().refreshSkill(6);
				c.oldSpellId = 0;
			}
		}	
		c.getPA().requestUpdates();
		@SuppressWarnings("unused")
		int oldindex = c.oldPlayerIndex;
		if(c.bowSpecShot <= 0) {
			c.oldPlayerIndex = 0;	
			c.projectileStage = 0;
			c.lastWeaponUsed = 0;
			c.doubleHit = false;
			c.bowSpecShot = 0;
		}
		if(c.bowSpecShot != 0) {
			c.bowSpecShot = 0;
		}
		c.swingXpAwarded = false;
	}
	

	/**
	 * Tiles to hold at while this weapon is on cooldown. Uses what is equipped,
	 * because usingBow is only set on the tick an arrow actually fires.
	 */
	public int attackRange() {
		if (c.usingMagic || c.mageFollow || c.autocasting || c.autocastId > 0) {
			return 8;
		}
		int weapon = c.playerEquipment[c.playerWeapon];
		if (weapon == 9185 || usingCrystalBow()) {
			return 8;
		}
		for (int bowId : c.BOWS) {
			if (weapon == bowId) {
				return 8;
			}
		}
		String name = ItemAssistant.getItemName(weapon);
		if (name != null) {
			String lower = name.toLowerCase();
			if (lower.contains("javelin") || lower.contains("dart")
					|| lower.contains("thrownaxe") || lower.contains("knife")) {
				return 4;
			}
		}
		if (usingHally()) {
			return 2;
		}
		return 1;
	}

	/** Chebyshev distance to closest tile of an NPC footprint (size×size). */
	public boolean withinNpcDistance(int px, int py, int nx, int ny, int size, int distance) {
		if (size < 1) {
			size = 1;
		}
		int closestX = px;
		if (px < nx) {
			closestX = nx;
		} else if (px > nx + size - 1) {
			closestX = nx + size - 1;
		}
		int closestY = py;
		if (py < ny) {
			closestY = ny;
		} else if (py > ny + size - 1) {
			closestY = ny + size - 1;
		}
		int dx = Math.abs(px - closestX);
		int dy = Math.abs(py - closestY);
		return dx <= distance && dy <= distance;
	}

	public boolean multis() {
		switch (c.MAGIC_SPELLS[c.oldSpellId][0]) {
			case 12891:
			case 12881:
			case 13011:
			case 13023:
			case 12919: // blood spells
			case 12929:
			case 12963:
			case 12975:
			return true;
		}
		return false;
	
	}
	/*public void appendMultiBarrage(int playerId, boolean splashed) {
		if (Server.playerHandler.players[playerId] != null) {
			Client c2 = (Client)Server.playerHandler.players[playerId];
			if (c2.isDead || c2.respawnTimer > 0)
				return;
			if (checkMultiBarrageReqs(playerId)) {
				c.barrageCount++;
				if (Misc.random(mageAtk()) > Misc.random(mageDef()) && !c.magicFailed) {
					if(getEndGfxHeight() == 100){ // end GFX
						c2.gfx100(c.MAGIC_SPELLS[c.oldSpellId][5]);
					} else {
						c2.gfx0(c.MAGIC_SPELLS[c.oldSpellId][5]);
					}
					int damage = Misc.random(c.MAGIC_SPELLS[c.oldSpellId][6]);
					if (c2.prayerActive[12]) {
						damage *= (int)(.60);
					}
					if (c2.playerLevel[3] - damage < 0) {
						damage = c2.playerLevel[3];					
					}
					c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage*Config.MAGIC_EXP_RATE), 6); 
					c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage*Config.MAGIC_EXP_RATE/3), 3);
					//Server.playerHandler.players[playerId].setHitDiff(damage);
					//Server.playerHandler.players[playerId].setHitUpdateRequired(true);
					Server.playerHandler.players[playerId].handleHitMask(damage);
					//Server.playerHandler.players[playerId].playerLevel[3] -= damage;
					Server.playerHandler.players[playerId].dealDamage(damage);
					Server.playerHandler.players[playerId].damageTaken[c.playerId] += damage;
					c2.getPA().refreshSkill(3);
					c.totalPlayerDamageDealt += damage;
					multiSpellEffect(playerId, damage);
				} else {
					c2.gfx100(85);
				}			
			}		
		}	
	}*/
	
	public void appendMultiBarrage(int playerId, boolean splashed) {
		if (PlayerHandler.players[playerId] != null) {
			Client c2 = (Client)PlayerHandler.players[playerId];
			if (c2.isDead || c2.respawnTimer > 0)
				return;
			if (checkMultiBarrageReqs(playerId)) {
				c.barrageCount++;
				if (Misc.random(mageAtk()) > Misc.random(mageDef()) && !c.magicFailed) {
					if(getEndGfxHeight() == 100){ // end GFX
						c2.gfx100(c.MAGIC_SPELLS[c.oldSpellId][5]);
					} else {
						c2.gfx0(c.MAGIC_SPELLS[c.oldSpellId][5]);
					}
					int damage = Misc.random(c.MAGIC_SPELLS[c.oldSpellId][6]);
					if (c2.prayerActive[12] || c2.curseActive[7]) {
						damage *= (int)(.60);
					}
					if (c2.playerLevel[3] - damage < 0) {
						damage = c2.playerLevel[3];					
					}
					
					c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage*Config.MAGIC_EXP_RATE), 6); 
					c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage*Config.MAGIC_EXP_RATE/3), 3);
					//Server.playerHandler.players[playerId].setHitDiff(damage);
					//Server.playerHandler.players[playerId].setHitUpdateRequired(true);
					PlayerHandler.players[playerId].handleHitMask(damage);
					//Server.playerHandler.players[playerId].playerLevel[3] -= damage;
					PlayerHandler.players[playerId].dealDamage(damage);
					PlayerHandler.players[playerId].damageTaken[c.playerId] += damage;
					c2.getPA().refreshSkill(3);
					
					c.totalPlayerDamageDealt += damage;
					multiSpellEffect(playerId, damage);
				} else {
					c2.gfx100(85);
				}			
			}		
		}	
	}
	
	public void multiSpellEffect(int playerId, int damage) {					
		switch(c.MAGIC_SPELLS[c.oldSpellId][0]) {
			case 13011:
			case 13023:
			if(System.currentTimeMillis() - PlayerHandler.players[playerId].reduceStat > 35000) {
				PlayerHandler.players[playerId].reduceStat = System.currentTimeMillis();
				PlayerHandler.players[playerId].playerLevel[0] -= ((PlayerHandler.players[playerId].getLevelForXP(PlayerHandler.players[playerId].playerXP[0]) * 10) / 100);
			}	
			break;
			case 12919: // blood spells
			case 12929:
				int heal = (int)(damage / 4);
				if(c.playerLevel[3] + heal >= c.getPA().getLevelForXP(c.playerXP[3])) {
					c.playerLevel[3] = c.getPA().getLevelForXP(c.playerXP[3]);
				} else {
					c.playerLevel[3] += heal;
				}
				c.getPA().refreshSkill(3);
			break;
			case 12891:
			case 12881:
				if (PlayerHandler.players[playerId].freezeTimer < -4) {
					PlayerHandler.players[playerId].freezeTimer = getFreezeTime();
					PlayerHandler.players[playerId].stopMovement();
				}
			break;
		}	
	}

	public boolean checkMultiBarrageNpcReqs(int npcIndex, int primaryX, int primaryY) {
		if (NPCHandler.npcs[npcIndex] == null) {
			return false;
		}
		NPC n = NPCHandler.npcs[npcIndex];
		if (n.isDead || n.HP <= 0) {
			return false;
		}
		if (n.heightLevel != c.heightLevel) {
			return false;
		}
		return c.goodDistance(primaryX, primaryY, n.getX(), n.getY(), 1);
	}

	public void appendMultiBarrageNpc(int npcIndex) {
		if (NPCHandler.npcs[npcIndex] == null || c.oldSpellId <= 0) {
			return;
		}
		NPC n = NPCHandler.npcs[npcIndex];
		if (n.isDead || n.HP <= 0) {
			return;
		}
		c.barrageCount++;
		int damage = Misc.random(c.MAGIC_SPELLS[c.oldSpellId][6]);
		if (godSpells()) {
			if (System.currentTimeMillis() - c.godSpellDelay < Config.GOD_SPELL_CHARGE) {
				damage += Misc.random(10);
			}
		}
		boolean magicFailed = false;
		int bonusAttack = getBonusAttack(npcIndex);
		if (Misc.random(n.defence) > 10 + Misc.random(mageAtk()) + bonusAttack) {
			magicFailed = true;
		} else if (n.npcType == 2881 || n.npcType == 2882) {
			magicFailed = true;
		}
		if (!magicFailed) {
			damage = Nex.modifyIncomingDamage(n, damage, 2);
		}
		if (n.HP - damage < 0) {
			damage = n.HP;
		}
		if (magicFailed) {
			n.gfx100(85);
			n.updateRequired = true;
			return;
		}
		if (getEndGfxHeight() == 100) {
			n.gfx100(c.MAGIC_SPELLS[c.oldSpellId][5]);
		} else {
			n.gfx0(c.MAGIC_SPELLS[c.oldSpellId][5]);
		}
		c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage * Config.MAGIC_EXP_RATE), 6);
		c.getPA().addSkillXP((c.MAGIC_SPELLS[c.oldSpellId][7] + damage * Config.MAGIC_EXP_RATE / 3), 3);
		c.getPA().refreshSkill(3);
		c.getPA().refreshSkill(6);
		applyNpcFreeze(npcIndex);
		multiSpellEffectNpc(npcIndex, damage);
		if (c.MAGIC_SPELLS[c.oldSpellId][6] != 0) {
			n.hitDiff = damage;
			n.HP -= damage;
			consumeNpcPending(npcIndex, damage);
			n.hitUpdateRequired = true;
			c.totalDamageDealt += damage;
		}
		n.underAttack = true;
		n.updateRequired = true;
		if (damage > 0) {
			c.getCurse().applyHitEffects(damage, null);
		}
	}

	public void multiSpellEffectNpc(int npcIndex, int damage) {
		switch (c.MAGIC_SPELLS[c.oldSpellId][0]) {
		case 12919:
		case 12929:
		case 12901:
		case 12911: {
			int heal = Misc.random(damage / 2);
			if (c.playerLevel[3] + heal >= c.getPA().getLevelForXP(c.playerXP[3])) {
				c.playerLevel[3] = c.getPA().getLevelForXP(c.playerXP[3]);
			} else {
				c.playerLevel[3] += heal;
			}
			c.getPA().refreshSkill(3);
			break;
		}
		case 12891:
		case 12881:
		case 12871:
		case 12861:
			applyNpcFreeze(npcIndex);
			break;
		default:
			break;
		}
	}
	
	public void applyPlayerMeleeDamage(int i, int damageMask){
		Client o = (Client) PlayerHandler.players[i];
		if(o == null) {
			return;
		}
		int damage = 0;
		boolean veracsEffect = false;
		boolean guthansEffect = false;
		if (c.getPA().fullVeracs()) {
			if (Misc.random(4) == 1) {
				veracsEffect = true;				
			}		
		}
		if (c.getPA().fullGuthans()) {
			if (Misc.random(4) == 1) {
				guthansEffect = true;
			}		
		}
		if (damageMask == 1) {
			damage = c.delayedDamage;
			c.delayedDamage = 0;
		} else {
			damage = c.delayedDamage2;
			c.delayedDamage2 = 0;
		}
		if (!c.swingXpAwarded) {
		if(Misc.random(o.getCombat().calculateMeleeDefence()) > Misc.random(calculateMeleeAttack()) && !veracsEffect) {
			damage = 0;
			c.bonusAttack = 0;
		}
		}
		if (ItemAssistant.getItemName(c.playerEquipment[c.playerArrows]).contains(("(p)")) && o.poisonDamage <= 0 && Misc.random(20) == 1) {
			o.getPA().appendPoison(o, 5);
			c.bonusAttack += damage/6;
		} else {
			c.bonusAttack += damage/6;
		}
		if (ItemAssistant.getItemName(c.playerEquipment[c.playerArrows]).contains(("(p+)")) && o.poisonDamage <= 0 && Misc.random(10) == 1) {
			o.getPA().appendPoison(o, 9);
			c.bonusAttack += damage/4;
		} else {
			c.bonusAttack += damage/4;
		}
		if ((ItemAssistant.getItemName(c.playerEquipment[c.playerArrows]).contains(("(p++)"))) && o.poisonDamage <= 0 && Misc.random(4) == 1) {
			o.getPA().appendPoison(o, 13);
			c.bonusAttack += damage/3;
		} else {
			c.bonusAttack += damage/3;
		}
		if (!c.swingXpAwarded) {
		if(protMelee(o) && !veracsEffect) { // if prayer active reduce damage by 40%
			damage = (int)damage * 60 / 100;
		}
		if (c.maxNextHit) {
			damage = calculateMeleeMaxHit();
		}
		}
		if (damage > 0 && guthansEffect) {
			c.playerLevel[3] += damage;
			if (c.playerLevel[3] > c.getLevelForXP(c.playerXP[3]))
				c.playerLevel[3] = c.getLevelForXP(c.playerXP[3]);
			c.getPA().refreshSkill(3);
			o.gfx0(398);		
		}
		if (!c.swingXpAwarded && c.ssSpec && damageMask == 2) {
			damage = 5 + Misc.random(11);
			c.ssSpec = false;
		}
		if (PlayerHandler.players[i].playerLevel[3] - damage < 0) { 
			damage = PlayerHandler.players[i].playerLevel[3];
		}
		if (o.vengOn && damage > 0)
			appendVengeance(i, damage);
		if (damage > 0)
			applyRecoil(damage, i);
		switch(c.specEffect) {
			case 1: // dragon scimmy special
			if(damage > 0) {
				if(o.prayerActive[16] || o.prayerActive[17] || o.prayerActive[18] || o.curseActive[7] || o.curseActive[8] || o.curseActive[9]) {
					o.headIcon = -1;
					o.getPA().sendFrame36(c.PRAYER_GLOW[16], 0);
					o.getPA().sendFrame36(c.PRAYER_GLOW[17], 0);
					o.getPA().sendFrame36(c.PRAYER_GLOW[18], 0);
					o.getPA().sendFrame36(o.CURSE_GLOW[7], 0);
					o.getPA().sendFrame36(o.CURSE_GLOW[8], 0);
					o.getPA().sendFrame36(o.CURSE_GLOW[9], 0);
				}
				o.sendMessage("You have been injured!");
				o.stopPrayerDelay = System.currentTimeMillis();
				o.prayerActive[16] = false;
				o.prayerActive[17] = false;
				o.prayerActive[18] = false;
				o.curseActive[7] = false;
				o.curseActive[8] = false;
				o.curseActive[9] = false;
				o.getPA().requestUpdates();		
			}
			break;
			case 2:
				if (damage > 0) {
					if (o.freezeTimer <= 0)
						o.freezeTimer = 30;
					o.gfx0(369);
					o.sendMessage("You have been frozen.");
					o.frozenBy = c.playerId;
					o.stopMovement();
					c.sendMessage("You freeze your enemy.");
				}		
			break;
			case 3:
				if (damage > 0) {
					o.playerLevel[1] -= damage;
					o.sendMessage("You feel weak.");
					if (o.playerLevel[1] < 1)
						o.playerLevel[1] = 1;
					o.getPA().refreshSkill(1);
				}
			break;
			case 4:
				if (damage > 0) {
					if (c.playerLevel[3] + damage > c.getLevelForXP(c.playerXP[3]))
						if (c.playerLevel[3] > c.getLevelForXP(c.playerXP[3]));
						else 
						c.playerLevel[3] = c.getLevelForXP(c.playerXP[3]);
					else 
						c.playerLevel[3] += damage;
					c.getPA().refreshSkill(3);
				}
			break;
		}
		c.specEffect = 0;
		if(!c.swingXpAwarded) {
		if(c.fightMode == 3) {
			
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 0); 
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 1);
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 2); 				
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 3);
			c.getPA().refreshSkill(0);
			c.getPA().refreshSkill(1);
			c.getPA().refreshSkill(2);
			c.getPA().refreshSkill(3);
			
		} else {
			
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE), c.fightMode); 
			c.getPA().addSkillXP((damage*Config.MELEE_EXP_RATE/3), 3);
			c.getPA().refreshSkill(c.fightMode);
			c.getPA().refreshSkill(3);
			
		}
		}
		PlayerHandler.players[i].logoutDelay = System.currentTimeMillis();
		PlayerHandler.players[i].underAttackBy = c.playerId;
		PlayerHandler.players[i].killerId = c.playerId;	
		PlayerHandler.players[i].singleCombatDelay = System.currentTimeMillis();
		if (c.killedBy != PlayerHandler.players[i].playerId)
			c.totalPlayerDamageDealt = 0;
		c.killedBy = PlayerHandler.players[i].playerId;
		applySmite(i, damage);
		if (damage > 0) {
			c.getCurse().applyHitEffects(damage, o);
		}
		switch(damageMask) {
			case 1:
			/*if (!Server.playerHandler.players[i].getHitUpdateRequired()){
				Server.playerHandler.players[i].setHitDiff(damage);
				Server.playerHandler.players[i].setHitUpdateRequired(true);
			} else {
				Server.playerHandler.players[i].setHitDiff2(damage);
				Server.playerHandler.players[i].setHitUpdateRequired2(true);			
			}*/
			//Server.playerHandler.players[i].playerLevel[3] -= damage;
			PlayerHandler.players[i].dealDamage(damage);
			PlayerHandler.players[i].damageTaken[c.playerId] += damage;
			c.totalPlayerDamageDealt += damage;
			PlayerHandler.players[i].updateRequired = true;
			o.getPA().refreshSkill(3);
			break;
		
			case 2:
			/*if (!Server.playerHandler.players[i].getHitUpdateRequired2()){
				Server.playerHandler.players[i].setHitDiff2(damage);
				Server.playerHandler.players[i].setHitUpdateRequired2(true);
			} else {
				Server.playerHandler.players[i].setHitDiff(damage);
				Server.playerHandler.players[i].setHitUpdateRequired(true);			
			}*/
			//Server.playerHandler.players[i].playerLevel[3] -= damage;
			PlayerHandler.players[i].dealDamage(damage);
			PlayerHandler.players[i].damageTaken[c.playerId] += damage;
			c.totalPlayerDamageDealt += damage;
			PlayerHandler.players[i].updateRequired = true;	
			c.doubleHit = false;
			o.getPA().refreshSkill(3);
			break;			
		}
		PlayerHandler.players[i].handleHitMask(damage);
	}
	
	public void applySmite(int index, int damage) {
		if (!c.prayerActive[23] && !c.curseActive[18])
			return;
		if (damage <= 0)
			return;
		if (PlayerHandler.players[index] != null) { 
			Client c2 = (Client)PlayerHandler.players[index];
			c2.playerLevel[5] -= (int)(damage/4);
			if (c2.playerLevel[5] <= 0) {
				c2.playerLevel[5] = 0;
				c2.getCombat().resetPrayers();
			}
			c2.getPA().refreshSkill(5);
			if (c.curseActive[18]) {
				c.getCurse().soulSplitPlayer(index, damage);
			}
		}
	}
	
	public void fireProjectilePlayer() {
		if(c.oldPlayerIndex > 0) {
			if(PlayerHandler.players[c.oldPlayerIndex] != null) {
				c.projectileStage = 2;
				int pX = c.getX();
				int pY = c.getY();
				int oX = PlayerHandler.players[c.oldPlayerIndex].getX();
				int oY = PlayerHandler.players[c.oldPlayerIndex].getY();
				int offX = (pY - oY)* -1;
				int offY = (pX - oX)* -1;	
				if (!c.msbSpec)
					c.getPA().createPlayersProjectile(pX, pY, offX, offY, 50, getProjectileSpeed(), getRangeProjectileGFX(), 43, 31, - c.oldPlayerIndex - 1, getStartDelay());
				else if (c.msbSpec) {
					c.getPA().createPlayersProjectile2(pX, pY, offX, offY, 50, getProjectileSpeed(), getRangeProjectileGFX(), 43, 31, - c.oldPlayerIndex - 1, getStartDelay(), 10);
					c.msbSpec = false;
				}
				if (usingDbow())
					c.getPA().createPlayersProjectile2(pX, pY, offX, offY, 50, getProjectileSpeed(), getRangeProjectileGFX(), 60, 31, - c.oldPlayerIndex - 1, getStartDelay(), 35);
			}
		}
	}
	
	public boolean usingDbow() {
		return c.playerEquipment[c.playerWeapon] == 11235;
	}
	
	
	

	
	/**Prayer**/
		
	public void activatePrayer(int i) {
		if(c.altarPrayed == 1) {
			if (i >= 0 && i < c.PRAYER_GLOW.length) {
				c.getPA().sendFrame36(c.PRAYER_GLOW[i], 0);
			}
			return;
		}
		if(c.duelRule[7]){
			for(int p = 0; p < c.PRAYER.length; p++) { // reset prayer glows 
				c.prayerActive[p] = false;
				c.getPA().sendFrame36(c.PRAYER_GLOW[p], 0);	
			}
			c.sendMessage("Prayer has been disabled in this duel!");
			return;
		}
		/*if (i == 24 && c.playerLevel[1] < 65) {
			c.getPA().sendFrame36(c.PRAYER_GLOW[i], 0);
			c.sendMessage("You may not use this prayer yet.");
			return;
		}
		if (i == 25 && c.playerLevel[1] < 70) {
			c.getPA().sendFrame36(c.PRAYER_GLOW[i], 0);
			c.sendMessage("You may not use this prayer yet.");
			return;
		}*/
		int[] defPray = {0,5,13,24,25};
		int[] strPray = {1,6,14,24,25};
		int[] atkPray = {2,7,15,24,25};
		int[] rangePray = {3,11,19};
		int[] magePray = {4,12,20};

		if(c.playerLevel[5] > 0 || !Config.PRAYER_POINTS_REQUIRED){
			if(c.getPA().getLevelForXP(c.playerXP[5]) >= c.PRAYER_LEVEL_REQUIRED[i] || !Config.PRAYER_LEVEL_REQUIRED) {
				boolean headIcon = false;
				switch(i) {
					case 0:
					case 5:
					case 13:
					if(c.prayerActive[i] == false) {
						for (int j = 0; j < defPray.length; j++) {
							if (defPray[j] != i) {
								c.prayerActive[defPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[defPray[j]], 0);
							}								
						}
					}
					break;
					
					case 1:
					case 6:
					case 14:
					if(c.prayerActive[i] == false) {
						for (int j = 0; j < strPray.length; j++) {
							if (strPray[j] != i) {
								c.prayerActive[strPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[strPray[j]], 0);
							}								
						}
						for (int j = 0; j < rangePray.length; j++) {
							if (rangePray[j] != i) {
								c.prayerActive[rangePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[rangePray[j]], 0);
							}								
						}
						for (int j = 0; j < magePray.length; j++) {
							if (magePray[j] != i) {
								c.prayerActive[magePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[magePray[j]], 0);
							}								
						}
					}
					break;
					
					case 2:
					case 7:
					case 15:
					if(c.prayerActive[i] == false) {
						for (int j = 0; j < atkPray.length; j++) {
							if (atkPray[j] != i) {
								c.prayerActive[atkPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[atkPray[j]], 0);
							}								
						}
						for (int j = 0; j < rangePray.length; j++) {
							if (rangePray[j] != i) {
								c.prayerActive[rangePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[rangePray[j]], 0);
							}								
						}
						for (int j = 0; j < magePray.length; j++) {
							if (magePray[j] != i) {
								c.prayerActive[magePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[magePray[j]], 0);
							}								
						}
					}
					break;
					
					case 3://range prays
					case 11:
					case 19:
					if(c.prayerActive[i] == false) {
						for (int j = 0; j < atkPray.length; j++) {
							if (atkPray[j] != i) {
								c.prayerActive[atkPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[atkPray[j]], 0);
							}								
						}
						for (int j = 0; j < strPray.length; j++) {
							if (strPray[j] != i) {
								c.prayerActive[strPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[strPray[j]], 0);
							}								
						}
						for (int j = 0; j < rangePray.length; j++) {
							if (rangePray[j] != i) {
								c.prayerActive[rangePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[rangePray[j]], 0);
							}								
						}
						for (int j = 0; j < magePray.length; j++) {
							if (magePray[j] != i) {
								c.prayerActive[magePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[magePray[j]], 0);
							}								
						}
					}
					break;
					case 4:
					case 12:
					case 20:
					if(c.prayerActive[i] == false) {
						for (int j = 0; j < atkPray.length; j++) {
							if (atkPray[j] != i) {
								c.prayerActive[atkPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[atkPray[j]], 0);
							}								
						}
						for (int j = 0; j < strPray.length; j++) {
							if (strPray[j] != i) {
								c.prayerActive[strPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[strPray[j]], 0);
							}								
						}
						for (int j = 0; j < rangePray.length; j++) {
							if (rangePray[j] != i) {
								c.prayerActive[rangePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[rangePray[j]], 0);
							}								
						}
						for (int j = 0; j < magePray.length; j++) {
							if (magePray[j] != i) {
								c.prayerActive[magePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[magePray[j]], 0);
							}								
						}
					}
					break;
					case 10:
						c.lastProtItem = System.currentTimeMillis();
					break;
					

					case 16:					
					case 17:
					case 18:
					if(System.currentTimeMillis() - c.stopPrayerDelay < 5000) {
						c.sendMessage("You have been injured and can't use this prayer!");
						c.getPA().sendFrame36(c.PRAYER_GLOW[16], 0);
						c.getPA().sendFrame36(c.PRAYER_GLOW[17], 0);
						c.getPA().sendFrame36(c.PRAYER_GLOW[18], 0);
						return;
					}
					if (i == 16)
						c.protMageDelay = System.currentTimeMillis();
					else if (i == 17)
						c.protRangeDelay = System.currentTimeMillis();
					else if (i == 18)
						c.protMeleeDelay = System.currentTimeMillis();
					case 21:
					case 22:
					case 23:
					headIcon = true;		
					for(int p = 16; p < 24; p++) {
						if(i != p && p != 19 && p != 20) {
							c.prayerActive[p] = false;
							c.getPA().sendFrame36(c.PRAYER_GLOW[p], 0);
						}
					}
					break;
					case 24:
					case 25:
					if (c.prayerActive[i] == false) {
						for (int j = 0; j < atkPray.length; j++) {
							if (atkPray[j] != i) {
								c.prayerActive[atkPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[atkPray[j]], 0);
							}								
						}
						for (int j = 0; j < strPray.length; j++) {
							if (strPray[j] != i) {
								c.prayerActive[strPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[strPray[j]], 0);
							}								
						}
						for (int j = 0; j < rangePray.length; j++) {
							if (rangePray[j] != i) {
								c.prayerActive[rangePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[rangePray[j]], 0);
							}								
						}
						for (int j = 0; j < magePray.length; j++) {
							if (magePray[j] != i) {
								c.prayerActive[magePray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[magePray[j]], 0);
							}								
						}
						for (int j = 0; j < defPray.length; j++) {
							if (defPray[j] != i) {
								c.prayerActive[defPray[j]] = false;
								c.getPA().sendFrame36(c.PRAYER_GLOW[defPray[j]], 0);
							}								
						}
					}
					break;
				}
				
				if(!headIcon) {
					if(c.prayerActive[i] == false) {
						c.prayerActive[i] = true;
						c.getPA().sendFrame36(c.PRAYER_GLOW[i], 1);					
					} else {
						c.prayerActive[i] = false;
						c.getPA().sendFrame36(c.PRAYER_GLOW[i], 0);
					}
				} else {
					if(c.prayerActive[i] == false) {
						c.prayerActive[i] = true;
						c.getPA().sendFrame36(c.PRAYER_GLOW[i], 1);
						c.headIcon = c.PRAYER_HEAD_ICONS[i];
						c.getPA().requestUpdates();
					} else {
						c.prayerActive[i] = false;
						c.getPA().sendFrame36(c.PRAYER_GLOW[i], 0);
						c.headIcon = -1;
						c.getPA().requestUpdates();
					}
				}
			} else {
				c.getPA().sendFrame36(c.PRAYER_GLOW[i],0);
				c.getPA().sendFrame126("You need a @blu@Prayer level of "+c.PRAYER_LEVEL_REQUIRED[i]+" to use "+c.PRAYER_NAME[i]+".", 357);
				c.getPA().sendFrame126("Click here to continue", 358);
				c.getPA().sendFrame164(356);
			}
		} else {
			c.getPA().sendFrame36(c.PRAYER_GLOW[i],0);
			c.sendMessage("You have run out of prayer points!");
		}	
				
	}
		
	/**
	*Specials
	**/
	
	public void activateSpecial(int weapon, int i){
		c.isUsingSpecial = true;
		c.doubleHit = false;
		c.specEffect = 0;
		c.projectileStage = 0;
		c.specMaxHitIncrease = 2;
		if(c.npcIndex > 0) {
			c.oldNpcIndex = i;
		} else if (c.playerIndex > 0){
			c.oldPlayerIndex = i;
			PlayerHandler.players[i].underAttackBy = c.playerId;
			PlayerHandler.players[i].logoutDelay = System.currentTimeMillis();
			PlayerHandler.players[i].singleCombatDelay = System.currentTimeMillis();
			PlayerHandler.players[i].killerId = c.playerId;
		}
		switch(weapon) {
		
		case 10887:
			c.gfx0(1027);
			c.startAnimation(5870);
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			c.specDamage = 1.15;
			c.specAccuracy = 1.50;
			break;
			
			case 1305: // dragon long
			c.gfx100(248);
			c.startAnimation(1058);
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			c.specAccuracy = 1.10;
			c.specDamage = 1.20;
			break;
			
			case 1215: // dragon daggers
			case 1231:
			case 5680:
			case 5698:
			c.gfx100(252);
			c.startAnimation(1062);
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			c.doubleHit = true;
			c.specAccuracy = 1.30;
			c.specDamage = 1.05;
			break;
			
			case 11730:
			c.gfx100(1224);
			c.startAnimation(7072);
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			c.doubleHit = true;
			c.ssSpec = true;
			c.specAccuracy = 1.30;
			break;
			
			case 4151: // whip
			case 15441:
			case 15442:
			case 15443:
			case 15444:
			if(NPCHandler.npcs[i] != null) {
				NPCHandler.npcs[i].gfx100(341);
			}
			c.specAccuracy = 1.10;
			c.startAnimation(1658);
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;
			
			case 700: // whip
			if(NPCHandler.npcs[i] != null) {
				NPCHandler.npcs[i].gfx100(341);
			}
			c.specAccuracy = 2;
			c.startAnimation(1658);
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;
			
			case 11694: // ags
			c.startAnimation(7074);
			c.specDamage = 1.25;
			c.specAccuracy = 1.85;
			c.gfx0(1222);
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;
			
			case 11700:
				c.startAnimation(7070);		
				c.gfx0(1221);
				c.specAccuracy = 1.25;
				c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
				c.specEffect = 2;
			break;
			
			case 11696:
				c.startAnimation(7073);
				c.gfx0(1223);
				c.specDamage = 1.10;
				c.specAccuracy = 1.5;
				c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
				c.specEffect = 3;
			break;
			
			case 11698:
				c.startAnimation(7071);
				c.gfx0(1220);
				c.specAccuracy = 1.25;
				c.specEffect = 4;
				c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;
			
			case 1249:
				c.startAnimation(405);
				c.gfx100(253);
				if (c.playerIndex > 0) {
					Client o = (Client)PlayerHandler.players[i];
					o.getPA().getSpeared(c.absX, c.absY);
				}	
			break;
			
			case 3204: // d hally
			c.gfx100(282);
			c.startAnimation(1203);
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			if(NPCHandler.npcs[i] != null && c.npcIndex > 0) {
				if(!c.goodDistance(c.getX(), c.getY(), NPCHandler.npcs[i].getX(), NPCHandler.npcs[i].getY(), 1)){
					c.doubleHit = true;
				}
			}
			if(PlayerHandler.players[i] != null && c.playerIndex > 0) {
				if(!c.goodDistance(c.getX(), c.getY(), PlayerHandler.players[i].getX(),PlayerHandler.players[i].getY(), 1)){
					c.doubleHit = true;
					c.delayedDamage2 = Misc.random(calculateMeleeMaxHit());
				}
			}
			break;
			
			case 4153: // maul
			c.startAnimation(1667);
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			/*if (c.playerIndex > 0)
				gmaulPlayer(i);
			else
				gmaulNpc(i);*/
			c.gfx100(337);
			break;
			
			case 4587: // dscimmy
			c.gfx100(347);
			c.specEffect = 1;
			c.startAnimation(1872);
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;
			
			case 1434: // mace
			c.startAnimation(1060);
			c.gfx100(251);
			c.specMaxHitIncrease = 3;
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase())+1;
			c.specDamage = 1.35;
			c.specAccuracy = 1.15;
			break;
			
			case 859: // magic long
			c.usingBow = true;
			c.bowSpecShot = 3;
			c.rangeItemUsed = c.playerEquipment[c.playerArrows];
			c.getItems().deleteArrow();	
			c.lastWeaponUsed = weapon;
			c.startAnimation(426);
			c.gfx100(250);	
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			c.projectileStage = 1;
			if (c.fightMode == 2)
				c.attackTimer--;
			break;
			
			case 861: // magic short	
			c.usingBow = true;			
			c.bowSpecShot = 1;
			c.rangeItemUsed = c.playerEquipment[c.playerArrows];
			c.getItems().deleteArrow();	
			c.lastWeaponUsed = weapon;
			c.startAnimation(1074);
			c.hitDelay = 3;
			c.projectileStage = 1;
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			if (c.fightMode == 2)
				c.attackTimer--;
			if (c.playerIndex > 0)
				fireProjectilePlayer();
			else if (c.npcIndex > 0)
				fireProjectileNpc();	
			break;
			
			case 11235: // dark bow	
			c.usingBow = true;
			c.dbowSpec = true;
			c.rangeItemUsed = c.playerEquipment[c.playerArrows];
			c.getItems().deleteArrow();
			c.getItems().deleteArrow();
			c.lastWeaponUsed = weapon;
			c.hitDelay = 3;
			c.startAnimation(426);
			c.projectileStage = 1;
			c.gfx100(getRangeStartGFX());
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			if (c.fightMode == 2)
				c.attackTimer--;
			if (c.playerIndex > 0)
				fireProjectilePlayer();
			else if (c.npcIndex > 0)
				fireProjectileNpc();
			c.specAccuracy = 1.75;
			c.specDamage = 1.50;
			break;

			case 14484:
			c.gfx0(1950);
			c.startAnimation(10961);
			c.specAccuracy = 1.85;
			c.specDamage = 1.05;
			c.doubleHit = true;
			c.usingClaws = true;
			c.clawDelay = 2;
			c.clawDamage = Misc.random(calculateMeleeMaxHit()) + (calculateMeleeMaxHit() / 3);
			if (c.playerIndex > 0) {
				c.clawIndex = c.playerIndex;
				c.clawType = 1;
			} else if (c.npcIndex > 0) {
				c.clawIndex = c.npcIndex;
				c.clawType = 2;
			}
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;

			case 19780:
			case 19784:
			c.gfx0(1247);
			c.startAnimation(4000);
			if (c.playerIndex > 0 && PlayerHandler.players[c.playerIndex] != null) {
				PlayerHandler.players[c.playerIndex].gfx0(1248);
			} else if (c.npcIndex > 0 && NPCHandler.npcs[c.npcIndex] != null) {
				NPCHandler.npcs[c.npcIndex].gfx0(1248);
			}
			c.specAccuracy = 1.85;
			c.specDamage = 1.50;
			c.ssSpec = true;
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;

			case 13902:
			c.startAnimation(10505);
			c.gfx0(1840);
			c.specDamage = 1.35;
			c.specAccuracy = 1.85;
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;

			case 13899:
			c.startAnimation(10502);
			c.specDamage = 1.15;
			c.specAccuracy = 1.70;
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;

			case 13905:
			c.startAnimation(10499);
			c.gfx0(1835);
			c.specAccuracy = 1.25;
			c.specEffect = 6;
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;

			case 13883:
			c.gfx100(1838);
			c.startAnimation(10504);
			c.usingRangeWeapon = true;
			c.specDamage = 1.25;
			c.specAccuracy = 1.75;
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;

			case 13879:
			c.gfx100(1836);
			c.startAnimation(10501);
			c.usingRangeWeapon = true;
			c.specDamage = 1.25;
			c.specAccuracy = 1.75;
			c.hitDelay = getHitDelay(ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase());
			break;
		}
		c.delayedDamage = Misc.random(calculateMeleeMaxHit());
		c.delayedDamage2 = Misc.random(calculateMeleeMaxHit());
		c.usingSpecial = false;
		c.getItems().updateSpecialBar();
	}

	public void applyClawFollowup() {
		if (!c.usingClaws) {
			return;
		}
		if (c.clawType == 1 && PlayerHandler.players[c.clawIndex] != null) {
			applyPlayerMeleeDamage(c.clawIndex, 1);
			applyPlayerMeleeDamage(c.clawIndex, 2);
		} else if (c.clawType == 2 && NPCHandler.npcs[c.clawIndex] != null) {
			applyNpcMeleeDamage(c.clawIndex, 1);
			applyNpcMeleeDamage(c.clawIndex, 2);
		}
		c.clawDelay = 0;
		c.usingClaws = false;
		c.clawType = 0;
		c.doubleHit = false;
	}

	public boolean checkSpecAmount(int weapon) {
		switch(weapon) {
			case 1249:
			case 1215:
			case 1231:
			case 5680:
			case 5698:
			case 1305:
			case 1434:
			if(c.specAmount >= 25) {
				c.specAmount -= 25;
				c.getItems().addSpecialBar(weapon);
				return true;
			}
			return false;
			
			case 4151:
			case 700:
			case 15441:
			case 15442:
			case 15443:
			case 15444:
            case 11694:
			case 11698:
			case 4153:
			case 10887:
			case 14484:
			case 13902:
			case 13905:
			case 13899:
			case 13879:
			case 13883:
			if(c.specAmount >= 50) {
				c.specAmount -= 50;
				c.getItems().addSpecialBar(weapon);
				return true;
			}
			return false;
			
			case 3204:
			if(c.specAmount >= 30) {
				c.specAmount -= 30;
				c.getItems().addSpecialBar(weapon);
				return true;
			}
			return false;
			
			case 1377:
			case 11696:
			case 11730:
			if(c.specAmount >= 100) {
				c.specAmount -= 100;
				c.getItems().addSpecialBar(weapon);
				return true;
			}
			return false;
			
			case 4587:
			case 859:
			case 861:
			case 11235:
			case 11700:
			case 19780:
			case 19784:
			if(c.specAmount >= 55) {
				c.specAmount -= 55;
				c.getItems().addSpecialBar(weapon);
				return true;
			}
			return false;

			
			default:
			return true; // incase u want to test a weapon
		}
	}
	
	public void resetPlayerAttack() {
		c.usingMagic = false;
		c.npcIndex = 0;
		c.faceUpdate(0);
		c.playerIndex = 0;
		c.getPA().resetFollow();
		//c.sendMessage("Reset attack.");
	}
	
	public int getCombatDifference(int combat1, int combat2) {
		if(combat1 > combat2) {
			return (combat1 - combat2);
		}
		if(combat2 > combat1) {
			return (combat2 - combat1);
		}	
		return 0;
	}
	
	/**
	*Get killer id 
	**/
	
	public int getKillerId(int playerId) {
		int oldDamage = 0;
		@SuppressWarnings("unused")
		int count = 0;
		int killerId = 0;
		for (int i = 1; i < Config.MAX_PLAYERS; i++) {	
			if (PlayerHandler.players[i] != null) {
				if(PlayerHandler.players[i].killedBy == playerId) {
					if (PlayerHandler.players[i].withinDistance(PlayerHandler.players[playerId])) {
						if(PlayerHandler.players[i].totalPlayerDamageDealt > oldDamage) {
							oldDamage = PlayerHandler.players[i].totalPlayerDamageDealt;
							killerId = i;
						}
					}	
					PlayerHandler.players[i].totalPlayerDamageDealt = 0;
					PlayerHandler.players[i].killedBy = 0;
				}	
			}
		}				
		return killerId;
	}
		
	
	
	public static double[] PRAYER_DRAIN = {
		0.5, // Thick Skin.
		0.5, // Burst of Strength.
		0.5, // Clarity of Thought.
		0.5, // Sharp Eye.
		0.5, // Mystic Will.
		1, // Rock Skin.
		1, // SuperHuman Strength.
		1, // Improved Reflexes.
		0.15, // Rapid restore
		0.3, // Rapid Heal.
		0.3, // Protect Items
		1, // Hawk eye.
		1, // Mystic Lore.
		2, // Steel Skin.
		2, // Ultimate Strength.
		2, // Incredible Reflexes.
		2, // Protect from Magic.
		2, // Protect from Missiles.
		2, // Protect from Melee.
		2, // Eagle Eye.
		2, // Mystic Might.
		0.5, // Retribution.
		1, // Redemption.
		2, // Smite
		4, // Chivalry.
		4, // Piety.
	};
	
	public void handlePrayerDrain(Client c) {
		c.usingPrayer = false;
		double toRemove = 0.0;
		for(int i = 0; i < PRAYER_DRAIN.length; i++) {
			if(c.prayerActive[i]) { 
				toRemove += PRAYER_DRAIN[i]/10;
				c.usingPrayer = true;
			}
		}
		for (int j = 0; j < c.CURSE_DRAIN.length; j++) {
			if (c.curseActive[j]) {
				toRemove += c.CURSE_DRAIN[j] / 20;
				c.usingPrayer = true;
			}
		}
		if (toRemove > 0) {
			toRemove /= (1 + (0.035 * c.playerBonus[11]));		
		}
		if (c.inBarrows()) {
			toRemove += 0.4 + (c.barrowsKill * 0.15);
		}
		c.prayerPoint -= toRemove;
		if (c.prayerPoint <= 0) {
			c.prayerPoint = 1.0 + c.prayerPoint;
			reducePrayerLevel();
		}
	}
	
	public void reducePrayerLevel() {
		if(c.playerLevel[5] - 1 > 0) {
			c.playerLevel[5] -= 1;
		} else {
			c.sendMessage("You have run out of prayer points!");
			c.playerLevel[5] = 0;
			resetPrayers();
			c.prayerId = -1;	
		}
		c.getPA().refreshSkill(5);
	}
	
	public void resetPrayers() {
		for(int i = 0; i < c.prayerActive.length; i++) {
			c.prayerActive[i] = false;
			c.getPA().sendFrame36(c.PRAYER_GLOW[i], 0);
		}
		if (c.getCurse() != null) {
			c.getCurse().resetCurse();
		}
		c.headIcon = -1;
		c.getPA().requestUpdates();
	}

	public boolean protMelee(Client o) {
		return o != null && (o.prayerActive[18] || o.curseActive[9]) && System.currentTimeMillis() - o.protMeleeDelay > 1500;
	}

	public boolean protRange(Client o) {
		return o != null && (o.prayerActive[17] || o.curseActive[8]) && System.currentTimeMillis() - o.protRangeDelay > 1500;
	}

	public boolean protMage(Client o) {
		return o != null && (o.prayerActive[16] || o.curseActive[7]) && System.currentTimeMillis() - o.protMageDelay > 1500;
	}
	
	/**
	* Wildy and duel info
	**/
	
	public boolean checkReqs() {
		if(PlayerHandler.players[c.playerIndex] == null) {
			return false;
		}
		if (c.playerIndex == c.playerId)
			return false;
		if (c.inPits && PlayerHandler.players[c.playerIndex].inPits)
			return true;
		if(c.inBH && PlayerHandler.players[c.playerIndex].inBH)
			return true;
		//castlewars
        if (CastleWars.isInCw(c) && CastleWars.isInCw(PlayerHandler.players[c.playerIndex]))
            return true;
		if(PlayerHandler.players[c.playerIndex].inDuelArena() && c.duelStatus != 5 && !c.usingMagic) {
			if(c.arenas() || c.duelStatus == 5) {
				c.sendMessage("You can't challenge inside the arena!");
				return false;
			}
			c.getTradeAndDuel().requestDuel(c.playerIndex);
			return false;
		}
		if(c.duelStatus == 5 && PlayerHandler.players[c.playerIndex].duelStatus == 5) {
			if(PlayerHandler.players[c.playerIndex].duelingWith == c.getId()) {
				return true;
			} else {
				c.sendMessage("This isn't your opponent!");
				return false;
			}
		}
		 if(!PlayerHandler.players[c.playerIndex].inWild()) {
			c.sendMessage("That player is not in the wilderness.");
			c.stopMovement();
			c.getCombat().resetPlayerAttack();
			return false;
		}
		 if(!c.inWild()) {
			 c.sendMessage("You are not in the wilderness.");
			 c.stopMovement();
			 c.getCombat().resetPlayerAttack();
			 return false;
			 }
		if(Config.COMBAT_LEVEL_DIFFERENCE && !c.inCw()) {
			int combatDif1 = c.getCombat().getCombatDifference(c.combatLevel, PlayerHandler.players[c.playerIndex].combatLevel);
			if(combatDif1 > c.wildLevel || combatDif1 > PlayerHandler.players[c.playerIndex].wildLevel) {
				c.sendMessage("Your combat level difference is too great to attack that player here.");
				c.stopMovement();
				c.getCombat().resetPlayerAttack();
				return false;
			}
		}
		
		if(Config.SINGLE_AND_MULTI_ZONES) {
			if(!PlayerHandler.players[c.playerIndex].inMulti()) {	// single combat zones
				if(PlayerHandler.players[c.playerIndex].underAttackBy != c.playerId  && PlayerHandler.players[c.playerIndex].underAttackBy != 0) {
					c.sendMessage("That player is already in combat.");
					c.stopMovement();
					c.getCombat().resetPlayerAttack();
					return false;
				}
				if(PlayerHandler.players[c.playerIndex].playerId != c.underAttackBy && c.underAttackBy != 0 || c.underAttackBy2 > 0) {
					c.sendMessage("You are already in combat.");
					c.stopMovement();
					c.getCombat().resetPlayerAttack();
					return false;
				}
			}
		}
		return true;
	}
	
	/*public boolean checkMultiBarrageReqs(int i) {
		if(Server.playerHandler.players[i] == null) {
			return false;
		}
		if (i == c.playerId)
			return false;
		if (c.inPits && Server.playerHandler.players[i].inPits)
			return true;
		if(!Server.playerHandler.players[i].inWild()) {
			return false;
		}
		if(Config.COMBAT_LEVEL_DIFFERENCE) {
			int combatDif1 = c.getCombat().getCombatDifference(c.combatLevel, Server.playerHandler.players[i].combatLevel);
			if(combatDif1 > c.wildLevel || combatDif1 > Server.playerHandler.players[i].wildLevel) {
				c.sendMessage("Your combat level difference is too great to attack that player here.");
				return false;
			}
		}
		
		if(Config.SINGLE_AND_MULTI_ZONES) {
			if(!Server.playerHandler.players[i].inMulti()) {	// single combat zones
				if(Server.playerHandler.players[i].underAttackBy != c.playerId  && Server.playerHandler.players[i].underAttackBy != 0) {
					return false;
				}
				if(Server.playerHandler.players[i].playerId != c.underAttackBy && c.underAttackBy != 0) {
					c.sendMessage("You are already in combat.");
					return false;
				}
			}
		}
		return true;
	}*/
	
	public boolean checkMultiBarrageReqs(int i) {
		if(PlayerHandler.players[i] == null) {
			return false;
		}
		if (i == c.playerId)
			return false;
		if (c.inPits && PlayerHandler.players[i].inPits)
			return true;
		if(!PlayerHandler.players[i].inWild()) {
			return false;
		}
		if(Config.COMBAT_LEVEL_DIFFERENCE) {
			int combatDif1 = c.getCombat().getCombatDifference(c.combatLevel, PlayerHandler.players[i].combatLevel);
			if(combatDif1 > c.wildLevel || combatDif1 > PlayerHandler.players[i].wildLevel) {
				c.sendMessage("Your combat level difference is too great to attack that player here.");
				return false;
			}
		}
		
		if(Config.SINGLE_AND_MULTI_ZONES) {
			if(!PlayerHandler.players[i].inMulti()) {	// single combat zones
				if(PlayerHandler.players[i].underAttackBy != c.playerId  && PlayerHandler.players[i].underAttackBy != 0) {
					return false;
				}
				if(PlayerHandler.players[i].playerId != c.underAttackBy && c.underAttackBy != 0) {
					c.sendMessage("You are already in combat.");
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	*Weapon stand, walk, run, etc emotes
	**/
	
	public void getPlayerAnimIndex(String weaponName){
		c.playerStandIndex = 0x328;
		c.playerTurnIndex = 0x337;
		c.playerWalkIndex = 0x333;
		c.playerTurn180Index = 0x334;
		c.playerTurn90CWIndex = 0x335;
		c.playerTurn90CCWIndex = 0x336;
		c.playerRunIndex = 0x338;
		
		if(weaponName.contains("anchor"))	{
			c.playerStandIndex = 5869;
			c.playerWalkIndex = 5867;
			c.playerRunIndex = 5868;
			return;
		}
		if(weaponName.contains("halberd") || weaponName.contains("guthan")) {
			c.playerStandIndex = 809;
			c.playerWalkIndex = 1146;
			c.playerRunIndex = 1210;
			return;
		}	
		if(weaponName.contains("dharok")) {
			c.playerStandIndex = 0x811;
			c.playerWalkIndex = 0x67F;
			c.playerRunIndex = 0x680;
			return;
		}	
		if(weaponName.contains("sled")) {
			c.playerStandIndex = 1461;
			c.playerWalkIndex = 1468;
			c.playerRunIndex = 1467;
			return;
		}
		if(weaponName.contains("ahrim")) {
			c.playerStandIndex = 809;
			c.playerWalkIndex = 1146;
			c.playerRunIndex = 1210;
			return;
		}
		if(weaponName.contains("verac")) {
			c.playerStandIndex = 1832;
			c.playerWalkIndex = 1830;
			c.playerRunIndex = 1831;
			return;
		}
		if (weaponName.contains("wand") || weaponName.contains("staff")) {
			c.playerStandIndex = 809;
			c.playerRunIndex = 1210;
			c.playerWalkIndex = 1146;
			return;
		}
		if(weaponName.contains("karil")) {
			c.playerStandIndex = 2074;
			c.playerWalkIndex = 2076;
			c.playerRunIndex = 2077;
			return;
		}
		if(weaponName.contains("2h sword") || weaponName.contains("godsword") || weaponName.contains("saradomin sw")) {
			c.playerStandIndex = 7047;
			c.playerWalkIndex = 7046;
			c.playerRunIndex = 7039;
			c.playerTurnIndex = 7044;
			c.playerTurn180Index = 7044;
			c.playerTurn90CWIndex = 7044;
			c.playerTurn90CCWIndex = 7044;
			return;
		}					
		if(weaponName.contains("bow")) {
			c.playerStandIndex = 808;
			c.playerWalkIndex = 819;
			c.playerRunIndex = 824;
			return;
		}

		switch(c.playerEquipment[c.playerWeapon]) {	
			case 4151:
			case 700:
			case 15441:
			case 15442:
			case 15443:
			case 15444:
			c.playerStandIndex = 1832;
			c.playerWalkIndex = 1660;
			c.playerRunIndex = 1661;
			break;
			case 6528:
				c.playerStandIndex = 0x811;
				c.playerWalkIndex = 2064;
				c.playerRunIndex = 1664;
			break;
			case 4153:
			case 13902:
			c.playerStandIndex = 1662;
			c.playerWalkIndex = 1663;
			c.playerRunIndex = 1664;
			break;
			case 14484:
			c.playerStandIndex = 2065;
			c.playerWalkIndex = 2064;
			c.playerRunIndex = 1664;
			break;
			case 11694:
			case 11696:
			case 11730:
			case 11698:
			case 11700:
			c.playerStandIndex = 4300;
			c.playerWalkIndex = 4306;
			c.playerRunIndex = 4305;
			break;
			case 1305:
			c.playerStandIndex = 809;
			break;
		}
	}
	
	/**
	* Weapon emotes
	**/
	
	public int getWepAnim(String weaponName) {
		if(c.playerEquipment[c.playerWeapon] <= 0) {
			switch(c.fightMode) {
				case 0:
				return 422;			
				case 2:
				return 423;			
				case 1:
				return 451;
			}
		}
		if(weaponName.contains("anchor"))	{
			return 5865;
		}
		if(weaponName.contains("knife") || weaponName.contains("dart") || weaponName.contains("javelin") || weaponName.contains("thrownaxe")){
			return 806;
		}
		if(weaponName.contains("halberd")) {
			return 440;
		}
		if(weaponName.startsWith("dragon dagger") || weaponName.contains("drag dagger")) {
			return 402;
		}	
		if(weaponName.endsWith("dagger")) {
			return 412;
		}	
		if(weaponName.endsWith("pickaxe")) {
			return 401;
		}
		if(weaponName.endsWith("axe")) {
			return 395;
		}	
		if(weaponName.contains("2h sword") || weaponName.contains("godsword") || weaponName.contains("aradomin sword")) {
			switch(c.fightMode) {
				case 0:
				return 7042;			
				case 2:
				return 7041;			
				case 1:
				return 7049;
			}
		}	
		if(weaponName.contains("sword")) {
			return 451;
		}
		if(weaponName.contains("karil")) {
			return 2075;
		}
		if(weaponName.contains("bow") && !weaponName.contains("'bow")) {
			return 426;
		}
		if (weaponName.contains("'bow"))
			return 4230;
			
		switch(c.playerEquipment[c.playerWeapon]) { // if you don't want to use strings
			case 6522:
			return 2614;
			case 13879:
			case 13883:
			return 806;
			case 14484:
			return 393;
			case 19780:
			case 19784:
			return 451;
			case 13902:
			return 2661;
			case 13899:
			return 451;
			case 13905:
			return 2080;
			case 4153: // granite maul
			return 1665;
			case 4726: // guthan 
			return 2080;
			case 4747: // torag
			return 0x814;
			case 4718: // dharok
			return 2067;
			case 4710: // ahrim
			return 406;
			case 4755: // verac
			return 2062;
			case 4734: // karil
			return 2075;
			case 4151:
			case 700:
			return 1658;
			case 6528:
			return 2661;
			default:
			return 451;
		}
	}
	
	/**
	* Block emotes
	*/
	
	public int getBlockEmote() {
		c.getItems();
		String shield = ItemAssistant.getItemName(c.playerEquipment[c.playerShield]).toLowerCase();
		c.getItems();
		String weapon = ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]).toLowerCase();
		if (shield.contains("book") && (weapon.contains("wand")))
                        return 420;
		if (shield.contains("defender"))
			return 4177;
		if (shield.contains("shield"))
			return 1156;
		if (weapon.contains("scimitar"))
			return 388;
                        //return 12030; //Or this one
                if (weapon.contains("whip") || weapon.contains("byssal tentacle"))
			return 1659;
                if (weapon.contains("2h sword") || weapon.contains("godsword") || weapon.contains("aradomin sword"))
			return 7050;
                if (weapon.contains("longsword"))
			return 388;
                if (weapon.contains("flail"))
			return 2063;
                if (weapon.contains("claws"))
			return 397;
                if (weapon.contains("spear") || weapon.contains("halberd"))
			return 430;
                if (weapon.contains("granite maul") || weapon.contains("gadderhammer"))
			return 1666;
                if (weapon.contains("staff"))
			return 420;
                if(weapon.contains("mace"))
                	return 403;
		switch(c.playerEquipment[c.playerWeapon]) {
		case 10887:
		return 5866;
		
			default:
			return 424;
		}
	}
	
	public int getAttackDelay(String s) {
		if(c.usingMagic) {
			switch(c.MAGIC_SPELLS[c.spellId][0]) {
				case 12871: // ice blitz
				case 13023: // shadow barrage
				case 12891: // ice barrage
				return 5;//5
				
				default:
				return 5;//5
			}
		}
		if(c.playerEquipment[c.playerWeapon] == -1)
			return 4;//unarmed
			
		switch (c.playerEquipment[c.playerWeapon]) {
			case 11235:
			return 9;
			case 11730:
			return 4;
			case 6528:
			return 7;
		}
		
		if(s.endsWith("greataxe"))
			return 7;
		else if(s.equals("torags hammers"))
			return 5;
		else if(s.equals("guthans warspear"))
			return 5;
		else if(s.equals("veracs flail"))
			return 5;
		else if(s.contains("anchor"))
			return 7;
		else if(s.equals("ahrims staff"))
			return 6;
		else if(s.contains("staff")){
			if(s.contains("zamarok") || s.contains("guthix") || s.contains("saradomian") || s.contains("slayer") || s.contains("ancient"))
				return 4;
			else
				return 5;
		} else if(s.contains("bow")){
			if(s.contains("composite") || s.equals("seercull"))
				return 5;
			else if (s.contains("aril"))
				return 4;
			else if(s.contains("Ogre"))
				return 8;
			else if(s.contains("short") || s.contains("hunt") || s.contains("sword"))
				return 4;
			else if(s.contains("long") || s.contains("crystal"))
				return 6;
			else if(s.contains("'bow"))
				return 7;
			
			return 5;
		}
		else if(s.contains("dagger"))
			return 4;
		else if(s.contains("godsword") || s.contains("2h"))
			return 6;
		else if(s.contains("longsword"))
			return 5;
		else if(s.contains("sword"))
			return 4;
		else if(s.contains("scimitar"))
			return 4;
		else if(s.contains("mace"))
			return 5;
		else if(s.contains("battleaxe"))
			return 6;
		else if(s.contains("pickaxe"))
			return 5;
		else if(s.contains("thrownaxe"))
			return 5;
		else if(s.contains("axe"))
			return 5;
		else if(s.contains("warhammer"))
			return 6;
		else if(s.contains("2h"))
			return 7;
		else if(s.contains("spear"))
			return 5;
		else if(s.contains("claw"))
			return 4;
		else if(s.contains("halberd"))
			return 7;
		
		//sara sword, 2400ms
		else if(s.equals("granite maul"))
			return 7;
		else if(s.equals("toktz-xil-ak"))//sword
			return 4;
		else if(s.equals("tzhaar-ket-em"))//mace
			return 5;
		else if(s.equals("tzhaar-ket-om"))//maul
			return 7;
		else if(s.equals("toktz-xil-ek"))//knife
			return 4;
		else if(s.equals("toktz-xil-ul"))//rings
			return 4;
		else if(s.equals("toktz-mej-tal"))//staff
			return 6;
		else if(s.contains("whip") || s.contains("byssal tentacle"))
			return 4;
		else if(s.contains("dart"))
			return 3;
		else if(s.contains("knife"))
			return 3;
		else if(s.contains("javelin"))
			return 6;
		return 5;
	}
	/**
	* How long it takes to hit your enemy
	**/
	public int getHitDelay(String weaponName) {
		if(c.usingMagic) {
			switch(c.MAGIC_SPELLS[c.spellId][0]) {			
				case 12891:
				return 4;
				case 12871:
				return 6;
				default:
				return 4;
			}
		} else {

			if(weaponName.contains("knife") || weaponName.contains("dart") || weaponName.contains("javelin") || weaponName.contains("thrownaxe")){
				return 3;
			}
			if(weaponName.contains("cross") || weaponName.contains("c'bow")) {
				return 4;
			}
			if(weaponName.contains("bow") && !c.dbowSpec) {
				return 4;
			} else if (c.dbowSpec) {
				return 4;
			}

			switch(c.playerEquipment[c.playerWeapon]) {	
				case 6522: // Toktz-xil-ul
				return 3;
				
				
				default:
				return 2;
			}
		}
	}
	
	public int getRequiredDistance() {
		if (c.followId > 0 && c.freezeTimer <= 0 && !c.isMoving)
			return 2;
		else if(c.followId > 0 && c.freezeTimer <= 0 && c.isMoving) {
			return 3;
		} else {
			return 1;
		}
	}
	
	public boolean usingHally() {
		switch(c.playerEquipment[c.playerWeapon]) {
			case 3190:
			case 3192:
			case 3194:
			case 3196:
			case 3198:
			case 3200:
			case 3202:
			case 3204:
			return true;
			
			default:
			return false;
		}
	}
	
	/**
	* Melee
	**/
	
	public int calculateMeleeAttack() {
		int attackLevel = c.playerLevel[0];
		//2, 5, 11, 18, 19
        if (c.prayerActive[2]) {
            attackLevel += c.getLevelForXP(c.playerXP[Player.playerAttack]) * 0.05;
        } else if (c.prayerActive[7]) {
            attackLevel += c.getLevelForXP(c.playerXP[Player.playerAttack]) * 0.1;
        } else if (c.prayerActive[15]) {
            attackLevel += c.getLevelForXP(c.playerXP[Player.playerAttack]) * 0.15;
        } else if (c.prayerActive[24]) {
            attackLevel += c.getLevelForXP(c.playerXP[Player.playerAttack]) * 0.15;
        } else if (c.prayerActive[25]) {
            attackLevel += c.getLevelForXP(c.playerXP[Player.playerAttack]) * 0.2;
        } else if (c.curseActive[19]) {
            attackLevel += c.getLevelForXP(c.playerXP[Player.playerAttack]) * 0.15 + c.getatt;
        } else if (c.curseActive[10]) {
            attackLevel += c.getLevelForXP(c.playerXP[Player.playerAttack]) * 0.05;
        }
        if (c.fullVoidMelee())
            attackLevel += c.getLevelForXP(c.playerXP[Player.playerAttack]) * 0.1;
		attackLevel *= c.specAccuracy;
		//c.sendMessage("Attack: " + (attackLevel + (c.playerBonus[bestMeleeAtk()] * 2)));
        int i = c.playerBonus[bestMeleeAtk()];
		i += c.bonusAttack;
		if (c.playerEquipment[c.playerAmulet] == 11128 && c.playerEquipment[c.playerWeapon] == 6528) {
			i *= 1.30;
		}
		return (int)(attackLevel + (attackLevel * 0.15) + (i + i * 0.05));
	}
	public int bestMeleeAtk()
    {
        if(c.playerBonus[0] > c.playerBonus[1] && c.playerBonus[0] > c.playerBonus[2])
            return 0;
        if(c.playerBonus[1] > c.playerBonus[0] && c.playerBonus[1] > c.playerBonus[2])
            return 1;
        return c.playerBonus[2] <= c.playerBonus[1] || c.playerBonus[2] <= c.playerBonus[0] ? 0 : 2;
    }
	
	public int calculateMeleeMaxHit() {
		double maxHit = 0;
		//int strBonus = c.playerBonus[10];
		int strength = c.playerLevel[2];
		int lvlForXP = c.getLevelForXP(c.playerXP[2]);
		if(c.prayerActive[1]) {
			strength += (int)(lvlForXP * .05);
		} else
		if(c.prayerActive[6]) {
			strength += (int)(lvlForXP * .10);
		} else
		if(c.prayerActive[14]) {
			strength += (int)(lvlForXP * .15);
		} else
		if(c.prayerActive[24]) {
			strength += (int)(lvlForXP * .18);
		} else
		if(c.prayerActive[25]) {
			strength += (int)(lvlForXP * .23);
		} else if(c.curseActive[19]) {
			strength += (int)(lvlForXP * .23) + c.getstr;
		} else if(c.curseActive[14]) {
			strength += (int)(lvlForXP * .05);
		}
		if(c.playerEquipment[c.playerHat] == 2526 && c.playerEquipment[c.playerChest] == 2520 && c.playerEquipment[c.playerLegs] == 2522) {	
			maxHit += (maxHit * 10 / 100);
		}
		maxHit += 1.05D + (double)(strBonus * strength) * 0.00175D;
		maxHit += (double)strength * 0.11D;
		if(c.playerEquipment[c.playerWeapon] == 4718 && c.playerEquipment[c.playerHat] == 4716 && c.playerEquipment[c.playerChest] == 4720 && c.playerEquipment[c.playerLegs] == 4722) {	
				maxHit += (c.getPA().getLevelForXP(c.playerXP[3]) - c.playerLevel[3]) / 2;			
		}
		if (c.specDamage > 1)
			maxHit = (int)(maxHit * c.specDamage);
		if (maxHit < 0)
			maxHit = 1;
		if (c.fullVoidMelee())
			maxHit = (int)(maxHit * 1.10);
		if (c.playerEquipment[c.playerAmulet] == 11128 && c.playerEquipment[c.playerWeapon] == 6528) {
			maxHit *= 1.20;
		}
		return (int)Math.floor(maxHit);
	}
	

	public int calculateMeleeDefence()
    {
        int defenceLevel = c.playerLevel[1];
		int i = c.playerBonus[bestMeleeDef()];
        if (c.prayerActive[0]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.05;
        } else if (c.prayerActive[5]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.1;
        } else if (c.prayerActive[13]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.15;
        } else if (c.prayerActive[24]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.2;
        } else if (c.prayerActive[25]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.25;
        } else if (c.curseActive[19]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.15 + c.getdef;
        } else if (c.curseActive[13]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.05;
        }
        return (int)(defenceLevel + (defenceLevel * 0.15) + (i + i * 0.05));
    }
	
	public int bestMeleeDef()
    {
        if(c.playerBonus[5] > c.playerBonus[6] && c.playerBonus[5] > c.playerBonus[7])
            return 5;
        if(c.playerBonus[6] > c.playerBonus[5] && c.playerBonus[6] > c.playerBonus[7])
            return 6;
        return c.playerBonus[7] <= c.playerBonus[5] || c.playerBonus[7] <= c.playerBonus[6] ? 5 : 7;
    }

	/**
	* Range
	**/
	
	public int calculateRangeAttack() {
		int attackLevel = c.playerLevel[4];
		attackLevel *= c.specAccuracy;
        if (c.fullVoidRange())
            attackLevel += c.getLevelForXP(c.playerXP[Player.playerRanged]) * 0.1;
		if (c.prayerActive[3])
			attackLevel *= 1.05;
		else if (c.prayerActive[11])
			attackLevel *= 1.10;
		else if (c.prayerActive[19])
			attackLevel *= 1.15;
		else if (c.curseActive[11])
			attackLevel *= 1.05;
		//dbow spec
		if (c.fullVoidRange() && c.specAccuracy > 1.15) {
			attackLevel *= 1.75;		
		}
        return (int) (attackLevel + (c.playerBonus[4] * 1.95));
	}
	
	public int calculateRangeDefence() {
		int defenceLevel = c.playerLevel[1];
        if (c.prayerActive[0]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.05;
        } else if (c.prayerActive[5]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.1;
        } else if (c.prayerActive[13]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.15;
        } else if (c.prayerActive[24]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.2;
        } else if (c.prayerActive[25]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.25;
        } else if (c.curseActive[19]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.15 + c.getdef;
        } else if (c.curseActive[13]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.05;
        }
        return (int) (defenceLevel + c.playerBonus[9] + (c.playerBonus[9] / 2));
	}
	
	public boolean usingBolts() {
		return c.playerEquipment[c.playerArrows] >= 9130 && c.playerEquipment[c.playerArrows] <= 9145 || c.playerEquipment[c.playerArrows] >= 9230 && c.playerEquipment[c.playerArrows] <= 9245;
	}
	public int rangeMaxHit() {
		int rangeLevel = c.playerLevel[4];
		double modifier = 1.0;
		double wtf = c.specDamage;
		int itemUsed = c.usingBow ? c.playerEquipment[c.playerArrows] : c.playerEquipment[c.playerWeapon];
		if (c.prayerActive[3])
			modifier += 0.05;
		else if (c.prayerActive[11])
			modifier += 0.10;
		else if (c.prayerActive[19])
			modifier += 0.15;
		else if (c.curseActive[11])
			modifier += 0.05;
		if (c.fullVoidRange())
			modifier += 0.20;
		double c = modifier * rangeLevel;
		int rangeStr = getRangeStr(itemUsed);
		double max =(c + 8) * (rangeStr + 64) / 640;
		if (wtf != 1)
			max *= wtf;
		if (max < 1)
			max = 1;
		return (int)max;
	}
	
	public int getRangeStr(int i) {
		if (i == 4214)
			return 100;
		switch (i) {
			//bronze to rune bolts
			case 877:
			return 10;
			case 9140:
			return 46;
			case 9141:
			return 64;
			case 9142:
			case 9241:
			case 9240:
			return 82;
			case 9143:
			case 9243:
			case 9242:
			return 100;
			case 9144:
			case 9244:
			case 9245:
			return 115;
			//bronze to dragon arrows
			case 882:
			return 7;
			case 884:
			return 10;
			case 886:
			return 16;
			case 888:
			case 6724:
			return 22;
			case 4160:
			return 28;
			case 890:
			return 31;
			case 892:
			case 4740:
			return 49;
			case 11212:
			return 60;
			//knifes
			case 864:
			return 3;
			case 863:
			return 4;
			case 865:
			return 7;
			case 866:
			return 10;
			case 867:
			return 14;
			case 868:
			return 24;
			case 13879:
			return 85;
			case 13883:
			return 69;
		}
		return 0;
	}
	
	/*public int rangeMaxHit() {
        int rangehit = 0;
        rangehit += c.playerLevel[4] / 7.5;
        int weapon = c.lastWeaponUsed;
        int Arrows = c.lastArrowUsed;
        if (weapon == 4223) {//Cbow 1/10
            rangehit = 2;
            rangehit += c.playerLevel[4] / 7;
        } else if (weapon == 4222) {//Cbow 2/10
            rangehit = 3;
            rangehit += c.playerLevel[4] / 7;
        } else if (weapon == 4221) {//Cbow 3/10
            rangehit = 3;
            rangehit += c.playerLevel[4] / 6.5;
        } else if (weapon == 4220) {//Cbow 4/10
            rangehit = 4;
            rangehit += c.playerLevel[4] / 6.5;
        } else if (weapon == 4219) {//Cbow 5/10
            rangehit = 4;
            rangehit += c.playerLevel[4] / 6;
        } else if (weapon == 4218) {//Cbow 6/10
            rangehit = 5;
            rangehit += c.playerLevel[4] / 6;
        } else if (weapon == 4217) {//Cbow 7/10
            rangehit = 5;
            rangehit += c.playerLevel[4] / 5.5;
        } else if (weapon == 4216) {//Cbow 8/10
            rangehit = 6;
            rangehit += c.playerLevel[4] / 5.5;
        } else if (weapon == 4215) {//Cbow 9/10
            rangehit = 6;
            rangehit += c.playerLevel[4] / 5;
        } else if (weapon == 4214) {//Cbow Full
            rangehit = 7;
            rangehit += c.playerLevel[4] / 5;
        } else if (weapon == 6522) {
            rangehit = 5;
            rangehit += c.playerLevel[4] / 6;
        } else if (weapon == 9029) {//dragon darts
            rangehit = 8;
            rangehit += c.playerLevel[4] / 10;
        } else if (weapon == 811 || weapon == 868) {//rune darts
            rangehit = 2;
            rangehit += c.playerLevel[4] / 8.5;
        } else if (weapon == 810 || weapon == 867) {//adamant darts
            rangehit = 2;
            rangehit += c.playerLevel[4] / 9;
        } else if (weapon == 809 || weapon == 866) {//mithril darts
            rangehit = 2;
            rangehit += c.playerLevel[4] / 9.5;
        } else if (weapon == 808 || weapon == 865) {//Steel darts
            rangehit = 2;
            rangehit += c.playerLevel[4] / 10;
        } else if (weapon == 807 || weapon == 863) {//Iron darts
            rangehit = 2;
            rangehit += c.playerLevel[4] / 10.5;
        } else if (weapon == 806 || weapon == 864) {//Bronze darts
            rangehit = 1;
            rangehit += c.playerLevel[4] / 11;
        } else if (Arrows == 4740 && weapon == 4734) {//BoltRacks
			rangehit = 3;
            rangehit += c.playerLevel[4] / 6;
        } else if (Arrows == 11212) {//dragon arrows
            rangehit = 4;
            rangehit += c.playerLevel[4] / 5.5;
        } else if (Arrows == 892) {//rune arrows
            rangehit = 3;
            rangehit += c.playerLevel[4] / 6;
        } else if (Arrows == 890) {//adamant arrows
            rangehit = 2;
            rangehit += c.playerLevel[4] / 7;
        } else if (Arrows == 888) {//mithril arrows
            rangehit = 2;
            rangehit += c.playerLevel[4] / 7.5;
        } else if (Arrows == 886) {//steel arrows
            rangehit = 2;
            rangehit += c.playerLevel[4] / 8;
        } else if (Arrows == 884) {//Iron arrows
            rangehit = 2;
            rangehit += c.playerLevel[4] / 9;
        } else if (Arrows == 882) {//Bronze arrows
            rangehit = 1;
            rangehit += c.playerLevel[4] / 9.5;
        } else if (Arrows == 9244) {
			rangehit = 8;
			rangehit += c.playerLevel[4] / 3;
		} else if (Arrows == 9139) {
			rangehit = 12;
			rangehit += c.playerLevel[4] / 4;
		} else if (Arrows == 9140) {
			rangehit = 2;
            rangehit += c.playerLevel[4] / 7;
		} else if (Arrows == 9141) {
			rangehit = 3;
            rangehit += c.playerLevel[4] / 6;
		} else if (Arrows == 9142) {
			rangehit = 4;
            rangehit += c.playerLevel[4] / 6;
		} else if (Arrows == 9143) {
			rangehit = 7;
			rangehit += c.playerLevel[4] / 5;
		} else if (Arrows == 9144) {
			rangehit = 7;
			rangehit += c.playerLevel[4] / 4.5;
		}
        int bonus = 0;
        bonus -= rangehit / 10;
        rangehit += bonus;
        if (c.specDamage != 1)
			rangehit *= c.specDamage;
		if (rangehit == 0)
			rangehit++;
		if (c.fullVoidRange()) {
			rangehit *= 1.10;
		}
		if (c.prayerActive[3])
			rangehit *= 1.05;
		else if (c.prayerActive[11])
			rangehit *= 1.10;
		else if (c.prayerActive[19])
			rangehit *= 1.15;
		return rangehit;
    }*/
	
	public boolean properBolts() {
		return c.playerEquipment[c.playerArrows] >= 9140 && c.playerEquipment[c.playerArrows] <= 9144
				|| c.playerEquipment[c.playerArrows] >= 9240 && c.playerEquipment[c.playerArrows] <= 9244;
	}
	
	public int getArrowTier(){
		switch(c.playerEquipment[c.playerArrows]){
			case 882:
			case 883:
			case 5616:
			case 5622:
				return 1;
			case 884:
			case 885:
			case 5617:
			case 5623:
				return 2;
			case 886:
			case 887:
			case 5618:
			case 5624:
				return 3;
			case 888:
			case 889:
			case 5619:
			case 5625:
				return 4;
			case 890:
			case 891:
			case 5620:
			case 5626:
				return 5;
			case 892:
			case 893:
			case 5621:
			case 5627:
			case 4160:
				return 6;
			case 4740:
				return 7;
			case 11212:
				return 8;
		}
		return -1;
	}
	
	public int getBowTier(){
		switch(c.playerEquipment[c.playerWeapon]){
			case 839:
			case 841:
				return 1;
	
			case 843:
			case 845:
				return 2;
	
			case 847:
			case 849:
				return 3;
	
			case 851:
			case 853:
				return 4;
	
			case 855:
			case 857:
				return 5;
	
			case 859:
			case 861:
			case 6724:
				return 6;
	
			case 4734:
			case 4935:
			case 4936:
			case 4937:
				return 7;
	
			case 11235:
				return 8;
		}
		return -1;
	}
	
	public boolean correctBowAndArrows() {
		if(getBowTier() == -1 || getArrowTier() == -1)
			return false;
		if(getBowTier() >= getArrowTier())
			return true;
		return false;
	}
	
	/*private static int[] arrows = {4160, 892};
	
	public int correctBowAndArrows() {
		if (usingBolts())
			return -1;
		for (int j = 0; j <= arrows.length; j++) {
		switch(c.playerEquipment[c.playerWeapon]) {
			
			case 839:
			case 841:
			return 882;
			
			case 843:
			case 845:
			return 884;
			
			case 847:
			case 849:
			return 886;
			
			case 851:
			case 853:
			return 888;        
			
			case 855:
			case 857:
			return 890;
			
			case 859:
			case 861:
			case 6724:
			return arrows[j];
			
			case 4734:
			case 4935:
			case 4936:
			case 4937:
			return 4740;
			
			case 11235:
			return 11212;
		}
		}
		return -1;
	}*/
	
	public int getRangeStartGFX() {
		if (c.dbowSpec) {
			return 1099;
		}
		RangedAmmoData data = getRangedAmmoData(c.rangeItemUsed);
		if (data != null) {
			return data.startGfx;
		}
		return -1;
	}
		
	public int getRangeProjectileGFX() {
		if (c.dbowSpec) {
			return 1099;
		}
		if(c.bowSpecShot > 0) {
			return 249;
		}
		if (c.playerEquipment[c.playerWeapon] == 9185)
			return 27;

		RangedAmmoData data = getRangedAmmoData(c.rangeItemUsed);
		if (data != null) {
			return data.projectileGfx;
		}

		// Special cases not in the map
		if (c.rangeItemUsed == 6522) { // Toktz-xil-ul
			return 442;
		}
		if (c.rangeItemUsed == 4740) { // bolt rack
			return 27;
		}

		return -1;
	}
	
	public int getProjectileSpeed() {
		if (c.dbowSpec)
			return 100;

		RangedAmmoData data = getRangedAmmoData(c.rangeItemUsed);
		if (data != null) {
			return data.speed;
		}
		return 70;
	}
	
	public int getProjectileShowDelay() {
		switch(c.playerEquipment[c.playerWeapon]) {
		case 863: //iron
		case 871:
		case 5655:
		case 5662:
		case 864: //bronze
		case 870:
		case 5654:
		case 5661:
		case 865:
		case 872:
		case 5656:
		case 5663:
		case 866: // knives
		case 873:
		case 5657:
		case 5664:
		case 867:
		case 875:
		case 5659:
		case 5666:
		case 868:
		case 876:
		case 5660:
		case 5667:
		case 869: //black
		case 874:
		case 5658:
		case 5665: 

		case 806:
		case 812:
		case 5628:
		case 5635:
		case 807:
		case 813:
		case 5629:
		case 5636:
		case 808:
		case 814:
		case 5630:
		case 5637:
		case 809: // darts
		case 815:
		case 5632:
		case 5639:
		case 810:
		case 816:
		case 5633:
		case 5640:
		case 811:
		case 817:
		case 5641:
		case 5634:

		case 825:
		case 831:
		case 5642:
		case 5648:
		case 826:
		case 832:
		case 5643:
		case 5649:
		case 827: // javelin
		case 833:
		case 5644:
		case 5650:
		case 828:
		case 834:
		case 5645:
		case 5651:
		case 829:
		case 835:
		case 5646:
		case 5652:
		case 830:
		case 836:
		case 5647:
		case 5653:

		case 800:
		case 801:
		case 802: // axes
		case 803:
		case 804:
		case 805:
			
			case 4734:
            case 9185:
			case 4935:
			case 4936:
			case 4937:
			return 15; 
			
		
			default:
			return 5;
		}
	}
	
	/**
	*MAGIC
	**/
	
	public int mageAtk()
    {
        int attackLevel = c.playerLevel[6];
		if (c.fullVoidMage())
            attackLevel += c.getLevelForXP(c.playerXP[6]) * 0.2;
        if (c.prayerActive[4])
			attackLevel *= 1.05;
		else if (c.prayerActive[12])
			attackLevel *= 1.10;
		else if (c.prayerActive[20])
			attackLevel *= 1.15;
		else if (c.curseActive[12])
			attackLevel *= 1.05;
        return (int) (attackLevel + (c.playerBonus[3] * 2));
    }
	public int mageDef()
    {
        int defenceLevel = c.playerLevel[1]/2 + c.playerLevel[6]/2;
        if (c.prayerActive[0]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.05;
        } else if (c.prayerActive[3]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.1;
        } else if (c.prayerActive[9]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.15;
        } else if (c.prayerActive[18]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.2;
        } else if (c.prayerActive[19]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.25;
        } else if (c.curseActive[19]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.15 + c.getdef;
        } else if (c.curseActive[13]) {
            defenceLevel += c.getLevelForXP(c.playerXP[Player.playerDefence]) * 0.05;
        }
        return (int) (defenceLevel + c.playerBonus[8] + (c.playerBonus[8] / 3));
    }
	
	public boolean wearingStaff(int runeId) {
		int wep = c.playerEquipment[c.playerWeapon];
		switch (runeId) {
			case 554:
			if (wep == 1387)
				return true;
			break;
			case 555:
			if (wep == 1383)
				return true;
			break;
			case 556:
			if (wep == 1381)
				return true;
			break;
			case 557:
			if (wep == 1385)
				return true;
			break;
		}
		return false;
	}
	
	public boolean checkMagicReqs(int spell) {
		if(c.usingMagic && Config.RUNES_REQUIRED) { // check for runes
			if((!c.getItems().playerHasItem(c.MAGIC_SPELLS[spell][8], c.MAGIC_SPELLS[spell][9]) && !wearingStaff(c.MAGIC_SPELLS[spell][8])) ||
				(!c.getItems().playerHasItem(c.MAGIC_SPELLS[spell][10], c.MAGIC_SPELLS[spell][11]) && !wearingStaff(c.MAGIC_SPELLS[spell][10])) ||
				(!c.getItems().playerHasItem(c.MAGIC_SPELLS[spell][12], c.MAGIC_SPELLS[spell][13]) && !wearingStaff(c.MAGIC_SPELLS[spell][12])) ||
				(!c.getItems().playerHasItem(c.MAGIC_SPELLS[spell][14], c.MAGIC_SPELLS[spell][15]) && !wearingStaff(c.MAGIC_SPELLS[spell][14]))){
			c.sendMessage("You don't have the required runes to cast this spell.");
			return false;
			} 
		}

		if(c.usingMagic && c.playerIndex > 0) {
			if(PlayerHandler.players[c.playerIndex] != null) {
				for(int r = 0; r < c.REDUCE_SPELLS.length; r++){	// reducing spells, confuse etc
					if(PlayerHandler.players[c.playerIndex].REDUCE_SPELLS[r] == c.MAGIC_SPELLS[spell][0]) {
						c.reduceSpellId = r;
						if((System.currentTimeMillis() - PlayerHandler.players[c.playerIndex].reduceSpellDelay[c.reduceSpellId]) > PlayerHandler.players[c.playerIndex].REDUCE_SPELL_TIME[c.reduceSpellId]) {
							PlayerHandler.players[c.playerIndex].canUseReducingSpell[c.reduceSpellId] = true;
						} else {
							PlayerHandler.players[c.playerIndex].canUseReducingSpell[c.reduceSpellId] = false;
						}
						break;
					}			
				}
				if(!PlayerHandler.players[c.playerIndex].canUseReducingSpell[c.reduceSpellId]) {
					c.sendMessage("That player is currently immune to this spell.");
					c.usingMagic = false;
					c.stopMovement();
					resetPlayerAttack();
					return false;
				}
			}
		}

		int staffRequired = getStaffNeeded();
		if(c.usingMagic && staffRequired > 0 && Config.RUNES_REQUIRED) { // staff required
			if(c.playerEquipment[c.playerWeapon] != staffRequired) {
				c.sendMessage("You need a "+ItemAssistant.getItemName(staffRequired).toLowerCase()+" to cast this spell.");
				return false;
			}
		}
		
		if(c.usingMagic && Config.MAGIC_LEVEL_REQUIRED) { // check magic level
			if(c.playerLevel[6] < c.MAGIC_SPELLS[spell][1]) {
				c.sendMessage("You need to have a magic level of " +c.MAGIC_SPELLS[spell][1]+" to cast this spell.");
				return false;
			}
		}
		if(c.usingMagic && Config.RUNES_REQUIRED) {
			if(c.MAGIC_SPELLS[spell][8] > 0) { // deleting runes
				if (!wearingStaff(c.MAGIC_SPELLS[spell][8]))
					c.getItems().deleteItem(c.MAGIC_SPELLS[spell][8], c.getItems().getItemSlot(c.MAGIC_SPELLS[spell][8]), c.MAGIC_SPELLS[spell][9]);
			}
			if(c.MAGIC_SPELLS[spell][10] > 0) {
				if (!wearingStaff(c.MAGIC_SPELLS[spell][10]))
					c.getItems().deleteItem(c.MAGIC_SPELLS[spell][10], c.getItems().getItemSlot(c.MAGIC_SPELLS[spell][10]), c.MAGIC_SPELLS[spell][11]);
			}
			if(c.MAGIC_SPELLS[spell][12] > 0) {
				if (!wearingStaff(c.MAGIC_SPELLS[spell][12]))
					c.getItems().deleteItem(c.MAGIC_SPELLS[spell][12], c.getItems().getItemSlot(c.MAGIC_SPELLS[spell][12]), c.MAGIC_SPELLS[spell][13]);
			}
			if(c.MAGIC_SPELLS[spell][14] > 0) {
				if (!wearingStaff(c.MAGIC_SPELLS[spell][14]))
					c.getItems().deleteItem(c.MAGIC_SPELLS[spell][14], c.getItems().getItemSlot(c.MAGIC_SPELLS[spell][14]), c.MAGIC_SPELLS[spell][15]);
			}
		}
		return true;
	}
	
	

	/** Apply ice freeze to an NPC (barrage/burst/blitz/rush). Stops movement immediately. */
	public void applyNpcFreeze(int npcIndex) {
		if (npcIndex <= 0 || NPCHandler.npcs[npcIndex] == null) {
			return;
		}
		int delay = getFreezeTime();
		if (delay <= 0 && c.oldSpellId >= 0 && c.oldSpellId < c.MAGIC_SPELLS.length) {
			// Fallback by spell id in case timer table missed an index.
			switch (c.MAGIC_SPELLS[c.oldSpellId][0]) {
			case 12861:
				delay = 10;
				break;
			case 12881:
				delay = 17;
				break;
			case 12871:
				delay = 25;
				break;
			case 12891:
				delay = 33;
				break;
			default:
				break;
			}
		}
		if (delay <= 0) {
			return;
		}
		NPC n = NPCHandler.npcs[npcIndex];
		// Refresh freeze if already frozen (stacking ice hits).
		if (n.freezeTimer < delay) {
			n.freezeTimer = delay;
		}
		n.moveX = 0;
		n.moveY = 0;
		n.direction = -1;
		n.updateRequired = true;
	}

	public int getFreezeTime() {
		switch(c.MAGIC_SPELLS[c.oldSpellId][0]) {
			case 1572:
			case 12861: // ice rush
			return 10;
						
			case 1582:
			case 12881: // ice burst
			return 17;
			
			case 1592:
			case 12871: // ice blitz
			return 25;
			
			case 12891: // ice barrage
			return 33;
			
			default:
			return 0;
		}
	}
	
	public void freezePlayer(int i) {
		
	
	}

	public int getStartHeight() {
		switch(c.MAGIC_SPELLS[c.spellId][0]) {
			case 1562: // stun
			return 25;
			
			case 12939:// smoke rush
			return 35;
			
			case 12987: // shadow rush
			return 38;
			
			case 12861: // ice rush
			return 15;
			
			case 12951:  // smoke blitz
			return 38;
			
			case 12999: // shadow blitz
			return 25;
			
			case 12911: // blood blitz
			return 25;
			
			default:
			return 43;
		}
	}
	

	
	public int getEndHeight() {
		switch(c.MAGIC_SPELLS[c.spellId][0]) {
			case 1562: // stun
			return 10;
			
			case 12939: // smoke rush
			return 20;
			
			case 12987: // shadow rush
			return 28;
			
			case 12861: // ice rush
			return 10;
			
			case 12951:  // smoke blitz
			return 28;
			
			case 12999: // shadow blitz
			return 15;
			
			case 12911: // blood blitz
			return 10;
				
			default:
			return 31;
		}
	}
	
	public int getStartDelay() {
		switch(c.MAGIC_SPELLS[c.spellId][0]) {
			case 1539:
			return 60;
			
			default:
			return 53;
		}
	}
	
	public int getStaffNeeded() {
		switch(c.MAGIC_SPELLS[c.spellId][0]) {
			case 1539:
			return 1409;
			
			case 12037:
			return 4170;
			
			case 1190:
			return 2415;
			
			case 1191:
			return 2416;
			
			case 1192:
			return 2417;
			
			default:
			return 0;
		}
	}
	
	public boolean godSpells() {
		switch(c.MAGIC_SPELLS[c.spellId][0]) {	
			case 1190:
			return true;
			
			case 1191:
			return true;
			
			case 1192:
			return true;
			
			default:
			return false;
		}
	}
		
	public int getEndGfxHeight() {
		switch(c.MAGIC_SPELLS[c.oldSpellId][0]) {
			case 12987:	
			case 12901:		
			case 12861:
			case 12445:
			case 1192:
			case 13011:
			case 12919:
			case 12881:
			case 12999:
			case 12911:
			case 12871:
			case 13023:
			case 12929:
			case 12891:
			return 0;
			
			default:
			return 100;
		}
	}
	
	public int getStartGfxHeight() {
		switch(c.MAGIC_SPELLS[c.spellId][0]) {
			case 12871:
			case 12891:
			return 0;
			
			default:
			return 100;
		}
	}
	
	public void handleDfs() {
		if (System.currentTimeMillis() - c.dfsDelay > 30000) {
			if (c.playerIndex > 0 && PlayerHandler.players[c.playerIndex] != null) {
				int damage = Misc.random(15) + 5;
				c.startAnimation(2836);
				c.gfx0(600);
				PlayerHandler.players[c.playerIndex].playerLevel[3] -= damage;
				PlayerHandler.players[c.playerIndex].hitDiff2 = damage;
				PlayerHandler.players[c.playerIndex].hitUpdateRequired2 = true;
				PlayerHandler.players[c.playerIndex].updateRequired = true;
				c.dfsDelay = System.currentTimeMillis();						
			} else {
				c.sendMessage("I should be in combat before using this.");
			}
		} else {
			c.sendMessage("My shield hasn't finished recharging yet.");
		}
	}
	
	public void handleDfsNPC() {
		if (System.currentTimeMillis() - c.dfsDelay > 30000) {
			if (c.npcIndex > 0 && NPCHandler.npcs[c.npcIndex] != null) {
				int damage = Misc.random(15) + 5;
				c.startAnimation(2836);
				c.gfx0(600);
				NPCHandler.npcs[c.npcIndex].HP -= damage;
				NPCHandler.npcs[c.npcIndex].hitDiff2 = damage;
				NPCHandler.npcs[c.npcIndex].hitUpdateRequired2 = true;
				NPCHandler.npcs[c.npcIndex].updateRequired = true;
				c.dfsDelay = System.currentTimeMillis();						
			} else {
				c.sendMessage("I should be in combat before using this.");
			}
		} else {
			c.sendMessage("My shield hasn't finished recharging yet.");
		}
	}
	
	public void applyRecoilNPC(int damage, int i) {
		if (damage <= 0 || i <= 0 || NPCHandler.npcs[i] == null) {
			return;
		}
		int recDamage = 0;
		boolean deflect = c.curseActive[7] || c.curseActive[8] || c.curseActive[9];
		if (c.playerEquipment[c.playerRing] == 2550) {
			recDamage += damage / 10 + 1;
		}
		if (deflect) {
			int dmg = damage / 6;
			if (dmg < 1) {
				dmg = 1;
			}
			recDamage += dmg;
		}
		if (recDamage <= 0) {
			return;
		}
		if (deflect && c.getCurse() != null) {
			c.getCurse().playDeflect();
		}
		NPCHandler.npcs[i].HP -= recDamage;
		NPCHandler.npcs[i].hitDiff2 = recDamage;
		NPCHandler.npcs[i].hitUpdateRequired2 = true;
		NPCHandler.npcs[i].updateRequired = true;
		c.updateRequired = true;
	}
	
	public void applyRecoil(int damage, int i) {
		if (damage > 0 && PlayerHandler.players[i].playerEquipment[c.playerRing] == 2550) {
			int recDamage = damage/10 + 1;
			if (!c.getHitUpdateRequired()) {
				c.setHitDiff(recDamage);
				c.setHitUpdateRequired(true);				
			} else if (!c.getHitUpdateRequired2()) {
				c.setHitDiff2(recDamage);
				c.setHitUpdateRequired2(true);
			}
			c.dealDamage(recDamage);
			c.updateRequired = true;
		}
		if (damage > 0 && PlayerHandler.players[i] != null
				&& (PlayerHandler.players[i].curseActive[7]
						|| PlayerHandler.players[i].curseActive[8]
						|| PlayerHandler.players[i].curseActive[9])) {
			int recDamage = damage / 6;
			if (recDamage < 1) {
				recDamage = 1;
			}
			Client defender = (Client) PlayerHandler.players[i];
			if (defender.getCurse() != null) {
				defender.getCurse().playDeflect();
			}
			if (!c.getHitUpdateRequired()) {
				c.setHitDiff(recDamage);
				c.setHitUpdateRequired(true);
			} else if (!c.getHitUpdateRequired2()) {
				c.setHitDiff2(recDamage);
				c.setHitUpdateRequired2(true);
			}
			c.dealDamage(recDamage);
			c.updateRequired = true;
		}
	}
	
	public int getBonusAttack(int i) {
		switch (NPCHandler.npcs[i].npcType) {
			case 2883:
			return Misc.random(50) + 30;
			case 2026:
			case 2027:
			case 2029:
			case 2030:
			return Misc.random(50) + 30;
		}
		return 0;
	}
	
	
	
	/*public void handleGmaulPlayer() {
		if (c.playerIndex > 0) {
			Client o = (Client)Server.playerHandler.players[c.playerIndex];
			if (c.goodDistance(c.getX(), c.getY(), o.getX(), o.getY(), getRequiredDistance())) {
				if (checkReqs()) {
					if (checkSpecAmount(4153)) {						
						boolean hit = Misc.random(calculateMeleeAttack()) > Misc.random(o.getCombat().calculateMeleeDefence());
						int damage = 0;
						if (hit)
							damage = Misc.random(calculateMeleeMaxHit());
						if (protMelee(o))
							damage *= .6;
						o.handleHitMask(damage);
						c.startAnimation(1667);
						c.gfx100(337);
						o.dealDamage(damage);
					}	
				}	
			}			
		}	
	}*/
	
	public void handleGmaulPlayer() {
		if (c.playerIndex > 0) {
			Client o = (Client)PlayerHandler.players[c.playerIndex];
			if (c.goodDistance(c.getX(), c.getY(), o.getX(), o.getY(), c.getCombat().getRequiredDistance())) {
 				if (c.getCombat().checkReqs()) {
					if (c.getCombat().checkSpecAmount(4153)) {						
 						boolean hit = Misc.random(c.getCombat().calculateMeleeAttack()) > Misc.random(o.getCombat().calculateMeleeDefence());
						int damage = 0;
						if (hit)
							damage = Misc.random(c.getCombat().calculateMeleeMaxHit());
						if (protMelee(o))
							damage *= .6;
						if(o.playerLevel[3] - damage <= 0) {
							damage = o.playerLevel[3];
						}
						damage *= 1.25;
						if(o.playerLevel[3] > 0) {
							o.handleHitMask(damage);
							c.startAnimation(1667);
							o.gfx100(337);
							o.dealDamage(damage);
						}
					}	
				}	
			}			
		} else if(c.npcIndex > 0) {
			int x = NPCHandler.npcs[c.npcIndex].absX;
			int y = NPCHandler.npcs[c.npcIndex].absY;
			if (c.goodDistance(c.getX(), c.getY(), x, y, 2)) {
				if (c.getCombat().checkReqs()) {
					if (c.getCombat().checkSpecAmount(4153)) {
						int damage = Misc.random(c.getCombat().calculateMeleeMaxHit());
						if(NPCHandler.npcs[c.npcIndex].HP - damage < 0) {
							damage = NPCHandler.npcs[c.npcIndex].HP;
						}
						if(NPCHandler.npcs[c.npcIndex].HP > 0) {
							NPCHandler.npcs[c.npcIndex].HP -= damage;
							NPCHandler.npcs[c.npcIndex].handleHitMask(damage);
							c.startAnimation(1667);
							c.gfx100(337);
						}
					}
				}
			}
		}
	}
	
	public boolean armaNpc(int i) {
		switch (NPCHandler.npcs[i].npcType) {
		case 6229:
		case 6230:
		case 6231:
		case 6232:
		case 6233:
		case 6234:
		case 6235:
		case 6236:
		case 6237:
		case 6238:
		case 6239:
		case 6240:
		case 6241:
		case 6242:
		case 6243:
		case 6244:
		case 6245:
		case 6246:
			return true;
		}
		return false;
	}

	private static class RangedAmmoData {
		final int startGfx;
		final int projectileGfx;
		final int speed;

		RangedAmmoData(int startGfx, int projectileGfx, int speed) {
			this.startGfx = startGfx;
			this.projectileGfx = projectileGfx;
			this.speed = speed;
		}
	}

	private static final java.util.Map<Integer, RangedAmmoData> RANGED_AMMO = new java.util.HashMap<>();

	static {
		// Knives
		RANGED_AMMO.put(863, new RangedAmmoData(220, 213, 70));
		RANGED_AMMO.put(871, new RangedAmmoData(220, 213, 70));
		RANGED_AMMO.put(5655, new RangedAmmoData(220, 213, 70));
		RANGED_AMMO.put(5662, new RangedAmmoData(220, 213, 70));
		RANGED_AMMO.put(864, new RangedAmmoData(219, 212, 70));
		RANGED_AMMO.put(870, new RangedAmmoData(219, 212, 70));
		RANGED_AMMO.put(5654, new RangedAmmoData(219, 212, 70));
		RANGED_AMMO.put(5661, new RangedAmmoData(219, 212, 70));
		RANGED_AMMO.put(865, new RangedAmmoData(221, 214, 70));
		RANGED_AMMO.put(872, new RangedAmmoData(221, 214, 70));
		RANGED_AMMO.put(5656, new RangedAmmoData(221, 214, 70));
		RANGED_AMMO.put(5663, new RangedAmmoData(221, 214, 70));
		RANGED_AMMO.put(866, new RangedAmmoData(222, 216, 70));
		RANGED_AMMO.put(873, new RangedAmmoData(222, 216, 70));
		RANGED_AMMO.put(5657, new RangedAmmoData(222, 216, 70));
		RANGED_AMMO.put(5664, new RangedAmmoData(222, 216, 70));
		RANGED_AMMO.put(867, new RangedAmmoData(223, 217, 70));
		RANGED_AMMO.put(875, new RangedAmmoData(223, 217, 70));
		RANGED_AMMO.put(5659, new RangedAmmoData(223, 217, 70));
		RANGED_AMMO.put(5666, new RangedAmmoData(223, 217, 70));
		RANGED_AMMO.put(868, new RangedAmmoData(224, 218, 70));
		RANGED_AMMO.put(876, new RangedAmmoData(224, 218, 70));
		RANGED_AMMO.put(5660, new RangedAmmoData(224, 218, 70));
		RANGED_AMMO.put(5667, new RangedAmmoData(224, 218, 70));
		RANGED_AMMO.put(869, new RangedAmmoData(222, 215, 70));
		RANGED_AMMO.put(874, new RangedAmmoData(222, 215, 70));
		RANGED_AMMO.put(5658, new RangedAmmoData(222, 215, 70));
		RANGED_AMMO.put(5665, new RangedAmmoData(222, 215, 70));

		// Darts
		RANGED_AMMO.put(806, new RangedAmmoData(232, 226, 70));
		RANGED_AMMO.put(812, new RangedAmmoData(232, 226, 70));
		RANGED_AMMO.put(5628, new RangedAmmoData(232, 226, 70));
		RANGED_AMMO.put(5635, new RangedAmmoData(232, 226, 70));
		RANGED_AMMO.put(807, new RangedAmmoData(233, 227, 70));
		RANGED_AMMO.put(813, new RangedAmmoData(233, 227, 70));
		RANGED_AMMO.put(5629, new RangedAmmoData(233, 227, 70));
		RANGED_AMMO.put(5636, new RangedAmmoData(233, 227, 70));
		RANGED_AMMO.put(808, new RangedAmmoData(234, 228, 70));
		RANGED_AMMO.put(814, new RangedAmmoData(234, 228, 70));
		RANGED_AMMO.put(5630, new RangedAmmoData(234, 228, 70));
		RANGED_AMMO.put(5637, new RangedAmmoData(234, 228, 70));
		RANGED_AMMO.put(809, new RangedAmmoData(235, 229, 70));
		RANGED_AMMO.put(815, new RangedAmmoData(235, 229, 70));
		RANGED_AMMO.put(5632, new RangedAmmoData(235, 229, 70));
		RANGED_AMMO.put(5639, new RangedAmmoData(235, 229, 70));
		RANGED_AMMO.put(810, new RangedAmmoData(236, 230, 70));
		RANGED_AMMO.put(816, new RangedAmmoData(236, 230, 70));
		RANGED_AMMO.put(5633, new RangedAmmoData(236, 230, 70));
		RANGED_AMMO.put(5640, new RangedAmmoData(236, 230, 70));
		RANGED_AMMO.put(811, new RangedAmmoData(237, 231, 70));
		RANGED_AMMO.put(817, new RangedAmmoData(237, 231, 70));
		RANGED_AMMO.put(5641, new RangedAmmoData(237, 231, 70));
		RANGED_AMMO.put(5634, new RangedAmmoData(237, 231, 70));

		// Javelins
		RANGED_AMMO.put(825, new RangedAmmoData(206, 200, 70));
		RANGED_AMMO.put(831, new RangedAmmoData(206, 200, 70));
		RANGED_AMMO.put(5642, new RangedAmmoData(206, 200, 70));
		RANGED_AMMO.put(5648, new RangedAmmoData(206, 200, 70));
		RANGED_AMMO.put(826, new RangedAmmoData(207, 201, 70));
		RANGED_AMMO.put(832, new RangedAmmoData(207, 201, 70));
		RANGED_AMMO.put(5643, new RangedAmmoData(207, 201, 70));
		RANGED_AMMO.put(5649, new RangedAmmoData(207, 201, 70));
		RANGED_AMMO.put(827, new RangedAmmoData(208, 202, 70));
		RANGED_AMMO.put(833, new RangedAmmoData(208, 202, 70));
		RANGED_AMMO.put(5644, new RangedAmmoData(208, 202, 70));
		RANGED_AMMO.put(5650, new RangedAmmoData(208, 202, 70));
		RANGED_AMMO.put(828, new RangedAmmoData(209, 203, 70));
		RANGED_AMMO.put(834, new RangedAmmoData(209, 203, 70));
		RANGED_AMMO.put(5645, new RangedAmmoData(209, 203, 70));
		RANGED_AMMO.put(5651, new RangedAmmoData(209, 203, 70));
		RANGED_AMMO.put(829, new RangedAmmoData(210, 204, 70));
		RANGED_AMMO.put(835, new RangedAmmoData(210, 204, 70));
		RANGED_AMMO.put(5646, new RangedAmmoData(210, 204, 70));
		RANGED_AMMO.put(5652, new RangedAmmoData(210, 204, 70));
		RANGED_AMMO.put(830, new RangedAmmoData(211, 205, 70));
		RANGED_AMMO.put(836, new RangedAmmoData(211, 205, 70));
		RANGED_AMMO.put(5647, new RangedAmmoData(211, 205, 70));
		RANGED_AMMO.put(5653, new RangedAmmoData(211, 205, 70));

		// Thrownaxes
		RANGED_AMMO.put(800, new RangedAmmoData(42, 36, 70));
		RANGED_AMMO.put(801, new RangedAmmoData(43, 35, 70));
		RANGED_AMMO.put(802, new RangedAmmoData(44, 37, 70));
		RANGED_AMMO.put(803, new RangedAmmoData(45, 38, 70));
		RANGED_AMMO.put(804, new RangedAmmoData(46, 39, 70));
		RANGED_AMMO.put(805, new RangedAmmoData(48, 40, 70));

		// Arrows
		RANGED_AMMO.put(882, new RangedAmmoData(19, 10, 70));
		RANGED_AMMO.put(883, new RangedAmmoData(19, 10, 70));
		RANGED_AMMO.put(5616, new RangedAmmoData(19, 10, 70));
		RANGED_AMMO.put(5622, new RangedAmmoData(19, 10, 70));
		RANGED_AMMO.put(884, new RangedAmmoData(18, 9, 70));
		RANGED_AMMO.put(885, new RangedAmmoData(18, 9, 70));
		RANGED_AMMO.put(5617, new RangedAmmoData(18, 9, 70));
		RANGED_AMMO.put(5623, new RangedAmmoData(18, 9, 70));
		RANGED_AMMO.put(886, new RangedAmmoData(20, 11, 70));
		RANGED_AMMO.put(887, new RangedAmmoData(20, 11, 70));
		RANGED_AMMO.put(5618, new RangedAmmoData(20, 11, 70));
		RANGED_AMMO.put(5624, new RangedAmmoData(20, 11, 70));
		RANGED_AMMO.put(4160, new RangedAmmoData(20, 11, 70));
		RANGED_AMMO.put(888, new RangedAmmoData(21, 12, 70));
		RANGED_AMMO.put(889, new RangedAmmoData(21, 12, 70));
		RANGED_AMMO.put(5619, new RangedAmmoData(21, 12, 70));
		RANGED_AMMO.put(5625, new RangedAmmoData(21, 12, 70));
		RANGED_AMMO.put(890, new RangedAmmoData(22, 13, 70));
		RANGED_AMMO.put(891, new RangedAmmoData(22, 13, 70));
		RANGED_AMMO.put(5620, new RangedAmmoData(22, 13, 70));
		RANGED_AMMO.put(5626, new RangedAmmoData(22, 13, 70));
		RANGED_AMMO.put(892, new RangedAmmoData(24, 15, 70));
		RANGED_AMMO.put(893, new RangedAmmoData(24, 15, 70));
		RANGED_AMMO.put(5621, new RangedAmmoData(24, 15, 70));
		RANGED_AMMO.put(5627, new RangedAmmoData(24, 15, 70));
		RANGED_AMMO.put(11212, new RangedAmmoData(26, 17, 70));

		// Crystal bow
		RANGED_AMMO.put(4212, new RangedAmmoData(250, 249, 70));
		RANGED_AMMO.put(4214, new RangedAmmoData(250, 249, 70));
		RANGED_AMMO.put(4215, new RangedAmmoData(250, 249, 70));
		RANGED_AMMO.put(4216, new RangedAmmoData(250, 249, 70));
		RANGED_AMMO.put(4217, new RangedAmmoData(250, 249, 70));
		RANGED_AMMO.put(4218, new RangedAmmoData(250, 249, 70));
		RANGED_AMMO.put(4219, new RangedAmmoData(250, 249, 70));
		RANGED_AMMO.put(4220, new RangedAmmoData(250, 249, 70));
		RANGED_AMMO.put(4221, new RangedAmmoData(250, 249, 70));
		RANGED_AMMO.put(4222, new RangedAmmoData(250, 249, 70));
		RANGED_AMMO.put(4223, new RangedAmmoData(250, 249, 70));
	}

	private RangedAmmoData getRangedAmmoData(int itemId) {
		return RANGED_AMMO.get(itemId);
	}

}