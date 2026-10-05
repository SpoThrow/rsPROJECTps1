package def;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;

import model.FrameSlots;
import model.Frames;
import sign.signlink;

/**
 * The single owner of EXTERNALLY-SOURCED frame content (Phase 6.5.5).
 *
 * <p><b>Why this exists.</b> {@link Animation} writes 25 animations by hand, and each
 * frame literal packs its source FILE id into the high 16 bits
 * ({@code frame = file << 16 | index}). A hand-written animation's dependency on an
 * external frame file is therefore a MAGIC NUMBER buried inside its own data - which is
 * exactly why three of those dependencies went unnoticed for so long:
 *
 * <p>Anims <b>4000, 4001 and 4002</b> need frame files <b>3403</b> and <b>3353</b>, and
 * NOTHING supplied them. The packed 474 archive stops at id 3229 (measured from the real
 * {@code Frames.dat}) and the cache root holds only the loose 1777, 2160 and 3502 - so
 * {@link Frames#method531} returned {@code null} and those animations rendered with NO
 * FRAMES AT ALL. Anims 4001 and 4002 are driven by spotanims 1247 and 1248, so this was
 * reachable graphics, not dead data.
 *
 * <p>This class is the one place that <b>declares</b> each such source - (slot, revision,
 * asset, the animations that need it) - and the one place that <b>loads</b> them. A future
 * writer with external frame content has somewhere to declare itself, and a missing or
 * drifting source becomes a REPORTED condition instead of silence.
 *
 * <p><b>Slot safety is asserted, not assumed.</b> The declared slots (3353, 3403) sit
 * ABOVE the packed 474 range (0..3229) and OUTSIDE the CursePack window
 * ({@link FrameSlots#CURSE_BASE}..{@link FrameSlots#CURSE_END}-1), so loading them cannot
 * overwrite existing content. {@code ContentRegistryDeclarationsAreSafe} pins that.
 *
 * <p><b>Layout</b>, mirroring {@link CurseData667}'s CursePack convention:
 * <pre>
 *  {cache}/667Anims/
 *    anims/3353.gz
 *    anims/3403.gz
 * </pre>
 *
 * <p>The files are the 667 revision's own animation files, so they need no remap: their
 * ids are already outside the packed range. That is why this loader can write them at
 * their OWN ids while {@code CurseData667} has to offset its files into a high window -
 * its source ids (2998..3020) sit INSIDE the packed range and would have collided.
 */
public final class ContentRegistry {

	/** Source revision a declared file comes from. */
	public static final int REVISION_667 = 667;

	/** Pack folder under the cache root (mirrors {@code CurseData667.PACK_FOLDER}). */
	public static final String PACK_FOLDER = "667Anims";

	/**
	 * One externally-sourced frame file: where it lands, where it comes from, and which
	 * hand-written animations would break without it.
	 */
	public static final class FrameSource {
		/** Target slot in {@link Frames#animationlist}, addressed by frame keys. */
		public final int slot;
		/** Source revision this file belongs to. */
		public final int revision;
		/** Path relative to the pack root. */
		public final String asset;
		/** Anim ids in {@link Animation#anims} whose frames decode to {@link #slot}. */
		public final int[] requiredByAnimIds;

		FrameSource(int slot, int revision, String asset, int[] requiredByAnimIds) {
			this.slot = slot;
			this.revision = revision;
			this.asset = asset;
			this.requiredByAnimIds = requiredByAnimIds;
		}
	}

	/**
	 * Every externally-sourced frame file, in one place.
	 *
	 * <p>Derived by decoding the {@code frame >> 16} of every hand-written animation, then
	 * subtracting what the packed 474 archive and the loose cache-root loader already
	 * supply. What remained - 3403 and 3353 - is this list.
	 */
	public static final FrameSource[] FRAME_SOURCES = {
		new FrameSource(3403, REVISION_667, "anims/3403.gz", new int[] { 4000 }),
		new FrameSource(3353, REVISION_667, "anims/3353.gz", new int[] { 4001, 4002 }),
	};

	/** Pack root; null when the pack is absent. Public so tests can aim it at a fixture. */
	public static File root;
	/** True when a usable pack root was found. */
	public static boolean active;

	private ContentRegistry() {
	}

	public static void init() {
		root = findRoot();
		active = root != null;
		if (active) {
			System.out.println("667Anims: " + root.getAbsolutePath());
		} else {
			System.out.println("667Anims pack not found under cache;"
					+ " anims 4000/4001/4002 will have no frames.");
		}
	}

	/** Only searches {@code {findcachedir()}/667Anims}. */
	public static File findRoot() {
		File dir = new File(signlink.findcachedir(), PACK_FOLDER);
		try {
			dir = dir.getCanonicalFile();
		} catch (Exception e) {
		}
		if (!dir.isDirectory()) {
			return null;
		}
		return dir;
	}

	/**
	 * Loads every declared source into its declared slot.
	 *
	 * <p>A declared-but-unreadable source is REPORTED rather than skipped silently - that
	 * silence is what let this defect live.
	 *
	 * @return the number of sources successfully loaded
	 */
	public static int loadAll() {
		if (root == null) {
			return 0;
		}
		int loaded = 0;
		for (int i = 0; i < FRAME_SOURCES.length; i++) {
			FrameSource source = FRAME_SOURCES[i];
			File file = new File(root, source.asset);
			if (!file.isFile()) {
				System.out.println("667Anims source missing: " + source.asset
						+ " - anims " + idsToString(source.requiredByAnimIds) + " will have no frames.");
				continue;
			}
			byte[] data = readGzip(file);
			if (data == null || data.length == 0) {
				System.out.println("667Anims source unreadable: " + source.asset);
				continue;
			}
			Frames.load(source.slot, data);
			loaded++;
		}
		return loaded;
	}

	/**
	 * Checks that every declared dependency still holds, and returns a report.
	 *
	 * <p>This is the drift guard for the declaration: it reads the REAL frames of each
	 * declared animation and asserts they decode to the declared slot, so editing an
	 * animation's frames to point somewhere else - or repointing a source at the wrong
	 * slot - surfaces here rather than as missing frames in game.
	 *
	 * @return an empty string when every declaration holds, otherwise one line per problem
	 */
	public static String validate() {
		StringBuilder report = new StringBuilder();
		for (int i = 0; i < FRAME_SOURCES.length; i++) {
			FrameSource source = FRAME_SOURCES[i];
			for (int k = 0; k < source.requiredByAnimIds.length; k++) {
				int animId = source.requiredByAnimIds[k];
				Animation anim = null;
				if (Animation.anims != null && animId >= 0 && animId < Animation.anims.length) {
					anim = Animation.anims[animId];
				}
				if (anim == null) {
					report.append("declared anim ").append(animId)
							.append(" is absent from Animation.anims\n");
					continue;
				}
				if (anim.anIntArray353 == null) {
					report.append("declared anim ").append(animId).append(" has no frames\n");
					continue;
				}
				for (int f = 0; f < anim.anIntArray353.length; f++) {
					int file = anim.anIntArray353[f] >>> 16;
					if (file != source.slot) {
						report.append("declared anim ").append(animId)
								.append(" frame ").append(f)
								.append(" points at file ").append(file)
								.append(", not declared slot ").append(source.slot).append('\n');
						break;
					}
				}
			}
		}
		return report.toString();
	}

	/** True when the declared source's slot currently holds decoded frames. */
	public static boolean isSourceLoaded(FrameSource source) {
		return Frames.fileFrameCount(source.slot) > 0;
	}

	private static String idsToString(int[] ids) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ids.length; i++) {
			if (i > 0) {
				sb.append('/');
			}
			sb.append(ids[i]);
		}
		return sb.toString();
	}

	private static byte[] readGzip(File file) {
		FileInputStream fis = null;
		GZIPInputStream gis = null;
		try {
			fis = new FileInputStream(file);
			gis = new GZIPInputStream(fis, 8192);
			ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length() * 4);
			byte[] buf = new byte[8192];
			int n;
			while ((n = gis.read(buf, 0, buf.length)) > 0) {
				bos.write(buf, 0, n);
			}
			return bos.toByteArray();
		} catch (Exception e) {
			return null;
		} finally {
			try {
				if (gis != null) {
					gis.close();
				} else if (fis != null) {
					fis.close();
				}
			} catch (Exception e) {
			}
		}
	}
}
