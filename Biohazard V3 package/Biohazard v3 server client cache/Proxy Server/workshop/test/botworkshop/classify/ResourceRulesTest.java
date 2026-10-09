package botworkshop.classify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import botworkshop.WorkshopFixture;
import botworkshop.data.LocDefs;

/**
 * The icon layer is what makes the map usable, so these check it against the real archive rather
 * than only against hand-made definitions — the interesting failures are the objects the table gets
 * wrong, not the ones it was written for.
 */
class ResourceRulesTest {

	private static LocDefs defs;

	@BeforeAll
	static void load() throws IOException {
		defs = LocDefs.load(WorkshopFixture.locDat(), WorkshopFixture.locIdx());
	}

	@Test
	void aTreeClassifiesAsAResourceNotAService() {
		assertEquals("tree", ResourceRules.classify(defs.get(1276)));
		assertFalse(ResourceRules.isService("tree"));
	}

	@Test
	void rocksClassifyAsRocks() {
		assertEquals("rock", ResourceRules.classify(defs.get(2091)));
	}

	@Test
	void aBankBoothClassifiesAsABankThroughItsName() {
		// The case the two-pass design exists for: 2213 has no "Bank" action.
		assertEquals("bank", ResourceRules.classify(defs.get(2213)));
		assertTrue(ResourceRules.isService("bank"));
	}

	@Test
	void theBareWordBankMatchesInFullSoBankSceneryIsNotAnIconForIt() {
		// Matching "bank" as a substring put a bank icon on 311 objects instead of 176: it caught
		// "Bank wall", "Bank sign", "Bank table" and the rest of the bank building's fittings.
		// The exact-name list exists for exactly this, so it is asserted rather than assumed.
		ResourceRules.Rule bank = ResourceRules.rule("bank");
		assertTrue(bank.exactNames().contains("bank"), "bank is an exact name");
		assertFalse(bank.names().contains("bank"), "and not a substring, which would over-match");
		assertTrue(bank.names().contains("bank booth"), "booths still match as a substring");
		assertTrue(bank.names().contains("bank chest"), "chests too");
	}

	@Test
	void anUnclassifiedObjectStaysUnclassified() {
		// A door is neither a resource nor a service. Forcing it into a category would put an icon
		// on the map that an author could build a bot around, so null is the correct answer.
		assertNull(ResourceRules.classify(defs.get(1530)));
	}

	@Test
	void aNullDefinitionIsNotAnError() {
		assertNull(ResourceRules.classify(null));
	}

	@Test
	void everyRuleHasADistinctKindSoTheFilterPanelCannotCollide() {
		Map<String, Integer> seen = new java.util.HashMap<String, Integer>();
		for (ResourceRules.Rule rule : ResourceRules.rules()) {
			assertNull(seen.put(rule.kind(), 1), "duplicate kind " + rule.kind());
			assertFalse(rule.kind().isEmpty());
		}
		assertTrue(ResourceRules.rules().size() >= 7, "the table ships the doc's rows");
	}

	@Test
	void theDocRowsAreAllPresent() {
		for (String kind : new String[] { "tree", "rock", "fishing", "bank", "cooking", "smithing", "prayer" }) {
			assertNotNull(ResourceRules.rule(kind), "BOT_TOOLING.md 1b row missing: " + kind);
		}
	}

	@Test
	void anUnknownKindIsNotAService() {
		assertFalse(ResourceRules.isService("nonsense"));
		assertNull(ResourceRules.rule("nonsense"));
	}

	@Test
	void theTableMatchesWhatTheShippedCacheActuallyContains() {
		// A sanity check on the whole table at once: if a row matched nothing, the icon layer would
		// silently show an empty category and the author would think the world had no trees.
		Map<String, Integer> counts = coverage();
		assertTrue(counts.getOrDefault("tree", 0) > 100, "choppable objects, got " + counts.get("tree"));
		assertTrue(counts.getOrDefault("rock", 0) > 100, "mineable objects, got " + counts.get("rock"));
		assertTrue(counts.getOrDefault("fishing", 0) > 0, "fishing spots, got " + counts.get("fishing"));
		assertTrue(counts.getOrDefault("bank", 0) > 0, "banks, got " + counts.get("bank"));
		assertTrue(counts.getOrDefault("prayer", 0) > 0, "altars, got " + counts.get("prayer"));
	}

	@Test
	void cookingAndSmithingCannotBeFoundByActionInThisCache() {
		// Recorded because it contradicts BOT_TOOLING.md §1b, which lists Cook and Smith as icon
		// rules. Measured across all 42001 entries, neither action appears on a single object: the
		// cache simply does not label ranges or anvils that way, so an action-keyed row can never
		// match and the editor needs a name- or id-based rule before it can iconise them.
		//
		// If this ever fails, the world changed and the rule may now work — update the comment in
		// ResourceRules with the new evidence rather than just deleting the assertion.
		Map<String, Integer> counts = coverage();
		assertEquals(0, counts.getOrDefault("cooking", 0), "no object in this cache has a Cook action");
		assertEquals(0, counts.getOrDefault("smithing", 0), "no object in this cache has a Smith action");
	}

	/** How many objects in the archive each rule matches. */
	private static Map<String, Integer> coverage() {
		Map<String, Integer> counts = new java.util.HashMap<String, Integer>();
		for (int id = 0; id < defs.count(); id++) {
			String kind = ResourceRules.classify(defs.get(id));
			if (kind != null) {
				Integer n = counts.get(kind);
				counts.put(kind, n == null ? 1 : n + 1);
			}
		}
		return counts;
	}

	@Test
	void isServiceAgreesWithTheRuleTable() {
		for (ResourceRules.Rule rule : ResourceRules.rules()) {
			assertEquals(rule.isService(), ResourceRules.isService(rule.kind()));
		}
	}
}
