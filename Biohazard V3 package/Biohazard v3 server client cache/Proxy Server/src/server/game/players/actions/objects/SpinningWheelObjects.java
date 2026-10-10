package server.game.players.actions.objects;

import server.content.skills.Spinning;

/**
 * The spinning wheel objects, lifted out of no switch — there was no case for them anywhere,
 * which is why spinning did not work.
 *
 * <p>First click, not second: the wheel's left-click option is "Spin". Second click is left
 * unclaimed so the legacy switch stays in charge of it, the same arrangement every other family
 * here uses.
 *
 * <p>Wool and flax are <em>not</em> registered here. They are item-on-object pairs and live in
 * {@code SpinningItemUses}, registered against {@code ItemOnObjectRegistry} — a different
 * registry with a different bootstrap, so neither family's class initialisation depends on the
 * other.
 */
public final class SpinningWheelObjects {

	private SpinningWheelObjects() {
	}

	static void register() {
		for (int wheel : Spinning.WHEEL_OBJECTS) {
			ObjectHandler.register(wheel, ObjectClick.FIRST,
					(c, objectType, objectX, objectY) -> Spinning.openWheel(c));
		}
	}

	static int[] ids() {
		return Spinning.WHEEL_OBJECTS.clone();
	}
}
