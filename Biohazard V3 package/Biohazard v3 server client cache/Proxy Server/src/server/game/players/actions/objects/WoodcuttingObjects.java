package server.game.players.actions.objects;

import server.content.skills.Woodcutting;

/**
 * The first-click action for choppable trees, migrated out of {@code ActionHandler}.
 *
 * <p>Positional: {@code IDS[treeType]} is the object id that starts that tree, and
 * the index is the argument the switch used to pass to
 * {@code Woodcutting.startWoodcutting}. The x/y come from the player's
 * {@code objectX/objectY/clickObjectType} fields rather than the clicked coordinates,
 * which is what the original did.
 */
public final class WoodcuttingObjects {

	private static final int[] IDS = {
			1276, // 0
			1278, // 1
			1286, // 2
			1281, // 3
			1308, // 4
			5552, // 5
			1307, // 6
			1309, // 7
			1306, // 8
			5551, // 9
			5553, // 10
	};

	private WoodcuttingObjects() {
	}

	static void register() {
		for (int tree = 0; tree < IDS.length; tree++) {
			final int treeType = tree;
			ObjectHandler.register(IDS[tree], ObjectClick.FIRST, (c, objectType, objectX, objectY) ->
					Woodcutting.startWoodcutting(c, treeType, c.objectX, c.objectY, c.clickObjectType));
		}
	}

	// Package-private for WoodcuttingObjectsTest, which pins the transcription.
	static int[] ids() {
		return IDS.clone();
	}
}
