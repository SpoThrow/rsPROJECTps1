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
 * Pins the hit bookkeeping after §4.17 moved it off {@link Player} into {@link HitUpdate}.
 *
 * <p>Three things are worth protecting. The defaults, because a stray {@code hitDiff} would paint a
 * splat on every spawn and a stray {@code true} flag would make the update mask advertise a block
 * that was never written. The <b>two-slot</b> design, because {@code handleHitMask} fills the
 * first slot and then the second and silently drops anything after that — the double hit disappears
 * if the slots are reordered. And the ⚠️ collision: the four mask names are still declared on
 * {@link NPC} (the NPC's splats go out through the same protocol), so the rewriter had to choose a
 * side by receiver type.
 */
class HitUpdateTest {

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void aFreshPlayerIsNotShowingAHit() {
		final HitUpdate h = new HitUpdate();

		assertEquals(0, h.hitDiff, "nothing to show in the first splat");
		assertEquals(0, h.hitDiff2, "nothing to show in the second splat");
		assertFalse(h.hitUpdateRequired, "the first slot must not be advertised by default");
		assertFalse(h.hitUpdateRequired2, "the second slot must not be advertised by default");
		assertEquals(0, h.pendingHitpoints, "no damage reserved against a fresh player");
	}

	@Test
	void eachPlayerOwnsItsOwnHitUpdate() {
		final Client a = client();
		final Client b = client();

		assertSame(a.hitUpdate, a.hitUpdate, "the same player must keep one hit-update bag");
		assertNotSame(a.hitUpdate, b.hitUpdate, "two players must not share hit bookkeeping");

		a.hitUpdate.hitDiff = 13;
		a.hitUpdate.hitUpdateRequired = true;
		a.hitUpdate.pendingHitpoints = 4;

		assertEquals(0, b.hitUpdate.hitDiff, "hitDiff leaked between players");
		assertFalse(b.hitUpdate.hitUpdateRequired, "hitUpdateRequired leaked between players");
		assertEquals(0, b.hitUpdate.pendingHitpoints, "pendingHitpoints leaked between players");
	}

	/**
	 * The two splat slots fill in order and the third call is dropped — this is the behaviour of
	 * {@code handleHitMask}, and it is what makes a double hit show as two splats rather than one.
	 */
	@Test
	void theTwoHitsplatSlotsFillInOrder() {
		final Client c = client();

		c.handleHitMask(5);
		assertTrue(c.hitUpdate.hitUpdateRequired, "the first hit takes the first slot");
		assertEquals(5, c.hitUpdate.hitDiff);
		assertFalse(c.hitUpdate.hitUpdateRequired2, "the second slot stays empty after one hit");

		c.handleHitMask(9);
		assertTrue(c.hitUpdate.hitUpdateRequired2, "a second hit takes the second slot");
		assertEquals(9, c.hitUpdate.hitDiff2);
		assertEquals(5, c.hitUpdate.hitDiff, "the first splat must not be overwritten by the second");

		c.handleHitMask(99);
		assertEquals(5, c.hitUpdate.hitDiff, "only two splats fit; the third call is dropped");
		assertEquals(9, c.hitUpdate.hitDiff2, "only two splats fit; the third call is dropped");
	}

	/**
	 * The fields are internal: every other class reaches them through the accessors that stayed on
	 * {@code Player}, so those must keep delegating to the bag.
	 */
	@Test
	void theAccessorsStillDelegateToTheBag() {
		final Client c = client();

		c.setHitDiff(7);
		c.setHitDiff2(11);
		c.setHitUpdateRequired(true);
		c.setHitUpdateRequired2(true);

		assertEquals(7, c.hitUpdate.hitDiff);
		assertEquals(11, c.hitUpdate.hitDiff2);
		assertTrue(c.hitUpdate.hitUpdateRequired);
		assertTrue(c.hitUpdate.hitUpdateRequired2);

		assertEquals(7, c.getHitDiff(), "getHitDiff must read through the bag");
		assertTrue(c.isHitUpdateRequired(), "isHitUpdateRequired must read through the bag");
		assertTrue(c.getHitUpdateRequired(), "getHitUpdateRequired must read through the bag");
		assertTrue(c.getHitUpdateRequired2(), "getHitUpdateRequired2 must read through the bag");
	}

	/**
	 * The §4.17 hazard pinned reflectively: the four mask names are deliberately <em>still</em>
	 * declared on {@link NPC}, and are gone from {@link Player}; {@code pendingHitpoints} is
	 * player-only (the NPC reserves damage under the name {@code pendingDamage}).
	 */
	@Test
	void theMaskFieldsLiveOnNpcTooButNotOnPlayer() throws Exception {
		assertTrue(NPC.class.getDeclaredField("hitDiff").getType() == int.class,
				"NPC must keep its own hitDiff; the rewriter left its update block alone");
		assertTrue(NPC.class.getDeclaredField("hitDiff2").getType() == int.class);
		assertTrue(NPC.class.getDeclaredField("hitUpdateRequired").getType() == boolean.class);
		assertTrue(NPC.class.getDeclaredField("hitUpdateRequired2").getType() == boolean.class);
		assertThrows(NoSuchFieldException.class, () -> NPC.class.getDeclaredField("pendingHitpoints"),
				"the NPC reserves damage as pendingDamage, not pendingHitpoints");

		for (final String name : new String[] { "hitDiff", "hitDiff2", "hitUpdateRequired",
				"hitUpdateRequired2", "pendingHitpoints" }) {
			assertThrows(NoSuchFieldException.class, () -> Player.class.getDeclaredField(name),
					name + " must no longer be declared on Player");
			assertTrue(HitUpdate.class.getDeclaredField(name) != null,
					name + " must now live on HitUpdate");
		}
	}
}
