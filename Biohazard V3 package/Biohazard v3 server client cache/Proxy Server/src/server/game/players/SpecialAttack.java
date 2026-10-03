package server.game.players;

/**
 * The player's special-attack state, reached as {@code c.specialAttack}.
 *
 * <p>Extracted in §4.13 as the second half of the combat pass (the first, §4.12, took the
 * prayer/curse state). This is the spec bar's own state and nothing else.
 *
 * <p><b>⚠️ Two of the defaults are non-zero <em>multipliers</em>, and they are load-bearing.</b>
 * {@link #specAccuracy} and {@link #specDamage} start at {@code 1} because the combat maths
 * multiplies by them — {@code attackLevel *= c.specialAttack.specAccuracy} and
 * {@code maxHit = (int)(maxHit * c.specialAttack.specDamage)}. A default of {@code 0} would zero
 * out accuracy and every hit; {@code 1} means "no special attack in progress". The spec weapons
 * set them above {@code 1} for the duration of a special and {@code CombatAssistant} resets them
 * back to {@code 1.0} afterwards. This is the opposite shape from §4.11's {@code freezeTimer},
 * where the non-zero default was a sentinel — here it is an identity value.
 *
 * <p>{@link #specAmount} is the charge, {@code 0}–{@code 100}, spent in {@code 25}/{@code 30}/
 * {@code 50}/{@code 55}/{@code 100} chunks by {@code CombatAssistant} and restored 10 at a time.
 * <b>It is persisted</b>: {@code PlayerSave} writes and reads it under the {@code special-amount}
 * key, which did not move — only the field reference changed.
 *
 * <p>{@link #specBarId} is the client frame id of the weapon's own special bar; it is per-player
 * because it is cached when the weapon is equipped, and it is <em>not</em> a constant despite
 * living in a constant-looking comma-list on {@link Player}.
 *
 * <p>{@link #specEffect} is a small enum-ish int ({@code 0} = none) that {@code CombatAssistant}
 * switches on to pick the special's effect. {@link #specMaxHitIncrease} is a flat bonus added to
 * the max hit for the specials that use it.
 *
 * <p>This is a data bag on purpose. Extracting the cluster and encapsulating it are separate
 * steps, so that a behaviour change cannot hide inside the mechanical move.
 */
public final class SpecialAttack {

	/** ⚠️ Charge 0–100, spent by special attacks and restored 10 at a time. Persisted under {@code special-amount}. */
	public double specAmount = 0;

	/** ⚠️ Non-zero <em>multiplier</em>: {@code 1} = no special. The combat maths multiplies by this, so {@code 0} would disable all accuracy. */
	public double specAccuracy = 1;

	/** ⚠️ Non-zero <em>multiplier</em>: {@code 1} = no special. Multiplied into the max hit, so {@code 0} would mean every hit lands for 0. */
	public double specDamage = 1;

	/** Client frame id of the equipped weapon's special bar; cached per player, not a constant. */
	public int specBarId;

	/** Which special effect is in flight; {@code 0} = none. Switched on in {@code CombatAssistant}. */
	public int specEffect;

	/** Flat addition to the max hit for the specials that use one. */
	public int specMaxHitIncrease;

	/** The special-attack toggle. */
	public boolean usingSpecial;

	/** Set by the double-hit specials (e.g. dragon dagger) so the second hit is dealt. */
	public boolean doubleHit;
}
