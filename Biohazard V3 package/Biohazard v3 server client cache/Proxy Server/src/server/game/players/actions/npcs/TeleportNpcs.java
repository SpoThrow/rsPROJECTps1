package server.game.players.actions.npcs;

/**
 * Generated from {@code ActionHandler}'s {@code switch(npcType)} by
 * {@code build/purge/gen-npc-table.js}; do not retype the table by hand.
 *
 * <p>Rows are {@code {npcId, x, y, height}}: first click teleports the player to that
 * tile. The ids and the destinations are unrelated, so the table is data. Generated
 * from the switch; {@code NpcTablesTest} pins it.
 */
public final class TeleportNpcs {

	static final int[][] TELEPORTS = {
			{ 378, 2977, 9515, 1 },
			{ 556, 2872, 5269, 2 },
			{ 2257, 2919, 5274, 0 },
			{ 2259, 2885, 5344, 2 },
	};

	// Third click, from a separate switch. 553's case wrapped the teleport in a
	// commented-out completion check, so the guard is disabled and it always moves.
	static final int[][] THIRD_CLICK = {
			{ 553, 2898, 4819, 0 },
	};

	// 2258 uses startTeleport (the animated form) rather than movePlayer, and both
	// clicks do the same thing -- the switch comment claims the second click opens
	// shops, but the code says otherwise.
	static final int[] ANIMATED = { 2258 };

	private TeleportNpcs() {
	}

	static void register() {
		for (int[] row : TELEPORTS) {
			NpcActionHandler.register(row[0], NpcClick.FIRST,
					(c, npcType) -> c.getPA().movePlayer(row[1], row[2], row[3]));
		}
		for (int[] row : THIRD_CLICK) {
			NpcActionHandler.register(row[0], NpcClick.THIRD,
					(c, npcType) -> c.getPA().movePlayer(row[1], row[2], row[3]));
		}
		for (int id : ANIMATED) {
			NpcActionHandler.register(id, NpcClick.FIRST,
					(c, npcType) -> c.getPA().startTeleport(3039, 4834, 0, "modern"));
			NpcActionHandler.register(id, NpcClick.SECOND,
					(c, npcType) -> c.getPA().startTeleport(3039, 4834, 0, "modern"));
		}
	}
}
