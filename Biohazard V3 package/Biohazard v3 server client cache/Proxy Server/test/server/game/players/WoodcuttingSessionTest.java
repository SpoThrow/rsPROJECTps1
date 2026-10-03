package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Pins the woodcutting cluster after Phase 4.5 moved its four fields off {@link Player}
 * into {@link WoodcuttingSession}.
 *
 * <p>The interesting risk in this pass is not the defaults -- it is that the move turned
 * four plain fields into a per-player object. {@code Woodcutting} already contains a
 * {@code static int a} holding one player's selected axe for everybody, so this codebase
 * has form for exactly that mistake; if {@code woodcutting} were ever made static, every
 * player on the server would share one chopping session and
 * {@code Woodcutting.cutDownTree}, which cancels the session of every player standing at
 * the tree that just fell, would cancel all of them at once. The per-instance tests below
 * are the guard for that.
 */
class WoodcuttingSessionTest {

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void everyFieldKeepsItsOldDefault() {
		final WoodcuttingSession wc = new WoodcuttingSession();

		assertFalse(wc.active, "isWc was initialised to false");
		assertEquals(0, wc.treeX, "treeX was initialised to 0");
		assertEquals(0, wc.treeY, "treeY was initialised to 0");
	}

	@Test
	void aFreshPlayerCanStartChopping() {
		// startWoodcutting opens with `if (active) return;`, so an active defaulting to true
		// would mean no player could ever cut a tree. The false default is the difference
		// between working and unplayable, not a style choice.
		final Client fresh = client();
		assertFalse(fresh.woodcutting.active, "a new player must not look mid-chop");
	}

	@Test
	void eachPlayerOwnsItsOwnSession() {
		final Client a = client();
		final Client b = client();

		assertSame(a.woodcutting, a.woodcutting, "the same player must keep one session object");
		assertNotSame(a.woodcutting, b.woodcutting, "two players must not share a session");

		a.woodcutting.active = true;
		a.woodcutting.treeX = 3200;
		a.woodcutting.treeY = 3201;

		assertFalse(b.woodcutting.active, "session leaked between players");
		assertEquals(0, b.woodcutting.treeX, "session leaked between players");
		assertEquals(0, b.woodcutting.treeY, "session leaked between players");
	}
}
