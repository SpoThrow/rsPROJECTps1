package server.game.players.packets.buttons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Pins the duel-rule toggle table.
 *
 * <p>The row is {@code {buttonId, duelSlot, rule}}. The slot is the half that is easy
 * to get wrong: the first eleven rules use {@code -1} and the equipment rules use a
 * real index, and the switch wrote the minus sign literally. A migration that loses it
 * still compiles and still toggles a rule, just the wrong slot.
 */
class DuelRuleButtonsTest {

	private static final int[][] EXPECTED = {
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

	@Test
	void tableMatchesTheSwitchItReplaced() {
		assertEquals(EXPECTED.length, DuelRuleButtons.rules().length);
		for (int i = 0; i < EXPECTED.length; i++) {
			int[] actual = DuelRuleButtons.rules()[i];
			assertEquals(EXPECTED[i][0], actual[0], "row " + i + " button id");
			assertEquals(EXPECTED[i][1], actual[1], "row " + i + " duel slot");
			assertEquals(EXPECTED[i][2], actual[2], "row " + i + " rule");
		}
	}

	@Test
	void everyRuleIsRepresentedExactlyOnce() {
		// The switch defines rules 0..21; if two rows collapsed onto one rule a pair
		// of buttons would toggle the same rule.
		Set<Integer> rules = new HashSet<>();
		for (int[] row : DuelRuleButtons.rules()) rules.add(row[2]);
		assertEquals(22, rules.size(), "rules 0..21, one group per rule");
	}

	@Test
	void slotIsMinusOneForTheFirstElevenRulesAndRealForTheRest() {
		for (int[] row : DuelRuleButtons.rules()) {
			if (row[2] <= 10) assertEquals(-1, row[1], "rule " + row[2] + " is slot-independent");
			else assertTrue(row[1] >= 0, "rule " + row[2] + " targets an equipment slot");
		}
	}

	@Test
	void everyDuelRuleButtonIsRegistered() {
		for (int[] row : EXPECTED) assertTrue(ButtonHandler.isRegistered(row[0]),
				"duel rule button " + row[0] + " has no handler");
	}

	@Test
	void noButtonIsShared() {
		Set<Integer> seen = new HashSet<>();
		for (int[] row : EXPECTED) assertTrue(seen.add(row[0]), "button " + row[0] + " appears twice");
	}
}
