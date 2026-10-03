package server.game.players.actions.objects;

import server.Config;
import server.content.skills.Runecrafting;
import server.game.players.Player;

/**
 * The runecrafting rift objects from {@code ActionHandler}'s abyss section.
 *
 * <p>All of them require either the talisman in the inventory or the tiara worn, and
 * say the same thing when it is missing. They split into two kinds by what happens
 * when you get through:
 *
 * <ul>
 *   <li>{@link #TELEPORT_RIFTS} — teleport to the altar and award runecrafting XP.
 *       Rows are {@code {objectId, talisman, tiara, x, y, height}}.
 *   <li>{@link #CRAFTING_RIFTS} — hand off to {@code Runecrafting.craftRunes}, which
 *       runs the altar's own crafting flow. Rows are
 *       {@code {objectId, talisman, tiara, altarObjectId}}.
 * </ul>
 *
 * <p>The single talisman/tiara guard shape is why these are data. The switch mixed the
 * two kinds together in id order, which is how the soul rift (7138) came to sit between
 * the water and air teleport rifts while behaving like the blood rift (7141).
 */
public final class RuneRiftObjects {

	private static final int[][] TELEPORT_RIFTS = {
			{ 7129, 1442, 5537, 2583, 4838, 0 }, // fire
			{ 7130, 1440, 5535, 2660, 4839, 0 }, // earth
			{ 7131, 1446, 5533, 2527, 4833, 0 }, // body
			{ 7132, 1454, 5539, 2162, 4833, 0 }, // cosmic
			{ 7133, 1462, 5541, 2398, 4841, 0 }, // nature
			{ 7134, 1452, 5543, 2269, 4843, 0 }, // chaos
			{ 7135, 1458, 5545, 2464, 4834, 0 }, // law
			{ 7136, 1456, 5547, 2207, 4836, 0 }, // death
			{ 7137, 1444, 5531, 2713, 4836, 0 }, // water
			{ 7139, 1438, 5527, 2845, 4832, 0 }, // air
			{ 7140, 1448, 5529, 2788, 4841, 0 }, // mind
	};

	private static final int[][] CRAFTING_RIFTS = {
			{ 7138, 1460, 5551, 30625 }, // soul
			{ 7141, 1450, 5549, 30624 }, // blood
	};

	private RuneRiftObjects() {
	}

	static void register() {
		for (int[] rift : TELEPORT_RIFTS) {
			final int talisman = rift[1];
			final int tiara = rift[2];
			final int x = rift[3];
			final int y = rift[4];
			final int height = rift[5];
			ObjectHandler.register(rift[0], ObjectClick.FIRST, (c, objectType, objectX, objectY) -> {
				if (hasTalismanOrTiara(c, talisman, tiara)) {
					c.getPA().spellTeleport(x, y, height);
					c.getPA().addSkillXP(15 * Config.RUNECRAFTING_EXPERIENCE, Player.playerRunecrafting);
				} else {
					c.sendMessage(MISSING_TALISMAN);
				}
			});
		}

		for (int[] rift : CRAFTING_RIFTS) {
			final int talisman = rift[1];
			final int tiara = rift[2];
			final int altar = rift[3];
			ObjectHandler.register(rift[0], ObjectClick.FIRST, (c, objectType, objectX, objectY) -> {
				if (hasTalismanOrTiara(c, talisman, tiara)) {
					Runecrafting.craftRunes(c, altar);
				} else {
					c.sendMessage(MISSING_TALISMAN);
				}
			});
		}
	}

	private static final String MISSING_TALISMAN =
			"You need either a Talisman or a Tiara to get past this.";

	private static boolean hasTalismanOrTiara(server.game.players.Client c, int talisman, int tiara) {
		return c.getItems().playerHasItem(talisman, 1)
				|| c.getItems().playerHasItem(tiara)
				|| c.playerEquipment[c.playerHat] == tiara;
	}

	// Package-private for RuneRiftObjectsTest, which pins the transcription.
	static int[][] teleportRifts() {
		return copy(TELEPORT_RIFTS);
	}

	static int[][] craftingRifts() {
		return copy(CRAFTING_RIFTS);
	}

	private static int[][] copy(int[][] table) {
		int[][] out = new int[table.length][];
		for (int i = 0; i < table.length; i++) {
			out[i] = table[i].clone();
		}
		return out;
	}
}
