package server.game.bots.meta;

import java.util.List;

/**
 * The reflected shape of one authored node: its identity plus the constructor the editor fills in.
 *
 * <p>Immutable, and equality is by value (a record), so the two sides of the parity check — the
 * schema derived from the running classes and the rows read back from {@code bot-nodes.json} — can
 * be compared directly instead of field by field.
 */
public record NodeSchema(String id, String className, String category, String summary,
		List<NodeParam> params) {

	public NodeSchema {
		params = List.copyOf(params);
	}

	/** The node's simple class name, for display. */
	public String simpleName() {
		int dot = className.lastIndexOf('.');
		return dot < 0 ? className : className.substring(dot + 1);
	}
}
