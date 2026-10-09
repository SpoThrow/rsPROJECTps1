package server.game.bots;

import core.util.Misc;
import server.Server;
import server.game.npcs.NPC;
import server.game.npcs.NPCHandler;
import server.game.players.actions.objects.ObjectClick;

/**
 * The {@link Agent} for an NPC — roadmap Phase G, and the second implementation that proves the seam is
 * real rather than a rename of {@code Client}.
 *
 * <p><b>What is genuinely shared.</b> Position, walking, idleness and identity mean the same thing for an
 * NPC as for a player, so every tree that only needs those — travel, waypoints, patrol, and
 * {@code WalkTo}/{@code WalkToNearest} — now runs on an NPC with no change to the state classes. That is
 * the phase's deliverable, and the tests drive a real tree through this agent to show it.
 *
 * <p><b>Movement is the NPC idiom, not the player one.</b> An NPC does not queue a route: it is stepped
 * one tile inside its own tick, so {@link #walkTo} sets the desired step, applies clipping and advances
 * it, the same shape {@code WorldAdventurer.walkToward} used by hand. A consequence worth stating plainly
 * is that {@link #isIdle} reads the pending step rather than a queue, so an NPC that has just been asked
 * to walk reports not-idle until its update flags are cleared at the end of the tick.
 *
 * <p><b>Object interaction is not supported, and that is a real limit rather than an oversight.</b>
 * Object dispatch is {@code ObjectHandler.dispatch(Client, ...)} and its {@code ObjectAction}s take a
 * {@code Client}, so there is no way to route a click for an NPC through the registries the migrated
 * skills live in. It throws instead of returning false, because a silent false is indistinguishable from
 * "out of range" and would make a {@code Gather} loop retry forever; a throw names the problem at the
 * point it happens. Making this work is a change to the skill-dispatch path so it takes an actor, which
 * is deliberately out of scope for the seam (roadmap §5.1 — G is "add the agent, re-point one
 * constructor", not "rewrite object dispatch").
 */
public final class NpcAgent implements Agent {

	private final NPC npc;

	public NpcAgent(NPC npc) {
		if (npc == null) {
			throw new IllegalArgumentException("an npc agent needs an npc");
		}
		this.npc = npc;
	}

	/** The NPC this agent drives. */
	public NPC npc() {
		return npc;
	}

	@Override
	public String name() {
		NPCHandler handler = Server.npcHandler;
		String type = handler == null ? null : handler.getNpcListName(npc.npcType);
		return type == null ? "npc " + npc.npcType : type;
	}

	@Override
	public int x() {
		return npc.absX;
	}

	@Override
	public int y() {
		return npc.absY;
	}

	@Override
	public int height() {
		return npc.heightLevel;
	}

	@Override
	public boolean arrivedAt(int x, int y, int range) {
		// Chebyshev distance, matching what the player side's goodDistance measures for tile ranges.
		return Math.max(Math.abs(npc.absX - x), Math.abs(npc.absY - y)) <= range;
	}

	@Override
	public boolean isIdle() {
		// No step pending. NPCHandler clears moveX/moveY when it clears the NPC's update flags, so this
		// reads as idle again once the tick that moved it has been flushed.
		return npc.moveX == 0 && npc.moveY == 0;
	}

	@Override
	public void walkTo(int x, int y) {
		NPCHandler handler = Server.npcHandler;
		if (handler == null) {
			return;
		}
		npc.moveX = handler.GetMove(npc.absX, x);
		npc.moveY = handler.GetMove(npc.absY, y);
		if (npc.moveX == 0 && npc.moveY == 0 && (npc.absX != x || npc.absY != y)) {
			// Clipped on both axes, or already aligned on one: take a diagonal step so a blocked
			// approach still makes progress instead of standing still forever.
			npc.moveX = Misc.random(2) - 1;
			npc.moveY = Misc.random(2) - 1;
		}
		handler.handleClipping(npc.npcId);
		npc.getNextNPCMovement(npc.npcId);
		npc.updateRequired = true;
	}

	@Override
	public boolean interactObject(int objectId, int x, int y, ObjectClick click, int range) {
		throw new UnsupportedOperationException("an NPC cannot interact with objects: object dispatch is "
				+ "Client-typed (ObjectHandler.dispatch / ObjectAction), so there is no registry path for an "
				+ "NPC click. Use a player agent for anything that clicks scenery.");
	}
}
