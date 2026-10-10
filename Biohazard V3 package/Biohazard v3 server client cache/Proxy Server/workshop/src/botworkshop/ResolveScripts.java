package botworkshop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import server.game.bots.meta.BotNodeRegistry;
import server.game.bots.meta.NodeParam;
import server.game.bots.meta.NodeSchema;
import server.game.bots.script.ScriptDocument;
import server.game.bots.world.Location;
import server.game.bots.world.LocationKind;
import server.game.bots.world.LocationsData;

/**
 * Answers the one question a script's {@code kind} parameters raise that no other check answers:
 * <em>does the place this names exist?</em> — {@code BOT_TOOLING.md} T6b.
 *
 * <p><b>Why this is needed.</b> {@code workshopValidateScripts} proves the server can <em>load</em> a
 * document, and {@code workshopValidate} proves the authored boxes in {@code locations.cfg} contain the
 * objects they claim. Neither says whether {@code gather(rock)} can find a rock: the schema does not
 * know the world, and the location table says nothing about a kind an author asked for and never
 * authored a row for. Until this ran, the only way to find out was to spawn the bot and watch it —
 * which is what "a manual check" meant in T6's notes. The failure is quiet by nature: a bot that finds
 * nothing logs one note and fails, and a bot with no such leaf loops forever.
 *
 * <p><b>Two oracles, and the cheap one comes first.</b> A kind resolves if either
 *
 * <ol>
 * <li>{@link LocationsData} produces at least one place of that kind — the authored rows plus the
 *     teleport, shop and monster joins — which needs only {@code Data/cfg} and no world at all; or
 * <li>the world contains an object of that kind, which needs the whole world counted
 *     ({@link WorldCensus}).
 * </ol>
 *
 * <p>The world is loaded <em>only when an answer depends on it</em> — when some named kind has no place
 * in the table. A script naming {@code tree} and {@code bank} is answered from {@code Data/cfg} alone
 * and stays instant, which is the common case; a script naming {@code rock} or {@code cooking} pays for
 * the census, because that is exactly the case where the answer is in doubt. {@code --world} forces the
 * census so an author can also see the whole-world counts.
 *
 * <p><b>Kinds come off the schema.</b> The document is walked through {@link BotNodeRegistry}, and every
 * parameter the schema calls a {@link server.game.bots.meta.ParamType#KIND} is read — so a node that
 * gains a kind parameter is checked the day it is annotated, and no node id or field name is written
 * down here. That is the same rule the palette ({@code §7.2}) and the loader ({@code §7.1}) follow.
 *
 * <p><b>Only a definite failure fails.</b> A kind with no authored place is not an error when the world
 * has objects of it — {@code Locations} falls through to a world scan, and {@code locations.cfg} says
 * as much about rocks and fishing spots. That is reported as {@link Verdict#SCAN_ONLY} with the reason,
 * not as a failure. A kind neither the table nor the world has is a {@link Verdict#NOT_FOUND} and exits
 * non-zero, because there is no reading of the script under which the bot clicks anything.
 *
 * <pre>gradlew workshopResolveScripts            # worlds loaded only where needed
 * gradlew workshopResolveScripts -PworkshopCensus   # also count every kind</pre>
 */
public final class ResolveScripts {

	/** Where the documents are; the loader's constant rather than a second copy of the path. */
	private static final String DIR = ScriptDocument.DIR;
	private static final String EXTENSION = ScriptDocument.EXTENSION;

	/** {@code --world}: count the world even when the table already answers. */
	private static final String FORCE_WORLD = "--world";

	/** The three answers a kind can get. */
	public enum Verdict {

		/** A place of this kind is in the table, so a locator answers without scanning. */
		RESOLVES,

		/**
		 * No place of this kind is authored or joined, but the world has objects of it, so a locator
		 * finds one by scanning a box around wherever the bot happens to be.
		 */
		SCAN_ONLY,

		/** Neither the table nor the world has it. The script cannot work. */
		NOT_FOUND
	}

	/**
	 * The kinds one document names, and which nodes named them.
	 *
	 * <p>Insertion-ordered and counted rather than deduplicated: "three leaves ask for tree" is worth
	 * seeing, and the report is read by a person deciding what to change.
	 */
	public static final class Wanted {

		private final Map<LocationKind, Map<String, Integer>> byKind =
				new LinkedHashMap<LocationKind, Map<String, Integer>>();

		/** Records that {@code node} asked for {@code kind}. */
		public void add(LocationKind kind, String node) {
			Map<String, Integer> nodes = byKind.get(kind);
			if (nodes == null) {
				nodes = new LinkedHashMap<String, Integer>();
				byKind.put(kind, nodes);
			}
			Integer seen = nodes.get(node);
			nodes.put(node, seen == null ? 1 : seen + 1);
		}

		/** The kinds asked for, in the order they appear in the document. */
		public Set<LocationKind> kinds() {
			return byKind.keySet();
		}

		/** The nodes that asked for {@code kind}, and how many times each did. */
		public Map<String, Integer> nodes(LocationKind kind) {
			Map<String, Integer> nodes = byKind.get(kind);
			return nodes == null ? new LinkedHashMap<String, Integer>() : nodes;
		}

		/** {@code walk_to_nearest x2, gather x1} — the node counts, for the report. */
		public String describe(LocationKind kind) {
			StringBuilder out = new StringBuilder();
			for (Map.Entry<String, Integer> entry : nodes(kind).entrySet()) {
				if (out.length() > 0) {
					out.append(", ");
				}
				out.append(entry.getKey());
				if (entry.getValue() > 1) {
					out.append(" x").append(entry.getValue());
				}
			}
			return out.toString();
		}
	}

	private ResolveScripts() {
	}

	/**
	 * The kinds a document names, read off the schema.
	 *
	 * @throws server.game.bots.script.Json.JsonException if the text is not JSON
	 */
	public static Wanted wanted(String json) {
		return wanted(asDocument(server.game.bots.script.Json.parse(json)));
	}

	/** The kinds a parsed document names. */
	public static Wanted wanted(Map<String, Object> document) {
		Wanted wanted = new Wanted();
		collect(document.get("root"), wanted);
		return wanted;
	}

	/** One node: its own kind parameters, then its children. */
	private static void collect(Object value, Wanted wanted) {
		if (!(value instanceof Map)) {
			// The loader rejected this document, so a caller that got here already skipped it.
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> node = (Map<String, Object>) value;
		Object idValue = node.get("node");
		if (!(idValue instanceof String)) {
			return;
		}
		String id = (String) idValue;
		NodeSchema schema = BotNodeRegistry.schemaById(id);
		if (schema == null) {
			return;
		}
		for (NodeParam param : schema.params()) {
			if (!node.containsKey(param.name())) {
				// Omitted, so the node's own default applies. A defaulted kind is still a kind the
				// script asks for, but it is the node's choice rather than the author's, so it is
				// reported under the node either way — the answer is the same.
				continue;
			}
			Object paramValue = node.get(param.name());
			switch (param.type()) {
			case KIND:
				LocationKind kind = paramValue instanceof String
						? LocationKind.byId((String) paramValue) : null;
				if (kind != null) {
					wanted.add(kind, id);
				}
				break;
			case NODE:
				collect(paramValue, wanted);
				break;
			case NODE_LIST:
				if (paramValue instanceof List) {
					for (Object child : (List<?>) paramValue) {
						collect(child, wanted);
					}
				}
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Whether a kind works, from the two counts. The whole rule, so it can be checked without a world.
	 *
	 * <p><b>A table row wins outright.</b> A place of this kind is in the table, so a locator answers
	 * without scanning and the script has somewhere to go. Whether that place is <em>worth</em> going to
	 * — a box an author drew around objects that are no longer there — is a different question and a
	 * different command: {@code workshopValidate} scans every authored box and fails on an empty one.
	 * Answering it here as well would make this report disagree with that one.
	 *
	 * <p><b>So only a kind with no row depends on the count</b>, which is exactly when
	 * {@link #main} pays to load the world. When a kind has no row and is nothing the world classifies,
	 * no scan can find it either and the table is the only oracle there could have been.
	 *
	 * @param curatedRows  places of this kind the table holds (authored rows plus joins)
	 * @param worldObjects objects of this kind the world holds; read only when {@code curatedRows} is 0
	 */
	public static Verdict verdict(LocationKind kind, long curatedRows, long worldObjects) {
		if (curatedRows > 0) {
			return Verdict.RESOLVES;
		}
		if (!kind.isObjectKind()) {
			// Nothing classifies an object as this, so the table was the only possible answer.
			return Verdict.NOT_FOUND;
		}
		// An object kind with no place in the table: a scan is the only remaining way to find one, and
		// only a counted world can say whether there is one to find.
		return worldObjects > 0 ? Verdict.SCAN_ONLY : Verdict.NOT_FOUND;
	}

	public static void main(String[] args) throws IOException {
		boolean forceWorld = false;
		for (String arg : args) {
			if (FORCE_WORLD.equals(arg)) {
				forceWorld = true;
			}
		}

		Path dir = Paths.get(".").toAbsolutePath().normalize().resolve(DIR);
		System.out.println();
		System.out.println("[resolve] " + dir);

		LocationsData.Result table = LocationsData.load();
		Map<LocationKind, Integer> curated = countByKind(table.locations());
		System.out.println("[resolve] the table holds " + table.locations().size() + " place(s) for "
				+ curated.size() + " kind(s)");
		for (String problem : table.problems()) {
			System.out.println("[resolve]   unreadable " + problem);
		}

		if (!Files.isDirectory(dir)) {
			// A server with no authored scripts. Nothing to resolve, and not a failure.
			System.out.println("[resolve] no authored scripts (the directory does not exist)");
			return;
		}

		List<Path> files;
		try (Stream<Path> stream = Files.list(dir)) {
			files = stream.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)
							.endsWith(EXTENSION))
					.sorted()
					.collect(Collectors.toList());
		}
		if (files.isEmpty()) {
			System.out.println("[resolve] no authored scripts (the directory is empty)");
			return;
		}

		// Parse every document first: whether the world is worth loading depends on what they name.
		List<String> names = new ArrayList<String>();
		Map<String, Wanted> wantedByScript = new LinkedHashMap<String, Wanted>();
		int bad = 0;
		for (Path file : files) {
			String name = ScriptDocument.nameOfFile(file.getFileName().toString());
			try {
				String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
				// Through the server's loader as well as the walk, so "this resolves" is only ever said
				// about a document the server would accept. Not this command's job to report schema
				// errors in detail — workshopValidateScripts is.
				ScriptDocument.fromJson(name, text);
				names.add(name);
				wantedByScript.put(name, wanted(text));
			} catch (RuntimeException e) {
				System.out.println("[resolve]   BAD   " + name + "  " + e.getMessage());
				bad++;
			}
		}

		boolean worldNeeded = forceWorld || needsWorld(wantedByScript.values(), curated);
		WorldCensus census = null;
		if (worldNeeded) {
			System.out.println("[resolve] counting the world (Data/world)…");
			census = WorldCensus.load();
			System.out.println("[resolve] the world holds " + census.objects() + " object(s) across "
					+ census.regions() + " region(s)");
		} else {
			System.out.println("[resolve] the world was not counted: every kind named has a place in "
					+ "the table (use --world to count it anyway)");
		}

		int notFound = 0;
		for (String name : names) {
			Wanted wanted = wantedByScript.get(name);
			System.out.println("[resolve] " + DIR + "/" + name + EXTENSION);
			if (wanted.kinds().isEmpty()) {
				System.out.println("[resolve]   names no kind of place — nothing to resolve");
				continue;
			}
			for (LocationKind kind : wanted.kinds()) {
				long rows = curated.containsKey(kind) ? curated.get(kind) : 0;
				boolean counted = census != null;
				long objects = counted ? census.objectsOf(kind) : 0;
				Verdict verdict = verdict(kind, rows, objects);
				System.out.println("[resolve]   " + pad(kind.id(), 9)
						+ " rows " + pad(String.valueOf(rows), 5)
						+ " world objects " + pad(counted ? String.valueOf(objects) : "-", 8)
						+ " " + describe(verdict)
						+ "   (" + wanted.describe(kind) + ")");
				if (verdict == Verdict.NOT_FOUND) {
					notFound++;
				}
			}
		}

		System.out.println("[resolve] " + names.size() + " script(s), " + notFound
				+ " kind(s) that resolve nowhere, " + bad + " unreadable");
		if (notFound > 0) {
			System.out.println("[resolve] a kind that resolves nowhere means the bot never clicks "
					+ "anything: author a locations.cfg row for it, or point the script at a kind the "
					+ "world has");
		}
		if (bad > 0) {
			System.out.println("[resolve] an unreadable script is skipped at boot (see "
					+ "workshopValidateScripts)");
		}
		if (notFound > 0 || bad > 0) {
			System.exit(1);
		}
	}

	/** True when some named kind has no place in the table and is one the world could answer about. */
	private static boolean needsWorld(Iterable<Wanted> wantedByScript, Map<LocationKind, Integer> curated) {
		for (Wanted wanted : wantedByScript) {
			for (LocationKind kind : wanted.kinds()) {
				if (kind.isObjectKind() && !curated.containsKey(kind)) {
					return true;
				}
			}
		}
		return false;
	}

	private static String describe(Verdict verdict) {
		switch (verdict) {
		case RESOLVES:
			return "ok";
		case SCAN_ONLY:
			return "ok by scan only — no authored place names it, so a bot finds one only inside the "
					+ "box it scans from wherever it stands";
		default:
			return "NOT FOUND — nothing in Data/cfg or the world is one, so no leaf can find it";
		}
	}

	/** Places of each kind the table holds. */
	private static Map<LocationKind, Integer> countByKind(List<Location> locations) {
		Map<LocationKind, Integer> counts = new TreeMap<LocationKind, Integer>();
		for (Location location : locations) {
			Integer seen = counts.get(location.kind());
			counts.put(location.kind(), seen == null ? 1 : seen + 1);
		}
		return counts;
	}

	private static String pad(String value, int width) {
		StringBuilder out = new StringBuilder(value);
		while (out.length() < width) {
			out.append(' ');
		}
		return out.toString();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asDocument(Object parsed) {
		if (!(parsed instanceof Map)) {
			throw new IllegalArgumentException("a script document must be a JSON object, got "
					+ (parsed == null ? "null" : parsed.getClass().getSimpleName()));
		}
		return (Map<String, Object>) parsed;
	}
}
