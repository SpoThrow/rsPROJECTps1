package server.game.bots.script;

import server.game.bots.BotState;
import server.game.bots.world.Locations;
import server.game.bots.world.ResourceScan;

/**
 * A named behaviour a bot can run — {@code BOT_ROADMAP.md} §5.5, and the thing the Bot Workshop's
 * timeline editor will serialise to.
 *
 * <p><b>Why a script is a named tree and not just a tree.</b> A tree is anonymous: once built there is
 * nothing to call it, so {@code bots.cfg} (roadmap Phase E) would have to hold a tree, and a trace or a
 * {@code ::botinfo} dump (Phase F) could only print a structure. A name is the handle a config line
 * refers to, the label a log line prints, and the thing a test asserts against. It is the smallest
 * piece of identity the rest of the roadmap needs.
 *
 * <p><b>{@link #root()} takes no context, on purpose.</b> The roadmap sketched {@code root(BotContext)}
 * because it assumed the nearest tree was resolved when the tree was built. Resolution is instead done
 * when a state is <em>entered</em> ({@code WalkToNearest}, {@code Gather}), which is better in two ways:
 * a bot that banked and returned re-resolves "nearest" against where it actually is now rather than
 * where it was when the script was assembled, and building a script touches no world — so registering
 * one reads no files and {@code BotController} needs no change to hand a context in.
 *
 * <p>When Phase G needs a context at construction time (to pick a locator for an NPC agent), adding an
 * overload here is additive; nothing depends on the current signature beyond the builder.
 */
public interface BotScript {

	/** The handle a config line, a trace and a test use. */
	String name();

	/** A freshly built root. Called once per possession, so two bots never share mutable nodes. */
	BotState root();

	/**
	 * Start building a script against the live world — the ordinary entry point
	 * ({@code BotScript.named("gather_oak")}).
	 *
	 * <p>Cheap and side-effect free: no world data is read until a state resolves a destination, so this
	 * is safe to call from a static initialiser.
	 */
	static ScriptBuilder named(String name) {
		return new ScriptBuilder(name, null, null);
	}

	/**
	 * Start building a script against a given world. The test form: a curated {@code Locations} and an
	 * injected {@code ResourceScan} make a script deterministic without loading {@code Data/cfg}.
	 */
	static ScriptBuilder named(String name, Locations locations, ResourceScan scan) {
		return new ScriptBuilder(name, locations, scan);
	}
}
