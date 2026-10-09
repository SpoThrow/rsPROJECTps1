package botworkshop.export;

import java.util.List;

import botworkshop.classify.ResourceRules;
import botworkshop.data.GroundMap;
import botworkshop.data.GroundTile;
import botworkshop.data.LocDefinition;
import botworkshop.data.LocDefs;
import botworkshop.data.MapIndex;
import server.clip.region.ObjectSizes;

/**
 * Renders one region as the JSON document the editor loads — {@code BOT_TOOLING.md} stage T1.
 *
 * <p><b>Format</b> ({@code Data/workshop/map/&lt;regionId&gt;.json}):
 *
 * <pre>
 * {
 *   "regionId": 12850, "baseX": 3200, "baseY": 3200,
 *   "planes": [ { "plane": 0, "overlay": "7*4096", "underlay": "...", "flags": "..." } ],
 *   "objects": [ { "id": 1276, "x": 3201, "y": 3202, "plane": 0, "type": 10, "rotation": 0,
 *                  "name": "Tree", "actions": ["Chop down"], "kind": "tree", "service": false,
 *                  "sizeX": 2, "sizeY": 2, "clip": 256 } ]
 * }
 * </pre>
 *
 * <p>The three tile strings are run-length encoded (see {@link Rle}) and indexed
 * {@code localX * 64 + localY} within a plane — the same order the decoder reads them, x outermost.
 * Getting that order wrong would transpose the map, so it is stated here and asserted in
 * {@code RegionDocumentTest}.
 *
 * <p>Everything a caller could disagree about is injected rather than looked up: the tile data, the
 * placements, the definitions, the size table and the clip source. That is what lets the builder be
 * tested without loading the world, while the CLI wires it to the server's own loaders.
 */
public final class RegionDocument {

	private RegionDocument() {
	}

	public static String toJson(MapIndex.Entry entry, GroundMap ground, List<Placement> placements,
			LocDefs defs, ObjectSizes sizes, TileClip clip) {
		Json json = new Json();
		json.openObject();
		json.field("regionId", entry.regionId);
		json.field("baseX", entry.baseX());
		json.field("baseY", entry.baseY());

		json.name("planes").openArray();
		for (int plane = 0; plane < GroundMap.planes(); plane++) {
			int size = GroundMap.size();
			int[] overlay = new int[size * size];
			int[] underlay = new int[size * size];
			int[] flags = new int[size * size];
			for (int localX = 0; localX < size; localX++) {
				for (int localY = 0; localY < size; localY++) {
					GroundTile tile = ground.tile(plane, localX, localY);
					int at = localX * size + localY;
					overlay[at] = tile.overlayId;
					underlay[at] = tile.underlayId;
					flags[at] = tile.flags;
				}
			}
			json.openObject();
			json.field("plane", plane);
			json.field("overlay", Rle.encode(overlay));
			json.field("underlay", Rle.encode(underlay));
			json.field("flags", Rle.encode(flags));
			json.closeObject();
		}
		json.closeArray();

		json.name("objects").openArray();
		for (Placement placement : placements) {
			LocDefinition def = defs == null ? null : defs.get(placement.id);
			String kind = ResourceRules.classify(def);

			json.openObject();
			json.field("id", placement.id);
			json.field("x", placement.x);
			json.field("y", placement.y);
			json.field("plane", placement.plane);
			json.field("type", placement.type);
			json.field("rotation", placement.rotation);
			json.field("name", def == null ? null : def.name());
			json.name("actions").openArray();
			if (def != null) {
				for (String action : def.actions()) {
					json.value(action);
				}
			}
			json.closeArray();
			json.field("kind", kind);
			json.field("service", kind != null && ResourceRules.isService(kind));
			// The size table wins over loc.dat exactly as Region.addObject resolves it, so a
			// footprint drawn here is the footprint the server collides with.
			int fallbackX = def == null ? 1 : def.sizeX();
			int fallbackY = def == null ? 1 : def.sizeY();
			json.field("sizeX", sizes == null ? fallbackX : sizes.width(placement.id, fallbackX));
			json.field("sizeY", sizes == null ? fallbackY : sizes.height(placement.id, fallbackY));
			if (clip != null) {
				json.field("clip", clip.clip(placement.x, placement.y, placement.plane));
			}
			json.closeObject();
		}
		json.closeArray();

		json.closeObject();
		return json.toString();
	}
}
