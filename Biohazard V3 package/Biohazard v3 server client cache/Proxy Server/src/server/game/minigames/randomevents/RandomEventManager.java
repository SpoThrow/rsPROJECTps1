package server.game.minigames.randomevents;

import java.util.EnumSet;
import java.util.Set;

import core.util.Misc;
import server.Config;
import server.Server;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.npcs.NPC;
import server.game.players.Client;

/**
 * The one place a random event is decided. Every skilling action calls
 * {@link #onSkillAction(Client)}; nothing else spawns a random event.
 *
 * <p><b>Why this exists.</b> Before this, each event was rolled inline at its own call site:
 * {@code Misc.random(250) == 0} in Woodcutting, three times in Mining, once in Fishing and three
 * times in Prayer, each immediately followed by a hardcoded {@code SpiritTree.spawnSpiritTree(c)}
 * or similar. Two things followed from that. The first is that {@code Config.RANDOM_EVENT_*} did
 * not do anything — {@code RANDOM_EVENT_CLASSIC_OTHERS_ENABLED} was {@code false} while
 * {@code SpiritTree}, {@code RockGolem}, {@code RiverTroll} and {@code Zombie} fired on every
 * matching action anyway, so the flags describing the shipped behaviour were wrong. The second is
 * that the rate was decided per skill with no way to see or change it as a whole.
 *
 * <p><b>Two different rhythms, deliberately.</b> A bird's nest and an interrupting NPC are not the
 * same kind of thing and are not rolled the same way:
 *
 * <ul>
 * <li>a nest is a <b>frequent small bonus</b>, rolled on every action at {@link #NEST_CHANCE},
 *     exactly as it was before this class existed. Giving it a countdown weight like the others
 *     would have made nests roughly twenty times rarer, which is a nerf that was not asked for;
 * <li>an NPC event is an <b>interruption</b>, so it runs on a per-player countdown of
 *     {@link #FIRST_DELAY} to {@code FIRST_DELAY + DELAY_SPREAD} actions. A countdown rather than a
 *     flat per-action chance is the shape taken from Necrotic ({@code 350 + random(100)}): it
 *     cannot fire twice in a row, and it makes "how often do I see a random event" a number
 *     somebody can read and set.
 * </ul>
 *
 * <p><b>The counter is per player and is saved</b>, so logging out does not reset it and a
 * relog is not a way to avoid events. It lives in {@code Player.randomEventCounter} and is
 * written by {@code PlayerSave}.
 */
public final class RandomEventManager {

	/** An NPC random event: the flag that gates it, its draw weight, and whether it stops the action. */
	public enum Event {

		/**
		 * The genie. Hands over an antique lamp, which is the lamp item the server already has
		 * ({@code 4447} — rub it to open the existing skill-choice interface), so the whole reward
		 * path is pre-existing and no client change is needed.
		 *
		 * <p>Does not interrupt: it appears, says its line and waits. There is nothing to react to,
		 * so stopping the player's woodcutting would only cost them a log.
		 */
		GENIE(Config.RANDOM_EVENT_GENIE_ENABLED, 20, false),

		SPIRIT_TREE(Config.RANDOM_EVENT_CLASSIC_OTHERS_ENABLED, 20, true),
		ROCK_GOLEM(Config.RANDOM_EVENT_CLASSIC_OTHERS_ENABLED, 20, true),
		RIVER_TROLL(Config.RANDOM_EVENT_CLASSIC_OTHERS_ENABLED, 20, true),
		ZOMBIE(Config.RANDOM_EVENT_CLASSIC_OTHERS_ENABLED, 20, true);

		private final boolean flag;
		private final int weight;
		private final boolean interrupts;

		Event(boolean flag, int weight, boolean interrupts) {
			this.flag = flag;
			this.weight = weight;
			this.interrupts = interrupts;
		}

		/** The event's own flag, without the master switch. */
		public boolean isFlagged() {
			return flag;
		}

		public int getWeight() {
			return weight;
		}

		/** Whether firing this event should stop the action that fired it. */
		public boolean interrupts() {
			return interrupts;
		}
	}

	/** The genie NPC, from {@code npc.cfg}. */
	public static final int GENIE_NPC = 409;

	/** The antique lamp the genie hands over, from {@code item.cfg}. */
	public static final int ANTIQUE_LAMP = 4447;

	/**
	 * Actions before the first interrupting event, and the spread added to it.
	 *
	 * <p>Kept from Necrotic's {@code CALL_RANDOM = 350 + random(100)}. Lower {@link #DELAY_SPREAD}
	 * for a more regular interval; lower {@link #FIRST_DELAY} to see events more often.
	 */
	public static final int FIRST_DELAY = 350;
	public static final int DELAY_SPREAD = 100;

	/**
	 * One roll in {@code NEST_CHANCE + 1} per skilling action, i.e. the {@code Misc.random(100) < 5}
	 * this replaces. Necrotic rolls nests at {@code 1/61}; see the class comment for why the rate
	 * was left alone.
	 */
	public static final int NEST_CHANCE = 100;
	public static final int NEST_PERCENT = 5;

	private static final int NEST_EMPTY = 5075;
	private static final int NEST_RED_EGG = 5070;
	private static final int NEST_GREEN_EGG = 5071;
	private static final int NEST_BLUE_EGG = 5072;
	private static final int NEST_SEED = 5073;
	private static final int NEST_RING = 5074;

	private static final int GENIE_TICKS = 200;

	private RandomEventManager() {
	}

	/**
	 * Rolls everything a skilling action can roll. Call this once per action, from the skill.
	 *
	 * @return true if an event fired and the caller should stop what it was doing. A nest never
	 *         asks for that; neither does the genie.
	 */
	public static boolean onSkillAction(Client c) {
		if (!Config.RANDOM_EVENTS_ENABLED || c.isBot) {
			return false;
		}
		rollNest(c);
		if (c.randomEventCounter == 0) {
			// Not armed yet: a fresh character, or a save written before this existed. Arm the
			// countdown and do not fire, so the first action cannot instantly produce an event.
			c.randomEventCounter = nextDelay();
			return false;
		}
		if (--c.randomEventCounter > 0) {
			return false;
		}
		Event fired = fire(c);
		c.randomEventCounter = nextDelay();
		return fired != null && fired.interrupts();
	}

	/** @return the delay before the next interrupting event, in actions. */
	public static int nextDelay() {
		return FIRST_DELAY + Misc.random(DELAY_SPREAD);
	}

	/**
	 * Rolls a bird's nest. Separate from the countdown; see the class comment.
	 */
	public static void rollNest(Client c) {
		if (!Config.RANDOM_EVENT_BIRD_NEST_ENABLED) {
			return;
		}
		if (Misc.random(NEST_CHANCE) >= NEST_PERCENT) {
			return;
		}
		c.getItems().addItem(nestType(Misc.random(1000)), 1);
		c.sendMessage("A bird's nest falls out of the tree!");
	}

	/**
	 * Which nest a roll gives, using Necrotic's distribution: seed 64.1%, ring 32.0%, and the
	 * remaining 3.9% split between the three egg nests.
	 *
	 * <p>The old code always gave {@code 5070}, the red-egg nest, so seed and ring nests were
	 * unreachable from woodcutting even though {@code ClickItem} already knows how to open them.
	 *
	 * <p>The egg branch keeps Necrotic's mapping as found: {@code 0 → 5070}, {@code 1 → 5072},
	 * {@code 2 → 5071}. That is not the obvious order, but it is the source's, and the three egg
	 * nests differ only in colour.
	 *
	 * @param roll the result of {@code Misc.random(1000)} — 0 to 1000 inclusive
	 */
	public static int nestType(int roll) {
		if (roll <= 640) {
			return NEST_SEED;
		}
		if (roll <= 960) {
			return NEST_RING;
		}
		int egg = Misc.random(2);
		if (egg == 1) {
			return NEST_BLUE_EGG;
		}
		if (egg == 2) {
			return NEST_GREEN_EGG;
		}
		return NEST_RED_EGG;
	}

	/** The events the master switch and their own flags currently allow. */
	public static Set<Event> enabledEvents() {
		Set<Event> enabled = EnumSet.noneOf(Event.class);
		if (!Config.RANDOM_EVENTS_ENABLED) {
			return enabled;
		}
		for (Event event : Event.values()) {
			if (event.isFlagged()) {
				enabled.add(event);
			}
		}
		return enabled;
	}

	/**
	 * Weighs a roll against the candidate set.
	 *
	 * <p>Takes the candidates rather than reading {@link Config} so the gating can be tested: the
	 * flags are {@code static final}, so there is no way to flip one at runtime and see what
	 * happens. Iterating {@link Event#values()} rather than the set keeps the result independent of
	 * the set's iteration order.
	 *
	 * @param roll       any int; reduced modulo the total weight
	 * @param candidates the events allowed to fire; an empty set picks nothing
	 * @return the chosen event, or null if nothing is allowed to fire
	 */
	public static Event pick(int roll, Set<Event> candidates) {
		int total = 0;
		for (Event event : candidates) {
			total += event.getWeight();
		}
		if (total <= 0) {
			return null;
		}
		int remaining = Math.floorMod(roll, total);
		for (Event event : Event.values()) {
			if (!candidates.contains(event)) {
				continue;
			}
			remaining -= event.getWeight();
			if (remaining < 0) {
				return event;
			}
		}
		return null;
	}

	private static Event fire(Client c) {
		Set<Event> candidates = enabledEvents();
		if (candidates.isEmpty()) {
			return null;
		}
		Event event = pick(Misc.random(totalWeight(candidates)), candidates);
		if (event == null) {
			return null;
		}
		boolean spawned;
		switch (event) {
		case GENIE:
			spawned = spawnGenie(c);
			break;
		case SPIRIT_TREE:
			spawned = SpiritTree.spawnSpiritTree(c);
			break;
		case ROCK_GOLEM:
			spawned = RockGolem.spawnRockGolem(c);
			break;
		case RIVER_TROLL:
			spawned = RiverTroll.spawnRiverTroll(c);
			break;
		case ZOMBIE:
			spawned = Zombie.spawnZombie(c);
			break;
		default:
			return null;
		}
		// The old call sites stopped the player's action whether or not the NPC had actually
		// spawned, so a level-3 player burying bones next to nothing could be interrupted by
		// nothing. An event only interrupts now if it appeared.
		return spawned ? event : null;
	}

	private static int totalWeight(Set<Event> events) {
		int total = 0;
		for (Event event : events) {
			total += event.getWeight();
		}
		return total;
	}

	/**
	 * Spawns the genie beside the player for {@link #GENIE_TICKS} cycles, or until it hands over
	 * the lamp. Returns whether it appeared.
	 *
	 * <p>Spawned through {@link server.game.npcs.NPCHandler#spawnNpc2} rather than
	 * {@code spawnNpc}, which the four older events use: {@code spawnNpc2} hands back the NPC, so
	 * the despawn timer does not have to be another copy of the block in {@code spawnNpc} that
	 * finds the NPC by matching npc type.
	 *
	 * <p>{@code spawnedBy} is set so the genie is recognisably this player's.
	 */
	public static boolean spawnGenie(Client c) {
		if (c.genieSpawned || Server.npcHandler == null) {
			return false;
		}
		NPC genie = Server.npcHandler.spawnNpc2(GENIE_NPC, c.getX() + Misc.random(1), c.getY() + Misc.random(1),
				c.position.heightLevel, 0, 0, 0, 0, 0);
		if (genie == null) {
			// Every NPC slot is in use. Come back on the next countdown rather than treating it
			// as an event the player saw.
			return false;
		}
		genie.spawnedBy = c.getId();
		genie.forceChat("Greetings. I have a gift for you.");
		c.genieSpawned = true;
		CycleEventHandler.addEvent(c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				container.stop();
			}

			@Override
			public void stop() {
				genie.isDead = true;
				genie.updateRequired = true;
				c.genieSpawned = false;
			}
		}, GENIE_TICKS);
		return true;
	}

	/**
	 * Gives the lamp, once, to the player whose genie is out.
	 *
	 * <p>One lamp per spawn and not per click: {@code genieSpawned} is the same flag that guards
	 * spawning, so talking twice does not produce two lamps. A player clicking somebody else's
	 * genie is told to clear off, because their own flag is not set.
	 */
	public static boolean talkToGenie(Client c) {
		if (!c.genieSpawned) {
			c.getDH().sendNpcChat1("This lamp is not for you.", GENIE_NPC, "Genie");
			c.nextChat = 0;
			return false;
		}
		c.genieSpawned = false;
		c.getItems().addItem(ANTIQUE_LAMP, 1);
		c.getDH().sendNpcChat1("A gift, mortal. A lamp - rub it and a skill will improve.", GENIE_NPC, "Genie");
		c.nextChat = 0;
		return true;
	}
}
