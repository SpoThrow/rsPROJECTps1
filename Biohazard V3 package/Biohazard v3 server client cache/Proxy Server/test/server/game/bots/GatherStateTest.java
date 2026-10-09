package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import server.game.bots.states.Gather;
import server.game.bots.world.LocationKind;
import server.game.bots.world.ResourceScan;
import server.game.bots.world.ScannedLocator;
import server.game.objects.Objects;

/**
 * The generic gather leaf: resolve, approach, click, watch the bag.
 *
 * <p>The decisions worth pinning are the ones that make it generic — that it finds a real object
 * through the kinds table rather than a hardcoded id, that it clicks the tile it resolved rather than
 * where the bot happens to be, that it stops when the bag fills, and that it gives up rather than
 * clicking an empty field forever.
 */
class GatherStateTest {

	private static final int OAK = 1276;
	private static final int LOG = 1511;
	private static final int TREE_X = 3200, TREE_Y = 3200;

	/** A world whose region at 3200,3200 holds {@code objects}, and whose only tree-kind id is the oak. */
	private static final class FakeWorld
			implements ScannedLocator.RegionSource, ScannedLocator.KindSource {

		private final List<Objects> objects;

		FakeWorld(List<Objects> objects) {
			this.objects = objects;
		}

		@Override
		public List<Objects> objects(int baseX, int baseY) {
			return baseX == 3200 && baseY == 3200 ? objects : Collections.<Objects>emptyList();
		}

		@Override
		public String kindOf(int objectId) {
			return objectId == OAK ? "tree" : null;
		}
	}

	private static ResourceScan world(Objects... objects) {
		FakeWorld fake = new FakeWorld(new ArrayList<Objects>(Arrays.asList(objects)));
		return ResourceScan.with(fake, fake);
	}

	private static Gather gather(int itemId) {
		return new Gather(world(new Objects(OAK, TREE_X, TREE_Y, 0, 0, 10)), LocationKind.TREE, itemId, 3,
				8, Gather.FIRST_CLICK);
	}

	private static FakeBotContext ctxNearTree() {
		return new FakeBotContext().at(TREE_X + 1, TREE_Y, 0).freeSlots(28);
	}

	@Test
	void itFindsTheTreeWalksToItAndClicksIt() {
		Gather gather = gather(LOG);
		FakeBotContext ctx = ctxNearTree();

		gather.enter(ctx);
		assertEquals(BotStatus.RUNNING, gather.tick(ctx), "arrivedAt is true, so it clicks immediately");

		assertEquals(TREE_X, gather.target().x(), "the target is the object it found, not the bot's tile");
		assertEquals(OAK, gather.target().objectId());
		assertEquals(TREE_X + "," + TREE_Y, ctx.lastWalk(), "it walked at the object first");
		assertEquals(Arrays.asList(OAK + "@" + TREE_X + "," + TREE_Y + ":FIRST"), ctx.interactions);
	}

	@Test
	void aFullBagIsSuccessWithoutWalkingAnywhere() {
		Gather gather = gather(LOG);
		FakeBotContext ctx = ctxNearTree().freeSlots(0);

		gather.enter(ctx);

		assertEquals(BotStatus.SUCCESS, gather.tick(ctx), "there is nothing to do with no free slot");
		assertTrue(ctx.walks.isEmpty());
		assertTrue(ctx.interactions.isEmpty());
	}

	@Test
	void theBagFillingIsWhatEndsIt() {
		Gather gather = gather(LOG);
		FakeBotContext ctx = ctxNearTree();

		gather.enter(ctx);
		assertEquals(BotStatus.RUNNING, gather.tick(ctx));

		ctx.freeSlots(27); // a log arrived
		assertEquals(BotStatus.RUNNING, gather.tick(ctx));

		ctx.freeSlots(0);
		assertEquals(BotStatus.SUCCESS, gather.tick(ctx), "the last free slot was the last log");
	}

	@Test
	void somethingThatNeverYieldsIsAbandonedAndThenFailed() {
		Gather gather = gather(LOG);
		FakeBotContext ctx = ctxNearTree();

		gather.enter(ctx);

		// A tree that never falls over and never yields: the leaf must not click it forever. The budget
		// is distinct targets, so this is the whole give-up path in one loop.
		int cap = (Gather.STALL_TICKS + 1) * (Gather.MAX_TARGETS + 1);
		BotStatus status = BotStatus.RUNNING;
		for (int ticks = 0; ticks < cap && status == BotStatus.RUNNING; ticks++) {
			status = gather.tick(ctx);
		}

		assertEquals(BotStatus.FAILURE, status, "it stopped rather than clicking an empty tree forever");
		assertTrue(ctx.interactions.size() > Gather.MAX_TARGETS,
				"it did click along the way, then stopped: " + ctx.interactions.size());
	}

	@Test
	void anEmptyWorldIsAnImmediateFailure() {
		Gather gather = new Gather(world(), LocationKind.TREE, LOG, 3, 8, Gather.FIRST_CLICK);
		FakeBotContext ctx = ctxNearTree();

		gather.enter(ctx);

		assertEquals(BotStatus.FAILURE, gather.tick(ctx));
		assertTrue(ctx.walks.isEmpty(), "nothing to walk to");
	}

	@Test
	void aSlowButWorkingTreeIsNotAbandonedForBeingSlow() {
		Gather gather = gather(LOG);
		FakeBotContext ctx = ctxNearTree();

		gather.enter(ctx);
		BotStatus status = BotStatus.RUNNING;

		// A log every 20 ticks, which is long enough to trip the stall timer unless progress resets it.
		// Progress must keep it on this target rather than walking off to find another tree.
		for (int gained = 0; gained < 5 && status == BotStatus.RUNNING; gained++) {
			ctx.freeSlots(27 - gained);
			for (int i = 0; i < 20; i++) {
				status = gather.tick(ctx);
			}
		}

		assertEquals(TREE_X, gather.target().x(), "it stayed on the tree it was working");
		assertEquals(BotStatus.RUNNING, status, "and it has not failed a working resource");
	}

	@Test
	void itIsSelfDescribingByKind() {
		assertEquals("Gather(tree)", gather(LOG).name());
	}

	@Test
	void aClickIndexOutsideTheClicksAvailableIsRejected() {
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			new Gather(world(), LocationKind.TREE, LOG, 3, 8, 99);
		});

		assertTrue(thrown.getMessage().contains("99"), thrown.getMessage());
	}
}
