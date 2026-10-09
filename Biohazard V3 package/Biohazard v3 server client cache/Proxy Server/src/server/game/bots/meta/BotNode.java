package server.game.bots.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link server.game.bots.BotState} implementation as a node the Bot Workshop can
 * author ({@code BOT_TOOLING.md} stage T4).
 *
 * <p><b>Why an annotation and not a hand-written list.</b> The editor needs a schema of every
 * node it can drop into a tree: its id, what it does, and what it takes. Writing that schema out
 * by hand in the tool guarantees it drifts from the states the server actually runs. Declaring it
 * on the class instead means the server's own {@link BotNodeRegistry} can re-derive the schema,
 * and a test can compare the two directions for equality.
 *
 * <p>The id is explicit rather than derived from the class name because it is a format: it is
 * written into authored trees and read back, so renaming a Java class must not silently invalidate
 * saved trees. Nothing else here is redundant — the parameter list is reflected from the
 * constructor, not repeated.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BotNode {

	/** Stable identifier used in {@code bot-nodes.json} and in authored trees, e.g. {@code walk_to}. */
	String id();

	/** One line the editor shows in its palette. */
	String summary();

	/** Palette grouping, e.g. {@code state} (a leaf that does work) or {@code composite}. */
	String category();
}
