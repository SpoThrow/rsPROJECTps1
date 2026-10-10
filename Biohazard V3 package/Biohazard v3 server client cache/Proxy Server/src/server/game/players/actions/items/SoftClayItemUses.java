package server.game.players.actions.items;

import server.content.skills.SoftClay;

/**
 * Soft clay's item pairs — the third family migrated out of the legacy body of
 * {@code UseItem.ItemonItem}.
 *
 * <p>Like bow stringing before it, there was no inline block to move: no pair in that method ever
 * mentioned clay or a water container, which is exactly why {@code 1761} had no source. The
 * registration is the whole of the wiring; the two orders are the same click because
 * {@link ItemUseRegistry} sorts the pair before keying on it.
 */
public final class SoftClayItemUses {

	private SoftClayItemUses() {
	}

	/**
	 * One entry per container, all against clay. Registered from the enum rather than by hand so a
	 * row added to {@link SoftClay.WaterContainer} is reachable without a second edit, and so the
	 * registry's duplicate check fails loudly if two rows ever share an id.
	 */
	public static void register() {
		for (SoftClay.WaterContainer container : SoftClay.WaterContainer.values()) {
			ItemUseRegistry.register(SoftClay.CLAY, container.getFull(),
					(c, itemUsed, useWith) -> SoftClay.mix(c, itemUsed, useWith));
		}
	}
}
