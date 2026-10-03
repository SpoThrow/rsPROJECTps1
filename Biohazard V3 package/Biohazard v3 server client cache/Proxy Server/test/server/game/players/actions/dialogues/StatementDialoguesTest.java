package server.game.players.actions.dialogues;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StatementDialoguesTest {

	@Test
	void ItemChat1NextChatHasExpectedShape() {
		final Object[][] rows = StatementDialogues.ItemChat1NextChat();
		assertEquals(1, rows.length);
		for (Object[] row : rows) {
			assertEquals(6, row.length);
		}
	}

	@Test
	void Statement1DialogueActionNextChatHasExpectedShape() {
		final Object[][] rows = StatementDialogues.Statement1DialogueActionNextChat();
		assertEquals(1, rows.length);
		for (Object[] row : rows) {
			assertEquals(4, row.length);
		}
	}

	@Test
	void Statement1NextChatHasExpectedShape() {
		final Object[][] rows = StatementDialogues.Statement1NextChat();
		assertEquals(5, rows.length);
		for (Object[] row : rows) {
			assertEquals(3, row.length);
		}
	}

	@Test
	void Statement2NextChatHasExpectedShape() {
		final Object[][] rows = StatementDialogues.Statement2NextChat();
		assertEquals(1, rows.length);
		for (Object[] row : rows) {
			assertEquals(4, row.length);
		}
	}

	@Test
	void Statement4NextChatHasExpectedShape() {
		final Object[][] rows = StatementDialogues.Statement4NextChat();
		assertEquals(1, rows.length);
		for (Object[] row : rows) {
			assertEquals(6, row.length);
		}
	}
}
