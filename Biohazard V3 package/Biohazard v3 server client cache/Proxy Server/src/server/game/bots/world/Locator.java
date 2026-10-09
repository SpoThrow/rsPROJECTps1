package server.game.bots.world;

import java.util.List;

/**
 * Answers "where is X" from a bot's point of view — {@code BOT_ROADMAP.md} §5.2.
 *
 * <p>The reason slice 1 hardcoded tiles: a behaviour should name a <em>resource</em>, not a
 * coordinate. Two implementations satisfy this, and they must pass the same tests
 * ({@code BOT_ROADMAP.md} §11, phase C):
 *
 * <ul>
 * <li>{@link CuratedLocator} — reads {@code Data/cfg}. Correct and O(1); the default.
 * <li>{@link ScannedLocator} — scans the loaded world. Flexible but costs per-tick work, so it is a
 *     <em>fallback</em> for what the curated table does not cover.
 * </ul>
 *
 * <p><b>{@link #nearest} is plane-strict, on purpose.</b> {@code BOT_LOCATIONS.md} A.6: dungeons are
 * not contiguous with the surface (hill giants at {@code 3117,9846}, the Fremennik dungeon at
 * {@code y≈10000}) and a bank one plane up is a different journey, not a shorter one. A locator that
 * quietly returned a cross-plane answer would hand a bot a destination it cannot walk to. Travel
 * between planes is a {@code Sequence} of legs, and that is the caller's job.
 */
public interface Locator<T extends Location> {

	/**
	 * The nearest entries on {@code plane}, closest first, at most {@code limit}.
	 *
	 * <p>Returns an empty list when nothing on that plane matches — never a cross-plane answer.
	 */
	List<T> nearest(int x, int y, int plane, int limit);

	/** Named lookup, or {@code null} when this locator has no such name. */
	T byName(String name);

	/** Everything this locator knows, in no particular order. A snapshot: safe to keep. */
	List<T> all();

	/** True when this locator has no entries at all. */
	default boolean isEmpty() {
		return all().isEmpty();
	}
}
