package server.game.players.packets.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import server.game.bots.BotPlayer;

/**
 * The {@code ::bot} entry — roadmap Phase E's command half.
 *
 * <p>The registered command is one {@code where} entry rather than five literals, so these tests are
 * about the two things that shape depends on: that it is gated to owners only, and that dispatching it
 * does not blow up. The behaviour behind the subcommands is {@code BotManager}'s, tested where it lives;
 * here the point is that {@code ::bot} is reachable, owner-only, and single.
 *
 * <p>A {@link BotPlayer} is used as the console target because it is a sessionless {@code Client} whose
 * frame writers work — {@code sendMessage} would NPE on a bare {@code Client}, which never ran a login.
 */
class BotCommandsTest {

	/** An owner's console. A bot is a safe sessionless client; see the class comment. */
	private static BotPlayer console(int playerRights) {
		BotPlayer c = new BotPlayer(0);
		c.playerName = "botconsole";
		c.playerRights = playerRights;
		return c;
	}

	private static int matches(String playerCommand) {
		int hits = 0;
		for (Command command : CommandHandler.all()) {
			if (command.appliesTo(console(3), playerCommand)) {
				hits++;
			}
		}
		return hits;
	}

	@Test
	void everyBotSubcommandIsTheOneRegisteredEntry() {
		assertEquals(1, matches("bot"), "a bare ::bot is the usage line");
		assertEquals(1, matches("bot list"));
		assertEquals(1, matches("bot spawn botwillow"));
		assertEquals(1, matches("bot despawn botwillow"));
		assertEquals(1, matches("bot reload"));
		assertEquals(1, matches("bot info botwillow"));
	}

	@Test
	void botinfoIsItsOwnEntryAndNeverCollidesWithTheBotFamily() {
		// The two are disjoint by one character: `"botinfo x".startsWith("bot ")` is false, so the family
		// entry cannot fire for ::botinfo and vice versa. That is the whole reason ::botinfo can be a
		// separate registration at all.
		assertEquals(1, matches("botinfo"));
		assertEquals(1, matches("botinfo botwillow"));
		assertEquals(0, matches("botinfox"));
	}

	@Test
	void theEntryIsGatedToOwners() {
		for (Command command : CommandHandler.all()) {
			if (command.appliesTo(console(3), "bot list")) {
				assertEquals(3, command.minimumRights());
				assertEquals(3, command.maximumRights(), "a bot command is owner-only");
				// A composite literal reports no literal, which is what keeps it from being five
				// separate prefixes that could each swallow a line.
				assertNull(command.literal());
				return;
			}
		}
		throw new AssertionError("no ::bot command is registered");
	}

	@Test
	void botInfoIsOwnerOnlyToo() {
		for (Command command : CommandHandler.all()) {
			if (command.appliesTo(console(3), "botinfo willownorth")) {
				assertEquals(3, command.minimumRights());
				assertEquals(3, command.maximumRights());
				assertNull(command.literal());
				return;
			}
		}
		throw new AssertionError("no ::botinfo command is registered");
	}

	@Test
	void theTrailingSpaceKeepsBotApartFromALongerWord() {
		// ::bot / ::bot ... only. A word that merely starts with "bot" must not reach it, the same rule
		// that keeps ::banki out of the ban list.
		assertEquals(0, matches("botany"));
		assertEquals(0, matches("botlist"));
	}

	@Test
	void dispatchingTheReadOnlySubcommandsDoesNotThrow() {
		BotPlayer owner = console(3);

		// list reads the last config (empty in a test JVM); spawn/despawn/info with an unknown account
		// only report "no row". None of these touch the live set, so the assertion is simply that the
		// entries run end to end.
		assertTrue(CommandHandler.dispatch(owner, "bot list"));
		assertTrue(CommandHandler.dispatch(owner, "bot spawn nobodyconfigured"));
		assertTrue(CommandHandler.dispatch(owner, "bot despawn nobodyconfigured"));
		assertTrue(CommandHandler.dispatch(owner, "bot info nobodyconfigured"));
		assertTrue(CommandHandler.dispatch(owner, "botinfo nobodyconfigured"));
		assertTrue(CommandHandler.dispatch(owner, "bot nonsense"));
	}
}
