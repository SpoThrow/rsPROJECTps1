package server.game.players.actions.objects;

import server.content.skills.Pottery;

/**
 * The potter's wheel objects, lifted out of no switch — there was no case for them in
 * {@code ActionHandler}, which is half of why pottery did not work.
 *
 * <p>First click, not second: the wheel's option is "Use" and it opens the "what would you like
 * to make?" chatbox. Second click is left unclaimed so the legacy switch stays in charge of it, the
 * same arrangement every other family here uses.
 *
 * <p>Soft clay is <em>not</em> registered here. It is an item-on-object pair and lives in
 * {@code PotteryItemUses}, against {@code ItemOnObjectRegistry} — a different registry with a
 * different bootstrap, so neither family's class initialisation depends on the other.
 *
 * <p>The oven objects are deliberately absent, both from here and from that item family's click
 * side. 2643 is the object this server hangs {@code JewelryMaking.mouldInterface} on, so a wheel
 * handler that claimed all six clay objects by id would silently take jewellery with it.
 */
public final class PotteryWheelObjects {

	private PotteryWheelObjects() {
	}

	static void register() {
		for (int wheel : Pottery.WHEEL_OBJECTS) {
			ObjectHandler.register(wheel, ObjectClick.FIRST,
					(c, objectType, objectX, objectY) -> Pottery.openWheel(c));
		}
	}
}
