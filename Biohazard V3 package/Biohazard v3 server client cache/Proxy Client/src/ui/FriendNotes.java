package ui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;import game.client;



/**
 * Right-click notes on friends list entries. Does not use Shift/Ctrl/Alt.
 */
public final class FriendNotes {

	public static final int ACTION_NOTE = 1614;
	public static boolean enabled = true;

	private static final HashMap notes = new HashMap();
	public static String pendingName = "";

	public static void load(Properties props) {
		enabled = readBool(props, "friendNotes", true);
		notes.clear();
		String packed = props.getProperty("friendNoteList", "");
		if (packed == null || packed.length() == 0) {
			return;
		}
		String[] parts = packed.split(";");
		for (int i = 0; i < parts.length; i++) {
			int eq = parts[i].indexOf('=');
			if (eq <= 0) {
				continue;
			}
			String name = unescape(parts[i].substring(0, eq));
			String note = unescape(parts[i].substring(eq + 1));
			if (name.length() > 0) {
				notes.put(name.toLowerCase(), note);
			}
		}
	}

	public static void save(Properties props) {
		props.setProperty("friendNotes", Boolean.toString(enabled));
		StringBuffer sb = new StringBuffer();
		Iterator it = notes.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry e = (Map.Entry) it.next();
			if (sb.length() > 0) {
				sb.append(';');
			}
			sb.append(escape((String) e.getKey())).append('=').append(escape((String) e.getValue()));
		}
		props.setProperty("friendNoteList", sb.toString());
	}

	public static String get(String name) {
		if (name == null) {
			return "";
		}
		String n = (String) notes.get(name.toLowerCase());
		return n == null ? "" : n;
	}

	public static void set(String name, String note) {
		if (name == null || name.length() == 0) {
			return;
		}
		if (note == null) {
			note = "";
		}
		if (note.length() > 128) {
			note = note.substring(0, 128);
		}
		String key = name.toLowerCase();
		if (note.length() == 0) {
			notes.remove(key);
		} else {
			notes.put(key, note);
		}
	}

	public static String displayName(String name) {
		return name;
	}

	static String noteFromMenu(String raw) {
		if (!enabled || raw == null) {
			return "";
		}
		String name = extractName(raw);
		if (name.length() == 0) {
			return "";
		}
		return get(name);
	}

	private static String extractName(String raw) {
		int at = raw.lastIndexOf("@whi@");
		if (at >= 0) {
			raw = raw.substring(at + 5);
		} else {
			at = raw.lastIndexOf("@lre@");
			if (at >= 0) {
				raw = raw.substring(at + 5);
			}
		}
		raw = raw.trim();
		int cut = raw.indexOf('@');
		if (cut > 0) {
			raw = raw.substring(0, cut).trim();
		}
		return raw;
	}

	public static String menuLabel(String name) {
		return get(name).length() == 0 ? "Add note" : "Edit note";
	}

	public static void drawHover(client c, String raw, int mouseX, int mouseY) {
		if (!enabled || c == null || c.smallText == null) {
			return;
		}
		String note = noteFromMenu(raw);
		if (note == null || note.length() == 0) {
			return;
		}
		int w = c.smallText.getTextWidth(note) + 8;
		int h = 16;
		int x = mouseX + 12;
		int y = mouseY - h - 6;
		if (x + w > DrawingArea.bottomX) {
			x = mouseX - w - 4;
		}
		if (y < DrawingArea.topY) {
			y = mouseY + 16;
		}
		if (x < DrawingArea.topX) {
			x = DrawingArea.topX;
		}
		DrawingArea.method335(0x000000, y, w, h, 210, x);
		DrawingArea.fillPixels(x, w, h, 0xC6B895, y);
		c.smallText.method385(0xFFE14A, note, y + 12, x + 4);
	}

	private static String escape(String s) {
		if (s == null) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (ch == '\\' || ch == ';' || ch == '=') {
				sb.append('\\');
			}
			sb.append(ch);
		}
		return sb.toString();
	}

	private static String unescape(String s) {
		StringBuffer sb = new StringBuffer();
		boolean esc = false;
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (esc) {
				sb.append(ch);
				esc = false;
			} else if (ch == '\\') {
				esc = true;
			} else {
				sb.append(ch);
			}
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
