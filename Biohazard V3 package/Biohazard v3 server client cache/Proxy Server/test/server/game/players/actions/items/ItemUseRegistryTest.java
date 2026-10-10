package server.game.players.actions.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Pins the strangler contract in {@link ItemUseRegistry}.
 *
 * <p>As with the object and button registries, the return value is load-bearing: the hook in
 * {@code UseItem.ItemonItem} does {@code if (ItemUseRegistry.dispatch(...)) return;} before
 * the inline checks, so {@code true} means those checks must not also run.
 *
 * <p><b>Every test uses its own pair.</b> The registry is global static state and JUnit does
 * not promise a method order, so two tests sharing a pair would pass or fail depending on
 * which ran first. Ids are negative, which no item has, so none can collide with a real
 * recipe either.
 */
class ItemUseRegistryTest {

	/** Read-only: never registered by any test, so it stays unclaimed. */
	private static final int FREE_A = -101;
	private static final int FREE_B = -102;

	private static final int CLAIM_A = -111;
	private static final int CLAIM_B = -112;

	private static final int ORDER_A = -113;
	private static final int ORDER_B = -114;

	private static final int DUP_A = -115;
	private static final int DUP_B = -116;

	@Test
	void dispatchReturnsFalseForAPairStillInTheLegacyChecks() {
		assertFalse(ItemUseRegistry.isRegistered(FREE_A, FREE_B));
		assertFalse(ItemUseRegistry.dispatch(null, FREE_A, FREE_B));
	}

	@Test
	void dispatchRunsTheHandlerAndReportsItClaimedThePair() {
		AtomicInteger calls = new AtomicInteger();
		AtomicInteger seen = new AtomicInteger();
		ItemUseRegistry.register(CLAIM_A, CLAIM_B, (c, itemUsed, useWith) -> {
			calls.incrementAndGet();
			seen.set(itemUsed * 1000 + useWith);
		});

		assertTrue(ItemUseRegistry.dispatch(null, CLAIM_A, CLAIM_B));
		assertEquals(1, calls.get());
		assertEquals(CLAIM_A * 1000 + CLAIM_B, seen.get(), "the handler must see the pair as dispatched");
	}

	@Test
	void thePairIsOrderIndependentBecauseTheLegacyChecksAre() {
		// Every check in ItemonItem is written "a && b || b && a", so the client sending the
		// pair the other way round is the same action. A registration that missed the reversed
		// order would silently handle half the clicks.
		AtomicInteger calls = new AtomicInteger();
		ItemUseRegistry.register(ORDER_A, ORDER_B, (c, itemUsed, useWith) -> calls.incrementAndGet());

		assertTrue(ItemUseRegistry.isRegistered(ORDER_A, ORDER_B));
		assertTrue(ItemUseRegistry.isRegistered(ORDER_B, ORDER_A), "the reversed lookup must agree");

		assertTrue(ItemUseRegistry.dispatch(null, ORDER_B, ORDER_A), "the reversed click must be claimed");
		assertEquals(1, calls.get());
	}

	@Test
	void duplicateRegistrationThrowsInEitherOrder() {
		AtomicInteger first = new AtomicInteger();
		AtomicInteger second = new AtomicInteger();
		ItemUseRegistry.register(DUP_A, DUP_B, (c, itemUsed, useWith) -> first.incrementAndGet());

		assertThrows(IllegalStateException.class,
				() -> ItemUseRegistry.register(DUP_A, DUP_B, (c, itemUsed, useWith) -> second.incrementAndGet()));
		assertThrows(IllegalStateException.class, () -> ItemUseRegistry.register(DUP_B, DUP_A,
				(c, itemUsed, useWith) -> second.incrementAndGet()), "the reversed order is the same pair");

		ItemUseRegistry.dispatch(null, DUP_A, DUP_B);
		assertEquals(1, first.get(), "the rejected registration must not replace the original");
		assertEquals(0, second.get());
	}
}
