package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import server.game.npcs.NPC;

/**
 * Pins the ranged-attack state after §4.18 moved it off {@link Player} into {@link RangedAttack}.
 *
 * <p>The defaults matter because a non-zero {@code projectileStage} would make a fresh player look
 * mid-shot, and the {@code 0} "no weapon remembered" value is what {@code lastWeaponUsed} is
 * compared against to detect the dark bow. The reflective half pins the property that made this
 * pass collision-free: <b>none of these five names is declared on {@code NPC}</b>, so unlike §4.16
 * and §4.17 there was nothing to tell apart by receiver type.
 */
class RangedAttackTest {

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void aFreshPlayerHasNoShotInFlight() {
		final RangedAttack r = new RangedAttack();

		assertEquals(0, r.projectileStage, "no projectile is in flight");
		assertEquals(0, r.rangeItemUsed, "no ammo was spent");
		assertEquals(0, r.lastWeaponUsed, "no weapon was launched");
		assertEquals(0, r.crystalBowArrowCount, "the crystal bow has not been fired");
		assertEquals(0, r.bowSpecShot, "no bow special is in progress");
	}

	@Test
	void eachPlayerOwnsItsOwnRangedAttack() {
		final Client a = client();
		final Client b = client();

		assertSame(a.rangedAttack, a.rangedAttack, "the same player must keep one ranged-attack bag");
		assertNotSame(a.rangedAttack, b.rangedAttack, "two players must not share ranged-attack state");

		a.rangedAttack.projectileStage = 1;
		a.rangedAttack.crystalBowArrowCount = 249;
		a.rangedAttack.lastWeaponUsed = 11235;

		assertEquals(0, b.rangedAttack.projectileStage, "projectileStage leaked between players");
		assertEquals(0, b.rangedAttack.crystalBowArrowCount, "the crystal-bow counter leaked between players");
		assertEquals(0, b.rangedAttack.lastWeaponUsed, "lastWeaponUsed leaked between players");
	}

	@Test
	void theFieldsAreGoneFromPlayerButOnTheBag() throws Exception {
		for (final String name : new String[] { "projectileStage", "rangeItemUsed", "lastWeaponUsed",
				"crystalBowArrowCount", "bowSpecShot" }) {
			assertThrows(NoSuchFieldException.class, () -> Player.class.getDeclaredField(name),
					name + " must no longer be declared on Player");
			assertEquals(int.class, fieldType(RangedAttack.class, name), name + " must now live on RangedAttack");
		}
	}

	/**
	 * The reason §4.18 had zero skipped references: {@code NPC} does not declare any of these names.
	 * If a future change adds one, this test fails and the next pass knows to look for collisions.
	 */
	@Test
	void noneOfTheseNamesExistOnNpc() {
		for (final String name : new String[] { "projectileStage", "rangeItemUsed", "lastWeaponUsed",
				"crystalBowArrowCount", "bowSpecShot" }) {
			assertThrows(NoSuchFieldException.class, () -> NPC.class.getDeclaredField(name),
					name + " unexpectedly exists on NPC; a future pass must resolve it by receiver type");
		}
	}

	private static Class<?> fieldType(Class<?> type, String name) throws Exception {
		return type.getDeclaredField(name).getType();
	}
}
