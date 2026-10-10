package server.game.players.actions.items;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import server.game.players.Client;
import server.game.players.actions.objects.ObjectHandler;

/**
 * Registry of item-on-item actions, keyed on the unordered pair of item ids, consulted
 * before the inline checks in {@code UseItem.ItemonItem}.
 *
 * <p>Same strangler arrangement as {@link ObjectHandler}: {@link #dispatch} runs after the
 * guards that cannot be expressed as a key and before the body of the method, so a recipe
 * can be moved out one family at a time without the registry and the legacy checks ever
 * both running for the same click.
 *
 * <p><b>A duplicate registration throws.</b> This is not decoration. The legacy method is a
 * sequence of {@code if} blocks rather than a {@code switch}, so two overlapping recipes
 * both fire — that is a real bug class the switch-based registries never had. A map would
 * silently let the second registration shadow the first, so the guarantee has to be
 * re-established here or the migration loses it.
 */
public final class ItemUseRegistry {

	private static final Map<Long, ItemUseAction> actions = new ConcurrentHashMap<>();

	static {
		FletchingItemUses.register();
	}

	private ItemUseRegistry() {
	}

	public static void register(int itemA, int itemB, ItemUseAction action) {
		if (actions.putIfAbsent(key(itemA, itemB), action) != null) {
			throw new IllegalStateException("item pair " + Math.min(itemA, itemB) + "+"
					+ Math.max(itemA, itemB) + " is registered twice");
		}
	}

	/**
	 * @return true if a handler claimed this pair, meaning the legacy body of
	 *         {@code ItemonItem} must not also run for it.
	 */
	public static boolean dispatch(Client c, int itemUsed, int useWith) {
		ItemUseAction action = actions.get(key(itemUsed, useWith));
		if (action == null) {
			return false;
		}
		action.handle(c, itemUsed, useWith);
		return true;
	}

	/**
	 * Order-independent, so {@code isRegistered(a, b)} and {@code isRegistered(b, a)} agree.
	 */
	public static boolean isRegistered(int itemA, int itemB) {
		return actions.containsKey(key(itemA, itemB));
	}

	public static int size() {
		return actions.size();
	}

	/**
	 * One key for both orders. The pair is sorted first so that {@code (a, b)} and
	 * {@code (b, a)} land on the same entry; the low id goes in the high 32 bits.
	 */
	private static long key(int itemA, int itemB) {
		int low = Math.min(itemA, itemB);
		int high = Math.max(itemA, itemB);
		return ((long) low << 32) | (high & 0xFFFFFFFFL);
	}
}
