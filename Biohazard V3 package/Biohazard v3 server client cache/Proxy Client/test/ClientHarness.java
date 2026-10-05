import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import cache.StreamLoader;
import def.EntityDef;
import def.Flo;
import def.IDK;
import def.ItemDef;
import def.ObjectDef;
import def.VarBit;
import def.Varp;
import model.Model;
import model.Texture;
import net.PacketTap;
import net.Stream;
import scene.CollisionMap;
import scene.Fog;
import scene.ObjectManager;
import scene.WorldController;
import ui.DrawingArea;
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
		}		boolean ok = TEXTURED_GOLDEN_HASH.equals(actual);
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
	 */
	private static void sceneRasterizerInterceptsWhenInstalled() {
		final int[] calls = new int[1];
		SceneRasterizer.install(new SceneRasterizer.Implementation() {
			public void drawModel(Model model, int orientation, int camA, int camB, int camC,
					int camD, int dx, int dy, int dz, int uid) {
				calls[0]++;
			}
		});
		try {
			Model.aModel_1621.method443(0, 0, 0, 0, 0, 0, 0, 0, 0);
			check("Scene rasteriser: an installed rasteriser intercepts method443",
					calls[0] == 1);
		} finally {
			SceneRasterizer.install(null);
		}
		check("Scene rasteriser: uninstalling restores the software path",
				SceneRasterizer.implementation() == null);
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
