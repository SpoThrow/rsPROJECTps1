package server.game.npcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * The two wilderness bandit tables: one row per level, both always dropping bones, and every item id
 * in them resolving against the shipped {@code item.cfg}.
 *
 * <p>The server only ever rolls a table it can parse, so a typo in a colon field - or in the tab
 * between two entries - becomes a silent missing drop rather than an error on start-up. The item
 * side has the same failure mode for the opposite reason: an id that is not in the cache still
 * rolls, and the kill then pays nothing. These tests are the cheapest place to catch both.
 */
class BanditDropTableTest {

	private static final int LEVEL_22 = 195;
	private static final int LEVEL_130 = 6605;

	private static File cfgDir() {
		return new File(System.getProperty("serverCfgDir", "Data/cfg"));
	}

	@Test
	void bothBanditLevelsShipATableThatAlwaysDropsBones() throws IOException {
		File cfg = new File(cfgDir(), "npc_drops.cfg");
		Assumptions.assumeTrue(cfg.isFile(), "no shipped drop table at " + cfg);
		List<String> lines = Files.readAllLines(cfg.toPath(), StandardCharsets.UTF_8);

		for (int npcId : new int[] { LEVEL_22, LEVEL_130 }) {
			String row = rowFor(lines, npcId);
			assertTrue(row != null, "no drop row for bandit " + npcId);
			assertTrue(row.contains("526:1:ALWAYS"), "bandit " + npcId + " must always drop bones");
		}
	}

	@Test
	void everyBanditDropIsARealItem() throws IOException {
		File cfg = new File(cfgDir(), "npc_drops.cfg");
		File items = new File(cfgDir(), "item.cfg");
		Assumptions.assumeTrue(cfg.isFile() && items.isFile(), "shipped cfg files are missing");

		Set<Integer> known = itemIds(Files.readAllLines(items.toPath(), StandardCharsets.UTF_8));
		assertFalse(known.isEmpty(), "item.cfg parsed to nothing, so this test would pass vacuously");

		List<String> lines = Files.readAllLines(cfg.toPath(), StandardCharsets.UTF_8);
		for (int npcId : new int[] { LEVEL_22, LEVEL_130 }) {
			String row = rowFor(lines, npcId);
			assertTrue(row != null, "no drop row for bandit " + npcId);
			for (int itemId : droppedItemIds(row)) {
				assertTrue(known.contains(itemId),
						"bandit " + npcId + " drops item " + itemId + ", which item.cfg does not define");
			}
		}
	}

	@Test
	void theTwoBanditSpawnSetsAreBalanced() throws IOException {
		File cfg = new File(cfgDir(), "spawn-config.cfg");
		Assumptions.assumeTrue(cfg.isFile(), "no shipped spawn config at " + cfg);
		List<String> lines = Files.readAllLines(cfg.toPath(), StandardCharsets.UTF_8);

		int low = 0, high = 0;
		for (String raw : lines) {
			String line = raw.trim();
			if (!line.startsWith("spawn = ")) {
				continue;
			}
			String[] fields = line.substring("spawn = ".length()).split("\t");
			if (fields.length < 2) {
				continue;
			}
			int npcId = Integer.parseInt(fields[0].trim());
			if (npcId == LEVEL_22) {
				low++;
			} else if (npcId == LEVEL_130) {
				high++;
			}
		}
		assertEquals(8, low, "eight level 22 bandits were added");
		assertEquals(8, high, "eight level 130 bandits were added");
	}

	private static String rowFor(List<String> lines, int npcId) {
		String prefix = "drop = ";
		for (String raw : lines) {
			String line = raw.trim();
			if (!line.startsWith(prefix)) {
				continue;
			}
			String idList = line.substring(prefix.length()).split("\\s+")[0];
			for (String id : idList.split("/")) {
				if (id.trim().equals(String.valueOf(npcId))) {
					return line;
				}
			}
		}
		return null;
	}

	private static Set<Integer> droppedItemIds(String row) {
		Set<Integer> ids = new LinkedHashSet<Integer>();
		String[] fields = row.split("\t");
		for (int i = 1; i < fields.length; i++) {
			String field = fields[i].trim();
			if (field.isEmpty()) {
				continue;
			}
			ids.add(Integer.parseInt(field.split(":")[0]));
		}
		return ids;
	}

	private static Set<Integer> itemIds(List<String> itemCfg) {
		Set<Integer> ids = new HashSet<Integer>();
		for (String raw : itemCfg) {
			String line = raw.trim();
			if (!line.startsWith("item = ")) {
				continue;
			}
			String rest = line.substring("item = ".length()).trim();
			int end = 0;
			while (end < rest.length() && Character.isDigit(rest.charAt(end))) {
				end++;
			}
			if (end > 0) {
				ids.add(Integer.parseInt(rest.substring(0, end)));
			}
		}
		return ids;
	}
}
