/**
 * The run-length codec for the tile planes — a mirror of `botworkshop.export.Rle`.
 *
 * Format: pairs joined by `,`, each `<value>*<count>`. An empty plane is the empty string.
 *
 * This is the one piece of decoding the viewer does for itself rather than reading a number the
 * server already worked out, so it is also the one piece that can silently disagree with Java and
 * shift every tile in the region. Two things guard it:
 *
 * 1. `decode` refuses a string whose runs do not total exactly the length asked for, so a
 *    truncated or misread plane throws instead of quietly producing a short one.
 * 2. `selfTest` exercises the same cases `ExportCodecTest` pins on the Java side, and
 *    `app.js` runs it at startup — a disagreement surfaces in the status line on load rather
 *    than as a subtly wrong map.
 */

/** The cases both sides agree on, written out here so a drift is visible and testable. */
export const CASES = [
  { name: 'a constant plane is one pair', encoded: '5*4', length: 4, expected: [5, 5, 5, 5] },
  { name: 'runs split where the value changes', encoded: '1*2,2*1,1*1', length: 4, expected: [1, 1, 2, 1] },
  { name: 'a negative value round-trips', encoded: '-1*3', length: 3, expected: [-1, -1, -1] },
  { name: 'a large clip value round-trips', encoded: '2031616*2', length: 2, expected: [2031616, 2031616] },
  { name: 'a single pair', encoded: '0*1', length: 1, expected: [0] },
];

/**
 * Decodes `encoded` into exactly `expectedLength` values.
 *
 * @throws {Error} if the runs do not total `expectedLength`, or a run is malformed. Both mean the
 *   viewer and the exporter disagree, and a wrong map is worse than a refused one.
 */
export function decode(encoded, expectedLength) {
  if (encoded === null || encoded === undefined || encoded === '') {
    if (expectedLength !== 0) {
      throw new Error(`empty run-length string for ${expectedLength} values`);
    }
    return new Int32Array(0);
  }
  const out = new Int32Array(expectedLength);
  let at = 0;
  for (const pair of encoded.split(',')) {
    const star = pair.indexOf('*');
    if (star <= 0 || star === pair.length - 1) {
      throw new Error(`malformed run "${pair}"`);
    }
    const value = Number(pair.slice(0, star).trim());
    const count = Number(pair.slice(star + 1).trim());
    if (!Number.isInteger(value) || !Number.isInteger(count)) {
      throw new Error(`malformed run "${pair}"`);
    }
    if (count <= 0) {
      throw new Error(`non-positive run length in "${pair}"`);
    }
    if (at + count > expectedLength) {
      throw new Error(`run "${pair}" runs past the ${expectedLength} values asked for`);
    }
    for (let i = 0; i < count; i++) {
      out[at++] = value;
    }
  }
  if (at !== expectedLength) {
    throw new Error(`run lengths total ${at}, expected ${expectedLength}`);
  }
  return out;
}

/**
 * Runs {@link CASES} and the rejection cases.
 *
 * @returns {string[]} the failures, empty when the decoder agrees with the Java one.
 */
export function selfTest() {
  const failures = [];
  for (const testCase of CASES) {
    try {
      const got = Array.from(decode(testCase.encoded, testCase.length));
      if (got.join(',') !== testCase.expected.join(',')) {
        failures.push(`${testCase.name}: got [${got}], expected [${testCase.expected}]`);
      }
    } catch (e) {
      failures.push(`${testCase.name}: threw ${e.message}`);
    }
  }
  // A short plane must throw. If it ever does not, every tile after the gap is wrong and nothing
  // downstream would notice.
  try {
    decode('5*3', 4);
    failures.push('a short plane was accepted instead of refused');
  } catch (expected) {
    // Correct.
  }
  try {
    decode('5*0', 1);
    failures.push('a zero-length run was accepted instead of refused');
  } catch (expected) {
    // Correct.
  }
  return failures;
}
