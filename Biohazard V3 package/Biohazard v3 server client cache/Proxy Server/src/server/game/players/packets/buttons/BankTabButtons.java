package server.game.players.packets.buttons;

import server.game.players.Client;

/**
 * The bank tab buttons, migrated out of the switch in {@code ClickingButtons}.
 *
 * <p>The switch wrote tab {@code 0} as a literal {@code openTab(0)} and every other
 * tab as {@code openTab(actionButtonId - 10324)}, so the whole family is one
 * contiguous id range. The handler keeps the subtraction rather than baking the index
 * in, so the mapping stays visible here.
 */
public final class BankTabButtons {

	private static final int FIRST_TAB_BUTTON = 10324;
	private static final int TAB_COUNT = 9;

	private BankTabButtons() {
	}

	static void register() {
		for (int tab = 0; tab < TAB_COUNT; tab++) {
			ButtonHandler.register(FIRST_TAB_BUTTON + tab,
					(c, actionButtonId) -> c.getBank().openTab(actionButtonId - FIRST_TAB_BUTTON));
		}
	}

	// Package-private for BankTabButtonsTest, which pins the range.
	static int firstTabButton() { return FIRST_TAB_BUTTON; }
	static int tabCount() { return TAB_COUNT; }
}
