package server.game.players.actions.objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the three small second-click delegation families migrated together from
 * {@code ActionHandler}: {@link FurnaceObjects}, {@link BankBoothObjects} and
 * {@link CastleWarsDoorObjects}.
 */
class SecondClickDelegatesTest {

	@Test
	void tableIdsMatchTheSwitch() {
		assertArrayEquals(new int[] { 11666, 3044, 2781 }, FurnaceObjects.ids());
		assertArrayEquals(new int[] { 2213, 26972, 14367 }, BankBoothObjects.ids());
		assertArrayEquals(new int[] { 4423, 4424, 4427, 4428 }, CastleWarsDoorObjects.ids());
	}

	@Test
	void everyDelegateObjectIsRegisteredOnSecondClick() {
		for (int id : FurnaceObjects.ids()) {
			assertTrue(ObjectHandler.isRegistered(id, ObjectClick.SECOND), "furnace " + id);
		}
		for (int id : BankBoothObjects.ids()) {
			assertTrue(ObjectHandler.isRegistered(id, ObjectClick.SECOND), "bank " + id);
		}
		for (int id : CastleWarsDoorObjects.ids()) {
			assertTrue(ObjectHandler.isRegistered(id, ObjectClick.SECOND), "castlewars door " + id);
		}
	}

	@Test
	void noneOfThemClaimFirstOrThirdClick() {
		// These were second-click cases only; claiming another click would shadow the
		// switch for an object that does something different there.
		for (int id : FurnaceObjects.ids()) {
			assertTrue(!ObjectHandler.isRegistered(id, ObjectClick.FIRST)
					&& !ObjectHandler.isRegistered(id, ObjectClick.THIRD), "furnace " + id);
		}
		for (int id : BankBoothObjects.ids()) {
			assertTrue(!ObjectHandler.isRegistered(id, ObjectClick.FIRST)
					&& !ObjectHandler.isRegistered(id, ObjectClick.THIRD), "bank " + id);
		}
		for (int id : CastleWarsDoorObjects.ids()) {
			assertTrue(!ObjectHandler.isRegistered(id, ObjectClick.FIRST)
					&& !ObjectHandler.isRegistered(id, ObjectClick.THIRD), "door " + id);
		}
	}
}
