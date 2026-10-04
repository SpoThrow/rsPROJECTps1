package ui;

import java.util.Properties;

/** Decimal combat level on the Combat Options "Combat Lvl" line. Always on. */
public final class CombatLevelPlugin {

	public static void load(Properties props) {
	}

	public static void save(Properties props) {
		props.setProperty("combatLevelDecimal", "true");
	}

	static double value(int[] maxStats) {
		if (maxStats == null || maxStats.length < 7) {
			return 3.0D;
		}
		int att = maxStats[0];
		int def = maxStats[1];
		int str = maxStats[2];
		int hp = maxStats[3];
		int range = maxStats[4];
		int pray = maxStats[5];
		int mage = maxStats[6];
		double base = 0.25D * (def + hp + Math.floor(pray / 2.0D));
		double melee = 0.325D * (att + str);
		double ranged = 0.325D * Math.floor(range * 1.5D);
		double magic = 0.325D * Math.floor(mage * 1.5D);
		double style = melee;
		if (ranged > style) {
			style = ranged;
		}
		if (magic > style) {
			style = magic;
		}
		return base + style;
	}

	static String label(int[] maxStats) {
		double v = value(maxStats);
		int tenths = (int) Math.round(v * 10.0D);
		int whole = tenths / 10;
		int frac = tenths % 10;
		if (frac < 0) {
			frac = 0;
		}
		return whole + "." + frac;
	}

	public static void apply(int[] maxStats) {
		if (RSInterface.interfaceCache == null || 3983 >= RSInterface.interfaceCache.length) {
			return;
		}
		RSInterface rsi = RSInterface.interfaceCache[3983];
		if (rsi == null) {
			return;
		}
		rsi.message = "Combat Lvl: " + label(maxStats);
		if (rsi.width < 130) {
			rsi.width = 140;
		}
	}
}
