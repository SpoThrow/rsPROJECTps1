package ui;

import java.util.Properties;import game.client;



/**
 * RuneLite-style overlay layout with snap-corner anchor zones.
 * Hold Alt and drag; cyan 80x80 zones highlight green when the mouse is over
 * them. Drop on a zone to dock and stack; drop elsewhere to float.
 */
public final class OverlayManager {

	public static final int TOP_LEFT = 0;
	static final int TOP_CENTER = 1;
	static final int TOP_RIGHT = 2;
	public static final int BOTTOM_LEFT = 3;
	static final int BOTTOM_RIGHT = 4;
	public static final int ABOVE_CHATBOX = 5;
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

	public static final class Panel {
		String id;
		int defaultPreferred;
		int preferred;
		public int x;
		public int y;
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
		/** True for RuneLite-style infoboxes, the only panels that support Shift right click grouping. */
		boolean infobox;
		/** Group this panel is laid out in; empty means the panel docks on its own. */
		String group = "";
		/** The panel that carries this group's position, or null when ungrouped. */
		Panel anchor;
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

	/* ------------------------------------------------------- infobox groups */

	/**
	 * Infobox groups, in the RuneLite sense: a set of infoboxes that share one position and
	 * stack along one axis.
	 *
	 * <p>The default group is "no group" - those panels dock individually exactly as they always
	 * have. Detaching gives one panel a group of its own; flipping chooses whether that group
	 * stacks vertically or horizontally; dropping one group onto another merges them.
	 *
	 * <p>A group owns its own position ({@link Group#fX} and friends) rather than borrowing it from
	 * a member panel, so a group whose member list changes - a potion boost wearing off, say -
	 * stays put instead of jumping to wherever the next survivor happens to sit.
	 */
	private static final int GROUP_GAP = 2;
	/** Per-frame stacking cursor, seeded from the group's position and advanced as members place. */
	private static final java.util.HashMap<String, int[]> groupCursor = new java.util.HashMap<>();
	private static final java.util.HashMap<String, Group> groups = new java.util.HashMap<>();

	/** A named set of infoboxes that share one position and stack along one axis. */
	public static final class Group {
		String id;
		int fX = -1;
		int fY = -1;
		int rX = -1;
		int rY = -1;
		boolean horizontal;
		/** Extent of the laid-out group, refreshed every frame for clamping and drop merging. */
		int ew;
		int eh;
		/**
		 * Extent of the last completed frame, which is the size the group is reserved at when it is
		 * docked. Docking has to know how much corner space to claim before the group has been laid
		 * out at all, and the previous frame's extent is the only honest estimate of this frame's -
		 * one frame stale, and self-correcting on the next.
		 */
		int lastW;
		int lastH;
		/** This frame's origin, so extents are measured from where the group actually started. */
		int ox;
		int oy;
		/**
		 * Docked zone per mode, or {@link #UNDOCKED}. A group without one floats at its free
		 * position exactly as before.
		 */
		int prefF = UNDOCKED;
		int prefR = UNDOCKED;
	}

	/** No anchor zone: the group floats at {@link Group#fX}/{@link Group#rX} as it always did. */
	private static final int UNDOCKED = -1;

	/**
	 * @param width above-chat / side-safe width used for corner packing
	 * @param height above-chat height (ABOVE_CHATBOX zone sits on this)
	 * @param fullH full client height — clamp/drag may use space below the chatbox
	 */
	public static void begin(int width, int height, int ox, int oy, int overlayTop, int fullW, int fullH,
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
		// Group layout is rebuilt from what is actually placed each frame, so a group whose
		// member list changes between frames simply re-stacks instead of stranding a position.
		groupCursor.clear();
		for (int i = 0; i < panelCount; i++) {
			panels[i].visible = false;
		}
		rebuildZones(chatHidden);
	}

	public static void startHud() {
		groupCursor.clear();
		for (Group g : groups.values()) {
			g.ew = 0;
			g.eh = 0;
		}
		for (int i = 0; i < panelCount; i++) {
			panels[i].placed = false;
		}
		for (int z = 0; z < ZONE_COUNT; z++) {
			packX[z] = cornerX[z];
			packY[z] = cornerY[z];
		}
	}

	public static boolean dragging() {
		return dragging != null;
	}

	public static Panel place(String id, int defaultPreferred, int w, int h) {
		return placeInternal(id, defaultPreferred, w, h);
	}

	private static Panel placeInternal(String id, int defaultPreferred, int w, int h) {
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
		p.anchor = null;
		if (p.group.length() > 0) {
			placeInGroup(p);
			p.placed = true;
			return p;
		}
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

	/**
	 * Positions one member of an infobox group and grows the group's recorded extent.
	 *
	 * <p>The first member placed each frame sits at the group's own position; every member after
	 * it continues along the group's axis. Members therefore keep their order in the group even
	 * when the one that happened to be placed first last frame has since expired.
	 *
	 * <p>A docked group takes its origin from the anchor zone rather than from {@link Group#fX}:
	 * see {@link #groupOrigin}. Members always stack down or right from that origin, and because a
	 * docked origin is the <em>top-left</em> of the box the zone reserved for the group, stacking
	 * down fills exactly that box whether the zone packs from the top or the bottom.
	 */
	private static void placeInGroup(Panel p) {
		Group g = group(p.group);
		int[] cursor = groupCursor.get(p.group);
		if (cursor == null) {
			int[] origin = groupOrigin(g);
			cursor = new int[] { origin[0], origin[1] };
			groupCursor.put(p.group, cursor);
			g.ox = origin[0];
			g.oy = origin[1];
			p.x = origin[0];
			p.y = origin[1];
			p.anchor = p;
		} else {
			p.x = cursor[0];
			p.y = cursor[1];
		}
		if (g.horizontal) {
			cursor[0] = p.x + p.w + GROUP_GAP;
			cursor[1] = p.y;
		} else {
			cursor[0] = p.x;
			cursor[1] = p.y + p.h + GROUP_GAP;
		}
		if (p.x + p.w - g.ox > g.ew) {
			g.ew = p.x + p.w - g.ox;
		}
		if (p.y + p.h - g.oy > g.eh) {
			g.eh = p.y + p.h - g.oy;
		}
		g.lastW = g.ew;
		g.lastH = g.eh;
	}

	private static final Panel dockGhost = new Panel();
	private static final int[] groupOriginOut = new int[2];

	/**
	 * Where the group starts this frame: its anchor zone if it has one, otherwise its free position.
	 *
	 * <p>Docking reuses {@link #dock} through a throwaway panel sized to the group, which is what
	 * makes a docked group stack against the corner <em>with</em> the panels already there rather
	 * than on top of them: {@code dock} advances the same zone packing cursor it advances for an
	 * ordinary panel, so the group claims one slot in the corner's stack and everything docked
	 * afterwards lands below it.
	 *
	 * <p>While the group is the one being dragged it is deliberately treated as undocked, so the
	 * drag moves the group rather than the zone it is currently parked in, and dropping it decides
	 * the zone afresh.
	 */
	private static int[] groupOrigin(Group g) {
		if (groupPref(g) >= 0 && !isDragging(g)) {
			dockGhost.w = g.lastW > 0 ? g.lastW : 1;
			dockGhost.h = g.lastH > 0 ? g.lastH : 1;
			dock(dockGhost, groupPref(g));
			groupOriginOut[0] = dockGhost.x;
			groupOriginOut[1] = dockGhost.y;
			return groupOriginOut;
		}
		groupOriginOut[0] = groupX(g);
		groupOriginOut[1] = groupY(g);
		return groupOriginOut;
	}

	private static boolean isDragging(Group g) {
		return dragging != null && dragging.group.equals(g.id);
	}


	/** @return true when this group stacks sideways rather than downwards. */
	public static boolean isHorizontal(String groupId) {
		return group(groupId).horizontal;
	}

	public static Panel panelAt(int mx, int my) {
		return hit(mx, my);
	}

	private static Group group(String id) {
		Group g = groups.get(id);
		if (g == null) {
			g = new Group();
			g.id = id;
			groups.put(id, g);
		}
		return g;
	}

	private static int groupX(Group g) {
		int v = client.isFixed() ? g.fX : g.rX;
		return v < 0 ? MARGIN : v;
	}

	private static int groupY(Group g) {
		int v = client.isFixed() ? g.fY : g.rY;
		return v < 0 ? topY : v;
	}

	private static void setGroupPos(Group g, int x, int y) {
		if (client.isFixed()) {
			g.fX = x;
			g.fY = y;
		} else {
			g.rX = x;
			g.rY = y;
		}
	}

	/** The group's anchor zone for the mode in play, or {@link #UNDOCKED}. */
	private static int groupPref(Group g) {
		return client.isFixed() ? g.prefF : g.prefR;
	}

	private static void setGroupPref(Group g, int pref) {
		if (client.isFixed()) {
			g.prefF = pref;
		} else {
			g.prefR = pref;
		}
	}

	/**
	 * The group's corner, as the anchor zone names it - {@code null} when the group floats.
	 *
	 * <p>Read by the group outline so it is drawn where the group actually is. It used to be drawn
	 * at {@link Group#fX}, which is the <em>undocked</em> position, so a docked group's cyan box
	 * stayed at whatever corner it was last dragged from while its infoboxes sat somewhere else.
	 */
	private static int[] groupCorner(Group g) {
		if (groupPref(g) >= 0 && !isDragging(g)) {
			groupOriginOut[0] = g.ox;
			groupOriginOut[1] = g.oy;
			return groupOriginOut;
		}
		int x = client.isFixed() ? g.fX : g.rX;
		int y = client.isFixed() ? g.fY : g.rY;
		if (x < 0 || y < 0) {
			return null;
		}
		groupOriginOut[0] = x;
		groupOriginOut[1] = y;
		return groupOriginOut;
	}

	/** Groups currently laid out, for drawing the group outline while Alt is held. */
	public static java.util.Collection<Group> groupList() {
		return groups.values();
	}

	public static void paint(Panel p, int alpha) {
		if (p == null) {
			return;
		}
		// One global colour and opacity for every plugin panel, so fixed and resizable look the
		// same and the player can make the panels read against whatever they are standing on. The
		// per-caller alpha is deliberately ignored: it is what made one overlay darker than another.
		int a = boxOpacity * 256 / 100;
		if (a < 0) {
			a = 0;
		}
		if (a > 256) {
			a = 256;
		}
		DrawingArea.method335(boxColor, p.y, p.w, p.h, a, p.x);
		boolean hi = RSApplet.altIsDown || p == dragging;
		DrawingArea.fillPixels(p.x, p.w, p.h, hi ? 0xFFE14A : 0x5A4933, p.y);
	}

	/* ------------------------------------------------- global panel appearance */

	/** Background colour of every plugin overlay panel, as 0xRRGGBB. */
	public static int boxColor = 0x000000;
	/** Background opacity of every plugin overlay panel, as a percentage. */
	public static int boxOpacity = 60;

	private static final int[] BOX_COLORS = { 0x000000, 0x1E1E1E, 0x2B2319, 0x3A2418, 0x102030 };
	private static final String[] BOX_COLOR_NAMES = { "Black", "Charcoal", "Brown", "Rust", "Navy" };

	public static void cycleBoxOpacity() {
		boxOpacity += 15;
		if (boxOpacity > 100) {
			boxOpacity = 30;
		}
	}

	public static String boxColorName() {
		for (int i = 0; i < BOX_COLORS.length; i++) {
			if (BOX_COLORS[i] == boxColor) {
				return BOX_COLOR_NAMES[i];
			}
		}
		return String.format("#%06X", Integer.valueOf(boxColor & 0xFFFFFF));
	}

	public static void cycleBoxColor() {
		int next = 0;
		for (int i = 0; i < BOX_COLORS.length; i++) {
			if (BOX_COLORS[i] == boxColor) {
				next = (i + 1) % BOX_COLORS.length;
				break;
			}
		}
		boxColor = BOX_COLORS[next];
	}

	public static void drawAnchors() {
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
		// Outline each infobox group so its members read as one unit while the player arranges
		// them. Drawn from the extents recorded during the layout pass, at the group's own origin
		// so a docked group's outline follows it to the corner it is anchored to.
		for (Group g : groups.values()) {
			if (g.ew <= 0 || g.eh <= 0) {
				continue;
			}
			int[] at = groupCorner(g);
			if (at == null) {
				continue;
			}
			DrawingArea.fillPixels(at[0] - 2, g.ew + 4, g.eh + 4, 0x00FFFF, at[1] - 2);
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

	public static boolean processDrag(client c) {
		if (c == null) {
			return false;
		}
		mouseX = c.mouseX - originX;
		mouseY = c.mouseY - originY;
		if (dragging != null) {
			if (c.clickMode2 != 1 || !RSApplet.altIsDown) {
				endDrag(c);
				return false;
			}
			moveDrag(mouseX, mouseY);
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
		if (hit.group.length() > 0) {
			// Grabbing any member moves the group, so the offset is measured against the group's
			// own origin rather than the member's, and the whole stack slides rigidly.
			Group g = group(hit.group);
			grabX = sx - groupX(g);
			grabY = sy - groupY(g);
		} else {
			grabX = sx - hit.x;
			grabY = sy - hit.y;
		}
		moveDrag(mouseX, mouseY);
		hoverZone = zoneAt(mouseX, mouseY);
		c.clickMode3 = 0;
		return true;
	}

	private static void moveDrag(int mx, int my) {
		if (dragging.group.length() > 0) {
			Group g = group(dragging.group);
			int gw = g.ew > 0 ? g.ew : dragging.w;
			int gh = g.eh > 0 ? g.eh : dragging.h;
			int nx = mx - grabX;
			int ny = my - grabY;
			if (nx + gw > canvasW) {
				nx = Math.max(0, canvasW - gw);
			}
			if (ny + gh > canvasH) {
				ny = Math.max(0, canvasH - gh);
			}
			if (nx < 0) {
				nx = 0;
			}
			if (ny < 0) {
				ny = 0;
			}
			setGroupPos(g, nx, ny);
			// The members are repositioned from the group on the next layout pass; mirroring the
			// dragged panel keeps pre-layout hit tests and the outline in step.
			dragging.x = groupX(g);
			dragging.y = groupY(g);
			return;
		}
		dragging.x = mx - grabX;
		dragging.y = my - grabY;
		clamp(dragging);
	}

	private static void endDrag(client c) {
		if (dragging.group.length() > 0) {
			// A group is dropped like a panel: on a zone it anchors there, anywhere else it floats
			// where it was let go. Dragging deliberately ignored the zone it was already in (see
			// groupOrigin), so the drop is the only thing that decides it.
			Group g = group(dragging.group);
			dragging = null;
			hoverZone = -1;
			// Dropping a group on an infobox that is not already in it absorbs that infobox,
			// which is how two loose infoboxes get merged into one group.
			if (!mergeAt(mouseX, mouseY, g.id)) {
				setGroupPref(g, zoneAt(mouseX, mouseY));
			}
			c.saveClientSettings();
			return;
		}
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
	}

	/* ------------------------------------------------- infobox group actions */

	public static final int ACTION_DETACH = 1540;
	public static final int ACTION_FLIP = 1541;
	public static final int ACTION_DELETE_GROUP = 1542;

	/** @return the infobox under the cursor that is not part of {@code gid}, if any. */
	private static Panel hitOutside(int mx, int my, String gid) {
		for (int i = panelCount - 1; i >= 0; i--) {
			Panel p = panels[i];
			if (!p.visible || !p.infobox || p.group.equals(gid)) {
				continue;
			}
			if (mx >= p.x && my >= p.y && mx < p.x + p.w && my < p.y + p.h) {
				return p;
			}
		}
		return null;
	}

	/** @return true when {@code gid}'s members were absorbed into another group. */
	private static boolean mergeAt(int mx, int my, String gid) {
		Panel other = hitOutside(mx, my, gid);
		if (other == null) {
			return false;
		}
		if (other.group.length() == 0) {
			// The docked infobox joins the group we just dropped on it.
			other.group = gid;
			return true;
		}
		String target = other.group;
		for (int i = 0; i < panelCount; i++) {
			if (panels[i].group.equals(gid)) {
				panels[i].group = target;
			}
		}
		groups.remove(gid);
		return true;
	}

	/**
	 * Adds the Shift right click entries for an infobox.
	 *
	 * <p>An infobox on its own can be detached into a group of its own; once it is in a group the
	 * options become Flip (swap the group between vertical and horizontal) and Delete (return
	 * every infobox in the group to the default, docked behaviour).
	 */
	public static boolean buildInfoboxMenu(client c) {
		if (c == null || !RSApplet.shiftIsDown) {
			return false;
		}
		Panel p = hit(c.mouseX - originX, c.mouseY - originY);
		if (p == null || !p.infobox) {
			return false;
		}
		if (p.group.length() == 0) {
			c.menuActionName[c.menuActionRow] = "Detach infobox";
			c.menuActionID[c.menuActionRow] = ACTION_DETACH;
			c.menuActionCmd1[c.menuActionRow] = index(p);
			c.menuActionRow++;
			return true;
		}
		c.menuActionName[c.menuActionRow] = "Flip " + (isHorizontal(p.group) ? "to vertical" : "to horizontal");
		c.menuActionID[c.menuActionRow] = ACTION_FLIP;
		c.menuActionCmd1[c.menuActionRow] = index(p);
		c.menuActionRow++;
		c.menuActionName[c.menuActionRow] = "Delete group";
		c.menuActionID[c.menuActionRow] = ACTION_DELETE_GROUP;
		c.menuActionCmd1[c.menuActionRow] = index(p);
		c.menuActionRow++;
		return true;
	}

	public static boolean doAction(client c, int action, int cmd1) {
		if (action != ACTION_DETACH && action != ACTION_FLIP && action != ACTION_DELETE_GROUP) {
			return false;
		}
		if (cmd1 < 0 || cmd1 >= panelCount) {
			return false;
		}
		Panel p = panels[cmd1];
		if (action == ACTION_DETACH) {
			p.group = p.id;
			Group g = group(p.id);
			// Carry the infobox's current spot into both layouts so the detached group appears
			// where the player was looking rather than jumping to a corner.
			g.fX = p.x;
			g.fY = p.y;
			g.rX = p.x;
			g.rY = p.y;
			// Detaching makes a floating group: it inherits the source panel's *position*, not its
			// anchor, so it does not silently become a docked group the player never chose.
			g.prefF = UNDOCKED;
			g.prefR = UNDOCKED;
		} else if (action == ACTION_FLIP) {
			Group g = group(p.group);
			g.horizontal = !g.horizontal;
		} else {
			String gid = p.group;
			for (int i = 0; i < panelCount; i++) {
				if (panels[i].group.equals(gid)) {
					panels[i].group = "";
					panels[i].anchor = null;
				}
			}
			groups.remove(gid);
		}
		if (c != null) {
			c.saveClientSettings();
		}
		return true;
	}

	private static int index(Panel p) {
		for (int i = 0; i < panelCount; i++) {
			if (panels[i] == p) {
				return i;
			}
		}
		return -1;
	}

	public static void load(Properties props) {
		ensureKnown();
		boxColor = read(props, "ov.boxColor", 0x000000);
		boxOpacity = read(props, "ov.boxOpacity", 60);
		if (boxOpacity < 0) {
			boxOpacity = 0;
		}
		if (boxOpacity > 100) {
			boxOpacity = 100;
		}
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
			p.group = props.getProperty("ov.groupOf." + p.id, "");
		}
		// Group positions are read back from the saved panel membership, so a stale group entry
		// with no infoboxes left in it is simply never looked up.
		for (int i = 0; i < panelCount; i++) {
			String gid = panels[i].group;
			if (gid.length() == 0) {
				continue;
			}
			Group g = group(gid);
			g.fX = read(props, "ov.grp." + gid + ".fx", -1);
			g.fY = read(props, "ov.grp." + gid + ".fy", -1);
			g.rX = read(props, "ov.grp." + gid + ".rx", -1);
			g.rY = read(props, "ov.grp." + gid + ".ry", -1);
			g.horizontal = read(props, "ov.grp." + gid + ".hz", 0) != 0;
			g.prefF = read(props, "ov.grp." + gid + ".pf", UNDOCKED);
			g.prefR = read(props, "ov.grp." + gid + ".pr", UNDOCKED);
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

	public static void save(Properties props) {
		ensureKnown();
		props.setProperty("ov.boxColor", Integer.toString(boxColor));
		props.setProperty("ov.boxOpacity", Integer.toString(boxOpacity));
		for (int i = 0; i < panelCount; i++) {
			Panel p = panels[i];
			props.setProperty(key("f", p.id, "x"), Integer.toString(p.userFX));
			props.setProperty(key("f", p.id, "y"), Integer.toString(p.userFY));
			props.setProperty(key("r", p.id, "x"), Integer.toString(p.userRX));
			props.setProperty(key("r", p.id, "y"), Integer.toString(p.userRY));
			props.setProperty(key("f", p.id, "p"), Integer.toString(p.prefF));
			props.setProperty(key("r", p.id, "p"), Integer.toString(p.prefR));
			props.setProperty("ov.groupOf." + p.id, p.group);
		}
		for (Group g : groups.values()) {
			props.setProperty("ov.grp." + g.id + ".fx", Integer.toString(g.fX));
			props.setProperty("ov.grp." + g.id + ".fy", Integer.toString(g.fY));
			props.setProperty("ov.grp." + g.id + ".rx", Integer.toString(g.rX));
			props.setProperty("ov.grp." + g.id + ".ry", Integer.toString(g.rY));
			props.setProperty("ov.grp." + g.id + ".hz", g.horizontal ? "1" : "0");
			props.setProperty("ov.grp." + g.id + ".pf", Integer.toString(g.prefF));
			props.setProperty("ov.grp." + g.id + ".pr", Integer.toString(g.prefR));
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
