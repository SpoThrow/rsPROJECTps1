package server.game.items;

import server.Config;
import server.Server;
import server.world.ItemHandler;

/**
 * Item definition lookups that <b>never return null</b> and never throw on an id that has
 * no definition.
 *
 * <p><b>Why this exists.</b> {@link Item#getItemName(int)} returns {@code null} for an id
 * with no definition, and a great deal of this codebase decides things from item
 * <em>names</em> — {@link ItemHandler#getRequirements} alone is a long chain of
 * {@code itemName.contains("bronze")} branches. So an id with no row does not present as a
 * missing feature; it presents as a {@code NullPointerException} at the point some unrelated
 * feature asks what the item is called.
 *
 * <p>That is a live problem now rather than a hypothetical one. {@code QOL_PLAN.md} §2
 * deliberately writes ids we do not have yet into content tables, so that a feature is
 * inert until the higher-revision item it needs is imported and then switches itself on
 * without a code change. Every such id is one careless {@code getItemName(...).contains(...)}
 * away from a crash. Lookups here answer with {@link #UNKNOWN_NAME} instead, which makes an
 * undefined item harmless: name-based checks simply do not match it.
 *
 * <p><b>This is a new entry point, not a change to the old ones.</b>
 * {@link Item#getItemName(int)} and {@code ItemAssistant.getItemName(int)} are left exactly
 * as they are. They are called from hundreds of places that may depend on their present
 * behaviour, including on the difference between {@code null} and a string, and changing
 * what a working lookup returns is not something to do behind a phase-0 scaffolding change.
 * New content and new name-based checks use this class.
 *
 * <p><b>Cost.</b> {@code ItemList[]} is indexed by item id — {@code ItemHandler.newItemList}
 * stores at {@code ItemList[ItemId]} — so the common path is a single array read, with a
 * linear-scan fallback for an entry that is not at its own index. That fallback is what the
 * two legacy accessors do for every lookup, so this is not slower than what it replaces.
 *
 * <p><b>Safe before load.</b> {@code Server.itemHandler} is null until the server
 * constructs it, so this returns {@code null} (and {@link #name} returns
 * {@link #UNKNOWN_NAME}) rather than throwing. That is what lets a unit test ask about an
 * item without a running server.
 */
public final class ItemDefinitions {

	/** What an item with no definition is called. Never returned by the legacy accessors. */
	public static final String UNKNOWN_NAME = "Unknown item";

	private ItemDefinitions() {
	}

	/** @return the definition, or {@code null} if this id has none. */
	public static ItemList get(int id) {
		if (id < 0 || id >= Config.ITEM_LIMIT) {
			return null;
		}
		ItemList[] list = listOrNull();
		if (list == null) {
			return null;
		}
		ItemList direct = list[id];
		if (direct != null && direct.itemId == id) {
			return direct;
		}
		for (ItemList candidate : list) {
			if (candidate != null && candidate.itemId == id) {
				return candidate;
			}
		}
		return null;
	}

	public static boolean exists(int id) {
		return get(id) != null;
	}

	/**
	 * @return the item's name, or {@link #UNKNOWN_NAME} if it has no definition. Never null,
	 *         so it is safe to call {@code name(id).contains(...)} on.
	 */
	public static String name(int id) {
		ItemList def = get(id);
		if (def == null || def.itemName == null) {
			return UNKNOWN_NAME;
		}
		return def.itemName;
	}

	/** @return the shop value, or {@code 0} if this id has no definition. */
	public static int value(int id) {
		ItemList def = get(id);
		return def == null ? 0 : (int) def.ShopValue;
	}

	private static ItemList[] listOrNull() {
		ItemHandler handler = Server.itemHandler;
		return handler == null ? null : handler.ItemList;
	}
}
