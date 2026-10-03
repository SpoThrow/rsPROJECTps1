package server.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Pins the Bounty Hunter cluster after Phase 4.6 moved its eleven fields off {@link Player}
 * into {@link BountyHunterState}.
 *
 * <p>Most of the defaults are plain zeros, but one is load-bearing: {@code killsMultiplier}
 * multiplies the reward a player receives, so it defaults to {@code 1} and a zero default
 * would silently pay out nothing. That is the same shape of trap as 4.4's {@code auto}.
 */
class BountyHunterStateTest {

	private static Client client() {
		return new Client(null, 1);
	}

	@Test
	void everyFieldKeepsItsOldDefault() {
		final BountyHunterState bh = new BountyHunterState();

		assertEquals(0, bh.targetIndex, "targetIndex was initialised to 0");
		assertFalse(bh.inBH, "inBH was initialised to false");
		assertFalse(bh.isRogue, "isRogue was initialised to false");
		assertFalse(bh.isBounty, "isBounty was initialised to false");
		assertFalse(bh.penaltyTimer, "penaltyTimer is a boolean and was initialised to false");
		assertEquals(0, bh.safeTimer, "safeTimer was initialised to 0");
		assertEquals(0, bh.bountyKills, "bountyKills was initialised to 0");
		assertEquals(0, bh.rogueKills, "rogueKills was initialised to 0");
		assertEquals(1, bh.killsMultiplier, "killsMultiplier was initialised to 1");
		assertEquals(0, bh.bountyIcon, "bountyIcon was initialised to 0");
	}

	@Test
	void theKillMultiplierDefaultsToOne() {
		// It is applied as a reward multiplier, so a 0 here would pay every Bounty Hunter
		// kill out at nothing while still looking like a perfectly ordinary int default.
		// This is the one non-zero default in the cluster, so it is pinned on its own.
		assertEquals(1, new BountyHunterState().killsMultiplier,
				"a 0 killsMultiplier would silently zero every Bounty Hunter reward");
	}

	@Test
	void aFreshPlayerHuntsNobodyAndIsNotInTheCrater() {
		final Client fresh = client();

		assertEquals(0, fresh.bountyHunter.targetIndex, "slot 0 is 'no target'; anything else points at a real player");
		assertNull(fresh.bountyHunter.targetName, "targetName has always defaulted to null");
		assertFalse(fresh.bountyHunter.inBH);
		assertFalse(fresh.bountyHunter.isRogue);
		assertFalse(fresh.bountyHunter.isBounty);
	}

	@Test
	void theTargetNameDefaultIsStillNull() {
		// Deliberately pinned, not endorsed: BountyHunter.resetTarget sets this back to
		// null and BountyHunter.handleBHDeath then calls targetName.equalsIgnoreCase(...)
		// with no guard, so null is the value that makes that path throw. Pinning it keeps
		// the hazard visible if anyone later "tidies" the default to "".
		assertNull(new BountyHunterState().targetName);
	}

	@Test
	void eachPlayerOwnsItsOwnState() {
		final Client a = client();
		final Client b = client();

		assertSame(a.bountyHunter, a.bountyHunter, "the same player must keep one state object");
		assertNotSame(a.bountyHunter, b.bountyHunter, "two players must not share Bounty Hunter state");

		a.bountyHunter.targetIndex = 42;
		a.bountyHunter.targetName = "Someone";
		a.bountyHunter.inBH = true;
		a.bountyHunter.isRogue = true;
		a.bountyHunter.isBounty = true;
		a.bountyHunter.penaltyTimer = true;
		a.bountyHunter.safeTimer = 180;
		a.bountyHunter.bountyKills = 9;
		a.bountyHunter.rogueKills = 7;
		a.bountyHunter.killsMultiplier = 4;

		assertEquals(0, b.bountyHunter.targetIndex, "state leaked between players");
		assertNull(b.bountyHunter.targetName, "state leaked between players");
		assertFalse(b.bountyHunter.inBH, "state leaked between players");
		assertFalse(b.bountyHunter.isRogue, "state leaked between players");
		assertFalse(b.bountyHunter.isBounty, "state leaked between players");
		assertFalse(b.bountyHunter.penaltyTimer, "state leaked between players");
		assertEquals(0, b.bountyHunter.safeTimer, "state leaked between players");
		assertEquals(0, b.bountyHunter.bountyKills, "state leaked between players");
		assertEquals(0, b.bountyHunter.rogueKills, "state leaked between players");
		assertEquals(1, b.bountyHunter.killsMultiplier, "state leaked between players");
	}
}
