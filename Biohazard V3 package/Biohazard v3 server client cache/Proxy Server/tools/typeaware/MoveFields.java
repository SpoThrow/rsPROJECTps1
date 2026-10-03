/*
 * MoveFields -- the type-aware field-cluster rewriter for the Phase 4 "decompose Player" work.
 *
 * It attributes the whole source tree with the javac compiler API and rewrites only the
 * identifiers whose *resolved declaring element* is the cluster's owner type. That is what
 * makes it safe where a name-keyed regex is not: Player, NPC and PlayerAssistant all declare
 * absX/absY/heightLevel, PlayerAssistant has totalLevel()/xpTotal() methods that share names
 * with Player fields, and identifiers like "length" must not be touched while "playerLevel"
 * inside "playerLevel.length" must be.
 *
 * Two modes:
 *
 *   java -cp <out> MoveFields <cluster> <root> [--apply]
 *       Rewrites the cluster. Without --apply it is a dry run and writes nothing.
 *       Aborts, writing nothing at all, if javac reports any error while attributing, or if
 *       any planned splice's original text does not exactly match what it expects.
 *
 *   java -cp <out> MoveFields --fields <qualified.TypeName> <root>
 *       Prints every field the type declares, in declaration order, with its qualified and
 *       bare reference counts. This is the measurement step that must run *before* a cluster
 *       is defined (see the plan's "measure before you move" rule).
 *
 * Classpath: every jar under <root>/deps plus whatever is in the EXTRA_CP environment
 * variable (used to add the JUnit jars so that test/ sources attribute too).
 *
 *   set EXTRA_CP=<junit jars joined by ;>            (cmd)
 *   $env:EXTRA_CP = "<junit jars joined by ;>"       (PowerShell)
 *
 * ⚠️ This file lives under tools/, NOT build/, because a `gradlew clean` deletes build/ and
 * destroyed the two earlier versions of this tooling. Keep it out of build/.
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MoveFields {

	/* ------------------------------------------------------------------ clusters -- */

	/** A move: which type owns the fields, and each field's replacement path. */
	private static final class Cluster {
		final String owner;
		final Map<String, String> moves;

		Cluster(String owner, Map<String, String> moves) {
			this.owner = owner;
			this.moves = moves;
		}
	}

	private static Map<String, String> moves(String... pairs) {
		Map<String, String> m = new LinkedHashMap<>();
		for (int i = 0; i + 1 < pairs.length; i += 2) {
			m.put(pairs[i], pairs[i + 1]);
		}
		return m;
	}

	private static final Map<String, Cluster> CLUSTERS = new LinkedHashMap<>();

	static {
		// 4.7 -- position + walk/repath bookkeeping. Replaced by Position + WalkRepath.
		CLUSTERS.put("position", new Cluster("server.game.players.Player", moves(
				"absX", "position.absX",
				"absY", "position.absY",
				"heightLevel", "position.heightLevel",
				"currentX", "position.currentX",
				"currentY", "position.currentY",
				"mapRegionX", "position.mapRegionX",
				"mapRegionY", "position.mapRegionY",
				"teleportToX", "position.teleportToX",
				"teleportToY", "position.teleportToY",
				"lastWalkDestX", "walkRepath.lastWalkDestX",
				"lastWalkDestY", "walkRepath.lastWalkDestY",
				"walkRepathPending", "walkRepath.walkRepathPending")));

		// 4.8 -- the skill table. The skill-index constants (playerAttack ..
		// playerRunecrafting) deliberately stay on Player: they are static and cannot live
		// on a per-player collaborator, but they index both arrays.
		CLUSTERS.put("skills", new Cluster("server.game.players.Player", moves(
				"playerLevel", "skills.playerLevel",
				"playerXP", "skills.playerXP",
				"totalLevel", "skills.totalLevel",
				"xpTotal", "skills.xpTotal")));

		// 4.9 -- appearance. playerProps is deliberately absent: it is a static scratch
		// buffer, not per-player state.
		CLUSTERS.put("appearance", new Cluster("server.game.players.Player", moves(
				"playerAppearance", "appearance.playerAppearance",
				"appearanceUpdateRequired", "appearance.appearanceUpdateRequired",
				"canChangeAppearance", "appearance.canChangeAppearance",
				"headIcon", "appearance.headIcon",
				"headIconPk", "appearance.headIconPk",
				"headIconHints", "appearance.headIconHints")));

		// 4.10 -- the pure "*Delay" availability timers (the "you may do X again" clocks).
		// The duration/schedule timers (hitDelay, attackTimer, freezeTimer, respawnTimer,
		// delayedDamage) and the persisted skullTimer are deliberately not here.
		CLUSTERS.put("timers", new Cluster("server.game.players.Player", moves(
				"foodDelay", "timers.foodDelay",
				"potDelay", "timers.potDelay",
				"alchDelay", "timers.alchDelay",
				"teleBlockDelay", "timers.teleBlockDelay",
				"godSpellDelay", "timers.godSpellDelay",
				"reduceSpellDelay", "timers.reduceSpellDelay",
				"ssDelay", "timers.ssDelay",
				"clawDelay", "timers.clawDelay",
				"buryDelay", "timers.buryDelay",
				"dfsDelay", "timers.dfsDelay",
				"protMageDelay", "timers.protMageDelay",
				"protMeleeDelay", "timers.protMeleeDelay",
				"protRangeDelay", "timers.protRangeDelay",
				"duelDelay", "timers.duelDelay",
				"logoutDelay", "timers.logoutDelay",
				"teleGrabDelay", "timers.teleGrabDelay",
				"singleCombatDelay", "timers.singleCombatDelay",
				"singleCombatDelay2", "timers.singleCombatDelay2")));

		// The clusters for 4.1-4.6 (shop, smelting, clan-chat, settings, woodcutting,
		// bounty hunter) are already applied and verified; their members are recorded in
		// the plan's sections 4.1-4.6 and would only be needed again to re-derive them.
	}

	/* ---------------------------------------------------------------------- main -- */

	public static void main(String[] args) throws Exception {
		if (args.length >= 3 && args[0].equals("--fields")) {
			report(args[1], Paths.get(args[2]).toAbsolutePath().normalize());
			return;
		}
		if (args.length < 2) {
			System.err.println("usage: MoveFields <cluster> <root> [--apply]");
			System.err.println("       MoveFields --fields <qualified.TypeName> <root>");
			System.err.println("known clusters: " + String.join(", ", CLUSTERS.keySet()));
			System.exit(2);
		}
		Cluster cluster = CLUSTERS.get(args[0]);
		if (cluster == null) {
			System.err.println("unknown cluster: " + args[0]);
			System.err.println("known clusters: " + String.join(", ", CLUSTERS.keySet()));
			System.exit(2);
		}
		rewrite(cluster, Paths.get(args[1]).toAbsolutePath().normalize(),
				Arrays.asList(args).contains("--apply"));
	}

	/* -------------------------------------------------------------------- engine -- */

	private static final class Edit {
		final String expected;
		final int start;
		final int end;
		final String replacement;

		Edit(String expected, int start, int end, String replacement) {
			this.expected = expected;
			this.start = start;
			this.end = end;
			this.replacement = replacement;
		}
	}

	private static void rewrite(Cluster cluster, Path root, boolean apply) throws Exception {
		List<Path> sources = collectSources(root);
		System.out.println("scanned  : " + sources.size() + " source files");

		DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
		JavacTask task = newTask(root, sources, diags);
		Iterable<? extends CompilationUnitTree> units = task.parse();
		task.analyze();
		long errors = printDiagnostics(diags);
		if (errors > 0) {
			System.err.println("aborting: refusing to rewrite a tree that does not attribute");
			System.exit(1);
		}

		System.out.println("owner    : " + cluster.owner);
		System.out.println("fields   : " + cluster.moves.size());

		Trees trees = Trees.instance(task);
		Map<String, List<Edit>> byFile = new LinkedHashMap<>();
		Map<String, Integer> leftAlone = new TreeMap<>();
		Map<String, Integer> perField = new LinkedHashMap<>();

		for (CompilationUnitTree unit : units) {
			Path file = Paths.get(unit.getSourceFile().toUri());
			List<Edit> edits = new ArrayList<>();
			new Rewriter(unit, trees, cluster, edits, leftAlone, perField).scan(unit, null);
			if (!edits.isEmpty()) {
				byFile.put(file.toString(), edits);
			}
		}

		int total = byFile.values().stream().mapToInt(List::size).sum();
		System.out.println("edits    : " + total + " across " + byFile.size() + " files");
		System.out.println();

		System.out.println("-- per field --");
		for (Map.Entry<String, String> e : cluster.moves.entrySet()) {
			int n = perField.getOrDefault(e.getKey(), 0);
			if (n > 0) {
				System.out.printf("  %-20s %5d  -> %s%n", e.getKey(), n, e.getValue());
			} else {
				System.out.printf("  %-20s %5d  -> %s   <- none%n", e.getKey(), n, e.getValue());
			}
		}
		System.out.println();

		System.out.println("-- left alone, by what the name actually resolved to --");
		if (leftAlone.isEmpty()) {
			System.out.println("  (nothing skipped)");
		}
		for (Map.Entry<String, Integer> e : leftAlone.entrySet()) {
			System.out.printf("  %-50s %6d%n", e.getKey(), e.getValue());
		}
		System.out.println();

		System.out.println("-- files touched --");
		byFile.entrySet().stream()
				.sorted(Comparator.comparingInt(e -> -e.getValue().size()))
				.forEach(e -> System.out.printf("  %6d  %s%n", e.getValue().size(),
						root.relativize(Paths.get(e.getKey()))));
		System.out.println();

		// Verify every splice against the file's current text BEFORE writing anything, so a
		// mismatch aborts the whole run instead of half-applying it.
		for (Map.Entry<String, List<Edit>> e : byFile.entrySet()) {
			Path file = Paths.get(e.getKey());
			String content = Files.readString(file, StandardCharsets.UTF_8);
			List<Edit> edits = e.getValue();
			edits.sort(Comparator.comparingInt(x -> x.start));
			int prevEnd = -1;
			for (Edit ed : edits) {
				if (ed.start < prevEnd || ed.end < ed.start || ed.end > content.length()) {
					throw new IllegalStateException("overlapping or out-of-range edit in " + file);
				}
				prevEnd = ed.end;
				String actual = content.substring(ed.start, ed.end);
				if (!actual.equals(ed.expected)) {
					throw new IllegalStateException("splice mismatch in " + file + " at " + ed.start
							+ ": expected '" + ed.expected + "' but found '" + actual + "'");
				}
			}
		}
		System.out.println("verified : every splice contains exactly the identifier it replaces");

		if (apply) {
			for (Map.Entry<String, List<Edit>> e : byFile.entrySet()) {
				Path file = Paths.get(e.getKey());
				String content = Files.readString(file, StandardCharsets.UTF_8);
				List<Edit> edits = e.getValue();
				StringBuilder sb = new StringBuilder(content);
				// Descending order so earlier offsets stay valid.
				for (int i = edits.size() - 1; i >= 0; i--) {
					Edit ed = edits.get(i);
					sb.replace(ed.start, ed.end, ed.replacement);
				}
				Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
			}
			System.out.println("applied  : " + byFile.size() + " files rewritten");
		} else {
			System.out.println("dry run  : nothing written (pass --apply)");
		}
	}

	/** Rewrites resolved identifiers; records what it refused to touch and why. */
	private static final class Rewriter extends TreePathScanner<Void, Void> {
		private final CompilationUnitTree unit;
		private final Trees trees;
		private final SourcePositions pos;
		private final Cluster cluster;
		private final List<Edit> edits;
		private final Map<String, Integer> leftAlone;
		private final Map<String, Integer> perField;

		Rewriter(CompilationUnitTree unit, Trees trees, Cluster cluster, List<Edit> edits,
				Map<String, Integer> leftAlone, Map<String, Integer> perField) {
			this.unit = unit;
			this.trees = trees;
			this.pos = trees.getSourcePositions();
			this.cluster = cluster;
			this.edits = edits;
			this.leftAlone = leftAlone;
			this.perField = perField;
		}

		@Override
		public Void visitIdentifier(IdentifierTree node, Void p) {
			// A bare name: only a hit if it resolves to the owner's field (it is a local or
			// parameter of the same name far more often than not).
			consider(node.getName().toString(), node);
			return super.visitIdentifier(node, p);
		}

		@Override
		public Void visitMemberSelect(MemberSelectTree node, Void p) {
			String name = node.getIdentifier().toString();
			if (cluster.moves.containsKey(name)) {
				consider(name, node);
				// Do NOT walk into the receiver: `c` in `c.playerLevel` is a local that must
				// not be counted, and a further target cannot hide behind it. When the name is
				// not ours we DO recurse, which is what finds `playerLevel` in
				// `p.playerLevel.length`.
				return null;
			}
			return super.visitMemberSelect(node, p);
		}

		private void consider(String name, Tree node) {
			// Only names this cluster actually moves are ever considered; every other
			// identifier is somebody else's business and must not be counted at all.
			if (!cluster.moves.containsKey(name)) {
				return;
			}
			Element el = trees.getElement(getCurrentPath());
			if (el == null) {
				bump(leftAlone, "unresolved");
				return;
			}
			if (el.getKind() == ElementKind.FIELD) {
				Element enclosing = el.getEnclosingElement();
				String enclosingName = (enclosing instanceof TypeElement te)
						? te.getQualifiedName().toString() : String.valueOf(enclosing);
				if (enclosingName.isEmpty()) {
					enclosingName = "<anonymous>";
				}
				if (enclosingName.equals(cluster.owner)) {
					long end = pos.getEndPosition(unit, node);
					if (end < 0) {
						throw new IllegalStateException("no source position for " + name);
					}
					edits.add(new Edit(name, (int) end - name.length(), (int) end,
							cluster.moves.get(name)));
					bump(perField, name);
					return;
				}
				bump(leftAlone, "field on " + enclosingName);
				return;
			}
			switch (el.getKind()) {
				case LOCAL_VARIABLE -> bump(leftAlone, "local variable");
				case PARAMETER -> bump(leftAlone, "parameter");
				case METHOD -> bump(leftAlone, "method");
				case CLASS, INTERFACE, ENUM -> bump(leftAlone, "type");
				case ENUM_CONSTANT -> bump(leftAlone, "enum constant");
				default -> bump(leftAlone,
						el.getKind().toString().toLowerCase(Locale.ROOT).replace('_', ' '));
			}
		}
	}

	/* --------------------------------------------------------------- --fields -- */

	private static void report(String ownerName, Path root) throws Exception {
		List<Path> sources = collectSources(root);
		System.out.println("scanned  : " + sources.size() + " source files");

		DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
		JavacTask task = newTask(root, sources, diags);
		Iterable<? extends CompilationUnitTree> units = task.parse();
		task.analyze();
		long errors = printDiagnostics(diags);
		if (errors > 0) {
			System.err.println("aborting: the tree does not attribute, so no counts would be trustworthy");
			System.exit(1);
		}

		TypeElement owner = task.getElements().getTypeElement(ownerName);
		if (owner == null) {
			System.err.println("no such type: " + ownerName);
			System.exit(2);
		}
		System.out.println("owner    : " + ownerName);

		List<String> order = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		for (Element e : owner.getEnclosedElements()) {
			if (e.getKind() == ElementKind.FIELD) {
				order.add(e.getSimpleName().toString());
				names.add(e.getSimpleName().toString());
			}
		}

		Map<String, long[]> counts = new LinkedHashMap<>();
		for (String f : order) {
			counts.put(f, new long[2]);
		}
		for (CompilationUnitTree unit : units) {
			new Counter(unit, Trees.instance(task), ownerName, names, counts).scan(unit, null);
		}

		System.out.printf("  %-33s %9s %7s %7s%n", "field", "qualified", "bare", "total");
		long tq = 0;
		long tb = 0;
		int never = 0;
		for (String f : order) {
			long[] c = counts.get(f);
			long tot = c[0] + c[1];
			tq += c[0];
			tb += c[1];
			StringBuilder line = new StringBuilder(String.format("  %-33s %9d %7d %7d", f, c[0], c[1], tot));
			if (tot == 0) {
				never++;
				line.append("   <- none");
			}
			System.out.println(line);
		}
		System.out.println();
		System.out.printf("declared fields  : %d%n", order.size());
		System.out.printf("never referenced : %d%n", never);
		System.out.printf("total refs       : %d qualified, %d bare%n", tq, tb);
	}

	/** Counts references to the owner's fields: qualified = member select, bare = plain name. */
	private static final class Counter extends TreePathScanner<Void, Void> {
		private final Trees trees;
		private final String ownerName;
		private final Set<String> names;
		private final Map<String, long[]> counts;

		Counter(CompilationUnitTree unit, Trees trees, String ownerName, Set<String> names,
				Map<String, long[]> counts) {
			this.trees = trees;
			this.ownerName = ownerName;
			this.names = names;
			this.counts = counts;
		}

		@Override
		public Void visitIdentifier(IdentifierTree node, Void p) {
			count(node.getName().toString(), false);
			return super.visitIdentifier(node, p);
		}

		@Override
		public Void visitMemberSelect(MemberSelectTree node, Void p) {
			String name = node.getIdentifier().toString();
			if (names.contains(name)) {
				count(name, true);
				return null;
			}
			return super.visitMemberSelect(node, p);
		}

		private void count(String name, boolean qualified) {
			if (!names.contains(name)) {
				return;
			}
			Element el = trees.getElement(getCurrentPath());
			if (el == null || el.getKind() != ElementKind.FIELD) {
				return;
			}
			Element enclosing = el.getEnclosingElement();
			if (!(enclosing instanceof TypeElement te) || !te.getQualifiedName().contentEquals(ownerName)) {
				return;
			}
			counts.get(name)[qualified ? 0 : 1]++;
		}
	}

	/* --------------------------------------------------------------------- util -- */

	private static long printDiagnostics(DiagnosticCollector<JavaFileObject> diags) {
		long errors = 0;
		for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
			if (d.getKind() == Diagnostic.Kind.ERROR) {
				if (errors < 10) {
					System.err.println("  " + d);
				}
				errors++;
			}
		}
		System.out.println("javac    : " + errors + " errors while attributing");
		return errors;
	}

	private static JavacTask newTask(Path root, List<Path> sources,
			DiagnosticCollector<JavaFileObject> diags) {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new IllegalStateException("no system Java compiler -- run this under a JDK, not a JRE");
		}
		StandardJavaFileManager fm = compiler.getStandardFileManager(diags, null, StandardCharsets.UTF_8);
		List<File> files = sources.stream().map(Path::toFile).collect(Collectors.toList());
		Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromFiles(files);
		List<String> options = new ArrayList<>(Arrays.asList("-proc:none", "-encoding", "UTF-8", "-nowarn"));
		String cp = classpath(root);
		if (!cp.isEmpty()) {
			options.add("-classpath");
			options.add(cp);
		}
		return (JavacTask) compiler.getTask(null, fm, diags, options, null, units);
	}

	private static List<Path> collectSources(Path root) throws IOException {
		try (Stream<Path> s = Files.walk(root)) {
			return s.filter(Files::isRegularFile)
					.filter(p -> p.toString().endsWith(".java"))
					.filter(p -> {
						for (Path seg : root.relativize(p)) {
							String n = seg.toString();
							if (n.equals("build") || n.equals(".git") || n.equals("out")
									|| n.equals("tools") || n.equals("node_modules")) {
								return false;
							}
						}
						return true;
					})
					.sorted()
					.collect(Collectors.toList());
		}
	}

	private static String classpath(Path root) {
		List<String> parts = new ArrayList<>();
		Path deps = root.resolve("deps");
		if (Files.isDirectory(deps)) {
			try (Stream<Path> s = Files.list(deps)) {
				s.filter(p -> p.toString().endsWith(".jar"))
						.sorted()
						.forEach(p -> parts.add(p.toAbsolutePath().toString()));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		String extra = System.getenv("EXTRA_CP");
		if (extra != null && !extra.isBlank()) {
			parts.addAll(Arrays.asList(extra.split(File.pathSeparator)));
		}
		return String.join(File.pathSeparator, parts);
	}

	private static void bump(Map<String, Integer> m, String key) {
		m.merge(key, 1, Integer::sum);
	}

	private MoveFields() {
	}
}
