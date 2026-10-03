import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * RuneLite-style NPC indicators: hull, tile, true tile, south-west tile,
 * names, minimap names, and right-click Tag / Untag.
 */
final class NpcIndicators {

	static final int ACTION_TAG_HULL = 1610;
	static final int ACTION_UNTAG_HULL = 1611;
	static final int ACTION_TAG_TILE = 1612;
	static final int ACTION_UNTAG_TILE = 1613;
	static final int ACTION_TAG = ACTION_TAG_HULL;
	static final int ACTION_UNTAG = ACTION_UNTAG_HULL;

	static final int STYLE_HULL = 1;
	static final int STYLE_TILE = 2;

	static int mode;
	static boolean hull = true;
	static boolean tile;
	static boolean trueTile;
	static boolean southWestTile;
	static boolean names;
	static boolean minimapNames;
	static int colorIndex;
	static int rgb = 0x30FF60;

	private static final int[] COLORS = {
			0x00FFFF, 0xFFFF00, 0xFF3030, 0x30FF60, 0xFF40FF, 0xFF981F, 0xFFFFFF
	};
	private static final String[] COLOR_NAMES = {
			"Cyan", "Yellow", "Red", "Green", "Magenta", "Orange", "White"
	};

	private static final HashMap idStyles = new HashMap();
	private static final HashMap nameStyles = new HashMap();

	private static final int MAP = 104;
	private static final int MAX_FOOTPRINTS = 256;
	private static final int[] fpSwX = new int[MAX_FOOTPRINTS];
	private static final int[] fpSwY = new int[MAX_FOOTPRINTS];
	private static final int[] fpSize = new int[MAX_FOOTPRINTS];
	private static final int[] fpColor = new int[MAX_FOOTPRINTS];
	private static int fpCount;

	private static final int[] hullX = new int[256];
	private static final int[] hullY = new int[256];
	private static final int[] projX = new int[512];
	private static final int[] projY = new int[512];
	private static final int[] scanMin = new int[2048];
	private static final int[] scanMax = new int[2048];

	static void load(Properties props) {
		mode = clamp(readInt(props, "npcIndMode", 0), 0, 2);
		hull = readBool(props, "npcIndHull", true);
		tile = readBool(props, "npcIndTile", false);
		trueTile = readBool(props, "npcIndTrueTile", false);
		southWestTile = readBool(props, "npcIndSwTile", false);
		names = readBool(props, "npcIndNames", false);
		minimapNames = readBool(props, "npcIndMinimapNames", false);
		colorIndex = clamp(readInt(props, "npcIndColor", 3), 0, COLORS.length - 1);
		rgb = readInt(props, "npcIndRgb", COLORS[colorIndex]);
		if (rgb <= 0) {
			rgb = COLORS[colorIndex];
		}
		idStyles.clear();
		nameStyles.clear();
		parseNames(props.getProperty("npcIndList", ""));
		parseIds(props.getProperty("npcIndIds", ""));
	}

	static void save(Properties props) {
		props.setProperty("npcIndMode", Integer.toString(mode));
		props.setProperty("npcIndHull", Boolean.toString(hull));
		props.setProperty("npcIndTile", Boolean.toString(tile));
		props.setProperty("npcIndTrueTile", Boolean.toString(trueTile));
		props.setProperty("npcIndSwTile", Boolean.toString(southWestTile));
		props.setProperty("npcIndNames", Boolean.toString(names));
		props.setProperty("npcIndMinimapNames", Boolean.toString(minimapNames));
		props.setProperty("npcIndColor", Integer.toString(colorIndex));
		props.setProperty("npcIndRgb", Integer.toString(rgb));
		props.setProperty("npcIndList", packMap(nameStyles, false));
		props.setProperty("npcIndIds", packMap(idStyles, true));
	}

	static boolean active() {
		return mode > 0;
	}

	static String modeLabel() {
		if (mode == 1) {
			return "Tagged";
		}
		if (mode == 2) {
			return "All";
		}
		return "Off";
	}

	static void cycleMode() {
		mode = (mode + 1) % 3;
	}

	static int color() {
		return rgb == 0 ? COLORS[colorIndex] : rgb;
	}

	static String colorLabel() {
		return "#" + toHex(color());
	}

	static void cycleColor() {
		colorIndex = (colorIndex + 1) % COLORS.length;
		rgb = COLORS[colorIndex];
	}

	static void setRgb(int value) {
		rgb = value & 0xFFFFFF;
		if (rgb == 0) {
			rgb = 1;
		}
	}

	private static String toHex(int value) {
		String hex = Integer.toHexString(value & 0xFFFFFF).toUpperCase();
		while (hex.length() < 6) {
			hex = "0" + hex;
		}
		return hex;
	}

	static void beginFrame() {
		fpCount = 0;
	}

	static int footprintCount() {
		return fpCount;
	}

	static int footprintSwX(int i) {
		return fpSwX[i];
	}

	static int footprintSwY(int i) {
		return fpSwY[i];
	}

	static int footprintSize(int i) {
		return fpSize[i];
	}

	static int footprintColor(int i) {
		return fpColor[i];
	}

	static boolean matches(NPC npc) {
		if (!active() || npc == null || npc.desc == null) {
			return false;
		}
		EntityDef def = resolve(npc.desc);
		if (def == null || def.name == null) {
			return false;
		}
		if (mode == 2) {
			return true;
		}
		return styleFor(def) != 0;
	}

	static boolean isTagged(EntityDef def) {
		def = resolve(def);
		return def != null && styleFor(def) != 0;
	}

	static boolean hasStyle(EntityDef def, int style) {
		return (styleFor(resolve(def)) & style) != 0;
	}

	static boolean showHull(NPC npc) {
		if (npc == null || npc.desc == null) {
			return false;
		}
		if (mode == 2) {
			return hull;
		}
		return (styleFor(resolve(npc.desc)) & STYLE_HULL) != 0;
	}

	static boolean showTile(NPC npc) {
		if (npc == null || npc.desc == null) {
			return false;
		}
		if (mode == 2) {
			return tile;
		}
		return (styleFor(resolve(npc.desc)) & STYLE_TILE) != 0;
	}

	static boolean showTrueTile(NPC npc) {
		if (mode == 2) {
			return trueTile;
		}
		return showTile(npc);
	}

	static boolean showSouthWest(NPC npc) {
		if (mode == 2) {
			return southWestTile;
		}
		return false;
	}

	static String toggleStyle(NPC npc, int style) {
		if (npc == null || npc.desc == null) {
			return null;
		}
		EntityDef def = resolve(npc.desc);
		if (def == null) {
			return null;
		}
		int id = (int) def.interfaceType;
		int next = styleFor(def) ^ style;
		Integer key = Integer.valueOf(id);
		if (next == 0) {
			idStyles.remove(key);
			removeName(def.name);
		} else {
			idStyles.put(key, Integer.valueOf(next));
			if (def.name != null) {
				nameStyles.put(def.name.toLowerCase().trim(), Integer.valueOf(next));
			}
			if (mode == 0) {
				mode = 1;
			}
		}
		String what = style == STYLE_TILE ? "tile" : "hull";
		boolean on = (next & style) != 0;
		if (def.name == null) {
			return (on ? "Tagged " : "Untagged ") + what + ".";
		}
		return (on ? "Tagged " : "Untagged ") + what + " on " + def.name + ".";
	}

	static int addCtrlEntries(client c, String[] names, int[] ids, int[] cmd1, int[] cmd2, int[] cmd3, int row) {
		if (c == null || row <= 0) {
			return row;
		}
		int npcIndex = -1;
		String label = "";
		for (int i = 0; i < row; i++) {
			if (!isNpcAction(ids[i])) {
				continue;
			}
			npcIndex = cmd1[i];
			label = menuTarget(names[i]);
			break;
		}
		if (npcIndex < 0 || c.npcArray == null || npcIndex >= c.npcArray.length) {
			return row;
		}
		NPC npc = c.npcArray[npcIndex];
		if (npc == null || npc.desc == null) {
			return row;
		}
		EntityDef def = resolve(npc.desc);
		boolean hullOn = hasStyle(def, STYLE_HULL);
		boolean tileOn = hasStyle(def, STYLE_TILE);
		if (label.length() == 0 && def != null && def.name != null) {
			label = def.name;
		}
		String target = label.length() > 0 ? " @yel@" + label : "";
		row = prepend(names, ids, cmd1, cmd2, cmd3, row,
				(tileOn ? "Untag tile" : "Tag tile") + target,
				tileOn ? ACTION_UNTAG_TILE : ACTION_TAG_TILE, npcIndex, 0, 0);
		row = prepend(names, ids, cmd1, cmd2, cmd3, row,
				(hullOn ? "Untag hull" : "Tag hull") + target,
				hullOn ? ACTION_UNTAG_HULL : ACTION_TAG_HULL, npcIndex, 0, 0);
		return row;
	}

	static boolean isTagAction(int id) {
		if (id >= 2000) {
			id -= 2000;
		}
		return id == ACTION_TAG_HULL || id == ACTION_UNTAG_HULL || id == ACTION_TAG_TILE || id == ACTION_UNTAG_TILE;
	}

	static void markTiles(NPC npc) {
		if (!active() || npc == null) {
			return;
		}
		boolean footprint = showTile(npc);
		boolean trueFoot = showTrueTile(npc);
		boolean sw = showSouthWest(npc);
		if (!footprint && !trueFoot && !sw) {
			return;
		}
		int size = npc.anInt1540;
		if (size < 1) {
			size = 1;
		}
		int color = color();
		if (footprint) {
			int swX = npc.x - size * 64 >> 7;
			int swY = npc.y - size * 64 >> 7;
			addFootprint(swX, swY, size, color);
		}
		if (trueFoot && mode == 2) {
			addFootprint(npc.smallX[0], npc.smallY[0], size, color);
		}
		if (sw) {
			addFootprint(npc.x - size * 64 >> 7, npc.y - size * 64 >> 7, 1, color);
		}
	}

	/** Legacy per-tile path no longer used — footprints are drawn merged once. */
	static int tileColor(int localX, int localY) {
		return 0;
	}

	private static void addFootprint(int swX, int swY, int size, int color) {
		if (size < 1 || fpCount >= MAX_FOOTPRINTS) {
			return;
		}
		if (swX < 0) {
			size += swX;
			swX = 0;
		}
		if (swY < 0) {
			size += swY;
			swY = 0;
		}
		if (size < 1) {
			return;
		}
		if (swX >= MAP || swY >= MAP) {
			return;
		}
		if (swX + size > MAP) {
			size = MAP - swX;
		}
		if (swY + size > MAP) {
			int lim = MAP - swY;
			if (lim < size) {
				size = lim;
			}
		}
		if (size < 1) {
			return;
		}
		for (int i = 0; i < fpCount; i++) {
			if (fpSwX[i] == swX && fpSwY[i] == swY && fpSize[i] == size) {
				return;
			}
		}
		fpSwX[fpCount] = swX;
		fpSwY[fpCount] = swY;
		fpSize[fpCount] = size;
		fpColor[fpCount] = color;
		fpCount++;
	}

	static void drawHull(client c, NPC npc) {
		drawHull(c, npc, color());
	}

	static void drawHull(client c, NPC npc, int rgb) {
		if (c == null || npc == null) {
			return;
		}
		Model model = npc.getRotatedModel();
		if (model == null || model.anIntArray1627 == null || model.anInt1626 < 3) {
			return;
		}
		int orientation = npc.turnDirection & 0x7ff;
		int sin = Model.modelIntArray1[orientation];
		int cos = Model.modelIntArray2[orientation];
		int count = model.anInt1626;
		int step = 1;
		if (count > 480) {
			step = (count + 479) / 480;
		}
		int n = 0;
		for (int i = 0; i < count && n < projX.length; i += step) {
			int vx = model.anIntArray1627[i];
			int vy = model.anIntArray1628[i];
			int vz = model.anIntArray1629[i];
			int rx = vz * sin + vx * cos >> 16;
			int rz = vz * cos - vx * sin >> 16;
			c.calcEntityScreenPos(npc.x + rx, -vy, npc.y + rz);
			int sx = c.getSpriteDrawX();
			int sy = c.getSpriteDrawY();
			if (sx >= 0 && sy >= 0) {
				projX[n] = sx;
				projY[n] = sy;
				n++;
			}
		}
		int hullN = convexHull(projX, projY, n, hullX, hullY);
		if (hullN < 2) {
			return;
		}
		if (hullN >= 3) {
			fillConvex(hullX, hullY, hullN, rgb, 28);
		}
		for (int i = 0; i < hullN; i++) {
			int j = i + 1 < hullN ? i + 1 : 0;
			DrawingArea.drawLine(hullX[i], hullY[i], hullX[j], hullY[j], rgb);
			DrawingArea.drawLine(hullX[i] + 1, hullY[i], hullX[j] + 1, hullY[j], rgb);
		}
	}

	static void drawName(client c, NPC npc, TextDrawingArea font) {
		if (!names || font == null || npc == null || npc.desc == null || npc.desc.name == null) {
			return;
		}
		c.npcScreenPos(npc, npc.height + 15);
		int x = c.getSpriteDrawX();
		int y = c.getSpriteDrawY();
		if (x < 0 || y < 0) {
			return;
		}
		int w = font.getTextWidth(npc.desc.name);
		int[] p = OverlayManager.placeWorld(x - w / 2, y - 10, w, 12);
		font.drawText(color(), npc.desc.name, p[1] + 10, p[0] + w / 2);
	}

	private static int convexHull(int[] xs, int[] ys, int n, int[] outX, int[] outY) {
		if (n < 2) {
			return 0;
		}
		int left = 0;
		for (int i = 1; i < n; i++) {
			if (xs[i] < xs[left] || xs[i] == xs[left] && ys[i] < ys[left]) {
				left = i;
			}
		}
		int hullN = 0;
		int current = left;
		int guard = 0;
		do {
			if (hullN >= outX.length) {
				break;
			}
			outX[hullN] = xs[current];
			outY[hullN] = ys[current];
			hullN++;
			int next = 0;
			boolean have = false;
			for (int i = 0; i < n; i++) {
				if (!have) {
					next = i;
					have = true;
					continue;
				}
				int cross = (ys[i] - ys[current]) * (xs[next] - xs[i])
						- (xs[i] - xs[current]) * (ys[next] - ys[i]);
				if (cross > 0) {
					next = i;
				} else if (cross == 0) {
					int dI = dist2(xs[current], ys[current], xs[i], ys[i]);
					int dN = dist2(xs[current], ys[current], xs[next], ys[next]);
					if (dI > dN) {
						next = i;
					}
				}
			}
			current = next;
			guard++;
		} while (current != left && guard < n + 2);
		return hullN;
	}

	private static int dist2(int x1, int y1, int x2, int y2) {
		int dx = x2 - x1;
		int dy = y2 - y1;
		return dx * dx + dy * dy;
	}

	private static void fillConvex(int[] xs, int[] ys, int n, int rgb, int alpha) {
		int minY = DrawingArea.height;
		int maxY = 0;
		for (int i = 0; i < n; i++) {
			if (ys[i] < minY) {
				minY = ys[i];
			}
			if (ys[i] > maxY) {
				maxY = ys[i];
			}
		}
		if (minY < DrawingArea.topY) {
			minY = DrawingArea.topY;
		}
		if (maxY >= DrawingArea.bottomY) {
			maxY = DrawingArea.bottomY - 1;
		}
		if (minY > maxY || maxY >= scanMin.length) {
			return;
		}
		for (int y = minY; y <= maxY; y++) {
			scanMin[y] = Integer.MAX_VALUE;
			scanMax[y] = Integer.MIN_VALUE;
		}
		for (int i = 0; i < n; i++) {
			int x0 = xs[i];
			int y0 = ys[i];
			int x1 = xs[i + 1 < n ? i + 1 : 0];
			int y1 = ys[i + 1 < n ? i + 1 : 0];
			if (y0 == y1) {
				continue;
			}
			if (y0 > y1) {
				int t = x0;
				x0 = x1;
				x1 = t;
				t = y0;
				y0 = y1;
				y1 = t;
			}
			int dy = y1 - y0;
			for (int y = y0; y < y1; y++) {
				if (y < minY || y > maxY) {
					continue;
				}
				int x = x0 + (x1 - x0) * (y - y0) / dy;
				if (x < scanMin[y]) {
					scanMin[y] = x;
				}
				if (x > scanMax[y]) {
					scanMax[y] = x;
				}
			}
		}
		for (int y = minY; y <= maxY; y++) {
			if (scanMax[y] >= scanMin[y]) {
				DrawingArea.blendHLine(y, scanMin[y], scanMax[y], rgb, alpha);
			}
		}
	}

	private static boolean nameTagged(String name) {
		return nameStyle(name) != 0;
	}

	private static int nameStyle(String name) {
		if (name == null) {
			return 0;
		}
		String lower = name.toLowerCase().trim();
		Integer exact = (Integer) nameStyles.get(lower);
		if (exact != null) {
			return exact.intValue();
		}
		Iterator it = nameStyles.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			String pattern = String.valueOf(entry.getKey());
			if (wildcard(lower, pattern)) {
				return ((Integer) entry.getValue()).intValue();
			}
		}
		return 0;
	}

	private static int styleFor(EntityDef def) {
		if (def == null) {
			return 0;
		}
		Integer byId = (Integer) idStyles.get(Integer.valueOf((int) def.interfaceType));
		if (byId != null) {
			return byId.intValue();
		}
		return nameStyle(def.name);
	}

	private static EntityDef resolve(EntityDef def) {
		if (def == null) {
			return null;
		}
		if (def.childrenIDs != null) {
			EntityDef child = def.method161();
			if (child != null) {
				return child;
			}
		}
		return def;
	}

	private static boolean isNpcAction(int id) {
		if (id >= 2000) {
			id -= 2000;
		}
		return id == 20 || id == 412 || id == 225 || id == 965 || id == 478 || id == 1025 || id == 582 || id == 413;
	}

	private static String menuTarget(String name) {
		if (name == null) {
			return "";
		}
		int at = name.lastIndexOf("@yel@");
		if (at >= 0) {
			return name.substring(at + 5).trim();
		}
		return "";
	}

	private static int prepend(String[] names, int[] ids, int[] cmd1, int[] cmd2, int[] cmd3, int row, String name, int id, int a, int b, int c) {
		if (row >= names.length) {
			return row;
		}
		for (int i = row; i > 0; i--) {
			names[i] = names[i - 1];
			ids[i] = ids[i - 1];
			cmd1[i] = cmd1[i - 1];
			cmd2[i] = cmd2[i - 1];
			cmd3[i] = cmd3[i - 1];
		}
		names[0] = name;
		ids[0] = id;
		cmd1[0] = a;
		cmd2[0] = b;
		cmd3[0] = c;
		return row + 1;
	}

	private static boolean wildcard(String name, String pattern) {
		if (pattern == null || pattern.length() == 0) {
			return false;
		}
		pattern = pattern.toLowerCase().trim();
		if (pattern.equals("*")) {
			return true;
		}
		if (pattern.indexOf('*') < 0) {
			return name.equals(pattern);
		}
		return name.matches(toRegex(pattern));
	}

	private static String toRegex(String pattern) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < pattern.length(); i++) {
			char ch = pattern.charAt(i);
			if (ch == '*') {
				sb.append(".*");
			} else if (".[]{}()+-?^$|\\".indexOf(ch) >= 0) {
				sb.append('\\').append(ch);
			} else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	private static boolean hasId(int id) {
		return idStyles.containsKey(Integer.valueOf(id));
	}

	private static void removeName(String name) {
		if (name == null) {
			return;
		}
		nameStyles.remove(name.toLowerCase().trim());
	}

	private static String packMap(HashMap map, boolean ids) {
		StringBuffer sb = new StringBuffer();
		Iterator it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(String.valueOf(entry.getKey()));
			sb.append(':');
			sb.append(String.valueOf(entry.getValue()));
		}
		return sb.toString();
	}

	private static void parseNames(String packed) {
		parseStyled(packed, false);
	}

	private static void parseIds(String packed) {
		parseStyled(packed, true);
	}

	private static void parseStyled(String packed, boolean ids) {
		if (packed == null || packed.length() == 0) {
			return;
		}
		String[] parts = packed.split(",");
		for (int i = 0; i < parts.length; i++) {
			String bit = parts[i].trim();
			if (bit.length() == 0) {
				continue;
			}
			int colon = bit.lastIndexOf(':');
			String key = colon >= 0 ? bit.substring(0, colon).trim() : bit;
			int flags = STYLE_HULL;
			if (colon >= 0) {
				try {
					flags = Integer.parseInt(bit.substring(colon + 1).trim());
				} catch (Exception e) {
					flags = STYLE_HULL;
				}
			}
			if (flags == 0) {
				continue;
			}
			if (ids) {
				try {
					idStyles.put(Integer.valueOf(Integer.parseInt(key)), Integer.valueOf(flags));
				} catch (Exception e) {
				}
			} else {
				nameStyles.put(key.toLowerCase(), Integer.valueOf(flags));
			}
		}
	}

	private static int readInt(Properties props, String key, int def) {
		try {
			return Integer.parseInt(props.getProperty(key, Integer.toString(def)));
		} catch (Exception e) {
			return def;
		}
	}

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}

	private static int clamp(int v, int min, int max) {
		if (v < min) {
			return min;
		}
		if (v > max) {
			return max;
		}
		return v;
	}
}
