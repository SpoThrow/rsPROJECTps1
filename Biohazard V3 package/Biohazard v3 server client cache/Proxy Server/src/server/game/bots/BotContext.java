package server.game.bots;

import server.game.players.actions.objects.ObjectClick;

/**
 * The single surface a {@link BotState} sees.
 *
 * <p>Everything else about a bot is internal and free to change. Keeping the context behind
 * an interface (rather than a concrete class) is deliberate: it is the seam that later lets
 * the same behaviour library drive an NPC as well as a player without editing a single
 * state (roadmap §3 seam 2, §5.1).
 *
 * <p>Intent methods are the <em>only</em> way a state acts, and {@link #interactObject} is
 * the single object-interaction entry point: it dispatches through the
 * {@code ObjectHandler} registry exactly as a real click does, so bot code never calls a
 * skill class and a newly migrated skill becomes bot-usable for free.
 */
public interface BotContext {

	// --- observation (reads only) ---

	/** The possessed client, for the few places a state legitimately needs it. */
	BotPlayer client();

	/**
	 * This bot's transition history (roadmap Phase F). Never null.
	 *
	 * <p>A state normally has no reason to touch this — {@link Traced} reports on its behalf. It is here
	 * for the one thing a wrapper cannot know: <em>why</em> a leaf decided to fail, which the leaf calls
	 * {@link BotTrace#note} with just before returning.
	 */
	BotTrace trace();

	int x();

	int y();

	int height();

	boolean arrivedAt(int x, int y, int range);

	/** Walk queue drained and no active skill session. */
	boolean isIdle();

	int freeSlots();

	boolean hasItem(int itemId);

	/** Ticks the current leaf state has been running, reset by the driver on each enter. */
	int ticksInState();

	int random(int bound);

	// --- intent (each is the single entry point for that action) ---

	void walkTo(int x, int y);

	/**
	 * Sets the click fields on the client, confirms range, then dispatches through the
	 * object registry (falling back to the matching {@code ActionHandler} entry point).
	 *
	 * @return true if the interaction was issued
	 */
	boolean interactObject(int objectId, int x, int y, ObjectClick click, int range);

	void openBank();

	boolean depositItem(int itemId);

	// --- lifecycle, used only by the driver when a root state ends ---

	void onStateEntered();

	void onTick();
}
