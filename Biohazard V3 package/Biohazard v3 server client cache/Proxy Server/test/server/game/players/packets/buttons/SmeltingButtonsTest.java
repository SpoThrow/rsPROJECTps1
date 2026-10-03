package server.game.players.packets.buttons;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the smelting button table against the switch cases it replaced.
 *
 * <p>The expected ids below were extracted mechanically from the original
 * {@code ClickingButtons} switch (each {@code case N://Metal, amount} paired with its
 * {@code startSmelting(c, actionButtonId, bar, amount)} body), not retyped from the
 * new table. The table is the entire migration: a wrong id makes a furnace button
 * dead, and a wrong index makes a player smelt the wrong metal or the wrong
 * quantity, silently.
 */
class SmeltingButtonsTest {

	private static final String[] METALS = {
			"bronze", "iron", "silver", "steel", "gold", "mithril", "adamant", "rune"
	};

	private static final int[] QUANTITIES = { 1, 5, 10, 28 };

	// [bar][amount], matching Smelting.startSmelting's (i1, i2) parameters.
	private static final int[][] EXPECTED = {
			{ 15147, 15146, 10247, 9110 },
			{ 15151, 15150, 15149, 15148 },
			{ 15155, 15154, 15153, 15152 },
			{ 15159, 15158, 15157, 15156 },
			{ 15163, 15162, 15161, 15160 },
			{ 29017, 29016, 24253, 16062 },
			{ 29022, 29021, 29019, 29018 },
			{ 29026, 29025, 29024, 29023 },
	};

	@Test
	void tableMatchesTheSwitchItReplaced() {
		assertEquals(EXPECTED.length, SmeltingButtons.BUTTONS.length, "bar count");
		for (int bar = 0; bar < EXPECTED.length; bar++) {
			assertArrayEquals(EXPECTED[bar], SmeltingButtons.BUTTONS[bar],
					METALS[bar] + " row");
		}
	}

	@Test
	void everyFurnaceButtonIsRegistered() {
		for (int bar = 0; bar < EXPECTED.length; bar++) {
			for (int amount = 0; amount < EXPECTED[bar].length; amount++) {
				int id = EXPECTED[bar][amount];
				assertTrue(ButtonHandler.isRegistered(id),
						METALS[bar] + " x" + QUANTITIES[amount] + " (button " + id
								+ ") must be claimed by the registry, or the switch that used to"
								+ " handle it is gone and the button is dead");
			}
		}
	}

	@Test
	void theGridIsEightMetalsByFourQuantities() {
		int total = 0;
		for (int[] row : SmeltingButtons.BUTTONS) {
			assertEquals(QUANTITIES.length, row.length);
			total += row.length;
		}
		assertEquals(32, total);
	}

	@Test
	void noButtonIdIsSharedBetweenTwoMetals() {
		// A repeated id would mean one furnace button can only ever reach one metal,
		// and register() would have thrown at class-load anyway. Belt and braces,
		// because the failure mode is otherwise invisible.
		java.util.Set<Integer> seen = new java.util.HashSet<>();
		for (int[] row : SmeltingButtons.BUTTONS) {
			for (int id : row) {
				assertTrue(seen.add(id), "button " + id + " appears twice in the table");
			}
		}
	}
}
