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
	 * An id with no row in {@code item.cfg}, and a second one to prove a neighbour does not
	 * resolve just because something near it does.
	 */
	private static final int DEFINED = 4151;
	private static final int UNDEFINED = 4153;

	/** A higher-revision overload dose — the exemplar of an id we reference before we have it. */
	private static final int FUTURE_OVERLOAD_DOSE = 15333;

	@Test
	void nameIsNeverNullForAnyId() {
		int[] ids = {
				-1, 0, 1, UNDEFINED, FUTURE_OVERLOAD_DOSE, 22694, 24000,
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
	}

	@Test
	void anEntryStoredAtAnotherIndexIsStillFound() {
		// The two legacy accessors always linear-scanned, so an entry not sitting at its own
		// index still resolved. The fast path must not quietly lose that.
		int storedAt = 9000;
		int actualId = 9001;
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
