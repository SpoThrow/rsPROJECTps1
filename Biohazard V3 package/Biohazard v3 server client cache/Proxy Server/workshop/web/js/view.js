/**
 * The map canvas: camera, drawing, and the mouse.
 *
 * This layer knows nothing about the DOM chrome or the data files; `app.js` hands it a scene and it
 * draws it. Everything it draws that has a meaning — walkability, the clip tint, footholds —
 * comes from the decoded region and `clip.js`, so the drawing code is presentation only.
 *
 * Two decisions worth stating, because they are what make the map readable and correct:
 *
 *   - **North is up.** World y increases north, so a tile's screen top edge is its `y + 1`. Drawing
 *     y downwards would mirror the world vertically against the game's own minimap, and every
 *     direction an author reasons about would be reversed.
 *   - **Only what is on screen is drawn.** A region is 16,384 tiles; at the zooms people actually
 *     work at, a few thousand are visible. Culling is what keeps hovering and panning smooth on a
 *     full region, which `BOT_WORKSHOP_UX.md` §10 asks for.
 */

import { overlayTone } from './clip.js';
import { SIZE, collidedSize } from './region.js';

/** Below this many pixels per tile, icons collapse into per-area clusters. */
const ICON_MIN_SCALE = 6.5;

/** Clusters are grouped on a screen grid this coarse. */
const CLUSTER_CELL = 54;

/** Tile grid lines only appear once they would not turn the map into a solid hatch. */
const GRID_MIN_SCALE = 7;

const CLIP_BLOCKED = 'rgba(224, 87, 74, 0.42)';
const CLIP_PROJECTILE = 'rgba(224, 163, 62, 0.26)';
const TILES_OFF_FILL = '#1c222c';
const SELECT_COLOUR = '#6ea8fe';
const HOVER_COLOUR = '#ffffff';

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

export class MapView {
  /**
   * @param {HTMLCanvasElement} canvas
   * @param {{onHover: Function, onSelect: Function, onViewChanged: Function}} callbacks
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
      region: null,
      plane: 0,
      palette: null,
      layers: { tiles: true, clipping: true, icons: true, footprints: false, grid: false },
      iconFor: () => null,
      isVisible: () => true,
    };

    this.hover = null;
    this.selected = null;

    this.dragging = null;
    this.dragDistance = 0;

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

  /** The tile under a CSS-pixel position, or null when it falls outside the region. */
  tileAtScreen(px, py) {
    const region = this.scene.region;
    const x = Math.floor(this.screenToWorldX(px));
    const y = Math.floor(this.screenToWorldY(py));
    if (!region || !region.contains(x, y)) {
      return null;
    }
    return { x, y, plane: this.scene.plane };
  }

  /** Zooms so the world point under (px, py) stays under it. */
  zoomAt(px, py, factor) {
    const before = this.screenToWorldX(px);
    const beforeY = this.screenToWorldY(py);
    this.scale = clamp(this.scale * factor, 0.5, 64);
    // Put the same world point back under the cursor after the scale change.
    this.cx += before - this.screenToWorldX(px);
    this.cy += beforeY - this.screenToWorldY(py);
    this.draw();
    this.viewChanged();
  }

  /** Fits the loaded region in the viewport with a small margin. */
  frameRegion() {
    const region = this.scene.region;
    if (!region) {
      return;
    }
    this.cx = region.baseX + SIZE / 2;
    this.cy = region.baseY + SIZE / 2;
    this.scale = clamp(Math.min(this.w / SIZE, this.h / SIZE) * 0.95, 0.5, 64);
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
      canvas.setPointerCapture(event.pointerId);
      this.dragging = { x: event.offsetX, y: event.offsetY, cx: this.cx, cy: this.cy };
      this.dragDistance = 0;
      canvas.classList.add('dragging');
    });

    canvas.addEventListener('pointermove', (event) => {
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
    const { region, plane, palette, layers } = this.scene;
    const s = this.scale;

    ctx.setTransform(this.pixelRatio, 0, 0, this.pixelRatio, 0, 0);
    ctx.fillStyle = '#0c0f13';
    ctx.fillRect(0, 0, this.w, this.h);

    if (!region) {
      return;
    }
    const data = region.planes[plane];

    // Visible tile window, clamped to the region. Tiles outside the region are simply not drawn.
    const minX = Math.max(region.baseX, Math.floor(this.screenToWorldX(0)));
    const maxX = Math.min(region.baseX + SIZE - 1, Math.ceil(this.screenToWorldX(this.w)));
    const minY = Math.max(region.baseY, Math.floor(this.screenToWorldY(this.h)));
    const maxY = Math.min(region.baseY + SIZE - 1, Math.ceil(this.screenToWorldY(0)));

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

        if (layers.tiles && palette) {
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

        if (layers.clipping) {
          const tone = overlayTone(data.clip[at]);
          if (tone) {
            ctx.fillStyle = tone === 'blocked' ? CLIP_BLOCKED : CLIP_PROJECTILE;
            ctx.fillRect(left, top, width, height);
          }
        }
      }
    }

    if (layers.grid && s >= GRID_MIN_SCALE) {
      this.drawGrid(region, minX, maxX, minY, maxY);
    }

    if (layers.footprints) {
      this.drawFootprints(region, plane);
    }

    if (layers.icons) {
      this.drawIcons(region, plane);
    }

    this.drawMarker(this.hover, HOVER_COLOUR, 1.5);
    this.drawMarker(this.selected, SELECT_COLOUR, 2);
  }

  drawGrid(region, minX, maxX, minY, maxY) {
    const ctx = this.ctx;
    ctx.save();
    ctx.lineWidth = 1;
    ctx.strokeStyle = 'rgba(255,255,255,0.07)';
    ctx.beginPath();
    for (let tx = minX; tx <= maxX + 1; tx++) {
      const px = Math.round(this.worldToScreenX(tx)) + 0.5;
      const top = this.worldToScreenY(maxY + 1);
      const bottom = this.worldToScreenY(minY);
      ctx.moveTo(px, top);
      ctx.lineTo(px, bottom);
    }
    for (let ty = minY; ty <= maxY + 1; ty++) {
      const py = Math.round(this.worldToScreenY(ty)) + 0.5;
      const left = this.worldToScreenX(minX);
      const right = this.worldToScreenX(maxX + 1);
      ctx.moveTo(left, py);
      ctx.lineTo(right, py);
    }
    ctx.stroke();

    // The region border, so the edge of the loaded data is never mistaken for the edge of the world.
    ctx.strokeStyle = 'rgba(110,168,254,0.55)';
    ctx.lineWidth = 2;
    ctx.strokeRect(
      this.worldToScreenX(region.baseX),
      this.worldToScreenY(region.baseY + SIZE),
      SIZE * this.scale,
      SIZE * this.scale,
    );
    ctx.restore();
  }

  drawFootprints(region, plane) {
    const ctx = this.ctx;
    ctx.save();
    for (const object of region.objects) {
      if (object.plane !== plane || !this.scene.isVisible(object)) {
        continue;
      }
      const [width, height] = footprint(object);
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
    ctx.restore();
  }

  /**
   * Icons, or clusters of them when zoomed out.
   *
   * A forest at low zoom is a green smear if every tree is drawn, which is why `BOT_WORKSHOP_UX.md`
   * §3 asks for clustering. The cluster is bucketed in screen space so it stays stable while
   * panning.
   */
  drawIcons(region, plane) {
    const ctx = this.ctx;
    const s = this.scale;
    const visible = region.objectsOn(plane).filter((object) => this.scene.isVisible(object));

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

/** The footprint to draw — the same pair the server collides with. */
function footprint(object) {
  return collidedSize(object);
}

function clamp(value, low, high) {
  return Math.max(low, Math.min(high, value));
}
