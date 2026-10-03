package server.game.players.packets.buttons;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import server.game.players.Client;

/**
 * Registry of interface button ids, consulted before the legacy switch in
 * {@code ClickingButtons}.
 *
 * <p>This is the strangler half of the migration: {@link #dispatch} runs first and
 * the switch becomes the fallthrough, so a button can be moved out of the 4000-line
 * switch one family at a time without the server ever being broken in between.
 *
 * <p>A duplicate registration throws. In a switch, two {@code case} labels with the
 * same value are a compile error; in a map the second silently shadows the first, so
 * the invariant has to be enforced here or the migration loses a guarantee it used
 * to get for free.
 */
public final class ButtonHandler {

	private static final Map<Integer, ButtonAction> actions = new ConcurrentHashMap<>();

	static {
		SmeltingButtons.register();
		PrayerButtons.register();
		SpecialAttackButtons.register();
		DuelRuleButtons.register();
		SettingsSliderButtons.register();
		BankTabButtons.register();
		FightModeButtons.register();
	}

	private ButtonHandler() {
	}

	public static void register(int actionButtonId, ButtonAction action) {
		if (actions.putIfAbsent(actionButtonId, action) != null) {
			throw new IllegalStateException("button " + actionButtonId + " is registered twice");
		}
	}

	/**
	 * @return true if a handler claimed this id, meaning the legacy switch must not
	 *         also run for it.
	 */
	public static boolean dispatch(Client c, int actionButtonId) {
		ButtonAction action = actions.get(actionButtonId);
		if (action == null) {
			return false;
		}
		action.handle(c, actionButtonId);
		return true;
	}

	public static boolean isRegistered(int actionButtonId) {
		return actions.containsKey(actionButtonId);
	}

	public static int size() {
		return actions.size();
	}
}
