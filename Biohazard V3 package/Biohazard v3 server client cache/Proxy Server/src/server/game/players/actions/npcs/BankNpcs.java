package server.game.players.actions.npcs;

/**
 * NPCs whose second click opens the bank, lifted out of {@code ActionHandler}'s
 * second-click switch.
 *
 * <p>Two fall-through groups collapsed to one list: {@code 494-499} shared a body and
 * {@code 958} was on its own. The handler takes no arguments, so the ids are just a
 * list.
 *
 * <p>Note {@code 494-497} also have a *first*-click handler (they start a dialogue, in
 * {@code FixedSpeakerNpcs}). Different click, so no conflict — but it is why the key
 * has to be {@code (npcType, click)} rather than {@code npcType}.
 */
public final class BankNpcs {

	private static final int[] IDS = { 958, 494, 495, 496, 497, 498, 499 };

	private BankNpcs() {
	}

	static void register() {
		for (int id : IDS) {
			NpcActionHandler.register(id, NpcClick.SECOND,
					(c, npcType) -> c.getPA().openUpBank());
		}
	}

	static int[] ids() {
		return IDS.clone();
	}
}
