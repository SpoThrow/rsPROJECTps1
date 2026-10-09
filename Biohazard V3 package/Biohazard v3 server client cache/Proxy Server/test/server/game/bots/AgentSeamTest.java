package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import server.game.npcs.NPC;
import server.game.npcs.NPCHandler;
import server.game.players.actions.objects.ObjectClick;

/**
 * Roadmap Phase G's deliverable, stated as a test: <b>one behaviour library, two kinds of actor.</b>
 *
 * <p><b>What is being proved.</b> Before the seam, a state reached its actor as a {@code BotPlayer}, so
 * every state was silently written against a player. These tests drive the same code — a generic walker
 * that only ever sees {@link Agent}, and the unmodified {@link server.game.bots.states.WalkTo} state —
 * through a player-side actor and through a real {@code NPC}, and show both arrive.
 *
 * <p><b>Why the NPC context lives here and not in the main tree.</b> There is no production NPC bot yet,
 * and a production {@code NpcBotContext} would have to invent an inventory for something that has none.
 * A test-only context that <em>throws</em> for the player-only facts is the honest version: it shows that
 * the interface is sufficient for travel while refusing to pretend an NPC can bank.
 */
class AgentSeamTest {

	private static final int SLOT = NPCHandler.maxNPCs - 2;

	@AfterEach
	void clearSlot() {
		NPCHandler.npcs[SLOT] = null;
	}

	private static NPC spawnNpc(int x, int y, int plane) {
		// Touch Server first: its static initialiser builds an NPCHandler whose constructor clears the
		// npcs array, so a slot filled before that point is silently wiped the first time anything refers
		// to Server — which is exactly what NpcAgent.walkTo does. Forcing the init here is what makes the
		// slot survive.
		NPCHandler handler = server.Server.npcHandler;
		NPC npc = new NPC(SLOT, 1);
		npc.absX = x;
		npc.absY = y;
		npc.heightLevel = plane;
		handler.npcs[SLOT] = npc;
		return npc;
	}

	// ---- the phase's claim -----------------------------------------------------------------------

	/**
	 * Walks any actor to a tile, touching <b>only</b> {@link Agent}. Not one method here knows or cares
	 * what is behind the interface — which is the whole point, and why the same call works for an NPC.
	 *
	 * @return the number of walk requests it took
	 */
	private static int walkToArrival(Agent agent, int destX, int destY, int budget) {
		int steps = 0;
		while (!agent.arrivedAt(destX, destY, 0)) {
			int before = chebyshev(agent, destX, destY);
			if (steps++ >= budget) {
				throw new AssertionError("agent never arrived; stuck " + before + " tiles away");
			}
			agent.walkTo(destX, destY);
			int after = chebyshev(agent, destX, destY);
			assertTrue(after <= before, "the walker moved away from its target: " + before + " -> " + after);
		}
		return steps;
	}

	private static int chebyshev(Agent agent, int x, int y) {
		return Math.max(Math.abs(agent.x() - x), Math.abs(agent.y() - y));
	}

	@Test
	void theSameWalkerReachesTheDestinationThroughAPlayerSideActorAndThroughAnNpc() {
		int destX = 3204;
		int destY = 3203;

		Agent inMemory = new InMemoryAgent("player-side", 3200, 3200);
		Agent npc = new NpcAgent(spawnNpc(3200, 3200, 0));

		int inMemorySteps = walkToArrival(inMemory, destX, destY, 20);
		int npcSteps = walkToArrival(npc, destX, destY, 20);

		// The same walker arrived both times. The step counts are close rather than identical because an
		// NPC steps one tile on each axis independently and so can briefly overshoot on one — a real
		// difference between the actors, absorbed by the interface instead of leaking into the walker.
		assertTrue(inMemorySteps >= 4, "at most one step per tile of distance: " + inMemorySteps);
		assertTrue(npcSteps >= 4 && npcSteps <= inMemorySteps + 2,
				"npc steps " + npcSteps + " vs in-memory " + inMemorySteps);
		assertEquals(destX, inMemory.x());
		assertEquals(destY, inMemory.y());
	}

	@Test
	void aStateWrittenBeforeTheSeamDrivesARealNpc() {
		NPC npc = spawnNpc(3200, 3200, 0);
		NpcBotContext ctx = new NpcBotContext(npc);

		// WalkTo was written in slice 1, when the only actor was a possessed player, and it was not
		// touched by Phase G. That it runs here, unmodified, is the deliverable.
		server.game.bots.states.WalkTo walk = new server.game.bots.states.WalkTo(3204, 3200, 0);

		ctx.onTick();
		walk.enter(ctx);
		BotStatus status = null;
		for (int tick = 0; tick < 40; tick++) {
			status = walk.tick(ctx);
			if (status != BotStatus.RUNNING) {
				break;
			}
			// What NPCHandler does at the end of a tick: flush the movement so the NPC reads as idle
			// again, which is what lets WalkTo ask for the next step.
			npc.clearUpdateFlags();
			ctx.onTick();
		}

		assertEquals(BotStatus.SUCCESS, status, "the state walked the NPC to its destination");
		assertEquals(3204, npc.absX);
		assertEquals(3200, npc.absY);
	}

	@Test
	void thePlayerFacingContextIsUnchangedByTheSeam() {
		// The regression guard: the same state over the fake player actor behaves as it always did.
		FakeBotContext ctx = new FakeBotContext().arrived(false).idle(true);
		server.game.bots.states.WalkTo walk = new server.game.bots.states.WalkTo(3210, 3200, 1);

		ctx.onTick();
		walk.enter(ctx);
		assertEquals("3210,3200", ctx.lastWalk(), "the enter still issues the route");

		assertEquals(BotStatus.RUNNING, walk.tick(ctx));
		assertEquals(2, ctx.walks.size(), "still idle and not arrived, so it re-issues");
	}

	@Test
	void theNpcContextRefusesPlayerOnlyFactsRatherThanFakingThem() {
		NpcBotContext ctx = new NpcBotContext(spawnNpc(3200, 3200, 0));

		// An NPC has no inventory, no bank and no skills, so the context says so instead of inventing
		// answers. That is what keeps "an NPC could run this state" an honest claim.
		assertThrows(UnsupportedOperationException.class, ctx::freeSlots);
		assertThrows(UnsupportedOperationException.class, ctx::openBank);
		assertThrows(UnsupportedOperationException.class, () -> ctx.skillLevel(8));
		assertThrows(UnsupportedOperationException.class, () -> ctx.hasItem(1511));
	}

	@Test
	void anNpcRunsNoSkillSessionWhichIsWhyItCanStillReportIdleness() {
		// isIdle asks two questions, and only one of them is a player fact. An NPC can always answer the
		// second, so travel states work even though the player-only half throws.
		NpcBotContext ctx = new NpcBotContext(spawnNpc(3200, 3200, 0));

		assertTrue(ctx.isIdle());
		assertFalse(ctx.isSkilling());
	}

	// ---- the two actors --------------------------------------------------------------------------

	/** A player-side actor that needs no client: one tile per request, measured like the real one. */
	private static final class InMemoryAgent implements Agent {
		private final String name;
		private int x;
		private int y;

		InMemoryAgent(String name, int x, int y) {
			this.name = name;
			this.x = x;
			this.y = y;
		}

		@Override public String name() { return name; }
		@Override public int x() { return x; }
		@Override public int y() { return y; }
		@Override public int height() { return 0; }
		@Override public boolean arrivedAt(int tx, int ty, int range) {
			return Math.max(Math.abs(x - tx), Math.abs(y - ty)) <= range;
		}
		@Override public boolean isIdle() { return true; }
		@Override public void walkTo(int tx, int ty) {
			x += Integer.signum(tx - x);
			y += Integer.signum(ty - y);
		}
		@Override public boolean interactObject(int o, int tx, int ty, ObjectClick c, int range) {
			return true;
		}
	}

	/**
	 * A {@link BotContext} over a real {@link NpcAgent}.
	 *
	 * <p>Test-only, and deliberately narrow: it implements the actor half and the trace, and throws for
	 * every player-only fact. Anything that refuses to fake an inventory is a better demonstration of the
	 * seam than a double that pretends to have one.
	 */
	private static final class NpcBotContext implements BotContext {

		private final NpcAgent agent;
		private final BotTrace trace = new BotTrace();
		private int ticks;

		NpcBotContext(NPC npc) {
			this.agent = new NpcAgent(npc);
		}

		private static UnsupportedOperationException playerOnly(String what) {
			return new UnsupportedOperationException("an NPC has no " + what + "; this fact is player-only "
					+ "and is deliberately not faked");
		}

		@Override public Agent agent() { return agent; }
		@Override public int x() { return agent.x(); }
		@Override public int y() { return agent.y(); }
		@Override public int height() { return agent.height(); }
		@Override public boolean arrivedAt(int x, int y, int range) { return agent.arrivedAt(x, y, range); }
		@Override public boolean isIdle() { return agent.isIdle() && !isSkilling(); }
		@Override public boolean isSkilling() { return false; }
		@Override public int freeSlots() { throw playerOnly("inventory"); }
		@Override public boolean hasItem(int itemId) { throw playerOnly("inventory"); }
		@Override public boolean isBanking() { throw playerOnly("bank interface"); }
		@Override public boolean isDead() { return false; }
		@Override public int skillLevel(int skill) { throw playerOnly("skills"); }
		@Override public BotTrace trace() { return trace; }
		@Override public int ticksInState() { return ticks; }
		@Override public int random(int bound) { return 0; }
		@Override public void walkTo(int x, int y) { agent.walkTo(x, y); }
		@Override public boolean interactObject(int o, int x, int y, ObjectClick c, int range) {
			return agent.interactObject(o, x, y, c, range);
		}
		@Override public void openBank() { throw playerOnly("bank"); }
		@Override public boolean depositItem(int itemId) { throw playerOnly("bank"); }
		@Override public void onStateEntered() { ticks = 0; }

		@Override
		public void onTick() {
			ticks++;
			trace.advance();
		}
	}
}
