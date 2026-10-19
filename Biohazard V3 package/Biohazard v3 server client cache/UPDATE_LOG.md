# Update Log

## 2026-10-19 - Wilderness bandit camp: level 22 + level 130 bandits, OSRS drops, aggression

**What changed:**
- Added 16 bandit spawns around the wilderness Bandit Camp from the two position screenshots, 8 of each level, alternating so both levels are spread through the camp: 3032/3683, 3039/3683, 3036/3686, 3032/3688, 3038/3691, 3043/3690, 3042/3696, 3034/3694, 3025/3690, 3025/3695, 3032/3701, 3031/3706, 3038/3701, 3044/3696, 3052/3692, 3046/3690.
- Level 22 is the cache's own id **195** (Bandit, 27 hp, max hit 3, attack/defence 17). The cache has no level 130 bandit at all, so id **6605** is a new client-side `EntityDef` entry: the level-74 bandit's body, combat level 130, Attack action. Server side is `npc = 6605 Bandit 130 155` (155 hp, max hit 12, attack/defence 57).
- Both bandit tables follow the OSRS wiki, filtered to items this server has. Level 22: bones always, then iron scimitar, steel sq shield, steel axe, chaos/water/air/death/law/blood/mind/nature runes, all 11 grimy herbs, coins 1-80, coal, gems and the rare key halves/rune spear/shield half/dragon spear. Level 130: bones always, then rune full/med helm, rune scimitar, adamant scimitar, mith sq shield, mith axe, dragon longsword, the same runes in larger amounts, herbs, coins up to 250, coal x5 and the gem table at its rarer rates.
- Omitted because they do not exist here: dark fishing bait and the looting bag (not in `item.cfg`), and the hard clue scroll (no handler on this server).
- `NPCHandler.isAggressive` now covers both bandit ids **and fire giants (110)**; red dragons (53) were already in the switch, so the new Waterfall fire giants no longer stand still while being hit.
- Runtime fixes that came with the custom id: 6605 has an explicit attack emote (806) and both bandits a human death emote (836), because the server's `npc.dat` has no row for 6605 and the default emotes would have been invalid.
- Corrected the `npc.cfg` rows for 195/196 (name typo `Gaurd_Bandit`, combat 21 -> 22) to match the cache; neither had any spawns before.

**Files touched:** `Proxy Server/Data/cfg/spawn-config.cfg`, `Proxy Server/Data/cfg/npc.cfg`, `Proxy Server/Data/cfg/npc_drops.cfg`, `Proxy Server/src/server/game/npcs/NPCHandler.java`, `Proxy Client/src/def/EntityDef.java`, `Proxy Server/test/server/game/npcs/BanditDropTableTest.java`, `UPDATE_LOG.md`

**Status:** done - client `compileJava` + `installBin`, server `compileJava` and the full `test` task pass. A probe loading the real config through `NPCHandler` confirms 41/44 drop entries, hp 27/155, 8 + 8 spawns and `isAggressive` true for 195, 6605, 110 and 53. Not yet checked in-game (the 130 bandit's look is the level-74 body, since the cache has no 130 model).

## 2026-10-19 - Red dragons added to Red Dragon Isle; OSRS drop table rebuilt

**What changed:**
- Added 11 Red Dragon spawns (id 53) on the tiles from the in-game position readouts: 3212/3846, 3210/3836, 3203/3837, 3198/3849, 3190/3834, 3202/3824, 3192/3817, 3182/3812, 3207/3810, 3214/3819, 3223/3828 (plane 0, random walk, same stats as the three existing Brimhaven red dragons).
- Replaced the thin red dragon drop row with the real OSRS table, filtered to items this server has: always dragon bones + red dragonhide, then one rolled winner from mith 2h/axe/battleaxe, rune dart x8, mith javelin x20, mith kiteshield, adamant platebody, rune longsword, rune arrow x4, law/blood/death runes, the 11 grimy herbs, 66-690 coins, chocolate cake x3, adamantite bar, uncut sapphire/emerald/ruby/diamond, chaos/nature talisman, rune javelin x5, key halves, rune spear, shield left half and dragon spear, at their wiki rates.
- Dragon javelin tips, ensouled dragon heads, grubby/brimstone keys, tattered pages and the hard clue scroll are omitted because this server has no item/mechanic for them.

**Files touched:** `Proxy Server/Data/cfg/spawn-config.cfg`, `Proxy Server/Data/cfg/npc_drops.cfg`, `UPDATE_LOG.md`

**Status:** done - server `compileJava` and the `server.game.npcs`/`server.game.bots.world` tests pass; every one of the 40 drop item ids was read back out of the cache to confirm it is a real item. Not yet checked in-game.

## 2026-10-19 - Fire giants added to the Waterfall dungeon; teleport moved to 2577 9889

**What changed:**
- Added 12 Fire Giant spawns (id 110) on the tiles from the in-game position readouts: 2580/9890, 2576/9889, 2574/9894, 2577/9897, 2580/9897, 2582/9894, 2563/9884, 2564/9887, 2567/9910, 2563/9910, 2573/9874, 2577/9877 (plane 0, random walk, same stats as the existing fire giant spawns).
- The teleporter's `Monsters > Fire Giants` destination now lands at **2577, 9889** (was 2575, 9885), still labelled "Waterfall dungeon".
- The 6 fire giants already in Taverley dungeon are untouched.

**Files touched:** `Proxy Server/Data/cfg/spawn-config.cfg`, `Proxy Server/Data/cfg/teleport-menu.cfg`, `UPDATE_LOG.md`

**Status:** done - server `test` passes; not yet checked in-game. `Proxy Server/bin/cfg/*` is a stale mirror of `Data/cfg` and was deliberately left alone (it is already missing other newer entries).

## 2026-10-19 - Equipment screen / Set Bonuses: fit inside the frame, green Worn, back to 15106

**What changed:**
- Both screens now fit inside the frame. Measured the background (`Equipment/CUSTOM 7` is 502x303 at (4,20), bottom border at y=316) instead of guessing: the Set Bonuses footnote moved from y=312 to 292, and on 15106 Undead/Slayer share a row (338/430, y=268) with Weapon speed and Base/Actual above the border (last row y=296). The worn slot grid stays where it was.
- A worn set now reads **@gre@Worn** (green) on 15200 instead of plain "Worn".
- Closing 15200 with its X returns to the equipment stats screen (15106) instead of closing everything. Handled in the client's menu-action 200 path.
- Both interfaces declare `width = 512, height = 334`. `addTabInterface` leaves height at 700, which made them pin to the top of the view; with the real height they centre in resizable exactly like the bank, but still do not stretch with the window.

**Files touched:** `Proxy Client/src/ui/Interfaces.java`, `Proxy Client/src/game/client.java`, `Proxy Server/src/server/game/players/combat/EquipmentSets.java`, `Proxy Server/src/server/game/items/ItemAssistant.java`, `docs/EQUIPMENT_TAB_DESIGN.md`, `docs/mockups/equipment-tab-layout-check.png`, `docs/mockups/set-bonus-layout-check.png`, `UPDATE_LOG.md`

**Status:** done - client `installBin` and server `compileJava` pass; in-game look still worth checking.

## 2026-10-19 - Worn equipment slots restored on the stats screen

**What changed:**
- Corrects the entry below: the worn equipment grid was dropped from interface **15106** when the stats screen was rewritten. It is back, using the same slot widgets (**1645-1667, 1688**) at their original coordinates, so the screen again reads bonuses left, character middle, worn slots right.
- The new rows were re-fitted around it: Attack/Defence/Other stay in the left column (now including Ranged STR and Magic DMG), and Target-specific + Weapon speed sit under the slot grid where the old Combat block was. The Set Bonus button is bottom-left at (20, 292).
- Client `installBin` run so `Proxy Client/bin` (what `Run.bat` launches) carries the change, not just `build/`.

**Files touched:** `Proxy Client/src/ui/Interfaces.java`, `Proxy Client/bin/ui/Interfaces.class`, `docs/EQUIPMENT_TAB_DESIGN.md`, `UPDATE_LOG.md`

**Status:** done - client `compileJava` and `installBin` pass; needs an in-game look to confirm spacing.

## 2026-10-19 - OSRS equipment tab, Set Bonus panel and Barrows effects implemented

**What changed:**
- Reworked client interface **15106** into the OSRS layout: paperdoll left, one stats column right (Attack/Defence/Other/Target-specific/Weapon speed), plus a **Set Bonus** button `15140`. Removed the equipment-slot grid from the stats screen and deleted the client-side Combat/DPS block (`Max hit`, `DPS`, `Accuracy`, `Style`) and its `refreshEquipmentCombatStats()` hooks. No View DPS button was added.
- Added interface **15200** ("Set Bonuses") started by `EquipmentSets.sendPanel(c)`; it lists all nine sets with a `Worn`/`-` status and effect line. `ClickingButtons` case 15140 opens it over the equipment tab (`sendFrame248(15200, 3213)`).
- `writeBonus()` now also sends Ranged STR (from `getRangeStr` on ammo + weapon), `Magic DMG: 0.0%`, `Undead: 0%`, `Slayer task: 0%`, and base/actual weapon speed in seconds. `getAttackDelay()` was split so the speed row uses the weapon-only `getBaseAttackDelay()`.
- New `EquipmentSets` registry is now the single source for Dharok/Guthan/Verac/Torag/Karil/Ahrim/Void set checks; `PlayerAssistant.fullVeracs/fullGuthans` and `Player.fullVoid*` delegate to it. Added Ahrim (magic, drain 5 Strength / NPC attack -5), Karil (ranged, drain 5 Agility / NPC defence -5) and Torag (melee, drain 20% energy / NPC defence -5) effects, all 25% on hit.

**Files touched:** `Proxy Client/src/ui/Interfaces.java`, `Proxy Client/src/game/client.java`, `Proxy Server/src/server/game/players/combat/EquipmentSets.java`, `Proxy Server/src/server/game/players/combat/CombatAssistant.java`, `Proxy Server/src/server/game/items/ItemAssistant.java`, `Proxy Server/src/server/game/players/PlayerAssistant.java`, `Proxy Server/src/server/game/players/Player.java`, `Proxy Server/src/server/game/players/packets/ClickingButtons.java`, `docs/EQUIPMENT_TAB_DESIGN.md`, `UPDATE_LOG.md`

**Status:** done - server `compileJava` and `test` pass, client `compileJava` passes. In-game visual check of 15106/15200 still to do.

## 2026-10-19 - Equipment tab, set-bonus panel and DPS: design draft only

**What changed:**
- Wrote `docs/EQUIPMENT_TAB_DESIGN.md` and a rendered mockup (`docs/mockups/equipment-tab-draft.html/.png`) for the OSRS-style equipment tab, the Set Bonus panel, and the deferred DPS display.
- No game code changed. The draft records what the server can honestly show today: 12 bonus fields, ranged strength only inside `rangeMaxHit()`, and no magic-damage/undead/slayer item stats. It also records that Ahrim, Karil and Torag have no set effect in the server while Dharok, Guthan, Verac and Void do.
- Recommendation in the draft: keep DPS off the tab, compute it server-side against a named target later, and drive the Set Bonus panel from one `EquipmentSets` registry shared with combat.

**Files touched:** `docs/EQUIPMENT_TAB_DESIGN.md`, `docs/mockups/equipment-tab-draft.html`, `docs/mockups/equipment-tab-draft.png`, `UPDATE_LOG.md`

**Status:** partial - no implementation started; waiting on the four decisions at the end of the design doc (ranged STR source, magic/undead/slayer rows, missing barrows effects, DPS target model).

## 2026-10-18 - Crystal key chest preview's close button now closes it

**What changed:**
- The preview's close button is really id **60502**, but packet 185 carries it as a 16-bit word (`0xEC56`) and `Misc.hexToInt` decodes that as byte0*1000+byte1, so it reached `ClickingButtons` as **236086** and matched no case. The teleport screens were already rescued by re-reading the true word for known ranges; the crystal chest block (**60500-60599**) was missing from that whitelist. Added it.
- With the true id the existing `CrystalChestPreviewButtons` registration handles the click with `removeAllWindows()`, the same close path the teleport menu uses. No other button on the screen is clickable, so nothing else changed.

**Files touched:** `Proxy Server/src/server/game/players/packets/ClickingButtons.java`, `UPDATE_LOG.md`

**Status:** done - `gradlew compileJava` passes; `CrystalChestPreviewTest` 8/8 passes.

## 2026-10-18 - RFD arena floor plates no longer block movement

**What changed:**
- The hatched grey floor plates in the Recipe for Disaster arena were baked into the map (region 7507) as object type 10, so the server registered them as solid scenery and the arena was full of invisible walls. The plates are ids **12366, 12367, 12368** (the same plate on each instance plane) plus the animated floor overlays **12701-12704**.
- `Region.addObject` now asks `RFDArenaDecor.isNonBlockingFloor(...)` before adding collision; inside the arena footprint (x 1870-1919, y 5325-5375) those ids are still registered as scenery but never clip. Bounds-limited, so the ids stay solid anywhere else.

**Files touched:** `Proxy Server/src/server/clip/region/Region.java`, new `Proxy Server/src/server/game/minigames/rfd/RFDArenaDecor.java`

**Status:** done - `gradlew compileJava` passes. `RFDMinigame.java` was not touched and needs no hook.

## 2026-10-18 - The godsword-on-the-platebody bug, closed for every god-named weapon

**What changed:**
- **The reported ZGS + SGS bug was real, and the fix that was already in was too narrow.** `Item.playerBody` decides the body slot by matching substrings of the item name, and its keyword list contains `"Saradomin"`, `"Guthix"` and `"Zamorak"` because the god platebodies and robe tops are called "Saradomin plate", "Guthix ...", "Zamorak ...". An exclusion for `"godsword"` (the fix the server's author posted) had already been applied, so the four godswords equip to slot 3 correctly. But the exclusion named **one kind of weapon**, and the same word-match was still stealing **24 other ids**: the three god staves (2415-2417), all six croziers (10440-10445), the Zamorakian spear (11716, 11717, 13454), the three god bows (19143-19151) and the three god arrows (19152-19162), which are 1h, 2h and ammunition respectively and were all equipped to the body slot.
- **`Item.playerBody` now excludes weapons by what they are, not by name.** New `Item.isWeaponName` matches a weapon word as a whole trailing word or a whole word inside the name (`sword`, `scimitar`, `dagger`, `mace`, `spear`, `hasta`, `halberd`, `whip`, `claw`, `bow`, `staff`, `crozier`, `sceptre`, `wand`, `trident`, `scythe`, `arrow`, `bolt`, `axe`, `hammer`, `maul`, `flail`, `knife`, `dart`, `javelin`, `rapier`, `mjolnir`, `2h`), so "Saradomin bow" and "longbow" are weapons while "Fishbowl helmet" and "Swordfish gloves" are not. The god armour the keywords exist for ("Saradomin platebody", the god robe tops, Armadyl/Bandos chestplates) still matches the body slot.
- **The 2h stance fallback was still pointing at an NPC's animations.** `CombatAssistant.getPlayerAnimIndex` applies the 2h set (stand 7047, walk 7046, run 7039, turns 7044) from the weapon name and returns, so the `switch` on godsword ids below it is unreachable for a normal item - but it carried **4300/4306/4305**, which are the Gorak's stand/walk/run (`NPCHandler` case 6218), and left the four turn indexes on the unarmed defaults. A godsword id whose name misses the "godsword" check (a renamed or custom item) would have been posed as an NPC in a rest pose. It now assigns the same 2h set as the name branch, turns included.
- **`canLoadObjects` is gone.** The author's second instruction was to remove every `canLoadObjects` reference from `ObjectHandler`, `PlayerAssistant` and `Player`; the object-loading guards were already gone, and the last trace was an unused `public static boolean canLoadObjects` field in `Player` (referenced nowhere). Removed, so a search for it now only finds the dated note in `REFACTORING_PLAN.md`.
- Nothing else about the posed weapons moved: `is2handed` already returned true for godswords, the Saradomin sword, spears and claws, so shield removal and the special-attack animations are unchanged.

**Verification:**
- **The 24-id scope is measured, not argued.** A probe ran the old `playerBody` predicate and the new one over the whole loaded `item.cfg` (`Config.ITEM_LIMIT` ids) and diffed the classification: **24 lost the body classification, exactly the weapon and ammunition ids above, and 0 gained one** - so no garment became unwearable.
- `itemType` was then asked for those ids through a real `new Client(null, 1)`: the godswords, staves, croziers, Zamorakian spears and god bows report `weapon`, the god arrows `arrows`, and the controls (rune/black/mithril platebodies, Saradomin platebody, god robe tops, Armadyl and Bandos chestplates) still report `body`.
- **The animation ids are checked against the real `seq.dat`** (this cache, 9851 sequences): 7047 = 49 frames, 7046 = 16, 7039 = 32, 7044 = 16, attacks 7042/7041/7049 = 20/19/21, block 7050 = 20, specials 7070-7074 present. The old fallback ids 4300/4306/4305 also exist (10/8/15) - they are simply the Gorak's NPC set, which is why they were wrong for a player.
- Server suite green: **1799 tests, 0 failures** (9 new in `TwoHandedEquipmentTest`): godsword slot and stance, the other god-named weapons, god arrows, the god-armour control, all seven stance/turn indexes, a scimitar and a whip as per-weapon controls, the three swings by fight mode, the block emote, and the id-only fallback.
- The fix is server-side only, so no client rebuild is needed; `Run.bat` compiles the server on start, and `startall.bat` already does the same for the client.

**Files touched:** `Proxy Server/src/server/game/items/Item.java`, `Proxy Server/src/server/game/players/combat/CombatAssistant.java`, `Proxy Server/src/server/game/players/Player.java`, `Proxy Server/test/server/game/items/TwoHandedEquipmentTest.java`, `REFACTORING_PLAN.md`, `UPDATE_LOG.md`

## 2026-10-18 - `::online`: finding the players who are actually on

**What changed:**
- **New staff command `::online` (rights 1-3).** It lists every player in the world with their name, rank and world coordinates, staff first, and accepts an optional name filter (`::online bob`). The gap it closes: `::players` reports a bare count, the only names were the console's `[ONLINE]:`/`[OFFLINE]:` lines, and `::xteleto` needs a name typed exactly — so a staff member who had not been watching the console could not discover who was online at all.
- Bots are **counted but not listed**. They are sessionless `Client`s in the same `PlayerHandler.players` array, so `::players` (which counts them) and this listing would otherwise disagree with no explanation.
- Output is capped at **20 lines**; the filter is what makes a larger roster work. Rank labels match the values the role commands write (1 Moderator, 2 Administrator, 3 Owner, 4+ Donator, 7 Respected, 8 Veteran, 9 Forum Moderator).
- Registered from a new hand-written `OnlineCommands` (not the generated `*Commands` classes, which must not be edited by hand) as a whole-word predicate so the optional argument works without swallowing any other command.
- Gated to staff on purpose: coordinates and rank are moderation information, not something every player should be able to poll. Players and donators (rights 4+) match nothing.

**Verification:**
- Server suite green: `CommandHandlerTest` gained **2 tests** (staff-only gating with a name filter, and the rank labels), and the registry assertions moved from 80 to 81 commands / 40 to 41 composite predicates.
- Confirmed with the existing harness rather than a live login: `sendMessage` NPEs on a bare `Client`, so the tests pin the gating and labels, and the listing itself was checked by reading the dispatch path.

**Files touched:** `Proxy Server/src/server/game/players/packets/commands/{OnlineCommands,CommandHandler}.java`, `Proxy Server/test/server/game/players/packets/commands/CommandHandlerTest.java`, `UPDATE_LOG.md`

## 2026-10-17 - The startup "Enable Music?" window is gone

**What changed:**
- **`game.Loader` is now a two-line launcher that calls `client.main`** instead of a Swing window. The window was titled *"Enable Music?"* but actually offered "474 characters" / "634 characters" (labelled 634, passing `"667"`), and its answer went to `new Jframe(args)`, whose constructor **never reads the argument** - so both buttons launched a byte-identical client, and 667 content is always loaded anyway. It was a mandatory click that changed nothing, under a title promising a music setting that does not exist. Double-clicking the jar now goes straight to the game window.
- The window was removed rather than wired up: making the radio buttons *do* something would change what data the client loads, which is a separate decision. `main(String[])` still forwards its argument, so a scripted `java -jar Soul-Trail.jar 474` keeps working and nothing is lost if a real revision switch is added later.
- The jar's manifest is unchanged (`Main-Class: game.Loader`), as are `Run.bat`, `Diff.bat` and the packager, so no launch path had to change. The update check that used to run before the window now runs at the top of `client.main`, before the game window is built, so an updating player still never sees the game open and close.
- A harness check pins the removal (`game.Loader` is not a `JFrame`, and keeps `main(String[])`), because re-adding a startup dialog is otherwise an easy "improvement" to reintroduce.

**Verification:**
- Client harness: **766 checks, 0 failures** (2 new).
- Packaged a `--single-jar` build and ran it: the jar's manifest still says `Main-Class: game.Loader`, `javap` shows `public final class game.Loader` with only `main`, and launching it goes straight into the client (it created `~/Soul-Trail` and its cache files) with no chooser window first.
- `gradlew installBin` clean; docs (`CLIENT_PACKAGING.md` §2 and §6a) and the refactoring plan's "dead UI" note updated to say it was removed rather than left in place.

**Files touched:** `Proxy Client/src/game/Loader.java`, `Proxy Client/src/sign/ClientUpdater.java`, `Proxy Client/test/ClientHarness.java`, `docs/CLIENT_PACKAGING.md`, `CLIENT_REFACTORING_PLAN.md`, `UPDATE_LOG.md`

## 2026-10-17 - One file for players, and the jar that updates itself

**What changed:**
- **Players can be given a single `Soul-Trail.jar` and nothing else.** New Build-tab checkbox **Ship one file** (`--single-jar`, `single.jar=true` in `packager.properties`) forces the fat-jar mode and stops writing `client.properties`, the two `Run*.bat` launchers, `libs/` and the distribution zip. The output folder then contains exactly one file; the settings that used to sit beside it are embedded in the jar, where they already were as the default. LWJGL extracts its bundled natives from the jar on first use, so the file works from any folder. The live `packager.properties` now has `single.jar=true`.
- **The client updates its own jar from a `client.version` manifest.** The packager writes `client.version` (version + the jar's SHA-256) into the Upload folder beside `cache.zip`/`cache.version`. New `sign.ClientUpdater` runs before `game.Loader` shows its window (and again at the top of `client.main`, answered from a cached decision, for launches that skip the loader), compares the jar's `Implementation-Version` with the manifest, and on a difference downloads the jar from `client.jar.url`, verifies the zip signature, the SHA-256 and the downloaded jar's own version, then replaces itself and restarts. New `client.version`, `client.version.url`, `client.jar.url` and `client.update.enabled` keys (`ClientConfig`, parsed and overridable with `-D`). The release loop is now: bump **Client version**, build, upload the jar and `client.version` over the old ones - players keep the same file and the same shortcut.
- **Replacing a running jar needed a helper, because Windows locks it.** Verified on this machine (JDK 25): `renameTo`, `Files.copy ... REPLACE_EXISTING` and `delete` on the running jar all fail with *"The process cannot access the file because it is being used by another process"*. So new `sign.ClientSwap` does the swap: the client extracts that one class file into a folder, starts it with `-cp` (a folder classpath is not locked), and exits; the helper retries for up to 60 s, moves the verified jar into place and relaunches it. A read-only folder falls back to installing `~/Soul-Trail/client/Soul-Trail-<version>.jar` and `latest-client.txt`, so the original file keeps working as a launcher.
- **An update leaves the player's folder byte-for-byte as it was, apart from the jar.** The first version of this feature wrote `Soul-Trail.jar.old` (a backup) and `Soul-Trail.jar.log` (the relaunched game's stdout) into the game folder, which is exactly the clutter a one-file install is supposed to avoid. The backup now goes to `~/Soul-Trail/client/previous-client.jar`, the swap helper is extracted into `~/Soul-Trail/client/swap`, and the relaunched game's output goes to the null device (`NUL` / `/dev/null`, since `Redirect.DISCARD` is Java 9+ and the client targets 8). A download that cannot be installed is deleted instead of being left beside the jar, and `installBeside` no longer drops a second jar next to the first. Any `Soul-Trail.jar.old` / `Soul-Trail.jar.log` / `.swap` / `.Soul-Trail-*.download` from an earlier build is deleted at startup, before the update check, so it is cleaned up even for a player who has updates switched off.
- **A relaunch keeps the JVM arguments it was started with.** `-Xmx` and any `-Dclient.*` override used to be lost (the replacement guessed the heap); the updater now forwards `RuntimeMXBean.getInputArguments()`, minus agents and debugger flags, so a player launched with more memory or a `-Dclient.config` keeps them.
- **An update can never brick a client or loop.** An unreachable manifest, an HTML page, a non-zip, a SHA-256 mismatch, a jar whose own version disagrees with the manifest and a jar whose `client.properties` disagrees with it all leave the current client running. A release that fails verification is recorded as rejected (`~/Soul-Trail/client/rejected-client-version.txt`), so a bad upload is downloaded once, not on every launch; a failed download or a swap that could not happen is *not* recorded, so a transient lock is retried on the next start (the earlier code wrote the marker before the swap, which could strand a player on an old jar forever). Every decision goes to `~/Soul-Trail/client/update.log`. The updater is skipped entirely when running from `bin/` or with `-Dclient.update.skip=true`.
- `Client version` must now be a short token (`3.1`, `2026-10-17a`); validation rejects spaces, because the comparison is against a jar manifest attribute.
- The Guide tab gained the single-file build, the release workflow for the jar itself, why the game closes and reopens during an update, and the update log's location; `docs/CLIENT_PACKAGING.md` gained **§6 Auto-updating the client jar itself** and a renumbered tail, plus the new keys, layout and troubleshooting entries.

**Verification:**
- Client harness: **764 checks, 0 failures** (22 new: both-URL requirement, non-http URLs ignored, equality/rejected/rollback rules, a failed install staying retryable, zip-signature and HTML/JSON refusals, manifest version parsing, and a real temp jar accepted at the matching version but refused at a mismatched one, at a wrong SHA-256 and at a `client.properties` version that disagrees with its own manifest).
- Headless `--single-jar` build into a scratch folder: the output folder held **only** `Soul-Trail.jar` (3273 KB) and nothing else; `cache-upload/` held `client.version` reading `4.2` + the jar's SHA-256. The jar's manifest says `Implementation-Version: 4.2`, contains `client.properties`, `sign/ClientUpdater`, `sign/ClientSwap` and the three LWJGL `.dll`s.
- Swap against an **exclusively locked** target file: the helper retried, then replaced the file (`NEW-JAR-BYTES-9.9`), consumed the download and kept the backup (`OLD-JAR-BYTES`).
- End-to-end self-update against a local HTTP host: a probe jar at `1.0` read the `2.0` manifest, downloaded, verified, swapped itself, and the relaunched process ran `2.0`; the update log has exactly two lines (no second download), and the jar on disk reports `Implementation-Version: 2.0`.
- Same run with the output-path assertions: afterwards the game folder held **only** `Soul-Trail.jar` - no `.old`, `.log`, `.swap` or `.download` - while `~/Soul-Trail/client` held `previous-client.jar` and `update.log`. The relaunched probe reported the forwarded `-Xmx512m` (`xmxMb=512`) and a `-Dclient.foo=bar` override, and `previous-client.jar` was byte-for-byte the *old* build.
- Bad upload, twice: a manifest saying `3.0` over a jar saying `2.0` was downloaded and refused on the first launch (`rejected: the jar is version 2.0 but the manifest says 3.0`), `rejected-client-version.txt` holds `3.0`, and the second launch did not even contact the host. The jar and the folder were untouched.
- Legacy cleanup: a game folder seeded with `Soul-Trail.jar.old`, `Soul-Trail.jar.log`, `.swap/sign/ClientSwap.class` and `.Soul-Trail-9.9.download` was reduced to only `Soul-Trail.jar` on a start with updates **disabled**, and `~/Soul-Trail/client` was not created at all.
- Packager recompiles; client `installBin` clean.

**Files touched:** `Proxy Client/src/sign/{ClientUpdater,ClientSwap,ClientConfig}.java`, `Proxy Client/src/game/{client,Loader}.java`, `Proxy Client/tools/packager/ClientPackager.java`, `Proxy Client/tools/packager/packager.properties`, `Proxy Client/test/ClientHarness.java`, `docs/CLIENT_PACKAGING.md`, `UPDATE_LOG.md`

## 2026-10-17 - Cache artifacts moved out of the client folder

**What changed:**
- `cache.zip` and `cache.version` are now written to a separate **Upload folder** (default `dist/cache-upload`, field on the Cache tab, `--cache-out` on the command line, `cache.output.dir` in `packager.properties`). The client output folder now holds only what players receive: the jar, `client.properties`, `libs/`, the two launchers and the distribution zip.
- The distribution zip excludes the upload folder by absolute path, so it stays clean even when the upload folder is set inside the output folder, and `cache.version` is skipped by name as well as the existing `.zip` rule.
- `--out` resets the upload folder to `<out>/cache-upload`, so a script that moves the whole output gets the split layout without a second flag; an explicit `--cache-out` after it still wins.
- A settings file from before the split has no `cache.output.dir`; it is derived from the loaded output folder rather than left pointing at the tool directory.
- Validation refuses an upload folder equal to the cache source folder, which would zip the zip.
- The Guide tab's Step 3 and Step 6 now name both folders and explain why they are apart, and the docs' output-layout diagram, quick start, cache-packaging section, troubleshooting and headless flag list were updated to match.

**Verification:**
- Headless run into a scratch output folder wrote `cache.zip`/`cache.version` under `cache-upload/` and a manifest reading `9` plus the zip's SHA-256.
- Packaged the real 70 MB cache into a scratch output, then rebuilt the client with the distribution zip: the zip has 10 entries (`client.properties`, six LWJGL jars, two launchers, the jar) and **no** `cache.zip`, `cache.version` or `cache-upload/` entry.
- The existing `dist/cache.zip` and `dist/cache.version` were moved into `dist/cache-upload/`; the manifest still reads version `1` with SHA-256 `643e432f...`, matching the uploaded zip.
- Packager recompiled; GUI layout check passes on every tab.

**Files touched:** `Proxy Client/tools/packager/ClientPackager.java`, `Proxy Client/tools/packager/packager.properties`, `docs/CLIENT_PACKAGING.md`, `UPDATE_LOG.md`

## 2026-10-17 - Soul-Trail cache rename, a readable packager, safe cache updates and a persisted Barrows run

**What changed:**
- **The cache folder is now `Soul-TrailCache`.** `CacheLocation` owns the decision (preferred name if it holds a cache, otherwise migrate `Biohazard.474`, otherwise create the new one), so `findcachedir()` is three lines and the rename is testable without owning a real home folder. A plain rename on the same volume costs nothing and keeps every file; the copy fallback is only accepted once `main_file_cache.dat` is really there, and a half copy is deleted rather than left under the new name looking installed. A `cache.directory=Biohazard.474` in an old `client.properties` is normalised to the new name, so a stale config cannot keep a second cache alive.
- **Settings and screenshots moved out of the cache entirely.** New `PlayerStore` writes `client_settings.properties` and its `.dat`/`.pre-glgate`/`.renderer-gl.bak` siblings into `%USERPROFILE%\Soul-Trail`, and screenshots into `Soul-Trail\Screenshots`. On first run the existing `client_settings*` files and screenshots are *copied* out of the cache folder (copies, not moves), so a rename that failed - or an older client still reading the cache folder - loses nothing. `ClientSettings`, `RendererConfig`, `GlPresent` and `Jframe` all go through it.
- **A cache update can no longer wipe a player's setup.** Two independent walls: the packager leaves `client_settings*`, `uid.dat`, `hud_layout.properties`, `Screenshots/`, `cacheVersion*.dat` and `cache.version` out of `cache.zip`, and `CacheDownloader.isPlayerOwned` refuses to extract those names even when a hand-made zip contains them (`Kept N player file(s) from the download untouched.`). Because the settings folder is not the folder being replaced, the third protection is structural.
- **The packager GUI no longer overlaps.** The old panels numbered `gridy` by hand in two places (`addRow` used `row * 2`, the cache panel numbered its tail from 7), so labels, hints and fields landed on the same coordinates. A single `Form` cursor now gives every section, label, field and hint its own row with padding, each tab scrolls, and the window has a fixed 900x700 minimum rather than packing to the guide. The old saved `packager.properties` (which still said `Biohazard.474`) is migrated on load and on save.
- **There is a Guide tab, and it is the first tab.** Seven numbered steps (point at the client, server tab, package the cache, upload with direct links, point the client at them, build, test on a clean PC), how to update the cache later with no new jar, what players keep and why, and a short troubleshooting list. It is HTML in `ClientPackager.java`, so it cannot drift away from the buttons it describes.
- **A Barrows run survives a relog.** `PlayerSave` now writes `barrows-killed` (a six-bit mask), `barrows-run-count`, `barrows-hidden` and `barrows-hidden-set`; the old `barrowskillcount` is still read. `hiddenBrotherSet` exists because the hidden brother is index 0 for Ahrim, which is indistinguishable from "not chosen yet"; choosing one saves immediately. `Client.initialize` calls `Barrows.syncOverlay` on login, so the plugin overlay is correct in the first frame rather than after the walkable-interface tick.

**Verification:**
- Client harness: **742 checks, 0 failures** (new: player-owned cache entries, legacy-name config, the partial-copy recovery, a complete folder surviving beside an old one).
- Full server suite: **1786 tests, 0 failures** (`PlayerSaveTest` covers the mask round trip, a save from before the persistence, and the six-bit bound).
- The packager GUI was rendered headlessly at **1000x780 and its 900x700 minimum**: every tab reported no overlapping siblings and no clipped label. The live `packager.properties` (saved by the old build with `Biohazard.474`) loads as `Soul-TrailCache` with `~/Soul-TrailCache` as the source.
- A headless packager run against a scratch cache containing `client_settings.properties`, `hud_layout.properties` and `Screenshots/shot.png` produced a zip with only `main_file_cache.dat` and `Sprites/icon.png`, and a `cache.version` holding version 7 plus the zip's SHA-256.
- Migration was exercised against the real install: the full 70 MB cache now lives in `Soul-TrailCache`, the 13 KB settings and both screenshots in `Soul-Trail`, and nothing was re-downloaded.

**Files touched:** `Proxy Client/src/sign/{CacheLocation,PlayerStore,ClientConfig,signlink}.java`, `Proxy Client/src/cache/CacheDownloader.java`, `Proxy Client/src/game/ClientSettings.java`, `Proxy Client/src/ui/{Jframe,RendererConfig,GlPresent}.java`, `Proxy Client/tools/packager/ClientPackager.java`, `Proxy Server/src/server/game/players/{Player,PlayerSave,Client}.java`, `Proxy Server/src/server/game/minigames/barrows/Barrows.java`, tests `ClientHarness.java` and `PlayerSaveTest.java`, `docs/CLIENT_PACKAGING.md`, `UPDATE_LOG.md`

## 2026-10-17 - Barrows HUD, brother list, and the prayer spam

**What changed:**
- **The tombs no longer switch the HUD off.** Two paths were blacking out the minimap: `Client`'s area loop sent frame 99 state 2 every tick while `inBarrows()`, and `Barrows.teleport` sent the same state on the way down. The client's black-map branch then returned *before* drawing the HP, prayer, run and spec orbs, so the whole corner looked dead. Barrows no longer asks for the black map at all, and `Barrows.teleport` explicitly resets the state to `0`.
- **The black-map branch no longer eats the orbs.** `drawMinimap` now clears the layer and falls through to the shared frame, compass, world-map button and orb drawing instead of returning early, so *any* map state - including Sailing's own frame 99 use - keeps the HUD. The fixed-mode blacking no longer copies `mapBack`'s mask into the 246x168 producer: the two row strides differ, which is what painted the stray bands across the fixed minimap.
- **Barrows brothers are no longer outlined.** `drawNpcIndicatorHulls` stopped drawing a hull for the six brother NPC ids. Tagged-NPC highlighting and all other plugins are untouched.
- **Running dry says it once.** In the tunnels the flat `0.4 + 0.15 * kc` drain was added even with no prayer active, so it walked the level to zero and `reducePrayerLevel` then fired every tick. The extra now applies only inside the `toRemove > 0` branch, and `reducePrayerLevel` returns immediately when the level is already empty. `handlePrayerDrain` is therefore a no-op once the last point is gone instead of re-sending the message.
- **The infobox lists the brothers, RuneLite order.** New `barrowsBrotherList` setting (default **on**, toggle id `24430`) draws Ahrim, Dharok, Guthan, Karil, Torag, Verac under the KC/prayer lines. The display order maps back to the server's kill-mask order (`{0,2,5,1,4,3}`), so each name is green `0x00FF00` once that brother is dead and grey `0x808080` while they remain. It is in the settings list and the plugin sidebar, and persists through `BarrowsPlugin.save/load`.

**Verification:**
- New: `BarrowsPrayerDrainTest.theTunnelsDoNotDrainPrayerWithNothingSwitchedOn` (200 barrows ticks with no prayer active leave the level and accumulator untouched) and `runningDryAnnouncesOnceAndThenStopsTalking` (one message, then 200 more ticks add none).
- Client compiles; headless harness **722 tests, 0 failures**; `installBin` refreshed so `bin/` carries the fix.
- Full server suite: **1783 tests, 0 failures**.

**Files touched:** `Proxy Server/src/server/game/players/Client.java`, `Proxy Server/src/server/game/minigames/barrows/Barrows.java`, `Proxy Server/src/server/game/players/combat/CombatAssistant.java`, `Proxy Client/src/game/client.java`, `Proxy Client/src/game/ClientSettings.java`, `Proxy Client/src/ui/{BarrowsPlugin,Interfaces,PluginSidebar}.java`, test `BarrowsPrayerDrainTest.java`, `UPDATE_LOG.md`

## 2026-10-17 - One revive per round, and the second death in a round is final

**What changed:**
- **The rule is stated where it is spent.** A completed revive now tells the player, in two short lines, that the allowance is used: *"You have used your revive for this round."* / *"If you die again this round, nobody can revive you until the next round."* The reviver gets *"You revive X."*, the rest of the party is told the target has none left (so nobody starts a pointless walk), and the downed message gained a third line, *"You can only be revived once per round."* The chest's risk explanation was reworded to say the same: after the allowance, *a death is final until the next round*.
- **A second fatal hit no longer kneels.** `DelveInstance.onFatalDamage` now checks `hasReviveLeft` first: if the round's allowance is spent, the player skips `DOWNED` entirely - no bleed-out bar, no 30-second window nobody can act on. They are told *"You have already used your revive this round."* / *"You die - nobody can revive you again until the next round."*, call out *"I can't be revived again!"*, lose the chest, and the party hears that they cannot be saved.
- **The death animation is played on the body, then the ghost takes over.** `DelveVisuals.playDeath` plays anim **836**; `DelveInstance` holds the corpse for `DEATH_ANIMATION_TICKS` (5) before `becomeGhost` + `sendToLedge`. The morph is deliberately not instant: 836 is a player-skeleton animation, and human frames over the ghost's NPC mesh read as a frozen or twisted body. The same delay now applies to a bleed-out death, so the timer ending also reads as a fall rather than a pop into a ghost.
- **A rejoiner comes back at full hitpoints.** The fatal-hit clamp leaves a corpse on 1hp so the ordinary death path cannot fire, but `DelveCombat.restore` only fills hitpoints that are at or below zero - so a player who died and rejoined was returning on 1hp. `beginSpin` now puts a rejoining corpse on 0 before the reset, where no hit splat is being written, which makes the design's "fresh start" real.
- **Config comment.** `delves.cfg` documents that `max-revives-per-round = 1` means the second fatal hit is a real death, and that the allowance refills at the next lock.

**Verification:**
- New: `DelveMemberStateTest.aSecondDeathInTheSameRoundIsFinalAndSkipsTheKnell` (no second kneel, chest gone, anim 836, ghost after the fall, both deaths counted) and `DelveVisualsTest.aFinalDeathPlaysTheDeathAnimationAndLeavesTheGhostUntilItRuns`.
- `aCompletedChannelRevivesTheTargetOnceAndOnlyOnce` now pins that the post-revive fatal hit is DEAD rather than DOWNED; `aRejoiningPlayerComesBackForTheNextRoundWithAnEmptyChest` asserts full hitpoints on rejoin.
- Full server suite: **1781 tests, 0 failures**.
- `DESIGN.md` (downing/reviving rules, the final-death path) and `delves.cfg` updated.

**Files touched:** `Proxy Server/src/server/game/minigames/delves/{DelveInstance,DelveVisuals,DelveChestInterface}.java`, `Proxy Server/Data/cfg/delves/delves.cfg`, tests `{DelveMemberStateTest,DelveVisualsTest}.java`, `docs/roulette/DESIGN.md`, `UPDATE_LOG.md`

## 2026-10-17 - One-click revives that walk themselves, and a rename-proof bleed-out bar

**What changed:**
- **Clicking Revive from across the arena is now the whole interaction.** The click records `DelveSession.pendingReviveTarget` and hands the walk to the delve's own pathing (`DelveInstance.routeTowards`), instead of leaving it to the base attack-follow. The follow stops at the equipped weapon's range, so a ranger or mage clicking a body eight tiles away walked to eight tiles and then heard "move closer" - the second click this removes. Every tick of the approach re-routes when the reviver is standing still (a body can be nudged by a hit, a route can open), and arrival starts the channel in the same tick, exactly as an adjacent click would.
- **The walk cannot become a wireless revive.** Arrival, not the click, decides: `tickPendingRevives` re-checks that the target is still `DOWNED`, the reviver is still `ALIVE`, and the live tile distance is inside `revive-proximity-tiles` before it calls `beginRevive`, which enforces the same distance again. Walking away mid-channel still breaks it.
- **A walk click cancels a queued revive.** `Walking.processPacket` calls `DelveCombat.cancelPendingRevive`, so the player's later instruction wins rather than the server dragging them back to the body. Giving up (target revived by somebody else, out of time, target dead) also stops the delve's walk instead of leaving it running; the intent's walk belongs to the intent.
- **Name matching for the overhead bar is whitespace-proof.** `ui/DelveDowned.key` now collapses runs of spaces as well as flattening case and underscores, so a cosmetic difference in how a name is written cannot be the reason a teammate's bleed-out bar never appears.

**Verification:**
- New `DelveCombatTest.aWalkClickAbandonsTheQueuedReviveInsteadOfDraggingThePlayerBack`; `clickingADownedTeammateFromFarAwayWalksOverAndStartsTheChannelOnArrival` now asserts the delve owns the walk (`followId` cleared) and still pins the arrival-triggered channel.
- Full server suite: **1779 tests, 0 failures**. Client compiles and `installBin` refreshed, so `bin/` (what `Run.bat` launches) carries the ghost animation fix and the bleed-out bar.

**Note for the next play test:** `Run.bat` runs `bin/` as-is and does not compile. A client launched before the last `Compile.bat` keeps the old classes - which is what a ghost that slides on human animations and a missing bleed-out bar look like from the outside. `StartAll.bat` (or `Compile.bat` before `Run.bat`) after pulling these changes.

**Files touched:** `Proxy Server/src/server/game/minigames/delves/{DelveCombat,DelveInstance}.java`, `Proxy Server/src/server/game/players/packets/Walking.java`, `Proxy Client/src/ui/DelveDowned.java`, tests `DelveCombatTest.java`, `UPDATE_LOG.md`

## 2026-10-17 - Delve revives hold the bleed-out, 30s downing, ghost animations, and a rejoin default

**What changed:**
- **A revive channel freezes the bleed-out.** `tickDowned` skips any member with an incoming channel, so the 30-second window stops where the pickup caught it and resumes there if the channel breaks (a hit on the healer, the target dying). Before this, a revive started at two seconds left finished after the timer and the player died mid-pickup, which read as the revive having failed.
- **Revive progress is a bar of its own, seen by the whole party.** The `downs` payload grew two fields, `name,msLeft,msTotal,reviveMsLeft,reviveMsTotal` (`DelveHud.downedPayload`); a non-zero revive total is the pause signal. `ui/DelveDowned` keeps the frozen bleed-out bar in its current colour and draws a second green bar above it that fills as the channel completes. Both are broadcast to every member, pushed immediately on down, revive, death and interrupted channel rather than waiting for the five-tick HUD beat, and `DelveInstance` exposes `reviveTicksRemaining` / `reviveTicksTotal` for the payload.
- **A revived player calls out and comes back at 30% (never under 10hp).** `DelveVisuals.sayAlive` plays the overhead "I'm alive!" on a completed revive and on the wave-clear auto-revive. `revive-hp-percent` is now 30 with a new `revive-min-hp = 10` floor, and `DelveMath.reviveHp` pays `max(30% of max, min(10, max))` clamped to the maximum, so a 20hp player returns at 10 rather than at 6. The stat tab and orb are refreshed (`refreshSkill(3)`) so the heal is visible, not just stored.
- **Downing lasts 30 seconds.** `downed-ticks` is now 50 (30s at the 600ms tick) instead of 25, so a teammate has a real window to cross the arena.
- **The ghost moves like a ghost.** A morphed player keeps the seven human animation ids the appearance block writes, which played human frames over the ghost's mesh and left the spectator looking frozen. `Player.updatePlayer` now detects the morph (`desc != null`) and replaces the whole set with the cache's own NPC animations (`EntityDef.standAnim` / `walkAnim`), pointing the live idle at the new stand so the first frame after the morph is right. The next appearance block after `restoreBody` carries no `desc`, so the human set comes back on its own when the player respawns.
- **An unanswered ledge prompt rejoins instead of leaving.** If the 20-second ledge clock runs out with no answer, the member is now recorded as `REJOIN`, their option interface is closed, and they are told they will rejoin: `expireLedgeDecisions` (and the near-unreachable chest-timeout branch) changed from `LEAVE`. An idle corpse no longer holds the party *and* no longer loses its place because the player was not looking at the screen.

**Verification:**
- New: `DelveMathTest.aReviveAlwaysPaysTheMinimumEvenWhenAShareOfALowMaximumWouldNot`; `DelveHudTest.aReviveChannelFreezesTheBleedOutAndCarriesItsProgressInTheSamePayload` and `breakingTheChannelResumesTheBleedOutAndDropsTheReviveProgress`; `DelveMemberStateTest.theBleedOutClockPausesWhileAChannelIsOnThePlayerAndResumesWhenItBreaks`.
- Rewritten: `aDeadPlayerWhoDoesNotAnswerIsNotSilentlyRejoined` became `aDeadPlayerWhoDoesNotAnswerRejoinsTheNextRoundByDefault` (asserts the closing prompt and the rejoining party), and `theLedgeClockLetsTheNextSpinStartLongBeforeTheChestTimeout` now asserts the default is Rejoin. `aCompletedChannelRevivesTheTargetOnceAndOnlyOnce` additionally pins the "I'm alive!" call and the revived hitpoints; the default-value test pins 50 ticks, 30% and `revive-min-hp`.
- Full server suite: **1776 tests, 0 failures**. Client compiles.
- `DESIGN.md` (R2, downing/reviving, the ledge clock, the config table) and `PACKETS.md` (new §2.5 for the `downs` payload, the ledge default) updated; `delves.cfg` documents the paused clock and the new floor.

**Files touched:** `Proxy Server/src/server/game/minigames/delves/{DelveInstance,DelveVisuals,DelveHud,DelveMath,DelveSettings,DelveChestInterface}.java`, `Proxy Server/Data/cfg/delves/delves.cfg`, `Proxy Client/src/{game/Player.java,ui/DelveDowned.java}`, tests `{DelveMathTest,DelveHudTest,DelveMemberStateTest}.java`, `docs/roulette/{DESIGN.md,PACKETS.md}`, `UPDATE_LOG.md`

## 2026-10-17 - Delve downed/revived presentation and a multi-combat arena

**What changed:**
- **A dead spectator is a ghost, not a hidden player.** The `PlayerHandler.isHiddenSpectator` filter is gone (it superseded the "Spectators are invisible" note below), so an eliminated member stays in every peer's local player list. `DelveVisuals.becomeGhost` sets `isNpc`/`npcId2 = 4388` and flags an appearance update, so teammates see the cache's ghost where the player stood and still read their public chat and overhead text. `DelveVisuals.restoreBody` puts the real model back on respawn, next-round rejoin and exit.
- **The downed have a bleed-out bar.** `DelveInstance` pushes `delve:<seq>:downs:<name>,<msLeft>,<msTotal>;…` (`DelveHud.downedPayload`) with the periodic HUD and immediately on every down/revive/death; a `;`-free name is sanitised first. The new client `ui/DelveDowned` interpolates the remaining time between pushes and draws a 30x5 bar above the player's head, narrowing as it drains and sliding from bright red `0xFF0000` to black `0x000000`, then clears it when the entry disappears. `downed-bar` in `delves.cfg` (default on) switches it off without touching the panel.
- **The downed kneel and the revived stand.** Anim `1331` on the way down, with the last frame held by `client.method101` for as long as the bar says the player is down; anim `1332` plus GFX `247` on revive and on every next-round respawn. The healer channels anim `2244` for the duration of the revive and is released (`65535`) when the channel breaks, so a heal that is interrupted does not leave them stuck mid-animation.
- **The downed message fits the chat box it is drawn in.** The old single line ran past the 317 chat box; it is now two short lines ("You are down!" then "A teammate must stand next to you and click Revive."), each inside the box width, so the interaction is readable without the sentence being cut off.
- **The whole arena is multi-combat.** `DelveArena.isArenaMultiTile(x, y, plane)` covers X `2724..2752`, Y `5061..5108` on any delve plane (`plane >= 100`), and is consulted by `Player.inMulti` and `NPC.inMulti`. Parties can pile onto the same boss, and the same coordinates in the live world are left exactly as they were.

**Verification:**
- New `DelveVisualsTest` (4: the downed/stand-up animations and the revive spark, the healer's channel and its stop, the ghost morph and its reversal, and that a plain body is never rewritten).
- `DelveHudTest` gained three (the payload carries only `DOWNED` members with their ms left and full window, an empty list is an empty field so the client clears every bar, `downed-bar = false` leaves the HUD switch alone) and `DelveArenaTest` gained two (the four quoted corners and the arena centre are multi while one tile outside and the same tiles at plane 0 are not; a spawned wave boss answers `inMulti()`).
- Full server suite: **1772 tests, 0 failures**. Client compiles.

**Files touched:** `Proxy Server/src/server/game/minigames/delves/{DelveVisuals,DelveArena,DelveInstance,DelveHud,DelveSettings}.java`, `Proxy Server/src/server/game/players/{Player,PlayerHandler}.java`, `Proxy Server/src/server/game/npcs/NPC.java`, `Proxy Server/Data/cfg/delves/delves.cfg`, `Proxy Client/src/{game/client.java,ui/DelveDowned.java,ui/DelveHud.java}`, tests `{DelveVisualsTest,DelveHudTest,DelveArenaTest}.java`, `UPDATE_LOG.md`

## 2026-10-17 - Vengeance, DFS splats/timer, Kalphite Queen styles, and Delve round polish

**What changed:**
- **Vengeance Other works.** Packet 249 routes the lunar interface id `30298` to `PlayerAssistant.vengOther`: animation `4411` on the caster, GFX `725` over the target's head, refused when the target already has a Vengeance up, and charged against the same 30s clock as the self-cast. Vengeance on yourself now plays GFX `726` (the red skull) instead of the old effect.
- **The Vengeance Other spell stays highlighted.** While `spellSelected` is that widget the client draws a one-pixel outline around it (`DrawingArea.fillPixels`, so it reads as a thin frame and not a white blob), and the existing `spellSelected = 0` reset on any menu click clears it when the player clicks a player, an NPC or the ground.
- **Dragonfire Shield splats wear the shield.** New wire style `HitStyle.DRAGONFIRE_SHIELD = 4` is set by `handleDfs` / `handleDfsNPC`; `HitStylePlugin` draws item `11283` cropped and scaled to the same 18px icon as the skill/vengeance icons. New `hitStyleDfs` toggle (default on, persisted with the other hit-style switches).
- **DFS cooldown infobox.** `StatusTimers` treats GFX `600` as a 30-second Dragonfire Shield cooldown and reuses the same 18px `11283` art, so the timer matches the splat icon.
- **The Kalphite Queen protects styles, not players.** `KalphiteQueen.protectsAgainst` replaces the "full Verac's or nothing" gate: form `1158` turns away melee and ranged and lets magic through, full Verac's pierces the melee protection with the flail, and form `1160` turns away magic and takes steel and arrows. Her delve copy stays exempt. `DwarfCannon` uses the same rule, so cannon fire cannot hit form one.
- **Leaving a delve mid-run no longer double-teleports.** Every removal path now sets `delveTeleportBypass` for the duration of the departure, so `DelveArena.exit` is skipped for a player who is teleporting out and the teleport they cast is the one that lands. `interceptTeleport` also leaves a lobby party alone, so talking to the Delve Master or leaving the party never moves the player.
- **A 20-second grace between waves.** New `DelveState.PREPARATION` (`prep-seconds = 20` in `delves.cfg`, `0` disables it). The wave's copies are already gone, the party is restored and placed back in the arena, both the HUD and chat count down, and "ten seconds" / "five seconds" are called out. The round only advances when the clock expires.
- **A dismissed chest menu comes back.** The chest is a chatbox interface the client closes on the next walk, so an unanswered member whose menu is no longer open is re-prompted every eight ticks with a one-line explanation. It is per-player, so a party of five cannot produce five prompts or answer for each other.
- **Special attack is refilled every round.** `restoreSpecialAttack` writes 100 and refreshes the bar at the start of the grace and at the reel lock, next to the existing HP/prayer restore.
- **The Delve Master answers for the run, and his menu loops correctly.** Talking to him mid-run opens the chest menu in `CHEST` (the recovery path) or the lobby menu in `LOBBY` instead of restarting the intro. Picking a difficulty returns to the lobby menu, and every `start` refusal now says why rather than looking like a dead button. Long dialogue lines were split into two short ones per page (intro gained a second page, id `9828`) because the chat box does not wrap.
- **Downed players read as downed.** Animation `836`, the overhead call "I am down! Revive me!", and a chat line naming the interaction and the seconds left.
- **Spectators are invisible and closer.** A delve member in `MemberState.DEAD` is filtered out of every other player's update (`PlayerHandler.isHiddenSpectator`) and put back by the ordinary add-player path when they respawn. The ledge offset is now `(5,0)` instead of `(12,0)` so the spectator actually sees the fight.
- **The Delve HUD fits its own box.** The panel height was one line pitch short of the rows it drew, which is why "No loot banked yet" and the chest grid sat on the game view; the height is now derived from the same row count the layout uses. The grace adds a "Prepare m:ss - next wave soon" row and a "trade now" hint.

**Verification:**
- New `KalphiteQueenTest` (6: both forms, form-one melee/ranged, form-one magic, Verac's pierce, form-two magic only, other NPCs untouched).
- New `DelvePreparationTest` (5: Continue enters the grace, the countdown runs and then rolls, the HUD carries the milliseconds, spec comes back, `prep-seconds = 0` skips).
- `DelveChestInterfaceTest` gained two (a dismissed menu re-opens, an answered member is not prompted again) and `withdrawingAtTheChestBanksExactlyWhatTheSummaryReports` was made deterministic (a random RARE roll was priced into the chest but not into the coin count it compares against).
- Full server suite: **1763 tests, 0 failures**. Client compiles.

**Files touched:** `Proxy Server/src/server/game/npcs/KalphiteQueen.java`, `Proxy Server/src/server/game/players/{PlayerAssistant,PlayerHandler}.java`, `Proxy Server/src/server/game/players/combat/CombatAssistant.java`, `Proxy Server/src/server/game/content/DwarfCannon.java`, `Proxy Server/src/server/game/minigames/delves/{DelveInstance,DelveMaster,DelveSettings,DelveHud,DelveChestInterface,DelveLobby}.java`, `Proxy Server/src/server/game/players/packets/{AttackPlayer,ClickingButtons}.java` (earlier in the batch), `Proxy Server/src/server/game/players/actions/dialogues/DelveDialogues.java`, `Proxy Server/Data/cfg/delves/delves.cfg`, `Proxy Client/src/{game/client.java,ui/DelveHud.java,ui/StatusTimers.java,ui/HitStylePlugin.java,ui/PluginSidebar.java}`, tests `{KalphiteQueenTest,DelvePreparationTest,DelveChestInterfaceTest,DelveResultsTest,DelveTestFixtures,DelveHudTest,DialogueRegistryTest}.java`, `UPDATE_LOG.md`

## 2026-10-17 - Boss Delves: roster cleanup, real bosses, and the Kalphite Queen's second phase

**What changed:**
- **Three non-bosses left the reel.** The `Black dragon` (54), `Dagannoth` (1344) and `Kalphite Guardian` (1155) rows are gone from `bosses.cfg` and `drops.cfg`. None of them is a boss or demi-boss on this base, so they were filling the COMMON pool with ordinary monsters.
- **Six real bosses joined it.** `Mutant Tarn` (5421) replaces the Dagannoth, with `Slash Bash` (2060) and `Sea Troll Queen` (3847) filling the two remaining COMMON slots; `Giant Roc` (4972) and `Barrelchest` (5666) join the RARE pool and `The Inadequacy` (5902) joins LEGENDARY. Each has its own `drops.cfg` table (coins ALWAYS, a MAIN roll, tier-appropriate RAREs, and a UNIQUE for the legendary), and all six already have hardcoded attack/block/death emotes in `NPCHandler`, so no `npc_anims.cfg` rows were needed.
- **The Queen could not be attacked in her own arena.** `CombatAssistant.runNpcAttack` applied the open-world lore gate - "only full Verac's, never magic" - to every `KalphiteQueen.KQnpc` target. Nobody is in Barrows gear inside a delve, so every swing was reset with "Your attacks have no effect on the Queen." The gate is now skipped for a `DelveNpc` instance boss; the world boss is unchanged.
- **Her second phase never existed in the delve.** `NPCHandler` intercepts a delve copy's death in `DelveCombat.onInstanceBossKilled` before `killedKQ` can run, so killing form one cleared the wave and paid the loot with a full-health queen still standing. New `Data/cfg/delves/forms.cfg` links `1158 -> 1160`, and `DelveInstance.onBossKilled` now spawns the next form at the same tile on the same `bossIndex` and leaves the wave (and the loot) unresolved; only the last form's death clears the round. `1160` is authored in `bosses.cfg` with `enabled` = `false`, so it is a phase and never a roll of its own.
- **A form link cannot become an unclearable wave.** A self-link, a cycle, a parent that is not a boss, or a follow-up with no `bosses.cfg` row is reported at load and the row is dropped, leaving the boss a single kill. If the follow-up cannot be spawned at all (no free NPC slot, or no definition) the kill resolves the wave rather than guaranteeing a timeout wipe.
- **The corpse slot cannot eat the new form.** The next form is spawned while the dead copy still occupies its slot, so the spawner hands back a different slot; `onInstanceBossKilled` re-checks that the slot still holds the corpse before clearing it and its registration, instead of blindly nulling a slot that may now carry the live second phase.

**Verification:**
- New `DelveFormsTest` (8 tests): config parsing (a link, a self-link, a cycle, an unlisted follow-up, an unlisted parent, a missing file) and the shipped tables (the `1158 -> 1160` link, `1160` disabled, the removed ids absent, the six new ids present), plus an instance-level two-phase kill that keeps `FIGHTING` and pays nothing after the first form, spawns the second on a new slot with the same `bossIndex`, and clears the round and pays the loot on the second.
- `DelveShippedConfigTest` still passes: every tier has an enabled boss and every enabled boss has a drop table.
- Full server suite: **1749 tests, 0 failures**.

**Files touched:** `Proxy Server/Data/cfg/delves/{bosses.cfg,drops.cfg,forms.cfg (new)}`, `Proxy Server/src/server/game/minigames/delves/{DelveConfig,DelveArena,DelveInstance,DelveCombat}.java`, `Proxy Server/src/server/game/players/combat/CombatAssistant.java`, `Proxy Server/src/server/game/content/DwarfCannon.java`, `Proxy Server/test/server/game/minigames/delves/DelveFormsTest.java`, `docs/roulette/DESIGN.md`, `UPDATE_LOG.md`

## 2026-10-17 - Boss Delves phase 7: wave map, run results, `::delveaudit`, `PACKETS.md`

**What changed:**
- **The wave map is derived, not stored.** `DelveWaveMap` builds one stop per authored `waves.cfg` round, priced with the same `DelveMath.roundMultiplier` and `DelveMath.dangerRating` the reels and the chest preview use. A missing round is skipped rather than invented; `::delveaudit` is what reports the hole. `DelveWaveMap.lines` renders the road as chat lines and marks the next stop.
- **Players can read the road ahead.** New `::delveplan` (rights 0) prints the whole road from the round that is actually next. The chest's **"What am I risking?"** row now prints the next three stops underneath the stakes, weighted for the current party size.
- **Every run ends with a summary.** New `DelveResults` reports the depth reached, bosses felled, chest value banked or lost, the run's best single drop, and the party's downings/revives. The numbers come from new per-run counters on `DelveInstance` (`deathsThisRun`, `revivesThisRun`, `bossesKilledThisRun`), not from the CSV, so a disabled or unwritable log cannot turn the summary into zeros. It is sent at `finish`, on a voluntary withdraw, on a ledge Leave, and on an unanswered chest's auto-withdraw; a disconnected member's outcome still travels to their next login.
- **Paths that share a `RunOutcome` read differently.** A voluntary withdraw, a completed capstone and a server teardown are all `WITHDRAW`; `DelveResults.defaultReason` names which one happened from the cleared-round count, without adding a second state field.
- **`::delveaudit [strict]`** audits the authored data and the live registry: wave-road contiguity and the single capstone, a checkpoint before the end, every tier with an enabled boss, boss hitpoints/max hit, drop-table presence and MAIN/ALWAYS + RARE coverage, ranges and weights, resolvable item ids (when the item table is loaded), orphan tables, timer/cap/danger-band/multiplier sanity, the chest window versus the Continue lock, the ledge clock versus the chest window, distinct log paths and their headers, and every live instance (registry identity, phase never past its timer, members still pointing at it, no `LEFT` member on a roster, chest value inside the cap). Like `::delvebosscheck`, it is report-only; `strict` is the gate.
- **`docs/roulette/PACKETS.md`** documents the whole wire surface: the `delve:` text-frame tunnel (opcode 126, frame 24491, the sequence counter that defeats `sendFrame126`'s dedupe), the three payloads field by field, the chest/lobby/Continue interfaces with their frames, buttons and `dialogueAction` ids, the Delve Master's dialogue chain, the command surface, and the rule that a new feature never adds an opcode.

**Verification:**
- 33 new tests: `DelveWaveMapTest` (10 - derived plan, multiplier/danger parity, checkpoint/capstone flags, next-stop marker and limit, past-the-end, gaps, null config, party floor), `DelveResultsTest` (8 - every outcome wording, banked/lost/empty chest lines, best drop, withdrawal banking, run counters), `DelveAuditTest` (15 - shipped tables pass, a healthy live instance passes, and one broken thing at a time is named: disabled tier, missing round, no capstone, absent/unrollable drop tables, inverted range, narrow chest window, long ledge clock, shared log path, foreign header, mismatched member, ended instance).
- Full server suite: **1664 tests, 0 failures**.
- `CommandHandlerTest` counts updated (76 → 78 commands, 36 → 38 composites).

**Files touched:** `Proxy Server/src/server/game/minigames/delves/{DelveWaveMap (new),DelveResults (new),DelveAudit (new),DelveInstance,DelveChestInterface,DelveConfig,DelveCsv}.java`, `Proxy Server/src/server/game/players/packets/commands/DelveCommands.java`, `Proxy Server/test/server/game/minigames/delves/{DelveWaveMapTest,DelveResultsTest,DelveAuditTest}.java`, `Proxy Server/test/server/game/players/packets/commands/CommandHandlerTest.java`, `docs/roulette/{DESIGN.md,PACKETS.md}`, `UPDATE_LOG.md`

**Still manual:** the ledge tile's walkability/clipping/camera check with `::ledgetp` remains open from phase 5. It is the only item left open in the phase plan.

## 2026-10-17 - Boss Delves phase 6: chest interface, reconnects and restarts

**What changed:**
- **The chest gate is now visible.** `openChest()` shows every member their own menu through the new `DelveChestInterface`: living players get `Withdraw` / `Continue` / `Show chest` / `What am I risking?` (option interface 2480, `dialogueAction` 9840); a DEAD player gets the ledge prompt `Rejoin next round - empty chest` / `Leave the party` (option interface 2460). The menu is presentation only — every click is re-checked by `DelveInstance` for phase, membership, lock and whether the member already answered.
- **Continue takes two clicks.** The first click only arms the decision and draws `Yes - risk N gp` / `No - go back` (`dialogueAction` 9841); only Yes commits. An empty chest skips the warning. The row reads "unlocks in a moment" until `chest-lock-millis` has passed, and the lock itself is still enforced by the instance.
- **Both clocks are live.** `DelveChestInterface.tick` rewrites the option title frame every chest tick with the seconds left — the 60 s chest window for the living, and for a DEAD member the shorter of that and their 20 s ledge window.
- **Checkpoints skip the absent.** `applyCheckpoint()` banks `checkpoint-percent` of each stackable stack but now leaves a disconnected member's stacks whole, so a reconnect does not find half a chest.
- **Reconnects are matched by account, not object.** A real socket drop destroys the `Client`; the returning player logs in as a new object in a possibly different slot. `DelveManager.handleLogin` tries `reattachIfPossible` before writing the run off: `DelveInstance.acceptsReconnect` matches the account (case-insensitive), and `reattach` copies the live session (`copyPersistedFrom`), swaps the member reference, moves the leader ring and re-keys `decisions`, `ledgeDecisions`, `armedContinue`, `damageByPlayer`, `supportByPlayer`, `settledPayouts` and revive channels. Loot paid during the window is neither lost nor duplicated. A DEAD member is refused, per the design.
- **A grace expiry outlives the object.** When the window closes and the socket really is gone, the bank on that client will never be saved, so the outcome is recorded against the account in `DelveManager.PENDING_SETTLEMENT` — forfeited mid-round, kept at an open chest — and consumed by the next login. The record is removed on use, so it cannot be replayed.
- **A server restart banks instead of forfeiting.** `Server.requestStop` calls `DelveManager.get().clearInstances()` before `PlayerHandler.saveAllPlayers()`, so every live chest is settled while the character file is still writable.
- **`::collectrewards` is player-facing.** It runs at rights 0-3 and goes through `DelveMaster.tryCollect`, which refuses in an instance, in combat, while trading or duelling, and inside an 8-tick cooldown. The Delve Master's "Collect Delve rewards" option takes the same gate. A refusal is always free — the command only ever moves items the player already owns.
- **A small bug found by the new test:** `lastClaimTick` used `0` both as "never claimed" and as a real clock value. It now initialises to `-1` and the cooldown test compares `>= 0`.

**Verification:**
- 18 new tests: `DelveChestInterfaceTest` (11 — menu contents and exact gp/stacks, lock, arm/confirm/No/Yes, empty-chest shortcut, withdraw, teardown close, ledge rejoin/leave, and the claim gate for combat, cooldown and in-instance) and `DelveReconnectTest` (7 — fresh-login reattach across a slot change, loot earned while away, a non-matching account, the DEAD refusal, both pending-settlement outcomes and their one-shot consumption).
- Full server suite: **1631 tests, 0 failures**.

**Files touched:** `Proxy Server/src/server/game/minigames/delves/{DelveChestInterface (new),DelveInstance,DelveManager,DelveMaster,DelveSession}.java`, `Proxy Server/src/server/game/players/packets/commands/DelveCommands.java`, `Proxy Server/src/server/game/players/packets/ClickingButtons.java`, `Proxy Server/src/server/game/players/actions/dialogues/DelveDialogues.java`, `Proxy Server/src/server/Server.java`, `Proxy Server/test/server/game/minigames/delves/{DelveChestInterfaceTest,DelveReconnectTest}.java`, `docs/roulette/DESIGN.md`

**Still manual:** the ledge tile's walkability/clipping/camera check with `::ledgetp` remains open from phase 5. Phase 7 (wave map, results, `::delveaudit`, `PACKETS.md`) is next.

## 2026-10-17 - Boss Delves phase 5: arena, boss spawner, combat wiring

**What changed:**
- **The arena is real.** `DelveArena` places the party at `arena-start-x`/`-y` on the instance's own height plane when the run starts, teleports a rejoiner back at the next reel lock, and sends a member to `exit-x`/`-y`/`exit-plane` when they leave a chest or the run ends. Two parties share the tiles but never the plane, so they cannot see or hit each other.
- **Bosses are spawned copies.** Each wave creates one `NPC` per rolled boss via `NPCHandler.spawnInstanceNpc`, scaled with the frozen party size and the round. The copy is registered in `DelveNpc` by array slot with its owning instance and boss index; the 3-boss tile ring is `boss-spawn-radius`. `clearBosses` removes the copies and their registrations on wave end and teardown, and a kill skips the ordinary corpse/respawn/ground-drop path.
- **Fatal damage downs, it does not kill.** `DelveCombat.onDamageTaken` is called from both the player damage path (`Player.dealDamage`) and the NPC melee/range/magic path in `NPCHandler`. A delve member at zero becomes `DOWNED`, their hitpoints are held at 1 so the engine's `isDead` path never fires, and their chest survives. A downed member cannot walk or click.
- **Targeting and contribution are party-scoped.** `NPCHandler.getClosePlayer`, `getCloseRandomPlayer`, `attackPlayer` and `multiAttackDamage` admit only a living member of the boss's own party. `CombatAssistant.consumeNpcPending` (plus the DFS and godsword special paths) books damage against the attacker, and an outsider's damage is ignored. The revive click is handled on the fourth player option `Revive` through `FollowPlayer`/`AttackPlayer`; it walks the reviver closer when needed and starts the existing channel.
- **The round closes properly.** Killing the last boss pays that boss's own drop table (not always the first), removes the copy, auto-revives the downed, applies the between-round 50% hitpoint/prayer recovery, and opens the chest gate on a non-final wave.
- **AOE is covered.** `multiAttackDamage` now asks the same `canNpcTarget` filter, so a boss blast cannot hit a downed, dead or foreign-party player.

**Verification:**
- 19 new tests: `DelveArenaTest` (arrival/exit tiles, the real spawned copy's type/plane/HP/max hit, single/double/triple placement, clearing, rejoin restore) and `DelveCombatTest` (downing, target filters, contribution, boss kill/gate, revive click + channel interruption, between-round recovery).
- Full server suite: **1613 tests, 0 failures**. `Proxy Client` compiles (`gradlew compileJava`, Java 8 release).

**Files touched:** `Proxy Server/src/server/game/minigames/delves/{DelveArena,DelveCombat,DelveInstance,DelveNpc}.java`, `Proxy Server/src/server/game/{npcs/NPCHandler.java,players/Client.java,players/Player.java,players/combat/CombatAssistant.java,players/packets/{AttackPlayer,ClickNPC,FollowPlayer,Walking}.java}`, `Proxy Server/test/server/game/minigames/delves/{DelveArenaTest,DelveCombatTest}.java`, `docs/roulette/DESIGN.md`

**Still manual:** the ledge tile's walkability, clipping and camera view need one in-game check with `::ledgetp` (the target/AOE rules are automated). Phase 6 (chest interface, checkpoints, reconnect/restart) is next.

## 2026-10-17 - Boss Delves phase 4: reel overlay and result

**What changed:**
- **The reel is now visible.** A HUD panel spins on every member's screen while a wave is rolled and holds the answer through the countdown. `DelveInstance.beginSpin()` rolls the wave and then calls `DelveReel.spinning(this)`, so the overlay animates towards a result the server has already decided — the client never rolls anything.
- **Three messages, one contract.** `spin` carries the round, wave, animation timings, multiplier, danger band, the rolled tiers/bosses and the enabled roster the client cycles; `land` carries the result; `clear` takes the panel down when the fight starts, when a member leaves and on teardown. Fields are `:`-separated and the list fields use `;`/`,`, so an authored boss name can never shift a column (`sanitise` flattens any that would).
- **No new opcode.** The payloads ride on text frame `126` (dummy widget `24491`) with a `delve:` prefix, the same tunnel `cannon:`/`poison:`/`barrows:` use. A client that does not know the prefix ignores it instead of logging out on an unknown opcode.
- **A per-player sequence number** (`DelveSession.reelSeq`, transient) is stamped on every payload because `sendFrame126` drops a body identical to the last one on the same widget — a repeated `clear` would otherwise be swallowed and leave a stale panel.
- **`reel-overlay` (default `true`)** is a display switch in `DelveSettings`/`delves.cfg`. With it off the run rolls and starts exactly as before; only the closing `clear` is still sent.
- **The danger band on the reel is the round being spun**, from the same `DelveMath.dangerRating` the chest preview uses — a DOUBLE round on a Medium band with three players reads Extreme.

**Verification:**
- 10 new tests in `DelveReelTest`: both payload formats, the roster excludes a disabled boss, separator flattening, the spin → land → clear → teardown sequence, the switch, the party-wide broadcast, leaving mid-spin, and round 4's Extreme danger + double-wave reels at three players.
- `Proxy Client` compiles (`gradlew compileJava`, Java 8 release, no new warnings). Full server suite run below.

**Files touched:** `Proxy Server/src/server/game/minigames/delves/{DelveReel,DelveInstance,DelveSession,DelveSettings}.java`, `Proxy Server/Data/cfg/delves/delves.cfg`, `Proxy Server/test/server/game/minigames/delves/DelveReelTest.java`, `Proxy Client/src/ui/DelveReel.java`, `Proxy Client/src/game/client.java`, `docs/roulette/DESIGN.md`

**Status:** done - phase 5 (arena map and combat wiring) is next; the reel still shows the result over the arena the players are standing in until then.

## 2026-10-17 - Boss Delves phase 3: party lobby and party-request invites

**What changed:**
- **The Delve Master's second option is now a working party lobby.** "Join a delve party" forms a party (the clicker becomes leader) or opens the five-option lobby menu: Start, Invite, Party roster, Leave, Close. The menu shows the live party count and marks leader-only rows; it is redrawn for members who have it open when the roster or leadership changes.
- **Party-request invites are implemented from scratch.** The leader types a name into the client's existing name prompt (frame 187); the target gets an Accept/Decline prompt. Invitations expire on the tick (`invite-expiry-ticks`, 50 = 30 s), are rate-limited per inviter+target pair (`invite-rate-limit-ticks`, 17 = ~10 s), and are re-validated on accept (party full, started, disbanded, target already in a party).
- **Lobby lifecycle:** leaving costs nothing and writes no run row; leadership passes through the existing succession rule; the last member leaving ends the lobby; starting the run withdraws every pending offer and closes every prompt; logout/login clears invitations and rate history. The phase-2 arena placeholder (`ARENA_DIALOGUE`) is removed.
- **New dev tools:** `::delveparty` and `::delveinvite <name>`.

**Verification:**
- 23 new tests in `DelveLobbyTest` (offer/accept/decline, expiry, both rate-limit rules, cap on send and on accept, leader-only actions, succession, menu text, prompt lifecycle). Full suite: 1584 tests, 0 failures; `compileJava` clean.

**Files touched:** `Proxy Server/src/server/game/minigames/delves/{DelveLobby,DelveInvite,DelveInstance,DelveManager,DelveMaster}.java`, `Proxy Server/src/server/game/players/{Client,packets/JoinChat,packets/ClickingButtons,packets/commands/DelveCommands,actions/dialogues/DelveDialogues}.java`, `Proxy Server/test/server/game/minigames/delves/{DelveLobbyTest,DelveMasterTest}.java`, `Proxy Server/test/server/game/players/{actions/dialogues/DialogueRegistryTest,packets/commands/CommandHandlerTest}.java`, `docs/roulette/DESIGN.md`

**Status:** done - phase 4 (reel overlay + result) is next; the arena teleport still belongs to phase 5.

## 2026-10-13 - Re-applied the doors.cfg partner rows now that the guard makes them safe

**What changed:**
- **Re-applied the data change that was reverted earlier.** 10 doors had only a shut row and no row on the tile they swing to, so nothing could ever close them: `doorHandling` finds a door's open state purely by coordinate match. Added the 10 missing open-state rows (duel arena gates, cow gate, Al Kharid gates, the two Edgeville doors at `3101,3510`/`3101,3509`, `2816,3438`, `2949,3450`, `2952,3452`).
- **Moved the duplicate row.** `doors.cfg` had both `door = 3079 3497 0 -2 0` and `door = 3079 3497 0 -1 1` on the same tile. `doorHandling` walks the whole table and applies every coordinate match, so that door was moved twice per click. The open row now sits at `3080,3497`, which is where the shut row actually swings to. 42 active rows, `MAX_DOORS` is 50.
- **Why this is safe now when it was not before.** The earlier revert was because a `State 1` row on a normally-empty tile could be fired by a stray click, running `doorId--` and spawning a door on the shut tile with a drifted id. The state guard added just before this removes exactly that path: a close row only applies if `openDoors` already holds that door's shut tile.

**Verification:**
- New `DoorsConfigTest` reads the real committed `Data/cfg/doors.cfg` (via the `serverCfgDir` property, the same convention `RealCharacterFilesTest` uses) and asserts every open row resolves to a shut row, that no tile carries two rows, and that the count fits `MAX_DOORS`. All three would have failed against the previous file - the pairing one on 10 doors and the duplicate one on `3079,3497`.
- 1228 tests pass (was 1217 before this door work), `compileJava` clean.

**Files touched:** `Proxy Server/Data/cfg/doors.cfg`, `Proxy Server/test/server/world/DoorsConfigTest.java`

**Status:** done - still needs an in-game click on a door to confirm the doorway is passable and that it closes again.

## 2026-10-13 - Doors could not be closed: doorHandling had no idea what state a door was in

**What changed:**
- **`doorHandling` acted on every click, regardless of the door's state.** It matches rows on `(x, y, height)` alone and runs `doorId++` on any `State 0` row, so a second click on the shut tile ran the open branch again and walked the id one step further from its pair each time. Once the id left the pair, closing it was impossible because the id the close row expects no longer existed. The row table records positions; nothing recorded state.
- **Added the missing state, plus the guard.** `ObjectHandler.openDoors` holds the shut tile of every door currently standing open. A `State 0` row only applies if its tile is not already in that set; a `State 1` row only applies if it is, and removes it. Either way a repeat click is a no-op instead of an id drift.
- **Both states now agree on which tile "the door" is.** A `State 1` row names the *open* tile, so `closedTileOf` translates it back through `OPEN_OFFSET` before the guard can ask anything. That table mirrors the `State 1` branch offsets, so `(3079, 3497, -2, 0)` and its open row resolve to the same key.
- **Side effect worth knowing:** a duplicated row now harmless-instead-of-harmful. `doors.cfg` has `door = 3079 3497 0 -2 0` followed by `door = 3079 3497 0 -1 1` on the same tile; the guard makes the second row a no-op rather than a second move. That door still cannot close, and can't be fixed by the guard alone - it needs its open row moved to `(3080, 3497)`, which is the `doors.cfg` change that was reverted earlier and is safe to re-apply now.

**Still open / notes:**
- Doors whose rows only describe the shut state (11 in `doors.cfg`) have no row on their open tile, so nothing can close them. The guard does not create that row.
- The guard is in-memory only: a server restart with a door left open re-reads the config as shut, which matches the config's own `0 closed` convention.
- 4 new tests in `DoorStateGuardTest` pin `closedTileOf` against the branch literals and against real `doors.cfg` pairs. 1225 tests pass, `compileJava` clean.

**Files touched:** `Proxy Server/src/server/world/ObjectHandler.java`, `Proxy Server/test/server/world/DoorStateGuardTest.java`

**Status:** done for the drift half; the 11 rows with no open-state partner are still outstanding.

## 2026-10-13 - Doors blocked their own doorway: collision had no way to be removed

**What changed:**
- **A door never unblocked the tile it swung off, so the doorway stayed shut to path finding.** Player walking is decided server-side (`Walking` -> `PathFinder.findRoute` -> `Region.getClipping`), and collision was only ever *added*: the single mutation was `clips[..] |= shift`. So opening a door removed the graphic but not the collision, the BFS stopped on the last reachable tile, and the player appeared stuck "one tile away" behind an invisible barrier.
- **Added the missing half.** `Region.removeClip` mirrors `addClip`, and `Region.setDoorBlocked(objectId, x, y, height, type, face, blocked)` blocks or clears exactly the bits a door's own shape contributes - type 0 walls via two new lookup tables, type 9 diagonals via the solid footprint, both mirroring `addClippingForVariableObject`/`addClippingForSolidObject`. Nothing else on those tiles is touched, so this cannot open a neighbouring wall.
- **Wired into both door paths.** `Doors.handleDoor` (doors.txt) now clears the tile it vacates, captured *before* the move because `getNextFace` mutates the face in place. `ObjectHandler.doorHandling` (doors.cfg) clears the entry tile. Opening clears; closing clears the other tile, so the two are symmetric and leave no residue.

**Correction to my own earlier pitch:** I described this option as "stop the door moving and turn it in place, like `Doors.java` already does". That was wrong - **`Doors.java` moves the door too** (`d.doorX += xAdjustment`), and so does `DoorUtil.translate` in the forum thread the user linked, and so does `doors.cfg`. All three agree the open door relocates one tile, so relocating is the 317 convention and it was left alone. The bug was never the move - it was that collision did not follow the move.

**Still open / notes:**
- **`doorHandling` still has no state check** - it matches on `(x, y, height)` alone and increments `doorId` on every click of a `State 0` tile, so clicking the original tile again drifts the id instead of closing. This is the "cannot close" half of the report and is *not* fixed here.
- The open door's placement still goes through the generic clip path, so it may leave a clip on the tile it lands on. Watch for that in game.
- `MAX_DOORS` (50) still has no bounds check against `loadDoorConfig`.
- 4 new tests in `DoorClippingTest` pin the bit tables against the literals they were transcribed from (region data is not loaded under the test task, so the arithmetic itself cannot be exercised there). 1221 tests pass, `compileJava` clean.

**Files touched:** `Proxy Server/src/server/clip/region/Region.java`, `server/game/objects/doors/Doors.java`, `server/world/ObjectHandler.java`, `Proxy Server/test/server/clip/region/DoorClippingTest.java`

**Status:** partial - needs an in-game click on a door to confirm the doorway is passable.

## 2026-10-13 - Reverted the doors.cfg patch: it fed a system that moves doors and cannot unclip

**Correction to the entry below.** That entry is left as written, but its `doors.cfg` change has been **reverted**. Editing the data was the wrong layer - the problem is in `doorHandling` itself, and adding entries there is actively unsafe.

**Why the revert:**
- **`doorHandling` matches on `(x, y, height)` alone - no state check, no `break`.** So a `State 1` entry is not "this door is open"; it is "clicking here will run the close branch". The 11 entries the patch added sit on the *open* tiles, which are **empty tiles when the door is closed**. Any object on one of those tiles that survives `ClickObject`'s `Region.objectExists` guard would fire the handler, run `doorId--`, and spawn a door on the closed tile with a **drifted id** - a visible door turning into a different object or vanishing. That is a worse failure than the one it fixed.
- **The invisible barrier is a server-side pathfinding problem, not a data problem.** `Walking` routes with `PathFinder.findRoute`, which reads `Region.getClipping`. Clipping is mutated in exactly one place - `Region.addClip`, `clips[..] |= shift` - and **nothing anywhere clears a bit**. So opening a door *removes the graphic* (client-side packet 101) but never the collision. The BFS therefore stops on the tile before the door: the "always 1 tile away" and "invisible barrier" are the same bug.
- **`doorHandling` also moves the door to a neighbouring tile** (`x-1`/`y+1`/etc.) rather than turning it in place, and `clipSpawnedObject` then adds a *permanent* clip on that new tile. So each door opened leaves an extra blocked tile behind it, forever.

**Kept from the reverted entry:**
- `DoubleDoors.handleDoor` now returns `false` when `getDoor` matched nothing (was `true`, which made `Doors.handleDoor` claim success for every object in the game). Harmless today and still correct.

**Still true, still worth doing:**
- `doorHandling` has no state check - whichever way doors are fixed, this needs one.
- `MAX_DOORS` is 50 with no bounds check on `loadDoorConfig`.

**Status:** reverted - awaiting a decision on the real fix (unclip on open, and stop moving the door).

## 2026-10-13 - Gates could open but never close: doors.cfg was missing half of each door

**What changed:**
- **`ObjectHandler.doorHandling` is stateless by design, and `doors.cfg` has to supply both ends of every door.** It matches an entry on tile alone and picks "open" or "close" from that entry's **State** column - but that column is only ever read, never written, so clicking the same door twice just replays the same move. The swing only reverses because the door's *open* tile is itself a separate entry with `State 1`. 10 of the 32 entries had that partner; **11 did not, so those gates opened and could never be closed again.**
- Added the missing partner entry for each of those 11, derived from the two face tables `doors.cfg` already documents in its own comments (the first block is the closed branch, the block after "the door starts as opened" is the open branch). Every door now round-trips: open, then close, back to the original tile and face.
- **`(3079,3497)` was listed twice** with different faces. `doorHandling` takes the *first* match, so the second was unreachable - and it was wrong anyway: that face (`-1`, State 1) is the door's *open* position, which is one tile east. Moved to `(3080,3497)`, which is what makes the Edgeville diagonal door closable.
- **`DoubleDoors.handleDoor` returned `true` when `getDoor` matched nothing**, so `Doors.handleDoor` reported success for *every* object in the game. Now `false`. Nothing reads that result today, but it is what made "use the result" unsafe.

**Not bugs (checked and dismissed):**
- `ClickObject`'s `if (Doors.handleDoor(...)) { }` really does discard the result, but it is harmless: `handleGenericObject` opens with `if (act.equals("open") || act.equals("close")) return false;`, so every door is already carved out of the generic path. Left alone - suppressing the dispatch would cost the turn-to-face, door 2558's distance/offset case, and the 1516/1519 `y == 9698` walk-around, for no gain.
- `doubledoors.txt`'s 5th column is **height**, not a state - the 10 rows showing `2` are upstairs doors, and they load correctly.

**Notes / follow-ups:**
- `doors.cfg` went 32 entries -> **42**. `MAX_DOORS` is 50 and `loadDoorConfig` has **no bounds check**, so headroom is now 8 door lines before startup throws `ArrayIndexOutOfBoundsException`. Worth guarding, and it wants a test: nothing validates that a door has a partner, which is exactly why 11 were silently broken.
- `DoubleDoors.getNextLeftFace`/`getNextRightFace` look like they each have one drifted row in the open branch (left maps `2->1` and `3->2`, both landing on 2). Left unchanged - needs a double door checked in game first, since guessing would break the ones that work.
- 1217 server tests pass, `compileJava` clean. Client untouched.

**Status:** done, pending one in-game click on a newly closable gate.

## 2026-10-12 - Dummies faced south: the cache sets their turn speed to 0

**What changed:**
- **The dummies now actually face east.** The server was already sending the right facing every tick; it was being thrown away on the client. `EntityDef`'s turn speed (`anInt79`) is **0** for NPC ids 1488/1489 in the cache, and it is copied into `Entity.anInt1504`. `client.method100` - the only code that applies a direction to an entity - opens with `if (entity.anInt1504 == 0) return;`, so no facing was ever applied and the dummy sat at the model's authored orientation (south) while every layer above it worked perfectly. The client override now sets `anInt79 = 1024`.
- **1024 rather than the usual 32** (what every normal NPC has): a turn is applied in whole steps of `anInt1504`, so any value >= 1024 lands the turn in a single tick. A dummy is a fixture, not a person - it should already be facing east the moment it comes into view, not pivot towards it over the ~10 seconds 32 would take. Only turning reads this field.

**Where the facing is actually set (for future edits):**
- The direction is the server's, in `TrainingDummy.faceEast`: `npc.turnNpc(npc.absX + 1, npc.absY)` - "look at the tile to the east". West would be `absX - 1`, north `absY + 1`, south `absY - 1` (in this world larger Y is north). It is re-sent every tick on purpose: the client clears the focus tile after applying it, and a player only learns an NPC's facing while that NPC is in their local list.
- ⚠ **The spawn config cannot set this.** `NPCHandler.loadAutoSpawn` parses only `type x y height walkingType maxHit attack defence` into `newNPC`, which takes no direction - unlike object spawns, which do have one. NPC rotation is only ever the `turnNpc` call.
- The rotation is applied client-side as a geometric Y rotation of the model by `Model.method443` (sine/cosine tables), so it works on a model with no animation frames - which is what the dummies have.

**Notes / follow-ups:**
- This was the "⚠️ still not seen on-screen" caveat from the entry below, now root-caused: the failing layer was the client's turn speed, not the model or the server's facing.
- `installBin` run, so `bin/` carries the fix. Three new harness checks pin the non-zero turn speed and the Attack action/combat level, so this cannot silently regress again - an unguarded guard is one refactor away from being deleted. 706 client checks pass.
- Still no on-screen confirmation of the model's exact height/orientation; the swivel question is settled (there is none), but whether the object model sits at the right height is not.

**Status:** done, pending an on-screen look.

**Files touched:** `Proxy Client/src/def/EntityDef.java`, `Proxy Client/test/ClientHarness.java`.

## 2026-10-12 - Dummies reskinned to object 23921's model, and they no longer pay XP

**What changed:**
- **Both dummies now wear the model of object 23921** (`25177`) instead of the cache's NPC-side dummy art, so they look like the world's own "Dummy" object. This corrects the previous entry's claim that NPC model 14035 was "the same art as the training dummies already in the world" - it was the cache's *NPC* dummy art, which is not the object a player recognises as a training dummy. Object 23921 is the real thing: name `Dummy`, action `Attack`, model `25177`, 1x1, with object animation 6481 (not used - an NPC has no hit animation to drive here, so the dummy stays a still target).
- **Hitting a dummy now pays no combat XP at all** (melee, ranged, magic, hitpoints). A target that cannot be killed, never misses and never becomes "in combat" would otherwise be the best XP rate in the game at no cost or risk, which is exactly the abuse the pair would have invited.
- **The rule lives in one place:** new `TrainingDummy.paysCombatXp(npc)`, which every NPC combat XP payout site now consults - the swing award (`awardCombatXpOnSwingNpc`), the three "hit landed" fallbacks in `delayedHit` (ranged + magic) and `applyNpcMeleeDamage`, the barrage path in `appendMultiBarrageNpc`, and `DwarfCannon.applyHit`. PvP XP is untouched, and the cannon still dims/brightens splats exactly as before.

**Notes / follow-ups:**
- In `awardCombatXpOnSwingNpc` the `swingXpAwarded` flag is still set for a dummy even though nothing is paid - it is what stops the hit path paying the same damage out a second time, so skipping it would have *created* a double-XP bug rather than removed a payout.
- Clients need `installBin` (or `Compile.bat`) for the reskin to show; the server needs its usual rebuild. `bin/` was reinstalled here.
- New harness checks `Dummy: 1488/1489 wears object 23921's model (25177)` and `the two share a model and are told apart by name` (client, 703 checks pass). `TrainingDummyTest.aDummyPaysNoCombatXpAndEveryOtherNpcStillDoes` covers the XP rule.
- ⚠️ **Still not seen on-screen.** Wearing an *object* model on an NPC is a real rendering case rather than a config change - the model's origin and ground offset are authored for world placement, so the dummy could sit slightly high, low or the wrong way round. Worth a look in game; the facing is applied server-side every tick (`TrainingDummy.tick`), which is what a screenshot should confirm.
- Full server suite: 1216 of 1217 pass. The one failure is the pre-existing flaky `PrayerAltarTest > aSecondClickWhileOfferingIsNotASecondOffering`, which passes 3/3 in isolation and involves neither dummies nor combat XP.

**Status:** done, pending an on-screen look at the model.

**Files touched:** `Proxy Client/src/def/EntityDef.java`, `Proxy Client/test/ClientHarness.java`, `Proxy Server/src/server/game/npcs/TrainingDummy.java`, `Proxy Server/src/server/game/players/combat/CombatAssistant.java`, `Proxy Server/src/server/game/content/DwarfCannon.java`, `Proxy Server/test/server/game/npcs/TrainingDummyTest.java`.

## 2026-10-12 - Two training dummies: "0 Defence Dummy" and "Max Hit Dummy"

**What changed:**
- **Added two attackable NPCs at `3084,3495` and `3084,3493`, both facing east**, using the cache's own "Dummy" NPC (ids 1488/1489, model 14035 - the same art as the training dummies already in the world). Ids 1488-1507 are all named "Dummy" in the cache and carry no actions, no combat level and no stand animation, so on their own they are unclickable scenery. `def.EntityDef` gives 1488/1489 a name, an "Attack" action and combat level 1; 1490 upwards are deliberately left alone.
- **New `server.game.npcs.TrainingDummy`** holds the whole rule set: effectively infinite hitpoints, zero defence, no attack of its own, never walks, never retaliates, and re-faces east every tick (a client only learns an NPC's facing while it is in that player's local list, so setting it once at spawn would leave anyone who walks up later looking at a dummy facing nowhere).
- **The max-hit dummy always takes the attacker's ceiling**, so every splat on it is a maximum roll - including on a spell that rolled a miss, which is why `dummyMissed` also clears the failure flag (otherwise the client would draw the splash gfx *and* a max hit on top of it, and a barrage would discard the damage behind its own splash early-return).
- **Everyone can attack a dummy at once, in single or multi combat.** Both single-combat gates in `runNpcAttack` and the cannon's equivalent stand aside for a dummy, and `TrainingDummy.tick` clears `underAttackBy` every tick so the lock can never be left set by an attack packet that arrives after the tick.
- **Dummies skip the entire generic NPC tick** in `NPCHandler.process()`, the same shape the world-adventurer NPC already uses. That is what keeps them out of the death/respawn path; the damage itself still lands, because player hits are applied from the player's own tick.
- **Zero defence is load-bearing, not cosmetic**: the accuracy checks roll `Misc.random(defence)`, so 0 can never beat the attacker's roll and every attack lands on either dummy.

**Notes / follow-ups:**
- The max-hit dummy gives its ceiling on *every* attack, misses included, since "always hit your max hit" is not much of a read-out if it splats zero half the time. Say the word if accuracy should be respected instead.
- `CombatAssistant.dummyDamage` / `dummyMissed` are no-ops for every NPC but 1489, which is what makes them safe to sit in the middle of a damage roll; the dwarf cannon reads the same rule.
- New `TrainingDummyTest` (6 tests) pins the restraint: only 1488/1489 are affected, an ordinary NPC keeps its roll, and a tick restores health, drops the combat locks and faces east.
- `PrayerAltarTest` is pre-existing flaky and unrelated: it fails ~1 run in 6 on its own (`--tests '*PrayerAltarTest'`, no dummy code involved), with a different bone count each time. Left alone.
- Not yet validated on-screen.

**Status:** done. Both projects compile; the full server suite passes apart from the flaky `PrayerAltarTest` above.

**Files touched:** `Proxy Server/src/server/game/npcs/TrainingDummy.java` (new), `Proxy Server/test/server/game/npcs/TrainingDummyTest.java` (new), `Proxy Server/src/server/game/npcs/NPCHandler.java`, `Proxy Server/src/server/game/players/combat/CombatAssistant.java`, `Proxy Server/src/server/game/content/DwarfCannon.java`, `Proxy Server/Data/cfg/spawn-config.cfg`, `Proxy Client/src/def/EntityDef.java`.

## 2026-10-12 - Max hits pop: optional "Max hit pulse" under Hitsplat Style Icons

**What changed:**
- **New "Max hit pulse" setting** (widget 24473, on by default). Your own max hits now swell to **145%** of the splat's size and brighten from 170% to 215% over their first **10 cycles**, then settle back to the size and colour an ordinary hit has. The tint says *whose* hit a splat was and holds for its whole life; the pulse is a burst of motion meaning "that one was your best roll", which is why it is its own toggle rather than folded into "Your hit tint".
- **The pulse is driven off the splat's remaining cycles**, not a stored start time: `elapsed = HITSPLAT_CYCLES - cyclesRemaining`, linear decay to zero by `PULSE_CYCLES`. The client books a splat with the cycle it *expires* on and keeps no birth time, so counting backwards from expiry is the only stateless way to do it - and it makes `pulseScalePercent` safe to call per frame.
- **`Sprite.drawScaledTint(x, y, w, h, tintPercent)` is new, and `drawScaled` now delegates to it with 100.** Scaling and brightening have to happen together, and the existing `drawScaled` had no colour scaling. Delegating keeps one copy of the sampler instead of two; a 100 tint is arithmetically identical to the old body, so existing `drawScaled` callers are unaffected. Nearest-neighbour sampling, so the enlarged splat stays blocky like the rest of the client's scaling rather than going soft.
- **Scaled from the splat's centre**, so the pop swells in place instead of sliding down-right (which a naive top-left scale would do).
- **`PluginSidebar`'s `ids`/`names`/`tips` overload ladders had a gap at 5 arguments** - they went 4 straight to 6. Added the missing 5-argument overloads, which is why the new sub-setting fits the existing style rather than needing a flattened layout.

**Notes / follow-ups:**
- The pulse is scale + brightness only; the damage number is not scaled or recoloured with it, so the number stays legible and stays put while the art pops around it.
- `maxHitPulse` is independent of `yourHitTint`: with the tint off and the pulse on you get a normal-coloured splat that still pops, and with the pulse off a max hit is just the brighter 170% art.
- The three tuning constants (`PULSE_SCALE` 45, `PULSE_CYCLES` 10, `PULSE_TINT` 45) are the whole tuning surface and are only judged by eye.
- Client-only; the max bit already rides the hit-source byte, so the server needs no rebuild and nothing about the wire changed.
- Still not validated on-screen.

**Status:** done.

**Files touched:** `Proxy Client/src/ui/HitStylePlugin.java`, `Proxy Client/src/ui/Sprite.java`, `Proxy Client/src/game/client.java`, `Proxy Client/src/ui/PluginSidebar.java`, `Proxy Client/src/game/ClientSettings.java`, `Proxy Client/src/ui/Interfaces.java`.

## 2026-10-12 - Cannon hits count as the owner's, so they are not dimmed for them

**What changed:**
- **Dwarf multicannon splats are now attributed to the owner.** `DwarfCannon.applyHit` had the owner `Client` in hand all along (`fire(player, cannon)` where `player` is resolved from `cannon.owner` each cycle), so it now passes `c.playerId` to `handleHitMask`. For the owner the cannon's splats read exactly like hits they swung themselves - undimmed, and bright on a max roll; for everyone else they dim like any other player's damage. This closes the gap noted in the previous entry, which had left cannon hits deliberately unattributed.
- **`isMaxRoll` moved from `CombatAssistant` to `HitMarker`** as a public static, with the 15 `CombatAssistant` call sites updated to `HitMarker.isMaxRoll`. The cannon rolls its own damage and needs the same "was that a max?" answer, and one definition of max hit beats two that can drift. Same behaviour, no logic change.
- **`rollDamage`'s ceiling extracted as `cannonMaxHit(Client)`** so `applyHit` can judge a max hit against the exact ceiling the damage was rolled from rather than re-deriving the formula. The clamp to `n.HP` still happens in `rollDamage` only, which is what makes a 40 against a monster on 3 hitpoints correctly *not* a max hit.

**Notes / follow-ups:**
- The owner lookup is per-cycle and already guarded (`player == null` stops the fire event), so `cannonMaxHit(c)` cannot be reached with a null owner. `applyHit` keeps its own null check regardless.
- Cannonballs only ever hit NPCs (`validTarget`/`findTarget` take `NPC`), so there is no PvP cannon path to attribute.
- Client needs no change: it already reads the hit-source byte off the NPC update block, so the server-side attribution is the whole fix.
- Still not validated on-screen.

**Status:** done.

**Files touched:** `Proxy Server/src/server/game/content/DwarfCannon.java`, `Proxy Server/src/server/game/players/combat/HitMarker.java`, `Proxy Server/src/server/game/players/combat/CombatAssistant.java`.

## 2026-10-12 - "Your hit" tints the splat instead of boxing it

**What changed:**
- **The gold/white box outline is gone.** Replaced with tinting, which is how OSRS reads: splats somebody else dealt are drawn at **62%** of their normal colour, and a splat that was **your own maximum is drawn at 170%** - lifted towards white so the one roll worth noticing is the loudest thing on screen. Numbers dim with the art (grey `0xB4B4B4` instead of white), because a dimmed splat still carrying a full-bright white number would read as half-done.
- **A translucent overlay could not do this.** The splat art is a shape on a transparent field, so blending a rectangle over its 24x24 bounding square would tint the scene behind it into a visible box. New `Sprite.drawSpriteTint(x, y, tintPercent)` therefore scales each opaque pixel per channel, clamped at 255 so brightening cannot wrap back round to dark, and filters on the same `!= 0 && != -1` transparency test as plain `drawSprite` so a 100% tint is pixel-identical to before.
- **The setting is now "Your hit tint"** (widget 24472, renamed from "Your hit outline"; field `yourHitOutline` -> `yourHitTint`). The persisted key `hitStyleYourHit` was left alone, so an existing on/off choice carries over.
- **Splats on your own character are never dimmed.** That is damage coming at you, so it is the last thing that should recede; `isMuted` takes an `onOwnCharacter` flag and the render site passes `obj == myPlayer`. Consequence: NPC hits on you and PvP hits on you stay full strength, while other players' hits on a shared target dim.
- **No server change.** This reuses the per-viewer hit-source byte added earlier today exactly as it was - `HitMarker` decides "mine" and "my max" while the block is serialised, and the client just renders it differently.

**Notes / follow-ups:**
- The two percentages (`OTHER_HIT_TINT`, `MAX_HIT_TINT` in `HitStylePlugin`) are the whole tuning surface and are only judged by eye; 62 and 170 are a first guess, not measured against OSRS.
- The damage number cannot brighten on a max hit - it is already drawn in white, so only the art has headroom, which is why the max hit reads as a brighter splat rather than a brighter number.
- Not validated on-screen. Client compiles; nothing about the wire protocol changed since the previous entry, so the server does not need rebuilding.

**Status:** done.

**Files touched:** `Proxy Client/src/ui/Sprite.java`, `Proxy Client/src/ui/HitStylePlugin.java`, `Proxy Client/src/game/client.java`, `Proxy Client/src/game/ClientSettings.java`, `Proxy Client/src/ui/PluginSidebar.java`, `Proxy Client/src/ui/Interfaces.java`.

## 2026-10-12 - "Your hit" splat outline, with a gold box for a max hit

**What changed:**
- **New "Your hit" setting under Hitsplat Style Icons, on by default** (widget 24472). Splats the local player dealt are outlined in white; one that was also their maximum gets a gold double box. Damage alone cannot show this: a 24 and a 28 look identical whether the ceiling is 30 or 40.
- **The server answers "was this mine?" per viewer.** A hitsplat is written into every nearby player's update stream, so the answer differs per recipient and only the server knows whose stream it is writing into. `Player`/`NPC` `appendHitUpdate`/`appendHitUpdate2` now append one byte after the style byte, from new `HitMarker.pack(attackerId, viewerId, wasMax)`, and `appendPlayerUpdateBlock`/`appendNPCUpdateBlock` gained a `viewerId` (5 call sites, all in `PlayerHandler`/`Player`). The max bit is withheld from anyone but the attacker.
- **Attribution is recorded where hits are booked**, mirroring the style byte exactly. `CombatAssistant` tags NPC melee/ranged/magic, PvP melee/ranged/magic, and the gmaul and DFS specs. `Player`/`NPC` gained `attributeHit(slot, attacker, wasMax)` for the longer list of paths that still write `hitDiff` directly, plus a four-argument `handleHitMask` for the ones that go through it. Splats nobody claims - recoil, poison, an NPC's own blow, a heal - keep `NO_ATTACKER` and reach the client as "not yours".
- **Max detection is `damage >= ceiling`,** with the ceiling being `calculateMeleeMaxHit()`, `rangeMaxHit()` or the spell's max recomputed at the splat. `Misc.random(n)` rolls `0..n` inclusive, so equality is the best roll; a ceiling of 0 (no spell selected) never counts, so anything unjudgeable stays unmarked rather than being called a max. Recomputing rather than carrying the ceiling from the roll site is safe because these are pure functions of stats, prayer and gear.
- Outline drawn as a border only (`DrawingArea.method335` at opacity 256), before the damage number so the text sits on top. `HitStylePlugin` holds the colours, the `WIRE_MINE`/`WIRE_MAX` bits and the predicates the renderer asks.

**Notes / follow-ups:**
- **Wire break:** the hit block gained a byte on both sides, so client and server must be rebuilt together. An old client against the new server would desync on the first hit.
- **Dwarf cannon hits are deliberately not attributed** - it would mean threading the owner through `DwarfCannon`. Cannon splats therefore have no outline even though they are yours; worth adding if it looks wrong in game.
- Not validated on-screen. Both sides compile; the outline placement and the max-hit threshold are the parts only play can confirm.

**Status:** done.

**Files touched:** `Proxy Server/src/server/game/players/combat/HitMarker.java` (new), `Proxy Server/src/server/game/players/HitUpdate.java`, `Proxy Server/src/server/game/players/Player.java`, `Proxy Server/src/server/game/players/PlayerHandler.java`, `Proxy Server/src/server/game/npcs/NPC.java`, `Proxy Server/src/server/game/players/combat/CombatAssistant.java`, `Proxy Client/src/game/Entity.java`, `Proxy Client/src/game/client.java`, `Proxy Client/src/ui/HitStylePlugin.java`, `Proxy Client/src/ui/PluginSidebar.java`, `Proxy Client/src/ui/Interfaces.java`, `Proxy Client/src/game/ClientSettings.java`.

## 2026-10-12 - Boost boxes drop the "/max"; Vengeance rebounds get a red skull beside the splat

**What changed:**
- **Boosted stats, "+N" off, now shows the boosted level alone.** It read "107/99" and now reads "107", in both forms of the plugin (the potion-icon infobox and the text overlay) since the setting drives both. The base level was noise: a boosted player is looking at the number their skill is actually at, and the extra "/max" only made the label long enough to fight the infobox for width. Tooltips updated to match ("+15 instead of 114/99" -> "instead of the boosted level").
- **New "Vengeance skull" setting under Hitsplat Style Icons, on by default** (widget 24471). A splat from Vengeance recoil now wears the red Vengeance skull beside it. Recoil is the one splat whose source is not a combat style, so the style table had no icon for it and a rebounded hit looked exactly like a hit the server had not labelled.
- **How the skull gets there.** `HitStyle.VENGEANCE = 3` is a new wire value; `CombatAssistant.appendVengeanceNPC` (NPC attackers, added earlier today) now tags the NPC's splat with it, and `appendVengeance` (PvP) tags the attacker's slot-2 splat via the new `Player.setHitStyle2`. The client resolves it in `drawHitStyleIcon` before the skill lookup, which is what keeps it off the skill table - 3 is inside the byte's range, so the decoder needs no special casing.
- **Transparency and size.** The art is the Vengeance spell's own icon (widget 30306, the same sprite `StatusTimers` uses), read back from the widget because `imageLoader` is unpack-time only. Two new `Sprite` helpers do the rest: `withoutBackground()` strips the spellbook's opaque plate off a copy, `trimmed()` crops the transparent canvas margin, and the new `scaled()` resamples to `HitStylePlugin.ICON_SIZE`. The XP skill icons are exactly 18x18 with no padding (verified against `Sprites/Gameframe/XP/skill *.png`), so 18 makes the skull match them rather than dwarfing a melee hit's sword; it fades with its splat through the existing opacity path.
- **Drive-by, needed for the above to be trustworthy.** `applyRecoil` booked the ring/deflect splat with `setHitDiff`/`setHitDiff2` and no style, so it inherited whatever the slot held earlier in the tick - which with the new tag could have put a Vengeance skull on a ring-of-recoil splat. Both branches now set the style to `NONE` explicitly, via a new `Player.setHitStyle`, mirroring what `applyRecoilNPC` already did on the NPC side.

**Notes / follow-ups:**
- `HitUpdate.clearUpdateFlags` and `NPC.clearUpdateFlags` both reset the style bytes every tick, so a splat only inherits a style from an earlier writer in the *same* tick - which is why the `applyRecoil` fix was the only one that mattered. The other direct splat writers (thieving stun, wall safes, prayer drain, gmaul spec) inherit within a tick too, but nothing sets VENGEANCE on those paths.
- Not validated on-screen. The skull is the one piece that can only be judged in game: if widget 30306 is not the skull art on this cache, the icon silently does not draw rather than drawing something wrong.
- `Sprite.trimmed()`/`scaled()` are general helpers and currently have one caller.

**Files touched:** `Proxy Client/src/ui/HitStylePlugin.java`, `Proxy Client/src/ui/Sprite.java`, `Proxy Client/src/game/client.java`, `Proxy Client/src/ui/PluginSidebar.java`, `Proxy Client/src/ui/Interfaces.java`, `Proxy Client/src/game/ClientSettings.java`, `Proxy Server/src/server/game/players/combat/HitStyle.java`, `Proxy Server/src/server/game/players/combat/CombatAssistant.java`, `Proxy Server/src/server/game/players/Player.java`.

## 2026-10-12 - Double-hit tile borders made whole; NPCs now take vengeance recoil too

**What changed:**
- **Why the ring looked uneven.** A tile's floor quad covers the tile boundary its neighbour drew a moment earlier, and because the scene pass walks far to near the covering tile is always the nearer one. A tile that plotted its outline only at its own seam therefore kept its far edges and had its near edges shaved thin or wiped out - the "some boards thicker, others thinner or missing" in the screenshot.
- **Every edge is now plotted twice** in `WorldController.drawFloorTileMarker`: once by the marked tile and once by the neighbour across that edge, whose floor is the one that covers it. Whichever of the two seams comes last wins, so no edge on the ring can stay shaved. Only the border is repeated, never the fill - a second fill pass would compound the 70/256 blend and darken - and `plotPixel` writes solid, so a repeat re-plots the same pixels instead of thickening them. Still drawn at the tile's floor seam, so the whole marker stays under the cannon and the monsters standing in it.
- **`CannonOverlay`'s accessor split again**, this time into a predicate rather than a colour: `doubleHitActive(plane)` gates the per-tile work (so nothing is paid while the feature is off) and `doubleHitAt(localX, localY, plane)` answers for a single tile. `doubleHitColor` and the now-unused `tileColor` are gone; the fill takes the colour from the new public `DOUBLE_HIT_COLOR`. Spots are untouched and still deferred.
- **NPC melee/range/magic vengeance.** `appendVengeance` could not be reused: it takes the attacker's *player* index and writes the recoil to `PlayerHandler.players[attacker]`, which is only correct in PvP - an NPC has no player index to pass. New `CombatAssistant.appendVengeanceNPC(damage, npcIndex)` mirrors it with the roles reversed, and `NPCHandler` calls it on both NPC-to-player paths: the melee/range/magic tail of `applyDamage` and the multi-combat `multiAttackDamage`. It is called before the hit is subtracted (the lethal-blow guard reads pre-hit hitpoints) and on the final post-prayer damage, matching the PvP path.

**Notes / follow-ups:**
- Kept the PvP quirk that a lethal blow spends the buff and shows "Taste Vengeance!" but deals no recoil, rather than diverging the two paths.
- The border repair can re-plot an edge over a model that was drawn at a seam in between (a monster standing on a marked tile with a nearer marked neighbour). The lines are tile boundaries, so the overlap is at the monster's feet edge, but it is the one place where a marker pixel can land on a model - worth watching in game.
- Not validated on-screen; server change only compiles, and neither path has a test.

**Files touched:** `Proxy Client/src/scene/WorldController.java`, `Proxy Client/src/ui/CannonOverlay.java`, `Proxy Server/src/server/game/players/combat/CombatAssistant.java`, `Proxy Server/src/server/game/npcs/NPCHandler.java`.

## 2026-10-12 - Cannon double-hit tiles now sit on the floor instead of over the cannon and the players standing in them

**What changed:**
- **Why they were on top.** Every tile marker (hover, destination, ground/object markers, player indicators, boss timers, cannon) is *collected* as its floor tile goes past and then *painted* from `finishScene()` - after all ground and every model. That is deliberate: those markers are meant to stay legible over whatever stands in them. It is exactly wrong for the cannon's double-hit squares, which are supposed to read as ground, so they covered the cannon and any player or NPC on the tile and looked like floating red quads.
- **The double-hit marker is now painted at the floor seam.** `WorldController.method314` draws a tile's floor (`method315`/`method316`) and then submits that tile's models, so a marker painted between the two lands under everything submitted later - the cannon on the neighbouring tile included - and on top of the floor it tints. That is the new `WorldController.drawFloorTileMarker`, called from the one point where both of a tile's ground meshes are down and `flag1` says the floor was actually drawn. There is no depth buffer in this renderer: models composite in submission order, far to near, which is what makes the seam work.
- **Split the overlay's one colour accessor into two.** `CannonOverlay.doubleHitColor` (painted with the floor) and `CannonOverlay.spotColor` (still deferred, unchanged), with `tileColor` kept as the combined convenience. `tileNeedsOverlay` and `paintTileOverlays` now ask for `spotColor`, so a tile carrying only a double-hit marker no longer drags the deferred path along and paints itself twice.
- Everything else - hover, destination tile, ground markers, object markers, player indicators, boss timers, agility, cannon *spots* - is untouched and still draws on top.

**Notes / follow-ups:**
- The one deliberate imprecision: on the rare tile that owns a linked ground (`aClass30_Sub3_1329`), the linked mesh's own objects are submitted before the floor marker, so the marker lands over them. Left as-is - it is a floor tint and that configuration is essentially the stacked-roof case.
- Not validated on-screen yet; worth a look at a cannon in both a grassy and a dirt/path spot to confirm the squares tint the path rather than vanishing under it.

**Files touched:** `Proxy Client/src/scene/WorldController.java`, `Proxy Client/src/ui/CannonOverlay.java`.

## 2026-10-12 - Client crashed on login: the status timers were loading lunar spell art after the media loader had been torn down

**What changed:**
- **The crash.** `StatusTimers.ensureSprites` called `RSInterface.imageLoader(35, "Lunar/LUNARON")` on every HUD frame, which NPE'd on the first login (`aMRUNodes_238` null) and killed the render thread. `RSInterface.unpack` *creates* the media cache, loads every interface through it, and then **nulls it on the way out** (`RSInterface.java:268`) - so `imageLoader` is usable only during unpack. `mediaCacheAvailable()` already existed for exactly this distinction and I did not use it. My own fault, and the previous batch's entry was premature in calling it verified.
- **The art now comes from the spellbook widgets**, which hold the sprites that were loaded at unpack time and keep them for the session: `30306` for the Vengeance spell (its `sprite2` is `LUNARON 36`) and `30298` for "Vengeance Other" (`sprite2` is `LUNARON 35`). No loader call, so nothing to tear down. `withoutBackground()` still strips the plate off a copy, leaving the shared widget sprite the spellbook draws untouched.
- **Removed the latching flag.** `triedVengLoad` was set on the *attempt*, not the success, so a single early failure would have disabled the icons for the whole session even once the widgets existed. Each sprite now latches on its own success (`if (vengSpell == null) ...`), and a draw before the interfaces load returns early instead of committing to the fallback art.
- **Hardened `imageLoader` itself**: it now returns `null` when the media cache is gone, rather than dereferencing it. That is the same thing it already returns on a load failure, and it means a future caller that forgets `mediaCacheAvailable()` loses a sprite instead of the whole client. Audited every other caller - they are all interface-construction code running inside `unpack`, and `MakeX` already guards its two with `mediaCacheAvailable()`; the only offender was mine.

**Notes / follow-ups:**
- Neither the harness nor any test touches `RSInterface` (it cannot: `unpack` needs a real cache), so this one has no test and the login itself is the gate.

**Files touched:** `Proxy Client/src/ui/StatusTimers.java`, `Proxy Client/src/ui/RSInterface.java`.

**Status:** done and built - `clean check installBin` green, harness **700/700**, now with the lunar art read from the widgets. **Needs one login to confirm.**

## 2026-10-12 - TokHaar-Kal install fully reverted; hitsplat icon fade, cannon ranged tag, fixed-size infoboxes, and a transparent Vengeance icon with a primed indicator

**What changed:**
- **TokHaar-Kal is out completely, and the spawn ceiling stays.** `ItemDef`'s `case 24500` and the `item = 24500` row in `Data/cfg/item.cfg` are gone, and the four cape meshes added for it (`62592`, `62575`, `62582`, `90500`) are deleted from the cache's `Raw`. `item.cfg` was restored byte-for-byte from HEAD, which also undid the re-encoding the earlier edit had accidentally left on 13 accented item names. The raised id ceiling is kept, as asked - `ItemDef.MAX_ITEM_ID = 65534` and the `streamIndices` floor, with the server's `Config.MAX_ITEM_ID` already matching. The pre-existing `item = 19111 TokHaar-Kal` placeholder row is untouched (it was there before this work and is unrelated to the install).
- **Hitsplat style icons fade out.** New `Hitsplat icon fade: On/Off` setting (default On) under the hitsplat style icons group. The style icon holds its configured opacity for the first three quarters of the splat's life, then ramps to nothing over the last quarter, so it leaves *with* the splat instead of blanking a frame early. Only the icon fades; the damage number is unchanged. Off restores the old behaviour exactly.
- **Dwarf multicannon damage is now tagged ranged**, so its hitsplats carry the Ranged icon like every other ranged hit.
- **Infoboxes are now strictly square, and text shrinks to fit them.** The panel was widened to fit a wide value (`max(size, textWidth + 6)`), which made one box in a column wider than the rest; that is gone. The value now picks the widest of the four client fonts that still fits the box, keeping the player's chosen font whenever it fits and falling back only when it cannot. Default size is now **32px**; the size setting is unchanged and still overrides it.
- **The Vengeance infobox icon is transparent, and a primed effect gets its own white skull.** The lunar spell art is an opaque white plate with the skull drawn on it, and the button path had been hiding that by treating pure white as transparent - the infobox path did not. A new `Sprite.withoutBackground()` flood-fills the plate away from the four corners on a *copy*, leaving the shared widget sprite (and therefore the spellbook) drawing exactly what it drew before. Verified against the real PNGs: it removes the 84 plate pixels and leaves the skull silhouette intact.
- **The white skull is "Vengeance is cast and primed", which is not the same as the cooldown.** `vengUntil` is the 30-second recast cooldown; the server's `vengOn` instead runs from the cast until the first *damaging* hit spends it, and can therefore outlive the cooldown. The two are now tracked separately: the white skull (the "Vengeance Other" art, `LUNARON 35`) appears on the cast graphic and comes down on the first splat of type damage-or-poison with `damage > 0`. A zero splat (blocked/missed) and a heal deliberately do not spend it, matching the server, which only rebounds for `damage > 0`. It also clears on death, which is where the server resets `vengOn` and `lastVeng` together.
- **The primed skull clears on the hit, not on the cooldown - and that is a fix, not a preference.** The old code cleared the *cooldown* display on the "Taste Vengeance!" overhead. That text is overhead chat, not a chatbox message, so it never reached the client's message hook; and clearing the cooldown there would have been wrong anyway, since the server keeps refusing recasts for the full 30s from the cast whether or not the effect was spent.

**Notes / follow-ups:**
- `Sprite.drawScaled` skips only `0`, while `Sprite.method349` (the unscaled path) skips `0` *and* `-1`. So a sprite with a pure-white plate looks right at 1:1 and shows a white square when scaled - which is exactly how this bug hid for so long, and it is a latent trap for the next icon. Left alone here on purpose: fixing it changes a shared draw path, and the one shipped case is fixed at the source instead.
- The `VENG_PRIMED_MAX` backstop is 5 minutes. Every real way the effect ends clears the flag directly, so this only exists so that a world hop or server restart - a fresh player server-side that the client sees no event for - cannot leave the skull up for the rest of the session.

**Verified:** client `clean check installBin` green, harness **700/700**; server `check` green. The sprite strip was simulated pixel-for-pixel against `LUNARON 35/36.PNG` before wiring it in.

**Files touched:** `Proxy Client/src/ui/{StatusTimers,Sprite,InfoBoxes,HitStylePlugin,PluginSidebar,Interfaces}.java`, `Proxy Client/src/game/{client,ClientSettings,Entity}.java`, `Proxy Client/src/def/ItemDef.java`, `Proxy Server/src/server/game/content/DwarfCannon.java`, `Proxy Server/Data/cfg/item.cfg`, cache `C:\Users\llrbi\Biohazard.474\Raw\{62592,62575,62582,90500}.dat` (deleted).

**Status:** done and built. **Not confirmed in game** - the four visible things to check in one live run are: the fade ramps in the last quarter of a splat, the cannon's splats carry the bow, a six-figure infobox value stays inside a square box, and the Vengeance icon is a bare red skull with a white skull beneath it while primed that goes out when you are hit.

## 2026-10-11 - Correction: the TokHaar-Kal worn cape was never oversized, and the "fix" for that was reverted

**What changed:**
- **The previous entry's scale conclusion was wrong, and the change it justified was reverted.** I then added a per-item worn scale (`wornScale`) plus a vertical offset, shrinking the mesh to 75/128 (0.59) and shifting it -55. That is gone: `anInt165`/`anInt200` are back to `62582` used exactly as authored, and `setDefaults`, the new field and the `method196` transform block were all removed. Net effect of this entry is **no behaviour change** - only corrected comments and one reworded diagnostic.
- **Why the earlier numbers were wrong: `Model.method465` drops orphan vertices.** A worn item is drawn through the player composite, and `method465` carries over only vertices a face actually REFERENCES (it de-duplicates on position). 54 of `62582`'s 516 vertices are orphans, and they held the extreme coordinates. So:
  - raw vertex box (what I measured before): `115 x 229 x 72`, y`[-186,43]`
  - **what actually renders: `52 x 153 x 28`, y`[-173,-20]`**
  - for comparison, the worn capes that already work here: `9639` Obsidian `54x139x39` y`[-165,-26]`, `9638` Fire `76x135x38`, `323` Sara `54x128x39`, `18920` Attack `64x159x41` y`[-177,-18]`.
  So `62582` was always in the right envelope, and scaling it made a correct cape 40% too small and too low.
- **The `62575` rejection still stands, for the right reason.** Measured the same way so it is not an orphan artefact, its drawn mesh is `507 x 689 x 469` - genuinely boss-scale, so it stays unused and was correctly the cause of the invisibility.
- **`Model.java` diagnostic reworded** so it no longer asserts the empty catch was what made the player vanish (that was never proven; the GL path is the likelier mechanism). It now just says an undrawable model used to leave no trace at all, which is accurate.
- **Tooling:** `tools/item-probe` gained a `rendered` line (and a `-worn <itemId>` mode) that builds the model through the player composite and prints the size the renderer is actually handed. Reading the raw vertex box is what caused this mistake, so the probe now reports both.

**Verified:** `installBin check` green, harness **700/700**; probe confirms `62582` renders at `52x153x28` with no transform applied.

**Files touched:** `Proxy Client/src/def/ItemDef.java`, `Proxy Client/src/model/Model.java`, `Proxy Client/tools/item-probe/ItemProbe.java`.

**Status:** done and built. **Still needs one live look** - the cape is geometrically correct, so if it is still wrong on screen the cause is something other than size, and the client's own `skipping MODEL uid ... Reason: ...` line is the next thing to read.

## 2026-10-11 - Wearing TokHaar-Kal made the player invisible: the male worn model is 5x oversized, and the GL path was dropping the whole actor over it

**What changed:**
- **The cause is scale, not the wiring.** `62575` decodes as 2322 verts / 3354 faces spanning **576 x 709 x 469**; a worn cape is about 54 x 139 x 39 (Obsidian) and the female mesh below is 115 x 229 x 72. The source declares it as the male worn model, but it is 5x the player's height - a boss/NPC-scale asset - so it was the wrong article to trust.
- **Why that costs the whole character.** `GlSceneRenderer.drawModel` undoes the ENTIRE model when any one of its faces is unrepresentable (`allRepresentable()` -> `batch.rewind`), and the software body it runs underneath is then replaced by that GL frame, so a dropped model is an actor MISSING from the finished image. The user's own overlay confirms GL is on. A cape far outside the range a worn model occupies is exactly the kind of geometry that trips this.
- **`ItemDef` case `24500` now uses `62582` (the correctly scaled mesh) for both `anInt165`/`anInt200`**, with the reason recorded at the case rather than here.
- **One-shot abort report.** The bare `catch` around `method483` in `Model.method443` used to swallow a mid-draw failure silently - which is why this showed up as nothing at all in the log. It now prints once per process, naming vertices/faces/rows, so an undrawable model is identifiable from a log. The temporary per-frame `M483 ...` spam was removed.

**Verified:** cape sizes measured by decoding the real files through this client's `Model.method460`/`method462`; `clean check installBin` green, harness **700/700**.

**Files touched:** `Proxy Client/src/def/ItemDef.java`, `Proxy Client/src/model/Model.java`. Scratch probe folder `_capeprobe/` removed.

**Status:** done and built, but **not yet confirmed in game** - needs one live equip. If the player is still missing, the client already logs `skipping MODEL uid ... Reason: ...` per distinct actor, and that line names the exact decline. Swapping in an authentic male mesh is a one-line change if one is found decoding near the female's size; `62717`/`62743`/`62746` in `Raw/` were checked and are not it (`62717` is oversized too).

## 2026-10-11 - Item spawn failed because of a hardcoded 20000, not the item table; id ceiling raised to the 16-bit maximum

**What changed:**
- **The bug was not the item table.** Something was capping spawns at 20000 in three separate places: `Commands.handleItemSpawnCommand` and both `ItemSpawnSearch.handleSelect` and `handleAmount`. Item 24500 was above it, so `::item 24500 1` answered "Could not complete spawn request." and - because the `return` sits after the if/else - never even reached the name-search fallback. `Config.ITEM_LIMIT` was never consulted, which is why the cape looked broken while the limit looked fine.
- **The three bounds are now one method**, `ItemSpawnSearch.isSpawnableId`, with a test that fails if the cape is refused again.
- **Ceiling raised 25000 -> 65535 (`Config.MAX_ITEM_ID = 65534`).** 100k is not reachable: item ids are an unsigned 16-bit word on the wire (`writeWordBigEndianA(itemId + 1)` out, two-byte read into `RSInterface.inv` in), so 65534 is the last id that survives the round trip and 65535 would wrap to -1. Going higher means changing the protocol, which the client/server compatibility rule rules out.
- **Client headroom widened to match.** `ItemDef.streamIndices` was `totalItems + 50000` (61812 here), so `forID` discarded any id from 61812 up before the custom switch could run. It is now at least `MAX_ITEM_ID + 1`, so the whole server-reachable range resolves.
- **No existing item changes.** `item.cfg`'s highest row is 24500 and nothing sits at or above 25000, so raising the ceiling only lifts the bound for future customs - the set of loaded items is identical.

**Verified:** `ItemProbe 24500` still resolves to the real cape models; `ItemProbe 63000` (previously past the client guard) now resolves; `65535` is still refused. Server `check` green with 5 new `ItemSpawnSearchTest` cases; client `clean check installBin` green, harness 700/700, warnings at the 100 baseline.

**Files touched:** `Proxy Server/src/server/Config.java`, `.../game/players/packets/Commands.java`, `.../game/content/ItemSpawnSearch.java`, `Proxy Server/test/server/game/content/ItemSpawnSearchTest.java` (new), `Proxy Client/src/def/ItemDef.java`.

**Status:** done and built, but **not confirmed in game** - `::item 24500 1` needs one live run. One thing deliberately left alone: `ItemAssistant.getItemName` and the two `ShopAssistant.getItemShopValue` overloads scan all of `ITEM_LIMIT` for an id that indexes the array directly, so their linear scans are now 2.6x longer. That is a few hundred microseconds and only on shop/examine paths, so it was not worth the churn - noted here in case a shop ever feels slow.

## 2026-10-11 - TokHaar-Kal: the authentic worn models are now wired in (completes the entry below)

**What changed:**
- `ItemDef` case `24500` no longer borrows the Obsidian cape. It now points at the real models: inventory/ground `62592`, male worn `62575`, female worn `62582`, with the source's own zoom `1616`, rotations `339`/`192` and offset `-4`.
- The three files were copied into the cache's `Raw/` folder, which is where `client.preloadModels()` picks loose models up (`Raw/<id>.dat` -> `Model.method460`).
- Rotation and offset were mapped **by opcode, not by name**. The source's `modelRotation1`/`modelRotation2` are opcodes 5/6, which this client reads into `modelRotationY`/`modelRotationX`; both clients feed the same two arguments into the same rotation call, so the numbers carry over unchanged.
- Verified both ends: each file decodes through this client's own `Model.method460`/`method462` (`tools/item-probe`), and `ItemProbe 24500` reports `modelID 62592 ... equip male 62575 female 62582`.

**Files touched:** `Proxy Client/src/def/ItemDef.java`; cache `C:\Users\llrbi\Biohazard.474\Raw\{62592,62575,62582}.dat` (added only - `90500.dat` is left in place).

**Status:** done - `clean check installBin` green, harness 700/700. **Not yet seen in game**, and that is the one thing left: the source cache is a much later revision, so the cape wants a look for scale on the back and for whether the worn model's animation frames line up with this client's skeletons. Swapping the inventory model back to the supplied `90500` is a one-line change if preferred.

## 2026-10-11 - TokHaar-Kal: the worn models exist after all, extracted from a downloaded cache (no code change yet)

**What changed:** Nothing in the client - this is the answer to "can the worn model be got from somewhere", and the answer is yes, with the files now sitting on disk.

- **The item definition was recovered from a downloaded client.** `F:\Download Archive\Necrotic-Client-1.1.0\...\cache\definition\ItemDefinition.java:1266` has `case 19111` for `TokHaar-Kal` with the ids the report was missing: inventory/ground model `62592`, male worn `62575`, female worn `62582`, plus zoom `1616`, rotation `339`/`192`, offset `-4`, and `Wear`/`Drop`. Note `19111` is the same id the previous batch already used for the placeholder name, so the upgrade path needs no id change.
- **The models were pulled out of a cache, not a loose file.** `F:\Download Archive\New folder (2)` is a **new-format** cache (`main_file_cache.dat2` + ~37 index files) - a completely different container from this client's `main_file_cache.dat` + `idx0..4`. Its sector layout was resolved by brute force rather than assumed (520-byte sectors, 8-byte header `fileId:2 part:2 next:3 type:1`, 99% of index-7 chains validating end to end), and each model entry turned out to be `[type:1][stored u32][plain u32][gzip stream]` - the declared plain size is what confirms a clean unpack.
- **All three decode through THIS client's own loader.** `tools/item-probe` `ItemProbe -model` runs the real `Model.method460`/`method462`: all three return `ITEM_PROBE_MODEL_OK`. `62592` 271 verts / 288 tris (bbox 120x27x191, centred, i.e. the inventory article), `62575` 2322 / 3354 (bbox 576x709x469, authored around the shoulders, i.e. the male worn article), `62582` 516 / 790. The `FF FF` footer that made them look foreign is the client's own format switch, not an incompatibility - `Model.method460` picks the header offset from exactly those two bytes.
- **Extracted files:** `_cape_models/62592.dat`, `62575.dat`, `62582.dat` (3920 / 46392 / 11577 bytes). `_capeextract2.py` reproduces them.

**Files touched:** none in the client or server - three extracted `.dat` files under `_cape_models/` plus `_capeextract2.py`. Read-only against `F:\Download Archive\New folder (2)` and `Necrotic-Client-1.1.0`.

**Status:** done for the models, **not wired in**. `ItemDef` still borrows the Obsidian cape's worn models (`9639`/`9641`), so the cape still looks wrong on the back. Worth knowing before wiring: the download is a much later revision, so the worn model should be checked in game for scale and for whether its animation frames line up with this client's skeletons.

## 2026-10-11 - Tropical tree 14: the palm canopy is now culled behind the ridge that hides it

**What changed:** Report 14 is fixed from the coordinates, and the two entries below were aimed at the wrong tree.

- **The tree the report points at is a four-plane stack in map square 43,42** (world 2752-2815 x 2688-2751), not one of the 4845/4846 pairs the earlier sweep found in 38,44 and 38,45. At world (2753, 2709) it is trunk `4845` on plane 0, stems `4846` / `4847` on planes 1 and 2, and the leaves **`4848` on plane 3**, its base 720 units up the stack and its 368-vertex model (1706) topping out 98 units above that.
- **Nothing in the vanilla renderer can cull it.** `method323` is the only terrain test scenery gets, and the occluder walls feeding it are 360 units tall; the ridge in front of the reporter (world 2763, 2727, plane 0) stands **1056 units above the eye**, and the canopy top 754. So the plane 3 pass drew the canopy over ground that had already been painted - the painter-order limit the entry below named, but reachable without a depth buffer because the ground that hides it is a *height*, not a wall.
- **New: a sight-line terrain cull.** `WorldController.terrainHidesSightLine` walks the tiles under the line from the camera eye to the object's own model top (two samples a tile) and culls when the base plane's blended ground sits more than `TERRAIN_OCCLUSION_SLACK` (16) above it. Applied at both scenery passes, and only for scenery **above the base plane** (`l <= anInt442 ||`), so ordinary ground scenery never pays for it. The top vertex alone is the right test, not a shortcut: anything whose highest point projects behind a ridge is behind it whole.
- **It degrades toward drawing, never toward hiding.** Off-map samples return `MIN_VALUE` and are skipped, a null plane or a map too small to hold the line culls nothing, and objects within 128 units of the camera are never culled. The maths is a pure static helper so the harness can pin it: 13 new assertions including the reporter's own geometry (culled with the ridge, drawn without it, drawn from the ridge top looking down), the slack boundary from both sides, and terrain **behind** the canopy.

**Files touched:** `Proxy Client/src/scene/WorldController.java` (predicate + both call sites), `Proxy Client/test/ClientHarness.java` (13 assertions).

**Status:** done - client `gradlew check` **700/700**, `installBin` synced. Verified offline against the real map data (`_occl3.py`: the ridge clears the sight line by 353 units at the worst sample, so the 16-unit slack is nowhere near it), but **not yet seen in game**: worth a look from (2776, 2750) facing south-west. Side effect to watch for: canopy that peeks past the *edge* of a ridge is culled with the rest of the object, so a narrow ridge in front of a wide canopy could now hide too much.

## 2026-10-11 - Tropical tree 4845: found the two-plane placement that causes the leaf clash (no code change yet)

**What changed:** Nothing in the client or server - this is the reconnaissance for report 14 and it corrects the guess in the entry below.

- **Where the trees are, read out of the map rather than guessed.** A scan of all 1226 region entries in `Data/world/map_index` finds **340 placements of object 4845, every one of them in four adjacent regions** - map squares 38,44 / 38,45 / 39,44 / 39,45, i.e. **x 2432-2560, y 2816-2944** (one jungle area, not the whole world). All are `type 10` scenery, `orientation` 0-3.
- **The same tiles carry a second copy of the tree one plane up, as object 4846.** Region 38,44 holds 2696 objects on plane 0 and exactly 2 on each of planes 1-3; the two plane-1 objects are 4846 at the same (x,y) as two of the 4845s. Region 38,45 is the same shape: 1349 on plane 0, 11 on plane 1, and those 11 are 4846 at the same coordinates as 11 of the 4845s. So **4845 is the plane-0 tropical tree and 4846 is its plane-1 twin, placed deliberately on both levels.**
- **That is the mechanism behind the report.** `WorldController.method313` draws the scene **one whole plane at a time** (`for(k = anInt442; k < anInt437; k++)`, planes 0 then 1 then 2 then 3), and the client has no depth buffer anywhere, so **submission order is occlusion**. Every plane-1 tile - including a tree canopy - is therefore submitted after every plane-0 tile, so a canopy on plane 1 paints over a cliff face nearer to the camera whose ground is on plane 0. Nothing about this is recent: `method313`/`method314`/`method319` are byte-identical to `_client-backup-20261004-145741` (verified by diff of the whole file - the only changes are the `SceneRasterizer.dispatchGroundTriangle` seam, the `tileHoverAllowed()` gates, the Agility overlay and the package/import header).
- **The vanilla mechanism that is supposed to prevent it is the plane shift, and it is map-driven.** `ObjectManager` calls `WorldController.method276(x, y)` for every tile whose **plane-1 settings bit 2 is set** (`(aByteArrayArrayArray149[1][y][x] & 2) == 2`), which shifts that whole column down a plane so the upper level is drawn in the base pass - this is how bridges and tree platforms stay behind the terrain. A plane-1 tree on a tile where that flag is **not** set is drawn in the late plane-1 pass and has nothing to hold it back. The tile the reporter is standing on decides which of the two they are seeing, which is why the coordinates are the missing piece.
- **The occlusion test is also asymmetric, and that is the one code-level candidate.** Ordinary scenery is drawn at `method314:1470` behind `!method323(...)` - the test that skips a tall model whose top is hidden by terrain. Objects belonging to a shifted column's under-ground are drawn at `method314:1216` with **only** the roof check and **no** `method323` call at all, so they can never be culled by terrain in front of them.

**Files touched:** none - read-only reconnaissance (`Data/world/map_index`, `Data/world/map/*.gz`, `Proxy Client/src/scene/WorldController.java`, `Proxy Client/src/scene/ObjectManager.java`).

**Status:** blocked on one fact from the reporter, not on code. The attached screenshot could not be inspected (images cannot be read in this session - verified with a control image), so the remaining unknowns are the exact tile and the camera direction. Asked for coordinates or a screenshot with the minimap visible. Two candidate fixes are on the table and the choice matters: (a) give the shifted-column object path the same `method323` terrain test that ordinary scenery gets - small, but it changes multi-level draw behaviour and wants an in-game check on a bridge; (b) restore `renderer=gl`, whose depth buffer and per-corner ground depths are the real fix for painter-order occlusion, though that path is parked and unverified.

## 2026-10-11 - QOL batch: short infobox numbers, green Vengeance timer, bank drag delay, 1k/1m/1b prompts, tile hover rules, alt-drag, agility overlay, TokHaar-kal, hit-style icons, one-action-a-tick fletching and pickpocketing, Izzy labels

**What changed:**

- **Infobox values above 999 are shortened** (`999`, `1k`, `1.1k`, `12.5k`, `3m`) so a counter can no longer widen its box. One decimal is kept below 100 of a unit; under a thousand is untouched.
- **The Vengeance cooldown reads green on the opaque red skull.** The icon is fully opaque again and the countdown is drawn in green (`StatusTimers`), which is what the report asked for - red on the red icon was unreadable.
- **Bank drag: auto-scroll waits a second.** Holding an item near the top or bottom of the bank used to scroll the instant the cursor entered the band; `client.dragScrollHold`/`DRAG_SCROLL_DELAY` now require the cursor to stay in the band (~1s) before each scroll step.
- **Enter-amount prompts accept `1k`/`1m`/`1b`.** `client.parseAmountInput` reads a trailing multiplier, so bank Withdraw-X/Deposit-X, shop Buy-X and trade offer counts all take shorthand. A bare suffix means one (`k` = 1000); anything unreadable is still 0.
- **Status bars sit closer to the panel edge** so each bar covers the stone pillar whole, instead of leaving a sliver of pillar showing outside it.
- **Tile Markers hover obeys two new rules.** Middle mouse held suppresses the hover until release, and the hover is dropped over the inventory/chatbox HUD (transparent mode made the tile sit behind the pack). Both are one predicate, `client.tileHoverAllowed()`, used by the ground hover and re-checked when the scene finishes so a stale tile cannot outlive the cursor.
- **Holding Alt to move an overlay can no longer walk the character.** `MenuSystem` swallows the world click while Alt is down over the game area, so dragging an infobox no longer fires the click underneath.
- **New Agility overlay plugin** (`ui/AgilityPlugin`, on by default): outlines the clickbox of every agility obstacle in the scene, RuneLite style, read straight off the tile's objects so multi-tile obstacles are found without knowing their shape. Coloured `0x00C8FF` with a light fill.
- **TokHaar-kal is in the game**: item `24500`, model `90500` moved into the cache's `Raw`, stats and metadata from the report's screenshot (attack `+4/+4/+4/+1/+1`, defence `+12` all, strength `+8`, value 10m, weight 1.814 kg, Wear/Drop). The earlier placeholder id `19111` keeps the old name only.
- **New hit-style icon plugin** (`ui/HitStylePlugin`, on by default, opacity configurable 0-100% in steps of 25, default 100%): the XP-drop sword/bow/rune sprites are drawn to the left of a hitsplat to show whether melee, ranged or magic dealt the damage, for both players and NPCs. The style is tagged server-side (`HitStyle`, `HitUpdate`, `CombatAssistant`, `NPCHandler`) and travels with the hitsplat.
- **Fletching arrows/bolts/darts is one action a tick when clicking fast.** A click queues one batch and the ticked action runs at most one per tick, instead of the old wall-clock throttle that ate clicks and made fast clicking useless.
- **Pickpocketing is one roll a tick, and never from a stun or a fight.** `CycleEventHandler.getTick()` stamps the roll so a second click in the same tick cannot pay out twice; a failed attempt stamps the stun immediately, so the clicking-while-stunned trick no longer banks XP; and an NPC that is in combat (under attack, has a killer, dead, or dying) refuses the attempt outright.
- **Cap'n Izzy No-Beard's course menu marks the unfinished courses.** Barbarian Outpost, Brimhaven Arena and Agility Pyramid read `(Not Complete)` on the right of the option while `Agility.courseIsComplete` says so.

**Files touched:** `Proxy Client/src/ui/{InfoBoxes,StatusTimers,StatusBars,AgilityPlugin,HitStylePlugin,Sprite,MenuSystem,PluginSidebar,Interfaces}.java`; `Proxy Client/src/game/{client,Entity,ClientSettings}.java`; `Proxy Client/src/scene/WorldController.java`; `Proxy Client/src/def/ItemDef.java`; `Proxy Server/src/server/content/skills/{Fletching,Thieving,Agility,AgilityCourseMenu}.java`; `Proxy Server/src/server/game/players/{ActionHandler,NpcInteraction}.java`; `Proxy Server/src/server/event/CycleEventHandler.java`; `Proxy Server/src/server/game/players/combat/{HitStyle,CombatAssistant}.java`; `Proxy Server/Data/cfg/item.cfg`; cache `Raw/90500.dat`; tests `ThievingTest` (new), `AgilityTest`, `FletchingTest`.

**Status:** done for 13 of the 14 reports - client `gradlew check` **687/0** with `installBin` synced, server `gradlew check` green at **1205 tests, 0 failures** (`ThievingTest` 12/12, `AgilityTest` 41/41, `FletchingTest` 61/61). One pre-existing flaky test, `PrayerAltarTest.offeringAgainAfterAWalkDoesNotRunTwoLoops`, failed on the first run and passed on re-run and in the full suite afterwards; it is wall-clock/tick alignment, not this batch. **Report 14 (tropical tree `4845` leaves showing through terrain) is NOT fixed and needs one more thing from the reporter.** What I established instead of guessing: `ObjectDef 4845` is a plain 1x1 scenery object (model `1676`, three recolours, no flags), and the scene walk itself is untouched - `WorldController.method313`/`method314`/`method319` are **byte-identical** to the pre-OpenGL backup (`_client-backup-20261004-145741`), the only scene differences being the `SceneRasterizer.dispatchGroundTriangle` seam, the `tileHoverAllowed()` gates and the new agility overlay. So this is either the vanilla 317 per-tile painter order (a tall model on a near tile is painted over terrain in front of its canopy, which only a depth buffer fixes - that is the parked GL path) or a placement/map artefact at the tile the reporter is standing on. Both need the spot: **a screenshot with the minimap visible, or the coordinates**, and the answer to "is the tree behind the mountain or the mountain in front of the tree".

## 2026-10-11 - Agility: rope swings face the right way, stones keep a beat, Barbarian roof and Pyramid landings fixed

**What changed:**

- **Wilderness rope swing `2283` now turns the player north before the swing**, so the animation no longer plays back the way they came (the client draws it facing whatever direction the player last walked). New `Obstacle.face(x, y)` turns them to the row's landing tile; the Barbarian swing uses it too.
- **Stepping stones `2311` now jump, land and stand for a tick before the next jump.** Each hop draws the 741 jump and moves the player onto the stone in the same tick, then spends the next tick standing on it - six hops in twelve ticks. Running them back to back put the player on stone six while the first jump was still playing, which read as one long slide.
- **Barbarian rope swing `2282` now starts from the southern bank** (`2548..2553, 3553..3558` - the bank a player running up from the crumbling walls stands on) and ends on `(2551, 3549)`, the platform tile the rope hangs from, which the map connects to the log balance's start. It was previously pinned to tiles either side of the rope and timed out doing nothing.
- **Barbarian roof: the ledge and ladder are now start areas with absolute ends.** The probe showed plane 1 above the net is a set of small walk-pockets, one per obstacle, and a pocket cannot be walked out of - which is what "stuck on the first floor" was. The net's landing pocket reaches the ledge's start, and the ledge's end is now always `(2532, 3547)` (one tile from the ladder) rather than four tiles west of wherever the player happened to be standing. The ladder `3205` matches both roof pockets the course puts the player down in and comes back down to `(2532, 3546, 0)`, so getting off the roof is always one click.
- **Pyramid gap `10882` and cross ledge `10886` now end on fixed tiles:** `(3368, 2832)` and `(3360, 2831)`. They were offsets from the player, so the same crossing from a tile either side landed on the gap itself - a tile nobody can stand on - and the player arrived stuck and unable to click the ledge. The gap's start also reaches back to `(3375, 2840)`, where the plank before it ends, because the walkway between the two does not walk in both directions. The probe confirms both landing tiles are standable on h1, that the gap's landing pocket reaches the ledge's start tile `(3364, 2831)`, and that the ledge's landing pocket reaches the stair start `(3357, 2832)` that climbs to the summit, so the whole tier is walkable end to end.

**Files touched:** `Proxy Server/src/server/content/skills/Agility.java`; `Proxy Server/test/server/content/skills/AgilityTest.java`.

**Status:** done - `gradlew check` green (1345 tests), `AgilityTest` 39/39 (new tests pin the Wilderness swing's facing, the stone jump-land-stand pacing, the Barbarian rope/ledge/ladder landings, and the Pyramid gap and ledge landings including from the plank's landing tile). Coordinates were checked against the real map with `gradlew agilityProbe` (`server/tools/AgilityProbe.java`), not guessed. Still wants the in-game look the user was already doing: the Barbarian roof and parts of the Pyramid tier are genuine pocket collision - tiles that do not walk between each other - so if a click still does not take, the report to bring back is which tile the player was standing on.

## 2026-10-11 - Agility: crossings walk their own line (the "runs around the log" fix)

**What changed:**

- **A `WALK` obstacle now walks a straight line instead of asking the path finder for a route, which is the actual cause of all three in-game symptoms.** A log over water, a tightrope over a drop and a ledge over nothing are tiles the map marks unwalkable - on purpose, or anyone could cross them without the obstacle - so the route the path finder returned went *around* the obstacle (the wide pink loop, animation playing the whole way), and when the far side was unwalkable too it returned nothing and the obstacle moved the player (the "emote in place then teleport", which was the stall fallback firing). `Agility.queueLine` now fills the walking queue by hand, one waypoint per tile along the line the obstacle defines, and a new `Player.forceWalk` tells the walk loop to take those tiles as given. Logs, tightropes, planks and ledges all cross visibly, animation playing while walking. A crossing is also pinned to **walk** speed - the player runs by default, so a seven tile log was being crossed in three and a half ticks.
- **Nothing can leave the player frozen any more.** `busy` is what the walking packet refuses on, so a crossing that ended without clearing it froze the player on the spot with no way out but a relog - the "I can't move in that building" report. Every exit now goes through one `Agility.release()`: arrival, the stall fallback, a cancellation, and the event *throwing* (the cycle-event handler funnels an exception through `stop()` too, which the events now override). An event that stops itself on purpose marks itself first, so an obstacle that hands over to a second event stays busy across the join instead of freeing the player a tick early.
- **Stepping stones draw the jump before they move the player**, which is what the Wilderness run was missing - the lead-in is on the first hop only, so a six stone crossing is seven ticks rather than twelve.
- **Barbarian rope swing `2282` and other platform obstacles now match an area, not one tile.** Pinned to a single tile, a click from a tile either side walked the player somewhere else, or - when that tile was not reachable - quietly timed out after 30 ticks having done nothing at all, which is the "clicking it does nothing" report. Widened for the Barbarian rope swing, net and ladder, and the Pyramid gap `10882` (the reported `(3370, 2831)`).
- **Ape Atoll's default teleport is `(2760, 2750, 0)`,** the course gatehouse, not the old glide drop-off.

**Files touched:** `Proxy Server/src/server/content/skills/Agility.java`; `Proxy Server/src/server/game/players/Player.java`; `Proxy Server/test/server/content/skills/AgilityTest.java`.

**Status:** done - server `gradlew check` green, `AgilityTest` 33/33 including new coverage for the line `WALK` walks (one tile a tick, no sideways drift), clipping being suspended only for the crossing, walk-not-run speed, the hop lead-in and its pacing, and that `busy`/`forceWalk` are cleared on all three ways a crossing can end. Still wants an in-game look: the **Pyramid gap `10882` landing tile** (currently 5 tiles west of wherever the player stands - wide start box, but the direction and distance are a guess), the **Barbarian ladder `3205`** destination, and the **Pyramid heights**, none of which can be checked from the server code alone. Brimhaven is unchanged and still parked by request.

## 2026-10-11 - Agility: one obstacle table, six courses, course-locations menu

**What changed:**

- **`Agility` is now a single data-driven table, one row per obstacle, and the animation plays while the player crosses.** Three behaviours cover all of them: `WALK` writes the obstacle animation over the player's stand/walk/run/turn renders and puts them back on arrival (logs, ropes, planks, ledges); `HOP` jumps one tile per animation (stepping stones); `JUMP` starts the animation and relocates part way through it (nets, branches, swings, walls, gaps, pipes, ladders). The old handlers played a one-shot emote and *then* walked, so the emote finished on the spot - and the nets, branches and pipes teleported first, so the whole animation played at the far end. All six courses are in the table (Gnome Stronghold, Barbarian Outpost, Brimhaven Arena, Agility Pyramid, Ape Atoll, Wilderness) with their levels, per-obstacle XP, lap bonus XP, agility points and teleport tiles.
- **A click away from the start tile now walks the player there first** instead of silently doing nothing, and a second click during a crossing is swallowed rather than paying out twice. `Walking` returns early while `isBusy()`, so a click cannot fight a crossing, and death and teleportation both call `cancel()` (which restores the renders).
- **Cap'n Izzy No-Beard's option 2 is now "Agility Course Locations"**, opening a two-page menu (options 1-4 = Gnome/Barbarian/Brimhaven/Pyramid + "More courses...", then Ape Atoll/Wilderness/Back) that level-checks before teleporting. `agilityPoints` is persisted in `PlayerSave`.
- **Two bugs found while testing the wiring, both of which would have made the feature dead in game.** (1) `ClickingButtons`' option cases end by resetting `dialogueAction = -1`, so opening the menu with a plain `break` wiped the very marker the menu claims its own option buttons with - the menu would draw and then answer nothing. It now returns, like the neighbouring `case 420`. (2) `Player.getHeightLevel()` returned a dead field that nothing ever wrote, so it answered `0` for every player on every plane: the height-gated obstacles (Gnome tree branch, Barbarian ledge) were unreachable, and `withinDistance(x, y, h)` (used to broadcast still graphics) was silently disabled. It now reads `position.heightLevel` and the stale field is gone; nothing read it.

**Files touched:** `Proxy Server/src/server/content/skills/{Agility,AgilityCourseMenu}.java` (new); `Proxy Server/src/server/game/players/{ActionHandler,Player,PlayerSave}.java`; `Proxy Server/src/server/game/players/packets/{ClickingButtons,Walking}.java`; `Proxy Server/src/server/game/players/actions/dialogues/OptionDialogues.java`; `Proxy Server/src/server/game/players/PlayerAssistant.java`; `Proxy Server/test/server/content/skills/AgilityTest.java` (new).

**Status:** done - server `gradlew check` green, new `AgilityTest` 25/25 (tick-by-tick crossings, render swap/restore, lap bonus + points, spam-click, level gate, walk-to-start, cancel on death/teleport, menu routing and teleports, and a real click through `ClickingButtons` for the dialogue option itself). Wants an in-game look: the animation ids marked `(?)` in `Agility`'s constants block (827, 1115, 840) and the Pyramid/Brimhaven coordinates are the weakest, and the heights now actually apply, so the Barbarian ledge and the Gnome branch ladder are the ones to try. Brimhaven has **no server-side object spawns** - `ObjectManager.loadCustomSpawns` has none and `Data/cfg/global-objects.cfg` is an empty template - so the arena has to come from the map cache; if the objects are not there in game, that is where they belong.

## 2026-10-11 - Infobox groups persist, size/font settings, dual-ammo box, bolt enchanting balanced

**What changed:**

- **Grouped infoboxes survive a restart.** `OverlayManager.load` now materialises panels from every saved `ov.groupOf.*` key before it restores groups, because a status-timer or boost infobox has no panel at load time and its membership was silently dropped. **Singular infoboxes no longer offer "Detach"** (they were never attached); the menu is Detach / Flip / Delete only for a grouped box, and Detach now pulls that one box back out. Attaching is done by holding Alt and dropping an infobox onto another one, which is now wired in `endDrag` via `attachAt` - the old flow only merged groups, so "Detach" had been the only way in.
- **Infobox size and font settings, RuneLite style.** New Client Display options: **Infobox font** (Small/Regular/Bold/Fancy), **Infobox size** (16-48px, default 32), and **Outline infobox text**. Every icon infobox is now a square of that size with its sprite scaled to fit, so a spell icon, a potion, and a bundle of arrows all land in the same box - which also fixes the oversized Vengeance box.
- **Two ammunition infoboxes.** `AmmoOverlay` draws the ammo slot *and* the weapon slot when both hold ammunition (darts in hand plus bolts quivered), under separate ids `ammo`/`ammoHand` so each can be moved and grouped. Thrown names (darts, knives, javelins, throwing, toktz) count for the hand slot.
- **Bolt enchanting is OSRS-faithful and no longer absurd.** One cast converts **10 bolts**, spends **one set of runes**, plays that tier's animation/graphic (719/720/721 with 114/115/116/452), and pays the per-cast experience from `boltData` **once** - the old code multiplied it by the batch size *and* the 550x rate, so a click was worth over a million XP. A cast also refuses when the pack is full and has nowhere for the enchanted bolts to stack. `magicOnItems` now decides once between bolt and jewellery instead of running the whole table per click.

**Files touched:** `Proxy Client/src/ui/{OverlayManager,InfoBoxes,AmmoOverlay,PluginSidebar,Interfaces}.java`; `Proxy Client/src/game/ClientSettings.java`; `Proxy Server/src/server/content/skills/Magic.java`; `Proxy Server/src/server/game/players/PlayerAssistant.java`; `Proxy Server/test/server/content/skills/BoltEnchantTest.java`.

**Status:** done - client `gradlew check` 687/0 and `installBin` synced; server `gradlew check` green with the new `BoltEnchantTest` 14/14 (XP, batch, runes, free slot, animation). One earlier server run had a single unrelated flaky failure in the bot tests; three re-runs passed. Wants an in-game look at the infobox size/font at each setting and at both ammo boxes at once.

## 2026-10-11 - Potions sit down in their box (and the gate that stopped them)

**What changed:**

- **Potions are dropped further than other products, on purpose.** `MakeX.POTION_DROP_NUM/DEN` (`4/3`) scales the centring term for bottles only: `dropOf` computes the plain centring, then multiplies it for a potion. Measured against the box, a potion's base now lands 2px above the red line drawn on the report (was floating ~7px clear of it, and before the centring existed its cork crossed the top of the frame). Everything else - a dagger, a fish, an ore - keeps the plain centring, so this moves bottles and nothing else.
- **Why the earlier "icons centred" fix never reached the screen.** `client`'s type 6 branch gated the drop and the clip on `MakeX.owns(class9.id)`, but `owns` answers about the five dialog *containers* and every product icon is a **child** of one, so it was false for every icon it was meant to move - the drop was correct, the probe agreed with it, and the player saw the old placement. Now gated on the new `MakeX.isProductFrame(class9.id)`. **This is the actual fix**; the fraction above is what it needed to be worth having.
- **What counts as a bottle.** `MakeX.isPotion` matches a name - potion, brew, serum, antidote, mixture, `mix(` - or a model drawn from one of the four dose bottles (`2789/2697/2384/2621`). The mesh half is what catches the drinks named after their stat rather than after being a potion: Super attack, strength, defence, energy and restore contain no "potion" and would otherwise be missed. It also picks up the other dose-drawn bottles (a vial, olive/sacred oil, the digsite chemicals), which is intended - same shape, same origin, same problem. The bottle every unfinished potion shares is excluded: 1225 items use it and a cannon base is one.

**Files touched:** `Proxy Client/src/ui/MakeX.java`; `Proxy Client/src/game/client.java`; `Proxy Client/tools/makex-probe/MakeXProbe.java`.

**Status:** done - `MakeXProbe` 245 passed / 0 (`MAKEX_OK`), `gradlew check` 687/0 and `installBin` synced, `EditChatbox.bat` still compiles. Wants an in-game look at a herblore menu (a full row of four potions, and the four-dose icon the report showed). Note for the next person: the red line in the report is **not** interface art - the whole cache was searched for its colour, 230,27,27, and no sprite holds it - so it is a mark drawn on the screenshot, and the placement is pinned to it in the probe as `REPORT_LINE_ABOVE_BOTTOM = 7`.

## 2026-10-11 - Fixed-mode flicker, icon size and place, title indent, editor crash

**What changed:**

- **Fixed mode no longer flickers its overlays.** The whole fixed frame - borders, viewport, minimap, tab panel, chatbox, then the panels on top - is composed into one HUD layer and presented once per frame (`presentFixedFrame`), and `ChatArea`/`TabArea` skip their own blit while `client.fixedFramePresentedByLayer()` is true. Before this a panel was written to the window in pieces, so it was off screen for the length of every composition that landed under it - the rapid flicker reported for infoboxes and plugin overlays.
- **Creation menu text right by 8px.** `MakeX.TITLE_X` 14 -> 22, which moves both "What would you like to make?" and "Choose a quantity, then click an item to begin." together (they share the constant).
- **Product icons 30% smaller and potions sit down in their box.** `ICON_ZOOM_NUM/DEN` `3/8` -> `15/28` (~60px drawn in the 82px slot, was ~85px). **This corrects the fraction the entry below logs as shipped.** Separately, `MakeX.iconDropY` measures the item's own model at draw time (vertex y bounds, cached per slot) and `client`'s type 6 branch adds it, so a potion - whose bottle is authored standing on its origin, above y 0 - is centred in its frame instead of crossing the top edge.
- **`EditChatbox.bat` opens again.** Each value spinner now takes its range from the value it loaded (`VALUE_SPAN` either side) instead of a fixed `-5000..5000`, so a widget id like `HELP_FIRST` (51920) no longer throws out of `SpinnerNumberModel`'s constructor and stops the editor before it starts.
- **`MakeXProbe` 227/227.** The last failing icon check was the probe's own bug: `ItemDef.forID` answers from a ten-entry ring of reused objects, so a definition held across another lookup had become a different item (a potion's model reported under the item's name). The model helpers now take item ids and look the item up at each use, as the client does.

**Files touched:** `Proxy Client/src/game/{client,ChatArea,TabArea}.java`; `Proxy Client/src/ui/MakeX.java`; `Proxy Client/tools/chatbox-editor/ChatboxEditor.java`; `Proxy Client/tools/makex-probe/MakeXProbe.java`.

**Status:** done - `MakeXProbe` 227/0 (`MAKEX_OK`), `gradlew harness check` 687/0 and `installBin` synced, `EditChatbox.bat` compiles and opens without the exception. Wants an in-game look: an overlay parked over the inventory and the minimap in fixed mode, a herblore potion's place in its box, and the title indent at 1x and in stretched mode.

## 2026-10-11 - Product icon size: checked by the probe, and Phase 5's logged fraction corrected

**What changed:**

- `MakeXProbe` now checks the size an icon is *drawn* at, not only the box it is drawn in: the model's zoom is the item's own scaled by `ICON_ZOOM_NUM/DEN` with the server's zoom argument discarded, the fraction is asserted against the slot (`3/8` is within 5% of the `32/SLOT_H` that fills an 82px box), and it is asserted to be below 1 - the direction that draws the icon *larger* than the 32px sprite.
- **Corrects the Phase 5 entry below**, which logged the fraction as `itemDef.modelZoom * 3 / 2`. That is the value the box was first forced to and it is a *smaller* icon (a zoom of 1.5 puts a 32px sprite at ~21px). What shipped, and what the source has, is `3 / 8` in `MakeX.ICON_ZOOM_NUM/ICON_ZOOM_DEN` - ~80px, the 4-5x asked for. The stale number is left in place per the log's no-edits rule and corrected here.

**Files touched:** `Proxy Client/tools/makex-probe/MakeXProbe.java`.

**Status:** done - `MakeXProbe` 220 passed / 0 failed (`MAKEX_OK`) against the real cache, client `gradlew classes` clean. The rest of Phase 5 is unchanged and still wants its in-game pass.

## 2026-10-10 - QOL Phase 5: chatbox polish, XP drops, altars, infobox groups, stretched mode

**What changed:**

- **Creation menu.** Title moved to the panel's top-left (`TITLE_X/Y`), the option row raised (`SLOT_TOP = 52`), every product icon forced to a single uniform size (`itemDef.modelZoom * 3 / 2`, discarding the per-family zoom the server sent), the item-name label inside the box removed, and the model frame now spans the whole slot so the icon centres instead of overflowing the container.
- **XP.** The fill-bar sprite is back to the left of the orb; drops render whether or not the counter is open; amounts use comma-grouped full numbers instead of `1.2k`; resizable mode shifts the drops a further 10px left.
- **Prayer altar.** Bone offering no longer rolls bird's nests - the nest roll moved from `onSkillAction` to a woodcutting-only `onWoodcuttingAction`.
- **Transparent chat, fixed.** "Press Enter to chat" is pure white, dialogue heads are 25% smaller, and only the plain body ink is whited out: the NPC name stays maroon, "Click here to continue" and the option rows keep their blue hover, and the teleport "Select an option" / pagination rows stay maroon/blue in every mode.
- **Plugin overlays.** One global opacity (0-100) and colour now apply to every panel, so fixed and resizable match; both are in the display settings and persist in `client_settings.properties`. Ammo shows the equipped ammo sprite with the stack count drawn over the icon, on by default.
- **Chat and bank.** The local player's own staff/donator rank icon is drawn left of their name in the chat prompt (self only, no gap for a normal account), and typing the first character of a bank search snaps interface 5385's scroll position back to the top.
- **Crystal chest.** Object 172 opens on left click when the pack holds a crystal key and has 3+ free slots; use-key-on-chest still works.
- **Stretched mode.** RuneLite-style plugin: the 765x503 fixed layout is scaled up in a resizable window with aspect-ratio locking and an adjustable scale, with mouse coordinates converted back through the transform.
- **Infobox groups.** Shift right click an infobox to Detach it into its own group; a group gets Flip (vertical/horizontal) and Delete (everything back to the default docked group). Groups are dragged with Alt as one unit and dropping one on another infobox merges it in. A group owns its position, so members expiring does not move the rest.

**Files touched:** `Proxy Client/src/ui/{MakeX,OverlayManager,InfoBoxes,AmmoOverlay,StretchedMode,Jframe,RSApplet,Interfaces,PluginSidebar}.java`; `Proxy Client/src/game/{client,ClientSettings,ChatArea}.java`; `Proxy Server/src/server/content/skills/{Woodcutting,Prayer}.java`; `Proxy Server/src/server/game/minigames/randomevents/RandomEventManager.java`; `Proxy Server/src/server/game/players/actions/objects/{CrystalChestObjects,ObjectHandler}.java`.

**Status:** done for infobox grouping and compile/tests; `Proxy Client gradlew check` 687/0 plus `installBin` synced, `Proxy Server gradlew build` green. Still wants one in-game pass: detach/flip/delete and a group merge, ammo sprite, stretched mode at a few window sizes, the crystal chest both ways, and the transparent-mode dialogue colours.

## 2026-10-10 - QOL Phase 4: creation chatbox, XP HUD, dialogues, bank, prayer

**What changed:**

- **Creation chatbox: only the real products, centred and spaced.** A slot is now considered empty when the server has not filled its model frame (`anInt233 == 0`), and `MakeX.recount` repositions each family for the filled slots only, parking the unused ones off-panel with zero size so no dead hitbox exists. The row is centred on a `width + SLOT_GAP` (6) pitch, labels truncate to the box and the item icon zoom is clamped. The whole block (title, pills, slots, labels) moved down 10px so the tops of the letters are no longer clipped by the dialog's `y = 20` clip. `Chatboxbackground.png` is now drawn as the panel while a creation menu is open (both fixed and resizable).
- **Make X removed end to end.** `AMOUNT_LABELS` is `1 / 5 / 10 / All`; the prompt branch, `acknowledgeAmount`, `MAKE_X_OPCODE`, the `interceptClick` hook, the client call sites and the `MakeXAmount` packet are gone. The bank's own enter-amount prompt is untouched.
- **Gem bolt tips use the chatbox.** Chisel on a cut gem now opens the creation menu (`CreationMenu.Kind.BOLT_TIPS` on interface 8938) instead of instantly crafting; chisel on an uncut gem still opens gem cutting.
- **Altar offers bones until empty.** Manual item-on-altar is still instant and unthrottled; it now also schedules a 2-tick `CycleEvent` (`ALTAR_EVENT = 4630`) that re-offers the same bone id until the pack is empty, the player moves, or the action is cancelled. The 1-in-40 "not satisfied" branch is kept per bone.
- **XP HUD.** Every gain accumulates into `sessionTotalXp`/`sessionXp` regardless of which skills are tracked, drops show in both counter states, and the counter/drop totals use `formatKOrMil` (`1.2k`, `34.5k`, `1.2m`). Drops are anchored `XP_DROP_NUDGE` (24px) left of the bar/orb centre so they no longer drift under the gameframe.
- **The invisible click-blocker near the XP counter is gone.** `isOverHudControls` keeps only the per-element boxes (compass, prayer/run/hp/spec orbs, `overXpCounter`); the blanket rectangle that declared everything right of the minimap down to `y=168` as HUD - about 140px of the 3D view with the counter open - is deleted, so walk clicks land again.
- **White text in resizable + transparent mode.** Chat prompts, the input dialog, "Click to continue", "Press Enter to chat", the bank search line and the hardcoded chat colours all route through `chatInk`/`chatInputColor` with a 1px shadow when `transparentChatActive()`. Dialogue and option text in the open chatbox dialog are forced to `0xffffff` with a shadow in that mode.
- **NPC chat heads animate again.** The server folds its 588..617 head emotes onto ids in 9760..9850, above the old `ensureCapacity(9000)` ceiling, so `safeAnimation` returned null and the head froze. `Animation` now sizes to `HEAD_EMOTE_LAST + 1` (9850) and `Animation.headEmote` translates those ids back onto the framed 588..617 emotes.
- **Special attack orb toggles on click.** The click was sent twice (once by `processMenuClick`'s `doAction`, once by `processHudClicks`), so it toggled and untoggled. A chosen HUD action (id >= 1000) now clears `clickMode3` after `doAction`, so the run orb's identical double path is fixed too.
- **Bank search filters while typing.** Frame 60 with the current text is sent on every keystroke and backspace while the bank is open, `applySearch` only announces when it was not already filtering, search state is cleared on close (bank close, `removeAllWindows`, `closeAllWindows`, `resetVariables`), and the two automatic `sendMessage` lines are deleted.

**Files touched:** `Proxy Client/src/ui/MakeX.java`; `Proxy Client/src/ui/RSInterface.java`; `Proxy Client/src/game/{client.java,ChatArea.java,MenuSystem.java}`; `Proxy Client/src/def/Animation.java`; `Proxy Client/tools/{makex-probe,head-anim-probe}/`; `Proxy Server/src/server/content/skills/{CreationMenu.java,Fletching.java,Prayer.java}`; `Proxy Server/src/server/game/{items/{UseItem.java,BankTabs.java},players/{PlayerAssistant.java,Player.java},players/packets/{ClickingStuff.java,JoinChat.java,Chat.java}}`; `Proxy Server/test/server/...` (CreationMenuTest, FletchingTest, GemCuttingTest, PrayerAltarTest, BankSearchTest).

**Status:** partial. Server `gradlew test` green (1136 tests, 0 failures), client `gradlew check` 687/0, `HeadAnimProbe` 6/0, `MakeXProbe` 207/0 against the real cache, `installBin` synced. **Not yet played in-game** - the last gate is a manual pass (chisel on one uncut and one cut gem, furnace, altar, XP counter and drops, the old dead-click zone, a dialogue in resizable transparent mode, a bank search typed and closed, and the spec orb).

## 2026-10-10 - QOL Phase 3 (eighteenth slice): the four defects the creation chatbox was reported with

**What changed:**

- **The item drawn and the item clicked were in different places, and that is why X's product click did nothing.** `MakeX` positioned the product models (and the make buttons) at `legacy.childX[i], legacy.childY[i]` — the coordinates the *cache's* interface uses — while positioning the frames, the labels and the amount row at the layout's own slot positions, and it subtracted the dialog's y draw offset but never its x. So the icon the player aimed at and the hitbox that would have received the click were two different places, which reads exactly as a dead button. Every child is now placed through one `childX()`/`childY()` pair, and the probe asserts that a product's frame, item model and armed hitbox occupy one and the same box.
- **The row was 20px right of where it meant, and smelting's eight boxes were 64 wide on a 58 pitch.** The pill block hung past the panel's right edge and nothing could be checked against "inside the chatbox". The slot width is now per family (`slotWidth(slots)`) and the block stops at the region the client builds hitboxes in (`CLICK_RIGHT` = 490, six pixels short of the message scrollbar) rather than at the panel edge.
- **The sword ornaments are gone.** The legacy interfaces carry two `sworddecor` children per family (8939/8940, 13714/13715, 8881/8882, 1744/1745, 2401/2402/2404) that the old row drew in its corners. Only the widgets the tables name are carried over now, so the containers hold the layout and nothing else.
- **A product click was drawing but dead.** `client` built an open dialog's hitboxes only while the mouse was inside the *messages* box (`x < 490, y < chatDrawY() + 125`), which cut off the right of the amount row and the bottom 5px of every product box. New `mouseOverChatDialog()` bounds them by the dialog's own rect (20..539, minus the scrollbar's strip) instead, and the "All" pill now sits inside it — that pill was drawn past both the panel and the gate.
- **Resizable transparency has one exception, and it is this dialog.** `ChatArea` keeps the solid chatbox background while a creation container is the open dialog (`MakeX.owns(backDialogID)`) and reverts to the player's preference when it closes.
- **Two latent widget bugs fixed on the way.** The slot frames were eight ids shared by all five families, which cannot hold two different widths — smelting's 58 was overwriting everyone's 64. They are now per family (`MakeX.slotFrameId`). And `applySelection` sized the armed hitbox with `SLOT_W` rather than the family's own width, so smelting's eight overlapped their neighbours by 6 pixels and a click near a box's right edge would make the next product. The frame art is scaled to the box for the same reason: a type-5 widget is drawn at its sprite's own size, so a 64-wide `SLOT.png` on a 58-wide box is 6 pixels painted over the neighbour.
- **Probe 95 → 127 checks**, the new ones a "clicking a product" section that asks the three gates at the centre of every product's box for all five families: the point is inside the hit-test region, it offers exactly one widget, that widget is the product's own armed id, and `MakeX.slotOfClick` claims it as that row — plus that each product's frame, model and hitbox share a box, that the frame's declared width and its art's width agree, and that the solid-background exception names exactly the five containers. The previews now also draw at the dialog's real offset, over the whole chat area, with the click region and the scrollbar's strip outlined, so an overflow is visible as one.

**Files touched:** `Proxy Client/src/ui/MakeX.java`; `Proxy Client/src/game/ChatArea.java`; `Proxy Client/src/game/client.java`; `Proxy Client/tools/makex-probe/{MakeXProbe,MakeXPreview,DumpLegacy}.java`.

**Status:** partial. Client `gradlew check` 687/0, probe 127/0 against the real cache, server `gradlew test` green, `installBin` synced. Still **not played in-game**: all four were reported from play, so one pass at a furnace is the last gate, then the placeholders can be replaced with the real sprites.

## 2026-10-10 - QOL Phase 3 (seventeenth slice): make-X, the first thing in this chatbox the server hears about

**What changed:**

- **X works.** Clicking it raises the chatbox's own enter-amount box; type a number, press enter, and the pill arms with it. Clicking a product then makes *exactly* that many. This supersedes the entry below, which had X as a pill that declined to be clicked.
- **The row is the first part of this chatbox the server is told anything about.** Everything else (layout, the four amounts, remembered selection) is client-only, because a product click already carries a button id the server has always understood. A custom count has no button id to ride on, so it leaves as its own packet: **opcode 5, four bytes**, row in the high half and count in the low (`MakeX.MAKE_X_OPCODE`). `Client.PACKET_SIZES[5] = 4`, registered in `PacketHandler`, answered by the new `MakeXAmount`, and handed to the families through `CreationMenu.submit` — refactored so make-X and a make-10 click share one `handTo`, since they differ in how they arrive and not in what they do.
- **Both guards are questions, not flags.** The number is only taken if the creation chatbox is still the open dialog (`backDialogID`), and a product click is only claimed if X is armed *and* that chatbox is up. A flag set when the box went up would have to be cleared on every one of the dozen ways it can close, or it would sit there waiting to swallow the next amount typed at a bank.
- **The client's count is not trusted.** The server re-clamps to `CreationMenu.MAX_X` (9999, kept equal to the client's) and checks the row against the menu the player actually has open rather than against the button table — a row can be on the interface and empty, and an empty row must do nothing rather than repeat the row above it.
- **Tests: 9 new server tests, 8 new probe checks.** Server (`CreationMenuTest`, `JewelryMakingTest`): make-X of 7 makes 7 (a count no button can express), still caps at the pack, is indistinguishable from make-10 once it lands, and is refused with no menu open / on a blank row / outside the rows / at zero, plus the opcode-size agreement. Probe 87 → **95**: X prompts instead of arming, a number typed elsewhere is ignored, the count clamps at both ends, and the payload's two halves.

**Files touched:** `Proxy Client/src/ui/MakeX.java`; `Proxy Client/src/game/client.java`; `Proxy Client/tools/makex-probe/`; `Proxy Server/src/server/game/players/packets/{MakeXAmount.java,PacketHandler.java}`; `Proxy Server/src/server/game/players/Client.java`; `Proxy Server/src/server/content/skills/CreationMenu.java`; `Proxy Server/test/server/content/skills/{CreationMenuTest,JewelryMakingTest}.java`.

**Status:** partial. Client `gradlew check` 687/0, probe 95/0, `installBin` synced; server `gradlew test` green. **None of it played in-game** — this slice adds a wire format, so the client and server have to be run together before it can be called done.

## 2026-10-10 - QOL Phase 3 (sixteenth slice): the row is five wide, and two of the measurements were wrong

**What changed:**

- **The amount row is now `1 / 5 / 10 / X / All`, in the top-right corner.** This supersedes the four-pill, 24x28 row in the entry below: a re-read of the reference screenshots found one of them (`Screenshot 2026-10-10 143222.jpg`) showing the row at **five** pills where the others showed three, which the earlier pass had missed by treating that shot as the outlier rather than the informative one.
- **Two earlier numbers were wrong and are corrected.** The pitch is **40**, not 48, and the pills are **35 tall, top y 18, ending flush 17px from the panel's right edge** — measured at x 378/419/459/499/539 with widths 30/29/30/30/36, the widest being "All". The earlier "40-wide, 8-gap, centred under the items" figures came from misreading a run-length table. Scaled to this 519-wide panel (×519/591 = 0.878): 28 wide, 9 gap, 31 tall, 15 right margin. One deliberate simplification: the pills are a uniform width where the reference sizes each to its caption, so one pair of sprites serves the row.
- **The header centring rule is confirmed, not guessed.** Across all eight screenshots the question and the help line share a centre, and it tracks the pill block: the three-pill shots centre at ≈239 and the five-pill one at ≈199.5 — a 40px shift for a 2-pill wider block. The earlier entry's "centred around x 260" was that same misreading. The question is centred in the room left of the pills (`titleRight()` = 322 here), which is why it sits left of the panel's centre.
- **X is drawn but deliberately cannot be armed.** The server's four buttons per product carry fixed amounts and there is nowhere on the wire for a typed number, and this client has no amount-input UI at all (`grep` for `Xamount`/`enterAmount` finds only server-side bank code). Arming X and sending the nearest of the four would silently make a *different* number, so `MakeX.select` refuses it and `client` says so in the chatbox. `select` now returns a line instead of void.
- **Both gates grew.** Probe 84 → **87 checks**, adding that the captions read `1 / 5 / 10 / X / All`, that the row's five entries resolve to the server's four amounts as 20 distinct widgets (X aliasing nothing), and that X refuses. `gradlew check` 687/0.

**Files touched:** `Proxy Client/src/ui/MakeX.java`; `Proxy Client/src/game/client.java` (fifth case + the note); `Proxy Client/tools/makex-probe/` (probe + preview); `Proxy Client/tools/make-quantity-sprites.ps1`; `Proxy Client/Sprites/Interfaces/MakeX/`.

**Status:** partial. Compiles, `installBin` synced, probe 87/0, `gradlew check` 687/0 — still **not played in-game**. Open question for the owner: X needs a chatbox amount prompt plus one small server-side channel (`CreationMenu` has no 5th button and `BankX2` is the obvious hook); that is a client+server slice, not a layout tweak. Until it lands X is a pill that declines to be clicked.

## 2026-10-10 - QOL Phase 3 (fifteenth slice): the chatbox is laid out from the screenshots, and the amount is remembered

**What changed:**

- **The chosen amount is remembered now.** `MakeX.substitute` used to call `select(0)` when a menu opened, which reset 1/5/10/All to make-one every time. That call is gone: the selection is a property of the player's habit, not of the menu, so it survives closing and reopening a chatbox and is shared across all five families (arm All at a furnace and it is still armed at a loom). The probe asserts it in both directions — reopening keeps the amount armed, and it carries between families.
- **The layout was re-measured off the reference screenshots instead of guessed at.** Four new tools read the JPEGs directly — `tools/ascii-shot.ps1` (luminance map), `tools/blobs-shot.ps1` (dilated ink blobs with boxes and fill ratios), `tools/dark-runs.ps1` (pixel runs on given rows) and `tools/crop-shot.ps1` (magnified crop). They were needed because the quantity selector is drawn art, so OCR only ever recovered it as "ooo", and the dither defeats colour clustering. Across all seven screenshots: the selector is a **right-aligned block of pills on the same two rows as the question and help line**, one of them drawn with a dark face (fill ratio 0.95 against 0.31); the question is centred in **the room left of that block and not in the whole 519** — it measures ~208px against the panel's 259.5px centre, which is about half a pill block out, and is the single easiest thing to get wrong; products get a bordered slot each, centred below; the chat tabs stay on y 142.
- **`MakeX` lays out that way.** Pills are now 24x28 (`QUANT_Y` 12) flush to the right edge and the title frame is widened to 402 and centred inside that; the model row, a new 64x82 slot frame behind each model and the name label moved to y 52/48/106; the old centred 40x21 row under the items is gone. The item art is the third placeholder file, `SLOT.png`.
- **Both gates grew.** The probe went 59 → **84 checks**, adding that the row really is on the header rows, that the pill block leaves the question exactly 402px, that each slot frame is drawn *behind* its model (a frame listed after its model is painted over the item), and that nothing the layout placed crosses y 142 where the chat filter row is drawn. `gradlew check` is 687 checks, 0 failures.
- **One deliberate divergence, flagged for a decision.** The screenshots show **three** pills, not four: pitch 40px with widths 31/31/37 and the widest one dark. This build keeps four options because that is what the request listed; three is a one-constant change.

**Files touched:** `Proxy Client/src/ui/MakeX.java`; `Proxy Client/tools/makex-probe/` (probe + preview); `Proxy Client/tools/make-quantity-sprites.ps1`; new `Proxy Client/tools/ascii-shot.ps1`, `blobs-shot.ps1`, `dark-runs.ps1`, `crop-shot.ps1`; `Proxy Client/Sprites/Interfaces/MakeX/`.

**Status:** partial. Compiles clean, probe 84/0 against the real cache, `gradlew check` 687/0 — but still **not played in-game**, so the click-to-make path and the pill art want one pass on a live server. Next: that pass, then the real sprites over the three placeholders. If the labels come up blank in-game, `MakeX.absorbName` (packet 126) is where to look, which the probe cannot check.

## 2026-10-10 - QOL Phase 3 (fourteenth slice): the quantity-first creation chatbox is built and verified (§12 3b / §15)

**What changed:**

- **The question that opened this was answered and then answered with code.** There is no button art: the panel is `Sprites/Gameframe/chatarea.png` (519x165), drawn by the chat area *under* the dialog, and the five families' 1/5/10/x buttons are type-4 text frames with a tooltip and no sprite — invisible clickable rectangles. So replicating the screenshots needs authored art, which is what the two new placeholder files are.
- **New `Proxy Client/src/ui/MakeX.java`** builds containers `51900`-`51904` standing in for creation `8938`, herblore `4429`, fletching `8880`, cooking `1743` and smelting `2400`. Layout is quantity first: title, help line, one centred row of product models with a name label under each, then a shared 1/5/10/All row (`51980`-`51983`). The four legacy make buttons stay on the widget ids the server already reads — `applySelection` sizes only the armed one, so a product click still sends the id it always did. **No server change.** The quantity row is client-only and answered in `client`'s `l == 315` branch before packet 185 is built. Amounts are 1/5/10/28, 28 being the existing make-all sentinel.
- **Placeholder art, and it is replaceable in two files.** `Sprites/Interfaces/MakeX/QUANTITY 0.png` (idle, 40x21) and `QUANTITY 1.png` (armed), drawn by new `tools/make-quantity-sprites.ps1` and written to both the repo's `Sprites/` folder and the live cache. `MakeX` loads them by name and falls back to a pill filled in code using the same four colours, so the row is never invisible and a re-skin only touches those files. Idle is light and armed dark, because their captions are black and white respectively.
- **New gate tool `Proxy Client/tools/makex-probe`** builds all five out of the REAL cache and checks the shape the layout tables must produce — **59 checks, 0 failures**, including that each armed hitbox is an id the legacy interface already had (the whole reason a click still reaches the server), that the four amounts pick four distinct widgets per product, and that smelting's eight names were seeded out of the cache. Its `--preview` mode also writes a layout diagram per family to `tools/makex-probe/out/`.
- **The names move house.** The cache keeps each product's name inside one of the product's own buttons, padded with line breaks to push it clear of the model. `absorbName` sits on packet 126, copies the name to a label and blanks the button; `seedLabels` does the same at build time for smelting, whose names never arrive in a packet.

**Files touched:** new `Proxy Client/src/ui/MakeX.java`; `Proxy Client/src/ui/Interfaces.java` (calls `MakeX.build`), `Proxy Client/src/game/client.java` (packet 164 substitution, packet 126 name absorption, `l == 315` quantity buttons); new `Proxy Client/tools/makex-probe/` (probe, preview renderer, runner), new `Proxy Client/tools/make-quantity-sprites.ps1`, new `Proxy Client/Sprites/Interfaces/MakeX/`; `UPDATE_LOG.md`.

**Status:** partial. Builds clean (`compileJava`, `installBin`) and the probe passes against the real cache, but **not played in-game** — the click-to-make path and the row's appearance want one pass on a live server. Next: that pass, then the real sprites over the placeholders. `MakeX.absorbName` also relies on the server sending names in packet 126 for these five interfaces, which the probe cannot check; if a menu shows blank labels in-game, that is where to look.

## 2026-10-10 - QOL Phase 3 (thirteenth, in progress): the make-chatbox buttons are drawn by nothing, and it is worth knowing which sprite the panel is (§12 3b)

**What changed:** investigation only, plus two new probes. No game code moved.

- **New `Proxy Client/tools/interface-dump-probe`** walks cache `index0` entry 3 itself instead of calling `RSInterface.unpack`. That is the whole point: `unpack` resolves a type 5 frame's sprite name to a `Sprite` and throws the name away, so a *loaded* interface cannot say which file drew it. The probe also has a `search:<text>` mode, which is what turns "is this wording already in the cache?" into a one-line answer.
- **New `Proxy Client/tools/palette-shot.ps1`** renders a screenshot region one letter per colour cluster with each cluster's bounding box. A luminance ramp is useless on this art — the whole chatbox is parchment browns inside one narrow brightness band, so everything collapses to a single character.
- **Answer: there is no button art.** Interfaces `8938` (creation), `4429` (herblore), `8880` (fletching), `1743` (cooking) and `2400` (smelting) are plain type 0 containers. Their only artwork is `sworddecor` frames 1 and 0 — the two header flourishes at `(2,5)` and `(421,5)` — plus the type 6 model frames that draw the products. The amount buttons are **type 4 text frames with an empty `message` and a tooltip and nothing else** ("Make 1"/"Make 5"/"Make 10"/"Make X"), so they are invisible clickable rectangles. That is why the 1/5/10/x/all buttons cannot be found: they are not drawn.
- **The chatbox panel is `Sprites/Gameframe/chatarea.png`** (519x165), loaded at `client.java:8127`; the resizable build uses `Sprites/Gameframe/resizable/chatarea.png` at `client.java:8132`, and `ChatArea.drawChatArea` paints a frosted overlay *instead* of it when the transparent-chat setting is on. The channel strip along the bottom is `chatbuttons` frames 0-3 (`client.java:8152`), drawn at y=142.
- **§15's screenshots are an authored rework of `8938`, not a cache interface.** Their title "What would you like to make?" is frame `8966` of `8938`, but `search:quantity` finds **0 frames** — the help line "Choose a quantity, then click an item to begin." is not in this archive, and neither is any quantity art. That is §12 row 3b.
- **A false positive was caught before it was written down.** `CreationMenu.BUTTONS` (`34242`-`34255`, `35000`-`35005`) looks like it addresses frames no cache defines. It does not: `Misc.hexToInt` decodes the 2-byte button packet as `b[0]*1000 + b[1]`, so frame `8946` arrives as `34242`. All twenty ids decode exactly onto `8946`-`8965`, and amount 28 lands on the "Make X" frame, which is the only "all" this interface has. Nothing to fix.

**Files touched:** new `Proxy Client/tools/interface-dump-probe/` (probe + runner), new `Proxy Client/tools/palette-shot.ps1`; `UPDATE_LOG.md`.

**Status:** partial. The build — an authored quantity-first chatbox with a panel sprite and visible 1/5/10/all buttons, on placeholder art — is waiting on a layout decision: whether the amount is per product (what `8938` does today) or a shared quantity row picked first (what the screenshots show). Both are reachable with client-only work; the second needs the item click remapped to the row's frame for the selected amount, and no server change at all.

## 2026-10-10 - QOL Phase 3 (twelfth slice): jewellery — the interface cannot ask for "all", and the onyx ring did nothing (§4j)

**What changed:**
- **The jewellery mould interface was read out of the real cache before anything was changed.** New `Proxy Client/tools/mould-interface-probe` loads cache `index0` entry 3 (`Decompressor` → `StreamLoader` → `RSInterface.unpack`, fonts built from archive 1) and dumps interface `4161`. That settled the question this slice turned on, and it is not answerable from the server source: containers `4233`/`4239`/`4245` carry **exactly three actions — `Make`, `Make 5`, `Make 10` — and two empty slots**. So a make-all click is not something this client can send; there is no missing server handler to write. The chatbox is the *only* place an "all" can exist.
- **Jewellery therefore goes onto the shared `CreationMenu`** (§15) — one row, 1/5/10/28 — but only for the amount. The interface keeps the product choice, because 21 products do not fit five rows. The left click (the interface's `Make`) opens the prompt; `Make 5` and `Make 10` stay the shortcuts they already were. Cost: left click is two clicks now. Gain: make-all, which the player has never had.
- **A product the interface offered could not be made.** The recipe table named `6583` as the onyx ring while the interface's own array drew `6575`. `6583` is the **Ring of stone** — `Magic.ONYXRING(6575, 6583, …)` enchants `6575` *into* `6583`, so the two ids are not interchangeable. Clicking the onyx ring matched no recipe and returned in silence. Fixed to `6575`.
- **The two lists that disagreed are now one.** That bug lived in a pair of hand-synced arrays. The interface rows are now derived from `Product` in declaration order, so a slot and its recipe cannot drift apart, and the probe exists to check the table against the real interface rather than trust either.
- **The batch is ticked, and it was not.** `mouldItem` ran a `while` over the whole stack in the click's own cycle — no animation, no way to stop, and the pack emptied before the player saw anything. Now one item every 2 cycles with animation `899` (the furnace pose smelting and the pottery oven use), all state in the event closure, cancelled by walking.
- **A full pack no longer eats the materials.** Jewellery consumes two items for one, so a tick with nowhere to put the product would have destroyed a bar *and* a gem and handed back nothing. The tick is not spent when there is no room.
- **The materials and the mould are re-checked at every entry point**, not just when the interface is drawn: both are things the player can bank while the interface is open, and the only symptom otherwise is a product that silently never appears.
- **Plain gold pieces no longer rest on a coincidence.** They take no gem, and the old code tested that by asking whether `ItemAssistant.getItemName(-1)` contained `"unarmed"` — which it does, because `getItemName` falls back to `"Unarmed"` for an unknown id. New `JewelryMaking.NO_GEM` says it out loud.
- **The amulet line read "You need a amulet mould to craft necklaces"**, overwriting the correct sentence the cache already had on frame `4242`.

**Deliberately left alone:** amulet **stringing** is still instant — a ball of wool on an unstrung amulet is a two-item recipe with no mould, no interface and no amount. Its XP was brought in line (it used `addSkillXP(4, 12)`, bypassing `CRAFTING_EXPERIENCE` while every other crafting award multiplies by it). Making it a "How many would you like to string?" chatbox is the obvious next slice, and §15's screenshots have one. Interface `4161` also stays the product picker.

**Files touched:** `server/content/skills/JewelryMaking.java` (rewritten), `CreationMenu.java` (new `Kind.JEWELLERY`), `CraftingData.java` (`resetCrafting` cancels it), `packets/RemoveItem.java` (left click → prompt), `packets/Bank5.java` / `Bank10.java` (shortcut amounts); new `test/.../JewelryMakingTest.java` (33); new `Proxy Client/tools/mould-interface-probe/` (probe + runner); `QOL_PLAN.md` (new §4j, §4 table, §12 rows 3/3b, §14.7, §15).

**Status:** done. **1092 tests, 0 failures, 0 errors** (1059 before; 33 new) plus the 156 workshop tests, all passing. Not run against a live client: the mould click and the chatbox button are packet paths, so jewellery wants one in-game pass — and the interface-probe output above is worth keeping beside it. Next in Phase 3 is a skill pick, or amulet stringing if the last instant crafting batch should go too.

## 2026-10-10 - QOL Phase 3 (eleventh slice): gem cutting and tanning move into the chatbox (§15)

**What changed:**
- **The nine inspiration screenshots were finally read.** They had been listed but never opened, so a new `Proxy Client/tools/read-chatbox-screenshots.ps1` runs Windows OCR over them (2x upscale, per-line boxes, because the quantity selector is graphics and the chatbox font is small). Eight are the 601x194 chatbox and share one shape: a per-skill question ("How many gems would you like to cut?"), the help line "Choose a quantity, then click an item to begin.", product icons, ordinary chat tabs still along the bottom. The ninth is the 749x488 smithing grid (`994`). Their real order is **quantity first, then item**.
- **A claim in §15 was wrong and is corrected.** It said the bow menu at `8880` is "a sidebar interface, not the chatbox". `sendFrame164` *is* the chatbox packet — the client's `case 164` sets `backDialogID`, which is the chat area. Most creation menus were already in the chatbox: herblore `4429`, fletching `8880`, leather/snakeskin/pottery `8938`, smelting `2400`, cooking `1743`. Only the anvil (`994`) and jewellery (`4161`) genuinely are not, and they are grids, not menus.
- **Gem cutting had no menu at all.** The chisel cut the entire stack silently until it ran out, with no way to stop. It now opens the chatbox on the uncut gems in the pack — one row per gem, make 1/5/10/28 — and running out of gems (or the amount) ends the batch. This also makes the batch cancellable by walking, which it was not.
- **Tanning's overlay is gone.** Interface `14670` (which existed only on this server) is replaced by a chatbox menu. The chatbox has no price frame, so the price moves onto the row's label ("Green d'hide - 20 gp"). Its rows are now **products, not hides**: cowhide makes soft *and* hard leather at different prices, so a menu keyed on the hide would have made that choice unreachable. The duplicate snakeskin row (same hide, same leather, 15 gp and 20 gp) is deleted.
- **One shared handler, one place that reads a click.** `CreationMenu` owns interface `8938` — five rows, four amounts a row. `8938` is shared by snakeskin, pottery, gem cutting and tanning, so the button ids are identical across all four; `CreationMenu.route` disambiguates off the player's open menu (`c.creationMenu`) rather than off the id, and every opener clears the other three. Opening pottery or leather closes it, and `resetVariables` closes it on walk, so an action can never outlive the menu that started it.
- **Five rows is the cap, and it says so.** `8938` holds five; a player holding more kinds than that sees the first five the table lists plus a message, rather than a menu that quietly omits the rest. That, and the amount-in-the-row interaction, are the two reasons the authored quantity-first interface (§12, 3b) is still worth building — the reuse was chosen now because it needs no client change.

**Files touched:** new `server/content/skills/CreationMenu.java`; `server/content/skills/GemCutting.java` (rewritten), `Tanning.java` (rewritten), `CraftingData.java` (`tanningData` sheds the dead overlay frames and button arrays), `Pottery.java` and `LeatherMaking.java` (close the menu on open); `server/game/players/Player.java` (`creationMenu`, `creationRows`), `PlayerAssistant.java` (`resetVariables`), `packets/ClickingButtons.java` (route first); new `test/.../CreationMenuTest.java` (15), `GemCuttingTest.java` (19), `TanningTest.java` (20); `Proxy Client/tools/read-chatbox-screenshots.ps1` (new); `QOL_PLAN.md` (§15 rewritten, §12 row 3 and new 3b, §14.7).

**Status:** done. **1059 tests, 0 failures, 0 errors** (1005 before; 54 new — 15 `CreationMenu`, 19 `GemCutting`, 20 `Tanning`). Forced with `--rerun-tasks`, so the count is real rather than cached. The 156 workshop tests are unaffected and were not run in this slice. Not run against a live client: both menus are click paths, so gem cutting and tanning are each worth one in-game pass. Next in Phase 3 is a skill pick, or the authored chatbox interface (3b) if the smithing grid and jewellery should come off their overlays.

## 2026-10-10 - QOL Phase 3 (tenth slice): pouches take pure essence, and the dead pouch twin is deleted

**What changed:**
- **A pure-essence player had no pouch.** `Pouches.fillPouch` searched for rune essence (`1436`) alone and `emptyPouch` handed the same id back, so pure essence (`7936`) — the members' essence, and what the essence impling drops — could not go into a pouch at all. §4h made both essences craftable at an altar and stopped at the pouch. Either essence works now, and filling an empty pouch draws rune essence first, the order the altar spends them in.
- **A pouch remembers which essence it holds.** A pouch stores one count, not a pair of stacks, so the kind travels beside it in a new `Player.pouchEssence`. Without that, emptying would either hand a rune essence back as pure (a free upgrade) or a pure essence back as rune (the same bug the other way up). A pouch holding one kind now says so rather than silently taking the other.
- **Every existing character file already depends on that rule, in the other direction.** None of them have a kind recorded, because rune essence was the only thing a pouch could hold when they were written — so a count with no kind reads as rune essence (`Pouches.kindOf`). Read as "empty" instead, the essence would be stranded in a pouch that reports itself empty and can never be filled again. The new `pouch-essence` key is additive and optional: an old file loads with the counts intact and no kind, which is what the rule exists for.
- **Two lies the messages told.** A pouch said "You fill your medium pouch" when it was already full or when the pack held no essence, and "You empty your medium pouch" with nothing coming out; either reads as a pouch that has eaten the essence. Each now says which of those it is.
- **A second, dead pouch implementation is deleted rather than left beside the live one.** `PlayerAssistant.checkPouch/fillPouch/emptyPouch`, `Player.pouches` and `Player.POUCH_SIZE` had no callers, and they hard-coded `1436` in exactly the same way — the same landmine, one refactor away from being wired up again. The three that remain live (click = fill, option 2 = check, wear = empty) all route through `Pouches`.
- **Essence stacking was checked and deliberately left alone.** Marking `1436`/`7936` stackable in `stackable.dat` looked like the same bug until the OSRS behaviour was checked: essence is *not* stackable, which is precisely why the pack is the real limit and why pouches exist. No data file was touched.
- **Recorded, not fixed:** the damaged pouches (`5511`, `5513`, `5515`) are still recognised by nothing — they hold less in OSRS and that number is not in this revision, so mapping them onto a full pouch would be inventing it. They are their own feature (§4i).

**Files touched:** `server/content/skills/Pouches.java` (rewritten), `server/game/players/Player.java` (new `pouchEssence`; `pouches`/`POUCH_SIZE` removed), `PlayerSave.java` (new key, read and written), `PlayerAssistant.java` (dead trio removed), `packets/ItemClick2.java` (loop replaced by the lookup); new `test/.../PouchesTest.java` (13), `test/.../PlayerSaveTest.java` (+2); `QOL_PLAN.md` (new §4i, §4 table, §12).

**Status:** done. **1005 tests, 0 failures, 0 errors** (1005 server + 156 workshop; 15 new). Not run against a live client: the three pouch actions are packet paths, so fill/empty/check are worth one in-game pass. Next in Phase 3 is still a skill pick — gems/jewellery or the `Check*` smithing table; glass stays UI-blocked (§4e) and agility stays parked.

## 2026-10-10 - QOL Phase 3 (ninth slice): runecrafting goes back to one click, pure essence works, and the XP-drop icon is fixed

**What changed:**
- **An altar takes the whole pack in the click again.** Last slice made runecrafting bind one essence every two cycles to match fletching. That is the wrong shape for this skill: OSRS binds the whole inventory in the single click, plays the bind animation once, and leaves no action running. The old loop was also an AFK loop — click once, walk away, keep being paid. `craftRunes` now counts the pack, draws it down, and hands over `essence × multiplier` runes plus the pack's experience in the same tick. The event, its id, the `playerSkilling[playerRunecrafting]` gate and `Runecrafting.cancel` are gone, and `PlayerAssistant.resetVariables` no longer calls it. The multiplier-ladder fix and the altar-list fix from the last slice are kept.
- **Pure essence (`7936`) did nothing at any altar.** `craftRunes` drew down `1436` alone, so a pack of the members' essence crafted nothing. Both are essence now and either works at any altar, rune essence spent first. The OSRS tiering where the higher altars insist on pure essence is **not** modelled and is recorded as an open question (§14.9) rather than guessed at.
- **The runecrafting XP drop drew a slayer skull.** The drop's icon is a client sprite indexed by the skill number the server sends — `Gameframe/XP/skill N.png` — and the set had been filed wrong at the top: slayer's art sat in `20`, runecrafting's in `19`, and there was no `18`, so runecrafting drops drew slayer, farming drew rune, and slayer drew nothing. New `Proxy Client/tools/rebuild-xp-skill-icons.ps1` re-derives an icon from the skill tab's own art (`Sprites/Interfaces/skillchat/skill N.png`, the set the skill tab and hiscores already use, swap included) and the three bad files are regenerated. **The mapping was checked against the art the old files were already drawing:** the new `20` comes out at the same `17x18` box and 175 pixels as the rune art that used to live in `19`, and the new `18` at the same `18x18`/220 as the slayer art that used to live in `20` — so the three are re-pointed rather than redrawn. Nearest-neighbour on purpose, so the alpha stays hard and the colours stay the source's own, matching the twenty icons beside them; re-deriving the ones that were already correct confirms the shipped set is the same art in the same style, only not by this exact rule, which is why three files are written and not twenty-three. **The client needs a restart to pick them up.** Originals kept as `skill 19.png.bak` / `skill 20.png.bak`.
- **One consequence recorded, not hidden:** the pack's experience is awarded as a single sum, so a click that crosses more than one level announces only the level it lands on.

**Files touched:** `server/content/skills/Runecrafting.java` (rewritten), `PlayerAssistant.java` (cancel hook and import removed); `test/.../RunecraftingTest.java` (17 → 15, pacing tests replaced); `Proxy Client/tools/rebuild-xp-skill-icons.ps1` (new), `Sprites/Gameframe/XP/skill 18|19|20.png` (new/fixed), `src/game/client.java` (a comment on the index at the icon loader); `QOL_PLAN.md` (new §4h, §4 table, §4g, §14.5/8/9).

**Status:** done. **990 tests, 0 failures, 0 errors** (server; the 156 workshop tests are unaffected and were not run in this slice). Client `compileJava` clean. The client was not restarted, so the icon fix is unverified on screen — check a runecrafting XP drop after a restart.

## 2026-10-10 - QOL Phase 3 (eighth slice): runecrafting and the anvil come off the instant batch

**What changed:**
- **Both skills were a single `while` over the whole stack, and now are one item every two cycles.** `Runecrafting.craftRunes` bound every essence in the pack in the game cycle of the click; `Smithing.doaction` hammered `maketimes` items in `while (maketimes > 0)`. Each is now a ticked action on the fletching cadence (animation and gfx re-armed per item), with all of its state in the event closure rather than on a class, and both cancel when the player walks (`PlayerAssistant.resetVariables`).
- **Runecrafting: the multiplier ladder was one rung short.** The old loop walked it from index 1, so it could return at most `length - 1` — an air essence at level 99 made **nine** air runes instead of ten, and every rune above air was one short of its top rung. `getMultiplierForLevel` now counts the rungs a level has passed, with the level requirement as the first rung. **This is a deliberate buff** at the top of every ladder.
- **Runecrafting: the altar list lived in two places, and the copy was wrong.** `ClickObject` carried a hand-written `int[] altarID` that stopped at blood, so the soul altar (`30625`) could not be used by clicking it. The list lives with the table now, behind `Runecrafting.isAltar`. An altar clicked with **no essence** also used to say nothing at all; it says so now.
- **Smithing: knives and cannon parts were made one per bar.** The per-bar batch sizes were read with case-sensitive `contains("knife")`/`contains("cannon")` against a capitalised item name, so those two branches could never match while darts, nails and arrowheads (whose names carry the word in lower case) worked. `batchSize` now lower-cases the name once, so all five yields are live: darts 10, nails and arrowheads 15, knives 5, cannon parts 4.
- **Smithing: the action reads no static fields.** The `Check*` chains hand `item`/`xp`/`remove`/`removeamount`/`maketimes` over through static fields, which was safe while the values were read back in the same call but not for an action that runs for cycles. Every value is a parameter now, and a test drives it through the real `readInput` entry point with two players so the property is pinned. Collapsing that chain into a table is left open (§4g).
- **Runecrafting experience is deliberately unchanged** — flat per essence (`(int) xp * Config.RUNECRAFTING_EXPERIENCE`, 15×), not scaled by the rune multiplier as OSRS does. Recorded as an open question (§14.8) rather than changed, because that is a balance decision.

**Files touched:** `server/content/skills/Runecrafting.java` (rewritten), `server/content/skills/Smithing.java` (`doaction`, new `batchSize`/`cancel`), `PlayerAssistant.java` (both cancels), `packets/ClickObject.java` (altar list removed); new `test/.../RunecraftingTest.java` (17) and `test/.../SmithingTest.java` (11); `QOL_PLAN.md` (new §4g, §4 table, §12, §14).

**Status:** done. **1148 tests, 0 failures, 0 errors** (992 server + 156 workshop; 28 new). Next in Phase 3 is a skill pick — gems/jewellery or the `Check*` smithing table; glass stays UI-blocked (§4e) and agility stays parked.

## 2026-10-10 - QOL Phase 3 (seventh slice): herblore's mixing gets its chatbox back — and the AFK batch behind it

**What changed:**
- **Combining ingredients now opens the OSRS "How many would you like to make?" chatbox.** A herb on a vial of water, or a secondary on an unfinished potion, sends interface `4429` with the product's own model and name and remembers the pair on the player. **Nothing is consumed until a button is clicked**, so the menu is safe to ignore.
- **Make 1/5/10/All works, and it is AFK.** Buttons `10239`/`10238`/`6212`/`6211` start a two-cycle batch (the fletching cadence, ~1.2 s a potion) that keeps making them until the amount asked for is reached, the materials run out, or the player walks. Walking both closes the menu and stops the loop.
- **This reverses last slice's "herblore does not batch" call, and that call was wrong.** OSRS does ask how many, and the chatbox is how it asks; what was wrong with the old menu was the state behind it, not the menu. The recipe used to sit in four static fields **together with the amount**, so one player's menu answered another player's click and "make 5" was a server-wide instruction. The pair now lives on the player and the amount in the event closure; two tests pin both halves (materials and amounts) as the old bug.
- **Grinding is unchanged: one per click, no menu.** The pestle is a tool rather than an ingredient, so there is no stack of second materials for an amount to be an amount *of*. The shared `Herblore.start` takes an amount now, so it is a one-line change if you ever want it.
- The button ids are interface `4429`'s own — the ones the pre-QOL herblore menu read — and are deliberately **not** the leather menu's, which puts ten on `10238` and twenty-eight on `6212` on interface 1743. Tests pin the table so the two cannot be swapped.

**Files touched:** `server/content/skills/Herblore.java` (mixing half: `mix`, new `openChatbox`/`select`/`amountFor`/`closeChatbox`, batched `start`), `Player.java` (`herbloreDialogue` + pending pair), `PlayerAssistant.java` (reset), `ClickingButtons.java` (routing), `test/.../HerbloreTest.java`; `QOL_PLAN.md` (§4f correction, §14.7, §15).

**Status:** done. **1120 tests, 0 failures, 0 errors** (964 server + 156 workshop; 10 new, all in `HerbloreTest`, updated from 22 to 32). Next in Phase 3 is still a skill pick — gems/jewellery, runecrafting or smithing completion; glass stays UI-blocked (§4e) and agility stays parked.

## 2026-10-10 - QOL Phase 3 (sixth slice): herblore — the tables were right, the implementation was not

**What changed:**
- **The recipes lived in static fields.** `itemToDelete`, `itemToDelete2`, `itemToAdd` and `potExp` were written by the click and read back by a tick loop, so **two players mixing at the same time cross-wired** — one could receive the other's potion and lose their materials. Every recipe now travels with its own action; a test named for the bug pins it.
- **`cleanHerb` cleaned everything.** It looped the whole table without breaking and acted on every herb in the pack, deleting each from the *first* matched herb's slot. Clicking one grimy herb with two kinds in the pack cleaned both. It now cleans exactly the herb that was clicked.
- **The make-X menu is gone, because herblore does not batch.** In OSRS a herb on a vial of water is one potion per click and an ingredient on an unfinished potion is one per click. Mixing and grinding are now ticked at two cycles (the fletching cadence), one product per action; cleaning is instant, because a single item in the pack has no walk to interrupt.
- **Four enums replace the five arrays**: `Cleaning` (16), `Grinding` (8), `Unfinished` (16), `Finished` (27). **Every pair is registered in `ItemUseRegistry`** via a new `HerbloreItemUses`, which is only possible now that the recipe is not shared state; cleaning stays in `ClickItem` because a grimy herb is clicked rather than combined.
- **Five potions the guide has printed all along could not be made at all**: energy (26), agility (34), super energy (52), antidote+ (68) and antidote++ (79). They are in the table now, using the secondaries this revision carries (chocolate dust, toad's legs, mort myre fungi, yew roots, magic roots) at the guide's levels and OSRS experience.
- **Two data errors fixed**: cleaning guam was level 1 where the guide and the attack potion both say 3, and the guide printed ranging at 69 and antifire at 72 where OSRS — and this table's own 163 and 158 xp — say 72 and 69, so the guide's two rows were swapped. The Herb tab also now prints wergali (30) and spirit weed (35), which the table cleaned and the tab did not list.
- **The legacy path was deleted, not left behind**: `setupPotion`, `makePotion`, `grindItem`, `setupGrinding`, `handleHerbloreButtons`, `resetHerblore`, the three `isX` helpers, the static fields, `Player.isPotionMaking`/`isGrinding`, and the `UseItem`/`ClickingButtons`/`PlayerAssistant` hooks they lived on. Spirit weed and wergali unfinished potions are the deliberate omission — this revision has the unfinished ids and no finished potion for either.

**Files touched:** rewrote `server/content/skills/Herblore.java`; added `actions/items/HerbloreItemUses.java`; edited `ItemUseRegistry.java`, `UseItem.java`, `ClickingButtons.java`, `PlayerAssistant.java`, `Player.java`, `misc/SkillInterfaces.java`; added 22 tests in `test/.../HerbloreTest.java` and four rows to `QolValidatorTest.java`; updated `QOL_PLAN.md` (new §4f, §12).

**Status:** done. **1110 tests, 0 failures, 0 errors** (22 new, all in `HerbloreTest`), and `QolValidatorTest` reports no undefined ids and no duplicate keys. Next in Phase 3 is a skill pick — gems/jewellery, runecrafting or smithing completion; glass stays UI-blocked (§4e) and agility stays parked.

## 2026-10-10 - QOL Phase 3 (fifth slice): darts, dragon arrows, and the bolt table's inputs — agility parked

**What changed:**
- **Darts now exist, and could not have before.** `Fletching.Bolts` was reading the **dart tips** (`819`-`824`) as its inputs, so a bolt cost a dart tip and a dart tip could never become a dart. Darts and bolts share the input pair (tip + feather `314`), so the bolt rows moved onto the six unfinished bolts this revision has always carried and nothing has ever used — `9375`, `9377`-`9381`. **Behaviour change: bolts now cost an unfinished bolt.** Blurite is left out on purpose (its pair is ready, `9376` → `9139`, but no level for it is printed anywhere in this revision).
- **`Fletching.Darts`, seven rows, ten per action**: bronze through rune (`819`-`824` → `806`-`811`) plus dragon (`11232` → `11230`, whose tip comes from implings and nothing else). A black dart (`3093`) exists with no black dart tip, so it is the one dart deliberately absent. Levels are the Fletching guide's own Darts tab (1/22/37/52/67/81, dragon 90); the guide prints no xp, so xp is the OSRS per-dart value for a batch of ten.
- **Dragon arrows**: `Arrows.DRAGON`, `11237` + `53` → `11212`, level 90, 245 xp, fifteen per action like every other arrow. Both new recipes were also added to the Fletching guide's Arrows and Darts tabs, so they are findable where the rest are.
- **Darts are registry-owned** (`FletchingItemUses`), the second family `ItemUseRegistry` owns outright; a test proves one click can no longer be a dart and a bolt at once. Also fixed while in the family: feathers on headless arrows used to spend fifteen feathers to rebuild the same fifteen headless arrows.
- **Agility: parked, per your call.** The recon stands (there is nothing on this machine to port; Barbarian and Wilderness courses are placed in the world and unimplemented) and the blocker is unchanged — no source here supplies each obstacle's landing tile and plane transition.
- **Noted separately, not designed: creation menus in the chatbox.** The wish that making things offers its options in the chatbox rather than in a sidebar menu is recorded in `QOL_PLAN.md` §15 with the screenshot folder as its reference, and listed in §14 as an open question. The screenshots were listed but **could not be read back**, so that entry is the request in words only and says so.

**Files touched:** `Fletching.java` (bolt rows, `Arrows.DRAGON`, new `Darts` enum with `forDart`/`makeDarts`, headless guard), `FletchingItemUses.java`, `UseItem.java` (arrowtip `11237` added, bolt ids changed), `SkillInterfaces.java` (two guide rows), `FletchingTest.java`, `QolValidatorTest.java`, `QOL_PLAN.md` (§5, §12, §14, new §15).

**Status:** done. **1088 tests, 0 failures, 0 errors** (10 new, all in `FletchingTest`; 6 existing bolt cases retargeted onto the unfinished bolts), and `QolValidatorTest` reports no undefined ids. Next in Phase 3 is a skill pick — gems/jewellery, runecrafting, smithing completion or herblore/potions; glass stays UI-blocked (§4e) and agility stays parked.

## 2026-10-10 - QOL Phase 3: agility recon — the table to port is not on this machine, but the courses are in the world

**What changed (docs only, no code):**
- **The plan's premise for agility does not hold.** §3 says "take the obstacle table" from Necrotic's `ObstacleData.java` (25.7 KB). No reference checkout exists anywhere on this machine any more — searched `Desktop`, `Downloads`, `Documents`, the drive root, this repo's git history, and the user tree: neither `2006Redone`, `Necrotic-Server-1.1.1`, nor any `ObstacleData.java`. So the table has to be **built**, not ported, and every obstacle's landing tile has to come from somewhere else.
- **Where it can come from: the world itself.** Scanned the placed objects through the server's own loaders (`MapIndex` + `Region`, the path the workshop exporter uses) for every object id whose `objectSize.cfg` name is an agility obstacle name (85 ids, 241 placements). The guide's Courses tab (`skillInterfaces.java:1131-1149`) advertises ten courses and only the Gnome course is implemented — but **two more are physically placed in the world and unimplemented**:
  - **Barbarian Outpost, level 35, guide row 6** at (2532-2552, 3544-3560): rope swing `2282` (2551,3550) with supports `2279/2280` and water `2306` beneath it, log balance `2291/2292/2293/2294` in a row along y=3546 from x=2542 to 2550, obstacle net `2284` (2538,3545) with its upper half `686/767/768` on plane 1, balancing ledge `1844/1865/2302` on plane 1 (2531-2537,3544-3548), crumbling wall `1948` at (2536/2539/2542, 3553).
  - **Wilderness, level 52, guide row 9** at (3000-3006, 3938-3960): log balance `2297` (3001,3945), `2288` (3004,3938 and 3004,3948), stepping stone `2311` (3001,3960), rope swing `2283` (3005,3952). Its entrance (object `2309` at 2998,3916) is already wired.
- Also found other placed clusters (monkey bars at 2598/9489 and 3120/9964, skull slope 3532/9909, stepping stones 3538-3540/9875-9881) whose course names are not yet established.

**Files touched:** none — this is reconnaissance. The scan tool was a temporary test in `workshop/test` and has been deleted; the exported map JSON is under the gitignored `Data/workshop/`.

**Status:** blocked on a decision. Object ids, placements and levels are now verified; what a course still needs is each obstacle's **landing tile and plane transition**, and no source on this machine supplies them. Options recorded in the next entry once chosen: derive the crossing from the player's approach direction (the shape `AgilityObjects` already uses for its push obstacles), or port a table if the reference zips can be provided.

## 2026-10-10 - QOL Phase 3 (fourth slice): soft clay, and the answer to the glass question

**What changed:**
- **Soft clay did not exist either, and pottery needed it.** The last slice shipped the wheel and the oven reading item `1761`; nothing in this server produced it — `1761` resolved in `item.cfg` and appeared only as a baby- and young-imping reward, so the Pottery tab was reachable in name only. Water on clay is the whole of the gap.
- **`SoftClay.WaterContainer` is seven rows**, each a full container and what it leaves behind: bucket of water `1929 -> 1925`, jug `1937 -> 1935`, vial `227 -> 229`, and the waterskin as a four-step ladder `1823 -> 1825 -> 1827 -> 1829 -> 1831`. No level, no experience, no animation, one clay per click. Registered in `ItemUseRegistry` through `SoftClayItemUses`, which iterates the enum, so both click orders are the same action.
- **Deliberately shorter than the wiki's list:** a bowl of water (`4454`) has no definition in this revision's `item.cfg` at all, and a watering can (`5331`) is an uncharged tool whose doses live in the farming layer, so neither is claimed rather than registered against a full form this server cannot back.
- **The four item operations are ordered delete/add/delete/add**, so each delete frees the slot its add then fills — a full pack can still soften clay. A test fills all 28 slots and checks the swap lands instead of quietly eating the clay.
- **The zero experience is the real number.** OSRS awards none, so a figure would be invented; a test pins `SoftClay.XP` and asserts crafting `playerXP` is untouched, so it reads as a decision rather than a forgotten `addSkillXP`. Not ticked either, on purpose: both items are in the pack, so there is no walk to stop, and a repeat would have to pick a container.
- **The open question from §4c is answered: this client has no glassblowing interface at all.** `11462` appears nowhere in the client source, and neither does the word "glassblow" or any of the eight product names; the only interface files are `ui/RSInterface.java` and `ui/Interfaces.java`, the 317-style hardcoded set. The guide's own Glass tab has no button behind it either — `menuLine` writes a `sendFrame126` label and an icon into stock interface 8714, so those rows are text, not slots a handler can hear. `QOL_PLAN.md` §4e lays out the three honest options (bend an existing chatbox menu, author the interface with `RSPSInterfaceMaker`, or build the sand + soda ash prerequisite that needs no menu).

**Files touched:** added `server/content/skills/SoftClay.java`, `server/game/players/actions/items/SoftClayItemUses.java`; edited `ItemUseRegistry.java` (static bootstrap); added 21 tests in `test/.../SoftClayTest.java` and a `SoftClay.WaterContainer` table to `QolValidatorTest.java`; updated `QOL_PLAN.md` (§4e, §3 sequencing).

**Status:** done. **1078 tests, 0 failures, 0 errors** (922 server + 156 workshop; 21 new, all in `SoftClayTest`), and `QolValidatorTest` reports no undefined ids. One test is the reachability claim rather than a tidiness check: it asserts `Pottery.SOFT_CLAY == SoftClay.SOFT_CLAY` and that soft clay is registered on both wheels, because the wheel reading an item nothing made is exactly the defect this slice closes. **Next in this phase is glass, which is blocked on a UI decision, not on a table** — §4e has the options; the guide's Weaponry battlestaves and the orb chain sit downstream of the same blocked menu.

## 2026-10-10 - QOL Phase 3 (third slice): weaving, the one neither reference server has either

**What changed:**
- **Weaving did not exist, and now does.** The Crafting guide's Weaving tab has printed "10 Cloth" and "21 Vegetable Sack" since before this work, and this server's own item examine texts already pointed at the loom ("I can weave this to make sacks." on jute fibre `5931`), but nothing read `5931` anywhere and nothing handled an object called "Loom" — `Data/objectSize.cfg` names two of them (787, 8717). Unlike the last two slices there was nothing to port: **2006Redone advertises the same tab with the same absence of implementation**, and Necrotic has only flax and gems. The numbers therefore come from the OSRS loom table.
- **`Weaving.Weave` is two rows**: four balls of wool `1759` into cloth `3224` at level 10 for 12 xp, and four jute fibres `5931` into an empty sack `5418` at level 21 for 38 xp. Both levels and both products are the guide's own lines, so the tab stops promising something the server will not do.
- **Four materials per action, not one.** Cloth and sacks are batches, which is the one shape this row has that spinning and pottery do not: the ticked loop consumes the whole batch per action and stops when a full batch is no longer held, rather than when the material hits zero. A test works the boundary — seven wool is one cloth and three left over.
- **Two ways in, through the same Phase-0 registries.** A first click on a loom (`WeavingLoomObjects`) and wool or jute used on one (`WeavingItemUses`). The click weaves whichever single material you are carrying and, as with the spinning wheel, refuses to guess when both are held. Ticked and repeating at two cycles, on `playerIsCrafting`.
- **The animation is borrowed, and said so.** No source implements a loom, so no source has a loom animation; it uses the spinning wheel's 896 — the closest action in the same skill — and a test pins it to `Spinning.SPIN_ANIMATION` so the borrow stays visible rather than being mistaken for a found id.
- One small honesty guard: `clickLoom`'s refusal message spells "four" out in words, which makes it a second copy of the amount column, so a test pins both amounts to 4.

**Files touched:** added `server/content/skills/Weaving.java`, `server/game/players/actions/objects/WeavingLoomObjects.java`, `server/game/players/actions/items/WeavingItemUses.java`; edited `CraftingData.java` (`resetCrafting` cancels weaving too), `ObjectHandler.java` and `ItemOnObjectRegistry.java` (static bootstrap); added 30 tests in `test/.../WeavingTest.java` and a `Weaving.Weave` table to `QolValidatorTest.java`; updated `QOL_PLAN.md` (§4d, §3 sequencing).

**Status:** done. **1057 tests, 0 failures, 0 errors** (901 server + 156 workshop; 30 new, all in `WeavingTest`), and `QolValidatorTest` reports no undefined ids. Deliberately not added: the other five OSRS loom recipes, which this guide tab does not advertise and three of which need intermediate yarns nothing here makes — listed in §4d as the next five rows if the tab is ever widened. Next in this phase: glassblowing, where the open question is whether this client has interface 11462.

## 2026-10-10 - QOL Phase 3 (second slice): pottery, both stages, at the wheel and the oven

**What changed:**
- **Pottery did not exist, and now does — both halves.** The Pottery tab has always listed five items (1 Pot, 7 Pie Dish, 8 Bowl, 19 Plant Pot, 25 Pot Lid) and all ten ids resolve in `item.cfg`, but nothing anywhere read the five *unfired* ids: the tab was a menu with no action behind it. The two fixtures were already named in `Data/objectSize.cfg` ("Potter's Wheel" 2642/4310, "Pottery Oven" 2643/4308/11601), so the world has had them the whole time. `Pottery.Shape` is five rows with **two xp columns**, because clay is two actions: soft clay `1761` into unfired clay at the wheel, then unfired clay into the finished item in the oven. Shaping xp 6.3/15/18/20/20, firing xp 6.3/10/15/17.5/20, levels 1/7/8/19/25 — the guide's own five levels, and both columns agree with OSRS. Redone's `Pottery` carries the same numbers for the three rows it implements, but its firing half is unreachable (written, never called), so the oven stage is built from the OSRS table rather than ported.
- **Firing has no level requirement, and that is now load-bearing.** Only shaping asks for one, so the check lives at the wheel; a level-1 player holding an unfired plant pot can still fire it. There is a test named after that rule, because it is the kind of thing a later reader assumes is a missing check and "fixes".
- **Three ways in, all through the Phase-0 registries.** First click on a wheel (`PotteryWheelObjects`), soft clay on a wheel, and unfired clay on an oven (`PotteryItemUses`) — the last is 15 pairs. Both stages are ticked and repeating at the same two cycles as spinning and fletching, one item per action, on `playerIsCrafting`.
- **The oven is only reachable by using an item on it.** A plain click is not claimed: the oven cannot know which of the five you meant, and 2643 is the object this server hangs `JewelryMaking.mouldInterface` on, so claiming its click would silently kill jewellery-making. The pair-keyed registry is exactly what makes the oven stage possible — gold bar on 2643 still falls through to the switch, and a test pins that.
- **The wheel reuses interface 8938**, the five-option chatbox snakeskin leather already uses, and therefore the same four buttons per row. Hence the new `Player.potteryDialogue` beside the existing `craftDialogue`, and why each menu clears the other on open: with one flag, a click with both menus having been open would make leather *and* pots from one button. Rows 4 and 5 are button ids Redone never wired up at all; they are taken from this server's own snakeskin rows for boots and vambraces, which occupy the same chatbox rows, and a test pins the two tables equal row-for-row.
- **One shared-code change.** `Spinning.cancel` and `Pottery.cancel` now stop their own event *unconditionally* and guard only the flag/animation. `playerIsCrafting` means "some crafting action is running", not "this one is", so with two skills sharing it whichever cancel ran second would have found the flag already cleared and left its own loop queued — the same double-speed bug already fixed for spinning, one layer up. `resetCrafting` calls both.

**Files touched:** added `server/content/skills/Pottery.java`, `server/game/players/actions/objects/PotteryWheelObjects.java`, `server/game/players/actions/items/PotteryItemUses.java`; edited `Spinning.java` (unconditional event stop), `CraftingData.java` (`resetCrafting` cancels pottery too), `ObjectHandler.java` and `ItemOnObjectRegistry.java` (static bootstrap), `Player.java` (`potteryDialogue`), `PlayerAssistant.java` (`resetVariables`), `ClickingButtons.java` (dispatch), `LeatherMaking.java` (three places clear the new flag); added 39 tests in `test/.../PotteryTest.java` and a `Pottery.Shape` table to `QolValidatorTest.java`; updated `QOL_PLAN.md` (§4c, §3 sequencing).

**Status:** done. **1027 tests, 0 failures, 0 errors** (871 server + 156 workshop; 39 new, all in `PotteryTest`), and `QolValidatorTest` reports no undefined ids. Not modelled and documented in §4c: the oven's small chance to crack a piece (no source gives the curve; Redone does not model it either). Next in this phase: Weaving (loom 787/8717 — cloth 4× ball of wool at 10 for 12 xp, empty sack 4× jute fibre at 21 for 38), then glassblowing, where the open question is whether this client has interface 11462.

## 2026-10-10 - QOL Phase 3 (first slice): spinning works, and the wheel was never there

**What changed:**
- **Spinning did not exist, and now does.** The Crafting guide has always printed a Spinning tab ("1 Wool", "10 Flax into Bow Strings") and the flax's own examine text says "I should use this with a spinning wheel", but there was no wheel handler anywhere — `Flax.java` only picked the flax, so both products were impling-only. That became load-bearing once bow stringing landed: the only bow string in the game was an impling drop. `Spinning` is a two-row `Material` table — wool `1737 -> 1759` at 1 (3 xp), flax `1779 -> 1777` at 10 (15 xp) — which is both the guide's pair and OSRS's, and the xp is Necrotic's and Redone's. Both ids resolve in `item.cfg`; `QolValidatorTest` reports no undefined ids.
- **Two ways in, both through the Phase-0 registries rather than a switch.** A first click on the wheel goes through `ObjectHandler` (new `SpinningWheelObjects`, ids 2644/4309/8748 taken from `Data/objectSize.cfg`), and using wool or flax on the wheel goes through `ItemOnObjectRegistry` (new `SpinningItemUses`). Both read the same `WHEEL_OBJECTS`, so the two cannot disagree about which objects are wheels.
- **A click spins the one material you carry; carrying both it refuses and says so.** Wool and flax become different items, and a click that silently turned a stack of flax into bow strings would be the wrong answer half the time — the player picks with the item instead. This is a deliberate extra click, documented at the method.
- **The action is ticked and repeating**, one item per 2-tick cycle until the material runs out or you walk, on the shared `playerIsCrafting` flag, exactly like gem cutting and leather. Animation 896 either way (Necrotic's and Redone's).
- **A real bug in shared code, fixed on the way.** `CraftingData.resetCrafting` cleared `playerIsCrafting` but left the looping event queued; the old event then woke on its next tick, found the flag re-set by a fresh action, and ran *alongside* it — two loops, double speed, over one stack. It now stops the spinning event before clearing the flag. The same shape is still reachable in `GemCutting` and `LeatherMaking`, which rely on the flag alone; flagged for whenever either is next touched. `QOL_PLAN.md` §4b carries it.

**Files touched:** added `server/content/skills/Spinning.java`, `server/game/players/actions/objects/SpinningWheelObjects.java`, `server/game/players/actions/items/SpinningItemUses.java`; edited `CraftingData.java` (`resetCrafting` stops the spin), `ObjectHandler.java` and `ItemOnObjectRegistry.java` (static bootstrap); added 24 tests in `test/.../SpinningTest.java` and a `Spinning.Material` table to `QolValidatorTest.java`; updated `QOL_PLAN.md` (§4b, §3 sequencing).

**Status:** done. **988 tests, 0 failures, 0 errors** (832 server + 156 workshop; 24 new, all in `SpinningTest`). Phase 3 first slice complete. Next in this phase: Pottery (ovens 2643/4308/11601), Glassblowing (pipe `1785`), Weaving (looms 787/8717), then gem-cutting breadth, agility and the runecrafting/smithing loops — pottery and glass are the same shape as spinning: a table, one object, one ticked loop.

## 2026-10-10 - QOL Phase 1 (finished): bow stringing exists now, and it went into the registry

**What changed:**
- **Bow stringing did not exist at all, and now does.** Item `1777` was an impling reward and a Crafting menu label; nothing in the server consumed it, so the twelve unstrung bows in `item.cfg` (48, 50, 54, 56, 58, 60, 62, 64, 66, 68, 70, 72) could never become bows. `Fletching.Stringing` is a thirteen-row table — Necrotic's `StringingData` verbatim, plus Redone's composite-bow row (`4825 + 1777 -> 4827`, level 30, 45 xp) which Necrotic does not have. All twenty-seven ids (unstrung, strung and the string) resolve in `item.cfg`; `QolValidatorTest` now reports "none: every referenced id resolves".
- **The lookup is on the pair, and it is order-blind.** `forStringing(a, b)` requires the bowstring to be one side, so an unstrung bow on a chisel is *not* stringing. Matching the unstrung id alone — which is what the raw tables do — would have claimed those pairs.
- **`stringBow` is a ticked, repeating action** on the same `playerFletch` flag and the same `FLETCH_EVENT` id as `fletchBow`, so the existing `cancel` and `PlayerAssistant.resetVariables` end it on a walk with no second hook, and only one of the two can run at a time. One bowstring and one unstrung bow per 2-tick cycle, until either stack empties.
- **Registered in `ItemUseRegistry`, not written inline.** This is the first item recipe to move into the registry: `FletchingItemUses.register()` is called from the registry's static block, the same bootstrap shape as `ObjectHandler` calling its object families. `UseItem.ItemonItem` returns on a claimed pair, so the registry and the four hundred lines of inline checks never both run.
- **Animations are Necrotic's per-bow set** (6678–6689, one per bow). Redone strings all thirteen with no animation and this server had no stringing animation to copy, so the sourced set wins over inventing ids. The composite bow is the one row with no source for an animation and borrows the longbow's; that borrow is documented at the row.
- **The magic longbow is level 85 here and 87 in `Fletch`.** Left as sourced rather than silently reconciled — it is a balance number, and it is unreachable in normal play because the unstrung bow needs 87 in the first place. Flagged in `QOL_PLAN.md` §5.

**Files touched:** `server/content/skills/Fletching.java` (`Stringing` enum, `BOW_STRING`, `forStringing`, `stringBow`); added `server/game/players/actions/items/FletchingItemUses.java`; edited `ItemUseRegistry.java` (static bootstrap); added 13 cases to `test/.../FletchingTest.java` and a `Fletching.Stringing` table to `test/.../QolValidatorTest.java`; updated `QOL_PLAN.md`.

**Status:** done. **964 tests, 0 failures, 0 errors** (808 server + 156 workshop; 13 new, all in `FletchingTest`). Phase 1 is now complete — bows, shafts, arrows, bolts, tipping and stringing. Next: Phase 3, skilling completeness, starting with the tables the guide already advertises but the server does not implement: pottery, glassblowing, spinning, gem cutting breadth, then agility and the runecrafting/smithing loops, using Necrotic's tables.

## 2026-10-10 - QOL Phase 2: random events get a manager, and the flags start doing something

**What changed:**
- **There is now one place that decides a random event.** `RandomEventManager` holds an `Event` enum of the five interrupting events, each carrying its `Config` flag and a draw weight, plus a single `onSkillAction(Client)` entry point. The seven inline rolls it replaces were `Misc.random(250)` written out by hand in `Woodcutting` (1), `Mining` (3), `Fishing` (1) and `Prayer` (2, plus the altar's separate `Misc.random(80)`), each followed immediately by a hardcoded spawn.
- **The `RANDOM_EVENT_*` flags were describing behaviour that did not exist.** `RANDOM_EVENT_CLASSIC_OTHERS_ENABLED` is `false`, and Spirit Tree, Rock Golem, River Troll and Zombie fired on every matching action anyway. They read the flag now, so those four are genuinely **off by default** — which is what was asked — and the genie and bird nest are the live pair.
- **A nest and an NPC are rolled differently, on purpose.** A nest is a frequent small bonus and keeps the per-action roll it already had (`Misc.random(100) < 5` — the rate is unchanged, deliberately: Necrotic's is `1/61` and lowering ours would be a nerf nobody asked for). An NPC event is an interruption and runs on a per-player countdown of **350–450 actions**, which is Necrotic's `350 + random(100)` shape. A countdown rather than a flat chance cannot fire twice in a row.
- **Nest type now uses Necrotic's table: seed 64.1%, ring 32.0%, the three egg nests 3.9%.** The old `birdNests` added `5070` (red egg) unconditionally, so seed and ring nests were unreachable from woodcutting even though `ClickItem` already knows how to open all five.
- **The genie is real, and hands over the lamp the server already has** — item `4447`, rub it and the existing skill-choice interface opens, so no client work and no new interface. It appears for 200 cycles, talks, and gives one lamp per spawn. Registered as an NPC action in a new `RandomEventNpcs`, not in the generated NPC tables: it was never in the switch.
- **`spawnSpiritTree` and its three siblings now return whether they actually spawned.** All four call sites stopped the player's action whether or not the NPC had appeared, so a level-3 player burying bones could be interrupted by nothing at all. An event now only interrupts if it arrived.
- **Bots are skipped.** They hold real player slots and run the same skilling code, so a bot left chopping would otherwise accumulate genies and nests.
- **The countdown is saved** as `randomEventCounter`, so relogging is not a way to avoid events. `0` means "not armed" and the manager arms it on the first action rather than firing, so a brand-new account cannot get an event on its first log.

**Files touched:** added `server/game/minigames/randomevents/RandomEventManager.java` and `server/game/players/actions/npcs/RandomEventNpcs.java`; edited the four `randomevents/` spawn classes, `Woodcutting.java`, `Mining.java`, `Fishing.java`, `Prayer.java`, `NpcActionHandler.java`, `Player.java`, `PlayerSave.java`; added `test/.../RandomEventManagerTest.java` and two round-trip cases in `PlayerSaveTest.java`; updated `QOL_PLAN.md`.

**Status:** done. **951 tests, 0 failures, 0 errors** (795 server + 156 workshop; 20 new — 18 for the manager, 2 for the countdown's persistence). Not done, and not attempted: Sandwich Lady, Evil Chicken, Freaky Forester, Swarm, Frog, Shade and Tree Spirit. Their NPC ids all exist but each needs behaviour and dialogue invented from nothing, so they wait for their dialogue to be written. Also left open for a decision: the lamp grants **level 70** in a skill, which is generous for a random event — the delay and the lamp id are both one-line changes. Next: Phase 3, skilling completeness, with bow stringing first since it does not exist in this server at all.

## 2026-10-10 - QOL Phase 0 (cont.): the content validator runs in the build, and three ids it caught as wrong were wrong in the test itself

**What changed:**
- **`QolValidatorTest` is now committed and running** (3 tests). It walks the authored content tables — fletching bows/shafts/bolts, and the item-use and item-on-object registries — and reports every id in them that `ItemDefinitions` cannot find, then fails the build on lookup-key duplicates and out-of-range ids. Missing ids are warnings rather than failures on purpose: §2 means to write ids we do not own yet, so "no definition" is often the *intent*, and a hard failure there would fight the plan. A duplicate key is different — two recipes claiming one key means one of them is unreachable — so that fails.
- **The id checks were passing for the wrong reason, and fixing that exposed three bad ids in `ItemDefinitionsTest`.** The test task runs in `build/testwork`, a throwaway directory with no `Data/` in it, so `item.cfg` never loaded, the item table was empty, and every "this id has no definition" assertion was true of *every* id. `ItemHandler` now honours a `serverCfgDir` system property and `build.gradle` points it at the real `Data/cfg`, so the tests see the real 19,966 rows (ids 0–20072). With the table actually loaded, `4153`, `9000` and `9001` — previously named as undefined — turned out to be the Granite maul, Bandana and eyepatch. They are now genuinely free ids (24000), and the two ids this class used to name are asserted *defined*, so the mistake cannot come back as a silent pass.
- **`15333` was also the wrong example of an unimported id.** `QOL_PLAN.md` §2 names an overload dose as the case of "an id we reference before we have it", but `item.cfg` already defines the entire dose chain — Recover special, Super antifire, all four doses of all five extremes, Super prayer, and overload twice over (15300–15338). There is no overload row left to import, so the exemplar is now simply an id past the end of the table, and the plan says so rather than leaning on a row we already have.
- **`Fletching.Fletch` is public** like its sibling `Bolts` and `Arrows` tables, so the validator can read the rows. A wrong product id in that table is a bow that silently does not exist rather than a compile error, which is exactly what the validator is for.

**Files touched:** added `test/server/content/QolValidatorTest.java`; edited `server/world/ItemHandler.java` (`cfgPath` override), `build.gradle` (`serverCfgDir` for the test task), `server/content/skills/Fletching.java` (table visibility), `test/server/game/items/ItemDefinitionsTest.java`; updated `UPDATE_LOG.md`.

**Status:** done. **931 tests, 0 failures, 0 errors** (775 server + 156 workshop; `ItemDefinitionsTest` 7, `FletchingTest` 20, `QolValidatorTest` 3). Phase 0 and Phase 1 are now both committed. Next: Phase 2, the config-gated random events (`RandomEventManager`, bird nests off woodcutting, genie lamp), then Phase 3 skilling completeness — bow stringing first, since we found last session that it does not exist at all.

## 2026-10-10 - QOL Phase 1 (cont.): arrows, bolts and bolt tips onto the tick, and iron-through-rune bolts made craftable

**What changed:**
- **Iron, steel, mithril, adamant and runite bolts could not be made at all.** `forBolts` looked up recipes by `getItem2()`, which is `314` (feathers) on every row of the table. So it returned `BRONZEBOLT` whenever it was asked about feathers and `null` for every actual bolt. Together with `makeBolts` checking only its first argument, using bolts on feathers did nothing and using feathers on bolts always made bronze. The lookup now matches the bolt column and `makeBolts` looks in both arguments, so either order works and all six metals are reachable.
- **Arrow making, bolt making, bolt tipping and bolt-tip crafting now run on the tick** through one shared helper, instead of a `System.currentTimeMillis()` gate in front of an instant action. A wall-clock throttle lets a fast clicker through on lag and lets a slow one do nothing; the tick paces them and the supply count is re-checked when the action actually executes.
- **These stay single actions, deliberately.** Fifteen arrows or ten bolts per click is the OSRS batch size, so "one item per tick" does not apply to them and `Config.FLETCHING_ONE_BY_ONE_ENABLED` does not gate them — that flag governs the repeating actions (bows and shafts), where "make 28" is 28 actions. The flag's javadoc now says this rather than over-claiming.
- **Removed four `System.out.println` debug lines** that printed supply counts to the console on every arrow and bolt attempt.

**Files touched:** `server/content/skills/Fletching.java`, `server/Config.java` (flag javadoc only); extended `test/server/content/skills/FletchingTest.java`; updated `QOL_PLAN.md`, `UPDATE_LOG.md`.

**Status:** done. **927 tests, 0 failures, 0 errors** (771 server + 156 workshop). The new tests drive the real `CycleEventHandler` loop rather than mocking it, so they pin the tick pacing (nothing consumed on tick 1, consumed on tick 2), the ten-per-click batch size, cancellation via `resetVariables` on the walk path, and the bolt lookup regression directly.

**Follow-up found while working, not fixed here:** bow **stringing does not exist** in our server — item 1777 is only an impling reward and a Crafting menu label, and nothing consumes it. There is no stringing to make ticked; there is stringing to add. It needs its own table and level/xp data, so it is a Phase 3 (skilling completeness) item. Also unchanged: a gem still yields 10 bolt tips and bolt-tip cutting still grants no xp, both left as found because the correct values need looking up rather than inventing.

## 2026-10-10 - QOL Phase 1: fletching cuts one log per tick instead of a whole inventory at once

**What changed:**
- **`Fletching.fletchBow` is now a ticked action.** It was instantaneous: one call deleted the whole amount and added the whole product, so "make 28" finished before the animation started and could not be walked away from. It now runs on `CycleEventHandler` at 2 cycles, consuming one log and producing one item per tick with animation 1248 — the OSRS behaviour, and the same shape our Cooking and Mining already use.
- **The log now comes from the product** (`forBow(product)`) rather than from `c.log`. `c.log` is only the log that opened the make-X interface, and the two disagree on a reachable path: the arrow-shaft buttons stay live whatever log opened the interface, so with an oak log held, pressing one fell into the shaft table's own log id and consumed normal logs instead.
- **The shaft path gained the guards the bow path already had.** It previously skipped the level check and the animation, and had no knife check of its own. All three now apply.
- **`cancel` stops only fletching.** The action runs on its own event id, so cancelling it cannot take other skills' events with it — `stopEvents(player)` would have put out a fire started while fletching next to it. `PlayerAssistant.resetVariables` (reached on every walk step) now calls `Fletching.cancel`, so walking away ends the action.
- **The old batch body is kept verbatim** as `fletchBowInstant` behind `Config.FLETCHING_ONE_BY_ONE_ENABLED`, so this reverts by flipping the flag rather than by reverting the commit.
- **One data correction:** the table gave arrow shafts `levelReq = 15`, but the old shaft path never checked a level, so the rewrite would have enforced that entry for the first time and taken shafts away from everyone below 15. Corrected to 1, which is the real requirement and matches what players could always make.
- **Corrected a wrong claim in `QOL_PLAN.md`.** I had written up a "shaft multiplication bug" (`15 * amount2` adding 15 shafts per matching log in the `logArray` loop). That was wrong on inspection: the loop returns at the end of the first matching log, and 1 log → 15 shafts is the correct ratio. The real defects were the batch behaviour, the wrong log source and the missing guards. Recording it because the bad rationale would have justified a bad fix.

**Files touched:** `server/content/skills/Fletching.java` (ticked action, `cancel`, table visibility, one level correction), `server/game/players/PlayerAssistant.java` (one line in `resetVariables`); added `test/server/content/skills/FletchingTest.java`; updated `QOL_PLAN.md`, `UPDATE_LOG.md`.

**Status:** done for bows and shafts. **915 tests, 0 failures, 0 errors** (759 server + 156 workshop; 8 new). Not done in this phase: `makeArrows`, `makeBolts`, `handleBoltTipping`, `handleBoltTipCrafting` and bow stringing are still one-shot batches with their own `System.currentTimeMillis()` throttles — next, unless Phase 2 (random events) is wanted first. Also open: the magic longbow is level 87 here where OSRS is 85; left alone as it is existing gameplay data, noted in `QOL_PLAN.md`.

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

## 2026-10-17 - Stage D: persistent HUD, leaving the delve, difficulty tiers

**What changed:**

- **Persistent HUD.** `DelveHud` sends `hud` and `chest` payloads every 5 ticks from arrival ("Awaiting spin") until exit/forfeit/run end, then `hudclear`. The client draws it as a settings-panel checkbox **Delve HUD** (default on, persisted as `delveHud`; `::delvehud` flips it) with the +/− chest grid persisted as `delveHudExpanded`. While off, the client still tracks state so re-enabling is immediate, and it hides the panel after 1.5 s on another plane as a backstop.
- **One departure path.** Teleports, involuntary disconnects and voluntary logouts all resolve through idempotent `DelveInstance.forfeitAndLeave(member, reason)`: chest forfeited/cleared, member removed and leader promoted at once, body removed, contribution kept in the denominator, next login message "You left the delve. Your chest of X gp was forfeited." Outer teleports show a stock 2-option prompt before any rune/charge is spent; forced teleports still forfeit; internal movement (`::ledgetp`, ledge, rejoin) does not. Servers shutting down route to `refundAndLeave` (`RESTART_REFUND`) and never forfeit.
- **Config:** `disconnect-grace-seconds` (default **0** = immediate), `chest-phase-disconnect-policy` (**FORFEIT**|`AUTO_WITHDRAW`). CSV outcomes gained `LOGOUT_FORFEIT` and `TELEPORT_FORFEIT`.
- **Difficulty tiers.** New `difficulties.cfg` (Easy/Regular/Hard factors, levels, danger shift, legendary weight) and optional `boss-difficulty.cfg` overrides; `bosses.cfg` gained a trailing `difficulties` column and `waves.cfg` a difficulty column (Easy 6 rounds, Hard 10). Regular is numerically unchanged (all 1.0, original table). Combat applies each factor once and caps the whole product at `absolute-max-hp-multiplier` 7.0 / `absolute-max-hit-multiplier` 1.6. Loot uses `loot-factor` (sqrt on UNCOMMON, RARE quantities untouched), `rare-chance-factor` (75% cap) and probabilistic rounding, with `chest-cap-factor`. Danger shifts clamp Low..Extreme.
- **Levels, unlock and lobby.** Recommended is soft (leader confirmation), minimum is hard (blocks ready/start, names the players). Hard is locked until an account reaches Regular round `hard-unlock-regular-round` (default 6); `::delveunlock` grants it. The leader's Difficulty row opens a 3-option dialogue (`dialogueAction` 9832, buttons 9167–9169) and changing the tier clears ready flags.
- **Persistence v4.** Difficulty, per-tier rare pity (old scalar migrates to Regular), highest round per tier, and the Hard unlock flag; v1–v3 files load with defaults and lose nothing.
- **Telemetry.** `difficulty` is the last CSV column (old rows = Regular). `::delvestats [runs] [tier]` filters and prints tier targets; `::delvesim` and the new `::delvedrops` accept a tier; the per-tier loot/hour and rares/hour table is relative to Regular.
- **Docs:** `docs/roulette/DESIGN.md` (new tier section, revised cap table, §6 departure rules, HUD, roadmap) and `docs/roulette/PACKETS.md` (spin/chest/hud grammar, `hudclear`, lobby dialogues, persist v4).

**Files touched:** `DelveInstance`, `DelveManager`, `DelveLobby`, `DelveMath`, `DelveConfig`, `DelveSettings`, `DelvePersist`, `DelveSession`, `DelveHud`, `DelveReel`, `DelveSim`, `DelveStats`, `DelveCsv`, `DelveChestInterface`, `DelveCommands`, `PlayerAssistant`, `DialogueHandler`, `Client`/logout wiring; client `DelveHud`, `DelveReel`, `ClientSettings`, `Interfaces`, `PluginSidebar`, `client`; configs `difficulties.cfg`, `boss-difficulty.cfg`, `bosses.cfg`, `waves.cfg`, `delves.cfg`; tests `DelveDifficultyTest`, `DelveLeavingTest`, `DelveLobbyTest`, `DelvePersistTest`, `DelveSimTest`, `DelveStatsTest`, `DelveCsvTest`, `DelveReelTest`, `CommandHandlerTest`.

**Status:** done. Full server suite green; client compiles.

## 2026-10-17 - Stage D follow-up: the delve HUD now fits its panel and is clickable

**What changed:**

- **The text fits.** `TextDrawingArea.drawText` centres a string on the x it is given (it passes `x - width/2` to `method385`), so every HUD line was drawn half its own width to the left and ran out of the panel - the same latent bug in `DelveReel`. Both now use `method385`, the left-aligned entry point the XP tracker uses.
- **One layout, one size.** `DelveHud.layout()` builds the exact lines that will be drawn and `measure()` sizes the box from them (first baseline, row pitch, last descender, padding), so the chest grid can no longer fall outside the box. Lines that would exceed the panel are clipped with an ellipsis instead of overhanging it, and the title is measured against the room the +/− button leaves.
- **The panel is no longer click-through.** The +/− is now a drawn 12x12 button (4px of slop around it) with a hover highlight; a left click anywhere on the panel is swallowed by the HUD and a click on the button toggles the chest grid.
- **The click is claimed before the menu.** The handler used to run inside `processHudClicks`, i.e. *after* `processMenuClick` had already fired the menu's permanent "Cancel" entry, so the button never saw the click. `DelveHud.handleClick` is now tested first in `processGameLoop`'s click block, before any menu processing, which also stops a click on the panel walking the player. As a second, independent path, the button also publishes a `Show/Hide delve loot list` menu entry (`ACTION_TOGGLE` 1650) from `processRightClick`, and `client.doAction` routes it - the same mechanism every other overlay uses.
- **Expanding an empty chest is visible.** Collapsed shows `No loot yet - click +` (or `N stacks - click + to show`), expanded with nothing banked shows `No loot banked yet.`, so the +/− toggle always produces a visible change.
- **Fixed-mode offset.** `OverlayManager.layerOriginX/Y` expose the HUD layer's origin: panel coordinates are relative to it (4,4 in fixed), which the old `checkClick` ignored, so the button's hotspot did not line up with the drawn button.
- **The panel's position persists.** `delveHud` joined `OverlayManager.ensureKnown`, so an Alt-drag position is written by `save` and read back by `load` (the panel used to be created after `load` ran).
- Collapsed now reads "N stacks - click + to show" instead of showing nothing where the grid was.

**Files touched:** client `src/ui/DelveHud.java`, `src/ui/DelveReel.java`, `src/ui/OverlayManager.java`, `src/game/client.java`, `UPDATE_LOG.md`.

**Status:** done. Client builds; 706 client harness checks pass; `installBin` refreshed `bin/`.

## 2026-10-17 - Client packaging: GUI, configurable server address, cache auto-update

**What changed:**

- **One jar-friendly configuration path.** New `sign/ClientConfig.java` reads server host/port and the cache URL/version from `client.properties`, layered lowest-first: jar-embedded defaults, working directory, jar folder, `-Dclient.config=...`, then per-key `-Dclient.*` overrides. No file means the previous behaviour exactly (`localhost`, `43594`, `~/Biohazard.474`, no download), so development from `bin/` is untouched.
- **The server address is no longer compiled in.** `client.main` loads the config before `signlink` (which opens the cache files), sets `server`/`portOff`, and `client()` uses the configured host. `signlink.findcachedir()` uses the configured cache folder (keeping the old `Documents/GitHub` and `.pre-deathly` fallbacks for a bare folder name).
- **The cache updater actually works.** `cache/CacheDownloader` was a stub: empty link, hardcoded version, no update path. Rewritten to fetch a version manifest (`cache.version.url`) or use the baked `cache.version`, compare it with `<cache>/cache.version`, download with progress, verify ZIP magic, optionally verify SHA-256, extract with parent-folder creation and zip-slip refusal, and record the new version. A failed check keeps the installed cache instead of re-downloading or downgrading; a failed install leaves the old version marker so the next launch retries. Stale temp files are removed, and install progress repaints per percent rather than per file.
- **Manifest parsing accepts both spellings** (`7` / `7 <sha>` / two lines) and refuses HTML or nonsense rather than treating it as a version.
- **Unresolvable host no longer blocks the window.** `ui/Jframe` resolves the host best-effort and logs a clear message; previously `UnknownHostException` aborted `initUI()`.
- **The packager.** New `tools/packager/ClientPackager.java` + `PackageClient.bat`: a Swing GUI (Server / Cache / Build / Preview / Log tabs) that runs `gradlew installBin`, jars `build/classes/java/main` (not the stale-class-ridden `bin/`) with `Main-Class: game.Loader`, writes `client.properties` both embedded and external, emits `Run.bat` / `Run (console).bat`, optionally bundles LWJGL as a fat jar or a `libs/` folder, and produces a distribution zip. It also packages the cache for upload (`cache.zip` + `cache.version` with SHA-256), excluding player state. `--headless` mode exposes the same build to scripts.
- **Docs:** `docs/CLIENT_PACKAGING.md` covers the jar mechanics, `client.properties` keys, Dropbox/GitHub/self-host direct-link formats, the zip layout, the update workflow, and troubleshooting.
- **Harness:** 16 new checks pin the config defaults/precedence/port fallback and the manifest parser (722 total).

**Files touched:** client `src/sign/ClientConfig.java` (new), `src/sign/signlink.java`, `src/game/client.java`, `src/ui/Jframe.java`, `src/cache/CacheDownloader.java`, `test/ClientHarness.java`, `tools/packager/ClientPackager.java` (new), `PackageClient.bat` (new), `.gitignore`, `docs/CLIENT_PACKAGING.md` (new), `UPDATE_LOG.md`.

**Status:** done. `gradlew check` green (722 harness checks, collision table in sync); `PackageClient.bat --headless` builds and zips the client; a local HTTP test verified first install, up-to-date skip, checksum mismatch rejection, HTML-manifest fallback, and a full pack-cache -> download -> install cycle.
