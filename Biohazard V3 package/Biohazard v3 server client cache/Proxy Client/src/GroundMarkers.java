import java.util.ArrayList;
import java.util.Properties;

final class GroundMarkers {

	static boolean enabled;
	static boolean minimap = true;

	private static final ArrayList markers = new ArrayList();
	private static final int[] COLORS = { 0xFFFF00, 0xFF3030, 0x3090FF, 0x30FF60, 0xFF40FF };

	static void load(Properties props) {
		markers.clear();
		String packed = props.getProperty("groundMarkerList", "");
		if (packed == null || packed.length() == 0) {
			return;
		}
		String[] parts = packed.split(";");
		for (int i = 0; i < parts.length; i++) {
			String[] bits = parts[i].split(",");
			if (bits.length < 3) {
				continue;
			}
			try {
				int x = Integer.parseInt(bits[0].trim());
				int y = Integer.parseInt(bits[1].trim());
				int plane = Integer.parseInt(bits[2].trim());
				int color = bits.length > 3 ? Integer.parseInt(bits[3].trim()) : COLORS[0];
				markers.add(new int[] { x, y, plane, color });
			} catch (Exception e) {
			}
		}
	}

	static void save(Properties props) {
		props.setProperty("groundMarkers", Boolean.toString(enabled));
		props.setProperty("groundMarkersMinimap", Boolean.toString(minimap));
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < markers.size(); i++) {
			int[] m = (int[]) markers.get(i);
			if (sb.length() > 0) {
				sb.append(';');
			}
			sb.append(m[0]).append(',').append(m[1]).append(',').append(m[2]).append(',').append(m[3]);
		}
		props.setProperty("groundMarkerList", sb.toString());
	}

	static int colorAt(int worldX, int worldY, int plane) {
		for (int i = 0; i < markers.size(); i++) {
			int[] m = (int[]) markers.get(i);
			if (m[0] == worldX && m[1] == worldY && m[2] == plane) {
				return m[3];
			}
		}
		return 0;
	}

	static int colorLocal(int localX, int localY, int plane) {
		return colorAt(client.getBaseX() + localX, client.getBaseY() + localY, plane);
	}

	static boolean hoveredIsMarked() {
		if (WorldController.hoverTileX < 0) {
			return false;
		}
		return colorLocal(WorldController.hoverTileX, WorldController.hoverTileY, client.scenePlane) != 0;
	}

	static void toggleHovered(client c, boolean mark) {
		if (WorldController.hoverTileX < 0) {
			c.pushMessage("Hover a tile first, then mark it.", 0, "");
			return;
		}
		setAt(c, client.getBaseX() + WorldController.hoverTileX, client.getBaseY() + WorldController.hoverTileY,
				client.scenePlane, mark);
	}

	static void setAt(client c, int wx, int wy, int plane, boolean mark) {
		if (wx <= 0 && wy <= 0) {
			toggleHovered(c, mark);
			return;
		}
		int existing = indexOf(wx, wy, plane);
		if (!mark) {
			if (existing >= 0) {
				markers.remove(existing);
				c.pushMessage("Tile unmarked.", 0, "");
				c.saveClientSettings();
			}
			return;
		}
		if (existing >= 0) {
			int[] m = (int[]) markers.get(existing);
			int next = 0;
			for (int i = 0; i < COLORS.length; i++) {
				if (COLORS[i] == m[3]) {
					next = (i + 1) % COLORS.length;
					break;
				}
			}
			m[3] = COLORS[next];
			c.pushMessage("Tile marker colour changed.", 0, "");
		} else {
			markers.add(new int[] { wx, wy, plane, COLORS[0] });
			c.pushMessage("Tile marked.", 0, "");
		}
		c.saveClientSettings();
	}

	static int size() {
		return markers.size();
	}

	static int[] get(int i) {
		return (int[]) markers.get(i);
	}

	private static int indexOf(int x, int y, int plane) {
		for (int i = 0; i < markers.size(); i++) {
			int[] m = (int[]) markers.get(i);
			if (m[0] == x && m[1] == y && m[2] == plane) {
				return i;
			}
		}
		return -1;
	}
}
