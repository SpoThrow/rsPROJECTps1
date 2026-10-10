package server.game.players.actions.items;

import server.content.skills.Fletching;

/**
 * The fletching item pairs that live in {@link ItemUseRegistry} rather than in the legacy
 * body of {@code UseItem.ItemonItem}.
 *
 * <p>Two families so far, and both are here for the same reason: the registry can own them
 * outright without a pair being handled twice. Bow stringing had no inline block to migrate
 * ({@code 1777} was unhandled entirely). Darts are new, and could not have gone inline even if
 * the style were preferred — the bolt block claimed their exact pairs (dart tip and feather) for
 * as long as darts did not exist. Everything else written inline there — arrows, bolts, bolt
 * tipping, the knife-on-logs menu — stays where it is until it is moved deliberately, so the
 * registry and the legacy checks never both run for a pair.
 */
public final class FletchingItemUses {

	private FletchingItemUses() {
	}

	/**
	 * One entry per unstrung bow, plus one per dart.
	 *
	 * <p>Both are registered as plain pairs because the registry sorts the two ids before keying on
	 * them, which is what makes the bowstring, or the feather, arriving as {@code itemUsed} or as
	 * {@code useWith} the same click.
	 */
	public static void register() {
		for (Fletching.Stringing stringing : Fletching.Stringing.values()) {
			ItemUseRegistry.register(stringing.getUnstrung(), Fletching.BOW_STRING,
					(c, itemUsed, useWith) -> Fletching.stringBow(c, itemUsed, useWith));
		}
		for (Fletching.Darts darts : Fletching.Darts.values()) {
			ItemUseRegistry.register(darts.getItem1(), darts.getItem2(),
					(c, itemUsed, useWith) -> Fletching.makeDarts(c, itemUsed, useWith));
		}
	}
}
