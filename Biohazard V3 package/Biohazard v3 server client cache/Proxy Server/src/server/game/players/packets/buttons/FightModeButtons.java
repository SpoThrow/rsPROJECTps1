package server.game.players.packets.buttons;

import server.game.players.Client;

/**
 * The combat-style (fight-mode) buttons, migrated out of the switch in
 * {@code ClickingButtons}.
 *
 * <p>Each row is {@code {buttonId, fightMode}}. {@code fightMode} is the value the
 * combat code reads to decide attack style; the four groups are accurate (0),
 * defensive (1), controlled (3) and aggressive (2), and the ids in each group belong
 * to different weapons, so the mode cannot be derived from the id. Every body also
 * clears a remembered autocast, because changing style invalidates it.
 */
public final class FightModeButtons {

	private static final int[][] MODES = {
			{ 9125, 0 },
			{ 6221, 0 },
			{ 22230, 0 },
			{ 48010, 0 },
			{ 21200, 0 },
			{ 1080, 0 },
			{ 6168, 0 },
			{ 6236, 0 },
			{ 17102, 0 },
			{ 8234, 0 },
			{ 9126, 1 },
			{ 48008, 1 },
			{ 22228, 1 },
			{ 21201, 1 },
			{ 1078, 1 },
			{ 6169, 1 },
			{ 33019, 1 },
			{ 18078, 1 },
			{ 8235, 1 },
			{ 9127, 3 },
			{ 48009, 3 },
			{ 33018, 3 },
			{ 6234, 3 },
			{ 6219, 3 },
			{ 18077, 3 },
			{ 18080, 3 },
			{ 18079, 3 },
			{ 17100, 3 },
			{ 9128, 2 },
			{ 6220, 2 },
			{ 22229, 2 },
			{ 21203, 2 },
			{ 21202, 2 },
			{ 1079, 2 },
			{ 6171, 2 },
			{ 6170, 2 },
			{ 33020, 2 },
			{ 6235, 2 },
			{ 17101, 2 },
			{ 8237, 2 },
			{ 8236, 2 },
	};

	private FightModeButtons() {
	}

	static void register() {
		for (int[] row : MODES) {
			final int mode = row[1];
			ButtonHandler.register(row[0], (c, actionButtonId) -> {
				c.fightMode = mode;
				if (c.autocasting) {
					c.getPA().resetAutocast();
				}
			});
		}
	}

	// Package-private for FightModeButtonsTest, which pins the transcription.
	static int[][] modes() {
		int[][] copy = new int[MODES.length][];
		for (int i = 0; i < MODES.length; i++) {
			copy[i] = MODES[i].clone();
		}
		return copy;
	}
}
