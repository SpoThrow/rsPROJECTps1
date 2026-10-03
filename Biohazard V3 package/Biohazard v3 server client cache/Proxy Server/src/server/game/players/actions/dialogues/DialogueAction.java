package server.game.players.actions.dialogues;

import server.game.players.Client;
import server.game.players.DialogueHandler;

/** Handles one dialogue id; see {@link DialogueRegistry}. */
@FunctionalInterface
public interface DialogueAction {

	void handle(DialogueHandler dialogueHandler, Client c, int dialogueId);
}
