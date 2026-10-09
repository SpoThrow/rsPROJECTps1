package server.game.bots.meta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import server.game.bots.BotState;
import server.game.bots.composite.Repeat;
import server.game.bots.composite.Sequence;
import server.game.bots.states.BankLogs;
import server.game.bots.states.ChopTree;
import server.game.bots.states.WalkTo;
import server.game.bots.world.Location;
import server.game.bots.world.Tile;

/**
 * Turns {@link BotNode}-annotated {@link BotState} classes into {@link NodeSchema}s
 * ({@code BOT_TOOLING.md} stage T4).
 *
 * <p><b>The schema is reflected, not written down twice.</b> The parameter names come from the
 * constructor (the build passes {@code -parameters} so they are real names, and this class refuses
 * to export {@code arg0}), the types come from {@link ParamType}, and only the human description is
 * declared in code. Adding a node therefore means annotating its class; it cannot mean annotating
 * its class <em>and</em> editing a list in the tool, which is the drift this exists to prevent.
 *
 * <p><b>One canonical constructor per node.</b> A node may offer convenience constructors — {@code
 * WalkTo} takes three arguments as well as four — but exactly one constructor may carry {@code
 * @Param} on every parameter, and that one is the schema. Two fully-annotated constructors are
 * rejected rather than guessed between, because the editor would otherwise show a coin flip.
 *
 * <p>The registry is a build-time and test-time facility: the server never calls it, so a bot
 * running a tree is unaffected by it and the annotations cost only their own bytes.
 */
public final class BotNodeRegistry {

	/**
	 * The node classes that ship with the server, in a deliberate order (leaves, then composites).
	 *
	 * <p>Not a classpath scan: scanning is fragile under a repackaged jar, and an explicit list
	 * means "this is the set" is a statement the code makes rather than a side effect of the build
	 * layout. Completeness is not left to good faith — the parity test walks the compiled tree for
	 * every concrete {@code BotState} and fails if one is missing from here, so the list cannot go
	 * stale silently.
	 */
	private static final List<Class<? extends BotState>> NODES = List.of(
			WalkTo.class,
			ChopTree.class,
			BankLogs.class,
			Sequence.class,
			Repeat.class);

	private BotNodeRegistry() {
	}

	/** Every node class the server ships, in declaration order. */
	public static List<Class<? extends BotState>> knownNodeClasses() {
		return NODES;
	}

	/** Whether {@code type} declares itself an authorable node. */
	public static boolean isNode(Class<?> type) {
		return type.isAnnotationPresent(BotNode.class);
	}

	/** The schemas of every known node, sorted by id so the export is a stable diff. */
	public static List<NodeSchema> schemas() {
		List<NodeSchema> schemas = new ArrayList<NodeSchema>(NODES.size());
		for (Class<? extends BotState> type : NODES) {
			schemas.add(schemaOf(type));
		}
		schemas.sort(Comparator.comparing(NodeSchema::id));
		return schemas;
	}

	/** The schema with this id, or null. */
	public static NodeSchema schemaById(String id) {
		for (NodeSchema schema : schemas()) {
			if (schema.id().equals(id)) {
				return schema;
			}
		}
		return null;
	}

	/**
	 * Reflects one node class. Throws with the offending class and member named rather than
	 * returning a half-built schema: an export that silently drops a node is worse than one that
	 * fails, because the editor would show a palette with a hole in it.
	 */
	public static NodeSchema schemaOf(Class<? extends BotState> type) {
		BotNode declared = type.getAnnotation(BotNode.class);
		if (declared == null) {
			throw new IllegalArgumentException(
					type.getName() + " is in the node registry but has no @BotNode");
		}
		if (declared.id().isBlank()) {
			throw new IllegalArgumentException(type.getName() + " declares a blank @BotNode id");
		}

		Constructor<?> canonical = canonicalConstructor(type);
		List<NodeParam> params = new ArrayList<NodeParam>();
		if (canonical != null) {
			for (Parameter parameter : canonical.getParameters()) {
				params.add(paramOf(type, parameter));
			}
		}
		return new NodeSchema(declared.id(), type.getName(), declared.category(),
				declared.summary(), params);
	}

	/**
	 * The one constructor whose parameters are all annotated, or the no-arg constructor when the
	 * node is a constant. Returns null when the node is a no-arg constant.
	 */
	private static Constructor<?> canonicalConstructor(Class<? extends BotState> type) {
		Constructor<?> canonical = null;
		for (Constructor<?> constructor : type.getDeclaredConstructors()) {
			if (constructor.getParameterCount() == 0 || !allAnnotated(constructor)) {
				continue;
			}
			if (canonical != null) {
				throw new IllegalArgumentException(type.getName() + " has two constructors fully "
						+ "annotated with @Param; the editor cannot choose between them");
			}
			canonical = constructor;
		}
		if (canonical == null && type.getDeclaredConstructors().length > 0) {
			boolean hasNoArg = false;
			for (Constructor<?> constructor : type.getDeclaredConstructors()) {
				hasNoArg |= constructor.getParameterCount() == 0;
			}
			if (!hasNoArg) {
				throw new IllegalArgumentException(type.getName() + " has no constructor with @Param "
						+ "on every parameter, so it has no authorable schema");
			}
		}
		return canonical;
	}

	private static boolean allAnnotated(Constructor<?> constructor) {
		for (Parameter parameter : constructor.getParameters()) {
			if (!parameter.isAnnotationPresent(Param.class)) {
				return false;
			}
		}
		return true;
	}

	private static NodeParam paramOf(Class<?> type, Parameter parameter) {
		Param declared = parameter.getAnnotation(Param.class);
		if (!parameter.isNamePresent()) {
			// Without -parameters the name is arg0, which would ship a schema the editor cannot
			// label. Failing loudly here is the only way the mistake is visible.
			throw new IllegalStateException(type.getName() + " was compiled without -parameters, so "
					+ "constructor parameter names are not available for the node schema");
		}
		ParamType paramType = typeOf(type, parameter);
		String value = declared.value();
		boolean required = declared.required();
		if (required && !value.isEmpty()) {
			throw new IllegalArgumentException(type.getName() + "." + parameter.getName()
					+ " is required but declares a default value \"" + value + "\"");
		}
		if (!required && value.isEmpty()) {
			throw new IllegalArgumentException(type.getName() + "." + parameter.getName()
					+ " is optional but declares no default value");
		}
		return new NodeParam(parameter.getName(), paramType, required, required ? null : value,
				declared.description());
	}

	/** The one place a Java type becomes a schema type; an unmapped type is an error, not a guess. */
	public static ParamType typeOf(Class<?> owner, Parameter parameter) {
		Class<?> javaType = parameter.getType();
		if (javaType == int.class || javaType == Integer.class) {
			return ParamType.INT;
		}
		if (javaType == boolean.class || javaType == Boolean.class) {
			return ParamType.BOOLEAN;
		}
		if (javaType == String.class) {
			return ParamType.STRING;
		}
		if (javaType == Tile.class) {
			return ParamType.TILE;
		}
		if (javaType == Location.class) {
			return ParamType.LOCATION;
		}
		if (javaType == BotState.class) {
			return ParamType.NODE;
		}
		if (javaType.isArray() && javaType.getComponentType() == BotState.class) {
			return ParamType.NODE_LIST;
		}
		throw new IllegalArgumentException(owner.getName() + "." + parameter.getName() + " has type "
				+ javaType.getName() + ", which maps to no ParamType");
	}

	/** Convenience for tests and the exporter: id to schema, in {@link #schemas()} order. */
	public static Map<String, NodeSchema> byId() {
		Map<String, NodeSchema> map = new LinkedHashMap<String, NodeSchema>();
		for (NodeSchema schema : schemas()) {
			map.put(schema.id(), schema);
		}
		return map;
	}
}
