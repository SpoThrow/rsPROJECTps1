package server.clip.region;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authoritative OSRS collision footprints for scenery objects, read from {@code Data/objectSize.cfg}.
 *
 * <p><b>Why this exists.</b> The server's collision comes from {@link Region#addObject}, which asks
 * {@link ObjectDef} for {@code xLength}/{@code yLength} — the size the cache's {@code loc.dat} stores.
 * That data is incomplete: this cache carries <em>no size at all</em> for the overwhelming majority of
 * scenery, so those definitions read back as {@code 1x1}. Measured against the shipped table,
 * <b>2229 objects disagree and 2224 of them are objects that block movement</b> — trees, statues,
 * multi-cannons, gliders, stairs, gates and sails are all collidable on a single tile, which is why
 * walking through them looks wrong.
 *
 * <p>The correct footprints already exist in the repository as {@code Data/objectSize.cfg} — 8520
 * entries of {@code objectId, name, size, examine} — but nothing read it. This class is the reader.
 *
 * <p><b>Precedence.</b> The table wins where it has an entry; otherwise the caller's fallback (normally
 * the cache size) is used. Both directions of disagreement are honoured, so an object the cache has
 * over-sized is also corrected.
 *
 * <p><b>Client parity.</b> The client builds its own collision from the same {@code loc.dat} sizes
 * ({@code Class11.method212}), so enabling this server-side without a matching client change makes the
 * two disagree about the same tiles. See {@code Config.USE_OBJECT_SIZE_TABLE} for the switch and the
 * plan for the client-side follow-up.
 */
public final class ObjectSizes {

	/** Where the table lives, resolved against the server working directory like every other Data file. */
	public static final Path DEFAULT_PATH = Paths.get("./Data/objectSize.cfg");

	/** Lazily-loaded table for {@link #DEFAULT_PATH}; {@code null} until first use. */
	private static volatile ObjectSizes defaultInstance;

	/** objectId to {width, height}. Both are at least 1. */
	private final Map<Integer, int[]> sizes;

	private ObjectSizes(Map<Integer, int[]> sizes) {
		this.sizes = sizes;
	}

	/** A table with no overrides — every lookup returns its fallback. */
	public static ObjectSizes empty() {
		return new ObjectSizes(Collections.<Integer, int[]>emptyMap());
	}

	/** Builds a table from already-parsed entries. */
	public static ObjectSizes of(Map<Integer, int[]> sizes) {
		return new ObjectSizes(new HashMap<Integer, int[]>(sizes));
	}

	/**
	 * The footprint width for {@code objectId}, or {@code fallback} when the table has no entry.
	 */
	public int width(int objectId, int fallback) {
		int[] size = sizes.get(objectId);
		return size == null ? fallback : size[0];
	}

	/**
	 * The footprint height for {@code objectId}, or {@code fallback} when the table has no entry.
	 */
	public int height(int objectId, int fallback) {
		int[] size = sizes.get(objectId);
		return size == null ? fallback : size[1];
	}

	/** True when the table has an explicit footprint for {@code objectId}. */
	public boolean has(int objectId) {
		return sizes.containsKey(objectId);
	}

	/** How many objects the table covers. */
	public int count() {
		return sizes.size();
	}

	/**
	 * Parses the {@code objectSize.cfg} line format:
	 *
	 * <pre>objectId = 1276&#9;Tree&#9;&#9;&#9;&#9;&#9;2x2&#9;"One of the most common trees in RuneScape."</pre>
	 *
	 * <p>The size is the first tab-separated field that is exactly {@code NxM}. Taking the first match
	 * matters: the name precedes it, and the examine text (last field) may embed numbers that look like
	 * a size. Unparseable lines are skipped rather than throwing, so hand-editing the file cannot stop
	 * the server from booting.
	 */
	public static ObjectSizes parse(List<String> lines) {
		Map<Integer, int[]> parsed = new HashMap<Integer, int[]>();
		for (String line : lines) {
			if (line == null || !line.startsWith("objectId = ")) {
				continue;
			}
			int equals = line.indexOf('=');
			String rest = line.substring(equals + 1).trim();
			int firstTab = rest.indexOf('\t');
			if (firstTab < 0) {
				continue;
			}
			int objectId;
			try {
				objectId = Integer.parseInt(rest.substring(0, firstTab).trim());
			} catch (NumberFormatException e) {
				continue;
			}
			if (objectId < 0) {
				continue;
			}
			for (String field : rest.split("\t")) {
				String candidate = field.trim();
				int x = candidate.indexOf('x');
				if (x <= 0 || x != candidate.lastIndexOf('x') || x == candidate.length() - 1) {
					continue;
				}
				try {
					int width = Integer.parseInt(candidate.substring(0, x));
					int height = Integer.parseInt(candidate.substring(x + 1));
					parsed.put(objectId, new int[] { Math.max(1, width), Math.max(1, height) });
					break;
				} catch (NumberFormatException e) {
					// Not a size field; keep looking across the remaining columns.
				}
			}
		}
		return new ObjectSizes(parsed);
	}

	/**
	 * Reads the table, returning {@link #empty()} (never {@code null}) if it is missing or unreadable so
	 * the caller silently falls back to cache sizes.
	 */
	public static ObjectSizes load(Path path) {
		if (path == null || !Files.isRegularFile(path)) {
			System.out.println("[ObjectSizes] " + path + " not found — using cache loc.dat sizes");
			return empty();
		}
		try {
			ObjectSizes loaded = parse(Files.readAllLines(path, StandardCharsets.UTF_8));
			System.out.println("[ObjectSizes] Loaded " + loaded.count() + " object footprints from " + path);
			return loaded;
		} catch (Exception e) {
			System.out.println("[ObjectSizes] Failed to read " + path + " (" + e.getClass().getSimpleName()
					+ ") — using cache loc.dat sizes");
			return empty();
		}
	}

	/** The shared table, loaded from {@link #DEFAULT_PATH} on first use. */
	public static ObjectSizes get() {
		ObjectSizes instance = defaultInstance;
		if (instance == null) {
			synchronized (ObjectSizes.class) {
				instance = defaultInstance;
				if (instance == null) {
					instance = load(DEFAULT_PATH);
					defaultInstance = instance;
				}
			}
		}
		return instance;
	}
}
