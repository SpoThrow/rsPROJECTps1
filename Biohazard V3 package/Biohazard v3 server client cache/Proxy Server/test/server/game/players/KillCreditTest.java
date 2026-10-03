package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import server.Config;
import server.game.npcs.NPC;

/**
 * Pins the kill-credit state after §4.19 moved it off {@link Player} into {@link KillCredit}.
 *
 * <p>Two things are load-bearing here beyond the {@code 0} defaults. First, the two composite fields
 * must be <b>initialised</b>: {@code attackedPlayers} is added to with no null check in
 * {@code CombatAssistant}, so losing the initialiser turns the mutual-attack check into an NPE.
 * Second, the table and the list must be <b>per player</b> — {@code damageTaken} is indexed by
 * attacker id, so a shared table would hand one player's damage to another and corrupt drop credit.
 *
 * <p>The reflective half pins the §4.16-style collision: {@code killerId} is still declared on
 * {@link NPC} (mirrored meaning), while it is gone from {@code Player}.
 */
class KillCreditTest {

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void aFreshPlayerHasNoEncounter() {
		final KillCredit k = new KillCredit();

		assertEquals(0, k.killerId, "nobody is credited with killing a fresh player");
		assertEquals(0, k.totalDamageDealt, "no damage has been dealt");
		assertNotNull(k.attackedPlayers, "the engagement list must be initialised or attackPlayer NPEs");
		assertTrue(k.attackedPlayers.isEmpty(), "nothing has been engaged");
		assertEquals(Config.MAX_PLAYERS, k.damageTaken.length,
				"the per-attacker table is indexed by player id, so it must be MAX_PLAYERS wide");
		assertEquals(0, Arrays.stream(k.damageTaken).sum(), "no damage is booked against anyone");
	}

	@Test
	void eachPlayerOwnsItsOwnTableAndList() {
		final Client a = client();
		final Client b = client();

		assertSame(a.killCredit, a.killCredit, "the same player must keep one kill-credit bag");
		assertNotSame(a.killCredit, b.killCredit, "two players must not share kill credit");

		// A shared table would be the worst kind of bug here: damage booked by one player would be
		// read back by another and change who gets the drop.
		assertNotSame(a.killCredit.damageTaken, b.killCredit.damageTaken, "the damage tables must not be shared");
		assertNotSame(a.killCredit.attackedPlayers, b.killCredit.attackedPlayers, "the engagement lists must not be shared");

		a.killCredit.damageTaken[3] = 17;
		a.killCredit.attackedPlayers.add(3);
		a.killCredit.totalDamageDealt = 17;
		a.killCredit.killerId = 3;

		assertEquals(0, b.killCredit.damageTaken[3], "a damage booking leaked into another player");
		assertTrue(b.killCredit.attackedPlayers.isEmpty(), "an engagement leaked into another player");
		assertEquals(0, b.killCredit.totalDamageDealt, "totalDamageDealt leaked between players");
		assertEquals(0, b.killCredit.killerId, "killerId leaked between players");
	}

	/**
	 * The two credit mechanisms are deliberately independent: {@code totalDamageDealt} is a single
	 * running total compared across attackers, {@code damageTaken} a per-attacker table. Writing one
	 * must not touch the other.
	 */
	@Test
	void theTwoCreditMechanismsAreSeparate() {
		final Client c = client();

		c.killCredit.totalDamageDealt += 12;
		assertEquals(12, c.killCredit.totalDamageDealt);
		assertEquals(0, c.killCredit.damageTaken[c.playerId],
				"the scalar total must not write the per-attacker table");

		c.killCredit.damageTaken[c.playerId] += 5;
		assertEquals(12, c.killCredit.totalDamageDealt,
				"the per-attacker table must not write the scalar total");
	}

	/**
	 * ⚠️ The collision, pinned reflectively: {@code killerId} is still declared on {@link NPC} — the
	 * mirrored meaning, and why the rewriter skipped 21 references — and is gone from {@link Player}.
	 */
	@Test
	void killerIdIsDeclaredOnBothSides() throws Exception {
		assertEquals(int.class, NPC.class.getDeclaredField("killerId").getType(),
				"NPC must keep its own killerId; the rewriter deliberately left its uses alone");
		assertEquals(int.class, KillCredit.class.getDeclaredField("killerId").getType(),
				"the player's killerId must now live on KillCredit");
		assertThrows(NoSuchFieldException.class, () -> Player.class.getDeclaredField("killerId"),
				"killerId must no longer be declared on Player");

		for (final String name : new String[] { "killerId", "totalDamageDealt", "damageTaken",
				"attackedPlayers" }) {
			assertThrows(NoSuchFieldException.class, () -> Player.class.getDeclaredField(name),
					name + " must no longer be declared on Player");
			assertNotNull(KillCredit.class.getDeclaredField(name), name + " must now live on KillCredit");
		}
	}
}
