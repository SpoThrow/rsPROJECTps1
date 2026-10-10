package server.content.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import server.Config;
import server.content.skills.Fletching.Fletch;

/**
 * Pins the fletching table and the two facts the ticked rewrite depends on: a product is what
 * names its log, and one log is one action.
 *
 * <p>This is the data half of {@code fletchBow}. The behaviour half — one log consumed per
 * tick, the action ending on a walk — needs a live {@code Client} and an event loop, so what
 * is asserted here is the part that fails silently otherwise: a product id that maps to no
 * log, or two table entries claiming the same product, would show up as a bow that simply
 * cannot be made rather than as an error.
 */
class FletchingTest {

	/** product id -> {log id, level required, xp}, exactly as the enum is written. */
	private static final int[][] TABLE = {
			{ 52, 1511, 1, 5 },   // arrow shafts
			{ 841, 1511, 5, 5 },  // shortbow
			{ 839, 1511, 10, 10 },// longbow
			{ 843, 1521, 20, 17 },// oak shortbow
			{ 845, 1521, 25, 25 },// oak longbow
			{ 849, 1519, 35, 34 },// willow shortbow
			{ 847, 1519, 40, 42 },// willow longbow
			{ 853, 1517, 50, 50 },// maple shortbow
			{ 851, 1517, 55, 59 },// maple longbow
			{ 857, 1515, 65, 68 },// yew shortbow
			{ 855, 1515, 70, 75 },// yew longbow
			{ 861, 1513, 80, 84 },// magic shortbow
			{ 859, 1513, 87, 92 },// magic longbow
	};

	@Test
	void everyProductResolvesToTheLogItIsCutFrom() {
		// The fix in fletchBow is that the log comes from the product rather than from whichever
		// log opened the make-X interface. That only works if this mapping exists for each
		// product, so it is asserted for all of them.
		for (int[] row : TABLE) {
			Fletch f = Fletching.forBow(row[0]);
			assertNotNull(f, "product " + row[0] + " has no table entry");
			assertEquals(row[1], f.getLogID(), "log for product " + row[0]);
			assertEquals(row[2], f.getLevelReq(), "level for product " + row[0]);
			assertEquals(row[3], f.getXp(), "xp for product " + row[0]);
			assertEquals(row[0], f.getBowID(), "the product id must round-trip");
		}
	}

	@Test
	void noTwoProductsAreTheSame() {
		// forBow returns the first match, so a duplicate product id would make the second entry
		// unreachable and the product would silently use the wrong log, level and xp.
		Set<Integer> products = new HashSet<>();
		for (Fletch f : Fletch.values()) {
			assertTrue(products.add(f.getBowID()), "duplicate product id " + f.getBowID() + " in " + f);
		}
	}

	@Test
	void aLogIdIsNotAProductId() {
		// forBow matches on the product, not the log. If the two id spaces ever overlapped, a
		// log could be mistaken for a product and the guards in fletchBow would stop working.
		Set<Integer> logs = new HashSet<>();
		for (Fletch f : Fletch.values()) {
			logs.add(f.getLogID());
		}
		for (Fletch f : Fletch.values()) {
			assertFalse(logs.contains(f.getBowID()),
					"product " + f.getBowID() + " is also used as a log id");
		}
	}

	@Test
	void forBowAnswersNullRatherThanThrowingForAnythingElse() {
		// fletchBow guards on null, so an id that is not a product must not throw. A log id is
		// the case that actually reaches it.
		assertNull(Fletching.forBow(1511), "a log id is not a product");
		assertNull(Fletching.forBow(-1));
		assertNull(Fletching.forBow(0));
		assertNull(Fletching.forBow(3150));
	}

	@Test
	void arrowShaftsAreLevelOneAndFifteenPerLog() {
		Fletch shafts = Fletching.forBow(52);
		assertNotNull(shafts, "the shaft entry must exist");
		assertEquals(1511, shafts.getLogID(), "shafts come from normal logs");
		// The ticked rewrite is the first code to enforce this entry's level, so if it regressed
		// to the 15 it used to carry, low-level players would lose shafts they could always make.
		assertEquals(1, shafts.getLevelReq(), "shafts are level 1 in OSRS");
		assertEquals(15, Fletching.ARROW_SHAFTS_PER_LOG, "one log is fifteen shafts");
	}

	@Test
	void bowLevelRequirementsAreTheKnownValues() {
		// Guards against a level being edited without the change being noticed: the numbers here
		// are the ones the make-X interface advertises, so a silent edit would make the interface
		// lie about what the player can make.
		Map<Integer, Integer> levels = new HashMap<>();
		for (Fletch f : Fletch.values()) {
			levels.put(f.getBowID(), f.getLevelReq());
		}
		assertEquals(5, levels.get(841));
		assertEquals(20, levels.get(843));
		assertEquals(35, levels.get(849));
		assertEquals(50, levels.get(853));
		assertEquals(65, levels.get(857));
		assertEquals(80, levels.get(861));
	}

	@Test
	void theActionIsTickBasedByDefault() {
		// The default the programme asked for: one log per action, not a whole inventory in one
		// call. Flipping this is what reverts to fletchBowInstant.
		assertTrue(Config.FLETCHING_ONE_BY_ONE_ENABLED,
				"fletching should be one-by-one unless deliberately turned off");
	}

	@Test
	void everyBowUsesAnAnimationForCuttingLogs() {
		// Not a per-product animation: the same cut plays for every bow and every shaft. Pinned
		// because a wrong id here is an action with no visual at all.
		assertEquals(1248, Fletching.FLETCH_ANIMATION);
	}
}
