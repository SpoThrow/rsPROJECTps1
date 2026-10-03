package server.game.players;

/**
 * How the player is currently attacking, reached as {@code c.attackMode}.
 *
 * <p>Extracted in §4.14 as the "mode flags" half of the attack-mode/magic bucket (the magic
 * configuration it is driven by went out as {@code MagicState}, reached as {@code c.magic}, in
 * §4.15). Five booleans, all
 * defaulting to {@code false}, and deliberately <em>not</em> named {@code CombatStyle}:
 * {@code fightMode} on {@link Player} is the accurate/aggressive/defensive attack <em>style</em>,
 * which is a different concept, and reusing that word here would have been actively misleading.
 *
 * <p>⚠️ <b>These flags are not merely descriptive — the combat code branches on them, and several
 * are set from the equipped weapon on every attack.</b> {@code CombatAssistant} reads them to pick
 * between the melee, ranged and magic paths, and to decide whether an attack is in range. They are
 * also read together, e.g. {@code (c.attackMode.usingBow || c.attackMode.usingMagic)}, so a flag
 * left in the wrong state silently sends an attack down the wrong path rather than failing.
 *
 * <p>{@link #autocasting} lives here rather than on {@code MagicState} because it behaves as a mode:
 * {@code CombatAssistant} branches on it the same way it branches on the weapon flags, and
 * {@code CastleWars} tests it alongside them. The spell it autocasts is on {@code c.magic}.
 *
 * <p>This is a data bag on purpose. Extracting the cluster and encapsulating it are separate
 * steps, so that a behaviour change cannot hide inside the mechanical move.
 */
public final class AttackMode {

	/** Attacking with throwing weapons / other non-bow ranged gear. */
	public boolean usingRangeWeapon;

	/** Attacking with a bow or crossbow. */
	public boolean usingBow;

	/** Attacking with a spell. */
	public boolean usingMagic;

	/** Currently in the act of casting. */
	public boolean castingMagic;

	/** Autocast is enabled for the current spellbook. The spell itself is {@code c.magic}. */
	public boolean autocasting;
}
