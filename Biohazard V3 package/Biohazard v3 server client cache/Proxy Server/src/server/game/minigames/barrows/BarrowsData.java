package server.game.minigames.barrows;

import server.game.players.Client;
import core.util.Misc;

public class BarrowsData {

	private static final int BARROWS_LOOT_CHANCE = 10;
	private static int[] loot = new int[2];
	private static int[] lootN = new int[2];

	@SuppressWarnings("unused")
	private static final int AHRIM_BROTHER = 0;
	@SuppressWarnings("unused")
	private static final int kARIL_BROTHER = 1;
	@SuppressWarnings("unused")
	private static final int DHAROK_BROTHER = 2;
	@SuppressWarnings("unused")
	private static final int VERAC_BROTHER = 3;
	@SuppressWarnings("unused")
	private static final int TORAG_BROTHER = 4;
	@SuppressWarnings("unused")
	private static final int GUTHAN_BROTHER = 5;

	public static final String[] BROTHER_NAME = {
		"Ahrim the Blighted", "Karil the Tainted", "Dharok the Wretched",
		"Verac the Defiled", "Torag the Corrupted", "Guthan the Infested",
	};

	public static final int[][] UNDERGROUND_SPAWN = {
		{3535, 9678}, {3534, 9694}, {3535, 9712}, {3542, 9721},
		{3569, 9713}, {3578, 9702}, {3570, 9692}, {3570, 9677},
	};

	public static final int[][] BROTHER_AREA = {
		{3563, 3285, 3567, 3291},
		{3563, 3273, 3568, 3278},
		{3573, 3295, 3578, 3300},
		{3554, 3294, 3560, 3300},
		{3551, 3280, 3556, 3285},
		{3575, 3280, 3580, 3284},
	};

	public static final int[][] BROTHER_SPAWN = {
		{3554, 9701}, {3549, 9681}, {3553, 9716},
		{3575, 9705}, {3571, 9684}, {3537, 9702},
	};

	public static final int[][] PLAYER_ENTRE = {
		{3557, 9703}, {3546, 9684}, {3556, 9718},
		{3578, 9706}, {3568, 9683}, {3534, 9704},
	};

	public static final int[][] PLAYER_EXIT = {
		{6702, 3565, 3289}, {6703, 3575, 3298}, {6704, 3577, 3283},
		{6705, 3565, 3276}, {6706, 3553, 3282}, {6707, 3556, 3298},
	};

	public static final int[][] BARROW_BROTHER = {
		{2025, 105, 20, 300, 200},
		{2028, 100, 21, 300, 200},
		{2026, 100, 68, 300, 300},
		{2030, 100, 24, 300, 200},
		{2029, 100, 23, 300, 200},
		{2027, 100, 24, 300, 200},
	};

	public static final int[] DOORS = {
		6726,6745,6736,6717,6750,6731,6748,6729,
		6737,6718,6738,6719,6735,6716,6740,6721,
		6739,6720,6723,6742,6722,6741,6744,6725,
		6747,6728,6749,6730,6746,6727,6743,6724,
	};

	public static final boolean[] DOORS_TYPE = {
		false,false,true,true,false,false,true,true,
		false,false,true,true,false,false,true,true,
		false,false,true,true,false,false,true,true,
		false,false,true,true,false,false,true,true,
	};

	public static final int[] BARROW_CAVE = {
		3523, 9666, 3589, 9735
	};

	public static final int[] SARCOPHAGUS = {
		6821, 6822, 6771, 6823, 6772, 6773
	};

	public static final int[] BARROW_LOOT = {
		4708, 4710, 4712, 4714,
		4716, 4718, 4720, 4722,
		4724, 4726, 4728, 4730,
		4732, 4734, 4736, 4738,
		4745, 4747, 4749, 4751,
		4753, 4755, 4757, 4749,
	};

	public static final int[] NOTED_LOOT = {
		1080, 1114, 1148, 1164,
		1202, 1214, 1276, 1304,
		1320, 1334, 1402, 1404,
	};

	public static final int[] MISC_LOOT = {
		560, 565, 562, 4740, 995
	};

	public static void addLoot(Client c) {
		@SuppressWarnings("unused")
		int slots = c.getItems().freeSlots();
		int chance = Misc.random(BARROWS_LOOT_CHANCE);
		loot[0] = MISC_LOOT[Misc.random(MISC_LOOT.length-1)];
		loot[1] = NOTED_LOOT[Misc.random(NOTED_LOOT.length-1)];
		lootN[0] = Misc.random(1000);
		lootN[1] = Misc.random(3);
		if(chance == 0) {
			loot[0] = MISC_LOOT[Misc.random(MISC_LOOT.length-1)];
			loot[1] = BARROW_LOOT[Misc.random(BARROW_LOOT.length-1)]; 
			lootN[1] = 1;
		} else if(chance == 2) {
			loot[0] = BARROW_LOOT[Misc.random(BARROW_LOOT.length-1)]; 
			lootN[0] = 1;
			loot[1] = BARROW_LOOT[Misc.random(BARROW_LOOT.length-1)]; 
			lootN[1] = 1;
		} else if(chance == 4) {
			loot[0] = NOTED_LOOT[Misc.random(NOTED_LOOT.length-1)];
			lootN[0] = Misc.random(3);
			loot[1] = BARROW_LOOT[Misc.random(BARROW_LOOT.length-1)]; 
			lootN[1] = 1;
		}
		if(loot[0] > 0) {
			c.getItems().addItem(loot[0], lootN[0]);
		}
		if(loot[1] > 0) {
			c.getItems().addItem(loot[1], lootN[1]);
		}
	}
}
