package botworkshop.export;

/**
 * One static object placement, as the server's {@code Region.realObjects} holds it.
 *
 * <p>Deliberately a plain carrier with no behaviour: the exporter never works out placements
 * itself, it only re-shapes what {@code Region} decoded, so the map the editor draws cannot
 * disagree with the map the server collides against.
 */
public final class Placement {

	public final int id;
	public final int x;
	public final int y;
	public final int plane;
	/** The map's object type (0-3 walls, 10/11 wall decorations, 22 floor, 12+ scenery). */
	public final int type;
	/** The map's rotation, 0-3. */
	public final int rotation;

	public Placement(int id, int x, int y, int plane, int type, int rotation) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.plane = plane;
		this.type = type;
		this.rotation = rotation;
	}

	@Override
	public String toString() {
		return id + "@" + x + "," + y + "," + plane + " type=" + type + " rot=" + rotation;
	}
}
