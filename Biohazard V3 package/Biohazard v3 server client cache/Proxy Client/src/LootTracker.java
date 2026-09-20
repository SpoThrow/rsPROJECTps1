import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

/**
 * RuneLite-style loot tracker. Values use item definition prices (no GE).
 */
final class LootTracker {

	private static final Color DARKER = new Color(30, 30, 30);
	private static final Color DARK = new Color(40, 40, 40);
	private static final Color LIGHT = new Color(165, 165, 165);
	private static final Color TEXT = new Color(198, 198, 198);
	private static final Color ORANGE = new Color(220, 138, 0);
	private static final Font TITLE_FONT = new Font("SansSerif", Font.PLAIN, 16);
	private static final Font ROW_FONT = new Font("SansSerif", Font.PLAIN, 12);
	private static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 11);
	private static final int MAX_EVENTS = 400;
	private static final int COMBAT_TTL = 25;
	private static final int BATCH_TTL = 8;

	static boolean grouped = true;
	static boolean chatLoot = false;

	private static final ArrayList events = new ArrayList();
	private static final HashMap ignoredSources = new HashMap();
	private static final HashMap hiddenItems = new HashMap();
	private static final HashMap iconCache = new HashMap();

	private static String combatName;
	private static int combatX;
	private static int combatY;
	private static int combatCycle;

	private static String pendingSource;
	private static final ArrayList pendingStacks = new ArrayList();
	private static int pendingCycle;

	private static JPanel root;
	private static JPanel logs;
	private static JLabel overallKills;
	private static JLabel overallValue;
	private static JLabel emptyLabel;
	private static JToggleButton groupedBtn;
	private static boolean uiDirty;

	static JPanel buildPanel() {
		root = new JPanel(new BorderLayout());
		root.setBackground(DARK);
		root.setPreferredSize(new Dimension(PluginSidebar.PANEL_W, 200));
		root.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(23, 23, 23)));

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setBackground(DARK);
		top.setBorder(BorderFactory.createEmptyBorder(10, 10, 8, 10));
		JLabel title = new JLabel("Loot Tracker");
		title.setForeground(Color.WHITE);
		title.setFont(TITLE_FONT);
		title.setAlignmentX(0f);
		top.add(title);
		top.add(Box.createVerticalStrut(8));

		JPanel overall = new JPanel(new BorderLayout());
		overall.setBackground(DARKER);
		overall.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		overall.setAlignmentX(0f);
		overall.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
		JPanel stats = new JPanel();
		stats.setLayout(new BoxLayout(stats, BoxLayout.Y_AXIS));
		stats.setOpaque(false);
		overallKills = new JLabel("Total kills: 0");
		overallKills.setForeground(TEXT);
		overallKills.setFont(ROW_FONT);
		overallValue = new JLabel("Value: 0");
		overallValue.setForeground(ORANGE);
		overallValue.setFont(ROW_FONT);
		stats.add(overallKills);
		stats.add(overallValue);
		overall.add(stats, BorderLayout.CENTER);
		top.add(overall);
		top.add(Box.createVerticalStrut(6));

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		actions.setOpaque(false);
		actions.setAlignmentX(0f);
		groupedBtn = new JToggleButton(grouped ? "Grouped" : "Ungrouped");
		groupedBtn.setSelected(grouped);
		styleChip(groupedBtn);
		groupedBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				grouped = groupedBtn.isSelected();
				groupedBtn.setText(grouped ? "Grouped" : "Ungrouped");
				rebuildUi();
				saveNow();
			}
		});
		JButton reset = new JButton("Reset");
		styleChip(reset);
		reset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int ok = JOptionPane.showConfirmDialog(root, "Permanently delete all loot?", "Reset loot tracker",
						JOptionPane.YES_NO_OPTION);
				if (ok == JOptionPane.YES_OPTION) {
					events.clear();
					clearPending();
					rebuildUi();
					saveNow();
				}
			}
		});
		final JCheckBox chat = new JCheckBox("Chat", chatLoot);
		chat.setForeground(TEXT);
		chat.setBackground(DARK);
		chat.setFont(SMALL_FONT);
		chat.setOpaque(true);
		chat.setFocusPainted(false);
		chat.setToolTipText("Chat a value summary when loot is received");
		chat.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				chatLoot = chat.isSelected();
				saveNow();
			}
		});
		actions.add(groupedBtn);
		actions.add(reset);
		actions.add(chat);
		top.add(actions);

		logs = new JPanel();
		logs.setLayout(new BoxLayout(logs, BoxLayout.Y_AXIS));
		logs.setBackground(DARK);
		logs.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
		emptyLabel = new JLabel("<html>You have not received any loot yet.<br>Values use item prices (no GE).</html>");
		emptyLabel.setForeground(LIGHT);
		emptyLabel.setFont(SMALL_FONT);
		JPanel north = new JPanel(new BorderLayout());
		north.setBackground(DARK);
		north.add(logs, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(north);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(DARK);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);

		root.add(top, BorderLayout.NORTH);
		root.add(scroll, BorderLayout.CENTER);
		rebuildUi();
		return root;
	}

	private static void styleChip(javax.swing.AbstractButton b) {
		b.setFont(SMALL_FONT);
		b.setForeground(TEXT);
		b.setBackground(DARKER);
		b.setFocusPainted(false);
		b.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	static void tick() {
		if (client.instance == null || !client.instance.loggedIn || client.myPlayer == null) {
			return;
		}
		client.instance.captureLootCombat();
		if (pendingSource != null && client.loopCycle - pendingCycle > BATCH_TTL) {
			flushPending();
		}
		if (ensureIcons() || uiDirty) {
			uiDirty = false;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					rebuildUi();
				}
			});
		}
	}

	static void noteCombat(String name, int wx, int wy) {
		if (name == null || name.length() == 0) {
			return;
		}
		combatName = name;
		combatX = wx;
		combatY = wy;
		combatCycle = client.loopCycle;
	}

	static void onGroundItem(int id, int amount, int localX, int localY) {
		if (client.instance == null || !client.instance.loggedIn) {
			return;
		}
		id &= 0x7fff;
		if (id <= 0) {
			return;
		}
		if (amount < 1) {
			amount = 1;
		}
		if (hiddenItems.containsKey(Integer.valueOf(id))) {
			return;
		}
		int wx = localX + client.mapBaseX();
		int wy = localY + client.mapBaseY();
		String source = sourceFor(wx, wy);
		if (source == null) {
			return;
		}
		if (ignoredSources.containsKey(source.toLowerCase())) {
			return;
		}
		if (pendingSource != null && pendingSource.equals(source) && client.loopCycle - pendingCycle <= BATCH_TTL) {
			addPending(id, amount);
			pendingCycle = client.loopCycle;
			return;
		}
		flushPending();
		pendingSource = source;
		pendingCycle = client.loopCycle;
		addPending(id, amount);
	}

	static void save(Properties props) {
		props.setProperty("lootGrouped", Boolean.toString(grouped));
		props.setProperty("lootChat", Boolean.toString(chatLoot));
		props.setProperty("lootIgnored", joinKeys(ignoredSources));
		props.setProperty("lootHiddenItems", joinKeys(hiddenItems));
		StringBuffer sb = new StringBuffer();
		int count = Math.min(events.size(), 200);
		for (int i = events.size() - count; i < events.size(); i++) {
			LootEvent ev = (LootEvent) events.get(i);
			if (sb.length() > 0) {
				sb.append(';');
			}
			sb.append(escape(ev.source)).append('|').append(ev.kills);
			for (int s = 0; s < ev.stacks.size(); s++) {
				Stack st = (Stack) ev.stacks.get(s);
				sb.append('|').append(st.id).append('x').append(st.qty);
			}
		}
		props.setProperty("lootEvents", sb.toString());
	}

	static void load(Properties props) {
		grouped = boolProp(props, "lootGrouped", true);
		chatLoot = boolProp(props, "lootChat", false);
		parseIgnored(stringProp(props, "lootIgnored", ""));
		parseHidden(stringProp(props, "lootHiddenItems", ""));
		events.clear();
		String raw = stringProp(props, "lootEvents", "");
		if (raw.length() == 0) {
			return;
		}
		String[] parts = split(raw, ';');
		for (int i = 0; i < parts.length; i++) {
			if (parts[i].length() == 0) {
				continue;
			}
			String[] bits = split(parts[i], '|');
			if (bits.length < 2) {
				continue;
			}
			LootEvent ev = new LootEvent();
			ev.source = unescape(bits[0]);
			try {
				ev.kills = Integer.parseInt(bits[1]);
			} catch (Exception ex) {
				ev.kills = 1;
			}
			if (ev.kills < 1) {
				ev.kills = 1;
			}
			for (int b = 2; b < bits.length; b++) {
				int x = bits[b].indexOf('x');
				if (x <= 0) {
					continue;
				}
				try {
					addStack(ev.stacks, Integer.parseInt(bits[b].substring(0, x)),
							Integer.parseInt(bits[b].substring(x + 1)), null);
				} catch (Exception ex) {
				}
			}
			events.add(ev);
		}
	}

	private static String sourceFor(int wx, int wy) {
		if (combatName == null || client.loopCycle - combatCycle > COMBAT_TTL) {
			return null;
		}
		int dx = wx - combatX;
		int dy = wy - combatY;
		if (dx < 0) {
			dx = -dx;
		}
		if (dy < 0) {
			dy = -dy;
		}
		if (dx > 3 || dy > 3) {
			return null;
		}
		return combatName;
	}

	private static void addPending(int id, int amount) {
		for (int i = 0; i < pendingStacks.size(); i++) {
			Stack st = (Stack) pendingStacks.get(i);
			if (st.id == id) {
				st.qty += amount;
				st.icon = iconFor(st.id, st.qty);
				return;
			}
		}
		Stack st = new Stack();
		st.id = id;
		st.qty = amount;
		st.icon = iconFor(id, amount);
		pendingStacks.add(st);
	}

	private static void flushPending() {
		if (pendingSource == null || pendingStacks.isEmpty()) {
			clearPending();
			return;
		}
		LootEvent ev = new LootEvent();
		ev.source = pendingSource;
		ev.kills = 1;
		for (int i = 0; i < pendingStacks.size(); i++) {
			Stack st = (Stack) pendingStacks.get(i);
			Stack copy = new Stack();
			copy.id = st.id;
			copy.qty = st.qty;
			copy.icon = st.icon;
			ev.stacks.add(copy);
		}
		events.add(ev);
		while (events.size() > MAX_EVENTS) {
			events.remove(0);
		}
		int value = eventValue(ev);
		if (chatLoot && client.instance != null) {
			client.instance.pushMessage(ev.source + ": " + formatGp(value) + " (item value)", 0, "");
		}
		clearPending();
		uiDirty = true;
		saveNow();
	}

	private static void clearPending() {
		pendingSource = null;
		pendingStacks.clear();
		pendingCycle = 0;
	}

	private static boolean ensureIcons() {
		boolean changed = false;
		for (int i = 0; i < events.size(); i++) {
			LootEvent ev = (LootEvent) events.get(i);
			for (int s = 0; s < ev.stacks.size(); s++) {
				Stack st = (Stack) ev.stacks.get(s);
				if (st.icon == null) {
					st.icon = iconFor(st.id, st.qty);
					if (st.icon != null) {
						changed = true;
					}
				}
			}
		}
		return changed;
	}

	private static void rebuildUi() {
		if (logs == null) {
			return;
		}
		logs.removeAll();
		ArrayList boxes = grouped ? groupedEvents() : copyEvents();
		int kills = 0;
		long value = 0L;
		for (int i = 0; i < boxes.size(); i++) {
			LootEvent ev = (LootEvent) boxes.get(i);
			if (ignoredSources.containsKey(ev.source.toLowerCase())) {
				continue;
			}
			kills += ev.kills;
			value += eventValue(ev);
			logs.add(new LootBox(ev));
			logs.add(Box.createVerticalStrut(6));
		}
		if (logs.getComponentCount() == 0) {
			logs.add(emptyLabel);
		}
		if (overallKills != null) {
			overallKills.setText("Total kills: " + kills);
			overallValue.setText("Value: " + formatGp((int) Math.min(value, 2000000000L)));
		}
		if (groupedBtn != null) {
			groupedBtn.setSelected(grouped);
			groupedBtn.setText(grouped ? "Grouped" : "Ungrouped");
		}
		logs.revalidate();
		logs.repaint();
	}

	private static ArrayList copyEvents() {
		ArrayList out = new ArrayList();
		for (int i = events.size() - 1; i >= 0; i--) {
			out.add(events.get(i));
		}
		return out;
	}

	private static ArrayList groupedEvents() {
		ArrayList keys = new ArrayList();
		HashMap map = new HashMap();
		for (int i = 0; i < events.size(); i++) {
			LootEvent ev = (LootEvent) events.get(i);
			String key = ev.source.toLowerCase();
			LootEvent agg = (LootEvent) map.get(key);
			if (agg == null) {
				agg = new LootEvent();
				agg.source = ev.source;
				map.put(key, agg);
				keys.add(key);
			}
			agg.kills += ev.kills;
			for (int s = 0; s < ev.stacks.size(); s++) {
				Stack st = (Stack) ev.stacks.get(s);
				addStack(agg.stacks, st.id, st.qty, st.icon);
			}
		}
		ArrayList out = new ArrayList();
		for (int i = keys.size() - 1; i >= 0; i--) {
			out.add(map.get(keys.get(i)));
		}
		return out;
	}

	private static int eventValue(LootEvent ev) {
		long total = 0L;
		for (int i = 0; i < ev.stacks.size(); i++) {
			Stack st = (Stack) ev.stacks.get(i);
			if (hiddenItems.containsKey(Integer.valueOf(st.id))) {
				continue;
			}
			total += (long) itemValue(st.id) * (long) st.qty;
		}
		if (total > 2000000000L) {
			return 2000000000;
		}
		return (int) total;
	}

	static int itemValue(int id) {
		if (id == 995) {
			return 1;
		}
		try {
			ItemDef def = ItemDef.forID(id);
			if (def == null || def.value <= 0) {
				return 0;
			}
			return def.value;
		} catch (Exception e) {
			return 0;
		}
	}

	static String itemName(int id) {
		try {
			ItemDef def = ItemDef.forID(id);
			if (def != null && def.name != null && def.name.length() > 0 && !def.name.equalsIgnoreCase("null")) {
				return def.name;
			}
		} catch (Exception e) {
		}
		if (id == 995) {
			return "Coins";
		}
		return "Item";
	}

	static String formatGp(int value) {
		if (value >= 10000000) {
			return (value / 1000000) + "m";
		}
		if (value >= 100000) {
			return (value / 1000) + "k";
		}
		return String.valueOf(value);
	}

	private static Image iconFor(int id, int qty) {
		String key = id + ":" + qty;
		Image cached = (Image) iconCache.get(key);
		if (cached != null) {
			return cached;
		}
		try {
			Sprite sprite = ItemDef.getSprite(id, qty, 0);
			if (sprite == null || sprite.myPixels == null) {
				return null;
			}
			int w = sprite.myWidth;
			int h = sprite.myHeight;
			BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int p = sprite.myPixels[x + y * w];
					if (p != 0) {
						img.setRGB(x, y, 0xff000000 | p);
					}
				}
			}
			Image scaled = img.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
			iconCache.put(key, scaled);
			return scaled;
		} catch (Exception e) {
			return null;
		}
	}

	private static void ignoreSource(String source) {
		ignoredSources.put(source.toLowerCase(), Boolean.TRUE);
		rebuildUi();
		saveNow();
	}

	private static void hideItem(int id) {
		hiddenItems.put(Integer.valueOf(id), Boolean.TRUE);
		rebuildUi();
		saveNow();
	}

	private static void deleteSource(String source) {
		for (int i = events.size() - 1; i >= 0; i--) {
			LootEvent ev = (LootEvent) events.get(i);
			if (ev.source.equalsIgnoreCase(source)) {
				events.remove(i);
			}
		}
		rebuildUi();
		saveNow();
	}

	private static void saveNow() {
		if (client.instance != null) {
			client.instance.saveClientSettings();
		}
	}

	private static void addStack(ArrayList list, int id, int qty, Image icon) {
		for (int i = 0; i < list.size(); i++) {
			Stack st = (Stack) list.get(i);
			if (st.id == id) {
				st.qty += qty;
				if (icon != null) {
					st.icon = icon;
				}
				return;
			}
		}
		Stack st = new Stack();
		st.id = id;
		st.qty = qty;
		st.icon = icon;
		list.add(st);
	}

	private static String joinKeys(HashMap map) {
		StringBuffer sb = new StringBuffer();
		Object[] keys = map.keySet().toArray();
		for (int i = 0; i < keys.length; i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(String.valueOf(keys[i]));
		}
		return sb.toString();
	}

	private static void parseIgnored(String raw) {
		ignoredSources.clear();
		String[] parts = split(raw, ',');
		for (int i = 0; i < parts.length; i++) {
			if (parts[i].length() > 0) {
				ignoredSources.put(parts[i].toLowerCase(), Boolean.TRUE);
			}
		}
	}

	private static void parseHidden(String raw) {
		hiddenItems.clear();
		String[] parts = split(raw, ',');
		for (int i = 0; i < parts.length; i++) {
			if (parts[i].length() == 0) {
				continue;
			}
			try {
				hiddenItems.put(Integer.valueOf(Integer.parseInt(parts[i])), Boolean.TRUE);
			} catch (Exception e) {
			}
		}
	}

	private static boolean boolProp(Properties props, String key, boolean fallback) {
		String value = props.getProperty(key);
		if (value != null) {
			return Boolean.parseBoolean(value.trim());
		}
		return fallback;
	}

	private static String stringProp(Properties props, String key, String fallback) {
		String value = props.getProperty(key);
		return value == null ? fallback : value;
	}

	private static String[] split(String raw, char sep) {
		ArrayList list = new ArrayList();
		int start = 0;
		for (int i = 0; i < raw.length(); i++) {
			if (raw.charAt(i) == sep) {
				list.add(raw.substring(start, i));
				start = i + 1;
			}
		}
		list.add(raw.substring(start));
		String[] out = new String[list.size()];
		list.toArray(out);
		return out;
	}

	private static String escape(String s) {
		return s.replace("|", "/").replace(";", ",");
	}

	private static String unescape(String s) {
		return s;
	}

	private static final class Stack {
		int id;
		int qty;
		Image icon;
	}

	private static final class LootEvent {
		String source;
		int kills;
		final ArrayList stacks = new ArrayList();
	}

	private static final class LootBox extends JPanel {
		private static final long serialVersionUID = 1L;

		LootBox(final LootEvent ev) {
			setLayout(new BorderLayout(0, 4));
			setBackground(DARKER);
			setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
			setAlignmentX(0f);
			setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
			JPanel head = new JPanel(new BorderLayout());
			head.setOpaque(false);
			JLabel name = new JLabel(ev.source);
			name.setForeground(Color.WHITE);
			name.setFont(ROW_FONT);
			JLabel kills = new JLabel(ev.kills + (ev.kills == 1 ? " kill" : " kills"));
			kills.setForeground(LIGHT);
			kills.setFont(SMALL_FONT);
			head.add(name, BorderLayout.WEST);
			head.add(kills, BorderLayout.EAST);
			JLabel gp = new JLabel(formatGp(eventValue(ev)) + " gp");
			gp.setForeground(ORANGE);
			gp.setFont(SMALL_FONT);
			JPanel icons = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
			icons.setOpaque(false);
			for (int i = 0; i < ev.stacks.size(); i++) {
				final Stack st = (Stack) ev.stacks.get(i);
				if (hiddenItems.containsKey(Integer.valueOf(st.id))) {
					continue;
				}
				JLabel icon = new JLabel();
				if (st.icon != null) {
					icon.setIcon(new ImageIcon(st.icon));
				} else {
					icon.setText(itemName(st.id));
					icon.setForeground(TEXT);
					icon.setFont(SMALL_FONT);
				}
				String tip = itemName(st.id);
				if (st.qty > 1) {
					tip = tip + " x" + st.qty;
				}
				tip = tip + " (" + formatGp(itemValue(st.id) * st.qty) + " gp)";
				icon.setToolTipText(tip);
				icon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				icon.addMouseListener(new MouseAdapter() {
					public void mouseReleased(MouseEvent e) {
						if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
							JPopupMenu menu = new JPopupMenu();
							JMenuItem hide = new JMenuItem("Hide " + itemName(st.id));
							hide.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									hideItem(st.id);
								}
							});
							menu.add(hide);
							menu.show(e.getComponent(), e.getX(), e.getY());
						}
					}
				});
				icons.add(icon);
			}
			add(head, BorderLayout.NORTH);
			add(gp, BorderLayout.CENTER);
			add(icons, BorderLayout.SOUTH);
			addMouseListener(new MouseAdapter() {
				public void mouseReleased(MouseEvent e) {
					if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
						JPopupMenu menu = new JPopupMenu();
						JMenuItem ignore = new JMenuItem("Ignore " + ev.source);
						ignore.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								ignoreSource(ev.source);
							}
						});
						JMenuItem reset = new JMenuItem("Reset " + ev.source);
						reset.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								deleteSource(ev.source);
							}
						});
						menu.add(ignore);
						menu.add(reset);
						menu.show(e.getComponent(), e.getX(), e.getY());
					}
				}
			});
		}
	}
}
