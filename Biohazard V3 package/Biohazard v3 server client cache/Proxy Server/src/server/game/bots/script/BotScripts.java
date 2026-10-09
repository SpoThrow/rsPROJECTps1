package server.game.bots.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import server.game.bots.BotManager;
import server.game.bots.BotPlayer;
import server.game.bots.world.LocationKind;

/**
 * The registry of named scripts — {@code BOT_ROADMAP.md} §5.5, the handle {@code bots.cfg} (Phase E)
 * will point at.
 *
 * <p><b>Registration is a static initialiser, not a startup call.</b> {@code BotManager.start()} stays
 * a no-op, so nothing new runs at boot; the built-ins are registered the first time anything touches
 * this class, and building a script reads no world (see {@link BotScript}). That is what keeps "the
 * server boots with no new work" true while still giving Phase E a table to read.
 *
 * <p><b>Names are unique and case-insensitive.</b> A duplicate is a programming error, not a runtime
 * one — two scripts claiming {@code gather_oak} would make the config line's meaning depend on class
 * load order — so it is rejected loudly at registration rather than resolved silently at spawn.
 *
 * <p>The one built-in is {@code gather_oak}, which exists to prove the Phase D acceptance criterion: a
 * whole gathering bot, declared in one block, with no new state class. It is also the example Phase E
 * can spawn before any config file exists.
 */
public final class BotScripts {

	/** Logs, from the shipped item ids. The built-in script's yield. */
	private static final int LOGS = 1511;

	private static final Map<String, BotScript> REGISTRY = new LinkedHashMap<String, BotScript>();

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
