package botworkshop.data;

import java.util.Collections;
import java.util.List;

/** One decoded {@code loc.dat} entry: what an object is called and what you can do to it. */
public final class LocDefinition {

	private final int id;
	private final String name;
	private final List<String> actions;
	private final int sizeX;
	private final int sizeY;
	private final boolean blocksWalk;
	private final boolean parsed;

	LocDefinition(int id, String name, List<String> actions, int sizeX, int sizeY,
			boolean blocksWalk, boolean parsed) {
		this.id = id;
		this.name = name;
		this.actions = Collections.unmodifiableList(actions);
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.blocksWalk = blocksWalk;
		this.parsed = parsed;
	}

	public int id() {
		return id;
	}

	/** The object's name, or {@code null} when the definition has none. */
	public String name() {
		return name;
	}

	/**
	 * The object's actions in slot order, with {@code "hidden"} slots dropped and gaps removed.
	 * Empty (never {@code null}) when the object has none.
	 */
	public List<String> actions() {
		return actions;
	}

	/** Cache footprint width, {@code 1} when the cache omits it. Prefer {@code ObjectSizes}. */
	public int sizeX() {
		return sizeX;
	}

	/** Cache footprint height, {@code 1} when the cache omits it. Prefer {@code ObjectSizes}. */
	public int sizeY() {
		return sizeY;
	}

	/** Whether this object's definition still says it blocks movement. */
	public boolean blocksWalk() {
		return blocksWalk;
	}

	/**
	 * False when the opcode walk met a byte it did not understand and stopped early. The name and
	 * actions read so far are still returned, but they are truncated, so callers that must not be
	 * wrong (the validator) treat these as unusable rather than as "no name".
	 */
	public boolean parsed() {
		return parsed;
	}

	@Override
	public String toString() {
		return id + " " + name + " " + actions;
	}
}
