package server.game.players.packets.buttons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the bank-tab id range.
 *
 * <p>The switch mapped button {@code 10324 + n} to tab {@code n}; the test
 * asserts the exact range so a stray tab button does not silently fall through to the
 * generic handler.
 */
class BankTabButtonsTest {

	@Test
	void theRangeIsTheContiguousRunInTheSwitch() {
		assertEquals(10324, BankTabButtons.firstTabButton());
		assertEquals(9, BankTabButtons.tabCount());
	}

	@Test
	void everyTabButtonIsRegistered() {
		for (int i = 0; i < BankTabButtons.tabCount(); i++) {
			assertTrue(ButtonHandler.isRegistered(BankTabButtons.firstTabButton() + i),
					"bank tab button " + (BankTabButtons.firstTabButton() + i) + " has no handler");
		}
	}

	@Test
	void theButtonsOnEitherSideAreNotClaimed() {
		assertFalse(ButtonHandler.isRegistered(BankTabButtons.firstTabButton() - 1));
		assertFalse(ButtonHandler.isRegistered(BankTabButtons.firstTabButton() + BankTabButtons.tabCount()));
	}
}
