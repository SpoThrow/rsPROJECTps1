package model;

/**
 * The single owner of the frame-slot budget (Phase 6.5.2).
 *
 * <p><b>Why this exists.</b> {@link Frames} keeps decoded frames in ONE array indexed by
 * FILE id ({@code animationlist[file]}), and three independent writers allocate into it.
 * Until now they were separated only by convention - nothing enforced the separation and
 * nothing would have complained if two of them had collided. This class is the one place
 * that states the budget, so a fourth writer has a single place to declare its range and
 * a single place to be checked against.
 *
 * <table>
 * <tr><th>writer</th><th>entry point</th><th>slots</th></tr>
 * <tr><td>packed 474</td><td>{@link Frames#load(int)}</td>
 *     <td>0..3229, dense - read from {@code Frames.dat}/{@code Skins.dat}</td></tr>
 * <tr><td>loose numeric</td><td>{@link Frames#load_647(int)}</td>
 *     <td>ANY id &gt;= {@link #LOOSE_MIN_ID}</td></tr>
 * <tr><td>CursePack 667</td><td>{@code CurseData667.loadAnimationFile}</td>
 *     <td>{@link #CURSE_BASE} .. {@link #CURSE_END}-1</td></tr>
 * </table>
 *
 * <p><b>The invariant this class exists to state.</b> The CursePack's SOURCE file ids are
 * 2998, 3012, 3013, 3016, 3018, 3019 and 3020 - every one of which sits INSIDE the packed
 * 474 range (0..3229, measured from the real {@code Frames.dat}). Loading them under their
 * own ids would therefore have OVERWRITTEN 474 frame files. {@link #CURSE_BASE} is the
 * offset that prevents it, and {@code CurseData667.remapFile} rewrites each sequence's
 * frame keys to point at it.
 *
 * <p><b>Nothing here decides precedence.</b> Which writer wins for a given slot is decided
 * by {@code Frames.method531} (a writer is only tried when the slot is EMPTY) and by
 * {@code Frames.loadFrames} running the packed load before the loose one. Those orderings
 * are load-bearing and live with the loader; this class only owns the NUMBERS.
 *
 * <p>&#9888; <b>{@link #isContestedByLooseLoading} is a real hazard, not a hypothetical
 * one.</b> The loose loader accepts ANY numeric {@code {id}.dat} &gt;= {@link #LOOSE_MIN_ID}
 * found in the cache root, which <em>includes</em> the whole CursePack window. So a file
 * named {@code 28000.dat} sitting in the cache root would be loaded into the CursePack's own
 * first slot at startup, and because {@code method531} never re-reads a populated slot the
 * CursePack's file for that slot would then be unreachable. Today no such file exists (the
 * real cache root holds exactly 1777, 2160 and 3502), so the two writers do not actually
 * collide - but nothing prevents it, which is precisely why the ranges are stated in one
 * place rather than left to convention.
 */
public final class FrameSlots {

	/** Lowest file id the loose-numeric loader will pick up from the cache root. */
	public static final int LOOSE_MIN_ID = 100;

	/** First slot the CursePack may allocate. Chosen to clear the packed 474 range. */
	public static final int CURSE_BASE = 28000;

	/** Number of CursePack animation files. Held in step with CURSE_ANIM_FILES by test. */
	public static final int CURSE_COUNT = 7;

	/** First slot PAST the CursePack window (exclusive end). */
	public static final int CURSE_END = CURSE_BASE + CURSE_COUNT;

	/**
	 * Highest file id a frame KEY can address.
	 *
	 * <p>{@code Animation.anIntArray353} packs the file into the HIGH 16 bits, so
	 * {@code file << 16} has to stay a positive {@code int}: ids above this would silently
	 * produce a negative key and the frame would simply never resolve. The CursePack window
	 * (28000..28006) is comfortably inside it.
	 */
	public static final int MAX_FILE_ID = 32767;

	private FrameSlots() {
	}

	/** True for a file id the loose-numeric loader would accept. */
	public static boolean isLooseSlot(int file) {
		return file >= LOOSE_MIN_ID;
	}

	/** True for a file id inside the CursePack's own window. */
	public static boolean isCurseSlot(int file) {
		return file >= CURSE_BASE && file < CURSE_END;
	}

	/** True when the 16-bit-packed frame key can represent this file id at all. */
	public static boolean isKeyAddressable(int file) {
		return file >= 0 && file <= MAX_FILE_ID;
	}

	/**
	 * True when MORE THAN ONE writer would accept this slot's file name - i.e. the loose
	 * loader and the CursePack both can. Not an error in itself, but the case a fourth
	 * writer has to be reasoned about against.
	 */
	public static boolean isContestedByLooseLoading(int file) {
		return isLooseSlot(file) && isCurseSlot(file);
	}

	/** Human-readable range summary, for diagnostics and messages. */
	public static String describe() {
		return "loose>=" + LOOSE_MIN_ID + ", curse=[" + CURSE_BASE + "," + CURSE_END
				+ "), maxKeyable=" + MAX_FILE_ID;
	}
}
