package botworkshop.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import botworkshop.export.BotNodes;
import server.game.bots.BotState;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.BotNodeRegistry;
import server.game.bots.meta.NodeParam;
import server.game.bots.meta.NodeSchema;

/**
 * The drift guard for the node palette — {@code BOT_TOOLING.md} stage T4.
 *
 * <p><b>Both directions, because either alone is a hole.</b> Checking only "every compiled
 * {@code BotState} is annotated" would still let a node be annotated and then left out of the
 * registry, so the editor never offers it. Checking only "every registry entry is annotated" would
 * let a new state be added, tested and shipped with no annotation at all, so it exists but cannot be
 * authored. The two together mean the set of states and the set of nodes are the same set.
 *
 * <p>The second half compares that set against the exported document itself — the artifact the
 * editor will actually read — rather than against an in-memory list, so a bug in the writer shows up
 * here and not in the browser.
 */
class BotNodeParityTest {

	/** Matches the node-level {@code "id"} only; parameter objects have no {@code id} field. */
	private static final Pattern EXPORTED_ID = Pattern.compile("\"id\": \"([^\"]+)\"");

	private static final Pattern EXPORTED_CLASS = Pattern.compile("\"className\": \"([^\"]+)\"");

	private static final Pattern EXPORTED_PARAM_NAME = Pattern.compile("\"name\": \"([^\"]+)\"");

	@Test
	void everyCompiledBotStateIsAnnotatedSoItCanBeAuthored() {
		List<Class<?>> unannotated = NodeCoverage.unannotated();
		assertTrue(unannotated.isEmpty(), () -> "not @BotNode: " + names(unannotated));
	}

	@Test
	void theCompiledTreeAndTheRegistryAreTheSameSetOfClasses() {
		Set<String> compiled = new LinkedHashSet<String>(names(NodeCoverage.botStateImplementations()));
		Set<String> registered = new LinkedHashSet<String>();
		for (Class<? extends BotState> type : BotNodeRegistry.knownNodeClasses()) {
			registered.add(type.getName());
		}
		assertEquals(compiled, registered,
				"a BotState and the registry disagree; add the missing class to BotNodeRegistry");
	}

	@Test
	void theExportedIdsAreExactlyTheRegistryIds() {
		Set<String> exported = new LinkedHashSet<String>();
		Matcher matcher = EXPORTED_ID.matcher(BotNodes.export());
		while (matcher.find()) {
			exported.add(matcher.group(1));
		}
		Set<String> registered = new LinkedHashSet<String>();
		for (NodeSchema schema : BotNodeRegistry.schemas()) {
			registered.add(schema.id());
		}
		assertEquals(registered, exported, "the export and the registry disagree on the node ids");
	}

	@Test
	void everyExportedClassNameIsARealAnnotatedBotState() throws Exception {
		Matcher matcher = EXPORTED_CLASS.matcher(BotNodes.export());
		int seen = 0;
		while (matcher.find()) {
			seen++;
			Class<?> type = Class.forName(matcher.group(1), false, BotState.class.getClassLoader());
			assertTrue(BotState.class.isAssignableFrom(type), type + " is not a BotState");
			assertTrue(type.isAnnotationPresent(BotNode.class), type + " is not annotated");
			assertTrue(BotNodes.isKnownNodeClass(type.getName()), type + " is not a known node");
		}
		assertEquals(NodeCoverage.botStateImplementations().size(), seen,
				"the export and the compiled tree disagree on how many nodes there are");
	}

	@Test
	void everyExportedParameterNameMatchesTheRegistry() {
		List<String> expected = new ArrayList<String>();
		for (NodeSchema schema : BotNodeRegistry.schemas()) {
			for (NodeParam param : schema.params()) {
				expected.add(param.name());
			}
		}
		List<String> exported = new ArrayList<String>();
		Matcher matcher = EXPORTED_PARAM_NAME.matcher(BotNodes.export());
		while (matcher.find()) {
			exported.add(matcher.group(1));
		}
		assertEquals(expected, exported, "the export's parameters are not the reflected ones");
	}

	@Test
	void theExportNeverLeaksACompilerGeneratedParameterName() {
		assertFalse(BotNodes.export().contains("arg0"),
				"the export contains arg0; -parameters is off and the schema is unlabelled");
	}

	@Test
	void theExportIsABalancedDocumentThatRepeatsByteForByte() {
		String first = BotNodes.export();
		String second = BotNodes.export();
		assertEquals(first, second, "the export is not deterministic, so it cannot be diffed");
		assertTrue(first.startsWith("{\n"), "not a JSON object");
		assertTrue(first.endsWith("}\n") || first.endsWith("}"), "the document is not closed");
		assertTrue(first.contains("\"version\": " + BotNodes.FORMAT_VERSION));
	}

	private static List<String> names(List<Class<?>> types) {
		List<String> out = new ArrayList<String>();
		for (Class<?> type : types) {
			out.add(type.getName());
		}
		return out;
	}
}
