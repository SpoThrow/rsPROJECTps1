package server.game.players;

/**
 * Which NPC this player last interacted with, reached as {@code c.npcInteraction}.
 *
 * <p>Extracted in §4.20. It is the NPC half of the "last thing I clicked" state — the type, the
 * slot it occupies in {@code NPCHandler.npcs}, and the menu option that was chosen.
 *
 * <p>⚠️ <b>{@code npcType} is declared on {@code NPC} as well</b> ({@code NPC.java:16}), and on the
 * NPC side it is the NPC's <em>own</em> type from the definition files — an entirely different role
 * from "the type this player last clicked". The two live side by side, often in the same method
 * ({@code c.npcType} beside {@code npcs[i].npcType}), so this pass produced **by far the largest
 * skip count in the refactor: 167 {@code NPC} references and 62 parameters were deliberately left
 * alone**, versus 148 real edits. That ratio is the point of §4.16's tool fix.
 *
 * <p>All three default to {@code 0}, which means "nothing clicked" — and note that {@code 0} is
 * also what {@link #clickNpcType} is reset to between clicks, so the sentinel is load-bearing.
 */
public final class NpcInteraction {

	/** Type id of the NPC this player last interacted with; {@code 0} = none. ⚠️ Also declared on {@code NPC}, where it means the NPC's own type. */
	public int npcType;

	/** Slot of the clicked NPC in {@code NPCHandler.npcs}; {@code 0} = none. */
	public int npcClickIndex;

	/**
	 * ⚠️ <b>Misnamed: this is not a type at all, it is the click-menu <em>option index</em></b> that
	 * was chosen — {@code 1}..{@code 4} for the four right-click options. {@code ClickNPC} sets it to
	 * the option number and immediately reads it back to decide which handler to run, so it behaves
	 * more like a one-shot "which menu entry" marker than conversational state.
	 *
	 * <p>It is used as a general "am I interacting with an NPC" test too: {@code PlayerAssistant}
	 * computes {@code boolean talking = c.clickNpcType > 0;}. A future reader who assumes it holds an
	 * NPC id will misread every one of those checks.
	 */
	public int clickNpcType;
}
