import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import sign.signlink;

public final class HudLayout {

	private static final HudLayout FIXED = createFixedDefaults();
	private static final HudLayout RESIZABLE = createResizableDefaults();

	public final boolean resizableMode;
	public int compassX;
	public int compassY;
	public int hpOrbX;
	public int hpOrbY;
	public int prayerOrbX;
	public int prayerOrbY;
	public int runOrbX;
	public int runOrbY;
	public int specOrbX;
	public int specOrbY;
	public int worldMapX;
	public int worldMapY;
	public int mapFrameX;
	public int mapFrameY;
	public int minimapX;
	public int minimapY;
	public int compassW;
	public int compassH;
	public int hpOrbW;
	public int hpOrbH;
	public int prayerOrbW;
	public int prayerOrbH;
	public int runOrbW;
	public int runOrbH;
	public int specOrbW;
	public int specOrbH;
	public int worldMapW;
	public int worldMapH;
	public int mapFrameW;
	public int mapFrameH;
	public int xpOrbX;
	public int xpOrbY;

	private HudLayout(boolean resizableMode) {
		this.resizableMode = resizableMode;
	}

	public static HudLayout get() {
		return client.isFixed() ? FIXED : RESIZABLE;
	}

	public static HudLayout fixed() {
		return FIXED;
	}

	public static HudLayout resizable() {
		return RESIZABLE;
	}

	public String modeName() {
		return resizableMode ? "RESIZABLE" : "FIXED";
	}

	public File propertiesFile() {
		if (resizableMode) {
			return new File(signlink.findcachedir() + "Sprites/Gameframe/resizable/hud_layout.properties");
		}
		return new File(signlink.findcachedir() + "Sprites/Gameframe/fixed/hud_layout.properties");
	}

	public static File propertiesFileForCurrentMode() {
		return get().propertiesFile();
	}

	public void load() {
		File file = propertiesFile();
		if (!file.exists()) {
			return;
		}
		Properties properties = new Properties();
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			properties.load(in);
			compassX = read(properties, "compassX", compassX);
			compassY = read(properties, "compassY", compassY);
			hpOrbX = read(properties, "hpOrbX", hpOrbX);
			hpOrbY = read(properties, "hpOrbY", hpOrbY);
			prayerOrbX = read(properties, "prayerOrbX", prayerOrbX);
			prayerOrbY = read(properties, "prayerOrbY", prayerOrbY);
			runOrbX = read(properties, "runOrbX", runOrbX);
			runOrbY = read(properties, "runOrbY", runOrbY);
			specOrbX = read(properties, "specOrbX", specOrbX);
			specOrbY = read(properties, "specOrbY", specOrbY);
			worldMapX = read(properties, "worldMapX", worldMapX);
			worldMapY = read(properties, "worldMapY", worldMapY);
			mapFrameX = read(properties, "mapFrameX", mapFrameX);
			mapFrameY = read(properties, "mapFrameY", mapFrameY);
			minimapX = read(properties, "minimapX", minimapX);
			minimapY = read(properties, "minimapY", minimapY);
			compassW = read(properties, "compassW", compassW);
			compassH = read(properties, "compassH", compassH);
			if (resizableMode && compassX < 0) {
				compassX = mapFrameX + 4;
				if (compassY < 0) {
					compassY = mapFrameY + 4;
				}
			}
			hpOrbW = read(properties, "hpOrbW", hpOrbW);
			hpOrbH = read(properties, "hpOrbH", hpOrbH);
			prayerOrbW = read(properties, "prayerOrbW", prayerOrbW);
			prayerOrbH = read(properties, "prayerOrbH", prayerOrbH);
			runOrbW = read(properties, "runOrbW", runOrbW);
			runOrbH = read(properties, "runOrbH", runOrbH);
			specOrbW = read(properties, "specOrbW", specOrbW);
			specOrbH = read(properties, "specOrbH", specOrbH);
			worldMapW = read(properties, "worldMapW", worldMapW);
			worldMapH = read(properties, "worldMapH", worldMapH);
			mapFrameW = read(properties, "mapFrameW", mapFrameW);
			mapFrameH = read(properties, "mapFrameH", mapFrameH);
			xpOrbX = read(properties, "xpOrbX", xpOrbX);
			xpOrbY = read(properties, "xpOrbY", xpOrbY);
		} catch (Exception ignored) {
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception ignored) {
				}
			}
		}
	}

	public void save() {
		File file = propertiesFile();
		file.getParentFile().mkdirs();
		Properties properties = new Properties();
		properties.setProperty("compassX", Integer.toString(compassX));
		properties.setProperty("compassY", Integer.toString(compassY));
		properties.setProperty("hpOrbX", Integer.toString(hpOrbX));
		properties.setProperty("hpOrbY", Integer.toString(hpOrbY));
		properties.setProperty("prayerOrbX", Integer.toString(prayerOrbX));
		properties.setProperty("prayerOrbY", Integer.toString(prayerOrbY));
		properties.setProperty("runOrbX", Integer.toString(runOrbX));
		properties.setProperty("runOrbY", Integer.toString(runOrbY));
		properties.setProperty("specOrbX", Integer.toString(specOrbX));
		properties.setProperty("specOrbY", Integer.toString(specOrbY));
		properties.setProperty("worldMapX", Integer.toString(worldMapX));
		properties.setProperty("worldMapY", Integer.toString(worldMapY));
		properties.setProperty("mapFrameX", Integer.toString(mapFrameX));
		properties.setProperty("mapFrameY", Integer.toString(mapFrameY));
		properties.setProperty("minimapX", Integer.toString(minimapX));
		properties.setProperty("minimapY", Integer.toString(minimapY));
		properties.setProperty("compassW", Integer.toString(compassW));
		properties.setProperty("compassH", Integer.toString(compassH));
		properties.setProperty("hpOrbW", Integer.toString(hpOrbW));
		properties.setProperty("hpOrbH", Integer.toString(hpOrbH));
		properties.setProperty("prayerOrbW", Integer.toString(prayerOrbW));
		properties.setProperty("prayerOrbH", Integer.toString(prayerOrbH));
		properties.setProperty("runOrbW", Integer.toString(runOrbW));
		properties.setProperty("runOrbH", Integer.toString(runOrbH));
		properties.setProperty("specOrbW", Integer.toString(specOrbW));
		properties.setProperty("specOrbH", Integer.toString(specOrbH));
		properties.setProperty("worldMapW", Integer.toString(worldMapW));
		properties.setProperty("worldMapH", Integer.toString(worldMapH));
		properties.setProperty("mapFrameW", Integer.toString(mapFrameW));
		properties.setProperty("mapFrameH", Integer.toString(mapFrameH));
		properties.setProperty("xpOrbX", Integer.toString(xpOrbX));
		properties.setProperty("xpOrbY", Integer.toString(xpOrbY));
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
			properties.store(out, "Soul-Trail " + modeName() + " minimap HUD layout. Width/height 0 = native sprite size.");
		} catch (Exception ignored) {
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (Exception ignored) {
				}
			}
		}
	}

	public String toJavaSnippet() {
		StringBuilder b = new StringBuilder();
		b.append("// ").append(modeName()).append(" layout\n");
		b.append("HudLayout h = HudLayout.get();\n");
		b.append("h.compassX = ").append(compassX).append(";\n");
		b.append("h.compassY = ").append(compassY).append(";\n");
		b.append("h.hpOrbX = ").append(hpOrbX).append(";\n");
		b.append("h.hpOrbY = ").append(hpOrbY).append(";\n");
		b.append("h.prayerOrbX = ").append(prayerOrbX).append(";\n");
		b.append("h.prayerOrbY = ").append(prayerOrbY).append(";\n");
		b.append("h.runOrbX = ").append(runOrbX).append(";\n");
		b.append("h.runOrbY = ").append(runOrbY).append(";\n");
		b.append("h.specOrbX = ").append(specOrbX).append(";\n");
		b.append("h.specOrbY = ").append(specOrbY).append(";\n");
		b.append("h.worldMapX = ").append(worldMapX).append(";\n");
		b.append("h.worldMapY = ").append(worldMapY).append(";\n");
		b.append("h.mapFrameX = ").append(mapFrameX).append(";\n");
		b.append("h.mapFrameY = ").append(mapFrameY).append(";\n");
		b.append("h.minimapX = ").append(minimapX).append(";\n");
		b.append("h.minimapY = ").append(minimapY).append(";\n");
		b.append("h.compassW = ").append(compassW).append(";\n");
		b.append("h.compassH = ").append(compassH).append(";\n");
		b.append("h.hpOrbW = ").append(hpOrbW).append(";\n");
		b.append("h.hpOrbH = ").append(hpOrbH).append(";\n");
		b.append("h.prayerOrbW = ").append(prayerOrbW).append(";\n");
		b.append("h.prayerOrbH = ").append(prayerOrbH).append(";\n");
		b.append("h.runOrbW = ").append(runOrbW).append(";\n");
		b.append("h.runOrbH = ").append(runOrbH).append(";\n");
		b.append("h.specOrbW = ").append(specOrbW).append(";\n");
		b.append("h.specOrbH = ").append(specOrbH).append(";\n");
		b.append("h.worldMapW = ").append(worldMapW).append(";\n");
		b.append("h.worldMapH = ").append(worldMapH).append(";\n");
		b.append("h.mapFrameW = ").append(mapFrameW).append(";\n");
		b.append("h.mapFrameH = ").append(mapFrameH).append(";\n");
		b.append("h.xpOrbX = ").append(xpOrbX).append(";\n");
		b.append("h.xpOrbY = ").append(xpOrbY).append(";\n");
		b.append("h.save();\n");
		return b.toString();
	}

	private static int read(Properties properties, String key, int fallback) {
		String value = properties.getProperty(key);
		if (value == null) {
			return fallback;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static HudLayout createFixedDefaults() {
		HudLayout h = new HudLayout(false);
		h.compassX = 8;
		h.compassY = 8;
		h.hpOrbX = 174;
		h.hpOrbY = 14;
		h.prayerOrbX = 190;
		h.prayerOrbY = 53;
		h.runOrbX = 190;
		h.runOrbY = 92;
		h.specOrbX = 180;
		h.specOrbY = 129;
		h.worldMapX = 8;
		h.worldMapY = 124;
		h.mapFrameX = 0;
		h.mapFrameY = 0;
		h.minimapX = 45;
		h.minimapY = 10;
		h.compassW = 0;
		h.compassH = 0;
		h.hpOrbW = 0;
		h.hpOrbH = 0;
		h.prayerOrbW = 0;
		h.prayerOrbH = 0;
		h.runOrbW = 0;
		h.runOrbH = 0;
		h.specOrbW = 0;
		h.specOrbH = 0;
		h.worldMapW = 0;
		h.worldMapH = 0;
		h.mapFrameW = 0;
		h.mapFrameH = 0;
		h.xpOrbX = -29;
		h.xpOrbY = 4;
		return h;
	}

	private static HudLayout createResizableDefaults() {
		HudLayout h = new HudLayout(true);
		h.compassX = 69;
		h.compassY = 5;
		h.hpOrbX = 39;
		h.hpOrbY = 35;
		h.prayerOrbX = 44;
		h.prayerOrbY = 69;
		h.runOrbX = 52;
		h.runOrbY = 103;
		h.specOrbX = 58;
		h.specOrbY = 136;
		h.worldMapX = 213;
		h.worldMapY = 122;
		h.mapFrameX = 65;
		h.mapFrameY = 0;
		h.minimapX = 90;
		h.minimapY = 8;
		h.compassW = 0;
		h.compassH = 0;
		h.hpOrbW = 0;
		h.hpOrbH = 0;
		h.prayerOrbW = 0;
		h.prayerOrbH = 0;
		h.runOrbW = 0;
		h.runOrbH = 0;
		h.specOrbW = 0;
		h.specOrbH = 0;
		h.worldMapW = 0;
		h.worldMapH = 0;
		h.mapFrameW = 0;
		h.mapFrameH = 0;
		h.xpOrbX = 33;
		h.xpOrbY = 4;
		return h;
	}

	public String saveAsBuiltin() {
		File src = sourceFile();
		if (src == null) {
			return null;
		}
		try {
			String text = readAll(src);
			String method = resizableMode ? "createResizableDefaults" : "createFixedDefaults";
			String startTok = "	private static HudLayout " + method + "() {";
			int start = text.indexOf(startTok);
			if (start < 0) {
				return src.getAbsolutePath();
			}
			int end = text.indexOf("\n	}", start);
			if (end < 0) {
				return src.getAbsolutePath();
			}
			end += 3;
			String rebuilt = factoryMethod();
			text = text.substring(0, start) + rebuilt + text.substring(end);
			writeAll(src, text);
			File props = propertiesFile();
			if (props.exists()) {
				props.delete();
			}
			return src.getAbsolutePath();
		} catch (Exception e) {
			return null;
		}
	}

	private String factoryMethod() {
		StringBuilder b = new StringBuilder();
		b.append("	private static HudLayout create");
		b.append(resizableMode ? "Resizable" : "Fixed");
		b.append("Defaults() {\n");
		b.append("		HudLayout h = new HudLayout(").append(resizableMode).append(");\n");
		b.append("		h.compassX = ").append(compassX).append(";\n");
		b.append("		h.compassY = ").append(compassY).append(";\n");
		b.append("		h.hpOrbX = ").append(hpOrbX).append(";\n");
		b.append("		h.hpOrbY = ").append(hpOrbY).append(";\n");
		b.append("		h.prayerOrbX = ").append(prayerOrbX).append(";\n");
		b.append("		h.prayerOrbY = ").append(prayerOrbY).append(";\n");
		b.append("		h.runOrbX = ").append(runOrbX).append(";\n");
		b.append("		h.runOrbY = ").append(runOrbY).append(";\n");
		b.append("		h.specOrbX = ").append(specOrbX).append(";\n");
		b.append("		h.specOrbY = ").append(specOrbY).append(";\n");
		b.append("		h.worldMapX = ").append(worldMapX).append(";\n");
		b.append("		h.worldMapY = ").append(worldMapY).append(";\n");
		b.append("		h.mapFrameX = ").append(mapFrameX).append(";\n");
		b.append("		h.mapFrameY = ").append(mapFrameY).append(";\n");
		b.append("		h.minimapX = ").append(minimapX).append(";\n");
		b.append("		h.minimapY = ").append(minimapY).append(";\n");
		b.append("		h.compassW = ").append(compassW).append(";\n");
		b.append("		h.compassH = ").append(compassH).append(";\n");
		b.append("		h.hpOrbW = ").append(hpOrbW).append(";\n");
		b.append("		h.hpOrbH = ").append(hpOrbH).append(";\n");
		b.append("		h.prayerOrbW = ").append(prayerOrbW).append(";\n");
		b.append("		h.prayerOrbH = ").append(prayerOrbH).append(";\n");
		b.append("		h.runOrbW = ").append(runOrbW).append(";\n");
		b.append("		h.runOrbH = ").append(runOrbH).append(";\n");
		b.append("		h.specOrbW = ").append(specOrbW).append(";\n");
		b.append("		h.specOrbH = ").append(specOrbH).append(";\n");
		b.append("		h.worldMapW = ").append(worldMapW).append(";\n");
		b.append("		h.worldMapH = ").append(worldMapH).append(";\n");
		b.append("		h.mapFrameW = ").append(mapFrameW).append(";\n");
		b.append("		h.mapFrameH = ").append(mapFrameH).append(";\n");
		b.append("		h.xpOrbX = ").append(xpOrbX).append(";\n");
		b.append("		h.xpOrbY = ").append(xpOrbY).append(";\n");
		b.append("		return h;\n");
		b.append("	}");
		return b.toString();
	}

	private static File sourceFile() {
		String[] paths = {
			"src/HudLayout.java",
			"Proxy Client/src/HudLayout.java",
			System.getProperty("user.dir") + "/src/HudLayout.java",
			"C:/Users/llrbi/Documents/GitHub/rsPROJECTps/Biohazard V3 package/Biohazard v3 server client cache/Proxy Client/src/HudLayout.java"
		};
		for (int i = 0; i < paths.length; i++) {
			File f = new File(paths[i]);
			if (f.isFile()) {
				return f;
			}
		}
		return null;
	}

	private static String readAll(File file) throws Exception {
		FileInputStream in = new FileInputStream(file);
		try {
			byte[] data = new byte[(int) file.length()];
			int n = 0;
			while (n < data.length) {
				int r = in.read(data, n, data.length - n);
				if (r < 0) {
					break;
				}
				n += r;
			}
			return new String(data, 0, n, "UTF-8");
		} finally {
			in.close();
		}
	}

	private static void writeAll(File file, String text) throws Exception {
		FileOutputStream out = new FileOutputStream(file);
		try {
			out.write(text.getBytes("UTF-8"));
		} finally {
			out.close();
		}
	}

	static {
		FIXED.load();
		RESIZABLE.load();
	}
}

