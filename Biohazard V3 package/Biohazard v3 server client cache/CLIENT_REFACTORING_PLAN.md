# Client Refactoring Plan

**Scope:** `Proxy Client/` only. The server is **out of scope** — it is treated as a fixed contract (see *The one non-negotiable*). Mirror of `REFACTORING_PLAN.md`, which covers `Proxy Server/`.

---

## 📍 Current status (update this block after every working session)

**Last updated:** 2026-10-04 · **Phase:** 0 — plan written, nothing executed yet. This document is new; no client step has been started.

**Last thing I did:** Wrote this plan. Before writing it I measured the client rather than trusting the brief, and **two findings changed the shape of the work**, so they are stated up front:

1. ⚠️ **The client already runs on Java 25 — only the *compile target* is ancient.** `Run.bat` launches a bare `java`, and the default JDK on this machine is **25.0.4**, so the game is already running on a modern JVM (and it works — it was played during the server session). The only thing stuck in the past is `Compile.bat`'s `-source 1.7 -target 1.7`, and that still works **only because `Compile.bat` hardcodes JDK 8's `javac`** (`C:\Program Files\Java\jdk1.8.0_202\bin\javac.exe`). Plain `javac` on PATH is 25, which **rejects `-source 1.7` outright** (support for source/target 7 was removed in JDK 21). So the build works by accident of two different JVMs being involved, and the Java upgrade is a *build* problem, not a *runtime survival* problem.
2. ⚠️ **Java version has literally nothing to do with server compatibility.** The user's stated top priority is "no issues with compatibility with the server". The client and server are **separate JVMs talking over a socket**, so their Java versions need not match and cannot conflict. The real contract is the **wire protocol and the cache format** — see below. This matters because it means the riskiest-sounding item (the Java upgrade) is *not* where the server risk lives, and the item that *is* risky (anything touching packet or cache reading) is small and testable.
3. **`openGlEnabled` is not OpenGL.** The client has no 3D GPU path at all. `openGlEnabled`/`GlPresent` only switch on Java2D's `sun.java2d.opengl` pipeline — i.e. accelerated **blitting of a software-rasterised framebuffer**. The `LWJGL` jar in `deps/` is **LWJGL 2.x and is imported by zero source files**.
4. ⚠️ **LWJGL + AWT parenting was already tried here and deliberately abandoned.** `LwjglPresent.java` is a stub whose every method returns `false`, with this comment: *"LWJGL `Display.setParent` is not used. Parenting a native GL window onto the AWT applet deadlocks Windows when the frame loses or gains focus (both AWT and LWJGL pump the same HWND)."* **Any OpenGL plan that begins with `Display.setParent` is re-walking a known dead end.** Phase 7 is built around that lesson.
5. **One of the two reference repos the user supplied is effectively empty.** `ubjelly/317-OpenGL` contains three files (~2.7 KB total) and no renderer. `Elvarg-Client-Public` is a full RuneLite client and is the only usable reference. Details in the review section.
6. ⚠️⚠️ **The client's committed state (`HEAD`) cannot compile — the working tree is load-bearing.** This is the most urgent thing in this document. `client.java` is **unmodified vs `HEAD`** yet references **`LwjglPresent` and `OverlayRefresh`**, and **neither is tracked by git at all**. In total the client has **seven untracked classes that tracked source depends on**, and they are heavily used: `OverlayRefresh` is referenced by **39 files**, `GroundItemLists` by **20**, `ChatboxItemSearch` by **18**, `HiscoresPanel` by **6**, `CurseData667` by **3**, `LwjglPresent` (by `client.java`) and `JavaUncompress` by **1** each. **A fresh clone, `git clean -fd`, or `git checkout .` therefore destroys the client build outright.** This is the same "untracked-until-lost" pattern the server plan already recorded as having destroyed two cluster tools — it has now hit the client, and here it is not a tool but the product. `Phase 0.6` commits them. ⚠️ **The server is *not* affected:** `ConnectionPool` (untracked `server/util/`) and `MusicState` (untracked) are referenced **only from files that are themselves modified in the working tree**, so the server's `HEAD` still compiles. The asymmetry is worth remembering before assuming the two trees are in the same state.

**Next step:** **Phase 0 — the safety net, before anything else, and it now has an urgent first action.** (a) ⚠️⚠️ **Item 0.6 first: commit the seven untracked load-bearing classes** — `HEAD` cannot compile the client without them and a `git clean` would destroy the build (finding 6). Do this before any refactoring touches the tree. (b) Then back up `Proxy Client/` (mirroring the server's item 0.1: the backup goes *before* the first build that can change output). (c) Record a byte-exact baseline of `bin/`, and (d) capture a **golden-master log of a live login/movement/combat session** (packet bytes and a screen recording) — because the entire plan's risk is "something about talking to the server changed", and that is only detectable against a recorded baseline. ⚠️ **Nothing in Phase 1+ may start until that baseline exists**, for the same reason the server plan's charset/save-format items are gated.

**Done:** **Phase 0.6 — the seven untracked load-bearing client classes are now tracked** (commit `f3ec2e1`, 7 files / 1,907 insertions, source byte-identical, no deletions). `HEAD` can now compile the client. Done nothing else yet.

**In progress:** nothing.

**Not started:** Phases 0–8.

**Verification:** ⚠️ **The client has *no* test infrastructure at all** — no `*Test.java`, no build file, no CI. `Compile.bat` is the whole build system and it only compiles. This is the single biggest structural difference from the server (which has 345 tests). **Phase 0 must establish a way to test the client headlessly**, and Phase 1 must establish a build that can run tests, or every later phase is unverifiable except by playing the game.

**Known broken / half-finished (found while writing this plan, not yet actioned):**
- ⚠️⚠️ **`HEAD` cannot compile the client — seven required classes are untracked.** `client.java` is unmodified vs `HEAD` but references the untracked `LwjglPresent` and `OverlayRefresh`; six untracked classes are referenced across the tree (`OverlayRefresh` 39 files, `GroundItemLists` 20, `ChatboxItemSearch` 18, `HiscoresPanel` 6, `CurseData667` 3, `JavaUncompress` 1). **A fresh clone or `git clean -fd` destroys the client build.** See finding 6 → **Phase 0.6**. (The server's `HEAD` is unaffected — verified.)
- ⚠️ **A malformed noted-item definition hard-crashes the client** — `ArrayIndexOutOfBoundsException: Index -1 ... ItemDef.forID`. Reported live on 2026-10-04 while ranging Rock Crabs. `ItemDef.readValues` takes `certTemplateID` from opcode **98** and `certID` from opcode **97** *separately* and leaves both at `-1`; an entry with 98 but no 97 makes `forID` call `toNote()`, which calls `forID(certID)` = `forID(-1)`, and line 178 does `streamIndices[-1]`. **This is pre-existing and unrelated to the server refactor** (the server cannot send it: every ground-item send passes `ItemHandler.createGroundItem`'s `if (itemId > 0)` guard, and `ItemAssistant.dropAllItems`' `-1` is filtered there). It is scheduled as **Phase 2.1** — the user already asked for this hardening.
- ⚠️ **The client's `-source 1.7` build is one PATH change away from failure** (see finding 1). `Compile.bat` currently survives only via its hardcoded JDK 8 path.
- ⚠️ **`Proxy Client/src/ObjectCollisionSizes.java` is a *generated* file** that must stay in sync with `Proxy Server/Data/objectSize.cfg` via `Proxy Client/tools/generate-object-collision-sizes.ps1`. Editing the cfg without regenerating silently drifts client collision from server collision. The generator validates that no footprint exceeds 4 bits per axis.
- ⚠️ **`deps/lwjgl.jar` (LWJGL 2.x, ~1.05 MB) and `deps/lwjgl-platform-natives-windows.jar` are dead weight** — no source imports `org.lwjgl`. Phase 8 decides whether LWJGL 3 replaces them (Phase 7) or they go.
- ⚠️ **`Proxy Client/dump4/` (dated 2014) and `Proxy Client/jdt-bin/` are stale scratch/build output** — Phase 8 cleanup.

---

## The rule this repo must keep (mirrors `.cursor/rules/refactor-progress.mdc`)

- At the start of every task, read the "Current status" block above and continue from "Next step".
- After each completed step, update that block: "Last updated", "Last thing I did", "Next step", and any counts you verified.
- Before stopping mid-task, update the block first.
- Edit only the status block and the relevant phase section. Never rewrite the rest of the file.
- **Never mark a step done until it has been verified** — and for this client that means *verified on a live client against the real server*, not merely "it compiles".
- **Flag a phase the moment it is blocked or contradicted**, in the phase section and in the status block. A phase that turns out to be wrong is more useful recorded than silently dropped.

---

## The one non-negotiable: compatibility with the server

The user's stated highest priority. Restated precisely so it is testable rather than a feeling:

**The client must always be able to log into and play the *current, unmodified* server.** Concretely, the following are **frozen contracts** and every phase must leave them byte-identical unless the phase is explicitly about them:

| Contract | Where it lives | Why it breaks compatibility |
|---|---|---|
| **Packet opcodes and their payload layouts** | `client.java`'s incoming-packet switch, and `Stream` read helpers | A reordered read desyncs the stream and every later packet is garbage. This is the single most dangerous surface in the whole plan. |
| **Login handshake** (ISAAC seeds, `CreateUID`/UKEY, the login block) | `client.java`, `ISAACRandomGen`, `CreateUID` | A mismatch = "invalid login" with no useful error. |
| **Cache format and indices** (474 cache; archive/file numbering; `Skins.dat`, `Frames.dat`, loose `*.dat` frames) | `StreamLoader`, `Decompressor`, `Class36`, `OnDemandFetcher` | The client reads what the server/cache serves. Renumbering a single index silently loads the wrong model. |
| **On-demand fetching** (`OnDemandFetcher` requests vs what the server answers) | `OnDemandFetcher` | Desyncs model/anim loading mid-game. |
| **Object collision footprints** | `ObjectCollisionSizes` (generated) vs `Data/objectSize.cfg` | Client and server must agree or players clip through/path into scenery inconsistently. |

**Consequences for how this plan is written:**
- ⚠️ **Java version is *not* on this list, deliberately.** It cannot be — separate JVMs. Do not let the version upgrade be treated as risky to the server.
- ⚠️ **No phase may "tidy up" a packet read.** Refactors of `client.java` must be *purely mechanical and behaviour-preserving* (rename/move), with the packet switch treated as frozen code that moves but never changes.
- ⚠️ **Cache-reading changes are the other frozen surface.** Phase 3 *adds bounds checks around* reads; it does not change what is read or in what order.
- The client must be regression-tested against the **same server build** before and after each phase, using the Phase 0 golden-master baseline.

---

## Reference review (what I actually found, not what the brief assumed)

### `ubjelly/317-OpenGL` — ⚠️ not usable
The entire repository is **three files**: `src/org/derithium/Client.java` (1,647 B), `Game.java` (215 B), `ImageProducer.java` (864 B), plus compiled copies in `bin/`. There is no renderer, no shader, no GL code — it is an abandoned skeleton of a structure (`org.derithium`) with nothing in it. **Nothing can be learned from it beyond "someone once intended a `Game`/`ImageProducer` split".** Do not spend further time on it.

### `Elvarg-Community/Elvarg-Client-Public` — the real reference, but **not portable**
This is a **full RuneLite client** (Gradle Kotlin build; **1,156** Java files: **1,085** under `net/runelite/`, **209** under `com/runescape/`; "Current Data: 206"; README lists Textures, Model Class, HD, GPU, RuneLite as its features). Relevant architecture:

- `net/runelite/client/plugins/gpu/GpuPlugin.java` — RuneLite's official GPU plugin; `Shader.java`, `GpuFloatBuffer.java`, `GpuIntBuffer.java`.
- `net/runelite/client/plugins/hd/…` — the HD plugin: `model/ModelPusher.java`, `model/ModelHasher.java`, `model/ModelCache.java`, `opengl/shader/Shader.java`, `utils/ModelHash.java`.
- `com/runescape/entity/model/` — clean `Model`, `ModelHeader`, `ModelLoader`, `Renderable`.
- `com/runescape/cache/anim/Frame.java`, `FrameBase.java` — the animation transform structures.

⚠️ **It cannot be dropped in or cherry-picked wholesale.** It is a RuneLite client: named packages, a mixin/API layer, a plugin host, and a different cache/data revision. Our client is a **flat default-package 317/474 client of 131 files and 59,694 lines**. The value is as a **technique blueprint** — specifically its *seam*: it renders the 3D scene through GL and then composites the software-drawn UI over the top, rather than trying to GPU everything.

### Verdict on the user's three suggested prompts

- **Prompt 1 (GpuRenderer + LWJGL window over the native frame hook)** — ✅ **the abstract-`GpuRenderer` idea is right, ❌ the "bind an LWJGL display over the client's native frame hook" part is the known dead end.** `Display.setParent` was already attempted in this exact codebase and abandoned for deadlocking Windows (see `LwjglPresent`). Phase 6–7 keep the abstraction but use **LWJGL 3's `AWTGLCanvas`** (a single AWT component, the supported path) or a headless offscreen context — **never `Display.setParent`**.
- **Prompt 2 (Model → vertex arrays / VBO)** — ✅ correct and safe **as an additive phase**. Phase 5 exposes geometry; it must not alter the software rasteriser's output.
- **Prompt 3 (474 animation transforms must survive GPU rendering)** — ✅ correct and important, and the resolution is the one RuneLite actually uses: **apply the per-frame transform on the CPU (as `Model.method443`/`Animable` already do) and upload the transformed vertices**, rather than moving skinning into a shader. Our frames are `Class36` (`Skins.dat`, `Frames.dat`, loose frame files, plus `load_647`) and skins are `Class18`. See Phase 6.

### The LWJGL lesson, recorded once so it is not relearned
- `deps/lwjgl.jar` is **LWJGL 2.x** (`org/lwjgl/WindowsSysImplementation`, `LWJGLUtil`, `BufferChecks`) and is **imported by zero files**.
- ⚠️ **`Display.setParent` + AWT = deadlock on Windows.** Both toolkits pump the same HWND. This is why `LwjglPresent` is a stub.
- **The recommendation for Phase 7 is LWJGL 3** (`org.lwjgl.opengl.awt.AWTGLCanvas`), which supports Java 8+ and has a supported AWT embedding, and is a different design from LWJGL 2 rather than an upgrade of it.

---

## Architecture as it stands (the baseline this plan must not break)

- **Entry:** `Loader` (Swing chooser: *474 characters* vs *634 characters*) → `client.main(new String[]{"474"})`.
- **Shell:** `RSApplet extends Applet implements Runnable` + `RSFrame`; full window/listener/input plumbing.
- **Game loop:** `RSApplet.run()` — the classic 317 loop (`delayTime = 20` → 50 logic ticks/s via `processGameLoop`, `minDelay = 1`), **plus an `fpsUnlocked` uncapped branch** (`while (now - lastLogicTime >= 20 && ticks < 8)`) that draws every frame. Rendering is `processDrawing()`.
- **Renderer:** pure **software** — `RSImageProducer` wraps `int[]` pixels; `DrawingArea` does primitives; `Texture.java` (2,016 lines) does software texture mapping; **`Model.java` (2,823 lines) is the software rasteriser** (`method443` applies transform, `method478` scale, `method476` recolour, `method479` lighting).
- **Present:** `Graphics.drawImage` of the framebuffer, optionally through Java2D's GL pipeline (`GlPresent`).
- **Scene:** `WorldController` (2,548), `ObjectManager`, `Ground`.
- **Big files:** `client.java` **20,129** lines; `Model` 2,823; `WorldController` 2,548; `Texture` 2,016; `RSInterface` 1,722; `Sprite` 1,111; `OnDemandFetcher` 629; `Class11` 571.
- **Total:** 131 files, **59,694** lines, flat default package, no tests, no build system.

---

## Phases

### Phase 0 — Safety net, baseline, and a way to test ⚠️ MUST BE FIRST
- [ ] **0.1 Back up `Proxy Client/`** (source + `bin/`) to `_client-backup-<timestamp>` before any build. Mirrors the server's item 0.1; the lesson there was that the backup must precede the first build that can change output.
- [x] **0.6 ✅ DONE (commit `f3ec2e1`).** Committed the seven untracked load-bearing classes: `LwjglPresent`, `OverlayRefresh`, `GroundItemLists`, `ChatboxItemSearch`, `HiscoresPanel`, `CurseData667`, `JavaUncompress`. Verified before committing that the claim held (`client.java` unmodified + references `LwjglPresent`/`OverlayRefresh`), that all seven exist on disk, and that **none were gitignored** (so no `-f` was hiding the problem). After the commit: all seven are in `HEAD`, **no untracked `.java` remains under `Proxy Client/src`**, and `git show --stat` confirms the commit added files only — **it modified no source and deleted nothing**, so the pre-existing uncommitted client work (46 modified files) is untouched. ⚠️ **Deliberately not included:** the two `*.bak-pre-orig` files (non-`.java`, harmless to `javac`, and Phase 8's business) and the 46 modified files, which are a separate decision. ⚠️ **Also verified in passing: the server's `HEAD` was never broken** — every file referencing its untracked `ConnectionPool`/`MusicState` is itself modified, so only the client had this fault.
- [ ] **0.2 Record a build baseline.** Hash `bin/*.class` after the current `Compile.bat` so a later phase can prove what it changed. ⚠️ Note `bin/` is git-tracked, so this is partly free — but hash it anyway, because `robocopy` in `Compile.bat` is not atomic.
- [ ] **0.3 Capture a golden-master session.** With the current server, log in and record: (a) the exact packet bytes for login + one walk + one attack + one item pickup, (b) a short screen recording of a fixed route, including the Rock Crab area. **This is the regression oracle for the entire plan**, because the plan's risk is protocol/cache drift and nothing else will catch it.
- [ ] **0.4 Establish headless testability.** Decide and prove *one* of: extract the pure logic (cache readers, `Stream`, `Class36`, `Model` geometry) into classes a JUnit test can drive without a display; or a programmatic "boot, connect, assert state" harness like the server's probes. ⚠️ **Without this, Phases 2–6 are only verifiable by playing the game**, which is how refactors rot.
- [ ] **0.5 Record the compatibility contract as a check** that can be run after every phase: the client logs into the unmodified server and the golden-master route replays identically.

### Phase 1 — Build system and Java version
- [ ] **1.1 Replace `Compile.bat` with a Gradle build** (the server already uses a wrapper, so tooling is familiar and consistent). Keep `Run.bat`'s behaviour available.
- [ ] **1.2 Pin a Java toolchain** instead of PATH luck. ⚠️ `-source 1.7` must go — it is unsupported by modern `javac` and only survives today because `Compile.bat` hardcodes JDK 8.
  - **Recommended ladder:** first land **Java 8 target** (`--release 8`) as the minimal safe step and verify the golden master; then move to **Java 17 LTS** once Phase 0.4's tests exist and pass. Java 17 is the sweet spot for LWJGL 3; Java 25 is available here but is unnecessarily bleeding-edge for a game client.
  - ⚠️ **The runtime is already Java 25** (bare `java` in `Run.bat`), so a higher target is *less* risky than it sounds — but the *compile* target change is what can surface latent warnings, so it is verified against the golden master.
- [ ] **1.3 Get the client compiling cleanly with warnings surfaced** (`-Xlint`), and record the baseline warning count. Do not fix warnings yet — just count them, so later phases can show they did not add any.
- [ ] **1.4 Wire the generator** (`tools/generate-object-collision-sizes.ps1`) into the build or a documented step, so `ObjectCollisionSizes` can never silently drift from the server's cfg.

### Phase 2 — Defensive data layer (crash hardening) ← the user's already-agreed fix
Additive guards only. ⚠️ **Change *what happens on invalid data*, never *what is read or in what order*** (frozen cache contract).
- [ ] **2.1 `ItemDef.forID` bounds + `toNote` guard** — the crash found on 2026-10-04. Bounds-check `forID`'s index, and make `toNote()` bail out (returning a sane def) when `certID <= 0` / out of range. The user selected this fix already.
- [ ] **2.2 Audit the other definition readers for the same shape** — `ObjectDef`, `EntityDef`, `Flo`, `IDK`, `VarBit`, `Varp`, `Animation`/`SpotAnim`: any place a cache-derived index or id is used to index an array or a `Stream` without a range check. Fix the *class* of bug, not just the one instance.
- [ ] **2.3 Audit `Stream` reads** for out-of-range offsets, and `StreamLoader`/`Decompressor` for truncated archives — a corrupt or partial cache file should degrade to "missing model", never crash.
- [ ] **2.4 Record each hardening with the failing case** so Phase 0.4's harness can test it (a synthetic bad def must be provably non-fatal).

### Phase 3 — Package structure and monolith decomposition
⚠️ **Highest-risk phase for the server contract.** Purely mechanical; the packet switch moves but never changes.
- [ ] **3.1 Introduce packages** for the flat default-package classes (e.g. `cache`, `model`, `scene`, `ui`, `net`, `game`). ⚠️ **Do this with an automated, verifiable rewriter** (the server work built exactly such a tool — `Proxy Server/tools/typeaware/`), never by hand, and lean on the compiler plus golden-master replay.
- [ ] **3.2 Decompose `client.java` (20,129 lines)** along existing seams (packet handling, scene build, UI/tabs, minimap, input). ⚠️ Extract *fields and methods together*, behaviour-preserving, one seam at a time, recompiling and replaying the golden master after each.
- [ ] **3.3 Name the obfuscated classes** (`Class4`, `Class6`, `Class11`, `Class13`, `Class18`, `Class29`, `Class30_Sub1`, `Class32`, `Class33`, `Class36`, `Class39`, `Class40`, `Class43`, `Class47`, `DummyClass`, `NodeSub`, `MRUNodes`, …) once their roles are confirmed from use. ⚠️ Renaming a class used in a `Stream`/cache path is where a rename can silently change behaviour — verify by replay, not by compile.

### Phase 4 — Renderer abstraction (no behaviour change)
- [ ] **4.1 Define a `GpuRenderer` seam** around the two things that will change: the **scene/model rasterisation** and the **present**. Software implementation only; it must produce output **byte-identical to today** (verified by hashing the framebuffer for a fixed scene, mirroring Phase 0.2's hashing approach).
- [ ] **4.2 Make the renderer selectable at runtime** behind a setting, defaulting to software. ⚠️ Reuse the existing `client_settings.properties` mechanism that `GlPresent` already reads, rather than inventing a second config path.
- [ ] **4.3 Prove the seam is behaviour-neutral** with the golden master *and* a framebuffer hash. No GL code is written in this phase.

### Phase 5 — Model geometry export (additive, for the GPU path)
- [ ] **5.1 Add read-only accessors** to `Model` for its post-transform geometry: vertex x/y/z, per-face colours, indices, and texture coordinates where present. ⚠️ **`Model`'s software rasteriser output must not change** — this phase only *exposes* what is already computed.
- [ ] **5.2 Add the buffer helpers** the GPU path will need (the RuneLite pattern: `GpuFloatBuffer`/`GpuIntBuffer` equivalents) — pure data holders, no GL calls yet.
- [ ] **5.3 Verify with the Phase 0.4 harness** that exported geometry matches what the software rasteriser draws for the same model (a numeric cross-check, not a visual one).

### Phase 6 — Animation preservation (474 frames/skins)
- [ ] **6.1 Document the actual pipeline before touching it:** `Class36` (frames/skins — `Skins.dat`, `Frames.dat`, loose `*.dat` such as `Nex 3502.dat`, plus `load_647`) → `Class18` (skin transforms) → `Animable.method443` → `Model.method443`/`method478`. ⚠️ This is the "474 skeletal structures and transform sequences" the brief refers to.
- [ ] **6.2 Pin the transform contract with tests** (Phase 0.4 harness): for a set of frames, assert the post-transform vertex positions are unchanged. ⚠️ This is the safety net that makes the GPU path safe later — it must exist *before* any GL code.
- [ ] **6.3 Confirm the CPU-side transform is the one uploaded** (RuneLite's approach), so GPU rendering cannot diverge from the software animation. ⚠️ **Do not move skinning into a shader** — that is where "animations stay fluid" turns into "animations drift".

### Phase 7 — OpenGL backend (proceed only after 0–6, all verified) ⚠️ LAST
- [ ] **7.1 ⚠️ Do NOT use `Display.setParent`** — it deadlocks Windows here already. Use **LWJGL 3** with `org.lwjgl.opengl.awt.AWTGLCanvas` (single AWT component), or an offscreen context, and record the choice with the reason.
- [ ] **7.2 Implement the GL backend behind Phase 4's seam**, hybrid by design: 3D scene via GL, **UI/chat/tabs stay software and composite on top** (the RuneLite seam). This is what keeps the work bounded.
- [ ] **7.3 Keep the software path forever as a fallback**, selectable, and the default until the GL path has proven itself across a long session.
- [ ] **7.4 Verify the full checklist below on a live client**, plus the golden master, plus a long session for stability (context loss, resolution change, alt-tab focus).

### Phase 8 — Cleanup (last, after the GL path is settled)
- [ ] **8.1 Delete dead weight:** `dump4/`, `jdt-bin/`, and the LWJGL 2 jars if Phase 7 supersedes them.
- [ ] **8.2 Remove superseded code:** `LwjglPresent` (and `GlPresent` if Java2D present is replaced), and any `class`/`jar` no longer referenced. ⚠️ Verify by *absence of imports and a clean compile*, not by filename.
- [ ] **8.3 Reconcile `src` and `bin`** so a stale class file can never be loaded after a rename (`Compile.bat`'s `robocopy` does not delete removed classes — this is how a renamed class silently keeps working from a stale `.class`).
- [ ] **8.4 Final golden-master run against the unmodified server.**

---

## Verification checklist (the gate for every phase)

1. **Compiles** with the pinned toolchain, warning count not increased.
2. **Logs into the unmodified server** and reaches the game world.
3. **Golden-master route replays identically** (Phase 0.3) — position, animation, and no desync over several minutes.
4. **Combat, item pickup/drop, and a level-up** behave as recorded (these are where cache/model/anim bugs surface).
5. **Cache-heavy actions** work: region change, teleport, NPC spawn, animation-heavy scene (the Rock Crab area where the crash happened).
6. **No new crash** in the console, and specifically the Phase 2 hardening cases hold.
7. **`ObjectCollisionSizes` still matches** the server's `Data/objectSize.cfg`.
8. For Phase 7 only: **software fallback still produces an identical framebuffer**, and alt-tab/resolution-change does not deadlock or lose the GL context.

---

## Open items

1. **Reference repo #1 (`ubjelly/317-OpenGL`) is empty** — if there was a *different* repo intended, supply it; otherwise this plan proceeds on Elvarg alone.
2. **Elvarg is RuneLite-based and cannot be ported wholesale** — confirmed, and the plan treats it as a technique blueprint only. If the user's intent was actually "adopt a RuneLite client", that is a **different, much larger project** and should be decided explicitly rather than assumed.
3. **The client has no tests and no build system** — Phase 0.4/1.1 must fix both, or the plan cannot be verified. This is the biggest single risk to the whole plan.
4. **Cache location is runtime-configured** (`signlink.findcachedir()`); the actual 474 cache was not found inside `Proxy Client/`, so it lives elsewhere (configured at runtime). ⚠️ Phase 0 must record where, because Phase 2's hardening needs a corrupt-cache test fixture.
5. **Java target decision is deferred to Phase 1** with a recommendation (8 → 17) rather than fixed now, because it should be landed against the golden master rather than chosen on paper.
6. **`bin/` is git-tracked** — every phase's rebuild shows up as a large binary diff. Decide whether that stays (it does make rollout simple) or moves to a build artifact; either way record it.
7. ⚠️⚠️ **The client's `HEAD` does not build** (finding 6) — until Phase 0.6 lands, treat the working tree as the only copy of the client. Do not run `git clean`, `git stash`, or `git checkout .` anywhere in this repo, and do not let a tool do it. Note the tracked-but-modified files (`ItemDef.java`, `Model.java`, `WorldController.java`, `RSApplet.java`, `client.java`'s peers, ~46 in total) are also uncommitted work with no commit to fall back to.
8. **Stray `.bak-pre-orig` files sit in `src/`** (`Animation.java.bak-pre-orig`, `Class36.java.bak-pre-orig`) — leftovers from an earlier editing session. Harmless to `javac` (not `*.java`) but they are history and intent that should be either committed deliberately or removed deliberately, in Phase 8.
