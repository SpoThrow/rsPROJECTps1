# BOT_PARKED.md — where the bot work stopped

**Parked 2026-10-10.** The bot system works end to end and two sample bots run unattended; what is
*not* done is a short, ranked list, and none of it is load-bearing for anything else in the server.
This file is the resume point. Everything below was true at commit `2e5a2eeb` on
`cursor/player-decomposition-refactor`.

Related, and still the authority on their own subjects: `BOT_ROADMAP.md` (phases A–I),
`BOT_TOOLING.md` (the visual author's track T1–T7), `BOT_ACCOUNTS.md` (profiles and credentials),
`BOT_GAMEPLAY.md`, `BOT_LOCATIONS.md`, `BOT_WORKSHOP_UX.md`. Per-change history is in `UPDATE_LOG.md`.

---

## 1. How to pick it up again

Everything is a Gradle task from `Proxy Server/`:

```powershell
cd "Proxy Server"
.\gradlew.bat run                 # the game server; it starts the configured bots itself
.\gradlew.bat workshopServe       # the map viewer + author, http://127.0.0.1:8080
.\gradlew.bat test workshopTest   # 889 tests, the Java suites
.\gradlew.bat workshopJsTest      # 49 checks, the two node suites behind the viewer
.\gradlew.bat workshopValidateScripts   # does each Data/cfg/bots/*.json still load?
.\gradlew.bat workshopResolveScripts    # do the kinds the scripts name exist in the world?
```

There is nothing to restart by hand: `Data/cfg/bots.cfg` is read at boot, and a bot named there whose
character file is missing is created (with its profile's kit and levels) before it spawns. From
`Proxy Server/`, the startup lines to look for are:

```
[bots] loaded 1 authored script(s) from Data/cfg/bots (2 registered in all)
[bots] 2 of 2 configured bot(s) running (2 live)
[bots] live view on http://127.0.0.1:8081/live/bots
```

`http://127.0.0.1:8081/live/bots` is the raw report; the viewer's **Live bots** panel is the same thing
through the workshop's `/live/bots` proxy. Turn **watch** on in that panel and it polls once a second.

---

## 2. What is actually finished

**Roadmap (`BOT_ROADMAP.md`): A, B, C, D, E, F, G, H, plus provisioning.** A bot added by editing
`Data/cfg/bots.cfg` is created on a real saved character with the tools its script needs, runs on the
tick with a bounded per-tick cost, is visible through `::bot`/`::botinfo` and the live endpoint, and is
stopped and saved cleanly on shutdown. Phase **I (RL)** is the only one not started — it is the one
that touches core (a tick-timer refactor) and nothing else waits on it.

**Tooling (`BOT_TOOLING.md`): T1–T7a.** The viewer draws the real world (map, planes, clipping,
resource icons), authors places into `locations.cfg`, authors scripts as a timeline *or* an outline
over one shared document, validates what was authored with the server's own loader, resolves the kinds
a script names offline, and shows the live bots. **T7b** (possess/release, pause/step from the browser)
is the optional remainder, deliberately unbuilt.

**Provisioning, which is what this last session was about.** `BotProfiles` carries per-profile
starting levels and `BotProvisioning` applies them with matching XP:

| profile | grants | why that level |
| --- | --- | --- |
| `woodcutter` | woodcutting 30 | Draynor/lumbridge willows (30); oaks (15) come free |
| `miner` | mining 15 | iron, the first ore the bronze pickaxe improves on |
| `fisher` | fishing 20 | trout, what a rod and feathers are actually for |
| `default` | all three of the above | a row that names no profile has said nothing about its resource |

The level matters because a tool without it is a dead bot: `Woodcutting` refuses a level-1 character at
*every* shipped tree place. The XP matters because `Client.process()` drains `playerLevel` back towards
`getLevelForXP(playerXP)`, so the level is written as `getPA().getXPForLevel(level) + 1` rather than on
its own (`+1` because `getLevelForXP` advances only once XP *exceeds* a threshold).

**Both shipped sample bots are enabled**, one per way a script can arrive:

```text
account oakchopper password ch0pme0ak script gather_oak    profile woodcutter home draynor_oaks enabled true
account oakbanker  password b4nkme0ak script chop_and_bank profile woodcutter home draynor_oaks enabled true
```

Watched on a real boot, not just tested: each spawns with Woodcutting 30 / 13364 XP, walks to a tree,
chops a full inventory (~350 ticks), banks it and returns, with no `lastFailure`.

**`Config.BOT_STATUS_PORT = 8081`** in the shipped config, so the live panel works from a plain server
start. It is loopback-only and read-only; `0` turns it off with no code change.

---

## 3. The open items, ranked

1. **A gather loop banks one item id, but the world picks the species.** `gather_oak` and the authored
   `chop_and_bank` both deposit plain logs (1511) while the nearest tree decides whether the log is
   plain (1511), oak (1521) or willow (1519). At a mixed field a bot can fill up with oak logs, deposit
   none of them, and shuttle between bank and field forever. Recorded in `BOT_ACCOUNTS.md` §4.1. Three
   ways out, all of which change something currently simple: a species-aware gather, a species-aware
   deposit, or a homogeneous authored place. **This is the one worth doing first** — it is the
   difference between the sample bots looking alive and looking broken.
2. **T7b, the writing half of the live channel** (`BOT_TOOLING.md` §10). Possess/release and pause/step
   from the browser. Deferred on purpose: a write path that can start a bot is a second way to drive
   bots that bypasses `::bot`'s gating, so it should not be built until the read-only half has been used
   enough to say what it should do.
3. **An NPC still cannot skill.** `NpcAgent.interactObject` throws rather than returning a false that a
   `Gather` loop would retry forever, because `ObjectHandler.dispatch` and its `ObjectAction`s are
   `Client`-typed. An NPC can travel and patrol today; making it chop is a change to the skill-dispatch
   path (roadmap G's one outstanding promise, §6).
4. **Roadmap I (RL).** Not started, needs a tick-timer refactor first. Optional, and the largest piece.

---

## 4. Gotchas that will otherwise cost you an hour

- **The bots look stuck and are not.** `Woodcutting.getTimer` gives this cache roughly 3 ticks per
  plain log and 19–39 for an oak, so a full 28-slot load is *minutes* of sitting on `Gather(tree)`. The
  state path and position genuinely do not change while that happens.
- **`bob.txt` changes on every boot.** The dev character's tile/`autoRet` are rewritten by simply
  running the server. It is tracked, so it shows up as a diff; don't commit it.
- **`server_run.log`, `workshop_run.log`, `bot_poll.log` are not ignored.**
  `git status` will offer them. Delete or leave them unstaged.
- **`jdt-bin/` is tracked, in three places** (`Proxy Server/jdt-bin`, `jdt-bin`, and `jdt-bin/*.bak`),
  and past commits include the recompiled `.class` files. Follow that if you want the diff to look like
  the others; the `*.bak` files are churn and should stay out.
- **A bot takes a player slot and counts as online.** `BotManager` registers each bot through
  `PlayerHandler.registerSessionless`, so it sits in `PlayerHandler.players[]` like anyone else.
  `updatePlayerNames()` runs every tick and counts every non-null slot, so the "Currently online: N"
  line includes bots and real logins share the same `Config.MAX_PLAYERS = 50` budget with up to
  `MAX_BOTS = 10` of them.
- **Autosave is every 5 minutes** (`PlayerSaving.SAVE_INTERVAL_MS`), one character per tick, on the
  tick thread; a bot's state is only on disk after that, on logout, or on a clean shutdown. Killing the
  JVM skips it.
- **Two background servers may still be running from this session** — the game server on `43594`
  (with the two bots) and the workshop on `8080`, both started with `gradlew ... run`/`workshopServe`.
  They are ordinary processes; stop them from Task Manager or by killing their Gradle/java PIDs.

---

## 5. Nothing here blocks the rest of the project

The bot package is deliberately removable: the boot path touches it through two guarded call sites
(`BotManager.start()` and `LiveBotsServer.startIfEnabled()`, the latter a no-op unless
`BOT_STATUS_PORT` is set), and with no `Data/cfg/bots.cfg` the server boots exactly as it always did.
Nothing in the client, the refactoring plans, or any non-bot feature depends on it being finished.
