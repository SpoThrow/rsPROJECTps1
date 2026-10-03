package server.game.players.actions.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Pins the strangler contract in {@link ObjectHandler}.
 *
 * <p>As with the button registry, the return value is load-bearing: each of
 * {@code ActionHandler}'s three object methods does
 * {@code if (ObjectHandler.dispatch(...)) return;} before its switch, so "true" means
 * the switch must not also run.
 *
 * <p>The ids used here are negative, which no object in the game has, so these
 * registrations cannot collide with a real object.
 */
class ObjectHandlerTest {

	private static final int TEST_OBJECT = -1;
	private static final int THIRD_TEST_OBJECT = -2;

	@Test
	void dispatchReturnsFalseForAnObjectStillInTheSwitch() {
		// 11214 (the construction platform) has not been migrated.
		assertFalse(ObjectHandler.isRegistered(11214, ObjectClick.FIRST));
		assertFalse(ObjectHandler.dispatch(null, 11214, ObjectClick.FIRST, 0, 0));
	}

	@Test
	void dispatchRunsTheHandlerAndReportsItClaimedTheObject() {
		AtomicInteger seen = new AtomicInteger();
		AtomicInteger coords = new AtomicInteger();
		ObjectHandler.register(TEST_OBJECT, ObjectClick.FIRST, (c, objectType, objectX, objectY) -> {
			seen.incrementAndGet();
			coords.set(objectX * 1000 + objectY);
		});

		assertTrue(ObjectHandler.dispatch(null, TEST_OBJECT, ObjectClick.FIRST, 3210, 3421));
		assertEquals(1, seen.get());
		assertEquals(3210 * 1000 + 3421, coords.get(), "the handler must receive the clicked coords");
	}

	@Test
	void theSameObjectIdCanBeHandledOnDifferentClicks() {
		// The key is (objectType, click), not objectType alone, so one id may have
		// separate first and third handlers.
		AtomicInteger third = new AtomicInteger();
		ObjectHandler.register(THIRD_TEST_OBJECT, ObjectClick.THIRD,
				(c, objectType, objectX, objectY) -> third.incrementAndGet());

		assertTrue(ObjectHandler.isRegistered(THIRD_TEST_OBJECT, ObjectClick.THIRD));
		assertFalse(ObjectHandler.isRegistered(THIRD_TEST_OBJECT, ObjectClick.FIRST));
		assertFalse(ObjectHandler.dispatch(null, THIRD_TEST_OBJECT, ObjectClick.FIRST, 0, 0),
				"a third-click registration must not claim first clicks");
		assertEquals(0, third.get());
	}

	@Test
	void duplicateRegistrationThrowsAndKeepsTheFirstHandler() {
		AtomicInteger first = new AtomicInteger();
		AtomicInteger second = new AtomicInteger();
		ObjectHandler.register(THIRD_TEST_OBJECT, ObjectClick.SECOND,
				(c, objectType, objectX, objectY) -> first.incrementAndGet());

		assertThrows(IllegalStateException.class, () -> ObjectHandler.register(
				THIRD_TEST_OBJECT, ObjectClick.SECOND,
				(c, objectType, objectX, objectY) -> second.incrementAndGet()));

		ObjectHandler.dispatch(null, THIRD_TEST_OBJECT, ObjectClick.SECOND, 0, 0);
		assertEquals(1, first.get(), "the rejected registration must not replace the original");
		assertEquals(0, second.get());
	}

	@Test
	void migratingTheSameFamilyTwiceThrows() {
		assertThrows(IllegalStateException.class, WoodcuttingObjects::register);
		assertThrows(IllegalStateException.class, DwarfCannonObjects::register);
	}
}
