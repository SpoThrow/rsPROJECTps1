/**
 * The whole world's region index — `BOT_TOOLING.md` §4 (the map view's world).
 *
 * `workshopExport` writes `world.json` from every region in `map_index`, not from the exported
 * subset, so this is the one place that knows where the world *is*: which regions exist, which ones
 * this export wrote, and which corner each sits in. The viewer uses it for two things — drawing the
 * continuous map (including the regions the export left out, which is a fact, not a gap) and
 * deciding which region documents to fetch as the camera moves.
 *
 * The lookup is by corner rather than a scan: region ids are not laid out in a way anyone can reason
 * about from a coordinate, but corners are aligned to the region size, so `regionAt` is two
 * divisions and a map read.
 */

/** Tiles per side of a region. Must match `region.js`. */
export const REGION_SIZE = 64;

export class WorldIndex {
  /** @param {object} doc the parsed `world.json` */
  constructor(doc) {
    if (!doc) {
      throw new Error('not a world document');
    }
    this.entries = doc.entries || [];
    this.count = typeof doc.regions === 'number' ? doc.regions : this.entries.length;
    this.exported = typeof doc.exported === 'number'
      ? doc.exported
      : this.entries.filter((entry) => entry.exported).length;

    /** @type {Map<number, object>} by region id, for the region picker and jump. */
    this.byId = new Map(this.entries.map((entry) => [entry.regionId, entry]));
    /** @type {Map<string, object>} by corner, for `regionAt`. */
    this.byCorner = new Map(this.entries.map((entry) => [`${entry.baseX}:${entry.baseY}`, entry]));

    const corners = this.entries;
    this.minX = typeof doc.minX === 'number' ? doc.minX : Math.min(0, ...corners.map((e) => e.baseX));
    this.maxX = typeof doc.maxX === 'number' ? doc.maxX : Math.max(0, ...corners.map((e) => e.baseX + 63));
    this.minY = typeof doc.minY === 'number' ? doc.minY : Math.min(0, ...corners.map((e) => e.baseY));
    this.maxY = typeof doc.maxY === 'number' ? doc.maxY : Math.max(0, ...corners.map((e) => e.baseY + 63));
  }

  /** The entry owning an absolute tile, or null when no region lives there. */
  regionAt(x, y) {
    const baseX = Math.floor(x / REGION_SIZE) * REGION_SIZE;
    const baseY = Math.floor(y / REGION_SIZE) * REGION_SIZE;
    return this.byCorner.get(`${baseX}:${baseY}`) || null;
  }

  byRegionId(regionId) {
    return this.byId.get(regionId) || null;
  }

  /**
   * The entries whose 64x64 box overlaps the window, by walking the region grid rather than all
   * 1226 entries — the cost is the number of regions on screen, not the size of the world.
   */
  overlapping(minX, maxX, minY, maxY) {
    const found = [];
    const startX = Math.floor(minX / REGION_SIZE) * REGION_SIZE;
    const startY = Math.floor(minY / REGION_SIZE) * REGION_SIZE;
    for (let baseX = startX; baseX <= maxX; baseX += REGION_SIZE) {
      for (let baseY = startY; baseY <= maxY; baseY += REGION_SIZE) {
        const entry = this.byCorner.get(`${baseX}:${baseY}`);
        if (entry) {
          found.push(entry);
        }
      }
    }
    return found;
  }

  /** The whole map's extent, for framing it. */
  get bounds() {
    return {
      minX: this.minX,
      maxX: this.maxX,
      minY: this.minY,
      maxY: this.maxY,
      width: this.maxX - this.minX + 1,
      height: this.maxY - this.minY + 1,
    };
  }

  /** Every region this export wrote a document for, for the picker. */
  openable() {
    return this.entries.filter((entry) => entry.exported);
  }
}

export async function loadWorld() {
  const response = await fetch('/map/world.json');
  if (!response.ok) {
    throw new Error(`GET /map/world.json -> ${response.status} ${response.statusText}`);
  }
  return new WorldIndex(await response.json());
}

/** The authored places `workshopExport` wrote from `locations.cfg`. */
export async function loadLocations() {
  const response = await fetch('/map/locations.json');
  if (!response.ok) {
    throw new Error(`GET /map/locations.json -> ${response.status} ${response.statusText}`);
  }
  const doc = await response.json();
  // The kinds come from the server's own parser, so the editor's picker cannot offer a kind the
  // server would reject. `objectKinds` is the subset the world can be scanned for: the authoring
  // tool counts objects in a drafted box, and a kind with no objects behind it (a shop, a teleport)
  // must not be reported as an empty box.
  return { rows: doc.rows || [], kinds: doc.kinds || [], objectKinds: doc.objectKinds || [] };
}

/**
 * Sends drafted `locations.cfg` rows to the workshop server, which parses them with the server's own
 * reader and answers with the canonical rows (or the reason each was rejected).
 *
 * The browser never decides what the file says. `action` is `check` to ask, or `append` to write.
 */
export async function draftLocations(rows, action = 'check') {
  const response = await fetch(`/locations/${action}`, {
    method: 'POST',
    headers: { 'Content-Type': 'text/plain; charset=utf-8' },
    body: rows.join('\n'),
  });
  const report = await response.json();
  if (!response.ok && report.rejected === 0) {
    // A transport-level failure (413, or no rows) rather than a per-row rejection.
    throw new Error(`POST /locations/${action} -> ${response.status} ${response.statusText}`);
  }
  return report;
}

/** The `@BotNode` palette, or null when it has not been exported yet. */
export async function loadNodes() {
  try {
    const response = await fetch('/nodes.json');
    if (!response.ok) {
      return null;
    }
    return await response.json();
  } catch (e) {
    return null;
  }
}
