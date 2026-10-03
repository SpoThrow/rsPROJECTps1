package server.game.players.packets.buttons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Pins the special-attack toggle table.
 *
 * <p>Each row is {@code {buttonId, specBarId}} and the two numbers are unrelated --
 * the spec bar id is the weapon's own frame id -- so a mis-transcription is exactly
 * the kind of thing that compiles and runs but redraws the wrong bar.
 */
class SpecialAttackButtonsTest {

	private static final int[][] EXPECTED = {
			{ 29188, 7636 },
			{ 29163, 7611 },
			{ 33033, 8505 },
			{ 48023, 12335 },
			{ 30108, 7812 },
			{ 29138, 7586 },
			{ 29113, 7561 },
			{ 29238, 7686 },
	};

	@Test
	void tableMatchesTheSwitchItReplaced() {
		int[][] actual = SpecialAttackButtons.buttons();
		assertEquals(EXPECTED.length, actual.length);
		for (int i = 0; i < EXPECTED.length; i++) {
			assertEquals(EXPECTED[i][0], actual[i][0], "row " + i + " button id");
			assertEquals(EXPECTED[i][1], actual[i][1], "row " + i + " spec bar id");
		}
	}

	@Test
	void everySpecialButtonIsRegistered() {
		for (int[] entry : EXPECTED) {
			assertTrue(ButtonHandler.isRegistered(entry[0]),
					"special attack button " + entry[0] + " has no handler");
		}
	}

	@Test
	void buttonsAreNotSharedBetweenWeapons() {
		Set<Integer> seen = new HashSet<>();
		for (int[] entry : EXPECTED) {
			assertTrue(seen.add(entry[0]), "button " + entry[0] + " appears twice");
		}
	}

	@Test
	void theGraniteMaulButtonIsNotInThisFamily() {
		// 29038 also sets specBarId, but its body calls handleGmaulPlayer() instead of
		// toggling, so it stays in the switch. It must not be claimed here.
		assertEquals(false, ButtonHandler.isRegistered(29038));
		assertEquals(false, ButtonHandler.isRegistered(29063));
	}
}
