package server.game.players.actions.dialogues;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EmoteChatDialoguesTest {

	@Test
	void NpcEmote1CalmContinuedNextChatHasExpectedShape() {
		final Object[][] rows = EmoteChatDialogues.NpcEmote1CalmContinuedNextChat();
		assertEquals(1, rows.length);
		for (Object[] row : rows) {
			assertEquals(4, row.length);
		}
	}

	@Test
	void NpcEmote1EvilNextChatHasExpectedShape() {
		final Object[][] rows = EmoteChatDialogues.NpcEmote1EvilNextChat();
		assertEquals(1, rows.length);
		for (Object[] row : rows) {
			assertEquals(4, row.length);
		}
	}

	@Test
	void NpcEmote2CalmNextChatHasExpectedShape() {
		final Object[][] rows = EmoteChatDialogues.NpcEmote2CalmNextChat();
		assertEquals(23, rows.length);
		for (Object[] row : rows) {
			assertEquals(5, row.length);
		}
	}

	@Test
	void NpcEmote2EvilNextChatHasExpectedShape() {
		final Object[][] rows = EmoteChatDialogues.NpcEmote2EvilNextChat();
		assertEquals(1, rows.length);
		for (Object[] row : rows) {
			assertEquals(5, row.length);
		}
	}

	@Test
	void NpcEmote3CalmContinuedNextChatHasExpectedShape() {
		final Object[][] rows = EmoteChatDialogues.NpcEmote3CalmContinuedNextChat();
		assertEquals(4, rows.length);
		for (Object[] row : rows) {
			assertEquals(6, row.length);
		}
	}

	@Test
	void NpcEmote4CalmContinuedNextChatHasExpectedShape() {
		final Object[][] rows = EmoteChatDialogues.NpcEmote4CalmContinuedNextChat();
		assertEquals(2, rows.length);
		for (Object[] row : rows) {
			assertEquals(7, row.length);
		}
	}

	@Test
	void PlayerEmoteCalmContinuedNextChatHasExpectedShape() {
		final Object[][] rows = EmoteChatDialogues.PlayerEmoteCalmContinuedNextChat();
		assertEquals(1, rows.length);
		for (Object[] row : rows) {
			assertEquals(3, row.length);
		}
	}

	@Test
	void PlayerEmoteCalmNextChatHasExpectedShape() {
		final Object[][] rows = EmoteChatDialogues.PlayerEmoteCalmNextChat();
		assertEquals(1, rows.length);
		for (Object[] row : rows) {
			assertEquals(3, row.length);
		}
	}

	@Test
	void PlayerEmoteHappyNextChatHasExpectedShape() {
		final Object[][] rows = EmoteChatDialogues.PlayerEmoteHappyNextChat();
		assertEquals(1, rows.length);
		for (Object[] row : rows) {
			assertEquals(3, row.length);
		}
	}
}
