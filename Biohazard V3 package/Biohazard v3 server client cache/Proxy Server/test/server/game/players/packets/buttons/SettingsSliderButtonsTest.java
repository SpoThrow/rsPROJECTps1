package server.game.players.packets.buttons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import server.game.players.ClientSettings;

/**
 * Pins the settings-slider tables.
 *
 * <p>Brightness rows are {@code {buttonId, level}}, music rows
 * {@code {buttonId, volume, enabled}} and sound rows {@code {buttonId, volume}}. The
 * music flag is called out because button 930 is the only one that turns music off.
 */
class SettingsSliderButtonsTest {

	private static final int[][] EXPECTED_BRIGHTNESS = {
			{ 906, 1 },
			{ 908, 2 },
			{ 910, 3 },
			{ 912, 4 },
	};

	private static final int[][] EXPECTED_MUSIC = {
			{ 930, 4, 0 },
			{ 931, 3, 1 },
			{ 932, 2, 1 },
			{ 933, 1, 1 },
			{ 934, 0, 1 },
	};

	private static final int[][] EXPECTED_SOUND = {
			{ 941, 4 },
			{ 942, 3 },
			{ 943, 2 },
			{ 944, 1 },
			{ 945, 0 },
	};

	@Test
	void brightnessTableMatchesTheSwitch() {
		int[][] actual = SettingsSliderButtons.brightness();
		assertEquals(EXPECTED_BRIGHTNESS.length, actual.length);
		for (int i = 0; i < EXPECTED_BRIGHTNESS.length; i++) {
			assertEquals(EXPECTED_BRIGHTNESS[i][0], actual[i][0], "row " + i + " button");
			assertEquals(EXPECTED_BRIGHTNESS[i][1], actual[i][1], "row " + i + " level");
		}
	}

	@Test
	void musicTableMatchesTheSwitch() {
		int[][] actual = SettingsSliderButtons.music();
		assertEquals(EXPECTED_MUSIC.length, actual.length);
		for (int i = 0; i < EXPECTED_MUSIC.length; i++) {
			assertEquals(EXPECTED_MUSIC[i][0], actual[i][0], "row " + i + " button");
			assertEquals(EXPECTED_MUSIC[i][1], actual[i][1], "row " + i + " volume");
			assertEquals(EXPECTED_MUSIC[i][2], actual[i][2], "row " + i + " enabled");
		}
	}

	@Test
	void soundTableMatchesTheSwitch() {
		int[][] actual = SettingsSliderButtons.sound();
		assertEquals(EXPECTED_SOUND.length, actual.length);
		for (int i = 0; i < EXPECTED_SOUND.length; i++) {
			assertEquals(EXPECTED_SOUND[i][0], actual[i][0], "row " + i + " button");
			assertEquals(EXPECTED_SOUND[i][1], actual[i][1], "row " + i + " volume");
		}
	}

	@Test
	void onlyTheLowestMusicButtonDisablesMusic() {
		// 930 sets musicEnabled = false; every other music button leaves it on.
		int disabled = 0;
		for (int[] row : SettingsSliderButtons.music()) if (row[2] == 0) disabled++;
		assertEquals(1, disabled, "exactly one music button turns music off");
	}

	@Test
	void everySliderIsRegistered() {
		for (int[][] table : new int[][][] { EXPECTED_BRIGHTNESS, EXPECTED_MUSIC, EXPECTED_SOUND }) {
			for (int[] row : table) assertTrue(ButtonHandler.isRegistered(row[0]),
					"slider button " + row[0] + " has no handler");
		}
	}

	@Test
	void noButtonIsShared() {
		Set<Integer> seen = new HashSet<>();
		for (int[][] table : new int[][][] { EXPECTED_BRIGHTNESS, EXPECTED_MUSIC, EXPECTED_SOUND }) {
			for (int[] row : table) assertTrue(seen.add(row[0]), "button " + row[0] + " appears twice");
		}
	}

	@Test
	void everyClusterDefaultIsReachableFromASlider() {
		// Added after Phase 4.4 moved these fields into ClientSettings. A default that no
		// slider position can produce (say brightness 0, or musicVolume 9) is unreachable
		// state: the player can never set it back, and the client is sent a value its
		// slider cannot display. The sliders are the only writer, so they define the
		// valid domain -- this pins the freshly-moved defaults against it.
		final ClientSettings defaults = new ClientSettings();

		Set<Integer> brightnessLevels = new HashSet<>();
		for (int[] row : SettingsSliderButtons.brightness()) brightnessLevels.add(row[1]);
		assertTrue(brightnessLevels.contains(defaults.brightness),
				"default brightness " + defaults.brightness + " is not a slider position " + brightnessLevels);

		Set<Integer> musicVolumes = new HashSet<>();
		for (int[] row : SettingsSliderButtons.music()) musicVolumes.add(row[1]);
		assertTrue(musicVolumes.contains(defaults.musicVolume),
				"default musicVolume " + defaults.musicVolume + " is not a slider position " + musicVolumes);

		Set<Integer> soundVolumes = new HashSet<>();
		for (int[] row : SettingsSliderButtons.sound()) soundVolumes.add(row[1]);
		assertTrue(soundVolumes.contains(defaults.soundEffectVolume),
				"default soundEffectVolume " + defaults.soundEffectVolume + " is not a slider position " + soundVolumes);
	}
}
