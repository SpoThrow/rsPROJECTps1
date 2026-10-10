package server.game.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import server.Config;
import server.Server;
import server.world.ItemHandler;

/**
 * Pins the contract {@link ItemDefinitions} exists to provide: a lookup for an item that has
 * no definition answers with a sentinel instead of {@code null}, so an id we reference before
 * we have its definition is inert rather than fatal.
 *
 * <p>{@link Item#getItemName(int)} returns {@code null} in that case, and this codebase is
 * full of {@code getItemName(id).contains(...)}, so the difference is a crash versus a
 * non-match. These tests are the record of which one we chose.
 */
class ItemDefinitionsTest {

	/**
	 * An id with a row in {@code item.cfg}: the Abyssal whip.
	 */
	private static final int DEFINED = 4151;

	/**
	 * An id with genuinely no row in {@code item.cfg}.
	 *
	 * <p><b>This was {@code 4153}, and that was wrong.</b> 4153 is the Granite maul. It read as
	 * undefined only because {@code item.cfg} was never loaded during tests — the test task runs
	 * from a throwaway working directory with no {@code Data} in it, so the item table was empty
	 * and every "no definition" assertion in this class passed for the wrong reason, by asserting
	 * that <em>nothing</em> had a definition. The test task now passes the real {@code Data/cfg}
	 * through {@code serverCfgDir}, so these ids have to be right.
	 *
	 * <p>{@code item.cfg} holds 19,966 rows running from 0 to 20,072, so anything above that
	 * range has no row and no import short of a new file can give it one.
	 */
	private static final int UNDEFINED = 24000;

	/**
	 * A higher-revision id we could reference before we have it.
	 *
	 * <p><b>This replaces {@code 15333} as that exemplar, which was also wrong.</b> {@code item.cfg}
	 * already defines the entire dose chain: 15300–15303 Recover special, 15304–15307 Super
	 * antifire, 15308–15327 all four doses of all five extremes, 15328–15331 Super prayer, and
	 * overload at both 15332–15334 and 15335–15338. So the overload row is not an example of an id
	 * we reference ahead of time; it is already imported, and the id named here would have resolved
	 * to a real item. An id past the table's end is the honest example of the case
	 * {@code QOL_PLAN.md} §2 is about.
	 */
	private static final int FUTURE_ITEM = 22694;

	@Test
	void nameIsNeverNullForAnyId() {
		int[] ids = {
				-1, 0, 1, UNDEFINED, FUTURE_ITEM, 20072, 20073,
				Config.ITEM_LIMIT - 1, Config.ITEM_LIMIT, Integer.MAX_VALUE,
		};
		for (int id : ids) {
			assertNotNull(ItemDefinitions.name(id), "id " + id + " must not resolve to a null name");
		}
	}

	@Test
	void outOfRangeIdsAnswerTheSentinelRatherThanThrow() {
		// The legacy accessors scan the array, so an id past the end never reaches them; the
		// O(1) path has to check the bound itself or it is an ArrayIndexOutOfBoundsException.
		assertEquals(ItemDefinitions.UNKNOWN_NAME, ItemDefinitions.name(-1));
		assertEquals(ItemDefinitions.UNKNOWN_NAME, ItemDefinitions.name(Config.ITEM_LIMIT));
		assertEquals(ItemDefinitions.UNKNOWN_NAME, ItemDefinitions.name(Integer.MAX_VALUE));
		assertNull(ItemDefinitions.get(-1));
		assertNull(ItemDefinitions.get(Integer.MAX_VALUE));
		assertFalse(ItemDefinitions.exists(Integer.MAX_VALUE));
		assertEquals(0, ItemDefinitions.value(Integer.MAX_VALUE));
	}

	@Test
	void aDefinedItemResolvesToItsNameAndValue() {
		ItemList saved = Server.itemHandler.ItemList[DEFINED];
		try {
			ItemList def = new ItemList(DEFINED);
			def.itemName = "Abyssal whip";
			def.ShopValue = 120001;
			Server.itemHandler.ItemList[DEFINED] = def;

			assertTrue(ItemDefinitions.exists(DEFINED));
			assertEquals("Abyssal whip", ItemDefinitions.name(DEFINED));
			assertEquals(120001, ItemDefinitions.value(DEFINED));
		} finally {
			Server.itemHandler.ItemList[DEFINED] = saved;
		}
	}

	@Test
	void anItemWithNoRowIsUndefinedWhileItsNeighbourIsNot() {
		assertFalse(ItemDefinitions.exists(UNDEFINED));
		assertEquals(ItemDefinitions.UNKNOWN_NAME, ItemDefinitions.name(UNDEFINED));
		assertNull(ItemDefinitions.get(UNDEFINED));

		// And the real table is actually loaded, so the assertions above are about this id rather
		// than about an empty array. Without this the whole class is vacuous, which is exactly how
		// 4153 passed as "undefined" while being the Granite maul.
		assertTrue(ItemDefinitions.exists(DEFINED), "the item table must be loaded for these to mean anything");
		assertEquals("Abyssal whip", ItemDefinitions.name(DEFINED));
	}

	@Test
	void theIdsThisClassUsedToCallUndefinedAreDefined() {
		// A regression guard with a small history. 4153 was named as the undefined id and 15333 as
		// the "not imported yet" exemplar; both have rows, and both passed only because no item
		// table was loaded. If the table ever fails to load again these flip back to passing
		// vacuously rather than failing, so they are asserted positively here.
		assertTrue(ItemDefinitions.exists(4153), "4153 is the Granite maul, not an undefined id");
		assertTrue(ItemDefinitions.exists(15333), "15333 is an overload dose and is already imported");
		assertEquals("Overload (3)", ItemDefinitions.name(15333));
	}

	@Test
	void anEntryStoredAtAnotherIndexIsStillFound() {
		// The two legacy accessors always linear-scanned, so an entry not sitting at its own
		// index still resolved. The fast path must not quietly lose that.
		//
		// These indices have to be genuinely free now that the table is loaded: 9000 and 9001 were
		// used here and both hold a real item ("Bandana and eyepatch"), which made the test find
		// the real entry and fail rather than proving anything about the scan.
		int storedAt = 24998;
		int actualId = 24999;
		assertFalse(ItemDefinitions.exists(storedAt), "the fixture needs a free index");
		assertFalse(ItemDefinitions.exists(actualId), "and a free id to look up");

		ItemList saved = Server.itemHandler.ItemList[storedAt];
		try {
			ItemList def = new ItemList(actualId);
			def.itemName = "Misplaced";
			Server.itemHandler.ItemList[storedAt] = def;

			assertEquals("Misplaced", ItemDefinitions.name(actualId), "the scan must back up the O(1) read");
			assertFalse(ItemDefinitions.exists(storedAt), "the index is not the identity");
		} finally {
			Server.itemHandler.ItemList[storedAt] = saved;
		}
	}

	@Test
	void aMissingHandlerIsNotAnError() {
		// Server.itemHandler is null until Server is constructed. Lookups that can run before
		// then — a content table being registered, a test — must not throw.
		ItemHandler saved = Server.itemHandler;
		try {
			Server.itemHandler = null;
			assertEquals(ItemDefinitions.UNKNOWN_NAME, ItemDefinitions.name(DEFINED));
			assertFalse(ItemDefinitions.exists(DEFINED));
			assertNull(ItemDefinitions.get(DEFINED));
			assertEquals(0, ItemDefinitions.value(DEFINED));
		} finally {
			Server.itemHandler = saved;
		}
	}
}
