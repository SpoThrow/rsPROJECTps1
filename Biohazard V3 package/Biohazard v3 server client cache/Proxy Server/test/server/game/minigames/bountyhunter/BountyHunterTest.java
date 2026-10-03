package server.game.minigames.bountyhunter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import server.game.players.Client;
import server.game.players.PlayerHandler;

/**
 * Covers the two Bounty Hunter defects found while moving the cluster in Phase 4.6 and
 * fixed immediately afterwards, because both are null/reference-comparison mistakes that
 * a mechanical move could not have introduced — they were simply invisible until the
 * fields were pulled together into one class.
 */
class BountyHunterTest {

	@AfterEach
	void tearDown() {
		PlayerHandler.players[1] = null;
		PlayerHandler.players[2] = null;
	}

	@Test
	void playerHasTargetRequiresBothASlotAndAName() {
		final Client c = new Client(null, 1);

		assertFalse(BountyHunter.playerHasTarget(c), "a fresh player is hunting nobody");

		c.bountyHunter.targetIndex = 5;
		assertFalse(BountyHunter.playerHasTarget(c),
				"a slot with no assigned name is not a usable target");

		c.bountyHunter.targetName = "";
		assertFalse(BountyHunter.playerHasTarget(c),
				"an empty name is not a usable target");

		c.bountyHunter.targetName = "Someone";
		assertTrue(BountyHunter.playerHasTarget(c),
				"a slot plus a non-empty name is a usable target");

		c.bountyHunter.targetIndex = 0;
		assertFalse(BountyHunter.playerHasTarget(c),
				"a name with no slot is not a usable target");
	}

	@Test
	void killCreditIsDecidedByANameMatch() {
		// handleBHDeath used to call target.bountyHunter.targetName.equalsIgnoreCase(...),
		// and targetName is null whenever resetTarget has just cleared it, so that threw
		// out of the death handler. The check now puts the dying player's name (never null
		// for a logged-in player) first, and lives in its own method so the award branch
		// can be tested in both directions -- the award itself continues into packet I/O.
		final Client target = new Client(null, 2);
		final Client dying = new Client(null, 1);
		dying.playerName = "Victim";

		target.bountyHunter.targetName = null;
		assertFalse(BountyHunter.isKillCreditFor(target, dying), "a null target name must not throw and must not credit");

		target.bountyHunter.targetName = "";
		assertFalse(BountyHunter.isKillCreditFor(target, dying), "an empty target name is not a match");

		target.bountyHunter.targetName = "SomeoneElse";
		assertFalse(BountyHunter.isKillCreditFor(target, dying), "a different name must not earn credit");

		target.bountyHunter.targetName = "Victim";
		assertTrue(BountyHunter.isKillCreditFor(target, dying), "a matching name must still earn the kill");

		dying.playerName = "victim";
		assertTrue(BountyHunter.isKillCreditFor(target, dying), "the match has always been case-insensitive");
	}

	@Test
	void aTargetWithNoAssignedNameDoesNotThrowOnKillCredit() {
		// handleBHDeath used to call target.bountyHunter.targetName.equalsIgnoreCase(...),
		// and targetName is null whenever resetTarget has just cleared it. That threw out
		// of the death handler. The credit check now puts the player name first, so a null
		// target name simply means "not the target" instead of an NPE.
		final Client victim = new Client(null, 1);
		victim.playerName = "Victim";
		victim.bountyHunter.targetIndex = 2;
		victim.killerId = 2;

		final Client target = new Client(null, 2);
		target.playerName = "Killer";
		target.bountyHunter.targetName = null;
		target.bountyHunter.safeTimer = 99;
		target.bountyHunter.bountyKills = 3;

		PlayerHandler.players[1] = victim;
		PlayerHandler.players[2] = target;

		BountyHunter.handleBHDeath(victim);

		assertEquals(0, target.bountyHunter.safeTimer,
				"the safe timer is still cleared on death, as before");
		assertEquals(3, target.bountyHunter.bountyKills,
				"no kill credit may be awarded when the target name does not match");
	}
}
