package server.game.players.actions.dialogues;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import server.game.players.Client;
import server.game.players.DialogueHandler;

/**
 * Registry of dialogue content keyed on the dialogue id, consulted by
 * {@link DialogueHandler#sendDialogues(int, int)} before its switch.
 *
 * Handlers are data-driven tables (see {@link ChatDialogues}, {@link EmoteChatDialogues},
 * {@link StatementDialogues}, {@link OptionDialogues}) registered once
 * when this class initialises.
 */
public final class DialogueRegistry {

	private static final Map<Integer, DialogueAction> ACTIONS = new ConcurrentHashMap<>();

	static {
		ChatDialogues.register();
		EmoteChatDialogues.register();
		StatementDialogues.register();
		OptionDialogues.register();
	}

	private DialogueRegistry() {
	}

	public static void register(int dialogueId, DialogueAction action) {
		if (ACTIONS.putIfAbsent(dialogueId, action) != null) {
			throw new IllegalStateException("Duplicate dialogue handler for id " + dialogueId);
		}
	}

	public static boolean dispatch(DialogueHandler dialogueHandler, Client c, int dialogueId) {
		final DialogueAction action = ACTIONS.get(dialogueId);
		if (action == null) {
			return false;
		}
		action.handle(dialogueHandler, c, dialogueId);
		return true;
	}

	public static boolean isRegistered(int dialogueId) {
		return ACTIONS.containsKey(dialogueId);
	}

	public static int size() {
		return ACTIONS.size();
	}
}
