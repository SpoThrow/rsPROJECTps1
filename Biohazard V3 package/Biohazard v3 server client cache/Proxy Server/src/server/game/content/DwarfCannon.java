package server.game.content;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import server.Config;
import server.Server;
import server.clip.region.Region;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.minigames.castlewars.CastleWars;
import server.game.npcs.KalphiteQueen;
import server.game.npcs.NPC;
import server.game.npcs.NPCHandler;
import server.game.objects.Object;
import server.game.objects.Objects;
import server.game.players.Client;
import server.game.players.PathFinder;
import server.game.players.Player;
import server.game.players.PlayerHandler;
import core.util.Misc;

/**
 * Dwarf multicannon: set up from the base, load balls, rotate/fire, pick up.
 */
public class DwarfCannon {

	public static final int ITEM_BASE = 6;
	public static final int ITEM_STAND = 8;
	public static final int ITEM_BARRELS = 10;
	public static final int ITEM_FURNACE = 12;
	public static final int ITEM_BALLS = 2;

	public static final int OBJ_BASE = 7;
	public static final int OBJ_STAND = 8;
	public static final int OBJ_BARRELS = 9;
	public static final int OBJ_CANNON = 6;

	private static final int[] SETUP_ITEMS = { ITEM_BASE, ITEM_STAND, ITEM_BARRELS, ITEM_FURNACE };
	private static final int[] SETUP_OBJECTS = { OBJ_BASE, OBJ_STAND, OBJ_BARRELS, OBJ_CANNON };
	/* Rotation index must match muzzle + firing arc: N, NE, E, SE, S, SW, W, NW */
	private static final int[] ROTATE_ANIMS = { 515, 516, 517, 518, 519, 520, 521, 514 };
	private static final int[] MUZZLE_X = { 0, 1, 1, 1, 0, -1, -1, -1 };
	private static final int[] MUZZLE_Y = { 1, 1, 0, -1, -1, -1, 0, 1 };

	private static final int BUILD_ANIM = 827;
	private static final int PROJECTILE = 53;
	private static final int MAX_BALLS = 30;
	private static final int RANGE = 8;
	private static final int MIN_CANNON_DISTANCE = 5;
	private static final int BUILD_EVENT = 77221;
	private static final int FIRE_EVENT = 77222;
	private static final int OVERLAY_FRAME = 24490;
	private static final long DECAY_WARN = 20L * 60L * 1000L;
	private static final long DECAY_FULL = 25L * 60L * 1000L;

	private static final CopyOnWriteArrayList<DwarfCannon> cannons = new CopyOnWriteArrayList<DwarfCannon>();

	String owner;
	int x;
	int y;
	int z;
	int stage;
	int balls;
	int rotation = 7;
	boolean shooting;
	boolean building;
	boolean decayWarned;
	long placedAt;

	public static boolean isCannonObject(int id) {
		return id == OBJ_CANNON || id == OBJ_BASE || id == OBJ_STAND || id == OBJ_BARRELS;
	}

	public static void setup(Client c) {
		if (c == null) {
			return;
		}
		if (c.duelStatus > 0 || c.inTrade || c.inDuelArena() || c.inFightCaves() || c.inPcGame()) {
			c.sendMessage("You cannot set up a cannon here.");
			return;
		}
		if (forUser(c.playerName) != null) {
			c.sendMessage("You already have a cannon set up.");
			return;
		}
		for (int i = 0; i < SETUP_ITEMS.length; i++) {
			if (!c.getItems().playerHasItem(SETUP_ITEMS[i], 1)) {
				c.sendMessage("You need a cannon base, stand, barrels and furnace to set this up.");
				return;
			}
		}
		int placeX = c.position.absX;
		int placeY = c.position.absY;
		int placeZ = c.position.heightLevel;
		if (!canBuildAt(placeX, placeY, placeZ)) {
			c.sendMessage("There isn't enough room to set up the cannon here.");
			return;
		}
		if (tooCloseToCannon(placeX, placeY, placeZ)) {
			c.sendMessage("You are trying to build too close to another cannon.");
			return;
		}
		final DwarfCannon cannon = new DwarfCannon();
		cannon.owner = c.playerName;
		cannon.x = placeX;
		cannon.y = placeY;
		cannon.z = placeZ;
		cannon.stage = 0;
		cannon.building = true;
		cannon.placedAt = System.currentTimeMillis();
		cannons.add(cannon);
		c.getPA().resetFollow();
		c.resetWalkingQueue();
		c.turnPlayerTo(placeX, placeY + 1);
		CycleEventHandler.addEvent(BUILD_EVENT, c, new CycleEvent() {
			int tick = 0;

			@Override
			public void execute(CycleEventContainer container) {
				Client player = playerByName(cannon.owner);
				if (player == null || player.disconnected) {
					cannon.building = false;
					pickupInternal(player, cannon, true);
					container.stop();
					return;
				}
				if (Math.abs(player.position.absX - cannon.x) > 3 || Math.abs(player.position.absY - cannon.y) > 3
						|| player.position.heightLevel != cannon.z) {
					player.sendMessage("You move too far away from the cannon.");
					cannon.building = false;
					pickupInternal(player, cannon, true);
					container.stop();
					return;
				}
				if (tick % 3 == 0) {
					player.startAnimation(BUILD_ANIM);
					player.turnPlayerTo(cannon.x, cannon.y + 1);
				}
				if (tick % 3 == 1) {
					if (cannon.stage >= SETUP_ITEMS.length) {
						container.stop();
						return;
					}
					if (!player.getItems().playerHasItem(SETUP_ITEMS[cannon.stage], 1)) {
						player.sendMessage("You no longer have the parts to finish this cannon.");
						cannon.building = false;
						pickupInternal(player, cannon, true);
						container.stop();
						return;
					}
					player.getItems().deleteItem(SETUP_ITEMS[cannon.stage], 1);
					spawnStage(cannon, cannon.stage);
					cannon.stage++;
					if (cannon.stage == 1) {
						stepOff(player);
					}
					if (cannon.stage >= SETUP_OBJECTS.length) {
						cannon.building = false;
						cannon.placedAt = System.currentTimeMillis();
						player.sendMessage("You set up the dwarf multicannon.");
						syncOverlay(player, cannon);
						container.stop();
						return;
					}
				}
				tick++;
				if (tick > 20) {
					cannon.building = false;
					container.stop();
				}
			}

			@Override
			public void stop() {
				cannon.building = false;
			}
		}, 1);
	}

	public static void firstClick(Client c, int objectId, int objectX, int objectY) {
		DwarfCannon cannon = forLocation(objectX, objectY, c.position.heightLevel);
		if (cannon == null) {
			return;
		}
		if (!cannon.owner.equalsIgnoreCase(c.playerName)) {
			c.sendMessage("This isn't your cannon.");
			return;
		}
		if (cannon.building) {
			c.sendMessage("The cannon is still being set up.");
			return;
		}
		if (objectId != OBJ_CANNON || cannon.stage < SETUP_OBJECTS.length) {
			pickup(c, objectX, objectY);
			return;
		}
		loadAndFire(c, cannon);
	}

	public static void pickup(Client c, int objectX, int objectY) {
		DwarfCannon cannon = forLocation(objectX, objectY, c.position.heightLevel);
		if (cannon == null) {
			return;
		}
		if (!cannon.owner.equalsIgnoreCase(c.playerName)) {
			c.sendMessage("This isn't your cannon.");
			return;
		}
		if (cannon.building) {
			c.sendMessage("The cannon is still being set up.");
			return;
		}
		pickupInternal(c, cannon, false);
	}

	public static void itemOnCannon(Client c, int itemId, int objectId, int objectX, int objectY) {
		if (itemId != ITEM_BALLS || !isCannonObject(objectId)) {
			return;
		}
		if (Math.abs(c.position.absX - objectX) > 4 || Math.abs(c.position.absY - objectY) > 4) {
			c.sendMessage("You can't reach that.");
			return;
		}
		DwarfCannon cannon = forLocation(objectX, objectY, c.position.heightLevel);
		if (cannon == null) {
			return;
		}
		if (!cannon.owner.equalsIgnoreCase(c.playerName)) {
			c.sendMessage("This isn't your cannon.");
			return;
		}
		if (cannon.stage < SETUP_OBJECTS.length) {
			c.sendMessage("Finish setting up the cannon first.");
			return;
		}
		loadBalls(c, cannon);
	}

	public static void logout(Client c) {
		if (c == null || c.playerName == null) {
			return;
		}
		DwarfCannon cannon = forUser(c.playerName);
		if (cannon != null) {
			pickupInternal(c, cannon, true);
		}
	}

	public static void process() {
		long now = System.currentTimeMillis();
		Iterator<DwarfCannon> it = cannons.iterator();
		while (it.hasNext()) {
			DwarfCannon cannon = it.next();
			if (cannon == null || cannon.building) {
				continue;
			}
			if (!cannon.decayWarned && now - cannon.placedAt > DECAY_WARN) {
				cannon.decayWarned = true;
				Client owner = playerByName(cannon.owner);
				if (owner != null) {
					owner.sendMessage("Your cannon is about to decay. Pick it up soon!");
				}
			} else if (now - cannon.placedAt > DECAY_FULL) {
				Client owner = playerByName(cannon.owner);
				if (owner != null) {
					owner.sendMessage("Your cannon has decayed.");
					pickupInternal(owner, cannon, true);
				} else {
					removeObject(cannon);
					cannons.remove(cannon);
				}
			}
		}
	}

	private static void loadAndFire(Client c, DwarfCannon cannon) {
		boolean loaded = loadBalls(c, cannon);
		if (cannon.balls <= 0) {
			if (!loaded) {
				c.sendMessage("You need some cannonballs to fire the cannon.");
			}
			return;
		}
		if (cannon.shooting) {
			return;
		}
		cannon.shooting = true;
		c.sendMessage("You fire the dwarf multicannon.");
		CycleEventHandler.addEvent(FIRE_EVENT, c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				Client player = playerByName(cannon.owner);
				if (player == null || !cannons.contains(cannon)) {
					cannon.shooting = false;
					container.stop();
					return;
				}
				if (cannon.balls <= 0) {
					player.sendMessage("Your cannon has run out of cannonballs.");
					cannon.shooting = false;
					container.stop();
					return;
				}
				rotate(cannon);
				fire(player, cannon);
				syncOverlay(player, cannon);
			}

			@Override
			public void stop() {
				cannon.shooting = false;
			}
		}, 1);
	}

	private static boolean loadBalls(Client c, DwarfCannon cannon) {
		int space = MAX_BALLS - cannon.balls;
		if (space <= 0) {
			c.sendMessage("Your cannon is already full of cannonballs.");
			return false;
		}
		int have = c.getItems().getItemAmount(ITEM_BALLS);
		if (have <= 0) {
			return false;
		}
		int add = have < space ? have : space;
		c.getItems().deleteItem(ITEM_BALLS, add);
		cannon.balls += add;
		c.sendMessage("You load " + add + " cannonball" + (add == 1 ? "" : "s") + ". Cannonballs: " + cannon.balls + ".");
		syncOverlay(c, cannon);
		return true;
	}

	private static void rotate(DwarfCannon cannon) {
		cannon.rotation = (cannon.rotation + 1) & 7;
		Client nearby = nearbyPlayer(cannon.x, cannon.y, cannon.z);
		if (nearby != null) {
			nearby.getPA().objectAnim(cannon.x, cannon.y, ROTATE_ANIMS[cannon.rotation], 10, 0);
		}
	}

	private static void fire(Client c, DwarfCannon cannon) {
		NPC ignore = null;
		int shots = 0;
		int maxShots = lockedNpc(c) > 0 ? 1 : 2;
		while (shots < maxShots && cannon.balls > 0) {
			NPC target = findTarget(c, cannon, ignore);
			if (target == null) {
				return;
			}
			shoot(c, cannon, target);
			ignore = target;
			shots++;
		}
	}

	private static void shoot(Client c, DwarfCannon cannon, NPC target) {
		cannon.balls--;
		int cx = cannon.x + 1 + MUZZLE_X[cannon.rotation];
		int cy = cannon.y + 1 + MUZZLE_Y[cannon.rotation];
		int offX = (cy - target.absY) * -1;
		int offY = (cx - target.absX) * -1;
		int lockon = targetIndex(target) + 1;
		c.getPA().createPlayersProjectile(cx, cy, offX, offY, 50, 50, PROJECTILE, 35, 20, lockon, 25);
		final NPC hitNpc = target;
		final int damage = rollDamage(c, target);
		CycleEventHandler.addEvent(c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				applyHit(c, hitNpc, damage);
				container.stop();
			}

			@Override
			public void stop() {
			}
		}, 1);
	}

	private static int targetIndex(NPC target) {
		if (target == null) {
			return -1;
		}
		for (int i = 0; i < NPCHandler.maxNPCs; i++) {
			if (NPCHandler.npcs[i] == target) {
				return i;
			}
		}
		return -1;
	}

	private static NPC findTarget(Client c, DwarfCannon cannon, NPC ignore) {
		NPC best = null;
		int bestDist = RANGE + 1;
		int cx = cannon.x + 1;
		int cy = cannon.y + 1;
		for (int i = 0; i < NPCHandler.maxNPCs; i++) {
			NPC n = NPCHandler.npcs[i];
			if (n == ignore) {
				continue;
			}
			if (!validTarget(c, n, cannon.z)) {
				continue;
			}
			if (!inFiringArc(cannon.rotation, cx, cy, n.absX, n.absY)) {
				continue;
			}
			int dist = Math.max(Math.abs(n.absX - cx), Math.abs(n.absY - cy));
			if (dist > RANGE || dist >= bestDist) {
				continue;
			}
			if (!PathFinder.hasLineOfSight(cx, cy, 1, n.absX, n.absY, 1, cannon.z)) {
				continue;
			}
			best = n;
			bestDist = dist;
		}
		return best;
	}

	private static boolean validTarget(Client c, NPC n, int height) {
		if (n == null || n.isDead || n.HP <= 0 || n.MaxHP <= 0) {
			return false;
		}
		if (n.heightLevel != height) {
			return false;
		}
		if (n.worldAdventurer) {
			return false;
		}
		if (n.spawnedBy > 0 && n.spawnedBy != c.playerId) {
			return false;
		}
		if (n.underAttackBy > 0 && n.underAttackBy != c.playerId && !n.inMulti() && !c.inMulti()) {
			return false;
		}
		int locked = lockedNpc(c);
		if (locked > 0 && n.npcId != locked) {
			return false;
		}
		if (c.getTT() != null && c.getTT().clueNpc(n.npcType)) {
			return false;
		}
		if (n.npcType == 1532 && n.team == CastleWars.getTeamNumber(c)) {
			return false;
		}
		if (KalphiteQueen.KQnpc(n.npcId) && !KalphiteQueen.fullVerac(c)) {
			return false;
		}
		return true;
	}

	private static int lockedNpc(Client c) {
		if (c == null || c.inMulti()) {
			return -1;
		}
		if (c.targeting.npcIndex > 0 && c.targeting.npcIndex < NPCHandler.maxNPCs) {
			NPC n = NPCHandler.npcs[c.targeting.npcIndex];
			if (n != null && !n.isDead && n.HP > 0) {
				return c.targeting.npcIndex;
			}
		}
		if (c.targeting.underAttackBy2 > 0 && c.targeting.underAttackBy2 < NPCHandler.maxNPCs) {
			NPC n = NPCHandler.npcs[c.targeting.underAttackBy2];
			if (n != null && !n.isDead && n.HP > 0) {
				return c.targeting.underAttackBy2;
			}
		}
		return -1;
	}

	private static void syncOverlay(Client c, DwarfCannon cannon) {
		if (c == null || c.getPA() == null) {
			return;
		}
		if (cannon == null) {
			c.getPA().sendFrame126("cannon:off", OVERLAY_FRAME);
			return;
		}
		c.getPA().sendFrame126(
				"cannon:" + cannon.x + ":" + cannon.y + ":" + cannon.z + ":" + cannon.balls, OVERLAY_FRAME);
	}

	private static boolean inFiringArc(int rotation, int cx, int cy, int nx, int ny) {
		switch (rotation) {
		case 0:
			return ny > cy && nx >= cx - 1 && nx <= cx + 1;
		case 1:
			return nx >= cx + 1 && ny >= cy + 1;
		case 2:
			return nx > cx && ny >= cy - 1 && ny <= cy + 1;
		case 3:
			return ny <= cy - 1 && nx >= cx + 1;
		case 4:
			return ny < cy && nx >= cx - 1 && nx <= cx + 1;
		case 5:
			return nx <= cx - 1 && ny <= cy - 1;
		case 6:
			return nx < cx && ny >= cy - 1 && ny <= cy + 1;
		case 7:
			return nx <= cx - 1 && ny >= cy + 1;
		default:
			return false;
		}
	}


	private static int rollDamage(Client c, NPC n) {
		int max = 5 + (c.skills.playerLevel[4] / 5);
		if (max > 30) {
			max = 30;
		}
		if (max < 2) {
			max = 2;
		}
		int damage = Misc.random(max);
		if (damage > n.HP) {
			damage = n.HP;
		}
		return damage;
	}

	private static void applyHit(Client c, NPC n, int damage) {
		if (c == null || n == null || n.isDead) {
			return;
		}
		n.HP -= damage;
		if (n.HP < 0) {
			n.HP = 0;
		}
		n.handleHitMask(damage);
		n.facePlayer(c.playerId);
		n.underAttack = true;
		n.underAttackBy = c.playerId;
		n.killerId = c.playerId;
		n.lastDamageTaken = System.currentTimeMillis();
		c.targeting.killingNpcIndex = n.npcId;
		c.killCredit.totalDamageDealt += damage;
		if (damage > 0) {
			c.getPA().addSkillXP(damage * Config.RANGE_EXP_RATE, 4);
			c.getPA().addSkillXP(damage * Config.RANGE_EXP_RATE / 3, 3);
			c.getPA().refreshSkill(3);
			c.getPA().refreshSkill(4);
		}
	}

	private static void spawnStage(DwarfCannon cannon, int stage) {
		int id = SETUP_OBJECTS[stage];
		new Object(id, cannon.x, cannon.y, cannon.z, 0, 10, id, -1);
		registerClick(id, cannon.x, cannon.y, cannon.z);
	}

	private static void pickupInternal(Client c, DwarfCannon cannon, boolean silent) {
		if (cannon == null) {
			return;
		}
		Client owner = cannon.owner == null ? c : playerByName(cannon.owner);
		if (owner == null) {
			owner = c;
		}
		CycleEventHandler.stopEvents(owner, FIRE_EVENT);
		CycleEventHandler.stopEvents(owner, BUILD_EVENT);
		cannon.shooting = false;
		cannon.building = false;
		if (c != null) {
			syncOverlay(c, null);
		} else if (owner != null) {
			syncOverlay(owner, null);
		}
		removeObject(cannon);
		cannons.remove(cannon);
		int parts = cannon.stage;
		if (parts > SETUP_ITEMS.length) {
			parts = SETUP_ITEMS.length;
		}
		if (c != null) {
			c.startAnimation(BUILD_ANIM);
			for (int i = 0; i < parts; i++) {
				giveOrDrop(c, SETUP_ITEMS[i], 1, cannon);
			}
			if (cannon.balls > 0) {
				giveOrDrop(c, ITEM_BALLS, cannon.balls, cannon);
			}
			if (!silent) {
				c.sendMessage("You pick up the cannon.");
			}
		}
	}

	private static void giveOrDrop(Client c, int id, int amount, DwarfCannon cannon) {
		if (c.getItems().addItem(id, amount)) {
			return;
		}
		Server.itemHandler.createGroundItem(c, id, cannon.x, cannon.y, amount, c.playerId);
		c.sendMessage("You don't have enough inventory space. Some items were dropped.");
	}

	private static void removeObject(DwarfCannon cannon) {
		Object spawned = Server.objectManager.getObject(cannon.x, cannon.y, cannon.z);
		if (spawned != null) {
			Server.objectManager.object.remove(spawned);
		}
		Server.objectManager.removeObject(cannon.x, cannon.y);
		unregisterClicks(cannon.x, cannon.y, cannon.z);
	}

	private static void registerClick(int id, int x, int y, int z) {
		Region r = Region.getRegion(x, y);
		if (r == null) {
			return;
		}
		r.realObjects.add(new Objects(id, x, y, z, 0, 10));
		if (z != 0) {
			r.realObjects.add(new Objects(id, x, y, 0, 0, 10));
		}
	}

	private static void unregisterClicks(int x, int y, int z) {
		Region r = Region.getRegion(x, y);
		if (r == null) {
			return;
		}
		Iterator<Objects> it = r.realObjects.iterator();
		while (it.hasNext()) {
			Objects o = it.next();
			if (o == null) {
				continue;
			}
			if (o.objectX == x && o.objectY == y && (o.objectHeight == z || o.objectHeight == 0)
					&& isCannonObject(o.objectId)) {
				it.remove();
			}
		}
	}

	private static void stepOff(Client c) {
		if (Region.getClipping(c.getX() - 1, c.getY(), c.position.heightLevel, -1, 0)) {
			c.getPA().walkTo(-1, 0);
		} else if (Region.getClipping(c.getX(), c.getY() - 1, c.position.heightLevel, 0, -1)) {
			c.getPA().walkTo(0, -1);
		} else if (Region.getClipping(c.getX() + 1, c.getY(), c.position.heightLevel, 1, 0)) {
			c.getPA().walkTo(1, 0);
		} else if (Region.getClipping(c.getX(), c.getY() + 1, c.position.heightLevel, 0, 1)) {
			c.getPA().walkTo(0, 1);
		}
	}

	private static boolean canBuildAt(int x, int y, int z) {
		for (int dx = 0; dx < 3; dx++) {
			for (int dy = 0; dy < 3; dy++) {
				if (dx == 0 && dy == 0) {
					continue;
				}
				int clip = Region.getClipping(x + dx, y + dy, z);
				if ((clip & 0x12801ff) != 0) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean tooCloseToCannon(int x, int y, int z) {
		Iterator<DwarfCannon> it = cannons.iterator();
		while (it.hasNext()) {
			DwarfCannon other = it.next();
			if (other == null || other.z != z) {
				continue;
			}
			int dist = Math.max(Math.abs(other.x - x), Math.abs(other.y - y));
			if (dist < MIN_CANNON_DISTANCE) {
				return true;
			}
		}
		return false;
	}

	private static DwarfCannon forUser(String name) {
		if (name == null) {
			return null;
		}
		Iterator<DwarfCannon> it = cannons.iterator();
		while (it.hasNext()) {
			DwarfCannon cannon = it.next();
			if (cannon != null && name.equalsIgnoreCase(cannon.owner)) {
				return cannon;
			}
		}
		return null;
	}

	private static DwarfCannon forLocation(int x, int y, int z) {
		Iterator<DwarfCannon> it = cannons.iterator();
		while (it.hasNext()) {
			DwarfCannon cannon = it.next();
			if (cannon == null || cannon.z != z) {
				continue;
			}
			if (x >= cannon.x && x < cannon.x + 3 && y >= cannon.y && y < cannon.y + 3) {
				return cannon;
			}
		}
		return null;
	}

	private static Client playerByName(String name) {
		if (name == null) {
			return null;
		}
		for (int i = 0; i < Config.MAX_PLAYERS; i++) {
			Player p = PlayerHandler.players[i];
			if (p != null && name.equalsIgnoreCase(p.playerName)) {
				return (Client) p;
			}
		}
		return null;
	}

	private static Client nearbyPlayer(int x, int y, int z) {
		for (int i = 0; i < Config.MAX_PLAYERS; i++) {
			Player p = PlayerHandler.players[i];
			if (p != null && p.position.heightLevel == z && p.distanceToPoint(x, y) <= 25) {
				return (Client) p;
			}
		}
		return null;
	}
}
