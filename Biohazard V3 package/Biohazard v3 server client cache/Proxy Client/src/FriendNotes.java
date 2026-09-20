import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Right-click notes on friends list entries. Does not use Shift/Ctrl/Alt.
 */
final class FriendNotes {

	static final int ACTION_NOTE = 1614;
	static boolean enabled = true;

	private static final HashMap notes = new HashMap();
	static String pendingName = "";

	static void load(Properties props) {
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

	static void save(Properties props) {
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

	static String get(String name) {
		if (name == null) {
			return "";
		}
		String n = (String) notes.get(name.toLowerCase());
		return n == null ? "" : n;
	}

	static void set(String name, String note) {
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

	static String displayName(String name) {
		if (!enabled || name == null) {
			return name;
		}
		String note = get(name);
		if (note.length() == 0) {
			return name;
		}
		if (note.length() > 18) {
			note = note.substring(0, 18) + "...";
		}
		return name + " - " + note;
	}

	static String menuLabel(String name) {
		return get(name).length() == 0 ? "Add note" : "Edit note";
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
