# QOL_PLAN.md — Skilling realism, world content and quality-of-life

Status: **Phase 0 landed; Phases 1-8 still design only.**
Scope: the whole QOL list, sequenced. Nothing here overrides an existing server
function; §0 is the mechanism for that and §2 is the contract that makes
"reference an item that does not exist yet" safe.

Phase 0 (below) is implemented and green: `ItemUseRegistry`, `ItemOnObjectRegistry`,
`ItemDefinitions` (the §2 never-null accessor) and the `Config` flag block. The content
validator is the remaining Phase 0 piece.

## The two references, and how they rank

| | Path | Role |
|---|---|---|
| **N1 — Necrotic-Server 1.1.1** | `F:\Download Archive\Necrotic-Server-1.1.1` | **Primary.** The 'Ruse' base: 512 Java files under `com.ruse`, Java 8, Netty 3, definitions as text/JSON, **600 ms tick**. Higher revision than us. This is what we rip from. |
| **N2 — Necrotic on GitHub** | `github.com/NecroticPublic/Necrotic-Server` | Same project, but the committed repo is data + IDE files only. The local N1 copy supersedes it — use N1. |
| **R — 2006Redone** | `F:\Download Archive\2006Redone\2006Redone Server` | **Secondary.** 292 files, package `redone`, no tests, closer to our revision. Used only where it beats N1 at something specific. |

The user's stated intent: they care more about ripping from Necrotic, plan to import
higher-revision content (models, items, e.g. overloads) **one at a time**, and
**nothing must break** while they do. That requirement drives §2 and it is why §0 is
not a formality.

Our server, for reference: `Proxy Server/`, packages `server.*` + `core.*`, JUnit 5 in
`test/` (107 test files), `Test.bat` → `gradlew test`, scheduler `CycleEventHandler` at
**600 ms** (`Config.CYCLE_TIME`) — the same tick as N1, which makes timing ports 1:1.
`Data/cfg/item.cfg` is 1.8 MB / `Config.ITEM_LIMIT = 25000`; N1's `items.txt` is 4.3 MB
with ~22,694 definitions, so **N1 describes a great many items we do not have yet.**

---

## 0. The non-overriding rules

We already have a **strangler-pattern dispatch layer**, built so content can leave the
legacy switches one family at a time:

| Intent | Hook | Duplicate behaviour |
|---|---|---|
| Object click | `ObjectHandler.register(objectType, ObjectClick.FIRST/SECOND/THIRD, action)` | **throws** |
| NPC click | `NpcActionHandler.register(npcType, NpcClick.X, action)` | **throws** |
| Interface button | `ButtonHandler.register(actionButtonId, action)` | **throws** |
| Dialogue | `DialogueRegistry.register(dialogueId, …)` | registry, not switch |

Dispatch runs **before** the legacy switch, and the switch is the fallthrough. Two gaps:

1. **Item use has no registry.** `UseItem.ItemonObject` / `ItemonItem` are single
   ~370-line methods with inline `if`-chains and early `return`s (`UseItem.java:26`,
   `:85`). Fillables, decanting, new crafting pairings would all edit that method —
   exactly what we are avoiding. **Land `ItemUseRegistry` + `ItemOnObjectRegistry`
   first**, modelled on `ObjectHandler` (same `EnumMap`/`ConcurrentHashMap`, same
   throw-on-duplicate).
2. **No collision story against the legacy switch.** The registries catch duplicates
   *within* themselves; nothing stops registering an id the switch still handles, where
   the registry silently wins. Phase 0 adds an audit for it.

Remaining rules:

- **Data lives in `Data/cfg/*.cfg`**; loaders already parse additively, so new content is
  a file edit. Where N1 uses JSON (`world_shops.json`, def json), prefer our `.cfg`
  convention and convert.
- **Every new feature sits behind a `Config` flag**, default off unless you asked for it
  on, following the existing `Config.WORLD_ADVENTURER_ENABLED` precedent.
- **Reference ids are a different revision.** N1's ids come from its own cache and its
  22,694-entry `items.txt`; ours from a 1.8 MB `item.cfg`. **No id is copied blind** —
  see §2.
- **Every ported table gets a test.** N1 has no tests at all; our repo does, and a wrong
  id or coordinate is silent otherwise (that is why `TeleportObjectsTest` exists).
- **Do not copy N1's red flags:** `CommandPacketListener` (142 KB),
  `ObjectActionPacketListener` (67 KB), `Consumables` (74 KB), `Construction` (95 KB)
  are god-class switches; name-string logic (`isPotion` by `"(4)"`, `BowData.forLog` by
  `toString().contains("shortbow")`); unguarded `ShopManager.getShops().get(id)` (NPE);
  the "leave the task running on empty materials" idiom; dead/commented code.

Phase 0 deliverables: `ItemUseRegistry`, `ItemOnObjectRegistry`, the §2 safety accessor,
a `tools/qol` validator, a `Config` flag block, and one `UPDATE_LOG` entry. No gameplay
change.

**Landed (2026-10-10).** `ItemUseRegistry` and `ItemOnObjectRegistry` in
`server.game.players.actions.items`, both consulted by `UseItem` after its predicate
guards and before its inline checks; `ItemOnObjectRegistry` refuses the nine cooking
object ids that `ItemOnObject.processPacket` handles itself, so two handlers can never
run for one click. `ItemDefinitions` in `server.game.items` provides the §2 never-null
lookup. The `Config` flag block is in. 16 new tests; 907 total, 0 failures.

**Still to do for Phase 0:** the `tools/qol` validator — warn for a referenced id with no
definition (that is the legitimate "not imported yet" case), fail for a registration that
collides with an id the legacy code still handles (that is a real conflict). Also open:
whether to harden the existing name-based paths (`Item.getItemName` returning `null`) in
this phase or as its own change; the safe accessor exists so new code never needs them.

---

## 2. Forward-compatible item ids — "add it now, make it work later"

This is the user's explicit new requirement, and it is the highest-value thing in this
plan because it is what makes the later one-at-a-time higher-revision import safe.

**What N1 does right.** `ItemDefinition.forId(int)` **can never return null**:

```27:37,115:119:src/main/java/com/ruse/model/definitions/ItemDefinition.java
private static final int MAX_AMOUNT_OF_ITEMS = 22694;
private static ItemDefinition[] definitions = new ItemDefinition[MAX_AMOUNT_OF_ITEMS];
...
public static ItemDefinition forId(int id) {
    return (id < 0 || id > definitions.length || definitions[id] == null) ? new ItemDefinition() : definitions[id];
}
```

An unknown id resolves to a default object (`name = "None"`, `description = "Null"`,
`stackable = false`, zero bonuses). A skill that references an unregistered id therefore
does nothing harmful, reads as "None", and **starts working the moment a row is added**.
That is precisely the behaviour you asked for.

**What we do today.** Two different answers, and one of them is a live hazard:

- `ItemAssistant.getItemName(int)` (`:2461`) linearly scans `ItemList[]` and returns
  `"Unarmed"` on a miss — safe, though an odd string.
- `Item.getItemName(int)` (`Item.java:18`) returns **`null`** on a miss.

So `Item.getItemName(id).contains(...)` — and there is a lot of name-based logic in this
codebase, including `getRequirements` which is built from `itemName.contains("bronze")`
and similar — **throws NPE the moment a not-yet-defined id reaches it.** N1 avoids this
by never returning null. This is the concrete "don't break the game" gap.

**The fix, in three parts:**

1. **A new accessor, not a change to the old one.** Add
   `ItemDefinitions.name(int)` / `ItemDefinitions.get(int)` that returns a shared
   **unknown sentinel** (name `"Unknown item"`, non-stackable, zero value) instead of
   null — the N1 contract. Existing `getItemName` keeps its current behaviour for
   defined items, so nothing that works today changes identity. New content tables and
   all new name-based logic go through the new accessor.
2. **Harden the existing name-based paths** against a null/unknown id (guard + neutral
   result) rather than rewriting them. This is a small, reviewable sweep; it is also
   what stops a future item import from turning a working feature into an NPE.
3. **A validator that warns, never fails, on unknown ids.** Built as `QolValidatorTest`,
   which runs with the rest of the suite (a separate CLI was not worth a second entry
   point; the tables are Java, so the test *is* the tool):
   - ids in our *new* content tables that resolve to the sentinel → **warn** ("item
     `<id>` referenced by `<table>`; no definition yet — will be inert until defined");
   - ids that resolve to a *real* definition but look wrong (e.g. a log id that is not a
     log) → **warn**;
   - ids that collide with an id the legacy switch still handles → **fail** (that is a
     real conflict, §0.2);
   - duplicate ids inside a registry → **fail** (already enforced at runtime).

That distinction — warn for "not there yet", fail for "two handlers for one id" — is the
whole safety model. It lets us write a row for an item we do not own yet and have the
build stay green, the server stay up, the feature stay inert, and the feature light up
the day the item is defined.

**Correction (found while building the validator).** This section used overload dose
`15333` as the standing example of "an id we reference before we have it". That example
is wrong: `item.cfg` **already defines** the whole potion chain — `15333` is
`Overload_(3)`, the extremes and super prayer are all present, and overload appears twice
under two naming styles. So the overload row is not a future import, it is an import we
have already made, and using it as the exemplar would have let a broken validator pass by
looking up a row that exists. The example is now an id past the end of the table's range
(it runs to 20072), where "no definition" is actually true.

Confirming facts, with the same correction applied:

- Our `ItemList[]` is sized `Config.ITEM_LIMIT = 25000` and indexed by item id, with
  `newItemList` guarding `slot < 0 || slot >= length` and returning silently. Higher
  revision ids fit and cannot corrupt the table.
- So "add the item id" is literally a row in `Data/cfg/item.cfg`; the loader already
  tolerates gaps, and gaps simply resolve to the sentinel.
- N1's own overload chain shows the scale of the future import: doses `15308–15335`,
  `OVERLOAD(15335, 15334, 15333, 15332)` and `OVERLOAD = 5 extremes → 15333`. **All of it
  is already in our `item.cfg`.** The whole potion chain is present — `Recover_special`
  (15300–15303), `Super_antifire` (15304–15307), all four doses of all five extremes
  (15308–15327), `Super_prayer` (15328–15331), and overload twice over: `Overload_(4..1)`
  at 15332–15334 plus a second `Overload(4..1)` at 15335–15338. So there is nothing here
  left to write, and the overload family cannot be the example of an id we reference
  before we have it. A higher-revision id past the table's end (20072) is the real
  example. Writing such a row is exactly the pattern this section protects.

---

## 3. What to take, per source

**From N1 (primary).**

| N1 asset | Verdict | Notes |
|---|---|---|
| `engine/task/Task` + `TaskManager` (owner-keyed tasks, `cancelTasks(key)`) | **Take the design** | Same 600 ms tick as us; maps 1:1 onto `CycleEventHandler`. Our scheduler already has an owner key, so adopt the *idiom* (per-action `Task`, explicit `stop()`), not the class. |
| `TeleportInterface` + `TeleportLocations` + `TeleportType` + `TeleportTabs` | **Take** | The hub in your screenshot. Full detail in §8. |
| `fletching/` incl. `BowData`, `ArrowData`, `BoltData`, `StringingData`, `GemData` | **Take** | Per-action ticked (`Task(2)`), 15 shafts per log. §4. |
| `herblore/` incl. `CombiningDoses`, `FinishedPotions`, `UnfinishedPotions`, `SpecialPotion` (extremes + overload), `Decanting`, `Crushing` | **Take** | §6.2. This is the overload/mixing groundwork you want. |
| `Consumables` — `FoodType` map, `drinkStatPotion`, `getBoostedStat`, `getExtremePotionBoost`, `OverloadPotionTask`, `PrayerRenewalPotionTask` | **Take the helpers, not the switch** | The 74 KB `switch(itemId)` is the anti-pattern; the boost maths is the value. |
| `Sounds.Sound` enum | **Take the shape** | Named catalogue (`TELEPORT({202,201})`, `DRINK_POTION({334})`, `FLETCH_ITEM({375})`, …). Ids are revision-specific — see §9. |
| `MinigameAttributes` per-player holder | **Take as a pattern** | Cleaner than stashing minigame state on `Player`. |
| `ItemDefinition.forId` never-null contract | **Take** | §2. |
| `WalkToTask` / `FinalizedMovementTask` ("interact after arriving") | **Take as a pattern** | |
| `CustomObjects.spawnGlobalObject` / `globalObjectRespawnTask` | **Take** | For pickables/depletion and world events. |
| Construction / Hunter / Summoning / Dungeoneering (~5.7k / 1.1k / 1.6k / 0.8k lines) | **Defer** | Cohesive but each drags `Player` fields and cross-references. Revisit after the QOL pass. |
| `EvilTree`, `ShootingStar` (global world events) | **Optional** | Self-contained (Stopwatch + singleton + `LAST_LOCATION`). Not classic randoms. §5. |
| `BirdNests` (1/60 woodcutting ground nests `5070–5074`) | **Take as data** | We already have a nest drop; this is a fuller loot table. |

**From R (secondary) — only where it beats N1.**

| R asset | Why R wins |
|---|---|
| `items/impl/Fillables.java` | **N1 has no water filling at all.** R has the empty→filled container map and the water-object id list. §5.1. |
| `objects/impl/Pickable.java` + `Searching.java` | R has a reusable `{objectId,itemId}` table and a `{objectIds[],message}` enum. N1 has three hardcoded cabbage/potato cases and no table. §5.2–5.3. |
| `globalworldobjects/*` door/gate face math + `doors.cfg` comments | N1's door system is hardcoded cases with the generic path commented out. §5.4. |
| `content/guilds/Guilds.java` requirement table | **Neither base has skill guilds.** R at least has the table. §6.3. |
| `content/skills/` course/tree/rock/fish tables for breadth | R's tables are closer to our revision than N1's higher-rev ones. §3 (per-skill). |
| `farming/*` | Neither is real; R's is a stub, ours is further along. Do not port either. |

**Do not take from either:** N1's god-class switches, name-string logic, unguarded shop
lookups and task-leak idiom; R's `Smithing.java` (897 lines of
`if (type.equals("1351") && levelReq >= 1)`) and its `ObjectsActions`/`ClickingButtons`/
`DialogueHandler` switch explosions. Take their *numbers* as data.

---

## 4. Per-skill audit

| Skill | Ours today | Best source | Worth taking |
|---|---|---|---|
| **Fletching** | `Fletching.java` — **instant whole inventory**; the shafts path also skipped the level check and the animation, and took its log from the table rather than from the product, so it could consume a log other than the one the interface was opened with. `handleFletchingClick` closes the window instead of looping. | **N1** (ticked, correct 1:15) | Full port of the tick loop and the `BowData`/`ArrowData`/`BoltData`/`StringingData` tables. §5. |
| **Herblore / potions** | `Herblore.java` 8.4 KB; `Potions.java` 73 `case`s; `PotionMixing.mixPotion2` already wired (`UseItem.java:107`). | **N1** (much fuller) | `CombiningDoses`, `FinishedPotions`, `UnfinishedPotions`, `SpecialPotion` (extremes/overload), `Crushing`, `Decanting`. §6.2. |
| **Runecrafting** | **One click binds the whole pack since 2026-10-10**; pure essence (`7936`) was unusable at every altar *and* in every pouch, and the pouch stored `1436` only. See §4g, §4h and §4i. The damaged pouches are the one row still missing. | N1 | The rune/multiplier table is ours and is now pinned by a test; little left to take. |
| **Smithing / Smelting** | Smelting was already ticked (`Smelting` + `SmeltingSession`); the **anvil came off the instant `while (maketimes > 0)` on 2026-10-10** (§4g). The 26.9 KB `Check*` chain and its static fields are still open. | R numbers, N1 shape | Keep our interface; port product/XP/level as a table. N1's `SmithingData` is 55 KB. |
| **Jewellery / Tanning / Leather** | **All off the instant batch as of 2026-10-10.** Tanning and leather went into the creation chatbox (§15); jewellery is ticked with its amount in the chatbox and its onyx-ring id fixed (§4j). Amulet **stringing** is still instant — the one row left. | N1 | `Gems`, `Jewelry`, `Tanning`/`tanningData`, `LeatherMaking`/`leatherData` tables; make ticked. |
| **Crafting** | Has `CraftingData`, `GemCutting`, `JewelryMaking`, `LeatherMaking`, `Tanning`. **Missing Pottery, Glassblowing, Spinning, Soft clay** — yet our guide advertises them (`SkillInterfaces.java:1379-1451`). | **N1** | N1's `crafting/` package (Gems, Jewelry, Flax, leather, tanning). Close the guide/behaviour mismatch. |
| **Agility** | One 14.3 KB `Agility.java`. | **N1** — `ObstacleData.java` is 25.7 KB | Take the obstacle table; biggest single content win. |
| **Cooking** | 5.5 KB. | N1 + R | N1 `cooking/`, R's `Potatoes`. |
| **Firemaking / Fishing / Mining / Woodcutting** | Thin tables (our `Tree_Settings` has 11 rows). | R for coverage, N1 for structure | Fuller log/rock/spot/tree tables. Keep our fixed shared-axe bug. |
| **Prayer** | 6.1 KB, bone bury + altar. | Ours is ahead | Little to take. N1 `PrayerRenewalPotionTask` is potion-adjacent, not Prayer. |
| **Thieving / Slayer** | 9.7 / 18.1 KB — comparable to both. | Diff only | Take missing stalls/tasks if any. |
| **Farming** | Herbs only, Entrana patch only, **in-memory** (`Patch[] p` not persisted). | Neither | Do not port. §12 open question. |
| **Hunter / Construction / Summoning / Dungeoneering** | Absent. | N1 | Defer, per §3. |

Our "**done to full completion**" bar, so this is measurable: every training method has
data; every action is ticked; animations + sounds exist; no guide advertises dead
content (both directions of the `SkillInterfaces` mismatch close); the table is pinned
by a test.

---

## 4b. Crafting gaps — spinning landed

**Landed (2026-10-10), spinning.** The Spinning tab of the Crafting guide has always printed
"1 Wool" and "10 Flax into Bow Strings", and the flax's own examine text tells the player to take
it to a wheel ("I should use this with a spinning wheel."), but there was no wheel handler
anywhere — `Flax.java` only picked the flax, and both products were impling-only. That mattered
more once bow stringing landed, because the only source of a bow string was an impling.

- `Spinning` holds a two-row `Material` table — wool `1737 -> 1759` at level 1, flax `1779 -> 1777`
  at level 10 — which is exactly the pair the guide advertises and exactly OSRS's. XP is Redone's
  and Necrotic's (wool 3, flax 15), paid through `Config.CRAFTING_EXPERIENCE` like every other
  crafting table here.
- Two ways in, registered in the two Phase-0 registries rather than in a switch: a first click on
  the wheel (`ObjectHandler`, ids from `Data/objectSize.cfg` — 2644, 4309, 8748) and using wool or
  flax on the wheel (`ItemOnObjectRegistry`).
- The click spins the single material you are carrying. Carrying **both** it refuses and says so;
  you pick with the item instead. Wool and flax become different items, and a click that silently
  turned a stack of flax into bow strings would be a worse trade than one extra click.
- The action is ticked and repeating — one item per 2-tick cycle until the material runs out —
  on `playerIsCrafting`, the same flag gem cutting and leather use.

**One bug fixed on the way, in shared code.** `CraftingData.resetCrafting` cleared
`playerIsCrafting` but left the looping event queued. The old event then woke up on its next tick,
found the flag set again by a fresh action, and ran alongside it — two loops over one stack, at
double speed. It now stops the spinning event before clearing the flag. `GemCutting` and
`LeatherMaking` still rely on the flag alone, so the same shape is still reachable there; worth
auditing when either is next touched.

---

## 4c. Crafting gaps — pottery landed

**Landed (2026-10-10), pottery, both stages.** The Pottery tab has always listed five items
(1 Pot, 7 Pie Dish, 8 Bowl, 19 Plant Pot, 25 Pot Lid) and all ten ids involved resolve in
`item.cfg`, but nothing read the five *unfired* ids at all — the tab was a menu with no action
behind it. The two fixtures were already named in `Data/objectSize.cfg` ("Potter's Wheel",
"Pottery Oven"), so the world has had them the whole time.

- `Pottery` holds the five rows with **two xp columns**, because clay is two actions: soft clay
  `1761` into unfired clay on the wheel, then unfired clay into the finished item in the oven.
  Shaping is 6.3/15/18/20/20; firing is 6.3/10/15/17.5/20. Levels are the guide's own 1/7/8/19/25,
  which are also the OSRS wheel levels. Redone's `Pottery` agrees on the three rows it implements
  and on the shaping column; its firing half is unreachable (written, but nothing ever calls
  `showFire`), so the second stage is built from the OSRS table, not ported.
- **Firing has no level requirement** — only shaping does. OSRS says so explicitly, and the check
  lives at the wheel. A level-1 player holding an unfired plant pot can finish it.
- Three ways in, all through the Phase-0 registries: a first click on the wheel (2642, 4310), soft
  clay on the wheel, and an unfired item on an oven (2643, 4308, 11601). Both stages are ticked and
  repeating at two cycles, one item at a time, on `playerIsCrafting`.
- **The oven is only reachable by using an item on it.** A plain click is not claimed: the oven
  cannot know which of the five you meant, and 2643 is the object this server hangs
  `JewelryMaking.mouldInterface` on, so taking its click would kill jewellery-making. The pair-keyed
  registry is what makes the stage possible at all — gold bar on 2643 still falls through to the
  switch, and there is a test that says so.
- **The wheel reuses interface 8938**, the five-option chatbox snakeskin leather already uses, and
  therefore the same four buttons per row. That is why `Player.potteryDialogue` exists next to the
  existing `craftDialogue`, and why the two are cleared against each other on open: with one flag,
  a click with both menus having been open would make leather *and* pots. The two tables are pinned
  equal row-for-row by a test, because rows 4 and 5 of Redone's button map do not exist and were
  taken from this server's own snakeskin rows for boots and vambraces.

**Not modelled: cracking.** The oven has a small chance to break a piece, falling to zero by level
14. Redone does not model it and no source gives the formula, so this is a fixed 100% success
rather than an invented curve. If it is ever wanted, the wiki gives the level cap (14) but not the
curve, so it would have to come from a datamined source.

**Still open on this row:** Glassblowing (the guide's eight glass items; needs the pipe `1785`,
molten glass `1775`, and the sand + soda ash furnace step), soft clay from water and clay, and the
guide's Weaponry tab (battlestaves). Glass is the next by size and is the same shape as pottery: a
table plus a ticked loop. The one open question is whether this client has interface 11462, which
Redone uses for the pipe menu — if it does not, glass needs the same kind of decision the oven's
input did.

---

## 4d. Crafting gaps — weaving landed

**Landed (2026-10-10), weaving, both guide lines.** The Weaving tab has printed "10 Cloth" and
"21 Vegetable Sack" since before this work, and this server's own item examine texts already
pointed at the loom ("I can weave this to make sacks." on jute fibre `5931`), but nothing read
`5931` anywhere and nothing handled an object called "Loom" — `Data/objectSize.cfg` names two of
them (787, 8717). **Neither reference server implements it either**: 2006Redone advertises the same
tab with the same absence, and its `SkillInterfaces` is the only place the word "weave" appears. So
unlike spinning and pottery there was nothing to port, and the numbers come from the OSRS loom
table.

- `Weaving.Weave` is two rows — four balls of wool `1759` into cloth `3224` at level 10 for 12 xp,
  four jute fibres `5931` into an empty sack `5418` at level 21 for 38 xp. Both levels and both
  products are the guide's own lines, so the tab stops being a promise it does not keep.
- **Four materials per action, not one.** Cloth and sacks are batches, which is the one shape this
  row has that spinning and pottery do not: the loop consumes the whole batch per action and stops
  when a full batch is no longer held, rather than when the material hits zero.
- Two ways in, through the same Phase-0 registries as the rest: a first click on a loom (787, 8717)
  and wool or jute used on one. The click weaves whichever single material you are carrying and, as
  with the spinning wheel, refuses to guess when you are carrying both.
- **The animation is borrowed from the spinning wheel.** No source implements a loom, so no source
  has a loom animation; 896 is the closest action in the same skill and is documented as a borrow
  rather than passed off as a found id. A test pins it to `Spinning.SPIN_ANIMATION` so the borrow
  stays visible.
- The refusal message in `clickLoom` spells "four" out in words, which makes it a second copy of the
  amount column — so a test pins both amounts to 4.

**Still open on this row:** the other five OSRS loom recipes the guide does not list — linen yarn
into a bolt of linen (12), jute into a drift net (26), willow branches into a basket (36), hemp
yarn into a bolt of canvas (39) and cotton yarn into a bolt of cotton (73). They are deliberately
not added: this guide tab does not advertise them, three of them need item ids for intermediate
yarns that nothing in this server makes, and adding them would be scope rather than a closed
mismatch. If the tab is ever widened, they are the next five rows.

---

## 4e. Crafting gaps — soft clay landed, and what the guide's Glass tab is actually waiting for

**Landed (2026-10-10), soft clay.** §4c shipped the wheel and the oven reading item `1761`, and
nothing in this server produced it: `1761` resolved in `item.cfg` and appeared only in the baby-
and young-imping tables, so pottery was reachable in name only. Water on clay is the whole of the
gap, and `SoftClay.WaterContainer` is the whole of the table — seven rows, no level, no
experience, no animation, one clay per click.

- **The container is the row, and it is consumed.** Bucket of water `1929 -> 1925`, jug
  `1937 -> 1935`, vial `227 -> 229`, and the waterskin as a four-step ladder
  `1823 -> 1825 -> 1827 -> 1829 -> 1831`. The plain three were checked against `item.cfg` by name;
  the ladder is written out rather than derived because a dose mistake would hide in a loop. A
  test walks all four steps and then checks that the empty skin, held with clay to spare, does
  nothing.
- **Deliberately shorter than the wiki's list.** A bowl of water (`4454`) has **no definition in
  this revision's `item.cfg` at all**, and a watering can (`5331`) is modelled as an uncharged tool
  whose doses live in the farming layer. Neither is claimed, rather than registered against a full
  form this server cannot back.
- **One pair per container, registered in `ItemUseRegistry`** via `SoftClayItemUses`, which iterates
  the enum so a new row is reachable without a second edit. The registry sorts the pair, so water
  used on clay and clay used on water are the same click.
- **The four item operations are ordered delete, add, delete, add.** Each delete frees the slot its
  add then fills, so a full pack can still soften clay; a test fills all 28 slots and checks the
  swap lands rather than quietly eating the clay.
- **The zero experience is the real number, not a placeholder.** OSRS awards none for soft clay, so
  any figure would be invented; a test pins `SoftClay.XP` and asserts `playerXP[Crafting]` is
  untouched, so the decision reads as one rather than as a forgotten `addSkillXP`.
- **Not ticked, on purpose.** Both items are already in the pack, so there is no walk to stop and no
  world object to pace against — and a repeat would have to pick a container, which a player
  carrying a jug and a waterskin would watch it do.

**The open question from §4c is answered, and the answer is no.** §4c asked whether this client has
interface `11462`, the pipe menu Redone uses. It does not: `11462` appears nowhere in the entire
client source, and neither does any glassblowing interface at all — searching the client for
"Fishbowl", "Unpowered orb", "Lantern lens", "Beer glass" and "glassblow" returns nothing, and the
only interface files are `ui/RSInterface.java` and `ui/Interfaces.java`, the 317-style hardcoded
set. **The guide's Glass tab (screen 5, `skillInterfaces.java:1440-1453`) has no button behind it
either**: `menuLine` writes a `sendFrame126` label and an icon into the stock guide interface 8714,
so those eight rows are text, not slots any server-side handler can hear.

So glass cannot be built the way pottery and weaving were, and the decision is not a table's shape
but a UI one, with three honest options:

1. **Reuse a chatbox menu that exists.** 8938 is the five-row "What would you like to make?"
   (snakeskin and pottery both use it) and 8880 is the three-row one (bows). Eight products do not
   fit either, so this means either dropping three rows or splitting the tab across two menus with
   a pagination trigger that has no natural home on the pipe.
2. **Author the interface.** `RSPSInterfaceMaker` exists in this repo for exactly this, and a
   seven-slot glass menu would make the whole chain work without bending an unrelated chatbox.
3. **Build the prerequisite first and leave the pipe for later.** `1775` has no source either, and
   the sand + soda ash furnace step needs no interface at all: bucket of sand `1783` on a furnace
   with soda ash `1781` is a pair-keyed item-on-object registration, the same shape as the oven.
   Note the chain is longer than it looks — seaweed `401` burned on a fire makes soda ash, the
   unpowered orb `567` is itself a glass product, so the orbs `569/571/573/575` and the guide's
   Weaponry tab (screen 7: water 54, earth 58, fire 62, air 66) sit downstream of the same blocked
   menu.

**Also still open on this row:** the guide's Weaponry tab, four battlestaves — water `1395`, earth
`1399`, fire `1393`, air `1397` at 54/58/62/66, orb on battlestaff, 100 xp. **Nothing in this
server consumes an orb.** All four orbs resolve in `item.cfg` but only `569` has any source at all
(gnome and hero thieving), and `1391` battlestaff is an eclectic-imping reward and nothing else. The
mismatch is real and worth closing, but it is downstream of glass: it would make three rows that
consume items no player can obtain yet.

---

## 4f. Herblore — the tables were right, the implementation was not

**Ours** was four static fields (`itemToDelete`, `itemToDelete2`, `itemToAdd`, `potExp`) that a
click filled in and a tick loop read back out, with the amounts chosen through a make-X menu
(buttons `10238`/`10239`/`6212`/`6211`), and five tables around them: cleaning, grinding, unfinished
potions, finished potions and the three `isX` helpers.

**The data was nearly perfect and the code around it was not.** Checked row by row against the
guide's own Herbs and Potions tabs and against OSRS, the mixing table's levels and experience are
right in 21 of 22 rows, the cleaning table's in 15 of 16, and the herbs, grimy ids, unfinished ids
and secondaries all resolve in `item.cfg`. What was wrong:

- **Static state, shared by every player.** Two people mixing at once — one an attack potion, one a
  super strength — wrote the same four fields, so the tick loop could deliver one player's product
  to the other and consume the wrong materials. This is the bug worth the rewrite on its own.
- **`cleanHerb` cleaned everything it found.** It looped the whole table without breaking and acted
  on every row present in the pack, deleting each one from the slot belonging to the *first* herb it
  matched. A player carrying two kinds of herb and clicking one cleaned both, from the wrong slot.
- **The make-X menu is not how herblore works.** In OSRS a herb on a vial of water is one potion per
  click and an ingredient on an unfinished potion is one potion per click; there is no "how many"
  for either. Fletching's fifteen-at-a-time batches exist because OSRS batches arrows; herblore
  does not batch anything.
- **Five of the guide's own promises could not be made at all** — energy (26), agility (34), super
  energy (52), antidote+ (68) and antidote++ (79) were printed on the Potions tab with nothing in
  the class able to produce them.
- **Two scattered data errors**: cleaning guam was level 1 (guide and OSRS say 3, which is also the
  attack potion's level, so attack potions were gated one tier low end-to-end), and the guide prints
  ranging at 69 and antifire at 72 where OSRS and this table's own experience values (163, 158) say
  72 and 69 — the guide's two rows were swapped.

**Proposal, implemented as one slice:** four enums (`Cleaning` 16 rows, `Grinding` 8, `Unfinished`
16, `Finished` 27) read directly by the action instead of flattened into shared state; **every pair
registered in `ItemUseRegistry`** through a new `HerbloreItemUses`, which is possible precisely
because the recipe now travels with the click; cleaning left in `ClickItem` because a grimy herb is
clicked rather than combined; mixing and grinding ticked at two cycles (the fletching cadence), one
product per action; cleaning instant, because one item in the pack has no walk to interrupt — the
`SoftClay` reasoning. The legacy path was **deleted**, not left behind: `setupPotion`, `makePotion`,
`grindItem`, `setupGrinding`, `handleHerbloreButtons`, `resetHerblore`, the three `isX` helpers, the
four static fields, the `isPotionMaking`/`isGrinding` flags on `Player`, and the `UseItem` and
`ClickingButtons` hooks they lived on.

The five missing rows use the recipes their secondaries in this revision imply — chocolate dust
(`1975`) for energy, toad's legs (`2152`) for agility, mort myre fungi (`2970`) for super energy,
yew roots (`6049`) for antidote+, magic roots (`6051`) for antidote++ — at the levels the guide
prints, with the OSRS experience. Every one of those ten ids resolves in `item.cfg`.

**Deliberate omissions.** Spirit weed and wergali clean (levels 35 and 30, both in the table) but
their unfinished potions lead nowhere: this revision has `Spirit_weed_potion_(unf)` and
`Wergali_potion_(unf)` and no finished product for either, so the pair would be a potion with no
recipe. They are listed in `QOL_PLAN.md` and named in the class comment rather than registered.
Blurite bolts stay out for the reason in §5.

**Guide corrections carried with the change:** the herbalism Herb tab now prints wergali (30) and
spirit weed (35), which the table could clean but the tab did not list, and the ranging/antifire
rows were swapped back. Combat and hunter stay off the tab — the tab is a summary, and a table may
know more than it prints.

Tests: 22 in `HerbloreTest` — all four tables pinned row by row, cleaning at the level boundary,
cleaning only the clicked herb, the experience each family awards (and the two that award none),
the pestle surviving grinding, both mixing steps ticked and re-checked, cancellation on a walk,
registry coverage of every pair and the duplicate-registration guard, and **the two-player
cross-wiring bug** as a named regression.

**Correction, same day — the menu was right and the second bullet above was wrong.** OSRS *does*
ask "how many would you like to make?" when a herb goes on a vial of water, and it does keep making
them while you stand there: the chatbox is interface `4429` with the four buttons
`10239`/`10238`/`6212`/`6211` (make 1/5/10/28), which is where the ids the pre-rewrite menu read
came from. What was wrong with our old one was the state behind it — four static fields holding the
recipe *and the amount*, so one player's menu answered another player's click, and "make 5" was a
server-wide instruction. So the mixing steps read it again, per player this time:

- `Herblore.mix` (both tables) opens `4429` through `SkillHandler.send1Item` with the product's own
  model and name, remembers the combined pair on the player (`herbloreDialogue` plus
  `herbloreItem1`/`herbloreItem2`) and **consumes nothing**. The pair is stored as the two ids that
  were combined rather than as a resolved recipe, so the tables stay the only thing that knows what
  a pair makes.
- Those four buttons are routed in `ClickingButtons` while the flag is set and start a two-cycle
  batch: one potion per action until the amount asked for is made, the materials run out, or the
  player walks. The amount lives in the event closure, not in `c.doAmount`.
- Nothing is consumed until the first potion is actually made, so the menu is safe to ignore, and a
  walk both closes it and stops the batch.
- **Grinding keeps one-per-click**, and that is a decision: the pestle is a tool rather than an
  ingredient, so there is no stack of second materials for an amount to be an amount *of*. Sharing
  `Herblore.start`'s amount parameter with it would be one line if that ever changes.

The button ids are this interface's own — deliberately not the leather menu's, which puts ten on
`10238` and twenty-eight on `6212` on interface 1743. Two rows are only interchangeable within
their own interface.

Tests: 32 in `HerbloreTest` now, the ten added ones pinning the button table, the open-consumes-
nothing step, the remembered pair, 1/5/all stopping exactly where they should, the two-cycle
pacing, and the amount not crossing between two players.

---

## 4g. Runecrafting and the anvil — off the instant batch

**Landed (2026-10-10).** These were the last two "whole batch in one tick" loops §4 names:
`Runecrafting.craftRunes` bound every essence in the pack in a single `while`, and
`Smithing.doaction` hammered `maketimes` items in `while (maketimes > 0)`. Both are now one item per
action on the fletching cadence (two cycles), with every value travelling in the event closure rather
than through a class, and both cancel when the player walks — `PlayerAssistant.resetVariables` calls
`Runecrafting.cancel` and `Smithing.cancel` beside the herblore and fletching ones. (`Runecrafting.cancel`
is gone as of §4h: an altar takes its whole pack in one click, so there is nothing to cancel.)

**Runecrafting.** ~~One essence every two cycles, animation `791` and gfx `186` re-armed each time.~~
**Superseded by §4h**: the pacing was wrong for this skill and the action is once again the click,
with the multiplier fix and the altar-list fix kept. Three silent defects went with the first
rewrite:

- **The multiplier ladder had an off-by-one.** The old loop walked the ladder from index 1, so it
  could return at most `length - 1`: an air essence at level 99 made **nine** air runes instead of
  ten, and every rune above air was one short of its own top rung. `getMultiplierForLevel` now counts
  the rungs a level has passed, with the level requirement itself as the first rung.
- **The altar list was duplicated** in `ClickObject` as a hand-written `int[] altarID`, and the copy
  stopped at blood — `30625`, the soul altar, was in the table and unreachable by clicking it. The
  list now lives with the table (`Runecrafting.isAltar`).
- **An altar clicked with no essence said nothing at all**, which reads as a broken altar. It says so
  now.

**Experience is deliberately unchanged**, and it is the one place this skill and OSRS disagree: the
award is `(int) xp * Config.RUNECRAFTING_EXPERIENCE` (15× here) per essence, *not* multiplied by the
rune ladder, where the real game does multiply it. Multiplying an already-fifteen-fold rate by a
further ten is a balance change rather than a realism one, so it is left alone and recorded as an
open question in §14.

**Smithing.** One item every two cycles, bars re-checked each cycle. The per-bar batch sizes (darts
10, nails and arrowheads 15, knives 5, cannon parts 4, everything else 1) are the real game's yields,
and extracting them into `batchSize` fixed a live bug: the old comparisons were case-sensitive against
a name the item table capitalises, so `contains("knife")` and `contains("cannon")` could never match
"Knife" or "Cannon_base" — **knives and cannon parts were made one per bar**.

The `item`/`xp`/`remove`/`removeamount`/`maketimes` **static** fields are still there and still how
the `Check*` chains hand their result to `doaction`, which is why the action takes every one of them
as a parameter and reads none of them: a ticked action outlives the click that started it, so a
second player's click would otherwise re-point a running batch at another product. That property is
driven through the real `readInput` entry point by a test. **Still open:** collapsing the 26.9 KB
`Check*` chain into a metal/product table, which is what would let the statics go.

Tests: 17 in `RunecraftingTest`, 11 in `SmithingTest`. **1148 tests, 0 failures** (992 server + 156
workshop). (The runecrafting count is 15 as of §4h, which replaced the two pacing tests with the
one-click ones.)

---

## 4h. Runecrafting, corrected twice over — one click, both essences, and the XP-drop icon

**Landed (2026-10-10), the same session as §4g.** Three reports came back and all three were real.

**The pacing was wrong for this skill.** §4g made `craftRunes` bind one essence every two cycles so
it matched fletching. Runecrafting is the one gathering skill where that is not the shape: an altar
takes **the whole pack in the single click**, the bind animation plays once, and there is no action
left running. The two-cycle loop was also an AFK loop — a player who clicked once and then banked
kept being paid. `craftRunes` now counts the pack, draws it down, hands over `essence × multiplier`
runes and the whole pack's experience in the same tick. The event, the `RUNECRAFTING_EVENT` id, the
`playerSkilling[playerRunecrafting]` gate and `cancel` are gone, and `PlayerAssistant.resetVariables`
no longer calls it. The multiplier fix and the altar-list fix from §4g are kept — they were the good
half.

One consequence, recorded rather than hidden: the pack's experience is awarded as a single sum, so a
click that crosses more than one level announces only the level it lands on.

**Known gap left open:** `Pouches` still stores and returns `1436` only, so a pure-essence player has
no pouch. It is a separate file and a separate behaviour (fill/empty/check), so it is not folded into
this change — but it is the next thing pure essence wants.

**Pure essence did nothing.** `craftRunes` drew down `1436` alone. Pure essence (`7936`) — what the
members' essence and the essence impling give out — was invisible to every altar, so a pack of it
crafted nothing at all. Both ids are essence now and either is accepted at any altar, rune essence
spent first. The OSRS tiering, where the higher altars insist on pure essence, is **not** modelled;
it is in §14 as a decision because inventing which altars gate on it would be a guess.

**The XP drop drew the wrong icon.** The drop's icon is client-side: `Gameframe/XP/skill N.png` for
the skill number the server sends (`Skills.skillNames` — runecrafting 20, slayer 18, farming 19).
The set had been filed wrong at the top: **slayer's artwork sat in `20`**, runecrafting's in `19`,
and there was no `18` at all — so every runecrafting drop drew a slayer skull, every farming drop a
rune, and slayer drops drew nothing. `tools/rebuild-xp-skill-icons.ps1` re-derives an icon from the
skill tab's own art (`Sprites/Interfaces/skillchat/skill N.png`, the set the skill tab and hiscores
panel already use, with their documented 21/22 hunter-construction swap applied). Skills 18, 19 and
20 are regenerated. 0-17, 21 and 22 are right where they are — they carry their own skill's art — and
are left alone: re-deriving them shows the shipped set is the same artwork with the same hard alpha in
the same 18px box, but not produced by this exact rule, so rewriting them would churn twenty files for
a pixel or two. The check that the three new ones are the *right* art is the old files themselves: the
new `20` lands on the same `17x18` box and 175 pixels as the rune art that used to sit in `19`, and the
new `18` on the same `18x18`/220 as the slayer art that used to sit in `20`. Nearest-neighbour on
purpose, so the alpha stays hard (0 or 255) and the colours stay the source's own. The client reads the
icons at startup, so the **client needs a restart** to see them.

Tests: 15 in `RunecraftingTest` (the two pacing tests became `theWholePackIsBoundInTheClickItself`,
`nothingIsBoundAfterTheClick`, `pureEssenceIsBoundJustLikeRuneEssence` and `aMixedPackIsBoundTogether`).
**990 tests, 0 failures** (990 server).

---

## 4i. Pouches — the two essences reach the pouch too, and the dead twin is deleted

**Landed (2026-10-10), the same session as §4h.** §4h recorded this as the next thing pure essence
wanted, and it was right: the altar accepted pure essence and the pouch did not, so half the skill
worked with the essence and half did not.

**What a pouch is for, which decides the rest.** Essence is **not stackable** — not here and not in
OSRS — so the twenty-eight inventory slots are the real limit on a trip and a pouch holding three to
twelve more is worth the bank walk. That is also why an altar binds only the pack and never a pouch:
you empty them into the inventory first, as the real routine does. This was checked before it was
changed, because marking `1436`/`7936` stackable in `stackable.dat` looked like the same bug from the
outside. It is not, and no data file was touched.

- **Both essences work, and the kind is remembered.** A pouch stores one count rather than a pair of
  stacks, and that count has been written to character files as `pouch = a\tb\tc\td` for long enough
  that its meaning is not negotiable. So the kind travels beside it in a new `Player.pouchEssence`,
  saved as its own optional key. Without it, emptying a pouch would either hand a rune essence back
  as pure essence — a free upgrade — or a pure essence back as rune, which is the same bug pointed
  the other way. Filling takes rune essence first and pure second, matching the altar's spend order.
- **A count with no kind recorded reads as rune essence, and that is a migration rather than a
  guess.** Every character file in existence is in exactly that state — a count, and no kind —
  because rune essence was the only thing a pouch could hold when they were written. Read as
  "empty", the essence would be stranded in a pouch that reports itself empty and can never be
  refilled (the count is still there, so filling it says "full"). `Pouches.kindOf` is the one place
  that rule lives.
- **A pouch holding one kind will not silently take the other.** One count cannot hold two kinds, and
  converting either into the other is the upgrade bug again.
- **Two messages that were not true.** A pouch said "You fill your medium pouch" when it was already
  full or when the pack held no essence, and said "You empty your medium pouch" with nothing coming
  out. Both read as a pouch that has eaten the essence; each now says which it is.
- **The dead second implementation is deleted.** `PlayerAssistant.checkPouch`/`fillPouch`/`emptyPouch`,
  `Player.pouches` and `Player.POUCH_SIZE` had no callers at all, and they hard-coded `1436` in
  precisely the way the live code did — the identical landmine, one refactor away from being wired
  back up. Deleted rather than left beside the live path, per §13's "don't leave two paths". The three
  actions that are live (click = fill, item option 2 = check, wear = empty) all route through
  `Pouches`; `ItemClick2`'s private scan of the table is now the same lookup the other two use.

**Open, deliberately:** the damaged pouches (`5511`, `5513`, `5515`) are recognised by nothing, so
clicking one still does nothing. In OSRS a damaged pouch holds less and is repaired; the reduced
capacities are not in this revision and guessing them would be inventing content. `ClickItem` has
carried `5511`/`5513` in its pouch case list all along, which is what a row that was never written
looks like. Worth doing as its own slice, with the repair or the degrade trigger it implies.

**The three actions live in three packet classes.** Fill is `ClickItem`, check is `ItemClick2`, empty
is `WearItem`'s pouch branch — which is also how a pouch is *not* equippable. It works, and nothing
here changed it, but it is three files to touch for one item's menu, and worth folding into one
registration if the pouches are ever revisited.

Tests: 13 in the new `PouchesTest` — the two essences in and out, the kind order, the refusal to mix,
the level gate being on filling only, the two migration halves, and the slot cap on emptying — plus
two in `PlayerSaveTest` for the new key, one of which removes the key and proves an old file still
loads its counts. **1005 tests, 0 failures** (1005 server + 156 workshop).

---

## 4j. Jewellery — the interface cannot ask for "all", and one product could not be made

**Landed 2026-10-10.** Interface `4161` is the jewellery mould: three rows of seven slots, a gem and
a gold bar per slot. It is the last of the instant-batch crafting families.

**Read out of the cache, not inferred.** New `Proxy Client/tools/mould-interface-probe` loads the
real interface archive (`index0` entry 3, through `Decompressor` → `StreamLoader` → `RSInterface.unpack`,
with the fonts built from archive 1) and dumps `4161`. That answered the question this slice turned
on, and it is not answerable from the server source:

```
container 4233: type=2 7x1 slots=7 actions: [1=Make, 2=Make 5, 3=Make 10, 4=-, 5=-]
container 4239: type=2 7x1 slots=7 actions: [1=Make, 2=Make 5, 3=Make 10, 4=-, 5=-]
container 4245: type=2 7x1 slots=7 actions: [1=Make, 2=Make 5, 3=Make 10, 4=-, 5=-]
frame 4229: type=6 32x32   frame 4230: type=4 400x14 "You need a ring mould to craft rings."
frame 4235: type=6 32x32   frame 4236: type=4 400x14 "You need a necklace mould to craft necklaces."
frame 4241: type=6 32x32   frame 4242: type=4 400x14 "You need an amulet mould to craft amulets."
```

**So make-all is not a click this client can send.** The containers carry three actions and two empty
slots — there is no fourth option whose opcode the server could be missing. The chatbox is therefore
not a stylistic choice here, it is the *only* route to "all", which is why jewellery goes onto the
same `CreationMenu` (§15) the chisel and the tanner use, one row and 1/5/10/28. The left click — the
interface's `Make` — opens that prompt; `Make 5` and `Make 10` are kept as the shortcuts they already
were. Cost: left click is now two clicks. Gain: "all" exists for the first time.

**A product the interface offered could not be made.** The recipe table named `6583` as the onyx ring
while the interface's own array drew `6575`. `6583` is the **Ring of stone** — `Magic.ONYXRING(6575,
6583, …)` enchants `6575` *into* `6583` — so clicking the onyx ring in the mould interface matched no
recipe and returned in silence: the one product the interface offered for onyx was the one product
that did nothing. Fixed to `6575`.

**The two lists that disagreed are now one list.** That bug existed because the recipes and the
interface rows were separate arrays that had to be kept in step by hand. The rows are now derived from
`Product` in declaration order, so a slot and its recipe cannot drift apart, and
`tools/mould-interface-probe` exists to check the table against the real interface rather than trust
either.

**Also fixed on the way past:**
- **The batch is ticked** — one item every 2 cycles, animation `899` (the furnace pose smelting and
  the pottery oven use; the old path played none), all state in the event closure, stopped by walking.
- **A full pack no longer eats the materials.** Jewellery consumes two items for one, so a tick with
  nowhere to put the product would have destroyed a bar and a gem and handed back nothing. The tick is
  not spent when there is no room.
- **The materials and the mould are re-checked on every entry point**, not only when the interface is
  drawn: the mould and the bar are both things the player can bank with the interface open.
- **Plain gold pieces no longer depend on a coincidence.** They have no gem, and the old code detected
  that by asking whether `ItemAssistant.getItemName(-1)` contained the word `"unarmed"` — which it
  does, because `getItemName` falls back to `"Unarmed"` for an unknown id. It worked, and said so
  nowhere. `JewelryMaking.NO_GEM` says so.
- **The amulet line** read "You need a amulet mould to craft necklaces", overwriting the correct
  sentence the cache already had on frame `4242`.

**Untouched, deliberately:**
- **Stringing an amulet is still instant.** A ball of wool on an unstrung amulet is a two-item recipe
  with no mould, no interface and no amount — the one part of jewellery that is not a menu question.
  Its XP was the other oddity: `addSkillXP(4, 12)` bypassed `CRAFTING_EXPERIENCE` while every other
  crafting award multiplies by it, so stringing paid 1/5th of what it says. Brought in line.
  It stays instant, and is the obvious next slice if it should become a "How many would you like to
  string?" chatbox — the screenshot set in §15 has one.
- **Interface `4161` stays the product picker.** Twenty-one products do not fit five chatbox rows, so
  the grid keeps the choice and the chatbox takes the amount.

Tests: 33 in the new `JewelryMakingTest` — the onyx ring, the seven-slot shape, table/interface
agreement, the cached container and frame ids, mould-gated rows, the prompt opening and refusing, the
ticked batch and its amount caps, the full pack, the shortcut amounts, the walk cancel, and the two
cross-checks that every gem is a *cut* gem and the necklace row cannot make a ring.
**1092 tests, 0 failures** (1092 server + 156 workshop).

---

## 5. Fletching realism — the headline change

**Ours**: `fletchBow(c, id, amount)` deletes `amount2` logs and adds `amount2` bows in
one call then `closeAllWindows()`. Interface is the OSRS "What would you like to make?"
at `8880`, buttons `34182-34193`.

**Correction to an earlier draft of this section.** It claimed a shaft multiplication bug —
`15 * amount2` adding fifteen shafts per matching log inside the loop over `logArray`. That
was wrong, and worth recording because the wrong rationale would have justified a wrong fix.
The ratio was already right (1 log → 15 shafts) and the loop `return`s at the end of the
first matching log, so only one log was ever processed. The real defects in that path are
different and smaller: it was a **batch** rather than ticked; it took its log from the table's
own `logID` (1511) rather than from the product, so with an oak log held the shaft buttons
consumed normal logs instead; and it skipped both the level check and the animation that the
bow path had. The corrected picture is what Phase 1 implements.

**N1** is exactly the behaviour you described — one log per action, ticked:

```238:272:src/main/java/com/ruse/world/content/skill/impl/fletching/Fletching.java
player.setCurrentTask(new Task(2, player, true) {
    int amount = 0;
    @Override
    public void execute() {
        BowData bow = BowData.forBow(product);
        boolean shafts = product == 52;
        if(bow == null && !shafts || !player.getInventory().contains(log)) { ... stop(); return; }
        if(bow != null && ... < bow.getLevelReq()) { ... stop(); return; }
        if(!player.getInventory().contains(946)) { ... stop(); return; }
        player.getInventory().delete(log, 1);
        player.performAnimation(new Animation(1248));
        player.getInventory().add(product, shafts ? 15 : 1);
        player.getSkillManager().addExperience(Skill.FLETCHING, shafts ? 1 : (int)(bow.getXp()));
        Sounds.sendSound(player, Sound.FLETCH_ITEM);
        amount++;
        if(amount >= amountToMake) stop();
    }
});
```

**Proposal:** keep our interface and every button id, change only what a button does —
start a `CycleEventHandler` loop using the existing `c.doAmount` convention
(`SkillHandler.deleteTime` already decrements it). Three commits:

1. Bows/staves 1-by-1, honouring the existing 1/5/10/28 buttons.
2. **Arrow shafts: 1 log → 15 shafts per action**, one action per log. (Not a ratio fix —
   the ratio was already correct. The log now comes from the product, and the level check
   and animation that the bow path had are applied here too.)
3. ~~`Stringing` + `ArrowMaking` as ticked actions from N1's `StringingData`/`ArrowData`.~~
   **Split.** Arrow/bolt making and tipping are done (see above). Stringing is not a port —
   we have none at all, so it is new content and belongs with the Phase 3 skilling gaps.

Note the N1 table shape worth keeping: `BowData(logID, unstrungBow, xp, levelReq, bowId)`
where `unstrungBow` is 48/50/54… and `bowId` is the finished 839/841… — and **avoid** its
`forLog(log, shortbow)` which string-matches `toString().contains("shortbow")`. Give the
enum an explicit `boolean shortbow` column instead.

Tests: level requirements, bow ids, the 1:15 ratio, and one-log-per-tick consumption.

**Landed (2026-10-10), partly.** `fletchBow` is now a ticked `CycleEventHandler` action:
one log per 2-tick cycle, the log derived from the product rather than from `c.log`, with the
knife/level/material guards brought onto the shaft path too. It runs on its own event id
(`FLETCH_EVENT`) so `cancel` can stop it without touching other skills' events; `resetVariables`
now calls `Fletching.cancel` unconditionally, so walking away ends it. The old batch body is
kept verbatim as `fletchBowInstant` behind `Config.FLETCHING_ONE_BY_ONE_ENABLED`, so the change
reverts without a revert.

One data correction was needed: the table gave arrow shafts `levelReq = 15`, but the old
shaft path never checked a level, so enforcing that entry would have taken shafts away from
everyone below 15 for the first time. Corrected to 1, which is the real requirement and
matches what players could always do.

**Not done:** bow **stringing does not exist in our server at all** — item 1777 appears only as
an impling reward and as the label on the Crafting "flax into bow strings" spinning option;
nothing consumes it. So there is no stringing to make ticked, there is stringing to *add*.
That is a Phase 3 item, not a Phase 1 tweak, because it needs its own log→unstrung→strung
table and level/xp data rather than a rewrite of something existing.

**Landed (2026-10-10), bow stringing.** Written as new content, as the note above said it had to
be. `Fletching.Stringing` is a thirteen-row table — Necrotic's `StringingData` verbatim for the
twelve bows it has, plus Redone's composite-bow row (`4825 + 1777 -> 4827`, 30, 45 xp), which
Necrotic lacks. Every unstrung id, strung id, level and xp was checked against both sources and
against `item.cfg`; all twenty-seven ids resolve.

- **Lookup is on the pair, and order-blind.** `forStringing(item1, item2)` requires the bowstring
  to be one side of it, so an unstrung bow on a chisel is not stringing. Matching the unstrung id
  alone would have claimed those pairs.
- **`stringBow` is ticked and repeating**, on the same `playerFletch` flag and the same
  `FLETCH_EVENT` id as `fletchBow`, so `cancel` and `resetVariables` end it with no new hook and
  only one of the two can run at a time.
- **Registered in `ItemUseRegistry`, not inline.** This is the first item recipe to move into the
  registry, via `FletchingItemUses.register()` called from the registry's static block — the same
  bootstrap shape as `ObjectHandler`. `UseItem.ItemonItem` returns on a claimed pair, so the
  registry and the inline checks never both run.
- **Animations are Necrotic's per-bow set** (6678-6689, one per bow). Redone strings all thirteen
  with no animation; the sourced set beats inventing ids. The composite bow borrows the longbow's,
  and that borrow is documented at the row because it is the one number without a source.
- **The magic longbow is 85 here, 87 in `Fletch`.** Left alone: it is the sourced value, it is a
  balance number, and it is unreachable anyway because the unstrung bow needs 87. Flagged rather
  than silently reconciled.

Tests: eleven new cases in `FletchingTest` (both orders, every level/xp/animation, xp parity with
the cutting table, no duplicate or self-referential rows, null for non-pairs, registration
enumeration, a click through the registry, repeat-until-empty, the level gate, no bowstrings, and
cancellation on a walk), plus a `Fletching.Stringing` row in `QolValidatorTest`.

**Also landed with this phase (same session):** `makeArrows`, `makeBolts`, `handleBoltTipping`
and `handleBoltTipCrafting` moved off their `System.currentTimeMillis()` throttles and onto the
tick via one shared helper. These stay single actions — fifteen arrows or ten bolts per click is
the OSRS batch size — so the flag does not gate them. Two real bugs were fixed on the way:

- **`forBolts` matched the wrong column.** It tested `getItem2()`, which is `314` (feathers) for
  every row, so it answered `BRONZEBOLT` whenever asked about feathers and `null` for every
  actual bolt. Combined with `makeBolts` looking only in its first argument, (bolts, feathers)
  did nothing and (feathers, bolts) always made bronze — **iron through runite bolts could not
  be made at all**. It now matches the bolt column, and `makeBolts` looks in both arguments.
- **Bolt tips and bolt tipping no longer use wall-clock throttles.** A `currentTimeMillis()` gate
  in front of an instant action lets a fast clicker through on lag and lets a slow one do
  nothing; the tick loop paces them instead.

Also removed: four `System.out.println` debug lines that ran on every arrow and bolt attempt.

**Landed (2026-10-10), darts, dragon arrows, and the bolt table's inputs.** Two additions you
asked for, plus the one change they forced.

- **Darts did not exist and could not have: the dart tips were being spent on bolts.** `Bolts`
  named `819`..`824` as its inputs, and those ids are the dart tips — `item.cfg` calls them
  `Bronze_dart_tip` .. `Rune_dart_tip`, Smithing makes them at 4/19/34/54/74/89 under the guide's
  own heading "Dart Tips - 1 Bar makes 10", and the Fletching shop sells `819`. So a bolt cost a
  dart tip and a dart tip could never become a dart. Darts and bolts share their input pair (tip +
  feather `314`), so the bolt rows **moved onto the six unfinished bolts this revision has always
  carried and nothing has ever used** — `9375`, `9377`-`9381`, `Bronze_bolts_(unf)` and friends —
  freeing the tips for darts. Blurite is the deliberate omission: its pair is ready (`9376` +
  feather → `9139`) but this revision prints no level for it anywhere.
- **`Fletching.Darts` is seven rows**, a dart tip and a feather, ten at a time — the bolt batch
  size. Every dart in this revision that has a tip is covered: bronze through rune, plus dragon
  (`11232` → `11230`), whose tip comes from implings and from nothing else. A black dart (`3093`)
  exists with no black dart tip, so it is the one dart left out, and the table says so by name
  rather than by silence.
- **The levels are this revision's own, the xp is not, and the split is recorded at the table.**
  The Fletching guide's Darts tab prints 1, 22, 37, 52, 67 and 81 — what a player sees when they
  look the recipe up — so those are the levels. The guide stops at rune, so dragon takes 90, the
  tier the dragon arrow row uses. It prints no xp at all, so xp is the OSRS per-dart value for a
  batch of ten: 18, 38, 75, 112, 150, 188, 250.
- **Dragon arrows are an eighth `Arrows` row**: `11237` + `53` → `11212`, fifteen at a time like
  every other arrow. The guide does not print this one either; 90 is the OSRS dragon tier, and 245
  xp keeps the table's own step (rune is 207, and the rows before it climb by 37/37/38/37).
- **Darts went into `ItemUseRegistry`, not into `UseItem`.** They are the second family the registry
  owns outright, and they could not have gone inline even if that were preferred: the bolt block
  claimed their exact pairs for as long as darts did not exist. A test asserts that every
  tip+feather pair is registry-owned, that the reverse order is the same key, and that `9375` +
  feather is *not* registered — one click can no longer be a dart and a bolt at once.
- **One adjacent bug fixed while in the family.** `forArrow` matches `getItem2()`, which for
  `HEADLESS` is the feather, so (headless arrow `53`, feather) also resolved to the headless row and
  spent fifteen feathers to hand back the fifteen headless arrows it started with. `makeArrows` now
  requires a shaft for that row.
- **The guide lists both**: "90 Dragon arrow" on the Fletching Arrows tab and "90 Dragon Dart" on
  the Darts tab, so the new recipes are findable where every other one is.

Tests: eight new cases in `FletchingTest` (every dart row looked up by its tip, dragon arrow by its
arrowtips, both level gates, ten-per-action through the registry, no bolts from a dart tip, the
headless guard, and a sweep asserting the tip and unfinished-bolt columns are disjoint), six
existing bolt cases retargeted onto the unfinished bolts, and a `Fletching.Darts` row in
`QolValidatorTest`. **Deliberate behaviour change:** bolts now cost an unfinished bolt, not a dart
tip. The Fletching shop still sells `819`, which is now simply a dart tip.

---

## 6. Random events, flag-driven

**Ours (before Phase 2)**: four events, **no dispatcher and no flag**, each fired inline by
`Misc.random(250) == 0` — `SpiritTree` (Woodcutting `:108`), `RockGolem` (Mining `:46,
151, 240`), `RiverTroll` (Fishing `:133`), `Zombie` (Prayer `:113, 124, 149`). Bird
nests already work: `Woodcutting.birdNests` grants `5070` at `random(100) < 5`. No genie.

**N1**: no classic randoms at all, and **no random-event manager or config flag**. It has
two *global world* events ticked from `World.sequence()` — `EvilTree` (1 h, 805 logs) and
`ShootingStar` (30 min, 600 mines) — each a singleton with a `Stopwatch`, a
`LAST_LOCATION` anti-repeat and a `LocationData` enum. Bird nests are a flat
`Misc.getRandom(60) == 1` woodcutting drop with a per-nest loot table.

So: classic randoms are **new in every source**; only the nest table and the world-event
shape are rippable.

**Built (Phase 2):** `server/game/minigames/randomevents/RandomEventManager` — an `Event` enum of
the five interrupting events, each carrying its `Config` flag and a draw weight; one
`onSkillAction(Client)` entry point; and a per-player countdown persisted as
`randomEventCounter` in `PlayerSave`.

- **The seven inline sites are gone.** `Misc.random(250)` in `Woodcutting` (1), `Mining` (3),
  `Fishing` (1) and `Prayer` (2 at 1/251, plus the altar at 1/81) are now one call each.
- **The flags now do something.** `RANDOM_EVENT_CLASSIC_OTHERS_ENABLED` was `false` while
  Spirit Tree, Rock Golem, River Troll and Zombie fired unconditionally, so the flag describing
  the shipped behaviour was wrong. They read it now, which means those four are **off** by
  default — the ask.
- **Two rhythms on purpose.** A nest is a frequent small bonus, so it keeps its per-action roll
  (`Misc.random(100) < 5`, unchanged); an NPC event is an interruption, so it runs on a
  countdown of **350–450 actions** — Necrotic's `CALL_RANDOM = 350 + random(100)` shape. Giving
  the nest a countdown weight would have made it ~20× rarer. Nest type now uses Necrotic's
  distribution: seed 64.1%, ring 32.0%, the three egg nests 3.9%. The old code always gave
  `5070` (red egg), so seed and ring nests — which `ClickItem` already opens — were unreachable.
- **The genie hands over the lamp we already have**, item `4447`: rub it and the existing
  skill-choice interface opens, so no client change and no new interface. It is spawned through
  `spawnNpc2` (which returns the NPC, so the despawn timer does not need a fifth copy of the
  block in `spawnNpc`) and registered as an NPC action in `RandomEventNpcs`, not in the generated
  NPC tables — it was never in the switch.
- **`spawnXxx` now returns whether it spawned.** All four stopped the player's action whether or
  not the NPC had appeared, so a low-level player could be interrupted by nothing. An event only
  interrupts if it actually arrived.
- **Bots are skipped.** They occupy real slots and run the same skilling code, so without the
  guard a bot would collect genies and nests.

**Not built, and why:** Sandwich Lady, Evil Chicken, Freaky Forester, Swarm, Frog, Shade and Tree
Spirit. The NPC ids exist (`411` Swarm, `2463` Evil Chicken, `1830` Frog, `425-430` Shade,
`438-443` Tree spirit), but each needs behaviour and dialogue invented from nothing, and this
section's own rule is not to invent values that can be looked up. They go in when their
dialogue is written, not before. Also open: the lamp is worth a **level-70** skill, which is
generous for a random event — the countdown and the lamp id are both one-line changes if that
should be toned down.

**The fail-teleport list from R1 is not here.** It only means something for events a player can
refuse or fail — the Sandwich Lady and friends — so it belongs with them, not with the genie,
which cannot be failed.

Tests: `RandomEventManagerTest` (18) pins the shipped enablement, that a disabled event can never
be picked, the weight bands, the nest distribution, and the countdown arming/firing/re-arming
contract. `PlayerSaveTest` pins that the counter round-trips and that a character file written
before the field existed loads as unarmed rather than as "fire now".

---

## 7. World interactivity

### 7.1 Water fillables — **R is the only source**

N1 **has none** (no fillable class, no water-source handling). R's `Fillables` is
`counterpart(emptyId) → fullId` (bucket `1925→1929`, jug `1935→1937`, vial `229→227`,
bowl `1923→1921`, cup `1980→4458`, watering cans → `5340`, waterskins → `1823`) plus a
water-object id list (sinks, wells, fountains, pumps). R fills the **whole stack at once
with no sound**; we do it 1-by-1 with animation `832` and a sound, via the Phase 0
`ItemOnObjectRegistry` so `UseItem` is never edited.

### 7.2 Pickables — R wins, N1 is 3 hardcoded cases

R: `{objectId,itemId}` over cabbage `1161`, flax `2646`, wheat `313`, potato `312`,
onion `3366`; 1800 ms throttle; animation `827`; deplete 5 ticks and restore. Ours has
none. Register via `ObjectHandler.register(id, ObjectClick.FIRST, …)` so the legacy
switch is untouched. Respawn through N1's `globalObjectRespawnTask` idea (or our
`ObjectManager`).

### 7.3 Working objects around the world

Make it measurable rather than aspirational. Port R's small self-contained set as data
tables + one registered action each: `Searching` (`{objectIds[],message}` enum),
`FlourMill`, `Webs`, `Levers`, `Climbing` (stairs/ladders), `SpecialObjects` (Al-Kharid
10 gp gate, Shantay pass), `UseOther`. Then:

- **A generic fallback**: an object with a recognised option that nobody handles
  currently does nothing *silently*. Register a fallback answering "Nothing interesting
  happens." Cheap, and makes the world feel finished without pretending to implement
  everything.
- **A coverage audit** (`tools/qol`): list object ids whose options include
  `Search`/`Climb`/`Pick`/`Open`, cross-reference `ObjectHandler` + `ActionHandler`, print
  handled/unhandled. Phase done when **every such object is handled or consciously
  listed**.

### 7.4 Doors and gates — ours is better than it looks

Ours is **split in two**: `Data/doors.txt` → `Doors.getSingleton()` (single doors), and
`Data/cfg/doors.cfg` → `ObjectHandler.loadDoorConfig` + `doorHandling` with
`doors[MAX_DOORS][5]` and a header documenting the face arithmetic (South 0 → face −3,
East −1 → 0, North −2 → −1, West −3 → −2). `DoorObjects.register()` covers 11 ids +
gates `1516`/`1519` with their `objectY == 9698` branch preserved.

**N1 is worse**: doors are hardcoded `case`s, the generic `Door.create(...)` path is
commented out, and there is no `Door`/`Gate` class. **R is the reference**, with
`DoubleGates` + `GateHandler` (`gateTicks` auto-close, `gateAmount` 0/1/2) and the
face-math comments.

Proposal: unify our two door data sources into `Data/cfg/doors.cfg`; add a `GateHandler`
for double gates with tick auto-close; add an audit test that no door id registers twice.
Do **not** rewrite `Doors.java`'s traversal — it is load-bearing for Barrows.

---

## 8. Content breadth

### 8.1 Shops

We are already file-driven: `Data/cfg/shops.cfg`
(`shop = <id> <name> <sellMod> <buyMod> <item> <amount> …`) and
`Data/cfg/npc-shops.cfg` (`npc-shop = <npcId> <shopId>`) — and our `npc-shops.cfg` header
admits it is a **partial** extraction. N1 uses `data/def/json/world_shops.json`, an array
of `{id, name, currency, items:[{id, amount}]}` loaded into a `Map<Integer,Shop>`, with
NPС/dialogue code holding shop ids as magic ints and `getShops().get(id)` **unguarded
(NPE on an unknown shop)**.

Proposal, purely additive: import R's ~143 shops **as new shop ids** that collide with
none of ours (script diffs ids and names first; a collision is a hard fail, not a merge);
convert N1's JSON shops to `.cfg` rows; **validate every item id** against `item.cfg` and
*warn* (per §2) for ones not yet defined; add `npc-shop` rows only where an NPC has no
mapping today, which also completes our partial file. No existing row is edited.

### 8.2 Potions — the overload/mixing groundwork

Ours: `Potions.handlePotion` with 73 cases, dose text parsed from the item name, a
1200 ms `potDelay`, and `PotionMixing.mixPotion2` decanting already wired.

N1 has the model worth taking: **doses are separate item ids per dose**, with one master
table —

```38:45:src/main/java/com/ruse/world/content/skill/impl/herblore/PotionCombinating.java
STRENGTH(119, 117, 115, 113, VIAL, "Strength"),
SUPER_STRENGTH(161,159, 157, 2440, VIAL, "Super strength"),
ATTACK(125, 123, 121, 2428, VIAL, "Attack"),
...
```
`(1-dose, 2-dose, 3-dose, 4-dose, vial, name)`, with dose 0 → empty vial `229`; plus
`FinishedPotions(finished, unfinished, itemNeeded, levelReq, exp)`, `UnfinishedPotions`,
`Crushing`, `Decanting`, and the extremes/overload chain:

```119:124:src/main/java/com/ruse/world/content/skill/impl/herblore/Herblore.java
EXTREME_ATTACK(new Item[]{new Item(145), new Item(261)}, new Item(15309), 88, 220),
EXTREME_STRENGTH(new Item[]{new Item(157), new Item(267)}, new Item(15313), 88, 230),
EXTREME_DEFENCE(new Item[]{new Item(163), new Item(2481)}, new Item(15317), 90, 240),
EXTREME_MAGIC(new Item[]{new Item(3042), new Item(9594)}, new Item(15321), 91, 250),
EXTREME_RANGED(new Item[]{new Item(169), new Item(12539, 5)}, new Item(15325), 92, 260),
OVERLOAD(new Item[]{new Item(15309), ...}, new Item(15333), 96, 300);
```

and the effects in `Consumables`: `drinkStatPotion` swaps the dose item and boosts
`max*0.13+1`, super `max*0.20`, `getExtremePotionBoost` = `levelForExp*0.25+1`, and
`OverloadPotionTask` (re-applies `overloadIncrease(…, 0.19)` every 30 ticks, 100 damage
every 2 ticks for the first 10 ticks).

Proposal: a **gap-fill plus a model upgrade**, not a port. Adopt the dose table as a
`Map<Integer, PotionDef>` — `{doseIds[4], effect, boost, name}` — so a potion is a row,
not 4 `case`s (this is *why* the 73-case switch does not scale to overloads). Add the
missing 2006 potions (antipoison, antifire, energy, agility, fishing, super
atk/str/def/range, super restore, Zamorak brew). Leave overload/extreme rows written but
inert per §2 until you import those items — which is exactly the case you described.
Keep the existing `(n)` dose message behaviour for output compatibility, but source it
from the table, not from name parsing.

### 8.3 Guilds — new in every source

Neither base has skill guild gating (N1's `clan/Guild.java` is a "not ready yet" stub; its
`WarriorsGuild` is a minigame). Ours has no guild system; our
`minigames/rangersguild/RangersGuild.java` (11.8 KB) is the only guild activity. **R's
`Guilds.attemptGuild` requirement table is the reference:**

| Guild | Object id(s) | Requirement |
|---|---|---|
| Champions | 1805 | all quest points |
| Legends | 2391/2392 | Legends complete |
| Prayer | 2641 | Prayer ≥ 31 |
| Cooking | 2712 | Cooking ≥ 32 + chefs hat `1949` |
| Crafting | 2647 | Crafting ≥ 40 + brown apron `1757` |
| Mining | 2113/1755 | Mining ≥ 60 |
| Fishing | 2025 | Fishing ≥ 68 |
| Wizards | 1600/1601 | Magic ≥ 66 |
| Ranging | 2514 | Ranged ≥ 40 |

Build `server/game/content/guilds/Guilds.java` with that table, each object registered via
`ObjectHandler` so `ActionHandler` is untouched, entry movement per guild, Rangers' Guild
kept as-is. **Validate every object id against our cache first.**

### 8.4 Gnome glider — verify only

Ours is **already complete** and carries the same six routes as R (Mountain `2848,3497`;
Grand Tree `2465,3501,3`; Castle `3321,3427`; Desert `3278,3212`; Crash Island
`2894,2730`; Ogre `2544,2970`), interface `802`, config `153`, 3-tick/4-tick events,
buttons `3056-3060`/`48054`. Confirm all six buttons reach the client, no `0,0`
coordinates, correct plane, and pin the six rows in a test.

---

## 9. Teleport hub — the Necrotic idea, with the real design

Your screenshot is N1's hub. The actual implementation is
`world/content/TeleportInterface.java` (the hub is **interface `44000`**; `50100` is a
stale whitelist id in `TeleportHandler.interfaceOpen`) driven entirely by enums:

```15:25:src/main/java/com/ruse/world/content/TeleportInterface.java
public static String[] CATEGORIES = {"Cities", "Monsters", "Dungeons", "Bosses", "Minigames", "Wilderness"};
public static int[] PAGEOPTIONS = {44202, 44402, 44602, 44802, 45002, 45202};
public static int[] PAGEBUTTONS = {-21434, -21234, -21034, -20834, -20634, -20434};
public static int INTERFACE_ID = 44000, DESCRIPTION_TITLE = 44086, TITLE = 44005, CATEGORY_ID = 44072,
    NEW_CATEGORY_ID = 44048, TELEPORTBUTTON = 44096, PREV_BUTTON = 44089;
public static int DESCRIPTION[] = { 44087, 44088 };
public static int PREVIOUS[] = { 44090, 44091, 44092, 44098 };
```

`TeleportInterfaceData` is one enum entry per destination —
`(stringId, descriptionTitle, text1, text2, TeleportLocations destination, buttonId)` —
placed by **arithmetic**: label widget = `PAGEOPTIONS[cat] + i`, click button =
`PAGEBUTTONS[cat] + i`. Action buttons: teleport `-21443`, exit `-21534`, history
`-21446/-21445/-21444/-21438`; `handleButton` accepts the whole range
`-21534 … -20428`. Destinations come from `TeleportLocations`, an enum of ~90
`(Position, hint)` pairs grouped as Cities / Monsters / Dungeons / Modern Bosses /
Oldschool Bosses / Minigames / Wilderness / Commands — matching your screenshot exactly,
including the hint messages ("Welcome to the Edge of the world.") and entries like
`EDGEVILLEDITCH` and `CHILL` visible in its History panel. History is per-player,
persisted (`"p-tps"` in the save, 4 de-duped recents). `TeleportType` supplies
start/end animations+graphics+tick per style (`NORMAL(3,…)`, `ANCIENT(5)`, `LUNAR(4)`,
`TELE_TAB(2)`, `RING_TELE(2)`, `LEVER(-1)`, `PURO_PURO(9)`), and `TeleportTabs` maps
tablet item ids → destinations.

**What we have:** `Data/cfg/teleports.cfg` (`teleport = <Category> <Name> <X> <Y>`) parsed
by `Config.loadTeleports()` into `Config.teleportLocations`, already carrying **11
categories / ~70 destinations** — Modern, Ancient, Glory, Monster, Boss, Minigame, PK,
Skill, Master, Fishing, Island. The *UI* is hardcoded: option buttons `9190-9194` with a
`teleAction` category number, plus a few custom ids (`117131`, `117154`, `117162`,
`117186`). Our client **also** already exposes Lunar teleport category buttons —
`30064` "Monster Teleports", `30083` "Minigame Teleports", `30106` "Boss Teleports",
`30114` "PK Teleports" (`Interfaces.java:791-800`). `TeleportObjects` is a separate
64-row object-click table with its own test.

**Proposal, staged so it cannot break anything:**

- **Stage A (no client work).** Add a `TeleportHub` that reads `teleports.cfg` so the file
  becomes the single source of truth, and generate the existing option menus from it
  instead of hardcoding them. Deliverable: the ~70 destinations become data, the
  `teleAction` ladder is retired, and "restore the original RS teleport locations" becomes
  *adding rows*. Adopt `TeleportType`'s animation table and `TeleportTabs`' item→dest
  table (great for tablets), and add the 4-entry persisted History.
- **Stage B (client work).** Build a `44000`-equivalent interface in
  `Proxy Client/src/ui/Interfaces.java` for the screenshot's two-list look. Our client
  builds interfaces programmatically and id space is large, but this needs sprites and is
  the largest client change in this plan. **N1's `44000`, `PAGEOPTIONS` and `PAGEBUTTONS`
  ids are its client's, not ours** — they will not exist here, so Stage B is a build, not
  a copy. Do Stage A first; treat B as a separate decision.

Do **not** copy N1's `handleButton(id)` numeric-range hack — it silently swallows any
future button id in that range.

---

## 10. Sounds — do last, deliberately

The transport already works on both ends:

- Server: `PlayerAssistant.sendSound(int id, int type, int delay)` (`:2078`) → frame
  `174` (word id, byte volume, word delay).
- Client: `Proxy Client/src/client.java:14786` — `case 174:` reads the same three.
- Music is a separate, working path (`createFrame(74)`), so this is only about effects.

What is missing is *our* catalogue. N1's `Sounds.Sound` enum is the shape to copy —
named, and `getSound()` picks randomly from an array:

```14:35:src/main/java/com/ruse/world/content/Sounds.java
ROTATING_CANNON({941}), FIRING_CANNON({341}), LEVELUP({51}), DRINK_POTION({334}),
EAT_FOOD({317}), EQUIP_ITEM({319,320}), DROP_ITEM({376}), PICKUP_ITEM({358,359}),
SMITH_ITEM({464,468}), SMELT_ITEM({352}), MINE_ITEM({429,431,432}), FLETCH_ITEM({375}),
WOODCUT({471,472,473}), LIGHT_FIRE({811}), TELEPORT({202,201}),
ACTIVATE_PRAYER_OR_CURSE({433}), DEACTIVATE_PRAYER_OR_CURSE({435}),
RUN_OUT_OF_PRAYER_POINTS({438}), BURY_BONE({380});
```

**Those ids are N1's revision.** Our cache indexes sounds differently, so copying the
numbers produces wrong noises that are hard to attribute. So: build **our** catalogue by
probing candidate ids against our client/cache and confirming by ear; ship a `Sound`
enum of *confirmed* ids only; wrap `sendSound` so call sites name a sound, not a number;
attach to skilling first (fletch, mine, smith, anvil, tree, fish), then
pickables/fillables/doors, then combat; master flag `Config.SOUND_ENABLED`. If probing
shows the cache is too thin, stop and say so — nothing depends on it.

Also take N1's idea of a **user audio toggle** (`soundsActive`/`musicActive` on buttons)
if we do not already have one.

---

## 11. Higher-revision import roadmap (why §2 matters)

You plan to import higher-revision content one at a time. The pattern that makes that
safe, in order:

1. **§2 lands first** — the never-null accessor, the hardened name paths, and the
   warn-not-fail validator. Until this exists, every new id we reference is a potential
   NPE in existing name-based code (`Item.getItemName` returns `null` today).
2. **Features you want early are written with ids now.** Overload/extreme rows
   (`15308–15335`), higher-rev shop stock, new potions — all written against ids that do
   not resolve yet. They stay inert and light up on import.
3. **A definition import is then a data change, not a code change.** Adding rows to
   `Data/cfg/item.cfg` (or a supplementary `item-extra.cfg` so the main file stays
   diffable) flips those features on. `ITEM_LIMIT = 25000` accommodates N1's ~22,694.
4. **Models are a client concern.** Item models/sprites are client-side; the server side
   is already tolerant, so a model import does not need a server change.
5. **A per-import checklist + test.** Each import: run the validator (expect warnings to
   disappear, no new failures), re-run the suite, confirm the previously-inert feature
   now works.

Recommended: keep imported definitions in a **separate `Data/cfg/item-extra.cfg`** loaded
after `item.cfg`, so our original 1.8 MB file stays untouched and the import is one
reviewable, revertible file per batch.

---

## 12. Sequencing

| # | Phase | Why here | Depends on |
|---|---|---|---|
| 0 | Registries + §2 safe accessor + validator | Everything after registers instead of editing switches, and may reference ids that do not exist yet | — — **done** |
| 1 | Fletching realism (1-by-1, shaft fix, stringing) | Your headline; small; very visible | 0 (optional) — **done**: bows, shafts, arrows, bolts, tipping and stringing |
| 2 | Random events, flag-driven (nest + genie on) | Cheap, visible, exact flags you asked for | 0 — **done**; further classics need their dialogue written first |
| 3 | Skilling completeness (N1 tables: potions, gems, glass, spinning, agility, rune/smith) | The "done to full completion" goal; §8.2 dose model | 0, 1 (pattern) — **spinning + pottery + weaving + soft clay + darts/arrows/bolts + herblore + runecrafting (altar, both essences, pouches) + anvil smithing + jewellery done + creation chatbox menus reused for gem cutting, tanning and jewellery (§15)**, glass is UI-blocked (§4e), agility **parked** (see the log entry for 2026-10-10), the `Check*` smithing table still open (§4g), the damaged pouches still open (§4i), amulet stringing still instant (§4j) |
| 3b | Authored quantity-first creation interface (client) | The shape §15's screenshots actually show, and the only way the anvil grid (`994`) reaches the chatbox | 3, and a client rebuild — **not started**; jewellery turned out not to need it (§4j) |
| 4 | World interactivity: fillables (R), pickables (R), searchable/climbable scenery, doors/gates | The "feels finished" layer; mostly data + registrations | 0 |
| 5 | Shops, potions breadth, guilds, glider verify | Pure breadth, additive data; safest wins | 0 |
| 6 | Bank PIN | One genuine client/UI decision first | 0 |
| 7 | Teleport hub Stage A (data-driven menus + History), then decide on Stage B | Ties `teleports.cfg` to the UI; enables restoring original RS destinations | 0 |
| 8 | Sounds | Explicitly last; revision-dependent; stop-worthy | everything |

Each phase ships and reverts independently via its flag; each gets an `UPDATE_LOG` entry,
and anything spanning a session is logged `partial` with what remains.

---

## 13. Bank PIN (Phase 6 detail)

The hook already exists and is a stub. Client: `addHoverButton(5294, "BankTab/BANK", 3,
114, 25, "Set a Bank PIN", 0, 5295, 1)` on bank interface `5292`
(`Interfaces.java:2602`). Server: `case 5294: c.sendMessage("A bank PIN is not required
on this server.");` (`ClickingButtons.java:2396`). Bank opens with
`sendFrame248(5292, 5063)`; **no PIN field, keypad interface or persistence exists**
anywhere in `src`, `Proxy Client` or `Data`.

N1 has no bank PIN either (it is not in `com.ruse`), so R is the only reference
(`game/content/BankPin.java`, interface `7424`, choose/confirm/re-enter, randomised pad,
unlock-on-open, delete-delay recovery, `PlayerSave` tokens `hasBankpin`,
`bankPin1..4`, `enterdBankpin`, `pinDeleteDateRequested`).

Proposal: a `BankPin` class + `PlayerSave` fields + `Config.BANK_PIN_ENABLED`; register
button `5294` through `ButtonHandler` and **delete** the now-dead stub case (don't leave
two paths). The open question is the UI — R's pad `7424` almost certainly does not exist
in our client, so either reuse the bank's chatbox numeric input (zero client work), or a
custom keypad (client work). Also decide the **bot interaction**: bots occupy real
`players[]` slots (`BOT_PARKED.md`), so they need a bypass, or PINs must be opt-in.

---

## 14. Open decisions for you

1. **Bank PIN UI** — chatbox numeric input (no client work), reuse an existing
   keypad-shaped interface (needs a client audit), or build a custom pad (client work)?
2. **Teleport hub** — Stage A only (data-driven menus from `teleports.cfg` + History, no
   client work), or also Stage B (the bespoke two-list `44000`-style interface)?
3. **Bots and bank PINs** — bypass for bots, or opt-in PINs only?
4. **Farming** — take it on properly (persisted patches, all patch types, growth timers),
   or defer and record it as the one skill we knowingly leave partial? N1's farming is
   not a usable reference either.
5. **Ticked-skilling default** — make every production skill OSRS-slow, or only fletching
   with the rest behind a flag defaulted to today's fast behaviour? This is a gameplay
   feel decision and it is yours. **What has actually happened since:** the production slices have
   been ticked without a flag — fletching, herblore, spinning, weaving, pottery, and the anvil — so
   the answer in practice is trending towards "ticked by default", at two cycles an action.
   **Runecrafting came back out of that on 2026-10-10 (§4h)**: an altar takes the whole pack in the
   click, because that is the shape of the skill rather than a speed setting. If you want a global
   fast/slow switch, say so before the next skill lands.
6. **Definition import file** — happy with a separate `Data/cfg/item-extra.cfg` for
   imported higher-revision items, leaving `item.cfg` untouched?
7. **Creation menus in the chatbox** — do you want the "choose what to make" menus moved from
   the sidebar into the chatbox, as in the screenshots? Recorded in §15, and **answered in
   practice for the menus that had no chatbox form:** gem cutting and tanning now open the chatbox
   on interface `8938` (2026-10-10), and herblore (`4429`) was already there. The remaining
   families — the anvil's grid (`994`) and jewellery (`4161`) — are the ones that need an authored
   interface rather than a reuse, so the open question is whether to commission that, not whether
   the chatbox is the right place. **Jewellery answered half of this without any authored work
   (§4j):** `4161` keeps the product choice and the chatbox took the amount, which is the only place
   an "all" can exist because `4161` has no make-all option. The anvil grid is the one still waiting.
8. **Runecrafting experience** — should the award scale with the rune multiplier, as it does in
   OSRS (so an air essence at 99 is 5 × 10 × the rate rather than 5 × the rate)? It is currently
   flat per essence, matching the pre-existing behaviour, because `Config.RUNECRAFTING_EXPERIENCE`
   is already 15× and compounding it with a further ×10 is a balance decision. §4g, §4h.
9. **Rune essence vs pure essence at the high altars** — as of §4h either essence works at every
   altar, rune essence spent first. OSRS gates the higher altars (cosmic and above, by most
   accounts) on pure essence. Do you want that tiering modelled, and if so at which altars? It is
   left out because the exact rung is a rule I would be guessing at, and a guess here is a player
   standing at a nature altar being told they have the wrong essence. **Pouches mirror the same
   order as of §4i** — either essence goes in, rune essence drawn first — so if this is ever
   decided, the pouch should be decided with it rather than left half-answering the question.

---

## 15. Creation menus in the chatbox (built, 2026-10-10)

**Recorded on request, 2026-10-10.** The wish: when you are making things, the choices should come
up **in the chatbox** rather than only in a sidebar interface — the pick-what-then-pick-how-many
prompt as a chatbox menu, the shape interacting with things has. It is about item creation
generally (fletching, crafting, smithing, herblore, and whatever else grows a recipe table), not
about one skill.

Reference for the exact look: nine screenshots in
`C:\Users\llrbi\Desktop\inspiration on chatbox ui when making interacting with things`
(`Screenshot 2026-10-10 1432*.jpg` through `1445*.jpg`).

**The screenshots have now been read.** They could not be read in the session that recorded this
request, so a new `Proxy Client/tools/read-chatbox-screenshots.ps1` was written to do it with the
Windows OCR engine (upscaled, with per-line boxes, because the quantity selector is graphics and
the chatbox font is small). What they actually say:

- Eight of the nine are 601x194 — the chatbox — and they share one shape: a **per-skill question**
  ("What would you like to make?", "How many gems would you like to cut?", "How many would you like
  to cook?", "How many would you like to string?", "How many sets of 12 will you make?"), a **help
  line** reading "Choose a quantity, then click an item to begin.", product icons below, and the
  ordinary chat tabs still along the bottom.
- The ninth is 749x488 and is the **smithing grid** — Dagger 1 bar, Sword 1 bar, Scimitar 2 bars,
  Cannon parts 5 bars — which is the shape of interface `994`.

**Correction to what this section used to claim.** It said the bow menu at `8880` is "a sidebar
interface, not the chatbox". It is not: `sendFrame164` *is* the chatbox packet — the client's
`case 164` sets `backDialogID`, which is the chat area. Checked family by family, most creation
menus were already in the chatbox before this entry: herblore `4429`, fletching bows and shafts
`8880`, leather and snakeskin `8938`/`8880`, pottery `8938`, smelting `2400`, cooking `1743`. The
families that genuinely were not are in the table below.

**What landed (2026-10-10).** One shared `server/content/skills/CreationMenu` over interface
`8938` — five rows, four amounts a row (1/5/10/28), no client change, because the frame, the title
and the buttons are all in the cache. The server fills each row's model, each row's label, and the
row that was clicked; `CreationMenu.route` is the single place that decides which family a shared
button belongs to, off the player's open menu rather than off the id, which is what stops one click
being read twice.

- **Gem cutting had no menu at all** — the chisel cut the whole stack, silently, until it ran out.
  It now opens the chatbox on the uncut gems in the pack, and the button ends the batch.
- **Tanning had its own overlay** (`14670`) with a price frame per row. It is now a chatbox menu.
  The price survives on the row's label ("Green d'hide - 20 gp"), because the chatbox interface has
  no price frame. Its rows are **products, not hides**: cowhide makes soft *and* hard leather, so a
  menu keyed on the hide would have made that choice unreachable. The duplicate snakeskin row (same
  hide, same leather, 15 gp and 20 gp) is gone.
- **Both cap at five rows and say so.** 8938 holds five; a player holding more kinds than that
  sees the first five the table lists plus a message, rather than a menu that quietly omits the
  rest. This is the one wart of the reuse, and it is the reason the authored interface below is
  still worth considering.

**Still open, and why:**

| Family | Today | Why it stayed |
|---|---|---|
| Smithing anvil | overlay `994` | ~30 products. A 165 px chatbox cannot show that grid legibly; it needs paging or a bigger authored interface. |
| Glass | nothing | Blocked on the same missing interface as §4e. |

**Jewellery moved off this list (2026-10-10, §4j) — and it is the clearest case for what the
chatbox half is for.** The mould interface `4161` keeps the product choice, because 21 products do
not fit five rows. But reading `4161` out of the cache showed its three containers carry exactly
three actions — `Make`, `Make 5`, `Make 10` — and two empty slots, so a make-all click is not
something this client can send and the server cannot be "missing a handler" for it. The chatbox is
the only place an "all" can exist, so the left click opens a one-row `CreationMenu` whose whole job
is the amount. That is a narrower use of the 8938 menu than the gem and hide cases, and it is worth
noting as the pattern to reach for: **when an existing interface can pick the product but not the
amount, reuse the menu for the amount rather than rebuilding the picker.**

**The one thing the screenshots ask for that this does not do.** Their interaction is
**quantity first, then item** — pick 1/5/10/X/All, then click the product. Every menu here is the
other way round: the amount is built into the row, so one click is product and amount together.
Matching the screenshots exactly needs a new authored chatbox interface with a quantity selector,
which is client work and a client rebuild. Reusing `8938` was chosen instead for this slice because
it needed no client change at all; the authored version is the natural follow-up, and smithing is
the family still waiting on it.

**One exception to "quantity first" landed with jewellery (§4j).** There the product is chosen in
interface `4161` and the chatbox is opened afterwards just for the amount, which is the screenshots'
order — because `4161` has no make-all option to offer and the menu does. So the second half of the
screenshot shape is already reachable without anything being authored; only the first half needs it.

**Not needed for darts, arrows or bolts**: those are item-on-item recipes with no menu to move —
one pair is one recipe, so there is nothing to choose. A menu earns its place where one material
has several products (logs → bows and shafts, gems → jewellery, herbs → potions).

**The interface does exist in this client** — that part of the old note was the §4e wall, and it
does not apply here. `8938` was already in the cache and this server was already using it for
pottery, leather and snakeskin when this slice started; gem cutting and tanning were added to it
without touching the client. Herblore's `4429` is the same story. **Only a new shape** — the
quantity-selector-first layout the screenshots show, or a grid big enough for smithing — would be
authored work and a client rebuild.

**Scheduled:** the reuse has landed, so this is no longer an unscheduled request. What remains
open is the authored quantity-first interface, and it belongs with the client-side work in §12.
