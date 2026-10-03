package server.game.players.actions.objects;

import server.game.minigames.castlewars.CastleWars;

/**
 * Castle Wars door objects whose second click attacks the door, lifted out of
 * {@code ActionHandler}.
 *
 * <p>{@code CastleWars.attackDoor} returns a boolean that the switch discarded, and
 * so does this. Four ids across two door pairs share the body as fall-through labels.
 */
public final class CastleWarsDoorObjects {

	private static final int[] IDS = { 4423, 4424, 4427, 4428 };

	private CastleWarsDoorObjects() {
	}

	static void register() {
		for (int id : IDS) {
			ObjectHandler.register(id, ObjectClick.SECOND,
					(c, objectType, objectX, objectY) -> CastleWars.attackDoor(c, objectType));
		}
	}

	static int[] ids() {
		return IDS.clone();
	}
}
