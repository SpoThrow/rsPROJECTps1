package botworkshop.classify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import botworkshop.WorkshopFixture;
import botworkshop.data.LocDefinition;
import botworkshop.data.LocDefs;
import server.clip.region.ObjectDef;
import server.game.bots.world.ResourceKinds;

/**
 * The drift guard between the two classifiers — {@code BOT_TOOLING.md} §5's "anti-drift" requirement,
 * made concrete.
 *
 * <p>{@link ResourceRules} and {@link ResourceKinds} are deliberately one table now: the tool's copy
 * delegates. But delegation can be undone by a well-meaning edit, and the failure it would cause is
 * quiet — the map draws a tree icon on something the runtime cannot find, and an author builds a bot
 * around an object that is not a resource at all.
 *
 * <p>So this compares the <em>decoded</em> answers over the whole archive rather than trusting the
 * delegation: the tool decodes {@code loc.dat} its own way ({@link LocDefs}) and the runtime decodes
 * it through {@code ObjectDef}, and the two must agree on every single id. Those are genuinely
 * different code paths — {@code ObjectDef.readValues} terminates strings on {@code 0x0A} where the
 * tool's decoder terminates them on {@code 0x00}, which is why the tool has its own decoder at all —
 * so agreement is evidence, not a tautology.
 */
class ResourceKindsParityTest {

	private static LocDefs defs;

	@BeforeAll
	static void load() throws IOException {
		defs = LocDefs.load(WorkshopFixture.locDat(), WorkshopFixture.locIdx());
		// The runtime's decoder, loaded from the same archive through the server's own entry point.
		ObjectDef.loadConfig();
	}

	@Test
	void theRuntimeAndTheToolAgreeOnEveryObjectIdInTheArchive() {
		int compared = 0;
		int classified = 0;
		for (int id = 0; id < defs.count(); id++) {
			LocDefinition definition = defs.get(id);
			if (definition == null) {
				continue;
			}
			// Only definitions the tool actually decoded: ObjectDef falls back to loc377.dat for an
			// id loc.dat does not hold, and the tool reads loc.dat alone, so the two are asked about
			// a different record there. Comparing those would be comparing a bug in the test.
			if (definition.name() == null && definition.actions().isEmpty()) {
				continue;
			}
			compared++;

			String fromTool = ResourceRules.classify(definition);
			String fromRuntime = ResourceKinds.classify(ObjectDef.getObjectDef(id));

			assertEquals(fromTool, fromRuntime,
					"id " + id + " (" + definition.name() + ") classifies differently in the two decoders");
			if (fromTool != null) {
				classified++;
			}
		}

		// Liveness: a loop that compared nothing would pass vacuously. The archive has ~42001
		// entries and ~19410 of them are named, so a real run is in the thousands at minimum.
		assertTrue(compared > 19000, "expected to compare the named archive, compared " + compared);
		assertTrue(classified > 500, "the table must classify a real share of the world, got " + classified);
	}

	@Test
	void theRuntimeClassifierSeesTheSameIconsTheMapDraws() {
		// A second, tighter check on the handful of ids the icon layer's tests call out, so a failure
		// names the object rather than only an id.
		assertEquals("tree", runtimeKind(1276), "id 1276 is the oak the map draws a tree for");
		assertEquals("rock", runtimeKind(2091), "id 2091 is the rocks");
		assertEquals("bank", runtimeKind(2213), "id 2213 is the bank booth with no Bank action");
		assertEquals(null, runtimeKind(1530), "id 1530 is a door, and a door is not a resource");
	}

	private static String runtimeKind(int id) {
		return ResourceKinds.classify(ObjectDef.getObjectDef(id));
	}
}
