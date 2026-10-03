package server.game.players.actions.objects;

/**
 * Castle Wars objects whose click is handled entirely by {@code firstClickObject}'s
 * preamble, which already calls {@code CastleWarObjects.handleObject} and returns when
 * it does anything.
 *
 * <p>These ids also appeared in the switch, calling {@code handleObject} a
 * <em>second</em> time. That was not harmless: {@code handleObject}'s movement branches
 * read the player's current position, so a click moved the player twice and the two
 * moves could disagree. {@code case 4419} moved to {@code (2416, 3074, 0)} and then the
 * second call moved the player back to {@code (2417, 3077, 0)}; {@code case 4420} ended
 * on the opposite branch from the one it entered.
 *
 * <p>They are registered as no-ops rather than deleted, because deleting them would let
 * them fall to {@code default: handleGenericObject(1, ...)}, which would start doing
 * generic work from the object definition. Registration here means dispatch claims the
 * id and returns, so the preamble's single call is the only call and the switch never
 * runs.
 *
 * <p>The group is the run of fall-through labels {@code 4411..4378}, which also had no
 * {@code break} and so fell into {@code case 1568}. That is why {@code case 1568} could
 * be removed along with them: {@code handleObject}'s own {@code case 1568} returns
 * {@code true} unconditionally, so the preamble already returns for id 1568 and the
 * switch case was unreachable for it.
 */
public final class CastleWarsObjectClicks {

	private static final int[] IDS = {
			4411, 4415, 4417, 4418, 4420, 4469, 4470, 4419, 4911, 4912, 1747, 1757, 4437,
			6281, 6280, 4472, 4471, 4406, 4407, 4458, 4902, 4903, 4900, 4901, 4461, 4463,
			4464, 4377, 4378,
	};

	private CastleWarsObjectClicks() {
	}

	static void register() {
		for (int id : IDS) {
			ObjectHandler.register(id, ObjectClick.FIRST, (c, objectType, objectX, objectY) -> {
				// Intentionally empty: the preamble already handled this object.
			});
		}
	}

	static int[] ids() {
		return IDS.clone();
	}
}
