package server.game.npcs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public final class NpcAnim667 {

	private static final Map<Integer, int[]> ANIMS = new HashMap<Integer, int[]>();
	private static final Map<Integer, Integer> STYLE = new HashMap<Integer, Integer>();
	private static boolean loaded;

	public static void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader("./Data/cfg/npc_anims.cfg"));
			String line;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (!line.startsWith("anim =")) {
					continue;
				}
				String[] t = line.substring(6).trim().split("\t");
				if (t.length < 6) {
					continue;
				}
				int id = Integer.parseInt(t[0].trim());
				int attack = Integer.parseInt(t[1].trim());
				int block = Integer.parseInt(t[2].trim());
				int death = Integer.parseInt(t[3].trim());
				int delay = Integer.parseInt(t[4].trim());
				int maxHit = Integer.parseInt(t[5].trim());
				ANIMS.put(Integer.valueOf(id), new int[] { attack, block, death, delay, maxHit });
				if (t.length > 6) {
					STYLE.put(Integer.valueOf(id), Integer.valueOf(style(t[6].trim())));
				}
			}
			System.out.println("[667] npc combat anims " + ANIMS.size());
		} catch (Exception e) {
			System.out.println("[667] npc_anims.cfg not loaded");
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception e) {
			}
		}
	}

	public static int attack(int npcType) {
		int[] row = row(npcType);
		return row == null ? -1 : row[0];
	}

	public static int block(int npcType) {
		int[] row = row(npcType);
		return row == null ? -1 : row[1];
	}

	public static int death(int npcType) {
		int[] row = row(npcType);
		return row == null ? -1 : row[2];
	}

	public static int delay(int npcType) {
		int[] row = row(npcType);
		return row == null ? -1 : row[3];
	}

	public static int maxHit(int npcType) {
		int[] row = row(npcType);
		if (row == null) {
			return -1;
		}
		int hit = row[4] / 10;
		return hit < 1 ? 1 : hit;
	}

	public static void applyStyle(NPC npc) {
		if (npc == null || npc.attackType != 0) {
			return;
		}
		Integer style = STYLE.get(Integer.valueOf(npc.npcType));
		if (style != null && style.intValue() > 0) {
			npc.attackType = style.intValue();
		}
	}

	private static int[] row(int npcType) {
		load();
		return ANIMS.get(Integer.valueOf(npcType));
	}

	private static int style(String raw) {
		String s = raw.toUpperCase();
		if (s.contains("RANGE")) {
			return 1;
		}
		if (s.contains("MAGIC") || s.equals("MAGE")) {
			return 2;
		}
		return 0;
	}

	private NpcAnim667() {
	}
}
