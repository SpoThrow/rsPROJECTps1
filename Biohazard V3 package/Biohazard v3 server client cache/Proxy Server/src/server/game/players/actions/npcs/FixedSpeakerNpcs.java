package server.game.players.actions.npcs;

/**
 * Generated from {@code ActionHandler}'s {@code switch(npcType)} by
 * {@code build/purge/gen-npc-table.js}; do not retype the table by hand.
 *
 * <p>Rows are {@code {npcId, dialogueId, speakerNpcId}}: first click starts that
 * dialogue, but with the speaker written literally in the source rather than taken
 * from the clicked NPC. For most rows the speaker happens to equal the clicked NPC, so
 * this looks redundant — but {@code 495}, {@code 496} and {@code 497} all start
 * dialogue {@code 1000} spoken by NPC {@code 494}, which is why the third column
 * cannot be derived. Generated from the switch; {@code NpcTablesTest} pins it.
 */
public final class FixedSpeakerNpcs {

	static final int[][] DIALOGUES = {
			{ 484, 88, 484 },
			{ 3001, 300, 3001 },
			{ 209, 86, 209 },
			{ 606, 673, 606 },
			{ 1917, 84, 1917 },
			{ 2201, 83, 2201 },
			{ 462, 17, 462 },
			{ 291, 77, 291 },
			{ 545, 999, 545 },
			{ 692, 75, 692 },
			{ 706, 70, 706 },
			{ 599, 63, 599 },
			{ 201, 9001, 201 },
			{ 494, 1000, 494 },
			{ 495, 1000, 494 },
			{ 496, 1000, 494 },
			{ 497, 1000, 494 },
	};

	private FixedSpeakerNpcs() {
	}

	static void register() {
		for (int[] row : DIALOGUES) {
			NpcActionHandler.register(row[0], NpcClick.FIRST, (c, npcType) -> c.getDH().sendDialogues(row[1], row[2]));
		}
	}
}
