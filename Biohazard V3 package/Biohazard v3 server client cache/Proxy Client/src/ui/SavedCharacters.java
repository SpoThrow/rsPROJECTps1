package ui;

import java.util.Properties;import def.IDK;
import def.ItemDef;
import game.client;
import model.Model;
import model.Texture;



public final class SavedCharacters {

	static final int MAX = 5;
	public static final int PANEL_X = 202;
	public static final int PANEL_Y = 371;
	public static final int PANEL_W = 360;
	static final int PANEL_H = 132;

	private static final SavedCharacters[] slots = new SavedCharacters[MAX];

	public String name = "";
	public String password = "";
	public int combat;
	public int totalLevel;
	public int totalXp;
	int gender;
	int[] equipment = new int[12];
	int[] colors = new int[5];

	public static int count() {
		int n = 0;
		for (int i = 0; i < MAX; i++) {
			if (slots[i] != null) {
				n++;
			}
		}
		return n;
	}

	public static SavedCharacters get(int i) {
		if (i < 0 || i >= MAX) {
			return null;
		}
		return slots[i];
	}

	public static int indexOf(String name) {
		if (name == null) {
			return -1;
		}
		String key = name.trim();
		for (int i = 0; i < MAX; i++) {
			if (slots[i] != null && slots[i].name.equalsIgnoreCase(key)) {
				return i;
			}
		}
		return -1;
	}

	public static void upsert(String name, String password, int combat, int totalLevel, int totalXp,
			int gender, int[] equipment, int[] colors) {
		if (name == null) {
			return;
		}
		name = name.trim();
		if (name.length() == 0) {
			return;
		}
		int idx = indexOf(name);
		if (idx < 0) {
			for (int i = 0; i < MAX; i++) {
				if (slots[i] == null) {
					idx = i;
					break;
				}
			}
		}
		if (idx < 0) {
			for (int i = 0; i < MAX - 1; i++) {
				slots[i] = slots[i + 1];
			}
			idx = MAX - 1;
		}
		SavedCharacters slot = slots[idx];
		if (slot == null) {
			slot = new SavedCharacters();
			slots[idx] = slot;
		}
		slot.name = name;
		if (password != null && password.length() > 0) {
			slot.password = password;
		}
		if (combat > 0) {
			slot.combat = combat;
		}
		if (totalLevel > 0) {
			slot.totalLevel = totalLevel;
		}
		if (totalXp > 0) {
			slot.totalXp = totalXp;
		}
		slot.gender = gender;
		if (hasLook(equipment)) {
			System.arraycopy(equipment, 0, slot.equipment, 0, Math.min(12, equipment.length));
			if (colors != null) {
				System.arraycopy(colors, 0, slot.colors, 0, Math.min(5, colors.length));
			}
		}
	}

	static boolean hasLook(int[] values) {
		if (values == null) {
			return false;
		}
		for (int i = 0; i < values.length; i++) {
			if (values[i] != 0) {
				return true;
			}
		}
		return false;
	}

	public static void remove(int index) {
		if (index < 0 || index >= MAX || slots[index] == null) {
			return;
		}
		for (int i = index; i < MAX - 1; i++) {
			slots[i] = slots[i + 1];
		}
		slots[MAX - 1] = null;
	}

	public static void save(Properties props) {
		for (int i = 0; i < MAX; i++) {
			SavedCharacters slot = slots[i];
			String p = "savedChar" + i + ".";
			if (slot == null) {
				props.remove(p + "name");
				continue;
			}
			props.setProperty(p + "name", slot.name);
			props.setProperty(p + "pass", slot.password == null ? "" : slot.password);
			props.setProperty(p + "combat", Integer.toString(slot.combat));
			props.setProperty(p + "total", Integer.toString(slot.totalLevel));
			props.setProperty(p + "xp", Integer.toString(slot.totalXp));
			props.setProperty(p + "gender", Integer.toString(slot.gender));
			props.setProperty(p + "equip", join(slot.equipment));
			props.setProperty(p + "colors", join(slot.colors));
		}
	}

	public static void load(Properties props) {
		for (int i = 0; i < MAX; i++) {
			slots[i] = null;
			String p = "savedChar" + i + ".";
			String name = props.getProperty(p + "name", "");
			if (name == null || name.trim().length() == 0) {
				continue;
			}
			SavedCharacters slot = new SavedCharacters();
			slot.name = name.trim();
			slot.password = props.getProperty(p + "pass", "");
			slot.combat = parseInt(props.getProperty(p + "combat"), 0);
			slot.totalLevel = parseInt(props.getProperty(p + "total"), 0);
			slot.totalXp = parseInt(props.getProperty(p + "xp"), 0);
			slot.gender = parseInt(props.getProperty(p + "gender"), 0);
			parseInts(props.getProperty(p + "equip"), slot.equipment);
			parseInts(props.getProperty(p + "colors"), slot.colors);
			slots[i] = slot;
		}
		if (count() == 0) {
			String user = props.getProperty("rememberUser", "");
			String pass = props.getProperty("rememberPass", "");
			if (user != null && user.trim().length() > 0) {
				upsert(user.trim(), pass, 0, 0, 0, 0, null, null);
			}
		}
	}

	public static int hoveredSlot(int mx, int my) {
		if (count() == 0) {
			return -1;
		}
		if (my < 18 || my >= 128) {
			return -1;
		}
		int n = count();
		int cardW = cardWidth(n);
		int startX = (PANEL_W - n * cardW) / 2;
		if (mx < startX || mx >= startX + n * cardW) {
			return -1;
		}
		return (mx - startX) / cardW;
	}

	public static boolean hitRemove(int mx, int my, int slot, int n) {
		int cardW = cardWidth(n);
		int startX = (PANEL_W - n * cardW) / 2;
		int x = startX + slot * cardW + 3;
		int w = cardW - 6;
		int rx = x + w - 8;
		int ry = 30;
		return mx >= rx - 6 && mx <= rx + 6 && my >= ry - 8 && my <= ry + 4;
	}

	public static int cardWidth(int n) {
		if (n < 1) {
			n = 1;
		}
		return Math.min(110, (PANEL_W - 16) / n);
	}

	private static int kit(int id) {
		if (IDK.cache == null || id < 0 || id >= IDK.cache.length || IDK.cache[id] == null) {
			return 0;
		}
		return 256 + id;
	}

	private Model kitHead(int value) {
		if (value < 256 || value >= 512 || IDK.cache == null) {
			return null;
		}
		int id = value - 256;
		if (id < 0 || id >= IDK.cache.length || IDK.cache[id] == null || !IDK.cache[id].method539()) {
			return null;
		}
		Model model = IDK.cache[id].method540();
		if (model == null || model.anInt1626 <= 0) {
			return null;
		}
		return model;
	}

	private Model itemHead(int value) {
		if (value < 512) {
			return null;
		}
		ItemDef def = ItemDef.forID(value - 512);
		if (def == null || !def.method192(gender)) {
			return null;
		}
		Model model = def.method194(gender);
		if (model == null || model.anInt1626 <= 0) {
			return null;
		}
		return model;
	}

	private Model buildHeadModel(boolean skipItems) {
		int hair = kit(gender == 1 ? 45 : 7);
		int beard = gender == 1 ? 0 : kit(14);
		int helm = 0;
		boolean fullHelm = false;
		if (hasLook(equipment)) {
			helm = equipment[0];
			if (equipment[8] >= 256 && equipment[8] < 512) {
				hair = equipment[8];
			}
			if (equipment[11] >= 256 && equipment[11] < 512) {
				beard = equipment[11];
			}
			// Check if helmet is a full helmet that hides hair
			if (helm >= 512) {
				ItemDef def = ItemDef.forID(helm - 512);
				if (def != null) {
					int headModel = gender == 1 ? def.anInt197 : def.anInt175;
					if (headModel != -1) {
						fullHelm = true;
					}
				}
			}
		}
		Model[] parts = new Model[4];
		int n = 0;
		if (!skipItems) {
			Model helmModel = itemHead(helm);
			if (helmModel != null) {
				parts[n++] = helmModel;
			}
		}
		if (!fullHelm) {
			Model hairModel = kitHead(hair);
			if (hairModel != null) {
				parts[n++] = hairModel;
			}
			Model beardModel = kitHead(beard);
			if (beardModel != null) {
				parts[n++] = beardModel;
			}
		}
		if (n == 0) {
			return null;
		}
		Model model = n == 1 ? parts[0] : new Model(n, parts);
		int[] lookColors = hasLook(equipment) ? colors : new int[] { 7, 8, 9, 5, 0 };
		for (int i = 0; i < 5; i++) {
			int color = lookColors[i];
			if (color > 0 && color < client.anIntArrayArray1003[i].length) {
				model.method476(client.anIntArrayArray1003[i][0], client.anIntArrayArray1003[i][color]);
				if (i == 1 && color < client.anIntArray1204.length) {
					model.method476(client.anIntArray1204[0], client.anIntArray1204[color]);
				}
			}
		}
		model.method469();
		model.method479(64, 850, -30, -50, -30, true);
		return model;
	}

	public void drawPortrait(int destX, int destY, int w, int h) {
		int[] dest = DrawingArea.pixels;
		int destW = DrawingArea.width;
		int destH = DrawingArea.height;
		int clipT = DrawingArea.topY;
		int clipL = DrawingArea.topX;
		int clipR = DrawingArea.bottomX;
		int clipB = DrawingArea.bottomY;
		int[] buf = new int[w * h];
		DrawingArea.initDrawingArea(h, w, buf);
		Texture.method364();
		DrawingArea.drawPixels(h, 0, 0, 0x0a0806, w);
		try {
			if (!drawLook(w, h, false)) {
				if (!drawLook(w, h, true)) {
					drawHelmet(w / 2, h / 2);
				}
			}
		} catch (Exception e) {
			drawHelmet(w / 2, h / 2);
		}
		for (int row = 0; row < h; row++) {
			int dy = destY + row;
			if (dy < 0 || dy >= destH) {
				continue;
			}
			int dx = destX;
			int src = row * w;
			int len = w;
			if (dx < 0) {
				src -= dx;
				len += dx;
				dx = 0;
			}
			if (dx + len > destW) {
				len = destW - dx;
			}
			if (len > 0) {
				System.arraycopy(buf, src, dest, dy * destW + dx, len);
			}
		}
		DrawingArea.initDrawingArea(destH, destW, dest);
		DrawingArea.setDrawingArea(clipB, clipL, clipR, clipT);
		Texture.method364();
	}

	private boolean drawLook(int w, int h, boolean kitsOnly) {
		if (Texture.anIntArray1470 == null || Texture.anIntArray1471 == null) {
			return false;
		}
		Model model = buildHeadModel(kitsOnly);
		if (model == null) {
			return false;
		}
		// Match the makeover / character-creator head boxes: eye-level,
		// slight 3/4 yaw, tight crop on the face.
		int yaw = 50;
		int pitch = 0;
		int zoom = 1200;
		Texture.textureInt1 = w / 2;
		Texture.textureInt2 = h / 2 + 10;
		int camY = Texture.anIntArray1470[pitch] * zoom >> 16;
		int camZ = Texture.anIntArray1471[pitch] * zoom >> 16;
		model.method482(yaw, 0, pitch, 0, camY, camZ);
		return true;
	}

	static void drawHelmet(int cx, int cy) {
		int x = cx - 12;
		int y = cy - 16;
		DrawingArea.method335(0x1a1610, y, 24, 24, 180, x);
		DrawingArea.drawPixels(6, y + 4, x + 4, 0xb0b0b0, 16);
		DrawingArea.drawPixels(8, y + 10, x + 3, 0x8a8a8a, 18);
		DrawingArea.drawPixels(4, y + 12, x + 7, 0x202020, 10);
		DrawingArea.fillPixels(x, 24, 24, 0x5A4933, y);
	}

	private static String join(int[] values) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				buf.append(',');
			}
			buf.append(values[i]);
		}
		return buf.toString();
	}

	private static void parseInts(String text, int[] dest) {
		if (text == null || text.length() == 0) {
			return;
		}
		int idx = 0;
		int start = 0;
		for (int i = 0; i <= text.length() && idx < dest.length; i++) {
			if (i == text.length() || text.charAt(i) == ',') {
				dest[idx++] = parseInt(text.substring(start, i), 0);
				start = i + 1;
			}
		}
	}

	private static int parseInt(String text, int fallback) {
		if (text == null) {
			return fallback;
		}
		try {
			return Integer.parseInt(text.trim());
		} catch (Exception e) {
			return fallback;
		}
	}
}
