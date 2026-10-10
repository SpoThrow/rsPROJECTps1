package server.game.bots;

import server.Config;

/**
 * Grants a freshly created bot account its kit, skills and starting tile — {@code BOT_ACCOUNTS.md} §4.1.
 *
 * <p><b>Why not {@code PlayerAssistant.addStarter()}.</b> That path is IP-gated, so on the shared loopback
 * address only the first bot would ever receive a kit; it is two-tier (a first <em>and</em> a second
 * grant); it starts the tutorial, which sets {@code canWalk = false} and freezes the bot it is meant to
 * help; and it grants 2,000,000 coins, which is the opposite of an ordinary new account. Provisioning is
 * the direct replacement, and it is deliberately small.
 *
 * <p><b>Runs once, at account creation — never on possess.</b> Re-possessing a character must not re-grant
 * its kit, or a bot would accumulate a fresh inventory every restart. {@link BotManager#createAccount} is
 * the only production caller; {@link #reprovision} exists for the dev command that needs to rebuild a
 * character on purpose.
 *
 * <p><b>No packets are sent, and that is not an oversight.</b> A bot is sessionless — it has no socket, so
 * {@code getOutStream()} is null and every send in this codebase guards on exactly that. Calling
 * {@code refreshSkill} here would therefore throw rather than refresh. The state that matters is the
 * arrays, and those are what {@code PlayerSave.saveGame} writes; the interface catches up from the saved
 * file if a human ever logs in.
 *
 * <p><b>The hitpoints trap is pinned here rather than left implicit.</b> {@link server.game.players.Player}'s
 * constructor seeds every skill to level {@code 1} <em>except</em> hitpoints, which starts at
 * {@code 10} with the XP for {@code 10}. A loop that sets "all skills to 1" would quietly undo that and
 * leave the account at one hitpoint, so {@link #applySkills} restores hitpoints explicitly and
 * {@code BotProvisioningTest} asserts it.
 *
 * <p><b>Levels come from the profile, and their XP is derived, not guessed.</b> A kit grants the level its
 * job needs ({@link BotProfiles} says why), and each is paired with the XP this server's own
 * {@code getLevelForXP} reads back as that level — see {@link #xpForLevel}. Writing the level without the
 * XP is the subtle half-failure: the skill would look right on the array and wrong everywhere the server
 * asks {@code getLevelForXP} instead, which is the interface, the total level and the level-up message.
 */
public final class BotProvisioning {

	/** What every skill but hitpoints starts at. */
	private static final int BASE_LEVEL = 1;
	private static final int BASE_XP = 0;

	/**
	 * The highest level a profile may grant. {@code 99} because that is where {@code getLevelForXP} stops
	 * and where the skill interface stops drawing; a profile is a starting kit, so nothing here should ever
	 * be near it, but the table is data and data gets typo'd.
	 */
	private static final int MAX_LEVEL = 99;

	/**
	 * Hitpoints' starting level and XP, copied from {@code Player}'s constructor. Kept as its own constant
	 * pair with the reason attached, because the value is not derivable from {@link #BASE_LEVEL} and the
	 * cost of guessing it wrong is a character that is dead on arrival.
	 */
	private static final int STARTING_HITPOINTS = 10;
	private static final int STARTING_HITPOINTS_XP = 1300;

	private BotProvisioning() {
	}

	/**
	 * Applies {@code profile} to a client being created: tie flag, kit, skills, starting tile, spellbook,
	 * a clean tutorial state and the persistence flags.
	 *
	 * <p><b>The contract is split, and the halves differ on purpose:</b> the tie, skills, position and
	 * flags are idempotent — setting them twice is the same as setting them once — but <b>items are
	 * additive</b>. Calling this twice grants the kit twice. That is safe because
	 * {@link BotManager#createAccount} is the only production caller and it always provisions a brand-new,
	 * empty character; the replace path is {@link #reprovision}, which {@link #clear}s first. Do not
	 * "fix" this to clear: clearing inside here would silently wipe an account's inventory if it were ever
	 * called on a character that already had one.
	 */
	public static void provision(BotPlayer bot, BotProfiles.Profile profile) {
		if (bot == null || profile == null) {
			return;
		}
		applyTie(bot, profile.tie());
		for (int i = 0; i < profile.itemCount(); i++) {
			bot.getItems().addItem(profile.itemId(i), profile.itemAmount(i));
		}
		applySkills(bot, profile);
		placeAt(bot, profile);
		bot.magic.playerMagicBook = profile.spellbook();

		// The tutorial state, asserted rather than assumed. Client.initialize freezes movement when
		// addStarter is set (canWalk = false, plus a forced dialogue), and a frozen bot never ticks.
		bot.addStarter = false;
		bot.canWalk = true;

		// Without these a created account is treated as a new player and never written back, so the
		// character would vanish on the next restart along with everything just granted.
		bot.newPlayer = false;
		bot.saveFile = true;
		bot.saveCharacter = true;
	}

	/**
	 * Undoes what {@link #provision} granted: empties the inventory and equipment, clears the tie flags and
	 * resets skills. Present so {@link #reprovision} has something honest to build on — clearing and then
	 * applying is the only way to change a profile without the old kit lingering.
	 */
	public static void clear(BotPlayer bot) {
		if (bot == null) {
			return;
		}
		// removeAllItems, not deleteAllItems: the latter routes through deleteItem, which runs drop and
		// ground-item logic meant for a live player. This one just zeroes the arrays.
		bot.getItems().removeAllItems();
		applyTie(bot, null);
		applySkills(bot, null);
	}

	/** Clears and then applies {@code profile} — the dev-only "rebuild this character" path. */
	public static void reprovision(BotPlayer bot, BotProfiles.Profile profile) {
		if (bot == null || profile == null) {
			return;
		}
		clear(bot);
		provision(bot, profile);
	}

	/** Sets exactly one {@code xxxPid} flag; a null tie clears all three. */
	private static void applyTie(BotPlayer bot, BotProfiles.Tie tie) {
		bot.adventurerPid = tie == BotProfiles.Tie.ADVENTURER;
		bot.pkerPid = tie == BotProfiles.Tie.PKER;
		bot.skillerPid = tie == BotProfiles.Tie.SKILLER;
	}

	/**
	 * Every skill to level {@link #BASE_LEVEL}, then hitpoints restored, then {@code profile}'s own levels
	 * — see the class comment for why hitpoints is the exception and {@link BotProfiles} for why a kit
	 * carries levels at all.
	 *
	 * <p>A null {@code profile} is {@link #clear}'s "base levels, nothing else".
	 */
	private static void applySkills(BotPlayer bot, BotProfiles.Profile profile) {
		int[] levels = bot.skills.playerLevel;
		int[] xp = bot.skills.playerXP;
		for (int skill = 0; skill < levels.length; skill++) {
			levels[skill] = BASE_LEVEL;
			if (skill < xp.length) {
				xp[skill] = BASE_XP;
			}
		}
		if (Config.HITPOINTS < levels.length) {
			levels[Config.HITPOINTS] = STARTING_HITPOINTS;
			xp[Config.HITPOINTS] = STARTING_HITPOINTS_XP;
		}
		if (profile == null) {
			return;
		}
		for (int i = 0; i < profile.skillCount(); i++) {
			int skill = profile.skillId(i);
			int level = profile.skillLevel(i);
			if (skill < 0 || skill >= levels.length || skill >= xp.length
					|| level < 1 || level > MAX_LEVEL) {
				// A profile out of step with the skill array is a programming error, and the safe answer
				// is to leave the base level rather than write past the end of it. BotProfilesTest reads
				// the table directly, so this is a belt-and-braces guard rather than the check.
				continue;
			}
			levels[skill] = level;
			xp[skill] = xpForLevel(bot, level);
		}
	}

	/**
	 * The smallest XP this server reads as {@code level} — see {@link #applySkills} for why the two must
	 * agree rather than the level being written on its own.
	 *
	 * <p><b>Deliberately not a second copy of the XP curve.</b> {@code getLevelForXP} is the server's own
	 * answer to "what level is this XP", and it is the one everything else uses: the skill tab, total
	 * level, the level-up message, and {@code Client.process}'s slow drain of {@code playerLevel} back
	 * towards the trained level. Granting a level by writing {@code playerLevel} alone would leave those
	 * disagreeing and let that drain walk the character back down a level at a time. So the XP is derived
	 * from the server's own function, and {@code BotProvisioningTest} pins the round trip.
	 *
	 * <p><b>The {@code + 1} is the whole point.</b> {@code getXPForLevel(L)} returns the largest XP that
	 * still reads as level {@code L - 1} ({@code getLevelForXP} advances only once XP <em>exceeds</em> a
	 * threshold), so the first XP that reads as {@code L} is one past it.
	 */
	private static int xpForLevel(BotPlayer bot, int level) {
		return bot.getPA().getXPForLevel(level) + 1;
	}

	private static void placeAt(BotPlayer bot, BotProfiles.Profile profile) {
		bot.position.absX = bot.position.teleportToX = profile.startX();
		bot.position.absY = bot.position.teleportToY = profile.startY();
		bot.position.heightLevel = profile.plane();
	}
}
