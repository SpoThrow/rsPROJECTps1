package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Pins the special-attack state after §4.13 moved it off {@link Player} into
 * {@link SpecialAttack}.
 *
 * <p>The point of interest is that two of the defaults are non-zero <em>multipliers</em>, not
 * sentinels: {@code specAccuracy} and {@code specDamage} start at {@code 1} because the combat
 * maths multiplies by them, and {@code 1} is the identity. A default of {@code 0} would zero
 * every player's accuracy and every hit, so that is asserted explicitly rather than left to the
 * reflective all-defaults check.
 */
class SpecialAttackTest {

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void aFreshPlayersDefaultsAreTheLoadBearingOnes() {
		final SpecialAttack s = new SpecialAttack();

		// The two identity multipliers: 1 means "no special attack in progress".
		assertEquals(1.0, s.specAccuracy, 0.0, "specAccuracy's 1.0 is the identity multiplier");
		assertEquals(1.0, s.specDamage, 0.0, "specDamage's 1.0 is the identity multiplier");

		// Zero / false for the rest.
		assertEquals(0.0, s.specAmount, 0.0, "a fresh player has no special-attack charge");
		assertEquals(0, s.specBarId, "no weapon's spec bar is cached yet");
		assertEquals(0, s.specEffect, "specEffect 0 means 'no special in flight'");
		assertEquals(0, s.specMaxHitIncrease, "no max-hit bonus is active");
		assertFalse(s.usingSpecial, "the spec toggle starts off");
		assertFalse(s.doubleHit, "no double hit is pending");
	}

	@Test
	void theMultipliersAreNoOpsForAFreshPlayer() {
		// This is the actual hazard: the combat maths does `attackLevel *= specAccuracy` and
		// `maxHit = (int)(maxHit * specDamage)`. At the defaults both must be no-ops.
		final SpecialAttack s = client().specialAttack;

		final double attackLevel = 99.0;
		final int maxHit = 37;

		assertEquals(attackLevel, attackLevel * s.specAccuracy, 0.0,
				"the default accuracy multiplier must not change the attack level");
		assertEquals(maxHit, (int) (maxHit * s.specDamage),
				"the default damage multiplier must not change the max hit");
	}

	@Test
	void eachPlayerOwnsItsOwnSpecialAttackState() {
		final Client a = client();
		final Client b = client();

		assertSame(a.specialAttack, a.specialAttack, "the same player must keep one spec state");
		assertNotSame(a.specialAttack, b.specialAttack, "two players must not share spec state");

		a.specialAttack.specAmount = 100;
		a.specialAttack.specAccuracy = 1.85;
		a.specialAttack.specDamage = 1.5;
		a.specialAttack.specEffect = 4;
		a.specialAttack.specBarId = 7486;
		a.specialAttack.specMaxHitIncrease = 3;
		a.specialAttack.usingSpecial = true;
		a.specialAttack.doubleHit = true;

		assertEquals(0.0, b.specialAttack.specAmount, 0.0, "the charge leaked between players");
		assertEquals(1.0, b.specialAttack.specAccuracy, 0.0, "the accuracy multiplier leaked");
		assertEquals(1.0, b.specialAttack.specDamage, 0.0, "the damage multiplier leaked");
		assertEquals(0, b.specialAttack.specEffect, "specEffect leaked between players");
		assertEquals(0, b.specialAttack.specBarId, "specBarId leaked between players");
		assertEquals(0, b.specialAttack.specMaxHitIncrease, "the max-hit bonus leaked");
		assertFalse(b.specialAttack.usingSpecial, "usingSpecial leaked between players");
		assertFalse(b.specialAttack.doubleHit, "doubleHit leaked between players");
	}

	@Test
	void theChargeSurvivesThePersistenceRoundTripShape() {
		// PlayerSave writes specAmount as a Double under "special-amount" and reads it back the
		// same way, so the field must stay a double that round-trips exactly.
		final SpecialAttack s = new SpecialAttack();
		s.specAmount = 55.0;
		assertEquals(55.0, Double.parseDouble(Double.toString(s.specAmount)), 0.0,
				"specAmount must round-trip through its save format unchanged");
	}
}
