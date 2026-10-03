package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the skills cluster after Phase 4.8 moved it off {@link Player} into {@link Skills}.
 *
 * <p>Unlike the earlier clusters, the defaults here are <em>not</em> in the collaborator's
 * own initialisers: a bare {@code new Skills()} is all zeros, and the real starting table
 * (level {@code 1} everywhere, {@code 10} hitpoints, {@code 1300} hitpoints XP) is written
 * by {@link Player}'s constructor. That is pinned from both ends, because the constructor
 * loop was rewritten by the move and is now the only thing making a new player's skills
 * well-formed.
 *
 * <p>Also pinned: that the {@link Player} skill-index constants still fit the array. They
 * are the index space these arrays are read with and they deliberately stayed on
 * {@code Player} (being static), so the coupling between the two classes is real and is
 * the thing most likely to break if either side is touched again.
 */
class SkillsTest {

	private static Client client() {
		return new Client(null, 1);
	}

	/** Every skill-index constant on {@link Player}, in index order. */
	private static int[] indexConstants() {
		return new int[] {
				Player.playerAttack, Player.playerDefence, Player.playerStrength,
				Player.playerHitpoints, Player.playerRanged, Player.playerPrayer,
				Player.playerMagic, Player.playerCooking, Player.playerWoodcutting,
				Player.playerFletching, Player.playerFishing, Player.playerFiremaking,
				Player.playerCrafting, Player.playerSmithing, Player.playerMining,
				Player.playerHerblore, Player.playerAgility, Player.playerThieving,
				Player.playerSlayer, Player.playerFarming, Player.playerRunecrafting,
		};
	}

	@Test
	void aBareSkillsTableIsEmpty() {
		// Not a valid player state -- the constructor seeds it -- but this is what the
		// initialisers declare, and the move must not have quietly seeded them instead.
		final Skills skills = new Skills();

		assertEquals(25, skills.playerLevel.length, "playerLevel must have 25 slots");
		assertEquals(25, skills.playerXP.length, "playerXP must have 25 slots");
		for (int i = 0; i < 25; i++) {
			assertEquals(0, skills.playerLevel[i], "bare playerLevel[" + i + "] should be 0");
			assertEquals(0, skills.playerXP[i], "bare playerXP[" + i + "] should be 0");
		}
		assertEquals(0, skills.totalLevel);
		assertEquals(0, skills.xpTotal);
	}

	@Test
	void theConstructorSeedsEveryLevelToOneExceptHitpoints() {
		final Skills skills = client().skills;

		for (int i = 0; i < skills.playerLevel.length; i++) {
			final int expected = (i == 3) ? 10 : 1;
			assertEquals(expected, skills.playerLevel[i],
					"a new player's level " + i + " should start at " + expected);
		}
	}

	@Test
	void theConstructorSeedsHitpointsExperience() {
		final Skills skills = client().skills;

		assertEquals(1300, skills.playerXP[3], "hitpoints XP starts at the level-10 amount");
		for (int i = 0; i < skills.playerXP.length; i++) {
			if (i != 3) {
				assertEquals(0, skills.playerXP[i], "a new player's XP " + i + " should start at 0");
			}
		}
	}

	@Test
	void theSkillIndexConstantsStillFitTheTable() {
		// The constants are static and stayed on Player; the arrays are here. If either is
		// reordered or resized, the indexing silently goes wrong at runtime.
		final int slots = new Skills().playerLevel.length;
		final int[] constants = indexConstants();

		assertEquals(21, constants.length, "the skill-index constant list changed size");
		for (int i = 0; i < constants.length; i++) {
			assertTrue(constants[i] >= 0 && constants[i] < slots,
					"skill constant " + i + " is " + constants[i]
							+ ", which is outside the " + slots + "-slot table");
			assertEquals(i, constants[i],
					"skill constant " + i + " should equal its own index");
		}
	}

	@Test
	void eachPlayerOwnsItsOwnSkillsAndItsOwnArrays() {
		final Client a = client();
		final Client b = client();

		assertSame(a.skills, a.skills, "the same player must keep one skills object");
		assertNotSame(a.skills, b.skills, "two players must not share skills");
		// A half-shared object would still pass the check above, so the arrays are
		// checked separately.
		assertNotSame(a.skills.playerLevel, b.skills.playerLevel,
				"two players must not share the playerLevel array");
		assertNotSame(a.skills.playerXP, b.skills.playerXP,
				"two players must not share the playerXP array");

		a.skills.playerLevel[1] = 99;
		a.skills.playerXP[1] = 200000000;
		a.skills.totalLevel = 2277;

		assertEquals(1, b.skills.playerLevel[1], "skill levels leaked between players");
		assertEquals(0, b.skills.playerXP[1], "skill XP leaked between players");
		assertEquals(0, b.skills.totalLevel, "the totals leaked between players");
	}
}
