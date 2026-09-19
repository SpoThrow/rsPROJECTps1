import sign.signlink;

import java.io.File;
import java.net.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.*;


public class Jframe extends client implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6978617783576386732L;
	private static JFrame frame;
	private static JPanel gamePanel;

	public static void setCanvasSize(int width, int height, boolean resizable) {
		if (frame == null || gamePanel == null || client.instance == null) {
			return;
		}
		Dimension canvas = new Dimension(width, height);
		gamePanel.setPreferredSize(canvas);
		instance.setPreferredSize(canvas);
		instance.setSize(canvas);
		frame.setResizable(resizable);
		if (resizable) {
			frame.setMinimumSize(new Dimension(773, 531));
			frame.pack();
			Insets insets = frame.getInsets();
			frame.setSize(width + insets.left + insets.right, height + insets.top + insets.bottom);
		} else {
			frame.setMinimumSize(new Dimension(0, 0));
			frame.pack();
			frame.setMinimumSize(frame.getSize());
			frame.setSize(frame.getPreferredSize());
		}
		frame.validate();
		frame.repaint();
	}

	public static JFrame getFrame() {
		return frame;
	}

	public Jframe(String args[]) {
		super();
		try {
			sign.signlink.startpriv(InetAddress.getByName(server));
			initUI();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void initUI() {
		try {
			loadClientSettings();
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			JPopupMenu.setDefaultLightWeightPopupEnabled(false);
			frame = new JFrame("Biohazard");
			frame.setLayout(new BorderLayout());
			setFocusTraversalKeysEnabled(false);
			setFocusable(true);
			boolean resizable = frameMode == ScreenMode.RESIZABLE;
			frame.setResizable(resizable);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			gamePanel = new JPanel();

			gamePanel.setLayout(new BorderLayout());
			gamePanel.setFocusable(false);
			gamePanel.add(this);
			int canvasW = resizable ? Math.max(765, savedResizeWidth) : 765;
			int canvasH = resizable ? Math.max(503, savedResizeHeight) : 503;
			gamePanel.setPreferredSize(new Dimension(canvasW, canvasH));
			setPreferredSize(new Dimension(canvasW, canvasH));
			frame.getContentPane().add(gamePanel, BorderLayout.CENTER);
			frame.pack();
			if (resizable) {
				frame.setMinimumSize(new Dimension(773, 531));
			} else {
				frame.setMinimumSize(frame.getSize());
			}
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
			frame.addKeyListener(this);
			frame.addFocusListener(this);
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					saveClientSettings();
				}
			});
			frame.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent e) {
					if (frameMode == ScreenMode.RESIZABLE) {
						refreshFrameSize();
					}
				}
			});
			requestFocus();
			requestFocusInWindow();
			init();
		} catch (Exception e) {
				e.printStackTrace();
		}
	}

	public URL getCodeBase() {
		try {
			return new URL("http://" + server + "/cache");
		} catch (Exception e) {
			return super.getCodeBase();
		}
	}

	public URL getDocumentBase() {
		return getCodeBase();
	}

	public void loadError(String s) {
		System.out.println("loadError: " + s);
	}

	public String getParameter(String key) {
			return "";
	}

	public static void takeScreenshot() {
		takeScreenshot(client.silentScreenshots);
	}

	public static void takeScreenshot(boolean silent) {
		try {
			Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
			if (window == null) {
				window = frame;
			}
			Point point = window.getLocationOnScreen();
			int x = (int)point.getX();
			int y = (int)point.getY();
			int w = window.getWidth();
			int h = window.getHeight();
			Robot robot = new Robot(window.getGraphicsConfiguration().getDevice());
			Rectangle captureSize = new Rectangle(x, y, w, h);
			BufferedImage bufferedimage = robot.createScreenCapture(captureSize);
			File folder = new File(signlink.findcachedir() + "Screenshots");
			folder.mkdirs();
			String imageName;
			if (silent) {
				java.util.Calendar cal = java.util.Calendar.getInstance();
				imageName = String.format("screenshot_%04d-%02d-%02d_%02d-%02d-%02d",
						Integer.valueOf(cal.get(java.util.Calendar.YEAR)),
						Integer.valueOf(cal.get(java.util.Calendar.MONTH) + 1),
						Integer.valueOf(cal.get(java.util.Calendar.DAY_OF_MONTH)),
						Integer.valueOf(cal.get(java.util.Calendar.HOUR_OF_DAY)),
						Integer.valueOf(cal.get(java.util.Calendar.MINUTE)),
						Integer.valueOf(cal.get(java.util.Calendar.SECOND)));
			} else {
				imageName = JOptionPane.showInputDialog(frame, "Image Name :", "Screenshot", JOptionPane.OK_CANCEL_OPTION);
				if (imageName == null || imageName.equals("null") || imageName.trim().length() == 0) {
					return;
				}
			}
			ImageIO.write(bufferedimage, "png", new File(folder, imageName + ".png"));
		} catch (Exception e) {
		}
	}
	
	public void actionPerformed(ActionEvent evt) {
	}
	
}