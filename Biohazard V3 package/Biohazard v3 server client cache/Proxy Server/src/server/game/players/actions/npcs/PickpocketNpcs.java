package server.game.players.actions.npcs;

/**
 * NPCs that can be pickpocketed, lifted out of {@code ActionHandler}'s second-click
 * switch.
 *
 * <p>This was one fall-through run of 46 labels sharing a single
 * {@code c.getThieving().pickpocketNpc(c, npcType)} body, so the ids carry no data:
 * the handler takes the clicked npc type. Hence a plain id list rather than a table.
 */
public final class PickpocketNpcs {

	private static final int[] IDS = {
			2234, 2235, 1, 2, 3, 4, 5, 6, 7, 1757, 1758, 1759, 1760, 1761, 1715, 1714,
			1710, 1711, 1712, 15, 18, 187, 9, 10, 1880, 1881, 1926, 1927, 1928, 1929,
			1930, 1931, 23, 26, 1883, 1884, 32, 1904, 1905, 20, 365, 2256, 66, 67, 68, 21,
	};

	private PickpocketNpcs() {
	}

	static void register() {
		for (int id : IDS) {
			NpcActionHandler.register(id, NpcClick.SECOND,
					(c, npcType) -> c.getThieving().pickpocketNpc(c, npcType));
		}
	}

	static int[] ids() {
		return IDS.clone();
	}
}
