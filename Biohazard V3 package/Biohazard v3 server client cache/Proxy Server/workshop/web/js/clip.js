/**
 * What the bits in a tile's clip value mean, and where each meaning comes from.
 *
 * Every mask here is quoted from the server rather than guessed, because the value of this tool is
 * that it cannot disagree with the server about the world. If a mask below is ever wrong the map
 * would show a wall where the server walks, which is the exact failure the tool exists to prevent.
 *
 *   - `WALK_MASK` is the union of every mask `SmartPathFinder.canStep` applies — the four cardinal
 *     masks `0x1280108 / 0x1280180 / 0x1280102 / 0x1280120` and the four diagonal ones
 *     `0x128010e / 0x1280183 / 0x1280138 / 0x12801e0`. Their union is exactly `0x12801FF`
 *     (`0x1280100 | 0xFF`), so "no block bit is set" is the same statement as "no direction
 *     refuses entry". That equivalence is what makes the `walkable` answer below exact rather than
 *     approximate.
 *   - `0x100` is the solid bit: `Region.addClippingForSolidObject` starts its value at `256` and
 *     applies it to every tile of the object's footprint.
 *   - `0x20000` is projectile-solid. It is *not* in `WALK_MASK` (see the masks above), so a tile
 *     carrying only this bit is still walkable — it just cannot be shot across.
 *   - `0x200000` is a walk-block: `Region.loadMaps` sets it on every tile whose terrain flags have
 *     bit 0, and `Region.addObject` sets it for a type-22 ground object that has actions *and*
 *     blocks walk. It is therefore *not* "there is a floor here" — it is "you cannot stand here",
 *     which is why it is inside every `SmartPathFinder` mask and inside the fail-closed `BLOCKED`
 *     value in `Region`. (The earlier session's notes called this bit "terrain occupancy"; that
 *     reading is wrong in the direction that matters, because it would make every walled-off lake
 *     and every building interior look walkable. A plane of Lumbridge has a non-zero underlay on
 *     all 4096 tiles and yet only 505 carry this bit, so it cannot mean "has a floor".)
 *   - `0x01`–`0x80` are the directional wall bits, set by the wall and wall-decoration object types
 *     (`Region.addObject`'s type 0–3 branch applies values like `128 + 8`, `1`, `4`, `32`).
 *   - Anything else is left unnamed on purpose. The server sets bits this viewer has no reading for,
 *     and inventing a label for one would be worse than printing the hex.
 */

/** Bits that refuse entry from at least one direction. See the derivation above. */
export const WALK_MASK = 0x12801FF;

/** The solid-object bit, and the projectile bit, as `SmartPathFinder.OCCUPANT` groups them. */
export const SOLID_BIT = 0x100;
export const PROJECTILE_BIT = 0x20000;
export const OCCUPANT_MASK = SOLID_BIT | PROJECTILE_BIT;

/** Terrain and type-22 walk-block bit. */
export const BLOCKED_BIT = 0x200000;

/** Directional wall bits, low byte. */
const DIRECTIONAL_MASK = 0xFF;

/** The named bits, most specific first. */
const NAMED = [
  { mask: SOLID_BIT, name: 'solid object', blocks: true },
  { mask: BLOCKED_BIT, name: 'blocked terrain, or a type-22 object that blocks walk', blocks: true },
  { mask: PROJECTILE_BIT, name: 'projectile-solid only', blocks: false },
];

/**
 * True when a creature can step onto this tile from any direction.
 *
 * Exact, not a heuristic: see the `WALK_MASK` derivation above.
 */
export function isWalkable(clip) {
  return (clip & WALK_MASK) === 0;
}

/** True when nothing at all has marked this tile — no wall, no object, no floor. */
export function isClear(clip) {
  return clip === 0;
}

/**
 * The named parts of a clip value.
 *
 * @returns {{mask: number, hex: string, name: string, blocks: boolean}[]} one entry per set bit or
 *   named group, in a stable order, with unnamed bits reported as `0x…` rather than dropped.
 */
export function describe(clip) {
  const parts = [];
  let remaining = clip;

  for (const entry of NAMED) {
    if ((clip & entry.mask) !== 0) {
      parts.push({ mask: entry.mask, hex: hex(entry.mask), name: entry.name, blocks: entry.blocks });
      remaining &= ~entry.mask;
    }
  }
  if ((remaining & DIRECTIONAL_MASK) !== 0) {
    parts.push({
      mask: remaining & DIRECTIONAL_MASK,
      hex: hex(remaining & DIRECTIONAL_MASK),
      name: 'directional wall',
      blocks: true,
    });
    remaining &= ~DIRECTIONAL_MASK;
  }
  // Whatever is left is a bit this viewer has no reading for. Print it rather than hide it: a
  // silent omission is how a viewer starts lying about the world.
  for (let bit = 0; bit < 32; bit++) {
    const mask = (1 << bit) >>> 0;
    if ((remaining & mask) !== 0) {
      parts.push({ mask, hex: hex(mask), name: 'unmapped bit', blocks: true });
    }
  }
  return parts;
}

/** How a tile should be tinted by the clipping overlay. */
export function overlayTone(clip) {
  if (!isWalkable(clip)) {
    return 'blocked';
  }
  if ((clip & PROJECTILE_BIT) !== 0) {
    return 'projectile';
  }
  return null;
}

/** The tile flags from the terrain stream, named only where the server actually reads them. */
export function describeFlags(flags) {
  const parts = [];
  if ((flags & 1) === 1) {
    // Not "there is a floor here": the server turns this into the 0x200000 walk-block, so on a
    // plane where it is set the tile cannot be stood on. See BLOCKED_BIT above.
    parts.push('blocked (0x1) — the server turns this into clip 0x200000');
  }
  if ((flags & 2) === 2) {
    parts.push('bridge bit (0x2) — on plane 1 this tile belongs to the plane below');
  }
  const rest = flags & ~3;
  if (rest !== 0) {
    parts.push(`${hex(rest)} set but not read by the server`);
  }
  return parts;
}

function hex(value) {
  return `0x${(value >>> 0).toString(16).toUpperCase()}`;
}
