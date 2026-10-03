package server.game.players.actions.objects;

/**
 * Bank booth and chest objects whose second click opens the bank, lifted out of
 * {@code ActionHandler}.
 */
public final class BankBoothObjects {

	private static final int[] IDS = { 2213, 26972, 14367 };

	private BankBoothObjects() {
	}

	static void register() {
		for (int id : IDS) {
			ObjectHandler.register(id, ObjectClick.SECOND,
					(c, objectType, objectX, objectY) -> c.getPA().openUpBank());
		}
	}

	static int[] ids() {
		return IDS.clone();
	}
}
