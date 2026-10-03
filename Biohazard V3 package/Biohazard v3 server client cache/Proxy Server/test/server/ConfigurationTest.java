package server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link Configuration}'s parsing and, more importantly, its fallback behaviour.
 *
 * <p>The contract that matters is that a checkout with no {@code server.properties} behaves exactly
 * as it did before the class existed, because the two credential call sites pass the old hardcoded
 * strings as their defaults. If a getter ever stopped returning its fallback, the server would start
 * with different credentials than before — silently, and only failing at connect time.
 */
class ConfigurationTest {

	@Test
	void parsesKeyValuePairsAndTrimsBothSides() {
		Configuration config = Configuration.parse(Arrays.asList(
				"hiscores.host = db.example.com",
				"  hiscores.user=root  "));
		assertEquals("db.example.com", config.getString("hiscores.host", "localhost"));
		assertEquals("root", config.getString("hiscores.user", "nobody"));
	}

	@Test
	void commentsAndBlankLinesAndJunkAreAllSkipped() {
		// Hand-editing this file must not be able to stop the server booting.
		Configuration config = Configuration.parse(Arrays.asList(
				"# a comment",
				"! also a comment",
				"",
				"   ",
				"this line has no equals sign",
				"=novalue",
				"hiscores.database=hiscores"));
		assertEquals(1, config.count());
		assertEquals("hiscores", config.getString("hiscores.database", "unset"));
	}

	@Test
	void lookupsAreCaseInsensitiveBecauseOperatorsTypeAnyway() {
		Configuration config = Configuration.parse(Collections.singletonList("Hiscores.Host=db"));
		assertEquals("db", config.getString("hiscores.host", "localhost"));
		assertEquals("db", config.getString("HISCORES.HOST", "localhost"));
	}

	@Test
	void theFirstOccurrenceOfAKeyWinsSoADuplicateCannotShadowIt() {
		Configuration config = Configuration.parse(Arrays.asList(
				"hiscores.password=first",
				"hiscores.password=second"));
		assertEquals("first", config.getString("hiscores.password", "fallback"));
	}

	@Test
	void aMissingKeyReturnsTheFallbackAndTheFallbackIsWhatOldCodeHardcoded() {
		Configuration config = Configuration.empty();
		// These four are the exact literals that used to sit in HiscoresHandler.
		assertEquals("localhost", config.getString("hiscores.host", "localhost"));
		assertEquals("hiscores", config.getString("hiscores.database", "hiscores"));
		assertEquals("root", config.getString("hiscores.user", "root"));
		assertEquals("------", config.getString("hiscores.password", "------"));
		// And the vote loader's, from Server's static initialiser.
		assertEquals("vote", config.getString("vote.database", "vote"));
		assertFalse(config.has("hiscores.host"));
		assertEquals(0, config.count());
	}

	@Test
	void aPresentButBlankValueFallsBackRatherThanSupplyingAnEmptyString() {
		// A template copied without filling in a password must not become an empty password.
		Configuration config = Configuration.parse(Collections.singletonList("hiscores.password="));
		assertEquals("------", config.getString("hiscores.password", "------"));
		assertFalse(config.has("hiscores.password"));

		Configuration blank = Configuration.parse(Collections.singletonList("hiscores.password=   "));
		assertEquals("------", blank.getString("hiscores.password", "------"));
	}

	@Test
	void aHashInsideAValueIsPreservedNotTreatedAsAComment() {
		// Only a leading # starts a comment, so a real password containing # survives.
		Configuration config = Configuration.parse(Collections.singletonList("hiscores.password=pa#ss"));
		assertEquals("pa#ss", config.getString("hiscores.password", "------"));
	}

	@Test
	void intsAndBooleansFallBackRatherThanThrowingOnBadInput() {
		Configuration config = Configuration.parse(Arrays.asList(
				"a=42",
				"b=notanumber",
				"c=true",
				"d=yes"));
		assertEquals(42, config.getInt("a", -1));
		assertEquals(-1, config.getInt("b", -1), "a typo must not stop the boot");
		assertEquals(7, config.getInt("missing", 7));
		assertTrue(config.getBoolean("c", false));
		// "yes" is not accepted: a typo keeps the default rather than guessing.
		assertFalse(config.getBoolean("d", false));
		assertTrue(config.getBoolean("missing", true));
	}

	@Test
	void aMissingFileYieldsAnEmptyConfigurationInsteadOfNull() {
		// Server's static initialiser calls this before main(), so it must never return null.
		Configuration missing = Configuration.load(Paths.get("./definitely/not/here.properties"));
		assertEquals(0, missing.count());
		assertEquals("localhost", missing.getString("vote.host", "localhost"));
	}

	@Test
	void theCheckedInTemplateParsesAndDocumentsEveryKeyWithoutChangingBehaviour() {
		// The template ships with blank passwords precisely so that copying it verbatim is a no-op.
		String configured = System.getProperty("serverPropertiesTemplate");
		if (configured == null) {
			return; // Not run through the Gradle test task; nothing to assert.
		}
		java.nio.file.Path path = Paths.get(configured);
		if (!java.nio.file.Files.isRegularFile(path)) {
			return;
		}
		Configuration template = Configuration.load(path);
		assertTrue(template.count() >= 8, "expected every documented key, got " + template.count());
		assertEquals("------", template.getString("hiscores.password", "------"));
		assertEquals("------", template.getString("vote.password", "------"));
		assertEquals("localhost", template.getString("hiscores.host", "localhost"));
		assertEquals("hiscores", template.getString("hiscores.database", "hiscores"));
		assertEquals("vote", template.getString("vote.database", "vote"));
		assertEquals("root", template.getString("hiscores.user", "root"));
		assertEquals("root", template.getString("vote.user", "root"));
	}
}
