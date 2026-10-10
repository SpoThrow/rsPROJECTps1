package server.game.players.actions.items;

import server.content.skills.Fletching;

/**
 * The fletching item pairs that live in {@link ItemUseRegistry} rather than in the legacy
 * body of {@code UseItem.ItemonItem}.
 *
 * <p>Currently only bow stringing, because it is the one fletching family that had no
 * inline block to migrate: {@code 1777} was unhandled entirely. Everything written inline
 * there — arrows, bolts, bolt tipping, the knife-on-logs menu — stays where it is until it
 * is moved deliberately, so the registry and the legacy checks never both run for a pair.
 */
public final class FletchingItemUses {

	private FletchingItemUses() {
	}

	/**
	 * One entry per unstrung bow. Registered as the pair {@code (unstrung, 1777)} because the
	 * registry sorts the two ids before keying on them, which is what makes the bowstring
	 * arriving as {@code itemUsed} or as {@code useWith} the same click.
	 */
	public static void register() {
		for (Fletching.Stringing stringing : Fletching.Stringing.values()) {
			ItemUseRegistry.register(stringing.getUnstrung(), Fletching.BOW_STRING,
					(c, itemUsed, useWith) -> Fletching.stringBow(c, itemUsed, useWith));
		}
	}
}
