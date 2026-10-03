package server.game.players.actions.npcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Pins the strangler contract in {@link NpcActionHandler}.
 *
 * <p>As with the button and object registries the return value decides whether the
 * legacy switch also runs, so "true" must mean a handler actually ran.
 *
 * <p>The ids used here are negative, which no NPC has, so they cannot collide with a
 * real registration.
 */
class NpcActionHandlerTest {

	private static final int TEST_NPC = -1;
	private static final int OTHER_TEST_NPC = -2;

	@Test
	void dispatchReturnsFalseForAnNpcStillInTheSwitch() {
		// 4947 is not in the shop table.
		assertFalse(NpcActionHandler.isRegistered(4947, NpcClick.FIRST));
		assertFalse(NpcActionHandler.dispatch(null, 4947, NpcClick.FIRST));
	}

	@Test
	void dispatchRunsTheHandlerAndReportsItClaimedTheNpc() {
		AtomicInteger seen = new AtomicInteger();
		AtomicInteger typePassed = new AtomicInteger();
		NpcActionHandler.register(TEST_NPC, NpcClick.FIRST, (c, npcType) -> {
			seen.incrementAndGet();
			typePassed.set(npcType);
		});

		assertTrue(NpcActionHandler.dispatch(null, TEST_NPC, NpcClick.FIRST));
		assertEquals(1, seen.get());
		assertEquals(TEST_NPC, typePassed.get(), "the handler must receive the clicked npc type");
	}

	@Test
	void theSameNpcIdCanBeHandledOnDifferentClicks() {
		AtomicInteger third = new AtomicInteger();
		NpcActionHandler.register(OTHER_TEST_NPC, NpcClick.THIRD,
				(c, npcType) -> third.incrementAndGet());

		assertTrue(NpcActionHandler.isRegistered(OTHER_TEST_NPC, NpcClick.THIRD));
		assertFalse(NpcActionHandler.isRegistered(OTHER_TEST_NPC, NpcClick.FIRST));
		assertFalse(NpcActionHandler.dispatch(null, OTHER_TEST_NPC, NpcClick.FIRST),
				"a third-click registration must not claim first clicks");
		assertEquals(0, third.get());
	}

	@Test
	void duplicateRegistrationThrowsAndKeepsTheFirstHandler() {
		AtomicInteger first = new AtomicInteger();
		AtomicInteger second = new AtomicInteger();
		NpcActionHandler.register(OTHER_TEST_NPC, NpcClick.SECOND,
				(c, npcType) -> first.incrementAndGet());

		assertThrows(IllegalStateException.class, () -> NpcActionHandler.register(
				OTHER_TEST_NPC, NpcClick.SECOND, (c, npcType) -> second.incrementAndGet()));

		NpcActionHandler.dispatch(null, OTHER_TEST_NPC, NpcClick.SECOND);
		assertEquals(1, first.get(), "the rejected registration must not replace the original");
		assertEquals(0, second.get());
	}

	@Test
	void registeringTheShopFamilyTwiceThrows() {
		assertThrows(IllegalStateException.class, ShopNpcs::register);
	}
}
