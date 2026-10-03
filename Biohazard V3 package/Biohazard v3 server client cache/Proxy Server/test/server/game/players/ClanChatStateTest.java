package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Pins the clan-chat cluster after Phase 4.3 moved its three fields off {@link Player}
 * into {@link ClanChatState}.
 *
 * <p>The defaults matter here more than they look, because two call sites dereference
 * {@code clanChat.channel} unguarded: {@code PlayerSave} calls
 * {@code channel.length()} when saving, and {@code Client} calls {@code channel.length()}
 * on login after a null check. A null default would NPE a brand-new character on its
 * first save, so the empty-string default is a real invariant, not a style choice.
 */
class ClanChatStateTest {

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void everyFieldKeepsItsOldDefault() {
		final ClanChatState clanChat = new ClanChatState();

		// `lastClanChat = ""` and `pendingClanKick = ""` were initialised to empty strings;
		// neither was null, and PlayerSave relies on that
		assertEquals("", clanChat.channel);
		assertEquals("", clanChat.pendingKick);
		assertEquals(0L, clanChat.pendingKickAt);
	}

	@Test
	void theChannelIsNeverNullSoTheSavePathCannotNpe() {
		// PlayerSave does `characterfile.write(p.clanChat.channel, 0, len)` with no null
		// guard, and Client does `channel != null && channel.length() > 0` on login. The
		// first of those is an unguarded `.length()`, so pin the default directly.
		final Client fresh = client();
		assertEquals("", fresh.clanChat.channel);
		assertEquals(0, fresh.clanChat.channel.length());
	}

	@Test
	void eachPlayerOwnsItsOwnState() {
		final Client a = client();
		final Client b = client();

		assertSame(a.clanChat, a.clanChat, "the same player must keep one state object");
		assertNotSame(a.clanChat, b.clanChat, "two players must not share clan-chat state");

		a.clanChat.channel = "Founder";
		a.clanChat.pendingKick = "Griefer";
		a.clanChat.pendingKickAt = 1234L;
		assertEquals("", b.clanChat.channel, "clan-chat state leaked between players");
		assertEquals("", b.clanChat.pendingKick, "clan-chat state leaked between players");
		assertEquals(0L, b.clanChat.pendingKickAt, "clan-chat state leaked between players");
	}
}
