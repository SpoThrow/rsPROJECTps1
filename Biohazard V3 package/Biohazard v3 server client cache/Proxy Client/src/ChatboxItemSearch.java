/**
 * OSRS GE-style item search drawn inside the chatbox.
 * Type to filter live; click an icon to select, then enter amount.
 */
public final class ChatboxItemSearch {

	public static final int MAX_RESULTS = 200;
	public static final int COLS = 11;
	public static final int ROWS_VISIBLE = 3;
	public static final int CELL_W = 42;
	public static final int CELL_H = 32;
	public static final int GRID_X = 8;
	public static final int GRID_Y = 36;
	public static final int SCROLL_X = 496;
	public static final int SCROLL_H = CELL_H * ROWS_VISIBLE;
	/** Keep in 1000–1999 so left-click wins the menu sort and doAction does not subtract 2000. */
	public static final int ACTION_SELECT = 1640;

	public static boolean open;
	public static String query = "";
	public static final int[] results = new int[MAX_RESULTS];
	public static int resultCount;
	public static int hoverIndex = -1;
	/** First visible row (0-based). */
	public static int scrollRow;
	private static boolean draggingScroll;

	private ChatboxItemSearch() {
	}

	public static void openSearch() {
		openSearch("");
	}

	public static void openSearch(String initial) {
		open = true;
		query = initial == null ? "" : initial;
		hoverIndex = -1;
		scrollRow = 0;
		draggingScroll = false;
		refresh();
		client.inputTaken = true;
	}

	public static void close() {
		if (!open) {
			return;
		}
		open = false;
		query = "";
		resultCount = 0;
		hoverIndex = -1;
		scrollRow = 0;
		draggingScroll = false;
		client.inputTaken = true;
	}

	public static void refresh() {
		resultCount = 0;
		scrollRow = 0;
		draggingScroll = false;
		String needle = query.toLowerCase().trim();
		if (needle.length() == 0) {
			return;
		}
		try {
			int asId = Integer.parseInt(needle);
			ItemDef def = ItemDef.forID(asId);
			if (isSpawnable(def)) {
				results[resultCount++] = asId;
			}
		} catch (NumberFormatException ignored) {
		}
		int limit = ItemDef.totalItems;
		if (limit > 25000) {
			limit = 25000;
		}
		for (int id = 0; id < limit && resultCount < MAX_RESULTS; id++) {
			ItemDef def = ItemDef.forID(id);
			if (!isSpawnable(def)) {
				continue;
			}
			if (!def.name.toLowerCase().contains(needle)) {
				continue;
			}
			boolean dupe = false;
			for (int j = 0; j < resultCount; j++) {
				if (results[j] == id) {
					dupe = true;
					break;
				}
			}
			if (!dupe) {
				results[resultCount++] = id;
			}
		}
		clampScroll();
	}

	private static boolean isSpawnable(ItemDef def) {
		return def != null && def.name != null && def.name.length() > 0
				&& !def.name.equalsIgnoreCase("null");
	}

	private static int totalRows() {
		if (resultCount <= 0) {
			return 0;
		}
		return (resultCount + COLS - 1) / COLS;
	}

	private static int maxScrollRow() {
		int extra = totalRows() - ROWS_VISIBLE;
		return extra < 0 ? 0 : extra;
	}

	private static boolean needsScrollbar() {
		return totalRows() > ROWS_VISIBLE;
	}

	private static void clampScroll() {
		int max = maxScrollRow();
		if (scrollRow < 0) {
			scrollRow = 0;
		}
		if (scrollRow > max) {
			scrollRow = max;
		}
	}

	public static boolean typeKey(int j) {
		if (!open) {
			return false;
		}
		if (j == 27) {
			close();
			return true;
		}
		if (j == 8 && query.length() > 0) {
			query = query.substring(0, query.length() - 1);
			refresh();
			client.inputTaken = true;
			return true;
		}
		if (j >= 32 && j <= 122 && query.length() < 28) {
			query += (char) j;
			refresh();
			client.inputTaken = true;
			return true;
		}
		return true;
	}

	/** Mouse-wheel scroll while the search is open. */
	public static boolean mouseWheel(int rotation) {
		if (!open || !needsScrollbar()) {
			return false;
		}
		scrollRow += rotation;
		clampScroll();
		client.inputTaken = true;
		return true;
	}

	/**
	 * Drag / click the scrollbar while LMB is held.
	 * Call each frame from the client loop.
	 */
	public static void process(client c) {
		if (!open) {
			draggingScroll = false;
			return;
		}
		if (!needsScrollbar() || c.clickMode2 != 1) {
			draggingScroll = false;
			return;
		}
		int mx = c.mouseX;
		int my = c.mouseY - c.chatDrawY();
		boolean overBar = mx >= SCROLL_X && mx < SCROLL_X + 16
				&& my >= GRID_Y && my < GRID_Y + SCROLL_H;
		if (!overBar && !draggingScroll) {
			return;
		}
		int contentH = totalRows() * CELL_H;
		int viewH = SCROLL_H;
		int thumbH = ((viewH - 32) * viewH) / contentH;
		if (thumbH < 8) {
			thumbH = 8;
		}
		int track = viewH - 32 - thumbH;
		if (track < 1) {
			track = 1;
		}
		if (my < GRID_Y + 16) {
			scrollRow--;
		} else if (my >= GRID_Y + viewH - 16) {
			scrollRow++;
		} else {
			draggingScroll = true;
			int rel = my - GRID_Y - 16 - thumbH / 2;
			if (rel < 0) {
				rel = 0;
			}
			if (rel > track) {
				rel = track;
			}
			scrollRow = (maxScrollRow() * rel) / track;
		}
		clampScroll();
		client.inputTaken = true;
	}

	private static int indexAt(int mouseX, int localY) {
		if (resultCount == 0) {
			return -1;
		}
		int gridBottom = GRID_Y + SCROLL_H;
		if (mouseX < GRID_X || mouseX >= GRID_X + COLS * CELL_W
				|| localY < GRID_Y || localY >= gridBottom) {
			return -1;
		}
		int col = (mouseX - GRID_X) / CELL_W;
		int row = (localY - GRID_Y) / CELL_H;
		if (col < 0 || col >= COLS || row < 0 || row >= ROWS_VISIBLE) {
			return -1;
		}
		int idx = (scrollRow + row) * COLS + col;
		if (idx < 0 || idx >= resultCount) {
			return -1;
		}
		return idx;
	}

	public static void draw(client c) {
		if (!open) {
			return;
		}
		TextDrawingArea small = c.aTextDrawingArea_1271;
		c.newBoldFont.drawCenteredString("What would you like to spawn?", 259, 14, 0, -1);
		c.newBoldFont.drawCenteredString(query + "*", 259, 30, 0x800000, -1);
		if (query.length() == 0) {
			small.method389(true, 12, 0xA0A0A0, "Start typing an item name...", 52);
			small.method389(true, 12, 0xA0A0A0, "Press Esc to cancel.", 66);
			return;
		}
		if (resultCount == 0) {
			small.method389(true, 12, 0x800000, "No items match \"" + query + "\".", 52);
			return;
		}

		DrawingArea.setDrawingArea(GRID_Y + SCROLL_H, GRID_X, GRID_X + COLS * CELL_W, GRID_Y);

		hoverIndex = -1;
		int mouseX = c.mouseX;
		int mouseY = c.mouseY - c.chatDrawY();
		int first = scrollRow * COLS;
		int last = first + COLS * ROWS_VISIBLE;
		if (last > resultCount) {
			last = resultCount;
		}
		for (int i = first; i < last; i++) {
			int local = i - first;
			int col = local % COLS;
			int row = local / COLS;
			int x = GRID_X + col * CELL_W;
			int y = GRID_Y + row * CELL_H;
			int itemId = results[i];
			boolean hover = mouseX >= x && mouseX < x + CELL_W
					&& mouseY >= y && mouseY < y + CELL_H;
			if (hover) {
				hoverIndex = i;
				DrawingArea.method335(0xD4A017, y + 1, CELL_W - 4, CELL_H - 4, 45, x + 1);
			}
			Sprite icon = ItemDef.getSprite(itemId, 1, 0);
			if (icon != null) {
				icon.drawSprite(x + (CELL_W - 32) / 2, y);
			}
		}
		DrawingArea.defaultDrawingAreaSize();

		if (needsScrollbar()) {
			int contentH = totalRows() * CELL_H;
			int scrollPos = scrollRow * CELL_H;
			c.drawScrollbar(SCROLL_H, scrollPos, GRID_Y, SCROLL_X, contentH);
		}

		String footer;
		if (hoverIndex >= 0 && hoverIndex < resultCount) {
			ItemDef def = ItemDef.forID(results[hoverIndex]);
			String name = def != null && def.name != null ? def.name : ("Item " + results[hoverIndex]);
			footer = name + " (" + results[hoverIndex] + ")";
		} else {
			footer = resultCount + (resultCount >= MAX_RESULTS ? "+" : "") + " result"
					+ (resultCount == 1 ? "" : "s") + " — click an item"
					+ (needsScrollbar() ? "  |  scroll for more" : "");
		}
		if (footer.length() > 58) {
			footer = footer.substring(0, 55) + "...";
		}
		small.method389(true, 12, hoverIndex >= 0 ? 0xFFFFFF : 0x808080, footer, 138);
	}

	public static boolean buildMenu(client c, int mouseX, int mouseY) {
		if (!open || resultCount == 0) {
			return false;
		}
		int localY = mouseY - c.chatDrawY();
		int found = indexAt(mouseX, localY);
		if (found >= 0) {
			ItemDef def = ItemDef.forID(results[found]);
			String name = def != null && def.name != null ? def.name : ("Item " + results[found]);
			c.menuActionName[c.menuActionRow] = "Select @lre@" + name;
			c.menuActionID[c.menuActionRow] = ACTION_SELECT;
			c.menuActionCmd1[c.menuActionRow] = results[found];
			c.menuActionRow++;
		}
		if (found != hoverIndex) {
			hoverIndex = found;
			client.inputTaken = true;
		}
		return found >= 0;
	}

	public static boolean doAction(client c, int action, int itemId) {
		if (action != ACTION_SELECT || !open) {
			return false;
		}
		close();
		c.sendString(6, String.valueOf(itemId));
		return true;
	}
}
