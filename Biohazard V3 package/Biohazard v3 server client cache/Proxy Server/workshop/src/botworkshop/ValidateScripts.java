package botworkshop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import server.game.bots.script.BotScript;
import server.game.bots.script.ScriptDocument;

/**
 * Re-loads every authored script through the server's own loader — {@code BOT_TOOLING.md} §6's
 * "Validate" row.
 *
 * <p>Runs {@link ScriptDocument} over {@code Data/cfg/bots/*.json}, so a document the server would reject
 * at boot fails here instead, in a command an author can run: a broken file that only shows up as a line in
 * the server's startup log is a broken file nobody notices. It builds each script's root as well as parsing
 * it, because "the document is well-formed" and "the tree can be assembled" are two different assertions
 * and the second is the one that matters.
 *
 * <p>Exits non-zero when a file is unreadable, so it can gate a commit the way {@code workshopValidate}
 * gates a map export.
 *
 * <pre>gradlew workshopValidateScripts</pre>
 */
public final class ValidateScripts {

	private ValidateScripts() {
	}

	public static void main(String[] args) throws IOException {
		Path dir = Paths.get(".").toAbsolutePath().normalize().resolve(ScriptDocument.DIR);
		System.out.println();
		System.out.println("[scripts] " + dir);

		if (!Files.isDirectory(dir)) {
			// The ordinary state for a server with no authored scripts — not a failure.
			System.out.println("[scripts] no authored scripts (the directory does not exist)");
			return;
		}

		List<Path> files;
		try (Stream<Path> stream = Files.list(dir)) {
			files = stream.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)
							.endsWith(ScriptDocument.EXTENSION))
					.sorted()
					.collect(Collectors.toList());
		}

		int ok = 0;
		int bad = 0;
		for (Path file : files) {
			String name = ScriptDocument.nameOfFile(file.getFileName().toString());
			try {
				String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
				BotScript script = ScriptDocument.fromJson(name, text);
				// Building the root proves the tree assembles, not only that the JSON parses.
				String root = script.root().name();
				System.out.println("[scripts]   ok    " + name + "  (" + root + ")");
				ok++;
			} catch (RuntimeException e) {
				System.out.println("[scripts]   BAD   " + name + "  " + e.getMessage());
				bad++;
			}
		}

		System.out.println("[scripts] " + ok + " ok, " + bad + " bad");
		if (bad > 0) {
			System.out.println("[scripts] a script the server cannot load will be skipped at boot; "
					+ "fix the file above");
			System.exit(1);
		}
	}
}
