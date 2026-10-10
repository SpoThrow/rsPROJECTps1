package server.game.players.actions.items;

import server.content.skills.Weaving;

/**
 * Balls of wool and jute fibres used on a loom.
 *
 * <p>The explicit half of weaving: clicking the loom weaves whatever single material you are
 * carrying, and using one of the two says which — including when you are carrying both, which is
 * the case the click deliberately refuses to guess at.
 *
 * <p>Registered against {@link ItemOnObjectRegistry}, which is consulted from
 * {@code UseItem.ItemonObject} before its switch. The loom ids come from
 * {@link Weaving#LOOM_OBJECTS} so this and the click family cannot drift apart.
 */
public final class WeavingItemUses {

	private WeavingItemUses() {
	}

	public static void register() {
		for (int loom : Weaving.LOOM_OBJECTS) {
			for (Weaving.Weave weave : Weaving.Weave.values()) {
				ItemOnObjectRegistry.register(weave.getMaterial(), loom,
						(c, itemId, objectId, objectX, objectY) -> Weaving.weave(c, weave));
			}
		}
	}
}
