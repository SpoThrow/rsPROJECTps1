package botworkshop;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import botworkshop.data.MapIndex;
import server.clip.region.ObjectDef;
import server.clip.region.Region;
import server.game.bots.world.LocationKind;
import server.game.bots.world.ResourceKinds;
import server.game.objects.Objects;

/**
 * How much of each kind the world actually contains — the fact a script's {@code kind} parameters are
 * checked against ({@code BOT_TOOLING.md} T6b).
 *
 * <p><b>The whole world, not a box.</b> {@link ResolveScripts} has to answer "does {@code gather(rock)}
 * resolve anywhere?" and the honest answer is a census of every region on disk. A local scan cannot
 * answer it: the nearest rock to a bot standing in Draynor is 600 tiles away, and a script that names
 * a kind only the far side of the map has is a script that fails at the first click. Loading the world
 * once and counting everything is slower and says something unambiguous.
 *
 * <p><b>Classified by the runtime's table.</b> {@link ResourceKinds} is the same classifier
 * {@code ScannedLocator} and {@code ResourceScan} use to decide what a bot may click, so "the world has
 * 412 trees" here means 412 objects a {@code gather(tree)} leaf would accept. The tool's own
 * {@code ResourceRules} wraps a second decode of {@code loc.dat} for the exporter; using it here would
 * risk census and runtime disagreeing about exactly the thing being checked.
 *
 * <p><b>Counted per object id, classified after.</b> The pass over the world only accumulates
 * {@code objectId -> count}, because classification needs {@link ObjectDef#getObjectDef} and that
 * caches twenty definitions at a time — classifying inside the loop would re-parse each definition for
 * every one of the world's ~1.9 million objects. The distinct ids are classified once, at the end,
 * which is the same order {@code ValidateMap} uses for the same reason.
 *
 * <p><b>Cheap to fake, because the check must be testable without a world.</b> The constructor is
 * public and takes the counts, so {@code ResolveScriptsTest} can ask about a world it made up without
 * loading 1226 regions. {@link #load} is the only part that touches {@code Data}.
 */
public final class WorldCensus {

	private final long[] byKind;
	private final long objects;
	private final int regions;

	private WorldCensus(long[] byKind, long objects, int regions) {
		this.byKind = byKind;
		this.objects = objects;
		this.regions = regions;
	}

	/** A census over counts already known, for tests and for a caller that has its own world. */
	public static WorldCensus of(long[] byKind, long objects, int regions) {
		return new WorldCensus(byKind.clone(), objects, regions);
	}

	/**
	 * Counts the world on disk: {@code Data/world/map_index} plus every region file it names.
	 *
	 * @throws IOException if the region directory cannot be read at all, which is a broken checkout
	 *                     rather than "a world with nothing in it"
	 */
	public static WorldCensus load() throws IOException {
		Path mapIndex = Paths.get("Data/world/map_index");
		ObjectDef.loadConfig();
		Region.load();

		Map<Integer, Integer> countsByObjectId = new HashMap<Integer, Integer>();
		Set<Integer> seen = new HashSet<Integer>();
		int regions = 0;
		long objects = 0;
		for (MapIndex.Entry entry : MapIndex.read(mapIndex)) {
			if (!seen.add(entry.regionId)) {
				// Region.load keys regions by id, so a repeated directory entry is one region and
				// counting it twice would inflate every kind.
				continue;
			}
			Region region = Region.getRegion(entry.baseX(), entry.baseY());
			if (region == null) {
				continue;
			}
			boolean any = false;
			for (Objects object : region.realObjects) {
				if (object == null || object.objectId < 0) {
					// A negative id is a removed object's tombstone (Region.addObject leaves it).
					continue;
				}
				objects++;
				any = true;
				Integer count = countsByObjectId.get(object.objectId);
				countsByObjectId.put(object.objectId, count == null ? 1 : count + 1);
			}
			if (any) {
				regions++;
			}
		}

		long[] byKind = new long[LocationKind.values().length];
		for (Map.Entry<Integer, Integer> entry : countsByObjectId.entrySet()) {
			String kindId = ResourceKinds.classify(ObjectDef.getObjectDef(entry.getKey()));
			if (kindId == null) {
				// Ordinary scenery: most of the world. Not a problem and not a kind.
				continue;
			}
			LocationKind kind = LocationKind.byId(kindId);
			if (kind == null) {
				// ResourceKinds named a kind LocationKind does not have, which is the drift the two
				// tables exist to prevent. Counting it as nothing would hide it, so it is loud.
				System.out.println("[resolve] note: the world classifier returned kind \"" + kindId
						+ "\" for object " + entry.getKey() + ", which is not a LocationKind");
				continue;
			}
			byKind[kind.ordinal()] += entry.getValue();
		}
		return new WorldCensus(byKind, objects, regions);
	}

	/** Objects of {@code kind} in the world, or 0 when the world has none. */
	public long objectsOf(LocationKind kind) {
		return byKind[kind.ordinal()];
	}

	/** Every object counted, whatever its kind. */
	public long objects() {
		return objects;
	}

	/** Regions that hold at least one object. */
	public int regions() {
		return regions;
	}

	/** The count for every kind, in {@link LocationKind}'s order, for a caller that wants the lot. */
	public long[] byKind() {
		return byKind.clone();
	}
}
