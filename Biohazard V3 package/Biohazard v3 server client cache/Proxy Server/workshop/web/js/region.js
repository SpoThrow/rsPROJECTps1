/**
 * One exported region, decoded and queryable.
 *
 * The document is what `workshopExport` wrote (`botworkshop.export.RegionDocument`), and nothing
 * here re-derives a value the exporter already worked out. The one piece of arithmetic it does do —
 * the rotation swap on an object's footprint — mirrors `Region.addObject`, which is where the server
 * applies it, so the box drawn and the tiles collided are the same box.
 */

import { decode } from './rle.js';

/** Tiles per side of a region. */
export const SIZE = 64;

/** Planes per region; 0 is ground level. */
export const PLANES = 4;

/** Tiles per plane. */
export const TILES = SIZE * SIZE;

/**
 * The footprint the server collides with, as `[width, height]` in tiles.
 *
 * `Region.addObject` swaps the pair when `direction` is 1 or 3 — after resolving it through the size
 * table — so the box that blocks is the swapped one. Reproducing that swap rather than drawing
 * `sizeX` by `sizeY` is the difference between a footprint overlay that matches the clip grid and
 * one that only matches for the unrotated objects in the world. Shared with the renderer so the two
 * cannot drift.
 */
export function collidedSize(object) {
  if (object.rotation === 1 || object.rotation === 3) {
    return [object.sizeY, object.sizeX];
  }
  return [object.sizeX, object.sizeY];
}

/** One region's tiles and objects. */
export class Region {
  /**
   * @param {object} doc the parsed `<regionId>.json`
   */
  constructor(doc) {
    if (!doc || typeof doc.regionId !== 'number') {
      throw new Error('not a region document');
    }
    this.id = doc.regionId;
    this.baseX = doc.baseX;
    this.baseY = doc.baseY;

    // planes[0..3] by plane number, so indexing does not depend on the file's order.
    this.planes = new Array(PLANES).fill(null);
    for (const raw of doc.planes || []) {
      if (raw.plane < 0 || raw.plane >= PLANES) {
        throw new Error(`plane ${raw.plane} out of range in region ${this.id}`);
      }
      this.planes[raw.plane] = {
        overlay: decode(raw.overlay, TILES),
        underlay: decode(raw.underlay, TILES),
        flags: decode(raw.flags, TILES),
        clip: decode(raw.clip, TILES),
      };
    }
    for (let plane = 0; plane < PLANES; plane++) {
      if (this.planes[plane] === null) {
        throw new Error(`region ${this.id} is missing plane ${plane}`);
      }
    }

    this.objects = (doc.objects || []).map((raw) => ({
      ...raw,
      // Absolute coordinates are the server's unit and what the inspector reports; these are the
      // same numbers the export wrote, kept under clearer names.
      x: raw.x,
      y: raw.y,
      plane: raw.plane,
      kind: raw.kind || null,
      actions: raw.actions || [],
    }));
  }

  /** True when an absolute tile is inside this region. */
  contains(x, y) {
    return x >= this.baseX && x < this.baseX + SIZE && y >= this.baseY && y < this.baseY + SIZE;
  }

  /** Region-local coordinate of an absolute tile; may be outside 0..63. */
  localX(x) {
    return x - this.baseX;
  }

  localY(y) {
    return y - this.baseY;
  }

  /** Index into a plane's arrays, in the exporter's order: localX outermost. */
  static index(localX, localY) {
    return localX * SIZE + localY;
  }

  /**
   * The tiles of one plane at a region-local coordinate, or `null` when the coordinate falls
   * outside the region — which a footprint at the region edge genuinely can — or when the export
   * carries no data for that plane. `planes` is filled with `null` for absent planes, so a missing
   * plane must return `null` rather than be indexed into; callers already guard on `null`.
   */
  tile(plane, localX, localY) {
    if (localX < 0 || localX >= SIZE || localY < 0 || localY >= SIZE) {
      return null;
    }
    const data = this.planes[plane];
    if (!data) {
      return null;
    }
    const at = Region.index(localX, localY);
    return {
      plane,
      overlay: data.overlay[at],
      underlay: data.underlay[at],
      flags: data.flags[at],
      clip: data.clip[at],
    };
  }

  /** The same, by absolute tile. */
  tileAt(plane, x, y) {
    return this.tile(plane, this.localX(x), this.localY(y));
  }

  /**
   * The objects whose collided footprint covers an absolute tile on a plane.
   *
   * Covers the whole footprint, not just the origin tile, because that is what "what is on this
   * tile" means to an author placing a waypoint next to a 2x2 tree. The origin tile is reported
   * separately in the inspector so the two are never confused.
   */
  objectsAt(plane, x, y) {
    const found = [];
    for (const object of this.objects) {
      if (object.plane !== plane) {
        continue;
      }
      const [width, height] = collidedSize(object);
      if (x >= object.x && x < object.x + width && y >= object.y && y < object.y + height) {
        found.push(object);
      }
    }
    return found;
  }

  /** Every object on a plane, for the icon layer. */
  objectsOn(plane) {
    return this.objects.filter((object) => object.plane === plane);
  }

  /** How many objects carry each `kind`, for the resource filter panel. */
  kindCounts() {
    const counts = new Map();
    for (const object of this.objects) {
      if (object.kind) {
        counts.set(object.kind, (counts.get(object.kind) || 0) + 1);
      }
    }
    return counts;
  }

  /** The kinds present, each with whether the exporter called it a service. */
  kinds() {
    const found = new Map();
    for (const object of this.objects) {
      if (object.kind) {
        found.set(object.kind, { kind: object.kind, service: !!object.service });
      }
    }
    return Array.from(found.values()).sort((a, b) => a.kind.localeCompare(b.kind));
  }
}

/** Reads one region document over HTTP. */
export async function loadRegion(regionId) {
  const response = await fetch(`/map/${regionId}.json`);
  if (!response.ok) {
    throw new Error(`GET /map/${regionId}.json -> ${response.status} ${response.statusText}`);
  }
  return new Region(await response.json());
}

/** Reads the index `workshopExport` wrote: what can be opened, and how big each region is. */
export async function loadIndex() {
  const response = await fetch('/map/index.json');
  if (!response.ok) {
    throw new Error(`GET /map/index.json -> ${response.status} ${response.statusText}`);
  }
  const doc = await response.json();
  return doc.entries || [];
}

/**
 * Every bank in the world, from the whole-world scan rather than the exported regions.
 *
 * Sorted by the server, so this only sorts by distance.
 */
export async function loadBanks() {
  const response = await fetch('/map/banks.json');
  if (!response.ok) {
    throw new Error(`GET /map/banks.json -> ${response.status} ${response.statusText}`);
  }
  const doc = await response.json();
  return doc.banks || [];
}

/**
 * The nearest bank overall, and the nearest one on the same plane.
 *
 * Returns both rather than one, because collapsing them needs a weighting — "how many tiles is a
 * plane change worth?" — and any number chosen for that would be invented. Lumbridge is the case
 * that makes it obvious: standing in the courtyard, the castle bank is about 22 tiles away but one
 * plane up, while the nearest bank on your own plane may be much further. Reporting both lets an
 * author decide; reporting one with a made-up penalty decides for them, wrongly half the time.
 */
export function bankSummary(banks, x, y, plane) {
  let samePlane = null;
  let anyPlane = null;
  for (const bank of banks) {
    const dx = bank.x - x;
    const dy = bank.y - y;
    const distance = Math.sqrt(dx * dx + dy * dy);
    if (bank.plane === plane) {
      if (samePlane === null || distance < samePlane.distance) {
        samePlane = { bank, distance };
      }
    }
    if (anyPlane === null || distance < anyPlane.distance) {
      anyPlane = { bank, distance };
    }
  }
  return { samePlane, anyPlane };
}
