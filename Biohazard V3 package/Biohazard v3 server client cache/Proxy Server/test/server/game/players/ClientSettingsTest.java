package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the client sound/display cluster after Phase 4.4 moved its six fields off
 * {@link Player} into {@link ClientSettings}.
 *
 * <p>The defaults are the load-bearing part. {@code Client.initialize} pushes
 * {@code settings.brightness}, {@code settings.musicVolume} and
 * {@code settings.soundEffectVolume} into client config frames 166, 168 and 169 as soon as
 * a player logs in, and those three are <em>not</em> restored for a character that has
 * never been saved -- so these initialisers are literally what a brand-new player's client
 * is told to display. Getting one wrong is a visible bug on the login screen, not an
 * invisible one.
 */
class ClientSettingsTest {

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void everyFieldKeepsItsOldDefault() {
		final ClientSettings settings = new ClientSettings();

		assertTrue(settings.isLoopingMusic, "isLoopingMusic was initialised to true");
		assertEquals(1, settings.auto, "auto was initialised to 1 (AUTO)");
		assertEquals(0, settings.musicVolume, "musicVolume was initialised to 0");
		assertEquals(0, settings.soundEffectVolume, "soundEffectVolume was initialised to 0");
		assertTrue(settings.musicEnabled, "musicEnabled was initialised to true");
		assertEquals(3, settings.brightness, "brightness was initialised to 3");
	}

	@Test
	void aFreshPlayerDoesNotStartWithMusicSuppressed() {
		// Music.playMusic holds `if (c.settings.auto == 0) return;`, so a default of 0 here
		// would silently deny a brand-new character every music track in the game while
		// looking like an ordinary settings field. Only the non-zero default is safe.
		assertTrue(client().settings.auto != 0,
				"auto == 0 disables music entirely; a fresh player must not start disabled");
	}

	@Test
	void aFreshPlayerStartsWithMusicOn() {
		// musicEnabled is the stored counterpart to the music slider's lowest position.
		// Defaulting to false would leave the client showing a volume the server treats as
		// muted.
		assertTrue(client().settings.musicEnabled);
	}

	@Test
	void eachPlayerOwnsItsOwnSettings() {
		final Client a = client();
		final Client b = client();

		assertSame(a.settings, a.settings, "the same player must keep one settings object");
		assertNotSame(a.settings, b.settings, "two players must not share settings");

		a.settings.brightness = 1;
		a.settings.musicVolume = 4;
		a.settings.soundEffectVolume = 4;
		a.settings.musicEnabled = false;
		a.settings.isLoopingMusic = false;
		a.settings.auto = 0;

		assertEquals(3, b.settings.brightness, "settings leaked between players");
		assertEquals(0, b.settings.musicVolume, "settings leaked between players");
		assertEquals(0, b.settings.soundEffectVolume, "settings leaked between players");
		assertTrue(b.settings.musicEnabled, "settings leaked between players");
		assertTrue(b.settings.isLoopingMusic, "settings leaked between players");
		assertEquals(1, b.settings.auto, "settings leaked between players");
	}
}
