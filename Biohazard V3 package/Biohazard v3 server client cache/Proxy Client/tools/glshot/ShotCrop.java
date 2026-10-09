import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Writes nearest-neighbour MAGNIFIED crops of the two dumped frames, so a region can be LOOKED AT. */
public class ShotCrop {
	public static void main(String[] args) throws Exception {
		BufferedImage sw = ImageIO.read(new File("gldiff-software.png"));
		BufferedImage gl = ImageIO.read(new File("gldiff-gl.png"));
		// name, x, y, w, h, scale
		Object[][] crops = {
				{ "ground-centre", 200, 540, 340, 126, 3 },
				{ "ground-right", 560, 400, 340, 200, 3 },
				{ "horizon", 150, 140, 420, 200, 3 },
				{ "mid-scene", 430, 250, 440, 220, 3 } };
		for (Object[] c : crops) {
			String name = (String) c[0];
			int x = (Integer) c[1], y = (Integer) c[2], w = (Integer) c[3], h = (Integer) c[4];
			int s = (Integer) c[5];
			write("crop-" + name + "-software.png", sw, x, y, w, h, s);
			write("crop-" + name + "-gl.png", gl, x, y, w, h, s);
			System.out.println("crop-" + name + "-software.png / crop-" + name + "-gl.png  ("
					+ x + "," + y + " " + w + "x" + h + " at " + s + "x)");
		}
	}

	static void write(String path, BufferedImage src, int x, int y, int w, int h, int scale)
			throws Exception {
		w = Math.min(w, src.getWidth() - x);
		h = Math.min(h, src.getHeight() - y);
		BufferedImage out = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
		for (int yy = 0; yy < h * scale; yy++) {
			for (int xx = 0; xx < w * scale; xx++) {
				out.setRGB(xx, yy, src.getRGB(x + xx / scale, y + yy / scale));
			}
		}
		ImageIO.write(out, "png", new File(path));
	}
}
