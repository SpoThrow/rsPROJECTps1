package server.game.bots.world;

import java.util.Random;

import server.clip.region.Region;

/**
 * A random walkable tile inside a box — {@code BOT_ROADMAP.md} §5.2, {@code BOT_LOCATIONS.md} A.7.
 *
 * <p>This is the mechanism behind "rough idea within certain tiles, not the same tile every time": a
 * bot sent to {@code draynor_willows} spreads out instead of stacking on one square.
 *
 * <p><b>Seeded, so it is reproducible.</b> The RNG is {@link Random}, whose sequence is specified and
 * stable, seeded from the per-bot seed. Two bots sharing a waypoint therefore land on different tiles,
 * and each bot resolving the same waypoint lands on the same tile every time — so a "stuck" bot can be
 * replayed from its trace rather than merely described.
 *
 * <p><b>Validated, then bounded, then deterministic.</b> A candidate is checked with
 * {@link Walkable} (the live one is {@code Region.getClipping} against the walk-block mask) before it
 * is used. After {@link #DEFAULT_RETRIES} failures it stops rolling and falls back — first to the box
 * centre, and if that is not walkable either, to the first walkable tile scanning outward from the
 * centre. Only when the whole box is unwalkable does it return the centre anyway, which is the honest
 * answer: the caller asked for a tile in a box that has none.
 */
public final class RandomTileIn implements Waypoint {

	/** Candidate rolls before falling back. Bounded so a mostly-blocked box cannot spin. */
	public static final int DEFAULT_RETRIES = 24;

	/**
	 * The bits that block walking, from {@code SmartPathFinder}'s directional masks: the union of
	 * {@code 0x1280102 | 0x1280108 | 0x1280120 | 0x1280180} plus the diagonal arms. A tile with any of
	 * these set cannot be stepped onto from some direction, which is not a tile to aim at.
	 */
	static final int WALK_BLOCK_MASK = 0x12801FF;

	/** Whether a tile can be stood on. Injected so this can be tested without a loaded world. */
	public interface Walkable {
		boolean test(int x, int y, int plane);
	}

	private final Location region;
	private final Walkable walkable;
	private final int retries;

	public RandomTileIn(Location region, Walkable walkable) {
		this(region, walkable, DEFAULT_RETRIES);
	}

	public RandomTileIn(Location region, Walkable walkable, int retries) {
		if (region == null) {
			throw new IllegalArgumentException("a waypoint needs a region");
		}
		if (walkable == null) {
			throw new IllegalArgumentException("a waypoint needs a walkability test");
		}
		this.region = region;
		this.walkable = walkable;
		this.retries = retries;
	}

	/** True when the running server would let a player stand on this tile. */
	public static Walkable liveWalkable() {
		return new Walkable() {
			@Override
			public boolean test(int x, int y, int plane) {
				return (Region.getClipping(x, y, plane) & WALK_BLOCK_MASK) == 0;
			}
		};
	}

	/** The box this waypoint resolves within. */
	public Location region() {
		return region;
	}

	@Override
	public Tile resolve(int seed) {
		Random rng = new Random(seed);
		for (int attempt = 0; attempt < retries; attempt++) {
			int x = region.x() + rng.nextInt(region.width());
			int y = region.y() + rng.nextInt(region.height());
			if (walkable.test(x, y, region.plane())) {
				return Tile.of(x, y, region.plane());
			}
		}
		return fallback();
	}

	/**
	 * The box centre when it is walkable, else the first walkable tile spiralling out from it.
	 *
	 * <p>Deterministic and seed-free on purpose: the fallback is reached when the random search failed,
	 * and a fallback that varied with the seed would make the failure look like success for some seeds.
	 */
	private Tile fallback() {
		int centreX = region.centreX();
		int centreY = region.centreY();
		if (walkable.test(centreX, centreY, region.plane())) {
			return Tile.of(centreX, centreY, region.plane());
		}
		int maxRing = Math.max(region.width(), region.height());
		for (int ring = 1; ring <= maxRing; ring++) {
			for (int dx = -ring; dx <= ring; dx++) {
				for (int dy = -ring; dy <= ring; dy++) {
					if (Math.max(Math.abs(dx), Math.abs(dy)) != ring) {
						continue;
					}
					int x = centreX + dx;
					int y = centreY + dy;
					if (x < region.x() || x >= region.x() + region.width()
							|| y < region.y() || y >= region.y() + region.height()) {
						continue;
					}
					if (walkable.test(x, y, region.plane())) {
						return Tile.of(x, y, region.plane());
					}
				}
			}
		}
		// Nothing in the box is walkable. The centre is the answer the caller can reason about.
		return Tile.of(centreX, centreY, region.plane());
	}

	@Override
	public String toString() {
		return "RandomTileIn(" + region.name() + ")";
	}
}
