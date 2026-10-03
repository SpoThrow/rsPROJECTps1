package server.game.npcs;

import server.Server;
import server.game.players.Client;
import server.game.players.PlayerHandler;
import core.util.Misc;

/**
 * Max — a living world NPC. He travels between real training spots, fights,
 * skills, banks, greets players, and talks about what he is actually doing.
 */
public class WorldAdventurer {

	public static final int NPC_ID = 650;
	public static final String NAME = "Max";

	private static NPC npc;
	private static Spot spot;
	private static Spot nextSpot;
	private static Phase phase = Phase.WORK;
	private static int jobTicks;
	private static int stuckTicks;
	private static int lastX;
	private static int lastY;
	private static int combatDelay;
	private static int actionDelay;
	private static int chatDelay;
	private static int greetDelay;
	private static int lastGreetPid = -1;
	private static int workDone;
	private static int sessionKills;
	private static int sessionLogs;
	private static int sessionFish;
	private static int sessionOre;
	private static int sessionBones;
	private static int sessionBanks;
	private static String lastGreetName = "";
	private static Spot lastSpot;
	private static int[][] path;
	private static int pathStep;
	private static int pathLen;

	private enum Phase {
		TRAVEL, WORK
	}

	private enum Kind {
		PVM, RANGE, MAGIC, WOODCUT, FISH, MINE, COOK, SMITH, PRAY, BANK, CITY, FIREMAKE
	}

	private static final Spot[] SPOTS = {
			spot("Lumbridge cows", Kind.PVM, 3253, 3267, 0, 451, 90, 8, new int[] { 81, 397, 1766 },
					new String[] { "These cows never learn.", "Beef for days.", "Watch the horns." },
					"Cows first. Always cows."),
			spot("Lumbridge chickens", Kind.PVM, 3230, 3294, 0, 422, 55, 6, new int[] { 41, 1017 },
					new String[] { "Easy feathers.", "Cluck off.", "Breakfast, sorted." },
					"Quick chickens, then I'm gone."),
			spot("Lumbridge goblins", Kind.PVM, 3244, 3247, 0, 451, 80, 7, new int[] { 100, 101, 102, 103 },
					new String[] { "Goblins first.", "Ugly little things.", "One more camp." },
					"Goblins. Warm-up."),
			spot("Al Kharid scorpions", Kind.PVM, 3298, 3296, 0, 451, 70, 7, new int[] { 107, 1477 },
					new String[] { "Mind the sting.", "Desert heat's worse.", "That one almost got me." },
					"Scorpions in the desert."),
			spot("South Varrock wizards", Kind.MAGIC, 3226, 3368, 0, 711, 70, 6, new int[] { 172, 174 },
					new String[] { "Eat bolt.", "Dark wizards, darker hats.", "Stay out of the splash." },
					"Wizards south of Varrock."),
			spot("Barbarian Village", Kind.PVM, 3082, 3422, 0, 451, 75, 7, new int[] { 3246, 3247, 142, 141 },
					new String[] { "Barbs hit back.", "Love a good axe fight.", "Village never sleeps." },
					"Barbarian Village. Proper scrap."),
			spot("Hobgoblins", Kind.PVM, 3023, 3472, 0, 451, 70, 7, new int[] { 122, 123 },
					new String[] { "Hobgoblins. Meaner goblins.", "Keep your distance.", "That's the one." },
					"Hobgoblins near Ice Mountain."),
			spot("Cow ranging", Kind.RANGE, 3257, 3271, 0, 426, 60, 8, new int[] { 81, 397, 1766 },
					new String[] { "Range practice.", "Don't block my shot.", "Dead centre." },
					"Going to range a few cows."),
			spot("Lumbridge trees", Kind.WOODCUT, 3192, 3223, 0, 875, 55, 1, null,
					new String[] { "A few logs never hurt.", "Timber.", "Need the xp." },
					"Chopping near Lumbridge."),
			spot("Draynor willows", Kind.WOODCUT, 3087, 3236, 0, 875, 70, 1, null,
					new String[] { "Willows are honest work.", "River's loud today.", "That's a good tree." },
					"Willows in Draynor."),
			spot("Varrock oaks", Kind.WOODCUT, 3277, 3426, 0, 875, 50, 1, null,
					new String[] { "Oaks by the palace.", "City trees, city noise.", "One more oak." },
					"Oaks east of Varrock."),
			spot("Draynor fishing", Kind.FISH, 3086, 3228, 0, 621, 70, 0, null,
					new String[] { "Something's biting.", "Come on, fish.", "Net's working." },
					"Fishing in Draynor."),
			spot("Barbarian fishing", Kind.FISH, 3104, 3432, 0, 618, 65, 0, null,
					new String[] { "River's cold.", "Fly fishing. Classy.", "Another trout." },
					"Fishing at Barbarian Village."),
			spot("Karamja dock", Kind.FISH, 2924, 3178, 0, 621, 60, 0, null,
					new String[] { "Karambwanji weather.", "Dock smells of fish. Good.", "That's a bite." },
					"Boat to Karamja, then fish."),
			spot("Varrock east mine", Kind.MINE, 3285, 3366, 0, 625, 70, 1, null,
					new String[] { "Ore's looking decent.", "Pick's singing.", "Tin, copper, repeat." },
					"Varrock east mine."),
			spot("Rimmington mine", Kind.MINE, 2975, 3239, 0, 625, 60, 1, null,
					new String[] { "Quiet pit.", "Gold if I'm lucky.", "Back to it." },
					"Rimmington rocks."),
			spot("Al Kharid mine", Kind.MINE, 3299, 3313, 0, 625, 55, 1, null,
					new String[] { "Sand in the boots.", "Iron if the world's kind.", "Hot work." },
					"Al Kharid mine."),
			spot("Lumbridge kitchen", Kind.COOK, 3208, 3213, 0, 896, 40, 1, null,
					new String[] { "Don't burn it.", "Cooked it proper.", "Range is hot." },
					"Cooking in Lumbridge."),
			spot("Varrock anvil", Kind.SMITH, 3228, 3435, 0, 898, 40, 1, null,
					new String[] { "Hammer down.", "Bars into blades.", "Smithy's loud. I like it." },
					"Smithing in Varrock."),
			spot("Lumbridge church", Kind.PRAY, 3244, 3207, 0, 827, 35, 1, null,
					new String[] { "Respect the dead.", "Bones for the altar.", "Quiet in here." },
					"Church. Need the prayer."),
			spot("Lumbridge bank", Kind.BANK, 3208, 3220, 0, 881, 22, 1, null,
					new String[] { "Bank's full of junk again.", "Deposit the lot.", "Pin's still 0000. Kidding." },
					"Banking in Lumbridge."),
			spot("Varrock bank", Kind.BANK, 3185, 3436, 0, 881, 22, 1, null,
					new String[] { "Varrock bank's busy.", "Need the space.", "That's the lot." },
					"Banking in Varrock."),
			spot("Falador bank", Kind.BANK, 2946, 3368, 0, 881, 22, 1, null,
					new String[] { "White knights, white marble.", "Falador's tidy.", "In and out." },
					"Banking in Falador."),
			spot("Lumbridge square", Kind.CITY, 3222, 3218, 0, -1, 28, 2, null,
					new String[] { "Just passing through Lumbridge.", "Castle's still standing.", "Newcomers everywhere." },
					"Back through Lumbridge."),
			spot("Varrock square", Kind.CITY, 3212, 3424, 0, -1, 30, 2, null,
					new String[] { "Varrock never sleeps.", "Fountain's crowded.", "News of the world." },
					"Varrock square."),
			spot("Falador square", Kind.CITY, 2965, 3381, 0, -1, 26, 2, null,
					new String[] { "Falador's too clean.", "Park's nice though.", "White walls, white lies." },
					"Stroll through Falador."),
			spot("Draynor market", Kind.CITY, 3080, 3250, 0, -1, 24, 2, null,
					new String[] { "Diango's still here.", "Market's a racket.", "Wise Old Man's watching." },
					"Draynor market."),
			spot("Port Sarim", Kind.CITY, 3028, 3236, 0, -1, 26, 2, null,
					new String[] { "Ships and drunk sailors.", "Sea air. Better than cows.", "Might take a boat later." },
					"Port Sarim for a look around."),
			spot("Edgeville", Kind.CITY, 3094, 3492, 0, -1, 24, 2, null,
					new String[] { "Quiet town. I like it.", "Wilderness is that way. Not today.", "Edgeville's honest." },
					"Cutting through Edgeville."),
			spot("Rimmington", Kind.CITY, 2957, 3216, 0, -1, 22, 2, null,
					new String[] { "Sleepy place.", "House portal's busy.", "Chemists and craftsmen." },
					"Rimmington. Change of pace."),
			spot("Lumbridge swamp rats", Kind.PVM, 3204, 3195, 0, 451, 55, 7, new int[] { 47, 87, 446 },
					new String[] { "Swamp's grim.", "Rats. Always rats.", "Watch the mud." },
					"Swamp rats. Easy bones."),
			spot("Fred's chickens", Kind.PVM, 3190, 3285, 0, 422, 40, 6, new int[] { 41, 1017 },
					new String[] { "Farm chickens.", "Fred still moaning?", "Feathers everywhere." },
					"Fred's farm. Quick kills."),
			spot("Ice Mountain dwarves", Kind.PVM, 3016, 3451, 0, 451, 65, 7, new int[] { 118, 121, 382 },
					new String[] { "Short and stubborn.", "Mountain wind's sharp.", "That's a solid hit." },
					"Dwarves on Ice Mountain."),
			spot("Edgeville hill giants", Kind.PVM, 3117, 9846, 0, 451, 80, 8, new int[] { 117, 469 },
					new String[] { "Big bones. Worth it.", "Watch the clubs.", "Dungeon air. Lovely." },
					"Hill giants under Edgeville."),
			spot("Varrock guards", Kind.PVM, 3212, 3429, 0, 451, 45, 7, new int[] { 9, 21, 23 },
					new String[] { "Don't tell the watch.", "Palace lawns.", "One more patrol." },
					"Guards in Varrock. Quietly.")
	};

	private static final int[][] HUBS = {
			{ 3222, 3218 }, { 3080, 3250 }, { 3028, 3236 }, { 2965, 3381 }, { 3212, 3424 },
			{ 3293, 3183 }, { 3082, 3422 }, { 3094, 3492 }, { 2946, 3368 }, { 3185, 3436 },
			{ 3208, 3220 }, { 2924, 3178 }, { 3116, 3510 }, { 3096, 9867 }
	};

	private static class Spot {
		final String name;
		final Kind kind;
		final int x;
		final int y;
		final int height;
		final int anim;
		final int duration;
		final int arrive;
		final int[] targets;
		final String[] workChat;
		final String travel;

		Spot(String name, Kind kind, int x, int y, int height, int anim, int duration, int arrive, int[] targets,
				String[] workChat, String travel) {
			this.name = name;
			this.kind = kind;
			this.x = x;
			this.y = y;
			this.height = height;
			this.anim = anim;
			this.duration = duration;
			this.arrive = arrive;
			this.targets = targets;
			this.workChat = workChat;
			this.travel = travel;
		}
	}

	private static Spot spot(String name, Kind kind, int x, int y, int h, int anim, int duration, int arrive,
			int[] targets, String[] chat, String travel) {
		return new Spot(name, kind, x, y, h, anim, duration, arrive, targets, chat, travel);
	}

	public static void spawn() {
		if (npc != null) {
			return;
		}
		spot = findKind(Kind.CITY);
		if (spot == null) {
			spot = SPOTS[SPOTS.length - 1];
		}
		npc = Server.npcHandler.spawnNpc2(NPC_ID, spot.x, spot.y, spot.height, 0, 200, 8, 80, 80);
		if (npc == null) {
			System.out.println("[WorldAdventurer] No NPC slot for " + NAME + ".");
			return;
		}
		npc.worldAdventurer = true;
		npc.randomWalk = false;
		npc.spawnX = spot.x;
		npc.spawnY = spot.y;
		npc.makeX = spot.x;
		npc.makeY = spot.y;
		phase = Phase.WORK;
		jobTicks = spot.duration;
		lastX = npc.absX;
		lastY = npc.absY;
		npc.forceChat("Name's Max. I actually train out here.");
		System.out.println("[WorldAdventurer] " + NAME + " is wandering the world.");
	}

	public static boolean isAdventurer(int index) {
		return index > 0 && NPCHandler.npcs[index] != null && NPCHandler.npcs[index].worldAdventurer;
	}

	public static String whereLine() {
		if (spot == null) {
			return "somewhere in Gielinor";
		}
		if (phase == Phase.TRAVEL) {
			return "heading to " + spot.name;
		}
		return "at " + spot.name;
	}

	public static void teleportTo(Client c) {
		if (c == null) {
			return;
		}
		if (npc == null) {
			spawn();
		}
		if (npc == null) {
			c.sendMessage("Max isn't in the world right now.");
			return;
		}
		c.getPA().spellTeleport(npc.absX, npc.absY, npc.heightLevel);
		c.sendMessage("You teleport to Max, who is " + whereLine() + ".");
	}

	public static void talk(Client c) {
		if (c == null) {
			return;
		}
		c.npcType = NPC_ID;
		c.talkingNpc = NPC_ID;
		c.getDH().sendDialogues(8800, NPC_ID);
	}

	public static void handleMenu(Client c, int option) {
		if (c == null) {
			return;
		}
		c.npcType = NPC_ID;
		c.talkingNpc = NPC_ID;
		if (option == 1) {
			c.getDH().sendDialogues(8802, NPC_ID);
		} else if (option == 2) {
			c.getDH().sendDialogues(8803, NPC_ID);
		} else if (option == 3) {
			c.getDH().sendDialogues(8804, NPC_ID);
		} else {
			c.getDH().sendDialogues(8805, NPC_ID);
		}
	}

	public static String greetingLine() {
		return greetingLine(null);
	}

	public static String greetingLine(Client c) {
		String who = c != null && c.playerName != null ? c.playerName : "stranger";
		if (spot == null) {
			return "Don't mind me, " + who + ". Just passing through.";
		}
		if (phase == Phase.TRAVEL) {
			return "On the road, " + who + ". " + spot.travel;
		}
		return "I'm at " + spot.name + " right now.";
	}

	public static String doingLine() {
		return doingLine(null);
	}

	public static String doingLine(Client c) {
		if (spot == null) {
			return "Training. Same as you.";
		}
		String extra = sessionSummary();
		String was = lastSpot != null && lastSpot != spot ? " Came from " + lastSpot.name + "." : "";
		switch (spot.kind) {
		case PVM:
			return "Fighting. " + extra + was;
		case RANGE:
			return "Ranging. Need the accuracy. " + extra + was;
		case MAGIC:
			return "Splashing some magic. " + extra + was;
		case WOODCUT:
			return "Woodcutting. " + extra + was;
		case FISH:
			return "Fishing. Beats buying it. " + extra + was;
		case MINE:
			return "Mining. Rocks don't talk back. " + extra + was;
		case COOK:
			return "Cooking whatever I caught." + was;
		case SMITH:
			return "Smithing. Hammer therapy." + was;
		case PRAY:
			return "Offering bones. Keeps me honest." + was;
		case BANK:
			return "Banking the junk so I can keep going." + was;
		case CITY:
			return "Taking a breather. Even I rest." + was;
		case FIREMAKE:
			return "Burning the logs I just cut." + was;
		default:
			return "Training.";
		}
	}

	public static String headingLine() {
		if (phase == Phase.WORK && nextSpot != null && nextSpot != spot) {
			return nextSpot.travel;
		}
		if (phase == Phase.TRAVEL && spot != null) {
			return spot.travel;
		}
		return "Haven't decided. Wherever the work is.";
	}

	public static String tipLine() {
		return tipLine(null);
	}

	public static String tipLine(Client c) {
		if (c != null) {
			if (c.combatLevel < 20) {
				return "Cows and goblins until you stop dying. Then we talk.";
			}
			if (c.combatLevel < 50) {
				return "Hill giants under Edgeville. Big bones, decent xp.";
			}
			if (c.combatLevel >= 80) {
				return "You're past the easy stuff. Don't get cocky in the wild.";
			}
		}
		if (spot == null) {
			return "Don't stand in other people's hitsplats.";
		}
		switch (spot.kind) {
		case PVM:
			return "Pray if you can. Eat if you can't. Simple.";
		case RANGE:
			return "Stand still, then shoot. Not the other way round.";
		case MAGIC:
			return "Runes first, ego second.";
		case WOODCUT:
			return "Willows beat regular trees if you want xp.";
		case FISH:
			return "Draynor's busy. Barb village is quieter.";
		case MINE:
			return "East Varrock if you want company. Rimmington if you don't.";
		case COOK:
			return "Cook on a range. Fires burn more food.";
		case SMITH:
			return "Bars in the bank before you start. Trust me.";
		case PRAY:
			return "Big bones if you've got them. Rats if you don't.";
		case BANK:
			return "Bank often. Nothing worse than a full bag in a fight.";
		default:
			return "Keep moving. Gielinor's big.";
		}
	}

	private static String sessionSummary() {
		if (sessionKills > 0 && (spot.kind == Kind.PVM || spot.kind == Kind.RANGE || spot.kind == Kind.MAGIC)) {
			return "Dropped " + sessionKills + " this trip.";
		}
		if (sessionLogs > 0 && spot.kind == Kind.WOODCUT) {
			return sessionLogs + " logs so far.";
		}
		if (sessionFish > 0 && spot.kind == Kind.FISH) {
			return sessionFish + " fish in the bag.";
		}
		if (sessionOre > 0 && spot.kind == Kind.MINE) {
			return sessionOre + " ore. Not bad.";
		}
		if (sessionBones > 0 && spot.kind == Kind.PRAY) {
			return sessionBones + " bones offered.";
		}
		if (sessionBanks > 0 && spot.kind == Kind.BANK) {
			return "Bank trip " + sessionBanks + " today.";
		}
		return "Just getting started.";
	}

	public static void tick(NPC n) {
		if (n == null || spot == null) {
			return;
		}
		if (n.isDead || n.HP <= 0) {
			n.isDead = false;
			n.HP = n.MaxHP;
			n.applyDead = false;
			n.needRespawn = false;
		}
		if (n.absX == lastX && n.absY == lastY) {
			stuckTicks++;
		} else {
			stuckTicks = 0;
			lastX = n.absX;
			lastY = n.absY;
		}
		if (combatDelay > 0) {
			combatDelay--;
		}
		if (actionDelay > 0) {
			actionDelay--;
		}
		if (chatDelay > 0) {
			chatDelay--;
		}
		if (greetDelay > 0) {
			greetDelay--;
		}
		if (n.heightLevel != spot.height) {
			n.heightLevel = spot.height;
			n.updateRequired = true;
		}
		socialTick(n);
		if (phase == Phase.TRAVEL) {
			travelTick(n);
			return;
		}
		workTick(n);
	}

	private static void travelTick(NPC n) {
		int dist = distance(n.absX, n.absY, spot.x, spot.y);
		if (dist <= spot.arrive) {
			arrive(n);
			return;
		}
		int tx = spot.x;
		int ty = spot.y;
		if (path != null && pathStep < pathLen) {
			tx = path[pathStep][0];
			ty = path[pathStep][1];
			if (distance(n.absX, n.absY, tx, ty) <= 3) {
				pathStep++;
				stuckTicks = 0;
				return;
			}
		}
		walkToward(n, tx, ty);
		if (chatDelay == 0 && Misc.random(22) == 0) {
			n.forceChat(spot.travel);
			chatDelay = 18;
		}
		if (dist > 55 && stuckTicks > 6 || stuckTicks > 18) {
			shortcut(n, dist > 40);
		}
	}

	private static void arrive(NPC n) {
		phase = Phase.WORK;
		jobTicks = spot.duration + Misc.random(25);
		workDone = 0;
		nextSpot = null;
		path = null;
		pathLen = 0;
		pathStep = 0;
		n.makeX = spot.x;
		n.makeY = spot.y;
		stuckTicks = 0;
		n.forceChat("This'll do. " + spot.name + ".");
		chatDelay = 10;
	}

	private static void shortcut(NPC n, boolean far) {
		int tx = spot.x;
		int ty = spot.y;
		if (path != null && pathStep < pathLen) {
			tx = path[pathStep][0];
			ty = path[pathStep][1];
			pathStep++;
		} else if (far) {
			tx = n.absX + (spot.x - n.absX) / 2;
			ty = n.absY + (spot.y - n.absY) / 2;
		}
		n.gfx100(308);
		n.absX = tx;
		n.absY = ty;
		n.makeX = tx;
		n.makeY = ty;
		n.updateRequired = true;
		stuckTicks = 0;
		if (far) {
			n.forceChat("Shortcut.");
		}
		chatDelay = 8;
	}

	private static void workTick(NPC n) {
		int dist = distance(n.absX, n.absY, spot.x, spot.y);
		if (dist > spot.arrive + 12) {
			phase = Phase.TRAVEL;
			buildPath(n.absX, n.absY, spot.x, spot.y);
			return;
		}
		if (jobTicks == 12 && nextSpot == null) {
			nextSpot = followUp();
			if (nextSpot == null) {
				nextSpot = randomOther();
			}
			if (nextSpot != null && chatDelay == 0) {
				n.forceChat(nextSpot.travel);
				chatDelay = 12;
			}
		}
		if (jobTicks <= 0) {
			pickNextSpot();
			return;
		}
		jobTicks--;
		switch (spot.kind) {
		case PVM:
		case RANGE:
		case MAGIC:
			fightNearby(n);
			break;
		case CITY:
			wander(n);
			maybeChat(n);
			break;
		case BANK:
			bankWork(n);
			break;
		default:
			skillWork(n);
			break;
		}
	}

	private static void skillWork(NPC n) {
		if (actionDelay > 0) {
			return;
		}
		if (spot.anim > 0) {
			n.animNumber = spot.anim;
			n.animUpdateRequired = true;
			n.updateRequired = true;
		}
		if (spot.kind == Kind.WOODCUT) {
			sessionLogs++;
			workDone++;
		} else if (spot.kind == Kind.FISH) {
			sessionFish++;
			workDone++;
		} else if (spot.kind == Kind.MINE) {
			sessionOre++;
			workDone++;
		} else if (spot.kind == Kind.PRAY) {
			sessionBones++;
			n.gfx0(624);
			workDone++;
		} else if (spot.kind == Kind.FIREMAKE) {
			n.gfx100(157);
			workDone++;
		} else {
			workDone++;
		}
		actionDelay = 3 + Misc.random(2);
		maybeChat(n);
	}

	private static void bankWork(NPC n) {
		if (actionDelay == 0) {
			if (spot.anim > 0) {
				n.animNumber = spot.anim;
				n.animUpdateRequired = true;
				n.updateRequired = true;
			}
			actionDelay = 4;
			workDone++;
			if (workDone == 2) {
				sessionBanks++;
			}
		}
		maybeChat(n);
	}

	private static void wander(NPC n) {
		if (Misc.random(6) != 0) {
			return;
		}
		int nx = spot.x + Misc.random(4) - 2;
		int ny = spot.y + Misc.random(4) - 2;
		walkToward(n, nx, ny);
	}

	private static void maybeChat(NPC n) {
		if (spot.workChat == null || spot.workChat.length == 0 || chatDelay > 0) {
			return;
		}
		if (Misc.random(16) != 0) {
			return;
		}
		n.forceChat(spot.workChat[Misc.random(spot.workChat.length - 1)]);
		chatDelay = 16;
	}

	private static void fightNearby(NPC n) {
		if (combatDelay > 0) {
			return;
		}
		NPC prey = findPrey(n);
		if (prey == null) {
			wander(n);
			maybeChat(n);
			return;
		}
		int dist = distance(n.absX, n.absY, prey.absX, prey.absY);
		int keep = spot.kind == Kind.PVM ? 1 : 5;
		if (dist > keep) {
			walkToward(n, prey.absX, prey.absY);
			return;
		}
		if (spot.kind != Kind.PVM && dist < 3) {
			walkToward(n, n.absX + (n.absX - prey.absX), n.absY + (n.absY - prey.absY));
			return;
		}
		n.turnNpc(prey.absX, prey.absY);
		n.animNumber = spot.anim > 0 ? spot.anim : 451;
		n.animUpdateRequired = true;
		n.updateRequired = true;
		if (spot.kind == Kind.MAGIC) {
			n.gfx100(100);
			prey.gfx0(101);
		} else if (spot.kind == Kind.RANGE) {
			n.gfx100(24);
		}
		int hit = 2 + Misc.random(6);
		boolean steal = playerNear(n, 8);
		if (steal && prey.HP - hit < 1) {
			hit = prey.HP - 1;
		}
		if (hit > 0 && prey.HP > 0) {
			prey.HP -= hit;
			if (prey.HP < 0) {
				prey.HP = 0;
			}
			prey.handleHitMask(hit);
			prey.animNumber = NPCHandler.getBlockEmote(prey.npcId);
			prey.animUpdateRequired = true;
			prey.updateRequired = true;
			if (prey.HP <= 0) {
				prey.isDead = true;
				sessionKills++;
				workDone++;
				n.forceChat("Down.");
				chatDelay = 8;
			}
		}
		maybeChat(n);
		combatDelay = spot.kind == Kind.PVM ? 4 : 5;
	}

	private static NPC findPrey(NPC n) {
		NPC closest = null;
		int best = spot.kind == Kind.PVM ? 12 : 15;
		for (int i = 1; i < NPCHandler.maxNPCs; i++) {
			NPC other = NPCHandler.npcs[i];
			if (other == null || other == n || other.worldAdventurer || other.isDead || other.HP <= 0) {
				continue;
			}
			if (other.heightLevel != n.heightLevel) {
				continue;
			}
			if (!isTarget(other.npcType)) {
				continue;
			}
			int d = distance(n.absX, n.absY, other.absX, other.absY);
			if (d < best) {
				best = d;
				closest = other;
			}
		}
		return closest;
	}

	private static boolean isTarget(int type) {
		if (spot == null || spot.targets == null) {
			return false;
		}
		for (int i = 0; i < spot.targets.length; i++) {
			if (spot.targets[i] == type) {
				return true;
			}
		}
		return false;
	}

	private static void socialTick(NPC n) {
		if (greetDelay > 0) {
			return;
		}
		Client near = nearestPlayer(n, 6);
		if (near == null || near.playerName == null) {
			return;
		}
		boolean again = near.playerId == lastGreetPid && near.playerName.equalsIgnoreCase(lastGreetName);
		lastGreetPid = near.playerId;
		lastGreetName = near.playerName;
		greetDelay = again ? 70 + Misc.random(40) : 36 + Misc.random(24);
		n.turnNpc(near.position.absX, near.position.absY);
		String name = near.playerName;
		int roll = Misc.random(6);
		if (again) {
			n.forceChat("Still here, " + name + "?");
		} else if (roll == 0) {
			n.forceChat("Afternoon, " + name + ".");
		} else if (roll == 1) {
			n.forceChat("Combat " + near.combatLevel + "? Not bad, " + name + ".");
		} else if (roll == 2) {
			n.forceChat("Don't stand in my way, " + name + ".");
		} else if (roll == 3) {
			n.forceChat(name + ". Try " + (nextSpot != null ? nextSpot.name : spot != null ? spot.name : "the cows")
					+ " if you're bored.");
		} else if (roll == 4 && lastSpot != null) {
			n.forceChat(name + ". Just came from " + lastSpot.name + ".");
		} else {
			n.forceChat("Keep at it, " + name + ".");
		}
		chatDelay = 12;
	}

	private static Client nearestPlayer(NPC n, int range) {
		Client best = null;
		int bestD = range + 1;
		for (int i = 0; i < PlayerHandler.players.length; i++) {
			if (PlayerHandler.players[i] == null) {
				continue;
			}
			Client p = (Client) PlayerHandler.players[i];
			if (p.disconnected || p.position.heightLevel != n.heightLevel) {
				continue;
			}
			int d = distance(n.absX, n.absY, p.position.absX, p.position.absY);
			if (d < bestD) {
				bestD = d;
				best = p;
			}
		}
		return best;
	}

	private static boolean playerNear(NPC n, int range) {
		return nearestPlayer(n, range) != null;
	}

	private static void walkToward(NPC n, int destX, int destY) {
		n.moveX = Server.npcHandler.GetMove(n.absX, destX);
		n.moveY = Server.npcHandler.GetMove(n.absY, destY);
		if (n.moveX == 0 && n.moveY == 0 && (n.absX != destX || n.absY != destY)) {
			n.moveX = Misc.random(2) - 1;
			n.moveY = Misc.random(2) - 1;
		}
		Server.npcHandler.handleClipping(n.npcId);
		n.getNextNPCMovement(n.npcId);
		n.updateRequired = true;
	}

	private static void pickNextSpot() {
		if (npc == null) {
			return;
		}
		Spot chosen = nextSpot != null ? nextSpot : followUp();
		if (chosen == null) {
			chosen = randomOther();
		}
		startTravel(chosen);
	}

	private static void startTravel(Spot chosen) {
		if (chosen == null || npc == null) {
			return;
		}
		lastSpot = spot;
		nextSpot = chosen;
		npc.forceChat(chosen.travel);
		chatDelay = 14;
		spot = chosen;
		phase = Phase.TRAVEL;
		stuckTicks = 0;
		npc.makeX = chosen.x;
		npc.makeY = chosen.y;
		buildPath(npc.absX, npc.absY, chosen.x, chosen.y);
	}

	private static void buildPath(int fromX, int fromY, int toX, int toY) {
		int[][] built = new int[12][2];
		int n = 0;
		int cx = fromX;
		int cy = fromY;
		int guard = 0;
		while (distance(cx, cy, toX, toY) > 28 && guard++ < 10) {
			int best = -1;
			int bestHere = 99999;
			int remain = distance(cx, cy, toX, toY);
			for (int i = 0; i < HUBS.length; i++) {
				int hx = HUBS[i][0];
				int hy = HUBS[i][1];
				int toDest = distance(hx, hy, toX, toY);
				if (toDest >= remain - 6) {
					continue;
				}
				int fromHere = distance(cx, cy, hx, hy);
				if (fromHere < 8) {
					continue;
				}
				if (fromHere < bestHere) {
					bestHere = fromHere;
					best = i;
				}
			}
			if (best < 0) {
				break;
			}
			built[n][0] = HUBS[best][0];
			built[n][1] = HUBS[best][1];
			cx = built[n][0];
			cy = built[n][1];
			n++;
		}
		built[n][0] = toX;
		built[n][1] = toY;
		n++;
		path = built;
		pathLen = n;
		pathStep = 0;
	}

	private static Spot followUp() {
		if (spot == null) {
			return null;
		}
		if (spot.kind == Kind.WOODCUT && Misc.random(2) == 0) {
			return new Spot(spot.name + " fire", Kind.FIREMAKE, spot.x, spot.y, spot.height, 733, 22, 1, null,
					new String[] { "Warmth.", "Logs to ash.", "That's a fire." }, "Might as well burn these.");
		}
		if ((spot.kind == Kind.FISH || spot.kind == Kind.WOODCUT || spot.kind == Kind.MINE) && Misc.random(2) == 0) {
			return nearestKind(Kind.BANK, spot.x, spot.y);
		}
		if (spot.kind == Kind.FISH && Misc.random(2) == 0) {
			return findKind(Kind.COOK);
		}
		if (spot.kind == Kind.MINE && Misc.random(3) == 0) {
			return findKind(Kind.SMITH);
		}
		if ((spot.kind == Kind.PVM || spot.kind == Kind.RANGE) && Misc.random(3) == 0) {
			return findKind(Kind.PRAY);
		}
		if (spot.kind == Kind.BANK && Misc.random(2) == 0) {
			return nearestKind(Kind.CITY, spot.x, spot.y);
		}
		return null;
	}

	private static Spot randomOther() {
		Spot pick = SPOTS[Misc.random(SPOTS.length - 1)];
		int guard = 0;
		while (spot != null && pick.name.equals(spot.name) && guard++ < 12) {
			pick = SPOTS[Misc.random(SPOTS.length - 1)];
		}
		return pick;
	}

	private static Spot findKind(Kind kind) {
		for (int i = 0; i < SPOTS.length; i++) {
			if (SPOTS[i].kind == kind) {
				return SPOTS[i];
			}
		}
		return null;
	}

	private static Spot nearestKind(Kind kind, int x, int y) {
		Spot best = null;
		int bestD = 99999;
		for (int i = 0; i < SPOTS.length; i++) {
			if (SPOTS[i].kind != kind) {
				continue;
			}
			int d = distance(x, y, SPOTS[i].x, SPOTS[i].y);
			if (d < bestD) {
				bestD = d;
				best = SPOTS[i];
			}
		}
		return best;
	}

	private static int distance(int x1, int y1, int x2, int y2) {
		return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
	}
}
