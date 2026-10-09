package botworkshop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import botworkshop.classify.ResourceRules;
import botworkshop.data.GroundMap;
import botworkshop.data.LocDefinition;
import botworkshop.data.LocDefs;
import botworkshop.data.MapIndex;
import botworkshop.export.BankIndex;
import botworkshop.export.LocationsDoc;
import botworkshop.export.Placement;
import botworkshop.export.RegionDocument;
import botworkshop.export.WorldDoc;
import server.clip.region.ObjectDef;
import server.clip.region.ObjectSizes;
import server.clip.region.Region;
import server.game.bots.world.LocationsConfig;
import server.game.objects.Objects;

/**
 * Bot Workshop exporter — {@code BOT_TOOLING.md} stage T1.
 *
 * <p>Reads the world the server reads and writes one JSON document per region for the editor's map
 * view. Run from the server directory so {@code ./Data} resolves the way it does at boot:
 *
 * <pre>gradlew workshopExport                 # a contiguous block around Lumbridge
 * gradlew workshopExport -PworkshopArea=3072,3136,4,3   # baseX,baseY,columns,rows
 * gradlew workshopExport -PworkshopRegions=12850,12338
 * gradlew workshopExport -PworkshopRegions=all          # every region the map contains</pre>
 *
 * <p><b>Where each field comes from, and why.</b> The division is deliberate:
 *
 * <ul>
 * <li><b>Objects and clip values</b> come from the <em>server</em> ({@code Region.realObjects} and
 *     {@code Region.getClipping}) after {@code Region.load()}. The exporter never recomputes a
 *     collision value, so what the editor draws cannot disagree with what the server walks on.
 * <li><b>Terrain ids</b> are decoded here, because {@code Region.loadMaps} reads them and discards
 *     them — the overlay and underlay ids exist nowhere in the running server.
 * <li><b>Names and actions</b> are decoded here, because {@code ObjectDef.readValues} terminates
 *     strings on {@code 0x0A} while this cache terminates them on {@code 0x00} (see {@link LocDefs}).
 * <li><b>Footprints</b> come from {@link ObjectSizes}, the same table {@code Region.addObject}
 *     consults, so a size drawn in the editor is the size the server collides with.
 * </ul>
 */
public final class ExportMap {

	/**
	 * The block a bare {@code workshopExport} writes: Draynor, Lumbridge and the castle, as one
	 * contiguous 4x3 area.
	 *
	 * <p>Deliberately a <em>block</em> rather than a list of landmarks. The viewer draws the world
	 * continuously ({@code BOT_TOOLING.md} §4, one map rather than a region picker), and three
	 * scattered regions would leave it showing a
	 * mostly-empty map whose gaps are an artefact of the export rather than a fact about the world.
	 * This is {@code baseX, baseY, columns, rows}, each region 64 tiles.
	 */
	private static final int[] DEFAULT_AREA = { 3072, 3136, 4, 3 };

	private static final String OUTPUT_DIR = "Data/workshop/map";

	private ExportMap() {
	}

	public static void main(String[] args) throws Exception {
		List<String> options = new ArrayList<String>();
		for (String arg : args) {
			if (!arg.isBlank()) {
				options.add(arg.trim());
			}
		}
		boolean all = options.remove("all");

		Path data = Paths.get("./Data");
		Path mapDir = data.resolve("world/map");
		Path outDir = Paths.get(OUTPUT_DIR);

		long started = System.currentTimeMillis();
		System.out.println("[workshop] loading the server's world model");
		// Both are what Server boots with, in the same order: Region.addObject asks ObjectDef for
		// every object it places, so the definitions have to be in memory first or nothing is placed.
		ObjectDef.loadConfig();
		Region.load();
		ObjectSizes sizes = ObjectSizes.get();
		LocDefs defs = LocDefs.load(data.resolve("world/object/loc.dat"),
				data.resolve("world/object/loc.idx"));
		List<MapIndex.Entry> index = MapIndex.read(data.resolve("world/map_index"));
		System.out.println("[workshop] world loaded in " + (System.currentTimeMillis() - started) + " ms");

		List<MapIndex.Entry> selected = select(index, all, options);
		if (selected.isEmpty()) {
			System.out.println("[workshop] nothing to export");
			return;
		}

		Files.createDirectories(outDir);
		Map<String, Integer> kindCounts = new LinkedHashMap<String, Integer>();
		Set<Integer> written = new LinkedHashSet<Integer>();
		int objectsWritten = 0;
		int skippedNoMapData = 0;
		long bytesWritten = 0;
		List<String> indexRows = new ArrayList<String>();

		for (MapIndex.Entry entry : selected) {
			Path[] files = entry.mapFiles(mapDir);
			if (files == null) {
				// 51 regions are listed but ship without map files. Region.load skips them too.
				skippedNoMapData++;
				continue;
			}
			GroundMap ground = GroundMap.read(files[0]);
			List<Placement> placements = placementsOf(entry);

			String json = RegionDocument.toJson(entry, ground, placements, defs, sizes,
					Region::getClipping);
			Path out = outDir.resolve(entry.regionId + ".json");
			byte[] encoded = json.getBytes(StandardCharsets.UTF_8);
			Files.write(out, encoded);
			bytesWritten += encoded.length;
			objectsWritten += placements.size();

			for (Placement placement : placements) {
				String kind = ResourceRules.classify(defs.get(placement.id));
				if (kind != null) {
					Integer n = kindCounts.get(kind);
					kindCounts.put(kind, n == null ? 1 : n + 1);
				}
			}
			indexRows.add(row(entry, placements.size(), encoded.length));
			written.add(entry.regionId);
			System.out.println("[workshop] " + entry.regionId + " " + entry.baseX() + "," + entry.baseY()
					+ " -> " + placements.size() + " objects, " + encoded.length + " bytes");
		}

		Path indexFile = outDir.resolve("index.json");
		Files.write(indexFile, indexDocument(selected.size(), skippedNoMapData, indexRows)
				.getBytes(StandardCharsets.UTF_8));

		// Banks are collected from every region the server loaded, not from `selected`: see BankIndex.
		// The overview and the bank index come out of the same walk, so the two cannot disagree about
		// how many banks a region holds. It is given the documents this run actually wrote, because
		// "the export can open it" is what the viewer needs to know — not "terrain exists for it",
		// which is true for regions this run never touched.
		WorldScan scan = worldScan(index, defs, written);
		Path bankFile = outDir.resolve("banks.json");
		Files.write(bankFile, BankIndex.toJson(scan.banks()).getBytes(StandardCharsets.UTF_8));

		// The whole-world overview: every region in the index, so the viewer can draw the world and
		// lazily open whatever the camera reaches (`BOT_TOOLING.md` §4, Layer 1).
		Path worldFile = outDir.resolve("world.json");
		Files.write(worldFile, WorldDoc.toJson(scan.entries()).getBytes(StandardCharsets.UTF_8));

		// The authored places, read through the server's own parser so the editor draws what the
		// server resolves, and each row carries the canonical text of LocationsConfig.toRow.
		LocationsConfig.Result curated = LocationsConfig.load();
		for (String problem : curated.problems()) {
			System.out.println("[workshop] locations.cfg: " + problem);
		}
		Path locationsFile = outDir.resolve("locations.json");
		Files.write(locationsFile, LocationsDoc.toJson(curated.locations())
				.getBytes(StandardCharsets.UTF_8));

		System.out.println();
		System.out.println("[workshop] regions written        = " + indexRows.size());
		if (skippedNoMapData > 0) {
			System.out.println("[workshop] regions with no map data = " + skippedNoMapData + " (the server skips these too)");
		}
		System.out.println("[workshop] object placements      = " + objectsWritten);
		System.out.println("[workshop] banks in the world     = " + scan.banks().size()
				+ " (whole-world scan, not just the exported regions)");
		System.out.println("[workshop] world overview         = " + scan.entries().size() + " regions, "
				+ index.size() + " in the map index");
		System.out.println("[workshop] authored places        = " + curated.locations().size()
				+ " row(s) from " + LocationsConfig.DEFAULT_PATH
				+ (curated.problems().isEmpty() ? "" : ", "
						+ curated.problems().size() + " unreadable"));
		System.out.println("[workshop] classified by icon rule:");
		for (ResourceRules.Rule rule : ResourceRules.rules()) {
			Integer n = kindCounts.get(rule.kind());
			System.out.println(String.format("              %-10s %6d%s", rule.kind(), n == null ? 0 : n,
					n == null ? "   <- no object in this selection matches this action" : ""));
		}
		System.out.println("[workshop] wrote " + (bytesWritten / 1024) + " KB to " + outDir.toAbsolutePath());
		System.out.println("[workshop] total " + (System.currentTimeMillis() - started) + " ms");
	}

	/**
	 * All regions when {@code all} is set, the named regions when any ids are given, and
	 * {@link #DEFAULT_AREA} otherwise — a bare {@code workshopExport} must not write half a gigabyte,
	 * but it must write a <em>contiguous</em> patch so the viewer's continuous view has something to
	 * be continuous about.
	 */
	private static List<MapIndex.Entry> select(List<MapIndex.Entry> index, boolean all,
			List<String> options) {
		if (all) {
			return new ArrayList<MapIndex.Entry>(index);
		}

		List<Integer> ids = new ArrayList<Integer>();
		int[] area = DEFAULT_AREA;
		for (String option : options) {
			if (option.startsWith("area=")) {
				area = parseArea(option.substring("area=".length()));
			} else {
				ids.add(Integer.parseInt(option));
			}
		}

		if (!ids.isEmpty()) {
			List<MapIndex.Entry> selected = new ArrayList<MapIndex.Entry>();
			for (Integer id : ids) {
				boolean found = false;
				for (MapIndex.Entry entry : index) {
					if (entry.regionId == id) {
						selected.add(entry);
						found = true;
						break;
					}
				}
				if (!found) {
					throw new IllegalArgumentException("region " + id + " is not in map_index");
				}
			}
			return selected;
		}

		// An area selects every indexed region whose corner falls in the window, without the caller
		// having to know the region-id numbering — which is the point: "the block around Lumbridge"
		// is what an author has in mind, not a list of ids.
		int minX = area[0];
		int minY = area[1];
		int maxX = minX + 64 * (area[2] - 1);
		int maxY = minY + 64 * (area[3] - 1);
		List<MapIndex.Entry> selected = new ArrayList<MapIndex.Entry>();
		for (MapIndex.Entry entry : index) {
			if (entry.baseX() >= minX && entry.baseX() <= maxX
					&& entry.baseY() >= minY && entry.baseY() <= maxY) {
				selected.add(entry);
			}
		}
		if (selected.isEmpty()) {
			throw new IllegalArgumentException("no region in map_index lies in the area "
					+ minX + "," + minY + " " + area[2] + "x" + area[3]);
		}
		return selected;
	}

	/** Parses {@code baseX,baseY,columns,rows}. */
	private static int[] parseArea(String value) {
		String[] parts = value.split(",");
		if (parts.length != 4) {
			throw new IllegalArgumentException("an area is baseX,baseY,columns,rows — got '" + value + "'");
		}
		int[] area = new int[4];
		for (int i = 0; i < 4; i++) {
			area[i] = Integer.parseInt(parts[i].trim());
		}
		if (area[2] < 1 || area[3] < 1) {
			throw new IllegalArgumentException("an area needs at least one region: '" + value + "'");
		}
		return area;
	}

	/**
	 * The objects the server placed in this region. Read out of {@code Region} rather than decoded
	 * again: {@code Region.addObject} is where the size table, the rotation swap and the
	 * projectile-solid rules are applied, and reimplementing that here is how the two would drift.
	 */
	private static List<Placement> placementsOf(MapIndex.Entry entry) {
		Region region = Region.getRegion(entry.baseX(), entry.baseY());
		List<Placement> placements = new ArrayList<Placement>();
		if (region == null) {
			return placements;
		}
		for (Objects object : region.realObjects) {
			if (object.objectId < 0) {
				// A negative id marks a removed object; Region.addObject leaves the tombstone behind.
				continue;
			}
			placements.add(new Placement(object.objectId, object.objectX, object.objectY,
					object.objectHeight, object.objectType, object.objectFace));
		}
		return placements;
	}

	/**
	 * One walk of the loaded world, producing both the bank index and the overview rows.
	 *
	 * <p>Deliberately not {@code select(..)}: the editor has to be able to answer "nearest bank" and
	 * "what regions exist" for regions that were never exported, and the world is already in memory
	 * after {@code Region.load()}. A single pass is also what keeps the overview's per-region bank
	 * count and {@code banks.json} from disagreeing.
	 */
	private static WorldScan worldScan(List<MapIndex.Entry> index, LocDefs defs, Set<Integer> written) {
		List<BankIndex.Bank> banks = new ArrayList<BankIndex.Bank>();
		List<WorldDoc.Entry> entries = new ArrayList<WorldDoc.Entry>(index.size());
		for (MapIndex.Entry entry : index) {
			Region region = Region.getRegion(entry.baseX(), entry.baseY());
			if (region == null) {
				entries.add(new WorldDoc.Entry(entry.regionId, entry.baseX(), entry.baseY(), false, 0, 0));
				continue;
			}
			int objects = 0;
			int regionBanks = 0;
			for (Objects object : region.realObjects) {
				if (object.objectId < 0) {
					// A negative id marks a removed object; Region.addObject leaves the tombstone.
					continue;
				}
				objects++;
				LocDefinition def = defs.get(object.objectId);
				if (!"bank".equals(ResourceRules.classify(def))) {
					continue;
				}
				regionBanks++;
				banks.add(new BankIndex.Bank(object.objectId, object.objectX, object.objectY,
						object.objectHeight, def == null ? null : def.name()));
			}
			entries.add(new WorldDoc.Entry(entry.regionId, entry.baseX(), entry.baseY(),
					written.contains(entry.regionId), objects, regionBanks));
		}
		return new WorldScan(banks, entries);
	}

	/** What the world walk produced: the bank list and the overview rows, from one pass. */
	private record WorldScan(List<BankIndex.Bank> banks, List<WorldDoc.Entry> entries) {
	}

	private static String row(MapIndex.Entry entry, int objects, int bytes) {
		return "    { \"regionId\": " + entry.regionId + ", \"baseX\": " + entry.baseX()
				+ ", \"baseY\": " + entry.baseY() + ", \"objects\": " + objects
				+ ", \"bytes\": " + bytes + " }";
	}

	private static String indexDocument(int regions, int skipped, List<String> rows) {
		StringBuilder out = new StringBuilder();
		out.append("{\n");
		out.append("  \"regions\": ").append(regions).append(",\n");
		out.append("  \"regionsWithoutMapData\": ").append(skipped).append(",\n");
		out.append("  \"entries\": [\n");
		out.append(String.join(",\n", rows));
		out.append("\n  ]\n}\n");
		return out.toString();
	}
}
