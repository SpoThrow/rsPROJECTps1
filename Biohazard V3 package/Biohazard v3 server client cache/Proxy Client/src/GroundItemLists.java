import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.JOptionPane;

/**
 * RuneLite-style ground-item whitelist / blacklist.
 * Hold Alt: blacklisted names show grey with clickable + / - buttons beside the text.
 * Double-tap Alt: temporarily hide all ground-item names (plugin stays on). Single Alt restores.
 */
final class GroundItemLists {

	static final int GREY = 0x888888;
	private static final int DOUBLE_TAP_MS = 1000;

	/** Plugin stays on; overlay names hidden until a single Alt press. */
	static boolean namesTemporarilyHidden;

	private static long lastAltPressMs;
	private static long lastClickMs;
	/** True after a press that started a double-tap window (requires release before 2nd tap). */
	private static boolean altArmedForDoubleTap;

	private static final HashSet whitelist = new HashSet();
	private static final HashSet blacklist = new HashSet();

	/** Screen-space click targets filled while drawing: x,y,w,h,itemId,mode(0=-,1=+). */
	private static final ArrayList hits = new ArrayList();

	static void load(Properties props) {
		whitelist.clear();
		blacklist.clear();
		parseIds(props.getProperty("groundItemWhitelist", ""), whitelist);
		parseIds(props.getProperty("groundItemBlacklist", ""), blacklist);
		namesTemporarilyHidden = false;
	}

	static void save(Properties props) {
		props.setProperty("groundItemWhitelist", packIds(whitelist));
		props.setProperty("groundItemBlacklist", packIds(blacklist));
	}

	/** Called once per Alt key-down edge (not key-repeat). */
	static void onAltPressed() {
		long now = System.currentTimeMillis();
		if (namesTemporarilyHidden) {
			namesTemporarilyHidden = false;
			lastAltPressMs = 0L;
			altArmedForDoubleTap = false;
			return;
		}
		if (altArmedForDoubleTap && lastAltPressMs > 0L && now - lastAltPressMs <= DOUBLE_TAP_MS) {
			namesTemporarilyHidden = true;
			lastAltPressMs = 0L;
			altArmedForDoubleTap = false;
			return;
		}
		lastAltPressMs = now;
		altArmedForDoubleTap = true;
	}

	/** Called when Alt is released so the next press can complete a double-tap. */
	static void onAltReleased() {
		// Keep lastAltPressMs / altArmedForDoubleTap so a quick re-press still counts.
	}

	static boolean isWhitelisted(int itemId) {
		return whitelist.contains(Integer.valueOf(itemId & 0x7fff));
	}

	static boolean isBlacklisted(int itemId) {
		return blacklist.contains(Integer.valueOf(itemId & 0x7fff));
	}

	static int whitelistCount() {
		return whitelist.size();
	}

	static int blacklistCount() {
		return blacklist.size();
	}

	static void clearHits() {
		hits.clear();
	}

	static void addHit(int x, int y, int w, int h, int itemId, boolean plus) {
		hits.add(new int[] { x, y, w, h, itemId & 0x7fff, plus ? 1 : 0 });
	}

	/**
	 * Left-click on a + / - button while Alt is held. Caller must only invoke on a fresh click.
	 */
	static boolean processClick(client c, int mx, int my) {
		if (c == null || !RSApplet.altIsDown || !client.groundItemNames || namesTemporarilyHidden) {
			return false;
		}
		long now = System.currentTimeMillis();
		if (now - lastClickMs < 200L) {
			return false;
		}
		for (int i = hits.size() - 1; i >= 0; i--) {
			int[] h = (int[]) hits.get(i);
			if (mx >= h[0] && my >= h[1] && mx < h[0] + h[2] && my < h[1] + h[3]) {
				lastClickMs = now;
				if (h[5] == 1) {
					plus(c, h[4]);
				} else {
					minus(c, h[4]);
				}
				return true;
			}
		}
		return false;
	}

	static void plus(client c, int itemId) {
		itemId &= 0x7fff;
		Integer key = Integer.valueOf(itemId);
		if (blacklist.remove(key)) {
			message(c, itemId, "removed from blacklist");
		} else if (whitelist.contains(key)) {
			whitelist.remove(key);
			message(c, itemId, "removed from whitelist");
		} else {
			whitelist.add(key);
			message(c, itemId, "added to whitelist");
		}
		if (c != null) {
			c.saveClientSettings();
		}
	}

	static void minus(client c, int itemId) {
		itemId &= 0x7fff;
		Integer key = Integer.valueOf(itemId);
		whitelist.remove(key);
		if (blacklist.contains(key)) {
			message(c, itemId, "already blacklisted");
		} else {
			blacklist.add(key);
			message(c, itemId, "added to blacklist");
		}
		if (c != null) {
			c.saveClientSettings();
		}
	}

	static void openManageWhitelist() {
		openManage(true);
	}

	static void openManageBlacklist() {
		openManage(false);
	}

	private static void openManage(boolean white) {
		HashSet set = white ? whitelist : blacklist;
		String title = white ? "Ground item whitelist" : "Ground item blacklist";
		ArrayList entries = new ArrayList();
		Iterator it = set.iterator();
		while (it.hasNext()) {
			int id = ((Integer) it.next()).intValue();
			entries.add(labelFor(id) + "\t" + id);
		}
		Collections.sort(entries);
		if (entries.isEmpty()) {
			String add = JOptionPane.showInputDialog(null,
					title + " is empty.\nType an item name or ID to add:", title, JOptionPane.PLAIN_MESSAGE);
			tryAdd(set, add, white);
			return;
		}
		Object[] choices = new Object[entries.size()];
		for (int i = 0; i < entries.size(); i++) {
			String row = (String) entries.get(i);
			choices[i] = row.substring(0, row.lastIndexOf('\t'));
		}
		Object picked = JOptionPane.showInputDialog(null,
				"Select an entry to remove.\nCancel opens an add prompt.",
				title + " (" + entries.size() + ")", JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
		if (picked == null) {
			String add = JOptionPane.showInputDialog(null,
					"Type an item name or ID to add to the " + (white ? "whitelist" : "blacklist") + ":",
					title, JOptionPane.PLAIN_MESSAGE);
			tryAdd(set, add, white);
			return;
		}
		String want = String.valueOf(picked);
		for (int i = 0; i < entries.size(); i++) {
			String row = (String) entries.get(i);
			int tab = row.lastIndexOf('\t');
			if (want.equals(row.substring(0, tab))) {
				set.remove(Integer.valueOf(Integer.parseInt(row.substring(tab + 1))));
				if (client.instance != null) {
					client.instance.saveClientSettings();
					client.instance.pushMessage("Removed " + want + " from " + (white ? "whitelist" : "blacklist") + ".", 0, "");
				}
				break;
			}
		}
	}

	private static void tryAdd(HashSet set, String input, boolean white) {
		if (input == null) {
			return;
		}
		input = input.trim();
		if (input.length() == 0) {
			return;
		}
		int id = resolveItemId(input);
		if (id < 0) {
			JOptionPane.showMessageDialog(null, "Could not find item: " + input);
			return;
		}
		Integer key = Integer.valueOf(id);
		if (white) {
			blacklist.remove(key);
			whitelist.add(key);
		} else {
			whitelist.remove(key);
			blacklist.add(key);
		}
		if (client.instance != null) {
			client.instance.saveClientSettings();
			client.instance.pushMessage(labelFor(id) + " added to " + (white ? "whitelist" : "blacklist") + ".", 0, "");
		}
	}

	private static int resolveItemId(String input) {
		try {
			return Integer.parseInt(input) & 0x7fff;
		} catch (Exception e) {
		}
		String want = input.toLowerCase();
		for (int id = 0; id < 30000; id++) {
			ItemDef def = ItemDef.forID(id);
			if (def == null || def.name == null) {
				continue;
			}
			if (def.name.equalsIgnoreCase(input) || def.name.toLowerCase().indexOf(want) >= 0) {
				return id;
			}
		}
		return -1;
	}

	private static String labelFor(int id) {
		ItemDef def = ItemDef.forID(id);
		String name = def != null && def.name != null ? def.name : "Item";
		return name + " (" + id + ")";
	}

	private static void message(client c, int itemId, String what) {
		if (c == null) {
			return;
		}
		c.pushMessage(labelFor(itemId) + " " + what + ".", 0, "");
	}

	private static void parseIds(String packed, HashSet into) {
		if (packed == null || packed.length() == 0) {
			return;
		}
		String[] parts = packed.split(",");
		for (int i = 0; i < parts.length; i++) {
			try {
				into.add(Integer.valueOf(Integer.parseInt(parts[i].trim()) & 0x7fff));
			} catch (Exception e) {
			}
		}
	}

	private static String packIds(HashSet set) {
		StringBuffer sb = new StringBuffer();
		Iterator it = set.iterator();
		while (it.hasNext()) {
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(it.next());
		}
		return sb.toString();
	}

	private GroundItemLists() {
	}
}
