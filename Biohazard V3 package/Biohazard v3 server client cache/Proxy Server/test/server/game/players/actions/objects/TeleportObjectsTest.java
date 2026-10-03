package server.game.players.actions.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Pins the teleport table lifted out of {@code ActionHandler}.
 *
 * <p>Rows are {@code {objectId, x, y, height}}. This is the largest single block of
 * data migrated so far and a wrong coordinate is silent -- the player arrives
 * somewhere plausible but wrong -- so the whole table is asserted, not sampled.
 *
 * <p>The expected table was produced by the same extraction that generated the class,
 * then checked independently two ways: a diff of the source showed the only lines
 * removed were the 184 teleport case lines, and six rows were read back against the
 * pre-migration file by eye.
 */
class TeleportObjectsTest {

	private static final int[][] EXPECTED = {
			{ 492, 2857, 9569, 0 },
			{ 1764, 2857, 3167, 0 },
			{ 9358, 2480, 5175, 0 },
			{ 9359, 2862, 9572, 0 },
			{ 12467, 2974, 9511, 0 },
			{ 12468, 2974, 9511, 0 },
			{ 8930, 1975, 4409, 3 },
			{ 10177, 1798, 4407, 3 },
			{ 10193, 2545, 10143, 0 },
			{ 10195, 1809, 4405, 2 },
			{ 10196, 1807, 4405, 3 },
			{ 10197, 1823, 4404, 2 },
			{ 10198, 1825, 4404, 3 },
			{ 10199, 1834, 4388, 2 },
			{ 10200, 1834, 4390, 3 },
			{ 10201, 1811, 4394, 1 },
			{ 10202, 1812, 4394, 2 },
			{ 10203, 1799, 4386, 2 },
			{ 10204, 1799, 4388, 1 },
			{ 10205, 1796, 4382, 1 },
			{ 10206, 1796, 4382, 2 },
			{ 10207, 1800, 4369, 2 },
			{ 10208, 1802, 4370, 1 },
			{ 10209, 1827, 4362, 1 },
			{ 10210, 1825, 4362, 2 },
			{ 10211, 1863, 4373, 2 },
			{ 10212, 1863, 4371, 1 },
			{ 10213, 1864, 4389, 1 },
			{ 10214, 1864, 4387, 2 },
			{ 10215, 1890, 4407, 0 },
			{ 10216, 1890, 4406, 1 },
			{ 10217, 1957, 4373, 1 },
			{ 10218, 1957, 4371, 0 },
			{ 10219, 1824, 4379, 3 },
			{ 10220, 1824, 4381, 2 },
			{ 10221, 1838, 4375, 2 },
			{ 10222, 1838, 4377, 3 },
			{ 10223, 1850, 4386, 1 },
			{ 10224, 1850, 4387, 2 },
			{ 10225, 1932, 4378, 1 },
			{ 10226, 1932, 4380, 2 },
			{ 10228, 1961, 4393, 3 },
			{ 10229, 1912, 4367, 0 },
			{ 10230, 2899, 4449, 0 },
			{ 2823, 2881, 5310, 2 },
			{ 12230, 2506, 3038, 0 },
			{ 2, 3029, 9582, 0 },
			{ 1765, 3067, 10256, 0 },
			{ 1766, 3016, 3849, 0 },
	};

	@Test
	void tableMatchesTheSwitchItReplaced() {
		int[][] actual = TeleportObjects.teleports();
		assertEquals(EXPECTED.length, actual.length, "row count");
		for (int i = 0; i < EXPECTED.length; i++) {
			assertEquals(4, actual[i].length, "row " + i + " must be {id, x, y, height}");
			for (int col = 0; col < 4; col++) {
				assertEquals(EXPECTED[i][col], actual[i][col],
						"row " + i + " column " + col + " (id " + EXPECTED[i][0] + ")");
			}
		}
	}

	@Test
	void everyTeleportIsRegisteredOnFirstClick() {
		for (int[] row : EXPECTED) {
			assertTrue(ObjectHandler.isRegistered(row[0], ObjectClick.FIRST),
					"teleport object " + row[0] + " has no handler");
		}
	}

	@Test
	void noObjectIdIsListedTwice() {
		// 12467 and 12468 share a destination, which is why they are two rows rather
		// than one; two rows with the *same* id would be a copy-paste slip.
		Set<Integer> seen = new HashSet<>();
		for (int[] row : EXPECTED) {
			assertTrue(seen.add(row[0]), "object " + row[0] + " appears twice");
		}
	}

	@Test
	void theCommentOnlyCaseIsNotPresent() {
		// case 10194 in the switch held only "//c.getPA().movePlayer(2544, 3741, 0)".
		// It never ran, so it must not have been lifted as a working teleport.
		assertEquals(false, ObjectHandler.isRegistered(10194, ObjectClick.FIRST));
	}

	@Test
	void theDagannothLadderBankIsFullyPresent() {
		// 10195..10230 is a contiguous run of one-ladder-per-destination objects, the
		// part of this table most likely to be damaged by a careless edit. Every id in
		// the run is present except 10227, which has an if/else body reaching two
		// destinations and so stayed in the switch; 10194 is the comment-only no-op
		// covered above.
		int present = 0;
		for (int id = 10195; id <= 10230; id++) {
			if (id == 10227) {
				assertEquals(false, ObjectHandler.isRegistered(id, ObjectClick.FIRST),
						"10227 branches, so it should not be a single-destination teleport");
			} else {
				assertTrue(ObjectHandler.isRegistered(id, ObjectClick.FIRST),
						"ladder object " + id + " has no handler");
				present++;
			}
		}
		assertEquals(35, present, "10195..10230 is 36 ids, minus 10227");
	}
}
