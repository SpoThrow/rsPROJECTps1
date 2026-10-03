package server.game.players.combat.strategy;

import server.game.players.combat.CombatAssistant;

public final class MagicStrategy implements CombatStrategy {

	public static final MagicStrategy INSTANCE = new MagicStrategy();

	private MagicStrategy() {
	}

	public int getStyle() {
		return STYLE_MAGIC;
	}

	public void executeNpc(CombatAssistant combat, int npcIndex) {
		combat.runNpcAttack(npcIndex);
	}

	public void executePlayer(CombatAssistant combat, int playerIndex) {
		combat.runPlayerAttack(playerIndex);
	}
}
