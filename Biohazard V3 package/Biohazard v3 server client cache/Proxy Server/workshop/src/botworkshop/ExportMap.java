package botworkshop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import botworkshop.classify.ResourceRules;
import botworkshop.data.GroundMap;
import botworkshop.data.LocDefinition;
import botworkshop.data.LocDefs;
import botworkshop.data.MapIndex;
import botworkshop.export.BankIndex;
import botworkshop.export.Placement;
import botworkshop.export.RegionDocument;
import server.clip.region.ObjectDef;
import server.clip.region.ObjectSizes;
import server.clip.region.Region;
import server.game.objects.Objects;

/**
 * Bot Workshop exporter — {@code BOT_TOOLING.md} stage T1.
 *
 * <p>Reads the world the server reads and writes one JSON document per region for the editor's map
 * view. Run from the server directory so {@code ./Data} resolves the way it does at boot:
 *
 * <pre>gradlew workshopExport                 # a small default set of landmark regions
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

	/** Regions a first run writes: Lumbridge, Draynor and Varrock. */
	private static final int[] DEFAULT_REGIONS = { 12850, 12338, 12853 };

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
			System.out.println("[workshop] " + entry.regionId + " " + entry.baseX() + "," + entry.baseY()
					+ " -> " + placements.size() + " objects, " + encoded.length + " bytes");
		}

		Path indexFile = outDir.resolve("index.json");
		Files.write(indexFile, indexDocument(selected.size(), skippedNoMapData, indexRows)
				.getBytes(StandardCharsets.UTF_8));

		// Banks are collected from every region the server loaded, not from `selected`: see BankIndex.
		List<BankIndex.Bank> banks = banksOf(index, defs);
		Path bankFile = outDir.resolve("banks.json");
		Files.write(bankFile, BankIndex.toJson(banks).getBytes(StandardCharsets.UTF_8));

		System.out.println();
		System.out.println("[workshop] regions written        = " + indexRows.size());
		if (skippedNoMapData > 0) {
			System.out.println("[workshop] regions with no map data = " + skippedNoMapData + " (the server skips these too)");
		}
		System.out.println("[workshop] object placements      = " + objectsWritten);
		System.out.println("[workshop] banks in the world     = " + banks.size()
				+ " (whole-world scan, not just the exported regions)");
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
	 * All regions when {@code all} is set, the named regions when any are given, and the small
	 * landmark set otherwise — a bare {@code workshopExport} must not write half a gigabyte.
	 */
	private static List<MapIndex.Entry> select(List<MapIndex.Entry> index, boolean all,
			List<String> wanted) {
		if (all) {
			return new ArrayList<MapIndex.Entry>(index);
		}
		List<Integer> ids = new ArrayList<Integer>();
		if (wanted.isEmpty()) {
			for (int id : DEFAULT_REGIONS) {
				ids.add(id);
			}
		} else {
			for (String value : wanted) {
				ids.add(Integer.parseInt(value));
			}
		}
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
	 * Every classified bank object in the world, walked over the whole region directory.
	 *
	 * <p>This deliberately does not reuse {@code select(...)}: the editor has to be able to answer
	 * "nearest bank" for a region that was never exported, and the world is already in memory after
	 * {@code Region.load()} either way.
	 */
	private static List<BankIndex.Bank> banksOf(List<MapIndex.Entry> index, LocDefs defs) {
		List<BankIndex.Bank> banks = new ArrayList<BankIndex.Bank>();
		for (MapIndex.Entry entry : index) {
			Region region = Region.getRegion(entry.baseX(), entry.baseY());
			if (region == null) {
				// The 51 regions with no map data — Region.load skipped them, so there is nothing
				// here and nothing to report.
				continue;
			}
			for (Objects object : region.realObjects) {
				if (object.objectId < 0) {
					continue;
				}
				LocDefinition def = defs.get(object.objectId);
				if (!"bank".equals(ResourceRules.classify(def))) {
					continue;
				}
				banks.add(new BankIndex.Bank(object.objectId, object.objectX, object.objectY,
						object.objectHeight, def == null ? null : def.name()));
			}
		}
		return banks;
	}

	private static String row(MapIndex.Entry entry, int objects, int bytes) {		return "    { \"regionId\": " + entry.regionId + ", \"baseX\": " + entry.baseX()
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
