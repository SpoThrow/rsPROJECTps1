package server.game.players.actions.npcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Pins the NPC-to-shop tables lifted out of {@code ActionHandler}.
 *
 * <p>Rows are {@code {npcId, shopId}} and a wrong shop id opens the wrong shop with no
 * error, so both tables are asserted in full rather than sampled.
 *
 * <p>Two independent checks were done at migration time. A diff showed the only removed
 * lines were the shop cases (393 first click, 426 second), and the 38 ids that also
 * appear in {@code Data/cfg/npc-shops.cfg} agree with that file on shop id. The merge
 * step additionally cross-checked the two tables against each other and reported them
 * consistent except for {@code 559}.
 */
class ShopNpcsTest {

	private static final int[][] FIRST_CLICK = {
			{ 1301, 81 }, { 537, 77 }, { 675, 76 }, { 239, 74 }, { 3789, 75 },
			{ 694, 73 }, { 588, 102 }, { 2356, 105 }, { 3796, 106 }, { 1860, 107 },
			{ 519, 108 }, { 562, 1010 }, { 581, 1011 }, { 554, 1013 }, { 601, 1014 },
			{ 1039, 1016 }, { 2353, 1017 }, { 3166, 1018 }, { 2161, 1019 }, { 2162, 1020 },
			{ 600, 1021 }, { 603, 1022 }, { 593, 1023 }, { 585, 1025 }, { 2305, 1026 },
			{ 2307, 1027 }, { 2304, 1028 }, { 2306, 1029 }, { 517, 1030 }, { 558, 1031 },
			{ 576, 1032 }, { 1369, 1033 }, { 557, 1034 }, { 1038, 1035 }, { 1433, 1036 },
			{ 584, 1037 }, { 540, 1038 }, { 538, 1040 }, { 1303, 1041 }, { 578, 1042 },
			{ 587, 1043 }, { 1398, 1044 }, { 1865, 1046 }, { 543, 1047 }, { 2198, 1048 },
			{ 580, 1049 }, { 1862, 1050 }, { 559, 9 }, { 583, 1051 }, { 461, 1053 },
			{ 903, 1054 }, { 1435, 1056 }, { 3800, 1057 }, { 2623, 1058 }, { 594, 1059 },
			{ 579, 1060 }, { 2160, 1061 }, { 2191, 1061 }, { 549, 8 }, { 542, 1064 },
			{ 3038, 1065 }, { 544, 1066 }, { 541, 1067 }, { 1434, 1068 }, { 577, 1069 },
			{ 539, 1070 }, { 1980, 1071 }, { 382, 1073 }, { 3541, 1074 }, { 1436, 1076 },
			{ 590, 1077 }, { 971, 1078 }, { 1040, 1080 }, { 563, 1081 }, { 522, 1082 },
			{ 524, 1083 }, { 526, 1084 }, { 2154, 1085 }, { 1334, 1086 }, { 2552, 1087 },
			{ 528, 1088 }, { 1254, 1089 }, { 2086, 1090 }, { 3824, 1091 }, { 1866, 1092 },
			{ 1699, 1093 }, { 1282, 1094 }, { 516, 1096 }, { 560, 1097 }, { 471, 1098 },
			{ 1208, 1099 }, { 532, 1100 }, { 534, 1102 }, { 836, 1103 }, { 551, 1104 },
			{ 586, 1105 }, { 564, 1106 }, { 573, 1108 }, { 1316, 1108 }, { 547, 1108 },
			{ 1787, 1110 }, { 1696, 47 },
	};

	private static final int[][] SECOND_CLICK = {
			{ 1301, 81 }, { 553, 6 }, { 537, 77 }, { 3789, 75 }, { 209, 22 },
			{ 1917, 21 }, { 2620, 20 }, { 2622, 19 }, { 1696, 47 }, { 545, 13 },
			{ 1658, 14 }, { 692, 11 }, { 520, 2 }, { 548, 24 }, { 546, 3 },
			{ 561, 4 }, { 595, 5 }, { 550, 7 }, { 549, 8 }, { 588, 102 },
			{ 2356, 105 }, { 3796, 106 }, { 1860, 107 }, { 519, 108 }, { 562, 1010 },
			{ 581, 1011 }, { 554, 1013 }, { 601, 1014 }, { 1039, 1016 }, { 2353, 1017 },
			{ 3166, 1018 }, { 559, 109 }, { 2161, 1019 }, { 2162, 1020 }, { 600, 1021 },
			{ 603, 1022 }, { 593, 1023 }, { 585, 1025 }, { 2305, 1026 }, { 2307, 1027 },
			{ 2304, 1028 }, { 2306, 1029 }, { 517, 1030 }, { 558, 1031 }, { 576, 1032 },
			{ 1369, 1033 }, { 557, 1034 }, { 1038, 1035 }, { 1433, 1036 }, { 584, 1037 },
			{ 540, 1038 }, { 538, 1040 }, { 1303, 1041 }, { 578, 1042 }, { 587, 1043 },
			{ 1398, 1044 }, { 1865, 1046 }, { 543, 1047 }, { 2198, 1048 }, { 580, 1049 },
			{ 1862, 1050 }, { 583, 1051 }, { 461, 1053 }, { 903, 1054 }, { 1435, 1056 },
			{ 3800, 1057 }, { 2623, 1058 }, { 594, 1059 }, { 579, 1060 }, { 2160, 1061 },
			{ 2191, 1061 }, { 542, 1064 }, { 3038, 1065 }, { 544, 1066 }, { 541, 1067 },
			{ 1434, 1068 }, { 577, 1069 }, { 539, 1070 }, { 1980, 1071 }, { 382, 1073 },
			{ 3541, 1074 }, { 1436, 1076 }, { 590, 1077 }, { 971, 1078 }, { 1040, 1080 },
			{ 563, 1081 }, { 522, 1082 }, { 524, 1083 }, { 526, 1084 }, { 2154, 1085 },
			{ 1334, 1086 }, { 2552, 1087 }, { 528, 1088 }, { 1254, 1089 }, { 2086, 1090 },
			{ 3824, 1091 }, { 1866, 1092 }, { 1699, 1093 }, { 1282, 1094 }, { 516, 1096 },
			{ 560, 1097 }, { 471, 1098 }, { 1208, 1099 }, { 532, 1100 }, { 534, 1102 },
			{ 836, 1103 }, { 551, 1104 }, { 586, 1105 }, { 564, 1106 }, { 573, 1108 },
			{ 1316, 1108 }, { 547, 1108 }, { 1787, 1110 },
	};

	@Test
	void tablesMatchTheSwitchTheyReplaced() {
		assertTable(FIRST_CLICK, ShopNpcs.FIRST_CLICK);
		assertTable(SECOND_CLICK, ShopNpcs.SECOND_CLICK);
	}

	@Test
	void everyShopNpcIsRegisteredOnTheRightClick() {
		for (int[] row : FIRST_CLICK) {
			assertTrue(NpcActionHandler.isRegistered(row[0], NpcClick.FIRST),
					"npc " + row[0] + " has no first-click shop handler");
		}
		for (int[] row : SECOND_CLICK) {
			assertTrue(NpcActionHandler.isRegistered(row[0], NpcClick.SECOND),
					"npc " + row[0] + " has no second-click shop handler");
		}
	}

	@Test
	void noNpcAppearsTwiceWithinOneTable() {
		Set<Integer> seen = new HashSet<>();
		for (int[] row : FIRST_CLICK) {
			assertTrue(seen.add(row[0]), "npc " + row[0] + " appears twice in FIRST_CLICK");
		}
		assertEquals(102, FIRST_CLICK.length);
		seen.clear();
		for (int[] row : SECOND_CLICK) {
			assertTrue(seen.add(row[0]), "npc " + row[0] + " appears twice in SECOND_CLICK");
		}
		assertEquals(113, SECOND_CLICK.length);
	}

	@Test
	void npc559OpensADifferentShopOnEachClick() {
		// The one genuine divergence between the two tables: 559 opens shop 9 on first
		// click but 109 on second. Easy to mistake for a copy-paste bug, so it is pinned
		// rather than "fixed" — the switch really did say this.
		Map<Integer, Integer> second = new HashMap<>();
		for (int[] row : SECOND_CLICK) {
			second.put(row[0], row[1]);
		}
		for (int[] row : FIRST_CLICK) {
			if (row[0] == 559) {
				assertEquals(9, row[1], "559 first click");
				assertEquals(109, second.get(559), "559 second click");
			}
		}
	}

	@Test
	void theTwoTablesOverlapButAreNotIdentical() {
		Set<Integer> first = new HashSet<>();
		for (int[] row : FIRST_CLICK) {
			first.add(row[0]);
		}
		Set<Integer> second = new HashSet<>();
		for (int[] row : SECOND_CLICK) {
			second.add(row[0]);
		}
		long both = first.stream().filter(second::contains).count();
		assertEquals(99, both, "ids in both tables");
		assertEquals(3, first.size() - both, "first-click-only ids");
		assertEquals(14, second.size() - both, "second-click-only ids");
	}

	private static void assertTable(int[][] expected, int[][] actual) {
		assertEquals(expected.length, actual.length, "row count");
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i].length, actual[i].length, "row " + i + " width");
			for (int col = 0; col < expected[i].length; col++) {
				assertEquals(expected[i][col], actual[i][col],
						"row " + i + " column " + col + " (npc " + expected[i][0] + ")");
			}
		}
	}
}
