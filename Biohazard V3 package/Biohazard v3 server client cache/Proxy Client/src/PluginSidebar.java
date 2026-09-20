import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * RuneLite Configuration sidebar. Same plugin IDs as Client Settings.
 */
final class PluginSidebar {

	static final int ICON_W = 36;
	static final int PANEL_INNER = 225;
	static final int SCROLL_W = 17;
	static final int PANEL_W = PANEL_INNER + SCROLL_W;
	static boolean open;
	static boolean sidebarOut = true;
	static final int TAB_CONFIG = 0;
	static final int TAB_LOOT = 1;
	static int selectedTab = TAB_CONFIG;

	private static final Color DARKER = new Color(30, 30, 30);
	private static final Color DARK = new Color(40, 40, 40);
	private static final Color MEDIUM = new Color(77, 77, 77);
	private static final Color LIGHT = new Color(165, 165, 165);
	private static final Color TEXT = new Color(198, 198, 198);
	private static final Color ORANGE = new Color(220, 138, 0);
	private static final Color BORDER = new Color(23, 23, 23);
	private static final Color HOVER = new Color(60, 60, 60);
	private static final Color SCROLL_TRACK = new Color(25, 25, 25);
	private static final Color SELECTED_TAB = new Color(50, 42, 28);
	private static final Font TITLE_FONT = new Font("SansSerif", Font.PLAIN, 16);
	private static final Font ROW_FONT = new Font("SansSerif", Font.PLAIN, 12);
	private static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 12);

	private static JPanel east;
	private static JPanel pluginHost;
	private static CardLayout pluginCards;
	private static JPanel configPanel;
	private static CardLayout cards;
	private static JPanel listPanel;
	private static JPanel configItems;
	private static JTextField search;
	private static JLabel clearSearch;
	private static JPanel wrench;
	private static JPanel lootTab;
	private static JLabel configTitle;
	private static ToggleSwitch configToggle;
	private static PluginDef openPlugin;
	private static final ArrayList rows = new ArrayList();
	private static final ArrayList configRows = new ArrayList();
	private static boolean syncing;

	static void install(JFrame frame) {
		east = new JPanel(new BorderLayout());
		east.setBackground(DARKER);
		cards = new CardLayout();
		configPanel = new JPanel(cards);
		configPanel.setBackground(DARK);
		configPanel.add(buildListCard(), "list");
		configPanel.add(buildConfigCard(), "config");
		pluginCards = new CardLayout();
		pluginHost = new JPanel(pluginCards);
		pluginHost.setBackground(DARK);
		pluginHost.add(configPanel, "config");
		pluginHost.add(LootTracker.buildPanel(), "loot");
		pluginHost.setVisible(open);
		east.add(pluginHost, BorderLayout.CENTER);
		east.add(buildIconBar(), BorderLayout.EAST);
		applyEastSize();
		frame.getContentPane().add(east, BorderLayout.EAST);
		rebuildList();
		cards.show(configPanel, "list");
		showPluginCard();
	}

	static int eastWidth() {
		if (east == null || !sidebarOut) {
			return 0;
		}
		int w = ICON_W;
		if (open) {
			w += PANEL_W;
		}
		return w;
	}

	static void toggle() {
		selectTab(TAB_CONFIG);
	}

	static void selectTab(int tab) {
		if (tab != TAB_LOOT) {
			tab = TAB_CONFIG;
		}
		if (sidebarOut && open && selectedTab == tab) {
			setOpen(false);
			return;
		}
		boolean alreadyOpen = sidebarOut && open;
		selectedTab = tab;
		showPluginCard();
		if (alreadyOpen) {
			syncChrome();
			if (client.instance != null) {
				client.instance.saveClientSettings();
			}
		} else {
			setOpen(true);
		}
	}

	static String windowTitle() {
		if (!sidebarOut || !open) {
			return "Biohazard";
		}
		if (selectedTab == TAB_LOOT) {
			return "Loot Tracker";
		}
		return "Configuration";
	}

	static void toggleSidebar() {
		setSidebarOut(!sidebarOut);
	}

	static void setSidebarOut(boolean value) {
		sidebarOut = value;
		if (sidebarOut && !open) {
			open = true;
		}
		if (!sidebarOut) {
			showList();
		}
		applyEastSize();
		syncChrome();
		applyFrameSize();
	}

	static void setOpen(boolean value) {
		open = value;
		if (open) {
			sidebarOut = true;
		}
		if (!open) {
			showList();
		}
		applyEastSize();
		syncChrome();
		applyFrameSize();
	}

	private static void syncChrome() {
		if (wrench != null) {
			wrench.setBackground(tabOn(TAB_CONFIG) ? SELECTED_TAB : DARKER);
			wrench.repaint();
		}
		if (lootTab != null) {
			lootTab.setBackground(tabOn(TAB_LOOT) ? SELECTED_TAB : DARKER);
			lootTab.repaint();
		}
		TitleBar.sync();
	}

	static boolean tabOn(int tab) {
		return open && selectedTab == tab;
	}

	private static void showPluginCard() {
		if (pluginCards != null && pluginHost != null) {
			pluginCards.show(pluginHost, selectedTab == TAB_LOOT ? "loot" : "config");
		}
	}

	private static void applyFrameSize() {
		if (Jframe.getFrame() != null && client.instance != null) {
			if (client.frameMode == client.ScreenMode.FIXED) {
				Jframe.setCanvasSize(765, 503, false);
			} else {
				Jframe.setCanvasSize(Math.max(765, client.frameWidth), Math.max(503, client.frameHeight), true);
			}
			client.instance.saveClientSettings();
		}
	}

	private static void applyEastSize() {
		if (east == null) {
			return;
		}
		east.setVisible(sidebarOut);
		if (pluginHost != null) {
			pluginHost.setVisible(open);
			pluginHost.setPreferredSize(open ? new Dimension(PANEL_W, 200) : new Dimension(0, 0));
		}
		int w = eastWidth();
		east.setPreferredSize(new Dimension(w, 100));
		east.setMinimumSize(new Dimension(w, 0));
	}

	static void refresh() {
		if (listPanel == null) {
			return;
		}
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					refreshNow();
				}
			});
			return;
		}
		refreshNow();
	}

	private static void refreshNow() {
		syncing = true;
		for (int i = 0; i < rows.size(); i++) {
			((PluginRow) rows.get(i)).sync();
		}
		for (int i = 0; i < configRows.size(); i++) {
			((ConfigItem) configRows.get(i)).sync();
		}
		if (openPlugin != null && configToggle != null) {
			configToggle.on = openPlugin.toggleId > 0 && isOn(openPlugin.toggleId);
			configToggle.repaint();
		}
		syncing = false;
		if (listPanel != null) {
			listPanel.revalidate();
			listPanel.repaint();
		}
		if (configItems != null) {
			configItems.revalidate();
			configItems.repaint();
		}
	}

	static void queue(final int id) {
		if (syncing || client.instance == null) {
			return;
		}
		client.instance.queueClientSetting(id);
		client.instance.requestFocus();
	}

	private static JPanel buildIconBar() {
		JPanel bar = new JPanel();
		bar.setPreferredSize(new Dimension(ICON_W, 100));
		bar.setBackground(DARKER);
		bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
		bar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER));
		wrench = new IconTab(TAB_CONFIG);
		wrench.setBackground(tabOn(TAB_CONFIG) ? SELECTED_TAB : DARKER);
		wrench.setAlignmentX(0.5f);
		wrench.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		wrench.setToolTipText("Configuration");
		wrench.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				selectTab(TAB_CONFIG);
			}
		});
		lootTab = new IconTab(TAB_LOOT);
		lootTab.setBackground(tabOn(TAB_LOOT) ? SELECTED_TAB : DARKER);
		lootTab.setAlignmentX(0.5f);
		lootTab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lootTab.setToolTipText("Loot Tracker");
		lootTab.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				selectTab(TAB_LOOT);
			}
		});
		bar.add(javax.swing.Box.createVerticalStrut(4));
		bar.add(wrench);
		bar.add(javax.swing.Box.createVerticalStrut(2));
		bar.add(lootTab);
		bar.add(javax.swing.Box.createVerticalGlue());
		return bar;
	}

	private static JPanel buildListCard() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(DARK);
		panel.setPreferredSize(new Dimension(PANEL_W, 200));
		panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER));

		JPanel top = new JPanel(new BorderLayout(0, 6));
		top.setBackground(DARK);
		top.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		JLabel title = new JLabel("Configuration");
		title.setForeground(Color.WHITE);
		title.setFont(TITLE_FONT);
		top.add(title, BorderLayout.NORTH);
		top.add(buildSearch(), BorderLayout.CENTER);

		listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		listPanel.setBackground(DARK);
		listPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
		listPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel north = new JPanel(new BorderLayout());
		north.setBackground(DARK);
		north.add(listPanel, BorderLayout.NORTH);

		JScrollPane scroll = wrapScroll(north);
		panel.add(top, BorderLayout.NORTH);
		panel.add(scroll, BorderLayout.CENTER);
		return panel;
	}

	private static JPanel buildSearch() {
		final JPanel field = new JPanel(new BorderLayout());
		field.setBackground(DARKER);
		field.setPreferredSize(new Dimension(PANEL_INNER - 20, 30));
		field.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		JPanel icon = new SearchIcon();
		icon.setPreferredSize(new Dimension(30, 30));
		icon.setOpaque(false);
		search = new JTextField();
		search.setOpaque(false);
		search.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
		search.setForeground(TEXT);
		search.setCaretColor(Color.WHITE);
		search.setFont(ROW_FONT);
		search.setSelectedTextColor(Color.WHITE);
		search.setSelectionColor(new Color(220, 138, 0, 120));
		search.setToolTipText("Search plugins");
		clearSearch = new JLabel("\u00D7");
		clearSearch.setForeground(new Color(230, 30, 30));
		clearSearch.setFont(new Font("SansSerif", Font.BOLD, 16));
		clearSearch.setHorizontalAlignment(JLabel.CENTER);
		clearSearch.setPreferredSize(new Dimension(24, 30));
		clearSearch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		clearSearch.setVisible(false);
		clearSearch.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				search.setText("");
			}
		});
		search.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				filter();
			}

			public void removeUpdate(DocumentEvent e) {
				filter();
			}

			public void changedUpdate(DocumentEvent e) {
				filter();
			}
		});
		MouseAdapter hover = new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				field.setBackground(HOVER);
			}

			public void mouseExited(MouseEvent e) {
				field.setBackground(DARKER);
			}
		};
		field.addMouseListener(hover);
		search.addMouseListener(hover);
		field.add(icon, BorderLayout.WEST);
		field.add(search, BorderLayout.CENTER);
		field.add(clearSearch, BorderLayout.EAST);
		return field;
	}

	private static JPanel buildConfigCard() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(DARK);
		panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER));

		JPanel top = new JPanel(new BorderLayout(6, 0));
		top.setBackground(DARK);
		top.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		JPanel back = new BackButton();
		back.setPreferredSize(new Dimension(22, 22));
		back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		back.setToolTipText("Back");
		back.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				showList();
			}
		});
		configTitle = new JLabel();
		configTitle.setForeground(Color.WHITE);
		configTitle.setFont(ROW_FONT);
		configToggle = new ToggleSwitch();
		configToggle.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (openPlugin != null && openPlugin.toggleId > 0) {
					queue(openPlugin.toggleId);
				}
			}
		});
		top.add(back, BorderLayout.WEST);
		top.add(configTitle, BorderLayout.CENTER);
		top.add(configToggle, BorderLayout.EAST);

		configItems = new JPanel();
		configItems.setLayout(new BoxLayout(configItems, BoxLayout.Y_AXIS));
		configItems.setBackground(DARK);
		configItems.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
		JPanel north = new JPanel(new BorderLayout());
		north.setBackground(DARK);
		north.add(configItems, BorderLayout.NORTH);
		panel.add(top, BorderLayout.NORTH);
		panel.add(wrapScroll(north), BorderLayout.CENTER);
		return panel;
	}

	private static JScrollPane wrapScroll(JPanel north) {
		JScrollPane scroll = new JScrollPane(north);
		scroll.setBorder(null);
		scroll.setBackground(DARK);
		scroll.getViewport().setBackground(DARK);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(7, 0));
		scroll.getVerticalScrollBar().setUI(new DarkScrollBarUI());
		return scroll;
	}

	private static void showList() {
		openPlugin = null;
		if (cards != null && configPanel != null) {
			cards.show(configPanel, "list");
		}
	}

	private static void showConfig(PluginDef def) {
		openPlugin = def;
		configTitle.setText(def.name);
		configToggle.setVisible(def.toggleId > 0);
		configToggle.on = def.toggleId > 0 && isOn(def.toggleId);
		configToggle.repaint();
		configRows.clear();
		configItems.removeAll();
		for (int i = 0; i < def.optionIds.length; i++) {
			ConfigItem item = new ConfigItem(def.optionIds[i], def.optionNames[i]);
			configRows.add(item);
			configItems.add(item);
		}
		configItems.add(javax.swing.Box.createVerticalGlue());
		configItems.revalidate();
		configItems.repaint();
		cards.show(configPanel, "config");
	}

	private static void rebuildList() {
		rows.clear();
		if (listPanel == null) {
			return;
		}
		listPanel.removeAll();
		PluginDef[] defs = defs();
		for (int i = 0; i < defs.length; i++) {
			PluginRow row = new PluginRow(defs[i]);
			rows.add(row);
			listPanel.add(row);
		}
		listPanel.add(javax.swing.Box.createVerticalGlue());
		listPanel.revalidate();
		listPanel.repaint();
	}

	private static void filter() {
		String q = search == null ? "" : search.getText().trim().toLowerCase();
		if (clearSearch != null) {
			clearSearch.setVisible(q.length() > 0);
		}
		for (int i = 0; i < rows.size(); i++) {
			PluginRow row = (PluginRow) rows.get(i);
			row.setVisible(q.length() == 0 || row.matches(q));
		}
		if (listPanel != null) {
			listPanel.revalidate();
		}
	}

	private static PluginDef[] defs() {
		return new PluginDef[] {
			def("Ammo Overlay", 24263, null, null),
			def("Anti Drag", 24264, ids(24265, 24266), names("Shift only", "Delay")),
			def("Attack Styles", 24224, ids(24267), names("Warn skill")),
			def("Barrows Brothers", 24408, null, null),
			def("Boosted Stats", 24221, ids(24223), names("Show as +N")),
			def("Boss Timers", 24276, null, null),
			def("Cannon", 24400, ids(24401, 24402, 24403, 24404),
					names("Infobox", "Low-ball warning", "Double-hit tiles", "Cannon spots")),
			def("Chat Channels", 24411, ids(24412), names("Join/leave messages")),
			def("Chat History", 24410, null, null),
			def("Chat Timestamps", 24243, null, null),
			def("Client Display", 0, ids(24210, 24248, 24211, 24212, 24213, 24215, 24216, 24217, 24233, 24234, 24231, 24232),
					names("Resizable client", "Inventory tab", "Distance fog", "Anti-aliasing",
							"Animation smoothing", "Draw distance", "Ground blending", "Hide roofs",
							"OpenGL acceleration", "FPS cap", "Performance stats", "Show ping")),
			def("Combat Level", 24409, null, null),
			def("Destination Tile", 24241, null, null),
			def("Friend List", 24413, null, null),
			def("Friend Notes", 24414, null, null),
			def("Ground Items", 24219, ids(24239, 24240, 24420, 24421),
					names("Hide loot below", "Loot beams", "Beam style", "Drop fanfare")),
			def("Ground Markers", 24229, null, null),
			def("Implings", 24405, ids(24406, 24407), names("Names", "Notify")),
			def("Inventory Tags", 24268, null, null),
			def("Item Stats", 24275, null, null),
			def("Key Remapping", 24310, ids(24228, 24311, 24312, 24313),
					names("Open key bind setup", "Enter to chat", "Space to continue", "WASD camera")),
			def("Low HP/Prayer Flash", 24247, null, null),
			def("Menu Entry Swapper", 24227, null, null),
			def("Middle-click Wear", 24237, null, null),
			def("Mouse Tooltips", 24269, null, null),
			def("NPC Attack", 24225, null, null),
			def("NPC Health Overlay", 24220, null, null),
			def("NPC Indicators", 24253, ids(24254, 24255, 24256, 24257, 24258, 24259, 24260),
					names("Hull", "Tile", "True tile", "South-west tile", "Colour", "Names", "Minimap names")),
			def("Object Markers", 24270, null, null),
			def("Player Attack", 24226, null, null),
			def("Player Indicators", 24271, ids(24272, 24273, 24274, 24277, 24278, 24279, 24280),
					names("Names", "Tiles", "Minimap names", "Friends", "Team", "Others", "Self")),
			def("Poison", 24415, null, null),
			def("Regeneration Meter", 24416, null, null),
			def("Shift-click Drop", 24236, null, null),
			def("Silent Screenshots", 24244, null, null),
			def("Slayer", 24261, ids(24262, 24281), names("Highlight task NPCs", "Count on gem/helm")),
			def("Special Attack Orb", 24238, null, null),
			def("Status Bars", 24417, ids(24418, 24419), names("Numbers", "Heal preview")),
			def("Status Timers", 24246, null, null),
			def("Tile Markers", 24218, null, null),
			def("True Tile", 24242, null, null),
			def("XP Drops", 24222, ids(24235, 24422), names("Drop speed", "Group XP drops"))
		};
	}

	private static PluginDef def(String name, int toggle, int[] options, String[] optionNames) {
		PluginDef d = new PluginDef();
		d.name = name;
		d.toggleId = toggle;
		d.optionIds = options == null ? new int[0] : options;
		d.optionNames = optionNames == null ? new String[0] : optionNames;
		return d;
	}

	private static int[] ids(int a) {
		return new int[] { a };
	}

	private static int[] ids(int a, int b) {
		return new int[] { a, b };
	}

	private static int[] ids(int a, int b, int c) {
		return new int[] { a, b, c };
	}

	private static int[] ids(int a, int b, int c, int d) {
		return new int[] { a, b, c, d };
	}

	private static int[] ids(int a, int b, int c, int d, int e, int f, int g) {
		return new int[] { a, b, c, d, e, f, g };
	}

	private static int[] ids(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l) {
		return new int[] { a, b, c, d, e, f, g, h, i, j, k, l };
	}

	private static String[] names(String a) {
		return new String[] { a };
	}

	private static String[] names(String a, String b) {
		return new String[] { a, b };
	}

	private static String[] names(String a, String b, String c) {
		return new String[] { a, b, c };
	}

	private static String[] names(String a, String b, String c, String d) {
		return new String[] { a, b, c, d };
	}

	private static String[] names(String a, String b, String c, String d, String e, String f, String g) {
		return new String[] { a, b, c, d, e, f, g };
	}

	private static String[] names(String a, String b, String c, String d, String e, String f, String g, String h,
			String i, String j, String k, String l) {
		return new String[] { a, b, c, d, e, f, g, h, i, j, k, l };
	}

	static boolean isOn(int id) {
		switch (id) {
		case 24210:
			return !client.isFixed();
		case 24248:
			return client.resizableInvTransparent;
		case 24213:
			return client.tweeningEnabled;
		case 24216:
			return client.tileBlending;
		case 24217:
			return client.hideRoofs;
		case 24233:
			return client.openGlEnabled;
		case 24234:
			return client.fpsUnlocked;
		case 24231:
			return client.performanceStats;
		case 24232:
			return client.showPing;
		case 24218:
			return client.tileMarkers;
		case 24219:
			return client.groundItemNames;
		case 24220:
			return client.npcHealthOverlay;
		case 24221:
			return client.boostedStatOverlay;
		case 24222:
			return client.xpDrops;
		case 24422:
			return client.xpDropGrouped;
		case 24223:
			return client.boostedPlusDisplay;
		case 24224:
			return client.attackStyleOverlay;
		case 24225:
			return client.npcAttackOption != 0;
		case 24226:
			return client.playerAttackOption != 0;
		case 24227:
			return client.menuEntrySwapper;
		case 24228:
			return client.keyRemapping;
		case 24310:
			return client.keyRemapping;
		case 24229:
			return GroundMarkers.enabled;
		case 24236:
			return client.shiftClickDrop;
		case 24237:
			return client.middleClickWear;
		case 24238:
			return client.specOrb;
		case 24241:
			return client.destTile;
		case 24242:
			return client.trueTile;
		case 24243:
			return client.chatTimestamps;
		case 24244:
			return client.silentScreenshots;
		case 24246:
			return client.statusTimers;
		case 24247:
			return client.orbFlash;
		case 24253:
			return NpcIndicators.mode != 0;
		case 24254:
			return NpcIndicators.hull;
		case 24255:
			return NpcIndicators.tile;
		case 24256:
			return NpcIndicators.trueTile;
		case 24257:
			return NpcIndicators.southWestTile;
		case 24259:
			return NpcIndicators.names;
		case 24260:
			return NpcIndicators.minimapNames;
		case 24261:
			return SlayerTracker.enabled;
		case 24262:
			return SlayerTracker.highlight;
		case 24281:
			return SlayerTracker.countOnItems;
		case 24276:
			return BossTimers.enabled;
		case 24270:
			return ObjectMarkers.enabled;
		case 24268:
			return InventoryTags.enabled;
		case 24271:
			return PlayerIndicators.enabled;
		case 24272:
			return PlayerIndicators.names;
		case 24273:
			return PlayerIndicators.tiles;
		case 24274:
			return PlayerIndicators.minimapNames;
		case 24277:
			return PlayerIndicators.friends;
		case 24278:
			return PlayerIndicators.team;
		case 24279:
			return PlayerIndicators.others;
		case 24280:
			return PlayerIndicators.ownPlayer;
		case 24263:
			return AmmoOverlay.enabled;
		case 24275:
			return ItemStats.enabled;
		case 24269:
			return MouseTooltips.enabled;
		case 24264:
			return AntiDrag.enabled;
		case 24265:
			return AntiDrag.shiftOnly;
		case 24400:
			return CannonOverlay.enabled;
		case 24401:
			return CannonOverlay.infobox;
		case 24403:
			return CannonOverlay.doubleHit;
		case 24404:
			return CannonOverlay.spots;
		case 24405:
			return ImplingsPlugin.enabled;
		case 24406:
			return ImplingsPlugin.names;
		case 24407:
			return ImplingsPlugin.notify;
		case 24408:
			return BarrowsPlugin.enabled;
		case 24409:
			return CombatLevelPlugin.enabled;
		case 24410:
			return ChatHistory.enabled;
		case 24411:
			return ChatChannels.enabled;
		case 24412:
			return ChatChannels.joinLeave;
		case 24413:
			return FriendListPlugin.enabled;
		case 24414:
			return FriendNotes.enabled;
		case 24415:
			return PoisonPlugin.enabled;
		case 24416:
			return RegenMeter.enabled;
		case 24417:
			return StatusBars.enabled;
		case 24418:
			return StatusBars.numbers;
		case 24419:
			return StatusBars.healPreview;
		case 24311:
			return client.enterToChat;
		case 24312:
			return client.spaceContinue;
		case 24313:
			return client.wasdCamera;
		default:
			return false;
		}
	}

	private static boolean isCycle(int id) {
		return id == 24211 || id == 24212 || id == 24215 || id == 24225 || id == 24226
				|| id == 24235 || id == 24239 || id == 24240 || id == 24420 || id == 24421 || id == 24253 || id == 24258
				|| id == 24266 || id == 24267 || id == 24402 || id == 24228 || id == 24248;
	}

	private static String optionText(int id, String fallback) {
		if (id == 24420) {
			return "Beam style: " + LootBeams.styleName();
		}
		if (id == 24421) {
			return "Drop fanfare: " + LootBeams.fanfareName();
		}
		if (id == 24228) {
			return fallback;
		}
		if (RSInterface.interfaceCache != null && id >= 0 && id < RSInterface.interfaceCache.length
				&& RSInterface.interfaceCache[id] != null && RSInterface.interfaceCache[id].message != null
				&& RSInterface.interfaceCache[id].message.length() > 0) {
			return RSInterface.interfaceCache[id].message;
		}
		return fallback;
	}

	private static String valuePart(int id, String fallback) {
		String t = optionText(id, fallback);
		int c = t.lastIndexOf(": ");
		if (c >= 0 && c + 2 < t.length()) {
			return t.substring(c + 2);
		}
		return t;
	}

	private static final class PluginDef {
		String name;
		int toggleId;
		int[] optionIds;
		String[] optionNames;
	}

	private static final class PluginRow extends JPanel {
		private static final long serialVersionUID = 1L;
		final PluginDef def;
		final ToggleSwitch toggle;
		final JLabel name;

		PluginRow(PluginDef def) {
			this.def = def;
			setLayout(new BorderLayout(3, 0));
			setBackground(DARK);
			setOpaque(true);
			setAlignmentX(0f);
			setPreferredSize(new Dimension(PANEL_INNER, 20));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
			setMinimumSize(new Dimension(0, 20));
			setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

			name = new JLabel(def.name);
			name.setForeground(Color.WHITE);
			name.setFont(ROW_FONT);
			add(name, BorderLayout.CENTER);

			JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
			buttons.setOpaque(false);
			if (def.optionIds.length > 0) {
				JPanel gear = new GearButton();
				gear.setPreferredSize(new Dimension(25, 20));
				gear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				gear.setToolTipText("Edit plugin configuration");
				gear.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent e) {
						showConfig(PluginRow.this.def);
					}
				});
				buttons.add(gear);
			}
			toggle = new ToggleSwitch();
			toggle.setVisible(def.toggleId > 0);
			toggle.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (PluginRow.this.def.toggleId > 0) {
						queue(PluginRow.this.def.toggleId);
					}
				}
			});
			if (def.toggleId > 0) {
				buttons.add(toggle);
			}
			add(buttons, BorderLayout.EAST);

			name.addMouseListener(new MouseAdapter() {
				public void mouseEntered(MouseEvent e) {
					name.setForeground(ORANGE);
				}

				public void mouseExited(MouseEvent e) {
					name.setForeground(Color.WHITE);
				}
			});
			sync();
		}

		void sync() {
			if (def.toggleId > 0) {
				toggle.on = isOn(def.toggleId);
				toggle.repaint();
			}
		}

		boolean matches(String q) {
			if (def.name.toLowerCase().indexOf(q) >= 0) {
				return true;
			}
			for (int i = 0; i < def.optionNames.length; i++) {
				if (def.optionNames[i].toLowerCase().indexOf(q) >= 0) {
					return true;
				}
			}
			return false;
		}
	}

	private static final class ConfigItem extends JPanel {
		private static final long serialVersionUID = 1L;
		final int id;
		final String fallback;
		final JLabel name;
		final ToggleSwitch toggle;
		final JButton value;

		ConfigItem(int id, String fallback) {
			this.id = id;
			this.fallback = fallback;
			setLayout(new BorderLayout(6, 0));
			setBackground(DARK);
			setOpaque(true);
			setAlignmentX(0f);
			setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
			setPreferredSize(new Dimension(PANEL_INNER, 28));
			setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
			name = new JLabel(fallback);
			name.setForeground(Color.WHITE);
			name.setFont(ROW_FONT);
			add(name, BorderLayout.CENTER);
			boolean cycle = isCycle(id);
			if (cycle) {
				toggle = null;
				value = new JButton();
				value.setFont(SMALL_FONT);
				value.setForeground(TEXT);
				value.setBackground(DARKER);
				value.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
				value.setFocusPainted(false);
				value.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				value.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						queue(ConfigItem.this.id);
					}
				});
				add(value, BorderLayout.EAST);
			} else {
				value = null;
				toggle = new ToggleSwitch();
				toggle.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent e) {
						queue(ConfigItem.this.id);
					}
				});
				add(toggle, BorderLayout.EAST);
			}
			name.addMouseListener(new MouseAdapter() {
				public void mouseEntered(MouseEvent e) {
					name.setForeground(ORANGE);
				}

				public void mouseExited(MouseEvent e) {
					name.setForeground(Color.WHITE);
				}
			});
			sync();
		}

		void sync() {
			if (toggle != null) {
				toggle.on = isOn(id);
				toggle.repaint();
			}
			if (value != null) {
				value.setText(valuePart(id, fallback));
			}
		}
	}

	private static final class ToggleSwitch extends JPanel {
		private static final long serialVersionUID = 1L;
		boolean on;

		ToggleSwitch() {
			setPreferredSize(new Dimension(25, 16));
			setMinimumSize(new Dimension(25, 16));
			setMaximumSize(new Dimension(25, 16));
			setOpaque(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText("Enable plugin");
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int w = 25;
			int h = 12;
			int y = (getHeight() - h) / 2;
			g2.setColor(on ? ORANGE : MEDIUM);
			g2.fillRoundRect(0, y, w - 1, h, h, h);
			g2.setColor(on ? Color.WHITE : LIGHT);
			int knob = h - 2;
			int x = on ? w - knob - 2 : 1;
			g2.fillOval(x, y + 1, knob, knob);
			g2.dispose();
		}
	}

	private static final class IconTab extends JPanel {
		private static final long serialVersionUID = 1L;
		private final int kind;

		IconTab(int kind) {
			this.kind = kind;
			setPreferredSize(new Dimension(ICON_W, 26));
			setMaximumSize(new Dimension(ICON_W, 26));
			setMinimumSize(new Dimension(ICON_W, 26));
			setOpaque(true);
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			boolean on = tabOn(kind);
			g2.setColor(on ? ORANGE : LIGHT);
			int cx = getWidth() / 2;
			int cy = getHeight() / 2;
			if (kind == TAB_LOOT) {
				g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g2.drawRoundRect(cx - 7, cy - 3, 14, 11, 4, 4);
				g2.drawArc(cx - 6, cy - 9, 12, 10, 200, 140);
				g2.fillOval(cx - 4, cy - 1, 5, 5);
				g2.fillOval(cx, cy + 1, 5, 5);
			} else {
				g2.translate(cx, cy);
				g2.rotate(Math.toRadians(-45));
				g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g2.drawRoundRect(-6, -8, 8, 6, 3, 3);
				g2.fillRect(-1, -2, 3, 11);
				g2.fillRect(-4, 7, 9, 3);
			}
			g2.dispose();
		}
	}

	private static final class GearButton extends JPanel {
		private static final long serialVersionUID = 1L;

		GearButton() {
			setOpaque(false);
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int cx = getWidth() / 2;
			int cy = getHeight() / 2;
			g2.setColor(LIGHT);
			for (int i = 0; i < 6; i++) {
				double a = i * Math.PI / 3.0;
				int x = cx + (int) Math.round(Math.cos(a) * 5);
				int y = cy + (int) Math.round(Math.sin(a) * 5);
				g2.fillOval(x - 2, y - 2, 5, 5);
			}
			g2.fillOval(cx - 4, cy - 4, 8, 8);
			g2.setColor(DARK);
			g2.fillOval(cx - 2, cy - 2, 4, 4);
			g2.dispose();
		}
	}

	private static final class SearchIcon extends JPanel {
		private static final long serialVersionUID = 1L;

		SearchIcon() {
			setOpaque(false);
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(LIGHT);
			g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.drawOval(8, 7, 11, 11);
			g2.drawLine(17, 17, 22, 22);
			g2.dispose();
		}
	}

	private static final class BackButton extends JPanel {
		private static final long serialVersionUID = 1L;

		BackButton() {
			setOpaque(false);
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(LIGHT);
			g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			int x = 8;
			int y = getHeight() / 2;
			g2.drawLine(x + 8, y - 6, x, y);
			g2.drawLine(x, y, x + 8, y + 6);
			g2.dispose();
		}
	}

	private static final class DarkScrollBarUI extends BasicScrollBarUI {
		protected void configureScrollBarColors() {
			thumbColor = MEDIUM;
			trackColor = SCROLL_TRACK;
			thumbDarkShadowColor = MEDIUM;
			thumbHighlightColor = MEDIUM;
			thumbLightShadowColor = MEDIUM;
			trackHighlightColor = SCROLL_TRACK;
		}

		protected JButton createDecreaseButton(int orientation) {
			return zero();
		}

		protected JButton createIncreaseButton(int orientation) {
			return zero();
		}

		private JButton zero() {
			JButton b = new JButton();
			Dimension d = new Dimension(0, 0);
			b.setPreferredSize(d);
			b.setMinimumSize(d);
			b.setMaximumSize(d);
			b.setBorder(null);
			b.setOpaque(false);
			b.setContentAreaFilled(false);
			return b;
		}

		protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
			g.setColor(SCROLL_TRACK);
			g.fillRect(r.x, r.y, r.width, r.height);
		}

		protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
			if (!c.isEnabled() || r.width > r.height && r.height < 8 || r.height >= r.width && r.width < 4) {
				return;
			}
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(MEDIUM);
			int pad = 1;
			g2.fillRoundRect(r.x + pad, r.y + pad, Math.max(4, r.width - pad * 2), Math.max(8, r.height - pad * 2), 6, 6);
			g2.dispose();
		}
	}
}
