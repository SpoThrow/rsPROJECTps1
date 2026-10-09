package server.game.bots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The kit table: what a freshly created bot of a given kind owns and where it stands —
 * {@code BOT_ACCOUNTS.md} §4.1.
 *
 * <p><b>Not to be confused with {@link BotProfile}, and the names really are one letter apart.</b>
 * {@code BotProfile} is one <em>row</em> of {@code Data/cfg/bots.cfg}: an account, a password, a script.
 * {@code BotProfiles} (this class) is the <em>archetype</em> a new account is created from: tie, kit,
 * starting tile, spellbook. The two names come from the design docs and are kept for that reason, but the
 * distinction is the one that matters: a row says <em>which</em> bot, a profile says <em>what a bot of this
 * kind starts with</em>.
 *
 * <p><b>Why this exists at all.</b> A bot account must not be created through the normal new-player path,
 * because {@code addStarter} is IP-gated (all bots share the loopback address, so only the first would
 * ever receive a kit) and starts a tutorial that sets {@code canWalk = false}, freezing the bot it was
 * meant to help ({@code BOT_ACCOUNTS.md} §1). Provisioning is the replacement, and this is its data.
 *
 * <p><b>Items are deliberately lean.</b> The {@code adventurer} starter grants 2,000,000 coins and a set
 * of armour; that is too rich for an account meant to look like an ordinary newcomer, and a gathering bot
 * needs none of it. Each profile therefore carries the tool for its job and nothing else.
 *
 * <p><b>Every item id here is checked against this server's own {@code Data/cfg/item.cfg}</b>, not taken
 * from OSRS memory, because a wrong id is an invisible failure: {@code addItem} silently grants nothing
 * and the bot simply never chops.
 *
 * <p><b>No {@code prefix} field.</b> The design doc lists one, but name generation is {@link BotNames}'s
 * job and {@code BotNames.PREFIX} is already the single source of it, so a per-profile copy would be a
 * second answer to the same question and would drift. It can be added when slug generation actually needs
 * to vary by kind.
 */
public final class BotProfiles {

	/** The three archetypes the server already has, as the {@code xxxPid} flags on {@code Player}. */
	public enum Tie {
		ADVENTURER, PKER, SKILLER
	}

	// Item ids, each verified against Data/cfg/item.cfg. Named so the table below reads as a kit.
	private static final int BRONZE_AXE = 1351;
	private static final int BRONZE_PICKAXE = 1265;
	private static final int SMALL_FISHING_NET = 303;
	private static final int FLY_FISHING_ROD = 309;
	private static final int FEATHER = 314;
	private static final int TINDERBOX = 590;

	/**
	 * A kit: the tie, the items, and where the character first stands.
	 *
	 * <p>Immutable, and the item arrays are copied on construction, so a shared profile constant cannot be
	 * mutated by a caller that only meant to read it.
	 */
	public static final class Profile {

		private final String name;
		private final Tie tie;
		private final int[] itemIds;
		private final int[] itemAmounts;
		private final int startX;
		private final int startY;
		private final int plane;
		private final int spellbook;

		private Profile(String name, Tie tie, int startX, int startY, int plane, int spellbook,
				int[] itemIds, int[] itemAmounts) {
			this.name = name;
			this.tie = tie;
			this.startX = startX;
			this.startY = startY;
			this.plane = plane;
			this.spellbook = spellbook;
			this.itemIds = itemIds.clone();
			this.itemAmounts = itemAmounts.clone();
		}

		/** The name a {@code bots.cfg} row uses to ask for this kit. */
		public String name() {
			return name;
		}

		/** Which {@code xxxPid} flag provisioning sets, so profile-dependent code agrees with the kit. */
		public Tie tie() {
			return tie;
		}

		/** How many distinct items this kit grants. */
		public int itemCount() {
			return itemIds.length;
		}

		public int itemId(int index) {
			return itemIds[index];
		}

		public int itemAmount(int index) {
			return itemAmounts[index];
		}

		/** The tile a newly created account stands on, before any configured {@code home} applies. */
		public int startX() {
			return startX;
		}

		public int startY() {
			return startY;
		}

		public int plane() {
			return plane;
		}

		/** {@code 0} modern, {@code 1} ancient, {@code 2} lunar — the values {@code MagicState} uses. */
		public int spellbook() {
			return spellbook;
		}

		@Override
		public String toString() {
			return name + " (" + tie + ", " + itemIds.length + " item(s))";
		}
	}

	/**
	 * The kit a row gets when it does not name one.
	 *
	 * <p>Deliberately the generalist rather than the cheapest: a bot with no stated profile is the case
	 * where the operator did not know or care what its script would need, so this carries a tool for each
	 * of the three resources the world supports ({@code LocationKind}: {@code TREE}, {@code ROCK},
	 * {@code FISHING}). That way the obvious config line — an account, a script, a home — works without a
	 * fourth field, which is what {@code BOT_ACCOUNTS.md} §1 means by "a config line is meant to be
	 * sufficient".
	 */
	public static final Profile DEFAULT = new Profile("default", Tie.SKILLER, 3087, 3236, 0, 0,
			new int[] { BRONZE_AXE, BRONZE_PICKAXE, SMALL_FISHING_NET, TINDERBOX },
			new int[] { 1, 1, 1, 1 });

	/** Draynor willows, matching the default tile so an unspecified kit and a woodcutter agree. */
	public static final Profile WOODCUTTER = new Profile("woodcutter", Tie.SKILLER, 3087, 3236, 0, 0,
			new int[] { BRONZE_AXE, TINDERBOX }, new int[] { 1, 1 });

	/** Varrock east mine. */
	public static final Profile MINER = new Profile("miner", Tie.SKILLER, 3285, 3366, 0, 0,
			new int[] { BRONZE_PICKAXE }, new int[] { 1 });

	/** Draynor fishing: a net for the shallows, a rod and feathers for the river. */
	public static final Profile FISHER = new Profile("fisher", Tie.SKILLER, 3086, 3228, 0, 0,
			new int[] { SMALL_FISHING_NET, FLY_FISHING_ROD, FEATHER }, new int[] { 1, 1, 200 });

	private static final List<Profile> ALL = Collections.unmodifiableList(java.util.Arrays.asList(
			DEFAULT, WOODCUTTER, MINER, FISHER));

	private BotProfiles() {
	}

	/** Every profile, in table order. */
	public static List<Profile> all() {
		return ALL;
	}

	/** The profile's name, sorted, for a message that lists what a config row could have asked for. */
	public static List<String> names() {
		List<String> out = new ArrayList<String>();
		for (Profile profile : ALL) {
			out.add(profile.name());
		}
		Collections.sort(out);
		return out;
	}

	/** The profile named {@code name}, case-insensitively, or null when there is no such profile. */
	public static Profile named(String name) {
		if (name == null) {
			return null;
		}
		String key = name.trim().toLowerCase(Locale.ROOT);
		for (Profile profile : ALL) {
			if (profile.name().equals(key)) {
				return profile;
			}
		}
		return null;
	}
}
