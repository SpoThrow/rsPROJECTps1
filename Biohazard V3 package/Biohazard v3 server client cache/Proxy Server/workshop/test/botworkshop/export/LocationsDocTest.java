package botworkshop.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import botworkshop.WorkshopFixture;
import server.game.bots.world.Location;
import server.game.bots.world.LocationKind;
import server.game.bots.world.LocationsConfig;

/**
 * The place overlay's document — {@code BOT_ROADMAP.md} §5.2.
 *
 * <p>The writer is checked against the parser rather than against a literal: the interesting
 * property is that a row the editor displays can be fed straight back to
 * {@link LocationsConfig#parseLines}, because that is exactly what the editor's "append" does. A
 * hand-written expected string would pin the formatting and miss the thing that matters.
 */
class LocationsDocTest {

	@Test
	void everyShippedRowSerialisesWithTextTheParserReadsBackUnchanged() throws IOException {
		LocationsConfig.Result shipped = LocationsConfig.load(
				Path.of(WorkshopFixture.dataRoot().toString(), "cfg", "bots", "locations.cfg"));
		assertFalse(shipped.locations().isEmpty(), "the seed file has rows");

		String json = LocationsDoc.toJson(shipped.locations());
		assertTrue(json.contains("\"count\": " + shipped.locations().size()), json);

		for (Location location : shipped.locations()) {
			// The row field is the whole point: it is what the editor shows and what it would append.
			String row = LocationsConfig.toRow(location);
			assertTrue(json.contains("\"row\": " + quoted(row)), "missing row for " + location.name());

			LocationsConfig.Result reparsed = LocationsConfig.parseLines(List.of(row));
			assertTrue(reparsed.problems().isEmpty(), "row is unreadable: " + row);
			assertEquals(location, reparsed.locations().get(0), "row changed meaning: " + row);
		}
	}

	@Test
	void theDocumentCarriesTheKindsTheParserAccepts() {
		String json = LocationsDoc.toJson(List.of());

		assertTrue(json.contains("\"kinds\": ["), json);
		assertTrue(json.contains("\"bank\""), json);
		assertTrue(json.contains("\"tree\""), json);
		assertTrue(json.contains("\"prayer\""), json);
	}

	/**
	 * `objectKinds` is what lets the editor tell "this box is empty" from "this kind has no objects
	 * behind it at all". Getting the set wrong either way is a false report on every row of that
	 * kind, so it is pinned to the runtime's own predicate rather than to a list written out here.
	 */
	@Test
	void theObjectKindsAreExactlyTheOnesTheRuntimeCanClassify() {
		String json = LocationsDoc.toJson(List.of());
		String list = objectKindsOf(json);

		for (LocationKind kind : LocationKind.values()) {
			assertEquals(kind.isObjectKind(), list.contains("\"" + kind.id() + "\""),
					kind.id() + " should" + (kind.isObjectKind() ? "" : " not")
							+ " be present in objectKinds");
		}
	}

	@Test
	void aKindWithNoWorldObjectsIsNotAnObjectKind() {
		List<String> list = List.of(objectKindsOf(LocationsDoc.toJson(List.of())).split(","));

		// Each of these is an authored place, not something a region scan could ever find; counting
		// objects for one would report an empty box on a perfectly good row.
		for (String kind : List.of("shop", "teleport", "monster", "master")) {
			assertFalse(list.contains("\"" + kind + "\""), kind + " must not be counted as objects");
		}
	}

	/** The `objectKinds` array's body, so the assertions do not read the whole document. */
	private static String objectKindsOf(String json) {
		int at = json.indexOf("\"objectKinds\": [");
		assertTrue(at >= 0, json);
		int start = json.indexOf('[', at) + 1;
		int end = json.indexOf(']', start);
		assertTrue(end > start, json);
		return json.substring(start, end);
	}

	@Test
	void aPointIsReportedAsAPointAndCarriesItsPlaneAndTags() {
		String json = LocationsDoc.toJson(List.of(
				new Location("castle_bank", LocationKind.BANK, 3207, 3221, 2, 4, 1,
						List.of("lumbridge", "castle"))));

		assertTrue(json.contains("\"plane\": 2"), json);
		assertTrue(json.contains("\"point\": false"), json);
		assertTrue(json.contains("\"w\": 4"), json);
		assertTrue(json.contains("\"tags\": ["), json);
		assertTrue(json.contains("\"lumbridge\""), json);
		assertTrue(json.contains("plane 2"), json);
	}

	@Test
	void theDocumentIsStableForTheSameInput() {
		List<Location> rows = new ArrayList<Location>();
		rows.add(Location.point("a", LocationKind.BANK, 1, 2, 0));
		rows.add(Location.point("b", LocationKind.TREE, 3, 4, 0));
		assertEquals(LocationsDoc.toJson(rows), LocationsDoc.toJson(rows));
	}

	@Test
	void anEmptyTableIsAnEmptyArrayNotAMissingField(@org.junit.jupiter.api.io.TempDir Path dir)
			throws IOException {
		Path empty = dir.resolve("empty.cfg");
		Files.write(empty, "# nothing yet".getBytes(StandardCharsets.UTF_8));
		LocationsConfig.Result result = LocationsConfig.load(empty);

		String json = LocationsDoc.toJson(result.locations());
		assertTrue(json.contains("\"count\": 0"), json);
		assertTrue(json.contains("\"rows\": []"), json);
	}

	private static String quoted(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}
}
