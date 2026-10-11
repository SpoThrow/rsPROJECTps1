package server.game.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import core.util.ISAACRandomGen;
import server.game.players.Client;

/**
 * Pins the bank search box: it filters as the player types, the box stays open until it is closed,
 * and closing the bank takes the search with it.
 *
 * <p><b>Why the per-keystroke calls are the interesting part.</b> The box used to send nothing until
 * Enter, so every call was a complete search. Now every letter arrives, and the state has to be
 * "there is a search running" rather than "this call is a search" - otherwise each letter would
 * clear the previous filter before applying itself, and the announcement would be printed once per
 * character. The two are separate pieces of state on purpose: {@code bankSearching} is the filter,
 * {@code awaitingBankSearch} is the open box, and an emptied term ends the first without ending the
 * second.
 */
class BankSearchTest {

	private static Client client() {
		Client c = new Client(null, 2);
		c.getOutStream().packetEncryption = new ISAACRandomGen(new int[] { 1, 2, 3, 4 });
		return c;
	}

	// ---------------------------------------------------------------------------------------
	// Filtering as the player types
	// ---------------------------------------------------------------------------------------

	@Test
	void typingStartsASearchAndRemembersTheTerm() {
		Client c = client();

		c.getBank().applySearch("drag");

		assertTrue(c.bankSearching, "there is a filter");
		assertEquals("drag", c.bankSearch);
	}

	@Test
	void everyFurtherKeystrokeKeepsTheSameSearchRatherThanRestartingIt() {
		// The announcement is once per search, and the search is the state rather than the call.
		// If a keystroke cleared and restarted it, the player would be told four times a word and
		// the bank would be re-sent for each letter.
		Client c = client();

		c.getBank().applySearch("d");
		boolean afterFirst = c.bankSearching;
		c.getBank().applySearch("dr");
		c.getBank().applySearch("dra");

		assertTrue(afterFirst);
		assertTrue(c.bankSearching, "still one search, four calls in");
		assertEquals("dra", c.bankSearch, "and the term is the last thing typed");
	}

	@Test
	void anEmptiedTermDropsTheFilterButLeavesTheBoxOpen() {
		// Backspacing the whole term must not end the search: the next letter typed is the next
		// search term, and routing it as ordinary input would have the bank treat "r" as a clan
		// name. Only a deliberate close reads as not searching.
		Client c = client();
		c.awaitingBankSearch = true;
		c.getBank().applySearch("rune");

		c.getBank().applySearch("");

		assertFalse(c.bankSearching, "there is no term, so there is no filter");
		assertEquals("", c.bankSearch);
		assertTrue(c.awaitingBankSearch, "but the box is still open");
	}

	// ---------------------------------------------------------------------------------------
	// Closing
	// ---------------------------------------------------------------------------------------

	@Test
	void closingEndsBothTheFilterAndTheBox() {
		// What a bank close needs: the next open starts from a clean slate rather than from the
		// last search's filter with an empty box.
		Client c = client();
		c.awaitingBankSearch = true;
		c.getBank().applySearch("rune");

		c.getBank().clearSearch();

		assertFalse(c.bankSearching);
		assertFalse(c.awaitingBankSearch, "the box itself is closed too, not just the filter");
		assertEquals("", c.bankSearch);
	}

	@Test
	void clearingWithNoSearchIsSafeAndRepeated() {
		// resetVariables and removeAllWindows call this unconditionally, so it has to survive being
		// called on every walk and on a bank that was never searched. isBanking is false here, so a
		// refresh would still be the whole bank's frames; the early return is what skips it.
		Client c = client();

		c.getBank().clearSearch();
		c.getBank().clearSearch();

		assertFalse(c.bankSearching);
		assertFalse(c.awaitingBankSearch);
		assertEquals("", c.bankSearch);
	}

	@Test
	void openingATabLeavesTheSearchBehind() {
		// The tabs and the search box are two ways to say what the bank is showing, and a tab click
		// is a deliberate "show me this tab" rather than a way to narrow a search.
		Client c = client();
		c.getBank().applySearch("rune");

		c.getBank().openTab(0);

		assertFalse(c.bankSearching);
		assertFalse(c.awaitingBankSearch);
		assertEquals("", c.bankSearch);
	}

	@Test
	void openingTheBankStartsWithNoSearch() {
		// openUpBank calls resetVariables, which calls clearSearch. Asserted through the same door
		// the game uses so a reset that stopped clearing the search is caught here.
		Client c = client();
		c.awaitingBankSearch = true;
		c.getBank().applySearch("rune");

		c.getPA().resetVariables();

		assertFalse(c.bankSearching, "a walk or a fresh open starts with no filter");
		assertFalse(c.awaitingBankSearch);
		assertEquals("", c.bankSearch);
	}
}
