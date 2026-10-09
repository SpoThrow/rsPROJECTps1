package server.game.bots;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads {@code Data/cfg/bots.cfg} — roadmap Phase E, the file that makes a bot a config line.
 *
 * <pre>
 * # comment
 * account botwillow  password wq7f2k9r  script gather_willow  home draynor  enabled true
 * account botoakh01  password h3n8tz4m  script gather_oak     home draynor
 * </pre>
 *
 * <p>Same two failures get the same two treatments as {@link server.game.bots.world.LocationsConfig},
 * and for the same reasons:
 *
 * <ul>
 * <li><b>No file at all</b> — "no bots configured yet". An empty, problem-free result, so
 *     {@link BotManager#start()} spawns nothing and the server boots exactly as it did before this
 *     existed. That is the "deleting the bot package leaves the server running" criterion, at the
 *     level of the file.
 * <li><b>A bad line</b> — a real authoring mistake. The line is skipped and recorded in
 *     {@link Result#problems()}, so one typo in the eighth bot does not stop the seven good ones, nor
 *     the server.
 * </ul>
 *
 * <p><b>{@code enabled} defaults to true.</b> A row exists because someone wants a bot; having to write
 * {@code enabled true} on every line to get the obvious behaviour would be noise. The keyword is there
 * to park a bot without deleting its credentials.
 */
public final class BotsConfig {

	/** Where the file lives by default, relative to the server directory. */
	public static final String DEFAULT_PATH = "Data/cfg/bots.cfg";

	/** The loader's outcome: what it read, and what it had to skip. */
	public static final class Result {
		private final List<BotProfile> profiles;
		private final List<String> problems;

		Result(List<BotProfile> profiles, List<String> problems) {
			this.profiles = Collections.unmodifiableList(profiles);
			this.problems = Collections.unmodifiableList(problems);
		}

		public List<BotProfile> profiles() {
			return profiles;
		}

		/** One entry per line that could not be used, with its line number. */
		public List<String> problems() {
			return problems;
		}

		/** The enabled rows, in file order. What {@link BotManager#start()} spawns. */
		public List<BotProfile> enabled() {
			List<BotProfile> out = new ArrayList<BotProfile>();
			for (BotProfile profile : profiles) {
				if (profile.enabled()) {
					out.add(profile);
				}
			}
			return out;
		}

		public BotProfile byAccount(String account) {
			if (account == null) {
				return null;
			}
			for (BotProfile profile : profiles) {
				if (account.trim().equalsIgnoreCase(profile.account())) {
					return profile;
				}
			}
			return null;
		}
	}

	private BotsConfig() {
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
		List<BotProfile> profiles = new ArrayList<BotProfile>();
		List<String> problems = new ArrayList<String>();
		if (path == null || !Files.isRegularFile(path)) {
			return new Result(profiles, problems);
		}
		List<String> lines;
		try {
			lines = Files.readAllLines(path, StandardCharsets.UTF_8);
		} catch (IOException e) {
			// The file exists but cannot be read: that is worth saying, unlike "not there yet".
			problems.add("cannot read " + path + ": " + e.getMessage());
			return new Result(profiles, problems);
		}
		return parseLines(lines);
	}

	/** Parses one non-blank, comment-stripped line. */
	public static BotProfile parse(String line) {
		// Two spellings, one grammar: `<record> <name> <field> <value> ...` (what the docs show) and
		// `<record> = <name> <field> <value> ...` (what toRow writes, matching locations.cfg). The
		// record word is the first token, or whatever sits before the '='.
		int equals = line.indexOf('=');
		String record;
		String body;
		if (equals < 0) {
			body = line;
			record = null;
		} else {
			record = line.substring(0, equals).trim().toLowerCase(Locale.ROOT);
			body = line.substring(equals + 1);
		}

		List<String> tokens = tokenize(body);
		if (record == null) {
			if (tokens.isEmpty()) {
				throw new IllegalArgumentException("no record given");
			}
			record = tokens.remove(0).toLowerCase(Locale.ROOT);
		}
		if (record.isEmpty()) {
			throw new IllegalArgumentException("no record given");
		}
		if (!"account".equals(record) && !"bot".equals(record)) {
			throw new IllegalArgumentException("unknown record '" + record + "' (expected account)");
		}
		if (tokens.isEmpty()) {
			throw new IllegalArgumentException("no account name given");
		}
		String account = tokens.remove(0);

		String password = null, script = null, home = null;
		boolean enabled = true;

		int i = 0;
		while (i < tokens.size()) {
			String field = tokens.get(i).toLowerCase(Locale.ROOT);
			if (i + 1 >= tokens.size()) {
				throw new IllegalArgumentException("field '" + field + "' has no value");
			}
			String value = tokens.get(i + 1);
			switch (field) {
			case "password":
			case "pass":
				password = value;
				break;
			case "script":
				script = value;
				break;
			case "home":
				home = value;
				break;
			case "enabled":
				enabled = booleanValue(value);
				break;
			default:
				throw new IllegalArgumentException("unknown field '" + field + "'");
			}
			i += 2;
		}

		account = account.trim().toLowerCase(Locale.ROOT);
		if (!BotNames.isLoginLegal(account)) {
			// The account a human would log into: over 12 chars, or punctuation the login decoder
			// refuses, would spawn a bot nobody could ever take over (BOT_ACCOUNTS.md §1).
			throw new IllegalArgumentException("account '" + account
					+ "' is not login-legal (<= " + BotNames.MAX_NAME_LENGTH + " chars, a-z 0-9 space)");
		}
		if (password == null || password.isEmpty()) {
			throw new IllegalArgumentException("missing password for " + account);
		}
		if (script == null || script.isBlank()) {
			throw new IllegalArgumentException("missing script for " + account);
		}
		return BotProfile.of(account, password, script.trim(), home, enabled);
	}

	private static boolean booleanValue(String value) {
		String key = value.toLowerCase(Locale.ROOT);
		if ("true".equals(key) || "yes".equals(key) || "on".equals(key) || "1".equals(key)) {
			return true;
		}
		if ("false".equals(key) || "no".equals(key) || "off".equals(key) || "0".equals(key)) {
			return false;
		}
		throw new IllegalArgumentException("enabled is not a boolean: '" + value + "'");
	}

	/** Splits on whitespace. Commas are not separators here: a password may contain one. */
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
		List<BotProfile> profiles = new ArrayList<BotProfile>();
		List<String> problems = new ArrayList<String>();
		Map<String, Integer> seen = new LinkedHashMap<String, Integer>();
		for (int i = 0; i < lines.size(); i++) {
			String raw = lines.get(i);
			String line = stripComment(raw).trim();
			if (line.isEmpty()) {
				continue;
			}
			BotProfile profile;
			try {
				profile = parse(line);
			} catch (IllegalArgumentException e) {
				problems.add("line " + (i + 1) + ": " + e.getMessage() + " — '" + raw.trim() + "'");
				continue;
			}
			Integer first = seen.put(profile.account(), i + 1);
			if (first != null) {
				// Two rows for one account would make which password wins depend on file order, and
				// the second spawn would be a no-op anyway. Say so instead of silently dropping it.
				problems.add("line " + (i + 1) + ": duplicate account '" + profile.account()
						+ "' (already on line " + first + ")");
				continue;
			}
			profiles.add(profile);
		}
		return new Result(profiles, problems);
	}

	/**
	 * The canonical one-line form of a profile: the exact inverse of {@link #parse}.
	 *
	 * <p>Present for the same reason {@code LocationsConfig.toRow} is: the workshop will write rows, and
	 * the server's parser is the only thing that should decide what a row looks like.
	 */
	public static String toRow(BotProfile profile) {
		StringBuilder row = new StringBuilder("account = ").append(profile.account())
				.append(" password ").append(profile.password())
				.append(" script ").append(profile.script());
		if (profile.home() != null) {
			row.append(" home ").append(profile.home());
		}
		row.append(" enabled ").append(profile.enabled() ? "true" : "false");
		return row.toString();
	}
}
