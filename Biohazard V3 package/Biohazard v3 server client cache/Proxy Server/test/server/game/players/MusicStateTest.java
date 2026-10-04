package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

import server.content.music.MusicTab;

/**
 * Pins the music-unlock state after it moved off {@code Music}'s shared static array into
 * {@link MusicState} ({@code player.music}).
 *
 * <p>The bug was that every player shared one array: one player unlocking a song lit it up in
 * everyone else's music tab, and each save wrote that same shared array back. These tests pin
 * the two things the fix has to guarantee — isolation between players, and the 384-slot index
 * space the song table uses.
 */
class MusicStateTest {

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void aFreshPlayerHasEverySongLocked() {
		final MusicState m = new MusicState();
		assertEquals(384, m.unlocked.length, "one slot per song index the song table can use");
		for (int i = 0; i < m.unlocked.length; i++) {
			assertFalse(m.unlocked[i], "song " + i + " must start locked");
		}
	}

	@Test
	void eachPlayerOwnsItsOwnUnlockArray() {
		final Client a = client();
		final Client b = client();

		assertNotSame(a.music.unlocked, b.music.unlocked,
				"two players must not share the unlock array");

		a.music.unlocked[3] = true;
		assertFalse(b.music.unlocked[3], "an unlock leaked into another player's tab");
	}

	@Test
	void aNewPlayerStartsWithOnlyTheClickableSongsUnlocked() {
		final Client c = client();
		MusicTab.initializeMusicBooleanFirstTime(c);
		for (int i = 0; i < c.music.unlocked.length; i++) {
			assertEquals(i > 350, c.music.unlocked[i],
					"slot " + i + " should be unlocked only above the region-music range");
		}
	}
}
