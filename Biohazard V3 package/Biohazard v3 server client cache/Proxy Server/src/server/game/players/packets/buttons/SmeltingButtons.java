package server.game.players.packets.buttons;

import server.content.skills.Smelting;

/**
 * The furnace interface's "smelt N bars" buttons, migrated out of the switch in
 * {@code ClickingButtons}.
 *
 * <p>It is a strict grid: {@code BUTTONS[bar][amount]}, where bar 0..7 is
 * bronze/iron/silver/steel/gold/mithril/adamant/rune and amount 0..3 is 1/5/10/28.
 * The ids are irregular -- mithril 10 is 24253 and mithril 28 is 16062, not the
 * 290xx the rest of that row uses -- so the table is data, not something that can be
 * computed.
 */
public final class SmeltingButtons {

	// Package-private so SmeltingButtonsTest can pin the transcription against the
	// switch it replaced. This table is the whole migration; a typo in it makes a
	// player smelt the wrong metal.
	static final int[][] BUTTONS = {
			{ 15147, 15146, 10247, 9110 },   // bronze
			{ 15151, 15150, 15149, 15148 },  // iron
			{ 15155, 15154, 15153, 15152 },  // silver
			{ 15159, 15158, 15157, 15156 },  // steel
			{ 15163, 15162, 15161, 15160 },  // gold
			{ 29017, 29016, 24253, 16062 },  // mithril
			{ 29022, 29021, 29019, 29018 },  // adamant
			{ 29026, 29025, 29024, 29023 },  // rune
	};

	private SmeltingButtons() {
	}

	static void register() {
		for (int bar = 0; bar < BUTTONS.length; bar++) {
			for (int amount = 0; amount < BUTTONS[bar].length; amount++) {
				// Loop variables are not effectively final, so the lambda needs copies.
				final int barType = bar;
				final int amountIndex = amount;
				ButtonHandler.register(BUTTONS[bar][amount],
						(c, actionButtonId) -> Smelting.startSmelting(c, actionButtonId, barType, amountIndex));
			}
		}
	}
}
