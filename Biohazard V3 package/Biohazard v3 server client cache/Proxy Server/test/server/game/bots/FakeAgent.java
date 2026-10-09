package server.game.bots;

import java.util.ArrayList;
import java.util.List;

import server.game.players.actions.objects.ObjectClick;

/**
 * An {@link Agent} that answers without a world and records what it was asked to do — the second half of
 * the shared test double, split out in roadmap Phase G.
 *
 * <p><b>Why it is separate from {@link FakeBotContext}.</b> The point of the phase is that a tree can be
 * driven by any actor, so the tests need a fake <em>actor</em> that can be put behind a context without a
 * player anywhere in sight. It also keeps the two halves honest: this class can only record the
 * movement and interaction an {@link Agent} can be asked for, so it cannot accidentally depend on
 * inventory or skill state that an NPC would not have.
 *
 * <p>Public because the script tests live in {@code server.game.bots.script} and use the same fake.
 */
public final class FakeAgent implements Agent {

	/** What this actor was asked to do. */
	public final List<String> walks = new ArrayList<String>();
	public final List<String> interactions = new ArrayList<String>();

	private String name = "fakebot";
	private int x;
	private int y;
	private int height;
	private boolean arrived = true;
	private boolean idle = true;

	public FakeAgent named(String name) {
		this.name = name;
		return this;
	}

	public FakeAgent at(int tileX, int tileY, int plane) {
		this.x = tileX;
		this.y = tileY;
		this.height = plane;
		return this;
	}

	/** Whether {@code arrivedAt} says this actor is in range of the tile it was asked about. */
	public FakeAgent arrived(boolean value) {
		this.arrived = value;
		return this;
	}

	public FakeAgent idle(boolean value) {
		this.idle = value;
		return this;
	}

	/** The last tile this actor walked to, or null. A convenience for a single-walk assertion. */
	public String lastWalk() {
		return walks.isEmpty() ? null : walks.get(walks.size() - 1);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public int x() {
		return x;
	}

	@Override
	public int y() {
		return y;
	}

	@Override
	public int height() {
		return height;
	}

	@Override
	public boolean arrivedAt(int tileX, int tileY, int range) {
		return arrived;
	}

	@Override
	public boolean isIdle() {
		return idle;
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
}
