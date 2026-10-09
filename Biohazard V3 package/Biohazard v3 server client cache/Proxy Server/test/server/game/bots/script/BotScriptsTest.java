package server.game.bots.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import server.game.bots.BotState;

/**
 * The script registry — the table {@code bots.cfg} will read in Phase E.
 *
 * <p>The cases worth pinning are the ones a config line depends on: a name resolves (case-insensitively,
 * because an author will not remember the case), an unknown name is a null rather than a throw, a
 * duplicate is rejected at registration rather than resolved by class-load order, and every possession
 * gets its own tree.
 */
class BotScriptsTest {

	@Test
	void theBuiltInGatheringBotIsRegisteredAndResolvable() {
		BotScript script = BotScripts.byName("gather_oak");

		assertNotNull(script, "the Phase D acceptance criterion is a registered script, not just a test");
		assertEquals("gather_oak", script.name());
		assertTrue(BotScripts.names().contains("gather_oak"));
	}

	@Test
	void aNameIsResolvedRegardlessOfCaseOrSurroundingSpace() {
		assertNotNull(BotScripts.byName("GATHER_OAK"));
		assertNotNull(BotScripts.byName("  gather_oak  "));
	}

	@Test
	void anUnknownNameIsNullRatherThanAnError() {
		assertNull(BotScripts.byName("no_such_script"));
		assertNull(BotScripts.byName(null));
		assertNull(BotScripts.possess("nobody", "swordfish", "no_such_script"),
				"an unknown script must not half-spawn a bot");
	}

	@Test
	void theListingIsSortedSoItDiffsCleanly() {
		BotScripts.register(BotScript.named("zzz_test_script").walkTo(1, 2, 1).once());
		BotScripts.register(BotScript.named("aaa_test_script").walkTo(1, 2, 1).once());

		List<String> names = BotScripts.names();
		List<String> expected = new ArrayList<String>(names);
		expected.sort(null);

		assertEquals(expected, names);
		assertTrue(names.indexOf("aaa_test_script") < names.indexOf("zzz_test_script"));
	}

	@Test
	void twoScriptsMayNotShareAName() {
		BotScripts.register(BotScript.named("dup_test_script").walkTo(1, 2, 1).once());

		// A duplicate would make the meaning of a config line depend on class-load order.
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
				() -> BotScripts.register(BotScript.named("DUP_TEST_SCRIPT").walkTo(1, 2, 1).once()));
		assertTrue(thrown.getMessage().contains("DUP_TEST_SCRIPT"), thrown.getMessage());
	}

	@Test
	void everyPossessionGetsItsOwnTree() {
		BotScript script = BotScripts.byName("gather_oak");

		BotState first = script.root();
		BotState second = script.root();

		assertNotNull(first);
		assertFalse(first == second, "two bots on one script must not share progress");
	}
}
