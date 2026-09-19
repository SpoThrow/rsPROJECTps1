package com.rsps.interfacemaker.util;

import javafx.scene.image.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpriteLoader {
    private static final Map<String, Image> spriteCache = new HashMap<>();
    private static final List<File> searchRoots = new ArrayList<>();
    private static String spriteRootDirectory = "";

    public static void setSpriteRootDirectory(String path) {
        spriteRootDirectory = path == null ? "" : path;
        if (path != null && !path.isEmpty()) {
            File dir = new File(path);
            if (dir.isDirectory() && !searchRoots.contains(dir)) {
                searchRoots.add(0, dir);
            }
        }
        clearCache();
    }

    public static void addSearchRoot(File dir) {
        if (dir != null && dir.isDirectory() && !searchRoots.contains(dir)) {
            searchRoots.add(dir);
        }
    }

    public static String getSpriteRootDirectory() {
        return spriteRootDirectory;
    }

    public static Image loadSprite(String spritePath) {
        return loadSprite(spritePath, 0);
    }

    public static Image loadSprite(String spritePath, int spriteId) {
        if (spritePath == null || spritePath.isEmpty()) {
            return null;
        }

        String normalized = spritePath.replace('\\', '/').replaceFirst("^/+", "");
        String cacheKey = normalized + "_" + spriteId;
        if (spriteCache.containsKey(cacheKey)) {
            return spriteCache.get(cacheKey);
        }

        String[] variations = {
            normalized + " " + spriteId + ".png",
            normalized + " " + spriteId + ".PNG",
            normalized + spriteId + ".png",
            normalized + ".png",
            normalized + ".PNG"
        };

        Image image = null;
        List<File> roots = new ArrayList<>();
        if (!spriteRootDirectory.isEmpty()) {
            roots.add(new File(spriteRootDirectory));
        }
        roots.addAll(searchRoots);
        if (roots.isEmpty()) {
            roots.add(new File("."));
        }

        search:
        for (File root : roots) {
            for (String variation : variations) {
                File spriteFile = new File(root, variation.replace("/", File.separator));
                if (!spriteFile.isFile()) {
                    spriteFile = new File(root, "Sprites" + File.separator + variation.replace("/", File.separator));
                }
                if (spriteFile.isFile()) {
                    try {
                        image = new Image(spriteFile.toURI().toString());
                        System.out.println("Loaded sprite: " + spriteFile.getAbsolutePath());
                        break search;
                    } catch (Exception e) {
                        System.out.println("Failed to load sprite: " + spriteFile.getAbsolutePath() + " - " + e.getMessage());
                    }
                }
            }
        }

        spriteCache.put(cacheKey, image);
        return image;
    }

    public static void clearCache() {
        spriteCache.clear();
    }

    public static boolean spriteExists(String spritePath) {
        return loadSprite(spritePath) != null;
    }
}
