package server.game.bots.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import server.game.objects.Objects;

/**
 * Region scans, remembered.
 *
 * <p>Two caches, because the two costs are different:
 *
 * <ul>
 * <li><b>classifications by object id</b> — permanent. What "id 1276" is cannot change while the
 *     server runs, so it is computed once and kept. This matters more than it looks:
 *     {@code ObjectDef} keeps a 20-entry cache and re-parses {@code loc.dat} on every miss, so
 *     without this a scan of one region would re-parse hundreds of definitions. It is also why the
 *     classification has to happen inside {@link #classify}: the {@code ObjectDef} instance handed
 *     back is only valid until the next call, because the cache it came from is 20 entries deep.
 * <li><b>scanned regions</b> — expiring. A region's contents change (trees fall, rocks deplete), so a
 *     scan is reused for {@link #ttlMillis} and then redone. {@code BOT_LOCATIONS.md} A.5 sanctions
 *     exactly this: start time-based, hook the mutation events later if it ever matters.
 * </ul>
 *
 * <p>Scans hold every classified kind, not one family's, so {@code Locations} can share a single
 * instance across trees, rocks, banks and the rest instead of each family scanning the world again.
 */
final class RegionScanCache {

	/** Marks "this id is ordinary scenery" in the memo, so a null result is not re-parsed. */
	private static final String NONE = "";

	/** Objects in one 64x64 region, or a placeholder for one the server never loaded. */
	static final class Scan {
		final List<Location> locations;
		final long scannedAt;

		Scan(List<Location> locations, long scannedAt) {
			this.locations = locations;
			this.scannedAt = scannedAt;
		}
	}

	private final Map<Long, Scan> regions = new HashMap<Long, Scan>();
	private final Map<Integer, String> kindOfId = new HashMap<Integer, String>();
	private final ScannedLocator.RegionSource source;
	private final ScannedLocator.KindSource kinds;
	private final ScannedLocator.Clock clock;
	private final long ttlMillis;

	RegionScanCache(ScannedLocator.RegionSource source, ScannedLocator.KindSource kinds,
			ScannedLocator.Clock clock, long ttlMillis) {
		this.source = source;
		this.kinds = kinds;
		this.clock = clock;
		this.ttlMillis = ttlMillis;
	}

	/** Every classified object location in the region, from cache or a fresh scan. */
	List<Location> region(int baseX, int baseY) {
		long key = key(baseX, baseY);
		long now = clock.now();
		Scan cached = regions.get(key);
		if (cached != null && now - cached.scannedAt < ttlMillis) {
			return cached.locations;
		}
		List<Location> scanned = scan(baseX, baseY);
		regions.put(key, new Scan(scanned, now));
		return scanned;
	}

	private List<Location> scan(int baseX, int baseY) {
		List<Objects> objects = source.objects(baseX, baseY);
		if (objects == null || objects.isEmpty()) {
			return Collections.emptyList();
		}
		List<Location> found = new ArrayList<Location>();
		for (Objects object : objects) {
			if (object == null || object.objectId < 0) {
				// A negative id marks a removed object; Region.addObject leaves the tombstone behind.
				continue;
			}
			// Fetch and classify back to back: the ObjectDef we get is only valid until the next
			// getObjectDef call, so it must not escape this loop iteration.
			String kind = classify(object.objectId);
			if (kind == null) {
				continue;
			}
			LocationKind locationKind = LocationKind.byId(kind);
			if (locationKind == null || !locationKind.isObjectKind()) {
				continue;
			}
			found.add(new Location(kind + ":" + object.objectX + "," + object.objectY
					+ ":" + object.objectHeight, locationKind, object.objectX, object.objectY,
					object.objectHeight, 1, 1, null));
		}
		return Collections.unmodifiableList(found);
	}

	/** The kind of an object id, memoised. {@code null} is stored as {@link #NONE} so it is not re-parsed. */
	private String classify(int objectId) {
		String cached = kindOfId.get(objectId);
		if (cached != null) {
			return cached == NONE ? null : cached;
		}
		String kind = kinds.kindOf(objectId);
		kindOfId.put(objectId, kind == null ? NONE : kind);
		return kind;
	}

	private static long key(int baseX, int baseY) {
		return ((long) baseX << 32) | (baseY & 0xffffffffL);
	}

	int scannedRegions() {
		return regions.size();
	}

	int knownObjectIds() {
		return kindOfId.size();
	}

	void invalidate() {
		regions.clear();
	}
}
