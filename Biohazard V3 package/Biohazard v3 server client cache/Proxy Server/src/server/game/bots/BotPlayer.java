package server.game.bots;

import core.util.ISAACRandomGen;
import server.game.players.Client;

/**
 * A possessed character driven by code instead of a socket.
 *
 * <p>A bot is <em>not</em> a second kind of entity: it is an ordinary {@link Client} that
 * lives in {@code PlayerHandler.players} and runs through the normal per-player tick, so
 * position, skills, inventory, walking, collision and saving are identical to a human
 * player. The only difference is that nothing is reading its outgoing frames.
 *
 * <p>That difference is handled in three places: {@link Client#flushOutStream()} drops the
 * bytes when there is no session, {@link #update()} is a no-op, and {@link #initialize()}
 * is trimmed to the world-side state only.
 *
 * <p>Persistence is deliberately left exactly as a normal player's: {@code saveFile} and
 * {@code saveCharacter} are true, so the character is written by the ordinary logout,
 * autosave and shutdown paths, and the same account can later be logged into by a human.
 */
public class BotPlayer extends Client {

	/**
	 * A deterministic stand-in for the login handshake's ISAAC seed. The frame writers
	 * ({@code sendMessage}, {@code getItems().addItem}, {@code openUpBank}) call
	 * {@code packetEncryption.getNextKey()} unconditionally, and a bot never ran a login —
	 * without this, every one of those helpers would NPE before {@code flushOutStream}
	 * ever got the chance to discard the bytes. The values are irrelevant because nothing
	 * reads a bot's frames.
	 */
	private static final int[] PSEUDO_SEED = { 0x9E3779B9, 0x243F6A88, 0xB7E15162, 0x85EBCA6B };

	private BotController controller;

	public BotPlayer(int slot) {
		super(null, slot);
		this.isBot = true;
		this.isActive = true;
		// Written like any other character — bots are real accounts (BOT_ACCOUNTS.md).
		this.saveFile = true;
		this.saveCharacter = true;
		this.outStream.packetEncryption = new ISAACRandomGen(PSEUDO_SEED);
	}

	/** Attaches a behaviour tree. The root enters immediately; ticks start next game tick. */
	public void attach(BotController controller) {
		this.controller = controller;
		if (controller != null) {
			controller.enter();
		}
	}

	public BotController controller() {
		return controller;
	}

	/**
	 * The per-player tick. {@code super.process()} keeps the ordinary timer, energy and
	 * stat-restore upkeep; the behaviour tree is then ticked once, on the game thread.
	 */
	@Override
	public void process() {
		super.process();
		if (controller != null) {
			controller.tick();
		}
	}

	/**
	 * A bot has no socket, so there is no update block to encode for it. Skipping the
	 * player/NPC update keeps the shared tick free of packet work for bots.
	 */
	@Override
	public void update() {
		// Intentionally empty: see the class comment.
	}

	/**
	 * A bot has no interface to build and no session to send it to. Only the world-side
	 * coordinate fix-up that other systems rely on is applied.
	 */
	@Override
	public void initialize() {
		correctCoordinates();
	}
}
