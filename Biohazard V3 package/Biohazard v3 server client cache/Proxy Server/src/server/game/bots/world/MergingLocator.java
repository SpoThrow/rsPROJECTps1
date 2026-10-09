package server.game.bots.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Curated first, scan as fallback — {@code BOT_LOCATIONS.md} A.4, in the order the roadmap prescribed.
 *
 * <p>Three rules, and the third is the one that is easy to get wrong:
 *
 * <ol>
 * <li>The curated table answers if it can, because a curated answer is global and O(1) while a scan is
 *     bounded and costly.
 * <li>When the curated table cannot fill the request, the scan tops it up — so "the three nearest oaks"
 *     does not come back with one oak because only one was ever authored.
 * <li>A scanned hit that falls <em>inside</em> a curated hit of the same kind is dropped. Otherwise
 *     the Draynor bank would be reported twice: once as the authored 6x5 region and once as the six
 *     booths the scan found in it. Two answers for one place is how a bot ends up oscillating between
 *     them.
 * </ol>
 */
public final class MergingLocator implements Locator<Location> {

	private final CuratedLocator curated;
	private final ScannedLocator scanned;

	public MergingLocator(CuratedLocator curated, ScannedLocator scanned) {
		this.curated = curated == null ? CuratedLocator.of(Collections.<Location>emptyList()) : curated;
		this.scanned = scanned;
	}

	@Override
	public List<Location> nearest(int x, int y, int plane, int limit) {
		List<Location> answer = new ArrayList<Location>(curated.nearest(x, y, plane, limit));
		if (scanned != null && answer.size() < limit) {
			for (Location candidate : scanned.nearest(x, y, plane, limit)) {
				if (answer.size() >= limit) {
					break;
				}
				if (!covered(answer, candidate)) {
					answer.add(candidate);
				}
			}
			Collections.sort(answer, new java.util.Comparator<Location>() {
				@Override
				public int compare(Location a, Location b) {
					return Integer.compare(a.distanceSquaredTo(x, y), b.distanceSquaredTo(x, y));
				}
			});
			if (answer.size() > limit) {
				answer = new ArrayList<Location>(answer.subList(0, limit));
			}
		}
		return answer;
	}

	/** True when an already-accepted hit of the same kind already stands where this candidate is. */
	private static boolean covered(List<Location> accepted, Location candidate) {
		for (Location location : accepted) {
			if (location.kind() == candidate.kind() && location.contains(
					candidate.x(), candidate.y(), candidate.plane())) {
				return true;
			}
		}
		return false;
	}

	/** Named lookup is a curated question: a scan has no names to offer. */
	@Override
	public Location byName(String name) {
		return curated.byName(name);
	}

	@Override
	public List<Location> all() {
		return curated.all();
	}

	/** True when neither arm can answer anything. */
	@Override
	public boolean isEmpty() {
		return curated.isEmpty() && (scanned == null || scanned.isEmpty());
	}
}
