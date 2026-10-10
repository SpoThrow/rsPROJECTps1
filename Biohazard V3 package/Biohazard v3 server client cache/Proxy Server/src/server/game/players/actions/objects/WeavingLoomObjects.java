package server.game.players.actions.objects;

import server.content.skills.Weaving;

/**
 * The loom objects, lifted out of no switch — nothing handled an object named "Loom" anywhere,
 * which is half of why weaving did not work.
 *
 * <p>First click, not second: the loom's left-click is the only option the client sends for it.
 * Second click is left unclaimed so the legacy switch stays in charge of it, the same arrangement
 * every other family here uses.
 *
 * <p>The materials are <em>not</em> registered here. Balls of wool and jute fibres are
 * item-on-object pairs and live in {@code WeavingItemUses}, against {@code ItemOnObjectRegistry} —
 * a different registry with a different bootstrap, so neither family's class initialisation
 * depends on the other.
 */
public final class WeavingLoomObjects {

	private WeavingLoomObjects() {
	}

	static void register() {
		for (int loom : Weaving.LOOM_OBJECTS) {
			ObjectHandler.register(loom, ObjectClick.FIRST,
					(c, objectType, objectX, objectY) -> Weaving.clickLoom(c));
		}
	}
}
