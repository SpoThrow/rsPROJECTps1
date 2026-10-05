import java.lang.reflect.Field;
import model.Frames;

/**
 * Headless live-gate probe for the ONE gap the Phase 6.5.2 unit tests cannot
 * reach: whether the predicate wired into Frames.loadLooseFrameFiles actually
 * FIRES against the real cache root.
 *
 * Why unit tests cannot cover it: loadLooseFrameFiles() reads the real cache
 * directory (%USERPROFILE%\Biohazard.474) via signlink.findcachedir(), and no
 * hermetic test supplies one. This probe calls the REAL entry point
 * (Frames.loadFrames) against the REAL cache, which holds exactly
 * 1777.dat, 2160.dat and 3502.dat.
 *
 * Why the result is decisive rather than "it did not crash":
 *   - loadPacked() populates `frameData` (byte[][]) ONLY.
 *   - animationlist[file] is populated ONLY by the loose loader at load time
 *     (the lazy decode in the accessor runs on access, not during loadFrames).
 *   - So animationlist[1777] being non-zero can only come from the loose path,
 *     and a predicate that never fires leaves animationlist unallocated (null)
 *     or the slot at its initial length-0 array.
 *
 * Non-vacuity controls:
 *   - frameData[500] != null  => packed decoding worked, so the environment is
 *     genuinely live and a "nothing loaded" run cannot masquerade as a pass.
 *   - animationlist[500] == 0 => a packed-only id stays OUT of animationlist,
 *     so the non-zero results are specific to the loose path rather than a
 *     blanket "everything loads" artefact.
 *
 * Exit code 0 = LOOSE_OK, 1 = LOOSE_FAIL.
 */
public class LooseFrameProbe {

    /** The loose numeric *.dat archives the real cache root is known to hold. */
    private static final int[] LOOSE_SLOTS = {1777, 2160, 3502};

    /** A packed-only id, used to prove animationlist is not blanket-populated. */
    private static final int PACKED_ONLY_CONTROL = 500;

    public static void main(String[] args) throws Exception {
        Frames.loadFrames();

        Field alField = Frames.class.getDeclaredField("animationlist");
        alField.setAccessible(true);
        Frames[][] animationlist = (Frames[][]) alField.get(null);

        Field fdField = Frames.class.getDeclaredField("frameData");
        fdField.setAccessible(true);
        byte[][] frameData = (byte[][]) fdField.get(null);

        boolean ok = true;

        System.out.println("animationlist null? " + (animationlist == null));
        System.out.println("frameData     null? " + (frameData == null));

        boolean packedLive = frameData != null
                && frameData.length > PACKED_ONLY_CONTROL
                && frameData[PACKED_ONLY_CONTROL] != null;
        System.out.println("CONTROL frameData[" + PACKED_ONLY_CONTROL
                + "] present (packed path live) = " + packedLive);
        if (!packedLive) {
            ok = false;
        }

        for (int id : LOOSE_SLOTS) {
            int len = -1;
            if (animationlist != null && id < animationlist.length && animationlist[id] != null) {
                len = animationlist[id].length;
            }
            System.out.println("LOOSE slot " + id + " -> animationlist length " + len);
            if (len <= 0) {
                ok = false;
            }
        }

        int control = -1;
        if (animationlist != null
                && PACKED_ONLY_CONTROL < animationlist.length
                && animationlist[PACKED_ONLY_CONTROL] != null) {
            control = animationlist[PACKED_ONLY_CONTROL].length;
        }
        System.out.println("CONTROL animationlist[" + PACKED_ONLY_CONTROL
                + "] (packed-only, expect 0) = " + control);
        if (control != 0) {
            ok = false;
        }

        System.out.println(ok ? "LOOSE_OK" : "LOOSE_FAIL");
        System.exit(ok ? 0 : 1);
    }
}
