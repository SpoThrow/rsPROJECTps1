package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the appearance cluster after Phase 4.9 moved it off {@link Player} into
 * {@link Appearance}.
 *
 * <p>Two defaults are checked harder than the others. {@code appearanceUpdateRequired}
 * starts {@code true}, so a new player's look is actually sent on the first update instead
 * of being skipped as clean; and the head icons start at {@code -1}, which is the "no icon"
 * value every reset path restores. {@code headIconPk = 0} is a real PK skull, so a
 * {@code 0} default here would paint one on every player's head.
 *
 * <p>As with the skill table, the appearance slots themselves are seeded by {@link Player}'s
 * constructor rather than by this class's initialiser, so the two are asserted separately:
 * a bare {@code new Appearance()} is all zeros, a new player is not.
 */
class AppearanceTest {

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void aBareAppearanceKeepsEveryDefault() {
		final Appearance appearance = new Appearance();

		assertEquals(13, appearance.playerAppearance.length, "the slot array was 13 long");
		for (int i = 0; i < 13; i++) {
			assertEquals(0, appearance.playerAppearance[i], "bare slot " + i + " should be 0");
		}
		assertTrue(appearance.appearanceUpdateRequired, "a new player's look must be sent");
		assertFalse(appearance.canChangeAppearance, "canChangeAppearance was initialised false");
		assertEquals(-1, appearance.headIcon, "headIcon was initialised to -1");
		assertEquals(-1, appearance.headIconPk, "headIconPk was initialised to -1");
		assertEquals(0, appearance.headIconHints, "headIconHints was uninitialised");
	}

	@Test
	void theNoIconSentinelIsMinusOneNotZero() {
		// headIconPk = 0 is a real skull (see ChangeRegions / CombatAssistant), and
		// headIcon = 0 is a real prayer icon, so -1 is the only value that means "none".
		final Appearance appearance = new Appearance();

		assertEquals(-1, appearance.headIcon);
		assertEquals(-1, appearance.headIconPk);
	}

	@Test
	void theConstructorSeedsTheAppearanceSlots() {
		final Appearance appearance = client().appearance;

		assertEquals(13, appearance.playerAppearance.length);
		assertEquals(0, appearance.playerAppearance[0], "gender slot");
		assertEquals(7, appearance.playerAppearance[1], "head slot");
		assertEquals(25, appearance.playerAppearance[2], "torso slot");
		assertEquals(5, appearance.playerAppearance[11], "feet colour slot");
		assertEquals(0, appearance.playerAppearance[12], "skin colour slot");
	}

	@Test
	void aFreshPlayerIsMarkedForAnAppearanceUpdate() {
		// If this were false, a login would render with no appearance until something else
		// forced an update.
		assertTrue(client().appearance.appearanceUpdateRequired);
	}

	@Test
	void eachPlayerOwnsItsOwnAppearanceAndItsOwnArray() {
		final Client a = client();
		final Client b = client();

		assertSame(a.appearance, a.appearance, "the same player must keep one appearance object");
		assertNotSame(a.appearance, b.appearance, "two players must not share appearance");
		// A half-shared object would pass the check above, so the array is checked too.
		assertNotSame(a.appearance.playerAppearance, b.appearance.playerAppearance,
				"two players must not share the appearance slot array");

		a.appearance.playerAppearance[1] = 99;
		a.appearance.headIcon = 2;
		a.appearance.headIconPk = 0;
		a.appearance.appearanceUpdateRequired = false;

		assertEquals(7, b.appearance.playerAppearance[1], "appearance slots leaked between players");
		assertEquals(-1, b.appearance.headIcon, "head icons leaked between players");
		assertEquals(-1, b.appearance.headIconPk, "head icons leaked between players");
		assertTrue(b.appearance.appearanceUpdateRequired, "the update flag leaked between players");
	}
}
