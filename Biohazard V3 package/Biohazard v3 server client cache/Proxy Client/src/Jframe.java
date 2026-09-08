import sign.signlink;

import java.io.File;
import java.net.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.*;


public class Jframe extends client implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6978617783576386732L;
	private static JFrame frame;

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
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			JPopupMenu.setDefaultLightWeightPopupEnabled(false);
			frame = new JFrame("Biohazard");
			frame.setLayout(new BorderLayout());
			setFocusTraversalKeysEnabled(false);
			setFocusable(true);
			frame.setResizable(false);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			JPanel gamePanel = new JPanel();

			gamePanel.setLayout(new BorderLayout());
			gamePanel.setFocusable(false);
			gamePanel.add(this);
			gamePanel.setPreferredSize(new Dimension(765, 503));
			frame.getContentPane().add(gamePanel, BorderLayout.CENTER);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true); // can see the client
			frame.setResizable(false); // resizeable frame
			frame.addKeyListener(this);
			frame.addFocusListener(this);
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
		try {
			Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
			Point point = window.getLocationOnScreen();
			int x = (int)point.getX();
			int y = (int)point.getY();
			int w = window.getWidth();
			int h = window.getHeight();
			Robot robot = new Robot(window.getGraphicsConfiguration().getDevice());
			Rectangle captureSize = new Rectangle(x, y, w, h);
			BufferedImage bufferedimage = robot.createScreenCapture(captureSize);
			String imageName = JOptionPane.showInputDialog(frame, "Image Name :", "Screenshot", JOptionPane.OK_CANCEL_OPTION);
			if(!imageName.equals("null"))
				ImageIO.write(bufferedimage, "png", new File(signlink.findcachedir()+"./Screenshots/" + imageName + ".png"));
		} catch (Exception e) {
		}
	}
	
	public void actionPerformed(ActionEvent evt) {
	}
	
}