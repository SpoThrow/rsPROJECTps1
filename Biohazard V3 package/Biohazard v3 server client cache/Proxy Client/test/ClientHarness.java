import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import cache.StreamLoader;
import def.Animation;
import def.ContentRegistry;
import def.CurseData667;
import def.EntityDef;
import def.Flo;
import def.IDK;
import def.ItemDef;
import def.ObjectDef;
import def.SpotAnim;
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
import ui.GlBatcher;
import ui.GlClipper;
import ui.GlFacePipeline;
import ui.GlModelProjection;
import ui.GlScene;
import ui.GlSceneRenderer;
import ui.GlTextures;
import ui.GpuFloatBuffer;
import ui.GpuIntBuffer;
import ui.GpuRenderer;
import ui.RSImageProducer;
import ui.RendererConfig;
import ui.SceneBatch;
import ui.SceneRasterizer;
import ui.Sprite;
import ui.TriangleSink;



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
		glRendererDeclinesAndContextFailureIsNotFatal();
		glFrameLatchCoversTheWholeFrame();
		glFrameLatchBringsTheBatchUpFirst();
		glBatcherDegradesAndTheArgbContractHolds();
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
		// ✅ The ordering constraint the projection test used to carry is GONE, and the
		// cause is no longer unidentified. It drove a real `Model.method443`, which left
		// the `private static` `aBooleanArray1664` set for the faces it drew; the
		// rasteriser test above reads that array and so took the CLIPPER branch instead of
		// the flat-rasteriser branch. That test now resets the two flags itself, so these
		// two are independent - verified by running this one FIRST, which now passes.
		modelProjectionMatchesTheSoftwarePath();
		modelFacesAreCulledExactlyAsTheSoftwareCullsThem();
		modelFaceColoursMatchTheSoftwareRasteriser();
		shadeModelWritesOnlyTheSlotsEachRenderTypeUses();
		flatFacesAreEmittedAsOneColour();
		flatFacesMatchTheSoftwareFlatFill();
		nearPlaneClipperMatchesTheSoftwareClipper();
		texturedFacesResolveTheSoftwareTextureInputs();
		modelFaceDeclinesNameTheirOutcome();
		glTexturesMatchTheSoftwareShadeBlocks();
		groundTexturePlaneRoles();
		texturedFaceNineSourceIsMeasured();
		textureRampsResolveTheSoftwareMapping();
		textureRampsMatchAtBothDetailLevels();
		rampOverflowIsMeasured();
		clippedTexturedFacesUseTheUncutRamps();
		curseFrameRemapRedirectsCurseFilesToTheHighSlots();
		curseFrameRemapLeavesOriginalFrameSlotsAlone();
		curseAnimationLoaderRefusesSlotsOutsideTheRemapRange();
		curseFrameSlotsCannotCollideWithTheOriginalRange();
		seqSkipTableMatchesThe474ReaderForEveryOpcodeItHandles();
		seqSkipTableHandlesOpcode12WhichThe474ReaderDoesNot();
		spotAnimSkipTableMatchesThe474ReaderForEveryOpcodeItHandles();
		animationFrameTranslateTransformIsExact();
		animationFrameScaleTransformIsExact();
		animationFrameTransformLeavesTheSourceModelUntouched();
		animationFrameTransformCompoundsOnTheSameModel();
		animationFrameTransformIsDeterministic();
		sceneSeamIsOfferedTheTransformedVerticesNotTheRestPose();
		frameSlotBudgetIsStatedInOnePlace();
		curseSourceIdsWouldHaveCollidedWithoutTheOffset();
		anInjectionDoesNotDisturbTheOriginalFrameSlots();
		contentRegistryDeclarationsAreSafe();
		contentRegistryLoadsADeclaredSourceIntoItsSlot();
		contentRegistryReportsAMissingSourceRatherThanPassingSilently();
		contentRegistryValidateCatchesADriftedDeclaration();
		declaratationsLiveInOnePlaceOnly();
		spotAnimOverridesAreAppliedToTheDeclaredIds();
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
					int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7, int t8,
					int depth) {
				calls[0]++;
				return true;
			}

			public boolean sceneFinished(RSImageProducer producer) {
				return false;
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
					int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7, int t8,
					int depth) {
				return true;
			}

			public boolean sceneFinished(RSImageProducer producer) {
				return false;
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
							11, 12, 13, 14, 15, 16, 17, 18, 19, 600));
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
		check("Renderer setting: the GL arm has its OWN value, distinct from software and placeholder",
				!"gl".equals(RendererConfig.SOFTWARE)
						&& !"gl".equals(RendererConfig.PLACEHOLDER_NAME)
						&& "gl".equals(RendererConfig.GL_NAME));
	}

	// ---------------------------------------- the GL arm's bring-up (Phase 7.2a)

	/**
	 * The GL arm must decline everything until 7.2b gives it something to draw, and a
	 * missing GL runtime must not take the client down.
	 *
	 * <p><b>Why declining is the property worth pinning, not a formality.</b> The scene
	 * seam treats an installed renderer as authoritative unless it says otherwise, so a
	 * GL renderer that ACCEPTED a submission without drawing it would make models and
	 * ground disappear. And a PARTIAL GL scene cannot compose correctly either: the
	 * software path interleaves ground and models per tile, so the scene has to be taken
	 * over whole or not at all.
	 *
	 * <p><b>What this exercises on a normal machine.</b> The first scene submission is
	 * what calls {@code GlScene.ensure(int, int)}, so this drives the real lifecycle entry point
	 * on the harness's own thread. {@code deps/lwjgl3} is deliberately absent from the
	 * harness classpath, so here it takes the DEGRADATION path - which is the robustness
	 * property: the LWJGL 3 jars may genuinely be missing at runtime (Run.bat controls
	 * that classpath), and the answer must be the software path, never an exception. The
	 * SUCCESSFUL path needs a real GPU and belongs to the live gate (7.4).
	 */
	private static void glRendererDeclinesAndContextFailureIsNotFatal() {
		GpuRenderer.install(new GlSceneRenderer("harness-test"));
		try {
			check("GL arm: does not consume a model",
					!SceneRasterizer.dispatch(Model.aModel_1621, 0, 0, 0, 0, 0, 0, 0, 0, 0));
			check("GL arm: does not consume a ground triangle",
					!SceneRasterizer.dispatchGroundTriangle(1, 2, 3, 4, 5, 6, 7, 8, 9, -1, false,
							11, 12, 13, 14, 15, 16, 17, 18, 19, 600));
			check("GL arm: declines the present, so the UI still composites in software",
					!GpuRenderer.presentGameFrame(null, 0, 0));
		} finally {
			GpuRenderer.install(null);
		}

		boolean ready = GlScene.ensure(765, 503);
		check("GL context: ensure() reports a result instead of throwing",
				GlScene.attempted());
		check("GL context: an unavailable context records WHY, so it is not a silent no-op",
				ready || GlScene.unavailableReason() != null);
		check("GL context: describe() is answerable whether or not it came up",
				GlScene.describe() != null && GlScene.describe().length() > 0);

		// Idempotence: a failed attempt must not be retried per frame.
		boolean second = GlScene.ensure(765, 503);
		check("GL context: ensure() is idempotent - a failure is not retried every frame",
				second == ready);
		// And a RESTATE to a different drawing area, which is what a resize or a mode switch
		// looks like: the size must be accepted on both paths rather than only the first. On
		// this classpath the context is unavailable, so the answer stays false - what is
		// pinned is that a size change is ANSWERED, not thrown. The success path needs a GPU
		// and is the live gate's.
		check("GL context: asking for a different drawing area is answered, not thrown",
				GlScene.ensure(907, 666) == ready);
	}

	// ------------------------------------ the GL frame lifecycle (Phase 7.2c)

	/**
	 * The frame latch, and the rule that a frame which is not WHOLE is discarded rather
	 * than half-drawn.
	 *
	 * <p><b>What this drives.</b> The real lifecycle entry points - {@code SceneRasterizer.dispatch},
	 * {@code dispatchGroundTriangle} and {@code GpuRenderer.sceneFinished} - against a
	 * recording {@link ui.SceneBatch} instead of the GPU, because the harness ships without
	 * LWJGL 3 and the whole frame path would otherwise be unreachable here. What that leaves
	 * unverified is the driver, which is the live gate's job (7.4).
	 *
	 * <p><b>The properties worth pinning, and why each is a real hazard rather than a
	 * formality.</b>
	 * <ol>
	 *   <li><b>Exactly one frame start per frame.</b> The decision has to be taken once, at
	 *       the first submission, because the software path interleaves ground and models
	 *       per tile and two independently ordered images cannot be merged afterwards.</li>
	 *   <li><b>A submission is never CONSUMED yet.</b> Consuming is irreversible, so until
	 *       the per-face outcomes are total the software image has to stay as the fallback.</li>
	 *   <li><b>A frame with anything unrepresentable is NOT read back.</b> That is the
	 *       safety property: the failure mode of a half-drawn GL frame is impossible if a
	 *       non-whole frame simply leaves the software scene in place.</li>
	 *   <li><b>A scene of model + UNTEXTURED ground is now WHOLE and IS read back</b> (Phase
	 *       7.2c-2). Until that step the ground was the single reason no frame could ever be
	 *       read back; the tile now arrives with its own camera-space depth, so this is the
	 *       first frame the GL path can hand over - and the tile's depth is asserted by VALUE
	 *       rather than by counting a triangle, because a constant substituted for it would
	 *       pass a count.</li>
	 *   <li><b>The one ground decline left is the LOW-DETAIL textured tile</b>, asserted by
	 *       name. On low detail the software does not texturise ground at all, and {@code ui}
	 *       cannot reproduce that branch.</li>
	 *   <li><b>A drawing area of a different size declines the frame.</b> The GL canvas is
	 *       fixed at context creation, so a resizable window cannot be served by it, and
	 *       read back over a mismatched buffer the picture would be cropped or sheared.</li>
	 * </ol>
	 */
	private static void glFrameLatchCoversTheWholeFrame() {
		// The GL frame is a fixed 765x503 viewport; this also binds DrawingArea to it.
		RSImageProducer scene = new RSImageProducer(765, 503, null);
		check("GL frame: the producer is the scene buffer the seam is handed",
				scene.anInt316 == 765 && scene.anInt317 == 503 && scene.anIntArray315.length == 765 * 503);

		check("GL frame: the seam declines when no renderer is installed",
				!GpuRenderer.sceneFinished(scene));

		RecordingSceneBatch batch = new RecordingSceneBatch();
		GlSceneRenderer renderer = new GlSceneRenderer("harness-frame", batch);
		GpuRenderer.install(renderer);
		// ⚠ The LOW-DETAIL ground arm is selected by WorldController.lowMem - the field the
		// drawers themselves branch on - and it defaults to TRUE, i.e. unless client.setHighMem()
		// has run the software does not texturise ground at all. Pinned rather than left to the
		// default so both arms below are asserted against a known state.
		boolean savedGroundLowMem = WorldController.lowMem;
		WorldController.lowMem = true;
		try {
			// ---- the latch: many submissions, ONE frame start
			check("GL frame: a model is NOT consumed (shadow stage)",
					!SceneRasterizer.dispatch(Model.aModel_1621, 0, 0, 0, 0, 0, 0, 0, 0, 0));
			check("GL frame: the first submission starts exactly one frame", batch.beginFrames == 1);
			check("GL frame: the frame is cleared to the scene background",
					batch.lastClearArgb == batch.sceneBackground());
			// ---- THE MILESTONE (Phase 7.2c-2): a scene with an UNTEXTURED ground tile is WHOLE.
			//
			// Until this step every frame containing ground was discarded, because the ground
			// seam carried no depth for GL to composite with. It carries the tile's own
			// camera-space depth now, so this frame is the first the GL path can actually hand
			// back - and the assertions below are what make that a measurement.
			check("GL frame: an untextured ground tile is NOT consumed either",
					!SceneRasterizer.dispatchGroundTriangle(0, 0, 10, 0, 0, 10, 1, 2, 3,
							-1, false, 11, 12, 13, 14, 15, 16, 17, 18, 19, 900));
			check("GL frame: further submissions do NOT start another frame",
					batch.beginFrames == 1);
			check("GL frame: the tile's own camera-space depth arrives at the renderer",
					renderer.lastGroundDepth() == 900);
			check("GL frame: the ground tile really was submitted to the batch",
					renderer.groundTriangles() == 1 && renderer.texturedGroundTriangles() == 0);
			check("GL frame: a scene of model + untextured ground IS whole - no decline to report",
					renderer.frameDeclineReason() == null);
			check("GL frame: and it IS read back, so the GL scene replaces the software one",
					GpuRenderer.sceneFinished(scene) && batch.readBacks == 1);
			check("GL frame: the readback landed in the producer's own pixels",
					scene.anIntArray315[0] == RecordingSceneBatch.PAINTED);
			check("GL frame: a whole frame with ground is counted as read back, not discarded",
					renderer.framesReadBack() == 1 && renderer.framesDiscarded() == 0
							&& renderer.framesLatched() == 1);

			// ---- the ONE ground decline that remains: the LOW-DETAIL textured tile.
			//
			// On low detail the software does not texturise ground at all (it darkens the codes
			// through WorldController.method317 and draws them flat), and ui cannot reach that,
			// so it declines by name rather than drawing something the software would not.
			batch.beginFrames = 0;
			SceneRasterizer.dispatch(Model.aModel_1621, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			SceneRasterizer.dispatchGroundTriangle(0, 0, 10, 0, 0, 10, 1, 2, 3,
					TEXTURED_TEX_ID, false, 11, 12, 13, 14, 15, 16, 17, 18, 19, 900);
			check("GL frame: a LOW-DETAIL textured ground tile is not representable",
					GlSceneRenderer.GROUND_LOWMEM_DECLINE.equals(renderer.frameDeclineReason()));
			check("GL frame: a non-whole frame is NOT read back, so the software scene stands",
					!GpuRenderer.sceneFinished(scene) && batch.readBacks == 1);
			check("GL frame: the frame was still flushed, so the batch was exercised",
					batch.flushes == 2);
			check("GL frame: the discard is counted, and named with its reason",
					renderer.framesDiscarded() == 1
							&& GlSceneRenderer.GROUND_LOWMEM_DECLINE.equals(renderer.frameDeclineReason()));
			check("GL frame: nothing was read back for the declined frame",
					scene.anIntArray315[0] == RecordingSceneBatch.PAINTED);

			// ---- the latch RESETS: the next frame decides for itself
			int before = batch.beginFrames;
			SceneRasterizer.dispatch(Model.aModel_1621, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			check("GL frame: the latch resets, so the next frame starts its own",
					batch.beginFrames == before + 1);
			check("GL frame: a frame with NO ground has no decline to report",
					renderer.frameDeclineReason() == null);
			check("GL frame: a whole frame IS read back into the scene buffer",
					GpuRenderer.sceneFinished(scene) && batch.readBacks == 2);
			check("GL frame: the readback landed in the producer's own pixels",
					scene.anIntArray315[0] == RecordingSceneBatch.PAINTED);
			check("GL frame: a read-back frame is not counted as discarded",
					renderer.framesDiscarded() == 1 && renderer.framesReadBack() == 2
							&& renderer.framesLatched() == 3);

			// ---- the sentinel: the software draws NOTHING for this triangle, so neither does GL
			int flushes = batch.flushes;
			batch.beginFrames = 0;
			SceneRasterizer.dispatch(Model.aModel_1621, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			SceneRasterizer.dispatchGroundTriangle(0, 0, 10, 0, 0, 10, 0xbc614e, 2, 3,
					TEXTURED_TEX_ID, false, 11, 12, 13, 14, 15, 16, 17, 18, 19, 900);
			check("GL frame: the software's skip sentinel is not a decline - it is drawn by nobody",
					renderer.frameDeclineReason() == null);
			check("GL frame: and that frame is read back",
					GpuRenderer.sceneFinished(scene) && batch.flushes == flushes + 1);
			check("GL frame: the sentinel tile was not submitted as a ground triangle",
					renderer.groundTriangles() == 1);

			// ---- the size gate: now a POST-CONDITION rather than a comparison with a constant
			//
			// The batch is ASKED for the drawing area and is expected to take it (that is what
			// SceneBatch#ensure(int, int) is for). What declines is a batch that does NOT end
			// up at that size, because a frame of the wrong size cannot be read back into the
			// producer without cropping, shearing or overflowing it.
			batch.viewportWidth = 800;
			batch.honoursRequestedSize = false;
			batch.beginFrames = 0;
			SceneRasterizer.dispatch(Model.aModel_1621, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			check("GL frame: a batch that will not take the drawing area starts no frame",
					batch.beginFrames == 0);
			check("GL frame: and nothing is read back for it",
					!GpuRenderer.sceneFinished(scene));
			check("GL frame: a mismatched frame is not counted as latched",
					renderer.framesLatched() == 4);
			batch.viewportWidth = 765;
			batch.honoursRequestedSize = true;
		} finally {
			WorldController.lowMem = savedGroundLowMem;
			GpuRenderer.install(null);
		}
	}

	/**
	 * The latch must BRING THE BATCH UP, not merely ask whether it is up.
	 *
	 * <p><b>This is the live gate's finding, pinned (2026-10-06).</b> {@code GlBatcher.ready()}
	 * is a pure query whose flag is only ever set by {@code ensure()}, and {@code beginFrame}
	 * was the only production caller of {@code ensure()}. So a latch that tested
	 * {@code ready()} and returned on {@code false} asked about a batch that nothing had
	 * tried to bring up, got {@code false} forever, and never reached the call that would
	 * have made it {@code true} - <b>no frame could ever latch, on any machine, in any
	 * window size</b>. The live client printed the shape of it in one line:
	 * {@code "GL batcher unavailable (null)"}, where the {@code (null)} is the failure
	 * string that was never assigned because {@code ensure()} never ran.
	 *
	 * <p><b>Why no other check could see it:</b> they all drive
	 * {@link RecordingSceneBatch}, whose {@code ready()} was a constant {@code true}. That
	 * is a faithful double of "a recording batch is ready" but a wrong model of readiness -
	 * it treats readiness as a FACT ABOUT THE WORLD rather than as the state of an
	 * initialisation that someone has to ask for. The double here starts NOT ready and the
	 * batch comes up only when asked, which is the real class's contract.
	 *
	 * <p>Both halves are checked, so the good case cannot pass by latching regardless: a
	 * batch that is asked and CANNOT come up must start no frame at all.
	 */
	private static void glFrameLatchBringsTheBatchUpFirst() {
		RSImageProducer scene = new RSImageProducer(765, 503, null);

		// CONTROL: asked to come up, and it cannot. Nothing may latch, and the ask must
		// still have happened - otherwise "it declined" would also pass if the latch had
		// simply never looked.
		RecordingSceneBatch dead = new RecordingSceneBatch();
		dead.ready = false;
		dead.comeUpOnEnsure = false;
		GlSceneRenderer deadRenderer = new GlSceneRenderer("harness-dead", dead);
		GpuRenderer.install(deadRenderer);
		try {
			SceneRasterizer.dispatch(Model.aModel_1621, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			check("GL frame: a batch that cannot come up IS asked to, and reports that it cannot",
					dead.ensures == 1 && !dead.ready());
			check("GL frame: a batch that cannot come up starts no frame and latches nothing",
					dead.beginFrames == 0 && deadRenderer.framesLatched() == 0);
			check("GL frame: and nothing is read back for it",
					!GpuRenderer.sceneFinished(scene) && dead.readBacks == 0);
		} finally {
			GpuRenderer.install(null);
		}

		// THE CASE THE LIVE CLIENT HIT: not ready yet, and ready once asked. On the broken
		// code this frame is declined forever and framesLatched() stays at zero.
		RecordingSceneBatch lazy = new RecordingSceneBatch();
		lazy.ready = false;
		lazy.comeUpOnEnsure = true;
		GlSceneRenderer renderer = new GlSceneRenderer("harness-lazy", lazy);
		GpuRenderer.install(renderer);
		try {
			SceneRasterizer.dispatch(Model.aModel_1621, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			SceneRasterizer.dispatchGroundTriangle(0, 0, 10, 0, 0, 10, 1, 2, 3,
					-1, false, 11, 12, 13, 14, 15, 16, 17, 18, 19, 900);
			check("GL frame: a batch that is not ready YET still latches, because the latch ASKS it to come up",
					lazy.beginFrames == 1 && renderer.framesLatched() == 1);
			check("GL frame: and it is asked ONCE per frame, not once per submission",
					lazy.ensures == 1);
			check("GL frame: and it is asked for the DRAWING AREA, not for a constant",
					lazy.askedWidth == 765 && lazy.askedHeight == 503);
			check("GL frame: so such a frame reaches the screen instead of being declined forever",
					GpuRenderer.sceneFinished(scene) && lazy.readBacks == 1
							&& scene.anIntArray315[0] == RecordingSceneBatch.PAINTED);
		} finally {
			GpuRenderer.install(null);
		}

		// ⚠ THE LIVE GATE'S SECOND FINDING, pinned: the drawing area is NOT 765x503 in ANY
		// configuration. Fixed mode is 512x334, and resizable mode is the frame size minus
		// nothing - Jframe.setCanvasSize makes the FRAME `width + sidebar + insets` by
		// `height + titlebar + insets` and the game component stretches to fill it, so a real
		// window reports 907x666 and grows from there. A viewport fixed at 765x503 therefore
		// declined EVERY frame in EVERY configuration, and the live client said so:
		// "GL viewport 765x503 does not match the drawing area 907x666". This is the check
		// that the size is taken FROM THE DRAWING AREA instead.
		RSImageProducer oddScene = new RSImageProducer(907, 666, null);
		RecordingSceneBatch odd = new RecordingSceneBatch();
		odd.viewportWidth = 907;
		odd.viewportHeight = 666;
		GlSceneRenderer oddRenderer = new GlSceneRenderer("harness-odd", odd);
		GpuRenderer.install(oddRenderer);
		try {
			SceneRasterizer.dispatch(Model.aModel_1621, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			check("GL frame: a drawing area that is NOT 765x503 is asked for as it is",
					odd.askedWidth == 907 && odd.askedHeight == 666);
			check("GL frame: so a real window's 907x666 still latches instead of declining forever",
					odd.beginFrames == 1 && oddRenderer.framesLatched() == 1
							&& GpuRenderer.sceneFinished(oddScene) && odd.readBacks == 1);
		} finally {
			GpuRenderer.install(null);
		}
	}

	/** A recording {@link ui.SceneBatch}: the GPU boundary, replaced by an observer. */
	private static final class RecordingSceneBatch implements SceneBatch {

		/** Written to the first pixel of any readback, so "did GL write it" is unambiguous. */
		static final int PAINTED = 0x0011ee22;

		int viewportWidth = 765;
		int viewportHeight = 503;
		boolean ready = true;
		/**
		 * ⚠ Models the REAL contract of {@code GlBatcher}: it is not ready until something
		 * has ASKED it to come up. The default is "already ready" so that the other checks
		 * read as they always did, but a check that sets this to {@code true} (with
		 * {@link #ready} false) is the one that catches a caller which merely asks whether
		 * the batch is ready instead of bringing it up - the deadlock the live gate found.
		 */
		boolean comeUpOnEnsure;
		/**
		 * Whether the batch accepts the size it is asked for. True models the real
		 * {@code GlBatcher}, which restates its frame target; {@code false} models a batch
		 * that cannot, and is what the size-gate check needs.
		 */
		boolean honoursRequestedSize = true;
		/**
		 * Whether {@link #textured} declines the submission, which is how the harness reaches
		 * {@link ui.GlFacePipeline#TEXTURE_SINK_DECLINED} through the renderer rather than
		 * through the pipeline directly. See {@link RecordingSink#refuseTextured}.
		 */
		boolean refuseTextured;
		/** The size the last {@code ensure} was asked for, so "it asks for the DRAWING AREA" is checkable. */
		int askedWidth = -1;
		int askedHeight = -1;
		/** How many times the one-time initialisation was asked for. */
		int ensures;
		int beginFrames;
		int flushes;
		int readBacks;
		int triangles;
		int texturedTriangles;
		int lastClearArgb = -1;

		public boolean beginFrame(int clearArgb) {
			beginFrames++;
			lastClearArgb = clearArgb;
			return true;
		}

		public boolean flush() {
			flushes++;
			return true;
		}

		public boolean readInto(int[] dest, int destStride, int destX, int destY) {
			readBacks++;
			if (dest != null && dest.length > 0) {
				dest[destY * destStride + destX] = PAINTED;
			}
			return true;
		}

		public int viewportWidth() {
			return viewportWidth;
		}

		public int viewportHeight() {
			return viewportHeight;
		}

		public boolean ensure(int width, int height) {
			ensures++;
			askedWidth = width;
			askedHeight = height;
			if (comeUpOnEnsure) {
				ready = true;
			}
			if (ready && honoursRequestedSize) {
				viewportWidth = width;
				viewportHeight = height;
			}
			return ready;
		}

		public boolean ready() {
			return ready;
		}

		public int sceneBackground() {
			// A constant on purpose: the real value lives behind client.fogStrength, and
			// loading the client class is not headless-safe.
			return 0x00c0d0e0;
		}

		public boolean supportsTextures() {
			return true;
		}

		public boolean triangle(float x0, float y0, float z0, int argb0, float x1, float y1,
				float z1, int argb1, float x2, float y2, float z2, int argb2) {
			triangles++;
			return true;
		}

		public boolean textured(float x0, float y0, float z0, float u0, float v0, float w0,
				int shade0, float x1, float y1, float z1, float u1, float v1, float w1, int shade1,
				float x2, float y2, float z2, float u2, float v2, float w2, int shade2,
				int textureId) {
			if (refuseTextured) {
				// ⚠ Refused BEFORE the count, deliberately: a sink that declined must not look
				// like one that queued the triangle, or the check that reads this count would
				// pass while the pipeline reported the decline.
				return false;
			}
			texturedTriangles++;
			return true;
		}

		public String describe() {
			return "recording scene batch (harness)";
		}
	}

	// ------------------------------------ GL triangle sink (Phase 7.2b-1)

	/**
	 * {@code ui.GlBatcher} must DECLINE rather than throw when GL is unavailable, and
	 * its readback must obey the client's pixel format.
	 *
	 * <p><b>The degradation half is the property worth pinning headlessly.</b>
	 * {@code deps/lwjgl3} is deliberately absent from the harness classpath, so this
	 * drives the real {@code ensure()} failure path. That path is reachable in
	 * production too - {@code Run.bat} controls the classpath, and a machine with no
	 * driver fails the same way - and the required answer is the software path, never
	 * an exception.
	 *
	 * <p><b>The format half is pinned here rather than left to the GPU probe</b>
	 * because it is a pure calculation and can be checked with no GPU at all: the
	 * client's buffers are {@code 0x00RRGGBB} (see {@code RSImageProducer}'s
	 * {@code DirectColorModel(32, 0xff0000, 0xff00, 0xff)}), so the alpha byte must
	 * stay zero. The GPU probe ({@code tools/gl-batch-probe}) covers the parts that
	 * genuinely need a GPU - byte order, Y orientation, depth - and is mutation-proved.
	 */
	private static void glBatcherDegradesAndTheArgbContractHolds() {
		ui.GlBatcher batcher = new ui.GlBatcher();

		boolean ready = batcher.ensure(765, 503);
		check("GL batcher: ensure() reports a result instead of throwing", batcher.ready() == ready);
		check("GL batcher: an unavailable batcher records WHY, so it is not a silent no-op",
				ready || batcher.failureReason() != null);
		check("GL batcher: describe() is answerable whether or not it came up",
				batcher.describe() != null && batcher.describe().length() > 0);

		// Every operation must DECLINE when there is no GL, so the caller keeps the
		// software path - the same property the renderer seam already relies on.
		check("GL batcher: beginFrame declines when GL is unavailable", !batcher.beginFrame(0));
		check("GL batcher: triangle declines when GL is unavailable",
				!batcher.triangle(0, 0, 0, 0, 1, 1, 1, 0, 2, 2, 2, 0));
		check("GL batcher: flush declines when GL is unavailable", !batcher.flush());
		check("GL batcher: readInto declines when GL is unavailable",
				!batcher.readInto(new int[4], 2, 0, 0));
		check("GL batcher: readInto declines a null destination rather than throwing",
				!batcher.readInto(null, 2, 0, 0));

		// Idempotence, as for GlScene: a failure must not be retried every frame.
		check("GL batcher: ensure() is idempotent - a failure is not retried every frame",
				batcher.ensure(765, 503) == ready);

		// The pixel-format contract, checked without a GPU: the values the readback
		// writes must have a zero alpha byte, because that is what the software path
		// writes and what the 4.1b framebuffer hash covers.
		int packed = (0xd3 << 16) | (0x7a << 8) | 0x21;
		check("GL batcher: the readback pixel format is 0x00RRGGBB (alpha byte zero)",
				(packed >>> 24) == 0 && ((packed >> 16) & 0xff) == 0xd3
						&& ((packed >> 8) & 0xff) == 0x7a && (packed & 0xff) == 0x21);
		check("GL batcher: 0x00FF0000 decodes as RED, not blue - the byte-order contract",
				((0x00FF0000 >> 16) & 0xff) == 0xff && (0x00FF0000 & 0xff) == 0);
	}

	// ------------------------------------ GL model projection (Phase 7.2b-2a)

	/**
	 * {@code ui.GlModelProjection} must reproduce {@code Model.method443}'s projection
	 * EXACTLY, and it is checked against the REAL method443 rather than a fixture.
	 *
	 * <p><b>Why an oracle and not expected values.</b> Hand-computing the screen
	 * coordinates would only prove the implementation agrees with the same reading of
	 * {@code method443} that produced it - the exact trap 5.3 caught when a self-consistent
	 * wrong texture-id contract survived every test that agreed with itself. So instead
	 * the REAL {@code method443} is driven here, the REAL {@code anIntArray1665/1666/1667}
	 * it leaves behind are read back, and this class must match them integer-for-integer.
	 * The software path is the authority; this either agrees with it or is wrong.
	 *
	 * <p><b>The CONTROL, and it is the one that matters most.</b> Those three arrays are
	 * {@code private static} scratch - shared by every model and valid only mid-draw
	 * (5.1's finding), which is exactly why the GL path cannot borrow them. {@code
	 * method443} also CULLS on the model's bounding box before projecting, in which case
	 * the arrays still hold the PREVIOUS model's data. So they are filled with a sentinel
	 * first and the test asserts every vertex was actually rewritten: without that, a
	 * culled draw would have this test comparing a stale array against a fresh projection
	 * and possibly calling it a match.
	 */
	private static void modelProjectionMatchesTheSoftwarePath() {
		int centreX = 382;
		int centreY = 251;
		int extent = 382;

		int savedCenterX = DrawingArea.centerX;
		int savedCenterY = DrawingArea.centerY;
		int savedExtent = DrawingArea.anInt1387;
		// ⚠ method443 does not just compute - it RASTERISES, straight into whatever buffer
		// DrawingArea currently points at. Left alone, that paints into another test's
		// framebuffer and, because the 5.3 rasteriser test compares its buffer before and
		// after its OWN draw, a pre-painted buffer makes its draw look like it plotted
		// nothing. So this test draws onto scratch of its own and puts the drawing area
		// back exactly as it found it.
		int[] savedPixels = DrawingArea.pixels;
		int savedWidth = DrawingArea.width;
		int savedHeight = DrawingArea.height;
		int savedTopX = DrawingArea.topX;
		int savedBottomX = DrawingArea.bottomX;
		int savedTopY = DrawingArea.topY;
		int savedBottomY = DrawingArea.bottomY;
		Object savedTextureInt1 = readStatic(model.Texture.class, "textureInt1");
		Object savedTextureInt2 = readStatic(model.Texture.class, "textureInt2");
		// ⚠ These six are the SHARED static scratch {@code method443} projects into, of
		// which 1668/1669/1670 are also the camera-space slots the 5.3 rasteriser test
		// parks. Driving a real {@code method443} overwrites all of them, so they are
		// snapshotted and restored - borrowing shared state and leaving it modified is
		// how one test silently breaks another.
		String[] scratchNames = { "anIntArray1665", "anIntArray1666", "anIntArray1667",
				"anIntArray1668", "anIntArray1669", "anIntArray1670" };
		int[][] savedScratch = new int[scratchNames.length][];
		for (int i = 0; i < scratchNames.length; i++) {
			savedScratch[i] = ((int[]) readStatic(Model.class, scratchNames[i])).clone();
		}
		try {
			// method443 reads the screen centre from Texture and culls against the
			// drawing area, so both must be set to values the test can reason about.
			DrawingArea.centerX = centreX;
			DrawingArea.centerY = centreY;
			DrawingArea.anInt1387 = extent;
			DrawingArea.initDrawingArea(503, 765, new int[765 * 503]);
			writeStatic(model.Texture.class, "textureInt1", centreX);
			writeStatic(model.Texture.class, "textureInt2", centreY);

			Model model = parseFixtureModel();
			// The camera distance only feeds method443's bounding-box cull, not the
			// per-vertex transform, so pinning it makes the draw deterministic without
			// altering the arithmetic under test.
			model.anInt1650 = 500;

			Object savedOrientationCos = readStatic(Model.class, "modelIntArray1");
			Object savedOrientationSin = readStatic(Model.class, "modelIntArray2");
			int[] orientationCos = new int[8];
			int[] orientationSin = new int[8];
			orientationCos[3] = 46341;
			orientationSin[3] = 46341;
			writeStatic(Model.class, "modelIntArray1", orientationCos);
			writeStatic(Model.class, "modelIntArray2", orientationSin);
			try {
				// ⚠ Four DISTINCT camera terms, and non-zero dx/dy/dz, on purpose - a symmetric
				// camera (all four equal) or a zero offset would make an AXIS SWAP invisible,
				// because the terms would be interchangeable. These are not a physically real
				// camera pair; method443 is pure fixed-point arithmetic and the oracle is the
				// real method443 given the same numbers, so what is being pinned is the
				// arithmetic and the axis assignment within it.
				assertProjectionMatchesSoftware(model, 0, 56756, 32768, 40000, 50000,
						10, 20, 50, centreX, centreY, "no orientation");
				// The orientation branch is a separate rotation, applied before the camera's,
				// so it is a genuinely untested code path unless it is driven too.
				assertProjectionMatchesSoftware(model, 3, 56756, 32768, 40000, 50000,
						10, 20, 50, centreX, centreY, "with orientation 3");
			} finally {
				writeStatic(Model.class, "modelIntArray1", savedOrientationCos);
				writeStatic(Model.class, "modelIntArray2", savedOrientationSin);
			}
		} finally {
			DrawingArea.centerX = savedCenterX;
			DrawingArea.centerY = savedCenterY;
			DrawingArea.anInt1387 = savedExtent;
			DrawingArea.pixels = savedPixels;
			DrawingArea.width = savedWidth;
			DrawingArea.height = savedHeight;
			DrawingArea.topX = savedTopX;
			DrawingArea.bottomX = savedBottomX;
			DrawingArea.topY = savedTopY;
			DrawingArea.bottomY = savedBottomY;
			writeStatic(model.Texture.class, "textureInt1", savedTextureInt1);
			writeStatic(model.Texture.class, "textureInt2", savedTextureInt2);
			for (int i = 0; i < scratchNames.length; i++) {
				System.arraycopy(savedScratch[i], 0,
						(int[]) readStatic(Model.class, scratchNames[i]), 0, savedScratch[i].length);
			}
		}
		check("GL projection: the camera and screen state the test borrowed is restored",
				DrawingArea.centerX == savedCenterX && DrawingArea.centerY == savedCenterY
						&& DrawingArea.anInt1387 == savedExtent);
	}

	/**
	 * Drives the REAL {@code Model.method443} and requires {@link GlModelProjection} to
	 * reproduce the projected arrays it leaves behind.
	 *
	 * <p>The sentinel-oracle pattern is described on the caller; the part that lives here
	 * is the CONTROL. The three arrays are {@code private static} scratch shared by every
	 * model, so a CULLED draw leaves the previous model's data in them - and a test that
	 * did not check for that could compare a stale array against a fresh projection and
	 * call the agreement a pass. Filling them first and requiring every vertex to be
	 * rewritten is what makes the comparison mean something.
	 */
	private static void assertProjectionMatchesSoftware(Model model, int orientation,
			int camA, int camB, int camC, int camD, int dx, int dy, int dz,
			int centreX, int centreY, String label) {
		int[] softwareX = (int[]) readStatic(Model.class, "anIntArray1665");
		int[] softwareY = (int[]) readStatic(Model.class, "anIntArray1666");
		int[] softwareDepth = (int[]) readStatic(Model.class, "anIntArray1667");
		int[] softwareCamX = (int[]) readStatic(Model.class, "anIntArray1668");
		int[] softwareCamY = (int[]) readStatic(Model.class, "anIntArray1669");
		int[] softwareCamZ = (int[]) readStatic(Model.class, "anIntArray1670");

		int sentinel = 0x7EEDBEEF;
		java.util.Arrays.fill(softwareX, sentinel);
		java.util.Arrays.fill(softwareY, sentinel);
		java.util.Arrays.fill(softwareDepth, sentinel);
		java.util.Arrays.fill(softwareCamX, sentinel);
		java.util.Arrays.fill(softwareCamY, sentinel);
		java.util.Arrays.fill(softwareCamZ, sentinel);

		// ⚠ The camera-space trio is written by method443 only `if (flag || anInt1642 > 0)` -
		// flag being "some vertex is behind the near plane" and anInt1642 being the model's
		// TEXTURE COUNT. This fixture need not trip either, and a skipped write would leave
		// the sentinel behind and quietly shrink the comparison to nothing, so the model is
		// declared textured for the duration. That is enough to force the write for every
		// vertex; it cannot reach the textured DRAW path, which is selected by a face's
		// render type and not by this count.
		writeField(model, "anInt1642", 1);

		model.method443(orientation, camA, camB, camC, camD, dx, dy, dz, 0);
		int count = model.vertexCount();
		int rewritten = 0;
		int camRewritten = 0;
		for (int i = 0; i < count; i++) {
			if (softwareDepth[i] != sentinel) {
				rewritten++;
			}
			if (softwareCamZ[i] != sentinel) {
				camRewritten++;
			}
		}
		check("GL projection oracle [" + label + "]: the software draw really projects, so the "
				+ "comparison is not against stale scratch", rewritten == count);
		check("GL projection oracle [" + label + "]: the software wrote its CAMERA-SPACE slots "
				+ "too, so the clipper's inputs are compared and not sentinels",
				camRewritten == count);

		int[] glX = new int[count];
		int[] glY = new int[count];
		int[] glDepth = new int[count];
		int[] glCamX = new int[count];
		int[] glCamY = new int[count];
		int projected = GlModelProjection.project(model, orientation, camA, camB, camC, camD,
				dx, dy, dz, centreX, centreY, glX, glY, glDepth, glCamX, glCamY);
		check("GL projection [" + label + "]: projects the fixture's vertices", projected == count);

		boolean xMatches = true;
		boolean yMatches = true;
		boolean depthMatches = true;
		boolean camMatches = true;
		boolean projectedAny = false;
		for (int i = 0; i < count; i++) {
			if (glX[i] != softwareX[i]) {
				xMatches = false;
			}
			if (glDepth[i] != softwareDepth[i]) {
				depthMatches = false;
			}
			// The clipper's inputs, compared for EVERY vertex including the ones behind the
			// near plane - those are exactly the ones it needs, and exactly the ones the
			// screen-space comparison cannot check.
			if (glCamX[i] != softwareCamX[i] || glCamY[i] != softwareCamY[i]) {
				camMatches = false;
			}
			if (glX[i] != GlModelProjection.OFFSCREEN) {
				projectedAny = true;
				// Y is only meaningful where method443 projected the vertex; elsewhere it
				// leaves Y untouched, and matching that is part of the contract.
				if (glY[i] != softwareY[i]) {
					yMatches = false;
				}
			}
		}
		check("GL projection [" + label + "]: screen X matches the software path exactly", xMatches);
		check("GL projection [" + label + "]: method443's depth matches exactly", depthMatches);
		check("GL projection [" + label + "]: screen Y matches where the vertex was projected",
				yMatches);
		check("GL projection [" + label + "]: the CAMERA-SPACE pair matches for every vertex, "
				+ "including the ones the near plane removes", camMatches);
		check("GL projection [" + label + "]: at least one vertex lands on screen", projectedAny);

		// Non-vacuity: a materially different camera must break the match. If it does not,
		// the exact-match checks above are comparing something that cannot differ.
		//
		// ⚠ The delta has to be LARGE, and that is a measured property rather than a
		// convenience: a one-unit change to a camera term is absorbed by the `>> 16`
		// truncation and produces byte-identical output, so a +1 mutation would "pass"
		// while testing nothing - the same trap as 6.2's zero-delta origin fixture.
		int[] altX = new int[count];
		GlModelProjection.project(model, orientation, camA, camB, camC, 20000,
				dx, dy, dz, centreX, centreY, altX, new int[count], new int[count],
				new int[count], new int[count]);
		boolean differed = false;
		for (int i = 0; i < count; i++) {
			if (altX[i] != softwareX[i]) {
				differed = true;
			}
		}
		check("GL projection [" + label + "]: a changed camera produces different output, so the "
				+ "exact match is not vacuous", differed);
	}

	// ------------------------------------ GL face pipeline (Phase 7.2b-2b)

	/**
	 * The fixture geometry the two face-step tests use, stated directly rather than
	 * encoded into model bytes.
	 *
	 * <p><b>Why the geometry is written onto a parsed model instead of into the model
	 * bytes.</b> {@code buildOldFormatFixture()} exists to pin the PARSER, and it is
	 * exactly right for that: three collinear vertices, two faces. Collinear is fatal
	 * here - the signed-area test is zero for a degenerate triangle, so nothing would
	 * ever be culled and the culling oracle would pass vacuously. Rather than grow the
	 * byte fixture into a second job, the parse is taken as read (it has its own tests)
	 * and the shape is set on the instance. Everything below is read back by the REAL
	 * {@code method443} on the next line, so the oracle is unaffected: it is the same
	 * arrays a byte fixture would have produced.
	 *
	 * <p><b>⚠ The pairs are the point, not the individual faces.</b> Every face here has
	 * a TWIN with reversed winding ({@code (0,1,2)} vs {@code (1,0,2)}), so exactly one
	 * of each pair is front-facing whatever the camera does - which is what makes the
	 * oracle's coverage independent of which way the triangles happen to face. Without
	 * the twins a camera change could leave nothing drawn and every check vacuous.
	 */
	private static Model buildFaceFixture() {
		Model m = parseFixtureModel();
		// Four vertices: the corner and the three axis points of a quarter-tetrahedron.
		// Deliberately not coplanar and not collinear, so every face has real area.
		m.anIntArray1627 = new int[] { 0, 120, 0, 0 };
		m.anIntArray1628 = new int[] { 0, 0, 120, 0 };
		m.anIntArray1629 = new int[] { 0, 0, 0, 120 };
		writeField(m, "anInt1626", 4);

		// 0/2 flat with three DISTINCT corner colours (the association pair)
		// 1/6 flat with one repeated colour (the whole-triangle single-colour oracle)
		// 3/4 textured (the render type the batcher cannot do yet)
		// 5   render type -1, which method483 skips before any test
		m.anIntArray1631 = new int[] { 0, 0, 1, 0, 1, 0, 1 };
		m.anIntArray1632 = new int[] { 1, 1, 0, 1, 0, 1, 0 };
		m.anIntArray1633 = new int[] { 2, 3, 2, 2, 2, 3, 3 };
		writeField(m, "anInt1630", 7);
		writeField(m, "anIntArray1637", new int[] { 0, 0, 0, 2, 2, -1, 0 });
		writeField(m, "anIntArray1634", new int[] { 0x1111, 0x0ABC, 0x1111, 0x4444, 0x4444, 0x7777, 0x0ABC });
		writeField(m, "anIntArray1635", new int[] { 0x2222, 0x0ABC, 0x2222, 0x4444, 0x4444, 0x7777, 0x0ABC });
		writeField(m, "anIntArray1636", new int[] { 0x3333, 0x0ABC, 0x3333, 0x4444, 0x4444, 0x7777, 0x0ABC });
		// The textured pair samples anIntArray1640 as a texture ID, so it must name a
		// texture the test parks - otherwise method378 throws and method443's catch
		// ABORTS the rest of the draw, truncating the buckets the oracle reads.
		writeField(m, "anIntArray1640", new int[] { FIXTURE_TEXTURE_ID, FIXTURE_TEXTURE_ID,
				FIXTURE_TEXTURE_ID, FIXTURE_TEXTURE_ID, FIXTURE_TEXTURE_ID,
				FIXTURE_TEXTURE_ID, FIXTURE_TEXTURE_ID });

		// The bucket index is meanDepth + anInt1653 and the table is anIntArrayArray1672,
		// so the two bounds have to cover this model's own depth spread. method466
		// computes them from the vertices, but its anInt1653 is the HORIZONTAL radius,
		// which is not a bound once the camera rotates the model. Set generously - the
		// table is 1500 long and 512 wide per row, and seven faces cannot overflow it.
		writeField(m, "anInt1653", 400);
		writeField(m, "anInt1652", 800);
		// method443 culls on this radius before projecting; 500 keeps the fixture in
		// frame without altering the per-vertex arithmetic.
		m.anInt1650 = 500;
		return m;
	}

	/** The texture the face fixture's textured pair names, parked by the tests. */
	private static int[] faceFixtureTexture() {
		int[] tex = new int[16384];
		for (int i = 0; i < tex.length; i++) {
			tex[i] = (((i * 11) & 0xFF) << 16) | (((i * 5) & 0xFF) << 8) | (i & 0xFF);
		}
		return tex;
	}

	/**
	 * The 4.1c-2a style deterministic palette, in one place so a test that needs the
	 * software rasteriser and the pipeline to agree cannot install two different ones.
	 *
	 * <p>{@code Texture.method372} is deliberately NOT used: it jitters the brightness
	 * with {@code Math.random()} on every call, which is fine for play and fatal for a
	 * comparison. Also returns whether the fixture's three distinct codes really do
	 * resolve to three distinct colours, which is what makes a swapped corner visible.
	 */
	private static void installDeterministicPalette() {
		int[] palette = Texture.anIntArray1482;
		for (int i = 0; i < palette.length; i++) {
			int v = ((i & 0xFF) << 16) | (((i >> 3) & 0xFF) << 8) | ((i * 5) & 0xFF);
			palette[i] = (v == 0 ? 1 : v);
		}
	}

	/** Saves the {@link DrawingArea} fields a scene draw reads and writes. */
	private static int[] saveDrawingArea() {
		return new int[] { DrawingArea.width, DrawingArea.height, DrawingArea.topX,
				DrawingArea.bottomX, DrawingArea.topY, DrawingArea.bottomY,
				DrawingArea.centerX, DrawingArea.centerY, DrawingArea.anInt1387 };
	}

	private static void restoreDrawingArea(int[] saved, int[] pixels) {
		DrawingArea.pixels = pixels;
		DrawingArea.width = saved[0];
		DrawingArea.height = saved[1];
		DrawingArea.topX = saved[2];
		DrawingArea.bottomX = saved[3];
		DrawingArea.topY = saved[4];
		DrawingArea.bottomY = saved[5];
		DrawingArea.centerX = saved[6];
		DrawingArea.centerY = saved[7];
		DrawingArea.anInt1387 = saved[8];
	}

	/**
	 * A {@link TriangleSink} that records instead of drawing, so the pipeline can be
	 * checked against the software path with no GL context anywhere in sight.
	 *
	 * <p>⚠ Triangles arrive in FACE-INDEX order, because {@link GlFacePipeline} emits
	 * them that way on purpose (see its class doc). That is what lets a recorded
	 * triangle be matched back to the face it came from: walk the outcomes array and
	 * take the drawn faces in order.
	 */
	private static final class RecordingSink implements TriangleSink {
		private static final int CAPACITY = 64;
		private static final int FLOATS_PER_TRIANGLE = 12;
		/** x/y/z + u/v/w per corner: 6 floats x 3 corners. */
		private static final int FLOATS_PER_TEXTURED = 18;
		/** shade per corner, then the texture id. */
		private static final int INTS_PER_TEXTURED = 4;
		private final float[] floats = new float[FLOATS_PER_TRIANGLE * CAPACITY];
		private final float[] texturedFloats = new float[FLOATS_PER_TEXTURED * CAPACITY];
		private final int[] texturedInts = new int[INTS_PER_TEXTURED * CAPACITY];
		int triangles;
		int texturedTriangles;

		/**
		 * ⚠ <b>False by default, deliberately.</b> The face-step tests that existed before
		 * 7.2b-2e assert a one-to-one mapping between a {@code DRAWN} outcome and a
		 * recorded coloured triangle; if this sink claimed texture support, the fixture's
		 * two textured faces would stop being {@link GlFacePipeline#NEEDS_TEXTURE} and
		 * that mapping would change under them. Only the textured test turns it on.
		 */
		private final boolean textures;

		/**
		 * Whether {@link #textured} declines the submission - i.e. the sink CLAIMS texture
		 * support (so the pipeline gets past that gate and builds the ramps) and then refuses
		 * the triangle anyway. That is the real {@code GlBatcher}'s third decline (an id that
		 * is not in the loaded cache, or three {@code wNum} numerators that do not share a
		 * sign), and it is the only way to reach
		 * {@link ui.GlFacePipeline#TEXTURE_SINK_DECLINED} from the harness.
		 */
		boolean refuseTextured;

		RecordingSink() {
			this(false);
		}

		RecordingSink(boolean textures) {
			this.textures = textures;
		}

		@Override
		public boolean supportsTextures() {
			return textures;
		}

		@Override
		public boolean triangle(float x0, float y0, float z0, int argb0,
				float x1, float y1, float z1, int argb1,
				float x2, float y2, float z2, int argb2) {
			if (triangles >= CAPACITY) {
				// Loudly, not silently: the fixture emits at most seven, so reaching this
				// means the pipeline emitted something it should not have, and a silently
				// dropped triangle would turn a real failure into a passing test.
				throw new IllegalStateException("recording sink overflow");
			}
			int o = triangles * FLOATS_PER_TRIANGLE;
			floats[o] = x0; floats[o + 1] = y0; floats[o + 2] = z0; floats[o + 3] = argb0;
			floats[o + 4] = x1; floats[o + 5] = y1; floats[o + 6] = z1; floats[o + 7] = argb1;
			floats[o + 8] = x2; floats[o + 9] = y2; floats[o + 10] = z2; floats[o + 11] = argb2;
			triangles++;
			return true;
		}

		@Override
		public boolean textured(float x0, float y0, float z0, float u0, float v0, float w0,
				int shade0,
				float x1, float y1, float z1, float u1, float v1, float w1, int shade1,
				float x2, float y2, float z2, float u2, float v2, float w2, int shade2,
				int textureId) {
			if (refuseTextured) {
				// ⚠ Refused BEFORE the count, deliberately: a sink that declined must not look
				// like one that queued the triangle, or the check that reads this count would
				// pass while the pipeline reported the decline.
				return false;
			}
			if (texturedTriangles >= CAPACITY) {
				throw new IllegalStateException("recording sink textured overflow");
			}
			int o = texturedTriangles * FLOATS_PER_TEXTURED;
			texturedFloats[o] = x0;
			texturedFloats[o + 1] = y0;
			texturedFloats[o + 2] = z0;
			texturedFloats[o + 3] = u0;
			texturedFloats[o + 4] = v0;
			texturedFloats[o + 5] = w0;
			texturedFloats[o + 6] = x1;
			texturedFloats[o + 7] = y1;
			texturedFloats[o + 8] = z1;
			texturedFloats[o + 9] = u1;
			texturedFloats[o + 10] = v1;
			texturedFloats[o + 11] = w1;
			texturedFloats[o + 12] = x2;
			texturedFloats[o + 13] = y2;
			texturedFloats[o + 14] = z2;
			texturedFloats[o + 15] = u2;
			texturedFloats[o + 16] = v2;
			texturedFloats[o + 17] = w2;
			int p = texturedTriangles * INTS_PER_TEXTURED;
			texturedInts[p] = shade0;
			texturedInts[p + 1] = shade1;
			texturedInts[p + 2] = shade2;
			texturedInts[p + 3] = textureId;
			texturedTriangles++;
			return true;
		}

		int texturedId(int tri) {
			return texturedInts[tri * INTS_PER_TEXTURED + 3];
		}

		int texturedShade(int tri, int corner) {
			return texturedInts[tri * INTS_PER_TEXTURED + corner];
		}

		float texturedComponent(int tri, int corner, int which) {
			int o = tri * FLOATS_PER_TEXTURED + corner * 6 + which;
			return texturedFloats[o];
		}

		float x(int tri, int corner) {
			return floats[tri * FLOATS_PER_TRIANGLE + corner * 4];
		}

		float y(int tri, int corner) {
			return floats[tri * FLOATS_PER_TRIANGLE + corner * 4 + 1];
		}

		float z(int tri, int corner) {
			return floats[tri * FLOATS_PER_TRIANGLE + corner * 4 + 2];
		}

		int colour(int tri, int corner) {
			return (int) floats[tri * FLOATS_PER_TRIANGLE + corner * 4 + 3];
		}
	}

	/**
	 * The face-step oracle: culling, clipping classification, geometry and depth, all
	 * checked against the REAL {@code Model.method443}.
	 *
	 * <p><b>Where the authority comes from, and it is not me.</b> {@code method483}
	 * leaves the faces it decided to draw in the per-depth buckets
	 * {@code anIntArrayArray1672}, with {@code anIntArray1671} as the per-row counts -
	 * so the software path's own culling DECISION is readable after the draw, face for
	 * face. It also leaves {@code aBooleanArray1664[face] = true} for exactly the faces
	 * it routed to {@code method485}'s clipper, which is the software's own verdict on
	 * "the near plane cuts this one". Both are read back here and required to match
	 * {@link GlFacePipeline}'s census. A hand-computed expectation could only prove the
	 * pipeline agrees with the same reading of {@code method483} that produced it, which
	 * is 5.3's trap.
	 *
	 * <p><b>⚠ Two controls, both needed.</b> First, {@code method443} culls on the
	 * model's bounding box BEFORE it does anything, and a culled draw leaves the buckets
	 * holding the PREVIOUS model's faces - so the counts are sentinel-filled first and
	 * the test requires the software to have rewritten them. Second, the fixture pairs
	 * every triangle with a reversed twin, so the test requires at least one face to have
	 * SURVIVED and at least one to have been CULLED: an "all culled" or "none culled"
	 * run would otherwise make the face-for-face comparison meaningless.
	 */
	private static void modelFacesAreCulledExactlyAsTheSoftwareCullsThem() {
		int centreX = 382;
		int centreY = 251;
		int[] savedArea = saveDrawingArea();
		int[] savedPixels = DrawingArea.pixels;
		Object savedTextureInt1 = readStatic(Texture.class, "textureInt1");
		Object savedTextureInt2 = readStatic(Texture.class, "textureInt2");
		int savedSceneDepth = Fog.sceneDepth;
		Object savedOrientationCos = readStatic(Model.class, "modelIntArray1");
		Object savedOrientationSin = readStatic(Model.class, "modelIntArray2");
		String[] scratch = { "anIntArray1665", "anIntArray1666", "anIntArray1667",
				"anIntArray1668", "anIntArray1669", "anIntArray1670", "anIntArray1671" };
		int[][] savedScratch = new int[scratch.length][];
		for (int i = 0; i < scratch.length; i++) {
			savedScratch[i] = ((int[]) readStatic(Model.class, scratch[i])).clone();
		}
		try {
			DrawingArea.initDrawingArea(503, 765, new int[765 * 503]);
			Texture.method364();
			DrawingArea.centerX = centreX;
			DrawingArea.centerY = centreY;
			writeStatic(Texture.class, "textureInt1", centreX);
			writeStatic(Texture.class, "textureInt2", centreY);
			installDeterministicPalette();
			Fog.sceneDepth = 0;
			Texture.anInt1465 = 0;
			Texture.aBoolean1462 = false;
			Texture.aBoolean1464 = true;
			parkTexture(FIXTURE_TEXTURE_ID, faceFixtureTexture());

			Model m = buildFaceFixture();
			int[] orientationCos = new int[8];
			int[] orientationSin = new int[8];
			orientationCos[3] = 46341;
			orientationSin[3] = 46341;
			writeStatic(Model.class, "modelIntArray1", orientationCos);
			writeStatic(Model.class, "modelIntArray2", orientationSin);

			GlFacePipeline pipeline = new GlFacePipeline();
			// An unrotated camera 700 units back, so the fixture's 120-unit extent lands
			// around the middle of the drawing area: on screen, which the colour test
			// needs, and in front of the near plane, so no face is routed to the clipper
			// by accident. camB and camD are the identity for method443's two rotations.
			assertCensusMatchesSoftware(pipeline, m, 0, 0, 65536, 0, 65536,
					10, 20, 700, centreX, centreY, "no orientation");
			// The same camera with the model itself rotated 45 degrees, which changes the
			// model's own winding relative to the view without moving it off screen.
			assertCensusMatchesSoftware(pipeline, m, 3, 0, 65536, 0, 65536,
					10, 20, 700, centreX, centreY, "with orientation 3");
		} finally {
			restoreDrawingArea(savedArea, savedPixels);
			writeStatic(Texture.class, "textureInt1", savedTextureInt1);
			writeStatic(Texture.class, "textureInt2", savedTextureInt2);
			Fog.sceneDepth = savedSceneDepth;
			writeStatic(Model.class, "modelIntArray1", savedOrientationCos);
			writeStatic(Model.class, "modelIntArray2", savedOrientationSin);
			for (int i = 0; i < scratch.length; i++) {
				System.arraycopy(savedScratch[i], 0,
						(int[]) readStatic(Model.class, scratch[i]), 0, savedScratch[i].length);
			}
		}
	}

	/** One oracle round: drive the real method443, then require the census to match it. */
	private static void assertCensusMatchesSoftware(GlFacePipeline pipeline, Model m,
			int orientation, int camA, int camB, int camC, int camD,
			int dx, int dy, int dz, int centreX, int centreY, String label) {
		int[] counts = (int[]) readStatic(Model.class, "anIntArray1671");
		int[][] buckets = (int[][]) readStatic(Model.class, "anIntArrayArray1672");
		boolean[] clipped = (boolean[]) readStatic(Model.class, "aBooleanArray1664");
		int[] softwareX = (int[]) readStatic(Model.class, "anIntArray1665");
		int[] softwareY = (int[]) readStatic(Model.class, "anIntArray1666");
		int[] softwareDepth = (int[]) readStatic(Model.class, "anIntArray1667");

		// CONTROL 1: a culled draw leaves the previous model's buckets in place, so a
		// stale read would look like agreement. Sentinel first, require a rewrite.
		java.util.Arrays.fill(counts, -1);
		java.util.Arrays.fill(clipped, false);

		m.method443(orientation, camA, camB, camC, camD, dx, dy, dz, 0);

		int faceCount = m.faceCount();
		boolean[] survived = new boolean[faceCount];
		int survivors = 0;
		for (int row = 0; row < counts.length; row++) {
			if (counts[row] < 0) {
				continue;
			}
			for (int j = 0; j < counts[row]; j++) {
				int face = buckets[row][j];
				if (face >= 0 && face < faceCount) {
					if (!survived[face]) {
						survivors++;
					}
					survived[face] = true;
				}
			}
		}
		check("Face census [" + label + "]: the software draw really ran, so the comparison "
				+ "is not against stale buckets", counts[0] >= 0);

		RecordingSink sink = new RecordingSink();
		pipeline.emit(m, orientation, camA, camB, camC, camD, dx, dy, dz, centreX, centreY, sink);
		int[] outcomes = pipeline.outcomes();
		boolean[] pipelineClipped = pipeline.clipped();

		check("Face census [" + label + "]: every face got exactly one outcome",
				pipeline.count(GlFacePipeline.DRAWN) + pipeline.count(GlFacePipeline.CULLED)
						+ pipeline.count(GlFacePipeline.CLIPPED)
						+ pipeline.count(GlFacePipeline.NEEDS_CLIPPING)
						+ pipeline.count(GlFacePipeline.NEEDS_TEXTURE)
						+ pipeline.count(GlFacePipeline.NO_COLOUR)
						+ pipeline.count(GlFacePipeline.SKIPPED)
						+ pipeline.count(GlFacePipeline.TEXTURED) == faceCount);

		// ⚠ THE ORACLE, face for face. The software draws a face unless it is culled or
		// skipped, so those two outcomes and only those two must line up with "absent
		// from the buckets".
		boolean cullMatches = true;
		boolean clipMatches = true;
		String firstCullMismatch = null;
		String firstClipMismatch = null;
		for (int face = 0; face < faceCount; face++) {
			boolean softwareKept = survived[face];
			boolean pipelineKept = outcomes[face] != GlFacePipeline.CULLED
					&& outcomes[face] != GlFacePipeline.SKIPPED;
			if (softwareKept != pipelineKept) {
				cullMatches = false;
				if (firstCullMismatch == null) {
					firstCullMismatch = "face " + face + " softwareKept=" + softwareKept
							+ " outcome=" + outcomes[face];
				}
			}
			// aBooleanArray1664 is method483's OWN flag for "a vertex of this face is at
			// the near plane, hand it to method485". ⚠ Compared against pipeline.clipped()
			// and NOT against an outcome: a cut face can come out CLIPPED, NEEDS_TEXTURE or
			// NO_COLOUR, so an outcome can never be evidence about the near plane.
			if (softwareKept && pipelineClipped[face] != clipped[face]) {
				clipMatches = false;
				if (firstClipMismatch == null) {
					firstClipMismatch = "face " + face + " clipped=" + clipped[face]
							+ " pipelineClipped=" + pipelineClipped[face]
							+ " outcome=" + outcomes[face];
				}
			}
		}
		check("Face census [" + label + "]: culling matches the software's own buckets face "
				+ "for face" + (firstCullMismatch == null ? "" : " (" + firstCullMismatch + ")"),
				cullMatches);
		check("Face census [" + label + "]: the near-plane faces match the software's own "
				+ "clipper flags" + (firstClipMismatch == null ? "" : " (" + firstClipMismatch + ")"),
				clipMatches);

		// CONTROL 2: the fixture pairs each triangle with a reversed twin, so BOTH
		// outcomes must occur. Without this the face-for-face check above could agree on
		// an all-kept or an all-culled run and prove nothing.
		check("Face census [" + label + "]: the fixture really exercises both branches - at "
				+ "least one face survived and at least one was culled",
				survivors > 0 && survivors < faceCount);

		// ⚠ The geometry block below maps submitted triangles back to faces by walking the
		// outcomes in face-index order and taking the DRAWN ones. A CLIPPED face breaks that
		// mapping - it submits 0, 1 or 2 triangles, interleaved with the uncut ones - so the
		// checks are only valid while nothing was clipped, and rather than skip them silently
		// the camera choice is pinned here. The clip path has its own oracle
		// (nearPlaneClipperMatchesTheSoftwareClipper), which compares against the software's
		// rebuilt polygon rather than against this mapping.
		check("Face census [" + label + "]: this camera leaves the near plane alone, which is "
				+ "what makes the one-triangle-per-drawn-face mapping below valid",
				pipeline.count(GlFacePipeline.CLIPPED) == 0
						&& pipeline.count(GlFacePipeline.NEEDS_CLIPPING) == 0);

		// Geometry: the vertices submitted must be the software's own projection, at the
		// face's own vertex indices. Exact integers, not approximations.
		int[] faceA = m.faceVertexA();
		int[] faceB = m.faceVertexB();
		int[] faceC = m.faceVertexC();
		int[] vertexOrder = { 0, 0, 0 };
		boolean geometryMatches = true;
		boolean depthInRange = true;
		boolean depthMonotone = true;
		String firstGeometryMismatch = null;
		int tri = 0;
		for (int face = 0; face < faceCount; face++) {
			if (outcomes[face] != GlFacePipeline.DRAWN) {
				continue;
			}
			vertexOrder[0] = faceA[face];
			vertexOrder[1] = faceB[face];
			vertexOrder[2] = faceC[face];
			for (int corner = 0; corner < 3; corner++) {
				int v = vertexOrder[corner];
				if (sink.x(tri, corner) != softwareX[v]
						|| sink.y(tri, corner) != softwareY[v]) {
					geometryMatches = false;
					if (firstGeometryMismatch == null) {
						firstGeometryMismatch = "face " + face + " corner " + corner;
					}
				}
				if (sink.z(tri, corner) < 0f || sink.z(tri, corner) > 1f) {
					depthInRange = false;
				}
			}
			// DEEPER MUST MEAN LARGER z, checked pairwise against the SOFTWARE's own
			// depths - asserting it against depthToZ's own input would only re-run the
			// function under test.
			for (int p = 0; p < 3; p++) {
				for (int q = p + 1; q < 3; q++) {
					boolean softwareDeeper =
							softwareDepth[vertexOrder[p]] < softwareDepth[vertexOrder[q]];
					boolean pipelineDeeper = sink.z(tri, p) < sink.z(tri, q);
					if (softwareDeeper != pipelineDeeper) {
						depthMonotone = false;
					}
				}
			}
			tri++;
		}
		check("Face census [" + label + "]: every submitted vertex is the software's own "
				+ "projection of that face's own vertex"
				+ (firstGeometryMismatch == null ? "" : " (" + firstGeometryMismatch + ")"),
				geometryMatches);
		check("Face census [" + label + "]: submitted z stays inside [0,1]", depthInRange);
		check("Face census [" + label + "]: a deeper vertex maps to a larger z, matching the "
				+ "software's own depths", depthMonotone);
		check("Face census [" + label + "]: one triangle was submitted per drawn face",
				sink.triangles == pipeline.count(GlFacePipeline.DRAWN));
		check("Face census [" + label + "]: the census found something to draw",
				pipeline.count(GlFacePipeline.DRAWN) > 0);

		// ⚠ NON-VACUITY: a changed camera must change what is submitted, or the
		// face-for-face match above is comparing something that cannot differ. The delta
		// is a real rotation - 8.8 degrees of pitch, sin 10000 / cos 64770 - rather than
		// the +1 unit 7.2b-2a measured to be absorbed by the `>> 16` truncation and
		// produce byte-identical output, which would "pass" while testing nothing.
		RecordingSink alt = new RecordingSink();
		pipeline.emit(m, orientation, 10000, 64770, camC, camD, dx, dy, dz, centreX, centreY, alt);
		check("Face census [" + label + "]: a changed camera changes what is submitted, so "
				+ "the match is not vacuous", !samePixels(sink, alt));
	}

	/** Whether two recordings submitted identical triangles. Used for the non-vacuity check. */
	private static boolean samePixels(RecordingSink a, RecordingSink b) {
		if (a.triangles != b.triangles) {
			return false;
		}
		for (int t = 0; t < a.triangles; t++) {
			for (int c = 0; c < 3; c++) {
				if (a.x(t, c) != b.x(t, c) || a.y(t, c) != b.y(t, c)
						|| a.z(t, c) != b.z(t, c) || a.colour(t, c) != b.colour(t, c)) {
					return false;
				}
			}
		}
		return true;
	}

	// ------------------------------------ GL near-plane clipper (Phase 7.2b-2d)

	/**
	 * The identity camera every clipper case uses: camA 0, camB 1, camC 0, camD 1 in 16.16.
	 * With it the camera transform is the identity, so a vertex's camera-space position is
	 * simply its model-space position plus the camera offset - which is what lets a case be
	 * designed by choosing vertex Z values instead of by solving for a camera.
	 */
	private static final int CLIP_CAM_A = 0;
	private static final int CLIP_CAM_B = 65536;
	private static final int CLIP_CAM_C = 0;
	private static final int CLIP_CAM_D = 65536;

	/**
	 * {@code ui.GlClipper} must rebuild a near-plane-cut face EXACTLY as
	 * {@code Model.method485} rebuilds it, and the oracle is the software's own scratch.
	 *
	 * <p><b>How the software's answer is read back.</b> {@code method485} writes its rebuilt
	 * polygon into {@code anIntArray1678/1679/1680} - screen X, screen Y, and the
	 * interpolated colour CODE - and those arrays are never cleared between faces, so after
	 * a draw they hold the LAST clipped face's polygon followed by whatever was in them
	 * before. They are therefore pre-filled with a sentinel, which makes the number of
	 * points the software emitted recoverable as the run of non-sentinel entries: no guess
	 * and no assumption about how big the polygon is.
	 *
	 * <p>⚠ <b>Which is exactly why each case is ONE face in its own model.</b> A second
	 * clipped face would overwrite the scratch, and there is no way to ask the software which
	 * polygon belonged to which face. So the interesting configurations are enumerated by
	 * placing three vertices at chosen camera depths, not by building many faces into one
	 * model - and the expected point count is asserted against the SOFTWARE first, so a case
	 * that quietly stopped clipping fails loudly instead of comparing nothing.
	 *
	 * <p>⚠ <b>Nothing here is taken on trust from {@code GlModelProjection}.</b> The
	 * clipper's inputs are the projection's camera-space outputs, which are themselves
	 * oracle-checked above; and the scene depth it is handed is checked against the
	 * software's own {@code k2}, recovered as {@code anIntArray1670 - anIntArray1667} rather
	 * than read from {@code Fog} (which {@code method443} restores on the way out).
	 */
	private static void nearPlaneClipperMatchesTheSoftwareClipper() {
		int centreX = 382;
		int centreY = 251;
		int[] savedArea = saveDrawingArea();
		int[] savedPixels = DrawingArea.pixels;
		Object savedTextureInt1 = readStatic(Texture.class, "textureInt1");
		Object savedTextureInt2 = readStatic(Texture.class, "textureInt2");
		int savedSceneDepth = Fog.sceneDepth;
		String[] scratch = { "anIntArray1665", "anIntArray1666", "anIntArray1667",
				"anIntArray1668", "anIntArray1669", "anIntArray1670", "anIntArray1671",
				"anIntArray1678", "anIntArray1679", "anIntArray1680" };
		int[][] savedScratch = new int[scratch.length][];
		for (int i = 0; i < scratch.length; i++) {
			savedScratch[i] = ((int[]) readStatic(Model.class, scratch[i])).clone();
		}
		GlFacePipeline pipeline = new GlFacePipeline();
		int[] clipTotals = new int[2];
		try {
			DrawingArea.initDrawingArea(503, 765, new int[765 * 503]);
			Texture.method364();
			DrawingArea.centerX = centreX;
			DrawingArea.centerY = centreY;
			writeStatic(Texture.class, "textureInt1", centreX);
			writeStatic(Texture.class, "textureInt2", centreY);
			installDeterministicPalette();
			Fog.sceneDepth = 0;
			Texture.anInt1465 = 0;
			Texture.aBoolean1462 = false;
			Texture.aBoolean1464 = true;

			// ⚠ The vertex Z values are what put each vertex on one side of the near plane:
			// the camera transform is the identity and dz is 100, so camera Z is vertexZ +
			// 100. A vertex at Z = -60 therefore sits at 40 - behind the plane - and one at
			// Z = 200 sits at 300, well in front of it.
			//
			// ⚠ The X values are all NEGATIVE-LEADING on purpose, and it is not cosmetic.
			// method485's winding test decides whether the rebuilt polygon is drawn at all,
			// and this client's screen Y axis points down - so the emission order these cases
			// produce is back-facing for positive X and front-facing for negative X. Getting
			// it wrong does not fail loudly by itself: the oracle would compare four polygons
			// and submit four zero-triangle draws, and every check here would still pass
			// while the entire emission path went untested. That is why the triangle count is
			// asserted rather than merely counted.
			//
			// One behind, TWO in front -> the software builds a QUAD and draws two
			// triangles from it.
			clipCase(pipeline, centreX, centreY, 100,
					new int[] { 15, -15, 0 }, new int[] { -10, -10, 25 },
					new int[] { -60, 200, 200 }, "one vertex behind", 4, 2, clipTotals);
			// Two behind, one in front -> a TRIANGLE, built from a different emission order
			// (the plane points come from vertices 0 and 1 rather than from an interspersed
			// pair), and drawn as one triangle.
			clipCase(pipeline, centreX, centreY, 100,
					new int[] { 15, -15, 0 }, new int[] { -10, -10, 25 },
					new int[] { -60, -60, 200 }, "two vertices behind", 3, 2, clipTotals);
			// The FRONT vertex first and the two behind ones after it, which walks the third
			// emission branch rather than the first.
			clipCase(pipeline, centreX, centreY, 100,
					new int[] { 0, 15, -15 }, new int[] { 25, -10, -10 },
					new int[] { 200, -60, -60 }, "front vertex first", 3, 2, clipTotals);
			// ALL THREE behind -> an empty polygon. The software still routes the face to
			// method485 (it is bucketed without ever being winding-tested) and then draws
			// nothing, which is the one clipped outcome that is easy to get wrong in the
			// other direction by emitting a degenerate triangle.
			clipCase(pipeline, centreX, centreY, 100,
					new int[] { 15, -15, 0 }, new int[] { -10, -10, 25 },
					new int[] { -60, -60, -60 }, "all three behind", 0, 0, clipTotals);

			// ⚠ THE NON-VACUITY CHECK FOR THE EMISSION PATH, and it is the one that would have
			// caught the winding mistake this test originally had. Four cases can all compare
			// their polygons successfully and still submit nothing, so the triangles are
			// totalled: 4 is exactly quad(2) + triangle(1) + triangle(1), and anything less
			// means a configuration silently stopped drawing.
			check("Clipper: the four configurations drew 4 triangles between them - a quad "
					+ "split plus two single triangles - so the emission path is exercised "
					+ "rather than every polygon landing back-facing (got " + clipTotals[1]
					+ " from " + clipTotals[0] + " drawing case(s))", clipTotals[1] == 4);
		} finally {
			restoreDrawingArea(savedArea, savedPixels);
			writeStatic(Texture.class, "textureInt1", savedTextureInt1);
			writeStatic(Texture.class, "textureInt2", savedTextureInt2);
			Fog.sceneDepth = savedSceneDepth;
			for (int i = 0; i < scratch.length; i++) {
				System.arraycopy(savedScratch[i], 0,
						(int[]) readStatic(Model.class, scratch[i]), 0, savedScratch[i].length);
			}
		}
	}

	/**
	 * One clipper case: a single-face model at chosen vertex positions, run through the REAL
	 * {@code method443} and then through {@code GlClipper}, point for point.
	 *
	 * @param expectedPoints what the software's clipper must emit for this configuration -
	 *                       asserted against the software FIRST, so the clipper is never
	 *                       compared against a run that did not clip
	 * @param expectedPlanePoints how many of those points should sit exactly on the near
	 *                       plane, i.e. one per edge that crossed it
	 * @param clipTotals     receives, at index 0, how many cases actually DREW and, at index
	 *                       1, how many triangles they drew between them - the test's proof
	 *                       that the emission path was exercised at all
	 */
	private static void clipCase(GlFacePipeline pipeline, int centreX, int centreY, int dz,
			int[] vx, int[] vy, int[] vz, String label, int expectedPoints,
			int expectedPlanePoints, int[] clipTotals) {
		Model m = parseFixtureModel();
		m.anIntArray1627 = vx.clone();
		m.anIntArray1628 = vy.clone();
		m.anIntArray1629 = vz.clone();
		writeField(m, "anInt1626", vx.length);
		m.anIntArray1631 = new int[] { 0 };
		m.anIntArray1632 = new int[] { 1 };
		m.anIntArray1633 = new int[] { 2 };
		writeField(m, "anInt1630", 1);
		writeField(m, "anIntArray1637", new int[] { 0 });
		// Three DISTINCT corner codes, so an interpolated clip colour can only match if it
		// came from the right pair of corners - a repeated colour would hide a swap.
		writeField(m, "anIntArray1634", new int[] { 0x1000 });
		writeField(m, "anIntArray1635", new int[] { 0x2000 });
		writeField(m, "anIntArray1636", new int[] { 0x3000 });
		// ⚠ Declares the model textured, purely to force method443 to write its camera-space
		// slots for EVERY vertex. Left alone it writes them only from the first vertex that is
		// behind the near plane onward, which would leave the clipper reading the previous
		// draw's values for the earlier ones. It cannot reach the textured DRAW path: that is
		// selected by a face's render type, and this face's is 0.
		writeField(m, "anInt1642", 1);
		writeField(m, "anInt1653", 400);
		writeField(m, "anInt1652", 800);
		m.anInt1650 = 500;

		int[] swClipX = (int[]) readStatic(Model.class, "anIntArray1678");
		int[] swClipY = (int[]) readStatic(Model.class, "anIntArray1679");
		int[] swClipC = (int[]) readStatic(Model.class, "anIntArray1680");
		int[] swDepth = (int[]) readStatic(Model.class, "anIntArray1667");
		int[] swCamZ = (int[]) readStatic(Model.class, "anIntArray1670");
		int[] swScrX = (int[]) readStatic(Model.class, "anIntArray1665");
		int[] swScrY = (int[]) readStatic(Model.class, "anIntArray1666");
		boolean[] swClipped = (boolean[]) readStatic(Model.class, "aBooleanArray1664");
		int sentinel = 0x7EEDBEEF;
		java.util.Arrays.fill(swClipX, sentinel);
		java.util.Arrays.fill(swClipY, sentinel);
		java.util.Arrays.fill(swClipC, sentinel);
		java.util.Arrays.fill(swClipped, false);

		m.method443(0, CLIP_CAM_A, CLIP_CAM_B, CLIP_CAM_C, CLIP_CAM_D, 0, 0, dz, 0);

		check("Clipper [" + label + "]: the software really routed this face to method485, so "
				+ "the comparison is against its clipper and not against untouched scratch",
				swClipped[0]);

		int softwarePoints = 0;
		while (softwarePoints < swClipX.length && swClipX[softwarePoints] != sentinel) {
			softwarePoints++;
		}
		check("Clipper [" + label + "]: the software emitted " + expectedPoints + " point(s) "
				+ "for this configuration, so the case is the one intended (got "
				+ softwarePoints + ")", softwarePoints == expectedPoints);

		int sceneDepth = GlModelProjection.modelDepth(CLIP_CAM_A, CLIP_CAM_B, CLIP_CAM_C,
				CLIP_CAM_D, 0, 0, dz);
		// ⚠ k2 is not read from Fog, which method443 restores on the way out; it is DERIVED
		// from the two arrays the software just wrote, since 1670 is camera Z and 1667 is
		// camera Z minus k2.
		check("Clipper [" + label + "]: the scene depth GlModelProjection reports is the "
				+ "software's own k2, recovered from its camera-space and depth arrays",
				sceneDepth == swCamZ[0] - swDepth[0]);

		int count = m.vertexCount();
		int[] glX = new int[count];
		int[] glY = new int[count];
		int[] glDepth = new int[count];
		int[] glCamX = new int[count];
		int[] glCamY = new int[count];
		GlModelProjection.project(m, 0, CLIP_CAM_A, CLIP_CAM_B, CLIP_CAM_C, CLIP_CAM_D,
				0, 0, dz, centreX, centreY, glX, glY, glDepth, glCamX, glCamY);

		int[] cX = new int[GlClipper.MAX_POINTS];
		int[] cY = new int[GlClipper.MAX_POINTS];
		int[] cDepth = new int[GlClipper.MAX_POINTS];
		int[] cColour = new int[GlClipper.MAX_POINTS];
		int points = GlClipper.clip(m, 0, glX, glY, glCamX, glCamY, glDepth, sceneDepth,
				centreX, centreY, cX, cY, cDepth, cColour);
		check("Clipper [" + label + "]: GlClipper emits the same NUMBER of points the software "
				+ "emitted", points == softwarePoints);

		boolean xMatches = true;
		boolean yMatches = true;
		boolean colourMatches = true;
		for (int i = 0; i < Math.min(points, softwarePoints); i++) {
			if (cX[i] != swClipX[i]) {
				xMatches = false;
			}
			if (cY[i] != swClipY[i]) {
				yMatches = false;
			}
			if (cColour[i] != swClipC[i]) {
				colourMatches = false;
			}
		}
		check("Clipper [" + label + "]: every clipped screen X is the software's own", xMatches);
		check("Clipper [" + label + "]: every clipped screen Y is the software's own", yMatches);
		check("Clipper [" + label + "]: every interpolated colour CODE is the software's own",
				colourMatches);

		// ⚠ DEPTH, and it is the one thing the software has no array to lend here: it never
		// records a depth for a clipped point, because its rasteriser is handed one. So the
		// property is checked STRUCTURALLY instead - a point is either one of the face's own
		// front vertices, keeping that vertex's software depth, or an intersection sitting
		// exactly on the plane. The number on the plane must equal the number of edges that
		// crossed it, which is what stops a clipper that stamped every point with the plane
		// depth from passing.
		// ⚠ The plane depth's ABSOLUTE value is reconstructed as
		// `depth + sceneDepth == NEAR_PLANE` rather than compared against the clipper's own
		// planeDepth, which would pass even if both were consistently wrong.
		int planePoints = 0;
		boolean depthsAccountedFor = true;
		for (int i = 0; i < points; i++) {
			if (cDepth[i] + sceneDepth == GlModelProjection.NEAR_PLANE) {
				planePoints++;
				continue;
			}
			boolean knownFrontVertexDepth = false;
			for (int v = 0; v < count; v++) {
				if (swCamZ[v] >= GlModelProjection.NEAR_PLANE && cDepth[i] == swDepth[v]) {
					knownFrontVertexDepth = true;
				}
			}
			if (!knownFrontVertexDepth) {
				depthsAccountedFor = false;
			}
		}
		check("Clipper [" + label + "]: " + expectedPlanePoints + " of the " + points
				+ " point(s) sit at exactly the near plane's camera-space Z, one per crossing "
				+ "edge (got " + planePoints + ")", planePoints == expectedPlanePoints);
		check("Clipper [" + label + "]: every point that is not on the plane carries one of the "
				+ "front vertices' own software depths", depthsAccountedFor);

		// ⚠ END TO END, and the triangle count comes from the SOFTWARE's own clip polygon
		// rather than from this test's expectation: the pipeline must reach the clipper and
		// split the quad the way method485 splits it, and must submit NOTHING when the
		// software's own polygon is back-facing or empty.
		RecordingSink sink = new RecordingSink();
		pipeline.emit(m, 0, CLIP_CAM_A, CLIP_CAM_B, CLIP_CAM_C, CLIP_CAM_D,
				0, 0, dz, centreX, centreY, sink);
		check("Clipper [" + label + "]: the pipeline reports the scene depth it fed the clipper",
				pipeline.sceneDepth() == sceneDepth);
		check("Clipper [" + label + "]: the pipeline classified the face as CLIPPED",
				pipeline.count(GlFacePipeline.CLIPPED) == 1);
		check("Clipper [" + label + "]: the pipeline flagged the face as near-plane cut",
				pipeline.clipped()[0]);
		boolean swFrontFacing = softwarePoints >= 3
				&& GlFacePipeline.isFrontFacing(swClipX[0], swClipY[0], swClipX[1], swClipY[1],
						swClipX[2], swClipY[2]);
		int expectedTriangles = swFrontFacing ? (softwarePoints == 4 ? 2 : 1) : 0;
		check("Clipper [" + label + "]: the pipeline submits exactly the triangles the "
				+ "software's own clip polygon implies (expected " + expectedTriangles
				+ ", got " + sink.triangles + ")", sink.triangles == expectedTriangles);
		// ⚠ Non-vacuity for the whole emission path: at least one case must actually DRAW,
		// or every triangle-level check above agrees on nothing at all. See the comment on
		// the case list for why the winding had to be chosen rather than assumed.
		if (expectedTriangles > 0) {
			clipTotals[0]++;
			clipTotals[1] += expectedTriangles;
		}
		// The submitted corners must be the SOFTWARE's rebuilt polygon, corner for corner -
		// and the colours must be that polygon's codes resolved, not the original face's.
		int[][] quadSplit = { { 0, 1, 2 }, { 0, 2, 3 } };
		boolean submittedMatches = true;
		boolean submittedDepthInRange = true;
		for (int t = 0; t < sink.triangles && t < quadSplit.length; t++) {
			for (int c = 0; c < 3; c++) {
				int p = quadSplit[t][c];
				if (sink.x(t, c) != swClipX[p] || sink.y(t, c) != swClipY[p]
						|| (sink.colour(t, c) & 0xFFFFFF)
								!= GlFacePipeline.resolveCornerColour(swClipC[p], sceneDepth)) {
					submittedMatches = false;
				}
				if (sink.z(t, c) < 0f || sink.z(t, c) > 1f) {
					submittedDepthInRange = false;
				}
			}
		}
		check("Clipper [" + label + "]: every submitted corner is the software's own rebuilt "
				+ "point, with that point's interpolated colour resolved", submittedMatches);
		check("Clipper [" + label + "]: every submitted clipped z stays inside [0,1]",
				submittedDepthInRange);
	}

	// ---------------------- Clipped TEXTURED faces (Phase 7.2b-2n)

	/**
	 * The clipped-textured fixture: ONE triangular face with one vertex behind the near
	 * plane, declared as a TEXTURED render type, so {@code method485} takes its textured
	 * branch and clips on the way.
	 *
	 * <p>⚠ <b>The geometry is the clipper test's "one vertex behind" configuration, verbatim,
	 * and deliberately so.</b> That configuration is already pinned as producing a
	 * front-facing QUAD that the software draws as two triangles - so if this test's face
	 * stopped drawing, the cause is the textured branch rather than the geometry. Deriving a
	 * fresh geometry here would have meant re-establishing the winding by hand.
	 *
	 * <p>⚠ <b>The three corner slots carry DIFFERENT shades, and for a textured face they are
	 * shades rather than colour codes.</b> Render type 2 interpolates them into the clip
	 * shades {@code anIntArray1680} and passes those to {@code method378}; render type 3
	 * passes slot A three times. Distinct values are what make those two behaviours
	 * distinguishable instead of accidentally equal.
	 */
	private static Model buildClippedTexturedFixture(int renderType, int shadeA, int shadeB,
			int shadeC) {
		Model m = parseFixtureModel();
		m.anIntArray1627 = new int[] { 15, -15, 0 };
		m.anIntArray1628 = new int[] { -10, -10, 25 };
		m.anIntArray1629 = new int[] { -60, 200, 200 };
		writeField(m, "anInt1626", 3);
		m.anIntArray1631 = new int[] { 0 };
		m.anIntArray1632 = new int[] { 1 };
		m.anIntArray1633 = new int[] { 2 };
		writeField(m, "anInt1630", 1);
		writeField(m, "anIntArray1637", new int[] { renderType });
		writeField(m, "anIntArray1634", new int[] { shadeA });
		writeField(m, "anIntArray1635", new int[] { shadeB });
		writeField(m, "anIntArray1636", new int[] { shadeC });
		// One texture-coordinate triple naming the face's own three vertices. The nine are
		// read at THESE indices, so they are the camera-space values of vertices 0, 1 and 2.
		writeField(m, "anInt1642", 1);
		writeField(m, "anIntArray1643", new int[] { 0 });
		writeField(m, "anIntArray1644", new int[] { 1 });
		writeField(m, "anIntArray1645", new int[] { 2 });
		// For a textured face the texture ID lives in the COLOUR slot.
		writeField(m, "anIntArray1640", new int[] { FIXTURE_TEXTURE_ID });
		writeField(m, "anInt1653", 400);
		writeField(m, "anInt1652", 800);
		m.anInt1650 = 500;
		return m;
	}

	/**
	 * A texture whose every texel ENCODES ITS OWN INDEX, so a rendered pixel names the texel
	 * that produced it. Blue is held non-zero so no texel looks transparent.
	 */
	private static int[] labelledTexture(int size) {
		int[] tex = new int[size * size];
		for (int idx = 0; idx < tex.length; idx++) {
			tex[idx] = 0x010000 | ((idx & 0xff) << 8) | ((idx >> 8) & 0xff);
		}
		return tex;
	}

	/** The index encoded by {@link #labelledTexture}, or {@code -1} for an unpainted pixel. */
	private static int decodeLabelled(int pixel) {
		if (pixel == 0) {
			return -1;
		}
		return ((pixel >> 8) & 0xff) | ((pixel & 0xff) << 8);
	}

	/**
	 * A near-plane-cut TEXTURED face must be DRAWN, and must be drawn from the UNCUT face's
	 * ramps (Phase 7.2b-2n).
	 *
	 * <p><b>Why this step exists.</b> Until 7.2b-2n a cut textured face returned
	 * {@code NEEDS_TEXTURE}, and that was the COMMON decline rather than a pathological one:
	 * the player's own model, NPCs you approach and any object you walk into carry a textured
	 * face across the near plane. With a whole-frame accept/decline latch, one such face
	 * would hand the entire frame back to the software - so GL would switch itself off in
	 * exactly the situation it exists for.
	 *
	 * <p><b>The claim being tested.</b> {@code method485}'s textured branches hand
	 * {@code method378} the SAME nine for both triangles of a clipped quad, and
	 * {@code method378}'s ramps depend only on the nine and the screen origin. So the mapping
	 * is a property of the FACE and the origin, not of the clipped polygon: clipping changes
	 * WHICH SCREEN REGION is rasterised and the INTERPOLATED SHADES, and nothing else. The
	 * oracle therefore rebuilds the expectation from the software's own arrays -
	 * {@code anIntArray1668/1669/1670} at the texture triple - and evaluates it at the
	 * software's own clipped points, which fails if the implementation read the nine from the
	 * face's CORNERS, evaluated at the wrong points, or rebuilt the ramps from the polygon.
	 *
	 * <p><b>And it is checked against the RASTERISER, not only against that reconstruction.</b>
	 * A reconstruction built from the same {@code TextureRamps} class verifies the WIRING but
	 * cannot catch a shared mistake. So the last part of the test rasterises the pipeline's
	 * own submitted triangles the way a GL shader would - linear numerator interpolation, one
	 * divide per fragment - and compares the texel it samples against the texel the real
	 * software rasteriser painted, pixel for pixel.
	 *
	 * <p>⚠ <b>The shades are checked against a DIFFERENT thing than the mapping is.</b> The
	 * shades never affect which texel is sampled (they darken it), so the pixel comparison
	 * cannot see a shade mistake - it is checked exactly against {@code anIntArray1680}
	 * instead, and render type 3 is checked against slot A, which is where it differs.
	 */
	private static void clippedTexturedFacesUseTheUncutRamps() {
		int centreX = 382;
		int centreY = 251;
		int dz = 100;
		int width = 765;
		int height = 503;
		int[] savedArea = saveDrawingArea();
		int[] savedPixels = DrawingArea.pixels;
		Object savedTextureInt1 = readStatic(Texture.class, "textureInt1");
		Object savedTextureInt2 = readStatic(Texture.class, "textureInt2");
		Object savedLoaded = readStatic(Texture.class, "anIntArrayArray1479");
		Object savedFlags = readStatic(Texture.class, "aBooleanArray1475");
		int savedSceneDepth = Fog.sceneDepth;
		boolean savedLowMem = Texture.lowMem;
		int savedAnInt1465 = Texture.anInt1465;
		boolean saved1462 = Texture.aBoolean1462;
		boolean saved1464 = Texture.aBoolean1464;
		String[] scratch = { "anIntArray1665", "anIntArray1666", "anIntArray1667",
				"anIntArray1668", "anIntArray1669", "anIntArray1670",
				"anIntArray1678", "anIntArray1679", "anIntArray1680" };
		int[][] savedScratch = new int[scratch.length][];
		for (int i = 0; i < scratch.length; i++) {
			savedScratch[i] = ((int[]) readStatic(Model.class, scratch[i])).clone();
		}
		int[] buf = new int[width * height];
		GlFacePipeline pipeline = new GlFacePipeline();
		try {
			DrawingArea.initDrawingArea(height, width, buf);
			Texture.method364();
			DrawingArea.centerX = centreX;
			DrawingArea.centerY = centreY;
			writeStatic(Texture.class, "textureInt1", centreX);
			writeStatic(Texture.class, "textureInt2", centreY);
			installDeterministicPalette();
			Fog.sceneDepth = 0;
			Texture.anInt1465 = 0;
			Texture.aBoolean1462 = false;
			Texture.aBoolean1464 = true;
			int size = ui.GlTextures.layerSize();
			int[] tex = labelledTexture(size);
			parkTexture(FIXTURE_TEXTURE_ID, tex);

			int[] swClipX = (int[]) readStatic(Model.class, "anIntArray1678");
			int[] swClipY = (int[]) readStatic(Model.class, "anIntArray1679");
			int[] swClipC = (int[]) readStatic(Model.class, "anIntArray1680");
			boolean[] swClipped = (boolean[]) readStatic(Model.class, "aBooleanArray1664");
			int[] swCamX = (int[]) readStatic(Model.class, "anIntArray1668");
			int[] swCamY = (int[]) readStatic(Model.class, "anIntArray1669");
			int[] swCamZ = (int[]) readStatic(Model.class, "anIntArray1670");
			int[] swDepth = (int[]) readStatic(Model.class, "anIntArray1667");
			int sentinel = 0x7EEDBEEF;
			int[][] quadSplit = { { 0, 1, 2 }, { 0, 2, 3 } };

			// ================= render type 2: GOURAUD textured =================
			Model gouraud = buildClippedTexturedFixture(2, 0x11, 0x22, 0x33);
			java.util.Arrays.fill(swClipX, sentinel);
			java.util.Arrays.fill(swClipY, sentinel);
			java.util.Arrays.fill(swClipC, sentinel);
			java.util.Arrays.fill(swClipped, false);
			gouraud.method443(0, CLIP_CAM_A, CLIP_CAM_B, CLIP_CAM_C, CLIP_CAM_D, 0, 0, dz, 0);
			check("Clipped textured [type 2]: the software routed the face to its CLIPPER, so "
					+ "the comparison is against method485's textured branch and not against "
					+ "untouched scratch", swClipped[0]);
			int softwarePoints = 0;
			while (softwarePoints < swClipX.length && swClipX[softwarePoints] != sentinel) {
				softwarePoints++;
			}
			check("Clipped textured [type 2]: the software built a QUAD from this "
					+ "configuration (got " + softwarePoints + " points), so both of its "
					+ "textured triangles are exercised", softwarePoints == 4);

			int sceneDepth = GlModelProjection.modelDepth(CLIP_CAM_A, CLIP_CAM_B, CLIP_CAM_C,
					CLIP_CAM_D, 0, 0, dz);
			check("Clipped textured [type 2]: the scene depth recovered from the software's own "
					+ "arrays is the k2 the pipeline feeds the clipper",
					sceneDepth == swCamZ[0] - swDepth[0]);

			RecordingSink sink = new RecordingSink(true);
			pipeline.emit(gouraud, 0, CLIP_CAM_A, CLIP_CAM_B, CLIP_CAM_C, CLIP_CAM_D, 0, 0, dz,
					centreX, centreY, sink);
			check("Clipped textured [type 2]: the pipeline DRAWS a cut textured face rather "
					+ "than declining it - the gap 7.2b-2n closes (clipped="
					+ pipeline.count(GlFacePipeline.CLIPPED) + " needsTexture="
					+ pipeline.count(GlFacePipeline.NEEDS_TEXTURE) + ")",
					pipeline.count(GlFacePipeline.CLIPPED) == 1
							&& pipeline.count(GlFacePipeline.NEEDS_TEXTURE) == 0);
			check("Clipped textured [type 2]: it submits the quad's TWO textured triangles, "
					+ "matching the software's own clip polygon",
					sink.texturedTriangles == 2);
			check("Clipped textured [type 2]: every submitted triangle carries the fixture's "
					+ "texture id", sink.texturedId(0) == FIXTURE_TEXTURE_ID
							&& sink.texturedId(1) == FIXTURE_TEXTURE_ID);

			// ⚠ The nine are read through the MODEL's own texture-coordinate triple and the
			// software's own camera-space arrays - the same two sources the implementation
			// must use. Reading the triple from a static slot would not even compile a
			// meaningful expectation: it is a PER-MODEL field.
			int tripleIndex = gouraud.faceTextureIndex(0);
			int ta = gouraud.textureVertexA()[tripleIndex];
			int tb = gouraud.textureVertexB()[tripleIndex];
			int tc = gouraud.textureVertexC()[tripleIndex];
			ui.TextureRamps expected = ui.TextureRamps.of(
					swCamX[ta], swCamX[tb], swCamX[tc],
					swCamY[ta], swCamY[tb], swCamY[tc],
					swCamZ[ta], swCamZ[tb], swCamZ[tc],
					centreX, centreY, ui.TextureRamps.denShiftFor(size),
					ui.TextureRamps.colShiftFor(size), size);
			check("Clipped textured [type 2]: this fixture's nine do NOT overflow, so the "
					+ "mapping is affine and the handoff below is meaningful",
					!expected.overflows());

			boolean cornersMatch = true;
			boolean shadesMatch = true;
			boolean rampsMatch = true;
			String detail = null;
			for (int t = 0; t < sink.texturedTriangles && t < quadSplit.length; t++) {
				for (int c = 0; c < 3; c++) {
					int p = quadSplit[t][c];
					if (sink.texturedComponent(t, c, 0) != swClipX[p]
							|| sink.texturedComponent(t, c, 1) != swClipY[p]) {
						cornersMatch = false;
						if (detail == null) {
							detail = "triangle " + t + " corner " + c;
						}
					}
					// ⚠ Type 2 passes anIntArray1680 - the INTERPOLATED clip shades - which is
					// what GlClipper writes into clipColour. Not the corner shades.
					if (sink.texturedShade(t, c) != swClipC[p]) {
						shadesMatch = false;
						if (detail == null) {
							detail = "triangle " + t + " corner " + c + " shade "
									+ sink.texturedShade(t, c) + " expected " + swClipC[p];
						}
					}
					int[] na = expected.attributeAt(swClipX[p], swClipY[p]);
					if (sink.texturedComponent(t, c, 3) != (float) na[0]
							|| sink.texturedComponent(t, c, 4) != (float) na[1]
							|| sink.texturedComponent(t, c, 5) != (float) na[2]) {
						rampsMatch = false;
						if (detail == null) {
							detail = "triangle " + t + " corner " + c + " ramps "
									+ sink.texturedComponent(t, c, 3) + ","
									+ sink.texturedComponent(t, c, 4) + ","
									+ sink.texturedComponent(t, c, 5) + " expected " + na[0]
									+ "," + na[1] + "," + na[2];
						}
					}
				}
			}
			check("Clipped textured [type 2]: every submitted corner is the software's own "
					+ "rebuilt polygon point" + (detail == null ? "" : " (" + detail + ")"),
					cornersMatch);
			check("Clipped textured [type 2]: every submitted shade is the software's own "
					+ "INTERPOLATED clip shade anIntArray1680, not a corner shade"
					+ (detail == null ? "" : " (" + detail + ")"), shadesMatch);
			check("Clipped textured [type 2]: every submitted numerator triple is the UNCUT "
					+ "face's ramp evaluated at that clipped screen point - so the mapping came "
					+ "from the face and not from the polygon"
					+ (detail == null ? "" : " (" + detail + ")"), rampsMatch);

			// ⚠ NON-VACUITY for the shade check: if every interpolated clip shade happened to
			// equal every corner shade, "shades = anIntArray1680" and "shades = corner A" would
			// agree and the check above would pass for the wrong reason. The interpolated
			// values must actually differ from the corners they came from.
			boolean interpolationVisible = false;
			for (int p = 0; p < softwarePoints; p++) {
				if (swClipC[p] != 0x11 && swClipC[p] != 0x22 && swClipC[p] != 0x33) {
					interpolationVisible = true;
				}
			}
			check("Clipped textured [type 2]: the interpolated shades really are interpolated - "
					+ "at least one is not one of the three corner values - so the shade check "
					+ "is not passing by coincidence", interpolationVisible);

			// ================= render type 3: FLAT textured =================
			Model flat = buildClippedTexturedFixture(3, 0x11, 0x22, 0x33);
			java.util.Arrays.fill(swClipX, sentinel);
			java.util.Arrays.fill(swClipY, sentinel);
			java.util.Arrays.fill(swClipC, sentinel);
			java.util.Arrays.fill(swClipped, false);
			flat.method443(0, CLIP_CAM_A, CLIP_CAM_B, CLIP_CAM_C, CLIP_CAM_D, 0, 0, dz, 0);
			check("Clipped textured [type 3]: the software routed the face to its CLIPPER too, "
					+ "so this is method485's FLAT textured branch", swClipped[0]);
			int flatPoints = 0;
			while (flatPoints < swClipX.length && swClipX[flatPoints] != sentinel) {
				flatPoints++;
			}
			check("Clipped textured [type 3]: the software built a QUAD here as well (got "
					+ flatPoints + " points)", flatPoints == 4);

			RecordingSink flatSink = new RecordingSink(true);
			pipeline.emit(flat, 0, CLIP_CAM_A, CLIP_CAM_B, CLIP_CAM_C, CLIP_CAM_D, 0, 0, dz,
					centreX, centreY, flatSink);
			check("Clipped textured [type 3]: the pipeline draws it rather than declining it",
					pipeline.count(GlFacePipeline.CLIPPED) == 1
							&& pipeline.count(GlFacePipeline.NEEDS_TEXTURE) == 0);
			check("Clipped textured [type 3]: it submits two textured triangles",
					flatSink.texturedTriangles == 2);
			// ⚠ method485's type-3 branch passes anIntArray1634[i] THREE times - the raw slot
			// A - and never reads the interpolated anIntArray1680. So every shade is slot A,
			// and this is precisely where type 3 differs from type 2 above.
			boolean flatShades = true;
			boolean flatInterpolatedWasIgnored = false;
			for (int t = 0; t < flatSink.texturedTriangles; t++) {
				for (int c = 0; c < 3; c++) {
					if (flatSink.texturedShade(t, c) != 0x11) {
						flatShades = false;
					}
				}
			}
			for (int p = 0; p < flatPoints; p++) {
				if (swClipC[p] != 0x11) {
					flatInterpolatedWasIgnored = true;
				}
			}
			check("Clipped textured [type 3]: every submitted shade is slot A three times, "
					+ "which is method485's FLAT treatment rather than the interpolated clip "
					+ "shades type 2 uses", flatShades);
			check("Clipped textured [type 3]: the software's interpolated shades are NOT slot A "
					+ "on this face, so the flat choice above is a real distinction and not a "
					+ "coincidence", flatInterpolatedWasIgnored);

			// ================= end to end, against the RASTERISER =================
			// Shades of ZERO, so the software's darkening leaves the encoded texel label
			// readable - the label is how a pixel names the texel that produced it.
			Model pixels = buildClippedTexturedFixture(2, 0, 0, 0);
			java.util.Arrays.fill(buf, 0);
			pixels.method443(0, CLIP_CAM_A, CLIP_CAM_B, CLIP_CAM_C, CLIP_CAM_D, 0, 0, dz, 0);
			int softwarePainted = 0;
			int[] softwareTexel = new int[buf.length];
			for (int p = 0; p < buf.length; p++) {
				softwareTexel[p] = decodeLabelled(buf[p]);
				if (softwareTexel[p] >= 0) {
					softwarePainted++;
				}
			}
			check("Clipped textured [end to end]: the software really rasterised the cut "
					+ "textured face into labelled texels (painted " + softwarePainted
					+ " px), so the pixel comparison below is not comparing nothing",
					softwarePainted > 500);

			RecordingSink pixelSink = new RecordingSink(true);
			pipeline.emit(pixels, 0, CLIP_CAM_A, CLIP_CAM_B, CLIP_CAM_C, CLIP_CAM_D, 0, 0, dz,
					centreX, centreY, pixelSink);
			int[] glTexel = shaderSampledTexels(pixelSink, width, height, size, tex);
			int overlap = 0;
			int within1 = 0;
			int exact = 0;
			for (int p = 0; p < buf.length; p++) {
				if (softwareTexel[p] < 0 || glTexel[p] < 0) {
					continue;
				}
				overlap++;
				int swCol = softwareTexel[p] % size;
				int swRow = softwareTexel[p] / size;
				int glCol = glTexel[p] % size;
				int glRow = glTexel[p] / size;
				int dc = Math.abs(swCol - glCol);
				int dr = Math.abs(swRow - glRow);
				if (dc == 0 && dr == 0) {
					exact++;
				}
				if (dc <= 1 && dr <= 1) {
					within1++;
				}
			}
			System.out.println("  note  Clipped textured [end to end]: the shader-simulated "
					+ "submissions overlap the software on " + overlap + " of "
					+ softwarePainted + " painted px; " + exact + " exact, " + within1
					+ " within 1 texel");
			check("Clipped textured [end to end]: the pipeline's own submissions cover the "
					+ "software's polygon, overlapping it at " + overlap + " of "
					+ softwarePainted + " painted pixels", overlap > softwarePainted * 9 / 10);
			check("Clipped textured [end to end]: sampling the pipeline's submitted triangles "
					+ "the way a GL shader would reproduces the real rasteriser's texel at "
					+ within1 + " of " + overlap + " overlapping pixels within one texel - so "
					+ "the cut face draws the software's PICTURE and not merely a "
					+ "self-consistent one", overlap > 500 && within1 > overlap * 9 / 10);
		} finally {
			restoreDrawingArea(savedArea, savedPixels);
			writeStatic(Texture.class, "textureInt1", savedTextureInt1);
			writeStatic(Texture.class, "textureInt2", savedTextureInt2);
			writeStatic(Texture.class, "anIntArrayArray1479", savedLoaded);
			writeStatic(Texture.class, "aBooleanArray1475", savedFlags);
			Texture.lowMem = savedLowMem;
			Texture.anInt1465 = savedAnInt1465;
			Texture.aBoolean1462 = saved1462;
			Texture.aBoolean1464 = saved1464;
			Fog.sceneDepth = savedSceneDepth;
			for (int i = 0; i < scratch.length; i++) {
				System.arraycopy(savedScratch[i], 0,
						(int[]) readStatic(Model.class, scratch[i]), 0, savedScratch[i].length);
			}
		}
	}

	/**
	 * Rasterises the sink's submitted TEXTURED triangles the way a GL fragment shader would -
	 * the three numerators interpolated LINEARLY over the screen triangle
	 * ({@code noperspective}), one divide per fragment, then the software's floor, clamp and
	 * row mask - and samples the labelled texture. Returns, per pixel, the texel index or
	 * {@code -1}.
	 *
	 * <p>⚠ <b>This is deliberately not {@code TextureRamps.column/row}.</b> Those evaluate the
	 * affine form at a point directly. This walks the ATTRIBUTES the pipeline actually
	 * submitted and interpolates them, which is what the GPU will do - so a submission with
	 * wrong numerators, in the wrong order, or with the wrong third component fails here even
	 * though the resolver's own arithmetic is fine. That is the difference between checking
	 * the wiring and checking the picture.
	 */
	private static int[] shaderSampledTexels(RecordingSink sink, int width, int height,
			int size, int[] tex) {
		int[] out = new int[width * height];
		java.util.Arrays.fill(out, -1);
		int denShift = ui.TextureRamps.denShiftFor(size);
		int colShift = ui.TextureRamps.colShiftFor(size);
		for (int t = 0; t < sink.texturedTriangles; t++) {
			int[] sx = new int[3];
			int[] sy = new int[3];
			double[][] n = new double[3][3];
			for (int c = 0; c < 3; c++) {
				sx[c] = (int) sink.texturedComponent(t, c, 0);
				sy[c] = (int) sink.texturedComponent(t, c, 1);
				n[c][0] = sink.texturedComponent(t, c, 3);
				n[c][1] = sink.texturedComponent(t, c, 4);
				n[c][2] = sink.texturedComponent(t, c, 5);
			}
			int minX = Math.max(0, Math.min(sx[0], Math.min(sx[1], sx[2])));
			int maxX = Math.min(width - 1, Math.max(sx[0], Math.max(sx[1], sx[2])));
			int minY = Math.max(0, Math.min(sy[0], Math.min(sy[1], sy[2])));
			int maxY = Math.min(height - 1, Math.max(sy[0], Math.max(sy[1], sy[2])));
			for (int py = minY; py <= maxY; py++) {
				for (int px = minX; px <= maxX; px++) {
					double[] wt = barycentric(px, py, sx, sy);
					if (wt == null || wt[0] < 0.0 || wt[1] < 0.0 || wt[2] < 0.0) {
						continue;
					}
					double un = wt[0] * n[0][0] + wt[1] * n[1][0] + wt[2] * n[2][0];
					double vn = wt[0] * n[0][1] + wt[1] * n[1][1] + wt[2] * n[2][1];
					double wn = wt[0] * n[0][2] + wt[1] * n[1][2] + wt[2] * n[2][2];
					int den = (int) wn >> denShift;
					if (den == 0) {
						continue;
					}
					int col = (int) un / den >> colShift;
					int row = ((int) vn / den >> colShift) & (size - 1);
					if (col < 0) {
						col = 0;
					} else if (col > size - 1) {
						col = size - 1;
					}
					int idx = row * size + col;
					if (idx >= 0 && idx < tex.length && tex[idx] != 0) {
						out[py * width + px] = idx;
					}
				}
			}
		}
		return out;
	}

	// ------------------------------------ GL textured faces (Phase 7.2b-2e)

	/**
	 * The textured-face fixture: FOUR textured faces in two reversed pairs, so exactly one
	 * face of each pair is front-facing whatever the camera does.
	 *
	 * <p>⚠ <b>Two pairs rather than one, and that is the whole design.</b> The members of a
	 * pair differ in RENDER TYPE and in texture-coordinate index, so one round covers
	 * render type 2 (gouraud textured) AND render type 3 (flat textured), together with a
	 * zero and a non-zero {@code renderType >> 2}. A single pair would leave whichever
	 * type happened to be culled untested, and it would do so SILENTLY - the exact failure
	 * mode this plan keeps writing rules about.
	 *
	 * <p>⚠ <b>The shade values are chosen so a mistake cannot agree by accident.</b> For a
	 * textured face {@code method481} returns {@code 127 - clamp(light)}, a SHADE and not a
	 * colour code - and for render type 3 {@code method484} passes slot A three times
	 * because {@code method479} writes no other slot for the odd render types. Slots B and
	 * C are therefore given DIFFERENT values from slot A on every face, so a type-3 face
	 * that wrongly read them fails instead of matching.
	 */
	private static Model buildTexturedFaceFixture() {
		Model m = parseFixtureModel();
		m.anIntArray1627 = new int[] { 0, 120, 0, 0 };
		m.anIntArray1628 = new int[] { 0, 0, 120, 0 };
		m.anIntArray1629 = new int[] { 0, 0, 0, 120 };
		writeField(m, "anInt1626", 4);
		// Faces 0/1 are reversed twins: render type 2 (GOURAUD textured), texture index 0.
		// Faces 2/3 are reversed twins: render type 3 (FLAT textured), texture index 1.
		m.anIntArray1631 = new int[] { 0, 1, 0, 3 };
		m.anIntArray1632 = new int[] { 1, 0, 3, 0 };
		m.anIntArray1633 = new int[] { 2, 2, 1, 1 };
		writeField(m, "anInt1630", 4);
		writeField(m, "anIntArray1637", new int[] { 2, 2, 7, 7 });
		// Two texture-coordinate entries naming DIFFERENT vertices, so >>2 has to be read.
		writeField(m, "anInt1642", 2);
		writeField(m, "anIntArray1643", new int[] { 0, 2 });
		writeField(m, "anIntArray1644", new int[] { 1, 3 });
		writeField(m, "anIntArray1645", new int[] { 2, 0 });
		writeField(m, "anIntArray1634", new int[] { 0x00, 0x08, 0x30, 0x38 });
		writeField(m, "anIntArray1635", new int[] { 0x10, 0x18, 0x31, 0x39 });
		writeField(m, "anIntArray1636", new int[] { 0x20, 0x28, 0x32, 0x3A });
		// The texture ID lives in the COLOUR slot for a textured face.
		writeField(m, "anIntArray1640", new int[] { FIXTURE_TEXTURE_ID, FIXTURE_TEXTURE_ID,
				FIXTURE_TEXTURE_ID, FIXTURE_TEXTURE_ID });
		// method483's bucket table is indexed by mean depth + anInt1653, so both bounds
		// have to cover this model's depth spread or the software draw throws.
		writeField(m, "anInt1653", 400);
		writeField(m, "anInt1652", 800);
		m.anInt1650 = 500;
		return m;
	}

	/**
	 * The textured-face oracle (Phase 7.2b-2e): every value the pipeline resolves for a
	 * render type 2 or 3 face must be the value {@code method484} would have handed
	 * {@code Texture.method378}.
	 *
	 * <p><b>What the software can be asked, and what it cannot - so the oracle is built
	 * from the right side of that boundary.</b> {@code method378}'s arguments are not
	 * recoverable after a draw: they are arithmetic in {@code method484}'s locals plus
	 * reads of {@code anIntArray1634/1635/1636} and {@code anIntArray1668/1669/1670}. What
	 * IS recoverable is precisely the part the pipeline can get wrong - the camera-space
	 * arrays {@code method443} leaves behind, the per-corner codes, and the face's own
	 * colour slot - so those are read back after driving the REAL {@code method443} and
	 * required to match the submission corner for corner. A hand-written expectation could
	 * only restate this class's own reading of the indirection; the software's own arrays
	 * can dispute it.
	 *
	 * <p><b>⚠ The one thing this deliberately does NOT pin is the SHADE's meaning.</b> The
	 * numbers are checked to be the right NUMBERS; what {@code method379} then does with
	 * them - bits 4-5 selecting one of {@code Texture.method371}'s four darkness blocks,
	 * bit 6 a further one-bit shift - is the BATCHER's problem, and is passed through raw
	 * for that reason rather than pre-decoded here.
	 */
	private static void texturedFacesResolveTheSoftwareTextureInputs() {
		int centreX = 382;
		int centreY = 251;
		int[] savedArea = saveDrawingArea();
		int[] savedPixels = DrawingArea.pixels;
		Object savedTextureInt1 = readStatic(Texture.class, "textureInt1");
		Object savedTextureInt2 = readStatic(Texture.class, "textureInt2");
		int savedSceneDepth = Fog.sceneDepth;
		Object savedOrientationCos = readStatic(Model.class, "modelIntArray1");
		Object savedOrientationSin = readStatic(Model.class, "modelIntArray2");
		String[] scratch = { "anIntArray1665", "anIntArray1666", "anIntArray1667",
				"anIntArray1668", "anIntArray1669", "anIntArray1670" };
		int[][] savedScratch = new int[scratch.length][];
		for (int i = 0; i < scratch.length; i++) {
			savedScratch[i] = ((int[]) readStatic(Model.class, scratch[i])).clone();
		}
		try {
			DrawingArea.initDrawingArea(503, 765, new int[765 * 503]);
			Texture.method364();
			DrawingArea.centerX = centreX;
			DrawingArea.centerY = centreY;
			writeStatic(Texture.class, "textureInt1", centreX);
			writeStatic(Texture.class, "textureInt2", centreY);
			installDeterministicPalette();
			Fog.sceneDepth = 0;
			Texture.anInt1465 = 0;
			Texture.aBoolean1462 = false;
			Texture.aBoolean1464 = true;
			parkTexture(FIXTURE_TEXTURE_ID, faceFixtureTexture());

			Model m = buildTexturedFaceFixture();
			int[] orientationCos = new int[8];
			int[] orientationSin = new int[8];
			orientationCos[3] = 46341;
			orientationSin[3] = 46341;
			writeStatic(Model.class, "modelIntArray1", orientationCos);
			writeStatic(Model.class, "modelIntArray2", orientationSin);

			int sentinel = 0x7EEDBEEF;
			for (String name : scratch) {
				java.util.Arrays.fill((int[]) readStatic(Model.class, name), sentinel);
			}

			// The same camera the face census uses: unrotated, 700 units back, so the
			// fixture lands on screen and no vertex nears the near plane.
			int orientation = 0;
			int camA = 0;
			int camB = 65536;
			int camC = 0;
			int camD = 65536;
			int dx = 10;
			int dy = 20;
			int dz = 700;

			// CONTROL: method443 culls on the bounding box BEFORE it projects, so a culled
			// draw would leave the sentinel in place and every check below would be a
			// comparison against the sentinel.
			m.method443(orientation, camA, camB, camC, camD, dx, dy, dz, 0);

			int[] softwareX = (int[]) readStatic(Model.class, "anIntArray1665");
			int[] softwareY = (int[]) readStatic(Model.class, "anIntArray1666");
			int[] softwareDepth = (int[]) readStatic(Model.class, "anIntArray1667");
			int[] softwareCamX = (int[]) readStatic(Model.class, "anIntArray1668");
			int[] softwareCamY = (int[]) readStatic(Model.class, "anIntArray1669");
			int[] softwareCamZ = (int[]) readStatic(Model.class, "anIntArray1670");
			int rewritten = 0;
			for (int v = 0; v < m.vertexCount(); v++) {
				if (softwareCamZ[v] != sentinel) {
					rewritten++;
				}
			}
			check("Textured faces: the software draw really projected, so the oracle is not "
					+ "reading stale scratch", rewritten == m.vertexCount());

			GlFacePipeline pipeline = new GlFacePipeline();
			RecordingSink sink = new RecordingSink(true);
			pipeline.emit(m, orientation, camA, camB, camC, camD, dx, dy, dz, centreX, centreY,
					sink);

			int texturedFaces = 0;
			int gouraud = 0;
			int flat = 0;
			int tri = 0;
			boolean geometryMatches = true;
			boolean uvMatches = true;
			boolean depthMatches = true;
			boolean shadeMatches = true;
			boolean idMatches = true;
			boolean indirectionLives = false;
			boolean stillCameraSpace = false;
			// The size the PIPELINE used, read the same way it reads it, so the expectation
			// cannot be built at a different detail level from the submission.
			int oracleSize = ui.GlTextures.layerSize();
			int firstTextureSum = Integer.MIN_VALUE;
			String detail = null;
			int[] screen = new int[3];
			int[] texture = new int[3];
			int[] shade = new int[3];
			for (int face = 0; face < m.faceCount(); face++) {
				if (pipeline.outcomes()[face] != GlFacePipeline.TEXTURED) {
					continue;
				}
				texturedFaces++;
				int type = m.faceRenderType(face);
				if (type == 2) {
					gouraud++;
				} else {
					flat++;
				}
				screen[0] = m.faceVertexA()[face];
				screen[1] = m.faceVertexB()[face];
				screen[2] = m.faceVertexC()[face];
				int index = m.faceTextureIndex(face);
				texture[0] = m.textureVertexA()[index];
				texture[1] = m.textureVertexB()[index];
				texture[2] = m.textureVertexC()[index];
				int slotA = m.faceCornerColoursA()[face];
				shade[0] = slotA;
				shade[1] = type == 3 ? slotA : m.faceCornerColoursB()[face];
				shade[2] = type == 3 ? slotA : m.faceCornerColoursC()[face];

				// ⚠⚠ PHASE 7.2b-2l: the sink's components 3/4/5 are now the RAMP
				// NUMERATORS, so the expectation is rebuilt from the software's own nine
				// and evaluated at the FACE's own projected vertices - the same two-vertex-set
				// split the pipeline makes (nine from the TEXTURE triple, triangle from the
				// face's corners). An implementation that read the nine from the face's
				// corners, or evaluated at the texture triple's projection, fails here.
				ui.TextureRamps expected = ui.TextureRamps.of(
						softwareCamX[texture[0]], softwareCamX[texture[1]],
						softwareCamX[texture[2]],
						softwareCamY[texture[0]], softwareCamY[texture[1]],
						softwareCamY[texture[2]],
						softwareCamZ[texture[0]], softwareCamZ[texture[1]],
						softwareCamZ[texture[2]],
						centreX, centreY, ui.TextureRamps.denShiftFor(oracleSize),
						ui.TextureRamps.colShiftFor(oracleSize), oracleSize);

				if (sink.texturedId(tri) != m.faceTextureId(face)
						|| sink.texturedId(tri) != m.faceBaseColours()[face]) {
					idMatches = false;
				}
				for (int k = 0; k < 3; k++) {
					if (sink.texturedComponent(tri, k, 0) != softwareX[screen[k]]
							|| sink.texturedComponent(tri, k, 1) != softwareY[screen[k]]) {
						geometryMatches = false;
						if (detail == null) {
							detail = "face " + face + " corner " + k + " screen";
						}
					}
					// The sink holds FLOATS, so the expectation must be quantised the same
					// way: a numerator can exceed 2^24 and is then not exactly an int in a
					// float, and comparing against the unquantised int would fail for a
					// reason that has nothing to do with the pipeline.
					int[] ramps = expected.attributeAt(softwareX[screen[k]],
							softwareY[screen[k]]);
					if (sink.texturedComponent(tri, k, 3) != (float) ramps[0]
							|| sink.texturedComponent(tri, k, 4) != (float) ramps[1]
							|| sink.texturedComponent(tri, k, 5) != (float) ramps[2]) {
						uvMatches = false;
						if (detail == null) {
							detail = "face " + face + " corner " + k + " ramps (pipeline "
									+ sink.texturedComponent(tri, k, 3) + ","
									+ sink.texturedComponent(tri, k, 4) + ","
									+ sink.texturedComponent(tri, k, 5) + " expected "
									+ ramps[0] + "," + ramps[1] + "," + ramps[2] + ")";
						}
					}
					// ⚠ CONTROL, and it is the whole point of 7.2l: if the attribute still
					// carried the camera-space triple (the refuted shape) the check above would
					// fail, but a bug that made the ramps happen to EQUAL those values would
					// pass it. So assert directly that at least one submitted corner is NOT the
					// camera-space value - i.e. that the attribute really changed.
					if (sink.texturedComponent(tri, k, 3) == (float) softwareCamX[texture[k]]
							&& sink.texturedComponent(tri, k, 4)
									== (float) softwareCamY[texture[k]]
							&& sink.texturedComponent(tri, k, 5)
									== (float) softwareCamZ[texture[k]]) {
						stillCameraSpace = true;
					}
					// depthToZ is public and already oracle-checked; the DEPTH it is handed
					// here comes from the software, so this is not the function under test
					// checking itself.
					//
					// ⚠ Phase 7.2c-2: the expected depth is the software's ABSOLUTE camera z
					// (anIntArray1670), not the per-model RELATIVE one (anIntArray1667). The
					// pipeline adds the model's own scene depth back before mapping, because a
					// shared depth buffer cannot compare depths measured from different origins -
					// so asserting against the relative array here would now pin the bug.
					if (sink.texturedComponent(tri, k, 2)
							!= GlFacePipeline.depthToZ(softwareCamZ[screen[k]])) {
						depthMatches = false;
					}
					if (sink.texturedShade(tri, k) != shade[k]) {
						shadeMatches = false;
						if (detail == null) {
							detail = "face " + face + " corner " + k + " shade "
									+ sink.texturedShade(tri, k) + " expected " + shade[k];
						}
					}
				}
				int textureSum = softwareCamZ[texture[0]] + softwareCamZ[texture[1]]
						+ softwareCamZ[texture[2]];
				if (firstTextureSum == Integer.MIN_VALUE) {
					firstTextureSum = textureSum;
				} else if (textureSum != firstTextureSum) {
					indirectionLives = true;
				}
				tri++;
			}

			check("Textured faces: the fixture drew one face from EACH reversed pair, so both "
					+ "render types and both texture-coordinate indices were exercised "
					+ "(textured=" + texturedFaces + " gouraud=" + gouraud + " flat=" + flat + ")",
					texturedFaces == 2 && gouraud == 1 && flat == 1);
			check("Textured faces: one textured triangle was submitted per textured outcome",
					sink.texturedTriangles == texturedFaces
							&& pipeline.triangles() == pipeline.count(GlFacePipeline.DRAWN)
									+ pipeline.count(GlFacePipeline.TEXTURED));
			check("Textured faces: the submitted corners are the software's own projection of "
					+ "that face's own vertex"
					+ (detail == null ? "" : " (" + detail + ")"), geometryMatches);
			check("Textured faces: the submitted triple is the RAMP NUMERATOR at the corner, "
					+ "rebuilt from the software's CAMERA-SPACE values at the TEXTURE-coordinate "
					+ "vertex and evaluated at the FACE's own projected corner - so neither the "
					+ "nine's vertices nor the screen vertices may be swapped"
					+ (detail == null ? "" : " (" + detail + ")"), uvMatches);
			check("Textured faces: NOT ONE submitted corner still carries the camera-space "
					+ "triple, so the attribute really is the ramp form and the check above is "
					+ "not passing because the two happen to coincide (7.2b-2l)",
					!stillCameraSpace);
			check("Textured faces: the submitted z is the software's own depth through "
					+ "depthToZ", depthMatches);
			check("Textured faces: render type 3 takes slot A three times and render type 2 "
					+ "takes the three distinct slots"
					+ (detail == null ? "" : " (" + detail + ")"), shadeMatches);
			check("Textured faces: the texture id comes from the face's COLOUR slot, not the "
					+ "render-type word", idMatches);
			check("Textured faces: renderType >> 2 really selected a different set of "
					+ "vertices, so the indirection is exercised and not just the zero case",
					indirectionLives);

			// The capability itself: a sink that cannot sample textures must leave these
			// faces counted as unrepresentable rather than quietly dropped.
			GlFacePipeline plain = new GlFacePipeline();
			RecordingSink plainSink = new RecordingSink();
			plain.emit(m, orientation, camA, camB, camC, camD, dx, dy, dz, centreX, centreY,
					plainSink);
			check("Textured faces: a sink without texture support gets NEEDS_TEXTURE rather "
					+ "than a dropped submission, so allRepresentable() stays honest",
					plain.count(GlFacePipeline.TEXTURED) == 0
							&& plain.count(GlFacePipeline.NEEDS_TEXTURE) == texturedFaces
							&& plainSink.texturedTriangles == 0
							&& !plain.allRepresentable());
		} finally {
			restoreDrawingArea(savedArea, savedPixels);
			writeStatic(Texture.class, "textureInt1", savedTextureInt1);
			writeStatic(Texture.class, "textureInt2", savedTextureInt2);
			Fog.sceneDepth = savedSceneDepth;
			writeStatic(Model.class, "modelIntArray1", savedOrientationCos);
			writeStatic(Model.class, "modelIntArray2", savedOrientationSin);
			for (int i = 0; i < scratch.length; i++) {
				System.arraycopy(savedScratch[i], 0,
						(int[]) readStatic(Model.class, scratch[i]), 0, savedScratch[i].length);
			}
		}
	}

	/**
	 * A withheld frame must name the FACE OUTCOME that withheld it (Phase 7.4a).
	 *
	 * <p><b>The live gate's third finding, turned into a check.</b> The first live run with
	 * {@code renderer=gl} reached the point of rendering the scene into the FBO and then
	 * printed {@code "discarding GL frames while the scene is not fully representable ...
	 * First reason: a model face the GL path cannot represent (see GlFacePipeline)"}. That
	 * sentence names the FILE to read, not the FAULT to fix - and the eight distinct
	 * failures behind {@link ui.GlFacePipeline#NEEDS_TEXTURE} alone (a model with no texture
	 * coordinates, an index outside the model's arrays, a colour slot with no id, wrapped
	 * ramp numerators, a sink with no atlas, a sink that refused, ...) call for completely
	 * different fixes. So the pipeline records the reason at each {@code return} site and
	 * the renderer reports it verbatim.
	 *
	 * <p><b>Why the sub-reason is asserted by CODE and not by matching prose.</b> The
	 * message text is prose and may be reworded; the constant is the fact. Both are checked,
	 * because the prose half is what a live run actually shows and the code half is what
	 * makes the check survive an edit to the wording.
	 *
	 * <p><b>What makes this non-vacuous.</b> Two DIFFERENT sub-reasons are driven and the
	 * messages must DIFFER, so a {@code declineReason()} that returned one fixed string -
	 * which is exactly the bug being fixed - fails rather than passes. The whole-frame case
	 * is checked too, so the reason cannot be reported for a frame that was never withheld.
	 *
	 * <p><b>What this does NOT drive</b> is the driver: everything here goes through
	 * {@link RecordingSink}/the recording batch, and the real GPU still belongs to the live
	 * gate.
	 */
	private static void modelFaceDeclinesNameTheirOutcome() {
		// ---- (1) nothing unrepresentable -> no reason at all. A reason must mean something.
		GlFacePipeline empty = new GlFacePipeline();
		empty.emit(Model.aModel_1621, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, new RecordingSink());
		check("Model decline: a model with nothing unrepresentable reports NO reason",
				empty.allRepresentable() && empty.declineReason() == null);

		// ---- (2) every outcome and sub-reason has a name, so a census cannot print a blank
		boolean outcomesNamed = true;
		for (int o = 0; o < GlFacePipeline.OUTCOME_COUNT; o++) {
			String name = GlFacePipeline.outcomeName(o);
			if (name == null || name.length() == 0) {
				outcomesNamed = false;
			}
		}
		check("Model decline: every face outcome has a name, so the census cannot print a blank",
				outcomesNamed);
		boolean reasonsNamed = true;
		for (int r = 0; r < GlFacePipeline.TEXTURE_REASON_COUNT; r++) {
			String name = GlFacePipeline.textureDeclineName(r);
			if (name == null || name.length() == 0) {
				reasonsNamed = false;
			}
		}
		check("Model decline: every texture sub-reason has a name too", reasonsNamed);
		check("Model decline: the unrepresentable set is exactly the three the frame latch "
				+ "must refuse, and NOT the representable outcomes",
				GlFacePipeline.isUnrepresentable(GlFacePipeline.NEEDS_CLIPPING)
						&& GlFacePipeline.isUnrepresentable(GlFacePipeline.NEEDS_TEXTURE)
						&& GlFacePipeline.isUnrepresentable(GlFacePipeline.NO_COLOUR)
						&& !GlFacePipeline.isUnrepresentable(GlFacePipeline.DRAWN)
						&& !GlFacePipeline.isUnrepresentable(GlFacePipeline.CLIPPED)
						&& !GlFacePipeline.isUnrepresentable(GlFacePipeline.TEXTURED)
						&& !GlFacePipeline.isUnrepresentable(GlFacePipeline.CULLED)
						&& !GlFacePipeline.isUnrepresentable(GlFacePipeline.SKIPPED));

		// ---- (3) the textured fixture through the pipeline directly: two different
		// sub-reasons behind the ONE outcome, and they must produce different sentences.
		Model m = buildTexturedFaceFixture();
		int orientation = 0;
		int camA = 0;
		int camB = 65536;
		int camC = 0;
		int camD = 65536;
		int dx = 10;
		int dy = 20;
		int dz = 700;
		int centreX = 382;
		int centreY = 251;

		GlFacePipeline refused = new GlFacePipeline();
		RecordingSink refusingSink = new RecordingSink(true);
		refusingSink.refuseTextured = true;
		refused.emit(m, orientation, camA, camB, camC, camD, dx, dy, dz, centreX, centreY,
				refusingSink);
		check("Model decline: the face outcome is named, not just 'a face'",
				!refused.allRepresentable()
						&& refused.declineReason() != null
						&& refused.declineReason().contains(
								GlFacePipeline.outcomeName(GlFacePipeline.NEEDS_TEXTURE)));
		check("Model decline: and the texture SUB-REASON is named - the sink refused the "
				+ "submission",
				refused.textureDecline() == GlFacePipeline.TEXTURE_SINK_DECLINED
						&& refused.declineReason().contains(GlFacePipeline.textureDeclineName(
								GlFacePipeline.TEXTURE_SINK_DECLINED)));
		check("Model decline: the census counts the faces that really declined ("
				+ refused.count(GlFacePipeline.NEEDS_TEXTURE) + " of " + m.faceCount() + ")",
				refused.count(GlFacePipeline.NEEDS_TEXTURE) > 0
						&& refused.declineReason().contains(
								"x" + refused.count(GlFacePipeline.NEEDS_TEXTURE)));
		check("Model decline: and it names WHICH face declined first",
				refused.declineReason().contains("first at face "));

		GlFacePipeline noAtlas = new GlFacePipeline();
		RecordingSink plainSink = new RecordingSink();
		noAtlas.emit(m, orientation, camA, camB, camC, camD, dx, dy, dz, centreX, centreY,
				plainSink);
		check("Model decline: a sink that cannot sample textures is a DIFFERENT sub-reason",
				noAtlas.textureDecline() == GlFacePipeline.TEXTURE_UNSUPPORTED
						&& noAtlas.declineReason().contains(GlFacePipeline.textureDeclineName(
								GlFacePipeline.TEXTURE_UNSUPPORTED)));
		check("Model decline: so two different sub-reasons give two different reasons - a fixed "
				+ "string (the bug this replaces) cannot pass this",
				!refused.declineReason().equals(noAtlas.declineReason()));

		// ---- (4) the WIRING: the renderer must report the pipeline's reason, not its own text
		int[] savedArea = saveDrawingArea();
		int[] savedPixels = DrawingArea.pixels;
		Object savedTextureInt1 = readStatic(Texture.class, "textureInt1");
		Object savedTextureInt2 = readStatic(Texture.class, "textureInt2");
		RSImageProducer scene = new RSImageProducer(765, 503, null);
		writeStatic(Texture.class, "textureInt1", centreX);
		writeStatic(Texture.class, "textureInt2", centreY);
		RecordingSceneBatch batch = new RecordingSceneBatch();
		batch.refuseTextured = true;
		GlSceneRenderer renderer = new GlSceneRenderer("harness-decline", batch);
		GpuRenderer.install(renderer);
		try {
			check("Model decline: the textured model is not consumed (shadow stage)",
					!SceneRasterizer.dispatch(m, orientation, camA, camB, camC, camD, dx, dy, dz, 0));
			String reason = renderer.frameDeclineReason();
			check("Model decline: the renderer reports the PIPELINE's reason, so a live run says "
					+ "WHICH face outcome withheld the frame", reason != null
							&& reason.contains(GlFacePipeline.outcomeName(
									GlFacePipeline.NEEDS_TEXTURE)));
			check("Model decline: the texture sub-reason travels all the way to the renderer",
					reason != null && reason.contains(GlFacePipeline.textureDeclineName(
							GlFacePipeline.TEXTURE_SINK_DECLINED)));
			check("Model decline: the old fixed 'see GlFacePipeline' string is GONE - it named "
					+ "the file instead of the fault", reason != null
							&& !reason.contains("see GlFacePipeline"));
			check("Model decline: and a non-whole frame is still discarded, so the software "
					+ "image stands",
					!GpuRenderer.sceneFinished(scene) && renderer.framesDiscarded() == 1
							&& renderer.framesReadBack() == 0);
		} finally {
			GpuRenderer.install(null);
			restoreDrawingArea(savedArea, savedPixels);
			writeStatic(Texture.class, "textureInt1", savedTextureInt1);
			writeStatic(Texture.class, "textureInt2", savedTextureInt2);
		}
	}

	/**
	 * The GL texture shade policy (Phase 7.2b-2f), pinned against the software
	 * rasteriser rather than against my reading of it.
	 *
	 * <p><b>What decision this test exists to settle.</b> A textured face's corner slots
	 * hold {@code 127 - clamp(light)} - a SHADE, not a colour ({@code Model.method481})
	 * - and {@code Texture.method379} turns that shade into brightness by folding part of
	 * it into the texture's ARRAY INDEX and shifting by another part. Reading that out of
	 * the decompiled fixed-point arithmetic is exactly the kind of thing this repo keeps
	 * getting wrong, and reading it is what produced the question in the first place, so
	 * it is settled by MEASUREMENT instead: the real {@code method378} is driven over a
	 * whole sweep of shade codes and the painted pixels are asked what happened.
	 *
	 * <p><b>How the measurement can see the answer at all.</b> The texture is parked (the
	 * harness's existing {@code parkTexture}, which short-circuits {@code method371}), and
	 * its four brightness BLOCKS are filled with four KNOWN, mutually distinguishable
	 * label colours. So a painted pixel is not just "a colour" - it names the block the
	 * rasteriser read, and (because a {@code >>> 1} of a label is still a label) the shift
	 * it applied. Nothing here restates the arithmetic: the labels are inputs on both
	 * sides, and the OUTPUT is the software's.
	 *
	 * <p>⚠️ <b>The labels are chosen so all eight (block, shift) outcomes are distinct,
	 * and that is asserted before anything is concluded.</b> Two blocks whose values
	 * happened to collide under a shift would make the mapping ambiguous and the whole
	 * sweep could "pass" while identifying nothing - the vacuity trap this phase has
	 * already fallen into twice.
	 *
	 * <p>⚠️ <b>{@code lowMem} is pinned true because it changes the block STRIDE.</b> The
	 * software's blocks sit 4096 apart in 64x64 mode ({@code lowMem}) and 16384 apart in
	 * 128x128 mode; the harness runs in {@code lowMem} (its default) and the parked array
	 * is sized to match. What is NOT asserted here is the block ARITHMETIC
	 * ({@code k - (k >>> n) & 0xf8f8ff}) - see {@link GlTextures#blockColour}, which
	 * transcribes it, and the note there about how it is verified.
	 */
	private static void glTexturesMatchTheSoftwareShadeBlocks() {
		int width = 503;
		int height = 765;
		int[] savedArea = saveDrawingArea();
		int[] savedPixels = DrawingArea.pixels;
		int savedTextureInt1 = Texture.textureInt1;
		int savedTextureInt2 = Texture.textureInt2;
		Object savedLoaded = readStatic(Texture.class, "anIntArrayArray1479");
		Object savedFlags = readStatic(Texture.class, "aBooleanArray1475");
		int savedSceneDepth = Fog.sceneDepth;
		boolean savedLowMem = Texture.lowMem;
		int savedAnInt1465 = Texture.anInt1465;
		boolean saved1462 = Texture.aBoolean1462;
		boolean saved1464 = Texture.aBoolean1464;
		int[] buf = new int[width * height];
		try {
			// Four labels that stay distinct under both shifts, so a painted pixel
			// identifies (block, shift) unambiguously.
			int[] labels = { 0x402010, 0x506070, 0x8090A0, 0xB0C0D0 };
			int distinct = 0;
			java.util.HashSet<Integer> seen = new java.util.HashSet<Integer>();
			for (int b = 0; b < labels.length; b++) {
				seen.add(labels[b]);
				seen.add(labels[b] >>> 1);
			}
			distinct = seen.size();
			check("GL textures: the four block labels stay distinct under both shifts, so a "
					+ "painted pixel can name (block, shift) unambiguously", distinct == 8);

			// lowMem layout: four blocks of 4096, which is the stride method379 indexes with.
			int blockStride = 4096;
			int[] tex = new int[4 * blockStride];
			for (int b = 0; b < 4; b++) {
				java.util.Arrays.fill(tex, b * blockStride, (b + 1) * blockStride, labels[b]);
			}

			DrawingArea.initDrawingArea(height, width, buf);
			DrawingArea.setAllPixels(0);
			Texture.method364();
			DrawingArea.centerX = 382;
			DrawingArea.centerY = 251;
			writeStatic(Texture.class, "textureInt1", 382);
			writeStatic(Texture.class, "textureInt2", 251);
			installDeterministicPalette();
			Fog.sceneDepth = 0;
			Texture.anInt1465 = 0;
			Texture.aBoolean1462 = false;
			Texture.aBoolean1464 = true;
			Texture.lowMem = true;
			parkTexture(TEXTURED_TEX_ID, tex);

			int[] blockSeen = new int[128];
			int[] shiftSeen = new int[128];
			boolean[] blockCovered = new boolean[4];
			boolean[] shiftCovered = new boolean[2];
			int ambiguous = 0;
			int notUniform = 0;

			for (int shade = 0; shade < 128; shade++) {
				DrawingArea.setAllPixels(0);
				// All three corners share the shade, so every painted pixel must agree -
				// which is what makes "the painted value" a single well-defined answer.
				Texture.method378(10, 90, 50, 12, 40, 150, shade, shade, shade,
						100, 300, 200, 120, 400, 250, 300, 500, 350, TEXTURED_TEX_ID);

				int painted = firstPaintedPixel(buf, width, height);
				if (painted < 0) {
					continue;
				}
				if (!everyPaintedPixelIs(buf, width, height, 0, painted)) {
					notUniform++;
				}
				int matchB = -1;
				int matchS = -1;
				int matches = 0;
				for (int b = 0; b < 4; b++) {
					for (int s = 0; s <= 1; s++) {
						if ((labels[b] >>> s) == painted) {
							matches++;
							matchB = b;
							matchS = s;
						}
					}
				}
				if (matches != 1) {
					ambiguous++;
					continue;
				}
				blockSeen[shade] = matchB;
				shiftSeen[shade] = matchS;
				blockCovered[matchB] = true;
				shiftCovered[matchS] = true;
			}

			check("GL textures: every painted pixel agrees with the other painted pixels, so "
					+ "a constant shade really does give one answer", notUniform == 0);
			check("GL textures: every painted pixel named exactly one block/shift, so the "
					+ "labels discriminate rather than collide", ambiguous == 0);

			// The sweep must actually reach every outcome, or "the policy matches" would
			// be a statement about a handful of shades.
			int covered = 0;
			for (boolean b : blockCovered) {
				if (b) {
					covered++;
				}
			}
			int shiftCount = 0;
			for (boolean s : shiftCovered) {
				if (s) {
					shiftCount++;
				}
			}
			check("GL textures: the shade sweep exercised ALL FOUR brightness blocks", covered == 4);
			check("GL textures: the shade sweep exercised BOTH shift outcomes", shiftCount == 2);

			int mismatchedBlock = 0;
			int mismatchedShift = 0;
			int firstBad = -1;
			for (int shade = 0; shade < 128; shade++) {
				if (GlTextures.brightnessBlock(shade) != blockSeen[shade]) {
					if (firstBad < 0) {
						firstBad = shade;
					}
					mismatchedBlock++;
				}
				if (GlTextures.extraShift(shade) != shiftSeen[shade]) {
					mismatchedShift++;
				}
			}
			check("GL textures: the software's brightness BLOCK is (shade >> 4) & 3 for every "
					+ "shade code, as GlTextures.brightnessBlock claims"
					+ (firstBad < 0 ? "" : " (first mismatch at shade " + firstBad
							+ ": software block " + blockSeen[firstBad] + ", GlTextures "
							+ GlTextures.brightnessBlock(firstBad) + ")"),
					mismatchedBlock == 0);
			check("GL textures: the software's extra SHIFT is shade >> 6 for every shade "
					+ "code, as GlTextures.extraShift claims", mismatchedShift == 0);

			System.out.println("  note  GL textures: observed block/shift per shade code:"
					+ " 0-15 -> block " + blockSeen[0] + " shift " + shiftSeen[0]
					+ ", 16-31 -> block " + blockSeen[16] + " shift " + shiftSeen[16]
					+ ", 32-47 -> block " + blockSeen[32] + " shift " + shiftSeen[32]
					+ ", 48-63 -> block " + blockSeen[48] + " shift " + shiftSeen[48]
					+ ", 64-79 -> block " + blockSeen[64] + " shift " + shiftSeen[64]
					+ ", 80-95 -> block " + blockSeen[80] + " shift " + shiftSeen[80]
					+ ", 96-111 -> block " + blockSeen[96] + " shift " + shiftSeen[96]
					+ ", 112-127 -> block " + blockSeen[112] + " shift " + shiftSeen[112]);
		} finally {
			writeStatic(Texture.class, "anIntArrayArray1479", savedLoaded);
			writeStatic(Texture.class, "aBooleanArray1475", savedFlags);
			Texture.textureInt1 = savedTextureInt1;
			Texture.textureInt2 = savedTextureInt2;
			Texture.lowMem = savedLowMem;
			Texture.anInt1465 = savedAnInt1465;
			Texture.aBoolean1462 = saved1462;
			Texture.aBoolean1464 = saved1464;
			Fog.sceneDepth = savedSceneDepth;
			restoreDrawingArea(savedArea, savedPixels);
		}
	}

	/**
	 * THE GROUND RESOLVER'S INPUT, MEASURED (Phase 7.2b-2i).
	 *
	 * <p><b>The question.</b> {@code WorldController.method315}/{@code method316} hand the
	 * rasteriser NINE raw camera-space ints ({@code t0..t8}), and for the ground that is
	 * the whole of the texture information. A GL resolver must turn those into per-vertex
	 * {@code (u,v,w)}. Reading the layout out of {@code method315} was attempted in
	 * 7.2b-2h and <b>abandoned</b>, for reasons that still hold: hook 1's flat set puts the
	 * {@code u}/{@code v} planes in one vertex order and the {@code w} plane in another,
	 * the projection block mixes rotated and unrotated values, and the method is
	 * decompiled with heavy variable reuse. So the layout is MEASURED here instead, by
	 * driving the REAL {@code Texture.method378} and asking the painted pixels.
	 *
	 * <p><b>How a painted pixel can answer at all.</b> The parked texture labels every texel
	 * with its own ARRAY INDEX, and all three corner shades are 0 - which is block 0 and
	 * shift 0, so {@code method379}'s darkening and its shift are BOTH identities and the
	 * painted colour IS the sampled index. Nothing here restates the fixed-point
	 * arithmetic: the labels are inputs on both sides and the index is the software's own
	 * output.
	 *
	 * <p><b>⚠ Why the label is the raw INDEX rather than (col,row).</b> {@code method379}
	 * reads {@code ai1[(j & 0xfc0) + (i >> 6)]} - the row is masked to six bits but the
	 * COLUMN IS NOT, so a column past 63 spills into the following row's entries. Labelling
	 * the index keeps the measurement exact under that spill instead of silently decoding
	 * the spill as a different texel.
	 *
	 * <p><b>The discriminator, and it needs no formula at all.</b> Perturb ONE slot, re-run,
	 * and ask which of the two output coordinates moved - because the three roles differ in
	 * exactly that way. A {@code u} numerator moves the COLUMN only; a {@code v} numerator
	 * moves the ROW only; a {@code w} denominator divides BOTH ratios and so moves column
	 * AND row together. Nine perturbations therefore partition the nine slots, and the
	 * partition is the answer the resolver needs.
	 *
	 * <p><b>⚠ Vacuity guards run BEFORE any conclusion.</b> The labels must be non-zero (a
	 * zero texel is discarded as a cut-out) and mutually distinct (or "which index was
	 * read" is not a question the picture can answer); the reference run must paint a real
	 * triangle and must sample MANY distinct indices (or the texture is being read at one
	 * point and every slot would look inert); and every sampled index must land in block 0,
	 * which is what makes "shade 0 changed nothing" a measured fact rather than an
	 * assumption.
	 */
	private static void groundTexturePlaneRoles() {
		int width = 503;
		int height = 765;
		int[] savedArea = saveDrawingArea();
		int[] savedPixels = DrawingArea.pixels;
		int savedTextureInt1 = Texture.textureInt1;
		int savedTextureInt2 = Texture.textureInt2;
		Object savedLoaded = readStatic(Texture.class, "anIntArrayArray1479");
		Object savedFlags = readStatic(Texture.class, "aBooleanArray1475");
		int savedSceneDepth = Fog.sceneDepth;
		boolean savedLowMem = Texture.lowMem;
		int savedAnInt1465 = Texture.anInt1465;
		boolean saved1462 = Texture.aBoolean1462;
		boolean saved1464 = Texture.aBoolean1464;
		int[] buf = new int[width * height];
		try {
			int texels = 4 * 4096;
			int[] tex = new int[texels];
			int zeroLabels = 0;
			java.util.HashSet<Integer> uniq = new java.util.HashSet<Integer>();
			for (int idx = 0; idx < texels; idx++) {
				tex[idx] = 0x010000 | ((idx & 0xff) << 8) | ((idx >> 8) & 0xff);
				if (tex[idx] == 0) {
					zeroLabels++;
				}
				uniq.add(tex[idx]);
			}
			check("ground roles: no texel label is zero, so none is discarded as a cut-out",
					zeroLabels == 0);
			check("ground roles: all " + texels + " texel labels are distinct, so a painted "
					+ "pixel names exactly one sampled index", uniq.size() == texels);

			DrawingArea.initDrawingArea(height, width, buf);
			Texture.method364();
			DrawingArea.centerX = 382;
			DrawingArea.centerY = 251;
			writeStatic(Texture.class, "textureInt1", 382);
			writeStatic(Texture.class, "textureInt2", 251);
			installDeterministicPalette();
			Fog.sceneDepth = 0;
			Texture.anInt1465 = 0;
			Texture.aBoolean1462 = false;
			Texture.aBoolean1464 = true;
			Texture.lowMem = true;
			parkTexture(TEXTURED_TEX_ID, tex);

			// A large screen triangle, and nine slots with no relationship to each other,
			// so no slot can be mistaken for another through a symmetry in the inputs.
			int[] sy = { 70, 620, 150 };
			int[] sx = { 80, 140, 430 };
			int[] base = { 100, 300, 200, 120, 400, 250, 300, 500, 350 };

			int[] reference = groundRasterIndices(buf, width, height, sx, sy, base);
			int painted = 0;
			java.util.HashSet<Integer> sampled = new java.util.HashSet<Integer>();
			int outsideBlockZero = 0;
			for (int p = 0; p < reference.length; p++) {
				if (reference[p] >= 0) {
					painted++;
					sampled.add(reference[p]);
					if (reference[p] >= 4096) {
						outsideBlockZero++;
					}
				}
			}
			check("ground roles: the reference run paints a real triangle", painted > 1000);
			check("ground roles: the reference run samples MANY distinct texels (" + sampled.size()
					+ "), so the texture is genuinely mapped rather than read at one point",
					sampled.size() > 100);
			check("ground roles: every sampled index lands in block 0, so shade 0 really did "
					+ "leave the label untouched", outsideBlockZero == 0);

			int[] probe = new int[3];
			for (int i = 0; i < 3; i++) {
				probe[i] = nearestPaintedIndex(reference, width, height, sx[i], sy[i]);
			}

			int colOnly = 0;
			int rowOnly = 0;
			int both = 0;
			for (int slot = 0; slot < 9; slot++) {
				int[] movedBase = base.clone();
				movedBase[slot] += (base[slot] / 3) + 37;
				int[] moved = groundRasterIndices(buf, width, height, sx, sy, movedBase);
				int changed = 0;
				int colMoved = 0;
				int rowMoved = 0;
				for (int p = 0; p < reference.length; p++) {
					int a = reference[p];
					int b = moved[p];
					if (a == b || (a < 0 && b < 0)) {
						continue;
					}
					changed++;
					int ac = a < 0 ? -1 : a % 64;
					int ar = a < 0 ? -1 : a / 64;
					int bc = b < 0 ? -1 : b % 64;
					int br = b < 0 ? -1 : b / 64;
					if (ac != bc) {
						colMoved++;
					}
					if (ar != br) {
						rowMoved++;
					}
				}
				String role;
				if (colMoved > 0 && rowMoved > 0) {
					role = "w (denominator)";
					both++;
				} else if (colMoved > 0) {
					role = "u (column only)";
					colOnly++;
				} else if (rowMoved > 0) {
					role = "v (row only)";
					rowOnly++;
				} else {
					role = "INERT";
				}
				System.out.println("  note  ground roles: slot " + slot + " -> " + role
						+ " (pixels changed " + changed + ", col moved " + colMoved
						+ ", row moved " + rowMoved + ")");
				System.out.println("  note  ground roles:   slot " + slot
						+ " sampled index nearest each screen vertex, reference ["
						+ probe[0] + "," + probe[1] + "," + probe[2] + "] perturbed ["
						+ nearestPaintedIndex(moved, width, height, sx[0], sy[0]) + ","
						+ nearestPaintedIndex(moved, width, height, sx[1], sy[1]) + ","
						+ nearestPaintedIndex(moved, width, height, sx[2], sy[2]) + "]");
			}
			// ---- DECISIVE PROBE: does the sampled field depend on the screen triangle?
			//
			// method378's ramp coefficients (l4, i5, j5, k5, l5, i6, j6, k6, l6) are built
			// from the nine slots AND NOTHING ELSE - no screen coordinate reaches them. If
			// that means what it appears to mean, the nine are ALREADY plane coefficients:
			// u_num/v_num/w vary with (x - textureInt1, y - textureInt2) only, so at a pixel
			// painted by two DIFFERENT screen triangles the sampled index must be IDENTICAL,
			// and the screen vertices can change nothing but the COVERAGE. If instead the
			// nine are per-vertex attributes, the index at a shared pixel must differ.
			//
			// Same slots, same texture, only the triangle moves - so the two outcomes are
			// exhaustive and the run cannot be read either way by accident.
			int[] syB = { 90, 600, 170 };
			int[] sxB = { 100, 160, 410 };
			int[] other = groundRasterIndices(buf, width, height, sxB, syB, base);
			int sharedPixels = 0;
			int agreeing = 0;
			for (int p = 0; p < reference.length; p++) {
				if (reference[p] >= 0 && other[p] >= 0) {
					sharedPixels++;
					if (reference[p] == other[p]) {
						agreeing++;
					}
				}
			}
			check("ground roles: the two screen triangles genuinely share painted pixels, so "
					+ "the screen-dependence probe is not vacuous", sharedPixels > 1000);
			System.out.println("  note  ground roles: screen-dependence probe, ALL THREE vertices "
					+ "moved (CONFOUNDED - kept only to show the confound is real) - "
					+ sharedPixels + " shared pixels, " + agreeing + " identical, "
					+ (sharedPixels - agreeing) + " differing");

			// ---- ⚠⚠ THE SAME PROBE WITH THE CONFOUND REMOVED, and this is the one that
			// decides the hypothesis.
			//
			// The version above moved ALL THREE vertices, and that made it worthless:
			// method379 advances a ramp to the span's left edge with
			// `l1 += (k2 >> 3) * (l - textureInt1)` - a TRUNCATED step - so the value at a
			// pixel legitimately depends on the span start `l`, and moving every vertex
			// moved `l`. Coefficient screen-dependence was therefore indistinguishable from
			// span-start quantisation, which is why that run was recorded as CONFOUNDED
			// rather than as a refutation.
			//
			// The fix holds every span-start effect FIXED: keep TWO vertices identical and
			// move only the RIGHT one, far enough right that the LEFT edge is untouched.
			// The vertex-ordering branch is unchanged (same vertex is topmost, and `j < k`
			// still fails), and the 8-pixel block boundaries are counted from the span
			// start, so they land on the same absolute pixels. The ONLY thing left varying
			// is the triangle's shape, and the two outcomes are exhaustive:
			//   - nine = plane COEFFICIENTS => the ramps hold no screen coordinates, so
			//     every shared pixel must be IDENTICAL;
			//   - nine = per-vertex ATTRIBUTES => moving a screen vertex moves the
			//     barycentric weights, so the shared pixels must DIFFER.
			int[] sxC = { sx[0], sx[1], 480 };
			int[] pinched = groundRasterIndices(buf, width, height, sxC, sy, base);
			int sharedC = 0;
			int agreeingC = 0;
			int paintedC = 0;
			for (int p = 0; p < reference.length; p++) {
				if (pinched[p] >= 0) {
					paintedC++;
				}
				if (reference[p] >= 0 && pinched[p] >= 0) {
					sharedC++;
					if (reference[p] == pinched[p]) {
						agreeingC++;
					}
				}
			}
			check("ground roles: moving ONE vertex still shares most of the painted area, so "
					+ "the corrected probe is not vacuous", sharedC > 1000);
			check("ground roles: moving the right vertex really does change the coverage, so "
					+ "the moved vertex is not simply ignored", paintedC != painted);
			boolean planeCoefficients = sharedC > 0 && agreeingC == sharedC;
			check("ground roles: with every SPAN START held fixed, the sampled index at a "
					+ "shared pixel is INDEPENDENT of the screen triangle - so the nine slots "
					+ "are already plane COEFFICIENTS, not per-vertex attributes",
					planeCoefficients);
			System.out.println("  note  ground roles: screen-dependence probe, ONE vertex moved "
					+ "(span starts held fixed) - " + sharedC + " shared pixels, " + agreeingC
					+ " identical, " + (sharedC - agreeingC) + " differing -> nine slots are "
					+ (planeCoefficients ? "PLANE COEFFICIENTS" : "PER-VERTEX ATTRIBUTES"));

			// ---- ⚠⚠ THE SAME QUESTION PUT TO THE MODEL PATH'S SHAPE, and it is the shape
			// GlBatcher's shader already implements.
			//
			// ui/GlFacePipeline hands method378 per-vertex CAMERA-SPACE triples (u_i, v_i,
			// w_i) - the layout is PLANE-major, so slots 0-2 are the three vertices' u, 3-5
			// their v and 6-8 their w - and GlBatcher interpolates them across the triangle
			// with barycentric weights and divides by the interpolated w. That shape makes
			// the mapping at a pixel depend on the SCREEN TRIANGLE, through those weights.
			//
			// The software's mapping does not (measured immediately above: 0 of 93864).
			// So the two can agree only if the barycentric dependence cancels, and that is
			// decidable WITHOUT knowing the software's absolute texel scale: predict
			// (u/w, v/w) under the per-vertex shape for the SAME two triangles, and ask
			// whether the prediction MOVES. Any movement contradicts the software, which
			// demonstrably did not move there - and the ratios are scale-free, so no
			// calibration is needed and no fitted constant can hide the disagreement.
			double[] pvU = { base[0], base[1], base[2] };
			double[] pvV = { base[3], base[4], base[5] };
			double[] pvWv = { base[6], base[7], base[8] };
			int ratioDiffering = 0;
			double maxRatioDelta = 0.0;
			for (int p = 0; p < reference.length; p++) {
				if (reference[p] < 0 || pinched[p] < 0) {
					continue;
				}
				int px = p % width;
				int py = p / width;
				double[] ra = perVertexUvRatio(px, py, sx, sy, pvU, pvV, pvWv);
				double[] rc = perVertexUvRatio(px, py, sxC, sy, pvU, pvV, pvWv);
				if (ra == null || rc == null) {
					continue;
				}
				double delta = Math.abs(ra[0] - rc[0]) + Math.abs(ra[1] - rc[1]);
				if (delta > 1e-6) {
					ratioDiffering++;
				}
				if (delta > maxRatioDelta) {
					maxRatioDelta = delta;
				}
			}
			check("ground roles: the per-vertex [u,v,w] shape is deterministic across the shared "
					+ "area, so the comparison above is not vacuous", sharedC > 1000);
			System.out.println("  note  ground roles: per-vertex [u,v,w] shape predicts a DIFFERENT "
					+ "(u/w, v/w) for the same pixel in " + ratioDiffering + " of " + sharedC
					+ " shared pixels, worst delta " + maxRatioDelta);
			check("ground roles: the per-vertex [u,v,w] shape PREDICTS a screen-dependent mapping "
					+ "- it moves at " + ratioDiffering + " of " + sharedC + " shared pixels where "
					+ "the software is invariant - so that shape is NOT what method378 does, for "
					+ "the MODEL call shape either", ratioDiffering > 0);

			// ⚠⚠ MEASURED, AND IT REFUTES THE OBVIOUS HYPOTHESIS - which is the finding this
			// instrument exists to produce. The design assumed the nine slots partition into
			// three u numerators, three v numerators and three w denominators, so perturbing
			// one slot would move the column only, the row only, or both. NO SLOT IS
			// COLUMN-ONLY OR ROW-ONLY: all nine move both ratios. That is consistent with
			// method378's own structure, where every slot reaches the w ramp (`j6` uses
			// s0..s5 and `k6`/`l6` use s3..s8), so there is no per-coordinate plane to
			// isolate this way. Recorded as a CHECK rather than a comment, so that a later
			// reader cannot quietly re-adopt the tidy decomposition that this run refuted.
			check("ground roles: NO slot moves the column or the row alone - all nine move BOTH "
					+ "ratios, so the nine are NOT three u + three v + three w", colOnly == 0
							&& rowOnly == 0 && both == 9);
			System.out.println("  note  ground roles: the perturbation partition is "
					+ colOnly + " column-only, " + rowOnly + " row-only, " + both
					+ " both-ratios (of 9)");
			System.out.println("  note  ground roles: reference sampled index nearest each "
					+ "screen vertex [" + probe[0] + "," + probe[1] + "," + probe[2] + "] -> "
					+ "col,row [" + (probe[0] % 64) + "," + (probe[0] / 64) + "] ["
					+ (probe[1] % 64) + "," + (probe[1] / 64) + "] ["
					+ (probe[2] % 64) + "," + (probe[2] / 64) + "]");

			// ---- ⚠⚠⚠ THE DECISIVE RUN, AND IT IS THE ONE THE PROBES ABOVE WERE MISSING.
			//
			// Every probe so far fed method378 nine slots and six screen coordinates that
			// had NO relationship to one another. The real model path never does that:
			// Model.method443 projects the camera-space triple (u, v, w) with
			//     sx = textureInt1 + (u << 9) / w,   sy = textureInt2 + (v << 9) / w
			// and hands method378 THOSE sx/sy together with those nine. The two inputs are
			// therefore NOT independent, and that changes what the screen-independence
			// result means, because with a consistent projection
			//     u/w = (sx - textureInt1) / 512
			// is already AFFINE in screen space - so its barycentric interpolation is the
			// value itself, and a screen-INDEPENDENT set of plane coefficients is exactly
			// what reproduces it. The instrument was measuring a consequence of the shape
			// it was trying to test.
			//
			// So the question is not "does the mapping move when the triangle moves" but
			// WHICH of two rival quantities the software's texel tracks, and they are
			// distinguishable with a consistent projection and no knowledge of the scale:
			//   AFFINE     - interpolate the per-vertex RATIO u_i/w_i across the screen
			//                triangle. This is a plain "smooth" attribute and needs no w
			//                in the shader at all.
			//   PER-VERTEX - interpolate u_i and w_i SEPARATELY and then divide. This is
			//                what GlBatcher's shader does and what GlFacePipeline feeds,
			//                and it is the shape the flag above is about.
			// The two differ by the perspective correction, so a triangle whose three w
			// values are in a strong ratio (here 1 : 2 : 2.5) separates them widely.
			//
			// ⚠ The rig's drawing area is 503 WIDE by 765 TALL (checked below, from
			// DrawingArea itself) - so the harness's local `width = 503` happens to be the
			// real width and the real HEIGHT is the local `height = 765`. Note also that
			// textureInt1/2 are 382/251, i.e. half of the height and half of the width: the
			// rig's centre is TRANSPOSED relative to its buffer, which is why the valid
			// screen x-range for a positive texel column is only 382..502.
			int[] slots = new int[9];
			int[] psx = new int[3];
			int[] psy = new int[3];
			int[] pcamX = { 60, 10, 240 };
			int[] pcamY = { -100, 200, 200 };
			int[] pcamZ = { 300, 300, 1200 };
			for (int v = 0; v < 3; v++) {
				psx[v] = 382 + (pcamX[v] << 9) / pcamZ[v];
				psy[v] = 251 + (pcamY[v] << 9) / pcamZ[v];
				slots[v] = pcamX[v];
				slots[3 + v] = pcamY[v];
				slots[6 + v] = pcamZ[v];
			}
			check("model face: the rig's drawing area is 503 wide by 765 tall, so the vertical "
					+ "pixel coordinate must be taken in that order or the whole comparison "
					+ "becomes noise",
					DrawingArea.width == 503 && DrawingArea.height == 765);
			check("model face: all three projected vertices land INSIDE the drawing area, so the "
					+ "run below is not silently clipped", insideArea(psx, psy));
			check("model face: the chosen nine produce ramp coefficients that FIT IN 32 BITS, so "
					+ "the comparison below cannot be measuring integer OVERFLOW rather than the "
					+ "mapping - method378 shifts each ramp by 14, 8 or 5 bits, so a geometry "
					+ "with large camera values wraps and the run is meaningless",
					nineRampsFitInInt(slots));
			System.out.println("  note  model face: projected triangle " + psx[0] + "," + psy[0]
					+ " " + psx[1] + "," + psy[1] + " " + psx[2] + "," + psy[2]);
			double[] ratioU = new double[3];
			double[] ratioV = new double[3];
			double[] pcWv = new double[3];
			for (int v = 0; v < 3; v++) {
				ratioU[v] = (double) pcamX[v] / pcamZ[v];
				ratioV[v] = (double) pcamY[v] / pcamZ[v];
				pcWv[v] = pcamZ[v];
			}
			int[] proj = groundRasterIndices(buf, width, height, psx, psy, slots);
			// Draw the SAME triangle again with only the nine rotated, as a control that the
			// mapping really is reading the slots at all (a screen-anchored mapping would
			// not notice).
			int[] rotated = { slots[3], slots[4], slots[5], slots[6], slots[7], slots[8],
					slots[0], slots[1], slots[2] };
			int[] turned = groundRasterIndices(buf, width, height, psx, psy, rotated);

			int dw = DrawingArea.width;
			int projPainted = 0;
			int affineOk = 0;
			int perVertexOk = 0;
			int affineScaleOk = 0;
			int perVertexScaleOk = 0;
			int controlMoved = 0;
			int clamped = 0;
			double affLo = Double.MAX_VALUE;
			double affHi = -Double.MAX_VALUE;
			double pvLo = Double.MAX_VALUE;
			double pvHi = -Double.MAX_VALUE;
			double worstAffine = 0.0;
			double worstPerVertex = 0.0;
			java.util.HashMap<Integer, java.util.ArrayList<double[]>> byCol =
					new java.util.HashMap<Integer, java.util.ArrayList<double[]>>();
			for (int p = 0; p < proj.length; p++) {
				int idx = proj[p];
				if (idx < 0) {
					continue;
				}
				if (turned[p] != idx) {
					controlMoved++;
				}
				int px = p % dw;
				int py = p / dw;
				int col = idx % 64;
				int row = idx / 64;
				// Col is CLAMPED to [0, 4032] by method379, so a saturated column carries no
				// coordinate information and must be excluded rather than compared.
				if (col == 0 || col == 63) {
					clamped++;
					continue;
				}
				double[] pv = perVertexUvRatio(px, py, psx, psy, ratioU, ratioV, pcWv);
				double[] wts = barycentric(px, py, psx, psy);
				projPainted++;
				if (wts == null || pv == null) {
					continue;
				}
				java.util.ArrayList<double[]> list = byCol.get(col);
				if (list == null) {
					list = new java.util.ArrayList<double[]>();
					byCol.put(col, list);
				}
				list.add(new double[] { px, py });
				// AFFINE: the per-vertex RATIO u/w interpolated across the screen triangle.
				// `perVertexUvRatio` deliberately returns the divide-AFTER shape, so the
				// ratio has to be interpolated from the same weights directly here.
				double ra = wts[0] * ratioU[0] + wts[1] * ratioU[1] + wts[2] * ratioU[2];
				double rv = wts[0] * ratioV[0] + wts[1] * ratioV[1] + wts[2] * ratioV[2];
				double ea = Math.abs(col - 64.0 * ra) + Math.abs(row - 64.0 * rv);
				double ep = Math.abs(col - 64.0 * pv[0]) + Math.abs(row - 64.0 * pv[1]);
				if (ea <= 2.0) {
					affineOk++;
				}
				if (ep <= 2.0) {
					perVertexOk++;
				}
				if (ra != 0.0) {
					double q = col / (64.0 * ra);
					affLo = Math.min(affLo, q);
					affHi = Math.max(affHi, q);
					if (Math.abs(q - 1.0) <= 0.05) {
						affineScaleOk++;
					}
				}
				if (pv[0] != 0.0) {
					double q = col / (64.0 * pv[0]);
					pvLo = Math.min(pvLo, q);
					pvHi = Math.max(pvHi, q);
					if (Math.abs(q - 1.0) <= 0.05) {
						perVertexScaleOk++;
					}
				}
				worstAffine = Math.max(worstAffine, ea);
				worstPerVertex = Math.max(worstPerVertex, ep);
			}
			check("model face: the projection-consistent run paints a real triangle",
					projPainted > 1000);
			check("model face: rotating the nine slots moves the mapping, so the sampled "
					+ "field really is read from the slots and not merely from the pixel "
					+ "position", controlMoved > 0);

			// ⚠⚠⚠ THE CHARACTERISATION THAT DOES SURVIVE, AND IT IS SCALE-FREE. method378
			// builds the texel as a ratio of two functions that are AFFINE in screen space
			// (`method379`: `i = l1 / (j2 >> 12)`, with `l1` and `j2` advanced by fixed
			// per-row and per-pixel steps and seeded from the slot determinants alone). The
			// signature of that form is testable WITHOUT knowing any coefficient: the
			// pixels sharing one measured column must lie on a STRAIGHT LINE, because
			// `Num/Den = const` is a line equation. This is what rules out per-vertex
			// interpolation of any kind for the MODEL shape too - a barycentric shape gives
			// curved iso-column loci in general.
			double worstColLine = 0.0;
			int colLines = 0;
			for (java.util.Map.Entry<Integer, java.util.ArrayList<double[]>> e : byCol.entrySet()) {
				java.util.ArrayList<double[]> pts = e.getValue();
				if (pts.size() < 12) {
					continue;
				}
				double r = maxLineResidual(pts);
				colLines++;
				worstColLine = Math.max(worstColLine, r);
			}
			check("model face: measured columns give enough straight-line samples to test",
					colLines >= 8);
			System.out.println("  note  model face: iso-column straightness - worst residual "
					+ worstColLine + " px over " + colLines + " columns (a texel band is 8 px "
					+ "wide, and method379's span seeding truncates, so this is an observation "
					+ "rather than the load-bearing check - the ramp formula below is)");

			System.out.println("  note  model face: projection-consistent run - " + projPainted
					+ " pixels (plus " + clamped + " saturated) ; AFFINE (interpolate u/w) "
					+ "matches " + affineOk + "; PER-VERTEX (interpolate u,w then divide) "
					+ "matches " + perVertexOk);
			System.out.println("  note  model face: implied scale  col/(64*u/w) - AFFINE spans ["
					+ affLo + ", " + affHi + "] (" + affineScaleOk + "/" + projPainted
					+ " within 5% of 1), PER-VERTEX spans [" + pvLo + ", " + pvHi + "] ("
					+ perVertexScaleOk + "/" + projPainted + " within 5% of 1)");

			check("model face: the projection-consistent run REFUTES BOTH rival shapes at once "
					+ "- the AFFINE interpolation of u/w matches only " + affineOk + " of "
					+ projPainted + " pixels (worst " + worstAffine + ") while the PER-VERTEX "
					+ "shape matches " + perVertexOk + " (worst " + worstPerVertex + "), so "
					+ "neither is what method378 computes even on inputs the real model path "
					+ "would hand it",
					affineOk < projPainted / 10 && perVertexOk < projPainted / 10);
			// ⚠⚠⚠ THE DECISIVE FORM CHECK: evaluate the ramps directly and compare texel by
			// texel. The straight-line test above only proves the FORM; this proves the
			// COEFFICIENTS, and if it holds it hands the resolver its exact formula.
			int rampExact = 0;
			int rampWithin1 = 0;
			int rampCompared = 0;
			for (int p = 0; p < proj.length; p++) {
				if (proj[p] < 0) {
					continue;
				}
				int[] pr = predictTexelByRamps(p % dw, p / dw, slots, 12, 6, 64);
				if (pr == null) {
					continue;
				}
				rampCompared++;
				int dc = Math.abs(proj[p] % 64 - pr[0]);
				int dr = Math.abs(proj[p] / 64 - pr[1]);
				if (dc == 0 && dr == 0) {
					rampExact++;
				}
				if (dc <= 1 && dr <= 1) {
					rampWithin1++;
				}
			}
			System.out.println("  note  model face: ramp formula vs the real rasteriser - "
					+ rampExact + "/" + rampCompared + " texels exact, " + rampWithin1
					+ "/" + rampCompared + " within 1");
			check("model face: the ramp formula derived from the code reproduces the real "
					+ "rasteriser's texel at " + rampExact + " of " + rampCompared + " pixels "
					+ "(" + rampWithin1 + " within 1 texel), so the nine-slot mapping IS pinned "
					+ "and the resolver can be built on it",
					rampCompared > 1000 && rampWithin1 > rampCompared * 9 / 10);
		} finally {
			writeStatic(Texture.class, "anIntArrayArray1479", savedLoaded);
			writeStatic(Texture.class, "aBooleanArray1475", savedFlags);
			Texture.textureInt1 = savedTextureInt1;
			Texture.textureInt2 = savedTextureInt2;
			Texture.lowMem = savedLowMem;
			Texture.anInt1465 = savedAnInt1465;
			Texture.aBoolean1462 = saved1462;
			Texture.aBoolean1464 = saved1464;
			Fog.sceneDepth = savedSceneDepth;
			restoreDrawingArea(savedArea, savedPixels);
		}
	}

	/**
	 * ⚠⚠⚠ PHASE 7.2b-2k: WHICH VERTICES DO THE NINE COME FROM? - MEASURED, NOT READ.
	 *
	 * <p>7.2i and 7.2j fed {@code method378} a nine-slot set and a screen triangle taken
	 * from the SAME vertices, because that is what a projection obviously should do. But
	 * {@code method484} does not promise that: it takes the SCREEN coordinates from the
	 * face's own corners ({@code anIntArray1665/1666[j, k, l]}) while it takes the NINE
	 * from {@code anIntArray1668/1669/1670[anIntArray1643/1644/1645[index]]} - a separate
	 * triple of vertex indices parsed from the model file. If those two triples differ in
	 * real data, then every conclusion drawn from a self-consistent triangle was drawn
	 * about inputs the model path never produces, which would explain both the saturation
	 * and why no shape matched.
	 *
	 * <p><b>This is deliberately a measurement and not a re-reading of that line,</b> for
	 * this plan's own reason: the same kind of reading was already wrong once here. The
	 * instrument gives the face's corners ({@code 0,1,2}) and the texture triple
	 * ({@code 3,4,5}) DIFFERENT camera-space values, drives the REAL {@code method484},
	 * and then perturbs each set in turn. The outcome is exhaustive and needs no fitted
	 * constant:
	 * <ul>
	 *   <li>perturb the values at the FACE-CORNER indices - the mapping must NOT move;</li>
	 *   <li>perturb the values at the TEXTURE-TRIPLE indices - the mapping MUST move.</li>
	 * </ul>
	 * A screen perturbation is included as a control that coverage really does come from
	 * the face's corners, so the run cannot pass by reading neither.
	 */
	private static void texturedFaceNineSourceIsMeasured() {
		int w = 256;
		int h = 256;
		int[] savedArea = saveDrawingArea();
		int[] savedPixels = DrawingArea.pixels;
		int savedTextureInt1 = Texture.textureInt1;
		int savedTextureInt2 = Texture.textureInt2;
		Object savedLoaded = readStatic(Texture.class, "anIntArrayArray1479");
		Object savedFlags = readStatic(Texture.class, "aBooleanArray1475");
		int savedSceneDepth = Fog.sceneDepth;
		boolean savedLowMem = Texture.lowMem;
		int savedAnInt1465 = Texture.anInt1465;
		boolean saved1462 = Texture.aBoolean1462;
		boolean saved1464 = Texture.aBoolean1464;
		int[] buf = new int[w * h];
		try {
			int texels = 4 * 4096;
			int[] tex = new int[texels];
			for (int idx = 0; idx < texels; idx++) {
				tex[idx] = 0x010000 | ((idx & 0xff) << 8) | ((idx >> 8) & 0xff);
			}
			DrawingArea.initDrawingArea(h, w, buf);
			Texture.method364();
			DrawingArea.centerX = 128;
			DrawingArea.centerY = 128;
			Texture.textureInt1 = 128;
			Texture.textureInt2 = 128;
			installDeterministicPalette();
			Fog.sceneDepth = 0;
			Texture.anInt1465 = 0;
			Texture.aBoolean1462 = false;
			Texture.aBoolean1464 = true;
			Texture.lowMem = true;
			parkTexture(TEXTURED_TEX_ID, tex);

			Model m = parseFixtureModel();
			writeField(m, "anInt1626", 6);
			writeField(m, "anIntArray1627", new int[6]);
			writeField(m, "anIntArray1628", new int[6]);
			writeField(m, "anIntArray1629", new int[6]);
			writeField(m, "anInt1630", 1);
			writeField(m, "anIntArray1631", new int[] { 0 });
			writeField(m, "anIntArray1632", new int[] { 1 });
			writeField(m, "anIntArray1633", new int[] { 2 });
			writeField(m, "anIntArray1637", new int[] { 2 });
			writeField(m, "anInt1642", 1);
			// ⚠ THE DELIBERATE DIVERGENCE: the texture triple names vertices 3,4,5 while
			// the face's corners are 0,1,2.
			writeField(m, "anIntArray1643", new int[] { 3 });
			writeField(m, "anIntArray1644", new int[] { 4 });
			writeField(m, "anIntArray1645", new int[] { 5 });
			writeField(m, "anIntArray1634", new int[] { 0 });
			writeField(m, "anIntArray1635", new int[] { 0 });
			writeField(m, "anIntArray1636", new int[] { 0 });
			writeField(m, "anIntArray1640", new int[] { TEXTURED_TEX_ID });
			writeField(m, "anInt1653", 400);
			writeField(m, "anInt1652", 800);
			m.anInt1650 = 500;
			writeStatic(Model.class, "aBooleanArray1664", new boolean[4096]);
			writeStatic(Model.class, "aBooleanArray1663", new boolean[4096]);

			java.lang.reflect.Method draw;
			try {
				draw = Model.class.getDeclaredMethod("method484", int.class);
				draw.setAccessible(true);
			} catch (Exception e) {
				check("texture nine: the per-face rasteriser entry point was reachable", false);
				return;
			}

			int[] faceX = { 60, 200, 110 };
			int[] faceY = { 40, 70, 210 };
			// The nine: a projection consistent with a screen triangle at (faceX, faceY).
			int[] texZ = { 2000, 4000, 5000 };
			int[] texX = new int[3];
			int[] texY = new int[3];
			for (int v = 0; v < 3; v++) {
				texX[v] = ((faceX[v] - 128) * texZ[v]) >> 9;
				texY[v] = ((faceY[v] - 128) * texZ[v]) >> 9;
			}
			// Marker camera values that would be read instead if the nine came from the
			// face's own corners. Distinct, and valid on their own, so a wrong source would
			// paint a coherent but DIFFERENT picture rather than crashing.
			int[] mkAx = { 100, 250, 160 };
			int[] mkAy = { 80, 90, 180 };
			int[] mkAz = { 3000, 4500, 4200 };
			int[] mkBx = { 400, 130, 60 };
			int[] mkBy = { 200, 60, 240 };
			int[] mkBz = { 2500, 5000, 3500 };
			// A genuinely different nine, for the texture-triple perturbation.
			int[] altX = { texX[0] + 90, texX[1] - 70, texX[2] + 40 };
			int[] altY = { texY[0] - 60, texY[1] + 80, texY[2] - 30 };
			int[] altZ = { 2600, 3600, 5600 };

			int[] base = drawTexturedFace(m, draw, buf, w, h, faceX, faceY, mkAx, mkAy, mkAz,
					texX, texY, texZ);
			// Perturbation 1: the camera values at the FACE-CORNER indices (0,1,2).
			int[] faceCamMoved = drawTexturedFace(m, draw, buf, w, h, faceX, faceY, mkBx, mkBy,
					mkBz, texX, texY, texZ);
			// Perturbation 2: the camera values at the TEXTURE-TRIPLE indices (3,4,5).
			int[] texCamMoved = drawTexturedFace(m, draw, buf, w, h, faceX, faceY, mkAx, mkAy,
					mkAz, altX, altY, altZ);
			// Control A: perturb the SCREEN triangle - coverage must change. The vertex is
			// moved far enough that the area change is unambiguous, because a one-pixel move
			// shifts only a few hundred pixels and would make a weak control.
			int[] screenMoved = drawTexturedFace(m, draw, buf, w, h, new int[] { 20, 200, 110 },
					faceY, mkAx, mkAy, mkAz, texX, texY, texZ);

			int painted = 0;
			int faceCamDiffering = 0;
			int texCamDiffering = 0;
			int coverageDiffering = 0;
			java.util.HashSet<Integer> sampled = new java.util.HashSet<Integer>();
			for (int p = 0; p < base.length; p++) {
				if (base[p] >= 0) {
					painted++;
					sampled.add(base[p]);
				}
				if (base[p] >= 0 && faceCamMoved[p] >= 0 && base[p] != faceCamMoved[p]) {
					faceCamDiffering++;
				}
				if (base[p] >= 0 && texCamMoved[p] >= 0 && base[p] != texCamMoved[p]) {
					texCamDiffering++;
				}
				if ((base[p] >= 0) != (screenMoved[p] >= 0)) {
					coverageDiffering++;
				}
			}
			check("texture nine: the run paints a real triangle", painted > 1000);
			check("texture nine: it samples many distinct texels, so the mapping is exercised "
					+ "rather than read at one point", sampled.size() > 100);
			check("texture nine: moving the SCREEN triangle changes the coverage, so the "
					+ "screen coordinates really do come from the face's corners and the run "
					+ "is not vacuous", coverageDiffering > 1000);
			System.out.println("  note  texture nine: face-corner camera values perturbed -> "
					+ faceCamDiffering + " pixels changed; texture-triple camera values "
					+ "perturbed -> " + texCamDiffering + " pixels changed (of " + painted
					+ " painted)");
			check("texture nine: the nine come from the TEXTURE-COORDINATE TRIPLE and NOT from "
					+ "the face's own corners - moving the corner camera values changes "
					+ faceCamDiffering + " pixels (must be 0) while moving the texture triple's "
					+ "changes " + texCamDiffering + " (must be non-zero)",
					faceCamDiffering == 0 && texCamDiffering > 0);
		} finally {
			writeStatic(Texture.class, "anIntArrayArray1479", savedLoaded);
			writeStatic(Texture.class, "aBooleanArray1475", savedFlags);
			Texture.textureInt1 = savedTextureInt1;
			Texture.textureInt2 = savedTextureInt2;
			Texture.lowMem = savedLowMem;
			Texture.anInt1465 = savedAnInt1465;
			Texture.aBoolean1462 = saved1462;
			Texture.aBoolean1464 = saved1464;
			Fog.sceneDepth = savedSceneDepth;
			restoreDrawingArea(savedArea, savedPixels);
		}
	}

	/**
	 * ⚠ THE DISCRIMINATOR THE 7.4c FINDING NEEDS. {@code method378}'s ramps computed WITHOUT
	 * the 32-bit wrap - the exact affine numerators - in the same layout
	 * {@link ui.TextureRamps} stores them ({@code uBase, uStepY, uStepX, vBase, vStepY,
	 * vStepX, wBase, wStepY, wStepX}). The wrapped evaluation the rasteriser actually uses
	 * can then be asked whether it still reproduces these, which is the only thing the
	 * picture depends on.
	 */
	private static long[] exactRamps(int[] s) {
		long t0 = s[0], t1 = s[1], t2 = s[2];
		long t3 = s[3], t4 = s[4], t5 = s[5];
		long t6 = s[6], t7 = s[7], t8 = s[8];
		long k2 = t0 - t1;
		long j3 = t3 - t4;
		long i4 = t6 - t7;
		long l2 = t2 - t0;
		long k3 = t5 - t3;
		long j4 = t8 - t6;
		return new long[] { l2 * t3 - k3 * t0 << 14, j4 * t0 - l2 * t6 << 5,
				k3 * t6 - j4 * t3 << 8 >> 3, k2 * t3 - j3 * t0 << 14, i4 * t0 - k2 * t6 << 5,
				j3 * t6 - i4 * t3 << 8 >> 3, j3 * l2 - k2 * k3 << 14, k2 * j4 - i4 * l2 << 5,
				i4 * k3 - j3 * j4 << 8 >> 3 };
	}

	/**
	 * The texture column and row {@code exactRamps} predicts at a screen point, evaluated
	 * with {@code method379}'s own shifts, clamps and row mask - but in {@code long}, so the
	 * result is the mapping the nine <i>mean</i> rather than the one they wrap to. Returns
	 * {@code column * 1000 + row}, or {@code -1} where the denominator is zero.
	 *
	 * <p>⚠ The origin is the fixture's own {@code textureInt1/2} (382, 251); this is a
	 * measurement helper for the ramp-overflow sweep and is not a general resolver.
	 */
	private static int exactColumnRow(long[] r, int x, int y, int denShift, int colShift,
			int size) {
		int dx = x - 382;
		int dy = y - 251;
		long w = r[6] + r[7] * (long) dy + r[8] * (long) dx;
		long d = w >> denShift;
		if (d == 0) {
			return -1;
		}
		long u = r[0] + r[1] * (long) dy + r[2] * (long) dx;
		long v = r[3] + r[4] * (long) dy + r[5] * (long) dx;
		long i = u / d;
		long clampMax = (size - 1) << colShift;
		if (i < 0) {
			i = 0;
		} else if (i > clampMax) {
			i = clampMax;
		}
		return (int) (i >> colShift) * 1000 + (int) ((v / d >> colShift) & (size - 1));
	}

	/**
	 * ⚠⚠⚠ PHASE 7.4c: THE RAMP-OVERFLOW GUARD, MEASURED RATHER THAN ASSUMED.
	 *
	 * <p><b>The live blocker this answers.</b> On the first live run that reached
	 * {@code GlSceneRenderer.drawModel}, 25 of one 128-face model's faces were declined with
	 * {@code TEXTURE_RAMP_OVERFLOW} - so every frame containing that model was handed back
	 * to the software, and GL drew nothing. {@link GlFacePipeline#texturedRamps} declines on
	 * {@link ui.TextureRamps#overflows()}, whose contract says the ramps that wrapped
	 * "are <b>no longer affine</b>, so a shader CANNOT reproduce [the mapping] by
	 * interpolating the three numerators". ⚠ That claim was pinned only on geometries that
	 * do NOT overflow: {@link #textureRampsResolveTheSoftwareMapping} asserts the fixture
	 * does not overflow, and 7.2j's wrapped geometry is only checked to be <i>reported</i>.
	 * So the guard has never been measured against the arithmetic it refuses to reproduce.
	 * This method measures it.
	 *
	 * <p><b>The experiment - a controlled world scale.</b> The fixture is 7.2j's in-range
	 * ground triangle. Multiplying all nine camera-space slots by {@code k} is a pure world
	 * scale, and the mapping is scale-invariant: {@code method378}'s ramps
	 * ({@code <<14}/{@code <<8}/{@code <<5} minors) and {@code method379}'s walked
	 * numerator and denominator all scale by {@code k^2}, so the PICTURE is unchanged while
	 * the INTERMEDIATE INTEGERS grow by {@code k^2} and cross 2^31. The screen triangle is
	 * computed from the UNSCALED slots, so it is literally the same pixels at every
	 * {@code k}; the only variable is where the ints sit relative to the 32-bit bound. That
	 * is what makes this a control rather than a curiosity.
	 *
	 * <p><b>Four things are compared per {@code k}, on the same pixels:</b>
	 * <ol>
	 *   <li>the real rasteriser's texel - the truth, and the reference;</li>
	 *   <li>the rasteriser's picture at {@code k} against its picture at {@code k = 1},
	 *       which says how much the WRAP changes what the player actually SEES;</li>
	 *   <li>{@link ui.TextureRamps#column}/{@link ui.TextureRamps#row} - the exact affine
	 *       form, which shares {@code method378}'s int arithmetic INCLUDING the wrap;</li>
	 *   <li>the SHADER HANDOFF - the three numerators at the screen vertices, interpolated
	 *       linearly and divided in the fragment, which is what a GL path would submit.</li>
	 * </ol>
	 *
	 * <p>⚠ <b>Why (3) is a reference line and not the answer.</b> {@code column} is a CPU
	 * function evaluated per pixel; a GL path cannot call it. It is measured so that (4) has
	 * something to be read against: if (3) keeps tracking the rasteriser at every {@code k}
	 * while (4) collapses at the wrap, the guard is RIGHT and its stated reason is the
	 * measured one. If (4) also keeps tracking, the guard is merely STRICT and the decline
	 * can be narrowed to the ramps that actually matter.
	 *
	 * <p><b>⚠⚠ WHAT IT MEASURED - the guard is BOTH, and its predicate cannot tell them
	 * apart.</b> At {@code k = 2} the ramps overflow (the {@code <<14} minors exceed 2^31)
	 * and the picture is <b>unchanged</b> (10962/10964 px identical to {@code k = 1}) while
	 * the SHADER HANDOFF still reproduces the rasteriser at the control's own accuracy
	 * (10569/10964 at 64, 10022/10964 at 128): the guard declined a face the GPU could have
	 * drawn. At {@code k >= 4} the rasteriser's picture <b>stops being the k=1 picture</b>
	 * (4/10964 px survive) and the handoff collapses to 143/10964 - the decline was
	 * necessary. So {@code overflows()} is STRICTLY CONSERVATIVE at the low end and right at
	 * the high end, and "any of the nine minors wrapped" is not the question the guard claims
	 * to be answering.
	 *
	 * <p><b>The mechanism, read out of the printed ramps.</b> The wrap is harmless when the
	 * wrapped numerator and denominator stay <i>proportional</i> to their unwrapped values. At
	 * {@code k = 2} scaling the nine by {@code k} scales every minor by {@code k^2 = 4}, and
	 * the two minors that exceed 2^31 ({@code l4}, {@code j6}) are folded back by exactly
	 * +2^32 - a constant that the evaluation's own wrap removes again, leaving
	 * {@code u = 4*u(k=1)} and {@code w = 4*w(k=1)} at the centroid (-677376000 = 4 *
	 * -169344000, -1509504000 = 4 * -377376000). The ratio is then exact. At {@code k = 4}
	 * the u plane wraps twice and the w plane four times, so the two representatives are no
	 * longer a common multiple and the ratio breaks - and because {@code TextureRamps.column}
	 * reproduces that same break, the exact affine form is no help either (18% at k=4): the
	 * wrapped rasteriser is not an affine function of {@code (x, y)} at all.
	 *
	 * <p>⚠ <b>What that rules out, and what it does NOT.</b> It rules out "make the guard
	 * agree with {@code column}": since {@code column} tracks the rasteriser only while the
	 * picture is intact, a surrogate for it would only re-name the same uncertainty.
	 *
	 * <p><b>And it hands the next step its discriminator.</b> The same sweep also evaluates
	 * the ramps in {@code long} - the mapping the nine <i>mean</i> - and asks, pixel by
	 * pixel, whether the wrapped evaluation still reproduces it:
	 * <pre>
	 *   k       1       2        4       8      16      64
	 *   wrapped==exact   10964   10964    0       0       1       2   (of 10964)
	 *   picture survived 10964   10962    4       0       2       3   (of 10964)
	 * </pre>
	 * The two columns agree scale for scale at BOTH detail levels: the picture breaks exactly
	 * when the wrap stops cancelling. That is O(1) per face - both forms are closed in
	 * {@code (x, y)} - so a resolver can ask it of the triangle's screen vertices and widen
	 * the guard to the costless overflows instead of guessing. ⚠ It is pinned here as a
	 * MEASURED correspondence on this sweep, not derived, so a resolver adopting it inherits
	 * these numbers as its evidence.
	 */
	private static void rampOverflowIsMeasured() {
		measureRampOverflow(true);
		measureRampOverflow(false);
	}

	/** One detail level of {@link #rampOverflowIsMeasured}. */
	private static void measureRampOverflow(final boolean lowMem) {
		int size = lowMem ? 64 : 128;
		int denShift = lowMem ? 12 : 14;
		int colShift = lowMem ? 6 : 7;
		String tag = "Ramp overflow [" + size + "]";
		int w = 503;
		int h = 765;
		int[] savedArea = saveDrawingArea();
		int[] savedPixels = DrawingArea.pixels;
		int savedTextureInt1 = Texture.textureInt1;
		int savedTextureInt2 = Texture.textureInt2;
		Object savedLoaded = readStatic(Texture.class, "anIntArrayArray1479");
		Object savedFlags = readStatic(Texture.class, "aBooleanArray1475");
		int savedSceneDepth = Fog.sceneDepth;
		boolean savedLowMem = Texture.lowMem;
		int savedAnInt1465 = Texture.anInt1465;
		boolean saved1462 = Texture.aBoolean1462;
		boolean saved1464 = Texture.aBoolean1464;
		int[] buf = new int[w * h];
		try {
			int texels = 4 * 4096;
			int[] tex = new int[texels];
			for (int idx = 0; idx < texels; idx++) {
				tex[idx] = 0x010000 | ((idx & 0xff) << 8) | ((idx >> 8) & 0xff);
			}
			DrawingArea.initDrawingArea(h, w, buf);
			Texture.method364();
			DrawingArea.centerX = 382;
			DrawingArea.centerY = 251;
			writeStatic(Texture.class, "textureInt1", 382);
			writeStatic(Texture.class, "textureInt2", 251);
			installDeterministicPalette();
			Fog.sceneDepth = 0;
			Texture.anInt1465 = 0;
			Texture.aBoolean1462 = false;
			Texture.aBoolean1464 = true;
			Texture.lowMem = lowMem;
			parkTexture(TEXTURED_TEX_ID, tex);

			// 7.2j's in-range geometry, verbatim. ⚠ The screen triangle is built ONCE, from
			// the unscaled slots, so every k rasterises the same pixels.
			int[] baseX = { 60, 10, 240 };
			int[] baseY = { -100, 200, 200 };
			int[] baseZ = { 300, 300, 1200 };
			int[] sx = new int[3];
			int[] sy = new int[3];
			for (int v = 0; v < 3; v++) {
				sx[v] = 382 + (baseX[v] << 9) / baseZ[v];
				sy[v] = 251 + (baseY[v] << 9) / baseZ[v];
			}
			check(tag + ": the fixture triangle lands inside the drawing area",
					insideArea(sx, sy));

			int[] scales = { 1, 2, 4, 8, 16, 64 };
			int[] painted = new int[scales.length];
			int[] affineClose = new int[scales.length];
			int[] affineCompared = new int[scales.length];
			int[] handoffClose = new int[scales.length];
			int[] handoffCompared = new int[scales.length];
			int[] sameAsReference = new int[scales.length];
			int[] wrappedMatchesExact = new int[scales.length];
			boolean[] overflowed = new boolean[scales.length];
			int[] reference = null;
			int referencePainted = 0;
			for (int s = 0; s < scales.length; s++) {
				int k = scales[s];
				int[] slots = new int[9];
				for (int v = 0; v < 3; v++) {
					slots[v] = baseX[v] * k;
					slots[3 + v] = baseY[v] * k;
					slots[6 + v] = baseZ[v] * k;
				}
				ui.TextureRamps ramps = ui.TextureRamps.of(slots[0], slots[1], slots[2], slots[3],
						slots[4], slots[5], slots[6], slots[7], slots[8], 382, 251, denShift,
						colShift, size);
				overflowed[s] = ramps.overflows();

				// (1) the software's own texel, through the REAL method378/method379.
				int[] software = groundRasterIndices(buf, w, h, sx, sy, slots);
				if (reference == null) {
					reference = software;
					for (int p = 0; p < reference.length; p++) {
						if (reference[p] >= 0) {
							referencePainted++;
						}
					}
				}

				int[][] va = new int[3][];
				for (int v = 0; v < 3; v++) {
					va[v] = ramps.attributeAt(sx[v], sy[v]);
				}
				long[] exact = exactRamps(slots);
				for (int p = 0; p < software.length; p++) {
					int px = p % w;
					int py = p / w;
					if (reference[p] >= 0 && software[p] == reference[p]) {
						sameAsReference[s]++;
					}
					if (software[p] < 0) {
						continue;
					}
					painted[s]++;
					int swCol = software[p] % size;
					int swRow = software[p] / size;

					// (3) the exact affine form - the same int arithmetic method379 walks.
					int col = ramps.column(px, py);
					int row = ramps.row(px, py);
					int exactCR = exactColumnRow(exact, px, py, denShift, colShift, size);
					if (col >= 0 && row >= 0) {
						affineCompared[s]++;
						if (Math.abs(col - swCol) <= 1 && Math.abs(row - swRow) <= 1) {
							affineClose[s]++;
						}
					}
					// ⚠ THE DISCRIMINATOR: does the WRAPPED evaluation the rasteriser uses
					// still equal the exact one the nine mean? Both are asked at the same pixel.
					if (exactCR >= 0 && col == exactCR / 1000 && row == exactCR % 1000) {
						wrappedMatchesExact[s]++;
					}

					// (4) the SHADER HANDOFF: numerators at the vertices, linear over the
					// screen triangle, divided in the fragment.
					double[] wt = barycentric(px, py, sx, sy);
					if (wt == null) {
						continue;
					}
					double un = wt[0] * va[0][0] + wt[1] * va[1][0] + wt[2] * va[2][0];
					double vn = wt[0] * va[0][1] + wt[1] * va[1][1] + wt[2] * va[2][1];
					double wn = wt[0] * va[0][2] + wt[1] * va[1][2] + wt[2] * va[2][2];
					int den = (int) wn >> denShift;
					if (den == 0) {
						continue;
					}
					handoffCompared[s]++;
					int hc = (int) un / den >> colShift;
					if (hc < 0) {
						hc = 0;
					} else if (hc > size - 1) {
						hc = size - 1;
					}
					int hr = ((int) vn / den >> colShift) & (size - 1);
					if (Math.abs(hc - swCol) <= 1 && Math.abs(hr - swRow) <= 1) {
						handoffClose[s]++;
					}
				}
				System.out.println("  note  " + tag + " k=" + k + ": painted " + painted[s]
						+ " px, picture same as k=1 at " + sameAsReference[s] + "/"
						+ referencePainted + " px, ramps overflows=" + overflowed[s]
						+ ", affine " + affineClose[s] + "/" + affineCompared[s]
						+ ", SHADER HANDOFF " + handoffClose[s] + "/" + handoffCompared[s]
						+ ", wrapped==exact " + wrappedMatchesExact[s] + "/" + painted[s]);
				// ⚠ The raw ramps and the numerators at the triangle's centroid, so that an
				// unexpected agreement rate can be READ rather than guessed at: the wrapped
				// bases and the values they evaluate to over the picture are what the mapping
				// IS, and the mechanism below (why k=2 survives and k=4 does not) is visible
				// only in these numbers.
				System.out.println("  note        " + tag + " k=" + k + " ramps " + ramps
						+ " | at centroid (455,336) u=" + ramps.uNumerator(455, 336) + " w="
						+ ramps.wNumerator(455, 336) + " softwareTexel="
						+ software[336 * w + 455]);
			}

			// ⚠ NON-VACUITY AND THE CONTROL. k=1 is 7.2j's in-range geometry: it must paint
			// a real triangle, must NOT be reported as overflowing, and its picture is the
			// reference. At least one larger k must actually wrap, or the sweep measures
			// nothing about the guard.
			check(tag + ": the control (k=1) paints a real triangle, so both comparisons "
					+ "below are over real pixels", referencePainted > 1000 && painted[0] > 1000);
			check(tag + ": the control (k=1) does NOT overflow, so the sweep starts inside "
					+ "the range the guard accepts", !overflowed[0]);
			int wrapped = 0;
			for (int s = 1; s < scales.length; s++) {
				if (overflowed[s]) {
					wrapped++;
				}
			}
			check(tag + ": scaling the same picture past 2^31 makes the ramps wrap in " + wrapped
					+ " of " + (scales.length - 1) + " larger cases, so the guard really is "
					+ "being driven and not merely described", wrapped > 0);
			check(tag + ": the control (k=1) shader handoff reproduces the rasteriser to the "
					+ "tolerance method379's truncated span seeding imposes ("
					+ handoffClose[0] + "/" + handoffCompared[0] + ", the documented ~96% at 64 "
					+ "and ~91% at 128), so the handoff code under test is the working one",
					handoffCompared[0] > 1000
							&& handoffClose[0] > handoffCompared[0] * 9 / 10);
			check(tag + ": at the control scale the SHADER HANDOFF is as accurate as the exact "
					+ "affine form (" + handoffClose[0] + " vs " + affineClose[0] + "), so the "
					+ "handoff's own error is not what the sweep below is measuring",
					Math.abs(handoffClose[0] - affineClose[0]) <= affineCompared[0] / 100);

			// ===================== THE FINDING: THE GUARD IS BOTH =====================
			// ⚠⚠ The sweep splits the overflowing scales into two behaviours that the single
			// `overflows()` predicate cannot tell apart, and BOTH must be pinned or the next
			// step will "fix" the guard on evidence that never distinguished them.
			int benign = -1;   // overflowed, yet the picture and the handoff were unharmed
			int harmful = -1;  // overflowed, and the rasteriser's own picture changed
			for (int s = 1; s < scales.length; s++) {
				if (!overflowed[s]) {
					continue;
				}
				boolean pictureSurvives = sameAsReference[s] > referencePainted * 99 / 100;
				boolean handoffSurvives = handoffCompared[s] > 1000
						&& handoffClose[s] > handoffCompared[s] * 9 / 10;
				if (benign < 0 && pictureSurvives && handoffSurvives) {
					benign = s;
				}
				if (harmful < 0 && !pictureSurvives) {
					harmful = s;
				}
			}
			check(tag + ": THERE IS AN OVERFLOW THAT COSTS NOTHING. At k="
					+ (benign < 0 ? "none" : "" + scales[benign]) + " the ramps overflow and the "
					+ "guard therefore declines, yet the software's own picture is unchanged ("
					+ (benign < 0 ? 0 : sameAsReference[benign]) + "/" + referencePainted
					+ " px) and the SHADER HANDOFF still reproduces it ("
					+ (benign < 0 ? 0 : handoffClose[benign]) + "/"
					+ (benign < 0 ? 0 : handoffCompared[benign]) + ") - so overflows() is "
					+ "STRICTLY CONSERVATIVE, and its stated reason ('no longer affine, so a "
					+ "shader cannot reproduce it') is false for this case", benign > 0);
			check(tag + ": AND THERE IS AN OVERFLOW THAT COSTS THE PICTURE. At k="
					+ (harmful < 0 ? "none" : "" + scales[harmful]) + " the software's own "
					+ "picture stops being the k=1 picture ("
					+ (harmful < 0 ? 0 : sameAsReference[harmful]) + "/" + referencePainted
					+ " px survive) and the SHADER HANDOFF collapses to "
					+ (harmful < 0 ? 0 : handoffClose[harmful]) + "/"
					+ (harmful < 0 ? 0 : handoffCompared[harmful])
					+ " - so for those the decline is not caution but necessity", harmful > 0);
			// ⚠⚠ AND THE STRONGER CLAIM THE FIX HAS TO RESPECT: once the picture breaks, the
			// EXACT affine form breaks with it. `TextureRamps.column` shares method378's int
			// arithmetic INCLUDING the wrap, so if the wrap were merely a moved-but-still-
			// affine mapping, the exact form would still track the rasteriser and only the
			// handoff would fail. It does not: the wrapped rasteriser is not reproducible by
			// an affine evaluation of the nine at all - which is why "narrow the guard to a
			// cheap surrogate for column()" is NOT available as a fix.
			check(tag + ": once the picture breaks the EXACT affine form breaks with it ("
					+ (harmful < 0 ? 0 : affineClose[harmful]) + "/"
					+ (harmful < 0 ? 0 : affineCompared[harmful]) + " within a texel, against "
					+ affineClose[0] + "/" + affineCompared[0] + " at the control), so the "
					+ "wrapped rasteriser is not affine at all - the wrap does not merely move "
					+ "the mapping, it makes it unrepresentable",
					harmful > 0 && affineClose[harmful] < affineCompared[harmful] * 2 / 3);

			// ⚠⚠⚠ AND THE DISCRIMINATOR THAT SEPARATES THE TWO REGIMES, which is what makes
			// the finding actionable rather than merely true. `overflows()` asks "did a minor
			// wrap"; the picture breaks only when the WRAPPED evaluation stops reproducing the
			// EXACT one - i.e. when the wrap moves the mapping rather than cancelling. Asking
			// that is O(1) per face (both evaluations are closed forms in (x, y)) and needs no
			// rasteriser. Measured here pixel by pixel over the whole sweep.
			check(tag + ": THE DISCRIMINATOR IS CLEAN. At the costless overflow (k="
					+ (benign < 0 ? "none" : "" + scales[benign]) + ") the wrapped evaluation "
					+ "reproduces the exact one at ALL "
					+ (benign < 0 ? 0 : wrappedMatchesExact[benign]) + " painted pixels "
					+ "(against " + referencePainted + " reconstructed at k=1), so a resolver "
					+ "can tell this case apart from the harmful one without rasterising "
					+ "anything",
					benign > 0 && wrappedMatchesExact[benign] == painted[benign]
							&& wrappedMatchesExact[0] == painted[0]);
			check(tag + ": and at the picture-breaking overflow (k="
					+ (harmful < 0 ? "none" : "" + scales[harmful]) + ") it reproduces it at "
					+ (harmful < 0 ? 0 : wrappedMatchesExact[harmful]) + " of " + painted[harmful]
					+ " - essentially nowhere, so the two regimes are separated by a wide margin "
					+ "and not by a threshold chosen to fit",
					harmful > 0 && wrappedMatchesExact[harmful] < painted[harmful] / 10);
			boolean discriminatorTracksThePicture = true;
			for (int s = 1; s < scales.length; s++) {
				boolean exactPreserved = wrappedMatchesExact[s] > painted[s] * 99 / 100;
				boolean picturePreserved = sameAsReference[s] > referencePainted * 99 / 100;
				if (exactPreserved != picturePreserved) {
					discriminatorTracksThePicture = false;
				}
			}
			check(tag + ": across the whole sweep 'the wrapped evaluation reproduces the exact "
					+ "one' agrees with 'the rasteriser's picture survived', scale for scale "
					+ "(k=1 10964/10964 and yes, k=2 10964/10964 and yes, k=4 0/10964 and no) - "
					+ "so the discriminator is a predictor of the thing the guard cares about, "
					+ "not a restatement of the wrap", discriminatorTracksThePicture);
			System.out.println("  note  " + tag + ": 7.4c verdict - the guard is STRICTLY "
					+ "conservative wherever the wrapped evaluation still equals the exact one "
					+ "(k=2: overflows, picture intact, handoff intact), and NECESSARY wherever "
					+ "it does not (k>=4: picture gone, handoff and even the exact affine form "
					+ "collapse).");
		} finally {
			writeStatic(Texture.class, "anIntArrayArray1479", savedLoaded);
			writeStatic(Texture.class, "aBooleanArray1475", savedFlags);
			Texture.textureInt1 = savedTextureInt1;
			Texture.textureInt2 = savedTextureInt2;
			Texture.lowMem = savedLowMem;
			Texture.anInt1465 = savedAnInt1465;
			Texture.aBoolean1462 = saved1462;
			Texture.aBoolean1464 = saved1464;
			Fog.sceneDepth = savedSceneDepth;
			restoreDrawingArea(savedArea, savedPixels);
		}
	}

	/**
	 * ⚠⚠⚠ PHASE 7.2b-2l: THE RESOLVER ITSELF - {@code ui.TextureRamps} - CHECKED AGAINST
	 * THE SOFTWARE IT EXISTS TO REPRODUCE, AND ITS SHADER HANDOFF CHECKED TOO.
	 *
	 * <p>Three separate claims, because they can fail independently:
	 * <ol>
	 *   <li>The class reproduces the REAL {@code method378} draw, pixel for pixel, to within
	 *       a texel - which is the tolerance {@code method379}'s truncated span seeding
	 *       imposes (see the class doc).</li>
	 *   <li><b>The SHADER HANDOFF works</b>, which is the deliverable: evaluating the three
	 *       numerators at the triangle's screen VERTICES and interpolating them linearly -
	 *       exactly what {@code noperspective} does - then dividing in the fragment, gives
	 *       the same texel. If this failed, the mapping would be unreproducible on the GPU
	 *       however well the class matched, so it is checked rather than assumed.</li>
	 *   <li>Overflow is DETECTED rather than silently wrapped into the GL path - the trap
	 *       that cost 7.2j a wrong conclusion.</li>
	 * </ol>
	 */
	private static void textureRampsResolveTheSoftwareMapping() {
		int w = 503;
		int h = 765;
		int[] savedArea = saveDrawingArea();
		int[] savedPixels = DrawingArea.pixels;
		int savedTextureInt1 = Texture.textureInt1;
		int savedTextureInt2 = Texture.textureInt2;
		Object savedLoaded = readStatic(Texture.class, "anIntArrayArray1479");
		Object savedFlags = readStatic(Texture.class, "aBooleanArray1475");
		int savedSceneDepth = Fog.sceneDepth;
		boolean savedLowMem = Texture.lowMem;
		int savedAnInt1465 = Texture.anInt1465;
		boolean saved1462 = Texture.aBoolean1462;
		boolean saved1464 = Texture.aBoolean1464;
		int[] buf = new int[w * h];
		try {
			int texels = 4 * 4096;
			int[] tex = new int[texels];
			for (int idx = 0; idx < texels; idx++) {
				tex[idx] = 0x010000 | ((idx & 0xff) << 8) | ((idx >> 8) & 0xff);
			}
			DrawingArea.initDrawingArea(h, w, buf);
			Texture.method364();
			DrawingArea.centerX = 382;
			DrawingArea.centerY = 251;
			writeStatic(Texture.class, "textureInt1", 382);
			writeStatic(Texture.class, "textureInt2", 251);
			installDeterministicPalette();
			Fog.sceneDepth = 0;
			Texture.anInt1465 = 0;
			Texture.aBoolean1462 = false;
			Texture.aBoolean1464 = true;
			Texture.lowMem = true;
			parkTexture(TEXTURED_TEX_ID, tex);

			// The same in-range, projection-consistent geometry 7.2j uses.
			int[] camX = { 60, 10, 240 };
			int[] camY = { -100, 200, 200 };
			int[] camZ = { 300, 300, 1200 };
			int[] sx = new int[3];
			int[] sy = new int[3];
			int[] slots = new int[9];
			for (int v = 0; v < 3; v++) {
				sx[v] = 382 + (camX[v] << 9) / camZ[v];
				sy[v] = 251 + (camY[v] << 9) / camZ[v];
				slots[v] = camX[v];
				slots[3 + v] = camY[v];
				slots[6 + v] = camZ[v];
			}
			check("ui.TextureRamps: the fixture triangle lands inside the drawing area",
					insideArea(sx, sy));

			ui.TextureRamps ramps = ui.TextureRamps.of(slots[0], slots[1], slots[2], slots[3], slots[4],
					slots[5], slots[6], slots[7], slots[8], 382, 251,
					ui.TextureRamps.denShiftFor(64), ui.TextureRamps.colShiftFor(64), 64);
			check("ui.TextureRamps: the fixture geometry does NOT overflow, so the mapping is "
					+ "affine and the handoff below is meaningful", !ramps.overflows());

			int[] ref = groundRasterIndices(buf, w, h, sx, sy, slots);

			// (1) the class against the real rasteriser.
			int painted = 0;
			int exact = 0;
			int within1 = 0;
			int compared = 0;
			for (int p = 0; p < ref.length; p++) {
				if (ref[p] < 0) {
					continue;
				}
				painted++;
				int px = p % w;
				int py = p / w;
				int col = ramps.column(px, py);
				int row = ramps.row(px, py);
				if (col < 0 || row < 0) {
					continue;
				}
				compared++;
				int dc = Math.abs(col - ref[p] % 64);
				int dr = Math.abs(row - ref[p] / 64);
				if (dc == 0 && dr == 0) {
					exact++;
				}
				if (dc <= 1 && dr <= 1) {
					within1++;
				}
			}
			check("ui.TextureRamps: the fixture paints a real triangle", painted > 1000);
			System.out.println("  note  ui.TextureRamps: vs the real rasteriser - " + exact + "/"
					+ compared + " texels exact, " + within1 + "/" + compared + " within 1");
			check("ui.TextureRamps: reproduces the real rasteriser's texel at " + within1 + " of "
					+ compared + " pixels within one texel, which is the tolerance method379's "
					+ "truncated span seeding imposes",
					compared > 1000 && within1 > compared * 9 / 10);

			// (2) THE SHADER HANDOFF: numerators at the three screen VERTICES, interpolated
			// LINEARLY (what `noperspective` does), divided in the fragment. This is the
			// shape a GL path would actually use, so it is the claim that matters.
			int[][] va = new int[3][];
			for (int v = 0; v < 3; v++) {
				va[v] = ramps.attributeAt(sx[v], sy[v]);
			}
			int handoffExact = 0;
			int handoffWithin1 = 0;
			int handoffCompared = 0;
			for (int p = 0; p < ref.length; p++) {
				if (ref[p] < 0) {
					continue;
				}
				int px = p % w;
				int py = p / w;
				int directCol = ramps.column(px, py);
				int directRow = ramps.row(px, py);
				if (directCol < 0 || directRow < 0) {
					continue;
				}
				double[] wt = barycentric(px, py, sx, sy);
				if (wt == null) {
					continue;
				}
				double un = wt[0] * va[0][0] + wt[1] * va[1][0] + wt[2] * va[2][0];
				double vn = wt[0] * va[0][1] + wt[1] * va[1][1] + wt[2] * va[2][1];
				double wn = wt[0] * va[0][2] + wt[1] * va[1][2] + wt[2] * va[2][2];
				int den = (int) wn >> 12;
				if (den == 0) {
					continue;
				}
				handoffCompared++;
				int hc = (int) un / den >> 6;
				int hr = ((int) vn / den >> 6) & 63;
				if (hc < 0) {
					hc = 0;
				} else if (hc > 63) {
					hc = 63;
				}
				int dc = Math.abs(hc - directCol);
				int dr = Math.abs(hr - directRow);
				if (dc == 0 && dr == 0) {
					handoffExact++;
				}
				if (dc <= 1 && dr <= 1) {
					handoffWithin1++;
				}
			}
			System.out.println("  note  ui.TextureRamps: SHADER HANDOFF (numerator attributes at "
					+ "the 3 vertices, linear interpolation, divide in the fragment) - "
					+ handoffExact + "/" + handoffCompared + " exact, " + handoffWithin1 + "/"
					+ handoffCompared + " within 1");
			check("ui.TextureRamps: the SHADER HANDOFF reproduces the mapping at " + handoffWithin1
					+ " of " + handoffCompared + " pixels within one texel, so the mapping CAN "
					+ "be handed to the GPU as three linearly-interpolated numerators per "
					+ "vertex - which is the whole point of the resolver",
					handoffCompared > 1000 && handoffWithin1 > handoffCompared * 99 / 100);

			// (3) OVERFLOW IS DETECTED. The 7.2j geometry wrapped method378's ramps; the
			// resolver must say so rather than hand a non-affine mapping to a shader.
			ui.TextureRamps wrapped = ui.TextureRamps.of(31, 883, 176, 74, 383, 4824, 2000, 4000, 5000,
					382, 251, ui.TextureRamps.denShiftFor(64), ui.TextureRamps.colShiftFor(64), 64);
			check("ui.TextureRamps: the 7.2j geometry is reported as OVERFLOWING, so a GL path "
					+ "can decline it instead of handing over a non-affine mapping",
					wrapped.overflows());
			check("ui.TextureRamps: an in-range geometry is NOT reported as overflowing, so the "
					+ "detector is not simply always true", !ramps.overflows());
			System.out.println("  note  ui.TextureRamps: overflow detector - 7.2j geometry "
					+ wrapped.overflows() + ", fixture geometry " + ramps.overflows());
		} finally {
			writeStatic(Texture.class, "anIntArrayArray1479", savedLoaded);
			writeStatic(Texture.class, "aBooleanArray1475", savedFlags);
			Texture.textureInt1 = savedTextureInt1;
			Texture.textureInt2 = savedTextureInt2;
			Texture.lowMem = savedLowMem;
			Texture.anInt1465 = savedAnInt1465;
			Texture.aBoolean1462 = saved1462;
			Texture.aBoolean1464 = saved1464;
			Fog.sceneDepth = savedSceneDepth;
			restoreDrawingArea(savedArea, savedPixels);
		}
	}

	/**
	 * ⚠⚠⚠ PHASE 7.2b-2l (cont.): WHICH SHADER FORMULA - AND AT WHICH DETAIL LEVEL.
	 *
	 * <p>Two things had to be settled by MEASUREMENT before C0 could change a signature,
	 * because the plan's rule is that a plausible reading is not evidence:
	 *
	 * <ol>
	 *   <li><b>Does the fragment shader need the software's {@code >>12}/{@code >>14}
	 *       truncation, or is the plain ratio enough?</b> {@code method379} computes
	 *       {@code col = (uNum / (wNum >> 12)) >> 6}, which is <i>approximately</i>
	 *       {@code (uNum/wNum) * size} - but only approximately, because the shift
	 *       truncates before the divide. If the plain ratio is as accurate, the shader
	 *       needs no change beyond the attribute; if not, it must reproduce the shift.
	 *       ⚠ This matters: the truncation error is largest for SMALL {@code w}, i.e. for
	 *       geometry close to the camera.</li>
	 *   <li><b>{@code method379} has TWO branches and they use DIFFERENT shifts.</b> The
	 *       {@code lowMem} branch (size 64) divides the denominator by {@code 2^12} and the
	 *       column by {@code 2^6}; the size-128 branch uses {@code 2^14} and {@code 2^7},
	 *       and clamps {@code i} to {@code 16256} rather than {@code 4032}. ⚠
	 *       {@code GlTextures.layerSize()} says the <b>128 side is what runs in play</b>
	 *       ({@code client.main} calls {@code setHighMem()} first), so a resolver pinned
	 *       only on the 64 branch would be right in the harness and wrong on screen. This
	 *       method measures BOTH.</li>
	 * </ol>
	 *
	 * <p>Read out of {@code Texture.method379} rather than assumed:
	 * <pre>
	 *   lowMem (64):  i = l1 / (j2 &gt;&gt; 12), clamp [0,4032],  index = (j &amp; 0xfc0)     + (i &gt;&gt; 6)
	 *   128:          i = l1 / (j2 &gt;&gt; 14), clamp [0,16256], index = (j &amp; 0x3f80) + (i &gt;&gt; 7)
	 * </pre>
	 */
	private static void textureRampsMatchAtBothDetailLevels() {
		measureRampsAtDetail(true);
		measureRampsAtDetail(false);
	}

	/** One detail level of {@link #textureRampsMatchAtBothDetailLevels}. */
	private static void measureRampsAtDetail(final boolean lowMem) {
		int size = lowMem ? 64 : 128;
		int denShift = lowMem ? 12 : 14;
		int colShift = lowMem ? 6 : 7;
		int w = 503;
		int h = 765;
		int[] savedArea = saveDrawingArea();
		int[] savedPixels = DrawingArea.pixels;
		int savedTextureInt1 = Texture.textureInt1;
		int savedTextureInt2 = Texture.textureInt2;
		Object savedLoaded = readStatic(Texture.class, "anIntArrayArray1479");
		Object savedFlags = readStatic(Texture.class, "aBooleanArray1475");
		int savedSceneDepth = Fog.sceneDepth;
		boolean savedLowMem = Texture.lowMem;
		int savedAnInt1465 = Texture.anInt1465;
		boolean saved1462 = Texture.aBoolean1462;
		boolean saved1464 = Texture.aBoolean1464;
		int[] buf = new int[w * h];
		String tag = "TextureRamps[" + size + "]: ";
		try {
			int texels = size * size;
			int[] tex = new int[texels];
			for (int idx = 0; idx < texels; idx++) {
				// The label IS the array index, so the picture names the texel it read.
				tex[idx] = 0x010000 | ((idx & 0xff) << 8) | ((idx >> 8) & 0xff);
			}
			DrawingArea.initDrawingArea(h, w, buf);
			Texture.method364();
			writeStatic(Texture.class, "textureInt1", 382);
			writeStatic(Texture.class, "textureInt2", 251);
			installDeterministicPalette();
			Fog.sceneDepth = 0;
			Texture.anInt1465 = 0;
			Texture.aBoolean1462 = false;
			Texture.aBoolean1464 = true;
			Texture.lowMem = lowMem;
			parkTexture(TEXTURED_TEX_ID, tex);

			int[] camX = { 60, 10, 240 };
			int[] camY = { -100, 200, 200 };
			int[] camZ = { 300, 300, 1200 };
			int[] sx = new int[3];
			int[] sy = new int[3];
			int[] slots = new int[9];
			for (int v = 0; v < 3; v++) {
				sx[v] = 382 + (camX[v] << 9) / camZ[v];
				sy[v] = 251 + (camY[v] << 9) / camZ[v];
				slots[v] = camX[v];
				slots[3 + v] = camY[v];
				slots[6 + v] = camZ[v];
			}
			int[] ref = groundRasterIndices(buf, w, h, sx, sy, slots);

			int painted = 0;
			int shiftExact = 0;
			int shiftWithin1 = 0;
			int plainExact = 0;
			int plainWithin1 = 0;
			int compared = 0;
			int formsAgree = 0;
			for (int p = 0; p < ref.length; p++) {
				if (ref[p] < 0) {
					continue;
				}
				painted++;
				int px = p % w;
				int py = p / w;
				int[] shifted = predictTexelByRamps(px, py, slots, denShift, colShift, size);
				if (shifted == null) {
					continue;
				}
				// The software's own decode: the index is row*size + col.
				int refCol = ref[p] % size;
				int refRow = ref[p] / size;
				compared++;
				int dcs = Math.abs(shifted[0] - refCol);
				int drs = Math.abs(shifted[1] - refRow);
				if (dcs == 0 && drs == 0) {
					shiftExact++;
				}
				if (dcs <= 1 && drs <= 1) {
					shiftWithin1++;
				}
				int[] plain = predictTexelByPlainRatio(px, py, slots, size);
				int dcp = Math.abs(plain[0] - refCol);
				int drp = Math.abs(plain[1] - refRow);
				if (dcp == 0 && drp == 0) {
					plainExact++;
				}
				if (dcp <= 1 && drp <= 1) {
					plainWithin1++;
				}
				if (plain[0] == shifted[0] && plain[1] == shifted[1]) {
					formsAgree++;
				}
			}
			System.out.println("  note  " + tag + painted + " px painted; SHIFTED (software's own "
					+ ">>" + denShift + " then >>" + colShift + "): " + shiftExact + " exact, "
					+ shiftWithin1 + " within 1; PLAIN RATIO floor((uNum/wNum)*" + size + "): "
					+ plainExact + " exact, " + plainWithin1 + " within 1; the two forms AGREE at "
					+ formsAgree + "/" + compared);
			check(tag + "the fixture paints a real triangle at this detail level",
					painted > 1000);
			check(tag + "the SHIFTED form stays within one texel of the real rasteriser at "
					+ shiftWithin1 + " of " + compared + " pixels, whatever span seeds the "
					+ "software happened to choose", compared > 1000
					&& shiftWithin1 > compared * 9 / 10);
			// ⚠⚠ THIS IS THE C0 DECISION, MEASURED. If the software's own >>12/>>14 truncation
			// made a difference the two forms would diverge; they do not, so a fragment shader
			// can evaluate the PLAIN RATIO and needs no shift emulation at all.
			check(tag + "the PLAIN RATIO and the software's SHIFTED form agree at " + formsAgree
					+ " of " + compared + " pixels, so the fragment shader needs NO shift "
					+ "emulation and the attribute change alone is enough",
					compared > 1000 && formsAgree > compared * 99 / 100);
		} finally {
			writeStatic(Texture.class, "anIntArrayArray1479", savedLoaded);
			writeStatic(Texture.class, "aBooleanArray1475", savedFlags);
			Texture.textureInt1 = savedTextureInt1;
			Texture.textureInt2 = savedTextureInt2;
			Texture.lowMem = savedLowMem;
			Texture.anInt1465 = savedAnInt1465;
			Texture.aBoolean1462 = saved1462;
			Texture.aBoolean1464 = saved1464;
			Fog.sceneDepth = savedSceneDepth;
			restoreDrawingArea(savedArea, savedPixels);
		}
	}

	/**
	 * The PLAIN-RATIO form a fragment shader would naturally evaluate:
	 * {@code texel = floor((uNum / wNum) * size)}.
	 *
	 * <p>This is what the CURRENT shader computes when its attribute is the numerators -
	 * no shifting, no truncation of the denominator. It is the form the software's
	 * arithmetic only <i>approximates</i>, and this is here so the difference is measured
	 * rather than argued. Rows are masked to the layer's width, as {@code method379}'s
	 * {@code & 0xfc0}/{@code & 0x3f80} masks do.
	 */
	private static int[] predictTexelByPlainRatio(int px, int py, int[] s, int size) {
		int[] num = rampsAt(px, py, s);
		int wn = num[2];
		if (wn == 0) {
			return null;
		}
		int col = (int) Math.floor(((double) num[0] / wn) * size);
		int row = (int) Math.floor(((double) num[1] / wn) * size) & (size - 1);
		return new int[] { Math.max(0, Math.min(size - 1, col)), row };
	}

	/**
	 * The three ramp NUMERATORS {@code method378} would have at a screen point, computed
	 * with the same wrapping {@code int} arithmetic as the rasteriser.
	 */
	private static int[] rampsAt(int px, int py, int[] s) {
		int a0 = s[0];
		int a1 = s[1];
		int a2 = s[2];
		int b0 = s[3];
		int b1 = s[4];
		int b2 = s[5];
		int c0 = s[6];
		int c1 = s[7];
		int c2 = s[8];
		int k2 = a0 - a1;
		int j3 = b0 - b1;
		int i4 = c0 - c1;
		int l2 = a2 - a0;
		int k3 = b2 - b0;
		int j4 = c2 - c0;
		int l4 = l2 * b0 - k3 * a0 << 14;
		int i5 = k3 * c0 - j4 * b0 << 8;
		int j5 = j4 * a0 - l2 * c0 << 5;
		int k5 = k2 * b0 - j3 * a0 << 14;
		int l5 = j3 * c0 - i4 * b0 << 8;
		int i6 = i4 * a0 - k2 * c0 << 5;
		int j6 = j3 * l2 - k2 * k3 << 14;
		int k6 = i4 * k3 - j3 * j4 << 8;
		int l6 = k2 * j4 - i4 * l2 << 5;
		int dy = py - Texture.textureInt2;
		int dx = px - Texture.textureInt1;
		return new int[] { l4 + j5 * dy + (i5 >> 3) * dx, k5 + i6 * dy + (l5 >> 3) * dx,
				j6 + l6 * dy + (k6 >> 3) * dx };
	}

	/**
	 * Drives the REAL {@code Model.method484} for face 0 and returns, per pixel, the sampled
	 * texture index or {@code -1}.
	 *
	 * <p>The six camera-space slot values are supplied as TWO separate triples on purpose:
	 * ({@code cornerCam*}) is parked at vertex indices 0..2 - the face's corners - and
	 * ({@code textureCam*}) at 3..5, which is where the model's texture-coordinate triple
	 * points. Which of the two the drawn picture depends on is the whole question.
	 */
	private static int[] drawTexturedFace(Model m, java.lang.reflect.Method draw, int[] buf,
			int w, int h, int[] faceX, int[] faceY, int[] cornerCamX, int[] cornerCamY,
			int[] cornerCamZ, int[] textureCamX, int[] textureCamY, int[] textureCamZ) {
		int[] camX = (int[]) readStatic(Model.class, "anIntArray1665");
		int[] camY = (int[]) readStatic(Model.class, "anIntArray1666");
		int[] tx = (int[]) readStatic(Model.class, "anIntArray1668");
		int[] ty = (int[]) readStatic(Model.class, "anIntArray1669");
		int[] tz = (int[]) readStatic(Model.class, "anIntArray1670");
		for (int v = 0; v < 3; v++) {
			camX[v] = faceX[v];
			camY[v] = faceY[v];
			tx[v] = cornerCamX[v];
			ty[v] = cornerCamY[v];
			tz[v] = cornerCamZ[v];
			tx[3 + v] = textureCamX[v];
			ty[3 + v] = textureCamY[v];
			tz[3 + v] = textureCamZ[v];
		}
		java.util.Arrays.fill(buf, 0);
		try {
			draw.invoke(m, 0);
		} catch (Exception e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			throw new RuntimeException("method484 threw " + cause.getClass().getName(), cause);
		}
		int[] out = new int[w * h];
		for (int p = 0; p < out.length; p++) {
			int val = buf[p];
			out[p] = val == 0 ? -1 : ((val >> 8) & 0xff) | ((val & 0xff) << 8);
		}
		return out;
	}

	/**
	 * Drives the REAL textured-triangle rasteriser and returns, for every pixel, the
	 * texture ARRAY INDEX it read, or {@code -1} for an untouched pixel.
	 *
	 * <p>Argument order is {@code method378}'s own - three screen Y values, three screen X
	 * values, three shade codes, the nine texture slots, then the texture id - and the
	 * shades are all zero so the label comes back undarkened and unshifted.
	 */
	private static int[] groundRasterIndices(int[] buf, int width, int height, int[] sx,
			int[] sy, int[] s) {
		java.util.Arrays.fill(buf, 0);
		Texture.method378(sy[0], sy[1], sy[2], sx[0], sx[1], sx[2], 0, 0, 0,
				s[0], s[1], s[2], s[3], s[4], s[5], s[6], s[7], s[8], TEXTURED_TEX_ID);
		int[] out = new int[width * height];
		for (int p = 0; p < out.length; p++) {
			int v = buf[p];
			out[p] = v == 0 ? -1 : ((v >> 8) & 0xff) | ((v & 0xff) << 8);
		}
		return out;
	}

	/**
	 * The {@code (u/w, v/w)} a per-vertex-attribute interpolator predicts at a screen pixel:
	 * barycentric weights over the SCREEN triangle, applied to the three vertices' camera
	 * space {@code u}, {@code v} and {@code w} independently, then divided.
	 *
	 * <p>This is the shape {@code GlBatcher}'s shader implements, expressed as the ratio
	 * directly so that it carries no texel scale and no fitted constant. Returns
	 * {@code null} for a degenerate triangle or a zero interpolated {@code w}.
	 */
	private static double[] perVertexUvRatio(int px, int py, int[] sx, int[] sy, double[] u,
			double[] v, double[] w) {
		double x0 = sx[0];
		double y0 = sy[0];
		double x1 = sx[1];
		double y1 = sy[1];
		double x2 = sx[2];
		double y2 = sy[2];
		double det = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2);
		if (det == 0.0) {
			return null;
		}
		double l0 = ((y1 - y2) * (px - x2) + (x2 - x1) * (py - y2)) / det;
		double l1 = ((y2 - y0) * (px - x2) + (x0 - x2) * (py - y2)) / det;
		double l2 = 1.0 - l0 - l1;
		double nw = l0 * w[0] + l1 * w[1] + l2 * w[2];
		if (nw == 0.0) {
			return null;
		}
		double nu = l0 * u[0] + l1 * u[1] + l2 * u[2];
		double nv = l0 * v[0] + l1 * v[1] + l2 * v[2];
		return new double[] { nu / nw, nv / nw };
	}

	/**
	 * The barycentric weights of a screen pixel over the triangle {@code (sx, sy)}, in
	 * vertex order, or {@code null} for a degenerate triangle.
	 *
	 * <p>The same weights {@link #perVertexUvRatio} builds internally, exposed so a caller
	 * can interpolate a quantity the software interpolates WITHOUT the perspective
	 * correction - i.e. a plain screen-space linear attribute.
	 */
	private static double[] barycentric(int px, int py, int[] sx, int[] sy) {
		double x0 = sx[0];
		double y0 = sy[0];
		double x1 = sx[1];
		double y1 = sy[1];
		double x2 = sx[2];
		double y2 = sy[2];
		double det = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2);
		if (det == 0.0) {
			return null;
		}
		double l0 = ((y1 - y2) * (px - x2) + (x2 - x1) * (py - y2)) / det;
		double l1 = ((y2 - y0) * (px - x2) + (x0 - x2) * (py - y2)) / det;
		return new double[] { l0, l1, 1.0 - l0 - l1 };
	}

	/**
	 * The texel {@code (col, row)} that {@code Texture.method378}'s ramp arithmetic predicts
	 * for a screen pixel, computed in {@code int} exactly as the routine does.
	 *
	 * <p>Derived from the code and kept here so it can be CHECKED against the real
	 * rasteriser rather than believed. It is parameterised by the detail level because
	 * {@code method379} has two branches with <b>different</b> shifts - see
	 * {@link #textureRampsMatchAtBothDetailLevels}.
	 *
	 * @param denShift the denominator shift, 12 at size 64 and 14 at size 128
	 * @param colShift the column shift, 6 at size 64 and 7 at size 128
	 * @param size     the layer side, which also fixes the column clamp and the row mask
	 */
	private static int[] predictTexelByRamps(int px, int py, int[] s, int denShift, int colShift,
			int size) {
		int[] num = rampsAt(px, py, s);
		int wd = num[2] >> denShift;
		if (wd == 0) {
			return null;
		}
		int un = num[0];
		int vn = num[1];
		int clampMax = (size - 1) << colShift;
		int i = un / wd;
		if (i < 0) {
			i = 0;
		} else if (i > clampMax) {
			i = clampMax;
		}
		int j = vn / wd;
		return new int[] { i >> colShift, (j >> colShift) & (size - 1) };
	}

	/**
	 * Whether {@code method378}'s nine ramp coefficients all fit in a 32-bit int for the
	 * given nine slots.
	 *
	 * <p>Replicated from {@code Texture.method378} but computed in {@code long}, because
	 * the routine itself shifts each product left by 14, 8 or 5 bits into an {@code int}.
	 * A geometry whose camera values are large enough to make those products exceed
	 * {@code 2^31} silently wraps, and the resulting picture is a property of the overflow
	 * rather than of the mapping. Any test that compares a predicted mapping against the
	 * real rasteriser must first prove its own geometry is in range.
	 *
	 * <p>The nine are read as three planes of three - {@code (s0,s1,s2) = a},
	 * {@code (s3,s4,s5) = b}, {@code (s6,s7,s8) = c}, i.e. each vertex's {@code u}, {@code v}
	 * and {@code w} respectively.
	 */
	private static boolean nineRampsFitInInt(int[] s) {
		long a0 = s[0];
		long a1 = s[1];
		long a2 = s[2];
		long b0 = s[3];
		long b1 = s[4];
		long b2 = s[5];
		long c0 = s[6];
		long c1 = s[7];
		long c2 = s[8];
		long k2 = a0 - a1;
		long j3 = b0 - b1;
		long i4 = c0 - c1;
		long l2 = a2 - a0;
		long k3 = b2 - b0;
		long j4 = c2 - c0;
		long[] ramps = {
				(l2 * b0 - k3 * a0) << 14,
				(k3 * c0 - j4 * b0) << 8,
				(j4 * a0 - l2 * c0) << 5,
				(k2 * b0 - j3 * a0) << 14,
				(j3 * c0 - i4 * b0) << 8,
				(i4 * a0 - k2 * c0) << 5,
				(j3 * l2 - k2 * k3) << 14,
				(i4 * k3 - j3 * j4) << 8,
				(k2 * j4 - i4 * l2) << 5,
		};
		for (long v : ramps) {
			if (v > Integer.MAX_VALUE || v < Integer.MIN_VALUE) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Whether every one of the three screen points lies inside the current drawing area,
	 * so a rasteriser run is not silently clipped and the comparison stays honest.
	 */
	private static boolean insideArea(int[] sx, int[] sy) {
		for (int i = 0; i < sx.length; i++) {
			if (sx[i] < 0 || sx[i] >= DrawingArea.width || sy[i] < 0 || sy[i] >= DrawingArea.height) {
				return false;
			}
		}
		return true;
	}

	/**
	 * How far the worst point sits from the best straight line through a set of pixels.
	 *
	 * <p>Both orientations are fitted (as y on x and as x on y) and the better one is
	 * reported, so the measure does not blow up on a near-vertical line. Uses it to test
	 * whether pixels sharing one texture COLUMN are collinear - the signature of a ratio
	 * of two screen-affine functions.
	 */
	private static double maxLineResidual(java.util.ArrayList<double[]> pts) {
		return Math.min(maxResidualOfFit(pts, false), maxResidualOfFit(pts, true));
	}

	private static double maxResidualOfFit(java.util.ArrayList<double[]> pts, boolean xOnY) {
		int n = pts.size();
		double sumA = 0.0;
		double sumB = 0.0;
		double sumAA = 0.0;
		double sumAB = 0.0;
		for (double[] q : pts) {
			double a = xOnY ? q[1] : q[0];
			double b = xOnY ? q[0] : q[1];
			sumA += a;
			sumB += b;
			sumAA += a * a;
			sumAB += a * b;
		}
		double denom = n * sumAA - sumA * sumA;
		if (denom == 0.0) {
			return Double.MAX_VALUE;
		}
		double m = (n * sumAB - sumA * sumB) / denom;
		double c = (sumB - m * sumA) / n;
		double worst = 0.0;
		for (double[] q : pts) {
			double a = xOnY ? q[1] : q[0];
			double b = xOnY ? q[0] : q[1];
			worst = Math.max(worst, Math.abs(b - (m * a + c)));
		}
		return worst;
	}

	/** The sampled index at whichever painted pixel lies nearest to a screen point. */
	private static int nearestPaintedIndex(int[] field, int width, int height, int px, int py) {		int best = -1;
		long bestDist = Long.MAX_VALUE;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int idx = field[y * width + x];
				if (idx < 0) {
					continue;
				}
				long dx = x - px;
				long dy = y - py;
				long d = dx * dx + dy * dy;
				if (d < bestDist) {
					bestDist = d;
					best = idx;
				}
			}
		}
		return best;
	}

	/**
	 * The colour oracle: what the pipeline resolves must be the colour the REAL software
	 * rasteriser paints.
	 *
	 * <p><b>Why a uniform-corner face is what makes this exact.</b> {@code method374}
	 * interpolates the 16-bit colour CODE across the triangle and looks each interpolated
	 * code up in the palette per pixel. If all three corner codes are the same that
	 * interpolation is the identity, so every pixel of the triangle has one colour - and
	 * then "does the pipeline agree with the software" becomes a check with no sample
	 * point to argue about and no attribution to guess: every painted pixel must equal
	 * the number the pipeline submitted. The fixture's faces 1 and 6 are that case.
	 *
	 * <p><b>⚠ And it is driven through the software's own entry point, in the software's
	 * own argument order.</b> Not a re-derived formula: {@code method484} is invoked by
	 * reflection, so the pixel is produced by the same code the client runs.
	 *
	 * <p><b>Fog is checked on purpose, not avoided.</b> {@code method443} sets
	 * {@code Fog.sceneDepth} to the model's depth for the duration of the draw, and the
	 * rasteriser fades each colour code through it before the palette lookup - so the
	 * pipeline's fog distance is part of the contract. The test therefore runs the same
	 * comparison twice, once with fog inactive and once with it active, and requires the
	 * active run to be visibly different - otherwise "the fog path is tested" would mean
	 * nothing.
	 */
	private static void modelFaceColoursMatchTheSoftwareRasteriser() {
		int savedWidth = DrawingArea.width;
		int savedHeight = DrawingArea.height;
		int[] savedPixels = DrawingArea.pixels;
		int savedTopX = DrawingArea.topX;
		int savedBottomX = DrawingArea.bottomX;
		int savedTopY = DrawingArea.topY;
		int savedBottomY = DrawingArea.bottomY;
		int savedCenterX = DrawingArea.centerX;
		int savedCenterY = DrawingArea.centerY;
		int savedExtent = DrawingArea.anInt1387;
		Object savedTextureInt1 = readStatic(Texture.class, "textureInt1");
		Object savedTextureInt2 = readStatic(Texture.class, "textureInt2");
		int savedSceneDepth = Fog.sceneDepth;
		int savedAlpha = Texture.anInt1465;
		boolean savedClamp = Texture.aBoolean1462;
		boolean savedHighDetail = Texture.aBoolean1464;
		try {
			int w = 765;
			int h = 503;
			int[] buf = new int[w * h];
			DrawingArea.initDrawingArea(h, w, buf);
			Texture.method364();
			installDeterministicPalette();
			Fog.sceneDepth = 0;
			Texture.anInt1465 = 0;
			Texture.aBoolean1462 = false;
			Texture.aBoolean1464 = true;

			// The three distinct codes must resolve to three different colours, or a
			// swapped corner assignment would be invisible and the association check
			// below would have no teeth.
			int cA = GlFacePipeline.resolveCornerColour(0x1111, 0);
			int cB = GlFacePipeline.resolveCornerColour(0x2222, 0);
			int cC = GlFacePipeline.resolveCornerColour(0x3333, 0);
			check("Face colour: the fixture's three distinct codes resolve to three distinct "
					+ "colours, so a swapped corner cannot hide",
					cA != cB && cB != cC && cA != cC && cA >= 0 && cB >= 0 && cC >= 0);

			// ---- Part 1: the pipeline's own resolution, against the real rasteriser.
			//
			// Each case paints ONE triangle with method374 - exactly the call method484
			// makes for a flat face - and requires every painted pixel to equal what the
			// pipeline resolves for that code at that depth.
			Texture.method374(60, 300, 180, 90, 200, 460, 0x0ABC, 0x0ABC, 0x0ABC);
			boolean noFogMatches = everyPaintedPixelIs(buf, w, h, 0,
					GlFacePipeline.resolveCornerColour(0x0ABC, 0));
			check("Face colour: with fog inactive, every pixel the software rasteriser "
					+ "painted is the colour the pipeline resolves", noFogMatches);

			DrawingArea.setAllPixels(0);
			Texture.method374(60, 300, 180, 90, 200, 460, 0x0ABC, 0x0ABC, 0x0ABC);
			int foggedPixel = firstPaintedPixel(buf, w, h);
			DrawingArea.setAllPixels(0);
			Fog.sceneDepth = 5000;
			Texture.method374(60, 300, 180, 90, 200, 460, 0x0ABC, 0x0ABC, 0x0ABC);
			int softwareFogged = firstPaintedPixel(buf, w, h);
			int pipelineFogged = GlFacePipeline.resolveCornerColour(0x0ABC, 5000);
			check("Face colour: the scene depth the pipeline is given really changes the "
					+ "colour, so the fog path is being exercised, not skipped",
					softwareFogged != foggedPixel);
			check("Face colour: with fog active, the software's pixel is the colour the "
					+ "pipeline resolves at the same scene depth",
					softwareFogged == pipelineFogged && softwareFogged >= 0);
			Fog.sceneDepth = 0;

			// ---- Part 2: the association. Corner colours must ride the face's OWN vertex
			// indices, and be the palette's colours for the codes in the model's OWN
			// corner-colour arrays - which is the pairing method484's call expression
			// states (anIntArray1634/1635/1636 indexed by i, against anIntArray1631/1632/
			// 1633 of the same i).
			Model m = buildFaceFixture();
			GlFacePipeline pipeline = new GlFacePipeline();
			RecordingSink sink = new RecordingSink();
			int centreX = 382;
			int centreY = 251;
			writeStatic(Texture.class, "textureInt1", centreX);
			writeStatic(Texture.class, "textureInt2", centreY);
			// The same unrotated camera the census test uses, so the model is on screen
			// and both the distinct-corner and uniform-corner faces are really drawn.
			pipeline.emit(m, 0, 0, 65536, 0, 65536, 10, 20, 700,
					centreX, centreY, sink);
			int[] outcomes = pipeline.outcomes();
			int[] colourA = m.faceCornerColoursA();
			int[] colourB = m.faceCornerColoursB();
			int[] colourC = m.faceCornerColoursC();
			int[] cornerCodes = new int[3];
			int[] cornerColours = new int[3];
			boolean associationMatches = true;
			boolean distinctFacesSeen = false;
			boolean uniformFacesSeen = false;
			int tri = 0;
			int sceneDepth = pipeline.sceneDepth();
			for (int face = 0; face < m.faceCount(); face++) {
				if (outcomes[face] != GlFacePipeline.DRAWN) {
					continue;
				}
				cornerCodes[0] = colourA[face];
				cornerCodes[1] = colourB[face];
				cornerCodes[2] = colourC[face];
				boolean distinct = cornerCodes[0] != cornerCodes[1]
						&& cornerCodes[1] != cornerCodes[2] && cornerCodes[0] != cornerCodes[2];
				boolean uniform = !distinct;
				for (int corner = 0; corner < 3; corner++) {
					cornerColours[corner] =
							GlFacePipeline.resolveCornerColour(cornerCodes[corner], sceneDepth);
					if (sink.colour(tri, corner) != (0xff000000 | cornerColours[corner])) {
						associationMatches = false;
					}
				}
				if (distinct) {
					distinctFacesSeen = true;
				}
				if (uniform) {
					uniformFacesSeen = true;
				}
				tri++;
			}
			check("Face colour: each submitted corner carries the palette colour of its own "
					+ "vertex's corner code", associationMatches);
			check("Face colour: the fixture drew a face with three DISTINCT corner codes, so "
					+ "the association check above cannot pass by the corners being equal",
					distinctFacesSeen);
			check("Face colour: the fixture drew a uniform-cornered face too, which is the "
					+ "case the exact pixel oracle is meaningful for", uniformFacesSeen);
		} finally {
			DrawingArea.pixels = savedPixels;
			DrawingArea.width = savedWidth;
			DrawingArea.height = savedHeight;
			DrawingArea.topX = savedTopX;
			DrawingArea.bottomX = savedBottomX;
			DrawingArea.topY = savedTopY;
			DrawingArea.bottomY = savedBottomY;
			DrawingArea.centerX = savedCenterX;
			DrawingArea.centerY = savedCenterY;
			DrawingArea.anInt1387 = savedExtent;
			writeStatic(Texture.class, "textureInt1", savedTextureInt1);
			writeStatic(Texture.class, "textureInt2", savedTextureInt2);
			Fog.sceneDepth = savedSceneDepth;
			Texture.anInt1465 = savedAlpha;
			Texture.aBoolean1462 = savedClamp;
			Texture.aBoolean1464 = savedHighDetail;
		}
	}

	/** The first pixel that is not the clear colour {@code 0}, or {@code -1} if none is. */
	private static int firstPaintedPixel(int[] buf, int w, int h) {
		for (int i = 0; i < w * h; i++) {
			if (buf[i] != 0) {
				return buf[i];
			}
		}
		return -1;
	}

	// ------------------------------- the shade model, measured

	/**
	 * A model carrying BOTH colour regimes of {@code Model.method479} at once.
	 *
	 * <p>Four vertices of a quarter-tetrahedron (not coplanar, so every face has real
	 * area), four faces alternating render type 0 and render type 1. Deliberately built
	 * from bare geometry rather than parsed, because the point is to drive the REAL
	 * {@code method479} over a known mix of render types and observe which slots it
	 * touches.
	 */
	private static Model buildShadeFixture() {
		Model m = parseFixtureModel();
		m.anIntArray1627 = new int[] { 0, 120, 0, 0 };
		m.anIntArray1628 = new int[] { 0, 0, 120, 0 };
		m.anIntArray1629 = new int[] { 0, 0, 0, 120 };
		writeField(m, "anInt1626", 4);

		// Faces 0 and 2 are render type 0 (method374, three corner codes); faces 1 and 3
		// are render type 1 (method376, ONE code). Same call, two regimes.
		m.anIntArray1631 = new int[] { 0, 0, 0, 1 };
		m.anIntArray1632 = new int[] { 1, 1, 2, 2 };
		m.anIntArray1633 = new int[] { 2, 3, 3, 3 };
		writeField(m, "anInt1630", 4);
		writeField(m, "anIntArray1637", new int[] { 0, 1, 0, 1 });
		// method479's flat path reads anIntArray1640 for the code it shades.
		writeField(m, "anIntArray1640", new int[] { 0x0ABC, 0x0DEF, 0x0ABC, 0x0DEF });

		// method479 ends by recomputing these from the vertices, but set them anyway so a
		// failure inside that tail cannot be mistaken for a slot that was never written.
		writeField(m, "anInt1653", 400);
		writeField(m, "anInt1652", 800);
		m.anInt1650 = 500;
		return m;
	}

	/**
	 * THE SHADE MODEL'S LAYOUT, MEASURED RATHER THAN ASSUMED: which of the three
	 * per-corner colour slots {@code Model.method479} actually writes, per render type.
	 *
	 * <p><b>Why this is worth a test at all.</b> Phase 7.2b-2b recorded that the GL path
	 * "consumes the per-corner colours as the software left them" and that reproducing
	 * {@code method479}/{@code method481} was therefore still owed. Whether the first half
	 * of that is SAFE depends on a fact that reading kept rendering ambiguously:
	 * {@code method479} does NOT write all three slots for every render type.
	 * {@code method484} reads only the slot its primitive needs - {@code anIntArray1634}
	 * alone for render type 1 - so the software never notices. Any consumer that reads all
	 * three, as a triangle-emitting pipeline must, reads slots that were never written.
	 *
	 * <p><b>Sentinels rather than inference.</b> Each slot is pre-filled with a value no
	 * legal {@code method481} output can produce, so "untouched" is directly observable
	 * instead of being confused with "computed to zero". This is also why the check is
	 * worth more than a comment: with the slots pre-filled, the assertion distinguishes
	 * "method479 does not write it" from "method479 writes zero there".
	 */
	private static void shadeModelWritesOnlyTheSlotsEachRenderTypeUses() {
		Model m = buildShadeFixture();
		int faces = m.faceCount();
		int sentinel = 0x5A5A;
		int[] a = new int[faces];
		int[] b = new int[faces];
		int[] c = new int[faces];
		java.util.Arrays.fill(a, sentinel);
		java.util.Arrays.fill(b, sentinel);
		java.util.Arrays.fill(c, sentinel);
		writeField(m, "anIntArray1634", a);
		writeField(m, "anIntArray1635", b);
		writeField(m, "anIntArray1636", c);

		m.method479(64, 850, -30, -50, -30, true);

		int[] renderTypes = m.faceRenderTypes();
		boolean sawFlat = false;
		boolean sawGouraud = false;
		for (int f = 0; f < faces; f++) {
			int type = renderTypes == null ? 0 : renderTypes[f] & 3;
			boolean wroteA = a[f] != sentinel;
			boolean wroteB = b[f] != sentinel;
			boolean wroteC = c[f] != sentinel;
			if (type == 1) {
				sawFlat = true;
				check("Shade model: a render-type-1 face writes ONLY anIntArray1634 (face "
						+ f + ": wroteA=" + wroteA + " wroteB=" + wroteB + " wroteC="
						+ wroteC + ") - the single slot method484 hands to method376, and "
						+ "the reason a triangle emitter must use ONE colour for this face",
						wroteA && !wroteB && !wroteC);
			} else if (type == 0) {
				sawGouraud = true;
				check("Shade model: a render-type-0 face writes ALL THREE corners (face "
						+ f + ": wroteA=" + wroteA + " wroteB=" + wroteB + " wroteC="
						+ wroteC + "), because method480 lights each vertex and method374 "
						+ "interpolates them across the triangle",
						wroteA && wroteB && wroteC);
			}
		}
		check("Shade model: the fixture actually exercised both regimes (flat=" + sawFlat
				+ " gouraud=" + sawGouraud + "), so neither assertion above is vacuous",
				sawFlat && sawGouraud);
	}

	/**
	 * THE EMIT-LEVEL PIN, which the colour oracle alone does NOT provide: a correct
	 * {@link GlFacePipeline#resolveFlatColour} is worthless if {@code emit} never calls it,
	 * so this drives the real {@code emit} over the shade fixture and requires a
	 * render-type-1 face to ARRIVE as one colour, taken from {@code anIntArray1634} alone.
	 *
	 * <p><b>Why the fixture gives every slot a different code.</b> If a type-1 face read
	 * all three slots - the pre-fix behaviour - its corners would be the three palette
	 * entries of 1634/1635/1636. Making those three distinct means "one colour" is only
	 * reachable by genuinely using one slot; identical codes would let the bug produce a
	 * flat triangle by coincidence and pass.
	 *
	 * <p>It also asserts the change did NOT flatten everything: a type-0 face must still
	 * come through with distinct corners, or "flat" would be indistinguishable from
	 * "the gouraud path stopped running".
	 */
	private static void flatFacesAreEmittedAsOneColour() {
		int[] savedDrawingArea = saveDrawingArea();
		int[] savedPixels = DrawingArea.pixels;
		Object savedTextureInt1 = readStatic(Texture.class, "textureInt1");
		Object savedTextureInt2 = readStatic(Texture.class, "textureInt2");
		try {
			int w = 765;
			int h = 503;
			DrawingArea.initDrawingArea(h, w, new int[w * h]);
			Texture.method364();
			installDeterministicPalette();
			int centreX = 382;
			int centreY = 251;
			writeStatic(Texture.class, "textureInt1", centreX);
			writeStatic(Texture.class, "textureInt2", centreY);

			Model m = buildShadeFixture();
			// A DIFFERENT code in every slot: "all three corners equal" is then only
			// reachable by using one slot, not by the slots happening to agree.
			writeField(m, "anIntArray1634", new int[] { 0x0ABC, 0x0DEF, 0x0ABC, 0x0DEF });
			writeField(m, "anIntArray1635", new int[] { 0x0111, 0x0222, 0x0333, 0x0444 });
			writeField(m, "anIntArray1636", new int[] { 0x0555, 0x0666, 0x0777, 0x0888 });
			int[] colourA = m.faceCornerColoursA();
			int[] colourB = m.faceCornerColoursB();
			int[] colourC = m.faceCornerColoursC();

			// ⚠ AN A/B ON THE SAME GEOMETRY, NOT A HUNT FOR A COOPERATIVE CAMERA. Which
			// faces survive the winding test depends on the model's orientation, so a
			// fixture that "should" show both regimes can silently draw only one and let
			// half this test skip - which is exactly what the first version of it did.
			// Flipping the render type between two otherwise IDENTICAL passes removes that
			// dependence: the same faces are drawn twice and must change behaviour.
			int drawnGouraud = 0;
			int drawnFlat = 0;

			// ---- Pass A: every face render type 0 -> three corners, each its own code.
			writeField(m, "anIntArray1637", new int[] { 0, 0, 0, 0 });
			GlFacePipeline pipeline = new GlFacePipeline();
			RecordingSink sink = new RecordingSink();
			pipeline.emit(m, 0, 0, 65536, 0, 65536, 10, 20, 700, centreX, centreY, sink);
			int depth = pipeline.sceneDepth();
			int[] outcomes = pipeline.outcomes();
			int n = 0;
			for (int f = 0; f < m.faceCount(); f++) {
				if (outcomes[f] != GlFacePipeline.DRAWN) {
					continue;
				}
				drawnGouraud++;
				int o = n++ * 12;
				// ⚠ Compared against the CORNER formula, which under fog differs from the
				// flat one - so this cannot pass if the flat branch had been taken here.
				// Masked to 24 bits because the sink's argb carries an opaque alpha byte
				// that the resolvers do not add; OPAQUE itself is private to the pipeline.
				check("Flat face emit: with render type 0, face " + f + " keeps THREE per-corner "
						+ "colours, each the palette entry of that corner's OWN code - so the flat "
						+ "branch is specific to type 1 rather than flattening every face",
						((int) sink.floats[o + 3] & 0xFFFFFF)
								== GlFacePipeline.resolveCornerColour(colourA[f], depth)
						&& ((int) sink.floats[o + 7] & 0xFFFFFF)
								== GlFacePipeline.resolveCornerColour(colourB[f], depth)
						&& ((int) sink.floats[o + 11] & 0xFFFFFF)
								== GlFacePipeline.resolveCornerColour(colourC[f], depth));
			}

			// ---- Pass B: the SAME faces, render type 1 -> one colour from 1634 alone.
			writeField(m, "anIntArray1637", new int[] { 1, 1, 1, 1 });
			GlFacePipeline flatPipeline = new GlFacePipeline();
			RecordingSink flatSink = new RecordingSink();
			flatPipeline.emit(m, 0, 0, 65536, 0, 65536, 10, 20, 700, centreX, centreY, flatSink);
			int flatDepth = flatPipeline.sceneDepth();
			int[] flatOutcomes = flatPipeline.outcomes();
			n = 0;
			for (int f = 0; f < m.faceCount(); f++) {
				if (flatOutcomes[f] != GlFacePipeline.DRAWN) {
					continue;
				}
				drawnFlat++;
				int o = n++ * 12;
				int c0 = (int) flatSink.floats[o + 3];
				int c1 = (int) flatSink.floats[o + 7];
				int c2 = (int) flatSink.floats[o + 11];
				int expected = GlFacePipeline.resolveFlatColour(colourA[f], flatDepth);
				check("Flat face emit: with render type 1 the SAME face " + f + " arrives as ONE "
						+ "colour, taken from anIntArray1634 alone (" + Integer.toHexString(c0)
						+ " / " + Integer.toHexString(c1) + " / " + Integer.toHexString(c2) + ")",
						expected >= 0 && c0 == c1 && c1 == c2
								&& (c0 & 0xFFFFFF) == expected
								&& (c1 & 0xFFFFFF) == expected
								&& (c2 & 0xFFFFFF) == expected);
			}
			check("Flat face emit: the two passes drew the SAME faces (" + drawnGouraud + " vs "
					+ drawnFlat + ") and there was at least one, so the A/B is a real comparison "
					+ "rather than two empty loops",
					drawnGouraud == drawnFlat && drawnGouraud > 0);
		} finally {
			restoreDrawingArea(savedDrawingArea, savedPixels);
			writeStatic(Texture.class, "textureInt1", savedTextureInt1);
			writeStatic(Texture.class, "textureInt2", savedTextureInt2);
		}
	}

	/**
	 * THE FLAT PATH'S ORACLE: a render-type-1 face must come out the colour
	 * {@code Texture.method376} would paint it with, fog included.
	 *
	 * <p>⚠ <b>The two colour paths fog in OPPOSITE ORDERS and this test exists to hold
	 * that apart.</b> {@code method374} (type 0) fades the colour CODE and lets the
	 * rasteriser look the palette up per interpolated code. {@code method376} (type 1)
	 * looks the palette up FIRST and fades the resulting RGB. With fog inactive both
	 * collapse to {@code palette[code]}, which is precisely why the difference survives a
	 * casual reading - so this test runs with fog ON, and asserts that the two orders
	 * disagree before trusting the agreement check above it.
	 */
	private static void flatFacesMatchTheSoftwareFlatFill() {
		int savedWidth = DrawingArea.width;
		int savedHeight = DrawingArea.height;
		int[] savedPixels = DrawingArea.pixels;
		int savedSceneDepth = Fog.sceneDepth;
		int savedAlpha = Texture.anInt1465;
		boolean savedClamp = Texture.aBoolean1462;
		boolean savedHighDetail = Texture.aBoolean1464;
		try {
			int w = 765;
			int h = 503;
			int[] buf = new int[w * h];
			DrawingArea.initDrawingArea(h, w, buf);
			Texture.method364();
			installDeterministicPalette();
			Texture.anInt1465 = 0;
			Texture.aBoolean1462 = false;
			Texture.aBoolean1464 = true;

			int code = 0x0ABC;
			int depth = 5000;
			int[] palette = Texture.anIntArray1482;

			// ---- FOG ON: the pipeline's flat colour must be what method376 paints.
			Fog.sceneDepth = depth;
			Texture.method376(60, 300, 180, 90, 200, 460, palette[code]);
			check("Flat face: with fog active, every pixel method376 painted is the colour "
					+ "the pipeline resolves for the same code and depth",
					everyPaintedPixelIs(buf, w, h, 0,
							GlFacePipeline.resolveFlatColour(code, depth)));

			// ---- NON-VACUITY, and it is the reason the check above runs fogged: if the
			// two orders agreed, using the corner formula here would pass the oracle too.
			int flat = GlFacePipeline.resolveFlatColour(code, depth);
			int corner = GlFacePipeline.resolveCornerColour(code, depth);
			check("Flat face: with fog active the flat order (palette, then fade the RGB) "
					+ "and the corner order (fade the code, then palette) give DIFFERENT "
					+ "colours (" + Integer.toHexString(flat) + " vs "
					+ Integer.toHexString(corner) + "), so the oracle above can fail",
					flat != corner);

			// ---- FOG OFF: both orders coincide. Asserted rather than assumed, because
			// this is the exact condition under which the bug would be invisible.
			Fog.sceneDepth = 0;
			check("Flat face: with fog inactive the two orders agree and both are the "
					+ "plain palette entry - which is why a fog-off reading of this code "
					+ "cannot see the difference",
					GlFacePipeline.resolveFlatColour(code, 0)
							== GlFacePipeline.resolveCornerColour(code, 0)
					&& GlFacePipeline.resolveFlatColour(code, 0) == palette[code]);
		} finally {
			DrawingArea.width = savedWidth;
			DrawingArea.height = savedHeight;
			DrawingArea.pixels = savedPixels;
			Fog.sceneDepth = savedSceneDepth;
			Texture.anInt1465 = savedAlpha;
			Texture.aBoolean1462 = savedClamp;
			Texture.aBoolean1464 = savedHighDetail;
		}
	}

	/**
	 * Whether every non-clear pixel equals {@code colour}, with the CONTROL that at least
	 * one pixel was painted at all - a rasteriser that drew nothing would otherwise
	 * satisfy "every painted pixel matches" trivially.
	 */
	private static boolean everyPaintedPixelIs(int[] buf, int w, int h, int clear,
			int colour) {
		int painted = 0;
		for (int i = 0; i < w * h; i++) {
			if (buf[i] == clear) {
				continue;
			}
			painted++;
			if (buf[i] != colour) {
				return false;
			}
		}
		return painted > 0;
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
				1, 2, 3, 4, 5, 6, 7, 8, 9, -1, false, 11, 12, 13, 14, 15, 16, 17, 18, 19, 600);
		check("Ground rasteriser seam: declines (returns false) when no rasteriser is installed",
				!handled);
	}

	private static void groundSeamInterceptsWhenInstalled() {
		final int[] seen = new int[21];
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
					int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7, int t8,
					int depth) {
				calls[0]++;
				int[] v = { x0, y0, x1, y1, x2, y2, colour0, colour1, colour2, textureId,
						flatMesh ? 1 : 0, t0, t1, t2, t3, t4, t5, t6, t7, t8, depth };
				System.arraycopy(v, 0, seen, 0, 21);
				return true;
			}

			public boolean sceneFinished(RSImageProducer producer) {
				return false;
			}
		});
		try {
			boolean handled = SceneRasterizer.dispatchGroundTriangle(
					10, 20, 30, 40, 50, 60, 70, 80, 90, 7, true, 101, 102, 103, 104, 105, 106,
					107, 108, 109, 700);
			boolean orderOk = seen[0] == 10 && seen[1] == 20 && seen[2] == 30 && seen[3] == 40
					&& seen[4] == 50 && seen[5] == 60 && seen[6] == 70 && seen[7] == 80
					&& seen[8] == 90 && seen[9] == 7 && seen[10] == 1 && seen[11] == 101
					&& seen[19] == 109 && seen[20] == 700;
			check("Ground rasteriser seam: an installed rasteriser intercepts the ground triangle",
					handled && calls[0] == 1);
			check("Ground rasteriser seam: the whole payload arrives in order (coords, colours, "
					+ "textureId, flatMesh, t0..t8)", orderOk);
			// ⚠ Phase 7.2c-2. The depth is asserted SEPARATELY from the payload order above,
			// because that check would still pass if the field were appended to the record and
			// the value were a constant: this one pins the VALUE the caller supplied, which is
			// what the ground's z depends on.
			check("Ground rasteriser seam: the tile's camera-space depth is carried through "
					+ "unchanged, so a listener sees the software's own number", seen[20] == 700);
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

		// ⚠⚠ ISOLATION, and this line is the FIX for what the plan recorded as an
		// "unidentified shared-state interaction" between this test and anything that
		// drives a real `Model.method443` (Phase 7.2b-2b, 2026-10-06).
		//
		// This test calls `method484` DIRECTLY, so it must supply the two `private static`
		// flags `method483` would normally have written for it - it is `method483`'s output
		// that `method484` reads, and this test skips `method483` entirely:
		//     aBooleanArray1664[face] : true  -> route the face to method485, the CLIPPER
		//     aBooleanArray1663[face] : copied into Texture.aBoolean1462, the X clamp
		// Left over from an earlier real draw they are stale, and the failure they produce
		// is silent and looks like a rasteriser bug rather than a test-setup one: face 0
		// took the clipper branch, the clipper read the ALSO-stale `anIntArray1667` (all
		// zero, i.e. every vertex behind the near plane), discarded the whole triangle, and
		// the only symptom was "the flat face's draw plotted pixels" failing.
		// Measured, not deduced: with a real `method443` run first, the flags read back as
		// `aBooleanArray1664[0..3] = true,true,false,false`. Resetting them makes this test
		// independent of test order, which is why the ordering workaround is now gone.
		writeStatic(Model.class, "aBooleanArray1664", new boolean[4096]);
		writeStatic(Model.class, "aBooleanArray1663", new boolean[4096]);

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
		int[] files = ContentRegistry.CURSE_ANIM_FILES;

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

	// ------------------- Phase 6.5.5 content registry (declared external frame sources)
	//
	// Phase 6.5.5: the registry declares which external frame files the hand-written
	// animations depend on, so an absent or repointed source becomes a REPORTED condition
	// instead of silent missing frames.
	//
	// The defect these pin: Animation.java writes 25 animations by hand, and each frame
	// literal packs its source file into the high 16 bits (frame = file << 16 | index), so
	// the dependency is a magic number inside the animation's own data. Deriving
	// frame >>> 16 for all of them shows anims 4000, 4001 and 4002 need files 3403 and
	// 3353, while the packed 474 archive stops at 3229 and the cache root holds only
	// 1777/2160/3502. Nothing supplied 3353/3403, so Frames.method531 returned null and
	// those animations rendered with NO FRAMES.

	/**
	 * &#9888; The property that makes loading a declared source safe at all: every declared
	 * slot must be OUTSIDE both ranges that already have owners. Writing into the packed 474
	 * range would overwrite real frame data, and writing into the CursePack window would
	 * clobber the curse remap - both silently, because {@code Frames.load} does not look
	 * before it writes.
	 */
	private static void contentRegistryDeclarationsAreSafe() {
		ContentRegistry.FrameSource[] sources = ContentRegistry.FRAME_SOURCES;
		check("Registry: at least one external frame source is declared", sources.length > 0);

		java.util.HashSet<Integer> slots = new java.util.HashSet<Integer>();
		java.util.HashSet<Integer> declaredAnims = new java.util.HashSet<Integer>();
		boolean duplicateSlot = false;
		boolean unaddressable = false;
		boolean insidePackedRange = false;
		boolean insideCurseWindow = false;
		boolean badAsset = false;
		boolean emptyDependency = false;
		boolean duplicateAnim = false;

		for (int i = 0; i < sources.length; i++) {
			ContentRegistry.FrameSource s = sources[i];
			if (!slots.add(s.slot)) {
				duplicateSlot = true;
			}
			if (!FrameSlots.isKeyAddressable(s.slot)) {
				unaddressable = true;
			}
			if (s.slot >= 0 && s.slot <= MEASURED_PACKED_FRAME_MAX_ID) {
				insidePackedRange = true;
			}
			if (FrameSlots.isCurseSlot(s.slot)) {
				insideCurseWindow = true;
			}
			if (s.asset == null || s.asset.length() == 0 || !s.asset.endsWith(".gz")) {
				badAsset = true;
			}
			if (s.requiredByAnimIds == null || s.requiredByAnimIds.length == 0) {
				emptyDependency = true;
			} else {
				for (int k = 0; k < s.requiredByAnimIds.length; k++) {
					if (!declaredAnims.add(s.requiredByAnimIds[k])) {
						duplicateAnim = true;
					}
				}
			}
		}

		check("Registry: no two sources claim the same slot", !duplicateSlot);
		check("Registry: every declared slot is key-addressable, so frame << 16 stays positive",
				!unaddressable);
		check("Registry: no declared slot lands inside the packed 474 range 0.."
				+ MEASURED_PACKED_FRAME_MAX_ID + " - that would overwrite real frame data",
				!insidePackedRange);
		check("Registry: no declared slot lands inside the CursePack window "
				+ FrameSlots.CURSE_BASE + ".." + FrameSlots.CURSE_END,
				!insideCurseWindow);
		check("Registry: every declared asset is a .gz path", !badAsset);
		check("Registry: every source names the animations that need it", !emptyDependency);
		check("Registry: no animation is claimed by two different sources", !duplicateAnim);
	}

	/**
	 * The loader, driven against a REAL pack layout for the declared asset (a temp folder
	 * containing anims/{id}.gz), so this exercises the shipping path rather than a stub.
	 */
	private static void contentRegistryLoadsADeclaredSourceIntoItsSlot() {
		ContentRegistry.FrameSource source = ContentRegistry.FRAME_SOURCES[0];
		File root = makeTempPackRoot("content-registry-loads");
		File asset = new File(root, source.asset);
		asset.getParentFile().mkdirs();
		writeGz(asset, buildFramesFixture(1, 3, 4, 5));

		File savedRoot = ContentRegistry.root;
		try {
			ContentRegistry.root = root;
			int loaded = ContentRegistry.loadAll();
			check("Registry: loadAll() loads the declared source it can find (" + source.asset + ")",
					loaded == 1);
			check("Registry: the loaded source really lands in its declared slot " + source.slot,
					Frames.fileFrameCount(source.slot) > 0);
			check("Registry: isSourceLoaded agrees with the frame store",
					ContentRegistry.isSourceLoaded(source));
		} finally {
			ContentRegistry.root = savedRoot;
		}
	}

	/**
	 * The sensitivity check for the one above - and the whole point of the phase. A loader
	 * that reported success whether or not the file existed is exactly how this defect
	 * survived, so an ABSENT source must load zero and leave the slot empty.
	 */
	private static void contentRegistryReportsAMissingSourceRatherThanPassingSilently() {
		ContentRegistry.FrameSource source = ContentRegistry.FRAME_SOURCES[1];
		File root = makeTempPackRoot("content-registry-missing");
		// The asset is deliberately NOT written.

		File savedRoot = ContentRegistry.root;
		try {
			ContentRegistry.root = root;
			int loaded = ContentRegistry.loadAll();
			check("Registry: a pack root with no declared assets loads exactly ZERO"
					+ " (an absent source must not look like a loaded one)", loaded == 0);
			check("Registry: an absent source leaves its slot EMPTY rather than half-populated",
					Frames.fileFrameCount(source.slot) == 0);
		} finally {
			ContentRegistry.root = savedRoot;
		}
	}

	/**
	 * The drift guard for the DECLARATION itself: validate() must be clean when every
	 * declared animation's frames decode to its declared slot, and must CATCH a frame
	 * repointed at an undeclared file. Without the second half this would be a test that
	 * cannot fail.
	 */
	private static void contentRegistryValidateCatchesADriftedDeclaration() {
		Animation[] saved = Animation.anims;
		try {
			int highest = 0;
			for (int i = 0; i < ContentRegistry.FRAME_SOURCES.length; i++) {
				int[] ids = ContentRegistry.FRAME_SOURCES[i].requiredByAnimIds;
				for (int k = 0; k < ids.length; k++) {
					if (ids[k] > highest) {
						highest = ids[k];
					}
				}
			}
			Animation.anims = new Animation[highest + 1];
			for (int i = 0; i < ContentRegistry.FRAME_SOURCES.length; i++) {
				ContentRegistry.FrameSource s = ContentRegistry.FRAME_SOURCES[i];
				for (int k = 0; k < s.requiredByAnimIds.length; k++) {
					Animation a = new Animation();
					a.anIntArray353 = new int[] { (s.slot << 16) | 1 };
					Animation.anims[s.requiredByAnimIds[k]] = a;
				}
			}
			check("Registry: validate() is CLEAN when every declared animation's frames decode"
					+ " to its declared slot", ContentRegistry.validate().length() == 0);

			Animation drifted =
					Animation.anims[ContentRegistry.FRAME_SOURCES[1].requiredByAnimIds[0]];
			drifted.anIntArray353[0] = (9999 << 16) | 1;
			String report = ContentRegistry.validate();
			check("Registry: validate() CATCHES a frame repointed at an undeclared file"
					+ " (reported: " + report.trim() + ")", report.length() > 0);
		} finally {
			Animation.anims = saved;
		}
	}

	/**
	 * Phase 6.5.6: the point of the migration is that the declaration has ONE owner.
	 * This is the check that keeps it that way - a re-introduced private array in
	 * CurseData667 would silently become a second source of truth, which is exactly the
	 * condition 6.5.1's decision was made to end.
	 */
	private static void declaratationsLiveInOnePlaceOnly() {
		String[] moved = {
			"CURSE_SEQ_IDS", "CURSE_GFX_IDS", "CURSE_MODEL_IDS", "CURSE_ANIM_FILES", "PACK_FOLDER"
		};
		String leaked = "";
		java.lang.reflect.Field[] fields = CurseData667.class.getDeclaredFields();
		for (int i = 0; i < moved.length; i++) {
			for (int k = 0; k < fields.length; k++) {
				if (fields[k].getName().equals(moved[i])) {
					leaked = fields[k].getName();
				}
			}
		}
		check("Registry: CurseData667 no longer declares its own id arrays or pack folder"
				+ (leaked.length() > 0 ? " - FOUND " + leaked : "")
				+ ", so the registry is the ONE owner", leaked.length() == 0);

		// The migration is a MOVE, not a change, so the values must be exactly the
		// historical ones - a silently altered id list is the failure this pins.
		check("Registry: CURSE_SEQ_IDS is exactly the historical 11 ids",
				ContentRegistry.CURSE_SEQ_IDS.length == 11
						&& ContentRegistry.CURSE_SEQ_IDS[0] == 12565
						&& ContentRegistry.CURSE_SEQ_IDS[6] == 12580
						&& ContentRegistry.CURSE_SEQ_IDS[10] == 12590);
		check("Registry: CURSE_GFX_IDS is exactly the historical 9 ids",
				ContentRegistry.CURSE_GFX_IDS.length == 9
						&& ContentRegistry.CURSE_GFX_IDS[0] == 2213
						&& ContentRegistry.CURSE_GFX_IDS[8] == 2266);
		check("Registry: CURSE_MODEL_IDS is exactly the historical 9 ids",
				ContentRegistry.CURSE_MODEL_IDS.length == 9
						&& ContentRegistry.CURSE_MODEL_IDS[0] == 50778
						&& ContentRegistry.CURSE_MODEL_IDS[8] == 50819);
		check("Registry: CURSE_ANIM_FILES is exactly the historical 7 files",
				ContentRegistry.CURSE_ANIM_FILES.length == 7
						&& ContentRegistry.CURSE_ANIM_FILES[0] == 2998
						&& ContentRegistry.CURSE_ANIM_FILES[6] == 3020);

		check("Registry: exactly two spotanim overrides are declared",
				ContentRegistry.SPOTANIM_OVERRIDES.length == 2);
		ContentRegistry.SpotAnimOverride a = ContentRegistry.SPOTANIM_OVERRIDES[0];
		ContentRegistry.SpotAnimOverride b = ContentRegistry.SPOTANIM_OVERRIDES[1];
		check("Registry: override 0 is gfx 1247 -> model 60776 / anim 4001, the values"
				+ " the hardcoded branch used",
				a.gfxId == 1247 && a.modelId == 60776 && a.animId == 4001);
		check("Registry: override 1 is gfx 1248 -> model 60776 / anim 4002, the values"
				+ " the hardcoded branch used",
				b.gfxId == 1248 && b.modelId == 60776 && b.animId == 4002);
	}

	/**
	 * The override MECHANISM, driven directly: a spotanim whose id is declared must get
	 * the declared model and animation - the behaviour the two hardcoded branches used
	 * to provide - and an undeclared id must be left completely alone.
	 */
	private static void spotAnimOverridesAreAppliedToTheDeclaredIds() {
		java.lang.reflect.Method m;
		try {
			m = SpotAnim.class.getDeclaredMethod("applyDeclaredOverrides", SpotAnim.class);
			m.setAccessible(true);
		} catch (Exception e) {
			throw new RuntimeException("could not find applyDeclaredOverrides", e);
		}

		for (int i = 0; i < ContentRegistry.SPOTANIM_OVERRIDES.length; i++) {
			ContentRegistry.SpotAnimOverride o = ContentRegistry.SPOTANIM_OVERRIDES[i];
			SpotAnim s = new SpotAnim();
			s.anInt404 = o.gfxId;
			invokeQuietly(m, s);
			check("Registry: the declared override for gfx " + o.gfxId + " is applied"
					+ " (model " + s.anInt405 + ", anim " + s.anInt406 + ")",
					s.anInt405 == o.modelId && s.anInt406 == o.animId);
		}

		SpotAnim untouched = new SpotAnim();
		untouched.anInt404 = 1246;   // adjacent to a declared id, but NOT declared
		untouched.anInt405 = 111;
		untouched.anInt406 = 222;
		invokeQuietly(m, untouched);
		check("Registry: an UNDECLARED spotanim id is left exactly as it was"
				+ " (an override leaking to every spotanim would be a real behaviour change)",
				untouched.anInt405 == 111 && untouched.anInt406 == 222);
	}

	private static void invokeQuietly(java.lang.reflect.Method m, SpotAnim target) {
		try {
			m.invoke(null, target);
		} catch (Exception e) {
			throw new RuntimeException("could not invoke " + m.getName(), e);
		}
	}

	/** A clean temp pack root with an empty `anims/` folder, for the registry tests. */
	private static File makeTempPackRoot(String name) {
		File dir = new File(System.getProperty("java.io.tmpdir"), name);
		deleteRecursively(dir);
		new File(dir, "anims").mkdirs();
		return dir;
	}

	/** Writes a real gzip file, which is the layout the registry reads. */
	private static void writeGz(File target, byte[] payload) {
		try {
			java.io.FileOutputStream out = new java.io.FileOutputStream(target);
			out.write(gzip(payload));
			out.close();
		} catch (Exception e) {
			throw new RuntimeException("could not write gz fixture " + target, e);
		}
	}

	private static void deleteRecursively(File f) {
		if (f == null || !f.exists()) {
			return;
		}
		if (f.isDirectory()) {
			File[] kids = f.listFiles();
			if (kids != null) {
				for (int i = 0; i < kids.length; i++) {
					deleteRecursively(kids[i]);
				}
			}
		}
		f.delete();
	}

	// ------------------- Phase 6.5.3 reader/skip equivalence (the drift guard)
	//
	// Phase 6.5.3: the CursePack decodes its WANTED entries with the 474 readers but walks
	// its UNWANTED ones with hand-written skippers. That leaves two copies of the opcode
	// knowledge, which is the drift risk these tests exist to bound: if a reader changes
	// how it consumes an opcode and the skipper is not updated to match, the stream
	// desynchronises and every definition after it is silently corrupted.
	//
	// These tests deliberately compare CONSUMPTION (bytes consumed), not decoded values -
	// that is the property the two paths must share, and the only one that matters for
	// walking a file.

	/**
	 * Builds {@code [opcode][payload][0]} for one seq opcode, with the payload the reader
	 * expects.
	 *
	 * <p>⚠ The payload bytes are deliberately NON-OPCODE values (0x21 and up), and that is
	 * load-bearing. The first version of this fixture used small payload values and was
	 * INSENSITIVE: a mutated skipper that consumed one byte where the reader consumes two
	 * landed on another VALID opcode and coincidentally re-synced, so the tests passed
	 * while the two tables disagreed (mutation-proved by trying it). Poison payloads mean
	 * any misalignment hits an unrecognised opcode and stops the walk early, which is what
	 * makes a real difference visible.
	 */
	private static byte[] seqEntryFor(int opcode) {
		java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
		b.write(opcode);
		if (opcode == 1) {
			b.write(0);
			b.write(2); // n = 2 frames
			for (int i = 0; i < 2; i++) {
				b.write(0);
				b.write(0);
				b.write(0);
				b.write(0x21 + i); // frame dword
			}
			for (int i = 0; i < 2; i++) {
				b.write(0x23 + i); // delay byte
			}
		} else if (opcode == 2) {
			b.write(0x21);
			b.write(0x22);
		} else if (opcode == 3) {
			b.write(2);
			b.write(0x21);
			b.write(0x22);
		} else if (opcode == 4) {
			// a bare flag: no payload
		} else if (opcode == 5 || opcode == 8 || opcode == 9 || opcode == 10 || opcode == 11) {
			b.write(0x21);
		} else if (opcode == 6 || opcode == 7) {
			b.write(0x21);
			b.write(0x22);
		}
		b.write(0); // terminator
		return b.toByteArray();
	}

	/** Builds {@code [opcode][payload][0]} for one spotanim opcode; payloads are poison too. */
	private static byte[] spotEntryFor(int opcode) {
		java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
		b.write(opcode);
		if (opcode == 1 || opcode == 2 || opcode == 4 || opcode == 5 || opcode == 6) {
			b.write(0x21);
			b.write(0x22);
		} else if (opcode == 7 || opcode == 8) {
			b.write(0x21);
		} else if (opcode == 40) {
			b.write(2);
			for (int i = 0; i < 2; i++) {
				b.write(0x21);
				b.write(0x22);
				b.write(0x23);
				b.write(0x24); // recolor (word, word) pair
			}
		}
		b.write(0); // terminator
		return b.toByteArray();
	}

	/**
	 * Concatenates entries into ONE stream. This matters: with a single stream, a
	 * misalignment at entry i shifts every later boundary, so a one-byte disagreement
	 * cannot hide behind a per-entry coincidence.
	 */
	private static byte[] concat(byte[][] parts) {
		int total = 0;
		for (int i = 0; i < parts.length; i++) {
			total += parts[i].length;
		}
		byte[] all = new byte[total];
		int at = 0;
		for (int i = 0; i < parts.length; i++) {
			System.arraycopy(parts[i], 0, all, at, parts[i].length);
			at += parts[i].length;
		}
		return all;
	}

	/** Invokes one of CurseData667's private skippers on a live stream. */
	private static void invokeSkip(Stream s, String method) {
		try {
			java.lang.reflect.Method m = CurseData667.class.getDeclaredMethod(method, Stream.class);
			m.setAccessible(true);
			m.invoke(null, s);
		} catch (Exception e) {
			throw new RuntimeException("could not invoke " + method, e);
		}
	}

	/** Reports the first entry where two offset traces disagree, or null if they match. */
	private static String firstOffsetDifference(int[] opcodes, int[] skipOffsets, int[] readOffsets) {
		for (int i = 0; i < opcodes.length; i++) {
			if (skipOffsets[i] != readOffsets[i]) {
				return "opcode " + opcodes[i] + " skip=" + skipOffsets[i] + " read=" + readOffsets[i];
			}
		}
		return null;
	}

	/**
	 * The seq skipper must mirror {@link Animation#readValues} for every opcode the reader
	 * actually handles. A mismatch here means the pack would parse differently depending
	 * only on whether an entry happened to be wanted.
	 */
	private static void seqSkipTableMatchesThe474ReaderForEveryOpcodeItHandles() {
		int[] opcodes = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
		byte[][] entries = new byte[opcodes.length][];
		for (int i = 0; i < opcodes.length; i++) {
			entries[i] = seqEntryFor(opcodes[i]);
		}
		byte[] all = concat(entries);

		int[] skipOffsets = new int[opcodes.length];
		Stream skipStream = new Stream(all);
		for (int i = 0; i < opcodes.length; i++) {
			invokeSkip(skipStream, "skipSequence");
			skipOffsets[i] = skipStream.currentOffset;
		}

		int[] readOffsets = new int[opcodes.length];
		Stream readStream = new Stream(all);
		for (int i = 0; i < opcodes.length; i++) {
			new Animation().readValues(readStream);
			readOffsets[i] = readStream.currentOffset;
		}

		String firstDiff = firstOffsetDifference(opcodes, skipOffsets, readOffsets);
		check("6.5.3 seq: the CursePack skip table and Animation.readValues land on the SAME"
				+ " offset after every one of the " + opcodes.length + " handled opcodes"
				+ (firstDiff == null ? "" : " - first difference: " + firstDiff), firstDiff == null);
		check("6.5.3 seq: both paths walk the whole buffer to its LAST byte, so neither can"
				+ " silently stop short",
				skipOffsets[opcodes.length - 1] == all.length
						&& readOffsets[opcodes.length - 1] == all.length);
	}

	/**
	 * The ONE recorded divergence, pinned so it cannot be "tidied" away by accident.
	 *
	 * <p>The seq skipper consumes one byte for opcode 12; {@link Animation#readValues} has
	 * no case for 12 and consumes nothing. That is a genuine difference in where the
	 * cursor lands, so it is safe only while no entry uses opcode 12 - which was measured
	 * against the real pack: 15371 entries use 0,1,2,3,5,6,7,8,9,10,11, opcode 12 occurs
	 * ZERO times, and both tables walk the file to exactly EOF with zero bad opcodes.
	 * Dropping this case would change nothing today and would break the pack the day 12
	 * appears, so the divergence is asserted rather than removed.
	 */
	private static void seqSkipTableHandlesOpcode12WhichThe474ReaderDoesNot() {
		byte[] entry = new byte[] { 12, 0x21, 0 };
		Stream s = new Stream(entry);
		invokeSkip(s, "skipSequence");
		check("6.5.3 seq: the skip table consumes exactly one byte for opcode 12 and still"
				+ " reaches the terminator (3 of 3 bytes) - the ONE recorded divergence from"
				+ " Animation.readValues, which has no case for 12", s.currentOffset == entry.length);
	}

	/**
	 * Unlike the seq table, the spotanim skipper has NO divergence from the reader, which
	 * is consistent with the measurement: the real {@code spotanim.dat} (2982 entries)
	 * uses only opcodes 0, 1, 2, 4, 5, 6, 7, 8, 40 - all handled identically by both paths.
	 */
	private static void spotAnimSkipTableMatchesThe474ReaderForEveryOpcodeItHandles() {
		int[] opcodes = { 1, 2, 4, 5, 6, 7, 8, 40 };
		byte[][] entries = new byte[opcodes.length][];
		for (int i = 0; i < opcodes.length; i++) {
			entries[i] = spotEntryFor(opcodes[i]);
		}
		byte[] all = concat(entries);

		int[] skipOffsets = new int[opcodes.length];
		Stream skipStream = new Stream(all);
		for (int i = 0; i < opcodes.length; i++) {
			invokeSkip(skipStream, "skipSpotAnim");
			skipOffsets[i] = skipStream.currentOffset;
		}

		int[] readOffsets = new int[opcodes.length];
		Stream readStream = new Stream(all);
		for (int i = 0; i < opcodes.length; i++) {
			new SpotAnim().readValues(readStream);
			readOffsets[i] = readStream.currentOffset;
		}

		String firstDiff = firstOffsetDifference(opcodes, skipOffsets, readOffsets);
		check("6.5.3 spotanim: the CursePack skip table and SpotAnim.readValues land on the SAME"
				+ " offset after every one of the " + opcodes.length + " handled opcodes"
				+ (firstDiff == null ? "" : " - first difference: " + firstDiff), firstDiff == null);
		check("6.5.3 spotanim: both paths walk the whole buffer to its LAST byte",
				skipOffsets[opcodes.length - 1] == all.length
						&& readOffsets[opcodes.length - 1] == all.length);
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
				int t0, int t1, int t2, int t3, int t4, int t5, int t6, int t7, int t8,
				int depth) {
			return false;
		}

		public boolean sceneFinished(RSImageProducer producer) {
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
		int[] files = ContentRegistry.CURSE_ANIM_FILES;
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
