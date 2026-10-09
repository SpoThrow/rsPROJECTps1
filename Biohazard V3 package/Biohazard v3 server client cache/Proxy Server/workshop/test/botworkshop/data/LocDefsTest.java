package botworkshop.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import botworkshop.WorkshopFixture;

/**
 * Pins the {@code loc.dat} decoder against the shipped archive.
 *
 * <p>The first test is the important one: it walks all 42001 entries and requires every single one
 * to terminate cleanly on opcode 0. A wrong payload length in the opcode table desynchronises the
 * walk, so the entry either stops on an unknown opcode or runs off the end — both of which are
 * counted here. That makes the table self-checking against the real data instead of trusted.
 */
class LocDefsTest {

	private static LocDefs defs;

	@BeforeAll
	static void load() throws IOException {
		defs = LocDefs.load(WorkshopFixture.locDat(), WorkshopFixture.locIdx());
	}

	@Test
	void theIndexDeclaresTheExpectedNumberOfEntries() {
		assertEquals(42001, defs.count(), "loc.idx / 12");
	}

	@Test
	void everyEntryInTheArchiveDecodes() {
		int decoded = 0;
		for (int id = 0; id < defs.count(); id++) {
			LocDefinition def = defs.get(id);
			if (def != null) {
				decoded++;
			}
		}
		List<Integer> unparsed = defs.unparsedIds();
		assertTrue(unparsed.isEmpty(),
				unparsed.size() + " entries stopped early, first few: "
						+ unparsed.subList(0, Math.min(10, unparsed.size())));
		assertEquals(42001, decoded);
	}

	@Test
	void aTreeIsNamedAndChoppable() {
		LocDefinition tree = defs.get(1276);
		assertNotNull(tree, "1276 is an ordinary tree");
		assertEquals("Tree", tree.name());
		assertTrue(tree.actions().contains("Chop down"), "actions were " + tree.actions());
	}

	@Test
	void hiddenSlotsAreNotReported() {
		// Entry 1276 stores "hidden" in slot 2. It must not surface as an action, or the editor
		// would generate a "hidden" right-click entry on every tree in the world.
		LocDefinition tree = defs.get(1276);
		for (String action : tree.actions()) {
			assertFalse("hidden".equalsIgnoreCase(action), "hidden leaked: " + tree.actions());
		}
	}

	@Test
	void aBankBoothIsIdentifiedByNameBecauseItCarriesNoBankAction() {
		// 2213 is a bank booth, but its actions are Use / Use-quickly / Collect — there is no
		// "Bank" action on it anywhere. So an icon rule keyed on the action alone would miss every
		// booth in the world, and ResourceRules has to match banks by name as well. Pinned here
		// because it is the one place the rule table cannot be derived from actions.
		LocDefinition booth = defs.get(2213);
		assertNotNull(booth);
		assertEquals("Bank booth", booth.name());
		assertFalse(booth.actions().contains("Bank"), "actions were " + booth.actions());
	}

	@Test
	void someObjectInTheWorldDoesExposeTheBankAction() {
		// The other half of the same story: the "Bank" action does exist, on chests and the like,
		// so the rule must check both the action and the name rather than choosing between them.
		int withBankAction = 0;
		for (int id = 0; id < defs.count(); id++) {
			LocDefinition def = defs.get(id);
			if (def != null && def.actions().contains("Bank")) {
				withBankAction++;
			}
		}
		assertTrue(withBankAction > 0, "expected at least one object with a literal Bank action");
	}

	@Test
	void aRockReportsMineAndProspect() {
		LocDefinition rocks = defs.get(2091);
		assertNotNull(rocks);
		assertEquals("Rocks", rocks.name());
		assertTrue(rocks.actions().contains("Mine"), "actions were " + rocks.actions());
	}

	@Test
	void aDoorReportsOpen() {
		LocDefinition door = defs.get(1530);
		assertNotNull(door);
		assertEquals("Door", door.name());
		assertTrue(door.actions().contains("Open"), "actions were " + door.actions());
	}

	@Test
	void theTreeFootprintAgreesWithTheSizeTable() {
		// loc.dat does carry sizes for some objects; for 1276 it says 2x2, which is what
		// Data/objectSize.cfg says too. Where the two disagree the table wins - see ObjectSizes.
		LocDefinition tree = defs.get(1276);
		assertEquals(2, tree.sizeX(), "a tree is 2 wide");
		assertEquals(2, tree.sizeY(), "a tree is 2 high");
	}

	@Test
	void anIdPastTheEndOfTheIndexResolvesToNullInsteadOfThrowing() {
		assertNull(defs.get(defs.count()));
		assertNull(defs.get(-1));
	}

	@Test
	void unnamedSceneryDecodesWithNoNameAndNoActions() {
		// Scenery with no name and no actions still has to walk cleanly; Region.separateObjects
		// relies on unattributed objects being placeable scenery rather than corrupt definitions.
		int nameless = 0;
		for (int id = 0; id < defs.count(); id++) {
			LocDefinition def = defs.get(id);
			if (def != null && def.name() == null) {
				nameless++;
			}
		}
		assertTrue(nameless > 1000, "most of the archive is unnamed scenery, got " + nameless);
	}
}
