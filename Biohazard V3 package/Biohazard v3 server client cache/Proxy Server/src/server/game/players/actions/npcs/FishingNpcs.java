package server.game.players.actions.npcs;

import server.content.skills.Fishing;

/**
 * Fishing spot NPCs, lifted out of {@code ActionHandler}.
 *
 * <p>The same five ids appear in both switches: first click calls
 * {@code Fishing.forSpot(npcType, false)} and second click the same with {@code true}.
 * So one id is registered twice, on different clicks — which is exactly the case the
 * {@code (npcType, NpcClick)} key exists for, and why the two must not be collapsed
 * into one table.
 */
public final class FishingNpcs {

	private static final int[] IDS = { 309, 312, 313, 316, 326 };

	private FishingNpcs() {
	}

	static void register() {
		for (int id : IDS) {
			NpcActionHandler.register(id, NpcClick.FIRST,
					(c, npcType) -> Fishing.setupFishing(c, Fishing.forSpot(npcType, false)));
			NpcActionHandler.register(id, NpcClick.SECOND,
					(c, npcType) -> Fishing.setupFishing(c, Fishing.forSpot(npcType, true)));
		}
	}

	static int[] ids() {
		return IDS.clone();
	}
}
