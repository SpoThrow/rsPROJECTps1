package server.game.bots.world;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Reads the authored {@code locations.cfg} — {@code BOT_LOCATIONS.md} A.3, the file the editor writes.
 *
 * <pre>
 * # comment
 * region = draynor_bank   x 3091 y 3241 w 6 h 5  plane 0  kind bank
 * point  = lumbridge_altar x 3243 y 3207          plane 0  kind prayer
 * region = varrock_oaks   x 3240 y 3241 w 23 h 19 plane 0  kind tree  tags oak
 * </pre>
 *
 * <p><b>A missing file is not an error, and neither is a bad line.</b> Two different failures get two
 * different treatments, and the difference is deliberate:
 *
 * <ul>
 * <li><b>No file at all</b> — the "not authored yet" case. The result is an empty list and no
 *     problems, so {@code Locations} falls through to {@link ScannedLocator} and a bot still works.
 *     That is the acceptance criterion in {@code BOT_LOCATIONS.md}: deleting the file degrades to
 *     scanning, not a crash.
 * <li><b>A file with a bad line</b> — a real authoring mistake. The line is skipped, and every skip
 *     is recorded in {@link Result#problems()} so it can be reported. Skipping rather than throwing is
 *     what keeps one typo from stopping the server; the problems list is what keeps it from being
 *     silent.
 * </ul>
 */
public final class LocationsConfig {

	/** Where the file lives by default, relative to the server directory. */
	public static final String DEFAULT_PATH = "Data/cfg/bots/locations.cfg";

	/** The loader's outcome: what it read, and what it had to skip. */
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

		/** One entry per line that could not be used, with its line number. */
		public List<String> problems() {
			return problems;
		}
	}

	private LocationsConfig() {
	}

	/** Reads {@link #DEFAULT_PATH}, relative to the process working directory. */
	public static Result load() {
		return load(java.nio.file.Paths.get(DEFAULT_PATH));
	}

	/**
	 * Reads {@code path}. A file that does not exist yields an empty, problem-free result; see the
	 * class comment for why that is not an error.
	 */
	public static Result load(Path path) {
		List<Location> locations = new ArrayList<Location>();
		List<String> problems = new ArrayList<String>();
		if (path == null || !Files.isRegularFile(path)) {
			return new Result(locations, problems);
		}
		List<String> lines;
		try {
			lines = Files.readAllLines(path, StandardCharsets.UTF_8);
		} catch (IOException e) {
			// The file exists but cannot be read: that is worth saying, unlike "not there yet".
			problems.add("cannot read " + path + ": " + e.getMessage());
			return new Result(locations, problems);
		}
		return parseLines(lines);
	}

	/** Parses one non-blank, comment-stripped line. */
	static Location parse(String line) {
		int equals = line.indexOf('=');
		if (equals < 0) {
			throw new IllegalArgumentException("expected 'key = name ...'");
		}
		String key = line.substring(0, equals).trim().toLowerCase(Locale.ROOT);
		boolean region = "region".equals(key);
		if (!region && !"point".equals(key)) {
			throw new IllegalArgumentException("unknown record '" + key + "' (expected region or point)");
		}

		List<String> tokens = tokenize(line.substring(equals + 1));
		if (tokens.isEmpty()) {
			throw new IllegalArgumentException("no name given");
		}
		String name = tokens.get(0);

		Integer x = null, y = null, plane = null, width = null, height = null;
		LocationKind kind = null;
		List<String> tags = new ArrayList<String>();

		int i = 1;
		while (i < tokens.size()) {
			String field = tokens.get(i).toLowerCase(Locale.ROOT);
			if ("tags".equals(field)) {
				// Everything after `tags` is the list; commas separate the values.
				for (int j = i + 1; j < tokens.size(); j++) {
					for (String tag : tokens.get(j).split(",")) {
						if (!tag.trim().isEmpty()) {
							tags.add(tag.trim());
						}
					}
				}
				break;
			}
			if (i + 1 >= tokens.size()) {
				throw new IllegalArgumentException("field '" + field + "' has no value");
			}
			String value = tokens.get(i + 1);
			switch (field) {
			case "x":
				x = integer(field, value);
				break;
			case "y":
				y = integer(field, value);
				break;
			case "plane":
				plane = integer(field, value);
				break;
			case "w":
			case "width":
				width = integer(field, value);
				break;
			case "h":
			case "height":
				height = integer(field, value);
				break;
			case "kind":
				kind = LocationKind.byId(value);
				if (kind == null) {
					throw new IllegalArgumentException("unknown kind '" + value + "'");
				}
				break;
			default:
				throw new IllegalArgumentException("unknown field '" + field + "'");
			}
			i += 2;
		}

		if (x == null || y == null) {
			throw new IllegalArgumentException("missing x or y");
		}
		if (kind == null) {
			throw new IllegalArgumentException("missing kind");
		}
		// `point` and `region` differ only in their default extent: a point is 1x1, a region must say
		// how big it is. Leaving w/h off a region is a mistake worth catching — the alternative is a
		// silent 1x1 that resolves a whole mining site to one tile.
		if (region && (width == null || height == null)) {
			throw new IllegalArgumentException("a region needs w and h");
		}
		return new Location(name, kind, x, y, plane == null ? 0 : plane,
				width == null ? 1 : width, height == null ? 1 : height, tags);
	}

	private static int integer(String field, String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(field + " is not a number: '" + value + "'");
		}
	}

	/** Splits on whitespace, treating a comma-separated token as one field. */
	private static List<String> tokenize(String value) {
		String[] parts = value.trim().split("\\s+");
		List<String> tokens = new ArrayList<String>(parts.length);
		for (String part : parts) {
			if (!part.isEmpty()) {
				tokens.add(part);
			}
		}
		return tokens;
	}

	/** Drops a trailing comment: {@code #} anywhere, or a line that starts with {@code //}. */
	private static String stripComment(String line) {
		String trimmed = line.trim();
		if (trimmed.startsWith("//")) {
			return "";
		}
		int hash = line.indexOf('#');
		return hash < 0 ? line : line.substring(0, hash);
	}

	/** Reads already-split lines, so a test can cover the parser without touching the disk. */
	public static Result parseLines(List<String> lines) {
		List<Location> locations = new ArrayList<Location>();
		List<String> problems = new ArrayList<String>();
		for (int i = 0; i < lines.size(); i++) {
			String raw = lines.get(i);
			String line = stripComment(raw).trim();
			if (line.isEmpty()) {
				continue;
			}
			try {
				locations.add(parse(line));
			} catch (IllegalArgumentException e) {
				problems.add("line " + (i + 1) + ": " + e.getMessage() + " — '" + raw.trim() + "'");
			}
		}
		return new Result(locations, problems);
	}

	/** The kinds the table can hold, for callers that want to validate a hand-written row list. */
	public static List<String> knownKindIds() {
		List<String> ids = new ArrayList<String>();
		for (LocationKind kind : LocationKind.values()) {
			ids.add(kind.id());
		}
		return Collections.unmodifiableList(Arrays.asList(ids.toArray(new String[0])));
	}

	/**
	 * The canonical one-line form of a location: the exact inverse of {@link #parse}.
	 *
	 * <p><b>Why the formatter lives here and not in the editor.</b> The map viewer lets an author drag
	 * a box and appends the result to this file ({@code BOT_ROADMAP.md} §5.2). If the browser owned
	 * the text, a change to the format would have to be made twice and the server's parser would be
	 * the only thing that noticed a mistake. Instead the browser sends the row it intends to write and
	 * the server re-parses it with {@link #parse}, re-formats it with this method, and appends
	 * <em>that</em> — so what lands in the file is always something this parser accepts.
	 */
	public static String toRow(Location location) {
		StringBuilder row = new StringBuilder();
		if (location.isPoint()) {
			row.append("point  = ");
		} else {
			row.append("region = ");
		}
		row.append(location.name()).append(" x ").append(location.x())
				.append(" y ").append(location.y());
		if (!location.isPoint()) {
			row.append(" w ").append(location.width()).append(" h ").append(location.height());
		}
		row.append(" plane ").append(location.plane())
				.append(" kind ").append(location.kind().id());
		if (!location.tags().isEmpty()) {
			row.append(" tags ").append(String.join(",", location.tags()));
		}
		return row.toString();
	}

	/** Canonical rows for a draft block, one per line, in the order given. */
	public static String toRows(List<Location> locations) {
		StringBuilder text = new StringBuilder();
		for (Location location : locations) {
			text.append(toRow(location)).append('\n');
		}
		return text.toString();
	}

	/**
	 * Appends rows to {@code path} as a new, dated, commented block, creating the file if it is not
	 * there. Returns how many rows were written.
	 *
	 * <p>Append-only on purpose: the editor cannot rewrite, reorder or delete an authored row, so a
	 * mistake it makes is additive and visible in a diff. Removing a place stays a deliberate edit to
	 * the file by hand.
	 */
	public static int appendRows(Path path, List<Location> locations, String note) throws IOException {
		if (locations.isEmpty()) {
			return 0;
		}
		StringBuilder block = new StringBuilder();
		block.append("\n# ---- appended from the Bot Workshop editor").append(note == null || note.isBlank()
				? "" : " (" + note + ")").append(" ----\n");
		block.append(toRows(locations));
		if (path.getParent() != null) {
			Files.createDirectories(path.getParent());
		}
		Files.write(path, block.toString().getBytes(StandardCharsets.UTF_8),
				java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
		return locations.size();
	}
}
