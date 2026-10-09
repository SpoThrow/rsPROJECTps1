package botworkshop.export;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import server.game.bots.meta.BotNodeRegistry;
import server.game.bots.meta.NodeParam;
import server.game.bots.meta.NodeSchema;
import server.game.bots.script.BotScripts;
import server.game.bots.script.ScriptDocument;

/**
 * Reads, validates and writes {@code Data/cfg/bots/*.json} — the behaviour graphs the timeline editor
 * authors, and the tool's half of {@code BOT_TOOLING.md} §6's "Save" row.
 *
 * <p><b>The tool does not decide what a valid script is.</b> {@link ScriptDocument} — the server's own
 * loader — is the only validator here: a document is checked by being handed to it, so the file this
 * writes is exactly the file the server reads back at boot. That is the same arrangement the location
 * authoring has with {@code LocationsConfig}, and it is the one that keeps a tool from inventing a
 * dialect the runtime then rejects.
 *
 * <p><b>The tool does decide the bytes.</b> After the loader accepts a document it is re-emitted here in
 * a canonical form — {@code node} first, then each parameter in the node's declared order, integers as
 * integers, no field left at its default unless the document stated it. Two authors who build the same
 * graph get the same file, and re-saving an unchanged graph produces a byte-identical file, which is what
 * makes the output git-diffable ({@code BOT_TOOLING.md} §6). Re-emission is lossless because the loader
 * has already refused any field name the node does not declare.
 */
public final class ScriptDocs {

	/** Where documents live and what they are called; the server's constants, not copies. */
	public static final String DIR = ScriptDocument.DIR;
	public static final String EXTENSION = ScriptDocument.EXTENSION;

	/**
	 * A script name is a file name, so it is bounded to what is safe and portable on every filesystem the
	 * repository might sit on. Lowercase on purpose: Windows and macOS are case-insensitive, where
	 * {@code Foo.json} and {@code foo.json} are one file, so allowing both would let two names share one
	 * script depending on the machine.
	 */
	private static final Pattern NAME = Pattern.compile("[a-z0-9_]{1,48}");

	/** The tool's own errors — a bad name, a non-object document — as opposed to the loader's. */
	public static final class DocException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		DocException(String message) {
			super(message);
		}
	}

	private ScriptDocs() {
	}

	/**
	 * Validates a document through the server's loader and returns it in canonical form.
	 *
	 * @param name the script's name, which must equal the document's {@code name} field when it has one
	 * @throws server.game.bots.script.Json.JsonException if the text is not valid JSON
	 * @throws ScriptDocument.ScriptException             if it is not a valid script
	 * @throws DocException                               if it is not a JSON object at all
	 */
	public static String canonicalize(String name, String json) {
		Object parsed = server.game.bots.script.Json.parse(json);
		Map<String, Object> document = asObject(parsed, "a script document");
		// The authoritative check, before anything is reformatted.
		ScriptDocument.fromDocument(name, document);
		return emit(document);
	}

	/**
	 * Writes a document to {@code <dir>/<name>.json}, or writes nothing.
	 *
	 * <p>Validation runs before the file is touched, so a rejected document leaves the previous file — if
	 * any — exactly as it was. A tool that half-wrote a script the server cannot load would be worse than
	 * one that refused it.
	 *
	 * @return the canonical text that was written
	 */
	public static String save(Path dir, String name, String json) throws IOException {
		requireSafeName(name);
		if (BotScripts.isBuiltIn(name)) {
			// The loader refuses a file that shadows a built-in, so writing one would create a file that
			// silently does nothing. Better to refuse here, where the author can be told why.
			throw new DocException("\"" + name + "\" is a built-in script; pick another name");
		}
		String canonical = canonicalize(name, json);
		Files.createDirectories(dir);
		Files.write(file(dir, name), (canonical + "\n").getBytes(StandardCharsets.UTF_8));
		return canonical;
	}

	/**
	 * The scripts in {@code dir}, each with whether the server's loader accepts it, as JSON for the editor.
	 *
	 * <p>A malformed file is reported with its reason rather than skipped: the editor lists what is on disk
	 * so an author can reopen it, and hiding a file the loader would reject is how someone comes to edit a
	 * script that has never run.
	 */
	public static String list(Path dir) {
		Json out = new Json();
		out.openObject();
		out.name("scripts").openArray();
		for (Path file : jsonFiles(dir)) {
			String name = ScriptDocument.nameOfFile(file.getFileName().toString());
			out.openObject();
			out.field("name", name);
			try {
				String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
				ScriptDocument.fromJson(name, text);
				out.field("ok", true);
			} catch (RuntimeException e) {
				out.field("ok", false);
				out.field("error", e.getMessage());
			} catch (IOException e) {
				out.field("ok", false);
				out.field("error", "cannot read: " + e.getMessage());
			}
			out.closeObject();
		}
		out.closeArray();
		out.closeObject();
		return out.toString();
	}

	/** The document for one script, or null when there is no such file. For the editor's "open". */
	public static String read(Path dir, String name) throws IOException {
		requireSafeName(name);
		Path file = file(dir, name);
		if (!Files.isRegularFile(file)) {
			return null;
		}
		return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
	}

	/** The file a script name maps to. Only ever called after {@link #requireSafeName}. */
	private static Path file(Path dir, String name) {
		return dir.resolve(name + EXTENSION);
	}

	/** The {@code *.json} files in {@code dir}, sorted, or an empty list when it does not exist yet. */
	private static List<Path> jsonFiles(Path dir) {
		if (dir == null || !Files.isDirectory(dir)) {
			return List.of();
		}
		try (Stream<Path> stream = Files.list(dir)) {
			return stream.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)
							.endsWith(EXTENSION))
					.sorted()
					.collect(Collectors.toList());
		} catch (IOException e) {
			return List.of();
		}
	}

	/** A name that is safe to use as a file name, or a refusal naming the rule. */
	public static void requireSafeName(String name) {
		if (name == null || !NAME.matcher(name).matches()) {
			throw new DocException("a script name must be 1-48 characters of a-z, 0-9 or underscore, "
					+ "got \"" + name + "\"");
		}
	}

	// ---- canonical emission --------------------------------------------------------------------

	private static String emit(Map<String, Object> document) {
		// `root` is present and is a node: ScriptDocument.fromDocument rejected the document otherwise.
		Json out = new Json();
		out.openObject();
		out.name("root");
		writeNode(out, document.get("root"));
		out.closeObject();
		return out.toString();
	}

	/**
	 * One node, its parameters in the order the node's constructor declares them.
	 *
	 * <p>An optional parameter the document omitted is left out, so the file says only what the author
	 * chose; a value equal to the default is written when the document stated it, because the document is
	 * the author's intent and this must not silently drop it.
	 */
	private static void writeNode(Json out, Object value) {
		Map<String, Object> node = asObject(value, "a node");
		Object idValue = node.get("node");
		String id = String.valueOf(idValue);
		// Non-null: the loader resolved this id before it accepted the document.
		NodeSchema schema = BotNodeRegistry.schemaById(id);
		if (schema == null) {
			throw new DocException("internal: node \"" + id + "\" validated but has no schema");
		}
		out.openObject();
		out.field("node", id);
		for (NodeParam param : schema.params()) {
			if (node.containsKey(param.name())) {
				writeParam(out, param, node.get(param.name()));
			}
		}
		out.closeObject();
	}

	private static void writeParam(Json out, NodeParam param, Object value) {
		out.name(param.name());
		switch (param.type()) {
		case INT:
			out.value(whole(value, param));
			break;
		case BOOLEAN:
			out.value((Boolean) value);
			break;
		case STRING:
		case KIND:
			out.value((String) value);
			break;
		case TILE:
			writeTile(out, value, param);
			break;
		case NODE:
			writeNode(out, value);
			break;
		case NODE_LIST:
			out.openArray();
			for (Object child : asList(value, param)) {
				writeNode(out, child);
			}
			out.closeArray();
			break;
		default:
			// LOCATION and anything added later: the loader would have rejected it, so reaching here means
			// the tool is out of step with the schema rather than that the input is bad.
			throw new DocException("internal: no canonical form for a " + param.type() + " parameter");
		}
	}

	private static void writeTile(Json out, Object value, NodeParam param) {
		Map<String, Object> tile = asObject(value, "the tile for \"" + param.name() + "\"");
		out.openObject();
		out.field("x", whole(tile.get("x"), param));
		out.field("y", whole(tile.get("y"), param));
		if (tile.containsKey("plane")) {
			out.field("plane", whole(tile.get("plane"), param));
		}
		out.closeObject();
	}

	/** A parsed number as a JSON integer: the loader already refused fractions and out-of-range values. */
	private static long whole(Object value, NodeParam param) {
		if (!(value instanceof Number)) {
			throw new DocException("internal: \"" + param.name() + "\" held " + value + ", not a number");
		}
		return ((Number) value).longValue();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asObject(Object value, String what) {
		if (!(value instanceof Map)) {
			throw new DocException(what + " must be a JSON object");
		}
		return (Map<String, Object>) value;
	}

	@SuppressWarnings("unchecked")
	private static List<Object> asList(Object value, NodeParam param) {
		if (!(value instanceof List)) {
			throw new DocException("internal: \"" + param.name() + "\" held a non-list");
		}
		return (List<Object>) value;
	}
}
