package server.game.bots.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The single source of "where in the world is X" — {@code BOT_LOCATIONS.md} part A.
 *
 * <p>A bot asks {@code locations.banks().nearest(x, y, 0, 1)} and gets an answer regardless of which
 * locator served it: the authored table if it can, a world scan if it cannot
 * ({@link MergingLocator}). Adding support for a new resource is a row in
 * {@code Data/cfg/bots/locations.cfg}, not code in every bot.
 *
 * <p><b>Derived, never duplicated.</b> The authored file holds only what a file cannot already answer
 * — human names and tags for resource regions. Teleports, shops and monsters are <em>joined</em> from
 * the server's own {@code Data/cfg} ({@link LocationsData}), so the bot's world-model cannot drift from
 * the server's.
 *
 * <p><b>Loaded on first use, not at startup.</b> {@link #live()} builds the table the first time a bot
 * asks for it and caches it. The server therefore boots with no new work and with no call added to
 * {@code Server.main}; deleting this whole package leaves it running. A load failure is recorded in
 * {@link #problems()} and yields whatever could be read, because a bot with a partial world-model is
 * still better than a server that will not start.
 */
public final class Locations {

	private static Locations instance;

	private final List<Location> all;
	private final List<String> problems;
	private final RegionScanCache cache;
	private final Map<String, Locator<Location>> families = new HashMap<String, Locator<Location>>();

	private Locations(List<Location> all, List<String> problems, RegionScanCache cache) {
		this.all = Collections.unmodifiableList(new ArrayList<Location>(all));
		this.problems = Collections.unmodifiableList(new ArrayList<String>(problems));
		this.cache = cache;
	}

	/**
	 * The process-wide instance, read from {@code Data/cfg} on first call.
	 *
	 * <p>Guarded by synchronisation rather than a holder idiom so a failed load can be retried: if
	 * {@code Data/cfg} was not readable the first time, a later call should try again rather than
	 * remember the failure forever.
	 */
	public static synchronized Locations live() {
		if (instance == null) {
			LocationsData.Result result = LocationsData.load();
			instance = new Locations(result.locations(), result.problems(),
					ScannedLocator.liveCache(ScannedLocator.DEFAULT_TTL_MILLIS));
		}
		return instance;
	}

	/** A table with no scanning arm — for curated-only families and for tests. */
	public static Locations curated(List<Location> all) {
		return new Locations(all, Collections.<String>emptyList(), null);
	}

	/** A table backed by an injected cache, so a test can scan a fake world. */
	public static Locations of(List<Location> all, RegionScanCache cache) {
		return new Locations(all, Collections.<String>emptyList(), cache);
	}

	/** Drops the process-wide instance. Tests only; production never reloads the world mid-run. */
	static synchronized void reset() {
		instance = null;
	}

	/** Everything the table holds. */
	public List<Location> all() {
		return all;
	}

	/** Anything the loader had to skip, each naming its file and line. */
	public List<String> problems() {
		return problems;
	}

	// ---- families --------------------------------------------------------------------------

	/** Trees, rocks and fishing spots: the gatherable resources. */
	public Locator<Location> resources() {
		return family("resources", LocationKind.TREE, LocationKind.ROCK, LocationKind.FISHING);
	}

	/** Banks, ranges, anvils, altars, shops and masters. */
	public Locator<Location> services() {
		return family("services", LocationKind.BANK, LocationKind.COOKING, LocationKind.SMITHING,
				LocationKind.PRAYER, LocationKind.SHOP, LocationKind.MASTER);
	}

	public Locator<Location> trees() {
		return family("trees", LocationKind.TREE);
	}

	public Locator<Location> rocks() {
		return family("rocks", LocationKind.ROCK);
	}

	public Locator<Location> fishing() {
		return family("fishing", LocationKind.FISHING);
	}

	public Locator<Location> banks() {
		return family("banks", LocationKind.BANK);
	}

	public Locator<Location> shops() {
		return family("shops", LocationKind.SHOP);
	}

	public Locator<Location> teleports() {
		return family("teleports", LocationKind.TELEPORT);
	}

	public Locator<Location> monsters() {
		return family("monsters", LocationKind.MONSTER);
	}

	public Locator<Location> masters() {
		return family("masters", LocationKind.MASTER);
	}

	/** Every family at once. Curated only — there is nothing sensible to scan the whole world for. */
	public Locator<Location> everything() {
		return CuratedLocator.of(all);
	}

	/**
	 * The family for one kind, whatever it is.
	 *
	 * <p>Needed because a script names a kind, not a family method: a behaviour there is built from a
	 * {@code LocationKind} (so the same builder can say {@code walkToNearest(TREE)} and {@code
	 * walkToNearest(BANK)}), and this is the one place that turns a kind into a locator. It reuses
	 * {@link #family}, so the curated-then-scanned preference and the caching are the same as the named
	 * families rather than a second implementation of them.
	 */
	public Locator<Location> forKind(LocationKind kind) {
		if (kind == null) {
			throw new IllegalArgumentException("a kind is required");
		}
		return family("kind:" + kind.id(), kind);
	}

	/**
	 * A family view, built once and kept.
	 *
	 * <p>The scan arm is attached only when the family contains object kinds. A teleport has no object
	 * to scan for, so attaching one would cost a region scan per lookup to discover nothing.
	 */
	private Locator<Location> family(String key, LocationKind... kinds) {
		Locator<Location> existing = families.get(key);
		if (existing != null) {
			return existing;
		}
		Set<LocationKind> set = EnumSet.noneOf(LocationKind.class);
		Collections.addAll(set, kinds);
		CuratedLocator curated = CuratedLocator.filter(all, kinds);

		ScannedLocator scanned = null;
		if (cache != null && hasObjectKind(set)) {
			scanned = ScannedLocator.live(set, ScannedLocator.DEFAULT_MAX_RINGS, cache);
		}
		Locator<Location> built = new MergingLocator(curated, scanned);
		families.put(key, built);
		return built;
	}

	private static boolean hasObjectKind(Set<LocationKind> kinds) {
		for (LocationKind kind : kinds) {
			if (kind.isObjectKind()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "Locations(" + all.size() + " entries, cache=" + (cache == null ? "none" : "yes") + ")";
	}
}
