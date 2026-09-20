import java.util.Properties;

/** Shows Friends — current/max in the friends list title. */
final class FriendListPlugin {

	static boolean enabled = true;

	private static final int[] TITLE_IDS = { 5067, 5072, 571 };

	static void load(Properties props) {
		enabled = readBool(props, "friendListCounts", true);
	}

	static void save(Properties props) {
		props.setProperty("friendListCounts", Boolean.toString(enabled));
	}

	static void updateTitle(int friends, int cap) {
		if (RSInterface.interfaceCache == null) {
			return;
		}
		String text = enabled ? ("Friends — " + friends + "/" + cap) : "Friends List";
		for (int i = 0; i < TITLE_IDS.length; i++) {
			int id = TITLE_IDS[i];
			if (id >= 0 && id < RSInterface.interfaceCache.length && RSInterface.interfaceCache[id] != null) {
				String msg = RSInterface.interfaceCache[id].message;
				if (msg == null || msg.length() == 0 || msg.toLowerCase().indexOf("friend") >= 0
						|| msg.startsWith("Friends")) {
					RSInterface.interfaceCache[id].message = text;
				}
			}
		}
	}

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}
}
