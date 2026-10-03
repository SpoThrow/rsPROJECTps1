package server.game.players.packets.buttons;

import server.game.players.Client;

/**
 * The duel-arena rule toggles, migrated out of the switch in {@code ClickingButtons}.
 *
 * <p>Each row is {@code {buttonId, duelSlot, rule}}: the button sets the slot the
 * pending rule applies to, then asks {@code TradeAndDuel.selectRule} to toggle the
 * rule itself. The slot is {@code -1} for the eleven rules that are not tied to an
 * equipment slot and a real equipment index for the rest, which is why it is carried
 * in the table rather than derived from the rule number.
 *
 * <p>The two halves of the table read {@code c.duelSlot = -1} and {@code c.duelSlot =
 * <index>} in the switch; the minus sign is easy to lose in a hand migration, which is
 * why the slot is pinned by {@code DuelRuleButtonsTest} rather than computed.
 */
public final class DuelRuleButtons {

	private static final int[][] RULES = {
			{ 26065, -1, 0 },
			{ 26040, -1, 0 },
			{ 26066, -1, 1 },
			{ 26048, -1, 1 },
			{ 26069, -1, 2 },
			{ 26042, -1, 2 },
			{ 26070, -1, 3 },
			{ 26043, -1, 3 },
			{ 26071, -1, 4 },
			{ 26041, -1, 4 },
			{ 26072, -1, 5 },
			{ 26045, -1, 5 },
			{ 26073, -1, 6 },
			{ 26046, -1, 6 },
			{ 26074, -1, 7 },
			{ 26047, -1, 7 },
			{ 26076, -1, 8 },
			{ 26075, -1, 8 },
			{ 2158, -1, 9 },
			{ 2157, -1, 9 },
			{ 30136, -1, 10 },
			{ 30137, -1, 10 },
			{ 53245, 0, 11 },
			{ 53246, 1, 12 },
			{ 53247, 2, 13 },
			{ 53249, 3, 14 },
			{ 53250, 4, 15 },
			{ 53251, 5, 16 },
			{ 53252, 7, 17 },
			{ 53255, 9, 18 },
			{ 53254, 10, 19 },
			{ 53253, 12, 20 },
			{ 53248, 13, 21 },
	};

	private DuelRuleButtons() {
	}

	static void register() {
		for (int[] row : RULES) {
			final int slot = row[1];
			final int rule = row[2];
			ButtonHandler.register(row[0], (c, actionButtonId) -> {
				c.duelSlot = slot;
				c.getTradeAndDuel().selectRule(rule);
			});
		}
	}

	// Package-private for DuelRuleButtonsTest, which pins the transcription.
	static int[][] rules() {
		int[][] copy = new int[RULES.length][];
		for (int i = 0; i < RULES.length; i++) {
			copy[i] = RULES[i].clone();
		}
		return copy;
	}
}
