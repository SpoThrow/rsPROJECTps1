package ui;

import java.util.Properties;

public final class AntiDrag {

	public static boolean enabled;
	public static boolean shiftOnly = true;
	public static int delay = 30;

	public static void load(Properties props) {
		enabled = readBool(props, "antiDrag", false);
		shiftOnly = readBool(props, "antiDragShift", true);
		delay = clamp(readInt(props, "antiDragDelay", 30), 1, 80);
	}

	public static void save(Properties props) {
		props.setProperty("antiDrag", Boolean.toString(enabled));
		props.setProperty("antiDragShift", Boolean.toString(shiftOnly));
		props.setProperty("antiDragDelay", Integer.toString(delay));
	}

	public static int threshold() {
		if (!enabled) {
			return 5;
		}
		if (shiftOnly && !RSApplet.shiftIsDown) {
			return 5;
		}
		return delay;
	}

	public static void cycleDelay() {
		if (delay <= 10) {
			delay = 20;
		} else if (delay <= 20) {
			delay = 30;
		} else if (delay <= 30) {
			delay = 50;
		} else {
			delay = 10;
		}
	}

	public static void setDelay(int value) {
		delay = clamp(value, 1, 80);
	}

	private static int readInt(Properties props, String key, int def) {
		try {
			return Integer.parseInt(props.getProperty(key, Integer.toString(def)));
		} catch (Exception e) {
			return def;
		}
	}

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}

	private static int clamp(int v, int min, int max) {
		if (v < min) {
			return min;
		}
		if (v > max) {
			return max;
		}
		return v;
	}
}
