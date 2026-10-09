package botworkshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import botworkshop.data.LocDefs;
import botworkshop.data.LocDefinition;
import server.clip.region.ObjectDef;

/**
 * Pins the server's {@code ObjectDef} against this tool's decoder, entry by entry.
 *
 * <p>This exists because the two once disagreed about <em>everything</em>. {@code readValues} read
 * its strings with {@code readString()} — which scans for {@code 0x0A} — while {@code loc.dat}
 * terminates them with {@code 0x00}. Every named entry therefore failed to parse;
 * {@code getObjectDef} swallowed that failure with {@code setDefaults()} and answered "no name, no
 * actions, blocks walk" for all <b>19410</b> named objects. The client's {@code forID} uses a
 * {@code 0x00} reader and saw the real values, so the server and the client disagreed about the
 * name, the actions and the walkability of every named object — and walkability is collision, not
 * cosmetics.
 *
 * <p>Agreement is now exact, and this test keeps it that way. It lives in the workshop source set
 * on purpose: it needs the shipped {@code loc.dat}, and it is the same tool-versus-server contract
 * {@link ValidateMap} enforces, so a regression fails both.
 */
public class ObjectDefParityTest {

	private static LocDefs defs;

	@BeforeAll
	static void loadBothReaders() throws IOException {
		ObjectDef.loadConfig();
		defs = LocDefs.load(WorkshopFixture.locDat(), WorkshopFixture.locIdx());
	}

	/**
	 * Guards every loop below. {@code ObjectDef.loadConfig} resolves {@code
	 * ./Data/world/object/loc.dat} against the working directory and takes {@code null} bytes when
	 * the file is absent, and {@code getObjectDef} then answers {@code setDefaults()} for every id —
	 * which is indistinguishable from perfect agreement. Without this check the parity tests would
	 * pass vacuously in exactly the situation they exist to catch.
	 */
	@Test
	void theServerReaderLoadedRealData() {
		ObjectDef tree = ObjectDef.getObjectDef(1276);
		assertNotNull(tree);
		assertEquals("Tree", tree.name);
	}

	@Test
	void namesAgreeForEveryEntry() {
		int named = 0;
		List<String> wrong = new ArrayList<String>();
		for (int id = 0; id < defs.count(); id++) {
			LocDefinition ours = defs.get(id);
			if (ours == null || !ours.parsed() || ours.name() == null) {
				continue;
			}
			named++;
			String theirs = ObjectDef.getObjectDef(id).name;
			if (!ours.name().equals(theirs) && wrong.size() < 10) {
				wrong.add(id + ": expected " + ours.name() + ", the server read " + theirs);
			}
		}
		assertTrue(named > 19000, "expected the shipped cache's ~19410 named objects, saw " + named);
		assertTrue(wrong.isEmpty(), "the server's ObjectDef disagrees with loc.dat: " + wrong);
	}

	@Test
	void actionsAgreeForEveryEntry() {
		List<String> wrong = new ArrayList<String>();
		for (int id = 0; id < defs.count(); id++) {
			LocDefinition ours = defs.get(id);
			if (ours == null || !ours.parsed()) {
				continue;
			}
			List<String> theirs = serverActions(ObjectDef.getObjectDef(id));
			if (!ours.actions().equals(theirs) && wrong.size() < 10) {
				wrong.add(id + ": expected " + ours.actions() + ", the server read " + theirs);
			}
		}
		assertTrue(wrong.isEmpty(), "the server's ObjectDef disagrees with loc.dat: " + wrong);
	}

	@Test
	void footprintsAgreeForEveryEntry() {
		List<String> wrong = new ArrayList<String>();
		for (int id = 0; id < defs.count(); id++) {
			LocDefinition ours = defs.get(id);
			if (ours == null || !ours.parsed()) {
				continue;
			}
			ObjectDef theirs = ObjectDef.getObjectDef(id);
			if ((ours.sizeX() != theirs.anInt744 || ours.sizeY() != theirs.anInt761)
					&& wrong.size() < 10) {
				wrong.add(id + ": expected " + ours.sizeX() + "x" + ours.sizeY() + ", the server read "
						+ theirs.anInt744 + "x" + theirs.anInt761);
			}
		}
		assertTrue(wrong.isEmpty(), "the server's ObjectDef disagrees with loc.dat: " + wrong);
	}

	@Test
	void walkBlockingAgreesForEveryEntry() {
		List<String> wrong = new ArrayList<String>();
		for (int id = 0; id < defs.count(); id++) {
			LocDefinition ours = defs.get(id);
			if (ours == null || !ours.parsed()) {
				continue;
			}
			boolean theirs = ObjectDef.getObjectDef(id).aBoolean767();
			if (ours.blocksWalk() != theirs && wrong.size() < 10) {
				wrong.add(id + " (" + ours.name() + "): expected blocksWalk=" + ours.blocksWalk()
						+ ", the server says " + theirs);
			}
		}
		assertTrue(wrong.isEmpty(),
				"the server's ObjectDef disagrees with loc.dat about walk-blocking: " + wrong);
	}

	/** The server's action slots in order, with the null and empty holes removed. */
	private static List<String> serverActions(ObjectDef def) {
		if (def.actions == null) {
			return Collections.emptyList();
		}
		List<String> out = new ArrayList<String>();
		for (String action : def.actions) {
			if (action != null && !action.isEmpty()) {
				out.add(action);
			}
		}
		return out;
	}
}
