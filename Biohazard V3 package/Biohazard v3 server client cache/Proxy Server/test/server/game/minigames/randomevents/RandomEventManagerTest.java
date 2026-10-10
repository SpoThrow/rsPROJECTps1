package server.game.minigames.randomevents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import core.util.ISAACRandomGen;
import server.Config;
import server.Server;
import server.event.CycleEventHandler;
import server.game.minigames.randomevents.RandomEventManager.Event;
import server.game.players.Client;
import server.game.players.PlayerHandler;

/**
 * Pins the two things {@code RandomEventManager} decides: which events are allowed to fire at
 * all, and how often. Both used to be unassertable — the flags did not gate anything, and the
 * rate was a {@code Misc.random(250)} written out at seven call sites.
 *
 * <p>{@code Config} fields are {@code static final}, so no test can flip one. That is why
 * {@link RandomEventManager#pick(int, java.util.Set)} takes its candidates as a parameter: the
 * gating is tested by passing a set rather than by changing the configuration, and the shipped
 * configuration is tested separately by asserting what {@code enabledEvents()} contains.
 */
class RandomEventManagerTest {

	private static final int SLOT = 1;

	private static final int NEST_RED_EGG = 5070;
	private static final int NEST_GREEN_EGG = 5071;
	private static final int NEST_BLUE_EGG = 5072;
	private static final int NEST_SEED = 5073;
	private static final int NEST_RING = 5074;
	private static final int[] NEST_IDS = { NEST_RED_EGG, NEST_GREEN_EGG, NEST_BLUE_EGG, NEST_SEED, NEST_RING };

	private Client lastClient;

	private Client client() {
		Client c = new Client(null, SLOT);
		c.getOutStream().packetEncryption = new ISAACRandomGen(new int[] { 1, 2, 3, 4 });
		lastClient = c;
		return c;
	}

	@AfterEach
	void tearDown() {
		if (lastClient != null) {
			CycleEventHandler.stopEvents(lastClient);
		}
		lastClient = null;
		PlayerHandler.players[SLOT] = null;
	}

	// ---------------------------------------------------------------- gating

	@Test
	void onlyTheGenieIsEnabledAsShipped() {
		// The ask was bird nests and the genie live, the intrusive classics not. The nest is not
		// in this enum at all -- it runs on its own per-action roll -- so the genie is the whole
		// of what should be enabled here.
		assertEquals(EnumSet.of(Event.GENIE), RandomEventManager.enabledEvents(),
				"only the genie should be live with the shipped Config flags");

		assertTrue(Event.GENIE.isFlagged());
		assertFalse(Event.SPIRIT_TREE.isFlagged());
		assertFalse(Event.ROCK_GOLEM.isFlagged());
		assertFalse(Event.RIVER_TROLL.isFlagged());
		assertFalse(Event.ZOMBIE.isFlagged());
	}

	@Test
	void everyClassicEventIsGatedOnTheOneFlagThatSaysSo() {
		// These four used to fire unconditionally at their call sites, so the flag describing them
		// was false while the behaviour was on. Pin that they now read it.
		for (Event event : Event.values()) {
			if (event == Event.GENIE) {
				continue;
			}
			assertEquals(Config.RANDOM_EVENT_CLASSIC_OTHERS_ENABLED, event.isFlagged(),
					event + " must follow RANDOM_EVENT_CLASSIC_OTHERS_ENABLED");
		}
	}

	@Test
	void noCandidatesPicksNothing() {
		// This is the "everything is off" path, including the master switch being false: it must
		// return null rather than divide by a total weight of zero.
		assertNull(RandomEventManager.pick(0, EnumSet.noneOf(Event.class)));
		assertNull(RandomEventManager.pick(9999, EnumSet.noneOf(Event.class)));
	}

	@Test
	void aDisabledEventIsNeverPicked() {
		Set<Event> justTheGenie = EnumSet.of(Event.GENIE);
		for (int roll = 0; roll < 500; roll++) {
			assertEquals(Event.GENIE, RandomEventManager.pick(roll, justTheGenie),
					"nothing but the only candidate can come back, whatever the roll");
		}
	}

	@Test
	void theSelectorIsNeverNullForAnyRollWhenSomethingIsEnabled() {
		Set<Event> all = EnumSet.allOf(Event.class);
		for (int roll = -200; roll < 200; roll++) {
			assertNotNull(RandomEventManager.pick(roll, all), "roll " + roll + " must land on some event");
		}
	}

	@Test
	void theWeightBandsFollowTheEnumOrder() {
		// Every event carries weight 20, so 100 in total and each band is exactly 20 wide. The
		// bands being contiguous and in ordinal order is what makes a run of rolls predictable.
		Set<Event> all = EnumSet.allOf(Event.class);
		Event[] order = Event.values();
		for (int i = 0; i < order.length; i++) {
			assertEquals(order[i], RandomEventManager.pick(i * 20, all), "first roll of band " + i);
			assertEquals(order[i], RandomEventManager.pick(i * 20 + 19, all), "last roll of band " + i);
		}
		// The selection is modulo the total, so the band repeats rather than running off the end.
		assertEquals(order[0], RandomEventManager.pick(100, all));
		assertEquals(order[1], RandomEventManager.pick(120, all));
	}

	@Test
	void aNegativeRollWrapsInsteadOfThrowing() {
		// floorMod, not %, so the band index cannot come out negative.
		Set<Event> all = EnumSet.allOf(Event.class);
		assertEquals(Event.values()[Event.values().length - 1], RandomEventManager.pick(-1, all));
		assertEquals(Event.values()[0], RandomEventManager.pick(-100, all));
	}

	@Test
	void aPartialCandidateSetRenormalisesTheWeightBands() {
		// Only two of five enabled: the total drops to 40, so the bands are 0-19 and 20-39 rather
		// than the disabled events leaving a hole in the range.
		Set<Event> pair = EnumSet.of(Event.SPIRIT_TREE, Event.ZOMBIE);
		assertEquals(Event.SPIRIT_TREE, RandomEventManager.pick(0, pair));
		assertEquals(Event.SPIRIT_TREE, RandomEventManager.pick(19, pair));
		assertEquals(Event.ZOMBIE, RandomEventManager.pick(20, pair));
		assertEquals(Event.ZOMBIE, RandomEventManager.pick(39, pair));
		assertEquals(Event.SPIRIT_TREE, RandomEventManager.pick(40, pair));
	}

	// ------------------------------------------------------------------ nest

	@Test
	void theNestDistributionIsMostlySeedsThenRings() {
		// Necrotic's bands: 0-640 seed (64.1%), 641-960 ring (32.0%), 961-1000 the egg nests.
		assertEquals(NEST_SEED, RandomEventManager.nestType(0));
		assertEquals(NEST_SEED, RandomEventManager.nestType(640));
		assertEquals(NEST_RING, RandomEventManager.nestType(641));
		assertEquals(NEST_RING, RandomEventManager.nestType(960));
	}

	@Test
	void theEggBandsReachAllThreeEggNests() {
		// The egg branch picks its colour at random, so what is assertable is that the whole
		// 961-1000 band stays inside the egg ids and that all three are reachable.
		Set<Integer> seen = new HashSet<>();
		for (int roll = 961; roll <= 1000; roll++) {
			for (int attempt = 0; attempt < 50; attempt++) {
				int nest = RandomEventManager.nestType(roll);
				assertTrue(nest == NEST_RED_EGG || nest == NEST_GREEN_EGG || nest == NEST_BLUE_EGG,
						"roll " + roll + " gave " + nest + ", which is not an egg nest");
				seen.add(nest);
			}
		}
		assertEquals(3, seen.size(), "all three egg nests must be reachable, not just the red one");
	}

	@Test
	void everyRollOfTheWholeBandGivesANestThatExists() {
		for (int roll = 0; roll <= 1000; roll++) {
			int nest = RandomEventManager.nestType(roll);
			boolean known = false;
			for (int id : NEST_IDS) {
				if (id == nest) {
					known = true;
				}
			}
			assertTrue(known, "roll " + roll + " gave " + nest + ", which is not a nest item");
		}
	}

	@Test
	void theOldCodeCouldOnlyEverGiveTheRedEggNest() {
		// The point of taking Necrotic's table: the old birdNests() added 5070 unconditionally, so
		// the seed and ring nests -- both of which ClickItem already knows how to open -- were
		// unreachable from woodcutting.
		assertNotEquals(NEST_RED_EGG, RandomEventManager.nestType(0),
				"the common band is no longer the red egg nest");
		assertEquals(NEST_SEED, RandomEventManager.nestType(0));
	}

	@Test
	void aNestRollEventuallyPutsANestInTheInventory() {
		// Proves rollNest is wired through to the inventory rather than only computing a table.
		// One roll in twenty-one lands, so the loop breaks almost immediately; it is bounded only
		// so a broken roll cannot hang the suite.
		Client c = client();
		for (int attempt = 0; attempt < 400 && !hasNest(c); attempt++) {
			RandomEventManager.rollNest(c);
		}
		assertTrue(hasNest(c), "400 rolls at 1-in-21 should have produced a nest");
	}

	private static boolean hasNest(Client c) {
		for (int slot = 0; slot < c.playerItems.length; slot++) {
			// Items are stored as id + 1.
			for (int id : NEST_IDS) {
				if (c.playerItems[slot] == id + 1) {
					return true;
				}
			}
		}
		return false;
	}

	// ------------------------------------------------------------- countdown

	@Test
	void aFreshCharacterArmsTheCountdownRatherThanFiringAtOnce() {
		// The field starts at 0 and 0 means "not armed", so the first action must set a delay and
		// fire nothing. Otherwise every new account's first log would roll a random event.
		Client c = client();
		assertEquals(0, c.randomEventCounter, "the field starts unarmed");

		RandomEventManager.onSkillAction(c);

		assertTrue(c.randomEventCounter >= RandomEventManager.FIRST_DELAY,
				"the first action should arm the countdown, not consume it");
		assertTrue(c.randomEventCounter <= RandomEventManager.FIRST_DELAY + RandomEventManager.DELAY_SPREAD);
		CycleEventHandler.stopEvents(c);
	}

	@Test
	void theCountdownFiresOnTheActionItReachesZeroAndThenRearms() {
		// The whole countdown contract in one assertion pair: after N-1 actions the counter is
		// exactly 1, so nothing has fired; the Nth action re-arms it to a full delay, which can
		// only have happened by way of the fire path.
		Client c = client();
		c.randomEventCounter = RandomEventManager.FIRST_DELAY;
		for (int i = 1; i < RandomEventManager.FIRST_DELAY; i++) {
			RandomEventManager.onSkillAction(c);
		}
		assertEquals(1, c.randomEventCounter, "one action short of firing, the counter must sit at 1");

		RandomEventManager.onSkillAction(c);

		assertTrue(c.randomEventCounter >= RandomEventManager.FIRST_DELAY,
				"firing must re-arm the next delay");
	}

	@Test
	void theFirstArmingDelayIsInsideTheDocumentedRange() {
		for (int i = 0; i < 200; i++) {
			int delay = RandomEventManager.nextDelay();
			assertTrue(delay >= RandomEventManager.FIRST_DELAY && delay <= RandomEventManager.FIRST_DELAY + RandomEventManager.DELAY_SPREAD,
					"delay " + delay + " is outside the configured range");
		}
	}

	@Test
	void aBotNeverRollsARandomEvent() {
		// Bots occupy real player slots and run the same skilling code, so without this a bot
		// chopping for an hour would accumulate genies and nests.
		Client c = client();
		c.isBot = true;
		c.randomEventCounter = 1;
		RandomEventManager.onSkillAction(c);
		assertEquals(1, c.randomEventCounter, "a bot's counter must not even advance");
		assertFalse(hasNest(c));
	}

	@Test
	void anEventThatCouldNotAppearDoesNotInterrupt() {
		// Force the no-NPC case rather than relying on it: with Server.npcHandler null the genie
		// cannot be spawned, so the counter is spent but the caller must not stop the player's
		// action for an NPC that never arrived. The old code stopped the action whether or not the
		// spawn had happened.
		server.game.npcs.NPCHandler saved = Server.npcHandler;
		try {
			Server.npcHandler = null;
			Client c = client();
			c.randomEventCounter = 1;
			boolean interrupted = RandomEventManager.onSkillAction(c);
			assertFalse(interrupted, "a genie that could not spawn must not stop the action");
		} finally {
			Server.npcHandler = saved;
		}
	}
}
