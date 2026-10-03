package server.game.players.actions.dialogues;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChatDialoguesTest {

	@Test
	void NpcChat1NextChatHasExpectedShape() {
		final Object[][] rows = ChatDialogues.NpcChat1NextChat();
		assertEquals(42, rows.length);
		for (Object[] row : rows) {
			assertEquals(5, row.length);
		}
	}

	@Test
	void NpcChat2NextChatHasExpectedShape() {
		final Object[][] rows = ChatDialogues.NpcChat2NextChat();
		assertEquals(43, rows.length);
		for (Object[] row : rows) {
			assertEquals(6, row.length);
		}
	}

	@Test
	void NpcChat3NextChatHasExpectedShape() {
		final Object[][] rows = ChatDialogues.NpcChat3NextChat();
		assertEquals(5, rows.length);
		for (Object[] row : rows) {
			assertEquals(7, row.length);
		}
	}

	@Test
	void NpcChat4NextChatHasExpectedShape() {
		final Object[][] rows = ChatDialogues.NpcChat4NextChat();
		assertEquals(5, rows.length);
		for (Object[] row : rows) {
			assertEquals(8, row.length);
		}
	}

	@Test
	void PlayerChat1NextChatHasExpectedShape() {
		final Object[][] rows = ChatDialogues.PlayerChat1NextChat();
		assertEquals(21, rows.length);
		for (Object[] row : rows) {
			assertEquals(3, row.length);
		}
	}

	@Test
	void PlayerChat2NextChatHasExpectedShape() {
		final Object[][] rows = ChatDialogues.PlayerChat2NextChat();
		assertEquals(1, rows.length);
		for (Object[] row : rows) {
			assertEquals(4, row.length);
		}
	}

	@Test
	void PlayerChat3NextChatHasExpectedShape() {
		final Object[][] rows = ChatDialogues.PlayerChat3NextChat();
		assertEquals(1, rows.length);
		for (Object[] row : rows) {
			assertEquals(5, row.length);
		}
	}
}
