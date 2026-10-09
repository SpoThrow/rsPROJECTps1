package server.game.bots.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Finds the actual world objects of a kind near a tile — {@code BOT_ROADMAP.md} §5.3's {@code Interact}
 * leaf needs something to click, and this is what supplies it.
 *
 * <p><b>Why {@link ScannedLocator} was not enough.</b> That class answers "nearest place", and a place
 * is a box. An interaction needs an object id and a tile ({@link ObjectTarget}), and {@code
 * ScannedLocator} deliberately drops both — it names a place by coordinate and never by object. Rather
 * than widen that class' answer, this one asks the same two questions it does ({@link
 * ScannedLocator.RegionSource} and {@link ScannedLocator.KindSource}, both reused rather than
 * re-invented) and keeps the object instead of collapsing it to a place.
 *
 * <p><b>Classification still comes from one table.</b> A scan here classifies through the same {@link
 * ResourceKinds} rule set the map draws and the world layer scans with, so "is this a tree" cannot have
 * a second answer. That is the drift {@code BOT_TOOLING.md} §11 names, and reusing the seam interfaces
 * is what avoids it.
 *
 * <p><b>Bounded, and only on retarget.</b> A search covers the regions overlapping a {@code radius}
 * box around the query, which for the default 8 tiles is at most 2x2 regions — not a world sweep. It is
 * called when a gather leaf needs a target, never per tick ({@code BOT_ROADMAP.md} §7), and unlike
 * {@code ScannedLocator} it does not cache: the caller decides when a fresh look is worth the cost, and
 * a gather leaf re-looks precisely when the object it had has gone.
 */
public final class ResourceScan {

	/** The default search radius, in tiles. Two regions at most, and wider than any interaction range. */
	public static final int DEFAULT_RADIUS = 8;

	private final ScannedLocator.RegionSource source;
	private final ScannedLocator.KindSource kinds;

	private ResourceScan(ScannedLocator.RegionSource source, ScannedLocator.KindSource kinds) {
		this.source = source;
		this.kinds = kinds;
	}

	/** The live wiring: regions and classifications from the running server. */
	public static ResourceScan live() {
		return new ResourceScan(ScannedLocator.regionSource(), ScannedLocator.kindSource());
	}

	/** An injected world, so the search is testable without loading 1226 regions. */
	public static ResourceScan with(ScannedLocator.RegionSource source, ScannedLocator.KindSource kinds) {
		return new ResourceScan(source, kinds);
	}

	/**
	 * The objects of {@code kind} within {@code radius} tiles of {@code x,y} on {@code plane}, closest
	 * first, at most {@code limit}.
	 *
	 * <p>Empty when nothing matches — never a cross-plane answer, the same rule the locators follow.
	 */
	public List<ObjectTarget> nearest(int x, int y, int plane, LocationKind kind, int radius, int limit) {
		if (limit <= 0 || kind == null || radius < 0) {
			return Collections.emptyList();
		}
		final int qx = x;
		final int qy = y;
		final int radiusSquared = radius * radius;
		List<ObjectTarget> found = new ArrayList<ObjectTarget>();
		int fromX = baseOf(x - radius);
		int toX = baseOf(x + radius);
		int fromY = baseOf(y - radius);
		int toY = baseOf(y + radius);
		for (int baseX = fromX; baseX <= toX; baseX += ScannedLocator.SIZE) {
			for (int baseY = fromY; baseY <= toY; baseY += ScannedLocator.SIZE) {
				collect(baseX, baseY, plane, kind, x, y, radiusSquared, found);
			}
		}
		if (found.isEmpty()) {
			return Collections.emptyList();
		}
		Collections.sort(found, new Comparator<ObjectTarget>() {
			@Override
			public int compare(ObjectTarget a, ObjectTarget b) {
				return Integer.compare(a.distanceSquaredTo(qx, qy), b.distanceSquaredTo(qx, qy));
			}
		});
		return found.size() <= limit ? found : new ArrayList<ObjectTarget>(found.subList(0, limit));
	}

	/** The nearest object of {@code kind}, or null. The one call the gather leaf actually makes. */
	public ObjectTarget nearest(int x, int y, int plane, LocationKind kind, int radius) {
		List<ObjectTarget> found = nearest(x, y, plane, kind, radius, 1);
		return found.isEmpty() ? null : found.get(0);
	}

	private void collect(int baseX, int baseY, int plane, LocationKind kind, int fromX, int fromY,
			int radiusSquared, List<ObjectTarget> into) {
		List<server.game.objects.Objects> objects = source.objects(baseX, baseY);
		if (objects == null || objects.isEmpty()) {
			return;
		}
		for (server.game.objects.Objects object : objects) {
			if (object == null || object.objectId < 0) {
				// A negative id marks a removed object; Region.addObject leaves the tombstone behind.
				continue;
			}
			if (object.objectHeight != plane) {
				continue;
			}
			int dx = object.objectX - fromX;
			int dy = object.objectY - fromY;
			if (dx * dx + dy * dy > radiusSquared) {
				continue;
			}
			// Classify and drop the String immediately: KindSource reads ObjectDef, whose 20-entry
			// cache means a retained ObjectDef would be invalidated by the next classification.
			if (!kind.id().equals(kinds.kindOf(object.objectId))) {
				continue;
			}
			into.add(ObjectTarget.of(object.objectId, object.objectX, object.objectY,
					object.objectHeight));
		}
	}

	private static int baseOf(int coordinate) {
		return (coordinate >> 6) << 6;
	}
}
