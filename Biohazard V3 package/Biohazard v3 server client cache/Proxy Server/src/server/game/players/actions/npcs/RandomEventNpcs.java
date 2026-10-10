package server.game.players.actions.npcs;

import server.game.minigames.randomevents.RandomEventManager;

/**
 * NPC actions belonging to the random event system.
 *
 * <p>Hand-written rather than generated: the other tables in this package were lifted out of
 * {@code ActionHandler}'s {@code switch(npcType)} by {@code build/purge/gen-npc-table.js}, and the
 * genie was never in that switch. It is new behaviour, so it belongs in its own file and must not
 * be added to the generated tables.
 *
 * <p>The genie is spawned and torn down by {@link RandomEventManager#spawnGenie}, which also owns
 * the reward. Nothing about the event lives here beyond "clicking it talks to it".
 */
public final class RandomEventNpcs {

	private RandomEventNpcs() {
	}

	static void register() {
		NpcActionHandler.register(RandomEventManager.GENIE_NPC, NpcClick.FIRST,
				(c, npcType) -> RandomEventManager.talkToGenie(c));
	}
}
