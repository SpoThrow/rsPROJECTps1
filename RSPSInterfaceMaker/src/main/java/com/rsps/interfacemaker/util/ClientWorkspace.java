package com.rsps.interfacemaker.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds Proxy Client Interfaces.java and sprite folders next to this repo / the live cache.
 */
public class ClientWorkspace {
    private File interfacesJava;
    private final List<File> spriteRoots = new ArrayList<>();

    public File getInterfacesJava() {
        return interfacesJava;
    }

    public List<File> getSpriteRoots() {
        return spriteRoots;
    }

    public static ClientWorkspace detect() {
        ClientWorkspace workspace = new ClientWorkspace();
        File interfaces = findInterfacesJava();
        workspace.interfacesJava = interfaces;

        File cacheSprites = new File(System.getProperty("user.home"), "Biohazard.474" + File.separator + "Sprites");
        addIfDir(workspace.spriteRoots, cacheSprites);
        addIfDir(workspace.spriteRoots, new File(System.getProperty("user.home"), "Biohazard.474"));

        if (interfaces != null) {
            File clientRoot = interfaces.getParentFile() != null ? interfaces.getParentFile().getParentFile() : null;
            if (clientRoot != null) {
                addIfDir(workspace.spriteRoots, new File(clientRoot, "Sprites"));
                addIfDir(workspace.spriteRoots, clientRoot);
            }
        }

        File fromCwd = findInAncestors(new File(System.getProperty("user.dir")),
            "Biohazard V3 package" + File.separator + "Biohazard v3 server client cache" + File.separator + "Proxy Client" + File.separator + "Sprites");
        addIfDir(workspace.spriteRoots, fromCwd);
        return workspace;
    }

    private static File findInterfacesJava() {
        String relative = "Biohazard V3 package" + File.separator + "Biohazard v3 server client cache"
            + File.separator + "Proxy Client" + File.separator + "src" + File.separator + "Interfaces.java";

        File fromCwd = findInAncestors(new File(System.getProperty("user.dir")), relative);
        if (fromCwd != null) {
            return fromCwd;
        }

        File homeRepo = new File(System.getProperty("user.home"),
            "Documents" + File.separator + "GitHub" + File.separator + "rsPROJECTps" + File.separator + relative);
        if (homeRepo.isFile()) {
            return homeRepo;
        }
        return null;
    }

    private static File findInAncestors(File start, String relative) {
        File dir = start.getAbsoluteFile();
        for (int i = 0; i < 8 && dir != null; i++) {
            File candidate = new File(dir, relative);
            if (candidate.isFile() || candidate.isDirectory()) {
                return candidate;
            }
            dir = dir.getParentFile();
        }
        return null;
    }

    private static void addIfDir(List<File> roots, File dir) {
        if (dir != null && dir.isDirectory() && !roots.contains(dir)) {
            roots.add(dir);
        }
    }
}
