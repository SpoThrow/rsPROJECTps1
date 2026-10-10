package server.game.bots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import server.Config;

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
 * <p><b>A tool is not enough, so a profile also carries the levels its job needs.</b> This was found the
 * hard way rather than designed in: the shipped tree places are {@code draynor_oaks} (oaks, level 15) and
 * the Draynor willows the {@code WOODCUTTER} row starts on (level 30), and every one of them refuses a
 * level-1 character — {@code Woodcutting.startWoodcutting} answers "You need a Woodcutting level of 30 to
 * cut this tree" and nothing happens. An axe in the hand and no level to swing it is the same dead bot as
 * no axe at all, so the level is part of the kit in the sense that matters: what the character must have
 * to do the job the profile is for.
 *
 * <p><b>The level is the profile's job, not a raise.</b> These are the first <em>useful</em> targets of
 * each kit — the tree the axe is meant for, the ore the pickaxe is meant for, the fish a rod and feathers
 * are for — and no more. A profile does not hand out 99s, and it does not touch a skill its kit has no
 * business with, so a miner is still a level-1 woodcutter. The generalist {@link #DEFAULT} carries the
 * levels of all three of its tools for the same reason it carries all three tools: a row that names no
 * profile has said nothing about which resource its script wants.
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

	// The levels the kits are for, from this server's own requirement tables rather than from memory:
	// Woodcutting.Tree_Settings has oak 15 and willow 30, Mining.data has iron 15, and the fishing
	// requirements put trout and salmon at 20 — the species a rod and feathers are actually for.
	private static final int OAK_LEVEL = 15;
	private static final int WILLOW_LEVEL = 30;
	private static final int IRON_LEVEL = 15;
	private static final int TROUT_LEVEL = 20;

	/**
	 * A kit: the tie, the items, the levels the job needs, and where the character first stands.
	 *
	 * <p>Immutable, and the item and skill arrays are copied on construction, so a shared profile constant
	 * cannot be mutated by a caller that only meant to read it.
	 */
	public static final class Profile {

		private final String name;
		private final Tie tie;
		private final int[] itemIds;
		private final int[] itemAmounts;
		private final int[] skillIds;
		private final int[] skillLevels;
		private final int startX;
		private final int startY;
		private final int plane;
		private final int spellbook;

		private Profile(String name, Tie tie, int startX, int startY, int plane, int spellbook,
				int[] itemIds, int[] itemAmounts) {
			this(name, tie, startX, startY, plane, spellbook, itemIds, itemAmounts,
					new int[0], new int[0]);
		}

		private Profile(String name, Tie tie, int startX, int startY, int plane, int spellbook,
				int[] itemIds, int[] itemAmounts, int[] skillIds, int[] skillLevels) {
			if (itemIds.length != itemAmounts.length) {
				throw new IllegalArgumentException(name + ": " + itemIds.length + " item id(s) but "
						+ itemAmounts.length + " amount(s)");
			}
			if (skillIds.length != skillLevels.length) {
				throw new IllegalArgumentException(name + ": " + skillIds.length + " skill id(s) but "
						+ skillLevels.length + " level(s)");
			}
			this.name = name;
			this.tie = tie;
			this.startX = startX;
			this.startY = startY;
			this.plane = plane;
			this.spellbook = spellbook;
			this.itemIds = itemIds.clone();
			this.itemAmounts = itemAmounts.clone();
			this.skillIds = skillIds.clone();
			this.skillLevels = skillLevels.clone();
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

		/** How many skills this profile sets above the base. */
		public int skillCount() {
			return skillIds.length;
		}

		/** The skill index of the {@code index}th entry, as {@code Config}'s skill constants name it. */
		public int skillId(int index) {
			return skillIds[index];
		}

		/** The level that skill starts at. Never 1: naming a skill here is the claim it needs lifting. */
		public int skillLevel(int index) {
			return skillLevels[index];
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
			return name + " (" + tie + ", " + itemIds.length + " item(s), " + skillIds.length
					+ " skill(s))";
		}
	}

	/**
	 * The kit a row gets when it does not name one.
	 *
	 * <p>Deliberately the generalist rather than the cheapest: a bot with no stated profile is the case
	 * where the operator did not know or care what its script would need, so this carries a tool for each
	 * of the three resources the world supports ({@code LocationKind}: {@code TREE}, {@code ROCK},
	 * {@code FISHING}) <em>and</em> the level that makes each tool useful — otherwise the obvious config
	 * line would fail on the first oak the way a level-1 axe-hand did. That is what
	 * {@code BOT_ACCOUNTS.md} §1 means by "a config line is meant to be sufficient".
	 */
	public static final Profile DEFAULT = new Profile("default", Tie.SKILLER, 3087, 3236, 0, 0,
			new int[] { BRONZE_AXE, BRONZE_PICKAXE, SMALL_FISHING_NET, TINDERBOX },
			new int[] { 1, 1, 1, 1 },
			new int[] { Config.WOODCUTTING, Config.MINING, Config.FISHING },
			new int[] { WILLOW_LEVEL, IRON_LEVEL, TROUT_LEVEL });

	/**
	 * Draynor willows, matching the default tile so an unspecified kit and a woodcutter agree.
	 *
	 * <p>Willows are level 30 in this cache and oaks 15, so the level is the higher of the two shipped
	 * tree places: {@code lumbridge_willows} and the Draynor willow cluster this row starts on are the
	 * profile's own job, and an oak field works for free. Not {@code OAK_LEVEL}, which would leave a
	 * woodcutter standing at its own start tile unable to cut anything.
	 */
	public static final Profile WOODCUTTER = new Profile("woodcutter", Tie.SKILLER, 3087, 3236, 0, 0,
			new int[] { BRONZE_AXE, TINDERBOX }, new int[] { 1, 1 },
			new int[] { Config.WOODCUTTING }, new int[] { WILLOW_LEVEL });

	/** Varrock east mine, where the first ore the pickaxe improves on is iron. */
	public static final Profile MINER = new Profile("miner", Tie.SKILLER, 3285, 3366, 0, 0,
			new int[] { BRONZE_PICKAXE }, new int[] { 1 },
			new int[] { Config.MINING }, new int[] { IRON_LEVEL });

	/** Draynor fishing: a net for the shallows, a rod and feathers for the river. */
	public static final Profile FISHER = new Profile("fisher", Tie.SKILLER, 3086, 3228, 0, 0,
			new int[] { SMALL_FISHING_NET, FLY_FISHING_ROD, FEATHER }, new int[] { 1, 1, 200 },
			new int[] { Config.FISHING }, new int[] { TROUT_LEVEL });

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
