package server.game.players;

/**
 * A player's skill levels and experience.
 *
 * <p>Four fields that used to be declared straight on {@link Player}, reached as
 * {@code player.skills}. The two arrays are the skill table itself, indexed by the
 * skill-index constants on {@link Player} ({@code playerAttack} = 0 …
 * {@code playerRunecrafting} = 20); the two ints are the derived totals the hiscores and
 * the quest tab display.
 *
 * <p><b>The skill-index constants deliberately stay on {@link Player}.</b> They are
 * {@code static} and so cannot live on a per-player collaborator, and they are the index
 * space both of these arrays are read with — {@code skills.playerXP[Player.playerMagic]}.
 * They are also not {@code final}, which is a separate pre-existing smudge; making them
 * {@code final} is safe but is not part of this move.
 *
 * <p><b>Index 3 is hitpoints, and that is the only place a level is not symmetric.</b>
 * {@link Player}'s constructor fills the table with {@code 1} in every slot except
 * {@code playerLevel[3]} (current hitpoints), which starts at {@code 10}, and
 * {@code playerXP[3]}, which starts at {@code 1300} — the XP for level 10. So the real
 * defaults are applied by that constructor, not by this class's initialisers: a bare
 * {@code new Skills()} is all zeros and is <em>not</em> a valid player state. The
 * constructor is left as the single place that seeds a player, so that this move stays
 * mechanical.
 *
 * <p>{@code playerLevel} is the <em>current</em> level (it moves with drains and boosts),
 * while the unboosted level is derived from {@code playerXP} via
 * {@code getLevelForXP(...)}; the two are not the same number and the code depends on the
 * difference.
 *
 * <p>Persistence is a table, not a single key: {@code PlayerSave} writes the whole thing as
 * an {@code [SKILLS]} section of {@code character-skill = <index> <level> <xp>} lines, and
 * reads it back the same way. The section and key names are string literals, so the path
 * change cannot move them.
 *
 * <p>This is a data bag on purpose. Extracting the cluster and encapsulating it are
 * separate steps, so that a behaviour change cannot hide inside the mechanical move.
 */
public final class Skills {

	/**
	 * Current level per skill, indexed by the skill constants on {@link Player}. Slot 3 is
	 * current hitpoints. Starts at {@code 1} (slot 3: {@code 10}) for a new player; a bare
	 * {@code new Skills()} leaves it zeroed.
	 */
	public int[] playerLevel = new int[25];

	/**
	 * Experience per skill, indexed by the skill constants on {@link Player}. Slot 3 is
	 * hitpoints. A bare {@code new Skills()} leaves it zeroed; {@link Player}'s constructor
	 * sets slot 3 to {@code 1300}.
	 */
	public int[] playerXP = new int[25];

	/**
	 * Cached sum of the unboosted levels, assigned from
	 * {@code PlayerAssistant.totalLevel()} during {@code Client.initialize()}.
	 *
	 * <p>⚠️ Dead: written once and never read. The display code calls
	 * {@code getPA().totalLevel()} instead. Brought along so the skills concept lives in
	 * one place; deletable, like {@code BountyHunterState.bountyIcon}.
	 */
	public int totalLevel;

	/**
	 * Cached sum of all experience, assigned from {@code PlayerAssistant.xpTotal()} during
	 * {@code Client.initialize()}.
	 *
	 * <p>⚠️ Dead: written once and never read, exactly as {@code totalLevel} above.
	 */
	public int xpTotal;
}
