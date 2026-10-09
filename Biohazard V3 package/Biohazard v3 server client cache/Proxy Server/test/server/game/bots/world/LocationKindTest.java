package server.game.bots.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The one vocabulary: {@link LocationKind} ids and the service flag must agree with the classifier.
 *
 * <p>Two tables describe the same kinds — the place table here and {@link ResourceKinds} — and the
 * only thing keeping them one vocabulary is that the ids are literally the same strings. If they ever
 * drift, a bot asks for {@code tree} and the map draws {@code wood}, and nothing fails loudly. Hence
 * this test.
 */
class LocationKindTest {

	@Test
	void theObjectKindsAreExactlyTheKindsTheClassifierCanProduce() {
		// Every kind the scanner can ever emit must be a kind LocationKind knows, or a scanned hit
		// would be dropped on the floor for want of a name.
		for (ResourceKinds.Row row : ResourceKinds.rows()) {
			LocationKind kind = LocationKind.byId(row.kind());
			assertTrue(kind != null, "the classifier can emit " + row.kind()
					+ " but LocationKind cannot name it");
			assertTrue(kind.isObjectKind(), row.kind() + " must be scannable, since it comes from the world");
		}

		int objectKinds = 0;
		for (LocationKind kind : LocationKind.values()) {
			if (kind.isObjectKind()) {
				objectKinds++;
			}
		}
		assertEquals(ResourceKinds.rows().size(), objectKinds,
				"the two tables list the same number of object kinds");
	}

	@Test
	void theServiceFlagAgreesWithTheClassifier() {
		for (ResourceKinds.Row row : ResourceKinds.rows()) {
			LocationKind kind = LocationKind.byId(row.kind());
			assertEquals(row.isService(), kind.isService(),
					row.kind() + " is described differently by the two tables");
		}
	}

	@Test
	void thePlaceOnlyKindsAreNotSomethingAWorldScanCouldFind() {
		// A teleport, a monster, a shop and a master are facts about the server, not objects standing
		// in the world, so a family of them must not be given a scan arm.
		assertFalse(LocationKind.TELEPORT.isObjectKind());
		assertFalse(LocationKind.MONSTER.isObjectKind());
		assertFalse(LocationKind.SHOP.isObjectKind());
		assertFalse(LocationKind.MASTER.isObjectKind());
	}

	@Test
	void theDesignDocsPlaceWordsAreAliasesOfTheCanonicalKinds() {
		assertEquals(LocationKind.ROCK, LocationKind.byId("mine"));
		assertEquals(LocationKind.SMITHING, LocationKind.byId("anvil"));
		assertEquals(LocationKind.COOKING, LocationKind.byId("range"));
		assertEquals(LocationKind.COOKING, LocationKind.byId("fire"));
		assertEquals(LocationKind.PRAYER, LocationKind.byId("altar"));
		assertEquals(LocationKind.FISHING, LocationKind.byId("fish"));
	}

	@Test
	void lookupIsCaseInsensitiveAndTrimmed() {
		assertEquals(LocationKind.BANK, LocationKind.byId("  BANK "));
		assertEquals(LocationKind.TREE, LocationKind.byId("Tree"));
	}

	@Test
	void anUnknownKindIsNullRatherThanAGuess() {
		assertNull(LocationKind.byId("volcano"));
		assertNull(LocationKind.byId(""));
		assertNull(LocationKind.byId(null));
	}

	@Test
	void everyKindHasANonEmptyIdAndTheyAreAllDistinct() {
		java.util.Set<String> seen = new java.util.HashSet<String>();
		for (LocationKind kind : LocationKind.values()) {
			assertTrue(kind.id() != null && !kind.id().isEmpty());
			assertTrue(seen.add(kind.id()), "duplicate kind id " + kind.id());
		}
	}
}
