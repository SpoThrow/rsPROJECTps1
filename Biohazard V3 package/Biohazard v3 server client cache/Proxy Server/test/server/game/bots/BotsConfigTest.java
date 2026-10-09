package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The {@code bots.cfg} parser — roadmap Phase E's authoring surface.
 *
 * <p>The same two things {@code LocationsConfigTest} cares about: that a good row becomes the fields it
 * says, and that a bad row is <em>reported and skipped</em> rather than stopping the file. The second is
 * the one that matters operationally — one typo must not cost the operator every other bot.
 */
class BotsConfigTest {

	private static BotsConfig.Result parse(String... lines) {
		return BotsConfig.parseLines(Arrays.asList(lines));
	}

	private static BotProfile only(BotsConfig.Result result) {
		assertTrue(result.problems().isEmpty(), "no problems: " + result.problems());
		assertEquals(1, result.profiles().size(), "exactly one row");
		return result.profiles().get(0);
	}

	@Test
	void anAccountRowParsesIntoAProfile() {
		BotProfile profile = only(parse("account botwillow password wq7f2k9r script gather_willow "
				+ "home draynor enabled true"));

		assertEquals("botwillow", profile.account());
		assertEquals("wq7f2k9r", profile.password());
		assertEquals("gather_willow", profile.script());
		assertEquals("draynor", profile.home());
		assertTrue(profile.enabled());
		assertEquals("willow", profile.title(), "the display name drops the reserved prefix");
	}

	@Test
	void enabledDefaultsToTrueAndHomeIsOptional() {
		BotProfile profile = only(parse("account botoakh01 password h3n8tz4m script gather_oak"));

		assertTrue(profile.enabled(), "a row is written because someone wants a bot; enabled is the default");
		assertNull(profile.home(), "no home means spawn where the account was saved");
	}

	@Test
	void theEqualsFormAndTheAliasesAreAccepted() {
		BotProfile profile = only(parse("account = botmaples pass secret script gather_oak "
				+ "home varrock enabled no"));

		assertEquals("botmaples", profile.account());
		assertEquals("secret", profile.password());
		assertFalse(profile.enabled(), "a parked row still keeps its credentials");
	}

	@Test
	void theAccountIsLowercasedBecauseLoginLowercases() {
		assertEquals("botwillow", only(parse("account BOTWILLOW password x script s")).account());
	}

	@Test
	void commentsAndBlankLinesAreIgnored() {
		BotsConfig.Result result = parse(
				"# a comment",
				"",
				"   ",
				"// another comment",
				"account botwillow password x script s  # trailing comment",
				"account botoakh01 password y script s");

		assertEquals(2, result.profiles().size());
		assertTrue(result.problems().isEmpty(), result.problems().toString());
	}

	// ---- bad rows are reported, not fatal --------------------------------------------------

	@Test
	void oneBadRowDoesNotCostTheGoodOnes() {
		BotsConfig.Result result = parse(
				"account botwillow password x script s",
				"account !!! bogus password x script s",
				"account botoakh01 password y script s");

		assertEquals(2, result.profiles().size(), "the two good rows survived");
		assertEquals(1, result.problems().size());
		assertTrue(result.problems().get(0).startsWith("line 2:"), result.problems().get(0));
	}

	@Test
	void anAccountThatCouldNotBeLoggedIntoIsRejected() {
		// Over 12 chars, or punctuation: the login decoder would refuse it, so no human could ever
		// take the account over (BOT_ACCOUNTS.md §1).
		BotsConfig.Result tooLong = parse("account botwillowlong password x script s");
		BotsConfig.Result punctuation = parse("account bot_willow password x script s");

		assertEquals(1, tooLong.problems().size());
		assertTrue(tooLong.problems().get(0).contains("not login-legal"), tooLong.problems().get(0));
		assertEquals(1, punctuation.problems().size());
	}

	@Test
	void theRequiredFieldsAreEachReportedWhenMissing() {
		assertTrue(parse("account botwillow password x").problems().get(0).contains("missing script"));
		assertTrue(parse("account botwillow script s").problems().get(0).contains("missing password"));
		assertTrue(parse("= ").problems().get(0).contains("no record given"));
		assertTrue(parse("account botwillow password x script s enabled maybe").problems().get(0)
				.contains("not a boolean"));
		assertTrue(parse("account botwillow password x script s flavour salty").problems().get(0)
				.contains("unknown field"));
		assertTrue(parse("fixture botwillow password x script s").problems().get(0)
				.contains("unknown record"));
		assertTrue(parse("account").problems().get(0).contains("no account name given"));
	}

	@Test
	void aDuplicateAccountIsReportedRatherThanOrderDependent() {
		BotsConfig.Result result = parse(
				"account botwillow password first script a",
				"account botwillow password second script b");

		assertEquals(1, result.profiles().size(), "one row wins; which one must not depend on file order");
		assertEquals("first", result.profiles().get(0).password());
		assertEquals(1, result.problems().size());
		assertTrue(result.problems().get(0).contains("duplicate account"), result.problems().get(0));
	}

	// ---- files -----------------------------------------------------------------------------

	@Test
	void aMissingFileIsNoBotsRatherThanAnError(@TempDir Path dir) {
		BotsConfig.Result result = BotsConfig.load(dir.resolve("not-there.cfg"));

		// "not configured yet" is the ordinary first run, and it must not be a problem to report.
		assertTrue(result.profiles().isEmpty());
		assertTrue(result.problems().isEmpty());
	}

	@Test
	void theShippedTemplateHasNoLiveRows() {
		// The template lives at the default path. A template that parsed with a live row would spawn
		// bots on a fresh checkout, and one with a real credential in it would be a leak.
		Path template = java.nio.file.Paths.get(BotsConfig.DEFAULT_PATH);
		if (!Files.isRegularFile(template)) {
			return;
		}
		BotsConfig.Result result = BotsConfig.load(template);
		assertTrue(result.problems().isEmpty(), "the shipped template must parse clean: "
				+ result.problems());
		assertTrue(result.enabled().isEmpty(), "a shipped template must not spawn anything on boot");
	}

	@Test
	void aRowIsWrittenInTheOneFormTheParserAccepts(@TempDir Path dir) throws IOException {
		BotProfile profile = BotProfile.of("botwillow", "wq7f2k9r", "gather_willow", "draynor", true);
		Path file = dir.resolve("bots.cfg");
		Files.write(file, Arrays.asList(BotsConfig.toRow(profile)), StandardCharsets.UTF_8);

		BotsConfig.Result reloaded = BotsConfig.load(file);

		assertTrue(reloaded.problems().isEmpty(), reloaded.problems().toString());
		assertEquals(profile.account(), only(reloaded).account());
		assertEquals(profile.password(), only(reloaded).password());
		assertEquals(profile.script(), only(reloaded).script());
		assertEquals(profile.home(), only(reloaded).home());
	}
}
