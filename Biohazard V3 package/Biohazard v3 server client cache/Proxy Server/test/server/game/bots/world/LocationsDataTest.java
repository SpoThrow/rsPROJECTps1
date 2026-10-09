package server.game.bots.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The joins: teleports from {@code teleports.cfg}, shops from {@code npc-shops.cfg} + {@code shops.cfg}
 * + {@code spawn-config.cfg}, monsters from {@code spawn-config.cfg} + {@code npc.cfg}.
 *
 * <p>Deriving rather than copying is the whole point — a second copy of a shop's location would drift
 * the first time a shop moved — so these tests are about the join being real, and about the two cases
 * where the join cannot be made (a shop whose keeper has no spawn, a spawn with no {@code npc.cfg}
 * row). Guessing in those cases would put a bot on a made-up tile, which is worse than no answer.
 */
class LocationsDataTest {

	private static void write(Path dir, String name, String... lines) throws IOException {
		Files.write(dir.resolve(name), Arrays.asList(lines), StandardCharsets.UTF_8);
	}

	private static Location find(List<Location> locations, String name) {
		for (Location location : locations) {
			if (location.name().equals(name)) {
				return location;
			}
		}
		return null;
	}

	@Test
	void teleportsAreJoinedWithTheirMultiWordNamesIntact(@TempDir Path dir) throws IOException {
		write(dir, "teleports.cfg",
				"// Player Teleport Locations",
				"// Format: teleport = Category Name X Y",
				"",
				"teleport = Modern Varrock 3210 3424",
				"teleport = Modern White Wolf Mountain 2848 3498");

		LocationsData.Result result = LocationsData.load(dir);

		assertTrue(result.problems().isEmpty(), "no problems: " + result.problems());
		Location varrock = find(result.locations(), "modern_varrock");
		assertTrue(varrock != null, "the two-word name parsed: " + result.locations());
		assertEquals(LocationKind.TELEPORT, varrock.kind());
		assertEquals(3210, varrock.x());
		assertEquals(3424, varrock.y());
		assertTrue(varrock.hasTag("modern"));

		Location wolf = find(result.locations(), "modern_white_wolf_mountain");
		assertTrue(wolf != null, "the four-word name parsed: " + result.locations());
		assertEquals(2848, wolf.x(), "the coordinates were peeled off the end, not counted from the front");
		assertEquals(3498, wolf.y());
	}

	@Test
	void aShopIsPlacedAtItsKeepersSpawnTile(@TempDir Path dir) throws IOException {
		write(dir, "shops.cfg",
				"//-----ShopID---ShopName---",
				"shop = 8\tHorvik's_Armour_Shop\t2\t1\t1155\t100");
		write(dir, "npc-shops.cfg",
				"// NPC to Shop Assignment Configuration",
				"npc-shop = 549 8");
		write(dir, "spawn-config.cfg",
				"spawn = 549\t3080\t3509\t0\t1\t0\t0\t0\tHorvik");

		LocationsData.Result result = LocationsData.load(dir);

		assertTrue(result.problems().isEmpty(), "no problems: " + result.problems());
		Location shop = find(result.locations(), "horvik_s_armour_shop");
		assertTrue(shop != null, "the shop was joined: " + result.locations());
		assertEquals(LocationKind.SHOP, shop.kind());
		assertEquals(3080, shop.x(), "placed at the keeper's tile, not a copied coordinate");
		assertEquals(3509, shop.y());
		assertTrue(shop.hasTag("shop:8"));
		assertTrue(shop.hasTag("npc:549"));
	}

	@Test
	void aShopWhoseKeeperHasNoSpawnIsSkippedRatherThanGuessed(@TempDir Path dir) throws IOException {
		write(dir, "shops.cfg", "shop = 8\tHorvik's_Armour_Shop\t2\t1\t1155\t100");
		write(dir, "npc-shops.cfg", "npc-shop = 549 8");
		// No spawn-config.cfg at all: there is nowhere to walk to.

		LocationsData.Result result = LocationsData.load(dir);

		assertTrue(result.locations().isEmpty(), "nothing is invented: " + result.locations());
		assertTrue(result.problems().isEmpty(), "and it is not an error, it is an absence");
	}

	@Test
	void aMonsterIsNamedFromNpcCfgAndAnUnknownSpawnIsSkipped(@TempDir Path dir) throws IOException {
		write(dir, "spawn-config.cfg",
				"spawn = 117\t3117\t9846\t0\t1\t0\t0\t0\thillgiant",
				"spawn = 4242\t3200\t3200\t0\t1\t0\t0\t0\tmystery");
		write(dir, "npc.cfg",
				"//    NpcID     NpcName                         combat  health",
				"npc = 117 Hill_Giant\t28\t35");

		LocationsData.Result result = LocationsData.load(dir);

		assertTrue(result.problems().isEmpty(), "no problems: " + result.problems());
		assertEquals(1, result.locations().size(), "the unknown id is skipped: " + result.locations());
		Location giant = result.locations().get(0);
		assertEquals("hill_giant@3117,9846", giant.name(), "the tile keeps a repeated name unique");
		assertEquals(LocationKind.MONSTER, giant.kind());
		assertEquals(3117, giant.x());
		assertEquals(9846, giant.y(), "the dungeon y is carried through, not clamped to the surface");
		assertTrue(giant.hasTag("npc:117"));
	}

	@Test
	void npcShopLinesAreNotReadAsNpcOrSpawnRows(@TempDir Path dir) throws IOException {
		// "npc-shop" starts with both "npc" and, near enough, "spawn". A prefix-matching reader would
		// take "549 8" as an npc row (id 549, name "8") and as a spawn row, inventing a monster at
		// tile 549,8 in the middle of nowhere. Only an exact key match keeps that from happening.
		write(dir, "npc-shops.cfg", "npc-shop = 549 8");

		LocationsData.Result result = LocationsData.load(dir);

		assertTrue(result.locations().isEmpty(),
				"nothing is invented from a shop row: " + result.locations());
		assertTrue(result.problems().isEmpty(), "and nothing is reported either: " + result.problems());
	}

	@Test
	void aMissingCfgDirectoryIsEmptyAndNotAProblem(@TempDir Path dir) {
		LocationsData.Result result = LocationsData.load(dir.resolve("nothing_here"));

		assertTrue(result.locations().isEmpty());
		assertTrue(result.problems().isEmpty(), "absent files are not errors: " + result.problems());
	}

	@Test
	void theAuthoredTableSitsAlongsideTheJoins(@TempDir Path dir) throws IOException {
		Files.createDirectories(dir.resolve("bots"));
		write(dir.resolve("bots"), "locations.cfg",
				"region = draynor_bank x 3091 y 3241 w 6 h 5 plane 0 kind bank");
		write(dir, "teleports.cfg", "teleport = Modern Varrock 3210 3424");

		LocationsData.Result result = LocationsData.load(dir);

		assertTrue(result.problems().isEmpty(), "no problems: " + result.problems());
		assertTrue(find(result.locations(), "draynor_bank") != null, "the authored row is read");
		assertTrue(find(result.locations(), "modern_varrock") != null, "and so is the join");
	}

	@Test
	void theShippedDataDirectoryJoinsWithoutASingleProblem() {
		String dataRoot = System.getProperty("workshopDataRoot");
		assertTrue(dataRoot != null && !dataRoot.isBlank(),
				"the test task must set workshopDataRoot to Data/; see build.gradle");

		LocationsData.Result result = LocationsData.load(Path.of(dataRoot, "cfg"));

		// The real files are the test. A parser that only handles hand-made input while the shipped
		// teleports.cfg has a shape it cannot read is a parser that does not work.
		assertTrue(result.problems().isEmpty(),
				"the shipped Data/cfg must join cleanly: " + result.problems());

		int teleports = 0, shops = 0, monsters = 0, banks = 0;
		for (Location location : result.locations()) {
			switch (location.kind()) {
			case TELEPORT:
				teleports++;
				break;
			case SHOP:
				shops++;
				break;
			case MONSTER:
				monsters++;
				break;
			case BANK:
				banks++;
				break;
			default:
				break;
			}
		}
		assertTrue(teleports >= 50, "teleports.cfg has dozens of entries, got " + teleports);
		assertTrue(shops >= 5, "the Edgeville mall alone is seven shops, got " + shops);
		// One row per distinct npc id, because spawnTiles keeps the first spawn of each — 2465 spawn
		// lines collapse to a few hundred monsters. That is the intended shape: nearest() is the
		// lookup that matters, and the scanned path covers the other spawns of the same monster.
		assertTrue(monsters >= 400, "spawn-config.cfg is 2465 lines, got " + monsters + " distinct npcs");
		assertTrue(banks >= 3, "the authored seed has three banks, got " + banks);

		// Spot-check one join end to end against the real files.
		Location varrock = find(result.locations(), "modern_varrock");
		assertTrue(varrock != null, "Modern Varrock is a real teleport");
		assertEquals(3210, varrock.x());
		assertEquals(3424, varrock.y());
	}

	@Test
	void aRepeatedMonsterNameIsDisambiguatedByItsTileSoBylineLookupsCannotCollide() {
		String dataRoot = System.getProperty("workshopDataRoot");
		LocationsData.Result result = LocationsData.load(Path.of(dataRoot, "cfg"));

		Location hill = null;
		for (Location location : result.locations()) {
			if (location.name().startsWith("hill_giant@")) {
				hill = location;
			}
		}
		assertTrue(hill != null, "hill giants are spawned on this server");
		assertNull(find(result.locations(), "hill_giant"),
				"the bare name is not a key, because dozens of spawns would share it");
		assertEquals(LocationKind.MONSTER, hill.kind());
	}
}
