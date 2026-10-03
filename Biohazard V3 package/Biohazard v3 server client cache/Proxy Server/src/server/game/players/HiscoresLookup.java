package server.game.players;

import core.util.Misc;

/**
 * Looks up a character's skills from the live player or their save file.
 */
public class HiscoresLookup {

	public static void lookup(Client c, String query) {
		if (c == null) {
			return;
		}
		if (query == null) {
			query = "";
		}
		query = query.trim();
		if (query.length() == 0) {
			c.sendMessage("@hsle@Type a player name.");
			return;
		}
		if (query.length() > 12) {
			query = query.substring(0, 12);
		}
		Client online = PlayerHandler.getPlayer(query);
		int[] levels = new int[23];
		int[] xp = new int[23];
		String display;
		boolean isOnline;
		if (online != null) {
			display = online.playerName;
			isOnline = true;
			for (int i = 0; i < 23; i++) {
				xp[i] = online.skills.playerXP[i];
				levels[i] = online.getLevelForXP(xp[i]);
			}
		} else if (PlayerSave.readSkills(query, levels, xp)) {
			display = PlayerSave.matchedCharacterName(query);
			if (display == null || display.length() == 0) {
				display = Misc.formatPlayerName(query);
			}
			isOnline = false;
		} else {
			c.sendMessage("@hsle@Player not found.");
			return;
		}
		int totalLvl = 0;
		long totalXp = 0L;
		for (int i = 0; i < 23; i++) {
			if (levels[i] < 1) {
				levels[i] = 1;
			}
			totalLvl += levels[i];
			totalXp += xp[i];
		}
		int combat = combatLevel(levels);
		StringBuffer levelsBuf = new StringBuffer();
		StringBuffer xpBuf = new StringBuffer();
		for (int i = 0; i < 23; i++) {
			if (i > 0) {
				levelsBuf.append('.');
				xpBuf.append('.');
			}
			levelsBuf.append(levels[i]);
			xpBuf.append(xp[i]);
		}
		c.sendMessage("@hsl@" + display + "|" + (isOnline ? "1" : "0") + "|" + combat + "|" + totalLvl + "|" + totalXp
				+ "|" + levelsBuf.toString());
		c.sendMessage("@hsx@" + xpBuf.toString());
		int[] ranks = computeRanks(display, xp, totalXp);
		StringBuffer rankBuf = new StringBuffer();
		for (int i = 0; i < ranks.length; i++) {
			if (i > 0) {
				rankBuf.append('.');
			}
			rankBuf.append(ranks[i]);
		}
		c.sendMessage("@hsr@" + rankBuf.toString());
	}

	private static int[] computeRanks(String selfName, int[] xp, long totalXp) {
		int[] ranks = new int[24];
		for (int i = 0; i < ranks.length; i++) {
			ranks[i] = 1;
		}
		java.util.HashMap live = new java.util.HashMap();
		for (int i = 0; i < PlayerHandler.players.length; i++) {
			Client other = PlayerHandler.players[i];
			if (other == null || other.playerName == null) {
				continue;
			}
			live.put(other.playerName.toLowerCase(), other);
		}
		java.io.File dir = new java.io.File("./Data/characters");
		java.io.File[] files = dir.listFiles();
		if (files != null) {
			int[] otherLvl = new int[23];
			int[] otherXp = new int[23];
			for (int f = 0; f < files.length; f++) {
				String fn = files[f].getName();
				if (!fn.toLowerCase().endsWith(".txt")) {
					continue;
				}
				String otherName = fn.substring(0, fn.length() - 4);
				if (otherName.equalsIgnoreCase(selfName) || live.containsKey(otherName.toLowerCase())) {
					continue;
				}
				for (int i = 0; i < 23; i++) {
					otherLvl[i] = 1;
					otherXp[i] = 0;
				}
				if (!PlayerSave.readSkillsFromFile(files[f], otherLvl, otherXp)) {
					continue;
				}
				long otherTotal = 0L;
				for (int i = 0; i < 23; i++) {
					otherTotal += otherXp[i];
					if (otherXp[i] > xp[i]) {
						ranks[i]++;
					}
				}
				if (otherTotal > totalXp) {
					ranks[23]++;
				}
			}
		}
		Object[] online = live.values().toArray();
		for (int i = 0; i < online.length; i++) {
			Client other = (Client) online[i];
			if (other.playerName.equalsIgnoreCase(selfName)) {
				continue;
			}
			long otherTotal = 0L;
			for (int s = 0; s < 23; s++) {
				int oxp = other.skills.playerXP[s];
				otherTotal += oxp;
				if (oxp > xp[s]) {
					ranks[s]++;
				}
			}
			if (otherTotal > totalXp) {
				ranks[23]++;
			}
		}
		return ranks;
	}

	private static int combatLevel(int[] lvl) {
		int atk = lvl[0];
		int def = lvl[1];
		int str = lvl[2];
		int hp = lvl[3];
		int range = lvl[4];
		int pray = lvl[5];
		int mage = lvl[6];
		int combat = (int) (((def + hp) + Math.floor(pray / 2)) * 0.25D) + 1;
		double melee = (atk + str) * 0.325D;
		double ranged = Math.floor(range * 1.5D) * 0.325D;
		double magic = Math.floor(mage * 1.5D) * 0.325D;
		if (melee >= ranged && melee >= magic) {
			combat += melee;
		} else if (ranged >= melee && ranged >= magic) {
			combat += ranged;
		} else {
			combat += magic;
		}
		return combat;
	}
}
