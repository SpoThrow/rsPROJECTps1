import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import cache.StreamLoader;
import def.Animation;
import def.CurseData667;
import def.EntityDef;
import def.Flo;
import def.IDK;
import def.ItemDef;
import def.ObjectDef;
import def.VarBit;
import def.Varp;
import model.FrameSlots;
import model.Frames;
import model.Model;
import model.Texture;
import net.PacketTap;
import net.Stream;
import scene.CollisionMap;
import scene.Fog;
import scene.ObjectManager;
import scene.WorldController;
import ui.DrawingArea;
import ui.GpuFloatBuffer;
import ui.GpuIntBuffer;
import ui.GpuRenderer;
import ui.RSImageProducer;
import ui.RendererConfig;
import ui.SceneRasterizer;
import ui.Sprite;



/**
 * Headless self-check harness for the Soul-Trail client - Phase 0.4 of
 * CLIENT_REFACTORING_PLAN.md.
 *
 * <p><b>Why this exists.</b> The client has no test infrastructure of any kind: no
 * {@code *Test.java}, no build file, no CI. {@code Compile.bat} only compiles. That makes every
 * later phase unverifiable except by playing the game - and a refactor verified only by "it looked
 * fine when I walked around" is how a client breaks silently. This file proves the *cheapest* of the
 * two options the plan names: that the client's pure-logic classes can be driven, and asserted
 * against, with no display, no server and no game loop.
 *
 * <p><b>It deliberately does not use JUnit.</b> The client has no dependency resolution at all, and
 * adding a test framework before Phase 1's build exists would put the cart before the horse. A
 * plain {@code main} that exits non-zero on failure is enough to gate a phase, and Phase 1 can
 * formalise it once Gradle is in.
 *
 * <p><b>What it covers.</b> {@link Stream} - the byte reader/writer every packet and every cache
 * read goes through, and therefore the one class where a silent bug corrupts everything downstream.
 * These are round-trip and byte-order assertions against the *real* implementation, so they pin
 * current behaviour rather than my idea of it.
 *
 * <p><b>ASCII only, on purpose.</b> {@code Compile.bat} passes no {@code -encoding}, so javac uses
 * the platform default (Cp1252 on this machine) and any non-ASCII character in a client source file
 * is a compile error. The first version of this file failed to compile for exactly that reason, so
 * the rule is recorded here: keep client sources ASCII.
 *
 * <p>Run with {@code RunTests.bat}. ⚠️ <b>Classes come from the Gradle build output
 * ({@code build/classes/java/main}), not from {@code bin/}.</b> That distinction matters:
 * {@code installBin} copies build output into {@code bin/}, and {@code bin/} is what
 * {@code Run.bat} actually plays. So a green harness proves the <em>built</em> code is
 * correct, and says nothing about {@code bin/} until {@code installBin} has run - the two
 * can silently differ, which would mean testing one thing and playing another.
 */
public final class ClientHarness {

	private static int passed;
	private static int failed;

	public static void main(String[] args) {
		System.out.println("Soul-Trail client harness (Phase 0.4)");
		System.out.println("=====================================");

		streamWordRoundTrip();
		streamWordIsBigEndian();
		streamDWordRoundTrip();
		streamDWordHighBitSurvives();
		streamUnsignedAndSignedByte();
		streamThreeByteRead();
		streamStringRoundTrip();
		streamSignedWordSignExtends();
		streamG2G4Aliases();
		streamBitAccess();
		streamOutOfRangeReadIsSilentAndWrong();
		rasterFramebufferHashIsPinned();
		rasterFramebufferHashIsDeterministic();
		rasterFramebufferHashDetectsAChangedDrawOp();
		triangleRasteriserIsPinned();
		triangleRasteriserIsDeterministic();
		triangleRasteriserDetectsAChangedTriangle();
		texturedTriangleRasteriserIsPinned();
		texturedTriangleRasteriserIsDeterministic();
		texturedTriangleRasteriserDetectsAChangedTriangle();
		sceneRasterizerIsInertByDefault();
		sceneRasterizerInterceptsWhenInstalled();
		groundSeamIsInertByDefault();
		groundSeamInterceptsWhenInstalled();
		gpuRendererIsInertByDefault();
		gpuRendererWiresBothSeams();
		decliningRendererFallsThrough();
		rendererSettingIsItsOwnKey();
		rasterPinsAreStanding();
		modelGeometryExposesLocalVertices();
		modelGeometryDecodesRenderTypeAndTexture();
		modelGeometryTextureIdComesFromTheColourSlot();
		modelGeometryHandlesAbsentAttributes();
		modelGeometryTextureArraysAreIndexedByTextureNotFace();
		gpuBufferReusesStorageAfterClear();
		gpuBufferGrowsAndPreservesContents();
		gpuBufferEnsureCapacityIsMonotonic();
		gpuBufferFailsFastOnBadSizesAndIndices();
		gpuBufferBulkCopyHonoursTheWindow();
		gpuIntBufferMirrorsTheFloatBuffer();
		realModelExportMatchesTheParsedFormat();
		realModelExportIndicesAreInRangeForTheRasteriser();
		realModelExportDecodeMatchesTheRasteriser();
		realModelExportDrivesTheRasteriser();
		curseFrameRemapRedirectsCurseFilesToTheHighSlots();
		curseFrameRemapLeavesOriginalFrameSlotsAlone();
		curseAnimationLoaderRefusesSlotsOutsideTheRemapRange();
		curseFrameSlotsCannotCollideWithTheOriginalRange();
		animationFrameTranslateTransformIsExact();
		animationFrameScaleTransformIsExact();
		animationFrameTransformLeavesTheSourceModelUntouched();
		animationFrameTransformCompoundsOnTheSameModel();
		animationFrameTransformIsDeterministic();
		sceneSeamIsOfferedTheTransformedVerticesNotTheRestPose();
		frameSlotBudgetIsStatedInOnePlace();
		curseSourceIdsWouldHaveCollidedWithoutTheOffset();
		anInjectionDoesNotDisturbTheOriginalFrameSlots();
		packetTapProducesDiffableLog();
		itemDefOutOfRangeIdIsSafe();
		itemDefCertTemplateWithoutCertIdIsSafe();
		entityDefOutOfRangeIdIsSafe();
		objectDefOutOfRangeIdIsSafe();
		varBitVarpIdOutOfRangeIsSafe();
		varpIndexCounterOverflowIsSafe();
		idkOpcodePastColourTableIsSafe();
		idkSkippedStoreStillConsumesTheWord();
		objectManagerOverlayIdOutOfRangeIsSafe();
		streamLoaderOversizedGzipPayloadIsSafe();
		streamLoaderOutOfRangeIndexExtentIsSafe();
		streamLoaderTruncatedHeaderIsSafe();
		streamLoaderTruncatedIndexTableIsSafe();

		System.out.println("=====================================");
		System.out.println("passed: " + passed + "   failed: " + failed);
		if (failed > 0) {
			System.out.println("RESULT: FAIL");
			System.exit(1);
		}
		System.out.println("RESULT: PASS");
	}

	// ------------------------------------------------------------------ Stream

	private static void streamWordRoundTrip() {
		Stream s = new Stream(new byte[8]);
		s.writeWord(0x1234);
		s.currentOffset = 0;
		check("Stream: writeWord/readUnsignedWord round-trips 0x1234",
				s.readUnsignedWord() == 0x1234);
	}

	/**
	 * Round-tripping alone would pass if both halves were little-endian, so the byte order is
	 * asserted directly on the buffer. The wire protocol is big-endian; a flip here would desync
	 * every packet while still round-tripping cleanly.
	 */
	private static void streamWordIsBigEndian() {
		Stream s = new Stream(new byte[8]);
		s.writeWord(0x1234);
		check("Stream: writeWord is big-endian on the wire (hi byte first)",
				(s.buffer[0] & 0xff) == 0x12 && (s.buffer[1] & 0xff) == 0x34);
	}

	private static void streamDWordRoundTrip() {
		Stream s = new Stream(new byte[8]);
		s.writeDWord(0x01020304);
		s.currentOffset = 0;
		check("Stream: writeDWord/readDWord round-trips 0x01020304",
				s.readDWord() == 0x01020304);
	}

	/** 0xDEADBEEF is negative as an int; a naive shift-based read can lose the high bit. */
	private static void streamDWordHighBitSurvives() {
		Stream s = new Stream(new byte[8]);
		s.writeDWord(0xDEADBEEF);
		s.currentOffset = 0;
		check("Stream: readDWord preserves the high bit (0xDEADBEEF)",
				s.readDWord() == 0xDEADBEEF);
	}

	private static void streamUnsignedAndSignedByte() {
		Stream s = new Stream(new byte[8]);
		s.writeByte(0xFF);
		s.writeByte(0xFF);
		s.currentOffset = 0;
		int unsigned = s.readUnsignedByte();
		byte signed = s.readSignedByte();
		check("Stream: readUnsignedByte gives 255 for 0xFF", unsigned == 255);
		check("Stream: readSignedByte gives -1 for 0xFF", signed == -1);
	}

	private static void streamThreeByteRead() {
		Stream s = new Stream(new byte[8]);
		s.writeByte(0x01);
		s.writeByte(0x02);
		s.writeByte(0x03);
		s.currentOffset = 0;
		check("Stream: read3Bytes reads 0x010203 as 66051", s.read3Bytes() == 0x010203);
	}

	private static void streamStringRoundTrip() {
		Stream s = new Stream(new byte[64]);
		s.writeString("hello");
		s.currentOffset = 0;
		check("Stream: writeString/readString round-trips 'hello'",
				"hello".equals(s.readString()));
	}

	/** readSignedWord must sign-extend above 32767, or negative coordinates read as large positives. */
	private static void streamSignedWordSignExtends() {
		Stream s = new Stream(new byte[8]);
		s.writeWord(0xFFFF);
		s.currentOffset = 0;
		check("Stream: readSignedWord sign-extends 0xFFFF to -1", s.readSignedWord() == -1);
	}

	private static void streamG2G4Aliases() {
		Stream s = new Stream(new byte[8]);
		s.writeWord(0xABCD);
		s.writeDWord(0x11223344);
		s.currentOffset = 0;
		check("Stream: g2 agrees with readUnsignedWord", s.g2() == 0xABCD);
		check("Stream: g4 agrees with readDWord", s.g4() == 0x11223344);
	}

	private static void streamBitAccess() {
		Stream s = new Stream(new byte[8]);
		s.writeByte(0xA5);
		s.currentOffset = 0;
		s.initBitAccess();
		int bits = s.readBits(8);
		s.finishBitAccess();
		check("Stream: readBits(8) reads the byte at the offset (0xA5)", bits == 0xA5);
	}

	/**
	 * WARNING: this pins a real hazard, and it is a bug, not a contract worth keeping.
	 * {@code Stream.readUnsignedWord()} wraps its body in {@code try { ... } catch (Exception e) {
	 * return readUnsignedWord2(); }} and {@code readUnsignedWord2()} returns a <b>hardcoded
	 * 1795</b>. So reading past the end of the buffer does not throw - it silently yields 1795.
	 * The practical consequence is that a protocol desync shows up as plausible-looking wrong data
	 * rather than as an error, which is far harder to diagnose than a crash.
	 *
	 * <p>The assertions below record the behaviour <em>as it is today</em> rather than the behaviour
	 * it should have, so that Phase 2 (defensive data layer) has to change this test deliberately
	 * when it fixes the silent fallback. A test asserting the fix would fail right now and get
	 * deleted; a test asserting today's behaviour is a tripwire.
	 */
	private static void streamOutOfRangeReadIsSilentAndWrong() {
		Stream s = new Stream(new byte[2]);
		s.writeWord(0x1234);   // exactly fills the buffer
		s.currentOffset = 0;
		s.readUnsignedWord();  // in range
		boolean threw = false;
		int value = -1;
		try {
			value = s.readUnsignedWord();  // now out of range
		} catch (Throwable t) {
			threw = true;
		}
		check("Stream: out-of-range readUnsignedWord does NOT throw (silent failure)", !threw);
		check("Stream: out-of-range readUnsignedWord returns the hardcoded 1795 sentinel",
				value == 1795);
	}

	// ------------------------------------------------------------------ PacketTap

	/**
	 * Verifies the Phase 0.3 capture tool actually writes what it claims.
	 *
	 * <p>This exists because the failure it guards against is silent and expensive: if
	 * the tap does not enable, or writes a format that cannot be diffed, the user spends
	 * a session capturing and ends up with a log that is worthless as an oracle - and
	 * the mistake surfaces later, when there is no second chance to record that exact
	 * session against the same server build.
	 *
	 * <p>⚠️ The system property must be set <em>before</em> the first reference to
	 * {@code PacketTap}, because it reads it in a static initialiser. Nothing else in
	 * this harness touches it, so setting it here is safe.
	 */
	private static void packetTapProducesDiffableLog() {
		File out = new File(System.getProperty("java.io.tmpdir"), "soultrail-tap-selftest.log");
		if (out.exists()) {
			out.delete();
		}
		System.setProperty("soultrail.packettap", out.getAbsolutePath());

		// First reference initialises PacketTap, which opens the file.
		check("PacketTap: enables when its system property points at a file", PacketTap.enabled());

		PacketTap.incoming(new byte[] { 0x0a }, 0, 1);
		PacketTap.outgoing(new byte[] { 0x00, 0x04, 0x01 }, 0, 3);
		PacketTap.incoming(new byte[] { (byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef }, 0, 4);

		String text = readFileText(out);
		check("PacketTap: wrote a log", out.exists() && text.length() > 0);
		check("PacketTap: records an incoming read as 'R <len> <hex>'", text.contains("R 1 0a\n"));
		check("PacketTap: records an outgoing frame as 'W <len> <hex>'", text.contains("W 3 000401\n"));
		check("PacketTap: zero-pads bytes to two hex digits (0xde.. stays 8 chars)",
				text.contains("R 4 deadbeef\n"));

		// Diffability: every data line must be exactly "<dir> <len> <hex>" - no
		// timestamps, no counters, nothing that differs between two runs.
		boolean everyLineDiffable = true;
		for (String line : text.split("\n", -1)) {
			if (line.length() == 0 || line.startsWith("#")) {
				continue;
			}
			if (!line.matches("[RW] [0-9]+ [0-9a-f]+")) {
				everyLineDiffable = false;
			}
		}
		check("PacketTap: every data line is diffable ('<dir> <len> <hex>', no timestamps)",
				everyLineDiffable);

		out.delete();
	}

	private static String readFileText(File file) {
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			byte[] buf = new byte[(int) file.length()];
			int offset = 0;
			int n;
			while (offset < buf.length && (n = in.read(buf, offset, buf.length - offset)) > 0) {
				offset += n;
			}
			return new String(buf, 0, offset, "UTF-8");
		} catch (Exception e) {
			return "";
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception e) {
			}
		}
	}

	// ---------------------------------------------------------------- ItemDef

	/**
	 * Pins the crash reported live while ranging Rock Crabs: a noted item whose opcode
	 * 98 (certTemplateID) is present without opcode 97 (certID) leaves certID at -1,
	 * and toNote() then calls forID(-1), which indexed streamIndices[-1].
	 *
	 * <p>The cache is built for real rather than left unloaded, and that distinction is
	 * the whole point of this test: with cache == null the guard short-circuits before
	 * the index test, so a regression that dropped ONLY the bounds check would still
	 * pass. Loading a valid archive puts a non-null streamIndices in place, which is
	 * exactly what the crash indexed.
	 */
	private static void itemDefOutOfRangeIdIsSafe() {
		try {
			ItemDef.unpackConfig(syntheticItemCache());
		} catch (Throwable t) {
			check("ItemDef: synthetic cache loads (required to reach the index check)", false);
			return;
		}

		boolean threw = false;
		String detail = "";
		ItemDef negative = null;
		ItemDef huge = null;
		ItemDef inRange = null;
		try {
			negative = ItemDef.forID(-1);
			huge = ItemDef.forID(Integer.MAX_VALUE);
			inRange = ItemDef.forID(3);
		} catch (Throwable t) {
			threw = true;
			detail = t.getClass().getSimpleName();
		}

		check("ItemDef: forID(-1) does not throw (was ArrayIndexOutOfBoundsException: Index -1)"
				+ (threw ? " - got " + detail : ""), !threw);
		check("ItemDef: forID(-1) returns a usable definition whose name is non-null",
				!threw && negative != null && negative.name != null && negative.name.length() > 0);
		check("ItemDef: forID(Integer.MAX_VALUE) does not throw", !threw && huge != null);
		check("ItemDef: an in-range id still reads the cache unchanged", !threw && inRange != null);
	}

	/**
	 * Phase 2.4 - the OTHER half of the 2.1 fix, and the only Phase 2 guard the consolidated
	 * mutation sweep found with no test at all. `forID` calls `toNote()` whenever
	 * `certTemplateID != -1`, and `toNote` reads `certID` (opcode 97) and `certTemplateID`
	 * (opcode 98) INDEPENDENTLY, so a definition carrying 98 without 97 arrives with
	 * `certID == -1`.
	 *
	 * <p>What the guard promises is documented in the code as "leave the base definition
	 * untouched rather than half-rewriting it into a broken note", so that is what is asserted.
	 * ⚠️ With the guard deleted this does NOT throw, which is why the sweep missed it and why
	 * asserting "did not throw" would be blind: `forID(-1)` returns the `invalid()` placeholder,
	 * whose name is the four-character string `"null"` - so it passes the non-empty-name check
	 * just below and the note conversion proceeds, quietly flipping `stackable` to true and
	 * renaming the item to `"null"`. The observable therefore has to be the field, not the
	 * exception.
	 *
	 * <p>⚠️ The name cannot be omitted from the fixture: `toNote` bails out on a null name
	 * before applying the note, so a nameless definition would pass even with the guard gone.
	 * That is also why this is the first fixture in the harness that is not all-zero.
	 */
	private static void itemDefCertTemplateWithoutCertIdIsSafe() {
		String name = null;
		boolean stackable = true;
		Throwable thrown = null;
		try {
			ItemDef.unpackConfig(syntheticCertItemCache());
			ItemDef def = ItemDef.forID(0);
			name = def.name;
			stackable = def.stackable;
		} catch (Throwable t) {
			thrown = t;
		}
		check("ItemDef: certTemplateID set without certID does not throw"
				+ (thrown == null ? "" : " - got " + thrown.getClass().getSimpleName()),
				thrown == null);
		check("ItemDef: ... and the base definition is LEFT ALONE, not converted into a note"
				+ " (name=[" + name + "] stackable=" + stackable + ")",
				thrown == null && "Thing".equals(name) && !stackable);
	}

	/**
	 * A real definition, because the guard under test is unreachable without one:
	 * `[u2 unused header][op 2: name "Thing"][op 98: certTemplateID = 5][op 0]`, with opcode 97
	 * deliberately absent. The definition begins at offset 2 because `unpackConfig` starts its
	 * index table there.
	 */
	private static byte[] itemWithCertTemplateOnlyBlob() {
		byte[] head = new byte[] { 0, 0, 2, 'T', 'h', 'i', 'n', 'g', 0, 98, 0, 5, 0 };
		return padded(head);
	}

	private static StreamLoader syntheticCertItemCache() {
		return syntheticArchive(new String[] { "obj.dat", "obj.idx" },
				new byte[][] { itemWithCertTemplateOnlyBlob(), indexBuffer(1) });
	}

	/**
	 * The same out-of-range shape as ItemDef, for the other two classes that hold a
	 * definition index table.
	 *
	 * <p>Both turned out to ALREADY be guarded, so these tests fix nothing. They exist
	 * to pin guards that previously had no coverage at all: an unguarded guard is one
	 * refactor away from being deleted silently, and that is exactly how ItemDef came
	 * to crash in the first place.
	 */
	private static void entityDefOutOfRangeIdIsSafe() {
		try {
			EntityDef.unpackConfig(syntheticNpcCache());
		} catch (Throwable t) {
			check("EntityDef: synthetic npc cache loads (required to reach the index check)", false);
			return;
		}
		Throwable thrown = null;
		EntityDef negative = null;
		EntityDef huge = null;
		try {
			negative = EntityDef.forID(-1);
			huge = EntityDef.forID(Integer.MAX_VALUE);
		} catch (Throwable t) {
			thrown = t;
		}
		check("EntityDef: forID(-1) / forID(MAX_VALUE) do not throw (readId clamp already present)"
				+ (thrown == null ? "" : " - got " + thrown.getClass().getSimpleName()),
				thrown == null);
		check("EntityDef: both return a definition",
				thrown == null && negative != null && huge != null);
	}

	private static void objectDefOutOfRangeIdIsSafe() {
		try {
			ObjectDef.unpackConfig(syntheticObjectCache());
		} catch (Throwable t) {
			check("ObjectDef: synthetic object cache loads (required to reach the index check)", false);
			return;
		}
		Throwable thrown = null;
		ObjectDef negative = null;
		ObjectDef sameAsZero = null;
		ObjectDef huge = null;
		try {
			// NB: -1 is NOT usable here, and a mutation test proved it. ObjectDef's
			// constructor defaults `type` to -1, so the cache scan at the top of forID()
			// matches cache[0] and returns early: the clamp is never reached and a test
			// written with -1 passes even when the clamp is deleted. -2 misses the scan
			// and really does fall through to `if (i < 0) i = 0`.
			negative = ObjectDef.forID(-2);
			// The clamped call stored `type = 0` in its cache slot (ObjectDef.java:26), so
			// a plain forID(0) must now hit the cache scan and hand back that same object.
			// Identity, not just "did not throw", is what proves the clamp actually ran.
			sameAsZero = ObjectDef.forID(0);
			// pins BOTH the delegation to forID525 and its own `i >= length` clamp
			huge = ObjectDef.forID(Integer.MAX_VALUE);
		} catch (Throwable t) {
			thrown = t;
		}
		check("ObjectDef: forID(-2) / forID(MAX_VALUE) do not throw (clamp + 525 delegation)"
				+ (thrown == null ? "" : " - got " + thrown.getClass().getSimpleName()),
				thrown == null);
		check("ObjectDef: forID(-2) is clamped to index 0 (forID(0) returns the same object)",
				thrown == null && negative != null && negative == sameAsZero);
		check("ObjectDef: forID(MAX_VALUE) still returns a definition",
				thrown == null && huge != null);
	}

	/**
	 * Phase 2.2 — `VarBit.unpackConfig` flag-propagates to the Varp a varbit points at:
	 *
	 *   if (cache[j].aBoolean651) Varp.cache[cache[j].anInt648].aBoolean713 = true;
	 *
	 * `anInt648` (the target **varp** id) is read out of `varbit.dat`, while `Varp.cache`
	 * is sized from `varp.dat`'s entry count — so the two files can simply disagree, and
	 * nothing checks. The index is reached **during cache load**, before a single packet
	 * arrives. This is the ItemDef crash's exact shape in a second reader: a
	 * cache-derived id used to index an array with no bounds check.
	 *
	 * <p>The two files genuinely disagree here (1 varp; a varbit pointing at varp 2) and
	 * are loaded in the real order — `client.java` unpacks `Varp` (line 11570) before
	 * `VarBit` (line 11571) — which is what makes the failure reproducible rather than
	 * hypothetical.
	 */
	private static void varBitVarpIdOutOfRangeIsSafe() {
		Throwable thrown = null;
		try {
			StreamLoader loader = syntheticArchive(
					new String[] { "varp.dat", "varp.idx", "varbit.dat", "varbit.idx" },
					new byte[][] { varpOneEntryBlob(), indexBuffer(1),
							arbitraryVarBitBlob(), indexBuffer(1) });
			Varp.unpackConfig(loader);
			VarBit.unpackConfig(loader);
		} catch (Throwable t) {
			thrown = t;
		}
		check("VarBit: a varbit pointing past the end of Varp.cache does not throw"
				+ (thrown == null ? "" : " - got " + thrown.getClass().getSimpleName()),
				thrown == null);
	}

	/**
	 * Phase 2.2 — `Varp.readValues` writes into a fixed-size index table:
	 *
	 *   if (j == 3) anIntArray703[anInt702++] = i;
	 *
	 * `anIntArray703` is allocated with the entry count from `varp.dat`, so the file and
	 * the array are sized by the same number — but the write is driven by a *counter* over
	 * opcode-3 occurrences, not by the array. An entry carrying opcode 3 twice (a corrupt
	 * file, or a parse desync that re-reads it) walks off the end at cache load.
	 *
	 * <p>Both tests use a one-entry cache on purpose, so they allocate the same static
	 * tables and neither depends on running first.
	 */
	private static void varpIndexCounterOverflowIsSafe() {
		Throwable thrown = null;
		try {
			StreamLoader loader = syntheticArchive(
					new String[] { "varp.dat", "varp.idx" },
					new byte[][] { varpTwoOpcode3Blob(), indexBuffer(1) });
			Varp.unpackConfig(loader);
		} catch (Throwable t) {
			thrown = t;
		}
		check("Varp: a definition carrying opcode 3 twice does not walk off the index table"
				+ (thrown == null ? "" : " - got " + thrown.getClass().getSimpleName()),
				thrown == null);
	}

	/**
	 * Phase 2.2 — `IDK.readValues` writes body colours and head models through opcode ranges
	 * that are WIDER than the arrays they index:
	 *
	 *   if (i >= 40 &amp;&amp; i &lt; 50) anIntArray659[i - 40] = ...   // anIntArray659 is int[6]
	 *   if (i >= 50 &amp;&amp; i &lt; 60) anIntArray660[i - 50] = ...   // anIntArray660 is int[6]
	 *   if (i >= 60 &amp;&amp; i &lt; 70) anIntArray661[i - 60] = ...   // anIntArray661 is int[5]
	 *
	 * Each range can address 10 slots of a 6/6/5-slot array, so opcodes 46-49, 56-59 and
	 * 65-69 walk past the end — at cache load, before any packet arrives. This is the
	 * ItemDef/VarBit shape again, and this instance is one of the plainest: the range and
	 * the array size simply disagree in the same statement.
	 *
	 * <p>Opcode 46 is used here rather than a "wild" value because it is the smallest
	 * out-of-range one: valid 317 data only uses 40-45 / 50-55 / 60-64, so a well-formed
	 * cache never reaches the guard.
	 */
	private static void idkOpcodePastColourTableIsSafe() {
		Throwable thrown = null;
		try {
			StreamLoader loader = syntheticArchive(
					new String[] { "idk.dat", "idk.idx" },
					new byte[][] { idkOpcode46Blob(), indexBuffer(1) });
			IDK.unpackConfig(loader);
		} catch (Throwable t) {
			thrown = t;
		}
		check("IDK: opcode 46 (one past the 6-slot colour table) does not throw"
				+ (thrown == null ? "" : " - got " + thrown.getClass().getSimpleName()),
				thrown == null);
	}

	/**
	 * The subtle half of the IDK fix, pinned separately because it is the part a later
	 * "simplification" is most likely to break: the out-of-range store is skipped, but the
	 * byte pair it used to consume MUST still be read. Moving the read inside the guard would
	 * leave the stream 2 bytes ahead, so every later definition in `idk.dat` would be parsed
	 * from the wrong offset — a **silent** corruption, where the crash it replaced was at
	 * least loud.
	 *
	 * <p>Observed through a following opcode-2 model list: if the word were not consumed, the
	 * parser would read the word's first byte (0x00) as the next opcode, terminate the
	 * definition early, and leave the model list null.
	 *
	 * <p>Uses reflection on the private `anIntArray658` only because `IDK` exposes no getter.
	 * ⚠️ A Phase 3 field rename will break this test — that is acceptable, it just has to be
	 * renamed with it rather than deleted.
	 */
	private static void idkSkippedStoreStillConsumesTheWord() {
		int[] models = null;
		Throwable thrown = null;
		try {
			StreamLoader loader = syntheticArchive(
					new String[] { "idk.dat", "idk.idx" },
					new byte[][] { idkOpcode46ThenModelBlob(), indexBuffer(1) });
			IDK.unpackConfig(loader);
			java.lang.reflect.Field f = IDK.class.getDeclaredField("anIntArray658");
			f.setAccessible(true);
			models = (int[]) f.get(IDK.cache[0]);
		} catch (Throwable t) {
			thrown = t;
		}
		check("IDK: the guarded opcode-46 word is still consumed, so the next field parses"
				+ (thrown == null ? "" : " - got " + thrown.getClass().getSimpleName()),
				thrown == null && models != null && models.length == 1 && models[0] == 7);
	}

	/**
	 * Phase 2.2, the CONSUMER side — `ObjectManager.method171` blends region tile colours and
	 * looks each tile's floor up by id straight out of the landscape data:
	 *
	 *   int l18 = aByteArrayArrayArray142[l][l6][k17] &amp; 0xff;   // tile overlay id
	 *   if (l18 &gt; 0) { Flo floLocal = Flo.cache[l18 - 1]; ... }
	 *
	 * The `&gt; 0` test covers the zero case but there is **no upper bound**, so a floor id past
	 * `flo.dat`'s entry count indexes off the end of `Flo.cache`. ⚠️ **This is the ItemDef shape
	 * on the consumer side**: the id comes from cache data (the landscape map) rather than from a
	 * packet, and it fires during **region load**. Five sites share it, all four ids unguarded:
	 * `:111` and `:124` (overlay, smoothing pass), `:189` (overlay), `:222` and `:243` (underlay).
	 * Note `:242` *looks* guarded, but it tests the already-decremented `i19 - 1` against a length
	 * that is an exclusive bound, so it misses by one and lets `Flo.cache[length]` through.
	 *
	 * <p>⚠️ <b>The failure mode is silence, not a crash.</b> `method171`'s entire body (36-450) is
	 * wrapped in `try { ... } catch(Exception e) { }` with an empty handler, so an out-of-range id
	 * does not surface at all — it abandons the rest of the region's blending and lighting and
	 * writes nothing, with no message. The test therefore cannot assert "does not throw"; it has
	 * to assert on a *later* side effect of the same pass.
	 *
	 * <p>Fixture: a 55-entry `Flo.cache`, so id 56 is exactly one past the end, plus three tiles in
	 * region level 1:
	 * <ul>
	 *   <li>`overlay[1][5][5] = 56` — read by the smoothing pass at `:111` (added at l6 = 0) and
	 *       again at `:124` (subtracted at l6 = r + blendR = 10); read by the tile pass at `:189`.
	 *   <li>`underlay[1][6][5] = 56` — read by the tile pass at `:222` and `:243`.
	 *   <li>`overlay[1][12][5] = 1` — a <b>valid</b> witness tile past all of the above. Its
	 *       flat-tile shading bit (0x924, set at `:225`) is the assertion, so a guard missing at
	 *       *any* of the five sites — all of which are reached before l6 = 12 — fails the test.
	 * </ul>
	 *
	 * <p>The witness is a real `WorldController` because the valid path renders into it; the
	 * underlay tile deliberately resolves to `Flo.cache.length` (55) because `:244` skips the
	 * whole texture/render block for `i19 - 1 == 54`, which keeps the fixture free of textures.
	 */
	private static void objectManagerOverlayIdOutOfRangeIsSafe() {
		// Non-low-mem is a shipped configuration (client.java sets it from the detail setting)
		// and it reduces `:164`'s guard to a plain `!lowMem` instead of a method182/anInt131 test.
		boolean savedLowMem = ObjectManager.lowMem;
		ObjectManager.lowMem = false;
		Throwable thrown = null;
		int witness = -1;
		try {
			Flo.unpackConfig(syntheticFloorCache(55));
			ObjectManager manager = new ObjectManager(new byte[4][104][104], new int[4][105][105]);
			// The constructor allocates these itself, so they are fetched and filled in place
			// rather than passed in.
			byte[][][] tileOverlay =
					(byte[][][]) readField(manager, "aByteArrayArrayArray142");
			byte[][][] tileUnderlay =
					(byte[][][]) readField(manager, "aByteArrayArrayArray130");
			tileOverlay[1][5][5] = 56;
			tileUnderlay[1][6][5] = 56;
			tileOverlay[1][12][5] = 1;
			manager.method171(new CollisionMap[4], new WorldController(new int[4][105][105]));
			int[][][] shade = (int[][][]) readField(manager, "anIntArrayArrayArray135");
			witness = shade[1][12][5] & 0x924;
		} catch (Throwable t) {
			thrown = t;
		} finally {
			ObjectManager.lowMem = savedLowMem;
		}
		// The throwable is reported, not asserted on: method171 swallows its own exceptions, so a
		// non-null one here means the FIXTURE is wrong (reflection, constructor, Flo.unpackConfig),
		// which is a different failure from the guard being absent.
		check("ObjectManager: tile ids past the end of Flo.cache do not abandon the region blend"
				+ (thrown == null
						? " (witness shading 0x" + Integer.toHexString(witness) + ")"
						: " - fixture threw " + thrown),
				thrown == null && witness == 0x924);
	}

	/**
	 * Phase 2.3 - `StreamLoader.a`'s gzip branch copies the raw payload into a buffer sized to
	 * the *declared uncompressed* length:
	 *
	 *   byte[] abyte1 = new byte[i];
	 *   System.arraycopy(abyte0, 6, abyte1, 0, abyte0.length - 6);
	 *
	 * so a corrupt entry declaring a size smaller than its actual payload overflowed - and that
	 * line sits OUTSIDE the surrounding try, so the ArrayIndexOutOfBoundsException escaped `a()`
	 * instead of degrading to a missing entry.
	 *
	 * <p>This builds a genuinely valid gzip archive and only then lies the declared size down to
	 * 4, so nothing but the bound is under test: the copy is clamped, the 4-byte gzip fails to
	 * inflate, and the loader ends up with no entries. Asserting {@code dataSize == 0} is the
	 * half that matters - "did not throw" alone would also pass for a loader that fabricated a
	 * bogus index table.
	 *
	 * <p>⚠️ The {@code EOFException} that this prints to stderr while the harness passes is
	 * EXPECTED: it is the loader's own gzip catch reporting the clamped payload, which is the
	 * deliberate degrade. It is left as a printed trace rather than silenced because the catch
	 * is the only place a corrupt cache entry is ever reported at all.
	 */
	private static void streamLoaderOversizedGzipPayloadIsSafe() {
		Throwable thrown = null;
		int dataSize = -1;
		try {
			byte[] file = wrapGzip(emptyDefinitionBlob());
			write3(file, 0, 4); // the lie: far shorter than the real (compressed) payload
			StreamLoader loader = new StreamLoader(file, "synthetic-oversized");
			dataSize = loader.dataSize;
		} catch (Throwable t) {
			thrown = t;
		}
		check("StreamLoader: a gzip payload longer than its declared size does not throw"
				+ (thrown == null ? "" : " - got " + thrown.getClass().getSimpleName()),
				thrown == null);
		check("StreamLoader: ... and it degrades to no entries, not a fabricated index table",
				thrown == null && dataSize == 0);
	}

	/**
	 * Phase 2.3 - `StreamLoader.getDataForName` trusts its own index: it decompressed or copied
	 * the declared range with no bound against the buffer, so an archive whose index disagrees
	 * with its data read past the end. Both branches are covered, because the extent that has to
	 * fit differs between them:
	 *
	 * <ul>
	 *   <li>the gzip branch is already decompressed, so the copy length is the UNCOMPRESSED
	 *       size and the source is the decompressed buffer (a plain arraycopy);</li>
	 *   <li>the LZ branch passes the COMPRESSED size to `Class13.method225`, which then ran off
	 *       the end mid-decompression - unbounded and inside a byte-at-a-time reader, so it
	 *       threw only once it had already consumed everything past the end.</li>
	 * </ul>
	 *
	 * <p>Each assertion is paired: no throw, and the lookup returns null, because "missing" is
	 * the degradation the plan asks for and a non-null return would mean the guard let a
	 * garbage entry through.
	 */
	private static void streamLoaderOutOfRangeIndexExtentIsSafe() {
		Object gzipResult = "not-called";
		Throwable gzipThrown = null;
		try {
			StreamLoader loader = new StreamLoader(
					wrapGzip(payloadOneEntry("X", 250, 250)), "synthetic-bad-index");
			gzipResult = loader.getDataForName("X");
		} catch (Throwable t) {
			gzipThrown = t;
		}
		check("StreamLoader: an index extent past the decompressed buffer does not throw"
				+ (gzipThrown == null ? "" : " - got " + gzipThrown.getClass().getSimpleName()),
				gzipThrown == null);
		check("StreamLoader: ... and the lookup degrades to null (missing), not garbage",
				gzipThrown == null && gzipResult == null);

		Object rawResult = "not-called";
		Throwable rawThrown = null;
		try {
			StreamLoader loader = new StreamLoader(
					rawArchive(payloadOneEntry("X", 8, 200)), "synthetic-bad-index-raw");
			rawResult = loader.getDataForName("X");
		} catch (Throwable t) {
			rawThrown = t;
		}
		check("StreamLoader: an LZ entry whose compressed extent runs past the file does not throw"
				+ (rawThrown == null ? "" : " - got " + rawThrown.getClass().getSimpleName()),
				rawThrown == null);
		check("StreamLoader: ... and that lookup degrades to null (missing) too",
				rawThrown == null && rawResult == null);
	}

	/**
	 * Phase 2.3 - a file shorter than its own six-byte header (two 3-byte sizes plus the
	 * data). `a()` opened by reading both sizes unconditionally, so a truncated download or a
	 * partial cache entry threw {@code ArrayIndexOutOfBoundsException} out of the constructor.
	 * Guarded by the `length &lt; 6` check, which is inert on valid data because a valid
	 * archive always carries the header.
	 */
	private static void streamLoaderTruncatedHeaderIsSafe() {
		int dataSize = -1;
		Throwable thrown = null;
		try {
			StreamLoader loader = new StreamLoader(new byte[] { 0, 0, 1 }, "synthetic-stub");
			dataSize = loader.dataSize;
		} catch (Throwable t) {
			thrown = t;
		}
		check("StreamLoader: a file shorter than its own header does not throw"
				+ (thrown == null ? "" : " - got " + thrown.getClass().getSimpleName()),
				thrown == null);
		check("StreamLoader: ... and it exposes no entries (dataSize 0)",
				thrown == null && dataSize == 0);
	}

	/**
	 * Phase 2.3 - the index-table counterpart of the two bounds above: a payload whose declared
	 * entry count does not fit inside it. The entry loop reads 10 bytes per entry through
	 * `readDWord`/`read3Bytes`, and neither of those is guarded, so the count is now checked
	 * against the buffer before the loop rather than trusted.
	 *
	 * <p>Chosen so the count is absurd (1000 entries into 128 bytes) rather than merely one
	 * short: the guard is a plain fit test, and an obviously-impossible count is what makes a
	 * regression show up as an exception instead of as a silently truncated index.
	 */
	private static void streamLoaderTruncatedIndexTableIsSafe() {
		int dataSize = -1;
		Object result = "not-called";
		Throwable thrown = null;
		try {
			StreamLoader loader = new StreamLoader(
					wrapGzip(payloadWithDeclaredDataSize(1000)), "synthetic-short-index");
			dataSize = loader.dataSize;
			result = loader.getDataForName("obj.dat");
		} catch (Throwable t) {
			thrown = t;
		}
		check("StreamLoader: an index count that does not fit its payload does not throw"
				+ (thrown == null ? "" : " - got " + thrown.getClass().getSimpleName()),
				thrown == null);
		check("StreamLoader: ... and the archive exposes no entries rather than reading past the end",
				thrown == null && dataSize == 0 && result == null);
	}

	/** Reads a private field of an instance, for the arrays `method171` blends into. */
	private static Object readField(Object target, String name) throws Exception {
		java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(target);
	}

	/**
	 * Writes a field of an instance. Used to park synthetic geometry onto a bare
	 * {@link Model} so the Phase 5.1 accessors can be checked without a cache.
	 */
	private static void writeField(Object target, String name, Object value) {
		try {
			java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
			f.setAccessible(true);
			f.set(target, value);
		} catch (Exception e) {
			throw new RuntimeException("could not write field " + name, e);
		}
	}

	/**
	 * A bare {@link Model} with no geometry at all.
	 *
	 * <p>Built through the private no-arg constructor by reflection, deliberately:
	 * every other constructor needs cache data or model streams, and reusing the
	 * shared {@link Model#aModel_1621} singleton would let one test's parked data
	 * leak into another.
	 */
	private static Model bareModel() {
		try {
			java.lang.reflect.Constructor<Model> c = Model.class.getDeclaredConstructor();
			c.setAccessible(true);
			return c.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("could not create a bare Model", e);
		}
	}

	// ------------------------------- Phase 5.1 geometry accessors

	/**
	 * The accessors must return the model's OWN arrays, in their own slots.
	 *
	 * <p>Identity comparison (==) is the point: it fails if a mapping is swapped
	 * (ys for zs), if a copy is returned, or if the wrong field is read - none of
	 * which a value comparison of one array would catch.
	 */
	private static void modelGeometryExposesLocalVertices() {
		Model m = bareModel();
		int[] xs = { 1, 2, 3 };
		int[] ys = { 4, 5, 6 };
		int[] zs = { 7, 8, 9 };
		int[] fa = { 0, 2 };
		int[] fb = { 1, 0 };
		int[] fc = { 2, 1 };
		writeField(m, "anInt1626", 3);
		writeField(m, "anInt1630", 2);
		writeField(m, "anIntArray1627", xs);
		writeField(m, "anIntArray1628", ys);
		writeField(m, "anIntArray1629", zs);
		writeField(m, "anIntArray1631", fa);
		writeField(m, "anIntArray1632", fb);
		writeField(m, "anIntArray1633", fc);

		check("Model geometry: vertex and face counts are exposed",
				m.vertexCount() == 3 && m.faceCount() == 2);
		check("Model geometry: vertices map to X, Y, Z in that order (not swapped)",
				m.vertexXs() == xs && m.vertexYs() == ys && m.vertexZs() == zs);
		check("Model geometry: face indices map to A, B, C in that order (not swapped)",
				m.faceVertexA() == fa && m.faceVertexB() == fb && m.faceVertexC() == fc);
	}

	private static void modelGeometryDecodesRenderTypeAndTexture() {
		Model m = bareModel();
		writeField(m, "anIntArray1637", new int[] { 2 | (5 << 2), 3 });
		check("Model geometry: render type is the LOW 2 BITS of the render-type word",
				m.faceRenderType(0) == 2);
		check("Model geometry: the texture-COORDINATE INDEX is the render-type word shifted right by 2",
				m.faceTextureIndex(0) == 5);
		check("Model geometry: a purely flat face decodes to texture-coordinate index 0",
				m.faceRenderType(1) == 3 && m.faceTextureIndex(1) == 0);
	}

	/**
	 * The distinction Phase 5.3 caught by driving the real rasteriser: the texture ID
	 * lives in the face COLOUR slot for textured faces, while {@code renderType >> 2}
	 * only selects which texture-coordinate entry to use.
	 *
	 * <p>⚠️ This is asserted deliberately with the two values set to DIFFERENT numbers,
	 * so a regression that collapses them into one (as the first draft of 5.1 did) fails
	 * rather than coincidentally passing.
	 */
	private static void modelGeometryTextureIdComesFromTheColourSlot() {
		Model m = bareModel();
		// render type 2 (textured), texture-coordinate index 5, texture id 99.
		writeField(m, "anIntArray1637", new int[] { 2 | (5 << 2) });
		writeField(m, "anIntArray1640", new int[] { 99 });
		check("Model geometry: a textured face's texture ID comes from the COLOUR slot",
				m.faceTextureId(0) == 99);
		check("Model geometry: ... and is a DIFFERENT value from the texture-coordinate index",
				m.faceTextureIndex(0) == 5 && m.faceTextureId(0) != m.faceTextureIndex(0));

		// A flat face keeps its colour and has no texture id at all.
		Model flat = bareModel();
		writeField(flat, "anIntArray1637", new int[] { 0 });
		writeField(flat, "anIntArray1640", new int[] { 0xF800 });
		check("Model geometry: a FLAT face has no texture ID, even though its colour slot is set",
				flat.faceTextureId(0) == -1);
		check("Model geometry: ... and its colour slot really is a colour",
				flat.faceBaseColours()[0] == 0xF800);
	}

	/**
	 * Not every model has every attribute, and the software path already guards for
	 * that. The accessors must mirror those guards rather than inventing values -
	 * a texture id of 0 would mean "texture zero", which is a real texture.
	 */
	private static void modelGeometryHandlesAbsentAttributes() {
		Model m = bareModel();
		check("Model geometry: a model with no render types reports so", !m.hasFaceRenderTypes());
		check("Model geometry: absent render types decode to type 0 without throwing",
				m.faceRenderType(0) == 0);
		check("Model geometry: absent render types decode to texture-coordinate index -1, NOT 0",
				m.faceTextureIndex(0) == -1);
		check("Model geometry: absent render types decode to texture ID -1",
				m.faceTextureId(0) == -1);
		check("Model geometry: a model with no textures reports so", !m.hasTextures());
		check("Model geometry: texture count is 0 when there are no textures", m.textureCount() == 0);
		check("Model geometry: absent alpha is reported, not replaced by a sentinel", !m.hasFaceAlphas());
	}

	/**
	 * The texture arrays are indexed by TEXTURE ID, while faces are indexed by face
	 * id. Confusing the two is the most likely way a GPU upload goes subtly wrong -
	 * so the counts are made to differ here, which any such mix-up would trip over.
	 */
	private static void modelGeometryTextureArraysAreIndexedByTextureNotFace() {
		Model m = bareModel();
		writeField(m, "anInt1630", 3);
		writeField(m, "anInt1642", 2);
		writeField(m, "anIntArray1643", new int[] { 10, 11 });
		writeField(m, "anIntArray1644", new int[] { 12, 13 });
		writeField(m, "anIntArray1645", new int[] { 14, 15 });

		check("Model geometry: texture arrays are sized by TEXTURE count, not face count",
				m.textureCount() == 2 && m.faceCount() == 3);
		check("Model geometry: texture vertex indices are exposed per texture",
				m.textureVertexA()[1] == 11 && m.textureVertexB()[1] == 13 && m.textureVertexC()[1] == 15);
		check("Model geometry: a model carrying textures reports so", m.hasTextures());
	}

	/**
	 * A minimal but genuinely valid StreamLoader holding the given named entries.
	 *
	 * <p>Uses the gzip branch (compressed length 0) deliberately: it is what the real
	 * cache uses, and it is the only branch that sets aBoolean732, so getDataForName
	 * simply copies instead of running Class13 LZ decompression.
	 */
	private static StreamLoader syntheticArchive(String[] names, byte[][] datas) {
		int dataSize = names.length;
		int dataLen = 0;
		for (int i = 0; i < datas.length; i++)
			dataLen += datas[i].length;

		byte[] payload = new byte[2 + dataSize * 10 + dataLen];
		Stream p = new Stream(payload);
		p.writeWord(dataSize);
		for (int i = 0; i < dataSize; i++)
			writeEntry(p, names[i], datas[i].length);
		// Data begins here, matching StreamLoader's k = currentOffset + dataSize * 10.
		int offset = p.currentOffset;
		for (int i = 0; i < dataSize; i++) {
			System.arraycopy(datas[i], 0, payload, offset, datas[i].length);
			offset += datas[i].length;
		}

		return new StreamLoader(wrapGzip(payload), "synthetic-cache");
	}

	/**
	 * Wraps an archive payload into the gzip-branch file layout: [u3 uncompressed][u3 0][gzip].
	 *
	 * <p>Split out of {@link #syntheticArchive} because the overflow test needs a valid gzip
	 * file whose declared size it can then corrupt, and because the shrink check below is a
	 * hard constraint rather than a nicety: the loader sizes its copy buffer to the UNCOMPRESSED
	 * length, so a payload that does not compress throws before the code under test runs.
	 */
	private static byte[] wrapGzip(byte[] payload) {
		byte[] compressed = gzip(payload);
		if (compressed.length > payload.length) {
			throw new IllegalStateException("gzip did not shrink the payload, but the loader's "
					+ "buffer is sized to the UNCOMPRESSED length, so the copy would overflow");
		}
		byte[] file = new byte[6 + compressed.length];
		write3(file, 0, payload.length);
		write3(file, 3, 0); // 0 selects the gzip branch
		System.arraycopy(compressed, 0, file, 6, compressed.length);
		return file;
	}

	/**
	 * Wraps an archive payload the OTHER way: stored uncompressed ([u3 i][u3 i][payload]) so the
	 * loader takes its `j == i` branch and sets no aBoolean732 - the branch whose lookups go
	 * through `Class13.method225`.
	 */
	private static byte[] rawArchive(byte[] payload) {
		byte[] file = new byte[6 + payload.length];
		write3(file, 0, 1);
		write3(file, 3, 1); // equal and non-zero, so neither the gzip nor the LZ branch is taken
		System.arraycopy(payload, 0, file, 6, payload.length);
		return file;
	}

	/**
	 * A zero-padded payload holding ONE index entry whose two declared lengths are set
	 * independently - which is the point, since {@link #writeEntry} only ever writes a valid
	 * extent. The loader derives the entry's offset as `currentOffset + 10` (12 here), leaving
	 * 116 bytes of data, so a declared length past that is the corrupt case under test.
	 *
	 * <p>Padded for the same reason as {@link #padded}: gzip must still shrink it.
	 */
	private static byte[] payloadOneEntry(String name, int length, int compressedLength) {
		byte[] payload = new byte[128];
		Stream p = new Stream(payload);
		p.writeWord(1);
		p.writeDWord(nameHash(name));
		int at = p.currentOffset;
		write3(payload, at, length);
		write3(payload, at + 3, compressedLength);
		p.currentOffset = at + 6;
		return payload;
	}

	/**
	 * A zero-padded payload whose index table CLAIMS {@code dataSize} entries but contains none.
	 * Padded so gzip still shrinks it; the claim is what the fit test has to reject.
	 */
	private static byte[] payloadWithDeclaredDataSize(int dataSize) {
		byte[] payload = new byte[128];
		Stream p = new Stream(payload);
		p.writeWord(dataSize);
		return payload;
	}

	/**
	 * A one-entry index table: [u2 count][u2 size]. Every reader here accepts this
	 * shape, they just interpret count differently (ItemDef adds 21 to it), so one
	 * helper serves all of them.
	 */
	private static byte[] indexBuffer(int count) {
		return new byte[] { (byte) (count >> 8), (byte) count, 0, 0 };
	}

	/**
	 * opcode 0 in the first byte, so every readValues loop terminates immediately and
	 * the test never depends on real definition data.
	 */
	private static byte[] emptyDefinitionBlob() {
		return new byte[128];
	}

	private static StreamLoader syntheticItemCache() {
		return syntheticArchive(new String[] { "obj.dat", "obj.idx" },
				new byte[][] { emptyDefinitionBlob(), indexBuffer(1) });
	}

	private static StreamLoader syntheticNpcCache() {
		return syntheticArchive(new String[] { "npc.dat", "npc.idx" },
				new byte[][] { emptyDefinitionBlob(), indexBuffer(1) });
	}

	private static StreamLoader syntheticObjectCache() {
		return syntheticArchive(
				new String[] { "loc.dat", "loc.idx", "525loc.dat", "525loc.idx" },
				new byte[][] { emptyDefinitionBlob(), indexBuffer(1),
						emptyDefinitionBlob(), indexBuffer(1) });
	}

	/** [u2 count = 1][one definition whose only byte is the 0 terminator]. */
	private static byte[] varpOneEntryBlob() {
		return padded(new byte[] { 0, 1, 0 });
	}

	/**
	 * [u2 count = 1][opcode 1: anInt648 = 2, anInt649 = 0, anInt650 = 0][opcode 2: sets
	 * aBoolean651, which is what reaches the Varp.cache write][0 terminator]. Varp.cache
	 * holds a single entry, so the target varp 2 is one past the end.
	 */
	private static byte[] arbitraryVarBitBlob() {
		return padded(new byte[] { 0, 1, 1, 0, 2, 0, 0, 2, 0 });
	}

	/** [u2 count = 1][opcode 3 twice, one more than the single-slot index table holds][0]. */
	private static byte[] varpTwoOpcode3Blob() {
		return padded(new byte[] { 0, 1, 3, 3, 0 });
	}

	/**
	 * [u2 count = 1][opcode 46: one past the 6-slot colour table, value 0][0 terminator].
	 * 46 = 40 + 6, so it addresses anIntArray659[6] on a six-element array.
	 */
	private static byte[] idkOpcode46Blob() {
		return padded(new byte[] { 0, 1, 46, 0, 0, 0 });
	}

	/** [u2 count][count floor definitions, each no more than its 0 terminator]. */
	private static StreamLoader syntheticFloorCache(int count) {
		byte[] dat = new byte[2 + count];
		dat[0] = (byte) (count >> 8);
		dat[1] = (byte) count;
		return syntheticArchive(new String[] { "flo.dat", "flo.idx" },
				new byte[][] { padded(dat), indexBuffer(1) });
	}

	/**
	 * [u2 count = 1][opcode 46: out of range, value 0][opcode 2: a ONE-model list whose only
	 * id is 7][0 terminator]. The model list sits after the skipped store on purpose — it is
	 * the probe for whether the opcode-46 word was still consumed.
	 */
	private static byte[] idkOpcode46ThenModelBlob() {
		return padded(new byte[] { 0, 1, 46, 0, 0, 2, 1, 0, 7, 0 });
	}

	/**
	 * Zero-pads a definition blob to {@code emptyDefinitionBlob()}'s length. Not cosmetic:
	 * {@link #syntheticArchive} requires gzip to actually shrink the payload, and a handful
	 * of bytes deflates into a *larger* buffer, which the loader's uncompressed-length
	 * sizing would then overflow. The readers stop at their count/terminator, so the tail
	 * is never parsed; it only makes the file big enough to compress.
	 */
	private static byte[] padded(byte[] head) {
		byte[] b = new byte[128];
		System.arraycopy(head, 0, b, 0, head.length);
		return b;
	}

	private static void writeEntry(Stream p, String name, int size) {
		p.writeDWord(nameHash(name));
		byte[] b = p.buffer;
		int offset = p.currentOffset;
		write3(b, offset, size);
		write3(b, offset + 3, size);
		p.currentOffset = offset + 6;
	}

	private static void write3(byte[] b, int offset, int value) {
		b[offset] = (byte) (value >> 16);
		b[offset + 1] = (byte) (value >> 8);
		b[offset + 2] = (byte) value;
	}

	/*
	 * Must match StreamLoader.getDataForName exactly - the toUpperCase and the -32
	 * included - or the lookups return null and unpackConfig dies on a null stream.
	 */
	private static int nameHash(String s) {
		s = s.toUpperCase();
		int h = 0;
		for (int j = 0; j < s.length(); j++)
			h = (h * 61 + s.charAt(j)) - 32;
		return h;
	}

	private static byte[] gzip(byte[] data) {
		try {
			java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
			java.util.zip.GZIPOutputStream gz = new java.util.zip.GZIPOutputStream(out);
			gz.write(data);
			gz.close();
			return out.toByteArray();
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
	}

	// ------------------------------------------------- raster framebuffer gate (Phase 4.1b)

	/**
	 * The framebuffer-hash gate for Phase 4.1.
	 *
	 * <p><b>Why this exists.</b> Phase 4.1c moves ~3,700 lines of software rasteriser
	 * ({@code WorldController}, {@code Model}, {@code Texture}) that all write through the
	 * {@code static} {@link DrawingArea} substrate. A wrong pixel there is not obvious by
	 * eye and is not something a compiler can see, so "it still looks fine" is not
	 * evidence. This renders a fixed, deterministic workload through the REAL raster
	 * primitives and pins the resulting framebuffer by SHA-256, so any refactor that moves
	 * a single pixel fails here instead of shipping.
	 *
	 * <p><b>Why the workload is synthetic rather than a real scene.</b> The real cache is
	 * runtime-configured and is not in the repo (see the plan's open item), so a
	 * cache-backed scene cannot be rendered headlessly today. This covers the DRAWING
	 * SUBSTRATE - {@link DrawingArea} and {@link Sprite} - which is exactly the shared
	 * surface 4.1c must capture behind the seam. It does NOT yet cover {@code Model} or
	 * {@code WorldController} geometry; that needs a cache fixture and is a separate step.
	 * Stated plainly rather than implied, because a gate that quietly covers less than it
	 * appears to is worse than a smaller honest one.
	 *
	 * <p><b>What it pins.</b> The exact byte content of the framebuffer after the workload,
	 * including clipping behaviour ({@code setDrawingArea}) and the alpha blend path. The
	 * hash is over the buffer's little-endian byte expansion, so it is sensitive to a
	 * change in any channel of any pixel.
	 */
	private static final int RASTER_W = 192;
	private static final int RASTER_H = 128;

	/**
	 * The pinned framebuffer hash.
	 *
	 * <p>Regenerate ONLY when a change to the raster output is intentional and reviewed.
	 * If this test fails, the first assumption should be that the refactor changed a
	 * pixel, not that the constant is stale. To re-pin, set the constant to all zeros,
	 * run the harness, and read the value it prints as {code Observed}.
	 */
	private static final String RASTER_GOLDEN_HASH =
			"775ffc78fd316faf03cbf1499cb9da547f9b12a7b9e6a2117f936c3776a9e39e";

	/**
	 * Runs a fixed raster workload and returns the framebuffer.
	 *
	 * @param perturb when non-zero, one draw operation is deliberately altered, so the
	 *                caller can prove the gate notices a changed draw call.
	 */
	private static int[] rasterWorkload(int perturb) {
		int[] buf = new int[RASTER_W * RASTER_H];
		DrawingArea.initDrawingArea(RASTER_H, RASTER_W, buf);

		DrawingArea.setAllPixels(0x102030);

		// Horizontal lines: the plain clipped fill path.
		for (int i = 0; i < 16; i++) {
			DrawingArea.drawHorizontalLine(i * 7 + 1, 0x204060 + i * 2731, 100 + i * 5, i * 3);
		}

		// Alpha-blended fill: the blend path, which is easy to get subtly wrong.
		DrawingArea.method335(0x8899AA + perturb, 12, 70, 40, 160, 9);

		// A sprite blit and a sub-region blit, with deterministic source pixels.
		Sprite sp = new Sprite(32, 24);
		for (int i = 0; i < sp.myPixels.length; i++) {
			sp.myPixels[i] = 0xFF000000 | (int) ((i * 2654435761L) & 0xFFFFFF);
		}
		sp.drawSprite(8, 8);
		sp.drawSpriteRegion(60, 30, 4, 2, 20, 12);

		// Clipped drawing: a rectangle that overhangs the region must be cut, not wrapped.
		DrawingArea.setDrawingArea(100, 40, 150, 20);
		DrawingArea.drawHorizontalLine(60, 0xF0E0D0, 200, 10);
		DrawingArea.method335(0x123456, 55, 120, 60, 128, 30);
		DrawingArea.defaultDrawingAreaSize();

		return buf;
	}

	private static String framebufferHash(int[] buf) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] bytes = new byte[buf.length * 4];
			for (int i = 0; i < buf.length; i++) {
				bytes[i * 4] = (byte) (buf[i] & 0xff);
				bytes[i * 4 + 1] = (byte) ((buf[i] >>> 8) & 0xff);
				bytes[i * 4 + 2] = (byte) ((buf[i] >>> 16) & 0xff);
				bytes[i * 4 + 3] = (byte) ((buf[i] >>> 24) & 0xff);
			}
			byte[] digest = md.digest(bytes);
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (int i = 0; i < digest.length; i++) {
				int v = digest[i] & 0xff;
				if (v < 16) {
					sb.append('0');
				}
				sb.append(Integer.toHexString(v));
			}
			return sb.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void rasterFramebufferHashIsPinned() {
		String actual = framebufferHash(rasterWorkload(0));
		if (RASTER_GOLDEN_HASH.equals("0000000000000000000000000000000000000000000000000000000000000000")) {
			// Unpinned: report the value so it can be pinned, and FAIL rather than pass
			// quietly, because an unpinned gate that reports success is worthless.
			System.out.println("  NOTE  Raster golden hash not pinned yet. Observed: " + actual);
			check("Raster: framebuffer hash is pinned", false);
			return;
		}
		boolean ok = RASTER_GOLDEN_HASH.equals(actual);
		if (!ok) {
			System.out.println("  NOTE  Raster framebuffer changed. Expected " + RASTER_GOLDEN_HASH);
			System.out.println("  NOTE                              Observed " + actual);
		}
		check("Raster: fixed framebuffer workload hashes to the pinned value (Phase 4.1b gate)", ok);
	}

	/**
	 * A hash that cannot be reproduced is worthless as a gate, so determinism is asserted
	 * rather than assumed: two independent runs of the workload must agree exactly.
	 */
	private static void rasterFramebufferHashIsDeterministic() {
		String first = framebufferHash(rasterWorkload(0));
		String second = framebufferHash(rasterWorkload(0));
		check("Raster: framebuffer workload is deterministic (two runs agree exactly)",
				first.equals(second));
	}

	/**
	 * The teeth proof, and the reason this gate is worth having: a gate that always passes
	 * is worse than no gate. Perturbing ONE draw operation must change the hash, which is
	 * exactly the failure mode a rasteriser refactor would produce.
	 */
	private static void rasterFramebufferHashDetectsAChangedDrawOp() {
		String normal = framebufferHash(rasterWorkload(0));
		String perturbed = framebufferHash(rasterWorkload(1));
		check("Raster: gate has teeth (a single altered draw op changes the hash)",
				!normal.equals(perturbed));
	}

	// ---------------------------------------- triangle rasteriser gate (Phase 4.1c-2a)

	/**
	 * Gate for the scene's triangle rasterisation - the surface a GPU path actually
	 * intercepts, and the one 4.1c-2b will route through a swappable entry.
	 *
	 * <p><b>Why this is separate from the 4.1b substrate gate.</b> That one covers the
	 * generic 2D drawing surface ({@link DrawingArea} + {@link Sprite}). It does NOT
	 * cover the triangle rasterisers, which are what {@code Model.method443} ultimately
	 * calls to paint the 3D scene. Without this, a change to the scene rasteriser would
	 * be gated by nothing but a human looking at the screen - the gap 4.1c-2a exists to
	 * close.
	 *
	 * <p><b>How it drives the real rasteriser without a cache.</b>
	 * {@code Model.method443} delegates to exactly three public entry points -
	 * {@link Texture#method374} (flat), {@link Texture#method376} and
	 * {@link Texture#method378} (textured). This gate drives {@code method374} directly
	 * with fixed integer coordinates, so no {@link Model} has to be constructed: a Model
	 * would require either hand-synthesised model bytes or a cache fixture the repo does
	 * not have.
	 *
	 * <p><b>Setup, and one deliberate trick.</b> {@link Texture#method364} derives the
	 * row-offset table and texture centres from the current {@link DrawingArea} size, so
	 * it must run after {@code initDrawingArea}. {@code Fog.sceneDepth} is set to 0 so
	 * that {@code method374}'s guard ({@code Fog.sceneDepth > 50 && client.fogStrength > 0})
	 * short-circuits on its first operand - which keeps the gate from loading the
	 * {@code client} class at all. That matters: {@code client} has heavy static
	 * initialisers and is not safely loadable headlessly.
	 *
	 * <p><b>Scope.</b> This covers the FLAT path ({@code method374}). The TEXTURED path
	 * ({@code method378}) is covered separately by {@link #texturedTriangleWorkload},
	 * added because the ground draws through {@code method378} and that is reachable in
	 * normal play. {@code method376} is still not driven: it calls {@link Fog#applyFlat}
	 * unconditionally, and {@code applyFlat} reads {@code client.fogStrength}, which
	 * would load the {@code client} class.
	 */
	private static int[] triangleWorkload(int perturb) {
		int[] buf = new int[RASTER_W * RASTER_H];
		DrawingArea.initDrawingArea(RASTER_H, RASTER_W, buf);
		DrawingArea.setAllPixels(0);
		Texture.method364();
		Fog.sceneDepth = 0;
		Texture.anInt1465 = 0;
		Texture.aBoolean1462 = false;
		Texture.aBoolean1464 = true;

		// Install a DETERMINISTIC colour palette.
		//
		// Do NOT call Texture.method372 here: its first statement adds
		//   Math.random() * 0.03 - 0.015
		// to the brightness, so the client's real palette is deliberately jittered on
		// every load and is not reproducible. That is fine for play but fatal for a gate,
		// so the palette is filled deterministically instead. This gate pins the
		// RASTERISER (geometry, span stepping, the write path), not the palette.
		int[] palette = Texture.anIntArray1482;
		for (int i = 0; i < palette.length; i++) {
			int v = ((i & 0xFF) << 16) | (((i >> 3) & 0xFF) << 8) | ((i * 5) & 0xFF);
			palette[i] = (v == 0 ? 1 : v);
		}

		// method374(y0, y1, y2, x0, x1, x2, c0, c1, c2) - flat-shaded triangles.
		// Colours are 16-BIT model face colours (indices into the palette above), NOT
		// 24-bit RGB: passing RGB overruns the 65536-entry palette.
		Texture.method374(10, 90, 50, 12, 40, 150, 0x1234 + perturb, 0x5678, 0x9ABC);
		Texture.method374(20, 100, 40, 30, 170, 80, 0x2468, 0x1357, 0x0F0F);
		Texture.method374(5, 60, 110, 5, 180, 60, 0x7FFF, 0x3FFF, 0xBFFF);

		return buf;
	}

	private static final String TRIANGLE_GOLDEN_HASH =
			"57838498b8af2b9c834b7a82bcd37305712f1014665926449b3aaa9c88732d67";

	private static void triangleRasteriserIsPinned() {
		String actual = framebufferHash(triangleWorkload(0));
		if (TRIANGLE_GOLDEN_HASH.equals("0000000000000000000000000000000000000000000000000000000000000000")) {
			System.out.println("  NOTE  Triangle golden hash not pinned yet. Observed: " + actual);
			check("Triangle rasteriser: framebuffer hash is pinned", false);
			return;
		}
		boolean ok = TRIANGLE_GOLDEN_HASH.equals(actual);
		if (!ok) {
			System.out.println("  NOTE  Triangle rasterisation changed. Expected " + TRIANGLE_GOLDEN_HASH);
			System.out.println("  NOTE                                  Observed " + actual);
		}
		check("Triangle rasteriser: fixed triangle workload hashes to the pinned value", ok);
	}

	private static void triangleRasteriserIsDeterministic() {
		String first = framebufferHash(triangleWorkload(0));
		String second = framebufferHash(triangleWorkload(0));
		check("Triangle rasteriser: workload is deterministic (two runs agree exactly)",
				first.equals(second));
	}

	private static void triangleRasteriserDetectsAChangedTriangle() {
		String normal = framebufferHash(triangleWorkload(0));
		String perturbed = framebufferHash(triangleWorkload(1));
		check("Triangle rasteriser: gate has teeth (one altered triangle colour changes the hash)",
				!normal.equals(perturbed));
	}

	// ------------------------------------ textured triangle gate (extends 4.1c-2a)

	/**
	 * Gate for the TEXTURED triangle rasteriser - {@link Texture#method378}.
	 *
	 * <p><b>Why the flat gate was not enough.</b> 4.1c-2a drives
	 * {@link Texture#method374}, which is the flat path. The ground draws through
	 * {@code method378} at 5 of its 11 rasteriser call sites, and that path is
	 * reachable in normal play - {@code client.java:3812} sets
	 * {@code Texture.lowMem = false} under high detail, which is exactly the
	 * condition enabling it. So without this the textured path would be covered by
	 * nothing but the live screen.
	 *
	 * <p><b>Why {@code method378} and not {@code method376}.</b> {@code method376}
	 * calls {@link Fog#applyFlat} unconditionally, and {@code applyFlat} reads
	 * {@code client.fogStrength} - so it would load the {@code client} class, whose
	 * static initialisers are not headless-safe. {@code method378}'s fog use sits
	 * behind {@code Fog.sceneDepth > 50}, so setting {@code sceneDepth = 0}
	 * short-circuits it and avoids {@code client} entirely.
	 *
	 * <p><b>How the texture is supplied without loading anything.</b>
	 * {@link Texture#method371} returns immediately when
	 * {@code anIntArrayArray1479[tid] != null}, BEFORE it touches
	 * {@code aBackgroundArray1474s} or the free pool. So parking a synthetic texture
	 * array in that slot is enough to drive the real textured span routine
	 * ({@link Texture#method379}) with no cache, no {@code Background} and no loader.
	 */
	private static final int TEXTURED_TEX_ID = 3;

	private static int[] texturedTriangleWorkload(int perturb) {
		int[] buf = new int[RASTER_W * RASTER_H];
		DrawingArea.initDrawingArea(RASTER_H, RASTER_W, buf);
		DrawingArea.setAllPixels(0);
		Texture.method364();
		Fog.sceneDepth = 0;
		Texture.anInt1465 = 0;
		Texture.aBoolean1462 = false;
		Texture.aBoolean1464 = true;

		// Same reasoning as the flat gate: the real palette is jittered per load by
		// Texture.method372 (Math.random()), so it is filled deterministically here.
		int[] palette = Texture.anIntArray1482;
		for (int i = 0; i < palette.length; i++) {
			int v = ((i & 0xFF) << 16) | (((i >> 3) & 0xFF) << 8) | ((i * 5) & 0xFF);
			palette[i] = (v == 0 ? 1 : v);
		}

		// Synthetic texture: parked in the loaded-texture slot so method371 returns it
		// instead of trying to build one from a Background. lowMem is true here (its
		// default), so method379 samples with 64x64 addressing inside 16384 entries.
		//
		// The slot is private, and it is deliberately NOT widened to public: production
		// visibility should not be relaxed to suit a test. The harness already reaches
		// private state by reflection elsewhere, so it does so here too.
		int[] tex = new int[16384];
		for (int i = 0; i < tex.length; i++) {
			tex[i] = (((i * 7) & 0xFF) << 16) | (((i * 3) & 0xFF) << 8) | (i & 0xFF);
		}
		parkTexture(TEXTURED_TEX_ID, tex);

		// method378(y0,y1,y2, x0,x1,x2, c0,c1,c2, sx0,sx1,sx2, sy0,sy1,sy2, sz0,sz1,sz2, tid)
		// y/x are screen coords; colours are 16-bit palette indices; s* are the
		// per-vertex camera-space coords the texture is mapped through.
		Texture.method378(10, 90, 50, 12, 40, 150, 0x1234 + perturb, 0x5678, 0x9ABC,
				100, 300, 200, 120, 400, 250, 300, 500, 350, TEXTURED_TEX_ID);
		Texture.method378(20, 100, 40, 30, 170, 80, 0x2468, 0x1357, 0x0F0F,
				150, 120, 380, 90, 300, 200, 280, 340, 420, TEXTURED_TEX_ID);

		return buf;
	}

	/**
	 * Parks a synthetic texture in {@code Texture}'s loaded-texture slot so that
	 * {@link Texture#method371} returns it at its early return rather than trying to
	 * build one from a {@code Background} (which would need a cache).
	 *
	 * <p>Done by reflection on purpose: the field is private, and relaxing production
	 * visibility to make a test easier is the wrong trade.
	 */
	private static void parkTexture(int tid, int[] tex) {
		try {
			java.lang.reflect.Field f = Texture.class.getDeclaredField("anIntArrayArray1479");
			f.setAccessible(true);
			int[][] loaded = (int[][]) f.get(null);
			loaded[tid] = tex;
		} catch (Exception e) {
			throw new RuntimeException("could not park synthetic texture " + tid, e);
		}
	}

	private static final String TEXTURED_GOLDEN_HASH =
			"018753a038c2c05a895f7c341a176a8f77436093f7c5d419df22154fd721efe1";

	private static void texturedTriangleRasteriserIsPinned() {
		String actual = framebufferHash(texturedTriangleWorkload(0));
		if (TEXTURED_GOLDEN_HASH.startsWith("0000000000000000000000000000000000000000000000000000000000000000")) {
			System.out.println("  NOTE  Textured golden hash not pinned yet. Observed: " + actual);
			check("Textured triangle rasteriser: framebuffer hash is pinned", false);
			return;
		}
		boolean ok = TEXTURED_GOLDEN_HASH.equals(actual);
		if (!ok) {
			System.out.println("  NOTE  Textured rasterisation changed. Expected " + TEXTURED_GOLDEN_HASH);
			System.out.println("  NOTE                                 Observed " + actual);
		}
		check("Textured triangle rasteriser: fixed workload hashes to the pinned value", ok);
	}

	private static void texturedTriangleRasteriserIsDeterministic() {
		String first = framebufferHash(texturedTriangleWorkload(0));
		String second = framebufferHash(texturedTriangleWorkload(0));
		check("Textured triangle rasteriser: workload is deterministic (two runs agree exactly)",
				first.equals(second));
	}

	private static void texturedTriangleRasteriserDetectsAChangedTriangle() {
		String normal = framebufferHash(texturedTriangleWorkload(0));
		String perturbed = framebufferHash(texturedTriangleWorkload(1));
		check("Textured triangle rasteriser: gate has teeth (one altered triangle colour changes the hash)",
				!normal.equals(perturbed));
	}

	// --------------------------------------- scene rasteriser seam (Phase 4.1c-2b)

	/**
	 * Proves the scene seam is INERT until something is installed - i.e. that
	 * adding it cannot have changed what the client draws.
	 */
	private static void sceneRasterizerIsInertByDefault() {
		check("Scene rasteriser: no implementation is installed by default",
				SceneRasterizer.implementation() == null);
		check("Scene rasteriser: dispatch declines when nothing is installed",
				!SceneRasterizer.dispatch(Model.aModel_1621, 0, 0, 0, 0, 0, 0, 0, 0, 0));
	}

	/**
	 * Proves the seam actually intercepts - and, because
	 * {@code Model.aModel_1621} is an empty model that {@code method443} would
	 * otherwise cull and return from before drawing anything, proves the dispatch
	 * sits at the TOP of the method rather than after the culling.
	 *
	 * <p>Installed through {@link GpuRenderer} rather than
	 * {@code SceneRasterizer.install}, which is package-private precisely so the
	 * facade is the only install point - so this exercises the real path.
	 */
	private static void sceneRasterizerInterceptsWhenInstalled() {
		final int[] calls = new int[1];
		GpuRenderer.install(new GpuRenderer.Implementation() {
			public boolean presentGameFrame(RSImageProducer producer, int destX, int destY) {
				return true;
			}

			public boolean drawModel(Model model, int orientation, int camA, int camB, int camC,
					int camD, int dx, int dy, int dz, int uid) {
				calls[0]++;
				return true;
			}

			public boolean drawGroundTriangle(int x0, int y0, int x1, int y1, int x2, int y2,
					int colour0, int colour1, int colour2, int textureId, boolean flatMesh,
					int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7, int t8) {
				calls[0]++;
				return true;
			}
		});
		try {
			Model.aModel_1621.method443(0, 0, 0, 0, 0, 0, 0, 0, 0);
			check("Scene rasteriser: an installed rasteriser intercepts method443",
					calls[0] == 1);
		} finally {
			GpuRenderer.install(null);
		}
		check("Scene rasteriser: uninstalling restores the software path",
				SceneRasterizer.implementation() == null);
	}

	// --------------------------------------------- the renderer facade (Phase 4.2a)

	/**
	 * The facade must be inert with nothing installed - the property that makes
	 * prepending it to the present path safe.
	 *
	 * <p>The producer is deliberately {@code null}: with nothing installed
	 * {@code presentGameFrame} returns {@code false} without touching it, and that
	 * "declines without dereferencing" is part of what is being asserted.
	 */
	private static void gpuRendererIsInertByDefault() {
		check("Renderer facade: no renderer is installed by default",
				GpuRenderer.implementation() == null);
		check("Renderer facade: presentGameFrame declines when nothing is installed",
				!GpuRenderer.presentGameFrame(null, 0, 0));
	}

	/**
	 * The point of the facade: installing once wires BOTH seams, and uninstalling
	 * once clears both, so a caller cannot leave half a renderer installed.
	 */
	private static void gpuRendererWiresBothSeams() {
		final int[] present = new int[1];
		GpuRenderer.install(new GpuRenderer.Implementation() {
			public boolean presentGameFrame(RSImageProducer producer, int destX, int destY) {
				present[0]++;
				return true;
			}

			public boolean drawModel(Model model, int orientation, int camA, int camB, int camC,
					int camD, int dx, int dy, int dz, int uid) {
				return true;
			}

			public boolean drawGroundTriangle(int x0, int y0, int x1, int y1, int x2, int y2,
					int colour0, int colour1, int colour2, int textureId, boolean flatMesh,
					int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7, int t8) {
				return true;
			}
		});
		try {
			check("Renderer facade: one install wires the SCENE seam too",
					SceneRasterizer.implementation() != null
							&& SceneRasterizer.implementation() == GpuRenderer.implementation());
			check("Renderer facade: the present call reaches the installed renderer",
					GpuRenderer.presentGameFrame(null, 1, 2) && present[0] == 1);
		} finally {
			GpuRenderer.install(null);
		}
		check("Renderer facade: one uninstall clears BOTH seams",
				GpuRenderer.implementation() == null && SceneRasterizer.implementation() == null);
	}

	// ------------------------------- renderer selection and the decline (Phase 4.2b)

	/**
	 * A renderer that DECLINES everything must leave the software path untouched.
	 *
	 * <p>This is the property that makes the bring-up placeholder safe, and it is not
	 * hypothetical: the scene seam treats an installed renderer as authoritative unless
	 * it says otherwise, so a placeholder that accepted submissions without drawing them
	 * would make models and ground disappear. Each assertion here is the "says
	 * otherwise" half.
	 */
	private static void decliningRendererFallsThrough() {
		GpuRenderer.install(new RendererConfig.DecliningRenderer("harness-test"));
		try {
			check("Decline contract: a declining renderer does not consume a model",
					!SceneRasterizer.dispatch(Model.aModel_1621, 0, 0, 0, 0, 0, 0, 0, 0, 0));
			check("Decline contract: a declining renderer does not consume a ground triangle",
					!SceneRasterizer.dispatchGroundTriangle(1, 2, 3, 4, 5, 6, 7, 8, 9, -1, false,
							11, 12, 13, 14, 15, 16, 17, 18, 19));
			check("Decline contract: a declining renderer declines the present",
					!GpuRenderer.presentGameFrame(null, 0, 0));
			check("Decline contract: the renderer is installed, so this is DECLINING not absent",
					GpuRenderer.implementation() != null
							&& SceneRasterizer.implementation() != null);
		} finally {
			GpuRenderer.install(null);
		}
	}

	/**
	 * The selection must be its own key and must default to software.
	 *
	 * <p>The key assertion guards the documented trap: {@code openGl} already exists and
	 * selects Java2D's OWN pipeline via {@code sun.java2d.opengl}, which is a different
	 * thing from selecting a renderer of ours. Reusing that key would make the name lie.
	 *
	 * <p>Defaults to software is what makes an absent key harmless on a cache that has
	 * never seen the property.
	 */
	private static void rendererSettingIsItsOwnKey() {
		check("Renderer setting: uses its own key, NOT the openGl key (which is Java2D's pipeline)",
				!RendererConfig.PROPERTY.equalsIgnoreCase("openGl"));
		check("Renderer setting: defaults to software before any selection runs",
				RendererConfig.SOFTWARE.equals(RendererConfig.requestedName()));
		check("Renderer setting: the default leaves the software path installed",
				SceneRasterizer.implementation() == null);
	}

	// ------------------------------------ ground triangle seam (Phase 4.1c-2c)

	/**
	 * The ground seam must be inert with nothing installed.
	 *
	 * <p>This is the property that makes the change safe: {@code dispatchGroundTriangle}
	 * returning false is what leaves the software ground path running unchanged.
	 *
	 * <p><b>What this does NOT prove, stated plainly:</b> that the three hooks sit in the
	 * right place. Driving {@code WorldController.method315}/{@code method316} headlessly
	 * is not possible - both read {@code client.tileMarkers} for tile picking, which loads
	 * the {@code client} class. So hook PLACEMENT (after the picking, mid-loop) is verified
	 * by reading and by the live gate, not here. What is asserted here is the plumbing and
	 * the argument order, which is what a future implementation depends on.
	 */
	private static void groundSeamIsInertByDefault() {
		boolean handled = SceneRasterizer.dispatchGroundTriangle(
				1, 2, 3, 4, 5, 6, 7, 8, 9, -1, false, 11, 12, 13, 14, 15, 16, 17, 18, 19);
		check("Ground rasteriser seam: declines (returns false) when no rasteriser is installed",
				!handled);
	}

	private static void groundSeamInterceptsWhenInstalled() {
		final int[] seen = new int[20];
		final int[] calls = new int[1];
		GpuRenderer.install(new GpuRenderer.Implementation() {
			public boolean presentGameFrame(RSImageProducer producer, int destX, int destY) {
				return true;
			}

			public boolean drawModel(Model model, int orientation, int camA, int camB, int camC,
					int camD, int dx, int dy, int dz, int uid) {
				calls[0] += 1000;
				return true;
			}

			public boolean drawGroundTriangle(int x0, int y0, int x1, int y1, int x2, int y2,
					int colour0, int colour1, int colour2, int textureId, boolean flatMesh,
					int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7, int t8) {
				calls[0]++;
				int[] v = { x0, y0, x1, y1, x2, y2, colour0, colour1, colour2, textureId,
						flatMesh ? 1 : 0, t0, t1, t2, t3, t4, t5, t6, t7, t8 };
				System.arraycopy(v, 0, seen, 0, 20);
				return true;
			}
		});
		try {
			boolean handled = SceneRasterizer.dispatchGroundTriangle(
					10, 20, 30, 40, 50, 60, 70, 80, 90, 7, true, 101, 102, 103, 104, 105, 106,
					107, 108, 109);
			boolean orderOk = seen[0] == 10 && seen[1] == 20 && seen[2] == 30 && seen[3] == 40
					&& seen[4] == 50 && seen[5] == 60 && seen[6] == 70 && seen[7] == 80
					&& seen[8] == 90 && seen[9] == 7 && seen[10] == 1 && seen[11] == 101
					&& seen[19] == 109;
			check("Ground rasteriser seam: an installed rasteriser intercepts the ground triangle",
					handled && calls[0] == 1);
			check("Ground rasteriser seam: the whole payload arrives in order (coords, colours, "
					+ "textureId, flatMesh, t0..t8)", orderOk);
		} finally {
			GpuRenderer.install(null);
		}
		check("Ground rasteriser seam: uninstalling restores the software path",
				SceneRasterizer.implementation() == null);
	}

	// ------------------------------- Phase 5.2 geometry buffers

	/**
	 * The property the buffers exist for: after a warm-up, a steady frame must
	 * reuse the same backing array. Asserted by ARRAY IDENTITY, not by capacity,
	 * because a capacity that happens to match would still hide a reallocation.
	 */
	private static void gpuBufferReusesStorageAfterClear() {
		GpuFloatBuffer b = new GpuFloatBuffer(2);
		b.put(1);
		b.put(2, 3, 4); // forces a grow past the initial capacity

		float[] afterGrowth = b.array();
		int grownCapacity = b.capacity();

		check("GpuFloatBuffer: position counts entries written, not capacity",
				b.position() == 4 && grownCapacity >= 4);

		b.clear();
		check("GpuFloatBuffer: clear resets the position", b.position() == 0 && b.isEmpty());
		check("GpuFloatBuffer: clear KEEPS the capacity (emptying must not mean freeing)",
				b.capacity() == grownCapacity);

		b.put(1);
		b.put(2, 3, 4);
		check("GpuFloatBuffer: refilling to the same size reuses the SAME array - no reallocation",
				b.array() == afterGrowth);
		check("GpuFloatBuffer: ... and the reused buffer is filled identically",
				b.position() == 4 && b.get(0) == 1 && b.get(3) == 4);
	}

	private static void gpuBufferGrowsAndPreservesContents() {
		GpuFloatBuffer b = new GpuFloatBuffer(2);
		b.put(10);
		b.put(20);
		b.put(30); // grows here
		check("GpuFloatBuffer: grows on demand rather than failing",
				b.capacity() >= 3 && b.position() == 3);
		check("GpuFloatBuffer: growth preserves the entries already written",
				b.get(0) == 10 && b.get(1) == 20 && b.get(2) == 30);
	}

	private static void gpuBufferEnsureCapacityIsMonotonic() {
		GpuFloatBuffer b = new GpuFloatBuffer(16);
		b.ensureCapacity(4);
		check("GpuFloatBuffer: ensureCapacity below the current capacity is a no-op",
				b.capacity() == 16);
		b.ensureCapacity(100);
		int grown = b.capacity();
		check("GpuFloatBuffer: ensureCapacity grows when the request exceeds capacity",
				grown >= 100);
		b.ensureCapacity(5);
		check("GpuFloatBuffer: ensureCapacity does not shrink back on a smaller request",
				b.capacity() == grown);
		check("GpuFloatBuffer: ensureCapacity does not disturb the write position",
				b.position() == 0);
	}

	/**
	 * A buffer that returns stale data past its position is the classic silent
	 * corruption: the upload reads garbage, and nothing throws. Failing fast is the
	 * whole point of the bound check.
	 */
	private static void gpuBufferFailsFastOnBadSizesAndIndices() {
		boolean threw = false;
		try {
			new GpuFloatBuffer(0);
		} catch (IllegalArgumentException e) {
			threw = true;
		}
		check("GpuFloatBuffer: rejects a non-positive initial capacity", threw);

		GpuFloatBuffer b = new GpuFloatBuffer(8);
		b.put(1, 2);

		threw = false;
		try {
			b.get(2);
		} catch (IndexOutOfBoundsException e) {
			threw = true;
		}
		check("GpuFloatBuffer: reading past the position fails fast instead of returning stale data", threw);

		threw = false;
		try {
			b.get(-1);
		} catch (IndexOutOfBoundsException e) {
			threw = true;
		}
		check("GpuFloatBuffer: a negative index fails fast", threw);
	}

	private static void gpuBufferBulkCopyHonoursTheWindow() {
		GpuFloatBuffer b = new GpuFloatBuffer(16);
		b.put(new float[] { 1, 2, 3, 4, 5 }, 1, 3);
		check("GpuFloatBuffer: bulk copy takes exactly the requested window, not the whole array",
				b.position() == 3 && b.get(0) == 2 && b.get(1) == 3 && b.get(2) == 4);
		b.put(new float[] { 9 }, 0, 0);
		check("GpuFloatBuffer: a zero-length bulk copy is a no-op", b.position() == 3);

		GpuFloatBuffer grown = new GpuFloatBuffer(2);
		grown.put(new float[] { 1, 2, 3, 4, 5, 6 }, 0, 6);
		check("GpuFloatBuffer: bulk copy grows the array when the window does not fit, preserving data",
				grown.position() == 6 && grown.get(0) == 1 && grown.get(5) == 6);
	}

	/**
	 * The divergence guard. The two buffers are near-identical classes with no
	 * shared base, so nothing but a test stops them drifting apart. Run the same
	 * sequence through both and require identical reported behaviour.
	 */
	private static void gpuIntBufferMirrorsTheFloatBuffer() {
		GpuFloatBuffer f = new GpuFloatBuffer(2);
		GpuIntBuffer i = new GpuIntBuffer(2);
		for (int n = 1; n <= 40; n++) {
			f.put(n);
			i.put(n);
		}
		check("GpuIntBuffer: position matches GpuFloatBuffer for an identical write sequence",
				i.position() == f.position());
		check("GpuIntBuffer: capacity matches GpuFloatBuffer for an identical write sequence (same growth policy)",
				i.capacity() == f.capacity());

		float[] fArray = f.array();
		int[] iArray = i.array();
		f.clear();
		i.clear();
		check("GpuIntBuffer: clear keeps capacity, exactly as the float buffer does",
				i.capacity() == f.capacity() && i.position() == 0 && f.position() == 0);
		check("Gpu buffers: clear does not reallocate, in EITHER buffer",
				i.array() == iArray && f.array() == fArray);

		GpuIntBuffer t = new GpuIntBuffer(4);
		t.putTriangle(7, 8, 9);
		check("GpuIntBuffer: putTriangle appends three indices in winding order",
				t.position() == 3 && t.get(0) == 7 && t.get(1) == 8 && t.get(2) == 9);

		boolean threw = false;
		try {
			new GpuIntBuffer(0);
		} catch (IllegalArgumentException e) {
			threw = true;
		}
		check("GpuIntBuffer: rejects a non-positive initial capacity", threw);

		threw = false;
		try {
			t.get(3);
		} catch (IndexOutOfBoundsException e) {
			threw = true;
		}
		check("GpuIntBuffer: reading past the position fails fast", threw);
	}

	// ------------------------------- Phase 5.3 real-format geometry cross-check

	/** A model id far above anything the client loads, so the parse cannot collide. */
	private static final int FIXTURE_MODEL_ID = 70000;

	/** The vertex positions the fixture encodes, in x/y/z order. */
	private static final int[][] FIXTURE_VERTICES = {
			{ 1, 10, 100 }, { 2, 20, 200 }, { 3, 30, 300 }
	};

	/** Face 0 is FLAT (render type 0) so its colour slot really is a colour. */
	private static final int FIXTURE_FLAT_COLOUR = 0xF800;
	/** Face 1 is TEXTURED (render type 2), texture-coordinate index 0, texture id 0. */
	private static final int FIXTURE_TEXTURE_ID = 0;

	/**
	 * Builds a model in the client's REAL 622-era file format.
	 *
	 * <p><b>Why craft bytes rather than assign fields.</b> Phase 5.1's tests wrote the
	 * private arrays directly, which proves the accessors read <i>those fields</i> but
	 * cannot prove the fields are the <i>right ones</i> - a self-consistent swap would
	 * pass. Here the PARSER decides which field holds x, y and z, following the real
	 * format; so if an accessor reads the wrong array, the values come back permuted
	 * and the assertions fail. That is the gap 5.1 explicitly left open, and closing it
	 * is what exposed the texture-id convention.
	 *
	 * <p><b>Two faces on purpose, one of each kind:</b> a flat face (whose colour slot
	 * holds a colour) and a textured face (whose colour slot holds a TEXTURE ID). A
	 * single-face fixture of either kind cannot tell the two conventions apart, which is
	 * how the original texture-id mistake survived Phase 5.1.
	 *
	 * <p><b>Layout, from {@code method460} + {@code readOldModel} (not guessed).</b>
	 * The body is laid out in exactly the order {@code method460} computes its offsets,
	 * and the trailing 18 bytes are the footer it reads from {@code length - 18}:
	 * <pre>
	 *   0..2   vertex flags (bit0=x delta, bit1=y delta, bit2=z delta)
	 *   3..4   face index types (1 = three explicit deltas)
	 *   5..6   face render types (0 = flat, 2 = textured-gouraud)
	 *   7..12  face index deltas        (method421 encoded)
	 *   13..16 face colour slots         (flat colour, then the texture id)
	 *   17..22 texture coordinates       (3 words: vertex indices 0,1,2)
	 *   23..25 x deltas                  (method421 encoded)
	 *   26..28 y deltas
	 *   29..34 z deltas                  (100 needs the two-byte form)
	 *   35..52 footer: counts, 5 presence flags, 4 stream lengths
	 * </pre>
	 * The last footer word is 0x0006, so the final two bytes are not 0xFF 0xFF and the
	 * parse takes the <b>old</b> format branch - which is what the offsets assume.
	 *
	 * <p>Face topology is delta-coded and cumulative: face 0's deltas (0,1,1) give
	 * vertices 0,1,2; face 1's deltas (0,-1,-1) then give 2,1,0, which exercises the
	 * cumulative decode rather than just repeating one triangle.
	 */
	private static byte[] buildOldFormatFixture() {
		byte[] b = new byte[53];

		// Vertex flags: all three deltas present for all three vertices.
		b[0] = 7; b[1] = 7; b[2] = 7;
		// Both face index types are 1 (three explicit deltas).
		b[3] = 1; b[4] = 1;
		// Face 0 flat, face 1 textured.
		b[5] = 0; b[6] = 2;
		// Face 0 deltas 0,1,1 -> vertices 0,1,2. Face 1 deltas 0,-1,-1 -> 2,1,0.
		b[7] = delta(0); b[8] = delta(1); b[9] = delta(1);
		b[10] = delta(0); b[11] = delta(-1); b[12] = delta(-1);
		// Face 0 colour, big-endian; face 1's slot holds the texture id.
		putWord(b, 13, FIXTURE_FLAT_COLOUR);
		putWord(b, 15, FIXTURE_TEXTURE_ID);
		// Texture coordinates: vertex indices 0, 1, 2.
		putWord(b, 17, 0); putWord(b, 19, 1); putWord(b, 21, 2);
		// x deltas of 1, y of 10, z of 100 - cumulative, so positions differ per vertex.
		b[23] = delta(1); b[24] = delta(1); b[25] = delta(1);
		b[26] = delta(10); b[27] = delta(10); b[28] = delta(10);
		putDelta2(b, 29, 100); putDelta2(b, 31, 100); putDelta2(b, 33, 100);

		// Footer at 35: counts, then the five presence flags, then four stream lengths.
		putWord(b, 35, 3);           // vertex count
		putWord(b, 37, 2);           // face count
		b[39] = 1;                   // texture count
		b[40] = 1;                   // render types present
		b[41] = 0;                   // priorities absent
		b[42] = 0;                   // alphas absent
		b[43] = 0;                   // texture pointers absent
		b[44] = 0;                   // skin absent
		putWord(b, 45, 3);           // x delta stream length
		putWord(b, 47, 3);           // y delta stream length
		putWord(b, 49, 6);           // z delta stream length
		putWord(b, 51, 6);           // face index stream length
		return b;
	}

	/**
	 * Encodes one delta in {@code Stream.method421}'s one-byte form: a byte below 128
	 * is read back as {@code value - 64}. Only valid for deltas in [-64, 63].
	 */
	private static byte delta(int d) {
		if (d < -64 || d > 63) {
			throw new IllegalArgumentException("delta " + d + " needs the two-byte form: " + d);
		}
		return (byte) (d + 64);
	}

	/** The two-byte form: the first byte must be >= 128, and it reads back as {@code word - 49152}. */
	private static void putDelta2(byte[] b, int off, int d) {
		int word = d + 49152;
		b[off] = (byte) (word >> 8);
		b[off + 1] = (byte) word;
		if ((b[off] & 0xFF) < 128) {
			throw new IllegalArgumentException("delta " + d + " would encode as a one-byte value");
		}
	}

	private static void putWord(byte[] b, int off, int value) {
		b[off] = (byte) (value >> 8);
		b[off + 1] = (byte) value;
	}

	/** Big-endian word at a moving cursor; returns the cursor past the word written. */
	private static int word(byte[] b, int off, int value) {
		putWord(b, off, value);
		return off + 2;
	}

	/**
	 * Parses the fixture through the client's REAL loader and returns the built model.
	 *
	 * <p>Uses {@code Model.method459} to initialise the model store, {@code method460}
	 * to parse, and the private {@code Model(int)} constructor - the same three steps
	 * the client takes at runtime. {@code null} is passed for the on-demand fetcher,
	 * which {@code method459} merely stores.
	 */
	private static Model parseFixtureModel() {
		try {
			Model.method459(FIXTURE_MODEL_ID, null);
			Model.method460(buildOldFormatFixture(), FIXTURE_MODEL_ID);
			java.lang.reflect.Constructor<Model> c =
					Model.class.getDeclaredConstructor(int.class);
			c.setAccessible(true);
			return c.newInstance(FIXTURE_MODEL_ID);
		} catch (Exception e) {
			throw new RuntimeException("could not parse the fixture model", e);
		}
	}

	private static Object readStatic(Class<?> owner, String name) {
		try {
			java.lang.reflect.Field f = owner.getDeclaredField(name);
			f.setAccessible(true);
			return f.get(null);
		} catch (Exception e) {
			throw new RuntimeException("could not read static field " + name, e);
		}
	}

	private static void writeStatic(Class<?> owner, String name, Object value) {
		try {
			java.lang.reflect.Field f = owner.getDeclaredField(name);
			f.setAccessible(true);
			f.set(null, value);
		} catch (Exception e) {
			throw new RuntimeException("could not write static field " + name, e);
		}
	}

	/**
	 * The headline check: the exported geometry equals what the REAL PARSER produced.
	 *
	 * <p>Exact values, not ranges. The parser places each delta stream into a specific
	 * field (x from one stream, y from another, z from a third), so a permuted mapping
	 * in the accessors cannot produce these values.
	 */
	private static void realModelExportMatchesTheParsedFormat() {
		Model m = parseFixtureModel();

		check("Real model: counts come from the parsed footer, not from defaults",
				m.vertexCount() == 3 && m.faceCount() == 2 && m.textureCount() == 1);

		int[] xs = m.vertexXs();
		int[] ys = m.vertexYs();
		int[] zs = m.vertexZs();
		boolean positionsMatch = xs.length == 3 && ys.length == 3 && zs.length == 3;
		for (int i = 0; positionsMatch && i < 3; i++) {
			positionsMatch = xs[i] == FIXTURE_VERTICES[i][0]
					&& ys[i] == FIXTURE_VERTICES[i][1]
					&& zs[i] == FIXTURE_VERTICES[i][2];
		}
		check("Real model: exported vertices equal the encoded positions, x/y/z in the right fields"
				+ (positionsMatch ? "" : " - got x=" + java.util.Arrays.toString(xs)
						+ " y=" + java.util.Arrays.toString(ys) + " z=" + java.util.Arrays.toString(zs)),
				positionsMatch);

		check("Real model: vertex arrays are sized by the parsed vertex count",
				xs.length == m.vertexCount());
		check("Real model: face arrays are sized by the parsed face count",
				m.faceVertexA().length == m.faceCount()
						&& m.faceVertexB().length == m.faceCount()
						&& m.faceVertexC().length == m.faceCount());

		check("Real model: face 0's topology is what the parser decoded (0,1,2)",
				m.faceVertexA()[0] == 0 && m.faceVertexB()[0] == 1 && m.faceVertexC()[0] == 2);
		check("Real model: face 1's topology follows the CUMULATIVE delta decode (2,1,0)",
				m.faceVertexA()[1] == 2 && m.faceVertexB()[1] == 1 && m.faceVertexC()[1] == 0);
		check("Real model: texture coords are the encoded vertex indices",
				m.hasTextures() && m.textureVertexA()[0] == 0
						&& m.textureVertexB()[0] == 1 && m.textureVertexC()[0] == 2);
		check("Real model: an absent attribute is null, not a zero-filled array (alphas)",
				m.faceAlphas() == null);
		check("Real model: an absent attribute is null, not a zero-filled array (priorities)",
				m.facePriorities() == null);
	}

	/**
	 * The invariant the rasteriser depends on and never checks itself.
	 *
	 * <p>{@code method484} reads {@code anIntArray1666[anIntArray1631[i]]} with no bounds
	 * test, so an out-of-range index would read past the scratch array rather than throw.
	 * Asserting every exported index is within the vertex count is therefore a property
	 * of the EXPORT that the rasteriser's safety rests on.
	 */
	private static void realModelExportIndicesAreInRangeForTheRasteriser() {
		Model m = parseFixtureModel();
		int vc = m.vertexCount();
		boolean ok = true;
		for (int i = 0; i < m.faceCount(); i++) {
			int a = m.faceVertexA()[i];
			int b = m.faceVertexB()[i];
			int c = m.faceVertexC()[i];
			if (a < 0 || b < 0 || c < 0 || a >= vc || b >= vc || c >= vc) {
				ok = false;
			}
		}
		check("Real model: every face index is inside the vertex range the rasteriser indexes unchecked",
				ok);
	}

	/**
	 * The export's decode must agree with the expressions {@code method484} uses, and the
	 * ids it hands the rasteriser must resolve.
	 *
	 * <p>⚠️ Honest about what this is: the draw routine is {@code word & 3} and the
	 * coordinate index is {@code word >> 2}, and the accessors use the same expressions,
	 * so that agreement is a <b>drift guard</b> rather than independent evidence. The
	 * substantive assertions are the literal expected values - and, above all, that the
	 * texture ID comes from the COLOUR slot, which is the convention Phase 5.1 got wrong
	 * and which this check is what caught.
	 */
	private static void realModelExportDecodeMatchesTheRasteriser() {
		Model m = parseFixtureModel();

		// Quoted from method484: it branches on `anIntArray1637[i] & 3` for the draw
		// routine, uses `anIntArray1637[i] >> 2` to pick the texture-COORDINATE entry,
		// and passes `anIntArray1640[i]` to method378 as the texture TO SAMPLE.
		int face0 = m.faceRenderTypes()[0];
		int face1 = m.faceRenderTypes()[1];

		check("Real model: face 0's render type decodes to the encoded value (0 = flat)",
				m.faceRenderType(0) == 0);
		check("Real model: face 1's render type decodes to the encoded value (2 = textured)",
				m.faceRenderType(1) == 2);

		check("Real model: a FLAT face's colour slot is a colour, as the format intends",
				m.faceBaseColours()[0] == FIXTURE_FLAT_COLOUR);
		check("Real model: a FLAT face reports NO texture id, even though the slot is populated",
				m.faceTextureId(0) == -1);

		check("Real model: a TEXTURED face's texture id comes from the COLOUR slot, not the render word",
				m.faceTextureId(1) == FIXTURE_TEXTURE_ID);
		check("Real model: ... and its texture-coordinate index is the render word shifted right by 2",
				m.faceTextureIndex(1) == (face1 >> 2));

		check("Real model: the export's draw-routine decode agrees with the rasteriser's own expression",
				m.faceRenderType(0) == (face0 & 3) && m.faceRenderType(1) == (face1 & 3));

		boolean resolves = true;
		for (int i = 0; i < m.faceCount(); i++) {
			int tid = m.faceTextureId(i);
			int coord = m.faceTextureIndex(i);
			if (tid >= 0 && tid >= 4096) {
				resolves = false;
			}
			if (coord >= 0 && coord >= m.textureCount()) {
				resolves = false;
			}
		}
		check("Real model: every texture id and coordinate index resolves inside the texture arrays",
				resolves);
	}

	/**
	 * The integration check: the parsed model's own face data drives the REAL per-face
	 * rasteriser, and it plots pixels.
	 *
	 * <p>Drives {@code method484} - the routine {@code method443} calls per face - for
	 * BOTH faces, so both rasteriser paths are exercised from parsed data: face 0 is flat
	 * ({@code method374}, colours only) and face 1 is textured ({@code method378}, which
	 * resolves its texture through {@link Model#faceTextureId}).
	 *
	 * <p>⚠️ <b>This is the check that caught the texture-id mistake.</b> The first draft
	 * encoded a textured face whose colour slot held 0xF800, which the rasteriser then
	 * used as a texture id and blew up on - proving the slot is not a colour for textured
	 * faces. A parse-and-inspect test cannot find that, because the export and the parser
	 * agreed with each other; only driving the real draw disputes it.
	 *
	 * <p>Screen-space and camera-space coordinates are parked rather than transformed, so
	 * the test does not depend on a camera. That is deliberate: the question here is
	 * whether the model's face data drives the rasteriser, not whether projection works.
	 */
	private static void realModelExportDrivesTheRasteriser() {
		Model m = parseFixtureModel();

		int w = 256;
		int h = 256;
		int[] buf = new int[w * h];
		DrawingArea.initDrawingArea(h, w, buf);
		DrawingArea.setAllPixels(0);
		Texture.method364();
		Fog.sceneDepth = 0;
		Texture.anInt1465 = 0;
		Texture.aBoolean1462 = false;
		Texture.aBoolean1464 = true;

		// Same reasoning as the 4.1c-2a gate: Texture.method372 jitters the palette with
		// Math.random(), so fill it deterministically here.
		int[] palette = Texture.anIntArray1482;
		for (int i = 0; i < palette.length; i++) {
			int v = ((i & 0xFF) << 16) | (((i >> 3) & 0xFF) << 8) | ((i * 5) & 0xFF);
			palette[i] = (v == 0 ? 1 : v);
		}

		// Park a texture at the id the parsed model names for its textured face, so
		// method371 returns it instead of trying to build one from a Background.
		int[] tex = new int[16384];
		for (int i = 0; i < tex.length; i++) {
			tex[i] = (((i * 7) & 0xFF) << 16) | (((i * 3) & 0xFF) << 8) | (i & 0xFF);
		}
		parkTexture(m.faceTextureId(1), tex);

		// The transform would normally fill these; park a visible triangle instead, in the
		// slots the model's own vertex and texture-coordinate indices tell method484 to read.
		int[] xs = (int[]) readStatic(Model.class, "anIntArray1665");
		int[] ys = (int[]) readStatic(Model.class, "anIntArray1666");
		int[] sx = (int[]) readStatic(Model.class, "anIntArray1668");
		int[] sy = (int[]) readStatic(Model.class, "anIntArray1669");
		int[] sz = (int[]) readStatic(Model.class, "anIntArray1670");
		for (int i = 0; i < 3; i++) {
			xs[i] = new int[] { 40, 160, 100 }[i];
			ys[i] = new int[] { 20, 30, 150 }[i];
			sx[i] = new int[] { 100, 300, 200 }[i];
			sy[i] = new int[] { 120, 400, 250 }[i];
			sz[i] = new int[] { 300, 500, 350 }[i];
		}
		// The lit colours normally come from method479; supply them directly so this test
		// is about rasterisation rather than lighting.
		writeField(m, "anIntArray1634", new int[] { 0x1234, 0x2345 });
		writeField(m, "anIntArray1635", new int[] { 0x5678, 0x6789 });
		writeField(m, "anIntArray1636", new int[] { 0x9ABC, 0xABCD });

		java.lang.reflect.Method draw;
		try {
			draw = Model.class.getDeclaredMethod("method484", int.class);
			draw.setAccessible(true);
		} catch (Exception e) {
			check("Real model: could reach the per-face rasteriser entry point", false);
			return;
		}

		String beforeFlat = framebufferHash(buf);
		try {
			draw.invoke(m, 0);
		} catch (Exception e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			check("Real model: the rasteriser draws the parsed FLAT face without error"
					+ " - got " + cause.getClass().getName() + ": " + cause.getMessage(), false);
			return;
		}
		check("Real model: the rasteriser draws the parsed flat face without error", true);
		String afterFlat = framebufferHash(buf);
		check("Real model: the flat face's draw plotted pixels", !beforeFlat.equals(afterFlat));

		String beforeTextured = framebufferHash(buf);
		try {
			draw.invoke(m, 1);
		} catch (Exception e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			check("Real model: the rasteriser draws the parsed TEXTURED face without error"
					+ " - got " + cause.getClass().getName() + ": " + cause.getMessage(), false);
			return;
		}
		check("Real model: the rasteriser draws the parsed textured face without error"
				+ " (its texture id resolved from the colour slot)", true);
		check("Real model: the textured face's draw plotted pixels",
				!beforeTextured.equals(framebufferHash(buf)));
	}

	// ------------------- Phase 6.1 frame-slot budget (the invariant, enforced)

	/**
	 * The reason the CursePack lives at 28000+ instead of under its own file ids.
	 *
	 * <p>The seven 667 animation files it needs are 2998, 3012, 3013, 3016, 3018, 3019 and
	 * 3020 - every one of which sits INSIDE the packed 474 frame range (0..3229, measured
	 * from `Frames.dat`). Loading them under their own ids would OVERWRITE 474 content, so
	 * {@code remapSequenceFrames} rewrites each sequence's frame keys to point at
	 * {@code ANIM_FILE_BASE + i}.
	 */
	private static void curseFrameRemapRedirectsCurseFilesToTheHighSlots() {
		Animation anim = new Animation();
		anim.anIntArray353 = new int[] { (2998 << 16) | 5, (3020 << 16) | 7 };

		remapSequenceFrames(anim);

		int expected0 = ((28000 + 0) << 16) | 5;  // 2998 is CURSE_ANIM_FILES[0]
		int expected1 = ((28000 + 6) << 16) | 7;  // 3020 is CURSE_ANIM_FILES[6]
		check("CursePack: a curse sequence's frame key is remapped to slot 28000+i (2998 -> 28000)",
				anim.anIntArray353[0] == expected0);
		check("CursePack: ... and the LAST curse file is remapped too (3020 -> 28006)",
				anim.anIntArray353[1] == expected1);
		check("CursePack: the frame INDEX survives the remap (only the file id moves)",
				(anim.anIntArray353[0] & 0xffff) == 5 && (anim.anIntArray353[1] & 0xffff) == 7);
	}

	/**
	 * The other half of the invariant, and the half that protects 474 content: a frame key
	 * that does NOT name a CursePack source file must be left EXACTLY as it was.
	 *
	 * <p>1777 is deliberate - it is a real loose frame file in the cache root, so if the
	 * remap ever grew a catch-all it would silently steal a slot that 474/loose content is
	 * using.
	 */
	private static void curseFrameRemapLeavesOriginalFrameSlotsAlone() {
		Animation anim = new Animation();
		int original = (1777 << 16) | 62;
		anim.anIntArray353 = new int[] { original, -1, 0 };

		remapSequenceFrames(anim);

		check("CursePack: a NON-curse frame key is left exactly as it was (it never steals an OG slot)",
				anim.anIntArray353[0] == original);
		check("CursePack: a -1 frame key is skipped rather than remapped high",
				anim.anIntArray353[1] == -1);
		check("CursePack: a 0 frame key is skipped too", anim.anIntArray353[2] == 0);
	}

	/**
	 * The bounds that keep the high slots high: {@code loadAnimationFile} refuses any id
	 * outside the remap window BEFORE it touches the file system.
	 *
	 * <p>Only out-of-range ids are asserted, so this does not depend on whether a CursePack
	 * happens to be installed.
	 */
	private static void curseAnimationLoaderRefusesSlotsOutsideTheRemapRange() {
		check("CursePack: loadAnimationFile refuses 1777 - an OG/loose frame id, not a curse slot",
				!CurseData667.loadAnimationFile(1777));
		check("CursePack: loadAnimationFile refuses the slot just BELOW the window (27999)",
				!CurseData667.loadAnimationFile(27999));
		check("CursePack: loadAnimationFile refuses the slot just ABOVE the window (28007)",
				!CurseData667.loadAnimationFile(28007));
		check("CursePack: loadAnimationFile refuses a negative id", !CurseData667.loadAnimationFile(-1));
	}

	/**
	 * Pins the base and the source ids themselves, because the SEPARATION of the two ranges
	 * is the invariant - not any one constant in isolation.
	 */
	private static void curseFrameSlotsCannotCollideWithTheOriginalRange() {
		int base = FrameSlots.CURSE_BASE;
		int[] files = (int[]) readStatic(CurseData667.class, "CURSE_ANIM_FILES");

		check("CursePack: ANIM_FILE_BASE is 28000 (the whole invariant is this number)",
				base == 28000);
		check("CursePack: there are 7 curse source animation files", files.length == 7);
		check("CursePack: FrameSlots.CURSE_COUNT agrees with the CURSE_ANIM_FILES array,"
				+ " so the budget and the data cannot drift apart",
				files.length == FrameSlots.CURSE_COUNT);

		int maxSource = 0;
		for (int i = 0; i < files.length; i++) {
			if (files[i] > maxSource) {
				maxSource = files[i];
			}
		}
		check("CursePack: every curse SOURCE id is below the base, so the ranges are disjoint"
				+ " (max source " + maxSource + " < " + base + ")", maxSource < base);
		check("CursePack: the dest window is [" + base + ", " + (base + files.length)
				+ ") and its top is the last remapped slot",
				base + files.length - 1 == 28006);
	}

	/** Invokes CurseData667's private remap, which is otherwise only reachable from init. */
	private static void remapSequenceFrames(Animation anim) {
		try {
			java.lang.reflect.Method m = CurseData667.class.getDeclaredMethod(
					"remapSequenceFrames", Animation.class);
			m.setAccessible(true);
			m.invoke(null, anim);
		} catch (Exception e) {
			throw new RuntimeException("could not invoke remapSequenceFrames", e);
		}
	}

	// ------------------- Phase 6.2 animation transform contract (the safety net)

	/**
	 * Frame file ids above every real range the loader can reach: packed 474 occupies
	 * 0..3229, the loose files are ~1.7k-3.5k, and the CursePack owns 28000..28006. The
	 * key must also keep {@code (file << 16)} inside a POSITIVE int, i.e. file <= 32767.
	 */
	private static final int FIXTURE_ANIM_FILE_TRANSLATE = 30000;
	private static final int FIXTURE_ANIM_FILE_SCALE = 30001;

	/** Label per vertex: 0 and 1 share label 5, vertex 2 gets label 6. */
	private static final int FIXTURE_SKIN_LABELS[] = { 5, 5, 6 };
	/**
	 * The origin opcode 0 must compute: the mean of the label-5 vertices (0,0,0) and
	 * (10,20,30), which is (5,10,15), PLUS the frame's own encoded delta (1,2,3).
	 * ⚠️ The delta is non-zero on purpose - with a zero delta, dropping the "+ delta" term
	 * in opcode 0 changes nothing and the assertion silently stops testing it.
	 */
	private static final int FIXTURE_ORIGIN[] = { 6, 12, 18 };
	/** Vertex 2 before any transform. */
	private static final int FIXTURE_VERTEX2[] = { 20, 30, 40 };

	private static final int FIXTURE_SKIN_MODEL_ID = 70010;

	/**
	 * A model in the REAL old format that additionally carries a SKIN stream, so the
	 * animation pipeline has labels to transform.
	 *
	 * <p>⚠️ The skin stream is the whole point: `method470` is a no-op without
	 * `anIntArrayArray1657`, and that is only built by {@code method469} from the
	 * per-vertex labels this fixture encodes. Setting the arrays by hand would prove the
	 * transform reads the fields the test wrote; parsing them proves the fields are the
	 * right ones - the same distinction 5.3's real-format parse was built to make.
	 *
	 * <pre>
	 *   0..2   vertex flags (bit0=x delta, bit1=y, bit2=z)
	 *   3      face index type (1 = three explicit deltas)
	 *   4..6   SKIN: one LABEL per vertex
	 *   7..9   face index deltas
	 *   10..11 face colour
	 *   12..20 x/y/z delta streams   (positions 0,10,20 / 0,20,30 / 0,30,40)
	 *   21..38 footer, with the 5th flag (k1) set so the SKIN stream is present
	 * </pre>
	 */
	private static byte[] buildSkinModelFixture() {
		byte[] b = new byte[39];

		b[0] = 7; b[1] = 7; b[2] = 7;          // all three deltas present
		b[3] = 1;                               // face index type 1
		b[4] = (byte) FIXTURE_SKIN_LABELS[0];   // SKIN stream
		b[5] = (byte) FIXTURE_SKIN_LABELS[1];
		b[6] = (byte) FIXTURE_SKIN_LABELS[2];
		b[7] = delta(0); b[8] = delta(1); b[9] = delta(1);   // face 0 -> vertices 0,1,2
		putWord(b, 10, 0xF800);                 // face colour
		b[12] = delta(0); b[13] = delta(10); b[14] = delta(10);
		b[15] = delta(0); b[16] = delta(20); b[17] = delta(10);
		b[18] = delta(0); b[19] = delta(30); b[20] = delta(10);

		putWord(b, 21, 3);   // vertex count
		putWord(b, 23, 1);   // face count
		b[25] = 0;           // texture count
		b[26] = 0;           // k  - render types ABSENT
		b[27] = 0;           // l  - priorities absent
		b[28] = 0;           // i1 - alphas absent
		b[29] = 0;           // j1 - texture pointers absent
		b[30] = 1;           // k1 - SKIN PRESENT
		putWord(b, 31, 3);   // x delta stream length
		putWord(b, 33, 3);   // y delta stream length
		putWord(b, 35, 3);   // z delta stream length
		putWord(b, 37, 3);   // face index stream length
		return b;
	}

	private static Model parseSkinModel() {
		try {
			Model.method459(FIXTURE_SKIN_MODEL_ID, null);
			Model.method460(buildSkinModelFixture(), FIXTURE_SKIN_MODEL_ID);
			java.lang.reflect.Constructor<Model> c =
					Model.class.getDeclaredConstructor(int.class);
			c.setAccessible(true);
			Model m = c.newInstance(FIXTURE_SKIN_MODEL_ID);
			// method469 is what turns the per-vertex labels into the label->vertices map
			// that method470 needs; the live client calls it at Player.java:300.
			m.method469();
			return m;
		} catch (Exception e) {
			throw new RuntimeException("could not parse the skin fixture", e);
		}
	}

	/**
	 * A frame file in the REAL format, containing ONE frame that sets the origin from
	 * label 5 and then applies {@code label6Opcode} to label 6.
	 *
	 * <p>Layout from {@code Frames.decodeFrames}: a Skin, then a frame count, then per
	 * frame a frame index, a group count, and one flag byte per label - with 2-byte
	 * deltas present only when the corresponding flag bit is set.
	 */
	private static byte[] buildFramesFixture(int label6Opcode, int dx, int dy, int dz) {
		byte[] b = new byte[58];
		int p = 0;

		p = word(b, p, 7);                                     // label count
		for (int i = 0; i < 7; i++) {
			p = word(b, p, i == 6 ? label6Opcode : 0);          // opcode per label
		}
		for (int i = 0; i < 7; i++) {
			p = word(b, p, i >= 5 ? 1 : 0);                    // member count per label
		}
		p = word(b, p, 5);                                     // label 5's member: label id 5
		p = word(b, p, 6);                                     // label 6's member: label id 6

		p = word(b, p, 1);                                     // frame count
		p = word(b, p, 0);                                     // frame index 0
		b[p++] = 7;                                               // group count: labels 0..6
		for (int i = 0; i < 5; i++) {
			b[p++] = 0;                                           // labels 0..4 inactive
		}
		b[p++] = 7;                                               // label 5: all three deltas
		p = word(b, p, 1);                                        // ...deliberately NON-ZERO, so the
		p = word(b, p, 2);                                        //    "+ delta" term in opcode 0 is
		p = word(b, p, 3);                                        //    actually exercised
		b[p++] = 7;                                               // label 6: all three deltas
		p = word(b, p, dx);
		p = word(b, p, dy);
		p = word(b, p, dz);
		if (p != b.length) {
			throw new IllegalStateException("frames fixture wrote " + p + " of " + b.length);
		}
		return b;
	}

	/** Encodes a frame key the way {@code Animation.anIntArray353} does. */
	private static int frameKey(int file, int frame) {
		return (file << 16) | frame;
	}

	private static void animationFrameTranslateTransformIsExact() {
		Frames.load(FIXTURE_ANIM_FILE_TRANSLATE, buildFramesFixture(1, 7, 8, 9));
		Model m = parseSkinModel();
		m.method470(frameKey(FIXTURE_ANIM_FILE_TRANSLATE, 0));

		check("Animation transform: the label-5 vertices are UNTOUCHED by a translate frame"
				+ " (were 0,0,0 and 10,20,30)",
				m.vertexXs()[0] == 0 && m.vertexYs()[0] == 0 && m.vertexZs()[0] == 0
						&& m.vertexXs()[1] == 10 && m.vertexYs()[1] == 20 && m.vertexZs()[1] == 30);
		check("Animation transform: the label-6 vertex moves by EXACTLY the encoded delta"
				+ " (20,30,40 + 7,8,9 = 27,38,49) - got "
				+ m.vertexXs()[2] + "," + m.vertexYs()[2] + "," + m.vertexZs()[2],
				m.vertexXs()[2] == 27 && m.vertexYs()[2] == 38 && m.vertexZs()[2] == 49);
		check("Animation transform: opcode 0 set the origin to the MEAN of the label-5 vertices"
				+ " (5,10,15) PLUS the frame delta (1,2,3) = 6,12,18",
				((Integer) readStatic(Model.class, "anInt1681")) == FIXTURE_ORIGIN[0]
						&& ((Integer) readStatic(Model.class, "anInt1682")) == FIXTURE_ORIGIN[1]
						&& ((Integer) readStatic(Model.class, "anInt1683")) == FIXTURE_ORIGIN[2]);
	}

	/**
	 * Opcode 3 scales about the origin opcode 0 just set, which is the part a
	 * translate-only test would never reach.
	 */
	private static void animationFrameScaleTransformIsExact() {
		Frames.load(FIXTURE_ANIM_FILE_SCALE, buildFramesFixture(3, 64, 64, 64));
		Model m = parseSkinModel();
		m.method470(frameKey(FIXTURE_ANIM_FILE_SCALE, 0));

		// origin is (5,10,15) + the frame's delta (1,2,3) = (6,12,18)
		// (v - origin) * 64 / 128 + origin, integer division:
		// x (20-6)=14 -> 7 + 6  = 13 ; y (30-12)=18 -> 9 + 12 = 21 ; z (40-18)=22 -> 11 + 18 = 29
		check("Animation transform: a scale frame scales the label-6 vertex about the origin"
				+ " by 64/128 (expected 13,21,29) - got "
				+ m.vertexXs()[2] + "," + m.vertexYs()[2] + "," + m.vertexZs()[2],
				m.vertexXs()[2] == 13 && m.vertexYs()[2] == 21 && m.vertexZs()[2] == 29);
		check("Animation transform: scaling leaves the label-5 vertices alone",
				m.vertexXs()[1] == 10 && m.vertexYs()[1] == 20 && m.vertexZs()[1] == 30);
	}

	/**
	 * Transforms a COPY, exactly as the live client does, and requires the source to be
	 * untouched.
	 *
	 * <p>⚠️ <b>This is the invariant that keeps animation from drifting, and it is easy to
	 * break by accident.</b> {@code method470} mutates `anIntArray1627/1628/1629` IN PLACE,
	 * so a model re-transformed every frame would accumulate. What prevents that is
	 * {@code method464}: the client transforms a shared scratch model
	 * (`Model.aModel_1621`, Player.java:308) after {@code method464} has COPIED the base
	 * vertices into it. This test pins the copy.
	 */
	private static void animationFrameTransformLeavesTheSourceModelUntouched() {
		Frames.load(FIXTURE_ANIM_FILE_TRANSLATE, buildFramesFixture(1, 7, 8, 9));
		Model source = parseSkinModel();
		int sx = source.vertexXs()[2], sy = source.vertexYs()[2], sz = source.vertexZs()[2];

		Model scratch = parseSkinModel();
		scratch.method464(source, false);
		scratch.method470(frameKey(FIXTURE_ANIM_FILE_TRANSLATE, 0));

		check("Animation transform: the SOURCE model is untouched when the transform runs on a copy"
				+ " (this is what stops animations drifting frame over frame)",
				source.vertexXs()[2] == sx && source.vertexYs()[2] == sy
						&& source.vertexZs()[2] == sz);
		check("Animation transform: the COPY is the one that moved",
				scratch.vertexXs()[2] == 27 && scratch.vertexYs()[2] == 38
						&& scratch.vertexZs()[2] == 49);
	}

	/**
	 * The flip side, recorded so the reason for the copy above is not lost: applied twice
	 * to the SAME model, the transform compounds.
	 */
	private static void animationFrameTransformCompoundsOnTheSameModel() {
		Frames.load(FIXTURE_ANIM_FILE_TRANSLATE, buildFramesFixture(1, 7, 8, 9));
		Model m = parseSkinModel();
		int key = frameKey(FIXTURE_ANIM_FILE_TRANSLATE, 0);
		m.method470(key);
		m.method470(key);

		check("Animation transform: applying the SAME frame twice COMPOUNDS on one model"
				+ " (27,38,49 -> 34,46,58), which is exactly why the client transforms a copy - got "
				+ m.vertexXs()[2] + "," + m.vertexYs()[2] + "," + m.vertexZs()[2],
				m.vertexXs()[2] == 34 && m.vertexYs()[2] == 46 && m.vertexZs()[2] == 58);
	}

	private static void animationFrameTransformIsDeterministic() {
		Frames.load(FIXTURE_ANIM_FILE_TRANSLATE, buildFramesFixture(1, 7, 8, 9));
		int key = frameKey(FIXTURE_ANIM_FILE_TRANSLATE, 0);

		Model a = parseSkinModel();
		a.method470(key);
		Model b = parseSkinModel();
		b.method470(key);

		boolean same = true;
		for (int i = 0; i < 3; i++) {
			same = same && a.vertexXs()[i] == b.vertexXs()[i]
					&& a.vertexYs()[i] == b.vertexYs()[i]
					&& a.vertexZs()[i] == b.vertexZs()[i];
		}
		check("Animation transform: two independent models given the same frame agree exactly", same);
	}

	// ------------- Phase 6.3 the seam receives the CPU-transformed geometry

	/**
	 * Records what the Phase 4 scene seam is offered, and claims it so the software body
	 * is skipped. Everything else declines, so nothing else about the client changes.
	 */
	private static final class RecordingScene implements GpuRenderer.Implementation {
		Model lastModel;
		boolean sawModel;

		public boolean presentGameFrame(RSImageProducer producer, int destX, int destY) {
			return false;
		}

		public boolean drawModel(Model model, int orientation, int camA, int camB, int camC,
				int camD, int dx, int dy, int dz, int uid) {
			lastModel = model;
			sawModel = true;
			return true;
		}

		public boolean drawGroundTriangle(int x0, int y0, int x1, int y1, int x2, int y2,
				int colour0, int colour1, int colour2, int textureId, boolean flatMesh,
				int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7, int t8) {
			return false;
		}
	}

	/**
	 * 6.3: the rasteriser seam must be handed the CPU-TRANSFORMED vertices, so that GPU
	 * rendering cannot diverge from the software animation.
	 *
	 * <p>⚠️ <b>This is the check that lets skinning stay on the CPU forever, which is what
	 * the plan demands:</b> "Do not move skinning into a shader - that is where 'animations
	 * stay fluid' turns into 'animations drift'." A shader path would have to reproduce
	 * {@code method472}'s opcodes in GLSL and keep them bit-identical, including the
	 * integer division in opcode 3 and the fixed-point sin/cos tables in opcode 2.
	 *
	 * <p>The ordering that makes this true is not obvious from either method alone:
	 * {@code applyAnimationFrame} runs during the model's BUILD
	 * ({@code Player.getRotatedModel}, Player.java:308-320), and {@code method443} is the
	 * later DRAW step that dispatches {@code this} to the seam. So the model the rasteriser
	 * is offered already carries the posed vertices.
	 *
	 * <p>⚠️ The control matters: the rest-pose model is dispatched through the SAME seam
	 * first, so the difference between the two assertions is the animation and not some
	 * other property of the fixture.
	 */
	private static void sceneSeamIsOfferedTheTransformedVerticesNotTheRestPose() {
		Frames.load(FIXTURE_ANIM_FILE_TRANSLATE, buildFramesFixture(1, 7, 8, 9));

		Model rest = parseSkinModel();
		Model posed = parseSkinModel();
		posed.method470(frameKey(FIXTURE_ANIM_FILE_TRANSLATE, 0));

		RecordingScene rec = new RecordingScene();
		GpuRenderer.install(rec);
		try {
			// Control: an unanimated model.
			rest.method443(0, 0, 0, 0, 1, 1, 1, 1, 0);
			check("Animation upload: the scene seam is actually REACHED by method443"
					+ " (otherwise this whole test is vacuous)", rec.sawModel);
			int restX = rec.lastModel.vertexXs()[2];
			int restY = rec.lastModel.vertexYs()[2];
			int restZ = rec.lastModel.vertexZs()[2];
			check("Animation upload: control - the seam sees the REST pose (20,30,40) for an"
					+ " unanimated model - got " + restX + "," + restY + "," + restZ,
					restX == 20 && restY == 30 && restZ == 40);

			// The real assertion: the same seam, with the frame applied.
			rec.sawModel = false;
			rec.lastModel = null;
			posed.method443(0, 0, 0, 0, 1, 1, 1, 1, 0);

			check("Animation upload: the seam is handed the SAME model object that was posed -"
					+ " there is no intermediate copy to go stale", rec.lastModel == posed);
			check("Animation upload: the seam sees the ANIMATED vertex (27,38,49), NOT the rest"
					+ " pose - so the CPU transform is already folded in and needs no shader - got "
					+ rec.lastModel.vertexXs()[2] + "," + rec.lastModel.vertexYs()[2] + ","
					+ rec.lastModel.vertexZs()[2],
					rec.lastModel.vertexXs()[2] == 27 && rec.lastModel.vertexYs()[2] == 38
							&& rec.lastModel.vertexZs()[2] == 49);
			check("Animation upload: ... and that is genuinely DIFFERENT from the rest pose"
					+ " (a rest-pose-versus-rest-pose comparison would pass while being wrong)",
					posed.vertexXs()[2] != rest.vertexXs()[2]);
			check("Animation upload: a rasteriser reading the model needs no shader skinning,"
					+ " because vertexXs() IS the array method472 wrote",
					rec.lastModel.vertexXs() == posed.vertexXs());
		} finally {
			GpuRenderer.install(null);
		}
	}

	// --------------- Phase 6.5.2 the frame-slot budget has a single owner

	/**
	 * The measured shape of the packed 474 frame store, read from the real `Frames.dat`:
	 * 3230 compressed entries with ids 0..3229, dense. Recorded here as a literal because
	 * it is a property of the DATA, not of the code - which is exactly why the code cannot
	 * assert it and a test has to.
	 */
	private static final int MEASURED_PACKED_FRAME_MAX_ID = 3229;

	private static void frameSlotBudgetIsStatedInOnePlace() {
		check("Slot budget: the CursePack window is [28000, 28007) as one range",
				FrameSlots.CURSE_BASE == 28000 && FrameSlots.CURSE_END == 28007);
		check("Slot budget: the loose-numeric floor is 100",
				FrameSlots.LOOSE_MIN_ID == 100);
		check("Slot budget: MAX_FILE_ID is 32767, because the frame key packs the file into"
				+ " the HIGH 16 bits and (file << 16) must stay a positive int",
				FrameSlots.MAX_FILE_ID == 32767);

		check("Slot budget: every CursePack slot is key-addressable"
				+ " (28007-1 <= 32767)", FrameSlots.isKeyAddressable(FrameSlots.CURSE_END - 1));
		check("Slot budget: a file id past 32767 is NOT key-addressable, so the predicate"
				+ " would catch it", !FrameSlots.isKeyAddressable(FrameSlots.MAX_FILE_ID + 1));
		check("Slot budget: isCurseSlot accepts both ends and refuses just outside",
				FrameSlots.isCurseSlot(FrameSlots.CURSE_BASE)
						&& FrameSlots.isCurseSlot(FrameSlots.CURSE_END - 1)
						&& !FrameSlots.isCurseSlot(FrameSlots.CURSE_END)
						&& !FrameSlots.isCurseSlot(FrameSlots.CURSE_BASE - 1));
	}

	/**
	 * The invariant the offset exists for, checked against the MEASURED packed range rather
	 * than against an assumption: the CursePack's source ids sit inside it, so loading them
	 * under their own ids would overwrite 474 content.
	 */
	private static void curseSourceIdsWouldHaveCollidedWithoutTheOffset() {
		int[] files = (int[]) readStatic(CurseData667.class, "CURSE_ANIM_FILES");
		int maxSource = 0;
		int minSource = Integer.MAX_VALUE;
		for (int i = 0; i < files.length; i++) {
			if (files[i] > maxSource) maxSource = files[i];
			if (files[i] < minSource) minSource = files[i];
		}

		check("Slot budget: the CursePack's SOURCE ids (" + minSource + ".." + maxSource
				+ ") really are INSIDE the measured packed range 0.."
				+ MEASURED_PACKED_FRAME_MAX_ID + " - which is why the offset exists",
				minSource >= 0 && maxSource <= MEASURED_PACKED_FRAME_MAX_ID);
		check("Slot budget: the CursePack window starts ABOVE that measured range",
				FrameSlots.CURSE_BASE > MEASURED_PACKED_FRAME_MAX_ID);
		check("Slot budget: the loose loader WOULD also accept the CursePack's slot names,"
				+ " so the two writers are only separated by the offset, not by the loader",
				FrameSlots.isContestedByLooseLoading(FrameSlots.CURSE_BASE));
	}

	/**
	 * 6.5.2's stated minimum bar: "a test that asserts the 474 slots still decode identically
	 * after an injection".
	 *
	 * <p>Both slots are driven through the SAME transform path, so a difference in the
	 * output can only come from the frame data - not from the harness.
	 */
	private static void anInjectionDoesNotDisturbTheOriginalFrameSlots() {
		final int ORIGINAL_SLOT = 500;   // inside the packed 474 range

		Frames.load(ORIGINAL_SLOT, buildFramesFixture(1, 7, 8, 9));
		Model before = parseSkinModel();
		before.method470(frameKey(ORIGINAL_SLOT, 0));
		int bx = before.vertexXs()[2], by = before.vertexYs()[2], bz = before.vertexZs()[2];

		// The injection, into the CursePack's own window.
		Frames.load(FrameSlots.CURSE_BASE, buildFramesFixture(1, 100, 100, 100));
		Model injected = parseSkinModel();
		injected.method470(frameKey(FrameSlots.CURSE_BASE, 0));

		Model after = parseSkinModel();
		after.method470(frameKey(ORIGINAL_SLOT, 0));
		int ax = after.vertexXs()[2], ay = after.vertexYs()[2], az = after.vertexZs()[2];

		check("Slot budget: the injection really landed at " + FrameSlots.CURSE_BASE
				+ " carrying its OWN data (expected 120,130,140) - got "
				+ injected.vertexXs()[2] + "," + injected.vertexYs()[2] + ","
				+ injected.vertexZs()[2],
				injected.vertexXs()[2] == 120 && injected.vertexYs()[2] == 130
						&& injected.vertexZs()[2] == 140);
		check("Slot budget: the two slots genuinely hold DIFFERENT data, so the comparison"
				+ " below is not vacuous",
				injected.vertexXs()[2] != bx || injected.vertexYs()[2] != by
						|| injected.vertexZs()[2] != bz);
		check("Slot budget: the ORIGINAL 474-range slot (" + ORIGINAL_SLOT + ") still decodes"
				+ " IDENTICALLY after the injection (was " + bx + "," + by + "," + bz + ", now "
				+ ax + "," + ay + "," + az + ")",
				ax == bx && ay == by && az == bz);
	}

	// ------------------------- the standing raster gate, consolidated (Phase 4.3)

	/**
	 * Asserts the three raster pins are REAL pins, not sentinels.
	 *
	 * <p>Why this exists as its own assertion: Phase 4.3's job is to make the
	 * framebuffer hashes the standing objective oracle for the renderer seam. Three
	 * separate tests already compare a workload against a pinned hash and each one
	 * already fails if its constant is still the all-zeros sentinel - but nothing
	 * asserted, in one place, that the pins exist as a set and are the values the plan
	 * claims. A pin quietly reset to zeros would otherwise be visible only as three
	 * scattered "not pinned yet" notes.
	 *
	 * <p>The pins, and what each covers:
	 * <ul>
	 *   <li>{@link #RASTER_GOLDEN_HASH} - the 2D substrate ({@code DrawingArea} +
	 *       {@code Sprite}), from Phase 4.1b</li>
	 *   <li>{@link #TRIANGLE_GOLDEN_HASH} - the FLAT triangle rasteriser
	 *       ({@code Texture.method374}), from Phase 4.1c-2a</li>
	 *   <li>{@link #TEXTURED_GOLDEN_HASH} - the TEXTURED triangle rasteriser
	 *       ({@code Texture.method378}), the 4.1c-2c precondition</li>
	 * </ul>
	 *
	 * <p>{@code gradlew check} depends on the harness, so these run on every build and
	 * a moved hash fails it - that is what makes them a gate rather than a report.
	 */
	private static void rasterPinsAreStanding() {
		check("Raster gate: the substrate pin is a real value, not the sentinel",
				isRealHash(RASTER_GOLDEN_HASH));
		check("Raster gate: the flat-triangle pin is a real value, not the sentinel",
				isRealHash(TRIANGLE_GOLDEN_HASH));
		check("Raster gate: the textured-triangle pin is a real value, not the sentinel",
				isRealHash(TEXTURED_GOLDEN_HASH));
		check("Raster gate: the three pins are distinct (one per workload, not copy-pasted)",
				!RASTER_GOLDEN_HASH.equals(TRIANGLE_GOLDEN_HASH)
						&& !TRIANGLE_GOLDEN_HASH.equals(TEXTURED_GOLDEN_HASH)
						&& !RASTER_GOLDEN_HASH.equals(TEXTURED_GOLDEN_HASH));
	}

	private static boolean isRealHash(String h) {
		if (h == null || h.length() != 64) {
			return false;
		}
		for (int i = 0; i < h.length(); i++) {
			if (h.charAt(i) != '0') {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ plumbing

	private static void check(String what, boolean ok) {
		if (ok) {
			passed++;
			System.out.println("  PASS  " + what);
		} else {
			failed++;
			System.out.println("  FAIL  " + what);
		}
	}

	private ClientHarness() {
	}
}
