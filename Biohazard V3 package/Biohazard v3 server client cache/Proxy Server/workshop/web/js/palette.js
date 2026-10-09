/**
 * Floor colours, as served by `GET /palette.json`.
 *
 * The palette is generated in Java (`botworkshop.serve.FloorPalette`) and handed over finished, so
 * there is one implementation of "what colour is floor id 63" rather than one per language. This
 * only looks colours up.
 *
 * `Data/` carries no floor colour table, so these are not the game's colours — they are generated
 * from the floor id purely so that different floors are visibly different. The UI says so; see
 * `index.html`'s left panel. Overriding one is a line in `Data/cfg/floor-palette.cfg`.
 */
export class Palette {
  constructor(colors, meta) {
    this.colors = colors || {};
    this.meta = meta || {};
    /** Lookup results, keyed `kind:id`. Ids repeat across a plane, so this is worth caching. */
    this.cache = new Map();
  }

  static async load() {
    const response = await fetch('/palette.json');
    if (!response.ok) {
      throw new Error(`GET /palette.json -> ${response.status} ${response.statusText}`);
    }
    const doc = await response.json();
    return new Palette(doc.colors, { note: doc.note, overridden: doc.overridden });
  }

  /**
   * The CSS colour for a floor id, or `null` when there is no floor of that kind there.
   *
   * Id 0 means "none" and arrives as `#00000000`. Returning `null` rather than a transparent colour
   * lets the renderer skip the fill entirely, which is both faster and the only way an overlay of 0
   * correctly shows the underlay beneath it.
   */
  colour(kind, id) {
    const key = `${kind}:${id}`;
    if (this.cache.has(key)) {
      return this.cache.get(key);
    }
    const raw = this.colors[key];
    let css = null;
    if (typeof raw === 'string' && /^#[0-9a-f]{6}([0-9a-f]{2})?$/i.test(raw)) {
      css = raw.length === 9 && raw.slice(7).toLowerCase() === '00' ? null : raw;
    }
    this.cache.set(key, css);
    return css;
  }
}
