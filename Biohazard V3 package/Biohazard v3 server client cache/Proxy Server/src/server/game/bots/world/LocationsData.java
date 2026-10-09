package server.game.bots.world;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the curated table from every file the server already owns — {@code BOT_LOCATIONS.md} A.3.
 *
 * <p><b>The joins, and why they are joins.</b> Teleports, shops and monsters already exist in
 * {@code Data/cfg}. Copying them into {@code locations.cfg} would create a second source of truth for
 * facts the server already has, and the two would drift the first time someone changed a shop. So this
 * class derives them instead:
 *
 * <table>
 * <tr><td>{@code teleports.cfg}</td><td>{@code teleport = Category Name X Y}</td></tr>
 * <tr><td>shops</td><td>{@code npc-shops.cfg} (npc → shop) joined to {@code shops.cfg} (shop → name)
 *     and {@code spawn-config.cfg} (npc → tile)</td></tr>
 * <tr><td>monsters</td><td>{@code spawn-config.cfg} (npc, tile) joined to {@code npc.cfg} (npc → name)</td></tr>
 * </table>
 *
 * <p>Only {@code locations.cfg} holds anything a file cannot already answer: the human names and tags
 * for resource regions ("draynor_willows", tag {@code willow}) that an author chose. That is exactly
 * what the editor writes.
 *
 * <p>Each file is optional. A missing one contributes nothing and is <em>not</em> an error, matching
 * {@link LocationsConfig}: the server has to keep running with whatever is on disk.
 */
public final class LocationsData {

	/** Everything read, plus anything that had to be skipped. */
	public static final class Result {
		private final List<Location> locations;
		private final List<String> problems;

		Result(List<Location> locations, List<String> problems) {
			this.locations = Collections.unmodifiableList(locations);
			this.problems = Collections.unmodifiableList(problems);
		}

		public List<Location> locations() {
			return locations;
		}

		public List<String> problems() {
			return problems;
		}
	}

	private LocationsData() {
	}

	/** Reads every source under {@code cfgDir} (usually {@code Data/cfg}). */
	public static Result load(Path cfgDir) {
		List<Location> all = new ArrayList<Location>();
		List<String> problems = new ArrayList<String>();

		// The authored table. Usually Data/cfg/bots/locations.cfg, but a flat Data/cfg/locations.cfg is
		// accepted too: one of the two design docs placed it at each path, and reading both costs
		// nothing while either could be what an author produced.
		LocationsConfig.Result authored = LocationsConfig.load(cfgDir.resolve("bots/locations.cfg"));
		all.addAll(authored.locations());
		problems.addAll(authored.problems());
		LocationsConfig.Result flat = LocationsConfig.load(cfgDir.resolve("locations.cfg"));
		all.addAll(flat.locations());
		problems.addAll(flat.problems());

		all.addAll(teleports(cfgDir, problems));
		all.addAll(shops(cfgDir, problems));
		all.addAll(monsters(cfgDir, problems));
		return new Result(all, problems);
	}

	/** {@code Data/cfg} relative to the process working directory. */
	public static Result load() {
		return load(Paths.get("Data/cfg"));
	}

	// ---- teleports -------------------------------------------------------------------------

	private static List<Location> teleports(Path cfgDir, List<String> problems) {
		List<Location> out = new ArrayList<Location>();
		for (String line : lines(cfgDir.resolve("teleports.cfg"), problems)) {
			String body = after(line, "teleport");
			if (body == null) {
				continue;
			}
			List<String> tokens = tokens(body);
			// "Modern White Wolf Mountain 2848 3498": the name can be several words, so the numbers
			// are peeled off the end rather than counted from the front.
			if (tokens.size() < 4) {
				problems.add("teleports.cfg: too few fields — '" + line.trim() + "'");
				continue;
			}
			Integer x = number(tokens.get(tokens.size() - 2));
			Integer y = number(tokens.get(tokens.size() - 1));
			if (x == null || y == null) {
				problems.add("teleports.cfg: no coordinates — '" + line.trim() + "'");
				continue;
			}
			String category = tokens.get(0);
			StringBuilder name = new StringBuilder();
			for (int i = 1; i < tokens.size() - 2; i++) {
				if (name.length() > 0) {
					name.append(' ');
				}
				name.append(tokens.get(i));
			}
			out.add(new Location(slug(category + "_" + name), LocationKind.TELEPORT, x, y, 0, 1, 1,
					singleton(category.toLowerCase(Locale.ROOT))));
		}
		return out;
	}

	// ---- shops -----------------------------------------------------------------------------

	private static List<Location> shops(Path cfgDir, List<String> problems) {
		Map<Integer, String> shopNames = shopNames(cfgDir, problems);
		Map<Integer, int[]> npcTiles = spawnTiles(cfgDir, problems);
		List<Location> out = new ArrayList<Location>();
		for (String line : lines(cfgDir.resolve("npc-shops.cfg"), problems)) {
			String body = after(line, "npc-shop");
			if (body == null) {
				continue;
			}
			List<String> tokens = tokens(body);
			if (tokens.size() < 2) {
				problems.add("npc-shops.cfg: expected 'npcId shopId' — '" + line.trim() + "'");
				continue;
			}
			Integer npcId = number(tokens.get(0));
			Integer shopId = number(tokens.get(1));
			if (npcId == null || shopId == null) {
				continue;
			}
			int[] tile = npcTiles.get(npcId);
			if (tile == null) {
				// The shop exists but its keeper has no spawn, so there is nowhere to walk to. Skipped
				// rather than guessed at — a bot sent to a made-up tile is worse than one that knows
				// this shop has no reachable location.
				continue;
			}
			String shopName = shopNames.get(shopId);
			String name = shopName == null ? "shop_" + shopId : slug(shopName);
			out.add(new Location(name, LocationKind.SHOP, tile[0], tile[1], tile[2], 1, 1,
					asList("shop:" + shopId, "npc:" + npcId)));
		}
		return out;
	}

	private static Map<Integer, String> shopNames(Path cfgDir, List<String> problems) {
		Map<Integer, String> names = new HashMap<Integer, String>();
		for (String line : lines(cfgDir.resolve("shops.cfg"), problems)) {
			String body = after(line, "shop");
			if (body == null) {
				continue;
			}
			// shop = <id> <Name> <sell> <buy> <item> <amount> ...
			List<String> tokens = tokens(body);
			if (tokens.size() < 2) {
				continue;
			}
			Integer id = number(tokens.get(0));
			if (id != null) {
				names.put(id, tokens.get(1));
			}
		}
		return names;
	}

	// ---- monsters --------------------------------------------------------------------------

	private static List<Location> monsters(Path cfgDir, List<String> problems) {
		Map<Integer, String> npcNames = npcNames(cfgDir, problems);
		Map<Integer, int[]> tiles = spawnTiles(cfgDir, problems);
		List<Location> out = new ArrayList<Location>();
		for (Map.Entry<Integer, int[]> entry : tiles.entrySet()) {
			int[] tile = entry.getValue();
			String name = npcNames.get(entry.getKey());
			if (name == null) {
				// spawn-config carries a name column, but it is not the canonical one and is often a
				// tag rather than a name; npc.cfg is the authority, so an id it does not know is
				// skipped rather than labelled with whatever the spawn line said.
				continue;
			}
			// Names repeat (every "Man" is a separate spawn), so the tile is part of the name to keep
			// it unique. byName therefore returns the first spawn of that name; nearest() is the
			// family's real lookup and does not care.
			out.add(new Location(slug(name) + "@" + tile[0] + "," + tile[1], LocationKind.MONSTER,
					tile[0], tile[1], tile[2], 1, 1, singleton("npc:" + entry.getKey())));
		}
		return out;
	}

	private static Map<Integer, String> npcNames(Path cfgDir, List<String> problems) {
		Map<Integer, String> names = new HashMap<Integer, String>();
		for (String line : lines(cfgDir.resolve("npc.cfg"), problems)) {
			String body = after(line, "npc");
			if (body == null) {
				continue;
			}
			// npc = <id> <Name> <combat> <health>
			List<String> tokens = tokens(body);
			if (tokens.size() < 2) {
				continue;
			}
			Integer id = number(tokens.get(0));
			if (id != null) {
				names.put(id, tokens.get(1));
			}
		}
		return names;
	}

	/**
	 * npcId → {x, y, plane} from {@code spawn-config.cfg}.
	 *
	 * <p>The first spawn of an id wins. A monster can be spawned in several places; the table holds one
	 * tile per id so a curated lookup stays a single row rather than becoming a list keyed by id, and
	 * the scanned path still covers the rest of the world's spawns.
	 */
	private static Map<Integer, int[]> spawnTiles(Path cfgDir, List<String> problems) {
		Map<Integer, int[]> tiles = new HashMap<Integer, int[]>();
		for (String line : lines(cfgDir.resolve("spawn-config.cfg"), problems)) {
			String body = after(line, "spawn");
			if (body == null) {
				continue;
			}
			// spawn = <npcId> <x> <y> <walkType> ... <name>
			List<String> tokens = tokens(body);
			if (tokens.size() < 3) {
				continue;
			}
			Integer id = number(tokens.get(0));
			Integer x = number(tokens.get(1));
			Integer y = number(tokens.get(2));
			if (id == null || x == null || y == null) {
				continue;
			}
			if (!tiles.containsKey(id)) {
				tiles.put(id, new int[] { x, y, 0 });
			}
		}
		return tiles;
	}

	// ---- helpers ---------------------------------------------------------------------------

	/**
	 * The lines of a file, or nothing when it is absent.
	 *
	 * <p>Absent is not reported as a problem: every one of these files is optional here, and a missing
	 * one means "that family resolves to nothing", which is a legitimate state.
	 */
	private static List<String> lines(Path path, List<String> problems) {
		if (path == null || !Files.isRegularFile(path)) {
			return Collections.emptyList();
		}
		try {
			return Files.readAllLines(path, StandardCharsets.UTF_8);
		} catch (IOException e) {
			problems.add("cannot read " + path + ": " + e.getMessage());
			return Collections.emptyList();
		}
	}

	/** The text after {@code key =} on a line, or {@code null} when the line is a comment or another key. */
	private static String after(String line, String key) {
		String trimmed = line.trim();
		if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("#")) {
			return null;
		}
		int equals = trimmed.indexOf('=');
		if (equals < 0) {
			return null;
		}
		if (!trimmed.substring(0, equals).trim().equalsIgnoreCase(key)) {
			return null;
		}
		return trimmed.substring(equals + 1).trim();
	}

	private static List<String> tokens(String value) {
		List<String> out = new ArrayList<String>();
		for (String part : value.split("[\\s]+")) {
			if (!part.isEmpty()) {
				out.add(part);
			}
		}
		return out;
	}

	private static Integer number(String value) {
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String slug(String value) {
		return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
				.replaceAll("^_+|_+$", "");
	}

	private static List<String> singleton(String value) {
		return Collections.singletonList(value);
	}

	private static List<String> asList(String... values) {
		List<String> out = new ArrayList<String>(values.length);
		Collections.addAll(out, values);
		return out;
	}
}
