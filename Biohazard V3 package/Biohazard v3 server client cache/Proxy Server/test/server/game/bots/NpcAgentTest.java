package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import server.game.npcs.NPC;
import server.game.npcs.NPCHandler;
import server.game.players.actions.objects.ObjectClick;

/**
 * {@link NpcAgent} — roadmap Phase G's second {@link Agent} implementation.
 *
 * <p>This is what makes the seam more than a rename of {@code Client}: the same interface, honoured by
 * something that is not a player. These tests pin the parts that genuinely differ (an NPC steps a tile
 * rather than queueing a route) and the one part that genuinely cannot work (object dispatch is
 * {@code Client}-typed), so the limit is recorded in a test rather than only in a comment.
 */
class NpcAgentTest {

	/**
	 * A slot far from anything else: NPCs are a flat static array and {@code npcId} doubles as the index
	 * in this codebase, which is how {@code WorldAdventurer} uses it too. Cleared after each test so no
	 * other test can see this NPC.
	 */
	private static final int SLOT = NPCHandler.maxNPCs - 1;

	@AfterEach
	void clearSlot() {
		NPCHandler.npcs[SLOT] = null;
	}

	private static NPC spawn(int npcType, int x, int y, int plane) {
		// Touch Server first: its static initialiser builds an NPCHandler that clears the npcs array, so a
		// slot filled before that point is wiped the first time anything refers to Server — which is what
		// NpcAgent.walkTo does. Forcing the init here is what makes the slot survive.
		NPCHandler handler = server.Server.npcHandler;
		NPC npc = new NPC(SLOT, npcType);
		npc.absX = x;
		npc.absY = y;
		npc.heightLevel = plane;
		handler.npcs[SLOT] = npc;
		return npc;
	}

	@Test
	void positionComesFromTheNpc() {
		NPC npc = spawn(1, 3200, 3223, 1);
		NpcAgent agent = new NpcAgent(npc);

		assertEquals(3200, agent.x());
		assertEquals(3223, agent.y());
		assertEquals(1, agent.height());

		npc.absX = 3100;
		assertEquals(3100, agent.x(), "the agent reads through, it does not snapshot");
	}

	@Test
	void walkingStepsOneTileTowardTheDestinationAndAsksForAnUpdate() {
		NPC npc = spawn(1, 3200, 3200, 0);
		NpcAgent agent = new NpcAgent(npc);

		agent.walkTo(3205, 3200);

		// The NPC idiom, not the player one: a step is taken inside the call, not queued for later.
		assertEquals(3201, agent.x(), "one tile east");
		assertEquals(3200, agent.y());
		assertTrue(npc.updateRequired, "the client has to be told");
	}

	@Test
	void walkingDiagonallyStepsBothAxes() {
		NPC npc = spawn(1, 3200, 3200, 0);
		NpcAgent agent = new NpcAgent(npc);

		agent.walkTo(3205, 3205);

		assertEquals(3201, agent.x());
		assertEquals(3201, agent.y());
	}

	@Test
	void aDestinationAlreadyReachedDoesNotMoveTheNpc() {
		NPC npc = spawn(1, 3200, 3200, 0);
		NpcAgent agent = new NpcAgent(npc);

		agent.walkTo(3200, 3200);

		assertEquals(3200, agent.x(), "already there");
		assertEquals(3200, agent.y());
	}

	@Test
	void arrivedAtMeasuresTilesNotExactTiles() {
		NPC npc = spawn(1, 3200, 3200, 0);
		NpcAgent agent = new NpcAgent(npc);

		assertTrue(agent.arrivedAt(3202, 3202, 2), "two tiles away, within range 2");
		assertFalse(agent.arrivedAt(3203, 3200, 2), "three tiles away");
		assertTrue(agent.arrivedAt(3203, 3200, 3));
	}

	@Test
	void isIdleFollowsWhetherAStepIsPending() {
		NPC npc = spawn(1, 3200, 3200, 0);
		NpcAgent agent = new NpcAgent(npc);

		assertTrue(agent.isIdle(), "nothing pending");
		agent.walkTo(3205, 3200);
		assertFalse(agent.isIdle(), "a step was just taken");

		// What NPCHandler does at the end of the tick, once the movement has been sent.
		npc.clearUpdateFlags();
		assertTrue(agent.isIdle());
	}

	@Test
	void objectInteractionIsRefusedAndSaysWhy() {
		NpcAgent agent = new NpcAgent(spawn(1, 3200, 3200, 0));

		UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
				() -> agent.interactObject(1276, 3200, 3200, ObjectClick.FIRST, 3));

		// Named, not silent: a false return would be indistinguishable from "out of range" and would
		// make a Gather loop retry forever instead of reporting the real problem.
		assertTrue(thrown.getMessage().contains("Client-typed"), thrown.getMessage());
	}

	@Test
	void theNameSurvivesAnUnloadedNpcList() {
		// A test JVM has no Data/npcs loaded, and getNpcListName answers "nothing" rather than throwing.
		// What matters is that name() never blows up, because a trace or a log line calls it.
		assertNotNull(new NpcAgent(spawn(1, 3200, 3200, 0)).name());
	}

	@Test
	void anAgentNeedsAnNpc() {
		assertThrows(IllegalArgumentException.class, () -> new NpcAgent(null));
	}
}
