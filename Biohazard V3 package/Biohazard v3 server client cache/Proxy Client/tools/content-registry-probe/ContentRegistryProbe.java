import java.lang.reflect.Method;

import def.Animation;
import def.ContentRegistry;
import model.Frames;

/**
 * Verifies the content registry end to end against the REAL cache - Phase 6.5.5.
 *
 * <p><b>What it proves.</b> Anims 4000, 4001 and 4002 are written by hand in
 * {@code Animation.java} and their frame literals decode to source files 3403 and 3353.
 * Neither the packed 474 archive (ids 0..3229) nor the loose cache-root loader (1777,
 * 2160, 3502) supplied those files, so {@code Frames.method531} returned null and the
 * animations rendered with no frames. The registry now declares and loads them, and this
 * probe checks EVERY frame of every declared animation actually resolves.
 *
 * <p><b>Why it runs the CONTROL first.</b> "All frames resolve" is only meaningful if they
 * did NOT resolve before the load. The control resolves the same frames with the registry's
 * load suppressed, so a probe that silently did nothing cannot report success - the failure
 * mode that let this defect live in the first place.
 *
 * <p><b>Why it reads the frames off the real animations</b> rather than the declaration:
 * the declaration is the CLAIM and the animation data is the FACT. Checking the fact is the
 * point - a probe that re-read the declaration would only prove it agrees with itself.
 *
 * <p>Deliberately non-hermetic: it needs the real cache and the deployed 667Anims pack, so
 * it is a gate tool that must be RUN, not a unit test.
 */
public class ContentRegistryProbe {

	public static void main(String[] args) throws Exception {
		Frames.loadFrames();
		buildHandWrittenAnims();

		ContentRegistry.init();

		System.out.println("declared sources: " + ContentRegistry.FRAME_SOURCES.length);
		System.out.println();

		int totalFrames = 0;
		int resolvedBefore = 0;
		int resolvedAfter = 0;
		int unusableAfter = 0;
		String firstUnresolved = null;

		// ---- CONTROL: resolve with NOTHING loaded, so the fix has to earn its result.
		for (int i = 0; i < ContentRegistry.FRAME_SOURCES.length; i++) {
			ContentRegistry.FrameSource s = ContentRegistry.FRAME_SOURCES[i];
			for (int k = 0; k < s.requiredByAnimIds.length; k++) {
				Animation a = anim(s.requiredByAnimIds[k]);
				if (a == null || a.anIntArray353 == null) {
					continue;
				}
				for (int f = 0; f < a.anIntArray353.length; f++) {
					totalFrames++;
					if (Frames.method531(a.anIntArray353[f]) != null) {
						resolvedBefore++;
					}
				}
			}
		}

		int loaded = ContentRegistry.loadAll();

		for (int i = 0; i < ContentRegistry.FRAME_SOURCES.length; i++) {
			ContentRegistry.FrameSource s = ContentRegistry.FRAME_SOURCES[i];
			for (int k = 0; k < s.requiredByAnimIds.length; k++) {
				Animation a = anim(s.requiredByAnimIds[k]);
				if (a == null || a.anIntArray353 == null) {
					continue;
				}
				for (int f = 0; f < a.anIntArray353.length; f++) {
					int key = a.anIntArray353[f];
					if (Frames.method531(key) != null) {
						resolvedAfter++;
					} else {
						unusableAfter++;
						if (firstUnresolved == null) {
							firstUnresolved = "anim " + s.requiredByAnimIds[k] + " frame " + f
									+ " (key " + key + " -> file " + (key >>> 16)
									+ " idx " + (key & 0xffff) + ")";
						}
					}
				}
			}
		}

		System.out.println("sources loaded            : " + loaded + "/"
				+ ContentRegistry.FRAME_SOURCES.length);
		System.out.println("declared anim frames      : " + totalFrames);
		System.out.println("resolved BEFORE the load  : " + resolvedBefore + " / " + totalFrames
				+ "   (control - must be 0)");
		System.out.println("resolved AFTER the load   : " + resolvedAfter + " / " + totalFrames);
		if (firstUnresolved != null) {
			System.out.println("first unresolved off. load: " + firstUnresolved);
		}

		String validation = ContentRegistry.validate();
		System.out.println("declaration drift report  : "
				+ (validation.length() == 0 ? "clean" : validation.trim()));
		System.out.println();

		boolean controlHeld = resolvedBefore == 0;
		boolean everythingResolved = resolvedAfter == totalFrames && totalFrames > 0;
		boolean declarationHolds = validation.length() == 0;

		if (!controlHeld) {
			System.out.println("CONTROL FAILED - frames already resolved before the load, so this"
					+ " probe cannot attribute the result to the registry.");
		}
		if (!declarationHolds) {
			System.out.println("DECLARATION DRIFTED - see the report above.");
		}
		if (!everythingResolved) {
			System.out.println("UNRESOLVED FRAMES - " + unusableAfter + " frame(s) still null after"
					+ " the load; the declared sources do not carry every index the animations use.");
		}

		boolean ok = controlHeld && everythingResolved && declarationHolds;
		System.out.println(ok ? "CONTENT_REGISTRY_OK" : "CONTENT_REGISTRY_FAIL");
		System.out.println(ok
				? "OK: every frame of the declared animations resolves once the registry loads its sources."
				: "FAIL: see the lines above.");
		System.exit(ok ? 0 : 1);
	}

	private static Animation anim(int id) {
		if (Animation.anims == null || id < 0 || id >= Animation.anims.length) {
			return null;
		}
		return Animation.anims[id];
	}

	/**
	 * Rebuilds Animation.anims the way the client's {@code Animation.unpackConfig} does,
	 * minus the cache read: ensureCapacity fills the slots, then the three hand-written
	 * writers populate them. They are private, so this is done by reflection - the same
	 * technique the other probes in this repo use.
	 */
	private static void buildHandWrittenAnims() throws Exception {
		Animation.ensureCapacity(9000);
		String[] writers = { "applyDeathlyExtraAnims", "applyBorkAnims", "applyBloodReaverAnims" };
		for (int i = 0; i < writers.length; i++) {
			Method m = Animation.class.getDeclaredMethod(writers[i]);
			m.setAccessible(true);
			m.invoke(null);
		}
	}

	private ContentRegistryProbe() {
	}
}
