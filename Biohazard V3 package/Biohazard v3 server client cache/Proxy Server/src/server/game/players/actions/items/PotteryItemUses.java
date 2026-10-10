package server.game.players.actions.items;

import server.content.skills.Pottery;

/**
 * Soft clay on a potter's wheel, and unfired clay on a pottery oven.
 *
 * <p>Registered against {@link ItemOnObjectRegistry}, which {@code UseItem.ItemonObject} consults
 * before its switch, keyed on the {@code (item, object)} pair. That is what lets the oven stage
 * exist at all: object 2643 already belongs to {@code JewelryMaking.mouldInterface} for gold bars,
 * and a pair-keyed registry claims only the fifteen (unfired, oven) combinations this file names
 * while gold bar on 2643 carries on down the switch.
 *
 * <p>Both halves read their object lists from {@link Pottery}, so the click family and this one
 * cannot drift apart about which objects are wheels or ovens.
 */
public final class PotteryItemUses {

	private PotteryItemUses() {
	}

	public static void register() {
		for (int wheel : Pottery.WHEEL_OBJECTS) {
			ItemOnObjectRegistry.register(Pottery.SOFT_CLAY, wheel,
					(c, itemId, objectId, objectX, objectY) -> Pottery.openWheel(c));
		}
		for (int oven : Pottery.OVEN_OBJECTS) {
			for (Pottery.Shape shape : Pottery.Shape.values()) {
				ItemOnObjectRegistry.register(shape.getUnfired(), oven,
						(c, itemId, objectId, objectX, objectY) -> Pottery.fire(c, itemId));
			}
		}
	}
}
