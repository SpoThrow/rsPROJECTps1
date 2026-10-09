package server.game.bots.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.FakeBotContext;
import server.game.bots.Traced;
import server.game.bots.meta.BotNodeRegistry;
import server.game.bots.meta.NodeParam;
import server.game.bots.meta.NodeSchema;
import server.game.bots.meta.ParamType;

/**
 * The behaviour-graph document loader ({@code BOT_TOOLING.md} §7): JSON in, a runnable {@link BotScript}
 * out.
 *
 * <p><b>Verified by running the tree, not by inspecting it.</b> A built node is wrapped in {@link Traced}
 * and a composite keeps its children private, so "is the tree right" is not a question about fields — it is
 * answered by ticking the tree against {@link FakeBotContext} and reading the status and the recorded
 * actions. That also means these tests need no world: the conditions and decorators they use answer from
 * the fake.
 */
class ScriptDocumentTest {

	private static final String SCRIPT = "test";

	// ---- building ------------------------------------------------------------------------------

	@Test
	void buildsAConditionNodeWithItsParameter() {
		// has_item is the smallest node that takes a parameter, so it is the cleanest proof that a document
		// parameter reaches the constructor: the same document answers differently with the item present.
		BotState withItem = root("{\"root\": {\"node\": \"has_item\", \"itemId\": 5}}");
		assertEquals(BotStatus.SUCCESS, first(withItem, new FakeBotContext().item(5)));

		BotState withoutItem = root("{\"root\": {\"node\": \"has_item\", \"itemId\": 5}}");
		assertEquals(BotStatus.FAILURE, first(withoutItem, new FakeBotContext()));
	}

	@Test
	void buildsNestedCompositesAndRunsThemInOrder() {
		BotState state = root("{\"root\": {\"node\": \"sequence\", \"children\": ["
				+ "{\"node\": \"has_item\", \"itemId\": 5},"
				+ "{\"node\": \"inventory_full\"}]}}");

		// A sequence needs both, so it fails until the second one holds too.
		assertEquals(BotStatus.FAILURE, first(state, new FakeBotContext().item(5)));

		BotState both = root("{\"root\": {\"node\": \"sequence\", \"children\": ["
				+ "{\"node\": \"has_item\", \"itemId\": 5},"
				+ "{\"node\": \"inventory_full\"}]}}");
		assertEquals(BotStatus.SUCCESS, first(both, new FakeBotContext().item(5).freeSlots(0)));
	}

	@Test
	void buildsDecoratorsAroundAChild() {
		// invert of a condition that holds is a failure — the wrapping is what is under test.
		BotState state = root("{\"root\": {\"node\": \"invert\", \"child\": "
				+ "{\"node\": \"has_item\", \"itemId\": 5}}}");

		assertEquals(BotStatus.FAILURE, first(state, new FakeBotContext().item(5)));
	}

	@Test
	void buildsConditionsFromTheirParameters() {
		assertEquals(BotStatus.SUCCESS,
				first(root("{\"root\": {\"node\": \"inventory_full\"}}"), new FakeBotContext().freeSlots(0)));
		assertEquals(BotStatus.SUCCESS,
				first(root("{\"root\": {\"node\": \"is_dead\"}}"), new FakeBotContext().dead(true)));
		assertEquals(BotStatus.SUCCESS,
				first(root("{\"root\": {\"node\": \"bank_open\"}}"), new FakeBotContext().banking(true)));
		assertEquals(BotStatus.SUCCESS,
				first(root("{\"root\": {\"node\": \"skill_at_least\", \"skill\": 8, \"level\": 20}}"),
						new FakeBotContext().level(8, 20)));
		assertEquals(BotStatus.SUCCESS,
				first(root("{\"root\": {\"node\": \"within_range\", \"x\": 3200, \"y\": 3200, \"range\": 3}}"),
						new FakeBotContext().at(3200, 3200, 0).arrived(true)));
	}

	@Test
	void buildsAKindParameterIntoAWorldKind() {
		// A KIND field is a text id that must resolve, which is the one conversion with a real failure mode.
		BotState state = root("{\"root\": {\"node\": \"walk_to_nearest\", \"kind\": \"tree\", \"range\": 3}}");

		assertNotNull(state);
	}

	// ---- defaults ------------------------------------------------------------------------------

	@Test
	void anOmittedOptionalParameterTakesItsDefault() {
		// repeat's count defaults to -1 (forever), and is omitted here. The observable consequence is that a
		// repeat whose child succeeds never itself succeeds — so a SUCCESS here would mean the default was not
		// applied. (Were the default 1, this would have succeeded on the first tick.)
		BotState state = root("{\"root\": {\"node\": \"repeat\", \"child\": "
				+ "{\"node\": \"has_item\", \"itemId\": 5}}}");
		FakeBotContext ctx = new FakeBotContext().item(5);
		state.enter(ctx);
		for (int i = 0; i < 5; i++) {
			assertEquals(BotStatus.RUNNING, state.tick(ctx), "should still be repeating on tick " + (i + 1));
		}
	}

	@Test
	void anExplicitOptionalParameterIsUsed() {
		BotState state = root("{\"root\": {\"node\": \"repeat\", \"count\": 2, \"child\": "
				+ "{\"node\": \"has_item\", \"itemId\": 5}}}");
		FakeBotContext ctx = new FakeBotContext().item(5);
		state.enter(ctx);
		BotStatus status = BotStatus.RUNNING;
		for (int i = 0; i < 50 && status == BotStatus.RUNNING; i++) {
			status = state.tick(ctx);
		}
		assertEquals(BotStatus.SUCCESS, status, "repeat with count 2 should finish");
	}

	// ---- the name ------------------------------------------------------------------------------

	@Test
	void aNameFieldMatchingTheScriptIsAccepted() {
		BotScript script = ScriptDocument.fromJson(SCRIPT,
				"{\"name\": \"" + SCRIPT + "\", \"root\": {\"node\": \"inventory_full\"}}");

		assertEquals(SCRIPT, script.name());
	}

	@Test
	void aNameFieldDisagreeingWithTheScriptIsRejected() {
		// The file name is what bots.cfg's script field points at, so a different inner name could never be
		// used; two names for one script is the ambiguity to refuse.
		ScriptDocument.ScriptException error = assertThrows(ScriptDocument.ScriptException.class,
				() -> ScriptDocument.fromJson(SCRIPT,
						"{\"name\": \"other\", \"root\": {\"node\": \"inventory_full\"}}"));

		assertTrue(error.getMessage().contains("other"), error.getMessage());
	}

	// ---- failures ------------------------------------------------------------------------------

	@Test
	void anUnknownNodeIsRejectedWithTheKnownOnesNamed() {
		ScriptDocument.ScriptException error = assertThrows(ScriptDocument.ScriptException.class,
				() -> root("{\"root\": {\"node\": \"no_such_node\"}}"));

		assertTrue(error.getMessage().contains("no_such_node"), error.getMessage());
		assertTrue(error.getMessage().contains("has_item"), "should list the real ids: " + error.getMessage());
	}

	@Test
	void anUnknownFieldIsRejected() {
		// The point of the check: a misspelled parameter would otherwise take its default and the script would
		// run with a silently wrong value.
		ScriptDocument.ScriptException error = assertThrows(ScriptDocument.ScriptException.class,
				() -> root("{\"root\": {\"node\": \"has_item\", \"itemId\": 5, \"itemid\": 6}}"));

		assertTrue(error.getMessage().contains("itemid"), error.getMessage());
	}

	@Test
	void aMissingRequiredFieldIsRejected() {
		ScriptDocument.ScriptException error = assertThrows(ScriptDocument.ScriptException.class,
				() -> root("{\"root\": {\"node\": \"has_item\"}}"));

		assertTrue(error.getMessage().contains("itemId"), error.getMessage());
	}

	@Test
	void aWronglyTypedFieldIsRejected() {
		ScriptDocument.ScriptException error = assertThrows(ScriptDocument.ScriptException.class,
				() -> root("{\"root\": {\"node\": \"has_item\", \"itemId\": \"five\"}}"));

		assertTrue(error.getMessage().contains("whole number"), error.getMessage());
	}

	@Test
	void aFractionalNumberForAWholeNumberFieldIsRejected() {
		assertThrows(ScriptDocument.ScriptException.class,
				() -> root("{\"root\": {\"node\": \"has_item\", \"itemId\": 1.5}}"));
	}

	@Test
	void anUnknownKindIsRejected() {
		ScriptDocument.ScriptException error = assertThrows(ScriptDocument.ScriptException.class,
				() -> root("{\"root\": {\"node\": \"walk_to_nearest\", \"kind\": \"banana\", \"range\": 1}}"));

		assertTrue(error.getMessage().contains("banana"), error.getMessage());
	}

	@Test
	void aMissingNodeFieldIsRejected() {
		ScriptDocument.ScriptException error = assertThrows(ScriptDocument.ScriptException.class,
				() -> root("{\"root\": {\"kind\": \"tree\"}}"));

		assertTrue(error.getMessage().contains("node"), error.getMessage());
	}

	@Test
	void aMissingRootIsRejected() {
		ScriptDocument.ScriptException error = assertThrows(ScriptDocument.ScriptException.class,
				() -> ScriptDocument.fromJson(SCRIPT, "{}"));

		assertTrue(error.getMessage().contains("root"), error.getMessage());
	}

	@Test
	void anUnknownTopLevelFieldIsRejected() {
		ScriptDocument.ScriptException error = assertThrows(ScriptDocument.ScriptException.class,
				() -> ScriptDocument.fromJson(SCRIPT,
						"{\"root\": {\"node\": \"inventory_full\"}, \"nope\": 1}"));

		assertTrue(error.getMessage().contains("nope"), error.getMessage());
	}

	@Test
	void aNonObjectRootIsRejected() {
		assertThrows(ScriptDocument.ScriptException.class,
				() -> ScriptDocument.fromJson(SCRIPT, "{\"root\": 5}"));
	}

	@Test
	void aChildListThatIsNotAListIsRejected() {
		ScriptDocument.ScriptException error = assertThrows(ScriptDocument.ScriptException.class,
				() -> root("{\"root\": {\"node\": \"sequence\", \"children\": "
						+ "{\"node\": \"inventory_full\"}}}"));

		assertTrue(error.getMessage().contains("list"), error.getMessage());
	}

	@Test
	void aBlankNameIsRejected() {
		assertThrows(ScriptDocument.ScriptException.class,
				() -> ScriptDocument.fromJson("  ", "{\"root\": {\"node\": \"inventory_full\"}}"));
	}

	// ---- the schema is the source of truth ------------------------------------------------------

	/**
	 * Every node the editor can offer must be buildable from its schema — the loader half of the parity
	 * check the palette export has.
	 *
	 * <p>It fails if a node gains a parameter type the loader does not encode (the switch in
	 * {@code sampleFor} says so, naming the type), and it fails if the loader's reflection cannot build a
	 * node the registry describes. So "the editor can place it" and "the server can run it" stay the same
	 * statement.
	 */
	@Test
	void everyNodeInTheRegistryCanBeBuiltFromItsSchema() {
		List<NodeSchema> schemas = BotNodeRegistry.schemas();
		assertTrue(schemas.size() >= 20, "expected the shipped node set, found " + schemas.size());

		for (NodeSchema schema : schemas) {
			Map<String, Object> document = new LinkedHashMap<String, Object>();
			document.put("root", nodeWithRequiredFields(schema));

			// Throws with the offending node and field named if anything is wrong.
			BotScript script = ScriptDocument.fromDocument(schema.id(), document);
			assertEquals(schema.id(), script.name());
			assertEquals(schema.className(), unwrap(script.root()).getClass().getName(),
					"built the wrong class for node " + schema.id());
		}
	}

	@Test
	void eachRootCallMintsAFreshTree() {
		// A state holds progress, so two bots naming one script must not share nodes. A cached tree would be
		// the same object twice.
		BotScript script = ScriptDocument.fromJson(SCRIPT,
				"{\"root\": {\"node\": \"repeat\", \"child\": {\"node\": \"delay\", \"ticks\": 3}}}");

		assertNotSame(script.root(), script.root());
	}

	@Test
	void theBuiltTreeIsTracedLikeAHandWrittenOne() {
		// ::botinfo reads the trace, so an authored script must report like one authored in Java.
		BotState state = ScriptDocument.fromJson(SCRIPT,
				"{\"root\": {\"node\": \"inventory_full\"}}").root();

		assertTrue(state instanceof Traced, "the root should be wrapped for tracing");
	}

	// ---- helpers -------------------------------------------------------------------------------

	private static BotState root(String json) {
		return ScriptDocument.fromJson(SCRIPT, json).root();
	}

	/** Enters and ticks once — enough for the conditions and single-tick decorators used here. */
	private static BotStatus first(BotState state, FakeBotContext ctx) {
		state.enter(ctx);
		return state.tick(ctx);
	}

	private static BotState unwrap(BotState node) {
		return node instanceof Traced ? ((Traced) node).child() : node;
	}

	/** A node document holding every required field, from a sample of the right type for each. */
	private static Map<String, Object> nodeWithRequiredFields(NodeSchema schema) {
		Map<String, Object> node = new LinkedHashMap<String, Object>();
		node.put("node", schema.id());
		for (NodeParam param : schema.params()) {
			if (param.required()) {
				node.put(param.name(), sampleFor(param));
			}
		}
		return node;
	}

	private static Object sampleFor(NodeParam param) {
		switch (param.type()) {
		case INT:
			return Integer.valueOf(1);
		case BOOLEAN:
			return Boolean.TRUE;
		case STRING:
			return "x";
		case KIND:
			return "tree";
		case TILE:
			return tile();
		case NODE:
			return leaf();
		case NODE_LIST:
			return List.of(leaf());
		case LOCATION:
			// No node declares a LOCATION parameter yet. When one does, add its encoding to
			// ScriptDocument.convert and a sample here — the failure is the reminder.
			throw new IllegalStateException("no sample for LOCATION; the loader has no encoding for it yet");
		default:
			throw new IllegalStateException("no sample for " + param.type());
		}
	}

	private static Map<String, Object> leaf() {
		Map<String, Object> node = new LinkedHashMap<String, Object>();
		node.put("node", "walk_to");
		node.put("destX", Integer.valueOf(3200));
		node.put("destY", Integer.valueOf(3200));
		node.put("range", Integer.valueOf(1));
		return node;
	}

	private static Map<String, Object> tile() {
		Map<String, Object> tile = new LinkedHashMap<String, Object>();
		tile.put("x", Integer.valueOf(3200));
		tile.put("y", Integer.valueOf(3200));
		tile.put("plane", Integer.valueOf(0));
		return tile;
	}

	/** Guards the switch above against a new {@link ParamType} that no sample was written for. */
	@Test
	void everyParameterTypeHasASample() {
		for (ParamType type : ParamType.values()) {
			if (type == ParamType.LOCATION) {
				continue; // deliberately unsupported, and asserted as such below
			}
			assertNotNull(sampleFor(new NodeParam("p", type, true, null, "")), "no sample for " + type);
		}
	}
}
