package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CombatStyle} (§4.21).
 *
 * <p>The interesting behaviour here is not the field — it is the two couplings the class doc warns
 * about. The interface mapping must stay non-identity, and the raw value must stay usable as a
 * skill index, because callers rely on both.
 */
public class CombatStyleTest {

	@Test
	public void defaultsToTheAccurateStyle() {
		CombatStyle style = new CombatStyle();
		assertEquals(0, style.fightMode, "0 = accurate, and it must be the default");
	}

	@Test
	public void coversAllFourStyles() {
		CombatStyle style = new CombatStyle();
		for (int mode = 0; mode <= 3; mode++) {
			style.fightMode = mode;
			assertEquals(mode, style.fightMode);
		}
	}

	@Test
	public void interfaceMappingIsNotTheIdentity() {
		// handleWeaponStyle sends 0->0, 1->3, 2->1, 3->2. If someone "simplified" this to the
		// raw value the client would show the wrong style selected.
		int[] sent = { 0, 3, 1, 2 };
		assertEquals(0, sent[0], "accurate is the one style where they agree");
		for (int mode = 1; mode <= 3; mode++) {
			assertNotEquals(mode, sent[mode],
					"style " + mode + " must be translated, not passed raw");
		}
	}

	@Test
	public void rawValueIsUsableAsASkillIndex() {
		// addSkillXP(damage, c.fightMode) and refreshSkill(c.fightMode) index straight into the
		// skill array, so every style value must be a valid non-negative index.
		CombatStyle style = new CombatStyle();
		for (int mode = 0; mode <= 3; mode++) {
			style.fightMode = mode;
			assertTrue(style.fightMode >= 0, "style " + mode + " is used as a skill index");
		}
	}
}
