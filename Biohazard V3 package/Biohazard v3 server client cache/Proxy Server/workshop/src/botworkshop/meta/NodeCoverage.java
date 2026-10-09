package botworkshop.meta;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import server.game.bots.BotState;
import server.game.bots.meta.BotNode;

/**
 * Walks the compiled server classes for every {@link BotState} implementation
 * ({@code BOT_TOOLING.md} stage T4).
 *
 * <p><b>Why a directory walk and not a classpath scan.</b> The registry keeps an explicit list of
 * nodes, which is honest but can go stale. This class is the answer to that: it starts from
 * {@code BotState} itself, asks the classloader which directory that class came from, and walks it
 * for concrete implementations. So the check is against the code that actually compiled, not
 * against a manifest someone remembered to update.
 *
 * <p>Used by both {@code ExportBotNodes} (so a node added without an annotation fails the export)
 * and the parity test (so the two directions can be compared). Deliberately kept in the workshop
 * and not in the server: the server has no reason to scan its own class files at runtime.
 */
public final class NodeCoverage {

	private NodeCoverage() {
	}

	/** The directory {@code BotState} was compiled into, i.e. {@code build/classes/java/main}. */
	public static Path classesRoot() {
		try {
			var location = BotState.class.getProtectionDomain().getCodeSource().getLocation();
			Path root = Path.of(location.toURI());
			if (!Files.isDirectory(root)) {
				throw new IllegalStateException("BotState did not load from a classes directory but "
						+ "from " + root + "; the node coverage check needs the compiled tree");
			}
			return root;
		} catch (URISyntaxException e) {
			throw new IllegalStateException("BotState's class location is not a file path", e);
		}
	}

	/** Every concrete, top-level {@code BotState} implementation in the compiled server tree. */
	public static List<Class<?>> botStateImplementations() {
		Path root = classesRoot();
		List<Class<?>> found = new ArrayList<Class<?>>();
		try (Stream<Path> files = Files.walk(root)) {
			for (Path file : files.toList()) {
				if (!file.toString().endsWith(".class")) {
					continue;
				}
				String name = classNameOf(root, file);
				if (name == null) {
					continue;
				}
				Class<?> type = Class.forName(name, false, BotState.class.getClassLoader());
				if (isConcreteBotState(type)) {
					found.add(type);
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			throw new IllegalStateException("could not walk " + root, e);
		}
		found.sort((a, b) -> a.getName().compareTo(b.getName()));
		return found;
	}

	/**
	 * The implementations that forgot {@code @BotNode}. Empty is the only correct answer; the
	 * exporter and the test both treat a non-empty list as a failure, not a warning.
	 */
	public static List<Class<?>> unannotated() {
		List<Class<?>> missing = new ArrayList<Class<?>>();
		for (Class<?> type : botStateImplementations()) {
			if (!type.isAnnotationPresent(BotNode.class)) {
				missing.add(type);
			}
		}
		return missing;
	}

	private static boolean isConcreteBotState(Class<?> type) {
		return BotState.class.isAssignableFrom(type)
				&& type != BotState.class
				&& !type.isInterface()
				&& !Modifier.isAbstract(type.getModifiers())
				&& !type.isAnonymousClass()
				&& !type.isLocalClass()
				&& !type.isSynthetic()
				&& !type.isMemberClass();
	}

	/** {@code build/classes/java/main/server/game/bots/BotState.class} to a class name, or null. */
	private static String classNameOf(Path root, Path file) {
		String relative = root.relativize(file).toString();
		if (!relative.endsWith(".class") || relative.contains("module-info")) {
			return null;
		}
		String name = relative.substring(0, relative.length() - ".class".length())
				.replace(File.separatorChar, '.');
		// Inner, anonymous and local classes carry a '$'; none of them is an authored node, and
		// loading them would only add noise to the unannotated list.
		return name.indexOf('$') < 0 ? name : null;
	}
}
