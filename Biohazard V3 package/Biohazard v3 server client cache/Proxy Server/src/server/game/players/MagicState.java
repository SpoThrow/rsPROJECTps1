package server.game.players;

/**
 * The player's magic casting configuration, reached as {@code c.magic}.
 *
 * <p>Extracted in §4.15 as the "magic state" half of the attack-mode/magic bucket, paired with
 * {@link AttackMode} in §4.14. ⚠️ <b>The class is named {@code MagicState}, not {@code Magic},
 * on purpose:</b> {@code server.content.skills.Magic} already exists, and {@code PlayerAssistant}
 * — which lives in <em>this</em> package — imports it and calls {@code Magic.enchantBolt(...)}.
 * A same-package class named {@code Magic} would shadow that import for every class in
 * {@code server.game.players}, so the collaborator would have hijacked the skills helper. The
 * <em>field</em> is still {@code magic}, so call sites read {@code c.magic.spellId}.
 *
 * <p>⚠️ <b>{@link #autocastId}'s declared default disagrees with the {@code -1} sentinel the code
 * uses for "no autocast".</b> It is declared with no initialiser in a comma-list, so it starts at
 * {@code 0}; but {@code PlayerAssistant.resetAutocast} sets it to {@code -1} and the guard tests
 * are written as {@code c.autocastId < 0} and {@code c.autocastId >= 0}. A fresh player therefore
 * reads as having autocast spell {@code 0} selected rather than none — the same shape as
 * {@code respawnTimer}'s default/sentinel mismatch in §4.11. Left as-is: this pass moves state, it
 * does not change it.
 *
 * <p><b>The three memory arrays are one persisted row, not three independent tables.</b>
 * {@code PlayerSave} writes and reads them interleaved as a {@code weapon, spell, book} triple per
 * slot ({@code token3[j * 3]}, {@code [j * 3 + 1]}, {@code [j * 3 + 2]}, and the matching
 * {@code append} chain). They are 12 slots — one per autocast-capable weapon — and the autocast
 * selection in {@link #autocastId} is what gets applied into a slot.
 *
 * <p>This is a data bag on purpose. Extracting the cluster and encapsulating it are separate
 * steps, so that a behaviour change cannot hide inside the mechanical move.
 */
public final class MagicState {

	/** Which spellbook is open: {@code 0} normal, {@code 1} ancient, {@code 2} lunar. */
	public int playerMagicBook;

	/** ⚠️ The selected autocast spell; {@code -1} means none in the code, but the declared default is {@code 0} — see the class note. */
	public int autocastId;

	/** Autocast memory, slot = weapon id. Persisted as the first third of each {@code weapon, spell, book} row. */
	public int[] autocastMemWeapon = new int[12];

	/** Autocast memory, slot = weapon id. Persisted as the second third of each row. */
	public int[] autocastMemSpell = new int[12];

	/** Autocast memory, slot = weapon id. Persisted as the third third of each row. */
	public int[] autocastMemBook = new int[12];

	/** The spell currently selected (0 = none). Compared against {@code MAGIC_SPELLS[i][0]}. */
	public int spellId;

	/** The previously selected spell, used to clear the old spell's state when the selection changes. */
	public int oldSpellId;
}
