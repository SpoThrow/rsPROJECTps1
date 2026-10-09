/**
 * The viewer's wiring: load the data, build the chrome, keep the inspector honest.
 *
 * `BOT_TOOLING.md` stage T2. What this file deliberately does *not* do is work out anything about
 * the world — no clipping rules, no decode, no footprint arithmetic of its own. Those all come from
 * the exporter and `clip.js`/`region.js`, because a tool whose job is to show the author the world
 * the server walks on must not be the second opinion nobody checks. `ValidateMap` is what holds the
 * exporter to that; this file just draws it.
 */

import { selfTest } from './rle.js';
import { Palette } from './palette.js';
import { collidedSize, loadRegion, loadIndex, loadBanks, bankSummary } from './region.js';
import { MapView } from './view.js';
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
  { key: 'grid', label: 'tile grid' },
];

const state = {
  palette: null,
  index: [],
  banks: [],
  /** Loaded regions by id, so switching back and forth is instant. */
  regions: new Map(),
  region: null,
  plane: 0,
  layers: { tiles: true, icons: true, clipping: true, footprints: false, grid: false },
  /** When set, only this kind is drawn — the resource filter of BOT_WORKSHOP_UX.md §4. */
  selectedKind: null,
  classes: { resource: true, service: true },
};

const dom = {
  regionSelect: document.getElementById('region-select'),
  planeButtons: document.getElementById('plane-buttons'),
  jumpInput: document.getElementById('jump-input'),
  jumpButton: document.getElementById('jump-button'),
  frameButton: document.getElementById('frame-button'),
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
  onViewChanged: () => updateStatus(),
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
    region: state.region,
    plane: state.plane,
    palette: state.palette,
    layers: state.layers,
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

  try {
    state.palette = await Palette.load();
    state.index = await loadIndex();
    // Banks describe the whole world, not just what is exported, so a missing bank list is worth a
    // warning rather than a failed start — the map is still usable without the nearest-bank line.
    try {
      state.banks = await loadBanks();
    } catch (e) {
      state.banks = [];
      console.warn('no bank index; the nearest-bank line will be unavailable', e);
    }
  } catch (e) {
    setStatus(`could not load the export: ${e.message}`, true);
    return;
  }

  if (state.index.length === 0) {
    setStatus('the export contains no regions — run gradlew workshopExport', true);
    return;
  }

  buildRegionSelect();
  bindKeys();
  await selectRegion(state.index[0].regionId, { frame: true });
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
      view.setLayers(state.layers);
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
    selectRegion(Number(dom.regionSelect.value), { frame: true });
  });
}

function buildLegend() {
  const rows = [
    ['swatch', 'rgba(224,87,74,0.42)', 'blocks walking (any bit in 0x12801FF)'],
    ['swatch', 'rgba(224,163,62,0.26)', 'projectile-solid only — still walkable'],
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

/** The left panel's resource list, rebuilt whenever the region changes. */
function renderResources() {
  dom.resourceList.replaceChildren();
  const region = state.region;
  if (!region) {
    dom.resourceTotal.textContent = '';
    return;
  }
  const counts = region.kindCounts();
  const kinds = region.kinds();
  const total = Array.from(counts.values()).reduce((a, b) => a + b, 0);
  dom.resourceTotal.textContent = String(total);

  if (kinds.length === 0) {
    dom.resourceHint.textContent = 'No object in this region carries an action the classifier '
      + 'recognises, so there is nothing to filter or draw. That is a fact about the region, not '
      + 'a failure.';
    return;
  }

  for (const { kind, service } of kinds) {
    const row = el('div', 'resource-row');
    if (state.selectedKind === kind) {
      row.classList.add('selected');
    }
    row.append(
      el('span', 'icon', ICONS[kind] || '•'),
      el('span', 'label', `${kind}${service ? ' (service)' : ''}`),
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

// ---- region loading ------------------------------------------------------------------------

async function selectRegion(regionId, { frame } = {}) {
  showLoading(true);
  try {
    let region = state.regions.get(regionId);
    if (!region) {
      region = await loadRegion(regionId);
      state.regions.set(regionId, region);
    }
    state.region = region;
    dom.regionSelect.value = String(regionId);
    pushScene();
    view.setSelection(null);
    renderResources();
    renderInspector(null);
    if (frame) {
      view.frameRegion();
    }
    updateStatus();
  } catch (e) {
    setStatus(`could not load region ${regionId}: ${e.message}`, true);
  } finally {
    showLoading(false);
  }
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
  updateStatus();
}

// ---- inspector -----------------------------------------------------------------------------

function renderInspector(tile) {
  if (!tile || !state.region) {
    dom.inspector.replaceChildren(el('p', 'hint', 'Click a tile to inspect it. Hovering shows the '
      + 'same summary.'));
    return;
  }
  const region = state.region;
  const local = region.tile(tile.plane, region.localX(tile.x), region.localY(tile.y));
  if (!local) {
    dom.inspector.replaceChildren(el('p', 'hint', 'That tile is outside the loaded region.'));
    return;
  }

  const parts = [];
  parts.push(el('div', 'insp-title', `Tile ${tile.x}, ${tile.y}   plane ${tile.plane}`));

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
    const [width, height] = footprintOf(object);
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
  const region = state.region;
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

  // Regions are not contiguous (BOT_WORKSHOP_UX.md §2), so "jump to 3117,9846" may mean loading a
  // different region first. The index knows every region's corner, so say which one owns the tile
  // instead of silently doing nothing.
  const owner = state.index.find((entry) => x >= entry.baseX && x < entry.baseX + 64
    && y >= entry.baseY && y < entry.baseY + 64);
  if (!owner) {
    setStatus(`${x},${y} is not inside any exported region`, true);
    return;
  }
  if (!state.region || state.region.id !== owner.regionId) {
    selectRegion(owner.regionId, { frame: false }).then(() => view.centreOn(x, y));
    return;
  }
  view.centreOn(x, y);
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
  const region = state.region;
  if (!region) {
    return;
  }
  const bankNote = state.banks.length ? '' : '  ·  no bank index';
  setStatus(`region ${region.id}  ·  plane ${state.plane}  ·  `
    + `${view.scale.toFixed(1)} px/tile  ·  ${region.objects.length} objects${bankNote}`);
}

function setStatus(message, bad = false) {
  dom.status.textContent = message;
  dom.status.classList.toggle('bad', bad);
}

function showLoading(visible) {
  dom.loading.hidden = !visible;
}

function footprintOf(object) {
  return collidedSize(object);
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
dom.frameButton.addEventListener('click', () => view.frameRegion());
dom.classResource.addEventListener('change', () => {
  state.classes.resource = dom.classResource.checked;
  pushScene();
});
dom.classService.addEventListener('change', () => {
  state.classes.service = dom.classService.checked;
  pushScene();
});

boot();

// Referenced so the values stay discoverable from the console while the viewer is running.
window.workshop = { state, view, WALK_MASK, PROJECTILE_BIT };
