package server.content.skills;

import server.Config;
import server.game.players.Client;
import server.game.players.Player;

/**
 * Runecrafting: binding rune essence into runes at an altar.
 *
 * <p><b>One click at an altar binds the whole pack, in one tick.</b> That is how the skill reads:
 * the bound animation plays once, the whole stack of essence is drawn in, and the player walks away
 * with the runes. An earlier revision paced it one essence every two cycles, which is not that —
 * it made a full pack a fourteen-second wait and, worse, it turned the altar into an AFK loop, so
 * clicking once and banking mid-action kept paying out. The click is the action now; there is no
 * action left to interrupt, so nothing here is tied into {@code resetVariables} any more.
 *
 * <p><b>Rune essence and pure essence are both essence.</b> The old code consumed only rune essence
 * ({@code 1436}), so a pack of pure essence ({@code 7936}) — what the members' mine and the essence
 * impling hand out — did nothing at all at an altar. Both are accepted at every altar, with rune
 * essence drawn down first; the OSRS tiering where the higher altars insist on pure essence is not
 * modelled, which is recorded in {@code QOL_PLAN.md} rather than guessed at.
 *
 * <p><b>The multiplier ladder is read as "how many rungs has this level passed".</b> The first rung
 * is the level requirement itself, so air is x1 at level 1 up to x10 at 99. The old loop walked the
 * ladder from index 1, so it counted at most {@code length - 1} thresholds — an air essence at
 * level 99 made <b>nine</b> air runes, not ten, and every rune above it was one short of its top
 * rung.
 *
 * <p><b>Experience is deliberately unchanged.</b> It is the rune's base experience times
 * {@link Config#RUNECRAFTING_EXPERIENCE} (15 here) per essence — it does <em>not</em> scale with the
 * rune multiplier. OSRS does scale it, so this is the one place the skill and the real game
 * disagree, and it is left alone on purpose: the config value is already a fifteen-fold rate, and
 * multiplying that by ten more would be a balance change rather than a realism one. It is recorded
 * in {@code QOL_PLAN.md} as an open question for the operator. Note the whole pack's experience is
 * awarded as one sum, so a click that crosses more than one level announces the level it lands on.
 *
 * <p>The table itself is this revision's own: thirteen altars, the runes and levels as the guide
 * prints them. {@link #isAltar(int)} exists so the altar list is asked for rather than copied —
 * {@code ClickObject} used to carry its own duplicate of it, and that copy was missing the soul
 * altar ({@code 30625}), so the plain altar could not be used directly.
 */
public class Runecrafting {

	/** Rune essence: the free-to-play essence, and the one the old code knew about. */
	public static final int RUNE_ESSENCE = 1436;

	/** Pure essence: the members' essence. Same job, and previously unusable at every altar. */
	public static final int PURE_ESSENCE = 7936;

	/** Binding. Played once per click, not once per essence. */
	static final int BIND_ANIMATION = 791;
	static final int BIND_GFX = 186;

	/** Said when an altar is clicked with nothing to bind, rather than the old silence. */
	static final String NO_ESSENCE = "You do not have any rune or pure essence to bind.";

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
	 * Binds every essence the player is carrying at an altar, in one tick.
	 *
	 * <p>Called on every altar click, including the rifts that teleport first ({@code
	 * RuneRiftObjects}), so an object id that is not an altar is a no-op rather than an error.
	 *
	 * <p>The level and the essence are checked before anything is spent. The rune and experience
	 * totals are worked out from the whole pack up front, the essence is drawn down, and the runes
	 * and the experience are handed over together — one animation, one experience drop. Rune
	 * essence is consumed before pure essence so that a mixed pack spends the cheap essence first.
	 */
	public static void craftRunes(final Client c, int objectId) {
		final RunecraftingData altar = forAltar(objectId);
		if (altar == null) {
			return;
		}
		if (c.skills.playerLevel[Player.playerRunecrafting] < altar.getLevel()) {
			c.sendMessage("You need a runecrafting level of " + altar.getLevel()
					+ " to craft this rune.");
			return;
		}

		int runeEssence = c.getItems().getItemAmount(RUNE_ESSENCE);
		int pureEssence = c.getItems().getItemAmount(PURE_ESSENCE);
		int essence = runeEssence + pureEssence;
		if (essence <= 0) {
			c.sendMessage(NO_ESSENCE);
			return;
		}

		int runes = essence * altar.getMultiplierForLevel(c.skills.playerLevel[Player.playerRunecrafting]);
		int xp = essence * (int) altar.getXp() * Config.RUNECRAFTING_EXPERIENCE;

		c.startAnimation(BIND_ANIMATION);
		c.gfx100(BIND_GFX);
		if (runeEssence > 0) {
			c.getItems().deleteItem2(RUNE_ESSENCE, runeEssence);
		}
		if (pureEssence > 0) {
			c.getItems().deleteItem2(PURE_ESSENCE, pureEssence);
		}
		c.getItems().addItem(altar.getRuneId(), runes);
		c.getPA().addSkillXP(xp, Player.playerRunecrafting);
	}
}
