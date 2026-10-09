package server.game.bots.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import server.clip.region.ObjectDef;
import server.clip.region.Region;
import server.game.objects.Objects;

/**
 * Finds places by looking at the world the server has actually loaded — {@code BOT_LOCATIONS.md} A.2.
 * The fallback, not the default: correct answers cost a scan, so {@link CuratedLocator} is preferred
 * wherever it has an answer, and this covers what the authored table does not.
 *
 * <p><b>What it guarantees, and what it does not.</b> A scan covers a bounded ring of regions around
 * the query, {@link #defaultMaxRings} rings by default — 5x5 regions, 320 tiles across. The answer is
 * therefore <em>"the nearest within that box"</em>, not "the nearest in the world". That limitation is
 * inherent to not scanning 1226 regions on the game thread ({@code BOT_ROADMAP.md} §7), and it is why
 * a curated answer is better: a curated lookup is global and O(1). Inside its box the answer is exact,
 * and distance is measured to the nearest edge of the region box rather than to its centre.
 *
 * <p><b>Injection, so this is testable.</b> {@link RegionSource} and {@link KindSource} are the two
 * things that touch the live server. Production wires them to {@code Region.getRegion} and
 * {@code ObjectDef}; a test wires them to a handful of hand-placed objects, so the scan, the ring
 * search, the plane filter and the cache expiry are all covered without loading 1226 regions and 1.9
 * million objects into a test JVM.
 */
public final class ScannedLocator implements Locator<Location> {

	/** Default ring count: 2 means the 5x5 regions centred on the query. */
	public static final int DEFAULT_MAX_RINGS = 2;

	/** How long a scanned region is reused. See {@code BOT_LOCATIONS.md} A.5. */
	public static final long DEFAULT_TTL_MILLIS = 30_000;

	/** Objects in one 64x64 region. */
	public interface RegionSource {
		List<Objects> objects(int baseX, int baseY);
	}

	/** The kind of an object id, or {@code null} for ordinary scenery. */
	public interface KindSource {
		String kindOf(int objectId);
	}

	/** Milliseconds. Injected so a test can advance time without sleeping. */
	public interface Clock {
		long now();
	}

	private final RegionScanCache cache;
	private final Set<LocationKind> kinds;
	private final int maxRings;

	private ScannedLocator(RegionScanCache cache, Set<LocationKind> kinds, int maxRings) {
		this.cache = cache;
		this.kinds = kinds;
		this.maxRings = maxRings;
	}

	/** The live wiring: regions from the server, kinds from {@link ResourceKinds} over {@code ObjectDef}. */
	public static RegionScanCache liveCache(long ttlMillis) {
		return new RegionScanCache(regionSource(), kindSource(), new Clock() {
			@Override
			public long now() {
				return System.currentTimeMillis();
			}
		}, ttlMillis);
	}

	/**
	 * A locator over the kinds in {@code kinds}, reading the live world.
	 *
	 * <p>Kept as a factory rather than a public constructor so the injected form used by tests is not
	 * something a production caller can reach for by accident — every {@code ScannedLocator} of one
	 * {@code Locations} shares one cache, or each family would scan the same region again.
	 */
	public static ScannedLocator live(Set<LocationKind> kinds, int maxRings, RegionScanCache cache) {
		return new ScannedLocator(cache, kinds, maxRings);
	}

	/** The test form: injected sources and clock, so no world load is needed. */
	static ScannedLocator with(RegionScanCache cache, Set<LocationKind> kinds, int maxRings) {
		return new ScannedLocator(cache, kinds, maxRings);
	}

	/** A cache over the given sources, for tests that inject a fake world. */
	static RegionScanCache cacheOver(RegionSource source, KindSource kinds, Clock clock, long ttlMillis) {
		return new RegionScanCache(source, kinds, clock, ttlMillis);
	}

	/** The kinds this locator serves. */
	public Set<LocationKind> kinds() {
		return Collections.unmodifiableSet(kinds);
	}

	@Override
	public List<Location> nearest(int x, int y, int plane, int limit) {
		if (limit <= 0) {
			return Collections.emptyList();
		}
		final int qx = x;
		final int qy = y;
		Comparator<Location> closestFirst = new Comparator<Location>() {
			@Override
			public int compare(Location a, Location b) {
				return Integer.compare(a.distanceSquaredTo(qx, qy), b.distanceSquaredTo(qx, qy));
			}
		};

		int centreX = baseOf(x);
		int centreY = baseOf(y);
		List<Location> found = new ArrayList<Location>();
		// Rings outward from the query's own region. The early exit is a *bound*, not a count: after
		// finishing ring `r`, every tile in a further ring is at least `(r+1)*64 - 63` away on one
		// axis, because region bases are 64 apart and the query can sit at most 63 into its own
		// region. So the search may stop only once something found is closer than that. Stopping as
		// soon as `limit` candidates exist would be wrong — the query's own region usually holds a
		// few objects, and the nearest one is often in the region next door.
		for (int ring = 0; ring <= maxRings; ring++) {
			for (int i = -ring; i <= ring; i++) {
				for (int j = -ring; j <= ring; j++) {
					if (Math.max(Math.abs(i), Math.abs(j)) != ring) {
						continue; // already covered by an inner ring
					}
					collect(centreX + i * SIZE, centreY + j * SIZE, plane, found);
				}
			}
			if (found.isEmpty()) {
				continue;
			}
			int nearest = Integer.MAX_VALUE;
			for (Location location : found) {
				nearest = Math.min(nearest, location.distanceSquaredTo(x, y));
			}
			int lowerBound = (ring + 1) * SIZE - (SIZE - 1);
			if (nearest <= lowerBound * lowerBound) {
				break;
			}
		}
		Collections.sort(found, closestFirst);
		return found.size() <= limit ? found : new ArrayList<Location>(found.subList(0, limit));
	}

	private void collect(int baseX, int baseY, int plane, List<Location> into) {
		for (Location location : cache.region(baseX, baseY)) {
			if (location.plane() == plane && kinds.contains(location.kind())) {
				into.add(location);
			}
		}
	}

	/**
	 * The scanned locator has no names: it names places by coordinate, and those names are generated,
	 * not authored. Returning {@code null} rather than a synthetic hit keeps "look up draynor_bank" a
	 * curated question, which is what it is.
	 */
	@Override
	public Location byName(String name) {
		return null;
	}

	@Override
	public List<Location> all() {
		// An exhaustive answer would be a full world scan, which is exactly what this class exists to
		// avoid. Empty is the honest answer; callers that need everything use the curated table.
		return Collections.emptyList();
	}

	/** 64-tile regions, matching {@code Region}'s own ({@code x >> 3 / 8}). */
	static final int SIZE = 64;

	private static int baseOf(int coordinate) {
		return (coordinate >> 6) << 6;
	}

	// ---- production wiring -----------------------------------------------------------------

	/** Regions from the running server. A region that was never loaded is simply empty. */
	static RegionSource regionSource() {
		return new RegionSource() {
			@Override
			public List<Objects> objects(int baseX, int baseY) {
				Region region = Region.getRegion(baseX, baseY);
				return region == null ? Collections.<Objects>emptyList() : region.realObjects;
			}
		};
	}

	/**
	 * Kinds from {@link ResourceKinds} over {@code ObjectDef}.
	 *
	 * <p>The returned {@code ObjectDef} is only valid until the next call — {@code ObjectDef} caches
	 * 20 at a time — so it is classified and dropped here, never stored.
	 */
	static KindSource kindSource() {
		return new KindSource() {
			@Override
			public String kindOf(int objectId) {
				return ResourceKinds.classify(ObjectDef.getObjectDef(objectId));
			}
		};
	}

	/** All the object kinds, which is what a shared cache is built for. */
	static Set<LocationKind> objectKinds() {
		Set<LocationKind> all = EnumSet.noneOf(LocationKind.class);
		for (LocationKind kind : LocationKind.values()) {
			if (kind.isObjectKind()) {
				all.add(kind);
			}
		}
		return all;
	}
}
