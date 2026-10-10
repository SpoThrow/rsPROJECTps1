package server.game.players.actions.items;

import server.content.skills.Herblore;

/**
 * Every herblore recipe, registered as a pair.
 *
 * <p>Herblore is the third family the registry owns outright and the largest: all four of its
 * tables are pairs of item ids, so unlike arrows and bolts there is nothing left in the legacy body
 * of {@code UseItem.ItemonItem} to migrate one row at a time. The block that used to sit there is
 * gone rather than left behind, because a recipe the registry claims never reaches it.
 *
 * <p>Cleaning is the one table that is not here, and cannot be: a grimy herb is clicked rather than
 * combined with anything, so it belongs to {@code ClickItem}.
 */
public final class HerbloreItemUses {

	private HerbloreItemUses() {
	}

	/**
	 * One entry per recipe in every pair-keyed herblore table.
	 *
	 * <p>The pestle is registered as a pair with each grindable even though it is never consumed:
	 * the registry's key is what decides whether a click is herblore's, and the pestle is one side of
	 * that decision.
	 */
	public static void register() {
		for (Herblore.Grinding grindable : Herblore.Grinding.values()) {
			ItemUseRegistry.register(grindable.getInput(), Herblore.PESTLE_AND_MORTAR,
					(c, itemUsed, useWith) -> Herblore.grind(c, itemUsed, useWith));
		}
		for (Herblore.Unfinished unfinished : Herblore.Unfinished.values()) {
			ItemUseRegistry.register(unfinished.getBase(), unfinished.getHerb(),
					(c, itemUsed, useWith) -> Herblore.mix(c, itemUsed, useWith));
		}
		for (Herblore.Finished finished : Herblore.Finished.values()) {
			ItemUseRegistry.register(finished.getUnfinished(), finished.getSecondary(),
					(c, itemUsed, useWith) -> Herblore.mix(c, itemUsed, useWith));
		}
	}
}
