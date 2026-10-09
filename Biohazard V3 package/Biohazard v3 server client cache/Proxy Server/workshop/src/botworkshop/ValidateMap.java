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
 * <p><b>What is actually compared.</b> Collision, not just file integrity:
 *
 * <ol>
 * <li><b>Every tile's clip bitmask, exactly.</b> The export carries {@code Region.getClipping} for
 *     all four planes, and the validator re-reads it out of the JSON and compares it bit for bit.
 *     This is the check that makes the editor's clipping overlay trustworthy: it cannot show a tile
 *     as walkable that the server blocks without this failing.
 * <li>{@code Region.loadMaps} blocks a tile when bit 0 of its flags is set, adding the
 *     {@code 0x200000} walk-block bit. The validator decodes the exported flags back into a grid,
 *     applies the same plane rule the server applies (a tile in plane 1 with bit 1 set belongs to the
 *     plane below), and asks {@code Region.getClipping} whether that tile really is blocked.
 * <li>A mismatch in either means the decoder in {@code GroundMap} reads the map differently from the
 *     server — the one failure an export can have that no amount of unit testing against the file
 *     would catch, because both sides would be reading the same wrong thing.
 * </ol>
 *
 * <p>Exit code is non-zero when anything mismatches, so this can gate a commit.
 */
public final class ValidateMap {

	/**
	 * The bit {@code Region.loadMaps} sets on a tile whose terrain flags have bit 0, and which a
	 * placed type-22 object can set too.
	 *
	 * <p>It is a <b>walk-block</b>, not "there is a floor here". Two things in the server say so:
	 * {@code Region.addObject} applies it to a type-22 object only when that object has actions and
	 * blocks walk, and it is inside {@code Region.BLOCKED}, the value used to fail closed on a region
	 * with no collision data — including it in a blocked mask would be meaningless if it meant the
	 * opposite. It is also inside every {@code SmartPathFinder} walk mask.
	 *
	 * <p>Because terrain and objects share the one bit, the comparison below only holds one way: a
	 * tile the export calls blocked must have the bit, but a tile that has the bit may owe it to an
	 * object rather than to terrain. That is what keeps this test immune to object-side changes — the
	 * {@code ObjectDef} terminator fix moved 307 object clip values across the three landmark regions
	 * and this count stayed at zero, which is the expected outcome and not a sign the check is inert.
	 */
	private static final int BLOCKED_BIT = 0x200000;

	/**
	 * Anchored on the plane object's field order, which {@code RegionDocumentTest} pins.
	 *
	 * <p>{@code clip} is captured rather than skipped because it is the one plane whose exact value
	 * the validator can compare: it is {@code Region.getClipping} written down, so any difference is
	 * a real disagreement between the export and the server.
	 */
	private static final Pattern PLANE_BLOCK = Pattern.compile(
			"\\{\\s*\"plane\":\\s*(\\d+),\\s*\"overlay\":\\s*\"([^\"]*)\",\\s*\"underlay\":\\s*\"([^\"]*)\","
					+ "\\s*\"flags\":\\s*\"([^\"]*)\",\\s*\"clip\":\\s*\"([^\"]*)\"\\s*\\}");

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
		List<String> skipped = new ArrayList<String>();
		try (Stream<Path> stream = Files.list(outDir)) {
			stream.filter(p -> p.getFileName().toString().endsWith(".json"))
					.sorted()
					.forEach(p -> {
						// Region documents are named after their region id. Anything else in this
						// directory (banks.json, and later whatever else the exporter adds) is not a
						// region, and guessing that from "not index.json" would silently feed a
						// non-region file to the region parser.
						String name = p.getFileName().toString();
						String stem = name.substring(0, name.length() - ".json".length());
						if (stem.chars().allMatch(Character::isDigit) && !stem.isEmpty()) {
							files.add(p);
						} else {
							skipped.add(name);
						}
					});
		}
		System.out.println("[validate] " + files.size() + " region document(s) in " + outDir);
		if (!skipped.isEmpty()) {
			System.out.println("[validate] not region documents, skipped: " + String.join(", ", skipped));
		}

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
		int readerDisagreements = reportLocReaderDivergence();
		boolean ok = mismatches == 0 && badDocuments == 0 && readerDisagreements == 0;
		System.out.println("[validate] " + (ok
				? "OK — exported terrain agrees with Region.getClipping and ObjectDef agrees with loc.dat"
				: "FAILED — the tool and the server disagree"));
		if (!ok) {
			System.exit(1);
		}
	}

	/**
	 * Checks whether the server's own {@code ObjectDef} still agrees with this tool's decoder.
	 *
	 * <p>{@code readValues} used to terminate {@code loc.dat}'s strings on {@code 0x0A} while the
	 * file terminates them on {@code 0x00}. Every named object then failed to parse, and because
	 * {@code getObjectDef} catches that and calls {@code setDefaults()}, the server answered "no
	 * name, no actions, blocks walk" for <b>19410 of 19410</b> named objects — while the client's
	 * {@code 0x00} reader saw the real values, so the two disagreed about the name, the actions and
	 * the walkability of every named object.
	 *
	 * <p>That is fixed, which turns this from a description into a guard. It compares every entry on
	 * name, ordered actions, footprint and the walk-blocking flag, and a disagreement now fails the
	 * run: the two readers disagreeing about walkability is a collision bug, not a cosmetic one.
	 *
	 * @return the number of entries the two readers disagree about
	 */
	private static int reportLocReaderDivergence() throws IOException {
		Path data = Paths.get("./Data");
		botworkshop.data.LocDefs defs = botworkshop.data.LocDefs.load(
				data.resolve("world/object/loc.dat"), data.resolve("world/object/loc.idx"));
		int compared = 0;
		int named = 0;
		int nameDiffers = 0;
		int actionsDiffer = 0;
		int footprintDiffers = 0;
		int blockingDiffers = 0;
		for (int id = 0; id < defs.count(); id++) {
			botworkshop.data.LocDefinition ours = defs.get(id);
			if (ours == null || !ours.parsed()) {
				continue;
			}
			ObjectDef theirs = ObjectDef.getObjectDef(id);
			if (theirs == null) {
				continue;
			}
			compared++;
			if (ours.name() != null) {
				named++;
				if (!ours.name().equals(theirs.name)) {
					nameDiffers++;
				}
			}
			if (!ours.actions().equals(serverActions(theirs))) {
				actionsDiffer++;
			}
			if (ours.sizeX() != theirs.anInt744 || ours.sizeY() != theirs.anInt761) {
				footprintDiffers++;
			}
			if (ours.blocksWalk() != theirs.aBoolean767()) {
				blockingDiffers++;
			}
		}
		System.out.println("[validate] loc.dat entries compared = " + compared);
		System.out.println("[validate]   named objects          = " + named);
		System.out.println("[validate]   name differs           = " + nameDiffers);
		System.out.println("[validate]   actions differ         = " + actionsDiffer);
		System.out.println("[validate]   footprint differs      = " + footprintDiffers);
		System.out.println("[validate]   walk-blocking differs  = " + blockingDiffers);
		return nameDiffers + actionsDiffer + footprintDiffers + blockingDiffers;
	}

	/** The server's action slots in order, with the null and empty holes removed. */
	private static List<String> serverActions(ObjectDef def) {
		List<String> out = new ArrayList<String>();
		if (def.actions != null) {
			for (String action : def.actions) {
				if (action != null && !action.isEmpty()) {
					out.add(action);
				}
			}
		}
		return out;
	}

	/** @return the number of tiles that disagree, or {@code -1} when the document cannot be read. */
	private static int validateRegion(int regionId, Path file) throws IOException {
		String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
		int baseX = (regionId >> 8) * 64;
		int baseY = (regionId & 0xff) * 64;

		// flagsByPlane[plane] indexed localX * 64 + localY, the same order the exporter wrote.
		int[][] flagsByPlane = new int[GroundMap.planes()][GroundMap.size() * GroundMap.size()];
		int[][] clipByPlane = new int[GroundMap.planes()][GroundMap.size() * GroundMap.size()];
		boolean[] seen = new boolean[GroundMap.planes()];

		Matcher matcher = PLANE_BLOCK.matcher(json);
		while (matcher.find()) {
			int plane = Integer.parseInt(matcher.group(1));
			if (plane < 0 || plane >= GroundMap.planes()) {
				throw new IllegalStateException("plane " + plane + " out of range");
			}
			flagsByPlane[plane] = Rle.decode(matcher.group(4), GroundMap.size() * GroundMap.size());
			clipByPlane[plane] = Rle.decode(matcher.group(5), GroundMap.size() * GroundMap.size());
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
					int tileX = baseX + localX;
					int tileY = baseY + localY;

					// The exact check, and the strongest one available: every bit of the exported
					// clip grid must equal the server's own bitmask for that tile and plane. The
					// editor draws this grid as its clipping overlay, so a difference here would be
					// the tool showing a tile as walkable that the server refuses to walk.
					int serverClip = Region.getClipping(tileX, tileY, plane);
					if (clipByPlane[plane][at] != serverClip) {
						if (mismatches < 5) {
							System.out.println("[validate] " + regionId + " tile " + tileX + "," + tileY
									+ " plane " + plane + ": exported clip " + clipByPlane[plane][at]
									+ ", server clip " + serverClip);
						}
						mismatches++;
					}

					if ((flagsByPlane[plane][at] & 1) != 1) {
						continue;
					}
					// The plane rule Region.loadMaps uses: a tile whose plane-1 flags have bit 1
					// set belongs to the plane below, whatever plane we are reading. Note this is a
					// different plane from the clip comparison above, deliberately: the clip grid is
					// queried by the plane as stored, because that is where the server put the bit.
					int height = plane;
					if ((flagsByPlane[1][at] & 2) == 2) {
						height--;
					}
					if (height < 0 || height > 3) {
						continue;
					}
					// Separate from the clip check because it asserts something weaker and only
					// about blocked tiles; keeping it means a terrain-decoder disagreement still
					// reports as a terrain disagreement rather than as a clip mismatch.
					int clip = Region.getClipping(tileX, tileY, height);
					if ((clip & BLOCKED_BIT) == 0) {
						if (mismatches < 5) {
							System.out.println("[validate] " + regionId + " tile " + tileX + ","
									+ tileY + " plane " + height
									+ ": exported flags say blocked, server clip " + clip + " has no 0x200000");
						}
						mismatches++;
					}
				}
			}
		}
		return mismatches;
	}
}
