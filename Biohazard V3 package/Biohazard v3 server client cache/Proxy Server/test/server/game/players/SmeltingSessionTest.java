package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import server.content.skills.Smelting;
import server.content.skills.Smelting.Bars;

/**
 * Pins the smelting cluster after Phase 4.2 moved its fields off {@link Player} into
 * {@link SmeltingSession}. (The write-only {@code active} flag has since been deleted.)
 *
 * <p>As with {@code PosSessionTest}, a mechanical move can only break the defaults or the
 * ownership. The third thing worth pinning is the invariant {@link Smelting#startSmelting}
 * depends on: it dereferences {@code c.smelt.bar} with no null check, immediately after
 * {@code Bars.forType(...)}, which <em>can</em> return null. These tests record why that is
 * currently unreachable rather than leaving it as folklore -- the registered buttons only
 * ever pass bar index 0..7.
 */
class SmeltingSessionTest {

	private static final int[] SMELT_AMOUNTS = { 1, 5, 10, 28 };

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void everyFieldKeepsItsOldDefault() {
		final SmeltingSession smelt = new SmeltingSession();

		assertEquals(0, smelt.amount);
		assertNull(smelt.bar);
		assertEquals("", smelt.barType);
		assertEquals(0L, smelt.lastSmelt);
		// the old declaration was `smeltEventId = 5567`
		assertEquals(5567, smelt.eventId);
	}

	@Test
	void eachPlayerOwnsItsOwnSession() {
		final Client a = client();
		final Client b = client();

		assertSame(a.smelt, a.smelt, "the same player must keep one session object");
		assertNotSame(a.smelt, b.smelt, "two players must not share a smelt session");

		a.smelt.amount = 28;
		a.smelt.bar = Bars.BRONZE;
		a.smelt.barType = "bronze";
		assertEquals(0, b.smelt.amount, "smelt state leaked between players");
		assertNull(b.smelt.bar, "smelt state leaked between players");
		assertEquals("", b.smelt.barType, "smelt state leaked between players");
	}

	@Test
	void aFreshClientIsNotMidSmelt() {
		final Client c = client();
		assertEquals(0, c.smelt.amount);
		assertEquals(0L, c.smelt.lastSmelt);
		assertNull(c.smelt.bar);
	}

	@Test
	void everyBarTheButtonsCanAskForResolvesToANonNullBar() {
		// SmeltingButtons is a strict 8x4 grid and passes bar index 0..7, so the
		// `c.smelt.bar.twoOres()` in startSmelting cannot NPE from the UI. Pin it: if the
		// range ever widens without widening getType, that deref becomes live.
		for (int bar = 0; bar < Smelting.buttons.length; bar++) {
			final String type = Smelting.getType(bar);
			assertTrue(!type.isEmpty(), "bar index " + bar + " has no type");
			assertTrue(Bars.forType(type) != null, "bar index " + bar + " -> \"" + type + "\" has no Bars");
		}
		// ...and the boundary, so the coupling is explicit rather than assumed
		assertEquals("", Smelting.getType(Smelting.buttons.length));
		assertNull(Bars.forType(""));
	}

	@Test
	void theFourAmountsAreUnchanged() {
		for (int i = 0; i < SMELT_AMOUNTS.length; i++) {
			assertEquals(SMELT_AMOUNTS[i], Smelting.getAmount(i), "amount index " + i);
		}
		// out of range is -1, not 0 -- `smelt.amount > 0` is what drives the cycle, so a 0
		// here would silently do nothing instead of stopping
		assertEquals(-1, Smelting.getAmount(SMELT_AMOUNTS.length));
	}
}
