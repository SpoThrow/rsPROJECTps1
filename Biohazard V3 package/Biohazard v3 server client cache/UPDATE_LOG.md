# Update Log

## 2026-10-10 - Bot roadmap F: a per-bot trace buffer, ::botinfo, and one-line failure logs

**What changed:**
- **`BotTrace` is the per-bot window into a running tree** — a fixed 64-event ring of `(tick, kind, name, ticksInState, note)` for `ENTER`/`SUCCESS`/`FAILURE`/`ABORT`. Only *transitions* are recorded, never a running tick, so a leaf that walks for two hundred ticks costs two events rather than two hundred. The live path is **derived** from the enters not yet matched by an outcome instead of being passed in by each node, which means a node cannot get its own depth wrong. The last failure is remembered even after the ring has overwritten it.
- **`Traced`: tracing by wrapping, not by editing.** A tree's transitions are known exactly where a child is entered and exited — fifteen-odd call sites across `Sequence`, `Selector`, `Repeat`, the decorators and the controller — and adding a reporting call at each would be fifteen chances to miss one plus tracing inside the composition vocabulary. Instead `Traced` is a transparent `BotState` wrapper (`name()` delegates, `tick` returns exactly what its child returned) that reports on the node's behalf, and `ScriptBuilder` wraps each node where the tree is assembled bottom-up. **Not one composite changed.**
- **`BotContext.trace()`** is the per-bot seam (`PlayerBotContext` owns the trace, `FakeBotContext` gets one too), and a leaf that knows *why* it gave up calls `trace().note(...)` before returning — so `Gather` and `WalkToNearest` now explain themselves ("no tree within 8 tiles of 3200,3200", "tried 8 tree that yielded nothing") and the note rides along with the failure event.
- **`::botinfo <account>`** (and the alias `::bot info <account>`) prints the script and its tick, the current path, the last failure and the last ten transitions — wired into the existing `CommandHandler` as its own entry and gated to owners like the rest of the family.
- **A throttled one-line failure log** from `BotController`: `[bot] botoak: t=41 Gather(tree) -> FAILURE (after 41t, no tree within 8 tiles)`. The controller restarts a failed root on the very next tick, so it logs on each *change* of failure signature rather than every 600 ms, and reports how many repeats it swallowed when something different finally happens. Nothing is hidden, only summarised.

**Two details worth recording:**
- **`@RuntimeOnly` exists so the workshop's parity guard could stay absolute.** `Traced` is a `BotState`, so the T4 check ("the set of compiled `BotState`s and the set of palette nodes are the same set") would have demanded a palette entry for a wrapper no author places. Loosening the check is how a real unannotated node slips through, so the exception is declared on the class with its reason and `NodeCoverage` reads it. Both directions are pinned: anything unmarked must be annotated *and* registered; anything marked must *not* be registered, so the marker can never hide a real node. The exporter prints the marked classes, so the exception is visible in the build rather than implied.
- **The wrap point is the builder, and that is an honest boundary.** Nodes a terminal creates *inside* `Root.make` (the `Sequence` a `Repeat` wraps) do not exist when the step loop runs, so each terminal wraps its own composite too. A tree assembled by hand rather than by `ScriptBuilder` is therefore **not** traced: there is no generic child accessor to walk it. The builder is the sanctioned authoring surface, so every scripted bot is traced end to end; adding `children()` for full-tree coverage is a clean follow-up if a hand-built tree ever needs it.

**Files touched:** `Proxy Server/src/server/game/bots/` (new `BotTrace`, `Traced`; `BotContext.trace()`, `PlayerBotContext` clock, `BotController` reporting + `trace()`, `states/Gather`, `states/WalkToNearest`), `bots/script/ScriptBuilder.java` (wrap steps and terminals), `bots/meta/RuntimeOnly.java` (new), `players/packets/commands/BotCommands.java` (+ `info`), `workshop/src/botworkshop/` (`NodeCoverage`, `ExportBotNodes`), `test/server/game/bots/` (new `BotTraceTest`, `TracedTest`, `BotControllerTraceTest`, `script/ScriptTraceTest`; `FakeBotContext`), `test/.../commands/` (`BotCommandsTest`, `CommandHandlerTest` counts), `workshop/test/.../BotNodeParityTest.java`, `Data/workshop/bot-nodes.json` (re-exported), `BOT_ROADMAP.md`, `BOT_TOOLING.md`.

**Status:** done. **729 tests, 0 failures** (612 server + 117 workshop); `workshopValidate` green (42,001 objects, 10 authored locations). Roadmap A–F complete. Next: **G (the `Agent` abstraction)** — migrate `WorldAdventurer` onto the shared tree so one behaviour library drives players and NPCs, which needs no new tracing work because `Traced` reports through the context. The one follow-up F leaves open: **bot accounts still start with no kit** (unchanged from E), so a spawned woodcutter has no axe until `BotProvisioning` (`BOT_ACCOUNTS.md` §4.1) lands.

## 2026-10-09 - Bot roadmap E: bots.cfg, data-driven spawn, and ::bot commands

**What changed:**
- **`Data/cfg/bots.cfg` is now the definition of a bot.** `BotProfile` is one row (`account`, `password`, `script`, `home`, `enabled`); `BotsConfig` reads the file in the `LocationsConfig` style — a missing file is "no bots configured" (empty, no problems, the server boots exactly as before), a bad line is reported and skipped so one typo does not cost the other rows. `enabled` defaults to true. The account must be *login-legal* (`[a-z0-9 ]`, ≤12 chars): a possessed bot is a real account a human can log into, so the roadmap's sketch names (`oak_chop`, `yew_north`) were corrected in the docs — the login decoder would refuse them.
- **`BotManager` gained the config→live path.** `start()` reads the file and spawns every enabled row (one call, beside `WorldAdventurer.spawn()` in `Server.main`); `reload()` re-reads and moves the live set to match (spawn newly enabled, release removed/disabled); `spawn(profile)` creates the account if it is not there yet, then possesses it through the ordinary login path and attaches the script; `apply(Result)` is the shared, testable seam. Spawn is idempotent, and an unknown script is refused *before* any account is created.
- **`::bot` command family** (owner-only, `BotCommands`): `list`, `spawn <account>`, `despawn <account>`, `reload`. Registered as **one** `where` entry rather than five literals, the same shape `ban `/`kick ` use to avoid swallowing longer words.
- **`Data/cfg/bots.cfg` template shipped** with every line commented out, so a fresh checkout spawns nothing but the format is documented in place.

**Three details worth recording:**
- **CommandHandler is hand-edited, but the generated `*Commands` files are not.** `OwnerCommands` says "do not edit by hand", so `::bot` lives in its own `BotCommands` and is registered with one added line in `CommandHandler`. `CommandHandlerTest` pinned the registry at 61 commands and 21 composites; those counts are now 62 and 22, with the reason in the test.
- **Creating an account grants no kit.** A config line for an account that does not exist yet is created and possessed, but it starts with nothing — what a fresh account of a given kind should *own* is `BotProfiles`/`BotProvisioning` (`BOT_ACCOUNTS.md` §4.1), which is not this phase. So a spawned woodcutter has no axe until it is provisioned. That is the next thing to wire, and it is called out in the spawn javadoc.
- **`home` is resolved at spawn, not at parse.** Reading `bots.cfg` therefore touches no world (important: it happens at boot), and a home that no longer exists is a console message rather than a reason the whole file fails to load. `home` resolving to a real place is the one line of this phase not covered by a unit test — it would load the world into a test JVM; the grace path (unknown home still spawns) is covered.

**Files touched:** `Proxy Server/src/server/game/bots/` (new `BotProfile`, `BotsConfig`; `BotManager` new `start/reload/apply/spawn/despawn/profiles/profileFor/applyHome`), `src/server/game/players/packets/commands/` (new `BotCommands`, `CommandHandler` registration), `src/server/Server.java` (one call), `Data/cfg/bots.cfg` (new template), `test/server/game/bots/` (new `BotsConfigTest`, `BotProfileSpawnTest`), `test/.../commands/` (new `BotCommandsTest`, updated `CommandHandlerTest` counts), `BOT_ROADMAP.md`, `BOT_TOOLING.md`.

**Status:** done. **693 tests, 0 failures** (578 server + 115 workshop); `workshopValidate` green (42,001 objects, 10 authored locations). Roadmap A–E complete. Next: **F (observability)** — `::botinfo`, a per-bot trace buffer and failure reasons, which now has real config-spawned bots to inspect and a `::bot` family to extend.

## 2026-10-09 - Bot roadmap D: scripts, the fluent builder, and a generic gather leaf

**What changed:**
- **`BotScript` + `ScriptBuilder` + `BotScripts`.** A script is a *named* tree (`name()` + `root()`), and `BotScript.named("...")` reads down the chain: `walkToNearest(...)`, `gather(...)`, `bankAll(...)`, then a terminal (`forever()`/`times(n)`/`once()`). `gatherLoop(resource, item, service)` is the whole four-step cycle in one call. `BotScripts` is the name→script registry `bots.cfg` (Phase E) will read, with a built-in `gather_oak` so there is something to spawn before a config file exists.
- **Two generic leaves, no per-resource state.** `WalkToNearest(kind, range, seed)` resolves the nearest place of a `LocationKind` through the new `Locations.forKind` and spreads within its box (`RandomTileIn`) — points are taken as-is. `Gather(kind, itemId, range, radius, click)` resolves a real world object through the new `ResourceScan`, walks to it, clicks it, and treats "a slot filled" as progress. `walkToNearest(TREE, 3)` + `gather(TREE, LOGS)` is a woodcutter; the same two lines with `ROCK` are a miner.
- **`ResourceScan` + `ObjectTarget`.** `ScannedLocator` answers "nearest *place*" and deliberately drops the object; an interaction needs the id and tile. `ResourceScan` asks the same two injected seams `ScannedLocator` uses (`RegionSource`/`KindSource`), classifies through the same `ResourceKinds` table, and keeps the object. Bounded to a radius (8 tiles = at most 2x2 regions) and called on retarget, never per tick.
- **Palette.** `ParamType.KIND` (so the editor has a field for a kind rather than free text) and the two nodes registered; `bot-nodes.json` now exports **23 nodes**.

**Three details worth recording:**
- **D's "no core file changed" criterion holds** — `BotController`, `BotPlayer`, `BotManager`, `BotContext` and `PlayerBotContext` are untouched. That is a consequence of `root()` taking no `BotContext`, which is a deliberate deviation from the roadmap sketch in §5.5 (recorded there): resolution moved to state *entry*, so a bot that banked and returned re-resolves "nearest" against where it actually is, and building a script reads no world.
- **Progress is measured in slots filled, not by a skill's session flag.** `ctx.isIdle()` and `ChopTree`'s `woodcutting.active` are woodcutting-shaped by name, so a generic leaf reading them would be lying about mining and fishing. A slot filling is true for every gathering skill — but a *stackable* yield (coins, feathers) would show no progress and be given up on. Every gathering skill in this cache drops a slot-filling item, so this is a documented limitation rather than a live bug.
- **The give-up budget resets when a slot fills.** Otherwise a normal retarget after a tree falls would count towards "there is no resource here" and abandon a good tree field after a few logs. `MAX_TARGETS` now means distinct objects tried since the last gain.

**Files touched:** `Proxy Server/src/server/game/bots/` (new `script/{BotScript,BotScripts,ScriptBuilder}`, new `states/{WalkToNearest,Gather}`, new `world/{ObjectTarget,ResourceScan}`, `world/Locations` (`forKind`), `meta/{ParamType,BotNodeRegistry}`), `test/server/game/bots/` (new `FakeBotContext` shared double, `WalkToNearestStateTest`, `GatherStateTest`, `ScriptLoopTest`, `world/ResourceScanTest`, `script/{ScriptBuilderTest,BotScriptsTest}`; `TreeKitTest` now uses the shared fake), `BOT_ROADMAP.md`, `BOT_TOOLING.md`. (`Data/workshop/bot-nodes.json` was regenerated to 23 nodes; that folder is gitignored, so it is a local build output.)

**Status:** done. **669 tests, 0 failures** (554 server + 115 workshop), including `ScriptLoopTest` — Phase D's acceptance criterion, which declares a chopping-and-banking bot in one `BotScript` block with no new state class and runs it through the real tick loop. `workshopValidate` green. Roadmap A–D complete; **T5 (timeline → `BotScript` JSON) is now unblocked** and Phase E (`bots.cfg`) has a registry to point at.

## 2026-10-09 - Bot roadmap B: the tree kit

**What changed:**
- **Composition.** `Selector` reports the first child that succeeds (the "else if" of a tree), `RandomSelector` does the same in a shuffled order so equally valid choices vary, and `Parallel` ticks every child on every tick and succeeds when all of them have. A parallel that gives up interrupts whichever siblings are still mid-run — otherwise they would stay inside `enter`/`exit`, which is the one rule slice 1 established that a multi-child composite can break.
- **Decorators.** `Retry(child, attempts)`, `Timeout(child, ticks)` (turns "stuck" into FAILURE generically, so a leaf no longer has to invent a tick budget), `Delay(ticks)`, `Cooldown(child, ticks)`, and the adapters `Invert`/`Succeed`/`Fail`.
- **Conditions.** `HasItem`, `InventoryFull`, `WithinRange`, `SkillAtLeast`, `BankOpen`, `IsDead` — stateless one-tick leaves, so a `Selector` can walk a run of alternatives inside a single tick. That is what makes `Selector(not-full -> work, bank)` readable in the tree instead of wired into a leaf.
- **Palette.** All 16 are `@BotNode`-annotated and registered; `bot-nodes.json` now exports **21 nodes** in four groups (`state`, `condition`, `composite`, `decorator`).

**Three details worth recording:**
- **B's "no core file changed" criterion still holds.** Nothing in `BotPlayer`, `BotManager`, `BotContext`, `PlayerBotContext`, `Sequence` or `Repeat` changed; the three conditions that read player state use the existing `ctx.client()` escape hatch, exactly as `ChopTree` reads its session flag. When the `Agent` seam lands (Phase G) those three should move behind a `BotContext` observation, because "is the bank open" is not a player-only question.
- **`ctx.random(bound)` is inclusive.** `PlayerBotContext.random` delegates to `Misc.random`, which returns `0..bound` *inclusive* despite the parameter's name. `RandomSelector`'s Fisher-Yates is correct either way (it wants a uniform `0..i`, and `0..i-1` is also a valid shuffle), but it is a trap for the next caller. It still has no other callers, so documenting or narrowing it is a cheap follow-up.
- **`Cooldown` is a named composition, not a second wait loop.** Without a game clock a cooldown can only be a pause counted in the ticks the node is ticked, which is exactly `Sequence(child, Delay(n))`; it delegates to that so there is one implementation, and exists so the palette and the trace say "cooldown".

**Files touched:** `Proxy Server/src/server/game/bots/` (`composite/{Selector,RandomSelector,Parallel}`, new `decorator/` and `condition/`, `meta/BotNodeRegistry`), `test/server/game/bots/` (new `TreeKitTest`, `ConditionStatesTest`; extended `meta/BotNodeRegistryTest`).

**Status:** done. **627 tests, 0 failures** (512 server + 115 workshop), including both directions of `BotNodeParityTest`; palette re-exported. Roadmaps A, B and C are now complete, so T5b's blocker (Selector/Parallel in the runtime) is cleared — but T5 still needs roadmap **D** (`BotScript`, the fluent builder, and a generic `GatherLoop`), which is next.

## 2026-10-09 - Bot Workshop T3 + T4, and a continuous world map

**What changed:**
- **T4 (node registry).** `@BotNode`/`@Param` annotate the runtime states and composites; `BotNodeRegistry` reflects the *compiled* classes (including real parameter names, via the compiler's `-parameters`); `gradlew workshopExportNodes` writes `Data/workshop/bot-nodes.json`; the server serves it at `/nodes.json` and the viewer reads a palette from it. `BotNodeParityTest` fails if either side gains an unregistered node, checks the export against the registry in both directions, and proves no compiler-generated parameter name (`arg0`) ever leaks into the schema.
- **T3 (authoring).** The viewer has a box tool: drag a rectangle, name it, pick a kind, and the row is sent to the workshop server (`POST /locations/check`), parsed with the server's own `LocationsConfig`, canonicalised with `toRow`, and only then appended (`POST /locations/append`) as a dated commented block. Append-only: the editor cannot rewrite or delete an authored row, so its mistakes are additive and visible in a diff.
- **The editor warns before it writes.** `countDraftObjects` counts objects of the drafted kind inside the box from the regions the viewer has loaded, and reports the count, or that the box is empty — the failure `workshopValidate` otherwise reports only after the file has changed. It is a warning, not the check: only loaded regions are counted, and the count says so. `locations.json` gained `objectKinds` (from `LocationKind.isObjectKind()`) so a kind with no objects behind it (shop, teleport, monster, master) is never reported as an empty box.
- **The map is continuous.** `world.json` carries every region in `map_index` (1226), not the exported subset, so the overview can draw the whole world; below 1.5 px/tile it shades regions by object density with a bank dot, and above it the viewer lazily fetches, renders and bounds the regions the camera reaches (64 in cache). `workshopExport` now defaults to a contiguous 4x3 block around Lumbridge/Draynor (`-PworkshopArea=3072,3136,4,3`) so multi-region panning has something to pan across.
- **Two names corrected to say what they mean.** `world.json`'s per-region `hasData` is now `exported`, and the summary `withMapData` is now `exported`: the flag means "this run wrote a document for it", which is a different claim from "the world has terrain there" (51 regions have no map files at all, and `Region.load` skips them).
- **Fixed: "Frame region" was a no-op from the world overview.** It framed the region under the camera and fell through to whole-world framing when the camera sat in a gap — which is most of the world at overview zoom, so the button looked broken. It now falls back to the region picked in the navigator.

**Files touched:** `Proxy Server/src/server/game/bots/{meta/*,states/*,composite/*,world/LocationsConfig}.java`, `workshop/src/botworkshop/{ExportMap,ValidateMap,ExportBotNodes,meta/*,export/{BotNodes,NodeCoverage,WorldDoc,LocationsDoc},serve/WorkshopServer}.java`, `workshop/web/js/{app,view,world,region}.js`, `workshop/test/...` (new `BotNodeParityTest`, `WorldDocTest`, `BotNodesTest`, node/locations cases), `build.gradle`.

**Status:** done. **591 tests, 0 failures** (476 server + 115 workshop); `workshopValidate` green (0 mismatched tiles, loc.dat agrees with the server on all 42001 entries, and all 10 seed rows contain the objects they claim); verified live in the browser — the overview, lazy paging, the T3 drag, `/locations/check` rejection, and a real append (then restored and re-exported). Next: T5 (step timeline → `BotScript` JSON).

## 2026-10-09 - Bot roadmap C: the `Locations` world-model

**What changed:** Bots can now name a *place* instead of a coordinate. New `server.game.bots.world` package: `Location` (a named point or box), `LocationKind`, `Locator`/`CuratedLocator`/`ScannedLocator`/`MergingLocator`, the `Locations` facade with families (`resources`, `services`, `banks`, `trees`, `rocks`, `fishing`, `shops`, `teleports`, `monsters`, `masters`), and `Waypoint`/`FixedPoint`/`RandomTileIn`. `locations.banks().nearest(x, y, 0, 1)` answers from the authored table when it can and from a bounded, cached world scan when it cannot.

**One table, two consumers.** `ResourceKinds` (in main) is now the single action→kind rule set. The workshop's `ResourceRules` was refactored to delegate to it, so the map's icon layer and the runtime scanner cannot drift. A new workshop test compares both *decoders* end to end over all 42001 `loc.dat` entries and asserts they classify identically — the delegation is evidence-checked, not assumed.

**Derived, not duplicated.** `locations.cfg` holds only what no existing file can answer: human names and tags for resource regions. Teleports are joined from `teleports.cfg`, shops from `npc-shops.cfg` + `shops.cfg` + `spawn-config.cfg`, monsters from `spawn-config.cfg` + `npc.cfg`. Each join is optional and a missing file is not an error — deleting `locations.cfg` degrades to scanned lookups rather than failing, which is the documented acceptance criterion and is tested.

**Seed data is measured, not invented.** `Data/cfg/bots/locations.cfg` ships 10 rows whose boxes are the exact extents of clusters found in the real world. `gradlew workshopValidate` now re-derives those extents and fails if any row's box contains no matching object; it reports 9/4/6 bank booths, the Lumbridge altar, and the oak/tree/willow boxes with 23/20/19/6/3/1 objects, all as expected.

**Three details worth recording:**
- **`nearest` is plane-strict.** Lumbridge's castle bank is at 3207,3221 on plane 2 — the same coordinates on plane 0 are a wall. A cross-plane answer is not a shorter journey, so a locator never returns one; travel between planes is a caller's `Sequence` of legs.
- **The scanned locator is bounded, and says so.** It searches 2 rings (5x5 regions) and its answer is "nearest within that box", not "nearest in the world". Its early exit is a *distance bound*, not a candidate count: stopping as soon as `limit` results existed was wrong, because the query's own region usually holds something and the nearest object is often next door.
- **`RandomTileIn` is seeded and deterministic.** Same seed, same tile — so a stuck bot can be replayed from its trace. Different seeds spread out, so two bots on one waypoint do not stack. The fallback (box centre, then a spiral outward) is seed-independent on purpose, so a failure cannot look like a success for some seeds.

**Files touched:** `Proxy Server/src/server/game/bots/world/` (new, 13 files), `test/server/game/bots/world/` (new, 7 test classes), `Data/cfg/bots/locations.cfg` (new), `workshop/src/botworkshop/{classify/ResourceRules,ValidateMap}.java`, `workshop/test/botworkshop/classify/ResourceKindsParityTest.java` (new), `build.gradle` (test task now sets `workshopDataRoot`).

**Status:** done. **556 tests, 0 failures**, plus `workshopValidate` green. Roadmap C is complete; T3 (authoring into `locations.cfg` from the editor) now has the runtime it needs. Next: **T4** — the `@BotNode` registry and `bot-nodes.json` export.

## 2026-10-09 - Bot Workshop T2 (+T2b): the web map viewer

**What changed:** The Bot Workshop now renders the exported world in a browser. `gradlew workshopServe` starts a dependency-free JDK `HttpServer` on `http://127.0.0.1:8080` that serves a canvas map, the exported region JSON at `/map/*`, and a `/palette.json` floor-colour table. The viewer pans/zooms (anchored at the cursor), switches planes 0-3, and has layer toggles for tiles, icons, clipping, footprints, tile grid and region bounds. Clicking a tile fills the inspector with its coordinates, walkability, decoded clip bits, floor ids, region/local position, the objects covering it with their definition actions, and the distance to the nearest bank. Hovering shows the same summary as a tooltip.

**T2b came along with it:** `ResourceRules` classifies each object's `actions` into a resource kind (tree, rock, fish, bank, cook, smith, pray), the map draws those as icons, and the resource panel lists all 54 kinds found with counts and isolates one on click. Anything unclassified is left undrawn on purpose — a wrong icon is worse than no icon.

**Three things worth recording:**
- **The palette is generated, not looked up.** The cache carries no floor colour table, so `FloorPalette` derives one from the floor id, confined to a 22°-187° hue band at low saturation so it reads as terrain rather than neon. Overridable per id in `Data/cfg/floor-palette.cfg`.
- **Collision is byte-exact, not re-derived.** `RegionDocument` exports `Region.getClipping` per tile per plane, so the clipping overlay shows the server's own numbers. `ValidateMap` fails on any disagreement.
- **The bank line now reports two facts, not one.** It originally ranked same-plane banks absolutely, which meant a booth 87 tiles away beat the Lumbridge castle bank 21 tiles away and one plane up. There is no honest way to collapse "tiles" and "a plane change" into one number, so it prints the nearest bank on your plane and, when that differs, the nearest on any plane with a plane-change warning.

`banks.json` scans all 1226 regions rather than the exported subset, so nearest-bank distances are correct across region boundaries. The exports themselves stay out of git (`Data/workshop/` is ignored) since they are regenerated by `workshopExport`.

**Files touched:** `Proxy Server/workshop/web/` (new: `index.html`, `styles.css`, `js/{app,view,region,rle,clip,palette}.js`), `workshop/src/botworkshop/serve/` (new: `WorkshopServer`, `FloorPalette`), `workshop/src/botworkshop/export/{BankIndex,RegionDocument}.java`, `workshop/{src/botworkshop/{ExportMap,ValidateMap}.java,test/...}`, `build.gradle`.

**Status:** done. **474 tests, 0 failures** (385 server + 89 workshop); verified live in the browser against region 12850 — the Lumbridge tree at 3217,3241 inspects as expected. Confirmed the server jar still contains zero `botworkshop` classes, so the tool remains deletable. Next: **T3** — region/patch authoring into `locations.cfg` and drag-a-box `RandomTileIn` waypoints; needs roadmap C (`Locations`) to exist first.

## 2026-10-09 - Recorded (not yet measured): a second server/client divergence in hasActions

**What changed:** No code. Recording a difference noticed while comparing the two `loc.dat` readers, so it is not lost.

The client's `readValues474` gates its model-based `hasActions` fallback on the object having a name:

```java
if (flag == -1 && name != "null" && name != null) { ... }
```

while the server's `readValues` has no such guard:

```java
if (flag == -1) { ... }
```

So for an object with no name, no actions and no opcode-19 flag, the **server** can set `hasActions` true from the model clause where the **client** leaves it false. `hasActions` only reaches collision through the type-22 branch — `if (type == 22) { if (def.hasActions() && blocksWalk) addClipping(x, y, height, 0x200000); }` — so the visible effect would be a small set of tiles the server marks occupied that the client does not.

**Not measured, and deliberately not fixed.** Quantifying it needs a decoder that retains the opcode-19 flag and the model list, because `hasActions` true is ambiguous between "opcode 19 set flag to 1" (both sides agree) and "flag was -1 and the model list matched" (the two sides can differ). It is also orthogonal to the terminator fix: these are objects with no strings, so their parse never threw and their behaviour is unchanged by that work. Before touching it, note the server's `hasActions` also feeds object interaction, not just collision, so a change needs its own verification rather than riding along with this one.

**Files touched:** `UPDATE_LOG.md`

**Status:** blocked on measurement — needs the decoder field above before the count can be stated, so it is recorded as a difference and not as a number.

## 2026-10-09 - Correction: the ObjectDef collision impact was described backwards

**What changed:** No production code — this corrects the entry below, which reported that 77 placements "became walkable" and 230 "became blocked" after the `ObjectDef` fix. Both numbers came from testing `clip == 0`, which is not the walkability test. The clip is a bitmask, and `0x100` is the walk-block bit (`Region.addClippingForSolidObject` sets `clipping = 256`), while `0x20000` is projectile-solid and `0x200000` is terrain/type-22 occupancy. A tile with clip `8` or `128` is still walkable; a tile with `0x20000` is walkable for people but not for projectiles.

**Measured properly, across the same 307 changed object-origin tiles:**
- walk-block `0x100` — **lost on 217**, gained on 61
- projectile `0x20000` — lost 161, gained 57
- occupancy `0x200000` — **gained 19, lost 0** (never lost, as expected: terrain keeps it)
- by region — Draynor 62 walkable / 0 blocked, Lumbridge 152 / 5, Varrock 3 / 56

So the change runs **towards more walkability**, not more blocking — the direction that matches the client, since these are decorations the client lets you walk over. The cleanest example is "Stones": `131328 -> 0` (that is `0x20100 -> 0`) around Draynor and Lumbridge, 214 tiles in those two regions released in total.

**The cause was also wrong, in two parts.** The earlier entry attributed the blocking direction to type-22 objects registering `hasActions()`. That explains the 19 gained `0x200000` bits, not the 61 gained `0x100` bits. The latter is consistent instead with **footprint growth**: `ObjectDef.setDefaults()` pins `anInt744`/`anInt761` to 1, so a definition that failed to parse blocked only its origin tile, while the same definition now blocks its true area — `addClippingForSolidObject` loops over the whole footprint. Worth recording because it is the part that is easy to misread: **a changed tile is not necessarily the changed object's own doing.** Clip is a per-tile union, and objects with no strings at all — e.g. 324/325/326, whose bytes are `15 28 03 13 1c 1b 90 17 21 17 16 13 23 1b 90 01 01 04 64 16 00` — moved only because of neighbouring objects. The bit counts above are measured; the two mechanisms are inferred from the code and labelled as such.

**Files touched:** `UPDATE_LOG.md`

**Status:** done — record corrected, no code change. The 307 targets are listed in `Proxy Server/build/workshop-clip-changes.txt` (build output, gitignored) for the live check.

## 2026-10-09 - Fixed the ObjectDef reader: the server had no object names, actions or walkability

**What changed:**
- **The terminator fix.** `ObjectDef.readValues` read `loc.dat` strings with `readString()`, which scans for `0x0A`, while the file terminates them with `0x00`. Every named entry failed to parse, and `getObjectDef`'s `catch` hid it behind `setDefaults()`. Name (opcode 2) and actions (opcodes 30-38) now use `readNewString()`, and description (opcode 3) uses a new `ByteStreamExt.readNewBytes()`. `readValues377` and the existing `readBytes()` are deliberately untouched — the `0x0A` reader is still correct for the streams that encode strings that way.
- **Impact, measured on both sides of the change.** Before: **19410 of 19410** named objects returned no name, no actions and `aBoolean767 = true`. After: name, ordered actions, footprint and walk-blocking agree with the tool's decoder for **all 42001 entries, 0 differences**. The client never had this bug — its `forID` calls `readValues474`, which reads `0x00` — so the fix closes a server/client disagreement about the name, actions and walkability of every named object rather than just tidying a parser.
- **Collision change, quantified.** Re-exporting the three landmark regions and diffing the per-object `clip` values: **307 of 13860 placements changed** — 77 became walkable, 230 became blocked. The blocked direction is type-22 floors whose real actions now register through `hasActions()`, which is what the client already did. `workshopValidate` still reports **0 mismatched tiles**: terrain and objects share the `0x200000` bit, so a terrain-occupied tile keeps it whatever the objects do.
- **A bug in the tool, found by this work.** `LocDefs` treated opcode 74 as an inert flag, but both the client's `readValues474` and the server finish with `if (aBoolean766) aBoolean767 = false`, so 74's objects are walkable. The client's reader was the reference that exposed it.
- **New guard.** `ObjectDefParityTest` (5 tests) compares the server's reader against the decoder across all 42001 entries on name, actions, footprint and walk-blocking, and opens with a liveness check because a failed `loadConfig` would otherwise make every parity assertion pass vacuously. `ValidateMap` now **fails** on disagreement instead of only reporting it, and its `0x200000` comment records that terrain and objects share that bit and the check is therefore one-directional.

**Files touched:**
- `Proxy Server/src/server/clip/region/ObjectDef.java`, `ByteStreamExt.java`
- `Proxy Server/workshop/src/botworkshop/ValidateMap.java`, `workshop/src/botworkshop/data/LocDefs.java`
- `Proxy Server/workshop/test/botworkshop/ObjectDefParityTest.java` (new)

**Status:** done. **452 tests, 0 failures** (385 server + 67 workshop). ⚠️ Not yet verified live: this moves walkability for 307 placements across three regions, so a live session is worth running before trusting pathing around the affected objects.

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
