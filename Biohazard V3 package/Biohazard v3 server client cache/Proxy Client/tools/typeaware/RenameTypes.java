/*
 * RenameTypes -- the type-aware class renamer for the client refactor (Phase 3.3).
 *
 * WHY TYPE-AWARE RATHER THAN A WORD-BOUNDARY REGEX.
 * A regex on "\bClass36\b" cannot tell a type reference from a field, a local or a
 * parameter that happens to share the name. It would also happily rewrite the name
 * inside a string literal or a comment. This tool attributes the whole source tree
 * with the javac compiler API and rewrites only the identifiers whose *resolved
 * declaring element* is the type being renamed -- the same method the server's
 * MoveFields.java uses for field clusters, and for the same reason.
 *
 * SAFETY MODEL (inherited deliberately from MoveFields):
 *   * dry run by default -- without --apply nothing is written;
 *   * aborts, writing nothing, if javac reports any error while attributing;
 *   * verifies each planned splice against the original text before applying it;
 *   * refuses overlapping splices.
 *
 * USAGE
 *   javac -d tools/typeaware/out tools/typeaware/RenameTypes.java
 *   java -cp "tools/typeaware/out;<deps jars>" RenameTypes --map <map> --roots src test [--apply]
 *
 *   <map> is a text file of "OldName=NewName" per line; blank lines and lines
 *   beginning with # are ignored.
 *
 *   Without --apply it prints every splice it would make (file:line Old -> New)
 *   and writes nothing. With --apply it rewrites the sources AND renames the
 *   .java file of each renamed type in place.
 *
 * This file lives under tools/, NOT build/, because `gradlew clean` deletes build/
 * and has already destroyed two earlier versions of the server's rewriter.
 */

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RenameTypes {

    /** One textual replacement: replace [start,end) in file with `text`. */
    private static final class Splice {
        final long start, end;
        final String text, why;
        Splice(long start, long end, String text, String why) {
            this.start = start; this.end = end; this.text = text; this.why = why;
        }
    }

    public static void main(String[] args) throws Exception {
        String mapFile = null, rootsArg = null;
        boolean apply = false;
        List<String> roots = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--map")) { mapFile = args[++i]; }
            else if (args[i].equals("--roots")) { while (i + 1 < args.length && !args[i + 1].startsWith("--")) roots.add(args[++i]); }
            else if (args[i].equals("--apply")) { apply = true; }
            else { System.err.println("unknown argument: " + args[i]); System.exit(2); }
        }
        if (mapFile == null || roots.isEmpty()) {
            System.err.println("usage: RenameTypes --map <file> --roots <dir>... [--apply]");
            System.exit(2);
        }

        Map<String, String> map = loadMap(mapFile);
        if (map.isEmpty()) { System.err.println("empty rename map: " + mapFile); System.exit(2); }
        System.out.println("rename map (" + map.size() + "):");
        for (Map.Entry<String, String> e : map.entrySet()) System.out.println("  " + e.getKey() + " -> " + e.getValue());

        List<File> sources = collect(roots);
        System.out.println("sources: " + sources.size() + " under " + roots);

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
        SourcePositions positions = trees.getSourcePositions();

        int totalSplices = 0, totalFiles = 0;
        for (CompilationUnitTree cu : parsed) {
            CharSequence cs = cu.getSourceFile().getCharContent(false);
            final String src = cs.toString();
            final List<Splice> splices = new ArrayList<Splice>();
            final Map<String, Integer> hits = new LinkedHashMap<String, Integer>();

            new TreePathScanner<Void, Void>() {
                private void add(Tree t, String oldName, String why) {
                    long s = positions.getStartPosition(getCurrentPath().getCompilationUnit(), t);
                    if (s < 0) return;
                    splices.add(new Splice(s, s + oldName.length(), map.get(oldName), why));
                }
                @Override public Void visitClass(ClassTree t, Void p) {
                    String n = t.getSimpleName().toString();
                    if (map.containsKey(n)) {
                        // The declaration name is not visited as an IdentifierTree, so
                        // find it as the first whole-word occurrence at/after the start.
                        long s = positions.getStartPosition(getCurrentPath().getCompilationUnit(), t);
                        if (s >= 0) {
                            long at = wholeWord(src, n, (int) s);
                            if (at >= 0) {
                                splices.add(new Splice(at, at + n.length(), map.get(n), "declaration"));
                                hits.put(n, hits.containsKey(n) ? hits.get(n) + 1 : 1);
                            } else {
                                System.err.println("could not locate declaration of " + n + " -- aborting");
                                System.exit(1);
                            }
                        }
                        // Constructors MUST be renamed too. Do NOT test the method's name:
                        // javac reports a constructor's getName() as "<init>", not the class
                        // name, so a name test silently skips every constructor. A null
                        // return type is the definitive constructor marker instead.
                        for (Tree m : t.getMembers()) {
                            if (!(m instanceof MethodTree)) continue;
                            MethodTree mt = (MethodTree) m;
                            if (mt.getReturnType() != null) continue;
                            long ms = positions.getStartPosition(getCurrentPath().getCompilationUnit(), mt);
                            if (ms < 0) continue;
                            long at = wholeWord(src, n, (int) ms);
                            if (at < 0) {
                                System.err.println("could not locate constructor of " + n + " -- aborting");
                                System.exit(1);
                            }
                            splices.add(new Splice(at, at + n.length(), map.get(n), "constructor"));
                            hits.put(n, hits.containsKey(n) ? hits.get(n) + 1 : 1);
                        }
                    }
                    return super.visitClass(t, p);
                }
                @Override public Void visitIdentifier(IdentifierTree t, Void p) {
                    String n = t.getName().toString();
                    if (!map.containsKey(n)) return super.visitIdentifier(t, p);
                    Element e = trees.getElement(getCurrentPath());
                    if (e != null && (e.getKind() == ElementKind.CLASS || e.getKind() == ElementKind.INTERFACE
                            || e.getKind() == ElementKind.ENUM)) {
                        add(t, n, "reference");
                        hits.put(n, hits.containsKey(n) ? hits.get(n) + 1 : 1);
                    }
                    return super.visitIdentifier(t, p);
                }
            }.scan(cu, null);

            if (splices.isEmpty()) continue;
            totalSplices += splices.size(); totalFiles++;

            splices.sort(new Comparator<Splice>() {
                public int compare(Splice a, Splice b) {
                    if (a.start != b.start) return Long.compare(a.start, b.start);
                    return Long.compare(a.end, b.end);
                }
            });
            // The same identifier can legitimately be reached twice (a NewClassTree and its
            // child IdentifierTree both resolve to the type), producing identical splices.
            // Collapse exact duplicates; abort only on genuinely PARTIAL overlaps.
            List<Splice> uniq = new ArrayList<Splice>();
            int dropped = 0;
            long last = -1;
            for (Splice s : splices) {
                if (!uniq.isEmpty()) {
                    Splice prev = uniq.get(uniq.size() - 1);
                    if (prev.start == s.start && prev.end == s.end) { dropped++; continue; }
                }
                if (s.start < last) { System.err.println("PARTIAL OVERLAP in " + cu.getSourceFile() + " -- aborting"); System.exit(1); }
                String got = src.substring((int) s.start, (int) s.end);
                String want = keyFor(map, s.text);
                if (!got.equals(want)) {
                    System.err.println("SPLICE MISMATCH in " + cu.getSourceFile() + " at " + s.start
                            + ": expected '" + want + "' found '" + got + "' -- aborting");
                    System.exit(1);
                }
                uniq.add(s);
                last = s.end;
            }
            splices.clear();
            splices.addAll(uniq);
            if (dropped > 0) System.out.println("  (collapsed " + dropped + " duplicate splice(s) in " + cu.getSourceFile().getName() + ")");

            File f = new File(cu.getSourceFile().toUri());
            String name = f.getName();
            if (apply) {
                StringBuilder sb = new StringBuilder(src);
                for (int i = splices.size() - 1; i >= 0; i--) {
                    Splice s = splices.get(i);
                    sb.replace((int) s.start, (int) s.end, s.text);
                }
                Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            }
            for (Splice s : splices) {
                long line = lineOf(src, (int) s.start);
                System.out.printf("  %-22s %5d  %-12s -> %-16s (%s)%n", name, line,
                        keyFor(map, s.text), s.text, s.why);
            }
        }

        System.out.println();
        System.out.println((apply ? "APPLIED" : "DRY RUN (nothing written)") + ": "
                + totalSplices + " splice(s) across " + totalFiles + " file(s)");

        if (apply) {
            int renamed = 0;
            for (File f : sources) {
                String base = f.getName();
                if (!base.endsWith(".java")) continue;
                String old = base.substring(0, base.length() - 5);
                if (map.containsKey(old)) {
                    Path dst = f.toPath().resolveSibling(map.get(old) + ".java");
                    if (Files.exists(dst)) { System.err.println("target already exists: " + dst); System.exit(1); }
                    Files.move(f.toPath(), dst);
                    System.out.println("  renamed file " + base + " -> " + dst.getFileName());
                    renamed++;
                }
            }
            System.out.println("files renamed: " + renamed);
        }
    }

    private static String keyFor(Map<String, String> map, String value) {
        for (Map.Entry<String, String> e : map.entrySet()) if (e.getValue().equals(value)) return e.getKey();
        return "?";
    }

    private static long lineOf(String src, int pos) {
        long line = 1;
        for (int i = 0; i < pos && i < src.length(); i++) if (src.charAt(i) == '\n') line++;
        return line;
    }

    /** Index of the first whole-word occurrence of `word` at or after `from`, or -1. */
    private static long wholeWord(String src, String word, int from) {
        int i = from;
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
        for (String raw : Files.readAllLines(Paths.get(file), StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) { System.err.println("bad map line: " + raw); System.exit(2); }
            m.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
        }
        return m;
    }

    private static List<File> collect(List<String> roots) {
        List<File> out = new ArrayList<File>();
        for (String r : roots) walk(new File(r), out);
        out.sort(new Comparator<File>() { public int compare(File a, File b) { return a.getPath().compareTo(b.getPath()); } });
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
