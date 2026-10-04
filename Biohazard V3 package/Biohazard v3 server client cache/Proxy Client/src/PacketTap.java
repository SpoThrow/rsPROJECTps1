import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Passive packet tap, for the golden-master capture in Phase 0.3 of
 * CLIENT_REFACTORING_PLAN.md.
 *
 * <p><b>Why it exists.</b> Phase 0.3 is the regression oracle for the whole plan: the
 * plan's risk is protocol drift, and no unit test can catch that, because a test of
 * {@code Stream} proves only that the client agrees with itself. The oracle has to be
 * the actual byte sequence the client sends and receives on a real session, captured
 * once now and re-captured after every phase.
 *
 * <p><b>Why it taps {@link RSSocket} and not the TCP stream.</b> TCP chunk boundaries
 * are not reproducible - the same session produces different {@code write()} call
 * sizes on different runs - so a log of raw socket chunks cannot be compared at all.
 * But the client does not consume the stream in TCP chunks: {@code client.java} reads
 * exactly one packet at a time through {@code read()} (opcodes/lengths) and
 * {@code flushInputStream()} (payloads), and queues whole frames through
 * {@code queueBytes()}. Tapping those three methods therefore records the protocol the
 * way the client actually understands it, which <em>is</em> reproducible.
 *
 * <p><b>Reproducible in STRUCTURE, not in bytes - and the difference matters.</b> A
 * byte-for-byte comparison of two sessions is IMPOSSIBLE on this protocol, for reasons
 * that have nothing to do with this tap: every packet opcode after login is
 * ISAAC-encrypted with keys the client seeds from {@code Math.random()} plus the
 * server's 8 random bytes, and the login block embeds those seeds. Two sessions of the
 * same actions therefore share neither opcode bytes nor handshake contents. What they
 * <em>do</em> share is the sequence of directions and lengths, which is what
 * {@code tools/compare-capture.ps1} compares - and a desync is precisely what changes
 * it, because reading a packet in the wrong order consumes a different number of bytes.
 *
 * <p><b>Off unless asked for.</b> Enabled only by the {@value #PROPERTY} system
 * property. With it unset, {@link #enabled()} is false, every record call returns
 * immediately, and the client behaves exactly as before - which is what lets this ship
 * into {@code bin/} without changing anything the user can observe. {@code Record.bat}
 * is the launcher that turns it on.
 *
 * <p><b>Log format</b> - one line per read/write, deliberately minimal and free of
 * timestamps. The absence of timestamps matters for a different reason than byte
 * comparison: a clock or a counter in the file would make even the structural
 * comparison useless, since every line would then differ:
 * <pre>
 * # Soul-Trail packet tap v1
 * # dir len hex            (R = client&lt;-server, W = client-&gt;server)
 * R 1 0a
 * W 3 000401
 * </pre>
 * ⚠️ <b>Flushed on every record on purpose.</b> The capture most likely to matter is
 * the one taken during a crash, and a buffer that dies with the process would lose
 * exactly the packets under investigation.
 */
public final class PacketTap {

	/** Set to a file path to enable, e.g. -Dsoultrail.packettap=golden-master.log */
	private static final String PROPERTY = "soultrail.packettap";

	private static final boolean ENABLED;
	private static final BufferedWriter WRITER;

	/** Set once logging itself fails, so a broken tap cannot spam the game console. */
	private static boolean failed;

	static {
		String path = System.getProperty(PROPERTY);
		BufferedWriter opened = null;
		if (path != null && path.length() > 0) {
			try {
				opened = new BufferedWriter(new FileWriter(path, false), 1 << 16);
				opened.write("# Soul-Trail packet tap v1\n");
				opened.write("# dir len hex            (R = client<-server, W = client->server)\n");
				opened.flush();
			} catch (IOException e) {
				System.out.println("PacketTap: cannot write to " + path + " (" + e + "); tap disabled");
				opened = null;
			}
		}
		WRITER = opened;
		ENABLED = opened != null;
	}

	public static boolean enabled() {
		return ENABLED;
	}

	/** Records bytes the client just read from the server. */
	public static void incoming(byte[] data, int offset, int length) {
		record('R', data, offset, length);
	}

	/** Records bytes the client is about to send to the server. */
	public static void outgoing(byte[] data, int offset, int length) {
		record('W', data, offset, length);
	}

	private static void record(char direction, byte[] data, int offset, int length) {
		if (!ENABLED || failed || data == null || length <= 0) {
			return;
		}
		try {
			StringBuilder line = new StringBuilder(length * 2 + 16);
			line.append(direction).append(' ').append(length).append(' ');
			for (int i = 0; i < length; i++) {
				int b = data[offset + i] & 0xff;
				if (b < 0x10) {
					line.append('0');
				}
				line.append(Integer.toHexString(b));
			}
			line.append('\n');
			// Both directions can be tapped from different threads, so serialise.
			synchronized (PacketTap.class) {
				WRITER.write(line.toString());
				WRITER.flush();
			}
		} catch (Throwable t) {
			// Logging must never be able to break the game.
			failed = true;
			System.out.println("PacketTap: logging failed (" + t + "); tap disabled");
		}
	}

	private PacketTap() {
	}
}
