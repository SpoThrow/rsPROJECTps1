package server.game.players.combat.strategy;

import server.game.items.ItemAssistant;
import server.game.players.Client;

/**
 * Resolves melee / ranged / magic from current equipment and spell state.
 * Rollback: callers can invoke {@code CombatAssistant.runNpcAttack} directly.
 */
public final class CombatStrategies {

	private CombatStrategies() {
	}

	public static CombatStrategy resolve(Client c) {
		if (c == null) {
			return MeleeStrategy.INSTANCE;
		}
		if (c.attackMode.autocasting || c.magic.spellId > 0 || c.attackMode.usingMagic) {
			return MagicStrategy.INSTANCE;
		}
		if (isUsingRanged(c)) {
			return RangedStrategy.INSTANCE;
		}
		return MeleeStrategy.INSTANCE;
	}

	private static boolean isUsingRanged(Client c) {
		if (c.playerEquipment[c.playerWeapon] == 9185) {
			return true;
		}
		if (c.BOWS != null) {
			for (int i = 0; i < c.BOWS.length; i++) {
				if (c.playerEquipment[c.playerWeapon] == c.BOWS[i]) {
					return true;
				}
			}
		}
		String name = ItemAssistant.getItemName(c.playerEquipment[c.playerWeapon]);
		if (name == null) {
			return false;
		}
		String lower = name.toLowerCase();
		return lower.indexOf("javelin") != -1 || lower.indexOf("dart") != -1
				|| lower.indexOf("thrownaxe") != -1 || lower.indexOf("knife") != -1;
	}
}
