import java.util.Properties;

/**
 * RuneLite-style overlay layout with snap-corner anchor zones.
 * Hold Alt and drag; cyan 80x80 zones highlight green when the mouse is over
 * them. Drop on a zone to dock and stack; drop elsewhere to float.
 */
final class OverlayManager {

	static final int TOP_LEFT = 0;
	static final int TOP_CENTER = 1;
	static final int TOP_RIGHT = 2;
	static final int BOTTOM_LEFT = 3;
	static final int BOTTOM_RIGHT = 4;
	static final int ABOVE_CHATBOX = 5;
	static final int CANVAS_TOP_RIGHT = 6;
	static final int DYNAMIC = 7;
	private static final int ZONE_COUNT = 7;

	private static final int MARGIN = 6;
	private static final int BORDER = 5;
	private static final int BORDER_TOP = 20;
	private static final int PADDING = 2;
	private static final int ZONE_SIZE = 80;
	private static final int ZONE_IDLE = 50;
	private static final int ZONE_HOT = 90;

	static final class Panel {
		String id;
		int defaultPreferred;
		int preferred;
		int x;
		int y;
		int w;
		int h;
		int userFX = -1;
		int userFY = -1;
		int userRX = -1;
		int userRY = -1;
		int prefF = -1;
		int prefR = -1;
		boolean visible;
		boolean placed;
	}

	private static Panel[] panels = new Panel[16];
	private static int panelCount;
	private static int viewW = 512;
	private static int viewH = 334;
	private static int canvasW = 512;
	private static int canvasH = 334;
	private static int originX;
	private static int originY;
	private static int topY = 8;
	private static int chatW = 519;
	private static Panel dragging;
	private static int grabX;
	private static int grabY;
	private static int mouseX;
	private static int mouseY;
	private static int hoverZone = -1;

	private static final int[] cornerX = new int[ZONE_COUNT];
	private static final int[] cornerY = new int[ZONE_COUNT];
	private static final int[] packX = new int[ZONE_COUNT];
	private static final int[] packY = new int[ZONE_COUNT];
	private static final int[] zoneX = new int[ZONE_COUNT];
	private static final int[] zoneY = new int[ZONE_COUNT];
	private static final boolean[] zoneOn = new boolean[ZONE_COUNT];

	private static final int[] worldOut = new int[2];
	private static final int[] wx = new int[160];
	private static final int[] wy = new int[160];
	private static final int[] ww = new int[160];
	private static final int[] wh = new int[160];
	private static int worldN;

	/**
	 * @param width above-chat / side-safe width used for corner packing
	 * @param height above-chat height (ABOVE_CHATBOX zone sits on this)
	 * @param fullH full client height — clamp/drag may use space below the chatbox
	 */
	static void begin(int width, int height, int ox, int oy, int overlayTop, int fullW, int fullH,
			int chatWidth, boolean chatHidden) {
		viewW = width < 64 ? 64 : width;
		viewH = height < 64 ? 64 : height;
		canvasW = fullW < viewW ? viewW : fullW;
		canvasH = fullH < viewH ? viewH : fullH;
		originX = ox;
		originY = oy;
		topY = overlayTop < 0 ? BORDER_TOP : overlayTop;
		chatW = chatWidth > 0 ? chatWidth : 519;
		worldN = 0;
		hoverZone = -1;
		draggingKeep();
		for (int i = 0; i < panelCount; i++) {
			panels[i].visible = false;
		}
		rebuildZones(chatHidden);
	}

	static void startHud() {
		for (int i = 0; i < panelCount; i++) {
			panels[i].placed = false;
		}
		for (int z = 0; z < ZONE_COUNT; z++) {
			packX[z] = cornerX[z];
			packY[z] = cornerY[z];
		}
	}

	static boolean dragging() {
		return dragging != null;
	}

	static Panel place(String id, int defaultPreferred, int w, int h) {
		if (w < 8) {
			w = 8;
		}
		if (h < 8) {
			h = 8;
		}
		Panel p = get(id);
		p.defaultPreferred = defaultPreferred;
		p.preferred = effectivePref(p);
		p.w = w;
		p.h = h;
		p.visible = true;
		if (p == dragging) {
			clamp(p);
			p.placed = true;
			return p;
		}
		if (p.preferred == DYNAMIC) {
			int ux = userX(p);
			int uy = userY(p);
			p.x = ux < 0 ? MARGIN : ux;
			p.y = uy < 0 ? topY : uy;
			clamp(p);
			p.placed = true;
			return p;
		}
		dock(p, p.preferred);
		p.placed = true;
		return p;
	}

	static void paint(Panel p, int alpha) {
		if (p == null) {
			return;
		}
		DrawingArea.method335(0x000000, p.y, p.w, p.h, alpha, p.x);
		boolean hi = RSApplet.altIsDown || p == dragging;
		DrawingArea.fillPixels(p.x, p.w, p.h, hi ? 0xFFE14A : 0x5A4933, p.y);
	}

	static void drawAnchors() {
		if (dragging == null && !RSApplet.altIsDown) {
			return;
		}
		for (int z = 0; z < ZONE_COUNT; z++) {
			if (!zoneOn[z]) {
				continue;
			}
			boolean hot = hoverZone == z;
			DrawingArea.method335(hot ? 0x00FF00 : 0x00FFFF, zoneY[z], ZONE_SIZE, ZONE_SIZE, hot ? ZONE_HOT : ZONE_IDLE, zoneX[z]);
			DrawingArea.fillPixels(zoneX[z], ZONE_SIZE, ZONE_SIZE, hot ? 0x33FF33 : 0x33CCCC, zoneY[z]);
		}
	}

	static int[] placeWorld(int x, int y, int w, int h) {
		if (w < 1) {
			w = 1;
		}
		if (h < 1) {
			h = 1;
		}
		int guard = 0;
		while (worldHits(x, y, w, h) && guard < 48) {
			y -= 12;
			guard++;
		}
		if (worldN < wx.length) {
			wx[worldN] = x;
			wy[worldN] = y;
			ww[worldN] = w;
			wh[worldN] = h;
			worldN++;
		}
		worldOut[0] = x;
		worldOut[1] = y;
		return worldOut;
	}

	static boolean processDrag(client c) {
		if (c == null) {
			return false;
		}
		mouseX = c.mouseX - originX;
		mouseY = c.mouseY - originY;
		if (dragging != null) {
			if (c.clickMode2 != 1 || !RSApplet.altIsDown) {
				int zone = zoneAt(mouseX, mouseY);
				if (zone >= 0) {
					int current = effectivePref(dragging);
					if (zone == current && userX(dragging) < 0) {
						setPref(dragging, -1);
					} else {
						setPref(dragging, zone);
					}
					clearUser(dragging);
				} else {
					setPref(dragging, DYNAMIC);
					setUser(dragging, dragging.x, dragging.y);
				}
				dragging = null;
				hoverZone = -1;
				c.saveClientSettings();
				return false;
			}
			dragging.x = mouseX - grabX;
			dragging.y = mouseY - grabY;
			clamp(dragging);
			hoverZone = zoneAt(mouseX, mouseY);
			c.clickMode3 = 0;
			return true;
		}
		if (!RSApplet.altIsDown) {
			return false;
		}
		if (c.clickMode2 != 1 && c.clickMode3 != 1) {
			return false;
		}
		int sx = c.clickMode3 == 1 ? c.saveClickX - originX : mouseX;
		int sy = c.clickMode3 == 1 ? c.saveClickY - originY : mouseY;
		Panel hit = hit(sx, sy);
		if (hit == null) {
			return false;
		}
		dragging = hit;
		grabX = sx - hit.x;
		grabY = sy - hit.y;
		hit.x = mouseX - grabX;
		hit.y = mouseY - grabY;
		clamp(hit);
		hoverZone = zoneAt(mouseX, mouseY);
		c.clickMode3 = 0;
		return true;
	}

	static void load(Properties props) {
		ensureKnown();
		for (int i = 0; i < panelCount; i++) {
			Panel p = panels[i];
			p.userFX = read(props, key("f", p.id, "x"), -1);
			p.userFY = read(props, key("f", p.id, "y"), -1);
			p.userRX = read(props, key("r", p.id, "x"), -1);
			p.userRY = read(props, key("r", p.id, "y"), -1);
			p.prefF = read(props, key("f", p.id, "p"), -2);
			p.prefR = read(props, key("r", p.id, "p"), -2);
			if (p.prefF == -2) {
				p.prefF = p.userFX >= 0 ? DYNAMIC : -1;
			}
			if (p.prefR == -2) {
				p.prefR = p.userRX >= 0 ? DYNAMIC : -1;
			}
		}
		Panel xp = get("xpTracker");
		if (xp.userFX < 0) {
			int sx = read(props, "xpTrackerFixedX", -1);
			int sy = read(props, "xpTrackerFixedY", -1);
			if (sx >= 0 && sy >= 0) {
				xp.userFX = sx - 4;
				xp.userFY = sy - 4;
				if (xp.prefF < 0) {
					xp.prefF = DYNAMIC;
				}
			}
		}
		if (xp.userRX < 0) {
			xp.userRX = read(props, "xpTrackerResizeX", -1);
			xp.userRY = read(props, "xpTrackerResizeY", -1);
			if (xp.userRX >= 0 && xp.prefR < 0) {
				xp.prefR = DYNAMIC;
			}
		}
	}

	static void save(Properties props) {
		ensureKnown();
		for (int i = 0; i < panelCount; i++) {
			Panel p = panels[i];
			props.setProperty(key("f", p.id, "x"), Integer.toString(p.userFX));
			props.setProperty(key("f", p.id, "y"), Integer.toString(p.userFY));
			props.setProperty(key("r", p.id, "x"), Integer.toString(p.userRX));
			props.setProperty(key("r", p.id, "y"), Integer.toString(p.userRY));
			props.setProperty(key("f", p.id, "p"), Integer.toString(p.prefF));
			props.setProperty(key("r", p.id, "p"), Integer.toString(p.prefR));
		}
		Panel xp = get("xpTracker");
		props.setProperty("xpTrackerFixedX", Integer.toString(xp.userFX < 0 ? -1 : xp.userFX + 4));
		props.setProperty("xpTrackerFixedY", Integer.toString(xp.userFY < 0 ? -1 : xp.userFY + 4));
		props.setProperty("xpTrackerResizeX", Integer.toString(xp.userRX));
		props.setProperty("xpTrackerResizeY", Integer.toString(xp.userRY));
	}

	private static void rebuildZones(boolean chatHidden) {
		boolean resize = !client.isFixed();
		// Viewport = game area excluding chat + side HUD (OSRS / RuneLite style).
		int viewportBottom = viewH - BORDER;
		if (viewportBottom < BORDER_TOP + ZONE_SIZE) {
			viewportBottom = BORDER_TOP + ZONE_SIZE;
		}
		cornerX[TOP_LEFT] = BORDER;
		cornerY[TOP_LEFT] = BORDER_TOP;
		cornerX[TOP_CENTER] = viewW / 2;
		cornerY[TOP_CENTER] = BORDER;
		// Top-right of the viewport — left of minimap/inventory column.
		cornerX[TOP_RIGHT] = viewW - BORDER;
		cornerY[TOP_RIGHT] = BORDER;
		// Bottom docks sit on the viewport floor (above chat), not under the chatbox.
		cornerX[BOTTOM_LEFT] = BORDER;
		cornerY[BOTTOM_LEFT] = viewportBottom;
		// Bottom-right of the viewport — left of the inventory panel, above chat.
		cornerX[BOTTOM_RIGHT] = viewW - BORDER;
		cornerY[BOTTOM_RIGHT] = viewportBottom;
		if (resize) {
			// Directly above the right side of the chatbox.
			int chatRight = chatHidden ? viewW - BORDER : Math.min(chatW - BORDER, viewW - BORDER);
			if (chatRight < BORDER + ZONE_SIZE) {
				chatRight = BORDER + ZONE_SIZE;
			}
			cornerX[ABOVE_CHATBOX] = chatRight;
			cornerY[ABOVE_CHATBOX] = viewportBottom;
			zoneOn[ABOVE_CHATBOX] = !chatHidden;
			// Absolute canvas top-right (RuneLite CANVAS_TOP_RIGHT).
			cornerX[CANVAS_TOP_RIGHT] = canvasW - BORDER;
			cornerY[CANVAS_TOP_RIGHT] = BORDER;
			zoneOn[CANVAS_TOP_RIGHT] = true;
		} else {
			cornerX[ABOVE_CHATBOX] = cornerX[BOTTOM_RIGHT];
			cornerY[ABOVE_CHATBOX] = cornerY[BOTTOM_RIGHT];
			cornerX[CANVAS_TOP_RIGHT] = cornerX[TOP_RIGHT];
			cornerY[CANVAS_TOP_RIGHT] = cornerY[TOP_RIGHT];
			zoneOn[ABOVE_CHATBOX] = false;
			zoneOn[CANVAS_TOP_RIGHT] = false;
		}
		zoneOn[TOP_LEFT] = true;
		zoneOn[TOP_CENTER] = true;
		zoneOn[TOP_RIGHT] = true;
		zoneOn[BOTTOM_LEFT] = true;
		zoneOn[BOTTOM_RIGHT] = true;
		placeZone(TOP_LEFT, 0, 0);
		placeZone(TOP_CENTER, -ZONE_SIZE / 2, 0);
		placeZone(TOP_RIGHT, -ZONE_SIZE, 0);
		placeZone(BOTTOM_LEFT, 0, -ZONE_SIZE);
		placeZone(BOTTOM_RIGHT, -ZONE_SIZE, -ZONE_SIZE);
		placeZone(ABOVE_CHATBOX, -ZONE_SIZE, -ZONE_SIZE);
		placeZone(CANVAS_TOP_RIGHT, -ZONE_SIZE, 0);
	}

	private static void placeZone(int z, int dx, int dy) {
		zoneX[z] = cornerX[z] + dx;
		zoneY[z] = cornerY[z] + dy;
		if (zoneX[z] < 0) {
			zoneX[z] = 0;
		}
		if (zoneY[z] < 0) {
			zoneY[z] = 0;
		}
		if (zoneX[z] + ZONE_SIZE > canvasW) {
			zoneX[z] = Math.max(0, canvasW - ZONE_SIZE);
		}
		if (zoneY[z] + ZONE_SIZE > canvasH) {
			zoneY[z] = Math.max(0, canvasH - ZONE_SIZE);
		}
	}

	private static void dock(Panel p, int pref) {
		pref = corrected(pref);
		if (pref < 0 || pref >= ZONE_COUNT) {
			pref = TOP_LEFT;
		}
		int tx = packX[pref];
		int ty = packY[pref];
		int dx = 0;
		int dy = 0;
		if (pref == TOP_CENTER) {
			dx = -p.w / 2;
		} else if (pref == BOTTOM_LEFT) {
			dy = -p.h;
		} else if (pref == BOTTOM_RIGHT || pref == ABOVE_CHATBOX) {
			dx = -p.w;
			dy = -p.h;
		} else if (pref == TOP_RIGHT || pref == CANVAS_TOP_RIGHT) {
			dx = -p.w;
		}
		p.x = tx + dx;
		p.y = ty + dy;
		clamp(p);
		shiftCorner(pref, p);
	}

	private static void shiftCorner(int pref, Panel p) {
		if (pref == BOTTOM_LEFT) {
			packX[pref] = Math.max(packX[pref], p.x + p.w + PADDING);
		} else if (pref == BOTTOM_RIGHT) {
			packX[pref] = Math.min(packX[pref], p.x - PADDING);
		} else if (pref == ABOVE_CHATBOX) {
			packY[pref] = Math.min(packY[pref], p.y - PADDING);
		} else {
			packY[pref] = Math.max(packY[pref], p.y + p.h + PADDING);
		}
	}

	private static int zoneAt(int mx, int my) {
		for (int z = 0; z < ZONE_COUNT; z++) {
			if (!zoneOn[z]) {
				continue;
			}
			if (mx >= zoneX[z] && my >= zoneY[z] && mx < zoneX[z] + ZONE_SIZE && my < zoneY[z] + ZONE_SIZE) {
				return z;
			}
		}
		return -1;
	}

	private static int corrected(int pref) {
		if (client.isFixed()) {
			if (pref == ABOVE_CHATBOX) {
				return BOTTOM_RIGHT;
			}
			if (pref == CANVAS_TOP_RIGHT) {
				return TOP_RIGHT;
			}
		}
		return pref;
	}

	private static int effectivePref(Panel p) {
		int saved = client.isFixed() ? p.prefF : p.prefR;
		if (saved == DYNAMIC) {
			return DYNAMIC;
		}
		if (saved >= 0 && saved < ZONE_COUNT) {
			return corrected(saved);
		}
		return corrected(p.defaultPreferred);
	}

	private static void ensureKnown() {
		String[] ids = { "xpTracker", "npcHealth", "boosted", "statusTimers", "cannon", "poison",
				"barrows", "slayer", "ammo", "bosses", "attackStyle", "performance", "quickPray" };
		for (int i = 0; i < ids.length; i++) {
			get(ids[i]);
		}
	}

	private static Panel get(String id) {
		for (int i = 0; i < panelCount; i++) {
			if (panels[i].id.equals(id)) {
				return panels[i];
			}
		}
		if (panelCount == panels.length) {
			Panel[] next = new Panel[panels.length * 2];
			System.arraycopy(panels, 0, next, 0, panels.length);
			panels = next;
		}
		Panel p = new Panel();
		p.id = id;
		p.defaultPreferred = TOP_LEFT;
		panels[panelCount++] = p;
		return p;
	}

	private static void draggingKeep() {
		if (dragging == null) {
			return;
		}
		for (int i = 0; i < panelCount; i++) {
			if (panels[i] == dragging) {
				return;
			}
		}
		dragging = null;
	}

	private static boolean worldHits(int x, int y, int w, int h) {
		for (int i = 0; i < worldN; i++) {
			if (rects(x, y, w, h, wx[i], wy[i], ww[i], wh[i], 2)) {
				return true;
			}
		}
		for (int i = 0; i < panelCount; i++) {
			Panel o = panels[i];
			if (!o.placed) {
				continue;
			}
			if (rects(x, y, w, h, o.x, o.y, o.w, o.h, 2)) {
				return true;
			}
		}
		return false;
	}

	private static boolean rects(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2, int gap) {
		return x1 < x2 + w2 + gap && x1 + w1 + gap > x2 && y1 < y2 + h2 + gap && y1 + h1 + gap > y2;
	}

	private static Panel hit(int mx, int my) {
		for (int i = panelCount - 1; i >= 0; i--) {
			Panel p = panels[i];
			if (!p.visible) {
				continue;
			}
			if (mx >= p.x && my >= p.y && mx < p.x + p.w && my < p.y + p.h) {
				return p;
			}
		}
		return null;
	}

	private static void clamp(Panel p) {
		int maxW = canvasW;
		int maxH = canvasH;
		if (p.x < 0) {
			p.x = 0;
		}
		if (p.y < 0) {
			p.y = 0;
		}
		if (p.x + p.w > maxW) {
			p.x = Math.max(0, maxW - p.w);
		}
		if (p.y + p.h > maxH) {
			p.y = Math.max(0, maxH - p.h);
		}
	}

	private static int userX(Panel p) {
		return client.isFixed() ? p.userFX : p.userRX;
	}

	private static int userY(Panel p) {
		return client.isFixed() ? p.userFY : p.userRY;
	}

	private static void setUser(Panel p, int x, int y) {
		if (client.isFixed()) {
			p.userFX = x;
			p.userFY = y;
		} else {
			p.userRX = x;
			p.userRY = y;
		}
	}

	private static void clearUser(Panel p) {
		if (client.isFixed()) {
			p.userFX = -1;
			p.userFY = -1;
		} else {
			p.userRX = -1;
			p.userRY = -1;
		}
	}

	private static void setPref(Panel p, int pref) {
		if (client.isFixed()) {
			p.prefF = pref;
		} else {
			p.prefR = pref;
		}
		p.preferred = pref < 0 ? p.defaultPreferred : pref;
	}

	private static String key(String mode, String id, String axis) {
		return "ov." + mode + "." + id + "." + axis;
	}

	private static int read(Properties props, String key, int def) {
		try {
			return Integer.parseInt(props.getProperty(key, Integer.toString(def)));
		} catch (Exception e) {
			return def;
		}
	}
}
