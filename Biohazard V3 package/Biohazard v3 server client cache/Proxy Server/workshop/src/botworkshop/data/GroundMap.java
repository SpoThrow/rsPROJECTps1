package botworkshop.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

/**
 * Decoder for one landscape file, {@code Data/world/map/&lt;groundFileId&gt;.gz}.
 *
 * <p>This is a faithful port of the client's tile reader ({@code ObjectManager.method181}) — the
 * authoritative decoder for this cache. It is written here because the server does not keep the
 * result: {@code Region.loadMaps} runs the same loop against the same bytes but stores only the
 * bridge bit of the tile flags, so every overlay and underlay id is read and thrown away. The
 * editor needs them to draw the map.
 *
 * <p>Per tile the stream is a sequence of opcodes, terminated by 0 or 1:
 *
 * <pre>
 *   0        end of tile, height is derived (the client computes it procedurally)
 *   1, h     end of tile, explicit height h*8 (with h == 1 normalised to 0)
 *   2..49    an overlay follows: (opcode - 2) / 4 is the shape, ((opcode - 2) + rotation) &amp; 3
 *            the rotation, then one signed byte holding the overlay floor id
 *   50..81   tile flags, opcode - 49  (this is the only part the server keeps)
 *   82+      underlay floor id, opcode - 81
 * </pre>
 *
 * <p>Tile order is plane, then localX, then localY — the same order {@code Region.loadMaps} uses,
 * which is what makes an exported tile line up with a server clip value at the same coordinate.
 */
public final class GroundMap {

	private static final int PLANES = 4;
	private static final int SIZE = 64;

	/** {@code Region.getBuffer} treats anything under this as a missing file, not an empty map. */
	private static final int MIN_VALID_BYTES = 10;

	private final GroundTile[][][] tiles = new GroundTile[PLANES][SIZE][SIZE];

	private GroundMap() {
	}

	/** Reads and gunzips a landscape file. */
	public static GroundMap read(Path gzFile) throws IOException {
		return parse(gunzip(Files.readAllBytes(gzFile), gzFile.toString()));
	}

	static byte[] gunzip(byte[] deflated, String what) throws IOException {
		try (GZIPInputStream gzip = new GZIPInputStream(new java.io.ByteArrayInputStream(deflated))) {
			ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(1024, deflated.length * 4));
			byte[] buffer = new byte[8192];
			int n;
			while ((n = gzip.read(buffer)) != -1) {
				out.write(buffer, 0, n);
			}
			return out.toByteArray();
		} catch (IOException e) {
			throw new IOException("cannot read " + what + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Decodes a decompressed landscape buffer.
	 *
	 * @throws IOException if the buffer ends mid-tile, which would silently shift every later tile
	 *         rather than fail, and so must not be tolerated.
	 */
	public static GroundMap parse(byte[] raw) throws IOException {
		if (raw == null || raw.length < MIN_VALID_BYTES) {
			throw new IOException("landscape buffer is " + (raw == null ? "null" : raw.length + " bytes")
					+ ", under the " + MIN_VALID_BYTES + "-byte floor Region.getBuffer rejects");
		}
		GroundMap map = new GroundMap();
		Cursor in = new Cursor(raw);
		for (int plane = 0; plane < PLANES; plane++) {
			for (int localX = 0; localX < SIZE; localX++) {
				for (int localY = 0; localY < SIZE; localY++) {
					map.tiles[plane][localX][localY] = readTile(in, plane, localX, localY);
				}
			}
		}
		return map;
	}

	private static GroundTile readTile(Cursor in, int plane, int localX, int localY) throws IOException {
		int overlay = 0;
		int underlay = 0;
		int flags = 0;
		int height = GroundTile.HEIGHT_DERIVED;
		while (true) {
			int opcode = in.u8();
			if (opcode == 0) {
				break;
			}
			if (opcode == 1) {
				int h = in.u8();
				height = (h == 1 ? 0 : h) * 8;
				break;
			}
			if (opcode <= 49) {
				// The overlay id the server skips. The client stores it as a signed byte and reads
				// it back masked, so the id is the unsigned value.
				overlay = in.s8() & 0xff;
			} else if (opcode <= 81) {
				flags = opcode - 49;
			} else {
				underlay = opcode - 81;
			}
		}
		return new GroundTile(overlay, underlay, flags, height);
	}

	/** Tile at a plane and region-local coordinate; never {@code null} for a 0..63 input. */
	public GroundTile tile(int plane, int localX, int localY) {
		if (plane < 0 || plane >= PLANES || localX < 0 || localX >= SIZE || localY < 0 || localY >= SIZE) {
			return null;
		}
		return tiles[plane][localX][localY];
	}

	public static int planes() {
		return PLANES;
	}

	public static int size() {
		return SIZE;
	}

	/** A bounds-checked reader; every overrun is reported instead of returning zero-filled bytes. */
	private static final class Cursor {
		private final byte[] buffer;
		private int offset;

		Cursor(byte[] buffer) {
			this.buffer = buffer;
		}

		int u8() throws IOException {
			require(1);
			return buffer[offset++] & 0xff;
		}

		int s8() throws IOException {
			require(1);
			return buffer[offset++];
		}

		private void require(int n) throws IOException {
			if (offset + n > buffer.length) {
				throw new IOException("landscape buffer truncated at byte " + offset + " of "
						+ buffer.length + " — the tile grid does not fit");
			}
		}
	}
}
