package botworkshop.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The world overview's document — {@code BOT_TOOLING.md} §4 (the Layer 1 map view).
 *
 * <p>Two things have to be true for the viewer to draw a continuous world rather than one region:
 * the bounds must cover every entry (or the map is framed wrongly), and regions the export skipped
 * must be present and marked, not omitted (or the map shows holes that belong to the export, not to
 * the world — and those are different claims).
 */
class WorldDocTest {

	@Test
	void theBoundariesCoverEveryEntry() {
		String json = WorldDoc.toJson(List.of(
				new WorldDoc.Entry(12850, 3200, 3200, true, 900, 9),
				new WorldDoc.Entry(12338, 3072, 3264, true, 40, 0),
				new WorldDoc.Entry(9999, 2944, 3136, true, 3, 0)));

		assertTrue(json.contains("\"minX\": 2944"), json);
		assertTrue(json.contains("\"maxX\": 3263"), json);
		assertTrue(json.contains("\"minY\": 3136"), json);
		assertTrue(json.contains("\"maxY\": 3327"), json);
	}

	@Test
	void aRegionTheExportSkippedIsStillListedAndFlagged() {
		String json = WorldDoc.toJson(List.of(
				new WorldDoc.Entry(1, 100, 200, true, 5, 0),
				new WorldDoc.Entry(2, 164, 200, false, 0, 0)));

		assertTrue(json.contains("\"regions\": 2"), json);
		assertTrue(json.contains("\"exported\": 1"), json);
		assertTrue(json.contains("\"exported\": false"), json);
	}

	@Test
	void eachEntryCarriesItsObjectAndBankCounts() {
		String json = WorldDoc.toJson(List.of(
				new WorldDoc.Entry(12850, 3200, 3200, true, 1234, 9)));

		assertTrue(json.contains("\"objects\": 1234"), json);
		assertTrue(json.contains("\"banks\": 9"), json);
		assertTrue(json.contains("\"regionId\": 12850"), json);
	}

	@Test
	void anEmptyWorldIsAZeroedDocumentRatherThanAMalformedOne() {
		String json = WorldDoc.toJson(List.of());

		assertTrue(json.contains("\"regions\": 0"), json);
		assertTrue(json.contains("\"entries\": []"), json);
		assertEquals(0, countOf(json, "\"regionId\""));
	}

	@Test
	void theDocumentIsStableForTheSameInput() {
		List<WorldDoc.Entry> entries = List.of(new WorldDoc.Entry(1, 0, 0, true, 1, 0));
		assertEquals(WorldDoc.toJson(entries), WorldDoc.toJson(entries));
	}

	private static int countOf(String text, String needle) {
		int count = 0;
		int from = 0;
		while (true) {
			int at = text.indexOf(needle, from);
			if (at < 0) {
				return count;
			}
			count++;
			from = at + needle.length();
		}
	}
}
