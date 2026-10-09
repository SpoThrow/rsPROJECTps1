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
import server.game.bots.meta.RuntimeOnly;

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

	/**
	 * Every concrete, top-level {@code BotState} implementation in the compiled server tree that is a
	 * candidate <em>node</em>.
	 *
	 * <p>States marked {@link RuntimeOnly} are excluded: they are {@code BotState}s the runtime applies
	 * itself (Phase F's tracing wrapper) and are not things an author places, so including them would make
	 * the unannotated check fail on infrastructure and the registry check demand a palette entry for one.
	 * The exclusion is declared on the class, not listed here, so adding another wrapper is a reviewable
	 * one-line decision rather than a quiet edit to this filter.
	 */
	public static List<Class<?>> botStateImplementations() {
		List<Class<?>> found = new ArrayList<Class<?>>();
		for (Class<?> type : everyBotState()) {
			// A wrapper the runtime applies itself is a BotState but never a node: no author places one,
			// and the palette must not offer one. Marked on the class, with the reason.
			if (!type.isAnnotationPresent(RuntimeOnly.class)) {
				found.add(type);
			}
		}
		return found;
	}

	/**
	 * Every concrete, top-level {@code BotState} in the compiled server tree, including
	 * {@link RuntimeOnly} ones.
	 *
	 * <p>Needed by the parity test, which has to see the runtime-only states in order to assert the
	 * <em>other</em> direction: that they are absent from the registry and that the marker carries a
	 * reason. Filtering them out here — rather than in {@link #botStateImplementations()} — is what keeps
	 * "the marker is only used for non-nodes" testable at all.
	 */
	public static List<Class<?>> everyBotState() {
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
