package server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime configuration read from {@code Data/server.properties}.
 *
 * <p><b>Why this exists.</b> Phase 6 of the refactor calls for getting configuration out of the
 * source. The sharpest instance is credentials: the vote loader's database password is a string
 * literal in {@link Server}'s static initialiser and the hiscores password is a local variable in
 * {@link server.game.players.HiscoresHandler#createConnection()}, so anyone with read access to the
 * repository — or to a built jar — has them.
 *
 * <p><b>Fallback semantics are the important part.</b> Every getter takes the value to use when the
 * file or the key is absent, and the defaults passed by the two call sites are exactly the strings
 * that used to be hardcoded. So on a checkout with <em>no</em> {@code server.properties} the server
 * behaves identically to before this class existed — which is what makes the change safe to land
 * without a migration. Creating the file is what opts in.
 *
 * <p>Precedence is per key, not per file: a properties file that sets only {@code hiscores.password}
 * still gets the built-in defaults for everything else.
 *
 * <p>The format is the usual {@code key=value}, with {@code #} or {@code !} comments and blank lines
 * ignored; keys and values are trimmed, and a {@code #} starts a trailing comment only when it is the
 * first non-blank character of the line (so a password containing {@code #} is preserved). See
 * {@code Data/server.properties.template} for the documented keys.
 */
public final class Configuration {

	/** Where the live configuration lives, resolved against the working directory like every other Data file. */
	public static final Path DEFAULT_PATH = Paths.get("./Data/server.properties");

	/** Lazily-loaded table for {@link #DEFAULT_PATH}; {@code null} until first use. */
	private static volatile Configuration defaultInstance;

	/** Lower-cased key to trimmed value. */
	private final Map<String, String> values;

	private Configuration(Map<String, String> values) {
		this.values = values;
	}

	/** A configuration with no keys — every getter returns its fallback. */
	public static Configuration empty() {
		return new Configuration(Collections.<String, String>emptyMap());
	}

	/** Builds a configuration from already-parsed entries. */
	public static Configuration of(Map<String, String> values) {
		return new Configuration(new HashMap<String, String>(values));
	}

	/**
	 * The value for {@code key}, or {@code fallback} when absent or blank.
	 *
	 * <p>A key present but empty is treated as absent. That is deliberate: a template copied without
	 * filling in a blank password would otherwise hand the driver an empty string and produce a
	 * confusing authentication failure rather than falling back to the working default.
	 */
	public String getString(String key, String fallback) {
		String value = values.get(key.toLowerCase());
		return value == null || value.isEmpty() ? fallback : value;
	}

	/**
	 * The value for {@code key} parsed as an int, or {@code fallback} when absent or unparseable.
	 * A bad number falls back rather than throwing, so a typo in the file cannot stop the boot.
	 */
	public int getInt(String key, int fallback) {
		String value = values.get(key.toLowerCase());
		if (value == null || value.isEmpty()) {
			return fallback;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	/**
	 * The value for {@code key} as a boolean, or {@code fallback} when absent.
	 *
	 * <p>Only {@code true}/{@code false} (any case) are accepted; anything else falls back, so
	 * {@code enabled=yes} is a typo that silently keeps the default rather than a surprise true.
	 */
	public boolean getBoolean(String key, boolean fallback) {
		String value = values.get(key.toLowerCase());
		if (value == null || value.isEmpty()) {
			return fallback;
		}
		if (value.equalsIgnoreCase("true")) {
			return true;
		}
		if (value.equalsIgnoreCase("false")) {
			return false;
		}
		return fallback;
	}

	/** True when the file supplied an explicit, non-blank value for {@code key}. */
	public boolean has(String key) {
		String value = values.get(key.toLowerCase());
		return value != null && !value.isEmpty();
	}

	/** How many keys the file supplied. */
	public int count() {
		return values.size();
	}

	/**
	 * Parses {@code key=value} lines.
	 *
	 * <p>Returns a comment/whitespace-tolerant map. Lines without an {@code =}, or with an empty key,
	 * are skipped — hand-editing this file must not be able to stop the server booting. Keys are
	 * lower-cased so lookups are case-insensitive; the first occurrence of a key wins, so a stray
	 * duplicate lower down cannot silently override the value at the top.
	 */
	public static Configuration parse(List<String> lines) {
		Map<String, String> parsed = new HashMap<String, String>();
		for (String raw : lines) {
			if (raw == null) {
				continue;
			}
			String line = raw.trim();
			if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '!') {
				continue;
			}
			int equals = line.indexOf('=');
			if (equals <= 0) {
				continue;
			}
			String key = line.substring(0, equals).trim().toLowerCase();
			String value = line.substring(equals + 1).trim();
			if (key.isEmpty()) {
				continue;
			}
			if (!parsed.containsKey(key)) {
				parsed.put(key, value);
			}
		}
		return new Configuration(parsed);
	}

	/**
	 * Reads the file, returning {@link #empty()} (never {@code null}) when it is absent or unreadable
	 * so every getter falls back to the built-in default.
	 */
	public static Configuration load(Path path) {
		if (path == null || !Files.isRegularFile(path)) {
			System.out.println("[Configuration] " + path + " not found — using built-in defaults");
			return empty();
		}
		try {
			Configuration loaded = parse(Files.readAllLines(path, StandardCharsets.UTF_8));
			System.out.println("[Configuration] Loaded " + loaded.count() + " setting(s) from " + path);
			return loaded;
		} catch (IOException e) {
			System.out.println("[Configuration] Failed to read " + path + " (" + e.getClass().getSimpleName()
					+ ") — using built-in defaults");
			return empty();
		}
	}

	/** The shared configuration, loaded from {@link #DEFAULT_PATH} on first use. */
	public static Configuration get() {
		Configuration instance = defaultInstance;
		if (instance == null) {
			synchronized (Configuration.class) {
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
