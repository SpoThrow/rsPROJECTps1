package botworkshop.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import server.game.bots.meta.NodeParam;
import server.game.bots.meta.NodeSchema;
import server.game.bots.meta.ParamType;

/**
 * The shape of {@code bot-nodes.json} itself, pinned with synthetic schemas so the writer's
 * contracts are checked without depending on which nodes happen to ship.
 */
class BotNodesTest {

	@Test
	void aRequiredParamWritesADefaultOfNullRatherThanOmittingTheField() {
		String json = BotNodes.toJson(Collections.singletonList(node(List.of(
				new NodeParam("treeId", ParamType.INT, true, null, "which tree")))));
		assertTrue(json.contains("\"name\": \"treeId\""), json);
		assertTrue(json.contains("\"type\": \"INT\""), json);
		assertTrue(json.contains("\"required\": true"), json);
		assertTrue(json.contains("\"default\": null"), json);
	}

	@Test
	void anOptionalParamWritesTheDefaultTheConstructorSupplies() {
		String json = BotNodes.toJson(Collections.singletonList(node(List.of(
				new NodeParam("count", ParamType.INT, false, "-1", "negative means forever")))));
		assertTrue(json.contains("\"required\": false"), json);
		assertTrue(json.contains("\"default\": \"-1\""), json);
	}

	@Test
	void aNodeWithNoParametersRendersAnEmptyArrayRatherThanOmission() {
		String json = BotNodes.toJson(Collections.singletonList(node(Collections.emptyList())));
		assertTrue(json.contains("\"params\": []"), json);
	}

	@Test
	void aKindParamCarriesTheKindsTheServerKnows() {
		// The editor builds a dropdown from this rather than a text box, so the values have to ship. They are
		// enumerated from LocationKind — the same vocabulary ScriptDocument resolves against.
		String json = BotNodes.toJson(Collections.singletonList(node(List.of(
				new NodeParam("kind", ParamType.KIND, true, null, "what to look for")))));

		assertTrue(json.contains("\"values\""), json);
		assertTrue(json.contains("\"tree\""), json);
		assertTrue(json.contains("\"bank\""), json);
	}

	@Test
	void aNonKindParamCarriesNoValues() {
		String json = BotNodes.toJson(Collections.singletonList(node(List.of(
				new NodeParam("count", ParamType.INT, true, null, "how many")))));

		assertFalse(json.contains("\"values\""), json);
	}

	@Test
	void nodesRenderInTheOrderTheRegistryGaveThem() {
		NodeSchema first = new NodeSchema("alpha", "x.Alpha", "state", "first",
				Collections.emptyList());
		NodeSchema second = new NodeSchema("beta", "x.Beta", "state", "second",
				Collections.emptyList());
		String json = BotNodes.toJson(Arrays.asList(first, second));
		assertTrue(json.indexOf("\"id\": \"alpha\"") < json.indexOf("\"id\": \"beta\""), json);
	}

	@Test
	void theDocumentDeclaresItsFormatVersion() {
		assertEquals("1", Integer.toString(BotNodes.FORMAT_VERSION));
	}

	private static NodeSchema node(List<NodeParam> params) {
		return new NodeSchema("sample", "server.game.bots.states.Sample", "state", "a sample", params);
	}
}
