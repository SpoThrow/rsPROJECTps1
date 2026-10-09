package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import server.game.bots.states.WalkToNearest;
import server.game.bots.world.Location;
import server.game.bots.world.LocationKind;
import server.game.bots.world.Locations;

/**
 * {@code walkToNearest} — the step that turns roadmap C's world model into a step a script can name.
 *
 * <p>The subject is the <em>resolution</em>: which place is chosen, what tile comes out of its box, and
 * what happens when there is nothing of that kind to walk to. The walking itself is {@code WalkTo}'s,
 * tested where it lives; here a fake context answers {@code arrivedAt} so the walk finishes immediately
 * and the assertions stay about the destination.
 *
 * <p>The locator comes from {@code Locations.forKind}, which is what {@code ScriptBuilder} gives the
 * state — a bare {@code CuratedLocator} would answer with a bank when asked for a tree, which is exactly
 * the bug the kind filter exists to prevent.
 */
class WalkToNearestStateTest {

	private static final Location OAKS = new Location("oaks", LocationKind.TREE, 3100, 3200, 0, 10, 5,
			null);
	private static final Location YEWS = new Location("yews", LocationKind.TREE, 3200, 3200, 0, 4, 4,
			null);
	private static final Location BANK = Location.point("a_bank", LocationKind.BANK, 3091, 3243, 0);

	private static WalkToNearest state(LocationKind kind, int seed, Location... locations) {
		return new WalkToNearest(Locations.curated(Arrays.asList(locations)).forKind(kind), kind, 3, seed);
	}

	private static FakeBotContext ctxAt(int x, int y) {
		return new FakeBotContext().at(x, y, 0);
	}

	private static String walkOnce(WalkToNearest walk, FakeBotContext ctx) {
		walk.enter(ctx);
		assertEquals(BotStatus.SUCCESS, walk.tick(ctx), "arrivedAt answers true, so the walk completes");
		return ctx.lastWalk();
	}

	@Test
	void itWalksToTheNearestPlaceOfTheKind() {
		// 3160 is 51 tiles from the oak box's right edge and 40 from the yew box's left edge.
		WalkToNearest walk = state(LocationKind.TREE, 7, OAKS, YEWS);
		FakeBotContext ctx = ctxAt(3160, 3200);

		String destination = walkOnce(walk, ctx);

		assertEquals(YEWS, walk.place());
		assertTrue(destination.startsWith("32"), "the destination is inside the yew box: " + destination);
	}

	@Test
	void aBoxDestinationIsAWalkableTileInsideTheBoxAndTheSameOneEveryTime() {
		FakeBotContext ctx = ctxAt(3100, 3200);
		String first = walkOnce(state(LocationKind.TREE, 7, OAKS), ctx);

		// A second run with the same seed resolves the same tile: that is what makes a stuck bot
		// replayable from its trace.
		assertEquals(first, walkOnce(state(LocationKind.TREE, 7, OAKS), ctxAt(3100, 3200)));

		String[] parts = first.split(",");
		int x = Integer.parseInt(parts[0]);
		int y = Integer.parseInt(parts[1]);
		assertTrue(OAKS.contains(x, y, 0), first + " is inside " + OAKS);
	}

	@Test
	void aPointDestinationIsThePointItselfRatherThanASpread() {
		WalkToNearest walk = state(LocationKind.BANK, 7, BANK);
		FakeBotContext ctx = ctxAt(3091, 3250);

		assertEquals("3091,3243", walkOnce(walk, ctx),
				"a scanned place is one object's tile; there is no box to spread in");
	}

	@Test
	void nothingOfTheKindOnThisPlaneIsAFailureRatherThanAWait() {
		WalkToNearest walk = state(LocationKind.TREE, 7, BANK); // a table with a bank and no tree
		FakeBotContext ctx = ctxAt(3100, 3200);

		walk.enter(ctx);

		assertEquals(BotStatus.FAILURE, walk.tick(ctx));
		assertNull(walk.place());
		assertTrue(ctx.walks.isEmpty(), "it did not set off for nowhere");
	}

	@Test
	void aResolutionIsKeptForATraceRatherThanRepeated() {
		WalkToNearest walk = state(LocationKind.TREE, 7, OAKS);
		FakeBotContext ctx = ctxAt(3100, 3200);

		walkOnce(walk, ctx);

		assertEquals(1, ctx.walks.size());
		assertEquals(OAKS, walk.place());
	}

	@Test
	void anEmptyTableIsAnEmptyAnswer() {
		WalkToNearest walk = state(LocationKind.TREE, 7);
		FakeBotContext ctx = ctxAt(3100, 3200);

		walk.enter(ctx);

		assertEquals(BotStatus.FAILURE, walk.tick(ctx));
	}

	@Test
	void aDifferentPlaneIsNotAWalkToMake() {
		// The oak is two planes up; a bot on plane 0 has no destination, not a shortcut.
		Location upstairs = Location.point("upstairs_oak", LocationKind.TREE, 3100, 3200, 2);
		WalkToNearest walk = state(LocationKind.TREE, 7, upstairs);
		FakeBotContext ctx = ctxAt(3100, 3200);

		walk.enter(ctx);

		assertEquals(BotStatus.FAILURE, walk.tick(ctx));
	}

	@Test
	void itIsSelfDescribingByKind() {
		assertEquals("WalkToNearest(tree)", state(LocationKind.TREE, 7, OAKS).name());
	}

	@Test
	void theSeedDefaultsToTheBotsOwnIdentitySoTwoBotsDoNotShareATile() {
		// Names are fixed, so this is deterministic rather than merely likely: the same hash and the
		// same Random sequence every run.
		Set<String> tiles = new HashSet<String>();
		for (String name : Arrays.asList("seed_a", "seed_b", "seed_c", "seed_d")) {
			tiles.add(walkForSeedBot(name));
		}
		assertTrue(tiles.size() > 1, "four bots sent to one box stand in more than one square: " + tiles);

		// And each bot individually resolves the same way every time, which is what makes a stuck bot
		// replayable from its trace.
		assertEquals(walkForSeedBot("seed_a"), walkForSeedBot("seed_a"));
	}

	private static String walkForSeedBot(String name) {
		WalkToNearest walk = new WalkToNearest(
				Locations.curated(Arrays.asList(OAKS)).forKind(LocationKind.TREE), LocationKind.TREE, 3,
				WalkToNearest.SEED_FROM_BOT);
		FakeBotContext ctx = ctxAt(3100, 3200).named(name);

		walk.enter(ctx);
		assertEquals(BotStatus.SUCCESS, walk.tick(ctx));
		return ctx.lastWalk();
	}

	/** Guards the helper's own assumption: a curated table with nothing in it filters to nothing. */
	@Test
	void aTableWithNoRowsOfThatKindFiltersToNothing() {
		assertTrue(Locations.curated(Collections.<Location>emptyList()).forKind(LocationKind.TREE)
				.isEmpty());
	}
}
