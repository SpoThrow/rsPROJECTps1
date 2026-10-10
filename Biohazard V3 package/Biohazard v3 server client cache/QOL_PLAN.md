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
| **Runecrafting** | `craftRunes` crafts the **whole inventory in one `while`** (`:65-70`). | N1 | Tick per essence; rune/multiplier table. |
| **Smithing / Smelting** | Instant `while (maketimes > 0)`; 26.9 KB interface. | R numbers, N1 shape | Keep our interface; tick the loop; port product/XP/level as a table. N1's `SmithingData` is 55 KB. |
| **Jewellery / Tanning / Leather** | Instant batches. | N1 | `Gems`, `Jewelry`, `Tanning`/`tanningData`, `LeatherMaking`/`leatherData` tables; make ticked. |
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
| 1 | Fletching realism (1-by-1, shaft fix, stringing) | Your headline; small; very visible | 0 (optional) — **bows and shafts done**, stringing/arrows/bolts still open |
| 2 | Random events, flag-driven (nest + genie on) | Cheap, visible, exact flags you asked for | 0 — **done**; further classics need their dialogue written first |
| 3 | Skilling completeness (N1 tables: potions, gems, glass, spinning, agility, rune/smith) | The "done to full completion" goal; §8.2 dose model | 0, 1 (pattern) |
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
   feel decision and it is yours.
6. **Definition import file** — happy with a separate `Data/cfg/item-extra.cfg` for
   imported higher-revision items, leaving `item.cfg` untouched?
