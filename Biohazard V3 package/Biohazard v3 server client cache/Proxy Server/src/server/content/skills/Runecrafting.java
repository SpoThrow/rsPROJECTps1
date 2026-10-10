package server.content.skills;

import server.Config;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.players.Client;
import server.game.players.Player;

/**
 * Runecrafting: binding rune essence into runes at an altar.
 *
 * <p><b>The old version was a single {@code while} loop, so the whole inventory was one tick.</b>
 * Clicking an altar played the animation and then bound every essence the player was carrying in
 * the same game cycle, which is not how the skill looks or reads anywhere: OSRS binds one essence
 * at a time, with the animation repeating for each, and a player who walks away stops. That is now
 * what happens here — one essence every {@link #ACTION_CYCLES} cycles, cancelling on a walk — using
 * the same shape as fletching, herblore and pottery.
 *
 * <p><b>Three things the loop rewrite fixed, all of them silent before.</b> The first is pacing,
 * above. The second is a real off-by-one in the multiplier: the old loop walked the level ladder
 * from index 1, so it counted at most {@code length - 1} thresholds — an air essence at level 99
 * made <b>nine</b> air runes, not ten, and every rune above it was one short of its top rung. The
 * ladder is now read as "how many thresholds has this level passed", where the first rung is the
 * level requirement itself, which is what the table has always meant. The third is that an altar
 * clicked with no essence did nothing at all and said nothing; it now says so.
 *
 * <p><b>Experience is deliberately unchanged.</b> It used to be, and still is, the rune's base
 * experience times {@link Config#RUNECRAFTING_EXPERIENCE} (15 here) per essence — it does
 * <em>not</em> scale with the rune multiplier. OSRS does scale it, so this is the one place the
 * skill and the real game disagree, and it is left alone on purpose: the config value is already a
 * fifteen-fold rate, and multiplying that by ten more would be a balance change rather than a
 * realism one. It is recorded in {@code QOL_PLAN.md} as an open question for the operator.
 *
 * <p>The table itself is this revision's own: thirteen altars, the runes and levels as the guide
 * prints them. {@link #isAltar(int)} exists so the altar list is asked for rather than copied —
 * {@code ClickObject} used to carry its own duplicate of it, and that copy was missing the soul
 * altar ({@code 30625}), so the plain altar could not be used directly.
 */
public class Runecrafting {

	/** Rune essence, the only thing an altar consumes. */
	public static final int RUNE_ESSENCE = 1436;

	/** Binding, played once per essence. Both ids are the old path's, unchanged. */
	static final int BIND_ANIMATION = 791;
	static final int BIND_GFX = 186;

	/** Said when an altar is clicked with nothing to bind, rather than the old silence. */
	static final String NO_ESSENCE = "You do not have any rune essence to bind.";

	/**
	 * Event id for the ticked binding action, non-zero for the same reason fletching's is: it is what
	 * lets {@link #cancel} stop this action and nothing else the player is running.
	 */
	private static final int RUNECRAFTING_EVENT = 4620;

	/** Two cycles per essence, matching fletching and herblore. */
	private static final int ACTION_CYCLES = 2;

	/**
	 * Altar object id -> the rune it makes, its base experience, and the level ladder that decides
	 * how many runes one essence yields.
	 *
	 * <p>The ladder is the OSRS one transcribed: the first rung is the level needed to make the rune
	 * at all, and each rung after it adds one more rune per essence. So air is x1 at level 1 up to
	 * x10 at 99, and law — whose ladder is a single rung — is one per essence for ever, which is why
	 * it looks empty rather than broken.
	 */
	public enum RunecraftingData {

		AIR(2478, 556, 5, new int[] { 1, 11, 22, 33, 44, 55, 66, 77, 88, 99 }),
		MIND(2479, 558, 5.5, new int[] { 1, 14, 28, 42, 56, 70, 84, 98 }),
		WATER(2480, 555, 6, new int[] { 5, 19, 38, 57, 76, 95 }),
		EARTH(2481, 557, 6.5, new int[] { 9, 26, 52, 78 }),
		FIRE(2482, 554, 7, new int[] { 14, 35, 70 }),
		BODY(2483, 559, 7.5, new int[] { 20, 46, 92 }),
		COSMIC(2484, 564, 8, new int[] { 27, 59 }),
		CHAOS(2487, 562, 8.5, new int[] { 35, 74 }),
		NATURE(2486, 561, 9, new int[] { 44, 91 }),
		LAW(2485, 563, 9.5, new int[] { 54 }),
		DEATH(2488, 565, 10, new int[] { 65 }),
		BLOOD(30624, 560, 10.5, new int[] { 77 }),
		SOUL(30625, 566, 11, new int[] { 90 });

		private final int altarId;
		private final int runeId;
		private final double xp;
		private final int[] multiplier;

		RunecraftingData(int altarId, int runeId, double xp, int[] multiplier) {
			this.altarId = altarId;
			this.runeId = runeId;
			this.xp = xp;
			this.multiplier = multiplier;
		}

		public int getAltarId() {
			return altarId;
		}

		public int getRuneId() {
			return runeId;
		}

		public double getXp() {
			return xp;
		}

		/** The level needed to make the rune at all, which is the ladder's first rung. */
		public int getLevel() {
			return multiplier[0];
		}

		/**
		 * How many runes one essence yields at {@code level}: one, plus one for every rung of the
		 * ladder beyond the level requirement that the level has reached.
		 *
		 * <p>Counted from the table rather than walked, so the last rung is included — see the class
		 * comment for the off-by-one this replaces.
		 */
		public int getMultiplierForLevel(int level) {
			int runes = 0;
			for (int threshold : multiplier) {
				if (level >= threshold) {
					runes++;
				}
			}
			return runes;
		}
	}

	/** The altar a clicked object id is, or {@code null} when it is not an altar. */
	public static RunecraftingData forAltar(int objectId) {
		for (RunecraftingData altar : RunecraftingData.values()) {
			if (altar.altarId == objectId) {
				return altar;
			}
		}
		return null;
	}

	/** True when the clicked object is one of the thirteen altars. */
	public static boolean isAltar(int objectId) {
		return forAltar(objectId) != null;
	}

	/**
	 * Binds the player's essence at an altar, one essence every {@link #ACTION_CYCLES} cycles, until
	 * the essence runs out or the player walks away.
	 *
	 * <p>Called on every altar click, including the rifts that teleport first ({@code
	 * RuneRiftObjects}), so an object id that is not an altar is a no-op rather than an error.
	 *
	 * <p>The level and the essence are checked before the action starts, and the essence again on
	 * every cycle: a batch runs long enough for the player to bank or drop the stack, and a depleted
	 * stack has to end the action rather than bind a rune out of nothing. The multiplier is re-read
	 * each cycle, so a level-up part-way through a batch pays out at the new rate immediately.
	 */
	public static void craftRunes(final Client c, int objectId) {
		final RunecraftingData altar = forAltar(objectId);
		if (altar == null) {
			return;
		}
		// A batch is already running. Without this, clicking the altar again would start a second
		// loop over the same essence.
		if (c.playerSkilling[Player.playerRunecrafting]) {
			return;
		}
		if (c.skills.playerLevel[Player.playerRunecrafting] < altar.getLevel()) {
			c.sendMessage("You need a runecrafting level of " + altar.getLevel()
					+ " to craft this rune.");
			return;
		}
		if (!c.getItems().playerHasItem(RUNE_ESSENCE, 1)) {
			c.sendMessage(NO_ESSENCE);
			return;
		}

		// Read once here so the first essence is bound on the same tick the animation starts, rather
		// than a cycle later; inside the loop it is re-read from the player.
		final int xpPerEssence = (int) altar.getXp() * Config.RUNECRAFTING_EXPERIENCE;

		c.playerSkilling[Player.playerRunecrafting] = true;
		c.startAnimation(BIND_ANIMATION);
		c.gfx100(BIND_GFX);

		CycleEventHandler.addEvent(RUNECRAFTING_EVENT, c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				if (!c.playerSkilling[Player.playerRunecrafting]
						|| !c.getItems().playerHasItem(RUNE_ESSENCE, 1)) {
					container.stop();
					return;
				}
				int runes = altar.getMultiplierForLevel(c.skills.playerLevel[Player.playerRunecrafting]);
				c.getItems().deleteItem2(RUNE_ESSENCE, 1);
				c.getItems().addItem(altar.getRuneId(), runes);
				c.getPA().addSkillXP(xpPerEssence, Player.playerRunecrafting);
				// Re-armed each cycle, as herblore and pottery do: one animation lasts about one
				// binding, so a full inventory would otherwise be silent and still after the first.
				c.startAnimation(BIND_ANIMATION);
				c.gfx100(BIND_GFX);
			}

			@Override
			public void stop() {
				cancel(c);
			}
		}, ACTION_CYCLES);
	}

	/**
	 * Stops a running binding action, if one is running.
	 *
	 * <p>Keyed on its own event id rather than on the player: {@code stopEvents(c)} would stop every
	 * event the player owns, and they own other skills' events too. Called from
	 * {@code PlayerAssistant.resetVariables}, which every walk reaches, so walking away ends the
	 * action the way it ends fletching and herblore.
	 */
	public static void cancel(Client c) {
		if (c.playerSkilling[Player.playerRunecrafting]) {
			c.playerSkilling[Player.playerRunecrafting] = false;
			CycleEventHandler.stopEvents(c, RUNECRAFTING_EVENT);
			c.startAnimation(65535);
		}
	}
}
