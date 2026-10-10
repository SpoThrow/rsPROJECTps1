package botworkshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import botworkshop.ResolveScripts.Verdict;
import botworkshop.ResolveScripts.Wanted;
import server.game.bots.world.LocationKind;

/**
 * The two halves of {@link ResolveScripts} that decide the answer, checked without a world.
 *
 * <p>{@code WorldCensus.load} needs 1226 region files, so a test that drove the CLI could only ever
 * assert the report for one checkout — and the interesting cases (a kind the world has none of, a kind
 * with no authored place) are not in this checkout's scripts at all. Both are therefore injected:
 * {@code WorldCensus.of} takes counts, and the verdict is a pure function. What is left to check is
 * exactly the part that was written by hand, which is the point.
 */
class ResolveScriptsTest {

	/** A document whose root names {@code kind}, so the walk has something schema-shaped to read. */
	private static String walkTo(String kind) {
		return "{\"root\": {\"node\": \"walk_to_nearest\", \"kind\": \"" + kind + "\", \"range\": 3}}";
	}

	@Test
	void readsKindsOffTheSchemaRatherThanANodeList() {
		Wanted wanted = ResolveScripts.wanted(walkTo("rock"));
		assertEquals(List.of(LocationKind.ROCK), new ArrayList<LocationKind>(wanted.kinds()));
		assertEquals(Map.of("walk_to_nearest", 1), wanted.nodes(LocationKind.ROCK));
	}

	@Test
	void findsAKindBehindACompositeAndCountsTheNodesThatAskForIt() {
		// A sequence holding two gathers, so the walk has to descend a NODE and a NODE_LIST and
		// report the kind once per asking node rather than once per document.
		String json = "{\"root\": {\"node\": \"repeat\", \"count\": -1, \"child\": {"
				+ "  \"node\": \"sequence\", \"children\": ["
				+ "    {\"node\": \"gather\", \"kind\": \"tree\", \"itemId\": 1511},"
				+ "    {\"node\": \"gather\", \"kind\": \"tree\", \"itemId\": 1511},"
				+ "    {\"node\": \"walk_to_nearest\", \"kind\": \"bank\", \"range\": 2}"
				+ "  ]}}}";
		Wanted wanted = ResolveScripts.wanted(json);
		assertEquals(List.of(LocationKind.TREE, LocationKind.BANK),
				new ArrayList<LocationKind>(wanted.kinds()));
		assertEquals(Map.of("gather", 2), wanted.nodes(LocationKind.TREE));
		assertEquals(Map.of("walk_to_nearest", 1), wanted.nodes(LocationKind.BANK));
	}

	@Test
	void aScriptThatNamesNoKindIsNotTreatedAsNamingOne() {
		// delay takes an INT and nothing else, so the walk must come back empty rather than inventing
		// a kind from a field that is not one.
		Wanted wanted = ResolveScripts.wanted("{\"root\": {\"node\": \"delay\", \"ticks\": 2}}");
		assertTrue(wanted.kinds().isEmpty());
	}

	@Test
	void aPlaceInTheTableResolves() {
		assertEquals(Verdict.RESOLVES, ResolveScripts.verdict(LocationKind.BANK, 3, 176));
	}

	@Test
	void anObjectKindWithNoPlaceButWorldObjectsResolvesByScanning() {
		// The rocks-and-fishing case locations.cfg documents: no row, and the bot still finds one.
		assertEquals(Verdict.SCAN_ONLY, ResolveScripts.verdict(LocationKind.ROCK, 0, 1183));
	}

	@Test
	void aKindWithNoPlaceTheWorldHasNoneOfIsNotFound() {
		// Cooking is the real instance: no object in this cache carries a Cook action, so the scan has
		// nothing to find and a script naming it could only work if a row existed.
		assertEquals(Verdict.NOT_FOUND, ResolveScripts.verdict(LocationKind.COOKING, 0, 0));
	}

	@Test
	void aTableRowResolvesEvenWhenTheWorldHasNoSuchObject() {
		// Rows win outright, even against a census of zero, because these two answers are about
		// different things and disagreeing with workshopValidate would be worse than saying nothing.
		// validate is the command that scans an authored box and fails on an empty one.
		assertEquals(Verdict.RESOLVES, ResolveScripts.verdict(LocationKind.COOKING, 4, 0));
	}

	@Test
	void aKindWithRowsDoesNotNeedTheWorldCounted() {
		// The same rule read the other way: a kind answered from the table gives the same verdict
		// whether or not the world was counted, which is what lets main skip the load.
		assertEquals(ResolveScripts.verdict(LocationKind.BANK, 3, 0),
				ResolveScripts.verdict(LocationKind.BANK, 3, 176));
	}

	@Test
	void aKindNothingCanClassifyNeedsATableRowAndCannotScan() {
		// A shop is not an object, so world objects of it are meaningless: the table is the only
		// oracle, and a scan fallback would make this pass when nothing can find a shop.
		assertEquals(Verdict.RESOLVES, ResolveScripts.verdict(LocationKind.SHOP, 1, 0));
		assertEquals(Verdict.NOT_FOUND, ResolveScripts.verdict(LocationKind.SHOP, 0, 500));
	}

	@Test
	void theCensusAnswersPerKindAndTotalsEverything() {
		long[] byKind = new long[LocationKind.values().length];
		byKind[LocationKind.TREE.ordinal()] = 412;
		byKind[LocationKind.BANK.ordinal()] = 176;
		WorldCensus census = WorldCensus.of(byKind, 588, 3);
		assertEquals(412, census.objectsOf(LocationKind.TREE));
		assertEquals(0, census.objectsOf(LocationKind.ROCK));
		assertEquals(588, census.objects());
		assertEquals(3, census.regions());
	}

	@Test
	void aCountedWorldIsCopiedRatherThanAliased() {
		// The array comes from the caller, so handing it back would let a later edit change a census
		// that has already been reported on.
		long[] byKind = new long[LocationKind.values().length];
		WorldCensus census = WorldCensus.of(byKind, 1, 1);
		byKind[LocationKind.TREE.ordinal()] = 999;
		assertEquals(0, census.objectsOf(LocationKind.TREE));
	}

	@Test
	void anUnknownKindIsRefusedRatherThanSilentlyDropped() {
		assertThrows(IllegalArgumentException.class, () -> ResolveScripts.wanted("[1, 2, 3]"));
	}

	@Test
	void aKindTheSchemaDoesNotKnowNeverEntersTheReport() {
		// Belt and braces on the walk: a document is only walked if the loader accepted it, but a
		// hand-built map should still not smuggle an unparsable kind past the check.
		Wanted wanted = ResolveScripts.wanted(
				"{\"root\": {\"node\": \"walk_to_nearest\", \"kind\": \"not_a_kind\", \"range\": 3}}");
		assertFalse(wanted.kinds().contains(LocationKind.TREE));
		assertTrue(wanted.kinds().isEmpty());
	}
}
