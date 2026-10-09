package server.game.bots.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link server.game.bots.BotState} that the runtime applies itself and that is deliberately
 * <b>not</b> an authorable node.
 *
 * <p><b>Why this exists.</b> Phase F's tracing is a decorator: {@code Traced} is a {@code BotState} so it
 * can sit in a tree, but no author places one and the editor palette must never offer one. Without a way
 * to say that, the tooling's parity check — "the set of compiled {@code BotState}s and the set of palette
 * nodes are the same set" ({@code BOT_TOOLING.md} T4) — would either fail on every runtime wrapper or
 * have to be loosened for everything, which is how a real unannotated node slips through.
 *
 * <p>So the check stays absolute and the exception is declared on the class itself, next to the reason.
 * The tooling then enforces both directions: everything not marked here must be annotated, and anything
 * marked here must not be registered.
 *
 * <p>Being explicit is the point. Adding a second wrapper means adding a second {@code @RuntimeOnly} —
 * a one-line, reviewable decision — rather than quietly widening a filter in a scanner.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RuntimeOnly {

	/** Why this state is not authorable. Read by a human, so keep it to one sentence. */
	String value();
}
