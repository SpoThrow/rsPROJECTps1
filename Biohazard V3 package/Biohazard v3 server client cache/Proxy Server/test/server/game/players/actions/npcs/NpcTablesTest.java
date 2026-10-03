package server.game.players.actions.npcs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Pins the four NPC tables lifted out of {@code ActionHandler} in one pass:
 * {@link TalkNpcs}, {@link FixedSpeakerNpcs}, {@link TeleportNpcs} and
 * {@link FishingNpcs}.
 *
 * <p>Each table was generated from the switch rather than retyped, and the source
 * deletion was checked by enumerating the removed case labels. These assertions exist
 * to catch later drift.
 */
class NpcTablesTest {

	private static final int[][] TALK = {
			{ 663, 750 }, { 213, 555 }, { 216, 557 }, { 377, 559 }, { 376, 561 },
			{ 1704, 563 }, { 1304, 565 }, { 231, 567 }, { 263, 569 }, { 2139, 571 },
			{ 1182, 573 }, { 647, 577 }, { 608, 579 }, { 741, 581 }, { 746, 583 },
			{ 1841, 585 }, { 2637, 587 }, { 1703, 589 }, { 514, 591 }, { 515, 593 },
			{ 2580, 595 }, { 5113, 551 }, { 4247, 549 }, { 2157, 505 }, { 664, 502 },
			{ 261, 487 }, { 511, 486 }, { 510, 488 }, { 693, 483 }, { 4297, 444 },
			{ 4288, 445 }, { 705, 449 }, { 961, 447 }, { 1658, 455 }, { 802, 451 },
			{ 682, 453 }, { 1599, 442 }, { 3295, 427 }, { 604, 429 }, { 308, 431 },
			{ 4946, 435 }, { 847, 433 }, { 575, 425 }, { 805, 423 }, { 3299, 439 },
			{ 4906, 437 }, { 455, 417 }, { 2270, 419 }, { 437, 415 }, { 905, 5 },
			{ 460, 3 }, { 520, 999 }, { 812, 998 }, { 546, 1004 }, { 548, 1008 },
			{ 641, 997 }, { 530, 996 },
			{ 1526, 55 }, { 589, 56 }, { 1152, 16 },
	};

	private static final int[][] TALK_SECOND = { { 2157, 505 } };

	private static final int[][] FIXED_SPEAKER = {
			{ 484, 88, 484 }, { 3001, 300, 3001 }, { 209, 86, 209 }, { 606, 673, 606 },
			{ 1917, 84, 1917 }, { 2201, 83, 2201 }, { 462, 17, 462 }, { 291, 77, 291 },
			{ 545, 999, 545 }, { 692, 75, 692 }, { 706, 70, 706 }, { 599, 63, 599 },
			{ 201, 9001, 201 }, { 494, 1000, 494 }, { 495, 1000, 494 },
			{ 496, 1000, 494 }, { 497, 1000, 494 },
	};

	private static final int[][] TELEPORTS = {
			{ 378, 2977, 9515, 1 },
			{ 556, 2872, 5269, 2 },
			{ 2257, 2919, 5274, 0 },
			{ 2259, 2885, 5344, 2 },
	};

	private static final int[] FISHING = { 309, 312, 313, 316, 326 };

	private static final int[] PICKPOCKET = {
			2234, 2235, 1, 2, 3, 4, 5, 6, 7, 1757, 1758, 1759, 1760, 1761, 1715, 1714,
			1710, 1711, 1712, 15, 18, 187, 9, 10, 1880, 1881, 1926, 1927, 1928, 1929,
			1930, 1931, 23, 26, 1883, 1884, 32, 1904, 1905, 20, 365, 2256, 66, 67, 68, 21,
	};

	private static final int[] BANK = { 958, 494, 495, 496, 497, 498, 499 };

	@Test
	void tablesMatchTheSwitchTheyReplaced() {
		assertTable(TALK, TalkNpcs.DIALOGUES);
		assertTable(TALK_SECOND, TalkNpcs.SECOND_CLICK);
		assertTable(FIXED_SPEAKER, FixedSpeakerNpcs.DIALOGUES);
		assertTable(TELEPORTS, TeleportNpcs.TELEPORTS);
		assertArrayEquals(new int[] { 2258 }, TeleportNpcs.ANIMATED);
	}

	@Test
	void thirdClickTablesMatchTheSwitch() {
		int[][] shops = ShopNpcs.THIRD_CLICK;
		int[][] expectedShops = {
				{ 70, 1109 }, { 1596, 1109 }, { 1597, 1109 }, { 1598, 1109 }, { 1599, 1109 },
				{ 836, 1103 }, { 1526, 78 },
		};
		assertTable(expectedShops, shops);
		int[][] teleports = TeleportNpcs.THIRD_CLICK;
		assertEquals(1, teleports.length);
		assertEquals(553, teleports[0][0]);
		for (int[] row : expectedShops) {
			assertTrue(NpcActionHandler.isRegistered(row[0], NpcClick.THIRD), "third shop " + row[0]);
		}
		assertTrue(NpcActionHandler.isRegistered(553, NpcClick.THIRD), "third teleport 553");
	}

	@Test
	void everyNpcIsRegisteredOnFirstClick() {
		for (int[] row : TALK) {
			assertTrue(NpcActionHandler.isRegistered(row[0], NpcClick.FIRST), "talk " + row[0]);
		}
		for (int[] row : FIXED_SPEAKER) {
			assertTrue(NpcActionHandler.isRegistered(row[0], NpcClick.FIRST), "speaker " + row[0]);
		}
		for (int[] row : TELEPORTS) {
			assertTrue(NpcActionHandler.isRegistered(row[0], NpcClick.FIRST), "teleport " + row[0]);
		}
		for (int id : FISHING) {
			assertTrue(NpcActionHandler.isRegistered(id, NpcClick.FIRST), "fishing first " + id);
		}
	}

	@Test
	void fishingSpotsAreAlsoRegisteredOnSecondClick() {
		// The five fishing ids appear in both switches: first click passes false, second
		// passes true. Collapsing them into one click would lose the bait/lure variant.
		for (int id : FISHING) {
			assertTrue(NpcActionHandler.isRegistered(id, NpcClick.SECOND),
					"fishing id " + id + " must also handle second click");
		}
	}

	@Test
	void talkNpcsThatAlsoHaveASecondClickAreExactlyTheExpectedOnes() {
		// A handler for the same npc on both clicks is normal, not a conflict: these
		// NPCs start a dialogue on first click and open a shop (or the bank) on second.
		// The (npcType, NpcClick) key exists precisely so both can coexist. Asserting
		// the exact sets means a future family silently claiming one of these ids on the
		// wrong click shows up here.
		assertEquals(Set.of(520, 546, 548, 1658, 2157), idsWithSecondClick(TALK),
				"TalkNpcs ids that also have a second-click handler");
		assertEquals(Set.of(209, 494, 495, 496, 497, 545, 692, 1917), idsWithSecondClick(FIXED_SPEAKER),
				"FixedSpeakerNpcs ids that also have a second-click handler");
	}

	private static Set<Integer> idsWithSecondClick(int[][] rows) {
		Set<Integer> ids = new HashSet<>();
		for (int[] row : rows) {
			if (NpcActionHandler.isRegistered(row[0], NpcClick.SECOND)) {
				ids.add(row[0]);
			}
		}
		return ids;
	}

	@Test
	void theLiteralSpeakerIsNotAlwaysTheNpcItself() {
		// 495/496/497 start dialogue 1000 spoken by 494, so the third column cannot be
		// derived from the first. This is the whole reason the column exists.
		for (int[] row : FIXED_SPEAKER) {
			if (row[0] == 495 || row[0] == 496 || row[0] == 497) {
				assertEquals(494, row[2], "row " + row[0] + " speaker");
			}
		}
	}

	@Test
	void pickpocketAndBankIdsMatchTheSwitch() {
		assertArrayEquals(PICKPOCKET, PickpocketNpcs.ids());
		assertArrayEquals(BANK, BankNpcs.ids());
		for (int id : PICKPOCKET) {
			assertTrue(NpcActionHandler.isRegistered(id, NpcClick.SECOND), "pickpocket " + id);
		}
		for (int id : BANK) {
			assertTrue(NpcActionHandler.isRegistered(id, NpcClick.SECOND), "bank " + id);
		}
	}

	@Test
	void noNpcIsClaimedByTwoFirstClickTables() {
		// register() would have thrown at class-load if this were violated, so this is
		// documentation as much as a guard: it makes the invariant explicit.
		Set<Integer> seen = new HashSet<>();
		for (int[] row : ShopNpcs.FIRST_CLICK) {
			assertTrue(seen.add(row[0]), "npc " + row[0] + " is in two first-click tables");
		}
		for (int[] row : TALK) {
			assertTrue(seen.add(row[0]), "npc " + row[0] + " is in two first-click tables");
		}
		for (int[] row : FIXED_SPEAKER) {
			assertTrue(seen.add(row[0]), "npc " + row[0] + " is in two first-click tables");
		}
		for (int[] row : TELEPORTS) {
			assertTrue(seen.add(row[0]), "npc " + row[0] + " is in two first-click tables");
		}
		for (int id : TeleportNpcs.ANIMATED) {
			assertTrue(seen.add(id), "npc " + id + " is in two first-click tables");
		}
		for (int id : FISHING) {
			assertTrue(seen.add(id), "npc " + id + " is in two first-click tables");
		}
		assertEquals(102 + 60 + 17 + 4 + 1 + 5, seen.size());
	}

	@Test
	void noNpcIsClaimedByTwoSecondClickTables() {
		Set<Integer> seen = new HashSet<>();
		for (int[] row : ShopNpcs.SECOND_CLICK) {
			assertTrue(seen.add(row[0]), "npc " + row[0] + " is in two second-click tables");
		}
		for (int id : PICKPOCKET) {
			assertTrue(seen.add(id), "npc " + id + " is in two second-click tables");
		}
		for (int id : BANK) {
			assertTrue(seen.add(id), "npc " + id + " is in two second-click tables");
		}
		for (int id : FISHING) {
			assertTrue(seen.add(id), "npc " + id + " is in two second-click tables");
		}
		for (int[] row : TalkNpcs.SECOND_CLICK) {
			assertTrue(seen.add(row[0]), "npc " + row[0] + " is in two second-click tables");
		}
		assertEquals(113 + 46 + 7 + 5 + 1, seen.size());
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
