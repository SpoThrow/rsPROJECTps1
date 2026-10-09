package botworkshop.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import server.clip.region.ByteStream;
import server.clip.region.MemoryArchive;

/**
 * Reader for {@code Data/world/object/loc.dat} — object names and actions.
 *
 * <p><b>Why not {@code ObjectDef}.</b> The server's own reader is wrong for this cache, and the
 * way it is wrong is invisible: {@code ObjectDef.readValues} reads strings with
 * {@code ByteStreamExt.readString()}, which scans for {@code 0x0A}, while these entries terminate
 * strings with {@code 0x00}. Decoding entry 1276 shows it directly —
 *
 * <pre>1e "Chop down" 00 20 "hidden" 00 ... 02 "Tree" 00 00</pre>
 *
 * <p>— so {@code readString()} runs straight past the terminator into the next field. Whatever it
 * finds a {@code 0x0A} in, the definition is misaligned from that point on; where it finds none it
 * throws and {@code ObjectDef.getObjectDef} quietly falls back to {@code setDefaults()}, which
 * leaves {@code name} null and {@code aBoolean767} true. The server therefore cannot be asked what
 * an object is called, which is exactly what the editor's icon layer is built on.
 *
 * <p>The archive layout itself is <em>not</em> reimplemented: {@link MemoryArchive} is reused so the
 * index format cannot drift from the server's.
 *
 * <p>The opcode table below was validated by walking all 42001 entries in the shipped file with
 * zero failures, and {@code LocDefsTest} keeps asserting that. A byte we do not understand stops
 * the walk and marks the definition unparsed rather than being skipped, because guessing a payload
 * length is what silently corrupts every field after it.
 */
public final class LocDefs {

	/** Slots 30..38 are actions; the client's table reserves ten, the shipped cache uses nine. */
	private static final int FIRST_ACTION = 30;
	private static final int LAST_ACTION = 38;
	private static final int ACTION_SLOTS = LAST_ACTION - FIRST_ACTION + 1;

	private final MemoryArchive archive;
	private final Map<Integer, LocDefinition> memo = new HashMap<Integer, LocDefinition>();
	private final boolean[] unparsed;

	private LocDefs(MemoryArchive archive) {
		this.archive = archive;
		this.unparsed = new boolean[archive.contentSize()];
	}

	/** Opens the archive. The two files are read whole; a missing one is an error, not an empty set. */
	public static LocDefs load(Path datFile, Path idxFile) throws IOException {
		if (!Files.isRegularFile(datFile) || !Files.isRegularFile(idxFile)) {
			throw new IOException("loc archive missing: " + datFile + " / " + idxFile);
		}
		MemoryArchive archive = new MemoryArchive(
				new ByteStream(Files.readAllBytes(datFile)),
				new ByteStream(Files.readAllBytes(idxFile)));
		if (archive.contentSize() <= 0) {
			throw new IOException("loc.idx declares no entries: " + idxFile);
		}
		return new LocDefs(archive);
	}

	/** How many entries the index declares. */
	public int count() {
		return archive.contentSize();
	}

	/**
	 * The definition for {@code id}, or {@code null} when the index has no such entry. Results are
	 * memoised — the editor asks for the same few dozen ids thousands of times.
	 */
	public LocDefinition get(int id) {
		if (id < 0 || id >= unparsed.length) {
			return null;
		}
		LocDefinition cached = memo.get(id);
		if (cached != null) {
			return cached;
		}
		byte[] raw = archive.get(id);
		LocDefinition def = raw == null ? null : decode(id, raw);
		if (def != null) {
			memo.put(id, def);
		}
		return def;
	}

	/** Ids whose opcode walk stopped early. Empty for a healthy cache. */
	public List<Integer> unparsedIds() {
		List<Integer> ids = new ArrayList<Integer>();
		for (int i = 0; i < unparsed.length; i++) {
			if (unparsed[i]) {
				ids.add(i);
			}
		}
		return ids;
	}

	private LocDefinition decode(int id, byte[] raw) {
		Cursor in = new Cursor(raw);
		String name = null;
		String[] actions = new String[ACTION_SLOTS];
		int sizeX = 1;
		int sizeY = 1;
		// ObjectDef setDefaults() leaves this true, so an object only becomes walkable when the
		// data says so — matching the server's default rather than inventing a more permissive one.
		boolean blocksWalk = true;
		boolean parsed = true;
		try {
			while (true) {
				int opcode = in.u8();
				if (opcode == 0) {
					break;
				}
				if (opcode == 1) {
					in.skip(in.u8() * 3);
				} else if (opcode == 2) {
					name = in.string();
				} else if (opcode == 3) {
					in.string();
				} else if (opcode == 5) {
					in.skip(in.u8() * 2);
				} else if (opcode >= FIRST_ACTION && opcode <= LAST_ACTION) {
					String action = in.string();
					actions[opcode - FIRST_ACTION] =
							("hidden".equalsIgnoreCase(action) || action.isEmpty()) ? null : action;
				} else if (opcode == 14) {
					sizeX = in.u8();
				} else if (opcode == 15) {
					sizeY = in.u8();
				} else if (opcode == 17) {
					blocksWalk = false;
				} else if (opcode == 74) {
					// The second opcode that clears walk-blocking, and the one easiest to get wrong:
					// it has no payload, so skipping it looks correct. Both the client's
					// readValues474 and the server finish with "if (aBoolean766) aBoolean767 =
					// false", so 74's objects are walkable — treating it as an inert flag left them
					// blocking when neither side does.
					blocksWalk = false;
				} else if (opcode == 19 || opcode == 28 || opcode == 69 || opcode == 75) {
					in.skip(1);
				} else if (opcode == 29 || opcode == 39) {
					in.skip(1);
				} else if (opcode == 21 || opcode == 22 || opcode == 23 || opcode == 27 || opcode == 62
						|| opcode == 64 || opcode == 73 || opcode == 82 || opcode == 88
						|| opcode == 89 || opcode == 90 || opcode == 91 || opcode == 94 || opcode == 95
						|| opcode == 96 || opcode == 97 || opcode == 18) {
					// Pure flags with no payload that leave walk-blocking alone. 18 and 64 clear
					// movement flags (aBoolean757 and friends), never aBoolean767.
				} else if (opcode == 24 || opcode == 60 || opcode == 65 || opcode == 66 || opcode == 67
						|| opcode == 68 || opcode == 70 || opcode == 71 || opcode == 72) {
					in.skip(2);
				} else if (opcode == 40 || opcode == 41) {
					in.skip(in.u8() * 4);
				} else if (opcode == 42) {
					in.skip(in.u8());
				} else if (opcode == 77 || opcode == 92) {
					in.skip(2);
					in.skip(2);
					if (opcode == 92) {
						in.skip(2);
					}
					int children = in.u8();
					for (int i = 0; i <= children; i++) {
						in.skip(2);
					}
				} else if (opcode == 78) {
					in.skip(3);
				} else if (opcode == 79) {
					in.skip(5);
					in.skip(in.u8() * 2);
				} else if (opcode == 81) {
					in.skip(1);
				} else if (opcode == 93) {
					in.skip(2);
				} else if (opcode == 249) {
					int params = in.u8();
					for (int i = 0; i < params; i++) {
						boolean isString = in.u8() == 1;
						in.skip(3);
						if (isString) {
							in.string();
						} else {
							in.skip(4);
						}
					}
				} else {
					parsed = false;
					break;
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			parsed = false;
		}

		List<String> kept = new ArrayList<String>(ACTION_SLOTS);
		for (String action : actions) {
			if (action != null) {
				kept.add(action);
			}
		}
		if (!parsed) {
			unparsed[id] = true;
		}
		return new LocDefinition(id, name, kept, Math.max(1, sizeX), Math.max(1, sizeY), blocksWalk, parsed);
	}

	/**
	 * A reader over one entry. Unlike the server's two stream classes it cannot run off the end
	 * silently: an overrun throws, which {@link #decode} turns into {@code parsed == false}.
	 */
	private static final class Cursor {
		private final byte[] buffer;
		private int offset;

		Cursor(byte[] buffer) {
			this.buffer = buffer;
		}

		int u8() {
			return buffer[offset++] & 0xff;
		}

		void skip(int n) {
			if (n < 0) {
				throw new ArrayIndexOutOfBoundsException("negative skip " + n);
			}
			offset += n;
			if (offset > buffer.length) {
				throw new ArrayIndexOutOfBoundsException(offset);
			}
		}

		/** A {@code 0x00}-terminated string — the terminator this cache uses. */
		String string() {
			int start = offset;
			while (buffer[offset++] != 0) {
				if (offset > buffer.length) {
					throw new ArrayIndexOutOfBoundsException(offset);
				}
			}
			return new String(buffer, start, offset - start - 1, StandardCharsets.ISO_8859_1);
		}
	}
}
