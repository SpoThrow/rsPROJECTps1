package tools.glbatchprobe;

import java.lang.reflect.Field;

import model.Texture;
import ui.GlBatcher;
import ui.GlTextures;

/**
 * Phase 7.2b-1 and 7.2b-2f probe: drives the PRODUCTION {@link GlBatcher} on a real GPU
 * and asserts the things that can be subtly wrong rather than obviously broken.
 *
 * Why each check exists:
    10| *   1. BYTE ORDER - a packed ARGB int reaches GL as bytes (b,g,r,a); if the shader
 *      swizzle is missing, red and blue are SWAPPED. A picture that is "red and blue
 *      the wrong way round" is easy to miss by eye and impossible to miss here.
 *   2. Y FLIP   - glReadPixels returns rows bottom-up, the client is top-down. Draw a
 *      triangle only in the TOP half and require it to land in the TOP of dest; a
 *      missing flip puts it at the bottom.
 *   3. ALPHA    - the client's DirectColorModel has no alpha mask and the software
 *      path writes 0x00RRGGBB. Inventing 0xFF changes every pixel the framebuffer
 *      hash covers, so the probe requires the alpha byte to be exactly 0.
    20| *   4. DEPTH    - two triangles over the same pixels; the NEARER one must win.
 *
 * And for the textured path (7.2b-2f):
 *   5. TEXEL    - the fragment shader's shade arithmetic must agree with
 *      {@link GlTextures}, the class the HARNESS has already pinned against the real
 *      {@code Texture.method379}. This is the shader-vs-Java half of a two-step chain,
 *      and it is the only way the shader can be verified at all: LWJGL is deliberately
 *      off the harness classpath, so the shader is invisible to it.
 *   6. MAPPING  - the sampled texel must be the one the screen position implies, which
 *      pins the UV divide, the floor, the clamp and the row/column order together.
 *   7. CUTOUT  - a zero texel must be DISCARDED, showing what is behind it, because
 *      that is what the software does when it skips writing transparent texels.
 *
 * Plus a CONTROL: an undrawn pixel must be the clear colour, so a readback that
 * returns one constant cannot report success.
 *
 * A swap/copy that passes this but is wrong would have to be wrong in a way none of
 * these can express - which is the standard the rest of this plan uses.
 *
 * Exit code 0 (GL_BATCH_OK) or 1 (GL_BATCH_FAIL).
 */
public final class GlBatchProbe {

    private static final int BLACK = 0x000000;
    private static final int RED = 0x00FF0000;
    private static final int BLUE = 0x000000FF;
    private static final int GREEN = 0x0000FF00;
    private static final int MAGENTA = 0x00FF00FF;

    /** Parked texture ids. Small, and NOT 0, so nothing is special-cased by accident. */
    private static final int FLAT_TEX = 2;
    private static final int MAP_TEX = 3;
    private static final int CUTOUT_TEX = 4;

    /** The colour every texel of {@link #FLAT_TEX} carries. */
    private static final int FLAT_COLOUR = 0x804020;

    private static boolean ok = true;

    public static void main(String[] args) {
        // The harness's trick, reproduced here: method371 returns the LOADED slot
        // immediately when it is non-null, BEFORE it looks for a Background - so parking a
        // synthetic array is enough to drive the real texture path with no cache at all.
        // lowMem is pinned so the layer side is deterministic for the mapping check.
        Texture.lowMem = true;
        int side = GlTextures.layerSize();
        parkTexture(FLAT_TEX, flatTexture(side));
        parkTexture(MAP_TEX, mappingTexture(side));
        parkTexture(CUTOUT_TEX, cutoutTexture(side));

        GlBatcher batcher = new GlBatcher();

        // The frame target's size is now STATED by the caller rather than baked into GlScene
        // as a constant - see SceneBatch#ensure(int, int). The probe keeps 765x503, which is
        // what it has always rendered at.
        int w = 765;
        int h = 503;

        if (!batcher.ensure(w, h)) {
            System.out.println("GlBatcher.ensure() FAILED: " + batcher.failureReason());
            System.out.println("GL_BATCH_PROBE_FAIL");
            System.exit(1);
        }
        System.out.println("batcher   : " + batcher.describe());
        System.out.println("textures  : side " + side + ", supportsTextures="
                + batcher.supportsTextures());
        ok &= batcher.supportsTextures();

        if (!batcher.beginFrame(BLACK)) {
            System.out.println("beginFrame FAILED");
            System.out.println("GL_BATCH_PROBE_FAIL");
            System.exit(1);
        }

        // (1) top half, flat RED
        batcher.triangle(100, 10, 0.5f, RED, 400, 10, 0.5f, RED, 250, 60, 0.5f, RED);
        // (2) bottom half, flat BLUE
        batcher.triangle(100, 300, 0.5f, BLUE, 400, 300, 0.5f, BLUE, 250, 400, 0.5f, BLUE);
        // (3) depth pair over the SAME pixels: far GREEN first, near MAGENTA second
        batcher.triangle(500, 100, 0.7f, GREEN, 700, 100, 0.7f, GREEN, 600, 200, 0.7f, GREEN);
        batcher.triangle(500, 100, 0.3f, MAGENTA, 700, 100, 0.3f, MAGENTA, 600, 200, 0.3f, MAGENTA);
        // (4) interpolated: RED -> BLUE across, to prove colour is per-vertex
        batcher.triangle(100, 150, 0.5f, RED, 400, 150, 0.5f, BLUE, 250, 220, 0.5f, RED);

        // (5) textured, uniform texel, FOUR different shades - so the shader's block and
        //     shift arithmetic is exercised at every level it can take.
        int[] shades = { 0, 16, 64, 127 };
        for (int i = 0; i < shades.length; i++) {
            int x = 40 + i * 90;
            batcher.textured(x, 430, 0.4f, 0.5f, 0.5f, 1f, shades[i],
                    x + 80, 430, 0.4f, 0.5f, 0.5f, 1f, shades[i],
                    x + 40, 490, 0.4f, 0.5f, 0.5f, 1f, shades[i],
                    FLAT_TEX);
        }

        // (6) textured MAPPING: u/v/w carried by the corners, and w is NOT constant, so the
        //     per-pixel divide is load-bearing rather than a no-op. The expected texel is
        //     computed from the same corners, so this checks the mapping instead of
        //     restating a number.
        batcher.textured(420, 430, 0.4f, 0f, 0f, 1f, 0,
                700, 430, 0.4f, 1f, 0f, 4f, 0,
                560, 490, 0.4f, 0.5f, 1f, 2f, 0,
                MAP_TEX);

        // (7) CUTOUT: left half of the texture is zero (transparent), right half is solid.
        //     Drawn over the black clear colour, so the left must stay CLEAR and the right
        //     must be the solid colour.
        batcher.textured(60, 200, 0.2f, 0f, 0.5f, 1f, 0,
                360, 200, 0.2f, 1f, 0.5f, 1f, 0,
                210, 280, 0.2f, 0.5f, 0.5f, 1f, 0,
                CUTOUT_TEX);

        int count = batcher.triangleCount();
        System.out.println("queued    : " + count + " triangles (" + batcher.texturedCount()
                + " textured)");
        ok &= count == 11;
        ok &= batcher.texturedCount() == 6;

        if (!batcher.flush()) {
            System.out.println("flush FAILED");
            System.out.println("GL_BATCH_PROBE_FAIL");
            System.exit(1);
        }

        int[] dest = new int[w * h];
        if (!batcher.readInto(dest, w, 0, 0)) {
            System.out.println("readInto FAILED");
            System.out.println("GL_BATCH_PROBE_FAIL");
            System.exit(1);
        }

        // (1) byte order + top half + flip, all in one sample: the top triangle is RED.
        int top = dest[20 * w + 250];
        report("top-triangle pixel (250,20)", top, "expect 0x00FF0000 RED");
        ok &= near(top >> 16 & 0xff, 255) && near(top >> 8 & 0xff, 0) && near(top & 0xff, 0);

        // (2) Y flip: (120,470) must be clear. That point is chosen carefully rather than
        //     far away: it lies INSIDE the rectangle the TOP triangle would occupy if the
        //     readback flip were dropped (x 100..400, y 443..493), and the 7.2b-2f
        //     textured geometry that now fills much of the bottom band does not reach it.
        //     So a missing flip paints it RED and fails here, while today it is empty.
        int below = dest[470 * w + 120];
        report("same column at y=470", below, "expect 0x000000 (clear) - proves Y flip");
        ok &= (below & 0xffffff) == 0;

        // (3) bottom triangle present where expected (so the flip is right, not reversed)
        int bottom = dest[380 * w + 250];
        report("bottom-triangle pixel (250,380)", bottom, "expect 0x000000FF BLUE");
        ok &= near(bottom & 0xff, 255) && near(bottom >> 16 & 0xff, 0) && near(bottom >> 8 & 0xff, 0);

        // (4) alpha byte must be 0 everywhere a colour was written
        ok &= reportAlpha(top);
        ok &= reportAlpha(bottom);
        ok &= reportAlpha(dest[150 * w + 300]);

        // (5) depth: nearer MAGENTA beats farther GREEN
        int overlap = dest[130 * w + 600];
        report("depth overlap (600,130)", overlap, "expect 0x00FF00FF MAGENTA (the NEARER one)");
        ok &= near(overlap >> 16 & 0xff, 255) && near(overlap & 0xff, 255) && near(overlap >> 8 & 0xff, 0);

        // (6) interpolation: mid-span between RED and BLUE is neither
        int mid = dest[165 * w + 250];
        report("interpolated (250,165)", mid, "expect RED->BLUE blend (both channels > 0)");
        ok &= (mid >> 16 & 0xff) > 20 && (mid & 0xff) > 20;

        // (7) CONTROL: an undrawn pixel is the clear colour, so readback discriminates
        int control = dest[250 * w + 20];
        report("control undrawn (20,250)", control, "expect 0x000000 clear");
        boolean controlHeld = (control & 0xffffff) == 0;
        System.out.println("readback control : " + (controlHeld
                ? "an UNDRAWN pixel is NOT a drawn colour (good - readback discriminates)"
                : "FAILED - readback returns something constant, so it proves nothing"));
        ok &= controlHeld;

        System.out.println();
        System.out.println("--- textured path: shader vs the harness-pinned GlTextures policy ---");
        ok &= checkShadeSweep(dest, w, shades);
        ok &= checkMapping(dest, w, side);
        ok &= checkCutout(dest, w);

        System.out.println();
        System.out.println(ok ? "GL_BATCH_OK" : "GL_BATCH_PROBE_FAIL");
        batcher.dispose();
        System.exit(ok ? 0 : 1);
    }

    /**
     * The shader's shade arithmetic must equal {@link GlTextures#shade}, which the HARNESS
     * has already pinned against the software rasteriser's own output.
     *
     * <p>Each quad is one shade over a uniform texel, so every pixel of it must be exactly
     * the policy's answer - not "close", because both sides are integer arithmetic on the
     * same packed colour.
     */
    private static boolean checkShadeSweep(int[] dest, int w, int[] shades) {
        boolean good = true;
        for (int i = 0; i < shades.length; i++) {
            int x = 40 + i * 90 + 40;
            int pixel = dest[460 * w + x];
            // The policy is applied to the RAW top-left byte triple the shader packs.
            int expected = GlTextures.shade(FLAT_COLOUR, shades[i]);
            boolean match = (pixel & 0xffffff) == (expected & 0xffffff);
            System.out.println(String.format(
                    "  shade %-4d at (%d,460): 0x%06X  expect 0x%06X %s", shades[i], x, pixel & 0xffffff,
                    expected & 0xffffff, match ? "" : "  <-- MISMATCH"));
            good &= match;
        }
        return good;
    }

    /**
     * The sampled texel must be the one the screen position implies.
     *
     * <p>⚠️ <b>The expected value is COMPUTED from the triangle's own corners, not
     * hardcoded</b>, so this checks the mapping rather than restating one number: the
     * barycentric weights are derived from the screen coordinates, the triple is
     * interpolated with them, divided by its third component, floored and clamped exactly
     * as {@link GlTextures#texelIndex} does - and the texel's own encoded identity is what
     * comes back. That pins the divide, the floor, the clamp and the row/column order in
     * one comparison.
     *
     * <p>⚠️ <b>The triple below is a synthetic AFFINE function of the triangle's corners,
     * and that is not a simplification of the real path - it IS the real path's shape
     * (Phase 7.2b-2l).</b> The attribute carried across this seam is three screen-affine
     * RAMP NUMERATORS, so any affine triple stands in for a real one exactly as far as
     * this probe can tell - which is the point: the probe pins the SHADER's behaviour
     * (linear interpolation, one divide per fragment, floor, clamp) and deliberately does
     * not care how the numerators were derived. ⚠ The weights are unequal on purpose so
     * the divide really matters; with {@code w} constant the divide would be a no-op and
     * a broken shader would still pass.
     */
    private static boolean checkMapping(int[] dest, int w, int side) {
        // Must mirror the corners queued in main(). w varies, so the divide matters.
        float x0 = 420, y0 = 430, u0 = 0, v0 = 0, w0 = 1;
        float x1 = 700, y1 = 430, u1 = 1, v1 = 0, w1 = 4;
        float x2 = 560, y2 = 490, u2 = 0.5f, v2 = 1, w2 = 2;
        float area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);

        boolean good = true;
        // Two interior sample points, so a single lucky hit cannot carry the check.
        int[][] points = { { 560, 450 }, { 520, 460 } };
        for (int[] p : points) {
            float px = p[0];
            float py = p[1];
            float l0 = ((x1 - px) * (y2 - py) - (x2 - px) * (y1 - py)) / area;
            float l1 = ((x2 - px) * (y0 - py) - (x0 - px) * (y2 - py)) / area;
            float l2 = 1f - l0 - l1;
            float u = l0 * u0 + l1 * u1 + l2 * u2;
            float v = l0 * v0 + l1 * v1 + l2 * v2;
            float ww = l0 * w0 + l1 * w1 + l2 * w2;
            int col = GlTextures.texelIndex(u / ww, side);
            int row = GlTextures.texelIndex(v / ww, side);
            int expected = mappingColour(col, row);
            int pixel = dest[p[1] * w + p[0]];
            boolean match = (pixel & 0xffffff) == (expected & 0xffffff);
            System.out.println(String.format(
                    "  mapping at (%d,%d): barycentric u=%.4f v=%.4f w=%.4f -> texel (%d,%d)"
                            + " = 0x%06X, read 0x%06X %s",
                    p[0], p[1], u, v, ww, col, row, expected & 0xffffff, pixel & 0xffffff,
                    match ? "" : "  <-- MISMATCH"));
            good &= match;
        }
        return good;
    }

    /**
     * A texel of ZERO must be discarded, leaving what was already there.
     *
     * <p>⚠️ The left and right samples are both taken INSIDE the quad and must differ: a
     * probe that only checked "something was drawn" would pass even if the cutout were
     * ignored, so the two halves are required to disagree.
     */
    private static boolean checkCutout(int[] dest, int w) {
        // Both samples are well inside the triangle: at y=240 it spans x 135..285.
        int left = dest[240 * w + 160];
        int right = dest[240 * w + 260];
        int expected = mappingColour(40, 32) & 0xffffff;
        System.out.println(String.format("  cutout left  (160,240): 0x%06X  expect clear 0x000000",
                left & 0xffffff));
        System.out.println(String.format("  cutout right (260,240): 0x%06X  expect 0x%06X",
                right & 0xffffff, expected));
        boolean leftClear = (left & 0xffffff) == 0;
        boolean rightSolid = (right & 0xffffff) == expected;
        if (!leftClear || !rightSolid) {
            System.out.println("  CUTOUT FAIL: transparent texels were not discarded (or solid ones"
                    + " were) - the two halves must differ");
        }
        return leftClear && rightSolid;
    }

    /** Every texel the same colour, so only the shade can change the output. */
    private static int[] flatTexture(int side) {
        int[] tex = new int[side * side * 4];
        for (int i = 0; i < side * side; i++) {
            tex[i] = FLAT_COLOUR;
        }
        return tex;
    }

    /**
     * Block 0 encodes each texel's own (col,row), so a painted pixel names the texel the
     * shader sampled.
     *
     * <p>⚠️ The low three bits of red and green are avoided, and the value is never zero,
     * so the encoding survives the shade policy's mask AND is never mistaken for a
     * transparent texel - either would make the readback ambiguous.
     */
    private static int[] mappingTexture(int side) {
        int[] tex = new int[side * side * 4];
        for (int row = 0; row < side; row++) {
            for (int col = 0; col < side; col++) {
                tex[row * side + col] = mappingColour(col, row);
            }
        }
        return tex;
    }

    /** {@code (col << 16) | (row << 8) | 1}: unique per texel, never zero, mask-stable. */
    private static int mappingColour(int col, int row) {
        return ((col & 0x7f) << 16) | ((row & 0x7f) << 8) | 1;
    }

    /** Left half zero (transparent), right half the mapping colour. */
    private static int[] cutoutTexture(int side) {
        int[] tex = new int[side * side * 4];
        for (int row = 0; row < side; row++) {
            for (int col = 0; col < side; col++) {
                tex[row * side + col] = col < side / 2 ? 0 : mappingColour(40, 32);
            }
        }
        return tex;
    }

    /**
     * Parks a synthetic texture in {@code Texture}'s loaded-texture slot.
     *
     * <p>By reflection on purpose: the field is private, and relaxing production visibility
     * so that a probe can reach it is the wrong trade.
     */
    private static void parkTexture(int tid, int[] tex) {
        try {
            Field f = Texture.class.getDeclaredField("anIntArrayArray1479");
            f.setAccessible(true);
            int[][] loaded = (int[][]) f.get(null);
            loaded[tid] = tex;
        } catch (Exception e) {
            throw new RuntimeException("could not park synthetic texture " + tid, e);
        }
    }

    private static boolean reportAlpha(int pixel) {
        int a = pixel >>> 24;
        if (a != 0) {
            System.out.println("  ALPHA FAIL: pixel 0x" + Integer.toHexString(pixel)
                    + " has alpha byte 0x" + Integer.toHexString(a)
                    + " but the client's buffers are 0x00RRGGBB");
            return false;
        }
        return true;
    }

    private static boolean near(int value, int expected) {
        return Math.abs(value - expected) <= 6;
    }

    private static void report(String what, int pixel, String expectation) {
        System.out.println(String.format("  %-32s 0x%06X   %s", what, pixel & 0xffffff, expectation));
    }

    private GlBatchProbe() {
    }
}
