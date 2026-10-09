package server.game.bots;

/**
 * One bot line from {@code Data/cfg/bots.cfg} — roadmap Phase E, {@code BOT_ACCOUNTS.md} §3.
 *
 * <p><b>This is the operator's record, not the account.</b> The account is the character file under
 * {@code Data/characters/}; this is the readable row that says which account to possess, with what
 * password, running which script, starting where. The two are kept separate because the character file
 * stores only an {@code md5} — a human cannot log into a hash, so the plaintext the operator needs has
 * to live somewhere readable, and this is that place.
 *
 * <p><b>{@code account} is the identity.</b> It is the name {@link BotManager} keys on and the name the
 * server knows the character by, so there is no second spelling to keep in step: {@code ::bot despawn}
 * takes what the login would take. It must be login-legal ({@link BotNames#isLoginLegal}) because a
 * possessed bot is a real account a human can also log into ({@code BOT_ACCOUNTS.md} §1) — the login
 * decoder only accepts {@code [a-z0-9 ]}, so a row naming anything else could never be handed over.
 *
 * <p><b>{@code home} stays a name, not a place.</b> Parsing reads a file; resolving a name loads the
 * world ({@link server.game.bots.world.Locations}). Keeping the string here and resolving it at spawn
 * means merely reading the config touches no world, and a home that no longer exists is a message at
 * spawn rather than a reason the whole file fails to load.
 */
public final class BotProfile {

	private final String account;
	private final String password;
	private final String script;
	private final String home;
	private final String profile;
	private final boolean enabled;

	/** A row with no {@code profile}: it gets {@link BotProfiles#DEFAULT} if the account is created. */
	public static BotProfile of(String account, String password, String script, String home,
			boolean enabled) {
		return new BotProfile(account, password, script, home, null, enabled);
	}

	public static BotProfile of(String account, String password, String script, String home,
			String profile, boolean enabled) {
		return new BotProfile(account, password, script, home, profile, enabled);
	}

	private BotProfile(String account, String password, String script, String home, String profile,
			boolean enabled) {
		this.account = account;
		this.password = password;
		this.script = script;
		this.home = home == null || home.isBlank() ? null : home.trim();
		this.profile = profile == null || profile.isBlank() ? null : profile.trim();
		this.enabled = enabled;
	}

	/** The login name, and the only identity this row has. Already lowercased, as login lowercases. */
	public String account() {
		return account;
	}

	/** The plaintext password. Present so a human can log in; the character file holds only its hash. */
	public String password() {
		return password;
	}

	/** The {@code BotScripts} name to run. */
	public String script() {
		return script;
	}

	/** The authored {@code Locations} name to stand at on spawn, or null for wherever the account was. */
	public String home() {
		return home;
	}

	/**
	 * The {@link BotProfiles} name this account is created from, or null for
	 * {@link BotProfiles#DEFAULT}.
	 *
	 * <p>Applied <b>only when the character does not exist yet</b>: the kit belongs to creation, so a row
	 * edited after its account was made changes nothing about what that account owns. That is stated
	 * rather than merely implemented, because it is the kind of thing an operator would otherwise expect
	 * to work and then report as a bug.
	 */
	public String profile() {
		return profile;
	}

	/** Whether {@link BotManager#start()} should spawn this row. */
	public boolean enabled() {
		return enabled;
	}

	/** The display name: the account without the reserved bot prefix, for logs and listings. */
	public String title() {
		return BotNames.hasBotPrefix(account) ? account.substring(BotNames.PREFIX.length()) : account;
	}

	@Override
	public String toString() {
		return account + " (" + script + (profile == null ? "" : ", " + profile)
				+ (enabled ? "" : ", disabled") + ")";
	}
}
