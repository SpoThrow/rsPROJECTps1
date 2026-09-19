package server.game.items;

import server.Config;
import server.game.players.Client;

public class BankTabs {

	public static final int TAB_COUNT = 9;
	public static final int COLUMNS = 10;
	public static final int DISPLAY_SIZE = 500;
	public static final int MAIN_BUTTON = 10324;
	public static final int TAB_ITEM_START = 10335;
	public static final int USED_COUNT_ID = 19995;
	public static final int MAX_COUNT_ID = 19996;
	public static final int SEARCH_CONFIG = 116;
	public static final int VIEW_TAB_CONFIG = 160;
	public static final int TAB_AMOUNT_CONFIG = 540;

	private final Client c;
	private int[] displayMap = new int[DISPLAY_SIZE];

	public BankTabs(Client client) {
		this.c = client;
	}

	public void ensureInitialized() {
		compactOccupied();
		int items = itemCount();
		int sum = tabSum();
		if (sum != items) {
			for (int i = 0; i < TAB_COUNT; i++) {
				c.tabAmounts[i] = 0;
			}
			c.tabAmounts[0] = items;
		}
		if (c.bankingTab < 0 || c.bankingTab >= TAB_COUNT) {
			c.bankingTab = 0;
		}
		if (c.bankingTab > 0 && c.tabAmounts[c.bankingTab] <= 0) {
			c.bankingTab = 0;
		}
	}

	public void compactOccupied() {
		int write = 0;
		for (int read = 0; read < Config.BANK_SIZE; read++) {
			if (c.bankItems[read] > 0 && c.bankItemsN[read] > 0) {
				if (write != read) {
					c.bankItems[write] = c.bankItems[read];
					c.bankItemsN[write] = c.bankItemsN[read];
				}
				write++;
			}
		}
		for (int i = write; i < Config.BANK_SIZE; i++) {
			c.bankItems[i] = 0;
			c.bankItemsN[i] = 0;
		}
	}

	public int itemCount() {
		int count = 0;
		for (int i = 0; i < Config.BANK_SIZE; i++) {
			if (c.bankItems[i] > 0 && c.bankItemsN[i] > 0) {
				count++;
			}
		}
		return count;
	}

	public int tabSum() {
		int sum = 0;
		for (int i = 0; i < TAB_COUNT; i++) {
			if (c.tabAmounts[i] > 0) {
				sum += c.tabAmounts[i];
			} else {
				c.tabAmounts[i] = 0;
			}
		}
		return sum;
	}

	public int tabStart(int tab) {
		int start = 0;
		for (int i = 0; i < tab; i++) {
			start += c.tabAmounts[i];
		}
		return start;
	}

	public int tabForSlot(int absSlot) {
		int end = 0;
		for (int i = 0; i < TAB_COUNT; i++) {
			end += c.tabAmounts[i];
			if (absSlot < end) {
				return i;
			}
		}
		return 0;
	}

	public int toAbsolute(int displaySlot) {
		if (displaySlot < 0 || displaySlot >= DISPLAY_SIZE) {
			return -1;
		}
		rebuildDisplay();
		int abs = displayMap[displaySlot];
		if (abs < 0 || abs >= Config.BANK_SIZE) {
			return -1;
		}
		return abs;
	}

	public int[] getDisplayMap() {
		rebuildDisplay();
		return displayMap;
	}

	public void rebuildDisplay() {
		for (int i = 0; i < DISPLAY_SIZE; i++) {
			displayMap[i] = -1;
		}
		if (c.bankSearching && c.bankSearch != null && c.bankSearch.length() > 0) {
			String term = c.bankSearch.toLowerCase();
			int d = 0;
			for (int i = 0; i < Config.BANK_SIZE && d < DISPLAY_SIZE; i++) {
				if (c.bankItems[i] <= 0 || c.bankItemsN[i] <= 0) {
					continue;
				}
				String name = ItemAssistant.getItemName(c.bankItems[i] - 1);
				if (name != null && name.toLowerCase().indexOf(term) != -1) {
					displayMap[d++] = i;
				}
			}
			return;
		}
		if (c.bankingTab > 0) {
			int start = tabStart(c.bankingTab);
			int n = c.tabAmounts[c.bankingTab];
			for (int i = 0; i < n && i < DISPLAY_SIZE; i++) {
				displayMap[i] = start + i;
			}
			return;
		}
		int d = 0;
		boolean started = false;
		for (int tab = 0; tab < TAB_COUNT; tab++) {
			int n = c.tabAmounts[tab];
			if (n <= 0) {
				continue;
			}
			if (started) {
				if (d % COLUMNS != 0) {
					d += COLUMNS - (d % COLUMNS);
				}
				d += COLUMNS;
			}
			started = true;
			int start = tabStart(tab);
			for (int i = 0; i < n && d < DISPLAY_SIZE; i++) {
				displayMap[d++] = start + i;
			}
		}
	}

	public void promptSearch() {
		if (c.bankSearching || c.awaitingBankSearch) {
			clearSearch();
			c.sendMessage("Bank search cleared.");
			return;
		}
		c.awaitingBankSearch = true;
		c.getPA().sendFrame36(SEARCH_CONFIG, 1);
		if (c.getOutStream() != null) {
			c.getOutStream().createFrame(187);
			c.flushOutStream();
		}
		c.sendMessage("Enter an item name to search. Click Search again to cancel.");
	}

	public void applySearch(String term) {
		c.awaitingBankSearch = false;
		if (term == null) {
			term = "";
		}
		term = term.trim();
		if (term.length() == 0) {
			clearSearch();
			return;
		}
		c.bankSearching = true;
		c.bankSearch = term;
		c.bankingTab = 0;
		refresh();
		c.sendMessage("Showing bank items matching '" + term + "'.");
	}

	public void clearSearch() {
		c.bankSearching = false;
		c.awaitingBankSearch = false;
		c.bankSearch = "";
		refresh();
	}

	public int firstEmptyTab() {
		for (int i = 1; i < TAB_COUNT; i++) {
			if (c.tabAmounts[i] <= 0) {
				return i;
			}
		}
		return -1;
	}

	public int findExistingSlot(int bankId) {
		for (int i = 0; i < Config.BANK_SIZE; i++) {
			if (c.bankItems[i] == bankId && c.bankItemsN[i] > 0) {
				return i;
			}
		}
		return -1;
	}

	public int findInsertSlot() {
		ensureInitialized();
		int tab = c.bankingTab < 0 ? 0 : c.bankingTab;
		return tabStart(tab) + c.tabAmounts[tab];
	}

	public int insertNewItem(int bankId, int amount) {
		if (bankId <= 0 || amount < 0) {
			return -1;
		}
		if (itemCount() >= Config.BANK_SIZE) {
			return -1;
		}
		ensureInitialized();
		int slot = findInsertSlot();
		if (slot < 0 || slot >= Config.BANK_SIZE) {
			return -1;
		}
		shiftRight(slot);
		c.bankItems[slot] = bankId;
		c.bankItemsN[slot] = amount;
		int tab = c.bankingTab < 0 ? 0 : c.bankingTab;
		c.tabAmounts[tab]++;
		return slot;
	}

	public void onEmptiedSlot(int absSlot) {
		if (absSlot < 0 || absSlot >= Config.BANK_SIZE) {
			return;
		}
		if (c.bankItems[absSlot] > 0 && c.bankItemsN[absSlot] > 0) {
			return;
		}
		int tab = tabForSlot(absSlot);
		shiftLeft(absSlot);
		if (c.tabAmounts[tab] > 0) {
			c.tabAmounts[tab]--;
		}
		if (tab > 0 && c.tabAmounts[tab] <= 0) {
			collapseTab(tab);
		}
	}

	public void openTab(int tab) {
		if (tab < 0 || tab >= TAB_COUNT) {
			return;
		}
		ensureInitialized();
		c.bankSearching = false;
		c.awaitingBankSearch = false;
		c.bankSearch = "";
		if (tab > 0 && c.tabAmounts[tab] <= 0) {
			c.sendMessage("Drag an item here to create a new tab.");
			return;
		}
		c.bankingTab = tab;
		refresh();
	}

	public void moveToTab(int absSlot, int destTab) {
		ensureInitialized();
		if (absSlot < 0 || absSlot >= Config.BANK_SIZE || c.bankItems[absSlot] <= 0 || c.bankItemsN[absSlot] <= 0) {
			return;
		}
		if (destTab < 0 || destTab >= TAB_COUNT) {
			return;
		}
		if (c.tabAmounts[destTab] <= 0) {
			int empty = firstEmptyTab();
			if (empty == -1) {
				c.sendMessage("You already have the maximum number of bank tabs.");
				return;
			}
			destTab = empty;
		}
		int srcTab = tabForSlot(absSlot);
		if (srcTab == destTab) {
			return;
		}
		int id = c.bankItems[absSlot];
		int amt = c.bankItemsN[absSlot];
		shiftLeft(absSlot);
		if (c.tabAmounts[srcTab] > 0) {
			c.tabAmounts[srcTab]--;
		}
		if (srcTab > 0 && c.tabAmounts[srcTab] <= 0) {
			if (destTab > srcTab) {
				destTab--;
			}
			collapseTab(srcTab);
		}
		int destSlot = tabStart(destTab) + c.tabAmounts[destTab];
		if (destSlot < 0 || destSlot >= Config.BANK_SIZE) {
			int recover = insertAtEnd(id, amt);
			if (recover >= 0) {
				c.tabAmounts[0]++;
			}
			refresh();
			return;
		}
		shiftRight(destSlot);
		c.bankItems[destSlot] = id;
		c.bankItemsN[destSlot] = amt;
		c.tabAmounts[destTab]++;
		c.bankingTab = destTab;
		refresh();
	}

	public void refresh() {
		ensureInitialized();
		rebuildDisplay();
		c.getItems().resetBank();
		sendTabIcons();
		c.getPA().sendFrame126(String.valueOf(itemCount()), USED_COUNT_ID);
		c.getPA().sendFrame126(String.valueOf(Config.BANK_SIZE), MAX_COUNT_ID);
		c.getPA().sendFrame36(304, c.insertMode ? 1 : 0);
		c.getPA().sendFrame36(115, c.takeAsNote ? 1 : 0);
		c.getPA().sendFrame36(SEARCH_CONFIG, (c.bankSearching || c.awaitingBankSearch) ? 1 : 0);
		c.getPA().sendFrame36(VIEW_TAB_CONFIG, c.bankingTab);
	}

	public void sendTabIcons() {
		ensureInitialized();
		for (int tab = 1; tab < TAB_COUNT; tab++) {
			int frame = TAB_ITEM_START + tab - 1;
			if (c.tabAmounts[tab] <= 0) {
				c.getPA().sendFrame34a(frame, -1, 0, 0);
				continue;
			}
			int slot = tabStart(tab);
			c.getPA().sendFrame34a(frame, c.bankItems[slot] - 1, 0, 1);
		}
	}

	public void swapOrInsert(int fromDisplay, int toDisplay, boolean insert) {
		ensureInitialized();
		int from = toAbsolute(fromDisplay);
		int to = toAbsolute(toDisplay);
		if (from < 0 || to < 0 || from >= Config.BANK_SIZE || to >= Config.BANK_SIZE) {
			return;
		}
		if (c.bankItems[from] <= 0 || c.bankItemsN[from] <= 0) {
			return;
		}
		if (c.bankingTab > 0) {
			int start = tabStart(c.bankingTab);
			int end = start + c.tabAmounts[c.bankingTab] - 1;
			if (from < start || from > end || to < start || to > end) {
				return;
			}
		} else if (insert && tabForSlot(from) != tabForSlot(to)) {
			return;
		}
		if (insert) {
			int id = c.bankItems[from];
			int amt = c.bankItemsN[from];
			if (from < to) {
				for (int i = from; i < to; i++) {
					c.bankItems[i] = c.bankItems[i + 1];
					c.bankItemsN[i] = c.bankItemsN[i + 1];
				}
			} else {
				for (int i = from; i > to; i--) {
					c.bankItems[i] = c.bankItems[i - 1];
					c.bankItemsN[i] = c.bankItemsN[i - 1];
				}
			}
			c.bankItems[to] = id;
			c.bankItemsN[to] = amt;
		} else {
			int id = c.bankItems[from];
			int amt = c.bankItemsN[from];
			c.bankItems[from] = c.bankItems[to];
			c.bankItemsN[from] = c.bankItemsN[to];
			c.bankItems[to] = id;
			c.bankItemsN[to] = amt;
		}
		refresh();
	}

	private int insertAtEnd(int id, int amt) {
		for (int i = 0; i < Config.BANK_SIZE; i++) {
			if (c.bankItems[i] <= 0) {
				c.bankItems[i] = id;
				c.bankItemsN[i] = amt;
				return i;
			}
		}
		return -1;
	}

	private void collapseTab(int tab) {
		for (int i = tab; i < TAB_COUNT - 1; i++) {
			c.tabAmounts[i] = c.tabAmounts[i + 1];
		}
		c.tabAmounts[TAB_COUNT - 1] = 0;
		if (c.bankingTab == tab) {
			c.bankingTab = 0;
		} else if (c.bankingTab > tab) {
			c.bankingTab--;
		}
	}

	private void shiftRight(int slot) {
		for (int i = Config.BANK_SIZE - 1; i > slot; i--) {
			c.bankItems[i] = c.bankItems[i - 1];
			c.bankItemsN[i] = c.bankItemsN[i - 1];
		}
		c.bankItems[slot] = 0;
		c.bankItemsN[slot] = 0;
	}

	private void shiftLeft(int slot) {
		for (int i = slot; i < Config.BANK_SIZE - 1; i++) {
			c.bankItems[i] = c.bankItems[i + 1];
			c.bankItemsN[i] = c.bankItemsN[i + 1];
		}
		c.bankItems[Config.BANK_SIZE - 1] = 0;
		c.bankItemsN[Config.BANK_SIZE - 1] = 0;
	}
}
