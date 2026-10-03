package server.game.players.actions.objects;

/**
 * First-click teleport objects, lifted out of {@code ActionHandler}.
 *
 * <p>Each row is {@code {objectId, x, y, height}} and registers a {@code movePlayer}
 * to that destination. The ids are irregular and the coordinates bear no relation to
 * them, so the table is data, not something computable. It was generated from the
 * switch rather than retyped, and {@code TeleportObjectsTest} pins it: a wrong
 * coordinate sends a player to the wrong place and reports no error.
 */
public final class TeleportObjects {

	private static final int[][] TELEPORTS = {
			{ 492, 2857, 9569, 0 },
			{ 1764, 2857, 3167, 0 },
			{ 9358, 2480, 5175, 0 },
			{ 9359, 2862, 9572, 0 },
			{ 12467, 2974, 9511, 0 },
			{ 12468, 2974, 9511, 0 },
			{ 8930, 1975, 4409, 3 },
			{ 10177, 1798, 4407, 3 },
			{ 10193, 2545, 10143, 0 },
			{ 10195, 1809, 4405, 2 },
			{ 10196, 1807, 4405, 3 },
			{ 10197, 1823, 4404, 2 },
			{ 10198, 1825, 4404, 3 },
			{ 10199, 1834, 4388, 2 },
			{ 10200, 1834, 4390, 3 },
			{ 10201, 1811, 4394, 1 },
			{ 10202, 1812, 4394, 2 },
			{ 10203, 1799, 4386, 2 },
			{ 10204, 1799, 4388, 1 },
			{ 10205, 1796, 4382, 1 },
			{ 10206, 1796, 4382, 2 },
			{ 10207, 1800, 4369, 2 },
			{ 10208, 1802, 4370, 1 },
			{ 10209, 1827, 4362, 1 },
			{ 10210, 1825, 4362, 2 },
			{ 10211, 1863, 4373, 2 },
			{ 10212, 1863, 4371, 1 },
			{ 10213, 1864, 4389, 1 },
			{ 10214, 1864, 4387, 2 },
			{ 10215, 1890, 4407, 0 },
			{ 10216, 1890, 4406, 1 },
			{ 10217, 1957, 4373, 1 },
			{ 10218, 1957, 4371, 0 },
			{ 10219, 1824, 4379, 3 },
			{ 10220, 1824, 4381, 2 },
			{ 10221, 1838, 4375, 2 },
			{ 10222, 1838, 4377, 3 },
			{ 10223, 1850, 4386, 1 },
			{ 10224, 1850, 4387, 2 },
			{ 10225, 1932, 4378, 1 },
			{ 10226, 1932, 4380, 2 },
			{ 10228, 1961, 4393, 3 },
			{ 10229, 1912, 4367, 0 },
			{ 10230, 2899, 4449, 0 },
			{ 2823, 2881, 5310, 2 },
			{ 12230, 2506, 3038, 0 },
			{ 2, 3029, 9582, 0 },
			{ 1765, 3067, 10256, 0 },
			{ 1766, 3016, 3849, 0 },
	};

	private TeleportObjects() {
	}

	static void register() {
		for (int[] teleport : TELEPORTS) {
			ObjectHandler.register(teleport[0], ObjectClick.FIRST, (c, objectType, objectX, objectY) ->
					c.getPA().movePlayer(teleport[1], teleport[2], teleport[3]));
		}
	}

	// Package-private for TeleportObjectsTest, which pins the transcription.
	static int[][] teleports() {
		int[][] copy = new int[TELEPORTS.length][];
		for (int i = 0; i < TELEPORTS.length; i++) {
			copy[i] = TELEPORTS[i].clone();
		}
		return copy;
	}
}
