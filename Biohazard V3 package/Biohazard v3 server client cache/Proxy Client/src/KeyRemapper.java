import java.awt.event.KeyEvent;
import java.util.Properties;

final class KeyRemapper {

	static final int INTERFACE_ID = 24300;
	static final int CLOSE_ID = 24304;
	static final int ENABLE_ID = 24310;
	static final int ENTER_CHAT_ID = 24311;
	static final int SPACE_ID = 24312;
	static final int WASD_ID = 24313;
	static final int RESET_ID = 24314;
	static final int NUMBERS_ID = 24315;
	static final int BIND_START_ID = 24320;

	static final int[] TABS = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 12, 13 };
	static final String[] NAMES = {
		"Combat", "Skills", "Quest", "Inventory", "Equipment", "Prayer", "Magic",
		"Friends", "Ignore", "Logout", "Settings", "Emotes", "Music"
	};

	private static final int[] DEFAULT_KEYS = {
		KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_ESCAPE,
		KeyEvent.VK_F4, KeyEvent.VK_F5, KeyEvent.VK_F6, KeyEvent.VK_F7,
		KeyEvent.VK_F8, KeyEvent.VK_F9, KeyEvent.VK_F10, KeyEvent.VK_F11,
		KeyEvent.VK_F12
	};

	private static final int[] binds = new int[TABS.length];
	private static int capturing = -1;

	static {
		resetDefaults();
	}

	static void resetDefaults() {
		for (int i = 0; i < binds.length; i++) {
			binds[i] = DEFAULT_KEYS[i];
		}
		capturing = -1;
	}

	static int capturingIndex() {
		return capturing;
	}

	static boolean isCapturing() {
		return capturing >= 0;
	}

	static void startCapture(int bindIndex) {
		if (bindIndex >= 0 && bindIndex < TABS.length) {
			capturing = bindIndex;
			refreshInterface();
		}
	}

	static void cancelCapture() {
		capturing = -1;
		refreshInterface();
	}

	static boolean captureKey(int keyCode) {
		if (capturing < 0) {
			return false;
		}
		if (keyCode == KeyEvent.VK_ESCAPE && capturing != indexOfTab(3)) {
			cancelCapture();
			return true;
		}
		if (keyCode == KeyEvent.VK_SHIFT || keyCode == KeyEvent.VK_CONTROL
				|| keyCode == KeyEvent.VK_ALT || keyCode == KeyEvent.VK_META
				|| keyCode == KeyEvent.VK_ENTER) {
			return true;
		}
		if (keyCode == KeyEvent.VK_BACK_SPACE || keyCode == KeyEvent.VK_DELETE) {
			binds[capturing] = 0;
		} else {
			for (int i = 0; i < binds.length; i++) {
				if (i != capturing && binds[i] == keyCode) {
					binds[i] = 0;
				}
			}
			binds[capturing] = keyCode;
		}
		capturing = -1;
		refreshInterface();
		if (client.instance != null) {
			client.instance.saveClientSettings();
		}
		return true;
	}

	static int tabForKey(int keyCode) {
		if (keyCode == 0) {
			return -1;
		}
		for (int i = 0; i < binds.length; i++) {
			if (binds[i] == keyCode) {
				return TABS[i];
			}
		}
		return -1;
	}

	static boolean handleClick(int id) {
		if (id == CLOSE_ID) {
			capturing = -1;
			client.instance.openInterfaceID = 24200;
			client.instance.refreshClientSettingsInterface();
			return true;
		}
		if (id == ENABLE_ID) {
			client.keyRemapping = !client.keyRemapping;
			if (!client.keyRemapping) {
				client.chatTypeFocused = true;
				capturing = -1;
			} else if (client.enterToChat) {
				client.chatTypeFocused = false;
			} else {
				client.chatTypeFocused = false;
			}
			refreshInterface();
			client.instance.refreshClientSettingsInterface();
			client.instance.saveClientSettings();
			return true;
		}
		if (id == ENTER_CHAT_ID) {
			client.enterToChat = !client.enterToChat;
			if (client.keyRemapping) {
				client.chatTypeFocused = false;
			}
			refreshInterface();
			client.instance.saveClientSettings();
			return true;
		}
		if (id == SPACE_ID) {
			client.spaceContinue = !client.spaceContinue;
			refreshInterface();
			client.instance.saveClientSettings();
			return true;
		}
		if (id == WASD_ID) {
			client.wasdCamera = !client.wasdCamera;
			refreshInterface();
			client.instance.saveClientSettings();
			return true;
		}
		if (id == RESET_ID) {
			resetDefaults();
			refreshInterface();
			client.instance.saveClientSettings();
			return true;
		}
		if (id == NUMBERS_ID) {
			applyNumberKeys();
			refreshInterface();
			client.instance.saveClientSettings();
			return true;
		}
		if (id >= BIND_START_ID && id < BIND_START_ID + TABS.length) {
			startCapture(id - BIND_START_ID);
			return true;
		}
		return false;
	}

	static void refreshInterface() {
		setLine(ENABLE_ID, "Key remapping: " + onOff(client.keyRemapping));
		setLine(ENTER_CHAT_ID, "Enter to chat: " + onOff(client.enterToChat));
		setLine(SPACE_ID, "Space to continue: " + onOff(client.spaceContinue));
		setLine(WASD_ID, "WASD camera: " + onOff(client.wasdCamera));
		setLine(RESET_ID, "Reset sidebar keys to defaults");
		setLine(NUMBERS_ID, "Preset: number keys 1-0");
		for (int i = 0; i < TABS.length; i++) {
			String value = capturing == i ? "press a key..." : keyName(binds[i]);
			setLine(BIND_START_ID + i, NAMES[i] + ": " + value);
		}
	}

	static void applyNumberKeys() {
		int[] keys = {
			KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5,
			KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9, KeyEvent.VK_0,
			KeyEvent.VK_MINUS, KeyEvent.VK_EQUALS, KeyEvent.VK_BACK_SLASH
		};
		for (int i = 0; i < binds.length && i < keys.length; i++) {
			binds[i] = keys[i];
		}
		capturing = -1;
	}

	static void load(Properties props) {
		resetDefaults();
		String raw = props.getProperty("keyBinds", "");
		if (raw.length() == 0) {
			return;
		}
		String[] parts = raw.split(",");
		for (int i = 0; i < parts.length && i < binds.length; i++) {
			try {
				binds[i] = Integer.parseInt(parts[i].trim());
			} catch (Exception ignored) {
			}
		}
	}

	static void save(Properties props) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < binds.length; i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(binds[i]);
		}
		props.setProperty("keyBinds", sb.toString());
	}

	private static int indexOfTab(int tab) {
		for (int i = 0; i < TABS.length; i++) {
			if (TABS[i] == tab) {
				return i;
			}
		}
		return -1;
	}

	private static String onOff(boolean value) {
		return value ? "On" : "Off";
	}

	private static String keyName(int keyCode) {
		if (keyCode <= 0) {
			return "None";
		}
		String name = KeyEvent.getKeyText(keyCode);
		if (name == null || name.length() == 0) {
			return "Key " + keyCode;
		}
		return name;
	}

	private static void setLine(int id, String text) {
		if (RSInterface.interfaceCache == null || id < 0
				|| id >= RSInterface.interfaceCache.length
				|| RSInterface.interfaceCache[id] == null) {
			return;
		}
		RSInterface.interfaceCache[id].message = text;
	}
}
