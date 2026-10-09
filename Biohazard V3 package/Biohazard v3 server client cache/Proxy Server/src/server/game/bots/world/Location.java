package server.game.bots.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A named place in the world: a point, or a rectangular region of tiles.
 *
 * <p>A point is a region of size 1x1, so there is one concept and not two. That matters because the
 * authoring tool lets an author drag a <em>box</em> rather than click one tile
 * ({@code BOT_ROADMAP.md} §5.2), and {@link RandomTileIn} resolves such a box to a walkable tile —
 * the box waypoint and the map region in the editor are literally the same object, so nothing has to
 * be converted between the two.
 *
 * <p>Coordinates are absolute and match the server's ({@code BOT_LOCATIONS.md} A.6). {@code plane} is
 * part of identity on purpose: Lumbridge's castle bank is at {@code 3207,3221} on plane 2, twenty-one
 * tiles from the same coordinates on plane 0, and a locator that ignored the plane would send a bot to
 * walk into a wall below it.
 */
public final class Location {

	private final String name;
	private final LocationKind kind;
	private final int x;
	private final int y;
	private final int plane;
	private final int width;
	private final int height;
	private final List<String> tags;

	public Location(String name, LocationKind kind, int x, int y, int plane, int width, int height,
			List<String> tags) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("a location needs a name");
		}
		if (kind == null) {
			throw new IllegalArgumentException("location " + name + " has no kind");
		}
		if (width < 1 || height < 1) {
			throw new IllegalArgumentException("location " + name + " has an empty box "
					+ width + "x" + height);
		}
		this.name = name;
		this.kind = kind;
		this.x = x;
		this.y = y;
		this.plane = plane;
		this.width = width;
		this.height = height;
		this.tags = Collections.unmodifiableList(new ArrayList<String>(tags == null
				? Collections.<String>emptyList() : tags));
	}

	/** A single-tile location. */
	public static Location point(String name, LocationKind kind, int x, int y, int plane) {
		return new Location(name, kind, x, y, plane, 1, 1, null);
	}

	public String name() {
		return name;
	}

	public LocationKind kind() {
		return kind;
	}

	/** Minimum X of the box. */
	public int x() {
		return x;
	}

	/** Minimum Y of the box. */
	public int y() {
		return y;
	}

	public int plane() {
		return plane;
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public List<String> tags() {
		return tags;
	}

	public boolean hasTag(String tag) {
		if (tag == null) {
			return false;
		}
		String key = tag.toLowerCase(Locale.ROOT);
		for (String candidate : tags) {
			if (candidate.toLowerCase(Locale.ROOT).equals(key)) {
				return true;
			}
		}
		return false;
	}

	/** True for a 1x1 location. */
	public boolean isPoint() {
		return width == 1 && height == 1;
	}

	public int centreX() {
		return x + width / 2;
	}

	public int centreY() {
		return y + height / 2;
	}

	/** True when the tile is inside this box on this plane. */
	public boolean contains(int tileX, int tileY, int tilePlane) {
		return tilePlane == plane
				&& tileX >= x && tileX < x + width
				&& tileY >= y && tileY < y + height;
	}

	/**
	 * Squared distance from a tile to the <em>nearest edge</em> of the box, so a bot standing inside a
	 * 6x5 bank reads as distance zero rather than being pushed to the centre.
	 *
	 * <p>Squared on purpose: the only use is ordering, and the square root would be taken and thrown
	 * away for every candidate. Callers that need a real distance take the root of this.
	 */
	public int distanceSquaredTo(int tileX, int tileY) {
		int dx = axisDistance(tileX, x, width);
		int dy = axisDistance(tileY, y, height);
		return dx * dx + dy * dy;
	}

	private static int axisDistance(int value, int start, int size) {
		if (value < start) {
			return start - value;
		}
		int end = start + size - 1;
		return value > end ? value - end : 0;
	}

	@Override
	public String toString() {
		return name + " (" + kind.id() + ") " + x + "," + y + " p" + plane
				+ (isPoint() ? "" : " " + width + "x" + height);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Location)) {
			return false;
		}
		Location that = (Location) other;
		return x == that.x && y == that.y && plane == that.plane && width == that.width
				&& height == that.height && kind == that.kind && name.equals(that.name)
				&& tags.equals(that.tags);
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + kind.hashCode();
		result = 31 * result + x;
		result = 31 * result + y;
		result = 31 * result + plane;
		result = 31 * result + width;
		result = 31 * result + height;
		result = 31 * result + tags.hashCode();
		return result;
	}
}
