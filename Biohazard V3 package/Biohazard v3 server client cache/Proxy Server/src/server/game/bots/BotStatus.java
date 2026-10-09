package server.game.bots;

/**
 * The outcome of one tick of a state.
 *
 * <p>A state never names its successor: it reports an outcome and the owning composite
 * decides what runs next ({@code BOT_PLAN.md} §3.1). That is what keeps transitions in one
 * place instead of scattered across every leaf.
 */
public enum BotStatus {

	/** Still working; tick this state again next game tick. */
	RUNNING,

	/** This state's job is done. The owning composite decides what runs next. */
	SUCCESS,

	/** This state cannot finish. The owning composite decides retry or abort. */
	FAILURE
}
