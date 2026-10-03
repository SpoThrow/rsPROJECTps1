package server.game.npcs;

import server.Server;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.players.Client;
import server.game.players.PlayerHandler;
import core.util.Misc;

/**
 * Nex (667): Zaros phases, curse Deflect overheads, Soul Split late-fight, death Wrath.
 */
public final class Nex {

	public static final int NEX_MELEE = 13447;
	public static final int NEX_RANGE = 13448;
	public static final int NEX_MAGE = 13449;
	public static final int NEX_SOUL_SPLIT = 13450;
	public static final int NEX_WRATH = 13451;
	public static final int BLOOD_REAVER = 13458;

	private static final int REAVER_COUNT = 4;
	/** Half of Matrix 1010. */
	private static final int REAVER_HP = 168;
	private static final int REAVER_MAX_HIT = 15;
	private static final int REAVER_ATTACK = 60;
	private static final int REAVER_DEFENCE = 60;
	private static final int[][] REAVER_OFFSETS = {
			{ 3, 0 }, { -3, 0 }, { 0, 3 }, { 0, -3 }
	};

	/** Curse styles: 0 Deflect Melee, 1 Missiles, 2 Magic, 3 Soul Split, 4 Wrath. */
	public static final int PRAY_MELEE = 0;
	public static final int PRAY_RANGE = 1;
	public static final int PRAY_MAGE = 2;
	public static final int PRAY_SOUL_SPLIT = 3;
	public static final int PRAY_WRATH = 4;

	public static boolean isNex(int npcType) {
		return npcType >= 13447 && npcType <= 13451;
	}

	public static boolean isBloodReaver(int npcType) {
		return npcType == BLOOD_REAVER;
	}

	public static void loadSpell(NPC n) {
		if (n == null) {
			return;
		}
		int pct = n.MaxHP <= 0 ? 100 : (n.HP * 100) / n.MaxHP;
		int phase = 0;
		if (pct <= 25) {
			phase = 3;
		} else if (pct <= 50) {
			phase = 2;
		} else if (pct <= 75) {
			phase = 1;
		}
		if (n.phaseChange != phase) {
			n.phaseChange = phase;
			if (phase == 0) {
				n.forceChat("Fill my soul with smoke!");
			} else if (phase == 1) {
				n.forceChat("Darken my shadow!");
			} else if (phase == 2) {
				n.forceChat("Flood my lungs with blood!");
			} else {
				n.forceChat("Infuse me with the power of ice!");
			}
		}
		int roll = Misc.random(4);
		if (phase == 0) {
			n.attackType = 2;
			n.projectileId = 386;
			n.endGfx = 387;
			if (roll == 0) {
				n.forceChat("Let the virus flow through you!");
			}
		} else if (phase == 1) {
			n.attackType = 1;
			n.projectileId = 380;
			n.endGfx = 381;
			if (roll == 0) {
				n.forceChat("Fear the shadow!");
			}
		} else if (phase == 2) {
			n.attackType = 0;
			n.projectileId = -1;
			n.endGfx = 373;
			if (roll == 0) {
				n.forceChat("I demand a blood sacrifice!");
				n.HP += 20;
				if (n.HP > n.MaxHP) {
					n.HP = n.MaxHP;
				}
			}
		} else {
			n.attackType = 2;
			n.projectileId = 368;
			// Ice bind gfx only when freeze actually applies (see hitPlayer / tryFreeze)
			n.endGfx = -1;
			if (roll == 0) {
				n.forceChat("Contain this!");
				n.endGfx = 369;
				n.nexIceBind = true;
			}
		}
		if (roll == 1) {
			n.forceChat("There is...NO ESCAPE!");
			n.attackType = 0;
			n.projectileId = -1;
			n.endGfx = 377;
		}
	}

	/**
	 * Slow Deflect switches (~25–50s). ≤40% Soul Split; ≤12% critical → Wrath.
	 */
	public static void tickPrayers(NPC n) {
		if (n == null || n.isDead) {
			return;
		}
		int pct = n.MaxHP <= 0 ? 100 : (n.HP * 100) / n.MaxHP;
		if (pct <= 12) {
			if (n.protectStyle != PRAY_WRATH) {
				setPrayer(n, PRAY_WRATH);
				n.forceChat("Taste my wrath!");
			}
			return;
		}
		if (pct <= 40) {
			if (n.protectStyle != PRAY_SOUL_SPLIT) {
				setPrayer(n, PRAY_SOUL_SPLIT);
				n.forceChat("Your soul is mine!");
			}
			return;
		}
		if (n.prayerSwitchTimer > 0) {
			n.prayerSwitchTimer--;
			return;
		}
		if (n.protectStyle < 0 || n.protectStyle >= PRAY_SOUL_SPLIT) {
			setPrayer(n, Misc.random(2));
		} else {
			int next = Misc.random(2);
			if (next == n.protectStyle) {
				next = (next + 1 + Misc.random(1)) % 3;
			}
			setPrayer(n, next);
		}
		n.prayerSwitchTimer = 42 + Misc.random(42);
	}

	public static void setPrayer(NPC n, int style) {
		if (n == null) {
			return;
		}
		int previous = n.protectStyle;
		n.protectStyle = style;
		int id = NEX_MELEE;
		if (style == PRAY_RANGE) {
			id = NEX_RANGE;
		} else if (style == PRAY_MAGE) {
			id = NEX_MAGE;
		} else if (style == PRAY_SOUL_SPLIT) {
			id = NEX_SOUL_SPLIT;
		} else if (style == PRAY_WRATH) {
			id = NEX_WRATH;
		}
		if (n.npcType != id) {
			n.npcType = id;
			n.transformId = id;
			n.transformUpdateRequired = true;
			n.updateRequired = true;
		}
		if (style == PRAY_SOUL_SPLIT && !n.nexReaversSpawned) {
			spawnBloodReavers(n);
			n.nexReaversSpawned = true;
		}
	}

	/** Spawn Blood reavers around Nex when Soul Split begins (once per fight). */
	public static void spawnBloodReavers(NPC nex) {
		if (nex == null || Server.npcHandler == null) {
			return;
		}
		int baseX = nex.absX + 1;
		int baseY = nex.absY + 1;
		int targetId = nex.killerId > 0 ? nex.killerId : 0;
		for (int i = 0; i < REAVER_COUNT; i++) {
			int ox = REAVER_OFFSETS[i][0];
			int oy = REAVER_OFFSETS[i][1];
			NPC reaver = Server.npcHandler.spawnNpc2(BLOOD_REAVER,
					baseX + ox, baseY + oy, nex.heightLevel, 1,
					REAVER_HP, REAVER_MAX_HIT, REAVER_ATTACK, REAVER_DEFENCE);
			if (reaver == null) {
				continue;
			}
			reaver.nexLink = nex.npcId;
			reaver.attackType = 2;
			reaver.randomWalk = false;
			reaver.walkingHome = false;
			reaver.spawnedBy = 0;
			// Prefer Nex's current target; otherwise pick a nearby player.
			int killer = targetId;
			if (killer <= 0 || PlayerHandler.players[killer] == null) {
				killer = Server.npcHandler.getCloseRandomPlayer(reaver.npcId);
			}
			if (killer > 0 && PlayerHandler.players[killer] != null) {
				reaver.killerId = killer;
				reaver.underAttack = true;
				reaver.facePlayer(killer);
			} else {
				reaver.killerId = 0;
				reaver.underAttack = false;
			}
			reaver.updateRequired = true;
		}
	}

	/** Kill Blood reavers tied to this Nex (leave Soul Split / Nex death). */
	public static void despawnBloodReavers(NPC nex) {
		if (nex == null || NPCHandler.npcs == null) {
			return;
		}
		int link = nex.npcId;
		for (int i = 0; i < NPCHandler.npcs.length; i++) {
			NPC n = NPCHandler.npcs[i];
			if (n == null || !isBloodReaver(n.npcType)) {
				continue;
			}
			if (n.nexLink != link && n.nexLink != nex.npcId) {
				continue;
			}
			killBloodReaver(n);
		}
	}

	/** Immediately remove a Blood reaver (no respawn). */
	public static void killBloodReaver(NPC n) {
		if (n == null) {
			return;
		}
		n.isDead = true;
		n.applyDead = true;
		// needRespawn=true makes Player.withinDistance false so clients drop the ghost.
		n.needRespawn = true;
		n.actionTimer = 0;
		n.HP = 0;
		n.MaxHP = 0;
		n.killerId = 0;
		n.underAttack = false;
		n.nexLink = -1;
		n.updateRequired = true;
		if (NPCHandler.npcs != null) {
			for (int i = 0; i < NPCHandler.npcs.length; i++) {
				if (NPCHandler.npcs[i] == n) {
					NPCHandler.npcs[i] = null;
					break;
				}
			}
		}
	}

	/** Reavers whose linked Nex is gone or dead die as well. */
	public static void tickBloodReaverLinks(NPC reaver) {
		if (reaver == null || !isBloodReaver(reaver.npcType)) {
			return;
		}
		if (reaver.nexLink < 0 || reaver.nexLink >= NPCHandler.npcs.length) {
			killBloodReaver(reaver);
			return;
		}
		NPC nex = NPCHandler.npcs[reaver.nexLink];
		if (nex == null || nex.isDead || nex.HP <= 0 || !isNex(nex.npcType)) {
			killBloodReaver(reaver);
		}
	}

	/** Safety: wipe every Blood reaver (used when a Nex dies). */
	public static void despawnAllBloodReavers() {
		if (NPCHandler.npcs == null) {
			return;
		}
		for (int i = 0; i < NPCHandler.npcs.length; i++) {
			NPC n = NPCHandler.npcs[i];
			if (n != null && isBloodReaver(n.npcType)) {
				killBloodReaver(n);
			}
		}
	}

	/** Heal Nex for damage dealt by a linked Blood reaver. */
	public static void applyReaverHeal(NPC reaver, int damage) {
		if (reaver == null || !isBloodReaver(reaver.npcType) || damage <= 0) {
			return;
		}
		if (reaver.nexLink < 0 || reaver.nexLink >= NPCHandler.npcs.length) {
			return;
		}
		NPC nex = NPCHandler.npcs[reaver.nexLink];
		if (nex == null || nex.isDead || !isNex(nex.npcType)) {
			return;
		}
		int before = nex.HP;
		nex.HP += damage;
		if (nex.HP > nex.MaxHP) {
			nex.HP = nex.MaxHP;
		}
		int healed = nex.HP - before;
		if (healed > 0) {
			nex.handleHealHitMask(healed);
			nex.gfx0(2264);
			nex.updateRequired = true;
		}
	}

	/**
	 * Incoming player damage vs Deflect curses. Style: 0 melee, 1 range, 2 mage.
	 * Soul Split / Wrath never reduce damage.
	 */
	public static int modifyIncomingDamage(NPC n, int damage, int style) {
		if (n == null || !isNex(n.npcType) || damage <= 0) {
			return damage;
		}
		if (n.protectStyle == PRAY_SOUL_SPLIT || n.protectStyle == PRAY_WRATH) {
			return damage;
		}
		if (n.protectStyle >= 0 && n.protectStyle <= 2 && n.protectStyle == style) {
			int reduced = damage / 5;
			return reduced < 1 ? 1 : reduced;
		}
		return damage;
	}

	/**
	 * Death Wrath: 5×5 floor clouds, then damage after an escape window (~3s).
	 * Triggers in Soul Split / Wrath (late) phase.
	 */
	public static void applyDeathWrath(NPC n) {
		if (n == null || !isNex(n.npcType)) {
			return;
		}
		despawnBloodReavers(n);
		boolean late = n.protectStyle == PRAY_SOUL_SPLIT
				|| n.protectStyle == PRAY_WRATH
				|| (n.MaxHP > 0 && (n.HP * 100) / n.MaxHP <= 40);
		if (!late) {
			return;
		}
		// Size-3 Nex: centre of occupied tiles
		final int cx = n.absX + 1;
		final int cy = n.absY + 1;
		final int height = n.heightLevel;
		final int hit = 25 + Misc.random(25);

		Client broadcaster = null;
		for (int i = 0; i < PlayerHandler.players.length; i++) {
			Client o = (Client) PlayerHandler.players[i];
			if (o == null || o.disconnected) {
				continue;
			}
			if (o.position.heightLevel != height) {
				continue;
			}
			if (o.distanceToPoint(cx, cy) > 25) {
				continue;
			}
			broadcaster = o;
			break;
		}
		if (broadcaster != null) {
			// Full 5×5 floor Wrath cloud
			for (int dx = -2; dx <= 2; dx++) {
				for (int dy = -2; dy <= 2; dy++) {
					broadcaster.getPA().createPlayersStillGfx(2259, cx + dx, cy + dy, 0, 0);
				}
			}
		}

		// Escape window before damage (~2s at 600ms ticks)
		CycleEventHandler.addEvent(new Object(), new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				dealWrathDamage(cx, cy, height, hit);
				container.stop();
			}

			@Override
			public void stop() {
			}
		}, 3);
	}

	private static void dealWrathDamage(int x, int y, int height, int hit) {
		for (int i = 0; i < PlayerHandler.players.length; i++) {
			Client o = (Client) PlayerHandler.players[i];
			if (o == null || o.isDead || o.timers.respawnTimer > 0) {
				continue;
			}
			if (o.position.heightLevel != height) {
				continue;
			}
			int dx = Math.abs(o.position.absX - x);
			int dy = Math.abs(o.position.absY - y);
			if (dx > 2 || dy > 2) {
				continue;
			}
			int dmg = hit;
			if (o.skills.playerLevel[3] - dmg < 0) {
				dmg = o.skills.playerLevel[3];
			}
			if (dmg <= 0) {
				continue;
			}
			o.dealDamage(dmg);
			o.handleHitMask(dmg);
			o.getPA().refreshSkill(3);
			o.updateRequired = true;
		}
	}

	/** True if target tile is inside a 5×5 centred on (cx,cy), accounting for source size. */
	public static boolean inArea5x5(int cx, int cy, int size, int tx, int ty) {
		if (size < 1) {
			size = 1;
		}
		int best = 999;
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				int dx = Math.abs((cx + x) - tx);
				int dy = Math.abs((cy + y) - ty);
				int d = dx > dy ? dx : dy;
				if (d < best) {
					best = d;
				}
			}
		}
		return best <= 2;
	}

	public static void hitPlayer(Client c, NPC n) {
		if (c == null || n == null || !isNex(n.npcType)) {
			return;
		}
		if (Misc.random(6) == 0) {
			c.prayers.prayerActive[16] = false;
			c.prayers.prayerActive[17] = false;
			c.prayers.prayerActive[18] = false;
			c.prayers.curseActive[7] = false;
			c.prayers.curseActive[8] = false;
			c.prayers.curseActive[9] = false;
			c.prayers.curseActive[18] = false;
			c.appearance.headIcon = -1;
			c.getPA().requestUpdates();
			c.sendMessage("Nex smashes through your overhead prayers!");
		}
		if (n.phaseChange == 0 && Misc.random(5) == 0) {
			c.getPA().appendPoison(c, 6);
		}
	}

	/** Soul Split: heal Nex for a portion of damage dealt to the player. */
	public static void applySoulSplitHeal(NPC n, int damage) {
		if (n == null || !isNex(n.npcType) || damage <= 0) {
			return;
		}
		if (n.protectStyle != PRAY_SOUL_SPLIT) {
			return;
		}
		int heal = Math.max(1, damage / 5);
		n.HP += heal;
		if (n.HP > n.MaxHP) {
			n.HP = n.MaxHP;
		}
		n.gfx0(2264);
		n.updateRequired = true;
	}

	/** Ice phase bind — real freezeTimer so movement is blocked server-side. */
	public static void tryFreeze(Client c, NPC n) {
		if (c == null || n == null) {
			return;
		}
		boolean iceSpecial = n.nexIceBind;
		boolean iceChance = n.phaseChange == 3 && n.attackType == 2 && Misc.random(3) == 0;
		n.nexIceBind = false;
		if (!iceSpecial && !iceChance) {
			return;
		}
		// Immunity window after a freeze (same idea as ice barrage)
		if (c.timers.freezeTimer > -3) {
			return;
		}
		c.timers.freezeTimer = 25;
		c.frozenBy = 0; // NPC freeze — don't clear via player-distance check
		c.resetWalkingQueue();
		c.newWalkCmdSteps = 0;
		c.gfx0(369);
		c.sendMessage("Nex freezes you in place!");
	}

	private Nex() {
	}
}
