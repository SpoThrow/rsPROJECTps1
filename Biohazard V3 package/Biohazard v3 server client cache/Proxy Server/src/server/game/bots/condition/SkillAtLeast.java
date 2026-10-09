package server.game.bots.condition;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.meta.BotNode;
import server.game.bots.meta.Param;

/**
 * True when the bot's level in a skill is at least a threshold — the gate that stops a bot
 * being scripted into a tree it cannot run ({@code BOT_ROADMAP.md} §5.3).
 *
 * <p><b>Current level, not base level.</b> {@code playerLevel} is the level that moves with
 * drains and boosts, and it is what this codebase's own requirement checks compare against
 * ({@code c.skills.playerLevel[6] >= 64} for a spell); the unboosted level is derived from
 * {@code playerXP}. Gating on the current level therefore agrees with the code that will
 * actually refuse the action, which is the point of the condition — it predicts the skill,
 * it does not second-guess it.
 *
 * <p><b>An out-of-range skill index fails rather than throws.</b> The skill table has a fixed
 * number of slots and this is authored data, so a wrong index is a tree bug; but {@code
 * BotPlayer.process()} ticks the tree with no guard, so letting an {@code
 * ArrayIndexOutOfBoundsException} escape would take down the game tick for every player, not
 * just the bot. An unsatisfiable question is FAILURE.
 *
 * <p>This reads the possessed client directly, like the work states do for their session
 * flag. The honest long-term home is an observation on {@link BotContext}; it moves there
 * when the context stops being player-shaped ({@code BOT_ROADMAP.md} §5.1).
 */
@BotNode(id = "skill_at_least", category = "condition",
		summary = "Succeeds when the bot's level in a skill is at least a threshold.")
public final class SkillAtLeast implements BotState {

	private final int skill;
	private final int level;

	/**
	 * @param skill index into the skill table, using {@code Player}'s skill constants
	 *              ({@code playerAttack} = 0 … {@code playerRunecrafting} = 20)
	 * @param level the level the skill must be at
	 */
	public SkillAtLeast(
			@Param(description = "Skill index to check (Player's skill constants).") int skill,
			@Param(description = "The level the skill must be at.") int level) {
		this.skill = skill;
		this.level = level;
	}

	@Override
	public void enter(BotContext ctx) {
		// Stateless.
	}

	@Override
	public BotStatus tick(BotContext ctx) {
		return ctx.skillLevel(skill) >= level ? BotStatus.SUCCESS : BotStatus.FAILURE;
	}

	@Override
	public void exit(BotContext ctx, boolean interrupted) {
		// Stateless.
	}

	@Override
	public String name() {
		return "SkillAtLeast(" + skill + "," + level + ")";
	}
}
