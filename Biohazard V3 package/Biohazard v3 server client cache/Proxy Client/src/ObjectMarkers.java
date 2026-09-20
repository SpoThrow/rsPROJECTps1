import java.util.ArrayList;
import java.util.Properties;

final class ObjectMarkers {

	static final int ACTION_MARK = 1630;
	static final int ACTION_UNMARK = 1631;

	static boolean enabled;

	private static final ArrayList markers = new ArrayList();
	private static final int[] COLORS = { 0x00FFFF, 0xFFFF00, 0xFF3030, 0x30FF60, 0xFF40FF };

	static void load(Properties props) {
		enabled = readBool(props, "objMarkers", false);
		markers.clear();
		String packed = props.getProperty("objMarkerList", "");
		if (packed == null || packed.length() == 0) {
			return;
		}
		String[] parts = packed.split(";");
		for (int i = 0; i < parts.length; i++) {
			String[] bits = parts[i].split(",");
			if (bits.length < 4) {
				continue;
			}
			try {
				int id = Integer.parseInt(bits[0].trim());
				int x = Integer.parseInt(bits[1].trim());
				int y = Integer.parseInt(bits[2].trim());
				int plane = Integer.parseInt(bits[3].trim());
				int color = bits.length > 4 ? Integer.parseInt(bits[4].trim()) : COLORS[0];
				markers.add(new int[] { id, x, y, plane, color });
			} catch (Exception e) {
			}
		}
	}

	static void save(Properties props) {
		props.setProperty("objMarkers", Boolean.toString(enabled));
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < markers.size(); i++) {
			int[] m = (int[]) markers.get(i);
			if (sb.length() > 0) {
				sb.append(';');
			}
			sb.append(m[0]).append(',').append(m[1]).append(',').append(m[2]).append(',').append(m[3]).append(',')
					.append(m[4]);
		}
		props.setProperty("objMarkerList", sb.toString());
	}

	static boolean isAction(int id) {
		if (id >= 2000) {
			id -= 2000;
		}
		return id == ACTION_MARK || id == ACTION_UNMARK;
	}

	static int colorLocal(int localX, int localY, int plane) {
		if (!enabled) {
			return 0;
		}
		int wx = client.getBaseX() + localX;
		int wy = client.getBaseY() + localY;
		for (int i = 0; i < markers.size(); i++) {
			int[] m = (int[]) markers.get(i);
			if (m[1] == wx && m[2] == wy && m[3] == plane) {
				return m[4];
			}
		}
		return 0;
	}

	static int addAltEntries(client c, String[] names, int[] ids, int[] cmd1, int[] cmd2, int[] cmd3, int row) {
		if (!enabled || row <= 0) {
			return row;
		}
		int uid = -1;
		int lx = 0;
		int ly = 0;
		String label = "";
		for (int i = 0; i < row; i++) {
			if (!isObjectAction(ids[i])) {
				continue;
			}
			uid = cmd1[i];
			lx = cmd2[i];
			ly = cmd3[i];
			label = target(names[i]);
			break;
		}
		if (uid < 0) {
			return row;
		}
		int id = uid >> 14 & 0x7fff;
		int wx = client.getBaseX() + lx;
		int wy = client.getBaseY() + ly;
		boolean marked = indexOf(id, wx, wy, client.scenePlane) >= 0;
		String target = label.length() > 0 ? " @cya@" + label : "";
		if (marked) {
			row = prepend(names, ids, cmd1, cmd2, cmd3, row, "Unmark object" + target, ACTION_UNMARK, uid, lx, ly);
		}
		row = prepend(names, ids, cmd1, cmd2, cmd3, row,
				(marked ? "Marker colour" : "Mark object") + target, ACTION_MARK, uid, lx, ly);
		return row;
	}

	static void handle(client c, int action, int uid, int lx, int ly) {
		int id = uid >> 14 & 0x7fff;
		int wx = client.getBaseX() + lx;
		int wy = client.getBaseY() + ly;
		int plane = client.scenePlane;
		int existing = indexOf(id, wx, wy, plane);
		if (action == ACTION_UNMARK) {
			if (existing >= 0) {
				markers.remove(existing);
				c.pushMessage("Object unmarked.", 0, "");
			}
			return;
		}
		if (existing >= 0) {
			int[] m = (int[]) markers.get(existing);
			int next = 0;
			for (int i = 0; i < COLORS.length; i++) {
				if (COLORS[i] == m[4]) {
					next = (i + 1) % COLORS.length;
					break;
				}
			}
			m[4] = COLORS[next];
			c.pushMessage("Object marker colour changed.", 0, "");
		} else {
			markers.add(new int[] { id, wx, wy, plane, COLORS[0] });
			c.pushMessage("Object marked. Alt-right-click again to unmark.", 0, "");
		}
	}

	private static int indexOf(int id, int x, int y, int plane) {
		for (int i = 0; i < markers.size(); i++) {
			int[] m = (int[]) markers.get(i);
			if (m[0] == id && m[1] == x && m[2] == y && m[3] == plane) {
				return i;
			}
		}
		return -1;
	}

	private static boolean isObjectAction(int id) {
		if (id >= 2000) {
			id -= 2000;
		}
		return id == 502 || id == 900 || id == 113 || id == 872 || id == 1062 || id == 1226 || id == 62 || id == 956;
	}

	private static String target(String name) {
		if (name == null) {
			return "";
		}
		int at = name.lastIndexOf("@cya@");
		if (at >= 0) {
			String rest = name.substring(at + 5).trim();
			int sp = rest.indexOf(" @");
			return sp > 0 ? rest.substring(0, sp) : rest;
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

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}
}
