package server.game.players.actions.objects;

/**
 * The Gnome Stronghold agility obstacles, lifted out of {@code ActionHandler}.
 *
 * <p>Three behaviours sit together in the switch but are not one family, so they are
 * kept separate here rather than forced into a single table:
 *
 * <ul>
 *   <li>four obstacles that check an agility level and then teleport;
 *   <li>four that shove the player two tiles along X, away from the side they
 *       approached from;
 *   <li>one that does the same along Y.
 * </ul>
 *
 * <p>The level-gated handlers use {@code return} to abort, exactly as the switch did.
 * That is safe because nothing follows the switch in {@code firstClickObject}, so
 * returning from the handler and returning from the method are the same thing.
 */
public final class AgilityObjects {

	// {objectId, requiredLevel, x, y, height} for the level-gated obstacles.
	private static final int[][] GATED = {
			{ 5088, 30, 2687, 9506, 0 },
			{ 5090, 30, 2682, 9506, 0 },
			{ 5110, 12, 2647, 9557, 0 },
			{ 5111, 12, 2649, 9562, 0 },
	};

	private static final int[] PUSH_ALONG_X = { 5103, 5105, 5106, 5107 };
	private static final int PUSH_ALONG_Y = 5104;

	// Agility is skill index 16 in playerLevel.
	private static final int AGILITY = 16;

	private AgilityObjects() {
	}

	static void register() {
		for (int[] obstacle : GATED) {
			final int required = obstacle[1];
			final int x = obstacle[2];
			final int y = obstacle[3];
			final int height = obstacle[4];
			ObjectHandler.register(obstacle[0], ObjectClick.FIRST, (c, objectType, objectX, objectY) -> {
				if (c.skills.playerLevel[AGILITY] < required) {
					c.sendMessage("You need an Agility level of " + required + " to pass this.");
					return;
				}
				c.getPA().movePlayer(x, y, height);
			});
		}

		for (int obstacle : PUSH_ALONG_X) {
			ObjectHandler.register(obstacle, ObjectClick.FIRST, (c, objectType, objectX, objectY) -> {
				if (c.getX() > objectX) {
					c.getPA().walkTo(-2, 0);
				} else {
					c.getPA().walkTo(2, 0);
				}
			});
		}

		ObjectHandler.register(PUSH_ALONG_Y, ObjectClick.FIRST, (c, objectType, objectX, objectY) -> {
			if (c.getY() > objectY) {
				c.getPA().walkTo(0, -2);
			} else {
				c.getPA().walkTo(0, 2);
			}
		});
	}

	static int[][] gated() {
		int[][] copy = new int[GATED.length][];
		for (int i = 0; i < GATED.length; i++) {
			copy[i] = GATED[i].clone();
		}
		return copy;
	}

	static int[] pushAlongX() {
		return PUSH_ALONG_X.clone();
	}

	static int pushAlongY() {
		return PUSH_ALONG_Y;
	}
}
