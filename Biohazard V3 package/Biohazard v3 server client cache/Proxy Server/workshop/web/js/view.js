/**
 * The map canvas: camera, drawing, and the mouse.
 *
 * This layer knows nothing about the DOM chrome or the data files; `app.js` hands it a scene and it
 * draws it. Everything it draws that has a meaning — walkability, the clip tint, footholds — comes
 * from the decoded region and `clip.js`, so the drawing code is presentation only.
 *
 * **The map is continuous, not one region at a time** (`BOT_TOOLING.md` §4 — one top-down map, not
 * a region picker). The scene carries
 * every region that has been loaded plus the world index, and the draw loop walks the regions that
 * overlap the viewport. Nothing special happens at a region border: the camera pans across it and
 * the next region is simply there, or is drawn as "not exported" until `app.js` fetches it.
 *
 * Four decisions worth stating, because they are what make the map readable and correct:
 *
 *   - **North is up.** World y increases north, so a tile's screen top edge is its `y + 1`. Drawing
 *     y downwards would mirror the world vertically against the game's own minimap, and every
 *     direction an author reasons about would be reversed.
 *   - **Only what is on screen is drawn.** A region is 16,384 tiles; at the zooms people actually
 *     work at, a few thousand are visible. Culling is what keeps hovering and panning smooth, which
 *     `BOT_WORKSHOP_UX.md` §10 asks for.
 *   - **Zoomed out, a region is one rectangle.** Below {@link OVERVIEW_MAX_SCALE} the whole world is
 *     on screen at once, and drawing 1226 regions tile by tile would be five million rectangles a
 *     frame. The overview instead shades each region by how much is in it — which is what an author
 *     zoomed out is actually asking ("where is there anything?"), not a pixelated map.
 *   - **A region with no data is drawn as no data.** 51 regions are in the index but ship no map
 *     files. Drawing them blank would say "empty ground"; drawing them hatched says "never exported",
 *     and those are different claims about the world.
 */

import { overlayTone } from './clip.js';
import { SIZE, collidedSize } from './region.js';

/** Below this many pixels per tile, icons collapse into per-area clusters. */
const ICON_MIN_SCALE = 6.5;

/** Clusters are grouped on a screen grid this coarse. */
const CLUSTER_CELL = 54;

/** Tile grid lines only appear once they would not turn the map into a solid hatch. */
const GRID_MIN_SCALE = 7;

/** Above this, regions are drawn tile by tile; below it, as one shaded rectangle. */
export const OVERVIEW_MAX_SCALE = 1.5;

/** Region labels only appear once there is room for one. */
const LABEL_MIN_REGION_PX = 84;

const CLIP_BLOCKED = 'rgba(224, 87, 74, 0.42)';
const CLIP_PROJECTILE = 'rgba(224, 163, 62, 0.26)';
const TILES_OFF_FILL = '#1c222c';
const SELECT_COLOUR = '#6ea8fe';
const HOVER_COLOUR = '#ffffff';

/** A live bot (T7). Deliberately not one of the location colours: it is a different kind of thing. */
const LIVE_COLOUR = '#7ee787';

/** Cases: a region that has data but has not been fetched yet, and one that has none. */
const MISSING_LOADING = 'rgba(110, 168, 254, 0.07)';
const MISSING_NO_DATA = 'rgba(120, 132, 150, 0.13)';
const BORDER = 'rgba(255, 255, 255, 0.10)';
const BORDER_LOADED = 'rgba(110, 168, 254, 0.30)';

/** Per-kind colours for the footprint overlay, keyed by the exporter's kind ids. */
const KIND_COLOUR = {
  tree: 'rgba(94, 178, 94, 0.30)',
  rock: 'rgba(160, 160, 168, 0.30)',
  fishing: 'rgba(94, 156, 214, 0.30)',
  bank: 'rgba(226, 190, 80, 0.34)',
  cooking: 'rgba(224, 120, 70, 0.30)',
  smithing: 'rgba(150, 140, 190, 0.30)',
  prayer: 'rgba(230, 230, 230, 0.28)',
};

/** The outline drawn around an authored place, by kind. Brighter than the footprint fills. */
const LOCATION_COLOUR = {
  bank: 'rgba(226, 190, 80, 0.95)',
  tree: 'rgba(110, 220, 110, 0.95)',
  rock: 'rgba(190, 190, 200, 0.95)',
  fishing: 'rgba(110, 180, 240, 0.95)',
  cooking: 'rgba(240, 140, 90, 0.95)',
  smithing: 'rgba(175, 160, 220, 0.95)',
  prayer: 'rgba(245, 245, 245, 0.95)',
  shop: 'rgba(240, 200, 120, 0.95)',
  teleport: 'rgba(200, 130, 240, 0.95)',
  monster: 'rgba(235, 110, 110, 0.95)',
  master: 'rgba(140, 235, 220, 0.95)',
};

export class MapView {
  /**
   * @param {HTMLCanvasElement} canvas
   * @param {{onHover: Function, onSelect: Function, onViewChanged: Function,
   *          onBox?: Function}} callbacks
   */
  constructor(canvas, callbacks = {}) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    this.callbacks = callbacks;

    // Camera: the world tile at the centre of the viewport, and pixels per tile.
    this.cx = 3200;
    this.cy = 3200;
    this.scale = 8;

    this.w = 0;
    this.h = 0;
    this.pixelRatio = 1;

    this.scene = {
      regions: new Map(),
      world: null,
      plane: 0,
      palette: null,
      layers: { tiles: true, clipping: true, icons: true, footprints: false, grid: false },
      locations: [],
      draft: null,
      live: [],
      iconFor: () => null,
      isVisible: () => true,
    };

    /** `pan` moves the map; `box` drags out a rectangle for the authoring tool. */
    this.mode = 'pan';

    this.hover = null;
    this.selected = null;

    this.dragging = null;
    this.dragDistance = 0;
    /** The in-progress authoring rectangle, while the mouse is down in `box` mode. */
    this.box = null;

    this.onResize = () => this.resize();
    window.addEventListener('resize', this.onResize);

    this.attachPointer();
    this.resize();
  }

  /** Replaces the scene and redraws. */
  setScene(scene) {
    this.scene = { ...this.scene, ...scene };
    this.draw();
  }

  /** Selects a tile, or clears the selection with `null`, and redraws the marker. */
  setSelection(tile) {
    this.selected = tile;
    this.draw();
  }

  setLayers(layers) {
    this.scene.layers = { ...this.scene.layers, ...layers };
    this.draw();
  }

  setMode(mode) {
    this.mode = mode;
    this.box = null;
    this.canvas.classList.toggle('boxing', mode === 'box');
  }

  /** Matches the backing store to the element's CSS size and the device pixel ratio. */
  resize() {
    const rect = this.canvas.getBoundingClientRect();
    // Capped at 2: a 3x backing store on a large window is a lot of pixels for a tile map that is
    // already chromatically blunt, and the frame time is better spent on smooth panning.
    this.pixelRatio = Math.min(window.devicePixelRatio || 1, 2);
    this.w = Math.max(1, Math.round(rect.width));
    this.h = Math.max(1, Math.round(rect.height));
    this.canvas.width = Math.round(this.w * this.pixelRatio);
    this.canvas.height = Math.round(this.h * this.pixelRatio);
    this.draw();
  }

  // ---- camera ---------------------------------------------------------------------------

  worldToScreenX(x) {
    return (x - this.cx) * this.scale + this.w / 2;
  }

  /** North is up: a larger world y is further up the screen. */
  worldToScreenY(y) {
    return (this.cy - y) * this.scale + this.h / 2;
  }

  screenToWorldX(px) {
    return this.cx + (px - this.w / 2) / this.scale;
  }

  screenToWorldY(py) {
    return this.cy - (py - this.h / 2) / this.scale;
  }

  /** The world rectangle the viewport covers, rounded outward to whole tiles. */
  visibleWindow() {
    const halfW = this.w / 2 / this.scale;
    const halfH = this.h / 2 / this.scale;
    return {
      minX: Math.floor(this.cx - halfW),
      maxX: Math.ceil(this.cx + halfW),
      minY: Math.floor(this.cy - halfH),
      maxY: Math.ceil(this.cy + halfH),
    };
  }

  /** The region entry at an absolute tile, whether or not its document is loaded. */
  entryAt(x, y) {
    return this.scene.world ? this.scene.world.regionAt(x, y) : null;
  }

  /** The loaded region at an absolute tile, or null when there is none. */
  regionAt(x, y) {
    const entry = this.entryAt(x, y);
    return entry ? (this.scene.regions.get(entry.regionId) || null) : null;
  }

  /** The tile under a CSS-pixel position, or null when no region lives there. */
  tileAtScreen(px, py) {
    const x = Math.floor(this.screenToWorldX(px));
    const y = Math.floor(this.screenToWorldY(py));
    if (!this.entryAt(x, y)) {
      return null;
    }
    return { x, y, plane: this.scene.plane };
  }

  /** Zooms so the world point under (px, py) stays under it. */
  zoomAt(px, py, factor) {
    const before = this.screenToWorldX(px);
    const beforeY = this.screenToWorldY(py);
    this.scale = clamp(this.scale * factor, 0.05, 64);
    // Put the same world point back under the cursor after the scale change.
    this.cx += before - this.screenToWorldX(px);
    this.cy += beforeY - this.screenToWorldY(py);
    this.draw();
    this.viewChanged();
  }

  /** Fits the region under the camera in the viewport, or the whole world if there is none. */
  frameRegion() {
    const entry = this.entryAt(Math.round(this.cx), Math.round(this.cy));
    if (!entry) {
      this.frameWorld();
      return;
    }
    this.cx = entry.baseX + SIZE / 2;
    this.cy = entry.baseY + SIZE / 2;
    this.scale = clamp(Math.min(this.w / SIZE, this.h / SIZE) * 0.95, 0.05, 64);
    this.draw();
    this.viewChanged();
  }

  /** Fits every region the index knows about — the whole-world overview. */
  frameWorld() {
    const world = this.scene.world;
    if (!world || world.entries.length === 0) {
      return;
    }
    const bounds = world.bounds;
    this.cx = bounds.minX + bounds.width / 2;
    this.cy = bounds.minY + bounds.height / 2;
    this.scale = clamp(Math.min(this.w / bounds.width, this.h / bounds.height) * 0.92, 0.05, 64);
    this.draw();
    this.viewChanged();
  }

  /** Frames one region by id, loaded or not. */
  frameById(regionId) {
    const entry = this.scene.world ? this.scene.world.byRegionId(regionId) : null;
    if (!entry) {
      return;
    }
    this.cx = entry.baseX + SIZE / 2;
    this.cy = entry.baseY + SIZE / 2;
    this.scale = clamp(Math.min(this.w / SIZE, this.h / SIZE) * 0.95, 0.05, 64);
    this.draw();
    this.viewChanged();
  }

  /** Centres on an absolute tile, keeping the current zoom. */
  centreOn(x, y) {
    this.cx = x;
    this.cy = y;
    this.draw();
    this.viewChanged();
  }

  viewChanged() {
    if (this.callbacks.onViewChanged) {
      this.callbacks.onViewChanged({
        scale: this.scale,
        centreX: Math.round(this.cx),
        centreY: Math.round(this.cy),
      });
    }
  }

  // ---- input ----------------------------------------------------------------------------

  attachPointer() {
    const canvas = this.canvas;

    canvas.addEventListener('pointerdown', (event) => {
      if (event.button !== 0 && event.button !== 1) {
        return;
      }
      if (this.mode === 'box' && event.button === 0) {
        canvas.setPointerCapture(event.pointerId);
        this.box = { x0: Math.floor(this.screenToWorldX(event.offsetX)),
          y0: Math.floor(this.screenToWorldY(event.offsetY)),
          x1: Math.floor(this.screenToWorldX(event.offsetX)),
          y1: Math.floor(this.screenToWorldY(event.offsetY)) };
        this.hideTooltip();
        this.draw();
        return;
      }
      canvas.setPointerCapture(event.pointerId);
      this.dragging = { x: event.offsetX, y: event.offsetY, cx: this.cx, cy: this.cy };
      this.dragDistance = 0;
      canvas.classList.add('dragging');
    });

    canvas.addEventListener('pointermove', (event) => {
      if (this.box) {
        this.box.x1 = Math.floor(this.screenToWorldX(event.offsetX));
        this.box.y1 = Math.floor(this.screenToWorldY(event.offsetY));
        this.draw();
        return;
      }
      if (this.dragging) {
        const dx = event.offsetX - this.dragging.x;
        const dy = event.offsetY - this.dragging.y;
        this.dragDistance = Math.max(this.dragDistance, Math.abs(dx) + Math.abs(dy));
        // Dragging moves the world with the cursor; y is inverted because north is up.
        this.cx = this.dragging.cx - dx / this.scale;
        this.cy = this.dragging.cy + dy / this.scale;
        this.hideTooltip();
        this.draw();
        this.viewChanged();
        return;
      }
      this.updateHover(event.offsetX, event.offsetY);
    });

    const endDrag = (event) => {
      if (this.box) {
        const box = this.box;
        this.box = null;
        if (canvas.hasPointerCapture(event.pointerId)) {
          canvas.releasePointerCapture(event.pointerId);
        }
        this.draw();
        if (this.callbacks.onBox) {
          this.callbacks.onBox(box);
        }
        return;
      }
      if (!this.dragging) {
        return;
      }
      const wasDrag = this.dragDistance > 3;
      this.dragging = null;
      canvas.classList.remove('dragging');
      if (canvas.hasPointerCapture(event.pointerId)) {
        canvas.releasePointerCapture(event.pointerId);
      }
      // A press that did not move is a click, and a click selects. Treating every press as a
      // selection would make panning also move the inspector, which is maddening.
      if (!wasDrag) {
        const tile = this.tileAtScreen(event.offsetX, event.offsetY);
        this.selected = tile;
        this.draw();
        if (this.callbacks.onSelect) {
          this.callbacks.onSelect(tile);
        }
      }
    };

    canvas.addEventListener('pointerup', endDrag);
    canvas.addEventListener('pointercancel', endDrag);

    canvas.addEventListener('pointerleave', () => {
      this.hover = null;
      this.hideTooltip();
      this.draw();
    });

    canvas.addEventListener('wheel', (event) => {
      event.preventDefault();
      this.zoomAt(event.offsetX, event.offsetY, event.deltaY < 0 ? 1.2 : 1 / 1.2);
    }, { passive: false });

    canvas.addEventListener('contextmenu', (event) => event.preventDefault());
  }

  updateHover(px, py) {
    const tile = this.tileAtScreen(px, py);
    const same = tile && this.hover && tile.x === this.hover.x && tile.y === this.hover.y;
    this.hover = tile;
    if (!same) {
      this.draw();
      if (this.callbacks.onHover) {
        this.callbacks.onHover(tile, px, py);
      }
    }
    this.moveTooltip(px, py);
  }

  // ---- tooltip (owned here because it is positioned in canvas space) ---------------------

  setTooltipElement(element) {
    this.tooltip = element;
  }

  moveTooltip(px, py) {
    if (!this.tooltip || this.tooltip.hidden) {
      return;
    }
    // Flip to the other side of the cursor near the edges so the tooltip is never clipped.
    const rect = this.tooltip.getBoundingClientRect();
    const flipX = px + rect.width + 24 > this.w;
    const flipY = py + rect.height + 24 > this.h;
    this.tooltip.style.left = `${flipX ? Math.max(4, px - rect.width - 14) : px + 14}px`;
    this.tooltip.style.top = `${flipY ? Math.max(4, py - rect.height - 14) : py + 14}px`;
  }

  hideTooltip() {
    if (this.tooltip) {
      this.tooltip.hidden = true;
    }
  }

  // ---- drawing --------------------------------------------------------------------------

  draw() {
    const ctx = this.ctx;
    ctx.setTransform(this.pixelRatio, 0, 0, this.pixelRatio, 0, 0);
    ctx.fillStyle = '#0c0f13';
    ctx.fillRect(0, 0, this.w, this.h);

    const { world } = this.scene;
    if (!world || world.entries.length === 0) {
      return;
    }

    const window = this.visibleWindow();
    const entries = world.overlapping(window.minX, window.maxX, window.minY, window.maxY);
    if (entries.length === 0) {
      return;
    }

    if (this.scale < OVERVIEW_MAX_SCALE) {
      this.drawOverview(entries);
    } else {
      this.drawRegions(entries, window);
    }

    this.drawBorders(entries);
    this.drawLocations();
    this.drawLive();
    this.drawDraft();
    this.drawMarker(this.hover, HOVER_COLOUR, 1.5);
    this.drawMarker(this.selected, SELECT_COLOUR, 2);
  }

  /**
   * One rectangle per region, shaded by how much is in it.
   *
   * This is the whole-world view. Per-tile drawing at this zoom would be millions of rectangles a
   * frame, and the answer to "what is out there" is a density, not a colour per tile.
   */
  drawOverview(entries) {
    const ctx = this.ctx;
    const side = SIZE * this.scale;
    for (const entry of entries) {
      const left = this.worldToScreenX(entry.baseX);
      const top = this.worldToScreenY(entry.baseY + SIZE);
      if (!entry.exported) {
        ctx.fillStyle = MISSING_NO_DATA;
        ctx.fillRect(left, top, side, side);
        continue;
      }
      ctx.fillStyle = densityColour(entry.objects);
      ctx.fillRect(left, top, side, side);
      if (entry.banks > 0) {
        // A bank is the anchor of most bot trees, so it is worth one bright dot in a grey world.
        ctx.fillStyle = 'rgba(240, 200, 90, 0.95)';
        ctx.beginPath();
        ctx.arc(left + side / 2, top + side / 2, Math.max(1.5, side * 0.09), 0, Math.PI * 2);
        ctx.fill();
      }
    }
  }

  /** Tile-by-tile drawing for the regions that overlap the window. */
  drawRegions(entries, window) {
    const { regions, plane, palette, layers } = this.scene;
    const loaded = [];
    for (const entry of entries) {
      const region = regions.get(entry.regionId);
      if (!region) {
        this.drawMissingRegion(entry);
        continue;
      }
      loaded.push(region);
      this.drawRegionTiles(region, window);
    }
    if (loaded.length === 0) {
      return;
    }

    if (layers.grid && this.scale >= GRID_MIN_SCALE) {
      for (const region of loaded) {
        this.drawGrid(region, window);
      }
    }
    if (layers.footprints) {
      this.drawFootprints(loaded, plane);
    }
    if (layers.icons) {
      this.drawIcons(loaded, plane);
    }
  }

  /** A region the export wrote but the viewer has not fetched yet, or one the export skipped. */
  drawMissingRegion(entry) {
    const ctx = this.ctx;
    const side = SIZE * this.scale;
    const left = this.worldToScreenX(entry.baseX);
    const top = this.worldToScreenY(entry.baseY + SIZE);
    if (left > this.w || top > this.h || left + side < 0 || top + side < 0) {
      return;
    }
    ctx.fillStyle = entry.exported ? MISSING_LOADING : MISSING_NO_DATA;
    ctx.fillRect(left, top, side, side);
    if (side >= LABEL_MIN_REGION_PX) {
      ctx.save();
      ctx.fillStyle = 'rgba(200,210,225,0.55)';
      ctx.font = '11px "Segoe UI", system-ui, sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(entry.exported ? `region ${entry.regionId} — loading` : 'not exported',
        left + side / 2, top + side / 2);
      ctx.restore();
    }
  }

  drawRegionTiles(region, window) {
    const ctx = this.ctx;
    const { plane, palette, layers } = this.scene;
    const data = region.planes[plane];
    const s = this.scale;

    // The window intersected with the region: tiles outside either are simply not drawn.
    const minX = Math.max(region.baseX, window.minX);
    const maxX = Math.min(region.baseX + SIZE - 1, window.maxX);
    const minY = Math.max(region.baseY, window.minY);
    const maxY = Math.min(region.baseY + SIZE - 1, window.maxY);
    if (minX > maxX || minY > maxY) {
      return;
    }

    // Half a pixel of overlap: at a fractional scale, exact rectangles leave hairline seams
    // between tiles, which read as a grid that is not really there.
    const bleed = 0.5;
    for (let ty = minY; ty <= maxY; ty++) {
      const top = this.worldToScreenY(ty + 1) - bleed;
      const height = s + bleed * 2;
      for (let tx = minX; tx <= maxX; tx++) {
        const at = (tx - region.baseX) * SIZE + (ty - region.baseY);
        const left = this.worldToScreenX(tx) - bleed;
        const width = s + bleed * 2;

        if (layers.tiles && palette && data) {
          const under = palette.colour('underlay', data.underlay[at]);
          if (under) {
            ctx.fillStyle = under;
            ctx.fillRect(left, top, width, height);
          }
          const over = palette.colour('overlay', data.overlay[at]);
          if (over) {
            ctx.fillStyle = over;
            ctx.fillRect(left, top, width, height);
          }
        } else if (!layers.tiles) {
          // No floor of either kind is genuinely empty — off the edge of the world — so with the
          // tile layer off the extent is shown flat, which still says where the data ends.
          ctx.fillStyle = TILES_OFF_FILL;
          ctx.fillRect(left, top, width, height);
        }

        if (layers.clipping && data) {
          const tone = overlayTone(data.clip[at]);
          if (tone) {
            ctx.fillStyle = tone === 'blocked' ? CLIP_BLOCKED : CLIP_PROJECTILE;
            ctx.fillRect(left, top, width, height);
          }
        }
      }
    }
  }

  /** Every region's outline, so the edge of the data is never mistaken for the edge of the world. */
  drawBorders(entries) {
    const ctx = this.ctx;
    const side = SIZE * this.scale;
    if (side < 2) {
      return;
    }
    ctx.save();
    ctx.lineWidth = 1;
    for (const entry of entries) {
      const left = this.worldToScreenX(entry.baseX);
      const top = this.worldToScreenY(entry.baseY + SIZE);
      if (left > this.w || top > this.h || left + side < 0 || top + side < 0) {
        continue;
      }
      ctx.strokeStyle = this.scene.regions.has(entry.regionId) ? BORDER_LOADED : BORDER;
      ctx.strokeRect(left + 0.5, top + 0.5, side - 1, side - 1);
    }
    ctx.restore();
  }

  drawGrid(region, window) {
    const ctx = this.ctx;
    const minX = Math.max(region.baseX, window.minX);
    const maxX = Math.min(region.baseX + SIZE - 1, window.maxX);
    const minY = Math.max(region.baseY, window.minY);
    const maxY = Math.min(region.baseY + SIZE - 1, window.maxY);
    if (minX > maxX || minY > maxY) {
      return;
    }
    const side = SIZE * this.scale;
    ctx.save();
    // Clipped to the region, so the lines of one region do not spill into its neighbour — which at
    // this zoom would look like a grid that is misaligned by a tile rather than two grids meeting.
    ctx.beginPath();
    ctx.rect(this.worldToScreenX(region.baseX), this.worldToScreenY(region.baseY + SIZE), side, side);
    ctx.clip();
    ctx.lineWidth = 1;
    ctx.strokeStyle = 'rgba(255,255,255,0.07)';
    ctx.beginPath();
    for (let tx = minX; tx <= maxX + 1; tx++) {
      const px = Math.round(this.worldToScreenX(tx)) + 0.5;
      ctx.moveTo(px, this.worldToScreenY(maxY + 1));
      ctx.lineTo(px, this.worldToScreenY(minY));
    }
    for (let ty = minY; ty <= maxY + 1; ty++) {
      const py = Math.round(this.worldToScreenY(ty)) + 0.5;
      ctx.moveTo(this.worldToScreenX(minX), py);
      ctx.lineTo(this.worldToScreenX(maxX + 1), py);
    }
    ctx.stroke();
    ctx.restore();
  }

  drawFootprints(loaded, plane) {
    const ctx = this.ctx;
    ctx.save();
    for (const region of loaded) {
      for (const object of region.objects) {
        if (object.plane !== plane || !this.scene.isVisible(object)) {
          continue;
        }
        const [width, height] = collidedSize(object);
        const left = this.worldToScreenX(object.x);
        const top = this.worldToScreenY(object.y + height);
        const w = width * this.scale;
        const h = height * this.scale;
        if (left > this.w || top > this.h || left + w < 0 || top + h < 0) {
          continue;
        }
        ctx.fillStyle = KIND_COLOUR[object.kind] || 'rgba(150,150,150,0.22)';
        ctx.fillRect(left, top, w, h);
        ctx.strokeStyle = 'rgba(0,0,0,0.35)';
        ctx.lineWidth = 1;
        ctx.strokeRect(left + 0.5, top + 0.5, w - 1, h - 1);
      }
    }
    ctx.restore();
  }

  /**
   * Icons, or clusters of them when zoomed out.
   *
   * A forest at low zoom is a green smear if every tree is drawn, which is why `BOT_WORKSHOP_UX.md`
   * §3 asks for clustering. The cluster is bucketed in screen space so it stays stable while
   * panning. Clustering spans regions on purpose: a forest that happens to cross a region border is
   * one forest, and drawing two badges for it would be reporting the export, not the world.
   */
  drawIcons(loaded, plane) {
    const ctx = this.ctx;
    const s = this.scale;
    const visible = [];
    for (const region of loaded) {
      for (const object of region.objects) {
        if (object.plane === plane && this.scene.isVisible(object)) {
          visible.push(object);
        }
      }
    }
    if (visible.length === 0) {
      return;
    }

    if (s < ICON_MIN_SCALE) {
      const cells = new Map();
      for (const object of visible) {
        const px = this.worldToScreenX(object.x + 0.5);
        const py = this.worldToScreenY(object.y + 0.5);
        if (px < -CLUSTER_CELL || py < -CLUSTER_CELL || px > this.w + CLUSTER_CELL || py > this.h + CLUSTER_CELL) {
          continue;
        }
        const key = `${Math.floor(px / CLUSTER_CELL)}:${Math.floor(py / CLUSTER_CELL)}:${object.kind}`;
        const cell = cells.get(key);
        if (cell) {
          cell.count++;
          cell.sumX += px;
          cell.sumY += py;
        } else {
          cells.set(key, { kind: object.kind, count: 1, sumX: px, sumY: py });
        }
      }
      ctx.save();
      ctx.font = '600 11px "Segoe UI", system-ui, sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      for (const cell of cells.values()) {
        // The badge sits at the mean of the icons it stands for, so a cluster over a forest edge
        // lands on the trees rather than on the empty cell centre.
        const x = cell.sumX / cell.count;
        const y = cell.sumY / cell.count;
        ctx.beginPath();
        ctx.arc(x, y, 13, 0, Math.PI * 2);
        ctx.fillStyle = KIND_COLOUR[cell.kind] || 'rgba(150,150,150,0.35)';
        ctx.fill();
        ctx.strokeStyle = 'rgba(255,255,255,0.35)';
        ctx.lineWidth = 1;
        ctx.stroke();
        const glyph = this.scene.iconFor(cell.kind) || '';
        ctx.fillStyle = '#0d1117';
        ctx.fillText(`${glyph}${cell.count}`, x, y);
      }
      ctx.restore();
      return;
    }

    ctx.save();
    ctx.font = `${Math.round(Math.min(s * 1.15, 22))}px "Segoe UI Emoji", "Segoe UI", system-ui, sans-serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    for (const object of visible) {
      const glyph = this.scene.iconFor(object.kind);
      if (!glyph) {
        continue;
      }
      const px = this.worldToScreenX(object.x + 0.5);
      const py = this.worldToScreenY(object.y + 0.5);
      if (px < -20 || py < -20 || px > this.w + 20 || py > this.h + 20) {
        continue;
      }
      ctx.fillStyle = 'rgba(0,0,0,0.45)';
      ctx.beginPath();
      ctx.arc(px + 1, py + 1, s * 0.46, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillText(glyph, px, py);
    }
    ctx.restore();
  }

  /** The authored `locations.cfg` boxes on the current plane — the places a bot can be sent to. */
  drawLocations() {
    const places = this.scene.locations;
    if (!places || places.length === 0) {
      return;
    }
    const ctx = this.ctx;
    const plane = this.scene.plane;
    const s = this.scale;
    ctx.save();
    for (const place of places) {
      if (place.plane !== plane) {
        continue;
      }
      const w = place.w * s;
      const h = place.h * s;
      const left = this.worldToScreenX(place.x);
      const top = this.worldToScreenY(place.y + place.h);
      if (left > this.w || top > this.h || left + w < 0 || top + h < 0) {
        continue;
      }
      ctx.strokeStyle = LOCATION_COLOUR[place.kind] || 'rgba(255,255,255,0.8)';
      ctx.lineWidth = place.point ? 2 : 1.5;
      // Inset by half a line so the box outlines the tiles it covers instead of straddling them.
      ctx.strokeRect(left + 1, top + 1, Math.max(w - 2, 2), Math.max(h - 2, 2));
      if (s > 3.5) {
        ctx.fillStyle = ctx.strokeStyle;
        ctx.font = '11px "Segoe UI", system-ui, sans-serif';
        ctx.textAlign = 'left';
        ctx.textBaseline = 'bottom';
        ctx.fillText(`${place.name} (${place.kind})`, left + 2, top - 2);
      }
    }
    ctx.restore();
  }

  /**
   * The live bots, on the plane being looked at (T7). One marker per bot, named.
   *
   * Only the current plane is drawn, for the same reason the authored places are: a marker on a tile
   * that is not the one under the camera would be a lie about where the bot is. A bot on another plane
   * is named as such in the panel instead, which is where "which plane is it on" can actually be said.
   *
   * The marker is drawn as a ring plus a dot rather than as the tile outline `drawMarker` uses: a bot is
   * standing *in* a tile, not *on* it, and a ring reads as a point in a way a square does not. The tile
   * is still outlined underneath so the position is exact when zoomed in.
   */
  drawLive() {
    const bots = this.scene.live;
    if (!bots || bots.length === 0) {
      return;
    }
    const ctx = this.ctx;
    const plane = this.scene.plane;
    const s = this.scale;
    ctx.save();
    for (const bot of bots) {
      if (bot.plane !== plane) {
        continue;
      }
      const left = this.worldToScreenX(bot.x);
      const top = this.worldToScreenY(bot.y + 1);
      if (left > this.w || top > this.h || left + s < 0 || top + s < 0) {
        continue;
      }
      ctx.strokeStyle = LIVE_COLOUR;
      ctx.lineWidth = 1;
      ctx.strokeRect(left + 0.5, top + 0.5, s, s);
      const cx = left + s / 2;
      const cy = top + s / 2;
      ctx.beginPath();
      ctx.arc(cx, cy, Math.max(s * 0.28, 3), 0, Math.PI * 2);
      ctx.fillStyle = 'rgba(126,231,135,0.35)';
      ctx.fill();
      ctx.stroke();
      if (s > 3) {
        ctx.font = '11px "Segoe UI", system-ui, sans-serif';
        ctx.textAlign = 'left';
        ctx.textBaseline = 'bottom';
        // A dark halo behind the text, because a bot standing on a tree draws a label over a glyph.
        ctx.lineWidth = 3;
        ctx.strokeStyle = 'rgba(13,17,23,0.85)';
        ctx.strokeText(bot.name, left + 2, top - 2);
        ctx.fillStyle = LIVE_COLOUR;
        ctx.fillText(bot.name, left + 2, top - 2);
      }
    }
    ctx.restore();
  }

  /** The rectangle the author is dragging out right now, before it becomes a row. */
  drawDraft() {
    const box = this.scene.draft;
    if (!box) {
      return;
    }
    const ctx = this.ctx;
    const minX = Math.min(box.x0, box.x1);
    const maxX = Math.max(box.x0, box.x1);
    const minY = Math.min(box.y0, box.y1);
    const maxY = Math.max(box.y0, box.y1);
    const left = this.worldToScreenX(minX);
    const top = this.worldToScreenY(maxY + 1);
    const w = (maxX - minX + 1) * this.scale;
    const h = (maxY - minY + 1) * this.scale;
    ctx.save();
    ctx.setLineDash([5, 4]);
    ctx.strokeStyle = '#6ea8fe';
    ctx.lineWidth = 2;
    ctx.strokeRect(left, top, w, h);
    ctx.setLineDash([]);
    ctx.fillStyle = 'rgba(110,168,254,0.14)';
    ctx.fillRect(left, top, w, h);
    ctx.restore();
  }

  drawMarker(tile, colour, lineWidth) {
    if (!tile) {
      return;
    }
    const ctx = this.ctx;
    ctx.save();
    ctx.strokeStyle = colour;
    ctx.lineWidth = lineWidth;
    const size = Math.max(this.scale, 6);
    ctx.strokeRect(
      this.worldToScreenX(tile.x) + 0.5,
      this.worldToScreenY(tile.y + 1) + 0.5,
      size,
      size,
    );
    ctx.restore();
  }

  destroy() {
    window.removeEventListener('resize', this.onResize);
  }
}

/**
 * A shade for the overview, from the region's object count.
 *
 * Linear on a log scale: the counts run from a handful to a few thousand, and a linear ramp would
 * make every quiet region indistinguishable from an empty one.
 */
function densityColour(objects) {
  if (!objects) {
    return 'rgba(70, 80, 95, 0.35)';
  }
  const t = Math.min(1, Math.log10(objects + 1) / Math.log10(4000));
  const r = Math.round(30 + 40 * t);
  const g = Math.round(55 + 95 * t);
  const b = Math.round(80 + 110 * t);
  return `rgba(${r}, ${g}, ${b}, 0.55)`;
}

function clamp(value, low, high) {
  return Math.max(low, Math.min(high, value));
}
