# BOT_GAMEPLAY.md — the server's loop, and what a bot wants

Answers three questions in one place:

1. **What is this server for** — what is a player meant to *do*?
2. **When does a bot attempt a boss?**
3. **When does a bot PvP, and when does it just chill at home?**

The first is analysis; the second and third are a new design layer — the **motivation
arbiter** — that sits above the activity scripts from `BOT_ACTIVITIES.md` and decides
*which* one runs. Companion to `BOT_LOCATIONS.md` (the data layer) and `BOT_ROADMAP.md`
(the runtime). Everything below is grounded in verified server facts.

---

## 1. What this server is for

It is a progression-and-economy RSPS with a PvP endgame — `SERVER_NAME = "Soul-Trail"`.
The intent, read off the systems that were actually built:

- **Progression** is the spine: max every skill. XP is heavily boosted
  (`Config`: woodcutting ×15, mining ×16, slayer ×19; melee ×600, range ×575, magic ×550),
  so 99s are a *session* goal, not a lifetime goal.
- **Combat** is where the money is: all loot is bankable, shops sell the gear ladder, and
  the combat XP rates are ~40× skilling, so a bot that fights progresses fastest.
- **Bosses** are the gear ceiling: GWD, KBD, Dag Kings and KQ are the sources of the best
  uniques in the world.
- **PvP** is the prestige loop: a Bounty Hunter crater system, a wilderness with PK
  teleports, and a PvP points shop. This is what a small-population server is actually *for*
  socially.
- **Home** is Edgeville — the start spawn, the bank, and a full shop mall
  (`BOT_LOCATIONS.md` B.3). Lumbridge is the death respawn. There is no separate
  "home instance"; home is a **place**, not a portal.

So the player's job is the classic loop, at high speed:

> **earn XP and loot → spend it on better gear → earn faster → take on bigger bosses and
> other players.**

---

## 2. The loop, concretely

```
            ┌──────────────────────────── Edgeville (home) ────────────────────────────┐
            │  bank · shops · re-gear · re-stock · sell loot · pick next goal          │
            └───────────────┬──────────────────────────────────────────────────────────┘
                            │
     ┌──────────────────────┼──────────────────────┬───────────────────────┐
     ▼                      ▼                      ▼                       ▼
  SKILL to 99          SLAYER task            TRAIN combat           BOSS / MINIGAME
  (tools, bank)     (Vannaka → monster)     (spot by level band)    (teleport + gate)
     │                      │                      │                       │
     └──────── loot + XP + coins ─────────────────┴───────────────────────┘
                            │
                      back to home to bank/spend
```

Every arm ends the same way: **more loot, more coins, more levels — then spend at home and
go again.** The bots are there to keep this loop visibly turning.

---

## 3. The activity map (verified)

| Family | Where | Entry | Reward |
| --- | --- | --- | --- |
| Skilling | per-skill spots (`BOT_ACTIVITIES.md`) | tool + level | XP, sellable goods |
| Slayer | Vannaka `2871,2982` | `Slayer.Task` gates | XP, points, loot |
| Combat training | level-band spots | gear | XP, loot, coins |
| **Bosses** | KBD, Dag Kings, GWD ×4, KQ | see §4 | best uniques |
| **PvP** | wilderness + BH craters | see §4 | PvP points, BH cash |
| Minigames | Barrows, Fight Caves, Castle Wars, Pest Control, Trawler, Duel Arena, Soul Wars | per-game | per-game |
| Economy | Player-Owned Shops (`::pos`) | — | trading, wealth |
| Home | Edgeville `3087,3505` | always | bank, shops, rest |

Boss teleports are all in `teleports.cfg` (`Boss GWD 2902 3724`, `Boss KBD 3007 3849`,
`Boss Dag Kings 2547 3758`, `Boss KQ 3310 3109`, …), and PK teleports too
(`PK Edgeville 3089 3528`, `PK Castle 3016 3632`, `PK Magebank 2539 4716`,
`PK Hill Giants 3288 3631`, `PK Ardougne Lever 2561 3311`).

---

## 4. The gates — what decides "can I do this yet"

These are the facts that convert "the bot wants to boss" into "the bot may boss".

### Bosses

| Boss | NPC | Combat | Entry |
| --- | --- | --- | --- |
| King Black Dragon | 50 | 276 | `Boss KBD 3007 3849`; **anti-dragon shield** + food |
| Dagannoth Supreme / Prime / Rex | 2881 / 2882 / 2883 | 303 | `Boss Dag Kings 2547 3758`; tribrid gear, prayer |
| General Graardor | 6260 | 624 | GWD chamber (see route below) |
| K'ril Tsutsaroth | 6203 | 650 | GWD chamber |
| Commander Zilyana | 6247 | 596 | GWD chamber |
| Kree'arra | 6222 | 580 | GWD chamber |
| Kalphite Queen | 1158 → 1160 | — | `Boss KQ 3310 3109`; two forms (`KalphiteQueen`) |

**GWD — the real route (verified).** The boss-chamber teleport object (`2492`) is gated on
**`killCount >= 20`** — but that gate is **dead**: `Player.killCount` is loaded from the
save and *read* at `ActionHandler:250`, and **never incremented anywhere** in the live
code. The object therefore always refuses, and the teleport interface is the only way in:

1. Teleport-interface boss button → `spellTeleport(2902, 3724, 0)` (`ClickingButtons:577`).
2. Walk to the **hole (object `2823` @ `2903, 3732`)**; `TeleportObjects` sends you to
   **`2881, 5310, plane 2`** — inside GWD.
3. Kill **15 minions** of the faction you want.
4. Enter that faction's chamber door:

| Faction | Door object | Door needs |
| --- | --- | --- |
| Bandos | `26425` | `bandosKills >= 15` |
| Armadyl | `26426` | `armaKills >= 15` |
| Saradomin | `26427` | `saraKills >= 15` |
| Zamorak | `26428` | `zamorakKills >= 15` |

The faction counters are incremented on killing that faction's *minions*, are **capped at
15** (`NPCHandler.appendBandosKC` etc.), and reset to 0 when the player teleports or leaves
GWD (`PlayerAssistant:2415-2433`) — so KC must be built and spent in one outing.

**Boss loot (verified, `npc_drops.cfg`).**

| Boss | NPC | Always | Uniques (rarity) |
| --- | --- | --- | --- |
| King Black Dragon | 50 | dragon bones `1747`, bones `536` | **draconic visage `11286`** (RARE), dragon med helm `1149` (RARE), rune sq shield `1185` (RARE), rune platebody `1127` (VERY_RARE) |
| Dagannoth Supreme | 2881 | dagannoth bones `6729`, hide `6155` | **archers' ring `6733`**, **dragon axe `6739`**, seercull `6724`, fremennik blade `3757` |
| Dagannoth Prime | 2882 | dagannoth bones `6729`, hide `6155` | **seer's ring `6731`**, **dragon axe `6739`**, farseer helm `3755`, skeletal top/bottoms `6139`/`6141` |
| Dagannoth Rex | 2883 | dagannoth bones `6729`, hide `6155` | **berserker ring `6737`**, **warrior ring `6735`**, rock-shell plate/legs `6129`/`6130` |
| General Graardor | 6260 | bones `532`, 12–23k coins | Bandos hilt `11704`, Bandos chestplate `11724`, tassets `11726`, boots `11728`, shards `11710`/`11712`/`11714` (all RARE) |
| K'ril Tsutsaroth | 6203 | bones `532`, 12–23k coins | Zamorak hilt `11708`, **dragon boots `11732`** (UNCOMMON), shards |
| Kree'arra | 6222 | bones `532`, 12–23k coins | Armadyl hilt `11702`, Armadyl helm/chest/skirt `11718`/`11720`/`11722`, shards |
| Commander Zilyana | 6247 | bones `532`, 12–23k coins | Saradomin hilt `11706`, **Saradomin sword `11730`**, shards |
| Kalphite Queen (2nd form) | 1160 | 1–4.8k coins | **dragon chainbody `3140`**, **dragon 2h sword `7158`**, rune spear `1247`, rune axe `1359` |

Two flags:
- **KQ only drops from its second form (`1160`).** The first form (`1158`) has no entry, so
  the bot must kill both forms before it can loot.
- The dagannoth rings are split canonically — **archers on Supreme, seers on Prime,
  berserker/warrior on Rex** — so a ring-hunting bot picks its king, not the lair.

### PvP

| Route | Gate |
| --- | --- |
| Wilderness | none — walk/teleport in; `wildLevel` computed from `absY` |
| Teleport block | cannot teleport above `NO_TELEPORT_WILD_LEVEL = 20` |
| BH crater — low | combat **3–55** |
| BH crater — med | combat **50–100** |
| BH crater — high | combat **95+** |
| BH entry kit limit | **≤4 weapons, 1 body, 1 legs** (verified `BountyHunter.checkReqs`) |

BH rewards: kills convert to cash via **Veteran Hervi** once the bot has **10+ bounty
kills** (`killsMultiplier * 10 - bountyKills`), and there is a **PvP points shop (id 47)**.

### Minigames

Barrows (kill count → chest), Fight Caves (fire cape), Castle Wars, Pest Control,
Trawler (**needs Fishing 50**), Duel Arena, Soul Wars (`teleports.cfg` Minigame rows).

---

## 5. The motivation arbiter (new)

Everything so far tells the bot *how* to do an activity. Nothing tells it *which*. This is
the missing layer, and it is exactly roadmap **Phase E/F** ("the bot decides").

```java
package server.game.bots.motivation;

/** One thing a bot might want to do, scored against its current state. */
public interface Motivation {
    String id();                                  // "boss.gwd.bandos"
    boolean isAvailable(BotContext ctx);          // gates: level, gear, items, KC
    int score(BotContext ctx);                    // 0 = not now; higher = keener
    BotState build(BotContext ctx);               // the script to run
}
```

The arbiter picks the highest-scoring available motivation each time the bot reaches a
decision point (a goal completes, fails, or is interrupted):

```java
public final class GoalArbiter {
    Motivation next(BotContext ctx) {
        return motivations.stream()
            .filter(m -> m.isAvailable(ctx))
            .max(Comparator.comparingInt(m -> m.score(ctx)))
            .orElse(chillHome);
    }
}
```

`score` blends **hard state** (levels, gear, coins, food, HP) with an **archetype weight**
so two bots with identical stats can behave differently. Archetypes (a data file, not
code): `grinder` (skilling-first), `slayer` (task-first), `bosser`, `pker`, `lurker`
(mostly home/idle), `generalist`.

---

## 6. When does a bot attempt a boss?

**Answer: when it is geared to survive the specific boss, stocked to finish it, and has the
key/gate — scored against its archetype.**

`BossMotivation.isAvailable` requires *all* of:

| Check | Why |
| --- | --- |
| Combat level ≥ boss floor | a level-40 bot into KQ is just a death |
| Gear meets boss minimum (`EquipmentPlanner.bestFor`, `BOT_LOCATIONS.md` B) | e.g. anti-dragon shield for KBD |
| ≥ N food + prayer/restore pots in inventory | the fight has eat/pot upkeep |
| Teleport reachable (spell or item) | `Locations.teleports()` |
| Boss-specific gate met | GWD: **20 KC** for the teleport **and 15 faction kills** |

`score` rises with: combat level, gear tier, **banked wealth** (can afford to rebuy), food
surplus, and **archetype weight** (`bosser` ≫ `grinder`); it is damped when the bank is low
(a broke bot should farm, not boss).

A boss **run** is a `Sequence`: `Teleport → PrePot → FightLoop(eat/pot) → Loot → Bank →
return`. The `FightLoop` has a hard **bail predicate** — HP below a floor, food empty, or
another player entering a multi zone — that aborts to `chillHome`. For GWD the script is
longer: `Teleport(GWD) → KillFactionMinionsUntil(15) → EnterChamber → Boss → Loot → Bank`.

**Ordering in practice:** early game the bot never bosses (levels/gear fail the gate).
Bossing becomes the *dominant* score once the bot has ~full adamant/rune + a boss-capable
weapon + food income + teleports — i.e. the same "boss-ready" threshold the equipment
planner already computes.

---

## 7. When does a bot PvP?

**Answer: rarely, deliberately, and only with a disposable kit — plus a high archetype
weight.**

PvP is the one activity that **loses** wealth, so it must be gated conservatively:

| Check | Why |
| --- | --- |
| Archetype weight high (`pker`) **or** periodic "mood" roll | PvP is a choice, not a default |
| A **PvP kit** exists (cheap set + food + teleblock-escape) | never risk the boss gear |
| Banked wealth ≥ a buffer | can absorb a death |
| Combat level in a BH band (3–55 / 50–100 / 95+) | the crater refuses it otherwise |
| Kit ≤ 4 weapons, 1 body, 1 legs | `BountyHunter.checkReqs` |

Two routes, both from `teleports.cfg`:

- **Edgeville / PK teleports** → walk into the wilderness. `PK Edgeville 3089,3528` is the
  main gateway; the bot hunts in the level band it can survive, and **must not teleport
  above level 20** (`NO_TELEPORT_WILD_LEVEL`).
- **Bounty Hunter crater** — teleport to the craters, pick the band matching its combat
  level, then honour the rogue/penalty/target rules. On exit the server moves it to
  `3179,3685` (the BH hub) — a natural *home-adjacent* rest point.

**Hard fail-safe, above all else:** the bot **flees** on any of — HP below floor, out of
food, **teleblocked** with multiple attackers, or a skull above its risk tolerance. A PvP
bot that doesn't flee is just a loot piñata.

**Frequency:** in a mixed population, PvP should be the *least* frequent goal — a low
archetype weight plus a cooldown (e.g. at most once per N goals) keeps it from dominating.

---

## 8. When does a bot chill at home?

**Answer: whenever it has no pressing goal — and by design, sometimes on purpose.**

Home = **Edgeville** (`3087,3505`), the bank and shop mall. "Chill" is not idleness; it is
the **reset state**, and it is where several loops close:

| Trigger | What home does |
| --- | --- |
| Inventory full / loot to sell | bank + sell drops to shops |
| Out of food/pots/ammo | buy from the mall (`BOT_LOCATIONS.md` B.3) |
| Upgraded equipment becomes available | buy + equip the next tier |
| Just died | re-gear (respawn is Lumbridge, walk back) |
| Between goals | the arbiter's default when nothing scores |
| **Realism / pacing** | linger at the bank, wander a few tiles, "afk" briefly, then re-decide |

The last row matters for believability: a bot that teleports goal-to-goal with zero pause
reads as a machine. A short, jittered **home dwell** (`WaitTicks(random)` + `RandomTileIn`
near the bank) between outings is what makes the population look human — and it is cheap.

If the bot has **nothing** to do and no archetype pressure, `chillHome` is the arbiter's
fallback (`orElse(chillHome)`), so the default is always safe and always on-screen.

---

## 9. The decision table

| Signal | Goal the arbiter should pick |
| --- | --- |
| Inventory full | `chillHome` (bank/sell) |
| HP low, no food | `chillHome` (restock) |
| Available upgrade && affordable | `chillHome` (buy/equip) |
| Slayer task active && location available | `DO_SLAYER` |
| Boss gate met && geared && stocked | `BOSS` |
| Archetype `pker` && kit ready && off cooldown | `PVP` |
| No goal && archetype pressure | next `TRAIN_SKILL` in the plan |
| Nothing scores | `chillHome` (dwell, then re-decide) |
| Attacked in the wild / HP critical | `SURVIVE` → escape or `chillHome` |

---

## 10. Fail-safes (non-negotiable)

- **Never** take boss/PvP gear into the wilderness.
- **Always** bail a boss at the HP/food floor — a run that dies returns nothing.
- **Skip impossible tasks** — aberrant spectre (1604) and cave horror (4353) have no spawns
  (`BOT_LOCATIONS.md` C.1); the arbiter must mark them unavailable, or the bot loops forever.
- **Never** teleport above wilderness level 20.
- **Never** PvP with more than the crater's kit limit — the server refuses entry anyway.

---

## 11. How this hooks into the roadmap

- **Data** it reads: `Locations` (A), the equipment planner (B), the Slayer registry (C) —
  all in `BOT_LOCATIONS.md`.
- **Runtime phase:** this is **Phase E/F** (motivation + the bot decides). The activity
  scripts (Phase D) are the *what*; this arbiter is the *when*.
- **Editor:** archetype weights and the motivation list are authored data, so they belong
  in the Bot Workshop as a per-bot "personality" panel — one more generated form from the
  node registry (`BOT_WORKSHOP_UX.md` §5.1).

## Open items

Resolved this pass:

- ✅ **`killCount` source** — dead. Persisted and read, never incremented; the GWD
  `killCount >= 20` teleport object is unreachable, and the real entry is the teleport
  button → hole `2823` → `2881,5310` (see §4).
- ✅ **Boss loot tables** — verified for KBD, the three Dagannoth Kings, all four GWD
  bosses and KQ (see §4), with KQ's second-form-only drop noted.

Still open:

- **BH hub as a second home** — `3179,3685` (crater exit) is a natural rest point; decide
  whether "chill" can happen there or only at Edgeville.
- **Minigame value** — Barrows/Fight Caves rewards need a data pass before the arbiter can
  score them against bossing.
- **Boss *risk* scoring** — loot is now known; the arbiter still needs each boss's expected
  kill time and death risk to score value-vs-danger (uses the equipment planner).

## Acceptance criteria

- The arbiter picks the highest-scoring *available* motivation and falls back to
  `chillHome`, never to a stall.
- A bot blocks a boss it is not geared for, and a GWD run builds 15 faction kills before
  entering.
- A bot aborts a boss/PvP outing at the HP/food floor and returns home to restock.
- A bot refuses tasks with no spawns and never enters a BH crater above its combat band.
- Two bots with identical stats but different archetypes make visibly different choices.
