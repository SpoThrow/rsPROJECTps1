package ui;

import java.util.Properties;

public final class AttackStyleWarn {

	static boolean warnDefence;
	static boolean warnAttack;
	static boolean warnStrength;
	static boolean warnRanged;
	static boolean warnMagic;

	public static void load(Properties props) {
		warnDefence = readBool(props, "atkWarnDef", false);
		warnAttack = readBool(props, "atkWarnAtk", false);
		warnStrength = readBool(props, "atkWarnStr", false);
		warnRanged = readBool(props, "atkWarnRange", false);
		warnMagic = readBool(props, "atkWarnMage", false);
	}

	public static void save(Properties props) {
		props.setProperty("atkWarnDef", Boolean.toString(warnDefence));
		props.setProperty("atkWarnAtk", Boolean.toString(warnAttack));
		props.setProperty("atkWarnStr", Boolean.toString(warnStrength));
		props.setProperty("atkWarnRange", Boolean.toString(warnRanged));
		props.setProperty("atkWarnMage", Boolean.toString(warnMagic));
	}

	public static void cycle() {
		if (!warnDefence && !warnAttack && !warnStrength && !warnRanged && !warnMagic) {
			warnDefence = true;
		} else if (warnDefence) {
			warnDefence = false;
			warnAttack = true;
		} else if (warnAttack) {
			warnAttack = false;
			warnStrength = true;
		} else if (warnStrength) {
			warnStrength = false;
			warnRanged = true;
		} else if (warnRanged) {
			warnRanged = false;
			warnMagic = true;
		} else {
			warnMagic = false;
		}
	}

	public static String label() {
		if (warnDefence) {
			return "Defence";
		}
		if (warnAttack) {
			return "Attack";
		}
		if (warnStrength) {
			return "Strength";
		}
		if (warnRanged) {
			return "Ranged";
		}
		if (warnMagic) {
			return "Magic";
		}
		return "Off";
	}

	public static boolean warned(String style) {
		if (style == null) {
			return false;
		}
		String s = style.toLowerCase();
		if (warnDefence && (s.indexOf("defen") >= 0 || s.indexOf("longrange") >= 0)) {
			return true;
		}
		if (warnAttack && s.indexOf("accurate") >= 0) {
			return true;
		}
		if (warnStrength && (s.indexOf("aggress") >= 0 || s.indexOf("rapid") >= 0)) {
			return true;
		}
		if (warnRanged && (s.indexOf("rapid") >= 0 || s.indexOf("accurate") >= 0 && s.indexOf("range") >= 0)) {
			return true;
		}
		if (warnMagic && s.indexOf("magic") >= 0) {
			return true;
		}
		return false;
	}

	private static boolean readBool(Properties props, String key, boolean def) {
		String v = props.getProperty(key);
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equals("1");
	}
}
