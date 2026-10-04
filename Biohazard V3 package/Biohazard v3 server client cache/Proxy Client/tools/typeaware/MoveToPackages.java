/*
 * MoveToPackages -- the type-aware package introducer for the client refactor (Phase 3.1).
 *
 * WHY THIS IS A SEPARATE TOOL FROM RenameTypes, NOT AN OPTION ON IT.
 * A rename changes one identifier to another and touches nothing else. A package move
 * has to do four things at once, and the last two are the ones that bite:
 *   (1) put the file under its new directory and add the `package` declaration;
 *   (2) add imports so every cross-package reference still resolves;
 *   (3) do (2) from the RESOLVED element, not from text -- because a regex cannot tell
 *       a type reference from a commented-out call. That is not hypothetical here:
 *       `signlink` is referenced by 15 files, not 16, because Model matches only on a
 *       COMMENTED-OUT `sign.signlink.findcachedir()` call. A regex would have written
 *       an import for a file that does not use the type;
 *   (4) WIDEN the visibility of the types the move would otherwise hide.
 *
 * (4) IS THE ONE THAT MAKES A MOVE NOT PURELY MECHANICAL, AND IT IS NOT OBVIOUS.
 * These are decompiled sources, and most classes are declared package-private
 * (`final class NodeList`, not `public final class NodeList`). That was harmless while
 * everything sat in the ONE default package, where package-private means "visible to the
 * whole program". The moment a package boundary is introduced, package-private means
 * something entirely different, and every cross-package reference fails with
 * "<T> is not public in <pkg>; cannot be accessed from outside package".
 * Measured on the first real move: moving the 6 `node` classes produced 76 such errors
 * -- 70 for `NodeList` and 6 for `NodeSubList` -- while `Node`, `NodeSub` and `MRUNodes`
 * were already `public` and `NodeCache` is never referenced from outside, so it stays
 * package-private. This is why the tool runs TWO PASSES: "is this type reached from
 * another package?" is a GLOBAL question that cannot be answered file-by-file.
 *
 * THE OTHER CONSTRAINT THAT SHAPES EVERYTHING.
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
 * KNOWN LIMITATION, STATED RATHER THAN LEFT TO BE DISCOVERED.
 * This widens TYPES only, not their MEMBERS. A package-private field or method of a moved
 * class that is touched from another package will still fail to compile. The move of the
 * `node` package produced no such error, because the decompiler emitted `public` members,
 * but the remaining packages have not been applied yet and the build is the check for each
 * one. If a build fails with a MEMBER-access error rather than a type-access error, that
 * is this limitation and the same rule applies one level down.
 *
 * SAFETY MODEL (deliberately inherited from RenameTypes/MoveFields):
 *   * dry run by default -- without --apply nothing is written;
 *   * aborts, writing nothing, if javac reports any error while attributing;
 *   * verifies every insertion point against the original text before applying it;
 *   * refuses overlapping edits;
 *   * refuses a map with a duplicate entry, since a class can only go to one package.
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
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class MoveToPackages {

    /**
     * One textual insertion: insert `text` at [start,end) (end==start for pure inserts).
     * `priority` only matters when several insertions share an offset -- the package
     * declaration must end up first, then the imports, then the `public` modifier -- so
     * it is part of the sort key rather than left to chance.
     */
    private static final class Edit {
        final long start, end;
        final String text, why;
        final int priority;
        Edit(long start, long end, String text, int priority, String why) {
            this.start = start; this.end = end; this.text = text;
            this.priority = priority; this.why = why;
        }
    }

    /**
     * A file that pass 2 has decided to write. Nothing is written during the scan: a
     * 131-class move must not be able to stop half-applied because of a bad offset, a
     * blocked class or a collision discovered on file 90. Everything is decided and
     * validated first, then written in one batch.
     */
    private static final class Pending {
        File delete;                     // the original file, when the class moves
        Path write;                      // where the new text goes
        String content;
    }

    /** Everything decided about one source file, so the second pass can settle visibility. */
    private static final class Unit {
        CompilationUnitTree cu;
        String src;
        ClassTree own;
        String ownType;
        String targetPkg;                                  // null when the file is not moving
        boolean isMoving;
        String ownPkg;                                     // null means the default package
        boolean isPublic;
        boolean hasMain;
        final Set<String> needed = new TreeSet<String>();  // FQNs to import
        final List<String> defaultRefs = new ArrayList<String>();
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

        final Trees trees = Trees.instance(task);
        final SourcePositions positions = trees.getSourcePositions();
        final Types types = task.getTypes();
        // Counted separately from markMember's widenings: "reached from another package"
        // and "an override the move quietly severed" are different faults, and the second
        // is the one that can never fail a build.
        final int[] overrideWidenings = new int[1];

        // -------------------------------------------------------------------
        // PASS 1: attribute every file and record what it needs. Nothing is
        // written. Visibility needs a GLOBAL answer -- "is this type reached
        // from outside its new package?" -- so it cannot be decided
        // file-by-file in a single pass.
        // -------------------------------------------------------------------
        List<Unit> unitList = new ArrayList<Unit>();
        // MEMBER-level widening, keyed by the OWNER's compilation unit. Unlike a type, a
        // member's edit lands in a DIFFERENT file from the one that revealed the need, so
        // it is collected globally and attached during pass 2. Identity maps because these
        // are the very Tree instances the compiler produced.
        final Map<CompilationUnitTree, Set<Tree>> memberWiden =
                new IdentityHashMap<CompilationUnitTree, Set<Tree>>();
        for (CompilationUnitTree cu : parsed) {
            ClassTree own = null;
            for (Tree t : cu.getTypeDecls()) if (t instanceof ClassTree) { own = (ClassTree) t; break; }
            if (own == null) continue;                        // package-info or similar

            final Unit u = new Unit();
            u.cu = cu;
            u.src = cu.getSourceFile().getCharContent(false).toString();
            u.own = own;
            u.ownType = own.getSimpleName().toString();
            u.targetPkg = map.get(u.ownType);
            u.isMoving = u.targetPkg != null && (only.isEmpty() || only.contains(u.targetPkg));
            u.ownPkg = u.isMoving ? u.targetPkg
                    : (cu.getPackage() != null ? cu.getPackage().getPackageName().toString() : null);
            u.isPublic = own.getModifiers().getFlags().contains(Modifier.PUBLIC);
            u.hasMain = hasMain(own);

            final String ownPkg = u.ownPkg;
            new TreePathScanner<Void, Void>() {
                @Override public Void visitIdentifier(IdentifierTree t, Void p) {
                    String n = t.getName().toString();
                    Element e = trees.getElement(getCurrentPath());
                    // A NESTED type reached from another package must be public too, and it is
                    // NOT in `map` -- that lists the top-level types the move relocates, while a
                    // nested type travels with its owner. This is the third hole the first real
                    // apply fell into (OverlayManager.Panel), after members.
                    if (e instanceof TypeElement
                            && ((TypeElement) e).getEnclosingElement() instanceof TypeElement) {
                        markMember(e, ownPkg, trees, map, memberWiden);
                        return super.visitIdentifier(t, p);
                    }
                    if (!map.containsKey(n)) return super.visitIdentifier(t, p);
                    if (e == null || !(e instanceof TypeElement)) return super.visitIdentifier(t, p);
                    ElementKind k = e.getKind();
                    if (k != ElementKind.CLASS && k != ElementKind.INTERFACE && k != ElementKind.ENUM) {
                        return super.visitIdentifier(t, p);
                    }
                    TypeElement top = topLevel((TypeElement) e);
                    String refName = top.getSimpleName().toString();
                    String refTarget = map.get(refName);
                    String refPkg = pkgOf(top);
                    if (refPkg.isEmpty()) {
                        // Still in the default package ON DISK. Fatal only if it STAYS
                        // there: if it is moving in this same run it will end up in a
                        // named package, and the import is then legal.
                        if (refTarget != null && (only.isEmpty() || only.contains(refTarget))) {
                            if (!refTarget.equals(ownPkg)) u.needed.add(refTarget + "." + refName);
                        } else {
                            u.defaultRefs.add(refName);
                        }
                        return super.visitIdentifier(t, p);
                    }
                    if (refTarget != null && !refTarget.equals(ownPkg)) u.needed.add(refTarget + "." + refName);
                    return super.visitIdentifier(t, p);
                }
                // MEMBER widening. The type-only rule is NOT enough, and the first real
                // apply proved it: mapping 54 types public compiled straight into 100
                // errors of the form "X is not public in pkg.Y", because the MEMBERS
                // stayed package-private. Every call, field read and `new` resolves to an
                // element here, so the same attribution that finds types finds members.
                @Override public Void visitMethodInvocation(MethodInvocationTree t, Void p) {
                    markMember(trees.getElement(getCurrentPath()), ownPkg, trees, map, memberWiden);
                    return super.visitMethodInvocation(t, p);
                }
                @Override public Void visitNewClass(NewClassTree t, Void p) {
                    markMember(trees.getElement(getCurrentPath()), ownPkg, trees, map, memberWiden);
                    return super.visitNewClass(t, p);
                }
                @Override public Void visitMemberSelect(MemberSelectTree t, Void p) {
                    markMember(trees.getElement(getCurrentPath()), ownPkg, trees, map, memberWiden);
                    return super.visitMemberSelect(t, p);
                }
                // RULE 4: a package-private method cannot be overridden from another
                // package, so a move silently severs every override that crossed one.
                @Override public Void visitMethod(MethodTree t, Void p) {
                    Element e = trees.getElement(getCurrentPath());
                    if (e instanceof ExecutableElement && e.getKind() == ElementKind.METHOD) {
                        markOverride((ExecutableElement) e, trees, types, map, memberWiden, overrideWidenings);
                    }
                    return super.visitMethod(t, p);
                }
            }.scan(cu, null);

            unitList.add(u);
        }

        // Exactly the type names that must be public for the move to keep compiling:
        // every type reached from a file that will be in a DIFFERENT package.
        Set<String> crossPackage = new TreeSet<String>();
        for (Unit u : unitList) {
            for (String imp : u.needed) crossPackage.add(imp.substring(imp.lastIndexOf('.') + 1));
        }

        // -------------------------------------------------------------------
        // PASS 2: turn the decisions into edits, verify them, and apply.
        // -------------------------------------------------------------------
        int totalFiles = 0, moved = 0, importsAdded = 0, widened = 0, membersWidened = 0;
        List<String> blockers = new ArrayList<String>();
        List<String> entryPoints = new ArrayList<String>();
        List<Pending> pending = new ArrayList<Pending>();

        for (Unit u : unitList) {
            final String src = u.src;
            final CompilationUnitTree cu = u.cu;

            if (u.isMoving && !u.defaultRefs.isEmpty()) {
                blockers.add(srcName(cu) + " (" + u.ownType + " -> " + u.targetPkg + ") references "
                        + new TreeSet<String>(u.defaultRefs) + " in the DEFAULT package, which a packaged "
                        + "class cannot import -- that class must move too (same run or a later one).");
                continue;
            }

            Set<String> needed = new TreeSet<String>(u.needed);
            for (ImportTree it : cu.getImports()) needed.remove(it.getQualifiedIdentifier().toString());

            // Rule (4): a moved, non-public type reached from another package must be
            // widened, or the move silently changes accessibility and the build fails.
            boolean mustWiden = u.isMoving && !u.isPublic && crossPackage.contains(u.ownType);

            // Members reached from another package must be public too, and the edits land
            // HERE because this unit owns them -- even when the unit is not itself moving.
            Set<Tree> memberEdits = memberWiden.get(cu);

            if (needed.isEmpty() && !u.isMoving && !mustWiden
                    && (memberEdits == null || memberEdits.isEmpty())) continue;
            totalFiles++;

            List<Edit> edits = new ArrayList<Edit>();
            long declAt = -1;                                  // validated declaration start

            // The declaration is an insertion point for three separate reasons, and all
            // three have to be listed: a moved class with no imports goes to the top of
            // the file; so do NEW imports in a file that has none (which is NOT the same
            // as moving -- a default-package class staying put can need imports); and the
            // `public` modifier obviously belongs at the declaration. Miss one and the
            // offset stays -1.
            boolean packageAtDecl = u.isMoving && cu.getImports().isEmpty();
            boolean importsAtDecl = !needed.isEmpty() && cu.getImports().isEmpty();
            if (packageAtDecl || importsAtDecl || mustWiden) {
                declAt = declStart(src, u.ownType, positions.getStartPosition(cu, u.own));
                if (declAt < 0) {
                    System.err.println("could not locate declaration of " + u.ownType + " in " + srcName(cu) + " -- aborting");
                    System.exit(1);
                }
            }

            // (1) the package declaration -- before every import and every type, since
            //     only comments may precede it.
            if (u.isMoving) {
                long at = cu.getImports().isEmpty()
                        ? declAt
                        : positions.getStartPosition(cu, cu.getImports().get(0));
                if (at < 0) { System.err.println("no package insertion point in " + srcName(cu) + " -- aborting"); System.exit(1); }
                edits.add(new Edit(at, at, "package " + u.targetPkg + ";\n\n", 0, "package"));
            }

            // (2) the imports.
            StringBuilder block = new StringBuilder();
            if (!needed.isEmpty()) {
                for (String imp : needed) block.append("import ").append(imp).append(";\n");
                block.append("\n");
                importsAdded += needed.size();

                long at;
                if (!cu.getImports().isEmpty()) {
                    at = positions.getEndPosition(cu, cu.getImports().get(cu.getImports().size() - 1));
                    if (at < 0 || at > src.length() || src.charAt((int) at - 1) != ';') {
                        System.err.println("import insertion point is not after an import in " + srcName(cu) + " -- aborting");
                        System.exit(1);
                    }
                } else {
                    at = declAt;
                }
                edits.add(new Edit(at, at, block.toString(), 1, "imports"));
            }

            // (3) the visibility widening.
            if (mustWiden) {
                edits.add(new Edit(declAt, declAt, "public ", 2, "visibility"));
                widened++;
            }

            // (4) the MEMBER widening: the same rule one level down, but keyed to the
            //     owner's file rather than the one that revealed the need. An insertion is
            //     only safe where a declaration truly begins, so a field that shares a
            //     declaration (`int a, b;`) is refused rather than split into `int a, public b;`.
            if (memberEdits != null) {
                for (Tree decl : memberEdits) {
                    long at = positions.getStartPosition(cu, decl);
                    if (at < 0) {
                        System.err.println("could not locate a member declaration in " + srcName(cu) + " -- aborting");
                        System.exit(1);
                    }
                    int back = (int) at - 1;
                    while (back >= 0 && Character.isWhitespace(src.charAt(back))) back--;
                    if (back >= 0 && src.charAt(back) == ',') {
                        System.err.println("member at " + srcName(cu) + ":" + at
                                + " shares a declaration with another (multi-declarator field);"
                                + " handle this file by hand -- aborting");
                        System.exit(1);
                    }
                    edits.add(new Edit(at, at, "public ", 3, "member"));
                    membersWidened++;
                }
            }

            edits.sort(new Comparator<Edit>() {
                public int compare(Edit a, Edit b) {
                    if (a.start != b.start) return Long.compare(a.start, b.start);
                    return Integer.compare(a.priority, b.priority);
                }
            });
            long last = -1;
            for (Edit e : edits) {
                // `e.start < last` alone does NOT catch a -1 offset, because -1 < -1 is
                // false -- which is how a bad insertion point once reached the StringBuilder.
                if (e.start < 0 || e.end < e.start || e.end > src.length()) {
                    System.err.println("INVALID edit offset " + e.start + ".." + e.end + " (" + e.why + ","
                            + " length " + src.length() + ") in " + srcName(cu) + " -- aborting");
                    System.exit(1);
                }
                if (e.start < last) { System.err.println("OVERLAPPING edits in " + srcName(cu) + " -- aborting"); System.exit(1); }
                last = e.end > e.start ? e.end : e.start;
            }

            StringBuilder sb = new StringBuilder(src);
            // Applied back-to-front so offsets stay valid. Within one offset this
            // yields package, then imports, then `public` -- see the Edit javadoc.
            for (int i = edits.size() - 1; i >= 0; i--) {
                Edit e = edits.get(i);
                sb.replace((int) e.start, (int) e.end, e.text);
            }
            Pending p = new Pending();
            if (u.isMoving) {
                p.delete = new File(cu.getSourceFile().toUri());
                p.write = Paths.get("src", u.targetPkg).resolve(u.ownType + ".java");
            } else {
                p.write = new File(cu.getSourceFile().toUri()).toPath();
            }
            p.content = sb.toString();
            pending.add(p);

            StringBuilder what = new StringBuilder();
            for (Edit e : edits) {
                if (what.length() > 0) what.append(", ");
                what.append(e.why);
            }
            System.out.printf("  %-22s %-46s %s%n", u.ownType,
                    (needed.isEmpty() ? "(no new imports)" : needed.toString()),
                    (u.isMoving ? "MOVE -> src/" + u.targetPkg + "/" + u.ownType + ".java" : "in place")
                            + "  [" + what + "]");
            if (u.isMoving) {
                moved++;
                if (u.hasMain) entryPoints.add(u.targetPkg + "." + u.ownType);
            }
        }

        System.out.println();
        System.out.println((apply ? "APPLIED" : "DRY RUN (nothing written)") + ": "
                + totalFiles + " file(s), " + moved + " class(es) moved, "
                + widened + " class(es) widened to public, " + membersWidened
                + " member(s) widened to public, " + overrideWidenings[0]
                + " override(s) reconnected, " + importsAdded + " import(s) added");

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
            System.err.println("Nothing was written for ANY class -- fix the blockage and re-run.");
            System.exit(1);
        }

        if (apply) {
            // Validate the whole batch before touching the disk, so a collision on the
            // last file cannot leave the first 130 half-written.
            for (Pending p : pending) {
                if (p.delete != null && Files.exists(p.write)) {
                    System.err.println("target already exists: " + p.write + " -- aborting, nothing written");
                    System.exit(1);
                }
            }
            for (Pending p : pending) {
                if (p.write.getParent() != null) Files.createDirectories(p.write.getParent());
                Files.write(p.write, p.content.getBytes(StandardCharsets.UTF_8));
                if (p.delete != null) Files.delete(p.delete.toPath());
            }
        }
    }

    private static boolean hasMain(ClassTree t) {
        for (Tree m : t.getMembers()) {
            if (!(m instanceof MethodTree)) continue;
            MethodTree mt = (MethodTree) m;
            if (mt.getName().contentEquals("main")
                    && mt.getModifiers().getFlags().contains(Modifier.PUBLIC)
                    && mt.getModifiers().getFlags().contains(Modifier.STATIC)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Record that a FIELD, METHOD or CONSTRUCTOR is reached from another package and so
     * must be public -- the second half of the visibility rule, added after the first
     * real apply produced 100 "X is not public in pkg.Y" errors with every TYPE already
     * public.
     *
     * The owner's TARGET package is what decides, not where it sits on disk, so this
     * works off the same map as the rest of the tool and is correct on an unmoved tree.
     * An already-public member is skipped, as is a private one (unreachable across a
     * package boundary by definition, so it can never be the cause of one of these
     * errors). A `protected` member is skipped too, and that is a deliberate boundary:
     * it is legal to reach one through inheritance, so widening it is not the same
     * certain fix, and pass 2 refuses rather than emitting "public protected".
     *
     * The declaration TREE is recorded rather than a text offset precisely because the
     * edit belongs to the OWNER's file, which may be a different compilation unit from
     * the one being scanned.
     */
    private static void markMember(Element e, String ownPkg, Trees trees, Map<String, String> pkgMap,
                                   Map<CompilationUnitTree, Set<Tree>> out) {
        if (e == null) return;
        ElementKind k = e.getKind();
        if (k != ElementKind.FIELD && k != ElementKind.METHOD && k != ElementKind.CONSTRUCTOR
                && k != ElementKind.CLASS && k != ElementKind.INTERFACE && k != ElementKind.ENUM
                && k != ElementKind.ANNOTATION_TYPE) return;
        Set<Modifier> mods = e.getModifiers();
        if (mods.contains(Modifier.PUBLIC) || mods.contains(Modifier.PRIVATE)
                || mods.contains(Modifier.PROTECTED)) return;
        Element owner = e.getEnclosingElement();
        if (!(owner instanceof TypeElement)) return;
        TypeElement ownerTop = topLevel((TypeElement) owner);
        String ownerName = ownerTop.getSimpleName().toString();
        String ownerTarget = pkgMap.get(ownerName);
        String ownerPkg = ownerTarget != null ? ownerTarget : pkgOf(ownerTop);
        if (ownerPkg.isEmpty()) return;                       // the owner stays in the default package
        if (ownerPkg.equals(ownPkg)) return;                  // same target package: still visible
        Tree decl = trees.getTree(e);
        if (decl == null) return;                             // implicit (e.g. default ctor): the type covers it
        TreePath path = trees.getPath(e);
        if (path == null) return;
        CompilationUnitTree declCu = path.getCompilationUnit();
        Set<Tree> set = out.get(declCu);
        if (set == null) {
            set = Collections.newSetFromMap(new IdentityHashMap<Tree, Boolean>());
            out.put(declCu, set);
        }
        set.add(decl);
    }

    /**
     * RULE 4 -- preserve cross-package OVERRIDES.
     *
     * The most dangerous of the rules, because it fails SILENTLY. A package-private
     * method cannot be overridden from another package, so the moment a subclass and its
     * superclass move into different packages, every such override becomes two unrelated
     * methods: the code still compiles, and the superclass's own calls keep invoking the
     * superclass's empty body.
     *
     * That is not hypothetical -- it is how the first full move produced a white game
     * window with no compile error and no exception. `ui.RSApplet.run()` calls
     * `startUp()`, `processGameLoop()` and `processDrawing()`, all package-private and all
     * empty, and `game.client`'s real implementations of them stopped overriding. The
     * loop ran, did nothing, and never drew a frame.
     *
     * Detection deliberately does NOT use {@code Elements.overrides}, which correctly
     * answers "no" here -- the whole point is that the override no longer exists. So the
     * signature is matched by hand and the relation is treated as intended. The base is
     * widened to public, and the override with it when it is not already public, since a
     * package-private override of a public method is a weaker-access error.
     */
    private static void markOverride(ExecutableElement me, Trees trees, Types types,
                                     Map<String, String> pkgMap, Map<CompilationUnitTree, Set<Tree>> out,
                                     int[] counter) {
        if (me.getModifiers().contains(Modifier.STATIC) || me.getModifiers().contains(Modifier.PRIVATE)) return;
        Element declEl = me.getEnclosingElement();
        if (!(declEl instanceof TypeElement)) return;
        TypeElement declType = (TypeElement) declEl;
        String ownerPkg = targetPkgOf(topLevel(declType), pkgMap);
        if (ownerPkg.isEmpty()) return;
        boolean touched = false;
        for (TypeElement sup : supertypes(declType, types)) {
            String supPkg = targetPkgOf(topLevel(sup), pkgMap);
            if (supPkg.isEmpty() || supPkg.equals(ownerPkg)) continue;   // same package: the override survives
            for (Element m : sup.getEnclosedElements()) {
                if (m.getKind() != ElementKind.METHOD) continue;
                ExecutableElement sm = (ExecutableElement) m;
                if (sm.getModifiers().contains(Modifier.STATIC) || sm.getModifiers().contains(Modifier.PRIVATE)) continue;
                if (sm.getModifiers().contains(Modifier.PUBLIC) || sm.getModifiers().contains(Modifier.PROTECTED)) continue;
                if (!sameSignature(me, sm, types)) continue;
                if (widenDeclaration(sm, trees, out)) touched = true;
                if (!me.getModifiers().contains(Modifier.PUBLIC)
                        && widenDeclaration(me, trees, out)) touched = true;
            }
        }
        if (touched) counter[0]++;
    }

    /** The target package of a type: its destination if it is moving, else where it is. */
    private static String targetPkgOf(TypeElement t, Map<String, String> pkgMap) {
        String target = pkgMap.get(t.getSimpleName().toString());
        return target != null ? target : pkgOf(t);
    }

    /** Every supertype (class and interface, transitively), breadth-first. */
    private static List<TypeElement> supertypes(TypeElement t, Types types) {
        List<TypeElement> out = new ArrayList<TypeElement>();
        Set<String> seen = new LinkedHashSet<String>();
        List<TypeMirror> queue = new ArrayList<TypeMirror>();
        queue.add(t.getSuperclass());
        for (TypeMirror i : t.getInterfaces()) queue.add(i);
        while (!queue.isEmpty()) {
            TypeMirror tm = queue.remove(0);
            if (tm == null || tm.getKind() != TypeKind.DECLARED) continue;
            TypeElement te = (TypeElement) ((DeclaredType) tm).asElement();
            if (!seen.add(te.getQualifiedName().toString())) continue;
            out.add(te);
            queue.add(te.getSuperclass());
            for (TypeMirror i : te.getInterfaces()) queue.add(i);
        }
        return out;
    }

    /** Name and erased parameter types match -- i.e. these would override, given visibility. */
    private static boolean sameSignature(ExecutableElement a, ExecutableElement b, Types types) {
        if (!a.getSimpleName().contentEquals(b.getSimpleName())) return false;
        List<? extends VariableElement> pa = a.getParameters();
        List<? extends VariableElement> pb = b.getParameters();
        if (pa.size() != pb.size()) return false;
        for (int i = 0; i < pa.size(); i++) {
            String ea = types.erasure(pa.get(i).asType()).toString();
            String eb = types.erasure(pb.get(i).asType()).toString();
            if (!ea.equals(eb)) return false;
        }
        return true;
    }

    /** Queue one declaration for a `public ` insertion; false when it is already recorded. */
    private static boolean widenDeclaration(Element e, Trees trees, Map<CompilationUnitTree, Set<Tree>> out) {
        Tree decl = trees.getTree(e);
        if (decl == null) return false;
        TreePath path = trees.getPath(e);
        if (path == null) return false;
        CompilationUnitTree cu = path.getCompilationUnit();
        Set<Tree> set = out.get(cu);
        if (set == null) {
            set = Collections.newSetFromMap(new IdentityHashMap<Tree, Boolean>());
            out.put(cu, set);
        }
        return set.add(decl);
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
     * The offset at which to insert a package declaration, import block or modifier: the
     * start of the type declaration, INCLUDING its modifiers and annotations.
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
