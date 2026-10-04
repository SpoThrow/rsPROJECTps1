package ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;import cache.StreamLoader;
import game.Skills;
import game.client;



/**
 * RuneLite-style hiscores lookup against this server's player saves.
 */
public final class HiscoresPanel {

	private static final Color DARKER = new Color(30, 30, 30);
	private static final Color DARK = new Color(40, 40, 40);
	private static final Color TEXT = new Color(198, 198, 198);
	private static final Color LIGHT = new Color(165, 165, 165);
	private static final Color ORANGE = new Color(255, 152, 31);
	private static final Color TIP_BG = new Color(43, 43, 43);
	private static final Color TIP_BORDER = new Color(70, 70, 70);
	private static final Font TITLE_FONT = new Font("SansSerif", Font.PLAIN, 16);
	private static final Font ROW_FONT = new Font("SansSerif", Font.PLAIN, 13);
	private static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 11);
	private static final Font LEVEL_FONT = new Font("SansSerif", Font.PLAIN, 14);
	private static final Font TIP_FONT = new Font("SansSerif", Font.PLAIN, 12);

	private static final int[] GRID = {
			0, 3, 14,
			2, 16, 13,
			1, 15, 10,
			4, 17, 7,
			5, 12, 11,
			6, 9, 8,
			20, 18, 19,
			22, 21, -1
	};
	private static final int[] SKILL_WIDGET = {
			8654, 8660, 8657, 8655, 8663, 8666, 8669, 8665, 8671, 8670, 8662, 8668,
			8667, 8659, 8656, 8661, 8658, 8664, 12162, 13928, 8672, 19747, 19748
	};
	private static final int[] XP_TABLE = buildXpTable();
	private static final int ICON = 20;

	private static JPanel root;
	private static JTextField search;
	private static JLabel clearSearch;
	private static JPanel result;
	private static JLabel nameLabel;
	private static JLabel statusLabel;
	private static JLabel[] levelLabels;
	private static JLabel[] skillIcons;
	private static SkillCell[] skillCells;
	private static JLabel combatLabel;
	private static JLabel totalLabel;
	private static SkillCell totalCell;
	private static JPanel empty;
	private static final Image[] skillImages = new Image[23];
	private static int[] lastXp = new int[23];
	private static int[] lastLvl = new int[23];
	private static int[] lastRank = new int[24];
	private static int lastCombat;
	private static int lastTotalLvl;
	private static long lastTotalXp;

	static JPanel buildPanel() {
		ToolTipManager.sharedInstance().setInitialDelay(250);
		ToolTipManager.sharedInstance().setDismissDelay(20000);
		root = new JPanel(new BorderLayout());
		root.setBackground(DARK);
		root.setPreferredSize(new Dimension(PluginSidebar.PANEL_W, 200));
		root.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(23, 23, 23)));

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setBackground(DARK);
		top.setBorder(BorderFactory.createEmptyBorder(8, 8, 6, 8));
		top.add(buildSearch());

		result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
		result.setBackground(DARK);
		result.setBorder(BorderFactory.createEmptyBorder(4, 10, 10, 10));
		result.add(buildHeader());
		result.add(Box.createVerticalStrut(8));
		result.add(buildGrid());
		result.add(Box.createVerticalStrut(10));
		result.add(buildFooter());
		result.setVisible(false);

		empty = new JPanel(new BorderLayout());
		empty.setBackground(DARK);
		empty.setBorder(BorderFactory.createEmptyBorder(20, 12, 12, 12));
		JLabel hint = new JLabel("<html>Search a player name to look up<br>their skills on this server.</html>");
		hint.setForeground(LIGHT);
		hint.setFont(SMALL_FONT);
		empty.add(hint, BorderLayout.NORTH);

		JPanel body = new JPanel(new BorderLayout());
		body.setBackground(DARK);
		body.add(result, BorderLayout.NORTH);
		body.add(empty, BorderLayout.CENTER);

		root.add(top, BorderLayout.NORTH);
		root.add(body, BorderLayout.CENTER);
		applyIcons();
		return root;
	}

	public static void loadSkillSprites(StreamLoader media) {
		try {
			for (int skill = 0; skill < 23; skill++) {
				try {
					Image image = toImage(spriteFromWidget(SKILL_WIDGET[skill]), ICON);
					if (image == null && (skill == 21 || skill == 22)) {
						image = toImage(diskSprite("Skill/CUSTOM5 " + (skill == 21 ? 51 : 50)), ICON);
					}
					if (image == null) {
						int chat = skill;
						if (skill == 21) {
							chat = 22;
						} else if (skill == 22) {
							chat = 21;
						}
						image = toImage(diskSprite("Interfaces/skillchat/skill " + chat), ICON);
					}
					if (image != null) {
						skillImages[skill] = image;
					}
				} catch (Throwable t) {
				}
			}
		} catch (Throwable t) {
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				applyIcons();
			}
		});
	}

	private static Sprite spriteFromWidget(int id) {
		if (RSInterface.interfaceCache == null || id < 0 || id >= RSInterface.interfaceCache.length) {
			return null;
		}
		RSInterface rsi = RSInterface.interfaceCache[id];
		if (rsi == null) {
			return null;
		}
		if (iconSprite(rsi.sprite1)) {
			return rsi.sprite1;
		}
		if (iconSprite(rsi.sprite2)) {
			return rsi.sprite2;
		}
		return null;
	}

	private static boolean iconSprite(Sprite sprite) {
		if (sprite == null || sprite.myPixels == null) {
			return false;
		}
		int w = sprite.myWidth;
		int h = sprite.myHeight;
		if (w < 4 || h < 4 || w > 128 || h > 128) {
			return false;
		}
		long pixels = (long) w * (long) h;
		return pixels <= sprite.myPixels.length && pixels <= 16384L;
	}

	private static Sprite diskSprite(String path) {
		try {
			Sprite sprite = new Sprite(path);
			return iconSprite(sprite) ? sprite : null;
		} catch (Throwable e) {
			return null;
		}
	}

	private static boolean opaquePixel(int p) {
		int rgb = p & 0xffffff;
		return rgb != 0 && rgb != 0xff00ff;
	}

	private static Image toImage(Sprite sprite, int size) {
		if (!iconSprite(sprite)) {
			return null;
		}
		int w = sprite.myWidth;
		int h = sprite.myHeight;
		int[] px = sprite.myPixels;
		int minX = w;
		int minY = h;
		int maxX = -1;
		int maxY = -1;
		for (int y = 0; y < h; y++) {
			int row = y * w;
			for (int x = 0; x < w; x++) {
				if (opaquePixel(px[row + x])) {
					if (x < minX) {
						minX = x;
					}
					if (y < minY) {
						minY = y;
					}
					if (x > maxX) {
						maxX = x;
					}
					if (y > maxY) {
						maxY = y;
					}
				}
			}
		}
		if (maxX < minX) {
			return null;
		}
		int cw = maxX - minX + 1;
		int ch = maxY - minY + 1;
		if (cw >= ch * 3 / 2 && ch >= 8 && ch <= 48) {
			cw = ch;
			if (minX + cw > w) {
				cw = w - minX;
			}
		}
		if (cw > 64) {
			cw = 32;
		}
		if (ch > 64) {
			ch = 32;
		}
		BufferedImage img = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < ch; y++) {
			int row = (minY + y) * w;
			for (int x = 0; x < cw; x++) {
				int p = px[row + minX + x];
				if (opaquePixel(p)) {
					img.setRGB(x, y, 0xff000000 | (p & 0xffffff));
				}
			}
		}
		if (cw == size && ch == size) {
			return img;
		}
		int dw = size;
		int dh = size;
		if (cw > ch) {
			dh = Math.max(1, size * ch / cw);
		} else if (ch > cw) {
			dw = Math.max(1, size * cw / ch);
		}
		BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = scaled.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(img, (size - dw) / 2, (size - dh) / 2, dw, dh, null);
		g.dispose();
		return scaled;
	}

	private static void applyIcons() {
		if (skillIcons == null) {
			return;
		}
		for (int i = 0; i < GRID.length; i++) {
			int skill = GRID[i];
			if (skill < 0 || skillIcons[i] == null) {
				continue;
			}
			Image image = skillImages[skill];
			if (image == null) {
				image = skillImage(skill);
			}
			skillIcons[i].setIcon(new ImageIcon(image));
		}
	}

	private static JPanel buildSearch() {
		final JPanel field = new JPanel(new BorderLayout());
		field.setBackground(DARKER);
		field.setPreferredSize(new Dimension(PluginSidebar.PANEL_INNER - 16, 28));
		field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		field.setAlignmentX(0f);
		JPanel icon = new SearchGlyph();
		icon.setPreferredSize(new Dimension(28, 28));
		icon.setOpaque(false);
		search = new JTextField();
		search.setOpaque(false);
		search.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		search.setForeground(Color.WHITE);
		search.setCaretColor(Color.WHITE);
		search.setFont(ROW_FONT);
		search.setBackground(DARKER);
		search.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				lookup(search.getText());
			}
		});
		search.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				syncClear();
			}

			public void removeUpdate(DocumentEvent e) {
				syncClear();
			}

			public void changedUpdate(DocumentEvent e) {
				syncClear();
			}
		});
		clearSearch = new JLabel("x");
		clearSearch.setForeground(LIGHT);
		clearSearch.setFont(TITLE_FONT);
		clearSearch.setBorder(BorderFactory.createEmptyBorder(0, 6, 2, 8));
		clearSearch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		clearSearch.setVisible(false);
		clearSearch.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				search.setText("");
				search.requestFocusInWindow();
			}
		});
		field.add(icon, BorderLayout.WEST);
		field.add(search, BorderLayout.CENTER);
		field.add(clearSearch, BorderLayout.EAST);
		return field;
	}

	private static JPanel buildHeader() {
		JPanel head = new JPanel(new BorderLayout(8, 2));
		head.setOpaque(false);
		head.setAlignmentX(0f);
		head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
		JLabel trophy = new JLabel(new ImageIcon(trophyImage()));
		nameLabel = new JLabel(" ");
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setFont(TITLE_FONT);
		statusLabel = new JLabel(" ");
		statusLabel.setForeground(LIGHT);
		statusLabel.setFont(SMALL_FONT);
		JPanel names = new JPanel();
		names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
		names.setOpaque(false);
		names.add(nameLabel);
		names.add(statusLabel);
		head.add(trophy, BorderLayout.WEST);
		head.add(names, BorderLayout.CENTER);
		return head;
	}

	private static JPanel buildGrid() {
		JPanel grid = new JPanel(new GridLayout(8, 3, 4, 6));
		grid.setOpaque(false);
		grid.setAlignmentX(0f);
		levelLabels = new JLabel[GRID.length];
		skillIcons = new JLabel[GRID.length];
		skillCells = new SkillCell[GRID.length];
		for (int i = 0; i < GRID.length; i++) {
			final int skill = GRID[i];
			if (skill < 0) {
				JPanel cell = new JPanel(new BorderLayout());
				cell.setOpaque(false);
				grid.add(cell);
				continue;
			}
			SkillCell cell = new SkillCell(skill);
			skillIcons[i] = new HoverLabel(skill);
			skillIcons[i].setIcon(new ImageIcon(skillImage(skill)));
			levelLabels[i] = new HoverLabel(skill);
			levelLabels[i].setText("-");
			levelLabels[i].setForeground(TEXT);
			levelLabels[i].setFont(LEVEL_FONT);
			cell.add(skillIcons[i], BorderLayout.WEST);
			cell.add(levelLabels[i], BorderLayout.CENTER);
			skillCells[i] = cell;
			grid.add(cell);
		}
		return grid;
	}

	private static JPanel buildFooter() {
		JPanel foot = new JPanel(new GridLayout(1, 2, 8, 0));
		foot.setOpaque(false);
		foot.setAlignmentX(0f);
		foot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		combatLabel = new HoverLabel(-2);
		combatLabel.setIcon(new ImageIcon(crossedSwordsImage()));
		combatLabel.setText("0");
		combatLabel.setForeground(TEXT);
		combatLabel.setFont(LEVEL_FONT);
		combatLabel.setIconTextGap(6);
		totalLabel = new HoverLabel(-1);
		totalLabel.setIcon(new ImageIcon(barsImage()));
		totalLabel.setText("0");
		totalLabel.setForeground(TEXT);
		totalLabel.setFont(LEVEL_FONT);
		totalLabel.setIconTextGap(6);
		SkillCell combatCell = new SkillCell(-2);
		combatCell.add(combatLabel, BorderLayout.WEST);
		totalCell = new SkillCell(-1);
		totalCell.add(totalLabel, BorderLayout.WEST);
		foot.add(combatCell);
		foot.add(totalCell);
		return foot;
	}

	private static void syncClear() {
		if (clearSearch != null && search != null) {
			clearSearch.setVisible(search.getText().length() > 0);
		}
	}

	static void lookup(String raw) {
		if (search != null && raw == null) {
			raw = search.getText();
		}
		if (raw == null) {
			raw = "";
		}
		raw = raw.trim();
		if (raw.length() == 0 && client.myPlayer != null && client.myPlayer.name != null) {
			raw = client.myPlayer.name;
		}
		if (raw.length() == 0) {
			showHint("Type a player name.");
			return;
		}
		if (client.instance == null || !client.instance.loggedIn) {
			showHint("Log in to look up players on this server.");
			return;
		}
		statusLabel.setText("Looking up...");
		empty.setVisible(false);
		result.setVisible(true);
		nameLabel.setText(raw);
		client.instance.sendString(5, raw);
	}

	public static void onPacket(final String payload) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				apply(payload);
			}
		});
	}

	public static void onXpPacket(final String payload) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				applyXp(payload);
			}
		});
	}

	public static void onRankPacket(final String payload) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				applyRanks(payload);
			}
		});
	}

	public static void onMissing(final String msg) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				showHint(msg == null || msg.length() == 0 ? "Player not found." : msg);
			}
		});
	}

	private static void showHint(String text) {
		if (empty != null) {
			empty.removeAll();
			JLabel hint = new JLabel("<html>" + text + "</html>");
			hint.setForeground(LIGHT);
			hint.setFont(SMALL_FONT);
			empty.add(hint, BorderLayout.NORTH);
			empty.setVisible(true);
		}
		if (result != null) {
			result.setVisible(false);
		}
		if (statusLabel != null) {
			statusLabel.setText(" ");
		}
		if (root != null) {
			root.revalidate();
			root.repaint();
		}
	}

	private static void apply(String payload) {
		if (payload == null || payload.length() == 0) {
			showHint("Player not found.");
			return;
		}
		String[] parts = split(payload, '|');
		if (parts.length < 6) {
			showHint("Could not read that hiscores reply.");
			return;
		}
		String name = parts[0];
		boolean online = parts[1].equals("1");
		lastCombat = parseInt(parts[2], 3);
		lastTotalLvl = parseInt(parts[3], 32);
		lastTotalXp = parseLong(parts[4], 0L);
		String[] lvls = split(parts[5], '.');
		String[] xps = parts.length > 6 ? split(parts[6], '.') : new String[0];
		nameLabel.setText(name);
		statusLabel.setText(online ? "Online" : "Offline");
		combatLabel.setText(Integer.toString(lastCombat));
		totalLabel.setText(formatCount(lastTotalLvl));
		for (int i = 0; i < 23; i++) {
			lastLvl[i] = i < lvls.length ? parseInt(lvls[i], 1) : 1;
			if (xps.length > 0) {
				lastXp[i] = i < xps.length ? parseInt(xps[i], 0) : 0;
			}
		}
		for (int i = 0; i < GRID.length; i++) {
			int skill = GRID[i];
			if (skill < 0 || levelLabels[i] == null) {
				continue;
			}
			levelLabels[i].setText(Integer.toString(lastLvl[skill]));
		}
		applyIcons();
		empty.setVisible(false);
		result.setVisible(true);
		root.revalidate();
		root.repaint();
	}

	private static void applyXp(String payload) {
		if (payload == null) {
			return;
		}
		String[] xps = split(payload, '.');
		for (int i = 0; i < 23; i++) {
			lastXp[i] = i < xps.length ? parseInt(xps[i], lastXp[i]) : lastXp[i];
		}
	}

	private static void applyRanks(String payload) {
		if (payload == null) {
			return;
		}
		String[] ranks = split(payload, '.');
		for (int i = 0; i < lastRank.length; i++) {
			lastRank[i] = i < ranks.length ? parseInt(ranks[i], 0) : 0;
		}
	}

	private static String skillTitle(int skill) {
		if (skill < 0 || skill >= Skills.skillNames.length) {
			return "Skill";
		}
		String n = Skills.skillNames[skill];
		if (n.equals("hitpoints")) {
			return "Hitpoints";
		}
		if (n.equals("runecraft")) {
			return "Runecraft";
		}
		return Character.toUpperCase(n.charAt(0)) + n.substring(1);
	}

	static String formatCount(int value) {
		return String.format("%,d", Integer.valueOf(value));
	}

	static String formatCount(long value) {
		return String.format("%,d", Long.valueOf(value));
	}

	private static int parseInt(String s, int fallback) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return fallback;
		}
	}

	private static long parseLong(String s, long fallback) {
		try {
			return Long.parseLong(s);
		} catch (Exception e) {
			return fallback;
		}
	}

	private static String[] split(String raw, char sep) {
		int n = 1;
		for (int i = 0; i < raw.length(); i++) {
			if (raw.charAt(i) == sep) {
				n++;
			}
		}
		String[] out = new String[n];
		int start = 0;
		int at = 0;
		for (int i = 0; i < raw.length(); i++) {
			if (raw.charAt(i) == sep) {
				out[at++] = raw.substring(start, i);
				start = i + 1;
			}
		}
		out[at] = raw.substring(start);
		return out;
	}

	private static int[] buildXpTable() {
		int[] table = new int[100];
		table[1] = 0;
		int points = 0;
		for (int lvl = 1; lvl < 99; lvl++) {
			points += (int) Math.floor(lvl + 300.0 * Math.pow(2.0, lvl / 7.0));
			table[lvl + 1] = points / 4;
		}
		return table;
	}

	private static int xpForLevel(int level) {
		if (level <= 1) {
			return 0;
		}
		if (level > 99) {
			return XP_TABLE[99];
		}
		return XP_TABLE[level];
	}

	private static String rankText(int rank) {
		if (rank < 1) {
			return "--";
		}
		return formatCount(rank);
	}

	private static BufferedImage trophyImage() {
		BufferedImage img = new BufferedImage(22, 22, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(218, 165, 32));
		g.fillOval(5, 2, 12, 10);
		g.fillRect(8, 11, 6, 5);
		g.fillRect(6, 16, 10, 3);
		g.setColor(new Color(184, 134, 11));
		g.drawOval(5, 2, 12, 10);
		g.dispose();
		return img;
	}

	private static BufferedImage crossedSwordsImage() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(180, 180, 180));
		g.setStroke(new BasicStroke(2f));
		g.drawLine(3, 13, 13, 3);
		g.drawLine(3, 3, 13, 13);
		g.dispose();
		return img;
	}

	private static BufferedImage barsImage() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(new Color(80, 160, 220));
		g.fillRect(2, 10, 3, 5);
		g.setColor(new Color(80, 200, 90));
		g.fillRect(7, 6, 3, 9);
		g.setColor(new Color(220, 80, 80));
		g.fillRect(12, 3, 3, 12);
		g.dispose();
		return img;
	}

	private static BufferedImage skillImage(int skill) {
		BufferedImage img = new BufferedImage(ICON, ICON, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(70, 70, 70));
		g.fillRoundRect(1, 1, ICON - 2, ICON - 2, 4, 4);
		g.setColor(Color.WHITE);
		g.setFont(new Font("SansSerif", Font.BOLD, 9));
		g.drawString(skillTitle(skill).substring(0, 1), 6, 14);
		g.dispose();
		return img;
	}

	private static final class HoverLabel extends JLabel {
		private static final long serialVersionUID = 1L;
		private final int skill;

		HoverLabel(int skill) {
			this.skill = skill;
			setOpaque(false);
			setToolTipText("hiscore");
		}

		public JToolTip createToolTip() {
			HiscoreTip tip = new HiscoreTip(skill);
			tip.setComponent(this);
			return tip;
		}

		public String getToolTipText() {
			return result != null && result.isVisible() ? "hiscore" : null;
		}
	}

	private static final class SkillCell extends JPanel {
		private static final long serialVersionUID = 1L;
		private final int skill;

		SkillCell(int skill) {
			this.skill = skill;
			setOpaque(false);
			setLayout(new BorderLayout(4, 0));
			setToolTipText("hiscore");
		}

		public JToolTip createToolTip() {
			HiscoreTip tip = new HiscoreTip(skill);
			tip.setComponent(this);
			return tip;
		}

		public String getToolTipText() {
			return result != null && result.isVisible() ? "hiscore" : null;
		}
	}

	private static final class HiscoreTip extends JToolTip {
		private static final long serialVersionUID = 1L;
		private final int skill;

		HiscoreTip(int skill) {
			this.skill = skill;
			setOpaque(false);
		}

		public Dimension getPreferredSize() {
			boolean bar = skill >= 0;
			int lines = skill == -2 ? 2 : 3;
			if (bar) {
				lines = 4;
			}
			return new Dimension(210, 10 + lines * 16 + (bar ? 10 : 0));
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int w = getWidth();
			int h = getHeight();
			g2.setColor(TIP_BG);
			g2.fillRect(0, 0, w, h);
			g2.setColor(TIP_BORDER);
			g2.drawRect(0, 0, w - 1, h - 1);
			g2.setFont(TIP_FONT);
			FontMetrics fm = g2.getFontMetrics();
			int x = 8;
			int y = 16;
			if (skill == -1) {
				drawLine(g2, fm, x, y, "Overall", "");
				y += 16;
				drawLine(g2, fm, x, y, "Rank: ", rankText(lastRank[23]));
				y += 16;
				drawLine(g2, fm, x, y, "Experience: ", formatCount(lastTotalXp));
			} else if (skill == -2) {
				drawLine(g2, fm, x, y, "Combat", "");
				y += 16;
				drawLine(g2, fm, x, y, "Level: ", Integer.toString(lastCombat));
			} else {
				drawLine(g2, fm, x, y, "Skill: ", skillTitle(skill));
				y += 16;
				drawLine(g2, fm, x, y, "Rank: ", rankText(lastRank[skill]));
				y += 16;
				drawLine(g2, fm, x, y, "Experience: ", formatCount(lastXp[skill]));
				y += 16;
				int level = lastLvl[skill];
				int xp = lastXp[skill];
				int start = xpForLevel(level);
				int end = level >= 99 ? start : xpForLevel(level + 1);
				int remain = level >= 99 ? 0 : Math.max(0, end - xp);
				drawLine(g2, fm, x, y, "Remaining XP: ", formatCount(remain));
				y += 8;
				int barX = x;
				int barY = y;
				int barW = w - 16;
				int barH = 5;
				g2.setColor(Color.BLACK);
				g2.fillRect(barX, barY, barW, barH);
				float pct = 1f;
				if (end > start && level < 99) {
					pct = (xp - start) / (float) (end - start);
					if (pct < 0f) {
						pct = 0f;
					}
					if (pct > 1f) {
						pct = 1f;
					}
				}
				g2.setColor(ORANGE);
				g2.fillRect(barX, barY, Math.max(0, (int) (barW * pct)), barH);
			}
			g2.dispose();
		}

		private void drawLine(Graphics2D g2, FontMetrics fm, int x, int y, String label, String value) {
			g2.setColor(TEXT);
			g2.drawString(label, x, y);
			if (value != null && value.length() > 0) {
				g2.setColor(ORANGE);
				g2.drawString(value, x + fm.stringWidth(label), y);
			}
		}
	}

	private static final class SearchGlyph extends JPanel {
		private static final long serialVersionUID = 1L;

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(LIGHT);
			g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.drawOval(7, 7, 10, 10);
			g2.drawLine(16, 16, 20, 20);
			g2.dispose();
		}
	}
}
