import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public final class HudEditor extends JFrame {

	private static final long serialVersionUID = 1L;
	private static HudEditor instance;

		private final SizeRow compass = new SizeRow("Compass", "compass", -2000, 2000, 1, 4096);
		private final SizeRow hp = new SizeRow("HP orb", "hp", -2000, 2000, 1, 4096);
		private final SizeRow prayer = new SizeRow("Prayer", "prayer", -2000, 2000, 1, 4096);
		private final SizeRow run = new SizeRow("Run", "run", -2000, 2000, 1, 4096);
		private final SizeRow world = new SizeRow("World map", "world", -2000, 2000, 1, 4096);
		private final SizeRow frame = new SizeRow("Map frame", "frame", -2000, 2000, 1, 4096);
	private final JSpinner mapX = numberSpinner(0, -2000, 2000);
	private final JSpinner mapY = numberSpinner(0, -2000, 2000);
	private final JCheckBox lockAspect = new JCheckBox("Lock aspect ratio (pixel W/H stay proportional)", true);
	private final JTextArea output = new JTextArea(10, 56);
	private boolean loading;
	private final SizeRow[] rows;

	public static void open() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					HudLayout.get().load();
					if (instance == null) {
						instance = new HudEditor();
					}
					instance.setTitle("Minimap HUD editor (" + HudLayout.get().modeName() + ")");
					instance.reloadFromLayout();
					instance.setVisible(true);
					instance.toFront();
				} catch (RuntimeException e) {
					e.printStackTrace();
					javax.swing.JOptionPane.showMessageDialog(null,
							e.getClass().getName() + ": " + e.getMessage(),
							"HUD editor failed to open",
							javax.swing.JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}

	public static void main(String[] args) {
		open();
	}

	private HudEditor() {
		super("Minimap HUD layout editor");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		rows = new SizeRow[] { compass, hp, prayer, run, world, frame };

		JPanel position = new JPanel(new GridLayout(0, 5, 4, 4));
		position.setBorder(BorderFactory.createTitledBorder("Position (pixels from minimap buffer)"));
		position.add(new JLabel("Sprite"));
		position.add(new JLabel("X"));
		position.add(new JLabel("Y"));
		position.add(new JLabel(""));
		position.add(new JLabel(""));
		for (int i = 0; i < rows.length; i++) {
			position.add(new JLabel(rows[i].name));
			position.add(rows[i].x);
			position.add(rows[i].y);
			position.add(new JLabel(""));
			position.add(new JLabel(""));
		}
		position.add(new JLabel("Minimap terrain"));
		position.add(mapX);
		position.add(mapY);
		position.add(new JLabel(""));
		position.add(new JLabel(""));

		JPanel size = new JPanel(new GridLayout(0, 7, 4, 4));
		size.setBorder(BorderFactory.createTitledBorder("Sprite size (exact pixels, 1px steps)"));
		size.add(new JLabel("Sprite"));
		size.add(new JLabel("Native"));
		size.add(new JLabel("Width px"));
		size.add(new JLabel("Height px"));
		size.add(new JLabel("Scale %"));
		size.add(new JLabel(""));
		size.add(new JLabel(""));
		for (int i = 0; i < rows.length; i++) {
			size.add(new JLabel(rows[i].name));
			size.add(rows[i].nativeLabel);
			size.add(rows[i].w);
			size.add(rows[i].h);
			size.add(rows[i].scale);
			JButton reset = new JButton("Native");
			final SizeRow row = rows[i];
			reset.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					row.resetNative();
					pushToLayout();
					refreshSnippet();
				}
			});
			size.add(reset);
			size.add(new JLabel(""));
		}

		JPanel top = new JPanel(new BorderLayout(6, 6));
		top.add(position, BorderLayout.NORTH);
		JPanel sizeWrap = new JPanel(new BorderLayout());
		sizeWrap.add(lockAspect, BorderLayout.NORTH);
		sizeWrap.add(size, BorderLayout.CENTER);
		top.add(sizeWrap, BorderLayout.CENTER);

		JPanel buttons = new JPanel();
		JButton apply = new JButton("Apply live");
		JButton save = new JButton("Save file");
		JButton reload = new JButton("Reload file");
		JButton copy = new JButton("Copy Java");
		apply.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pushToLayout();
				refreshSnippet();
			}
		});
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pushToLayout();
				HudLayout.get().save();
				refreshSnippet();
			}
		});
		reload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				HudLayout.get().load();
				reloadFromLayout();
			}
		});
		copy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pushToLayout();
				refreshSnippet();
				Toolkit.getDefaultToolkit().getSystemClipboard()
						.setContents(new StringSelection(output.getText()), null);
			}
		});
		buttons.add(apply);
		buttons.add(save);
		buttons.add(reload);
		buttons.add(copy);

		output.setEditable(false);
		JPanel south = new JPanel(new BorderLayout());
		south.add(buttons, BorderLayout.NORTH);
		south.add(new JScrollPane(output), BorderLayout.CENTER);

		getContentPane().setLayout(new BorderLayout(8, 8));
		getContentPane().add(top, BorderLayout.NORTH);
		getContentPane().add(south, BorderLayout.CENTER);

		ChangeListener live = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (loading) {
					return;
				}
				Object source = e.getSource();
				for (int i = 0; i < rows.length; i++) {
					if (rows[i].owns(source)) {
						rows[i].onSpinner(source, lockAspect.isSelected());
						break;
					}
				}
				pushToLayout();
				refreshSnippet();
			}
		};
		for (int i = 0; i < rows.length; i++) {
			rows[i].listen(live);
		}
		mapX.addChangeListener(live);
		mapY.addChangeListener(live);
		reloadFromLayout();
		setPreferredSize(new Dimension(980, 720));
		pack();
		setLocationRelativeTo(null);
	}

	private static int clamp(int value, int min, int max) {
		if (value < min) {
			return min;
		}
		if (value > max) {
			return max;
		}
		return value;
	}

	private static JSpinner spinner(int min, int max) {
		return numberSpinner(min > 0 ? min : 0, min, max);
	}

	private static JSpinner numberSpinner(int value, int min, int max) {
		if (min > max) {
			int swap = min;
			min = max;
			max = swap;
		}
		value = clamp(value, min, max);
		return new JSpinner(new SpinnerNumberModel(value, min, max, 1));
	}

	private static void setSpinnerValue(JSpinner spinner, int value) {
		SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
		int min = ((Number) model.getMinimum()).intValue();
		int max = ((Number) model.getMaximum()).intValue();
		spinner.setValue(Integer.valueOf(clamp(value, min, max)));
	}

	private void reloadFromLayout() {
		loading = true;
		try {
			HudLayout h = HudLayout.get();
			compass.load(h.compassX, h.compassY, h.compassW, h.compassH);
			hp.load(h.hpOrbX, h.hpOrbY, h.hpOrbW, h.hpOrbH);
			prayer.load(h.prayerOrbX, h.prayerOrbY, h.prayerOrbW, h.prayerOrbH);
			run.load(h.runOrbX, h.runOrbY, h.runOrbW, h.runOrbH);
			world.load(h.worldMapX, h.worldMapY, h.worldMapW, h.worldMapH);
			frame.load(h.mapFrameX, h.mapFrameY, h.mapFrameW, h.mapFrameH);
			setSpinnerValue(mapX, h.minimapX);
			setSpinnerValue(mapY, h.minimapY);
			refreshSnippet();
		} finally {
			loading = false;
		}
	}

	private void pushToLayout() {
		HudLayout h = HudLayout.get();
		h.compassX = compass.xValue();
		h.compassY = compass.yValue();
		h.compassW = compass.wToSave();
		h.compassH = compass.hToSave();
		h.hpOrbX = hp.xValue();
		h.hpOrbY = hp.yValue();
		h.hpOrbW = hp.wToSave();
		h.hpOrbH = hp.hToSave();
		h.prayerOrbX = prayer.xValue();
		h.prayerOrbY = prayer.yValue();
		h.prayerOrbW = prayer.wToSave();
		h.prayerOrbH = prayer.hToSave();
		h.runOrbX = run.xValue();
		h.runOrbY = run.yValue();
		h.runOrbW = run.wToSave();
		h.runOrbH = run.hToSave();
		h.worldMapX = world.xValue();
		h.worldMapY = world.yValue();
		h.worldMapW = world.wToSave();
		h.worldMapH = world.hToSave();
		h.mapFrameX = frame.xValue();
		h.mapFrameY = frame.yValue();
		h.mapFrameW = frame.wToSave();
		h.mapFrameH = frame.hToSave();
		h.minimapX = ((Integer) mapX.getValue()).intValue();
		h.minimapY = ((Integer) mapY.getValue()).intValue();
	}

	private void refreshSnippet() {
		HudLayout h = HudLayout.get();
		output.setText("Mode: " + h.modeName()
				+ "\nSizes are exact pixels (1px spinner steps). Scale % converts to whole pixels."
				+ "\nFile:\n" + h.propertiesFile().getAbsolutePath()
				+ "\n\nJava:\n" + h.toJavaSnippet());
	}

	private final class SizeRow {
		final String name;
		final String id;
		final JSpinner x;
		final JSpinner y;
		final JSpinner w;
		final JSpinner h;
		final JSpinner scale;
		final JLabel nativeLabel = new JLabel("?x?");
		private boolean updating;
		private boolean sizeEdited;
		private int savedW;
		private int savedH;

		SizeRow(String name, String id, int posMin, int posMax, int sizeMin, int sizeMax) {
			this.name = name;
			this.id = id;
			x = numberSpinner(0, posMin, posMax);
			y = numberSpinner(0, posMin, posMax);
			w = numberSpinner(sizeMin, sizeMin, sizeMax);
			h = numberSpinner(sizeMin, sizeMin, sizeMax);
			scale = numberSpinner(100, 1, 2000);
		}

		boolean owns(Object source) {
			return source == x || source == y || source == w || source == h || source == scale;
		}

		void listen(ChangeListener live) {
			x.addChangeListener(live);
			y.addChangeListener(live);
			w.addChangeListener(live);
			h.addChangeListener(live);
			scale.addChangeListener(live);
		}

		void load(int px, int py, int pw, int ph) {
			updating = true;
			savedW = pw;
			savedH = ph;
			sizeEdited = false;
			setSpinnerValue(x, px);
			setSpinnerValue(y, py);
			int nw = nativeW();
			int nh = nativeH();
			nativeLabel.setText(nw + " x " + nh + " px");
			if (pw < 1) {
				pw = nw;
			}
			if (ph < 1) {
				ph = nh;
			}
			setSpinnerValue(w, pw);
			setSpinnerValue(h, ph);
			setSpinnerValue(scale, Math.max(1, pw * 100 / Math.max(1, nw)));
			updating = false;
		}

		void resetNative() {
			updating = true;
			sizeEdited = true;
			savedW = 0;
			savedH = 0;
			int nw = nativeW();
			int nh = nativeH();
			setSpinnerValue(w, nw);
			setSpinnerValue(h, nh);
			setSpinnerValue(scale, 100);
			updating = false;
		}

		void onSpinner(Object source, boolean lock) {
			if (updating) {
				return;
			}
			if (source == w || source == h || source == scale) {
				sizeEdited = true;
			}
			updating = true;
			try {
				int nw = nativeW();
				int nh = nativeH();
				if (source == scale) {
					int pct = ((Integer) scale.getValue()).intValue();
					setSpinnerValue(w, Math.max(1, nw * pct / 100));
					setSpinnerValue(h, Math.max(1, nh * pct / 100));
				} else if (source == w && lock) {
					int width = ((Integer) w.getValue()).intValue();
					setSpinnerValue(h, Math.max(1, width * nh / Math.max(1, nw)));
					setSpinnerValue(scale, Math.max(1, width * 100 / Math.max(1, nw)));
				} else if (source == h && lock) {
					int height = ((Integer) h.getValue()).intValue();
					setSpinnerValue(w, Math.max(1, height * nw / Math.max(1, nh)));
					setSpinnerValue(scale, Math.max(1, height * 100 / Math.max(1, nh)));
				} else if (source == w || source == h) {
					int width = ((Integer) w.getValue()).intValue();
					setSpinnerValue(scale, Math.max(1, width * 100 / Math.max(1, nw)));
				}
			} finally {
				updating = false;
			}
		}

		int xValue() {
			return ((Integer) x.getValue()).intValue();
		}

		int yValue() {
			return ((Integer) y.getValue()).intValue();
		}

		int wToSave() {
			if (!sizeEdited) {
				return savedW;
			}
			int width = wValue();
			if (width == nativeW() && hValue() == nativeH()) {
				return 0;
			}
			return width;
		}

		int hToSave() {
			if (!sizeEdited) {
				return savedH;
			}
			int height = hValue();
			if (wValue() == nativeW() && height == nativeH()) {
				return 0;
			}
			return height;
		}

		int wValue() {
			return ((Integer) w.getValue()).intValue();
		}

		int hValue() {
			return ((Integer) h.getValue()).intValue();
		}

		private int nativeW() {
			if (client.instance != null) {
				return client.instance.hudNativeWidth(id);
			}
			return "compass".equals(id) ? 33 : 1;
		}

		private int nativeH() {
			if (client.instance != null) {
				return client.instance.hudNativeHeight(id);
			}
			return "compass".equals(id) ? 33 : 1;
		}
	}
}
