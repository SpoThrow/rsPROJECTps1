package botworkshop.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
/**
 * The world's region directory: {@code Data/world/map_index}.
 *
 * <p>This is the same file {@code server.clip.region.Region.load()} reads, parsed with the same
 * layout — a flat array of 6-byte records, {@code regionId}, {@code groundFileId},
 * {@code objectFileId} as three unsigned shorts, in that order. Parsing it here rather than
 * asking the server for a region list is deliberate: {@code Region} exposes lookups by
 * coordinate but no enumeration, and the exporter has to walk every region.
 *
 * <p>The ids are the map file numbers under {@code Data/world/map/} (the {@code N.gz} names),
 * <em>not</em> region ids — they only happen to coincide for some regions.
 */
public final class MapIndex {

	/** One region and the two map files that hold its ground and its objects. */
	public static final class Entry {
		public final int regionId;
		public final int groundFileId;
		public final int objectFileId;

		Entry(int regionId, int groundFileId, int objectFileId) {
			this.regionId = regionId;
			this.groundFileId = groundFileId;
			this.objectFileId = objectFileId;
		}

	/**
	 * The ground and object files for this region, or {@code null} when the map ships without them.
	 *
	 * <p>51 of the 1226 regions are in this state — the directory lists them but the files are
	 * absent — and it is not an error: {@code Region.load} skips exactly those and reports
	 * "51 of 1226 region(s) have no map data" at boot. The exporter has to skip them too, or it
	 * would fail on a world the server itself boots happily on.
	 */
	public Path[] mapFiles(Path mapDir) {
		Path ground = mapDir.resolve(groundFileId + ".gz");
		Path objects = mapDir.resolve(objectFileId + ".gz");
		if (!Files.isRegularFile(ground) || !Files.isRegularFile(objects)) {
			return null;
		}
		return new Path[] { ground, objects };
	}

	/** World tile of the region's south-west corner: {@code (regionX * 64, regionY * 64)}. */
	public int baseX() {
		return (regionId >> 8) * 64;
	}

		public int baseY() {
			return (regionId & 0xff) * 64;
		}

		@Override
		public String toString() {
			return "region " + regionId + " (" + baseX() + "," + baseY() + ") ground=" + groundFileId
					+ " objects=" + objectFileId;
		}
	}

	private static final int RECORD_SIZE = 6;

	private static final int MAX_ENTRIES = 1 << 16;

	private MapIndex() {
	}

	public static List<Entry> read(Path path) throws IOException {
		return read(Files.readAllBytes(path));
	}

	/**
	 * @throws IOException if the file is not a whole number of 6-byte records, which would mean the
	 *         exporter and the server were reading different files.
	 */
	public static List<Entry> read(byte[] data) throws IOException {
		if (data == null || data.length == 0) {
			throw new IOException("map_index is empty");
		}
		if (data.length % RECORD_SIZE != 0) {
			throw new IOException("map_index is " + data.length + " bytes, not a multiple of "
					+ RECORD_SIZE + " — not a region directory");
		}
		int size = data.length / RECORD_SIZE;
		if (size > MAX_ENTRIES) {
			throw new IOException("map_index declares " + size + " regions, past the 16-bit id space");
		}
		List<Entry> entries = new ArrayList<Entry>(size);
		for (int i = 0; i < size; i++) {
			int p = i * RECORD_SIZE;
			entries.add(new Entry(ushort(data, p), ushort(data, p + 2), ushort(data, p + 4)));
		}
		return entries;
	}

	private static int ushort(byte[] data, int offset) {
		return ((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff);
	}
}
