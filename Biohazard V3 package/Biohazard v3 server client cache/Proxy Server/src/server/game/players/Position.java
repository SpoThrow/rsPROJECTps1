package server.game.players;

/**
 * Where a {@link Player} is, and where it is about to be moved to.
 *
 * <p>Nine fields that used to be declared straight on {@link Player}, reached as
 * {@code player.position}. The split is the position trio -- {@code absX}, {@code absY},
 * {@code heightLevel} -- plus the six numbers the walking and region code bookkeeps
 * around it: the walk cursor {@code currentX}/{@code currentY}, the map-region base
 * {@code mapRegionX}/{@code mapRegionY} the client is currently viewing, and the queued
 * destination {@code teleportToX}/{@code teleportToY}.
 *
 * <p><b>This cluster is why the rewriter had to become type-aware.</b> {@code absX},
 * {@code absY} and {@code heightLevel} are declared on {@code NPC} as well, and
 * {@code PlayerAssistant} has its own public {@code absX}/{@code absY}/{@code heightLevel}
 * on top of that, while {@code Region} and {@code WalkingCheck} use locals with those
 * names and {@code withinDistance(int absX, int getY, int getHeightLevel)} has parameters
 * with them too. A rename pass keyed on the name cannot tell those apart -- 325
 * {@code NPC} uses alone would have been corrupted, and the same identifier ({@code n})
 * is an {@code NPC} in one file and a {@code Player} in another. The move was therefore
 * done by resolving each identifier to its declaring element; see the plan for the
 * tooling.
 *
 * <p><b>The defaults are load-bearing and are reproduced exactly.</b> Everything defaults
 * to {@code 0} except {@code teleportToX} and {@code teleportToY}, which default to
 * {@code -1}. Callers test for {@code -1} to mean "no pending teleport", so a {@code 0}
 * default here would look like a request to teleport to the origin.
 *
 * <p><b>Five of these are persisted</b> -- {@code heightLevel} (key
 * {@code character-height}), {@code absX} and {@code absY} (save keys
 * {@code character-posx}/{@code character-posy}) and the values loaded back into
 * {@code teleportToX}/{@code teleportToY}. The save keys are string literals in
 * {@code PlayerSave}, so the path change cannot move them; the field names are kept
 * identical anyway, as in 4.4, so code and save file stay in step.
 *
 * <p>This is a data bag on purpose. Extracting the cluster and encapsulating it are
 * separate steps, so that a behaviour change cannot hide inside the mechanical move.
 * Note that {@code Player.getX()}/{@code getY()} already wrap {@code absX}/{@code absY};
 * they are the future seam.
 */
public final class Position {

	/** Base X of the 8x8 map-region grid the client is currently viewing. */
	public int mapRegionX;

	/** Base Y of the 8x8 map-region grid the client is currently viewing. */
	public int mapRegionY;

	/** Absolute world X. Persisted as {@code character-posx}. */
	public int absX;

	/** Absolute world Y. Persisted as {@code character-posy}. */
	public int absY;

	/** Walk cursor X within the current map region, sent to the client each update. */
	public int currentX;

	/** Walk cursor Y within the current map region, sent to the client each update. */
	public int currentY;

	/** Height plane. Persisted as {@code character-height}. */
	public int heightLevel;

	/** Queued teleport destination X, or {@code -1} for none. The client walk applies it. */
	public int teleportToX = -1;

	/** Queued teleport destination Y, or {@code -1} for none. */
	public int teleportToY = -1;
}
