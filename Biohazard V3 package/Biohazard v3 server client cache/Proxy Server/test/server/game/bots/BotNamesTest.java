package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Slice-1 step 2: {@code BotNames} is the naming half of the account rules in
 * {@code BOT_ACCOUNTS.md} — a name is only usable if the login decoder would accept it.
 */
class BotNamesTest {

	@Test
	void thePrefixIsAlphanumericAndLowercase() {
		assertEquals("bot", BotNames.PREFIX);
		for (int i = 0; i < BotNames.PREFIX.length(); i++) {
			char c = BotNames.PREFIX.charAt(i);
			assertTrue(c >= 'a' && c <= 'z', "an illegal prefix char would make every bot unloggable");
		}
	}

	@Test
	void accountForJoinsPrefixAndLowercasedSlug() {
		assertEquals("botwillow", BotNames.accountFor("willow"));
		assertEquals("botwillow", BotNames.accountFor("Willow"));
		assertEquals("botwillow", BotNames.accountFor("  willow  "));
		assertEquals("bot", BotNames.accountFor(null));
	}

	@Test
	void hasBotPrefixIsCaseInsensitiveAndNeedsTheLength() {
		assertTrue(BotNames.hasBotPrefix("botwillow"));
		assertTrue(BotNames.hasBotPrefix("BOTWILLOW"));
		assertFalse(BotNames.hasBotPrefix("willow"));
		assertFalse(BotNames.hasBotPrefix("bo"));
		assertFalse(BotNames.hasBotPrefix(null));
	}

	@Test
	void aGeneratedNameIsAlwaysLoginLegal() {
		String name = BotNames.accountFor("willow");
		assertTrue(BotNames.isLoginLegal(name), "a name the server would reject defeats possession");
	}

	@Test
	void punctuationIsRejectedBecauseTheLoginDecoderRejectsIt() {
		// The original `[bot]Name` from BOT_PLAN's first draft could never be logged into.
		assertFalse(BotNames.isLoginLegal("[bot]willow"));
		assertFalse(BotNames.isLoginLegal("bot-willow"));
		assertFalse(BotNames.isLoginLegal("bot_willow"));
	}

	@Test
	void lengthAndEmptinessMatchTheServerLimits() {
		assertFalse(BotNames.isLoginLegal(null));
		assertFalse(BotNames.isLoginLegal(""));
		assertFalse(BotNames.isLoginLegal("   "));
		assertTrue(BotNames.isLoginLegal("abcdefghijkl"), "12 chars is the cap, inclusive");
		assertFalse(BotNames.isLoginLegal("abcdefghijklm"), "13 chars is refused by login");
	}

	@Test
	void aSpaceIsLegalBecauseTheDecoderAllowsIt() {
		assertTrue(BotNames.isLoginLegal("bot willow"));
	}
}
