package def;

import node.MRUNodes;import cache.StreamLoader;
import model.Model;
import net.Stream;



public final class SpotAnim {

	public static void unpackConfig(StreamLoader streamLoader) {
		Stream stream = new Stream(streamLoader.getDataForName("spotanim.dat"));
		int length = stream.readUnsignedWord();
		if (cache == null)
			cache = new SpotAnim[length];
		for (int j = 0; j < length; j++) {
			if (cache[j] == null)
				cache[j] = new SpotAnim();
			cache[j].anInt404 = j;
			cache[j].readValues(stream);
			if (j == 1247) {
				cache[j].anInt405 = 60776;
				cache[j].anInt406 = 4001;
				if (Animation.anims != null && Animation.anims.length > 4001)
					cache[j].aAnimation_407 = Animation.anims[4001];
			}
			if (j == 1248) {
				cache[j].anInt405 = 60776;
				cache[j].anInt406 = 4002;
				if (Animation.anims != null && Animation.anims.length > 4002)
					cache[j].aAnimation_407 = Animation.anims[4002];
			}
		}

	}

	public static void ensureCapacity(int size) {
		if (cache != null && cache.length >= size) {
			return;
		}
		SpotAnim[] grown = new SpotAnim[size];
		if (cache != null) {
			System.arraycopy(cache, 0, grown, 0, cache.length);
		}
		for (int i = 0; i < grown.length; i++) {
			if (grown[i] == null) {
				grown[i] = new SpotAnim();
				grown[i].anInt404 = i;
			}
		}
		cache = grown;
	}

	/**
	 * The 474 spotanim reader - reused verbatim for 667 spotanims, because the
	 * formats AGREE (Phase 6.5.3, measured rather than assumed).
	 *
	 * <p>This was previously reached through a pass-through alias named
	 * {@code readValues667}, whose name implied the 667 format differed. It does
	 * not: the real {@code CursePack/spotanim.dat} (2982 entries) uses only
	 * opcodes 0, 1, 2, 4, 5, 6, 7, 8 and 40, every one of which this reader
	 * consumes exactly as the CursePack skip path does (pinned by
	 * {@code spotAnimSkipTableMatchesThe474ReaderForEveryOpcodeItHandles}).
	 * The alias was in fact only a VISIBILITY shim: this method was private and
	 * {@link CurseData667} lives in the same package but needed to call it.
	 * It is now reachable directly instead of via a differently-named copy.
	 */
	public void readValues(Stream stream) {
		do {
			int i = stream.readUnsignedByte();
			if (i == 0) {
				return;
			}
			if (i == 1) {
				anInt405 = stream.readUnsignedWord();
			} else if (i == 2) {
				anInt406 = stream.readUnsignedWord();
				if (anInt406 == 65535) {
					anInt406 = -1;
				}
				if (Animation.anims != null && anInt406 >= 0 && anInt406 < Animation.anims.length) {
					aAnimation_407 = Animation.anims[anInt406];
				}
			} else if (i == 4) {
				anInt410 = stream.readUnsignedWord();
			} else if (i == 5) {
				anInt411 = stream.readUnsignedWord();
			} else if (i == 6) {
				anInt412 = stream.readUnsignedWord();
			} else if (i == 7) {
				anInt413 = stream.readUnsignedByte();
			} else if (i == 8) {
				anInt414 = stream.readUnsignedByte();
			} else if (i == 40) {
				int j = stream.readUnsignedByte();
				if (j > anIntArray408.length) {
					int[] grownSrc = new int[j];
					int[] grownDst = new int[j];
					System.arraycopy(anIntArray408, 0, grownSrc, 0, anIntArray408.length);
					System.arraycopy(anIntArray409, 0, grownDst, 0, anIntArray409.length);
					anIntArray408 = grownSrc;
					anIntArray409 = grownDst;
				}
				for (int k = 0; k < j; k++) {
					anIntArray408[k] = stream.readUnsignedWord();
					anIntArray409[k] = stream.readUnsignedWord();
				}
			} else {
				System.out.println("Error unrecognised spotanim config code: " + i);
			}
		} while (true);
	}

	public Model getModel() {
		Model model = (Model) aMRUNodes_415.insertFromCache(anInt404);
		if (model != null)
			return model;
		model = Model.method462(anInt405);
		if (model == null)
			return null;
		for (int i = 0; i < anIntArray408.length; i++)
			if (anIntArray408[0] != 0)
				model.method476(anIntArray408[i], anIntArray409[i]);

		aMRUNodes_415.removeFromCache(model, anInt404);
		return model;
	}

	public SpotAnim() {
		anInt400 = 9;
		anInt406 = -1;
		anIntArray408 = new int[10];
		anIntArray409 = new int[10];
		anInt410 = 128;
		anInt411 = 128;
	}

	public final int anInt400;
	public static SpotAnim cache[];
	public int anInt404;
	public int anInt405;
	public int anInt406;
	public Animation aAnimation_407;
	public int[] anIntArray408;
	public int[] anIntArray409;
	public int anInt410;
	public int anInt411;
	public int anInt412;
	public int anInt413;
	public int anInt414;
	public static MRUNodes aMRUNodes_415 = new MRUNodes(30);

}
