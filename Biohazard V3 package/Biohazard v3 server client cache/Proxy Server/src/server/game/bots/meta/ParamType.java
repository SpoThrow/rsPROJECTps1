package server.game.bots.meta;

/**
 * What a {@link Param} holds, decided by the Java type the constructor declares.
 *
 * <p>The mapping lives here, in one place, rather than in the annotation: a parameter annotated
 * {@code int} is an {@link #INT} and a parameter annotated {@code BotState[]} is a
 * {@link #NODE_LIST}, and no author has to keep the two in agreement. {@code BotNodeRegistry}
 * throws on a type it cannot map rather than guessing, so a new node taking an unmapped type fails
 * the export instead of shipping a wrong field.
 */
public enum ParamType {

	/** A whole number. */
	INT,

	/** A true/false flag. */
	BOOLEAN,

	/** Free text, e.g. a resource name. */
	STRING,

	/** A resolved world tile ({@link server.game.bots.world.Tile}). */
	TILE,

	/** A named place ({@link server.game.bots.world.Location}). */
	LOCATION,

	/** A single child node ({@link server.game.bots.BotState}). */
	NODE,

	/** An ordered list of child nodes ({@code BotState[]}). */
	NODE_LIST
}
