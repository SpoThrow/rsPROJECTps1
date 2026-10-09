package server.game.bots.script;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import server.game.bots.BotState;
import server.game.bots.Traced;
import server.game.bots.meta.BotNodeRegistry;
import server.game.bots.meta.NodeParam;
import server.game.bots.meta.NodeSchema;
import server.game.bots.meta.ParamType;
import server.game.bots.world.LocationKind;
import server.game.bots.world.Tile;

/**
 * Reads a behaviour-graph document into a {@link BotScript} — {@code BOT_TOOLING.md} §7's
 * {@code Data/cfg/bots/*.json}, "one behaviour graph per script".
 *
 * <p><b>This is the half that did not exist.</b> Registered scripts were Java-only
 * ({@link BotScripts}'s static initialiser), so authoring a bot's logic meant writing a class and
 * rebuilding — the limitation the tooling track exists to remove. A document names nodes by their
 * {@code @BotNode} id and supplies parameters by name; this turns that into the same tree a Java block
 * would have built.
 *
 * <p><b>The schema is not restated here.</b> Which nodes exist, what parameters each takes, their types
 * and their defaults all come from {@link BotNodeRegistry}, which reflects them off the real classes. So a
 * new {@code BotState} becomes authorable the moment it is annotated, with no change to this file — the
 * same anti-drift property the palette export has, applied to the loader.
 *
 * <p><b>The document shape.</b> One object per node, with a {@code node} key naming the type and one key
 * per parameter:
 *
 * <pre>
 * {
 *   "name": "gather_willow",                     // optional; must match the file name when present
 *   "root": {
 *     "node": "sequence",
 *     "children": [
 *       { "node": "walkToNearest", "kind": "tree", "range": 3 },
 *       { "node": "gather", "kind": "tree", "itemId": 1519 }
 *     ]
 *   }
 * }
 * </pre>
 *
 * <p><b>Unknown keys are errors, not ignored.</b> A misspelled parameter would otherwise fall back to its
 * default and the script would run with a silently wrong value — the failure mode this whole check exists
 * to prevent. Requiring the document to name only real fields means a typo is caught when the file is
 * loaded, which for a script is at startup, not three hours into a bot walking to the wrong place.
 *
 * <p><b>{@code LOCATION} parameters are refused rather than guessed.</b> {@link ParamType} defines the type
 * and no node declares one yet, so there is no encoding to agree with the editor. Inventing one now would
 * be a second format to drift from the tool's; failing with a clear message means the day a node takes a
 * {@code Location} is the day someone writes its encoding deliberately.
 */
public final class ScriptDocument {

	/** Where script documents live, relative to the server directory. */
	public static final String DIR = "Data/cfg/bots";

	/** The suffix a script document must carry to be picked up. */
	public static final String EXTENSION = ".json";

	/** The node key: which {@code @BotNode} id this object is. */
	private static final String NODE_KEY = "node";

	/** Top-level keys, so a typo at the document level is caught like a typo in a node. */
	private static final String NAME_KEY = "name";
	private static final String ROOT_KEY = "root";
	private static final String[] DOCUMENT_KEYS = { NAME_KEY, ROOT_KEY };

	/** Thrown for anything wrong with a document: unknown node, bad type, missing field, typo. */
	public static final class ScriptException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		ScriptException(String message) {
			super(message);
		}
	}

	private ScriptDocument() {
	}

	/**
	 * Parses JSON text into a script.
	 *
	 * @param name the script's name — normally the file name without its extension, so that
	 *             {@code bots.cfg}'s {@code script} field names the file it came from
	 * @throws Json.JsonException  if the text is not valid JSON
	 * @throws ScriptException     if the document is not a valid script
	 */
	public static BotScript fromJson(String name, String json) {
		Object parsed = Json.parse(json);
		return fromDocument(name, asObject(parsed, "the document"));
	}

	/**
	 * Builds a script from an already-parsed document. Package-visible rather than private only so the
	 * directory loader can hand over a map it has already checked is an object; tests build documents
	 * directly rather than round-tripping through JSON text.
	 *
	 * @throws ScriptException if the document is not a valid script
	 */
	static BotScript fromDocument(String name, Map<String, Object> document) {
		if (name == null || name.isBlank()) {
			throw new ScriptException("a script document needs a name");
		}
		rejectUnknownKeys(document, setOf(DOCUMENT_KEYS), "the document");

		Object declared = document.get(NAME_KEY);
		if (declared != null) {
			String declaredName = asString(declared, "name");
			if (!declaredName.equalsIgnoreCase(name)) {
				// Two names for one script is the ambiguity BotsConfig rejects for accounts: the file name
				// is what bots.cfg's `script` field points at, so a different inner name could never be used.
				throw new ScriptException("declares the name \"" + declaredName + "\" but the file is \""
						+ name + "\"; a script's name is the file's, so remove the \"name\" field or match it");
			}
		}

		Object root = document.get(ROOT_KEY);
		if (root == null) {
			throw new ScriptException("has no \"root\" node");
		}
		// Build once here so a malformed document fails at load, with the precise node and field named.
		// The result is then thrown away on purpose: root() mints a fresh tree per possession (see
		// DocumentScript), because a state holds progress and one tree shared by two bots would have them
		// overwrite each other's walk routes and gather targets.
		buildNode(root, "\"root\"");
		return new DocumentScript(name, document);
	}

	/**
	 * Builds one node from its object: the type named by {@code node}, then each parameter converted to the
	 * Java type the node's constructor declares.
	 */
	private static BotState buildNode(Object value, String context) {
		Map<String, Object> node = asObject(value, context);
		Object idValue = node.get(NODE_KEY);
		if (idValue == null) {
			throw new ScriptException(context + " has no \"node\" field naming its type");
		}
		String id = asString(idValue, context + ".\"" + NODE_KEY + "\"");

		NodeSchema schema = BotNodeRegistry.schemaById(id);
		if (schema == null) {
			throw new ScriptException(context + ": unknown node \"" + id + "\" (known: "
					+ knownIds() + ")");
		}
		Constructor<?> constructor = BotNodeRegistry.constructorFor(id);
		if (constructor == null) {
			// schemaById found it, so this is an inconsistency in the registry rather than bad input.
			throw new ScriptException(context + ": node \"" + id + "\" has no usable constructor");
		}

		Set<String> known = new LinkedHashSet<String>();
		known.add(NODE_KEY);
		List<NodeParam> params = schema.params();
		for (NodeParam param : params) {
			known.add(param.name());
		}
		rejectUnknownKeys(node, known, context + " (node \"" + id + "\")");

		Object[] args = new Object[params.size()];
		for (int i = 0; i < params.size(); i++) {
			NodeParam param = params.get(i);
			String where = context + ".\"" + param.name() + "\"";
			if (node.containsKey(param.name())) {
				args[i] = convert(node.get(param.name()), param, where);
			} else if (param.required()) {
				throw new ScriptException(context + ": node \"" + id + "\" is missing required field \""
						+ param.name() + "\"");
			} else {
				args[i] = fromDefault(param, where);
			}
		}

		try {
			BotState built = (BotState) constructor.newInstance(args);
			// Wrapped for tracing, exactly as ScriptBuilder does, so a document-authored script reports to
			// the bot's trace ring like a hand-authored one and ::botinfo works on both. Idempotent, since a
			// child list is already wrapped by its own buildNode call.
			return traced(built);
		} catch (InvocationTargetException e) {
			// The node's own validation, e.g. a range that cannot be negative. Its message is the useful
			// part, so it is carried through rather than replaced.
			Throwable cause = e.getCause();
			throw new ScriptException(context + ": node \"" + id + "\" rejected its fields: "
					+ (cause == null ? e.toString() : cause.getMessage()));
		} catch (ReflectiveOperationException | IllegalArgumentException e) {
			throw new ScriptException(context + ": could not build node \"" + id + "\": " + e.getMessage());
		}
	}

	/** One parameter value, converted to the Java type its {@link ParamType} describes. */
	private static Object convert(Object value, NodeParam param, String where) {
		switch (param.type()) {
		case INT:
			return asInt(value, where);
		case BOOLEAN:
			return asBoolean(value, where);
		case STRING:
			return asString(value, where);
		case KIND:
			return asKind(value, where);
		case TILE:
			return asTile(value, where);
		case NODE:
			return buildNode(value, where);
		case NODE_LIST:
			return asNodeList(value, where);
		case LOCATION:
			throw new ScriptException(where + " is a LOCATION parameter, and no node declares one yet, so "
					+ "the document format does not define how to write one");
		default:
			throw new ScriptException(where + ": unhandled parameter type " + param.type());
		}
	}

	/**
	 * An omitted optional parameter, from the default the registry read off the annotation. Defaults are
	 * strings on the annotation, so this is the one place they become real values — and it must agree with
	 * {@link #convert}, which is why both switch on the same {@link ParamType}.
	 */
	private static Object fromDefault(NodeParam param, String where) {
		String value = param.defaultValue();
		switch (param.type()) {
		case STRING:
			return value;
		case INT:
			try {
				return Integer.valueOf(value.trim());
			} catch (NumberFormatException e) {
				throw new ScriptException(where + ": node declares a non-numeric default \"" + value + "\"");
			}
		case BOOLEAN:
			return Boolean.valueOf(value.trim());
		case KIND:
			LocationKind kind = LocationKind.byId(value);
			if (kind == null) {
				throw new ScriptException(where + ": node declares an unknown kind default \"" + value + "\"");
			}
			return kind;
		default:
			// A child node or a tile cannot be defaulted from a string, and the registry can only mark a
			// parameter optional if it has one. Reaching here means the annotation is inconsistent.
			throw new ScriptException(where + ": a " + param.type() + " parameter cannot have a default");
		}
	}

	private static BotState[] asNodeList(Object value, String where) {
		List<Object> list = asList(value, where);
		BotState[] children = new BotState[list.size()];
		for (int i = 0; i < children.length; i++) {
			children[i] = buildNode(list.get(i), where + "[" + i + "]");
		}
		return children;
	}

	private static Tile asTile(Object value, String where) {
		Map<String, Object> tile = asObject(value, where);
		rejectUnknownKeys(tile, setOf(new String[] { "x", "y", "plane" }), where);
		int x = asInt(required(tile, "x", where), where + ".\"x\"");
		int y = asInt(required(tile, "y", where), where + ".\"y\"");
		int plane = tile.containsKey("plane") ? asInt(tile.get("plane"), where + ".\"plane\"") : 0;
		return Tile.of(x, y, plane);
	}

	private static LocationKind asKind(Object value, String where) {
		String id = asString(value, where);
		LocationKind kind = LocationKind.byId(id);
		if (kind == null) {
			throw new ScriptException(where + ": \"" + id + "\" is not a kind of place or resource");
		}
		return kind;
	}

	private static int asInt(Object value, String where) {
		if (!(value instanceof Number)) {
			throw new ScriptException(where + " must be a whole number, got " + describe(value));
		}
		double number = ((Number) value).doubleValue();
		if (Double.isInfinite(number) || Double.isNaN(number) || number != Math.rint(number)) {
			throw new ScriptException(where + " must be a whole number, got " + number);
		}
		if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
			throw new ScriptException(where + " is out of range for a whole number: " + number);
		}
		return (int) number;
	}

	private static boolean asBoolean(Object value, String where) {
		if (!(value instanceof Boolean)) {
			throw new ScriptException(where + " must be true or false, got " + describe(value));
		}
		return (Boolean) value;
	}

	private static String asString(Object value, String where) {
		if (!(value instanceof String)) {
			throw new ScriptException(where + " must be text, got " + describe(value));
		}
		return (String) value;
	}

	private static Map<String, Object> asObject(Object value, String where) {
		if (!(value instanceof Map)) {
			throw new ScriptException(where + " must be an object, got " + describe(value));
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) value;
		return map;
	}

	private static List<Object> asList(Object value, String where) {
		if (!(value instanceof List)) {
			throw new ScriptException(where + " must be a list of nodes, got " + describe(value));
		}
		@SuppressWarnings("unchecked")
		List<Object> list = (List<Object>) value;
		return list;
	}

	private static Object required(Map<String, Object> map, String key, String where) {
		if (!map.containsKey(key)) {
			throw new ScriptException(where + " is missing the \"" + key + "\" field");
		}
		return map.get(key);
	}

	private static void rejectUnknownKeys(Map<String, Object> map, Set<String> allowed, String where) {
		for (String key : map.keySet()) {
			if (!allowed.contains(key)) {
				throw new ScriptException(where + " has an unknown field \"" + key + "\" (expected "
						+ String.join(", ", allowed) + ")");
			}
		}
	}

	/** Wraps a built node for tracing. Same rule as {@code ScriptBuilder.traced}: never double-wrap. */
	private static BotState traced(BotState node) {
		return node == null || node instanceof Traced ? node : new Traced(node);
	}

	private static Set<String> setOf(String[] keys) {
		Set<String> set = new LinkedHashSet<String>();
		for (String key : keys) {
			set.add(key);
		}
		return set;
	}

	private static String knownIds() {
		List<NodeSchema> schemas = BotNodeRegistry.schemas();
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < schemas.size(); i++) {
			if (i > 0) {
				out.append(", ");
			}
			out.append(schemas.get(i).id());
		}
		return out.toString();
	}

	/** A short description of a wrongly-typed value, for the error message. */
	private static String describe(Object value) {
		if (value == null) {
			return "null";
		}
		if (value instanceof Map) {
			return "an object";
		}
		if (value instanceof List) {
			return "a list";
		}
		if (value instanceof String) {
			return "the text \"" + value + "\"";
		}
		return String.valueOf(value);
	}

	/** The name a file with this name would carry: {@code gather_willow.json} to {@code gather_willow}. */
	public static String nameOfFile(String fileName) {
		String lower = fileName.toLowerCase(Locale.ROOT);
		return lower.endsWith(EXTENSION) ? fileName.substring(0, fileName.length() - EXTENSION.length())
				: fileName;
	}

	/**
	 * The script a document describes: a name plus the document, minting a fresh tree on every
	 * {@link #root()}.
	 *
	 * <p>Modelled on {@code ScriptBuilder.Built}, and for the same reason — a state holds progress, so
	 * {@code root()} must not hand the same instances to two bots. Here the recipe is the parsed document
	 * rather than a list of suppliers, but the property is identical and load-order independent.
	 */
	private static final class DocumentScript implements BotScript {

		private final String name;
		private final Map<String, Object> document;

		DocumentScript(String name, Map<String, Object> document) {
			this.name = name;
			this.document = document;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public BotState root() {
			// Rebuilt, not cached: two bots naming this script each get their own nodes. The document was
			// validated when it was loaded, so a failure here would be a bug in this class rather than bad
			// input, and it is deliberately not caught.
			return buildNode(document.get(ROOT_KEY), "\"root\"");
		}

		@Override
		public String toString() {
			return "BotScript(" + name + ", from a document)";
		}
	}
}
