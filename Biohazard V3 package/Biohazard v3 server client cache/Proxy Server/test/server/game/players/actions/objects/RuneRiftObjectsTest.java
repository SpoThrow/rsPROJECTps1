package server.game.players.actions.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Pins the runecrafting rift tables lifted out of {@code ActionHandler}'s abyss section.
 *
 * <p>There are two kinds and the switch interleaved them by id: eleven rifts teleport
 * and award XP, while the soul rift (7138) and blood rift (7141) hand off to
 * {@code Runecrafting.craftRunes}. The first attempt at this migration extracted only
 * the teleport kind and then deleted the whole id range, silently dropping 7138.
 * {@link #everyRiftInTheAbyssSectionIsRegistered()} exists so that cannot happen again:
 * it asserts coverage of 7129..7141 as a block, not per-table.
 */
class RuneRiftObjectsTest {

	private static final int[][] EXPECTED_TELEPORTS = {
			{ 7129, 1442, 5537, 2583, 4838, 0 },
			{ 7130, 1440, 5535, 2660, 4839, 0 },
			{ 7131, 1446, 5533, 2527, 4833, 0 },
			{ 7132, 1454, 5539, 2162, 4833, 0 },
			{ 7133, 1462, 5541, 2398, 4841, 0 },
			{ 7134, 1452, 5543, 2269, 4843, 0 },
			{ 7135, 1458, 5545, 2464, 4834, 0 },
			{ 7136, 1456, 5547, 2207, 4836, 0 },
			{ 7137, 1444, 5531, 2713, 4836, 0 },
			{ 7139, 1438, 5527, 2845, 4832, 0 },
			{ 7140, 1448, 5529, 2788, 4841, 0 },
	};

	private static final int[][] EXPECTED_CRAFTING = {
			{ 7138, 1460, 5551, 30625 },
			{ 7141, 1450, 5549, 30624 },
	};

	@Test
	void teleportTableMatchesTheSwitchItReplaced() {
		assertTable(EXPECTED_TELEPORTS, RuneRiftObjects.teleportRifts());
	}

	@Test
	void craftingTableMatchesTheSwitchItReplaced() {
		assertTable(EXPECTED_CRAFTING, RuneRiftObjects.craftingRifts());
	}

	@Test
	void everyRiftInTheAbyssSectionIsRegistered() {
		// 7129..7141 inclusive. If a future edit deletes an id range again instead of
		// deriving it from what was matched, this is what fails.
		for (int id = 7129; id <= 7141; id++) {
			assertTrue(ObjectHandler.isRegistered(id, ObjectClick.FIRST),
					"abyss rift " + id + " has no handler");
		}
	}

	@Test
	void noRiftIsInBothTables() {
		Set<Integer> seen = new HashSet<>();
		for (int[] row : EXPECTED_TELEPORTS) {
			assertTrue(seen.add(row[0]), "rift " + row[0] + " appears twice");
		}
		for (int[] row : EXPECTED_CRAFTING) {
			assertTrue(seen.add(row[0]), "rift " + row[0] + " appears twice");
		}
	}

	@Test
	void theTwoCraftingRiftsAreTheSoulAndBloodOnes() {
		// Named because these are the pair that a teleport-only extraction misses.
		assertEquals(1460, RuneRiftObjects.craftingRifts()[0][1], "soul talisman");
		assertEquals(30625, RuneRiftObjects.craftingRifts()[0][3], "soul altar");
		assertEquals(1450, RuneRiftObjects.craftingRifts()[1][1], "blood talisman");
		assertEquals(30624, RuneRiftObjects.craftingRifts()[1][3], "blood altar");
	}

	private static void assertTable(int[][] expected, int[][] actual) {
		assertEquals(expected.length, actual.length, "row count");
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i].length, actual[i].length, "row " + i + " width");
			for (int col = 0; col < expected[i].length; col++) {
				assertEquals(expected[i][col], actual[i][col],
						"row " + i + " column " + col + " (id " + expected[i][0] + ")");
			}
		}
	}
}
