package server.game.players;

/**
 * This player's attack style, reached as {@code c.combatStyle}.
 *
 * <p>Extracted in §4.21. The value selects how melee XP is distributed — the accurate / aggressive /
 * defensive / controlled choice made on the combat tab.
 *
 * <p>⚠️ <b>{@link #fightMode} does two jobs at once, and that is why its numbering is load-bearing.</b>
 * It is not only a style selector: it is passed <b>straight through as a skill index</b> —
 * {@code addSkillXP(damage, c.fightMode)} and {@code refreshSkill(c.fightMode)} — so the enum values
 * are silently coupled to the skill-array layout. Reordering the styles to "tidy them up" would
 * award XP to the wrong skill and refresh the wrong tab, with no compile error.
 *
 * <p>⚠️ It is also <b>persisted</b>, under the save key <b>{@code fightMode}</b>
 * ({@code PlayerSave} load 318 / save 672). The key did not move; only the field reference did.
 *
 * <p>And the value is <b>not</b> the interface config id: {@code PlayerAssistant.handleWeaponStyle}
 * maps it non-identically ({@code 0→0}, {@code 1→3}, {@code 2→1}, {@code 3→2}) before sending it to
 * the client, so the two numberings must not be confused.
 *
 * <p>It is deliberately its own bag rather than part of {@link AttackMode}: that class holds the
 * "what am I attacking with" flags, this is the "how it distributes XP" value, and §4.14 chose the
 * name {@code AttackMode} precisely to stay out of {@code fightMode}'s way. Default {@code 0}, the
 * accurate style.
 */
public final class CombatStyle {

	/** Attack style: {@code 0} accurate, {@code 1} aggressive, {@code 2} defensive, {@code 3} controlled. Doubles as the melee XP skill index — see the class doc. */
	public int fightMode;
}
