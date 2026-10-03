package server.game.players.packets.buttons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Pins the strangler contract in {@link ButtonHandler}.
 *
 * <p>The load-bearing rule is the return value: {@code ClickingButtons} does
 * {@code if (ButtonHandler.dispatch(...)) return;} immediately before its switch, so
 * "true" means the switch must not also run. If dispatch ever returned true without
 * running a handler, or false after running one, a button would either be silently
 * dead or fire twice.
 *
 * <p>The ids used here are negative, which no client can produce -- actionButtonId is
 * decoded from at most a couple of bytes -- so these registrations cannot collide
 * with a real button.
 */
class ButtonHandlerTest {

	private static final int TEST_BUTTON = -1;
	private static final int OTHER_TEST_BUTTON = -2;

	@Test
	void dispatchReturnsFalseForAButtonStillInTheSwitch() {
		// 50006 is the custom-shop close button; it has not been migrated yet, so the
		// switch must still be reached for it.
		assertFalse(ButtonHandler.isRegistered(50006));
		assertFalse(ButtonHandler.dispatch(null, 50006));
	}

	@Test
	void dispatchRunsTheHandlerAndReportsItClaimedTheButton() {
		AtomicInteger seen = new AtomicInteger();
		AtomicInteger idPassed = new AtomicInteger();
		ButtonHandler.register(TEST_BUTTON, (c, actionButtonId) -> {
			seen.incrementAndGet();
			idPassed.set(actionButtonId);
		});

		assertTrue(ButtonHandler.dispatch(null, TEST_BUTTON));
		assertEquals(1, seen.get());
		assertEquals(TEST_BUTTON, idPassed.get(), "the handler must receive the dispatched id");
	}

	@Test
	void duplicateRegistrationThrowsAndKeepsTheFirstHandler() {
		AtomicInteger first = new AtomicInteger();
		AtomicInteger second = new AtomicInteger();
		ButtonHandler.register(OTHER_TEST_BUTTON, (c, id) -> first.incrementAndGet());

		assertThrows(IllegalStateException.class,
				() -> ButtonHandler.register(OTHER_TEST_BUTTON, (c, id) -> second.incrementAndGet()),
				"a repeated button id must fail loudly: in the switch it was a compile"
						+ " error, in a map it would silently shadow");

		ButtonHandler.dispatch(null, OTHER_TEST_BUTTON);
		assertEquals(1, first.get(), "the rejected registration must not replace the original");
		assertEquals(0, second.get());
	}

	@Test
	void registeringTheWholeSmeltingFamilyAgainThrows() {
		// SmeltingButtons already ran via ButtonHandler's static initialiser, so a
		// second pass over the same ids must blow up rather than quietly rebind them.
		assertThrows(IllegalStateException.class, SmeltingButtons::register);
	}
}
