import java.io.File;
import java.io.RandomAccessFile;

import cache.Decompressor;
import cache.StreamLoader;
import def.Animation;

/**
 * Checks whether the chat-head animation ids the server sends can actually animate, against the
 * REAL seq.dat.
 *
 * <p><b>The defect.</b> NPC chat heads are drawn as a live model with an animation id on it, and
 * the server sends those ids from {@code PlayerAssistant.chatheadEmote}: every one of the 588..617
 * head emotes is folded onto one of six ids in 9760..9850. seq.dat does not define anything above
 * the packed range (a few thousand), so a head asked for one of those six has no frames at all and
 * freezes on its base model - which is exactly what "the chat heads stopped animating" looks like.
 *
 * <p><b>What it checks.</b> Two things that cannot be known from the source: that the six ids the
 * server sends really are frameless in this cache (the CONTROL - if they were not, the translation
 * would be beside the point), and that the six emotes the client translates them back onto really
 * do have frames. A translation table that pointed at another frameless id would look like a fix
 * in the source and animate nothing in play.
 *
 * <p><b>Why it is a probe and not a unit test.</b> The answer is a fact about seq.dat and no
 * hermetic test supplies a cache. Like {@code MakeXProbe}, this is a gate tool that must be RUN.
 *
 * <p>Exit code 0 = HEAD_ANIM_OK, 1 = a check failed or the cache could not be read.
 */
public class HeadAnimProbe {

	/** The six ids the server's chatheadEmote can send. See PlayerAssistant.chatheadEmote. */
	private static final int[] SERVER_HEAD_IDS = { 9760, 9840, 9843, 9844, 9847, 9850 };

	/** The client's own head emotes, the range the six above are folded down from. */
	private static final int EMOTE_FIRST = 588;
	private static final int EMOTE_LAST = 617;

	private static int passed;
	private static int failed;

	public static void main(String[] args) throws Exception {
		String cache = findCache();
		if (cache == null) {
			System.out.println("CACHE_NOT_FOUND");
			System.exit(1);
		}
		System.out.println("cache root: " + cache);

		RandomAccessFile data = null;
		RandomAccessFile index = null;
		byte[] config;
		try {
			data = new RandomAccessFile(new File(cache, "main_file_cache.dat"), "r");
			index = new RandomAccessFile(new File(cache, "main_file_cache.idx0"), "r");
			config = new Decompressor(data, index, 1).decompress(2);
		} finally {
			if (data != null) data.close();
			if (index != null) index.close();
		}
		if (config == null) {
			System.out.println("CONFIG_UNREADABLE");
			System.exit(1);
		}

		Animation.unpackConfig(new StreamLoader(config, "config"));

		int packed = 0;
		for (int i = 0; i < Animation.anims.length; i++) {
			Animation a = Animation.anims[i];
			if (a != null && a.hasFrames()) {
				packed++;
			}
		}
		System.out.println("anims[] length            : " + Animation.anims.length
				+ "   (framed: " + packed + ")");
		System.out.println("head emote ids the server sends: " + SERVER_HEAD_IDS.length);
		System.out.println();

		// CONTROL - the ids the server sends carry no frames, so the head cannot animate on them.
		int framedServerIds = 0;
		for (int i = 0; i < SERVER_HEAD_IDS.length; i++) {
			Animation a = anim(SERVER_HEAD_IDS[i]);
			boolean framed = a != null && a.hasFrames();
			if (framed) {
				framedServerIds++;
			}
		}
		check("control: none of the server's " + SERVER_HEAD_IDS.length
				+ " head ids carries frames in seq.dat (" + framedServerIds + " did)", framedServerIds == 0);

		// The capacity covers the whole head range, so an id in it resolves to an Animation at all
		// rather than to null. Null is handled too, but a covered id is the case the render sees.
		check("capacity covers the head range: anims.length > " + Animation.HEAD_EMOTE_LAST,
				Animation.anims.length > Animation.HEAD_EMOTE_LAST);

		// The translation: each server id maps away from itself and onto a framed emote.
		boolean translated = true;
		boolean framed = true;
		for (int i = 0; i < SERVER_HEAD_IDS.length; i++) {
			int id = SERVER_HEAD_IDS[i];
			int emote = Animation.headEmote(id);
			translated &= emote != id;
			Animation a = anim(emote);
			framed &= a != null && a.hasFrames();
			System.out.println("  " + id + " -> " + emote + "  ("
					+ (a != null && a.hasFrames() ? a.anInt352 + " frames" : "NO FRAMES") + ")");
		}
		check("every head id the server sends translates off itself", translated);
		check("every translated emote has frames to animate with", framed);

		// The whole 588..617 range is what the server folds: a client-side translation only has to
		// name one representative per group, so this is the map telling the truth about its groups.
		int emotesFramed = 0;
		for (int emote = EMOTE_FIRST; emote <= EMOTE_LAST; emote++) {
			Animation a = anim(emote);
			if (a != null && a.hasFrames()) {
				emotesFramed++;
			}
		}
		check("the client's own " + EMOTE_FIRST + ".." + EMOTE_LAST + " head emotes have frames ("
				+ emotesFramed + "/" + (EMOTE_LAST - EMOTE_FIRST + 1) + ")", emotesFramed > 0);

		// And it leaves everything else alone: a translation that rewrote an ordinary animation id
		// would silently change what players see for no reason at all.
		check("an unrelated id is handed back untouched", Animation.headEmote(5292) == 5292
				&& Animation.headEmote(-1) == -1 && Animation.headEmote(0) == 0);

		System.out.println();
		System.out.println("passed: " + passed + "   failed: " + failed);
		System.out.println(failed == 0 ? "HEAD_ANIM_OK" : "HEAD_ANIM_FAIL");
		System.exit(failed == 0 ? 0 : 1);
	}

	private static Animation anim(int id) {
		if (Animation.anims == null || id < 0 || id >= Animation.anims.length) {
			return null;
		}
		return Animation.anims[id];
	}

	private static void check(String what, boolean ok) {
		if (ok) {
			passed++;
			System.out.println("  PASS  " + what);
		} else {
			failed++;
			System.out.println("  FAIL  " + what);
		}
	}

	private static String findCache() {
		String home = System.getProperty("user.home");
		String[] candidates = {
				"Biohazard.474",
				"Documents" + File.separator + "GitHub" + File.separator + "Biohazard.474",
				"Biohazard.474.pre-deathly",
		};
		for (int i = 0; i < candidates.length; i++) {
			File f = new File(home, candidates[i]);
			if (f.isDirectory() && new File(f, "main_file_cache.dat").isFile()) {
				return f.getPath();
			}
		}
		return null;
	}

	private HeadAnimProbe() {
	}
}
