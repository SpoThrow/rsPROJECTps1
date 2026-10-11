package server.content.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import core.util.ISAACRandomGen;
import server.Config;
import server.event.CycleEventHandler;
import server.game.players.Client;
import server.game.players.Player;

/**
 * Pins the altar's offering loop: the first bone is offered on the click, and the rest follow on a
 * two-tick loop until the pack is empty, the player walks, or a random event takes over.
 *
 * <p>The two things worth defending are the shape and the stopping condition. The shape is that a
 * single use must still feel instant - the click's own bone is offered in the same tick, not a tick
 * later - and the stopping condition is that the loop is a loop rather than an inventory dump: it
 * ends when the pack runs out, and a walk ends it through {@code playerIsPraying} rather than
 * through a wall clock.
 *
 * <p>Random events are switched off for these clients by marking them bots, which is the same
 * predicate {@code RandomEventManager} reads. Without it the 1-in-40 refusal and the spawn roll
 * would make the counts depend on {@code Misc.random}.
 */
class PrayerAltarTest {

	/** Regular bones: id 526, and the cheapest offering in the table. */
	private static final int BONES = 526;
	private static final int PRAYER = Player.playerPrayer;
	/** Two ticks between offerings, which is what the altar has always run at. */
	private static final int ALTAR_CYCLES = 2;

	private Client lastClient;

	/** A client with a working out-stream and no random events, so the counts are deterministic. */
	private static Client client() {
		Client c = new Client(null, 2);
		c.getOutStream().packetEncryption = new ISAACRandomGen(new int[] { 1, 2, 3, 4 });
		c.expModifier = 1;
		c.isBot = true;
		return c;
	}

	private Client offerer(int bones) {
		Client c = client();
		lastClient = c;
		c.playerItems[0] = BONES + 1;
		c.playerItemsN[0] = bones;
		return c;
	}

	@AfterEach
	void stopEvents() {
		if (lastClient != null) {
			CycleEventHandler.stopEvents(lastClient);
		}
		lastClient = null;
	}

	// ---------------------------------------------------------------------------------------
	// The click
	// ---------------------------------------------------------------------------------------

	@Test
	void theClickOffersItsOwnBoneWithoutWaitingForATick() {
		// The manual path must not become slower than it was. One bone is gone before any tick is
		// processed; everything after that is the loop's business.
		Client c = offerer(1);
		double before = c.skills.playerXP[PRAYER];

		Prayer.bonesOnAltar(c, BONES);

		assertEquals(0, count(c, BONES), "the click's own bone is offered now");
		assertTrue(c.skills.playerXP[PRAYER] > before, "and it pays experience");
	}

	@Test
	void oneBoneWithNoMoreLeftLeavesNothingRunning() {
		// The loop still has to be registered and then stop on its first tick, rather than offer a
		// bone that is not there or keep the player flagged as offering forever.
		Client c = offerer(1);
		Prayer.bonesOnAltar(c, BONES);

		passCycles(ALTAR_CYCLES * 4);

		assertEquals(0, count(c, BONES));
		assertFalse(c.playerIsPraying, "with the pack empty the offering is over");
	}

	@Test
	void somethingThatIsNotABoneIsIgnored() {
		Client c = client();
		lastClient = c;
		c.playerItems[0] = 995 + 1; // coins are not in the table
		c.playerItemsN[0] = 100;

		Prayer.bonesOnAltar(c, 995);

		assertFalse(c.playerIsPraying, "nothing was offered, so nothing is running");
		assertEquals(100, count(c, 995), "and nothing is consumed");
	}

	// ---------------------------------------------------------------------------------------
	// The loop
	// ---------------------------------------------------------------------------------------

	@Test
	void theLoopOffersUntilThePackIsEmpty() {
		// A full pack used to be a click per bone. The loop is what makes it one click.
		Client c = offerer(5);

		Prayer.bonesOnAltar(c, BONES);
		passCycles(ALTAR_CYCLES * 40);

		assertEquals(0, count(c, BONES), "every bone in the pack was offered");
		assertFalse(c.playerIsPraying);
	}

	@Test
	void theLoopStopsAtTheLastBoneRatherThanTickingOnNothing() {
		// The stopping condition is the pack, not a count carried from the click: three bones means
		// three offerings, whichever way the click asked for them.
		Client c = offerer(3);
		double before = c.skills.playerXP[PRAYER];

		Prayer.bonesOnAltar(c, BONES);
		passCycles(ALTAR_CYCLES * 40);

		assertEquals(0, count(c, BONES));
		// Regular bones are 5 xp, paid at four times the rate for an altar offering. The refusal
		// roll costs nothing but a tick, so it cannot change the total; the global bonus and the
		// player's own modifier are applied by addSkillXP and are the same on every offering.
		double perBone = 5 * 4 * Config.PRAYER_EXPERIENCE * Config.SERVER_EXP_BONUS * c.expModifier;
		assertEquals(3 * perBone, c.skills.playerXP[PRAYER] - before, 1e-9);
	}

	@Test
	void aSecondClickWhileOfferingIsNotASecondOffering() {
		// A player holding the button down would otherwise offer two bones a tick. The loop owns
		// the pace from its first tick, so the second click has to be dropped.
		Client c = offerer(5);

		Prayer.bonesOnAltar(c, BONES); // the click's bone
		Prayer.bonesOnAltar(c, BONES); // ignored: the loop is already running
		passCycles(ALTAR_CYCLES);      // exactly one loop tick

		assertEquals(3, count(c, BONES), "the click and one tick, not the click twice");
		assertTrue(c.playerIsPraying);
	}

	@Test
	void walkingEndsTheOfferingBeforeTheNextBone() {
		// The cancellation path every walk step reaches. The loop must not deliver the bones it was
		// about to, which is the same contract the other ticked skills have.
		Client c = offerer(5);

		Prayer.bonesOnAltar(c, BONES);
		c.getPA().resetVariables();
		passCycles(ALTAR_CYCLES * 10);

		assertEquals(4, count(c, BONES), "only the click's own bone was offered");
		assertFalse(c.playerIsPraying, "a walk clears the flag the loop reads");
	}

	@Test
	void offeringAgainAfterAWalkDoesNotRunTwoLoops() {
		// The queued event has to be removed, not just have its flag cleared: otherwise it wakes on
		// its next tick, finds the flag set by the new offering and offers alongside it.
		Client c = offerer(6);

		Prayer.bonesOnAltar(c, BONES); // the first click's bone
		c.getPA().resetVariables();
		Prayer.bonesOnAltar(c, BONES); // the second click's bone
		passCycles(ALTAR_CYCLES);      // exactly one loop tick, not two loops' worth

		assertEquals(3, count(c, BONES), "two clicks and one loop tick, not two offerings a tick");
	}

	// ---------------------------------------------------------------------------------------

	private static void passCycles(int cycles) {
		for (int i = 0; i < cycles; i++) {
			CycleEventHandler.process();
		}
	}

	private static int count(Client c, int id) {
		int total = 0;
		for (int i = 0; i < c.playerItems.length; i++) {
			if (c.playerItems[i] == id + 1) {
				total += c.playerItemsN[i];
			}
		}
		return total;
	}
}
