package server.game.players.actions.items;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Pins the strangler contract in {@link ItemOnObjectRegistry}, including the one rule that is
 * specific to it: the cooking objects are not claimable.
 *
 * <p><b>Every test uses its own pair.</b> The registry is global static state and JUnit does
 * not promise a method order, so two tests sharing a pair would pass or fail depending on
 * which ran first. Ids are negative, which no object has, so none can collide with a real one.
 */
class ItemOnObjectRegistryTest {

	/** Read-only: never registered by any test, so it stays unclaimed. */
	private static final int FREE_ITEM = -201;
	private static final int FREE_OBJECT = -202;

	private static final int CLAIM_ITEM = -203;
	private static final int CLAIM_OBJECT = -204;

	private static final int ORDER_ITEM = -205;
	private static final int ORDER_OBJECT = -206;

	private static final int DUP_ITEM = -207;
	private static final int DUP_OBJECT = -208;

	/** Only ever used for registrations that are expected to be refused. */
	private static final int REFUSED_ITEM = -209;

	@Test
	void theCookingObjectsAreExactlyThePacketHandlerSwitch() {
		// These are the ids ItemOnObject.processPacket handles itself, after calling
		// UseItem.ItemonObject. The list is duplicated in the registry to make them
		// unregisterable, so this test is what stops the two copies drifting apart.
		assertArrayEquals(new int[] {
				12269, 2732, 114, 9374, 2728, 25465, 11404, 11405, 11406,
		}, ItemOnObjectRegistry.COOKING_OBJECTS);
	}

	@Test
	void aCookingObjectCannotBeRegistered() {
		// Claiming one would leave the cooking switch in ItemOnObject.processPacket running as
		// well, because it fires after the call into UseItem regardless of what UseItem does.
		for (int id : ItemOnObjectRegistry.COOKING_OBJECTS) {
			assertThrows(IllegalArgumentException.class,
					() -> ItemOnObjectRegistry.register(REFUSED_ITEM, id, (c, itemId, objectId, x, y) -> {
					}), "object " + id);
		}
	}

	@Test
	void dispatchReturnsFalseForAPairStillInTheLegacySwitch() {
		assertFalse(ItemOnObjectRegistry.isRegistered(FREE_ITEM, FREE_OBJECT));
		assertFalse(ItemOnObjectRegistry.dispatch(null, FREE_ITEM, FREE_OBJECT, 0, 0));
	}

	@Test
	void dispatchRunsTheHandlerAndReportsItClaimedThePair() {
		AtomicInteger calls = new AtomicInteger();
		AtomicInteger coords = new AtomicInteger();
		ItemOnObjectRegistry.register(CLAIM_ITEM, CLAIM_OBJECT, (c, itemId, objectId, objectX, objectY) -> {
			calls.incrementAndGet();
			coords.set(objectX * 1000 + objectY);
		});

		assertTrue(ItemOnObjectRegistry.dispatch(null, CLAIM_ITEM, CLAIM_OBJECT, 3210, 3421));
		assertEquals(1, calls.get());
		assertEquals(3210 * 1000 + 3421, coords.get(), "the handler must receive the clicked coords");
	}

	@Test
	void thePairIsOrdered() {
		// "Use this item on that object" is not "use that object on this item": the legacy
		// method switches on the object and checks the item inside, so the pair has a direction.
		AtomicInteger calls = new AtomicInteger();
		ItemOnObjectRegistry.register(ORDER_ITEM, ORDER_OBJECT,
				(c, itemId, objectId, x, y) -> calls.incrementAndGet());

		assertTrue(ItemOnObjectRegistry.isRegistered(ORDER_ITEM, ORDER_OBJECT));
		assertFalse(ItemOnObjectRegistry.isRegistered(ORDER_OBJECT, ORDER_ITEM),
				"swapping the roles makes a different pair");
		assertFalse(ItemOnObjectRegistry.dispatch(null, ORDER_OBJECT, ORDER_ITEM, 0, 0));
		assertEquals(0, calls.get());
	}

	@Test
	void duplicateRegistrationThrowsAndKeepsTheFirstHandler() {
		AtomicInteger first = new AtomicInteger();
		AtomicInteger second = new AtomicInteger();
		ItemOnObjectRegistry.register(DUP_ITEM, DUP_OBJECT, (c, itemId, objectId, x, y) -> first.incrementAndGet());

		assertThrows(IllegalStateException.class, () -> ItemOnObjectRegistry.register(DUP_ITEM, DUP_OBJECT,
				(c, itemId, objectId, x, y) -> second.incrementAndGet()));

		ItemOnObjectRegistry.dispatch(null, DUP_ITEM, DUP_OBJECT, 0, 0);
		assertEquals(1, first.get(), "the rejected registration must not replace the original");
		assertEquals(0, second.get());
	}
}
