package ui;

import java.util.Properties;

/** Keep game chat across logout / login in this client session. */
public final class ChatHistory {

	public static boolean enabled = true;

	public static void load(Properties props) {
		enabled = readBool(props, "chatHistory", true);
	}

	public static void save(Properties props) {
		props.setProperty("chatHistory", Boolean.toString(enabled));
	}

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}
}
