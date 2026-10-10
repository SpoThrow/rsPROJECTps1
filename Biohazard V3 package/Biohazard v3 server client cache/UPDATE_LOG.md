# Update Log

## 2026-10-10 - QOL Phase 0: item-use registries, and item lookups that cannot return null

**What changed:**
- **New `ItemUseRegistry` and `ItemOnObjectRegistry`** (`server.game.players.actions.items`), modelled on the existing `ObjectHandler`/`ButtonHandler`. `UseItem.ItemonItem` and `UseItem.ItemonObject` consult them after their predicate guards (poison, dwarf cannon) and before their inline checks/switch, so a recipe family moves out one at a time with the two paths never both running. Item-on-item pairs key order-independently, because the legacy checks are written `a && b || b && a`; item-on-object pairs key on the pair, so one item can migrate without dragging a whole object out of the switch. A duplicate registration throws.
- **`ItemOnObjectRegistry` refuses the nine cooking object ids** (`12269, 2732, 114, 9374, 2728, 25465, 11404-11406`). `ItemOnObject.processPacket` runs its own cooking switch *after* calling `UseItem`, so claiming one would leave two handlers running for the same click. Enforced at registration and pinned by a test rather than left as a comment.
- **New `ItemDefinitions` — the §2 safety contract.** `Item.getItemName(int)` returns `null` for an id with no definition, and this codebase decides a lot from item *names* (`ItemHandler.getRequirements` is a long chain of `itemName.contains("bronze")`), so an id we reference before we own its definition is an NPE waiting for the wrong caller, not a missing feature. `ItemDefinitions.name/get/value/exists` answer with an `"Unknown item"` sentinel instead — Necrotic's `ItemDefinition.forId` never-returns-null contract — so a future higher-revision id is inert, not fatal. The two legacy accessors are deliberately untouched (hundreds of call sites may rely on their present behaviour). O(1) array read with the linear-scan fallback the old accessors always did, and safe to call before `Server.itemHandler` exists.
- **`Config` flag block for the programme:** fletching one-by-one on; random events on with bird nest and genie on and the intrusive classic events off; world events, fillables, pickables, guilds, bank PIN, teleport hub and sounds off until their phase — the defaults are the ask, so flipping one is never a code change.

**Files touched:** added `server/game/players/actions/items/` (4 classes) and `server/game/items/ItemDefinitions.java`; edited `server/game/items/UseItem.java` (two hooks) and `server/Config.java`; added 3 test classes; updated `QOL_PLAN.md`, `UPDATE_LOG.md`.

**Status:** done. **907 tests, 0 failures, 0 errors** (751 server + 156 workshop; 16 new). Next: the Phase 0 content validator (warn on an id with no definition, fail on a registry/legacy collision), then Phase 1 — fletching one-by-one with the `15 * amount2` shaft fix.

## 2026-10-10 - QOL_PLAN.md rewritten with Necrotic local: it is the primary source, and item ids get a safety contract

**What changed:**
- **Supersedes the earlier `QOL_PLAN.md` entry below.** The user downloaded Necrotic locally (`F:\Download Archive\Necrotic-Server-1.1.1`), so the previous "Necrotic is data-only, inspiration only" finding is now wrong — it is 512 Java files of real source (the 'Ruse' base, Java 8, 600 ms tick — the same tick as ours) and it is now the **primary** rip source, with 2006Redone demoted to secondary where it genuinely wins.
- **New §2 is now the centre of the plan: forward-compatible item ids.** The requirement is "add an id that does not exist yet, don't break the game, and have it work when the item is added later". Necrotic already does this — `ItemDefinition.forId(int)` **never returns null**, resolving unknown ids to a default (`name = "None"`) — so its skills are inert-not-broken for unregistered ids. **Ours is not safe today: `Item.getItemName(int)` returns `null`** (`Item.java:18`), so any existing name-based logic (`getRequires` is built from `itemName.contains("bronze")`, and there is a lot more) throws NPE the moment a not-yet-defined id reaches it. The plan adds a never-null accessor alongside the existing one (so nothing that works today changes), hardens the name paths, and adds a validator that **warns** for "not defined yet" but **fails** for "two handlers for one id".
- **Teleport hub now has the real design, not a guess.** It is Necrotic's `TeleportInterface` on interface `44000` (the `50100` I saw earlier is a stale whitelist id): categories `Cities/Monsters/Dungeons/Bosses/Minigames/Wilderness`, labels and buttons placed by arithmetic from `PAGEOPTIONS`/`PAGEBUTTONS`, one `TeleportInterfaceData` entry per destination, action buttons teleport `-21443` / exit `-21534` / history `-21446..-21438`, destinations from the ~90-entry `TeleportLocations` enum, and a persisted 4-entry History. The screenshot matches this line for line, including `EDGEVILLEDITCH` and `CHILL` in the History. Split into Stage A (data-driven menus from our existing `teleports.cfg`, no client work) and Stage B (build the interface — those ids are Necrotic's client's, not ours, so it is a build not a copy).
- **Fletching confirmed as the model the user described.** Necrotic's `fletchBow` is a `Task(2)` that deletes one log per action and adds `15` shafts for shafts — so our instant whole-inventory version plus the `15*amount2`-inside-the-`logArray`-loop bug is the thing to fix, not port. Per-skill audit now names the best source per skill (Necrotic for potions/gems/glass/spinning/agility/smith tables, 2006Redone for fillables/pickables/door-gate face math/guild requirements — because Necrotic has **no water filling at all**, only three hardcoded pickable cases, hardcoded doors with the generic path commented out, and no skill-guild system).
- **Overload groundwork written down.** Necrotic's doses are separate item ids with one master `CombiningDoses` table, and extremes/overload chain `15308–15335` (`OVERLOAD = 5 extremes → 15333`) with the effect in `Consumables` plus an `OverloadPotionTask`. Those rows can be written today and stay inert under §2 until the items are imported — which is exactly the future import the user described.
- **New §11 covers the one-at-a-time import itself**: land §2 first, write features against ids now, then each import is a *data* change (`ITEM_LIMIT = 25000` already accommodates Necrotic's ~22,694), with a suggested separate `Data/cfg/item-extra.cfg` so our 1.8 MB `item.cfg` stays untouched and each batch is one revertible file.

**Files touched:** rewrote `QOL_PLAN.md`. Modified `UPDATE_LOG.md`.

**Status:** done. **Documentation only — no source, data or test change**, so the last verified figures stand: 889 tests / 0 failures. Next: the six open decisions in `QOL_PLAN.md` §14, then Phase 0 (`ItemUseRegistry`, `ItemOnObjectRegistry`, the never-null accessor, the warn-not-fail validator).

## 2026-10-10 - QOL_PLAN.md: the skilling / QOL programme, planned before it is written

**What changed:**
- **New `QOL_PLAN.md` at the root.** Writes down the whole QOL list as design only: the non-overriding rules, what is worth taking from each reference and what is not, a per-skill audit against our code, and eight phases that each ship and revert independently behind a `Config` flag.
- **Two references, used differently.** `2006Redone` is a readable 2006 source we can take tables and mechanics from. `Necrotic-Server`'s committed repo is data + IDE files only (no server source), so it is inspiration only — the teleport hub is the idea worth having.
- **Reconnaissance findings that change the work, all verified in our tree:** fletching is instant *and* has a real bug (`addItem(fle.getBowID(), 15*amount2)` inside the `logArray` loop, so one log yields 15 shafts); the Gnome glider is **already complete** with the same six routes as the reference, so it is verify-only; our `Potions.java` already has 73 cases and `PotionMixing` decanting is already wired, so potions are a gap-fill not a port; the bank PIN has a **client button already** (`5294`, "Set a Bank PIN", on interface `5292`) and a **server stub** in `ClickingButtons`, so the hook exists; sound effects already have a working transport both ends (`sendSound` → frame 174 → client case 174) and only the id catalogue is missing; random events have no dispatcher and no flag, and are triggered inline at four skills; craft guilds do not exist; `teleports.cfg` already holds 11 categories, so the hub is a UI problem, not a data one.
- **The one structural gap the plan opens with.** Item use (`UseItem.ItemonObject` / `ItemonItem`) is the only interaction with no registry, so fillables/decanting/new pairings would mean editing a 370-line method — the thing the user asked us to avoid. Phase 0 adds `ItemUseRegistry` + `ItemOnObjectRegistry` modelled on the existing `ObjectHandler`/`NpcActionHandler`/`ButtonHandler`, plus an id-collision audit and a skill-object coverage report (which turns "all working objects around the world" into a number).
- **Five decisions left open for the user**, flagged in the doc: the bank-PIN UI (the reference's keypad is interface `7424`, which our client likely lacks), teleport-hub scope (data-driven menus vs the bespoke two-list interface), bots vs bank PINs, whether farming goes for full persistence, and whether all production skills become OSRS-slow or only fletching.

**Files touched:** new `QOL_PLAN.md`. Modified `UPDATE_LOG.md`.

**Status:** done. **Documentation only — no source, data or test change**, so the last verified figures stand: 889 tests / 0 failures. Next: the user picks the open decisions in §11 and the phase to start on.

## 2026-10-10 - Parked: BOT_PARKED.md, so the bot work can be put down and picked up

**What changed:**
- **New `BOT_PARKED.md` at the root.** The bot system works end to end (roadmap A–H plus provisioning; tooling T1–T7a) and nothing else in the server depends on finishing it, so the remaining work is now written down as a ranked resume point rather than held in a session's head: the commands to bring it back up and the startup lines that say it did, what is actually finished, the four open items in priority order (mixed-species banking first, then T7b, the `NpcAgent` skill path, roadmap I), and the gotchas that otherwise cost an hour.
- **Nothing was removed or changed in behaviour.** The two sample bots and both servers are left as they were; `BOT_PARKED.md` says so, including that the game server on 43594 and the workshop on 8080 may still be running as ordinary processes.
- **The gotchas are recorded because they read as bugs.** The bots look stuck and are not (`Woodcutting.getTimer` is ~3 ticks per plain log, 19–39 for an oak, so a full load is minutes of one unchanging state path); `bob.txt` is rewritten on every boot; `server_run.log`/`workshop_run.log`/`bot_poll.log` are not git-ignored; `jdt-bin/` is tracked in three places with `.bak` churn; a bot occupies a real `PlayerHandler.players[]` slot and counts towards the "Currently online" figure, sharing `MAX_PLAYERS = 50` with up to `MAX_BOTS = 10`; and autosave is on a 5-minute sweep, not on every change.

**Files touched:** new `BOT_PARKED.md`. Modified `UPDATE_LOG.md`.

**Status:** done. Documentation only — no code, config or test change, so the last verified figures stand: 889 tests / 0 failures, `workshopJsTest` 49/49 at `2e5a2eeb`.

## 2026-10-10 - An axe and no level is still a dead bot, and the two sample bots are on

**What changed:**
- **A profile now grants the level its tool is for, not just the tool.** Found by running the thing: a `woodcutter` bot owned its bronze axe, walked to its home trees, and was refused by every one of them — `Woodcutting` puts oaks at 15 and willows at 30 and *every* shipped tree place is one of those, so the axe alone buys nothing. `BotProfiles.Profile` gained `skillIds`/`skillLevels` (`skillCount`, `skillId(i)`, `skillLevel(i)`): `WOODCUTTER` 30 woodcutting, `MINER` 15 mining (iron), `FISHER` 20 fishing (trout), and `DEFAULT` all three, for the same reason it already carried all three tools — a row that names no profile has said nothing about which resource its script wants.
- **The level is written with the XP that reads back as it, or it walks back down.** `Client.process()` drains `playerLevel` a point at a time towards `getLevelForXP(playerXP)`, and the skill tab, total level and level-up message all read the XP, so `BotProvisioning.applySkills` derives the XP from the server's own `getPA().getXPForLevel(level) + 1` (the `+1` because `getLevelForXP` advances only once XP *exceeds* a threshold) instead of writing `playerLevel` alone. The guard skips an out-of-range entry rather than writing past the array; `clear()` still means base levels, via a null profile.
- **Both shipped sample bots are enabled.** `Data/cfg/bots.cfg` now runs `oakchopper` (built-in `gather_oak`) and `oakbanker` (authored `chop_and_bank.json`), both `profile woodcutter home draynor_oaks` — one row per way a script can arrive, so the workshop's Live panel shows a config row and a script file driving the same job.
- **`Config.BOT_STATUS_PORT` is 8081 in the shipped config** (was `0`), so the workshop's live panel works from a plain server start with no extra flags. It is loopback-only and read-only, and `0` still turns it off without a code change.
- **A gap the two bots are about to hit is written down rather than designed around.** A gather loop banks one `itemId` but the world picks the species: at `draynor_oaks` the neighbours are mixed, so a bot whose nearest tree is an oak fills up with oak logs and deposits none. Recorded in `BOT_ACCOUNTS.md` §4.1 with the three ways out (species-aware gather, species-aware deposit, or a homogeneous place), none of which is free.

**Files touched:** `src/server/Config.java` (`BOT_STATUS_PORT` 8081), `src/server/game/bots/{BotProfiles,BotProvisioning,LiveBotsServer}.java`, `test/server/game/bots/BotProvisioningTest.java` (5 new), `Data/cfg/bots.cfg`, `BOT_ACCOUNTS.md` (§4.1 levels, two new ⚠️ findings, the mixed-species gap).

**Status:** done. **889 tests, 0 failures** (733 + 156, 5 of them new in `BotProvisioningTest`); `workshopJsTest` 49/49. Then watched on a real boot rather than assumed: both bots are created with Woodcutting 30 / 13364 XP (exactly `getXPForLevel(30) + 1` — this cache's own curve, not OSRS memory), walk to a tree from the profile tile, chop a full 28-slot inventory (~350 ticks), bank it, and come back for more — no `lastFailure`, no restart. The workshop's Live panel lists both from `/live/bots` at 8081 with no extra flags, and clicking a row centres the map on the bot. Worth knowing before judging them slow: `Woodcutting.getTimer` gives this cache about 3 ticks per plain log and 19–39 for an oak, so a full load is minutes and the state path sits on `Gather(tree)` the whole time.

## 2026-10-10 - T7a: the live bot view — one read-only route, and the bot you cannot see from a chat window

**What changed:**
- **A running bot is now visible from the browser.** `Config.BOT_STATUS_PORT` (default `0`, off) starts a loopback-only endpoint in the game server answering `GET /live/bots` with every live bot's name, script, tile, plane, tree, current state path, last failure and recent transitions — the `::botinfo` facts, which until now needed a logged-in owner and a chat window. `LiveBots` builds the report, `LiveBotsServer` serves it, and `Server.main` gets one no-op-unless-enabled call.
- **Deliberately a view, read from another thread.** A consistent snapshot would mean copying every bot's trace on the tick thread once per tick, which the tick budget exists to prevent. So the report copies through the accessors that already copy (`BotManager.all`, `BotTrace.path/history`), null-checks everything a concurrent mutation could hand it, and is documented as occasionally one tick stale — the honest trade for reading it "now".
- **No write path, and that is a decision rather than an omission.** Possess/release and pause/step are the other half of T7 and are deferred: a bot is a real character, so a route that can start one is a second way to drive bots that bypasses `::bot`'s gating. The endpoint answers and cannot be made to do anything — the worst case is that it says where the bots are standing.
- **The tool proxies, so the browser only ever talks to one origin.** `/live/bots` on the workshop server fetches the game server and wraps it as `{live, status, error}`, passing the report through verbatim (`Json.raw`) rather than re-parsing it. That keeps the game server's headers untouched, and makes "the game server is not running" — the ordinary state, since you do not run a game server to browse a map — an answer the panel renders rather than a network error it has to guess at. Port via `-PbotStatusPort=`, default 8081.
- **The viewer lists the bots and draws them, on their own plane.** A **Live bots** panel (watch = poll once a second, or one-shot refresh) shows each bot's state leaf, position and last failure with the whole path as the tooltip; clicking one switches plane and centres the map on it. Markers are drawn only for the plane being looked at — a bot drawn on another plane would be a marker at a tile it is not standing on — with its name, and a new `live bots` layer toggle.
- **Both halves are checked where they can actually be wrong.** `LiveBotsTest` pins the JSON and its escaping from injected views and then fetches the endpoint over a real socket (200 on `GET`, 405 on `POST`, 404 on anything but the exact path — the context matches by prefix, so that check is load-bearing); `LiveProxyTest` runs the proxy against the real endpoint *and* against a dead port and a non-JSON answer; `live.test.mjs` checks what an envelope means and that a slow answer cannot stack requests up behind it.

**Files touched:** new `src/server/game/bots/{LiveBots,LiveBotsServer}.java`, `test/server/game/bots/LiveBotsTest.java`, `workshop/src/botworkshop/serve/LiveProxy.java`, `workshop/test/botworkshop/serve/LiveProxyTest.java`, `workshop/web/js/live.js`, `workshop/web/test/live.test.mjs`. Modified `src/server/{Config,Server}.java`, `workshop/src/botworkshop/serve/WorkshopServer.java`, `workshop/web/{index.html,styles.css}`, `workshop/web/js/{app,view}.js`, `build.gradle` (`workshopServe` property, `workshopJsTest` now runs both node suites), `BOT_TOOLING.md` (§6, T7a/T7b, §11, acceptance).

**Status:** done. **884 tests, 0 failures**; `workshopJsTest` 49/49 across both suites. Verified in the browser against a live report: three bots listed with their state leaves and last failure, two drawn on plane 0, clicking the third switched to plane 1 and centred on 3098,3245. Remaining in the track: **T7b** (optional possess/step).

## 2026-10-10 - T6b: checking what a script names, without starting a server

**What changed:**
- **The gap T6 recorded as "a manual check" is now a command.** `workshopValidateScripts` proves the server can *load* a document and `workshopValidate` proves the authored boxes contain what they claim, but neither answers "can `gather(rock)` find a rock?" — the schema knows a kind, not the world, and the location table says nothing about a kind an author asked for and never authored a row for. `gradlew workshopResolveScripts` answers it offline.
- **Two oracles, and the cheap one first.** A kind resolves if `LocationsData` yields a place of it (which needs only `Data/cfg`), or if the world contains an object of it (which needs the whole world counted). The world is loaded *only when an answer depends on it* — a kind with no place in the table — so a script naming `tree` and `bank` stays instant and a script naming `rock` pays for the census. `-PworkshopCensus` forces it.
- **The census is whole-world and classified by the runtime.** `WorldCensus` walks every region `map_index` names, accumulating `objectId → count` in one pass and classifying the distinct ids afterwards — classifying inline would re-parse a definition for each of the world's ~1.9 million objects, because `ObjectDef` caches twenty. It classifies with `ResourceKinds`, the same table `ScannedLocator` and `ResourceScan` use, so "the world has 412 trees" means 412 objects a `gather(tree)` leaf would accept.
- **The kinds come off the schema.** `ResolveScripts.wanted` walks the document through `BotNodeRegistry` and reads every `KIND` parameter — the T4 anti-drift rule applied to the check, so a node that gains a kind parameter is covered the day it is annotated. No node id or field name is written down.
- **Only a definite failure fails.** `SCAN_ONLY` (no row, but the world has objects) is reported with the reason rather than as an error, because `Locations` falls through to a scan and `locations.cfg` says as much about rocks and fishing spots. `NOT FOUND` — the table and the world both lack it — exits non-zero.
- **A bug the first real run found, and the rule it forced.** The first version reported `tree` as `NOT FOUND` even though six authored rows name trees: the rule read a census of zero as "the world has none of this" when it only meant "the world was not counted". Fixed by making a table row win outright — and now the rule and the load condition agree exactly, since only a kind with no row ever consults the count. Whether an authored box is *worth* walking to stays `workshopValidate`'s `EMPTY` check; answering it here too would make the two commands disagree.
- **A doc example that would have misled.** BOT_TOOLING §4's graph sketch still used the roadmap-D-era shape (`"type"`, `walkToNearest`, `resource: "tree.oak"`, `region: "bank.draynor"`) — none of which the loader accepts. Corrected to the real format, with a note on why `kind` is the vocabulary, since a typo'd example is how a script comes to name a kind that resolves nowhere.

**Files touched:** new `workshop/src/botworkshop/{ResolveScripts,WorldCensus}.java`, `workshop/test/botworkshop/ResolveScriptsTest.java`. Modified `build.gradle` (`workshopResolveScripts`), `BOT_TOOLING.md` (§4 example, §6 the three questions, §7.2, T6b, acceptance).

**Status:** done. **868 tests, 0 failures** (855 → 868, the 13 new ones in `workshopTest`); `workshopJsTest` 18/18. Verified on this checkout: `tree` 12,951 objects and `bank` 176 (the same count `ResourceKinds` documents for the bank rule, reached by a different route), `rock` 1,724 and `fishing` 39 resolving by scan with no authored row, `cooking` reported as resolving nowhere and exiting 1. Remaining in the track: **T7** (optional live channel).

## 2026-10-10 - T5b: the graph view — one document, two projections, no view holding state the other can lose

**What changed:**
- **Both views are now projections of one document.** The model moved out of `timeline.js` into `workshop/web/js/script-doc.js` as pure functions (`timelineShape`, `buildTimeline`, `nodeAt`, `removeAt`, `moveAt`, `addChild`, `convertNode`), and the new `graph.js` renders the same document as an editable outline. Nothing is cloned and nothing is synchronised: switching tabs re-derives the view, so "lossless two-way parity" is structural rather than a promise two editors keep about each other. Verified byte-identically in the browser (a branch built in the graph, Graph→Timeline→Graph, same JSON).
- **The graph is an outline, not a canvas** — nesting *is* the edge, so there is no node position to store, lay out, or let go stale, which is what lets it be rebuilt from the document alone. A composite shows its children with add/remove/reorder and an "add child" menu built from the palette; a decorator or leaf shows its parameters through the same generated forms (§7.2), which is why `fields.js` was split out.
- **The timeline's one limitation is now local to the timeline.** A document the timeline cannot flatten (a nested composite, a decorator around a step) is read-only *in that tab* with the reason shown, and fully editable in the graph — instead of the whole document opening read-only, which would have made T5's rule "no document the editor can't hold" cost authors the ability to edit valid scripts. `convertNode` only copies parameters the target node's schema declares, so converting a leaf into a composite cannot smuggle `count` into a `sequence` and produce JSON the server would refuse.
- **The model is checked without a browser.** `workshop/web/test/script-doc.test.mjs` (18 checks) runs under `gradlew workshopJsTest` — what the timeline can and cannot hold, what it compiles to, and how a path is addressed, moved and replaced — against the real `bot-nodes.json` rather than a fixture, because the parameter names come off the schema and a fixture would agree with a bug. Deliberately not wired into `check`, so a Java-only build is unaffected.

**Files touched:** new `workshop/web/js/{script-doc,graph,fields}.js`, `workshop/web/test/script-doc.test.mjs`, `workshop/web/package.json`. Modified `workshop/web/js/{timeline,app}.js`, `workshop/web/{index.html,styles.css}`, `build.gradle` (`workshopJsTest`), `BOT_TOOLING.md` (T5b ✅, §4 Layer 3b "As built", §7.2 round-trip, acceptance).

**Status:** done. **855 tests, 0 failures**; `workshopJsTest` 18/18. Verified in the browser end to end: tab switching preserving bytes, building a nested selector + delay in the graph, the server refusing an incomplete branch (with its path) through the same `POST /scripts/check`, saving a valid branch as `graph_branch_demo.json`, `workshopValidateScripts` accepting it, reopening it into the graph, and the demo file removed after. Remaining in the track: **T7** (optional live channel).

## 2026-10-10 - T6: the file the editor writes is the thing that runs

**What changed:**
- **The authored script now runs for real.** `AuthoredScriptRunTest` reads `Data/cfg/bots/chop_and_bank.json`, loads it through `ScriptDocument` — the server's own loader — and ticks it until logs are banked: the real click path, the real skill dispatch, the real `BotManager` tick boundary. Nothing between the file and the bank tells the bot what to do, which is `BOT_TOOLING.md` T6's criterion. Two tests, one running the loop and one guarding the artifact on disk.
- **The last claim about T6 was wrong, and this corrects it.** The previous entry said the authored loop "cannot be driven with an injected world" because the loader builds nodes with the live `Locations`/`ResourceScan` constructors. True but not binding: those two leaves resolve their world *lazily on first tick*, so injecting the world through the live seams works even though there is no constructor to pass it to. `Locations.install`/`uninstall` and `ResourceScan.install`/`uninstall` are that seam — small, off by default, and the same seam the editor's world-less preview (`BOT_WORKSHOP_UX.md` §5.4) needs, so they are not test-only scaffolding.
- **The test has teeth, and checking that is how a papercut was found.** Rewriting the committed file's `"kind": "tree"` to `"rock"` makes it fail after 20,000 ticks with nothing banked — so the file drives the outcome rather than the test passing vacuously. The check also exposed that Gradle treated `:test` as up-to-date across a script edit; `build.gradle` now declares `Data/cfg/bots` as a test input, so editing a script re-runs the test instead of leaving a stale pass.
- **The committed example is pinned as canonical.** `ScriptDocsTest` asserts `chop_and_bank.json` is already the bytes the editor would emit, so re-saving it after an unrelated edit is not a diff, and a hand-edit is caught.

**Files touched:** new `test/server/game/bots/AuthoredScriptRunTest.java`. Modified `src/server/game/bots/world/{Locations,ResourceScan}.java` (install/uninstall seam), `workshop/test/botworkshop/export/ScriptDocsTest.java`, `build.gradle` (test input), `BOT_TOOLING.md` (T6 ✅, acceptance, §7.2).

**Status:** done. **855 tests, 0 failures** (852 → 855). What T6 does *not* cover: resolution against the live `Data/world`, which needs a running server — `::bot reload` then the loop in game remains a manual check. Remaining in the track: **T5b** (graph view over the same document) and **T7** (optional live channel).


## 2026-10-10 - The timeline editor: author a bot by hand, saved as the file the server runs

**What changed:**
- **The authoring loop is now closed at both ends.** `BOT_TOOLING.md` T5 adds the step timeline (`workshop/web/js/timeline.js`) — an ordered list of steps wrapped in a `Sequence`/`Repeat`, compiled to exactly the §7.1 document T4b loads. Palette and every parameter form are generated from `bot-nodes.json`, so the editor hardcodes no node id, parameter name or type. The single rule that shapes the palette is that a *step* is a node taking no child (`NODE`/`NODE_LIST`) — which is why `delay` is offered and `retry`/`sequence` are not.
- **The browser never writes the file.** `POST /scripts/check` and `/scripts/save` take the document, run it through `ScriptDocument` — the server's own loader — and re-emit it canonically before writing (`botworkshop.export.ScriptDocs`): `node` first, parameters in the node's declared order, integers as integers. Same graph, same bytes, so a re-save is not a spurious diff. Save refuses a name that is not a file name and a name that would shadow a built-in, and validates before it touches the disk.
- **Round-trip is lossless, or it declines.** Reopening a saved script reconstructs its steps; a document with a nested composite or a decorator around a step opens read-only with the reason, rather than flattening into a list that would silently drop it.
- **A parameter the author never touched is omitted**, so the node's own default applies and a later change to that default is honoured; a stated value is written, because the document is the author's intent.
- **`kind` parameters gained their values.** `bot-nodes.json` now carries a `values` array on `KIND` parameters, enumerated from the server's `LocationKind`, so the editor's dropdown cannot offer a kind the loader would refuse.
- `gradlew workshopValidateScripts` re-loads `Data/cfg/bots/*.json` through the server's loader, so a bad authored script fails in a command rather than as a skipped line at boot.
- **`Data/cfg/bots/chop_and_bank.json`** — the slice-1 chop→bank loop, built in the editor — is committed and loads as `Repeat(forever)`.

**Files touched:** new `workshop/web/js/timeline.js`, `workshop/src/botworkshop/export/ScriptDocs.java`, `workshop/src/botworkshop/ValidateScripts.java`, `workshop/test/botworkshop/export/ScriptDocsTest.java`, `Data/cfg/bots/chop_and_bank.json`. Modified `workshop/web/{index.html,styles.css,js/app.js}`, `workshop/src/botworkshop/export/BotNodes.java`, `workshop/src/botworkshop/serve/WorkshopServer.java` (script routes), `workshop/test/.../BotNodesTest.java`, `src/server/game/bots/script/{BotScripts,ScriptDocument}.java` (`isBuiltIn`, public `fromDocument`), `build.gradle`, `BOT_TOOLING.md` (§7.2, T5).

**Status:** done. **852 tests, 0 failures** (831 → 852). Verified in the browser end to end: palette, add/reorder/delete, generated parameter forms, live JSON preview, save, built-in/traversal refusal, and reopening `chop_and_bank` reconstructing all four steps. Two editor bugs were found and fixed this way — `INT` inputs stored text (`"ticks": "2"`, which the server correctly refused) and its status line stuck on "saving…". **T6** remains: the in-world timed run of the authored loop, which needs a live world (the loader's nodes use the live `Locations`/`ResourceScan`, so it cannot be driven with an injected one).


## 2026-10-10 - Authored bot scripts: a behaviour graph is now a file, not a Java class

**What changed:**
- **The authoring gap is closed on the server side.** Until now a bot's logic had to be registered in Java (`BotScripts`' static initialiser), so a new routine meant a rebuild — the exact limitation the tooling track exists to remove. `Data/cfg/bots/*.json` is now real: one behaviour graph per file, loaded at boot and on `::bot reload`, and referable from a `bots.cfg` row's `script` field exactly like the built-in `gather_oak`.
- **The schema is the source of truth, so nothing is transcribed.** `ScriptDocument` builds each node through `BotNodeRegistry` (`BOT_TOOLING.md` §7.1), reading ids, parameter names, types and defaults off the annotated classes. A node becomes authorable the moment it is annotated; there is no second copy of the vocabulary to drift. `Json` is a small self-contained reader in the bot package, because the server must not depend on the workshop source set (the tool has to stay deletable).
- **Strict on purpose:** an unknown node, an unknown *field* (a typo would otherwise silently take a default), a missing required field or a wrong type is refused with the node and field named. `LOCATION` parameters are refused until a node declares one, rather than inventing an encoding the editor must match.
- **Reload reflects the directory:** file-sourced scripts are dropped and re-read, a deleted file's script disappears and an edited one is replaced, and a built-in is never dropped. A bad file is reported and skipped — never fatal — and a missing directory is the ordinary "no authored scripts" state.
- **Document-built trees are traced** (`Traced`-wrapped, fresh per possession) exactly like builder-built ones, so `::botinfo` works on both.

**Files touched:** new `bots/script/Json.java`, `bots/script/ScriptDocument.java`, `test/.../script/{JsonTest,ScriptDocumentTest,ScriptDocumentsLoadTest}.java`. Modified `bots/script/BotScripts.java` (directory loader, reload semantics), `bots/meta/BotNodeRegistry.java` (`constructorFor`), `bots/BotManager.java` (load scripts at boot and reload), `BOT_TOOLING.md` (§7.1 format, T4b stage, acceptance).

**Status:** done. **831 tests, 0 failures** (781 → 831). Next: **T5** — the timeline editor in `workshop/web` now has a concrete format to emit and a loader to be validated against (T5b graph view, T6 round-trip). The end-to-end "authored timeline runs the chop→bank loop" check is **T6**, and note it needs a live world: the loader's nodes use the live `Locations`/`ResourceScan` (their canonical constructors), so unlike the Java-built loop tests it cannot be driven with an injected world.


## 2026-10-10 - Bot roadmap H: the per-tick budget (scale)

**What changed:**
- **The bot system now has a bounded per-tick cost.** `BotManager.beginTick()` runs first in `Server.tick()` and picks which bots may act; `BotPlayer.process()` offers its tree through `BotManager.tickTree(...)` instead of ticking it directly. At most `Config.BOT_TICK_BUDGET` trees tick per game tick.
- **A rotating window, not a queue.** "First K, resume at K" would starve the tail, because the player loop always visits bots in slot order. Offsetting the window by the tick number (`inTickWindow`) guarantees every bot is reached within `ceil(live / budget)` ticks. With `live <= budget` (the default at `MAX_BOTS = 10`) nothing changes at all.
- **The count budget is the mechanism; the wall-clock cap is a backstop, and it engages only when `live > budget`.** This is a correction made while building it: an always-on wall-clock cap needs a per-tick reset, and any caller driving `process()` without signalling a tick accumulates forever and starves every bot — which is exactly what **two existing loop tests (`ChopBankLoopTest`, `ScriptLoopTest`) did** the moment this landed. Those loops now call `beginTick()`, because they *are* the tick, and the cap is scoped to the case it exists to bound so no future caller can be starved by it.
- **Graceful stop:** `stopAll()` wired into `Server.requestStop()` before the characters are written, so every tree is exited with `interrupted = true` (releasing charges and `CycleEvent`s) before its save. **Cohorts:** `spawnAll(...)`, counting by delta because `spawn()` is idempotent; `apply()` now uses it.
- **Staggered scans come free:** `ResourceScan` already runs on retarget rather than per tick, so bounding when a tree acts bounds when it scans. No second scheduler was added.
- `::bot list` now reports the last tick (`ticked, deferred, ms in trees`), because a budget-deferred bot otherwise looks like a slow bot.

**Files touched:** new `test/.../BotSchedulingTest.java` (14 tests). Modified `bots/BotManager.java`, `bots/BotPlayer.java`, `Config.java` (`BOT_TICK_BUDGET`, `BOT_TICK_BUDGET_MS`), `Server.java` (`beginTick` in `tick()`, `stopAll` in `requestStop()`), `commands/BotCommands.java`, `test/.../{ChopBankLoopTest,ScriptLoopTest}.java`, `BOT_ROADMAP.md`.

**Status:** done. **781 tests, 0 failures** (664 server + 117 workshop); `workshopValidate` green. ⚠️ The budget (32) exceeds `MAX_BOTS` (10), so it is deliberately dormant until the cap is raised — raising the cap is a product decision, not a Phase H one. Next: **I (RL)**, the only phase left and the one that touches core (tick-timer refactor first); or the residual half of G (object dispatch made actor-generic so an NPC can skill).

## 2026-10-10 - Bot provisioning: a spawned bot now owns the tools its script needs

**What changed:**
- **The gap is closed.** `BotManager.createAccount` used to grant no kit at all, so a config-spawned woodcutter owned no axe and `Gather` failed on its first click. It now applies **`BotProvisioning.provision(...)`** from a named **`BotProfiles`** kit before the character is saved.
- **`BotProfiles`** — four lean kits: `default`, `woodcutter`, `miner`, `fisher`. **`default` is the generalist** (bronze axe + pickaxe + net + tinderbox), so the obvious config line — account, password, script — works with no fourth field; a row that wants a leaner kit names one. **`bots.cfg` gained an optional `profile` column** (a bad name logs and falls back to `default`, like a bad `home`).
- **Granted only at creation, never on possess**, so a restart cannot accumulate a second axe. `reprovision` (clear, then apply) is the replace path, and **`::bot reprovision <account> <profile>`** makes it reachable — the command `BOT_ACCOUNTS.md` §4.1 promised but which did not exist.
- Three traps found and pinned by tests rather than left implicit: **(1)** `Player`'s constructor seeds every skill to 1 *except hitpoints (10)*, so a "set all skills to 1" reset leaves a character dead on arrival — hitpoints is restored explicitly; **(2)** `provision` is **additive for items**, safe only because creation starts empty, documented so nobody "fixes" it into an inventory wipe; **(3)** item ids are checked against this server's own `Data/cfg/item.cfg` (a test reads it), because `addItem` silently grants nothing for an undefined id.
- The starter-kit "drift note" in `BOT_ACCOUNTS.md` §4.1 was **wrong in its goal** and is corrected: the adventurer starter grants 2,000,000 coins, so sharing the list would have meant sharing *and* filtering. The bots keep their own lean kits and there is no drift to reconcile.
- ⚠️ One test premise had to change, and it failed *because provisioning works*: `ChopTreeStateTest`'s "without an axe" case relied on a fresh bot owning nothing. The fixture now hands every bot the default kit, so that test clears the inventory explicitly.

**Files touched:** new `bots/BotProfiles.java`, `bots/BotProvisioning.java`, `test/.../BotProvisioningTest.java`. Modified `bots/BotManager.java`, `bots/BotProfile.java`, `bots/BotsConfig.java`, `players/packets/commands/BotCommands.java`, `Data/cfg/bots.cfg` (template + `profile` docs), `test/.../{BotProfileSpawnTest,BotsConfigTest,ChopTreeStateTest}.java`, `BOT_ACCOUNTS.md`, `BOT_ROADMAP.md`.

**Status:** done. **767 tests, 0 failures** (650 server + 117 workshop). Next: **H (scale)** — or the residual half of G, making object dispatch actor-generic so an NPC can skill.

## 2026-10-10 - Bot roadmap G: the Agent seam (one behaviour library, two actors)

**What changed:**
- **`Agent`** (`name`/`x`/`y`/`height`/`arrivedAt`/`isIdle`/`walkTo`/`interactObject`) with two implementations: **`PlayerAgent`** (wraps `Client`) and **`NpcAgent`** (wraps `NPC`). **`BotContext.client()` is gone** — states get `agent()` plus narrow observation (`isBanking`/`isDead`/`isSkilling`/`skillLevel`), so nothing in the tree is written against "the actor is a player" any more.
- The movement/interaction glue moved out of `PlayerBotContext` into `PlayerAgent` **verbatim**, so runtime behaviour is unchanged; the 7 `client()` call sites needed only the new narrow observations and **no state class changed shape**. That is the payoff for having put the context behind an interface in slice 1.
- ⚠️ **`NpcAgent.interactObject` throws.** Object dispatch is `Client`-typed (`ObjectHandler.dispatch` and its `ObjectAction`s), so an NPC has no registry path to click scenery. It throws *with the reason* rather than returning `false`, because `false` is indistinguishable from "out of range" and would make a `Gather` loop retry forever. **An NPC can travel and patrol; it cannot yet skill** — widening object dispatch is the one piece of G still outstanding.
- Tests: `NpcAgentTest` (9) pins the NPC contract including the refusal; `AgentSeamTest` (5) drives one walker that only ever sees `Agent` through **both** actors, and runs slice-1's `WalkTo` — never modified — against a **real `NPC`** to its destination. `FakeAgent` was split out of `FakeBotContext` to mirror the actor/player-only division.
- Roadmap §5.1/§6/§8 updated: G recorded as **the seam, not a `WorldAdventurer` migration** (that NPC is disabled), with the object-dispatch limit written down instead of glossed.

**Files touched:** new `bots/Agent.java`, `bots/PlayerAgent.java`, `bots/NpcAgent.java`, `test/.../FakeAgent.java`, `test/.../NpcAgentTest.java`, `test/.../AgentSeamTest.java`. Modified `bots/BotContext.java`, `bots/PlayerBotContext.java`, `bots/BotController.java`, `bots/states/{WalkToNearest,BankLogs,ChopTree}.java`, `bots/condition/{IsDead,BankOpen,SkillAtLeast}.java`, `test/.../FakeBotContext.java`, `BOT_ROADMAP.md`.

**Status:** done. **743 tests, 0 failures** (626 server + 117 workshop); `workshopValidate` green (19,410 named objects). Next: **H (scale)**, or bot provisioning — still the gap that leaves a spawned woodcutter with no axe.

## 2026-10-10 - WorldAdventurer ("Max") disabled behind a config flag

**What changed:**
- **`Config.WORLD_ADVENTURER_ENABLED` (default `false`)** turns the wandering NPC off. He is a hand-rolled travel/work state machine written before the behaviour-tree bot system existed, and he force-chats at players and teleports himself around the world, so off is the honest default while the bot system is what does this job.
- **The gate is inside `WorldAdventurer.spawn()`, not at the call sites.** `teleportTo` lazily calls `spawn()` when the NPC is missing, so a gate at `Server.main` alone would have let `::max`/`::adventurer` quietly bring back a disabled NPC. One gate covers boot, the command, and the lazy re-spawn.
- **`::max`/`::adventurer` now says so** ("Max is not enabled on this world.") instead of the misleading "Max isn't in the world right now."
- Everything else is unchanged and inert without a live NPC: the click/dialogue/button hooks all go through `WorldAdventurer.isAdventurer(index)`, which reads the per-NPC flag that only `spawn()` sets, and `NPCHandler`'s tick branch is behind the same flag. Code is kept, not deleted, because Phase G of the roadmap would migrate this NPC onto the shared behaviour tree.

**Files touched:** `Proxy Server/src/server/Config.java` (new flag), `src/server/game/npcs/WorldAdventurer.java` (`isEnabled()`, guards in `spawn`/`teleportTo`), `src/server/Server.java` (comment on the startup call).

**Status:** done. **729 tests, 0 failures** (612 server + 117 workshop). To bring Max back, set the flag to `true`; nothing else changes.

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
