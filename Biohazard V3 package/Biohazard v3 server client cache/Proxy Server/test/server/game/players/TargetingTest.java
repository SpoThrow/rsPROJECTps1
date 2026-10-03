package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import server.game.npcs.NPC;

/**
 * Pins the combat targeting pointers after §4.16 moved them off {@link Player} into
 * {@link Targeting}.
 *
 * <p>Two things are worth protecting here. The first is that every pointer defaults to {@code 0},
 * because the whole cluster uses {@code 0} as "no target" while the indices themselves are 1-based
 * — so a stray non-zero default would read as "already attacking slot N". The second, and the
 * reason this pass needed care at all, is that ⚠️ <b>{@code underAttackBy} also exists on
 * {@link NPC}</b>, with the mirrored meaning. The rewriter had to tell the two apart by type, and
 * these tests pin the outcome from both directions: the name really is declared on {@code NPC},
 * and it really is gone from {@code Player}.
 */
class TargetingTest {

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void aFreshPlayerTargetsNothing() {
		final Targeting t = new Targeting();

		// `0` is the sentinel for "no target" / "not under attack"; the real indices are 1-based.
		// The multi-combat checks branch on `> 0`, so a non-zero default here would make a fresh
		// player look like they were already in a fight.
		assertEquals(0, t.playerIndex, "no player target");
		assertEquals(0, t.npcIndex, "no NPC target");
		assertEquals(0, t.oldPlayerIndex, "no previous player target");
		assertEquals(0, t.oldNpcIndex, "no previous NPC target");
		assertEquals(0, t.killingNpcIndex, "no pending kill credit");
		assertEquals(0, t.underAttackBy, "not under attack");
		assertEquals(0, t.underAttackBy2, "no second attacker");
		assertEquals(0, t.ssTarget, "no Soul Split player target");
		assertEquals(0, t.ssTargetNpc, "no Soul Split NPC target");
	}

	@Test
	void eachPlayerOwnsItsOwnTargeting() {
		final Client a = client();
		final Client b = client();

		assertSame(a.targeting, a.targeting, "the same player must keep one targeting bag");
		assertNotSame(a.targeting, b.targeting, "two players must not share targeting");

		a.targeting.npcIndex = 42;
		a.targeting.underAttackBy = 7;

		assertEquals(0, b.targeting.npcIndex, "npcIndex leaked between players");
		assertEquals(0, b.targeting.underAttackBy, "underAttackBy leaked between players");
	}

	/**
	 * The §4.16 hazard, pinned reflectively: {@code underAttackBy} was <em>not</em> moved blindly.
	 * It is still declared on {@link NPC} (the NPC's side of the same idea, which is why the
	 * rewriter skipped 11 references), and on the player side it now lives on {@link Targeting}
	 * rather than {@link Player}.
	 */
	@Test
	void underAttackByIsDeclaredOnBothSides() throws Exception {
		assertTrue(NPC.class.getDeclaredField("underAttackBy").getType() == int.class,
				"NPC must keep its own underAttackBy; the rewriter deliberately left its uses alone");
		assertTrue(Targeting.class.getDeclaredField("underAttackBy").getType() == int.class,
				"the player's underAttackBy must now live on Targeting");
		assertThrows(NoSuchFieldException.class, () -> Player.class.getDeclaredField("underAttackBy"),
				"underAttackBy must no longer be declared on Player");
	}

	@Test
	void theMovedPointersAreGoneFromPlayer() {
		for (final String name : new String[] { "playerIndex", "npcIndex", "oldPlayerIndex",
				"oldNpcIndex", "killingNpcIndex", "underAttackBy", "underAttackBy2", "ssTarget",
				"ssTargetNpc" }) {
			assertThrows(NoSuchFieldException.class, () -> Player.class.getDeclaredField(name),
					name + " must no longer be declared on Player");
		}
		assertFalse(java.util.Arrays.stream(Targeting.class.getDeclaredFields())
				.anyMatch(f -> f.getName().equals("ssHeal")),
				"ssHeal is the heal counter, not a target, and must stay on Player");
	}
}
