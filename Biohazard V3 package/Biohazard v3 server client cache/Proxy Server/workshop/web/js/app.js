/**
 * The viewer's wiring: load the data, build the chrome, keep the inspector and the author honest.
 *
 * `BOT_TOOLING.md` stages T2–T3. What this file deliberately does *not* do is work out anything
 * about the world — no clipping rules, no decode, no footprint arithmetic of its own. Those all come
 * from the exporter and `clip.js`/`region.js`, because a tool whose job is to show the author the
 * world the server walks on must not be the second opinion nobody checks. `ValidateMap` is what holds
 * the exporter to that; this file just draws it.
 *
 * Two things it does own, and owns reluctantly:
 *
 *   - **Which regions are loaded.** The map is continuous, so panning into a region the viewer has
 *     not fetched has to be answered. `pumpRegions` keeps a bounded set of region documents around
 *     the camera and evicts the ones it leaves behind.
 *   - **The text of an authored row.** It drafts one and sends it; the server parses it and answers
 *     with the canonical form, and only that canonical text is ever written to disk. See
 *     `locations.cfg`'s row builder in `LocationsConfig`.
 */

import { selfTest } from './rle.js';
import { Palette } from './palette.js';
import { collidedSize, loadRegion, loadIndex, loadBanks, bankSummary } from './region.js';
import { MapView, OVERVIEW_MAX_SCALE } from './view.js';
import { loadWorld, loadLocations, draftLocations, loadNodes } from './world.js';
import { isWalkable, describe, describeFlags, PROJECTILE_BIT, WALK_MASK } from './clip.js';

/** Icons for the exporter's kinds. The curated set of `BOT_WORKSHOP_UX.md` §3, not sprites yet. */
const ICONS = {
  tree: '🌳',
  rock: '⛏',
  fishing: '🐟',
  bank: '🏦',
  cooking: '🔥',
  smithing: '🔨',
  prayer: '✝',
};

const LAYERS = [
  { key: 'tiles', label: 'tiles' },
  { key: 'icons', label: 'icons' },
  { key: 'clipping', label: 'clipping' },
  { key: 'footprints', label: 'footprints' },
  { key: 'places', label: 'authored places' },
  { key: 'grid', label: 'tile grid' },
];

/**
 * How many region documents to fetch at once, and how many to keep.
 *
 * A cap on both because the world is 1226 regions: without the first, crossing a border would fire
 * dozens of requests at once; without the second, a session of panning would eventually hold the
 * whole world in memory, which is what the lazy loading exists to avoid.
 */
const MAX_PARALLEL_LOADS = 4;
const MAX_LOADED_REGIONS = 64;

const state = {
  palette: null,
  world: null,
  index: [],
  banks: [],
  /** Every authored row, as `locations.json` reports it. */
  places: [],
  kinds: [],
  /** The subset of `kinds` the world can be scanned for; see `loadLocations`. */
  objectKinds: [],
  /** Loaded regions by id, so panning back is instant. */
  regions: new Map(),
  plane: 0,
  layers: { tiles: true, icons: true, clipping: true, footprints: false, places: true, grid: false },
  /** When set, only this kind is drawn — the resource filter of BOT_WORKSHOP_UX.md §4. */
  selectedKind: null,
  classes: { resource: true, service: true },
  /** The authoring tool's own state: the box, and what the server made of the last draft. */
  draft: null,
  draftRow: null,
  selectedPlace: null,
  nodeCount: 0,
};

const dom = {
  regionSelect: document.getElementById('region-select'),
  planeButtons: document.getElementById('plane-buttons'),
  jumpInput: document.getElementById('jump-input'),
  jumpButton: document.getElementById('jump-button'),
  frameButton: document.getElementById('frame-button'),
  worldButton: document.getElementById('world-button'),
  status: document.getElementById('status'),
  layerList: document.getElementById('layer-list'),
  resourceList: document.getElementById('resource-list'),
  resourceTotal: document.getElementById('resource-total'),
  resourceHint: document.getElementById('resource-hint'),
  legend: document.getElementById('legend'),
  inspector: document.getElementById('inspector'),
  tooltip: document.getElementById('tooltip'),
  loading: document.getElementById('loading'),
  canvas: document.getElementById('map'),
  classResource: document.getElementById('class-filter-resource'),
  classService: document.getElementById('class-filter-service'),
  placeList: document.getElementById('place-list'),
  placeCount: document.getElementById('place-count'),
  authorName: document.getElementById('author-name'),
  authorKind: document.getElementById('author-kind'),
  authorTags: document.getElementById('author-tags'),
  authorDraw: document.getElementById('author-draw'),
  authorClear: document.getElementById('author-clear'),
  authorRow: document.getElementById('author-row'),
  authorFeedback: document.getElementById('author-feedback'),
  authorAppend: document.getElementById('author-append'),
  authorCopy: document.getElementById('author-copy'),
  worldSummary: document.getElementById('world-summary'),
};

const view = new MapView(dom.canvas, {
  onHover: (tile, px, py) => {
    if (!tile) {
      dom.tooltip.hidden = true;
      return;
    }
    dom.tooltip.textContent = tooltipText(tile);
    dom.tooltip.hidden = false;
    view.moveTooltip(px, py);
  },
  onSelect: (tile) => {
    renderInspector(tile);
  },
  onViewChanged: () => {
    updateStatus();
    pumpRegions();
  },
  onBox: (box) => {
    adoptBox(box);
  },
});
view.setTooltipElement(dom.tooltip);

// ---- scene ---------------------------------------------------------------------------------

function isVisible(object) {
  if (!object.kind) {
    return false;
  }
  const service = object.service === true;
  if (service && !state.classes.service) {
    return false;
  }
  if (!service && !state.classes.resource) {
    return false;
  }
  return state.selectedKind === null || object.kind === state.selectedKind;
}

function pushScene() {
  view.setScene({
    regions: state.regions,
    world: state.world,
    plane: state.plane,
    palette: state.palette,
    layers: state.layers,
    locations: state.layers.places ? state.places : [],
    draft: state.draft,
    iconFor: (kind) => ICONS[kind] || null,
    isVisible,
  });
}

// ---- boot ----------------------------------------------------------------------------------

async function boot() {
  // The run-length decoder is the one piece of decoding this side does for itself, so it is
  // checked against the Java cases before anything is drawn. A silent disagreement here would
  // shift every tile in the region.
  const decoderFailures = selfTest();
  if (decoderFailures.length > 0) {
    setStatus(`run-length decoder disagrees with the exporter: ${decoderFailures[0]}`, true);
    console.error('rle self-test failures', decoderFailures);
    return;
  }

  buildPlaneButtons();
  buildLayerToggles();
  buildLegend();
  showLoading(true);

  try {
    state.palette = await Palette.load();
    state.world = await loadWorld();
    state.index = await loadIndex();
    // Banks describe the whole world, not just what is exported, so a missing bank list is worth a
    // warning rather than a failed start — the map is still usable without the nearest-bank line.
    try {
      state.banks = await loadBanks();
    } catch (e) {
      state.banks = [];
      console.warn('no bank index; the nearest-bank line will be unavailable', e);
    }
    try {
      const places = await loadLocations();
      state.places = places.rows || [];
      state.kinds = places.kinds || [];
      state.objectKinds = places.objectKinds || [];
    } catch (e) {
      state.places = [];
      state.kinds = [];
      state.objectKinds = [];
      console.warn('no authored places; re-run gradlew workshopExport', e);
    }
  } catch (e) {
    setStatus(`could not load the export: ${e.message}`, true);
    return;
  }

  if (!state.world || state.world.entries.length === 0) {
    setStatus('the export contains no world index — run gradlew workshopExport', true);
    return;
  }

  // Optional: the node palette is a separate export, and the viewer is still a map without it.
  loadNodes().then((nodes) => {
    state.nodeCount = nodes && nodes.nodes ? nodes.nodes.length : 0;
    updateStatus();
  });

  buildRegionSelect();
  buildAuthorKindSelect();
  bindKeys();
  renderPlaces();
  dom.worldSummary.textContent = `${state.world.count} regions, ${state.world.exported} exported`;
  // Push the scene before moving the camera: the first `viewChanged` reads the scene to describe
  // where the camera is, and a scene without the world in it would report "no region here" for a
  // camera that is plainly standing on one.
  pushScene();

  // Open on Lumbridge if it is in the export, otherwise on the first openable region. Framing the
  // whole world at boot would be an overview of 1226 mostly-ungenerated regions, which is not a
  // useful first impression of a tool whose job is inspecting tiles.
  const first = state.world.openable()[0];
  const start = state.world.byId.get(12850) || first;
  if (start) {
    centreRegion(start.regionId, { frameRegion: true });
  }
  await pumpRegions();
  showLoading(false);
}

// ---- chrome --------------------------------------------------------------------------------

function buildPlaneButtons() {
  dom.planeButtons.replaceChildren();
  for (let plane = 0; plane < 4; plane++) {
    const button = el('button', null, String(plane));
    button.type = 'button';
    button.title = `Show plane ${plane} (key ${plane})`;
    button.setAttribute('aria-pressed', String(plane === state.plane));
    button.addEventListener('click', () => setPlane(plane));
    dom.planeButtons.append(button);
  }
}

function buildLayerToggles() {
  dom.layerList.replaceChildren();
  for (const layer of LAYERS) {
    const label = el('label');
    const input = document.createElement('input');
    input.type = 'checkbox';
    input.checked = state.layers[layer.key];
    input.addEventListener('change', () => {
      state.layers[layer.key] = input.checked;
      pushScene();
    });
    label.append(input, document.createTextNode(layer.label));
    dom.layerList.append(label);
  }
}

function buildRegionSelect() {
  dom.regionSelect.replaceChildren();
  for (const entry of state.index) {
    const option = document.createElement('option');
    option.value = String(entry.regionId);
    option.textContent = `${entry.regionId}  (${entry.baseX},${entry.baseY})  ${entry.objects} objects`;
    dom.regionSelect.append(option);
  }
  dom.regionSelect.addEventListener('change', () => {
    centreRegion(Number(dom.regionSelect.value), { frameRegion: true });
  });
}

function buildAuthorKindSelect() {
  dom.authorKind.replaceChildren();
  for (const kind of state.kinds) {
    const option = document.createElement('option');
    option.value = kind;
    option.textContent = kind;
    dom.authorKind.append(option);
  }
  if (state.kinds.includes('bank')) {
    dom.authorKind.value = 'bank';
  }
}

function buildLegend() {
  const rows = [
    ['swatch', 'rgba(224,87,74,0.42)', 'blocks walking (any bit in 0x12801FF)'],
    ['swatch', 'rgba(224,163,62,0.26)', 'projectile-solid only — still walkable'],
    ['swatch', 'rgba(110,168,254,0.07)', 'region in the index whose document is not fetched yet'],
    ['swatch', 'rgba(120,132,150,0.13)', 'region the export skipped — not in this map'],
  ];
  const iconRows = Object.entries(ICONS).map(([kind, glyph]) => ['icon', glyph, kind]);
  const container = el('div');
  for (const [type, value, label] of [...rows, ...iconRows]) {
    const row = el('div', 'legend-row');
    const marker = el('span', type);
    if (type === 'swatch') {
      marker.style.background = value;
    } else {
      marker.textContent = value;
    }
    row.append(marker, el('span', 'label', label));
    container.append(row);
  }
  dom.legend.replaceChildren(container);
}

/** The regions currently loaded *and* on screen; what the resource panel and the count describe. */
function visibleLoadedRegions() {
  if (!state.world) {
    return [];
  }
  const window = view.visibleWindow();
  const found = [];
  for (const entry of state.world.overlapping(window.minX, window.maxX, window.minY, window.maxY)) {
    const region = state.regions.get(entry.regionId);
    if (region) {
      found.push(region);
    }
  }
  return found;
}

/**
 * The left panel's resource list, over the regions on screen.
 *
 * It counts what is *loaded* and visible rather than the whole world, because the counts have to
 * agree with the icons the author can see; a world-wide total that does not match the map would be
 * worse than no total.
 */
function renderResources() {
  dom.resourceList.replaceChildren();
  const regions = visibleLoadedRegions();
  if (regions.length === 0) {
    dom.resourceTotal.textContent = '';
    dom.resourceHint.textContent = 'No region is loaded here yet. Pan onto one, or jump to it.';
    return;
  }

  const counts = new Map();
  const services = new Map();
  for (const region of regions) {
    for (const [kind, n] of region.kindCounts()) {
      counts.set(kind, (counts.get(kind) || 0) + n);
    }
    for (const { kind, service } of region.kinds()) {
      services.set(kind, service);
    }
  }
  const total = Array.from(counts.values()).reduce((a, b) => a + b, 0);
  dom.resourceTotal.textContent = String(total);

  const kinds = Array.from(counts.keys()).sort();
  if (kinds.length === 0) {
    dom.resourceHint.textContent = 'No object in the loaded regions carries an action the '
      + 'classifier recognises, so there is nothing to filter or draw. That is a fact about the '
      + 'regions, not a failure.';
    return;
  }

  for (const kind of kinds) {
    const row = el('div', 'resource-row');
    if (state.selectedKind === kind) {
      row.classList.add('selected');
    }
    row.append(
      el('span', 'icon', ICONS[kind] || '•'),
      el('span', 'label', `${kind}${services.get(kind) ? ' (service)' : ''}`),
      el('span', 'n', String(counts.get(kind) || 0)),
    );
    row.title = state.selectedKind === kind ? 'Click to stop isolating this kind' : `Click to isolate ${kind}`;
    row.addEventListener('click', () => {
      state.selectedKind = state.selectedKind === kind ? null : kind;
      renderResources();
      pushScene();
    });
    dom.resourceList.append(row);
  }

  dom.resourceHint.textContent = state.selectedKind
    ? `Showing only ${state.selectedKind}. Click it again to show everything.`
    : 'Click a kind to isolate it. Everything with no classified action is left undrawn either '
      + 'way — a wrong icon is worse than no icon.';
}

/** The authored places, clickable to jump to them. */
function renderPlaces() {
  dom.placeList.replaceChildren();
  dom.placeCount.textContent = String(state.places.length);
  if (state.places.length === 0) {
    dom.placeList.append(el('p', 'hint',
      'No authored rows yet. Drag a box with "draw box" on, then append it.'));
    return;
  }
  const sorted = [...state.places].sort((a, b) => a.kind.localeCompare(b.kind)
    || a.name.localeCompare(b.name));
  for (const place of sorted) {
    const row = el('div', 'place-row');
    if (state.selectedPlace === place.name) {
      row.classList.add('selected');
    }
    const size = place.point ? 'point' : `${place.w}x${place.h}`;
    row.append(
      el('span', 'icon', ICONS[place.kind] || '•'),
      el('span', 'label', place.name),
      el('span', 'n', `p${place.plane} ${size}`),
    );
    row.title = `${place.row}\nClick to jump to it`;
    row.addEventListener('click', () => {
      state.selectedPlace = place.name;
      if (place.plane !== state.plane) {
        setPlane(place.plane);
      }
      view.centreOn(place.x + Math.floor(place.w / 2), place.y + Math.floor(place.h / 2));
      renderPlaces();
    });
    dom.placeList.append(row);
  }
}

// ---- region loading ------------------------------------------------------------------------

async function pumpRegions() {
  if (!state.world) {
    return;
  }
  // In the overview nothing is drawn tile by tile, so nothing needs loading. This is also the guard
  // that stops "frame the world" from queueing all 1226 regions.
  if (view.scale < OVERVIEW_MAX_SCALE) {
    return;
  }
  while (inFlight.size < MAX_PARALLEL_LOADS) {
    const next = nextRegionToLoad();
    if (!next) {
      return;
    }
    loadOneRegion(next);
  }
}

const inFlight = new Set();

/**
 * Regions this export lists but that would not fetch. Kept beside the world index rather than by
 * clearing the entry's `exported` flag: the export did write the region and the index is the shared
 * description of the world, so a transient fetch failure must not rewrite it — including in the
 * overview, which would then shade the region as if the export had skipped it.
 */
const failed = new Set();

/** The missing region nearest the camera, or null when everything on screen is loaded. */
function nextRegionToLoad() {
  const window = view.visibleWindow();
  const candidates = state.world.overlapping(window.minX, window.maxX, window.minY, window.maxY)
    .filter((entry) => entry.exported && !state.regions.has(entry.regionId)
      && !failed.has(entry.regionId) && !inFlight.has(entry.regionId));
  if (candidates.length === 0) {
    return null;
  }
  candidates.sort((a, b) => distanceToCamera(a) - distanceToCamera(b));
  return candidates[0];
}

function distanceToCamera(entry) {
  const dx = (entry.baseX + 32) - view.cx;
  const dy = (entry.baseY + 32) - view.cy;
  return dx * dx + dy * dy;
}

async function loadOneRegion(entry) {
  inFlight.add(entry.regionId);
  try {
    const region = await loadRegion(entry.regionId);
    state.regions.set(entry.regionId, region);
    evictDistantRegions();
    pushScene();
    renderResources();
    updateStatus();
  } catch (e) {
    // Worth saying once, not on every pan.
    console.warn(`could not load region ${entry.regionId}`, e);
    failed.add(entry.regionId);
    setStatus(`region ${entry.regionId} failed to load: ${e.message}`, true);
  } finally {
    inFlight.delete(entry.regionId);
    pumpRegions();
  }
}

/** Keeps the loaded set bounded without thrashing the region the author is looking at. */
function evictDistantRegions() {
  while (state.regions.size > MAX_LOADED_REGIONS) {
    let worst = null;
    let worstDistance = -1;
    for (const entry of state.world.entries) {
      if (!state.regions.has(entry.regionId)) {
        continue;
      }
      const distance = distanceToCamera(entry);
      if (distance > worstDistance) {
        worst = entry;
        worstDistance = distance;
      }
    }
    if (!worst) {
      return;
    }
    state.regions.delete(worst.regionId);
  }
}

/** Jump the camera to a region (loading it), optionally framing it. */
async function centreRegion(regionId, { frameRegion } = {}) {
  const entry = state.world ? state.world.byRegionId(regionId) : null;
  if (!entry) {
    setStatus(`region ${regionId} is not in the world index`, true);
    return;
  }
  dom.regionSelect.value = String(regionId);
  view.centreOn(entry.baseX + 32, entry.baseY + 32);
  if (frameRegion) {
    view.frameById(regionId);
  }
  pumpRegions();
}

function setPlane(plane) {
  state.plane = plane;
  for (const [index, button] of Array.from(dom.planeButtons.children).entries()) {
    button.setAttribute('aria-pressed', String(index === plane));
  }
  // The selection belongs to a plane, so a plane change clears it rather than leaving the
  // inspector describing a tile that is no longer on screen.
  view.setSelection(null);
  renderInspector(null);
  pushScene();
}

// ---- inspector -----------------------------------------------------------------------------

function renderInspector(tile) {
  if (!tile) {
    dom.inspector.replaceChildren(el('p', 'hint', 'Click a tile to inspect it. Hovering shows the '
      + 'same summary.'));
    return;
  }

  const parts = [];
  parts.push(el('div', 'insp-title', `Tile ${tile.x}, ${tile.y}   plane ${tile.plane}`));

  const region = view.regionAt(tile.x, tile.y);
  const entry = view.entryAt(tile.x, tile.y);
  if (!region) {
    parts.push(el('p', 'hint', entry
      ? `Region ${entry.regionId} is in the index but its document is not loaded. Pan or zoom to `
        + 'fetch it, or run gradlew workshopExport with an area that covers it.'
      : 'No region in the map index covers this tile — this is outside the world the export knows.'));
    parts.push(bankLine(tile));
    dom.inspector.replaceChildren(...parts);
    return;
  }

  const local = region.tile(tile.plane, region.localX(tile.x), region.localY(tile.y));
  if (!local) {
    parts.push(el('p', 'hint', 'That tile is outside the loaded region.'));
    dom.inspector.replaceChildren(...parts);
    return;
  }

  const walkable = isWalkable(local.clip);
  const facts = el('dl', 'kv');
  addFact(facts, 'walkable', walkable ? 'yes' : 'no', walkable ? 'yes' : 'no');
  addFact(facts, 'clipping', `0x${(local.clip >>> 0).toString(16).toUpperCase()}${local.clip === 0 ? ' (clear)' : ''}`);
  for (const part of describe(local.clip)) {
    addFact(facts, '', `${part.hex}  ${part.name}${part.blocks ? '' : ' — does not block walking'}`);
  }
  addFact(facts, 'flags', `0x${(local.flags >>> 0).toString(16).toUpperCase()}`);
  for (const line of describeFlags(local.flags)) {
    addFact(facts, '', line);
  }
  addFact(facts, 'floor', `underlay ${local.underlay}  overlay ${local.overlay}`);
  addFact(facts, 'region', `${region.id}  local ${region.localX(tile.x)},${region.localY(tile.y)}`);
  parts.push(facts);

  // Objects, with the origin tile distinguished from the rest of the footprint: "a 2x2 tree is
  // here" and "you are standing on this tree" are different statements.
  const objects = region.objectsAt(tile.plane, tile.x, tile.y);
  parts.push(el('div', 'insp-title', `Objects (${objects.length})`));
  if (objects.length === 0) {
    parts.push(el('p', 'hint', 'Nothing stands here.'));
  }
  for (const object of objects) {
    const box = el('div', 'obj');
    const name = object.name || '(unnamed)';
    box.append(el('div', 'obj-name', name));
    const [width, height] = collidedSize(object);
    const onOrigin = object.x === tile.x && object.y === tile.y;
    box.append(el('div', 'obj-meta',
      `id ${object.id}  ${onOrigin ? 'origin' : `footprint ${width}x${height} from ${object.x},${object.y}`}`
      + `  type ${object.type}  rot ${object.rotation}  ${width}x${height}`));
    box.append(el('div', 'obj-meta',
      object.kind
        ? `kind ${object.kind} (${object.service ? 'service' : 'resource'})  clip 0x${(object.clip >>> 0).toString(16).toUpperCase()}`
        : `unclassified  clip 0x${(object.clip >>> 0).toString(16).toUpperCase()}`));
    box.append(el('div', 'obj-actions',
      object.actions.length ? object.actions.join(' · ') : 'no actions on the definition'));
    parts.push(box);
  }

  // Authored places covering this tile: what a bot asking for "the nearest bank" would resolve to.
  const covering = state.places.filter((place) => place.plane === tile.plane
    && tile.x >= place.x && tile.x < place.x + place.w
    && tile.y >= place.y && tile.y < place.y + place.h);
  if (covering.length > 0) {
    parts.push(el('div', 'insp-title', `Authored places (${covering.length})`));
    for (const place of covering) {
      const box = el('div', 'obj');
      box.append(el('div', 'obj-name', `${place.name} — ${place.kind}`));
      box.append(el('div', 'obj-meta', place.row));
      parts.push(box);
    }
  }

  parts.push(bankLine(tile));
  dom.inspector.replaceChildren(...parts);
}

/** The nearest-bank line — the one thing BOT_WORKSHOP_UX.md §2 singles out as most useful. */
function bankLine(tile) {
  if (state.banks.length === 0) {
    return el('p', 'hint', 'No bank index loaded (banks.json is missing from the export).');
  }
  const { samePlane, anyPlane } = bankSummary(state.banks, tile.x, tile.y, tile.plane);
  const line = el('div', 'bank-line');
  if (anyPlane === null) {
    return line;
  }
  if (samePlane !== null) {
    line.classList.add('near');
    line.append(el('div', null, `nearest bank on this plane: ${describeBank(samePlane)}`));
  }
  // Only worth mentioning when it is genuinely a different, nearer option — otherwise the two lines
  // name the same bank and the second is noise.
  if (anyPlane.bank.plane !== tile.plane && (samePlane === null || anyPlane.distance < samePlane.distance)) {
    line.append(el('div', 'bank-other',
      `nearest on any plane: ${describeBank(anyPlane)}  ⚠ plane ${anyPlane.bank.plane}, so this leg needs a plane change`));
  }
  return line;
}

function describeBank({ bank, distance }) {
  const name = bank.name || `object ${bank.id}`;
  const tiles = distance < 0.5 ? 'on this tile' : `${distance.toFixed(1)} tiles away`;
  return `${name} (${bank.id}) at ${bank.x},${bank.y} — ${tiles}`;
}

function tooltipText(tile) {
  const region = view.regionAt(tile.x, tile.y);
  if (!region) {
    const entry = view.entryAt(tile.x, tile.y);
    return `${tile.x}, ${tile.y}  plane ${tile.plane}\n`
      + (entry ? `region ${entry.regionId} — not loaded` : 'no region here');
  }
  const local = region.tile(tile.plane, region.localX(tile.x), region.localY(tile.y));
  if (!local) {
    return `${tile.x}, ${tile.y}  plane ${tile.plane}\n(no data)`;
  }
  const objects = region.objectsAt(tile.plane, tile.x, tile.y);
  const lines = [
    `${tile.x}, ${tile.y}  plane ${tile.plane}`,
    isWalkable(local.clip) ? 'walkable' : `blocked (clip 0x${(local.clip >>> 0).toString(16).toUpperCase()})`,
  ];
  for (const object of objects.slice(0, 4)) {
    const label = object.name || `object ${object.id}`;
    const action = object.actions[0] ? ` — ${object.actions[0]}` : '';
    lines.push(`${label} (${object.id})${action}`);
  }
  if (objects.length > 4) {
    lines.push(`… and ${objects.length - 4} more`);
  }
  return lines.join('\n');
}

// ---- authoring (BOT_TOOLING.md T3) ---------------------------------------------------------

/** A drag has finished: remember the box and ask the server what it would write. */
function adoptBox(box) {
  const x0 = Math.min(box.x0, box.x1);
  const x1 = Math.max(box.x0, box.x1);
  const y0 = Math.min(box.y0, box.y1);
  const y1 = Math.max(box.y0, box.y1);
  state.draft = { x0, y0, x1, y1 };
  pushScene();
  checkDraft();
}

function currentDraft() {
  if (!state.draft) {
    return null;
  }
  const x = Math.min(state.draft.x0, state.draft.x1);
  const y = Math.min(state.draft.y0, state.draft.y1);
  const w = Math.abs(state.draft.x1 - state.draft.x0) + 1;
  const h = Math.abs(state.draft.y1 - state.draft.y0) + 1;
  const name = (dom.authorName.value || '').trim() || 'unnamed_place';
  const kind = dom.authorKind.value || 'bank';
  const tags = (dom.authorTags.value || '').split(',').map((t) => t.trim()).filter(Boolean);
  return { x, y, w, h, plane: state.plane, name, kind, tags };
}

/**
 * The candidate row, built the way an author reads it.
 *
 * Written here rather than fetched because it has to appear the instant the box is dragged; the
 * server is the authority on whether it is *accepted*, which is what `checkDraft` asks. A 1x1 drag
 * is written as a `point` because that is the row the file would carry for it either way.
 */
function draftRowText(draft) {
  const parts = [];
  parts.push(draft.w === 1 && draft.h === 1 ? 'point  =' : 'region =');
  parts.push(draft.name);
  parts.push(`x ${draft.x}`);
  parts.push(`y ${draft.y}`);
  if (!(draft.w === 1 && draft.h === 1)) {
    parts.push(`w ${draft.w}`);
    parts.push(`h ${draft.h}`);
  }
  parts.push(`plane ${draft.plane}`);
  parts.push(`kind ${draft.kind}`);
  if (draft.tags.length > 0) {
    parts.push(`tags ${draft.tags.join(',')}`);
  }
  return parts.join(' ');
}

/**
 * Objects of the draft's kind inside its box, counted from the regions the viewer has loaded.
 *
 * This is a warning, not the check: `ValidateMap` scans the world on the server and is the authority.
 * But a row that claims oaks where there are none is a bot walking to an empty field, and the only
 * place that failure surfaces today is the validator — after the file has already changed. Since the
 * viewer is already holding the boxes the author is drawing on, the ones it can see are counted here,
 * before the append. {@code loaded}/{@code total} are reported so a partial answer can say so rather
 * than implying a world-wide one.
 *
 * @return {{found: number, loaded: number, total: number}|null} null when the kind has no objects
 *     behind it at all (a shop, a teleport, a spawn), where "0 found" would be a false alarm.
 */
function countDraftObjects(draft) {
  if (!state.objectKinds.includes(draft.kind)) {
    return null;
  }
  let found = 0;
  let loaded = 0;
  let total = 0;
  const maxX = draft.x + draft.w - 1;
  const maxY = draft.y + draft.h - 1;
  for (const entry of state.world.overlapping(draft.x, maxX, draft.y, maxY)) {
    total++;
    const region = state.regions.get(entry.regionId);
    if (!region) {
      continue;
    }
    loaded++;
    for (const object of region.objects) {
      if (object.plane !== draft.plane || object.kind !== draft.kind) {
        continue;
      }
      // The origin tile, not the footprint: this mirrors the validator's scan, which walks the same
      // per-object coordinates out of the server's own region.
      if (object.x >= draft.x && object.x <= maxX && object.y >= draft.y && object.y <= maxY) {
        found++;
      }
    }
  }
  return { found, loaded, total };
}

/** One clause describing what the count found, or an empty string when there is nothing to say. */
function describeDraftObjects(count, kind) {
  if (!count) {
    return '';
  }
  if (count.loaded === 0) {
    return '; no part of this box is loaded yet, so nothing could be counted';
  }
  if (count.loaded < count.total) {
    return `; ${count.found} ${kind} object(s) in the ${count.loaded} of ${count.total} region(s) `
      + 'checked so far';
  }
  if (count.found === 0) {
    return `; no ${kind} object is in this box — workshopValidate will report that row as EMPTY`;
  }
  return `; ${count.found} ${kind} object(s) in the box`;
}

async function checkDraft() {
  const draft = currentDraft();
  if (!draft) {
    dom.authorRow.textContent = '';
    dom.authorFeedback.textContent = 'Drag a box on the map with "draw box" on.';
    dom.authorAppend.disabled = true;
    return;
  }
  const candidate = draftRowText(draft);
  dom.authorRow.textContent = candidate;
  dom.authorFeedback.textContent = 'checking…';
  dom.authorAppend.disabled = true;
  try {
    const report = await draftLocations([candidate], 'check');
    const row = report.rows[0];
    if (row && row.ok) {
      state.draftRow = row.canonical;
      dom.authorRow.textContent = row.canonical;
      dom.authorFeedback.textContent = `${draft.w}x${draft.h} box on plane ${draft.plane} — the server `
        + `accepts this row${describeDraftObjects(countDraftObjects(draft), draft.kind)}.`;
      dom.authorAppend.disabled = false;
    } else {
      state.draftRow = null;
      dom.authorFeedback.textContent = `rejected: ${row ? row.error : 'no row'}`;
    }
  } catch (e) {
    state.draftRow = null;
    dom.authorFeedback.textContent = `could not reach the workshop server: ${e.message}`;
  }
}

async function appendDraft() {
  if (!state.draftRow) {
    return;
  }
  dom.authorAppend.disabled = true;
  dom.authorFeedback.textContent = 'appending…';
  try {
    const report = await draftLocations([state.draftRow], 'append');
    if (report.appended > 0) {
      dom.authorFeedback.textContent = `wrote ${report.appended} row to locations.cfg — re-run `
        + 'gradlew workshopExport to see it on the map.';
      state.draft = null;
      pushScene();
      // Re-read the table so the panel shows the new row; the file on disk is the source of truth.
      try {
        const places = await loadLocations();
        state.places = places.rows || [];
        renderPlaces();
      } catch (e) {
        console.warn('appended, but could not re-read locations.json', e);
      }
    } else {
      dom.authorFeedback.textContent = `not written: ${report.rejected} row(s) rejected`;
    }
  } catch (e) {
    dom.authorFeedback.textContent = `append failed: ${e.message}`;
  } finally {
    dom.authorAppend.disabled = !state.draftRow;
  }
}

// ---- misc chrome ---------------------------------------------------------------------------

function jump() {
  const raw = dom.jumpInput.value.trim();
  const match = raw.match(/^(-?\d+)\s*[,\s]\s*(-?\d+)$/);
  if (!match) {
    setStatus('enter a tile as x,y — for example 3208,3218', true);
    return;
  }
  const x = Number(match[1]);
  const y = Number(match[2]);

  // Regions are not contiguous, so "jump to 3117,9846" may mean moving to a different region first.
  // The world index knows every region's corner, so say which one owns the tile instead of silently
  // doing nothing.
  const entry = state.world.regionAt(x, y);
  if (!entry) {
    setStatus(`${x},${y} is not inside any region the map index knows`, true);
    return;
  }
  if (!entry.exported) {
    setStatus(`region ${entry.regionId} covers ${x},${y} but this export skipped it`, true);
    return;
  }
  view.centreOn(x, y);
  pumpRegions();
  setStatus(`centre ${x},${y}  ·  region ${entry.regionId}`);
}

function bindKeys() {
  window.addEventListener('keydown', (event) => {
    if (event.target instanceof HTMLInputElement || event.target instanceof HTMLSelectElement) {
      return;
    }
    if (event.key >= '0' && event.key <= '3') {
      setPlane(Number(event.key));
      return;
    }
    const panTiles = Math.max(4, Math.round(view.w / view.scale / 4));
    switch (event.key) {
      case 'f':
      case 'F':
        view.frameRegion();
        break;
      case 'w':
      case 'W':
        view.frameWorld();
        break;
      case 'Escape':
        view.setSelection(null);
        renderInspector(null);
        break;
      case '+':
      case '=':
        view.zoomAt(view.w / 2, view.h / 2, 1.25);
        break;
      case '-':
        view.zoomAt(view.w / 2, view.h / 2, 1 / 1.25);
        break;
      case 'ArrowLeft':
        view.centreOn(view.cx - panTiles, view.cy);
        break;
      case 'ArrowRight':
        view.centreOn(view.cx + panTiles, view.cy);
        break;
      case 'ArrowUp':
        view.centreOn(view.cx, view.cy + panTiles);
        break;
      case 'ArrowDown':
        view.centreOn(view.cx, view.cy - panTiles);
        break;
      default:
        return;
    }
    event.preventDefault();
  });
}

function updateStatus() {
  if (!state.world) {
    return;
  }
  const entry = view.entryAt(Math.round(view.cx), Math.round(view.cy));
  const overview = view.scale < OVERVIEW_MAX_SCALE ? '  ·  overview' : '';
  const nodes = state.nodeCount ? `  ·  ${state.nodeCount} bot nodes` : '';
  const bankNote = state.banks.length ? '' : '  ·  no bank index';
  setStatus(`${view.scale.toFixed(2)} px/tile  ·  centre ${Math.round(view.cx)},${Math.round(view.cy)}`
    + (entry ? `  ·  region ${entry.regionId}` : '  ·  no region here')
    + `  ·  plane ${state.plane}${overview}  ·  ${state.regions.size} region(s) loaded`
    + ` of ${state.world.exported} exported${bankNote}${nodes}`);
}

function setStatus(message, bad = false) {
  dom.status.textContent = message;
  dom.status.classList.toggle('bad', bad);
}

function showLoading(visible) {
  dom.loading.hidden = !visible;
}

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) {
    node.className = className;
  }
  if (text !== undefined) {
    node.textContent = text;
  }
  return node;
}

function addFact(list, key, value, cls) {
  list.append(el('dt', null, key), el('dd', cls || null, value));
}

// ---- go ------------------------------------------------------------------------------------

dom.jumpButton.addEventListener('click', jump);
dom.jumpInput.addEventListener('keydown', (event) => {
  if (event.key === 'Enter') {
    jump();
  }
});
dom.frameButton.addEventListener('click', () => {
  // The camera wins when it is over a region. When it sits in a gap between regions — which is most
  // of the world at overview zoom — the picker's region is the one the author means; falling through
  // to the whole-world framing there would look like the button did nothing at all.
  if (view.entryAt(Math.round(view.cx), Math.round(view.cy))) {
    view.frameRegion();
    pumpRegions();
    return;
  }
  const picked = state.world ? state.world.byRegionId(Number(dom.regionSelect.value)) : null;
  if (picked) {
    view.frameById(picked.regionId);
    pumpRegions();
    return;
  }
  view.frameRegion();
});
dom.worldButton.addEventListener('click', () => {
  view.frameWorld();
  pumpRegions();
});
dom.classResource.addEventListener('change', () => {
  state.classes.resource = dom.classResource.checked;
  pushScene();
});
dom.classService.addEventListener('change', () => {
  state.classes.service = dom.classService.checked;
  pushScene();
});

dom.authorDraw.addEventListener('change', () => {
  view.setMode(dom.authorDraw.checked ? 'box' : 'pan');
  dom.authorFeedback.textContent = dom.authorDraw.checked
    ? 'Drag a box on the map. The box is sent to the server to be parsed before anything is written.'
    : 'Box drawing off; the map pans again.';
});
dom.authorClear.addEventListener('click', () => {
  state.draft = null;
  state.draftRow = null;
  pushScene();
  checkDraft();
});
for (const input of [dom.authorName, dom.authorTags]) {
  input.addEventListener('change', checkDraft);
}
dom.authorKind.addEventListener('change', checkDraft);
dom.authorAppend.addEventListener('click', appendDraft);
dom.authorCopy.addEventListener('click', async () => {
  const text = state.draftRow || dom.authorRow.textContent;
  if (!text) {
    return;
  }
  try {
    await navigator.clipboard.writeText(text);
    dom.authorFeedback.textContent = 'row copied to the clipboard';
  } catch (e) {
    // Clipboard access needs a secure context and a user gesture; the row is visible either way.
    dom.authorFeedback.textContent = 'clipboard blocked — select the row text and copy it manually';
  }
});

dom.worldSummary.addEventListener('click', () => {
  view.frameWorld();
  pumpRegions();
});

boot();

// Referenced so the values stay discoverable from the console while the viewer is running.
window.workshop = { state, view, WALK_MASK, PROJECTILE_BIT, OVERVIEW_MAX_SCALE };
