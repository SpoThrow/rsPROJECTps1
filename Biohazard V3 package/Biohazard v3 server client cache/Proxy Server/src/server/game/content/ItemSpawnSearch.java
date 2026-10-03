package server.game.content;

import server.game.items.ItemAssistant;
import server.game.players.Client;

/**
 * Admin item spawn: OSRS GE-style chatbox search on the client,
 * then amount prompt on select.
 */
public final class ItemSpawnSearch {

	public static final int INTERFACE = 51000;

	private ItemSpawnSearch() {
	}

	public static boolean canUse(Client c) {
		return c != null && c.playerRights >= 2;
	}

	public static void start(Client c) {
		start(c, null);
	}

	public static void start(Client c, String query) {
		if (!canUse(c)) {
			return;
		}
		c.itemSpawnPendingId = -1;
		c.itemSpawnSearching = false;
		c.posSearchingItem = false;
		c.posSearchingPlayer = false;
		openChatboxSearch(c, query);
	}

	public static void openChatboxSearch(Client c, String query) {
		if (!canUse(c) || c.getOutStream() == null) {
			return;
		}
		String q = query == null ? "" : query.trim();
		c.getOutStream().createFrameVarSize(186);
		c.getOutStream().writeString(q);
		c.getOutStream().endFrameVarSize();
		c.flushOutStream();
	}

	public static boolean handleSelect(Client c, String rawId) {
		if (!canUse(c) || rawId == null) {
			return false;
		}
		int itemId;
		try {
			itemId = Integer.parseInt(rawId.trim());
		} catch (NumberFormatException e) {
			return false;
		}
		if (itemId < 0 || itemId > 20000) {
			c.sendMessage("Could not complete spawn request.");
			return true;
		}
		selectItem(c, itemId);
		return true;
	}

	public static void selectItem(Client c, int itemId) {
		if (!canUse(c) || itemId < 0) {
			return;
		}
		c.itemSpawnPendingId = itemId;
		c.xInterfaceId = INTERFACE;
		String name = ItemAssistant.getItemName(itemId);
		c.sendMessage("Enter amount of " + (name == null ? ("item " + itemId) : name) + " to spawn:");
		if (c.getOutStream() != null) {
			c.getOutStream().createFrame(27);
			c.flushOutStream();
		}
	}

	public static boolean handleAmount(Client c, int amount) {
		if (c.itemSpawnPendingId < 0 || c.xInterfaceId != INTERFACE) {
			return false;
		}
		int itemId = c.itemSpawnPendingId;
		c.itemSpawnPendingId = -1;
		if (!canUse(c)) {
			return true;
		}
		if (amount < 1) {
			amount = 1;
		}
		if (itemId < 0 || itemId > 20000) {
			c.sendMessage("Could not complete spawn request.");
			return true;
		}
		c.getItems().addItem(itemId, amount);
		String name = ItemAssistant.getItemName(itemId);
		c.sendMessage("Spawned " + amount + " x " + (name == null ? itemId : name) + " (" + itemId + ").");
		System.out.println("Spawned: " + itemId + " x" + amount + " by: " + c.playerName);
		openChatboxSearch(c, name == null ? "" : name);
		return true;
	}

	/** Legacy button handler — interface no longer used for spawn. */
	public static boolean handleButton(Client c, int buttonId) {
		return false;
	}

	/** Legacy string search — filtering is client-side now. */
	public static boolean handleSearchString(Client c, String query) {
		if (!c.itemSpawnSearching) {
			return false;
		}
		c.itemSpawnSearching = false;
		return true;
	}
}
