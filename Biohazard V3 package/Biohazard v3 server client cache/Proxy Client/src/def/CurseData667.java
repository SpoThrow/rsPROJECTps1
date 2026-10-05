package def;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;

import sign.signlink;import cache.FileOperations;
import model.FrameSlots;
import model.Frames;
import model.Model;
import net.Stream;



/**
 * Selectively injects Ancient Curse animations/graphics from the cache
 * {@code CursePack} folder (under {@link signlink#findcachedir()}) without
 * replacing OG 474 seq/spotanim/maps/items/npcs.
 *
 * <p>Expected layout:
 * <pre>
 *  {cache}/CursePack/
 *    seq.dat
 *    spotanim.dat
 *    anims/*.gz
 *    models/*.gz
 * </pre>
 *
 * Curse frame files are remapped into high unused Class36 slots so they cannot
 * overwrite whip / godsword / run frames that share the original 667 file IDs.
 */
public final class CurseData667 {

	// The content this class injects - ids, asset names and target slots - is declared
	// in ONE place (Phase 6.5.6): see def.ContentRegistry. This class keeps the
	// MECHANISM (how to walk an unwanted entry, how to remap a frame, when to load),
	// not the DECLARATION. Before 6.5.6 the id arrays lived here and the bounds lived
	// in model.FrameSlots, so "what do we inject?" had two answers.

	public static File root;
	public static boolean active;

	public static void init() {
		root = findRoot();
		active = root != null;
		if (active) {
			System.out.println("CursePack: " + root.getAbsolutePath());
		} else {
			System.out.println("CursePack not found under cache; curse anims/gfx may be missing.");
		}
	}

	/** Only searches {@code {findcachedir()}/CursePack}. */
	public static File findRoot() {
		File dir = new File(signlink.findcachedir(), ContentRegistry.CURSE_PACK_FOLDER);
		try {
			dir = dir.getCanonicalFile();
		} catch (Exception e) {
		}
		if (!dir.isDirectory()) {
			return null;
		}
		if (!new File(dir, ContentRegistry.ASSET_SEQ).isFile() || !new File(dir, ContentRegistry.ASSET_SPOTANIM).isFile()) {
			return null;
		}
		return dir;
	}

	private static File asset(String relativePath) {
		if (root == null) {
			return null;
		}
		File file = new File(root, relativePath);
		return file.isFile() ? file : null;
	}

	/** Call after OG Animation + SpotAnim unpack. */
	public static void inject() {
		if (!active || root == null) {
			return;
		}
		loadCurseModels();
		injectSequences();
		injectSpotAnims();
		preloadRemappedAnimFiles();
		relinkSpotAnimSequences();
		System.out.println("Injected curse seq/gfx from CursePack (frames remapped)");
	}

	private static void relinkSpotAnimSequences() {
		if (SpotAnim.cache == null) {
			return;
		}
		for (int i = 0; i < ContentRegistry.CURSE_GFX_IDS.length; i++) {
			int id = ContentRegistry.CURSE_GFX_IDS[i];
			if (id < 0 || id >= SpotAnim.cache.length || SpotAnim.cache[id] == null) {
				continue;
			}
			int animId = SpotAnim.cache[id].anInt406;
			if (animId >= 0 && Animation.anims != null && animId < Animation.anims.length) {
				SpotAnim.cache[id].aAnimation_407 = Animation.anims[animId];
			}
		}
	}

	/**
	 * Only serves remapped curse frame slots. Never hijacks OG file IDs.
	 */
	public static boolean loadAnimationFile(int id) {
		if (!FrameSlots.isCurseSlot(id)) {
			return false;
		}
		int local = id - FrameSlots.CURSE_BASE;
		if (local >= ContentRegistry.CURSE_ANIM_FILES.length) {
			return false;
		}
		return loadIntoSlot(ContentRegistry.CURSE_ANIM_FILES[local], id);
	}

	private static int remapFile(int originalFile) {
		for (int i = 0; i < ContentRegistry.CURSE_ANIM_FILES.length; i++) {
			if (ContentRegistry.CURSE_ANIM_FILES[i] == originalFile) {
				return FrameSlots.CURSE_BASE + i;
			}
		}
		return originalFile;
	}

	private static void remapSequenceFrames(Animation anim) {
		if (anim == null || anim.anIntArray353 == null) {
			return;
		}
		for (int i = 0; i < anim.anIntArray353.length; i++) {
			int frame = anim.anIntArray353[i];
			if (frame <= 0) {
				continue;
			}
			int file = frame >>> 16;
			int idx = frame & 0xffff;
			int mapped = remapFile(file);
			if (mapped != file) {
				anim.anIntArray353[i] = (mapped << 16) | idx;
			}
		}
	}

	private static void preloadRemappedAnimFiles() {
		for (int i = 0; i < ContentRegistry.CURSE_ANIM_FILES.length; i++) {
			loadIntoSlot(ContentRegistry.CURSE_ANIM_FILES[i], FrameSlots.CURSE_BASE + i);
		}
	}

	private static boolean loadIntoSlot(int sourceFileId, int destSlot) {
		File file = asset(ContentRegistry.ASSET_ANIMS + File.separator + sourceFileId + ".gz");
		if (file == null) {
			return false;
		}
		byte[] data = readGzip(file);
		if (data == null || data.length == 0) {
			return false;
		}
		Frames.load(destSlot, data);
		return true;
	}

	private static void loadCurseModels() {
		int loaded = 0;
		for (int i = 0; i < ContentRegistry.CURSE_MODEL_IDS.length; i++) {
			File file = asset(ContentRegistry.ASSET_MODELS + File.separator + ContentRegistry.CURSE_MODEL_IDS[i] + ".gz");
			if (file == null) {
				System.out.println("CursePack model missing: " + ContentRegistry.CURSE_MODEL_IDS[i]);
				continue;
			}
			byte[] data = readGzip(file);
			if (data != null && data.length > 0) {
				Model.method460(data, ContentRegistry.CURSE_MODEL_IDS[i]);
				loaded++;
			}
		}
		System.out.println("CursePack models loaded: " + loaded + "/" + ContentRegistry.CURSE_MODEL_IDS.length);
	}

	private static void injectSequences() {
		File seqFile = asset(ContentRegistry.ASSET_SEQ);
		if (seqFile == null) {
			return;
		}
		byte[] data = FileOperations.ReadFile(seqFile.getPath());
		if (data == null || data.length < 2) {
			return;
		}
		Stream stream = new Stream(data);
		int length = stream.readUnsignedWord();
		Animation.ensureCapacity(Math.max(length, 12600));
		for (int j = 0; j < length; j++) {
			boolean wanted = false;
			for (int k = 0; k < ContentRegistry.CURSE_SEQ_IDS.length; k++) {
				if (ContentRegistry.CURSE_SEQ_IDS[k] == j) {
					wanted = true;
					break;
				}
			}
			if (wanted) {
				if (Animation.anims[j] == null) {
					Animation.anims[j] = new Animation();
				}
				// Phase 6.5.3: the 474 reader IS the 667 reader for every opcode the pack
				// actually uses - see skipSequence() for the one recorded divergence (12).
				Animation.anims[j].readValues(stream);
				remapSequenceFrames(Animation.anims[j]);
				// Slow Wrath cloud slightly so telegraph is readable
				if (j == 12580) {
					slowSequence(Animation.anims[j], 5, 4); // *1.25
				}
			} else {
				skipSequence(stream);
			}
		}
	}

	private static void slowSequence(Animation anim, int mul, int div) {
		if (anim == null || anim.anIntArray355 == null || mul <= 0 || div <= 0) {
			return;
		}
		for (int i = 0; i < anim.anIntArray355.length; i++) {
			int d = anim.anIntArray355[i];
			if (d > 0) {
				int next = (d * mul) / div;
				anim.anIntArray355[i] = next < 1 ? 1 : next;
			}
		}
	}

	/**
	 * Walks one UNWANTED sequence, consuming exactly the bytes its definition occupies.
	 *
	 * <p>Phase 6.5.3 - why this is a hand-written walk rather than a second call to
	 * {@link Animation#readValues}: the readers ALLOCATE per entry (opcode 1 builds three
	 * int[] of length n), so reusing them to walk the ~15k unwanted entries of the pack
	 * would churn garbage for data that is immediately discarded. This walk exists for
	 * that reason, not by neglect.
	 *
	 * <p>The cost of that choice is a SECOND copy of the opcode knowledge, which is
	 * exactly the kind of duplication Phase 6.5.3 exists to make visible and bounded.
	 * The two tables are therefore pinned against each other opcode by opcode in the
	 * harness ({@code seqSkipTableMatchesThe474ReaderForEveryOpcodeItHandles}), and
	 * there is exactly ONE known divergence, deliberately recorded rather than tidied:
	 *
	 * <p><b>Opcode 12.</b> This walk consumes one byte for it; {@link Animation#readValues}
	 * has NO case for 12 and consumes nothing, printing "Unrecognized seq.dat config
	 * code: 12". That is a real difference in where the cursor lands, so it is latent
	 * only as long as no entry uses opcode 12 - which was MEASURED against the real pack
	 * (15371 entries, 2516296 bytes): the opcodes actually present are
	 * 0,1,2,3,5,6,7,8,9,10,11, with opcode 12 occurring ZERO times, and no opcode above
	 * 11 occurring at all. Both tables walk the file to exactly EOF with zero bad
	 * opcodes. Removing this case, or "tidying" it to match the reader, would therefore
	 * change nothing today and would silently break the pack the day it does use 12.
	 */
	private static void skipSequence(Stream stream) {
		do {
			int i = stream.readUnsignedByte();
			if (i == 0) {
				return;
			}
			if (i == 1) {
				int n = stream.readUnsignedWord();
				for (int k = 0; k < n; k++) {
					stream.readDWord();
				}
				for (int k = 0; k < n; k++) {
					stream.readUnsignedByte();
				}
			} else if (i == 2) {
				stream.readUnsignedWord();
			} else if (i == 3) {
				int k = stream.readUnsignedByte();
				for (int l = 0; l < k; l++) {
					stream.readUnsignedByte();
				}
			} else if (i == 4) {
				// flag
			} else if (i == 5 || i == 8 || i == 9 || i == 10 || i == 11 || i == 12) {
				stream.readUnsignedByte();
			} else if (i == 6 || i == 7) {
				stream.readUnsignedWord();
			} else {
				return;
			}
		} while (true);
	}

	private static void injectSpotAnims() {
		File spotFile = asset(ContentRegistry.ASSET_SPOTANIM);
		if (spotFile == null) {
			return;
		}
		byte[] data = FileOperations.ReadFile(spotFile.getPath());
		if (data == null || data.length < 2) {
			return;
		}
		Stream stream = new Stream(data);
		int length = stream.readUnsignedWord();
		SpotAnim.ensureCapacity(Math.max(length, 2300));
		for (int j = 0; j < length; j++) {
			boolean wanted = false;
			for (int k = 0; k < ContentRegistry.CURSE_GFX_IDS.length; k++) {
				if (ContentRegistry.CURSE_GFX_IDS[k] == j) {
					wanted = true;
					break;
				}
			}
			if (wanted) {
				if (SpotAnim.cache[j] == null) {
					SpotAnim.cache[j] = new SpotAnim();
				}
				SpotAnim.cache[j].anInt404 = j;
				SpotAnim.cache[j].readValues(stream);
			} else {
				skipSpotAnim(stream);
			}
		}
	}

	/**
	 * Walks one UNWANTED spotanim, consuming exactly the bytes its definition occupies.
	 *
	 * <p>Same trade-off as {@link #skipSequence} (the readers allocate, so unwanted
	 * entries are walked instead of decoded), but with a better outcome: unlike the seq
	 * table, this one has NO divergence from {@link SpotAnim#readValues}. Both consume
	 * opcodes 1, 2, 4, 5 and 6 as a word, 7 and 8 as a byte, and 40 as a count followed
	 * by that many (word, word) pairs. That is pinned by
	 * {@code spotAnimSkipTableMatchesThe474ReaderForEveryOpcodeItHandles}, and it is
	 * consistent with the measured pack: {@code spotanim.dat} (2982 entries) uses only
	 * opcodes 0, 1, 2, 4, 5, 6, 7, 8 and 40.
	 */
	private static void skipSpotAnim(Stream stream) {
		do {
			int i = stream.readUnsignedByte();
			if (i == 0) {
				return;
			}
			if (i == 1 || i == 2 || i == 4 || i == 5 || i == 6) {
				stream.readUnsignedWord();
			} else if (i == 7 || i == 8) {
				stream.readUnsignedByte();
			} else if (i == 40) {
				int j = stream.readUnsignedByte();
				for (int k = 0; k < j; k++) {
					stream.readUnsignedWord();
					stream.readUnsignedWord();
				}
			} else {
				return;
			}
		} while (true);
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

	private CurseData667() {
	}
}
