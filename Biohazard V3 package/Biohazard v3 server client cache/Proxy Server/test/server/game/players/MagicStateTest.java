package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

/**
 * Pins the magic state after §4.15 moved it off {@link Player} into {@link MagicState}.
 *
 * <p>⚠️ The important assertion here is a <em>wart</em>, not a guarantee: {@code autocastId}'s
 * declared default is {@code 0}, but the code treats {@code -1} as "no autocast"
 * ({@code autocastId < 0} / {@code autocastId >= 0} guards in {@code PlayerAssistant}). So a fresh
 * player reads as having autocast spell 0 selected. The test pins the actual declared default so
 * that a future change (either fixing it to {@code -1} or changing the guards) has to be
 * deliberate and must update this test.
 *
 * <p>The three memory arrays are one persisted row in {@code PlayerSave} (a {@code weapon, spell,
 * book} triple per slot), so their length and per-player ownership are pinned together.
 */
class MagicStateTest {

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void aFreshPlayersMagicStateIsZeroed() {
		final MagicState m = new MagicState();

		assertEquals(0, m.playerMagicBook, "a fresh player is on the normal spellbook");
		assertEquals(0, m.spellId, "no spell is selected");
		assertEquals(0, m.oldSpellId, "no previous spell");
	}

	@Test
	void theAutocastSelectionDefaultDisagreesWithItsSentinel() {
		// ⚠️ Declared default is 0, but -1 is what the code means by "no autocast". Pinned
		// deliberately so that the mismatch is visible and any fix is a conscious change.
		final MagicState m = new MagicState();
		assertEquals(0, m.autocastId,
				"autocastId starts at 0 even though the guards use autocastId < 0 to mean 'none'");
	}

	@Test
	void theAutocastMemoryIsThreeParallelTwelveSlotRows() {
		final MagicState m = new MagicState();

		// One slot per autocast-capable weapon; the three arrays are saved together per slot.
		assertEquals(12, m.autocastMemWeapon.length, "autocastMemWeapon is 12 slots");
		assertEquals(12, m.autocastMemSpell.length, "autocastMemSpell is 12 slots");
		assertEquals(12, m.autocastMemBook.length, "autocastMemBook is 12 slots");

		for (int i = 0; i < 12; i++) {
			assertEquals(0, m.autocastMemWeapon[i], "empty autocast slot (weapon) at " + i);
			assertEquals(0, m.autocastMemSpell[i], "empty autocast slot (spell) at " + i);
			assertEquals(0, m.autocastMemBook[i], "empty autocast slot (book) at " + i);
		}
	}

	@Test
	void eachPlayerOwnsItsOwnMagicStateAndMemory() {
		final Client a = client();
		final Client b = client();

		assertNotSame(a.magic, b.magic, "two players must not share magic state");
		assertNotSame(a.magic.autocastMemWeapon, b.magic.autocastMemWeapon,
				"two players must not share the weapon memory array");
		assertNotSame(a.magic.autocastMemSpell, b.magic.autocastMemSpell,
				"two players must not share the spell memory array");
		assertNotSame(a.magic.autocastMemBook, b.magic.autocastMemBook,
				"two players must not share the book memory array");

		a.magic.autocastId = 1171;
		a.magic.spellId = 1171;
		a.magic.playerMagicBook = 2;
		a.magic.autocastMemSpell[3] = 1171;

		assertEquals(0, b.magic.autocastId, "the autocast selection leaked between players");
		assertEquals(0, b.magic.spellId, "the selected spell leaked between players");
		assertEquals(0, b.magic.playerMagicBook, "the spellbook leaked between players");
		assertEquals(0, b.magic.autocastMemSpell[3], "the memory array leaked between players");
	}
}
