package server.game.players.actions.objects;

import server.Server;

/**
 * Door objects dispatched to {@code ObjectManager.doorHandling}, lifted out of
 * {@code ActionHandler}.
 *
 * <p>Eleven fall-through labels sharing one call, so the ids carry no data and this is a
 * plain list.
 *
 * <p>The call passes {@code c.objectX}/{@code c.objectY} — the coordinates the client
 * last reported for the object — rather than the {@code obX}/{@code obY} the method was
 * given. The original did that, so this does too; swapping them would change which tile
 * the door acts on.
 *
 * <p>{@code 1516} and {@code 1519} are registered separately because they never simply
 * shared that body. They had an {@code objectY == 9698} branch of their own and then
 * <em>fell through</em> into the door call for every other coordinate, so their handler
 * has to carry both halves. Making that explicit is what allows the switch cases to be
 * deleted: while they were still present, removing the door labels on their own would
 * have dropped {@code 1516} and {@code 1519} into {@code case 9319} and moved the player
 * up a height level instead of opening the gate.
 */
public final class DoorObjects {

	private static final int[] IDS = {
			1530, 1531, 1533, 1534, 11712, 11711, 11707, 11708, 6725, 3198, 3197,
	};

	private static final int[] GATES = { 1516, 1519 };

	private static final int GATE_WALK_AROUND_Y = 9698;

	private DoorObjects() {
	}

	static void register() {
		for (int id : IDS) {
			ObjectHandler.register(id, ObjectClick.FIRST, (c, objectType, objectX, objectY) ->
					Server.objectHandler.doorHandling(objectType, c.objectX, c.objectY, 0));
		}
		for (int id : GATES) {
			ObjectHandler.register(id, ObjectClick.FIRST, (c, objectType, objectX, objectY) -> {
				if (c.objectY == GATE_WALK_AROUND_Y) {
					if (c.position.absY >= c.objectY) {
						c.getPA().walkTo(0, -1);
					} else {
						c.getPA().walkTo(0, 1);
					}
				} else {
					Server.objectHandler.doorHandling(objectType, c.objectX, c.objectY, 0);
				}
			});
		}
	}

	static int[] ids() {
		return IDS.clone();
	}

	static int[] gates() {
		return GATES.clone();
	}
}
