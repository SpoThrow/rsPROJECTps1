package server.game.bots.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The authored-table parser, against hand-written lines and against the shipped seed file.
 *
 * <p>The seed file is read too, and that is the point of the last test: a parser that accepts
 * invented input while the real file has a typo in it is not a parser that works.
 */
class LocationsConfigTest {

	private static LocationsConfig.Result parse(String... lines) {
		return LocationsConfig.parseLines(Arrays.asList(lines));
	}

	@Test
	void aRegionRowParsesIntoABox() {
		LocationsConfig.Result result = parse(
				"region = draynor_bank x 3091 y 3241 w 6 h 5 plane 0 kind bank tags draynor,river");

		assertTrue(result.problems().isEmpty(), "no problems: " + result.problems());
		assertEquals(1, result.locations().size());
		Location bank = result.locations().get(0);
		assertEquals("draynor_bank", bank.name());
		assertEquals(LocationKind.BANK, bank.kind());
		assertEquals(3091, bank.x());
		assertEquals(3241, bank.y());
		assertEquals(6, bank.width());
		assertEquals(5, bank.height());
		assertEquals(0, bank.plane());
		assertFalse(bank.isPoint());
		assertTrue(bank.hasTag("draynor"));
		assertTrue(bank.hasTag("RIVER"), "tags are case-insensitive");
		assertFalse(bank.hasTag("lumbridge"));
	}

	@Test
	void aPointRowIsOneTileAndDefaultsToPlaneZero() {
		LocationsConfig.Result result = parse("point = lumbridge_altar x 3243 y 3207 kind prayer");

		assertTrue(result.problems().isEmpty(), "no problems: " + result.problems());
		Location altar = result.locations().get(0);
		assertTrue(altar.isPoint());
		assertEquals(1, altar.width());
		assertEquals(1, altar.height());
		assertEquals(0, altar.plane(), "a missing plane is plane 0, not an error");
	}

	@Test
	void aRegionWithoutASizeIsRejectedRatherThanSilentlyBecomingOneTile() {
		// The failure this prevents: "draynor_oaks" landing on a single tile because w/h were left
		// off, and every bot walking to the same square in the middle of a forest.
		LocationsConfig.Result result = parse("region = draynor_oaks x 3103 y 3217 plane 0 kind tree");

		assertTrue(result.locations().isEmpty());
		assertEquals(1, result.problems().size());
		assertTrue(result.problems().get(0).contains("needs w and h"), result.problems().get(0));
	}

	@Test
	void thePlaceWordsFromTheDesignDocAreAcceptedAsAliases() {
		// BOT_LOCATIONS.md A.3 writes "kind mine" and "kind anvil"; the classifier says rock and
		// smithing. Both spellings have to work, or a row copied out of the doc is rejected.
		LocationsConfig.Result result = parse(
				"point = varrock_east_mine x 3285 y 3366 kind mine",
				"point = varrock_anvil x 3228 y 3435 kind anvil",
				"point = a_range x 1 y 1 kind range",
				"point = an_altar x 2 y 2 kind altar",
				"point = a_spot x 3 y 3 kind fish");

		assertTrue(result.problems().isEmpty(), "no problems: " + result.problems());
		assertEquals(LocationKind.ROCK, result.locations().get(0).kind());
		assertEquals(LocationKind.SMITHING, result.locations().get(1).kind());
		assertEquals(LocationKind.COOKING, result.locations().get(2).kind());
		assertEquals(LocationKind.PRAYER, result.locations().get(3).kind());
		assertEquals(LocationKind.FISHING, result.locations().get(4).kind());
	}

	@Test
	void anUnknownKindIsRejectedWithTheOffendingValue() {
		LocationsConfig.Result result = parse("point = mystery x 1 y 1 kind volcano");

		assertTrue(result.locations().isEmpty());
		assertTrue(result.problems().get(0).contains("volcano"), result.problems().get(0));
	}

	@Test
	void anUnknownFieldIsRejectedRatherThanIgnored() {
		// Silently ignoring a field would mean a typo like "plnae 2" puts a bank on the wrong floor
		// and nothing says so.
		LocationsConfig.Result result = parse("point = x x 1 y 1 plnae 2 kind bank");

		assertTrue(result.locations().isEmpty());
		assertTrue(result.problems().get(0).contains("plnae"), result.problems().get(0));
	}

	@Test
	void commentsAndBlankLinesAreSkipped() {
		LocationsConfig.Result result = parse(
				"# a hash comment",
				"// a slash comment",
				"",
				"   ",
				"point = a_bank x 1 y 2 kind bank",
				"point = another x 3 y 4 kind bank # trailing comment");

		assertTrue(result.problems().isEmpty(), "no problems: " + result.problems());
		assertEquals(2, result.locations().size());
		assertEquals("another", result.locations().get(1).name());
	}

	@Test
	void aBadLineIsSkippedAndReportedWithItsLineNumberWithoutLosingTheGoodOnes() {
		LocationsConfig.Result result = parse(
				"point = good_one x 1 y 1 kind bank",
				"point = bad_one x 1 kind bank",
				"point = good_two x 2 y 2 kind bank");

		assertEquals(2, result.locations().size(), "both good rows survive");
		assertEquals(1, result.problems().size());
		assertTrue(result.problems().get(0).startsWith("line 2:"), result.problems().get(0));
	}

	@Test
	void aMissingFileIsAnEmptyTableAndNotAProblem(@TempDir Path dir) {
		// The acceptance criterion: deleting locations.cfg degrades to scanned lookups, not a crash.
		LocationsConfig.Result result = LocationsConfig.load(dir.resolve("nope.cfg"));

		assertTrue(result.locations().isEmpty());
		assertTrue(result.problems().isEmpty(), "absent is not a problem: " + result.problems());
	}

	@Test
	void aFileOnDiskIsReadAsUtf8(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("locations.cfg");
		Files.write(file, Arrays.asList(
				"# authored",
				"point = one_bank x 3091 y 3241 kind bank"),
				StandardCharsets.UTF_8);

		LocationsConfig.Result result = LocationsConfig.load(file);

		assertTrue(result.problems().isEmpty(), "no problems: " + result.problems());
		assertEquals(1, result.locations().size());
		assertEquals("one_bank", result.locations().get(0).name());
	}

	@Test
	void theShippedSeedFileParsesCleanlyAndHoldsTheRowsItClaimsTo() {
		String dataRoot = System.getProperty("workshopDataRoot");
		assertTrue(dataRoot != null && !dataRoot.isBlank(),
				"the test task must set workshopDataRoot to Data/; see build.gradle");

		LocationsConfig.Result result = LocationsConfig.load(
				Path.of(dataRoot, "cfg", "bots", "locations.cfg"));

		assertTrue(result.problems().isEmpty(),
				"the shipped file must parse without a single problem: " + result.problems());
		assertTrue(result.locations().size() >= 10,
				"the seed holds the measured banks, altar and tree regions, got " + result.locations().size());

		// Spot-check the row whose plane is the whole point of carrying a plane at all.
		Location castle = null;
		for (Location location : result.locations()) {
			if ("lumbridge_castle_bank".equals(location.name())) {
				castle = location;
			}
		}
		assertTrue(castle != null, "lumbridge_castle_bank is in the seed");
		assertEquals(2, castle.plane(), "the castle bank is on plane 2, not the courtyard's plane 0");
		assertEquals(LocationKind.BANK, castle.kind());
	}

	@Test
	void knownKindIdsCoverTheTable() {
		assertTrue(LocationsConfig.knownKindIds().contains("bank"));
		assertTrue(LocationsConfig.knownKindIds().contains("tree"));
		assertNull(LocationKind.byId("nonsense"));
	}

	/**
	 * The formatter is the parser's inverse, over the real authored rows.
	 *
	 * <p>This is what lets the editor trust {@code toRow}: the map viewer drafts a row, the server
	 * re-parses and re-formats it, and what lands in the file came out of this method. If the
	 * round-trip were lossy — a dropped tag, a 1x1 region rewritten as a point and coming back
	 * different — the editor would be quietly authoring something other than what was drawn.
	 */
	@Test
	void everyAuthoredRowRoundTripsThroughTheFormatterUnchanged() {
		String dataRoot = System.getProperty("workshopDataRoot");
		assertTrue(dataRoot != null && !dataRoot.isBlank(), "workshopDataRoot is required");

		LocationsConfig.Result shipped = LocationsConfig.load(
				Path.of(dataRoot, "cfg", "bots", "locations.cfg"));
		assertFalse(shipped.locations().isEmpty(), "the seed file has rows to round-trip");

		for (Location location : shipped.locations()) {
			String row = LocationsConfig.toRow(location);
			LocationsConfig.Result reparsed = LocationsConfig.parseLines(Arrays.asList(row));
			assertTrue(reparsed.problems().isEmpty(),
					"the formatter emitted a row the parser rejects: " + row + " — " + reparsed.problems());
			assertEquals(location, reparsed.locations().get(0),
					"round-trip changed the location: " + row);
		}
	}

	@Test
	void aPointFormatsAsAPointAndABoxAsARegion() {
		String point = LocationsConfig.toRow(Location.point("a_bank", LocationKind.BANK, 1, 2, 0));
		String box = LocationsConfig.toRow(new Location("oaks", LocationKind.TREE, 10, 20, 0, 5, 4,
				Arrays.asList("oak")));

		assertTrue(point.startsWith("point  = "), point);
		assertFalse(point.contains(" w "), "a point must not carry a size: " + point);
		assertTrue(box.startsWith("region = "), box);
		assertTrue(box.contains(" w 5 h 4"), box);
		assertTrue(box.contains(" tags oak"), box);
	}

	@Test
	void appendingAddsACommentedBlockAndLeavesWhatWasAlreadyThere(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("locations.cfg");
		Files.write(file, Arrays.asList("# hand-written", "point = old_bank x 1 y 1 kind bank"),
				StandardCharsets.UTF_8);

		int written = LocationsConfig.appendRows(file, Arrays.asList(
				Location.point("new_bank", LocationKind.BANK, 2, 2, 0),
				new Location("new_oaks", LocationKind.TREE, 5, 6, 0, 3, 3, Arrays.asList("oak"))),
				"test");

		assertEquals(2, written);
		LocationsConfig.Result result = LocationsConfig.load(file);
		assertTrue(result.problems().isEmpty(), "the appended block parses: " + result.problems());
		assertEquals(3, result.locations().size(), "the pre-existing row is untouched");
		assertEquals("old_bank", result.locations().get(0).name());
		assertEquals("new_bank", result.locations().get(1).name());
		assertEquals("new_oaks", result.locations().get(2).name());

		String text = String.join("\n", Files.readAllLines(file, StandardCharsets.UTF_8));
		assertTrue(text.contains("hand-written"), "the file was appended to, not rewritten");
		assertTrue(text.contains("Bot Workshop editor"), text);
	}

	@Test
	void appendingNothingWritesNothingAndDoesNotCreateTheFile(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("absent.cfg");
		assertEquals(0, LocationsConfig.appendRows(file, Arrays.asList(), "test"));
		assertFalse(Files.exists(file), "an empty append must not leave an empty file behind");
	}
}
