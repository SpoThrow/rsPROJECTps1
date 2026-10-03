package server.game.players.actions.npcs;

/**
 * Generated from {@code ActionHandler}'s {@code switch(npcType)} by
 * {@code build/purge/gen-npc-table.js}; do not retype the table by hand.
 *
 * <p>Rows are {@code {npcId, dialogueId}}: first click starts that dialogue, spoken by
 * the NPC itself (the switch passed {@code npcType} as the speaker). Generated from the
 * switch, not retyped; {@code NpcTablesTest} pins it.
 */
public final class TalkNpcs {

	static final int[][] DIALOGUES = {
			{ 663, 750 },
			{ 213, 555 },
			{ 216, 557 },
			{ 377, 559 },
			{ 376, 561 },
			{ 1704, 563 },
			{ 1304, 565 },
			{ 231, 567 },
			{ 263, 569 },
			{ 2139, 571 },
			{ 1182, 573 },
			{ 647, 577 },
			{ 608, 579 },
			{ 741, 581 },
			{ 746, 583 },
			{ 1841, 585 },
			{ 2637, 587 },
			{ 1703, 589 },
			{ 514, 591 },
			{ 515, 593 },
			{ 2580, 595 },
			{ 5113, 551 },
			{ 4247, 549 },
			{ 2157, 505 },
			{ 664, 502 },
			{ 261, 487 },
			{ 511, 486 },
			{ 510, 488 },
			{ 693, 483 },
			{ 4297, 444 },
			{ 4288, 445 },
			{ 705, 449 },
			{ 961, 447 },
			{ 1658, 455 },
			{ 802, 451 },
			{ 682, 453 },
			{ 1599, 442 },
			{ 3295, 427 },
			{ 604, 429 },
			{ 308, 431 },
			{ 4946, 435 },
			{ 847, 433 },
			{ 575, 425 },
			{ 805, 423 },
			{ 3299, 439 },
			{ 4906, 437 },
			{ 455, 417 },
			{ 2270, 419 },
			{ 437, 415 },
			{ 905, 5 },
			{ 460, 3 },
			{ 520, 999 },
			{ 812, 998 },
			{ 546, 1004 },
			{ 548, 1008 },
			{ 641, 997 },
			{ 530, 996 },
			// These three write the call as `sendDialogues(N,npcType)` with no space, so
			// the original extraction pattern missed them and they were left behind.
			{ 1526, 55 },
			{ 589, 56 },
			{ 1152, 16 },
	};

	// One second-click talker, from the second-click switch.
	static final int[][] SECOND_CLICK = {
			{ 2157, 505 },
	};

	private TalkNpcs() {
	}

	static void register() {
		for (int[] row : DIALOGUES) {
			NpcActionHandler.register(row[0], NpcClick.FIRST,
					(c, npcType) -> c.getDH().sendDialogues(row[1], npcType));
		}
		for (int[] row : SECOND_CLICK) {
			NpcActionHandler.register(row[0], NpcClick.SECOND,
					(c, npcType) -> c.getDH().sendDialogues(row[1], npcType));
		}
	}
}
