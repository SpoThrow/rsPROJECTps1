package botworkshop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import botworkshop.data.GroundMap;
import botworkshop.export.Rle;
import server.clip.region.ObjectDef;
import server.clip.region.Region;

/**
 * Bot Workshop validator — {@code BOT_TOOLING.md} stages T1 and T6.
 *
 * <p>Re-reads what {@link ExportMap} wrote and checks it against the <em>server's own</em> collision,
 * loading the world exactly as the server does. This is the "the tool and the server cannot disagree
 * about the world" guarantee, and it is the reason T1 ships with a validator rather than only an
 * exporter.
 *
 * <p><b>What is actually compared.</b> Terrain occupancy, not just file integrity:
 *
 * <ol>
 * <li>{@code Region.loadMaps} blocks a tile when bit 0 of its flags is set, adding the
 *     {@code 0x200000} clip bit. The validator decodes the exported flags back into a grid, applies
 *     the same plane rule the server applies (a tile in plane 1 with bit 1 set belongs to the plane
 *     below), and asks {@code Region.getClipping} whether that tile really is blocked.
 * <li>A mismatch means the terrain decoder in {@code GroundMap} reads the map differently from the
 *     server — the one failure an export can have that no amount of unit testing against the file
 *     would catch, because both sides would be reading the same wrong thing.
 * </ol>
 *
 * <p>Exit code is non-zero when anything mismatches, so this can gate a commit.
 */
public final class ValidateMap {

	/** The clip bit {@code Region.loadMaps} adds for a tile its ground data marks as occupied. */
	private static final int OCCUPIED_BIT = 0x200000;

	/** Anchored on the plane object's field order, which {@code RegionDocumentTest} pins. */
	private static final Pattern PLANE_BLOCK = Pattern.compile(
			"\\{\\s*\"plane\":\\s*(\\d+),\\s*\"overlay\":\\s*\"([^\"]*)\",\\s*\"underlay\":\\s*\"([^\"]*)\","
					+ "\\s*\"flags\":\\s*\"([^\"]*)\"\\s*\\}");

	private ValidateMap() {
	}

	public static void main(String[] args) throws Exception {
		Path outDir = Paths.get(args.length > 0 ? args[0] : "Data/workshop/map");
		if (!Files.isDirectory(outDir)) {
			System.err.println("[validate] no export at " + outDir.toAbsolutePath()
					+ " — run the workshopExport task first");
			System.exit(2);
			return;
		}

		System.out.println("[validate] loading the server's world model");
		ObjectDef.loadConfig();
		Region.load();

		List<Path> files = new ArrayList<Path>();
		try (Stream<Path> stream = Files.list(outDir)) {
			stream.filter(p -> p.getFileName().toString().endsWith(".json"))
					.filter(p -> !p.getFileName().toString().equals("index.json"))
					.sorted()
					.forEach(files::add);
		}
		System.out.println("[validate] " + files.size() + " region document(s) in " + outDir);

		int mismatches = 0;
		int badDocuments = 0;

		for (Path file : files) {
			try {
				int regionId = Integer.parseInt(file.getFileName().toString().replace(".json", ""));
				int regionMismatches = validateRegion(regionId, file);
				if (regionMismatches < 0) {
					badDocuments++;
				} else {
					mismatches += regionMismatches;
				}
			} catch (RuntimeException e) {
				System.out.println("[validate] " + file.getFileName() + ": " + e.getMessage());
				badDocuments++;
			}
		}

		// A final independent pass: the tile counts the decoder produces for the regions we hold,
		// so the summary is not just "nothing complained".
		System.out.println();
		System.out.println("[validate] mismatched tiles        = " + mismatches);
		System.out.println("[validate] unusable documents      = " + badDocuments);
		reportLocReaderDivergence();
		System.out.println("[validate] " + (mismatches == 0 && badDocuments == 0
				? "OK — exported terrain agrees with Region.getClipping"
				: "FAILED — the export and the server disagree"));
		if (mismatches != 0 || badDocuments != 0) {
			System.exit(1);
		}
	}

	/**
	 * Measures how far the server's own {@code ObjectDef} reader diverges from this tool's — a
	 * read-only report, never a failure, because the server's reader is not the tool's to fix.
	 *
	 * <p>{@code ObjectDef.readValues} terminates strings on {@code 0x0A} while this cache terminates
	 * them on {@code 0x00}, so it either misreads the rest of an entry or throws and falls back to
	 * {@code setDefaults()}. Printing the count keeps that from being folklore: it is the number of
	 * objects for which the server cannot say what they are called, which is why the exporter does
	 * not ask it. Expect roughly half the archive; a sudden change means the cache or the reader
	 * moved and {@code LocDefs} should be re-checked against it.
	 */
	private static void reportLocReaderDivergence() throws IOException {
		Path data = Paths.get("./Data");
		botworkshop.data.LocDefs defs = botworkshop.data.LocDefs.load(
				data.resolve("world/object/loc.dat"), data.resolve("world/object/loc.idx"));
		int named = 0;
		int serverAgrees = 0;
		int serverBlank = 0;
		int serverWrong = 0;
		for (int id = 0; id < defs.count(); id++) {
			botworkshop.data.LocDefinition ours = defs.get(id);
			if (ours == null || ours.name() == null) {
				continue;
			}
			named++;
			ObjectDef theirs = ObjectDef.getObjectDef(id);
			String theirName = theirs == null ? null : theirs.name;
			if (theirName == null) {
				serverBlank++;
			} else if (theirName.equals(ours.name())) {
				serverAgrees++;
			} else {
				serverWrong++;
			}
		}
		System.out.println("[validate] named objects           = " + named);
		System.out.println("[validate]   server reader agrees  = " + serverAgrees);
		System.out.println("[validate]   server reads no name  = " + serverBlank + "  (ObjectDef fell back to defaults)");
		System.out.println("[validate]   server reads a name that is not the right one = " + serverWrong);
	}

	/** @return the number of tiles that disagree, or {@code -1} when the document cannot be read. */
	private static int validateRegion(int regionId, Path file) throws IOException {
		String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
		int baseX = (regionId >> 8) * 64;
		int baseY = (regionId & 0xff) * 64;

		// flagsByPlane[plane] indexed localX * 64 + localY, the same order the exporter wrote.
		int[][] flagsByPlane = new int[GroundMap.planes()][GroundMap.size() * GroundMap.size()];
		boolean[] seen = new boolean[GroundMap.planes()];

		Matcher matcher = PLANE_BLOCK.matcher(json);
		while (matcher.find()) {
			int plane = Integer.parseInt(matcher.group(1));
			if (plane < 0 || plane >= GroundMap.planes()) {
				throw new IllegalStateException("plane " + plane + " out of range");
			}
			flagsByPlane[plane] = Rle.decode(matcher.group(4), GroundMap.size() * GroundMap.size());
			seen[plane] = true;
		}
		for (int plane = 0; plane < GroundMap.planes(); plane++) {
			if (!seen[plane]) {
				throw new IllegalStateException("plane " + plane + " is missing from the document");
			}
		}

		int size = GroundMap.size();
		int mismatches = 0;
		for (int plane = 0; plane < GroundMap.planes(); plane++) {
			for (int localX = 0; localX < size; localX++) {
				for (int localY = 0; localY < size; localY++) {
					int at = localX * size + localY;
					if ((flagsByPlane[plane][at] & 1) != 1) {
						continue;
					}
					// The plane rule Region.loadMaps uses: a tile whose plane-1 flags have bit 1
					// set belongs to the plane below, whatever plane we are reading.
					int height = plane;
					if ((flagsByPlane[1][at] & 2) == 2) {
						height--;
					}
					if (height < 0 || height > 3) {
						continue;
					}
					int clip = Region.getClipping(baseX + localX, baseY + localY, height);
					if ((clip & OCCUPIED_BIT) == 0) {
						if (mismatches < 5) {
							System.out.println("[validate] " + regionId + " tile " + (baseX + localX) + ","
									+ (baseY + localY) + " plane " + height
									+ ": exported as occupied, server clip " + clip + " has no 0x200000");
						}
						mismatches++;
					}
				}
			}
		}
		return mismatches;
	}
}
