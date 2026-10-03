package server.game.players;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Pins the attack-mode flags after §4.14 moved them off {@link Player} into {@link AttackMode}.
 *
 * <p>Unlike the neighbouring bags there is no non-zero default to protect here — every flag is
 * {@code false} — so the interesting assertions are ownership and the fact that a fresh player is
 * in <em>no</em> attack mode, which is what the combat code's branch logic assumes.
 */
class AttackModeTest {

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void aFreshPlayerIsInNoAttackMode() {
		final AttackMode m = new AttackMode();

		// The combat code branches on these to pick the melee/ranged/magic path and to compute
		// range, so a fresh player being in no mode is load-bearing, not cosmetic.
		assertFalse(m.usingRangeWeapon, "not attacking with throwing weapons");
		assertFalse(m.usingBow, "not attacking with a bow");
		assertFalse(m.usingMagic, "not attacking with magic");
		assertFalse(m.castingMagic, "not mid-cast");
		assertFalse(m.autocasting, "autocast is off for a fresh player");
	}

	@Test
	void eachPlayerOwnsItsOwnAttackMode() {
		final Client a = client();
		final Client b = client();

		assertSame(a.attackMode, a.attackMode, "the same player must keep one attack mode");
		assertNotSame(a.attackMode, b.attackMode, "two players must not share attack mode");

		a.attackMode.usingMagic = true;
		a.attackMode.autocasting = true;

		assertFalse(b.attackMode.usingMagic, "usingMagic leaked between players");
		assertFalse(b.attackMode.autocasting, "autocasting leaked between players");
	}
}
