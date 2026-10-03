package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the POS cluster after Phase 4 moved its 17 fields off {@link Player} into
 * {@link PosSession}.
 *
 * <p>This is a mechanical move, so the tests are about the two things a mechanical move
 * can silently change: the <em>defaults</em> (the old fields carried initialisers, and a
 * dropped one changes the first shop screen a player sees) and the <em>ownership</em> of
 * the object (it must be per-player -- a `static` slip here would have every player
 * sharing one shop session, which no other test in the suite would catch).
 */
class PosSessionTest {

	private static final int[] ZERO_INTS = new int[PosSession.LISTING_SLOTS];
	private static final long[] ZERO_LONGS = new long[PosSession.LISTING_SLOTS];

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void everyFieldKeepsItsOldDefault() {
		final PosSession pos = new PosSession();

		assertFalse(pos.selling);
		assertEquals(0, pos.sellItemId);
		assertEquals(0, pos.sellAmount);
		assertEquals(0, pos.sellPrice);
		assertEquals(0, pos.sellStep);

		assertFalse(pos.buying);
		assertEquals(0L, pos.buyListingId);
		assertEquals(0, pos.buyMax);

		assertEquals(0, pos.sortMode);
		assertEquals(0, pos.browseType);
		// the two initialised String fields -- `browseQuery = ""` was NOT null before,
		// and the shop screen renders it directly
		assertEquals("", pos.browseQuery);
		assertEquals("Recent Listings", pos.browseTitle);

		assertEquals(0L, pos.confirmRemoveId);
		assertEquals(0L, pos.editListingId);
	}

	@Test
	void theThreeListingArraysAreSizedAndZeroedAsBefore() {
		final PosSession pos = new PosSession();

		// sizes: the old fields were declared `new String[20]`, `new int[20]`, `new long[20]`
		assertEquals(20, PosSession.LISTING_SLOTS);
		assertEquals(PosSession.LISTING_SLOTS, pos.buySellers.length);
		assertEquals(PosSession.LISTING_SLOTS, pos.buyIndexes.length);
		assertEquals(PosSession.LISTING_SLOTS, pos.buyListingIds.length);

		// contents: an int[] defaulted to 0s, not -1s -- buyIndexes is explicitly filled
		// with -1 by the display code, so a changed default would leak through as slot 0
		for (int i = 0; i < PosSession.LISTING_SLOTS; i++) {
			assertNull(pos.buySellers[i], "buySellers[" + i + "]");
			assertEquals(ZERO_INTS[i], pos.buyIndexes[i], "buyIndexes[" + i + "]");
			assertEquals(ZERO_LONGS[i], pos.buyListingIds[i], "buyListingIds[" + i + "]");
		}
	}

	@Test
	void eachPlayerOwnsItsOwnSession() {
		final Client a = client();
		final Client b = client();

		assertSame(a.pos, a.pos, "the same player must keep one session object");
		assertNotSame(a.pos, b.pos, "two players must not share a POS session");

		a.pos.selling = true;
		a.pos.sellStep = 3;
		a.pos.buySellers[0] = "someone";
		assertFalse(b.pos.selling, "buy/sell state leaked between players");
		assertEquals(0, b.pos.sellStep, "buy/sell state leaked between players");
		assertNull(b.pos.buySellers[0], "listing state leaked between players");
	}

	@Test
	void theSessionIsNotSharedThroughArraysEither() {
		// a `static PosSession` would still pass aSameObject for one player; this catches
		// the case where only the arrays were shared, which is the easy mistake to make
		final Client a = client();
		final Client b = client();
		assertNotSame(a.pos.buySellers, b.pos.buySellers);
		assertNotSame(a.pos.buyIndexes, b.pos.buyIndexes);
		assertNotSame(a.pos.buyListingIds, b.pos.buyListingIds);
	}

	@Test
	void aFreshClientStartsWithAnIdleShopSession() {
		final Client c = client();
		assertFalse(c.pos.selling, "a newly connected player must not be mid-sell");
		assertFalse(c.pos.buying, "a newly connected player must not be mid-buy");
		assertEquals(0, c.pos.sellStep);
		assertEquals("Recent Listings", c.pos.browseTitle);
	}
}
