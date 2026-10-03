package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import core.util.Misc;

/**
 * Loads the <b>real, pre-refactor character files</b> through the production loader.
 *
 * <p>Why this exists, and why {@code PlayerSaveTest} does not already cover it: that test
 * round-trips a character it writes with the <em>new</em> code, which proves only that a file the
 * new code produced can be read by the new code. It would pass unchanged if a save key had been
 * renamed during Phase 4, because it writes and reads the same key. The property that actually
 * matters here is different — <b>the characters on disk were written by the old code, and every one
 * of them must still load after the refactor.</b> Six clusters moved fields whose file keys were
 * deliberately left alone (§4.7 position, §4.10/§4.11 timers, §4.13 special attack, §4.15 magic,
 * §4.18 ranged attack, §4.21 combat style), and a renamed key degrades silently: the player logs in
 * with a default value and nothing throws. This test is the only place that would notice.
 *
 * <p>It is strictly read-only against the real directory. Each file is parsed as text here, copied
 * into the isolation tree, and only the <em>copy</em> is touched (to plant a known password), so
 * this can never mutate a live character.
 */
class RealCharacterFilesTest {

	private static final int SLOT = 1;
	/** Any plaintext works: the copy's password line is rewritten to this hash before loading. */
	private static final String PASS = "swordfish";

	private static Path workDir() {
		return Paths.get("./Data/characters");
	}

	/**
	 * The real character directory. Gradle passes it explicitly; the relative fallback keeps the
	 * test runnable from an IDE whose working directory is the project root.
	 */
	private static Path realDir() {
		String configured = System.getProperty("realCharactersDir");
		return configured != null ? Paths.get(configured) : Paths.get("../../Data/characters");
	}

	@AfterEach
	void tearDown() throws IOException {
		PlayerHandler.players[SLOT] = null;
	}

	/** The value after "key = ", or empty when the character predates that key. */
	private static OptionalInt intField(List<String> lines, String key) {
		String prefix = key + " = ";
		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.startsWith(prefix)) {
				return OptionalInt.of(Integer.parseInt(trimmed.substring(prefix.length()).trim()));
			}
		}
		return OptionalInt.empty();
	}

	private static OptionalDouble doubleField(List<String> lines, String key) {
		String prefix = key + " = ";
		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.startsWith(prefix)) {
				return OptionalDouble.of(Double.parseDouble(trimmed.substring(prefix.length()).trim()));
			}
		}
		return OptionalDouble.empty();
	}

	@TestFactory
	Stream<DynamicTest> everyRealCharacterStillLoadsWithItsMovedFieldsIntact() throws IOException {
		Path real = realDir();
		assumeTrue(Files.isDirectory(real), "no real character directory at " + real.toAbsolutePath());

		List<Path> files = new ArrayList<>();
		try (Stream<Path> entries = Files.list(real)) {
			entries.filter(Files::isRegularFile)
					.filter(p -> p.getFileName().toString().endsWith(".txt"))
					.filter(p -> !p.getFileName().toString().endsWith(".bak"))
					.forEach(files::add);
		}
		assumeTrue(!files.isEmpty(), "no .txt character files found in " + real.toAbsolutePath());

		return files.stream().map(path -> DynamicTest.dynamicTest(
				path.getFileName().toString().replace(".txt", ""),
				() -> assertRealCharacterLoads(path)));
	}

	private void assertRealCharacterLoads(Path realFile) throws IOException {
		List<String> lines = new ArrayList<>(Files.readAllLines(realFile, StandardCharsets.UTF_8));
		assumeTrue(lines.stream().anyMatch(l -> l.trim().startsWith("character-password")),
				realFile + " has no password line to stand in for");

		// Read the expected values out of the file before touching anything, so the assertions
		// below compare the loader's result against the file itself rather than a guess. Keys a
		// given character predates are simply absent, and are then not asserted.
		OptionalInt filePosX = intField(lines, "character-posx");
		OptionalInt fileHeight = intField(lines, "character-height");
		OptionalInt fileCrystalBow = intField(lines, "crystal-bow-shots");
		OptionalInt fileSkull = intField(lines, "skull-timer");
		OptionalInt fileTeleblock = intField(lines, "teleblock-length");
		OptionalInt fileFightMode = intField(lines, "fightMode");
		OptionalInt fileMagicBook = intField(lines, "magic-book");
		OptionalDouble fileSpec = doubleField(lines, "special-amount");

		// Plant a password we know on the COPY only.
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).trim().startsWith("character-password")) {
				lines.set(i, "character-password = " + Misc.md5Hash(PASS));
				break;
			}
		}
		Path work = workDir();
		Files.createDirectories(work);
		Path copy = work.resolve("RealLoad-check.txt");
		Files.write(copy, lines, StandardCharsets.UTF_8);

		Client c = new Client(null, SLOT);
		PlayerHandler.players[SLOT] = c;
		assertEquals(1, PlayerSave.loadGame(c, "RealLoad-check", PASS),
				realFile.getFileName() + " must load cleanly to [EOF]");

		int asserted = 0;

		// §4.7 -- position. `character-posx` has a documented <=0 fallback, so mirror it.
		if (fileHeight.isPresent()) {
			assertEquals(fileHeight.getAsInt(), c.position.heightLevel,
					"character-height must reach position.heightLevel");
			asserted++;
		}
		if (filePosX.isPresent()) {
			int expected = filePosX.getAsInt() <= 0 ? 3210 : filePosX.getAsInt();
			assertEquals(expected, c.position.teleportToX,
					"character-posx must reach position.teleportToX");
			asserted++;
		}

		// §4.18 -- ranged attack. The key stayed `crystal-bow-shots` while the field moved.
		if (fileCrystalBow.isPresent()) {
			assertEquals(fileCrystalBow.getAsInt(), c.rangedAttack.crystalBowArrowCount,
					"crystal-bow-shots must reach rangedAttack.crystalBowArrowCount (§4.18)");
			asserted++;
		}

		// §4.10/§4.11 -- timers. skullTimer is a negative sentinel here, so equality is the test.
		if (fileSkull.isPresent()) {
			assertEquals(fileSkull.getAsInt(), c.timers.skullTimer,
					"skull-timer must reach timers.skullTimer");
			asserted++;
		}
		if (fileTeleblock.isPresent()) {
			assertEquals(fileTeleblock.getAsInt(), c.timers.teleBlockLength,
					"teleblock-length must reach timers.teleBlockLength");
			asserted++;
		}

		// §4.21 -- combat style. A renamed key here silently resets every player to accurate.
		if (fileFightMode.isPresent()) {
			assertEquals(fileFightMode.getAsInt(), c.combatStyle.fightMode,
					"fightMode must reach combatStyle.fightMode (§4.21)");
			asserted++;
		}

		// §4.15 -- magic.
		if (fileMagicBook.isPresent()) {
			assertEquals(fileMagicBook.getAsInt(), c.magic.playerMagicBook,
					"magic-book must reach magic.playerMagicBook");
			asserted++;
		}

		// §4.13 -- special attack. Stored as a double in the file.
		if (fileSpec.isPresent()) {
			assertEquals(c.specialAttack.specAmount, fileSpec.getAsDouble(),
					"special-amount must reach specialAttack.specAmount (§4.13)");
			asserted++;
		}

		assertTrue(asserted > 0,
				realFile.getFileName() + " exercised none of the moved keys -- the test would be vacuous");

		Files.deleteIfExists(copy);
	}
}
