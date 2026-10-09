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
 * <p><b>Split in two by roadmap Phase G.</b> The actor half — position, walking, interaction, idleness —
 * now lives in {@link FakeAgent}, because that is the surface a second kind of actor can honour. What
 * stays here is the player-only half: inventory, the bank, skills. That division is not cosmetic; it is
 * the same division the production classes make, so a state that leans on an inventory fact is visibly
 * leaning on something an NPC could not provide.
 *
 * <p>Everything a state can ask about is settable ({@code at}, {@code arrived}, {@code freeSlots},
 * {@code item}) and everything a state can <em>do</em> is recorded ({@code walks},
 * {@code interactions}, {@code deposits}, {@code bankOpens}). So a test can assert on the decisions a
 * state made rather than only on the outcome it returned.
 *
 * <p>{@code walks} and {@code interactions} are <em>aliases</em> of the actor's own lists rather than
 * copies: the actor does the recording, and these fields exist so a test can read it without a hop
 * through {@code agent()}. Two views of one list, so they cannot disagree.
 *
 * <p>Public because the script tests live in {@code server.game.bots.script} and drive the same fake — a
 * state does not care which package its context came from.
 */
public final class FakeBotContext implements BotContext {

	/** The actor. Declared first so the aliases below can point at its lists. */
	public final FakeAgent actor = new FakeAgent();

	/** Aliases of {@link FakeAgent#walks} / {@link FakeAgent#interactions}. */
	public final List<String> walks = actor.walks;
	public final List<String> interactions = actor.interactions;

	/** Values {@code random} returns, in order; then {@code randomDefault} forever. */
	private final Deque<Integer> randoms = new ArrayDeque<Integer>();

	private int randomDefault;

	private int freeSlots = 28;
	private final Set<Integer> items = new HashSet<Integer>();
	private int stateTicks;
	private boolean banking;
	private boolean dead;
	private boolean skilling;
	private final int[] levels = new int[25];

	/** Player-only facts a state asked about, or the player-only things it did. */
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

	/** Names the actor, which is what a bot-derived seed reads. */
	public FakeBotContext named(String name) {
		actor.named(name);
		return this;
	}

	public FakeBotContext at(int tileX, int tileY, int plane) {
		actor.at(tileX, tileY, plane);
		return this;
	}

	/** Whether {@code arrivedAt} says the actor is in range of the tile it was asked about. */
	public FakeBotContext arrived(boolean value) {
		actor.arrived(value);
		return this;
	}

	public FakeBotContext idle(boolean value) {
		actor.idle(value);
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

	public FakeBotContext banking(boolean value) {
		this.banking = value;
		return this;
	}

	public FakeBotContext dead(boolean value) {
		this.dead = value;
		return this;
	}

	public FakeBotContext skilling(boolean value) {
		this.skilling = value;
		return this;
	}

	public FakeBotContext level(int skill, int level) {
		levels[skill] = level;
		return this;
	}

	/** The last tile the actor walked to, or null. A convenience for a single-walk assertion. */
	public String lastWalk() {
		return actor.lastWalk();
	}

	// --- the actor ---

	@Override
	public Agent agent() {
		return actor;
	}

	// --- observation ---

	@Override public int x() { return actor.x(); }
	@Override public int y() { return actor.y(); }
	@Override public int height() { return actor.height(); }
	@Override public boolean arrivedAt(int tileX, int tileY, int range) { return actor.arrivedAt(tileX, tileY, range); }

	/** One answer from both halves, exactly as {@code PlayerBotContext} combines them. */
	@Override public boolean isIdle() { return actor.isIdle() && !skilling; }

	@Override public boolean isSkilling() { return skilling; }
	@Override public int freeSlots() { return freeSlots; }
	@Override public boolean hasItem(int itemId) { return items.contains(itemId); }
	@Override public boolean isBanking() { return banking; }
	@Override public boolean isDead() { return dead; }

	@Override
	public int skillLevel(int skill) {
		return skill >= 0 && skill < levels.length ? levels[skill] : 0;
	}

	@Override public BotTrace trace() { return trace; }
	@Override public int ticksInState() { return stateTicks; }

	@Override
	public int random(int bound) {
		return randoms.isEmpty() ? randomDefault : randoms.poll();
	}

	// --- intent ---

	@Override
	public void walkTo(int tileX, int tileY) {
		actor.walkTo(tileX, tileY);
	}

	@Override
	public boolean interactObject(int objectId, int tileX, int tileY, ObjectClick click, int range) {
		return actor.interactObject(objectId, tileX, tileY, click, range);
	}

	@Override public void openBank() { bankOpens++; }

	@Override
	public boolean depositItem(int itemId) {
		deposits.add(itemId);
		return true;
	}

	// --- lifecycle ---

	@Override public void onStateEntered() { stateEnters++; }

	@Override
	public void onTick() {
		ticks++;
		// Mirrors PlayerBotContext: the fake is the driver in a state test, so it advances the clock.
		trace.advance();
	}
}
