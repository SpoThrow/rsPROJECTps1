# BOT_WORKSHOP_UX.md — the editor's user experience

The detailed UX spec for the **Bot Workshop** defined in `BOT_TOOLING.md`. That document
says *what* the tool is (architecture, stack, data formats, phases); this one says how it
**feels to use** — the layout, the map interactions, the icon layer, and the step timeline.

The goal in one line, in the author's voice:

> *Open the map, see the world with trees and banks on it, click "new bot", click the
> map to say where to stand, right-click a willow to say "chop", right-click a bank to
> say "bank", press repeat, save. No code.*

---

## 1. Screen layout

One window, four regions, one mode:

```
┌───────────────────────────────────────────────────────────────────────────────┐
│  Bot Workshop   [Explore] [Author] [Region]   Bot: willow_draynor ▾   Plane 0 ▾ │
│                                                       ⌕ Draynor…   [Validate] [Save] │
├──────────────┬───────────────────────────────────────────────┬────────────────┤
│  LAYERS      │                                               │  INSPECTOR     │
│  ☑ icons     │                  MAP CANVAS                   │  ─────────     │
│  ☑ clipping  │           (pan / zoom / hover )               │  Step: Waypoint │
│  ☑ NPCs      │                                               │  Region [Draynor│
│  ☑ regions   │        🌳        🌳        🌳                  │   willows]     │
│  ☑ teleports │                                               │  Jitter  [3 ]  │
│  ─────────   │              🏦 Draynor Bank                  │  ☑ randomize   │
│  RESOURCES   │                                               │                │
│  ▸ trees     │                                               │  ─────────     │
│    • oak     │                                               │  Validation    │
│    • willow  │                                               │  ✅ ok         │
│  ▸ rocks     │                                               │                │
│  ▸ fish      │                                               │                │
├──────────────┴───────────────────────────────────────────────┴────────────────┤
│  TIMELINE   ▸ Waypoint Draynor willows  ▸ Chop willow  ▸ Waypoint bank  ↺ Repeat │
│             [+ step from map]  [⌫]  [⧉]  [↶ undo] [↷ redo]      playhead ◀ ▶    │
└───────────────────────────────────────────────────────────────────────────────┘
```

- **Toolbar** — mode, which bot is open, plane, search, Validate, Save.
- **Left** — layer toggles and the **resource filter** (§4).
- **Centre** — the **map** (§2, §3).
- **Right** — the **inspector**: parameters of the current selection (typed from
  `bot-nodes.json`), plus inline validation and a JSON preview toggle.
- **Bottom** — the **timeline** (§5), shown while a bot is open.

**Modes** keep it simple and prevent mistakes:

| Mode | What clicks do |
| --- | --- |
| **Explore** | Read-only. Inspect tiles, objects, NPCs. No writes. |
| **Author** | Map clicks add steps to the open bot; the timeline is active. |
| **Region** | Drag draws regions/points → `locations.cfg` (§4 of `BOT_TOOLING.md`). |

Modes are a single radio group, not a tool palette, so an author is never guessing what a
click will do.

---

## 2. Map interactions

| Gesture | Action |
| --- | --- |
| Drag / space-drag | Pan |
| Wheel | Zoom (anchored at cursor) |
| `0`–`3` | Switch plane |
| Hover tile | Tooltip: `x, y, plane`, walkable?, objects present, clipping |
| Left-click tile | Select; inspector shows tile details |
| Left-click object | Select object; context menu of its **def actions** |
| Right-click object (Author) | *Add step:* the action chosen from the def (§5) |
| Drag (Region mode) | Draw a region box; release fills the inspector |
| `F` | Frame selection |

**The tile inspector** answers the exact questions an author has while placing a bot:

```
Tile 3087, 3236   plane 0
  walkable   yes
  objects    Willow (1308)  actions: [Chop down]
  clipping   clear
  region     draynor_willows   (kind: tree)
  near       bank  Draynor  (5 tiles)
```

That last line — *nearest bank* — is the single most useful thing the tool can show,
because every gathering bot needs a bank leg and authors otherwise guess.

**Region navigator.** Dungeons are not contiguous (`BOT_ACTIVITIES.md` §1: hill giants at
`3117,9846`), so the map is a **list of regions**, not one infinite plane. A dropdown/
minimap lists known regions and jumps between them; the tile inspector always shows
absolute coordinates (the server's unit — `BOT_TOOLING.md` §9).

---

## 3. The icon layer

The map is *accurate*; the icon layer makes it *usable* (§1b of `BOT_TOOLING.md`).

**Classification is automatic** — an object's `ObjectDef.actions` decides its icon:

| Action on the def | Icon | Class |
| --- | --- | --- |
| `Bank` | 🏦 | service |
| `Chop down` | 🌳 | resource |
| `Mine` | ⛏ | resource |
| `Net`/`Bait`/`Lure`/`Cage`/`Harpoon` | 🐟 | resource |
| `Cook` | 🔥 | service |
| `Smith` | 🔨 | service |
| `Pray`/`Recharge` | ✝ | service |

Behaviour details that keep it readable:

- **Level of detail.** Below a zoom threshold, icons collapse into per-region **clusters**
  with a count (`🌳 24`); zoom in and they separate. Without this, a forest is a green
  smear.
- **Hover** shows the label (`Willow (1308)`) and the action it would offer.
- **Toggleable per class** (left panel) — hide services when placing a gathering bot.
- **Legend** so the symbols are never a puzzle.
- **Curated set first** (decision in `BOT_TOOLING.md`), with real interface sprites from
  the client's sprite archive as a later swap; true model thumbnails deferred.

---

## 4. The resource filter

The left panel lists resource categories discovered from the defs, and selecting one
isolates it: pick `tree.oak` and every oak is highlighted while everything else dims.

This is the "load all the trees in the world" requirement, and it doubles as a **reachability
check**: an author can see at a glance whether the resource they want has a bank nearby.

Combined with the region navigator, the workflow is: *jump to region → filter to the
resource → see the bank → start placing waypoints.*

---

## 5. The step timeline (the primary authoring surface)

A bot is a **list of steps**, which is how it is described in words. The timeline shows
that list literally:

```
Bot: willow_draynor
  ▸ Waypoint   Draynor willows     jitter 3  [randomize ✓]
  ▸ Action     Chop willows        until inventory full
  ▸ Waypoint   Draynor bank        jitter 2  [randomize ✓]
  ▸ Action     Bank                deposit: willow logs
  ↺ Repeat     (the whole block above)
```

### 5.1 Step types

Small, and each maps to a node the runtime already has (`BOT_ROADMAP.md` §5.3):

| Step | Underlying node | Parameters |
| --- | --- | --- |
| **Waypoint** | `WalkTo(region, jitter)` | region, jitter, randomize, arrive-range |
| **Action** | `Interact(...)` | the object/NPC action, until-condition |
| **Gather** | `Gather(resource, untilFull)` | resource, stop condition |
| **Bank** | `BankAll(item)` | item, withdraw list |
| **Withdraw** | `Withdraw(item, qty)` | item, quantity |
| **Wait** | `WaitTicks(n)` | ticks |
| **Repeat** | `Repeat(child, count)` | count (−1 = forever) |
| **If / Selector** | `Selector` (Phase B) | condition children |

The palette for these is **generated from `bot-nodes.json`** (`BOT_TOOLING.md` §5), so a
new `BotState` + annotation appears here with **zero editor changes**. The timeline never
hardcodes a step type.

### 5.2 Adding steps from the map

This is the core loop and it must be two clicks:

1. **Waypoint** — in Author mode, **drag a box** on the map (or click a tile for an exact
   one). The box becomes the region; **jitter** and **randomize** default from the box
   size. This is the "rough idea within certain tiles, not the exact same tile"
   requirement — the box's jitter field is literally the `RandomTileIn` spread
   (`BOT_ROADMAP.md` §5.2).
2. **Action** — **right-click a map object**; the menu is the object's own
   `ObjectDef.actions` (`Chop down`, `Bank`, `Mine`…). Choosing one inserts an Action step
   with typed parameters opened in the inspector.

Because the menu comes from the object, the author can never pick an action the object
doesn't support — the tool and the world agree by construction.

### 5.3 Editing

| Operation | Gesture |
| --- | --- |
| Reorder | Drag a step up/down |
| Nest / un-nest | Indent/outdent (Tab / Shift-Tab) into a `Repeat` or `Sequence` |
| Wrap in repeat | `↺` button on a selection |
| Duplicate / delete | `⧉` / `⌫` |
| Copy / paste | `Ctrl-C` / `Ctrl-V` (steps are plain JSON) |
| Undo / redo | `Ctrl-Z` / `Ctrl-Y` (whole-document history) |
| Edit parameters | Select → right inspector |
| Edit on the map | Select a Waypoint step → its box highlights; drag to move |

### 5.4 The playhead and dry-run

A playhead scrubs the timeline and **draws the route on the map**: the waypoint boxes light
up in order, the travel line is sketched, and the inspector shows the step's compiled node.
`▶` steps through. This is a *preview*, not execution — no server needed — but it catches
the common mistakes (a waypoint with no bank, an action on the wrong object) before export.

### 5.5 Inline validation

Mistakes are shown **where they happen**, not at save time:

| Message | Detected from |
| --- | --- |
| ⚠️ *Target region is empty* | the region's box resolves to no matching object |
| ⚠️ *No bank within reach of this waypoint* | nearest-bank lookup (§2) |
| ⚠️ *Unknown node id* | `bot-nodes.json` parity |
| ⚠️ *Requires level N you don't have* | optional: cross-check against the account |

Red badge on the step, message in the inspector, and a summary in the toolbar's
**Validate** button.

---

## 6. From timeline to data

The timeline **is** the `BotScript`; it is not a lesser format (`BOT_TOOLING.md` Layer 3).

- **Compile** — the step list becomes the `Sequence`/`Repeat` tree and the JSON exactly as
  shown in `BOT_TOOLING.md` Layer 3b.
- **Preview pane** — the inspector can show the generated `bots/*.json` live, so an author
  who wants to see the data can, but never has to edit it by hand.
- **Validate** — calls the Java CLI, which loads the output through the **server's own
  loaders** (`BOT_TOOLING.md` §8). Bad data fails here, not at server start.
- **Save** — writes `Data/cfg/bots/*.json` (and `locations.cfg` for regions) in stable
  order, so the result is **git-diffable**.
- **Round-trip** — reopening a saved bot reconstructs the timeline; switching to the graph
  view is the same document (§7).

---

## 7. Timeline ↔ graph

Two views of one document (`BOT_TOOLING.md` Layer 3b):

- **Timeline** is the default and covers the common bot (a linear gather/bank loop).
- **Graph** appears once the runtime gains `Selector`/`Parallel` (roadmap Phase B) and the
  author needs branching.

Switching never loses data because both are rendered from the same compiled tree. An
author who started in the timeline sees their steps as nodes; a graph author can collapse
a branch back to timeline steps.

---

## 8. Keyboard and mouse model (summary)

| Key | Action |
| --- | --- |
| `1`–`4` | Modes: Explore / Author / Region / … |
| `0`–`3` | Plane |
| `F` | Frame selection |
| `Tab` / `Shift-Tab` | Indent / outdent a step |
| `⌫` | Delete step |
| `⌘/Ctrl-Z`, `⌘/Ctrl-Y` | Undo / redo |
| `⌘/Ctrl-S` | Save |
| `Space` | Playhead play/pause (dry-run) |

Mouse: drag = pan (Explore) or draw box (Region/Author); right-click object = action menu
(Author); wheel = zoom.

---

## 9. Empty, error and edge states

- **No bot open** — map is in Explore; the timeline shows *"Create a bot to begin"*.
- **Empty region** — the box draws but is flagged (§5.5), not silently accepted.
- **Unknown action** — an object with no classified action still offers its raw def
  actions; it just has no icon.
- **Dungeon/plane mismatch** — placing a waypoint on a different plane than the previous
  step flags *"plane change — needs a travel leg"* (`BOT_LOCATIONS.md` A.6).
- **Stale map data** — a saved region pointing at a tile that no longer holds its object is
  flagged by the validator (`BOT_TOOLING.md` §9).

---

## 10. Performance notes that affect UX

- The exporter emits **tiles for the regions the author views**, not the whole world, so
  panning is instant and the tool stays light.
- Icons render only in the viewport, clustered at low zoom (§3).
- The hot path (hover, click, drag) is canvas-level and must stay smooth on a full region.

---

## 11. Acceptance criteria (UX)

- A new author can go from an empty window to a saved, validated `gather_willow` bot in
  **under five minutes** without reading a schema, using only: create bot → drag box →
  right-click object → widget → save.
- Every step offers only parameters that exist in `bot-nodes.json` — no free-text node
  names.
- A mistake (empty region, missing bank) is visible **on the step**, not at export.
- The map shows resources, banks and teleports clearly enough that the author never has to
  look up a coordinate.
- Switching to the graph view and back changes nothing in the saved file.
- Deleting the tool and its outputs leaves the server fully functional
  (`BOT_TOOLING.md` §3, §13).
