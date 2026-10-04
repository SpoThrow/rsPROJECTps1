package server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import server.game.players.Client;
import server.game.players.PlayerHandler;

/**
 * Covers the shutdown sequence itself: {@link Server#requestStop()}, which is what the JVM's
 * shutdown hook runs.
 *
 * <p>The hook is registered from {@code Server.main}, which a test cannot reasonably do — it binds
 * a socket and blocks. So the sequence was split out of the hook body precisely so the part that
 * can be wrong (stop scheduling, then write every character exactly once) is reachable from here,
 * and the untested remainder is the one line of {@code Runtime.addShutdownHook}.
 *
 * <p>No character file is written twice by design: the exactly-once guarantee lives in
 * {@code Client.saveCharacterOnce()}, so a shutdown that follows a logout — or two shutdown
 * notifications — cannot clobber the file a second time.
 */
class ServerShutdownTest {

	private static final int SLOT = 1;
	private static final String NAME = "ShutdownChar";

	private static Path characterFile() {
		return Paths.get("./Data/characters/" + NAME + ".txt");
	}

	@AfterEach
	void tearDown() throws IOException {
		PlayerHandler.players[SLOT] = null;
		Files.deleteIfExists(characterFile());
	}

	private static Client loggedInClient() {
		final Client c = new Client(null, SLOT);
		c.playerName = NAME;
		c.playerName2 = NAME;
		c.playerPass = "swordfish";
		c.saveFile = true;
		c.saveCharacter = true;
		c.newPlayer = false;
		c.isActive = true;
		PlayerHandler.players[SLOT] = c; // saveGame refuses a client that is not logged in
		return c;
	}

	@Test
	void theStopSequenceSavesEveryLoggedInCharacterExactlyOnce() {
		final Client player = loggedInClient();

		Server.requestStop();

		assertTrue(Server.shutdownServer, "the ticker must be told to stop scheduling further ticks");
		assertTrue(player.isCharacterSaved(), "a logged-in character must be written on shutdown");
		assertTrue(Files.exists(characterFile()), "the character file must be on disk");

		assertEquals(0, PlayerHandler.saveAllPlayers(),
				"a character already written must never be written again");
	}

	@Test
	void stoppingWithNobodyLoggedInIsHarmless() {
		Server.requestStop();

		assertTrue(Server.shutdownServer);
	}

	@Test
	void stoppingTwiceIsSafe() {
		// The JVM calls the hook once, but a manual stop followed by the hook (or two hooks) must
		// not throw, and must still leave exactly one copy of the character on disk.
		final Client player = loggedInClient();

		Server.requestStop();
		Server.requestStop();

		assertTrue(player.isCharacterSaved());
		assertTrue(Files.exists(characterFile()));
	}
}
