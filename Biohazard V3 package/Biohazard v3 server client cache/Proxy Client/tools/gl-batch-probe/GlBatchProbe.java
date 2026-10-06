package tools.glbatchprobe;

import ui.GlBatcher;

/**
 * Phase 7.2b-1 probe: drives the PRODUCTION {@link GlBatcher} on a real GPU and
 * asserts the four things that can be subtly wrong rather than obviously broken.
 *
 * Why each check exists:
 *   1. BYTE ORDER - a packed ARGB int reaches GL as bytes (b,g,r,a); if the shader
 *      swizzle is missing, red and blue are SWAPPED. A picture that is "red and blue
 *      the wrong way round" is easy to miss by eye and impossible to miss here.
 *   2. Y FLIP   - glReadPixels returns rows bottom-up, the client is top-down. Draw a
 *      triangle only in the TOP half and require it to land in the TOP of dest; a
 *      missing flip puts it at the bottom.
 *   3. ALPHA    - the client's DirectColorModel has no alpha mask and the software
 *      path writes 0x00RRGGBB. Inventing 0xFF changes every pixel the framebuffer
 *      hash covers, so the probe requires the alpha byte to be exactly 0.
 *   4. DEPTH    - two triangles over the same pixels; the NEARER one must win.
 *
 * Plus a CONTROL: an undrawn pixel must be the clear colour, so a readback that
 * returns one constant cannot report success.
 *
 * A swap/copy that passes this but is wrong would have to be wrong in a way none of
 * these four can express - which is the standard the rest of this plan uses.
 *
 * Exit code 0 (GL_BATCH_OK) or 1 (GL_BATCH_FAIL).
 */
public final class GlBatchProbe {

    private static final int BLACK = 0x000000;
    private static final int RED = 0x00FF0000;
    private static final int BLUE = 0x000000FF;
    private static final int GREEN = 0x0000FF00;
    private static final int MAGENTA = 0x00FF00FF;

    private static boolean ok = true;

    public static void main(String[] args) {
        GlBatcher batcher = new GlBatcher();

        if (!batcher.ensure()) {
            System.out.println("GlBatcher.ensure() FAILED: " + batcher.failureReason());
            System.out.println("GL_BATCH_PROBE_FAIL");
            System.exit(1);
        }
        System.out.println("batcher   : " + batcher.describe());

        int w = 765;
        int h = 503;

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

        int count = batcher.triangleCount();
        System.out.println("queued    : " + count + " triangles");
        ok &= count == 5;

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

        // (2) Y flip: the same column far down must be clear (the top triangle must NOT
        //     have wrapped to the bottom).
        int below = dest[450 * w + 250];
        report("same column at y=450", below, "expect 0x000000 (clear) - proves Y flip");
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
        System.out.println(ok ? "GL_BATCH_OK" : "GL_BATCH_PROBE_FAIL");
        batcher.dispose();
        System.exit(ok ? 0 : 1);
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
