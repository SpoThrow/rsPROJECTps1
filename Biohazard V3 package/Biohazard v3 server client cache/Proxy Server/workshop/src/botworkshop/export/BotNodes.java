package botworkshop.export;

import java.util.List;

import server.game.bots.BotState;
import server.game.bots.meta.BotNodeRegistry;
import server.game.bots.meta.NodeParam;
import server.game.bots.meta.NodeSchema;

/**
 * Serialises the server's reflected node schemas to {@code bot-nodes.json}
 * ({@code BOT_TOOLING.md} stage T4).
 *
 * <p><b>This file does not describe any node.</b> Every field comes from
 * {@link BotNodeRegistry}, which reads the annotations and the constructors off the server's own
 * classes. The tool's only job is to shape that into JSON, so an editor that reads this file cannot
 * know more — or less — about a node than the server does.
 *
 * <p>The {@code default} field is {@code null} for a required parameter rather than omitted, so a
 * reader distinguishes "no default" from "a default the writer forgot". The document is sorted by
 * id upstream in {@code BotNodeRegistry.schemas()}, so re-exporting without a change produces a
 * byte-identical file and a clean diff.
 */
public final class BotNodes {

	/** Bumped when the document's shape changes, so an editor can refuse a file it cannot read. */
	public static final int FORMAT_VERSION = 1;

	private BotNodes() {
	}

	public static String toJson(List<NodeSchema> schemas) {
		Json json = new Json();
		json.openObject();
		json.field("version", FORMAT_VERSION);
		json.name("nodes").openArray();
		for (NodeSchema schema : schemas) {
			writeNode(json, schema);
		}
		json.closeArray();
		json.closeObject();
		return json.toString();
	}

	private static void writeNode(Json json, NodeSchema schema) {
		json.openObject();
		json.field("id", schema.id());
		json.field("className", schema.className());
		json.field("category", schema.category());
		json.field("summary", schema.summary());
		json.name("params").openArray();
		for (NodeParam param : schema.params()) {
			json.openObject();
			json.field("name", param.name());
			json.field("type", param.type().name());
			json.field("required", param.required());
			json.name("default").value(param.defaultValue());
			json.field("description", param.description());
			json.closeObject();
		}
		json.closeArray();
		json.closeObject();
	}

	/** The one entry point the exporter uses; kept here so the tool never sees the registry. */
	public static String export() {
		return toJson(BotNodeRegistry.schemas());
	}

	/** True when {@code type} is a node the server ships, for callers holding class names. */
	public static boolean isKnownNodeClass(String className) {
		for (Class<? extends BotState> type : BotNodeRegistry.knownNodeClasses()) {
			if (type.getName().equals(className)) {
				return true;
			}
		}
		return false;
	}
}
