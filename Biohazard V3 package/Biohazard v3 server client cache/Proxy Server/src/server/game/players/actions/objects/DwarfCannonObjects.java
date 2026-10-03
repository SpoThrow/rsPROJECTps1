package server.game.players.actions.objects;

import server.game.content.DwarfCannon;

/**
 * Dwarf cannon objects, migrated out of {@code ActionHandler}.
 *
 * <p>First click places the cannon; second click picks it up. Both are registered
 * here because they are the same four object ids, keyed on different clicks.
 *
 * <p>{@code DwarfCannon} also has predicate-based guards in {@code ActionHandler}'s
 * preamble and at the top of {@code thirdClickObject} ({@code isCannonObject}),
 * which claim objects by test rather than by exact id and therefore stay where they
 * are.
 */
public final class DwarfCannonObjects {

	private static final int[] IDS = { 6, 7, 8, 9 };

	private DwarfCannonObjects() {
	}

	static void register() {
		for (int id : IDS) {
			ObjectHandler.register(id, ObjectClick.FIRST, (c, objectType, objectX, objectY) ->
					DwarfCannon.firstClick(c, objectType, objectX, objectY));
			ObjectHandler.register(id, ObjectClick.SECOND, (c, objectType, objectX, objectY) ->
					DwarfCannon.pickup(c, objectX, objectY));
		}
	}

	static int[] ids() {
		return IDS.clone();
	}
}
