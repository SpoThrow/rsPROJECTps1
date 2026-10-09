package server.game.bots;

import server.game.players.Client;
import server.game.players.PathFinder;
import server.game.players.actions.objects.ObjectClick;
import server.game.players.actions.objects.ObjectHandler;

/**
 * The {@link Agent} for a real, logged-in character — roadmap Phase G.
 *
 * <p>This is the movement and interaction glue lifted verbatim out of {@code PlayerBotContext}, which is
 * where it lived before the seam existed. Nothing about it changed in the move; what changed is who can
 * see it. A state now reaches these through {@link Agent} and cannot tell whether the actor behind it is
 * a player or an NPC.
 *
 * <p><b>Why movement is not just "set a destination".</b> A player moves by a route: the packet path
 * queues steps and the character advances one tile per tick, so this issues a {@code PathFinder} route
 * rather than a step. An NPC moves a tile at a time inside its own tick (see {@link NpcAgent}). The two
 * implementations differ here on purpose — that is what "the implementation's business" means in
 * {@link Agent#walkTo} — and it is why a tree can be written once against the interface.
 *
 * <p><b>Interaction goes through the registry.</b> {@link #interactObject} dispatches through
 * {@link ObjectHandler} exactly as a real click does, so bot code never calls a skill class and a newly
 * migrated skill becomes bot-usable for free. The legacy switch is only a fallback for objects that have
 * not been migrated yet.
 */
public final class PlayerAgent implements Agent {

	/**
	 * The {@code Client} this agent drives. The one place the seam is deliberately opened, and only for
	 * the player-specific context that owns this agent: inventory, skills and the bank are player facts
	 * that {@link Agent} does not promise, so the context needs the real thing.
	 */
	private final Client bot;

	public PlayerAgent(Client bot) {
		if (bot == null) {
			throw new IllegalArgumentException("a player agent needs a client");
		}
		this.bot = bot;
	}

	/** The character this agent drives. */
	public Client client() {
		return bot;
	}

	@Override
	public String name() {
		return bot.playerName;
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
		// Walk queue drained. Whether a skill session is running is a separate question and a player
		// fact, so it lives on BotContext rather than being folded in here.
		return bot.wQueueReadPtr == bot.wQueueWritePtr;
	}

	@Override
	public void walkTo(int x, int y) {
		// Mirror the walking packet: arriving by walking closes any open interface (a bank, a shop).
		// Without this a bank left open would make isIdle() permanently false.
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
		// Not migrated yet: fall through to the legacy switch, the same order ActionHandler itself uses.
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
}
