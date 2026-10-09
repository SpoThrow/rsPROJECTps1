package server.game.bots;

import server.game.players.actions.objects.ObjectClick;

/**
 * An actor a behaviour tree can drive — the "an agent" seam from {@code BOT_ROADMAP.md} §5.1, Phase G.
 *
 * <p><b>The problem this solves.</b> A {@link BotState} should not know what it is driving. Before this
 * interface, {@link BotContext} handed out a {@code BotPlayer}, so every state that touched the actor was
 * silently written against a player and could never run on anything else. Narrowing that to an
 * {@code Agent} means the same tree drives a player bot and an NPC, which is the whole point of the
 * phase.
 *
 * <p><b>Deliberately small, and deliberately not complete.</b> The roadmap's sketch lists five methods,
 * and they are exactly the ones an NPC can genuinely honour: where it is, how to walk somewhere, whether
 * it is busy, and nothing about inventories, skills or banks. Those are player facts, and they live on
 * {@link BotContext} rather than here precisely so that this interface does not promise something a
 * second implementation cannot keep.
 *
 * <p><b>One member is honestly not implementable for every actor: {@link #interactObject}.</b> Object
 * dispatch runs through {@code ObjectHandler.dispatch(Client, ...)} and its {@code ObjectAction}s are
 * {@code Client}-typed, so an NPC cannot click a tree through the existing registries. Rather than have
 * this interface lie — a silent {@code false} would make a {@code Gather} loop retry forever — a second
 * implementation reports it by throwing, and {@link NpcAgent} documents why. Clicking objects is
 * therefore player-only today; making it actor-generic is a change to the skill-dispatch path, not to
 * this interface.
 *
 * <p><b>Position, not a client.</b> Every method takes and returns plain coordinates, so nothing here
 * leaks a tile-editing surface and nothing needs to be cast back to a {@code Client} — a seam that
 * required that cast would not be a seam.
 */
public interface Agent {

	/** A display name for logs and traces: the player's account name, or the NPC's type name. */
	String name();

	int x();

	int y();

	/** The plane this actor is on. */
	int height();

	/** Whether this actor is already within {@code range} tiles of the target (Chebyshev distance). */
	boolean arrivedAt(int x, int y, int range);

	/** No walking pending: this actor will not move again without being asked to. */
	boolean isIdle();

	/**
	 * Asks this actor to head for a tile. Whether that is a path, a single step or a glide is the
	 * implementation's business and is what makes the two implementations genuinely different.
	 */
	void walkTo(int x, int y);

	/**
	 * Issues an object interaction, the way a real click would.
	 *
	 * @return true if the interaction was issued; false if it could not be (for example the target is out
	 *         of range, so the caller's walk can close the gap and try again)
	 * @throws UnsupportedOperationException if this kind of actor cannot interact with objects at all;
	 *         see the class note, and {@link NpcAgent}
	 */
	boolean interactObject(int objectId, int x, int y, ObjectClick click, int range);
}
