# BOT_LOCATIONS.md — the bot's data layer

Three design deliverables in one place, because they are all "data the bot reads, never
code it learns":

- **A — the `Locations` service**: the single source of "where in the world is X".
- **B — the equipment planner**: pick, buy and equip the best gear the bot qualifies for.
- **C — the Slayer registry**: tasks, verified monster locations, XP, and the items gap.

Companion to `BOT_ACTIVITIES.md` (the activity analysis this operationalises) and
`BOT_ROADMAP.md` §5.2 (where `Locator`/`Locations` were first sketched).

---

# Part A — the `Locations` service

## A.1 What problem it solves

Slice 1 hardcodes `(treeX, treeY)`. Every activity in `BOT_ACTIVITIES.md` needs *named*
places instead: "nearest oak", "a bank", "the Slayer master", "the shop that sells rune
scimitars", "the teleport to the Slayer Tower". `Locations` is the one place those live,
so adding a resource is a data row, not a code change in every bot
(`BOT_ROADMAP.md` §5.2).

## A.2 The shape

```java
package server.game.bots.world;

/** A named point or region, on one plane. */
public final class Location {
    public final String name;      // "draynor_willows"
    public final Kind kind;        // TREE, ROCK, FISH, BANK, SHOP, ALTAR, TELEPORT, MONSTER, MASTER
    public final int x, y, plane;
    public final int radius;       // 0 = exact tile; >0 = a region (see RandomTileIn)
    public final String[] tags;    // "oak", "rune", "task:gargoyle"
}

public interface Locator<T extends Location> {
    /** Nearest entries to (x,y,plane), closest first, capped at limit. */
    List<T> nearest(int x, int y, int plane, int limit);
    /** Named lookup; null if unknown. */
    Location byName(String name);
}
```

Two implementations, in the order the roadmap already prescribed:

- **`CuratedLocator`** — reads the data files below. Correct, O(1), the default.
- **`ScannedLocator`** — scans `ObjectManager` / `NPCHandler` on demand. Flexible, costs
  per-tick work, so it is a **fallback** and its results are cached (see A.5).

`Locations` is the facade that holds one locator per family (`trees()`, `banks()`,
`shops()`, `teleports()`, `monsters()`, `masters()`), so a bot asks
`locations.banks().nearest(x, y, 0, 1)` and gets an answer regardless of which locator
served it.

## A.3 Data files

One new file, parsed at startup like everything else in `Data/cfg`. Format is the
`locations.cfg` the tooling already specified (`BOT_TOOLING.md` Layer 2), extended with
the families this analysis found:

```
# Data/cfg/bots/locations.cfg
# region = <name>  x <x> y <y>  w <w> h <h>  plane <p>  kind <kind>  [tags]

region = draynor_bank      x 3092 y 3243  w 6 h 5  plane 0  kind bank
region = varrock_anvil     x 3228 y 3435  w 1 h 1  plane 0  kind anvil
point  = slayer_master     x 2871 y 2982  plane 0  kind master  tags vannaka
point  = slayer_tower      x 3429 y 3538  plane 0  kind teleport  tags monster
point  = varrock_east_mine x 3285 y 3366  plane 0  kind mine   tags copper,tin,iron
point  = varrock_oaks      x 3277 y 3426  plane 0  kind tree   tags oak
```

The **teleports are not duplicated** — `Data/cfg/teleports.cfg` already exists and is
authoritative. `Locations` parses it directly (the format is
`teleport = <Category> <Name> <X> <Y>`), so that file stays the single source of truth and
the editor shows it read-only.

Likewise **shops and monsters are joined from existing files** rather than copied:

| Family | Primary source | Join |
| --- | --- | --- |
| Teleports | `Data/cfg/teleports.cfg` | — |
| Shops | `Data/cfg/shops.cfg` | NPC → shop from `ShopNpcs.java` / `npc-shops.cfg`; NPC coords from `spawn-config.cfg` |
| Monsters | `Data/cfg/spawn-config.cfg` | HP/level from `npc.cfg` |
| Resources | `locations.cfg` (new) | object ids from `ObjectDef.actions` |
| Banks / altars / anvils / ranges | `locations.cfg` (new) | — |
| Masters | `locations.cfg` (new) | — |

This keeps the bot's world-model derived from the server's own data, so the two cannot
drift — the same principle as `BOT_TOOLING.md` §2.

## A.4 Resolution order

For any request:

1. **Curated** (`CuratedLocator`) — return if it has an answer.
2. **Cache** — a per-family map keyed by `(family, plane, regionId)`; checked before any
   scan.
3. **Scan** (`ScannedLocator`) — only on a cache miss, results cached.

This is the roadmap's "curated first, scan as fallback" rule made concrete, and it is what
keeps per-tick work bounded (`BOT_ROADMAP.md` §7).

## A.5 Cache and invalidation

A scan result is cached per 64×64 region and **invalidated on the events that change it**:

- `ObjectHandler`/`Object` create or remove (chopped tree, mined rock, fire)
- `NPCHandler` spawn/death
- region load/unload

For slice-2 and beyond, the cheap version is a **time-based expiry** (e.g. re-scan a
region at most once per N ticks); the tight version hooks the object/NPC events. Start
time-based, tighten later — do not couple the first cut to every mutation site.

## A.6 Planes and dungeons

`Location` carries `plane` and the `(x,y)` are **absolute**, as the server uses them. But
dungeons are not contiguous with the surface (hill giants at `3117,9846`, Fremennik
Slayer Dungeon at `y≈10000`), so:

- a locator never returns a cross-plane nearest — the bot must `plane`-match;
- travel between planes is a **leg** (`WalkTo(dungeonEntrance) → ClimbDown → WalkTo`), which
  is a `Sequence`, not a single `WalkTo`. This is exactly the gotcha `BOT_TOOLING.md` §9
  warned about.

## A.7 Region waypoints reuse this

`RandomTileIn` (`BOT_ROADMAP.md` §5.2) is just "a `Location` with `radius > 0`": the
bot resolves the region to a random walkable tile via `SmartPathFinder.canStep`, seeded
per bot. So the editor's drag-a-box waypoint and the runtime are the same object — no
second concept.

---

# Part B — the equipment planner

## B.1 Where requirements actually live (correction)

Requirements are **not** read from `Data/cfg/item.cfg`. They are computed in code, by
**matching the item's name**:

```293:295:Proxy Server/src/server/world/ItemHandler.java
	public int[] getRequirements(String itemName, int itemId) {
	int[] req = new int[24];
	req[0] = 0; req[1] = 0; req[2] = 0; req[3] = 0; req[4] = 0; req[5] = 0; req[6] = 0;
```

The array is **indexed by skill id**, which the equip check confirms:

```1444:1470:Proxy Server/src/server/game/items/ItemAssistant.java
					if(Server.itemHandler.ItemList[wearID].req[1] > 0) {
						if(c.getPA().getLevelForXP(c.skills.playerXP[1]) < ...req[1]) {
							c.sendMessage("You need a defence level of "+...);
						if(c.getPA().getLevelForXP(c.skills.playerXP[4]) < ...req[4]) {   // ranged
						if(c.getPA().getLevelForXP(c.skills.playerXP[6]) < ...req[6]) {   // magic
						if(c.getPA().getLevelForXP(c.skills.playerXP[0]) < ...req[0]) {   // attack
						if(c.getPA().getLevelForXP(c.skills.playerXP[2]) < ...req[2]) {   // strength
```

| `req` index | Skill | Set for |
| --- | --- | --- |
| 0 | Attack | weapons (and armour — see quirk) |
| 1 | Defence | armour |
| 2 | Strength | some weapons (e.g. tzhaar-ket-om) |
| 4 | Ranged | bows, d'hide |
| 6 | Magic | mystic, infinity, splitbark, staves |

**The metal ladder as implemented** (`getRequirements`, name-match):

| Tier | Weapons `req[0]` | Armour |
| --- | --- | --- |
| Bronze | 1 | `req[0] = req[1] = 1` |
| Iron | 1 | `req[0] = req[1] = 1` |
| Steel | 5 | `req[0] = req[1] = 5` |
| Black | 10 | `req[0] = req[1] = 10` |
| Mithril | 20 | `req[0] = req[1] = 20` |
| Adamant | 30 | `req[0] = req[1] = 30` |
| Rune | 40 | `req[1] = 40` |

**Quirk to replicate exactly:** for most tiers the *armour* branch sets **both** `req[0]`
and `req[1]`, so this server requires **Attack *and* Defence** to wear e.g. mithril plate —
unlike retail. Rune is the exception (`req[1]` only). A planner that assumed retail rules
would tell a bot to buy armour it cannot actually wear. **Do not re-derive the rules; read
`ItemHandler.ItemList[id].req`.**

## B.2 The planner

```java
package server.game.bots.gear;

public final class GearSlot {
    final int itemId;          // 1351
    final String name;         // "Bronze axe"
    final int slot;            // equipment slot
    final int[] req;           // = ItemHandler.ItemList[itemId].req (authoritative)
    final int tier;            // ordering score, derived from req + bonuses
    final Source source;       // SHOP(shopId, price) | DROP(npcId) | MADE
}

public interface EquipmentPlanner {
    /** Best equippable item for a slot given the bot's levels, or null. */
    GearSlot bestFor(int slot, Skills s, ItemSet owned);
    /** The next item to acquire: better than worn, requirements met, can afford/obtain. */
    GearSlot nextUpgrade(int slot, Client c);
}
```

`GearSlot.req` is copied straight from `ItemHandler`, so the planner can never disagree
with `wearItem` at equip time. `bestFor` filters to items where every `req[i] > 0` is met
by `getLevelForXP(playerXP[i])` — the exact predicate `wearItem` uses.

## B.3 Acquisition — shops and drops

**Price source (verified).** Buy price is `ShopAssistant.getItemShopValue(id, 0, slot)`,
which reads `Server.itemHandler.ItemList[id].ShopValue` — the **first numeric field after
the description in `item.cfg`** (set in `ItemHandler.newItemList(..., ShopValue, LowAlch,
HighAlch, ...)`). Sell price is `ShopValue * 0.80` (`sellToShopPrice`). There is no markup
on buy — the `*1.35` is commented out — so the affordability check is simply:

```java
int price = (int) Server.itemHandler.ItemList[itemId].ShopValue;
if (bot.coins >= price) buy();
```

**There is an Edgeville shop mall at spawn (verified).** The start spawn is Edgeville
`3087,3505`, and the merchant NPCs for the core gear shops stand within ~10 tiles of it —
so a bot needs **no travel** to re-gear:

| Shop (`shops.cfg`) | Opens via | NPC | Verified tile | Click |
| --- | --- | --- | --- | --- |
| 2 General Store | `ShopNpcs` SECOND | 520 Shop_keeper | `3077,3508` | second |
| 6 Aubury's Runes | SECOND | 553 Aubury | `3082,3512` | second |
| 7 Lowe's Archery | SECOND | 550 Lowe | `3078,3512` | second |
| 8 Horvik's Armour | FIRST | 549 Horvik | `3080,3509` | **first** |
| 11 Weapon Shop | SECOND | 692 Tribal_Weapon_Salesman | `3080,3512` | second |
| 14 Magician's Robes | SECOND | 1658 Robe_Store_owner | `3083,3510` | second |
| 24 Thessalia's Clothing | SECOND | 548 Thessalia | `3083,3508` | second |

Two caveats from the data:

- **NPC → shop lives in `ShopNpcs.java`** (the `FIRST_CLICK`/`SECOND_CLICK` tables) and
  `npc-shops.cfg`, **not** in `shops.cfg`. The click type matters: Horvik is `first`,
  everything else above is `second`. The bot must use the right click.
- **Axes above bronze and the pure skill stores are button-only.** The Woodcutting Store
  (16, which holds every axe), Magician's Robes (also 14), and the skill-master stores
  (`67` Defence … `72` Prayer) are opened from `ClickingButtons`, with **no NPC** — the bot
  needs the interface-button path for those, or buys bronze from the General Store and
  upgrades via drops.
- **Pickaxes** come from Nulodion (shop 22), NPC 209 at `3010,3452`, second click.



```
next = planner.nextUpgrade(slot, bot)
if next.source == SHOP and bot.coins >= next.price:
    Locations.shops().nearest(...) -> WalkTo -> openShop(shopId) -> buy -> equip
else:
    continue the current training/gathering routine   // drops are the other path
```

## B.4 Style switching at 99

Because requirements are just `req[]` numbers, "can I switch combat style" is
`planner.bestFor(WEAPON_SLOT, stats, owned)` returning a weapon for the *other* style
(i.e. one whose `req[4]` or `req[6]` is met). If none qualifies, the bot stays on its
current style and moves to the next skill in the plan (`BOT_ACTIVITIES.md` §8). This makes
your "switch now and then if they have equipment for it" precise and checkable.

---

# Part C — the Slayer registry

## C.1 Verified locations

`Slayer.Task` carries a location *string*; the spawns are real and I verified they sit near
the matching teleport:

| Task location | Verified spawn | Teleport (from `teleports.cfg`) | Leg | Verified |
| --- | --- | --- | --- | --- |
| **Slayer Tower** | banshee `3439,3562` (p0), bloodveld/dust devil (p1), gargoyle/nechryael/abyssal (p2) | `Monster Slayer Tower 3429 3538` | **climb stairs ×2** — tower has 3 planes | ✅ teleport lands at the tower |
| **Fremennik Slayer Dungeon** | cave crawler `2786,9998`, pyrefiend `2758,10011`, basilisk, cockatrice, rockslug, turoth, kurask | `Monster Snow Mountain 2834 3518` | walk → **cave entrance → descend** (interior `y≈10000`) | ✅ spawns real, entrance leg |
| **Brimhaven Dungeon** | bronze dragon `2734,9480`, iron dragon `2737,9418` | `Fishing Karamja 2925 3171` | walk Karamja → **dungeon entrance → descend** (`y+6300` offset) | ✅ |
| **Kalphite Lair** | kalphite worker `3507,9519` | `Monster Desert 3283 3329` | walk → **lair hole → descend** | ✅ |
| **Lumbridge Swamp Caves** | cave crawler `3189,9569`, rockslug `3207,9589` | `Modern Lumbridge 3222 3218` | walk south → **cave entrance → descend** | ✅ |
| **Taverley Dungeon** | black demon 84, hellhound 49 | `Monster Dungeons 2926 2910` | walk → **dungeon entrance** | ⚠️ confirm entrance tile |
| **Waterbirth Island** | dagannoth `2445,10148` | `Boss Dag Kings 2547 3758` | boat leg | ⚠️ island transport |
| **Edgeville Dungeon** | earth warrior 124 | `Glory Edgeville 3087 3500` | ladder (near spawn) | ⚠️ confirm ladder tile |

**Two tasks cannot be completed on this server — no spawns exist.** A search of
`spawn-config.cfg` for **aberrant spectre (1604)** and **cave horror (4353)** returns
nothing (and no name match for either). The data model must therefore let a task be marked
unavailable, and the bot must **skip / block those tasks** — otherwise it takes a task it
can never finish and loops forever. This is the Slayer equivalent of the "target region is
empty" check in `BOT_WORKSHOP_UX.md` §5.5.

The ✅ rows are confirmed from real spawns; the ⚠️ rows need one verified travel leg
(dungeon entrance / boat), which is the multi-leg `Sequence` from Part A.6.

**Plane handling matters here.** Slayer Tower is not one map: banshee is plane 0,
bloodveld/dust devil plane 1, gargoyle/nechryael/abyssal plane 2. A bot sent to
"the Slayer Tower" must climb to the task's plane — the `Location` for each monster must
carry its plane, and the route is `Teleport → WalkTo(stairs) → ClimbUp → WalkTo(monster)`.

## C.2 The task table with HP → XP

XP is `MaxHP × 19` per kill, plus `MaxHP × 8 × 19` on completion
(`NPCHandler.appendSlayerExperience`). HP comes from `npc.cfg` (`npc = <id> <Name> <level> <hp>`):

| Task | NPC id | Slayer lvl | Combat lvl | HP | XP/kill | Completion XP | Points | Difficulty |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Crawling hand | 1648 | 5 | 8 | 16 | 304 | 2,432 | 4 | easy |
| Cave crawler | 1600 | 10 | 23 | 22 | 418 | 3,344 | 4 | easy |
| Banshee | 1612 | 15 | 23 | 22 | 418 | 3,344 | 8 | medium |
| Rockslug | 1622 | 20 | 29 | 27 | 513 | 4,104 | 4 | easy |
| Cockatrice | 1620 | 25 | 37 | 37 | 703 | 5,624 | 8 | medium |
| Pyrefiend | 1633 | 30 | 43 | 45 | 855 | 6,840 | 4 | easy |
| Basilisk | 1616 | 40 | 61 | 75 | 1,425 | 11,400 | 8 | medium |
| Infernal mage | 1643 | 45 | — | — | — | — | 8 | medium |
| Bloodveld | 1618 | 50 | 76 | 120 | 2,280 | 18,240 | 8 | medium |
| Jelly | 1637 | 52 | — | — | — | — | 8 | medium |
| Turoth | 1632 | 55 | 89 | 81 | 1,539 | 12,312 | 8 | medium |
| Cave horror | 4353 | 58 | 80 | 100 | 1,900 | 15,200 | 8 | medium |
| Aberrant spectre | 1604 | 60 | 96 | 90 | 1,710 | 13,680 | 8 | medium |
| Dust devil | 1624 | 65 | 93 | 105 | 1,995 | 15,960 | 8 | medium |
| Kurask | 1608 | 70 | 106 | 97 | 1,843 | 14,744 | 12 | hard |
| Gargoyle | 1610 | 75 | 111 | 105 | 1,995 | 15,960 | 12 | hard |
| Nechryael | 1613 | 80 | 115 | 105 | 1,995 | 15,960 | 12 | hard |
| Abyssal demon | 1615 | 85 | 124 | 150 | 2,850 | 22,800 | 12 | hard |
| Dark beast | 2783 | 90 | 182 | 220 | 4,180 | 33,440 | 12 | hard |

Non-slayer-gated tasks (dragons, giants, demons, skeletons) use the same formula; HP is in
`npc.cfg` (hill giant 117 = 35 HP, green dragon 941 = 85, blue dragon 55 = 109, black
dragon 54 = 199, hellhound 49 = 116, greater demon 83 = 87…).

## C.3 The required-items gap — and what is actually true

`Slayer.Task` is `(npcId, levelReq, difficulty, location)`. It has **no items column**, so
the design must add one:

```java
enum Task {
    //  npcId, levelReq, difficulty, location,            requiredItems
    BANSHEE   (1612, 15, 2, "Slayer Tower",            new int[]{4166}),       // earmuffs
    COCKATRICE(1620, 25, 2, "Fremennik Slayer Dungeon", new int[]{4156}),      // mirror shield
    ROCKSLUG  (1622, 20, 1, "Fremennik Slayer Dungeon", new int[]{4161}),      // bag of salt
    DUST_DEVIL(1624, 65, 2, "Slayer Tower",            new int[]{4168}),       // facemask/nose peg
    GARGOYLE  (1610, 75, 3, "Slayer Tower",            new int[]{4551}),       // rock hammer
    ...
}
```

**But — verified — the server does not enforce them.** A search of `Proxy Server/src` for
the item ids `4156` (mirror shield), `4166` (earmuffs), `4168` (nose peg), `4161` (bag of
salt), `4551` (spiny helmet) finds them only in:

- `SkillInterfaces` — the Slayer **reward shop** menu (`menuLine("1","Bag Of Salt",4161…)`), and
- `ShopAssistant` — a shop entry.

There is **no combat code** that checks them. So:

- **The bot does not need them to function.** It can kill every task monster with plain
  gear.
- The column is still worth adding for **fidelity and future-proofing** — if the effects
  are ever implemented, the bot already knows what to buy — but it must be marked
  *informational*, not a hard requirement, or bots will refuse tasks for no reason.

The reward shop (`slayerPoints`) is the acquisition path for all of them, so a bot that
wants them buys from there with task points (`buySlayerExperience`, `buySlayerDart`,
`buyBroadArrows`, `buyRespite`, and the `SkillInterfaces` menu).

## C.4 The loop, restated with verified data

```
Repeat(Sequence(
    TeleportOrWalk(locations.byName("slayer_master")),   // 2871,2982
    TalkTo(1597),                                        // generateTask()
    Repeat(Sequence(
        TravelToLegs(taskLocation),                      // Part A.6 multi-leg
        KillUntil(taskNpcId, taskAmount),
        LootAll(), BankIfFull(),
        TaskDone(taskAmount)
    )),
    ReturnToMaster()
))
```

Difficulty auto-selects from combat level (`getSlayerDifficulty`), so the bot scales its own
task pool — no configuration needed.

---

# Cross-cutting notes

- **Nothing here is code the bot learns.** It is all data files the server loads and the
  editor (`BOT_TOOLING.md`) renders — the tool's map, region, teleport and shop layers are
  literally these tables.
- **Nothing changes the server's runtime behaviour.** `Locations`, the planner and the
  Slayer registry are read-only consumers of existing loaders.
- **Drift guard.** A parity test should assert that every `GearSlot.req` equals
  `ItemHandler.ItemList[id].req` and that every shop/slayer row resolves to a real file
  row — the same discipline as `WoodcuttingObjectsTest`.

# Open items

Resolved this pass:

- ✅ **Price source** — buy = `ItemList[id].ShopValue` (first numeric in `item.cfg`), sell =
  80% (B.3).
- ✅ **Shop NPC coordinates** — core gear shops verified at the Edgeville mall within ~10
  tiles of spawn, with the exact click type per NPC (B.3).
- ✅ **Slayer location legs** — teleports and spawns confirmed, planes identified; two
  tasks (aberrant spectre 1604, cave horror 4353) marked **unavailable — no spawns** (C.1).

Still open:

- **Dungeon entrance tiles** — the ⚠️ rows in C.1 (Taverley entrance, Waterbirth boat,
  Edgeville ladder) need the exact object tile for the travel leg.
- **`getRequirements` name-match fragility** — e.g. "black" excludes `vamb`/`chap`/`ele'`/
  `beret`; the planner reads the computed array so it inherits the quirks, but a bot that
  *predicts* requirements without the server would get them wrong.
- **XP table completion** — the `—` cells in C.2 (infernal mage 1643, jelly 1637) need
  `npc.cfg` lookups.
- **Button-only stores** — Woodcutting Store (16) and the skill-master stores have no NPC;
  the bot needs the `ClickingButtons` interface path to reach them (B.3).

# Acceptance criteria

- `Locations` resolves, for every family, a named entry and a nearest entry, without a
  per-tick world scan.
- A bot equips the best item per slot whose `req[]` it meets, matching `wearItem` exactly.
- A bot obtains its next upgrade from the correct shop (or a drop), spending looted coins.
- A bot gets a Slayer task, travels the (possibly multi-leg) route to the **verified**
  location, completes it, banks, and returns.
- Deleting `Data/cfg/bots/locations.cfg` degrades the bot to scanned lookups, not a crash.
