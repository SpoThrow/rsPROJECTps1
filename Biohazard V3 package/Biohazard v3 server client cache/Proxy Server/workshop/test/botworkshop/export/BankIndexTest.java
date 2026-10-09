package botworkshop.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Pins the bank index the editor's "nearest bank" line reads.
 *
 * <p>The interesting properties are not the JSON's shape so much as its stability and its honesty:
 * the file is committed, so it must not reshuffle when the scan order changes, and it must not claim
 * a bank count it does not contain.
 */
class BankIndexTest {

	@Test
	void anEmptyWorldIsAnEmptyArrayRatherThanNull() {
		String json = BankIndex.toJson(new ArrayList<BankIndex.Bank>());

		assertTrue(json.contains("\"count\": 0"), json);
		assertTrue(json.contains("\"banks\": []"), "an empty list must be [] not null: " + json);
		assertFalse(json.contains("null,"), "an empty index must not contain null entries");
	}

	@Test
	void banksAreSortedByPlaneThenYThenXSoTheFileIsStable() {
		// Given in deliberately scrambled order. B and E share a y so the x key is exercised, and
		// C shares x/y with A so the plane key is.
		List<BankIndex.Bank> banks = new ArrayList<BankIndex.Bank>();
		banks.add(new BankIndex.Bank(2213, 3092, 3243, 0, "Bank booth")); // A
		banks.add(new BankIndex.Bank(2213, 3100, 3200, 0, "Bank booth")); // B
		banks.add(new BankIndex.Bank(2213, 3092, 3243, 1, "Bank booth")); // C
		banks.add(new BankIndex.Bank(2213, 2655, 3283, 0, "Bank booth")); // D
		banks.add(new BankIndex.Bank(2213, 3200, 3200, 0, "Bank booth")); // E

		String json = BankIndex.toJson(banks);

		int a = orderOf(json, 3092, 3243, 0);
		int b = orderOf(json, 3100, 3200, 0);
		int c = orderOf(json, 3092, 3243, 1);
		int d = orderOf(json, 2655, 3283, 0);
		int e = orderOf(json, 3200, 3200, 0);

		assertTrue(b < e, "same y should fall through to x: " + json);
		assertTrue(e < a, "lower y should come first: " + json);
		assertTrue(a < d, "y 3243 before 3283: " + json);
		assertTrue(a < c, "plane is the outermost key: " + json);
	}

	@Test
	void theScanOrderDoesNotChangeTheFile() {
		// The whole world is walked in map_index order, which is not something a reader should have
		// to know. Feeding the same banks backwards must produce the same bytes, or the file would
		// churn in git whenever the directory's order changed.
		List<BankIndex.Bank> banks = new ArrayList<BankIndex.Bank>();
		for (int i = 0; i < 20; i++) {
			banks.add(new BankIndex.Bank(2213, 3000 + i, 3200 + (i % 3), i % 4, "Bank booth"));
		}
		List<BankIndex.Bank> reversed = new ArrayList<BankIndex.Bank>(banks);
		Collections.reverse(reversed);

		assertEquals(BankIndex.toJson(banks), BankIndex.toJson(reversed));
	}

	@Test
	void theDeclaredCountMatchesTheNumberOfEntries() {
		List<BankIndex.Bank> banks = new ArrayList<BankIndex.Bank>();
		for (int i = 0; i < 7; i++) {
			banks.add(new BankIndex.Bank(2213, 3000 + i, 3200, 0, "Bank booth"));
		}

		String json = BankIndex.toJson(banks);

		assertTrue(json.contains("\"count\": 7"), json);
		assertEquals(7, json.split("\"plane\":", -1).length - 1,
				"the count and the entries disagree: " + json);
	}

	@Test
	void anUnparsedDefinitionIsWrittenAsNullRatherThanDropped() {
		// A bank whose loc.dat entry failed to parse still occupies the tile. Dropping it would make
		// the tool claim there is no bank there; a null name is visible and traceable.
		List<BankIndex.Bank> banks = new ArrayList<BankIndex.Bank>();
		banks.add(new BankIndex.Bank(2213, 3092, 3243, 0, null));

		String json = BankIndex.toJson(banks);

		assertTrue(json.contains("\"name\": null"), json);
		assertTrue(json.contains("\"count\": 1"), json);
	}

	/** Where a bank's coordinates appear in the document, for asserting relative order. */
	private static int orderOf(String json, int x, int y, int plane) {
		Pattern entry = Pattern.compile(
				"\\{\\s*\"id\": \\d+,\\s*\"x\": " + x + ",\\s*\"y\": " + y + ",\\s*\"plane\": " + plane);
		Matcher matcher = entry.matcher(json);
		assertTrue(matcher.find(), "no bank at " + x + "," + y + " plane " + plane + " in " + json);
		return matcher.start();
	}
}
