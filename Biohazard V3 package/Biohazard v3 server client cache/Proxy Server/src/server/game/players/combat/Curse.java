package server.game.players.combat;

import server.Config;
import server.game.npcs.NPC;
import server.game.npcs.NPCHandler;
import server.game.npcs.Nex;
import server.game.players.Client;
import server.game.players.PlayerHandler;

public class Curse {

	private Client c;

	public Curse(Client c) {
		this.c = c;
	}

	public void resetCurse() {
		for (int p = 0; p < c.curseActive.length; p++) {
			c.curseActive[p] = false;
			c.getPA().sendFrame36(c.CURSE_GLOW[p], 0);
		}
		c.headIcon = -1;
		c.getatt = 0;
		c.getstr = 0;
		c.getdef = 0;
		c.getPA().requestUpdates();
	}

	public void strCurse(int i) {
		for (int j = 0; j < str.length; j++) {
			if (str[j] != i) {
				c.curseActive[str[j]] = false;
				c.getPA().sendFrame36(c.CURSE_GLOW[str[j]], 0);
			}
		}
	}

	public void atkCurse(int i) {
		for (int j = 0; j < atk.length; j++) {
			if (atk[j] != i) {
				c.curseActive[atk[j]] = false;
				c.getPA().sendFrame36(c.CURSE_GLOW[atk[j]], 0);
			}
		}
	}

	public void defCurse(int i) {
		for (int j = 0; j < def.length; j++) {
			if (def[j] != i) {
				c.curseActive[def[j]] = false;
				c.getPA().sendFrame36(c.CURSE_GLOW[def[j]], 0);
			}
		}
	}

	public void rngCurse(int i) {
		for (int j = 0; j < rng.length; j++) {
			if (rng[j] != i) {
				c.curseActive[rng[j]] = false;
				c.getPA().sendFrame36(c.CURSE_GLOW[rng[j]], 0);
			}
		}
	}

	public void mgeCurse(int i) {
		for (int j = 0; j < mge.length; j++) {
			if (mge[j] != i) {
				c.curseActive[mge[j]] = false;
				c.getPA().sendFrame36(c.CURSE_GLOW[mge[j]], 0);
			}
		}
	}

	public void sprtCurse(int i) {
		for (int j = 0; j < sprt.length; j++) {
			if (sprt[j] != i) {
				c.curseActive[sprt[j]] = false;
				c.getPA().sendFrame36(c.CURSE_GLOW[sprt[j]], 0);
			}
		}
	}

	public int[] def = { 13, 19 };
	public int[] str = { 14, 19 };
	public int[] atk = { 1, 10, 19 };
	public int[] rng = { 2, 11, 19 };
	public int[] mge = { 3, 12, 19 };
	public int[] sprt = { 4, 16 };

	public void activateCurse(int i) {
		if (c.altarPrayed != 1) {
			if (i >= 0 && i < c.CURSE_GLOW.length) {
				c.getPA().sendFrame36(c.CURSE_GLOW[i], 0);
			}
			return;
		}
		if (c.duelRule[7]) {
			resetCurse();
			c.sendMessage("Prayer has been disabled in this duel!");
			return;
		}
		if (c.playerLevel[1] < 30) {
			c.getPA().sendFrame36(c.CURSE_GLOW[i], 0);
			c.sendMessage("You need 30 Defence to use this prayer.");
			return;
		}
		if (c.playerLevel[5] > 0 || !Config.PRAYER_POINTS_REQUIRED) {
			if (c.getPA().getLevelForXP(c.playerXP[5]) >= c.CURSE_LEVEL_REQUIRED[i]
					|| !Config.PRAYER_LEVEL_REQUIRED) {
				boolean headIcon = false;
				switch (i) {
				case 0: // Protect Item
					if (!c.curseActive[i]) {
						c.startAnimation(12567);
						c.gfx0(2213);
						c.prayerActive[10] = true;
						c.lastProtItem = System.currentTimeMillis();
					} else {
						c.prayerActive[10] = false;
					}
					break;
				case 1:
				case 10:
					if (!c.curseActive[i]) {
						atkCurse(i);
					}
					break;
				case 2:
				case 11:
					if (!c.curseActive[i]) {
						rngCurse(i);
					}
					break;
				case 3:
				case 12:
					if (!c.curseActive[i]) {
						mgeCurse(i);
					}
					break;
				case 4:
				case 16:
					if (!c.curseActive[i]) {
						sprtCurse(i);
					}
					break;
				case 5: // Berserker
					if (!c.curseActive[i]) {
						c.startAnimation(12589);
						c.gfx0(2266);
					}
					break;
				case 13:
					if (!c.curseActive[i]) {
						defCurse(i);
					}
					break;
				case 14:
					if (!c.curseActive[i]) {
						strCurse(i);
					}
					break;
				case 6:
				case 7:
				case 8:
				case 9:
					if (System.currentTimeMillis() - c.stopPrayerDelay < 5000) {
						c.sendMessage("You have been injured and can't use this prayer!");
						c.getPA().sendFrame36(c.CURSE_GLOW[7], 0);
						c.getPA().sendFrame36(c.CURSE_GLOW[8], 0);
						c.getPA().sendFrame36(c.CURSE_GLOW[9], 0);
						return;
					}
					if (i == 7)
						c.protMageDelay = System.currentTimeMillis();
					else if (i == 8)
						c.protRangeDelay = System.currentTimeMillis();
					else if (i == 9)
						c.protMeleeDelay = System.currentTimeMillis();
				case 17:
				case 18:
					headIcon = true;
					for (int p = 6; p < 19; p++) {
						if (i != p && p != 10 && p != 11 && p != 12 && p != 13
								&& p != 14 && p != 15 && p != 16) {
							c.curseActive[p] = false;
							c.getPA().sendFrame36(c.CURSE_GLOW[p], 0);
						}
					}
					break;
				case 19: // Turmoil
					if (!c.curseActive[i]) {
						c.startAnimation(12565);
						c.gfx0(2226);
						strCurse(i);
						atkCurse(i);
						defCurse(i);
						mgeCurse(i);
						rngCurse(i);
					}
					break;
				}
				if (!headIcon) {
					if (!c.curseActive[i]) {
						c.curseActive[i] = true;
						c.getPA().sendFrame36(c.CURSE_GLOW[i], 1);
					} else {
						c.curseActive[i] = false;
						c.getPA().sendFrame36(c.CURSE_GLOW[i], 0);
						if (i == 19) {
							c.getatt = 0;
							c.getstr = 0;
							c.getdef = 0;
						}
					}
				} else {
					if (!c.curseActive[i]) {
						c.curseActive[i] = true;
						c.getPA().sendFrame36(c.CURSE_GLOW[i], 1);
						c.headIcon = c.CURSE_HEAD_ICONS[i];
						c.getPA().requestUpdates();
					} else {
						c.curseActive[i] = false;
						c.getPA().sendFrame36(c.CURSE_GLOW[i], 0);
						c.headIcon = -1;
						c.getPA().requestUpdates();
					}
				}
			} else {
				c.getPA().sendFrame36(c.CURSE_GLOW[i], 0);
				c.getPA().sendFrame126(
						"You need a @blu@Prayer level of "
								+ c.CURSE_LEVEL_REQUIRED[i] + " to use "
								+ c.CURSE_NAME[i] + ".", 357);
				c.getPA().sendFrame126("Click here to continue", 358);
				c.getPA().sendFrame164(356);
			}
		} else {
			c.getPA().sendFrame36(c.CURSE_GLOW[i], 0);
			c.sendMessage("You have run out of prayer points!");
		}
	}

	public void applyHitEffects(int damage, Client victim) {
		if (damage <= 0) {
			return;
		}
		if (c.curseActive[18]) {
			if (victim != null) {
				soulSplitPlayer(victim.playerId, damage);
			} else if (c.oldNpcIndex > 0) {
				soulSplitNpc(c.oldNpcIndex, damage);
			}
		}
		if (victim != null && !victim.disconnected) {
			if (c.curseActive[19]) {
				c.getatt = victim.playerLevel[0] * 15 / 100;
				c.getstr = victim.playerLevel[2] * 10 / 100;
				c.getdef = victim.playerLevel[1] * 15 / 100;
			}
			sapAndLeech(victim);
		}
	}

	/** Deflect Magic / Missiles / Melee reaction: anim 12573 + style gfx. */
	public void playDeflect() {
		int gfx = -1;
		if (c.curseActive[7]) {
			gfx = 2228; // Deflect Magic
		} else if (c.curseActive[8]) {
			gfx = 2229; // Deflect Missiles
		} else if (c.curseActive[9]) {
			gfx = 2230; // Deflect Melee
		}
		if (gfx == -1) {
			return;
		}
		c.startAnimation(12573);
		c.gfx0(gfx);
	}

	/** Soul Split cloud: player -> target (2263), then return + heal. */
	public void soulSplitPlayer(int index, int damage) {
		if (damage <= 0 || !c.curseActive[18] || c.ssDelay > 0) {
			return;
		}
		Client target = (Client) PlayerHandler.players[index];
		if (target == null) {
			return;
		}
		int offX = (c.getX() - target.getX()) * -1;
		int offY = (c.getY() - target.getY()) * -1;
		c.getPA().createPlayersProjectile(c.getX(), c.getY(), offX, offY, 50, 75,
				2263, 25, 25, -index - 1, 0);
		c.ssTarget = index;
		c.ssTargetNpc = 0;
		c.ssHeal = Math.max(1, damage / 5);
		c.ssDelay = 4;
	}

	public void soulSplitNpc(int index, int damage) {
		if (damage <= 0 || !c.curseActive[18] || c.ssDelay > 0) {
			return;
		}
		NPC n = NPCHandler.npcs[index];
		if (n == null) {
			return;
		}
		int offX = (c.getX() - n.absX) * -1;
		int offY = (c.getY() - n.absY) * -1;
		c.getPA().createPlayersProjectile(c.getX(), c.getY(), offX, offY, 50, 75,
				2263, 25, 25, index + 1, 0);
		c.ssTarget = 0;
		c.ssTargetNpc = index;
		c.ssHeal = Math.max(1, damage / 5);
		c.ssDelay = 4;
	}

	public void handleProcess() {
		if (c.ssDelay > 0) {
			c.ssDelay--;
		}
		if (c.ssDelay == 3) {
			if (c.ssTarget > 0 && PlayerHandler.players[c.ssTarget] != null) {
				Client target = (Client) PlayerHandler.players[c.ssTarget];
				// Same axis convention as outbound: off = destination - start.
				int offX = (target.getX() - c.getX()) * -1;
				int offY = (target.getY() - c.getY()) * -1;
				c.getPA().createPlayersProjectile(target.getX(), target.getY(),
						offX, offY, 50, 75, 2263, 25, 25, -c.playerId - 1, 40);
				target.gfx0(2264);
			} else if (c.ssTargetNpc > 0 && NPCHandler.npcs[c.ssTargetNpc] != null) {
				NPC n = NPCHandler.npcs[c.ssTargetNpc];
				int offX = (n.absX - c.getX()) * -1;
				int offY = (n.absY - c.getY()) * -1;
				c.getPA().createPlayersProjectile(n.absX, n.absY, offX, offY, 50,
						75, 2263, 25, 25, -c.playerId - 1, 40);
				n.gfx0(2264);
			}
			heal(c.ssHeal);
			c.ssHeal = 0;
			c.ssTarget = 0;
			c.ssTargetNpc = 0;
		}
	}

	private void sapAndLeech(Client o) {
		if (core.util.Misc.random(7) != 0) {
			return;
		}
		if (c.curseActive[1]) {
			drainLevel(o, 0, 2);
			drainLevel(o, 1, 2);
			drainLevel(o, 2, 2);
		}
		if (c.curseActive[10]) {
			drainLevel(o, 0, 2);
			boostLevel(0, 1);
		}
		if (c.curseActive[2]) {
			drainLevel(o, 4, 2);
			drainLevel(o, 1, 2);
		}
		if (c.curseActive[11]) {
			drainLevel(o, 4, 2);
			boostLevel(4, 1);
		}
		if (c.curseActive[3]) {
			drainLevel(o, 6, 2);
			drainLevel(o, 1, 2);
		}
		if (c.curseActive[12]) {
			drainLevel(o, 6, 2);
			boostLevel(6, 1);
		}
		if (c.curseActive[13]) {
			drainLevel(o, 1, 2);
			boostLevel(1, 1);
		}
		if (c.curseActive[14]) {
			drainLevel(o, 2, 2);
			boostLevel(2, 1);
		}
		if (c.curseActive[4] || c.curseActive[16]) {
			if (o.specAmount > 0) {
				o.specAmount -= 10;
				if (o.specAmount < 0) {
					o.specAmount = 0;
				}
				o.getItems().updateSpecialBar();
			}
			if (c.curseActive[16] && c.specAmount < 100) {
				c.specAmount += 10;
				if (c.specAmount > 100) {
					c.specAmount = 100;
				}
				c.getItems().updateSpecialBar();
			}
		}
		if (c.curseActive[15]) {
			if (o.playerEnergy > 0) {
				o.playerEnergy -= 10;
				if (o.playerEnergy < 0) {
					o.playerEnergy = 0;
				}
				o.getPA().sendFrame126(o.playerEnergy + "%", 149);
			}
			if (c.playerEnergy < 100) {
				c.playerEnergy += 10;
				if (c.playerEnergy > 100) {
					c.playerEnergy = 100;
				}
				c.getPA().sendFrame126(c.playerEnergy + "%", 149);
			}
		}
	}

	private void drainLevel(Client o, int skill, int amount) {
		if (o.playerLevel[skill] - amount < 1) {
			o.playerLevel[skill] = 1;
		} else {
			o.playerLevel[skill] -= amount;
		}
		o.getPA().refreshSkill(skill);
	}

	private void boostLevel(int skill, int amount) {
		int max = c.getPA().getLevelForXP(c.playerXP[skill]) + 5;
		if (c.playerLevel[skill] + amount > max) {
			c.playerLevel[skill] = max;
		} else {
			c.playerLevel[skill] += amount;
		}
		c.getPA().refreshSkill(skill);
	}

	private void heal(int amount) {
		if (amount <= 0) {
			return;
		}
		int max = c.getPA().getLevelForXP(c.playerXP[3]);
		if (c.playerLevel[3] + amount > max) {
			c.playerLevel[3] = max;
		} else {
			c.playerLevel[3] += amount;
		}
		c.getPA().refreshSkill(3);
	}

	public void applyWrath() {
		if (!c.curseActive[17]) {
			return;
		}
		c.gfx0(2259);
		// 25% of Prayer level, 5×5 Chebyshev area centred on the player
		int hit = c.getPA().getLevelForXP(c.playerXP[5]) * 25 / 100;
		if (hit < 1) {
			hit = 1;
		}
		for (int i = 0; i < PlayerHandler.players.length; i++) {
			Client o = (Client) PlayerHandler.players[i];
			if (o == null || o == c || o.isDead || o.respawnTimer > 0) {
				continue;
			}
			if (o.heightLevel != c.heightLevel) {
				continue;
			}
			int dx = Math.abs(o.absX - c.absX);
			int dy = Math.abs(o.absY - c.absY);
			if (dx > 2 || dy > 2) {
				continue;
			}
			int dmg = hit;
			if (o.playerLevel[3] - dmg < 0) {
				dmg = o.playerLevel[3];
			}
			if (dmg <= 0) {
				continue;
			}
			o.dealDamage(dmg);
			o.handleHitMask(dmg);
			o.getPA().refreshSkill(3);
			o.updateRequired = true;
		}
		for (int i = 0; i < NPCHandler.npcs.length; i++) {
			NPC n = NPCHandler.npcs[i];
			if (n == null || n.isDead || n.HP <= 0) {
				continue;
			}
			if (n.heightLevel != c.heightLevel) {
				continue;
			}
			int size = 1;
			if (Nex.isNex(n.npcType)) {
				size = 3;
			}
			if (!overlapsNpc5x5(c.absX, c.absY, n.absX, n.absY, size)) {
				continue;
			}
			int dmg = hit;
			if (n.HP - dmg < 0) {
				dmg = n.HP;
			}
			if (dmg <= 0) {
				continue;
			}
			n.HP -= dmg;
			n.hitDiff = dmg;
			n.hitUpdateRequired = true;
			n.updateRequired = true;
		}
	}

	/** Any tile of a size×size NPC inside the player's 5×5 Wrath area. */
	private boolean overlapsNpc5x5(int px, int py, int nx, int ny, int size) {
		if (size < 1) {
			size = 1;
		}
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				int dx = Math.abs((nx + x) - px);
				int dy = Math.abs((ny + y) - py);
				if (dx <= 2 && dy <= 2) {
					return true;
				}
			}
		}
		return false;
	}

}
