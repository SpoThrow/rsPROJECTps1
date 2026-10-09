package server.game.bots.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes one constructor parameter of a {@link BotNode}.
 *
 * <p><b>The name and the type are not repeated here.</b> They are read off the constructor by
 * reflection: the name from {@code Parameter.getName()} (which is why the build passes
 * {@code -parameters}) and the type from {@link ParamType}. Repeating them in the annotation would
 * be a second copy that could disagree with the code, so the annotation carries only what
 * reflection cannot know: what the parameter means, and whether a shorter constructor supplies a
 * default for it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Param {

	/** What the parameter means, shown next to the field in the editor. */
	String description();

	/**
	 * Whether a caller may omit this parameter. False means a convenience constructor exists that
	 * supplies it; the value it supplies must be given in {@link #value()}.
	 */
	boolean required() default true;

	/**
	 * The value a shorter convenience constructor supplies for an optional parameter, as the
	 * editor's text field would show it. Empty when {@link #required()} is true.
	 */
	String value() default "";
}
