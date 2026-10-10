package server.content;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import server.Config;
import server.content.skills.Fletching;
import server.content.skills.Spinning;
import server.game.items.ItemDefinitions;

/**
 * The Phase 0 content validator: reports every item id we reference that has no definition yet,
 * and fails on the conflicts that are real bugs rather than missing content.
 *
 * <p><b>The distinction is the whole safety model.</b> {@code QOL_PLAN.md} §2 writes ids we do not
 * have into content tables on purpose, so a feature for a higher-revision item is inert until the
 * item is imported and then switches itself on with no code change. An undefined id is therefore
 * <em>the expected state</em>, not an error, and this reports it as a warning and stays green.
 * What it fails on is the opposite kind of problem: two entries claiming one id, or an id outside
 * the item range — the cases where content is silently unreachable or a lookup goes out of bounds.
 *
 * <p>Run it on its own with
 * {@code gradlew test --tests server.content.QolValidatorTest}. Warnings are printed rather than
 * asserted on, because their number is supposed to fall every time an import lands; asserting a
 * count would make an import fail the build for succeeding.
 *
 * <p>After an import, the check is: run this, expect the warnings for the imported ids to be gone
 * and no new failures.
 */
class QolValidatorTest {

	/**
	 * A content table: a name for the report, the ids it is looked up by, and every item id it
	 * references.
	 *
	 * <p>The two are separate because they fail differently. A repeated <em>reference</em> is
	 * ordinary — every bolt row names feather {@code 314}, that is a shared material. A repeated
	 * <em>key</em> is a bug: the lookup returns the first match, so the second row is unreachable
	 * and its product silently stops existing.
	 *
	 * <p>Keys are {@code long} because some tables are keyed on a pair. {@code craftingVariables}
	 * is looked up by (unfinished bolt, bolt tips), and its first column repeats — three rows share
	 * bolt {@code 9142} — so a single-column key would report a conflict that is not there.
	 */
	private static final class Table {
		final String name;
		final long[] keys;
		final int[] ids;

		Table(String name, long[] keys, int[] ids) {
			this.name = name;
			this.keys = keys;
			this.ids = ids;
		}
	}

	private static long key(int a, int b) {
		return ((long) a << 32) | (b & 0xFFFFFFFFL);
	}

	/**
	 * Every authored table that names item ids.
	 *
	 * <p>The registries ({@code ItemUseRegistry}, {@code ItemOnObjectRegistry}) are not here yet:
	 * they cannot be enumerated without an accessor, and a duplicate registration throws at
	 * class-load rather than being a row this could find. Bow stringing is the first item recipe
	 * to live in one, so its ids are listed here directly from {@code Fletching.Stringing} and its
	 * registration is covered by {@code ItemUseRegistryTest} and {@code FletchingTest}, which
	 * enumerate the registry through {@code isRegistered}.
	 */
	private static List<Table> tables() {
		List<Table> tables = new ArrayList<>();

		List<Integer> bolts = new ArrayList<>();
		List<Long> boltKeys = new ArrayList<>();
		for (Fletching.Bolts b : Fletching.Bolts.values()) {
			boltKeys.add((long) b.getItem1());
			bolts.add(b.getItem1());
			bolts.add(b.getItem2());
			bolts.add(b.getOutcome());
		}
		tables.add(new Table("Fletching.Bolts", toKeys(boltKeys), toArray(bolts)));

		List<Integer> arrows = new ArrayList<>();
		List<Long> arrowKeys = new ArrayList<>();
		for (Fletching.Arrows a : Fletching.Arrows.values()) {
			// forArrow matches getItem2(), which is the arrowhead or the feather.
			arrowKeys.add((long) a.getItem2());
			arrows.add(a.getItem1());
			arrows.add(a.getItem2());
			arrows.add(a.getOutcome());
		}
		tables.add(new Table("Fletching.Arrows", toKeys(arrowKeys), toArray(arrows)));

		List<Integer> bows = new ArrayList<>();
		List<Long> bowKeys = new ArrayList<>();
		for (Fletching.Fletch f : Fletching.Fletch.values()) {
			// forBow matches the product, not the log.
			bowKeys.add((long) f.getBowID());
			bows.add(f.getLogID());
			bows.add(f.getBowID());
		}
		tables.add(new Table("Fletching.Fletch", toKeys(bowKeys), toArray(bows)));

		List<Integer> stringing = new ArrayList<>();
		List<Long> stringingKeys = new ArrayList<>();
		for (Fletching.Stringing s : Fletching.Stringing.values()) {
			// forStringing is looked up by the pair (unstrung, bow string), so that is the key.
			// Every row shares 1777, which is a repeated *reference* and not a conflict.
			stringingKeys.add(key(s.getUnstrung(), Fletching.BOW_STRING));
			stringing.add(s.getUnstrung());
			stringing.add(s.getStrung());
			stringing.add(Fletching.BOW_STRING);
		}
		tables.add(new Table("Fletching.Stringing", toKeys(stringingKeys), toArray(stringing)));

		List<Integer> spinning = new ArrayList<>();
		List<Long> spinningKeys = new ArrayList<>();
		for (Spinning.Material m : Spinning.Material.values()) {
			// forId matches the material, so that is the key.
			spinningKeys.add((long) m.getMaterial());
			spinning.add(m.getMaterial());
			spinning.add(m.getProduct());
		}
		tables.add(new Table("Spinning.Material", toKeys(spinningKeys), toArray(spinning)));

		List<Integer> tips = new ArrayList<>();
		List<Long> tipKeys = new ArrayList<>();
		for (int[] row : Fletching.boltTips) {
			tipKeys.add((long) row[0]);
			tips.add(row[0]);
			tips.add(row[1]);
		}
		tables.add(new Table("Fletching.boltTips", toKeys(tipKeys), toArray(tips)));

		List<Integer> tipping = new ArrayList<>();
		List<Long> tippingKeys = new ArrayList<>();
		for (int[] row : Fletching.craftingVariables) {
			// Looked up by the pair: three rows share unfinished bolt 9142.
			tippingKeys.add(key(row[0], row[1]));
			tipping.add(row[0]);
			tipping.add(row[1]);
			tipping.add(row[2]);
		}
		tables.add(new Table("Fletching.craftingVariables", toKeys(tippingKeys), toArray(tipping)));

		List<Integer> interfaceRows = new ArrayList<>();
		for (int[] row : Fletching.ifItems) {
			for (int id : row) {
				interfaceRows.add(id);
			}
		}
		// ifItems is a display table, not a lookup: it is read by position, so it has no key.
		tables.add(new Table("Fletching.ifItems", new long[0], toArray(interfaceRows)));

		return tables;
	}

	private static int[] toArray(List<Integer> ids) {
		int[] out = new int[ids.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = ids.get(i);
		}
		return out;
	}

	private static long[] toKeys(List<Long> keys) {
		long[] out = new long[keys.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = keys.get(i);
		}
		return out;
	}

	@Test
	void reportsEveryReferencedIdThatHasNoDefinitionYet() {
		List<String> warnings = new ArrayList<>();

		for (Table table : tables()) {
			Set<Integer> seen = new LinkedHashSet<>();
			for (int id : table.ids) {
				seen.add(id);
			}
			for (int id : seen) {
				if (!ItemDefinitions.exists(id)) {
					warnings.add("  " + table.name + " references item " + id
							+ ", which has no definition yet — inert until it is imported");
				}
			}
		}

		StringBuilder report = new StringBuilder();
		report.append("\n=== QOL content validator: undefined item ids ===\n");
		if (warnings.isEmpty()) {
			report.append("  none: every referenced id resolves\n");
		} else {
			report.append("  ").append(warnings.size())
					.append(" id(s) referenced but not defined (warnings, not failures):\n");
			for (String warning : warnings) {
				report.append(warning).append('\n');
			}
		}
		System.out.println(report);

		// Deliberately not asserted on. An undefined id is the designed-for state.
		assertTrue(true);
	}

	@Test
	void noTableIsLookedUpByTheSameKeyTwice() {
		// A repeated key is a real bug rather than missing content: the lookup returns the first
		// match, so the second row is unreachable and its product silently stops existing.
		//
		// Repeated *references* are not checked, and should not be: every bolt row names feather
		// 314, which is a shared material, not a conflict.
		for (Table table : tables()) {
			Set<Long> seen = new LinkedHashSet<>();
			for (long k : table.keys) {
				assertTrue(seen.add(k),
						table.name + " is looked up by key " + k + " twice, so one row is unreachable");
			}
		}
	}

	@Test
	void everyReferencedIdIsInsideTheItemRange() {
		// An id at or past ITEM_LIMIT is not "not imported yet" — no import can ever define it,
		// because the array cannot hold it. That is a typo, and it should read as one.
		for (Table table : tables()) {
			for (int id : table.ids) {
				assertTrue(id >= 0, table.name + " references a negative id " + id);
				assertTrue(id < Config.ITEM_LIMIT, table.name + " references id " + id
						+ ", which is past ITEM_LIMIT (" + Config.ITEM_LIMIT
						+ ") and can never be defined");
			}
		}
	}
}
