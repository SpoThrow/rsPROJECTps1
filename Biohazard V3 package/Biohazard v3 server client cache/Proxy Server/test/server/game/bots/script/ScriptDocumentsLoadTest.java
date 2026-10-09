package server.game.bots.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Reading a directory of script documents into {@link BotScripts} — {@code BOT_TOOLING.md} §6's "the
 * server loads at startup or on a {@code ::bot reload}".
 *
 * <p>The properties worth pinning are the ones a bad reload would break in production: a bad file costs
 * only itself, a file may not shadow a built-in, and a reload reflects the directory rather than
 * accumulating.
 */
class ScriptDocumentsLoadTest {

	private static final String GOOD = "{\"root\": {\"node\": \"delay\", \"ticks\": 1}}";

	@AfterEach
	void clearFileScripts() {
		// loadFiles drops everything it previously loaded before reading, so this returns the registry to
		// built-ins only between tests. Null is the "no directory" case, which still clears.
		BotScripts.loadFiles(null);
	}

	@Test
	void aDirectoryOfDocumentsRegistersEachScript(@TempDir Path dir) throws IOException {
		write(dir, "gather_willow.json", GOOD);
		write(dir, "mine_iron.json", GOOD);

		BotScripts.LoadResult result = BotScripts.loadFiles(dir);

		assertEquals(2, result.loaded());
		assertTrue(result.problems().isEmpty(), String.valueOf(result.problems()));
		assertNotNull(BotScripts.byName("gather_willow"));
		assertNotNull(BotScripts.byName("mine_iron"));
	}

	@Test
	void scriptNamesComeFromFileNames(@TempDir Path dir) throws IOException {
		// bots.cfg's script field names the file, so the file name is the script's handle.
		write(dir, "gather_willow.json", GOOD);

		BotScripts.loadFiles(dir);

		assertNotNull(BotScripts.byName("gather_willow"));
		assertNull(BotScripts.byName("gather_willow.json"));
	}

	@Test
	void aMalformedFileIsSkippedAndReported(@TempDir Path dir) throws IOException {
		write(dir, "good.json", GOOD);
		write(dir, "broken.json", "{\"root\": {\"node\": \"no_such_node\"}}");

		BotScripts.LoadResult result = BotScripts.loadFiles(dir);

		assertEquals(1, result.loaded(), "the good file should still load");
		assertEquals(1, result.problems().size());
		assertTrue(result.problems().get(0).startsWith("broken.json"), result.problems().get(0));
		assertNotNull(BotScripts.byName("good"));
		assertNull(BotScripts.byName("broken"));
	}

	@Test
	void aFileOfBadJsonIsSkippedAndReported(@TempDir Path dir) throws IOException {
		write(dir, "broken.json", "{\"root\": {");

		BotScripts.LoadResult result = BotScripts.loadFiles(dir);

		assertEquals(0, result.loaded());
		assertEquals(1, result.problems().size());
		assertTrue(result.problems().get(0).contains("line"), result.problems().get(0));
	}

	@Test
	void aFileShadowingABuiltInIsReportedAndTheBuiltInIsKept(@TempDir Path dir) throws IOException {
		// gather_oak ships in code. A file must not be able to replace the script the server always has, and
		// the operator has to be told why their file did nothing.
		BotScript before = BotScripts.byName("gather_oak");
		assertNotNull(before, "the built-in should exist");

		write(dir, "gather_oak.json", GOOD);
		BotScripts.LoadResult result = BotScripts.loadFiles(dir);

		assertEquals(0, result.loaded());
		assertEquals(1, result.problems().size());
		assertTrue(result.problems().get(0).contains("built-in"), result.problems().get(0));
		assertTrue(BotScripts.byName("gather_oak") == before, "the built-in should be untouched");
	}

	@Test
	void reloadingReflectsTheDirectory(@TempDir Path dir) throws IOException {
		Path first = write(dir, "first.json", GOOD);
		BotScripts.loadFiles(dir);
		assertNotNull(BotScripts.byName("first"));

		// Edit the file, delete it, and add another: a reload must show the directory as it is now rather
		// than the union of every load.
		Files.delete(first);
		write(dir, "second.json", GOOD);
		BotScripts.LoadResult result = BotScripts.loadFiles(dir);

		assertEquals(1, result.loaded());
		assertNull(BotScripts.byName("first"), "a deleted file's script should be gone");
		assertNotNull(BotScripts.byName("second"));
		assertNotNull(BotScripts.byName("gather_oak"), "a reload must never drop a built-in");
	}

	@Test
	void reloadingAChangedFileReplacesItsScript(@TempDir Path dir) throws IOException {
		write(dir, "a_script.json", "{\"root\": {\"node\": \"delay\", \"ticks\": 1}}");
		BotScripts.loadFiles(dir);
		BotScript before = BotScripts.byName("a_script");

		write(dir, "a_script.json", "{\"root\": {\"node\": \"delay\", \"ticks\": 5}}");
		BotScripts.LoadResult result = BotScripts.loadFiles(dir);

		assertEquals(1, result.loaded());
		assertTrue(BotScripts.byName("a_script") != before, "the edited file should be re-read");
	}

	@Test
	void nonJsonFilesAreIgnored(@TempDir Path dir) throws IOException {
		write(dir, "notes.txt", "not a script");
		write(dir, "locations.cfg", "not a script either");
		write(dir, "real.json", GOOD);

		BotScripts.LoadResult result = BotScripts.loadFiles(dir);

		assertEquals(1, result.loaded());
		assertTrue(result.problems().isEmpty(), String.valueOf(result.problems()));
	}

	@Test
	void aMissingDirectoryIsNoScriptsRatherThanAnError() {
		// A server with no authored scripts is the ordinary state, like a missing bots.cfg.
		BotScripts.LoadResult result = BotScripts.loadFiles(Path.of("does", "not", "exist"));

		assertEquals(0, result.loaded());
		assertTrue(result.problems().isEmpty(), String.valueOf(result.problems()));
	}

	private static Path write(Path dir, String name, String content) throws IOException {
		Path file = dir.resolve(name);
		Files.write(file, content.getBytes(StandardCharsets.UTF_8));
		return file;
	}
}
