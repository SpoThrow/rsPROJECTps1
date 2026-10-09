package server.game.bots.script;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import server.game.bots.BotManager;
import server.game.bots.BotPlayer;
import server.game.bots.world.LocationKind;

/**
 * The registry of named scripts — {@code BOT_ROADMAP.md} §5.5, the handle {@code bots.cfg} (Phase E)
 * points at.
 *
 * <p><b>Two ways in, one namespace.</b> The built-ins register from a static initialiser; authored
 * scripts load from {@code Data/cfg/bots/*.json} ({@link #loadFiles}) at boot and on {@code ::bot reload}.
 * Both end up here, so a {@code bots.cfg} row naming a script cannot tell — and should not — which way its
 * script arrived.
 *
 * <p><b>Names are unique and case-insensitive.</b> Two scripts claiming {@code gather_oak} would make the
 * config line's meaning depend on load order, so a duplicate is refused. The rule differs by source
 * because the intent does: two <em>built-ins</em> colliding is a programming error and throws
 * ({@link #register}), while a <em>file</em> colliding with a built-in is an authoring mistake and is
 * reported and skipped ({@link #loadFiles}) — a bad file must never take down a server whose built-in
 * works.
 *
 * <p>The one built-in is {@code gather_oak}, which exists to prove the Phase D acceptance criterion: a
 * whole gathering bot, declared in one block, with no new state class. It is also the fallback Phase E
 * can spawn before any config file exists, and the name a file may not shadow.
 */
public final class BotScripts {

	/** Logs, from the shipped item ids. The built-in script's yield. */
	private static final int LOGS = 1511;

	private static final Map<String, BotScript> REGISTRY = new LinkedHashMap<String, BotScript>();

	/**
	 * The names that came from files rather than code, so {@link #loadFiles} can replace exactly those on a
	 * reload. A reload must reflect the directory, which means dropping a script whose file was deleted —
	 * and must not drop a built-in, which has no file to be deleted.
	 */
	private static final Set<String> FROM_FILES = new HashSet<String>();

	private BotScripts() {
	}

	/** Adds a script. Throws when the name is missing or already taken. */
	public static void register(BotScript script) {
		if (script == null || script.name() == null || script.name().isBlank()) {
			throw new IllegalArgumentException("a script needs a name");
		}
		String key = key(script.name());
		if (REGISTRY.containsKey(key)) {
			throw new IllegalArgumentException("two scripts share the name \"" + script.name() + "\"");
		}
		REGISTRY.put(key, script);
	}

	/** The outcome of reading a scripts directory: how many loaded, and what had to be skipped. */
	public static final class LoadResult {
		private final int loaded;
		private final List<String> problems;

		LoadResult(int loaded, List<String> problems) {
			this.loaded = loaded;
			this.problems = Collections.unmodifiableList(new ArrayList<String>(problems));
		}

		public int loaded() {
			return loaded;
		}

		/** One entry per file that could not be used, naming the file. */
		public List<String> problems() {
			return problems;
		}
	}

	/**
	 * Reads every {@code *.json} in {@code dir} and registers it — {@code BOT_TOOLING.md} §6's "the server
	 * loads at startup or on a {@code ::bot reload}".
	 *
	 * <p><b>A reload reflects the directory.</b> Everything previously loaded from a file is dropped first,
	 * so a script whose file was edited is replaced and one whose file was deleted disappears. Built-ins are
	 * never dropped — they have no file to lose — so this cannot empty the registry of the scripts that
	 * exist in code.
	 *
	 * <p><b>One bad file does not stop the others, and none of it is fatal.</b> Same treatment
	 * {@code BotsConfig} gives a bad line: the file is skipped and recorded, so a typo in the second script
	 * costs the operator that script and nothing else. A missing directory is not even a problem — a server
	 * with no authored scripts is the ordinary state, exactly as with a missing {@code bots.cfg}.
	 *
	 * @param dir the directory to read, normally {@link ScriptDocument#DIR}
	 */
	public static LoadResult loadFiles(Path dir) {
		List<String> problems = new ArrayList<String>();

		// Drop the previous file-sourced scripts before re-reading, so a deleted or renamed file is
		// reflected rather than lingering until the next restart.
		for (String key : new ArrayList<String>(FROM_FILES)) {
			REGISTRY.remove(key);
		}
		FROM_FILES.clear();

		if (dir == null || !Files.isDirectory(dir)) {
			return new LoadResult(0, problems);
		}
		List<Path> files;
		try (Stream<Path> stream = Files.list(dir)) {
			files = stream.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)
							.endsWith(ScriptDocument.EXTENSION))
					.sorted()
					.collect(Collectors.toList());
		} catch (IOException e) {
			problems.add("cannot read " + dir + ": " + e.getMessage());
			return new LoadResult(0, problems);
		}

		int loaded = 0;
		for (Path file : files) {
			String fileName = file.getFileName().toString();
			String name = ScriptDocument.nameOfFile(fileName);
			// Sorted, so which script wins a name clash is a fact about the names rather than about how the
			// filesystem happened to enumerate the directory.
			if (REGISTRY.containsKey(key(name))) {
				problems.add(fileName + ": a built-in script is already named \"" + name
						+ "\"; keeping the built-in");
				continue;
			}
			try {
				String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
				BotScript script = ScriptDocument.fromJson(name, text);
				REGISTRY.put(key(script.name()), script);
				FROM_FILES.add(key(script.name()));
				loaded++;
			} catch (ScriptDocument.ScriptException | Json.JsonException e) {
				problems.add(fileName + ": " + e.getMessage());
			} catch (IOException e) {
				problems.add(fileName + ": cannot read: " + e.getMessage());
			}
		}
		return new LoadResult(loaded, problems);
	}

	/** The script with this name, or null. */
	public static BotScript byName(String name) {
		return name == null ? null : REGISTRY.get(key(name));
	}

	/** Every registered name, sorted, so a listing is a stable diff. */
	public static List<String> names() {
		List<String> out = new ArrayList<String>(REGISTRY.keySet());
		Collections.sort(out);
		return out;
	}

	/**
	 * Possesses an account and attaches the named script — the shape Phase E's {@code bots.cfg} will
	 * call once per enabled row.
	 *
	 * @return the live bot, or null when the script is unknown or the account cannot be possessed
	 */
	public static BotPlayer possess(String account, String password, String scriptName) {
		BotScript script = byName(scriptName);
		if (script == null) {
			return null;
		}
		return BotManager.possess(account, password, script.root());
	}

	private static String key(String name) {
		return name.trim().toLowerCase(Locale.ROOT);
	}

	static {
		// The Phase D acceptance criterion, as a script: travel to the trees, chop until full, travel to
		// the bank, deposit. No state class is written for it — the leaves it names already exist.
		register(BotScript.named("gather_oak")
				.gatherLoop(LocationKind.TREE, LOGS, LocationKind.BANK)
				.forever());
	}
}
