package server.game.bots;

/**
 * Bot account naming.
 *
 * <p><b>Why a prefix and not punctuation.</b> The login decoder only accepts
 * {@code [A-Za-z0-9 ]+} ({@code RS2LoginProtocolDecoder.load()}: {@code returnCode = 4}
 * otherwise), so the {@code [bot]} form from the first draft could never be logged into —
 * which would defeat the possession model where a human can take the account over. The
 * prefix is therefore alphanumeric: {@link #PREFIX} + slug, e.g. {@code botwillow}.
 *
 * <p>This is a <em>naming convention</em> only. It is not a save gate: bots persist like
 * any other character (see {@code BOT_ACCOUNTS.md}).
 */
public final class BotNames {

	/** Lowercase, no punctuation; 3 chars leaves 9 for the slug inside the 12-char cap. */
	public static final String PREFIX = "bot";

	/** The server's login limit ({@code name.length() > 12} is refused). */
	public static final int MAX_NAME_LENGTH = 12;

	private BotNames() {
	}

	/** Builds a legal bot account name from a slug: {@code bot} + {@code slug}. */
	public static String accountFor(String slug) {
		return PREFIX + (slug == null ? "" : slug.trim().toLowerCase());
	}

	/** True when {@code name} carries the reserved bot prefix (case-insensitive). */
	public static boolean hasBotPrefix(String name) {
		return name != null && name.length() >= PREFIX.length()
				&& name.regionMatches(true, 0, PREFIX, 0, PREFIX.length());
	}

	/**
	 * Mirrors the login decoder's own checks so an account is only created if it could
	 * actually be logged into: non-empty, at most {@link #MAX_NAME_LENGTH} chars after
	 * {@code trim()}, and only {@code [a-z0-9 ]}.
	 */
	public static boolean isLoginLegal(String name) {
		if (name == null) {
			return false;
		}
		String normalised = name.trim().toLowerCase();
		if (normalised.isEmpty() || normalised.length() > MAX_NAME_LENGTH) {
			return false;
		}
		for (int i = 0; i < normalised.length(); i++) {
			char c = normalised.charAt(i);
			boolean allowed = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == ' ';
			if (!allowed) {
				return false;
			}
		}
		return true;
	}
}
