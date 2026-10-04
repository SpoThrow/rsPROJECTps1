/*
 * MoveToPackages -- the type-aware package introducer for the client refactor (Phase 3.1).
 *
 * WHY THIS IS A SEPARATE TOOL FROM RenameTypes, NOT AN OPTION ON IT.
 * A rename changes one identifier to another and touches nothing else. A package move
 * has to do three things at once, and the third is the one that bites:
 *   (1) put the file under its new directory and add the `package` declaration;
 *   (2) add imports so every cross-package reference still resolves;
 *   (3) do (2) from the RESOLVED element, not from text -- because a regex cannot tell
 *       a type reference from a commented-out call. That is not hypothetical here:
 *       `signlink` is referenced by 15 files, not 16, because Model matches only on a
 *       COMMENTED-OUT `sign.signlink.findcachedir()` call. A regex would have written
 *       an import for a file that does not use the type.
 *
 * THE CONSTRAINT THAT SHAPES EVERYTHING.
 * The default package is not importable. So a class in a NAMED package can never see a
 * type in the DEFAULT package -- not with an import, not with a qualified name. A
 * half-finished move therefore does not compile, in a way that reads like a missing
 * symbol rather than a design error. This tool enforces the constraint instead of
 * discovering it: it ABORTS if a file that is moving references any default-package type.
 *
 * AND THE OTHER DIRECTION IS FINE. A default-package class CAN import a named package,
 * so a partial move is legal as long as the closure points one way -- which is exactly
 * what makes the leaf-first order work. `--only <pkg>` moves just the named packages and
 * rewrites the imports of everything left behind, so one leaf package can be moved,
 * built and replayed before the rest are touched.
 *
 * SAFETY MODEL (deliberately inherited from RenameTypes/MoveFields):
 *   * dry run by default -- without --apply nothing is written;
 *   * aborts, writing nothing, if javac reports any error while attributing;
 *   * verifies every insertion point against the original text before applying it;
 *   * refuses overlapping edits;
 *   * refuses a map that does not cover every top-level class it is asked about.
 *
 * USAGE
 *   javac -d tools/typeaware/out tools/typeaware/MoveToPackages.java
 *   java -cp tools/typeaware/out MoveToPackages --map tools/typeaware/packages-3.1.txt \
 *        --roots src test [--only node] [--apply]
 *
 *   <map> is "Class=package" per line; blank lines and lines starting with # are ignored.
 *
 *   Without --apply it prints what it would do and writes nothing. With --apply it
 *   rewrites the sources in place and, for moving classes, writes the file under
 *   src/<package>/ and deletes the original.
 *
 * This file lives under tools/, NOT build/, because `gradlew clean` deletes build/ and
 * has already destroyed two earlier versions of the server's rewriter.
 */

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class MoveToPackages {

    /** One textual insertion: insert `text` at [start,end) (end==start for pure inserts). */
    private static final class Edit {
        final long start, end;
        final String text, why;
        Edit(long start, long end, String text, String why) {
            this.start = start; this.end = end; this.text = text; this.why = why;
        }
    }

    public static void main(String[] args) throws Exception {
        String mapFile = null;
        boolean apply = false;
        List<String> roots = new ArrayList<String>();
        Set<String> only = new LinkedHashSet<String>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--map")) { mapFile = args[++i]; }
            else if (args[i].equals("--roots")) { while (i + 1 < args.length && !args[i + 1].startsWith("--")) roots.add(args[++i]); }
            else if (args[i].equals("--only")) { only.add(args[++i]); }
            else if (args[i].equals("--apply")) { apply = true; }
            else { System.err.println("unknown argument: " + args[i]); System.exit(2); }
        }
        if (mapFile == null || roots.isEmpty()) {
            System.err.println("usage: MoveToPackages --map <file> --roots <dir>... [--only <pkg>]... [--apply]");
            System.exit(2);
        }

        Map<String, String> map = loadMap(mapFile);
        if (map.isEmpty()) { System.err.println("empty package map: " + mapFile); System.exit(2); }
        System.out.println("package map (" + map.size() + " classes):");
        Map<String, List<String>> byPkg = new LinkedHashMap<String, List<String>>();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (!byPkg.containsKey(e.getValue())) byPkg.put(e.getValue(), new ArrayList<String>());
            byPkg.get(e.getValue()).add(e.getKey());
        }
        for (Map.Entry<String, List<String>> e : byPkg.entrySet()) {
            System.out.println("  " + e.getKey() + " (" + e.getValue().size() + "): "
                    + String.join(", ", e.getValue()));
        }
        System.out.println("moving: " + (only.isEmpty() ? "ALL packages above" : only.toString()));

        List<File> sources = collect(roots);
        System.out.println("sources: " + sources.size() + " under " + roots);
        System.out.println();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) { System.err.println("no system java compiler (run this on a JDK, not a JRE)"); System.exit(3); }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8);
        String cp = System.getProperty("java.class.path");
        if (cp != null) {
            List<File> cpFiles = new ArrayList<File>();
            for (String p : cp.split(File.pathSeparator)) if (!p.isEmpty()) cpFiles.add(new File(p));
            fm.setLocation(javax.tools.StandardLocation.CLASS_PATH, cpFiles);
        }
        Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromFiles(sources);

        JavacTask task = (JavacTask) compiler.getTask(null, fm, diagnostics, null, null, units);
        Iterable<? extends CompilationUnitTree> parsed = task.parse();
        task.analyze();

        boolean attributed = true;
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                System.err.println("ATTRIBUTION ERROR: " + d.getSource() + ":" + d.getLineNumber() + " " + d.getMessage(Locale.ROOT));
                attributed = false;
            }
        }
        if (!attributed) {
            System.err.println("javac reported errors while attributing -- refusing to rewrite anything.");
            System.exit(1);
        }

        Trees trees = Trees.instance(task);
        Elements elements = task.getElements();
        SourcePositions positions = trees.getSourcePositions();

        int totalFiles = 0, moved = 0, importsAdded = 0;
        List<String> blockers = new ArrayList<String>();
        List<String> entryPoints = new ArrayList<String>();

        for (final CompilationUnitTree cu : parsed) {
            final String src = cu.getSourceFile().getCharContent(false).toString();
            final TreePath cuPath = new TreePath(cu);

            // --- who is this file, and where is it going? -----------------------
            ClassTree own = null;
            for (Tree t : cu.getTypeDecls()) if (t instanceof ClassTree) { own = (ClassTree) t; break; }
            if (own == null) continue;                       // package-info or similar
            final String ownType = own.getSimpleName().toString();

            String existingPkg = null;
            if (cu.getPackage() != null) existingPkg = cu.getPackage().getPackageName().toString();

            String targetPkg = map.get(ownType);
            final boolean isMoving = targetPkg != null && (only.isEmpty() || only.contains(targetPkg));
            // The package this file will be IN once we are done. `null` means the default
            // package, which cannot equal any named package -- that asymmetry is the point.
            final String ownPkg = isMoving ? targetPkg : existingPkg;

            // --- which moved types does it reference? ---------------------------
            final Set<String> needed = new TreeSet<String>();
            final List<String> defaultRefs = new ArrayList<String>();
            final boolean[] hasMain = new boolean[1];

            new TreePathScanner<Void, Void>() {
                @Override public Void visitClass(ClassTree t, Void p) {
                    if (t == getCurrentPath().getCompilationUnit().getTypeDecls().get(0)) {
                        for (Tree m : t.getMembers()) {
                            if (!(m instanceof MethodTree)) continue;
                            MethodTree mt = (MethodTree) m;
                            if (mt.getName().contentEquals("main")
                                    && mt.getModifiers().getFlags().contains(Modifier.PUBLIC)
                                    && mt.getModifiers().getFlags().contains(Modifier.STATIC)) {
                                hasMain[0] = true;
                            }
                        }
                    }
                    return super.visitClass(t, p);
                }
                @Override public Void visitIdentifier(IdentifierTree t, Void p) {
                    String n = t.getName().toString();
                    if (!map.containsKey(n)) return super.visitIdentifier(t, p);
                    Element e = trees.getElement(getCurrentPath());
                    if (e == null || !(e instanceof TypeElement)) return super.visitIdentifier(t, p);
                    ElementKind k = e.getKind();
                    if (k != ElementKind.CLASS && k != ElementKind.INTERFACE && k != ElementKind.ENUM) {
                        return super.visitIdentifier(t, p);
                    }
                    TypeElement top = topLevel((TypeElement) e);
                    String refPkg = pkgOf(top);
                    String refName = top.getSimpleName().toString();
                    String refTarget = map.get(refName);
                    if (refPkg.isEmpty()) {
                        // Still in the default package ON DISK -- but that is only fatal if it
                        // is STAYING there. If it is moving in this same run it will end up in
                        // a named package, so the import is legal after all.
                        if (refTarget != null && (only.isEmpty() || only.contains(refTarget))) {
                            if (!refTarget.equals(ownPkg)) needed.add(refTarget + "." + refName);
                            return super.visitIdentifier(t, p);
                        }
                        defaultRefs.add(refName);
                        return super.visitIdentifier(t, p);
                    }
                    if (refTarget != null && !refTarget.equals(ownPkg)) needed.add(refTarget + "." + refName);
                    return super.visitIdentifier(t, p);
                }
            }.scan(cu, null);

            if (isMoving && !defaultRefs.isEmpty()) {
                blockers.add(srcName(cu) + " (" + ownType + " -> " + targetPkg + ") references "
                        + new TreeSet<String>(defaultRefs) + " in the DEFAULT package, which a packaged "
                        + "class cannot import -- that class must move too (same run or a later one).");
                continue;
            }

            // --- drop imports that are already present ---------------------------
            Set<String> existing = new LinkedHashSet<String>();
            for (ImportTree it : cu.getImports()) existing.add(it.getQualifiedIdentifier().toString());
            needed.removeAll(existing);
            if (needed.isEmpty() && !isMoving) continue;

            totalFiles++;

            List<Edit> edits = new ArrayList<Edit>();
            StringBuilder block = new StringBuilder();
            if (!needed.isEmpty()) {
                for (String imp : needed) block.append("import ").append(imp).append(";\n");
                block.append("\n");
                importsAdded += needed.size();
            }

            // --- where do the package + imports go? ------------------------------
            // The package declaration must precede every import and every type, but
            // comments may precede IT -- so it goes immediately before the first import,
            // or immediately before the type declaration when there are none.
            if (isMoving) {
                long at;
                if (!cu.getImports().isEmpty()) {
                    at = positions.getStartPosition(cu, cu.getImports().get(0));
                } else {
                    at = declStart(src, ownType, positions.getStartPosition(cu, own));
                    if (at < 0) {
                        System.err.println("could not locate declaration of " + ownType + " in " + srcName(cu) + " -- aborting");
                        System.exit(1);
                    }
                }
                if (at < 0) { System.err.println("no insertion point in " + srcName(cu) + " -- aborting"); System.exit(1); }
                edits.add(new Edit(at, at, "package " + targetPkg + ";\n\n", "package"));
            }

            if (block.length() > 0) {
                long at;
                String why;
                if (!cu.getImports().isEmpty()) {
                    ImportTree last = cu.getImports().get(cu.getImports().size() - 1);
                    at = positions.getEndPosition(cu, last);
                    why = "imports";
                    if (at < 0 || at > src.length() || src.charAt((int) at - 1) != ';') {
                        System.err.println("import insertion point is not after an import in " + srcName(cu) + " -- aborting");
                        System.exit(1);
                    }
                } else {
                    at = declStart(src, ownType, positions.getStartPosition(cu, own));
                    if (at < 0) {
                        System.err.println("could not locate declaration of " + ownType + " in " + srcName(cu) + " -- aborting");
                        System.exit(1);
                    }
                    why = "imports (no existing imports)";
                }
                edits.add(new Edit(at, at, block.toString(), why));
            }

            // --- refuse overlaps, then apply or report ---------------------------
            edits.sort(new Comparator<Edit>() {
                public int compare(Edit a, Edit b) {
                    if (a.start != b.start) return Long.compare(a.start, b.start);
                    return Long.compare(a.end, b.end);
                }
            });
            long last = -1;
            for (Edit e : edits) {
                if (e.start < last) { System.err.println("OVERLAPPING edits in " + srcName(cu) + " -- aborting"); System.exit(1); }
                last = e.start;
            }

            String newSrc = src;
            if (apply) {
                StringBuilder sb = new StringBuilder(src);
                for (int i = edits.size() - 1; i >= 0; i--) {
                    Edit e = edits.get(i);
                    sb.replace((int) e.start, (int) e.end, e.text);
                }
                newSrc = sb.toString();
                File f = new File(cu.getSourceFile().toUri());
                if (isMoving) {
                    Path dir = Paths.get("src", targetPkg);
                    Files.createDirectories(dir);
                    Path dst = dir.resolve(ownType + ".java");
                    if (Files.exists(dst)) { System.err.println("target already exists: " + dst + " -- aborting"); System.exit(1); }
                    Files.write(dst, newSrc.getBytes(StandardCharsets.UTF_8));
                    Files.delete(f.toPath());
                } else {
                    Files.write(f.toPath(), newSrc.getBytes(StandardCharsets.UTF_8));
                }
            }

            String action = isMoving ? ("MOVE -> src/" + targetPkg + "/" + ownType + ".java") : "imports only";
            System.out.printf("  %-22s %-42s %s%n", ownType,
                    (needed.isEmpty() ? "(no new imports)" : needed.toString()), action);
            if (isMoving) {
                moved++;
                if (hasMain[0]) entryPoints.add(targetPkg + "." + ownType);
            }
        }

        System.out.println();
        System.out.println((apply ? "APPLIED" : "DRY RUN (nothing written)") + ": "
                + totalFiles + " file(s), " + moved + " class(es) moved, " + importsAdded + " import(s) added");

        if (!entryPoints.isEmpty()) {
            System.out.println();
            System.out.println("ENTRY POINTS MOVED -- the launchers must change with them:");
            for (String ep : entryPoints) System.out.println("  " + ep);
            System.out.println("  (Run.bat, Record.bat and build.gradle's mainClass all name their entry class)");
        }

        if (!blockers.isEmpty()) {
            System.out.println();
            System.err.println("BLOCKED -- " + blockers.size() + " class(es) cannot move in this run:");
            for (String b : blockers) System.err.println("  " + b);
            System.out.println("Nothing was written for the blocked classes.");
            System.exit(1);
        }
    }

    /** The outermost type declaration enclosing `t` (i.e. the one that owns the file). */
    private static TypeElement topLevel(TypeElement t) {
        Element e = t;
        while (e.getEnclosingElement() != null && e.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
            e = e.getEnclosingElement();
        }
        return (TypeElement) e;
    }

    private static String pkgOf(TypeElement top) {
        Element p = top.getEnclosingElement();
        if (p instanceof PackageElement) return ((PackageElement) p).getQualifiedName().toString();
        return "";
    }

    private static String srcName(CompilationUnitTree cu) {
        return new File(cu.getSourceFile().toUri()).getName();
    }

    /**
     * The offset at which to insert a package declaration or import block: the start of
     * the type declaration, INCLUDING its modifiers and annotations.
     *
     * Why this is not a simple equality test: javac's SourcePositions reports a ClassTree's
     * start at its FIRST MODIFIER, not at its name, so `public abstract class Animable`
     * starts at `public`. RenameTypes could get away with "find the name at or after the
     * start" because it only had to rewrite the name itself; an insertion point needs the
     * real declaration start. So verify that the gap between them holds nothing but
     * modifiers, annotations and whitespace -- which is what proves we are at a declaration
     * start and not, say, in the middle of an expression that mentions the type.
     */
    private static long declStart(String src, String typeName, long declAt) {
        if (declAt < 0) return -1;
        long nameAt = wholeWord(src, typeName, (int) declAt);
        if (nameAt < 0) return -1;
        String between = src.substring((int) declAt, (int) nameAt);
        for (int i = 0; i < between.length(); i++) {
            char c = between.charAt(i);
            if (Character.isJavaIdentifierPart(c) || Character.isWhitespace(c) || c == '@' || c == '.') continue;
            return -1;
        }
        return declAt;
    }

    /** Index of the first whole-word occurrence of `word` at or after `from`, or -1. */
    private static long wholeWord(String src, String word, int from) {
        int i = Math.max(0, from);
        while (true) {
            int at = src.indexOf(word, i);
            if (at < 0) return -1;
            boolean beforeOk = at == 0 || !Character.isJavaIdentifierPart(src.charAt(at - 1));
            int end = at + word.length();
            boolean afterOk = end >= src.length() || !Character.isJavaIdentifierPart(src.charAt(end));
            if (beforeOk && afterOk) return at;
            i = at + 1;
        }
    }

    private static Map<String, String> loadMap(String file) throws Exception {
        Map<String, String> m = new LinkedHashMap<String, String>();
        Set<String> seen = new LinkedHashSet<String>();
        for (String raw : Files.readAllLines(Paths.get(file), StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) { System.err.println("bad map line: " + raw); System.exit(2); }
            String cls = line.substring(0, eq).trim(), pkg = line.substring(eq + 1).trim();
            if (pkg.isEmpty()) { System.err.println("empty package for " + cls); System.exit(2); }
            if (!seen.add(cls)) {
                System.err.println("DUPLICATE map entry for " + cls + " -- a class can only go to one package");
                System.exit(2);
            }
            m.put(cls, pkg);
        }
        return m;
    }

    private static List<File> collect(List<String> roots) {
        List<File> out = new ArrayList<File>();
        for (String r : roots) walk(new File(r), out);
        out.sort(new Comparator<File>() {
            public int compare(File a, File b) { return a.getPath().compareTo(b.getPath()); }
        });
        return out;
    }

    private static void walk(File f, List<File> out) {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) walk(k, out);
        } else if (f.getName().endsWith(".java")) {
            out.add(f);
        }
    }
}
