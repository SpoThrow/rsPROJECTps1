package server.game.players.packets.buttons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Pins the combat-style table.
 *
 * <p>Each row is {@code {buttonId, fightMode}}. The four groups cover different
 * weapons, so the mode is data, not derivable. Getting one wrong makes a weapon's
 * style button select the wrong style, which no test of the registry contract alone
 * would catch.
 */
class FightModeButtonsTest {

	private static final int[][] EXPECTED = {
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

	@Test
	void tableMatchesTheSwitchItReplaced() {
		int[][] actual = FightModeButtons.modes();
		assertEquals(EXPECTED.length, actual.length);
		for (int i = 0; i < EXPECTED.length; i++) {
			assertEquals(EXPECTED[i][0], actual[i][0], "row " + i + " button id");
			assertEquals(EXPECTED[i][1], actual[i][1], "row " + i + " fight mode");
		}
	}

	@Test
	void theFourStyleGroupsHaveTheirOriginalSizes() {
		Map<Integer, Integer> counts = new HashMap<>();
		for (int[] row : FightModeButtons.modes()) counts.merge(row[1], 1, Integer::sum);
		assertEquals(10, counts.get(0), "accurate");
		assertEquals(9, counts.get(1), "defensive");
		assertEquals(13, counts.get(2), "aggressive");
		assertEquals(9, counts.get(3), "controlled");
	}

	@Test
	void everyFightModeButtonIsRegistered() {
		for (int[] row : EXPECTED) assertTrue(ButtonHandler.isRegistered(row[0]),
				"fight mode button " + row[0] + " has no handler");
	}

	@Test
	void noButtonIsShared() {
		Set<Integer> seen = new HashSet<>();
		for (int[] row : EXPECTED) assertTrue(seen.add(row[0]), "button " + row[0] + " appears twice");
	}
}
