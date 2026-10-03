package server.game.players.actions.dialogues;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OptionDialoguesTest {

	@Test
	void Option2DialogueActionHasExpectedShape() {
		final Object[][] rows = OptionDialogues.Option2DialogueAction();
		assertEquals(14, rows.length);
		for (Object[] row : rows) {
			assertEquals(4, row.length);
		}
	}

	@Test
	void Option2DialogueActionDialogueIdHasExpectedShape() {
		final Object[][] rows = OptionDialogues.Option2DialogueActionDialogueId();
		assertEquals(11, rows.length);
		for (Object[] row : rows) {
			assertEquals(5, row.length);
		}
	}

	@Test
	void Option2DialogueActionNextChatHasExpectedShape() {
		final Object[][] rows = OptionDialogues.Option2DialogueActionNextChat();
		assertEquals(1, rows.length);
		for (Object[] row : rows) {
			assertEquals(5, row.length);
		}
	}

	@Test
	void Option3DialogueActionHasExpectedShape() {
		final Object[][] rows = OptionDialogues.Option3DialogueAction();
		assertEquals(23, rows.length);
		for (Object[] row : rows) {
			assertEquals(5, row.length);
		}
	}

	@Test
	void Option3DialogueActionDialogueIdHasExpectedShape() {
		final Object[][] rows = OptionDialogues.Option3DialogueActionDialogueId();
		assertEquals(1, rows.length);
		for (Object[] row : rows) {
			assertEquals(6, row.length);
		}
	}

	@Test
	void Option3DialogueActionDialogueIdTeleActionHasExpectedShape() {
		final Object[][] rows = OptionDialogues.Option3DialogueActionDialogueIdTeleAction();
		assertEquals(1, rows.length);
		for (Object[] row : rows) {
			assertEquals(7, row.length);
		}
	}

	@Test
	void Option4DialogueActionHasExpectedShape() {
		final Object[][] rows = OptionDialogues.Option4DialogueAction();
		assertEquals(22, rows.length);
		for (Object[] row : rows) {
			assertEquals(6, row.length);
		}
	}

	@Test
	void Option4DialogueActionNextChatHasExpectedShape() {
		final Object[][] rows = OptionDialogues.Option4DialogueActionNextChat();
		assertEquals(1, rows.length);
		for (Object[] row : rows) {
			assertEquals(7, row.length);
		}
	}

	@Test
	void Option5DialogueActionHasExpectedShape() {
		final Object[][] rows = OptionDialogues.Option5DialogueAction();
		assertEquals(17, rows.length);
		for (Object[] row : rows) {
			assertEquals(7, row.length);
		}
	}

	@Test
	void Option5DialogueActionDialogueIdTeleActionHasExpectedShape() {
		final Object[][] rows = OptionDialogues.Option5DialogueActionDialogueIdTeleAction();
		assertEquals(3, rows.length);
		for (Object[] row : rows) {
			assertEquals(9, row.length);
		}
	}
}
