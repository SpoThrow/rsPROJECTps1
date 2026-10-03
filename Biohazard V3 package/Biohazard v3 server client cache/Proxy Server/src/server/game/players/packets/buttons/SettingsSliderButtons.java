package server.game.players.packets.buttons;

import server.game.players.Client;

/**
 * The settings-interface sliders, migrated out of the switch in {@code ClickingButtons}.
 *
 * <p>Three near-identical bodies, one per slider: brightness writes frame 166, music
 * volume frame 168 and sound-effect volume frame 169. The music slider additionally
 * carries the enabled flag, because dragging it to its lowest position ({@code 930})
 * turns music off rather than merely setting volume zero.
 *
 * <p>The frame ids and the volume constants are unrelated -- both are carried in the
 * tables, because a mis-transcription compiles and looks like a working slider that
 * writes the wrong frame.
 */
public final class SettingsSliderButtons {

	private static final int BRIGHTNESS_FRAME = 166;
	private static final int MUSIC_FRAME = 168;
	private static final int SOUND_FRAME = 169;

	private static final int[][] BRIGHTNESS = {
			{ 906, 1 },
			{ 908, 2 },
			{ 910, 3 },
			{ 912, 4 },
	};

	private static final int[][] MUSIC = {
			{ 930, 4, 0 },
			{ 931, 3, 1 },
			{ 932, 2, 1 },
			{ 933, 1, 1 },
			{ 934, 0, 1 },
	};

	private static final int[][] SOUND = {
			{ 941, 4 },
			{ 942, 3 },
			{ 943, 2 },
			{ 944, 1 },
			{ 945, 0 },
	};

	private SettingsSliderButtons() {
	}

	static void register() {
		for (int[] row : BRIGHTNESS) {
			final int level = row[1];
			ButtonHandler.register(row[0], (c, actionButtonId) -> {
				c.settings.brightness = level;
				c.getPA().sendFrame36(BRIGHTNESS_FRAME, level);
			});
		}
		for (int[] row : MUSIC) {
			final int volume = row[1];
			final boolean enabled = row[2] != 0;
			ButtonHandler.register(row[0], (c, actionButtonId) -> {
				c.settings.musicVolume = volume;
				c.settings.musicEnabled = enabled;
				c.getPA().sendFrame36(MUSIC_FRAME, volume);
			});
		}
		for (int[] row : SOUND) {
			final int volume = row[1];
			ButtonHandler.register(row[0], (c, actionButtonId) -> {
				c.settings.soundEffectVolume = volume;
				c.getPA().sendFrame36(SOUND_FRAME, volume);
			});
		}
	}

	// Package-private for SettingsSliderButtonsTest, which pins the transcriptions.
	static int[][] brightness() { return copy(BRIGHTNESS); }
	static int[][] music() { return copy(MUSIC); }
	static int[][] sound() { return copy(SOUND); }

	private static int[][] copy(int[][] table) {
		int[][] result = new int[table.length][];
		for (int i = 0; i < table.length; i++) {
			result[i] = table[i].clone();
		}
		return result;
	}
}
