package ui;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;import game.client;



/**
 * RuneLite-style dark title bar with sidebar chevron, min/max/close.
 */
final class TitleBar extends JPanel {

	static final int HEIGHT = 32;
	static final int BTN_W = 46;
	static final int SIDE_W = 32;

	private static final Color BG = new Color(30, 30, 30);
	private static final Color TEXT = new Color(198, 198, 198);
	private static final Color ICON = new Color(165, 165, 165);
	private static final Color HOVER = new Color(50, 50, 50);
	private static final Color CLOSE_HOVER = new Color(232, 17, 35);
	private static final Color BORDER = new Color(23, 23, 23);
	private static final Font TITLE_FONT = new Font("SansSerif", Font.PLAIN, 12);

	private static TitleBar instance;
	private static JLabel title;
	private static SidebarButton sidebarBtn;
	private static WinButton maxBtn;
	private static Rectangle restoreBounds;
	private static boolean maximized;
	private static boolean dragging;
	private static int dragOffsetX;
	private static int dragOffsetY;
	private static int resizeEdge;
	private static Rectangle resizeStart;
	private static Point resizeAnchor;
	private static final int EDGE_PAD = 8;

	static void install(JFrame frame) {
		frame.setUndecorated(true);
		frame.getRootPane().setBorder(BorderFactory.createLineBorder(BORDER, 1));
		frame.getContentPane().setBackground(BG);
		instance = new TitleBar();
		frame.getContentPane().add(instance, BorderLayout.NORTH);
		raise(frame);
		frame.setMaximizedBounds(workingArea(frame));
		installResize(frame);
	}

	static void raise(JFrame frame) {
		if (instance != null && frame != null) {
			frame.getContentPane().setComponentZOrder(instance, 0);
		}
	}

	static int barHeight() {
		return instance == null ? 0 : HEIGHT;
	}

	static void sync() {
		if (title != null) {
			title.setText(PluginSidebar.windowTitle());
		}
		if (sidebarBtn != null) {
			sidebarBtn.repaint();
		}
		if (maxBtn != null) {
			maxBtn.setToolTipText(isMaximized() ? "Restore" : "Maximize");
			maxBtn.repaint();
		}
	}

	static boolean isMaximized() {
		if (maximized) {
			return true;
		}
		JFrame frame = Jframe.getFrame();
		return frame != null && (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
	}

	TitleBar() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBackground(BG);
		setOpaque(true);
		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

		title = new JLabel("Soul-Trail");
		title.setForeground(TEXT);
		title.setFont(TITLE_FONT);
		title.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 8));
		title.setAlignmentY(0.5f);
		add(title);
		add(Box.createHorizontalGlue());

		sidebarBtn = new SidebarButton();
		add(sidebarBtn);
		add(new WinButton(WinButton.MIN));
		maxBtn = new WinButton(WinButton.MAX);
		add(maxBtn);
		add(new WinButton(WinButton.CLOSE));

		MouseAdapter drag = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (!SwingUtilities.isLeftMouseButton(e)) {
					return;
				}
				JFrame frame = Jframe.getFrame();
				if (frame == null) {
					return;
				}
				Point local = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), frame);
				if (!isMaximized() && edgeAtPoint(frame, local) != 0) {
					return;
				}
				dragging = true;
				Point loc = frame.getLocationOnScreen();
				dragOffsetX = e.getXOnScreen() - loc.x;
				dragOffsetY = e.getYOnScreen() - loc.y;
			}

			public void mouseReleased(MouseEvent e) {
				dragging = false;
			}

			public void mouseDragged(MouseEvent e) {
				if (!dragging) {
					return;
				}
				JFrame frame = Jframe.getFrame();
				if (frame == null) {
					return;
				}
				if (isMaximized()) {
					if (Math.abs(e.getXOnScreen() - (frame.getX() + dragOffsetX)) < 8
							&& Math.abs(e.getYOnScreen() - (frame.getY() + dragOffsetY)) < 8) {
						return;
					}
					Rectangle next = usableRestore(frame, restoreBounds);
					int x = e.getXOnScreen() - Math.min(dragOffsetX, next.width - 24);
					int y = e.getYOnScreen() - Math.min(dragOffsetY, HEIGHT - 4);
					restoreFromMaximize(frame, new Rectangle(x, y, next.width, next.height));
					dragOffsetX = e.getXOnScreen() - frame.getX();
					dragOffsetY = e.getYOnScreen() - frame.getY();
				}
				frame.setLocation(e.getXOnScreen() - dragOffsetX, e.getYOnScreen() - dragOffsetY);
			}

			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
					toggleMaximize();
				}
			}
		};
		addMouseListener(drag);
		addMouseMotionListener(drag);
		title.addMouseListener(drag);
		title.addMouseMotionListener(drag);
	}

	public Dimension getPreferredSize() {
		return new Dimension(200, HEIGHT);
	}

	public Dimension getMinimumSize() {
		return new Dimension(200, HEIGHT);
	}

	public Dimension getMaximumSize() {
		return new Dimension(Integer.MAX_VALUE, HEIGHT);
	}

	static void toggleMaximize() {
		JFrame frame = Jframe.getFrame();
		if (frame == null) {
			return;
		}
		if (isMaximized()) {
			restoreFromMaximize(frame, usableRestore(frame, restoreBounds));
		} else {
			restoreBounds = usableRestore(frame, frame.getBounds());
			Rectangle us = workingArea(frame);
			maximized = true;
			frame.setExtendedState(Frame.NORMAL);
			frame.setMaximizedBounds(us);
			frame.setBounds(us);
		}
		sync();
	}

	private static void restoreFromMaximize(JFrame frame, Rectangle bounds) {
		maximized = false;
		frame.setExtendedState(Frame.NORMAL);
		frame.setBounds(usableRestore(frame, bounds));
		sync();
	}

	private static Rectangle workingArea(JFrame frame) {
		GraphicsConfiguration gc = frame.getGraphicsConfiguration();
		if (gc == null) {
			gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
		}
		Rectangle b = gc.getBounds();
		Insets in = Toolkit.getDefaultToolkit().getScreenInsets(gc);
		return new Rectangle(b.x + in.left, b.y + in.top,
				Math.max(1, b.width - in.left - in.right),
				Math.max(1, b.height - in.top - in.bottom));
	}

	private static Rectangle defaultRestoreBounds(JFrame frame) {
		Rectangle us = workingArea(frame);
		int extra = PluginSidebar.eastWidth();
		int w = Math.min(us.width, 1024 + extra);
		int h = Math.min(us.height, 700 + HEIGHT);
		int minW = Math.min(us.width, 765 + extra + 2);
		int minH = Math.min(us.height, 503 + HEIGHT + 2);
		if (w < minW) {
			w = minW;
		}
		if (h < minH) {
			h = minH;
		}
		return new Rectangle(us.x + (us.width - w) / 2, us.y + (us.height - h) / 2, w, h);
	}

	private static Rectangle usableRestore(JFrame frame, Rectangle r) {
		Rectangle us = workingArea(frame);
		int extra = PluginSidebar.eastWidth();
		int minW = Math.min(us.width, 765 + extra + 2);
		int minH = Math.min(us.height, 503 + HEIGHT + 2);
		if (r == null || (r.width >= us.width - 8 && r.height >= us.height - 8)) {
			return defaultRestoreBounds(frame);
		}
		int w = r.width;
		int h = r.height;
		if (w < minW) {
			w = minW;
		}
		if (h < minH) {
			h = minH;
		}
		if (w > us.width) {
			w = us.width;
		}
		if (h > us.height) {
			h = us.height;
		}
		int x = r.x;
		int y = r.y;
		if (x < us.x) {
			x = us.x;
		}
		if (y < us.y) {
			y = us.y;
		}
		if (x + w > us.x + us.width) {
			x = us.x + us.width - w;
		}
		if (y + h > us.y + us.height) {
			y = us.y + us.height - h;
		}
		return new Rectangle(x, y, w, h);
	}

	private static void installResize(final JFrame frame) {
		AWTEventListener listener = new AWTEventListener() {
			public void eventDispatched(AWTEvent event) {
				if (!(event instanceof MouseEvent)) {
					return;
				}
				MouseEvent e = (MouseEvent) event;
				if (!(e.getSource() instanceof java.awt.Component)) {
					return;
				}
				java.awt.Component src = (java.awt.Component) e.getSource();
				if (SwingUtilities.getWindowAncestor(src) != frame) {
					return;
				}
				if (client.frameMode != client.ScreenMode.RESIZABLE || isMaximized()) {
					if (e.getID() == MouseEvent.MOUSE_MOVED && resizeEdge == 0 && !dragging) {
						src.setCursor(Cursor.getDefaultCursor());
					}
					return;
				}
				int id = e.getID();
				int edge = (id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_PRESSED)
						? edgeAt(frame, e) : resizeEdge;
				if (isTitleBarComponent(src) && edge == 0 && resizeStart == null) {
					if (id == MouseEvent.MOUSE_MOVED) {
						src.setCursor(Cursor.getDefaultCursor());
					}
					return;
				}
				if (id == MouseEvent.MOUSE_MOVED && !dragging && resizeStart == null) {
					Cursor c = cursorFor(edge);
					frame.setCursor(c);
					src.setCursor(c);
				} else if (id == MouseEvent.MOUSE_PRESSED && SwingUtilities.isLeftMouseButton(e)) {
					if (edge != 0) {
						resizeEdge = edge;
						resizeStart = frame.getBounds();
						resizeAnchor = e.getLocationOnScreen();
						e.consume();
					}
				} else if (id == MouseEvent.MOUSE_DRAGGED && resizeStart != null) {
					resizeTo(frame, e.getLocationOnScreen());
					e.consume();
				} else if (id == MouseEvent.MOUSE_RELEASED && resizeStart != null) {
					resizeStart = null;
					resizeEdge = 0;
				}
			}
		};
		Toolkit.getDefaultToolkit().addAWTEventListener(listener,
				AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
	}

	private static boolean isTitleBarComponent(java.awt.Component src) {
		return instance != null && (src == instance || SwingUtilities.isDescendingFrom(src, instance));
	}

	private static int edgeAt(JFrame frame, MouseEvent e) {
		Point p = SwingUtilities.convertPoint((java.awt.Component) e.getSource(), e.getPoint(), frame);
		return edgeAtPoint(frame, p);
	}

	private static int edgeAtPoint(JFrame frame, Point p) {
		int t = 0;
		if (p.x <= EDGE_PAD) {
			t |= 1;
		}
		if (p.x >= frame.getWidth() - EDGE_PAD) {
			t |= 2;
		}
		if (p.y <= EDGE_PAD) {
			t |= 4;
		}
		if (p.y >= frame.getHeight() - EDGE_PAD) {
			t |= 8;
		}
		return t;
	}

	private static Cursor cursorFor(int edge) {
		switch (edge) {
		case 1:
			return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
		case 2:
			return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
		case 4:
			return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
		case 8:
			return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
		case 5:
			return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
		case 6:
			return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
		case 9:
			return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
		case 10:
			return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
		default:
			return Cursor.getDefaultCursor();
		}
	}

	private static void resizeTo(JFrame frame, Point now) {
		if (resizeStart == null || resizeAnchor == null) {
			return;
		}
		int dx = now.x - resizeAnchor.x;
		int dy = now.y - resizeAnchor.y;
		int x = resizeStart.x;
		int y = resizeStart.y;
		int w = resizeStart.width;
		int h = resizeStart.height;
		int extra = PluginSidebar.eastWidth();
		int minW = 765 + extra + 2;
		int minH = 503 + HEIGHT + 2;
		if ((resizeEdge & 1) != 0) {
			int nw = Math.max(minW, w - dx);
			x += w - nw;
			w = nw;
		}
		if ((resizeEdge & 2) != 0) {
			w = Math.max(minW, w + dx);
		}
		if ((resizeEdge & 4) != 0) {
			int nh = Math.max(minH, h - dy);
			y += h - nh;
			h = nh;
		}
		if ((resizeEdge & 8) != 0) {
			h = Math.max(minH, h + dy);
		}
		maximized = false;
		frame.setBounds(x, y, w, h);
	}

	private static final class SidebarButton extends JPanel {
		private static final long serialVersionUID = 1L;

		SidebarButton() {
			setPreferredSize(new Dimension(SIDE_W, HEIGHT));
			setMinimumSize(new Dimension(SIDE_W, HEIGHT));
			setMaximumSize(new Dimension(SIDE_W, Integer.MAX_VALUE));
			setOpaque(true);
			setBackground(BG);
			setAlignmentY(0f);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText("Toggle sidebar");
			addMouseListener(new MouseAdapter() {
				public void mouseEntered(MouseEvent e) {
					setBackground(HOVER);
				}

				public void mouseExited(MouseEvent e) {
					setBackground(BG);
				}

				public void mouseReleased(MouseEvent e) {
					if (SwingUtilities.isLeftMouseButton(e) && contains(e.getPoint())) {
						PluginSidebar.toggleSidebar();
					}
				}
			});
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(ICON);
			g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			int cx = getWidth() / 2;
			int cy = getHeight() / 2;
			if (PluginSidebar.sidebarOut) {
				g2.drawLine(cx + 4, cy - 6, cx - 3, cy);
				g2.drawLine(cx - 3, cy, cx + 4, cy + 6);
			} else {
				g2.drawLine(cx - 4, cy - 6, cx + 3, cy);
				g2.drawLine(cx + 3, cy, cx - 4, cy + 6);
			}
			g2.dispose();
		}
	}

	private static final class WinButton extends JPanel {
		private static final long serialVersionUID = 1L;
		static final int MIN = 0;
		static final int MAX = 1;
		static final int CLOSE = 2;
		private final int kind;
		private boolean hover;

		WinButton(int type) {
			this.kind = type;
			setPreferredSize(new Dimension(BTN_W, HEIGHT));
			setMinimumSize(new Dimension(BTN_W, HEIGHT));
			setMaximumSize(new Dimension(BTN_W, Integer.MAX_VALUE));
			setOpaque(true);
			setBackground(BG);
			setAlignmentY(0f);
			if (type == MIN) {
				setToolTipText("Minimize");
			} else if (type == MAX) {
				setToolTipText("Maximize");
			} else {
				setToolTipText("Close");
			}
			addMouseListener(new MouseAdapter() {
				public void mouseEntered(MouseEvent e) {
					hover = true;
					setBackground(kind == CLOSE ? CLOSE_HOVER : HOVER);
					repaint();
				}

				public void mouseExited(MouseEvent e) {
					hover = false;
					setBackground(BG);
					repaint();
				}

				public void mouseReleased(MouseEvent e) {
					if (!SwingUtilities.isLeftMouseButton(e) || !contains(e.getPoint())) {
						return;
					}
					JFrame frame = Jframe.getFrame();
					if (frame == null) {
						return;
					}
					if (kind == MIN) {
						frame.setExtendedState(frame.getExtendedState() | Frame.ICONIFIED);
					} else if (kind == MAX) {
						toggleMaximize();
					} else {
						frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
					}
				}
			});
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(kind == CLOSE && hover ? Color.WHITE : ICON);
			g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			int cx = getWidth() / 2;
			int cy = getHeight() / 2;
			if (kind == MIN) {
				g2.drawLine(cx - 5, cy + 3, cx + 5, cy + 3);
			} else if (kind == MAX) {
				if (isMaximized()) {
					g2.drawRect(cx - 3, cy - 5, 8, 8);
					g2.drawLine(cx - 6, cy - 2, cx - 6, cy + 6);
					g2.drawLine(cx - 6, cy + 6, cx + 2, cy + 6);
					g2.drawLine(cx - 6, cy - 2, cx - 4, cy - 2);
					g2.drawLine(cx + 2, cy + 4, cx + 2, cy + 6);
				} else {
					g2.drawRect(cx - 5, cy - 5, 10, 10);
				}
			} else {
				g2.drawLine(cx - 4, cy - 4, cx + 4, cy + 4);
				g2.drawLine(cx + 4, cy - 4, cx - 4, cy + 4);
			}
			g2.dispose();
		}
	}
}
