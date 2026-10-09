package server.game.bots.meta;

/**
 * One reflected constructor parameter of a node, as the editor sees it.
 *
 * <p>Built by {@link BotNodeRegistry} from a real {@code java.lang.reflect.Parameter} plus its
 * {@link Param}, so every field here was read from the code rather than transcribed. {@code
 * defaultValue} is null exactly when the parameter is required.
 */
public record NodeParam(String name, ParamType type, boolean required, String defaultValue,
		String description) {
}
