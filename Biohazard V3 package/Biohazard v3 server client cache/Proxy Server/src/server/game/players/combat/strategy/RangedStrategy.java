package server.game.players.combat.strategy;

import server.game.players.combat.CombatAssistant;

public final class RangedStrategy implements CombatStrategy {

	public static final RangedStrategy INSTANCE = new RangedStrategy();

	private RangedStrategy() {
	}

	public int getStyle() {
		return STYLE_RANGED;
	}

	public void executeNpc(CombatAssistant combat, int npcIndex) {
		combat.runNpcAttack(npcIndex);
	}

	public void executePlayer(CombatAssistant combat, int playerIndex) {
		combat.runPlayerAttack(playerIndex);
	}
}
