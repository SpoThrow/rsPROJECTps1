package server.game.bots;

import server.game.players.actions.objects.ObjectClick;

/**
 * The concrete {@link BotContext} for a possessed player — roadmap Phase G.
 *
 * <p><b>The actor half is delegated; the player half is here.</b> Position, walking, idleness and object
 * interaction moved to {@link PlayerAgent} when the seam was introduced, so this class is now mostly the
 * player-specific facts {@link Agent} deliberately does not promise: inventory, skills and the bank. The
 * division is what makes {@code NpcAgent} possible at all.
 */
public final class PlayerBotContext implements BotContext {

	private final BotPlayer bot;
	private final PlayerAgent agent;
	private final BotTrace trace = new BotTrace();
	private int ticks;

	public PlayerBotContext(BotPlayer bot) {
		this.bot = bot;
		this.agent = new PlayerAgent(bot);
	}

	// --- the actor ---

	@Override
	public Agent agent() {
		return agent;
	}

	// --- observation ---

	@Override
	public int x() {
		return agent.x();
	}

	@Override
	public int y() {
		return agent.y();
	}

	@Override
	public int height() {
		return agent.height();
	}

	@Override
	public boolean arrivedAt(int x, int y, int range) {
		return agent.arrivedAt(x, y, range);
	}

	@Override
	public boolean isIdle() {
		// Two separate questions, deliberately: the actor is not walking, and no skill session is running.
		// A bot standing still mid-chop is not idle, which is what a caller draining a queue means.
		return agent.isIdle() && !isSkilling();
	}

	@Override
	public boolean isSkilling() {
		// Woodcutting is the only skill that runs a session today; see WoodcuttingSession.active.
		return bot.woodcutting.active;
	}

	@Override
	public int freeSlots() {
		return bot.getItems().freeSlots();
	}

	@Override
	public boolean hasItem(int itemId) {
		return bot.getItems().playerHasItem(itemId);
	}

	@Override
	public boolean isBanking() {
		return bot.isBanking;
	}

	@Override
	public boolean isDead() {
		return bot.isDead;
	}

	@Override
	public int skillLevel(int skill) {
		int[] levels = bot.skills.playerLevel;
		return skill >= 0 && skill < levels.length ? levels[skill] : 0;
	}

	@Override
	public BotTrace trace() {
		return trace;
	}

	@Override
	public int ticksInState() {
		return ticks;
	}

	@Override
	public int random(int bound) {
		return core.util.Misc.random(bound);
	}

	// --- intent ---

	@Override
	public void walkTo(int x, int y) {
		agent.walkTo(x, y);
	}

	@Override
	public boolean interactObject(int objectId, int x, int y, ObjectClick click, int range) {
		return agent.interactObject(objectId, x, y, click, range);
	}

	@Override
	public void openBank() {
		bot.getPA().openUpBank();
	}

	@Override
	public boolean depositItem(int itemId) {
		boolean deposited = false;
		for (int slot = 0; slot < bot.playerItems.length; slot++) {
			// playerItems stores itemId + 1; 0 means empty.
			while (bot.playerItems[slot] - 1 == itemId && bot.playerItemsN[slot] > 0) {
				if (!bot.getItems().bankItem(itemId, slot, bot.playerItemsN[slot])) {
					break;
				}
				deposited = true;
			}
		}
		return deposited;
	}

	// --- lifecycle ---

	@Override
	public void onStateEntered() {
		ticks = 0;
	}

	@Override
	public void onTick() {
		ticks++;
		// The trace's clock is the driver's clock: advanced once per tick, before the tree runs, so
		// every event a node reports carries the tick it happened on and "after Nt" is measurable.
		trace.advance();
	}
}
