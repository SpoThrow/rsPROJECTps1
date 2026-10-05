import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.TreeMap;

import cache.FileOperations;
import def.Animation;
import def.CurseData667;
import def.SpotAnim;
import net.Stream;

/**
 * Repeatable probe for Phase 6.5.3's central claim: that the CursePack's 667 definitions
 * are read correctly by the 474 readers.
 *
 * WHY THIS IS A TOOL AND NOT A HARNESS TEST
 * The claim is about the REAL pack at %USERPROFILE%\Biohazard.474\CursePack. No hermetic
 * test can supply that file, so the harness can only pin the two opcode tables against
 * EACH OTHER. This probe closes the remaining gap by running BOTH paths over the ACTUAL
 * data and requiring them to agree entry by entry. It calls the shipping code by
 * reflection, so it cannot drift from what it checks.
 *
 * WHY THAT MATTERS MORE THAN IT SOUNDS
 * CurseData667 reads WANTED entries with the 474 readers but walks UNWANTED ones with
 * hand-written skippers. If the two disagree about how many bytes an opcode occupies, the
 * walk desynchronises. What happens next depends on how far it drifts, and BOTH outcomes
 * are bad: if it walks past the end of the file the reads throw (Stream indexes its array
 * directly, so an overrun is an ArrayIndexOutOfBoundsException propagating out of
 * CurseData667.inject, which has no try/catch of its own); if it stays inside the file it
 * lands on the wrong bytes and silently mangles every definition after the desync point.
 * Either way "the two paths agree on the real data" is the property the live gate rests
 * on, and this probe measures exactly that instead of inferring it.
 *
 * NO THIRD OPCODE TABLE, AND NO OVERCLAIM
 * The entry-start opcode counts below are read from the raw byte at each entry boundary
 * the REAL skipper produced, so this probe adds no opcode table of its own. They are
 * explicitly NOT a full opcode histogram: a full one is not observable from here (Stream
 * is final, and the skippers do not report the opcodes they read), and it is not needed.
 * What the probe asserts is the AGREEMENT of the two paths entry by entry, and that alone
 * settles the opcode-12 question: a single 12 makes the reader consume nothing where the
 * skipper consumes one byte, so the cursor ends up one byte out and every later boundary
 * disagrees. Per-entry agreement therefore PROVES opcode 12 never occurs in the data - a
 * stronger statement than a histogram, because it is derived from the shipping code rather
 * than from a copy of its rules. (A mutation test confirms this probe detects exactly such
 * a drift: making the skipper read opcode 7 as a byte instead of a word fails it.)
 *
 * Exit codes: 0 = the two paths agree and both reach exact EOF; 1 = they disagree,
 * short-walk, or error; 2 = the CursePack is not present under the cache root.
 */
public class CurseOpcodeProbe {

    public static void main(String[] args) {
        File root = CurseData667.findRoot();
        if (root == null) {
            System.out.println("CursePack not found under the cache root - nothing to probe.");
            System.exit(2);
            return;
        }
        System.out.println("CursePack: " + root.getAbsolutePath());
        System.out.println();

        boolean ok = true;
        ok &= probe(root, "seq.dat", "skipSequence", "seq");
        ok &= probe(root, "spotanim.dat", "skipSpotAnim", "spotanim");

        System.out.println(ok ? "OPCODE_PROBE_OK" : "OPCODE_PROBE_FAIL");
        System.exit(ok ? 0 : 1);
    }

    /**
     * Walks every entry of one file twice - once with the pack's private skipper, once with
     * the 474 reader - and requires the cursor to land identically after each. A walk that
     * errors, overruns the file, or fails to advance is reported as a failure rather than
     * escaping as a stack trace, so a broken pack reads as a verdict instead of a crash.
     */
    private static boolean probe(File root, String fileName, String skipperName, String label) {
        File file = new File(root, fileName);
        if (!file.isFile()) {
            System.out.println("[" + label + "] missing: " + fileName);
            return false;
        }
        byte[] raw = FileOperations.ReadFile(file.getPath());
        if (raw == null || raw.length < 2) {
            System.out.println("[" + label + "] unreadable or too short: " + fileName);
            return false;
        }

        // The same header read the production path performs.
        Stream header = new Stream(raw);
        int declaredEntries = header.readUnsignedWord();
        int bodyStart = header.currentOffset;

        int[] entryStarts = new int[declaredEntries];
        int[] skipOffsets = new int[declaredEntries];
        int[] readOffsets = new int[declaredEntries];

        // Pass 1: the REAL skipper. Records where each entry starts and ends.
        String skipError = null;
        int skipWalked = 0;
        Stream skipStream = new Stream(raw);
        skipStream.currentOffset = bodyStart;
        for (int i = 0; i < declaredEntries; i++) {
            entryStarts[i] = skipStream.currentOffset;
            if (entryStarts[i] >= raw.length) {
                skipError = "entry " + i + " starts past EOF (" + entryStarts[i] + ")";
                break;
            }
            try {
                invokeSkip(skipStream, skipperName);
            } catch (Throwable t) {
                skipError = "entry " + i + " threw " + describe(t);
                break;
            }
            skipOffsets[i] = skipStream.currentOffset;
            if (skipOffsets[i] <= entryStarts[i] || skipOffsets[i] > raw.length) {
                skipError = "entry " + i + " did not advance cleanly (" + entryStarts[i]
                        + " -> " + skipOffsets[i] + " of " + raw.length + ")";
                break;
            }
            skipWalked++;
        }

        // Pass 2: the 474 reader, over the entries the skipper managed to walk.
        String readError = null;
        int readWalked = 0;
        Stream readStream = new Stream(raw);
        readStream.currentOffset = bodyStart;
        for (int i = 0; i < skipWalked; i++) {
            try {
                if (label.equals("seq")) {
                    new Animation().readValues(readStream);
                } else {
                    new SpotAnim().readValues(readStream);
                }
            } catch (Throwable t) {
                readError = "entry " + i + " threw " + describe(t);
                break;
            }
            readOffsets[i] = readStream.currentOffset;
            if (readOffsets[i] <= entryStarts[i] || readOffsets[i] > raw.length) {
                readError = "entry " + i + " did not advance cleanly (" + entryStarts[i]
                        + " -> " + readOffsets[i] + ")";
                break;
            }
            readWalked++;
        }

        int firstDiff = -1;
        for (int i = 0; i < skipWalked && i < readWalked; i++) {
            if (skipOffsets[i] != readOffsets[i]) {
                firstDiff = i;
                break;
            }
        }

        // Entry-start opcodes, observed from the boundaries the REAL skipper produced.
        TreeMap<Integer, Integer> entryStartOpcodes = new TreeMap<Integer, Integer>();
        for (int i = 0; i < skipWalked; i++) {
            int op = raw[entryStarts[i]] & 0xff;
            Integer prev = entryStartOpcodes.get(op);
            entryStartOpcodes.put(op, prev == null ? 1 : prev + 1);
        }

        boolean fullyWalked = skipWalked == declaredEntries && readWalked == declaredEntries;
        boolean reachedEof = fullyWalked
                && skipOffsets[declaredEntries - 1] == raw.length
                && readOffsets[declaredEntries - 1] == raw.length;
        boolean agree = fullyWalked && firstDiff < 0;

        System.out.println("[" + label + "] " + fileName);
        System.out.println("  declared entries     : " + declaredEntries);
        System.out.println("  file bytes           : " + raw.length);
        System.out.println("  skipper walked       : " + skipWalked + " entries"
                + (skipError == null ? "" : "   ERROR: " + skipError));
        System.out.println("  reader walked        : " + readWalked + " entries"
                + (readError == null ? "" : "   ERROR: " + readError));
        System.out.println("  both reached EOF     : " + reachedEof);
        System.out.println("  paths agree per entry: " + agree
                + (firstDiff < 0 ? "" : "   FIRST DIFF at entry " + firstDiff
                        + " skip=" + skipOffsets[firstDiff] + " read=" + readOffsets[firstDiff]));
        System.out.println("  entry-start opcodes  : " + entryStartOpcodes
                + "   (first opcode of each entry only)");
        if (agree) {
            System.out.println("  opcode 12            : ABSENT - proven by the per-entry agreement above,");
            System.out.println("                         since a single 12 would move the cursor by one byte");
        } else {
            System.out.println("  opcode 12            : UNKNOWN - the paths already disagree; see FIRST DIFF");
        }
        System.out.println();

        return agree && reachedEof;
    }

    /** Invokes one of CurseData667's private skippers on a live stream. */
    private static void invokeSkip(Stream s, String method) {
        try {
            Method m = CurseData667.class.getDeclaredMethod(method, Stream.class);
            m.setAccessible(true);
            m.invoke(null, s);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw new RuntimeException(cause == null ? e.toString() : cause.toString(), cause);
        } catch (Exception e) {
            throw new RuntimeException("could not invoke " + method, e);
        }
    }

    private static String describe(Throwable t) {
        String s = t.toString();
        return s.length() > 90 ? s.substring(0, 90) + "..." : s;
    }

    private CurseOpcodeProbe() {
    }
}
