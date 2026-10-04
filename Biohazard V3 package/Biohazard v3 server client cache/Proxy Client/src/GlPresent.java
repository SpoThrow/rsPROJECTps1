import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import sign.signlink;

final class GlPresent {

	static void applyPipeline() {
		boolean enabled = false;
		try {
			File propsFile = new File(signlink.findcachedir() + "client_settings.properties");
			if (propsFile.exists()) {
				Properties props = new Properties();
				FileInputStream in = new FileInputStream(propsFile);
				props.load(in);
				in.close();
				String v = props.getProperty("openGl", "false");
				if (v != null) {
					v = v.trim();
					enabled = v.equalsIgnoreCase("true") || v.equals("1") || v.equalsIgnoreCase("on")
							|| v.equalsIgnoreCase("yes");
				}
			}
		} catch (Exception e) {
		}
		if (!enabled) {
			return;
		}
		client.openGlEnabled = true;
		try {
			System.setProperty("sun.java2d.opengl", "true");
			System.setProperty("sun.java2d.opengl.fbobject", "true");
			System.setProperty("sun.java2d.accthreshold", "0");
			System.setProperty("sun.java2d.translaccel", "true");
			System.setProperty("sun.java2d.d3d", "false");
			System.setProperty("sun.java2d.noddraw", "true");
			System.setProperty("sun.java2d.dpiaware", "true");
			System.setProperty("sun.java2d.uiScale", "1");
			System.setProperty("sun.java2d.uiScale.enabled", "false");
			System.setProperty("sun.java2d.win.uiScaleX", "1.0");
			System.setProperty("sun.java2d.win.uiScaleY", "1.0");
		} catch (Exception e) {
		}
	}

	static BufferedImage wrapPixels(int[] pixels, int width, int height, DirectColorModel model) {
		DataBufferInt db = new DataBufferInt(pixels, width * height);
		WritableRaster raster = Raster.createPackedRaster(db, width, height, width,
				new int[] { 0xff0000, 0xff00, 0xff }, null);
		return new BufferedImage(model, raster, false, null);
	}

	static boolean presentGame(RSImageProducer producer, int destX, int destY) {
		return false;
	}

	static void blit(Graphics g, Image image, int x, int y) {
		if (g instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
			g2.drawImage(image, x, y, null);
			return;
		}
		g.drawImage(image, x, y, null);
	}
}
