package server.game.bots;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import server.game.players.actions.objects.ObjectClick;

/**
 * A {@link BotContext} that answers without a world, and records what it was asked to do — the shared
 * test double for every state test.
 *
 * <p>One fake rather than one per test file: a second copy of this would drift from {@code BotContext}
 * the moment the interface grows a method, and the tests that use it are the ones that would not notice.
 *
 * <p>Everything a state can ask about is settable ({@code at}, {@code arrived}, {@code freeSlots},
 * {@code item}) and everything a state can <em>do</em> is recorded ({@code walks},
 * {@code interactions}, {@code deposits}, {@code bankOpens}). So a test can assert on the decisions a
 * state made rather than only on the outcome it returned.
 *
 * <p>Public because the script tests live in {@code server.game.bots.script} and drive the same fake —
 * a state does not care which package its context came from.
 */
public final class FakeBotContext implements BotContext {

	/** Values {@code random} returns, in order; then {@code randomDefault} forever. */
	private final Deque<Integer> randoms = new ArrayDeque<Integer>();

	private int randomDefault;

	private BotPlayer client;
	private int x;
	private int y;
	private int height;
	private boolean arrived = true;
	private boolean idle = true;
	private int freeSlots = 28;
	private final Set<Integer> items = new HashSet<Integer>();
	private int stateTicks;

	/** What the state asked the bot to do. */
	public final List<String> walks = new ArrayList<String>();
	public final List<String> interactions = new ArrayList<String>();
	public final List<Integer> deposits = new ArrayList<Integer>();
	public int bankOpens;
	public int stateEnters;
	public int ticks;

	private final BotTrace trace = new BotTrace();

	/** Queues the values {@code random} returns, in order. */
	public FakeBotContext scripted(int... values) {
		for (int value : values) {
			randoms.add(value);
		}
		return this;
	}

	public FakeBotContext randomDefault(int value) {
		this.randomDefault = value;
		return this;
	}

	public FakeBotContext client(BotPlayer bot) {
		this.client = bot;
		return this;
	}

	public FakeBotContext at(int tileX, int tileY, int plane) {
		this.x = tileX;
		this.y = tileY;
		this.height = plane;
		return this;
	}

	/** Whether {@code arrivedAt} says the bot is in range of the tile it was asked about. */
	public FakeBotContext arrived(boolean value) {
		this.arrived = value;
		return this;
	}

	public FakeBotContext idle(boolean value) {
		this.idle = value;
		return this;
	}

	public FakeBotContext freeSlots(int value) {
		this.freeSlots = value;
		return this;
	}

	public FakeBotContext item(int itemId) {
		items.add(itemId);
		return this;
	}

	/** The last tile a state walked to, or null. A convenience for a single-walk assertion. */
	public String lastWalk() {
		return walks.isEmpty() ? null : walks.get(walks.size() - 1);
	}

	@Override public BotPlayer client() { return client; }
	@Override public BotTrace trace() { return trace; }
	@Override public int x() { return x; }
	@Override public int y() { return y; }
	@Override public int height() { return height; }
	@Override public boolean arrivedAt(int tileX, int tileY, int range) { return arrived; }
	@Override public boolean isIdle() { return idle; }
	@Override public int freeSlots() { return freeSlots; }
	@Override public boolean hasItem(int itemId) { return items.contains(itemId); }
	@Override public int ticksInState() { return stateTicks; }

	@Override
	public int random(int bound) {
		return randoms.isEmpty() ? randomDefault : randoms.poll();
	}

	@Override
	public void walkTo(int tileX, int tileY) {
		walks.add(tileX + "," + tileY);
	}

	@Override
	public boolean interactObject(int objectId, int tileX, int tileY, ObjectClick click, int range) {
		interactions.add(objectId + "@" + tileX + "," + tileY + ":" + click);
		return arrived;
	}

	@Override public void openBank() { bankOpens++; }

	@Override
	public boolean depositItem(int itemId) {
		deposits.add(itemId);
		return true;
	}

	@Override public void onStateEntered() { stateEnters++; }

	@Override
	public void onTick() {
		ticks++;
		// Mirrors PlayerBotContext: the fake is the driver in a state test, so it advances the clock.
		trace.advance();
	}
}
