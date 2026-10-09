# Update Log

## 2026-10-09 - Bot Workshop T1: isolated world exporter + parity validator

**What changed:**
- **Isolated source set.** `workshop` + `workshopTest` in `Proxy Server/build.gradle`, with `workshopTest`, `workshopExport` and `workshopValidate` tasks. Nothing here reaches the server jar; no server source file was modified.
- **Decoders** (`botworkshop.data`). `MapIndex` reads `map_index` into region/landscape/object file ids and tolerates the **51** regions that have no landscape file, matching what `Region.load()` skips at boot. `GroundMap`/`GroundTile` port the client-style terrain byte stream (heights, overlay floors 2-49, flags 50-81, underlay floors 82+) and **keep the overlay/underlay floor ids that the server's `Region.loadMaps()` discards**, which is what makes the map renderable. `LocDefs`/`LocDefinition` decode all **42001** `loc.dat` entries using the `0x00` terminator this cache actually uses.
- **Classifier and serializers** (`botworkshop.classify`/`export`). `ResourceRules` maps actions and exact names to icon categories (tree/rock/fishing/bank/cooking/smithing/prayer); bank booths such as object `2213` carry no `Bank` action and are matched by exact name so that scenery like "Bank wall" is not over-matched. Zero-dependency `Json` builder, `Rle` (`value*count` runs per 64x64 plane), and `RegionDocument`, which emits name, actions, category, footprint (`objectSize.cfg`) and the server-verified clipping bits.
- **CLIs.** `ExportMap` writes `Data/workshop/map/<regionId>.json` plus `index.json`, defaulting to three landmark regions (Lumbridge 12850, Draynor 12338, Varrock 12853) instead of all 1175 (~500 MB) unless `-PworkshopRegions=all` is passed. `ValidateMap` re-reads the written JSON and checks terrain occupancy against the live `Region.getClipping` bit `0x200000`.
- **⚠️ A server defect, measured rather than assumed.** `ValidateMap` now also reports how far the server's own reader diverges from this tool's, and the number is total: of **19410 named objects, `ObjectDef.getObjectDef` returns a name for 0 of them and falls back to `setDefaults()` for all 19410.** The cause is exact — `getObjectDef` always prefers `archive` → `readValues`, whose `readString()` scans for `0x0A`, while this cache terminates strings with `0x00`; the parse therefore throws and the catch applies defaults. The reader that is correct for this file, `readValues377` (`readNewString()`, `0x00`), is only reached when the archive has *no* entry for the id, which is never true for `loc.dat`. **The client does not share the bug** — its `forID` calls `readValues474`, which uses `readNewString()`. So for every named object the server holds a null name, null actions and `aBoolean767 = true`, while the client holds the real values. **Deliberately not fixed here:** it changes world collision globally and needs live verification, so it is logged below as the top follow-up rather than slipped into tooling work.

**Files touched:**
- `Proxy Server/build.gradle`, `.gitignore`
- `Proxy Server/workshop/src/botworkshop/**` (new), `Proxy Server/workshop/test/botworkshop/**` (new)

**Status:** done. **447 tests, 0 failures** (385 server + 62 workshop); `workshopValidate` reports **0 mismatched tiles**. Next: `BOT_TOOLING.md` **T2** — the web map viewer over `Data/workshop/map/*.json`, or the `ObjectDef` reader fix first; that ordering is the user's call.

## 2026-10-09 - Bot slice 1 (Phase A), steps 3-7: a working chop-to-bank bot

**What changed:**
- **Step 3 — `BotManager`.** `createAccount`/`possess`/`release` with a `MAX_BOTS` (10) cap; `possess` refuses duplicates and wrong passwords, and reuses the existing `PlayerSave` path so bots persist like players.
- **Step 4 — behaviour tree core.** `BotStatus` (`RUNNING`/`SUCCESS`/`FAILURE`), `BotState` contract (`enter`/`tick`/`exit`/`name`), `BotContext`/`PlayerBotContext` (sense + intent), `BotController` (ticks the root during `process()`), and the `Sequence`/`Repeat` composites.
- **Steps 5-7 — the vertical slice.** `WalkTo` (range-aware, bounded `noProgressTicks` stuck budget), `ChopTree` (dispatches through `ObjectHandler.dispatch` → `Woodcutting`, so bot code never calls a skill class), and `BankLogs` (open + deposit all). Step 7 wires them as `Repeat(Sequence(WalkTo(tree), ChopTree, WalkTo(bank), BankLogs), -1)`.
- **Key enabler:** `PlayerHandler.registerSessionless(Client)` allocates a slot atomically and flags `connectedFrom = "bot"`. Bots are ordinary `Client`s in `PlayerHandler.players`, so no skill inventory or movement code was forked.
- **Tests:** `BotManagerCapTest` (8), `BotStateMachineTest` (10), `WalkToStateTest` (3), `ChopTreeStateTest` (3), `ChopBankLoopTest` (1 — drives real ticks, no network, asserts logs actually reach the bank). Bot package total **38**.

**Files touched:**
- `Proxy Server/src/server/game/players/{Player,PlayerHandler}.java`
- `Proxy Server/src/server/game/bots/**` (new: manager, context, controller, composites, states)
- `Proxy Server/test/server/game/bots/**` (new)

**Status:** done. Full suite **385 tests, 0 failures**. Next: `BOT_TOOLING.md` T1+ — the visual workshop editor, now backed by a real runtime data model.

## 2026-10-09 - Bot slice 1 (Phase A), steps 1-2: sessionless safety + bot identity

**What changed:**
- **Step 1 — sessionless flush guard.** `Client.flushOutStream()` now drops buffered bytes when `session == null` instead of dereferencing it, so a bot cannot NPE and cannot grow `outStream` past `Config.BUFFER_SIZE` (10000).
- **Step 2 — bot identity/persistence.** Added `Player.isBot` (a label, **not** a save gate), `BotNames` (reserved `bot` prefix + login-legal checks mirroring `RS2LoginProtocolDecoder`), and `BotPlayer extends Client` (sessionless, `isBot=true`, no-op `update()`, persists like a player).
- New tests, all green: `SessionlessFlushTest` (4), `BotNamesTest` (7), `BotPersistenceTest` (4).

**Files touched:**
- `Proxy Server/src/server/game/players/Client.java`, `Player.java`
- `Proxy Server/src/server/game/bots/{BotNames,BotPlayer}.java` (new)
- `Proxy Server/test/server/game/bots/*` (new)

**Status:** partial. Per `BOT_PLAN.md` §6 the remaining steps are 3-7: `BotManager` possess/release + cap, `BotContext`/`BotState`/`BotStatus` + `Sequence`/`Repeat`, `WalkTo`, `ChopTree`, and the `BankLogs` end-to-end loop. Editor (`BOT_TOOLING.md` T1-T7) follows the runtime data model by design.

## 2026-10-09 - Bot Workshop data probe (throwaway tool) + loc.dat codec finding

**What changed:**
- Added **one throwaway tool**, `Proxy Server/tools/BotDataProbe.java` (outside the Gradle build), and ran it headlessly. Outputs: `Proxy Server/tools/botdata-dump.txt` (full) and `tools/probe-stdout.txt`. No server source changed.
- **Codec finding:** `loc.dat` strings are **`0x00`-terminated**, but `ObjectDef.readValues`/`readString()` waits for `0x0A`, so the server's default reader produces garbage `name`/`actions`. The probe decodes with the correct terminator: **42001 entries, 0 decode failures, 19406 named, 472 distinct action strings.**
- **Measured (not estimated):** handler union **261** ids (registry 132/37/0 + switch 100/4/1); def-only **10268**, both **258**, handler-only **3**. Icon rules (object ids / live tiles): Bank 33/176, Chop down 112/12951, Mine 795/1724, Pray 63/45, Cook 0, Smith 0, Recharge 0. `Region`: **1939931** `realObjects` over **809/1226** regions; nearest-resource scan 0.13 ms (distance) / 0.44 ms (+name) over a 9-region window.
- Also confirmed: `loadCustomSpawns` is per-client and 60-tile gated but clips **globally**; doors start closed and never auto-close; only object action slots 0-2 are reachable server-side.

**Files touched:**
- `Proxy Server/tools/BotDataProbe.java` (new, throwaway)
- `UPDATE_LOG.md`

**Status:** done. Docs only - no server source modified.

## 2026-10-09 - GWD gate verified dead + boss loot tables

**What changed:**
- **A - the GWD 20-KC gate is dead.** `Player.killCount` is loaded from the save and read at `ActionHandler:250`, but **never incremented anywhere** in the live code, so the boss-chamber teleport object (`2492`) always refuses. Documented the real route: teleport-interface boss button (spellTeleport 2902,3724) -> hole object **2823 @ 2903,3732** -> **2881,5310 plane 2** (via `TeleportObjects`), then build 15 faction kills and enter the chamber door (26425-26428).
- **B - boss loot tables (verified in `npc_drops.cfg`):** KBD (draconic visage 11286, dragon med 1149); Dag Kings split canonically (archers/seercull Supreme, seers/skeletal Prime, berserker/warrior/rock-shell Rex; dragon axe 6739 on Supreme+Prime); GWD bosses (hilts 11702/11704/11706/11708, Bandos chest/tassets/boots, Armadyl helm/chest/skirt, Saradomin sword 11730, dragon boots 11732, godsword shards 11710/11712/11714); KQ (dragon chainbody 3140, dragon 2h 7158).
- **Finding:** the Kalphite Queen first form (1158) has no drop entry - loot only comes from the second form (1160), so a bot must kill both forms.

**Files touched:**
- `BOT_GAMEPLAY.md` (S4 boss route + loot, open items)
- `UPDATE_LOG.md`

**Status:** done. Docs only - no source changes.

## 2026-10-09 - Gameplay loop analysis + bot motivation arbiter

**What changed:**
- Added `BOT_GAMEPLAY.md`: an analysis of the server's purpose and full gameplay loop, plus a new **motivation arbiter** design that decides *which* activity a bot runs (the *when*, above the *what* in `BOT_ACTIVITIES.md`).
- **Purpose/loop:** an economy + progression server with a PvP endgame; boosted XP (melee x600 vs woodcutting x15) makes combat the fastest income, and the loop is earn -> spend at home -> earn faster -> boss/PvP.
- **Bosses (verified gates):** KBD 50, Dag Kings 2881-2883, GWD Graardor 6260 / K'ril 6203 / Zilyana 6247 / Kree'arra 6222, KQ. GWD needs **`killCount >= 20`** at the teleport object (2492) **then 15 faction kills** at the chamber doors (26425-26428); faction counters cap at 15 and reset on leaving.
- **PvP (verified):** BH craters by combat band (3-55 / 50-100 / 95+), kit limit (<=4 weapons, 1 body, 1 legs), cash reward at 10+ bounty kills via Veteran Hervi, PvP points shop 47; `NO_TELEPORT_WILD_LEVEL = 20`.
- **Home:** Edgeville (`3087,3505`) - bank + shop mall; respawn is Lumbridge. "Chill" is the reset state (bank/sell/restock/re-gear), the arbiter's fallback, and a short jittered dwell that makes bots look human.
- Specified `Motivation`/`GoalArbiter` (score = hard state + archetype weight) and the boss/PvP/home rules, with fail-safes (never boss-gear into the wild, always bail at the HP/food floor, skip the two spawn-less slayer tasks).

**Files touched:**
- `BOT_GAMEPLAY.md` (new)
- `BOT_ROADMAP.md` (§5.2 cross-link)
- `UPDATE_LOG.md`

**Status:** done. Docs only - no source changes. Open item worth checking: `Player.killCount` is persisted but never incremented in the live path, so the GWD 20-KC teleport gate may be dead.

## 2026-10-09 - Resolved the data-layer open items

**What changed:**
- **Price source (verified):** buy price is `ItemList[id].ShopValue` - the first numeric field after the description in `item.cfg` - read via `ShopAssistant.getItemShopValue`; sell is 80%. No buy markup (the `*1.35` is commented out).
- **Shop NPC coordinates (verified):** found an **Edgeville shop mall within ~10 tiles of the start spawn** (`3087,3505`) holding the core gear shops - General (2), Aubury's Runes (6), Lowe's Archery (7), Horvik's Armour (8), Weapon Shop (11), Magician's Robes (14), Thessalia's Clothes (24). Mapped each NPC to its shop via `ShopNpcs.java` and noted the **click type** (Horvik is first-click, the rest second).
- Flagged that **Woodcutting Store (16) and the skill-master stores are button-only** (opened from `ClickingButtons`, no NPC), so the bot needs the interface path for axe upgrades.
- **Slayer legs (verified):** confirmed real spawns and teleports for the Slayer Tower (spans **3 planes**), Fremennik Slayer Dungeon, Brimhaven Dungeon, Kalphite Lair, Lumbridge Swamp Caves.
- **Finding:** **aberrant spectre (1604) and cave horror (4353) have no spawns** in `spawn-config.cfg` - those tasks are impossible on this server, so the bot must skip/block them.

**Files touched:**
- `BOT_LOCATIONS.md` (B.3, C.1, A.3, open items)
- `UPDATE_LOG.md`

**Status:** done. Docs only - no source changes. Next: implement Step 1 (`flushOutStream` guard), or start the exporter/map layer (T1-T2b).

## 2026-10-09 - Bot Workshop UX spec

**What changed:**
- Added `BOT_WORKSHOP_UX.md`: the editor's user experience, fleshing out `BOT_TOOLING.md`'s map/icon/timeline layers.
- Layout: one window, four regions (toolbar, layers/resource panel, map canvas, inspector) plus a bottom timeline strip, with three modes (Explore / Author / Region) so a click's effect is never ambiguous.
- Map: pan/zoom/plane, hover tile inspector that also shows the **nearest bank** to a tile (the check every gathering bot needs), and a region navigator for non-contiguous dungeons.
- Icon layer detail: auto-classified from `ObjectDef.actions`, with zoom-based clustering for level-of-detail, per-class toggles, hover labels and a legend.
- Timeline: the step palette is generated from `bot-nodes.json`; waypoints come from dragging a box (jitter = the `RandomTileIn` spread) and actions from right-clicking a map object (menu = the object's own def actions). Reorder/indent/repeat-wrap/undo, a dry-run playhead that draws the route on the map, and inline per-step validation.
- Timeline<->graph are two views of one document; the JSON preview is available live but never required.

**Files touched:**
- `BOT_WORKSHOP_UX.md` (new)
- `BOT_TOOLING.md` (§4 pointer, §10 stage references)
- `UPDATE_LOG.md`

**Status:** done. Docs only - no source changes. Next: implement Step 1 (`flushOutStream` guard), or start the map/icon layer (T1-T2b) it describes.

## 2026-10-09 - Locations service, equipment planner, Slayer registry

**What changed:**
- Added `BOT_LOCATIONS.md` covering three data-layer designs: (A) the `Locations` service, (B) the equipment-upgrade planner, (C) the Slayer registry.
- **A:** `Location`/`Locator`/`Locations` interfaces; one new `Data/cfg/bots/locations.cfg`, with teleports/shops/monsters **joined from existing files** rather than copied; resolution is curated -> cache -> scan; planes/dungeons handled as multi-leg travel; region waypoints reuse `Location` with `radius > 0`.
- **B — correction:** equipment requirements are **not** read from `item.cfg`. They are computed by **name-matching** in `ItemHandler.getRequirements`, and `req[]` is **indexed by skill id** (0 attack, 1 defence, 2 strength, 4 ranged, 6 magic). Noted the quirk that most armour tiers set **both** `req[0]` and `req[1]` (attack + defence), unlike retail - so the planner must read `ItemHandler.ItemList[id].req`, never re-derive.
- **C:** verified Slayer locations against real spawns (Slayer Tower confirmed ~18 tiles from its teleport; the rest need a dungeon/boat leg) and built the task table with **HP from `npc.cfg`** -> per-kill XP (`HP x 19`) and completion XP (`HP x 8 x 19`).
- **C — finding:** the per-task items (mirror shield, earmuffs, nose peg, bag of salt, spiny helmet) exist only in the Slayer **reward shop** (`SkillInterfaces`/`ShopAssistant`); **no combat code enforces them**, so bots do not need them to function. The proposed `requiredItems` column is informational.

**Files touched:**
- `BOT_LOCATIONS.md` (new)
- `BOT_ACTIVITIES.md` (§10 gap references)
- `BOT_ROADMAP.md` (§5.2 cross-link)
- `UPDATE_LOG.md`

**Status:** done. Docs only - no source changes. Next: implement Step 1 (`flushOutStream` guard), or resolve the open items (shop NPC coords, slayer travel legs, price source).

## 2026-10-09 - Server activity analysis (skills, travel, combat, shops, slayer)

**What changed:**
- Added `BOT_ACTIVITIES.md`: how a bot trains every skill, travels, fights, loots, banks, shops and runs Slayer tasks - the activity graph behind the "level everything to 99" goal.
- Each skill documented with its start method, game object/NPC ids, required tools, level, and XP formula (all XP is base x Config multipliers; e.g. woodcutting logs = 25 x 15, mining = 18 x 16, slayer kill = MaxHP x 19).
- Teleport network catalogued from `Data/cfg/teleports.cfg` (Modern/Ancient/Glory/Monster/Skill/Boss/Minigame/Master/Fishing), plus home = Edgeville `3087,3505` and respawn = Lumbridge `3221,3218` from `spawn-points.cfg`.
- Combat XP split read from `CombatAssistant` (controlled spreads across attack/strength/defence/hp; a specific style focuses one skill). Training spots by level band from `WorldAdventurer.SPOTS` + Monster teleports.
- Loot + banking loop from `npc_drops.cfg` (itemId:amount:rarity, coins = 995); shops from `shops.cfg` + `npc-shops.cfg` (bot buys its next gear tier with looted coins).
- Equipment requirements modelled from `ItemAssistant.wearItem` -> `ItemHandler.ItemList[id].req[]` in `item.cfg`.
- Slayer loop: master NPC 1597 (Vannaka) at `2871,2982` (= the `Skill Slayer 2873,2980` teleport), difficulty by combat level, full task table, slayer-level attack gates, kill/completion XP and points.
- Listed the data the `Locations` service must gain and honest gaps (per-task required items missing from `Slayer.Task`; `npc-shops.cfg` is a partial extraction; shop NPC coords and slayer-location->teleport mapping still unverified).

**Files touched:**
- `BOT_ACTIVITIES.md` (new)
- `BOT_ROADMAP.md` (§5.2 cross-link)
- `UPDATE_LOG.md`

**Status:** done. Docs only - no source changes. Note: the parallel research subagents failed (provider balance/rate limit), so this was gathered with direct reads; gaps in §10 remain. Next: implement Step 1 (`flushOutStream` guard) or build the `Locations` registry from `BOT_ACTIVITIES.md`.

## 2026-10-09 - Bot accounts and credentials (corrects the `[bot]` prefix)

**What changed:**
- Added `BOT_ACCOUNTS.md` covering account creation, naming and credentials for possessed bots.
- **Correction:** the `[bot]` name prefix from the first `BOT_PLAN.md` draft is **illegal**. The login path rejects any name outside `[A-Za-z0-9 ]` (`returnCode = 4`) and lowercases it, so a `[bot]Name` account could never be logged into - which defeats the possession model. The prefix is now `bot` (lowercase, no punctuation).
- **Correction:** bot accounts must **not** use the normal `addStarter` new-player path - it is IP-gated (`hasRecieved1stStarter(connectedFrom)`, so only the first bot per loopback IP would get a kit) and it starts a tutorial (`canWalk = false` + a forced dialogue) that would freeze the bot. Creation now provisions the profile kit directly.
- Recorded the other constraints read off the code: names ≤12 chars and lowercased, passwords lowercased and ≤20, and the character file stores only `md5` (so the plaintext must be recorded in `Data/cfg/bots.cfg` for a human to log in).
- Possession authenticates via `loadGame` with the recorded plaintext (no core change, same auth path as a human); a "trusted" bypass was rejected as a drifting second path.
- Specced **`BotProfiles` / direct provisioning** (`BOT_ACCOUNTS.md` §4.1): profiles mirror the `adventurerPid`/`pkerPid`/`skillerPid` archetypes and carry tie-flag, items, skills, start position and spellbook. `BotProvisioning.provision` replaces `addStarter`, which is IP-gated, two-tier, starts a movement-freezing tutorial, broadcasts a welcome, and grants 2,000,000 coins.

**Files touched:**
- `BOT_ACCOUNTS.md` (new)
- `BOT_PLAN.md` (amendment item 5, §5.2, §5.4)
- `UPDATE_LOG.md`

**Status:** done. Docs only - no source changes. Next: implement Step 1 (`flushOutStream` guard), or flesh out another area (locations, formats, control plane, realism).

## 2026-10-09 - Possession model + visual-authoring decisions

**What changed:**
- Resolved the bot model as **possession of real characters**: `BotManager.possess` loads-or-creates a character (`PlayerSave.loadGame`), registers a sessionless `Client`, and attaches a controller; `release` saves the character and removes it. A released account is a normal character file a human can log into and play.
- **Revised amendment 5**: bots are **no longer excluded from saving** - they persist exactly like normal players. The reserved `[bot]` prefix is now a naming convention only, and `isBot` no longer gates the save paths (so `PlayerSave`/`PlayerSaving`/`saveAllPlayers` stay untouched).
- Added the possession lifecycle + a sessionless `PlayerHandler` registration overload (the stock `newPlayerClient` needs a session for `connectedFrom`).
- Recorded the visual-editor decisions: **curated** resource/service icons auto-classified from `ObjectDef.actions` (`Chop down`/`Mine`/`Bank`/...), and a **step timeline** as the primary authoring UX (node graph deferred to the roadmap's Phase B).
- Added **region waypoints**: waypoints are boxes with jitter + a per-bot seed (`RandomTileIn`), so bots sharing a waypoint land on different tiles instead of the same one.
- Bots compile to **data** (`bots/*.json`), not generated Java.

**Files touched:**
- `BOT_PLAN.md` (scope, §3.4, file layout, §5.2, new §5.4, steps 2/3/7, §9)
- `BOT_ROADMAP.md` (§5.1, §5.2, phase table, `bots.cfg` examples)
- `BOT_TOOLING.md` (§1, §2, Layer 1b, Layers 3/3b, §6, §7, §10, §13)
- `UPDATE_LOG.md`

**Status:** done. Docs only - no source changes yet. Next: implement Step 1 (the `flushOutStream` sessionless guard).

## 2026-10-09 - Bot-maker tooling plan (visual bot editor)

**What changed:**
- Added `BOT_TOOLING.md`: a plan for a separate companion program (the "Bot Workshop") - a web app plus a Java exporter/validator CLI - that authors bots visually.
- Confirmed feasibility from existing code: the server already owns an offline map dataset (`Region.load()` reads `./Data/world/map_index` and `Data/world/map/*.gz`), and "patches" already exist as the `Patch` region class, so the tool draws/emits concepts the server already has.
- Defined three layers (map view, region/patch authoring, behavior graph editor), the shared `@BotNode` registry that generates the editor palette from the runtime (with a parity test), and the tool's output files (`locations.cfg`, `bots/*.json`, `bots.cfg`).
- Recorded decisions: own file as a sibling track; stack is web app + Java exporter/validator CLI; scope is bots only for now (general content editor deferred).
- Phases T1-T7, with a hard dependency on roadmap phases C (`Locations`) and D (`BotScript`) existing first.

**Files touched:**
- `BOT_TOOLING.md` (new)
- `UPDATE_LOG.md`

**Status:** done. No source code changed - planning only. Next: build the roadmap data models (A-D) before starting tool stages T1-T3.

## 2026-10-09 - Scalability roadmap for bots

**What changed:**
- Added `BOT_ROADMAP.md`: how slice 1 grows into a scalable, easy-to-author bot system, without changing the slice-1 spec in `BOT_PLAN.md`.
- Framed scalability as three axes (authoring, composition, world knowledge) and defined the target abstractions: `Agent`, `Locator`/`Locations`, a standard leaf/decorator kit, `BotScript` + registry + `Data/CFG/bots.cfg` definitions, per-bot tracing, and tick-budgeted scheduling.
- Phased plan A-I (slice 1 through RL) with per-phase acceptance criteria; each phase additive except the slice-1 work itself.
- Recorded two decisions: the roadmap lives in its own file; the player/NPC `Agent` abstraction is deferred to Phase G.
- Flagged 4 cheap slice-1 seams (state `name()`, `BotContext` as interface, `FixedLocator`, `BotManager` keyed by `BotProfile`) that avoid later rework.

**Files touched:**
- `BOT_ROADMAP.md` (new)
- `UPDATE_LOG.md`

**Status:** done. No source code changed - planning only. Next: begin slice 1, adopting the 4 recommended seams, when approved.

## 2026-10-09 - Slice 1 bot design (chop trees, bank logs)

**What changed:**
- Added `BOT_PLAN.md`: read-only review findings plus the slice-1 design for a state-machine bot (walk to tree, chop, bank logs, repeat).
- Design uses a `BotState.tick` returning `BotStatus` (RUNNING/SUCCESS/FAILURE) with `Sequence`/`Repeat` composites owning all transitions.
- Object interaction goes through the `ObjectHandler` registry via a single `BotContext.interactObject` helper; no direct skill calls.
- Covered cross-cutting requirements: sessionless `flushOutStream` guard (resets `outStream.currentOffset`), reserved bot name prefix with full saving exclusion, and a `MAX_BOTS` cap.
- Work broken into 7 steps, each with a named JUnit 5 test, plus a later-phase note that RL needs wall-clock timers converted to tick counts first.

**Files touched:**
- `BOT_PLAN.md` (new)
- `UPDATE_LOG.md` (new)

**Status:** done. No source code changed - design/plan only. Next: implement step 1 (sessionless `flushOutStream` guard) when approved.
