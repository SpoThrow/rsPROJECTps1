package server.game.bots;

import server.game.players.actions.objects.ObjectClick;

/**
 * The single surface a {@link BotState} sees.
 *
 * <p>Everything else about a bot is internal and free to change. Keeping the context behind
 * an interface (rather than a concrete class) is deliberate: it is the seam that lets the same
 * behaviour library drive an NPC as well as a player without editing a single state (roadmap §3 seam 2,
 * §5.1). Phase G is the payoff — {@link #agent()} hands out an {@link Agent}, not a {@code Client}, so
 * nothing here is written against the assumption that the actor is a player.
 *
 * <p><b>Two halves, and the split is the design.</b> {@link Agent} is what <em>any</em> actor can do —
 * where it is, how to walk, whether it is busy, how to click. Everything below that is a player fact:
 * inventories, skills, the bank. Those live here rather than on {@code Agent}, because an interface that
 * promised an NPC an inventory would be promising something no NPC can keep.
 *
 * <p>Intent methods are the <em>only</em> way a state acts, and {@link #interactObject} is the single
 * object-interaction entry point: it delegates to the agent, which dispatches through the
 * {@code ObjectHandler} registry exactly as a real click does, so bot code never calls a skill class and
 * a newly migrated skill becomes bot-usable for free.
 */
public interface BotContext {

	// --- the actor ---

	/**
	 * The actor this context drives: a possessed player, or an NPC.
	 *
	 * <p>Asking the agent directly is how a state stays actor-agnostic. The methods below are the same
	 * questions with the same answers, so anything that only needs position or movement can use either.
	 */
	Agent agent();

	// --- observation (reads only) ---

	int x();

	int y();

	int height();

	boolean arrivedAt(int x, int y, int range);

	/** Walk queue drained and no active skill session. */
	boolean isIdle();

	/** Whether a skill action this bot started is still running. Woodcutting is the only such session
	 * today; the name is generic so a mining or fishing leaf can ask the same question honestly. */
	boolean isSkilling();

	int freeSlots();

	boolean hasItem(int itemId);

	/** Whether a bank interface is open on this actor. Player fact: an NPC never has one. */
	boolean isBanking();

	/** Whether this actor is dead. Player fact: an NPC's death is handled by the NPC system. */
	boolean isDead();

	/** The actor's current level in a skill, or 0 for an actor that has no skills. */
	int skillLevel(int skill);

	/** This bot's transition history (roadmap Phase F). Never null. */
	BotTrace trace();

	/** Ticks the current leaf state has been running, reset by the driver on each enter. */
	int ticksInState();

	int random(int bound);

	// --- intent (each is the single entry point for that action) ---

	void walkTo(int x, int y);

	/**
	 * Sets the click fields on the actor, confirms range, then dispatches through the object registry
	 * (falling back to the matching {@code ActionHandler} entry point).
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
