package server.game.bots;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Shared possession fixture for the slice-1 state tests. Not a test itself: it only builds
 * and tears down possessed bots on the throwaway test filesystem.
 */
final class BotTestFixture {

	static final String PASSWORD = "swordfish";

	private BotTestFixture() {
	}

	/** Creates and possesses a fresh account, failing loudly if either step goes wrong. */
	static BotPlayer possess(String name) {
		if (!BotManager.createAccount(name, PASSWORD)) {
			throw new IllegalStateException("could not create account " + name);
		}
		BotPlayer bot = BotManager.possess(name, PASSWORD);
		if (bot == null) {
			throw new IllegalStateException("could not possess " + name);
		}
		return bot;
	}

	/**
	 * Moves the bot instantly to a tile. This is test scaffolding only: the teleport path is
	 * how a client arrives in the world, and it is what keeps {@code currentX/currentY} in
	 * step with {@code absX/absY} so the walking queue works.
	 */
	static void teleport(BotPlayer bot, int x, int y) {
		bot.position.teleportToX = x;
		bot.position.teleportToY = y;
		bot.getNextPlayerMovement();
	}

	static void cleanUp(String... names) throws IOException {
		for (BotPlayer bot : BotManager.all()) {
			BotManager.release(bot.playerName);
		}
		for (String name : names) {
			Files.deleteIfExists(Paths.get("./Data/characters/" + name + ".txt"));
		}
	}
}
