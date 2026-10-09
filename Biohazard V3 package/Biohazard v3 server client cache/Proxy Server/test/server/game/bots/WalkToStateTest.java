package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import server.game.bots.states.WalkTo;

/**
 * Slice-1 step 5: {@link WalkTo} walks to a tile through the real path finder and fails
 * instead of running forever when it cannot get there.
 *
 * <p>Region data is not loaded under the test task, and {@code Config.REGION_FAIL_CLOSED}
 * is false, so the test world is empty but walkable — these tests pin the state machine and
 * the walking queue, not real-world geometry.
 */
class WalkToStateTest {

	private static final String NAME = "botwalk";

	@AfterEach
	void tearDown() throws IOException {
		BotTestFixture.cleanUp(NAME);
	}

	@Test
	void walksToTheDestinationAndSucceeds() {
		BotPlayer bot = BotTestFixture.possess(NAME);
		BotTestFixture.teleport(bot, 3200, 3200);
		PlayerBotContext ctx = new PlayerBotContext(bot);

		WalkTo walk = new WalkTo(3206, 3200, 0);
		walk.enter(ctx);

		BotStatus status = BotStatus.RUNNING;
		for (int i = 0; i < 200 && status == BotStatus.RUNNING; i++) {
			bot.getNextPlayerMovement();
			status = walk.tick(ctx);
		}

		assertEquals(BotStatus.SUCCESS, status, "the bot reached the destination");
		assertEquals(3206, bot.position.absX);
		assertEquals(3200, bot.position.absY);
	}

	@Test
	void stopsWithinRangeWithoutStandingOnTheTile() {
		BotPlayer bot = BotTestFixture.possess(NAME);
		BotTestFixture.teleport(bot, 3200, 3200);
		PlayerBotContext ctx = new PlayerBotContext(bot);

		WalkTo walk = new WalkTo(3210, 3200, 3);
		walk.enter(ctx);

		BotStatus status = BotStatus.RUNNING;
		for (int i = 0; i < 200 && status == BotStatus.RUNNING; i++) {
			bot.getNextPlayerMovement();
			status = walk.tick(ctx);
		}

		assertEquals(BotStatus.SUCCESS, status);
		// Arriving "within 3" must not require walking into the target tile.
		assertTrue(bot.position.absX >= 3207, "stopped at range 3, not on the tile");
	}

	@Test
	void aBotThatCannotMoveFailsInsteadOfRunningForever() {
		BotPlayer bot = BotTestFixture.possess(NAME);
		BotTestFixture.teleport(bot, 3200, 3200);
		PlayerBotContext ctx = new PlayerBotContext(bot);

		// A freeze makes getNextPlayerMovement() reset the queue and return without stepping,
		// so no progress is ever made. Any real blocker (a closed door, an unreachable tile)
		// exercises the same path.
		bot.timers.freezeTimer = 1000;

		WalkTo walk = new WalkTo(3210, 3200, 0, 5);
		walk.enter(ctx);

		BotStatus status = BotStatus.RUNNING;
		for (int i = 0; i < 50 && status == BotStatus.RUNNING; i++) {
			bot.getNextPlayerMovement();
			status = walk.tick(ctx);
		}

		assertEquals(BotStatus.FAILURE, status, "a stuck walk is a failure, not an infinite RUNNING");
	}
}
