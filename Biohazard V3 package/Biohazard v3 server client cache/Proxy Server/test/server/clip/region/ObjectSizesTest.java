package server.clip.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

/**
 * Pins the {@code Data/objectSize.cfg} reader.
 *
 * <p>The parser half runs on every build. The second half only runs when
 * {@code -DobjectSizeCfg=<path>} is set (the {@code test} task sets it, mirroring
 * {@code RealCharacterFilesTest}'s {@code realCharactersDir}) and asserts the <em>shipped</em> table
 * really does carry the footprints that motivated this class — because the whole reason it exists is
 * that the cache reports those objects as 1x1, and a silently-empty table would look identical to a
 * working one from the caller's side.
 */
class ObjectSizesTest {

	private static final String TREE = "objectId = 1276\tTree\t\t\t\t\t2x2\t\"One of the most common trees in RuneScape.\"";

	@Test
	void parsesTheRealLineFormat() {
		ObjectSizes sizes = ObjectSizes.parse(Collections.singletonList(TREE));
		assertTrue(sizes.has(1276));
		assertEquals(2, sizes.width(1276, 1));
		assertEquals(2, sizes.height(1276, 1));
		assertEquals(1, sizes.count());
	}

	@Test
	void aMissingObjectFallsBackToTheCallerSuppliedSize() {
		ObjectSizes sizes = ObjectSizes.parse(Collections.singletonList(TREE));
		assertFalse(sizes.has(9999));
		// The fallback is normally the cache's loc.dat size, which must survive untouched.
		assertEquals(3, sizes.width(9999, 3));
		assertEquals(5, sizes.height(9999, 5));
	}

	@Test
	void theSizeIsTheFirstNxMFieldNotNumbersInTheNameOrExamine() {
		// The examine is the last column and may embed digits; the name precedes the size.
		String line = "objectId = 42\tCrate of 4x2 apples\t\t\t1x3\t\"Contains 9x9 nails.\"";
		ObjectSizes sizes = ObjectSizes.parse(Collections.singletonList(line));
		assertEquals(1, sizes.width(42, 9));
		assertEquals(3, sizes.height(42, 9));
	}

	@Test
	void nonSquareFootprintsKeepTheirOrientation() {
		// Orientation matters: 2x3 is not 3x2, and the caller relies on both being preserved.
		ObjectSizes sizes = ObjectSizes.parse(Collections.singletonList(
				"objectId = 54\tStairs\t\t\t2x3\t\"It's a flight of stairs.\""));
		assertEquals(2, sizes.width(54, 1));
		assertEquals(3, sizes.height(54, 1));
	}

	@Test
	void junkLinesAreSkippedRatherThanFailingTheBuildOrTheBoot() {
		ObjectSizes sizes = ObjectSizes.parse(Arrays.asList(
				"ID------Name--------Size----Examin Info",
				"",
				"objectId = notanumber\tBroken",
				"objectId = 100\tNo size column here",
				"objectId = 101\tWeird\t\tx\t\"no digits\"",
				TREE));
		assertEquals(1, sizes.count());
		assertTrue(sizes.has(1276));
	}

	@Test
	void aZeroOrNegativeFootprintIsClampedToOne() {
		ObjectSizes sizes = ObjectSizes.parse(Collections.singletonList(
				"objectId = 7\tZero\t\t\t0x0\t\"nothing\""));
		assertEquals(1, sizes.width(7, 9));
		assertEquals(1, sizes.height(7, 9));
	}

	@Test
	void aMissingFileYieldsAnEmptyTableInsteadOfNull() {
		// load() must never return null: Region.addObject calls it on every object at boot.
		ObjectSizes missing = ObjectSizes.load(Paths.get("./definitely/not/here.cfg"));
		assertEquals(0, missing.count());
		assertEquals(4, missing.width(1276, 4));
	}

	@Test
	void theShippedTableCarriesTheFootprintsThatMotivatedThisClass() throws Exception {
		String configured = System.getProperty("objectSizeCfg");
		assumeTrue(configured != null, "objectSizeCfg system property not set");
		Path path = Paths.get(configured);
		assumeTrue(Files.isRegularFile(path), "shipped objectSize.cfg not found at " + path);

		ObjectSizes sizes = ObjectSizes.load(path);
		assertTrue(sizes.count() > 8000, "expected the full table, got " + sizes.count());

		// The user's original report, and the family around it.
		assertEquals(2, sizes.width(1276, 1), "Tree 1276");
		assertEquals(2, sizes.height(1276, 1), "Tree 1276");
		for (int tree : new int[] { 1277, 1278, 1279, 1280, 1306 }) {
			assertEquals(2, sizes.width(tree, 1), "tree " + tree);
			assertEquals(2, sizes.height(tree, 1), "tree " + tree);
		}
		assertEquals(3, sizes.width(1281, 1), "Oak 1281");
		assertEquals(3, sizes.height(1281, 1), "Oak 1281");

		// The three objects the old hardcoded Region.addObject hack covered by hand.
		assertEquals(2, sizes.width(410, 1), "Altar of Guthix 410");
		assertEquals(2, sizes.height(410, 1), "Altar of Guthix 410");
		assertTrue(sizes.has(409), "Altar 409 must be covered");
		assertTrue(sizes.has(6552), "Altar 6552 must be covered");

		// A handful of large scenery that the cache reports as 1x1.
		assertEquals(4, sizes.width(21, 1), "Mysterious statue 21");
		assertEquals(4, sizes.height(21, 1), "Mysterious statue 21");
	}
}
