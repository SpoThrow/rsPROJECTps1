import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Visual editor for the creation chatbox's layout - the row of Make 1 / 5 / 10 / All pills and the
 * product boxes beneath them.
 *
 * <p><b>Why this exists, and why it is not part of the client.</b> The layout in {@code MakeX} is a
 * couple of dozen pixel constants, and tuning those by describing a screenshot in words is slow and
 * error prone in both directions - the dialogue chat heads went 25% the wrong way for exactly this
 * reason. This tool draws the panel at 2x with a draggable box for each thing {@code MakeX} places,
 * and writes the numbers back into the source when a box is dropped. Nothing here is a second
 * layout engine: it reads the constants out of {@code MakeX.java} by name and writes the same names
 * back, so the source file stays the single source of truth and the tool cannot drift away from it.
 *
 * <p>It has no dependency on the client at all - no cache, no {@code client}, no {@code Sprite} - so
 * it compiles and runs on its own from {@code EditChatbox.bat}. The panel background art is loaded
 * straight off disk as a plain image and is only ever decoration; every box comes from a fresh read
 * of the source, so a missing sprite degrades the picture and nothing else.
 *
 * <p><b>What cannot be dragged, and why.</b> {@code QUANT_RIGHT} is not a number in the source, it
 * is {@code = CLICK_RIGHT} - the same value the dialog's own hitbox gate uses - so the pill block's
 * right edge is tied to the clickable region on purpose and that row only moves vertically.
 * {@code CLICK_BOTTOM} is a reference in the same way. Both are listed as readouts and neither is
 * editable here. Everything else is drag-or-type.
 */
public final class ChatboxEditor extends JFrame {

	private static final long serialVersionUID = 1L;

	/** The panel is authored at 519x142 and drawn scaled by this, so its text stays clickable. */
	private static final int DEFAULT_ZOOM = 2;

	private static final String DEFAULT_SOURCE = "src/ui/MakeX.java";

	/**
	 * How far a number's spinner reaches either side of the value it loaded.
	 *
	 * <p><b>Why the range follows the value rather than being one fixed pair.</b> This tool reads
	 * every {@code int} declaration in the file, and not all of them are pixel offsets: the widget
	 * ids are in there too, and {@code HELP_FIRST} is 51920. A fixed -5000..5000 range threw
	 * {@code IllegalArgumentException} out of {@code SpinnerNumberModel}'s constructor the moment it
	 * was handed one, so the editor would not start at all. A window around whatever was read cannot
	 * disagree with the file, and a hundred thousand either side of a layout constant is room to
	 * type a typo in without it being the maximum that catches it.
	 */
	private static final int VALUE_SPAN = 100000;

	/** Matches what the writer expects: one declaration per line, as MakeX documents it must stay. */
	private static final Pattern DECL = Pattern.compile(
			"(private\\s+static\\s+(?:final\\s+)?int\\s+)([A-Za-z_][A-Za-z0-9_]*)(\\s*=\\s*)(-?\\d+)(\\s*;)");

	/** Draw order and the order they are listed in, which is the file's own order. */
	private static final String[] READOUTS = { "WIDTH", "HEIGHT", "CLICK_LEFT", "CLICK_RIGHT",
			"CLICK_TOP", "CLICK_BOTTOM" };

	private static final String[] CAPTION = { "What would you like to make?" };
	private static final String[] HELP = { "Choose a quantity, then click an item." };
	private static final String[] PILLS = { "1", "5", "10", "All" };

	private final File source;
	private final File spriteDir;

	private final Map<String, Integer> values = new LinkedHashMap<String, Integer>();
	private final Map<String, JSpinner> spinners = new LinkedHashMap<String, JSpinner>();

	private final Canvas canvas = new Canvas();
	private final JPanel spinnerTable = new JPanel(new GridBagLayout());
	private final JTextArea status = new JTextArea(8, 60);
	private final JCheckBox autoWrite = new JCheckBox("Write MakeX.java as soon as a box is dropped", true);
	private int zoom = DEFAULT_ZOOM;

	private String dragTarget;
	private int dragStartX;
	private int dragStartY;
	private int dragOriginA;
	private int dragOriginB;

	private Image backgroundImage;
	private boolean backgroundTried;
	private boolean loading;

	public static void main(String[] args) {
		String src = args.length > 0 ? args[0] : DEFAULT_SOURCE;
		String sprites = args.length > 1 ? args[1]
				: new File(System.getProperty("user.home"), "Biohazard.474" + File.separator + "Sprites").getPath();
		ChatboxEditor editor = new ChatboxEditor(new File(src), new File(sprites));
		editor.setVisible(true);
	}

	private ChatboxEditor(File source, File spriteDir) {
		super("Creation chatbox layout editor - " + source.getName());
		this.source = source;
		this.spriteDir = spriteDir;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel right = new JPanel();
		right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
		right.add(headerPanel());
		spinnerTable.setBorder(BorderFactory.createTitledBorder("Values (drag a box, or type a number)"));
		right.add(spinnerTable);
		right.add(Box.createVerticalStrut(6));
		right.add(buttonPanel());
		right.add(autoWrite);
		right.add(Box.createVerticalStrut(6));
		status.setEditable(false);
		status.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
		right.add(new JScrollPane(status));

		getContentPane().setLayout(new BorderLayout(8, 8));
		getContentPane().add(canvas, BorderLayout.CENTER);
		JScrollPane rightScroll = new JScrollPane(right);
		rightScroll.setPreferredSize(new Dimension(440, 720));
		getContentPane().add(rightScroll, BorderLayout.EAST);

		canvas.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				startDrag(e);
			}

			public void mouseReleased(MouseEvent e) {
				finishDrag();
			}
		});
		canvas.addMouseMotionListener(new MouseAdapter() {
			public void mouseDragged(MouseEvent e) {
				continueDrag(e);
			}
		});

		loadFromSource();
		pack();
		setLocationRelativeTo(null);
	}

	private JPanel headerPanel() {
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(BorderFactory.createTitledBorder("Source"));
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 2, 2, 2);
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.weightx = 1;
		JTextField path = new JTextField(source.getPath(), 24);
		path.setEditable(false);
		p.add(path, c);
		c.gridwidth = 1;
		c.gridy = 1;
		c.weightx = 0;
		p.add(new JLabel("Zoom"), c);
		c.gridx = 1;
		final JSpinner z = new JSpinner(new SpinnerNumberModel(zoom, 1, 5, 1));
		z.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				zoom = ((Integer) z.getValue()).intValue();
				canvas.revalidate();
				canvas.repaint();
			}
		});
		p.add(z, c);
		return p;
	}

	private JPanel buttonPanel() {
		JPanel p = new JPanel();
		JButton save = new JButton("Save to MakeX.java");
		JButton reload = new JButton("Reload from file");
		JButton compile = new JButton("Compile client");
		JButton copy = new JButton("Copy values");
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				writeSource();
			}
		});
		reload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadFromSource();
			}
		});
		compile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				compile();
			}
		});
		copy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
						.setContents(new java.awt.datatransfer.StringSelection(values.toString()), null);
				say("Copied " + values.size() + " values to the clipboard.");
			}
		});
		p.add(save);
		p.add(reload);
		p.add(compile);
		p.add(copy);
		return p;
	}

	private void say(String line) {
		status.append(line);
		status.append("\n");
		status.setCaretPosition(status.getDocument().getLength());
	}

	/* -------------------------------------------------------------- source io */

	private void loadFromSource() {
		values.clear();
		if (!source.isFile()) {
			say("!! " + source.getPath() + " not found.");
			say("   Run EditChatbox.bat from the Proxy Client folder, or pass MakeX.java as argument 1.");
			rebuildSpinners();
			return;
		}
		String text;
		try {
			text = read(source);
		} catch (IOException e) {
			say("!! could not read " + source.getPath() + ": " + e);
			return;
		}
		Matcher m = DECL.matcher(text);
		while (m.find()) {
			String name = m.group(2);
			if (!values.containsKey(name)) {
				values.put(name, Integer.valueOf(m.group(4)));
			}
		}
		rebuildSpinners();
		say("Loaded " + values.size() + " layout values from " + source.getPath());
		canvas.revalidate();
		canvas.repaint();
	}

	/**
	 * Rebuilds the number table from {@link #values}, readouts included.
	 *
	 * <p>A readout gets a label rather than a spinner, so a name that is a reference to another
	 * constant says so instead of offering a number this tool would then write over the reference.
	 */
	private void rebuildSpinners() {
		loading = true;
		spinnerTable.removeAll();
		spinners.clear();
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(1, 3, 1, 3);
		c.anchor = GridBagConstraints.WEST;
		int row = 0;
		for (final Map.Entry<String, Integer> e : values.entrySet()) {
			final String name = e.getKey();
			c.gridy = row++;
			c.gridx = 0;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			spinnerTable.add(new JLabel(name), c);
			c.gridx = 1;
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			final JSpinner s = new JSpinner(new SpinnerNumberModel(
					e.getValue().intValue(), e.getValue().intValue() - VALUE_SPAN,
					e.getValue().intValue() + VALUE_SPAN, 1));
			s.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent ev) {
					if (loading) {
						return;
					}
					int v = ((Integer) s.getValue()).intValue();
					if (values.get(name).intValue() == v) {
						return;
					}
					values.put(name, Integer.valueOf(v));
					canvas.repaint();
				}
			});
			spinners.put(name, s);
			spinnerTable.add(s, c);
		}
		for (int i = 0; i < READOUTS.length; i++) {
			if (values.containsKey(READOUTS[i])) {
				continue;
			}
			// A reference, so it has no number to offer; shown so the panel's own bounds are visible.
			c.gridy = row++;
			c.gridx = 0;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			spinnerTable.add(new JLabel(READOUTS[i]), c);
			c.gridx = 1;
			JLabel ref = new JLabel("reference to another constant");
			ref.setForeground(Color.GRAY);
			spinnerTable.add(ref, c);
		}
		loading = false;
		spinnerTable.revalidate();
		spinnerTable.repaint();
	}

	/** Rewrites every matched declaration in place, leaving the rest of the file byte for byte. */
	private void writeSource() {
		if (!source.isFile()) {
			say("!! nothing to write to.");
			return;
		}
		String text;
		try {
			text = read(source);
		} catch (IOException e) {
			say("!! could not read " + source.getPath() + ": " + e);
			return;
		}
		StringBuilder out = new StringBuilder(text.length());
		Matcher m = DECL.matcher(text);
		int written = 0;
		int last = 0;
		while (m.find()) {
			out.append(text, last, m.start());
			Integer v = values.get(m.group(2));
			out.append(m.group(1)).append(m.group(2)).append(m.group(3))
					.append(v != null ? v.toString() : m.group(4)).append(m.group(5));
			last = m.end();
			written++;
		}
		out.append(text, last, text.length());
		if (written == 0) {
			say("!! no declarations matched; MakeX.java was left alone.");
			return;
		}
		try {
			write(source, out.toString());
		} catch (IOException e) {
			say("!! could not write " + source.getPath() + ": " + e);
			return;
		}
		say("Wrote " + written + " declarations to " + source.getPath());
	}

	private void compile() {
		final File dir = source.getAbsoluteFile().getParentFile().getParentFile();
		say("Compiling in " + dir.getPath() + " ...");
		new Thread(new Runnable() {
			public void run() {
				try {
					Process p = new ProcessBuilder("cmd", "/c", "gradlew.bat", "compileJava")
							.directory(dir).redirectErrorStream(true).start();
					final String output = drain(p);
					final int code = p.waitFor();
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							say(code == 0 ? "Compile OK." : "Compile FAILED (exit " + code + ").");
							say(tail(output, code == 0 ? 6 : 40));
						}
					});
				} catch (final Exception e) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							say("!! compile failed: " + e);
						}
					});
				}
			}
		}, "chatbox-editor-compile").start();
	}

	private static String drain(Process p) throws IOException {
		InputStream in = p.getInputStream();
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		byte[] chunk = new byte[4096];
		int n;
		while ((n = in.read(chunk)) > 0) {
			buf.write(chunk, 0, n);
		}
		return new String(buf.toByteArray());
	}

	private static String tail(String text, int lines) {
		String[] all = text.split("\r?\n");
		int from = Math.max(0, all.length - lines);
		StringBuilder b = new StringBuilder();
		for (int i = from; i < all.length; i++) {
			b.append(all[i]).append('\n');
		}
		return b.toString();
	}

	/* ------------------------------------------------------------- the canvas */

	private int v(String name, int fallback) {
		Integer i = values.get(name);
		return i != null ? i.intValue() : fallback;
	}

	/** One draggable (or purely informative) thing: an id for the drag, a box, and a caption. */
	private final class Cell {
		String target;
		String label;
		int x;
		int y;
		int w;
		int h;
		int a;
		int b;
	}

	private List<Cell> boxes() {
		List<Cell> out = new ArrayList<Cell>();
		int clickLeft = v("CLICK_LEFT", 20);
		int clickRight = v("CLICK_RIGHT", 490);

		Cell title = new Cell();
		title.target = "TITLE_Y";
		title.label = "question   TITLE_X=" + v("TITLE_X", 4) + "  TITLE_Y=" + v("TITLE_Y", 22);
		title.x = v("TITLE_X", 4);
		title.y = v("TITLE_Y", 22);
		title.w = Math.max(80, CAPTION[0].length() * 5);
		title.h = v("TITLE_H", 18);
		title.a = v("TITLE_X", 4);
		title.b = v("TITLE_Y", 22);
		out.add(title);

		Cell help = new Cell();
		help.target = "HELP_Y";
		help.label = "help line   x follows the question   HELP_Y=" + v("HELP_Y", 43);
		help.x = v("TITLE_X", 4);
		help.y = v("HELP_Y", 43);
		help.w = Math.max(80, HELP[0].length() * 5);
		help.h = v("HELP_H", 12);
		help.a = v("TITLE_X", 4);
		help.b = v("HELP_Y", 43);
		out.add(help);

		int pillW = v("QUANT_W", 28);
		int gap = v("QUANT_GAP", 9);
		int rowW = PILLS.length * (pillW + gap) - gap;
		Cell pills = new Cell();
		pills.target = "QUANT_Y";
		pills.label = "amount pills   1 / 5 / 10 / All   vertical only (QUANT_RIGHT is CLICK_RIGHT)   QUANT_Y="
				+ v("QUANT_Y", 22);
		pills.x = clickRight - rowW;
		pills.y = v("QUANT_Y", 22);
		pills.w = rowW;
		pills.h = v("QUANT_H", 31);
		pills.a = v("QUANT_Y", 22);
		out.add(pills);

		int slotW = v("SLOT_W", 64);
		int slotH = v("SLOT_H", 82);
		int slotTop = v("SLOT_TOP", 52);
		int slotGap = v("SLOT_GAP", 6);
		for (int i = 0; i < 2; i++) {
			Cell slot = new Cell();
			slot.target = "SLOT_TOP";
			slot.label = "product slot " + (i + 1) + "   " + slotW + "x" + slotH + "   SLOT_TOP=" + slotTop;
			slot.x = clickLeft + i * (slotW + slotGap);
			slot.y = slotTop;
			slot.w = slotW;
			slot.h = slotH;
			slot.a = slotTop;
			out.add(slot);

			Cell label = new Cell();
			// No constant owns the gap between a slot and its name, so this box is a readout:
			// dragging it does nothing rather than quietly editing a number that means something
			// else. LABEL_H is the label's own height and is in the table.
			label.target = null;
			label.label = "name label   LABEL_H=" + v("LABEL_H", 12);
			label.x = slot.x;
			label.y = slotTop + slotH;
			label.w = slotW;
			label.h = v("LABEL_H", 12);
			out.add(label);
		}
		return out;
	}

	private final class Canvas extends JPanel {
		private static final long serialVersionUID = 1L;

		Canvas() {
			setBackground(new Color(0x303030));
			setPreferredSize(new Dimension(519 * DEFAULT_ZOOM + 20, 142 * DEFAULT_ZOOM + 40));
		}

		protected void paintComponent(Graphics g0) {
			super.paintComponent(g0);
			Graphics2D g = (Graphics2D) g0;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int ox = 10;
			int oy = 24;

			int w = v("WIDTH", 519);
			int h = v("HEIGHT", 142);
			g.setColor(new Color(0x1A1208));
			g.fillRect(ox, oy, w * zoom, h * zoom);
			Image art = background();
			if (art != null) {
				g.drawImage(art, ox, oy, w * zoom, h * zoom, null);
			}
			g.setColor(new Color(0x6B5A3C));
			g.drawRect(ox, oy, w * zoom, h * zoom);
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
			g.setColor(Color.LIGHT_GRAY);
			g.drawString("panel " + w + "x" + h + "   zoom " + zoom + "x", ox, oy - 8);

			// The region the client builds a dialog's hitboxes in: a widget outside it is drawn but
			// dead, which is what CLICK_LEFT/RIGHT/TOP/BOTTOM exist to record.
			int cl = v("CLICK_LEFT", 20);
			int cr = v("CLICK_RIGHT", 490);
			int ct = v("CLICK_TOP", 20);
			int cb = v("CLICK_BOTTOM", h);
			g.setColor(new Color(0x50, 0xA0, 0x50));
			g.drawRect(ox + cl * zoom, oy + ct * zoom, (cr - cl) * zoom, (cb - ct) * zoom);

			drawIconPreview(g, ox, oy);

			for (Cell b : boxes()) {
				boolean hot = b.target != null && b.target.equals(dragTarget);
				g.setColor(hot ? new Color(0xFF, 0xE1, 0x4A, 200) : new Color(0x4A, 0xC8, 0xFF, 170));
				g.fillRect(ox + b.x * zoom, oy + b.y * zoom, b.w * zoom, b.h * zoom);
				g.setColor(hot ? new Color(0xFF, 0xE1, 0x4A) : new Color(0x00, 0xCC, 0xFF));
				g.setStroke(new BasicStroke(1f));
				g.drawRect(ox + b.x * zoom, oy + b.y * zoom, b.w * zoom, b.h * zoom);
				g.setColor(Color.WHITE);
				g.drawString(b.label, ox + b.x * zoom, oy + b.y * zoom - 3);
			}

			g.setColor(Color.LIGHT_GRAY);
			g.drawString("Drag a box to edit the constants it names; the green rect is the clickable region.",
					ox, oy + h * zoom + 16);
		}

		/**
		 * The product icon's size, drawn inside the first slot at the scale the zoom fraction asks
		 * for.
		 *
		 * <p>Anchored to the reference {@code MakeX} documents rather than to a guess: 3/8 of the
		 * item's own {@code modelZoom} is the value the source says makes an icon span the Cell
		 * height, so this circle is {@code min(SLOT_W, SLOT_H) * (NUM/DEN) / (3/8)} across and its
		 * edge touching the slot's is the "fills the container" the layout was aiming for - read
		 * against the fraction it stands at now, which is a step below it by request.
		 */
		private void drawIconPreview(Graphics2D g, int ox, int oy) {
			int num = v("ICON_ZOOM_NUM", 15);
			int den = Math.max(1, v("ICON_ZOOM_DEN", 28));
			int slotTop = v("SLOT_TOP", 52);
			int clickLeft = v("CLICK_LEFT", 20);
			int slotW = v("SLOT_W", 64);
			int slotH = v("SLOT_H", 82);
			double reference = 3.0 / 8.0;
			double scale = (num / (double) den) / reference;
			if (scale < 0.05) {
				scale = 0.05;
			}
			if (scale > 3.0) {
				scale = 3.0;
			}
			int side = (int) Math.round(Math.min(slotW, slotH) * scale);
			int cx = ox + (clickLeft + slotW / 2) * zoom;
			int cy = oy + (slotTop + slotH / 2) * zoom;
			int half = Math.max(1, side * zoom / 2);
			g.setColor(new Color(0xFF, 0xE1, 0x4A, 90));
			g.fillOval(cx - half, cy - half, half * 2, half * 2);
			g.setColor(new Color(0xFF, 0xE1, 0x4A));
			g.drawOval(cx - half, cy - half, half * 2, half * 2);
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
			g.drawString("icon = " + side + "px in a " + slotH + "px slot   (" + num + "/" + den + ")",
					cx - half, cy - half - 3);
		}
	}

	/** The chat panel's own art, if this tool was pointed at a cache that has it. Decoration only. */
	private Image background() {
		if (!backgroundTried) {
			backgroundTried = true;
			try {
				File f = new File(spriteDir, "ItemCraftingChatInterface" + File.separator + "Chatboxbackground.png");
				if (f.isFile()) {
					backgroundImage = ImageIO.read(f);
				}
			} catch (Exception e) {
				backgroundImage = null;
			}
		}
		return backgroundImage;
	}

	/* ---------------------------------------------------------------- dragging */

	private void startDrag(MouseEvent e) {
		int px = (e.getX() - 10) / zoom;
		int py = (e.getY() - 24) / zoom;
		for (Cell b : boxes()) {
			if (b.target == null) {
				continue;
			}
			if (px >= b.x && py >= b.y && px < b.x + b.w && py < b.y + b.h) {
				dragTarget = b.target;
				dragStartX = px;
				dragStartY = py;
				dragOriginA = b.a;
				dragOriginB = b.b;
				canvas.repaint();
				return;
			}
		}
		dragTarget = null;
	}

	private void continueDrag(MouseEvent e) {
		if (dragTarget == null) {
			return;
		}
		int px = (e.getX() - 10) / zoom;
		int py = (e.getY() - 24) / zoom;
		int dx = px - dragStartX;
		int dy = py - dragStartY;
		if ("TITLE_Y".equals(dragTarget)) {
			set("TITLE_X", dragOriginA + dx);
			set("TITLE_Y", dragOriginB + dy);
		} else if ("HELP_Y".equals(dragTarget)) {
			// The question and the help line share their x in the source, so a horizontal drag on
			// either moves both. Saying so on the box is cheaper than two constants that disagree.
			set("TITLE_X", dragOriginA + dx);
			set("HELP_Y", dragOriginB + dy);
		} else if ("QUANT_Y".equals(dragTarget)) {
			set("QUANT_Y", dragOriginA + dy);
		} else if ("SLOT_TOP".equals(dragTarget)) {
			set("SLOT_TOP", dragOriginA + dy);
		}
		canvas.repaint();
	}

	private void finishDrag() {
		if (dragTarget == null) {
			return;
		}
		dragTarget = null;
		if (autoWrite.isSelected()) {
			writeSource();
		}
		canvas.repaint();
	}

	private void set(String name, int value) {
		Integer current = values.get(name);
		if (current == null || current.intValue() == value) {
			return;
		}
		values.put(name, Integer.valueOf(value));
		JSpinner s = spinners.get(name);
		if (s != null) {
			loading = true;
			s.setValue(Integer.valueOf(value));
			loading = false;
		}
	}

	/* ----------------------------------------------------------------- file io */

	private static String read(File f) throws IOException {
		FileInputStream in = new FileInputStream(f);
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			byte[] chunk = new byte[8192];
			int n;
			while ((n = in.read(chunk)) > 0) {
				buf.write(chunk, 0, n);
			}
			return new String(buf.toByteArray(), "UTF-8");
		} finally {
			in.close();
		}
	}

	private static void write(File f, String text) throws IOException {
		FileOutputStream out = new FileOutputStream(f);
		try {
			out.write(text.getBytes("UTF-8"));
		} finally {
			out.close();
		}
	}
}
