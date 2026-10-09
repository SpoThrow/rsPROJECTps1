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
 */
public final class BotProvisioning {

	/** What every skill but hitpoints starts at. */
	private static final int BASE_LEVEL = 1;
	private static final int BASE_XP = 0;

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
		applySkills(bot);
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
		applySkills(bot);
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

	/** Every skill to level {@link #BASE_LEVEL}, then hitpoints restored — see the class comment. */
	private static void applySkills(BotPlayer bot) {
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
	}

	private static void placeAt(BotPlayer bot, BotProfiles.Profile profile) {
		bot.position.absX = bot.position.teleportToX = profile.startX();
		bot.position.absY = bot.position.teleportToY = profile.startY();
		bot.position.heightLevel = profile.plane();
	}
}
