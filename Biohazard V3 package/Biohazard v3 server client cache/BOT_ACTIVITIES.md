# BOT_ACTIVITIES.md — how a bot trains, travels, fights, shops and banks

This is the **activity graph** the bot needs before it can be autonomous: every skill's
method, every way to travel, where the monsters and shops are, how loot works, and the
full Slayer loop. It is grounded in the code — each row cites the file it came from.

Companion to `BOT_PLAN.md` (slice 1), `BOT_ROADMAP.md` (scaling) and `BOT_TOOLING.md`
(the visual author). Everything here is expressed as **states the roadmap already
anticipates** (`WalkTo`, `Interact`, `Bank`, `Shop`) plus a **`Locations` service** the
tooling will eventually draw on the map (`BOT_ROADMAP.md` §5.2).

---

## 0. The model in one picture

Every activity is the same four-part loop:

```
BANK ──> TRAVEL ──> DO THE WORK ──> LOOT/PROCESS ──> BACK TO BANK
        (teleport+w)   (skill/combat)   (pick up / cook / smith)
```

The only thing that varies per skill is the middle two steps. So the bot's state
vocabulary is small and reusable, and the *data* (where the trees are, which teleport
goes to which dungeon, what a shop sells) lives in `Locations`.

### 0.1 XP multipliers — train with the rate in mind

Every skill multiplies its base XP by a constant in `Config.java`:

```263:279:Proxy Server/src/server/Config.java
	public static final int WOODCUTTING_EXPERIENCE = 15; //9
	public static final int MINING_EXPERIENCE = 16; //10
	...
	public static final int SLAYER_EXPERIENCE = 19;//15
	public static final int COOKING_EXPERIENCE = 18;//8
```

Combat is damage-based, not action-based:

```77:79:Proxy Server/src/server/Config.java
	public static final int MELEE_EXP_RATE = 600;
	public static final int RANGE_EXP_RATE = 575;
	public static final int MAGIC_EXP_RATE = 550;
```

So a bot's "time to 99" is dominated by **actions per hour**, which is what locations and
travel choice optimise. (The server also has `::train` for instant XP — bots must **not**
use it; the whole point is to train legitimately.)

---

## 1. The activity locations (already curated)

`WorldAdventurer.SPOTS` is a hand-built table of exactly the places a bot wants: name,
kind, coordinates, arrival radius, and — for combat — the **NPC ids to fight**:

```52:59:Proxy Server/src/server/game/npcs/WorldAdventurer.java
	private static final Spot[] SPOTS = {
			spot("Lumbridge cows", Kind.PVM, 3253, 3267, 0, 451, 90, 8, new int[] { 81, 397, 1766 },
			spot("Lumbridge chickens", Kind.PVM, 3230, 3294, 0, 422, 55, 6, new int[] { 41, 1017 },
			spot("Lumbridge goblins", Kind.PVM, 3244, 3247, 0, 451, 80, 7, new int[] { 100, 101, 102, 103 },
```

It covers woodcutting, fishing, mining, cooking, smithing, prayer, banking, city hubs,
and combat spots from cows to hill giants. **This table is the seed of the `Locations`
service** — the tool (`BOT_TOOLING.md`) will render it and let authors add to it.

| Kind | Spot | X, Y | Targets (NPC/Object ids) |
| --- | --- | --- | --- |
| PVM | Lumbridge cows | 3253, 3267 | 81, 397, 1766 |
| PVM | Lumbridge chickens | 3230, 3294 | 41, 1017 |
| PVM | Lumbridge goblins | 3244, 3247 | 100–103 |
| PVM | Al Kharid scorpions | 3298, 3296 | 107, 1477 |
| PVM | Barbarian Village | 3082, 3422 | 3246, 3247, 142, 141 |
| PVM | Hobgoblins | 3023, 3472 | 122, 123 |
| PVM | Edgeville hill giants | 3117, 9846 | 117, 469 |
| PVM | Ice Mountain dwarves | 3016, 3451 | 118, 121, 382 |
| RANGE | Cow ranging | 3257, 3271 | 81, 397, 1766 |
| MAGIC | South Varrock wizards | 3226, 3368 | 172, 174 |
| WOODCUT | Lumbridge trees | 3192, 3223 | — |
| WOODCUT | Draynor willows | 3087, 3236 | — |
| WOODCUT | Varrock oaks | 3277, 3426 | — |
| FISH | Draynor fishing | 3086, 3228 | — |
| FISH | Barbarian fishing | 3104, 3432 | — |
| FISH | Karamja dock | 2924, 3178 | — |
| MINE | Varrock east mine | 3285, 3366 | — |
| MINE | Rimmington mine | 2975, 3239 | — |
| MINE | Al Kharid mine | 3299, 3313 | — |
| COOK | Lumbridge kitchen | 3208, 3213 | — |
| SMITH | Varrock anvil | 3228, 3435 | — |
| PRAY | Lumbridge church | 3244, 3207 | — |
| BANK | Lumbridge bank | 3208, 3220 | bank booth |
| BANK | Varrock bank | 3185, 3436 | bank booth |
| BANK | Falador bank | 2946, 3368 | bank booth |

Note the hill giants at `3117, 9846` — **plane 0 but a dungeon offset** (the `y+6400`-style
trick called out in `BOT_TOOLING.md` §9). A location is `(x, y, plane)` and dungeons are
not contiguous with the surface map.

---

## 2. Travel — the teleport network

`Data/cfg/teleports.cfg` is the server's own teleport catalogue, grouped by category:

```2:14:Proxy Server/Data/cfg/teleports.cfg
// Modern Spellbook Teleports
teleport = Modern Varrock 3210 3424
teleport = Modern Lumbridge 3222 3218
teleport = Modern Falador 2964 3378
teleport = Modern Camelot 2757 3477
teleport = Modern Catherby 2804 3433
```

### 2.1 The categories that matter to a bot

| Category | Entries | Use |
| --- | --- | --- |
| Modern spells | Varrock, Lumbridge, Falador, Camelot, Catherby, Ardougne, Trollheim, Ape Atoll | General routing |
| Ancient spells | Paddewwa, Senntisten, Kharyrll, Lassar, Dareeyak, Carrallangar, Annakarl, Ghorrock | Wildy / high-level |
| Glory | Edgeville, Al Kharid, Karamja, Magebank | Fast hub hopping |
| **Monster** | Basic `3026,3217`, Desert `3283,3329`, Snow Mountain `2834,3518`, **Slayer Tower `3429,3538`**, Dungeons `2926,2910` | Getting to combat |
| Boss | GWD, KBD, Dag Kings, KQ, Cave, Underwater, Mutant Tarn, Inadequacy | Bossing |
| Minigame | Barrows, BA, Pest Control, Duel Arena, Tzhaar, Castle Wars, Soul Wars | Minigames |
| **Skill** | General, Cooking `3209,3215`, Crafting, Farming, Firemaking, **Mining `3016,3339`**, Runecrafting, **Slayer `2873,2980`**, Woodcutting `2969,3423`, Fishing, Fletching, Herblore, Smithing `2996,3145`, Construction, Hunter, Thieving | Direct to a skill area |
| Master | Strength/Attack `2846,3541`, Prayer `3052,3481`, Defence `3221,3237`, Ranging `2667,3427` | Skillcape masters |
| Fishing | Al Kharid, Shilo, Karamja, Catherby, Trawler | Fishing spots |

### 2.2 Home and respawn

```1:4:Proxy Server/Data/cfg/spawn-points.cfg
spawn = Start_Location 3087 3505
spawn = Respawn 3221 3218
```

`Config.START_LOCATION_X/Y` are loaded from this file. The **start location is Edgeville
`3087,3505`**; **respawn is Lumbridge `3221,3218`**. There is no `::home` command — "home"
is the start location, reachable with the Lumbridge teleport (`3222,3218`).

### 2.3 Routing rule for the bot

`WorldAdventurer` already implements a coarse router: it keeps a small `HUBS` list and
picks the hub that reduces distance to the goal, short-cutting only when stuck.

```159:162:Proxy Server/src/server/game/npcs/WorldAdventurer.java
	private static final int[][] HUBS = {
			{ 3222, 3218 }, { 3080, 3250 }, { 3028, 3236 }, { 2965, 3381 }, { 3212, 3424 },
			{ 3293, 3183 }, { 3082, 3422 }, { 3094, 3492 }, { 2946, 3368 }, { 3185, 3436 },
```

**For real player bots this is different — and better.** A bot walks for real, so the rule
is: **if a teleport lands meaningfully closer to the destination and the bot meets the
requirement, cast it; otherwise walk.** No teleporting to skip work, per your rule. The
router is therefore `nearestTeleport(target)` then `PathFinder` for the remainder.

---

## 3. Per-skill reference

Each row: how the action starts, what it needs, the XP, and where to do it. All XP is
`base × Config multiplier`.

### 3.1 Woodcutting

```15:39:Proxy Server/src/server/content/skills/Woodcutting.java
	public final static int[][] Axe_Settings = {
		{1351, 1, 1, 879}, //Bronze
		...
		{1359, 41, 7, 867}, //Rune
		{6739, 61, 8, 2846}, //Dragon
	public final static int[][] Tree_Settings = {
		{1276, 1342, 1, 25, 1511, 45, 100}, //Tree
		{1281, 1356, 15, 38, 1521, 11, 20}, //Oak
		{1308, 7399, 30, 68, 1519, 11, 8}, //Willow
		{1307, 1343, 45, 100, 1517, 48, 8}, //Maple
		{1309, 7402, 60, 175, 1515, 79, 5}, //Yew
		{1306, 7401, 75, 250, 1513, 150, 3}, //Magic
```

| Tree | Object id | Level | Base XP | Log item | Best spot (X,Y) |
| --- | --- | --- | --- | --- | --- |
| Tree | 1276/1278/1286 | 1 | 25 | 1511 | Lumbridge 3192,3223 |
| Oak | 1281 | 15 | 38 | 1521 | Varrock 3277,3426 |
| Willow | 1308/5551/5553 | 30 | 68 | 1519 | Draynor 3087,3236 |
| Maple | 1307 | 45 | 100 | 1517 | (maple stands) |
| Yew | 1309 | 60 | 175 | 1515 | (yew groves) |
| Magic | 1306 | 75 | 250 | 1513 | (magic trees) |

**Tools:** axe by level — bronze 1351 (1), iron 1349 (1), steel 1353 (6), black 1361 (6),
mithril 1355 (21), adamant 1357 (31), rune 1359 (41), dragon 6739 (61). Axe is found in
inventory *or* weapon slot.

**Notes:** birds' nests (5070) fill slots; the **Spirit Tree** random event can interrupt —
states must treat "session ended" as recoverable (`BOT_PLAN.md` §7). `cutDownTree`
cancels every chopper at that tile, so multi-bot contention is real.

### 3.2 Mining

```360:391:Proxy Server/src/server/content/skills/Mining.java
	private static int[][] data = {
		{2091, 436, 1, 18, 1, 5},	//COPPER
		{2093, 440, 15, 35, 2, 5},	//IRON
		{2097, 453, 30, 50, 3, 8},	//COAL
		{2098, 444, 40, 65, 3, 10},	//GOLD
		{2103, 447, 55, 80, 5, 20},	//MITH
		{2104, 449, 70, 95, 7, 50},	//ADDY
		{2106, 451, 85, 125, 40, 100},//RUNE
```

| Ore | Object ids | Level | Base XP | Item | Mine time |
| --- | --- | --- | --- | --- | --- |
| Copper | 2091/2090 | 1 | 18 | 436 | 1 |
| Tin | 2094/2095 | 1 | 18 | 438 | 1 |
| Iron | 2093/2092 | 15 | 35 | 440 | 2 |
| Silver | 2100/2101 | 20 | 40 | 442 | 5 |
| Coal | 2097/2096 | 30 | 50 | 453 | 3 |
| Gold | 2098/2099 | 40 | 65 | 444 | 3 |
| Mithril | 2103/2102 | 55 | 80 | 447 | 5 |
| Adamant | 2104/2105 | 70 | 95 | 449 | 7 |
| Runite | 2106/2107/14859/14860 | 85 | 125 | 451 | 40 |

**Tools:** pickaxe — bronze 1265 (1), iron 1267 (1), steel 1269 (6), mith 1273 (21), addy
1271 (31), rune 1275 (41), exactly as the woodcutting pattern.

**Essence:** `mineEss` gives rune essence (1436) at 5×16 = 80 XP (the essence mine, i.e. the
Runecrafting feeder loop).

### 3.3 Fishing

Fishing is NPC-based — the bot interacts with a **fishing spot NPC**, and the spot
determines the equipment, bait, level and fish:

```17:27:Proxy Server/src/server/content/skills/Fishing.java
	private enum Spot {
		LURE(309, new int[]{335, 331}, 309, 314, new int[]{20, 30}, false, new int[]{50, 70}, 623),
		CAGE(312, new int[]{377}, 301, -1, new int[]{40}, false, new int[]{90}, 619),
		BIGNET(313, new int[]{353, 341, 363}, 305, -1, new int[]{16, 23, 46}, false, new int[]{20, 45, 100}, 620),
		SMALLNET(316, new int[]{317, 321}, 303, -1, new int[]{1, 15}, false, new int[]{10, 40}, 621),
		MONKNET(326, new int[]{7944}, 303, -1, new int[]{68}, false, new int[]{120}, 621),
		HARPOON(312, new int[]{359, 371}, 311, -1, new int[]{35, 50}, true, new int[]{80, 100}, 618),
		BAIT(316, new int[]{327, 345}, 307, 313, new int[]{5, 10}, true, new int[]{20, 30}, 623),
```

| Spot (NPC id) | Equipment | Bait | Fish | Level | Base XP |
| --- | --- | --- | --- | --- | --- |
| Small net (316) | 303 | — | shrimp 317, anchovies 321 | 1, 15 | 10, 40 |
| Bait (316) | 307 | 313 | sardine 327, herring 345 | 5, 10 | 20, 30 |
| Fly/lure (309) | 309 | 314 (feather) | trout 335, salmon 331 | 20, 30 | 50, 70 |
| Lure (309) | 307 | 313 | pike 349 | 25 | 60 |
| Cage (312) | 301 | — | lobster 377 | 40 | 90 |
| Harpoon (312) | 311 | — | tuna 359, swordfish 371 | 35, 50 | 80, 100 |
| Big net (313) | 305 | — | mackerel 353, cod 341, bass 363 | 16, 23, 46 | 20, 45, 100 |
| Monkfish (326) | 303 | — | monkfish 7944 | 68 | 120 |

**Tool items:** 303 small net, 305 big net, 307 fishing rod, 309 fly fishing rod, 311
harpoon, 301 lobster pot, 313 bait, 314 feather.

**Locations:** Draynor `3086,3228` (net/bait), Barbarian `3104,3432` (fly), Karamja
`2924,3178` (cage/harpoon). Fishing teleports exist for Al Kharid, Shilo, Karamja,
Catherby.

### 3.4 Cooking

`Cooking.cookThisFood` maps raw → cooked with a level and a "burn stop" level:

```19:30:Proxy Server/src/server/content/skills/Cooking.java
			case 317:	cookFish(p, i, 30, 1, 323, 315, object); break;   // shrimp
			case 321:	cookFish(p, i, 30, 1, 323, 319, object); break;   // anchovies
			case 327: 	cookFish(p, i, 40, 1, 369, 325, object); break;   // sardine
			case 345: 	cookFish(p, i, 50, 5, 357, 347, object); break;   // herring
			case 335: 	cookFish(p, i, 70, 15, 343, 333, object); break;  // trout
			case 359: 	cookFish(p, i, 100, 30, 367, 361, object); break; // tuna
```

**Object:** a range or fire. **Location:** Lumbridge kitchen `3208,3213` (a range).

**Role in the bot loop:** this is the natural "process what you gathered" step — fish →
cook → bank, which raises two skills per trip.

### 3.5 Firemaking

```25:36:Proxy Server/src/server/content/skills/Firemaking.java
	private static int[][] logsdata = {
		{1511, 1,  40,  2732},   // logs
		{1521, 15, 60,  2732},   // oak
		{1519, 30, 105, 2732},   // willow
		{1517, 45, 135, 2732},   // maple
		{1515, 60, 203, 2732},   // yew
		{1513, 75, 304, 2732},   // magic
```

**Tool:** tinderbox (590). Logs are consumed, a fire object is created, and the bot steps
aside. This is the classic **pair with woodcutting**: chop, then burn on the spot
(`WorldAdventurer.followUp` literally does "chop → firemake").

### 3.6 Prayer

```20:29:Proxy Server/src/server/content/skills/Prayer.java
                REGULAR(526, 5, "Bones"),
                BIG(532, 15, "Big Bones"),
                BABY_DRAG(534, 30, "Baby Dragon Bones"),
                DRAG(536, 72, "Dragon Bones"),
                DAG(6729, 125, "Dagannoth Bones"),
```

Two methods, very different rates:

- **Bury** (`buryBone`): `xp × PRAYER_EXPERIENCE`, with a 1-in-20 **double**.
- **Altar** (`bonesOnAltar`): `xp × 4 × PRAYER_EXPERIENCE`, with a small chance of failure
  (the gods "not satisfied").

**So the altar is ~4× better** and a bot that banks bones should use one. Lumbridge church
`3244,3207`. Bones come free from combat — this is why "loot everything" pays off.

### 3.7 Smithing (smelting + smithing)

Smelting at a **furnace** into bars:

```17:24:Proxy Server/src/server/content/skills/Smelting.java
		BRONZE(436,438,2349,1,6,2405,true,"bronze"),
		IRON(440,-1,2351,15,13,2406,false,"iron"),
		STEEL(440,453,2353,30,18,2409,true,"steel"),
		GOLD(444,-1,2357,40,23,2410,false,"gold"),
		MITHRIL(447,453,2359,50,30,2411,true,"mithril"),
		ADAMANT(449,453,2361,70,38,2412,true,"adamant"),
		RUNE(451,453,2363,85,50,2413,true,"rune");
```

| Bar | Ores | Level | Base XP | Bar item |
| --- | --- | --- | --- | --- |
| Bronze | copper 436 + tin 438 | 1 | 6 | 2349 |
| Iron | iron 440 | 15 | 13 | 2351 |
| Silver | silver 442 | 20 | 14 | 2355 |
| Steel | iron 440 + coal 453 | 30 | 18 | 2353 |
| Gold | gold 444 | 40 | 23 | 2357 |
| Mithril | mith 447 + coal 453 | 50 | 30 | 2359 |
| Adamant | addy 449 + coal 453 | 70 | 38 | 2361 |
| Runite | rune 451 + coal 453 | 85 | 50 | 2363 |

Furnace object ids include `11666`, `3044`, `2781` (ActionHandler → `Smelting.openInterface`).
Anvils (e.g. Varrock `3228,3435`) turn bars into gear with a hammer (2347).

**The mining → smithing → equipment chain is the bot's self-supply route for melee gear.**

### 3.8 The rest (mechanism located, details follow in the same pattern)

| Skill | Entry method | Notes |
| --- | --- | --- |
| Crafting | `GemCutting.cutGem`, `LeatherMaking.craftLeather`, `JewelryMaking` | chisel 1755; gold + gems |
| Fletching | `Fletching.fletchBow` | knife 946 + logs |
| Herblore | `Herblore.java` | vials, herbs, pestle |
| Runecrafting | `Runecrafting.craftRunes` | talismans, essence (1436) |
| Agility | `Agility.java` (obstacles) | `addSkillXP(360, ...)` per lap-style action |
| Thieving | `Thieving.pickpocketNpc` + stalls | NPCs and market stalls |
| Farming | `Farming.java`, `Patch[] patches` | the `Patch` region concept the tool renders |
| Hunter | `Implings`, hunter areas | Skill Hunter teleport `2775,2887` |
| Construction | `SkillMasters` CONSTRUCTION | house portal, Rimmington |

Every one of these is reachable with a **`Skill` teleport** from
`teleports.cfg` (§2.1), which is exactly why the bot needs a teleport registry.

---

## 4. Combat training

### 4.1 How XP is granted

Damage is converted at the point of the hit. The "controlled"/shared mode spreads across
all four:

```467:470:Proxy Server/src/server/game/players/combat/CombatAssistant.java
			c.getPA().addSkillXP((damage * Config.MELEE_EXP_RATE / 3), 0);
			c.getPA().addSkillXP((damage * Config.MELEE_EXP_RATE / 3), 1);
			c.getPA().addSkillXP((damage * Config.MELEE_EXP_RATE / 3), 2);
			c.getPA().addSkillXP((damage * Config.MELEE_EXP_RATE / 3), 3);
```

and a specific style focuses one skill while Hitpoints gets a third:

```476:477:Proxy Server/src/server/game/players/combat/CombatAssistant.java
			c.getPA().addSkillXP((damage * Config.MELEE_EXP_RATE), c.combatStyle.fightMode);
			c.getPA().addSkillXP((damage * Config.MELEE_EXP_RATE / 3), 3);
```

Ranged grants Ranged + Hitpoints (+ a third to another index); Magic grants the spell's base
XP plus damage-scaled XP:

```503:504:Proxy Server/src/server/game/players/combat/CombatAssistant.java
		c.getPA().addSkillXP((c.MAGIC_SPELLS[c.magic.oldSpellId][7] + damage * Config.MAGIC_EXP_RATE), 6);
		c.getPA().addSkillXP((c.MAGIC_SPELLS[c.magic.oldSpellId][7] + damage * Config.MAGIC_EXP_RATE / 3), 3);
```

**Bot implication:** "which skill am I training" is the **attack style**, so the bot sets
`combatStyle.fightMode` (accurate → Attack, aggressive → Strength, defensive → Defence,
controlled → all). Switching Attack → Strength → Defence at 99 is a fight-mode change, not
a different activity. Ranged/Magic are separate combat loops with their own gear.

### 4.2 Training spots by band

Combine the `SPOTS` table (§1) with the Monster teleports (§2.1):

| Band | Spot | X, Y | Targets | Reach it |
| --- | --- | --- | --- | --- |
| 1–20 | Lumbridge chickens | 3230, 3294 | 41, 1017 | Lumbridge teleport or walk |
| 1–20 | Lumbridge cows | 3253, 3267 | 81, 397, 1766 | Lumbridge |
| 1–25 | Lumbridge goblins | 3244, 3247 | 100–103 | Lumbridge |
| 15–35 | Al Kharid scorpions | 3298, 3296 | 107, 1477 | Glory Al Kharid `3293,3174` |
| 20–40 | Barbarian Village | 3082, 3422 | 141/142/3246/3247 | walk from Edgeville |
| 30–50 | Hobgoblins | 3023, 3472 | 122, 123 | walk |
| 40–60 | Varrock guards | 3212, 3429 | 9, 21, 23 | Varrock teleport |
| 45–70 | Ice Mountain dwarves | 3016, 3451 | 118, 121, 382 | walk |
| 60–80 | Edgeville hill giants | 3117, 9846 | 117, 469 | Edgeville, down the dungeon |
| 60+ | Slayer Tower | 3429, 3538 | task monsters | Monster → Slayer Tower |
| 60+ | Taverley Dungeon | 2926, 2910 | dragons, demons | Monster → Dungeons |
| 70+ | Brimhaven Dungeon | (Brimhaven) | metal dragons | Karamja teleport + walk |
| 85+ | Kalphite Lair | (desert) | 1156 | Monster → Desert `3283,3329` |

**Magic** is best trained at South Varrock wizards (172, 174) or by splashing; **Ranged** at
the cow ranging spot or hill giants. The safest early Magic/Ranged training is on cows.

### 4.3 Loot — always pick up, always bank

Drop tables live in `Data/cfg/npc_drops.cfg`:

```
drop = 1183	556:20:COMMON	555:20:COMMON	995:10000:RARE	995:25000:VERY_RARE	...
```

Format is `itemId:amount:rarity` (or `itemId:amountStart:amountEnd:rarity`), with rarities
`COMMON`, `UNCOMMON`, `RARE`, `VERY_RARE`, `SUPER_RARE`, `ALWAYS`. **Coins are item 995**
— a valuable kill can drop 10,000–25,000 coins, which is the bot's upgrade budget.

The rule you asked for — **loot everything, then bank** — is a fixed part of every combat
routine:

```
Repeat(Selector(
    KillNearest(target),
    LootAllGroundItems(),     // pick up every drop
    BankWhenFull()            // walk to nearest bank, deposit all
))
```

Ground items are created via `Server.itemHandler.createGroundItem` and removed on pickup,
so "loot all" is a scan of ground items within a small radius, then a `WalkTo` the bank.

---

## 5. Shops — buying upgrades with looted coins

### 5.1 How shops work

Merchants are mapped NPC → shop in `Data/cfg/npc-shops.cfg`, and shops are opened through
`ActionHandler`:

```
npc-shop = 1301 81
npc-shop = 537 77
npc-shop = 675 76
...
```

Shop contents live in `Data/cfg/shops.cfg` as `shop = <id> <Name> <flags> <currency> <itemId> <stock> ...`.
Examples relevant to a bot:

| Shop | Id | Notable stock |
| --- | --- | --- |
| General_Store | 2 | tinderbox 590, chisel 1755, hammer 2347, bronze axe 1351, fishing items |
| Woodcutting_Store | 16 | bronze→rune axes (1351/1349/1353/1355/1357/1359) |
| Masterfisher's_Supplies | 10 | nets, rods, harpoon, bait |
| Smithing_Store | 62 | hammer 2347, ores 437/439 |
| Cooking_Store | 66 | cooking gear, flour 1949 |
| Firemaking_Store | 65 | tinderbox 590, bows |
| Horvik's_Armour_Shop | 8 | bronze→iron armour (1153/1155/1115/1067…) |
| Weapon_Shop | 11 | bronze→rune weapons (1205/1323/1333/1215/4587…) |
| Varrock_Sword_Shop | 4 | scimitars/longswords by tier |
| Lowe's_Archery_Emporium | 7 | bows, arrows, dragonhide |
| Aubury's_Rune_Shop | 6 | all runes (554–565), staves |

### 5.2 The bot's shopping loop

```
if (needsUpgrade() && coins >= price(nextTier))
    walkTo(shopNpc);
    openShop(shopId);
    buy(item, qty);
    equip(item);
```

`needsUpgrade()` comes from **equipment requirements** (§6): the bot buys the best item
whose requirement it meets and whose tier exceeds what it is wearing. This is what makes
looted coins matter — combat funds skilling and vice versa.

Note many "Skill X" teleports (§2.1) land near the matching skill master/shop, so
`Teleport(Skill.Smithing)` → shop is often shorter than walking.

---

## 6. Equipment — requirements and progression

Requirements are per-item and enforced on equip:

```1445:1469:Proxy Server/src/server/game/items/ItemAssistant.java
						if(c.getPA().getLevelForXP(c.skills.playerXP[1]) < Server.itemHandler.ItemList[wearID].req[1]) {
						if(c.getPA().getLevelForXP(c.skills.playerXP[4]) < Server.itemHandler.ItemList[wearID].req[4]) {
						if(c.getPA().getLevelForXP(c.skills.playerXP[6]) < Server.itemHandler.ItemList[wearID].req[6]) {
						if(c.getPA().getLevelForXP(c.skills.playerXP[0]) < Server.itemHandler.ItemList[wearID].req[0]) {
						if(c.getPA().getLevelForXP(c.skills.playerXP[2]) < Server.itemHandler.ItemList[wearID].req[2]) {
```

So each item has a `req[]` array (attack, strength, defence, ranged, magic) parsed from
`Data/cfg/item.cfg`, one line per item:

```
item = 2	Cannonball	Ammo_for_the_Dwarf_Cannon.	5	5	5	0	0	0	...
```

| Index | Skill | Example gate |
| --- | --- | --- |
| req[0] | Attack | rune weapons (40) |
| req[1] | Strength | some weapons |
| req[2] | Defence | armour tiers (platebody per metal) |
| req[4] | Ranged | bows, dragonhide |
| req[6] | Magic | staves, robes |

**Progression (metal tiers; exact reqs read from `item.cfg`):** bronze → iron → steel →
black → mithril → adamant → rune. Tools follow the same ladder (§3.1, §3.2). A bot's gear
plan is a list of `(itemId, requiredLevel, source)` sorted ascending; it equips the highest
entry it qualifies for.

**Style gear:** melee = weapon + full metal; ranged = bow/crossbow + leather→dragonhide;
magic = staff + robes. The bot **switches style at 99** only if it owns the gear for the
next one (§7).

---

## 7. Slayer — the task loop

### 7.1 The master

The task master is **NPC 1597** ("Vannaka"/"Slayertasker"), spawned at:

```5:6:Proxy Server/Data/cfg/spawn-config.cfg
spawn = 1599	2874	2982	0	1	0	0	0	1	Slayer
spawn = 1597	2871	2982	0	0	0	0	1	0	Slayertasker
```

so **`2871, 2982`** — and the **`Skill Slayer 2873,2980`** teleport lands right on top of it.
(The 1599 NPC is the slayer *skillcape* master.)

Talking to 1597 runs the dialogue → `generateTask()`:

```229:239:Proxy Server/src/server/content/skills/Slayer.java
    public void generateTask() {
        if (hasTask() && !c.needsNewTask) {
            c.getDH().sendDialogues(407, 1597);
            return;
        }
```

### 7.2 Difficulty and amount

Difficulty is chosen from **combat level**, so a bot naturally progresses:

```346:353:Proxy Server/src/server/content/skills/Slayer.java
    public int getSlayerDifficulty() {
        if (c.combatLevel > 0 && c.combatLevel <= 45) {
            return EASY_TASK;
        } else if (c.combatLevel > 45 && c.combatLevel <= 90) {
            return MEDIUM_TASK;
        } else if (c.combatLevel > 90) {
            return HARD_TASK;
        }
```

Amounts: easy 25–30, medium 30–40, hard 30–50 (`getTaskAmount`).

### 7.3 The task table

The full task list lives in `Slayer.Task` (`npcId, levelReq, difficulty, location`):

```35:89:Proxy Server/src/server/content/skills/Slayer.java
        ABERRANT_SPECTRE(1604, 60, 2, "Slayer Tower"),
        ABYSSAL_DEMON(1615, 85,3, "Slayer Tower"),
        BANSHEE(1612, 15, 2, "Slayer Tower"),
        BASILISK(1616, 40, 2, "Fremennik Slayer Dungeon"),
        CRAWLING_HAND(1648, 5, 1,"Slayer Tower"),
        DUST_DEVIL(1624, 65, 2, "Slayer Tower"),
        GARGOYLE(1610, 75, 3, "Slayer Tower"),
```

The **location string** maps to a teleport from §2.1:

| Task location (as written) | Nearest teleport |
| --- | --- |
| Slayer Tower | `Monster Slayer Tower 3429 3538` |
| Taverley Dungeon | `Monster Dungeons 2926 2910` |
| Edgeville Dungeon | Edgeville `3094,3492` (+ dungeon entrance) |
| Brimhaven Dungeon | Karamja `2925,3171` then walk |
| Fremennik Slayer Dungeon | `Monster Snow Mountain 2834 3518` |
| Kalphite Lair | `Monster Desert 3283 3329` |
| Asgarnian Ice Caves | `Monster Snow Mountain 2834 3518` |
| Waterbirth Island | (island transport) |
| Canifis | (walk / Kharyrll ancient teleport `3492,3471`) |
| The Wilderness | (PK/lever) |

> **Follow-up:** the mapping above is inferred from location names. The validator
> (`BOT_TOOLING.md` T1) should confirm each by checking that task-monster spawns exist
> near the teleport landing tile.

### 7.4 Slayer-only monsters

Some monsters cannot be damaged without the slayer level (checked on attack):

```36:36:Proxy Server/src/server/game/players/combat/CombatAssistant.java
	public int[][] slayerReqs = {{1648,5},{1612,15},{1643,45},{1618,50},{1624,65},{1610,75},{1613,80},{1615,85},{2783,90}};
```

| Monster | NPC id | Slayer level |
| --- | --- | --- |
| Crawling hand | 1648 | 5 |
| Banshee | 1612 | 15 |
| Infernal mage | 1643 | 45 |
| Bloodveld | 1618 | 50 |
| Dust devil | 1624 | 65 |
| Gargoyle | 1610 | 75 |
| Nechryael | 1613 | 80 |
| Abyssal demon | 1615 | 85 |
| Dark beast | 2783 | 90 |

### 7.5 Progress, completion, XP and points

On each kill of the assigned monster:

```3472:3494:Proxy Server/src/server/game/npcs/NPCHandler.java
	public void appendSlayerExperience(int i) {
		Client c = (Client) PlayerHandler.players[npcs[i].killedBy];
		if (c != null) {
			if (c.getSlayer().isSlayerTask(npcs[i].npcType)) {
				c.taskAmount--;
				c.getPA().addSkillXP(npcs[i].MaxHP * Config.SLAYER_EXPERIENCE, 18);
				if (c.taskAmount <= 0) {
					c.getPA().addSkillXP((npcs[i].MaxHP * 8) * Config.SLAYER_EXPERIENCE, 18);
					int points = c.getSlayer().getDifficulty(c.slayerTask) * 4;
					c.slayerTask = -1;
					c.slayerPoints += points;
```

So: **per kill** = `MaxHP × 19`; **completion bonus** = `MaxHP × 8 × 19`; **points** =
`difficulty × 4` (4 / 8 / 12). Then the bot returns to 1597 for the next task.

Points spend (`Slayer.java`): 50 pts → 32,500 XP; 35 → slayer darts; 25 → broad arrows or
respite; 30 → cancel task; 100 → block a task.

### 7.6 The Slayer state machine

```
Repeat(
    Sequence(
        WalkToOrTeleport(Skill.Slayer),   // 2873,2980
        TalkTo(1597),                     // assign/collect task
        Repeat(
            Sequence(
                TravelTo(taskLocation),   // §7.3 teleport mapping
                KillTaskMonsters(),
                LootAll(),
                BankIfFull(),             // bank loot + resupply
                CheckTaskAmountDone()
            )
        ),
        ReturnToMaster()                  // task done -> new task
    )
)
```

Required items per task (spiny helmet, mirror shield, etc.) are **not** yet modelled in
`Slayer.Task` — it only carries `npcId, levelReq, difficulty, location`. That is a genuine
gap: the task table needs a `requiredItems` column before the bot can self-serve harder
tasks. Flagged below.

---

## 8. The "all skills to 99" plan

Your goal — level every skill to 99, then switch style — becomes a **priority scheduler**
over the routines above:

1. **Skill plan** — an ordered list with an enable flag and a target level (default 99).
2. **Per-skill routine** — the `Repeat(Sequence(travel, gather, process, bank))` from §0.
3. **Completion check** — when `getLevelForXP(xp) >= target`, mark the skill done and move
   to the next.
4. **Style rotation** — for combat, when the current style hits 99, set a new
   `combatStyle.fightMode` (Attack → Strength → Defence) **if the bot has the gear**;
   otherwise fall through to the next non-combat skill.
5. **Resource coupling** — mining→smithing→gear, fishing→cooking→food, woodcutting→
   firemaking→fletching, combat→bones→prayer, combat→coins→shops. The plan should exploit
   these rather than treat skills independently.

**Skillcape at 99:** each master sells a cape for 99,000 coins once the skill is 99
(`SkillMasters.addSkillCape`, cape ids in the enum, e.g. WC 9807). A nice milestone the bot
can buy.

---

## 9. What the `Locations` service must gain

Slice 1 hardcodes tiles; everything here needs **data**, not code. The additions:

| Registry | Source | Example entry |
| --- | --- | --- |
| `Teleports` | `teleports.cfg` | `Skill Slayer -> 2873,2980` |
| `Shops` | `shops.cfg` + `npc-shops.cfg` + spawn coords | `Horvik's Armour -> npc ?, 3228,3435` |
| `Resources` | `ObjectDef.actions` + object maps | `tree.oak @ 3277,3426` |
| `Monsters` | `spawn-config.cfg` + `npc.cfg` | `hill giant 117 @ 3117,9846` |
| `Banks` | object/bank booths | `lumbridge 3208,3220` |
| `SlayerMasters` | `Slayer.Task` + spawns | `Vannaka 1597 @ 2871,2982` |
| `Skills` | this document | per-skill object/tool/level/xp |

All of these are exactly what `BOT_TOOLING.md`'s map layers render and let authors edit.
Building this registry is the bridge between the slice-1 bot and the "custom bot"
tool.

---

## 10. Gaps and follow-ups (honest list)

- **Per-task required items are missing** from `Slayer.Task` — needed for hard tasks
  (mirror shield, earmuffs, spiny helmet, ice gloves, bag of salt). **Addressed in
  `BOT_LOCATIONS.md` Part C.3** — and, verified there, the server does **not** enforce
  them in combat, so the column is informational until the effects are implemented.
- **`npc-shops.cfg` is a partial extraction** ("extracted from ActionHandler.java") — the
  authoritative NPC→shop map is in `ActionHandler`, so the tool must parse that (line 928
  shows a table-driven `openShop(shopId)`).
- **Shop NPC coordinates** need joining from `spawn-config.cfg` — not done here.
- **Slayer location → teleport** mapping (§7.3) is inferred and should be validated.
  **Now verified in `BOT_LOCATIONS.md` Part C.1** (✅ confirmations and ⚠️ multi-leg rows).
- **Brimhaven / Waterbirth / Canifis** have no direct teleport — routing needs the boat/
  walk legs modelled.
- **Random events** (Spirit Tree, Rock Golem, River Troll, Zombie) can interrupt any
  gathering routine; each leaf must be recoverable.
- **Items in `Data/cfg/item.cfg`** must be parsed for gear `req[]` and prices to drive
  the upgrade planner.

---

## 11. Acceptance criteria

- A bot can be told a skill and reach its location: `Locations` resolves a skill → a
  `(x,y,plane)` via a teleport it can cast, and the bot walks the remainder.
- A bot mines ore, smelts a bar at a furnace, smiths an item at an anvil, and equips it.
- A bot fights by level band from §4.2, loots **every** drop, and banks when full.
- A bot buys its next tool/armour tier with looted coins from the correct shop NPC.
- A bot gets a Slayer task from 1597, travels to the task location, kills to completion,
  banks, and returns for a new task — with the correct slayer level gates respected.
- A skill plan runs multiple skills to 99 and rotates combat style at 99 when geared.
