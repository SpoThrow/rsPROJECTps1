package server.game.players.actions.items;

import server.content.skills.Spinning;

/**
 * Wool and flax used on a spinning wheel.
 *
 * <p>This is the explicit half of spinning: clicking the wheel spins whatever single material you
 * are carrying, and using one of the two on the wheel says which — including when you are
 * carrying both, which is the case the click deliberately refuses to guess at.
 *
 * <p>Registered against {@link ItemOnObjectRegistry}, which is consulted from
 * {@code UseItem.ItemonObject} before its switch. The wheel ids come from
 * {@link Spinning#WHEEL_OBJECTS} so this and the click family cannot drift apart.
 */
public final class SpinningItemUses {

	private SpinningItemUses() {
	}

	public static void register() {
		for (int wheel : Spinning.WHEEL_OBJECTS) {
			for (Spinning.Material material : Spinning.Material.values()) {
				ItemOnObjectRegistry.register(material.getMaterial(), wheel,
						(c, itemId, objectId, objectX, objectY) -> Spinning.spin(c, material));
			}
		}
	}
}
