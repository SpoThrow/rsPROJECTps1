package server.game.players.actions.objects;

import server.game.minigames.bountyhunter.BountyHunter;

/**
 * The bounty hunter crater entrance and exit, lifted out of {@code ActionHandler}.
 *
 * <p>Three ids share the entrance body as fall-through labels in the switch, so they
 * are three registrations to one handler here.
 */
public final class BountyHunterObjects {

	private static final int[] ENTRANCES = { 28119, 28120, 28121 };
	private static final int EXIT = 28122;

	private BountyHunterObjects() {
	}

	static void register() {
		for (int entrance : ENTRANCES) {
			ObjectHandler.register(entrance, ObjectClick.FIRST,
					(c, objectType, objectX, objectY) -> BountyHunter.enterCrater(c, objectType));
		}
		ObjectHandler.register(EXIT, ObjectClick.FIRST,
				(c, objectType, objectX, objectY) -> BountyHunter.leaveCrater(c));
	}

	static int[] entrances() {
		return ENTRANCES.clone();
	}

	static int exit() {
		return EXIT;
	}
}
