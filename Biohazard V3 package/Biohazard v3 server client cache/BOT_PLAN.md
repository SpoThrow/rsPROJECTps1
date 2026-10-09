# BOT_PLAN.md — Slice 1: chop trees, bank logs

Status: **design only, no code written yet.**
Scope: **slice 1 only.** One bot, one behaviour: walk to a tree, chop until the
inventory is full, walk to a bank, deposit the logs, repeat.

---

## 0. Scope and non-goals

### In scope

- A `BotPlayer` that is a real `Client` living in `PlayerHandler.players`, so it runs
  through the normal per-player tick and the normal walking/action code paths. It
  **possesses a real character** (load-or-create, save-on-release) and is driven by code
  instead of a socket — see §5.4.
- A minimal state machine: `BotState` returning a `Status`, with `Sequence`/`Repeat`
  composites that own all transitions.
- Exactly one high-level task: `Repeat(Sequence(WalkTo(tree), ChopTree, WalkTo(bank), BankLogs))`.
- Object interaction routed through the `ObjectHandler` registry via one `BotContext`
  helper. **No direct skill calls** (`Woodcutting.startWoodcutting` is never called by
  bot code).
- Sessionless-client safety (`flushOutStream` guard), bot identity, character
  persistence, and a hard bot-count cap.

### Out of scope (this phase)

- **No `Policy`, `Observation`, or `Action` types.** Those belong to the later ML/RL
  phase (see §8). Slice 1 is a hand-written state machine and nothing else.
- No combat, no death handling beyond "pause and reset", no pathing across planes, no
  multi-bot coordination, no region/jitter waypoints (slice 1 uses fixed tiles).
- No tick-rate change. `Server.cycleRate` stays `600`.

---

## 1. Why this shape

The read-only review established the seams this plan leans on:

- The game loop is single-threaded and owns all mutable game state:

```286:302:Proxy Server/src/server/Server.java
	private static void tick() {
		itemHandler.process();
		playerHandler.process();	
        npcHandler.process();
        shopHandler.process();
        CycleEventHandler.process();
		server.game.content.DwarfCannon.process();
		objectManager.process();
		//castlewars
		CastleWars.process();
		fightPits.process();
		pestControl.process();
		PlayerSaving.process();
	}
```

- Each player already has a per-tick hook, and `PlayerHandler` documents the order:

```131:152:Proxy Server/src/server/game/players/PlayerHandler.java
			// Pass 1: packets → timers/hits → merge walk → step → follow+swing
			for(int i = 0; i < Config.MAX_PLAYERS; i++) {
				Client player = players[i];
				if(player == null || !player.isActive) continue;
				try {
					if(shouldRelease(player)) {
						releasePlayer(i, player);
						continue;
					}
					player.preProcessing();
					while(player.processQueuedPackets());
					player.process();
					player.postProcessing();
					player.getNextPlayerMovement();
					player.processCombatAfterMovement();
```

  `process()` is the natural place to drive bot logic: `super.process()` keeps the
  normal timer/death/energy upkeep, and movement still happens afterwards through
  `getNextPlayerMovement()`.

- Object clicks already funnel through a registry, which is the correct place for a bot
  to enter:

```53:64:Proxy Server/src/server/game/players/actions/objects/ObjectHandler.java
	public static boolean dispatch(Client c, int objectType, ObjectClick click, int objectX, int objectY) {
		ObjectAction action = byClick.get(click).get(objectType);
		if (action == null) {
			return false;
		}
		action.handle(c, objectType, objectX, objectY);
		return true;
	}
```

- Timed repeat work already uses `CycleEventHandler`, so `ChopTree` gets tick-paced
  chopping "for free" once the object is clicked.

---

## 2. Amendments applied to the original design

These are the changes requested, and each is reflected in the sections below.

1. **`tick` returns a `Status`.** `BotState.tick(ctx)` returns `BotStatus`
   (`RUNNING`/`SUCCESS`/`FAILURE`); a state never names its successor. Composites
   (`Sequence`, `Repeat`) read child status and decide what runs next. See §3.
2. **No `Policy`/`Observation`/`Action`.** Removed from this phase entirely; recorded
   as a later-phase note in §8.
3. **Object interaction through the registry.** All clicks go through one helper,
   `BotContext.interactObject(...)`, which dispatches via `ObjectHandler.dispatch(...)`.
   Bot states contain **no** direct calls to skill classes. See §3.4.
4. **`flushOutStream` guard resets `outStream.currentOffset`.** A sessionless client
   must drop buffered bytes rather than let `outStream` grow forever or NPE on
   `session.write`. See §5.1.
5. **Reserved name prefix + persistence (amendment 5, revised).** Bots are named with a
   reserved prefix — a naming convention, **not** a save gate — and it must be
   **alphanumeric** (`bot`, not `[bot]`), because the server rejects punctuation in login
   names. They persist like normal players: the possessed character is written on release,
   autosave and shutdown, so the same account can also be logged into and played by a
   human. See §5.2, §5.4, and `BOT_ACCOUNTS.md`.
6. **Cap the bot count.** `BotManager.MAX_BOTS`, enforced at possess. See §5.3.
7. **Seven steps, each with a test, plus the RL prerequisite note.** See §6 and §8.

---

## 3. Core interfaces

### 3.1 `BotStatus`

```java
package server.game.bots;

/** Outcome of one tick of a state. A state never names its successor. */
public enum BotStatus {
    /** Still working; tick again next game tick. */
    RUNNING,
    /** This state's job is done. The owning composite decides what runs next. */
    SUCCESS,
    /** This state cannot finish. The owning composite decides retry/abort. */
    FAILURE
}
```

### 3.2 `BotState`

```java
package server.game.bots;

/**
 * One node of the bot's behaviour tree. Implementations keep their own progress and
 * report it through the return value; they must NOT choose the next state.
 */
public interface BotState {

    /** Called once, on the game thread, when this state becomes current. */
    void enter(BotContext ctx);

    /** One game tick. Return RUNNING to continue, SUCCESS/FAILURE to report done. */
    BotStatus tick(BotContext ctx);

    /**
     * Called once when this state stops being current.
     *
     * @param interrupted true when left before reporting SUCCESS or FAILURE
     *                    (death, reset, bot removed, shutdown) so it can release
     *                    charges, animations and CycleEvents.
     */
    void exit(BotContext ctx, boolean interrupted);
}
```

### 3.3 Composites own transitions

```java
package server.game.bots.composite;

/** Runs children in order; a child's SUCCESS advances, FAILURE aborts the sequence. */
public final class Sequence implements BotState { /* index + current child */ }

/** Re-runs a child (or a whole sequence); count < 0 means "forever". */
public final class Repeat implements BotState { /* child + remaining */ }
```

Rules, stated once so every later composite copies them:

- `Sequence` runs child `i` until it returns `SUCCESS`, then `enter`s child `i+1`.
  A child returning `FAILURE` makes the whole sequence `FAILURE` (the failed child's
  `exit(ctx, false)` still runs — it reported its own outcome).
- `Repeat` re-`enter`s its child on `SUCCESS`. With `count < 0` it never returns
  `RUNNING`-adjacent outcomes itself, i.e. a forever loop is the root and simply keeps
  ticking. A `FAILURE` propagates and ends the repeat.
- **Only composites call `enter`/`exit`.** Leaf states never transition themselves;
  that is the whole point of the `Status` return.

The slice-1 root is therefore:

```java
BotState root = new Repeat(new Sequence(
        new WalkTo(treeX, treeY, ARRIVE_RANGE),
        new ChopTree(treeId, LOG_ID),
        new WalkTo(bankX, bankY, 1),
        new BankLogs(LOG_ID)),
        -1 /* forever */);
```

### 3.4 `BotContext`

`BotContext` is the only thing a `BotState` sees. It exposes the client, the per-state
tick counter, and a small set of **intent helpers**. It never leaks "which state am I in"
back to the state machine core.

```java
package server.game.bots;

public final class BotContext {

    private final BotPlayer bot;

    // --- observation (reads only) ---
    public BotPlayer client();
    public int x();
    public int y();
    public int height();
    public boolean arrivedAt(int x, int y, int range);
    public boolean isIdle();          // walk queue drained and no active skill session
    public int freeSlots();
    public boolean hasItem(int itemId);
    public int ticksInState();
    public int random(int bound);

    // --- intent (actions; each is the single entry point for that action) ---
    public void walkTo(int x, int y);
    public boolean interactObject(int objectId, int x, int y, ObjectClick click, int range);
    public void openBank();
    public boolean depositItem(int itemId);

    // --- lifecycle, used only by BotPlayer when a root state ends ---
    void onStateEntered();
    void onTick();
}
```

**`interactObject` is the single object-interaction helper** (amendment 3). Its
contract:

1. Set the packet-equivalent fields on the client (`clickObjectType`, `objectX`,
   `objectY`, `objectId`) that the migrated `ObjectAction`s read.
2. Confirm the bot is within `range` of the object (`goodDistance`); if not, return
   `false` and let the caller's `WalkTo` retry.
3. Dispatch through the **registry**:

```java
if (ObjectHandler.dispatch(bot, objectId, click, x, y)) {
    return true;
}
```

4. If the registry does not claim the object (not yet migrated), fall through to the
   matching `ActionHandler` entry point (`firstClickObject`/`secondClickObject`/
   `thirdClickObject`), which is the same fall-through order
   `ActionHandler` itself uses.

Consequence: **bot code never calls a skill class.** `ChopTree` calls
`ctx.interactObject(1276, treeX, treeY, ObjectClick.FIRST, 3)`; the registry then
invokes `Woodcutting.startWoodcutting` exactly as a real click would. A newly migrated
skill becomes bot-usable with zero bot changes.

---

## 4. File layout

New package `server.game.bots` and `server.game.bots.composite` / `.states`:

```
Proxy Server/src/server/game/bots/
├── BotPlayer.java        extends Client; drives the state machine from process()
├── BotManager.java       possess/release/cap; the only entry point for creating bots
├── BotController.java    attaches a state machine to a BotPlayer (§5.4)
├── BotContext.java       the single surface a state sees (§3.4)
├── BotState.java         interface (§3.2)
├── BotStatus.java        enum (§3.1)
├── BotNames.java         reserved prefix + name generation (§5.2)
├── composite/
│   ├── Sequence.java
│   └── Repeat.java
└── states/
    ├── WalkTo.java
    ├── ChopTree.java
    ├── BankLogs.java
    └── WaitTicks.java    utility leaf used by the tests and by retry delays

Proxy Server/test/server/game/bots/
├── SessionlessFlushTest.java
├── BotPersistenceTest.java
├── BotManagerCapTest.java
├── BotStateMachineTest.java
├── WalkToStateTest.java
├── ChopTreeStateTest.java
└── ChopBankLoopTest.java
```

Modified core files (small, contained):

- `server/game/players/Client.java` — `flushOutStream` sessionless guard (§5.1).
- `server/game/players/Player.java` — add `public boolean isBot = false;` (§5.2).
- `server/game/players/PlayerHandler.java` — a sessionless registration overload for
  bots, since `newPlayerClient` reads `getSession().getRemoteAddress()` (§5.4).
- `server/Server.java` — one `BotManager.start()` call in `main` (optional; see §6, step 3).

**No save path is modified.** Bots persist like players, so `PlayerSave`,
`PlayerSaving` and `saveAllPlayers` need no bot-specific branches (revision of
amendment 5 — see §5.2).

---

## 5. Cross-cutting mechanics

### 5.1 `flushOutStream` guard (amendment 4)

Today the method writes to a session that a bot does not have:

```147:158:Proxy Server/src/server/game/players/Client.java
	public void flushOutStream() {	
		if(disconnected || outStream.currentOffset == 0) return;
		synchronized(this) {	
			StaticPacketBuilder out = new StaticPacketBuilder().setBare(true);
			byte[] temp = new byte[outStream.currentOffset]; 
			System.arraycopy(outStream.buffer, 0, temp, 0, temp.length);
			out.addBytes(temp);
			session.write(out.toPacket());
			outStream.currentOffset = 0;
		}
	}
```

Required behaviour: when `session == null`, **drop the buffered bytes by resetting
`outStream.currentOffset = 0`** (not merely return). Returning without resetting would
let every `sendMessage`/`addItem` frame accumulate in `outStream.buffer` without bound
— a slow leak that eventually overruns the 10000-byte `Config.BUFFER_SIZE` buffer.

```java
public void flushOutStream() {
    if (disconnected) return;
    synchronized (this) {
        if (outStream == null || outStream.currentOffset == 0) return;
        if (session == null) {
            // Sessionless client (a bot): discard what would have been sent so the
            // buffer cannot grow without bound, and never touch the session.
            outStream.currentOffset = 0;
            return;
        }
        StaticPacketBuilder out = new StaticPacketBuilder().setBare(true);
        byte[] temp = new byte[outStream.currentOffset];
        System.arraycopy(outStream.buffer, 0, temp, 0, temp.length);
        out.addBytes(temp);
        session.write(out.toPacket());
        outStream.currentOffset = 0;
    }
}
```

`BotPlayer` additionally overrides `update()` as a no-op and keeps `initialize()`
minimal, so the normal tick does no packet work; the guard is the safety net for the
helpers that do write frames (`sendMessage`, `getItems().addItem`, `openUpBank`).

### 5.2 Bot identity and persistence (amendment 5, revised)

> **Revision.** The first draft of this section excluded bots from *all* saving and made
> the reserved prefix a hard rule. That is **superseded** by the possession model in
> §5.4: a bot is a real character, its progress must persist, and its owner must be able
> to log in and play it later. Bots therefore **save exactly like normal players.** The
> only part of amendment 5 that survives is the naming convention, and it is no longer
> a saving rule.

- **Reserved prefix.** `BotNames.PREFIX = "bot"` (lowercase, **no punctuation**). The
  earlier `[bot]` prefix is **illegal**: the server rejects any login name outside
  `[A-Za-z0-9 ]` (`returnCode = 4`), so a `[bot]Name` account could never be logged into
  and would defeat the possession model. It is an **account-naming convention** — bot
  accounts are recognisable in logs and in `Data/characters/` — **not** a saving
  exclusion. Full rules in `BOT_ACCOUNTS.md`.
- **`isBot` flag.** `public boolean isBot = false;` on `Player`, set true by
  `BotPlayer`. Used for observability (`::bot` commands, tracing, the editor's live
  view) and, if desired, to keep bots off the hiscores — **never** to block persistence.
- **Persistence.** A bot is written through the ordinary path, with the same flags login
  sets (`saveFile = true`, `saveCharacter = true`, `newPlayer = false`):
  - `Client.saveCharacterOnce()` → `PlayerSave.saveGame` writes
    `./Data/characters/<name>.txt`, unchanged.
  - The periodic autosave (`PlayerSaving.process`) includes bots, unchanged.
  - The shutdown sweep (`PlayerHandler.saveAllPlayers`) includes bots, unchanged.
  - On release from possession, the controller calls `saveCharacterOnce()` exactly as a
    logout would (§5.4).
- **Log in and play it yourself.** Because the file is an ordinary character file,
  releasing the possession (which saves) and then connecting a normal client to that
  account works with **no new machinery**. While a bot is possessed the account is in
  `PlayerHandler.players`, so a second login is refused by the existing duplicate-name
  check — one controller at a time, which is the correct behaviour.

The load/create contract a possession uses is the same one login uses:

```java
int load = PlayerSave.loadGame(cl, name, pass);
// 0  = no file -> NEW account (set addStarter, let the world initialise it)
// 3  = wrong password -> refuse the possession
// 13 = truncated file -> refuse to persist (saveFile = false)
// otherwise -> loaded; saveFile = true
```

### 5.3 Bot cap (amendment 6)

`BotManager.MAX_BOTS = 10` (well under `Config.MAX_PLAYERS = 50`, leaving headroom for
real players and for the 1-slot-per-bot cost). `BotManager.possess(...)`:

- returns `null` and logs once when `liveBots.size() >= MAX_BOTS`;
- refuses when no free slot exists in `PlayerHandler.players`;
- refuses a duplicate name (a character already in the world, bot or human).

Bot count is tracked on `BotManager`, not derived by scanning the player array, so the
cap is O(1) and cannot be defeated by a bot that is temporarily `!isActive`.

### 5.4 The possession lifecycle

A bot is not a second kind of entity. It is a **controller attached to a real
character**: `Client` is the character in the world, `BotController` is the script
driving it.

**Open — `BotManager.possess(name, script)`:**

1. Create a sessionless `Client` (no socket). This is an already-supported shape — the
   tests build one with `new Client(null, slot)`.
2. Load or create the character with `PlayerSave.loadGame(cl, name, pass)`; handle the
   return codes per §5.2 (`0` new, `3` bad password, `13` truncated). Account **creation**
   is its own flow — see `BOT_ACCOUNTS.md` (it must *not* use `addStarter`).
3. Register the client in `PlayerHandler.players` through a sessionless registration
   overload. The existing `newPlayerClient` cannot be used unchanged — it reads
   `((InetSocketAddress) client1.getSession().getRemoteAddress())` — so the overload sets
   `connectedFrom` to a bot marker instead.
4. Attach the `BotController`; the root `BotState` starts on the next tick.

**Close — `BotManager.release(name)`:**

1. `root.exit(ctx, true)`, so charges, animations and `CycleEvent`s are released.
2. `saveCharacterOnce()` — the character reaches disk.
3. Remove from `PlayerHandler.players`.

**Why sessionless is fine.** A bot needs no socket: nothing is watching its frames. The
single behavioural difference is outgoing traffic, and the `flushOutStream` guard in
§5.1 handles it. Everything else — position, skills, inventory, walking, collision,
saving — is identical to a human player. That is precisely the "no shortcuts" property
you asked for: **the bot is a player in the world, driven by code instead of a socket.**

**Movement is real, by construction.** `BotContext.walkTo` goes through
`PathFinder.findRoute` → `addToWalkingQueue`, and the player advances **one tile per
tick** in `getNextPlayerMovement`, with collision via `SmartPathFinder.canStep`. No bot
state can move a player instantly: the only such API is the teleport system
(`movePlayer` / `startTeleport`), which the bot layer simply does not expose. "Walk
there in real time" is the default, not a rule that has to be enforced.

---

## 6. The seven steps, each with a test

Each step is independently reviewable and leaves the tree green. Ordered so the
riskiest enabling changes land first.

### Step 1 — Sessionless client safety

**Change.** Add the `flushOutStream` guard from §5.1.
**Test** — `test/server/game/bots/SessionlessFlushTest.java`:
- `new Client(null, 1)`, write bytes via `sendMessage`, call `flushOutStream()`:
  no exception, and `getOutStream().currentOffset == 0`.
- Repeat 1000× and assert `currentOffset == 0` each time (no growth / no overflow past
  `Config.BUFFER_SIZE`).
- A `disconnected` client with buffered bytes still returns before touching anything.

### Step 2 — Bot identity and persistence

**Change.** Add `Player.isBot`; add `BotNames`; add the `BotPlayer` skeleton with
`isBot=true`, overridden `update()` (no-op) and the possession flags a bot needs
(`saveFile`/`saveCharacter` true, `newPlayer` false once loaded). **No save path is
touched** — bots persist like players (§5.2).
**Test** — `test/server/game/bots/BotPersistenceTest.java` (mirrors
`PlayerLifecycleTest`'s fixture, which already builds sessionless clients and points
hiscores at a dead local port):
- `saveGame(bot)` returns `true` and `./Data/characters/<bot>.txt` appears; cleaning up
  deletes it (the same pattern `PlayerLifecycleTest` uses).
- `PlayerHandler.saveAllPlayers()` writes the bot (returns `>= 1`).
- Drive `PlayerSaving.process(now)` past `SAVE_INTERVAL_MS` and assert the bot is
  included, not skipped.
- `isBot == true` does **not** change any of the above — it is a label, not a gate.

### Step 3 — `BotManager`, possess/release, cap

**Change.** `BotManager` with `MAX_BOTS`, `possess(name, script)`, `release(name)`,
`all()`, and a `start()` that is a no-op until slice-1 wiring (keeps `Server.main`
untouched if you prefer). `possess` performs the §5.4 lifecycle: load-or-create the
character, register the client through the **sessionless** `PlayerHandler` overload
(the stock `newPlayerClient` needs a session for `connectedFrom`), attach the
controller.
**Test** — `test/server/game/bots/BotManagerCapTest.java`:
- `possess` up to `MAX_BOTS` succeeds; the next `possess` returns `null` and adds
  nothing.
- Every possessed bot has the reserved prefix, `isBot == true`, `isActive == true`, and
  occupies a distinct slot.
- Duplicate names are refused — including a name already in `PlayerHandler.players`.
- **Round-trip:** `possess` → mutate state (e.g. move the bot, add an item) → `release`
  → assert the character file exists and `PlayerSave.loadGame` returns the mutated
  state; then `possess` again and assert state is intact.

### Step 4 — `BotContext`, `BotState`/`BotStatus`, composites

**Change.** The interfaces in §3 plus `Sequence` and `Repeat`.
**Test** — `test/server/game/bots/BotStateMachineTest.java`, using fake states that
return scripted statuses (no client needed):
- `Sequence(A,B,C)`: A=SUCCESS → B runs; B=FAILURE → whole sequence FAILURE and C never
  `enter`s; `exit(ctx,false)` is called on B.
- A child returning `RUNNING` keeps the same child current across ticks.
- `Repeat(A, 3)` ticks A to SUCCESS exactly three times, then returns SUCCESS.
- `Repeat(A, -1)` never reports done.
- `enter`/`exit` are called exactly once per activation, and `exit(...,true)` fires when
  a composite is abandoned early.

### Step 5 — `WalkTo`

**Change.** `WalkTo(x, y, range)` — `enter` calls `ctx.walkTo`; `tick` returns
`SUCCESS` when `ctx.arrivedAt(x,y,range)` and `isIdle()`, `RUNNING` otherwise, and
`FAILURE` after a tick budget with no net movement (stuck detection via a
last-position/last-progress counter, the same idea as `WorldAdventurer.stuckTicks`).
**Test** — `test/server/game/bots/WalkToStateTest.java`:
- Put a bot in `PlayerHandler.players`, call `enter`, then loop
  `c.getNextPlayerMovement()` until `tick` returns SUCCESS; assert the bot reached the
  destination within the budget.
- Blocked destination (clip the single step via `Region.tempClip`, or aim at an
  unreachable tile) → `tick` eventually returns FAILURE, not an infinite RUNNING.

### Step 6 — `ChopTree` via the registry

**Change.** `ChopTree(treeId, logItemId)`. `enter` calls
`ctx.interactObject(treeId, treeX, treeY, ObjectClick.FIRST, 3)` (the tree click
distance the packet handler uses). `tick` returns `SUCCESS` once
`ctx.freeSlots() == 0`, `FAILURE` if the chop session never starts or the tree dies,
and re-issues the interaction if `woodcutting.active` goes false before the bag is full.
**No `Woodcutting` reference appears in this class.**
**Test** — `test/server/game/bots/ChopTreeStateTest.java`:
- Bot holding a bronze axe (1351) and empty slots: `enter` → assert
  `c.woodcutting.active == true` (proving the registry, not a direct call, started it).
- Run `CycleEventHandler.process()` N times: logs appear (`hasItem(LOG_ID)`), the
  session ends (`active == false`) and `tick` returns SUCCESS when the bag is full.
- A registered-but-unstartable case (bot with no axe) returns FAILURE rather than
  spinning.
- **Regression pin:** assert `ObjectHandler.isRegistered(treeId, ObjectClick.FIRST)`
  is true, so if the registry entry is ever removed the bot test fails loudly instead of
  silently falling through.

### Step 7 — `BankLogs` and the full loop

**Change.** `BankLogs(logItemId)`: `enter` calls `ctx.openBank()`; `tick` deposits every
inventory slot holding the log (`ctx.depositItem`) and returns `SUCCESS` when none
remain, `FAILURE` after a budget. Then wire the root
`Repeat(Sequence(WalkTo(tree), ChopTree, WalkTo(bank), BankLogs), -1)` into
`BotPlayer.process()`.
**Test** — `test/server/game/bots/ChopBankLoopTest.java` (the end-to-end vertical
slice, still no network):
- One bot, one tree, one bank tile. Drive the game tick manually
  (`Server`'s tick body is private, so call the same handler sequence the test needs:
  `playerHandler.process()` + `CycleEventHandler.process()` in a loop with a tick cap).
- Assert, in order: bot walks to the tree → `woodcutting.active` becomes true → logs
  fill the inventory → bot walks to the bank → `isBanking` becomes true → inventory has
  no logs → the loop re-enters `WalkTo` (proving `Repeat` re-entered the sequence).
- Assert the bot's character persists: `release` writes `./Data/characters/<name>.txt`,
  and the bot stays within `MAX_BOTS` while possessed.
- Assert the whole run completes inside a generous tick cap (guards against a state
  that returns RUNNING forever).

---

## 7. Risks and gaps specific to this slice

- **Random events.** Chopping can fire `SpiritTree` (and bird nests fill slots). A leaf
  state must treat "session ended unexpectedly" as recoverable, not as failure-forever:
  `ChopTree` re-issues the interaction while slots remain. (Covered in step 6's tick
  logic.)
- **`cutDownTree` cancels every session at that tile** — including other bots'. Slice 1
  runs one bot, so this is noted for the multi-bot phase, not fixed here.
- **Wall-clock cooldowns.** Skills and the walking loop consult
  `System.currentTimeMillis()` thresholds (`timers.foodDelay`, etc.). Slice 1 only
  chops and banks, which have no such gate, so this is deferred — but it is the
  blocker for RL (see §8).
- **`openUpBank` calls `resetVariables()`**, which clears transient click fields. States
  must re-derive what they need after opening the bank rather than caching click
  context across it.
- **`initialize()` on first tick.** A freshly inserted bot gets `initialize()` in pass 2.
  Keep `addStarter=false` and `canWalk=true`, and override `update()` as a no-op so the
  normal tick does no packet work.
- **No spatial index.** Slice 1 uses fixed tree/bank tiles, so no scanning is needed;
  scanning becomes a concern only when the bot must "find the nearest tree" (later).

---

## 8. Later phase (not now): RL requires tick-based timers first

The eventual ML/RL bridge (`Policy { Action decide(Observation) }`, per the review) is
**deliberately out of scope for slice 1**. When that phase starts, one prerequisite
comes before any RL work:

> **Convert the game's wall-clock timers to tick counts before building an RL bridge.**

Reinforcement learning assumes a fixed-timestep, deterministic environment: the agent
observes, acts, and the world advances by exactly one step. This codebase mixes two
timer families, and an RL step cannot straddle both:

- **Tick countdowns** (decrement once per tick): `attackTimer`, `teleTimer`,
  `hitDelay`, `freezeTimer`, `respawnTimer`, `skullTimer`, `clawDelay`, `ssDelay`.
- **Wall-clock stamps** (compared against `System.currentTimeMillis()`):
  `foodDelay` (2000 ms), `potDelay`, `singleCombatDelay`/`singleCombatDelay2`
  (3300 ms), `logoutDelay` (10000 ms), `restoreStatsDelay` (60000 ms), `alchDelay`,
  `duelDelay` (800 ms), and others in `Timers`.
- Plus `Server.cycleRate = 600` (ms), the single constant tying the two together.

An RL step defined as "one tick" therefore produces non-stationary observations
whenever a wall-clock timer is in play, and an RL step defined as "N milliseconds" does
not align with the tick the server actually runs. The fix is a later, isolated
refactor: express every entry in `Timers` in ticks, make the tick the sole unit of game
time, and only then expose the `Observation`/`Action` surface. That refactor is its own
change with its own tests and must land **before** `Policy`/`Observation`/`Action` are
introduced.

---

## 9. Acceptance criteria

- One bot possesses a real character, chops to a full inventory, walks to a bank,
  banks the logs, and repeats — verifiably, with no network session.
- The possessed character **persists**: `release` writes its file, and it can be
  re-possessed (or logged into with a normal client) with state intact.
- No bot code calls a skill class directly; chopping goes through
  `ObjectHandler.dispatch`.
- `flushOutStream` on a sessionless client never throws and never grows the buffer.
- `BotManager` refuses to exceed `MAX_BOTS` or `Config.MAX_PLAYERS`, and refuses a name
  already in the world.
- Every step in §6 has a green test; `PlayerLifecycleTest`, `WoodcuttingObjectsTest`
  and `CycleEventHandlerTest` still pass unchanged.
