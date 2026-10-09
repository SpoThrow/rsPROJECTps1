package server.game.players.packets.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import server.game.players.Client;
import server.game.players.packets.Commands;

/**
 * Guards the Phase 3.5 command migration. The old chain in {@code Commands} was a run
 * of independent {@code if}s, so these tests care about three things a normal dispatch
 * registry would not: the rights window each command was gated by, the registration
 * order, and the fact that a single input can match more than one command.
 */
class CommandHandlerTest {

	private static final String[] OWNER_EXACT = { "test", "mypos" };
	private static final String[] OWNER_PREFIX = {
			"empty", "item", "givemod", "demote", "giveadmin", "giveowner", "giveresp",
			"givevet", "givefmod", "givedonator", "uidban",
	};
	/** Owner commands whose condition was a literal plus an extra `rights == 3` clause. */
	private static final String[] OWNER_COMPOSITE = {
			"object", "tele", "switch", "interface", "npc", "openbank", "gfx", "update",
			"anim", "setlevel", "pnpc", "unpc", "bot", "botinfo",
	};
	private static final String[] MODERATOR = { "mute", "xteleto", "ipmute" };
	/**
	 * Restored in the Phase 3.5 follow-up. Both sat inside {@code ipmute}'s block and
	 * could never run; both match with a trailing space so they cannot swallow the
	 * player commands {@code banki}/{@code banke}.
	 */
	private static final String[] MODERATOR_RESTORED = { "ban ", "kick " };
	private static final String[] ADMIN = { "unipban", "unipmute", "unmute" };

	/** A bare Client whose predicates the registry can evaluate without a real player. */
	private static Client client(int playerRights) {
		Client c = new Client(null, 1);
		c.playerRights = playerRights;
		return c;
	}

	private static List<Command> matching(int playerRights, String playerCommand) {
		final Client c = client(playerRights);
		final List<Command> hits = new ArrayList<>();
		for (Command command : CommandHandler.all()) {
			if (command.appliesTo(c, playerCommand)) hits.add(command);
		}
		return hits;
	}

	private static String key(Command command) {
		if (command.literal() == null) return "(composite)";
		return (command.isPrefix() ? "^" : "=") + command.literal();
	}

	private static List<String> keys() {
		final List<String> keys = new ArrayList<>();
		for (Command command : CommandHandler.all()) keys.add(key(command));
		return keys;
	}

	private static Command literal(String text, boolean prefix) {
		for (Command command : CommandHandler.all()) {
			if (text.equals(command.literal()) && command.isPrefix() == prefix) return command;
		}
		throw new AssertionError("no " + (prefix ? "prefix" : "exact") + " command \"" + text + "\"");
	}

	@Test
	void registryHoldsEveryMigratedCommandPlusTheTwoRestoredOnesAndBot() {
		// 59 commands were reachable in the old chain; ban and kick were restored on top, and the two
		// hand-written bot commands (::bot, ::botinfo) on top of that — the generated groups must not
		// be edited, so new commands live in their own class.
		assertEquals(63, CommandHandler.all().size());
	}

	@Test
	void compositeCommandsCarryNoLiteral() {
		// The old chain had this many conditions that were not a bare literal test:
		// `a || b` groups, and tests with an extra clause such as `&& c.isBanking`. ::bot and ::botinfo
		// add two more.
		assertEquals(23, keys().stream().filter("(composite)"::equals).count());
	}

	@Test
	void noclipWasNotMigratedAsAServerCommand() {
		// noclip is handled client-side. The server-side guard that used to sit inside
		// `yell` is now Commands.blocksNoclip, not a registry entry.
		for (Command command : CommandHandler.all()) {
			assertFalse("noclip".equals(command.literal()), "noclip should not be a registry entry");
		}
	}

	@Test
	void restoredBanAndKickRunForModeratorsAndAboveOnly() {
		for (String text : MODERATOR_RESTORED) {
			final Command command = literal(text, true);
			assertEquals(1, command.minimumRights(), text);
			assertEquals(3, command.maximumRights(), text);
			assertTrue(command.matchesLiteral(1, text + "Bob"), text);
			assertTrue(command.matchesLiteral(3, text + "Bob"), text);
			assertFalse(command.matchesLiteral(0, text + "Bob"), text);
			assertFalse(command.matchesLiteral(4, text + "Bob"), text);
		}
	}

	@Test
	void restoredBanAndKickNeedTheTrailingSpace() {
		// `startsWith("ban")` would also match the player commands banki and banke, which
		// are registered earlier in the same dispatch list. The trailing space is what
		// keeps ::banki and ::banke out of the ban list.
		assertEquals(1, matching(1, "banki").size());
		assertEquals("=banki", key(matching(1, "banki").get(0)));
		assertEquals(1, matching(1, "banke").size());
		assertEquals("=banke", key(matching(1, "banke").get(0)));
		assertEquals(0, matching(1, "bank").size());
		assertEquals(0, matching(1, "ban").size());
		assertEquals(1, matching(1, "ban Bob").size());
		assertEquals("^ban ", key(matching(1, "ban Bob").get(0)));
		assertEquals(1, matching(1, "kick Bob").size());
		assertEquals("^kick ", key(matching(1, "kick Bob").get(0)));
	}

	@Test
	void noclipGuardRefusesEveryoneButOwners() {
		// The guard used to live at the bottom of `yell`, where it could never fire
		// because reaching it required the input to start with two different prefixes.
		// It is now evaluated once, at the packet entry, before dispatch.
		assertTrue(Commands.blocksNoclip(client(0), "noclip"));
		assertTrue(Commands.blocksNoclip(client(1), "noclip"));
		assertTrue(Commands.blocksNoclip(client(2), "noclip"));
		assertFalse(Commands.blocksNoclip(client(3), "noclip"));
		// it is a prefix test on the whole command text, as it was in the old chain
		assertTrue(Commands.blocksNoclip(client(0), "noclip 1 2"));
		assertTrue(Commands.blocksNoclip(client(0), "noclips"));
		assertFalse(Commands.blocksNoclip(client(0), "unoclip"));
	}

	@Test
	void ownerCommandsAreGatedToRightsThree() {
		for (String text : OWNER_EXACT) {
			assertEquals(3, literal(text, false).minimumRights(), text);
			assertEquals(3, literal(text, false).maximumRights(), text);
		}
		for (String text : OWNER_PREFIX) {
			assertEquals(3, literal(text, true).minimumRights(), text);
			assertEquals(3, literal(text, true).maximumRights(), text);
		}
		for (String text : OWNER_COMPOSITE) {
			assertEquals(3, matching(3, text + " 1").get(0).minimumRights(), text);
			assertTrue(matching(2, text + " 1").stream().noneMatch(c -> c.minimumRights() == 3), text);
		}
	}

	@Test
	void moderatorCommandsRunForRightsOneToThree() {
		for (String text : MODERATOR) {
			final Command command = literal(text, true);
			assertEquals(1, command.minimumRights(), text);
			assertEquals(3, command.maximumRights(), text);
			assertTrue(command.matchesLiteral(1, text + " x"), text);
			assertFalse(command.matchesLiteral(4, text + " x"), text);
		}
	}

	@Test
	void adminCommandsRequireRightsTwoOrMore() {
		for (String text : ADMIN) {
			final Command command = literal(text, true);
			assertEquals(2, command.minimumRights(), text);
			assertEquals(3, command.maximumRights(), text);
			assertFalse(command.matchesLiteral(1, text + " x"), text);
			assertTrue(command.matchesLiteral(2, text + " x"), text);
		}
	}

	@Test
	void commandsClosedOneLevelEarlyStillRunForModerators() {
		// unban, xteletome and ipban were indented as if inside the `rights 2-3` block,
		// but that block had already closed, so they actually ran for rights 1-3. The
		// migration preserves that rather than quietly tightening them to admins.
		for (String text : Arrays.asList("unban", "xteletome", "ipban")) {
			final Command command = literal(text, true);
			assertEquals(1, command.minimumRights(), text);
			assertEquals(3, command.maximumRights(), text);
		}
	}

	@Test
	void xtelToAndXteltomeBothMatchTheLongerCommand() {
		final List<Command> hits = matching(1, "xteletome Bob");
		assertEquals(2, hits.size());
		// source order: `xteleto` sat above `xteletome` in the chain, and both ran
		assertEquals("^xteleto", key(hits.get(0)));
		assertEquals("^xteletome", key(hits.get(1)));
	}

	@Test
	void itemSpawnsTwiceForAnOwnerAndNotAtAllForAnAdmin() {
		// `::item` existed twice: once in the rights 2-3 block but guarded by
		// `rights == 3`, and once again in the rights 3 block. An owner therefore got
		// two spawns; an admin got none.
		assertEquals(2, matching(3, "item 4151 1").size());
		assertEquals(0, matching(2, "item 4151 1").size());
	}

	@Test
	void bankiIsTheOnlyMatchForBanki() {
		// Now that `^ban ` is registered, the trailing space matters: `::banki` must add
		// nothing to the ban list. Covered in more detail by
		// restoredBanAndKickNeedTheTrailingSpace; this pins the single match.
		final List<Command> hits = matching(1, "banki");
		assertEquals(1, hits.size());
		assertEquals("=banki", key(hits.get(0)));
	}

	@Test
	void literalCommandsAreCaseInsensitiveAndPrefixCommandsAreNotExact() {
		assertTrue(literal("players", false).matchesLiteral(0, "PLAYERS"));
		assertFalse(literal("players", false).matchesLiteral(0, "playersx"));
		assertTrue(literal("help", true).matchesLiteral(0, "helper"));
		assertFalse(literal("players", false).matchesLiteral(0, "players x"));
	}

	@Test
	void unknownCommandMatchesNothing() {
		assertTrue(matching(3, "definitelynotacommand").isEmpty());
	}

	@Test
	void yellIsRegisteredExactlyOnce() {
		// `yell` used to contain the (unreachable) noclip guard; lifting it out to
		// Commands.blocksNoclip must not duplicate or drop the command itself.
		assertEquals(1, keys().stream().filter("^yell"::equals).count());
	}
}
