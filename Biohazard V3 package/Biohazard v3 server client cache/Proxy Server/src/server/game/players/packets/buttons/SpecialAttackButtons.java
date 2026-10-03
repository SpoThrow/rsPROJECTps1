package server.game.players.packets.buttons;

import server.game.players.Client;

/**
 * The weapon "special attack" toggle buttons, migrated out of the switch in
 * {@code ClickingButtons}.
 *
 * <p>Each entry is {@code {buttonId, specBarId}}: toggling flips
 * {@code usingSpecial} and redraws the special-attack bar at the weapon's own frame
 * id, which is why the two numbers are unrelated and cannot be derived from one
 * another.
 *
 * <p>Only the buttons with this exact toggle body are here. {@code 29038} (granite
 * maul) and {@code 29063} share the surrounding section but have their own bodies and
 * stay in the switch.
 */
public final class SpecialAttackButtons {

	private static final int[][] BUTTONS = {
			{ 29188, 7636 },
			{ 29163, 7611 },
			{ 33033, 8505 },
			{ 48023, 12335 },
			{ 30108, 7812 },
			{ 29138, 7586 },
			{ 29113, 7561 },
			{ 29238, 7686 },
	};

	private SpecialAttackButtons() {
	}

	static void register() {
		for (int[] entry : BUTTONS) {
			final int specBarId = entry[1];
			ButtonHandler.register(entry[0], (c, actionButtonId) -> {
				c.specBarId = specBarId;
				c.usingSpecial = !c.usingSpecial;
				c.getItems().updateSpecialBar();
			});
		}
	}

	// Package-private for SpecialAttackButtonsTest, which pins the table.
	static int[][] buttons() {
		int[][] copy = new int[BUTTONS.length][];
		for (int i = 0; i < BUTTONS.length; i++) {
			copy[i] = BUTTONS[i].clone();
		}
		return copy;
	}
}
