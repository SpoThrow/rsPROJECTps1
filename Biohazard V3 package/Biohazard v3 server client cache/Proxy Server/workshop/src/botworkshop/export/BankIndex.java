package botworkshop.export;

import java.util.ArrayList;
import java.util.List;

/**
 * Every bank in the world, as one small file the editor can answer "what is the nearest bank?" from
 * — {@code BOT_WORKSHOP_UX.md} §2 calls that the single most useful thing the tool can show, because
 * every gathering bot needs a bank leg and an author otherwise guesses.
 *
 * <p><b>Why this is separate from the per-region documents.</b> {@code workshopExport} writes only
 * the regions that were asked for, so answering the question from those documents would be wrong the
 * moment the author looks at a region next to one that was not exported: a bank one tile inside the
 * neighbouring region would simply not be found, and "no bank nearby" is the worst possible lie for
 * this tool to tell. The export therefore scans <em>every</em> region the server loaded — which is
 * the whole world regardless of what was exported — and writes the banks once. The file is a few
 * hundred lines, so shipping all of them costs nothing.
 *
 * <p>Sorted by plane then y then x so the file is stable: adding a bank to the world shows up as one
 * added line rather than a reshuffled diff.
 */
public final class BankIndex {

	/** One bank object. A booth can occupy several objects; they are listed individually. */
	public static final class Bank {
		public final int id;
		public final int x;
		public final int y;
		public final int plane;
		/** The definition's name, e.g. {@code "Bank booth"}. Null if the cache failed to parse it. */
		public final String name;

		public Bank(int id, int x, int y, int plane, String name) {
			this.id = id;
			this.x = x;
			this.y = y;
			this.plane = plane;
			this.name = name;
		}

		@Override
		public String toString() {
			return (name == null ? "?" : name) + " (" + id + ") at " + x + "," + y + " plane " + plane;
		}
	}

	private BankIndex() {
	}

	public static String toJson(List<Bank> banks) {
		List<Bank> sorted = new ArrayList<Bank>(banks);
		sorted.sort((a, b) -> {
			if (a.plane != b.plane) {
				return Integer.compare(a.plane, b.plane);
			}
			if (a.y != b.y) {
				return Integer.compare(a.y, b.y);
			}
			if (a.x != b.x) {
				return Integer.compare(a.x, b.x);
			}
			return Integer.compare(a.id, b.id);
		});

		Json json = new Json();
		json.openObject();
		json.field("note", "Every object the classifier calls a bank, across all "
				+ sorted.size() + " placements. The editor reads this to answer 'nearest bank'; it "
				+ "covers the whole world rather than only the exported regions, because a bank one "
				+ "tile inside a neighbouring region must not go missing.");
		json.field("count", sorted.size());
		json.name("banks").openArray();
		for (Bank bank : sorted) {
			json.openObject();
			json.field("id", bank.id);
			json.field("x", bank.x);
			json.field("y", bank.y);
			json.field("plane", bank.plane);
			json.field("name", bank.name);
			json.closeObject();
		}
		json.closeArray();
		json.closeObject();
		return json.toString();
	}
}
