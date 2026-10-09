package server.game.bots;

import server.game.players.PathFinder;
import server.game.players.actions.objects.ObjectClick;
import server.game.players.actions.objects.ObjectHandler;

/**
 * The concrete {@link BotContext} for a possessed player.
 *
 * <p>Every intent method keeps the client's own state in step with what a real click or
 * walk would have done, so the ordinary skill/action code sees a bot exactly as it sees a
 * human. Nothing here calls a skill class: {@link #interactObject} dispatches through the
 * registry.
 */
public final class PlayerBotContext implements BotContext {

	private final BotPlayer bot;
	private int ticks;

	public PlayerBotContext(BotPlayer bot) {
		this.bot = bot;
	}

	// --- observation ---

	@Override
	public BotPlayer client() {
		return bot;
	}

	@Override
	public int x() {
		return bot.getX();
	}

	@Override
	public int y() {
		return bot.getY();
	}

	@Override
	public int height() {
		return bot.position.heightLevel;
	}

	@Override
	public boolean arrivedAt(int x, int y, int range) {
		return bot.goodDistance(x, y, bot.getX(), bot.getY(), range);
	}

	@Override
	public boolean isIdle() {
		// Walk queue drained and no skill session running.
		return bot.wQueueReadPtr == bot.wQueueWritePtr && !bot.woodcutting.active;
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
		// Mirror the walking packet: arriving by walking closes any open interface (a bank,
		// a shop). Without this a bank left open would make isIdle() permanently false.
		bot.getPA().closeAllWindows();
		bot.getPA().removeAllWindows();
		bot.clickObjectType = 0;
		bot.walkRepath.lastWalkDestX = x;
		bot.walkRepath.lastWalkDestY = y;
		PathFinder.getPathFinder().findRoute(bot, x, y, true, 1, 1);
	}

	@Override
	public boolean interactObject(int objectId, int x, int y, ObjectClick click, int range) {
		// The click fields the migrated ObjectActions read.
		bot.objectX = x;
		bot.objectY = y;
		bot.objectId = objectId;
		bot.objectDistance = range;
		bot.objectXOffset = 0;
		bot.objectYOffset = 0;

		if (!bot.goodDistance(x, y, bot.getX(), bot.getY(), range)) {
			return false; // let the caller's WalkTo retry
		}
		if (ObjectHandler.dispatch(bot, objectId, click, x, y)) {
			return true;
		}
		// Not migrated yet: fall through to the legacy switch, the same order ActionHandler
		// itself uses.
		switch (click) {
			case FIRST:
				bot.getActions().firstClickObject(objectId, x, y);
				return true;
			case SECOND:
				bot.getActions().secondClickObject(objectId, x, y);
				return true;
			case THIRD:
				bot.getActions().thirdClickObject(objectId, x, y);
				return true;
			default:
				return false;
		}
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
	}
}
