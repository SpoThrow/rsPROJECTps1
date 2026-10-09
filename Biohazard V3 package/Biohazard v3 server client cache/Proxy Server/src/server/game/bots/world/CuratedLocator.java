package server.game.bots.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * A locator backed by a table — {@code BOT_LOCATIONS.md} A.2. The default, because it is correct and
 * its cost is O(1) rather than a world scan.
 *
 * <p>A family is a filtered view of one table: {@code CuratedLocator.filter(all, LocationKind.BANK)}
 * is "the banks". Filtering rather than separate tables means a row added to {@code locations.cfg} is
 * immediately visible to every family that includes its kind, with nothing to keep in step.
 */
public final class CuratedLocator implements Locator<Location> {

	private final List<Location> locations;
	private final Map<String, Location> byName;
	private final Set<LocationKind> kinds;

	private CuratedLocator(List<Location> all, Set<LocationKind> kinds) {
		this.kinds = kinds;
		List<Location> kept = new ArrayList<Location>();
		Map<String, Location> index = new HashMap<String, Location>();
		for (Location location : all) {
			if (kinds != null && !kinds.contains(location.kind())) {
				continue;
			}
			kept.add(location);
			// First wins: a name is how a script refers to a place, so a duplicate is an authoring
			// bug. Keeping the first is deterministic, which is what a reproducible bot needs.
			if (!index.containsKey(location.name())) {
				index.put(location.name(), location);
			}
		}
		this.locations = Collections.unmodifiableList(kept);
		this.byName = Collections.unmodifiableMap(index);
	}

	/** Every entry. */
	public static CuratedLocator of(List<Location> all) {
		return new CuratedLocator(all, null);
	}

	/** The subset whose kind is one of {@code kinds}. */
	public static CuratedLocator filter(List<Location> all, LocationKind... kinds) {
		Set<LocationKind> set = EnumSet.noneOf(LocationKind.class);
		Collections.addAll(set, kinds);
		return new CuratedLocator(all, set);
	}

	@Override
	public List<Location> nearest(int x, int y, int plane, int limit) {
		if (limit <= 0) {
			return Collections.emptyList();
		}
		// A bounded max-heap rather than a full sort: the table can hold thousands of monster spawns
		// and this is called on the game thread (BOT_ROADMAP.md §7). O(n log limit), and nothing is
		// allocated per candidate.
		Comparator<Location> farthestFirst = new Comparator<Location>() {
			@Override
			public int compare(Location a, Location b) {
				return Integer.compare(b.distanceSquaredTo(x, y), a.distanceSquaredTo(x, y));
			}
		};
		PriorityQueue<Location> best = new PriorityQueue<Location>(Math.min(limit, 16) + 1, farthestFirst);
		for (Location location : locations) {
			// Plane-strict: a bank one plane up is a different journey, not a shorter one.
			if (location.plane() != plane) {
				continue;
			}
			best.add(location);
			if (best.size() > limit) {
				best.poll();
			}
		}
		List<Location> result = new ArrayList<Location>(best);
		Collections.sort(result, new Comparator<Location>() {
			@Override
			public int compare(Location a, Location b) {
				return Integer.compare(a.distanceSquaredTo(x, y), b.distanceSquaredTo(x, y));
			}
		});
		return result;
	}

	@Override
	public Location byName(String name) {
		return name == null ? null : byName.get(name);
	}

	@Override
	public List<Location> all() {
		return locations;
	}

	/** The kinds this view holds, or {@code null} for "all". */
	public Set<LocationKind> kinds() {
		return kinds;
	}

	@Override
	public String toString() {
		return "CuratedLocator(" + (kinds == null ? "all" : kinds.toString()) + ", " + locations.size() + ")";
	}
}
