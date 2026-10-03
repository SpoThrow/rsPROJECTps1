package server.game.players.actions.objects;

import server.content.skills.Smelting;

/**
 * Furnace objects whose second click opens the smelting interface, lifted out of
 * {@code ActionHandler}.
 *
 * <p>Distinct from {@code SmeltingButtons} in the button package: that one is the
 * "smelt N bars" buttons inside the interface, this is the furnace objects that open
 * it. Three ids share the body as fall-through labels in the switch.
 */
public final class FurnaceObjects {

	private static final int[] IDS = { 11666, 3044, 2781 };

	private FurnaceObjects() {
	}

	static void register() {
		for (int id : IDS) {
			ObjectHandler.register(id, ObjectClick.SECOND,
					(c, objectType, objectX, objectY) -> Smelting.openInterface(c));
		}
	}

	static int[] ids() {
		return IDS.clone();
	}
}
