package botworkshop;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import botworkshop.export.BotNodes;
import botworkshop.meta.NodeCoverage;
import server.game.bots.meta.BotNodeRegistry;
import server.game.bots.meta.NodeParam;
import server.game.bots.meta.NodeSchema;

/**
 * Bot Workshop node exporter — {@code BOT_TOOLING.md} stage T4.
 *
 * <p>Writes the palette the editor authors trees from: one row per {@code @BotNode}, with its
 * reflected constructor parameters. Run from the server directory so the export lands next to the
 * map export:
 *
 * <pre>gradlew workshopExportNodes</pre>
 *
 * <p><b>It refuses to export a tree it cannot fully describe.</b> Before writing, it walks the
 * compiled server classes for every concrete {@code BotState} and fails if any of them is missing
 * {@code @BotNode}, or if two nodes claim the same id. An editor that silently omitted a node would
 * be worse than a build that stops: the omission would surface as a missing palette entry months
 * later, while this failure surfaces with the class name that caused it.
 */
public final class ExportBotNodes {

	private static final String OUTPUT = "Data/workshop/bot-nodes.json";

	private ExportBotNodes() {
	}

	public static void main(String[] args) throws Exception {
		List<Class<?>> unannotated = NodeCoverage.unannotated();
		if (!unannotated.isEmpty()) {
			StringBuilder message = new StringBuilder("these BotState classes are not @BotNode and so "
					+ "cannot be authored:\n");
			for (Class<?> type : unannotated) {
				message.append("  ").append(type.getName()).append('\n');
			}
			message.append("annotate them, or the editor cannot offer them.");
			throw new IllegalStateException(message.toString());
		}

		List<NodeSchema> schemas = BotNodeRegistry.schemas();
		Set<String> ids = new HashSet<String>();
		for (NodeSchema schema : schemas) {
			if (!ids.add(schema.id())) {
				throw new IllegalStateException("two nodes share the id \"" + schema.id() + "\"");
			}
		}

		Path out = Paths.get(OUTPUT);
		Files.createDirectories(out.getParent());
		byte[] document = BotNodes.toJson(schemas).getBytes(StandardCharsets.UTF_8);
		Files.write(out, document);

		List<String> coverage = new ArrayList<String>();
		for (Class<?> type : NodeCoverage.botStateImplementations()) {
			coverage.add(type.getSimpleName());
		}
		List<String> runtimeOnly = new ArrayList<String>();
		for (Class<?> type : NodeCoverage.everyBotState()) {
			if (!coverage.contains(type.getSimpleName())) {
				runtimeOnly.add(type.getSimpleName());
			}
		}

		System.out.println();
		System.out.println("[workshop] Bot Workshop node schema");
		for (NodeSchema schema : schemas) {
			System.out.println("[workshop]   " + pad(schema.id(), 14) + pad(schema.category(), 11)
					+ params(schema));
		}
		System.out.println("[workshop] nodes                = " + schemas.size());
		System.out.println("[workshop] BotState classes     = " + coverage.size()
				+ " (" + String.join(", ", coverage) + ")");
		// Reported rather than left implicit: an @RuntimeOnly state is a deliberate exception to the
		// one-to-one rule, so the build output should say which one it is.
		System.out.println("[workshop] runtime-only states  = " + runtimeOnly.size()
				+ " (" + (runtimeOnly.isEmpty() ? "none" : String.join(", ", runtimeOnly)) + ")");
		System.out.println("[workshop] wrote " + document.length + " bytes to " + out.toAbsolutePath());
	}

	private static String params(NodeSchema schema) {
		if (schema.params().isEmpty()) {
			return "(no parameters)";
		}
		StringBuilder text = new StringBuilder();
		for (NodeParam param : schema.params()) {
			if (text.length() > 0) {
				text.append(", ");
			}
			text.append(param.type().name().toLowerCase()).append(' ').append(param.name());
			if (!param.required()) {
				text.append(" = ").append(param.defaultValue());
			}
		}
		return text.toString();
	}

	private static String pad(String value, int width) {
		return value.length() >= width ? value + " " : value + " ".repeat(width - value.length());
	}
}
