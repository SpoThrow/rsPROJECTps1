package server.game.players.actions.items;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import server.game.players.Client;
import server.game.players.actions.objects.ObjectHandler;

/**
 * Registry of item-on-object actions, keyed on {@code (itemId, objectId)}, consulted before
 * the {@code switch} in {@code UseItem.ItemonObject}.
 *
 * <p><b>Keyed on the pair, not the object alone.</b> Every case in the legacy switch is
 * really a pair — {@code case 3044: if (itemId == 2357) ...} — so an object-centric key
 * could not express "this item yes, that item no" and would force a whole object out of the
 * switch in one go. The pair is what allows one item on a water source to be migrated while
 * another item on the same object stays where it is.
 *
 * <p><b>One group of object ids is not free to claim here.</b>
 * {@code ItemOnObject.processPacket} runs its own cooking {@code switch} <em>after</em>
 * calling {@code UseItem.ItemonObject}, so registering one of those ids would leave the
 * cooking handler and the new handler both running for the same click — the exact
 * double-handling the registries exist to prevent. {@link #register} refuses them, and
 * {@link #COOKING_OBJECTS} exists so that refusal is visible and testable rather than a
 * comment. When the cooking switch is itself migrated, that guard comes off with it.
 */
public final class ItemOnObjectRegistry {

	/**
	 * Object ids that {@code ItemOnObject.processPacket} handles itself, after the call into
	 * {@code UseItem}. Not claimable here while that switch exists.
	 */
	public static final int[] COOKING_OBJECTS = {
			12269, 2732, 114, 9374, 2728, 25465, 11404, 11405, 11406,
	};

	private static final Map<Long, ItemOnObjectAction> actions = new ConcurrentHashMap<>();

	static {
		SpinningItemUses.register();
	}

	private ItemOnObjectRegistry() {
	}

	public static void register(int itemId, int objectId, ItemOnObjectAction action) {
		for (int cooking : COOKING_OBJECTS) {
			if (cooking == objectId) {
				throw new IllegalArgumentException("object " + objectId
						+ " is handled by the cooking switch in ItemOnObject.processPacket, which"
						+ " runs after UseItem and would fire as well; migrate that switch first");
			}
		}
		if (actions.putIfAbsent(key(itemId, objectId), action) != null) {
			throw new IllegalStateException("item " + itemId + " on object " + objectId
					+ " is registered twice");
		}
	}

	/**
	 * @return true if a handler claimed this pair, meaning the legacy switch in
	 *         {@code ItemonObject} must not also run for it.
	 */
	public static boolean dispatch(Client c, int itemId, int objectId, int objectX, int objectY) {
		ItemOnObjectAction action = actions.get(key(itemId, objectId));
		if (action == null) {
			return false;
		}
		action.handle(c, itemId, objectId, objectX, objectY);
		return true;
	}

	public static boolean isRegistered(int itemId, int objectId) {
		return actions.containsKey(key(itemId, objectId));
	}

	public static int size() {
		return actions.size();
	}

	private static long key(int itemId, int objectId) {
		return ((long) itemId << 32) | (objectId & 0xFFFFFFFFL);
	}
}
