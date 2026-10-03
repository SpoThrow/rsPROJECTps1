package server.game.players.combat.strategy;

import server.game.players.combat.CombatAssistant;

public final class MeleeStrategy implements CombatStrategy {

	public static final MeleeStrategy INSTANCE = new MeleeStrategy();

	private MeleeStrategy() {
	}

	public int getStyle() {
		return STYLE_MELEE;
	}

	public void executeNpc(CombatAssistant combat, int npcIndex) {
		combat.runNpcAttack(npcIndex);
	}

	public void executePlayer(CombatAssistant combat, int playerIndex) {
		combat.runPlayerAttack(playerIndex);
	}
}
