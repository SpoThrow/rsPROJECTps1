package server.game.players.combat.strategy;

import server.game.players.combat.CombatAssistant;

/**
 * Incremental combat style extraction. Day-one strategies delegate into
 * {@link CombatAssistant#runNpcAttack(int)} / {@link CombatAssistant#runPlayerAttack(int)}.
 * Later PRs move style-specific logic into the strategy bodies.
 */
public interface CombatStrategy {

	int STYLE_MELEE = 0;
	int STYLE_RANGED = 1;
	int STYLE_MAGIC = 2;

	int getStyle();

	void executeNpc(CombatAssistant combat, int npcIndex);

	void executePlayer(CombatAssistant combat, int playerIndex);
}
