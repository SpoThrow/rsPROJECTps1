import java.io.File;
import java.io.FileInputStream;

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
 * <p>Run with {@code RunTests.bat}. Classes come from {@code bin/}, so this tests the compiled
 * client, not a copy of the source.
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
		packetTapProducesDiffableLog();

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
