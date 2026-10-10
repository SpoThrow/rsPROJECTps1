package botworkshop.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import server.game.bots.BotState;
import server.game.bots.script.ScriptDocument;

/**
 * The script-document writer — {@code BOT_TOOLING.md} T5's Save/Validate half.
 *
 * <p>Two properties matter and both are about what reaches disk: the file is a document the server's own
 * loader accepts (so the tool cannot invent a dialect), and the same graph always produces the same bytes
 * (so the output is git-diffable and a re-save is not a spurious change). The rest is refusals — a name
 * that is not a file name, a built-in a file may not shadow, a document that is not a script.
 */
class ScriptDocsTest {

	private static final String NAME = "test_script";

	/** A small but complete graph: chop loop with a condition, so nesting and a list are both covered. */
	private static final String CHOP =
			"{\"root\": {\"node\": \"sequence\", \"children\": ["
			+ "{\"node\": \"walk_to_nearest\", \"kind\": \"tree\", \"range\": 3},"
			+ "{\"node\": \"gather\", \"kind\": \"tree\", \"itemId\": 1519},"
			+ "{\"node\": \"bank_logs\", \"logItemId\": 1519}]}}";

	// ---- canonical form ------------------------------------------------------------------------

	@Test
	void theCanonicalFormPutsNodeFirstThenTheSchemaOrder() {
		// The input names the parameters backwards; the canonical form must not care, so an author's
		// formatting never shows up as a diff.
		String out = ScriptDocs.canonicalize(NAME,
				"{\"root\": {\"itemId\": 1519, \"kind\": \"tree\", \"node\": \"gather\"}}");

		int node = out.indexOf("\"node\"");
		int kind = out.indexOf("\"kind\"");
		int item = out.indexOf("\"itemId\"");
		assertTrue(node >= 0 && kind > node && item > kind,
				"expected node, kind, itemId in that order, got:\n" + out);
	}

	@Test
	void aWholeNumberStaysAWholeNumber() {
		String out = ScriptDocs.canonicalize(NAME, "{\"root\": {\"node\": \"has_item\", \"itemId\": 1519}}");

		assertTrue(out.contains("\"itemId\": 1519"), out);
		assertFalse(out.contains("1519.0"), out);
	}

	@Test
	void anOmittedOptionalParameterIsNotInvented() {
		// gather's range and radius have defaults; a document that did not state them must not gain them, or
		// the file would say more than the author did.
		String out = ScriptDocs.canonicalize(NAME, "{\"root\": {\"node\": \"gather\", \"kind\": \"tree\", "
				+ "\"itemId\": 1519}}");

		assertFalse(out.contains("\"range\""), out);
		assertFalse(out.contains("\"radius\""), out);
	}

	@Test
	void aStatedDefaultIsKept() {
		// The opposite rule: what the document said is the author's intent, so an explicit default stays.
		String out = ScriptDocs.canonicalize(NAME, "{\"root\": {\"node\": \"gather\", \"kind\": \"tree\", "
				+ "\"itemId\": 1519, \"range\": 3}}");

		assertTrue(out.contains("\"range\": 3"), out);
	}

	@Test
	void childrenAreEmittedInOrder() {
		String out = ScriptDocs.canonicalize(NAME, CHOP);

		int walk = out.indexOf("\"walk_to_nearest\"");
		int gather = out.indexOf("\"gather\"");
		int bank = out.indexOf("\"bank_logs\"");
		assertTrue(walk >= 0 && gather > walk && bank > gather, out);
	}

	@Test
	void aNameFieldIsDroppedBecauseTheFileNameIsTheName() {
		String out = ScriptDocs.canonicalize(NAME,
				"{\"name\": \"" + NAME + "\", \"root\": {\"node\": \"inventory_full\"}}");

		assertFalse(out.contains("\"name\""), out);
		assertTrue(out.contains("\"inventory_full\""), out);
	}

	@Test
	void canonicalizingTwiceIsByteIdentical() {
		// The whole point of a canonical form: a re-save with no edit is not a change.
		String once = ScriptDocs.canonicalize(NAME, CHOP);
		String twice = ScriptDocs.canonicalize(NAME, once);

		assertEquals(once, twice);
	}

	// ---- the file the tool writes is a file the server loads -----------------------------------

	@Test
	void theCanonicalFormIsAcceptedByTheServerLoader() {
		// The one assertion that makes the tool trustworthy: what it writes, the runtime reads.
		String canonical = ScriptDocs.canonicalize(NAME, CHOP);

		BotState root = ScriptDocument.fromJson(NAME, canonical).root();

		assertTrue(root != null, "the loader built no root from the canonical text");
	}

	@Test
	void theCanonicalFormKeepsTheStructureTheLoaderRead() {
		String canonical = ScriptDocs.canonicalize(NAME, CHOP);

		@SuppressWarnings("unchecked")
		Map<String, Object> document = (Map<String, Object>) server.game.bots.script.Json.parse(canonical);
		@SuppressWarnings("unchecked")
		Map<String, Object> root = (Map<String, Object>) document.get("root");
		assertEquals("sequence", root.get("node"));
		@SuppressWarnings("unchecked")
		List<Object> children = (List<Object>) root.get("children");
		assertEquals(3, children.size());
		@SuppressWarnings("unchecked")
		Map<String, Object> gather = (Map<String, Object>) children.get(1);
		assertEquals("gather", gather.get("node"));
		assertEquals(1519.0, gather.get("itemId"));
	}

	// ---- refusals ------------------------------------------------------------------------------

	@Test
	void anInvalidDocumentIsRejectedWithTheOffendingNodeNamed() {
		ScriptDocument.ScriptException error = assertThrows(ScriptDocument.ScriptException.class,
				() -> ScriptDocs.canonicalize(NAME, "{\"root\": {\"node\": \"no_such_node\"}}"));

		assertTrue(error.getMessage().contains("no_such_node"), error.getMessage());
	}

	@Test
	void aDocumentThatIsNotAnObjectIsRejected() {
		assertThrows(ScriptDocs.DocException.class, () -> ScriptDocs.canonicalize(NAME, "5"));
	}

	@Test
	void aNameThatIsNotAFileNameIsRejected() {
		// The name becomes a file name, so anything that could leave the directory or that two machines
		// would read differently is refused.
		for (String bad : new String[] { "../evil", "has space", "Uppercase", "dash-ed", "dot.name", "", null }) {
			assertThrows(ScriptDocs.DocException.class, () -> ScriptDocs.requireSafeName(bad),
					"should have refused: " + bad);
		}
	}

	@Test
	void aNameAtTheLengthLimitIsAcceptedAndOneOverIsNot() {
		ScriptDocs.requireSafeName("a".repeat(48));
		assertThrows(ScriptDocs.DocException.class, () -> ScriptDocs.requireSafeName("a".repeat(49)));
	}

	// ---- writing -------------------------------------------------------------------------------

	@Test
	void saveWritesTheCanonicalTextWithATrailingNewline(@TempDir Path dir) throws IOException {
		String written = ScriptDocs.save(dir, NAME, CHOP);

		String onDisk = new String(Files.readAllBytes(dir.resolve(NAME + ".json")), StandardCharsets.UTF_8);
		assertEquals(written + "\n", onDisk);
		assertTrue(onDisk.contains("\"bank_logs\""), onDisk);
	}

	@Test
	void saveRefusesABuiltInNameAndWritesNothing(@TempDir Path dir) throws IOException {
		assertThrows(ScriptDocs.DocException.class,
				() -> ScriptDocs.save(dir, "gather_oak", CHOP));

		assertFalse(Files.exists(dir.resolve("gather_oak.json")),
				"a refused save must not create the file the loader would skip");
	}

	@Test
	void saveLeavesTheOldFileAloneWhenTheNewDocumentIsInvalid(@TempDir Path dir) throws IOException {
		ScriptDocs.save(dir, NAME, CHOP);
		String before = new String(Files.readAllBytes(dir.resolve(NAME + ".json")), StandardCharsets.UTF_8);

		assertThrows(RuntimeException.class,
				() -> ScriptDocs.save(dir, NAME, "{\"root\": {\"node\": \"no_such_node\"}}"));

		String after = new String(Files.readAllBytes(dir.resolve(NAME + ".json")), StandardCharsets.UTF_8);
		assertEquals(before, after, "a rejected save must not half-write the script");
	}

	// ---- listing and reading -------------------------------------------------------------------

	@Test
	void listReportsEachFileAndWhetherTheLoaderAcceptsIt(@TempDir Path dir) throws IOException {
		ScriptDocs.save(dir, "good_one", CHOP);
		Files.write(dir.resolve("bad_one.json"), "{\"root\": {\"node\": \"nope\"}}".getBytes(StandardCharsets.UTF_8));

		String json = ScriptDocs.list(dir);

		assertTrue(json.contains("\"good_one\""), json);
		assertTrue(json.contains("\"bad_one\""), json);
		// The malformed file is listed with its reason rather than hidden: an author has to be able to see
		// the script that is on disk and broken.
		assertTrue(json.contains("nope"), json);
	}

	@Test
	void listOnAMissingDirectoryIsAnEmptyList(@TempDir Path dir) {
		String json = ScriptDocs.list(dir.resolve("nope"));

		assertTrue(json.contains("\"scripts\""), json);
		assertFalse(json.contains("good_one"), json);
	}

	@Test
	void readReturnsTheSavedDocumentOrNull(@TempDir Path dir) throws IOException {
		ScriptDocs.save(dir, NAME, CHOP);

		String text = ScriptDocs.read(dir, NAME);
		assertTrue(text != null && text.contains("\"sequence\""), String.valueOf(text));
		assertNull(ScriptDocs.read(dir, "not_there"));
	}

	@Test
	void theCommittedExampleIsCanonicalSoReSavingItIsNotADiff() throws IOException {
		// Pins the artifact the repo ships (BOT_TOOLING.md T6): the committed script must be exactly the
		// bytes the editor would write, so re-saving it after an unrelated edit produces no diff. If this
		// fails, the file was hand-edited or the canonical form changed — re-save it from the editor.
		String dataRoot = System.getProperty("workshopDataRoot");
		assertTrue(dataRoot != null && !dataRoot.isBlank(),
				"the workshopTest task must set workshopDataRoot to Data/; see build.gradle");

		Path file = Path.of(dataRoot, "cfg", "bots", "chop_and_bank.json");
		assertTrue(Files.isRegularFile(file), "missing " + file + " — the committed example");

		String onDisk = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
		assertEquals(onDisk, ScriptDocs.canonicalize("chop_and_bank", onDisk) + "\n",
				"re-saving chop_and_bank in the editor should be a no-op");
	}
}
