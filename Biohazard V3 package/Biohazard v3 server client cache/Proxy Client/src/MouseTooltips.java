import java.util.Properties;

final class MouseTooltips {

	static boolean enabled;
	static boolean interfaces = true;
	static boolean chatbox = true;

	static void load(Properties props) {
		enabled = readBool(props, "mouseTips", false);
		interfaces = readBool(props, "mouseTipsUi", true);
		chatbox = readBool(props, "mouseTipsChat", true);
	}

	static void save(Properties props) {
		props.setProperty("mouseTips", Boolean.toString(enabled));
		props.setProperty("mouseTipsUi", Boolean.toString(interfaces));
		props.setProperty("mouseTipsChat", Boolean.toString(chatbox));
	}

	static boolean skipTab(int tabId, int openInterfaceId) {
		if (tabId == 1 || tabId == 5 || tabId == 6) {
			return true;
		}
		if (openInterfaceId == 15106 || openInterfaceId == 17100 || openInterfaceId == 18100) {
			return true;
		}
		return false;
	}

	static boolean skip(String s) {
		if (s == null) {
			return true;
		}
		String t = strip(s).toLowerCase();
		if (t.startsWith("walk here") || t.equals("cancel") || t.startsWith("continue") || t.startsWith("move")) {
			return true;
		}
		if (t.indexOf("auto retaliat") >= 0 || t.indexOf("retaliate") >= 0) {
			return true;
		}
		return false;
	}

	static void drawAtMouse(client c, String raw, int mouseX, int mouseY) {
		if (!enabled || c == null || c.smallText == null || raw == null) {
			return;
		}
		if (skipTab(client.tabID, c.openInterfaceID)) {
			return;
		}
		if (skip(raw)) {
			return;
		}
		String text = strip(raw);
		int cut = text.indexOf(" / ");
		if (cut > 0) {
			text = text.substring(0, cut);
		}
		String note = FriendNotes.noteFromMenu(raw);
		int w = c.smallText.getTextWidth(text) + 8;
		int h = 16;
		if (note != null && note.length() > 0) {
			int nw = c.smallText.getTextWidth(note) + 8;
			if (nw > w) {
				w = nw;
			}
			h = 30;
		}
		int x = mouseX + 12;
		int y = mouseY - h - 6;
		if (x + w > DrawingArea.bottomX) {
			x = mouseX - w - 4;
		}
		if (y < DrawingArea.topY) {
			y = mouseY + 16;
		}
		if (y + h > DrawingArea.bottomY) {
			y = DrawingArea.bottomY - h;
		}
		if (x < DrawingArea.topX) {
			x = DrawingArea.topX;
		}
		DrawingArea.method335(0x000000, y, w, h, 210, x);
		DrawingArea.fillPixels(x, w, h, 0xC6B895, y);
		c.smallText.method385(0xffffff, text, y + 12, x + 4);
		if (note != null && note.length() > 0) {
			c.smallText.method385(0xFFE14A, note, y + 26, x + 4);
		}
	}

	private static String strip(String s) {
		StringBuffer sb = new StringBuffer(s.length());
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '@' && i + 4 < s.length()) {
				i += 4;
				continue;
			}
			sb.append(s.charAt(i));
		}
		return sb.toString();
	}

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}
}
