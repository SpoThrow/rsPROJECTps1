package botworkshop.export;

import java.util.List;

/**
 * Serialises every region the map index knows about as one overview document
 * ({@code BOT_TOOLING.md} §4, the Layer 1 map view — one continuous map).
 *
 * <p><b>All 1226 regions, not the exported subset.</b> The overview is what tells the viewer where
 * the world <em>is</em> — which regions exist, and where the next bank is — so it has to cover the
 * whole index; an overview drawn only from the exported regions would show holes that are not in the
 * world but in the export. That is also why each entry carries an {@code exported} flag: a region
 * with terrain the export never wrote is drawn as "not in this export", which is a different claim
 * from "empty ground".
 *
 * <p>{@code objects} and {@code banks} are counts the exporter already produced while walking the
 * loaded world, so nothing here re-scans anything — and they are reported for every region with
 * terrain, exported or not, because "where is there anything worth looking at" is a question about
 * the world, not about this run.
 */
public final class WorldDoc {

	/** One region's summary, positioned by its corner. */
	public record Entry(int regionId, int baseX, int baseY, boolean exported, int objects, int banks) {
	}

	private WorldDoc() {
	}

	/**
	 * The document. World bounds are derived from the entries rather than assumed: the map is not a
	 * rectangle and its corner is not at the origin, so the viewer needs the real extent to frame it.
	 */
	public static String toJson(List<Entry> entries) {
		// Seeded from the first entry rather than from zero: the map's corner is wherever its lowest
		// region is, and starting at the origin would report a bound the world does not occupy.
		int minX = 0;
		int minY = 0;
		int maxX = 0;
		int maxY = 0;
		if (!entries.isEmpty()) {
			minX = entries.get(0).baseX();
			minY = entries.get(0).baseY();
			maxX = minX + 63;
			maxY = minY + 63;
		}
		int withData = 0;
		for (Entry entry : entries) {
			minX = Math.min(minX, entry.baseX());
			minY = Math.min(minY, entry.baseY());
			maxX = Math.max(maxX, entry.baseX() + 63);
			maxY = Math.max(maxY, entry.baseY() + 63);
			if (entry.exported()) {
				withData++;
			}
		}

		Json json = new Json();
		json.openObject();
		json.field("regions", entries.size());
		json.field("exported", withData);
		json.field("minX", minX);
		json.field("maxX", maxX);
		json.field("minY", minY);
		json.field("maxY", maxY);
		json.name("entries").openArray();
		for (Entry entry : entries) {
			json.openObject();
			json.field("regionId", entry.regionId());
			json.field("baseX", entry.baseX());
			json.field("baseY", entry.baseY());
			json.field("exported", entry.exported());
			json.field("objects", entry.objects());
			json.field("banks", entry.banks());
			json.closeObject();
		}
		json.closeArray();
		json.closeObject();
		return json.toString();
	}
}
