import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Generic magnified crop: src, x, y, w, h, scale, out. */
public class ShotCrop2 {
	public static void main(String[] a) throws Exception {
		BufferedImage src = ImageIO.read(new File(a[0]));
		int x = Integer.parseInt(a[1]), y = Integer.parseInt(a[2]);
		int w = Integer.parseInt(a[3]), h = Integer.parseInt(a[4]);
		int s = Integer.parseInt(a[5]);
		w = Math.min(w, src.getWidth() - x);
		h = Math.min(h, src.getHeight() - y);
		BufferedImage out = new BufferedImage(w * s, h * s, BufferedImage.TYPE_INT_RGB);
		for (int yy = 0; yy < h * s; yy++) {
			for (int xx = 0; xx < w * s; xx++) {
				out.setRGB(xx, yy, src.getRGB(x + xx / s, y + yy / s));
			}
		}
		ImageIO.write(out, "png", new File(a[6]));
		System.out.println("wrote " + a[6] + "  " + w + "x" + h + " at " + s + "x");
	}
}
